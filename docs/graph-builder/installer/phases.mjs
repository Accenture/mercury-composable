// The four phases of the graph-builder workflow, with their HAND-AUTHORED
// connective framing — the only part of a wrapper that is not lifted verbatim
// from the canonical spec (distribution-plan.md, D1 / decision 2).
//
// `purpose` and `invocation` are the framing prose. `spec` is the canonical
// source the wrapper links to (and the generator lifts `## Step checklist` +
// `## Gate` from). `prefix` + `phase` -> command name `graph-<phase>`.
// `engine` marks whether the phase needs a live engine + companion.mjs
// (per-command prerequisites — distribution-plan.md decision 5 / K5).

export const PREFIX = "graph";

export const PHASES = [
  {
    phase: "requirements",
    spec: "requirements-gathering.md",
    engine: false,
    purpose:
      "Turn rough intent into a design-ready MiniGraph brief: gather the behavior, contracts, dependencies, mappings, failure semantics, and test scenarios a graph must satisfy — before any topology is chosen. The phase is done only when the brief passes its gate.",
    invocation:
      "Run when starting a new graph or a substantive change to one. Produces the design-ready brief that /graph-design consumes. You do not need to know nodes, skills, or commands — this phase gathers obligations, not syntax.",
    argumentHint: "[optional: path to intent notes, a sample request/response, or a generated artifact]",
  },
  {
    phase: "design",
    spec: "graph-design.md",
    engine: false,
    purpose:
      "Convert a design-ready brief into a buildable MiniGraph architecture: node responsibilities, skill primitives, edges, state paths, control-flow and failure behavior, and the inspection points /test will use — without emitting any commands.",
    invocation:
      "Run after /graph-requirements passes its gate. Consumes the brief; produces the graph design spec that /graph-build lowers into commands.",
    argumentHint: "[path to the requirements brief]",
  },
  {
    phase: "build",
    spec: "build.md",
    engine: true,
    purpose:
      "Lower a design spec into executable MiniGraph commands, building the live graph one verified mutation at a time, then smoke-test that it instantiates and runs once on the happy path, and export it.",
    invocation:
      "Run after /graph-design passes. Requires a live engine + companion.mjs (see Prerequisites). Produces a build log + the exported graph. It does not re-choose topology (that is /graph-design) or prove full behavior (that is /graph-test).",
    argumentHint: "[path to the design spec]",
  },
  {
    phase: "test",
    spec: "test.md",
    engine: true,
    purpose:
      "Prove a built graph's runtime behavior: run each required scenario, assert the output contract with --expect, confirm the important state transitions by inspection, and record defects and accepted limitations.",
    invocation:
      "Run after /graph-build. Requires a live engine + companion.mjs (see Prerequisites). Produces a test report; passing it is the precondition for deploy.",
    argumentHint: "[graph name, or path to the build log]",
  },
];
