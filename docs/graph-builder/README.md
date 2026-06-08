# Graph Builder Documentation Index

Status: document-system index.

Purpose: identify which graph-builder documents are canonical, which are support files, and how they relate.

## Canonical Documents

| Document | Status | Role |
|---|---|---|
| [workflow.md](./workflow.md) | Canonical overview | Defines the four phase boundaries and points to phase specs. |
| [requirements-gathering.md](./requirements-gathering.md) | Canonical phase spec | Defines `/requirements` and the design-ready graph brief schema. |
| [graph-design.md](./graph-design.md) | Canonical phase spec | Defines `/design` and the graph design specification schema. |
| [minigraph-syntax.md](./minigraph-syntax.md) | Canonical syntax reference | Defines Companion API command syntax and graph authoring mechanics. |
| [companion.mjs](./companion.mjs) | Canonical helper | Executes and verifies Companion API commands. |

## Support Documents

| Document | Status | Role |
|---|---|---|
| [requirements-recalibration-assessment.md](./requirements-recalibration-assessment.md) | Rationale | Explains why `/requirements` was recalibrated. |
| [examples/customer-360-requirements-brief.md](./examples/customer-360-requirements-brief.md) | Example | Shows a filled design-ready graph brief using the canonical schema. |

## Scratchpad Notes

The notes under `system/minigraph-playground-engine/notes/` are working notes. They may explain how the current docs evolved, but they are not canonical when they conflict with this directory.

In particular, [requirements-gathering.md](./requirements-gathering.md) owns the `/requirements` brief schema.