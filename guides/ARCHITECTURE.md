# Architecture Overview

Mercury Composable is a Java 21 framework for building event-driven applications from self-contained,
stateless functions wired together by YAML-configured event choreography. It targets microservices,
serverless deployments, and any system where maintainability and horizontal scalability are priorities.
Because each function is a pure input-process-output unit with an explicit interface contract, the
framework is also well suited for AI-assisted code generation — an AI agent can generate correct
functions, flow configurations, and REST definitions from this page alone.

---

## System Architecture

A Mercury Composable application is structured in five distinct layers that process a request from
outside in.

The outermost layer is the **flow adapter**. Adapters convert external requests into internal events.
The built-in HTTP flow adapter intercepts HTTP requests routed through the REST automation engine and
packages them as `EventEnvelope` objects destined for the flow engine, then delivers the final response
back to the caller. The Kafka adapter provides the same contract for stream-based event sources. The
adapter API is open, so custom adapters can consume any event source (serverless triggers, file watchers,
MQ listeners). Each adapter exposes a named route — the HTTP adapter uses the route `http.flow.adapter`.

**REST Automation** sits at the HTTP boundary and eliminates controller boilerplate. HTTP endpoints are
declared in a `rest.yaml` configuration file. Each entry maps a URL pattern and HTTP method set to
either a flow (via the `flow` key, routing through the HTTP flow adapter) or directly to a named function
(via `service`). REST automation handles CORS headers, per-endpoint authentication functions, request and
response header rules, distributed tracing activation, and timeout enforcement — all in configuration.

The **Event Manager** (also called the flow engine) is the core orchestrator. When an event arrives from
an adapter, the event manager resolves the matching flow configuration by its ID, creates a transient
in-memory **state machine** for that transaction, and begins executing the task sequence. For each task,
the event manager performs the input data mapping (populating the function's argument scope from the
state machine and request dataset), dispatches the event to the target function, collects the result,
and applies the output data mapping (writing result values back to the state machine or flow output).
Exception handling — per-task or per-flow — is also managed by the event manager.

The **in-memory event system** is the transport backbone, built on Eclipse Vertx's event bus with
Java 21 virtual thread management. All inter-function communication within a single JVM travels through
this bus. Point-to-point delivery routes an event to exactly one worker instance of the target function.
Broadcast delivery sends an event to all registered instances. The event system uses `/tmp` as an
overflow buffer when a consumer is slower than the producer, removing the need for explicit back-pressure
handling in user code.

**Composable functions** are the innermost layer and the only place where application business logic
lives. A function knows nothing about HTTP, the flow configuration, or other user functions. It receives
typed input, executes its logic, and returns typed output. Its only permitted external dependency is a
platform or infrastructure component consumed through the event system — never a direct method call to
another user function.

---

## Composable Functions

A composable function is a Java class implementing `TypedLambdaFunction<I, O>` or the untyped
`LambdaFunction`. The typed interface defines the exact contract:

```java
public interface TypedLambdaFunction<I, O> {
    O handleEvent(Map<String, String> headers, I input, int instance) throws Exception;
}
```

`LambdaFunction` is equivalent to `TypedLambdaFunction<Object, Object>` and is used when input type
cannot be determined statically. Prefer `TypedLambdaFunction` with a concrete PoJo or
`Map<String, Object>` as the input type for all new functions.

The `@PreLoad` annotation registers the function with the event system at startup:

```java
@PreLoad(route = "v1.get.profile", instances = 50, isPrivate = false)
public class GetProfile implements TypedLambdaFunction<Map<String, Object>, Profile> {
    @Override
    public Profile handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String profileId = headers.get("profile_id");
        // business logic — return result or throw AppException(statusCode, message)
        return profile;
    }
}
```

The `route` is a lowercase, dot-separated identifier (e.g., `v1.get.profile`). At least one dot is
required. The `instances` value sets the maximum number of concurrent workers for this function.
`isPrivate = false` makes the function addressable by other application instances through the service
mesh; the default is `true` (local only). Optional `@PreLoad` parameters include `envInstances`
(read instance count from application.properties at startup), `customSerializer`, `inputPojoClass`,
and `inputStrategy` / `outputStrategy` for snake-case or camel-case serialization control.

Functions must be stateless. They must not share mutable state via static fields or `ThreadLocal`
variables, and must never call other user functions directly. All inter-function communication goes
through `PostOffice`. This isolation is what enables independent unit testing, safe parallel execution,
and hot-deployment of individual functions.

Throw `AppException(statusCode, message)` to return structured error responses with HTTP-compatible
status codes.

---

## Event Envelope

Every message in the framework is transported as an `EventEnvelope` — an immutable container with three
distinct parts: **metadata** (routing address, trace ID, correlation ID, status code, execution timing),
**headers** (`Map<String, String>` of user-defined parameters passed to `handleEvent`), and **body**
(the event payload — a PoJo, `Map<String, Object>`, Java primitive, or `byte[]`).

Envelopes are serialized with MsgPack (binary JSON) inside the event bus and converted to standard JSON
at HTTP boundaries. User code never serializes directly. The framework maps the envelope body to the
typed `input` argument of `handleEvent`, and wraps the return value into a new envelope automatically.

```java
EventEnvelope event = new EventEnvelope()
    .setTo("v1.get.profile")
    .setHeader("profile_id", "100")
    .setBody(requestMap);
EventEnvelope response = po.request(event, 5000).get();
Profile profile = response.getBody(Profile.class);
```

The `status` field uses HTTP-compatible codes; `getStatus() >= 400` indicates an error. Fluent setter
methods (`setTo`, `setHeader`, `setBody`, `setStatus`, `setTrace`, `setCorrelationId`) all return
`EventEnvelope` for chaining. To inspect all envelope metadata from inside a function, declare
the input type as `EventEnvelope` in the `TypedLambdaFunction` signature.

---

## Event Script and Flow Configuration

Event Script is a YAML DSL that represents an event flow as a data structure rather than as code.
A flow file specifies which function executes first, how data is mapped between functions, and what
to do on success or error. An entire transaction's orchestration logic lives in a YAML file that can
be changed and redeployed without modifying Java code.

```yaml
flow:
  id: 'create-profile'
  description: 'Create a user profile with field encryption'
  ttl: 30s
  exception: 'v1.hello.exception'

first.task: 'v1.encrypt.fields'

tasks:
  - input:
      - 'input.body -> *'
      - 'text(address, telephone) -> protected_fields'
    process: 'v1.encrypt.fields'
    output:
      - 'result -> model.profile'
    description: 'Encrypt PII fields before storage'
    execution: sequential
    next:
      - 'v1.save.profile'

  - input:
      - 'model.profile -> *'
    process: 'v1.save.profile'
    output:
      - 'text(application/json) -> output.header.content-type'
      - 'result -> output.body'
    description: 'Persist profile and return to caller'
    execution: end
```

**Input/output data mapping** uses an `origin -> destination` syntax with namespaces. Every expression
in the `input:` and `output:` lists is a mapping statement. The namespaces are:

- `input.` — the incoming request dataset. For HTTP: `input.body`, `input.header.<name>`,
  `input.path_parameter.<name>`, `input.query.<name>`, `input.method`, `input.uri`, `input.session`
- `model.` — the per-transaction state machine, readable and writable by all tasks in the flow.
  Use `model.parent.` (alias: `model.root.`) to share state with sub-flows.
- `result` — the entire return value of the just-executed task
- `result.<key>` — a specific field from the return value (uses dot-bracket notation)
- `output.body` — the final response body returned to the caller
- `output.header.<name>` — a response header
- `output.status` — the HTTP response status code
- `header.<name>` — a key-value passed into the next function's `headers` argument
- `error.status`, `error.message`, `error.task`, `error.stack` — available in exception handlers
- `text(value)` — a string constant (also used for content-type, e.g., `text(application/json)`)
- `int(n)`, `long(n)`, `float(n)`, `double(n)`, `boolean(true|false)` — typed constants
- `map(k=v, ...)` or `map(config.key)` — a map of key-values or values from application config

The wildcard `-> *` maps the entire source object as the function's input body. Dot-bracket notation
(`model.user.address`, `numbers[1]`) is used throughout for nested access.

**Execution types** control flow advancement after each task: `sequential` continues to the `next`
task list; `end` terminates the flow and delivers the result; `response` sends the HTTP response
immediately and continues executing remaining tasks asynchronously; `fork-n-join` dispatches to
multiple tasks in parallel and waits for all; `parallel` dispatches without waiting; `decision`
evaluates the task's boolean return to branch; `pipeline` chains tasks as a streaming pipeline;
`sink` discards the result and continues without waiting.

Sub-flows are referenced using the `flow://` protocol: `process: 'flow://my-sub-flow'`. The parent
TTL must cover the combined execution time of all sub-flows.

---

## REST Automation

HTTP endpoints are declared in `rest.yaml` without writing Java controllers:

```yaml
rest:
  - service: "http.flow.adapter"
    methods: ['GET', 'POST']
    url: "/api/profile/{profile_id}"
    flow: 'get-profile'
    timeout: 10s
    cors: cors_1
    headers: header_1
    tracing: true
    authentication: 'v1.auth.validator'
```

When `flow` is specified, the HTTP flow adapter packages the request and passes it to the flow engine.
When `service` points directly to a function route (without `flow`), requests go to that function
without a flow configuration. Path template parameters (`{profile_id}`) are accessible in flows as
`input.path_parameter.profile_id`. The optional `authentication` field names a function that receives
an `AsyncHttpRequest` and returns `true` to approve, `false` for HTTP 401, or throws
`AppException(statusCode, message)` for custom rejections.

---

## Function Execution Model

Virtual threads are the default execution environment for all functions. Java 21 virtual threads
are cooperatively scheduled, cheap to create in large numbers, and suspend non-destructively on
blocking calls. A `po.request(event, timeout).get()` call appears sequential in code but the
virtual thread is suspended while waiting, freeing the JVM to schedule other work. This means
sequential, readable code behaves with the performance characteristics of reactive code, without
the callback complexity.

For functions that make blocking calls incompatible with virtual threads — tight CPU-bound loops
or legacy code using kernel-thread-specific constructs — add `@KernelThreadRunner`:

```java
@PreLoad(route = "v1.heavy.computation", instances = 5)
@KernelThreadRunner
public class HeavyTask implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    // executes in kernel thread pool (configurable via kernel.thread.pool, default 100)
}
```

Keep `instances` small for kernel-thread functions (5–10 is typical). The default kernel thread
pool is capped at 100 (`kernel.thread.pool` in application.properties). Avoid the `synchronized`
keyword and `ThreadLocal` in virtual-thread functions; both create contention that undermines the
cooperative scheduling model.

Functions can also return `Mono<T>` or `Flux<T>` from Project Reactor. The framework integrates
these reactive return types automatically. For `Flux`, the consumer function receives stream
coordinates in headers (`x-stream-id`, `x-ttl`) and processes the stream via `FluxConsumer<T>`.

---

## Core APIs

**PostOffice** is the messaging client. Always obtain it from `handleEvent`'s `headers` and `instance`
arguments to preserve the distributed trace chain:

```java
PostOffice po = PostOffice.trackable(headers, instance);
```

Key methods: `po.send(route, body)` — fire-and-forget; `po.request(event, timeoutMs).get()` — blocking
RPC on virtual thread; `po.asyncRequest(event, timeoutMs)` — async RPC returning a Vert.x `Future`;
`po.eRequest(event, timeoutMs)` — async RPC returning `CompletableFuture`; `po.asyncRequest(events, timeout)`
— fork-n-join parallel requests; `po.sendLater(event, futureDate)` — scheduled delivery;
`po.broadcast(route, body)` — send to all instances of a function. The `po.exists(route)` method can
discover public functions in other application instances when running in service mesh mode.

**Platform** is the singleton service registry:

```java
Platform platform = Platform.getInstance();
platform.register("my.dynamic.function", new MyFunction(), 5); // dynamic registration
platform.hasRoute("v1.get.profile");   // check if function is registered locally
platform.release("my.dynamic.function");
platform.waitForProvider("cloud.connector", 10); // wait up to 10s for a provider to be ready
```

**AsyncHttpRequest** is the typed input class for functions declared directly as REST endpoints without
a flow. It provides accessors for all HTTP request fields. **FluxConsumer\<T\>** wraps Flux streams
returned by other functions. **AppConfigReader** provides runtime access to `application.properties`
and `application.yml` values via `config.get("my.key")` and `config.getProperty("my.key")`.

---

## Distributed Architecture

Mercury scales beyond a single JVM through two complementary mechanisms. **Event over HTTP** allows
functions in separate application instances to communicate by exposing a built-in `POST /api/event`
endpoint (`event.api.service`). A caller routes an event to a function in a peer instance using
`po.asyncRequest(event, timeout, headers, "http://peer/api/event", true)` — the same `EventEnvelope`
serialization crosses the network boundary transparently. In Kubernetes, the event API endpoint is
reached via internal cluster DNS without requiring an ingress.

**Minimalist Service Mesh** uses Kafka as a distributed routing table and event bridge. Setting
`cloud.connector=kafka` in `application.properties` enables the `cloud.connector` module, which
publishes public-function routes to the distributed registry and bridges inter-instance events
through Kafka. Functions with `isPrivate = false` become reachable by any instance in the mesh.
Calling code uses the same `PostOffice` API whether the target is local or remote.

---

## Key Annotations Reference

| Annotation | Purpose |
|---|---|
| `@PreLoad` | Register a function at startup: `route`, `instances`, `isPrivate`, `envInstances`, `customSerializer`, `inputStrategy`, `outputStrategy` |
| `@MainApplication` | Mark the application entry point class (implements `EntryPoint`); `sequence` controls order (1–999) |
| `@BeforeApplication` | Initialization hook that runs before `@MainApplication`; use sequences 3–999 for user code |
| `@KernelThreadRunner` | Execute the function in the kernel thread pool instead of virtual threads |
| `@EventInterceptor` | Receive the raw `EventEnvelope` as input body; return value is ignored (advanced routing patterns) |
| `@ZeroTracing` | Suppress distributed tracing for this function |
| `@WebSocketService` | Register a WebSocket endpoint handler; annotated class implements `LambdaFunction` |
| `@CloudConnector` | Mark a class as a cloud connector plug-in selected by `cloud.connector` in application.properties |
| `@CloudService` | Mark a class as a cloud service plug-in selected by `cloud.services` in application.properties |
| `@OptionalService` | Conditionally load a function based on a configuration expression (e.g., `!feature.flag`) |

---

## Key Configuration Files

| File | Purpose |
|---|---|
| `application.properties` / `application.yml` | Port (`rest.server.port`), component scan (`web.component.scan`), flow list reference (`yaml.flow.automation`), cloud connector (`cloud.connector`), serialization strategy (`snake.case.serialization`) |
| `rest.yaml` | Declarative HTTP endpoint definitions: URL, methods, flow or service route, CORS config, auth function, tracing |
| `flows.yaml` | Index of individual flow YAML files to load (`flows:` list; optional `location:` for non-classpath paths) |
| `*.yml` in `flows/` | Individual event flow configurations, each defining one transaction's complete task sequence |

All configuration files are loaded from the classpath (`classpath:/`) in `src/main/resources` by
default. File-system paths can be specified using the `file:/` prefix, and multiple files can be
comma-separated (e.g., `yaml.rest.automation=file:/tmp/config/rest.yaml, classpath:/rest.yaml`).

---

## Further Reading

- [Methodology](METHODOLOGY.md) — design principles: input-process-output, zero dependency, event choreography, platform abstraction
- [Getting Started](CHAPTER-1.md) — hands-on walkthrough with the composable example application
- [Function Execution Strategies](CHAPTER-2.md) — virtual vs. kernel threads, Mono/Flux, authentication functions
- [REST Automation](CHAPTER-3.md) — complete `rest.yaml` syntax reference
- [Event Script Syntax](CHAPTER-4.md) — complete flow DSL reference including all task types, data mapping, sub-flows, and preload overrides
- [API Overview](CHAPTER-9.md) — full `PostOffice`, `Platform`, `EventEnvelope`, and configuration API reference
- [Build, Test and Deploy](CHAPTER-5.md) — CI/CD, packaging, and deployment patterns
