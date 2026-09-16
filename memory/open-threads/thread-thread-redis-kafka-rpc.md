- [x] (next iteration — Eric, 2026-06-24) **Cross-pod request-response via Redis Pub/Sub RPC +
  Kafka. DONE — closed 2026-09-16** on a stale-record flag from Copilot (the thread still listed
  Gradle support as open, long after PR #357 shipped it). MVP completed 2026-06-26; the Kafka legs
  were promoted to `system/minimalist-kafka` and sync-over-async became purely the Redis
  return-route engine. Every post-MVP residual is now satisfied or superseded: Gradle build (PR
  #357, [[thread-add-gradle-build]] archived), per-module README (`extensions/sync-over-async/`),
  two-JVM test (`docs/test-reports/streaming-return-route-cross-pod.md` — two JVMs, cross-pod, with
  chaos legs), and 503 guardrails (`StreamBridge` fails the exchange at stream capacity).
  **NOT done, deliberately dropped:** the "metrics" half of that item — there are no counters in the
  module. Three months of heavy development passed without anyone wanting them, and the module
  already exposes a health probe and per-task trace spans. Re-raise as a thread if a field
  deployment actually asks.
  **Durable lesson:** a checklist nested *inside* another record has no independent decay signal.
  This thread never decayed (unchecked threads never do), so its "still open" sub-list sat unread
  while every item was quietly delivered elsewhere. Second instance in one day — the same shape as
  the release commitment buried in continuity's `latest_release`. Track a commitment as a thread, or
  accept that it will go stale unnoticed.
  origin: sessions 2026-06-25 → 2026-07-04; closed per `memory/sessions/2026-09-16-211927.md`
  <!-- id: thread-redis-kafka-rpc | created: 2026-06-24 | last_used: 2026-07-31 | uses: 8 | tier: archive-candidate -->
