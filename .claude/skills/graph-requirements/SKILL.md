---
name: graph-requirements
description: "Graph-builder phase 1/4 — turn rough intent into a design-ready brief (user-invoked)."
argument-hint: "[optional: intent notes, a sample request/response, or a generated artifact]"
disable-model-invocation: true
---

Run the graph-builder requirements phase (Phase 1 of 4).

**Source of truth:** open and follow `docs/graph-builder/requirements-gathering.md`
in full — it is authoritative for every step, gate, and schema. This command is
only a launcher: read the doc now, do not work from memory of it, and if this
text and the doc ever disagree, the doc wins.

**Input:** treat the command argument (if any) as a path to intent notes, a
sample request/response, or a generated artifact to seed the brief.

**Naming note:** the doc refers to the phases as `/requirements`, `/design`,
`/build`, `/test`; in this installation they are `/graph-requirements`,
`/graph-design`, `/graph-build`, `/graph-test`.
