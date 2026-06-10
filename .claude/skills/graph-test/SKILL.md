---
name: graph-test
description: "Graph-builder workflow phase 4/4 — Prove a built graph's runtime behavior (user-invoked)."
argument-hint: "[graph name, or path to the build log]"
disable-model-invocation: true
---

Prove a built graph's runtime behavior: run each required scenario, assert the output contract with --expect, confirm the important state transitions by inspection, and record defects and accepted limitations.

**Invocation.** Run after /graph-build. Requires a live engine + companion.mjs (see Prerequisites). Produces a test report; passing it is the precondition for deploy.

**Prerequisites.** This phase drives a live MiniGraph engine via `companion.mjs`: it needs Node >=20, a reachable `MINIGRAPH_HOST` (default `localhost:8085`), and — for `/graph-test` — the stub-server pattern for mocked sources. (`/graph-requirements` and `/graph-design` need none of this.)

**Canonical spec (authoritative).** Full detail and precedence live in [`.graph-builder/test.md`](.graph-builder/test.md); defer to it on any conflict. The step checklist and gate below are lifted verbatim from that spec — this wrapper is not a second source of truth.

**Command names.** The spec text below names the phases `/requirements`, `/design`, `/build`, `/test`; in this installation they are `/graph-requirements`, `/graph-design`, `/graph-build`, `/graph-test`.

## Step checklist

- **T1** Load the built graph into a clean session; confirm structure matches the build export.
- **T2** Run the **happy path**; assert every required `output.body` path; inspect the key `model.*` transitions.
- **T3** Run each **required failure/edge scenario** from `/requirements` (missing input, empty source, dependency failure, fallback/degraded, branch); assert the degraded/error contract and the failure state paths.
- **T4** Confirm **important state transitions** named in the design's `test_handoff` (e.g. fallback set `model.degraded=true`; a convergence node ran; a `for_each` produced N results).
- **T5** Record **defects** with suspected owning phase, and **accepted limitations** (things not testable in this environment).
- **T6** Run the gate.

## Gate

`/test` passes only when:

- The **happy path passes** — every required `output.body`/`output.header` path is present and correct.
- Every **required failure and edge scenario** from `/requirements` passes (produces the specified degraded/error contract), or is explicitly recorded as `blocked-on-mock`/accepted-limitation with rationale.
- The **output contract is satisfied** for every scenario (asserted via `--expect`, not eyeballed).
- The **important state transitions** from the design are confirmed by inspection — not inferred from the final output alone.
- **Remaining risks are explicit**: every defect has an owning phase; every accepted limitation has a reason and (if it blocks production) a carried deploy blocker.

A scenario that fails because the *graph* is wrong is a defect routed to `/build` or `/design`; a scenario that fails because the *spec* was silent is routed to `/requirements`. `/test` does not invent the missing behavior.
