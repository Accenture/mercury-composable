- [ ] **The agent loop: `llm.run` in the engines, model turns and MCP bindings in the packs** (serves [[bp-agent-orchestration]];
  → proposal: RFC-0011). The foundation of Track 2, finalized before the AI lab sprint resumes. Design spec
  `draft-design-specs/llm-helper-agent-loop.md`, carrying Eric's six review rounds of 2026-10-28 and 2026-10-29: narrow loops composed by the
  graph; a tool name maps to a route; the catalog is a dataset node under the island; the transcript is opaque, append-only, in the
  pack's access-controlled store; approval comes in two kinds (the node's mode for a marked write, a business decision for the
  graph); `max_turns` is the budget (default 10); agent graphs are asynchronous (`graph.extension async=true`, with `for_each`
  fan-out, + HTTP 202 + notification, or a watched session with progress events to the caller's return route -
  `async.http.response`, the progressive-rendering lane, or an event's `x-return-path` - a generic function route, local,
  Event-over-HTTP or a Kafka-publishing wrapper, honoured only when declared in `return.path.routes`, which takes a prefix wildcard such as `order.status.*`); transcripts share the suspend store's
  Redis under their own prefix; MCP clients live in the packs, never as an engine's child process; no nesting; `llm.run` in
  `extensions/agent-loop`. No open question remains. PR #595 (squash `f950a4bd`) merged rounds 1-5; round 6 is PR #596. RFC-0011 stays Open through implementation (Eric: minor design adjustments may follow). Next: the build plan, then acceptance and
  promotion to an ADR (the build plan:
  pack's loop contract and store, `llm.run` in Java, async `graph.extension` and progress events, the Python MCP binding, the
  Rust catch-up). Development runs on the packs' Claude API backend.
  <!-- id: agent-loop-llm-run | created: 2026-10-28 | last_used: 2026-10-28 | uses: 1 | tier: working | origin: 2026-10-28-233011 -->
