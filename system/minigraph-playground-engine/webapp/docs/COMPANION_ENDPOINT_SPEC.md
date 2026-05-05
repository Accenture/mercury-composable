# Companion REST Endpoint — Specification

Status: **Draft — Phase 1 (revised)**
Scope: `minigraph-playground-engine` (dev-only, `app.env=dev`)

---

## 1. Goal

Expose a REST endpoint `POST /api/companion/{id}` that accepts the same command
grammar currently processed by the WebSocket playground CLI (e.g. `create node`,
`update node`, `connect`, `run`, `inspect`, `export graph`, etc.), and delegates
execution to the existing `GraphCommandService` with **near-zero changes to
that service** (one read-only static accessor).

The REST endpoint is a *companion* to an already-open WebSocket playground
session: it provides a scriptable HTTP surface for issuing commands against
the same draft graph / graph instance that the operator is editing through
the browser. Primary intended consumer in Phase 1 is an **AI agent** acting
on behalf of the operator.

---

## 2. Design Principles (Load-Bearing)

These principles drove every decision in this spec and should be preserved in
any future modification:

1. **The Map body is the contract.** `GraphUserInterface` and
   `GraphCommandService` already communicate via a decoupled body shape
   inside an `EventEnvelope`: `{ type, in, out, message }`. The REST
   endpoint emits the *same* Map body to the same `EventEnvelope.setTo(ROUTE)`
   sink. That is what makes this feature possible without service-layer changes.

2. **Existing command-processing behavior is untouched.** `GraphCommandService`'s
   command dispatch, `GraphLambdaFunction`, and all skills behave identically.
   The only additions are (a) one new REST handler class, (b) one entry in
   `rest.yaml`, (c) one read-only static accessor on `GraphCommandService`,
   and (d) one additional outbound send to `txPath` in `GraphUserInterface`'s
   OPEN case so the operator/agent learns the session id. The OPEN-case send
   is additive — it does not modify existing OPEN behavior, only appends one
   message to the same `txPath` the WS already owns.

3. **Reuse the existing `id` ↔ `inRoute` convention.** The same transformation
   used by `UploadMockContent`, `UploadJsonContent`, and `InspectStateMachine`
   (via `GraphCommandService.uploadContent` / `downloadContent`):
   `inRoute = id.replace('-', '.') + ".in"`. The session id surfaced to the
   client at WS OPEN is simply the inverse of this transform applied to the
   server-generated route. No new identifier scheme is introduced.

4. **The WebSocket remains the console.** Command output continues to flow to
   the existing WebSocket `outRoute` (the browser playground CLI). The REST
   caller is a *command source*, not a *result sink*. Phase 2 will add
   synchronous result capture; Phase 1 preserves the current async model and
   keeps it bulletproof first.

5. **Single-operator assumption preserved.** `MiniGraph` instances in the
   `graphModels` map are not internally locked. The intended workflow is one
   operator per session, optionally with an AI agent acting on the operator's
   behalf via REST while the operator watches the browser. Concurrent mutations
   from REST and WS overlapping in wall-clock time are out of scope and will
   not be defended against in Phase 1.

---

## 3. Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  Operator opens browser → WebSocket /ws/graph/playground          │
│                                                                   │
│  GraphUserInterface OPEN handler now also sends:                  │
│    txPath ← { "type": "session", "id": "ws-<r>-<n>" }             │
│                                                                   │
│  Browser/agent reads the id and uses it for REST calls.           │
└──────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  HTTP client (curl, AI agent, test harness)                       │
│    POST /api/companion/{id}                                       │
│    Content-Type: text/plain                                       │
│    Body: "create node root\nwith type Root\nwith properties\n..." │
└──────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  NEW: rest/PostCompanionCommand.java                              │
│       @OptionalService("app.env=dev")                             │
│       @PreLoad("post.companion.command", instances=10)            │
│                                                                   │
│  Responsibilities (thin translator, no graph logic):              │
│    1. Extract path param "id"                                     │
│    2. Read raw body, require non-empty String                     │
│    3. Verify session exists via GraphCommandService.hasSession    │
│       → throws AppException(404, ...) if not                      │
│    4. Emit event to GraphCommandService.ROUTE with body:          │
│         { type: "command", in: <inRoute>, out: <outRoute>,        │
│           message: <command text> }                               │
│    5. Return HTTP 200 with small JSON acknowledgement             │
└──────────────────────────────────────────────────────────────────┘
                             │ fire-and-forget event
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  UNCHANGED behavior: services/GraphCommandService.java            │
│    - Receives identical body Map as from WebSocket                │
│    - Processes the command (single-line / multi-line / JSON)      │
│    - Emits responses via po.send(...) to outRoute                 │
│    (only addition: new public static hasSession(String id) method)│
└──────────────────────────────────────────────────────────────────┘
                             │ output messages
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  UNCHANGED: WebSocket txPath subscribed to outRoute               │
│    - Browser playground CLI displays messages                     │
│    - REST caller sees only the HTTP 200 ack (Phase 1)             │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Session Identifier Lifecycle

This section is new and load-bearing — it resolves the question of **how the
REST caller knows which `id` to use**.

### 4.1 Server-side route generation (unchanged platform behavior)

When a client connects to `/ws/graph/playground`, the WebSocket framework in
`platform-core` generates the session route at
`WsRequestHandler.handle(...)`:

```java
final int r = crypto.nextInt(100000, 1000000);
final int n = counter.incrementAndGet();
final String session = "ws."+r+"."+n;     // e.g. "ws.384729.17"
final String rxPath  = session + ".in";    // delivered to GraphUserInterface
final String txPath  = session + ".out";   // browser receives messages here
```

The `session` is server-generated; the path suffix in the WS URL plays no
role in it. The route `rxPath` is what gets stored as the key in
`GraphCommandService.graphModels` when the OPEN event fires.

### 4.2 Public-facing `id` (new)

The **public-facing session id** surfaced to clients is the inverse of the
existing `id.replace('-', '.') + ".in"` convention applied to `rxPath`:

```
rxPath:    "ws.384729.17.in"
strip .in: "ws.384729.17"
'.' → '-': "ws-384729-17"        ← this is the "id" used in /api/companion/{id}
```

This satisfies the URL-safe constraint (`a-z`, `A-Z`, `0-9`, `-`, `_`) already
documented for `/api/mock/{id}`, `/api/json/content/{id}`, and
`/api/inspect/{id}/{key}`. Round-trip is exact because the generated route
contains only digits and the literal `ws` prefix joined by `.` — no
ambiguity with `-`.

### 4.3 Welcome message (new — only client-visible behavior change)

`GraphUserInterface.handleEvent`, in the `WsEnvelope.OPEN` case, will send
one additional message to `txPath` immediately after registering OPEN with
`GraphCommandService`:

```java
String publicId = route.substring(0, route.length() - IN_SUFFIX.length()).replace('.', '-');
po.send(txPath, Map.of("type", "session", "id", publicId));
```

Where `IN_SUFFIX = ".in"` is a **new private constant declared locally in
`GraphUserInterface`** (see §6.2). Do **not** reuse `GraphLambdaFunction.IN`,
which is `"in"` (2 chars, no leading dot) and would produce a slice off by
one character. `WsRequestHandler`'s own `IN = ".in"` is `private` and not
re-exposed via `WsEnvelope`, so it cannot be imported either.

This is the **single mechanism** by which a browser, AI agent, or test
harness learns the id required for `/api/companion/{id}`. The frontend
already renders Map bodies (see e.g.
`GraphCommandService.handleInspectCommand` which sends `Map.of(INSPECT, key,
OUTCOME, value)` to `outRoute`), so no frontend code change is *required*
for the session id to appear in the CLI output — though a small frontend
enhancement to highlight the session id on connect would be a nice future
polish (out of scope here).

### 4.4 Lifetime — `graphModels` membership IS the session

`graphModels.put(inRoute, new MiniGraph())` happens on OPEN
(`GraphCommandService.handleCommand`, lines 126-128 of the current file).
`graphModels.remove(inRoute)` happens on CLOSE (lines 129-138). This map's
membership therefore corresponds 1:1 with WS connection lifetime.

The companion endpoint adopts **`graphModels` membership as the definition
of "session exists"**. Rationale:

- It matches the WS connection lifetime exactly.
- It is broad enough to permit draft-graph commands (`create node`,
  `connect`, `export graph`, `import graph from ...`) which only require a
  draft model and not an instantiated graph.
- The narrower alternative (`graphInstances` membership, used internally by
  `uploadContent`/`downloadContent`) would reject all pre-`instantiate
  graph` commands, defeating most of the use case.
- It does not change which map any existing endpoint reads.

`graphInstances`-only commands (`run`, `execute`, `inspect`, `seen`,
`clear cache`) still emit the standard `Graph instance <id> not started`
error to `outRoute` if dispatched prematurely — that error path lives in
`GraphCommandService` and is unchanged.

---

## 5. API Contract

### Request

```
POST /api/companion/{id}
Content-Type: text/plain
```

| Element | Type | Required | Notes |
|---|---|---|---|
| `id` (path) | string | yes | Public session id obtained from the WS welcome message at §4.3. URL-safe: `a-z`, `A-Z`, `0-9`, `-`, `_`. Mapped to `inRoute` via `id.replace('-', '.') + ".in"` — the same convention used by `/api/mock/{id}`, `/api/json/content/{id}`, and `/api/inspect/{id}/{key}`. |
| body | text | yes | A single command in the exact grammar accepted by the WebSocket CLI. May be multi-line. Must be a non-empty `String`. |

### Response — Success (200 OK)

```json
{
  "type": "companion",
  "status": "accepted",
  "id": "ws-384729-17",
  "message": "Command dispatched to graph.command.service. Output streams to the WebSocket console for this session."
}
```

### Response — Errors

| HTTP | When | Mechanism |
|---|---|---|
| 400 | Missing `id` path param, missing/empty body, or body is not a `String` | `throw new IllegalArgumentException("...")` — framework translates to 400 (matching `UploadMockContent` / `UploadJsonContent` patterns) |
| 404 | No active session for `id` (no entry in `graphModels` for the derived `inRoute`) | `throw new AppException(404, "No active session for id <id>")` — matching the 404 pattern used at `InspectStateMachine.java:50` |

Both error responses use the framework's standard `{type, status, message}`
envelope translation — no new error plumbing introduced.

### Command Grammar

The body is passed verbatim (after `trim()`) as the `message` field to
`GraphCommandService`, so **every command accepted by the WebSocket CLI is
automatically accepted here**. Examples:

Single-line:
```
run
```
```
inspect model
```
```
connect a1 to a2 with next
```

Multi-line:
```
create node root
with type Root
with properties
foo = bar
```
```
update node foo
with type End
with properties
bar = baz
```

JSON commands (the `{...}` form handled by `handleJsonCommand`) are accepted
verbatim but of limited utility via REST in Phase 1.

---

## 6. Component Detail — `PostCompanionCommand.java`

**Location**: `src/main/java/com/accenture/minigraph/rest/PostCompanionCommand.java`

**Package**: `com.accenture.minigraph.rest`

**Class contract**:
- Implements `TypedLambdaFunction<AsyncHttpRequest, Object>` (matching all
  peer REST handlers in this package)
- Annotated `@OptionalService("app.env=dev")` — dev-only, matching peers
- Annotated `@PreLoad(route = "post.companion.command", instances = 10)` —
  matching the `instances = 10` used by `UploadMockContent`,
  `UploadJsonContent`, and `InspectStateMachine`

**Dependencies (imports)**:
- `com.accenture.minigraph.services.GraphCommandService` — for the `ROUTE`
  constant and the new `hasSession(String id)` accessor (§6.1)
- `org.platformlambda.core.annotations.{OptionalService, PreLoad}`
- `org.platformlambda.core.exception.AppException`
- `org.platformlambda.core.models.{AsyncHttpRequest, EventEnvelope, TypedLambdaFunction}`
- `org.platformlambda.core.system.EventEmitter`
- `java.util.Map`

**Logic outline** (`handleEvent`):

```java
public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
    var id = input.getPathParameter("id");
    if (id == null) {
        throw new IllegalArgumentException("Missing path parameter: id");
    }
    if (!(input.getBody() instanceof String raw)) {
        throw new IllegalArgumentException("Body must be a non-empty text/plain command");
    }
    var command = raw.trim();
    if (command.isEmpty()) {
        throw new IllegalArgumentException("Body must be a non-empty text/plain command");
    }
    if (!GraphCommandService.hasSession(id)) {
        throw new AppException(404, "No active session for id " + id);
    }
    var route    = id.replace('-', '.');
    var inRoute  = route + ".in";
    var outRoute = route + ".out";
    EventEmitter.getInstance().send(new EventEnvelope()
            .setTo(GraphCommandService.ROUTE)
            .setBody(Map.of(
                "type",    "command",
                "in",      inRoute,
                "out",     outRoute,
                "message", command)));
    return new EventEnvelope()
            .setHeader("Content-Type", "application/json")
            .setBody(Map.of(
                "type",    "companion",
                "status",  "accepted",
                "id",      id,
                "message", "Command dispatched to graph.command.service. " +
                           "Output streams to the WebSocket console for this session."));
}
```

Notes on the sketch:

- `instanceof String raw` covers both null and wrong-type cases in one check
  (a `null` body fails the pattern test). No NPE possible.
- The empty-string check after `trim()` catches `"   "` and `"\n"` bodies.
- `EventEmitter.getInstance()` is used (not `PostOffice`), matching
  `GraphUserInterface`'s established pattern. See §10.4 for the trace
  consequence.
- Default response status is 200; no `setStatus()` call required.

### 6.1 Minimal addition to `GraphCommandService`

This is the only concession to not-quite-zero changes. The REST handler
must check whether a session exists without leaking the
`id ↔ inRoute` transform into the REST package.

**Add** (to `GraphCommandService`, alongside the existing `uploadContent` /
`downloadContent` static accessors):

```java
public static boolean hasSession(String id) {
    var inRoute = id.replace('-', '.') + ".in";
    return graphModels.containsKey(inRoute);
}
```

Rationale:

- Takes the **raw `id`** (not the pre-transformed `inRoute`), exactly
  mirroring the signatures of `uploadContent(String id, Object content)` and
  `downloadContent(String id, String key)` already in the file. The
  `id ↔ inRoute` transform stays encapsulated in `GraphCommandService`,
  consistent with the existing accessors.
- Read-only, no behavior change to any code path.
- Three lines of code; no fields touched.

The alternative — extending `GraphLambdaFunction` from the REST handler to
reach `protected static graphModels` directly — is rejected because no other
REST handler does this and it would tangle the REST package into the
graph-lambda hierarchy.

### 6.2 Welcome message addition to `GraphUserInterface`

Declare a new private constant at the top of the class (adjacent to
`GRAPH_COMMAND_SERVICE`):

```java
private static final String IN_SUFFIX = ".in";
```

Inside the existing `WsEnvelope.OPEN` case in `handleEvent`, send the
welcome message **after** `po.send(... GRAPH_COMMAND_SERVICE ...)` — that
ordering guarantees `GraphCommandService` has registered the session's
`MiniGraph` before the client is told about it. Placement relative to the
existing `log.info("Started ...")` line is a judgment call; this
implementation places the welcome send *after* the log line so the existing
"Started ..." entry remains the first log emitted for the new session.

```java
// Surface the public-facing session id to the REST companion / browser client.
// Inverse of the id.replace('-', '.') + ".in" convention used by REST endpoints.
// ".in" is invariant — WsRequestHandler hardcodes it as a private constant and
// does not re-expose it via WsEnvelope. Do NOT reuse GraphLambdaFunction.IN,
// which is "in" (2 chars) and would slice off by one.
var publicId = route.substring(0, route.length() - IN_SUFFIX.length()).replace('.', '-');
po.send(txPath, Map.of("type", "session", "id", publicId));
```

The `route` value here is the `rxPath` the framework supplied via
`WsEnvelope.ROUTE`, always of the form `"ws.<r>.<n>.in"`, so the
`length() - IN_SUFFIX.length()` slice is exact. Three additions: one
private constant declaration plus two executable lines inside the OPEN
case. No other change to the file.

---

## 7. `rest.yaml` Addition

Add exactly one entry, mirroring the shape of `upload.mock.content`:

```yaml
  - service: 'post.companion.command'
    methods: ['POST']
    url: '/api/companion/{id}'
    timeout: 30s
    cors: cors_1
    headers: header_1
    tracing: true
```

No other YAML files need changes. No `flows.yaml` entry needed — this is a
direct service invocation, not a flow.

---

## 8. Response Semantics (Phase 1)

### Where output appears

When a command is dispatched via REST:
- The REST caller receives an immediate `200 OK` with the ack JSON.
- Every `po.send(... outRoute ...)` emitted by `GraphCommandService` during
  command processing is delivered to the **WebSocket `txPath`** subscribed to
  `outRoute`.
- The user watching the browser playground CLI sees the command's output as
  if someone else typed the command at their terminal.

### Timing / ordering

- The REST handler returns before `GraphCommandService` has finished
  processing. This is *by design*; it preserves the current fire-and-forget
  pattern of `EventEmitter.send`.
- The REST caller has **no direct signal** that the command succeeded or
  failed. Even syntactically invalid commands return 200, because validation
  happens inside `GraphCommandService.handleEvent`'s
  `catch (IllegalArgumentException | IOException | InvalidPathException)`
  block, which sends an `"ERROR: ..."` string to `outRoute`.
- This asymmetry is acknowledged and accepted for Phase 1. Phase 2 (§9)
  will add opt-in synchronous result capture for callers without a WS.

### Behavior for `run` / `execute` / `instantiate graph`

These commands are long-running or async by nature. Phase 1 treats them the
same as any other command: dispatch and return 200. Output (including node
traversal updates from `GraphTraveler`) flows to the WebSocket.

---

## 9. Phase 2 — Synchronous Result Capture (Planned, Not Implemented)

Phase 1 is intentionally observation-via-WS only. Phase 2 will add opt-in
synchronous result capture so callers without a WebSocket (scripts, tests,
CI jobs, isolated agents) can read command output from the HTTP response
body. Sketch for later reference:

- Add query parameter `?sync=true` (default `false` preserves Phase 1
  behavior).
- When `sync=true`, the REST handler:
  - Generates a unique collector route, e.g. `companion.response.<uuid>`.
  - Registers a temporary `LambdaFunction` on that route that appends each
    received body to a list and signals completion after a quiet period
    (e.g. 500 ms idle) or an absolute cap (e.g. 5 s).
  - Sets the outgoing envelope's `out` to the collector route instead of
    the WS `outRoute` — OR dual-fans to both (dual delivery mode).
  - Returns the collected list in the HTTP response body.
- Excludes `run` / `execute` commands from sync mode (they can stream for
  many seconds and do not fit a bounded response window). Those still
  return 200 and route to the WebSocket.

Phase 2 is intentionally out of scope for this ticket. Nothing in Phase 1
forecloses Phase 2: the body Map's `out` field is the sole coupling point,
and substituting a collector route is trivial.

The reason for splitting is to keep Phase 1 surgical: get the dispatch path
and the session-id handshake bulletproof first, then layer sync capture on
top once the foundation has soaked.

---

## 10. Constraints, Caveats & Gotchas

### 10.1 Dev-only

`@OptionalService("app.env=dev")` means this endpoint is not registered in
production builds. Same as `GraphCommandService`, `GraphUserInterface`,
`UploadMockContent`, `UploadJsonContent`, `InspectStateMachine`. If the
endpoint must ever be enabled in production, that is a separate decision
that must include auth / rate-limiting / audit logging, none of which the
playground currently has.

### 10.2 Concurrency — single-operator expectation

`MiniGraph` and `GraphInstance` are mutated without internal locking. The
intended workflow is one operator per session, optionally augmented by an
AI agent acting on the operator's behalf via REST while the operator
watches the browser CLI. **Concurrent mutations** from REST and WS that
overlap in wall-clock time are out of scope and not defended against.

In practice this is a non-issue:
- Realistic usage is sequential — the operator types a command, waits for
  the CLI to redraw, then either continues typing or hands control to the
  agent. The agent likewise waits for visible output before issuing the
  next command.
- A human typing a command at the same wall-clock instant the agent
  dispatches one would have to be deliberate.
- The dev playground is single-user by nature.

If concurrent mutation is ever observed and causes corruption, the
targeted fix is a `ReentrantLock` in `GraphCommandService` keyed by
`inRoute` — but *not* preemptive work for Phase 1.

### 10.3 Session must already be open

Phase 1 returns 404 if `graphModels` does not contain an entry for the
derived `inRoute`. The session is created by the WebSocket OPEN flow and
torn down by the WebSocket CLOSE flow; there is no way to open a session
from the REST side in Phase 1.

**Operational consequence**: a REST consumer must either (a) be the
operator's own browser, which obtains the id from the welcome message
described in §4.3, or (b) be a test harness / agent that opens its own
WebSocket to `/ws/graph/playground`, reads the `{"type":"session", "id":...}`
message from the first inbound frame, and uses that id for subsequent REST
calls.

### 10.4 Tracing

The `rest.yaml` entry sets `tracing: true`, so the REST entry point gets a
trace id from the framework. The inner `EventEmitter.getInstance().send(...)`
does **not** propagate that trace id into `GraphCommandService` — this
matches `GraphUserInterface`'s pattern (see the comment at the top of its
`handleEvent`: *"EventEmitter can be used instead of PostOffice when tracing
is not required"*). Switching to `PostOffice` would propagate tracing but
diverge from the WS pattern. Phase 1 stays consistent with the WS path.

If end-to-end trace correlation becomes important later, the change is
small: construct `new PostOffice(headers, instance)` in the REST handler
and use it instead of `EventEmitter.getInstance()`. No envelope shape
change required.

### 10.5 TOCTOU between REST dispatch and WS close

Between the `hasSession(id)` check in the REST handler and the moment
`GraphCommandService` processes the dispatched command, a concurrent WS
CLOSE could remove the entry from `graphModels` and `graphInstances`. The
command would then run against a removed model and either no-op or surface
an error to a now-defunct `outRoute`. Probability is low and consequence is
benign (the WS is gone — nobody is watching anyway). Not defended in
Phase 1.

### 10.6 Body parsing

The REST framework populates `AsyncHttpRequest.getBody()` based on
`Content-Type`. For `text/plain`, the body arrives as a `String`; for
`application/json` as a `Map` or `List`. The handler accepts only `String`
and rejects everything else with `IllegalArgumentException` → 400. The
`instanceof String raw` pattern in §6 is null-safe (a null body fails the
pattern match).

### 10.7 No multi-command batching

One command per POST. The command grammar uses newlines as significant
whitespace within a single command (multi-line `create node` blocks), so
a batch separator would require a new grammar token. Deferred — can be
added later without breaking existing callers.

### 10.8 Public id format invariants

The public id is generated as `"ws-<6 digits>-<positive int>"` from the
server-side route `"ws.<r>.<n>.in"`. The reverse transform
(`replace('-', '.')` + `".in"`) is exact because the generated route
contains only digits and the literal `ws` token joined by dots — no
`-` to confuse the round trip. If the WS framework ever changes its route
generator to emit characters that conflict with this convention (e.g.
literal `-` or `.` in the random component), the welcome-message derivation
in §4.3 must be revisited.

---

## 11. Files Changed / Added

| Change | File | Approximate size |
|---|---|---|
| **New** | `src/main/java/com/accenture/minigraph/rest/PostCompanionCommand.java` | ~75 lines incl. license header |
| **New entry** | `src/main/resources/rest.yaml` | +8 lines |
| **Minimal addition** | `src/main/java/com/accenture/minigraph/services/GraphCommandService.java` | +4 lines (one `public static boolean hasSession(String id)`) |
| **Three-line addition** | `src/main/java/com/accenture/minigraph/websocket/server/GraphUserInterface.java` | +1 private constant (`IN_SUFFIX`) and +2 lines inside the existing OPEN case (welcome message with public id) |

**Nothing else is touched.**

---

## 12. Test Plan

Add tests under `src/test/java/com/accenture/minigraph/playground/` alongside
the existing `RestEndpointTest`. Each test that needs a session first opens
a WebSocket to `/ws/graph/playground`, captures the `{"type":"session",
"id":...}` welcome message, and uses that captured id for `/api/companion/{id}`.

A small reusable test helper is recommended:

```
SessionHandle openSession() {
    // 1. Connect WS to /ws/graph/playground
    // 2. Block until the first inbound text frame arrives
    // 3. Parse the JSON, assert type == "session", extract id
    // 4. Return a handle exposing { id, ws, receivedMessages }
}
```

### 12.1 Welcome message

1. Open a WS to `/ws/graph/playground`.
2. Assert the first inbound text frame is JSON `{"type":"session","id":"ws-<digits>-<digits>"}`.
3. Assert the id matches the regex `^ws-\d{6}-\d+$`.

### 12.2 Happy path — single-line command

1. Open a session via the helper; capture `id`.
2. POST `create node root\nwith type Root\nwith properties\nfoo = bar` to
   `/api/companion/{id}` with `Content-Type: text/plain`.
3. Assert HTTP 200 with body `{ type: "companion", status: "accepted", id, message: ... }`.
4. Assert the WebSocket subsequently receives a confirmation message that
   `node root` was created (exact wording per the current
   `handleCreateNodeCommand` output — verify against source at test-write time).

### 12.3 Happy path — two commands in order

1. Open a session.
2. POST a `create node` multi-line command.
3. POST `describe graph`.
4. Assert the WS receives both responses in dispatch order.

### 12.4 Error — missing session (404)

1. POST to `/api/companion/ws-000000-0` (non-existent id) with any valid
   command.
2. Assert HTTP 404 with message `"No active session for id ws-000000-0"`.

### 12.5 Error — empty body (400)

1. Open a session.
2. POST empty body to `/api/companion/{id}`.
3. Assert HTTP 400 with message `"Body must be a non-empty text/plain command"`.

### 12.6 Error — non-text body (400)

1. Open a session.
2. POST `Content-Type: application/json` with body `{"foo":"bar"}` to
   `/api/companion/{id}`.
3. Assert HTTP 400 with the same message as 12.5 (the handler rejects
   non-`String` bodies).

### 12.7 Error — command syntax fails inside GraphCommandService (200, ERROR on WS)

1. Open a session.
2. POST `not a real command` to `/api/companion/{id}`.
3. Assert HTTP 200 (dispatch succeeded; the REST surface does not validate
   grammar).
4. Assert the WS subsequently receives `"Please try 'help' for details"`
   (the `TRY_HELP` fallback emitted by `handleCommandPartThree`).

### 12.8 Run command streams to WS

1. Open a session, build a minimal graph via REST, POST `instantiate graph`.
2. POST `run` to `/api/companion/{id}`.
3. Assert HTTP 200 immediately.
4. Assert the WS receives node-traversal updates from `GraphTraveler`.

---

## 13. Open Questions Deferred to Phase 2 or Later

- Synchronous result capture (Phase 2, §9).
- REST-only sessions (no WS required).
- Batch commands per POST.
- Auth / rate limiting (would gate any production deployment).
- Per-session concurrency locking.
- Frontend enhancement to highlight the session id on connect (§4.3 sends
  the id as a Map; the existing CLI will render it but a styled badge would
  be nicer).

None of these block Phase 1.
