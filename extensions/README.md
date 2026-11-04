# This folder contains optional extensions

## Reactive PostGreSQL (R2DBC) library

This optional reactive database library is a reactive version of the PostGreSQL database client
using Spring R2DBC.

It supports four ways to access PostGreSQL:

1. Reactive Repository pattern
2. Database client API
3. PostOffice RPC calls to the PgService using route "postgres.service"
4. PgRequest query and update methods that are wrappers of method-3 above

To use this library, please build it from source using `mvn clean install` or publish it
to your organization artifactory.

You may then use the pg-example as a template to use the reactive PostGreSQL library.

## OpenTelemetry trace forwarder

A drop-in `distributed.trace.forwarder` that maps Mercury's W3C-propagated trace metrics to
OpenTelemetry spans and exports them over OTLP/HTTP to a collector or backend such as Dynatrace,
Splunk, Jaeger or Tempo. Adding the jar to the classpath auto-registers the route (no code), and the
collector endpoint, service name and credentials are configured in `application.properties` with
`${ENV_VAR:default}` substitution.

See the [Observability guide](https://accenture.github.io/mercury-composable/guides/observability/) and
the module's own `README.md` for the full configuration and the metric-to-span mapping.

## Sync-over-async (Redis-assisted Kafka request/reply)

Exposes synchronous REST semantics over asynchronous Kafka processing across horizontally scaled pods.
The pod that consumes the Kafka response is usually not the pod holding the original HTTP connection, so
Redis carries the cross-pod return route: the response payload is stored in Redis (`SETEX`) and a per-pod
Pub/Sub channel wakes the originating pod, keyed by a correlation-id. Kafka remains the durable business
transport; Redis Pub/Sub is only a low-latency wake-up signal.

The Kafka transport legs (the `simple.kafka.notification` function and the Kafka Flow Adapter that routes
each topic into an Event Script flow) are provided by the reusable `system/minimalist-kafka` library, which
this module depends on; sync-over-async itself only adds the Redis cross-pod return-route engine on top.

This module is under active development (prototype). See the module's own `README.md` for the threading
model and the virtual-thread-safety evidence for the blocking Lettuce sync API.

## Agent loop (`llm.run`)

The agent loop of RFC-0011: a graph node calls `llm.run` with a tool catalog fixed on the graph, and the loop runs
model turns through a language pack's `llm.chat` and calls a composable-function route for each tool the model asks
for. Approval modes (`auto`, `manual`, `unattended`) hold a high-risk write for a person, and the graph suspends and
resumes around it. Adding the jar registers the route; the module links no provider SDK, no MCP client and no store
client, and the transcript stays in the pack's store. See the module's own `README.md`.

## API playground

The API playground is an example application to deploy multiple OpenAPI 3.0 yaml files to test
various REST endpoints.
