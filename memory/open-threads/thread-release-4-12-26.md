- [ ] **Release v4.12.26 — PREPARED 2026-10-26 on both engines: JSONPath to RFC 9535 (`minimalist-jsonpath`), live invocation
  observers for an APM agent, the date plugins, `reactive-postgres` batches and transactions, the Playground round and three
  dependency-advisory fixes; Rust catches up from 4.12.24.** Java `release/4.12.26`: the CHANGELOG cut with Changed 5 added (#567, the
  graph-set worked example, which the tag range caught missing), 50 build files / 114 occurrences plus the lab's three literals, BUILD
  SUCCESS 6:02, 263 suites / 2793 tests / 0 failures / 3 skipped; the standalone api-playground and the lab at 4.12.26. Rust
  `release/4.12.26`: 13 manifests / 24, 142 suites / 732 passed. Next (Eric's gates): open and merge the two PRs, tag `v4.12.26` on
  each, publish the releases and the crates; then verify the tag commits, rewrite `latest_release` on both repos, close this thread,
  and record the field's acceptance when reported. The packs stay at 4.12.21 (their catch-up is held).
  origin: 2026-10-26-165029
  → serves: vision-mercury-composable
  <!-- id: release-4-12-26 | created: 2026-10-26 | last_used: 2026-10-26 | uses: 1 | tier: working | origin: 2026-10-26-165029 -->
