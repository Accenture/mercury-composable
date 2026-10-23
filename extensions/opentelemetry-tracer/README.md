# OpenTelemetry invocation tracer

Code-level tracing through the vendor-neutral **OpenTelemetry API**. Each composable-function hop becomes a
live `CONSUMER` span named after its route. Its parent is the W3C context the sender wrote into the event
envelope's tags, so a request is one connected trace across function calls, RPCs, Event Script flows and
Event-over-HTTP. A span is not bound to a thread, so a function that returns a `Mono` shows its real duration.

This module depends on the API only. Whatever implements the API in the JVM records the spans:

- **Dynatrace OneAgent** with its *OpenTelemetry (Java)* feature enabled, which nests the spans into its PurePaths;
- the **OpenTelemetry Java agent**, which exports them to any OpenTelemetry backend;
- nothing: without an implementation the API is a no-op.

This is different from `extensions/opentelemetry-forwarder`, which exports finished spans after the fact. Do not
run both into the same backend.

The design, the settings, the limits and how to check it are in the Observability guide,
section *Code-level tracing with an agent* (`docs/guides/observability.md#live-tracing`).

## Use it

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

```xml
<dependency>
    <groupId>org.platformlambda</groupId>
    <artifactId>opentelemetry-tracer</artifactId>
    <version>x.y.z</version>
</dependency>
```

```properties
# master switch - default off; the jar alone does nothing
otel.tracing=true
```

The startup log names the API implementation in use, so you can see which agent is recording.
