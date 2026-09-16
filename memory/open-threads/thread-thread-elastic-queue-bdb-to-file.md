- [x] (complete — P4 landed 2026-09-16) **Replace ElasticQueue's Berkeley DB spill tier with a
  portable file-backed segmented FIFO.** Outcome: `FileElasticStore` is now the only store —
  `BdbElasticStore`, the `elastic.queue.store` switch, `deferred.commit.log`, the
  `elastic.queue.cleanup` route and the `com.sleepycat:je` dependency are all gone, and
  ServiceQueue's dual dispatch collapsed to one per-route virtual thread. P0–P3 shipped via
  [PR #137](https://github.com/Accenture/mercury-composable/pull/137) (throughput +56%, write
  p99.9 ~47× better, stalls>20ms 90→3); P4 gated on a field canary that reported no ElasticQueue
  issues (Eric, 2026-09-16), shipped for v4.12.10. **Durable lesson:** the microbenchmark favoured
  the store we removed — BDB was competitive-to-faster on a *single isolated route*. Only the
  mixed-workload probe (a latency-sensitive route measured while another route's spill runs)
  exposed the property that actually mattered: keeping spill off the shared event loop. Benchmark
  the interference, not just the component. Analysis retained at
  `benchmark/benchmark-reporter/analysis/`; formalized as ADR-0024 (proposed);
  see [[elastic-queue-file-store]]. origin: 2026-07-05-033922.
  <!-- id: thread-elastic-queue-bdb-to-file | created: 2026-07-05 | last_used: 2026-09-16 | uses: 14 | tier: active | origin: 2026-07-05-033922 -->
