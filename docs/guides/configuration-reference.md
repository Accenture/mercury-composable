---
title: Configuration Reference
summary: Exhaustive listing of application.properties / application.yml configuration keys.
layer: reference
audience: [developer, reference]
keywords: [configuration, application.properties, application.yml, settings, reference]
---

# Configuration Reference

*Reference: All application.properties and application.yml configuration keys for Mercury Composable.*

> **At a glance**
>
> - **What** — every `application.properties` / `application.yml` key supported by the framework
>   and its optional modules, with type, default, and description.
> - **Why it matters** — configuration is how you tune ports, threading, security, and module
>   behavior without touching code.
> - **For** developers and operators configuring and deploying applications.

Mercury Composable applications are configured through `application.properties` (or the
equivalent `application.yml`). This page is the exhaustive reference for every configuration
key supported by the framework and its optional modules.

Properties are set in `src/main/resources/application.properties` and can be overridden via
environment variables or JVM system properties (`-Dkey=value`) using Spring Boot's standard
property resolution order. When both `application.properties` and `application.yml` are
present, `.properties` takes precedence.

> **Tip**: In `application.yml`, dots in property names become nested YAML keys.
> For example, `rest.server.port=8100` becomes:
> ```yaml
> rest:
>   server:
>     port: 8100
> ```

---

## Application Identity

### `application.name`

| Type | Default |
|------|---------|
| `String` | `application` |

Human-readable service name shown in the `/info` actuator response. Falls back to `spring.application.name` if set.

### `info.app.version`

| Type | Default |
|------|---------|
| `String` | — |

Application version string (e.g. `1.2.3`) shown in the `/info` actuator response.

### `info.app.description`

| Type | Default |
|------|---------|
| `String` | — |

Application description shown in the `/info` actuator response.

### `spring.application.name`

| Type | Default |
|------|---------|
| `String` | `application` |

Standard Spring Boot application name; used as a fallback for `application.name`.

---

## Server

### `api.origin`

| Type | Default |
|------|---------|
| `String` | `*` |

Value for the `Access-Control-Allow-Origin` CORS response header.

### `hsts.feature`

| Type | Default |
|------|---------|
| `boolean` | `true` |

Enable the `Strict-Transport-Security` response header (HSTS).

### `oversize.http.response.header`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Allow HTTP response headers up to 16 KB (default limit is 8 KB).

### `rest.server.port`

| Type | Default |
|------|---------|
| `int` | `8085` |

Listening port for the built-in reactive HTTP server.

### `rest.server.ssl-enabled`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Enable TLS on the reactive HTTP server. Requires `rest.server.ssl.cert` and `rest.server.ssl.key`.

### `rest.server.ssl.cert`

| Type | Default |
|------|---------|
| `String` (path) | — |

Path to a PEM-format X.509 certificate. Use `classpath://` or `file://` prefix. Required when `rest.server.ssl-enabled=true`.

### `rest.server.ssl.key`

| Type | Default |
|------|---------|
| `String` (path) | — |

Path to a PEM-format private key. Use `classpath://` or `file://` prefix. Required when `rest.server.ssl-enabled=true`.

### `server.port`

| Type | Default |
|------|---------|
| `int` | `8085` |

Fallback listening port used when `rest.server.port` is not set; also the Tomcat port under Spring Boot.

### `websocket.binary.size`

| Type | Default |
|------|---------|
| `int` | _(framework default)_ |

Maximum WebSocket binary message size in bytes.

### `websocket.idle.timeout`

| Type | Default |
|------|---------|
| `int` | `60` |

WebSocket idle connection timeout in minutes.

### `websocket.server.port`

| Type | Default |
|------|---------|
| `int` | _(same as `rest.server.port`)_ |

Listening port for WebSocket connections. Defaults to `rest.server.port`.

---

## REST Automation

REST Automation enables declarative HTTP endpoint mapping via a YAML configuration file.
Set `rest.automation=true` and provide a `yaml.rest.automation` path to activate it. See
[REST Automation](rest-automation/index.md) for the full endpoint configuration syntax.

### `rest.automation`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Enable the REST Automation engine.

### `spring.mvc.static-path-pattern`

| Type | Default |
|------|---------|
| `String` | `/**` |

URL pattern for serving static resources (Spring MVC).

### `spring.web.resources.static-locations`

| Type | Default |
|------|---------|
| `String` (path) | — |

Spring Boot alias for `static.html.folder`; checked as a fallback.

### `static-content.filter.exclusion`

| Type | Default |
|------|---------|
| `String` (comma-sep) | — |

URL paths excluded from the static content filter.

### `static-content.filter.path`

| Type | Default |
|------|---------|
| `String` (comma-sep) | — |

URL prefixes that the static content filter applies to.

### `static-content.filter.service`

| Type | Default |
|------|---------|
| `String` | — |

Route name of a function that pre-processes matching static content requests.

### `static-content.no-cache-pages`

| Type | Default |
|------|---------|
| `String` (comma-sep) | — |

URL paths served with `Cache-Control: no-cache`.

### `static.html.folder`

| Type | Default |
|------|---------|
| `String` (path) | `classpath:/public` |

Location of static web content (HTML, CSS, JS, images).

### `yaml.rest.automation`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `classpath:/rest.yaml` |

Location(s) of REST endpoint configuration file(s). Multiple files are merged.

---

## Event Script / Flow Engine

Flow configuration files are compiled at startup. Each file lists one or more flow definitions
identified by `flow.id`. Multiple comma-separated paths are merged. See
[Event Script Syntax](event-script/syntax.md) for the full DSL reference.

### `max.model.array.size`

| Type | Default |
|------|---------|
| `int` | `1000` |

Maximum array index size for dynamic list variables in event script data models.

### `yaml.event.over.http`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `classpath:/event-over-http.yaml` |

Location(s) of the event-over-HTTP target mapping configuration.

### `yaml.flow.automation`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `classpath:/flows.yaml` |

Location(s) of Event Script flow definition files.

### `yaml.journal`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `classpath:/journal.yaml` |

Location(s) of the journal configuration (lists routes whose messages are recorded).

### `yaml.multicast`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `classpath:/multicast.yaml` |

Location(s) of the multicast route configuration. Multicast is a **local-JVM fan-out**: a message sent to a source route is automatically relayed to all listed target routes within the same in-memory event bus. Format: `multicast: [{source: "a.route", targets: ["b.route", "c.route"]}]`. Not the same as `PostOffice.broadcast()`, which is a distributed service-mesh operation.

---

## Component Scanning & Startup

### `modules.autostart`

| Type | Default |
|------|---------|
| `String` (comma-sep or YAML list) | — |

Route names or `flow://<flow-id>` identifiers to activate at startup without an inbound request.

### `spring.component.scan`

| Type | Default |
|------|---------|
| `String` | — |

Package for Spring Boot component scanning; used alongside `web.component.scan` when running with `rest-spring-3`.

### `web.component.scan`

| Type | Default |
|------|---------|
| `String` (comma-sep packages) | — |

Java packages to scan for `@PreLoad`, `@MainApplication`, `@BeforeApplication`, and `@WebSocketService` annotations.

### `yaml.preload.override`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | — |

YAML file(s) that override `@PreLoad` annotation settings (route name, instance count) without recompiling.

---

## Threading & Performance

By default all functions run on Java 21 virtual threads. Use `@KernelThreadRunner` to pin a
function to the kernel thread pool for blocking-I/O operations that are incompatible with
virtual threads.

### `deferred.commit.log`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Defer write commits in the ElasticQueue overflow buffer. For unit-test use only — do not set in production.

### `kernel.thread.pool`

| Type | Default |
|------|---------|
| `int` | `100` |

Size of the kernel thread pool for `@KernelThreadRunner` functions. The framework enforces a minimum of 32; there is no hard maximum, but ~200 is advisory — a JVM can rarely sustain more than ~250 kernel threads.

### `worker.instances.<route>`

| Type | Default |
|------|---------|
| `int` | _(from `@PreLoad`)_ |

Override the concurrency instance count for any named route at startup. Example: `worker.instances.my.service=50`.

### `worker.instances.no.op`

| Type | Default |
|------|---------|
| `int` | `500` |

Instance count for the built-in `no.op` placeholder function.

### `worker.instances.resilience.handler`

| Type | Default |
|------|---------|
| `int` | `500` |

Instance count for the built-in `resilience.handler`.

### `worker.instances.simple.exception.handler`

| Type | Default |
|------|---------|
| `int` | `250` |

Instance count for the built-in `simple.exception.handler`.

> The `worker.instances.<route>` pattern accepts any registered route name with dots preserved:
> `worker.instances.v1.get.profile=100`.

---

## HTTP Client

### `async.http.temp`

| Type | Default |
|------|---------|
| `String` (path) | `/tmp/async-http-temp` |

Temporary folder used to buffer large async HTTP response bodies.

### `http.client.connection.timeout`

| Type | Default |
|------|---------|
| `int` (ms) | `5000` |

Connection timeout in milliseconds for the built-in async HTTP client.

### `event.over.http.format`

| Type | Default |
|------|---------|
| `String` | `standard` |

Serialization format for outbound [Event over HTTP](event-over-http.md) calls: `standard`
(the language-neutral [wire format](event-envelope-wire-format.md), interoperable with the
Rust implementation and future ports) or `compact` (the classic single-character-key
format — a fallback for peers on older versions). Inbound decoding always accepts both
formats automatically, and the `/api/event` service replies in the requester's format.
Overridable per call (or per target in `yaml.event.over.http`) with the
`x-event-format` header.

---

## Distributed Tracing & Observability {#observability}

See the [Observability guide](observability.md) for the tracing design and OpenTelemetry/OTLP export; the keys
below tune it. The `otel.*` keys are read by the `opentelemetry-forwarder` extension.

### `app.log.context`

| Type | Default |
|------|---------|
| `boolean` | `true` |

Turns the [application log context](observability.md#log-context) on or off. When on (the default), the
structured JSON appenders (`log.format=json` or `compact`) stamp a `context` block — correlation id,
trace/span ids, service name — into every log line a traced function emits, using the built-in
`default-log-context.yaml` template. Provide your own `app-log-context.yaml` on the classpath to replace
the template, or set this key to `false` to opt out.

### `log.format`

| Type | Default |
|------|---------|
| `String` | `text` |

Application log output format: `text` (human-readable), `compact`, or `json`.

### `show.application.properties`

| Type | Default |
|------|---------|
| `String` (comma-sep list) | — |

Application property keys to expose via the `/env` actuator endpoint.

### `show.env.variables`

| Type | Default |
|------|---------|
| `String` (comma-sep list) | — |

Environment variable names to expose via the `/env` actuator endpoint.

### `skip.rpc.tracing`

| Type | Default |
|------|---------|
| `String` (comma-sep list) | `async.http.request` |

Route names excluded from distributed trace recording.

### `stack.trace.transport.size`

| Type | Default |
|------|---------|
| `int` | `10` |

Maximum stack-trace lines embedded in an `EventEnvelope` when an exception occurs.

### `http.correlation.id.header`

| Type | Default |
|------|---------|
| `String` | `X-Correlation-Id` |

HTTP header carrying the business correlation-id (enterprise-specific; case-insensitive on inbound). Captured at the edge and preserved end-to-end — exposed to flows as `model.cid` and to functions via `PostOffice.getMyCorrelationId()`. A fresh dash-less UUID is generated when the header is absent. Overridable per endpoint with `correlation.id.header` in a rest.yaml entry (impedance matching for a legacy caller).

### `http.trace.id.header`

| Type | Default |
|------|---------|
| `String` | `X-Trace-Id` |

HTTP header recognized (inbound, when no W3C `traceparent` is present) and emitted (outbound by the async HTTP client) as the trace-id. A well-formed `traceparent` always takes precedence inbound. Overridable per endpoint with `trace.id.header` in a rest.yaml entry.

### `kafka.correlation.id.header`

| Type | Default |
|------|---------|
| `String` | `cid` |

Kafka message header carrying the business correlation-id (no cross-vendor standard exists for non-HTTP transport, so it is configurable). Read inbound by the Kafka Flow Adapter and written outbound by `simple.kafka.notification`; a fresh UUID is generated inbound when absent. Overridable per binding with `correlation.id.header` in a kafka-flow-adapter.yaml entry.

### `kafka.trace.id.header`

| Type | Default |
|------|---------|
| `String` | — |

Optional Kafka message header for the trace-id. Inbound: a fallback trace-id source when the upstream sends no W3C `traceparent` (which always takes precedence). Outbound: `simple.kafka.notification` stamps the current trace-id under this name alongside `traceparent`, for legacy downstream consumers. Unset = traceparent-only behavior (unchanged). Overridable per binding with `trace.id.header` in a kafka-flow-adapter.yaml entry.

### `kafka.health.timeout`

| Type | Default |
|------|---------|
| `String` (duration) | `5s` |

Timeout for the `kafka.health` probe - a single Kafka Metadata request (`KafkaConsumer.listTopics`) issued from the module's consumer template; no consumer group, no offsets, no admin privileges. Add `kafka.health` to `mandatory.health.dependencies` (or the optional list) to include the cluster in `/health`.

### `kafka.health.startup.grace`

| Type | Default |
|------|---------|
| `String` (duration) | `30s` |

Start-up grace period for `kafka.health`: within it the check reports a placeholder healthy status while the Kafka client warms up in the background, so `/health` never fails or blocks during application start-up. After the first successful probe (or once the grace expires) every check is live and an unreachable cluster fails `/health` with 503.

### `otel.trace.forwarder.enabled`

| Type | Default |
|------|---------|
| `boolean` | `true` |

Used by the `opentelemetry-forwarder` extension. `false` makes the forwarder a no-op (jar present, no export).

### `otel.exporter.otlp.endpoint`

| Type | Default |
|------|---------|
| `String` | `http://localhost:4318/v1/traces` |

OTLP/HTTP traces endpoint the `opentelemetry-forwarder` extension exports spans to.

### `otel.exporter.otlp.timeout`

| Type | Default |
|------|---------|
| `int` (ms) | `10000` |

OTLP export timeout for the `opentelemetry-forwarder` extension.

### `otel.exporter.otlp.connect.timeout`

| Type | Default |
|------|---------|
| `int` (ms) | `10000` |

TCP/TLS connect timeout for the `opentelemetry-forwarder` extension's OTLP exporter, separate from the export timeout.

### `otel.exporter.otlp.compression`

| Type | Default |
|------|---------|
| `String` | `none` |

OTLP request-body compression for the `opentelemetry-forwarder` extension: `gzip` or `none`. `gzip` cuts egress bandwidth for high trace volumes.

### `otel.service.name`

| Type | Default |
|------|---------|
| `String` | `application.name` |

`service.name` resource attribute the `opentelemetry-forwarder` extension stamps on every exported span.

### `otel.exporter.otlp.headers`

| Type | Default |
|------|---------|
| `String` (comma-sep `key=value`) | — |

Request headers for the `opentelemetry-forwarder` extension — **where backend API credentials go** (e.g. `Authorization=Api-Token …` for Dynatrace, `X-SF-Token=…` for Splunk). Source it from the environment with **no default** — `otel.exporter.otlp.headers=${OTEL_EXPORTER_OTLP_HEADERS}` — so no secret is hard-coded; unset resolves to no headers.

> The `otel.*` values support `${ENV_VAR:default}` substitution, so the forwarder is configured entirely from `application.properties` while secrets stay in the environment.

---

## Health & Actuators

### `mandatory.health.dependencies`

| Type | Default |
|------|---------|
| `String` (comma-sep list) | — |

Route names of health-check functions that must all report healthy for `/health` to return HTTP 200.

### `optional.health.dependencies`

| Type | Default |
|------|---------|
| `String` (comma-sep list) | — |

Route names of health-check functions whose failure is reported but does not fail `/health`.

### `protect.info.endpoints`

| Type | Default |
|------|---------|
| `boolean` | `false` |

When `true`, the `/info`, `/routes`, `/lib`, and `/env` endpoints require an `X-App-Instance` header.

---

## Spring Boot Integration

These properties are relevant only when using the `rest-spring-3` module.

### `spring.boot.main`

| Type | Default |
|------|---------|
| `String` | `org.platformlambda.rest.RestServer` |

Main class for the Spring Boot application. Override to use a custom entry point.

### `spring.profiles.active`

| Type | Default |
|------|---------|
| `String` (comma-sep list) | — |

Active Spring profiles; enables profile-specific config files (e.g. `application-prod.properties`).

---

## Service Mesh / Cloud Connector

The service mesh is provided by the optional `connectors` modules. Set `cloud.connector=kafka`
to enable inter-instance routing through Kafka. See [Minimalist Service Mesh](service-mesh.md)
for a setup walkthrough.

### Core Connector

#### `application.feature.route.substitution`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Enable the route substitution feature to redirect one route name to another at runtime.

#### `cloud.client.properties`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `file:/tmp/config/kafka.properties, classpath:/kafka.properties` |

Location(s) of the Kafka client properties file.

#### `cloud.connector`

| Type | Default |
|------|---------|
| `String` | `none` |

Cloud connector type. `none` disables the service mesh; `kafka` enables the Kafka connector.

#### `cloud.services`

| Type | Default |
|------|---------|
| `String` (comma-sep list) | — |

Additional cloud services to register at startup.

#### `route.substitution`

| Type | Default |
|------|---------|
| `String` (comma-sep `a:b` pairs) | — |

Inline route substitutions where `b` replaces `a`. Example: `old.route:new.route`.

#### `user.cloud.client.properties`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `file:/tmp/config/second-kafka.properties, classpath:/second-kafka.properties` |

Location(s) of a second Kafka client properties file for dual-cluster deployments.

#### `yaml.route.substitution`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | — |

YAML file(s) defining route substitution mappings.

### Kafka Topic Management

#### `app.partitions.per.topic`

| Type | Default |
|------|---------|
| `int` | `32` |

Maximum Kafka partitions per application topic.

#### `app.topic.prefix`

| Type | Default |
|------|---------|
| `String` | `multiplex` |

Prefix for auto-generated application Kafka topics.

#### `application.feature.topic.substitution`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Enable topic substitution for pre-allocated Kafka topics.

#### `closed.user.group`

| Type | Default |
|------|---------|
| `int` | `1` |

Closed user group number for this instance (must be between 1 and `max.closed.user.groups`).

#### `default.app.group.id`

| Type | Default |
|------|---------|
| `String` | `appGroup` |

Default Kafka consumer group ID for application instances.

#### `default.monitor.group.id`

| Type | Default |
|------|---------|
| `String` | `monitorGroup` |

Kafka consumer group ID for presence monitor instances.

#### `kafka.replication.factor`

| Type | Default |
|------|---------|
| `int` | `3` |

Replication factor for auto-created Kafka topics.

#### `max.closed.user.groups`

| Type | Default |
|------|---------|
| `int` | `10` |

Number of closed user groups (range 3–30).

#### `max.virtual.topics`

| Type | Default |
|------|---------|
| `int` | `288` |

Maximum number of virtual topics (= partitions × topic count).

#### `monitor.topic`

| Type | Default |
|------|---------|
| `String` | `service.monitor` |

Kafka topic name for presence monitoring signals.

#### `presence.properties`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `file:/tmp/config/presence.properties, classpath:/presence.properties` |

Kafka connection properties for the presence monitor.

#### `service.monitor`

| Type | Default |
|------|---------|
| `boolean` | `false` |

When `true`, this instance acts as a presence monitor rather than a regular service node.

#### `yaml.topic.substitution`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `file:/tmp/config/topic-substitution.yaml, classpath:/topic-substitution.yaml` |

YAML file(s) defining topic substitution mappings.

---

## Scheduler

These properties are used by the optional `mini-scheduler` module.

### `deferred.start`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Defer scheduler startup until triggered manually, e.g. to wait for leader election.

### `leader.election`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Enable leader election so that only one instance in a cluster runs each scheduled job.

### `yaml.cron`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `file:/tmp/config/cron.yaml, classpath:/cron.yaml` |

Location(s) of the cron job definition file(s). Multiple files are merged.

---

## PostgreSQL (`reactive-postgres` extension)

These properties configure the optional `reactive-postgres` extension. `postgres.host`,
`postgres.port`, `postgres.user`, and `postgres.password` are **required** when the extension
is on the classpath.

### `logging.level.io.r2dbc.postgresql.PARAM`

| Type | Default |
|------|---------|
| `String` | — |

R2DBC parameter logging level (e.g. `DEBUG`). **Never set in production** — exposes parameter values and may leak PII.

### `logging.level.io.r2dbc.postgresql.QUERY`

| Type | Default |
|------|---------|
| `String` | — |

R2DBC query logging level (e.g. `DEBUG`). **Never set in production** — exposes SQL queries.

### `postgres.connection.pool`

| Type | Default |
|------|---------|
| `int` | `20` |

R2DBC connection pool size (minimum 5).

### `postgres.database`

| Type | Default |
|------|---------|
| `String` | `postgres` |

PostgreSQL database name.

### `postgres.host`

| Type | Default |
|------|---------|
| `String` | — |

PostgreSQL server hostname or IP. **Required.**

### `postgres.password`

| Type | Default |
|------|---------|
| `String` | — |

PostgreSQL password. **Required.** Supports `${ENV_VAR}` substitution.

### `postgres.port`

| Type | Default |
|------|---------|
| `int` | — |

PostgreSQL server port. **Required.**

### `postgres.repository.scan`

| Type | Default |
|------|---------|
| `String` | — |

Java package(s) to scan for Spring Data R2DBC repositories. **Required.**

### `postgres.ssl`

| Type | Default |
|------|---------|
| `boolean` | `true` |

Enable SSL/TLS for the PostgreSQL connection.

### `postgres.user`

| Type | Default |
|------|---------|
| `String` | — |

PostgreSQL username. **Required.** Supports `${ENV_VAR}` substitution.

---

## Minimalist Kafka library {#kafka-flow-adapter}

The opt-in `minimalist-kafka` library routes Kafka topics into Event Script flows and publishes events to
Kafka. See the [Kafka Flow Adapter guide](minimalist-kafka.md). The inbound adapter starts only when
`yaml.kafka.flow.adapter` is set; the outbound `simple.kafka.notification` function registers automatically.

### `yaml.kafka.flow.adapter`

| Type | Default |
|------|---------|
| `String` (path) | — |

Location of the `kafka-flow-adapter.yaml` binding file (`topic -> flow`). Unset = inbound adapter disabled.

### `kafka.producer.properties`

| Type | Default |
|------|---------|
| `String` (path, or comma-sep fallback list) | `classpath:/kafka-producer.properties` |

Producer client config template location. Externalization of configuration is opt-in: point this at a file rendered by the devops pipeline (e.g. `file:/tmp/config/kafka-producer.properties`), optionally with a classpath fallback.

### `kafka.consumer.properties`

| Type | Default |
|------|---------|
| `String` (path, or comma-sep fallback list) | `classpath:/kafka-consumer.properties` |

Consumer client config template location. Externalize the same way as the producer template.

### `kafka.dlq.timeout.ms`

| Type | Default |
|------|---------|
| `long` (ms) | `10000` |

Confirm-write timeout for the dead-letter publish (broker ack). Flow processing has no timeout knob — the flow's own `ttl` is the deadline (Kafka is asynchronous).

### `kafka.flow.max.retries`

| Type | Default |
|------|---------|
| `int` | `3` |

Retry attempts on a flow-processing failure before dead-lettering.

### `kafka.flow.retry.backoff.ms`

| Type | Default |
|------|---------|
| `long` (ms) | `500` |

Pause between retry attempts.

### `schema.registry.url`

| Type | Default |
|------|---------|
| `String` (URL) | — |

Confluent Schema Registry URL. Unset = schema features off (raw `byte[]`); set to enable the [Schema Registry integration](minimalist-kafka.md#schema) (JSON Schema / Avro — Protobuf is [not currently supported](minimalist-kafka.md#schema)).

### `schema.registry.properties`

| Type | Default |
|------|---------|
| `String` (comma-sep paths) | `classpath:/schema-registry.properties` |

Registry client template location; externalize the same way as the producer template. Entries pass verbatim to the Confluent client — [authentication](minimalist-kafka.md#schema-auth) (`bearer.auth.*` OAuth 2.0, basic auth) and SSL. OAuth token endpoint URLs found in a template are auto-registered on the JVM `org.apache.kafka.sasl.oauthbearer.allowed.urls` allow-list.

### `schema.registry.cache.ttl`

| Type | Default |
|------|---------|
| duration | `30m` |

Time-to-live for an entry in the in-memory (platform `ManagedCache`) cache of schemas fetched by id. Positive results only — a not-found id is never cached, so a newly-registered schema is visible immediately. The TTL bounds how long a cached schema is reused before re-fetching; `30m` lets schema changes be picked up without a pod restart (lengthen it in production where schemas change rarely). Cleared at startup (rebuildable).

### `yaml.secondary.kafka.flow.adapter`

| Type | Default |
|------|---------|
| `String` (location) | — |

[twin-kafka](twin-kafka.md): secondary-cluster adapter config location; unset = secondary inbound adapter off.

### `secondary.kafka.producer.properties` / `secondary.kafka.consumer.properties`

| Type | Default |
|------|---------|
| `String` (path, or comma-sep fallback list) | `classpath:/secondary-kafka-*.properties` |

[twin-kafka](twin-kafka.md): secondary-cluster client template location; same mechanics as the primary templates.

### `secondary.schema.registry.url` / `secondary.schema.registry.properties`

| Type | Default |
|------|---------|
| `String` | — |

[twin-kafka](twin-kafka.md): the secondary cluster's own optional Schema Registry (URL = feature switch; template passed verbatim to the Confluent client). Registries and their schema-id caches are per-cluster.

### `secondary.kafka.health.timeout` / `secondary.kafka.health.startup.grace`

| Type | Default |
|------|---------|
| `String` (duration) | fall back to the `kafka.health.*` globals |

[twin-kafka](twin-kafka.md#health): tunables for `secondary.kafka.health`, the secondary cluster's twin of `kafka.health`. A bridge lists both: `mandatory.health.dependencies=kafka.health, secondary.kafka.health`.

### `secondary.kafka.correlation.id.header` / `secondary.kafka.trace.id.header`

| Type | Default |
|------|---------|
| `String` | fall back to the `kafka.*` globals |

[twin-kafka](twin-kafka.md): outbound header names on the secondary cluster when the two clusters follow different conventions.

The Kafka **connection and security** settings (`bootstrap.servers`, `security.protocol`, `sasl.*`, `ssl.*`,
`acks`, `auto.offset.reset`) live in the `kafka-producer.properties` / `kafka-consumer.properties` template
files — not as `application.properties` keys — so any enterprise installation is configured by editing or
overriding those templates. All template values support `${ENV_VAR:default}` substitution; `bootstrap.servers`
defaults to `${KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}`. The library pins the (de)serializers; the consumer's
`enable.auto.commit` / `max.poll.records` are a per-binding overlay (see `auto-commit` below), not a global
template setting.

Per-binding fields in `kafka-flow-adapter.yaml` (all `${ENV_VAR:default}`-substitutable): exactly one of
`topic` (literal) or `topic-pattern` (regex, subscribed via `subscribe(Pattern)`) is required; `flow`
(required); `group` (optional consumer group, used verbatim; default `kafka-flow-adapter.<topic>` for a
literal topic, required for `topic-pattern`); `partition` (optional; pins one partition via manual
assignment, mutually exclusive with `topic-pattern`); `schema.enabled` (optional `boolean`, default `false`;
when `true`, decode the Confluent-framed value to a `Map` before routing — see the
[Schema Registry integration](minimalist-kafka.md#schema)); `dlq-topic` (optional; pre-provisioned
dead-letter topic for this binding, used verbatim — no DLQ if omitted); `auto-commit` (optional `boolean`,
default `false`; `true` uses Kafka-native auto-commit instead of manual commit-after-process); and
`max-poll-records` (optional `int`; overrides the delivery mode's default of `1` for manual-commit or `500`
for auto-commit). See the [Kafka Flow Adapter guide](minimalist-kafka.md#adapter-yaml) for the full
per-field rationale and validation rules.

---

## Sync-over-Async (`sync-over-async` extension) {#sync-over-async}

The opt-in `sync-over-async` extension exposes a synchronous REST request/response over an asynchronous,
cross-pod Kafka backend using a Redis return route. See the [Sync-over-Async guide](sync-over-async.md). It is
off by default and starts (eagerly connecting to Redis) only when `sync.over.async.enabled=true`.

### `sync.over.async.enabled`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Master switch. `true` starts the Redis return-route coordinator at boot.

### `redis.host`

| Type | Default |
|------|---------|
| `String` | `127.0.0.1` |

Redis host.

### `redis.port`

| Type | Default |
|------|---------|
| `int` | `6379` |

Redis port.

### `redis.password`

| Type | Default |
|------|---------|
| `String` | — (blank) |

Auth password; blank = no auth. Source from the environment (`${REDIS_PASSWORD}`).

### `redis.ssl`

| Type | Default |
|------|---------|
| `boolean` | `false` |

Use TLS (`rediss://`).

### `redis.database`

| Type | Default |
|------|---------|
| `int` | `0` |

Logical Redis database index.

### `redis.timeout.ms`

| Type | Default |
|------|---------|
| `long` (ms) | `5000` |

Default Redis command timeout.

### `sync.return.channel.prefix`

| Type | Default |
|------|---------|
| `String` | `svc-return` |

Prefix for the per-pod Pub/Sub return channel.

### `sync.route.ttl.seconds`

| Type | Default |
|------|---------|
| `long` (s) | `90` |

TTL for the return-route key; should cover the REST timeout plus a buffer.

### `sync.response.ttl.seconds`

| Type | Default |
|------|---------|
| `long` (s) | `30` |

TTL for the response key (short rendezvous window).

### `sync.max.pending.requests`

| Type | Default |
|------|---------|
| `int` | `10000` |

Per-pod ceiling on in-flight synchronous requests (backpressure).

All `redis.*` and `sync.*` values support `${ENV_VAR:default}` substitution.

---

## Serialization & Content Types

### `custom.content.types`

| Type | Default |
|------|---------|
| `String` (comma-sep list) | — |

Content-type resolver rules in `vendor-type -> canonical-type` format. Example: `application/vnd.acme+json -> application/json`.

### `mime.types`

| Type | Default |
|------|---------|
| Map (YAML only) | — |

Map of file extension to MIME type. Example: `mime.types.svg: image/svg+xml`.

### `snake.case.serialization`

| Type | Default |
|------|---------|
| `boolean` | `false` |

When `true`, JSON output uses `snake_case` field names instead of `camelCase`.

---

## Data Storage

### `running.in.cloud`

| Type | Default |
|------|---------|
| `boolean` | `false` |

When `false`, a per-instance subdirectory is created under `transient.data.store`. When `true`, the path is used as-is (for ephemeral containers).

### `transient.data.store`

| Type | Default |
|------|---------|
| `String` (path) | `/tmp/reactive` |

Base directory for the ElasticQueue overflow buffer that absorbs event bursts when consumers are slower than producers.

---

## Complete Example

A typical `src/main/resources/application.properties` for a REST-serving composable
application. This covers the properties most developers will need to configure.

```properties
#
# Mercury Composable — Example application.properties
#

# --- Application Identity ---
application.name=my-service
info.app.version=1.0.0
info.app.description=My composable microservice

# --- Server ---
rest.server.port=8100

# --- REST Automation ---
rest.automation=true
yaml.rest.automation=classpath:/rest.yaml
static.html.folder=classpath:/public

# --- Event Script / Flows ---
yaml.flow.automation=classpath:/flows.yaml

# --- Component Scanning ---
web.component.scan=com.example.myapp

# --- Threading ---
# Increase only if many @KernelThreadRunner functions are used.
kernel.thread.pool=100

# --- HTTP Client ---
http.client.connection.timeout=5000

# --- Distributed Tracing ---
# The trace ID travels as the "X-Trace-Id" header and the W3C "traceparent" header (traceId + spanId).
# Both are emitted outbound and accepted inbound; "traceparent" takes precedence on inbound. No config.

# --- Correlation ID ---
# Business correlation-id headers (configurable; defaults shown). Captured at the edge, preserved as
# model.cid, and propagated downstream. A fresh UUID is generated when absent.
http.correlation.id.header=X-Correlation-Id
kafka.correlation.id.header=cid

# --- Health ---
# List health-check routes that must all pass for /health to return 200.
# Add cloud.connector.health when cloud.connector != none.
# mandatory.health.dependencies=cloud.connector.health

# --- Actuators ---
protect.info.endpoints=false

# --- Serialization ---
snake.case.serialization=false

# --- Logging ---
log.format=text

# --- Cloud Connector (uncomment for service mesh) ---
# cloud.connector=kafka
# cloud.client.properties=file:/tmp/config/kafka.properties,classpath:/kafka.properties
```

---

## See Also

- [REST Automation](rest-automation/index.md) — declarative REST endpoint configuration syntax
- [Event Script Syntax](event-script/syntax.md) — flow YAML configuration syntax
- [Actuators & HTTP Client](actuators-and-http-client.md) — built-in actuator endpoint reference
