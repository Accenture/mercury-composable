- [ ] (defect candidate — minimalist-kafka, 2026-09-22) **Java Kafka consumers do not leave their groups on
  SIGTERM — register the adapter's `close()` (and the publisher's) on `Platform.onShutdown`.** Found by the
  two-engine OpenTelemetry drive with the Rust port (Java OTel report Scenario 7; Rust report Scenario 7): the
  broker fenced EVERY stopped Java member by session expiry ~40 s after SIGTERM (`fenced from the group because
  the member session expired`), while every Rust (rdkafka) member `left the consumer group` at once. Cost in the
  drive: the Rust facade joined `soa-reply-group` while the stopped Java facade still held all ten `soa.response`
  partitions — two replies published to parked partitions, two 408s. In production that is a rolling restart
  parking the old pod's partitions for the KIP-848 session timeout (broker default 45 s). Root: `KafkaFlowAdapter.close()`
  → `KafkaFlowConsumer.close()` (`consumer.wakeup()` + `consumer.close()`, which sends LeaveGroup) exists, but
  `KafkaFlowAutoStart.start()` never registers it — no `Platform.getInstance().onShutdown(adapter::close)`;
  `KafkaRequestPublisher.close()` (`producer.close()`, which flushes buffered records) is in the same position.
  Fix shape: register both on the platform shutdown lifecycle ([[platform-onshutdown-lifecycle]], the hook that
  exists for exactly this), with a bounded close timeout; verify with the broker log (`left the consumer group`
  on SIGTERM) and a Kubernetes-style SIGTERM test. **Also from the round (demo-level):** the Java
  `sync-over-async-demo`'s `SyncErrorHandler` calls `SyncRuntime.coordinator().abort(cid)` without a null check —
  a request arriving before `Return-route subscriber listening` (the REST port opens ~100 ms earlier) answers
  500 from an NPE instead of the flow's own error; a null check or readiness gating fixes it. Eric decides both.
  origin: 2026-09-21-233928.
  <!-- id: kafka-consumer-leave-group-on-shutdown | created: 2026-09-22 | last_used: 2026-09-22 | uses: 1 | tier: working | origin: 2026-09-21-233928 -->
