# Build Log: VoltaicMesh Dispatch Evaluator

Phase: `/graph-build` output. Lowers the design spec into live, verified MiniGraph commands.
Status: **gate PASS** — graph built, instantiates, runs happy path, exports + round-trips. Full scenario/failure proof handed to `/graph-test`.

```yaml
build_log:
  graph: "voltaicmesh-dispatch-evaluator"
  design_artifact: ".graph-builder/designs/voltaicmesh-dispatch-evaluator-design-spec.md"
  session_id: "ws-502311-2"
  host: "http://localhost:8085"

  commands:
    # All sent via companion.mjs send (auto re-fetch + assert). Every row below returned `✓ verified`.
    # --- nodes (B3) ---
    - { seq: 1,  command: "create node root (Root)",                       verified: "node root present" }
    - { seq: 2,  command: "create node end (End)",                         verified: "node end present" }
    - { seq: 3,  command: "create node prepare-input (graph.data.mapper)", verified: "node present (later updated, seq 35/40)" }
    - { seq: 4,  command: "create node validate-input (graph.math)",       verified: "node present (later updated, seq 36)" }
    - { seq: 5,  command: "create node reject-output (graph.data.mapper)", verified: "node present" }
    - { seq: 6,  command: "create node fetch-scada (graph.api.fetcher, exception=deny-critical)", verified: "node present" }
    - { seq: 7,  command: "create node scada-dict (Dictionary)",           verified: "node present" }
    - { seq: 8,  command: "create node scada-provider (Provider)",         verified: "node present" }
    - { seq: 9,  command: "create node fetch-registry (graph.api.fetcher, exception=deny-critical)", verified: "node present" }
    - { seq: 10, command: "create node registry-dict (Dictionary)",        verified: "node present" }
    - { seq: 11, command: "create node registry-provider (Provider)",      verified: "node present" }
    - { seq: 12, command: "create node join-context (graph.join)",         verified: "node present" }
    - { seq: 13, command: "create node count-candidates (graph.data.mapper)", verified: "node present  [DEVIATION D-B1]" }
    - { seq: 14, command: "create node validate-context (graph.math)",     verified: "node present" }
    - { seq: 15, command: "create node deny-critical (graph.data.mapper)", verified: "node present" }
    - { seq: 16, command: "create node no-candidates (graph.data.mapper)", verified: "node present" }
    - { seq: 17, command: "create node evaluate-dispatch (graph.js)",      verified: "node present (final form at seq 38)" }
    - { seq: 18, command: "create node shape-output (graph.data.mapper)",  verified: "node present" }
    # --- edges (B4) ---
    - { seq: 19, command: "connect scada-dict to scada-provider with provider",        verified: "connection present" }
    - { seq: 20, command: "connect registry-dict to registry-provider with provider",  verified: "connection present" }
    - { seq: 21, command: "connect root to prepare-input with execute",                verified: "connection present" }
    - { seq: 22, command: "connect prepare-input to validate-input with execute",      verified: "connection present" }
    - { seq: 23, command: "connect validate-input to fetch-scada with execute",        verified: "connection present (fan-out 1)" }
    - { seq: 24, command: "connect validate-input to fetch-registry with execute",     verified: "connection present (fan-out 2)" }
    - { seq: 25, command: "connect fetch-scada to join-context with wait",             verified: "connection present" }
    - { seq: 26, command: "connect fetch-registry to join-context with wait",          verified: "connection present" }
    - { seq: 27, command: "connect join-context to count-candidates with execute",     verified: "connection present" }
    - { seq: 28, command: "connect count-candidates to validate-context with execute", verified: "connection present" }
    - { seq: 29, command: "connect validate-context to evaluate-dispatch with execute",verified: "connection present" }
    - { seq: 30, command: "connect evaluate-dispatch to shape-output with execute",    verified: "connection present" }
    - { seq: 31, command: "connect deny-critical to shape-output with execute",        verified: "connection present" }
    - { seq: 32, command: "connect no-candidates to shape-output with execute",        verified: "connection present" }
    - { seq: 33, command: "connect shape-output to end with complete",                 verified: "connection present" }
    - { seq: 34, command: "connect reject-output to end with complete",                verified: "connection present" }
    # --- corrective updates discovered during smoke (B6) ---
    - { seq: 35, command: "update node prepare-input -> f:defaultValue sentinels",     verified: "node present  [DEVIATION D-B2]" }
    - { seq: 36, command: "update node validate-input -> single combined presence+range IF", verified: "node present  [DEVIATION D-B2]" }
    - { seq: 37, command: "update node evaluate-dispatch -> probes (OQ-D02 resolution)", verified: "node present (intermediate)" }
    - { seq: 38, command: "update node evaluate-dispatch -> final single-line allocator + 6 result->model mappings", verified: "node present  [DEVIATION D-B3]" }
    - { seq: 39, command: "update node prepare-input -> bake int(5) -> model.max_telemetry_age constant", verified: "node present  [DEVIATION D-B2/age]" }

  smoke_test:
    instantiated: >
      input.body seeded {event_id: evt_smoke_001, target_substation_id: sub_metro_west_04,
      required_mw: 12.5, target_duration_minutes: 30, timestamp: <now epoch s>}.
      model.max_telemetry_age is baked into prepare-input (not seeded), confirmed by a run with no age seed.
    ran: >
      output.body = {decision_status: FULLY_ALLOCATED, total_allocated_mw: 12.5,
      dispatches: [{storage_node_id: bat_a, allocated_mw: 8}, {storage_node_id: bat_b, allocated_mw: 4.5}],
      binding_constraint: NONE, event_id: evt_smoke_001, target_substation_id: sub_metro_west_04,
      metrics: {requested_mw: 12.5, headroom_mw: 60, shortfall_mw: 0}}.
      Matches the design output contract (--expect output.body.decision_status satisfied).
    intermediate_inspections:
      - "model.grid populated (freq 60, load 40, max 100, telemetry_timestamp) -> both fetchers fired (stub log confirms)"
      - "model.candidate_count = 2 (count-candidates :length works)"
      - "model.headroom_mw = 60 (validate-context graph.math compute works; staleness IF passed)"
      - "evaluate-dispatch.result.decision = correct allocation object (graph.js single-line script works)"

  exported_as: "voltaicmesh-dispatch-evaluator"
  export_roundtrip: >
    export graph as voltaicmesh-dispatch-evaluator -> session reset (live graph 0/0) ->
    import graph from voltaicmesh-dispatch-evaluator -> 18 nodes / 16 connections restored.
    Post-import re-run reproduced FULLY_ALLOCATED, total 12.5 — exported artifact is the working graph.

  deviations_from_design:
    - id: "D-B1"
      change: "Added node count-candidates (graph.data.mapper) between join-context and validate-context, computing model.candidate_count via the :length selector."
      reason: "The :length / null selectors reliably create values only inside a graph.data.mapper; inline in graph.math they do not create the value and a later IF on it halts the node (verified). validate-context retains the validity-gate responsibility — this is a mechanical lowering helper, not a topology/responsibility change."
      classification: "build-level lowering — non-blocking"
    - id: "D-B2"
      change: "prepare-input uses f:defaultValue sentinels (MISSING / -1) instead of :boolean(null=true) presence flags; validate-input folds presence + range into one IF; max_telemetry_age baked as int(5) constant in prepare-input."
      reason: ":boolean(null=true) does not write a false flag when the field is present, so validate-input's IF referenced an unresolved value and halted (verified during smoke). f:defaultValue (verified fn) guarantees every field resolves. Same responsibility (reject malformed input per INV-002); arguably more faithful. Baking the age constant makes the graph self-contained for deployed (non-seeded) invocation."
      classification: "build-level lowering — non-blocking"
    - id: "D-B3"
      change: "evaluate-dispatch authored as a single-line flat multi-statement graph.js script (var refs only at top level; reduce instead of a for-loop; expression-arrow callbacks; dispatches built by .map of parallel arrays) instead of a multi-line IIFE."
      reason: "Resolves OQ-D02 empirically (probed in-session). The single-line flat form keeps every {model.*} ref clean and is a readable authoring choice [NOT forced by any multi-line limitation — there is none; see w41_correction]. Same algorithm and responsibility (FLOW-003/004/005/006, FAIL-004/005, OUT-005)."
      w39_correction: "The original rationale here ('substituteVarIfAny SWALLOWS {model.*} refs nested inside any colon/newline brace') was over-broad and is corrected by W39 re-verification (dogfood rows 50-54): substitution is innermost-first, so {path} refs nested inside object literals / IIFE bodies DO resolve — the only swallow is a lone {path} sharing its braces with a colon/newline. The deviation to a single-line script was still correct, but for the right reason: a MULTI-LINE ''' COMPUTE fails to execute (404) even when the identical single-line form runs. See minigraph-syntax.md COMPUTE section."
      w41_correction: "The w39_correction's closing reason ('a MULTI-LINE COMPUTE fails to execute (404)') is ITSELF wrong, corrected by W41 (dogfood row 52; an independent /graph-verify reviewer; source InspectStateMachine.java:47-51). A multi-line ''' COMPUTE WORKS — the identical IIFE returns 15 both single-line and multi-line; the 404 was an inspect scalar-leaf artifact (enrich.result.<scalar> 404s by design while the container enrich.result holds the value), misread as a halt. NET: authoring evaluate-dispatch as a single-line flat script was a fine, readable choice but is NOT forced by any multi-line limitation (there is none). The only graph.js COMPUTE footgun is a lone {path} sharing its braces with a colon/newline. See minigraph-syntax.md COMPUTE section + the run/inspect scalar-leaf note."
      classification: "build-level lowering — non-blocking; closes OQ-D02"
      post_build_correction: "The status/binding section of this node was later corrected during /graph-test (DEF-1): the SAFETY-vs-SHORTAGE label now keys on whether the fill REACHED the headroom cap (hit_cap = total>=cap), not on req>head. See the test report. Graph re-exported after the fix."
    - id: "D-B4"
      change: "On DISPATCH_DENIED_CRITICAL and NO_CANDIDATES_AVAILABLE paths, output.body.dispatches is OMITTED rather than an explicit empty []."
      reason: "These paths bypass evaluate-dispatch (the dispatches writer), and the engine has no empty-list literal to seed in the terminal mappers. decision_status + total_allocated_mw=0 still convey 'nothing dispatched'. Happy path unaffected."
      classification: "build-level output-shape nuance vs OUT-002/OUT-004 strict []. FLAG FOR /graph-test: confirm denial/no-candidates output shape and decide whether an empty-[] shim (e.g. a seeded model.empty_list or a JS [] producer) is required before deploy."

  mocks_used:
    - real_dependency: "SCADA telemetry API"
      mock: ".graph-builder/evidence/voltaicmesh-stub.mjs  GET http://localhost:8099/scada/substations/{id}/telemetry"
      returns: "{grid_frequency_hz, current_load_mw, operating_max_capacity_mw, telemetry_timestamp(epoch s)} with scenario hooks by substation-id substring"
      produces_state: "model.grid.*"
      deploy_blockers: ["OQ-001 real URL + auth feature (omitted for local stub)", "OQ-003 real response field paths"]
    - real_dependency: "Market registry API"
      mock: ".graph-builder/evidence/voltaicmesh-stub.mjs  GET http://localhost:8099/market/candidates?substation={id}&type=storage"
      returns: "{candidates: [{storage_node_id, dischargeable_mw, sustain_window_minutes, sla_tier, price, ramp_rate_mw_per_s}]}; empty [] for *empty* ids; /fail/* -> 500"
      produces_state: "model.candidates"
      deploy_blockers: ["OQ-001 real URL + auth", "OQ-003 real array path + per-asset field names"]
    - real_dependency: "MAX_TELEMETRY_AGE threshold"
      mock: "baked as int(5) seconds in prepare-input"
      deploy_blockers: ["OQ-002 confirm real value with grid ops"]

  gate_result:
    status: "pass"
    blockers: []
    notes:
      - "All 18 design nodes (+1 deviation helper, count-candidates) and 16 edges structurally verified; no unverified mutation."
      - "Branch targets (reject-output, deny-critical, no-candidates) and exception handler (deny-critical) exist; join-context predecessors (fetch-scada, fetch-registry) both complete on the success path; failure sinks the join and completes via the denial terminal (source-verified, no deadlock)."
      - "Instantiates + runs happy path -> full output contract. Exports + import round-trip verified."
      - "Deviations D-B1..D-B4 are build-level lowerings; none change topology responsibility, so build is NOT blocked. D-B4 is flagged for /graph-test."
    carried_deploy_blockers:
      - { id: "OQ-001", item: "real SCADA + registry URLs and auth feature", plan: "replace stub provider URLs + add auth feature before production" }
      - { id: "OQ-002", item: "MAX_TELEMETRY_AGE real value", plan: "confirm with grid ops; update the baked constant" }
      - { id: "OQ-003", item: "real response field paths/names", plan: "rebind dictionary output mappings to the real API schema" }
      - { id: "OQ-005", item: "latency budget + fetch timeouts + retry", plan: "set fetcher timeouts to SLA at deploy" }
      - { id: "OQ-007", item: "deploy to extension-callable location", plan: "deploy exported JSON to location.graph.deployed (classpath target needs rebuild)" }
```
