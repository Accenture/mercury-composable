- [x] (complete — 2026-09-16, with P4) **Docs sync + ADR for the ElasticQueue file store / off-loop
  dispatch.** Outcome: all four surfaces the thread named are done — configuration-reference gained
  `elastic.queue.segment.size.bytes` and `elastic.queue.dispatch.mailbox.size` (replacing the
  retired `deferred.commit.log`) plus the tmpfs tip; reserved-names dropped `elastic.queue.cleanup`;
  architecture.md's overflow-buffer line now names the segmented file FIFO and why it permits
  off-loop dispatch; and **ADR-0024** is proposed for human approval. Deferring this until P4 was
  the right call — the config surface really did change underneath it, so writing it earlier would
  have documented three keys that no longer exist. Relates [[thread-elastic-queue-bdb-to-file]],
  [[elastic-queue-file-store]]. origin: 2026-07-05-033922.
  <!-- id: thread-elastic-queue-docs-adr | created: 2026-07-05 | last_used: 2026-09-16 | uses: 2 | tier: active | origin: 2026-07-05-033922 -->
