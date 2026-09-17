- [x] (next iteration → **DONE, closed 2026-09-16** on a stale-record flag from Copilot) **Cross-pod
  request-response via Redis Pub/Sub RPC + Kafka.** MVP complete 2026-06-26; Kafka legs promoted to
  `system/minimalist-kafka`, sync-over-async became the Redis return-route engine. All post-MVP
  residuals satisfied: Gradle (PR #357), per-module README, two-JVM test
  (`streaming-return-route-cross-pod.md`), 503 guardrails (`StreamBridge`). **Metrics deliberately
  dropped** — no counters, no demand in three months; re-raise if the field asks.
  **Lesson:** a checklist nested inside another record has no independent decay signal — the thread
  never decayed, so its sub-list rotted unread while every item shipped elsewhere.
  origin: sessions 2026-06-25 → 2026-07-04; closed per `memory/sessions/2026-09-16-211927.md`
  <!-- id: thread-redis-kafka-rpc | created: 2026-06-24 | last_used: 2026-07-31 | uses: 8 | tier: archive-candidate -->
