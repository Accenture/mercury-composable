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
  closed one a 409; a result that is not text is compact JSON. **Step 2 built 2026-11-04: PR #605** (`llm.run` in Java,
  `extensions/agent-loop`; branch `feat/agent-loop-llm-run`, commits `e98ed202`, `d855501d` (the IDE's findings, no
  behaviour change), `4f0178ab` (the four modes) and `dd87cd2d`; **MERGED 2026-11-04 02:50Z, squash `b4e0f35d`**, the tree
  identical to the branch head; step 2 COMPLETE): the loop, the pre-flight
  check, the modes with approval and resume, the settings (`llm.run.*`), the envelope; `llm-run-vectors.json` (81 loop
  cases, 29 schema cases) for the Rust catch-up and a platform test. **Round 7 (Eric, 2026-11-04):** the five defaults
  are accepted (a `timeout` input, since `graph.task` passes no time to live; the module's own JSON Schema subset;
  unknown inputs and row fields refused; `max_turns` over the conversation; `params`), and **the mode governs the use of
  tools, four modes**: `write` (the default) holds a row marked `write: true` (renamed from `approval: required`) for a
  person, `auto` runs every call, `manual` holds every call, `unattended` refuses a marked write - a write runs unasked
  only under a declared `mode=auto` (supersedes ruling 21 and ruling 14's default). **Built after #605 merged: PR #606 (MERGED 2026-11-04,
  squash `d9272c6e`; spec section 7.3, ruling 38; 98 loop cases; its three build details accepted by Eric): an "ask a
  person" catalog row** (Eric, 2026-11-04), for an LLM that recommends options for a person to select and must carry
  on in the same conversation (choosing among designed branches already works through section 7.2: the options an
  `enum` in the output schema, `graph.suspend`, the person's 1-based choice routed by a decision node). The row has
  `ask: true` and no route; the model calls it with a question and its options; the loop holds it like any waiting call
  in every mode but `unattended`, where it goes back as "nobody to ask"; the resume's decision for it carries `answer`,
  returned to the model as the call's result - the transcript stays append-only and the packs do not change. **Ruling 39 (Eric,
  2026-11-04): no compaction in `llm.run`;** if a loop needs it, the provider's server-side compaction in the packs'
  `llm.chat` behind a pack setting, deferred until a realistic loop reaches `cap: context` (docs PR #608, MERGED 2026-11-04, squash `abe43450`). **Step 3a built
  2026-11-04: PR #609, MERGED 2026-11-04 04:50Z, squash `5e528e9c`** (branch `feat/graph-extension-async`; spec section 8.1 *As built*): `async=true`
  on `graph.extension` starts the target and continues; the node's `ttl` or the target's own TTL; a failure to start (an
  unknown graph or flow, 404; an input the target's root schema refuses, 400, checked before any run) routes to
  `exception=`; `for_each` with a sliding `concurrency` window; CompileGraph refuses `async` elsewhere, a non-boolean
  value and an `output[]` on an async node. **Step 3b built 2026-11-04: PR #611, MERGED 2026-11-04 05:03Z, squash
  `889c6f9d`** (spec sections 8.2 and 8.4 *As built*; Eric: the WebSocket entry is in 3b) - **step 3 COMPLETE**: progress events, then a terminal `result` or `error`, on a REST `stream: true` lane, the new WebSocket entry
  `/ws/graph-run/{graph_id}` (off by default), or a declared `x-return-path` (`return.path.routes`, prefix `*`);
  `progress=<text>` on a node; a node's function and a synchronous subgraph get the node's private relay
  (`graph.progress.*`), drained before the terminal event; `llm.run`'s seven events (105 loop vectors); the HTTP edge
  drops a late single-shot reply to a started stream; `kafka.progress.publisher` in kafka-demo. **Step 5 SHIPPED
  ahead of step 4 (Eric, 2026-11-05):** the Rust catch-up merged as mercury #403-#407 (`llm.run` as the crate
  `mercury-agent-loop`, all 105 loop and 29 schema vectors; the async `graph.extension`; progress events and
  `/ws/graph-run`; the Playground bundle and help by `npm run release:rust`) and released in Rust 4.12.30. **The end-to-end drive from the Java engine to BOTH packs ran 2026-11-14 (sprint 3 WP1, Eric):** the new
  `order-assistant` graph of `examples/minigraph-playground` (a three-row catalog with a refund marked `write: true`, the
  approval advice at a checkpoint, suspend on Redis, a resume that re-runs the loop node) on Claude Haiku 4.5 through the
  Python helper and the Node helper and on Opus 5.5 through the Node helper, negative control first - 40 model calls, the
  supervised write held and resumed, a refusal's note read by the model, the events over SSE and the WebSocket entry; the
  report is `docs/test-reports/agent-loop-live-drive.md`. The loop's machinery never failed; the one defect was the
  example graph's ([[graph-failed-resume-readvises]]). Next: step 4 (the Python MCP binding, then Node; not started in
  either pack), then the drive again with an MCP tool in the catalog, then the drive again with an MCP tool in the catalog. **RFC-0011 PROMOTED to ADR-0033 on 2026-11-14 (Eric)**, the
  decision formalized as [[agent-loop-tool-names-map-to-routes]]; the spec header, the register and the Vision's Track 2
  status updated the same day (the stacked PR on #624). Step 4 is built under the accepted decision.
  The engines' mentions of the packs' `examples/llm-helper` (the Playground's LLM relay, its `event-over-http.yaml` and
  README, the certification reports) are fixed by #601 (squash `72d43d06`) and mercury #401 (merge `e66ae513`), the packs' by mercury-python #48 (`463e37c`), all
  merged 2026-11-03: no live mention of the old folder is left in the family.
  <!-- id: agent-loop-llm-run | created: 2026-10-28 | last_used: 2026-11-10 | uses: 17 | tier: working | origin: 2026-10-28-233011 -->
