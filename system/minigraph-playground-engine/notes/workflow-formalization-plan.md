# MiniGraph Workflow Formalization Plan

Status: planning scratchpad. Canonical phase boundaries now live in `docs/graph-builder/workflow.md`; the canonical `/requirements` phase spec lives in `docs/graph-builder/requirements-gathering.md`.

Scope rule: this plan records the workflow direction and should not restate phase schemas. When this plan conflicts with canonical docs under `docs/graph-builder/`, the canonical docs win.

## Goal

Formalize a practical authoring workflow for MiniGraph so a user can move from intent to a tested graph in a predictable sequence:

1. Requirements Gathering
2. Graph Design
3. Build
4. Test

The workflow should be model-assisted, command-aware, and testable. Each phase should have a clear purpose, a bounded output artifact, and completion criteria.

## Phase 1: Requirements Gathering

Essence: gather behavior and contracts before graph syntax.

This phase answers what the graph must accomplish, who invokes it, what data enters and leaves, what systems it depends on, how data should be transformed, what failures mean, and how correctness will be proven.

Primary command concept: `/requirements`.

Inputs:

- User intent.
- Example requests and responses if available.
- Known source APIs, flows, graphs, or systems.
- Existing generated graph artifacts if available.

Activities:

- Clarify graph purpose and user outcome.
- Define invocation contract.
- Define output contract.
- Inventory sources and capabilities.
- Identify canonical `model.*` state.
- Capture mapping rules.
- Discover control flow and failure behavior.
- Capture non-functional constraints.
- Generate test scenarios.

Output artifact:

- Design-ready graph brief.

Completion criteria:

- The embedded `/requirements` gate in `docs/graph-builder/requirements-gathering.md` passes.
- No open question blocks requirements or design.
- Build/deploy blockers have explicit mock-and-proceed plans where applicable.
- The brief uses the canonical schema from `docs/graph-builder/requirements-gathering.md`.

## Phase 2: Graph Design

Essence: translate the graph brief into a graph architecture.

This phase decides which nodes, skills, properties, edges, and state namespaces should exist. It should still avoid executing build commands until the design can be inspected and explained.

Primary command concept: `/design`.

Inputs:

- Design-ready graph brief from `/requirements`.

Activities:

- Choose graph shape: linear, fan-out/fan-in, branch, loop, extension, or hybrid.
- Define node inventory.
- Assign each node a semantic type and runtime skill.
- Define node aliases and naming conventions.
- Define root input mapping.
- Define provider, dictionary, fetcher, mapper, decision, join, island, extension, and end responsibilities as needed.
- Define edge relations and traversal expectations.
- Define state contract for `input.*`, `model.*`, node-local state, `result.*`, and `output.*`.
- Define test inspection points.
- Identify unsupported or unresolved requirements.

Output artifact:

- Graph design specification.

Recommended sections:

- Overview.
- Node table.
- Edge table.
- State contract.
- Mapping table.
- Control-flow rules.
- Error/fallback rules.
- Test plan preview.
- Build risks and open questions.

Completion criteria:

- Every requirement maps to at least one design element or open question.
- Every executable node has a known skill.
- Every source has a provider/dictionary/fetcher or extension strategy.
- Every required output field has a source or derivation.
- Every branch/fallback has a target node.
- The design can be lowered to MiniGraph commands.

## Phase 3: Build

Essence: lower the design into executable MiniGraph commands.

This phase should generate concrete commands, execute them through the existing command channel, and verify the graph model incrementally. It should not hand-edit graph JSON as the primary authoring path.

Primary command concept: `/build`.

Inputs:

- Graph design specification.

Activities:

- Generate `create node` and `update node` commands.
- Generate `connect` commands.
- Execute commands one at a time through the command API.
- Verify after each meaningful group of commands using graph description or model export.
- Instantiate the graph with minimal mock input when ready.
- Export the graph under the agreed graph name.

Output artifact:

- Build command script.
- Draft or exported graph JSON.
- Build log with verification notes.

Completion criteria:

- Graph contains expected nodes.
- Graph contains expected connections.
- Root and end nodes exist.
- Required node properties are present.
- Graph can be instantiated.
- Export succeeds.
- Known build deviations are documented.

## Phase 4: Test

Essence: prove the graph behavior through runtime execution and state inspection.

This phase should validate behavior using the test scenarios from requirements. It should inspect both final output and intermediate state, because MiniGraph behavior is often mapping-driven.

Primary command concept: `/test`.

Inputs:

- Exported or draft graph.
- Test scenarios from `/requirements`.
- Build artifacts from `/build`.

Activities:

- Instantiate graph with scenario-specific input.
- Upload mock data when needed.
- Run graph.
- Inspect `input.*`, `model.*`, node-local state, `result.*`, and `output.*`.
- Validate final response body and headers.
- Validate branch, fallback, join, retry, and degraded-mode behavior.
- Record failures and feed fixes back to design or build.

Output artifact:

- Test report.
- Scenario results.
- State inspection notes.
- Defect list or accepted limitations.

Completion criteria:

- Happy path passes.
- Required failure scenarios pass.
- Required edge cases pass.
- Output contract is satisfied.
- Important state transitions are confirmed.
- Remaining risks are documented.

## Cross-Phase Principles

- Requirements are behavioral, not syntactic.
- Design is graph-aware, not command-first.
- Build is command-first, not JSON-edit-first.
- Test is runtime-aware, not only structural.
- State contracts are first-class.
- Mappings should be explicit and reviewable.
- Fallback behavior should be designed, not improvised.
- Each phase should emit an artifact that the next phase can consume.

## Assistant Responsibilities

The model should act as a phase guide:

- In `/requirements`, ask adaptive questions and produce a graph brief.
- In `/design`, make graph architecture choices and explain tradeoffs.
- In `/build`, generate and execute commands safely and incrementally.
- In `/test`, run scenarios, inspect state, and identify design/build defects.

The assistant should keep a running distinction between:

- Observed facts.
- User-provided assumptions.
- Inferred design choices.
- Open questions.
- Risks.

Each phase should also keep source-category traceability for decisions, assumptions, mocks, and source-observed framework behavior. This traceability model is defined in the `/requirements` spec and should be reused by later phase specs where applicable.

## Near-Term Work Items

1. Define the exact markdown shape for a graph design specification.
2. Create a small example that moves from `/requirements` brief to `/design` spec.
3. Create a command generation checklist for `/build`.
4. Create a runtime verification checklist for `/test`.
5. Write canonical `/design`, `/build`, and `/test` phase specs using the common skeleton in `docs/graph-builder/workflow.md`.