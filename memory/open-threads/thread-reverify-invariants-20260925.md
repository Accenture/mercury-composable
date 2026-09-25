- [ ] **Re-verify invariants (due 2026-09-25 — 40 sessions since the 2026-09-16 check):** confirm that the never-decay facts
  still hold, or supersede any that don't (DECAY.md §9) — the three Architectural Invariants `functions-decoupled-routes`,
  `typed-io-map-or-pojo`, `virtual-threads-rpc`; the stack `stack-language-java21`, `stack-build-maven`,
  `stack-integration-spring-boot4`, `stack-messaging-kafka`, `stack-ci-gha`; the core decisions `trace-thread-keyed-mono-gotcha`,
  `instant-serialization`, `kafka-mesh-opt-in`, `event-script-over-code`, `event-api-local-routes-only`; the core conventions
  `conv-add-capability`, `conv-serialization-gotchas`, `conv-declare-consulted-references`, `conv-proposals-not-in-adr-ledger`;
  the preferences `eric-release-rhythm`, `eric-code-changes-via-pr`; and the Vision `vision-mercury-composable` — **this time
  against the 2026-09-17 two-track text** (Track 1 graph-as-application delivered; Track 2 graph-as-AI-SDLC in design, AI a
  run-time participant), not the 2026-06-20 one. Walk each against live-tree evidence with Eric; a fact that no longer holds
  gets a working-tier successor with `supersedes:`. Raised by the 2026-09-25 review; the review itself confirms nothing.
  → serves: vision-mercury-composable
  <!-- id: reverify-invariants-20260925 | created: 2026-09-25 | last_used: 2026-09-25 | uses: 1 | tier: working | origin: 2026-09-25-014100 -->
