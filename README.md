# Mercury Composable

A Java framework for building composable, event-driven applications from self-contained functions wired together by YAML-configured event flows.

[![License](https://img.shields.io/github/license/Accenture/mercury-composable)](LICENSE) [![Java 21+](https://img.shields.io/badge/Java-21%2B-blue)](https://openjdk.org/projects/jdk/21/) [![Last Commit](https://img.shields.io/github/last-commit/Accenture/mercury-composable)](https://github.com/Accenture/mercury-composable/commits/main) [![GitHub Stars](https://img.shields.io/github/stars/Accenture/mercury-composable)](https://github.com/Accenture/mercury-composable/stargazers)

> **New to Mercury Composable?** Jump to the [Quickstart](https://accenture.github.io/mercury-composable/guides/QUICKSTART/) to get a working app running in under 5 minutes.

## What is Mercury Composable?

Mercury Composable is a software development toolkit for building composable, event-driven Java applications.
Each application is assembled from independent functions — plain Java classes with no direct knowledge of each
other — that communicate exclusively through events. Event flows are defined in YAML configuration files, so
orchestration logic is configuration rather than code. The framework uses Java 21 virtual threads throughout,
making it suited for high-concurrency microservices and serverless deployments. Because each function is
self-contained with immutable I/O, the design is optimized for both human developers and AI code assistants.

## Quick Start

**[5-Minute Quickstart](https://accenture.github.io/mercury-composable/guides/QUICKSTART/)**
— Clone, build, and run your first composable app.

### Prerequisites

- Java 21 or higher
- Maven 3.9.7 or higher

### Build from source

```shell
git clone https://github.com/Accenture/mercury-composable.git
cd mercury-composable
mvn clean install
```

### Run the example application

```shell
cd examples/composable-example
java -jar target/composable-example-4.3.69.jar
```

### Try it out

```bash
# Create a profile
curl -X POST http://127.0.0.1:8100/api/profile \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"id": 100, "name": "Hello World", "address": "100 World Blvd", "telephone": "123-456-7890"}'

# Retrieve the profile
curl http://127.0.0.1:8100/api/profile/100
```

## How It Works

A composable application is designed in 3 steps:

1. **Describe** your use case as an event flow diagram
2. **Configure** the event flow in a YAML file
3. **Implement** each function as a self-contained unit

The YAML flow below defines a single-task "greetings" endpoint. The route name `greeting.demo` is the only
connection between the flow configuration and the Java function:

```yaml
flow:
  id: 'greetings'
  description: 'Simplest flow'
  ttl: 10s

first.task: 'greeting.demo'

tasks:
  - input:
      - 'input.path_parameter.user -> user'
    process: 'greeting.demo'
    output:
      - 'text(application/json) -> output.header.content-type'
      - 'result -> output.body'
    description: 'Hello World'
    execution: end
```

The corresponding function reads its input, builds a response, and returns it:

```java
@PreLoad(route="greeting.demo", instances=10)
public class Greetings implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        Map<String, Object> result = new HashMap<>();
        result.put("user", input.get("user"));
        result.put("message", "Welcome");
        result.put("time", new Date());
        return result;
    }
}
```

## Documentation

Full documentation is available at [https://accenture.github.io/mercury-composable/](https://accenture.github.io/mercury-composable/).

| Resource | Link |
|---|---|
| Full Documentation | https://accenture.github.io/mercury-composable/ |
| Architecture Overview | https://accenture.github.io/mercury-composable/guides/ARCHITECTURE/ |
| Methodology & Principles | https://accenture.github.io/mercury-composable/guides/METHODOLOGY/ |
| Getting Started Guide | https://accenture.github.io/mercury-composable/guides/CHAPTER-1/ |
| Event Script Syntax | https://accenture.github.io/mercury-composable/guides/CHAPTER-4/ |
| API Overview | https://accenture.github.io/mercury-composable/guides/CHAPTER-9/ |
| Application Properties Reference | https://accenture.github.io/mercury-composable/guides/APPENDIX-I/ |

## Also Available in Node.js

Mercury Composable is also available for Node.js. See
[mercury-nodejs](https://github.com/Accenture/mercury-nodejs) for the core library and
[mercury-composable-examples](https://github.com/Accenture/mercury-composable-examples) for usage examples.

## Key Concepts

**Composable Function** - A self-contained Java class implementing `LambdaFunction` or `TypedLambdaFunction`,
registered under a named route, with no direct references to other functions.

**Event Envelope** - The immutable message container used for all inter-function communication; the body is
MsgPack-serialized and headers are a `Map<String, String>`.

**Event Flow / Event Script** - A YAML configuration file that sequences named functions for a given
transaction, replacing orchestration code with configuration.

**Flow Adapter** - An entry point that converts external requests (HTTP, Kafka, serverless events) into
events and delivers them to the event manager.

**Route Name** - A string identifier used to register and address a function through the `Platform` registry
and `PostOffice`.

**State Machine** - A per-transaction key-value store (referenced in flows as `model`) that holds
intermediate results across the stateless functions in a flow.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a pull request,
and note that all contributors are expected to follow the [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
This project is licensed under the Apache 2.0 license.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
