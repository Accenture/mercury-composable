- [x] **A failing element of a fan-out was masked by a later success - DONE 2026-11-27 on both engines (a field report
  of 2026-11-26).** Each response of a batch wrote `{node}.status` while `{node}.error` stayed, and the platform's
  fork-join returns a batch's replies in no particular order and leaves out one that timed out. Fixed here as PR #682
  (squash `1adb6fc5a`): the first failing element in list order decides, a missing answer is a 408, the result follows
  the source list, no batch starts after a failure ([[fan-out-first-failure-decides]]). The Rust twin is mercury #426
  (merge `f8ac53f7`), which also rectified two differences from this engine: a `target` key given twice in an error
  body, and the timeout text. Lesson: reproduce before theorizing - a harness that tallied outcomes showed that the
  order the elements ended in played no part, and found four neighbours of the reported fault. origin: 2026-11-26-221000.
  → serves: vision-mercury-composable
  <!-- id: for-each-failure-masked | created: 2026-11-26 | last_used: 2026-11-26 | uses: 1 | tier: working | origin: 2026-11-26-221000 -->
