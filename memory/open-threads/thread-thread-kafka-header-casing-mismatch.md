- [x] (defect → **FIXED 2026-09-16**) **Mixed-case Kafka headers were unreachable from Event Script
  mapping** — the engine lowercases `input.header.*` while the Kafka adapter delivers wire casing, so
  a producer-sent `Content-Type` matched nothing. Fixed in `TaskExecutor` as a case-insensitive lookup
  retried only when the direct lookup misses, so HTTP is untouched; the scan is package-private
  `headerIgnoringCase` and tested directly.
  **Lesson:** normalize at the point of USE, not at ingestion, when the ingested form is part of
  someone else's contract — lowercasing at the adapter would have changed what `*` passthrough hands
  a function.
  origin: `memory/sessions/2026-09-16-234646.md`
  <!-- id: thread-kafka-header-casing-mismatch | created: 2026-07-30 | last_used: 2026-09-16 | uses: 2 | tier: active | origin: 2026-07-30-233623 -->
