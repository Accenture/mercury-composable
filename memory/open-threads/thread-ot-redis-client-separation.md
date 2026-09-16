- [x] (docs — shipped 2026-09-16 in v4.12.10) **sync-over-async and distributed-cache should use
  separate `RedisClient`s; the `redis.*` fallback is backward compatibility, not a peer option.**
  Outcome: new *Separate Redis clients, by design* section in `distributed-cache.md` (#separation),
  matching reframe in `sync-over-async.md`, pointers from `configuration-reference.md`. No code
  change — `RedisBackendFactory` already builds a client per call. PR #399, squash `9aa40089`.
  Lesson: a backward-compatibility affordance reads as a recommendation unless the docs say what it
  is *for*. origin: 2026-09-15-221451.
  <!-- id: ot-redis-client-separation | created: 2026-09-15 | last_used: 2026-09-16 | uses: 2 | tier: archive-candidate | origin: 2026-09-16-003354 -->
