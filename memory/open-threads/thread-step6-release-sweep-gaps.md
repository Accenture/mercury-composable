- [ ] (review) **Next review: two release conventions look faded but were relied on - apply step 6 to the v4.12.20 session before
  archiving them (2026-10-04).** `conv-template-version-sweep` and `snyk-retired-manifest-placeholders` read sslu 21 > `archive_window`
  20, yet the v4.12.20 sweep (#485, squash `9e515825`) bumped both rules' files - the templates' builds and the two Boot 3 placeholder
  poms - and its log `2026-09-30-221603.md` declared neither. Step 6 (v4.42.2) reaches the first through `templates/*`; it misses the
  second, whose paths are written without backticks and, as a retired module's, exceed the all-time 5% hub cut (15%) though only #485
  touches them in the window. At the review: hold `2026-09-30-221603.md` to the decision test for both, keep and re-affirm what passes,
  and backtick the placeholder fact's two paths. Upstream: a second feedback file to agent-memory proposes a hub be busy both over all
  history and in the window, a full timestamp for the window's start, and the next log-carrying commit for the mapping.
  <!-- id: step6-release-sweep-gaps | created: 2026-10-04 | last_used: 2026-10-04 | uses: 1 | tier: working | origin: 2026-10-04-173516 -->
