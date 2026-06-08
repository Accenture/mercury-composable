# Graph Design Phase

Status: canonical phase spec for `/design`.

Purpose: convert a design-ready graph brief into a buildable MiniGraph architecture without emitting Companion API commands.

## Source And Boundary Rule

This document is based on the live MiniGraph source and the built-in help docs. The help docs are useful pattern descriptions, but the source is authoritative when the two disagree.

Primary runtime surfaces used by this phase:

- Traversal and deployed execution: `GraphTraveler`, `GraphExecutor`.
- Built-in skills: `GraphDataMapper`, `GraphApiFetcher`, `GraphMath`, `GraphJs`, `GraphJoin`, `GraphIsland`, `GraphExtension`.
- Shared mapping/control helpers: `GraphLambdaFunction`.
- User-facing syntax reference: [minigraph-syntax.md](./minigraph-syntax.md).

`/design` does not rediscover requirements and does not write command scripts. It chooses graph shape, node responsibilities, skill primitives, state paths, traversal behavior, failure behavior, and inspection points. `/build` lowers that design into commands.

## Core Principle

`/design` chooses a graph architecture that can be built and tested directly.

It should be specific enough for `/build` to create nodes, properties, edges, and mappings without inventing topology, but abstract enough that command syntax remains owned by [minigraph-syntax.md](./minigraph-syntax.md).

## Inputs

Required input:

- A design-ready graph brief from [requirements-gathering.md](./requirements-gathering.md#design-ready-brief-template).
- A passing requirements gate, where `gate_result.status` allows `/design` to begin.

Optional input:

- Existing graph JSON or a draft graph to modify.
- Sample payloads, source mock data, or recorded responses.
- Existing build commands or inspection output.
- Source or help excerpts for primitives that look ambiguous.

## Output Artifact

The phase outputs a graph design specification.

The graph design specification must answer:

- What graph or graph family is being built.
- Which requirements are satisfied by which design elements.
- Which node aliases exist and what each node owns.
- Which built-in skill, if any, each node uses.
- Which edges express natural traversal and which nodes may jump with `NEXT`, `IF`, `exception`, or `.sink` behavior.
- Which state namespaces and paths are read or written.
- Which external sources, data dictionaries, providers, extensions, or flows are used.
- Which errors are terminal, handled, mocked, or carried forward.
- Which inspection points `/test` must use.

## Runtime Facts That Shape Design

These are design constraints, not command syntax.

### Graph Traversal

- A runnable graph needs a root node and an end node.
- A node without a `skill` property is a pass-through node: traversal walks to its forward links.
- A node with a skill executes that skill and then continues according to the skill result.
- `next` means traverse natural outgoing edges.
- A returned node alias means jump to that alias.
- `.sink` stops that branch of traversal.
- The end node completes graph execution. In deployed execution, `output.body` becomes the response body and `output.header` becomes response headers.
- Non-join nodes are normally only walked once per graph instance unless reset. `graph.join` is special because it can be revisited until all upstream nodes complete.
- Loops are possible with `RESET` or jumps, but the runtime has high-frequency loop protection for tight rapid loops. Bounded loops that do real work are valid; design every loop with an exit condition and observable progress.

### State And Mapping

- State is a hierarchical map. Important namespaces include `input`, `model`, `output`, `result`, `header`, `response`, `path_parameter`, `query`, `cache`, and node aliases.
- Initial request data enters through `input.body` and `input.header`.
- Shared working data belongs in `model.*` when it must survive across nodes.
- Response data belongs in `output.body.*` and `output.header.*`.
- Node-local results normally live under `{node_alias}.result`, `{node_alias}.header`, `{node_alias}.status`, and `{node_alias}.error`.
- Mapping statements use `source -> target`; source can include constants, state paths, JSONPath-like selectors, and supported `f:` plugins.
- A normal mapper target must refer to an existing node namespace unless it targets `model.*` or `output.*` through a source-verified skill path.
- For `for_each`, the right-hand side must use the `model.*` namespace. Multiple `for_each` arrays must resolve to the same length.

### Node Property Initialization

- For nodes with a skill, reserved properties such as `skill`, `mapping`, `statement`, `input`, `output`, `feature`, `exception`, `extension`, `status`, `error`, `dictionary`, `for_each`, `concurrency`, and `purpose` are treated as configuration.
- Non-reserved properties on skill nodes are initialized into state as `{node_alias}.{property}`.
- Nodes without a skill initialize their whole property map into state under the node alias.

Design implication: use no-skill nodes for static configuration/catalog data when traversal should pass through or when another node needs a simple namespace. Use `graph.island` when traversal must not continue into that area.

## Source-Verified Primitives

| Primitive | Use when | Required design choices | Source-checked notes |
|---|---|---|---|
| No-skill node | You need a structural waypoint or a static state namespace. | Alias, type/purpose, properties, incoming/outgoing edges. | Traversal continues through forward links. Properties initialize under the node alias. |
| `graph.data.mapper` | You only need state transformation. | Mapping entries, source paths, target paths, downstream edge. | Requires at least one `mapping` entry and returns `next`. Prefer over math/js for pure mapping. |
| `graph.api.fetcher` | You need to call one or more HTTP providers through dictionary/provider nodes. | Fetcher node, dictionary nodes, provider nodes, input mappings, output handling, optional `for_each`, `concurrency`, `exception`. | Source requires a dictionary. Help says output is required, but source allows empty output when another node consumes `{fetcher}.result`. Provider nodes need `url` and `method`; dictionary nodes need `provider`. |
| `graph.math` | You need lightweight numeric or boolean compute, branching, reset, delay, or local statement composition. | Statements, result keys, branch targets, optional `for_each`. | At least one `IF`, `COMPUTE`, `EXECUTE`, `RESET`, or `DELAY` statement is required. Mapping/next alone is rejected. |
| `graph.js` | You need JavaScript object/array/scalar computation beyond `graph.math`. | Statements, result keys, branch targets, optional `for_each`, justification for using JS. | Heavier runtime than math. Supports JavaScript compute and truthy branching. Mapping/next alone is rejected. |
| `graph.join` | You need fan-in after parallel natural traversal. | All upstream edges that must complete, downstream edge. | Returns `next` only when all backward-linked predecessors are complete; otherwise returns `.sink`. |
| `graph.island` | You need a reachable node to stop traversal or a catalog/config node that must not execute downstream. | Alias, purpose, inbound edge if reachable, data held by properties if any. | Always returns `.sink`; source does not validate a `skill` property in the handler, but design should still set `skill=graph.island` for clarity and build consistency. |
| `graph.extension` | You need to call another graph or an Event Script flow. | Extension target, input mappings, output handling, optional `for_each`, `concurrency`, `exception`. | Target is a graph ID or `flow://{flow_id}`. Single-call mode requires input mappings. Output is optional if another node consumes `{extension_node}.result`. |

## Design Patterns

These are reusable patterns, not mandatory topologies.

### Linear Transform

Use when a request can be normalized, processed, and shaped without fan-out.

Typical shape:

```text
root -> normalize -> process -> shape-output -> end
```

Useful primitives: no-skill root/end, `graph.data.mapper`, `graph.math`, `graph.js` only when needed.

### Fetch And Shape

Use when the graph reads from one provider cluster and maps the result to the output contract.

Typical shape:

```text
root -> prepare-input -> fetch-source -> shape-output -> end
```

Useful primitives: `graph.api.fetcher`, dictionary nodes, provider nodes, mapper.

### Fan-Out And Join

Use when independent branches can run from the same upstream state and must converge before output shaping.

Typical shape:

```text
root -> branch-a -> join -> shape-output -> end
     -> branch-b -> join
```

Use `graph.join` only when every upstream branch must finish before the next step. If branches are conditional or optional, design explicit branch completion/fallback behavior before the join.

### Decision Branch

Use when runtime data chooses one of several paths.

Typical shape:

```text
root -> evaluate -> path-a -> end
              \-> path-b -> end
```

Useful primitives: `graph.math` for boolean/numeric decisions, `graph.js` for richer object decisions. Design the branch targets as node aliases that exist in the graph.

### Iterative Fetch Or Extension

Use when an upstream array drives repeated API or extension calls.

Typical shape:

```text
root -> fetch-list -> fetch-each-item -> aggregate-or-shape -> end
```

Use `for_each` only when the source path resolves to a list and the target is a `model.*` path. Choose `concurrency` deliberately; default behavior is parallel but capped by the runtime.

### Composed Graph Or Flow

Use when a cohesive capability already exists as another graph or Event Script flow.

Typical shape:

```text
root -> prepare-extension-input -> call-extension -> map-extension-output -> end
```

Use `graph.extension` for graph IDs or `flow://` targets. Treat the called graph/flow contract as a source dependency in the design.

### Catalog Or Non-Traversing Support

Use no-skill nodes for provider/dictionary/config namespaces that are read by another skill. Use `graph.island` when a node may be reachable but must stop the traversal branch.

## Operating Protocol

Run `/design` as an architecture decision phase, not a silent translation pass. The assistant should explain the chosen shape, the load-bearing tradeoffs, and any assumptions before locking the gate. For example, call out choices such as math vs JS, join vs sequential flow, single graph vs extension, and source-node output mapping vs later mapper ownership.

### D1. Accept Or Reject The Brief

Check that the `/requirements` artifact is design-ready:

- `gate_result.status` allows `/design` to begin.
- Requirements blockers are absent.
- Build/deploy blockers have explicit `carried_blockers` entries and mock-and-proceed plans.
- The brief includes invocation, output contract, sources, mappings, control flow, failure behavior, non-functional expectations, and tests.

The carried-blocker scope narrows by phase. `/requirements` may carry both build and deploy blockers forward. `/design` must resolve or mock every build blocker (via the Mock Or Placeholder Rule), so that only deploy-level replacements remain carried when this phase completes. This is why `gate_result.carried_blockers` here is deploy-only. Copy the incoming brief's `gate_result.carried_blockers` into `source_brief.requirements_carried_blockers`; those may include build items, which `/design` then resolves or mocks.

If design cannot proceed, return a short blocker list and do not invent topology.

### D2. Restate The Graph Obligation

Write a concise design goal:

- Primary graph responsibility.
- Invocation mode.
- Response contract.
- Required external dependencies.
- In-scope and out-of-scope behavior.

This restatement is not a new requirements phase. It is a checksum against the brief.

### D3. Choose The Graph Shape

Select the smallest shape that satisfies the obligations:

- Linear transform.
- Fetch and shape.
- Fan-out and join.
- Decision branch.
- Iterative fetch or extension.
- Composed graph or flow.
- Catalog/support island.

Name why the shape is needed. If two shapes are possible, choose the one with fewer runtime moving parts unless requirements demand parallelism, reuse, or isolation.

### D4. Build The Node Inventory

For every node, specify:

- Alias.
- Type/purpose.
- Skill or no-skill.
- Responsibility.
- Inputs read.
- State written.
- Upstream and downstream neighbors.
- Required properties at a design level.
- Requirements satisfied.

Aliases should be stable, human-readable, and command-friendly. Do not use command syntax here; save exact `create node` and `update node` blocks for `/build`.

### D5. Assign Skill Primitives

Choose skills by responsibility:

- Use mapper for pure data movement.
- Use math for lightweight compute, boolean decisions, reset, delay, and simple loops.
- Use JS only when math cannot express the needed computation clearly.
- Use API fetcher for provider-backed HTTP calls through dictionary/provider nodes.
- Use extension for graph or flow composition.
- Use join for required fan-in.
- Use island for deliberate traversal stop points.

Record any source/help mismatch that affects build behavior.

### D6. Design State And Mappings

Define the state contract as a design table:

- Initial read paths from `input.body`, `input.header`, `path_parameter`, or `query`.
- Working paths under `model.*`.
- Node-local result paths under `{node_alias}.result`.
- Output paths under `output.body.*` and `output.header.*`.
- Mapping ownership by node.
- Constants, defaults, selectors, and plugin functions needed.

Every target path should have one owner unless the design explicitly describes controlled overwrite behavior.

### D7. Design Source And Provider Clusters

For each external source:

- Identify whether it is a direct provider, dictionary-backed provider, extension graph, or `flow://` flow.
- Define required input parameters.
- Define response shape used by the graph.
- Define status/error behavior.
- Define mock strategy for carried blockers.
- Define whether result mapping happens in the source node or a later mapper.

For API fetchers, include dictionary/provider support nodes in the node inventory even if they are not part of the main traversal path.

### D8. Design Edges And Control Flow

Define all natural edges and all non-natural jumps:

- Natural traversal edges for `next`.
- Branch targets returned by `IF` or `NEXT` statements.
- Exception target nodes.
- Join upstream requirements.
- `.sink` branches and why they stop.
- Any reset/loop behavior and exit condition.

The design must avoid ambiguous fan-in. If a join waits for every backward link, every backward link must represent a branch that will complete in the scenario that reaches the join.

### D9. Design Failure And Fallback Behavior

For each source, decision, and composition boundary:

- Terminal errors.
- Handled errors and handler aliases.
- Default values or partial output behavior.
- Status/header propagation.
- Logged/inspectable state paths.

If an error is handled by `exception`, the handler should usually be a `graph.math` or `graph.js` decision node that inspects `{source_node}.status` and `{source_node}.error`.

### D10. Design Test Inspection Points

Translate `/requirements` scenarios into runtime checkpoints:

- Invocation payload.
- Expected final `output.body` and `output.header` paths.
- Intermediate node result paths to inspect.
- Decision nodes and expected branch decisions.
- Join behavior, if any.
- Failure paths and expected error/fallback state.
- Mock payloads needed for unavailable dependencies.

These inspection points become `/test` inputs and should be precise enough to avoid guessing after build.

### D11. Run The Design Gate

Before handing off to `/build`, present the proposed graph shape and load-bearing tradeoffs to the user or reviewer, then evaluate the design gate and record the result in the artifact.

## Graph Design Specification Template

Use this structure for the phase artifact.

```yaml
graph_design_spec:
  graph:
    name: ""
    purpose: ""
    invocation: ""
    selected_shape: "linear | fetch-and-shape | fan-out-join | decision-branch | iterative | extension | mixed"

  source_brief:
    requirements_artifact: ""
    requirements_gate_status: "{accepted requirements gate status; must be pass}"
    requirements_carried_blockers: []

  requirement_traceability:
    - requirement_id: ""
      design_elements: []
      notes: ""

  node_inventory:
    - alias: "root"
      type: "root"
      skill: null
      responsibility: "entry point"
      reads: []
      writes: []
      upstream: []
      downstream: []
      design_properties: {}
      satisfies: []

  edge_plan:
    natural_edges:
      - from: ""
        to: ""
        purpose: ""
    jumps:
      - from: ""
        condition_or_statement: ""
        target: ""
    sinks:
      - node: ""
        reason: ""
    joins:
      - node: ""
        required_upstream: []

  state_plan:
    input_paths: []
    model_paths: []
    node_result_paths: []
    output_paths: []
    mapping_ownership:
      - node: ""
        mappings: []

  source_plan:
    api_fetchers: []
    dictionaries: []
    providers: []
    extensions: []
    flows: []
    mocks: []

  control_flow_plan:
    decisions: []
    loops_or_resets: []
    concurrency: []
    completion: ""

  failure_plan:
    terminal_errors: []
    handled_errors: []
    fallback_outputs: []

  test_handoff:
    scenarios: []
    inspection_points: []
    mock_data: []

  build_handoff:
    command_sensitive_notes: []
    source_help_mismatches: []
    syntax_reference: "docs/graph-builder/minigraph-syntax.md"

  decisions:
    - id: "D-01"
      decision: ""
      source_category: "user-answer | brief | source-verified | tradeoff | assumption | mock"
      source_note: ""
  open_questions:
    - id: "OQ-001"
      question: ""
      blocks: "design | build | deploy"
      closure_plan: ""

  gate_result:
    # status reflects ONLY whether /build may begin:
    #   pass    = no open question blocks design or build
    #             (deploy-only items may remain, listed under carried_blockers)
    #   blocked = at least one open question blocks design or build
    status: "pass | blocked"
    blockers: []          # items blocking design or build; MUST be empty when status is pass
    carried_blockers: []  # deploy blockers carried forward, each with a mock-and-proceed plan
```

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

## Mock Or Placeholder Rule

When a dependency is unavailable, `/design` must keep the dependency visible in the source plan and add a mock plan. It must not erase the dependency by pretending the mock is the real source.

For each mock or placeholder, record:

- Real dependency name and contract.
- Mock node/source strategy.
- State paths the mock must produce.
- Tests that use the mock.
- The deploy blocker that must be resolved later. At `/design`, the mock resolves the build-level need so `/build` can proceed; the residual real-data replacement is a deploy blocker carried under `gate_result.carried_blockers`.

## Completion Rule

The phase is complete when the graph design specification has `gate_result.status: pass`, deploy-only carried blockers are recorded under `gate_result.carried_blockers`, and the design can be handed to `/build` without requiring `/build` to invent graph topology, primitive selection, state ownership, or failure semantics.

If the design is blocked, return the partial design plus a concise blocker list. Do not proceed to `/build`.