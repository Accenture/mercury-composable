- [x] **Hot deploy and undeploy of graph sets (RFC-0017 → ADR-0038; Eric's sprint of 2026-11-24, done 2026-11-25).**
  Outcome: a certified set uploaded to a running engine, registered through the gate on every instance on demand through
  its own record, removed by name - built in both engines, driven live on one hot folder, promoted; the fact is
  `hot-deploy-set-record-on-demand`. Refs: #671, #672 (the design), #673, #675, #676, #677 (Java), mercury #424 (Rust),
  #680 and mercury #425 (the drive and its corrections), #681 (the ADR, squash `c2a04880b`). Lesson: a design's promised
  status code is a platform claim to verify live - the drive found 404 where the draft said 503 - and the digest header,
  not the strict read, is the check on altered bytes. origin: 2026-11-24-202705.md (the narrative continues in the logs
  of 2026-11-24-235243, 2026-11-25-014915, 2026-11-25-050603 and 2026-11-25-052834).
  → proposal: RFC-0017
  → serves: vision-mercury-composable
  <!-- id: hot-deploy-graph-sets | created: 2026-11-24 | last_used: 2026-11-25 | uses: 4 | tier: active | origin: 2026-11-24-202705 -->
