# Field follow-ups: graph-scoped workflow state + generic exception context

Status: RATIFIED by Eric 2026-08-10 (rulings R1–R5 below) — implementation in progress
Driver: field team demo review 2026-08-10 — a complex multi-suspension use case built on
the first iteration (checkpoint-only) of suspend/resume surfaced two structural asks.
Scope: Java reference engine first, then full Rust lock-step (regression-critical field core).

---

## Rulings (Eric, 2026-08-10)

- **R1** — No back-compat machinery for the store-key change: suspend/resume use cases are
  in the dev phase, no legacy production data. Document the breaking change in release notes.
- **R2** — Business-cid propagation everywhere: flow→subflow (already shipped,
  TaskExecutor), flow→graph (rides sub-flow launch), graph→subgraph and graph→subflow
  (this change, GraphExtension). Consistency improves the overall system.
- **R3** — Error-context naming follows Event Script parity: `error.source`, `error.code`,
  `error.message`, plus `error.stack` when available.
- **R4** — Reserve the node alias `error` at the gate: a small price to pay.
- **R5** — Orchestrator pattern: illustrate and validate with a unit test; mention the
  pattern in docs. No tutorial-14 change (keep it straightforward).

## Feature A — graph-scoped store reference + business-cid propagation

### Problem (field-confirmed, two independent root causes)

1. The store key is `graph:state:{cid}` (RedisStateConnection.KEY_PREFIX + cid). One
   business cid = one record globally: collisions across domains sharing a Redis, and
   between a parent graph and its subgraphs.
2. GraphExtension invokes a subgraph as a separate flow instance with
   `setCorrelationId(util.getUuid())` and no business-cid header, so the subgraph's
   `model.cid` is a random UUID per call — its resume can never find a record even with
   scoped keys.

### Design

- **Persistence contract gains `graph`** (the running graph's id, from
  `graphInstance.graphId`):
  - put body: `{cid, graph, node, ttl, model, seen, run}`
  - get body: `{cid, graph}`
  - Key composition stays store-internal. The Redis reference implementation composes
    `graph:{graph_id}:{cid}` (Eric's format) and REQUIRES `graph` (fail-fast teaching;
    no cid-only fallback per R1). FileStateStore (test mock) mirrors.
- **GraphExtension stamps `EventScriptManager.BUSINESS_CORRELATION_ID`** from the parent's
  `model.cid` on the forward event — both branches (single call + for_each fork-join),
  both protocols (`graph://` and `flow://`). This mirrors TaskExecutor's sub-flow launch
  ("sub-flow inherits the parent's business correlation-id").
- **Deliberate non-change:** the extension call does NOT set the `parent` header — an
  extension is a request/response delegation, not a nested lifecycle (`model.parent` and
  teardown listeners stay flow-only semantics).
- **Self-containment is now by construction:** a record written under one graph id is
  invisible to every other graph. You cannot suspend in one subgraph and resume in another.
- **Direct path resume:** a subgraph is a deployed graph, so a suspended path may also be
  resumed directly via `POST /api/graph/{subgraph-id}` with the same cid (document with
  the cid-as-capability note).
- **Documented caveat:** same subgraph × same cid = one record. A for_each fan-out of a
  suspending subgraph under one cid would last-write-wins; invoke a suspendable subgraph
  once per cid per run.
- **Breaking change (release note):** records persisted under the old key are invisible
  after upgrade — resume behaves as fresh (consistent with "absent and expired look the
  same"). Custom stores must adopt the `graph` field.

### Orchestrator pattern (R5)

Parent graph = orchestrator; each processing path = a subgraph that may suspend/resume
independently under `graph:{subgraph_id}:{cid}`. A suspending subgraph returns
`{"type": "suspended", "cid": ...}` (or its staged output) to the parent extension node's
`result`, so the parent can route on it — or pause itself under its own record. Validated
by unit test; mentioned in the workflow-suspension guide; tutorial-14 untouched.

## Feature B — generic exception handler context

### Problem (field-confirmed)

`exception=` routing already spans the whole family — graph.api.fetcher, graph.task
(pinned by unit-test-task-4), graph.extension, and the suspend/resume store skills — and a
handler may continue to further nodes (pinned by unit-test-join-retry). But the engine
stages only `{failing-node}.status` / `{failing-node}.error`, so a handler's input mapping
must name the failing node statically → one cloned handler per fetcher.

### Design

- **Walker-staged generic context** at the two exception choke points
  (GraphExecutor.handleSkillResponse, GraphTraveler.handleSkillSuccess), immediately
  before jumping to the handler:
  - `error.source`  = failing node alias
  - `error.code`    = `{node}.status` (int)
  - `error.message` = `{node}.error`
  - `error.stack`   = `{node}.stack` when present
  One staging site per walker covers every skill with no per-skill routing edits. For an
  extension node, `source` is the extension node in the parent graph; a subgraph's internal
  failures route to the subgraph's own handlers (composes with self-containment).
- **`{node}.stack`**: the skills' setError family (GraphTask, GraphApiFetcher,
  GraphExtension, GraphStateSkill) additionally stages `response.getStackTrace()` when the
  failing envelope carries one (Event Script parity: TaskExecutor stages `stack`).
- **`{node}.status` / `{node}.error` staging is unchanged** — existing handlers keep
  working.
- **Reserved alias `error` (R4):** new GraphModelValidator whole-graph rule (shared by the
  CompileGraph gate and the dry-run pre-run check): a node aliased `error` is rejected
  with a teaching error — the alias is reserved for the exception context namespace;
  suggest renaming (e.g. `on-error`) and mention `inspect error`.
- **`inspect error` needs no engine change:** the inspect command is a raw state-machine
  viewer (`stateMachine.getElement(key)`), so `inspect error` / `inspect error.source`
  work by construction once staging lands — same mechanics as `inspect model`. Docs teach it.
- **Documented rule:** a shared handler is jumped into at most once per run (nodeSeen
  dedup) — concurrent-branch failures collapse into the first jump; deliberate re-entry
  loops use the RESET idiom (see unit-test-join-retry).
- `exception=suspend` stays rejected (existing R7 gate rule).

## Test plan (Java)

- Redis module: graph-scoped key composition; two records same cid × different graph ids
  are isolated (consume one leaves the other); missing `graph` rejected; existing
  suite updated to the new contract.
- Engine: suspend persistence-envelope pin (`graph` field present); FileStateStore scoping.
- Orchestrator e2e: parent graph (graph.extension) → subgraph suspends at a checkpoint →
  parent receives suspended reply; re-invoke with same cid → subgraph resumes past the
  checkpoint; asserts subgraph saw the parent's business cid (cid propagation).
- Generic handler e2e: one island-anchored handler serving two failure sources — a
  graph.task with `task=async.http.request` (HTTP 404) and another failing node — asserts
  `error.source` distinguishes them, plus code/message (+stack presence where thrown).
- Gate: node aliased `error` rejected in both lanes with the teaching message.
- Dry-run twin: traveler stages error.*; `inspect error` returns the context.
- Back-compat: unit-test-task-4 and unit-test-join-retry pass unmodified.

## Docs plan

- workflow-suspension guide: store contract/key format update + BREAKING note;
  "Orchestrator pattern" section (per-graph records, direct subgraph resume, for_each
  caveat, suspended-reply routing in the parent).
- help pages: graph-api-fetcher / graph-task / graph-extension (error.* contract +
  generic-handler pattern), graph-suspend / graph-resume (contract `graph` field),
  inspect (`inspect error`); skills-reference; minigraph-commands.json AI catalog;
  reserved-names docs (`error` alias + namespace, `graph` contract field).
- CHANGELOG: Added (error context, orchestrator support) / Changed (store contract —
  BREAKING, cid propagation). Release note wording for the flag-day.
- Webapp bundle regeneration after help edits (`npm run release`).

## Rust lock-step scope (after Java merges)

`crates/knowledge-graph`: suspend.rs (envelope + get body), extension.rs (cid header),
executor.rs + traveler.rs (error.* staging), model_validator.rs (alias rule), tests;
`extensions/minigraph-state-redis`: key composition + tests; docs mirror (known port
divergences preserved); CHANGELOG + INCREMENTS.
