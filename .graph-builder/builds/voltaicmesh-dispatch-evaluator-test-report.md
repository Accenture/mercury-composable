# Test Report: VoltaicMesh Dispatch Evaluator

Phase: `/graph-test` output. Proves runtime behavior of the built graph by execution + state inspection.
Status: **gate PASS** — happy path and all 9 required scenarios verified; output contract asserted per scenario; key state transitions confirmed.

```yaml
test_report:
  graph: "voltaicmesh-dispatch-evaluator"
  build_artifact: ".graph-builder/builds/voltaicmesh-dispatch-evaluator-build-log.md"
  session_id: "ws-502311-2"
  mock_stub: ".graph-builder/evidence/voltaicmesh-stub.mjs (localhost:8099; scenario hooks by substation-id substring + scadafail/registryfail + /fail/*)"

  scenarios:
    - id: "T-01"
      name: "happy path — full allocation (NORMAL)"
      seed: "substation=sub_metro_west_04, required_mw=12.5, duration=30, timestamp=now"
      expected: "FULLY_ALLOCATED; dispatches sum=12.5; binding=NONE"
      observed: "decision_status=FULLY_ALLOCATED; dispatches=[{bat_a,8},{bat_b,4.5}]; total=12.5; binding=NONE; metrics{requested=12.5,headroom=60,shortfall=0}"
      inspected: ["output.body", "model.grid.* populated", "model.headroom_mw=60", "evaluate-dispatch.result.decision"]
      verdict: "pass"
    - id: "T-02"
      name: "invalid/missing input"
      seed: "required_mw omitted (substation=sub_metro_west_04, duration, timestamp)"
      expected: "reject envelope; output.header.status=400; no fetch"
      observed: "output.body={error:invalid_request, message:..., event_id}; output.header.status=400"
      inspected: ["output.body", "output.header.status=400"]
      verdict: "pass"
    - id: "T-03"
      name: "empty registry"
      seed: "substation=sub_empty, required_mw=12.5"
      expected: "NO_CANDIDATES_AVAILABLE; total=0"
      observed: "decision_status=NO_CANDIDATES_AVAILABLE; total=0; binding=SUPPLY; metrics{requested=12.5,headroom=60}; dispatches omitted (see AL-1)"
      inspected: ["output.body", "model.candidate_count=0 path"]
      verdict: "pass"
    - id: "T-04"
      name: "SCADA 5xx (fail-closed, no leak)"
      seed: "substation=sub_scadafail (telemetry route -> 500), required_mw=12.5"
      expected: "DISPATCH_DENIED_CRITICAL; NO upstream error body leaked"
      observed: "decision_status=DISPATCH_DENIED_CRITICAL; total=0; binding=NONE; metrics{requested=12.5} (headroom absent); body contains NO 'scada_unavailable'/upstream error"
      inspected: ["output.body (clean denial, no error key)", "exception path fetch-scada->deny-critical; join sinks, completes via denial terminal"]
      verdict: "pass"
    - id: "T-05"
      name: "stale telemetry (fail-closed)"
      seed: "substation=sub_stale (telemetry_timestamp = now-9999), required_mw=12.5"
      expected: "DISPATCH_DENIED_CRITICAL"
      observed: "decision_status=DISPATCH_DENIED_CRITICAL; total=0; metrics{requested=12.5} (headroom absent — stale IF jumped before headroom compute)"
      inspected: ["output.body", "headroom_mw correctly absent on stale path"]
      verdict: "pass"
    - id: "T-06"
      name: "supply shortage"
      seed: "substation=sub_shortage (candidates total 3 MW < demand), required_mw=12.5"
      expected: "PARTIALLY_ALLOCATED_SHORTAGE; binding=SUPPLY; shortfall>0"
      observed: "decision_status=PARTIALLY_ALLOCATED_SHORTAGE; dispatches=[{bat_small,3}]; total=3; shortfall=9.5; binding=SUPPLY; headroom=60"
      inspected: ["output.body"]
      verdict: "pass"
    - id: "T-07"
      name: "headroom cap (safety)"
      seed: "substation=sub_highload (headroom=5), required_mw=12.5"
      expected: "PARTIALLY_ALLOCATED_SAFETY; binding=HEADROOM; total=headroom"
      observed: "decision_status=PARTIALLY_ALLOCATED_SAFETY; dispatches=[{bat_a,5}]; total=5 (==headroom); shortfall=7.5; binding=HEADROOM"
      inspected: ["output.body", "clamp to headroom_mw=5 confirmed"]
      verdict: "pass"
    - id: "T-08"
      name: "distressed mode — reserve unlocked"
      seed: "substation=sub_distressed_reserve (freq 59.5 + CRITICAL_RESERVE asset), required_mw=12.5"
      expected: "DISTRESSED mode; CRITICAL_RESERVE asset eligible; ranked by ramp"
      observed: "evaluate-dispatch.result.decision.grid_mode=DISTRESSED; candidate_count=3; dispatches=[{bat_reserve,12.5}] (CRITICAL_RESERVE asset, highest ramp 9.0, ranked first); FULLY_ALLOCATED"
      inspected: ["evaluate-dispatch.result.decision (grid_mode=DISTRESSED)", "model.candidate_count=3", "reserve asset present in dispatch => lockout lifted only in distressed"]
      verdict: "pass"
    - id: "T-09"
      name: "supply exhausted BELOW the headroom ceiling (corrected — see DEF-1)"
      seed: "substation=sub_both (headroom=5, candidates total 3 MW), required_mw=12.5"
      expected: "PARTIALLY_ALLOCATED_SHORTAGE; binding=SUPPLY (allocation stopped at 3 MW of supply, never reached the 5 MW headroom cap — so headroom did NOT bind)"
      observed: "decision_status=PARTIALLY_ALLOCATED_SHORTAGE; dispatches=[{bat_small,3}]; total=3; shortfall=9.5; binding=SUPPLY; headroom=5"
      inspected: ["output.body — correctly SHORTAGE, not SAFETY, because fill.t(3) < cap(5)"]
      verdict: "pass (after DEF-1 fix; original expectation was WRONG)"
    - id: "T-10"
      name: "genuine both-bind (headroom caps a surplus-but-insufficient supply)"
      seed: "substation=sub_dualbind (headroom=5, candidates total 8 MW), required_mw=12.5"
      expected: "PARTIALLY_ALLOCATED_SAFETY; binding=BOTH (fill reaches the 5 MW headroom cap AND total supply 8 < demand 12.5)"
      observed: "decision_status=PARTIALLY_ALLOCATED_SAFETY; dispatches=[{bat_mid,5}]; total=5; shortfall=7.5; binding=BOTH"
      inspected: ["output.body — SAFETY+BOTH only when fill.t reached cap (hitCap) AND supply also short"]
      verdict: "pass"

  state_transitions_confirmed:
    - "Parallel fetch -> join: both model.grid.* and model.candidates populated before scoring (T-01)."
    - "count-candidates :length -> model.candidate_count (=3 in T-08, =0 routes NO_CANDIDATES in T-03)."
    - "validate-context headroom compute -> model.headroom_mw (60 in T-01, 5 in T-07); ABSENT on denial paths (T-04 fetch-fail, T-05 stale) per fail-closed ordering."
    - "Grid mode branch: NORMAL (T-01) vs DISTRESSED (T-08, grid_mode=DISTRESSED confirmed via result map)."
    - "Fail-closed: fetcher exception -> deny-critical with clean output.body, NO upstream error leak (T-04, FAIL-006 proven); stale -> deny-critical (T-05)."
    - "Convergent output ownership: evaluate-dispatch / deny-critical / no-candidates all -> shape-output (single output.body owner); reject-output owns the 400 envelope (T-02)."
    - "binding_constraint discriminator: NONE/SUPPLY/HEADROOM/BOTH all exercised (T-01/T-06/T-07/T-09); BOTH correctly emitted when headroom and supply both bind (T-09, K2 fix)."

  defects:
    - id: "DEF-1"
      scenario: "T-09 (supply exhausted below the headroom ceiling)"
      found_by: "adversarial review after initial /test pass"
      expected: "When allocation stops because supply ran out BELOW the headroom cap (fill.t < cap), status must be PARTIALLY_ALLOCATED_SHORTAGE / binding=SUPPLY — headroom never bound."
      observed_before_fix: "status=PARTIALLY_ALLOCATED_SAFETY / binding=BOTH — a false grid-congestion signal for what was actually a supply drought."
      root_cause: "evaluate-dispatch keyed the label on `clamped = req > head` (demand exceeds headroom) instead of on whether the allocation actually REACHED the headroom cap. `req>head` is true even when supply gives out far below `head`."
      suspected_phase: "build (logic lowering of FLOW-006/OUT-005) — the requirements/design intent ('headroom bound') was correct; the JS encoded the wrong predicate."
      fix: "Added `hitCap = fill.t >= cap` discriminator. status = FULLY (t>=req) | SHORTAGE (!hitCap) | SAFETY (hitCap). binding = NONE | SUPPLY (!hitCap) | (hitCap: supplyShort? BOTH : HEADROOM)."
      verification: "Re-ran T-06 (SHORTAGE/SUPPLY), T-07 (SAFETY/HEADROOM), T-09 (now SHORTAGE/SUPPLY), T-10 (SAFETY/BOTH) — all correct; happy path + denial regression clean. Corrected graph re-exported."
      test_lesson: "The original T-09 fixture (headroom 5, supply 3) was itself mislabeled 'both bind' and the test 'passed' against that wrong expectation — a fixture that encoded the same misunderstanding as the code. Added T-10 as a genuine both-bind fixture (supply 8 > headroom 5, both < demand)."

  adversarial_review_findings:   # symmetric rigor: 3 of 4 claims refuted with evidence, 1 real
    - { claim: "validate-context continues after a taken branch -> headroom=NaN race", verdict: "REFUTED", evidence: "GraphMath.executeStatements returns the instant an IF branches (source line 124-127); T-05 shows headroom ABSENT on the stale path." }
    - { claim: "null/absent model.candidates -> NPE crash + raw stack trace to caller", verdict: "REFUTED", evidence: "Tested a registry 200 with no candidates key (sub_nullarray): graceful NO_CANDIDATES_AVAILABLE, candidate_count=0, no crash, no leak. :length on absent yields 0." }
    - { claim: "shape-output emits headroom_mw/shortfall_mw as null on denial, violating OUT-004", verdict: "REFUTED", evidence: "T-04/T-05/nullarray all show those fields ABSENT (engine silently skips null-source mappings) — exactly satisfying OUT-004." }
    - { claim: "headroom double-bind: SAFETY label when supply exhausted below the cap", verdict: "CONFIRMED -> DEF-1 (fixed)", evidence: "T-09 (head5/supply3) returned SAFETY/BOTH pre-fix; corrected to SHORTAGE/SUPPLY." }

  accepted_limitations:
    - id: "AL-1"
      limitation: "On DISPATCH_DENIED_CRITICAL and NO_CANDIDATES_AVAILABLE paths, output.body.dispatches is OMITTED rather than an explicit empty [] (T-03/T-04/T-05). decision_status + total_allocated_mw=0 still convey 'nothing dispatched' unambiguously."
      origin: "build deviation D-B4 (engine has no empty-list literal; terminal mappers bypass the dispatches writer)."
      severity: "low — meaningful contract (status, totals, no-leak) satisfied; empty-[] vs omitted is cosmetic."
      action: "If the consuming event bus strictly requires dispatches:[] always present, route a /build fix (seed a model.empty_list constant or a tiny graph.js [] producer on the terminal paths). Otherwise accept."
    - id: "AL-2"
      limitation: "T-08 confirms DISTRESSED mode + CRITICAL_RESERVE unlock, but does not ISOLATE ramp-vs-price ranking: the reserve asset is both highest-ramp AND cheapest, so it would win under either key. The mode-dependent sort key (price asc / ramp desc) is implemented and the mode branch is proven; a dedicated fixture where ramp and price disagree was not run."
      origin: "test-fixture coverage gap, not a graph defect."
      severity: "low."
      action: "Add a distressed fixture where the cheapest asset has the lowest ramp and a pricier asset has the highest ramp, and assert the high-ramp (pricier) asset is dispatched first. Recommended before deploy."

  mocks_used:
    - { source: "SCADA telemetry API", mock: "voltaicmesh-stub.mjs /scada/substations/{id}/telemetry (+ scadafail->500)", deploy_blocker: "OQ-001 real URL+auth, OQ-003 field paths" }
    - { source: "Market registry API", mock: "voltaicmesh-stub.mjs /market/candidates (+ empty/shortage/reserve hooks, registryfail->500)", deploy_blocker: "OQ-001 real URL+auth, OQ-003 field paths" }
    - { source: "MAX_TELEMETRY_AGE", mock: "baked int(5) seconds in prepare-input", deploy_blocker: "OQ-002 confirm value with grid ops" }

  gate_result:
    status: "pass"
    blockers: []
    notes:
      - "Happy path (T-01) passes with full output contract."
      - "10 scenarios (T-01..T-10) pass after the DEF-1 fix; output contract asserted per scenario via output.body/output.header inspection."
      - "Key state transitions confirmed by inspection (parallel fetch+join, headroom, mode branch, fail-closed no-leak, convergent output, binding_constraint)."
      - "DEF-1 (headroom/supply status mislabel) found by adversarial review AFTER the initial pass, fixed in evaluate-dispatch, re-verified, and re-exported. 3 other adversarial claims refuted with source+runtime evidence (see adversarial_review_findings)."
      - "Two low-severity accepted limitations remain (AL-1 dispatches-empty-shape, AL-2 ranking-key isolation)."
    carried_deploy_blockers:
      - { id: "OQ-001", item: "real SCADA + registry URLs and auth" }
      - { id: "OQ-002", item: "MAX_TELEMETRY_AGE real value" }
      - { id: "OQ-003", item: "real response field paths" }
      - { id: "OQ-005", item: "fetch timeouts / latency SLA / retry" }
      - { id: "OQ-007", item: "deploy to extension-callable location" }
```
