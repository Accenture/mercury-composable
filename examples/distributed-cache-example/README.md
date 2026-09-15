# distributed-cache-example — one Redis cache across all three Mercury layers

A hands-on demonstration of the **distributed-cache** module (`v1.cache.redis`) driven the same three
ways Mercury lets you build anything — and the payoff: a profile written through **one** layer reads back
unchanged through the **other two**, because all three share one Redis cache and one wire format.

The app is a profile store with GET / POST / DELETE, exposed three times — one route family per layer:

| Layer | Surface | Endpoint | How the action is chosen |
| --- | --- | --- | --- |
| **1 — Platform Core** | event-driven **code** (PostOffice RPC) | `…/api/l1/profile/{id}` | the function switches on the HTTP method |
| **2 — Event Script** | one **YAML flow** + a decision | `…/api/l2/profile/{id}` | a tiny task maps `input.method` → an action, a decision routes |
| **3 — Knowledge Graph** | one **graph** + a decision node | `…/api/graph/profile-cache` | the action rides in the JSON payload (`{ "action", "id", "profile" }`) |

```
                          ┌───────────────────────────────────────────────┐
  GET/POST/DELETE ──L1──▶ │  v1.profile.l1  (code: switch on input.method) │─┐
                          └───────────────────────────────────────────────┘ │
                          ┌───────────────────────────────────────────────┐ │   ┌───────────────────┐
  GET/POST/DELETE ──L2──▶ │  l2-profile flow  (method→action, decision)    │─┼──▶│   v1.cache.redis   │──▶ Redis
                          └───────────────────────────────────────────────┘ │   │  (distributed-cache│    cache-demo:{id}
                          ┌───────────────────────────────────────────────┐ │   │     extension)     │
  POST {action,id,…} ─L3─▶│  graph-executor flow → profile-cache graph     │─┘   └───────────────────┘
                          └───────────────────────────────────────────────┘
     value at rest = the profile Map packed with MsgPack (MsgPack.packMapOrList) — just the map, no envelope wrapper
```

All three call the **same** `v1.cache.redis` action function, under the **same** key namespace
(`redis.cache.key.prefix=cache-demo:`), storing the **same** value shape — so they interoperate. This is
the point of the example: *event-driven programming, Event Script, and the knowledge graph are three views
of one composable function, not three different systems.* (Once the Rust twin lands, the plain-MsgPack
value makes the same cache interoperate across languages too — see below.)

## The shared value: a MsgPack-packed Map

A profile is a `Map<String,Object>` (`{name, email}`). To keep a Map in Redis as opaque bytes, the example
packs it directly with `MsgPack` — the same wire format Mercury uses between functions, but just the map:

- **store**: [`ProfileEncoder`](src/main/java/com/accenture/cache/demo/functions/ProfileEncoder.java) —
  `msgPack.packMapOrList(profile)`
- **restore**: [`ProfileDecoder`](src/main/java/com/accenture/cache/demo/functions/ProfileDecoder.java) —
  `msgPack.unpackMapOrList(bytes)`, or **HTTP 404 "Profile not found"** when the bytes are absent (a miss)

Because the format is identical everywhere, the bytes L1 writes are exactly the bytes L2 and L3 read.

> **Why plain MsgPack and not an `EventEnvelope` holder?** Two reasons. **(1) Efficiency** — the value is
> just the key-values; it needs none of the envelope's id or headers, so packing the bare map is smaller.
> **(2) Language portability** — the compacted `EventEnvelope` encoding is a Java-side wire format the Rust
> port does not read, and storing the envelope as ordinary key-values would waste space. Plain MsgPack
> key-values is both compact and the form every language pack reads, so the cache stays polyglot-ready.
>
> Note the method pair: `packMapOrList` / `unpackMapOrList` is the **general purpose** codec. The older
> `pack` / `unpack` is the *event payload* codec — it wraps a PoJo or primitive under the reserved `_T` /
> `_D` keys and strips them on the way back, which would mean a stored map containing a `_T` key came back
> changed. See [API Overview → Binary serialization with MsgPack](../../docs/guides/api-overview.md#binary-serialization-with-msgpack).

## What's app-specific vs. reused

Written here (five small functions + three config files); everything else is the framework:

| File | Role |
| --- | --- |
| [`ProfileCacheL1`](src/main/java/com/accenture/cache/demo/functions/ProfileCacheL1.java) | **Layer 1** — the whole CRUD as one event-driven function |
| [`MethodActionMapper`](src/main/java/com/accenture/cache/demo/functions/MethodActionMapper.java) | **Layer 2** — maps `input.method` → `{action, decision}` (the "small composable task") |
| [`ProfileEncoder`](src/main/java/com/accenture/cache/demo/functions/ProfileEncoder.java) / [`ProfileDecoder`](src/main/java/com/accenture/cache/demo/functions/ProfileDecoder.java) | the MsgPack pack / unpack helpers (+ the 404-on-miss contract) |
| [`ProfileExceptionHandler`](src/main/java/com/accenture/cache/demo/functions/ProfileExceptionHandler.java) | renders a flow error as `{type, status, message}` (Layer 2) |
| [`rest.yaml`](src/main/resources/rest.yaml) · [`flows/l2-profile.yml`](src/main/resources/flows/l2-profile.yml) · [`graph/profile-cache.json`](src/main/resources/graph/profile-cache.json) | the L1/L2/L3 wiring |
| [`flows/graph-executor.yml`](src/main/resources/flows/graph-executor.yml) | the standard Layer 3 exposure flow — copied unchanged from `templates/starter-graph`, not written for this app |
| [`scripts/playground-session-broker.mjs`](scripts/playground-session-broker.mjs) | lets an AI agent **host** a Playground session against this app — also copied from the template |

Reused from the platform: `v1.cache.redis` (the **distributed-cache** extension), `http.flow.adapter` +
the Event Script engine, and `graph.executor` + `graph.math` + `graph.task` (the **minigraph** engine).

## Prerequisites

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

- Java 21+ and Maven. Build once from the repo root: `mvn -pl examples/distributed-cache-example -am install`.
- A **standalone Redis** for the runnable app. Use the repo's helper (no Docker):
  ```shell
  cd helpers/redis-standalone && java -jar target/redis-standalone-x.y.z.jar
  ```
  It listens on `127.0.0.1:6379` — the app's default (`redis.host`/`redis.port` in
  [`application.properties`](src/main/resources/application.properties), overridable with `REDIS_HOST` /
  `REDIS_PORT`). The **unit tests need no standalone Redis** — they start an embedded Redis themselves.

## Run it

```shell
# Terminal A — Redis (see above)
cd helpers/redis-standalone && java -jar target/redis-standalone-x.y.z.jar

# Terminal B — the app (REST on :8305)
java -jar examples/distributed-cache-example/target/distributed-cache-example-x.y.z.jar
```
`GET http://127.0.0.1:8305/health` reports the `redis.health` dependency (it is a `mandatory.health.dependency`).

### Exercise one layer (Layer 1 shown; Layer 2 is identical on `/api/l2/…`)

```shell
# miss → HTTP 404 {"message":"Profile not found", ...}
curl -sS -i http://127.0.0.1:8305/api/l1/profile/alice

# store → HTTP 201 {"id":"alice","layer":1,"status":"stored"}
curl -sS -X POST http://127.0.0.1:8305/api/l1/profile/alice \
     -H 'content-type: application/json' -d '{"name":"Alice","email":"alice@example.com"}'

# read → HTTP 200 {"name":"Alice","email":"alice@example.com"}
curl -sS http://127.0.0.1:8305/api/l1/profile/alice

# evict → HTTP 200 {"id":"alice","layer":1,"deleted":true}
curl -sS -X DELETE http://127.0.0.1:8305/api/l1/profile/alice
```

### Layer 3 — one endpoint for every graph, the action is in the payload

Layer 3 has **no endpoint of its own**. The standard graph API `POST /api/graph/{graph_id}` serves every
graph the app deploys in [`graphs.yaml`](src/main/resources/graphs.yaml) — here that is `profile-cache`,
so the URL is `/api/graph/profile-cache`. Deploying a second graph adds no REST configuration.

```shell
curl -sS -X POST http://127.0.0.1:8305/api/graph/profile-cache \
     -H 'content-type: application/json' \
     -d '{"action":"save","id":"alice","profile":{"name":"Alice","email":"alice@example.com"}}'
curl -sS -X POST http://127.0.0.1:8305/api/graph/profile-cache -H 'content-type: application/json' -d '{"action":"get","id":"alice"}'
curl -sS -X POST http://127.0.0.1:8305/api/graph/profile-cache -H 'content-type: application/json' -d '{"action":"delete","id":"alice"}'
```
The `save` and `delete` acknowledgements carry `"layer": 3`, so you can see which surface answered; `get`
returns the stored profile itself (`{name, email}`) — identical to what L1 and L2 return.

The dispatch table is **closed**: an action that is not `get`, `save` or `delete` — including a missing
one — is rejected by the graph itself, rather than falling through to whichever branch happens to be last.

```shell
# → HTTP 400 {"message":"Invalid action. Use get, save or delete","status":400}
curl -sS -i -X POST http://127.0.0.1:8305/api/graph/profile-cache \
     -H 'content-type: application/json' -d '{"action":"purge","id":"alice"}'
```

### The interop demo — write on one layer, read on another

```shell
# write through Layer 1 ...
curl -sS -X POST http://127.0.0.1:8305/api/l1/profile/bob \
     -H 'content-type: application/json' -d '{"name":"Bob","email":"bob@example.com"}'

# ... read the same record through Layer 2 ...
curl -sS http://127.0.0.1:8305/api/l2/profile/bob
# ... and through Layer 3
curl -sS -X POST http://127.0.0.1:8305/api/graph/profile-cache -H 'content-type: application/json' -d '{"action":"get","id":"bob"}'
```
All three return the same `{"name":"Bob","email":"bob@example.com"}` — one cache, one wire format.

## Co-author the graph with an AI agent (dev mode)

[`application.properties`](src/main/resources/application.properties) sets **`app.env=dev`**, which opens
the MiniGraph Playground alongside the three CRUD layers: the UI at <http://127.0.0.1:8305>, the session
WebSocket at `/ws/graph/playground`, and the AI companion endpoint `POST /api/companion/{id}/sync`. Every
one of those services is gated by `@OptionalService("app.env=dev")`, so **removing that one line closes
the whole surface** for production — the matching `rest.yaml` entries are then skipped at start-up.

That is how the `profile-cache` graph itself is meant to be evolved: import the model into a Playground
session, edit a node, dry-run it, and export it back — with an AI agent hosting the session through
the bundled broker rather than borrowing your browser session:

```shell
node scripts/playground-session-broker.mjs --target http://127.0.0.1:8305
# → prints the session id; join it in the browser with: session subscribe <id>
# the agent then drives: POST /api/companion/<id>/sync   (one command per request, text/plain)
```

Inside the session, `import graph from profile-cache` brings the deployed model in, `run` dry-runs it,
and `export graph as profile-cache` writes it back for the next restart to redeploy through the
CompileGraph gate. See [`scripts/README.md`](scripts/README.md) and
[Playground & AI companion](../../docs/guides/knowledge-graph/playground-and-companion.md).

## Tracing & app-context logging

Every endpoint sets `tracing: true`. Each call logs a chain of trace records whose `span_id` /
`parent_span_id` link parent to child across every hop — the REST adapter, the flow/graph tasks, and the
`v1.cache.redis` call. The app functions also add **app-context** fields with `PostOffice.updateContext(...)`
(`layer=1|2`, and the derived `action` on L2), which appear in the same records. Watch, for example, an L2
GET fan out through `method.to.action → get.cache.read → get.decode`, or an L3 traversal log the graph walk
`root → decide → cache-get → decode → end` when `graph.traversal.log=true`.

## How the three layers are built

- **Layer 1** — [`ProfileCacheL1`](src/main/java/com/accenture/cache/demo/functions/ProfileCacheL1.java) is
  bound straight to the endpoint in `rest.yaml` (`service: 'v1.profile.l1'`, no flow). It switches on
  `input.getMethod()` and drives the cache with `po.request(...)` — the orchestration *is* the function body.
- **Layer 2** — [`l2-profile.yml`](src/main/resources/flows/l2-profile.yml) is one flow. Its first task
  ([`MethodActionMapper`](src/main/java/com/accenture/cache/demo/functions/MethodActionMapper.java)) turns
  the HTTP method into `{action, decision}`; an `execution: decision` task routes to the get / save / delete
  branch. Two branches call `v1.cache.redis`, so each task carries a distinct `name:` alias.
- **Layer 3** — [`profile-cache.json`](src/main/resources/graph/profile-cache.json) is one graph. A
  `graph.math` **decision** node routes on the payload `action`; `graph.task` nodes drive `v1.cache.redis`
  and the encode/decode functions, and a `graph.data.mapper` **reject** node answers HTTP 400 for anything
  that is not `get` / `save` / `delete` (`int(400) -> output.status` — a status of 400+ staged in the graph
  becomes the standard error response). It is deployed via
  [`graphs.yaml`](src/main/resources/graphs.yaml) and reached through the standard
  [`graph-executor.yml`](src/main/resources/flows/graph-executor.yml) flow — the *same* flow every Mercury
  graph application uses, taking the graph id from the URL path parameter.

> **Graph gotcha worth knowing.** A `graph.task` *output* mapping LHS may only be a constant or a
> `result.` / `model.` / `<node>.` element — **not** `input.*`. So the graph copies the id into the model
> at the decision node (`MAPPING: input.body.id -> model.id`) and the branches echo it back with
> `model.id -> output.body.id`. (An illegal `input.*` on the output side now aborts the node with a named
> error — before the async-callback guard landed it threw inside the task's callback and the flow simply
> timed out with nothing logged.)

## Test it

```shell
mvn test -f examples/distributed-cache-example/pom.xml
```
[`ProfileCacheTest`](src/test/java/com/accenture/cache/demo/ProfileCacheTest.java) runs the full CRUD cycle
against **all three layers** on an embedded Redis (no external server), including the 404-on-miss contract,
the graph's 400-on-unknown-action rejection, and the write-on-L1 / read-on-L2 interop check.
