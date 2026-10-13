- [ ] **Release v4.12.25 — PREPARED 2026-10-13 (Java only): the Sonar findings of the field's gate on 4.12.24 closed (#560 squash
  `a4d94916`, #561 squash `d776685d`); the field's pipeline PASSED on that main head (CI, Snyk, Sonar) before the cut.** `release/4.12.25`
  (`9a1e18d6`, PR #562): sweep 45 / 102, the CHANGELOG cut (item 1, both rounds); reactor 249 suites / 1830 tests / 0 failures / 3 skipped; the
  standalone api-playground jar built. **Rust stays at 4.12.24 (Eric, 2026-10-13): no change in the port, it catches up with its next
  feature or fix** ([[conv-ports-adopt-java-release-number]] - lag awaiting catch-up, not divergence; the packs stay at 4.12.21). Gates
  ([[eric-release-rhythm]]): Eric opens and merges the PR, tags `v4.12.25`; then verify the tag commit and the release, rewrite
  `latest_release`, close this thread; the field's deployment of 4.12.25 is the acceptance to record.
  → serves: vision-mercury-composable
  <!-- id: release-4-12-25 | created: 2026-10-13 | last_used: 2026-10-13 | uses: 1 | tier: working | origin: 2026-10-13-020033 -->
