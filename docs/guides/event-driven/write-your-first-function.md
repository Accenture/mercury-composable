---
title: Write your first function
summary: Step-by-step — define a composable function, expose it over HTTP via rest.yaml, call it
  from another function with PostOffice, and run it end-to-end.
layer: platform-core
audience: [developer]
keywords: [function, PreLoad, TypedLambdaFunction, LambdaFunction, rest.yaml, PostOffice, route name]
related:
  - guides/event-driven/function-execution.md
  - guides/rest-automation/index.md
  - guides/event-script/index.md
---

# Write your first function

*Tutorial: build a working composable function from scratch, wire it to HTTP, and call it
programmatically.*

> **At a glance**
>
> - **What** — write a `@PreLoad`-annotated class, expose it over HTTP in `rest.yaml`, and call it
>   from another function via `PostOffice`.
> - **Why it matters** — every higher-layer concept (flow task, graph skill) is still this same atom;
>   mastering Layer 1 is the foundation.
> - **For** developers new to Mercury or building their first composable feature.

A composable function is **plain Java** — no framework-specific base class, no dependency injection
container, no coupling to other functions. You pick the coding style that suits the problem:
sequential, object-oriented, or reactive. The framework constrains only *coupling* (functions
communicate through named routes and `EventEnvelope`, never by direct call).

---

## 1. Define the function

Create a class that implements `TypedLambdaFunction<I, O>` and annotate it with `@PreLoad`.

```java
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

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

**Three things to notice:**

| Part | What it does |
|:---|:---|
| `@PreLoad(route = "hello.function")` | Registers the function on the event bus under this name. Other components reach it *only* by this string. |
| `instances = 10` | How many concurrent workers the platform starts for this function. |
| `TypedLambdaFunction<I, O>` | Typed input/output. Use `Map<String, Object>` for flexible key-value data, or a PoJo for a strict interface contract. |

For an **untyped** function that works directly with the raw `EventEnvelope` (useful for
pass-through or routing logic), implement `LambdaFunction` instead:

```java
@PreLoad(route = "hello.passthrough", instances = 5)
public class HelloPassthrough implements LambdaFunction {

    @Override
    public Object handleEvent(Map<String, String> headers,
                              Object body,
                              int instance) throws Exception {
        return body; // echo the raw body back
    }
}
```

---

## 2. Expose it over HTTP

Open (or create) `src/main/resources/rest.yaml` and add an entry under the `rest:` section:

```yaml
rest:
  - service: "hello.function"
    methods: ['GET', 'POST']
    url: "/api/hello"
    timeout: 10s
```

The `service:` value is the route name from `@PreLoad`. That string is the *only* link between
the HTTP layer and your function — no import, no wiring code.

Enable REST automation in `application.properties`:

```properties
rest.server.port=8085
rest.automation=true
yaml.rest.automation=classpath:/rest.yaml
```

---

## 3. Call it from another function

Inside any other function, use `PostOffice` to send a request to `hello.function` by route name:

```java
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.models.EventEnvelope;

// inside handleEvent(...)
var po = new PostOffice(headers, instance);
EventEnvelope response = po.request(
    new EventEnvelope().setTo("hello.function").setBody(Map.of("name", "Mercury")),
    5000   // timeout in ms
).get();

var result = response.getBody(); // Map containing {"message": "Hello, Mercury!"}
```

`po.request(...).get()` reads as synchronous but never blocks a platform thread — the virtual
thread is parked while waiting, so you get sequential code at reactive throughput.

---

## 4. Run and test

Start the application (adjust the version to match your build):

```bash
cd examples/composable-example
java -jar target/composable-example-4.6.0.jar
```

Test with `curl`:

```bash
# GET — query parameter mapped automatically
curl "http://127.0.0.1:8085/api/hello?name=World"

# POST — JSON body
curl -s -X POST http://127.0.0.1:8085/api/hello \
     -H "content-type: application/json" \
     -d '{"name": "Mercury"}'
```

Expected response:

```json
{"message": "Hello, Mercury!"}
```

---

## 5. Add it to a flow (optional next step)

Once the function works standalone, you can orchestrate it as a **task** in an Event Script flow
without changing a single line of its code:

```yaml
# flows/hello-flow.yml
tasks:
  - name: greet
    input:
      - input.body.name -> name
    process: "hello.function"
    output:
      - result.message -> model.greeting
    execution: end
```

The route name `hello.function` is the only connection. See
[Composable Orchestration](../event-script/index.md) for the full flow model.

---

## See also

- [Function Execution Strategies](function-execution.md) — virtual vs. kernel threads, `Mono`/`Flux`,
  authentication functions, serialization.
- [REST Automation](../rest-automation/index.md) — full `rest.yaml` reference: CORS, headers,
  authentication, static content.
- [Event Script Syntax](../event-script/syntax.md) — orchestrate this function as a flow task.
- [API Overview](../api-overview.md) — `PostOffice`, `Platform`, `EventEnvelope` API reference.
