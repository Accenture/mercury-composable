- [ ] **v4.12.21 on the four repositories - PREPARED 2026-10-07, every further step Eric's gate.** `release/4.12.21` pushed in all
  four: mercury-composable (head `5103ff68`; 45 build files, the CHANGELOG cut; the reactor BUILD SUCCESS, 37 modules, 239 suites, 1778 tests, 0 failures, 3 skipped, in 6:16 min (4.12.20 had 1587 tests)), mercury (head `3dce049c`;
  13 manifests and the lock; 134 suites, 702 tests, 0 failed), mercury-nodejs (head `5ddcc9e`; 182 tests) and mercury-python (head `953ba1a`; 187 tests).
  The packs catch up from 4.12.15 with the LLM helper app. Gates in order: open the four PRs, merge, tag `v4.12.21` on each, publish
  (the Java artifacts, `cargo publish --workspace` from the tag, `npm publish`, PyPI); then verify the tag commits (the non-memory diff
  from the squash or merge empty, the version at the tag, the GitHub release published, main CI green), rewrite `latest_release` and
  close this thread. Relates [[graph-set-pack-and-deploy]], [[minimalist-msgpack-codec]] (the release's content).
  → serves: vision-mercury-composable
  <!-- id: release-4-12-21 | created: 2026-10-07 | last_used: 2026-10-07 | uses: 1 | tier: working | origin: 2026-10-07-013257 -->
