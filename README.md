# Mercury Composable

A Java framework for building composable, event-driven applications from self-contained functions wired together by YAML-configured event flows.

> **New here?** [**Getting Started**](https://accenture.github.io/mercury-composable/guides/getting-started/) runs a working app in five minutes. **Building with an AI agent?** Start with the [**AI Developer Guide**](https://accenture.github.io/mercury-composable/guides/ai-developer-guide/).

## What is Mercury Composable?

An application is assembled from **independent functions** — plain Java classes with no knowledge of one another — that communicate only through **events**. The flows that sequence them live in **YAML**, so orchestration is configuration, not code. Everything runs on Java 21 **virtual threads**, so straightforward blocking code performs like reactive, and each function's immutable input/output makes the design equally friendly to human developers and AI code assistants.

It ascends three layers — adopt only the ones you need:

1. **Platform Core** — an event-driven foundation: functions addressed by route name, exchanging immutable event envelopes over an in-memory event bus.
2. **Event Script** — composable orchestration: a YAML flow sequences functions for a transaction, replacing orchestration code with configuration.
3. **Active Knowledge Graph** — a semantic layer where a knowledge graph *is* the application.

## A taste

Three steps: **describe** the use case as an event flow, **configure** it in YAML, **implement** each function. The route name `greeting.demo` is the *only* link between the flow and the Java class.

```yaml
flow:
  id: 'greetings'
first.task: 'greeting.demo'
tasks:
  - input:
      - 'input.path_parameter.user -> user'
    process: 'greeting.demo'
    output:
      - 'text(application/json) -> output.header.content-type'
      - 'result -> output.body'
    execution: end
```

```java
@PreLoad(route = "greeting.demo", instances = 10)
public class Greetings implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        return Map.of("user", input.get("user"), "message", "Welcome", "time", new Date());
    }
}
```

The [Getting Started](https://accenture.github.io/mercury-composable/guides/getting-started/) guide walks this end to end — clone, build, and call your first endpoint.

## Documentation

Full documentation: **[accenture.github.io/mercury-composable](https://accenture.github.io/mercury-composable/)**. AI agents can use [`docs/llms.txt`](docs/llms.txt) as a machine-readable map.

| | |
|---|---|
| **Start here** | [Home](https://accenture.github.io/mercury-composable/) · [Getting Started](https://accenture.github.io/mercury-composable/guides/getting-started/) · [AI Developer Guide](https://accenture.github.io/mercury-composable/guides/ai-developer-guide/) |
| **Layer 1 — Platform Core** | [Architecture](https://accenture.github.io/mercury-composable/guides/architecture/) · [Methodology](https://accenture.github.io/mercury-composable/guides/methodology/) · [Event-driven Foundation](https://accenture.github.io/mercury-composable/guides/event-driven/) · [REST Automation](https://accenture.github.io/mercury-composable/guides/rest-automation/) |
| **Layer 2 — Event Script** | [Overview](https://accenture.github.io/mercury-composable/guides/event-script/) · [Syntax](https://accenture.github.io/mercury-composable/guides/event-script/syntax/) · [Flow Schema](https://accenture.github.io/mercury-composable/guides/flow-schema-reference/) · [Build, Test & Deploy](https://accenture.github.io/mercury-composable/guides/build-test-deploy/) |
| **Layer 3 — Knowledge Graph** | [Knowledge Graph as Application](https://accenture.github.io/mercury-composable/guides/knowledge-graph/) |
| **Operate & integrate** | [Observability](https://accenture.github.io/mercury-composable/guides/observability/) · [Spring Boot](https://accenture.github.io/mercury-composable/guides/spring-boot/) · [Minimalist Kafka](https://accenture.github.io/mercury-composable/guides/minimalist-kafka/) · [Sync-over-Async](https://accenture.github.io/mercury-composable/guides/sync-over-async/) |
| **Reference** | [API Overview](https://accenture.github.io/mercury-composable/guides/api-overview/) · [Configuration](https://accenture.github.io/mercury-composable/guides/configuration-reference/) · [Annotations](https://accenture.github.io/mercury-composable/guides/annotations-reference/) · [ADRs](https://accenture.github.io/mercury-composable/arch-decisions/ADR/) |

## Also available in Node.js

See [mercury-nodejs](https://github.com/Accenture/mercury-nodejs) for the core library and [mercury-composable-examples](https://github.com/Accenture/mercury-composable-examples) for usage examples.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and the [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before opening a pull request.

## License

Licensed under the [Apache License 2.0](https://github.com/Accenture/mercury-composable/blob/main/LICENSE).
