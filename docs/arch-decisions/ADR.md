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
the source of truth in case of ambiguity. Narrative design rationale that is *not* a
governance decision lives separately in [Design Notes](../notes/design-notes.md).

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

**Abstract.** A `TypedLambdaFunction<I, O>`'s input and output type must be a **Map or a
PoJo** — a List of PoJo is **not** supported. Functions exchange the immutable
`EventEnvelope` message container: headers are `Map<String,String>`, the body is
MsgPack-serialized on the wire, and PoJo↔Map conversion uses a customized Gson.

**Rationale.** Constraining I/O to Map-or-PoJo keeps the *input data mapping* in Event
Script clean and readable and avoids serialization edge cases. A PoJo enforces an
interface contract; a Map gives flexible structure — together they cover the spectrum
without admitting ambiguous generic collections. The accepted consequences are the
serialization gotchas that follow from the wire format: MsgPack downcasts a small `Long`
to `Integer` on the wire (pin the type with a PoJo when it matters); the customized Gson
treats integers in a Map as `Long` (use `util.str2int` / `util.str2long` for safe
conversion); Map keys must be strings (non-string keys are auto-converted). The trade-off:
a caller with a List of PoJo must wrap it in a holder PoJo or a Map — a deliberate price
for predictable serialization. Functions may still return `Mono<T>` / `Flux<T>` for
reactive pipelines.

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
(per the design notes) roughly halves application code. The alternatives — direct method
calls or dependency-injection wiring between components, and imperative orchestration code
— were rejected because they reintroduce compile-time coupling and bury the transaction
flow in control logic. The accepted consequence is that the route-name string is the whole
contract between a flow and a function, so route-naming discipline matters and is enforced
by convention. This decision is elaborated by ADR-0005 (the one function atom plays four
wiring roles) and realized on the runtime of ADR-0002.
