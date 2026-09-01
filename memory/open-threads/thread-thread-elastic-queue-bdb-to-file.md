- [ ] (P0–P5 code-complete — 2026-07-05, branch `feature/elastic-queue-file-fifo`, submitted as
  [PR #137](https://github.com/Accenture/mercury-composable/pull/137); remaining = field canary →
  P4 retire-BDB) **Replace ElasticQueue's Berkeley DB spill tier with a portable file-backed
  segmented FIFO.** State: `ElasticQueue` = facade over an `ElasticStore` strategy; **ONE switch**
  `elastic.queue.store` (`file` ⇒ per-route virtual-thread dispatch, `bdb` ⇒ event loop — derived,
  vthread+bdb unreachable since BDB pins VT carriers); **default flipped to `file`** (Eric chose:
  fold into #137 + re-frame). Results: throughput +56%, write p99.9 ~47× better, stalls>20ms
  90→3; the one `file` blemish is a rare OS dirty-page-flush outlier (document tmpfs). The
  `elastic.queue.cleanup` BDB work is `@KernelThreadRunner`-isolated (closes a live in-field
  VT-pinning vector). Copilot review hardening applied (bounded dispatch mailbox with no-drop
  back-pressure, O(1) segment channels, stale-dir cleanup). `benchmark/benchmark-reporter` module
  added — self-contained field A/B harness → HTML report. **Next = field steps: run
  benchmark-reporter on real envs, then P4 retire BDB.** Design spec + field notes:
  draft-design-specs/ (committed and public since the 2026-08 specs-go-public round; the
  earlier "gitignored" status here predated it). Docs/ADR sync:
  [[thread-elastic-queue-docs-adr]]. Full detail: origin log.
  <!-- id: thread-elastic-queue-bdb-to-file | created: 2026-07-05 | last_used: 2026-09-01 | uses: 13 | tier: working | origin: 2026-07-05-033922 -->
