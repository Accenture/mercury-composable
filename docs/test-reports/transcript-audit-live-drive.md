---
title: Test Report — The transcript audit store driven live through both language packs
summary: Permanent record of the first live drive of the transcript audit store (RFC-0012) - the order-assistant graph of
  the Playground example on Claude Haiku 4.5 through the Python and the Node.js LLM helper, every model turn recorded in
  PostgreSQL by the audit function behind a local event, one correlation id across a suspend and a resume, the working
  record released after the integrity check, one PostgreSQL outage injected and repaired by the sweep, and a resume
  served by the other pack from the record the first one wrote.
layer: reference
audience: [developer, architect]
keywords: [transcript audit, audit store, agent loop, llm.chat, transcript.audit, postgresql, redis, suspend, resume, claude, llm helper, test report]
---

# Test Report — The transcript audit store driven live through both language packs

*The first live drive of the transcript audit store of RFC-0012 (`draft-design-specs/transcript-audit-store.md`): the
Java engine's `llm.run` in front of the LLM helper's `llm.chat` in the Python ([mercury-python](https://github.com/Accenture/mercury-python))
and the Node.js ([mercury-nodejs](https://github.com/Accenture/mercury-nodejs)) packs, through the `order-assistant`
graph of `examples/minigraph-playground`, with the helper's working store on Redis and its audit store on PostgreSQL,
conducted 2026-11-17 (UTC+8) with real Claude calls on the Claude API backend. Until this drive the audit had been
proven by unit tests on a recording fake of the connection pool and by a gated live test against a container; no model
had taken a turn and no graph had suspended. This is the end-to-end test the RFC's promotion to an ADR was held on.*

## Result in brief

- **Every model turn of every run is in PostgreSQL, written off the turn's path, on both packs.** Five conversations,
  fifteen turns, all `done`: each conversation row carries the graph (`order-assistant`), the node (`assist`), the
  correlation id, the first turn's system prompt and the three-tool catalog; each turn row the trace id, the UTC bounds,
  the status and stop reason, the provider's request id, the usage, the messages the turn sent and the blocks it got
  back. The turns' latencies were the model's (1.8 to 14.6 s); the audit added nothing a caller could see.
- **One correlation id, two trace ids.** Every transaction suspended on the refund approval and resumed under a new
  trace: the correlation id finds all three turns, the trace id finds the turns of one request, exactly as the schema
  page promises.
- **The working record is released only after the integrity check.** In every closed run the Redis hash was gone within
  three seconds of the answer, after the check logged `written=0 released=True`; open records kept their seven-day
  time to live with `audited` equal to their turn count. At the end Redis held only the sweep's lock.
- **An outage is repaired, not lost.** With PostgreSQL stopped during a question, the turn took 3.9 s as usual, the two
  writes failed by type and SQLSTATE (`AdminShutdown sqlstate=57P01`, then `PoolTimeout`), nothing of the content
  reached a log, and the sweep filled both turns 46 s after the database came back, leaving the open record unreleased.
  The approval that followed wrote the third turn and the check released the record.
- **A paused database stalls rather than fails** (Scenario B): a paused container is a TCP black hole, so the pooled
  connection's statement waited the pause out and the writes landed a second after the unpause. Harmless here, but the
  audit mailbox should not wait for an outage's length: both packs gain a server-side `statement_timeout` from
  `llm.transcript.audit.postgres.timeout.ms` (the one follow-up of this drive).
- **The two helpers write the same rows from the same record.** A question served by the Python helper suspended; the
  Playground was re-pointed at the Node.js helper, which resumed the conversation from the version 2 record the Python
  helper had written, took the third turn, wrote it and released the record - one conversation row, three turn rows,
  two trace ids, indistinguishable from a single-pack run.
- **The engine's graph context arrived.** `x-graph-id` and `x-node`, set by `graph.task` and copied by `llm.run` (both
  engines, merged today), are on every conversation row without the graph mapping anything.
- **19,296 input tokens and 1,646 output tokens** on Haiku 4.5 over the five conversations; no server-side cache hits,
  as the loop's cache markers meet a fresh prompt each time at this size.

## What was under test

| Piece | Version | Role |
| --- | --- | --- |
| Java engine, `examples/minigraph-playground` | main at `8d86323f2` (after #641) | the `order-assistant` graph, the three tool routes, the REST entry, the Redis state store; `graph.task` sets `x-graph-id` and `x-node` |
| `extensions/agent-loop` | main (after #641) | `llm.run`: the loop, the approval and resume; copies the two headers onto each `llm.chat` turn |
| mercury-python `extensions/llm-helper` | main at `ddbc2d1` (after #59, #60, #61) | `llm.chat` with record version 2, the `transcript.audit` function, the sweep; port 8086 |
| mercury-nodejs `extensions/llm-helper` | main at `8bf63ac` (after #126, #127, #128) | the same, field for field; port 8087 |
| Redis 7 (container) | `redis:7-alpine` | the helpers' working store (`transcript:*`) and the graph's suspend store (`graph:*`) on 6379 |
| PostgreSQL 17 (container) | `postgres:17-alpine`, the schema of `schema/postgres.sql` applied at first start | the audit store on 5432; pgweb on 8081 as the SQL browser |
| Model | `claude-haiku-4-5-20251001` | the helpers' `llm.model`; the graph names none |

Both helpers ran with `-Dllm.transcript.store=redis -Dllm.transcript.audit=postgres -Dllm.transcript.audit.postgres.ssl=false`
and the database user and password from the environment through the shipped `application.yml`; the Playground ran from
a copy of its jar with `-Dllm.peer.port=8086`, then `8087`. The containers came from the pack's `dev/postgres-up.sh`
and a plain `docker run` for Redis, on Colima.

## Scenario A — The protocol through the Python helper

The question `My order ORD-1002 never arrived. Can you help?` with the correlation id `audit-drive-py-1` returned the
approval advice in 14.6 s: two model turns, the first calling `order_lookup` and `shipment_status`, the second asking
for the refund, which the loop held. Redis then held `transcript:5504ffab…` with `turn 2`, `audited 2` and the seven-day
time to live, and PostgreSQL the conversation row - `open`, two turns, `order-assistant` / `assist`, the correlation
id, a 378-character system prompt and the three tools - and two turn rows under one trace id (`bd0fd53f…`):

```text
turn | trace_id | status   | started      | ended        | in   | out | req msgs | resp blocks | calls
1    | bd0fd53f | tool_use | 09:15:16.288 | 09:15:28.270 |  993 | 125 | 1        | 3           | 2
2    | bd0fd53f | tool_use | 09:15:28.307 | 09:15:30.474 | 1355 | 169 | 1        | 2           | 1
```

The approval (`turn 2`, the waiting call approved with a note) answered in 1.85 s under a new trace id (`776fbd33…`):
the refund issued, the model's answer naming it. Three seconds later the Redis key was gone, and the conversation row
read `done`, three turns, `ended_at 09:16:08.810`, totals 3,936 and 353 tokens, `expires_at` 365 days after the start.
The third turn's `request` is the one `tool_result` message with the refund's JSON, its `response` the answer's text
block. The helper's log shows the write after each turn (`written=1`) and the closing check (`turn=3 written=0
check=True released=True`), each `transcript.audit` execution inside the trace of the turn that sent it.

## Scenario B — A paused database (Python helper)

`docker pause mercury-transcripts`, then the question for ORD-1004 under `audit-drive-py-2`: the advice came back in
4.0 s, Redis showed `turn 2` with no `audited` field, and the helper logged nothing for 17 s. A paused container
answers nothing - no reset, no refusal - so the pooled connection's statement simply waited. On `docker unpause` the
two writes landed within a second (`17:17:40.298` and `.305`), the approval wrote the third turn and the check released
the record. No turn was slowed and nothing was lost, but the audit mailbox had waited the pause out: the follow-up is a
server-side `statement_timeout` in both packs, so a stalled database frees the function after `timeout.ms` the way a
stopped one does.

## Scenario B2 — A stopped database, repaired by the sweep (Python helper)

`docker stop mercury-transcripts`, then the question for ORD-1002 under `audit-drive-py-3`: the advice in 3.9 s. The
helper logged the failures by type, never by content:

```text
17:18:39.082 WARNING transcript.audit conversation=4d3a999b… write failed - AdminShutdown sqlstate=57P01
17:18:45.971 WARNING transcript.audit conversation=4d3a999b… write failed - PoolTimeout: couldn't get a connection after 5.00 sec
17:18:50.977 WARNING transcript.audit conversation=4d3a999b… check failed - PoolTimeout: couldn't get a connection after 5.00 sec
```

The third line is the sweep's own check during the outage. `docker start` had PostgreSQL ready in a second; 46 s later
the next sweep found the record behind (`sweep: 1 record(s) behind their audit`) and its check wrote both missing turns
without releasing the open record (`turn=2 written=2 check=True released=False`); Redis read `audited 2`. The approval
then wrote the third turn and the check released the record: `done`, three turns, two trace ids. The model's answer
noted that this order had already been refunded in Scenario A - the Playground's order book is in memory - and the
transcript holds that exchange like any other.

## Scenario C — A resume served by the other pack

The question for ORD-1004 under `audit-drive-cross` went through the Python helper (4.3 s, the advice); Redis held the
version 2 record with two turns' facts, `order-assistant` / `assist`, `audited 2`. The Playground was then restarted
with `-Dllm.peer.port=8087`, and the approval went through the Node.js helper (2.3 s): it loaded the record the Python
helper had written, took the third turn, wrote it and released the record. One conversation row, `done`, three turns;
the turn rows `1:3a149ccd 2:3a149ccd 3:7622c1f1` - the Python helper's log carries the first two writes, the Node.js
helper's the third and the check (`check=true released=true`), the same words in both.

## Scenario D — A full run through the Node.js helper, with a refused decision

The question for ORD-1002 under `audit-drive-node-1` (3.2 s, the advice; `audited 2` in Redis), then the decision
refused with a note (1.8 s): the loop sent the note back to the model as the call's error, the model answered with it,
the record was released. The conversation row reads `done`, three turns, two trace ids, 3,856 and 336 tokens; the
third turn's `request` is the `tool_result` block `refused by the reviewer: The customer prefers a replacement`, as the
model saw it.

## The questions, answered from the store

The schema page's queries ran in `psql` against the drive's five conversations. pgweb, started with the database by
the up-script, had answered on 8081 earlier the same day; after Scenario B2's stop and start of PostgreSQL it no longer
answered - the browser does not outlive its database, so the packs' up-scripts now start it with a restart policy:

```text
-- what ran between two instants, with the graph and the node
conversation | graph_id        | node   | status | turns | started  | ended
5504ffab…    | order-assistant | assist | done   | 3     | 09:15:16 | 09:16:08
845aa4c9…    | order-assistant | assist | done   | 3     | 09:17:23 | 09:17:42
4d3a999b…    | order-assistant | assist | done   | 3     | 09:18:37 | 09:19:49
fe4076ab…    | order-assistant | assist | done   | 3     | 09:20:46 | 09:21:02
3af425a5…    | order-assistant | assist | done   | 3     | 09:21:06 | 09:21:12

-- every turn of a transaction, suspend and resume included: one correlation id, two trace ids, three turns, for each
-- the whole transcript of the cross-pack conversation, as the model exchanged it
turn | request                | response blocks       | calls
1    | user text (the prompt) | thinking, text, tool_use, tool_use | 2
2    | user tool_result       | text, tool_use        | 1
3    | user tool_result       | text                  | 0
```

Across the five conversations: 15 turn rows under 10 distinct trace ids. The helper logs contain no order id, no refund
text and no prompt text (`grep` for `ORD-`, `refund`, `never arrived`: zero lines in both).

## Findings

1. **The design holds under the conditions it was built for.** The turn's path ends at the compare-and-set; the audit
   function's mailbox orders the writes and the check; the record is released only after the check; a stopped database
   costs a logged failure and a sweep interval, not a turn and not a row. The one thing the drive asked for beyond the
   design is a bound on a *stalled* database (Scenario B).
2. **The audit is a side effect of a turn, so the two helpers interoperate through the record.** The cross-pack resume
   worked because both helpers write the same version 2 record and the same rows; the byte-identical schema and the
   build notes of the design spec (section 14) are what made that true.
3. **The graph context closes the loop between the engine and the audit** without any graph edit: the conversation rows
   of the drive carry the graph and the node from the engine merged earlier the same day.
4. **The sweep is what makes "every turn" true in practice.** Both outage scenarios ended complete because of it; its
   60 s interval was visible in the 46 s repair.
5. **Retention and purge stayed the installation's.** `expires_at` was written 365 days out on every row; nothing in the
   helper deleted anything, and the sample purge script was not needed.
6. **The development browser does not outlive its database.** pgweb exited when PostgreSQL was stopped under it and
   was not back after the restart; `dev/postgres-up.sh` in both packs now runs it with `--restart on-failure`.

**The follow-ups:** `statement_timeout` from `llm.transcript.audit.postgres.timeout.ms` in the Python and the Node.js
pool, and the browser's restart policy (mercury-python and mercury-nodejs PRs of the same day). **The promotion of RFC-0012 is Eric's call** with this
report in hand.

## Reproduce

```bash
# Redis for both stores, PostgreSQL with the schema and pgweb (mercury-python or mercury-nodejs, the same scripts)
docker run -d --name mercury-redis -p 6379:6379 redis:7-alpine
export POSTGRES_PASSWORD=...
extensions/llm-helper/dev/postgres-up.sh
# the Python helper on 8086, or the Node.js helper on 8087 - the key and the database credential from the environment
mercury-serve extensions/llm-helper/llm_helper.py -Dllm.model=claude-haiku-4-5 \
  -Dllm.transcript.store=redis -Dllm.transcript.audit=postgres -Dllm.transcript.audit.postgres.ssl=false
node dist/src/cli.js extensions/llm-helper/llm-helper.mjs -Dllm.model=claude-haiku-4-5 \
  -Dllm.transcript.store=redis -Dllm.transcript.audit=postgres -Dllm.transcript.audit.postgres.ssl=false
# the Playground from a copy of its jar, pointing at one helper
java -Dllm.peer.port=8086 -jar playground.jar
```

Then the protocol of `examples/minigraph-playground/src/main/resources/graph/README.md` - the question, the advice,
the decisions on the advised turn - and the questions of the schema page
([Transcript Audit Schema](../guides/transcript-audit-schema.md)) in pgweb at `http://127.0.0.1:8081` or in `psql`.
The outage of Scenario B is `docker pause mercury-transcripts` before the question and `docker unpause` after it.
