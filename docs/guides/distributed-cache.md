---
title: Distributed Cache
summary: The opt-in distributed-cache extension — a generic Redis-backed L2 cache exposed as one composable
  action function (v1.cache.redis) over opaque byte[] values, usable from all three layers.
layer: operate
audience: [developer, ai-agent]
keywords: [distributed cache, redis, l2 cache, v1.cache.redis, lettuce, setex, mget, mput,
  put if not present, key-value, ttl, byte array, cluster, redis.cache, redis.health, opt-in]
---

# Distributed Cache

*Guide: a generic Redis-backed L2 distributed cache, exposed as one composable function you call from
any layer.*

> **At a glance**
>
> - **What** — a shared key-value cache backed by Redis, exposed as a single composable action function at
>   route **`v1.cache.redis`**. One `action` header selects the operation; values are opaque `byte[]`.
> - **How** — `PUT`/`GET`/`MGET`/`MPUT`/`DELETE`/`PUT_IF_NOT_PRESENT` plus FIFO `LIST_PUSH`/`LIST_POP`/
>   `LIST_LEN`. Every stored key carries a TTL from creation. One shared, multiplexed Lettuce connection —
>   no pool.
> - **Advanced & opt-in** — off by default; enable with `redis.cache.enabled=true`. Standalone or
>   Redis Cluster with no code change.
> - **For** developers who need a cache shared across pods and across the three layers (Platform Core,
>   Event Script, Knowledge Graph).

The cache is a thin composable module over the same Redis client layer that powers
[sync-over-async](sync-over-async.md) — the shared **`redis-connection`** foundation (standalone/cluster
selection, auth, TLS, health probe). It adds only the cache *operations* and the composability surfaces;
it is **not** a rendezvous transport (that is sync-over-async) and **not** a cache-aside framework — your
flow orchestrates read-through / write-through, the module just stores and returns bytes.

> **Opt-in.** The cache function and its health check register only when `redis.cache.enabled=true`. The
> connection is built **lazily on first use**, so enabling it does not fail application start-up when Redis
> is briefly unreachable — a cache call then fails fast and the flow's exception handler decides the
> fallback.

## Operations {#operations}

The `action` header (case-insensitive) selects the operation. The key(s) and TTL ride in headers; the
value(s) ride in the body:

| `action` | Other headers | Body (input) | Result | Redis |
|----------|---------------|--------------|--------|-------|
| `PUT` | `key`, `ttl`? | value (`byte[]`) | `true` | `SETEX` |
| `GET` | `key` | — | value (`byte[]`), or `null` on a miss | `GET` |
| `DELETE` | `key` | — | count removed (`long`) | `DEL` |
| `PUT_IF_NOT_PRESENT` | `key`, `ttl`? | value (`byte[]`) | `boolean` — `true` if stored, `false` if the key existed | `SET NX EX` |
| `MGET` | — | keys (`List<String>`) | `Map<String, byte[]>` (misses omitted) | `MGET` |
| `MPUT` | `ttl`? | entries (`Map<String, byte[]>`) | `true` | pipelined per-entry `SETEX` |
| `LIST_PUSH` | `key`, `ttl`? | value (`byte[]`) | new list length (`long`) | `RPUSH` + `EXPIRE` (atomic) |
| `LIST_POP` | `key` | — | oldest value (`byte[]`), or `null` if empty | `LPOP` |
| `LIST_LEN` | `key` | — | list length (`long`) | `LLEN` |

Notes for callers:

- **`ttl`** is a duration string (`30s`, `5m`, `1h`). When a write omits it, the cache uses
  `redis.cache.default.ttl` (default `1h`). **Every stored key carries a TTL from creation** — there are no
  un-expiring keys; `PUT_IF_NOT_PRESENT` is atomic (`SET … NX EX`, never `SETNX` then `EXPIRE`).
- **`MPUT`** is a pipelined batch of single-key `SETEX` — one round trip, and each key keeps its own TTL
  (a raw `MSET` sets none). It is **not atomic** across the map (a partial failure is just cache misses),
  which is correct for a cache and unavoidable on a cluster.
- **`MGET`** keys may span cluster hash slots — the Lettuce cluster client scatter-gathers them for you.
- An unknown `action`, a missing `key`, or a missing value raises `IllegalArgumentException`, surfaced to
  the caller as the event's error.

## Enabling and configuring {#config}

Add the `distributed-cache` jar to the classpath and enable it. The connection uses the plain **`redis.*`**
namespace (the base namespace of the shared foundation), so it never collides with sync-over-async's
`soa.redis.*`:

```properties
redis.cache.enabled=true              # master switch: registers v1.cache.redis + redis.health
redis.cache.default.ttl=1h            # default TTL when a write omits 'ttl'
redis.cache.key.prefix=app1:          # optional: prepended to every key (namespace apps sharing one Redis)

redis.host=${REDIS_HOST:127.0.0.1}
redis.port=${REDIS_PORT:6379}
redis.username=${REDIS_USERNAME:}     # blank = default user; set for an ACL/RBAC user
redis.password=${REDIS_PASSWORD:}     # blank = no auth; keep secrets in the environment
redis.cluster.detect=auto             # auto = detect at start-up; else use the boolean below
redis.cluster.mode=false              # true = cluster, false = standalone (when detect is not auto)
```

The essentials — see the [Configuration Reference](configuration-reference.md#distributed-cache) for the
full list:

| Key | Default | Description |
|-----|---------|-------------|
| `redis.cache.enabled` | `false` | Master switch; `true` registers `v1.cache.redis` and `redis.health`. |
| `redis.cache.instances` | `20` | Virtual-thread **worker instances** (function concurrency), **not** a connection count — every instance shares the one multiplexed connection. |
| `redis.cache.default.ttl` | `1h` | TTL applied when a `PUT` / `MPUT` / `LIST_PUSH` omits `ttl`. |
| `redis.cache.key.prefix` | — (blank) | Prepended to every key; stripped again from `MGET` results. Isolate apps sharing one Redis. |
| `redis.host` / `redis.port` | `127.0.0.1` / `6379` | Redis connection (or the cluster configuration endpoint). |
| `redis.username` / `redis.password` | — (blank) | ACL/RBAC username / auth password. Source from the environment. |
| `redis.ssl` | `false` | Use TLS (`rediss://`). |
| `redis.cluster.detect` / `redis.cluster.mode` / `redis.cluster.nodes` | `auto` / `false` / — | Standalone-or-cluster selection — the same two-key scheme as [sync-over-async](sync-over-async.md#cluster). |
| `redis.timeout.ms` | `5000` | Default command timeout. |
| `redis.health.timeout` | `5s` | Timeout for the [`redis.health`](#health) probe. |
| `redis.health.startup.grace` | `30s` | Start-up grace for [`redis.health`](#health). |

### Separate Redis clients, by design {#separation}

**When an application runs both the cache and [sync-over-async](sync-over-async.md), give each its own
Redis client — configure the cache under `redis.*` and sync-over-async under `soa.redis.*`, fully.**
That is the intended shape, not merely a supported one. Nothing extra is needed to get it:
`RedisBackendFactory` builds a *new* client from whatever configuration it is handed, and every
endpoint-defining key — `host`, `port`, `username`, `password`, `ssl`, `database`, `timeout.ms`, and all
three `cluster.*` keys — resolves per namespace. The two can therefore differ in server, credentials,
TLS, and even topology: one standalone, one cluster-mode-enabled.

Why it matters:

- **Separation of concerns.** They are different things. Sync-over-async is a *rendezvous transport* whose
  keys live for the duration of one request; the cache is a *store* whose keys live for their TTL. Their
  sizing, eviction, and failure characteristics have nothing to do with each other.
- **A cache evicts; a rendezvous must not.** This is the sharp one. A cache under memory pressure with an
  eviction policy configured will evict whatever fits its policy — including a `request:{cid}` rendezvous
  key, **mid-request**. The caller then waits for a reply that can never arrive. Separate instances make
  that failure mode structurally impossible; a shared instance only avoids it by configuration discipline.
- **Independent operations.** Restarting, resizing, or failing over the cache should not disturb in-flight
  synchronous requests.

> **What the `redis.*` fallback is for.** Each `soa.redis.*` key falls back to the un-prefixed `redis.*`
> form when the prefixed one is absent. That exists for **backward compatibility** — sync-over-async
> predates the cache and was configured under plain `redis.*`, so those deployments keep working
> untouched, and an application running sync-over-async **alone** may still use `redis.*` throughout. It
> is not an invitation to share one instance between the two modules.

> **Override a namespace completely, or not at all.** Because the fallback is *per key*, a partial
> override silently mixes the two: setting `soa.redis.host` but not `soa.redis.password` points
> sync-over-async at the new host carrying the **cache's** credentials, and authentication fails. When you
> decouple, set the whole `soa.redis.*` connection set.

> **Both probes, both endpoints.** Two clients mean two health checks. List them together —
> `mandatory.health.dependencies=redis.health, soa.redis.health` — or one endpoint goes unmonitored.

Credentials follow the same pattern in both namespaces — resolved from the environment by a
lower-`sequence` `@MainApplication` credential bootstrap (see
[sync-over-async → auth](sync-over-async.md#cluster)).

> **Sharing one server anyway?** It is safe for correctness — the key shapes do not collide
> (`request:{cid}` / `queue:{cid}` versus your `redis.cache.key.prefix` namespace) — but you own the
> eviction-policy risk above, and the two workloads share one memory budget. Note also that
> `database` is **standalone-only**: Redis Cluster is database 0, so "one cluster, two logical databases"
> is not an alternative to two instances.

## Using the cache {#usage}

The same function is reachable from all three layers — it is just "call a route".

### Layer 1 — PostOffice {#layer1}

```java
var po = PostOffice.trackable(headers, instance);

// PUT: the value is the body; ttl is optional (defaults to redis.cache.default.ttl)
byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(profile);
po.request(new EventEnvelope().setTo("v1.cache.redis")
        .setHeader("action", "PUT").setHeader("key", "profile:42").setHeader("ttl", "10m")
        .setBody(payload), 5000).get();               // .get() suspends the virtual thread, no kernel-thread block

// GET: a miss returns a null body
EventEnvelope res = po.request(new EventEnvelope().setTo("v1.cache.redis")
        .setHeader("action", "GET").setHeader("key", "profile:42"), 5000).get();
byte[] cached = res.getBody() instanceof byte[] bytes ? bytes : null;   // null = cache miss
```

### Layer 2 — Event Script task {#layer2}

Drive it from a flow with input/output data mapping — a constant sets the `action`, `model.*` supplies the
key, and the `byte[]` value rides the whole-body `*` passthrough:

```yaml
tasks:
  # write-through: cache the serialized profile under a 10-minute TTL
  - input:
      - 'text(PUT) -> header.action'
      - 'model.cacheKey -> header.key'
      - 'text(10m) -> header.ttl'
      - 'model.profileBytes -> *'          # the byte[] value rides in the body
    process: 'v1.cache.redis'
    output:
      - 'result -> model.stored'           # the PUT ack (true)
    description: 'Cache the profile'
    execution: sequential
    next:
      - 'read.back'

  # read: GET returns the value, or null on a miss
  - input:
      - 'text(GET) -> header.action'
      - 'model.cacheKey -> header.key'
    process: 'v1.cache.redis'
    output:
      - 'result -> model.cached'           # byte[] value, or null on a miss (branch on it with a decision task)
    description: 'Read the cached profile'
    execution: sequential
```

### Layer 3 — Knowledge Graph node {#layer3}

A `graph.task` node calls the same route with the same mapping syntax:

```json
{
  "route": "graph.task",
  "task": "v1.cache.redis",
  "input": [
    "text(GET) -> header.action",
    "model.cacheKey -> header.key"
  ],
  "output": [
    "result -> model.cached"
  ]
}
```

## Value type & serialisation {#values}

Values are **opaque `byte[]`** — the cache stores and returns raw bytes and the caller owns serialisation.
This maximises interop: any layer, and any language (the Rust port reads and writes the same keys). A
`String` body is accepted as a UTF-8 convenience, but the canonical value type is `byte[]`; use
`SimpleMapper` (or your own codec) to (de)serialise your objects. A typed convenience helper is deliberately
**not** provided — the function plus the flow/graph surfaces already cover all three layers.

## Standalone or cluster Redis {#cluster}

The cache runs against a **single-node** Redis or a **Redis Cluster** with no code change — it reuses
sync-over-async's client layer, so the two-key selection (`redis.cluster.detect` / `redis.cluster.mode` /
`redis.cluster.nodes`) and authentication work identically; see
[sync-over-async → Standalone or cluster Redis](sync-over-async.md#cluster) for the full detail.

Every operation is **cluster-safe by construction**: `PUT`/`GET`/`DELETE`/`PUT_IF_NOT_PRESENT` and the list
ops are single-key; `MGET` keys may span slots and are scatter-gathered by the cluster client; `MPUT` is a
pipeline of independent single-key `SETEX` (each routes to its own slot). The module uses **one shared,
multiplexed** Lettuce connection — **no connection pool**: Lettuce pipelines any number of concurrent
callers over one in-order TCP connection, and this op set has no blocking commands (`LPOP`, not `BLPOP`)
or `MULTI`/`EXEC` transactions that would warrant a pool. `redis.cache.instances` is virtual-thread worker
concurrency, not a connection count.

## Health check {#health}

The module ships a health-check function at route **`redis.health`** (registered with the cache when
`redis.cache.enabled=true`). Opt in as a health dependency:

```properties
mandatory.health.dependencies=redis.health
# or, to report Redis without failing /health:
# optional.health.dependencies=redis.health
```

The probe is a single Redis **PING** on a dedicated connection built from the `redis.*` parameters — one
round trip proves connectivity, TLS, and authentication. Its semantics match
[`soa.redis.health`](sync-over-async.md#health) exactly: config is resolved **lazily** (so a
vault-published credential that lands after start-up is picked up), an unusable configuration or a rejected
credential (`NOAUTH` / `WRONGPASS`) reports a **passing** `Waiting for Redis connection` status rather than
failing `/health`, and only a genuine connectivity failure returns **503**. `redis.health.timeout`
(default `5s`) bounds the probe; `redis.health.startup.grace` (default `30s`) is the start-up placeholder
window.

> `redis.health` is the plain-named counterpart to sync-over-async's `soa.redis.health`; the two coexist,
> each reporting on its own `redis.*` / `soa.redis.*` server.

## When to use it {#when}

Reach for the distributed cache when you need a **shared L2 key-value cache** — cross-pod, cross-instance,
and reachable from any of the three layers — with TTL'd entries and a small, cache-shaped operation set.
It is opt-in: if a per-instance in-memory cache suffices, use that instead. It is **not**:

- a **rendezvous / streaming transport** — that is [sync-over-async](sync-over-async.md);
- a **cache-aside framework** — there is no automatic DB read-through/write-through or invalidation; your
  flow orchestrates that (a cache-miss branch calling the source of truth);
- a **general Redis client** — the operation set is bounded and cache-shaped, not arbitrary `EVAL` /
  pub-sub / streams.

The design rationale and the ruled decisions (Q1–Q8) live in the
[distributed-cache design spec](https://github.com/Accenture/mercury-composable/blob/main/draft-design-specs/distributed-cache.md).

## See also

- [Sync-over-Async](sync-over-async.md) — the sibling opt-in module that shares the same Redis client layer.
- [Configuration Reference](configuration-reference.md#distributed-cache) — every `redis.cache.*` / `redis.*` key.
- [Event Script Syntax](event-script/syntax.md) — the input/output data-mapping syntax the Layer 2 / Layer 3 examples use.
- [Observability](observability.md) — tracing a cache call end-to-end.
