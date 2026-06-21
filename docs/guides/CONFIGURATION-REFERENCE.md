# Configuration Reference

*Reference: All application.properties and application.yml configuration keys for Mercury Composable.*

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

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `application.name` | `String` | `application` | Human-readable service name shown in the `/info` actuator response. Falls back to `spring.application.name` if set. |
| `info.app.version` | `String` | — | Application version string (e.g. `1.2.3`) shown in the `/info` actuator response. |
| `info.app.description` | `String` | — | Application description shown in the `/info` actuator response. |
| `spring.application.name` | `String` | `application` | Standard Spring Boot application name; used as a fallback for `application.name`. |

---

## Server

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `api.origin` | `String` | `*` | Value for the `Access-Control-Allow-Origin` CORS response header. |
| `hsts.feature` | `boolean` | `true` | Enable the `Strict-Transport-Security` response header (HSTS). |
| `oversize.http.response.header` | `boolean` | `false` | Allow HTTP response headers up to 16 KB (default limit is 8 KB). |
| `rest.server.port` | `int` | `8085` | Listening port for the built-in reactive HTTP server. |
| `rest.server.ssl-enabled` | `boolean` | `false` | Enable TLS on the reactive HTTP server. Requires `rest.server.ssl.cert` and `rest.server.ssl.key`. |
| `rest.server.ssl.cert` | `String` (path) | — | Path to a PEM-format X.509 certificate. Use `classpath://` or `file://` prefix. Required when `rest.server.ssl-enabled=true`. |
| `rest.server.ssl.key` | `String` (path) | — | Path to a PEM-format private key. Use `classpath://` or `file://` prefix. Required when `rest.server.ssl-enabled=true`. |
| `server.port` | `int` | `8085` | Fallback listening port used when `rest.server.port` is not set; also the Tomcat port under Spring Boot. |
| `websocket.binary.size` | `int` | _(framework default)_ | Maximum WebSocket binary message size in bytes. |
| `websocket.idle.timeout` | `int` | `60` | WebSocket idle connection timeout in minutes. |
| `websocket.server.port` | `int` | _(same as `rest.server.port`)_ | Listening port for WebSocket connections. Defaults to `rest.server.port`. |

---

## REST Automation

REST Automation enables declarative HTTP endpoint mapping via a YAML configuration file.
Set `rest.automation=true` and provide a `yaml.rest.automation` path to activate it. See
[REST Automation](CHAPTER-3.md) for the full endpoint configuration syntax.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `rest.automation` | `boolean` | `false` | Enable the REST Automation engine. |
| `spring.mvc.static-path-pattern` | `String` | `/**` | URL pattern for serving static resources (Spring MVC). |
| `spring.web.resources.static-locations` | `String` (path) | — | Spring Boot alias for `static.html.folder`; checked as a fallback. |
| `static-content.filter.exclusion` | `String` (comma-sep) | — | URL paths excluded from the static content filter. |
| `static-content.filter.path` | `String` (comma-sep) | — | URL prefixes that the static content filter applies to. |
| `static-content.filter.service` | `String` | — | Route name of a function that pre-processes matching static content requests. |
| `static-content.no-cache-pages` | `String` (comma-sep) | — | URL paths served with `Cache-Control: no-cache`. |
| `static.html.folder` | `String` (path) | `classpath:/public` | Location of static web content (HTML, CSS, JS, images). |
| `yaml.rest.automation` | `String` (comma-sep paths) | `classpath:/rest.yaml` | Location(s) of REST endpoint configuration file(s). Multiple files are merged. |

---

## Event Script / Flow Engine

Flow configuration files are compiled at startup. Each file lists one or more flow definitions
identified by `flow.id`. Multiple comma-separated paths are merged. See
[Event Script Syntax](CHAPTER-4.md) for the full DSL reference.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `max.model.array.size` | `int` | `1000` | Maximum array index size for dynamic list variables in event script data models. |
| `yaml.event.over.http` | `String` (comma-sep paths) | `classpath:/event-over-http.yaml` | Location(s) of the event-over-HTTP target mapping configuration. |
| `yaml.flow.automation` | `String` (comma-sep paths) | `classpath:/flows.yaml` | Location(s) of Event Script flow definition files. |
| `yaml.journal` | `String` (comma-sep paths) | `classpath:/journal.yaml` | Location(s) of the journal configuration (lists routes whose messages are recorded). |
| `yaml.multicast` | `String` (comma-sep paths) | `classpath:/multicast.yaml` | Location(s) of the multicast route configuration. |

---

## Component Scanning & Startup

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `modules.autostart` | `String` (comma-sep or YAML list) | — | Route names or `flow://<flow-id>` identifiers to activate at startup without an inbound request. |
| `spring.component.scan` | `String` | — | Package for Spring Boot component scanning; used alongside `web.component.scan` when running with `rest-spring-3`. |
| `web.component.scan` | `String` (comma-sep packages) | — | Java packages to scan for `@PreLoad`, `@MainApplication`, `@BeforeApplication`, and `@WebSocketService` annotations. |
| `yaml.preload.override` | `String` (comma-sep paths) | — | YAML file(s) that override `@PreLoad` annotation settings (route name, instance count) without recompiling. |

---

## Threading & Performance

By default all functions run on Java 21 virtual threads. Use `@KernelThreadRunner` to pin a
function to the kernel thread pool for blocking-I/O operations that are incompatible with
virtual threads.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `deferred.commit.log` | `boolean` | `false` | Defer write commits in the ElasticQueue overflow buffer. For unit-test use only — do not set in production. |
| `kernel.thread.pool` | `int` | `100` | Size of the kernel thread pool for `@KernelThreadRunner` functions. Maximum 200. |
| `worker.instances.<route>` | `int` | _(from `@PreLoad`)_ | Override the concurrency instance count for any named route at startup. Example: `worker.instances.my.service=50`. |
| `worker.instances.no.op` | `int` | `500` | Instance count for the built-in `no.op` placeholder function. |
| `worker.instances.resilience.handler` | `int` | `500` | Instance count for the built-in `resilience.handler`. |
| `worker.instances.simple.exception.handler` | `int` | `250` | Instance count for the built-in `simple.exception.handler`. |

> The `worker.instances.<route>` pattern accepts any registered route name with dots preserved:
> `worker.instances.v1.get.profile=100`.

---

## HTTP Client

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `async.http.temp` | `String` (path) | `/tmp/async-http-temp` | Temporary folder used to buffer large async HTTP response bodies. |
| `http.client.connection.timeout` | `int` (ms) | `5000` | Connection timeout in milliseconds for the built-in async HTTP client. |

---

## Distributed Tracing & Observability

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `distributed.trace.processor` | `String` | — | Route name of a user function that receives all distributed trace entries for forwarding to a telemetry backend. |
| `show.application.properties` | `String` (comma-sep list) | — | Application property keys to expose via the `/env` actuator endpoint. |
| `show.env.variables` | `String` (comma-sep list) | — | Environment variable names to expose via the `/env` actuator endpoint. |
| `skip.rpc.tracing` | `String` (comma-sep list) | `async.http.request` | Route names excluded from distributed trace recording. |
| `stack.trace.transport.size` | `int` | `10` | Maximum stack-trace lines embedded in an `EventEnvelope` when an exception occurs. |
| `trace.http.header` | `String` (comma-sep list) | `X-Trace-Id` | HTTP request header(s) carrying a trace ID. The first entry is also used as the outbound trace header. |

---

## Health & Actuators

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `mandatory.health.dependencies` | `String` (comma-sep list) | — | Route names of health-check functions that must all report healthy for `/health` to return HTTP 200. |
| `optional.health.dependencies` | `String` (comma-sep list) | — | Route names of health-check functions whose failure is reported but does not fail `/health`. |
| `protect.info.endpoints` | `boolean` | `false` | When `true`, the `/info`, `/routes`, `/lib`, and `/env` endpoints require an `X-App-Instance` header. |

---

## Spring Boot Integration

These properties are relevant only when using the `rest-spring-3` module.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `spring.boot.main` | `String` | `org.platformlambda.rest.RestServer` | Main class for the Spring Boot application. Override to use a custom entry point. |
| `spring.profiles.active` | `String` (comma-sep list) | — | Active Spring profiles; enables profile-specific config files (e.g. `application-prod.properties`). |

---

## Service Mesh / Cloud Connector

The service mesh is provided by the optional `connectors` modules. Set `cloud.connector=kafka`
to enable inter-instance routing through Kafka. See [Minimalist Service Mesh](CHAPTER-8.md)
for a setup walkthrough.

### Core Connector

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `application.feature.route.substitution` | `boolean` | `false` | Enable the route substitution feature to redirect one route name to another at runtime. |
| `cloud.client.properties` | `String` (comma-sep paths) | `file:/tmp/config/kafka.properties, classpath:/kafka.properties` | Location(s) of the Kafka client properties file. |
| `cloud.connector` | `String` | `none` | Cloud connector type. `none` disables the service mesh; `kafka` enables the Kafka connector. |
| `cloud.services` | `String` (comma-sep list) | — | Additional cloud services to register at startup. |
| `route.substitution` | `String` (comma-sep `a:b` pairs) | — | Inline route substitutions where `b` replaces `a`. Example: `old.route:new.route`. |
| `user.cloud.client.properties` | `String` (comma-sep paths) | `file:/tmp/config/second-kafka.properties, classpath:/second-kafka.properties` | Location(s) of a second Kafka client properties file for dual-cluster deployments. |
| `yaml.route.substitution` | `String` (comma-sep paths) | — | YAML file(s) defining route substitution mappings. |

### Kafka Topic Management

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `app.partitions.per.topic` | `int` | `32` | Maximum Kafka partitions per application topic. |
| `app.topic.prefix` | `String` | `multiplex` | Prefix for auto-generated application Kafka topics. |
| `application.feature.topic.substitution` | `boolean` | `false` | Enable topic substitution for pre-allocated Kafka topics. |
| `closed.user.group` | `int` | `1` | Closed user group number for this instance (must be between 1 and `max.closed.user.groups`). |
| `default.app.group.id` | `String` | `appGroup` | Default Kafka consumer group ID for application instances. |
| `default.monitor.group.id` | `String` | `monitorGroup` | Kafka consumer group ID for presence monitor instances. |
| `kafka.replication.factor` | `int` | `3` | Replication factor for auto-created Kafka topics. |
| `max.closed.user.groups` | `int` | `10` | Number of closed user groups (range 3–30). |
| `max.virtual.topics` | `int` | `288` | Maximum number of virtual topics (= partitions × topic count). |
| `monitor.topic` | `String` | `service.monitor` | Kafka topic name for presence monitoring signals. |
| `presence.properties` | `String` (comma-sep paths) | `file:/tmp/config/presence.properties, classpath:/presence.properties` | Kafka connection properties for the presence monitor. |
| `service.monitor` | `boolean` | `false` | When `true`, this instance acts as a presence monitor rather than a regular service node. |
| `yaml.topic.substitution` | `String` (comma-sep paths) | `file:/tmp/config/topic-substitution.yaml, classpath:/topic-substitution.yaml` | YAML file(s) defining topic substitution mappings. |

---

## Scheduler

These properties are used by the optional `mini-scheduler` module.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `deferred.start` | `boolean` | `false` | Defer scheduler startup until triggered manually, e.g. to wait for leader election. |
| `leader.election` | `boolean` | `false` | Enable leader election so that only one instance in a cluster runs each scheduled job. |
| `yaml.cron` | `String` (comma-sep paths) | `classpath:/cron.yaml` | Location(s) of the cron job definition file(s). Multiple files are merged. |

---

## PostgreSQL (`reactive-postgres` extension)

These properties configure the optional `reactive-postgres` extension. `postgres.host`,
`postgres.port`, `postgres.user`, and `postgres.password` are **required** when the extension
is on the classpath.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `logging.level.io.r2dbc.postgresql.PARAM` | `String` | — | R2DBC parameter logging level (e.g. `DEBUG`). **Never set in production** — exposes parameter values and may leak PII. |
| `logging.level.io.r2dbc.postgresql.QUERY` | `String` | — | R2DBC query logging level (e.g. `DEBUG`). **Never set in production** — exposes SQL queries. |
| `postgres.connection.pool` | `int` | `20` | R2DBC connection pool size (minimum 5). |
| `postgres.database` | `String` | `postgres` | PostgreSQL database name. |
| `postgres.host` | `String` | — | PostgreSQL server hostname or IP. **Required.** |
| `postgres.password` | `String` | — | PostgreSQL password. **Required.** Supports `${ENV_VAR}` substitution. |
| `postgres.port` | `int` | — | PostgreSQL server port. **Required.** |
| `postgres.repository.scan` | `String` | — | Java package(s) to scan for Spring Data R2DBC repositories. **Required.** |
| `postgres.ssl` | `boolean` | `true` | Enable SSL/TLS for the PostgreSQL connection. |
| `postgres.user` | `String` | — | PostgreSQL username. **Required.** Supports `${ENV_VAR}` substitution. |

---

## Serialization & Content Types

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `custom.content.types` | `String` (comma-sep list) | — | Content-type resolver rules in `vendor-type -> canonical-type` format. Example: `application/vnd.acme+json -> application/json`. |
| `mime.types` | Map (YAML only) | — | Map of file extension to MIME type. Example: `mime.types.svg: image/svg+xml`. |
| `snake.case.serialization` | `boolean` | `false` | When `true`, JSON output uses `snake_case` field names instead of `camelCase`. |

---

## Data Storage

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `running.in.cloud` | `boolean` | `false` | When `false`, a per-instance subdirectory is created under `transient.data.store`. When `true`, the path is used as-is (for ephemeral containers). |
| `transient.data.store` | `String` (path) | `/tmp/reactive` | Base directory for the ElasticQueue overflow buffer that absorbs event bursts when consumers are slower than producers. |

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
# List headers in preference order; first entry is also used as the outbound header.
trace.http.header=X-Trace-Id

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

- [REST Automation](CHAPTER-3.md) — declarative REST endpoint configuration syntax
- [Event Script Syntax](CHAPTER-4.md) — flow YAML configuration syntax
- [Actuators & HTTP Client](APPENDIX-III.md) — built-in actuator endpoint reference
- [Application Properties (Appendix I)](APPENDIX-I.md) — original property reference
