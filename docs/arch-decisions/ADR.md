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
