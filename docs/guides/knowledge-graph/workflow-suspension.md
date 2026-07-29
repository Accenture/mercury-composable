---
title: Workflow Suspension (suspend/resume)
summary: Express a long-running business process as a sequence of short graph runs -
  suspend at a human checkpoint, persist the workflow state to a pluggable store, and
  resume later with the same business correlation ID without re-executing completed steps.
layer: knowledge-graph
audience: [architect, developer]
keywords: [suspend, resume, workflow, human-in-the-loop, approval, checkpoint, state store, redis, correlation id]
related:
  - guides/knowledge-graph/skills-reference.md
  - guides/knowledge-graph/build-your-first-graph.md
  - guides/reserved-names-and-headers.md
---

# Workflow Suspension (suspend/resume)

> **At a glance**
>
> - **What it is** — A graph run can **suspend** at a human checkpoint (approval,
>   intervention, inbox notification): its model is persisted to an external state store
>   and the run completes normally. A later request with the **same business correlation
>   ID resumes** past the checkpoint without re-executing it. A long-running business
>   process becomes a sequence of short runs — nothing stays in memory between them.
> - **Vocabulary** — the `graph.suspend` and `graph.resume` skills, the reserved node
>   alias `suspend`, and the node property `suspend=true`.
> - **The store is pluggable** — Redis ships as the `minigraph-state-redis` extension;
>   any composable function honoring the [store contract](#state-store-contract) works.

## Why short runs

An approval may take minutes or days. Parking a live graph instance for that long would
pin memory, defeat timeouts, and not survive a restart. Suspension inverts the problem:
the run **ends** — the caller gets a `{"type": "suspended", "cid": ...}` reply — and the
workflow's durable memory (the `model` namespace) waits in the state store under the
business correlation ID with a time-to-live you choose. The resumed run is an ordinary
graph execution that happens to start with restored state. Because the record key is the
correlation ID, a workflow suspended on one application instance can resume on **any**
instance sharing the store.

## The three vocabulary pieces

**1. The `suspend` node** — exactly one per graph, and the alias `suspend` is **reserved**
(like `root` and `end`): traversal jumps to it *by name*. Its skill assembles and persists
the state envelope through the attached store function — no data mapping needed:

```text
create node suspend
with type Suspend
with properties
purpose=Persist workflow state to the external state store
skill=graph.suspend
task=v1.redis.persist.model
ttl=2d
```

`ttl` is **mandatory with no default** — a checkpoint may wait a minute or days, and only
the workflow designer knows. It uses duration syntax (`20s`, `5m`, `2h`, `2d`) and becomes
the store record's expiry.

**2. A suspensible node** — any skilled node marked `suspend=true`. After its skill
completes and its output mapping runs, traversal routes to the `suspend` node instead of
its normal forward path. Draw **both** edges — the checkpoint edge to `suspend` and the
continuation edge — so the diagram tells the whole story (the compiler enforces this).
Routing skills (`graph.math`, `graph.js`) cannot be suspensible. A plain edge *into* the
`suspend` node is an unconditional suspension point — no property needed.

**3. The resume node** — conventionally named `resume`, placed right after `root` (or
after setup nodes). When the store has a record for `model.cid`, it restores the model,
re-arms the traversal bookkeeping (a downstream `graph.join` still sees branches that
completed before suspension), and jumps past the checkpoint. When there is no record —
a fresh transaction, the normal first-run case, or an expired one — traversal simply
continues along the resume node's own forward path. The optional `missing=<node>`
property jumps to a designated handler instead, for workflows where an expired approval
needs its own response.

```text
create node resume
with type Resume
with properties
purpose=Restore workflow state from the external state store
skill=graph.resume
task=v1.redis.retrieve.model
```

Types (`Suspend`, `Resume`, `Suspensible`) are **visual convention** — they pick the node
colors in the Playground; the skill defines the behavior.

## Walkthrough: the approval workflow (tutorial-14)

`tutorial-14` in the `minigraph-playground` example app is the complete pattern:
`root → resume → record-request (suspend=true) → {suspend | approve} → end`. Run it with
Redis (e.g. `helpers/redis-standalone`) and drive it with two requests sharing one
correlation ID:

```bash
curl -s -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \
  -H 'content-type: application/json' -H 'x-correlation-id: order-1001' \
  -d '{"item": "laptop", "amount": 2000}'
```

```json
{"type": "suspended", "cid": "order-1001"}
```

The workflow captured the request into `model.request` and suspended. When the approver
decides:

```bash
curl -s -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \
  -H 'content-type: application/json' -H 'x-correlation-id: order-1001' \
  -d '{"decision": "approved"}'
```

```json
{"stage": "completed", "decision": "approved", "request": {"item": "laptop", "amount": 2000}}
```

The original request crossed the suspension: run 2 restored `model.request` from the
store, skipped `record-request` (it already ran), and completed through `approve`. The
caller of the suspended run decides what "suspended" looks like — stage your own
`output.*` before the suspend node to override the default reply.

## Design rules

- **The model is the workflow's durable memory.** Only the `model` namespace persists —
  a node's `{node}.result` scratch does not survive suspension. Map anything a later step
  needs into `model.*` **before** the checkpoint.
- **A suspension point must be the sole active branch.** Do not suspend between a fan-out
  and its join — branches in flight cannot be persisted (the engine logs a warning);
  suspend *after* the join instead. Joins whose predecessors completed before suspension
  work: their completion marks are part of the persisted state.
- **One resume per transaction.** The shipped stores consume the record atomically on
  retrieval (Redis `GETDEL`), so a duplicate resume — a double click, a retried message —
  finds nothing and behaves as a fresh run instead of double-executing the continuation.
  A later checkpoint in the resumed run simply persists a new record under the same ID.
- **The correlation ID is a resume capability.** Whoever presents it continues the
  workflow: protect resume-bearing endpoints with rest.yaml `authentication`, and use
  non-guessable IDs (the engine generates UUIDs when the caller supplies none).
- **Suspension does not cross `graph.extension`.** The business correlation ID does not
  propagate into delegated sub-graphs or flows today — design resumable workflows as
  top-level graphs.
- Reserved model keys (`model.cid`, `model.instance`, `model.flow`, `model.ttl`,
  `model.trace`) are never persisted — the resumed run's own identity is authoritative.

## The state store contract {#state-store-contract}

The store is an ordinary composable function named by the suspend/resume nodes' `task`
property — the Redis module below is one implementation; PostgreSQL, DynamoDB, MongoDB or
anything else plugs in the same way.

**Persist** — invoked by `graph.suspend`; headers `type=put`; request body:

```json
{
  "cid":   "<business correlation ID - the retrieval key>",
  "node":  "<the suspension point>",
  "ttl":   172800,
  "model": { "the model namespace minus reserved keys": "..." },
  "seen":  { "traversal bookkeeping": true },
  "run":   { "traversal bookkeeping": true }
}
```

Store the body **opaquely** (the reference implementations use MsgPack — binary values
round-trip; note the platform's [serialization gotchas](../../guides/api-overview.md))
and reply 2xx only when the record is durable — the reply is the acknowledgement
`graph.suspend` requires before the graph completes; any error fails the suspension.

**Retrieve** — invoked by `graph.resume`; headers `type=get`; body `{"cid": "..."}`.
Return the stored record as-is, or **null / an empty map** when absent or expired — an
absent record is the normal fresh-transaction case, never an error. Consume the record
atomically on retrieval (or document your replay semantics). If the store has no native
TTL, implement record expiry yourself.

The smallest possible reference implementation is the engine's test fixture — a temp-file
store of ~60 lines (`FileStateStore` in the minigraph test sources).

## The Redis store module

`extensions/minigraph-state-redis` ships `v1.redis.persist.model` (SETEX, native expiry)
and `v1.redis.retrieve.model` (atomic `GETDEL` — Redis 6.2+). Include the jar and the two
functions register automatically; the connection is lazy, so the application boots
normally without Redis until a workflow actually suspends. Configuration uses the same
`redis.*` keys as the sync-over-async extension (`redis.host`, `redis.port`,
`redis.password`, `redis.ssl`, `redis.database`, `redis.timeout.ms`), and the worker
counts are ops-tunable via `worker.instances.v1.redis.persist.model` /
`worker.instances.v1.redis.retrieve.model`. See the module README for details.

## See also

- [Built-in skills reference](skills-reference.md) — `graph.suspend` / `graph.resume` entries.
- [Build your first graph](build-your-first-graph.md) — graph authoring basics.
- [Reserved names & headers](../reserved-names-and-headers.md) — the extension routes.
