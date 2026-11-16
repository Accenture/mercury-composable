# The transcript audit store — a durable, queryable record of every model turn — design spec

**Status:** DRAFT for review, round 1 (2026-11-16, Claude Code for Eric). Raised as RFC-0012 in
[`docs/arch-decisions/RFC.md`](../docs/arch-decisions/RFC.md); the rulings this paper needs are listed in section 11.
**Serves:** `bp-agent-orchestration` → `vision-mercury-composable` (Track 2, the graph as AI SDLC).
**Extends:** [`llm-helper-agent-loop.md`](./llm-helper-agent-loop.md) section 3.3 (the transcript store) and section 10
(telemetry, audit and security); answers the concept's Q4 (audit capture) in
[`ai-agent-orchestration.md`](./ai-agent-orchestration.md) section 6.
**ADR-0033 stands:** the transcript lives on the pack's side and never crosses an engine codec. This paper adds a second,
durable store beside the working one; it does not move the transcript.
**Repo scope:** the packs' LLM helper (mercury-python first, mercury-nodejs in lockstep): the writer, the PostgreSQL
implementation, the development setup. Here: the published schema specification under `docs/`, and a small engine
change in both engines (section 8) as a separate build step.

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

## 2. Principles

1. **The engine never holds a transcript** (ADR-0033). The pack's helper is the only writer of the audit store, as it is
   of the working store. The engine's loop contributes identity, never content (section 8).
2. **Two stores, two jobs.** The working store stays as built: Redis or memory, the compare-and-set, the TTL as the
   transaction's expiry. The audit store is durable, append-only, indexed for people, and keeps records on a policy the
   installation sets, independent of the transaction.
3. **A published contract, one reference implementation.** PostgreSQL is the reference store, shipped in both packs.
   The contract is the record (section 3) and the reference schema (section 5), published as a specification, so an
   installation that runs another database implements the same tables and the same semantics behind the same
   interface, and the helper's writer stays one.
4. **Content lives where the installation governs it.** The audit store holds prompts, completions and tool
   arguments, so it may hold personal data. It is the installation's governed database, with roles, TLS and a
   retention policy, never a file in a folder and never a log line.
5. **The record is the provider's format, as sent and as returned.** What the model saw and what it answered are
   stored as the content blocks the API exchanged, not a paraphrase, so an audit can reconstruct the exact turn. The
   normalized calls (`{id, name, input}`) are stored beside them for the person who queries, not instead of them.

## 3. What is recorded

Two kinds of row, so that the natural questions each hit one index.

**A conversation** (one row per reference `llm.chat` returned):

```text
conversation        the reference (32 hex characters), the key of the working-store record
correlation_id      the business correlation id of the transaction (my_correlation_id): the same across a graph's
                    suspend and resume, so one value finds every turn of a transaction
graph_id, node      the graph and the loop node, when the caller passes them (section 8); null until then
model               the model pinned at the first turn
status              open | done | refusal | cap | expired (section 4)
turns               the turn count so far
started_at          UTC, the first turn's start
updated_at          UTC, the last write
ended_at            UTC, when the model ended the run (null while open)
input_tokens, output_tokens, cache_creation_input_tokens, cache_read_input_tokens   totals
system              the system prompt, as sent on the first turn
tools               the catalog as advertised to the model on the first turn (name, description, input_schema, strict)
expires_at          UTC, started_at plus the installation's audit retention: a hint for the purge (section 5)
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
system prompt and the tools are on the conversation row, since the loop sends them unchanged on every turn.

## 4. When the audit is written

Three shapes were weighed.

| | A. Every turn (recommended) | B. At the end of the run | C. Every turn, plus a close message |
| --- | --- | --- | --- |
| What is written | the turn row after each model turn; the conversation row upserted | the whole record once, when the run ends | A, and the loop's own reason when it stops the run |
| A run that ends normally | complete | complete | complete |
| A run the loop stops (turn limit, deadline) or never resumes | complete: every turn that ran is there; status stays `open` until the TTL, then `expired` by the sweeper of section 5 | **lost**: the helper never learns that the loop stopped, so the record expires unaudited | complete, with the reason |
| A process that dies mid-run | every finished turn is there | lost | as A |
| Cost | one small write per turn, after the model call | one write per run | A plus one event per run |
| Engine change | none | a close contract, or an end-of-run event (the helper cannot know the loop stopped) | the close contract |

**Recommendation: A.** It needs no engine contract, it is complete under the failures an audit exists for, and the
per-turn row is what the identity asks for anyway: each turn carries its own trace id, which is how a resume shows up
as a different trace under the same correlation id. The one thing A does not record is *why* a run that the loop
stopped ended, because the loop decides that after the last turn; the turns themselves are all there. C adds it later
without changing A: the loop sends a close event (`conversation`, the outcome, the turn count) to a route the helper
serves, and the helper stamps the conversation row. It is proposed as a later step, not for the first build.

**Order of writes.** The working store's compare-and-set runs first; the audit row is written only for a turn that
won it, so a duplicated turn that lost the race never reaches the audit. **Failure mode:** `llm.transcript.audit.mode`
is `required` (the default when a store is configured: a turn whose audit cannot be written is refused with 503 after
the model call, named by the key, and the loop fails the node, so an unaudited turn never enters a graph that asked
for audit) or `best-effort` (the failure is logged without content and the turn proceeds; for development). With no
audit store configured, nothing is written and the helper behaves as today.

**Retention in Redis once audited.** `retention` keeps its meaning (how long the working record lingers after the
model ends the run) and its default `0`. With an audit store, there is no reason to raise it for audit's sake; the
long-term copy is the audit's. Ruling R3 asks whether the engine's `llm.run.transcript.retention` default and the
node's `retention` input should be documented as working-store settings only, which this paper proposes.

## 5. The reference schema (PostgreSQL)

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

**Writes** are idempotent, so a retried turn cannot duplicate a row: the turn row is `INSERT ... ON CONFLICT
(conversation, turn) DO NOTHING`; the conversation row is `INSERT ... ON CONFLICT (conversation) DO UPDATE` of
`status`, `turns`, `updated_at`, `ended_at` and the token totals (the totals are recomputed from the turn rows, never
added blindly, so a replayed write stays exact). Both statements run in one transaction per turn.

**Status.** A conversation is `open` while its last turn is `tool_use`; the model's end sets `done`, `refusal` or
`cap` (`cap` also for `max_tokens`, which closes the working record). `expired` is set by the installation's sweeper
(below) for an `open` row whose `updated_at` is older than the working store's TTL: the run was stopped by the loop or
abandoned, and its working record is gone.

**Retention and purge** are the installation's: `expires_at` is written as `started_at` plus the configured audit
retention (`llm.transcript.audit.retention`, default `365d`), and a scheduled job runs

```sql
DELETE FROM transcript WHERE expires_at < now();   -- the turns cascade
UPDATE transcript SET status = 'expired' WHERE status = 'open' AND updated_at < now() - interval '7 days';
```

or partitions `transcript_turn` by month and drops partitions. The helper runs no sweeper of its own in the first
build (ruling R6).

**Roles.** The helper connects as a writer that may `INSERT` and `UPDATE` the two tables and nothing else; people and
the SQL browser connect as a reader with `SELECT` only. TLS is on by default (`llm.transcript.audit.postgres.ssl`).

**Another store.** An installation that implements the contract elsewhere keeps the two tables (or their equivalent),
the key semantics (one conversation row, one turn row per turn, idempotent writes), the status values and the three
indexes (correlation id, trace id, time). The record versions are `transcript_schema.version` here and the `v` field
of the working record; the JSON columns carry the provider's format unchanged.

**The questions it answers.** By transaction, by request, by time, and the transcript itself:

```sql
-- every turn of a transaction, suspend and resume included (the same correlation id, different trace ids)
SELECT t.conversation, t.turn, t.trace_id, t.started_at, t.status, t.stop_reason
FROM transcript_turn t WHERE t.correlation_id = $1 ORDER BY t.started_at;

-- the turns one request ran (one trace id)
SELECT conversation, turn, status, started_at, ended_at FROM transcript_turn WHERE trace_id = $1 ORDER BY turn;

-- what ran between two instants, with the graph and node when recorded
SELECT c.conversation, c.graph_id, c.node, c.status, c.turns, c.started_at, c.ended_at
FROM transcript c WHERE c.started_at >= $1 AND c.started_at < $2 ORDER BY c.started_at;

-- the whole transcript of a conversation, as the model exchanged it
SELECT c.system, c.tools, t.turn, t.request, t.response, t.calls
FROM transcript c JOIN transcript_turn t USING (conversation) WHERE c.conversation = $1 ORDER BY t.turn;
```

## 6. Configuration (the helper, both packs)

The keys follow the working store's set, under the audit's own prefix:

| Key | Default | For |
| --- | --- | --- |
| `llm.transcript.audit` | unset | `postgres`; unset means no audit store, the helper as today |
| `llm.transcript.audit.mode` | `required` | `required` (a turn whose audit fails is a 503) or `best-effort` (logged, the turn proceeds) |
| `llm.transcript.audit.retention` | `365d` | written as `expires_at`; the purge is the installation's |
| `llm.transcript.audit.postgres.host`, `.port`, `.database`, `.username`, `.password`, `.ssl`, `.timeout.ms` | `127.0.0.1`, `5432`, `mercury`, none, none, `true`, `5000` | the connection; the password from the environment (`${POSTGRES_PASSWORD}`), never in a file |

The driver is an optional extra, as Redis is: Python `postgres = ["psycopg[binary,pool]>=3.2,<4"]` (asynchronous,
JSONB without ceremony, no compiler); Node `pg` as a dev dependency beside `redis`, so the published package stays
free of any database client. A configured audit store whose driver is not installed is a 503 that names the extra; one
that does not answer at boot is logged, and the first turn that needs it fails as `mode` says.

## 7. The writer, in the helper

`AuditStore` is the second protocol beside `TranscriptStore`:

```python
class AuditStore(Protocol):
    name: str
    async def record(self, conversation: ConversationRow, turn: TurnRow) -> None: ...   # one transaction
```

`_loop_turn` builds the rows after the working store's save returns true: the identity from the trace context the host
gives every function (`trace_id`, `trace_path`, `my_correlation_id`), the graph context from the event's headers when
present (section 8), the times it measured around the model call, the message it sent (`messages[-1]`: the prompt or
the tool-result message), the content it stored (`done.content`), the normalized calls, the usage, and the system
prompt and tools on the first turn. The PostgreSQL implementation holds one connection pool, writes the two statements
in one transaction, and raises a typed failure that `_loop_turn` maps per `mode`. Nothing is logged but the status and
the conversation reference, as today: no prompt, completion, argument or result reaches a log line.

The first build ships `postgres`. A `memory` audit store exists for tests only: it keeps the rows in a list the tests
read, so the unit tests pin the exact rows without a database, and one live test, gated by an environment variable as
the Redis live test is, runs the real statements against the development server of section 9.

## 8. Graph context (the engines, a separate step)

The loop forwards nothing but the correlation id and the return path to `llm.chat` today, and `graph.task` sets only
`x-return-path` on a task's request (an author may map constants to headers, `-> header.x-...`, but an audit must not
depend on an author remembering to). Two small changes, in Java and Rust in lockstep:

1. `graph.task` sets `x-graph-id` (the graph's id) and `x-node` (the node's alias) on every request it sends, beside
   `x-return-path`. A function that does not read them is unaffected; the pack's host already passes a caller's
   `x-` headers through.
2. The loop copies both onto each `llm.chat` request's headers. The helper writes them to the conversation row.

Until then the columns are null and the trace path still names the entry that started the transaction (for a
Playground run, `POST /api/graph/<graph id>`), so a person can usually tell the graph from the path alone. The change is
cheap and the record is better for it; ruling R5 asks whether it joins the first build.

## 9. Development and test setup

Colima runs the containers on a developer's machine (it is installed here; the Compose plugin is not, so the scripts
use `docker run`). Two scripts in each pack's `extensions/llm-helper/dev/`:

```bash
# postgres-up.sh: PostgreSQL 17 with the schema applied at first start, and pgweb as the SQL browser
docker run -d --name mercury-transcripts -p 5432:5432 \
  -e POSTGRES_DB=mercury -e POSTGRES_USER=mercury -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD:?set it}" \
  -v "$(dirname "$0")/../schema/postgres.sql:/docker-entrypoint-initdb.d/transcript.sql:ro" \
  postgres:17-alpine
docker run -d --name mercury-transcripts-browser -p 8081:8081 \
  sosedoff/pgweb --url "postgres://mercury:${POSTGRES_PASSWORD}@host.docker.internal:5432/mercury?sslmode=disable"
```

`postgres-down.sh` stops and removes both. The helper then runs with `-Dllm.transcript.store=redis
-Dllm.transcript.audit=postgres -Dllm.transcript.audit.postgres.ssl=false`, and the browser at `http://127.0.0.1:8081`
runs the queries of section 5: by correlation id, by trace id, by time range. pgweb is a single Go binary with no
dependencies and a query box, which is all the setup needs; Adminer is the alternative if a form-based browser is
preferred (ruling R7). The Playground's `order-assistant` graph is the natural proof: one transaction, a suspend for
the refund, a resume under a new trace id, and one correlation id across both halves in the browser.

Tests: the unit tests run on the memory audit store and on a recording fake of the PostgreSQL connection that pins
the statements and their parameters; the live test runs when `LLM_TRANSCRIPT_AUDIT_TEST_URL` is set and is otherwise
skipped, as the Redis live test is. The shared vector file gains no case: the audit is a side effect of a turn, not
part of the loop contract the twins repeat; the twins are held to one schema file instead (byte-identical `postgres.sql`).

## 10. Security and governance

- **Content.** The audit store holds everything telemetry refuses to: prompts, completions, tool arguments and
  results. It is personal data until proven otherwise. The installation sets the retention, the roles and the TLS; the
  helper writes with the least privilege (section 5) and logs nothing of the content.
- **The engine stays blind.** Nothing of this paper puts a transcript through an engine codec or an engine log; the
  engine contributes two identity headers (section 8).
- **Integrity.** The audit row is written after the working store's compare-and-set, so the audit never records a
  turn the conversation did not take; writes are idempotent; the conversation row's totals are recomputed from the
  turn rows.
- **Access to the browser** is the reader role over TLS, for development the local container only. A production
  installation puts the browser, if any, behind its own access control; the schema does not care which browser.
- **What the audit does not do.** It does not redact. A redaction policy (Q4's second half) is applied by the
  installation on read, or by a later writer option; the first build records what the model exchanged, because an
  audit that edits its own record is not one.

## 11. Rulings needed (Eric)

- **R1 — when.** Every turn (A, recommended), at the end of the run (B), or A now and the close event (C) later?
- **R2 — failure mode.** `required` as the default when an audit store is configured, `best-effort` for development?
- **R3 — `retention`.** Keep `retention` and `llm.run.transcript.retention` as working-store settings with the default
  `0`, documented as such, and let the audit store carry the long-term copy?
- **R4 — the store.** PostgreSQL first, psycopg 3 in Python and `pg` in Node, as optional extras?
- **R5 — graph context.** `x-graph-id` and `x-node` from `graph.task` and the loop (Java and Rust) in this build, or
  a later step?
- **R6 — purge.** The installation's job on `expires_at`, with `365d` as the written default, and no sweeper in the
  helper for now?
- **R7 — the browser.** pgweb (one binary, a query box) or Adminer for the development setup?
- **R8 — the first-turn snapshot.** The system prompt and the tools stored once on the conversation row (they are
  re-sent unchanged each turn), rather than per turn?

## 12. Build plan

1. **Python helper:** the `AuditStore` protocol, the memory and PostgreSQL implementations, the `postgres` extra, the
   configuration keys, the write in `_loop_turn`, `schema/postgres.sql`, the `dev/` scripts, the README section, the
   unit tests and the gated live test. The schema specification page here, `docs/guides/transcript-audit-schema.md`.
2. **Node helper in lockstep:** the same protocol, `pg`, the byte-identical schema file, the same scripts and tests.
3. **The engines (R5):** `x-graph-id` and `x-node` from `graph.task`, copied by the loop onto `llm.chat`; Java and Rust.
4. **The drive:** `order-assistant` with a suspend and a resume through the Python helper and then the Node helper on
   Colima's PostgreSQL; the browser shows one correlation id, two trace ids, every turn; the report under
   `docs/test-reports/`. Then promotion of RFC-0012 is Eric's call.
