# Developer's Guide

Mercury Composable is a software development toolkit for writing composable, event-driven applications.

---

## Tutorials

Guided, end-to-end experiences for learning the framework from scratch.

- [Getting Started](CHAPTER-1.md) — Build and run the composable example app; understand flows, functions, and REST endpoints through a working project.

---

## Guides

Task-oriented instructions for accomplishing specific goals.

- [REST Automation](CHAPTER-3.md) — Declare HTTP endpoints using rest.yaml without writing controllers.
- [Build, Test and Deploy](CHAPTER-5.md) — Structure your application entry point, write unit tests, and package for deployment.
- [Spring Boot Integration](CHAPTER-6.md) — Run Mercury Composable as a Spring Boot application using platform-core or rest-spring-3.
- [Event over HTTP](CHAPTER-7.md) — Enable cross-instance event communication using the built-in Event API endpoint.
- [Minimalist Service Mesh](CHAPTER-8.md) — Set up service discovery and inter-instance routing using Kafka as a connector.
- [Minimalist Property Graph](CHAPTER-10.md) — Use the built-in in-memory property graph for computation and decision-making.

---

## Concepts

Explanation-oriented pages for understanding design decisions and how the framework works.

- [Methodology](METHODOLOGY.md) — The four composable design principles: input-process-output, zero dependency, platform abstraction, event choreography.
- [Architecture Overview](ARCHITECTURE.md) — Complete technical mental model: layers, components, Event Script, threading, and core APIs.
- [Composable Design](COMPOSABLE-DESIGN.md) — Essay on composable design patterns and their benefits over tightly coupled architectures.
- [Function Execution Strategies](CHAPTER-2.md) — How virtual threads and kernel threads work, when to use each, and Mono/Flux support.
- [Design Notes](../arch-decisions/DESIGN-NOTES.md) — Architecture decisions and technical rationale behind key framework choices.

---

## Reference

Precise, exhaustive lookup material.

- [Event Script Syntax](CHAPTER-4.md) — Complete DSL reference: flow structure, all task execution types, input/output data mapping namespaces and constants.
- [API Overview](CHAPTER-9.md) — Full PostOffice, Platform, EventEnvelope, and configuration API documentation.
- [Application Properties](APPENDIX-I.md) — Complete listing of application.properties and application.yml configuration keys.
- [Reserved Names & Headers](APPENDIX-II.md) — System-reserved route names and HTTP headers that must not be overridden.
- [Actuators & HTTP Client](APPENDIX-III.md) — Built-in actuator endpoints, the AsyncHttpRequest API, and utility services.
- [Release Notes](../CHANGELOG.md) — Version history and notable changes by release.
