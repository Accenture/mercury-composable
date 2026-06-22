---
title: Mercury Composable
summary: A Java framework for building composable, event-driven backend applications — and,
  increasingly, Active Knowledge Graphs you evolve by editing a model rather than rewriting code.
audience: [developer, architect, business, ai-agent]
keywords: [composable, event-driven, active knowledge graph, semantic application, event script, ai collaboration]
---

# Mercury Composable for Java

Mercury Composable lets you build backend applications as **composable, event-driven systems** —
and, increasingly, as **Active Knowledge Graphs** you evolve by *editing a model* rather than
rewriting code. Functions are fully decoupled — they know each other only by route name, wired
through event envelopes — so orchestration is **configuration, not code**. It runs on Java 21
virtual threads, so straightforward synchronous code performs on par with reactive. The
event-driven core descends from the **Scala/Akka actor model**, realized on the **Eclipse Vert.x**
event bus.

> **New here?** Start with the **[5-Minute Quickstart](guides/quickstart.md)**, then
> **[Getting Started](guides/getting-started.md)**.

## A layered ascent

Mercury grew in three layers. Each builds on the one beneath it, and you can mix them in a single
application — drop down a layer exactly where you need more control, and no further.

| Layer | You express behavior as… | What you write | Start here |
|:------|:--------------------------|:---------------|:-----------|
| **Event-driven** — Platform Core | decoupled functions reacting to events | Java functions, addressed by route name | [Event-driven Foundation](guides/event-driven-foundation.md) |
| **Composable** — Event Script | YAML flows that choreograph functions | ~50% config, 50% code | [Composable Orchestration](guides/event-script/index.md) |
| **Semantic** — Active Knowledge Graph | a graph whose nodes *execute* during traversal | a model — little or no code | [Knowledge Graph as Application](guides/knowledge-graph/index.md) |

## Knowledge Graph as application

The newest layer is a paradigm shift: model business intent, enterprise knowledge, and system
behavior as **one executable [Active Knowledge Graph](guides/knowledge-graph/index.md)**. Behavior
runs as the graph is traversed, so changing what a system *does* means refining the model,
certifying it, and deploying the updated model — not rewriting and redeploying code. Humans and AI
companions co-author the same model.

## Building with an AI agent

Mercury's DSLs ship **agent-ready specifications** — a rule-based grammar plus a machine-readable
catalog — so an AI agent can generate correct artifacts *deterministically*, without inferring from
examples or reading engine source:

- [MiniGraph commands](guides/knowledge-graph/ai-agent-guide.md) — build graphs via the companion endpoint.
- [Event Script flows](guides/event-script/ai-agent-guide.md) — author flow YAML.
- [REST automation](guides/rest-automation/ai-agent-guide.md) — author `rest.yaml` endpoints.

A machine-readable map of the whole site lives at [`llms.txt`](llms.txt).

## Explore the docs

- **Quickstart & tutorials** — [Quickstart](guides/quickstart.md) · [Getting Started](guides/getting-started.md)
- **Guides** — [REST Automation](guides/rest-automation/index.md) · [Build, Test & Deploy](guides/build-test-deploy.md) ·
  [Spring Boot Integration](guides/spring-boot.md) · [Event over HTTP](guides/event-over-http.md) ·
  [Service Mesh](guides/service-mesh.md)
- **Knowledge Graph** — [Knowledge Graph as Application](guides/knowledge-graph/index.md) ·
  [Build your first graph](guides/knowledge-graph/build-your-first-graph.md) ·
  [Minimalist Property Graph](guides/knowledge-graph/property-graph.md)
- **Concepts** — [Methodology](guides/methodology.md) · [Architecture Overview](guides/architecture.md) ·
  [Event-driven Foundation](guides/event-driven-foundation.md) · [Design Notes](arch-decisions/DESIGN-NOTES.md)
- **Reference** — [Annotations](guides/annotations-reference.md) ·
  [Configuration](guides/configuration-reference.md) · [Event Envelope](guides/event-envelope-reference.md) ·
  [Flow Schema](guides/flow-schema-reference.md) · [API Overview](guides/api-overview.md)

## Project

- **Source:** [github.com/Accenture/mercury-composable](https://github.com/Accenture/mercury-composable)
- **Release notes:** [CHANGELOG](https://github.com/Accenture/mercury-composable/blob/main/CHANGELOG.md)
- **Contributing:** [CONTRIBUTING](https://github.com/Accenture/mercury-composable/blob/main/CONTRIBUTING.md)
  · [Code of Conduct](https://github.com/Accenture/mercury-composable/blob/main/CODE_OF_CONDUCT.md)
