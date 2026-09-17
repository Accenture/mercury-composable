- [ ] **kafka.health produce-only classloader fix — awaiting field confirmation.** v4.12.11 fixed the
  consumer path only; the produce-only leg still failed because template resolution
  (`ConsumerConfig.configNames()`) triggers a Kafka config class's static initializer *before*
  `buildClient`, outside the override. Fix pushed on `fix/kafka-health-producer-only-classloader`
  (`bee2bc51`): the whole probe runs under the module classloader, and a `LinkageError` renders as a
  503 naming the configuration. Full reactor green; the three regression tests fail on the 4.12.11
  source ([[kafka-config-class-static-init-loader]]).
  **Open:** PR open + merge (Eric gates), then a patch release for the field — the field is blocked
  on a produce-only deployment answering 500 on `/health`. Once confirmed in the field, close and
  record whether the 503 text read usefully to the DevOps reader.
  **Watch for:** the Rust twin has no analogue (JVM classloading), so no lockstep — but the same
  *shape* (a pooled thread's ambient state deciding a one-shot global initialization) is worth a look
  wherever a port runs client setup off a pool.
  origin: `memory/sessions/2026-09-17-183008.md`
  <!-- id: ot-kafka-health-producer-only-fix | created: 2026-09-17 | last_used: 2026-09-17 | uses: 1 | tier: working | origin: 2026-09-17-183008 -->
