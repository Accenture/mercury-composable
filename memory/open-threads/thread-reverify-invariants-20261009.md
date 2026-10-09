- [ ] **Re-verify invariants (due):** confirm stack-language-java21, stack-build-maven, stack-integration-spring-boot4, stack-messaging-kafka, stack-ci-gha, functions-decoupled-routes, typed-io-map-or-pojo, virtual-threads-rpc, trace-thread-keyed-mono-gotcha, kafka-mesh-opt-in, event-script-over-code, event-api-local-routes-only, conv-add-capability, conv-serialization-gotchas, conv-declare-consulted-references, conv-proposals-not-in-adr-ledger, conv-compartment-downstream-secrets, eric-release-rhythm, eric-code-changes-via-pr and the Vision (`memory/vision.md`, the 2026-09-17 two-track text) still hold, or
  supersede any that don't (DECAY.md §9). Raised at the 2026-10-09 review: 59 sessions since the 2026-09-25 walk (`2026-09-25-022841`),
  cadence 40. 19 `core` facts (the 3 Architectural Invariants among them) + the Vision. Candidates worth a second look while walking:
  `stack-language-java21` (the toolchain stays on 21 until Java 25 is mainstream in the field — still the trigger?), `kafka-mesh-opt-in`
  and `event-api-local-routes-only` (unchanged code, confirm as written), `conv-serialization-gotchas` (msgpack-core is gone since
  ADR-0028 — the Long/Integer downcast rule holds for the in-house codec too; confirm the wording), and the Vision's Track 2 state
  (*design phase*) against [[close-stalled-threads-20261009]]. The review never auto-invalidates; Eric confirms (checks this off) or
  supersedes.
  <!-- id: reverify-invariants-20261009 | created: 2026-10-09 | last_used: 2026-10-09 | uses: 1 | tier: working | origin: 2026-10-09-041128 -->
