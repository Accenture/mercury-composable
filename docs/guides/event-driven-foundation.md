---
title: Event-driven Foundation
summary: Layer 1 — the Platform Core. Fully decoupled functions that communicate only by route name and
  EventEnvelope over an in-memory event bus, with Java 21 virtual threads making synchronous code perform
  like reactive.
layer: platform-core
audience: [developer, architect]
keywords: [event-driven, platform core, function, route name, postoffice, event envelope, virtual threads, event bus]
related:
  - guides/function-execution.md
  - guides/rest-automation/index.md
  - guides/architecture.md
---

# Event-driven Foundation

*Overview: Layer 1 — the event-driven core that every higher layer is built on.*

> **At a glance**
>
> - **What** — the Platform Core: self-contained **functions** that know each other only by **route
>   name**, exchanging immutable **`EventEnvelope`** messages over an in-memory event bus.
> - **Why it matters** — this decoupling is the foundation the whole ascent reuses; the same function
>   becomes a *service*, a *task*, or a *skill* higher up without changing.
> - **Where it sits** — the bottom of Mercury's three layers: **Event-driven → Composable → Semantic**.
> - **For** developers and architects building or reasoning about the core.

## Functions, addressed by name

The building block is a **function** — a class annotated `@PreLoad` implementing `LambdaFunction` or
`TypedLambdaFunction<I, O>`, with Map-or-PoJo input and output. A function is **self-contained**: it
holds no direct reference to any other user function. Its only identity on the bus is a **route name**
(`@PreLoad(route = "my.function")`), and that name is the *only* thing other components use to reach it.

Because coupling is a string, not an `import`, you can replace, version, or relocate a function without
touching its callers. (*How* a function runs — virtual threads, kernel threads, or reactive `Mono`/`Flux`
— is covered in [Function Execution Strategies](function-execution.md).)

## Talking through the event bus

Functions never call each other directly. They send events through the **`PostOffice`**, the
inter-function messaging API. Every message is an immutable **`EventEnvelope`** — headers
(`Map<String,String>`), a body (serialized with MsgPack), and metadata such as a correlation id and
optional tracing. Delivery comes in two modes:

- **Point-to-point** — the event reaches exactly one worker instance of the target route (the default
  for request/response).
- **Broadcast** — the event reaches every registered instance of the route.

A `PostOffice` request *reads* as synchronous — you send, and the reply arrives on the next line — but it
never blocks a platform thread.

## Why synchronous code performs like reactive

The bus runs on the **Eclipse Vert.x** event loop, and functions execute on **Java 21 virtual threads**.
A virtual thread waiting for a reply is parked almost for free, so straightforward sequential code reaches
the throughput usually associated with callback-style reactive code — without the callback complexity.
(The lineage and the full request pipeline are in the [Architecture Overview](architecture.md).)

## The HTTP boundary

To expose a function — or a flow — over HTTP, you don't write a controller; you declare it in `rest.yaml`.
A function mapped straight to an endpoint via `service:` is a **service**; see
[REST Automation](rest-automation/index.md) for the full declarative model.

## See also

- [Function Execution Strategies](function-execution.md) — virtual vs. kernel threads, `Mono`/`Flux`.
- [REST Automation](rest-automation/index.md) — declare HTTP endpoints in `rest.yaml`, no controllers.
- [Architecture Overview](architecture.md) — the full request pipeline and the actor-model origin.
- [Methodology](methodology.md) — the four composable design principles.
