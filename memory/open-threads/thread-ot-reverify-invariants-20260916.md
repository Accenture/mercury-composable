- [ ] Re-verify invariants (due — 44 sessions since 2026-09-11, `verify_invariants_every` 40):
  confirm the **18 core facts** and **the Vision** still hold, or supersede any that don't
  (DECAY.md §9). The review never auto-invalidates an invariant — it only prompts; Eric confirms by
  checking this off, or supersedes the false ones.
  **Architectural Invariants (3):** functions-decoupled-routes · typed-io-map-or-pojo ·
  virtual-threads-rpc.
  **Stack (5):** stack-language-java21 · stack-build-maven · stack-integration-spring-boot4 ·
  stack-messaging-kafka · stack-ci-gha.
  **Key Decisions (5):** kafka-mesh-opt-in · event-script-over-code · event-api-local-routes-only ·
  trace-thread-keyed-mono-gotcha · instant-serialization.
  **Conventions (4):** conv-telemetry-presentation-parity · conv-add-capability ·
  conv-serialization-gotchas · conv-declare-consulted-references.
  **User Preferences (1):** eric-release-rhythm.
  **Plus the Vision** (`memory/vision.md`).
  Two worth a closer look this round, flagged rather than pre-judged — the review does not rule on
  them: (a) **stack-language-java21** carries "the toolchain STAYS on 21 until the majority of field
  installations run Java 25", which is a field-state claim that ages; (b)
  **conv-reentrantlock-not-synchronized** (not core, but tied to it) is explicitly written to decay
  once the toolchain moves to Java 25 — so (a) and (b) resolve together.
  → serves: vision-mercury-composable
  <!-- id: ot-reverify-invariants-20260916 | created: 2026-09-16 | last_used: 2026-09-16 | uses: 1 | tier: working | origin: 2026-09-16-041500 -->
