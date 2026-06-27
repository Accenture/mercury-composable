# Agent Instructions — mercury-composable

## What This Project Is

Mercury Composable is a Java framework for building composable, event-driven
applications from **self-contained functions wired by YAML-configured event
flows**. Functions have no direct knowledge of each other — all coupling is
through named routes and event envelopes, so orchestration is configuration,
not code. Uses Java 21 virtual threads throughout, making sequential synchronous
RPC perform on par with reactive code. Also available for Node.js (separate repo).

**Type:** Multi-module framework / SDK (Maven reactor)
**Primary language:** Java 21 (one Kotlin example module)
**Coordinates:** `com.accenture.mercury:parent-mercury` (currently `4.5.0`)
**Build:** Maven 3.9.7+ — `pom.xml` source of truth for the version
**Upstream:** github.com/Accenture/mercury-composable · docs: accenture.github.io/mercury-composable

> High-level only. The precise dependency list and current versions live in
> `memory/continuity.md` → `## Stack & Tools` (the live source of truth).

## Repository Structure

Maven reactor; modules declared in the root `pom.xml`:

```
system/
  platform-core            ← core engine: event bus, function registry,
                             PostOffice RPC, virtual-thread integration (no Spring)
  event-script-engine      ← compiles & executes YAML event flows
  rest-spring-3 / -4       ← Spring Boot integration (REST adapter, autoconfig)
  mini-scheduler           ← scheduled task support
  minigraph-playground-engine
extensions/reactive-postgres   ← optional add-ons (R2DBC, graph engine, playground)
connectors/                ← Kafka pub/sub adapters + presence/service monitoring
  core/{cloud-connector, service-monitor}
  adapters/kafka/{kafka-connector, kafka-presence}
helpers/                   ← standalone dev servers, no Docker (kafka-standalone, redis-standalone)
examples/                  ← reference apps; composable-example is the primary demo
  (lambda, rest-spring-3/-4, composable, kotlin, scheduler, minigraph, kafka-demo)
benchmark/benchmark-client ← benchmark tests
docs/ (guides/, arch-decisions/)  ← docs (mkdocs `docs_dir: docs`);
                                  arch-decisions/ADR.md = the ADR ledger (holds durable design rationale)
```

## Core Abstractions

- **Functions** — plain Java classes annotated `@PreLoad(route="...", instances=N)`
  implementing either `LambdaFunction` (untyped, takes `EventEnvelope`) or
  `TypedLambdaFunction<I, O>` (typed; key-by-key data mapping requires Map or PoJo — a List cannot be
  the key-by-key mapping contract; the `*` whole-body passthrough in Event Script bypasses key-by-key
  mapping and, with `@PreLoad(inputPojoClass=…)`, enables `List<PoJo>` at the function boundary;
  Layer 1 also uses `inputPojoClass` to ingest external JSON-list payloads directly). Registered by route name in the `Platform` registry
  and addressed exclusively by that string.
- **EventEnvelope** — immutable message container between functions. Headers are
  `Map<String,String>`; body is MsgPack-serialized.
- **PostOffice** — inter-function async RPC API. Virtual threads make calls appear
  synchronous without blocking a kernel thread.
- **Event Flows (Event Script)** — YAML files defining task sequences declaratively;
  they replace orchestration code. The *only* link between a flow and a function is
  the route-name string.
- **flows.yaml** — manifest of active flow files. **rest.yaml** — maps HTTP endpoints
  to flows (method, path, flow id, CORS, timeouts).
- **State Machine** (`model` in flows) — per-transaction key-value store holding
  intermediate results across stateless functions.

## Conventions Observed

- **Adding a capability** (the standard pattern): (1) write a function implementing
  `TypedLambdaFunction<I,O>` with `@PreLoad`; (2) create a flow YAML in
  `src/main/resources/flows/`; (3) register it in `flows.yaml`; (4) add a `rest.yaml`
  mapping if HTTP-facing. See `examples/composable-example` for a complete reference
  (profile CRUD with encryption, exception flows, end-to-end tests in `FlowTest.java`).
- **Apps** implement `EntryPoint`, annotated `@MainApplication`; launcher calls
  `AutoStart.main(args)`. Config via `application.properties`, `rest.yaml`,
  `flows.yaml` in `src/main/resources/`.
- **Serialization gotchas:**
  - MsgPack downcasts small `Long` → `Integer` on the wire; use a PoJo to enforce a
    specific numeric type.
  - Customized Gson treats Integers in Maps as `Long`; use `util.str2int` /
    `util.str2long` (`Utility` class) for safe numeric conversion.
  - Map keys must be strings; integer keys are auto-converted.
- Functions can also return `Mono<T>` / `Flux<T>` for reactive pipelines.

## Build & Test Commands

```bash
mvn clean install                                   # full build (all modules)
mvn clean install -f system/platform-core/pom.xml   # build/install a single module
mvn test                                            # all tests
mvn test -f examples/composable-example/pom.xml     # tests in one module
mvn test -Dtest=FlowTest -f examples/composable-example/pom.xml          # single class
mvn test -Dtest=FlowTest#endToEndFlowTest -f examples/composable-example/pom.xml  # single method
mvn clean install -DskipTests                       # skip tests
# Run the example app (use the built artifact's actual version):
cd examples/composable-example && java -jar target/composable-example-<version>.jar
```

**Requirements:** Java 21+, Maven 3.9.7+ (`.java-version` pins the JDK).

## Core Rules

1. Never modify files outside the project scope without asking; don't reformat
   files unnecessarily.
2. Functions stay decoupled — no direct references between them. Couple only via
   route names and `EventEnvelope`.
3. When adding HTTP-facing behavior, wire it through `rest.yaml` → flow → function,
   not ad-hoc controllers.
4. Respect the serialization gotchas above when choosing types.
5. Record significant decisions in the session log and `continuity.md`.

## Testing

- JUnit via Maven Surefire. The richest end-to-end reference is
  `examples/composable-example` (`FlowTest`).
- Run a single method with `-Dtest=Class#method -f <module>/pom.xml`.

## CI / CD

- GitHub Actions under `.github/workflows/`. Additional automation dirs present:
  `.github/java-upgrade/`, `.github/modernize/`.
- Docs site built with mkdocs (`mkdocs.yml`).
