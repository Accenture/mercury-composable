---
title: Composable Orchestration
summary: Layer 2 — composable orchestration. Event Script moves orchestration out of code into YAML flows
  that choreograph functions by route name, run by the flow engine with a per-transaction state machine.
layer: composable
audience: [developer, architect]
keywords: [event script, composable orchestration, flow, task, execution type, state machine, layer 2, choreography]
related:
  - guides/event-script/syntax.md
  - guides/event-driven/index.md
  - guides/knowledge-graph/composing-the-layers.md
---

# Composable Orchestration

*Overview: Layer 2 — choreograph functions as YAML flows with Event Script.*

> **At a glance**
>
> - **What** — **Event Script**, a YAML DSL that describes an end-to-end transaction as a **flow** that
>   choreographs functions — orchestration as configuration, not code.
> - **Why it matters** — changing how a transaction runs means editing a flow, not rewriting Java;
>   roughly half config, half code.
> - **Where it sits** — **Layer 2** of the ascent, built on the [event-driven foundation](../event-driven/index.md).
> - **For** developers and architects orchestrating composable functions.

Event Script is a Domain Specific Language (DSL) that uses YAML to represent an end-to-end transaction
flow. A transaction is a business use case, and the flow can be an API service, a batch job or a
real-time transaction.

It moves orchestration *out of code and into configuration*: instead of writing Java that calls one
function after another, you describe the sequence as a **flow** in YAML. Roughly half the work becomes
configuration, half stays code (the functions themselves). A flow builds on the
[event-driven foundation](../event-driven/index.md) beneath it — it never references a function's
class, only its **route name**.

## The mental model

- A **flow** is an ordered set of **tasks** with one entry point (`first.task`) and one or more terminal tasks.
- Each **task** runs a **function** (named by route) — a task *is* a function in its flow-step role. Input
  and output **data mapping** move values between the flow and the function's scope.
- A task's **execution type** decides what happens next: `sequential`, `decision`, `parallel`, `fork`,
  `pipeline`, `response`, `end`, or `sink`.
- A per-transaction **state machine** (`model`) holds intermediate results across the stateless functions.
- A **flow adapter** (HTTP via `rest.yaml`, or Kafka) drives a flow from outside and returns its result.

## See also

- [Event Script Syntax](syntax.md) — the full DSL: flow files, writing a flow end to end, every task type, and the data-mapping mini-language.
- [Flow grammar](flow-grammar.md) — the rule-based schema (the deterministic spec).
- [Flow AI agent guide](ai-agent-guide.md) — author flows deterministically with an agent.
- [Flow Configuration Schema](../flow-schema-reference.md) — exhaustive field reference.
- [Event-driven Foundation](../event-driven/index.md) — the Layer-1 core flows build on (down the ascent).
- [Knowledge Graph as Application](../knowledge-graph/index.md) — the semantic layer above (Layer 3).
- [Composing the layers](../knowledge-graph/composing-the-layers.md) — how the three layers stack.
