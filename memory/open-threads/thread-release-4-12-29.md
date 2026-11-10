- [x] **Release v4.12.29 — SHIPPED 2026-11-05, Java only: the scheduler's conditional claim with `_origin` (#612), its
  guide (#613) and the example on MongoDB (#615); the agent loop and RFC-0011 step 3 (#605, #606, #609, #611); #610;
  #614.** Release PR #616 squash `39c90da8`; tag `v4.12.29` → `3cf79ae8` (the squash plus one memory commit), published
  06:53:21Z; 276 suites / 3002 tests. Rust stays at 4.12.27: nothing to catch up, no number without content (Eric).
  Lesson: a memory commit pushed after the squash moves main's head, so a tag made from the release form lands on it -
  harmless when only `memory/` differs, and checked by diffing the tag against the squash outside `memory/`.
  origin: 2026-11-05-061842; close 2026-11-05-065411.
  → serves: vision-mercury-composable
  <!-- id: release-4-12-29 | created: 2026-11-05 | last_used: 2026-11-05 | uses: 2 | tier: archive-candidate | origin: 2026-11-05-061842 -->
