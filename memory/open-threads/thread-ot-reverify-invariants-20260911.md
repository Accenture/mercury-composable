- [ ] **Re-verify invariants (due — 45 sessions since the 2026-09-04 check ≥ 40):**
  confirm the never-decay facts still hold, or supersede any that don't (DECAY.md §9):
  stack-language-java21, stack-build-maven, stack-integration-spring-boot4,
  stack-messaging-kafka, stack-ci-gha, functions-decoupled-routes, typed-io-map-or-pojo,
  virtual-threads-rpc, trace-thread-keyed-mono-gotcha, instant-serialization,
  kafka-mesh-opt-in, event-script-over-code, event-api-local-routes-only,
  conv-telemetry-presentation-parity, conv-add-capability, conv-serialization-gotchas,
  conv-declare-consulted-references, eric-release-rhythm — and the Vision
  (memory/vision.md). One candidate wording note for the walkthrough: stack-build-maven
  says "Gradle support is planned to be added alongside it (see Open Thread
  thread-add-gradle-build)" — the starter templates (PR #357) now ship a Gradle option
  for consumer applications (templates only; the engine reactor stays Maven), so Eric
  may want to refresh that sentence and rule whether thread-add-gradle-build is now
  complete or stays open for engine-side work.
  <!-- id: ot-reverify-invariants-20260911 | created: 2026-09-11 | last_used: 2026-09-11 | uses: 1 | tier: working | origin: 2026-09-11-005808 -->
