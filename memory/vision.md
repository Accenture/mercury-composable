# Vision — mercury-composable

> Confirmed by maintainer (Eric Law) on 2026-06-20; re-confirmed 2026-07-27 (invariant
> re-verification walkthrough). **EVOLVED 2026-09-17 (Eric): the two tracks made explicit** — the
> graph as application (deterministic) and the graph as AI SDLC (governed AI processing for
> ambiguity). North star for the VBDI loop;
> tier `core`, re-confirmed on the invariant-verification cadence. The Blueprint
> (gap from Current State to here) lives as `(blueprint)` Open Threads in continuity.md.
>
> <!-- id: vision-mercury-composable | created: 2026-06-20 | last_used: 2026-06-24 | uses: 18 | tier: core -->

## Elevator statement

Mercury Composable lets people and AI **co-design an Active Knowledge Graph** — business intent,
enterprise knowledge, and system behavior captured in one executable model — and then run it. The
graph serves two tracks from one foundation: it **is the application** for backend services, and it
**is the SDLC** for autonomous and human-in-the-loop AI processing. Changing what a system *does*
means refining the model, certifying it with product owners, and deploying the updated model to the
live environment, rather than rewriting and redeploying code.

## The two tracks

One foundation, two ends. **The tracks echo each other by design** — same co-design practice, same
certification gate, same promotion lifecycle. What separates them is the nature of the problem:
**Track 1 is deterministic** — rules, business logic, outcome. **Track 2 is governed AI processing
that tackles ambiguity a deterministic program cannot handle.** Both are co-designed by AI with
human inputs; that collaboration is the shared foundation, and it is delivered.

| | **Track 1 — knowledge graph as application** | **Track 2 — knowledge graph as AI SDLC** |
|---|---|---|
| The graph **is** | the application | the SDLC |
| Nature of the work | deterministic — rules, business logic, outcome | ambiguous — judgment a deterministic program cannot express |
| Authoring | AI co-designs the graph with human inputs | AI co-designs the graph with human inputs |
| Execution | Layer 3 **leverages** Event Script (layer 2) and Platform Core (layer 1) | Layer 3 **is the foundation**; **AI is also the runtime for certain nodes** |
| AI at runtime | no — a design-time collaborator only | **yes** — AI-bearing nodes reach an agent through gateways, MCP servers or other tools |
| After promotion | authoring UI disabled; the graph runs on the Mercury stack | authoring UI disabled; the graph runs, and its AI nodes call out |
| State | **delivered** — production quality, measured in field installations | **design phase** — prerequisites proven (progressive rendering of token batches) |

The asymmetry is the whole of it: Track 1 delegates execution *down* the three layers; Track 2
stands on layer 3 and delegates *out* to an agent for the nodes that carry AI skills. Everything
else is shared, which is why Track 2 inherits a solved problem — authoring — and is left with a
governance one.

Today each AI use case (application code conversion, bug fixing, operations management) is
custom-built as a bespoke agent loop. Track 2 is how those become one governed, reusable practice
instead of many one-offs.

## Current-state context  *(inferred from memory + code — confirm or replace)*

A mature, multi-module Java framework realizing a three-layer ascent, each layer building on
the one below:

- **Event-driven — Platform Core.** Fully decoupled functions communicating only by
  route-name + `EventEnvelope` over an Eclipse Vert.x event bus; Java 21 virtual threads make
  synchronous RPC perform like reactive. Lineage: Scala / Akka actor model. (no Spring in core)
- **Composable — Event Script.** A YAML DSL (introduced in v4) choreographing immutable
  functions; orchestration is ~50% config / 50% code.
- **Semantic — Active Knowledge Graph (MiniGraph).** Graph models that *execute* behavior via
  skills embedded on nodes during traversal — zero imperative code for the common case.
  Realized today: the `graph.executor` engine, 10 built-in skills (`graph.math`,
  `graph.data.mapper`, `graph.js`, `graph.api.fetcher`, `graph.task`, `graph.extension`,
  `graph.suspend`, `graph.resume`, `graph.island`, `graph.join` — incl. workflow suspension
  with pluggable external state stores), gated REST execution at `/api/graph/{graph-id}`
  (CompileGraph manifest = the deployment quality gate), a React/Vite Playground UI, and a
  WebSocket session model.
- **Collaboration — AI companion (MATURE, 2026-09).** Human–AI collaboration for graph
  prototyping and productization is production quality, with measured success in field
  installations. It runs in **dev mode** by design: the Playground is where a human and an AI
  co-design a graph, and after promotion the authoring UI is disabled — what promotes is the
  *graph*, not the authoring surface. The live-Gemini progressive-rendering drive settled the
  pluggable-backend question. This is the foundation **both** tracks stand on.

**Type:** Multi-module Java 21 framework / SDK (Maven reactor, `com.accenture.mercury`,
version per `continuity.md` → `latest_release`; official Rust port and python/node.js language packs in
lock-step at the same version).

## What it should become  *(TARGET)*

**AI-assisted Semantic Application Development** — a software-engineering paradigm where
business intent, enterprise knowledge, and system behavior are captured as an Active Knowledge
Graph and continuously refined through collaboration between business users, architects,
developers, and AI companions.

*Scope: the **whole framework** serves this paradigm; the **Active Knowledge Graph is the
user-facing surface** — what business users and architects work in — with Event Script and
Platform Core as the foundation beneath it.* Concretely:

- **Track 1 — the Active Knowledge Graph is the application.** The zero-code default for common
  backend services, APIs and decision logic, where the work is deterministic; Event Script and
  (rarely) custom skills handle the demanding cases without breaking decoupling.
- **Track 2 — the Active Knowledge Graph is the SDLC.** Each AI use case is expressed as a graph
  co-designed by human and AI, rather than custom-built as a bespoke agent loop. Some nodes carry
  AI skills, so in production those nodes reach an AI agent through gateways, MCP servers or other
  tools — governed processing for the ambiguity a deterministic program cannot handle. Authoring is
  inherited from Track 1; **governance of AI-bearing execution is the open problem.**
- **Human–AI collaboration is first-class — and is the foundation both tracks stand on.**
  Delivered for graph prototyping and productization; the open work is extending the same
  co-design practice to AI-SDLC graphs.
- **One model unifies intent + knowledge + execution** — business rules, data contracts, and
  behavior live together, inspectable and explainable, not scattered across code, config, and
  tribal knowledge.
- **Enterprise lifecycle & governance** — graph models move through dry-run → certify → stage →
  approve → production and deploy as standard API endpoints / event listeners.

## For whom

- **Business users** — express intent and validate behavior without writing code.
- **Architects** — model knowledge, data contracts, and decision flows; certify and govern.
- **Developers** — build skills, Event Script flows, and platform-core functions for the
  demanding edges; own extensibility.
- **AI companions** — two roles, one per track. At **design time** (both tracks) they collaborate
  as drafting / refinement partners across all three human roles. At **run time** (Track 2 only)
  an AI is a **participant in execution**: the agent behind an AI-bearing node, reached through a
  gateway or MCP server.
- *(plus the existing audience: Java teams building decoupled, event-driven backends.)*

## Success criteria  *(how we'd know it's realized)*

- A common backend service (fetch → decide → transform → respond) is built and run
  **end-to-end with zero imperative code** — graph + config only — and dry-run-validated.
- A **non-developer**, working with an AI companion, can turn stated intent into a running,
  governed graph model.
- Execution is **inspectable and explainable** — any run's traversal path and intermediate
  state can be examined.
- The three layers **compose cleanly**: graph → Event Script → platform-core function, with no
  direct coupling.
- Graph models **promote through the governance lifecycle** to production as standard endpoints.
- **(Track 2)** An AI use case that would today be a bespoke agent loop — a code conversion, a
  bug-fix triage, an operational runbook — is expressed as a graph, co-designed with an AI,
  certified by a product owner, and promoted; its AI-bearing nodes reach their agent through a
  gateway or MCP server rather than through code written for that one use case.

## Non-goals  *(what it must never become)*

- **Not** a general-purpose graph database / OLAP analytics engine — the graph drives
  *execution and decisioning*, not storage-scale querying.
- **Never** couples functions directly — coupling stays route-name + `EventEnvelope` only
  (architectural invariant).
- **Not** locked to one LLM vendor — the AI companion is pluggable.
- **Not** a "no code ever" dogma — zero-code is the default, not a hard limit; Event Script +
  custom skills remain the escape hatch.
- **Not** a heavyweight runtime — stays lightweight; Spring is optional, never required by core.
- **Not** a claim of determinism for AI-bearing graphs (Track 2) — an AI node's output is not
  static. The graph governs *where* that variation is allowed, bounds it, and keeps the traversal
  inspectable. "Governed nondeterminism", never "deterministic AI".

## Mental model

> Humans and AI co-design one executable model, and what that model *is* depends on which track it
> serves: for deterministic work the **graph is the application**, executing down through Event
> Script and Platform Core; for ambiguous work the **graph is the SDLC**, with AI itself the runtime
> for the nodes that carry AI skills. Either way the runtime executes knowledge, and changing
> behavior means refining the model — not shipping code.
