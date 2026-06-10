---
name: graph-build
description: "Graph-builder workflow phase 3/4 — Lower a design spec into executable MiniGraph commands, building the live graph one verified mutation at a time, then smoke-test that it instantiates and runs once on the happy path, and export it (user-invoked)."
argument-hint: "[path to the design spec]"
disable-model-invocation: true
---

Lower a design spec into executable MiniGraph commands, building the live graph one verified mutation at a time, then smoke-test that it instantiates and runs once on the happy path, and export it.

**Invocation.** Run after /graph-design passes. Requires a live engine + companion.mjs (see Prerequisites). Produces a build log + the exported graph. It does not re-choose topology (that is /graph-design) or prove full behavior (that is /graph-test).

**Prerequisites.** This phase drives a live MiniGraph engine via `companion.mjs`: it needs Node >=20, a reachable `MINIGRAPH_HOST` (default `localhost:8085`), and — for `/graph-test` — the stub-server pattern for mocked sources. (`/graph-requirements` and `/graph-design` need none of this.)

**Canonical spec (authoritative).** Full detail and precedence live in [`.graph-builder/build.md`](.graph-builder/build.md); defer to it on any conflict. The section(s) below are lifted verbatim from that spec — this wrapper is not a second source of truth.

**Command names.** The spec text below names the phases `/requirements`, `/design`, `/build`, `/test`; in this installation they are `/graph-requirements`, `/graph-design`, `/graph-build`, `/graph-test`.

## Step checklist

- **B1** Accept the design: `gate_result.status: pass`; node inventory, edges, state plan, source plan, and failure plan are present. If not, return to `/design`.
- **B2** Resolve mocks: for each unavailable real source, stand up the stub/mock from the design's `source_plan.mocks` and record the carried deploy blocker.
- **B3** Emit and verify nodes (root/end → skills → providers/dictionaries), one at a time.
- **B4** Emit and verify edges (provider links → main flow → join `wait` edges → fan-out), one at a time.
- **B5** Verify branch/exception/join integrity: every jump/exception target exists; every join predecessor always completes in the scenarios that reach it.
- **B6** Smoke run: instantiate (happy seed) → run (`--expect` design output keys) → confirm output. Record in the log.
- **B7** Export the graph; confirm persistence **last** (after the smoke run) via an in-place `session reset` + `import graph from {name}`, verifying the node/connection count returns (companion does not verify `export`; the REST driver cannot mint a scratch session). Record `exported_as`.
- **B8** Record deviations from the design and run the gate.

## Gate

`/build` passes only when:

- Every design node and every design edge is present and **structurally verified** (companion.mjs `✓` for each), with no unverified mutation in the log.
- Every branch target, `exception` handler, and join predecessor named in the design exists as a node.
- No join has a backward-linked predecessor that can be skipped on a path that reaches it (hazard check).
- The graph **instantiates** (live instance, seeded `input.body`) and **runs once on the happy path**, producing the design's output keys.
- The graph **exports**, confirmed by an `import` round-trip (companion does not verify `export`; over REST this is an in-place `session reset` + re-import done as the final step).
- Every deviation from the design is recorded; if a deviation changes topology or responsibility, `/build` is **blocked** — return to `/design` rather than absorb it.
- Mocked sources are recorded with their carried deploy blockers.

A `/build` `blocked` status means the design could not be lowered as written; do not paper over it.
