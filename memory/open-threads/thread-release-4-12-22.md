- [ ] **Release v4.12.22 — the graph contract on both engines and the mini-scheduler's active environment, lock-step with the Rust port
  (PREPARED 2026-10-09; Eric's gates: PR-open, merge, tag, publish, each on its own).** Java `release/4.12.22` pushed (head `1ceefa20`): the sweep
  `4.12.21` → `4.12.22` in 45 build files / 102 occurrences (the reactor poms, the templates' poms and Gradle builds, the two Snyk
  placeholders), the CHANGELOG cut (`## Version 4.12.22, 10/9/2026`: Added 1–8, Fixed 9 renumbered from 3; upgrade action item 3; a fresh
  `## Unreleased`), derived from `git log v4.12.21..HEAD` (13 commits outside `memory/`; the two Sonar rounds and the RFC register commits
  carry no shipped behaviour of their own). Full reactor `mvn -o clean install` BUILD SUCCESS in 6:50, 37 modules, 245 suites, 1801 tests,
  0 failures, 3 skipped. Rust `release/4.12.22` pushed (head `aa5ecd49`): 13 manifests, `Cargo.lock` refreshed (26 lines), the CHANGELOG
  cut (Increments 170 to 174; items 4 and 5 had been appended under the 4.12.21 heading by the WP3/WP4 commits and moved into the new
  section), fmt and clippy clean, `cargo test --workspace --no-fail-fast` 136 suites, 707 passed, 0 failed, 9 ignored. The language packs
  have no change since v4.12.21 and stay at it ([[conv-ports-adopt-java-release-number]]: lag, not divergence). PR bodies and release notes
  drafted in the scratchpad. **PRs opened by Eric 2026-10-09: mercury-composable #542, mercury #374 (titles as the commits; CI running).** Next: Eric merges, tags `v4.12.22` on each, publishes the crates; then verify the tag
  commits, rewrite `latest_release`, close this thread. Then [[bp-agent-orchestration]] resumes.
  → serves: vision-mercury-composable
  <!-- id: release-4-12-22 | created: 2026-10-09 | last_used: 2026-10-09 | uses: 1 | tier: working | origin: 2026-10-09-222413 -->
