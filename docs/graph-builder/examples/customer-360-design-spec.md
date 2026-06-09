# Example Graph Design Specification - Customer 360

Status: example.

Purpose: show how `/design` consumes the Customer 360 requirements brief and produces a build-ready graph architecture. It completes the brief → design round-trip: every requirement ID in [customer-360-requirements-brief.md](./customer-360-requirements-brief.md) appears in `requirement_traceability` below. The topology applies the fan-out-join *pattern* validated in P0 ([../evidence/dogfood-customer-360.md](../evidence/dogfood-customer-360.md)) — a fan-out + `risk-complete` convergence + `exception=risk-fallback` (the verified hazard-avoidance and handled-failure shapes). Note: P0 exercised a **3-branch** subset (profile/accounts/risk); this 4-source graph and its `fetch-preferences` branch are designed from that validated pattern but were not themselves instantiated.

Source brief: [customer-360-requirements-brief.md](./customer-360-requirements-brief.md).

```yaml
graph_design_spec:
  graph:
    name: "customer-360"
    purpose: "Aggregate customer profile, accounts, preferences, and risk into one caller-facing response."
    invocation: "http"
    selected_shape: "fan-out-join"

  source_brief:
    requirements_artifact: "docs/graph-builder/examples/customer-360-requirements-brief.md"
    requirements_gate_status: "pass"
    requirements_carried_blockers:
      - { question_id: "OQ-002", mock_used: "conservative concurrency (e.g. 3) until source owners confirm limits" }
      - { question_id: "OQ-001", mock_used: "mock bearer-token auth feature + neutral risk fixture" }

  # Round-trip: every requirement id from the brief's `requirements` catalog is traced here.
  requirement_traceability:
    - { requirement_id: "INV-001", design_elements: ["root", "mapper-input"], notes: "input.body.person_id normalized to model.person_id at entry." }
    - { requirement_id: "OUT-001", design_elements: ["shape-output", "end"], notes: "Final mapper owns all successful output.body paths." }
    - { requirement_id: "SRC-001", design_elements: ["fetch-profile", "fetch-accounts", "fetch-preferences", "fetch-risk", "source_plan"], notes: "Four HTTP sources, each a fetcher with dictionary+provider support nodes." }
    - { requirement_id: "MAP-001", design_elements: ["mapper-input", "fetch-* output mappings", "risk-fallback", "shape-output"], notes: "Mapping ownership split by lifecycle: input → source result → fallback → output." }
    - { requirement_id: "FLOW-001", design_elements: ["fan-out", "join-sources", "risk-complete"], notes: "Parallel fan-out from a no-skill node; required branches converge at join-sources." }
    - { requirement_id: "FAIL-001", design_elements: ["validate-person", "validation-error"], notes: "Missing person_id jumps to caller-facing validation output (null detected in mapper-input)." }
    - { requirement_id: "FAIL-002", design_elements: ["fetch-risk", "risk-fallback", "risk-complete"], notes: "Risk fetcher exception=risk-fallback; degraded path rejoins via risk-complete." }
    - { requirement_id: "FAIL-003", design_elements: ["fetch-profile", "fetch-accounts"], notes: "No exception handler on profile/accounts → failure is terminal; the upstream error body surfaces to output.body. Inferred from claim 22 (same graph.api.fetcher no-handler path, run for the risk fetcher) — the profile/accounts scenario was not separately run." }
    - { requirement_id: "NFR-001", design_elements: ["test_handoff.inspection_points", "failure_plan"], notes: "Tests inspect state paths without logging sensitive source payloads." }
    - { requirement_id: "NFR-002", design_elements: ["fan-out", "control_flow_plan.concurrency", "source_plan.mocks"], notes: "Parallel fan-out for latency; source concurrency limits carried as a build blocker (OQ-002)." }
    - { requirement_id: "TEST-001", design_elements: ["test_handoff.scenarios"], notes: "Happy path, missing person_id, and risk degraded fallback are executable with mocks." }

  node_inventory:
    - { alias: "root", type: "Root", skill: null, responsibility: "entry point", reads: [], writes: [], upstream: [], downstream: ["mapper-input"], satisfies: ["INV-001"] }
    - alias: "mapper-input"
      type: "Mapper"
      skill: "graph.data.mapper"
      responsibility: "Normalize input, detect missing key, default degraded flag."
      reads: ["input.body.person_id"]
      writes: ["model.person_id", "model.person_id_missing", "model.degraded"]
      upstream: ["root"]
      downstream: ["validate-person"]
      design_properties:
        mapping:
          - "input.body.person_id -> model.person_id"
          - "model.person_id:boolean(null=true) -> model.person_id_missing"  # null-detect MUST be in a mapper (verified)
          - "boolean(false) -> model.degraded"
      satisfies: ["INV-001", "MAP-001", "FAIL-001"]
    - alias: "validate-person"
      type: "Evaluator"
      skill: "graph.math"
      responsibility: "Branch on the missing-key flag."
      reads: ["model.person_id_missing"]
      writes: []
      upstream: ["mapper-input"]
      downstream: ["fan-out", "validation-error"]
      design_properties:
        statement:
          - "IF {model.person_id_missing} == false THEN fan-out ELSE validation-error"   # named targets, not bare `!= null` (verified)
      satisfies: ["FAIL-001", "FLOW-001"]
    - alias: "fan-out"
      type: "Stage"
      skill: null
      responsibility: "No-skill fan-out to the parallel source branches."
      reads: []
      writes: []
      upstream: ["validate-person"]
      downstream: ["fetch-profile", "fetch-accounts", "fetch-preferences", "fetch-risk"]
      satisfies: ["FLOW-001", "NFR-002"]
    - alias: "fetch-profile"
      type: "Fetcher"
      skill: "graph.api.fetcher"
      responsibility: "Required profile lookup (terminal on failure)."
      reads: ["model.person_id"]
      writes: ["model.profile", "fetch-profile.result/.status/.error"]
      upstream: ["fan-out"]
      downstream: ["join-sources"]
      design_properties: { dictionary: ["dict-profile"], input: ["model.person_id -> person_id"], output: ["result.profile -> model.profile"] }
      satisfies: ["SRC-001", "MAP-001", "FAIL-003"]
    - alias: "fetch-accounts"
      type: "Fetcher"
      skill: "graph.api.fetcher"
      responsibility: "Required accounts lookup (terminal on failure)."
      reads: ["model.person_id"]
      writes: ["model.accounts", "fetch-accounts.result/.status/.error"]
      upstream: ["fan-out"]
      downstream: ["join-sources"]
      design_properties: { dictionary: ["dict-accounts"], input: ["model.person_id -> person_id"], output: ["result.accounts -> model.accounts"] }
      satisfies: ["SRC-001", "MAP-001", "FAIL-003"]
    - alias: "fetch-preferences"
      type: "Fetcher"
      skill: "graph.api.fetcher"
      responsibility: "Preferences lookup (included in the required join for this example)."
      reads: ["model.person_id"]
      writes: ["model.preferences", "fetch-preferences.result/.status/.error"]
      upstream: ["fan-out"]
      downstream: ["join-sources"]
      design_properties: { dictionary: ["dict-preferences"], input: ["model.person_id -> person_id"], output: ["result.preferences -> model.preferences"] }
      satisfies: ["SRC-001", "MAP-001"]
    - alias: "fetch-risk"
      type: "Fetcher"
      skill: "graph.api.fetcher"
      responsibility: "Risk lookup with handled fallback on failure."
      reads: ["model.person_id"]
      writes: ["model.risk", "fetch-risk.result/.status/.error"]
      upstream: ["fan-out"]
      downstream: ["risk-complete"]
      design_properties:
        dictionary: ["dict-risk"]
        input: ["model.person_id -> person_id"]
        output: ["result.risk -> model.risk"]
        exception: "risk-fallback"
      satisfies: ["SRC-001", "MAP-001", "FAIL-002"]
    - alias: "risk-fallback"
      type: "Mapper"
      skill: "graph.data.mapper"
      responsibility: "Neutral risk fallback; mark response degraded."
      reads: ["fetch-risk.status", "fetch-risk.error"]
      writes: ["model.risk", "model.degraded"]
      upstream: ["fetch-risk"]
      downstream: ["risk-complete"]
      design_properties: { mapping: ["text(unavailable) -> model.risk", "boolean(true) -> model.degraded"] }
      satisfies: ["FAIL-002", "MAP-001"]
    - alias: "risk-complete"
      type: "Mapper"
      skill: "graph.data.mapper"
      responsibility: "Single always-completing risk predecessor for the join (verified hazard-avoidance)."
      reads: []
      writes: ["model.risk_complete"]
      upstream: ["fetch-risk", "risk-fallback"]
      downstream: ["join-sources"]
      design_properties: { mapping: ["boolean(true) -> model.risk_complete"] }
      satisfies: ["FLOW-001", "FAIL-002"]
    - alias: "join-sources"
      type: "Join"
      skill: "graph.join"
      responsibility: "Wait for profile, accounts, preferences, and the completed risk branch."
      reads: []
      writes: []
      upstream: ["fetch-profile", "fetch-accounts", "fetch-preferences", "risk-complete"]
      downstream: ["shape-output"]
      satisfies: ["FLOW-001"]
    - alias: "shape-output"
      type: "Mapper"
      skill: "graph.data.mapper"
      responsibility: "Map model state into caller-facing output.body."
      reads: ["model.*"]
      writes: ["output.body.*"]
      upstream: ["join-sources"]
      downstream: ["end"]
      design_properties:
        mapping:
          - "model.person_id -> output.body.customer_id"
          - "model.profile -> output.body.profile"
          - "model.accounts -> output.body.accounts"
          - "model.preferences -> output.body.preferences"
          - "model.risk -> output.body.risk"
          - "model.degraded -> output.body.degraded"
      satisfies: ["OUT-001", "MAP-001"]
    - alias: "validation-error"
      type: "Mapper"
      skill: "graph.data.mapper"
      responsibility: "Caller-facing validation error output."
      reads: []
      writes: ["output.body.error", "output.body.message"]
      upstream: ["validate-person"]
      downstream: ["end"]
      design_properties: { mapping: ["text(validation_error) -> output.body.error", "text(Missing or malformed person_id) -> output.body.message"] }
      satisfies: ["FAIL-001", "OUT-001"]
    - { alias: "end", type: "End", skill: null, responsibility: "complete execution; output.body is the response", reads: ["output.body"], writes: [], upstream: ["shape-output", "validation-error"], downstream: [], satisfies: ["OUT-001", "FAIL-001"] }
    # support nodes (not on the main traversal path): provider + dictionary per source
    - { alias: "provider-profile / dict-profile", type: "Provider / Dictionary", skill: null, responsibility: "HTTP endpoint + input/output binding for profile", satisfies: ["SRC-001"] }
    - { alias: "provider-accounts / dict-accounts", type: "Provider / Dictionary", skill: null, responsibility: "accounts source cluster", satisfies: ["SRC-001"] }
    - { alias: "provider-preferences / dict-preferences", type: "Provider / Dictionary", skill: null, responsibility: "preferences source cluster", satisfies: ["SRC-001"] }
    - { alias: "provider-risk / dict-risk", type: "Provider / Dictionary", skill: null, responsibility: "risk source cluster", satisfies: ["SRC-001"] }

  edge_plan:
    natural_edges:
      - { from: "root", to: "mapper-input", purpose: "begin normalization" }
      - { from: "mapper-input", to: "validate-person", purpose: "branch on missing key" }
      - { from: "fan-out", to: "fetch-profile", purpose: "parallel branch" }
      - { from: "fan-out", to: "fetch-accounts", purpose: "parallel branch" }
      - { from: "fan-out", to: "fetch-preferences", purpose: "parallel branch" }
      - { from: "fan-out", to: "fetch-risk", purpose: "parallel branch" }
      - { from: "fetch-profile", to: "join-sources", purpose: "required branch completion (wait)" }
      - { from: "fetch-accounts", to: "join-sources", purpose: "required branch completion (wait)" }
      - { from: "fetch-preferences", to: "join-sources", purpose: "required branch completion (wait)" }
      - { from: "fetch-risk", to: "risk-complete", purpose: "risk success → convergence" }
      - { from: "risk-fallback", to: "risk-complete", purpose: "risk fallback → convergence" }
      - { from: "risk-complete", to: "join-sources", purpose: "single risk predecessor (wait)" }
      - { from: "join-sources", to: "shape-output", purpose: "shape after all required branches complete" }
      - { from: "shape-output", to: "end", purpose: "successful response" }
      - { from: "validation-error", to: "end", purpose: "validation error response" }
      - { from: "dict-* ", to: "provider-*", purpose: "connect each dictionary to its provider (with provider)" }
    jumps:
      - { from: "validate-person", condition_or_statement: "person_id_missing == false", target: "fan-out" }
      - { from: "validate-person", condition_or_statement: "else", target: "validation-error" }
      - { from: "fetch-risk", condition_or_statement: "exception", target: "risk-fallback" }
    sinks: []
    joins:
      - { node: "join-sources", required_upstream: ["fetch-profile", "fetch-accounts", "fetch-preferences", "risk-complete"] }

  state_plan:
    input_paths: ["input.body.person_id"]
    model_paths: ["model.person_id", "model.person_id_missing", "model.profile", "model.accounts", "model.preferences", "model.risk", "model.degraded", "model.risk_complete"]
    node_result_paths: ["fetch-*.result", "fetch-risk.status", "fetch-risk.error"]
    output_paths: ["output.body.customer_id", "output.body.profile", "output.body.accounts", "output.body.preferences", "output.body.risk", "output.body.degraded", "output.body.error", "output.body.message"]
    mapping_ownership:
      - { node: "mapper-input", mappings: ["input → model.person_id", "null-detect → model.person_id_missing", "boolean(false) → model.degraded"] }
      - { node: "source fetchers", mappings: ["result → model.profile/accounts/preferences/risk"] }
      - { node: "risk-fallback", mappings: ["neutral risk → model.risk", "boolean(true) → model.degraded"] }
      - { node: "shape-output", mappings: ["model.* → output.body.*"] }
      - { node: "validation-error", mappings: ["constants → output.body.error/message"] }

  source_plan:
    api_fetchers:
      - { alias: "fetch-profile", dictionaries: ["dict-profile"], behavior: "required; terminal on failure (FAIL-003)" }
      - { alias: "fetch-accounts", dictionaries: ["dict-accounts"], behavior: "required; terminal on failure (FAIL-003)" }
      - { alias: "fetch-preferences", dictionaries: ["dict-preferences"], behavior: "in the required join for this example" }
      - { alias: "fetch-risk", dictionaries: ["dict-risk"], behavior: "handled by exception=risk-fallback (FAIL-002)" }
    dictionaries: ["dict-profile→provider-profile", "dict-accounts→provider-accounts", "dict-preferences→provider-preferences", "dict-risk→provider-risk"]
    providers: ["provider-profile", "provider-accounts", "provider-preferences", "provider-risk"]
    extensions: []
    flows: []
    mocks:
      - { real_dependency: "risk-api auth feature", strategy: "mock bearer-token feature + neutral risk fixture", produces: ["model.risk", "fetch-risk.status", "fetch-risk.error"], carried_blocker: "OQ-001 (deploy)" }
      - { real_dependency: "source concurrency limits", strategy: "conservative fan-out; no for_each in this design, so the concurrency property does not apply", carried_blocker: "OQ-002 (build)" }

  control_flow_plan:
    decisions:
      - { node: "validate-person", rule: "person_id_missing==false → fan-out; else → validation-error" }
      - { node: "fetch-risk", rule: "exception → risk-fallback" }
    loops_or_resets: []
    concurrency:
      - { node: "fan-out", expectation: "four source branches run in parallel via natural traversal" }
      - { node: "iterative fetchers", expectation: "none; no for_each, so the concurrency property is not used" }
    completion: "Success/degraded paths reach end via shape-output; validation failure reaches end via validation-error."

  failure_plan:
    terminal_errors:
      - "Missing person_id → validation-error output."
      - "Profile or accounts source failure is terminal (no handler); the upstream error body surfaces to output.body — inferred from claim 22 (risk fetcher, same skill path), not separately run."
    handled_errors:
      - "Risk failure → fetch-risk.status/.error set → jump to risk-fallback → model.degraded=true."
    fallback_outputs:
      - "Risk fallback returns a neutral risk object and degraded=true while preserving the other sections."

  test_handoff:
    scenarios:
      - { id: "T-01", name: "happy path", invocation: "input.body.person_id=P-10001, all sources mocked OK", expected: "output.body has profile/accounts/preferences/risk, degraded=false" }
      - { id: "T-02", name: "missing person_id", invocation: "empty input body", expected: "output.body.error=validation_error" }
      - { id: "T-03", name: "risk degraded fallback", invocation: "person_id=P-10001, risk source fails", expected: "non-risk sections present, fallback risk, degraded=true" }
    inspection_points: ["model.person_id", "model.person_id_missing", "model.profile", "model.accounts", "model.preferences", "model.risk", "model.degraded", "model.risk_complete", "fetch-risk.status", "fetch-risk.error", "output.body"]
    mock_data: ["profile/accounts/preferences/risk fixtures for P-10001", "risk-failure fixture for the fallback path"]

  build_handoff:
    command_sensitive_notes:
      - "Null detection lives in mapper-input (:boolean(null=true)), not as a graph.math statement (verified)."
      - "Risk branch joins through risk-complete; do NOT wire risk-fallback or fetch-risk directly as additional join predecessors (the join deadlock hazard is verified)."
      - "No for_each is used; if source owners later require per-item calls, add model.* for_each paths + concurrency."
    source_help_mismatches: []
    syntax_reference: "docs/graph-builder/minigraph-syntax.md"

  decisions:
    - { id: "D-01", decision: "Use fan-out-join: four independent lookups from one person_id.", source_category: "tradeoff", source_note: "Lower latency than sequential; single output-shaping point." }
    - { id: "D-02", decision: "Use risk-complete as the only risk predecessor to join-sources.", source_category: "source-verified", source_note: "graph.join waits for every backward-linked predecessor; a conditional branch must converge first (deadlock hazard verified)." }
    - { id: "D-03", decision: "Use graph.data.mapper for fallback and output shaping.", source_category: "tradeoff", source_note: "Only constants/mapping needed; avoid math/js for pure mapping." }

  open_questions:
    - { id: "OQ-001", question: "What auth feature supplies upstream bearer tokens?", blocks: "deploy", closure_plan: "Mock during build/test; replace before deploy." }

  gate_result:
    status: "pass"
    blockers: []
    carried_blockers:
      - { question_id: "OQ-001", mock_used: "mock bearer-token auth feature + neutral risk fixture" }
```
