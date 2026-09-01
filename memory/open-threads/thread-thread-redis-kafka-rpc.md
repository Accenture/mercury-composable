- [ ] (next iteration — Eric, 2026-06-24; **design + implement**) **Cross-pod request-response via
  Redis Pub/Sub RPC + Kafka** — a distributed sync-over-async pattern (advanced opt-in, cf.
  `kafka-mesh-opt-in`): REST sync request → POD-1 → Kafka outbound; Kafka inbound (response) →
  POD-2 → Redis return route keyed by correlation-id back to POD-1. **Locked decisions:** return
  path = Redis (deliberately NOT the full mesh/presence discovery); client = Lettuce; module =
  `extensions/sync-over-async`; reliability cornerstones — Redis SETEX payload is the source of
  truth, pub/sub is wake-up only, a final Redis read before timeout is required; race-safe
  idempotent completion. **State: MVP COMPLETE (2026-06-26) and the Kafka legs promoted to the
  reusable `system/minimalist-kafka` library** (Eric's call) — sync-over-async is now purely the
  Redis return-route engine (96% cov). Full round-trip proven incl. OTel span propagation across
  Kafka (notification stamps its own span into traceparent; consumer chains the flow onto it).
  Subsequent minimalist-kafka growth (topic-pattern, dlq-topic, auto-commit, metadata.*,
  cid-header fix, terminology refactor businessCorrelationId/internalCorrelationId — Java
  identifiers only, wire strings unchanged) delivered via PR #133 + review rounds. **Still open
  (post-MVP): 503 guardrails/metrics, two-JVM test, per-module README; Gradle build
  ([[thread-add-gradle-build]]).** → serves `vision-mercury-composable`. Full detail: sessions
  2026-06-25 → 2026-07-04.
  <!-- id: thread-redis-kafka-rpc | created: 2026-06-24 | last_used: 2026-07-31 | uses: 8 | tier: working -->
