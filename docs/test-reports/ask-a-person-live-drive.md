---
title: Test Report — Ask a person driven live through the Playground and both language packs
summary: Permanent record of the first live drive of RFC-0014 (ask a person) - the order-assistant and plan-assistant
  graphs of the Playground example on Claude Haiku 4.5 behind the Python pack's ask-person flow and agent-question app on
  PostgreSQL, then the Node.js twins, through negative controls, a pack that is down, the link in a bare browser with no
  channel app, a device online, a device off and reconnecting, a decision after the suspension expired, and two devices
  on one ticket; two defects found and fixed. Extended the same day with the page's waiting section and history
  panel, checked on the Node.js backend through an all-Node chain.
layer: reference
audience: [developer, architect]
keywords: [ask a person, human in the loop, approval, question, resume link, ticket, agent-question app, store-and-forward,
  history panel, suspend, resume, nothing-waiting, agent loop, claude, test report, RFC-0014]
---

# Test Report — Ask a person driven live through the Playground and both language packs

*The first live drive of RFC-0014 (`draft-design-specs/ask-a-person-channel.md`, build plan step 5): the Java engine's
`order-assistant` and `plan-assistant` graphs of `examples/minigraph-playground` with the `person.ask` hold path, the
Python pack's `extensions/ask-person` (the main flow: the ticket, the resume link, the reply page and endpoint) and
`extensions/agent-question` (the first channel: the store-and-forward inbox on PostgreSQL, the replay, the stream, the
automated click), and then the Node.js twins of both, conducted 2026-11-19 (UTC+8) with real Claude calls on Haiku 4.5
through the Python LLM helper. Until this drive the chain had been proven piecewise: the graphs on a scripted model and a
mock of `person.ask`, the two extensions on shared vectors and in-process tests, each pack against a fake of the other
side. This is the end-to-end test the RFC's promotion to an ADR was held on.*

The person's device was headless in this drive: no browser automation was available in the session, so the device was a
client that holds the stream exactly as the page's script does (`Last-Event-ID`, the replay first), and the decisions went
through the endpoints the page's buttons call (`/questions/{id}/decide`, `/questions/{id}/seen`, `POST /ask/reply`). The
page itself was fetched and checked for what it renders. The browser walk-through is the [manual regression
test](#reproduce-by-hand-the-manual-regression-test) at the end.

## Result in brief

- **The chain held on every scenario, on both packs.** A wait left the engine as one message, reached the inbox before
  the caller had its advice, appeared on a connected device within 10 ms of the engine staging it, was decided from the
  device through the pack's reply endpoint, resumed the graph exactly once, and the acknowledgement and the outcome came
  back to the device as advices. A device that was off found every wait and every advice it had missed in one replay,
  answered the waits in the opposite order, and the reconnect resumed from its cursor. A late decision met the gate's
  410 and the card turned expired; two devices on one ticket both reached the engine and exactly one resumed.
- **Two defects found and fixed.** (1) The reply page showed a dead wait as *The graph refused* with the engine's
  input check, because the gate recognized only `decisions`: a re-advice of a dead wait carries the ticket's turn and no
  decisions, so it ran the fresh path. The gate now treats a request with a `turn` as a resume attempt too (both graphs,
  the guide, two new tests). (2) The inbox's decision write was unconditional: with two devices on one card the row took
  whichever outcome landed last, so a late `nothing waiting` could overwrite the accepted decision and its operator. A
  decision's write now applies when the row is still open or when the engine accepted the decision, in both packs
  (mercury-python #71, mercury-nodejs #136; vectors version 2, a race test on both orderings).
- **Who decided reaches the audit store only for a refusal.** The `[who at time]` stamp travels in the note; the loop
  sends a refused call's note to the model as the call's error, so it is in the next turn's request, while an approved
  call's note goes nowhere the model or the audit sees. The decision and its operator are on the inbox row and in the
  pack's log; carrying the decisions into the turn's audit facts is a loop change listed as a follow-up.
- **A wait decided elsewhere stays `open` on a device's card** until that device tries it (the engine's 410 turns it
  expired) or the sweeper reaches its expiry; the acknowledgement advice beside it tells the person. Marking it from
  the advice is a design question for the RFC, not done here.
- **The page's waiting section and history panel (mercury-python #72, mercury-nodejs #137, the same day) hold on both
  backends.** The maintainer's browser pass on the Python backend, and the all-Node chain below on the Node backend:
  a case stays under *Waiting for you* while any of its cards is open, moves into the scrollable *History* panel when
  the last one closes, the badges and the tab's title count the open cards, and the panel keeps its scroll position
  across the re-render every event causes.
- **13 conversations, 35 model turns, 42,951 input and 4,395 output tokens on Haiku 4.5**; the inbox ended with 29 rows
  (9 approvals, 2 questions, 18 advices) and a change sequence of 46; the Python flow served 27 `person.ask` calls and
  17 replies (8 answers, 8 `nothing-waiting`, one 502 from a negative control with no engine), the Node flow 8 calls and
  4 replies. No warning in any pack log, no channel failure, nothing of a prompt or an answer in a log.

## What was under test

| Piece | Version | Role |
| --- | --- | --- |
| Java engine, `examples/minigraph-playground` | main at `726344833` (after #656), the gate fix of this drive applied and rebuilt | `order-assistant` and `plan-assistant` with the hold path (`notify` → `advise` → `suspend`, the gate, `acknowledge`, `report-answer`/`report-stop`, the handlers), the REST entry, the Redis state store; `person.ask` over Event-over-HTTP (`ask.peer.port`) |
| `extensions/agent-loop` | main (after #641) | `llm.run`: the approval block, the resume with the decisions, at-most-once |
| mercury-python `extensions/llm-helper` | main at `a6c5a15` (after #70) | `llm.chat` on Haiku 4.5, transcript store on Redis, audit store on PostgreSQL; port 8086 |
| mercury-python `extensions/ask-person` | main at `a6c5a15` | `person.ask`, the ticket, the link, `GET /ask/{ticket}`, `POST /ask/reply`; port 8091; first with no channel, then with the app |
| mercury-python `extensions/agent-question` | main at `a6c5a15`, then the first-decision fix of this drive | the inbox on PostgreSQL, `/questions`, `/questions/events`, `/questions/{id}/decide`, `/questions/{id}/seen`; port 8092 |
| mercury-nodejs `extensions/ask-person`, `extensions/agent-question` | main at `b671ae6` (after #135), the first-decision fix applied | the twins, on 8094 and 8093, behind the same Playground (`-Dask.peer.port=8094`) |
| mercury-nodejs `extensions/llm-helper`, `extensions/ask-person`, `extensions/agent-question` (section H) | main at `d2fade2` (after #137) | the all-Node chain of the history-panel check: the Node helper on 8087 (Haiku, the transcript store in memory), the twins on 8094 and 8093, the Node inbox on a PostgreSQL of its own (5433) |
| Redis 7 (container) | `redis:7-alpine` | the graph's suspend store (`graph:<graph>:<cid>`) and the helper's working store, 6379 |
| PostgreSQL 17 (container) | `postgres:17-alpine`, the helper's `schema/postgres.sql` at first start, the app's `schema/postgres.sql` by `dev/schema-apply.sh` | the transcript audit store and the question inbox, one database; pgweb on 8081 |
| Model | `claude-haiku-4-5-20251001` | the helper's `llm.model`; the graphs name none |

The ticket key and the database password were random values generated for the drive and kept in the shell's environment
(`ASK_TICKET_KEY`, `POSTGRES_PASSWORD`), never in a file of either repository. The commands:

```bash
docker run -d --name mercury-redis -p 6379:6379 redis:7-alpine
POSTGRES_PASSWORD=... extensions/llm-helper/dev/postgres-up.sh          # mercury-python; the audit schema at first start
extensions/agent-question/dev/schema-apply.sh                            # the inbox schema, safe to repeat
mercury-serve extensions/llm-helper/llm_helper.py -Dllm.model=claude-haiku-4-5 \
  -Dllm.transcript.store=redis -Dllm.transcript.audit=postgres -Dllm.transcript.audit.postgres.ssl=false
mercury-serve extensions/agent-question/agent_question.py -Dquestion.inbox=postgres -Dquestion.inbox.postgres.ssl=false
mercury-serve extensions/ask-person/ask_person.py -Dask.ticket.key='${ASK_TICKET_KEY}' \
  -Dask.graphs='order-assistant=http://127.0.0.1:8085/api/graph/order-assistant,plan-assistant=http://127.0.0.1:8085/api/graph/plan-assistant' \
  -Dask.channels=agent.question.inbox -Dask.channel.endpoints='agent.question.inbox=http://127.0.0.1:8092/api/event'
java -jar examples/minigraph-playground/target/minigraph-playground-4.12.30.jar   # mercury-composable; person.ask -> 8091 by default
```

## Negative controls (no engine running)

Run first, with the Playground not yet started, so any call to the engine would have shown as a 502.

| Probe | Result |
| --- | --- |
| `GET /ask/not-a-ticket`; `POST /ask/reply` with `v1.abc`; no ticket; a body that is not JSON | 400 *Ticket is malformed*; 400; 400 *ticket is required*; 400 *the reply must be a JSON map with ticket and decisions* |
| A well-formed ticket with a wrong MAC (GET and POST); version `v2` | 403 *Ticket signature is invalid*, both; 400 *Ticket version v2 is not supported* |
| A ticket signed with the key but expired an hour ago (GET and POST) | 410 *This wait has expired* / `{"type": "expired"}` - no call to the engine |
| A valid ticket for a graph outside `ask.graphs` | 404 *graph other-graph is not configured for replies* |
| Decision shapes: `[]`, no id, neither `approved` nor `answer`, both, `approved: "yes"`, an answer with a note, a numeric note; `decided_by: 7` | each a 400 naming the rule, before any engine call |
| A valid shape on a valid ticket with no engine | 502 *the graph did not answer - ClientConnectorError* |
| The intake with an approval and no ticket; an unknown kind; an advice with no summary | 400 *ticket is required for an approval or a question*; 400 *kind must be one of approval, question, advice*; 400 *summary is required*; the inbox stayed empty |
| `/questions/999/decide`; `decisions: []`; `?since=abc`; the empty replay; the page | 404; 400; 400; `{"since": 0, "rows": [], "next": 0, "more": false}`; 200 `text/html` |

## S0 — The pack is down: the hold stands without a link

With `ask-person` stopped, the question for ORD-1002 (`s0-pack-down`) returned the approval in 4.70 s with
`notify_error: Connection refused (connect failed): /127.0.0.1:8091`, no `link`, the waiting `issue_refund` call and the
two answered lookups - the `notify-failed` handler ran and the run suspended. The pack came back; a re-advice (the same
correlation id, `{"turn": 2}`) answered in 13 ms with the same advice, still without a link and without telling any
channel (`person.ask` was not called: only a fresh approval notifies). The decision straight on the engine resumed the
run (1.31 s, the refund issued), and the pack - now up, with no channel configured - logged the acknowledgement and the
outcome advices with `0 of 0 channel(s) took it`.

## S1 — The link in a bare browser, no channel app at all

`ask-person` ran with `ask.channels` empty, so the link travelled in the advice alone - what a chat message would carry.
The question for ORD-1004 (`s1-chat-link`) returned in 3.57 s with `link: http://127.0.0.1:8091/ask/v1.eyJj…`,
`expires_at` two days out and `delivered: []`. Opening the link (`GET`) returned the page in one request: *Approval -
order-assistant*, the correlation id and the turn, the `issue_refund` call with its arguments, approve/refuse and a note
field, the submit button posting to `/ask/reply`. Behind it the engine's log shows the re-advice: `resume → decide →
advise → suspend → end` in 8 ms, no `notify`, the record re-persisted for a fresh 172,800 s. The submit (the page's own
`{ticket, decisions}` POST) resumed the graph in 2.17 s: `type: answer`, the refund issued, the acknowledgement and the
outcome advices sent to no channel. The Redis record was gone. The same ticket presented again: the POST got the gate's
410 `nothing-waiting`; the GET showed **the first defect** - *The graph refused* with the engine's
`Missing question. Provide the customer's question in the 'question' field` (400): the re-advice of a dead wait carries a
turn and no decisions, which the gate took for a new case. After the fix (below) the same GET answers 410 *This wait has
expired*.

## S2 — The device is online

`ask-person` restarted with `ask.channels: [agent.question.inbox]` and the endpoint of the app on 8092; a device held
`GET /questions/events` from cursor 0. The question for ORD-1002 (`s2-online`) returned in 3.98 s with
`delivered: [{"route": "agent.question.inbox", "ok": true, "status": 200}]`. What the device saw, in order:

```text
16:22:28.188  seq 1  approval  open      turn 2  A refund waits for the supervisor's decision    (8 ms after the engine staged the advice)
16:22:28.640  seq 2  advice    open      turn 2  The supervisor's decision was received; the assistant continues
16:22:30.105  seq 3  advice    open      turn 3  The order assistant answered the customer
16:22:30.118  seq 4  approval  answered  turn 2  … answered_by supervisor-a
16:22:32.494  seq 5  advice    seen      turn 2
16:22:32.542  seq 6  advice    seen      turn 3
```

The decision went through the app (`POST /questions/1/decide` as `supervisor-a`, 1.51 s): the app posted
`{ticket, decisions, decided_by}` to the link's own base, the engine acknowledged (the advice reached the inbox 0.45 s
later, before the loop reran), answered (the outcome advice at +1.9 s) and the app wrote the outcome on the row 13 ms
after that. The row carries the decision, the operator and the engine's `200 answer`; the two advices were marked seen.

## S3 — The device is off, then reconnects

The device was stopped. Three cases ran: an approval (`s3-order-a`, 3.66 s), a question on `plan-assistant`
(`s3-plan-b`, 1.98 s, three options drafted by the model), and a third approval (`s3-order-c`, 3.30 s) decided through
the link while the device was off - a refusal with a note, as a person with a chat link would do (1.57 s; the model
offered a replacement instead). The inbox then held five rows since the device's cursor: the two open waits, the third
wait (still `open` - the app did not see that decision), and the third case's acknowledgement and outcome advices. The
reconnect with `Last-Event-ID: 6` replayed exactly those five rows within 14 ms, and `GET /questions?since=6` listed the
same five with `next: 11`. The waits were answered in the opposite order: the question first (`owner-b`, option 2, 3.11
s - the model wrote the plan for the chosen option and its acknowledgement and outcome advices followed), then the
approval (`supervisor-a`, 1.46 s). The third case's card, tried from the device, met the engine's 410 in 34 ms and
turned `expired` with the outcome shown. The audit store holds the refusal's stamp in the third turn's request:
`refused by the reviewer: [anonymous at 2026-11-19T08:23:37.262Z] Not this one - offer a replacement instead`.

## S4 — A decision after the suspension expired

The question for ORD-1002 (`s4-expiry`, 4.02 s) left a card and a Redis record with a ttl of 172,800 s. The record's
ttl was set to one second and Redis expired it - what two days would have done. The decision from the card met the
gate's 410 in 26 ms and the card turned `expired` with `nothing-waiting` as its outcome; the same decision through the
link's endpoint got the same 410; the audit store shows the conversation stopped at two turns - no conversation was
started by the late decision. The link's GET showed the first defect again (*The graph refused*, 400); after the fix
it shows 410 *This wait has expired*, the re-advice itself answers 410 in 12 ms on both graphs, and a fresh case with
neither a turn nor decisions still meets the input check (400 *Missing question*).

## S5 — Two devices on one ticket

The question for ORD-1004 (`s5-two-devices`, 3.52 s), then two decisions on the same card posted at the same moment as
`device-1` and `device-2`. Both passed the app's open check and both reached the engine, which restored the record once:
`device-1` got `200 answer` (1.47 s) and `device-2` got `410 nothing-waiting` (22 ms). A third device after the fact got
the app's 409 *row 14 is answered - decided by device-1* without an engine call, and the bare link's POST the engine's
410. The stored row read `answered by device-1` - but only because the 410 had landed first and the acceptance last: the
write was unconditional, and the opposite order would have left the row `expired by device-2` over an accepted
decision (**the second defect**). After the fix the race was run twice more (`s5b-race-1`, `s5b-race-2`): the losing
device's 410 landed first both times (24 ms and 17 ms against 8.23 s and 1.64 s), turned the row expired for a moment,
and the acceptance won it back - `answered by device-1` with device-1's note, the losing device answered with its own
410. The opposite order is pinned by the race test with a scripted reply endpoint, where the 410 that follows an
acceptance changes nothing and the loser sees who decided.

## N — The Node.js twins end to end

The Node `ask-person` on 8094 (its channel the Node `agent-question` on 8093, the same PostgreSQL inbox) and the
Playground restarted with `-Dask.peer.port=8094`. A device on the Node app's stream from cursor 35. A `plan-assistant`
question (`node-plan`, 3.24 s) arrived as row 23; the owner answered through the Node app (`owner-node`, option 1,
3.13 s) and the acknowledgement, the outcome and the answered row followed on the stream (seq 37-39). Two devices on one
card (`node-race`): `device-2` 410 in 35 ms, `device-1` 200 in 1.38 s, the row `answered by device-1`, a third device
409 *decided by device-1*. A dead wait (`node-dead`, the record expired): the Node reply page answered 410 *This wait has
expired*, its endpoint 410 `nothing-waiting`, the card `expired`. The two twins wrote and read one inbox table through
the drive, and every row the Node app read of the Python app's rows rendered the same.

## H — The page's waiting section and history panel, on the Node.js backend

After the drive the page gained a sticky header with the count of open cards, a *Waiting for you* section and a
scrollable *History* panel (mercury-python #72, mercury-nodejs #137, the generated page byte-identical). The maintainer
checked it in a browser on the Python backend; the Node backend was checked here through an all-Node chain - the Node
helper on Haiku 4.5, the Node `person.ask` on 8094, the Node agent-question app on 8093 with its inbox on a PostgreSQL
container of its own, the Playground pointed at both (`-Dllm.peer.port=8087 -Dask.peer.port=8094`). Still headless:
after every step the page's own script, fetched from the Node backend, was run under a minimal DOM against the
backend's live rows, and a device held the stream throughout. What it rendered:

| Step | Waiting for you | History | Badges, title |
| --- | --- | --- | --- |
| the empty inbox | *Nothing waits for you.* | *Nothing yet.* | none, `Agent questions` |
| H1 an approval arrives (4.7 s) | `h1-order` [approval open] | - | 1 / -, `(1) Agent questions` |
| H1 decided through the app (1.5 s): two advices open | `h1-order` [advice open, advice open, approval answered] | - | 2 / - |
| H1 both advices marked seen | *Nothing waits for you.* | `h1-order` | - / 1 |
| H2 a question, H3 an approval refused through the Node reply link | `h3-order` [approval open, advice open, advice open], `h2-plan` [question open] | `h1-order` | 4 / 1 |
| H3 card tried (410, expired), its advices seen | `h2-plan` | `h3-order`, `h1-order` | 1 / 2 |
| H4 the record expired, decided late from the card (410) | `h2-plan` | `h4-order`, `h3-order`, `h1-order` | 1 / 3 |

A case stays under *Waiting for you* while any of its cards is open, an unread advice included, and moves into the
panel when the last one closes; the panel lists newest first; a case with nothing to read, such as H4's expired wait,
goes straight to the history. With the panel scrolled to 150 before the last render it read 150 after it. Every form
was in the waiting section and none in the panel. The device saw the fifteen changes in order (seq 1 to 15), and the replay from a mid-way cursor (`since=6`) returned exactly the
five later changes with `next: 15`. The Node backend logged the three decisions with their outcomes and states
(`200 -> answered`, `410 -> expired`, `410 -> expired`); the Node helper took the model turns.

## Defects found and fixed

| # | Where | Defect | Fix |
| --- | --- | --- | --- |
| 1 | the reference graphs (here) | the gate recognized only `decisions`, so the reply page's re-advice of a dead wait (a turn, no decisions) ran the fresh path and the page showed *The graph refused* with the engine's input check | the gate probes `turn` too (`&&` of the two probes), both graphs, the guide's snippet and the graph README; `aReadviceWithNothingWaitingIsRefused…` in both test classes (OrderAssistantTest 9, PlanAssistantTest 5); re-verified live |
| 2 | `extensions/agent-question`, both packs | the decision write was unconditional: two devices on one card both reach the engine, and the row took whichever outcome landed last, so a late 410 could overwrite the accepted decision and its operator | a decision's write applies when the row is still open or when the engine accepted the decision (`state = 'answered'`), memory and `SQL_DECIDED` alike; the losing device gets the row as it stands and nothing is broadcast for it; vectors version 2 with the two-devices transition case (31 cases, byte-identical, SHA-pinned in Node); a race test on both orderings; re-verified live on both packs |

## Findings and follow-ups

- **Who decided is in the audit store only when the note reaches the model.** The stamp rides the note; a refusal's note
  is the call's error and lands in the next turn's request (S3), an approval's note goes nowhere the audit sees. The
  decision with its operator is on the inbox row (`answered_by`, `decisions`) and in the pack's log. The durable fix is
  the loop carrying the decisions into the resumed turn's facts (RFC-0012's audit facts, an engine and helper change);
  the RFC's claim is narrowed accordingly.
- **A wait decided elsewhere stays `open` on another device's card** until that device tries it or the sweeper reaches
  the expiry; the acknowledgement advice for the same turn sits beside it. Closing the card from that advice would need
  a state the schema does not have (answered elsewhere, no operator known to the app) - a question for the RFC.
- **The health's `connected` count lags a device that dropped** until the next write or keep-alive finds the socket gone
  (it read 1 for a second after the device stopped). Cosmetic.
- **The demo's order book remembers refunds across cases**: a second case on the same lost order gets *already refunded*
  from the refund tool and the model says so. A Playground restart resets the book; use ORD-1002 and ORD-1004 in turn.
- **Scenario note:** the device's replay after a reconnect and the stream's `Last-Event-ID` produced identical row
  sets in every reconnect of the drive; the keep-alive comments never carried a row.

## Reproduce by hand: the manual regression test

The same chain, with a browser as the device. Four terminals, the engines' repository at `mercury-composable`, the pack
at `mercury-python` with its virtual environment active, Colima or Docker running.

1. **Infrastructure.** `docker run -d --name mercury-redis -p 6379:6379 redis:7-alpine`, then
   `POSTGRES_PASSWORD=<any throwaway> extensions/llm-helper/dev/postgres-up.sh` and
   `extensions/agent-question/dev/schema-apply.sh` from the pack's root. Export `ANTHROPIC_API_KEY`, the same
   `POSTGRES_PASSWORD` and a random `ASK_TICKET_KEY` in every pack terminal.
2. **The LLM helper** (terminal 1): `mercury-serve extensions/llm-helper/llm_helper.py -Dllm.model=claude-haiku-4-5
   -Dllm.transcript.store=redis -Dllm.transcript.audit=postgres -Dllm.transcript.audit.postgres.ssl=false`.
3. **The agent-question app** (terminal 2): `mercury-serve extensions/agent-question/agent_question.py
   -Dquestion.inbox=postgres -Dquestion.inbox.postgres.ssl=false`. Open http://127.0.0.1:8092/questions/app, type a
   name in the header (it travels as `decided_by`); the header reads *connected*.
4. **The main flow** (terminal 3): the `mercury-serve extensions/ask-person/ask_person.py` command of the setup block
   above, with `-Dask.channels=agent.question.inbox` and the endpoint of the app.
5. **The Playground** (terminal 4): `java -jar examples/minigraph-playground/target/minigraph-playground-4.12.30.jar`
   after `mvn clean package -DskipTests -f examples/minigraph-playground/pom.xml`; wait for the health at
   http://127.0.0.1:8085/health.
6. **Device online.** Post the question with a fresh correlation id:
   `curl -s -X POST http://127.0.0.1:8085/api/graph/order-assistant -H 'content-type: application/json'
   -H 'x-correlation-id: case-1' -d '{"question": "My order ORD-1002 never arrived. Can you help?"}'` - the reply carries
   `link` and `delivered`. The card appears under *Waiting for you* without a reload - the refund call with its
   arguments, the badge and the tab's title reading 1. Approve with a note and send: the card turns *answered ·
   decided by <your name>*, and two advices appear under the same case - the acknowledgement, then the outcome with the
   model's answer - so the badge reads 2. Mark them *Seen*: the whole case moves into the *History* panel, the badge
   disappears and the history count grows by one. Scroll the panel, then post the next case: the panel keeps its place
   while the waiting section updates.
7. **Device off.** Close the page. Post two cases: ORD-1004 on `order-assistant` (`case-2`) and
   `curl … /api/graph/plan-assistant -H 'x-correlation-id: plan-1' -d '{"task": "Ship the payment-service migration"}'`.
   Reopen the page: both cards are there, the question with its options as radio buttons. Answer the plan first, then
   the refund; each gets its advices.
8. **The bare link.** Post a third case (`case-3`, ORD-1002), copy `link` from the reply and open it in a new tab: the
   pack's own page shows the wait; refuse it with a note and submit - the outcome appears under the form. Back on the
   app's page the card is still *open*, with the acknowledgement advice beside it; try to send a decision from it and it
   turns *expired · nothing-waiting*. Open the link a second time: *This wait has expired*.
9. **After expiry.** Post `case-4`, then `docker exec mercury-redis redis-cli expire graph:order-assistant:case-4 1`
   and wait two seconds. Decide the card: *expired · nothing-waiting*; the link shows *This wait has expired*.
10. **Two devices.** Post `case-5` and open the app in a second browser (or a private window) under another name.
    Decide the same card from both as close together as you can: one browser shows *answered · decided by* its name
    and the other the 410 outcome; after a moment both show the row answered by the first. A third attempt is refused
    by the app: *row N is answered - decided by …*.
11. **Evidence.** pgweb at http://127.0.0.1:8081: `select correlation_id, kind, state, answered_by, outcome->>'status'
    from question_inbox order by id` for the cards, and `select correlation_id, turn, status, left(request::text, 200)
    from transcript_turn order by conversation, turn` for the turns - the refusal's `[name at time]` stamp is in the
    request of the turn that followed it.

## Before promotion

Everything step 5 asked for is driven, the two defects are fixed in the repositories they belong to, and the RFC's
build notes record the findings. What remains for the RFC is step 6 (the chat, mail and topic examples, when a server is
available to drive) and the promotion to an ADR on the maintainer's call, with the two follow-ups above as the loop's
and the app's open items.
