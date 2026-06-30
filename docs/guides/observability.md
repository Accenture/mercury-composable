---
title: Observability
summary: How Mercury's built-in distributed tracing produces an end-to-end span tree across all three
  layers, and how it exports to any OpenTelemetry backend over OTLP while preserving W3C trace context.
layer: operate
audience: [developer, ai-agent]
keywords: [observability, distributed tracing, opentelemetry, otlp, telemetry, w3c trace context,
  spans, dynatrace, splunk, jaeger, tempo, log context, structured logging, json logger, mdc,
  correlation id, updateContext]
---

# Observability

*Guide: how the built-in distributed tracing works, and how to export it to OpenTelemetry.*

> **At a glance**
>
> - **What** — every traced request produces a causal **span tree** across the three layers; the system
>   stamps W3C-compatible trace/span IDs automatically and can ship the telemetry to any OpenTelemetry backend.
> - **Built-in** — distributed tracing is part of the platform; you turn it on per endpoint/flow and the
>   spans propagate without code.
> - **For** developers and operators wiring Mercury to Dynatrace, Splunk, Jaeger, Tempo, or an OpenTelemetry Collector.
> - **Logs too** — an opt-in [application log context](#log-context) stamps the same correlation/trace ids (plus your
>   own key-values) into structured log lines, so logs and spans join up in your backend.

In an event-driven, composable system a single request fans out across **decoupled** functions, flows, and graph
nodes that never call each other directly. Observability is therefore not optional — it is the only way to see the
causal path a request actually took. Mercury answers this with a built-in distributed-tracing engine whose output
is **OpenTelemetry-compliant**, so the spans drop straight into the dashboard you already run.

## The built-in tracing design {#design}

### Turning tracing on {#enable}

Tracing is opt-in per entry point:

- **HTTP endpoints** — add `tracing: true` to the `rest.yaml` entry (see [REST Automation](rest-automation/index.md)).
- **Event Script flows** — a flow started through `FlowExecutor` carries a trace; the engine traces every task.
- **Programmatically** — construct a trace-aware `PostOffice(fromRoute, traceId, tracePath)` and the events it
  sends are traced end-to-end.

Two controls tune what is recorded:

- `@ZeroTracing` on a function suppresses tracing for that function (used by system services so they never trace themselves).
- `skip.rpc.tracing` (in `application.properties`) lists route names excluded from trace recording; the default is
  `async.http.request`.

### What a trace records {#metrics}

When a traced function finishes, the system sends a performance-metrics dataset to the built-in
`distributed.tracing` service. The dataset is a plain map:

```text
trace={ id=<32-hex trace id>, span_id=<16-hex>, parent_span_id=<16-hex>,
        service=<route name>, path=<request path>, from=<caller route>,
        origin=<instance id>, start=<ISO-8601>, exec_time=<ms>, round_trip=<ms>,
        success=<bool>, status=<int>, exception=<text> }
annotations={ <your key>=<value>, ... }
```

`span_id` and `parent_span_id` are the OpenTelemetry-compatible IDs (see [W3C trace context](#w3c)); you can attach
business context to the span with `PostOffice.annotateTrace(key, value)`. To attach context to **application logs**
instead, see [Application log context](#log-context).

### Spans across the three layers {#layers}

The span tree mirrors the [three paradigm layers](architecture.md), and tracing is **virtual-thread-safe** — the
parent span is threaded through a per-task anchor, not a `ThreadLocal`, so concurrently-dispatched siblings still
share the correct parent.

| Layer | What becomes a span | Lineage |
|:------|:--------------------|:--------|
| **Layer 1 — Platform Core** | each function execution | the caller's span becomes the child's `parent_span_id` |
| **Layer 2 — Event Script** | each task, **plus one synthetic `task.executor` flow-summary span** (annotated with the flow id) | tasks chain exactly like Layer 1; a sub-flow chains to the parent task that dispatched it |
| **Layer 3 — Knowledge Graph** | each node dispatch through `graph.executor` | the graph traversal threads the parent span through node execution |

Because Layer 2 and Layer 3 ride on Layer 1's engine, the task/node spans look identical to Layer-1 spans — the only
addition at Layer 2 is the synthetic flow summary that brackets the whole flow's timing.

## W3C Trace Context (OpenTelemetry compliance) {#w3c}

Mercury's trace and span IDs follow the [W3C Trace Context](https://www.w3.org/TR/trace-context/) format that
OpenTelemetry uses: a **32-hex trace ID** and **16-hex span ID**. Across an HTTP boundary the system propagates the
standard `traceparent` header:

- outbound — the HTTP client injects `traceparent` carrying the current trace + span;
- inbound — the HTTP layer extracts `traceparent`, continues the upstream trace, and adopts the caller's span as the parent.

For a transition period you can also emit the legacy `X-Trace-Id` header alongside `traceparent` — controlled by
`trace.http.legacy.header.enabled` (default `true`; set `false` once every service is on OpenTelemetry). Inbound
acceptance of the legacy header is unaffected by the flag.

## Exporting telemetry {#export}

### The forwarder extension point {#forwarder}

By default the trace dataset is logged. To ship it elsewhere, register a function at the reserved route
`distributed.trace.forwarder`; the system detects it and forwards every dataset to it. A companion hook,
`transaction.journal.recorder`, receives request/response payloads when journaling is enabled (see
[Reserved Names & Headers](reserved-names-and-headers.md) and [Build, Test & Deploy](build-test-deploy.md)).

### The OpenTelemetry forwarder (ready-made) {#otel-forwarder}

You do not have to write the forwarder for OpenTelemetry. The **`opentelemetry-forwarder`** extension ships a
`distributed.trace.forwarder` that maps each dataset to an OpenTelemetry span — preserving the **exact** W3C
trace/span/parent-span IDs — and exports it over **OTLP/HTTP** to a collector (and on to Dynatrace, Splunk, Jaeger,
Tempo, …). Add the dependency and it auto-registers; no code required:

```xml
<dependency>
    <groupId>org.platformlambda</groupId>
    <artifactId>opentelemetry-forwarder</artifactId>
    <version>4.5.0</version>
</dependency>
```

Configure it in `application.properties` (values support `${ENV_VAR:default}` substitution):

```properties
otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
otel.service.name=${OTEL_SERVICE_NAME:my-app}
# Backend credentials come from the environment — no secret hard-coded:
otel.exporter.otlp.headers=${OTEL_EXPORTER_OTLP_HEADERS}
```

Point the endpoint at an OpenTelemetry Collector, or directly at a SaaS backend with its API token in the headers:

```bash
# Dynatrace
export OTEL_EXPORTER_OTLP_ENDPOINT="https://{env-id}.live.dynatrace.com/api/v2/otlp/v1/traces"
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Api-Token dt0c01.XXXX"

# Splunk Observability Cloud
export OTEL_EXPORTER_OTLP_ENDPOINT="https://ingest.{realm}.signalfx.com/v2/trace/otlp"
export OTEL_EXPORTER_OTLP_HEADERS="X-SF-Token=YOUR_ACCESS_TOKEN"
```

Each span carries the route name (`route`), `path`, `from`, `origin`, `status`, timing, and your `annotation.*`
values as span attributes, with `service.name` on the resource. See the full key reference in the
[Configuration Reference](configuration-reference.md#observability).

### A custom forwarder {#custom-forwarder}

To target a system without an OTLP path, implement your own function at `distributed.trace.forwarder` and consume
the dataset described in [What a trace records](#metrics) — for example, write it to a metrics database or a
proprietary APM API.

## Application log context {#log-context}

Spans tell you the causal path; **application logs** tell you what happened inside each step. The log-context
feature closes the gap between them: it injects a `context` block — correlation id, trace/span ids, service name,
and any business key-values you add — into every structured log line a traced function emits. With the same
`traceId`/`spanId` on both the span and the log line, you can pivot from a Dynatrace/Splunk trace straight to the
exact log entries that belong to it.

It deliberately **avoids the ThreadLocal / Log4j MDC** pattern (heavy for a virtual-thread runtime). The context
rides the same per-request mechanism as the trace itself, keyed to the worker thread and torn down when the
function returns.

### Turning it on {#log-context-enable}

The feature is **opt-in** and activates only when an optional `app-log-context.yaml` is on the classpath
(`src/main/resources/`). It applies to the two **structured JSON appenders** — select one via the log4j2
configuration (`log4j2-json.xml` for pretty output, `log4j2-compact.xml` for single-line); the plain `Console`
appender is unaffected.

```yaml
# src/main/resources/app-log-context.yaml
context:
  cid: $cid
  traceId: $traceId
  tracePath: $tracePath
  spanId: $spanId
  parentSpanId: $parentSpanId
  service: $service
  timestamp: $utc
  environment: '${ENV_NAME:dev}'
  hello: world
```

The **left side** is the output key (your choice). The **right side** is one of three forms:

| Form | Example | Resolved |
|:-----|:--------|:---------|
| Reserved **`$token`** | `service: $service` | live, per log line, from the request's trace context |
| **`${ENV:default}`** substitution | `environment: '${ENV_NAME:dev}'` | once at startup, from the environment |
| **Hardcoded literal** | `hello: world` | emitted verbatim on every line |

The reserved tokens are `$cid`, `$traceId`, `$tracePath`, `$spanId`, `$parentSpanId`, `$service` (the current
function's route), and `$utc` (the log line's UTC timestamp). A token (or env value) that resolves to nothing is
**omitted** from the block rather than printed as `null` — so a root span simply has no `parentSpanId` key.

### Adding your own key-values {#log-context-custom}

Inside a function, add business context with `PostOffice.updateContext(key, value)`:

```java
var po = new PostOffice(headers, instance);
po.updateContext("user", "demo");   // appears in the context block of every subsequent log line
log.info("processing request");
```

The reserved keys (`cid`, `traceId`, `tracePath`, `spanId`, `parentSpanId`, `service`, `utc`) are protected —
passing one to `updateContext` throws `IllegalArgumentException`. On a non-traced request, or when the feature is
off, the call is a silent no-op.

> **`updateContext` vs `annotateTrace`** — two distinct sinks. `annotateTrace(...)` attaches business data to the
> **distributed-trace dataset** that flows to your APM backend ([What a trace records](#metrics));
> `updateContext(...)` attaches it to the **application log** stream only. Neither leaks into the other.

### What it looks like {#log-context-output}

A log line from a traced function then carries the resolved `context` (from the worked example above, with
`po.updateContext("user", "demo")` added in the function):

```json
{
  "level": "INFO",
  "context": {
    "cid": "20260630c6ee70d866cb4fae9ab3c44d926ce21a",
    "traceId": "fbb60df209084531b2b00f6b36a3e651",
    "tracePath": "GET /api/profile/100",
    "spanId": "bf8d4b2b6a923d67",
    "parentSpanId": "98f8e26ae7d9a422",
    "service": "v1.hello.exception",
    "environment": "dev",
    "hello": "world",
    "user": "demo",
    "timestamp": "2026-06-30T21:17:03Z"
  },
  "time": "2026-06-30 14:17:03.575",
  "source": "com.accenture.demo.tasks.HelloException.handleEvent(HelloException.java:51)",
  "thread": 297,
  "message": "User defined exception handler - status=404 error=Profile 100 not found"
}
```

The `traceId` and `spanId` here match the `v1.hello.exception` span the tracer emitted for the same request, so the
log line and the span join up in your backend. (Key order within `context` is not significant — log viewers reorder
keys on display.)

### Scope and boundaries {#log-context-scope}

- The `context` block appears **only** when a request is traced and a `traceId` is present. Framework boot logs and
  logs emitted from a `Mono`/`Flux` completion that runs **after** the worker returns (on a different thread) carry
  no context — the same boundary distributed tracing has.
- Feature **off** (no `app-log-context.yaml`) costs one boolean check per log line and nothing else.

## See also {#see-also}

- [Build, Test & Deploy](build-test-deploy.md) — enabling tracing and the forwarder hook.
- [Configuration Reference](configuration-reference.md#observability) — the `trace.*` and `otel.*` keys.
- [Reserved Names & Headers](reserved-names-and-headers.md) — `distributed.trace.forwarder`, `traceparent`.
- [Architecture Overview](architecture.md) — the three layers the span tree mirrors.
