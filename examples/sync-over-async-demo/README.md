# sync-over-async-demo — synchronous REST over an async Kafka backend

A hands-on, end-to-end demonstration of the **sync-over-async** pattern: a caller makes one **synchronous
HTTP request**, but under the hood the request travels over **Kafka** to a separate backend pod, the reply
comes back over Kafka, and a **Redis return route** delivers it to the exact facade pod that is waiting —
so the HTTP call returns the real backend response synchronously. This is the cross-pod request/response
use case from [ADR-0006](../../docs/arch-decisions/ADR.md).

```
  curl  --HTTP-->  facade pod (8400)                                   backend pod
                     sync-to-async flow                                 system-of-record flow
                       sync.prepare  -> register return route (Redis)
                       simple.kafka.notification --(soa.request)-->     system.of.record (process)
                       sync.await  (blocks) ........                    simple.kafka.notification
                                                .                              |
                     soa-reply flow  <--(soa.response)---------------------- --+
                       soa.reply -> coordinator.deliver -> Redis wakes sync.await
                     <--HTTP 200 (backend reply)
```

Both roles run from **one jar**, selected by the active Spring profile. The whole stack runs locally with
**no Docker** (use the [`redis-standalone`](../../helpers/redis-standalone) and
[`kafka-standalone`](../../helpers/kafka-standalone) helpers).

## What's app-specific vs. reused

Only [`SystemOfRecord`](src/main/java/com/accenture/soa/demo/tasks/SystemOfRecord.java) (the backend
business logic) and [`SyncErrorHandler`](src/main/java/com/accenture/soa/demo/support/SyncErrorHandler.java)
(the HTTP status policy) are written here. Everything else is reused from the `sync-over-async` extension:
`sync.prepare`, `sync.await`, `soa.reply`, the Redis coordinator, plus `simple.kafka.notification` and
`http.flow.adapter`.

## Prerequisites

- Java 21 + Maven. Build once from the repo root: `mvn -pl examples/sync-over-async-demo -am install`.
- The `redis-standalone` and `kafka-standalone` helper jars (built by the same reactor).
- Node.js 18+ — only for the one-time topic-creation helper in `node/` (the app client is `curl`).

## Run it

### Terminal A — Redis
```shell
cd helpers/redis-standalone && java -jar target/redis-standalone-4.5.0.jar
```
### Terminal B — Kafka
```shell
cd helpers/kafka-standalone && java -jar target/kafka-standalone-4.5.0.jar
```

### Create the topics (10 partitions each) — once
```shell
cd examples/sync-over-async-demo/node
npm install        # once
node create-topics.js
```
This creates `soa.request` / `soa.response` with **10 partitions** each. Don't rely on Kafka auto-creation
here: an auto-created topic has a single partition, so the `soa-reply-group` consumer group can only place
it on **one** facade — a second facade would stay idle. Multiple partitions let the group spread across
facades, which is what makes the multi-facade test below meaningful.

> Run this **before** the apps (Terminals C/D) — otherwise the flow adapter auto-creates the topics at 1
> partition first, and `create-topics.js` will then skip them as already existing. If they already exist at
> 1 partition from an earlier run, restart `kafka-standalone` first (it wipes all topics on restart), then
> create them here.

### Terminal C — backend pod
```shell
java -Dspring.profiles.active=backend -jar examples/sync-over-async-demo/target/sync-over-async-demo-4.5.0.jar
```
### Terminal D — facade pod (REST on :8400)
```shell
java -Dspring.profiles.active=facade -jar examples/sync-over-async-demo/target/sync-over-async-demo-4.5.0.jar
```
### Terminal E — call it synchronously
```shell
curl -sS -X POST http://127.0.0.1:8400/api/sync-to-async \
     -H 'content-type: application/json' \
     -d '{"order":"A-100","item":"widget","qty":3}'
```
You get the backend's reply **synchronously**:
```json
{"status":"processed","processedBy":"system-of-record","processedAt":"2026-...Z",
 "traceId":"...","request":{"order":"A-100","item":"widget","qty":3}}
```
The facade and backend logs show the same `traceId` across the Kafka hops.

## Two things worth trying

- **Multiple facade pods on one machine** (requires the multi-partition topics created above). Start a
  second facade on another port and call it — the Redis return route delivers each reply to the pod that
  originated the request, even when the *other* facade is the one that consumed the reply off `soa.response`
  (watch which facade logs `soa.reply` vs. which one returns the HTTP response):
  ```shell
  java -Dspring.profiles.active=facade -Drest.server.port=8401 -jar examples/sync-over-async-demo/target/sync-over-async-demo-4.5.0.jar
  curl -sS -X POST http://127.0.0.1:8401/api/sync-to-async -H 'content-type: application/json' -d '{"order":"B-200"}'
  ```
- **Timeout → HTTP 408.** Stop the backend (Terminal C) and call again with a short budget; with no reply,
  the facade times out and `sync.error.handler` returns 408:
  ```shell
  curl -sS -i -X POST http://127.0.0.1:8400/api/sync-to-async \
       -H 'content-type: application/json' -H 'x-sync-timeout: 2000' -d '{"order":"C-300"}'
  ```

## How it maps to the pattern

| Piece | Role | What it shows |
|-------|------|---------------|
| `rest.yaml` + `sync-to-async.yml` | facade | a synchronous REST endpoint backed by a flow (no controller) |
| `sync.prepare` → `simple.kafka.notification` → `sync.await` | facade | register return route, publish (fail-fast), block for the reply |
| `soa-reply.yml` (`soa.reply`) | facade | deliver the Kafka reply to the coordinator → wake the awaiting request |
| `system-of-record.yml` (`system.of.record`) | backend | the application's async processing, on a separate pod |
| Redis return route | facade | routes the reply back to the originating pod (cross-pod, scalable) |

## Validated runs (telemetry evidence)

The distributed-tracing telemetry below was captured from real runs (no Docker). Each hop gets a new
`span_id`; the `traceId` stays continuous, and each task's `span_id` becomes the next task's
`parent_span_id` — including across the Kafka hops and across pods.

### Single facade + backend

One trace `5464a099…` across both pods (facade `…438d90af`, backend `…47d04eda`); `cid=abc26147…`:

```
facade   http.flow.adapter 9f93ac
         └─ sync.prepare 89deb4 (parent 9f93ac)
            └─ simple.kafka.notification b00896 (parent 89deb4) ──soa.request──►
               ├─ sync.await 88d2e9 (parent b00896)                 [facade]
               │  └─ async.http.response bae656 (parent 88d2e9)
               └─ backend system.of.record a9a15c (parent b00896)   [cross-pod hop 1]
                  └─ simple.kafka.notification 83e2ba (parent a9a15c) ──soa.response──►
                     └─ facade soa.reply 993966 (parent 83e2ba)      [cross-pod hop 2]
```

- `sync.await` is parented to `simple.kafka.notification` (`b00896`) — the in-flow span chains correctly
  across a `Mono`-returning task (see platform-core fix `WorkerHandler.handleMonoResponse`).
- Both Kafka hops chain: `b00896 → system.of.record` and `83e2ba → soa.reply`. Trace-id continuous; `cid`
  round-trips; the 200 + `async.http.response` confirm the rendezvous.

### Two facades + backend (cross-pod return route)

Topics created with 10 partitions, so the `soa-reply-group` consumer group spreads across both facades:

```
soa.response partitions  ->  facade-1: [0,1,2,3,4]   facade-2: [5,6,7,8,9]
soa.request  partitions  ->  backend:  [0..9]
```

One trace `4e19c16a…`, three distinct origins (facade-1 `…176a5ec4`, facade-2 `…19be4a56`, backend
`…dfbad1e0`); `cid=8a8dc8d0…`:

```
facade-1  http.flow.adapter b33972
          └─ sync.prepare 844ad2
             └─ simple.kafka.notification a27535c0 ──soa.request──►
                ├─ sync.await 86241e43 (parent a27535c0)                [facade-1, returns HTTP 200]
                │  └─ async.http.response bdc25a19
                └─ backend system.of.record 92ea63ea (parent a27535c0)  [cross-pod hop 1]
                   └─ simple.kafka.notification 919535da ──soa.response──►
                      └─ facade-2 soa.reply 9babb293 (parent 919535da)   [cross-pod hop 2, OTHER facade]
```

**The key result:** facade-1 originated the request and blocked on `sync.await`; facade-2 (holding the
`soa.response` partition) consumed the reply and ran `soa.reply`; yet **facade-1** woke and returned the
HTTP response. `begin(cid)` registered facade-1's `svc-return:…176a5ec4` channel in Redis, and facade-2's
`deliver(cid)` looked it up and signalled **facade-1** — not itself. That cross-pod hand-off is the whole
point of sync-over-async (see [ADR-0006](../../docs/arch-decisions/ADR.md)), and the trace stays continuous
across all three pods.
