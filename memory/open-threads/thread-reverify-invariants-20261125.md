- [ ] **Re-verify invariants (due 2026-11-25; 49 sessions since the 2026-11-13 walk, cadence 40):** confirm the 20 `core`
  facts - stack-language-java21, stack-build-maven, stack-integration-spring-boot4, stack-messaging-kafka, stack-ci-gha, functions-decoupled-routes, typed-io-map-or-pojo, virtual-threads-rpc, clean-knowledge-design-over-engine-coverage, trace-thread-keyed-mono-gotcha, kafka-mesh-opt-in, event-script-over-code, event-api-local-routes-only, conv-add-capability, conv-serialization-gotchas, conv-declare-consulted-references, conv-proposals-not-in-adr-ledger, conv-compartment-downstream-secrets, eric-release-rhythm, eric-code-changes-via-pr - and the
  Vision (`memory/vision.md`, the 2026-09-17 two-track text with its Track 2 status row refreshed through 2026-11-25) still
  hold against the live tree, or supersede any that does not (DECAY.md §9). Since the last walk: Java 21 stays the baseline
  with the Java 25 lift planned ([[java25-baseline]]); `conv-reentrantlock-not-synchronized` re-affirmed and applied;
  hot deployment of graph sets decided (ADR-0038) with its on-demand convergence resting on the record, not the mesh
  (`kafka-mesh-opt-in` respected) and `server.url` resting on `event-api-local-routes-only`. The review raises this gate;
  the walk is Eric's.
  → serves: vision-mercury-composable
  <!-- id: reverify-invariants-20261125 | created: 2026-11-25 | last_used: 2026-11-25 | uses: 1 | tier: working | origin: 2026-11-25-053648 -->
