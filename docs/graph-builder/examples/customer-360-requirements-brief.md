# Example Design-Ready Brief - Customer 360

Status: example.

Purpose: show the canonical `/requirements` brief schema with plausible Customer 360 content. This is not a complete production requirement; it is a schema example.

```yaml
graph:
  name: "customer-360"
  purpose: "Aggregate customer profile, accounts, preferences, and risk signals into one caller-facing response."
  workflow_category: "composition"

invocation:
  trigger: "http"
  required_inputs:
    - path: "input.body.person_id"
      type: "string"
      source: "caller"
      missing_rule: "reject"
  optional_inputs:
    - path: "input.header.correlation_id"
      type: "string"
      source: "caller"
      missing_rule: "ignore"
  sample_inputs:
    - name: "happy path"
      body:
        person_id: "P-10001"

output_contract:
  body:
    - path: "output.body.customer_id"
      type: "string"
      required: true
      source_or_derivation: "model.person_id"
    - path: "output.body.profile"
      type: "object"
      required: true
      source_or_derivation: "model.profile"
    - path: "output.body.accounts"
      type: "array"
      required: true
      source_or_derivation: "model.accounts"
    - path: "output.body.preferences"
      type: "object"
      required: false
      source_or_derivation: "model.preferences"
    - path: "output.body.risk"
      type: "object"
      required: false
      source_or_derivation: "model.risk"
    - path: "output.body.degraded"
      type: "boolean"
      required: true
      source_or_derivation: "model.degraded"
  headers: []
  error_shape: "Reject missing or malformed person_id with a caller-facing validation error."
  degraded_shape: "Return available profile/account/preference data with degraded=true when risk is unavailable."

state_contract:
  inbound:
    - path: "input.body.person_id"
      source: "caller"
      type: "string"
      required: true
      notes: "Primary lookup key."
  model:
    - path: "model.person_id"
      type: "string"
      writer: "input mapping"
      readers: ["source requests", "output mapping"]
      lifecycle: "set at graph start; retained through output"
    - path: "model.profile"
      type: "object"
      writer: "profile source mapping"
      readers: ["output mapping"]
      lifecycle: "set after profile source returns"
    - path: "model.accounts"
      type: "array"
      writer: "accounts source mapping"
      readers: ["output mapping"]
      lifecycle: "set after accounts source returns"
    - path: "model.preferences"
      type: "object"
      writer: "preferences source mapping"
      readers: ["output mapping"]
      lifecycle: "set after preferences source returns"
    - path: "model.risk"
      type: "object"
      writer: "risk source mapping or fallback"
      readers: ["output mapping"]
      lifecycle: "set after risk source or fallback"
    - path: "model.degraded"
      type: "boolean"
      writer: "fallback handling"
      readers: ["output mapping"]
      lifecycle: "defaults false; set true if risk fallback is used"
  node_local: []
  outbound:
    - path: "output.body"
      type: "object"
      source_or_derivation: "model.*"
      notes: "Caller-facing aggregate."
  constants:
    - value: false
      meaning: "default non-degraded response"
      where_used: "model.degraded"
  opaque_pass_through: []

sources:
  - name: "profile-api"
    kind: "http"
    request_contract:
      - "requires person_id"
    response_contract:
      - "returns customer profile object"
    auth_or_feature_needs: []
    dependency_notes:
      - "required for successful full response"
    mock: null
  - name: "accounts-api"
    kind: "http"
    request_contract:
      - "requires person_id"
    response_contract:
      - "returns account array"
    auth_or_feature_needs: []
    dependency_notes:
      - "can run in parallel with profile, preferences, and risk"
    mock: null
  - name: "preferences-api"
    kind: "http"
    request_contract:
      - "requires person_id"
    response_contract:
      - "returns preferences object"
    auth_or_feature_needs: []
    dependency_notes:
      - "best-effort if product requirements allow"
    mock: null
  - name: "risk-api"
    kind: "http"
    request_contract:
      - "requires person_id"
    response_contract:
      - "returns risk object"
    auth_or_feature_needs:
      - "mocked bearer-token feature until auth integration is confirmed"
    dependency_notes:
      - "failure should degrade, not stop, unless policy changes"
    mock:
      what: "auth feature and response fixture"
      placeholder: "risk mock fixture with neutral score"
      blocks: "deploy"

mappings:
  input_to_model:
    - "input.body.person_id -> model.person_id"
  source_to_model:
    - "profile-api response body -> model.profile"
    - "accounts-api response body -> model.accounts"
    - "preferences-api response body -> model.preferences"
    - "risk-api response body -> model.risk"
  model_to_output:
    - "model.person_id -> output.body.customer_id"
    - "model.profile -> output.body.profile"
    - "model.accounts -> output.body.accounts"
    - "model.preferences -> output.body.preferences"
    - "model.risk -> output.body.risk"
    - "model.degraded -> output.body.degraded"
  derived_values:
    - "default model.degraded to false"
  repeated_mappings: []

control_flow:
  sequential:
    - "map input person_id into model"
    - "shape final output after all required branches complete"
  parallel:
    - "fetch profile"
    - "fetch accounts"
    - "fetch preferences"
    - "fetch risk"
  joins:
    - "join source branches before final output mapping"
  decisions:
    - "risk failure uses degraded fallback"
  repeated_steps: []

failure_behavior:
  stop_conditions:
    - "missing or malformed person_id"
    - "required profile source unavailable, unless product changes to allow degraded profile"
  degraded_conditions:
    - "risk source unavailable"
  retry_rules:
    - "risk may retry once if supported by design"
  fallback_rules:
    - "risk failure sets model.risk to neutral fallback and model.degraded to true"
  error_response_rules:
    - "validation failures produce caller-facing error shape"

non_functional:
  latency: "parallel source calls should keep aggregate response within caller timeout budget"
  concurrency: "parallel fan-out allowed; exact source concurrency limits are open"
  caching: "no cross-request caching assumed"
  ttl: null
  logging_security:
    - "do not log sensitive customer data"
  observability:
    - "inspect model.* and output.body during tests"

tests:
  - id: "T-01"
    name: "happy path"
    input: "person_id P-10001 with all source mocks returning data"
    expected_output: "profile, accounts, preferences, risk, degraded=false"
    inspect:
      - "model.person_id"
      - "model.profile"
      - "model.accounts"
      - "output.body"
  - id: "T-02"
    name: "missing person_id"
    input: "empty body"
    expected_output: "validation error"
    inspect:
      - "output.body"
  - id: "T-03"
    name: "risk degraded fallback"
    input: "person_id P-10001 with risk source failure"
    expected_output: "available non-risk sections, fallback risk, degraded=true"
    inspect:
      - "model.risk"
      - "model.degraded"
      - "output.body.degraded"

scope_boundary:
  in:
    - "aggregate caller-facing Customer 360 response"
    - "source fan-out and response shaping"
    - "risk degraded fallback behavior"
  out:
    - "source system ownership"
    - "production auth implementation"
  partial:
    - "risk auth is mocked for design/build and blocks deploy"
  decisions: []

decisions:
  - id: "D-01"
    decision: "Use person_id as the primary lookup key."
    source_category: "artifact"
    source_note: "Derived from generated Customer 360 graph artifact."
  - id: "D-02"
    decision: "Risk source failure degrades response instead of stopping graph."
    source_category: "assumption"
    source_note: "Useful example behavior; confirm with product owner for production."

open_questions:
  - id: "OQ-001"
    question: "What auth feature supplies upstream bearer tokens?"
    blocks: "deploy"
    closure_plan: "Use mock auth feature during design/build; replace before deploy."
  - id: "OQ-002"
    question: "What are the exact source latency and concurrency limits?"
    blocks: "build"
    closure_plan: "Design parallel fan-out; set conservative concurrency during build until source owners confirm."

gate_result:
  status: "blocked"
  blockers:
    - "OQ-002 blocks final build concurrency settings."
    - "OQ-001 blocks deploy."
```