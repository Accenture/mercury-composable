# Graph Builder Documentation Index

Status: document-system index.

> **Picking up this work?** [worklog.md](./worklog.md) has the detailed record of work.
>
> **Installing the workflow as `/` commands?** See [install-commands.md](./install-commands.md) — point your agent at it and it self-installs thin pointer commands (each just references the canonical doc) in its own native format.

Purpose: identify which graph-builder documents are canonical, which are support files, and how they relate.

## Canonical Documents

| Document | Status | Role |
|---|---|---|
| [workflow.md](./workflow.md) | Canonical overview | Defines the four phase boundaries and points to phase specs. |
| [requirements-gathering.md](./requirements-gathering.md) | Canonical phase spec | Defines `/requirements` and the design-ready graph brief schema. |
| [graph-design.md](./graph-design.md) | Canonical phase spec | Defines `/design` and the graph design specification schema. |
| [build.md](./build.md) | Canonical phase spec | Defines `/build` — lowering a design into verified MiniGraph commands. |
| [test.md](./test.md) | Canonical phase spec | Defines `/test` — proving runtime behavior via execution and state inspection. |
| [verify.md](./verify.md) | Canonical spec (optional, cross-cutting) | Defines `/graph-verify <target> [--report]` — an on-demand adversarial verification + quality review of any artifact from any phase; spawns a fresh-context reviewer and judges *quality/trustworthiness*, not just conformance. Optional; no pipeline gate. |
| [minigraph-syntax.md](./minigraph-syntax.md) | Canonical syntax reference | Defines Companion API command syntax and graph authoring mechanics. |
| [companion.mjs](./companion.mjs) | Canonical helper | Executes and verifies Companion API commands. |

## Authority And Precedence

When documents disagree, resolve in this order (higher wins):

1. **MiniGraph engine source / observed runtime behavior** — authoritative for what the engine actually does. A verified execution-evidence row (e.g. in [evidence/dogfood-customer-360.md](./evidence/dogfood-customer-360.md)) overrides any prose claim — but weight each row by its own verdict: rows marked *partial* or *untestable* are not conclusive.
2. **[minigraph-syntax.md](./minigraph-syntax.md)** — owns command syntax, reserved-property forms, the canonical node-type ↔ skill enumeration (its **Node Types** table), and Companion API mechanics.
3. **[requirements-gathering.md](./requirements-gathering.md)** — owns the `/requirements` brief schema and requirements-gate vocabulary.
4. **[graph-design.md](./graph-design.md)** — owns `/design` primitive *selection*, topology obligations, and the design gate. Its **Source-Verified Primitives** table is selection guidance and defers to minigraph-syntax's Node Types for the authoritative type/skill list.
5. **[workflow.md](./workflow.md)** — owns phase order and boundaries only.

Process, working, and rationale docs are advisory, not authoritative over content: [adversarial-review-checklist.md](../adversarial-review-checklist.md) governs *how* reviews run; [worklog.md](./worklog.md) is a record, not a spec. If a lower-priority doc is stale, fix it — do not carry two interpretations forward.

## Terminology

| Term | Meaning |
|---|---|
| Node type | The classification supplied by `with type` (e.g. `Fetcher`, `Mapper`, `Evaluator`). Optional and cosmetic for **skill** nodes (`skill` drives behavior), but **load-bearing** for `Root`, `End`, and no-skill nodes. |
| Skill | The executable route in `skill` (e.g. `graph.data.mapper`, `graph.api.fetcher`). The load-bearing property. |
| Primitive | A design-level capability choice (graph-design.md), usually a skill or a no-skill node pattern. |
| Decision branch | Runtime branching via `graph.math` / `graph.js` IF/THEN/ELSE — i.e. control flow. |
| Decision (node type) | A conventional outcome/result **mapper** (`graph.data.mapper`); **not** the same as a decision branch. |
| Provider | No-skill node defining an HTTP endpoint (url, method, request-param placement). |
| Dictionary | No-skill node binding a provider to named fetcher input/output. |
| Island / Entity | A `graph.island` node — always returns `.sink`, stopping that traversal branch; used to hold catalog/dictionary structure that must be reachable but must not execute downstream. |

## Support Documents

| Document | Status | Role |
|---|---|---|
| [install-commands.md](./install-commands.md) | Installer | Agent self-install of the workflow as thin pointer `/` commands, in the agent's own native format. |
| [examples/customer-360-requirements-brief.md](./examples/customer-360-requirements-brief.md) | Example | Shows a filled design-ready graph brief using the canonical schema. |
| [examples/customer-360-design-spec.md](./examples/customer-360-design-spec.md) | Example | Shows the matching `/design` artifact — completes the brief→design requirement-ID round-trip. |
| [worklog.md](./worklog.md) | Working log | Append-only record of work performed. |
| [evidence/dogfood-customer-360.md](./evidence/dogfood-customer-360.md) | Evidence | P0 dogfood claim matrix — execution verdicts against the live engine. |

## Scratchpad Notes

The notes under `system/minigraph-playground-engine/notes/` are working notes. They may explain how the current docs evolved, but they are not canonical when they conflict with this directory.

In particular, [requirements-gathering.md](./requirements-gathering.md) owns the `/requirements` brief schema.