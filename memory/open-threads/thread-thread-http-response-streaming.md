- [x] (feature — **COMPLETE BOTH ENGINES 2026-08-28/29**: Java
  [PR #299](https://github.com/Accenture/mercury-composable/pull/299) squash `2eea5038`;
  Rust [PR #216](https://github.com/Accenture/mercury/pull/216); ADR-0018/Rust ADR-0015)
  **HTTP response streaming — token/event streaming to the HTTP edge** (`stream: true`,
  500-lane checkout pool + HTTP-503 back-pressure, SSE/chunked+NDJSON standards-only
  wire, EventStreamWriter both engines; lane checkout became FIFO rotation 2026-09-01,
  [[ot-agent-orchestration-e0]]). Lesson: a vert.x drainHandler is a second concurrent
  flusher — lock the pending queue. origin: 2026-08-28-025445 (+ mercury 2026-08-29-012914).
  <!-- id: thread-http-response-streaming | created: 2026-08-28 | last_used: 2026-09-01 | uses: 5 | tier: active | origin: 2026-08-28-025445 -->
