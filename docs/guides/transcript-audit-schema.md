---
title: Transcript Audit Schema
summary: The published contract of the transcript audit store - the durable, queryable record of every
  model turn an agent loop takes, written by the language packs' LLM helper beside its working store,
  so an installation can answer "what was the model told, what did it say, what did it call" by
  transaction, by request and by time, and implement the same contract on its own database.
layer: operate
audience: [architect, developer, operator]
keywords: [transcript audit, audit store, agent loop, llm.chat, transcript.audit, postgresql, schema,
  correlation id, trace id, retention, purge, RFC-0012]
---

# Transcript Audit Schema

*Reference: The two tables, the write semantics and the questions they answer - the contract the LLM
helper writes and an installation may re-implement on another database.*

> **At a glance**
>
> - **What** — a conversation row per agent-loop run and a turn row per model turn, with the
>   prompt, the tool results, the assistant's content blocks and the normalized tool calls in the
>   provider's format, indexed by correlation id, trace id and time. PostgreSQL is the reference
>   store, shipped with the Python and Node.js LLM helpers as `extensions/llm-helper/schema/postgres.sql`.
> - **Why it matters** — the helper's transcript store is a *working* store: its time to live is
>   the transaction's, and the record goes when the run ends. Telemetry carries no content by rule.
>   The audit store is the one place a person can ask what a model exchanged, under the
>   installation's own retention, roles and TLS.
> - **For** architects and operators who run agent loops in production, and anyone implementing the
>   contract on a database other than PostgreSQL.
> - **Versions** — record version 2 of the helper's working store and schema version 1
>   (mercury-python PR #59; the Node.js twin follows). Settled as RFC-0012 in the
>   [RFC register](../arch-decisions/RFC.md).

## Two stores, two jobs

An agent loop (`llm.run` in the engines) calls the language pack's `llm.chat` once per model turn and
keeps only a reference to the conversation; the transcript itself never crosses an engine codec
(ADR-0033). The helper keeps it in a **working store** - memory, or Redis shared by replicas - written
with a compare-and-set on the turn count and expiring with the transaction. That is the right shape
for the transaction and the wrong one for an audit.

The **audit store** is the second store beside it: durable, append-only, indexed for people, and kept
on a policy the installation sets, independent of the transaction. The helper is its only writer. After
the working store's compare-and-set has accepted a turn, `llm.chat` sends one local event to
`transcript.audit`, a composable function of the helper's own process with one instance, so its
mailbox orders the writes of a conversation and the check that ends it without a lock. The function
reads the working record - the one place the content lives - and writes the rows the audit store
lacks. A failed write is logged by type and never refuses a turn; the end of a run is an integrity
check that counts the turns the record holds against the rows the store holds, writes the missing
ones, and only then releases the working record. A sweep sends the same check for every record whose
audit lags, for the runs the loop stopped and the writes a dying process lost.

The engine contributes identity, never content: the loop sends the graph's id and the node's alias as
the `x-graph-id` and `x-node` headers of each `llm.chat` request (RFC-0012, section 8), and the helper
records them at the first turn. A caller that sends neither - a direct call, an older engine - leaves
the two columns null; the trace path still names the entry that started the transaction.

## What is recorded

**A conversation** - one row per reference `llm.chat` returned:

| Column | Meaning |
|:--|:--|
| `conversation` | the reference, 32 hex characters: the key of the working-store record (`transcript:{conversation}`) |
| `correlation_id` | the business correlation id of the transaction (`my_correlation_id`): the same across a graph's suspend and resume, so one value finds every turn of a transaction |
| `graph_id`, `node` | the graph and the loop node, from the two headers above; null when a caller sends none |
| `model` | the model pinned at the first turn |
| `status` | `open` while the last turn asked for tools; `done`, `refusal` or `cap` when the model ended the run (`cap` also for a turn cut off at `max_tokens`); `expired`, set by the installation's purge |
| `turns` | the turn count so far |
| `started_at`, `updated_at`, `ended_at` | UTC: the first turn's start, the last write, the model's end of the run (null while open) |
| `expires_at` | UTC: `started_at` plus the helper's audit retention (`llm.transcript.audit.retention`, default `365d`) - a hint for the purge |
| `input_tokens`, `output_tokens`, `cache_creation_input_tokens`, `cache_read_input_tokens` | totals, recomputed from the turn rows on every write |
| `system` | the system prompt as sent on the first turn |
| `tools` | the catalog as advertised to the model on the first turn: `[{name, description, input_schema, strict}]` |

**A turn** - one row per model turn of the conversation:

| Column | Meaning |
|:--|:--|
| `conversation`, `turn` | the key; `turn` counts from 1 |
| `trace_id`, `trace_path` | the distributed trace of the event that carried this turn - a resume runs under a new one, so a trace id finds the turns of one request and a correlation id finds them all |
| `correlation_id` | repeated here, so a query by correlation id needs no join |
| `started_at`, `ended_at` | UTC, the model call's bounds |
| `stop_reason` | the provider's stop reason as returned |
| `status` | `done`, `tool_use`, `refusal` or `cap` |
| `request_id` | the provider's request id, when it returned one |
| `input_tokens`, `output_tokens`, `cache_creation_input_tokens`, `cache_read_input_tokens` | the turn's usage |
| `request` | the messages this turn sent before the model answered, as a JSON array: the prompt on the first turn (every message of a conversation history, when one was sent), or the one user message of `tool_result` blocks that answered the previous turn's calls (`is_error` on a failure) |
| `response` | the assistant's content blocks as returned - text, `tool_use`, thinking - exactly as the working record holds them, as a JSON array |
| `calls` | `[{id, name, input}]`, normalized from the response's `tool_use` blocks when `status` is `tool_use`; null otherwise |

The tool **results** are the next turn's `request`: its `tool_result` blocks carry `tool_use_id`, which
joins to the previous turn's `calls`. The transcript of a conversation is its turns in order, `request`
then `response` each; the system prompt and the tools sit once on the conversation row, since the loop
sends them unchanged on every turn. The two JSON columns keep constant shapes - an array of messages and
an array of blocks - so a query never branches on a turn's kind, and they carry the provider's format
unchanged, so an audit can reconstruct the exact exchange.

## The reference schema (PostgreSQL)

Shipped as `extensions/llm-helper/schema/postgres.sql` in each language pack, byte-identical, and
applied by the development setup at the container's first start.

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

## Write semantics

**Idempotent writes.** A repeated write, or a check that re-sends a row, cannot duplicate it: the turn
row is `INSERT ... ON CONFLICT (conversation, turn) DO NOTHING`; the conversation row is `INSERT ... ON
CONFLICT (conversation) DO UPDATE` of `status`, `turns`, `updated_at` and `ended_at`, and its token totals
are then recomputed from the turn rows by one `UPDATE`, never added blindly, so a replayed write stays
exact. The three statements run in one transaction per event, and JSON travels as text cast to `jsonb`,
UTC stamps as ISO-8601 text cast to `timestamptz`, so every pack runs the same statements.

**Integrity before release.** The closing turn's check reads the set of turn numbers the store holds
(`SELECT turn FROM transcript_turn WHERE conversation = $1`), writes every turn the record holds and the
store lacks, and only then releases the working record to the `retention` the turn carried; a store
that does not answer leaves the record alone, and the sweep repeats the check. A record from before
version 2 of the working record carries no facts for its earlier turns; the check logs them as
unaudited, the one time the audit is knowingly incomplete.

**Status.** A conversation is `open` while its last turn is `tool_use`; the model's end sets `done`,
`refusal` or `cap`. `expired` is set by the installation's purge for an `open` row whose `updated_at` is
older than the working store's time to live: the run was stopped by the loop or abandoned, and its
working record is gone.

**Retention and purge are the installation's.** `expires_at` is written as `started_at` plus the
configured audit retention; the helper runs no purge. Each pack ships `schema/purge.sql`, the two
statements a purge is, and `dev/audit-purge.sh`, a sample a developer runs against the development
container by hand; operations automate the same statements on their own schedule and policy - a job on
`expires_at`, or `transcript_turn` partitioned by month with partitions dropped.

```sql
-- schema/purge.sql: safe to repeat; the turns cascade with their conversation
DELETE FROM transcript WHERE expires_at < now();
UPDATE transcript SET status = 'expired' WHERE status = 'open' AND updated_at < now() - interval '7 days';
```

**Roles and TLS.** The audit function connects as a writer that may `INSERT`, `SELECT` and `UPDATE` the
two tables and nothing else; people and the SQL browser connect as a reader with `SELECT` only. TLS is
on by default (`llm.transcript.audit.postgres.ssl`). The audit store holds everything telemetry refuses
to - prompts, completions, tool arguments and results - so it is personal data until proven otherwise.
The audit does not redact: a redaction policy is applied by the installation on read, because an audit
that edits its own record is not one.

## The questions it answers

```sql
-- every turn of a transaction, suspend and resume included (one correlation id, several trace ids)
SELECT t.conversation, t.turn, t.trace_id, t.started_at, t.status, t.stop_reason
FROM transcript_turn t WHERE t.correlation_id = $1 ORDER BY t.started_at;

-- the turns one request ran (one trace id)
SELECT conversation, turn, status, started_at, ended_at FROM transcript_turn WHERE trace_id = $1 ORDER BY turn;

-- what ran between two instants, with the graph and the node
SELECT c.conversation, c.graph_id, c.node, c.status, c.turns, c.started_at, c.ended_at
FROM transcript c WHERE c.started_at >= $1 AND c.started_at < $2 ORDER BY c.started_at;

-- the whole transcript of a conversation, as the model exchanged it
SELECT c.system, c.tools, t.turn, t.request, t.response, t.calls
FROM transcript c JOIN transcript_turn t USING (conversation) WHERE c.conversation = $1 ORDER BY t.turn;
```

## Implementing the contract on another database

An installation that keeps its audit elsewhere implements the same contract behind the same helper
interface, and the helper's writer stays one:

- the two tables, or their equivalent: one conversation row, one turn row per turn, the columns above
  with the provider's JSON unchanged;
- the key semantics: idempotent writes, the conversation's totals recomputed from its turn rows, and
  the set of turn numbers a conversation holds answerable for the check;
- the status values of both tables;
- the three indexes: correlation id, trace id, time;
- the versions: `transcript_schema.version` for the schema, and the `v` field of the working record
  (version 2 carries the facts the rows are built from).

## Configuration (the helper, both packs)

| Key | Default | For |
|:--|:--|:--|
| `llm.transcript.audit` | unset | `postgres`; unset means no audit store and no audit function |
| `llm.transcript.audit.retention` | `365d` | written as `expires_at`; the purge is the installation's |
| `llm.transcript.audit.grace` | `1d` | how long a closed working record waits for its check before it expires unreleased |
| `llm.transcript.audit.sweep.ms` | `60000` | the sweep's interval; `0` disables it |
| `llm.transcript.audit.postgres.host`, `.port`, `.database`, `.username`, `.password`, `.ssl`, `.timeout.ms` | `127.0.0.1`, `5432`, `mercury`, none, none, `true`, `5000` | the connection; the credential from the environment, never a file |

The driver is an optional extra of each pack (`psycopg[binary,pool]` in Python, `pg` in Node.js). A
configured driver that is not installed stops the application at boot by name; a database that does not
answer at boot is logged, and the sweep repairs what the outage defers.

## See also

- [Polyglot Functions](polyglot-functions.md) — the language packs whose LLM helper writes this store.
- [Observability](observability.md) — the trace ids and correlation ids the rows are indexed by, and why telemetry carries no content.
- [Workflow suspension](knowledge-graph/workflow-suspension.md) — the suspend and resume that one correlation id spans.
- [RFC register](../arch-decisions/RFC.md) — RFC-0012, the proposal this schema realizes, and its rulings.
