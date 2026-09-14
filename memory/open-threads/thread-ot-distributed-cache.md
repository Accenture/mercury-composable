- [ ] (feature) **Distributed cache module — DESIGN RULED (Eric, Q1–Q8, 2026-09-14); implementation
  gated behind v4.12.9.** Generic Redis-backed L2 cache as `extensions/distributed-cache` (route
  `v1.cache.redis`), reusing sync-over-async's Redis client layer via an EXTRACTED
  `redis-connection` foundation (Q2, prefix-parameterised: `redis.*` for the cache, `soa.redis.*`
  for sync-over-async). Op set (Q5): PUT/GET/MGET/MPUT/DELETE/PUT_IF_NOT_PRESENT + PING (backs
  `redis.health`) + list push/pop/length; opaque `byte[]` (Q3); app key-prefix (Q6); typed helper
  deferred (Q7); RUST LOCKSTEP (Q8). MPUT = pipelined per-entry SETEX (TTL-preserving, non-atomic —
  NOT MSET, which has no TTL). Spec: draft-design-specs/distributed-cache.md. Sequence: (1) extract
  the `redis-connection` foundation from sync-over-async's support/, (2) cache module + action
  function (L1/L2/L3 surfaces), (3) `redis.health`, (4) Rust port in lockstep. Serves the lean-module
  vision; builds on [[cache-separate-from-soa]] + [[soa-redis-cluster-support]]; a chance to converge
  a field L2 cache library.
  → serves: vision-mercury-composable
  <!-- id: ot-distributed-cache | created: 2026-09-14 | last_used: 2026-09-14 | uses: 1 | tier: working | origin: 2026-09-14-214619 -->
