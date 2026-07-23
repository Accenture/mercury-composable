---
title: Reserved Names & Headers
summary: System-reserved route names and HTTP headers that must not be overridden.
layer: reference
audience: [developer, reference]
keywords: [reserved names, reserved headers, routes, system, reference]
---

# Reserved names

*Reference: System-reserved route names and HTTP headers that must not be overridden.*

> **At a glance**
>
> - **What** — the route names and HTTP headers the platform reserves for its own routing.
> - **Why it matters** — overloading a reserved name or header can silently break system routing;
>   this is the do-not-collide list.
> - **For** developers naming functions and setting headers.

The system reserves some route names and headers for routing purpose.

## System route names

The Mercury foundation code is written using the same core API and each function has a route name.

The following route names are reserved. Please DO NOT overload them in your application functions
to avoid breaking the system unintentionally.

| Route                        | Purpose                               | Modules          |
|:-----------------------------|:--------------------------------------|:-----------------|
| actuator.services            | Actuator endpoint services            | platform-core    |
| info.actuator.service        | Info actuator endpoint                | platform-core    |
| lib.actuator.service         | Library actuator endpoint             | platform-core    |
| routes.actuator.service      | Route info actuator endpoint          | platform-core    |
| env.actuator.service         | Environment actuator endpoint         | platform-core    |
| health.actuator.service      | Health actuator endpoint              | platform-core    |
| liveness.actuator.service    | Liveness actuator endpoint            | platform-core    |
| elastic.queue.cleanup        | Elastic event buffer clean up task    | platform-core    |
| distributed.tracing          | Distributed tracing logger            | platform-core    |
| system.ws.server.cleanup     | Websocket server cleanup service      | platform-core    |
| http.auth.handler            | REST automation authentication router | platform-core    |
| event.api.service            | Event API service                     | platform-core    |
| temporary.inbox              | Event listener for RPC                | platform-core    |
| event.script.manager         | Instantiate new event flow instance   | event-script     |
| task.executor                | Perform event choreography            | event-script     |
| http.flow.adapter            | Built-in flow adapter                 | event-script     |
| no.op                        | no-operation placeholder function     | event-script     |
| resilience.handler           | Resilience handler (retry / circuit breaker) for flows | event-script     |
| system.service.registry      | Distributed routing registry          | Connector        |
| system.service.query         | Distributed routing query             | Connector        |
| cloud.connector.health       | Cloud connector health service        | Connector        |
| cloud.health.inbox           | Event listerner for loopback test     | Connector        |
| cloud.manager                | Cloud manager service                 | Connector        |
| presence.service             | Presence signal service               | Connector        |
| presence.housekeeper         | Presence keep-alive service           | Connector        |
| cloud.connector              | Cloud event emitter                   | Connector        |
| init.multiplex.*             | reserved for event stream startup     | Connector        |
| completion.multiplex.*       | reserved for event stream clean up    | Connector        |
| async.http.request           | HTTP request event handler            | REST automation  |
| async.http.response          | HTTP response event handler           | REST automation  |
| cron.scheduler               | Cron job scheduler                    | Simple Scheduler |
| run.scheduled.job            | Scheduled job executor                | Simple Scheduler |
| init.service.monitor.*       | reserved for event stream startup     | Service monitor  |
| completion.service.monitor.* | reserved for event stream clean up    | Service monitor  |
| simple.kafka.notification    | Publish an event to a Kafka topic (drop-n-forget / fail-fast) | minimalist-kafka |
| sync.prepare                 | Sync-over-async facade: register the return route + serialize the request | sync-over-async  |
| sync.await                   | Sync-over-async facade: block for the asynchronous response   | sync-over-async  |

Routes from the last three rows belong to **opt-in extension modules** (`minimalist-kafka`,
`sync-over-async`): they are reserved only when that module is on the classpath. `sync.prepare` and
`sync.await` are the ready-made facade tasks an application wires into its own `sync-to-async` flow (see
[Event Script Syntax](event-script/syntax.md)); like every reserved route, do not register your own
function under these names.

## Optional user defined functions

The following optional route names will be detected by the system for additional user defined features.

| Route                        | Purpose                                                                               |
|:-----------------------------|:--------------------------------------------------------------------------------------|
| additional.info              | User application function to return information<br/> about your application status    |
| distributed.trace.forwarder  | Function to forward performance metrics to a telemetry<br/> system (ready-made OpenTelemetry/OTLP version: the<br/> `opentelemetry-forwarder` extension), or your own        |
| transaction.journal.recorder | Custom function to record transaction request-response<br/> payloads into an audit DB |

The `additional.info` function, if implemented, will be invoked from the "/info" endpoint and its response
will be merged into the "/info" response.

For `distributed.trace.forwarder` and `transaction.journal.recorder`, please refer to [Build, Test and Deploy](build-test-deploy.md)
for details.

## No-op function

The "no.op" function is used as a placeholder for building skeleton or simple decision function for
an event flow use case.

## Simple exception handler

The "simple.exception.handler" is a placeholder for a user defined exception handler for rapid prototyping.
For more sophisticated error handling, please use the "resilience.handler" or write your own composable function
as an exception handler. For more details, refer to [Event Script Syntax](event-script/syntax.md)

## Reserved event header names

The following event headers are injected by the system as READ only metadata. They are available from the
input "headers". However, they are not part of the EventEnvelope.

| Header            | Purpose                                                             |
|:------------------|:-------------------------------------------------------------------|
| my_route          | route name of your function                                        |
| my_trace_id       | trace ID, if any, for the incoming event                          |
| my_trace_path     | trace path, if any, for the incoming event                        |
| my_correlation_id | business correlation-id, if any (the flow's `model.cid`)          |

These are READ only. Do not set them as response headers - the framework injects them and filters them
out of responses. Read them through the PostOffice API (`getRoute`, `getTraceId`, `getTracePath`,
`getMyCorrelationId`) rather than the raw header map.

You can create a trackable PostOffice using the "headers" and the "instance" parameters in the input arguments
of your function.

```java
var po = new PostOffice(headers, instance);
String businessCorrelationId = po.getMyCorrelationId();   // upstream correlation-id, propagated to this function
```

## Reserved HTTP header names

| Header                   | Purpose                                                                     | 
|:-------------------------|:----------------------------------------------------------------------------|
| X-Stream-Id              | Temporal route name for streaming content                                   |
| X-TTL                    | Time to live in milliseconds for a streaming content                        |
| X-Small-Payload-As-Bytes | This header, if set to true, tells system to render stream content as bytes |
| X-Event-Api              | The system uses this header to indicate that the request is sent over HTTP  |
| X-Async                  | This header, if set to true, indicates it is a drop-n-forget request        |
| X-Trace-Id               | Propagates the trace ID (end-to-end telemetry)                              |
| X-Correlation-Id         | Business correlation-id (default header name; configurable)                 |
| traceparent              | W3C Trace Context header carrying trace ID + parent span ID (OpenTelemetry) |
| X-Content-Length         | If present, it is the expected length of a streaming content                |
| X-Raw-Xml                | This header, if set to true, tells to system to skip XML rendering          |
| X-Flow-Id                | This tells the event manager to select a flow configuration by ID           |
| X-App-Instance           | This header is used by some protected actuator REST endpoints               |

### Trace ID (X-Trace-Id and W3C traceparent)

The trace ID is for end-to-end telemetry. Two header mechanisms are supported, both accepted inbound and
emitted outbound:

- **X-Trace-Id** - carries the trace ID. When absent inbound, a fresh trace ID is generated at the edge.
  The header **name** is configurable via `http.trace.id.header` (HTTP) and `kafka.trace.id.header`
  (Kafka; unset by default - traceparent-only), for enterprises with their own convention.
- **traceparent** (W3C Trace Context) - carries the trace ID *and* the caller's span ID. On inbound it
  **takes precedence** over `X-Trace-Id`: the trace ID segment becomes the Mercury trace ID and the parent
  span ID is adopted, so the trace continues from the upstream caller (span lineage across HTTP boundaries).
  On outbound the system emits `traceparent` built from this hop's own span, alongside `X-Trace-Id`.

The framework does **not** echo the trace ID (or the correlation-id) back to the HTTP client.

> The legacy `trace.http.header` parameter (which allowed `X-Correlation-Id` to double as a trace ID) has
> been retired. Use `X-Trace-Id` / `traceparent` for the trace ID, and `X-Correlation-Id` for the
> correlation-id (below) - the two concerns are now separate.

> **Don't set `X-Trace-Id` / `traceparent` yourself in application code — let the framework handle it.** When a
> call is traced, the platform stamps both headers from the current trace context on every outbound HTTP request
> and overwrites anything you set, so the trace lineage stays correct and the upstream trace propagates
> automatically. Only when a call is **not** traced (`tracing: false`, or a call made outside a trace) does a
> value you set pass through unchanged — the intended escape hatch for propagating a trace to a third-party
> system or for unit tests.

### Correlation ID propagation

The correlation-id ties a transaction together across business domains, from upstream through the mid-tier
to downstream. Its header name is enterprise-specific, so it is **configurable**:

```properties
# HTTP: many enterprises use X-Correlation-Id (the default)
http.correlation.id.header=X-Correlation-Id
# Kafka: no cross-vendor standard exists, so "cid" is the default
kafka.correlation.id.header=cid
```

> **Per-entry overrides (impedance matching).** The keys above (and their trace-id counterparts
> `http.trace.id.header` / `kafka.trace.id.header`) are the **global defaults**. A single application often
> integrates with pre-existing or third-party systems that each use their own convention, so an individual
> rest.yaml endpoint or kafka-flow-adapter.yaml binding may override them with the optional
> `trace.id.header` and `correlation.id.header` keys - the per-entry value wins over the global one.

Propagation:

- **Captured at the edge.** REST automation (HTTP) and the Kafka Flow Adapter read the configured header;
  when absent, a fresh UUID (no dashes, via `util.getUuid()`) is generated. This is the **business
  correlation-id**, distinct from the **internal correlation-id** used to route the reply back to the
  caller (`EventEnvelope.getCorrelationId()`/`setCorrelationId()`) — the internal one is transport
  plumbing invisible to application code; the business one is the durable, end-to-end identifier.
- **Preserved in the flow** as `model.cid`, and exposed to every function task as the read-only
  `my_correlation_id` header (`PostOffice.getMyCorrelationId()`).
- **Carried to any touch point.** Every `PostOffice` send/RPC/broadcast carries the business
  correlation-id on an **engine-managed envelope tag** — never as an envelope header — so the
  correlation-id follows the call graph automatically: across in-memory calls, the cross-instance
  **event-over-HTTP** hop (the tag rides in the serialized envelope and the peer's engine injects
  `my_correlation_id` at delivery for `getMyCorrelationId()`), and into downstream systems. Metadata is
  never transported as envelope headers; the `my_*` keys exist only in the injected input-header copy.
- **Echoed on the HTTP response.** REST automation returns the request's business correlation-id
  (inbound or edge-generated) on the response under the configured header name (default
  `X-Correlation-Id`), so an edge caller can correlate without parsing the body. A response header of
  the same name set by the function takes precedence.
- **Handed downstream.** `simple.kafka.notification` stamps it on the outbound Kafka message (under
  `kafka.correlation.id.header`), and `AsyncHttpClient` emits it as the configured HTTP header
  (`http.correlation.id.header`) on downstream calls — in both cases an explicitly set header (e.g. a flow
  mapping `model.cid -> header.cid`, or a request that already carries the header) takes precedence.

## Transient data store

The system uses a temp folder in "/tmp/reactive" for event streaming.
## See also

- [Configuration Reference](configuration-reference.md) — all configuration keys.
- [Actuators & HTTP Client](actuators-and-http-client.md) — built-in endpoints and utilities.
