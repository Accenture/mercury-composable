# Agent loop (`llm.run`) — tool names map to routes — design spec

**Status:** DRAFT under **RFC-0011** (Open). Four review rounds on 2026-10-28 (Eric), section 14.
Not an ADR: the register entry promotes to one when Eric accepts the whole.
**Serves:** `bp-agent-orchestration` → `vision-mercury-composable` (Track 2, the graph as AI SDLC).
**Extends:** [`ai-agent-orchestration.md`](./ai-agent-orchestration.md) adapter families A
(`llm.chat`), B (MCP, route-per-tool) and C (agent-as-node). This paper defines the seam between
them. It answers the concept's Q1, Q3, Q4, Q5 and Q6, and part of Q8.
**Supersedes:** an earlier staged draft that ran the loop inside the language packs and put the MCP
client inside the loop. Its lessons that hold on any engine are carried in section 12.
**Repo scope:**

- The **agent loop** is engine-side function code, route `llm.run`, in a new opt-in module
  `extensions/agent-loop`: Java first, then the Rust port catches up. It is not engine core, and it
  links no provider SDK, no MCP client and no store client.
- The **model turn** (`llm.chat`), its **transcript store**, and the **MCP bindings** are functions
  of the language packs: Python first, then Node.js. No engine process spawns an MCP server.
- **The graph engine** gains two features: an asynchronous `graph.extension` (section 8.1) and
  progress events for a watched session (section 8.4).

---

## 1. What this specifies

A deterministic function runs an agentic loop. The agent is the LLM. When the agent needs a tool, it
says so in its completion. The loop resolves that ask to one composable-function route, calls it, and
returns the result to the agent. The loop repeats until the agent ends its turn or a limit stops it.

External systems (JIRA, Slack, mail, ServiceNow, GitHub, GitLab, Jenkins, and the like) are reached
by MCP. The MCP client sits inside a route in a language pack. The agent never hears about MCP, and
the loop never speaks it.

**Narrow loops, composed by the graph.** The developer gives each agent loop a narrow scope: one
question to answer, and a few tools to answer it with. A narrow loop needs only a few turns. A graph
may hold several loop nodes, each with its own catalog, mode and turn limit, and the graph composes
them — in sequence, in parallel under a `graph.join`, or behind a decision. A use case grows by adding
loop nodes, not by widening one loop. The graph stays the inspectable control flow, and each loop
stays small enough to certify and to repeat.

The tools a loop may use are fixed on the graph at design time. The product owner anticipates them,
and anticipates where a human approval may be needed. A2A is out of this design (section 11).

## 2. Names and roles

**The agent loop** is the component; **`llm.run`** is its route. The function is not an agent: the
model is. "LLM helper" stays the name of the packs' helper app, which serves `llm.chat`,
`llm.stream` and `llm.health`.

Why `llm.run`: the `llm.` prefix marks every node whose result is model output. A reviewer, or a
later certification check, finds every AI-bearing node in a graph by that one prefix. `run` reads as
one run of turns to an outcome, where `chat` is one turn. Proposed convention: the `llm.` prefix is
reserved for routes whose result is model output, plus the pack's own `llm.health`.

| Role | What it is | What it knows |
| --- | --- | --- |
| Graph | Outer control flow: sequence, decisions, approval, compensation | The `llm.run` nodes, their tool catalogs, their modes and limits |
| Agent loop | `llm.run`, deterministic Java and Rust, one function on each engine | Tool name → route, schemas, approval marks, limits. Never the transcript |
| Model turn | `llm.chat` in a language pack, one completion per call | The provider SDK and wire format. The transcript, in its own store |
| Agent | The LLM, at the provider | Transcript, system prompt, the tool catalog (name, description, schema) |
| Tool route | A composable function, local or over Event-over-HTTP | Its own input contract. Nothing about the loop |
| MCP binding | A pack function per allowlisted remote tool, one shared client per server | Server, transport (stdio or HTTP streaming), remote tool name, credentials |

`llm.chat` stays the only model caller. Bounded-agency graphs that call `llm.chat` and branch on a
verdict are unchanged.

## 3. How the agent asks

The loop calls `llm.chat`. The ask is the completion, not a callback.

On every turn the loop sends the system prompt, the catalog (name, description, input schema per
tool; never the route), the output schema if the node declares one, and a reference to the
conversation. The completion's stop reason decides the next step:

- **`end_turn`** — the outcome is `text`, or `data` when an output schema is declared. The loop
  returns it to the graph.
- **`tool_use`** — one or more calls, each `{ id, name, input }`. The model has filled a schema. It
  has not executed anything.
- **`refusal`**, **`max_tokens`**, **`model_context_window_exceeded`** — the loop stops and names the
  reason in its envelope (section 6).

```text
loop  → llm.chat:  system, tools[{name, description, input_schema}], prompt
llm.chat → loop:   status=tool_use, calls=[{id:"c1", name:"create_issue", input:{title:"…"}}],
                   conversation="9f3c…", turn=1
loop  → route:     jira.create.issue   body=input
route → loop:      {key:"ABC-1"}
loop  → llm.chat:  conversation="9f3c…", turn=1, results=[{id:"c1", output:{key:"ABC-1"}}]
llm.chat → loop:   status=done, text="Opened ABC-1.", conversation="9f3c…", turn=2
```

### 3.1 The split between the loop and the model turn

The transcript belongs to the provider's format, and **it never leaves the pack's side**:

- `llm.chat` keeps the transcript in an access-controlled store of its own (section 3.3), and the
  loop holds only a reference to it.
- `llm.chat` returns the calls in a normalized form for dispatch: `calls[{id, name, input}]`.
- The loop answers with `results[{id, output}]` or `results[{id, error}]`. The pack turns them into
  one user message of `tool_result` blocks, with `is_error` on a failure. It appends that message
  and the new assistant turn to the stored transcript, exactly as the provider returned it.

This keeps the transcript **append-only**, which the 5.5 models require: their thinking blocks are
bound to the model and to the conversation, and an edited history is rejected. Three consequences:

- No engine codec ever carries a provider block, so the serialization gotchas (an integer narrowed
  on the wire, a number widened by the map codec) cannot touch one.
- The record pins the **model** at the first turn. Every later turn, a resume included, uses it.
- Any replica of the pack can serve any turn, because the transcript lives in the shared store. A
  resume that comes days later may land on another pod.

### 3.2 What `llm.chat` gains (pack change, both packs)

Request fields, all optional; absent means today's behaviour:

```text
tools          [{name, description, input_schema, strict}]
conversation   the reference returned by the previous turn; absent on the first turn
turn           the turn count the loop expects the record to hold
results        [{id, output} | {id, error}]   answers to the previous turn's calls
ttl            how long the record lives while the run goes on (duration: 30m, 2d)
retention      how long the record is kept once the model ends the run; 0 deletes it
```

Reply fields when `tools` is present:

```text
status         done | tool_use | refusal | cap
calls          [{id, name, input}]
conversation   the reference
turn           the record's turn count after this turn
usage          input_tokens, output_tokens, cache_creation_input_tokens, cache_read_input_tokens
refusal        {category} when the model refused
```

- **Status, not 422.** With `tools` present, a refusal or a cut-off turn comes back as a status, not
  as today's 422, so the loop can name it.
- **The append check.** A request whose `turn` differs from the record's count is refused with 409.
  A duplicated resume can then never fork a conversation.
- **Caching is the pack's job.** Tools and system come first, unchanged across turns, and the pack
  marks the cache breakpoints.
- **Sampling parameters stay out**: current models refuse them.
- **The first turn** carries `prompt` or `messages` as today. The pack creates the record and
  returns its reference.

### 3.3 The transcript store (in the packs)

The pack's configuration names the store: Redis first, pluggable, with credentials by reference
(`llm.transcript.store`, plus that backend's connection keys). Only the pack reads or writes a
transcript. The engine never holds one.

A deployment may share one Redis between the transcripts and `minigraph-state-redis` (the graph's
suspend store). Each keeps its own key prefix: `transcript:{conversation}` for the pack, and
`graph:{graph_id}:{cid}` for the suspend store. A Redis ACL can confine the pack's user to its
own prefix (`~transcript:*`). The two kinds of record then share a server but not a namespace or
a credential.

Lifetime:

- Every write sets the record's TTL to the request's `ttl`. A run that is waiting for a person
  therefore keeps its transcript.
- When the model ends the run (done, a refusal, the context window), the pack applies `retention`.
  The default `0` deletes the record.
- A run the loop stops itself (a turn limit, the deadline) keeps its `ttl` and expires with it.

## 4. The loop

One `llm.run` call runs turns until one of these stops it:

1. **The outcome.** `end_turn`.
2. **An approval.** A call that must wait for a person (section 7).
3. **A limit.** The turn count (`max_turns`), the deadline, the context window, or one turn cut off
   at `max_tokens`.
4. **A refusal.**

`max_turns` is the run's budget. Each turn re-sends the conversation, so the turn count is a proxy for
the input tokens the run accumulates. A narrow loop (section 1) needs few turns, and the default limit
is small.

For a `tool_use` turn, each call goes through the same steps:

1. **Resolve the name** in the catalog. A name that is not there gets an unknown-tool error as its
   result. The agent decides the next turn: another tool, or end the turn. The loop does not fail
   the node.
2. **Validate the input** against the row's schema. An invalid input gets a schema error as its
   result, and the route is not called.
3. **Check approval** (section 7).
4. **Call the route** with the input as the event body, through the platform route — the same path
   `graph.task` uses. A local route is a local call; a pack route is one Event-over-HTTP hop.
5. **Cap the result.** A result over `result_limit` is cut and marked as cut. A large payload travels
   by reference, as the out-of-band rule for large payloads already says.

Calls of one turn run **concurrently**, each bounded by the time left. Their results go back
together in **one** message, in call order: splitting them teaches the model to stop calling tools
in parallel. A route that throws or times out answers with an error result carrying its status and
message, never its stack trace.

**What fails the node.** Only the loop's own machinery: the catalog fails its pre-flight check
(section 5), a resume carries decisions that do not match the waiting calls, or `llm.chat` fails
after the pack's own retries. These throw, so the node's `exception=` route handles them. A model
outcome — done, approval, refusal, a limit — is an envelope that a decision node routes on. Each
failure is retried in one layer only: the SDK retries a 429 or a 5xx inside the pack, and the graph
retries anything else through `RESET`.

**The deadline.** `graph.task` bounds the call by the graph's `model.ttl` (30 seconds unless the
graph sets it). The loop takes its deadline from the time left. It gives each `llm.chat` turn a
`timeout_ms` below that, so the loop's own limit fires first and names itself.

**No nesting.** A catalog row is a unit of work, never another agent loop. An agent is a graph node.
A node that runs `llm.run` returns its outcome and decision. The next node evaluates them, and may
run `llm.run` again with different parameters. This rule is declared, not enforced by a partial
check: a check could refuse a row that names `llm.run` but never one that reaches it through a flow,
and a partial gate teaches that an unflagged graph is safe.

## 5. The tool catalog

One map per loop node, held as a **skill-less dataset node** wired under the graph's `graph.island`.
Traversal sinks at the island, so the catalog is never a step in the run. It is graph data, certified
on the graph like a decision table, and the developer reviews it before anything executes. Each loop
node maps its own catalog, so a graph with several loops holds several small catalogs.

```text
name           the name advertised to the agent: ^[a-zA-Z0-9_-]{1,128}$, unique in the catalog
description    written on the graph; the model never sees the MCP server's own description
input_schema   the schema advertised to the agent, and the one the loop validates against
route          the composable-function route
strict         true | (absent)     grammar-constrained arguments
approval       required | (absent) a high-risk write, section 7
```

A route name such as `orders.search` cannot be an advertised name: the API refuses a dot. The row
maps an API-safe name (`orders_search`) to the route.

```text
create node knowledge
with type Island
with properties
skill=graph.island

create node triage-tools
with type ToolCatalog
with properties
tools='''[ { "name": "create_issue", "route": "jira.create.issue",
             "approval": "required", "description": "…", "input_schema": { } } ]'''

connect root to knowledge with contains
connect knowledge to triage-tools with data
```

The loop's settings are constant input mappings on its node, so they are visible on the graph and
certified with it:

```text
create node investigate
with type Task
with properties
skill=graph.task
task=llm.run
input[]=f:json(triage-tools.tools) -> tools
input[]=text(auto) -> mode
input[]=int(6) -> max_turns
```

The catalog is fixed before the first model turn, and the model picks only among its names, never an
arbitrary route string. Changing what an agent can reach means changing the dataset and redeploying
the graph. That is the certification boundary, and the answer to the concept's Q1.

**Pre-flight check.** The routes sit inside a JSON text, so the graph compiler never sees them.
Before the first model turn, the loop checks that every name is valid and unique, every schema
parses, every `approval` value is known, and every route is reachable (local, an Event-over-HTTP
target, or the mesh). Any failure throws before a token is spent. This is the loop's failure, and
it differs from an unknown name the model invents, which goes back to the model.

## 6. What the graph sees

`llm.run` is one node, called with `graph.task`. Any number of nodes may call it. Several may run in
parallel on forked branches, and a `graph.join` waits for all of them. Each call is its own loop,
with its own conversation and catalog; the loop shares no state between calls.

The outcome envelope:

```text
status         done | approval | refusal | cap
data | text    the outcome; data when the node declares an output schema
decision       optional: true | false, or a 1-based integer for multiple options
approval       when status = approval: {calls: [{id, name, route, input, wait}]}
cap            when status = cap: turns | deadline | context | max_tokens
refusal        when status = refusal: {category}
conversation   the reference to the transcript in the pack's store, never the transcript
turn           the conversation's turn count, for the next call's append check
turns          the turns this run took
usage          accumulated input, output, cache-creation and cache-read tokens
calls          [{id, name, route, status}]   names, routes and outcomes; arguments stay off
model
```

`decision` is lifted from `data.decision` when the node's output schema declares one. This is the
business decision of section 7.2, so one decision-node pattern routes any AI node: on `status`
first, then on `decision`. The same `status` and `decision` fields are proposed for `llm.chat`, so
both AI nodes answer in one shape.

**State stays small.** `model.*` is persisted on every suspension and serialized on every hop. It
holds only the status, the decision, the usage, the references, and the calls that wait for an
approval. The transcript stays in the pack's store.

### 6.1 Settings

Each setting is a constant input mapping on the loop's node. A global default for each lives in the
engine's `application.properties`:

| Node input | Global default | Meaning |
| --- | --- | --- |
| `mode` | `auto` | approval mode, section 7.1 |
| `max_turns` | `llm.run.max.turns=10` | the run's turn limit; a proxy for its cumulative input tokens |
| `result_limit` | `llm.run.result.limit=65536` | the largest tool result, in characters of JSON text |
| `transcript_ttl` | `llm.run.transcript.ttl=7d` | how long a running or waiting transcript lives; at least the longest approval wait |
| `retention` | `llm.run.transcript.retention=0` | how long a finished run's transcript is kept for audit; `0` deletes it at the end |

The loop passes `transcript_ttl` and `retention` to `llm.chat` on every turn (section 3.2). The
deployment's audit policy is therefore one default in the engine's configuration, which a node
overrides for a use case that must keep its transcripts. No key above is also the parent of another
key.

The other inputs describe the run: `system`, `prompt` or `messages`, `schema` (the output schema),
`model` and `tools`. On a resume, they are `conversation`, `turn`, `approval` (from `model.*`) and
`decisions` (from the resume request).

## 7. Approval: two kinds

### 7.1 A tool call that must wait (the node's mode)

A row marked `approval: required` is a high-risk write. The node that calls `llm.run` sets its mode,
much as Claude Code has permission modes. The difference: the row's mark is fixed on the graph and
certified with it, where Claude Code's `auto` asks a classifier to judge each call.

```text
mode=auto        (default) a row marked approval: required waits; every other row runs
mode=manual      every tool call waits: a supervised run
mode=unattended  a marked row is refused as a tool error and never waits: a run with nobody to ask
```

No mode runs a marked row without a person. A node may tighten (`manual`) or refuse (`unattended`),
never bypass.

When a turn holds a call that must wait, **the loop runs none of that turn's calls**. It returns
`status: approval` with every call of the turn, its arguments included, because a person cannot
approve what they cannot see. Each call carries `wait: true` (needs a decision) or `wait: false`
(runs on resume). Deferring the whole turn keeps every result of the turn in one message, and needs
no second store for results held back.

The graph suspends (`graph.suspend`), so the suspend store holds the waiting calls. It is held under
the same access control as the transcript store. The resume resets the loop node and runs
`llm.run` with:

- the conversation reference and the turn count;
- the `approval` block, restored from `model.*`;
- one decision per waiting call, `{id, approved, note}`, from the resume request.

The loop takes the calls from the restored state, never from the resume request, so the call it runs
is the call that was shown. Every waiting call needs a decision, and a decision for an unknown id
throws. Approved calls and the calls marked `wait: false` run. A refused call goes back as an error
result carrying the reviewer's note. All results return to the model in one message, and the loop
continues.

### 7.2 A business decision (the use case)

The agent's verdict may carry a `decision`: `true` or `false` for a binary choice, or an integer
for one of several options. Example: the agent finds that a transaction is over the credit limit
and that an approval is required. The graph, designed for the use case, has a decision node after
the `llm.run` node. That node examines the result set and either suspends for a person's approval
or continues to the next step.

The product owner who designs the use case knows which scenarios may need a person, so the graph
carries the decision node, and the output schema carries the `decision` field. Tool-call approval
guards a write; a business decision routes the case.

```text
investigate (llm.run) → triage (decision on status, then on decision)
   status=approval  → hold (graph.suspend) → resume → RESET investigate
   decision=true    → hold-for-officer (graph.suspend) → …
   decision=false   → book-transaction → …
```

Every loop in the graph carries a counter in `model.*` that bounds it, and the bound exits to a
person or a recovery node.

## 8. Asynchronous by design

A graph with agent loops can run for minutes or hours. A REST call from a UI has an SLA of a few
seconds, so **a graph with an agent loop is never a synchronous request**. There are two
interaction modes, chosen per use case.

### 8.1 Unattended: HTTP 202, then a notification

The user is not expected to watch. Event Script has a deferred final response: a task with
`execution: response` sends the HTTP response, and the flow continues in the background. MiniGraph
has no equivalent today. This design adds one to `graph.extension`:

```text
create node start-review
with type Extension
with properties
skill=graph.extension
extension=credit-review
async=true
input[]=input.body.application -> application
```

With `async=true`:

- **The node starts the target and returns `next` at once.** The target is a deployed subgraph or a
  flow (`flow://`), started with the node's input mapping.
- **The target inherits the business correlation ID**, as a delegated graph or flow does today, so
  its suspensions resume under that ID and its trace continues. It runs under its own TTL (the
  subgraph's `model.ttl`, or the flow's `ttl`), not the parent's.
- **Nothing comes back.** The node takes no `output[]`. CompileGraph refuses one: the check is
  complete, because the mapping is visible on the node itself.
- **`exception=` covers only a failure to start** (an unknown target, a refused input). A failure
  inside the target routes to the target's own handlers.

**Fan-out.** `async=true` with `for_each` starts one run of the target per element of the list, in
parallel:

- **The node still returns `next` at once.** `concurrency` (1-30, default 3) bounds how many of the
  runs are active at the same time. The engine starts the next element as an earlier run ends.
- **Each run is independent.** It has its own TTL, its own agent loops and conversations, and its
  own end. A run that suspends keeps its own record, keyed by its position in the array, as a
  synchronous `for_each` does today.
- **The `for_each` suspension rules apply unchanged.** The array must stay positionally consistent:
  appending is safe, but inserting, removing or reordering is not, so freeze a derived array into
  `model` before fanning out. A suspending graph is never reached through a flow launched by
  `for_each`.
- **One notification for the whole set.** A use case that needs one notification when every
  element has finished starts ONE asynchronous subgraph. Inside it, a synchronous `for_each` fans
  out, and the subgraph notifies at its end. The parent cannot join runs it does not wait for.

The parent graph continues to its end node, which maps the advice into the graph's output. The
generic `/api/graph/{graph_id}` endpoint returns it unchanged:

```http
HTTP/1.1 202 Accepted
Content-Type: application/json

{
  "type": "advice",
  "status": 202,
  "message": "ETA 5 minutes. You will receive a message notification once it is complete."
}
```

The subgraph runs its agent loops to its end. Its last node sends the notification through a route
chosen by the use case (mail, chat, a Kafka topic). With `graph.suspend` and `graph.resume` the run
can span days: each segment runs within its TTL, and the store's TTL bounds the wait.

### 8.2 Watched: a WebSocket session or a streaming HTTP response

The user stays on the line for a moderately long run, up to about 30 minutes:

1. The session starts with its first block of data as input. The engine creates the graph instance
   with that input and the return route the caller supplied (section 8.4).
2. Graph nodes and `llm.run` send progress events to the session (section 8.4).
3. The graph sends the final result, then closes the session and the graph instance.

Engine work: today a graph instance keeps its return route (`reply_to`) and replies once, at the
end. Progress sent mid-run is the staged follow-up the concept names under Q8.

### 8.3 Time limits, layered

| Layer | Bounds | Sized for |
| --- | --- | --- |
| `rest.yaml` timeout | the HTTP call | seconds: the 202, or the session handshake |
| flow `ttl` | the wrapping flow | the parent graph only, once the work is asynchronous |
| graph `model.ttl` | each `graph.task` call, so each `llm.run` segment | the longest segment |
| `llm.run` deadline | the loop, from the time left | below the node's TTL |
| `llm.chat` `timeout_ms` | one model turn, the pack's retries included | below the loop's time left |
| suspend store `ttl` | a wait for a person | days, per use case |
| `transcript_ttl` | the transcript while the run goes on | at least the suspend store's `ttl` |

### 8.4 Progress events

Every event is one JSON object:

```json
{
  "type": "progress",
  "event": "tool.start",
  "seq": 12,
  "time": "2026-10-28T23:30:11.120Z",
  "cid": "6f1e0c2a8b4d4f7e9a3c5d2b1e0f4a7c",
  "graph": "credit-review",
  "node": "investigate",
  "message": "Checking the credit limit",
  "data": { "turn": 2, "id": "c1", "name": "credit_limit", "route": "credit.limit.lookup" }
}
```

| Field | Meaning |
| --- | --- |
| `type` | `progress`, or one of the two terminal types: `result` and `error`. After a terminal event the engine closes the session |
| `event` | the event name, on `progress` only |
| `seq` | from 1, increasing per sender (the engine, or one `llm.run` call), identified by `graph` and `node`; a gap tells the client it lost an event |
| `time` | ISO 8601 UTC, with milliseconds |
| `cid` | the business correlation ID, the same ID a resume uses |
| `graph`, `node` | the graph ID and the node that sent the event; a subgraph names itself |
| `message` | optional short text, written on the graph or by the loop; never model output or tool data |
| `data` | optional and small: names, routes, counts and statuses only |

Event names:

| `event` | Sent by | `data` |
| --- | --- | --- |
| `node.start` | the engine, for a node that carries a `progress` property | none; the property's text is the `message` |
| `run.start` | `llm.run` | `model`, `tools` (a count), `max_turns` |
| `turn.start` | `llm.run` | `turn` |
| `turn.end` | `llm.run` | `turn`, `stop_reason`, `usage` |
| `tool.start` | `llm.run` | `turn`, `id`, `name`, `route` |
| `tool.end` | `llm.run` | `turn`, `id`, `name`, `route`, `status`, `ms` |
| `approval.wait` | `llm.run` | `calls`: `[{id, name, route}]`; the arguments stay in the envelope |
| `run.end` | `llm.run` | `status`, `turns`, `usage` |

The terminal events:

```json
{ "type": "result", "seq": 40, "time": "…", "cid": "…", "graph": "credit-review", "status": 200, "body": { } }
{ "type": "error",  "seq": 40, "time": "…", "cid": "…", "graph": "credit-review", "status": 500, "message": "…" }
```

**Transport.** On a WebSocket, each event is one text frame. On a streaming HTTP response, each event
is one server-sent event: `event:` names the `type`, and `data:` carries the JSON.

**The return route is the caller's.** The engine names no progress route of its own. It sends to the
route the caller supplied, and the engine carries that route from the entry into the graph
instance:

| Entry | Return route | Receives |
| --- | --- | --- |
| REST, a plain response | `async.http.response`, the REST automation's response route | the final result only: one response per request |
| REST, progressive rendering (`stream: true`) | the request's ordered reply lane | progress events, then the final result |
| WebSocket session | the session's outbound route | progress events, then the final result |
| An event, such as a Kafka message | the route named in its `x-return-path` header | progress events, then the final result |

A plain REST response takes the final result alone, so progress goes only to a route that accepts a
stream. An event that enters with `x-return-path` has no other way back, so its final result goes
there too.

**Opt-in per node.** A graph node opts in with a `progress=<text>` property. The engine then sends
`node.start` with that text. If the node is a `graph.task`, the engine also passes the return route
to the function in the same `x-return-path` header. `llm.run` sends its events to that route, and
sends none when the header is absent. A node without the property never sees the header, so a
function that forwards its headers downstream (to a Kafka topic, say) cannot leak a caller's return
path to a system that would answer it.

When a run suspends, the return route receives the graph's result (the approval advice) and the
session closes. A resume starts a new session, or notifies. `x-return-path` joins the
reserved-names guide.

**The model's own progress notes** (`display: "updates"` on the 5.5 models) are model-written text,
so v1 does not forward them.

## 9. MCP bindings in the language packs

No engine process runs a child process for a stdio MCP server. The MCP client lives in the packs,
Python first, then Node.js. The concept's security seam (§3.3) is restored: stdio children and
server credentials stay in the pack's process and environment, never in the JVM or the Rust engine.

**The client library is the official MCP SDK**, not the Anthropic SDK: `mcp` on PyPI
(`ClientSession`) and `@modelcontextprotocol/sdk` on npm. The Anthropic SDK's `anthropic[mcp]`
extra adds only conversion helpers (`anthropic.lib.tools.mcp`, beta). They turn an MCP session's
tools into tools for the SDK's own tool runner. A binding serves a route rather than a tool runner,
so it does not need them.

The binding:

- **Servers by name, in the pack's configuration.** Each server has its transport (a stdio command,
  or an HTTP streaming URL), its credential references, and its allowlist mapping each remote tool
  to a route. A graph never carries a command line or a URL.
- **One shared client per server.** The routes of one server share its session.
- **One route per allowlisted remote tool** (`jira.create.issue`, `slack.post.message`). On
  invocation it issues `tools/call` with the event body as `arguments`. It returns
  `structuredContent` when the server gives it, and the text content otherwise. A result with
  `isError` becomes an exception, which the loop returns to the model as an error result.
- **Boot fails fast** if an allowlisted tool is missing from `tools/list`, or if its input schema no
  longer matches the digest pinned in the configuration. A server-side schema change then stops a
  deploy instead of becoming run-time errors that the model improvises around.
- **The engines reach a binding through Event-over-HTTP**, as they reach `llm.chat`. The loop does
  not branch on whether a route is local code or an MCP binding.

The graph's catalog row copies the tool's description and schema, and the developer reviews them.
The model sees the graph's text, never the server's, so injection through a tool description is
closed by construction. A vendor with no MCP server takes the same shape: a pack or engine route
that calls its HTTP API. MCP resources are not catalog rows; a route that reads a resource is
ordinary function code. A single generic `mcp.call` route is not the dispatch: it would hide the
server and the remote tool name inside the arguments.

## 10. Telemetry, audit and security

**Telemetry is mandatory.** The `llm.run` span parents every `llm.chat` span and every tool-route
span. Annotations carry the turn count, the model, the usage and the status. They never carry
prompts, completions, tool arguments or tool results. Progress events follow the same rule.

**Audit is optional, per use case.** The transcript store already holds the content. Retention is a
global default in the engine's configuration (`llm.run.transcript.retention`, `0` = delete at the
end), and a node overrides it (section 6.1). A retained record stays under the store's access
control, because it may contain PII.

**Security.** The model is untrusted to choose well, and every tool result is untrusted data: a
ticket's text can carry instructions. The controls:

1. a fixed catalog per loop, certified on the graph;
2. descriptions written on the graph, never taken from a server;
3. inputs validated before dispatch;
4. approval marks, and the node's mode;
5. MCP children and credentials in the pack's process, with servers named, never addressed;
6. results capped, and kept as data that the loop never obeys;
7. narrow loops, and no nesting;
8. arguments off the trace and off the progress events;
9. the transcript in an access-controlled store, and the approval arguments in the suspend store
   under the same control.

The concept's honest-claim rule holds: this is governed nondeterminism, never deterministic
reasoning.

## 11. A2A

Not in this design. An agent that lives on the platform is a graph node running `llm.run`, and the
graph sequences those nodes. `graph.suspend` covers a peer that must wait for a person.

A2A is the external protocol for a peer we do not own. Add it when such a peer speaks A2A and offers
no route or HTTP API. The wrapper is then a route (`external.claims.agent`), and it is a catalog row
like any other tool. The peer is a tool to us, not a nested loop of ours.

## 12. Lessons carried from the prototype

A command-line prototype of a loop ran before this design, against the Claude API and Bedrock.
These lessons hold on any engine:

- **Tool names** match `^[a-zA-Z0-9_-]{1,128}$`. A route name with a dot is mapped, never advertised.
- **One grammar budget.** The output schema and the strict tool schemas compile into one grammar,
  and a large output schema beside a few tools can exceed it. The pack drops `strict` on the tools
  first, then reports the degradation. A node with a large output schema declares its tools
  non-strict. Narrow loops keep both schemas small.
- **Forced tool choice** (`any` or a named tool) is refused on the 5.5 models. Steer with `auto` and
  an instruction.
- **Append-only.** Thinking blocks go back unchanged, on the same model; an edited history is
  rejected.
- **One message per turn's results.** The prototype ran a stopped turn's ordinary calls at once and
  held their results until the decision. This design defers the whole turn instead (section 7.1).
  Both keep the one-message rule; deferral needs no second store.
- **A small model may skip its tools** and name an identifier it invented. A validator node after the
  verdict checks that what the verdict names exists.
- **A tool loop costs repeatability.** Two runs diverge wherever the model chose a different lookup.
  Context that the graph can fetch before the AI node, it fetches there. The loop is for what that
  context cannot answer: a decision to act, a read made on purpose, an approval. That is the
  argument for narrow loops.

## 13. Build plan

1. **The loop contract in the Python pack**: `llm.chat` gains the contract of section 3.2 and the
   transcript store of section 3.3, with shared vectors. Then the Node.js twin.
2. **`llm.run` in Java** (`extensions/agent-loop`): the loop, the pre-flight check, the modes and
   approvals, the settings, the envelope. Tests run on a scripted `llm.chat` route, with no tokens
   spent. Shared loop vectors (scripted turns → expected route calls and envelope) carry the
   behaviour to Rust.
3. **The graph engine**: `async=true` on `graph.extension`, with and without `for_each`, and its
   CompileGraph check (section 8.1). Then the return route carried from each entry into the
   instance, `x-return-path` in the reserved-names guide, the `progress` node property, and
   `llm.run`'s events (section 8.4).
4. **MCP bindings in the Python pack**: stdio and HTTP streaming, the allowlist, schema pinning.
   Then the Node.js twin.
5. **`llm.run` and the graph features in Rust** catch up from the shared vectors.

Development and the certification drives use the packs' Claude API backend (`llm.backend`), which
serves every current model. Bedrock stays a client-construction switch inside the pack, and the
loop does not change for it.

## 14. Review rulings (Eric, 2026-10-28)

**Round 1** (on the first draft):

1. Approval pauses the loop. The loop does not become a graph edge per tool call.
2. An unknown tool returns to the agent as that call's error result. The agent picks another tool or
   ends the turn.
3. Boot fails fast when an allowlisted MCP tool is missing.
4. The catalog is a dataset node under an island, reviewed at design time.
5. *(Superseded in round 2)* The route name was `agent.run`. Any `graph.task` node may call it, and
   several may run in parallel with `graph.join` as the barrier (this part stands).
6. Trace and audit are separate. Telemetry is mandatory, auditing is optional per use case, audit
   persistence is access-controlled, and tool arguments stay off the trace.
7. *(Superseded in round 2)* The first MCP binding was to run in the engines.

**Round 2** (on the review of the first draft):

8. The outcome envelope carries an optional approval.
9. The transcript lives in an access-controlled store, so the state machine stays small.
10. The split of section 3.1 is agreed: an opaque transcript, normalized calls, and the pack builds
    the tool-result message.
11. A graph with an agent loop is asynchronous: an HTTP 202 with a notification, or a watched
    WebSocket or streaming session (section 8). With suspend and resume, a run spans days.
12. A tool name maps to a route, and the loop calls the route when the agent asks.
13. No nesting: each `llm.run` is a graph node, and the graph sequences them.
14. The node calling the loop carries a mode, `auto` by default, which the graph can override.
    High-risk writes ask for a person.
15. No child processes in the JVM. MCP clients live in the language packs.
16. The loop lives in the engines: Java first, then the Rust port catches up.
17. The route is `llm.run` (Eric's proposal, agreed in review: the function is not an agent).
18. Approval is declared per use case. The product owner anticipates the tools, and the scenarios
    that need a person, at design time (section 7).
19. The component is called "the agent loop".
20. Development uses the Claude API backend.

**Round 3** (on the round-2 open questions):

21. Modes: `auto`, `manual` and `unattended`. No mode runs a marked row without a person.
22. The run's budget is `max_turns`, a node setting with a global default, as a proxy for cumulative
    input tokens. There is no separate token limit.
23. Audit retention has a global default in the application's configuration.
24. `llm.run` lives in a new opt-in module, `extensions/agent-loop`.
25. The deferred final response exists in Event Script only. MiniGraph gains it as a node that runs
    a subgraph or a flow asynchronously (section 8.1).
26. The progress contract of a watched session is proposed in section 8.4.
27. The language packs get store access: the transcript lives in the pack's store, and the loop holds
    a reference.
28. Each agent loop has a narrow scope, so its turns stay few. A graph may hold several loop nodes
    (section 1).

**Round 4** (on Q8–Q11):

29. The defaults of section 6.1 stand: 10 turns, a 64 KiB result limit, a 7-day transcript TTL and
    retention `0`.
30. The transcripts may share a deployment's Redis with the suspend store, under their own prefix and
    namespace (section 3.3).
31. The progress route is the caller's. For REST it is `async.http.response`, or the
    progressive-rendering reply lane. For an event such as a Kafka message, it is the route named in
    the message's `x-return-path` header (section 8.4).
32. `async=true` supports `for_each` for parallel fan-out (section 8.1).

## 15. Open questions

Rounds 3 and 4 answered Q1–Q11, and none remains open. One default was set without a ruling: v1
does not forward the model's own progress notes (section 8.4).
