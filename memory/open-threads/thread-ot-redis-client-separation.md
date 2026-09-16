- [x] (docs — shipped 2026-09-16 for v4.12.10) **Document that sync-over-async and distributed-cache SHOULD use separate
  `RedisClient` instances — the `redis.*` fallback is backward compatibility, not a peer option
  (Eric, 2026-09-15; for v4.12.10, after v4.12.9 ships).** Verified in code: `RedisBackendFactory.create()`
  is a plain static factory building a NEW `RedisClient` per call — no singleton, no client cache — and
  every endpoint-defining key resolves per prefix (`host`, `port`, `username`, `password`, `ssl`,
  `database`, `timeout.ms`, `cluster.detect`/`mode`/`nodes`). So two Redis instances (e.g. two AWS
  ElastiCache endpoints, differing in topology) work today; no code change needed. What is missing is the
  RECOMMENDATION: `distributed-cache.md` (~L104 "One Redis, or two?") and `sync-over-async.md` (~L84)
  both present shared-vs-separate NEUTRALLY. Rewrite both to state separate clients as the intended
  shape, and say what the fallback is FOR — sync-over-async deployed ALONE, so pre-namespace configs
  keep working. Three points a reader cannot derive from the config table: (1) separation of concerns —
  a request-scoped rendezvous transport and a cache have unrelated lifecycles; (2) the sharp operational
  reason — a cache with an eviction policy under memory pressure will evict a `request:{cid}` rendezvous
  key MID-REQUEST, a failure mode with no equivalent when the clients are separate; (3) the
  partial-override trap — `soa.redis.host` without a matching `soa.redis.password` points at the new host
  carrying the other module's credentials and fails auth, so override the namespace completely or not at
  all. Also: `configuration-reference.md` should point at the recommendation from the `soa.redis.*` keys,
  health guidance should list BOTH probes (`mandatory.health.dependencies=redis.health, soa.redis.health`
  — a two-instance deployment monitoring one endpoint is a real gap), and add a CHANGELOG entry under
  v4.12.10. Note `database` is standalone-only (Redis Cluster is db 0), so "one cluster, two logical
  databases" is NOT an alternative to two instances. Builds on [[redis-connection-foundation]] and
  [[soa-redis-cluster-support]]; relates [[cache-separate-from-soa]].
  **DONE:** new *Separate Redis clients, by design* section in `distributed-cache.md` (#separation)
  replacing the neutral "One Redis, or two?" note; the matching reframe in `sync-over-async.md`; pointers
  from both `configuration-reference.md` sections. All three points landed, the eviction-mid-request one
  as the decisive argument. Durable lesson: a backward-compatibility affordance reads as a recommendation
  unless the docs say otherwise — state what a fallback is *for*.
  → serves: vision-mercury-composable
  <!-- id: ot-redis-client-separation | created: 2026-09-15 | last_used: 2026-09-16 | uses: 2 | tier: active | origin: 2026-09-16-003354 -->
