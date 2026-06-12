# MiniGraph — Command Syntax & Conventions

All graph authoring is done via commands sent to the Companion API (`POST /api/companion/{session-id}`, `Content-Type: text/plain`). Never write graph JSON directly — it is an output artifact produced by the engine.

> **Verification legend:** claims tagged **(verified)** were execution-tested against the live engine and recorded in [`evidence/dogfood-customer-360.md`](./evidence/dogfood-customer-360.md). Unmarked syntax/behavior is documented from the engine's help surface and may not be execution-tested yet — treat it as **asserted** until dogfooded.

---

## Command Reference

### Command Termination

Commands are terminated by the end of the HTTP request body. There is NO explicit end-of-command marker.

**IMPORTANT:** Do NOT include `...` as a line in commands. The `...` shown in documentation examples is a placeholder meaning "more properties can follow" — it is NOT a literal terminator. If included literally, the parser interprets it as a malformed composite path key (dot-separated segments with empty values), causing `ERROR: Missing composite path`.

### create node
```
create node {name}
with type {Type}
with properties
{key}={value}
{key}[]={value}
```
- **Node name rules:** Only `0-9`, `A-Z`, `a-z`, underscore (`_`), and hyphen (`-`) are allowed. Convention is lowercase with hyphens (e.g. `policy-300-plan-allows`). Invalid characters produce: `Invalid syntax ({name}). Please use 0-9, A-Z, a-z, underscore and hyphen characters.`
- **Type rules:** Same character set as node names. Convention is PascalCase (e.g. `Root`, `Evaluator`, `Decision`).
- `{key}={value}` — single-value property
- `{key}[]={value}` — append to list property (mapping, statement, dictionary, input, output, feature, for_each)
- `'''` — triple single quotes wrap multi-line values (e.g. IF/THEN/ELSE blocks)
- The command ends when the HTTP POST body ends — no terminator needed
- **`with type` is optional and largely cosmetic for skill nodes** (verified): a node with a `skill` runs regardless of type and is stored as `types: ["untyped"]` when the line is omitted — the **`skill`** drives behavior. Type still matters for `Root`/`End` and no-skill nodes, and remains a useful model/UI classification.

### connect
```
connect {node-A} to {node-B} with {relation}
```

### update node
```
update node {name}
with type={Type}
with properties
{key}={value}
```
- Delimiter differs from `create node` (both verified working for their own command): `update` uses `with type={Type}` (equals); `create` uses `with type {Type}` (space).
- `update node` replaces the property set — re-specify list properties (`mapping[]`, `statement[]`, …) in full, not just the changed entries (verified).

### delete
```
delete node {name}
delete connection {nodeA} and {nodeB}
clear cache
```

### session
```
session reset
```
- `session reset` clears the current session and starts a fresh empty graph (verified). It is distinct from `clear cache` (a graph cache/state command).

### describe
```
describe graph
describe node {name}
describe connection {node-A} and {node-B}
describe skill {skill.route.name}
```

### list
```
list nodes
list connections
```

### instantiate graph
```
instantiate graph
{constant} -> input.body.{key}
{constant} -> input.header.{key}
{constant} -> model.{key}
```
Constants: `text(value)`, `int(n)`, `long(n)`, `float(n.n)`, `double(n.n)`, `boolean(true|false)` (also `map(...)`, `file(...)`, `classpath(...)`). **There is no `number()`** — use `double(n.n)` or `float(n.n)` for decimals. An unrecognized constant makes the whole `instantiate` fail silently (the instance is dropped and every `inspect` returns `404`). (All verified: `int(42)`, `double(3.14)` seed correctly; `number(7)` drops the instance.)

Array-append seeding works (verified): repeating `text(A-1) -> model.account_ids[]` and `text(A-2) -> model.account_ids[]` builds a list under `model.account_ids`.

### run / inspect / seen / execute
```
run
inspect {variable_name}
seen
execute {node-name}
```
- **`inspect` serves containers only — a `404` on a dotted leaf does NOT mean the node halted.** The inspect endpoint (`rest/InspectStateMachine.java:47-51`) returns the value **only if it is a `Map` or a `List`**; any scalar leaf (string, number, boolean) yields `404 "Not found"` by design. So `inspect enrich.result` returns the map `{ "x": 15 }`, but `inspect enrich.result.x` (a number) returns `404` **even though the value exists**. Always read the parent container, not the scalar leaf. To tell a genuine silent-halt apart from this artifact: inspect the **container** — a result key *missing from the container map* is a real halt; a `404` on the dotted scalar path under a present container is just this endpoint behavior.

### import / export
```
export graph as {name}
import graph from {name}
import node {node-name} from {graph-name}
```

---

## Node Types

| Type | Skill | Purpose |
|------|-------|---------|
| Root | — | Entry point. Named `root`. Properties: `name`, `purpose` |
| End | — | Terminal. Named `end`. Empty properties. |
| Fetcher | `graph.api.fetcher` | External API invocation via data dictionaries |
| Provider | — | HTTP endpoint definition (url, method, headers) |
| Dictionary | — | Maps API input/output for a specific data attribute |
| Island / Entity | `graph.island` | Isolates dictionary subgraph from execution |
| Evaluator | `graph.math` or `graph.js` | Conditional branching, computation |
| Decision | `graph.data.mapper` | Outcome/result node (maps output data) |
| Mapper | `graph.data.mapper` | Data transformation |
| Extension | `graph.extension` | Invokes another graph as sub-graph |
| Join / Joiner | `graph.join` | Synchronizes parallel branches |

---

## Naming Conventions

These are suggested conventions for readable, consistent names. They are not enforced by the engine — only the character set (`0-9 A-Z a-z _ -`) is. Adapt the prefixes to your own domain.

### Graphs
| Role | Pattern | Example |
|------|---------|---------|
| Orchestrator (sources data, calls sub-graphs) | `manager-[SUBJECT]-[ACTION]` | `manager-request-validate` |
| Sub-graph (logic / evaluation) | `ext-[SUBJECT]-[ACTION]` | `ext-request-evaluate` |
| Sub-graph (operation) | `ext-[OPERATION]` | `ext-sum-amounts` |
| Reusable dictionary collection | `common-dict-[PROVIDER]` | `common-dict-example-api` |

### Nodes
| Usage | Pattern | Example |
|-------|---------|---------|
| Provider | `[SERVICE]-[VERSION]` | `record-service-v2` |
| Dictionary | `[PROVIDER-ABBREV]-[ATTRIBUTE]` | `config-threshold` |
| Fetcher | `fetcher-[PROVIDER]` | `fetcher-config` |
| Extension | `ext-[NAME]` | `ext-evaluate` |
| Evaluator | `check-[SUBJECT]` or `[SCENARIO-ID]` | `check-balance` |
| Decision | `dec-[LOGICAL-NAME]` | `dec-reject-inactive` |
| Mapper | `mapper-[TYPE]` | `mapper-input` |
| Entity (island) | `[LOGICAL-NAME]` | `dictionary`, `record` |

### Connections
| Flow | Relation |
|------|----------|
| Root → fetcher | `ask` or `fetch` |
| Root → mapper-input | `ask` or `execute` |
| Fetcher → join | `wait` or `join` |
| Join → evaluator/extension | `evaluate` or `execute` |
| Node → extension/evaluator | `ask` or `evaluate` |
| Decision → end | `complete` or `finish` |
| Sequential flow | `execute` |
| Custom branch labels | descriptive (e.g. `approved`, `not-allowed`) |

---

## Mapping Expression Syntax

Format: `source -> target`

### Sources
- `input.body.{path}` / `input.header.{path}` — request data
- `model.{path}` — state machine
- `{node-alias}.result.{path}` — node computation result
- `text({literal})` / `int({n})` / `long({n})` / `float({n})` / `double({n})` / `boolean(true|false)` — constants (no `number()`)
- `model.none` — null initializer (use before array append); **verified** to resolve to null (used as a mapping source the target is skipped — a genuine null, unlike the undefined `model.zero`)
- `response.{path}` — raw API response (in Dictionary output only)

### Targets
- `output.body.{path}` / `output.header.{path}` — response output
- `model.{path}` — state machine
- `model.{path}[]` / `output.body.{path}[]` — array append
- `{node-alias}.{property}` — write to another node

> When a mapping's source resolves to null/absent, the target is **silently skipped** (not written as null) — verified. Force a value with `f:defaultValue(source, fallback)`.

### Functions
| Function | Usage |
|----------|-------|
| `f:defaultValue(source, fallback)` | Returns source if non-null, else fallback |
| `f:now(text(ms))` | Current epoch milliseconds |
| `f:now(text(local))` | Local timestamp |
| `f:parseDate(field, text(format; ms))` | Parse date to epoch ms |
| `f:uuid()` | Generate UUID |
| `f:text(field)` | Convert to string |
| `f:includes(field, text(value))` | Check if string contains substring |
| `f:concat(field1, field2)` | Concatenate two values |
| `f:listOfMap(model.{path})` | Convert parallel arrays to list of maps |
| `f:removeKey(source, text(key))` | Remove key from objects in list |

> **Verification status:** all functions in this table are **execution-verified**: `f:defaultValue` (present→source / null→fallback), `f:now` (epoch ms + local timestamp), `f:parseDate` (`yyyy-MM-dd; ms` → epoch ms), `f:uuid`, `f:text` (int→string), `f:includes`, `f:concat`, `f:listOfMap` (parallel arrays under a model path → list of maps), `f:removeKey` (drops a key from each map in a list).

### Null Checking

The `:boolean(null=true)` selector must run in a **`graph.data.mapper`** node (verified). As an inline `MAPPING:` statement inside `graph.math`/`graph.js` it does **not** create the value, and a later `IF` on the missing value then halts the node. Two steps — a mapper computes the flag, an evaluator branches on it:

```
# in a graph.data.mapper node:
mapping[]=model.field:boolean(null=true) -> model.is_field_null

# in a downstream graph.math node:
statement[]='''
IF: {model.is_field_null} == false
THEN: have-value
ELSE: handle-null
'''
```

> **Coerce the flag — the selector is asymmetric (verified, session ws-697490-4).** `:boolean(null=true)` writes `true` when the source is null but writes **nothing** when it is present (not `false`). So on the present-value path `model.is_field_null` is *absent*, and a downstream `IF: {model.is_field_null} == false` then references an unresolved value and **halts the node** (see the IF-halt rule below). Coerce it to a real boolean in the **same mapper**, right after the null-detect line:
> ```
> mapping[]=model.field:boolean(null=true) -> model.is_field_null
> mapping[]=f:defaultValue(model.is_field_null, boolean(false)) -> model.is_field_null
> ```

### JSONPath
- `$.model.array[*]` — all elements (use in graph.js COMPUTE)
- `$.input.body.list[?(@ == {value})]` — filter by value
- `$.model.list[?(@.field == {value})]` — filter by property
- `$.model.list[?(@.ids contains {value})]` — contains check
- `response.accounts[0].field` — array index access

### Array Append
```
mapping[]=text(value) -> model.list[]
```

---

## Statement Syntax (graph.math / graph.js)

### IF/THEN/ELSE
```
statement[]='''
IF: {model.field} == 'value' && {model.other} != 'x'
THEN: target-node-alias | next
ELSE: target-node-alias | next
'''
```
- `{path}` resolves state machine values
- `next` = continue to the next **statement** per the docs; the statement-`next` vs node-`next` distinction is **not yet cleanly isolated** (the one observed halt traced to an unresolved reference, not to `next`)
- After its statements, a skill node with no branch target returns node-level `next` and traversal follows the natural outgoing edges (verified for `graph.js`); a named target overrides this
- Named target = override traversal to that node (the target need not have a natural inbound edge — verified)
- Operators: `==`, `!=`, `>`, `<`, `>=`, `<=`, `&&`, `||`
- **An `IF` halts the node silently if it references a value that does not resolve** (verified) — including a bare `{x} != null` when `x` is absent. There is no working bare null-check; detect null with the mapper pattern under [Null Checking](#null-checking), then branch on the resulting boolean.
- **Use bare literals in expressions, not `int()`/`text()` (verified, session ws-697490-4).** The `int()` / `text()` / `boolean()` constants are for **seeds and mappings**, not IF/COMPUTE expressions. `IF: {model.x} == int(1)` substitutes to `0 == int(1)`, and the evaluator tries to **call `int` as a function** → `Attempting to call a non-function` → the run **aborts** (the error appears only on the WebSocket console, not the POST response). Write `IF: {model.x} == 1`, and `== 'value'` for strings.
- **`next` fans out to ALL natural outgoing edges (verified, session ws-697490-4).** A return of `next` — from `ELSE: next`, `THEN: next`, or a statement-less skill node — traverses **every** natural outgoing edge, exactly like a no-skill fan-out node (probe: a decision with edges to both `a` and `b` ran *both*). So a decision meant to continue to **one** node must have **exactly one** natural outgoing edge; route the alternatives as **named jump targets** (`THEN: x`), which need no inbound edge — and must **not** *also* carry a natural edge, or `next` will traverse them too. (This caused an early-completion bug: a loop head wired naturally to both its loop body and its exit node fanned `next` into the exit, completing the graph after one item.)

### COMPUTE
```
statement[]=COMPUTE: variable -> expression
```
- Result stored as `{node-alias}.result.variable`
- Route via MAPPING: `statement[]=MAPPING: node.result.variable -> model.target`
- The right-hand side is evaluated as JavaScript (`graph.js`) after `{path}` substitution. **Multi-line COMPUTE works**: in `GraphJs.compute` (`skills/GraphJs.java:180-195`) the statement text is passed to `context.eval(JS, text)` with its newlines intact; the `command.replace('\n',' ')` flatten in `processCommands` (`GraphJs.java:161-165`) runs *after* `compute()` and on a separate copy, so it never touches a COMPUTE expression. A COMPUTE may therefore be written on one physical line or wrapped across several lines with `'''` — both reach the JS engine identically. Complex IIFEs (nested object literals, `.filter`/`.reduce`) are ordinary JavaScript and evaluate normally.

#### `{path}` substitution rule (source: `GraphLambdaFunction.substituteVarIfAny`/`replaceWithParameter`, lines 208-261)
Substitution scans the right-hand side for `{…}` groups (`Utility.extractSegments`/`findEndBracket`, `Utility.java:1316-1356`) and resolves each group's inner text as a state-machine path **only when that text contains no colon, newline, tab, or carriage return** (the guard at `GraphLambdaFunction.java:238`). If it contains any of those, the engine assumes the braces are a JavaScript function body or a JSON object and leaves the whole group **verbatim**. Scanning is **innermost-first** (`extractSegments` walks right-to-left via `lastIndexOf`), so:
- A plain `{model.x}` resolves wherever it sits — inline, or nested inside an object literal or IIFE body — because the inner ref is found and substituted before any enclosing brace is examined.
- An object literal such as `{tag: {model.x}}` keeps its outer braces (their inner text holds a `:`) as a JS object while the inner `{model.x}` still resolves.
- **Footgun:** a lone `{path}` that shares its braces with a colon/newline (e.g. `{model.x : 0}`) hits the line-238 guard and is left verbatim; `eval` then receives a literal `{…}`, which is invalid JavaScript, and the node **halts silently** — the same failure mode as any unresolved/bad `graph.js` expression. Keep each `{path}` ref in its own clean braces.

### MAPPING (inline)
```
statement[]=MAPPING: source -> target
```

### EXECUTE / RESET / NEXT / DELAY
```
statement[]=EXECUTE: another-evaluator   # merge that evaluator's statements into THIS node and run them here
statement[]=RESET: node-to-reset         # clear a node's traversal state so it can run again (loops)
statement[]=NEXT: specific-node          # override traversal: jump to this node
statement[]=DELAY: milliseconds          # pause this node's completion
```
- **`EXECUTE`** (verified) merges another *evaluator's* `statement` list into the current node and runs them in the current node's context — a merged `COMPUTE` lands under `{this-node}.result.*`, **not** the source node's, and the source node is not traversed. It does not run a mapper.
- **`NEXT`** (verified) jumps traversal to the named node (overrides natural edges; the target needs no inbound edge).
- **`DELAY`** (verified) pauses the node's completion by the given milliseconds.
- **`RESET`** (source-verified) clears the named node(s) — `GraphMath.resetNodes` removes them from the `nodeSeen` set — so a bounded loop can re-traverse them; always pair with an `IF` exit condition. The runtime does enforce a tight-loop guard (source-verified): `GraphExecutor`/`GraphTraveler.checkFrequency` aborts with `400 "executed too frequently"` when a node is hit more than `graph.node.high.frequency` (default 10) times within `graph.max.loop.interval` (default 1000ms) — a backstop, not a substitute for the exit condition.
- **A loop must RESET its own re-entry head, not just the body (verified).** A non-join node runs **once** unless reset, so `NEXT`-ing back to an already-walked decision head does nothing — the loop runs exactly one pass and silently stops. Put the head in its **own** `RESET` list: a loop head `L` that re-runs body node `B` uses `RESET: B, L` — it resets *itself* as well as the body; omitting `L` freezes the loop after one pass. Reset every node on the cycle you intend to re-traverse, the head included.

### BEGIN / END (iterative block)
```
statement[]=BEGIN
statement[]=...statements to iterate...
statement[]=END
```

> **Verification status:** `IF/THEN/ELSE`, `COMPUTE`, inline `MAPPING:`, `NEXT`, `DELAY`, and `EXECUTE` are **execution-verified**. `RESET` and `BEGIN/END` are **source-verified** (read from `GraphMath` / `GraphLambdaFunction`): `RESET` clears nodes for loops; `BEGIN/END` bound which statements iterate under `for_each` (and `for_each` itself is execution-verified).

---

## Reserved Skill Properties

For nodes with a `skill`, these properties are treated as skill configuration, not initialized as ordinary node-local state: `skill`, `mapping`, `statement`, `input`, `output`, `feature`, `exception`, `extension`, `status`, `error`, `dictionary`, `for_each`, `concurrency`, `purpose`.

### exception (verified)

`exception={handler-node-alias}` on a `graph.api.fetcher` (or `graph.extension`) routes traversal to the handler when the call fails. On failure the skill records, under the node alias, `status` (HTTP code), `error` (response body), and `target` (the dictionary/graph), then jumps to the handler — usually a mapper or `graph.math`/`graph.js` that sets fallback state and rejoins the flow.

- **Without** `exception`, a fetcher failure stops forward traversal **and copies the upstream error body into `output.body`** (e.g. `{ "error": "upstream_unavailable" }`) — it is not a silent/empty abort.

### for_each + concurrency (verified)

`for_each` drives iterative execution for `graph.api.fetcher`, `graph.extension`, `graph.math`, `graph.js`.

```
for_each[]=model.account_ids -> model.account_id
input[]=model.account_id -> account_id
```

- Left side must resolve to a list; right side must be a `model.*` path. One iteration per list item (verified: 3 ids → 3 calls).
- `concurrency={n}` parallelizes iterations (verified: iterations complete out of order).
- **Output aggregation:** `output[]=result.x -> model.collected[]` appends **once per iteration**, producing a nested array. To flatten parallel arrays under a model path into a clean list, use `f:listOfMap(model.{path})` (verified: parallel arrays → list of maps).

### extension (verified)

`extension={graph-id}` or `extension=flow://{flow-id}` selects the target for `graph.extension`; a single call requires at least one `input[]` mapping.

- **Success path (verified):** a deployed target returns its `output.body` as `{node}.result` with `{node}.status = 200`; an `output[]=result -> …` mapping then routes it. (Confirmed by calling the shipped `tutorial-1` → `result = "hello world"`, status 200.)
- **Target resolution (source-verified):** the target loads from the **deployed-graph location** — config `location.graph.deployed`, default `classpath:/graph/{id}.json`, **read-only**. This is a *different* store from `export`/`import`, which use `location.graph.temp` (default `file:/tmp/graph`). So `export graph as X` makes `X` importable but **not** callable as `extension=X` (→ `"not found"`, `400`). To make a graph extension-callable, deploy its JSON to the deployed location; a `classpath:` location requires a rebuild/restart — the Companion API cannot deploy at runtime.
- On failure, the error and status are copied to `{node}.status`/`.error` and to `output.body`.

---

## Data Dictionary Pattern

### Provider
```
create node example-api
with type Provider
with properties
purpose=External configuration service
url=${example-api-host}/config-rs-v1/records/{entity_id}/settings
method=GET
feature[]=oauth2-bearer
input[]=text(application/json) -> header.accept
input[]=entity_id -> path_parameter.entity_id
input[]=setting_key -> query.settingKeys
```

### Dictionary
```
create node config-threshold
with type Dictionary
with properties
purpose=record threshold setting
provider=example-api
input[]=entity_id
input[]=setting_key:threshold
output[]=response.Settings.configurations.Configuration[0].variations.Variation[0].value -> result.threshold_value
```

### Fetcher
```
create node fetcher-config
with type Fetcher
with properties
skill=graph.api.fetcher
dictionary[]=config-threshold
dictionary[]=config-flag
input[]=input.body.entity_id -> entity_id
output[]=result.threshold_value -> model.config.threshold
output[]=f:defaultValue(fetcher-config.result.field, int(0)) -> model.config.field
```

### Connection pattern
```
connect root to dictionary with contains
connect root to fetcher-config with ask
connect dictionary to config-threshold with data
connect config-threshold to example-api with provider
connect fetcher-config to join-fetchers with wait
```

> **Minimal wiring (verified):** a single fetcher works with just Provider + Dictionary + Fetcher and one `connect {dictionary} to {provider} with provider` edge (plus the fetcher's `dictionary[]` property). The `dictionary` island container and its `contains`/`data` edges are organizational — useful for grouping many dictionaries — not required.

---

## Common Compositions

The engine does not define "graph types" — every graph is just a root, an end, and skill nodes wired together. The shapes below are conventional compositions, not categories the engine enforces. Mix building blocks freely to fit the use case.

### Orchestrator (data sourcing + delegation)
- An entry-point graph called by external callers
- Sources data, orchestrates sub-graph calls, and passes all data to the sub-graphs
- Flow: `root → parallel fetchers → join → extension(s)/evaluator(s) → end`

### Sub-graph (logic / evaluation)
- Receives all data via `input.body.*`; ideally does no data sourcing of its own (keeps it reusable and testable in isolation)
- Common forms: data retrieval/transformation, or conditional evaluation
- Flow: `root → mapper-input → evaluator chain → decision nodes → end`

### Reusable collection
- A graph holding reusable node definitions (e.g. shared Providers/Dictionaries), not executed directly
- Import nodes from it: `import node {name} from {source-graph}`

---

## Companion API Execution

### The request contract

Each command is one HTTP request:

```
POST /api/companion/{session-id}
Content-Type: text/plain

{single command, LF-separated lines}
```

- **Method / body:** `POST` with a `text/plain` body. The body is the entire command — multi-line is fine, but it must be exactly **one** command per request. The body is trimmed server-side and must not be empty.
- **Path parameter `{session-id}`:** the public session id. It is printed on the WebSocket console when a session starts (`session {id} started` / `Companion endpoint: /api/companion/{id}`), e.g. `ws-734563-3`.
- **Response — dispatch only:** a successful POST returns `200` with JSON `{"type":"companion","status":"accepted", ...}`. This confirms the command was *dispatched*, **not** that it succeeded. The command runs asynchronously and its textual output (the result of `describe`, `list`, `run`, `inspect`, `seen`, etc.) streams to the **WebSocket console only** — it is never in this HTTP response.
- **Errors:** `404` if there is no active session for the id; `400` if the body is empty or the id is missing.

Because command output goes to the WebSocket, a tool or agent driving the API over REST **verifies through the two GET endpoints**, not by reading the POST response:

| To check… | Use | Replaces the WebSocket-only command |
|-----------|-----|-------------------------------------|
| Nodes & connections (structural state) | `GET /api/graph/session/{session-id}` → full graph model JSON | `describe graph`, `list nodes`, `list connections` |
| One state-machine variable after a run   | `GET /api/inspect/{session-id}/{key}` → that value as JSON      | `inspect {key}` (e.g. `inspect output.body`) |

There is no REST equivalent for `seen` (traversal path) — that is WebSocket-only; infer the path from the resulting state instead.

### Execution rules

- One command per request; wait for each to complete before sending the next.
- The command ends at the HTTP body boundary — no terminator character needed.
- Do NOT include a literal `...` line — it causes `ERROR: Missing composite path`.
- The parser expects **LF (`\n`) line endings**. Files authored on Windows carry CRLF (`\r\n`); the stray `\r` gets attached to node names and produces `Invalid syntax` errors. Always strip `\r` from the body before sending.
- To test: `instantiate graph` → `run`, then `GET /api/inspect/{session-id}/output.body`.

### MANDATORY: verify after EVERY CRUD operation

**This rule is non-negotiable. It cannot be batched, deferred, or skipped.**

1. **One mutating command (`create` / `update` / `delete` / `connect` / `import`), then verify, then the next.** Do not send several CRUD commands and verify once at the end — a failure in command 2 silently corrupts everything built on top of it.
2. **`"status":"accepted"` is NOT success.** The POST is asynchronous and returns `accepted` even for a malformed or no-op command (e.g. `connect a to b` where `b` does not exist — the engine accepts it and quietly drops it). Proof a command took effect is to **re-fetch `GET /api/graph/session/{session-id}`** and confirm the change landed.
3. **For `create node` / `update node`, presence is necessary but NOT sufficient.** A malformed property line (e.g. an un-`'''`-wrapped `IF/THEN/ELSE`) is *accepted* but makes the engine **abort the entire property block** — the node is created, yet `properties` is empty (verified: probe session ws-697490-4). So confirming "the node exists" passes a hollow node. Re-fetch and confirm the **sent properties actually applied**: each `statement[]`/`mapping[]`/`input[]`/`output[]`/… entry is present on the stored node (a `'''` block is stored as one string with inner lines `\n`-joined and trimmed; single lines verbatim). The error itself goes only to the WebSocket console, never the POST response — so the re-fetch is the only HTTP-visible signal.
4. **If verification fails, stop and fix that command before sending anything else.** Never continue against an unverified graph.

The `companion.mjs send` helper (below) enforces all of this automatically — after any mutating command it re-fetches the live graph, asserts the change landed (including, for node create/update, that each sent property entry applied — `VERIFICATION FAILED … properties did NOT all apply` on a dropped entry), and exits non-zero if it did not. There is no flag to turn this off. Always send mutating commands through it rather than hand-rolling `curl`/`fetch`, which would skip the check.

### MANDATORY: verify the runtime after `instantiate` and `run`

CRUD verification proves the graph is *built*; it says nothing about whether it *works*. `instantiate` and `run` are not CRUD, so the structural check does not apply — **and they fail silently too.** A single rejected seed line (e.g. an unknown constant — there is no `number()`; use `double()`/`float()`) makes the engine drop the entire instance, after which every `inspect` returns `404`. The POST still returns `"accepted"`.

So you must **read state with `inspect` after every `instantiate` and every `run`** — do not trust the `"accepted"` response:

1. **After `instantiate`:** confirm a live instance exists — `GET /api/inspect/{session-id}/input.body` must return a JSON object (the seeded inputs), **not** `404`. A `404` means a seed line was rejected and the instance was dropped; fix the command before running.
2. **After `run`:** read the result — `GET /api/inspect/{session-id}/output.body` (or whichever key the graph writes). If the graph is meant to return a value and this is `404`/empty, the run did not complete; inspect node results (`{node}.result`) and use `seen` on the WebSocket to find where traversal stopped.

The `companion.mjs send` helper does both automatically: after `instantiate` it confirms the instance is live and prints the seeded `input.body` (hard-failing if the instance was dropped); after `run` it reads and prints `output.body` (warning loudly if absent). This puts the inspect result in the transcript, so "did you read inspect after the run?" is answered by evidence, not memory.

For a graph that returns via a non-default key (e.g. `output.header.status`) or whose value is a scalar leaf, assert it explicitly with one or more `--expect <key>` flags on the `run` send. Each declared key must exist or the run **hard-fails** (a declared output that is missing is unambiguous failure). `--expect` resolves scalar leaves correctly — the inspect endpoint only serves Map/List values, so the helper falls back to the parent container to confirm a scalar key like `output.body.verdict` is present.

### Recommended execution: the `companion.mjs` helper

Use the portable Node script [`companion.mjs`](./companion.mjs) (in this directory) to send commands and verify results. It runs anywhere Node.js (≥ 20) is installed, has **no npm dependencies**, and handles the footguns above automatically: it normalizes CRLF/CR to LF, rejects a literal `...` line, exits non-zero on HTTP errors, and — for every mutating command — performs the mandatory post-mutation verification described above (re-fetch + assert + `VERIFICATION FAILED` on mismatch).

```bash
# Send a command (inline)
node companion.mjs send {session-id} --command "connect root to mapper-input with execute"

# Send a command authored in a file (preferred for multi-line commands)
node companion.mjs send {session-id} --file ./commands/create-root.txt

# Send a command piped via stdin
echo "list nodes" | node companion.mjs send {session-id}

# Verify structural state (nodes + connections)
node companion.mjs graph {session-id}

# Inspect a state-machine variable after instantiate + run
node companion.mjs inspect {session-id} output.body

# Run and assert specific output keys exist (hard-fails if any is missing)
node companion.mjs send {session-id} --command "run" --expect output.body.verdict --expect output.header.status
```

The engine host defaults to `http://localhost:8085`; override it with `--host <url>` or the `MINIGRAPH_HOST` environment variable (e.g. when running on the test port `8090`).

A typical authoring loop is strictly one-command-at-a-time: `send` a single CRUD command — it auto-verifies and stops the build with a non-zero exit if the change did not land — then `send` the next only once the previous one is verified. After wiring is complete, `send "instantiate graph"`, `send "run"`, and `inspect output.body` to check the result.
