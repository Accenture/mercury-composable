- [ ] (invariant re-verification — DUE: 48 sessions since `2026-08-21-005515.md`, cadence
  `verify_invariants_every: 40`) Confirm each never-decay fact still holds, or supersede any that
  no longer do (`DECAY.md` §9). The review never auto-invalidates — a human confirms.

  **Architectural Invariants (3):** `functions-decoupled-routes` (ADR-0001) ·
  `typed-io-map-or-pojo` (ADR-0003) · `virtual-threads-rpc` (ADR-0002)

  **Stack & Tools (5):** `stack-language-java21` · `stack-build-maven` ·
  `stack-integration-spring-boot4` · `stack-messaging-kafka` · `stack-ci-gha`

  **Key Decisions / Conventions at `tier: core` (7):** `trace-thread-keyed-mono-gotcha` ·
  `instant-serialization` · `kafka-mesh-opt-in` (ADR-0006) · `event-script-over-code` (ADR-0007) ·
  `event-api-local-routes-only` · `conv-add-capability` · `conv-serialization-gotchas`

  **The Vision:** `vision-mercury-composable` (`memory/vision.md`) — elevator statement,
  target state, non-goals.

  Note for this round: `stack-messaging-kafka` names the Kafka family and is the one most
  recently touched by shipped work — the per-cluster producer/consumer opt-out (PR #315) added a
  config surface to `minimalist-kafka` / `twin-kafka` without changing the fact's substance.
  Worth a deliberate look when confirming.

  <!-- id: ot-reverify-invariants-20260904 | created: 2026-09-04 | last_used: 2026-09-04 | uses: 1 | tier: working | origin: 2026-09-04-040305 -->
