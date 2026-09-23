- [x] (defect — minimalist-kafka, 2026-09-22) **CLOSED 2026-09-22 — Java Kafka consumers now leave their group on
  shutdown.** PR #440 (squash `a9e5097c`, 04:18Z): `KafkaRuntime.shutdown()` (idempotent, ReentrantLock) closes the flow
  adapter's consumers (LeaveGroup) then the producer (flush), registered by `KafkaFlowAutoStart` on `Platform.onShutdown`;
  the twin-kafka pair likewise; `KafkaShutdownTest` pins the contract on an embedded broker; the demo `SyncErrorHandler`
  null-guarded; guide §"Shutdown: leaving the group" + claim `kafka-consumer-leaves-group-on-shutdown`. Live: SIGTERM →
  3 members left within 3 s, 0 fenced (before: fenced ~40 s later). Lesson: a resource-opening bootstrap must register
  its close on the platform lifecycle in the same place it opens the resource — an existing `close()` nobody calls is a
  latent outage of exactly the session-timeout length. Found by the two-engine OTel drive with the Rust port; the Rust twin check landed as mercury #312 (2026-09-22).
  origin: 2026-09-21-233928.
  <!-- id: kafka-consumer-leave-group-on-shutdown | created: 2026-09-22 | last_used: 2026-09-21 | uses: 1 | tier: active | origin: 2026-09-21-233928 -->
