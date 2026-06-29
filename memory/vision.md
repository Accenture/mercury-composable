# Vision — mercury-composable

> Confirmed by maintainer (Eric Law) on 2026-06-20. North star for the VBDI loop;
> tier `core`, re-confirmed on the invariant-verification cadence. The Blueprint
> (gap from Current State to here) lives as `(blueprint)` Open Threads in continuity.md.
>
> <!-- id: vision-mercury-composable | created: 2026-06-20 | last_used: 2026-06-24 | uses: 18 | tier: core -->

## Elevator statement

Mercury Composable lets organizations build and evolve backend applications as **Active
Knowledge Graphs** — business intent, enterprise knowledge, and system behavior captured in
one executable model and continuously refined through human–AI collaboration — so changing
what a system *does* means refining the model, certifying it with product owners, and
deploying the updated model to the live environment, rather than rewriting and redeploying code.

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
  Realized today: the `graph.executor` engine, 7 built-in skills (`graph.math`,
  `graph.data.mapper`, `graph.js`, `graph.api.fetcher`, `graph.extension`, `graph.island`,
  `graph.join`), REST execution at `/api/graph/{graph-id}`, a React/Vite Playground UI, and a
  WebSocket session model.
- **Collaboration — AI companion (early).** A dev-only `POST /api/companion/{id}` endpoint lets
  an external agent (incl. a Claude session) drive Playground commands into a live session. No
  LLM backend is integrated in-repo yet; today the "AI" is an external session following a
  documented prompt.

**Type:** Multi-module Java 21 framework / SDK (Maven reactor, `com.accenture.mercury` v4.5.0).

## What it should become  *(TARGET)*

**AI-assisted Semantic Application Development** — a software-engineering paradigm where
business intent, enterprise knowledge, and system behavior are captured as an Active Knowledge
Graph and continuously refined through collaboration between business users, architects,
developers, and AI companions.

*Scope: the **whole framework** serves this paradigm; the **Active Knowledge Graph is the
user-facing surface** — what business users and architects work in — with Event Script and
Platform Core as the foundation beneath it.* Concretely:

- The **Active Knowledge Graph is the application** — the zero-code default for common backend
  services, APIs, and decision logic; Event Script and (rarely) custom skills handle the most
  demanding cases without breaking decoupling.
- **Human–AI and human–human collaboration is first-class** — the companion endpoint matures
  from a dev-only command pipe into a governed collaboration layer with an integrated,
  pluggable AI companion that co-authors graphs.
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
- **AI companions** — collaborate as drafting / refinement partners across all three roles.
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

## Non-goals  *(what it must never become)*

- **Not** a general-purpose graph database / OLAP analytics engine — the graph drives
  *execution and decisioning*, not storage-scale querying.
- **Never** couples functions directly — coupling stays route-name + `EventEnvelope` only
  (architectural invariant).
- **Not** locked to one LLM vendor — the AI companion is pluggable.
- **Not** a "no code ever" dogma — zero-code is the default, not a hard limit; Event Script +
  custom skills remain the escape hatch.
- **Not** a heavyweight runtime — stays lightweight; Spring is optional, never required by core.

## Mental model

> The Active Knowledge Graph *is* the application: humans and AI co-author the model, the
> event-driven runtime executes it, and changing behavior means editing knowledge — not
> shipping code.
