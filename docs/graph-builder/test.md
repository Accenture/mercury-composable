# `/test` — Phase 4 Of The Graph Development Workflow

Status: canonical phase spec for `/test`. Replaces the provisional `/test` gate in [workflow.md](./workflow.md).

Purpose: prove a built graph's runtime behavior through execution and state inspection — run each required scenario, assert the output contract, confirm the important state transitions, and record defects and accepted limitations.

Written from execution experience (see [evidence/dogfood-customer-360.md](./evidence/dogfood-customer-360.md)). Engine-behavior claims cite the evidence verdict that backs them; an untagged claim is a convention, not a verified fact.

## Boundary

- `/test` **owns**: executing the scenarios from `/requirements` against the built graph, asserting outputs, inspecting intermediate state, and reporting verdicts/defects.
- `/test` does **not** own: building or rebuilding the graph (a structural defect goes back to `/build`); choosing topology or failure semantics (a behavior the design never specified goes back to `/design`/`/requirements`). `/test` proves what was specified — it does not invent the spec.
- Execution + inspection are driven by [companion.mjs](./companion.mjs); syntax by [minigraph-syntax.md](./minigraph-syntax.md).

## Inputs

- The built, exported graph from [build.md](./build.md) (re-`import graph from {name}` into a fresh session if needed).
- The test scenarios from the `/requirements` brief's `tests[]` block (happy path, missing/malformed input, empty-source, dependency failure, fallback/degraded, branch).
- The inspection points from the `/design` artifact's `test_handoff.inspection_points` (the design's chosen checkpoints — expected output paths, intermediate `model.*`/`{node}` paths, decision branches, and join behavior all live there).
- Mocks for unavailable real sources (stub endpoint — [evidence/stub-server.mjs](./evidence/stub-server.mjs)).

## Output artifact and schema

A **test report**:

```yaml
test_report:
  graph: ""
  build_artifact: ""
  session_id: ""
  scenarios:
    - id: "T-01"                 # mirror the /requirements test id
      name: "happy path"
      seed: "instantiate seeds, e.g. text(P-10001) -> input.body.person_id"
      expected: "output.body.* the contract requires"
      observed: "what inspect/--expect actually returned"
      inspected: []              # intermediate paths checked (model.*, {node}.status, …)
      verdict: "pass | fail | blocked-on-mock"
  state_transitions_confirmed: []  # important model.* / node transitions proven by inspection
  defects: []                       # id, scenario, expected vs observed, suspected phase (design/build)
  accepted_limitations: []          # e.g. extension success path not testable without a deployed sub-graph
  mocks_used: []                    # what was mocked + the carried deploy blocker
  gate_result:
    status: "pass | blocked"
    blockers: []
```

## Operating protocol

Per scenario: **seed → run → read state**. Never trust the `"accepted"` response — `companion.mjs` forces the inspect-after-run, and you assert explicitly:

1. `instantiate graph` with the scenario's seed (array inputs via repeated `… -> model.key[]`). Confirm the instance is live (companion hard-fails if a bad seed dropped it). For a **missing-required-input** scenario, omit that field's seed line — a bare `instantiate graph` with no seeds produces a live empty `input.body: {}` (not a `404`), so the instance check still passes (verified).
2. `run` with `--expect <key>` for each output the contract requires — a declared-but-missing key is an unambiguous failure. `--expect` resolves scalar leaves via their parent container.
3. `inspect` the design's intermediate checkpoints to prove *how* the result happened, not just that it appeared: `model.*` working paths, `{node}.result`/`.status`/`.error`, and the degraded/branch flags. For a failure scenario, confirm the failure state (e.g. `{fetcher}.status`, `model.degraded=true`) — not only the final output.
4. Record observed vs expected and a verdict.

Notes from execution (verified):
- `seen` (the traversal path) is **WebSocket-only** — there is no REST equivalent. Infer the path from inspected state; a missing downstream write means traversal stopped upstream.
- A `graph.join` that never proceeds (no `output.body`, all branches ran) indicates the deadlock hazard — a `/build`/`/design` defect, not a test flake.
- For sources you cannot reach, mock via a stub that produces the **same state paths** the real source would; mark the scenario `blocked-on-mock` only if the mock can't represent it.

## Step checklist

- **T1** Load the built graph into a clean session; confirm structure matches the build export.
- **T2** Run the **happy path**; assert every required `output.body` path; inspect the key `model.*` transitions.
- **T3** Run each **required failure/edge scenario** from `/requirements` (missing input, empty source, dependency failure, fallback/degraded, branch); assert the degraded/error contract and the failure state paths.
- **T4** Confirm **important state transitions** named in the design's `test_handoff` (e.g. fallback set `model.degraded=true`; a convergence node ran; a `for_each` produced N results).
- **T5** Record **defects** with suspected owning phase, and **accepted limitations** (things not testable in this environment).
- **T6** Run the gate.

## Gate

`/test` passes only when:

- The **happy path passes** — every required `output.body`/`output.header` path is present and correct.
- Every **required failure and edge scenario** from `/requirements` passes (produces the specified degraded/error contract), or is explicitly recorded as `blocked-on-mock`/accepted-limitation with rationale.
- The **output contract is satisfied** for every scenario (asserted via `--expect`, not eyeballed).
- The **important state transitions** from the design are confirmed by inspection — not inferred from the final output alone.
- **Remaining risks are explicit**: every defect has an owning phase; every accepted limitation has a reason and (if it blocks production) a carried deploy blocker.

A scenario that fails because the *graph* is wrong is a defect routed to `/build` or `/design`; a scenario that fails because the *spec* was silent is routed to `/requirements`. `/test` does not invent the missing behavior.

## Mock or placeholder rule

Mock unavailable real sources with a stub that emits the same state shape (status, body, error) the real source would, so the test exercises the real traversal and only the data is synthetic. Record each mock and its deploy blocker. A scenario that can only be proven against the real source (e.g. a `graph.extension` call into a not-yet-deployed sub-graph) is an **accepted limitation**, recorded with the deploy blocker — not a silent pass.

## Completion rule

`/test` is complete when the test report exists, the happy path and all required scenarios have verdicts (pass / blocked-on-mock / accepted-limitation), the output contract is asserted for each, the important state transitions are confirmed, defects carry an owning phase, and `gate_result.status: pass`. Passing `/test` is the precondition for the deployment boundary in [workflow.md](./workflow.md).
