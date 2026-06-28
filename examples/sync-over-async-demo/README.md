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
