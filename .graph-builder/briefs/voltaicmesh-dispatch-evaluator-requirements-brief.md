# Design-Ready Brief: VoltaicMesh Dispatch Evaluator

Phase: `/graph-requirements` output. Consumed by `/graph-design`.
Status: **gate PASS (re-issued)** — `/design` may begin. Build/deploy blockers carried as mocks.

> Revision history: an independent adversarial review (per `docs/adversarial-review-checklist.md`)
> caught that the first PASS mis-classified four requirements/design-level defects as build
> mocks. K1 (telemetry-staleness unit incoherence), K2 (SAFETY/SHORTAGE state collapse),
> K3 (fetcher error-leak vs. engine default), and K4 (fetch-failure routing ambiguity) are now
> resolved in-line; this is the corrected re-issue. The separation-of-duties control worked as
> designed (independent reviewer caught the author's self-pass), so no Incident Log entry is
> warranted — that protocol is for sycophancy that slips *past* review, which did not happen here.

> Scope reminder: this graph is the **per-event decision logic** for Project VoltaicMesh, not
> the live control plane. It is invoked by the event bus, runs once, returns an advisory
> recommendation, and dies. The autonomous control loop, the self-rewriting topology, the
> digital twin, the predictive blast-radius inference, and physical hardware actuation are all
> OUT of scope (owned by other components).

```yaml
graph:
  name: "voltaicmesh-dispatch-evaluator"
  purpose: "Given one high-demand event at a substation, source current grid telemetry and eligible storage assets, then return an advisory ranked dispatch allocation mapped to an explicit fulfillment status."
  workflow_category: "routing"   # read-only/advisory decision; no writes or actuation

requirements:
  - id: "INV-001"
    statement: "Invoked synchronously by the grid event bus over HTTP with a single demand-event payload."
    source_category: "user-answer"
    source_note: "User: 'external event bus says: Node X has a high demand event, tell me what to do right now.'"
  - id: "INV-002"
    statement: "Required inputs: event_id, target_substation_id, required_mw (>0 float), target_duration_minutes (>0 int), timestamp (epoch SECONDS). Missing or invalid required input is rejected."
    source_category: "user-answer"
    source_note: "User locked input payload contract; target_duration_minutes made required."
  - id: "OUT-001"
    statement: "Caller-facing decision_status is one of five explicit states: FULLY_ALLOCATED, PARTIALLY_ALLOCATED_SHORTAGE, PARTIALLY_ALLOCATED_SAFETY, NO_CANDIDATES_AVAILABLE, DISPATCH_DENIED_CRITICAL."
    source_category: "user-answer"
    source_note: "User enum, expanded to 5 states."
  - id: "OUT-002"
    statement: "Successful body returns total_allocated_mw, dispatches[] of {storage_node_id, allocated_mw}, and metrics {requested_mw, headroom_mw, shortfall_mw}."
    source_category: "user-answer"
    source_note: "User locked output payload contract."
  - id: "OUT-003"
    statement: "The graph is advisory and read-only: it performs no writes, switches, or actuation; the caller actuates."
    source_category: "user-answer"
    source_note: "Confirmed: 'advisory, read-only ranking and allocation strategy.'"
  - id: "OUT-004"
    statement: "DISPATCH_DENIED_CRITICAL and NO_CANDIDATES_AVAILABLE return empty dispatches[] and total_allocated_mw=0; metrics may be partially absent (headroom unknown when telemetry failed)."
    source_category: "assumption"
    source_note: "Derived from fail-closed + empty-list calls; veto-able."
  - id: "SRC-001"
    statement: "SCADA telemetry source (HTTP GET per substation) returns grid_frequency_hz, current_load_mw, operating_max_capacity_mw, and telemetry_timestamp."
    source_category: "user-answer"
    source_note: "User: SCADA returns dynamic operating_max_capacity_mw (ambient-derated) and current_load_mw; timestamp added for staleness check."
  - id: "SRC-002"
    statement: "Market registry source (HTTP GET per substation) returns the candidate storage assets connected to that substation as an array of {storage_node_id, dischargeable_mw, sustain_window_minutes, sla_tier, price, ramp_rate_mw_per_s}."
    source_category: "user-answer"
    source_note: "Registry returns instantaneous dischargeable power + sustain window; attribute set extended to carry ramp_rate for distressed-mode scoring."
  - id: "MAP-001"
    statement: "Headroom is derived: headroom_mw = operating_max_capacity_mw - current_load_mw."
    source_category: "user-answer"
    source_note: "User: the overload gatekeeper."
  - id: "MAP-002"
    statement: "Shortfall is derived: shortfall_mw = required_mw - total_allocated_mw (>=0)."
    source_category: "assumption"
    source_note: "Implied by shortage/partial states."
  - id: "FLOW-001"
    statement: "SCADA telemetry and market registry are fetched in parallel and joined before evaluation."
    source_category: "user-answer"
    source_note: "Root fans out to both fetchers; join aggregates."
  - id: "FLOW-002"
    statement: "Two distinct short-circuit mechanisms, resolved per review K4: (a) a FETCH FAILURE (SCADA or registry transport/5xx) routes via that fetcher's exception handler straight to terminal denial without waiting for the sibling fetch; (b) a DATA-VALIDITY failure (telemetry stale per FAIL-001, or empty candidate list) is checked by a post-join evaluator after both fetches resolve. Neither path enters the scoring chain."
    source_category: "user-answer"
    source_note: "Confirmed by user after review K4: transport failure short-circuits; data-validity is post-join."
  - id: "FLOW-003"
    statement: "Eligibility filter: exclude assets whose sustain_window_minutes < target_duration_minutes; exclude CRITICAL_RESERVE-tier assets unless the grid is in distressed mode."
    source_category: "user-answer"
    source_note: "Duration filter + reserve lockout."
  - id: "FLOW-004"
    statement: "Grid mode is derived from grid_frequency_hz: NORMAL when 59.9 <= f <= 60.1, else DISTRESSED. Mode selects the primary ranking key."
    source_category: "user-answer"
    source_note: "Deadband folded into normal; distressed strictly outside band."
  - id: "FLOW-005"
    statement: "Among eligible assets, rank by the mode-selected key (NORMAL: lowest price; DISTRESSED: highest ramp_rate_mw_per_s) and accumulate allocation until required_mw is met, headroom is reached, or candidates are exhausted."
    source_category: "user-answer"
    source_note: "Dynamic scoring + cumulative fill."
  - id: "FLOW-006"
    statement: "decision_status precedence: DENIED_CRITICAL > NO_CANDIDATES_AVAILABLE > PARTIALLY_ALLOCATED_SAFETY > PARTIALLY_ALLOCATED_SHORTAGE > FULLY_ALLOCATED. When BOTH headroom and supply bind, the label is SAFETY but binding_constraint=BOTH (OUT-005) carries the full truth so the SHORTAGE signal is not lost."
    source_category: "assumption"
    source_note: "Safety-wins-label confirmed; the both-bind information loss (review K2) is closed by binding_constraint rather than by changing the label."
  - id: "FAIL-001"
    statement: "If the SCADA fetch fails OR (input.timestamp - telemetry_timestamp) > MAX_TELEMETRY_AGE, return DISPATCH_DENIED_CRITICAL without evaluating (fail-closed). All three quantities are epoch SECONDS; MAX_TELEMETRY_AGE is in seconds."
    source_category: "user-answer"
    source_note: "Fail-closed on grid health. Unit coherence corrected per review K1 (was ms vs epoch-seconds)."
  - id: "FAIL-002"
    statement: "If the market registry fetch fails (5xx), return DISPATCH_DENIED_CRITICAL (cannot decide blind)."
    source_category: "assumption"
    source_note: "Symmetric extension of fail-closed; distinct from empty list; veto-able (OQ-006)."
  - id: "FAIL-003"
    statement: "If the registry succeeds but returns an empty candidate list, return NO_CANDIDATES_AVAILABLE so the caller can source from a neighboring substation."
    source_category: "user-answer"
    source_note: "Explicit fifth state."
  - id: "FAIL-004"
    statement: "If required_mw exceeds headroom_mw, clamp total allocation to headroom_mw and set PARTIALLY_ALLOCATED_SAFETY."
    source_category: "user-answer"
    source_note: "Overload gatekeeper."
  - id: "FAIL-005"
    statement: "If eligible+safe capacity cannot meet required_mw, allocate all available and set PARTIALLY_ALLOCATED_SHORTAGE."
    source_category: "user-answer"
    source_note: "Shortage state."
  - id: "FAIL-006"
    statement: "Every external fetcher MUST carry a failure handler that overwrites output.body with the clean DISPATCH_DENIED_CRITICAL shape; without it the engine's default copies the raw upstream error body into output.body, violating the no-leak rule."
    source_category: "source-observed"
    source_note: "minigraph-syntax.md (verified): fetcher failure without `exception=` handler copies upstream error body into output.body. Captured per review K3."
  - id: "OUT-005"
    statement: "Output includes binding_constraint (NONE|HEADROOM|SUPPLY|BOTH) so the caller can distinguish a transformer-capped dispatch from a storage-shortage dispatch when shortfall_mw > 0."
    source_category: "user-answer"
    source_note: "Confirmed by user after review K2; binding_constraint is a required output field."
  - id: "NFR-001"
    statement: "Decision must return within a real-time budget ('right now'); SCADA and registry must be fetched concurrently to meet it."
    source_category: "user-answer"
    source_note: "Second-by-second framing; exact budget is OQ-005."

invocation:
  trigger: "http"
  required_inputs:
    - path: "input.body.event_id"
      type: "string"
      source: "caller"
      missing_rule: "reject"
    - path: "input.body.target_substation_id"
      type: "string"
      source: "caller"
      missing_rule: "reject"
    - path: "input.body.required_mw"
      type: "float"
      source: "caller"
      missing_rule: "reject"   # also reject if <= 0
    - path: "input.body.target_duration_minutes"
      type: "int"
      source: "caller"
      missing_rule: "reject"   # also reject if <= 0
    - path: "input.body.timestamp"
      type: "int"
      source: "caller"
      missing_rule: "reject"   # epoch; used for telemetry staleness check
  optional_inputs: []
  sample_inputs:
    - |
      {
        "event_id": "evt_99482_demand_spike",
        "target_substation_id": "sub_metro_west_04",
        "required_mw": 12.5,
        "target_duration_minutes": 30,
        "timestamp": 1781132019
      }

output_contract:
  body:
    - path: "output.body.decision_status"
      type: "string-enum"
      required: true
      source_or_derivation: "FLOW-006 status precedence"
    - path: "output.body.binding_constraint"
      type: "string-enum NONE|HEADROOM|SUPPLY|BOTH"
      required: true
      source_or_derivation: "OUT-005; disambiguates why shortfall_mw>0 (transformer cap vs storage shortage vs both)"
    - path: "output.body.event_id"
      type: "string"
      required: true
      source_or_derivation: "input.body.event_id (correlation echo)"
    - path: "output.body.target_substation_id"
      type: "string"
      required: true
      source_or_derivation: "input.body.target_substation_id (echo)"
    - path: "output.body.total_allocated_mw"
      type: "float"
      required: true
      source_or_derivation: "sum of dispatches[].allocated_mw (0 on denial/no-candidates)"
    - path: "output.body.dispatches"
      type: "array<{storage_node_id:string, allocated_mw:float}>"
      required: true
      source_or_derivation: "ranked cumulative allocation; empty on denial/no-candidates"
    - path: "output.body.metrics.requested_mw"
      type: "float"
      required: true
      source_or_derivation: "input.body.required_mw"
    - path: "output.body.metrics.headroom_mw"
      type: "float"
      required: false
      source_or_derivation: "MAP-001; absent when telemetry failed"
    - path: "output.body.metrics.shortfall_mw"
      type: "float"
      required: false
      source_or_derivation: "MAP-002"
  headers: []
  error_shape: "decision_status = DISPATCH_DENIED_CRITICAL, empty dispatches[], total_allocated_mw = 0; metrics.headroom_mw/shortfall_mw may be absent."
  degraded_shape: "PARTIALLY_ALLOCATED_SHORTAGE / PARTIALLY_ALLOCATED_SAFETY / NO_CANDIDATES_AVAILABLE — successful body with reduced or zero allocation and an explanatory status + shortfall."

state_contract:
  inbound:
    - { path: "input.body.event_id", source: "caller", type: "string", required: true, notes: "echoed to output for correlation; added per review K5" }
    - { path: "input.body.target_substation_id", source: "caller", type: "string", required: true }
    - { path: "input.body.required_mw", source: "caller", type: "float", required: true }
    - { path: "input.body.target_duration_minutes", source: "caller", type: "int", required: true }
    - { path: "input.body.timestamp", source: "caller", type: "int (epoch SECONDS)", required: true }
  # 'origin' = data provenance (input | scada | registry | derived), NOT a node alias.
  # Node aliases are deferred to /design per R4 (review deletion-budget #1).
  model:
    - { path: "model.event_id", type: "string", origin: "input", readers: "output (echo)", lifecycle: "whole run" }
    - { path: "model.target_substation_id", type: "string", origin: "input", readers: "fetchers, output", lifecycle: "whole run" }
    - { path: "model.required_mw", type: "float", origin: "input", readers: "allocation, status", lifecycle: "whole run" }
    - { path: "model.target_duration_minutes", type: "int", origin: "input", readers: "eligibility filter", lifecycle: "whole run" }
    - { path: "model.grid.frequency_hz", type: "float", origin: "scada", readers: "mode, gate", lifecycle: "post-join" }
    - { path: "model.grid.current_load_mw", type: "float", origin: "scada", readers: "headroom", lifecycle: "post-join" }
    - { path: "model.grid.operating_max_capacity_mw", type: "float", origin: "scada", readers: "headroom", lifecycle: "post-join" }
    - { path: "model.grid.telemetry_timestamp", type: "int (epoch seconds)", origin: "scada", readers: "staleness gate", lifecycle: "post-join" }
    - { path: "model.headroom_mw", type: "float", origin: "derived", readers: "allocation, output", lifecycle: "post-join" }
    - { path: "model.candidates", type: "array<asset>", origin: "registry", readers: "filter, ranker, allocation", lifecycle: "post-join" }
    - { path: "model.eligible", type: "array<asset>", origin: "derived", readers: "ranker, allocation", lifecycle: "post-filter" }
    - { path: "model.grid_mode", type: "string-enum NORMAL|DISTRESSED", origin: "derived", readers: "filter, ranker", lifecycle: "post-join" }
    - { path: "model.dispatches", type: "array", origin: "derived", readers: "output", lifecycle: "post-allocate" }
    - { path: "model.total_allocated_mw", type: "float", origin: "derived", readers: "status, output", lifecycle: "post-allocate" }
    - { path: "model.shortfall_mw", type: "float", origin: "derived", readers: "status, output", lifecycle: "post-allocate" }
    - { path: "model.binding_constraint", type: "string-enum NONE|HEADROOM|SUPPLY|BOTH", origin: "derived", readers: "status, output", lifecycle: "post-allocate" }
    - { path: "model.decision_status", type: "string-enum", origin: "derived", readers: "output", lifecycle: "terminal" }
  node_local: []
  outbound:
    - { path: "output.body.decision_status", source: "model.decision_status" }
    - { path: "output.body.total_allocated_mw", source: "model.total_allocated_mw" }
    - { path: "output.body.dispatches", source: "model.dispatches" }
    - { path: "output.body.metrics", source: "model.required_mw / model.headroom_mw / model.shortfall_mw" }
  constants:
    - { value: "MAX_TELEMETRY_AGE", meaning: "max age (SECONDS) of SCADA reading before treated as stale → denial; units match epoch-seconds timestamps (review K1)", where_used: "FAIL-001 staleness gate", note: "value mocked (OQ-002)" }
    - { value: "FREQ_NORMAL_LOW=59.9 / FREQ_NORMAL_HIGH=60.1", meaning: "grid mode band", where_used: "FLOW-004", note: "boundary inclusivity build-tunable" }
    - { value: "CRITICAL_RESERVE", meaning: "sla_tier value locked out except in distressed mode", where_used: "FLOW-003" }
  opaque_pass_through: []   # event_id is now a normal inbound input echoed to output (review K5/deletion #5), not an opaque pass-through

sources:
  - name: "scada-telemetry"
    kind: "http"
    request_contract:
      - "GET, path/query keyed by target_substation_id"
    response_contract:
      - "grid_frequency_hz: float"
      - "current_load_mw: float"
      - "operating_max_capacity_mw: float (dynamic, ambient-derated)"
      - "telemetry_timestamp: int (epoch)"
    auth_or_feature_needs: ["internal API auth — feature name mocked (OQ-001)"]
    dependency_notes: ["Fetched in parallel with registry. Failure or staleness ⇒ fail-closed (FAIL-001)."]
    mock:
      url: "https://api.internal.grid/scada/substations/{target_substation_id}/telemetry"
      note: "endpoint + auth mocked; replace before deploy"
  - name: "market-registry"
    kind: "http"
    request_contract:
      - "GET /market/candidates?substation={target_substation_id}&type=storage"
    response_contract:
      - "array of { storage_node_id, dischargeable_mw, sustain_window_minutes, sla_tier, price, ramp_rate_mw_per_s }"
    auth_or_feature_needs: ["internal API auth — feature name mocked (OQ-001)"]
    dependency_notes: ["Parallel with SCADA. 5xx ⇒ DENIED_CRITICAL (FAIL-002); empty list ⇒ NO_CANDIDATES (FAIL-003)."]
    mock:
      url: "https://api.internal.grid/market/candidates?substation={target_substation_id}&type=storage"
      note: "endpoint + auth + exact field paths mocked (OQ-003)"

mappings:
  input_to_model:
    - "input.body.event_id -> model.event_id"
    - "input.body.target_substation_id -> model.target_substation_id"
    - "input.body.required_mw -> model.required_mw"
    - "input.body.target_duration_minutes -> model.target_duration_minutes"
    - "input.body.timestamp -> model.request_timestamp"
  source_to_model:
    - "scada.grid_frequency_hz -> model.grid.frequency_hz"
    - "scada.current_load_mw -> model.grid.current_load_mw"
    - "scada.operating_max_capacity_mw -> model.grid.operating_max_capacity_mw"
    - "scada.telemetry_timestamp -> model.grid.telemetry_timestamp"
    - "registry[*] -> model.candidates[]"
  model_to_output:
    - "model.event_id -> output.body.event_id   # correlation echo"
    - "model.decision_status -> output.body.decision_status"
    - "model.binding_constraint -> output.body.binding_constraint"
    - "model.total_allocated_mw -> output.body.total_allocated_mw"
    - "model.dispatches -> output.body.dispatches"
    - "model.required_mw -> output.body.metrics.requested_mw"
    - "model.headroom_mw -> output.body.metrics.headroom_mw"
    - "model.shortfall_mw -> output.body.metrics.shortfall_mw"
  derived_values:
    - "headroom_mw = operating_max_capacity_mw - current_load_mw"
    - "shortfall_mw = max(0, required_mw - total_allocated_mw)"
    - "grid_mode = NORMAL if 59.9 <= frequency_hz <= 60.1 else DISTRESSED"
    - "is_stale = (request_timestamp - telemetry_timestamp) > MAX_TELEMETRY_AGE   # all in seconds (review K1)"
    # CORRECTED per build/test DEF-1: 'headroom binds' means the allocation actually REACHED the headroom cap
    # (hit_cap = total_allocated >= min(required, headroom)), NOT merely required > headroom. Supply that gives out
    # below the headroom ceiling is a SHORTAGE, not a safety clamp.
    - "hit_cap = total_allocated >= min(required, headroom)"
    - "binding_constraint = NONE if total>=required ; SUPPLY if NOT hit_cap ; else (BOTH if eligible_capacity<required else HEADROOM)"
  defaulting_rules:
    - "No fail-open defaults for grid health — absence ⇒ denial, not a nominal substitute."
  repeated_mappings:
    - "Per-candidate: eligibility test (sustain_window_minutes >= target_duration_minutes; reserve lockout) and ranking-key extraction."

control_flow:
  sequential:
    - "join → telemetry-validity gate → headroom calc → eligibility filter → mode-based ranking → cumulative allocation → status determination → output"
  parallel:
    - "scada-telemetry fetch || market-registry fetch"
  joins:
    - "Successful fetches synchronize at the join, then a post-join validity gate (staleness + empty-list) runs before scoring."
    - "A transport/5xx fetch failure bypasses the join via its exception handler and routes directly to denial — it does not wait for the sibling fetch (FLOW-002a)."
  decisions:
    - "telemetry valid & fresh? no ⇒ DENIED_CRITICAL"
    - "registry failed? yes ⇒ DENIED_CRITICAL"
    - "candidates empty? yes ⇒ NO_CANDIDATES_AVAILABLE"
    - "grid_mode NORMAL vs DISTRESSED ⇒ ranking key + reserve lockout"
    - "required_mw > headroom? ⇒ clamp + SAFETY"
    - "eligible capacity < required? ⇒ SHORTAGE"
  repeated_steps:
    - "per-candidate filter and ranked accumulation"

failure_behavior:
  stop_conditions:
    - "SCADA fetch failure (FAIL-001)"
    - "Stale telemetry beyond MAX_TELEMETRY_AGE (FAIL-001)"
    - "Market registry fetch failure / 5xx (FAIL-002)"
  degraded_conditions:
    - "Empty candidate list ⇒ NO_CANDIDATES_AVAILABLE (FAIL-003)"
    - "Dispatch clamped by headroom ⇒ PARTIALLY_ALLOCATED_SAFETY (FAIL-004)"
    - "Eligible capacity < required ⇒ PARTIALLY_ALLOCATED_SHORTAGE (FAIL-005)"
  retry_rules:
    - "None specified in-graph; real-time budget likely precludes retries. Open (OQ-005)."
  fallback_rules:
    - "No fail-open fallback for grid telemetry (explicit). Caller handles escalation/manual control on DENIED_CRITICAL and neighbor-sourcing on NO_CANDIDATES."
  error_response_rules:
    - "All terminal failures return a well-formed body with the appropriate decision_status and empty dispatches; upstream error bodies are NOT leaked to the caller."
    - "REALIZABILITY (FAIL-006, review K3): the no-leak rule is NOT the engine default — a fetcher failure without an `exception=` handler copies the raw upstream error body into output.body. Each fetcher MUST therefore carry an exception handler that overwrites output.body with the clean denial shape. /design must satisfy this, not assume it."

non_functional:
  latency: "real-time / sub-second target; exact budget OQ-005. Parallel fetch mandatory (NFR-001)."
  concurrency: "two concurrent source fetches per request."
  caching: "none — each event is point-in-time; stale data is a denial condition, not a cache hit."
  ttl: "request-scoped only; no cross-request state (engine-given)."
  logging_security:
    - "Do not leak upstream API error bodies to caller."
    - "event_id carried for correlation/observability."
  observability:
    - "Inspectable: model.grid.*, model.grid_mode, model.eligible, model.dispatches, model.decision_status, model.headroom_mw, model.shortfall_mw."

tests:
  - { id: "T-01", name: "happy path — full allocation", input: "demand fully met by eligible assets, grid normal", expected_output: "FULLY_ALLOCATED; sum(dispatches)=required_mw", inspect: ["model.decision_status","model.total_allocated_mw"] }
  - { id: "T-02", name: "missing/invalid input", input: "required_mw missing or <=0, or target_duration_minutes missing", expected_output: "rejected per INV-002", inspect: ["output.body"] }
  - { id: "T-03", name: "empty registry", input: "registry returns []", expected_output: "NO_CANDIDATES_AVAILABLE; dispatches=[]", inspect: ["model.decision_status"] }
  - { id: "T-04", name: "SCADA failure", input: "SCADA returns 5xx", expected_output: "DISPATCH_DENIED_CRITICAL; eval chain skipped", inspect: ["model.decision_status","seen path"] }
  - { id: "T-05", name: "stale telemetry", input: "telemetry_timestamp older than MAX_TELEMETRY_AGE", expected_output: "DISPATCH_DENIED_CRITICAL", inspect: ["model.grid.telemetry_timestamp","model.decision_status"] }
  - { id: "T-06", name: "capacity shortage", input: "eligible capacity < required_mw, grid normal, within headroom", expected_output: "PARTIALLY_ALLOCATED_SHORTAGE; shortfall_mw>0", inspect: ["model.shortfall_mw","model.decision_status"] }
  - { id: "T-07", name: "headroom cap", input: "required_mw > headroom_mw", expected_output: "PARTIALLY_ALLOCATED_SAFETY; total_allocated_mw==headroom_mw", inspect: ["model.headroom_mw","model.total_allocated_mw"] }
  - { id: "T-08", name: "distressed mode", input: "frequency < 59.9; reserve assets present", expected_output: "reserves unlocked; ranking by highest ramp_rate_mw_per_s", inspect: ["model.grid_mode","model.eligible","model.dispatches"] }
  - { id: "T-09", name: "both bind — headroom cap AND shortage", input: "required_mw > headroom_mw AND eligible capacity < required_mw", expected_output: "decision_status=PARTIALLY_ALLOCATED_SAFETY, binding_constraint=BOTH, shortfall_mw>0", inspect: ["model.decision_status","model.binding_constraint","model.shortfall_mw"] }

scope_boundary:
  in:
    - "Single-event dispatch recommendation (advisory, read-only)"
    - "Parallel sourcing of grid telemetry + candidate storage assets"
    - "Eligibility filtering (sustain window, reserve lockout)"
    - "Grid-mode determination and mode-based ranking"
    - "Cumulative allocation to meet required_mw"
    - "Transformer/substation overload clamp from operating_max_capacity_mw"
    - "Status determination across the 5-state enum"
  out:
    - "Continuous / second-by-second control loop"
    - "Persistent topology, edge rewriting, digital twin"
    - "Physical hardware actuation / switch commands"
    - "Predictive blast-radius / cascading-failure ML inference"
    - "Cross-substation / neighbor sourcing (caller's job on NO_CANDIDATES)"
    - "SLA priority arbitration across competing demand events (event bus selects which event to send)"
    - "Event detection / threshold monitoring"
  partial:
    - "Overload prevention: this graph clamps only its own recommended dispatch; system-wide overload management is broader and external."
  decisions: []   # none blocking requirements or design

decisions:
  - { id: "D-01", decision: "Fail-closed on grid telemetry: SCADA failure or stale reading ⇒ DISPATCH_DENIED_CRITICAL, no nominal-frequency fallback.", source_category: "user-answer", source_note: "Explicit reversal of the original fail-open 60.0 default." }
  - { id: "D-02", decision: "Mode-dependent ranking: NORMAL ⇒ rank by lowest price; DISTRESSED (<59.9 or >60.1) ⇒ rank by highest ramp_rate and unlock CRITICAL_RESERVE assets.", source_category: "user-answer", source_note: "Dynamic scoring." }
  - { id: "D-03", decision: "Graph is the overload gatekeeper: clamp allocation to headroom_mw = operating_max_capacity_mw - current_load_mw.", source_category: "user-answer", source_note: "Overload responsibility IN." }
  - { id: "D-04", decision: "Five-state decision_status enum incl. distinct NO_CANDIDATES_AVAILABLE.", source_category: "user-answer", source_note: "Output contract." }
  - { id: "D-05", decision: "Capacity modeled in MW over a caller-supplied target_duration_minutes; assets filtered by sustain_window_minutes >= target_duration_minutes.", source_category: "user-answer", source_note: "MW/MWh reconciliation; duration is a required input." }
  - { id: "D-06", decision: "Deadband folded into NORMAL: grid mode band is 59.9–60.1 inclusive, DISTRESSED strictly outside.", source_category: "assumption", source_note: "Split from old bundled D-06 per review (deletion-budget #6); veto-able." }
  - { id: "D-07", decision: "When headroom and supply both bind, decision_status label is SAFETY but binding_constraint=BOTH (OUT-005) preserves the SHORTAGE signal; shortfall_mw remains populated. Adds binding_constraint as a required output-contract field.", source_category: "user-answer", source_note: "Confirmed by user after review K2." }
  - { id: "D-08", decision: "Registry transport/5xx failure ⇒ DISPATCH_DENIED_CRITICAL (distinct from empty list ⇒ NO_CANDIDATES_AVAILABLE), via FAIL-002 and the FAIL-006 exception-handler obligation.", source_category: "assumption", source_note: "Split from old bundled D-06; confirmation tracked in OQ-006." }

open_questions:
  - { id: "OQ-001", question: "Real endpoint URLs and auth/feature mechanism for SCADA and registry APIs.", blocks: "build", closure_plan: "Mock URLs + auth feature name now; replace before deploy." }
  - { id: "OQ-002", question: "Concrete MAX_TELEMETRY_AGE value (units now fixed to seconds per K1).", blocks: "build", closure_plan: "Mock (e.g. 5s); confirm with grid ops before deploy." }
  - { id: "OQ-003", question: "Exact response field names / JSON paths for both sources.", blocks: "build", closure_plan: "Mock against the documented response_contract; bind real paths during build with dictionaries." }
  - { id: "OQ-004", question: "Secondary ranking weights/tiebreaks and exact threshold inclusivity (primary keys are locked).", blocks: "build", closure_plan: "Mock single-key sort + stable tiebreak by storage_node_id; tune later." }
  - { id: "OQ-005", question: "Latency budget number, per-fetch timeouts, and whether any retry is permitted.", blocks: "deploy", closure_plan: "Carry as known risk; set conservative fetch timeouts as mock; confirm SLA." }
  - { id: "OQ-006", question: "Confirm registry 5xx ⇒ DENIED_CRITICAL (vs a distinct registry-down state).", blocks: "build", closure_plan: "Assume DENIED_CRITICAL (FAIL-002); cheap to split later." }
  - { id: "OQ-007", question: "Deployment of the graph to the extension-callable location (the event bus must reach it).", blocks: "deploy", closure_plan: "Deploy JSON to location.graph.deployed; classpath target needs rebuild/restart." }

gate_result:
  status: "pass"   # re-issued after independent review; first pass was unsupported until K1–K4 fixed
  blockers: []
  review_resolution:
    - { finding: "K1 telemetry-staleness unit incoherence (ms vs epoch-seconds)", disposition: "applied — units fixed to seconds across INV-002, FAIL-001, constants, derived_values, OQ-002" }
    - { finding: "K2 SAFETY/SHORTAGE state collapse", disposition: "applied — added binding_constraint (OUT-005, D-07), FLOW-006 + T-09; output-contract change flagged for user veto" }
    - { finding: "K3 fetcher error-leak vs engine default", disposition: "applied — added FAIL-006 obligation + error_response_rules realizability note" }
    - { finding: "K4 fetch-failure routing ambiguity", disposition: "applied — FLOW-002 + joins now split transport-failure short-circuit from post-join data-validity gate" }
    - { finding: "K5 event_id missing from inbound", disposition: "applied — added to inbound, model, mappings, output echo; opaque_pass_through emptied" }
    - { finding: "K6 optional-metric silence semantics", disposition: "overridden — headroom_mw absence on denial is intentional; acceptable to carry, low risk" }
    - { finding: "deletion #1 design-phase node-alias writers", disposition: "applied — writer→origin (provenance), node aliases deferred to /design per R4" }
    - { finding: "deletion #3 TEST-001 meta-requirement", disposition: "applied — removed; concrete T-01..T-09 retained" }
    - { finding: "deletion #6 D-06 bundled three decisions", disposition: "applied — split into D-06/D-07/D-08" }
    - { finding: "deletion #2 observability duplicates model contract", disposition: "overridden — harmless redundancy, kept for reader convenience" }
    - { finding: "deletion #4 partial-overload prose duplicate", disposition: "overridden — kept for explicitness of the PARTIAL scope boundary" }
  carried_blockers:
    - { question_id: "OQ-001", mock_used: "internal.grid mock URLs + named auth feature" }
    - { question_id: "OQ-002", mock_used: "MAX_TELEMETRY_AGE = 5 seconds (placeholder)" }
    - { question_id: "OQ-003", mock_used: "documented response_contract field set" }
    - { question_id: "OQ-004", mock_used: "single-key sort, tiebreak by storage_node_id" }
    - { question_id: "OQ-005", mock_used: "conservative fetch timeouts; no retry" }
    - { question_id: "OQ-006", mock_used: "registry 5xx ⇒ DISPATCH_DENIED_CRITICAL" }
    - { question_id: "OQ-007", mock_used: "deploy to location.graph.deployed before external calls" }
```
