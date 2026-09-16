- [x] (complete — 2026-09-16, with P4) **Docs sync + ADR for the ElasticQueue file store / off-loop
  dispatch.** Outcome: configuration-reference gained `elastic.queue.segment.size.bytes` and
  `elastic.queue.dispatch.mailbox.size` (replacing `deferred.commit.log`) plus the tmpfs tip;
  reserved-names dropped `elastic.queue.cleanup`; architecture.md names the segmented file FIFO;
  ADR-0024 proposed and accepted. PR #399. Lesson: deferring docs until the config surface settled
  was right — three keys they would have documented in July no longer exist.
  origin: 2026-07-05-033922.
  <!-- id: thread-elastic-queue-docs-adr | created: 2026-07-05 | last_used: 2026-09-16 | uses: 3 | tier: archive-candidate | origin: 2026-07-05-033922 -->
