# The transcript audit store — a durable, queryable record of every model turn — design spec

**Status:** DRAFT, round 2 (2026-11-17, Claude Code for Eric). Round 1 (2026-11-16) was raised as RFC-0012 in
[`docs/arch-decisions/RFC.md`](../docs/arch-decisions/RFC.md). Eric's rulings of 2026-11-17 are recorded in section 11 and
applied throughout: the audit is written by a composable function behind a local event, never on the turn's path, and
the end of a run is an integrity check before the working record is released. The defaults set without a ruling are
listed in section 12 for round 2.
**Serves:** `bp-agent-orchestration` → `vision-mercury-composable` (Track 2, the graph as AI SDLC).
**Extends:** [`llm-helper-agent-loop.md`](./llm-helper-agent-loop.md) section 3.3 (the transcript store) and section 10
(telemetry, audit and security); answers the concept's Q4 (audit capture) in
[`ai-agent-orchestration.md`](./ai-agent-orchestration.md) section 6.
**ADR-0033 stands:** the transcript lives on the pack's side and never crosses an engine codec. This paper adds a second,
durable store beside the working one; it does not move the transcript.
**Repo scope:** the packs' LLM helper (mercury-python first, mercury-nodejs in lockstep): the record, the audit
function, the PostgreSQL implementation, the development setup. Here: the published schema specification under
`docs/`, and the graph-context headers in both engines (section 8), part of this build (ruling R5).

---

## 1. The problem

The helper's transcript store is a **working store**. It holds a conversation while the run goes on, keyed by the
reference `llm.chat` returned, written with a compare-and-set on the turn count, and its time to live is the
transaction's own lifetime: a run waiting for a person keeps its record for `transcript_ttl`, and when the model ends
the run, `retention` says how long the record lingers before Redis drops it (the default, `0`, deletes it at the end).

That is the right shape for the transaction and the wrong shape for an audit. Telemetry carries no content by rule
(section 10 of the loop spec), so the only place a prompt, a completion or a tool call's arguments ever exist is the
working store, under a key nobody outside the loop knows, for as long as a TTL. A person who must answer "what was the
model told, what did it say, and what did it call, for this transaction on that day" has no store to ask, no index to
ask it by, and no retention policy to rely on. The concept's Q4 left exactly this open.

The two lifetimes must be separated: **the working store's expiry covers the transaction; the audit store covers the
record.** Eric's framing (2026-11-16): the Redis retention is the transaction's expiry timer; at the end of the graph's
processing the transcript is saved to a persistent store for long-term storage, indexed by trace id, correlation id and
UTC time, so a person can query it; the schema is published so that each field installation may implement its own.
His rulings of 2026-11-17 (section 11) set how: every turn is recorded, asynchronously, by a composable function that
receives a local event; the end of the run is a data-integrity check, in the same function, that persists whatever is
missing before the Redis record is deleted.

## 2. Principles

1. **The engine never holds a transcript** (ADR-0033). The pack's helper is the only writer of the audit store, as it is
   of the working store. The engine's loop contributes identity, never content (section 8).
2. **Two stores, two jobs.** The working store stays as built: Redis or memory, the compare-and-set, the TTL as the
   transaction's expiry. The audit store is durable, append-only, indexed for people, and keeps records on a policy the
   installation sets, independent of the transaction.
3. **A published contract, one reference implementation.** PostgreSQL is the reference store, shipped in both packs.
   The contract is the record (section 3) and the reference schema (section 6), published as a specification, so an
   installation that runs another database implements the same tables and the same semantics behind the same
   interface, and the helper's writer stays one.
4. **Content lives where the installation governs it.** The audit store holds prompts, completions and tool
   arguments, so it may hold personal data. It is the installation's governed database, with roles, TLS and a
   retention policy, never a file in a folder and never a log line.
5. **The record is the provider's format, as sent and as returned.** What the model saw and what it answered are
   stored as the content blocks the API exchanged, not a paraphrase, so an audit can reconstruct the exact turn. The
   normalized calls (`{id, name, input}`) are stored beside them for the person who queries, not instead of them.
6. **The audit never slows or fails a turn** (Eric, rulings R1 and R2). The turn's path ends at the working store's
   compare-and-set. The audit is a local event to a composable function that writes in the background; a failed write
   is logged and the turn is never refused. Integrity comes from a check at the end of the run and a sweep behind it,
   not from a synchronous write.

## 3. What is recorded

Two kinds of row, so that the natural questions each hit one index.

**A conversation** (one row per reference `llm.chat` returned):

```text
conversation        the reference (32 hex characters), the key of the working-store record
correlation_id      the business correlation id of the transaction (my_correlation_id): the same across a graph's
                    suspend and resume, so one value finds every turn of a transaction
graph_id, node      the graph and the loop node, from the two headers of section 8 (null when a caller sends none)
model               the model pinned at the first turn
status              open | done | refusal | cap | expired (section 6)
turns               the turn count so far
started_at          UTC, the first turn's start
updated_at          UTC, the last write
ended_at            UTC, when the model ended the run (null while open)
input_tokens, output_tokens, cache_creation_input_tokens, cache_read_input_tokens   totals
system              the system prompt, as sent on the first turn
tools               the catalog as advertised to the model on the first turn (name, description, input_schema, strict)
expires_at          UTC, started_at plus the installation's audit retention: a hint for the purge (section 6)
```

**A turn** (one row per model turn of the conversation):

```text
conversation, turn  the key; turn counts from 1
trace_id            the distributed trace id of the event that carried this turn - a resume runs under a new one,
                    so a trace id finds the turns of one request and a correlation id finds them all
trace_path          the trace's path (the entry that started the transaction, as the host records it)
correlation_id      repeated here so a query by correlation id needs no join
started_at, ended_at   UTC, the model call's bounds
stop_reason         the provider's stop reason as returned
status              done | tool_use | refusal | cap
request_id          the provider's request id, when it returned one
input_tokens, output_tokens, cache_creation_input_tokens, cache_read_input_tokens
request             the user message of this turn as sent: the prompt (turn 1), or the one message of tool_result
                    blocks that answered the previous turn's calls (is_error on a failure)
response            the assistant content blocks as returned: text, tool_use, thinking - exactly as stored in the
                    working record
calls               [{id, name, input}] normalized, when status = tool_use; null otherwise
```

The tool **results** are the next turn's `request`: the `tool_result` blocks carry `tool_use_id`, which joins to the
previous turn's `calls`. The transcript of a conversation is its turns in order, `request` then `response` each; the
system prompt and the tools are on the conversation row, since the loop sends them unchanged on every turn (ruling R8).
Everything a row needs is in the working record as the turn happens (section 5.1), so the audit function reads the
record and nothing else.

## 4. When the audit is written

Three shapes were weighed in round 1; the table stays as the record of why. **Ruling R1 (Eric, 2026-11-17): A, every
turn, asynchronously.**

| | A. Every turn (ruled) | B. At the end of the run | C. Every turn, plus a close message |
| --- | --- | --- | --- |
| What is written | the turn row after each model turn; the conversation row upserted | the whole record once, when the run ends | A, and the loop's own reason when it stops the run |
| A run that ends normally | complete | complete | complete |
| A run the loop stops (turn limit, deadline) or never resumes | complete: every turn that ran is there; status stays `open` until the TTL, then `expired` by the installation's purge (section 6) | **lost**: the helper never learns that the loop stopped, so the record expires unaudited | complete, with the reason |
| A process that dies mid-run | every finished turn is there | lost | as A |
| Cost | one small write per turn, after the model call, off the turn's path | one write per run | A plus one event per run |
| Engine change | none | a close contract, or an end-of-run event (the helper cannot know the loop stopped) | the close contract |

The one thing A does not record is *why* a run that the loop stopped ended, because the loop decides that after the
last turn; the turns themselves are all there. C adds it later without changing A: the loop sends a close event
(`conversation`, the outcome, the turn count) to a route the helper serves, and the helper stamps the conversation row.
It stays a later step.

**The write is decoupled from the turn (ruling R2).** `llm.chat` never writes the audit store. After the working
store's compare-and-set has accepted the turn, it sends one local event to the audit function (section 5) and returns
its reply; the function writes the row in the background. A failed write is logged - the conversation reference, the
turn number, the failure's type, nothing of the content - and the turn is not refused: the write is asynchronous, so
nothing on the turn's path can fail because of it. Integrity is restored by the check below and the sweep of section
5.4. The `required`/`best-effort` switch of round 1 is gone with the synchronous write it governed.

**The end of a run is an integrity check (Eric's notes, 2026-11-17).** When the model ends the run, the closing turn
sends a second event behind the first: the check. The function counts the turns the working record holds and the rows
the audit store holds for the conversation, writes the missing ones from the record, and only then releases the
working record. Both events go to one function with one worker, so they are processed in the order they were sent: the
check can never overtake the write it verifies, and no two writers race on one conversation. The sequencing is the
mailbox's, not a lock's, and there is no eventual-consistency window between a turn's write and the check that follows
it, because the check runs after the write by construction.

**The working record outlives the run by the audit's grace, not by `retention` (ruling R3).** Eric: there is not much
reason to keep the Redis record after the turns are complete. With an audit store configured, the closing compare-and-set
keeps the record for `llm.transcript.audit.grace` instead of applying `retention` (whose default `0` would delete it
before the check could read it), and the check releases it once the audit holds every turn: `retention` seconds to
linger, `0` deleting it at once. The deletion of the Redis record is therefore deferred until after the integrity check,
never before it. Without an audit store, `retention` applies at the close as today. `retention` and the engine's
`llm.run.transcript.retention` stay working-store settings, documented as such; the long-term copy is the audit's. A
record a turn cut off at `max_tokens` closed is released the same way: the run cannot go on, and once audited there is
nothing to keep it for.

## 5. The audit function

### 5.1 The record carries its own audit facts (record v2)

The working record (`llm-helper-agent-loop.md` section 3.3: versioned JSON under the hash `transcript:{conversation}`)
becomes version 2. It keeps `model`, `turn`, `pending`, `closed` and `messages` as they are and gains what the audit
rows need and the transcript did not hold: the first turn's `system` and `tools` (ruling R8: stored once), the
conversation's `correlation_id`, `graph_id` and `node`, the `retention` the closing turn was given, and one entry per
turn in `turns`, written by `llm.chat` as part of the record it saves - inside the compare-and-set, so a record and its
facts are one atomic write and a turn that lost the race leaves no facts behind:

```json
{"v": 2, "model": "claude-haiku-4-5", "turn": 2, "pending": [], "closed": true, "retention": 0,
 "correlation_id": "6f1c…", "graph_id": "order-assistant", "node": "assistant",
 "system": "You are …", "tools": [{"name": "order_lookup", "description": "…", "input_schema": {}, "strict": true}],
 "turns": [
   {"turn": 1, "trace_id": "3e9a…", "trace_path": "POST /api/graph/order-assistant", "correlation_id": "6f1c…",
    "started_at": "2026-11-17T01:02:03.456Z", "ended_at": "2026-11-17T01:02:05.012Z",
    "status": "tool_use", "stop_reason": "tool_use", "request_id": "req_…",
    "usage": {"input_tokens": 1180, "output_tokens": 64, "cache_creation_input_tokens": 0, "cache_read_input_tokens": 0},
    "messages": [0, 1]},
   {"turn": 2, "trace_id": "b07d…", "trace_path": "POST /api/graph/order-assistant", "correlation_id": "6f1c…",
    "started_at": "…", "ended_at": "…", "status": "done", "stop_reason": "end_turn", "request_id": "req_…",
    "usage": {"input_tokens": 1402, "output_tokens": 210, "cache_creation_input_tokens": 0, "cache_read_input_tokens": 1180},
    "messages": [2, 3]}
 ],
 "messages": ["…the provider-format messages, as today…"]}
```

`messages: [i, j]` are the indexes of the turn's request and response in `messages`, so a turn row is built without
assuming anything about alternation; `calls` are normalized from the response's `tool_use` blocks when the row is
written, not stored twice. The identity comes from the trace context the host gives every function (`trace_id`,
`trace_path`, `my_correlation_id`) and the graph context from the request's headers (section 8); the times are UTC
stamps taken around the model call. The facts add a few hundred bytes per turn to a record whose messages weigh
kilobytes; the system prompt and the catalog are stored once. `from_json` still reads a version 1 record, so a
conversation that was open when the helper was upgraded continues; its turns before the upgrade carry no facts and
are logged as unaudited by the check, the only time the audit is knowingly incomplete.

Beside `turn` and `record`, the hash gains a third field the audit function owns, `audited`: the highest turn the
audit store is known to hold. The compare-and-set script never touches it (it writes `turn` and `record` and sets
the TTL), so the two writers share a key and not a field. The memory store keeps the same three values per
conversation.

### 5.2 The function and its events

`transcript.audit` is a composable function of the helper's process: a private `preload` route, registered at boot
when `llm.transcript.audit` is configured, **with one instance**, so its mailbox is a queue the events leave in the
order they entered. It receives two kinds of local event, both sent by `llm.chat` through the pack's `PostOffice`
with no endpoint (in-process delivery over the primitive event bus, drop-n-forget, the caller's trace inherited so the
write shows in the trace as the function's own execution):

```text
{"conversation": "<reference>", "turn": N}        write - after every compare-and-set that accepted turn N
{"conversation": "<reference>", "check": true}    check - behind the write of a closing turn (section 5.3)
```

The write loads the working record, builds the row for turn N from `turns[N-1]` and `messages`, upserts the
conversation row and inserts the turn row in one transaction (section 6: both statements idempotent), and sets
`audited` to N when the store had every turn up to N. The event carries identity only; the content comes from the
record, which is the one place it lives, and the same code path serves the check. A Node helper and a Python helper
on one Redis write the same rows from the same record, as the loop contract requires of them.

Why a function with one worker and not a call inside `llm.chat`: the write leaves the turn's path (ruling R2), the
mailbox orders the writes and the check of a conversation without a lock, and a dedicated writer process is one
configuration change away if an installation ever wants the audit off the helper's replicas (a public route behind
Event-over-HTTP serves the same events; not built now). The throughput is not a concern: one event is a few
statements on an open pool, far below the rate at which model turns complete.

### 5.3 The integrity check and the release

The check is Eric's data-integrity step (notes, 2026-11-17). It loads the record and the set of turn numbers the audit
store holds for the conversation, writes every turn the store lacks from the record - the same rows the writes would
have written, in one transaction - and updates `audited`. For a closed record with every turn present it then
**releases** the working record: the TTL becomes the `retention` the closing turn carried (`0`: the record is deleted
at once). A check that finds the store unreachable leaves the record alone, logs the deferral, and the sweep repeats
it. An open record (a run waiting for a tool result, a person, or the loop's next turn) is never released by a check,
only filled.

The order is what makes the check sufficient: write(1) … write(N), check - one mailbox, one worker. The check sees the
store as it is after every write before it has run, so a count that matches means the audit is complete, and a
mismatch is a write that failed, which the check repairs from the record before anything is deleted.

### 5.4 The sweep (lost events, outages, runs the loop stopped)

Two cases escape the check: a conversation the loop stopped (a turn limit, a deadline) or never resumed, which has no
closing turn and so no check, and a process that died between a compare-and-set and the function's write. Neither
loses a turn - the facts are in the record - but each may leave `audited` behind `turn`. A sweep in the helper
(`llm.transcript.audit.sweep.ms`, default 60 s) scans the live records and sends a check event for every record
whose `audited` is below its `turn`; the function fills the gap, and releases it if it is closed. Up-to-date records
cost the sweep two hash fields each and no database query. On Redis the replicas share the work by a short lock
(`SET transcript:audit:sweep … NX PX <interval>`), so one sweep runs per interval; the memory store sweeps its own
process. A record that expires with `audited` below `turn` - an outage longer than the grace, or a stopped run whose
TTL ran out during one - is the one loss the design accepts, and the sweep logs each such gap it last saw.

### 5.5 Failure and logging

The function logs the route, the conversation reference, the turn numbers written or missing, the status and the
failure's type: never a prompt, a completion, an argument or a result, as `llm.chat` does today. A PostgreSQL that
does not answer at boot is logged and the function still registers, since the sweep repairs what the outage defers;
a configured driver that is not installed stops the application at boot by name, as the Redis extra does, because that
is a deployment error and not an outage.

## 6. The reference schema (PostgreSQL)

Published as `docs/guides/transcript-audit-schema.md` in this repository, and shipped as `schema/postgres.sql` in each
pack's `extensions/llm-helper/`. The names mirror the working store's key, `transcript:{conversation}`.

```sql
CREATE TABLE IF NOT EXISTS transcript_schema (
  version     INTEGER PRIMARY KEY,
  applied_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO transcript_schema (version) VALUES (1) ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS transcript (
  conversation                CHAR(32)     PRIMARY KEY,
  correlation_id              TEXT,
  graph_id                    TEXT,
  node                        TEXT,
  model                       TEXT         NOT NULL,
  status                      TEXT         NOT NULL CHECK (status IN ('open','done','refusal','cap','expired')),
  turns                       INTEGER      NOT NULL,
  started_at                  TIMESTAMPTZ  NOT NULL,
  updated_at                  TIMESTAMPTZ  NOT NULL,
  ended_at                    TIMESTAMPTZ,
  expires_at                  TIMESTAMPTZ,
  input_tokens                BIGINT       NOT NULL DEFAULT 0,
  output_tokens               BIGINT       NOT NULL DEFAULT 0,
  cache_creation_input_tokens BIGINT       NOT NULL DEFAULT 0,
  cache_read_input_tokens     BIGINT       NOT NULL DEFAULT 0,
  system                      TEXT,
  tools                       JSONB
);
CREATE INDEX IF NOT EXISTS transcript_correlation_id ON transcript (correlation_id);
CREATE INDEX IF NOT EXISTS transcript_started_at     ON transcript (started_at);
CREATE INDEX IF NOT EXISTS transcript_expires_at     ON transcript (expires_at);

CREATE TABLE IF NOT EXISTS transcript_turn (
  conversation                CHAR(32)     NOT NULL REFERENCES transcript (conversation) ON DELETE CASCADE,
  turn                        INTEGER      NOT NULL CHECK (turn >= 1),
  trace_id                    TEXT,
  trace_path                  TEXT,
  correlation_id              TEXT,
  started_at                  TIMESTAMPTZ  NOT NULL,
  ended_at                    TIMESTAMPTZ  NOT NULL,
  stop_reason                 TEXT,
  status                      TEXT         NOT NULL CHECK (status IN ('done','tool_use','refusal','cap')),
  request_id                  TEXT,
  input_tokens                INTEGER      NOT NULL DEFAULT 0,
  output_tokens               INTEGER      NOT NULL DEFAULT 0,
  cache_creation_input_tokens INTEGER      NOT NULL DEFAULT 0,
  cache_read_input_tokens     INTEGER      NOT NULL DEFAULT 0,
  request                     JSONB        NOT NULL,
  response                    JSONB        NOT NULL,
  calls                       JSONB,
  PRIMARY KEY (conversation, turn)
);
CREATE INDEX IF NOT EXISTS transcript_turn_trace_id       ON transcript_turn (trace_id);
CREATE INDEX IF NOT EXISTS transcript_turn_correlation_id ON transcript_turn (correlation_id);
CREATE INDEX IF NOT EXISTS transcript_turn_started_at     ON transcript_turn (started_at);
```

**Writes** are idempotent, so a repeated write or a check that re-sends a row cannot duplicate it: the turn row is
`INSERT ... ON CONFLICT (conversation, turn) DO NOTHING`; the conversation row is `INSERT ... ON CONFLICT
(conversation) DO UPDATE` of `status`, `turns`, `updated_at`, `ended_at` and the token totals (the totals are
recomputed from the turn rows, never added blindly, so a replayed write stays exact). Both statements run in one
transaction per event.

**Status.** A conversation is `open` while its last turn is `tool_use`; the model's end sets `done`, `refusal` or
`cap` (`cap` also for `max_tokens`, which closes the working record). `expired` is set by the installation's purge
(below) for an `open` row whose `updated_at` is older than the working store's TTL: the run was stopped by the loop or
abandoned, and its working record is gone.

**Retention and purge are the installation's (ruling R6).** `expires_at` is written as `started_at` plus the
configured audit retention (`llm.transcript.audit.retention`, default `365d`). The helper runs no purge. Each pack
ships `schema/purge.sql`, the two statements a purge is, and `dev/audit-purge.sh`, a sample script a developer runs
against the development server by hand (section 9); an installation's operations automate the same statements on
its own schedule and policy - a job on `expires_at`, or `transcript_turn` partitioned by month with partitions
dropped - and the helper does not need to know which.

```sql
-- schema/purge.sql: safe to repeat; the turns cascade with their conversation
DELETE FROM transcript WHERE expires_at < now();
UPDATE transcript SET status = 'expired' WHERE status = 'open' AND updated_at < now() - interval '7 days';
```

**Roles.** The audit function connects as a writer that may `INSERT`, `SELECT` and `UPDATE` the two tables and
nothing else; people and the SQL browser connect as a reader with `SELECT` only. TLS is on by default
(`llm.transcript.audit.postgres.ssl`).

**Another store.** An installation that implements the contract elsewhere keeps the two tables (or their equivalent),
the key semantics (one conversation row, one turn row per turn, idempotent writes, the set of turn numbers a
conversation holds answerable for the check), the status values and the three indexes (correlation id, trace id,
time). The record versions are `transcript_schema.version` here and the `v` field of the working record; the JSON
columns carry the provider's format unchanged.

**The questions it answers.** By transaction, by request, by time, and the transcript itself:

```sql
-- every turn of a transaction, suspend and resume included (the same correlation id, different trace ids)
SELECT t.conversation, t.turn, t.trace_id, t.started_at, t.status, t.stop_reason
FROM transcript_turn t WHERE t.correlation_id = $1 ORDER BY t.started_at;

-- the turns one request ran (one trace id)
SELECT conversation, turn, status, started_at, ended_at FROM transcript_turn WHERE trace_id = $1 ORDER BY turn;

-- what ran between two instants, with the graph and node
SELECT c.conversation, c.graph_id, c.node, c.status, c.turns, c.started_at, c.ended_at
FROM transcript c WHERE c.started_at >= $1 AND c.started_at < $2 ORDER BY c.started_at;

-- the whole transcript of a conversation, as the model exchanged it
SELECT c.system, c.tools, t.turn, t.request, t.response, t.calls
FROM transcript c JOIN transcript_turn t USING (conversation) WHERE c.conversation = $1 ORDER BY t.turn;
```

## 7. Configuration (the helper, both packs)

The keys follow the working store's set, under the audit's own prefix:

| Key | Default | For |
| --- | --- | --- |
| `llm.transcript.audit` | unset | `postgres`; unset means no audit store, no audit function, the helper as today |
| `llm.transcript.audit.retention` | `365d` | written as `expires_at`; the purge is the installation's |
| `llm.transcript.audit.grace` | `1d` | how long a closed working record waits for its check before it expires unreleased; the check releases it earlier |
| `llm.transcript.audit.sweep.ms` | `60000` | the sweep's interval; `0` disables it |
| `llm.transcript.audit.postgres.host`, `.port`, `.database`, `.username`, `.password`, `.ssl`, `.timeout.ms` | `127.0.0.1`, `5432`, `mercury`, none, none, `true`, `5000` | the connection; the password from the environment (`${POSTGRES_PASSWORD}`), never in a file |

The driver is an optional extra, as Redis is (ruling R4): Python `postgres = ["psycopg[binary,pool]>=3.2,<4"]`
(asynchronous, JSONB without ceremony, no compiler); Node `pg` as a dev dependency beside `redis`, so the published
package stays free of any database client. A configured audit store whose driver is not installed stops the
application at boot by name; one that does not answer at boot is logged, and the sweep repairs what the outage defers.

## 8. Graph context (the engines, in this build - ruling R5)

The loop forwards nothing but the correlation id and the return path to `llm.chat` today, and `graph.task` sets only
`x-return-path` on a task's request (an author may map constants to headers, `-> header.x-...`, but an audit must not
depend on an author remembering to). Two small changes, in Java and Rust in lockstep:

1. `graph.task` sets `x-graph-id` (the graph's id) and `x-node` (the node's alias) on every request it sends, beside
   `x-return-path`. A function that does not read them is unaffected; the pack's host already passes a caller's
   `x-` headers through.
2. The loop copies both onto each `llm.chat` request's headers. The helper records them in the record at the first
   turn, and the conversation row carries them.

A caller that sends neither (a direct call, an older engine) leaves the columns null, and the trace path still names
the entry that started the transaction (for a Playground run, `POST /api/graph/<graph id>`).

## 9. Development and test setup

Colima runs the containers on a developer's machine (it is installed here; the Compose plugin is not, so the scripts
use `docker run`). Three scripts in each pack's `extensions/llm-helper/dev/`:

```bash
# postgres-up.sh: PostgreSQL 17 with the schema applied at first start, and pgweb as the SQL browser (ruling R7)
docker run -d --name mercury-transcripts -p 5432:5432 \
  -e POSTGRES_DB=mercury -e POSTGRES_USER=mercury -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD:?set it}" \
  -v "$(dirname "$0")/../schema/postgres.sql:/docker-entrypoint-initdb.d/transcript.sql:ro" \
  postgres:17-alpine
docker run -d --name mercury-transcripts-browser -p 8081:8081 \
  sosedoff/pgweb --url "postgres://mercury:${POSTGRES_PASSWORD}@host.docker.internal:5432/mercury?sslmode=disable"

# audit-purge.sh: the sample purge (ruling R6) - psql inside the container, since none is installed on the host.
#   --dry-run   counts what would go;  --before <ISO date>   also removes conversations started before it
docker exec -i mercury-transcripts psql -U mercury -d mercury -v ON_ERROR_STOP=1 < "$(dirname "$0")/../schema/purge.sql"
```

`postgres-down.sh` stops and removes both containers. The helper then runs with `-Dllm.transcript.store=redis
-Dllm.transcript.audit=postgres -Dllm.transcript.audit.postgres.ssl=false`, and the browser at `http://127.0.0.1:8081`
runs the queries of section 6: by correlation id, by trace id, by time range. pgweb is a single Go binary with no
dependencies and a query box, which is all the setup needs. The Playground's `order-assistant` graph is the natural
proof: one transaction, a suspend for the refund, a resume under a new trace id, one correlation id across both halves
in the browser, and the Redis key gone once the check has run.

Tests: the unit tests run on the memory store and on a recording fake of the PostgreSQL connection that pins the
statements and their parameters, and they drive the function's mailbox directly - a write that fails, then a check
that repairs it, then the release; a sweep over a record whose `audited` lags. The live test runs when
`LLM_TRANSCRIPT_AUDIT_TEST_URL` is set and is otherwise skipped, as the Redis live test is. The shared loop vector file
gains no case: the audit is a side effect of a turn, not part of the loop contract the twins repeat; the twins are
held to one schema file (byte-identical `postgres.sql` and `purge.sql`) and to the version 2 record, which the
existing vectors' store fakes read.

## 10. Security and governance

- **Content.** The audit store holds everything telemetry refuses to: prompts, completions, tool arguments and
  results. It is personal data until proven otherwise. The installation sets the retention, the roles and the TLS; the
  function writes with the least privilege (section 6) and logs nothing of the content.
- **The engine stays blind.** Nothing of this paper puts a transcript through an engine codec or an engine log; the
  engine contributes two identity headers (section 8).
- **Integrity.** A turn's facts are saved inside the compare-and-set that accepts the turn, so the audit never records
  a turn the conversation did not take; writes are idempotent; one function with one worker orders the writes and the
  check of a conversation; the check repairs before the release deletes; the conversation row's totals are recomputed
  from the turn rows.
- **Access to the browser** is the reader role over TLS, for development the local container only. A production
  installation puts the browser, if any, behind its own access control; the schema does not care which browser.
- **What the audit does not do.** It does not redact. A redaction policy (Q4's second half) is applied by the
  installation on read, or by a later writer option; the first build records what the model exchanged, because an
  audit that edits its own record is not one.

## 11. Rulings (Eric, 2026-11-17, on round 1)

- **R1 — when.** Option A: every turn is recorded, asynchronously (section 4).
- **R2 — failure mode.** Decoupled. A failed audit write is logged; it does not block, because the write is
  asynchronous (section 4; the `required`/`best-effort` switch is withdrawn).
- **R3 — `retention`.** Confirmed as a working-store setting. Eric: there is in fact not much reason to keep the
  Redis record after the turns are complete - hence the release after the check (section 4, section 5.3).
- **R4 — the store.** Agreed: PostgreSQL first, psycopg 3 in Python and `pg` in Node, as optional extras.
- **R5 — graph context.** Agreed: record the graph context. The two headers are part of this build (section 8).
- **R6 — purge.** A sample script for the developer to purge the audit database; operations automate the purge on
  the enterprise's own policy (section 6, section 9).
- **R7 — the browser.** pgweb.
- **R8 — the first-turn snapshot.** Agreed: the system prompt and the tools once on the conversation row.

**Eric's notes, applied in section 5.** The write is an asynchronous, decoupled write by a composable function: a
local event to the function persists a record to the database. For data integrity, the number of turns for the
conversation is counted and compared between Redis and PostgreSQL, and the missing records are persisted. At the end
of the turns, the data-integrity check is itself an in-memory event to the same function, so that it executes
sequentially after the writes, with no race and no eventual-consistency issue from the asynchronous writes; the
deletion of the Redis records for the conversation is deferred until after the check.

## 12. Defaults set without a ruling (for round 2)

Each follows from the rulings; none was ruled on by name, so each is open to change:

1. **One instance** for `transcript.audit`: the sequencing the notes ask for is the mailbox's; a sharded variant
   (one instance per hash of the conversation) is the extension if the rate ever asks for it.
2. **The record's facts live in the working record** (version 2) rather than in a second key, so a record and its
   facts are one compare-and-set and the check reads one thing; `audited` is a third hash field beside `turn` and
   `record`.
3. **`llm.transcript.audit.grace` defaults to `1d`** - the outage the design rides through before a closed record is
   lost. A closed record is released at the check in the normal case, so the grace costs nothing then.
4. **The sweep** (`llm.transcript.audit.sweep.ms`, 60 s, one sweep per interval on Redis by a short lock) covers the
   runs the loop stopped and the process that died; it is the only part of the design that was not in the notes.
5. **A check never releases an open record**, since a suspended run may resume; only a closed one.

## 13. Build plan

1. **Python helper:** the version 2 record and its facts in `_loop_turn`, the store protocol's audit half (`audited`,
   the release, the scan), the `transcript.audit` function with its two events and the sweep, the PostgreSQL
   implementation and the `postgres` extra, the configuration keys, `schema/postgres.sql` and `schema/purge.sql`, the
   `dev/` scripts, the README section, the unit tests and the gated live test. The schema specification page here,
   `docs/guides/transcript-audit-schema.md`.
2. **Node helper in lockstep:** the same record, function, events and sweep; `pg`; the byte-identical schema and
   purge files; the same scripts and tests.
3. **The engines (R5):** `x-graph-id` and `x-node` from `graph.task`, copied by the loop onto `llm.chat`; Java and
   Rust, with the Playground's tests reading them back through a scripted `llm.chat`.
4. **The drive:** `order-assistant` with a suspend and a resume through the Python helper and then the Node helper on
   Colima's PostgreSQL; the browser shows one correlation id, two trace ids, every turn, and the Redis key released;
   a write failure injected once to watch the check repair it; the report under `docs/test-reports/`. Then promotion
   of RFC-0012 is Eric's call.
