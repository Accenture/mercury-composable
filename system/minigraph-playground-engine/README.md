# MiniGraph Playground Engine

This is the engine for the MiniGraph Playground for user to create and test Knowledge Graphs

## Introduction

MiniGraph Playground is a developer-focused environment for creating, testing, and executing **Active Knowledge Graphs**.

It provides both:
- A **graph execution engine**
- A **self-service interactive UI**

Developers use MiniGraph to model backend logic, decision flows, data access, and orchestration using graph structures and composable skills.

---

## Core Concepts

### Active Knowledge Graph

An Active Knowledge Graph is a directed graph with:
- A **root node** (execution entry point)
- An **end node** (execution completion)
- One or more nodes configured with executable skills

Traversal begins at the root and continues until the end node is reached or traversal is intentionally paused or redirected.

---

### Node Types

MiniGraph supports three primary node categories:

#### 1. Data Entity Nodes
Represent business entities and their attributes  
Examples:
- Person
- Account
- Order

These nodes describe *what the system knows*.

---

#### 2. Data Dictionary & Provider Nodes
Define external data sources and API contracts:
- Attribute definitions
- Endpoint configurations
- Request and response mappings

These nodes enable integration with external systems.

---

#### 3. Skill Nodes (Active Nodes)
Skill nodes execute actions during traversal:
- Computation
- Decision making
- Data fetching
- Flow control

Skill nodes are the **behavioral backbone** of the graph.

---

## Skills and Execution Model

A skill is implemented as a **Composable Function**:
- Self-contained
- Event-driven
- Stateless
- Independently deployable

During execution:
1. The graph executor sends input to the skill
2. The skill executes and returns a result
3. The executor updates the graph state machine
4. Traversal continues based on the outcome

---

## Built-In Skills

MiniGraph includes the following built-in skills:

- **graph.data.mapper**  
  Maps data between nodes

- **graph.math**  
  High-performance math and boolean evaluation using native Java

- **graph.js**  
  JavaScript-based expression evaluation (more flexible, slower)

- **graph.api.fetcher**  
  Invokes external APIs using data dictionary definitions

- **graph.extension**  
  Executes another graph model

- **graph.island**  
  Pauses traversal to isolated subgraphs

- **graph.join**  
  Synchronizes multiple execution paths

Each skill has its own help page with syntax, parameters, and examples.

---

## Interactive Development Experience

MiniGraph Playground provides:
- Command prompt for graph operations
- Console output with execution details
- Visual graph rendering via `describe graph`

The environment is intentionally **playful and exploratory**, encouraging incremental modeling and validation.

---

## Testing and Dry-Run Execution

Developers are encouraged to:
- Execute individual skill nodes
- Inspect intermediate state machine data
- Validate traversal paths visually

Conduct **dry-run**:
- Starts at the root node
- Traverses the graph using mock input
- Displays execution paths and outputs
- Requires no live system dependencies

---

## Deployment Model

After validation:
- Graph models are deployed to cloud environments
- Each graph is exposed via a generalized API: `/api/graph/{graph-id}`

Execution is decoupled from protocol using **Event Script**, allowing:
- REST invocation
- Event-driven execution (e.g., Kafka)
- Future protocol extensions

---

## Help System

MiniGraph includes comprehensive built-in help pages covering:
- Node creation, editing, deletion
- Graph traversal and execution
- Import/export
- Data dictionary configuration
- Skill-specific usage

These help pages are accessible directly from the Playground UI and serve as the primary learning resource.

---

## Extensibility

Developers can extend MiniGraph by:
- Writing new composable functions
- Registering them as custom skills
- Reusing them across multiple graph models

This enables organization-specific logic without modifying the core platform.

---

## Summary

MiniGraph Playground enables developers to:
- Model backend logic visually
- Execute complex flows without orchestration code
- Test and validate behavior early
- Deploy production-ready graph-based services

It bridges graph modeling, event-driven execution, and composable design into a single, coherent developer experience.

