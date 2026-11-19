---
title: Ask a Person
summary: How an agent loop's wait for a person reaches that person and how the decision comes back - the ask row
  and the marked write that make the loop wait, the main flow of RFC-0014 served by the language packs' ask-person
  extension (the message, the signed ticket, the resume link, the reply page and endpoint), the hold path a graph
  wires before it suspends, and the agent-question app as the first store-and-forward channel.
layer: knowledge-graph
audience: [developer, architect, ai-agent]
keywords: [ask a person, human in the loop, approval, question, ask row, person.ask, resume link, ticket,
  reply page, graph.suspend, re-advice, advice, agent-question app, store-and-forward, RFC-0014, ADR-0033]
related:
  - guides/knowledge-graph/workflow-suspension.md
  - guides/agent-tools.md
  - guides/polyglot-functions.md
  - guides/transcript-audit-schema.md
---

# Ask a Person

> **At a glance**
>
> - **What** — the loop can wait for a person: a tool call marked `write: true` waits under the default mode, and
>   the **ask row** (`ask: true`, no route) lets the model put a question to a person and carry on in the same
>   conversation with the answer. The graph suspends durably and resumes at most once. **Ask a person** is what
>   carries the wait to the person and brings the decision back: `person.ask`, a function of the language packs'
>   `extensions/ask-person`, sends one message with a signed ticket and a **resume link** to the installation's
>   channels, and the link opens the wait on the pack's reply page, where a submit resumes the graph.
> - **Why it matters** — nothing delivered a waiting call to anyone before, and nothing brought a decision back except a
>   request a developer typed. The person who must decide uses a phone or a laptop that is not always connected, and
>   several waits may be open when it reconnects.
> - **The shape** — the engine waits, the pack delivers and collects; the tool sends an event and knows no channel;
>   every channel is a store-and-forward system of its own; one reply path, the link, for all of them; a GET shows and
>   only a submit resumes; a reply never starts a fresh run. The engines' loop changed nothing.
> - **Where** — the design is RFC-0014 (`draft-design-specs/ask-a-person-channel.md`); the Playground's
>   `order-assistant` and `plan-assistant` are the reference graphs.

## Two ways the loop waits

The agent loop (`llm.run`, [ADR-0033](../arch-decisions/ADR.md)) holds a turn for a person in two cases, and both
reach the graph the same way: the node's outcome is `status: approval` with an `approval.calls` block that shows every
call of the turn with its arguments, each marked `wait: true` (needs a decision) or `wait: false` (runs on resume).

| | A marked write | The ask row |
| --- | --- | --- |
| The catalog row | `"write": true` on a tool that has a route | `"ask": true` and **no route** |
| Who decides to wait | the node's `mode`: `write` (the default) holds it, `manual` holds every call, `auto` runs it, `unattended` refuses it | every mode but `unattended`, where the model is told "nobody to ask" |
| What the model put | the tool's arguments | a question and its options, shaped by the row's input schema |
| The decision on resume | `{"id": ..., "approved": true or false, "note": "..."}` | `{"id": ..., "answer": <text or any JSON value>}` |
| What the model gets back | the tool's result, or the note as the call's error | the answer as the call's result; the call log says `answered` |

The ask row is the loop spec's section 7.3: a conversation continues only from a waiting call, so a question for a
person *is* a call. The model drafts the options and carries on with the one the person picks, with everything it has
learned so far. Choosing among the graph's own branches needs no row: the output schema declares the options and a
decision node routes the person's choice after a `graph.suspend` (section 7.2 of the same spec).

## The main flow: `person.ask`, the ticket and the resume link

The language packs serve `person.ask` in `extensions/ask-person` (mercury-python first; the Node.js twin follows). The
graph's hold path calls it before it suspends, with a message the hold node composes from the loop's outcome and the
graph's own knowledge of itself:

```json
{
  "kind": "approval",
  "graph_id": "order-assistant",
  "description": "Customer-support assistant: a refund waits for a supervisor",
  "correlation_id": "order-1002-case",
  "turn": 2,
  "node": "assist",
  "summary": "A refund waits for the supervisor's decision",
  "calls": [{"id": "toolu_01...", "name": "issue_refund", "route": "demo.refund.issue", "wait": true,
             "input": {"order_id": "ORD-1002", "amount": 174.0, "reason": "Shipment lost in transit"}}],
  "ttl": "2d"
}
```

- `kind` is `approval` (a call needs `approved`), `question` (an ask row) or `advice` - a notification with no decision:
  a status update of a long run, an acknowledgement after a response, the outcome at the end. An advice carries a
  `summary` and an optional `details`, no calls and no ttl.
- `calls` is the approval block as the loop returned it: a person cannot approve what they cannot see. `ttl` is the
  suspend node's own time to live, so the expiry the function computes is the suspension's.
- The function issues the **ticket** - `v1.<payload>.<mac>`, the canonical JSON of `{g, c, t, k, e}` (graph, correlation
  id, turn, kind, expiry) and its HMAC-SHA256 under the pack's key, base64url without padding - builds the **resume link**
  `<ask.reply.url>/ask/<ticket>`, and sends the whole message with the ticket, the link and `expires_at` as one event to
  every route in its `ask.channels`: the agent-question app's intake, a chat poster, a mail sender, a topic publisher.
  It knows nothing of what a route does with the event; a channel that fails is logged by type and never fails the
  hold. It answers `{ticket, link, expires_at, delivered}`, and `{delivered}` for an advice.
- The ticket is opaque and stateless: no question text, no business data, no person, and the pack verifies it with the
  key alone, so a chat or a topic needs no store. A ticket is accepted as many times as it is presented; single use is
  the engine's at-most-once resume, and the second presentation gets `nothing waiting`.

**The reply page and endpoint** live in the same extension, mounted on the pack's host:

- `GET /ask/{ticket}` verifies the ticket, identifies the person and shows the current wait, read from the engine by the
  reference graph's **re-advice** - a request on the graph's entry with the same correlation id and the turn but no
  decisions, which answers the waiting calls and suspends again. A GET never resumes: chat clients, mail scanners and
  link unfurlers fetch URLs to build previews.
- `POST /ask/reply` with `{"ticket": ..., "decisions": [...]}` - a person's browser, or a channel app on the person's
  behalf under its own bearer with `decided_by` - verifies the ticket, maps the graph id to the engine's entry through the
  pack's `ask.graphs` allowlist (never to an address a message told it), prefixes `[who at time]` to each tool call's
  note so the [transcript audit store](transcript-audit-schema.md) carries who decided, sends the resume
  (`{turn, decisions}` with the correlation id) and returns the engine's outcome to the invoker. An expired ticket is a
  410 without a call to the engine.
- Who may answer is the pack's `ask.reply.auth`: `none` in development, `proxy` (the installation's SSO: a trusted
  header set by the gateway names the person) or `oauth2` (an access token validated against the issuer's published
  keys). The packs carry no identity provider.

The pack's README documents the message, the ticket, the configuration and the shared vector file
(`tests/vectors/ask-person-vectors.json`) the two packs implement from.

## The hold path on a graph

Three things on the graph, none of them engine code. `order-assistant` and `plan-assistant` in
`examples/minigraph-playground` are the reference shapes; the second is the question shape, an ask row named
`ask_owner` that the model calls with the plans it drafted.

**1. Notify, then stage, then suspend.** The loop's `approval` outcome goes to a `graph.task` node that calls
`person.ask` with the message, keeps what it issued in the model, and continues into the checkpoint that stages the
advice for the caller - now with the link - and pauses:

```text
create node notify
with type Task
with properties
skill=graph.task
task=person.ask
ttl=10s
exception=notify-failed
input='''
text(approval) -> kind
text(order-assistant) -> graph_id
text(Customer-support assistant: a refund waits for a supervisor) -> description
model.cid -> correlation_id
model.agent.turn -> turn
text(assist) -> node
text(A refund waits for the supervisor's decision) -> summary
model.agent.approval.calls -> calls
text(2d) -> ttl
'''
output='''
result.ticket -> model.ask.ticket
result.link -> model.ask.link
result.expires_at -> model.ask.expires_at
result.delivered -> model.ask.delivered
'''
```

The `ttl` the message carries is the suspend node's own, so the ticket expires when the suspension does. `notify-failed`
is a handler that records the error in the model and continues into the checkpoint: a pack that is down never fails
the hold, the advice still shows the waiting calls, only without a link. **Only a fresh approval notifies.** A resumed
run that carries no decisions, or decisions for another turn, re-advises and suspends again through the checkpoint
alone, so a page view or a poll never re-posts to a channel; the link persists in the model and the advice carries it
again.

**2. The gate.** A resume is an ordinary request on the graph's entry, so a request that finds no suspended record would
run the graph from the start. The resume node's fresh path therefore gates on the request's shape, exactly as the
[suspension guide](knowledge-graph/workflow-suspension.md) suggests:

```text
create node gate
with type Decision
with properties
skill=graph.math
statement='''
MAPPING: text(={input.body.decisions}) -> model.decisions_probe
IF: {model.decisions_probe} == '=null'
THEN: check-input
ELSE: nothing-waiting
'''
```

`nothing-waiting` is a mapper that answers HTTP 410 with `type: nothing-waiting`: the suspension expired or was decided
already, and a conversation is never started by a late decision. The pack's reply endpoint shows it as expired.

**3. The advices.** When `decide` accepts the decisions, an `acknowledge` node sends `kind: advice` - the decision was
received, the assistant continues - before the loop node reruns, and `report-answer` and `report-stop` send the outcome
after `respond` and `stopped`, because the person who decided may not be the caller. Each has its own handler, so a
channel that is down never stops a resumed run or changes the staged response. A long-running graph sends a status
advice at each milestone: the store-and-forward counterpart of the engine's progress events, which reach only a client
watching the run.

**What the engine does with the ttl.** `graph.suspend` takes its `ttl` as written on the node, so a re-advice suspends
again for a fresh period. A ticket keeps the expiry of the message that issued it; a suspension that was re-advised
after the ticket's expiry still waits until its own period ends, and a late decision through a new request finds it.
Keeping the record's original expiry across a re-suspend is an engine refinement the RFC lists for a later increment.

## Delivery and collection: the channels

A channel is any route that accepts the message event, delivers it to its medium - a browser, a chat, a mailbox, a
topic - rendering at least the summary and the link, buffers in whatever that medium already does when the person is
not reachable, and hands the person the link or invokes it on the person's behalf. The main flow learns nothing beyond
`delivered`.

- **The agent-question app** (RFC-0014 build plan step 3) is the first channel: a standalone application on the pack
  framework with an intake route, a PostgreSQL inbox written before any push, a stream that wakes a connected device and
  a replay that gives a reconnecting device everything open since its cursor. The person selects which card to answer;
  the card's button automates the click. Waits upsert and advices append, so an acknowledgement follows the decision it
  answers and a status update tells the person whether to wait or come back later.
- **A chat, mail, a topic** follow as recipe-style examples: a pack function bound to the vendor's MCP server posts the
  summary and the link, the chat keeps the message, the person clicks the link; a topic is an engine route over the
  Event-over-HTTP map. A reply by mail is never a decision; the link is.

## Running the reference graphs

```bash
# the pack's main flow, with the ticket key in its environment and this application's entries in its allowlist
export ASK_TICKET_KEY='a long random secret'
mercury-serve extensions/ask-person/ask_person.py -Dask.ticket.key='${ASK_TICKET_KEY}' \
  -Dask.graphs='order-assistant=http://127.0.0.1:8085/api/graph/order-assistant,plan-assistant=http://127.0.0.1:8085/api/graph/plan-assistant'
# the LLM helper and Redis as for order-assistant; then the Playground, whose Event-over-HTTP map names person.ask on port 8091
curl -s -X POST http://127.0.0.1:8085/api/graph/plan-assistant -H 'content-type: application/json' \
  -H 'x-correlation-id: release-42' -d '{"task": "Ship the payment-service migration"}'
```

The advice carries `link`; opening it shows the question with the options the model drafted, and the submit resumes
the graph with `{"id": ..., "answer": ...}`. In the test scope `MockAskPerson` serves `person.ask`, so
`OrderAssistantTest` and `PlanAssistantTest` run both shapes without a pack, a browser or a token.

## See also

- [Workflow Suspension](knowledge-graph/workflow-suspension.md) — the checkpoint, the resume node and the gate on its fresh path.
- [Built-in Agent Tools](agent-tools.md) — the tools a loop names, and the marked writes that wait.
- [Transcript Audit Schema](transcript-audit-schema.md) — where who decided ends up.
- [Polyglot Functions](polyglot-functions.md) — how the engine reaches a pack's function over Event-over-HTTP.
