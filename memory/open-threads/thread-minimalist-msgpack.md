- [ ] **minimalist-msgpack: review, PR and RFC-0006 (2026-10-06, Eric's investigation ask).** The zero-dependency codec that
  replaces `msgpack-core` is built and verified on `feature/minimalist-msgpack` (commit `488c826e`, pushed 2026-10-06 on Eric's
  word; **PR #517 open**, opened by Eric by hand since `gh` is bound to an enterprise instance): the full reactor green (1,767 tests), the module's
  134 tests and 90% coverage gate, platform-core's serializer tests unchanged, a 20,000-document two-way differential
  byte-identical with 0 mismatches, and the benchmark at par or faster ([[minimalist-msgpack-codec]];
  `docs/test-reports/minimalist-msgpack-benchmark.md`). **Eric decides:** the PR's review and merge (CI the remaining gate), the
  release vehicle (next patch, or after 4.12.20's field acceptance), RFC-0006 promotion after review and green CI; then
  [[msgpack-core-cve-upgrade]] closes as moot. The Rust engine needs no change.
  → proposal: RFC-0006
  → serves: vision-mercury-composable
  <!-- id: minimalist-msgpack | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-164045 -->
