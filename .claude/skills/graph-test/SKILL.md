---
name: graph-test
description: "Graph-builder phase 4/4 — prove runtime behavior by execution + inspection (user-invoked)."
argument-hint: "[graph name, or path to the build log]"
disable-model-invocation: true
---

Run the graph-builder test phase (Phase 4 of 4).

**Source of truth:** open and follow `docs/graph-builder/test.md`
in full — it is authoritative for every step, gate, and schema. This command is
only a launcher: read the doc now, do not work from memory of it, and if this
text and the doc ever disagree, the doc wins.

**Input:** treat the command argument (if any) as a graph name, or a path to the build log.

**Naming note:** the doc refers to the phases as `/requirements`, `/design`,
`/build`, `/test`; in this installation they are `/graph-requirements`,
`/graph-design`, `/graph-build`, `/graph-test`.
