# Distributed cache — a generic Redis-backed L2 cache module — design spec

**Status:** IMPLEMENTED (Java) — Q1–Q8 ruled by Eric 2026-09-14 (§8); shipped for v4.12.9 as
`extensions/distributed-cache` (route `v1.cache.redis`) on the extracted `extensions/redis-connection`
foundation. The Rust port follows in lockstep (Q8). Drafted 2026-09-14 after a field installation asked
whether the sync-over-async module is "generic enough" to also cover distributed-cache use cases.

**Ruled shape (Eric, 2026-09-14):** module `extensions/distributed-cache`, route **`v1.cache.redis`**;
the Redis client layer is **extracted to a shared foundation** both modules depend on (Q2); values are
opaque `byte[]` (Q3); the operation set is the **five key-value actions + bulk `MPUT` + PING + list push/pop/length**
(Q5 — `MPUT` is a pipelined per-entry `SETEX` so bulk write keeps per-key TTLs where raw `MSET` cannot,
and the list ops reuse the append-with-TTL + destructive-pop machinery already built for the return
route); an **app key-prefix namespace** (Q6); the typed Java helper is **deferred** (Q7 — the function +
flow surfaces already cover L1/L2/L3); and the **Rust port ships in lockstep** with the Java module (Q8).
The client uses a **single shared, multiplexed Lettuce connection — no pool** (§4.4).

**Direction (ruled):** *Keep sync-over-async small.* sync-over-async is a cross-pod **rendezvous
transport** (correlation-id `request:`/`queue:` keys, `RPUSH`/`LPOP`/`EVAL` drains), **not** a
key-value cache. The distributed cache is a **separate opt-in module** that **reuses
sync-over-async's Redis client layer** (Eric, 2026-09-14; memory `cache-separate-from-soa`).

**Repo scope:** Java (`mercury-composable`) and the Rust port (`mercury`) in **lockstep** (Q8). The
`redis.health` route name was reserved for exactly this module when sync-over-async took
`soa.redis.health` (PR #377).

**Related:** [streaming-return-route.md](streaming-return-route.md) (the sync-over-async design,
source of the client layer to share); `soa-redis-cluster-support`, `cache-separate-from-soa`
(memory).

---

## 1. Problem

A field installation runs an L2 distributed cache backed by Redis via Lettuce, inside the
Platform Lambda / Event Script engine, and asked whether it can **leverage** Mercury's
sync-over-async work rather than maintain its own Redis client wiring. sync-over-async now owns a
capable, generic Redis **client layer** — standalone/cluster selection, auto-detect, RBAC/password
auth, TLS, health check — that a cache would want. But the cache's *operations* (key-value
`GET`/`PUT`/`MGET`/…) are a different concern from the rendezvous protocol.

Two things must not be conflated:

- **sync-over-async** = a *rendezvous* (one pod registers a return route, another posts a response;
  best-effort Pub/Sub wake-ups healed by final drains). Adding cache operations to it would bloat a
  deliberately lean, opt-in transport and violate the "lean, not heavyweight" vision.
- **distributed cache** = plain key-value storage with TTLs. Its value is the *operations* plus the
  composability surfaces, not a new Redis client.

The generic, reusable asset is the **Redis client layer**. The cache is a thin composable module on
top of it.

## 2. Driving use cases

A field installation wants a cache usable **two ways** (both are just "a composable function", the
standard Mercury add-a-capability shape):

1. **As a task.** A single cache function takes an `action` (`PUT`/`GET`/`MGET`/`DELETE`/
   `PUT_IF_NOT_PRESENT`) plus key(s)/value/ttl. In an **Event Script (Layer 2)** flow it is a task
   whose result is consumed through **output data mapping**; in a **Knowledge Graph (Layer 3)** model
   it is driven from a `graph.task` node.
2. **Directly via PostOffice (Layer 1).** Because a composable function backs the task, the same
   function is route-addressable — `po.request(...)` — for code that wants the cache without a flow.

Operation set (Q5 — the field's five key-value actions, the bulk `MPUT` pair for `MGET`, `PING`, plus
the FIFO **list** ops whose Redis machinery the return route already built):

| Action | Redis op | Result |
|---|---|---|
| `PUT` | `SETEX key ttl value` | ack |
| `GET` | `GET key` | value, or null (miss) |
| `MGET` | `MGET k1 k2 …` | `Map<key, value>` (misses omitted) |
| `MPUT` | pipelined `SETEX key ttl value` per map entry (one round trip) | ack (per-entry, non-atomic) |
| `DELETE` | `DEL key` | count removed |
| `PUT_IF_NOT_PRESENT` | `SET key value NX EX ttl` (atomic) | true if stored, false if key existed |
| `LIST_PUSH` | `RPUSH key value` + `EXPIRE key ttl` (one atomic Lua step) | new list length |
| `LIST_POP` | `LPOP key` | oldest value, or null (empty) |
| `LIST_LEN` | `LLEN key` | list length |
| `PING` | `PING` | liveness (backs the `redis.health` check) |

The list ops (`LIST_PUSH`/`LIST_POP`/`LIST_LEN`) are the same **append-with-TTL** (`RPUSH`+`EXPIRE` as
one server-side atomic step), **destructive `LPOP`**, and `LLEN` the return-route store already
implements — a FIFO queue on top of a Redis List, every key carrying a TTL from birth. `PING` is used
by the `redis.health` check rather than exposed as a general cache action.

## 3. Current state — what already exists to reuse

Shipped in `extensions/sync-over-async` (PR #385), the Redis client layer:

- **`RedisBackend`** — a seam hiding standalone vs cluster. Lettuce's standalone `RedisCommands` and
  cluster `RedisAdvancedClusterCommands` both extend `RedisClusterCommands`, so one command type
  serves both; `StandaloneRedisBackend` / `ClusterRedisBackend` implement it.
- **`RedisBackendFactory`** — builds the backend from config; two-key cluster selection
  (`cluster.detect=auto` probes `INFO`, else the boolean `cluster.mode`, which is also the
  inconclusive-probe fallback).
- **`RedisConfig`** — discrete connection parameters (`host`/`port`/`username`/`password`/`ssl`/
  `database`/`timeout.ms`/`cluster.*`), `${ENV_VAR}` substitution, `toUri()` / `seedUris()`, RBAC
  auth (`withAuthentication`) or password (`withPassword`), all under the **`soa.redis.*`** namespace
  with a plain-`redis.*` fallback.
- **`RedisHealthCheck`** — a `@PreLoad` PING probe with lazy config re-resolution and waiting-vs-outage
  classification, on route `soa.redis.health`.

The cache reuses all four. Only the **config key prefix** and the **health route name** differ
(`redis.*` / `redis.health` for the cache vs `soa.redis.*` / `soa.redis.health` for sync-over-async),
which the namespacing already anticipates.

## 4. Design (proposed shape)

### 4.1 Shape in one sentence

A separate opt-in module exposing **one composable action function** (`PUT`/`GET`/`MGET`/`DELETE`/
`PUT_IF_NOT_PRESENT`) over a shared Redis client layer, addressable at Layer 1 (PostOffice), Layer 2
(Event Script task + output mapping) and Layer 3 (`graph.task`), with its own `redis.health` check.

### 4.2 Module boundary

- **In:** the action function, the cache config (`redis.*` + cache tunables), the `redis.health`
  check, an optional consumer-facing Java helper (Q7).
- **Shared (extracted):** `RedisBackend` / `RedisBackendFactory` / `RedisConfig` / the health-probe
  logic — see §4.3.
- **Out (never):** the rendezvous protocol, Pub/Sub wake-ups, streaming — those stay in
  sync-over-async. The cache does not import sync-over-async.

### 4.3 The shared Redis client foundation (Q2)

The client layer currently lives in `extensions/sync-over-async/.../support/`. To share it without
the cache depending on the whole rendezvous module, extract it into a small foundation both modules
depend on (working name **`redis-connection`**), and **parameterise `RedisConfig` by key prefix**:

```
RedisConfig.from(config)                 // legacy: soa.redis.* -> redis.* fallback (unchanged for sync-over-async)
RedisConfig.from(config, "redis.")       // the cache: plain redis.* namespace
RedisConfig.from(config, "soa.redis.")   // sync-over-async, expressed via the prefix
```

The health check similarly takes a route + prefix so each module registers its own
(`redis.health` for the cache, `soa.redis.health` for sync-over-async) over the shared probe logic.

Because the `soa.redis.*` keys already fall back to `redis.*`, a deployment can run **one Redis for
both** (set `redis.*`, both read it) or **decouple** (set `soa.redis.*` for the rendezvous, `redis.*`
for the cache) with no collision — the namespacing done in PR #385 is what makes the split clean.

### 4.4 The cache action function

- **Route:** `v1.cache.redis` (Q4). `@PreLoad(instances=N)` — N is the number of virtual-thread
  **worker instances** handling cache requests in parallel (function-level concurrency), *not* a
  Redis connection count. All instances share the backend's **one connection** (see below).
- **Connection model: a single shared Lettuce connection, no pool.** Lettuce is thread-safe and
  **multiplexes** — any number of threads pipeline their commands over one TCP connection, and Lettuce
  correlates the ordered replies back to each caller (Redis needs no special support; it sees one
  connection and executes/replies in arrival order — the RESP contract). A `commons-pool2` pool would
  be needed only for genuinely **blocking** commands (`BLPOP`/`BRPOP`/`WAIT`) or `MULTI`/`EXEC`
  transactions — and this op set has none (`LPOP` not `BLPOP`; the append-with-TTL is a single atomic
  `EVAL`; Pub/Sub, if ever needed, uses its own connection as sync-over-async does). So the module uses
  the `RedisBackend`'s single shared connection, exactly as sync-over-async already runs in production.
  (Add a small fixed number of round-robin connections only if profiling ever shows the single
  connection's netty event-loop thread saturating at extreme throughput — a later tuning knob, never a
  20-slot pool.)
- **Input:** `action` (header or field) + `key` / `keys` (for `MGET`) / `entries` (a `Map` for `MPUT`) +
  `value` + `ttl`. A typed input PoJo is preferred over loose headers for clarity and for Event Script
  data mapping.
- **Value type (Q3):** opaque **`byte[]`** — the cache stores and returns bytes; the caller owns
  serialisation (a typed convenience helper is deferred, §4.6 / Q7). Opaque bytes maximise interop
  (any layer, any language) and match `MGET -> Map<key, byte[]>`.
- **Output:** per action — `GET` → value or null; `MGET` → `Map<key, byte[]>` (misses omitted);
  `PUT`/`MPUT` → ack; `PUT_IF_NOT_PRESENT` → boolean; `DELETE` → count; `LIST_PUSH`/`LIST_LEN` → length;
  `LIST_POP` → value or null. Shaped so **output data mapping** (L2) and `graph.task` output (L3) can
  consume it directly.

### 4.5 Cluster-safe operation mapping

Every op maps to a cluster-correct Redis call through the `RedisBackend` seam:

- `PUT`/`GET`/`DELETE` — single-key, cluster-safe as-is.
- `PUT_IF_NOT_PRESENT` — **atomic `SET key value NX EX ttl`**, not `SETNX` + `EXPIRE` (two commands
  leave a TTL-less key if the process dies between them — the same "every key carries a TTL from
  birth" discipline the return route follows).
- `LIST_PUSH`/`LIST_POP`/`LIST_LEN` — single-key, cluster-safe as-is; `LIST_PUSH` reuses the return
  route's atomic `RPUSH`+`EXPIRE` Lua step so the list key is never left TTL-less.
- `MGET` — keys may span hash slots; **Lettuce's cluster client scatter-gathers a cross-slot `MGET`
  for free** (`RedisAdvancedClusterCommands.mget` splits by slot and merges), so the cluster backend
  handles it where a naive standalone client would `CROSSSLOT`.
- `MPUT` — a **pipelined batch of single-key `SETEX`**, not `MSET` (which sets no TTL): each `SETEX`
  routes to its own slot, so the map's keys may span slots freely and every key keeps its TTL. The
  batch is **not atomic** across the map — acceptable for a cache (entries are independent; a partial
  write is just cache misses), and unavoidable on a cluster anyway (an atomic multi-key write hits
  `CROSSSLOT` unless all keys share a slot).
- A multi-key `DELETE`, if offered, is split into single-key `DEL`s on cluster (as the return-route
  `cleanup` does).

### 4.6 Consumer surfaces

- **L1 (PostOffice):** `po.request(new EventEnvelope().setTo("v1.cache.redis").setHeader("action","GET").setHeader("key", k), timeout)`.
- **L2 (Event Script):** a task with `v1.cache.redis`, input data mapping sets `action`/`key`/`value`/
  `ttl`, output data mapping maps the result into `model`.
- **L3 (Knowledge Graph):** a `graph.task` node driving the same route.
- **Typed Java helper — DEFERRED (Q7).** A `CacheUtility`-style facade
  (`put(po,key,value,ttl)` / `get(po,key,Class)` / `mget(...)` / `mput(...)` / `putIfAbsent(...)` /
  `delete(...)` / list helpers + key-namespacing) wrapping the PostOffice call and serialisation can
  follow later; the function + flow surfaces above already cover all three layers.

### 4.7 Configuration (proposed)

Reuses the shared `redis.*` connection keys (host/port/username/password/ssl/database/timeout.ms/
cluster.detect/cluster.mode/cluster.nodes), plus cache tunables:

```properties
redis.cache.enabled=true            # opt-in master switch (@OptionalService)
redis.cache.instances=20            # virtual-thread worker instances (function concurrency), NOT connections;
                                    #   all share the backend's one multiplexed Lettuce connection (no pool)
redis.cache.default.ttl=1h          # default TTL when a PUT omits one (Q5)
redis.cache.key.prefix=             # optional namespace to isolate apps sharing one Redis (Q6)
redis.health.timeout=5s
redis.health.startup.grace=30s
```

### 4.8 Health check

Route **`redis.health`** (the reserved name) — the shared PING probe with the same waiting-vs-outage
semantics as `soa.redis.health`. Opt in via `mandatory.health.dependencies` /
`optional.health.dependencies`.

## 5. Contracts and invariants

- Functions stay decoupled — the cache is reached only by route name + `EventEnvelope`
  (`functions-decoupled-routes`); the L2/L3 surfaces are pure Event Script wiring.
- Every stored key carries a TTL from creation (no un-expiring keys); `PUT_IF_NOT_PRESENT` is atomic.
- Auth is identical for standalone and cluster and needs no vendor SDK (a token → `withPassword`, a
  username+password → `withAuthentication`); credentials arrive as `${ENV_VAR}` published by a
  lower-`sequence` credential bootstrap (`preload-before-mainapp-lazy-config`).
- The cache never depends on sync-over-async; both depend on the shared client foundation.

## 6. Failure analysis

- **Redis unreachable / auth not yet landed** — the `redis.health` check reports *waiting* (not 503)
  while credentials are absent, mirroring `soa.redis.health`; cache calls fail fast with the Redis
  error surfaced to the caller (the flow's exception handler decides fallback — cache-aside is the
  app's concern, not the module's).
- **Cluster `MOVED`/`ASK` redirects, topology change** — handled by the Lettuce cluster client.
- **Connection lifecycle** — the single shared connection is owned by the `RedisBackend` and closed
  once on shutdown; there is no per-request or per-instance connection creation, so the per-instance
  connection-leak class a pooled/per-worker design can hit does not arise here.

## 7. Non-goals

- **Not** a rendezvous / streaming transport — that is sync-over-async.
- **Not** a cache-aside framework — no automatic DB read-through/write-through or invalidation; the
  application orchestrates that with flows (a cache miss branch calling the source of truth).
- **Not** vendor-locked — no secrets-manager or cloud SDK dependency; credentials via config/env.
- **Not** a general Redis client for arbitrary commands — a bounded, cache-shaped operation set (Q5),
  not `EVAL`/pub-sub/streams (those are other modules' concerns).

## 8. Decisions (Q1–Q8, ruled by Eric 2026-09-14)

- **Q1 — Module home & name → ACCEPTED.** `extensions/distributed-cache` (opt-in add-on, like
  sync-over-async / minigraph-state-redis); route `v1.cache.redis`.
- **Q2 — Extract the shared client layer → YES.** Extract `RedisBackend` / `RedisConfig` /
  `RedisBackendFactory` / the health-probe logic into a `redis-connection` foundation
  (prefix-parameterised) that both sync-over-async and the cache depend on — a contained refactor of
  sync-over-async's `support/` package; no duplication, correct dependency direction.
- **Q3 — Value type → ACCEPTED.** Opaque `byte[]`; the caller owns serialisation. The typed helper is
  deferred (Q7).
- **Q4 — Route name → `v1.cache.redis`.**
- **Q5 — Operation set → the five key-value actions + bulk `MPUT` + `PING` + list push/pop/length.**
  Eric's rulings: `PING` backs the `redis.health` service (not a general action); the FIFO **list** ops
  (`LIST_PUSH`/`LIST_POP`/`LIST_LEN`) are added because their Redis machinery — atomic `RPUSH`+`EXPIRE`,
  destructive `LPOP`, `LLEN` — is already built for the return route; and **`MPUT`** is added as
  `MGET`'s bulk-write pair, implemented as a **pipelined per-entry `SETEX`** (not raw `MSET`, which sets
  no TTL) so every key keeps a TTL — non-atomic across the map (correct for a cache; unavoidable on a
  cluster). Deferred still: `EXISTS`/`INCR`/`EXPIRE`/`TTL`, and the whole-map Redis-Hash model
  (`HSET`/`HGETALL`) — a different data structure from the individual-keys `MPUT`/`MGET` model.
- **Q6 — Key namespacing → YES.** An app namespace via a configurable `redis.cache.key.prefix`.
- **Q7 — Consumer Java helper → DEFER.** The action function + Event Script / graph surfaces already
  cover all three layers; a typed `CacheUtility` facade can follow later.
- **Q8 — Cross-engine → RUST LOCKSTEP.** The Java module and the Rust port (`mercury`) ship together, as
  the engines do for shared features. Cache keys are plain Redis keys, so the two caches interoperate on
  the same keys with no wire change.

## 9. Relation to the blueprint

This is an **infrastructure module**, not a Blueprint gap item — it does not directly advance
`bp-agent-orchestration`. It serves the vision indirectly: lean, decoupled, composable modules
(`vision-mercury-composable` — "not a heavyweight runtime"), and it is a concrete opportunity to
**converge a field L2 cache library** onto the OSS platform instead of maintaining a parallel one.
