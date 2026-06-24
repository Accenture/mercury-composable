# Graph Design Spec - Hello World

Status: build-ready (`/design` complete, gate: pass).

Purpose: lower the hello-world requirements brief into a buildable MiniGraph architecture. No Companion API command syntax appears here; `/build` owns that.

Revision note: the greeting computation uses `graph.data.mapper` + a `graph.math` empty-string
decision (user chose math over `graph.js`). `graph.math` cannot produce strings (see
build_handoff), so it contributes only the empty-string *decision*; the mapper does all string
work. The whitespace-only clause of FAIL-001 is descoped (no trim primitive without `graph.js`).

```yaml
graph_design_spec:
  graph:
    name: "hello-world"
    purpose: "Return a personalized hello-world JSON greeting; default the name to 'World' when missing/null/empty."
    invocation: "HTTP POST; optional input.body.name"
    selected_shape: "decision-branch"   # linear spine + one empty-string branch

  source_brief:
    requirements_artifact: "llms/graph-builder/briefs/hello-world-requirements-brief.md"
    requirements_gate_status: "pass"
    requirements_carried_blockers: []

  requirement_traceability:
    - requirement_id: "INV-001"
      design_elements: ["node:root", "state:input.body.name", "edge:root->resolve-input"]
      notes: "HTTP POST entry; optional name read by resolve-input."
    - requirement_id: "OUT-001"
      design_elements: ["state:output.body.message", "node:compose", "state:output.header.content-type"]
      notes: "JSON object body with single message field; content-type set in resolve-input."
    - requirement_id: "MAP-001"
      design_elements: ["node:resolve-input (text(Hello, ) -> model.greeting_prefix)", "node:compose (model.greeting_prefix:concat(model.name))"]
      notes: "Greeting = 'Hello, ' + effective name, built via comma-safe selector-concat."
    - requirement_id: "FAIL-001"
      design_elements: ["node:resolve-input (f:defaultValue, null/missing)", "node:check-empty (math IF name=='')", "node:default-empty (text(World) -> model.name)"]
      notes: "NARROWED: covers null/missing (mapper) + empty string '' (math branch). Whitespace-only is OUT of scope (no trim primitive without graph.js). User-confirmed during /design (D-05)."

  node_inventory:
    - alias: "root"
      type: "root"
      skill: null
      responsibility: "Graph entry point / structural waypoint."
      reads: []
      writes: []
      upstream: []
      downstream: ["resolve-input"]
      design_properties: {}
      satisfies: ["INV-001"]

    - alias: "resolve-input"
      type: "transform"
      skill: "graph.data.mapper"
      responsibility: "Default null/missing name to 'World', stage the greeting prefix, set content-type."
      reads: ["input.body.name"]
      writes: ["model.name", "model.greeting_prefix", "output.header.content-type"]
      upstream: ["root"]
      downstream: ["check-empty"]
      design_properties:
        mapping:
          - "f:defaultValue(input.body.name, text(World)) -> model.name"
          - "text(Hello, ) -> model.greeting_prefix"
          - "text(application/json) -> output.header.content-type"
      satisfies: ["INV-001", "FAIL-001", "MAP-001", "OUT-001"]
      notes: >
        f:defaultValue catches null/missing only (an empty string is non-null and passes through).
        The greeting prefix is staged here (not in compose) so concat never depends on intra-node
        mapping order. text() constants go through getConstantValue, which is comma-safe.

    - alias: "check-empty"
      type: "decision"
      skill: "graph.math"
      responsibility: "Branch when the resolved name is an empty string."
      reads: ["model.name"]
      writes: ["check-empty.decision"]
      upstream: ["resolve-input"]
      downstream: ["compose"]          # natural edge = ELSE/non-empty path
      design_properties:
        statement:
          - |
            IF: {model.name} == ''
            THEN: default-empty
            ELSE: compose
      satisfies: ["FAIL-001"]
      notes: >
        Source-verified: in a logical (==) expression a string value is single-quote-wrapped
        (GraphLambdaFunction.java:253), so this becomes ''=='' (true) or 'World'=='' (false).
        model.name is guaranteed non-null by resolve-input, so the IF never references an
        unresolved value. Returns a named THEN/ELSE target (never bare next), so no fan-out.

    - alias: "default-empty"
      type: "transform"
      skill: "graph.data.mapper"
      responsibility: "Overwrite an empty name with the 'World' default."
      reads: []
      writes: ["model.name"]   # controlled overwrite, empty-string branch only
      upstream: ["check-empty (THEN jump)"]
      downstream: ["compose"]
      design_properties:
        mapping:
          - "text(World) -> model.name"
      satisfies: ["FAIL-001"]

    - alias: "compose"
      type: "transform"
      skill: "graph.data.mapper"
      responsibility: "Concatenate prefix + effective name into the response body."
      reads: ["model.greeting_prefix", "model.name"]
      writes: ["output.body.message"]
      upstream: ["check-empty (ELSE/natural)", "default-empty (natural)"]
      downstream: ["end"]
      design_properties:
        mapping:
          - "model.greeting_prefix:concat(model.name) -> output.body.message"
      satisfies: ["MAP-001", "OUT-001"]
      notes: >
        Selector-concat (source:concat(params)) uses the comma-aware tokenizer
        (DataMappingHelper.tokenizeConcatParameters) and model-only params, so the comma in
        'Hello, ' is safe. The f:concat(a,b) plugin form is NOT safe here (splits on raw comma) —
        see build_handoff. Runs once: on THEN the natural check-empty->compose edge is overridden
        by the jump to default-empty; on ELSE check-empty targets compose directly.

    - alias: "end"
      type: "end"
      skill: null
      responsibility: "Completion node; output.body/output.header become the HTTP response."
      reads: ["output.body.message", "output.header.content-type"]
      writes: []
      upstream: ["compose"]
      downstream: []
      design_properties: {}
      satisfies: ["OUT-001"]

  edge_plan:
    natural_edges:
      - from: "root"
        to: "resolve-input"
        purpose: "Enter the transform."
      - from: "resolve-input"
        to: "check-empty"
        purpose: "Resolved name -> empty-string decision."
      - from: "check-empty"
        to: "compose"
        purpose: "ELSE / non-empty path: name is usable, go compose. (Single natural edge -> no next fan-out.)"
      - from: "default-empty"
        to: "compose"
        purpose: "After applying the 'World' default, go compose."
      - from: "compose"
        to: "end"
        purpose: "Greeting written -> complete."
    jumps:
      - from: "check-empty"
        condition_or_statement: "IF {model.name} == '' THEN default-empty"
        target: "default-empty"
    sinks: []
    joins: []          # no fan-in; compose has two inbound edges but runs once (not a graph.join)

  state_plan:
    input_paths: ["input.body.name (optional, string)"]
    model_paths:
      - "model.name : string — written by resolve-input (default for null/missing); overwritten by default-empty on the empty-string branch (controlled overwrite). Effective name. Inspection target."
      - "model.greeting_prefix : string — written by resolve-input; constant 'Hello, '."
    node_result_paths:
      - "check-empty.decision : string — 'default-empty' or 'compose' (math decision result)."
    output_paths:
      - "output.body.message : string — written by compose."
      - "output.header.content-type : string — written by resolve-input (application/json)."
    mapping_ownership:
      - node: "resolve-input"
        mappings:
          - "f:defaultValue(input.body.name, text(World)) -> model.name"
          - "text(Hello, ) -> model.greeting_prefix"
          - "text(application/json) -> output.header.content-type"
      - node: "default-empty"
        mappings:
          - "text(World) -> model.name"
      - node: "compose"
        mappings:
          - "model.greeting_prefix:concat(model.name) -> output.body.message"

  source_plan:
    api_fetchers: []
    dictionaries: []
    providers: []
    extensions: []
    flows: []
    mocks: []

  control_flow_plan:
    decisions:
      - node: "check-empty"
        rule: "model.name == '' -> default-empty (THEN); else -> compose (ELSE)."
        inspect: "check-empty.decision"
    loops_or_resets: []
    concurrency: []
    completion: "end node reached after compose; output.body.message is the response."

  failure_plan:
    terminal_errors: []     # no external dependency can fail
    handled_errors: []
    fallback_outputs:
      - "Null/missing name -> 'World' (resolve-input f:defaultValue)."
      - "Empty string name '' -> 'World' (check-empty THEN -> default-empty)."
      - "OUT OF SCOPE: whitespace-only name (e.g. '   ') -> stays as-is ('Hello,    '); no trim primitive without graph.js."

  test_handoff:
    scenarios:
      - "T-01 happy path named: {\"name\":\"Wes\"} -> {\"message\":\"Hello, Wes\"} (check-empty.decision == 'compose')"
      - "T-02 no name field: {} -> {\"message\":\"Hello, World\"} (model.name defaulted in resolve-input; decision 'compose')"
      - "T-03 empty name: {\"name\":\"\"} -> {\"message\":\"Hello, World\"} (check-empty.decision == 'default-empty')"
    inspection_points:
      - "model.name after resolve-input (T-01 'Wes'; T-02 'World'; T-03 '')"
      - "check-empty.decision (T-01/T-02 'compose'; T-03 'default-empty')"
      - "model.name after default-empty (T-03 -> 'World')"
      - "output.body.message (final, all scenarios)"
      - "output.header.content-type == 'application/json'"
    mock_data: []

  build_handoff:
    command_sensitive_notes:
      - "GREETING CONCAT — use the selector form 'model.greeting_prefix:concat(model.name)', NOT 'f:concat(text(Hello, ), model.name)'. f:concat (getValueFromSimplePlugin, DataMappingHelper.java:365) splits params on a raw comma and breaks on the comma inside 'Hello, '; the selector form (tokenizeConcatParameters, :523) is bracket-aware and comma-safe. The concat base must be a model path, not a text() constant — getConstantValue (:258) greedily reads to the last ')' and would mis-parse 'text(Hello, ):concat(...)'."
      - "EMPTY-STRING IF — 'IF: {model.name} == \"\"' relies on the logical-expression single-quote wrapping at GraphLambdaFunction.java:253; model.name must be present (resolve-input guarantees it) or the IF halts the node silently."
      - "MATH CANNOT BUILD STRINGS — GraphMath.compute (:168) returns only boolean/number (evalBoolean/evalNumber). Do not attempt a COMPUTE that yields the greeting; the string work stays in mappers."
      - "DECISION FAN-OUT — check-empty must keep exactly one natural outgoing edge (-> compose) and route the empty case as a named THEN target (default-empty). A bare next would fan out to all natural edges."
      - "Content-type header mapping is harmless if the engine also auto-sets it; it satisfies OUT-001 either way."
    source_help_mismatches:
      - "minigraph-syntax.md lists 'f:concat(field1, field2)' as the concat form, but that f: plugin form splits on raw commas and cannot carry a text constant containing a comma. The bracket-aware path is the selector form 'source:concat(params)'. Recommend the syntax doc note this."
    syntax_reference: "llms/graph-builder/minigraph-syntax.md"

  decisions:
    - id: "D-01"
      decision: "Decision-branch shape (linear spine + one empty-string branch); no fetch/join."
      source_category: "tradeoff"
      source_note: "Smallest shape that adds the math empty-check the user requested."
    - id: "D-02"
      decision: "graph.data.mapper does all string work (default, prefix stage, concat); graph.math contributes only the empty-string decision."
      source_category: "source-verified"
      source_note: "GraphMath.compute returns boolean/number only (GraphMath.java:168) — math cannot compose a string."
    - id: "D-03"
      decision: "Build the greeting via selector-concat 'model.greeting_prefix:concat(model.name)', prefix staged in resolve-input."
      source_category: "source-verified"
      source_note: "tokenizeConcatParameters is comma-aware (DataMappingHelper.java:523); f:concat and constant-base concat are not (lines 365, 258)."
    - id: "D-04"
      decision: "Expose model.name and check-empty.decision as inspection points for /test."
      source_category: "brief"
      source_note: "Requirements tests inspect model.name; decision proves the branch."
    - id: "D-05"
      decision: "Descope whitespace-only fallback from FAIL-001; cover null/missing + empty string only."
      source_category: "user-answer"
      source_note: "User chose 'use math' knowing math/mapper have no trim primitive; whitespace trim needs graph.js."

  open_questions: []

  gate_result:
    status: "pass"
    blockers: []
    carried_blockers: []
```

## Verify-the-design norm

`/design` introduced primitives the requirements brief did not name: `graph.data.mapper` (`f:defaultValue`, selector-`concat`) and `graph.math` (string-equality `IF`). The norm in [graph-design.md](../graph-design.md) (D11) recommends a `/graph-verify` pass on the design when such a primitive appears.

**User marked this verified ("ok, verified").** In support of that, the build-time preconditions of every introduced primitive were reproduced directly against the engine source during this phase — the `graph.math` boolean/number-only constraint ([GraphMath.java:168](../../system/minigraph-playground-engine/src/main/java/com/accenture/minigraph/skills/GraphMath.java#L168)), the logical-expression quote-wrapping ([GraphLambdaFunction.java:253](../../system/minigraph-playground-engine/src/main/java/com/accenture/minigraph/common/GraphLambdaFunction.java#L253)), and the concat comma-parsing split between the `f:` plugin and selector forms ([DataMappingHelper.java:365](../../system/event-script-engine/src/main/java/com/accenture/util/DataMappingHelper.java#L365) vs [:523](../../system/event-script-engine/src/main/java/com/accenture/util/DataMappingHelper.java#L523)). There are no external/deploy-time dependencies (the failure class the norm primarily guards against). The one real wall found — the `f:concat` comma footgun — is resolved in the design (selector-concat) and recorded in `build_handoff`.

## Plain-language handoff to `/build`

Build a 5-node graph. `root` (no skill) → `resolve-input` (`graph.data.mapper`: `f:defaultValue(input.body.name, text(World)) -> model.name`; `text(Hello, ) -> model.greeting_prefix`; `text(application/json) -> output.header.content-type`) → `check-empty` (`graph.math`: `IF {model.name} == '' THEN default-empty ELSE compose`) → on empty, `default-empty` (`graph.data.mapper`: `text(World) -> model.name`) → `compose` (`graph.data.mapper`: `model.greeting_prefix:concat(model.name) -> output.body.message`) → `end` (no skill). No sources, no joins. Heed the four build-handoff notes — especially the **selector-concat (not f:concat)** rule and the **single-natural-edge** rule on `check-empty`.
