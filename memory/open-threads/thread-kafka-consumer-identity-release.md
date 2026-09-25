- [x] **The field's consumer-side Schema Registry identity — SHIPPED in v4.12.17 on both engines (2026-09-25; Java #461
  squash `e9cde291` → tag `8a13a02e`; Rust #328 merge `ad957930` → tag `af9d6f30`; GitHub releases = the CHANGELOG entries).**
  Content: #458 (primary opt-in), #459 (sample), #460 (twin-kafka on the shared `SchemaCodec.forConsumer`), mercury #327
  (Increment 139); readiness Java 1514 / 0 / 3, Rust 603 / 0 / 9. Lessons: derive the CHANGELOG from the tag range — the Rust
  entry gained two unreleased increments that way; a lint-clean gate before every memory push (two `[stale-metadata]` slips
  taught it); two timing-sensitive Rust tests flaked on CI and are a recorded test-only follow-up. origin: 2026-09-24-222349.md
  … 2026-09-24-234713.md
  <!-- id: kafka-consumer-identity-release | created: 2026-09-24 | last_used: 2026-09-24 | uses: 3 | tier: archive-candidate | origin: 2026-09-24-222349 -->
