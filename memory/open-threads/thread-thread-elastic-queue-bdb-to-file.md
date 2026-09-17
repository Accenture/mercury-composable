- [x] (complete — P4 landed 2026-09-16 in v4.12.10) **Replace ElasticQueue's Berkeley DB spill tier
  with a portable file-backed segmented FIFO.** Outcome: `FileElasticStore` is the only store;
  `BdbElasticStore`, `elastic.queue.store`, `deferred.commit.log`, `elastic.queue.cleanup` and
  `com.sleepycat:je` all gone, ServiceQueue collapsed to one per-route virtual thread. PRs #137
  (P0–P3) and #399 (P4, squash `9aa40089`); formalized as ADR-0024 (accepted). Lesson: the
  microbenchmark favoured the store we removed — only the mixed-workload probe exposed the property
  that mattered. **Benchmark the interference, not just the component.** See
  [[elastic-queue-file-store]]. origin: 2026-07-05-033922.
  <!-- id: thread-elastic-queue-bdb-to-file | created: 2026-07-05 | last_used: 2026-09-16 | uses: 15 | tier: archive-candidate | origin: 2026-07-05-033922 -->
