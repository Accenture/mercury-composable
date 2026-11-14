---
title: Test Report — The agent loop driven live through both language packs
summary: Permanent record of the first live drive of llm.run (RFC-0011) - the order-assistant graph of the
  Playground example on Claude Haiku 4.5 and Opus 5.5 through the Python and the Node.js LLM helper, with a
  supervised write through suspend and resume, watched over server-sent events and a WebSocket session, negative
  control first - what was tested, what it found, and what was fixed.
layer: reference
audience: [developer, architect]
keywords: [agent loop, llm.run, llm.chat, tool use, approval, suspend, resume, progress events, claude, llm helper, test report]
---

# Test Report — The agent loop driven live through both language packs

*The first live drive of the agent loop of RFC-0011: the Java engine's `llm.run` (`extensions/agent-loop`) in front of
the LLM helper's `llm.chat` in the Python ([mercury-python](https://github.com/Accenture/mercury-python)) and the
Node.js ([mercury-nodejs](https://github.com/Accenture/mercury-nodejs)) packs, through the `order-assistant` graph
that `examples/minigraph-playground` ships from this round on, conducted 2026-11-14 (UTC+8) with real Claude calls on
the Claude API backend. Until this drive every test of the loop had run on a scripted `llm.chat`: the 105 shared loop
vectors prove the loop's contract, and the packs' 66 vectors prove the transcript store, but no model had taken a
turn. This is the end-to-end test the RFC's promotion to an ADR was held on.*

## Result in brief

- **The loop works end to end on both packs and both models, with no change to the graph.** One graph, deployed once,
  ran on Haiku 4.5 through the Python helper, on Haiku 4.5 through the Node.js helper, and on Opus 5.5 through the
  Node.js helper. The graph names routes; the model is the helper's configuration.
- **The supervised write holds.** The model looked the order up, checked the shipment, and asked for the refund with the
  order's exact total; the loop held the marked write, the graph staged the advice and suspended on Redis; the
  supervisor's approval resumed the loop, which ran the call it had shown and let the model answer. A refusal with a
  note went back to the model as the call's error, and the model offered a replacement instead. A resume without
  decisions, or for the wrong turn, re-advised in under 20 ms without a model call.
- **Watched runs deliver the loop's events** over server-sent events (11 events for a two-turn run) and over the
  WebSocket entry (closed with 1000 after the result), on both packs.
- **Negative control first.** With a wrong key in the helper, the loop failed by name - `401 authentication_error:
  invalid x-api-key` with the provider's request id - through the graph's handler, in 1.3 s. The success that followed
  therefore means something.
- **One defect found, in the example, fixed in the same round.** A resume carrying a decision for an unknown call id
  made the loop refuse with 400 before any token was spent, which is right, but the suspension record had been consumed
  on retrieval, so the transaction was gone. The graph now routes that failure back to its advice checkpoint and
  pauses again; verified live afterwards.
- **40 model calls** (one the deliberate failure), 3,839 output tokens; 28,795 uncached input tokens on Haiku 4.5, and
  on Opus 5.5 40 uncached input tokens against 13,734 read from the cache.

## What was under test

| Piece | Version | Role |
| --- | --- | --- |
| Java engine, `examples/minigraph-playground` | this branch on 4.12.30 | the `order-assistant` graph, the three tool routes, the REST and WebSocket entries, Redis state store |
| `extensions/agent-loop` | 4.12.30 | `llm.run`: the loop, the catalog check, the modes, the approval and resume, the progress events |
| mercury-python `extensions/llm-helper` | main at `1c79d15` | `llm.chat` with the loop contract, memory transcript store, port 8086 |
| mercury-nodejs `extensions/llm-helper` | main at `502d45b` | the same, port 8087 |
| Redis 7 (container) | `redis:7-alpine` | the graph's suspend store on 6379 |
| Models | `claude-haiku-4-5-20251001`, `claude-opus-5-5` | the helper's `llm.model`; the graph names none |

The graph: a `ToolCatalog` dataset node under the island with three rows - `order_lookup` and `shipment_status`
(reads) and `issue_refund` (`write: true`) - over an in-memory order book; the loop node in the default `write` mode
with `max_turns` 8 and a 50 s deadline; a decision on the status; a checkpoint that stages the approval advice and
suspends; a resumed run landing on a decision that re-advises or resets and re-runs the loop node. The Playground ran
from a copy of its jar with `-Dgraph.websocket.enabled=true` and, for the Node.js round, `-Dllm.peer.port=8087`.

## Scenario 1 — Negative control: a wrong key

The Python helper was started with `ANTHROPIC_API_KEY=sk-ant-bogus-negative-control`. The question about ORD-1002 ran
the pre-flight check (the catalog, the routes), then the first model turn failed:

```json
{"source": "assist", "type": "error", "status": 401,
 "message": "llm.chat failed - LLM provider error - 401 authentication_error: invalid x-api-key (request_id req_...)"}
```

HTTP 401 in 1.35 s; the helper logged `llm.chat failed - status=401 turn=1` under the same trace id. The helper was
then restarted with the real key from its environment; the engine never held one.

## Scenario 2 — The approval protocol (Python helper, Haiku 4.5)

| Run | Request | Response | Time |
| --- | --- | --- | --- |
| 1 | `{"question": "My order ORD-1002 never arrived and the tracking page stopped updating a week ago. What can you do?"}` | `type: approval`, `turn: 2`; `answered`: `order_lookup` ok, `shipment_status` ok; `calls`: one `issue_refund` with `wait: true`, `input: {order_id: ORD-1002, amount: 174.0, reason: "Shipment lost in transit - carrier declared parcel lost"}` | 14.35 s (turn 1 took 11.1 s, the first call of the session; turn 2 3.2 s) |
| 2 | `{"note": "checking in"}` | `type: approval`, `turn: 2`, `run: resume` - re-advised, the same call waiting | 17 ms, no model call |
| 3 | `{"turn": 2, "decisions": [{"id": "toolu_01KTL...", "approved": true, "note": "Carrier confirmed the loss. Approved."}]}` | `type: answer`, `turn: 3`, `run: resume`; `calls`: `issue_refund` ok; "Your refund of $174.00 USD has been issued under refund ID RF-5A0E91FC. ..." | 2.74 s |

The engine logged `llm.run approval after 2 turn(s)` and `llm.run done after 1 turn(s)` for the same conversation
reference, and a `service=llm.run` span of 14,320 ms under run 1 and 2,714 ms under run 3. Run 1's trace id appears in
33 span lines of the engine and in the helper's two turns; run 3 is a new run and a new trace, as a resume is by design.
The refund exists once in the order book: a second transaction for ORD-1002 would be refused with 409, which the
token-free test exercises.

## Scenario 3 — A refused write (Python helper, Haiku 4.5)

ORD-1004, also lost: the model asked for the refund of 240.0 with the carrier's tracking number in its reason
(approval in 5.99 s). The supervisor refused with the note "The customer asked for a replacement in an earlier chat.
Offer a replacement instead of a refund." The loop returned the note to the model as the call's error, the call log
reads `issue_refund: refused`, and the model answered in 2.26 s: "I see that according to our records, you previously
requested a replacement for this order instead of a refund. Would you prefer to receive a replacement shipment ...".
No refund was written.

## Scenario 4 — No approval needed (Python helper, Haiku 4.5)

ORD-1001, delivered: `type: answer` in one request (5.78 s, two model turns, both lookups in the first turn), naming
the delivery date, the carrier and the signature at the front desk from the tool results.

## Scenario 5 — Watched runs (Python helper, Haiku 4.5)

**Server-sent events**, `POST /api/stream/graph/order-assistant` with `accept: text/event-stream`: 11 events in
4.78 s - `node.start` ("Investigating the order", the node's own text), then the loop's `run.start` (`tools: 3`,
`max_turns: 8`), `turn.start`, `turn.end` (with the turn's usage and stop reason), `tool.start`, `tool.end` (with the
route and the milliseconds), `turn.start`, `turn.end`, `run.end` (`status: done`, the run's usage), then the `result`
with the answer and `done`. Each loop event is numbered from 1 by its sender and carries the run's correlation id.

**WebSocket**, `ws://127.0.0.1:8085/ws/graph-run/order-assistant`: the first frame started the run with the
correlation id header; the same events arrived as text frames, then the `result` frame, and the session closed with
code 1000 and the reason `done`, 5.29 s after the open.

## Scenario 6 — The Node.js helper, the same drives (Haiku 4.5)

The Playground restarted with `-Dllm.peer.port=8087` and nothing else changed. The approval protocol: approval in
6.13 s, re-advice in 16 ms, answer with refund RF-F1B68C27 in 2.48 s. The refusal: 6.14 s and 2.20 s, the call
`refused`, the model offering a replacement. The delivered order: 3.98 s. Server-sent events: 11 events in 4.64 s.
WebSocket: 5.25 s, closed 1000. The envelope's shape was the same from both helpers; one difference of
representation is a finding below.

## Scenario 7 — Opus 5.5 (Node.js helper)

The Node.js helper restarted on its default model. The approval protocol for ORD-1004: approval in 7.06 s with the
reason "Shipment lost: Blue Parcel closed the trace on 2026-11-12 without finding the parcel (tracking
BP-00912-4502)."; the approval answered in 2.49 s with refund RF-54B166BE. The resumed turn's usage read
`input_tokens: 2, cache_read_input_tokens: 1252, cache_creation_input_tokens: 254`: the pack's prompt caching, which
the loop design leaves to the pack, is at work. A watched WebSocket run (ORD-1001) took three turns in 7.05 s - Opus
called `order_lookup`, then `shipment_status`, then answered - where Haiku called both tools in one turn; 14 frames.

## Scenario 8 — A failed resume, before and after the fix

Before the fix, with the Opus helper: the question about ORD-1002 reached the approval (turn 2), and a resume with
`{"turn": 2, "decisions": [{"id": "toolu_does_not_exist", "approved": true}]}` answered HTTP 400 in 25 ms -
`decisions[0].id 'toolu_does_not_exist' is not a call waiting for approval`, from the loop's check, before any token
was spent. Right so far. But the next request on the same correlation id ran as a fresh transaction ("Missing
question"): `graph.resume` consumes the record on retrieval, the handler ended the run, and the suspension was gone
while the pack still held the conversation.

The fix is in the graph: the `failed` handler notes whether an approval was pending (`model.agent.status`), and a
new decision, `triage-failure`, sends such a failure back to the `advise` checkpoint with the error, so the run
pauses again. After the fix, live: the same bad decision answered `type: approval` in 31 ms, with
`error: "decisions[0].id 'toolu_does_not_exist' is not a call waiting for approval"`, `source: assist` and the same
call still waiting; the right decision then completed the refund (RF-13008685) in 2.67 s. The token-free test pins it.

## Findings

1. **The loop's own machinery never failed.** Every model turn, tool call, hold, resume and refusal behaved as the
   spec and the vectors say, on two packs and two models. The one defect was in the example graph's design.
2. **A prompt is advice; a catalog mark is a rule.** The system prompt asks the model to look the order up first, then
   its shipment. Haiku 4.5 did so in three runs and skipped `order_lookup` in two (the streamed run and the delivered
   order on the Node.js helper, both plain status questions); Opus 5.5 called the two tools in sequence over two
   turns where Haiku called them in parallel in one. None of this touched the write: `issue_refund` waited every time,
   because the hold comes from the row's `write: true`, not from the prompt. Design the graph's safety on the catalog
   and the mode, and leave the order of reads to the model.
3. **Caching depends on the model's minimum cacheable prompt.** Every Haiku turn reported zero cache reads and zero
   cache creation; the prompts were 995 to 2,310 input tokens, below the 2,048-token floor the API documents for the
   Haiku models. On Opus 5.5 (1,024-token floor) caching was in force from the first turn of the second run onward,
   and a resumed turn sent 2 uncached tokens. The pack's caching is correct; a short loop on Haiku simply does not
   reach it.
4. **A number crosses the packs in its own shape.** The refund amount came back as `174.0` from the Python helper and
   `174` from the Node.js helper for the same tool schema (`"type": "number"`); the Java tool took both as a `Number`.
   A tool function reads a numeric argument as `Number`, never as a concrete class.
5. **The first model call of a session is slow.** Turn 1 of the first live run took 11.1 s on Haiku; every later turn
   took 2 to 3.3 s on Haiku and 1.8 to 4.6 s on Opus. The loop's 50 s deadline and the node's 55 s TTL, under the
   executor flow's 60 s, left room; the Playground's REST entry was raised from 30 s to 60 s for the same reason.
6. **A resume is a new run and a new trace.** The engine's trace tree covers one HTTP request; the conversation
   reference, the turn count and the call ids are what tie the runs of one transaction together, with the correlation
   id. A reader of a trace store follows the `cid`, not the trace id, across an approval.

## Defects found and fixed

| # | Where | Defect | Fix |
| --- | --- | --- | --- |
| 1 | the example graph | a resume with a decision the loop refuses ended the run and lost the transaction | the handler notes a pending approval and `triage-failure` re-advises through the `advise` checkpoint; test and live run |
| 2 | the example test | the test expected the resumed call's log to carry the whole conversation's calls | the log is the calls that `llm.run` call answered; the advice now also shows the calls already answered (`answered`) |

## Reproduce

```bash
# Redis for the suspend store (or helpers/redis-standalone)
docker run -d --name mercury-redis -p 6379:6379 redis:7-alpine
# the Python helper on 8086, or the Node.js helper on 8087 - the key from the environment
mercury-serve extensions/llm-helper/llm_helper.py -Dllm.model=claude-haiku-4-5          # mercury-python
node dist/src/cli.js extensions/llm-helper/llm-helper.mjs                                # mercury-nodejs (Opus 5.5)
# the Playground from a copy of its jar, pointing at one helper
java -Dgraph.websocket.enabled=true -Dllm.peer.port=8087 -jar playground.jar
```

Then the protocol of `examples/minigraph-playground/src/main/resources/graph/README.md`: the question, the advice,
the decisions on the advised turn. The token-free version is `OrderAssistantTest` (`mvn test -f
examples/minigraph-playground/pom.xml`), on a scripted `llm.chat` and an embedded Redis.
