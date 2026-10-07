- [ ] **v4.12.21 on the four repositories - PREPARED 2026-10-07, every further step Eric's gate.** `release/4.12.21` pushed in all
  four: mercury-composable (head `5103ff68`; 45 build files, the CHANGELOG cut; the reactor BUILD SUCCESS, 37 modules, 239 suites, 1778 tests, 0 failures, 3 skipped, in 6:16 min (4.12.20 had 1587 tests)), mercury (head `3dce049c`;
  13 manifests and the lock; 134 suites, 702 tests, 0 failed), mercury-nodejs (head `5ddcc9e`; 182 tests) and mercury-python (head `953ba1a`; 187 tests).
  The packs catch up from 4.12.15 with the LLM helper app. **The four PRs are OPEN (Eric, 2026-10-07): mercury-composable #528,
  mercury #368, mercury-nodejs #110, mercury-python #42**, their descriptions set from the drafts with the cross-links. **All four
  MERGED 2026-10-07 (Eric):** #528 squash `b76db298` 02:15:55Z, #368 merge `28457ccf` 02:16:07Z, #110 merge `80d4889` 02:16:19Z,
  #42 merge `acb1f78` 02:16:34Z - each identical to its branch head outside `memory/`, titles clean, branches gone. CI at the one
  read: the packs green, Rust in progress; the Java post-merge run's *Starter Templates (Gradle)* job failed at its Maven install
  step - the Spring Boot parent POM 4.1.1 unresolvable on that runner, a resolution failure, since the PR head's run and the same
  run's reactor job resolved it; the failed job was re-run at 02:26Z and passed at 02:28Z, so the main run on `b76db298` is green on
  both jobs (a one-off Central refusal on that runner). **TAGGED 2026-10-07 (Eric), all four verified:** Java tag → `87dd9e78` (two
  memory-only commits past the squash, non-memory diff empty, pom 4.12.21, release published 02:35:00Z, CI green on the tag commit),
  Rust → `9f524fa3` (release 02:36:06Z), nodejs → `279e784` (02:36:40Z), python → `fe43380` (02:37:17Z) - each one memory commit past
  its merge, the version at the tag, the release published, CI green; `latest_release` rewritten in both engine repos, the packs'
  status lines moved to TAGGED. **Remaining: the publications (Eric preparing) - the twelve crates on crates.io, `npm publish`, PyPI -
  and their verification (the registry's version, created time and, for the crates, the published tarball against the tag); then
  close this thread. Relates [[graph-set-pack-and-deploy]], [[minimalist-msgpack-codec]] (the release's content).
  → serves: vision-mercury-composable
  <!-- id: release-4-12-21 | created: 2026-10-07 | last_used: 2026-10-07 | uses: 1 | tier: working | origin: 2026-10-07-013257 -->
