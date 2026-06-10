---
name: graph-build
description: "Graph-builder phase 3/4 — lower the design into verified MiniGraph commands (user-invoked)."
argument-hint: "[path to the design spec]"
disable-model-invocation: true
---

Run the graph-builder build phase (Phase 3 of 4).

**Source of truth:** open and follow `docs/graph-builder/build.md`
in full — it is authoritative for every step, gate, and schema. This command is
only a launcher: read the doc now, do not work from memory of it, and if this
text and the doc ever disagree, the doc wins.

**Input:** treat the command argument (if any) as a path to the design spec.

**Naming note:** the doc refers to the phases as `/requirements`, `/design`,
`/build`, `/test`; in this installation they are `/graph-requirements`,
`/graph-design`, `/graph-build`, `/graph-test`.
