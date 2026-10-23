# Dynatrace OneAgent invocation observer

Code-level tracing for Dynatrace OneAgent. Mercury hands an event from one composable function to the next
through its event bus and a per-route queue, so OneAgent's own instrumentation loses the request at every hop.
This extension brackets each hop with the **OneAgent SDK**: a send is an outgoing message whose Dynatrace tag
rides on the event envelope, and the receiving function is an incoming message that consumes it. A request
then shows as one connected PurePath, across function calls, RPCs, Event Script flows and Event-over-HTTP.

The design, the settings, the limits and how to check it are in the Observability guide,
section *Code-level tracing with an agent* (`docs/guides/observability.md#live-tracing`).

## Use it

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

```xml
<dependency>
    <groupId>org.platformlambda</groupId>
    <artifactId>dynatrace-oneagent</artifactId>
    <version>x.y.z</version>
</dependency>
```

```properties
# master switch - default off; the jar alone does nothing
dynatrace.oneagent.enabled=true
# optional: name every hop after its route instead of the application (more Dynatrace services)
dynatrace.oneagent.destination=application
# optional: write OneAgent's trace and span ids into the log context (not with OneAgent's own log enrichment)
dynatrace.oneagent.log.context=false
```

Run the application with OneAgent attached (1.311 or later, with its Java virtual-thread support enabled). The
startup log reports the SDK state. Without an attached agent the SDK is permanently inactive and nothing is
registered. The SDK's own warnings, such as a tracer used on the wrong thread, appear in the application log
with the prefix `OneAgent SDK:`.

The dependency is `com.dynatrace.oneagent.sdk.java:oneagent-sdk` 1.9.0 (Apache License 2.0, no dependencies
of its own).
