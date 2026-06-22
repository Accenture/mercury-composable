---
title: Knowledge Graph as Application
summary: Model a backend service as an Active Knowledge Graph — a property graph whose
  nodes carry executable skills that run during traversal, so behavior lives in the model
  rather than in imperative code.
layer: knowledge-graph
audience: [architect, developer]
keywords: [active knowledge graph, minigraph, skill, graph execution, semantic application, zero-code]
related:
  - guides/knowledge-graph/property-graph.md
  - guides/event-script/syntax.md
  - index.md
---

# Knowledge Graph as Application

> **At a glance**
>
> - **What it is** — An *Active Knowledge Graph* is a property graph whose nodes can carry
>   executable **skills**. As the engine traverses the graph, those skills run — so the graph
>   *is* the application. Knowledge, data contracts, and behavior live in one model.
> - **Who it's for** — Architects and developers building decision-centric, data-driven backend
>   services and APIs, often with little or no imperative code.
> - **Where it sits** — The **semantic** layer, the top of Mercury's ascent:
>   Platform Core (event-driven) → Event Script (composable) → **Active Knowledge Graph**.
> - **How you run it** — Execute a deployed graph via `POST /api/graph/{graph-id}`; design and
>   dry-run graphs interactively in the **MiniGraph Playground**.
> - **Before you start** — Skim [Minimalist Property Graph](property-graph.md) for the underlying
>   graph data model; [Event Script](../event-script/syntax.md) helps for the integration section below.

## From code, to configuration, to knowledge

Mercury Composable has always pushed *coupling* out of code and into the model. Functions are
[fully decoupled](../../index.md) — addressed only by route name, wired by events. **Event Script**
then moved orchestration out of code into YAML flows: roughly half config, half code.

An Active Knowledge Graph is the next step on that ascent. Instead of writing a flow that *calls*
functions, you describe what the system *knows* — entities, relationships, data sources, decisions —
as a graph, and attach a **skill** to the nodes that need to *do* something. Running the application
means **traversing the graph**. Changing behavior means **editing the model**, certifying it, and
deploying the updated model — not rewriting and redeploying code.

This is what "Knowledge Graph as application" means: a single, inspectable model that unifies
*what the system knows* with *what it does*.

## What an Active Knowledge Graph is {#what-it-is}

A traditional property graph models **nodes** (entities), **connections** (relationships), and
**properties** (key–values on either). Mercury's [MiniGraph](property-graph.md) adds one thing: a
node may carry a `skill` property naming a composable function. That node becomes an **active node**.

- Nodes without a skill are **passive** — they hold data and are traversed but not executed.
- Nodes with a skill are **active** — when traversal reaches them, the engine invokes the skill.

Because a skill is just a composable function addressed by route name, the same decoupling and
event-driven execution that powers the rest of Mercury powers the graph.

## How execution works {#execution-model}

Execution is event-driven and asynchronous — not a blocking tree-walk. The engine
(`graph.executor`) drives a single graph instance through these steps:

1. **Begin at the root.** The engine seeds a per-instance **state machine** (a hierarchical
   `MultiLevelMap`) with the request, then walks from the `Root` node toward the `End` node.
2. **Reach an active node → invoke its skill.** The engine sends an event to the skill's route,
   correlated as `{flowInstanceId}@{nodeName}`. The skill reads its inputs from the node's
   properties and from the state machine, does its work, and returns a **result** plus a
   **decision**.
3. **Record and decide the next step.** The engine stores the outcome at `{nodeName}.result`,
   `{nodeName}.status`, and (on failure) `{nodeName}.error`, then routes to the next node based on
   the skill's decision value:
     - `next` — follow the graph's outgoing connection;
     - a **node name** — jump to that node (this is how branching/decisions work);
     - `.sink` — pause this path (used by joins and isolated sub-graphs).
4. **Guard against runaway traversal.** Built-in **loop detection** (tunable via
   `graph.max.loop.interval` and `graph.node.high.frequency`) prevents unintended infinite loops.
5. **Finish at the end.** Reaching the `End` node completes execution and returns the response from
   the `output` namespace.

The **state machine** is the shared workspace across stateless skills. Its namespaces:

| Namespace | Holds |
|---|---|
| `input.body` / `input.header` | the incoming request |
| `model.*` | intermediate working state |
| `{nodeName}.result` / `.status` / `.error` | each skill's output and execution status |
| `output.body` / `output.header` | the final response |

## The node model {#node-model}

| Node type | Role | Carries a skill? |
|---|---|---|
| **Root** | Entry point — traversal starts here | no |
| **End** | Terminal — completes execution, returns `output` | no |
| **Data entity** | Passive business entity (e.g. Person, Order) | no |
| **Data dictionary** + **Provider** | Define an external data source: attributes, endpoint, request/response mapping | no |
| **Active (skill) node** | Runs a skill during traversal (compute, decide, fetch, branch, sync) | **yes** (`skill=...`) |

A node has **at most one** skill. Inputs come from the node's own properties plus values pulled from
the state machine via data-mapping (`{source} -> {target}`); outputs flow back into the state machine
for downstream nodes.

## Built-in skills {#built-in-skills}

Seven skills ship with the engine. Each is a composable function on a `graph.*` route:

| Skill (route) | What it does |
|---|---|
| `graph.data.mapper` | Copy/transform data between state-machine namespaces (`mapping[]`) |
| `graph.math` | Fast inline math & boolean evaluation, `IF/THEN/ELSE` branching, loops |
| `graph.js` | Full JavaScript evaluation via GraalVM — more flexible, slower than `graph.math` |
| `graph.api.fetcher` | Call external HTTP APIs using data-dictionary/provider nodes, with caching and fork-join concurrency |
| `graph.extension` | Run another graph model **or** an Event Script flow as a sub-routine |
| `graph.join` | Synchronization barrier — proceeds only once all upstream paths complete |
| `graph.island` | Mark an isolated sub-graph (pauses traversal); useful while building |

> Per-skill syntax, parameters, and worked examples are in the
> [Built-in skills reference](skills-reference.md); in the Playground, `describe skill {name}`
> prints the same content.

## Composing with the lower layers {#layer-integration}

An Active Knowledge Graph is not an island — it rides on the layers beneath it, without coupling:

- **Down to Event Script & functions** — `graph.extension` runs a **sub-graph** (`extension=<graph-id>`)
  or an **Event Script flow** (`extension=flow://<flow-id>`), so demanding logic can be delegated to
  composable modules and back.
- **Out to external systems** — `graph.api.fetcher` drives HTTP calls declaratively through
  data-dictionary and provider nodes, with response caching and bounded concurrency.
- **Up to any protocol** — a deployed graph is exposed by the `graph-executor` flow as
  `POST /api/graph/{graph-id}`, so REST (or, via connectors, Kafka events) can invoke it like any
  other endpoint. Protocol stays decoupled from execution.

![Active Knowledge Graph architecture](../diagrams/active-knowledge-graph.png)

## Designing graphs: the Playground & AI companion {#playground}

The **MiniGraph Playground** is a browser workbench (served over a WebSocket session at
`/ws/graph`) for building, dry-running, and inspecting graphs interactively. Its command grammar
covers the full lifecycle — `create node`, `connect`, `instantiate graph`, `run`, `inspect`,
`describe graph`, and `export/import graph`.

The same commands can be driven by an **AI companion**: `POST /api/companion/{session-id}` accepts a
Playground command (as `text/plain`) and dispatches it into your live session, with output streaming
back to the browser console. This is the seed of **user–AI collaboration** — a person and an AI
co-authoring the same graph in real time. (Both the Playground and companion endpoints are
**dev-only**, gated by `app.env=dev`.)

## What's built today vs. on the roadmap {#maturity}

To keep this honest — the engine is real, the enterprise lifecycle is partly aspirational:

**Available now:** interactive model authoring; instance creation (`instantiate graph` with mock
data); execution (`run`, or single-node `execute`); state-machine inspection; JSON `export`/`import`;
**dry-run** in the Playground; collaborative multi-user sessions; loop detection.

**On the roadmap (not yet in code):** the governance lifecycle (certify → stage → approve → promote)
described as the target operating model; graph versioning; access control on the dev endpoints; and
persistence of sessions across restarts. Where this Part describes those, it marks them as the
*intended* lifecycle, not current behavior.

## See also {#see-also}

- [Build your first Active Knowledge Graph](build-your-first-graph.md) — a hands-on walkthrough: model a service, dry-run it, deploy it, and call it over REST.
- [Built-in skills reference](skills-reference.md) — the seven `graph.*` skills with syntax and worked examples.
- [Composing the layers](composing-the-layers.md) — external APIs, sub-graph/flow extension, and REST exposure.
- [Playground & AI companion](playground-and-companion.md) — the interactive workbench and user–AI collaboration.
- [Minimalist Property Graph](property-graph.md) — the underlying graph data structures and API.
- [Event Script Syntax](../event-script/syntax.md) — the composable flow DSL that `graph.extension` delegates to.
- [Home](../../index.md) — Mercury's three-layer model and where this layer fits.
