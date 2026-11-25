---
title: Polyglot Functions
summary: Write functions in Python or Node.js and let Event Script flows and MiniGraph
  knowledge graphs call them as if they were local - Event-over-HTTP peers, not ports.
layer: operate
audience: [developer, architect]
keywords: [polyglot, python, node.js, event over http, wrapper, yaml.event.over.http,
  graph.task, interop, rest automation, rest.yaml, application server]
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
> - **And an application server** — with the engines' REST automation in the packs
>   (RFC-0018), `rest.yaml` is the router in front of the functions, so a Python or Node.js
>   utility application is built and deployed as an engine application is; see
>   [REST automation in the packs](#rest-automation-in-the-packs).
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
architecture keeps it that way. The HTTP edge *is* in scope: since RFC-0018 a pack serves
the engines' REST automation — ingress configuration in front of its functions, never a
flow (a `flow` binding is refused by name); see
[REST automation in the packs](#rest-automation-in-the-packs).

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
    mercury-serve examples/demo-app/demo_app.py -Drest.server.port=8085
    ```

    or the Node.js demo (from a clone of mercury-nodejs, after `npm install` and
    `npm run build`):

    ```shell
    node dist/src/cli.js examples/demo-app/demo-app.mjs -Drest.server.port=8085
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

## REST automation in the packs

Since RFC-0018 (2026-11-25) a pack is also a **full-service application server** for the
field's utility applications. With `rest.automation: true` the host serves the entries of
`rest.yaml` — the engines' [REST automation](rest-automation/index.md), in the engines'
[grammar](rest-automation/rest-grammar.md) — in front of its functions, on the one server
and port, so a Python or Node.js utility application is designed, configured and deployed as
an engine application is: functions, a `rest.yaml` and a web bundle, no web framework of its
own. The file is read from `yaml.rest.automation` (one location; the default
`classpath:/rest.yaml` is `resources/rest.yaml` next to the application file, under the
packs' resources convention) and a misconfigured entry fails the start with the engines'
message.

```yaml
rest:
  - service: "hello.rest"
    methods: ['GET']
    url: "/api/hello/{name}"
    timeout: 10s
    cors: cors_1
    tracing: true
```

```python
@preload(route="hello.rest", instances=10)
async def hello_rest(headers: dict[str, str], body: Body):
    request = AsyncHttpRequest(body)                      # the engines' request map, never the bare body
    return {"greeting": f"Hello, {request.path_parameter('name')}"}
```

**What a pack serves** — version 1 is the Rust engine's subset, with Java parity where the
Rust engine has it: the function binding; `{param}` and a trailing `*`, the most specific
route winning; `timeout` (1 s to 5 min, an inbound `x-ttl` winning); `cors` with the
preflight; `headers` keep, drop and add; `authentication` — a function whose `true` verdict
lets the request through and whose reply headers ride as the `session` map; `tracing` with
`traceparent` first, then the trace-id header, else a fresh id, the correlation id always
ensured and echoed, and the three per-endpoint header-name overrides; `stream: true`
rendered as Server-Sent Events or chunked text on the pack's reply sink; the content-type
dispatch and the `Accept` negotiation; the standard error body; `static-content` with its
`no-cache-pages` and `filter`; `POST /api/event` and the actuators as the default entries,
so a user entry can attach `authentication`, CORS or a timeout to the Event API itself.
`upload: true` is the packs' **bounded form** of the engines' streaming upload: the files of
a `multipart/form-data` body arrive in the request map under the engines' keys with their
bytes in `contents`, all together within `rest.upload.max.bytes` (16 MiB). The function
receives the engines' `AsyncHttpRequest` map and replies with a value, an envelope or an
exception, exactly as a function behind REST automation does on the engine; a traced call
leaves the `http.request` record and the function's round trip, so the OpenTelemetry
forwarder exports the request as a SERVER span.

**What a pack refuses**, by name at load, so the grammar itself shows the scope fence: a
`flow` binding (flows run in the engines), the HTTP relay (an `http(s)://` service) and the
A/B dual service. Deferred until a field request, as in the Rust engine: `custom.content.types`,
`mime.types`, XML bodies, request-body streaming and several file locations. No framework and
no new dependency: aiohttp in Python, `node:http` in Node.js.

The packs' own applications are the worked examples: the LLM helper's document intake
(`POST /api/llm/upload`), the ask-person reply page and endpoint, and the agent-question
app's feed, history, transcripts and click — a device's Server-Sent Events stream as a
`stream: true` entry and the sign-in as an authentication function among them. The two
packs build it in lockstep from one vector file (`rest-automation-vectors.json`,
byte-identical), so a `rest.yaml` loads and answers the same in either. The full guides:
[REST Automation for Python](https://accenture.github.io/mercury-python/guides/rest-automation/)
and [for Node.js](https://accenture.github.io/mercury-nodejs/guides/rest-automation/); an
agent authoring an entry starts at the
[REST AI agent guide](rest-automation/ai-agent-guide.md#language-packs).

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
- [REST Automation](rest-automation/index.md) — the engines' `rest.yaml`, which the packs
  serve too (RFC-0018); the packs' guides:
  [Python](https://accenture.github.io/mercury-python/guides/rest-automation/) ·
  [Node.js](https://accenture.github.io/mercury-nodejs/guides/rest-automation/).
- [Interop Test Report](../test-reports/event-over-http-interop.md) — the conformance
  evidence, including the polyglot wrapper round.
- [Built-in Agent Tools](agent-tools.md) — the packs' agent-tools extension: the file, web and clock
  tools an agent loop names in its catalog, reached over the same map.
- [Event Envelope Wire Format](event-envelope-wire-format.md) — the language-neutral
  contract everything above rides on.
