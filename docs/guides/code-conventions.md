---
title: Code Conventions
summary: How Mercury application code is organized and named — package layout, route naming, and function
  granularity recommendations that keep a composable codebase readable as it grows.
layer: reference
audience: [developer, contributor, ai-agent]
keywords: [code conventions, package layout, tasks package, route naming, function granularity, code style]
---

# Code Conventions

*Reference: Recommended conventions for organizing and naming Mercury application code.*

> **At a glance**
>
> - **What** — soft, evolving recommendations for how to organize and name code in a composable
>   application: package layout, route naming, function granularity.
> - **Why it matters** — composable apps are many small functions wired by route names; a little naming
>   and layout discipline keeps the whole readable and keeps route collisions out.
> - **For** developers and AI agents writing functions, flows, and the classes around them.

## Altitude — where this fits

These are **recommendations**, not hard rules. They sit below the two harder tiers:

- **[Architectural Decision Records](../arch-decisions/ADR.md)** — durable architecture *decisions* with
  rationale (the *why*); changing one is a governed event.
- **[Methodology](methodology.md)** — the four composable design *principles* (the philosophy).
- **Code conventions (this page)** — evolving "how we organize and name code." Add here when a practice is
  worth sharing but is a matter of readability or team convention rather than a binding decision.

If a guideline turns out to be load-bearing (breaking it breaks the system), promote it to an ADR instead.

## Package organization

Group classes by their **role in the composable model**, not by technical type:

- **Flow-task functions** — functions that exist to be wired as Event Script tasks (a flow's `process:`
  steps) go in a **`tasks` package** (e.g. `org.platformlambda.tasks`). Collecting them in one place makes
  the set of "things a flow can call" obvious at a glance and separates them from runtime plumbing. The
  `sync-over-async` extension follows this: `SyncPrepareTask` and `SyncAwaitTask` (the `sync.prepare` /
  `sync.await` facade tasks) live under `tasks`, while the return-route coordinator and its helpers stay in
  the feature package.
- **Runtime / engine classes** — coordinators, stores, autoloaders, and other plumbing stay in the
  **feature package** (e.g. `org.platformlambda.sync`), not under `tasks`.
- **Configuration holders** — config readers and constants go in a **`support`** (or `config`) package.

This is a readability convention; the framework does not require it — `@PreLoad` discovery scans the
configured `web.component.scan` base regardless of sub-package, so a function registers wherever it lives.

## Route naming

A function's **route name is its whole contract** with every flow that calls it (see
[ADR-0001](../arch-decisions/ADR.md) and [ADR-0005 — one atom, four roles](../arch-decisions/ADR.md)),
so name with care:

- Use **lowercase dotted** names: `v1.get.profile`, `sync.prepare`, `system.of.record`.
- Prefer a **stable, intent-revealing** name over an implementation detail — flows reference the string,
  so renaming a route is a breaking change to every flow that uses it.
- **Do not overload a reserved route.** Library and extension functions register their own routes; see
  [Reserved Names & Headers](reserved-names-and-headers.md) for the do-not-collide list (which includes
  the shipped `sync.prepare` / `sync.await` / `simple.kafka.notification` routes).

## Function granularity

- Keep a function a **single unit of work** (input → process → output). Sequencing, branching, and
  failure handling are **orchestration** and belong in the Event Script flow, not inside the function
  (see [ADR-0007](../arch-decisions/ADR.md)).
- Reserve in-function logic for what a flat task chain genuinely cannot express — e.g. a synchronous
  blocking rendezvous that must wrap an asynchronous call.

## See also

- [Architectural Decision Records](../arch-decisions/ADR.md) — the durable decisions these conventions realize.
- [Methodology](methodology.md) — the four composable design principles.
- [Reserved Names & Headers](reserved-names-and-headers.md) — routes and headers not to overload.
- [Function AI agent guide](event-driven/ai-agent-guide.md) — the `@PreLoad` + `TypedLambdaFunction` contract.
