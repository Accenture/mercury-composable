# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Full build (all modules)
mvn clean install

# Build and install a single module
mvn clean install -f system/platform-core/pom.xml

# Run all tests
mvn test

# Run tests in a specific module
mvn test -f examples/composable-example/pom.xml

# Run a single test class
mvn test -Dtest=FlowTest -f examples/composable-example/pom.xml

# Run a single test method
mvn test -Dtest=FlowTest#endToEndFlowTest -f examples/composable-example/pom.xml

# Skip tests during build
mvn clean install -DskipTests

# Run the example app
cd examples/composable-example && java -jar target/composable-example-4.4.3.jar
```

**Requirements:** Java 21+, Maven 3.9.7+

## Architecture Overview

Mercury Composable is a framework for building event-driven applications from **self-contained functions wired by YAML-configured event flows**. Functions have no direct knowledge of each other — all coupling is through named routes and event envelopes.

### Module Layout

- `system/platform-core` — Core engine: event bus, function registry, PostOffice RPC, virtual thread integration. No Spring dependency.
- `system/event-script-engine` — Compiles and executes YAML event flow files.
- `system/rest-spring-3` / `rest-spring-4` — Spring Boot integration layers (REST adapter, autoconfig).
- `system/mini-scheduler` — Scheduled task support.
- `extensions/` — Optional add-ons: reactive PostgreSQL (R2DBC), property graph engine, API playground.
- `connectors/` — Kafka pub/sub adapters and presence monitoring for distributed deployments.
- `examples/` — Reference applications; `composable-example` is the primary demo.

### Core Abstractions

**Functions** are plain Java classes annotated with `@PreLoad(route="...", instances=N)` that implement one of:
- `LambdaFunction` — untyped, takes `EventEnvelope`
- `TypedLambdaFunction<I, O>` — typed input/output (Map or PoJo only; List of PoJo not supported)

Functions are registered by route name in the `Platform` registry and addressed exclusively by that name.

**EventEnvelope** is the immutable message container transported between functions. Headers are `Map<String, String>`; body is MsgPack-serialized.

**PostOffice** is the inter-function communication API. Use it for async RPC calls from within a function. Virtual threads make these calls appear synchronous without blocking a kernel thread.

**Event Flows** (Event Script) are YAML files that define task sequences declaratively:
```yaml
flow:
  id: 'my-flow'
  ttl: 10s
first.task: 'my.function'
tasks:
  - input:
      - 'input.path_parameter.id -> id'
    process: 'my.function'
    output:
      - 'result -> output.body'
    execution: end
```
Flows replace orchestration code entirely. The only link between a flow and a function is the **route name string**.

**flows.yaml** is a manifest that lists which flow YAML files are active for an application.

**rest.yaml** maps HTTP endpoints to flows (method, path, flow ID, CORS, timeouts).

### Serialization Behavior

- **MsgPack**: used for EventEnvelope binary transport. Small `Long` values are downcast to `Integer` during serialization — use a PoJo to enforce specific numeric types.
- **Gson** (customized): used for PoJo↔Map conversion. Integers in Maps are treated as `Long`. Use `util.str2int` / `util.str2long` from the `Utility` class for safe numeric conversion.
- Map keys must always be strings; integer keys are auto-converted.

### Application Startup

Applications implement `EntryPoint` and are annotated with `@MainApplication`. The launcher calls `AutoStart.main(args)`. Configuration is driven by `application.properties`, `rest.yaml`, and `flows.yaml` in `src/main/resources/`.

### Virtual Threads and Concurrency

The framework uses Java 21 virtual threads throughout. Sequential synchronous RPC code (via `PostOffice`) performs equivalently to reactive code. Functions can also return `Mono<T>` or `Flux<T>` for reactive pipelines.

## Key Files for New Features

When adding a new capability, the typical pattern is:
1. Write a function class implementing `TypedLambdaFunction<I, O>` with `@PreLoad`
2. Create a flow YAML in `src/main/resources/flows/`
3. Register the flow in `flows.yaml`
4. Add a REST mapping in `rest.yaml` if the flow is HTTP-facing

See `examples/composable-example` for a complete working reference (profile CRUD with encryption, exception flows, and end-to-end tests in `FlowTest.java`).
