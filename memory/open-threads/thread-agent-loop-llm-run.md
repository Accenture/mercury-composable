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
  `extensions/agent-loop`. No open question remains. PR #595 (squash `f950a4bd`) merged rounds 1-5; round 6 is PR #596. RFC-0011 stays Open through implementation (Eric: minor design adjustments may follow). **Reviewed and APPROVED
  2026-11-03 (Eric); the promotion to an ADR is HELD until the end-to-end test from the engines to the language packs for
  AI SDLC is finished** (recorded by #602, squash `cc510abd`, merged 2026-11-03). A stdio MCP server runs as a subprocess of the pack - the reason the MCP
  client moved to the packs - and both packs' Visions allow it (2026-11-03). Next: the build plan, then the end-to-end
  test, then promotion to an ADR (the build plan:
  pack's loop contract and store, `llm.run` in Java, async `graph.extension` and progress events, the Python MCP binding, the
  Rust catch-up). Development runs on the packs' Claude API backend.
  **Step 1's Python half MERGED 2026-11-03** (mercury-python #46 merge `13cf394`, Eric's move of the helper to
  `extensions/llm-helper/`, and #47 merge `8f9ae34`, the loop contract; 66 shared cases in `llm-loop-vectors.json`, green
  on memory, a Redis fake and a live Redis). The Node twin's move, in lockstep (Eric), MERGED as mercury-nodejs
  #114 (merge `62958b5`, 2026-11-03), and its loop contract MERGED as mercury-nodejs #115 (merge `d6a0bd9`,
  2026-11-03): **step 1 is complete on both packs**, the shared 66 cases green on memory, a Redis fake and a live Redis, the six negative controls
  failing exactly as in Python, and a record written by one pack read and appended by the other on one Redis. Small adjustments for the design spec, folded into sections 3.2 and 3.3 by #602 (merged 2026-11-03, docs only): a
  `cap` reply field (`max_tokens` or `context`); no server-side refusal fallback on a loop turn (the pinned, append-only
  record); a loop turn without a configured store is a 503; the loop fields need `tools` on `llm.chat` and are a 400 on
  `llm.stream`; another `params.model` than the pinned one is a 400; an ended or expired conversation is a 404 and a
  closed one a 409; a result that is not text is compact JSON. Next: step 2 (`llm.run` in Java,
  `extensions/agent-loop`).
  The engines' mentions of the packs' `examples/llm-helper` (the Playground's LLM relay, its `event-over-http.yaml` and
  README, the certification reports) are fixed by #601 (squash `72d43d06`) and mercury #401 (merge `e66ae513`), the packs' by mercury-python #48 (`463e37c`), all
  merged 2026-11-03: no live mention of the old folder is left in the family.
  <!-- id: agent-loop-llm-run | created: 2026-10-28 | last_used: 2026-11-03 | uses: 11 | tier: working | origin: 2026-10-28-233011 -->
