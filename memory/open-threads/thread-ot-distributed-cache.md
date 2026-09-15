- [ ] (feature) **Distributed cache module — JAVA SHIPPED for v4.12.9; Rust lockstep remains.**
  Extracted the shared **`extensions/redis-connection`** foundation from sync-over-async's `support/`
  (generic `RedisBackend<V>` by value codec — `<String>` for sync-over-async, `<byte[]>` for the cache;
  prefix-parameterised `RedisConfig.from(config, prefix)` with the `redis.*` fallback; `RedisHealthProbe`
  as un-annotated reusable logic, bound per-module by a thin `@PreLoad` subclass — `soa.redis.health` /
  `redis.health`), then built **`extensions/distributed-cache`**: the `v1.cache.redis` action function
  (PUT/GET/MGET/MPUT/DELETE/PUT_IF_NOT_PRESENT + FIFO LIST_PUSH/POP/LEN, opaque `byte[]`, TTL-from-birth,
  MPUT = pipelined per-entry SETEX). One shared multiplexed connection, **no pool**; lazy `CacheRuntime`
  (app starts even if Redis is down; late credentials picked up on retry) using a **ReentrantLock, not
  `synchronized`** (Java-21 VT carrier pinning — Eric's mid-session directive). Commits `5b73f311` (refactor)
  + `bb88e65c` (cache) on branch `feat/redis-cache-foundation`; redis-connection 30 tests, distributed-cache
  32, sync-over-async 62, full reactor green. PR-open is Eric's gate (two logical commits, splittable).
  **REMAINING:** (4) Rust lockstep port in the `mercury` repo (Q8) — cache keys are plain Redis keys, so the
  two caches interoperate with no wire change. Builds on [[cache-separate-from-soa]] +
  [[soa-redis-cluster-support]]; applied [[preload-before-mainapp-lazy-config]] and
  [[conv-reentrantlock-not-synchronized]]. See [[redis-connection-foundation]].
  → serves: vision-mercury-composable
  <!-- id: ot-distributed-cache | created: 2026-09-14 | last_used: 2026-09-15 | uses: 4 | tier: working | origin: 2026-09-14-214619 -->
