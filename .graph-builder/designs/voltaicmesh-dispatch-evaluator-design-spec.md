# Graph Design Spec: VoltaicMesh Dispatch Evaluator

Phase: `/graph-design` output. Consumes the requirements brief; produces the architecture `/graph-build` lowers into commands.
Status: **gate PASS** — `/build` may begin. Deploy-only blockers carried.

Engine capabilities below are **source-verified** against
`system/minigraph-playground-engine/src/main/java/com/accenture/minigraph/skills/` (GraphJs, GraphMath,
GraphApiFetcher, GraphJoin) — not just docs.

```yaml
graph_design_spec:
  graph:
    name: "voltaicmesh-dispatch-evaluator"
    purpose: "Per demand event: validate input, fetch grid telemetry + candidate storage in parallel, fail-closed on bad/stale/absent data, then rank-and-allocate within transformer headroom and return an advisory dispatch recommendation with an explicit status."
    invocation: "HTTP, single demand-event JSON body (synchronous, request-scoped)."
    selected_shape: "mixed"   # validate (decision) -> fan-out-join -> decision-gate -> js-evaluator -> shape

  source_brief:
    requirements_artifact: ".graph-builder/briefs/voltaicmesh-dispatch-evaluator-requirements-brief.md"
    requirements_gate_status: "pass"
    requirements_carried_blockers:   # copied from brief; /design resolves build items, carries deploy items
      - { question_id: "OQ-001", item: "real SCADA/registry URLs + auth feature", phase: "deploy" }
      - { question_id: "OQ-002", item: "MAX_TELEMETRY_AGE value (seconds)", phase: "build->mocked" }
      - { question_id: "OQ-003", item: "exact response field paths", phase: "build->mocked" }
      - { question_id: "OQ-004", item: "secondary tiebreaks / threshold inclusivity", phase: "build->mocked" }
      - { question_id: "OQ-005", item: "latency budget + fetch timeouts + retry", phase: "deploy" }
      - { question_id: "OQ-006", item: "registry 5xx => DENIED_CRITICAL confirm", phase: "build->mocked" }
      - { question_id: "OQ-007", item: "deploy to extension-callable location", phase: "deploy" }

  # ---- D3: shape choice ----
  shape_rationale: >
    Not a single linear transform: FLOW-001 mandates parallel sourcing (fan-out + join), FLOW-002
    mandates a fail-closed decision gate before scoring, and FLOW-005 needs array sort + greedy
    cumulative allocation (only graph.js can express this). So the smallest faithful shape is
    a mixed pipeline: input-decision -> parallel fetchers -> join -> data-validity decision gate ->
    one graph.js evaluator -> single output mapper. Exactly one graph.js node is used (the allocator);
    everything else is mapper/math/fetcher/join, per "use JS only when math cannot express it."

  # ---- D4/D5: node inventory ----
  node_inventory:
    - alias: "root"
      type: "Root"
      skill: null
      responsibility: "entry point"
      reads: ["input.body.*"]
      writes: []
      upstream: []
      downstream: ["prepare-input"]
      satisfies: ["INV-001"]

    - alias: "prepare-input"
      type: "Mapper"
      skill: "graph.data.mapper"
      reason: "pure state movement + null-flag derivation; no compute/branch. Mapper is the lightest primitive and the only place :boolean(null=true) creates the flag (source-verified: the null selector must run in a mapper)."
      responsibility: "Copy required inputs into model.*; derive presence flags for required fields so validate-input can branch without an IF halting on an absent value."
      reads: ["input.body.event_id","input.body.target_substation_id","input.body.required_mw","input.body.target_duration_minutes","input.body.timestamp"]
      writes: ["model.event_id","model.target_substation_id","model.required_mw","model.target_duration_minutes","model.request_timestamp","model.flag_missing.*"]
      upstream: ["root"]
      downstream: ["validate-input"]
      design_properties:
        mapping:
          - "input.body.event_id -> model.event_id"
          - "input.body.target_substation_id -> model.target_substation_id"
          - "input.body.required_mw -> model.required_mw"
          - "input.body.target_duration_minutes -> model.target_duration_minutes"
          - "input.body.timestamp -> model.request_timestamp"
          - "input.body.required_mw:boolean(null=true) -> model.flag_missing.required_mw"
          - "input.body.target_duration_minutes:boolean(null=true) -> model.flag_missing.target_duration_minutes"
          - "input.body.target_substation_id:boolean(null=true) -> model.flag_missing.target_substation_id"
          - "input.body.event_id:boolean(null=true) -> model.flag_missing.event_id"
          - "input.body.timestamp:boolean(null=true) -> model.flag_missing.timestamp"
      satisfies: ["INV-002","MAP input_to_model","OUT-003 event echo (model.event_id)"]

    - alias: "validate-input"
      type: "Evaluator"
      skill: "graph.math"
      reason: "scalar/boolean branch only (presence flags + range checks). Math is rejected for mapping-only nodes but here it has real IF logic; JS would be overkill."
      responsibility: "Reject malformed requests (missing required field, or required_mw<=0, or target_duration_minutes<=0); else continue to parallel fetch."
      reads: ["model.flag_missing.*","model.required_mw","model.target_duration_minutes"]
      writes: ["model.reject_reason (on reject path)"]
      upstream: ["prepare-input"]
      downstream: ["fetch-scada","fetch-registry"]   # natural edges = parallel fan-out on continue
      design_properties:
        statement:
          - "IF: {model.flag_missing.required_mw} == true || {model.flag_missing.target_duration_minutes} == true || {model.flag_missing.target_substation_id} == true || {model.flag_missing.event_id} == true || {model.flag_missing.timestamp} == true  THEN: reject-output  ELSE: next"
          - "IF: {model.required_mw} <= 0 || {model.target_duration_minutes} <= 0  THEN: reject-output  ELSE: next"
      satisfies: ["INV-002","TEST T-02"]

    - alias: "reject-output"
      type: "Decision"
      skill: "graph.data.mapper"
      reason: "terminal client-error envelope; pure mapping."
      responsibility: "Emit a 400-class error envelope distinct from the dispatch enum (see D-07-design)."
      reads: ["model.event_id"]
      writes: ["output.body.error","output.body.message","output.header.status"]
      upstream: ["validate-input (jump)"]
      downstream: ["end"]
      design_properties:
        mapping:
          - "text(invalid_request) -> output.body.error"
          - "text(missing or non-positive required field) -> output.body.message"
          - "f:defaultValue(model.event_id, text(unknown)) -> output.body.event_id"
          - "int(400) -> output.header.status"
      satisfies: ["INV-002"]

    - alias: "fetch-scada"
      type: "Fetcher"
      skill: "graph.api.fetcher"
      reason: "provider-backed HTTP read."
      responsibility: "Fetch live grid telemetry for the substation; on transport/5xx failure jump to deny-critical (exception)."
      reads: ["model.target_substation_id"]
      writes: ["model.grid.frequency_hz","model.grid.current_load_mw","model.grid.operating_max_capacity_mw","model.grid.telemetry_timestamp"]
      upstream: ["validate-input"]
      downstream: ["join-context"]
      design_properties:
        dictionary: ["scada-dict"]
        input: ["model.target_substation_id -> substation_id"]
        output:
          - "result.frequency_hz -> model.grid.frequency_hz"
          - "result.current_load_mw -> model.grid.current_load_mw"
          - "result.operating_max_capacity_mw -> model.grid.operating_max_capacity_mw"
          - "result.telemetry_timestamp -> model.grid.telemetry_timestamp"
        exception: "deny-critical"
      satisfies: ["SRC-001","FLOW-001","FAIL-001 (transport)","FAIL-006"]

    - alias: "scada-dict"
      type: "Dictionary"
      skill: null
      reason: "maps the SCADA provider response into result.* fields; non-traversing support node."
      responsibility: "Declare provider + response field extraction for telemetry."
      reads: ["response.*"]
      writes: ["result.frequency_hz","result.current_load_mw","result.operating_max_capacity_mw","result.telemetry_timestamp"]
      upstream: []
      downstream: ["scada-provider (provider edge)"]
      design_properties:
        provider: "scada-provider"
        input: ["substation_id"]
        output:
          - "response.<frequency_path> -> result.frequency_hz"
          - "response.<current_load_path> -> result.current_load_mw"
          - "response.<max_cap_path> -> result.operating_max_capacity_mw"
          - "response.<telemetry_ts_path> -> result.telemetry_timestamp"
      satisfies: ["SRC-001"]
      mock_note: "exact response paths mocked (OQ-003)"

    - alias: "scada-provider"
      type: "Provider"
      skill: null
      reason: "HTTP endpoint definition."
      responsibility: "Declare SCADA telemetry endpoint."
      reads: []
      writes: []
      upstream: ["scada-dict"]
      downstream: []
      design_properties:
        url: "${scada-host}/scada/substations/{substation_id}/telemetry"
        method: "GET"
        feature: ["<auth-feature> (mocked, OQ-001)"]
        input: ["text(application/json) -> header.accept","substation_id -> path_parameter.substation_id"]
      satisfies: ["SRC-001"]
      mock_note: "URL + auth feature mocked (OQ-001, deploy blocker)"

    - alias: "fetch-registry"
      type: "Fetcher"
      skill: "graph.api.fetcher"
      reason: "provider-backed HTTP read returning an asset array."
      responsibility: "Fetch candidate storage assets for the substation; on transport/5xx jump to deny-critical (exception)."
      reads: ["model.target_substation_id"]
      writes: ["model.candidates"]
      upstream: ["validate-input"]
      downstream: ["join-context"]
      design_properties:
        dictionary: ["registry-dict"]
        input: ["model.target_substation_id -> substation_id"]
        output: ["result.candidates -> model.candidates"]   # source-verified: array maps whole into one model path
        exception: "deny-critical"
      satisfies: ["SRC-002","FLOW-001","FAIL-002 (transport)","FAIL-006"]

    - alias: "registry-dict"
      type: "Dictionary"
      skill: null
      reason: "extracts the candidate array from the registry response."
      responsibility: "Declare provider + array extraction."
      reads: ["response.*"]
      writes: ["result.candidates"]
      upstream: []
      downstream: ["registry-provider (provider edge)"]
      design_properties:
        provider: "registry-provider"
        input: ["substation_id"]
        output: ["response.<candidates_array_path> -> result.candidates"]
      satisfies: ["SRC-002"]
      mock_note: "exact array path + per-asset field names mocked (OQ-003)"

    - alias: "registry-provider"
      type: "Provider"
      skill: null
      reason: "HTTP endpoint definition."
      responsibility: "Declare market registry endpoint."
      reads: []
      writes: []
      upstream: ["registry-dict"]
      downstream: []
      design_properties:
        url: "${registry-host}/market/candidates"
        method: "GET"
        feature: ["<auth-feature> (mocked, OQ-001)"]
        input: ["substation_id -> query.substation","text(storage) -> query.type"]
      satisfies: ["SRC-002"]
      mock_note: "URL + auth mocked (OQ-001, deploy blocker)"

    - alias: "join-context"
      type: "Join"
      skill: "graph.join"
      reason: "fan-in: scoring must not start until BOTH fetches resolve on the success path."
      responsibility: "Synchronize the two fetchers; on a fetcher-exception path it never fires (returns .sink) and the graph completes via deny-critical."
      reads: []
      writes: []
      upstream: ["fetch-scada","fetch-registry"]
      downstream: ["validate-context"]
      satisfies: ["FLOW-001","FLOW-002b"]

    - alias: "validate-context"
      type: "Evaluator"
      skill: "graph.math"
      reason: "post-join data-validity gate: all checks are scalar/length. Staleness is arithmetic; emptiness uses the source-verified :length suffix; headroom is subtraction. No array transform => math, not JS."
      responsibility: "Fail-closed on stale telemetry; route empty candidate list to no-candidates; compute headroom; else continue to evaluate."
      reads: ["model.request_timestamp","model.grid.telemetry_timestamp","model.grid.operating_max_capacity_mw","model.grid.current_load_mw","model.candidates"]
      writes: ["model.headroom_mw","model.candidate_count"]
      upstream: ["join-context"]
      downstream: ["evaluate-dispatch"]
      design_properties:
        statement:
          - "IF: ({model.request_timestamp} - {model.grid.telemetry_timestamp}) > MAX_TELEMETRY_AGE  THEN: deny-critical  ELSE: next   # all seconds"
          - "MAPPING: model.candidates:length -> model.candidate_count"
          - "COMPUTE: headroom_mw -> {model.grid.operating_max_capacity_mw} - {model.grid.current_load_mw}"
          - "MAPPING: validate-context.result.headroom_mw -> model.headroom_mw"
          - "IF: {model.candidate_count} == 0  THEN: no-candidates  ELSE: next"
      satisfies: ["FLOW-002 (stale path)","FAIL-001 (stale)","FAIL-003 (routing)","MAP-001 (headroom)"]
      build_note: "MAX_TELEMETRY_AGE seeded as a constant/model value at build (mock 5s, OQ-002). Stale IF placed FIRST so headroom is not computed on the deny path."

    - alias: "deny-critical"
      type: "Decision"
      skill: "graph.data.mapper"
      reason: "terminal denial state; pure mapping. Reached from 3 jump sources (scada exception, registry exception, stale IF)."
      responsibility: "Set clean DISPATCH_DENIED_CRITICAL state (overwrites any fetcher error per FAIL-006) and converge to shape-output."
      reads: []
      writes: ["model.decision_status","model.binding_constraint","model.total_allocated_mw","model.dispatches"]
      upstream: ["fetch-scada (exception)","fetch-registry (exception)","validate-context (jump)"]
      downstream: ["shape-output"]
      design_properties:
        mapping:
          - "text(DISPATCH_DENIED_CRITICAL) -> model.decision_status"
          - "text(NONE) -> model.binding_constraint"
          - "double(0.0) -> model.total_allocated_mw"
          - "model.none -> model.dispatches"   # null source => target skipped; shape-output defaults to []
      satisfies: ["FAIL-001","FAIL-002","FAIL-006","OUT-004"]

    - alias: "no-candidates"
      type: "Decision"
      skill: "graph.data.mapper"
      reason: "terminal no-candidates state; pure mapping."
      responsibility: "Set NO_CANDIDATES_AVAILABLE and converge to shape-output (headroom already computed, so it remains in the body)."
      reads: []
      writes: ["model.decision_status","model.binding_constraint","model.total_allocated_mw","model.dispatches"]
      upstream: ["validate-context (jump)"]
      downstream: ["shape-output"]
      design_properties:
        mapping:
          - "text(NO_CANDIDATES_AVAILABLE) -> model.decision_status"
          - "text(SUPPLY) -> model.binding_constraint"
          - "double(0.0) -> model.total_allocated_mw"
      satisfies: ["FAIL-003","OUT-004"]

    - alias: "evaluate-dispatch"
      type: "Evaluator"
      skill: "graph.js"
      reason: "THE load-bearing JS node. Mode determination + eligibility filter + mode-dependent sort + greedy cumulative allocation clamped to headroom + status/binding derivation are array operations only GraalVM JS expresses. Math/mapper cannot sort or reduce arrays (source-verified: GraphMath is scalar-only; GraphJs is GraalVM Polyglot)."
      responsibility: "Compute grid_mode, eligible set, ranked set, dispatches, totals, shortfall, binding_constraint, and decision_status for the success path."
      reads: ["model.candidates","model.grid.frequency_hz","model.required_mw","model.target_duration_minutes","model.headroom_mw"]
      writes: ["model.grid_mode","model.eligible","model.dispatches","model.total_allocated_mw","model.shortfall_mw","model.binding_constraint","model.decision_status"]
      upstream: ["validate-context"]
      downstream: ["shape-output"]
      design_properties:
        statement:
          - "COMPUTE: grid_mode -> ({model.grid.frequency_hz} >= 59.9 && {model.grid.frequency_hz} <= 60.1) ? 'NORMAL' : 'DISTRESSED'"
          - "COMPUTE: eligible -> {model.candidates}.filter(a => a.sustain_window_minutes >= {model.target_duration_minutes} && (a.sla_tier !== 'CRITICAL_RESERVE' || ({model.grid.frequency_hz} < 59.9 || {model.grid.frequency_hz} > 60.1)))"
          - "COMPUTE: ranked -> [...{evaluate-dispatch.result.eligible}].sort((x,y) => (({model.grid.frequency_hz} >= 59.9 && {model.grid.frequency_hz} <= 60.1) ? (x.price - y.price) : (y.ramp_rate_mw_per_s - x.ramp_rate_mw_per_s)) || (x.storage_node_id < y.storage_node_id ? -1 : 1))"
          - "COMPUTE: allocation -> (function(){var cap=Math.min({model.required_mw},{model.headroom_mw});var t=0;var d=[];for(var i=0;i<{evaluate-dispatch.result.ranked}.length;i++){if(t>=cap)break;var a={evaluate-dispatch.result.ranked}[i];var take=Math.min(a.dischargeable_mw,cap-t);if(take<=0)break;d.push({storage_node_id:a.storage_node_id,allocated_mw:take});t+=take;}var clamped=({model.required_mw}>{model.headroom_mw});var elig=({evaluate-dispatch.result.eligible}).reduce((s,a)=>s+a.dischargeable_mw,0);var supplyShort=(elig<{model.required_mw});var status,binding;if(t>= {model.required_mw}){status='FULLY_ALLOCATED';binding='NONE';}else if(clamped&&supplyShort){status='PARTIALLY_ALLOCATED_SAFETY';binding='BOTH';}else if(clamped){status='PARTIALLY_ALLOCATED_SAFETY';binding='HEADROOM';}else{status='PARTIALLY_ALLOCATED_SHORTAGE';binding='SUPPLY';}return {dispatches:d,total:t,shortfall:Math.max(0,{model.required_mw}-t),status:status,binding:binding};})()"
          - "MAPPING: evaluate-dispatch.result.grid_mode -> model.grid_mode"
          - "MAPPING: evaluate-dispatch.result.eligible -> model.eligible"
          - "MAPPING: evaluate-dispatch.result.allocation.dispatches -> model.dispatches"
          - "MAPPING: evaluate-dispatch.result.allocation.total -> model.total_allocated_mw"
          - "MAPPING: evaluate-dispatch.result.allocation.shortfall -> model.shortfall_mw"
          - "MAPPING: evaluate-dispatch.result.allocation.binding -> model.binding_constraint"
          - "MAPPING: evaluate-dispatch.result.allocation.status -> model.decision_status"
      satisfies: ["FLOW-003","FLOW-004","FLOW-005","FLOW-006","FAIL-004","FAIL-005","OUT-005","MAP-002","D-02","D-07"]
      build_note: "JS shown is design intent, not final syntax — the exact GraalVM expression and how state paths interpolate into the script are /build's job (verify the {model.*} substitution form against GraphJs). This greedy-allocation COMPUTE is the single highest-risk build artifact; T-01/T-06/T-07/T-09 must prove it."

    - alias: "shape-output"
      type: "Decision"
      skill: "graph.data.mapper"
      reason: "single owner of the success/denial output.body contract; pure mapping. All non-reject terminal paths converge here so output.body has exactly one writer."
      responsibility: "Map model.* decision state into the output contract; default dispatches to [] when absent."
      reads: ["model.decision_status","model.binding_constraint","model.event_id","model.target_substation_id","model.total_allocated_mw","model.dispatches","model.required_mw","model.headroom_mw","model.shortfall_mw"]
      writes: ["output.body.*"]
      upstream: ["evaluate-dispatch","deny-critical","no-candidates"]
      downstream: ["end"]
      design_properties:
        mapping:
          - "model.decision_status -> output.body.decision_status"
          - "model.binding_constraint -> output.body.binding_constraint"
          - "model.event_id -> output.body.event_id"
          - "model.target_substation_id -> output.body.target_substation_id"
          - "f:defaultValue(model.total_allocated_mw, double(0.0)) -> output.body.total_allocated_mw"
          - "f:defaultValue(model.dispatches, model.empty_list) -> output.body.dispatches"
          - "model.required_mw -> output.body.metrics.requested_mw"
          - "model.headroom_mw -> output.body.metrics.headroom_mw"   # silently skipped if absent (denial) => matches OUT-004
          - "model.shortfall_mw -> output.body.metrics.shortfall_mw"
      satisfies: ["OUT-001","OUT-002","OUT-004","OUT-005","MAP model_to_output"]

    - alias: "end"
      type: "End"
      skill: null
      responsibility: "complete graph execution; output.body becomes the response."
      reads: []
      writes: []
      upstream: ["shape-output","reject-output"]
      downstream: []
      satisfies: []

  # ---- D8: edges & control flow ----
  edge_plan:
    natural_edges:
      - { from: "root", to: "prepare-input", purpose: "ingest" }
      - { from: "prepare-input", to: "validate-input", purpose: "validate" }
      - { from: "validate-input", to: "fetch-scada", purpose: "parallel fan-out (continue branch)" }
      - { from: "validate-input", to: "fetch-registry", purpose: "parallel fan-out (continue branch)" }
      - { from: "fetch-scada", to: "join-context", purpose: "fan-in" }
      - { from: "fetch-registry", to: "join-context", purpose: "fan-in" }
      - { from: "join-context", to: "validate-context", purpose: "post-join validity gate" }
      - { from: "validate-context", to: "evaluate-dispatch", purpose: "success path (ELSE next)" }
      - { from: "evaluate-dispatch", to: "shape-output", purpose: "shape result" }
      - { from: "deny-critical", to: "shape-output", purpose: "converge denial to single output owner" }
      - { from: "no-candidates", to: "shape-output", purpose: "converge no-candidates to single output owner" }
      - { from: "shape-output", to: "end", purpose: "respond" }
      - { from: "reject-output", to: "end", purpose: "respond (client error)" }
      - { from: "scada-dict", to: "scada-provider", purpose: "provider edge" }
      - { from: "fetch-scada", to: "scada-dict", purpose: "dictionary edge" }
      - { from: "registry-dict", to: "registry-provider", purpose: "provider edge" }
      - { from: "fetch-registry", to: "registry-dict", purpose: "dictionary edge" }
    jumps:
      - { from: "validate-input", condition_or_statement: "missing field or non-positive required_mw/duration", target: "reject-output" }
      - { from: "validate-context", condition_or_statement: "(request_ts - telemetry_ts) > MAX_TELEMETRY_AGE", target: "deny-critical" }
      - { from: "validate-context", condition_or_statement: "candidate_count == 0", target: "no-candidates" }
    exceptions:
      - { from: "fetch-scada", target: "deny-critical" }
      - { from: "fetch-registry", target: "deny-critical" }
    sinks:
      - { node: "join-context", reason: "When one fetcher takes its exception path to deny-critical, that upstream never completes; the join returns .sink for the surviving branch and the graph completes via deny-critical -> shape-output -> end (source-verified: join returns SINK on incomplete upstream, does not hang)." }
    joins:
      - { node: "join-context", required_upstream: ["fetch-scada","fetch-registry"] }

  # ---- D6: state & mapping ownership ----
  state_plan:
    input_paths: ["input.body.event_id","input.body.target_substation_id","input.body.required_mw","input.body.target_duration_minutes","input.body.timestamp"]
    model_paths:
      - { path: "model.event_id", owner: "prepare-input" }
      - { path: "model.target_substation_id", owner: "prepare-input" }
      - { path: "model.required_mw", owner: "prepare-input" }
      - { path: "model.target_duration_minutes", owner: "prepare-input" }
      - { path: "model.request_timestamp", owner: "prepare-input" }
      - { path: "model.flag_missing.*", owner: "prepare-input" }
      - { path: "model.grid.frequency_hz", owner: "fetch-scada" }
      - { path: "model.grid.current_load_mw", owner: "fetch-scada" }
      - { path: "model.grid.operating_max_capacity_mw", owner: "fetch-scada" }
      - { path: "model.grid.telemetry_timestamp", owner: "fetch-scada" }
      - { path: "model.candidates", owner: "fetch-registry" }
      - { path: "model.candidate_count", owner: "validate-context" }
      - { path: "model.headroom_mw", owner: "validate-context" }
      - { path: "model.grid_mode", owner: "evaluate-dispatch" }
      - { path: "model.eligible", owner: "evaluate-dispatch" }
      - { path: "model.decision_status", owner: "evaluate-dispatch | deny-critical | no-candidates (mutually exclusive by topology — controlled overwrite)" }
      - { path: "model.binding_constraint", owner: "evaluate-dispatch | deny-critical | no-candidates (mutually exclusive)" }
      - { path: "model.dispatches", owner: "evaluate-dispatch | deny-critical (mutually exclusive)" }
      - { path: "model.total_allocated_mw", owner: "evaluate-dispatch | deny-critical | no-candidates (mutually exclusive)" }
      - { path: "model.shortfall_mw", owner: "evaluate-dispatch" }
    node_result_paths: ["validate-context.result.headroom_mw","evaluate-dispatch.result.grid_mode","evaluate-dispatch.result.eligible","evaluate-dispatch.result.ranked","evaluate-dispatch.result.allocation"]
    output_paths:
      - { path: "output.body.decision_status", owner: "shape-output" }
      - { path: "output.body.binding_constraint", owner: "shape-output" }
      - { path: "output.body.event_id", owner: "shape-output" }
      - { path: "output.body.target_substation_id", owner: "shape-output" }
      - { path: "output.body.total_allocated_mw", owner: "shape-output" }
      - { path: "output.body.dispatches", owner: "shape-output" }
      - { path: "output.body.metrics.requested_mw", owner: "shape-output" }
      - { path: "output.body.metrics.headroom_mw", owner: "shape-output" }
      - { path: "output.body.metrics.shortfall_mw", owner: "shape-output" }
      - { path: "output.body.error / message / header.status", owner: "reject-output (separate client-error envelope)" }
    constants:
      - "MAX_TELEMETRY_AGE (seconds; mock 5) — seeded at build"
      - "FREQ band 59.9 / 60.1 — literals in validate/evaluate"
      - "CRITICAL_RESERVE — sla_tier literal in evaluate filter"
      - "model.empty_list = [] (seeded constant for dispatches default)"
    mapping_ownership:
      - { node: "prepare-input", mappings: ["input->model", "null flags"] }
      - { node: "validate-context", mappings: ["candidates:length->count", "headroom compute+map"] }
      - { node: "evaluate-dispatch", mappings: ["result.* -> model.*"] }
      - { node: "shape-output", mappings: ["model.* -> output.body.*"] }
      - { node: "deny-critical / no-candidates / reject-output", mappings: ["terminal state set"] }

  # ---- D7: sources ----
  source_plan:
    api_fetchers: ["fetch-scada","fetch-registry"]
    dictionaries: ["scada-dict","registry-dict"]
    providers: ["scada-provider","registry-provider"]
    extensions: []
    flows: []
    mocks:
      - real_dependency: "SCADA telemetry API"
        contract: "GET by substation_id -> {frequency_hz, current_load_mw, operating_max_capacity_mw, telemetry_timestamp(epoch s)}"
        mock_strategy: "build against a local mock provider/fixture; field paths assumed per response_contract"
        produces_paths: ["model.grid.*"]
        tests_using: ["T-01","T-04","T-05","T-07","T-08","T-09"]
        deploy_blocker: "OQ-001 real URL+auth; OQ-003 real field paths"
      - real_dependency: "Market registry API"
        contract: "GET by substation -> [{storage_node_id, dischargeable_mw, sustain_window_minutes, sla_tier, price, ramp_rate_mw_per_s}]"
        mock_strategy: "local mock returning a candidate array fixture (and an empty array for T-03)"
        produces_paths: ["model.candidates"]
        tests_using: ["T-01","T-03","T-06","T-08","T-09"]
        deploy_blocker: "OQ-001 real URL+auth; OQ-003 real field paths"

  # ---- D9: failure & fallback ----
  control_flow_plan:
    decisions:
      - "validate-input: reject vs continue"
      - "validate-context: stale->deny / empty->no-candidates / else->evaluate"
      - "evaluate-dispatch: status & binding_constraint selection (FULLY/SAFETY/SHORTAGE; HEADROOM/SUPPLY/BOTH/NONE)"
    loops_or_resets: []   # no loops; greedy allocation is a bounded JS for-loop INSIDE one COMPUTE, not a graph loop
    concurrency:
      - "fetch-scada || fetch-registry via dual natural edges from validate-input; join-context fan-in. No for_each."
    completion: "end reached via shape-output (success/denial/no-candidates) or reject-output (invalid input)."

  failure_plan:
    terminal_errors:
      - { trigger: "invalid/missing input", handler: "reject-output", result: "400 envelope {error,message}" }
    handled_errors:
      - { source: "fetch-scada", mechanism: "exception=deny-critical", result: "DISPATCH_DENIED_CRITICAL, no error leak (breakOnException=false, source-verified)" }
      - { source: "fetch-registry", mechanism: "exception=deny-critical", result: "DISPATCH_DENIED_CRITICAL, no error leak" }
      - { source: "stale telemetry", mechanism: "validate-context IF -> deny-critical", result: "DISPATCH_DENIED_CRITICAL" }
      - { source: "empty candidate list", mechanism: "validate-context IF -> no-candidates", result: "NO_CANDIDATES_AVAILABLE (headroom retained)" }
    fallback_outputs:
      - "headroom exceeded -> clamp in evaluate-dispatch -> PARTIALLY_ALLOCATED_SAFETY (binding HEADROOM, or BOTH if also supply-short)"
      - "eligible capacity < required -> PARTIALLY_ALLOCATED_SHORTAGE (binding SUPPLY)"

  # ---- D10: test handoff ----
  test_handoff:
    scenarios:
      - { id: "T-01", name: "happy path full", inspect: ["output.body.decision_status==FULLY_ALLOCATED","output.body.total_allocated_mw","model.grid_mode==NORMAL"] }
      - { id: "T-02", name: "invalid input", inspect: ["output.body.error==invalid_request","output.header.status==400","seen: reject-output reached, no fetch"] }
      - { id: "T-03", name: "empty registry", inspect: ["output.body.decision_status==NO_CANDIDATES_AVAILABLE","output.body.dispatches==[]","model.candidate_count==0"] }
      - { id: "T-04", name: "SCADA 5xx", inspect: ["output.body.decision_status==DISPATCH_DENIED_CRITICAL","NO upstream error body in output.body (FAIL-006)","seen: fetch-scada->deny-critical, join-context sinks"] }
      - { id: "T-05", name: "stale telemetry", inspect: ["output.body.decision_status==DISPATCH_DENIED_CRITICAL","model.headroom_mw absent on this path"] }
      - { id: "T-06", name: "supply shortage", inspect: ["decision_status==PARTIALLY_ALLOCATED_SHORTAGE","binding_constraint==SUPPLY","metrics.shortfall_mw>0"] }
      - { id: "T-07", name: "headroom cap", inspect: ["decision_status==PARTIALLY_ALLOCATED_SAFETY","binding_constraint==HEADROOM","total_allocated_mw==headroom_mw"] }
      - { id: "T-08", name: "distressed mode", inspect: ["model.grid_mode==DISTRESSED","ranking by ramp_rate desc","CRITICAL_RESERVE asset present in eligible"] }
      - { id: "T-09", name: "supply below headroom (DEF-1)", inspect: ["decision_status==PARTIALLY_ALLOCATED_SHORTAGE","binding_constraint==SUPPLY (supply gave out below the headroom cap)"] }
      - { id: "T-10", name: "genuine both-bind", inspect: ["decision_status==PARTIALLY_ALLOCATED_SAFETY","binding_constraint==BOTH (fill reached headroom cap AND supply<demand)"] }
    inspection_points: ["output.body","output.header.status","model.grid_mode","model.eligible","model.candidate_count","model.headroom_mw","model.dispatches","model.binding_constraint","model.decision_status"]
    mock_data:
      - "SCADA fixture: nominal (60.0Hz), distressed (<59.9Hz), high-load (low headroom), 5xx, stale-timestamp variants"
      - "Registry fixture: full-coverage set, shortage set, empty [], set including a CRITICAL_RESERVE asset"

  build_handoff:
    command_sensitive_notes:
      - "HIGHEST RISK: the evaluate-dispatch greedy-allocation COMPUTE. Verify how GraphJs interpolates {model.*} paths into the JS source string and that a self-invoking function returning an object maps cleanly via result.allocation.*. If interpolation of nested paths is awkward, split into smaller COMPUTEs."
      - "validate-context relies on the :length suffix in a MAPPING (source-verified via TypeConversionUtils) — confirm exact selector form during build."
      - "prepare-input uses :boolean(null=true) which must run in a mapper (source-verified). Do not move presence flags into graph.math."
      - "Both fetchers MUST carry exception=deny-critical or the upstream error body leaks into output.body (source-verified breakOnException)."
      - "Seed model.empty_list ([]) and MAX_TELEMETRY_AGE at instantiate; there is no number() constant — use double()/float()/int()."
      - "Minimal fetcher wiring (Provider+Dictionary+Fetcher + one provider edge) is sufficient per syntax doc; the dictionary island container is optional."
    source_help_mismatches:
      - "Help says fetcher output[] is required; source allows empty output when another node consumes {fetcher}.result. Here we DO map output, so no issue."
    syntax_reference: ".graph-builder/minigraph-syntax.md"

  requirement_traceability:
    - { requirement_id: "INV-001", design_elements: ["root","prepare-input"], notes: "HTTP entry" }
    - { requirement_id: "INV-002", design_elements: ["prepare-input","validate-input","reject-output"], notes: "presence flags + range branch" }
    - { requirement_id: "OUT-001", design_elements: ["shape-output"], notes: "5-state enum written by terminal branches, emitted by shape-output" }
    - { requirement_id: "OUT-002", design_elements: ["shape-output","evaluate-dispatch"], notes: "" }
    - { requirement_id: "OUT-003", design_elements: ["prepare-input","shape-output"], notes: "advisory/read-only: no write/extension nodes exist in graph" }
    - { requirement_id: "OUT-004", design_elements: ["deny-critical","no-candidates","shape-output"], notes: "empty dispatches + silent-skip headroom on denial" }
    - { requirement_id: "OUT-005", design_elements: ["evaluate-dispatch","shape-output"], notes: "binding_constraint" }
    - { requirement_id: "SRC-001", design_elements: ["fetch-scada","scada-dict","scada-provider"], notes: "mocked endpoint" }
    - { requirement_id: "SRC-002", design_elements: ["fetch-registry","registry-dict","registry-provider"], notes: "mocked endpoint" }
    - { requirement_id: "MAP-001", design_elements: ["validate-context"], notes: "headroom" }
    - { requirement_id: "MAP-002", design_elements: ["evaluate-dispatch"], notes: "shortfall" }
    - { requirement_id: "FLOW-001", design_elements: ["validate-input","fetch-scada","fetch-registry","join-context"], notes: "parallel + join" }
    - { requirement_id: "FLOW-002", design_elements: ["fetch-scada exception","fetch-registry exception","join-context","validate-context"], notes: "transport short-circuit (a) + post-join validity (b)" }
    - { requirement_id: "FLOW-003", design_elements: ["evaluate-dispatch"], notes: "eligibility filter" }
    - { requirement_id: "FLOW-004", design_elements: ["evaluate-dispatch"], notes: "grid mode" }
    - { requirement_id: "FLOW-005", design_elements: ["evaluate-dispatch"], notes: "rank + greedy fill" }
    - { requirement_id: "FLOW-006", design_elements: ["evaluate-dispatch"], notes: "status precedence + binding" }
    - { requirement_id: "FAIL-001", design_elements: ["fetch-scada exception","validate-context","deny-critical"], notes: "" }
    - { requirement_id: "FAIL-002", design_elements: ["fetch-registry exception","deny-critical"], notes: "" }
    - { requirement_id: "FAIL-003", design_elements: ["validate-context","no-candidates"], notes: "" }
    - { requirement_id: "FAIL-004", design_elements: ["evaluate-dispatch"], notes: "headroom clamp" }
    - { requirement_id: "FAIL-005", design_elements: ["evaluate-dispatch"], notes: "shortage" }
    - { requirement_id: "FAIL-006", design_elements: ["fetch-scada exception","fetch-registry exception","deny-critical","shape-output"], notes: "no error leak; clean body" }
    - { requirement_id: "NFR-001", design_elements: ["fetch-scada","fetch-registry","join-context"], notes: "parallel fetch; timeout/latency = deploy (OQ-005)" }
    - { requirement_id: "TEST-T01..T09", design_elements: ["test_handoff"], notes: "all 9 scenarios mapped to inspection points" }

  decisions:
    - { id: "DD-01", decision: "Exactly one graph.js node (evaluate-dispatch); validate-context is graph.math via :length + arithmetic.", source_category: "source-verified", source_note: "GraphMath scalar-only + :length suffix; GraphJs GraalVM. Minimizes the heavy primitive." }
    - { id: "DD-02", decision: "Compute headroom in validate-context (after staleness passes) so no-candidates path still reports headroom, and the deny path omits it.", source_category: "tradeoff", source_note: "Satisfies OUT-004 absent-on-failure cleanly; single owner for model.headroom_mw." }
    - { id: "DD-03", decision: "All non-reject terminals converge on shape-output (single output.body owner); decision_status/dispatches written by mutually-exclusive branch nodes.", source_category: "tradeoff", source_note: "Keeps one writer per output path; documented controlled overwrite." }
    - { id: "DD-04", decision: "Input rejection returns a separate {error,message} envelope with output.header.status=400, NOT a 6th dispatch enum value.", source_category: "user-answer", source_note: "LOCKED by user. Keeps the dispatch enum clean; reject-output owns this envelope. OQ-D01 closed." }
    - { id: "DD-05", decision: "Fail-fast fan-in: a fetcher exception jumps to deny-critical and the join sinks the surviving branch; graph completes via the denial terminal.", source_category: "source-verified", source_note: "GraphJoin returns SINK on incomplete upstream; does not hang." }
    - { id: "DD-06", decision: "No for_each; the registry returns the whole array in one call and per-asset logic runs inside the JS node.", source_category: "tradeoff", source_note: "Fewer moving parts; for_each is for repeated HTTP/extension calls, not in-memory iteration." }

  open_questions:
    - { id: "OQ-D01", question: "CLOSED — input-rejection shape locked by user: {error,message,event_id}+output.header.status=400 (DD-04).", blocks: "none", closure_plan: "Resolved." }
    - { id: "OQ-D02", question: "GraphJs interpolation of {model.*} paths into a multi-statement greedy-allocation script — exact form and whether nested-object results map via result.allocation.*", blocks: "build", closure_plan: "Resolve empirically in /build with a tiny COMPUTE probe before writing the full allocator; split COMPUTEs if needed. Behavior is defined; only syntax is open." }

  gate_result:
    status: "pass"
    blockers: []
    carried_blockers:
      - { question_id: "OQ-001", item: "real SCADA + registry URLs and auth feature", mock_used: "local mock providers / ${host} placeholders", deploy_plan: "bind real endpoints + auth before production" }
      - { question_id: "OQ-002", item: "MAX_TELEMETRY_AGE real value", mock_used: "5 seconds", deploy_plan: "confirm with grid ops" }
      - { question_id: "OQ-003", item: "exact response field paths", mock_used: "assumed paths per response_contract", deploy_plan: "bind to real API schema in dictionaries" }
      - { question_id: "OQ-005", item: "latency budget + fetch timeouts + retry policy", mock_used: "none (no retry, default timeouts)", deploy_plan: "set timeouts to SLA; decide retry" }
      - { question_id: "OQ-007", item: "deploy to extension-callable location", mock_used: "session build only", deploy_plan: "deploy JSON to location.graph.deployed (classpath needs rebuild)" }
```
