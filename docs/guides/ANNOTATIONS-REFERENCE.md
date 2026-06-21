# Annotations Reference

*Reference: Complete reference for all Mercury Composable Java annotations.*

Mercury Composable uses annotations to declare composable functions, control execution
behavior, and manage application lifecycle. This page documents every annotation with
its full parameter set, behavior, and usage examples.

All core annotations are in the `org.platformlambda.core.annotations` package.
The `@SimplePlugin` annotation is in `com.accenture.models` (event-script-engine module).

---

## Quick lookup

| Annotation | Category | Parameters | Purpose |
|------------|----------|------------|---------|
| [`@PreLoad`](#preload) | Service registration | 8 | Register a composable function at startup |
| [`@WebSocketService`](#websocketservice) | Service registration | 2 | Register a WebSocket endpoint handler |
| [`@MainApplication`](#mainapplication) | Lifecycle | 1 | Application entry point |
| [`@BeforeApplication`](#beforeapplication) | Lifecycle | 1 | Pre-startup initialization hook |
| [`@KernelThreadRunner`](#kernelthreadrunner) | Execution control | 0 | Run in kernel thread pool instead of virtual threads |
| [`@ZeroTracing`](#zerotracing) | Execution control | 0 | Suppress distributed tracing |
| [`@EventInterceptor`](#eventinterceptor) | Execution control | 0 | Receive raw `EventEnvelope` as input |
| [`@OptionalService`](#optionalservice) | Conditional loading | 1 | Load only when a config condition is true |
| [`@CloudConnector`](#cloudconnector) | Cloud integration | 2 | Service mesh connector plug-in |
| [`@CloudService`](#cloudservice) | Cloud integration | 2 | Cloud service plug-in |
| [`@SimplePlugin`](#simpleplugin) | Event Script | 0 | Event Script `f:` function plug-in |

---

## @PreLoad

Registers a class as a composable function with the event system at startup. This is the
most frequently used annotation in Mercury — every composable function requires it.

**Target:** `ElementType.TYPE` (class-level)
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `route` | `String` | *(required)* | Route name(s) for this function. Use lowercase with dots (e.g. `"v1.get.profile"`). Comma-separate multiple routes to register the same function under several names. |
| `instances` | `int` | `1` | Maximum concurrent worker instances. Range 1–1000. Controls how many events this function can handle simultaneously. |
| `envInstances` | `String` | `""` | Override `instances` from an application property or environment variable. Syntax: `"${SOME_VAR:default}"`. Takes precedence over `instances` when the property is present. |
| `isPrivate` | `boolean` | `true` | `true` = accessible only within this JVM. `false` = published to the service mesh and reachable from other instances via PostOffice or Event-over-HTTP. |
| `inputPojoClass` | `Class<?>` | `Void.class` | Hint for deserializing `List<T>` input. Set when the function accepts a list of PoJos and the generic type is erased at runtime (e.g. `inputPojoClass = MyPojo.class`). |
| `customSerializer` | `Class<?>` | `Void.class` | Custom serializer class implementing `CustomSerializer`. Overrides `inputStrategy`/`outputStrategy` for this function. Use when the default Gson/MsgPack serialization is insufficient for a PoJo type. |
| `inputStrategy` | `SerializationStrategy` | `DEFAULT` | Input deserialization case convention. `DEFAULT` — inherits global `snake.case.serialization`. `SNAKE` — forces snake_case. `CAMEL` — forces camelCase. |
| `outputStrategy` | `SerializationStrategy` | `DEFAULT` | Output serialization case convention. Same values as `inputStrategy`. |

### Behavior

During application startup the framework scans packages listed in `web.component.scan` for
classes annotated with `@PreLoad`. Each annotated class is instantiated (requires a default
no-arg constructor), validated against the required interface, and registered in the
Platform singleton under each route name. Worker instances are created on demand up to the
`instances` limit; excess requests queue until a worker is free.

### Required interface

The annotated class must implement `TypedLambdaFunction<I, O>` or `LambdaFunction`.

### Example

```java
// Basic registration
@PreLoad(route = "v1.get.profile", instances = 100)
public class GetProfile implements TypedLambdaFunction<Map<String, Object>, Profile> {

    @Override
    public Profile handleEvent(Map<String, String> headers,
                               Map<String, Object> input, int instance) throws Exception {
        String profileId = headers.get("profile_id");
        return profileService.findById(profileId);
    }
}

// Public function with serialization strategy
@PreLoad(route = "v1.search.users", instances = 50, isPrivate = false,
         outputStrategy = SerializationStrategy.CAMEL)
public class SearchUsers implements TypedLambdaFunction<SearchRequest, List<UserRecord>> { ... }

// Multiple routes from one class
@PreLoad(route = "greeting.case.1, greeting.case.2", instances = 10)
public class Greetings implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> { ... }

// Instance count from environment variable
@PreLoad(route = "v1.heavy.task", envInstances = "${HEAVY_TASK_INSTANCES:10}")
public class HeavyTask implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> { ... }
```

### Notes

- Route names must be lowercase letters, digits, and dots. At least one dot is required.
- Multiple routes: each name gets a full, independent set of `instances` workers.
- The `instance` parameter in `handleEvent` indicates which worker is executing (0-based).
- Spring Boot (`rest-spring-3` module): field injection (`@Autowired`, `@Value`) works.
  Constructor injection does NOT work because instances are created before the Spring context.
- `@PreLoad` can be combined with `@KernelThreadRunner`, `@EventInterceptor`, `@ZeroTracing`,
  and `@OptionalService` on the same class.
- Runtime instance count override (without recompiling):
  set `worker.instances.<route>=N` in `application.properties`.
- Preload override YAML: use `yaml.preload.override` to re-map routes or change instance
  counts for library functions you cannot recompile.

---

## @WebSocketService

Registers a class as a WebSocket endpoint handler.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String` | *(required)* | WebSocket endpoint path segment. Combined with `namespace` to form the full URL: `/{namespace}/{value}/{handle}`. |
| `namespace` | `String` | `"ws"` | URL namespace prefix. Default produces URLs like `/ws/hello/{handle}`. |

### Behavior

At startup the framework registers the annotated class as a handler for the constructed
WebSocket path. Incoming WebSocket events are delivered via the standard `handleEvent`
method with `type` headers set to `"open"`, `"close"`, `"bytes"`, or `"string"`.

### Required interface

The annotated class must implement `LambdaFunction`.

### Example

```java
@WebSocketService("hello")
public class WsEchoDemo implements LambdaFunction {

    @Override
    public Object handleEvent(Map<String, String> headers, Object body,
                              int instance) throws Exception {
        String type = headers.get("type");
        if ("string".equals(type)) {
            // echo the message back
            return body;
        }
        return null;
    }
}
```

This creates a WebSocket endpoint at `/ws/hello/{handle}`.

### Notes

- Requires `rest.automation=true` and either `rest.server.port` or `websocket.server.port`.
- Can be combined with `@OptionalService` for conditional loading.
- To disable WebSocket support, remove both the `websocket.server.port` property and all
  `@WebSocketService` classes.

---

## @MainApplication

Marks the application entry point class. Its `start()` method is called after the event
system initialises and all `@PreLoad` services are registered.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `sequence` | `int` | `10` | Execution order when multiple `@MainApplication` classes exist. Range 1–999. Lower values run first. |

### Behavior

After `@BeforeApplication` hooks and `@PreLoad` service registration complete, the
framework instantiates all `@MainApplication` classes in `sequence` order and calls
`start(String[] args)` on each.

### Required interface

The annotated class must implement `EntryPoint`.

### Example

```java
@MainApplication
public class MainApp implements EntryPoint {

    public static void main(String[] args) {
        AutoStart.main(args);   // bootstraps the Mercury runtime
    }

    @Override
    public void start(String[] args) throws Exception {
        log.info("Application started");
        // initialization logic: register dynamic routes, start background jobs, etc.
    }
}
```

### Notes

- Multiple `@MainApplication` classes are allowed in the same application. Use `sequence`
  to order them.
- The static `main` method should contain only `AutoStart.main(args)`. All initialization
  logic belongs in `start()`.
- Can be combined with `@OptionalService` for conditional activation.
- In Spring Boot apps (`rest-spring-3`) execution is deferred until the HTTP server
  completes startup.

---

## @BeforeApplication

Initialization hook that runs **before** `@PreLoad` service registration and before any
`@MainApplication` classes execute. Use it for pre-flight checks, secret decryption, or
generating X.509 certificates before the rest of the application starts.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `sequence` | `int` | `10` | Execution order. Range 1–999. Sequences 0 and 2 are reserved by the framework. |

### Behavior

`@BeforeApplication` classes are the first user code to execute. They run in `sequence`
order before the event system finishes starting and before any `@PreLoad` services are
registered.

### Required interface

The annotated class must implement `EntryPoint`.

### Example

```java
@BeforeApplication(sequence = 5)
public class EnvSetup implements EntryPoint {

    @Override
    public void start(String[] args) throws Exception {
        // decrypt secrets, write certs to /tmp, set system properties
        log.info("Environment initialized");
    }
}
```

### Notes

- **Reserved sequences**: `0` is used by `EssentialServiceLoader`; `2` is used by the
  Event Script engine. User code should use sequences 3–999 (or 1 if it must run before
  all framework modules).
- Execution order: `@BeforeApplication` → `@PreLoad` registration → `@MainApplication`.
- Can be combined with `@OptionalService`.

---

## @KernelThreadRunner

Forces the annotated function to run on the kernel thread pool instead of the default
Java 21 virtual thread pool. Use only for legacy blocking code or CPU-intensive loops
that are incompatible with virtual threads.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`
**Parameters:** None (marker annotation)

### Behavior

When the `ServiceDef` constructor detects `@KernelThreadRunner`, it sets the runner type
to `KERNEL_THREAD`. The `WorkerDispatcher` then uses the kernel thread pool (configured
by `kernel.thread.pool`, default 100, max 200) instead of virtual threads for every
invocation of this function.

### Example

```java
@PreLoad(route = "v1.heavy.computation", instances = 5)
@KernelThreadRunner
public class HeavyComputation implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers,
                                           Map<String, Object> input, int instance) throws Exception {
        // CPU-intensive or unavoidably blocking work
        return result;
    }
}
```

### When to use vs. virtual threads

| Scenario | Thread type |
|----------|-------------|
| Standard business logic | Virtual (default) |
| PostOffice RPC calls | Virtual — `request().get()` suspends, not blocks |
| `Thread.sleep()` | Virtual — sleep suspends the virtual thread |
| `synchronized` block | Kernel — synchronized blocks a carrier thread |
| `ThreadLocal` variables | Kernel — `ThreadLocal` makes virtual threads heavyweight |
| Legacy JDBC / blocking I/O that cannot be refactored | Kernel |
| CPU-bound tight loops | Kernel — keeps CPU hot without yielding |

### Notes

- Keep `instances` small (5–20) for kernel-thread functions to avoid exhausting the pool.
- The sum of worker instances across all `@KernelThreadRunner` functions should not exceed
  `kernel.thread.pool`.
- Can be combined with `@PreLoad`, `@EventInterceptor`, and `@ZeroTracing`.

---

## @EventInterceptor

Causes the function to receive the raw `EventEnvelope` as its input body instead of the
unwrapped payload. The function's return value is **ignored** — responses must be sent
programmatically via `PostOffice`.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`
**Parameters:** None (marker annotation)

### Behavior

When `ServiceDef` detects `@EventInterceptor`, it sets the `interceptor` flag. The
runtime delivers the original `EventEnvelope` as the input object rather than its
deserialized body. The function must inspect headers, route the event, and send any
response manually through `PostOffice`.

### Required interface

Typically `TypedLambdaFunction<EventEnvelope, Void>`.

### Example

```java
@EventInterceptor
@ZeroTracing
@PreLoad(route = "v1.request.router", instances = 200)
public class RequestRouter implements TypedLambdaFunction<EventEnvelope, Void> {

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input,
                            int instance) throws Exception {
        PostOffice po = new PostOffice(headers, instance);
        // examine input.getHeaders(), reroute or transform
        EventEnvelope forwarded = new EventEnvelope()
            .setTo("v1.backend.service")
            .setBody(input.getBody())
            .setHeaders(input.getHeaders());
        po.send(forwarded);
        return null;   // return value always ignored
    }
}
```

### Notes

- Use for middleware, protocol adapters, resilience patterns, and async callback handlers.
- Cannot return a value to the caller automatically — use `po.send(replyTo, response)` or
  `po.asyncRequest()` to reply.
- Frequently combined with `@ZeroTracing` (high-volume routing should not be traced).
- Can be combined with `@KernelThreadRunner` when the interception logic needs blocking I/O.
- For the complete `EventEnvelope` API, see the [Event Envelope Reference](EVENT-ENVELOPE-REFERENCE.md).

---

## @ZeroTracing

Suppresses distributed trace annotation for the annotated function. The function's
executions are not recorded in the telemetry stream.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`
**Parameters:** None (marker annotation)

### Behavior

When `ServiceDef` detects `@ZeroTracing`, it sets `trackable = false`. The telemetry
service skips trace recording for every invocation of this function.

### Example

```java
@ZeroTracing
@PreLoad(route = "v1.health.check", instances = 10)
public class HealthCheck implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers,
                                           Map<String, Object> input, int instance) throws Exception {
        return Map.of("status", "UP");
    }
}
```

### Notes

- Use for high-frequency internal utilities where tracing adds overhead without value.
- System services such as `HttpAuth`, `Telemetry`, and `EventApiService` use this.
- Can be combined with `@PreLoad`, `@EventInterceptor`, and `@KernelThreadRunner`.

---

## @OptionalService

Makes any annotated class conditional: the class is loaded only when the configuration
expression evaluates to `true`. The expression is evaluated against `application.properties`
or `application.yml`.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String` | *(required)* | One or more condition expressions, comma-separated (OR logic). See expression syntax below. |

### Expression syntax

| Expression | Loads when |
|------------|------------|
| `"key"` | `key=true` in application.properties |
| `"key=value"` | property `key` equals `value` |
| `"!key"` | `key` is absent or `false` |
| `"k1=v1, k2=v2"` | `k1=v1` **OR** `k2=v2` (any one match) |

### Example

```java
// Load only in production environment
@OptionalService("env=production")
@PreLoad(route = "v1.analytics.tracker", instances = 20)
public class AnalyticsTracker implements TypedLambdaFunction<Map<String, Object>, Void> { ... }

// Load only if feature flag is off
@OptionalService("!legacy.mode")
@MainApplication(sequence = 15)
public class NewMainApp implements EntryPoint { ... }

// Load if either condition matches
@OptionalService("server.port=8080, rest.automation")
public class DevHelper implements EntryPoint { ... }
```

### Notes

- Can be combined with `@PreLoad`, `@MainApplication`, `@BeforeApplication`, and
  `@WebSocketService`.
- When the condition is false, the framework logs "Skipping optional [class]" and does
  not instantiate the class.
- Multiple conditions use OR logic: the class loads if **any** condition matches.

---

## @CloudConnector

Marks a class as a cloud connector plug-in. Exactly one connector is activated per
application, selected by the `cloud.connector` application property.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | `String` | *(required)* | Connector identifier. Must match the value of `cloud.connector` in application.properties for this connector to be loaded. |
| `original` | `String` | `""` | When non-empty, this connector wraps the connector named by `original`. Use for the decorator pattern. |

### Behavior

When the application calls `Platform.connectToCloud()`, the framework scans for the
`@CloudConnector` whose `name` matches the `cloud.connector` property, instantiates it,
validates it implements `CloudSetup`, and calls `initialize()`. The connector then
publishes public-function routes to the distributed registry and bridges inter-instance
events through the message broker.

### Required interface

`CloudSetup` — `void initialize()`

### Example

```java
// From kafka-connector module
@CloudConnector(name = "kafka")
public class KafkaConnector implements CloudSetup {

    @Override
    public void initialize() {
        // set up Kafka producer/consumer and register presence monitor
    }
}
```

Activated by: `cloud.connector=kafka` in `application.properties`.

### Notes

- Only one `@CloudConnector` is loaded per application instance.
- Runs before any `@CloudService` modules.
- Use `cloud.connector=none` (the default) to disable the service mesh.

---

## @CloudService

Marks a class as a cloud service plug-in. Multiple services can be loaded per
application, selected by the `cloud.services` property.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `org.platformlambda.core.annotations`

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | `String` | *(required)* | Service identifier. Must match one entry in the `cloud.services` comma-separated list for this service to be loaded. |
| `original` | `String` | `""` | When non-empty, this service wraps the service named by `original`. Use for the decorator pattern. |

### Behavior

After connector initialization, the framework scans for all `@CloudService` classes
whose `name` appears in `cloud.services`, instantiates them, and calls `initialize()`
on each. Services are started automatically after the connector or can be started
standalone via `Platform.startCloudServices()`.

### Required interface

`CloudSetup` — `void initialize()`

### Example

```java
@CloudService(name = "kafka.pubsub")
public class PubSubSetup implements CloudSetup {

    @Override
    public void initialize() {
        // set up Kafka pub/sub topics and handlers
    }
}
```

Activated by: `cloud.services=kafka.pubsub` in `application.properties`.

### Notes

- Unlike `@CloudConnector`, multiple `@CloudService` entries can be active simultaneously.
- Used for optional cloud features such as pub/sub and service registries.

---

## @SimplePlugin

Registers a class as a plugin function usable in Event Script YAML flows via the `f:`
prefix. Plugins perform atomic calculations directly inside data mapping rules without
requiring a full `TypedLambdaFunction` service.

**Target:** `ElementType.TYPE`
**Retention:** `RetentionPolicy.RUNTIME`
**Package:** `com.accenture.models` (event-script-engine module)
**Parameters:** None (marker annotation)

### Behavior

At startup the `SimplePluginLoader` scans configured packages for `@SimplePlugin` classes,
validates security constraints (only `java.lang`, `java.util`, `java.math`, `java.time`,
and Mercury framework classes are permitted — no I/O), instantiates each class, and
registers it by the name returned by `getName()`.

### Required interface

`PluginFunction`:

```java
public interface PluginFunction {
    // Default: camelCase of the class simple name
    default String getName() {
        String name = this.getClass().getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    Object calculate(Object... input);
}
```

### Example

```java
@SimplePlugin
public class TaxCalculator implements PluginFunction {

    @Override
    public Object calculate(Object... input) {
        if (input.length != 2) throw new IllegalArgumentException("requires 2 args");
        double amount = ((Number) input[0]).doubleValue();
        double rate   = ((Number) input[1]).doubleValue();
        return amount * rate;
    }
    // getName() returns "taxCalculator" by default
}
```

Usage in a flow YAML:

```yaml
input:
  - 'model.subtotal -> amount'
  - 'double(0.08) -> rate'
  - 'f:taxCalculator(model.subtotal, double(0.08)) -> tax'
```

### Notes

- Plugin name defaults to the camelCase of the class simple name. Override `getName()` to
  use a different name.
- Security-enforced: classes that attempt I/O, reflection, or dynamic loading will fail
  to load (logged as errors at startup).
- Built-in plugins cover arithmetic, logic, type conversion, string operations, collection
  transformation, and generators. See [Flow Configuration Schema Reference](FLOW-SCHEMA-REFERENCE.md)
  for the full list.
- Located in the `event-script-engine` module; requires that module on the classpath.

---

## Annotation combinations

The following combinations are confirmed valid. Annotations applied to the same class are
processed independently; applying multiple annotations to one class stacks their effects.

| Combination | Valid | Effect |
|-------------|-------|--------|
| `@PreLoad` + `@KernelThreadRunner` | ✅ | Function runs on the kernel thread pool |
| `@PreLoad` + `@EventInterceptor` | ✅ | Function receives raw `EventEnvelope`; return value ignored |
| `@PreLoad` + `@ZeroTracing` | ✅ | Function executions are not traced |
| `@PreLoad` + `@OptionalService` | ✅ | Function registered only when condition is true |
| `@KernelThreadRunner` + `@EventInterceptor` | ✅ | Blocking interceptor on kernel threads |
| `@KernelThreadRunner` + `@ZeroTracing` | ✅ | Untraced kernel-thread function |
| `@EventInterceptor` + `@ZeroTracing` | ✅ | High-volume interceptor without tracing (used by system services) |
| `@PreLoad` + `@KernelThreadRunner` + `@EventInterceptor` + `@ZeroTracing` | ✅ | All four can coexist |
| `@MainApplication` + `@OptionalService` | ✅ | Entry point loaded conditionally |
| `@BeforeApplication` + `@OptionalService` | ✅ | Init hook loaded conditionally |
| `@WebSocketService` + `@OptionalService` | ✅ | WebSocket endpoint loaded conditionally |
| `@KernelThreadRunner` without `@PreLoad` | ⚠️ | No effect; only processed for registered functions |
| `@EventInterceptor` without `@PreLoad` | ⚠️ | No effect; only processed for registered functions |
| `@MainApplication` + `@PreLoad` | ⚠️ | Technically valid; rarely useful in practice |

---

## Required interfaces

Each annotation requires the annotated class to implement a specific interface.

| Annotation | Required Interface | Method |
|------------|-------------------|--------|
| `@PreLoad` | `TypedLambdaFunction<I, O>` or `LambdaFunction` | `handleEvent(headers, input, instance)` |
| `@WebSocketService` | `LambdaFunction` | `handleEvent(headers, input, instance)` |
| `@MainApplication` | `EntryPoint` | `start(String[] args)` |
| `@BeforeApplication` | `EntryPoint` | `start(String[] args)` |
| `@KernelThreadRunner` | *(combined with @PreLoad)* | — |
| `@EventInterceptor` | `TypedLambdaFunction<EventEnvelope, Void>` *(typical)* | `handleEvent(headers, input, instance)` |
| `@ZeroTracing` | *(combined with @PreLoad)* | — |
| `@OptionalService` | *(combined with another annotation)* | — |
| `@CloudConnector` | `CloudSetup` | `initialize()` |
| `@CloudService` | `CloudSetup` | `initialize()` |
| `@SimplePlugin` | `PluginFunction` | `calculate(Object... input)` |

### Interface signatures

```java
// org.platformlambda.core.models.TypedLambdaFunction<I, O>
public interface TypedLambdaFunction<I, O> {
    O handleEvent(Map<String, String> headers, I input, int instance) throws Exception;
}

// org.platformlambda.core.models.LambdaFunction
// (extends TypedLambdaFunction<Object, Object>)
public interface LambdaFunction extends TypedLambdaFunction<Object, Object> {
    Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception;
}

// org.platformlambda.core.system.EntryPoint
public interface EntryPoint {
    void start(String[] args) throws Exception;
}

// org.platformlambda.core.models.CloudSetup
public interface CloudSetup {
    void initialize();
}

// com.accenture.models.PluginFunction  (event-script-engine module)
public interface PluginFunction {
    default String getName() {
        String name = this.getClass().getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    Object calculate(Object... input);
}

// org.platformlambda.core.models.CustomSerializer  (for @PreLoad customSerializer)
public interface CustomSerializer {
    Map<String, Object> toMap(Object obj);
    <T> T toPoJo(Object obj, Class<T> toValueType);
}
```

---

## Component scanning

Annotated classes are discovered by scanning Java packages at startup. Only classes
located inside the configured packages are found.

```properties
# application.properties
web.component.scan=com.example.myapp, com.example.shared
```

The `web.component.scan` property accepts a comma-separated list of package names. The
scanner finds `@PreLoad`, `@MainApplication`, `@BeforeApplication`, `@WebSocketService`,
and `@SimplePlugin` classes within those packages (and all sub-packages).

**Spring Boot note**: When using `rest-spring-3` with multiple packages in
`web.component.scan`, Spring's own component scanner requires the separate
`spring.component.scan` property to scan for Spring beans.

**What happens if a class is outside the scan path**: The class is silently ignored.
No error is raised. If a function is unexpectedly missing from the event system, verify
that its package is listed in `web.component.scan`.

See the [Configuration Reference](CONFIGURATION-REFERENCE.md) for all scanning-related
properties.

---

## See also

- [Getting Started](CHAPTER-1.md) — first tutorial with `@PreLoad` and `@MainApplication` examples
- [Function Execution Strategies](CHAPTER-2.md) — deep dive on `@KernelThreadRunner` and `@EventInterceptor`
- [Event Script Syntax](CHAPTER-4.md) — `@PreLoad` serialization strategies; `@SimplePlugin` reference
- [Flow Configuration Schema Reference](FLOW-SCHEMA-REFERENCE.md) — `f:` plugin usage in flow YAML
- [Configuration Reference](CONFIGURATION-REFERENCE.md) — `web.component.scan`, `kernel.thread.pool`, and related properties
