# `/requirements` - Phase 1 Of The Graph Development Workflow

Status: canonical phase spec.

Canonical schema: the design-ready graph brief template in this file is the source of truth for `/requirements` output consumed by `/design`.

Scope rule: this document was rebuilt from source-observed MiniGraph behavior, generated artifacts, and the desired graph-authoring workflow. It intentionally avoids depending on older root-level narrative documentation. Rejoin the authoritative syntax surface only where `/design` or `/build` needs command grammar and engine constraints; use [minigraph-syntax.md](./minigraph-syntax.md) for that.

Purpose: guide a user from rough intent to a design-ready MiniGraph brief. The phase is complete only when the brief passes the embedded quality gate. The gate is not a later review pass; it is the definition of done for `/requirements`.

## Core Principle

`/requirements` gathers obligations, not graph syntax.

The user should not need to know which nodes, skills, relations, or command blocks will be used. The assistant must discover the behavior, data contracts, dependencies, mappings, control flow, failure rules, and test evidence that `/design` will later translate into graph topology.

The boundary is strict:

- `/requirements` defines what the graph must satisfy.
- `/design` chooses the topology and node responsibilities.
- `/build` emits and executes MiniGraph commands.
- `/test` proves runtime behavior through execution and state inspection.

`/design` should not be invoked while the output shape, required inputs, source contracts, failure behavior, or design-blocking decisions are still unknown.

## When To Run `/requirements`

Run this phase when:

- A user wants to author a new MiniGraph graph.
- A user wants to make a substantive change to an existing graph.
- A user has a rough idea and needs to determine whether it is a graph-shaped workflow.
- A user has generated artifacts and wants to turn them into a repeatable authoring workflow.

Do not require formal tickets, BRDs, ADRs, or architecture packets before starting. If such artifacts exist, they can be used as inputs, but the phase must still work from a plain-language user request.

## Inputs

At minimum, `/requirements` needs one of:

- A user description of the desired behavior.
- A sample request and response.
- A generated graph artifact.
- A sketch of source systems or data dependencies.
- A change request against an existing graph.

The assistant may ask for missing details during the phase. Lack of formal project documentation is not a reason to stop. Unknowns become open questions and are classified by what they block.

## Output

The required output is a design-ready graph brief.

The brief may live in one markdown file or be split into companion files when the feature is large. The minimum content is:

- Graph purpose.
- Invocation contract.
- Output contract.
- State contract.
- Source and capability catalog.
- Mapping rules.
- Control-flow requirements.
- Failure and degraded-mode behavior.
- Non-functional constraints.
- Test scenarios.
- Scope boundary.
- Decisions.
- Open questions with blocker classification.
- Completion gate result.

## Operating Protocol For The Assistant

The assistant should run `/requirements` as a guided conversation.

The steps below are a completion checklist, not a script to recite. The assistant may gather information adaptively and out of order, but the design-ready brief is incomplete until every step has been satisfied or explicitly blocked.

Use small batches of questions. After each answer, summarize:

- What is understood.
- What changed.
- What is still missing.
- Which question or decision most directly unblocks the brief.

The assistant must maintain a running distinction between:

- Observed framework behavior.
- User-provided facts.
- Existing artifact facts.
- Inferred choices.
- Explicit assumptions.
- Mock placeholders.
- Open questions.

The assistant should not ask the user to choose implementation details prematurely. For example, prefer "what data should be returned if the risk lookup fails?" over "should this be a `graph.math` fallback node?"

## Step checklist

Terse form of the Operating Protocol; each step is detailed in its `R#` section. Gather adaptively and out of order — the brief is incomplete until every step is satisfied or explicitly blocked.

- **R1** Name the outcome — purpose, caller, success definition, workflow category.
- **R2** Define the invocation contract — required/optional inputs, paths, sample payloads, missing-input rule.
- **R3** Define the output contract — mandatory/optional fields, error + degraded shapes.
- **R4** Build the state contract — `model.*`, node-local, constants, opaque pass-through.
- **R5** Inventory sources and capabilities — kind, request/response contract, auth, parallelism, mock.
- **R6** Capture mapping rules — input→model, source→model, model→output, derived/defaulted/repeated.
- **R7** Discover control flow — sequence, parallel, joins, decisions, per-item behavior.
- **R8** Define failure and degraded behavior for every meaningful dependency failure.
- **R9** Capture non-functional constraints — latency, concurrency, cache, TTL, security, observability.
- **R10** Define test scenarios — happy path, missing input, empty source, dependency failure, fallback, branch.
- **R11** Lock the requirements gate (see [Gate](#gate)) — decide whether `/design` may begin.

## Step R1 - Name The Outcome

Goal: understand the user-visible purpose before any graph structure is proposed.

Populates: `graph.name`, `graph.purpose`, `graph.workflow_category`, `decisions`, `open_questions`.

Ask:

- What should this graph accomplish?
- Who or what invokes it?
- What should the caller receive when it succeeds?
- Is the workflow read-only, write-oriented, enrichment, validation, routing, orchestration, or composition?
- What would make this graph clearly useful?

Capture:

- Graph name or working name.
- Purpose.
- Primary caller.
- Success definition.
- Workflow category.

Quality check:

- The assistant can explain the graph in one sentence without naming nodes.
- `workflow_category` is recorded as a `/design` hint. If the category is write-oriented, routing, validation, orchestration, or composition, capture any extra obligations it implies, such as idempotency, rollback, ordering, or side-effect handling.

## Step R2 - Define The Invocation Contract

Goal: define how data enters the graph.

Populates: `invocation`, `state_contract.inbound`, `tests`, `decisions`, `open_questions`.

Ask:

- How is the graph invoked: HTTP, another graph, a flow, a scheduler, or something else?
- What fields are required?
- What fields are optional?
- Where do inputs arrive: body, header, path parameter, or query?
- What sample input should be used for tests?
- What should happen when required input is missing or malformed?

Capture:

- Required inputs.
- Optional inputs.
- Input namespace paths such as `input.body.*`, `input.header.*`, `input.path_parameter.*`, and `input.query.*`.
- Validation expectations.
- Sample input payloads.

Quality check:

- Every required input has a path, source, type, and missing-data rule.

## Step R3 - Define The Output Contract

Goal: define the caller-facing result before internal state or topology is designed.

Populates: `output_contract`, `state_contract.outbound`, `failure_behavior.error_response_rules`, `decisions`, `open_questions`.

Ask:

- What exact response should the caller receive?
- Which fields are mandatory?
- Which fields can be missing, null, defaulted, or degraded?
- Should upstream technical details be hidden or exposed?
- What error response should the caller receive?
- Should response headers be set?

Capture:

- `output.body.*` contract.
- `output.header.*` contract, if needed.
- Mandatory and optional fields.
- Default values.
- Error and degraded response shapes.

Quality check:

- Every required output field has a source, derivation, default, or open question.

## Step R4 - Build The State Contract

Goal: define the graph's internal data model before designing nodes.

Populates: `state_contract`, `mappings`, `decisions`, `open_questions`.

MiniGraph behavior is state-machine driven. Requirements must describe the important state paths the graph reads, writes, computes, or passes through.

Ask:

- What internal facts should the graph accumulate under `model.*`?
- What is the primary entity key?
- Which fields are normalized business facts rather than raw source data?
- Which arrays or repeated items exist?
- What identifies an item in each array?
- Which values are temporary and node-local?

Capture:

| Group | Required Content |
|---|---|
| Inbound | Path, source, type, required/optional, notes |
| Internal model | `model.*` path, type, writer, readers, lifecycle |
| Node-local | Node path, type, created by, cleared by, notes |
| Outbound | `output.*` path, type, source or derivation, notes |
| Constants | Value, meaning, where used |
| Opaque pass-through | Payload path, source, destination, opacity note |

Do not choose node aliases in this step. If a user proposes names that are likely to become graph node aliases, defer naming validation to `/design` and [minigraph-syntax.md](./minigraph-syntax.md).

Quality check:

- Every important attribute touched by the graph has a known path, source, type, and lifecycle.

## Step R5 - Inventory Sources And Capabilities

Goal: identify external or internal capabilities the graph depends on.

Populates: `sources`, `control_flow`, `non_functional`, `decisions`, `open_questions`.

Ask:

- What APIs, systems, flows, or existing graphs provide data or behavior?
- For each source, what operation is needed?
- What request inputs does the source require?
- What response shape does it return?
- Does it require headers, auth, tenant context, correlation IDs, or feature hooks?
- Can calls happen in parallel?
- Are any sources optional or best-effort?

Capture:

- Source name.
- Source kind: HTTP API, flow, graph, static config, computed value, or user input.
- Request contract.
- Response contract.
- Auth or feature needs.
- Availability expectations.
- Parallelism and dependency notes.
- Mock contract if details are not yet known.

Quality check:

- Every required source has enough contract detail to design against, even if the endpoint or auth is mocked.

## Step R6 - Capture Mapping Rules

Goal: make data movement explicit.

Populates: `mappings`, `state_contract`, `output_contract`, `tests`, `decisions`, `open_questions`.

Ask:

- Which input fields map into internal state?
- Which source response fields map into internal state?
- Which internal state fields map into output?
- Are values renamed, defaulted, calculated, timestamped, formatted, or type-converted?
- Are mappings single-value or repeated for each item in a list?
- What happens when a source field is missing?

Capture:

- Input-to-model mappings.
- Source-to-model mappings.
- Model-to-output mappings.
- Derived value rules.
- Defaulting rules.
- Repeated mapping rules.

Quality check:

- The assistant can trace every required output field backward to input, source, constant, or computation.

## Step R7 - Discover Control Flow

Goal: define sequencing, branching, parallelism, joins, and repetition without choosing final topology yet.

Populates: `control_flow`, `failure_behavior`, `tests`, `decisions`, `open_questions`.

Ask:

- Which steps are unconditional?
- Which steps depend on data values?
- Which calls can run in parallel?
- Which steps must wait for earlier results?
- Where should parallel branches rejoin?
- Are there loops or per-item operations?
- Which decisions affect the user-visible output?

Capture:

- Sequential obligations.
- Parallel obligations.
- Join obligations.
- Decision rules.
- Repeated or per-item behavior.
- Ordering constraints.

Quality check:

- The assistant can describe the required workflow without yet naming implementation nodes.

## Step R8 - Define Failure And Degraded Behavior

Goal: prevent failure handling from being improvised during design or build.

Populates: `failure_behavior`, `output_contract.degraded_shape`, `output_contract.error_shape`, `tests`, `decisions`, `open_questions`.

Ask:

- Which failures should stop the graph?
- Which failures should produce partial or degraded output?
- Which missing values should be defaulted?
- Which dependency timeouts matter?
- Should a failed source be retried?
- What should the caller see when fallback occurs?
- What should be inspectable during testing?

Capture:

- Stop conditions.
- Continue-with-degraded-data conditions.
- Retry rules.
- Fallback rules.
- Error response rules.
- State flags or output flags that expose degraded behavior.

Quality check:

- Every meaningful dependency failure has an expected graph behavior.

## Step R9 - Capture Non-Functional Constraints

Goal: record constraints that influence design, build, and test.

Populates: `non_functional`, `sources`, `tests`, `decisions`, `open_questions`.

Ask:

- What is the latency budget?
- Are there concurrency limits?
- Are there rate limits?
- Is in-run caching allowed?
- What is sensitive and must not be logged?
- Are there model TTL expectations?
- What observability is needed during test and operation?

Capture:

- Latency and timeout expectations.
- Concurrency constraints.
- Cache constraints.
- Security and logging constraints.
- TTL expectations.
- Observability expectations.

Quality check:

- Each non-functional constraint is connected to a graph design, build, test, or deploy concern.

## Step R10 - Define Test Scenarios

Goal: make correctness testable before design starts.

Populates: `tests`, `failure_behavior`, `scope_boundary`, `decisions`, `open_questions`.

Ask:

- What is the happy path input and expected output?
- What missing-input cases matter?
- What empty-result cases matter?
- What upstream failure cases matter?
- What branch or fallback cases matter?
- Which state paths should be inspected to prove correctness?

Capture:

- Happy path scenario.
- Missing or malformed input scenario.
- Empty source response scenario.
- Dependency failure scenario.
- Fallback or degraded-mode scenario.
- Branch scenario.
- State inspection points.

Quality check:

- Every acceptance scenario is executable or can be made executable with mocks.

## Gate

Step R11 — Lock the requirements gate.

Goal: decide whether `/requirements` is complete and `/design` may begin.

Populates: `gate_result`, `scope_boundary`, `decisions`, `open_questions`.

The assistant must run the gate before advancing. If any required gate item fails, continue requirements gathering instead of invoking `/design`.

The gate consolidates the quality checks from Steps R1-R10. Do not maintain a separate, divergent checklist; if a gate item and a step quality check disagree, fix the step and gate together.

`gate_result.status` records only whether `/design` may begin. It is `pass` when nothing blocks requirements or design, even if build or deploy blockers remain. Record those remaining blockers under `gate_result.carried_blockers` with their mock-and-proceed plans. Never set `status: blocked` for a build- or deploy-only blocker — that is the mock-and-proceed case, and it is design-ready.

### Gate A - Scope Boundary

Every meaningful concern is classified:

| Verdict | Meaning |
|---|---|
| IN | This graph is responsible. |
| OUT | Another component or process is responsible. |
| PARTIAL | This graph owns only a defined slice. |
| DECISION | Ownership is not yet clear. |

Gate passes when no `DECISION` item blocks requirements or design.

### Gate B - Traceable Capture

Every decision has a source category:

| Category | Meaning |
|---|---|
| User answer | Confirmed by the user during `/requirements`. |
| Artifact | Taken from an attached/generated/project artifact. |
| Source-observed | Derived from framework source behavior. |
| Assumption | Explicitly assumed and reviewable. |
| Mock | Placeholder used to keep design/build moving. |

Gate passes when every design-relevant decision has a source category and unresolved assumptions are listed.

### Gate C - Open Questions

Every open question is classified by what it blocks:

| Class | Meaning | Required Action |
|---|---|---|
| Blocks requirements | Requirements are incoherent without this answer. | Resolve now. |
| Blocks design | Design cannot choose topology or responsibilities safely. | Resolve before `/design`. |
| Blocks build | Design can proceed, but commands cannot be finalized. | Mock if possible; resolve before final build. |
| Blocks deploy | Build and test can proceed, but production use is blocked. | Mock and carry as known risk. |

Gate passes when no open question blocks requirements or design.

### Gate D - Step Quality Checks

The quality checks in R1-R10 are the source of truth for completeness. Gate D passes when each check is satisfied or has a blocker recorded in `open_questions`.

If a quality check fails because required behavior is unknown, classify the blocker as requirements or design. If it fails because a concrete implementation detail is unavailable but the behavior is clear, classify it as build or deploy and apply the mock-and-proceed rule.

## Mock-And-Proceed Rule

Use a mock when an unknown blocks build or deploy but does not block requirements or design.

For each mock, record:

- What is mocked.
- The placeholder value or shape.
- Why it does not change graph topology.
- What must replace it later.
- Which phase is blocked until it is replaced.

Good mock candidates:

- Provider URL.
- Auth feature name.
- Header value.
- Source response fixture.
- Error envelope shape.

Poor mock candidates:

- Unknown output contract.
- Unknown graph responsibility boundary.
- Unknown failure semantics for a required dependency.
- Unknown primary entity key.

Those usually block requirements or design.

## Requirement IDs

Every design-relevant obligation in the brief gets a stable **requirement ID** so `/design` can trace each requirement to a design element without re-deriving it. IDs identify *obligations*, not implementation nodes — one requirement may later be satisfied by several design elements. Capture the obligations `/design` must trace; this is not a completeness checklist of every sentence in the brief.

Use a category prefix + zero-padded number:

| Prefix | Obligation kind |
|---|---|
| `INV` | Invocation / input contract |
| `OUT` | Output contract |
| `SRC` | Source / dependency |
| `MAP` | Mapping / data movement |
| `FLOW` | Control flow (sequence, parallel, join, branch) |
| `FAIL` | Failure / degraded behavior |
| `NFR` | Non-functional constraint |
| `TEST` | Test / acceptance scenario |

Record them in the `requirements` block of the brief (schema below). `/design` should echo each `id` under `requirement_traceability`, and its gate (see [graph-design.md](./graph-design.md)) requires every requirement to map to at least one design element (node, edge, state path, source-plan item, or failure-plan item) — any unavailable real dependency carried as an explicit blocker. This is a convention, not machinery — keep it lightweight; do not rebuild a string-matching doc checker around it. *(A filled design-spec example does not yet exist, so the brief → design round-trip is not yet demonstrated end-to-end.)*

## Design-Ready Brief Template

This template is the canonical `/requirements` output schema. Scratchpad notes and planning docs must reference this schema instead of restating their own variant.

```yaml
graph:
  name: "{working-graph-name}"
  purpose: "{one-sentence behavior, without node names}"
  workflow_category: "read | write | enrichment | validation | routing | orchestration | composition | other"

requirements:
  - id: "INV-001"
    statement: "{design-relevant obligation, one line, without node names}"
    source_category: "user-answer | artifact | source-observed | assumption | mock"
    source_note: "{short trace}"

invocation:
  trigger: "http | graph | flow | scheduler | other"
  required_inputs:
    - path: "input.body.example_id"
      type: "string"
      source: "caller"
      missing_rule: "reject | default | ignore | open-question"
  optional_inputs: []
  sample_inputs: []

output_contract:
  body:
    - path: "output.body.example"
      type: "object"
      required: true
      source_or_derivation: "model.example"
  headers: []
  error_shape: "{describe caller-facing error}"
  degraded_shape: "{describe partial-success output, if any}"

state_contract:
  inbound: []
  model: []
  node_local: []
  outbound: []
  constants: []
  opaque_pass_through: []

sources:
  - name: "{source-name}"
    kind: "http | flow | graph | static | computed | user-input"
    request_contract: []
    response_contract: []
    auth_or_feature_needs: []
    dependency_notes: []
    mock: null

mappings:
  input_to_model: []
  source_to_model: []
  model_to_output: []
  derived_values: []
  repeated_mappings: []

control_flow:
  sequential: []
  parallel: []
  joins: []
  decisions: []
  repeated_steps: []

failure_behavior:
  stop_conditions: []
  degraded_conditions: []
  retry_rules: []
  fallback_rules: []
  error_response_rules: []

non_functional:
  latency: null
  concurrency: null
  caching: null
  ttl: null
  logging_security: []
  observability: []

tests:
  - id: "T-01"
    name: "happy path"
    input: "{describe or link fixture}"
    expected_output: "{describe}"
    inspect: []

scope_boundary:
  in: []
  out: []
  partial: []
  decisions: []

decisions:
  - id: "D-01"
    decision: "{decision text}"
    source_category: "user-answer | artifact | source-observed | assumption | mock"
    source_note: "{short trace}"

open_questions:
  - id: "OQ-001"
    question: "{question}"
    blocks: "requirements | design | build | deploy"
    closure_plan: "{how it will be answered or mocked}"

gate_result:
  # status reflects ONLY whether /design may begin:
  #   pass    = no open question blocks requirements or design
  #             (build/deploy items may remain, listed under carried_blockers)
  #   blocked = at least one open question blocks requirements or design
  status: "pass | blocked"
  blockers: []          # items blocking requirements or design; MUST be empty when status is pass
  carried_blockers:     # build/deploy blockers carried forward; the referenced open question already holds `blocks` and `closure_plan`
    - question_id: "OQ-001"
      mock_used: "{placeholder value or shape standing in}"
```

## Final Completion Rule

`/requirements` is complete when:

1. The design-ready graph brief exists.
2. The embedded gate passes.
3. No open question blocks requirements or design.
4. Build/deploy blockers have explicit mock-and-proceed plans where applicable, recorded under `gate_result.carried_blockers`.
5. The assistant can state, in plain language, what `/design` must satisfy.
6. The obligations `/design` must trace are captured as requirement IDs in the `requirements` catalog.

If any of these are false, stay in `/requirements`.

## Worked Example

See [examples/customer-360-requirements-brief.md](examples/customer-360-requirements-brief.md) for a filled design-ready brief using the canonical schema.