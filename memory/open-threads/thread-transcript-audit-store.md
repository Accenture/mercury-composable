- [x] **The transcript audit store (RFC-0012 → ADR-0035) - CLOSED 2026-11-17 (Eric, after the live drive).** Outcome: the
  version 2 record and the `transcript.audit` function behind local events in both packs (mercury-python #59/#62,
  mercury-nodejs #126/#129), the graph context headers in both engines (#641, mercury #414), the schema page (#639), the
  drive report (#642) and the ADR (#643, squash `c283313ec`); the durable fact is [[transcript-audit-published-contract]].
  Lesson: a side effect of a turn is a function behind a local event with one worker - the mailbox is the ordering, the
  record the carrier, the check the integrity - proven by stopping the store under it. Full narrative: the logs from
  2026-11-16-105602 (origin) to 2026-11-17-113917.
  → proposal: RFC-0012
  → serves: vision-mercury-composable
  <!-- id: transcript-audit-store | created: 2026-11-16 | last_used: 2026-11-20 | uses: 9 | tier: archive-candidate | origin: 2026-11-16-105602 -->
