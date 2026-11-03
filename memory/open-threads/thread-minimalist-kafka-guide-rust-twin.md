- [x] **The Rust port caught up with the minimalist-kafka guide gaps and the dead-letter key** (done 2026-10-30, mercury #399
  merge `79b3755d`, Increments 191-193): the dead-letter copy keeps the record key; the registry client retries as
  Confluent's `RetryExecutor` does (it had none); the caches follow Confluent's after the Java ruling (#598). Lesson: a port
  twin starts by confirming what the Java engine gets from its libraries - the Rust "twin" of a docs-only gap needed two
  behaviours Java never had to write.
  - origin: 2026-10-30-182330
  <!-- id: minimalist-kafka-guide-rust-twin | created: 2026-10-30 | last_used: 2026-10-30 | uses: 2 | tier: archive-candidate | origin: 2026-10-30-182330 -->
