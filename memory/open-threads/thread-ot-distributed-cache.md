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
  **Worked example MERGED (2026-09-15, PR #392, squash `f66ac3f3`):** `examples/distributed-cache-example` — the same
  profile CRUD on all three layers over one `cache-demo:` cache (L1 code / L2 one flow with
  `v1.http.method.action` mapper + decision / L3 one graph with a payload-action decision node); 4/4
  e2e tests on embedded Redis incl. cross-layer interop; its L3 debugging yielded the engine guard
  [[minigraph-guarded-async-completion]]. **Cache VALUE format = plain MsgPack-packed Map**
  (`MsgPack.pack`/`unpack`), refactored from an EventEnvelope holder in PR #395 (Eric): the compacted
  `EventEnvelope.toBytes()` encoding is a Java-side wire format the Rust port cannot read, so the
  polyglot value must be plain MsgPack key-values — **the Rust cache client must pack/unpack the same
  format for the example to stay an interop harness.**
  **Polish round (2026-09-15, uncommitted at log time):** L3 collapsed onto the standard
  `/api/graph/{graph_id}` endpoint + stock `graph-executor` flow (bespoke `l3-profile.yml` deleted), dev
  mode + `scripts/` pre-wired in both the example and `templates/starter-graph`, and the `profile-cache`
  decision node turned into a CLOSED dispatch table — an unknown or absent action now answers HTTP-400
  from a `reject` node instead of falling through to DELETE. See [[minigraph-dev-mode-app-shape]].
  **Release path (Eric, 2026-09-15):** field Sonar scan of the new v4.12.9 code → successful field
  deployment → then cut the v4.12.9 release (per [[eric-release-rhythm]]); the Rust lockstep begins
  AFTER the release, not before.
  **REMAINING:** (4) Rust lockstep port in the `mercury` repo (Q8) — cache keys are plain Redis keys, so the
  two caches interoperate with no wire change (the example doubles as the interop harness once the
  Rust twin lands). Builds on [[cache-separate-from-soa]] +
  [[soa-redis-cluster-support]]; applied [[preload-before-mainapp-lazy-config]] and
  [[conv-reentrantlock-not-synchronized]]. See [[redis-connection-foundation]].
  → serves: vision-mercury-composable
  <!-- id: ot-distributed-cache | created: 2026-09-14 | last_used: 2026-09-17 | uses: 12 | tier: working | origin: 2026-09-14-214619 -->
