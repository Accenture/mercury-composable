---
title: Observability
summary: How Mercury's built-in distributed tracing produces an end-to-end span tree across all three
  layers, and how it exports to any OpenTelemetry backend over OTLP while preserving W3C trace context.
layer: operate
audience: [developer, ai-agent]
keywords: [observability, distributed tracing, opentelemetry, otlp, telemetry, w3c trace context,
  spans, dynatrace, splunk, jaeger, tempo]
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
business context with `PostOffice.annotateTrace(key, value)`.

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

## See also {#see-also}

- [Build, Test & Deploy](build-test-deploy.md) — enabling tracing and the forwarder hook.
- [Configuration Reference](configuration-reference.md#observability) — the `trace.*` and `otel.*` keys.
- [Reserved Names & Headers](reserved-names-and-headers.md) — `distributed.trace.forwarder`, `traceparent`.
- [Architecture Overview](architecture.md) — the three layers the span tree mirrors.
