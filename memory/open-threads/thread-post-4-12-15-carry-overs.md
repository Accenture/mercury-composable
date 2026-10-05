- [x] **Carry-overs after v4.12.15 — CLOSED 2026-10-05 at the stalled-thread gate (Eric: close).** Parked from the 2026-09-22 backlog survey behind the
  AI SDLC/MCP backlog and untouched for 42 sessions. Delivered elsewhere: the real-provider token stream (the 2026-10-01 LLM helper certification, real
  Claude tokens through both engines). Deliberately dropped: the Rust `async.http.response` record's `from`, an optional CLIENT span kind for
  `async.http.request`, the `minigraph-state-redis` lifecycle retry (Rust), the parked engine items (CompileGraph static output-mapping LHS check,
  Rust async-callback parity, a typed cache helper, a sync SSE client), the live Splunk header run, the Rust OTLP gzip. Lesson: a parking lot
  with no trigger stalls; an item returns as its own thread when a field ask or a parity check names it. origin: 2026-09-23-014650; close 2026-10-05-230716.
  <!-- id: post-4-12-15-carry-overs | created: 2026-09-23 | last_used: 2026-09-23 | uses: 1 | tier: working | origin: 2026-09-23-014650 -->
