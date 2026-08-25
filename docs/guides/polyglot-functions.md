---
title: Polyglot Functions
summary: Write functions in Python or Node.js and let Event Script flows and MiniGraph
  knowledge graphs call them as if they were local - Event-over-HTTP peers, not ports.
layer: operate
audience: [developer, architect]
keywords: [polyglot, python, node.js, event over http, wrapper, yaml.event.over.http,
  graph.task, interop]
---

# Polyglot Functions

*Guide: How to write composable functions in Python or Node.js and wire them into flows
and knowledge graphs.*

> **At a glance**
>
> - **What** — official lightweight wrappers let you write functions in **Python** and
>   **Node.js**; the engine calls them through the declarative
>   [Event-over-HTTP](event-over-http.md) map with **zero orchestration code** in the
>   foreign language.
> - **For** teams whose unit of work lives in another ecosystem — Python ML inference and
>   data tooling, Node.js services — while flows and graphs stay on the engine.
> - **Versions** — Event Script flows call polyglot functions on any engine that speaks
>   the standard wire format; MiniGraph `graph.task` requires engine **v4.11.11+**.

## Why peers, not ports

A Mercury function is already fully decoupled: it is addressed by a route-name string and
receives an `EventEnvelope`. Nothing in that contract says "Java" — so the shortest path
to another language is not porting the composable core, it is letting a function in that
language **speak the same envelope over the existing Event API endpoint**.

That is exactly what the polyglot wrappers are:

| Package | Repository | Documentation |
|---------|------------|---------------|
| Composable for Python | [Accenture/mercury-python](https://github.com/Accenture/mercury-python) | <https://accenture.github.io/mercury-python/> |
| Composable for Node.js | [Accenture/mercury-nodejs](https://github.com/Accenture/mercury-nodejs) | <https://accenture.github.io/mercury-nodejs/> |

Each wrapper is a long-lived **Event API peer**: an HTTP host serving `POST /api/event`
with the engines' exact semantics, a function registry with the engines' `preload`
contract (route name, `instances`, private visibility), a thin `PostOffice` client, a
primitive in-process event bus for leaf-side composition, and the minimalist utilities
(configuration, logging, trace context) in engine-consistent presentation. The envelope
codec implements the language-neutral
[standard wire format](event-envelope-wire-format.md) and is verified against the same
golden conformance vectors the Java and Rust engines share.

Just as important is what the wrappers deliberately **do not** contain: no flows, no
graphs, no persistence, no pub/sub. Orchestration — sequencing, branching, retries,
compensation — stays in Event Script and MiniGraph on the engine, where it is
declarative, inspectable, and governed. A polyglot function is a **unit of work**; the
architecture keeps it that way.

> Why not subprocesses? A child-process design was investigated and shelved: it binds the
> engine to foreign runtime lifecycles (kernel-thread pinning, process-tree cleanup,
> per-call interpreter startup) and creates a stability surface the peer model simply
> does not have. A peer scales, deploys, and restarts independently — it is an ordinary
> application in its own ecosystem.

## Wiring: one map entry per route

Polyglot functions ride the same declarative mechanism as any remote engine function —
the [`yaml.event.over.http` map](event-over-http.md#event-over-http-using-configuration).
On the engine application:

```properties
yaml.event.over.http=classpath:/event-over-http.yaml
```

```yaml
event.http:
  - route: 'hello.declarative'
    target: 'http://${peer.demo.host:127.0.0.1}:${peer.demo.port}/api/event'
```

That is the entire integration surface. Any flow task or `graph.task` node that names
`hello.declarative` now executes the Python (or Node.js) function — the flow and the
graph neither know nor care.

### The zero-code demo, third language

The [Event over HTTP zero-code demo](event-over-http.md#zero-code-demo) already swaps its
Java callee for the Rust hello-world with no changes. The polyglot wrappers extend the
same swap: their demo apps register the same public `hello.declarative` route, and the
wrapper's default port is 8085 — the lambda-example's slot.

1. Start the composable-example as in the demo walk-through.
2. Instead of the lambda-example, start the Python demo (from a clone of
   mercury-python, after its documented setup):

    ```shell
    mercury-serve examples/demo_app.py -Drest.server.port=8085
    ```

    or the Node.js demo (from a clone of mercury-nodejs, after `npm install` and
    `npm run build`):

    ```shell
    node dist/src/cli.js examples/demo-app.mjs -Drest.server.port=8085
    ```

    (The `-Dkey=value` override syntax is the engines' own — the wrappers carry the same
    configuration conventions, so operating them feels identical.)

3. Run the same `curl`:

    ```shell
    curl -s -X POST -H "content-type: application/json" \
         -d '{"hello": "world"}' http://127.0.0.1:8100/api/event/http/declarative
    ```

The reply now carries `"language": "python"` (or `"node.js"`), and the wrapper's log line
shows the **engine's trace id** in the engine-consistent log format. This exact drive —
the shipped `event-over-http-declarative` flow executing the Python function unchanged,
with the flow's business correlation id injected as `my_correlation_id` — was the
acceptance proof of the polyglot design (see the
[interop report](../test-reports/event-over-http-interop.md#the-polyglot-wrapper-round-2026-08-22)).

## Calling from a knowledge graph

`graph.task` invokes composable functions from graph nodes, and a declarative
Event-over-HTTP target is a composable function. From engine **v4.11.11** the deployment
gate and the executor consult the `yaml.event.over.http` map, so a deployed graph can
name a polyglot route directly:

```text
skill=graph.task, task=hello.declarative
```

On engines before v4.11.11 the route-existence guard only checked local routes and a
graph naming a remote target failed validation — upgrade both the Java and Rust engines
to at least v4.11.11 before pointing graphs at polyglot functions.

## The contract, end to end

The function contract is the engines' own, restated in each wrapper's documentation
(start at the wrapper's *AI agent guide* for the token-efficient version):

- **Input** — `(headers, body)` exactly as a `TypedLambdaFunction` sees them; reserved
  engine headers are cleaned at ingress; the caller's business correlation id arrives as
  the read-only `my_correlation_id` header.
- **Errors are portable** — a wrapper `AppException(400, "missing 'text'")` becomes a
  400 envelope on HTTP 200 (handler errors ride HTTP 200 with envelope status, exactly
  like the engines); the calling flow's `exception:` task or the graph's `error.*`
  context fires as if the function were local. Transport-level failures keep the engine
  status codes: 403 private target, 404 unknown route, 408 timeout.
- **Trace continuity** — the engine's trace id and path ride the wire; wrapper telemetry
  and log lines join the same aggregated trace, and `annotate_trace` entries return on
  the reply envelope.
- **Timeouts** — the flow's `ttl` (or the graph's) bounds the call; on breach the engine
  receives the standard 408 and the exception path decides recovery. Back-pressure and
  retries belong to the engine tier by design — the wrappers keep no spill queue.
- **Wire format** — the wrappers speak the **standard** envelope format only (the
  engines' default for Event over HTTP); the classic compact format is rejected with a
  teaching error.

## Operating a polyglot installation

The wrappers serve the engines' actuator endpoints on the same port as `/api/event` —
`/info`, `/info/routes`, `/env`, `/health`, `/livenessprobe` with the `type=info` /
`type=health` health-function contract — and their `log.format` carries the engines'
`text` / `json` / `compact` presentations. Kubernetes probes, dashboards, and log
aggregation treat a Python or Node.js app exactly like an engine app: one operational
surface, no per-language tooling. This is the
[telemetry presentation parity](../test-reports/event-over-http-interop.md) requirement
extended to the wrapper family.

## See also

- [Event over HTTP](event-over-http.md) — the underlying mechanism, endpoint security,
  and the full demo walk-through.
- [Composable for Python](https://accenture.github.io/mercury-python/) and
  [Composable for Node.js](https://accenture.github.io/mercury-nodejs/) — write the
  functions: handler styles, local composition, testing, AI agent guides.
- [Interop Test Report](../test-reports/event-over-http-interop.md) — the conformance
  evidence, including the polyglot wrapper round.
- [Event Envelope Wire Format](event-envelope-wire-format.md) — the language-neutral
  contract everything above rides on.
