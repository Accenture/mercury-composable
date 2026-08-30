# Architectural Decision Records

A human-facing ledger of the **durable architecture decisions** behind Mercury
Composable — one entry per decision, capturing the *why* (context, alternatives,
consequences) rather than the *what that holds now*. The live constraints themselves are
maintained in the project's working memory (`memory/continuity.md` →
*Architectural Invariants* / *Key Decisions*); each ADR cross-links to the constraint it
formalizes via a `formalizes:` pointer, and each such constraint carries a matching
`(ADR-NNNN)` tag. This ledger is read **on demand** — it is not part of any per-session
read path.

Entries are listed **newest first**. Numbering is monotonic and entries are **never
deleted**: a decision that no longer holds is marked *Superseded* (replaced by a newer
ADR) or *Deprecated* (no longer relevant), with its text left in place. ADR-0001 is the
foundational decoupling decision; the rest build on it.

ADR-0001 to 0005 were seeded as a **retrospective** in 2026-06-22 from the decisions already
governing the codebase — verified against the source (`platform-core`,
`event-script-engine`, `minigraph-playground-engine`) and the published guides, which are
the source of truth in case of ambiguity. The narrative design reasoning behind each decision lives
in that ADR's own *Rationale* section.

---

## ADR-0020 — Route pools: numbered singleton lanes as a first-class platform registration {#adr-0020}
**Status:** Proposed · **Date:** 2026-08-30 · **Serves:** vision-mercury-composable · **Formalizes:** route-pool-registration-design
<!-- id: adr-0020 | status: proposed -->

**Abstract.** `Platform.registerRoutePool(prefix, lambda, count)` registers a set of
private singleton routes `{prefix}.{n}` for n = 0 to count-1 and returns the member
names in order; `releaseRoutePool(prefix)` removes the set symmetrically. Each member is
a strict FIFO lane (instances=1, one shared stateless lambda), so a caller that checks
out a lane gets per-conversation event ordering while other lanes serve concurrent
traffic — the pattern the HTTP edge's SSE reply lanes (`async.http.response.stream.{n}`)
introduced, promoted from an open-coded loop in AppStarter to a platform API with
registry-level identity (a pool registry mapping prefix to lane count). Pools are always
private: lane checkout is an in-process rendezvous, so advertising members to the
service mesh would be meaningless. Registering an existing pool reloads it (the previous
member set is released first, house reload semantics); individual register/release calls
that touch a pool member are warned, never refused — range-checked, so a neighbor route
such as `{prefix}.10` beside a count-3 pool is never misclassified. Pool mutations are
atomic under a `ReentrantLock` (virtual-thread-friendly on Java 21).
`getLocalRoutingTable()` is deliberately untouched: it remains the truthful live
registry consumed by mesh route advertising and Spring autowiring; the compact pool
rendering stays display-only in the actuator (`compressRouteFamilies`).

**Rationale.** The v4.12.0 streaming milestone shipped the lane-pool pattern without an
abstraction: 500 `registerPrivate` calls, no release counterpart, and no way for the
platform to tell a pool from 500 coincidentally numbered routes; upcoming consumers
(graph-run streaming, wrapper relay pools under the AI SDLC work) would each re-open-code
it. Alternatives rejected: naming the API `registerStreams` (one character from the
existing `registerStream(String, StreamFunction)` with entirely different semantics —
and the abstraction is a pool of ordered lanes, not a stream); collapsing pool members
inside `getLocalRoutingTable()` (the collapsed display key is not a valid route name,
and the table has functional consumers — the Kafka mesh advertises from it, rest-spring-4
autowires from it); an `isPrivate` flag (no remote use case exists, and the platform
expresses privacy as method pairs, never booleans); renaming the lane family to
`http.response.stream.{n}` (the lanes are sibling instances of `async.http.response` —
renaming the child while the parent stays would split the `async.http.*` namespace
family, and the name already shipped in traces, actuator views and docs).

---

## ADR-0019 — The HTTP client consumes SSE progressively; Event-over-HTTP streams on the same call {#adr-0019}
**Status:** Accepted · **Date:** 2026-08-29 · **Serves:** vision-mercury-composable · **Formalizes:** async-http-client-sse-streaming-design
<!-- id: adr-0019 | status: accepted -->

**Abstract.** `async.http.request` consumes a `text/event-stream` response progressively
and relays it as the platform's own streaming protocol: one `x-event-stream: data`
envelope per upstream SSE event to the caller's reply route (the event's data as body,
`event:` name as `x-event-name`, head control - upstream status plus the SSE content
type - on the first envelope), `eof` on a clean end, and an in-band `exception` on idle
expiry or a mid-stream disconnect. Activation is explicit and standard: the request must
declare `Accept: text/event-stream`, the response must actually be SSE, and the request
must carry a `reply_to`; anything else keeps the buffered single-shot behavior. For a
stream, the request timeout becomes the per-read idle allowance (any upstream bytes,
keep-alive comments included, reset it). Payloads are never interpreted - provider
conventions such as `data: [DONE]` forward verbatim, keeping the client vendor-neutral.
Because the streaming producer contract is the one the HTTP edge already consumes, a
streaming endpoint's function can forward its own `reply_to` and correlation id into the
client call and the application becomes an SSE-to-SSE relay by configuration. The same
enhancement is the transport for Event-over-HTTP peer streaming (python/node wrapper
functions and engine⇄engine): the peer's `/api/event` answers the SAME call with an SSE
response using a hybrid control/data framing - control signals (the head, `eof`,
`exception`, and any segment that cannot round-trip as text) ride base64-encoded MsgPack
envelope frames under the reserved SSE event name `envelope`, while token segments ride
raw SSE frames with near-zero overhead - negotiated by the same Accept contract, so
version skew degrades explicitly, never silently.

**Rationale.** Progressive delivery had reached the HTTP edge (ADR-0018) but not the two
consumption paths AI-era workloads need: an engine function consuming an LLM provider's
token stream, and a polyglot wrapper function streaming results back to the engine. One
mechanism closes both because the Event-over-HTTP relay already flows through
AsyncHttpClient. Alternatives rejected: an on-demand WebSocket channel (breaks the
wrapper scope fence, grows four codebases, historically gateway-hostile, and drifts
toward the standing-connection mesh the framework keeps opt-in); per-segment POSTs back
to the reply lane (FIFO forces serialized posts - one round trip per token - and opens
an inbound path to reply lanes); gRPC/HTTP-2 push (a foreign dependency stack); and
long-polling (chatty and stateful). Streaming on the response of the engine's own
request adds no inbound surface, is ordered by TCP for free, and rides the wire shape
gateways already accommodate for LLM traffic.

---

## ADR-0018 — HTTP response streaming rides the multi-shot reply route; the wire stays standards-only {#adr-0018}
**Status:** Accepted · **Date:** 2026-08-28 · **Serves:** vision-mercury-composable · **Formalizes:** http-response-streaming-design
<!-- id: adr-0018 | status: accepted -->

**Abstract.** A function streams an HTTP response (LLM token segments, agent progress
events, live updates) by exercising the platform's native streaming pattern: the callee
sends a sequence of events to the caller-provided reply route until an
end-of-transmission signal. Each event carries the reserved **envelope** header
`x-event-stream: data | eof | exception` (the ObjectStream vocabulary); the marker is
internal protocol consumed by the REST automation edge — like `x-stream-id` and `x-ttl`,
it never appears on the wire. The public HTTP surface is standards-only: Server-Sent
Events framing when the content type is `text/event-stream` (typed events, a terminal
`done` event carrying trailing metadata, in-band `error` events, keep-alive comments),
chunked transfer with JSON Lines otherwise. A streaming endpoint is declared with
`stream: true` in rest.yaml, which checks out a dedicated ordered reply lane for the
request's lifetime — a single-instance route (`async.http.response.stream.{n}`) drawn
LIFO from a pool of 500 (the `async.http.response` concurrency), returned when the
request context closes — the "ready" signal pattern of the reactive manager/worker
design. All segments of one request ride its own lane (strict FIFO) while different
requests stream concurrently through their own lanes; an exhausted pool rejects further
streaming requests immediately with HTTP-503 (deterministic back-pressure, no
configuration knob). The first event commits the response head; each
arrival extends the idle timeout; stalls fail in-band; client disconnects turn late
segments into no-op drops; a bounded drain-aware buffer guards slow clients. Responses
without the marker are single-shot, exactly as before, and the legacy `x-stream-id`
relay is untouched.

**Rationale.** The prerequisite for AI-era workloads is progressive delivery over plain
HTTP — SSE is the de facto wire for chat token streams (OpenAI, Anthropic, Gemini and
every compatible server), agent progress protocols (MCP Streamable HTTP, A2A), and the
live-watch window of long-running workflows. The alternative — building on the existing
`Flux`/`x-stream-id` relay — was rejected: the producer API is Reactor-typed and
JVM-only (invisible to Event Script, knowledge graphs, and the polyglot wrappers, which
is exactly where LLM tokens will come from), structured segments buffered at the edge
instead of streaming, and the relay has no SSE framing or in-band terminal events. The
multi-shot reply route adds no new substrate — anything that can send an envelope to a
route can stream, which keeps the mechanism language-neutral by construction: flow
tasks, graph nodes, and Event-over-HTTP peers join by sending the same envelopes. A
custom HTTP header was rejected in favor of the envelope marker so the wire stays fully
standard (RFC 6648 discourages new X- wire headers; the envelope already has a reserved
x- vocabulary).

## ADR-0017 — Spring Boot integration targets Boot 4 only; the Boot 3 lane is retired {#adr-0017}
**Status:** Proposed · **Date:** 2026-08-27 · **Serves:** vision-mercury-composable · **Formalizes:** stack-integration-spring-boot4
<!-- id: adr-0017 | status: proposed -->

**Abstract.** The optional Spring Boot integration is provided by a single module,
`system/rest-spring-4` (with `examples/rest-spring-4-example` as its reference
application), targeting Spring Boot 4. The Spring Boot 3 lane — `system/rest-spring-3`
and `examples/rest-spring-3-example` — is removed from the reactor. The integration
surface carries over unchanged: the RestServer bootstrap, the `spring.boot.main`
override, `@PreLoad` autowiring, and the same configuration keys — so migrating an
application is a dependency swap plus that application's own Spring Boot 3 → 4 upgrade.
Spring remains optional and is never required by platform-core: the lightweight built-in
non-blocking HTTP server stays the default.

**Rationale.** The Spring community stopped issuing security patches for Spring Boot 3,
and field deployment pipelines enforce that directly: dependency security scanning
(Snyk) rejects Spring Boot 3 dependencies outright and requires Spring Framework 7 or
newer, so a build carrying the Boot 3 lane blocks deployment. Continuing to ship a
Boot 3 integration would hand field installations a permanently-unpatchable web
dependency lane — the opposite of the framework's security posture, where field
deployments are gated by dependency scanners (cf. the netty and lz4 remediation
rounds). Maintaining two lanes also doubled every Spring upgrade sweep
(both `spring-boot-starter-parent` versions, two example applications) while exposing
the same integration surface. The alternative — freezing `rest-spring-3` for legacy
consumers — was rejected: an EOL web stack is a liability regardless of freshness of the
rest of the build, and the migration cost is a dependency swap. Applications that must
stay on Spring Boot 3 can remain on Mercury releases up to this one.

## ADR-0016 — Polyglot functions are Event-over-HTTP peers, not subprocesses or ports {#adr-0016}
**Status:** Proposed · **Date:** 2026-08-22 · **Serves:** vision-mercury-composable · **Formalizes:** polyglot-event-over-http-design
<!-- id: adr-0016 | status: proposed -->

**Abstract.** Functions written in Python and Node.js join Event Script flows and
MiniGraph knowledge graphs as long-lived **Event API peers**: each official wrapper
([mercury-python](https://github.com/Accenture/mercury-python),
[mercury-nodejs](https://github.com/Accenture/mercury-nodejs)) hosts `POST /api/event`
with the engines' exact semantics and speaks the standard envelope wire format, verified
against the golden conformance vectors shared by the Java and Rust engines. The engine
addresses a polyglot route through the existing declarative `yaml.event.over.http` map —
non-blocking on the JVM (the relay is an interceptor; no thread is held per in-flight
call) — so a flow task or `graph.task` node calls a Python or Node.js function exactly as
if it were local, with trace context, the `my_cid` → `my_correlation_id` injection, and
the portable error contract (handler errors ride HTTP 200 with envelope status; transport
errors keep 400/403/404/408 with engine-identical messages) intact. The wrapper scope is
fenced: envelope codec, Event API host, `preload` registry, thin `PostOffice` client, a
primitive in-process event bus (per-route FIFO mailboxes with faithful `instances`; no
spill tier, no queue cap), the engines' actuator endpoints, the minimalist utilities
(configuration with the `resources/` convention and `-Dkey=value` overrides,
engine-format logging with `log.format=text|json|compact`, trace context), and a dev
runner — **no orchestration**: flows, graphs, persistence, and pub/sub stay on the
engines. The single engine change is the `graph.task` route-existence guard consulting
the Event-over-HTTP map (Java + Rust lock-step, shipped in v4.11.11). The wire
conformance vectors are the acceptance gate for every wrapper, and each wrapper release
extends the interop test report.

**Rationale.** The alternative designs were a full language port and an
engine-managed subprocess runner, and both were investigated. A full port re-implements
the composable core (event bus, flow engine, graph engine) per language — the Node.js
legacy port demonstrated the cost: it fell ~2 years behind and was retired. A
subprocess runner (functions as child processes over stdio) was prototyped and shelved:
on JDK 21 pipe I/O and `Process.waitFor` pin virtual-thread carriers, forcing
kernel-thread isolation per in-flight call, plus process-tree lifecycle management and
per-call interpreter startup — an operational stability surface the peer model does not
have, with the niche benefit (single-artifact embedded scripting) deferred until field
demand exists. The peer model reuses what already works: the function contract is
route-name + envelope (nothing in it is Java), the Event API endpoint already carries it
across instances and across the Rust engine, and the declarative map already abstracts
location. Keeping orchestration out of the wrappers preserves the architecture's one
boundary — the engine tier owns sequencing, retries, and back-pressure (a leaf host
fails fast by deadline instead of hoarding work) — and keeps each wrapper small enough
to stay in lock-step through a conformance suite rather than a porting effort.
Engine-consistent utilities and actuator endpoints are part of the decision, not
convenience: polyglot installations put every language's telemetry, logs, probes, and
dashboards in front of one DevSecOps team, so presentation parity is a field
requirement. The Node.js wrapper is also the sanctioned answer to the retired legacy
port — a fresh re-port was never going to stay current; a thin protocol wrapper can.

## ADR-0015 — AI discovery is a standalone composable app, not a runtime dependency {#adr-0015}
**Status:** Proposed · **Date:** 2026-08-21 · **Serves:** vision-mercury-composable · **Formalizes:** thread-ai-contract-provider
<!-- id: adr-0015 | status: proposed -->

**Abstract.** Mercury serves a version-matched operational contract for AI agents from a
standalone composable app, `system/ai-contract-provider`: read-only REST discovery endpoints
(port 8999) wired `rest.yaml` → Event Script flow → function, plus a local `--export` mode
that writes the offline `mercury-platform` Agent Skill with a hash manifest written last.
The dependency arrow points **into** the app — it depends on platform-core and
event-script-engine; no framework module depends on it, and no framework module changes.
Contract claims live in one `contracts.yaml` catalog whose behavior anchor classes are
resolved by `Class.forName` in the module's tests (MiniGraph anchors through a test-scope
dependency), so a renamed behavior class fails the reactor build. The served
`mercury_version` is read from the platform-core dependency's own `pom.properties`, and
startup refuses a mixed framework assembly.

**Rationale.** The motivating defect was documentation drift: the guides taught a
`rest.yaml` flow binding (`flow:` without `service:`) that the production `RoutingEntry`
parser rejects, and nothing tied the docs to the runtime. An earlier design packaged the
contract as a library that platform-core, event-script-engine, and the playground engine
all depended on, with ServiceLoader providers and playground chat commands. That inverted
the dependency graph (the lean core acquired a docs artifact), broke in Spring Boot
packaging (CodeSource-based resource reads), and leaked a Java-only command surface into
the playground webapp and command catalog that the Rust engine also ships — a cross-engine
parity break. A standalone app keeps the framework untouched, reads resources only through
the classloader (packaging-independent), keeps discovery off the shared playground surface
(no Rust lock-step obligation — the REST API itself is the portable contract), and is
itself a reference implementation of the composable pattern it documents. Since all
Mercury modules release together under one version, a build-verified static catalog gives
the same drift guarantee as runtime provider discovery with far less machinery; filesystem
export stays a local operator action rather than a remote capability.

## ADR-0014 — Generic exception context: one handler serves every `exception=` route {#adr-0014}
**Status:** Proposed · **Date:** 2026-08-10 · **Serves:** vision-mercury-composable · **Formalizes:** thread-field-graph-scoped-state-and-error-context
<!-- id: adr-0014 | status: proposed -->

**Abstract.** When a failed node routes to its `exception=` handler, the walkers
(GraphExecutor and GraphTraveler — one staging site each, covering every skill) stage a
**generic exception context** in the state machine: `error.source` (the failing node's
alias), `error.code`, `error.message`, and `error.stack` when the failing envelope
carries one. The names follow **Event Script's flow exception contract**
(`error.code/message/stack`) so both orchestration layers share one vocabulary; `source`
is the graph-side addition, since a graph handler — unlike a flow's — is a node that many
other nodes may name. The per-node error record (`{node}.status`/`{node}.error`, now plus
`{node}.stack`) is unchanged, so existing handlers keep working. `error` is a **reserved
node alias** (it always was, in the graph model's reserved-name list) — the namespace is a
first-class state-machine citizen like `model`, inspectable in a dry-run with
`inspect error`. A shared handler is island-anchored (`root → island → handler`, the
ADR-0012 anchoring idiom) because exception routing is a jump; it is entered at most once
per run unless RESET, and it may connect onward to further processing nodes.

**Rationale.** The field demo showed graphs cluttered with per-fetcher handler clones —
structurally forced, because the engine staged failures only under the failing node's own
scratch (`{node}.status`/`{node}.error`), so a handler's data mapping had to name its
failing node statically. Staging at the walkers' exception choke points (rather than in
each skill) covers `graph.api.fetcher`, `graph.task`, `graph.extension` and the
suspend/resume store failures in one move, with no per-skill drift. For an extension
node, `error.source` is the extension node in the parent graph — failures inside a
delegated subgraph route to that subgraph's own handlers, composing with ADR-0013's
self-containment. Event Script parity naming was chosen over graph-local naming
(`error.status`/`error.error`) for cross-layer consistency and because `error.error`
reads badly. The context is transient per run and never persisted across suspension.

## ADR-0013 — A business transaction spans graphs: workflow-state records are scoped by graph + cid, and delegation inherits the correlation ID {#adr-0013}
**Status:** Proposed · **Date:** 2026-08-10 · **Serves:** vision-mercury-composable · **Formalizes:** thread-field-graph-scoped-state-and-error-context · **Amends:** ADR-0010, ADR-0012
<!-- id: adr-0013 | status: proposed -->

**Abstract.** The suspend/resume state-store contract is scoped by **graph + cid**: the
persistence envelope gains a `graph` field (`{cid, graph, node, ttl, model, seen, run}`),
the retrieve body becomes `{cid, graph}`, and the Redis reference implementation keys
records `graph:{graph_id}:{cid}` (formerly `graph:state:{cid}`). Key composition remains
store-internal, but every store MUST scope by both — a cid-only key collapses all of a
transaction's suspensions into one record. Complementarily, **`graph.extension` stamps
the parent's business correlation ID** (`model.cid`) on the delegated call — both
protocols (`graph://` id and `flow://`), both branches (single and `for_each`) — exactly
as an Event Script sub-flow launch already did, closing the asymmetry where a subgraph's
`model.cid` was an unusable per-call random UUID. Together these make suspension
**self-contained per graph by construction**: one business transaction may suspend
independently in each domain's graph and in each subgraph, a resume only ever sees its
own graph's records, and an orchestrator parent can delegate independently resumable
subgraph paths (the documented orchestrator pattern, pinned by the
`unit-test-orchestrator`/`unit-test-sub-suspend` reference models). **Breaking change**
(accepted, dev-phase — no legacy fallback): records persisted under the old key are
invisible after upgrade and resume behaves as fresh; custom stores must adopt the
`graph` field.

**Rationale.** The field demo drove both halves: the same business correlation ID is used
across multiple domains, so `graph:state:{cid}` collided across domains sharing a Redis —
and their orchestrator use case (a parent graph delegating suspend/resume per processing
path) was structurally impossible, for two independent reasons the code study confirmed:
the key collision, and `graph.extension` minting a random UUID per call with no
business-cid header, so a subgraph's resume could never find its record even with scoped
keys. The graph ID is unique per domain, so `graph + cid` addresses both the domain and
the subgraph requirement with one convention. Scoping is enforced in the contract (the
reference stores fail fast on a missing `graph`) rather than by engine-composed opaque
keys, keeping key layout a store concern. The `for_each` caveat is documented, not
enforced: one graph × one cid = one record, so a suspendable subgraph is invoked once per
cid per run. ADR-0010's cid-as-capability note extends naturally: one cid now unlocks
resume in every graph that suspended under it, scoped per graph id, with endpoint
authentication unchanged.

## ADR-0012 — Suspension is a destination: edge-mode checkpoints and jump-mode decisions replace the `suspend=true` property {#adr-0012}
**Status:** Accepted · **Date:** 2026-08-07T22:50:34.000Z · **Serves:** vision-mercury-composable · **Formalizes:** thread-suspend-resume-rationalization · **Amends:** ADR-0010
<!-- id: adr-0012 | status: accepted -->

**Abstract.** A suspension point is declared by **how a node reaches the reserved
`suspend` node**, not by a node property — the `suspend=true` property is retired
(accepted and ignored for one deprecation window, with a compiler WARN). Two modes,
discriminated **purely by graph shape**: in **edge mode**, a working node with a drawn
edge to `suspend` redirects there when its skill completes normally — the drawn edge is
the declaration; the node must keep at least one continuation edge, where a resumed run
continues **without re-executing** the node (byte-for-byte the prior suspensible
behavior). In **jump mode**, a decision (`graph.math`) returns `suspend` from its
IF-THEN-ELSE; on resume the decision is **re-executed** against the new request input, so
it re-decides on every resume — a wait-on-invalid-input loop is one jump with no
auxiliary nodes. A routing-skill node must **not** draw an edge to `suspend` (its drawn
edges are outcome alternatives; the gate rejects the shape with a teaching error), the
`suspend` node cannot be an exception handler (`exception=suspend` rejected —
checkpoint-on-failure would smuggle in retry semantics), and a jump-only `suspend` node
is anchored behind an island (`root → island → suspend`) to satisfy the no-orphan export
rule — an island's outgoing edges are never traversed. The persisted record contract
(`{cid, node, ttl, model, seen, run}`) and the store put/get contract are **unchanged**.

**Rationale.** Two independent field teams hit the same conceptual wall within days: a
suspensible node ignores IF-THEN-ELSE routing and suspends unconditionally, which reads
as an inconsistency between decision-making and suspension — documenting the
decide-before-you-suspend rule (the first team's remedy) did not stop the second report,
so the constraint itself was the defect. The code study showed the property was only a
walker-level routing trigger — persistence was already predecessor-agnostic (the record
stores whoever routed in) and resume already continued along the persisted node's forward
links — and it was papering over two latent divergences: the documentation already
claimed "a plain edge into suspend is an unconditional suspension point" while the walker
actually fanned out (checkpoint and continuation in parallel), and a jump to `suspend`
half-worked but resume then fanned out a decision's outcome alternatives as if they were
branches. Shape discrimination was chosen over skill-class discrimination (both were
designed) because it is cheaper — one forward-link probe at resume, no statement
inspection at the gate — and because the classes coincide **by construction**: working
skills always return `next` so they can only be edge mode, and only routing skills can
jump, so re-execution can never touch a working node. Back-compat is structural: every
valid pre-change model necessarily drew its checkpoint edge (the prior gate required it),
so edge-inferred redirect reproduces the old behavior exactly and pre-change store
records replay correctly under the new engine — mixed-version fleets are safe in both
directions. Alternatives rejected: keeping the property alongside edge inference (two
declarations for one behavior — the confusion this fixes); statement inspection to let a
suspend-only decision pass the gate (computationally expensive and fragile); uniform
re-execution on resume (would double-execute working checkpoints, breaking the
at-most-once actor-step guarantee).

---

## ADR-0011 — CompileGraph is the mandatory deployment gate for graph models (CompileFlows parity) {#adr-0011}
**Status:** Accepted · **Date:** 2026-07-29T19:03:28.000Z · **Serves:** vision-mercury-composable · **Formalizes:** compilegraph-mandatory-gate
<!-- id: adr-0011 | status: accepted -->

**Abstract.** A deployed graph model is executable at `POST /api/graph/{graph-id}` **only**
when it is listed in the graph manifest (`graph.model.automation`) **and** passes the
CompileGraph quality gate at startup — a graph that fails the gate, or is not listed,
answers **HTTP-404 as if the model does not exist**, and the lazy, per-request loading of
deployed models is removed. Like `flows.yaml`, the manifest carries the location of its
own models (an optional `location` entry, default `classpath:/graph`) — there is no
separate application property. Validation follows **two explicit lanes**: *production* =
models → CompileGraph → deployed graphs → GraphExecutor, which **trusts the gate** and
drops per-request re-validation of gate-guaranteed rules, keeping only data-driven runtime
guards (store-record contents, dynamic jump targets, loop detection); *dry-run* = drafts
in the temp workspace → UI CLI input validation at node create/update → GraphTraveler with
full runtime validation. The gate's whole-graph rules live in a reusable
`GraphModelValidator`, which the playground's `run` command also invokes as a **pre-run
quality check** — draft authoring deliberately allows partial models, but the moment the
author asks to run, the contract must hold.

**Rationale.** This is the `CompileFlows` precedent applied to Layer 3: an invalid flow
never becomes executable, and the graph engine now gives the same guarantee — previously a
manifest graph that *failed* validation could still be resurrected by the lazy-load
fallback and executed unvalidated, which is untenable for field production. Compiled-or-404
(identical for failed and unlisted models) leaks nothing about why a model is absent, and
turning the deploy folder into a pure data directory removes it as a direct execution
vector. Startup-time rejection converts an entire class of runtime stalls and mid-run
errors (missing `end` node, checkpoint without a continuation edge, dead-end suspend node)
into immediate, logged deployment failures — while the same rules surface to graph authors
at dry-run `run` time, so the deployment contract is learned in the playground, not
discovered in the field. The consequences are accepted deliberately: the manifest is now a
**requirement** (a one-line migration for installations that relied on lazy loading, with
the `classpath:/graph` default preserving existing layouts and an obsolete-key warning for
the retired `location.graph.deployed` property), hot-dropping a JSON file into the deploy
folder no longer works (deployment is an explicit, restart-scoped act — consistent with
the governance lifecycle the Vision calls for), and the walkers' suspend/resume guards are
now exercised end-to-end only on the dry-run lane (the static validator carries the
per-rule coverage).

---

## ADR-0010 — Graph workflow suspension: short runs + an external state store, encapsulated in skills {#adr-0010}
**Status:** Accepted (amended by ADR-0012: the `suspend=true` declaration vocabulary is retired in favor of edge/jump modes; short runs, the store contract and the record envelope stand) · **Date:** 2026-07-29T02:00:00.000Z · **Serves:** vision-mercury-composable · **Formalizes:** graph-suspend-resume-design
<!-- id: adr-0010 | status: accepted | amended-by: adr-0012 -->

**Abstract.** A long-running business process with human checkpoints (approval,
intervention, inbox notification) is expressed as a **sequence of short graph runs**: at a
suspension point the run persists its workflow state — the `model` namespace plus
traversal bookkeeping — to an **external state store** keyed by the business correlation
ID with a designer-chosen TTL, then completes normally; a later request with the same
correlation ID restores that state and continues past the checkpoint without re-executing
it. The mechanics are **encapsulated in two skills** — `graph.suspend` and `graph.resume`,
supersets of `graph.task` that invoke a pluggable store function named by the node's
`task` property with a fixed put/get contract — so suspension nodes carry **no data
mapping**. The node alias `suspend` is **reserved** (the `root`/`end` pattern): traversal
routes to it by name when a node marked with the reserved property `suspend=true`
completes; node *types* (`Suspend`/`Resume`/`Suspensible`) remain visual convention —
**the skill defines behavior**. Store retrieval **consumes the record atomically**
(at-most-once resume); reserved model keys never persist; a suspension point must be the
sole active branch.

**Rationale.** Parking a live graph instance for a multi-day approval would pin memory,
defeat the flow ttl, and not survive a restart — the short-run model keeps the engine's
in-memory instance lifecycle untouched and makes cross-instance resume free (any pod
sharing the store can continue the workflow). Skill encapsulation was chosen over
node-level data mapping because the mapping variant required special-casing the mapping
grammar per node type and left the resume jump-target with no channel; a fixed store
contract also makes the persistence seam documentable and replaceable (Redis ships as an
optional extension module — never an engine dependency; engine tests use a temp-file
store). The reserved-alias routing reuses the existing jump-by-name directive vocabulary
instead of introducing edge classification, at the accepted cost of one suspend node per
graph. Consume-on-retrieve was preferred over keep-until-TTL so a duplicate resume cannot
double-execute a continuation; workflows needing stronger crash guarantees may implement
keep-until-ack semantics in a custom store. Alternatives rejected: engine-managed timers
or parked instances (memory + restart fragility); reusing the Event Script `ext:`
fire-and-forget external-state contract (durability requires a synchronous
acknowledgement); persisting `{node}.result` scratch (the model is the workflow's single
durable memory — an explicit, teachable rule).

---

## ADR-0009 — Registration metadata is a cross-language contract; carriers are per-language idioms {#adr-0009}
**Status:** Accepted · **Date:** 2026-07-26T01:40:00.000Z · **Serves:** vision-mercury-composable · **Formalizes:** registration-metadata-contract
<!-- id: adr-0009 | status: accepted -->

**Abstract.** Declarative registration — `@PreLoad` and its family (entry points, websocket
services, Event Script plugins, graph fetch features) — is governed by **one canonical
metadata model with fixed semantics**, specified in
`docs/guides/registration-metadata-contract.md` and proven by **golden vectors shared
verbatim** between engine repositories. How each language *carries* the metadata is an
idiom — Java annotations discovered by runtime classpath scan, Rust attribute macros
collected by link-time inventory, Python/Node decorators discovered by explicit
package/module walks — but the model and its semantics are the contract: attach at
definition / resolve at boot (`envInstances`); the `OptionalService` condition grammar;
order-free marker stacking; one conflict policy (explicit wins over declarative;
duplicates WARN + last-wins); extension-point naming (an explicit positional name, or
derivation from the declaration such that idiomatic declarations in every language yield
the same registered name); plugins are Event Script capabilities (flow vocabulary) and are
never conditionally gated, while features honor gating; the boot sequence
(discover → register → override → resolve → validate → route table); explicit
loud-failure discovery; and misuse as a first-class, tested error surface.

**Rationale.** The Rust port's first annotation pass proved that porting the *mechanism*
without fixing the *semantics* produces drift invisible to any single repository: built-ins
bypassing the extension points they exemplify, conflict policies diverging (skip-first-wins
vs last-wins), gating support absent where the reference has it, and an attribute
stack-order requirement Java never had. Each was individually small; together they meant a
developer — or an AI agent — could not transfer knowledge between engines, and every future
port would re-diverge independently. The same problem was already solved once for the wire
format (ADR-referenced spec + golden vectors, v4.10.0): fixing the contract in a
language-neutral artifact with executable conformance is what made the four-way interop
matrix provable. This ADR applies that method to the declaration surface. The maintainer's
two governing directives are part of the decision: developers must see **consistent,
decoupled** registration in every language, and the Rust port is the **best-practice
template** for the Python and Node ports.

**Alternatives.** (a) *Per-port judgment calls documented in each repo* — rejected: that is
the drift this ADR eliminates; N-of-1 documentation cannot be conformance-tested.
(b) *A shared runtime registry service* (as the original external blueprint's open item
suggested for multi-process parity) — rejected: registration is process-local by design in
a self-contained composable application; cross-process discovery is the service mesh's
concern and stays opt-in. (c) *Exporting the full live registry for byte comparison* —
rejected in favor of a fixed fixture set: engines legitimately differ in framework
built-ins (no Spring in Rust, no Kafka mesh), so whole-registry comparison would pin
incidental surface, not contract.

**Consequences.** New ports implement the carrier idiomatically, then pass the three
golden-vector suites (`registration-vectors/core.json`, `plugin.json`, `feature.json`)
before their declaration surface is considered done; every capability field a port cannot
honor is documented as N/A where developers would meet it, never silently dropped. The
engines accept a small ongoing cost: vector files are maintained verbatim in every
repository, and semantic changes to registration must update the contract page, the
vectors, and all engines in lock-step — which is precisely the point.

---

## ADR-0008 — Synchronous AI-companion endpoint: in-band command outcome + live tee {#adr-0008}
**Status:** Accepted · **Date:** 2026-07-18T18:13:53.000Z · **Serves:** vision-mercury-composable
<!-- id: adr-0008 | status: accepted -->

**Abstract.** Add an **additive** synchronous companion endpoint —
`POST /api/companion/{session-id}/sync` — that returns the command's **outcome in-band** as a
structured envelope `{ ok, command, output, error, result }`, alongside the existing fire-and-forget
`POST /api/companion/{session-id}` (which returns only `{status:"accepted"}` and streams the real
outcome to the WebSocket console). The synchronous handler also **tees** each output line to the
session's WebSocket `.out`, so a human at the Playground — and, via the command service's existing
subscriber fan-out, any `session subscribe`d session — sees the same output live. The existing
endpoint and the human console are unchanged. A **reference implementation is proven in the Rust
port** (`acn-ericlaw/mercury`); this ADR proposes adopting it in the Java engine.

**Rationale.** The current companion surface is a **write-only command bus**: `PostCompanionCommand`
dispatches the command fire-and-forget and returns an acknowledgement; the actual result — success
text *and errors* — reaches only the WebSocket console. An **AI agent** driving the endpoint over HTTP
is therefore blind to what happened: to learn the effect it must poll `GET /api/graph/session/{id}`
(shape) and `GET /api/inspect/{id}/{key}` (state), and a *rejected* command leaves the model unchanged
with no error at all — so polling cannot even distinguish "no-op" from "rejected". This was not
hypothetical: in an AI-companion validation exercise a capable agent posted an invalid `graph.math`
node, received HTTP 200, and never saw the engine's `node … does not have if:, then: or else:` →
*graph traversal aborted*; it only inferred failure from empty inspect state. A true AI companion needs
**synchronous, self-describing feedback** — send a command, get back what happened — so it can
**self-correct autonomously** instead of relying on a human to relay the console. The tee makes the
same endpoint a **real-time human+AI collaboration** surface: an architect and an AI draft a graph on
one live session while a product owner (subscribed) watches; work suspends/resumes across sprints via
`export`/`import`. **Mechanism (proven in Rust, mirrorable in Java):** the synchronous handler
dispatches the command to the command service via **request/response RPC** (Java `po.request` /
`AsyncInbox`) with a **private capture route** (`platform.register`) supplied as the command's `out`;
it drains the captured lines, classifies `ok`/`error`, folds a `run`/`inspect` result into `result`,
and returns the envelope — while fire-and-forget forwarding each line to the session's real `.out` for
the live view. It reuses existing primitives; the `say()`-based command functions are untouched.
**Alternatives.** An **MCP tool server** (typed tools) was considered and deferred — heavier, and it
forks the shared human/AI text surface into an AI-only one; the in-band envelope captures most of the
value while keeping one surface. Having the command handler **return its transcript** directly (no
capture route) is cleaner but threads an output sink through every command function; deferred.
**Consequences.** Additive and backward-compatible (the fire-and-forget route and the console stream
are unchanged). The **envelope shape is a cross-vendor contract** — the Rust and Java ports should
agree on it; open points: whether a large `run` `output.body` is inlined or spilled to
`GET /api/inspect` (mirror the existing large-payload rule), and whether `inspect` results fold the
same way. Refines the companion surface introduced with the MiniGraph Playground; bounded by ADR-0001
(decoupled functions — the endpoint is just another route) and ADR-0003 (Map-or-PoJo over
EventEnvelope — the envelope is a Map). **Reference implementation** (Rust port,
`acn-ericlaw/mercury`): the endpoint (`post.companion.command.sync`, dev-gated), an integration test
(`companion_sync_returns_outcome_in_band`), a design note (`docs/design/ai-companion-sync.md`), and a
**live multi-party demo** in which a fresh AI companion built + ran a decision graph autonomously via
`/sync` — self-correcting from in-band errors (including a retired-skill dead end) while an architect
and a subscribed product owner watched in real time. **Now implemented in this Java engine**:
`PostCompanionCommandSync` (route `post.companion.command.sync`, dev-gated) with `CompanionSyncTest`,
mirroring the Rust design — a private per-call capture route (`registerPrivate`) supplied as the
command's `out`, RPC to the singleton command handler, a FIFO sentinel to mark the buffer drained, and
a best-effort tee to the session's WebSocket `.out`. **End-of-transmission refinement (both ports):**
the sentinel is correct only for *synchronous* commands, which emit all output before the handler
replies. A traversal (`run`) is *asynchronous* — the handler launches the traveler and replies
immediately, then the traveler streams its output afterwards — so a post-reply sentinel races (and
usually beats) that tail and truncates the capture. A traversal is therefore drained on the traveler's
**terminal line** (`Graph traversal completed in N ms` | `Graph traversal aborted`), which is always
emitted last. To make that signal reliable, **every `run` now ends with one terminal line**: the
early-failure paths (no instance yet, missing root/end node) emit their reason *then* the canonical
`Graph traversal aborted`, so a companion mistake such as `run` before `instantiate` returns promptly
(`ok:false`) instead of waiting out the timeout. The bounded wait is only a safety net; correctness
comes from the signal. This keeps the REST contract byte-identical across the Rust and Java engines —
the companion surface is language-neutral.

---

## ADR-0007 — Event Script configuration is preferred over code for orchestration {#adr-0007}
**Status:** Accepted · **Date:** 2026-06-27T15:45:00.000Z · **Serves:** vision-mercury-composable
<!-- id: adr-0007 | status: accepted | formalizes: event-script-over-code -->

**Abstract.** When a step is **orchestration** — sequencing functions, branching on a condition,
handling a failure, or moving data between steps — express it as **Event Script YAML** (tasks,
`execution` types, input/output data mapping, exception handler), not as imperative code inside a
function. Code is reserved for the **unit of work** itself (the function body; ADR-0005). The
sync-over-async refactoring that produced this ADR converted an imperative facade — a
`PostOffice.send` publish buried in a function, with a hand-written `try/catch` mapping the failure to
an HTTP status — into a declarative flow: `prepare → simple.kafka.notification → await`, where a publish
failure is routed by the engine to the flow's exception handler (fail-fast → HTTP status) and a
`decision` task expresses the drop branch. The boundary holds in the other direction too: a genuinely
in-function concern — here, the synchronous **blocking rendezvous** that must bracket the publish —
stays in code. Not all code becomes YAML.

**Rationale.** Two properties make configuration the better home for orchestration. **(1) It
communicates intent.** The flow file is a single, legible statement of the event flow — a reviewer sees
the `begin → publish → await` sequence, the topic names (`text(topic-1)` / `text(topic-2)`), the
fail-fast path, and the no-reply branch without reading Java; the imperative version hid the topic and
the publish inside `PostOffice` calls and buried the control flow in `try/catch`. **(2) It manages
dependencies.** Event Script declares both control-flow dependencies (task order, decision branches,
exception routing) and data-flow dependencies (field-level mapping through `model`), and the engine
enforces them — so functions stay fully decoupled (ADR-0001), never importing one another, with the
only wiring in the flow. Reusable building blocks are composed **by reference, not duplicated in code**:
the one `simple.kafka.notification` function publishes the request in one flow and the reply in another.
Cross-cutting behavior (failure handling, status policy, `ttl` timeouts, trace propagation) becomes an
engine concern expressed in config rather than repeated boilerplate, and orchestration changes (add a
step, change a topic, re-route a branch) are reviewable config edits that need no recompile — the source
of the "roughly half the code" claim in ADR-0001. The accepted consequences are the cost of the
abstraction, not reasons to avoid it: the unit of work stays in code (a flat task chain cannot express a
blocking await that must wrap a publish — forcing it into config is contortion), and declarative routing
has its own vocabulary to learn — the `decision` type selects a `next` entry by value (`true` = `1` =
first entry, `false` = `2` = second; an integer is 1-based and enables a multi-way `switch`), and `byte[]`
payloads ride through `model` via the `*` whole-body passthrough (ADR-0003). This decision refines
ADR-0001 (orchestration as Event Script) and is bounded by ADR-0005 (one atom, four roles).

---

## ADR-0006 — Cloud-native by default; service mesh for sync-over-async and service discovery only {#adr-0006}
**Status:** Accepted · **Date:** 2026-06-23T18:30:00.000Z · **Serves:** vision-mercury-composable
<!-- id: adr-0006 | status: accepted | formalizes: kafka-mesh-opt-in -->

**Abstract.** The Kafka service mesh (`cloud.connector=kafka` + presence-monitor) is an **opt-in
capability** that solves two specific problems: (1) synchronous request-response between different
application instances over Kafka, and (2) service discovery between running pods. Applications that do
not need either capability must be designed **cloud-native** — each instance self-contained, stateless,
and horizontally scaled without cross-instance coupling. Enabling `cloud.connector=kafka` is a
deliberate architectural choice, not a default or a convenience. `cloud.connector=none` is the
framework default.

**Rationale.** Superimposing synchronous request-response over Kafka (an inherently asynchronous
transport) is technically feasible — the same pattern appears in IBM MQ, Redis pub/sub for RPC, and
other enterprise messaging systems — but it is architecturally expensive. Cross-instance synchronous
RPC creates latency dependencies between otherwise independent scaling units: if one pod is slow, every
caller waiting on it is slow; errors propagate across instance boundaries; horizontal scaling no longer
provides isolation between workloads. Overuse of this pattern degrades a cloud application into a
**distributed monolith** — all the operational complexity of a distributed system combined with the
tight coupling of a monolith. Cloud-native design avoids these risks: inbound load is distributed at
the infrastructure layer (load balancer / Kubernetes ingress), and each instance handles its share
independently. The service mesh should be adopted only when one of its two genuine use cases applies:
(a) cross-application synchronous RPC that cannot be decoupled further, or (b) distributed resilience
patterns that require peer awareness (leader selection, failover, pod-aware broadcast). Consequence:
documentation, tooling, and AI agent guides must treat the service mesh as an advanced, opt-in topic —
not the standard deployment model — to avoid steering users toward the distributed monolith
anti-pattern.

---

## ADR-0005 — One atom, four roles
**Status:** Accepted · **Date:** 2026-06-22T22:47:23.000Z · **Serves:** vision-mercury-composable
<!-- id: adr-0005 | status: accepted | formalizes: docs-content-canon -->

**Abstract.** The sole building block of an application is the **route-addressed
function** — a plain Java class annotated `@PreLoad` implementing `LambdaFunction` or
`TypedLambdaFunction`, with Map/PoJo I/O, private by default. There is no second
primitive; the same unit is **named by how it is wired**:

- **function** — the atom itself (registered in the `Platform` registry by route name);
- **service** — a function mapped straight to HTTP via `service:` in `rest.yaml` (a narrow
  REST role, distinct from `flow:`; see `RoutingEntry.java` `SERVICE = "service"`);
- **task** — a step in an Event Script flow carrying an `execution` type, one of
  `CompileFlows.EXECUTION_TYPES` (`decision, response, end, sequential, parallel,
  pipeline, fork, sink`);
- **skill** — a function attached to an Active Knowledge Graph node via that node's
  `skill:` property (`GraphLambdaFunction.java` `SKILL = "skill"`).

**Rationale.** One primitive means one mental model and one programming model regardless
of which paradigm layer you are working in — learning to write a function transfers to
every role, and a function can be promoted from a flow task to a graph skill without being
rewritten. The alternative — distinct primitives per layer (an HTTP-handler type, a
flow-step type, a graph-node type) — would fragment the model and break the decoupling
guarantee that the whole framework rests on (see ADR-0001). Consequence: the role-names
are kept precise in all documentation — "function" is the general atom, "service" is the
narrow REST role and is **not** a synonym for it, and a task is a role of the atom, never a
separate kind of thing.

---

## ADR-0004 — Three-paradigm-layer architecture
**Status:** Accepted · **Date:** 2026-06-22T22:47:23.000Z · **Serves:** vision-mercury-composable
<!-- id: adr-0004 | status: accepted | formalizes: docs-content-canon -->

**Abstract.** The framework is organized as **three ascending paradigm layers**, each
building on the one below:

1. **Event-driven foundation** — Platform Core: decoupled functions over the in-memory
   event bus (ADR-0001, ADR-0002).
2. **Composable orchestration** — Event Script: a YAML DSL choreographing those functions
   into transactions.
3. **Semantic — Active Knowledge Graph** — MiniGraph: graph models that *execute* behavior
   through skills embedded on nodes.

These conceptual layers are **distinct from the runtime request pipeline** — whose *stages*
run outside in: user / calling application → protocol boundary (REST automation for HTTP, a Kafka
listener, or another protocol) → flow adapter → Event Manager / flow engine → in-memory event bus
→ composable functions. (For each protocol there is a corresponding flow adapter; for HTTP, REST
automation is the boundary that invokes the built-in HTTP flow adapter.) The word "layers" is
reserved for the three paradigms; the request flow is a *pipeline* with *stages*, never a layering.

**Rationale.** A single coherent ascent gives users both a mental model and an on-ramp:
begin event-driven, compose with Event Script, model semantically with the Active
Knowledge Graph (the user-facing surface per the Vision). Naming is locked to remove a
recurring source of confusion: *Active Knowledge Graph* is the model, *Knowledge Graph as
Application* the tagline, *MiniGraph* the engine, *semantic* an adjective only. The origin
is told as part of the foundation — Scala/Akka actor model → Eclipse Vert.x event bus →
Java 21 virtual threads. Human–AI collaboration is a **cross-cutting capability** across
all three layers (agent-ready DSL specs + a companion endpoint), **not** a fourth layer.
This entry supersedes the earlier framing that described the runtime as five separate
layers, which conflated the conceptual layering with the request pipeline.

---

## ADR-0003 — Function I/O contract: Map-or-PoJo over an immutable EventEnvelope
**Status:** Accepted · **Date:** 2026-06-22T22:47:23.000Z · **Serves:** vision-mercury-composable
<!-- id: adr-0003 | status: accepted | formalizes: typed-io-map-or-pojo -->

**Abstract.** A `TypedLambdaFunction<I, O>`'s normal input and output type is a **Map or a PoJo**.
**Key-by-key data mapping** in Event Script (Layer 2) and the Knowledge Graph (Layer 3) maps fields
individually, so a List cannot serve as the mapping contract there — use a Map or a single PoJo.
However, the **`*` whole-body passthrough** (`model.list -> *`) is a special escape from key-by-key
mapping: it passes the entire state-machine value as the event body, bypassing field-level mapping.
Combined with `inputPojoClass` on `@PreLoad`, this enables a `List<PoJo>` at the function boundary
within an Event Script flow. Layer 1 (Platform Core) uses the same `inputPojoClass` mechanism to
ingest an incoming JSON-*list* payload directly from an external source (see Consequences). Functions
exchange the immutable `EventEnvelope` message container: headers are `Map<String,String>`, the body
is MsgPack-serialized on the wire, and PoJo↔Map conversion uses a customized Gson.

**Rationale.** Constraining key-by-key I/O to Map-or-PoJo keeps Event Script data mapping clean and
readable and avoids serialization edge cases. A PoJo enforces an interface contract; a Map gives
flexible structure — together they cover the spectrum without admitting ambiguous generic collections.
The `*` passthrough is the intentional escape hatch for List payloads (tested in the
event-script-engine suite). The accepted consequences are the serialization gotchas that follow from
the wire format: MsgPack downcasts a small `Long` to `Integer` on the wire (pin the type with a PoJo
when it matters); the customized Gson treats integers in a Map as `Long` (use `util.str2int` /
`util.str2long` for safe conversion); Map keys must be strings (non-string keys are auto-converted).
The `List<PoJo>` path (via `*` passthrough or Layer-1 external ingestion): declare
`inputPojoClass = X.class` on `@PreLoad` — the serializer deserializes the list of maps into the
typed list. (Outgoing list payloads need no special handling: Event Script's `AsyncHttpClient` and
the Knowledge Graph's API-fetcher skill do their own data mapping.) Functions may still return
`Mono<T>` / `Flux<T>` for reactive pipelines.

---

## ADR-0002 — Virtual-thread event engine: sequential RPC at reactive performance
**Status:** Accepted · **Date:** 2026-06-22T22:47:23.000Z · **Serves:** vision-mercury-composable
<!-- id: adr-0002 | status: accepted | formalizes: virtual-threads-rpc -->

**Abstract.** Functions execute on **Java 21 virtual threads** over an **Eclipse Vert.x**
in-memory event bus. A PostOffice RPC call (`po.request(...)`) appears synchronous and
sequential to the caller, while behind the curtain the virtual thread is *suspended* and
the carrier kernel thread is released — so blocking-style sequential code performs on par
with reactive code.

**Rationale.** The lineage is deliberate: Scala/Akka **actor model** (Mercury v1) →
Eclipse Vert.x event bus (v2) → a fully non-blocking engine with low-level execution
control (v3) → Java 21 virtual-thread integration (v3.1+). The goal is to keep the
**clarity of sequential code** — the code reads as the intent of the application, easy to
read and maintain — without paying the throughput cost of blocking a kernel thread per
in-flight request. Alternatives considered: a pure reactive API (`Mono`/`Flux`
everywhere), which is harder to read and maintain; and classic thread-per-request, which
caps concurrency. Consequences: synchronous PostOffice RPC ≈ reactive performance;
`Mono`/`Flux` remain available for genuinely reactive pipelines; the framework requires
Java 21.

---

## ADR-0001 — Decoupled functions wired by route names; orchestration as Event Script
**Status:** Accepted · **Date:** 2026-06-22T22:47:23.000Z · **Serves:** vision-mercury-composable
<!-- id: adr-0001 | status: accepted | formalizes: functions-decoupled-routes -->

**Abstract.** All application logic is packaged as **self-contained functions** —
`@PreLoad`-annotated classes implementing `LambdaFunction` or `TypedLambdaFunction`,
registered in the `Platform` registry and addressed **exclusively by a route-name
string**. Functions hold no direct reference to one another; they communicate only by
exchanging immutable `EventEnvelope` messages over the event bus. **Orchestration** — the
sequencing of functions into a transaction — is declared in **YAML Event Script**, not
written in code; the only link between a flow and a function is the route-name string.

**Rationale.** Full decoupling is the foundation the entire framework rests on: functions
can be developed, tested, deployed, relocated across a service mesh, and recomposed into
new flows without recompiling or knowing about each other. Moving orchestration out of
code and into configuration makes the sequencing reviewable and changeable on its own, and
roughly halves application code. The alternatives — direct method
calls or dependency-injection wiring between components, and imperative orchestration code
— were rejected because they reintroduce compile-time coupling and bury the transaction
flow in control logic. The accepted consequence is that the route-name string is the whole
contract between a flow and a function, so route-naming discipline matters and is enforced
by convention. This decision is elaborated by ADR-0005 (the one function atom plays four
wiring roles) and realized on the runtime of ADR-0002.
