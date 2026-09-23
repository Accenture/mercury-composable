- [ ] **Carry-overs after v4.12.15 (from the 2026-09-22 backlog survey; Eric: the AI SDLC/MCP backlog resumes first).**
  Small items parked with no release pressure — pick one up when a release has room or a field ask names it: the Rust
  `async.http.response` graph-run record has no `from` (Java `from=task.executor`); an optional CLIENT span kind for
  `async.http.request`; `minigraph-state-redis` still drives its own Redis manager without the foundation's lifecycle retry
  (Rust); the parked engine items — CompileGraph static output-mapping LHS check, the Rust async-callback parity check, a
  typed cache helper, a sync SSE client form; the Splunk header form never run live; the Rust gzip delta
  (`otel.exporter.otlp.compression` honours only `none`); a real-provider token stream when Gemini quota returns. Relates
  [[bp-agent-orchestration]] (the resumed epic takes precedence) and [[connected-edge-spans]].
  <!-- id: post-4-12-15-carry-overs | created: 2026-09-23 | last_used: 2026-09-23 | uses: 1 | tier: working | origin: 2026-09-23-014650 -->
