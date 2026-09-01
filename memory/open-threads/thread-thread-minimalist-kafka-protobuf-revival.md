- [ ] (planned — backlog, no ETA) **Reintroduce Protobuf support in minimalist-kafka's Schema
  Registry integration.** Blocked on Confluent adopting the renamed
  `com.squareup.wire:wire-runtime` coordinate in `kafka-protobuf-provider` (unchanged as of 8.3.0,
  checked 2026-07-01 — re-check on any future Confluent release). Alternative unblocks: (a) vendor
  a patched `wire-runtime-jvm` fork (the upstream fix is a one-liner); (b) a field installation
  explicitly needs Protobuf and accepts the residual CVE-2026-45799 risk. What to restore:
  [[minimalist-kafka-protobuf-removed]] (all still in git history).
  <!-- id: thread-minimalist-kafka-protobuf-revival | created: 2026-07-01 | last_used: 2026-08-03 | uses: 2 | tier: working | origin: 2026-07-01-224313 -->
