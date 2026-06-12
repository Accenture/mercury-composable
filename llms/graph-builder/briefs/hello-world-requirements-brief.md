# Design-Ready Brief - Hello World

Status: design-ready (`/requirements` complete, gate: pass).

Purpose: capture the obligations for a deployable hello-world graph so `/design` can choose topology. No node names or graph syntax appear here by design.

```yaml
graph:
  name: "hello-world"
  purpose: "Return a personalized hello-world JSON greeting when invoked over HTTP."
  workflow_category: "read"

requirements:
  - id: "INV-001"
    statement: "Invoked over HTTP via POST with an optional name supplied in the request body."
    source_category: "user-answer"
    source_note: "User chose HTTP endpoint, request body, personalized."
  - id: "OUT-001"
    statement: "Successful response is a JSON object with a single message field containing the greeting."
    source_category: "user-answer"
    source_note: "User chose JSON object response."
  - id: "MAP-001"
    statement: "The greeting is composed as 'Hello, ' + the effective name; the effective name is the input name or the default."
    source_category: "user-answer"
    source_note: "User chose personalized greeting with a default."
  - id: "FAIL-001"
    statement: "A missing or empty name defaults to 'World' so the graph always succeeds with no error path."
    source_category: "user-answer"
    source_note: "User chose default-to-World over reject."

invocation:
  trigger: "http"
  required_inputs: []
  optional_inputs:
    - path: "input.body.name"
      type: "string"
      source: "caller"
      missing_rule: "default"
      default: "World"
      notes: "When absent, empty, or whitespace, treat as the default 'World'."
  sample_inputs:
    - name: "named"
      payload: '{ "name": "Wes" }'
    - name: "no name"
      payload: "{}"

output_contract:
  body:
    - path: "output.body.message"
      type: "string"
      required: true
      source_or_derivation: "'Hello, ' + effective_name (see MAP-001)"
  headers:
    - path: "output.header.content-type"
      value: "application/json"
      notes: "JSON object response."
  error_shape: "None. The graph has no caller-facing error path; missing name degrades to the default (FAIL-001)."
  degraded_shape: "Not applicable as partial success; the default-name path is the normal success path, not a degraded one."

state_contract:
  inbound:
    - path: "input.body.name"
      source: "caller"
      type: "string"
      required: false
      notes: "Optional greeting target."
  model:
    - path: "model.name"
      type: "string"
      writer: "input-resolution step"
      readers: "greeting-composition step"
      lifecycle: "set once from input or default at start of run"
      notes: "Effective name = input.body.name if present and non-empty, else 'World'."
    - path: "model.message"
      type: "string"
      writer: "greeting-composition step"
      readers: "output-shaping step"
      lifecycle: "set once after model.name is resolved"
      notes: "Final greeting string."
  node_local: []
  outbound:
    - path: "output.body.message"
      type: "string"
      source: "model.message"
      notes: "Returned to caller."
  constants:
    - value: "World"
      meaning: "Default greeting target when no name is supplied."
      where_used: "name resolution (FAIL-001)"
    - value: "Hello, "
      meaning: "Greeting prefix."
      where_used: "greeting composition (MAP-001)"
  opaque_pass_through: []

sources: []
# No external or internal sources. The greeting is computed entirely from input + constants.

mappings:
  input_to_model:
    - from: "input.body.name"
      to: "model.name"
      rule: "Use as-is when present and non-empty; otherwise substitute constant 'World'."
  source_to_model: []
  model_to_output:
    - from: "model.message"
      to: "output.body.message"
      rule: "direct"
  derived_values:
    - target: "model.message"
      rule: "Concatenate 'Hello, ' + model.name."
  defaulting_rules:
    - target: "model.name"
      rule: "Default to 'World' when input.body.name is missing, null, empty, or whitespace-only."
  repeated_mappings: []

control_flow:
  sequential:
    - "Resolve effective name (input or default)."
    - "Compose greeting message."
    - "Shape output body."
  parallel: []
  joins: []
  decisions:
    - "If input.body.name is missing/empty -> use 'World'; else use the provided name. (Pure value-level default, not a topology branch.)"
  repeated_steps: []

failure_behavior:
  stop_conditions: []
  degraded_conditions: []
  retry_rules: []
  fallback_rules:
    - "Absent/empty name falls back to the constant default 'World'."
  error_response_rules: []
  # No external dependencies, so there are no dependency-failure behaviors to define.

non_functional:
  latency: "Negligible; no external calls. Should respond well within any normal HTTP timeout."
  concurrency: null
  caching: null
  ttl: null
  logging_security:
    - "name is non-sensitive; no special logging restriction. Echoes the supplied name back to the caller by design."
  observability:
    - "model.name and model.message should be inspectable during /test to prove the default and the greeting composition."

tests:
  - id: "T-01"
    name: "happy path - named"
    input: '{ "name": "Wes" }'
    expected_output: '{ "message": "Hello, Wes" }'
    inspect: ["model.name == 'Wes'", "model.message == 'Hello, Wes'"]
  - id: "T-02"
    name: "default - no name field"
    input: "{}"
    expected_output: '{ "message": "Hello, World" }'
    inspect: ["model.name == 'World'", "model.message == 'Hello, World'"]
  - id: "T-03"
    name: "default - empty name"
    input: '{ "name": "" }'
    expected_output: '{ "message": "Hello, World" }'
    inspect: ["model.name == 'World'"]

scope_boundary:
  in:
    - "Returning a JSON hello-world greeting over HTTP."
    - "Defaulting the greeting target when no name is given."
  out:
    - "Authentication / authorization."
    - "Rate limiting, input sanitization beyond default handling."
    - "Persistence or logging of greetings."
  partial: []
  decisions: []

decisions:
  - id: "D-01"
    decision: "Invoked over HTTP via POST."
    source_category: "user-answer"
    source_note: "Trigger question."
  - id: "D-02"
    decision: "Response is a JSON object: { message }."
    source_category: "user-answer"
    source_note: "Response-shape question."
  - id: "D-03"
    decision: "name arrives in the request body and is optional."
    source_category: "user-answer"
    source_note: "Input-path + personalization questions."
  - id: "D-04"
    decision: "Missing name defaults to 'World'; no error path."
    source_category: "user-answer"
    source_note: "Missing-name question."

open_questions: []

gate_result:
  status: "pass"
  blockers: []
  carried_blockers: []
```

## Plain-language statement of what `/design` must satisfy

Design an HTTP-invoked graph that reads an optional `name` from the request body,
resolves an effective name (the supplied name, or the constant `"World"` when it is
missing/empty), composes the string `"Hello, " + effective name`, and returns it as
a JSON body `{ "message": <greeting> }`. There are no external sources and no failure
paths — the graph always succeeds. Topology and node responsibilities are `/design`'s
to choose.
