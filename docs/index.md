# Mercury Composable for Java

Mercury Composable is a Java framework for building **composable, event-driven
applications** from self-contained functions wired by YAML-configured event flows.
Functions have no direct knowledge of each other — all coupling is through named routes
and event envelopes, so orchestration is *configuration, not code*. It runs on Java 21
virtual threads, so sequential synchronous RPC performs on par with reactive code.

> **New here?** Start with the **[5-Minute Quickstart](guides/QUICKSTART.md)**, then read
> **[Getting Started](guides/CHAPTER-1.md)**.

## Three ways to build

Mercury offers a layered ascent — each layer builds on the one beneath it, and you can
mix them in a single application:

| Layer | Technology | What you write | Start here |
|:------|:-----------|:---------------|:-----------|
| **Event-driven** | Platform Core | Decoupled functions, addressed by route name | [Function Execution Strategies](guides/CHAPTER-2.md) |
| **Composable** | Event Script | YAML event flows (~50% config, 50% code) | [Event Script Syntax](guides/CHAPTER-4.md) |
| **Semantic** | Active Knowledge Graph | Graph models that *execute* — zero code | [Active Knowledge Graph](guides/CHAPTER-11.md) |

## Explore the docs

- **Quickstart & tutorials** — [Quickstart](guides/QUICKSTART.md) · [Getting Started](guides/CHAPTER-1.md)
- **Guides** — [REST Automation](guides/CHAPTER-3.md) · [Build, Test & Deploy](guides/CHAPTER-5.md) ·
  [Spring Boot Integration](guides/CHAPTER-6.md) · [Event over HTTP](guides/CHAPTER-7.md) ·
  [Service Mesh](guides/CHAPTER-8.md)
- **Knowledge Graph** — [Minimalist Property Graph](guides/CHAPTER-10.md) ·
  [Active Knowledge Graph](guides/CHAPTER-11.md)
- **Concepts** — [Methodology](guides/METHODOLOGY.md) · [Architecture Overview](guides/ARCHITECTURE.md) ·
  [Composable Design](guides/COMPOSABLE-DESIGN.md) · [Design Notes](arch-decisions/DESIGN-NOTES.md)
- **Reference** — [Annotations](guides/ANNOTATIONS-REFERENCE.md) ·
  [Configuration](guides/CONFIGURATION-REFERENCE.md) · [Event Envelope](guides/EVENT-ENVELOPE-REFERENCE.md) ·
  [Flow Schema](guides/FLOW-SCHEMA-REFERENCE.md) · [API Overview](guides/CHAPTER-9.md) ·
  [Full Table of Contents](guides/TABLE-OF-CONTENTS.md)

## Project

- **Source:** [github.com/Accenture/mercury-composable](https://github.com/Accenture/mercury-composable)
- **Release notes:** [CHANGELOG](https://github.com/Accenture/mercury-composable/blob/main/CHANGELOG.md)
- **Contributing:** [CONTRIBUTING](https://github.com/Accenture/mercury-composable/blob/main/CONTRIBUTING.md)
  · [Code of Conduct](https://github.com/Accenture/mercury-composable/blob/main/CODE_OF_CONDUCT.md)
