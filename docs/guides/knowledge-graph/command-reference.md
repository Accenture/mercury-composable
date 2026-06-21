---
title: MiniGraph command grammar
summary: The authoritative, rule-based reference for the MiniGraph Playground command language —
  lexical rules, namespaces, every command's exact syntax, node types, and the skill→property
  matrix. Designed so a human or an AI agent can generate commands deterministically without
  inferring the grammar from examples.
layer: knowledge-graph
audience: [developer, architect, ai-agent, reference]
keywords: [minigraph, dsl, grammar, command syntax, companion endpoint, deterministic, data mapping]
related:
  - guides/knowledge-graph/skills-reference.md
  - guides/knowledge-graph/build-your-first-graph.md
  - guides/CHAPTER-4.md
---

# MiniGraph command grammar

> **At a glance**
>
> - This is the **source of truth** for the MiniGraph command language — the rules, not a tour.
>   Tutorials teach by example; this page states the grammar so you (or an AI agent) generate
>   commands **deterministically**, without inferring.
> - A machine-readable form of this grammar lives at
>   [`minigraph-commands.json`](minigraph-commands.json) (ingest it; validate against it).
> - Driving the Playground from an agent? See the [AI agent guide](ai-agent-guide.md).

## Lexical rules {#lexical}

| Element | Rule | Example |
|---|---|---|
| **Node name** | lowercase letters and hyphen only | `person-name`, `mdm-profile` |
| **Node type** | a descriptive label; shipped examples **capitalize** structural types | `Root`, `End`, `Provider`, `Dictionary`, `Fetcher`, `Island` |
| **Reserved names** | the root node **must** be named `root`; the end node **must** be `end` | — |
| **Property** | `key=value`; keys may be composite (dot-bracket) | `url=http://...`, `mapping[]=a -> b` |
| **List property** | a `key[]=entry` line *appends* one entry to the list `key` | repeat `mapping[]=...` per entry |
| **Multi-line value** | wrap the value in triple single quotes | `statement[]='''` … `'''` |
| **Constant** | Event Script constant syntax `type(value)` | `text(hello)`, `int(100)` — full set in [Event Script](../CHAPTER-4.md) |
| **Mapping operator** | `source -> target` (left = source, right = target) | `input.body.id -> person_id` |
| **Variable substitution** | `{namespace.key}` inside `COMPUTE`/`IF` expressions | `{book.price}`, `{input.body.discount}` |

## Namespaces {#namespaces}

The per-execution **state machine** is addressed through these namespaces (sources and targets in
mappings):

| Namespace | Meaning | Read | Write |
|---|---|---|---|
| `input.body` / `input.header` | the incoming request | ✓ | (seeded at instantiate) |
| `model.*` | intermediate working state | ✓ | ✓ |
| `output.body` / `output.header` | the response returned to the caller | ✓ | ✓ |
| `{node-name}` | a node's own properties | ✓ | ✓ |
| `{node-name}.result` | a skill's output | ✓ | (set by the skill) |
| `{node-name}.status` / `.error` | a skill's execution status | ✓ | (set by the engine) |
| `response.*` | a **data Provider's** raw HTTP response — used in a **Dictionary** node's `output[]` | ✓ (in a dictionary mapping) | (set by the fetch) |
| `result.*` | a **Dictionary/Fetcher** result set | ✓ | (set by the skill) |

## Commands {#commands}

Each command's exact form. Lines shown stacked are a single **multi-line** command (enter as one
block); one-line commands are self-contained.

### create node / update node {#create}

Multi-line. `update node` has the identical shape and replaces a node's definition.

```
create node {name}
with type {type}
with properties
{key}={value}
{key}={value}
```

- `with properties` and the key lines are **optional** (properties act as defaults).
- A node has **zero or one** skill, set with `skill={route}`.
- `{name}` and `{type}` are lowercase-and-hyphen; `root`/`end` are reserved (see [lexical](#lexical)).

### connect {#connect}

One-line. **Directional** — `connect a to b` differs from `connect b to a`.

```
connect {node-a} to {node-b} with {relation}
```

The `{relation}` is a **descriptive label** (e.g. `done`, `fetch`, `provider`) — free-form, not
interpreted for skill routing. For data-entity nodes, meaningful relationship names capture
enterprise knowledge.

### delete {#delete}

```
delete node {name}
delete connection {node-a} and {node-b}
```

### instantiate graph {#instantiate}

Multi-line. Creates a runnable instance with optional seeded mock input. **Required before**
`run`, `execute`, or `inspect`. Alias: `start`.

```
instantiate graph
{constant} -> input.body.{key}
{constant} -> input.header.{key}
{constant} -> model.{key}
```

```
instantiate graph
int(100) -> input.body.profile_id
text(application/json) -> input.header.content-type
```

### run / execute / inspect {#run}

```
run                        # traverse from root to end
execute {node}             # run a single node (after instantiate)
inspect {namespace.key}    # read a value from the state machine
```

```
inspect output                # a whole namespace: input | output | model
inspect {output.body.name}    # a specific composite key (braced)
```

### describe / list / seen {#describe}

```
describe graph
describe node {name}
describe connection {node-a} and {node-b}
describe skill {skill.route}     # prints the shipped help for a skill
list nodes
list connections
seen                             # nodes visited in the last run
```

### export / import {#export}

```
export graph as {name}
import graph from {name}
import node {node} from {name}
```

- `export` writes JSON to `location.graph.temp`; it adds `name={name}` to the root node and
  **fails if any node is an orphan** (every node must connect to ≥1 other).

### session {#session}

```
session                    # show this session id + subscribers
session subscribe {id}     # mirror another (primary) session's commands into yours
session unsubscribe
session reset
```

## Node types {#node-types}

`root` and `end` are **structural** (entry/exit). All other types are **descriptive labels**
validated by the node's skill, if any. Common conventional types:

| Type | Role | Typical skill |
|---|---|---|
| `Root` / `End` | entry / exit | — / often `graph.data.mapper` |
| data-entity | passive data holder | none |
| `Dictionary` | external attribute definition | none (config) |
| `Provider` | external endpoint definition | none (config) |
| `Island` | groups configuration / isolated nodes (not executed) | `graph.island` |
| (active node) | does work during traversal | a `graph.*` skill |

## Skill → property matrix {#skill-matrix}

Which properties each skill accepts. See the [skills reference](skills-reference.md) for semantics
and examples; this is the at-a-glance contract.

| Skill (`skill=`) | Required | Optional |
|---|---|---|
| `graph.data.mapper` | `mapping[]` | — |
| `graph.math` | `statement[]` (`COMPUTE`/`IF`/`MAPPING`/`EXECUTE`/`RESET`) | `for_each[]`, `NEXT:`, `DELAY:`, `BEGIN`/`END` |
| `graph.js` | `statement[]` (same as math) | `for_each[]`, `NEXT:`, `DELAY:` |
| `graph.api.fetcher` | `dictionary[]`, `input[]`, `output[]` | `for_each[]`, `concurrency` (1–30, def 3), `exception` |
| `graph.extension` | `extension` (`{graph-id}` or `flow://{flow-id}`), `input[]` | `output[]`, `for_each[]`, `concurrency`, `exception` |
| `graph.join` | — | — |
| `graph.island` | — | — |

Configuration nodes used by `graph.api.fetcher` (see [Composing the layers](composing-the-layers.md#data-dictionary)):

| Node | Properties |
|---|---|
| **Provider** | `url`, `method`, `feature[]`, `input[]` (targets: `header.*`, `query.*`, `path_parameter.*`, `body.*`) |
| **Dictionary** | `provider`, `input[]`, `output[]` (→ `result.*`) |

## Invariants {#invariants}

Hard rules the engine enforces — violate them and generation fails:

1. The root node is named `root`; the end node is named `end`.
2. A node has **0 or 1** skill.
3. Node **names** are **lowercase + hyphen** only (`root`/`end` reserved). Node **types** are
   descriptive labels — shipped examples capitalize structural types (see [lexical](#lexical)).
4. Every node **in the traversal path** must connect to ≥1 node, or `export` fails. **Exception:**
   `Dictionary` and `Provider` configuration nodes are referenced *by name* (`dictionary[]=`,
   `provider=`) — not traversed — and need **no** connections; optionally group them under a
   `graph.island` node purely for organization.
5. A node is **executed once** per run (loop guard); `graph.math`/`graph.js` `RESET` is the only
   escape, for advanced re-execution.
6. `instantiate graph` must precede `run` / `execute` / `inspect`.

## See also {#see-also}

- [`minigraph-commands.json`](minigraph-commands.json) — the machine-readable form of this grammar.
- [AI agent guide](ai-agent-guide.md) — driving the Playground via the companion endpoint.
- [Built-in skills reference](skills-reference.md) — per-skill semantics and examples.
- [Event Script Syntax](../CHAPTER-4.md) — the shared data-mapping syntax and the full constant set.
