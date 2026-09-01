- [ ] (observation — surfaced 2026-07-30 by the second-level-routing code study;
  pre-existing, separate from that feature) **Header-casing mismatch: mixed-case Kafka
  headers are unreachable in Event Script data mapping.** Event Script lowercases
  `input.header.*` references (TaskExecutor — matches the HTTP adapter, which ingests
  headers lowercased), but the Kafka flow adapter delivers record headers in original
  wire casing. A producer-sent `Content-Type` Kafka header can never be addressed by
  any `input.header.*` mapping today. Needs a ruling: lowercase at the Kafka adapter
  (HTTP parity — likely a breaking change for flows matching exact casing via `*`
  passthrough) vs case-insensitive header lookup in the engine. The routing feature
  itself dodges the trap (its header-name lookup is case-insensitive by design).
  Relates [[thread-kafka-2nd-level-routing]].
  <!-- id: thread-kafka-header-casing-mismatch | created: 2026-07-30 | last_used: 2026-07-30 | uses: 1 | tier: working | origin: 2026-07-30-233623 -->
