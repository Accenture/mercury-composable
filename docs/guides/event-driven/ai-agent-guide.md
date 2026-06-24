---
title: AI agent guide — writing composable functions (Layer 1)
summary: The authoritative context an AI agent needs to write mercury-composable functions
  correctly — the annotation contract, interface contract, a pre-write checklist, PostOffice
  RPC patterns, and worked examples.
layer: platform-core
audience: [ai-agent, developer]
keywords: [ai agent, composable function, PreLoad, TypedLambdaFunction, LambdaFunction, PostOffice, EventEnvelope, annotations, layer 1]
related:
  - guides/event-driven/write-your-first-function.md
  - guides/event-driven/function-execution.md
  - guides/annotations-reference.md
  - guides/api-overview.md
---

# AI agent guide — writing composable functions (Layer 1)

> **At a glance**
>
> - **Read this if you are an AI agent** asked to write or review a composable function. It is the
>   single context you need — you should not need to read the engine source.
> - **Generate from contracts.** The `@PreLoad` annotation and the `TypedLambdaFunction` interface are
>   the source of truth. Validate against the checklist below before proposing code.
> - A composable function is **plain Java** — no framework base class, no DI container, no coupling to
>   other functions by import. The only constraint is the `@PreLoad` annotation and the interface.

---

## How functions are loaded {#deploy}

Functions are **not called directly**. The framework loads them at startup:

1. Annotate the class with `@PreLoad(route="…")`.
2. Implement `TypedLambdaFunction<I, O>` (or `LambdaFunction` for raw body access).
3. The engine scans the classpath, registers every `@PreLoad` class on the in-memory event bus under
   its `route` name, and starts `instances` concurrent workers for it.
4. Any caller (REST endpoint, Event Script flow, another function) addresses the function **only by
   its route name string**.

A function that violates the contract (duplicate route, invalid `instances`, bad interface) causes the
application to fail at startup — correctness is checkable before runtime.

---

## `@PreLoad` annotation — full contract {#annotation}

```java
@PreLoad(
    route          = "my.function",        // REQUIRED — unique route name (dot-separated, lowercase)
    instances      = 1,                    // optional — workers (default 1, max 1000)
    envInstances   = "",                   // optional — property key to read instances from at startup
    isPrivate      = true,                 // optional — true = local event bus only (default)
    inputPojoClass = Void.class,           // optional — see List-of-PoJo note below
    customSerializer = Void.class,         // optional — implements CustomSerializer
    inputStrategy  = SerializationStrategy.DEFAULT,   // optional — SNAKE, CAMEL, DEFAULT
    outputStrategy = SerializationStrategy.DEFAULT    // optional — SNAKE, CAMEL, DEFAULT
)
```

| Parameter | Type | Default | Notes |
|:---|:---|:---|:---|
| `route` | String | **required** | Unique. Dot-separated lowercase convention (`v1.my.function`). |
| `instances` | int | `1` | Number of concurrent workers. Production services typically use 10–100. Max 1000. |
| `envInstances` | String | `""` | Property key in `application.properties` to read instances at startup (e.g. `"${MY_FN_WORKERS:10}"`). Falls back to `instances` if absent. |
| `isPrivate` | boolean | `true` | `true` = accessible only within this process (local event bus). `false` = published to the distributed service mesh. REST automation and Event Script flows call functions locally — `true` is correct for almost all functions. |
| `inputPojoClass` | Class | `Void.class` | Used when `I` is `Object` AND the arriving payload is a `List<Map>`. The engine deserializes each Map to this class. No effect when `I` is already a concrete PoJo type. |
| `customSerializer` | Class | `Void.class` | Implements `CustomSerializer` for non-standard wire formats. |
| `inputStrategy` | enum | `DEFAULT` | `SNAKE` / `CAMEL` / `DEFAULT` serialization for inbound JSON field names. |
| `outputStrategy` | enum | `DEFAULT` | `SNAKE` / `CAMEL` / `DEFAULT` serialization for outbound JSON field names. |

---

## Interface contract {#interface}

### `TypedLambdaFunction<I, O>` — preferred

```java
public interface TypedLambdaFunction<I, O> {
    O handleEvent(Map<String, String> headers, I input, int instance) throws Exception;
}
```

- `headers` — request headers (`Map<String, String>`); case-insensitive lookup via `EventEnvelope.getHeader()`.
- `input` — the deserialized request body. Type `I` can be `Map<String, Object>`, a PoJo, or a primitive.
  For key-by-key data mapping (Event Script / Knowledge Graph), `I` must be `Map` or a PoJo —
  not a `List`.
- `instance` — the worker index (0 to `instances − 1`). Pass to `new PostOffice(headers, instance)`.
- Return type `O` — can be `Map`, PoJo, `List`, primitive, or `Void`. Reactive: `Mono<O>` or `Flux<O>`.
- **Any thrown `Exception` is caught by the framework** and returned as an error `EventEnvelope` to the
  caller — no unhandled exception will crash the process.

### `LambdaFunction` — untyped fallback

```java
// extends TypedLambdaFunction<Object, Object>
public interface LambdaFunction extends TypedLambdaFunction<Object, Object> {
    Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception;
}
```

Use `LambdaFunction` only when you need raw access to the body (pass-through, routing logic, or when
the input type varies). For all other cases, prefer `TypedLambdaFunction<I, O>` for a stricter contract.

---

## Pre-write checklist {#checklist}

> **Validate before proposing code:**
> - [ ] `route` is set (required). Follows the dot-separated lowercase convention.
> - [ ] `route` is unique across the application (no two `@PreLoad` classes share a route).
> - [ ] `instances` is appropriate for concurrency needs (default `1`; typical services use `10–100`; max `1000`).
> - [ ] Input type `I` is `Map<String, Object>` or a PoJo when the function participates in key-by-key
>       data mapping. Do **not** use `List` as `I` in that context.
> - [ ] If the function receives a `List<PoJo>` (via Event Script `*` passthrough), use `I = Object`
>       and set `inputPojoClass = ElementType.class`.
> - [ ] `isPrivate = false` is set only if the function must be visible to other services over the
>       distributed event mesh. For REST or Event Script, leave the default `true`.
> - [ ] The function holds **no direct reference to another user function** (no `new OtherFunction()`).
> - [ ] PostOffice is constructed as `new PostOffice(headers, instance)` inside `handleEvent` — not
>       cached as a field (it carries per-request trace context).
> - [ ] Numeric values from event headers use `Utility.getInstance().str2int()` / `str2long()`, not
>       direct `Integer.parseInt()`, to survive the MsgPack Long↔Integer downcast.

---

## Patterns {#patterns}

### Typed function — Map I/O

```java
@PreLoad(route = "hello.function", instances = 10)
public class HelloFunction implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers,
                                           Map<String, Object> input,
                                           int instance) throws Exception {
        var name = input.getOrDefault("name", "world").toString();
        return Map.of("message", "Hello, " + name + "!");
    }
}
```

### Typed function — PoJo I/O

```java
@PreLoad(route = "v1.create.profile", instances = 10)
public class CreateProfile implements TypedLambdaFunction<Profile, ProfileConfirmation> {

    @Override
    public ProfileConfirmation handleEvent(Map<String, String> headers,
                                           Profile input,
                                           int instance) throws Exception {
        // input is already deserialized to Profile by the framework
        return new ProfileConfirmation(input);
    }
}
```

### LambdaFunction — raw body

```java
@PreLoad(route = "hello.passthrough", instances = 5)
public class HelloPassthrough implements LambdaFunction {

    @Override
    public Object handleEvent(Map<String, String> headers,
                              Object body,
                              int instance) throws Exception {
        return body;  // echo the raw body back unchanged
    }
}
```

### Kernel thread — CPU-intensive or blocking legacy I/O

```java
@KernelThreadRunner
@PreLoad(route = "legacy.blocking.call", instances = 5)
public class LegacyBlockingFunction implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    // instances should be small (kernel threads are limited; default pool = 100)
    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers,
                                           Map<String, Object> input,
                                           int instance) throws Exception {
        // may do synchronous blocking I/O here
        return result;
    }
}
```

### Event interceptor — access the raw EventEnvelope

```java
@EventInterceptor
@PreLoad(route = "my.interceptor", instances = 5)
public class MyInterceptor implements LambdaFunction {
    @Override
    public Object handleEvent(Map<String, String> headers,
                              Object body,    // body IS the original EventEnvelope
                              int instance) throws Exception {
        EventEnvelope original = (EventEnvelope) body;
        // forward, log, or transform — return value is ignored
        return null;
    }
}
```

---

## Calling another function (PostOffice) {#postoffice}

### Synchronous RPC (virtual-thread — recommended)

```java
var po = new PostOffice(headers, instance);   // always construct inside handleEvent

EventEnvelope response = po.request(
    new EventEnvelope().setTo("target.route").setBody(Map.of("key", "value")),
    5000   // timeout in milliseconds
).get();  // .get() suspends the virtual thread — does NOT block a kernel thread

if (response.hasError()) {
    throw new AppException(response.getStatus(), response.getError().toString());
}
var result = (Map<String, Object>) response.getBody();
```

### Fire-and-forget

```java
var po = new PostOffice(headers, instance);
po.send(new EventEnvelope().setTo("target.route").setBody(Map.of("key", "value")));
```

### Parallel RPC (multiple targets)

```java
var po = new PostOffice(headers, instance);
List<EventEnvelope> requests = List.of(
    new EventEnvelope().setTo("service.a").setBody(bodyA),
    new EventEnvelope().setTo("service.b").setBody(bodyB)
);
List<EventEnvelope> responses = po.request(requests, 5000).get();
```

### Fan-out: multicast (local) vs broadcast (service mesh)

Mercury has two fan-out mechanisms with **completely different scopes**. Do not confuse them.

| | Multicast | Broadcast |
|:---|:---|:---|
| **Scope** | Single JVM — local event bus only | All pods / containers in the service mesh |
| **How enabled** | `multicast.yaml` config — no API call | `po.broadcast()` API call |
| **Requires** | Nothing extra | `cloud.connector=kafka` + presence-monitor |
| **Fallback without mesh** | N/A (always local) | Degrades to unicast on local instance |

**Multicast** — configure in `src/main/resources/multicast.yaml`, then send to the source route normally:

```yaml
# multicast.yaml — fan out order.placed to three local subscribers
multicast:
  - source: "order.placed"
    targets:
      - "inventory.handler"
      - "notification.handler"
      - "audit.handler"
```

```properties
# application.properties
yaml.multicast=classpath:/multicast.yaml
```

Callers just `po.send("order.placed", body)` — the framework intercepts and relays to all targets. No API change.

**Broadcast** — delivers to every instance of the route **across all pods** in the Kafka service mesh:

```java
var po = new PostOffice(headers, instance);
po.broadcast(new EventEnvelope().setTo("target.route").setBody(payload));
// Only effective with cloud.connector=kafka + presence-monitor running.
// Without a service mesh it degrades to unicast on the local instance.
```

> **Do not use `po.broadcast()` for local fan-out.** Use `multicast.yaml` for that.

**PostOffice rules:**
- Create inside `handleEvent` — it carries per-request trace context from `headers`.
- Do not cache it as a field.
- `request().get()` is safe on a virtual thread. Use `@KernelThreadRunner` if the function itself
  must be on a kernel thread, but the PostOffice RPC is still virtual-thread-safe.

---

## Serialization gotchas {#serialization}

| Scenario | Pitfall | Fix |
|:---|:---|:---|
| Small `Long` value in a `Map` body | MsgPack downcasts it to `Integer` on the wire | Use a PoJo to enforce the type, or `Utility.str2long(String)` for safe conversion |
| Integer value in a `Map` from Gson | Customized Gson returns it as `Long` | Use `Utility.str2int(String)` / `str2long(String)` |
| Integer map key | Auto-converted to `String` on serialization | Always use `String` map keys |
| `List` as input type with key-by-key mapping | Cannot be mapped field-by-field | Use `*` passthrough in Event Script + `inputPojoClass` |

```java
// Safe numeric conversions
Utility util = Utility.getInstance();
int n  = util.str2int(headers.get("x-count"));    // returns -1 if null/invalid
long t = util.str2long(String.valueOf(map.get("amount")));  // returns -1 if null/invalid
```

---

## Worked example — full function + HTTP wiring {#example}

```java
// 1. The function
@PreLoad(route = "greeting.function", instances = 10)
public class GreetingFunction implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers,
                                           Map<String, Object> input,
                                           int instance) throws Exception {
        var name = input.getOrDefault("name", "world").toString();
        return Map.of("greeting", "Hello, " + name + "!");
    }
}
```

```yaml
# 2. Wire to HTTP in rest.yaml
rest:
  - service: "greeting.function"
    methods: ['GET', 'POST']
    url: "/api/greeting"
    timeout: 10s
```

```bash
# 3. Test
curl -s -X POST http://127.0.0.1:8085/api/greeting \
     -H "content-type: application/json" \
     -d '{"name": "Mercury"}'
# → {"greeting": "Hello, Mercury!"}
```

---

## See also {#see-also}

- [Write your first function](write-your-first-function.md) — step-by-step tutorial with PostOffice call.
- [Function Execution Strategies](function-execution.md) — virtual vs kernel threads, Mono/Flux in depth.
- [Annotations Reference](../annotations-reference.md) — complete `@PreLoad`, `@MainApplication`, and other annotations.
- [API Overview](../api-overview.md) — `PostOffice`, `Platform`, `EventEnvelope` API reference.
- [Event Script AI agent guide](../event-script/ai-agent-guide.md) — how to orchestrate this function as a flow task.
- [REST automation AI agent guide](../rest-automation/ai-agent-guide.md) — how to wire it to HTTP in rest.yaml.
