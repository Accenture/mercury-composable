# Mercury Composable

Since version 4.0, we have merged our enterprise extension ("Event Script") with the Mercury v3.1 foundation code
to be a comprehensive toolkit to write composable applications including microservices and serverless.
This technology was filed under US Patent application 18/459,307. The source code is provided as is under
Apache 2.0 license.

# Getting Started

A composable application is designed in 3 steps:

1. Describe your use case as an event flow diagram
2. Create a configuration file to represent the event flow
3. Write a user story for each user function

Documentation is available at https://accenture.github.io/mercury-composable/

To get started, please begin with Chapter one at https://accenture.github.io/mercury-composable/guides/CHAPTER-1/

# Conquer Complexity: Embrace Composable Design Patterns

## Introduction

Software development is an ongoing battle against complexity. Over time, codebases can become tangled and unwieldy,
hindering innovation and maintenance. This article introduces composable design patterns, a powerful approach to
build applications that are modular, maintainable, and scalable.

## The Perils of Spaghetti Code

We have all encountered it: code that resembles a plate of spaghetti – tangled dependencies, hidden logic,
and a general sense of dread when approaching modifications. These codebases are difficult to test, debug, 
and update. Composable design patterns offer a solution.

## Evolution of Design Patterns

Software development methodologies have evolved alongside hardware advancements. In the early days, developers 
prized efficiency, writing code from scratch due to limited libraries. The rise of frameworks brought structure
and boilerplate code, but also introduced potential rigidity.

## Functional Programming and Event-Driven Architecture

Functional programming, with its emphasis on pure functions and immutable data, paved the way for composable design.
This approach encourages building applications as chains of well-defined functions, each with a clear input and output.

Event-driven architecture complements this approach by using events to trigger functions. This loose coupling
promotes modularity and scalability.

## The Power of Composable Design

At its core, composable design emphasizes two principles:

1.	*Self-Contained Functions*: Each function should be a well-defined unit, handling its own logic and transformations
    with minimal dependencies.
2.	*Event-Driven Orchestration*: Functions communicate through events, allowing for loose coupling and independent
    execution.

## Benefits of Composable Design

- *Enhanced Maintainability*: Isolated functions are easier to understand, test, and modify.
- *Improved Reusability*: Self-contained functions can be easily reused across different parts of your application.
- *Superior Performance*: Loose coupling reduces bottlenecks and encourages asynchronous execution.
- *Streamlined Testing*: Well-defined functions facilitate unit testing and isolate potential issues.
- *Simplified Debugging*: Independent functions make it easier to pinpoint the source of errors.
- *Technology Agnostic*: Composable code is less dependent on specific frameworks, allowing for easier future
  adaptations.

## Implementing Composable Design

While seemingly simple, implementing composable design can involve some initial complexity. 
Here's a breakdown of the approach:

- *Function Design*: Each function serves a specific purpose, with clearly defined inputs and outputs.
- *Event Communication*: Functions communicate through well-defined events, avoiding direct dependencies.
- *Orchestration*: An orchestrator, either code-based or configuration-driven, sequences and triggers functions
  based on events.

## Conclusion

Composable design patterns offer a powerful paradigm for building maintainable, scalable, and future-proof applications.
By embracing the principles of self-contained functions and event-driven communication, you can conquer complexity and
write code that is a joy to work with. Are you ready to take your development practices to the next level? 
Embrace composable design today!
