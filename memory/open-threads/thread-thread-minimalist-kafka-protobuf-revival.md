- [x] (backlog → **CLOSED 2026-09-16, Eric — dormant, demand-triggered**) **Reintroduce Protobuf
  support in minimalist-kafka's Schema Registry integration.** No longer tracked as planned work:
  it is reactivated only if another field installation actually requests Protobuf. That is exactly
  unblock (b) the thread already listed, now made the sole condition.
  **Preserved so it is not re-derived:** the blocker is Confluent's `kafka-protobuf-provider` still
  depending on the pre-rename `wire-runtime` coordinate rather than
  `com.squareup.wire:wire-runtime` (unchanged as of 8.3.0, checked 2026-07-01) — re-check on any
  future Confluent release. The alternative unblock is vendoring a patched `wire-runtime-jvm` fork
  (the upstream fix is a one-liner). Reintroducing it means accepting the residual CVE-2026-45799
  risk, which is why a field request is the trigger. What to restore is catalogued in
  [[minimalist-kafka-protobuf-removed]]; all of it is in git history.
  origin: `memory/sessions/2026-07-01-224313.md`
  <!-- id: thread-minimalist-kafka-protobuf-revival | created: 2026-07-01 | last_used: 2026-08-03 | uses: 2 | tier: archive-candidate | origin: 2026-07-01-224313 -->
