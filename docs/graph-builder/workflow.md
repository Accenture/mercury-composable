# Graph Development Workflow

Status: top-level phase index.

Purpose: define the graph development lifecycle and the boundaries between phase specs. This file is intentionally thin. Detailed behavior belongs in each phase document.

## Canonical Document Set

| Document | Status | Role |
|---|---|---|
| [workflow.md](./workflow.md) | Canonical index | Names the lifecycle and artifact chain. |
| [requirements-gathering.md](./requirements-gathering.md) | Canonical phase spec | Defines `/requirements` and the design-ready graph brief schema. |
| [minigraph-syntax.md](./minigraph-syntax.md) | Canonical syntax reference | Defines Companion API command grammar and build-time command constraints. |
| [requirements-recalibration-assessment.md](./requirements-recalibration-assessment.md) | Rationale note | Explains why `/requirements` was recalibrated. |
| [examples/customer-360-requirements-brief.md](./examples/customer-360-requirements-brief.md) | Example | Shows a filled `/requirements` brief using the canonical schema. |

Working notes under `system/minigraph-playground-engine/notes/` are scratchpads. They may explain how the workflow evolved, but they are not canonical phase contracts.

## Lifecycle

```text
1. /requirements -> 2. /design -> 3. /build -> 4. /test -> deploy
```

Each phase produces an artifact consumed by the next phase. A later phase should not rediscover the core obligations of an earlier phase.

## Phase 1 - `/requirements`

Essence: gather behavior, contracts, dependencies, mappings, failure semantics, and test scenarios before graph topology is chosen.

Canonical spec: [requirements-gathering.md](./requirements-gathering.md).

Input:

- Plain-language user intent, an existing graph/change request, a sample request/response, or generated artifacts.

Output:

- Design-ready graph brief using the schema in [requirements-gathering.md](./requirements-gathering.md#design-ready-brief-template).

Gate:

- The embedded requirements gate must pass before `/design` begins.
- No open question may block requirements or design.
- Build/deploy blockers require explicit mock-and-proceed plans.

## Phase 2 - `/design`

Essence: convert the design-ready graph brief into graph architecture.

Input:

- Design-ready graph brief from `/requirements`.

Output:

- Graph design specification. This should define node responsibilities, skills, edges, mappings, state paths, control-flow choices, error/fallback choices, and test inspection points.

Gate:

- Every requirement maps to a design element or an explicit open question.
- Every executable behavior has an implementation strategy.
- The design can be lowered into Companion API commands.
- Syntax-sensitive details are checked against [minigraph-syntax.md](./minigraph-syntax.md).

## Phase 3 - `/build`

Essence: lower the design into executable MiniGraph commands and verify the live graph incrementally.

Input:

- Graph design specification from `/design`.

Output:

- Build command script.
- Draft or exported graph JSON.
- Build log with verification notes.

Gate:

- Commands are sent one at a time.
- Every create, update, delete, and connect operation is verified before the next mutation.
- The graph can be instantiated.
- The graph can be exported.

Command grammar and verification details belong in [minigraph-syntax.md](./minigraph-syntax.md).

## Phase 4 - `/test`

Essence: prove runtime behavior through execution and state inspection.

Input:

- Built graph artifacts from `/build`.
- Test scenarios from `/requirements`.
- Inspection points from `/design`.

Output:

- Test report with scenario results, state inspection notes, defects, and accepted limitations.

Gate:

- Happy path passes.
- Required failure and edge scenarios pass.
- Output contract is satisfied.
- Important state transitions are confirmed.
- Remaining risks are explicit.

## Deployment Boundary

Deployment starts only after `/test` passes. Deployment concerns such as resource placement, environment configuration, release process, and production monitoring should not backflow into requirements unless they change graph obligations.

## Common Phase-Spec Skeleton

Future phase specs should follow this shape for consistency:

- Purpose.
- Boundary.
- Inputs.
- Output artifact and schema.
- Operating protocol.
- Step checklist.
- Gate.
- Mock or placeholder rule.
- Completion rule.

## Non-Goals For This Index

This file does not define canonical manager/extension patterns, Java test generation, feature-specific folder layouts, or graph topology preferences. Those are design/build/test decisions and should be introduced only when a phase artifact requires them.