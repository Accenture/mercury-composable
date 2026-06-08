# Requirements Gathering Recalibration Assessment

Status: working assessment.

Purpose: record why the requirements guide was recalibrated and what changed in the formal `/requirements` step.

## Assessment

The earlier requirements guide contained useful rigor, but it operated at the wrong altitude for a user-facing MiniGraph authoring workflow.

It read like a formal enterprise requirements governance document. The target workflow needs to feel like a model-guided graph authoring intake: conversational, graph-native, and strict about readiness without requiring the user to arrive with a full documentation packet.

The central mismatch was this:

- The older guide asked, "How do we produce accountable requirements documentation?"
- The desired `/requirements` step asks, "What does the graph need to know so it can be designed correctly?"

Both questions matter, but the second one must lead.

## What Was Worth Keeping

The following ideas were preserved and embedded into the formal step:

- Requirements must complete before `/design` begins.
- Requirements should not be drafted from vague input alone.
- Scope boundaries should be explicit.
- Every important state attribute should be understood.
- Open questions should be classified by what they block.
- Mock-and-proceed is valid for build/deploy unknowns that do not change topology.
- Decisions should be traceable.
- Requirements must end with testable scenarios.
- Reusable source/dictionary clusters may need special handling later.

## What Was Recalibrated

The following ideas were changed or softened:

- Mandatory formal input documents were replaced with plain-language user intent as a valid starting point.
- Mandatory `requirements.md` plus `glossary.md` outputs were replaced with one design-ready graph brief, which can be split later if needed.
- Heavy provenance for every sentence was replaced with traceable capture for decisions, assumptions, mocks, and source-observed framework behavior.
- Enterprise-specific examples and domain assumptions were removed from the core step.
- The glossary concept was reframed as a graph state contract.
- The review checklist was moved inside the phase as an embedded completion gate.
- The separate quality pass was removed; the gate is now the definition of done.

## Why The Gate Belongs Inside `/requirements`

A separate review pass invites drift. Human reviewers can reinterpret, rationalize, or accidentally contradict the original requirements capture. The safer pattern is to make the assistant maintain quality continuously while gathering requirements.

The formal step now has three movements:

1. Guided discovery.
2. Structured capture.
3. Embedded completion gate.

The gate determines whether `/design` may begin. If it fails, the assistant remains in `/requirements` and asks the narrowest questions needed to clear the blocker.

## Boundary Between `/requirements` And `/design`

The recalibrated rule is:

> `/design` may choose topology. `/requirements` must define the obligations that topology has to satisfy.

This means `/design` should not be responsible for discovering:

- Required inputs.
- Required outputs.
- Source contracts.
- Primary entity keys.
- Failure semantics.
- Design-blocking ownership decisions.
- Test scenarios.

`/design` can still refine graph-specific implementation tradeoffs, but it should receive a coherent brief.

## Open Questions Classification

The blocker classes were retained, with language adjusted for the workflow:

- Blocks requirements.
- Blocks design.
- Blocks build.
- Blocks deploy.

No question that blocks requirements or design can remain unresolved when `/requirements` completes. Build and deploy blockers can remain only if they have an explicit mock-and-proceed plan.

## Net Result

The formal `/requirements` step is now lighter at the entry point and stricter at completion.

It does not require the user to bring enterprise artifacts up front. It does require the assistant to produce a graph brief that is coherent, traceable, testable, and ready for design.