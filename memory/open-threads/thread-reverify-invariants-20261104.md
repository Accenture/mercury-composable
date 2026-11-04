- [ ] **Re-verify invariants (due):** confirm stack-language-java21, stack-build-maven, stack-integration-spring-boot4,
  stack-messaging-kafka, stack-ci-gha, functions-decoupled-routes, typed-io-map-or-pojo, virtual-threads-rpc,
  trace-thread-keyed-mono-gotcha, kafka-mesh-opt-in, event-script-over-code, event-api-local-routes-only,
  conv-add-capability, conv-serialization-gotchas, conv-declare-consulted-references, conv-proposals-not-in-adr-ledger,
  conv-compartment-downstream-secrets, eric-release-rhythm, eric-code-changes-via-pr and the Vision
  (vision-mercury-composable, the 2026-09-17 two-track text, Type line refined 2026-11-03) still hold, or supersede any
  that don't (DECAY.md §9). Raised by the 2026-11-04 review: 47 sessions since 2026-10-22-234147, cadence 40.
  - **Evidence from this window, for the walk:**
    - `functions-decoupled-routes`: RFC-0011 steps 2 and 3 couple only by route names. Tools map to routes, a
      caller's return path is a route name, and each node's relay is a private route.
    - `trace-thread-keyed-mono-gotcha`: met again in #609. An async fan-out starts later runs on a thread with no
      trace context, so each run carries the node's span explicitly.
    - `stack-language-java21`: the toolchain is still 21.
    - The Vision's Track 2: the agent loop and its watched and unattended modes are built in Java. The end-to-end
      test with the packs and the ADR promotion are still to come ([[agent-loop-llm-run]]).
  <!-- id: reverify-invariants-20261104 | created: 2026-11-04 | last_used: 2026-11-04 | uses: 1 | tier: working | origin: 2026-11-04-050444 -->
