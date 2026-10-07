- [ ] **v4.12.21 on the four repositories - PREPARED 2026-10-07, every further step Eric's gate.** `release/4.12.21` pushed in all
  four: mercury-composable (head `5103ff68`; 45 build files, the CHANGELOG cut; the reactor BUILD SUCCESS, 37 modules, 239 suites, 1778 tests, 0 failures, 3 skipped, in 6:16 min (4.12.20 had 1587 tests)), mercury (head `3dce049c`;
  13 manifests and the lock; 134 suites, 702 tests, 0 failed), mercury-nodejs (head `5ddcc9e`; 182 tests) and mercury-python (head `953ba1a`; 187 tests).
  The packs catch up from 4.12.15 with the LLM helper app. **The four PRs are OPEN (Eric, 2026-10-07): mercury-composable #528,
  mercury #368, mercury-nodejs #110, mercury-python #42**, their descriptions set from the drafts with the cross-links. **All four
  MERGED 2026-10-07 (Eric):** #528 squash `b76db298` 02:15:55Z, #368 merge `28457ccf` 02:16:07Z, #110 merge `80d4889` 02:16:19Z,
  #42 merge `acb1f78` 02:16:34Z - each identical to its branch head outside `memory/`, titles clean, branches gone. CI at the one
  read: the packs green, Rust in progress; the Java post-merge run's *Starter Templates (Gradle)* job failed at its Maven install
  step - the Spring Boot parent POM 4.1.1 unresolvable on that runner, a resolution failure, since the PR head's run and the same
  run's reactor job resolved it - to be re-run before the tag. Gates remaining, in order: a green main, tag `v4.12.21` on each, publish
  (the Java artifacts, `cargo publish --workspace` from the tag, `npm publish`, PyPI); then verify the tag commits (the non-memory diff
  from the squash or merge empty, the version at the tag, the GitHub release published, main CI green), rewrite `latest_release` and
  close this thread. Relates [[graph-set-pack-and-deploy]], [[minimalist-msgpack-codec]] (the release's content).
  → serves: vision-mercury-composable
  <!-- id: release-4-12-21 | created: 2026-10-07 | last_used: 2026-10-07 | uses: 1 | tier: working | origin: 2026-10-07-013257 -->
