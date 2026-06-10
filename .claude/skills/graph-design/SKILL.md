---
name: graph-design
description: Graph-builder workflow phase 2/4 — Convert a design-ready brief into a buildable MiniGraph architecture (user-invoked).
argument-hint: "[path to the requirements brief]"
disable-model-invocation: true
---

Convert a design-ready brief into a buildable MiniGraph architecture: node responsibilities, skill primitives, edges, state paths, control-flow and failure behavior, and the inspection points /test will use — without emitting any commands.

**Invocation.** Run after /graph-requirements passes its gate. Consumes the brief; produces the graph design spec that /graph-build lowers into commands.

**Canonical spec (authoritative).** Full detail and precedence live in [`.graph-builder/graph-design.md`](.graph-builder/graph-design.md); defer to it on any conflict. The step checklist and gate below are lifted verbatim from that spec — this wrapper is not a second source of truth.

**Command names.** The spec text below names the phases `/requirements`, `/design`, `/build`, `/test`; in this installation they are `/graph-requirements`, `/graph-design`, `/graph-build`, `/graph-test`.

## Step checklist

Terse form of the Operating Protocol above; each step is detailed in its `D#` section.

- **D1** Accept or reject the brief — `gate_result.status` allows `/design`; build blockers mocked, deploy blockers carried.
- **D2** Restate the graph obligation — a checksum against the brief, not a new requirements pass.
- **D3** Choose the smallest graph shape that satisfies the obligations; name why.
- **D4** Build the node inventory — alias, responsibility, skill, reads/writes, neighbors, requirements satisfied.
- **D5** Assign a skill primitive (or no-skill) to every executable node, with a reason.
- **D6** Design state and mappings — every target path has exactly one owner.
- **D7** Design source/provider clusters — providers, dictionaries, extensions, flows, mocks.
- **D8** Design edges and control flow — natural edges, jumps, exception targets, joins, sinks, loop exits.
- **D9** Design failure and fallback behavior for each source and composition boundary.
- **D10** Design test inspection points for `/test`.
- **D11** Run the design gate (below) and record the result in the artifact.

## Gate

`/design` passes only when:

- Every requirement maps to at least one node, edge, state path, source plan item, or failure plan item; any unavailable real dependency additionally has an explicit carried deploy blocker, with its mock/source plan kept visible.
- The graph has a root and end strategy.
- Every executable node has a selected primitive and a reason.
- Every API fetcher, extension, decision, join, and island has its required supporting design elements.
- Every output contract path has a writer.
- Every state path read by a node has a producer or comes from input/model/static node properties.
- Every branch target, exception target, and join upstream node is named in the node inventory.
- Every `for_each` design maps list inputs into `model.*` paths and defines concurrency expectations.
- Failure behavior is explicit for each external source and composition boundary.
- Test inspection points cover happy path, required failure paths, branch decisions, and output contract paths.
- Remaining blockers are deploy-only blockers carried forward under `gate_result.carried_blockers` with mock-and-proceed plans, not missing design decisions or build blockers.
