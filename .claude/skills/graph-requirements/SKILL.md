---
name: graph-requirements
description: "Graph-builder workflow phase 1/4 — Turn rough intent into a design-ready MiniGraph brief (user-invoked)."
argument-hint: "[optional: path to intent notes, a sample request/response, or a generated artifact]"
disable-model-invocation: true
---

Turn rough intent into a design-ready MiniGraph brief: gather the behavior, contracts, dependencies, mappings, failure semantics, and test scenarios a graph must satisfy — before any topology is chosen. The phase is done only when the brief passes its gate.

**Invocation.** Run when starting a new graph or a substantive change to one. Produces the design-ready brief that /graph-design consumes. You do not need to know nodes, skills, or commands — this phase gathers obligations, not syntax.

**Canonical spec (authoritative).** Full detail and precedence live in [`.graph-builder/requirements-gathering.md`](.graph-builder/requirements-gathering.md); defer to it on any conflict. The step checklist and gate below are lifted verbatim from that spec — this wrapper is not a second source of truth.

**Command names.** The spec text below names the phases `/requirements`, `/design`, `/build`, `/test`; in this installation they are `/graph-requirements`, `/graph-design`, `/graph-build`, `/graph-test`.

## Step checklist

Terse form of the Operating Protocol; each step is detailed in its `R#` section. Gather adaptively and out of order — the brief is incomplete until every step is satisfied or explicitly blocked.

- **R1** Name the outcome — purpose, caller, success definition, workflow category.
- **R2** Define the invocation contract — required/optional inputs, paths, sample payloads, missing-input rule.
- **R3** Define the output contract — mandatory/optional fields, error + degraded shapes.
- **R4** Build the state contract — `model.*`, node-local, constants, opaque pass-through.
- **R5** Inventory sources and capabilities — kind, request/response contract, auth, parallelism, mock.
- **R6** Capture mapping rules — input→model, source→model, model→output, derived/defaulted/repeated.
- **R7** Discover control flow — sequence, parallel, joins, decisions, per-item behavior.
- **R8** Define failure and degraded behavior for every meaningful dependency failure.
- **R9** Capture non-functional constraints — latency, concurrency, cache, TTL, security, observability.
- **R10** Define test scenarios — happy path, missing input, empty source, dependency failure, fallback, branch.
- **R11** Lock the requirements gate (see [Gate](#gate)) — decide whether `/design` may begin.

## Gate

Step R11 — Lock the requirements gate.

Goal: decide whether `/requirements` is complete and `/design` may begin.

Populates: `gate_result`, `scope_boundary`, `decisions`, `open_questions`.

The assistant must run the gate before advancing. If any required gate item fails, continue requirements gathering instead of invoking `/design`.

The gate consolidates the quality checks from Steps R1-R10. Do not maintain a separate, divergent checklist; if a gate item and a step quality check disagree, fix the step and gate together.

`gate_result.status` records only whether `/design` may begin. It is `pass` when nothing blocks requirements or design, even if build or deploy blockers remain. Record those remaining blockers under `gate_result.carried_blockers` with their mock-and-proceed plans. Never set `status: blocked` for a build- or deploy-only blocker — that is the mock-and-proceed case, and it is design-ready.

### Gate A - Scope Boundary

Every meaningful concern is classified:

| Verdict | Meaning |
|---|---|
| IN | This graph is responsible. |
| OUT | Another component or process is responsible. |
| PARTIAL | This graph owns only a defined slice. |
| DECISION | Ownership is not yet clear. |

Gate passes when no `DECISION` item blocks requirements or design.

### Gate B - Traceable Capture

Every decision has a source category:

| Category | Meaning |
|---|---|
| User answer | Confirmed by the user during `/requirements`. |
| Artifact | Taken from an attached/generated/project artifact. |
| Source-observed | Derived from framework source behavior. |
| Assumption | Explicitly assumed and reviewable. |
| Mock | Placeholder used to keep design/build moving. |

Gate passes when every design-relevant decision has a source category and unresolved assumptions are listed.

### Gate C - Open Questions

Every open question is classified by what it blocks:

| Class | Meaning | Required Action |
|---|---|---|
| Blocks requirements | Requirements are incoherent without this answer. | Resolve now. |
| Blocks design | Design cannot choose topology or responsibilities safely. | Resolve before `/design`. |
| Blocks build | Design can proceed, but commands cannot be finalized. | Mock if possible; resolve before final build. |
| Blocks deploy | Build and test can proceed, but production use is blocked. | Mock and carry as known risk. |

Gate passes when no open question blocks requirements or design.

### Gate D - Step Quality Checks

The quality checks in R1-R10 are the source of truth for completeness. Gate D passes when each check is satisfied or has a blocker recorded in `open_questions`.

If a quality check fails because required behavior is unknown, classify the blocker as requirements or design. If it fails because a concrete implementation detail is unavailable but the behavior is clear, classify it as build or deploy and apply the mock-and-proceed rule.
