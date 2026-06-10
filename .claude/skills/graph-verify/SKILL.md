---
name: graph-verify
description: "Graph-builder — adversarial quality review of any artifact, runs any time (user-invoked)."
argument-hint: "<target> [--report [path]]"
disable-model-invocation: true
---

Run the graph-builder verify phase (optional, any time — adversarial quality review of any artifact).

**Source of truth:** open and follow `docs/graph-builder/verify.md`
in full — it is authoritative for every step, gate, and schema. This command is
only a launcher: read the doc now, do not work from memory of it, and if this
text and the doc ever disagree, the doc wins.

**Input:** treat the command argument (if any) as a target artifact to review,
optionally followed by `--report [path]`.

**Naming note:** the doc refers to the phases as `/requirements`, `/design`,
`/build`, `/test`; in this installation they are `/graph-requirements`,
`/graph-design`, `/graph-build`, `/graph-test`.
