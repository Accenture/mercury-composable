---
title: Documentation Conventions
summary: How Mercury's documentation is written — the canonical vocabulary, the story spine, and the
  AI-discovery contract every page follows, so humans read a story and AI agents find context fast.
layer: reference
audience: [contributor, ai-agent]
keywords: [documentation conventions, canon, terminology, layers, one atom four roles, llms.txt, ai-friendly docs]
---

# Documentation Conventions

*Reference: The canon every documentation page follows — vocabulary, story spine, and the AI-discovery contract.*

> **At a glance**
>
> - **What** — the rules that keep Mercury's docs consistent: canonical terminology, the narrative
>   spine, and the machine-readable contract that lets an AI agent discover context without reading source.
> - **Why it matters** — the docs serve two readers at once: a human reading a story, and an AI agent
>   resolving context fast and deterministically. One canon keeps both coherent as the docs evolve.
> - **For** anyone — human or AI — editing these docs. A CI drift check (`scripts/check-doc-canon.py`)
>   enforces the mechanical parts.

## Two readers, one canon

Every page is written for two audiences at once:

1. **Humans** — the docs tell a *story*: why before how, a clear arc, engagement over exhaustiveness.
2. **AI agents** — the docs are a *context surface*: structured metadata and deterministic specs let an
   agent find the right page and generate correct artifacts without drilling into engine source.

## Canonical vocabulary

### "Layers" means the three paradigm layers — nothing else

Mercury ascends **three layers**. Reserve the word *layer* for these:

| Layer | Express behavior as… | You write |
|:--|:--|:--|
| **1 · Event-driven** (Platform Core) | decoupled functions reacting to events | Java functions, addressed by route name |
| **2 · Composable** (Event Script) | YAML flows that choreograph functions | ~50% config, 50% code |
| **3 · Semantic** (Active Knowledge Graph) | a graph whose nodes execute during traversal | a model — little or no code |

Describe the path a request takes from the caller through to the functions
(see the [Architecture Overview](architecture.md)) as a *pipeline*; use *layer* only for the three
paradigm layers above. (Don't frame the pipeline as a contrast with the layers — it just confuses a
first-time reader.)

### Layer-3 names

- **Active Knowledge Graph (AKG)** — the *thing* (the model / the artifact).
- **Knowledge Graph as Application** — the *paradigm* (the tagline for the idea).
- **MiniGraph** — the *engine* (`graph.executor`, the in-memory property graph, the Playground).
- **semantic** — an adjective only ("the semantic layer"); never a standalone noun for the thing.

### One atom, four roles

There is one building block: the **function** — a `@PreLoad` class implementing
`LambdaFunction` / `TypedLambdaFunction`, addressed only by its **route name**, with Map-or-PoJo I/O.
What we *call* it depends on how it is wired:

| Term | A function that… |
|:--|:--|
| **Function** | the atom — used at every layer |
| **Service** | is mapped straight to an HTTP endpoint via `service:` in `rest.yaml` |
| **Task** | is a step in an Event Script flow (carrying an `execution` type) |
| **Skill** | is attached to an Active Knowledge Graph node via the node's `skill:` property |

"Function" is the general atom; "service" is only the narrow REST role.

## The story spine

Every page reinforces one arc:

1. **Origin (the why).** The event-driven core descends from the Scala/Akka **actor model**, runs on
   the **Eclipse Vert.x** event bus, and uses **Java 21 virtual threads** so synchronous code performs
   like reactive — which is *why* functions are decoupled actors addressed by name.
2. **The ascent: code → configuration → knowledge.** Each layer pushes more "how" out of imperative
   code: decouple functions → move orchestration into YAML flows → move behavior into a model you traverse.
3. **Cross-cutting: human–AI collaboration.** Agent-ready DSL specs and the companion endpoint let
   humans and AI co-author across all three layers — a capability, not a fourth layer.

Voice: product narrative, present tense, why before how. Avoid analyst/whitepaper framing.

## The AI-discovery contract

- **Every page** carries YAML frontmatter (`title`, `summary`, `layer`, `audience`, `keywords`,
  optional `related`), an **"At a glance"** block, consistent headings with stable anchors, and a
  **"See also"** footer. URLs are lowercase-kebab **semantic slugs**. This is a clean rewrite —
  old URLs are intentionally **not** preserved (no redirects); the navigation is the source of truth.
- **`llms.txt`** is the machine-readable site map, organized by layer, and is kept current.
- **The three DSLs** (MiniGraph, Event Script, REST automation) each ship a deterministic spec kit:
  a grammar reference, a machine-readable catalog, an AI-agent guide, and a CI drift test. A claim that
  an agent can generate correct artifacts "from this page alone" belongs **only** to those agent guides,
  never to a concept page.

## Enforcement

The mechanical rules are checked in CI by `scripts/check-doc-canon.py` — slugs, frontmatter,
At-a-glance, no UTF-8 BOM, and retired terminology — alongside the three DSL grammar drift checks.

## See also

- [Architecture Overview](architecture.md) — the request pipeline and the origin story in full.
- [Methodology](methodology.md) — the four composable design principles.
- [Knowledge Graph as Application](knowledge-graph/index.md) — the semantic layer and the AKG vocabulary in use.
- [Contributing](https://github.com/Accenture/mercury-composable/blob/main/CONTRIBUTING.md) — how to contribute code and docs.
