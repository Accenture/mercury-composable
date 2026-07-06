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
- The `schema-registry-standalone` helper jar — only for the [JSON Schema](#json-schema-variant-confluent-wire-format) and [Avro](#avro-variant-confluent-wire-format) variants below (Protobuf is [not currently supported](#protobuf-not-currently-supported)).
- Node.js 18+ — only for the one-time topic-creation helper in `node/` (the app client is `curl`).

## Run it

### Terminal A — Redis
```shell
cd helpers/redis-standalone && java -jar target/redis-standalone-4.6.1.jar
```
### Terminal B — Kafka
```shell
cd helpers/kafka-standalone && java -jar target/kafka-standalone-4.6.1.jar
```

### Create the topics (10 partitions each) — once
```shell
cd examples/sync-over-async-demo/node
npm install        # once
node create-topics.js
```
This creates six topics with **10 partitions** each: `soa.request` / `soa.response` (the byte[] path),
`json-topic-1` / `json-topic-2` (the [JSON Schema path](#json-schema-variant-confluent-wire-format)), and
`avro-topic-1` / `avro-topic-2` (the [Avro path](#avro-variant-confluent-wire-format)). Don't
rely on Kafka auto-creation here: an auto-created topic has a single partition, so the `soa-reply-group`
consumer group can only place it on **one** facade — a second facade would stay idle. Multiple partitions
let the group spread across facades, which is what makes the multi-facade test below meaningful.

> Run this **before** the apps (Terminals C/D) — otherwise the flow adapter auto-creates the topics at 1
> partition first, and `create-topics.js` will then skip them as already existing. If they already exist at
> 1 partition from an earlier run, restart `kafka-standalone` first (it wipes all topics on restart), then
> create them here.

### Terminal C — backend pod
```shell
java -Dspring.profiles.active=backend -jar examples/sync-over-async-demo/target/sync-over-async-demo-4.6.1.jar
```
### Terminal D — facade pod (REST on :8400)
```shell
java -Dspring.profiles.active=facade -jar examples/sync-over-async-demo/target/sync-over-async-demo-4.6.1.jar
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
  java -Dspring.profiles.active=facade -Drest.server.port=8401 -jar examples/sync-over-async-demo/target/sync-over-async-demo-4.6.1.jar
  curl -sS -X POST http://127.0.0.1:8401/api/sync-to-async -H 'content-type: application/json' -d '{"order":"B-200"}'
  ```
- **Timeout → HTTP 408.** Stop the backend (Terminal C) and call again with a short budget; with no reply,
  the facade times out and `sync.error.handler` returns 408:
  ```shell
  curl -sS -i -X POST http://127.0.0.1:8400/api/sync-to-async \
       -H 'content-type: application/json' -H 'x-sync-timeout: 2000' -d '{"order":"C-300"}'
  ```

## JSON Schema variant (Confluent wire format)

The same end-to-end pattern, but the Kafka legs carry **Confluent JSON Schema** instead of raw byte[]. It
runs over a parallel pair of topics — `json-topic-1` (request) and `json-topic-2` (reply) — so you can
compare the two side by side; the byte[] path (`soa.request` / `soa.response`) is untouched.

**Seed the registry, then start it.** Schemas are governed artifacts registered out-of-band, so the demo
ships pre-registered schemas rather than self-registering at runtime. The [`registry/`](registry/) folder
holds one file per schema id, each now carrying its `subject` + `version` — [`1.json`](registry/1.json)
(subject `sync-demo-json`, a permissive JSON Schema covering both JSON legs), and
[`2.json`](registry/2.json) (subject `sync-demo-avro`, the [Avro variant](#avro-variant-confluent-wire-format)).
Copy them into
the registry's store (default `/tmp/schema-registry`), then start the registry — one registry serves both
variants:
```shell
# Terminal F — seed + start the local Confluent-compatible Schema Registry (port 8081)
mkdir -p /tmp/schema-registry
cp examples/sync-over-async-demo/registry/*.json /tmp/schema-registry/
cd helpers/schema-registry-standalone && java -jar target/schema-registry-standalone-4.6.1.jar
```
The store keeps one `<id>.json` per schema, so you can also drop a new file (e.g. `4.json`) into
`/tmp/schema-registry` while the registry is running — it is picked up on the next request, no restart needed.
The schema (the escaped `schema` string in the seed) is:
```json
{ "$schema": "http://json-schema.org/draft-07/schema#", "title": "SyncDemoMessage",
  "type": "object",
  "properties": { "action": {"type":"string"}, "status": {"type":"string"}, "processedBy": {"type":"string"} },
  "additionalProperties": true }
```
Now call the JSON endpoint (same request shape + synchronous reply as the byte[] path):
```shell
curl -sS -X POST http://127.0.0.1:8400/api/sync-to-async-json \
     -H 'content-type: application/json' -d '{"action":"create","order":"A-100"}'
```

What's different from the byte[] path (everything else — Redis return route, `sync.prepare`/`sync.await`,
trace continuity — is identical):

| Aspect | How |
|--------|-----|
| **Producer is subject-driven** | the flows publish with a `subject: sync-demo-json` header (no explicit `version`, so it defaults to `latest`); `simple.kafka.notification` resolves the global schema id and type from that subject internally, then serializes the body into the Confluent wire format using the **pre-registered** schema. The producer never needs a naming strategy — see [the Kafka Flow Adapter guide](../../docs/guides/kafka-flow-adapter.md). |
| **Schema registered out-of-band** | the registry is seeded from `registry/1.json` (subject `sync-demo-json`, version 1); the producer resolves the subject to an id and the serializer fetches the schema. Mirrors enterprise reality, where schemas are governed artifacts — no runtime registration in the app. |
| **Consumer decodes by id** | the `json-topic-1` / `json-topic-2` adapter bindings set `schema.enabled: true`, so the adapter reads the embedded id, fetches the schema, and hands the flow a decoded **Map**. |
| **Map-input task variants** | because the decoded body is a Map, `system.of.record.json` and `soa.reply.json` take a `Map` (vs the byte[] `system.of.record` / `soa.reply`); they reuse the same logic. |

## Avro variant (Confluent wire format)

The same end-to-end pattern again, now over **Confluent Avro** on a third pair of topics — `avro-topic-1`
(request) and `avro-topic-2` (reply). Nothing in the producer/consumer machinery changes from the JSON path:
the flows publish with a `subject: sync-demo-avro` header, and `simple.kafka.notification` resolves that
subject to the Avro schema id + type and dispatches to the Avro serializer. The registry seed already
includes **subject `sync-demo-avro`** (`registry/2.json`, copied in the step above), so the same Terminal F
registry serves both variants.

Call the Avro endpoint:
```shell
curl -sS -X POST http://127.0.0.1:8400/api/sync-to-async-avro \
     -H 'content-type: application/json' -d '{"action":"create","order":"A-100"}'
```
You get the backend's reply synchronously — the record's four fields, with `traceId` continuous across the
Kafka hops:
```json
{ "traceId": "0c7a0d2e...", "action": "create", "processedBy": "system-of-record", "status": "processed" }
```
> Note `order` is **not** a field of the Avro `SyncDemoMessage` record, so it is **dropped on the wire** —
> Avro records are closed-shape, and the framework's Map→`GenericRecord` conversion only carries declared
> fields. (The JSON variant's `additionalProperties: true` would carry it through.) Send a field the schema
> declares to see it round-trip.

The Avro `SyncDemoMessage` schema (id 2 in the seed) is:
```json
{ "type": "record", "name": "SyncDemoMessage", "namespace": "com.accenture.soa.demo",
  "fields": [ {"name":"action","type":"string","default":""}, {"name":"status","type":"string","default":""},
              {"name":"processedBy","type":"string","default":""}, {"name":"traceId","type":"string","default":""} ] }
```

The one instructive difference from JSON Schema — **Avro records are closed-shape**:

| Aspect | How |
|--------|-----|
| **Closed record, not open** | the JSON Schema is `additionalProperties: true`, so the byte[]/JSON reply can be an open, nested object. An Avro record declares its fields exactly, so `system.of.record.avro` builds a **flat** reply (`action`, `status`, `processedBy`, `traceId`) matching the record. |
| **Defaults fill partial input** | the request carries only `action`; the framework's Map→`GenericRecord` conversion applies each field's **schema default** (`""`) for the rest, so a partial input serializes cleanly (Avro's own strict JSON decoder would reject it). |
| **Generic, no codegen** | decode yields a `GenericRecord` (not a generated class), rendered back to a `Map` — so the flow tasks stay plain `Map`-in/`Map`-out, exactly like the JSON variant. |

Everything else — subject-driven produce, out-of-band registration, `schema.enabled` decode-by-id, the Redis
return route, and trace continuity — is identical to the JSON path.

## Protobuf (not currently supported)

`minimalist-kafka`'s Schema Registry integration supports **JSON Schema and Avro only**. Protobuf was
implemented and demoed in an earlier development phase (the same subject-driven produce + `schema.enabled`
consume path shown above, over `protobuf-topic-1`/`protobuf-topic-2`), but was **removed before its first
release** because Confluent's `kafka-protobuf-provider` depends on `com.squareup.wire:wire-runtime-jvm`, a
**discontinued** artifact carrying an unpatched denial-of-service CVE
([CVE-2026-45799 / GHSA-7xpr-hc2w-34m9](https://github.com/square/wire/security/advisories/GHSA-7xpr-hc2w-34m9)):
a crafted 10-byte payload can crash any Wire-decoding consumer. Wire's maintainers will not patch that
coordinate — the fix exists only under the renamed `wire-runtime` artifact, which Confluent has not adopted
(confirmed unchanged as of `kafka-protobuf-provider:8.3.0`). Shipping it would fail our security gate.

**Path forward:** this is tracked as a backlog item, not abandoned. It gets re-wired once Confluent moves to
the patched coordinate, or sooner for a specific field installation that explicitly needs Protobuf and
accepts the residual risk. Until then, `SchemaType.PROTOBUF` is still recognized by the codec (see its
Javadoc) so an attempt to use it fails clearly (`UnsupportedOperationException`), never silently.

We're noting this here rather than quietly dropping it, so anyone evaluating this library knows exactly
what's supported and why — see also the [kafka-flow-adapter guide](../../docs/guides/kafka-flow-adapter.md#schema-registry-integration).

That leaves **two** Confluent wire formats — **JSON Schema and Avro** — over one subject-driven produce +
`schema.enabled` consume path, differing only by the `subject` header. (For context: industry usage of
Confluent Schema Registry skews heavily toward Avro and JSON Schema already — Protobuf's stronghold is
gRPC/polyglot microservices, a different ecosystem from Schema-Registry-backed Kafka — so this narrows
scope rather than dropping the most common path.)

## How it maps to the pattern

| Piece | Role | What it shows |
|-------|------|---------------|
| `rest.yaml` + `sync-to-async.yml` | facade | a synchronous REST endpoint backed by a flow (no controller) |
| `sync.prepare` → `simple.kafka.notification` → `sync.await` | facade | register return route, publish (fail-fast), block for the reply |
| `soa-reply.yml` (`soa.reply`) | facade | deliver the Kafka reply to the coordinator → wake the awaiting request |
| `system-of-record.yml` (`system.of.record`) | backend | the application's async processing, on a separate pod |
| Redis return route | facade | routes the reply back to the originating pod (cross-pod, scalable) |
| `*-json.yml` flows + `registry/1.json` (subject `sync-demo-json`, id 1) | both | the [JSON Schema variant](#json-schema-variant-confluent-wire-format) over `json-topic-1` / `json-topic-2` |
| `*-avro.yml` flows + `registry/2.json` (subject `sync-demo-avro`, id 2) | both | the [Avro variant](#avro-variant-confluent-wire-format) over `avro-topic-1` / `avro-topic-2` |

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
