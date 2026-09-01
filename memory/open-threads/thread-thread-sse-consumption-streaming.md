- [x] (feature — **COMPLETE, ALL PHASES — shipped in v4.12.0 2026-08-30**: Java Phase 1
  [PR #300](https://github.com/Accenture/mercury-composable/pull/300) + Phase 2
  [PR #301](https://github.com/Accenture/mercury-composable/pull/301) + Phase 3 in
  PR #302; Rust twins PR #217/#218/#219; ADR-0018/0019 + Rust ADR-0015/0016 Accepted)
  **SSE consumption in AsyncHttpClient + Event-over-HTTP peer streaming** — the engine
  consumes provider token streams (raw mode) and peer functions stream to the engine
  (envelope mode, D5 hybrid control/data-plane framing), one mechanism through
  AsyncHttpClient. Lesson: streaming is a send-with-reply_to EVENT-header opt-in
  (`accept: text/event-stream`); plain RPC never consumes a lane.
  Spec: draft-design-specs/async-http-client-sse-streaming.md §7. origin: 2026-08-29-162504.
  <!-- id: thread-sse-consumption-streaming | created: 2026-08-29 | last_used: 2026-09-01 | uses: 6 | tier: active | origin: 2026-08-29-162504 -->
