# MiniGraph Requirements Workflow Notes

Status: superseded scratchpad. The canonical `/requirements` contract now lives in `llms/graph-builder/requirements-gathering.md`.

Scope rule: this note records how the `/requirements` workflow evolved. Do not use the brief shape below as an implementation contract; it has been superseded by the canonical design-ready graph brief schema in `llms/graph-builder/requirements-gathering.md`.

## Core Idea

`/requirements` should be a guided requirements interview that produces a design-ready graph brief. It should not ask the user to think in node syntax first. Its job is to clarify behavior, contracts, dependencies, mappings, failure semantics, and test evidence so a later `/design` step can translate that intent into graph topology and commandable node definitions.

The useful boundary is:

- `/requirements`: gather intent and contracts.
- `/design`: convert contracts into nodes, skills, mappings, and edges.
- `/build`: generate and execute MiniGraph commands.
- `/test`: instantiate, run, inspect, and revise.

## What Requirements Gathering Should Produce

The output should be a structured graph brief containing enough information to design without guessing:

- Graph name and purpose.
- Caller or triggering mechanism.
- Required and optional input contract.
- Expected output contract.
- Internal `model.*` state contract.
- Source systems, APIs, flows, or reusable graphs.
- Source request and response examples.
- Field mapping rules.
- Sequential, parallel, conditional, and repeated steps.
- Join points.
- Retry, fallback, degraded-mode, and error behavior.
- Non-functional constraints such as latency, concurrency, cache, TTL, logging, and security.
- Test cases and state inspection points.
- Open questions.

## Step-By-Step Interview

### 1. Name The Outcome

Start with the business or user outcome, not the graph shape.

Useful questions:

- What should this graph accomplish?
- Who or what will call it?
- What should the caller receive when it succeeds?
- Is this read-only, write-oriented, enrichment, routing, validation, orchestration, or composition?

Capture:

- Purpose.
- Primary actor or caller.
- Success definition.
- Read/write nature.

### 2. Define The Invocation Contract

Identify how data enters the graph.

Useful questions:

- How is the graph invoked: HTTP request, another graph, scheduled flow, or internal flow?
- What input fields are required?
- Where do inputs arrive: body, header, path parameter, or query?
- What sample inputs should be used for tests?
- What should happen if required input is missing?

Capture:

- Required inputs.
- Optional inputs.
- Input namespace hints such as `input.body.*`, `input.header.*`, `input.path_parameter.*`, and `input.query.*`.
- Validation and fallback expectations.
- Sample request payloads.

### 3. Define The Output Contract

Clarify the caller-facing result before discussing graph internals.

Useful questions:

- What exact response shape should the caller receive?
- Which fields are mandatory?
- Which fields can be missing, null, defaulted, or degraded?
- Should upstream technical details appear in the response?
- What status or error shape should failure responses use?

Capture:

- Target response schema.
- Field-level rules.
- Default values.
- Error and degraded response model.

### 4. Inventory Data Sources And Capabilities

Discover likely provider, dictionary, fetcher, extension, or flow candidates without naming them too early.

Useful questions:

- What systems, APIs, databases, flows, or existing graphs provide the data?
- For each source, what URL, route, graph id, or flow id is used?
- What method or operation is needed?
- What inputs does the source require?
- What does the response look like?
- Does it require auth, headers, tokens, tenant IDs, correlation IDs, or feature hooks?
- Can calls run in parallel, or must some wait for prior results?

Capture:

- Source catalog.
- Source request contracts.
- Source response examples.
- Dependencies between sources.
- Auth or feature requirements.
- Parallelism opportunities.

### 5. Identify The Canonical Internal Model

MiniGraph becomes easier to design when the workflow has a clear internal `model.*` shape. The model should represent normalized facts, not raw provider details.

Useful questions:

- What internal facts should this graph accumulate?
- What is the main entity key?
- Which fields are raw source data, and which are normalized business facts?
- Do multiple sources contribute to the same conceptual object?
- Are arrays involved? If so, what identifies an item?

Capture:

- `model.*` contract.
- Entity keys.
- Normalized field names.
- Array and list semantics.
- Cross-source merge rules.

### 6. Capture Mapping Rules

Turn inputs, source responses, derived values, and final output into explicit mapping rules.

Useful questions:

- Which field from each source becomes which internal model field?
- Do fields need renaming, defaulting, arithmetic, timestamps, or formatting?
- Should missing source values be ignored, defaulted, or treated as errors?
- Are there calculated fields?
- Do mappings happen once, or for each item in a list?

Capture:

- Input-to-model mappings.
- Source-to-model mappings.
- Model-to-output mappings.
- Function/plugin needs such as default values, timestamps, arithmetic, or type conversion.
- `for_each` needs.

### 7. Discover Control Flow

Only after contracts and mappings are known should the assistant ask about graph flow.

Useful questions:

- Which steps are unconditional?
- Which steps depend on data values?
- Are there branches, retries, fallbacks, or degraded paths?
- What should happen when a fetcher fails?
- Should failed branches stop the graph or continue with partial data?
- Where do parallel branches rejoin?

Capture:

- Sequential steps.
- Parallel branches.
- Join points.
- Decision rules.
- Retry, fallback, and degradation rules.
- Candidate math, JavaScript, join, exception, extension, or mapping behavior.

### 8. Define Non-Functional Requirements

These constraints influence fetcher settings, graph properties, tests, and operational safety.

Useful questions:

- What is the expected latency budget?
- Can upstream responses be cached during graph execution?
- Are there concurrency limits?
- Are there rate limits?
- Are there model TTL expectations?
- What should be visible during test/debug mode?
- What data must not be logged?

Capture:

- Timeout and TTL expectations.
- Concurrency limits.
- Security and logging constraints.
- Caching expectations.
- Observability requirements.

### 9. Generate Test Scenarios

Requirements are not complete until they imply verifiable tests.

Useful questions:

- Give me a happy-path input and expected output.
- What edge cases matter?
- Which upstream failures should be simulated?
- What missing, empty, or null values matter?
- What proves this graph is correct?

Capture:

- Happy path test.
- Missing input test.
- Partial upstream failure test.
- Empty result test.
- Branch and fallback test.
- Expected state inspection points.

## Completion Criteria

`/requirements` is complete when the assistant can answer these questions without guessing:

- What starts the graph?
- What must be in `input.*`?
- What must be in `output.*`?
- What internal `model.*` state is needed?
- Which external or internal capabilities provide data or behavior?
- Which steps are sequential, parallel, conditional, or repeated?
- What happens on missing data or upstream failure?
- What test cases prove correctness?

## Design-Ready Brief Shape

Superseded. The schema was removed from this scratchpad to avoid contract drift. Use `llms/graph-builder/requirements-gathering.md#design-ready-brief-template`.

## Conversation Style For `/requirements`

The assistant should ask a small batch of high-value questions at a time, infer what it can, and summarize after each answer:

- What I understand.
- What is still missing.
- The next questions that unblock graph design.

It should avoid asking users to choose implementation details prematurely. The central question is not "what nodes do you want?" but "what behavior, data contracts, dependencies, and failure semantics do you need?"