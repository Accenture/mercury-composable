# Graph Development Workflow: Business Use Case → Deployed Graph

A domain-agnostic guide for translating a business use case into a working graph using the
[MiniGraph Playground Engine](https://accenture.github.io/mercury-composable/guides/CHAPTER-11/),
the open-source graph-based application modeling and execution platform from
[Accenture/mercury-composable](https://github.com/Accenture/mercury-composable). This guide stays at the
level of the engine itself, so the same workflow applies to any feature in any domain.

The workflow has four phases — **Requirements → Design → Build → Test** — followed by deployment. Each phase is a
distinct activity an AI coding agent (or a human) can carry out; the phases are tool-agnostic and do not depend on
any particular agent or IDE. The build phase is automatable through the Companion API and is the focus of the
companion syntax reference, [minigraph-syntax.md](./minigraph-syntax.md).

---

## Overview

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  1. Requirements│────>│  2. Design      │────>│  3. Build       │────>│  4. Test        │
└─────────────────┘     └─────────────────┘     └─────────────────┘     └─────────────────┘
  Issue / ticket          Diagrams              Live graph              Unit tests
  → requirements.md       → design.md           via Companion API       → *Test.java
  → glossary.md           → visual-graph-                               → mock infra
                            flow.text
```

Each phase produces artifacts that are consumed by the next phase. The flow is incremental — you can stop, inspect,
adjust, and resume between phases.

---

## Prerequisites

- MiniGraph Playground Engine running locally (default port `8085`)
- An active Companion / WebSocket session (the session ID is the suffix shown after the engine is started, used in
  `POST /api/companion/{session-id}`)
- A source of requirements: an issue tracker ticket, design doc, or business spec

The engine is included as a runtime dependency (`org.platformlambda:minigraph-playground-engine`) and exposes:

| Endpoint | Purpose |
|----------|---------|
| `POST /api/companion/{id}`        | Send authoring commands (text/plain body) |
| `GET  /api/graph/session/{id}`    | Inspect the live graph model in JSON |
| `GET  /api/inspect/{id}/{key}`    | Inspect a specific state-machine variable |
| `POST /api/graph/{graph_id}`      | Execute a deployed graph by ID |
| `POST /api/json/content/{id}`     | Upload JSON content to a session |
| `POST /api/mock/{id}`             | Upload mock data to the current graph instance |

Adjust `rest.server.port` in `application.properties` if you need to run on a different port.

---

## Step 1: Extract Requirements

**Input:** A requirements ticket, user story, or written spec describing the business question to implement.

**Example request to an agent:**
```
Extract complete requirements from the following ticket for MiniGraph graph development.

Feature name: subscription-renewal-check

Source:
<paste ticket text or link>
```

**What gets produced:**
```
assets/subscription-renewal-check/
├── requirements.md    ← User stories + GIVEN/WHEN/THEN acceptance criteria
└── glossary.md        ← Attribute definitions, types, sources
```

**What to review before moving on:**

- Does every business rule have its own requirement section?
- Does every acceptance criterion map to a testable scenario?
- Are edge cases covered (null, empty, missing data, unrecognized values)?
- Are external service contracts referenced explicitly (URL, method, request/response shape)?
- Is the glossary complete — do you know where every data attribute comes from (request body, external API, computed, constant)?

**Key decision at this stage:** Does this question need an extension call, or is it pure data retrieval, or pure
evaluation? The requirements should state this clearly. If unclear, ask the requirement author.

---

## Step 2: Design the Graph Architecture

**Input:** The `requirements.md` from Step 1.

**Example request to an agent:**
```
Design the complete graph architecture for the following feature.

Feature name: subscription-renewal-check

Use assets/subscription-renewal-check/requirements.md as input.
```

**What gets produced:**
```
assets/subscription-renewal-check/
├── design.md              ← Mermaid diagrams + interfaces + status codes
└── visual-graph-flow.text ← ASCII art of the graph structure
```

**`design.md` contains:**

1. Architecture Diagram — Manager + Extension subgraphs (Mermaid)
2. Sequence Diagram — Client → Manager → Services → Extension flow
3. Flow Diagram — Per-evaluator decision trees showing every branch
4. Component Interfaces — Extension input/output field contracts
5. Status Codes — Every result node mapped to a code and meaning

**What to review before moving on:**

- Does the architecture diagram show the correct graph type (manager vs extension)?
- Does the sequence diagram show all external service calls?
- Does the flow diagram cover every branch from the requirements?
- Do the component interfaces match what the manager will pass to the extension?
- Is every status code unique and meaningful?

**Key decision at this stage:** Which data comes from external APIs (needs Provider + Dictionary + Fetcher) vs
which comes from the request body (needs `mapper-input`)? This determines the manager graph structure.

---

## Step 3: Build the Graph

**Input:** The `design.md` and `visual-graph-flow.text` from Step 2.

The manager / extension split is the canonical pattern for non-trivial features:

- **Manager graph** — orchestrates data sourcing and calls the extension. Owns Provider, Dictionary, Fetcher,
  Joiner, and Extension nodes.
- **Extension graph** — pure evaluation. Receives all data via `input.body.*`. Contains a `mapper-input`,
  evaluator chain, and decision/result nodes. Never performs data sourcing.

For simple use cases, a single graph is fine — the engine's tutorials 1, 4, 7, 8, and 9 are all single-graph models.

**Example request to an agent (extension graph):**
```
Build the extension graph for subscription-renewal-check.

Use assets/subscription-renewal-check/design.md as the architecture reference.
The extension receives all data from the manager via input.body.

Session ID: {your-session-id}
```

**Example request to an agent (manager graph):**
```
Build the manager graph for subscription-renewal-check.

Use assets/subscription-renewal-check/design.md as the architecture reference.
Check existing dictionary graphs for reusable Provider/Dictionary nodes before creating new ones.

Session ID: {your-session-id}
```

**What this phase involves:**

1. Generate the command sequence based on the design
2. Send **one** command at a time to `POST /api/companion/{session-id}` (one command per request, plain text body)
3. **Verify that command before sending the next** — re-fetch `GET /api/graph/session/{id}` and confirm the node/connection actually landed. This is mandatory after every CRUD operation; never batch commands and verify once at the end (see the non-negotiable rule in [minigraph-syntax.md](./minigraph-syntax.md#mandatory-verify-after-every-crud-operation))
4. Test with `instantiate graph` → `run` → `inspect {output.body}`. **Reading `inspect` after `instantiate` and after `run` is mandatory** — both fail silently (`"accepted"` does not mean the instance was created or the run produced output). Confirm `input.body` is seeded after `instantiate`, and read `output.body` (or the graph's output key) after `run`

**What to review before moving on:**

- Does `describe graph` show all expected nodes and connections?
- Does `run` with test data produce the expected output for the happy path?
- Does `seen` show the expected traversal order?
- Do edge cases work (empty fields, null values, boundary numbers)?

**Reusing dictionary nodes:** If the graph needs data from an external service that other graphs already consume,
check the existing dictionary graphs first (e.g. `dict-{provider}.json`). Pull reusable nodes in with
`import node {node-name} from {graph-name}` rather than redefining Providers and Dictionaries.

When satisfied: `export graph as {graph-name}`.

---

## Step 4: Test the Graph

**Input:** The graph name (the JSON must be deployed to `src/main/resources/graph/`).

**Example request to an agent:**
```
Generate the unit tests for the graph ext-subscription-renewal-check.
```

**What this phase involves:**

1. Read the graph JSON and parse all nodes
2. Discover extensions recursively
3. Classify the mock strategy per service (in-memory vs file-based)
4. Check existing mock infrastructure
5. Run a graph integrity scan (typos, null-unsafe comparisons, type mismatches, unreachable branches)
6. Create missing mock infrastructure
7. Derive test scenarios from evaluator logic (happy path, null/empty, boundary values, all enum variants)
8. Generate a parameterized test class
9. Run tests — 0 failures required

**What gets generated (typical layout):**
```
src/test/java/.../start/
├── ExtSubscriptionRenewalCheckTest.java
└── mock/                       (if needed, per external service)

src/test/resources/
├── mock/                       (scenario-specific JSON overrides)
└── application.properties      (service host overrides for tests)
```

**Audit mode:** if the test class already exists, switch to audit mode — run the existing tests, scan for graph
integrity issues, trace data flow, produce a gap report, and wait for approval before changing anything.

**What to review:**

- Are all evaluator branches covered?
- Are null/empty/unrecognized scenarios included?
- Are boundary values tested for numeric comparisons?
- Do all tests pass?

---

## Step 5: Deploy

Once tests pass:

1. Ensure the graph JSON is in `src/main/resources/graph/{graph-name}.json`
2. If this is an extension called by a manager, the manager graph must also reference it (the `extension` property
   on the calling Extension node)
3. Run the full test suite (e.g. `gradlew clean test`)
4. Commit and push

The engine loads graphs from `location.graph.deployed` (default: `classpath:/graph`). Restart the application to
pick up new or modified graph JSON files.

---

## Workflow Variations

### Modifying an Existing Graph

Skip Steps 1–2 if the change is small (adding a new evaluator branch, fixing a condition):

```
I need to add a new policy check to ext-subscription-renewal-check.
Import the existing graph and add a check for {new condition}.

Session ID: {your-session-id}
```

Then audit the tests for `ext-subscription-renewal-check`. When a test class already exists, the testing phase runs
in audit mode — it reports gaps and proposes additions before changing anything.

### Building Only the Manager (Extension Already Exists)

```
Build a manager graph that sources data from {Service A} and {Service B},
then calls the existing extension ext-subscription-renewal-check.

The extension expects these inputs:
- account_status     (from Service A)
- balance            (from Service A)
- renewal_threshold  (from Service B)

Session ID: {your-session-id}
```

### Building Only an Extension (Manager Already Exists)

Build the extension first, in isolation, with hand-crafted `input.body` mock data via `instantiate graph`. Once it
behaves correctly under all scenarios, wire the manager to call it.

---

## Engine Capability Reference

The following skills are bundled with the engine — your graphs use them by setting the `skill` property on a node.
You do not need to register or install anything; they are available as soon as the engine starts.

| Skill | When to use |
|-------|-------------|
| `graph.data.mapper`  | Move data between `input`, `output`, `model`, and node namespaces |
| `graph.math`         | Decision-making, boolean ops, simple math (`IF/THEN/ELSE`, `COMPUTE`, `MAPPING`) |
| `graph.js`           | More flexible JS-engine evaluation (use only when `graph.math` is not enough) |
| `graph.api.fetcher`  | External API calls driven by Data Dictionary + Provider configuration |
| `graph.extension`    | Invoke another graph (or `flow://` event flow) as a sub-graph |
| `graph.island`       | Block traversal — used to organize configuration nodes (Dictionaries, Providers) |
| `graph.join`         | Synchronization point that waits for all upstream branches to complete |

The engine's bundled tutorials cover every one of these in isolation. Run `help.md` and `help tutorial 1.md`
through `help tutorial 12.md` inside the playground to walk through them interactively.

---

## Quick Reference: Which Phase When?

| I want to... | Phase |
|---|---|
| Extract requirements from a ticket | Step 1 — Requirements |
| Create architecture diagrams and flow charts | Step 2 — Design |
| Build or modify a graph via Companion API | Step 3 — Build |
| Generate unit tests for a graph | Step 4 — Test |
| Audit existing tests after a graph change | Step 4 — Test (audit mode) |

---

## Tips

- **Start with the extension.** Build and test the extension graph first (pure evaluation logic, no mocks needed for
  external services). Then build the manager that feeds it data.
- **Reuse Dictionary and Provider nodes.** Multiple graphs hitting the same service should share the same
  Provider + Dictionary configuration. Check existing `dict-*` / common graphs before creating new ones.
- **Test incrementally.** Don't build the entire graph before testing. Build root → first evaluator → first
  decision → end, test it, then add more branches.
- **Use `seen` for debugging.** If the graph doesn't reach the expected node, `seen` shows exactly which path was
  taken.
- **Export frequently.** Use `export graph as {name}` after each working milestone so you can roll back.
- **Naming matters.** Naming policy nodes after requirement / scenario IDs (e.g. `policy-scenario-4e`,
  `dec-reject-insufficient`) makes it trivial to trace test coverage back to requirements.
- **Verify after every CRUD operation — non-negotiable.** The Companion API only confirms *dispatch*
  (`"status":"accepted"`); a malformed or no-op command still returns `200`. After every single
  create/update/delete/connect, re-fetch `GET /api/graph/session/{id}` and confirm the change landed *before*
  sending the next command. Never batch. The [`companion.mjs`](./companion.mjs) `send` helper enforces this
  automatically and fails (`VERIFICATION FAILED`, non-zero exit) if a mutation did not take effect.
- **Verify the runtime after `instantiate` and `run` — also non-negotiable.** These are not CRUD, so the structural
  check doesn't cover them, yet they fail silently (a rejected seed line drops the whole instance → every `inspect`
  returns `404`). Always `inspect` after both: confirm `input.body` exists post-`instantiate`, and read the output
  key post-`run`. The `companion.mjs send` helper does this automatically — hard-failing on a dropped instance and
  printing `output.body` after a run — so the read is evidenced in the transcript, not assumed.
- **Send commands with the portable helper.** Use [`companion.mjs`](./companion.mjs) (`node companion.mjs send
  {session-id} --file <cmd-file>`) to author and `node companion.mjs graph {session-id}` to verify — it runs
  anywhere Node.js is installed, normalizes line endings to LF, and rejects the `...` placeholder automatically. See
  [minigraph-syntax.md](./minigraph-syntax.md) for the full request contract.
- **Avoid `...` in commands.** It is a documentation placeholder, not a command terminator. Including it literally
  causes `ERROR: Missing composite path`.
