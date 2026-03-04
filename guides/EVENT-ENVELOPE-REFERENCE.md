# Event Envelope Reference

*Reference: Complete API reference for `EventEnvelope` and `PostOffice`.*

`EventEnvelope` is the universal message container in Mercury Composable. Every event that
travels through the event bus — whether triggered by an HTTP request, a flow step, or a direct
`PostOffice` call — is carried inside an `EventEnvelope`. Understanding its structure is
prerequisite for advanced function patterns, error handling, and distributed tracing.

**Package:** `org.platformlambda.core.models`

---

## Structure overview

An envelope has three distinct, independent parts:

| Part | Java type | Purpose |
|------|-----------|---------|
| **Body** | `Object` (PoJo, `Map<String,Object>`, Java primitive, `byte[]`) | The event payload. Serialized as MsgPack on the event bus; JSON at HTTP boundaries. |
| **Headers** | `Map<String, String>` | User-defined key-value parameters passed directly to `handleEvent`. Keys and values are always strings. |
| **Metadata** | Various fields | Routing address, trace ID, correlation ID, status code, execution timing, exception state. Managed by the framework; user code reads but rarely writes. |

The framework serializes and deserializes the body automatically. User code never calls
MsgPack directly.

---

## Creating an EventEnvelope

### Constructors

```java
// Default constructor — assigns a random UUID as the envelope ID
EventEnvelope event = new EventEnvelope();

// Deserialize from a previously serialized byte array
EventEnvelope event = new EventEnvelope(byte[] bytes);

// Reconstruct from a map representation (e.g. from toMap())
EventEnvelope event = new EventEnvelope(Map<String, Object> map);
```

### Static factories

```java
// Equivalent to new EventEnvelope()
EventEnvelope event = EventEnvelope.of();

// Equivalent to new EventEnvelope(byte[])
EventEnvelope event = EventEnvelope.of(byte[] bytes);

// Equivalent to new EventEnvelope(Map)
EventEnvelope event = EventEnvelope.of(Map<String, Object> map);
```

### Builder pattern

All `set*` methods return `this`, enabling fluent construction:

```java
EventEnvelope event = new EventEnvelope()
    .setTo("v1.get.profile")
    .setHeader("profile_id", "100")
    .setHeader("tenant", "acme")
    .setBody(requestMap)
    .setCorrelationId("req-abc-123");
```

---

## Body methods

The body is the event payload. The framework maps it to the typed `input` argument of
`handleEvent` and wraps the return value into the response envelope automatically.

| Method | Return type | Description |
|--------|-------------|-------------|
| `getBody()` | `Object` | Returns the deserialized body. Type depends on what the sender set. Typically `Map<String,Object>` or a PoJo. |
| `getBody(Class<T> toValueType)` | `T` | Deserializes the body to the given PoJo class using the framework's registered serializer. |
| `getBody(Class<T> toValueType, Class<?>... parameterClass)` | `T` | Deserializes a generic type (e.g. `List<Profile>`). Pass the container class and the element class(es). |
| `getBodyAsListOfPoJo(Class<T> toValueType)` | `List<T>` | Convenience method to deserialize a body that is a JSON array into a typed list. |
| `getRawBody()` | `Object` | Returns the body before any deserialization. May be a `byte[]`, `Map`, or primitive depending on transport. |
| `getOriginalBody()` | `Object` | Returns the body exactly as set by the sender, without any intermediate serialization. Useful for debugging. |
| `setBody(Object body)` | `EventEnvelope` | Sets the body. Accepts any PoJo, `Map<String,Object>`, Java primitive, or `byte[]`. Returns `this`. |

### Declaring EventEnvelope as the input type

To access the full envelope (including metadata) inside a function, declare `EventEnvelope`
as the input type in the `TypedLambdaFunction` signature:

```java
@PreLoad(route = "v1.inspect.event")
public class InspectEvent implements TypedLambdaFunction<EventEnvelope, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers,
                                           EventEnvelope input, int instance) throws Exception {
        String traceId = input.getTraceId();
        String correlationId = input.getCorrelationId();
        Object body = input.getBody();
        return Map.of("trace", traceId, "body", body);
    }
}
```

This pattern is also required when a function is annotated with
[`@EventInterceptor`](ANNOTATIONS-REFERENCE.md#eventinterceptor).

---

## Header methods

Headers are `String → String` parameters attached to an event. They are delivered directly
to the `headers` argument of `handleEvent` — they are not HTTP headers unless the function
is a REST endpoint handler.

| Method | Return type | Description |
|--------|-------------|-------------|
| `getHeaders()` | `Map<String, String>` | Returns all user-defined headers as an immutable map. |
| `getHeader(String key)` | `String` | Returns the value for a single header key, or `null` if absent. |
| `setHeader(String key, Object value)` | `EventEnvelope` | Sets a header. The value is converted to `String` via `toString()`. Returns `this`. |
| `setHeaders(Map<String, String> headers)` | `EventEnvelope` | Replaces all headers with the provided map. Returns `this`. |

### Reserved internal header names

The framework injects the following keys into the `headers` Map parameter of `handleEvent`.
These keys are read-only context — do not set them on outgoing envelopes:

| Key | Value |
|-----|-------|
| `my_route` | Route name of the currently executing function |
| `my_trace_id` | Distributed trace ID for this transaction |
| `my_trace_path` | Accumulated trace path through the call chain |

Access these inside a function to preserve the trace when constructing a `PostOffice`:

```java
PostOffice po = PostOffice.trackable(headers, instance);
```

---

## Routing and metadata methods

| Method | Return type | Description |
|--------|-------------|-------------|
| `getId()` | `String` | The envelope's unique ID. Assigned automatically on construction (UUID). |
| `setId(String id)` | `EventEnvelope` | Override the auto-assigned ID. Rarely needed. Returns `this`. |
| `getTo()` | `String` | The destination route. Set by the sender or by the flow engine. |
| `setTo(String to)` | `EventEnvelope` | Set the destination route for this event. Returns `this`. |
| `getFrom()` | `String` | The originating route. Populated automatically by `PostOffice`. |
| `setFrom(String from)` | `EventEnvelope` | Overrides the sender identity. Returns `this`. |
| `getReplyTo()` | `String` | The route to which the response should be sent (used in async patterns). |
| `setReplyTo(String replyTo)` | `EventEnvelope` | Sets the reply-to route. Returns `this`. |
| `getTraceId()` | `String` | The distributed trace ID. Propagated across all calls in a transaction. |
| `setTraceId(String traceId)` | `EventEnvelope` | Sets the trace ID. Usually set by the framework. Returns `this`. |
| `getTracePath()` | `String` | The accumulated path of all routes this event has passed through. |
| `setTracePath(String tracePath)` | `EventEnvelope` | Sets the trace path. Returns `this`. |
| `setTrace(String traceId, String tracePath)` | `EventEnvelope` | Sets both trace ID and trace path in one call. Returns `this`. |
| `getCorrelationId()` | `String` | A caller-assigned correlation ID for matching responses to requests. |
| `setCorrelationId(String cid)` | `EventEnvelope` | Sets the correlation ID. Returns `this`. |

### Fields set automatically by PostOffice

When an event is sent via `PostOffice`, the following fields are populated automatically
from the sending function's context. User code should not override them:

| Field | Source |
|-------|--------|
| `from` | Current function's route (from `my_route` header) |
| `traceId` | Current transaction's trace ID (from `my_trace_id` header) |
| `tracePath` | Current trace path (from `my_trace_path` header) |

---

## Status and error methods

Envelope status codes follow the HTTP convention: 200 means success; 400–599 means error.
The `AppException` class maps Java exceptions to HTTP-compatible status codes.

| Method | Return type | Description |
|--------|-------------|-------------|
| `getStatus()` | `int` | HTTP-compatible status code. Default is `200`. |
| `setStatus(int status)` | `EventEnvelope` | Override the status code. Returns `this`. |
| `hasError()` | `boolean` | Returns `true` if `getStatus() >= 400`. |
| `getError()` | `Object` | Returns the error payload. When set by `setException()`, this is the exception message string. |
| `getException()` | `Throwable` | Returns the attached exception, or `null` if none. |
| `setException(Throwable ex)` | `EventEnvelope` | Attaches an exception and sets the status code from it. Returns `this`. |
| `isException()` | `boolean` | Returns `true` if a stack trace is present in the envelope. |
| `getStackTrace()` | `String` | Returns the stack trace string, or `null` if none. |
| `setStackTrace(String trace)` | `EventEnvelope` | Sets the stack trace string. **Reserved for test use only.** Returns `this`. |

### AppException status mapping

When `setException(Throwable ex)` is called, the framework determines the status code:

| Exception type | Resulting status |
|---------------|-----------------|
| `AppException` | `appException.getStatus()` (the value you passed to the constructor) |
| `IllegalArgumentException` | `400` |
| Any other `Throwable` | `500` |

Throw `AppException` from user functions to return structured error responses:

```java
throw new AppException(404, "Profile not found");
throw new AppException(422, "Invalid input: email is required");
throw new AppException(403, "Access denied", originalException);
```

### Error detection pattern

```java
EventEnvelope response = po.request(event, 5000).get();
if (response.hasError()) {
    throw new AppException(response.getStatus(),
        response.getError() != null ? response.getError().toString() : "Unknown error");
}
Profile profile = response.getBody(Profile.class);
```

---

## Tags

Tags are `String → String` metadata attached to an event for internal tracking and routing
decisions. They are distinct from user headers: tags are not delivered to `handleEvent`.

| Method | Return type | Description |
|--------|-------------|-------------|
| `getTags()` | `Map<String, String>` | Returns all tags. |
| `setTags(Map<String, String> tags)` | `EventEnvelope` | Replaces all tags. Returns `this`. |
| `addTag(String key)` | `EventEnvelope` | Adds a tag with an empty string value. Returns `this`. |
| `addTag(String key, Object value)` | `EventEnvelope` | Adds a tag. Value is converted to `String`. Returns `this`. |
| `removeTag(String key)` | `EventEnvelope` | Removes a tag by key. Returns `this`. |
| `getTag(String key)` | `String` | Returns the value of a single tag, or `null` if absent. |

---

## Annotations

Annotations carry arbitrary `Object` values for advanced routing and framework use cases.
They are not serialized over the event bus and are not delivered to user functions.

| Method | Return type | Description |
|--------|-------------|-------------|
| `getAnnotations()` | `Map<String, Object>` | Returns all annotations. |
| `setAnnotations(Map<String, Object> annotations)` | `EventEnvelope` | Replaces all annotations. Returns `this`. |
| `clearAnnotations()` | `EventEnvelope` | Removes all annotations. Returns `this`. |

---

## Type and serialization methods

These methods expose the body's type information and support low-level serialization. User
code rarely needs them directly; they are primarily used by the framework and adapters.

| Method | Return type | Description |
|--------|-------------|-------------|
| `getType()` | `String` | Fully qualified class name of the body object. |
| `getSimpleType()` | `String` | Simple class name of the body object (no package). |
| `setType(String type)` | `EventEnvelope` | Override the recorded type. Returns `this`. |
| `isBinary()` | `boolean` | Returns `true` if the body is a raw `byte[]`. |
| `setBinary(boolean binary)` | `EventEnvelope` | Sets the binary flag. Returns `this`. |
| `isOptional()` | `boolean` | Returns `true` if the body is `null` and the envelope is an intentional empty response. |
| `toBytes()` | `byte[]` | Serializes the entire envelope to MsgPack bytes for transport. |
| `load(byte[] bytes)` | `void` | Populates this envelope from a previously serialized byte array in place. |
| `toMap()` | `Map<String, Object>` | Converts the envelope to a plain Java map. Useful for logging or testing. |
| `fromMap(Map<String, Object> map)` | `void` | Populates this envelope from a map representation in place. |

---

## Performance metadata (read-only)

These fields are populated by the framework after an event is processed. User code should
read them for observability but must not set them.

| Method | Return type | Description |
|--------|-------------|-------------|
| `getExecutionTime()` | `float` | Milliseconds spent executing the target function. Set by the framework after processing. |
| `getRoundTrip()` | `float` | Total round-trip time in milliseconds from send to response. Set by the framework. |
| `getBroadcastLevel()` | `int` | Broadcast depth level. Used internally for broadcast routing. |

---

## Utility

| Method | Return type | Description |
|--------|-------------|-------------|
| `copy()` | `EventEnvelope` | Creates a deep copy of this envelope, including all headers, body, and metadata. Useful when you need to modify an envelope without affecting the original. |

---

## Relationship to Event Script data mapping

Event Script flows operate on envelopes internally, but the data mapping DSL exposes only
specific namespaces. Understanding the mapping between envelope fields and DSL namespaces
helps when debugging flows.

| DSL namespace | Envelope source |
|---------------|----------------|
| `input.body` | Incoming request envelope body |
| `input.header.<name>` | Incoming request HTTP header (HTTP flows) |
| `input.path_parameter.<name>` | URL path parameter extracted by REST automation |
| `input.query.<name>` | URL query parameter |
| `input.method` | HTTP method |
| `input.uri` | Request URI |
| `input.session` | Session key-values from the authentication function |
| `result` | The entire return value of the most recently completed task |
| `result.<key>` | A specific field from the task's return value |
| `model.<key>` | Per-transaction state machine (readable and writable across all tasks) |
| `output.body` | Sets the response envelope body |
| `output.header.<name>` | Sets a response header |
| `output.status` | Sets the HTTP response status code |
| `header.<name>` | Sets a key-value in the next function's `headers` argument |
| `error.status` | Status code when an exception handler task is invoked |
| `error.message` | Error message when an exception handler task is invoked |
| `error.task` | Route of the task that threw the exception |
| `error.stack` | Stack trace when an exception handler task is invoked |

For the complete data mapping syntax and all literal value constructors (`text()`, `int()`,
`map()`, etc.), see the [Flow Configuration Schema](FLOW-SCHEMA-REFERENCE.md).

---

## PostOffice quick reference

`PostOffice` is the messaging client. Always obtain it inside `handleEvent` to preserve
the distributed trace:

```java
PostOffice po = PostOffice.trackable(headers, instance);
```

### Constructors and static factories

```java
// Production constructors
PostOffice(Map<String, String> headers, int instance)
PostOffice(Map<String, String> headers, int instance, CustomSerializer serializer)

// Static factories (preferred)
PostOffice.trackable(Map<String, String> headers, int instance)
PostOffice.withSerializer(Map<String, String> headers, int instance, CustomSerializer serializer)

// Test constructors (no real function context available in unit tests)
PostOffice.trackable(String myRoute, String myTraceId, String myTracePath)
PostOffice.withSerializer(String myRoute, String myTraceId, String myTracePath, CustomSerializer serializer)
```

### Fire-and-forget

```java
// Send with headers only (no body)
po.send(String to, Kv... parameters)

// Send with body only
po.send(String to, Object body)

// Send with body and headers
po.send(String to, Object body, Kv... parameters)

// Send a pre-built envelope
po.send(EventEnvelope event)

// Schedule delivery at a future time; returns a cancellation token
String token = po.sendLater(EventEnvelope event, Date future)
```

### Broadcast

Delivers to all registered instances of the target function:

```java
po.broadcast(String to, Kv... parameters)
po.broadcast(String to, Object body)
po.broadcast(String to, Object body, Kv... parameters)
po.broadcast(EventEnvelope event)
```

### Request-Reply: three Future types

Choose the Future type based on your execution context:

| Method | Return type | Use when |
|--------|-------------|----------|
| `request()` | `java.util.concurrent.Future<EventEnvelope>` | Inside virtual threads — `.get()` suspends non-destructively |
| `asyncRequest()` | `io.vertx.core.Future<EventEnvelope>` | Inside reactive or Vert.x code |
| `eRequest()` | `java.util.concurrent.CompletableFuture<EventEnvelope>` | When composing with `CompletableFuture` chains |

```java
// Virtual thread — sequential, readable, non-blocking
EventEnvelope response = po.request(event, 5000).get();

// Async Vert.x future
po.asyncRequest(event, 5000).onSuccess(response -> { ... });

// CompletableFuture
po.eRequest(event, 5000).thenAccept(response -> { ... });
```

### Parallel requests (fork-n-join)

```java
List<EventEnvelope> requests = List.of(eventA, eventB, eventC);
List<EventEnvelope> responses = po.request(requests, 5000).get();
// responses[i] corresponds to requests[i]
```

### Event-over-HTTP (cross-instance)

```java
// Route to a function in a peer application instance
EventEnvelope response = po.request(event, 5000,
    Map.of("X-App-Instance", "peer-instance-id"),
    "http://peer-service/api/event",
    true  // true = RPC, false = fire-and-forget
).get();
```

### Service discovery and trace utilities

```java
// Check if a function is available (local or mesh)
boolean available = po.exists("v1.some.service");

// Add custom data to the distributed trace
po.annotateTrace("user_id", "A12345");
po.annotateTrace("request_context", Map.of("tenant", "acme"));

// Access current trace info
String traceId = po.getTraceId();
String tracePath = po.getTracePath();
```

### `Kv` helper class

`Kv` is a typed key-value pair for passing headers in PostOffice convenience methods:

```java
// Package: org.platformlambda.core.models
Kv kv = new Kv(String key, Object value);  // value is converted to String

// Usage
po.send("v1.notify.user", new Kv("user_id", "100"), new Kv("action", "welcome"));
```

---

## Common usage patterns

### Pattern 1: Simple RPC call from inside a function

```java
@PreLoad(route = "v1.create.profile")
public class CreateProfile implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers,
                                           Map<String, Object> input, int instance) throws Exception {
        PostOffice po = PostOffice.trackable(headers, instance);

        EventEnvelope request = new EventEnvelope()
            .setTo("v1.validate.input")
            .setBody(input);

        EventEnvelope response = po.request(request, 5000).get();

        if (response.hasError()) {
            throw new AppException(response.getStatus(), response.getError().toString());
        }

        return response.getBody(Map.class);
    }
}
```

### Pattern 2: Parallel requests

```java
PostOffice po = PostOffice.trackable(headers, instance);

EventEnvelope reqA = new EventEnvelope().setTo("v1.lookup.user").setHeader("id", userId);
EventEnvelope reqB = new EventEnvelope().setTo("v1.lookup.account").setHeader("id", accountId);

List<EventEnvelope> responses = po.request(List.of(reqA, reqB), 5000).get();
Map<String, Object> user = responses.get(0).getBody(Map.class);
Map<String, Object> account = responses.get(1).getBody(Map.class);
```

### Pattern 3: Authentication function returning session variables

An authentication function can attach session variables to the approved request by returning
an `EventEnvelope` with `body = true` and additional headers:

```java
@PreLoad(route = "v1.auth.validator")
public class AuthValidator implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers,
                                     AsyncHttpRequest input, int instance) throws Exception {
        String token = input.getHeader("Authorization");
        if (!validateToken(token)) {
            throw new AppException(401, "Invalid token");
        }
        String userId = extractUserId(token);
        // Approve and inject session variable accessible as input.session.user_id in flows
        return new EventEnvelope()
            .setHeader("user_id", userId)
            .setBody(true);
    }
}
```

### Pattern 4: Inspecting envelope metadata in an EventInterceptor

```java
@PreLoad(route = "v1.audit.interceptor")
@EventInterceptor
public class AuditInterceptor implements TypedLambdaFunction<EventEnvelope, Void> {

    @Override
    public Void handleEvent(Map<String, String> headers,
                            EventEnvelope input, int instance) throws Exception {
        log.info("Event from={} to={} traceId={} bodyType={}",
            input.getFrom(), input.getTo(),
            input.getTraceId(), input.getSimpleType());
        return null;  // return value is ignored for @EventInterceptor
    }
}
```

---

## Reserved HTTP headers

These header names are reserved by the framework and must not be used as custom headers
in user code when operating at the HTTP boundary:

| Header | Purpose |
|--------|---------|
| `X-Stream-Id` | Stream ID for Flux streaming responses |
| `X-TTL` | Time-to-live for Flux stream consumers |
| `X-Trace-Id` | Distributed trace identifier |
| `X-Correlation-Id` | Caller-assigned correlation ID |
| `X-Flow-Id` | Active flow ID |
| `X-App-Instance` | Target application instance for Event-over-HTTP |
| `X-Async` | Marks an event as fire-and-forget over HTTP |
| `X-Event-Api` | Marks a request as an Event API call |
| `X-Content-Length` | Content length for streaming responses |
| `X-Small-Payload-As-Bytes` | Hint to treat small payloads as binary |
| `X-Raw-Xml` | Hint to pass XML body as-is without parsing |

---

## See also

- [Architecture Overview](ARCHITECTURE.md) — Event Envelope structure and role in the system
- [Annotations Reference](ANNOTATIONS-REFERENCE.md) — `@EventInterceptor`, `@PreLoad`, and other annotations
- [Flow Configuration Schema](FLOW-SCHEMA-REFERENCE.md) — Complete data mapping DSL reference
- [Event Script Syntax](CHAPTER-4.md) — Flow DSL including input/output mapping and execution types
- [Function Execution Strategies](CHAPTER-2.md) — Virtual threads, auth functions, Flux/Mono return types
- [API Overview](CHAPTER-9.md) — Full `PostOffice`, `Platform`, and configuration API
