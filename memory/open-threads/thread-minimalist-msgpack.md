- [x] **minimalist-msgpack: the in-house MessagePack codec replaces msgpack-core - CLOSED 2026-10-06 (Eric: both PRs merged).**
  PR #517 (squash `527ac5eb`) shipped the module, the swap, 134 module tests under a 90% gate, the two-way differential and the
  benchmark report; PR #518 (squash `ed1f8136`) promoted RFC-0006 to ADR-0028. Lesson: a serialization dependency at the
  foundation is replaceable in a day when the engine uses a small, well-specified part of it - and the proof is the bytes
  (shared vectors plus a seeded two-way differential), not the tests alone. Rule: [[minimalist-msgpack-codec]] (ADR-0028).
  origin: 2026-10-06-164045; close 2026-10-06-164045.
  → proposal: RFC-0006
  → serves: vision-mercury-composable
  <!-- id: minimalist-msgpack | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-164045 -->
