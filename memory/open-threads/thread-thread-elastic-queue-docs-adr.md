- [ ] (backlog — do at ElasticQueue merge / P4) **Docs sync + ADR for the ElasticQueue file store /
  off-loop dispatch.** Deferred deliberately: nothing in the current guides is wrong today, and the
  config surface is still moving (P4 retires BDB → removes `deferred.commit.log`, the
  `elastic.queue.cleanup` reserved route, and collapses `elastic.queue.store`). At merge/P4:
  configuration-reference (segment size key, final store surface, tmpfs tip), reserved-names
  (drop elastic.queue.cleanup), architecture.md (overflow-buffer line), propose an ADR
  (human-gated), graduate the field notes into the PR/runbook.
  Relates [[thread-elastic-queue-bdb-to-file]], [[elastic-queue-file-fifo-plan]].
  <!-- id: thread-elastic-queue-docs-adr | created: 2026-07-05 | last_used: 2026-07-05 | uses: 1 | tier: working | origin: 2026-07-05-033922 -->
