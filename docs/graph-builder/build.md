# `/build` — Phase 3 Of The Graph Development Workflow

Status: canonical phase spec for `/build`. Replaces the provisional `/build` gate in [workflow.md](./workflow.md).

Purpose: lower a graph design specification into executable MiniGraph commands, build the live graph one verified mutation at a time, smoke-test that it instantiates and runs, and export it — without re-choosing topology (that is `/design`) or proving full runtime behavior (that is `/test`).

This spec was written from execution experience (see [evidence/dogfood-customer-360.md](./evidence/dogfood-customer-360.md)), not assertion. Engine-behavior claims cite the evidence verdict (or the [minigraph-syntax.md](./minigraph-syntax.md) tag) that backs them; an untagged claim is a convention, not a verified fact.

## Boundary

- `/build` **owns**: command emission order, one-mutation-at-a-time structural verification, build-time footgun avoidance, a single happy-path smoke run, and export.
- `/build` does **not** own: topology, primitive selection, or state ownership (those are fixed by `/design` — if the design is wrong, return to `/design`, don't improvise); nor full scenario/failure proof (that is `/test`).
- Syntax is owned by [minigraph-syntax.md](./minigraph-syntax.md); execution + verification by [companion.mjs](./companion.mjs). This spec references them; it does not restate grammar.

## Inputs

- The graph design specification from [graph-design.md](./graph-design.md) with `gate_result.status: pass`.
- A live engine session id (the `companion.mjs` driver targets `$MINIGRAPH_HOST`, default `localhost:8085`).
- For any unavailable real source: a mock plan from the design's `source_plan.mocks` (a stub endpoint — see [evidence/stub-server.mjs](./evidence/stub-server.mjs) — or a mock provider).

## Output artifact and schema

A **build log** plus the **exported graph**. The build log records every command, its verification result, the smoke run, and any deviation:

```yaml
build_log:
  graph: ""
  design_artifact: ""
  session_id: ""
  host: "http://localhost:8085"
  commands:                       # one row per mutating command, in send order
    - seq: 1
      command: "create node root …"
      verified: "node \"root\" present"   # companion.mjs ✓ line, or the failure + fix
  smoke_test:
    instantiated: "input.body seeded: {…}"
    ran: "output.body (or --expect keys) observed: {…}"
  exported_as: ""
  deviations_from_design: []      # anything built differently than the design said, and why
  mocks_used: []                  # what was mocked + the deploy blocker it carries
  gate_result:
    status: "pass | blocked"
    blockers: []
```

## Operating protocol

Drive every command through `companion.mjs send` so each mutation is auto-verified (re-fetch + assert) before the next. **`"status":"accepted"` is not success** — the helper's `✓ verified` line is. If a command fails verification, stop and fix it before sending anything else; never build on an unverified graph.

Build in dependency order so every referenced alias exists when it is referenced:

1. **`root` and `end`** first (a runnable graph needs both).
2. **Skill nodes** in the design's node inventory: mappers, evaluators (`graph.math`/`graph.js`), fetchers, joins, fallback/convergence mappers, islands. (`with type` is optional for skill nodes — the `skill` is load-bearing — but set it for readability.)
3. **Source support nodes** for each fetcher: the `Provider` and `Dictionary` nodes it needs (a fetcher requires at least one dictionary; a dictionary needs a provider).
4. **Edges**, after the nodes they connect exist:
   - `connect {dictionary} to {provider} with provider` for each source cluster (the island "container" is optional — not required).
   - Natural traversal edges for the main flow.
   - Join predecessor edges (`with wait`) — and verify the join hazard is avoided (below).
   - Fan-out edges from a no-skill node when the design fans to parallel branches.
5. **Branch / exception / join wiring checks** (the parts P0 proved are easy to get wrong):
   - Every `THEN:`/`ELSE:` alias target in a `graph.math`/`graph.js` statement must exist as a node. A named target overrides natural traversal and does **not** need a natural inbound edge.
   - Every `exception={alias}` handler must exist as a node.
   - **Join hazard:** `graph.join` waits for *all* backward-linked predecessors and `.sink`s forever if one never completes. A conditional/optional branch must converge through a single always-completing node before the join — never wire a branch that can be skipped directly as a join predecessor. (Verified: doing so deadlocks the run with no output.)
6. **Smoke run**: `instantiate graph` with a happy-path seed, then `run` (use `--expect` for the design's output keys). Confirm the instance is live and the run produces the expected output. This is a *buildability* check — full scenarios are `/test`.
7. **Export**: `export graph as {name}`. `companion.mjs` does **not** verify `export` (it is not a mutating verb), so the `accepted` response is not proof. Confirm persistence with an `import graph from {name}` round-trip — but note the REST driver cannot mint a fresh session (session ids are WebSocket-minted, see minigraph-syntax.md). So do this **last** (after the smoke run): in the same session, `session reset` then `import graph from {name}`, and verify the node/connection count returns. This clears and restores the live graph, which is why it is the final step. (Verified.)

Build-time footguns to avoid (each carries its own verification tag in [minigraph-syntax.md](./minigraph-syntax.md) — some from P0, some from the help surface):
- One command per request; LF line endings; never send a literal `...` line (companion.mjs guards these).
- No `number()` constant — `int()`/`double()`. A bad seed drops the whole instance silently.
- Null detection is a **mapper** step (`:boolean(null=true)`), not an inline `graph.math` `MAPPING:` statement; a `graph.math` IF that references an unresolved value halts the node silently.
- `update node` replaces the property set — re-specify list properties in full.

## Step checklist

- **B1** Accept the design: `gate_result.status: pass`; node inventory, edges, state plan, source plan, and failure plan are present. If not, return to `/design`.
- **B2** Resolve mocks: for each unavailable real source, stand up the stub/mock from the design's `source_plan.mocks` and record the carried deploy blocker.
- **B3** Emit and verify nodes (root/end → skills → providers/dictionaries), one at a time.
- **B4** Emit and verify edges (provider links → main flow → join `wait` edges → fan-out), one at a time.
- **B5** Verify branch/exception/join integrity: every jump/exception target exists; every join predecessor always completes in the scenarios that reach it.
- **B6** Smoke run: instantiate (happy seed) → run (`--expect` design output keys) → confirm output. Record in the log.
- **B7** Export the graph; confirm persistence **last** (after the smoke run) via an in-place `session reset` + `import graph from {name}`, verifying the node/connection count returns (companion does not verify `export`; the REST driver cannot mint a scratch session). Record `exported_as`.
- **B8** Record deviations from the design and run the gate.

## Gate

`/build` passes only when:

- Every design node and every design edge is present and **structurally verified** (companion.mjs `✓` for each), with no unverified mutation in the log.
- Every branch target, `exception` handler, and join predecessor named in the design exists as a node.
- No join has a backward-linked predecessor that can be skipped on a path that reaches it (hazard check).
- The graph **instantiates** (live instance, seeded `input.body`) and **runs once on the happy path**, producing the design's output keys.
- The graph **exports**, confirmed by an `import` round-trip (companion does not verify `export`; over REST this is an in-place `session reset` + re-import done as the final step).
- Every deviation from the design is recorded; if a deviation changes topology or responsibility, `/build` is **blocked** — return to `/design` rather than absorb it.
- Mocked sources are recorded with their carried deploy blockers.

A `/build` `blocked` status means the design could not be lowered as written; do not paper over it.

## Mock or placeholder rule

When a real dependency is unavailable, build against the design's mock (stub endpoint or mock provider), and record: what is mocked, the placeholder it returns, and the deploy blocker that must replace it. The mock must produce the same state paths the real source would, so `/test` and the eventual real source are interchangeable. Do not delete the dependency or pretend the mock is the real source.

## Completion rule

`/build` is complete when the build log exists, every mutation is verified, the graph instantiates + runs once on the happy path + exports, deviations and mocks are recorded, and `gate_result.status: pass`. Full scenario and failure proof is handed to `/test`; do not claim correctness here — only that the graph is built, instantiable, and runnable.
