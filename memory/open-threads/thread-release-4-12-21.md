- [x] **v4.12.21 on the four repositories — CLOSED 2026-10-07, released and verified.** Outcome: prepared on `release/4.12.21` in all
  four, PRs opened by Eric and merged (composable #528 squash `b76db298`, mercury #368 `28457ccf`, nodejs #110 `80d4889`, python #42
  `acb1f78`), tagged (→ `87dd9e78`, `9f524fa3`, `279e784`, `fe43380`; each memory-only past its merge, the version at the tag, the
  GitHub releases published 02:35–02:37Z, main CI green) and published (crates.io 12/12 at 02:40Z, npm 02:43Z, PyPI 02:46Z), every
  artifact checked against its tag. Lesson: zsh does not word-split an unquoted `for f in $FILES` — a sweep that "changed nothing"
  is the symptom, a `while read` loop the fix; and a one-off Maven Central refusal on a runner is a re-run, not a tree change.
  Origin: 2026-10-07-013257.md (the whole narrative).
  → serves: vision-mercury-composable
  <!-- id: release-4-12-21 | created: 2026-10-07 | last_used: 2026-10-07 | uses: 1 | tier: working | origin: 2026-10-07-013257 -->
