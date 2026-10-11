- [ ] **Release v4.12.23 — the API playground on both engines, lock-step with the Rust port (PREPARED 2026-10-11; Eric's gates: PR-open,
  merge, tag, publish, each on its own).** Java `release/4.12.23` pushed (head `3c054b00`): the sweep `4.12.22` → `4.12.23` in 45 build
  files / 102 occurrences (the same shape as 4.12.22), the CHANGELOG cut (`## Version 4.12.23, 10/11/2026`: Added 1–3, Fixed 4; the
  4.12.22-only jar-listing fix folded into item 3; the front-matter fix and guard, #543/#544, added as item 4 - they were missing from
  Unreleased; a fresh `## Unreleased`) derived from `git log v4.12.22..HEAD` (8 commits outside `memory/`). Full reactor
  `mvn -o clean install` BUILD SUCCESS in 7:05, 37 modules, 245 suites, 1802 tests, 0 failures, 3 skipped (4.12.22 had 1801); the
  standalone `extensions/api-playground` builds at 4.12.23 with the fetched assets. Rust `release/4.12.23` pushed (head `ac8e185e`): 13
  manifests (the new crate takes the workspace version), `Cargo.lock` refreshed (54 lines), the CHANGELOG cut (Increment 175 + the
  front-matter fix as item 2), fmt and clippy clean, `cargo test --workspace --no-fail-fast` 137 suites, 712 passed, 0 failed, 9
  ignored. The language packs have no change since v4.12.21 and stay at it. Release notes drafted in the scratchpad
  (`release-notes-java-4.12.23.md`, `release-notes-rust-4.12.23.md`). **PRs opened by Eric 2026-10-11: mercury-composable #551, mercury #378 (titles as the commits; CI running).** Next: Eric
  merges, tags `v4.12.23` on each, publishes the twelve crates; then verify the tag commits, rewrite `latest_release`, close this thread. Then [[bp-agent-orchestration]]
  resumes.
  → serves: vision-mercury-composable
  <!-- id: release-4-12-23 | created: 2026-10-11 | last_used: 2026-10-11 | uses: 1 | tier: working | origin: 2026-10-11-011444 -->
