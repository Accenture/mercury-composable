- [x] (feature) **Distributed cache module — CLOSED 2026-09-19, shipped in both engines.** Java v4.12.9:
  the shared `extensions/redis-connection` foundation, `extensions/distributed-cache` (`v1.cache.redis`, nine
  actions over opaque `byte[]`, `redis.health`) and the three-layer example (refactor `5b73f311`, cache
  `bb88e65c`, example #392 `f66ac3f3`, plain-MsgPack values #395). Rust: mercury #285 (merge `5ac55eaf`,
  Increment 119) — byte-compatible actions, headers, key layout and config keys, `Platform::on_shutdown`, and
  two Rust-only parity gaps fixed on the way (REST error body, typed-function `EventEnvelope` reply).
  **Proof RUN 2026-09-20:** the two examples side by side on one `redis-standalone` — 112/112 hard checks
  (`docs/test-reports/distributed-cache-interop.md`, twin in the Rust repo); it found the Layer 1 false-miss
  on a cache failure in BOTH examples, fixed on branches pending Eric's PR gate ([[l1-caller-checks-reply-status]]).
  Lesson: copied parity assertions, not code reading, caught both twin gaps — transcribe the reference suite
  into the port. → serves: vision-mercury-composable
  origin: `memory/sessions/2026-09-14-214619.md` (creation) → close record: `2026-09-19-020551.md`
  <!-- id: ot-distributed-cache | created: 2026-09-14 | last_used: 2026-09-17 | uses: 12 | tier: working | origin: 2026-09-14-214619 -->
