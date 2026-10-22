- [x] **Doc-improvement feedback loop** (initiative, 2026-09-01; closed by Eric 2026-10-22 at the stalled-thread gate
  [[close-stalled-threads-20261022]]).
  - **Outcome:** all three layers ran. The capture layer is the guide-first rule with `## Doc Gaps` in `memory/instructions.md`, in all
    four repos (#306, mercury #222, mercury-python #23, mercury-nodejs #91). The review sweeps from 2026-09-21 to 2026-10-09 closed every
    recorded gap. The W2 fresh-agent probe raised guide sufficiency from 11/15 to 15/15 (#434 `940994d5`). External feedback drove
    ADR-0021, ADR-0022 and an 8-guide campaign (#310, mercury #224), and the graph.math dialect gap closed with a claim test (#467).
  - **Dropped deliberately:** the re-measure with a fresh question set, never run. The capture rule stays, so a gap is still recorded
    and closed in the session that finds it.
  - **Lesson:** a queue that is only appended to rots; audit it at each sweep (8 of 9 merged-backlog items had shipped unrecorded).
  - origin: 2026-09-01-032130 (the narrative is in the logs it names; closed in 2026-10-22-234147)
  <!-- id: thread-doc-improvement-feedback-loop | created: 2026-09-01 | last_used: 2026-10-09 | uses: 9 | tier: working | origin: 2026-09-01-032130 -->
