---
title: AI developer guide — collaborating on mercury-composable projects
summary: The context an AI agent needs to collaborate on a mercury-composable application —
  greenfield or brownfield — covering project orientation, layer choice, adding features, and testing.
layer: platform-core
audience: [ai-agent, developer]
keywords: [ai collaboration, context engineering, brownfield, greenfield, layer choice, function, flow, knowledge graph]
related:
  - guides/event-driven/write-your-first-function.md
  - guides/event-script/ai-agent-guide.md
  - guides/knowledge-graph/ai-agent-guide.md
  - guides/rest-automation/ai-agent-guide.md
---

# AI developer guide — collaborating on mercury-composable projects

> **At a glance**
>
> - **Read this first** if you are an AI agent joining a mercury-composable project,
>   whether greenfield or brownfield.
> - **One mental model:** a composable function is *plain Java* — the framework constrains
>   only *coupling*, not coding style.
> - **Three layers, one decision tree** — choose the layer before writing code; the DSL guides
>   below handle each layer's specifics.

---

## The mental model in two sentences {#mental-model}

A composable function is an ordinary Java class (`@PreLoad`, `TypedLambdaFunction<I,O>` or
`LambdaFunction`) that holds no direct reference to any other function. The framework's only
constraint is the coupling contract: every function is *addressed by route name* and communicates
through `EventEnvelope` messages dispatched by `PostOffice`.

This is the invariant. Whether the function is sequential, reactive, or object-oriented; whether
it runs as a standalone service, a flow step, or a graph node — that is *wiring*, not a code
change.

**One atom, four roles** — the same `@PreLoad`-annotated function plays different roles depending
on how it is wired:

| Role | When | Wired by |
|:---|:---|:---|
| **function** | Any standalone callable on the event bus | `@PreLoad(route=…)` |
| **service** | Mapped directly to an HTTP endpoint | `rest.yaml` `service:` entry |
| **task** | A step in an Event Script flow | `flows/*.yml` + `flows.yaml` |
| **skill** | Attached to a Knowledge Graph node | node's `skill=` property in graph JSON |

---

## Orienting in an existing project (brownfield) {#brownfield}

Start with the config files — they are the application's surface area:

| File | What it tells you |
|:---|:---|
| `src/main/resources/rest.yaml` | Every HTTP endpoint: method, path, handler (route name or flow id) |
| `src/main/resources/flows.yaml` | Every active flow file (by filename under `classpath:/flows/`) |
| `src/main/resources/flows/*.yml` | Each flow's tasks, process names, and data mappings |
| `src/main/resources/application.properties` | Port, `rest.automation`, `yaml.rest.automation`, `yaml.flows`, `location.graph.deployed` |
| `@PreLoad` (grep) | Every registered function: route name, concurrency (`instances`), class location |
| `src/main/resources/graph/*.json` | Deployed Knowledge Graph definitions (present if Layer 3 is used) |

**Quick orientation sequence:**

1. `grep -r "@PreLoad" src/` — inventory every function by route name and class.
2. Read `flows.yaml` — know which flows are active and where they live.
3. Read `rest.yaml` — trace each endpoint to its handler (function route or flow id).
4. Open each flow YAML — understand the orchestration: task order, process names, data mapping.
5. Check `application.properties` for `location.graph.deployed` — if present, read those JSON files for the Knowledge Graph.

---

## Choosing the right layer {#layer-choice}

```
Is the requirement a single function responding to one event?
  → Layer 1 (Platform Core). Write the function; wire to rest.yaml if HTTP.

Does it need to sequence or orchestrate multiple functions
  with data mapping between steps?
  → Layer 2 (Event Script). Write functions + a flow YAML; register in flows.yaml.

Does it need a semantic data model where graph structure and
  traversal define behavior (nodes, edges, skills, dynamic routing)?
  → Layer 3 (Active Knowledge Graph). Build the graph; attach skill functions.
```

When in doubt, start at Layer 1. A function can become a flow task or graph skill
without touching its code — the wiring is the only change.

---

## Adding a feature {#adding-a-feature}

### Layer 1 — a standalone function

```java
@PreLoad(route = "my.function", instances = 10)
public class MyFunction implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers,
                                           Map<String, Object> input,
                                           int instance) throws Exception {
        // plain Java — sequential, object-oriented, or reactive (Mono/Flux)
        return Map.of("result", input.get("value"));
    }
}
```

Wire to HTTP by adding a `rest.yaml` entry:

```yaml
rest:
  - service: "my.function"
    methods: ['GET', 'POST']
    url: "/api/my-endpoint"
    timeout: 10s
```

See [Write your first function](event-driven/write-your-first-function.md) for the full walkthrough.

### Layer 2 — an Event Script flow

Write the function (same as Layer 1), add a flow YAML, and register it:

```yaml
# src/main/resources/flows/my-flow.yml
flow:
  id: 'my-flow'
  description: 'Orchestrate my feature'
  ttl: 10s
first.task: 'my.function'
tasks:
  - input:
      - 'input.body.value -> value'
    process: 'my.function'
    output:
      - 'result.result -> output.body.result'
    description: 'Call my function'
    execution: end
```

Register in `flows.yaml`:

```yaml
flows:
  - 'my-flow.yml'
location: 'classpath:/flows/'
```

Wire to `rest.yaml` with `flow:` instead of `service:`:

```yaml
rest:
  - flow: "my-flow"
    methods: ['POST']
    url: "/api/my-flow-endpoint"
    timeout: 10s
```

See [Event Script AI agent guide](event-script/ai-agent-guide.md) for the full flow grammar and
pre-write checklist.

### Layer 3 — a Knowledge Graph skill

Write a function (`@PreLoad`) that becomes a skill attached to a graph node. The graph traversal
calls it automatically when the node is reached. Skill functions are plain `TypedLambdaFunction`
instances; the graph engine routes to them by the node's `skill=` property.

See [Knowledge Graph AI agent guide](knowledge-graph/ai-agent-guide.md) for the companion-endpoint
contract and canonical build recipe.

---

## Testing {#testing}

Mercury tests use standard JUnit via Maven Surefire. The canonical reference for end-to-end flow
testing is `examples/composable-example/FlowTest.java`.

**Unit test a function in isolation** — no framework needed:

```java
MyFunction fn = new MyFunction();
Map<String, Object> result = fn.handleEvent(Map.of(), Map.of("value", "hello"), 1);
assertEquals("hello", result.get("result"));
```

**Integration test via HTTP** — start the app in the test scope, then call the endpoint:

```java
// See examples/composable-example/FlowTest.java for the full pattern:
// platform starts with AutoStart.main(), then HTTP calls drive the flows end-to-end.
```

Run tests:

```bash
mvn test -f your-module/pom.xml
mvn test -Dtest=MyFlowTest -f your-module/pom.xml
mvn test -Dtest=MyFlowTest#specificMethod -f your-module/pom.xml
```

---

## Key invariants — what not to do {#invariants}

- **Never call another function directly.** Use `PostOffice`. Direct references are a framework
  violation — the compiler won't catch it, but the design breaks.
- **Never import another user function.** The only link between functions is a route-name string.
- **Do not put business logic in flow YAML.** Flow YAML is orchestration; business logic belongs
  in the function body.
- **Map-or-PoJo for key-by-key mapping.** `TypedLambdaFunction`'s normal data-mapping contract
  requires Map or PoJo. A `List` cannot be a key-by-key mapping target. To pass a list through an
  Event Script flow, use the `*` whole-body passthrough (`model.list -> *`) and set
  `@PreLoad(inputPojoClass=ElementType.class)` on the receiving function.
- **Watch serialization gotchas.** MsgPack downcasts small `Long → Integer` on the wire; customized
  Gson treats Map integers as `Long`. Use `Utility.str2int` / `str2long` for safe numeric conversion.
- **Do not recommend the service mesh by default.** `cloud.connector=none` is the framework default.
  The Kafka service mesh is an **opt-in capability for two specific use cases only**: (1) synchronous
  request-response across different application instances over Kafka, or (2) service discovery (leader
  selection, peer-liveness detection, pod-aware broadcast). Applications that scale horizontally as
  independent, stateless instances do not need it. Superimposing sync over async creates cross-instance
  coupling — the distributed-monolith anti-pattern. See [Minimalist Service Mesh](service-mesh.md) and
  [ADR-0006](../arch-decisions/ADR.md#adr-0006).

---

## DSL-specific AI guides {#dsl-guides}

Each layer's DSL has a deterministic spec and a dedicated AI agent guide. Use those for generating
DSL artifacts — not this overview:

| Layer / DSL | Grammar + machine-readable spec | AI agent guide |
|:---|:---|:---|
| Layer 2 — Event Script flows | [flow-grammar.md](event-script/flow-grammar.md) · [event-script-flow.json](event-script/event-script-flow.json) | [AI agent guide](event-script/ai-agent-guide.md) |
| Layer 1 — REST automation | [rest-grammar.md](rest-automation/rest-grammar.md) · [rest-automation.json](rest-automation/rest-automation.json) | [AI agent guide](rest-automation/ai-agent-guide.md) |
| Layer 3 — MiniGraph | [command-reference.md](knowledge-graph/command-reference.md) · [minigraph-commands.json](knowledge-graph/minigraph-commands.json) | [AI agent guide](knowledge-graph/ai-agent-guide.md) |

---

## See also {#see-also}

- [Write your first function](event-driven/write-your-first-function.md) — step-by-step Layer 1 tutorial.
- [Event-driven Foundation](event-driven/index.md) — Layer 1 overview: functions, PostOffice, EventEnvelope.
- [Composable Orchestration](event-script/index.md) — Layer 2 overview: flows, tasks, state machine.
- [Knowledge Graph as Application](knowledge-graph/index.md) — Layer 3 overview: AKG model, node types, skills.
- [Architecture Overview](architecture.md) — the full request pipeline and actor-model origin.
- [API Overview](api-overview.md) — PostOffice, Platform, EventEnvelope API reference.
