- [x] (invariant re-verification — DONE 2026-09-04) All **15** never-decay facts re-confirmed by
  Eric plus the Vision, in a one-by-one walkthrough with live-tree evidence: 3 Architectural
  Invariants, 5 Stack & Tools, 7 core Key Decisions/Conventions. No supersessions; nothing drifted
  since 2026-08-21. The only fact touched by shipped work was `stack-messaging-kafka` — PR #315
  added a config surface to the Kafka family without changing its substance.
  Lesson: probe the mechanism, not the idiom — `startVirtualThread` alone under-reported
  `virtual-threads-rpc`; the real dispatch is `newVirtualThreadPerTaskExecutor` + per-route
  `Thread.ofVirtual()`.
  origin: 2026-09-04-043732.md

  <!-- id: ot-reverify-invariants-20260904 | created: 2026-09-04 | last_used: 2026-09-04 | uses: 1 | tier: working | origin: 2026-09-04-043732 -->
