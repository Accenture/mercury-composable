- [ ] **minimalist-msgpack: RFC-0006 promotion and the release vehicle (2026-10-06, Eric's investigation ask). PR #517 MERGED
  2026-10-06 17:20Z as squash `527ac5eb`.** The zero-dependency codec that
  replaces `msgpack-core` is built and verified on `feature/minimalist-msgpack` (commit `488c826e`, pushed 2026-10-06 on Eric's
  word; **PR #517 open**, opened by Eric by hand since `gh` is bound to an enterprise instance): the full reactor green (1,767 tests), the module's
  134 tests and 90% coverage gate, platform-core's serializer tests unchanged, a 20,000-document two-way differential
  byte-identical with 0 mismatches, and the benchmark at par or faster ([[minimalist-msgpack-codec]];
  `docs/test-reports/minimalist-msgpack-benchmark.md`). **RFC-0006 PROMOTED to ADR-0028 (Eric, 2026-10-06):** the ADR, the register's resolution and a CHANGELOG
  Documentation item are on branch `docs/adr-0028-minimalist-msgpack`, **PR #518** (opened by Eric). Remaining: #518's merge, then
  this thread closes; the release vehicle is the next release.
  [[msgpack-core-cve-upgrade]] closed as moot. The Rust engine needs no change.
  → proposal: RFC-0006
  → serves: vision-mercury-composable
  <!-- id: minimalist-msgpack | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-164045 -->
