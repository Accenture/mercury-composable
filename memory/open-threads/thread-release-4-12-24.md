- [ ] **Release v4.12.24 — PREPARED 2026-10-12: the Guava resolution the field's Snyk gate rejected in 4.12.23, plus the range plugins'
  bounds and refinements (#554–#558) on both engines.** Java `release/4.12.24` (`649f51ab`, PR #559; Rust PR mercury #383): Eric's pom edits - Guava 33.7.2-jre,
  `failureaccess` 1.0.3 and Tink 1.23.0 as DIRECT dependencies of `minimalist-kafka` (excluded from the Confluent artifacts that declare
  them), Confluent 8.3.2, `platform-core` without the Guava it never used, `sync-over-async` without the managed pin
  ([[conv-library-pins-are-direct-dependencies]]) - the sweep 45 / 102, the CHANGELOG cut (item 1 the range plugins, item 2 Build);
  reactor 249 suites / 1830 tests / 0 failures / 3 skipped; the standalone api-playground built. Rust `release/4.12.24` (`0bf12cd0`):
  13 manifests / 24, `cargo update -w`, the CHANGELOG (Increments 177–179, no dependency change); 139 suites / 719 passed / 0 failed /
  9 ignored. Gates ([[eric-release-rhythm]]): Eric opens and merges both PRs, tags `v4.12.24` on each, `cargo publish --workspace`;
  then verify the tag commits, rewrite `latest_release` on both repos, close this thread, and record the field's acceptance when
  reported. The packs stay at 4.12.21 ([[conv-ports-adopt-java-release-number]]).
  → serves: vision-mercury-composable
  <!-- id: release-4-12-24 | created: 2026-10-12 | last_used: 2026-10-12 | uses: 1 | tier: working | origin: 2026-10-12-221919 -->
