- [x] **The Rust port caught up with the minimalist-kafka guide gaps and the dead-letter key - and went further, on the
  Confluent client's mechanisms** (done 2026-10-30, mercury #399 merge `79b3755d`, Increments 191-193): the dead-letter copy
  keeps the record key; the registry client retries as Confluent's `RetryExecutor` does (it had no retry at all, so Eric's
  "retry unless the client has one" applied); the caches follow Confluent's after the Java ruling (#598); `libz-sys` 1.1.31
  for RUSTSEC-2026-0418 rode along to clear the audit gate. Lesson: a port twin starts by confirming what the Java engine
  gets from its libraries - the Rust "twin" of a docs-only gap needed two behaviours Java never had to write.
  - origin: 2026-10-30-182330
  <!-- id: minimalist-kafka-guide-rust-twin | created: 2026-10-30 | last_used: 2026-10-30 | uses: 1 | tier: working | origin: 2026-10-30-182330 -->
