# Task-level TTL override — catchable child timeouts

**Status:** ratified by Eric 2026-07-31 (investigation grounded by a 4-reader code study;
design + both decision points confirmed: name = `ttl`, compile bound + runtime WARN;
plus Eric's ruling: `model.ttl` becomes a guarded reserved key in the graph engine).

## Problem (field report)

TTL propagation copies the parent's FULL ttl into each child (subflow dataset ttl;
graph model copy; extension datasets), and each child re-arms a fresh full-length
timer at a later start — so the parent's deadline always fires first, and a flow's own
timeout bypasses exception handlers (TIMEOUT → abortFlow → 408 to the original
caller). A parent can therefore never catch a subflow / subgraph / API-fetcher timeout,
and cannot retry.

Two verified facts make the fix cheap:
- The child→parent 408 error channel already works end to end (abortFlow → task.executor
  → handleFunctionException → task-level `exception` or flow-level handler).
- EventScriptManager already honors a numeric dataset `ttl` over the flow template.

## Design

**Propagation stays the default** (top-down impedance matching). The per-task `ttl` is
an opt-in override at the invocation site — fixing catchability (child fails first,
parent alive to catch and retry within its remaining budget) and readability (the
deadline story of a composition is visible in one file).

### Event Script — per-task `ttl` on `flow://` tasks

- New optional task key `ttl` (duration string: `8s`, `2m` — flow.ttl grammar).
- Valid ONLY on sub-flow tasks (`flow://`); compile error elsewhere (a function task
  has no per-task watchdog — silent no-op is worse than rejection).
- Compile validation: parses via getDurationInSeconds, floor 1s, and MUST be
  `< flow.ttl` (the `delay < TTL` precedent).
- Runtime: TaskExecutor's flow:// dispatch uses task ttl when set, else
  flowInstance.getTtl(); WARN when task ttl >= the parent instance's EFFECTIVE ttl
  (rest.yaml may have shrunk it below compile-time flow.ttl).
- Task model: `private long ttl = -1` beside `delay` (same accessor pattern).

### Graph — per-node `ttl` on graph.extension / graph.api.fetcher / graph.task

- Optional node property `ttl`, SAME grammar as the suspend node's
  (`<digits>[s|m|h|d]`, parsed by GraphSuspend.getValidTtlSeconds) — semantics are
  skill-scoped: deadline for the child call on these three skills; store-record expiry
  on suspend (documented distinction).
- Overrides getModelTtl at the read sites: GraphExtension (:98, :193),
  GraphApiFetcher (:114, :167), GraphTask (:80) — both the child dataset ttl and the
  RPC wait. The extension's existing `exception` error path becomes reachable for
  timeouts.
- `ttl` stays NON-reserved in RESERVED_PARAMETERS (skill parameter, per the suspend
  precedent comment).
- Validation in GraphModelValidator (one rule covers the CompileGraph gate AND the
  playground `run` pre-check): grammar valid; allowed on suspend (mandatory, expiry) +
  the three deadline skills (optional); rejected on other skills.

### Model-metadata immutability guard (Eric's rulings, this round)

The graph engine currently allows a data mapping to rewrite `model.ttl` mid-run
(undocumented loophole; Event Script compile-blocks the same write). Eric's extended
ruling: guard the WHOLE model-metadata family, at both layers:
- **Family:** `model.{cid, instance, flow, ttl, trace, parent, root, none, run}` — one
  canonical constant on the graph side, kept aligned with GraphStateSkill's
  NON_PERSISTED_MODEL_KEYS (same nine names) and Event Script's RESERVED_MODEL_KEYS.
- **Compile/gate:** GraphModelValidator rejects any mapping entry whose RHS targets a
  reserved model-metadata key (CompileGraph gate + playground `run` pre-check).
- **Runtime immutability in BOTH walkers:** the shared mapping-application path
  (GraphLambdaFunction.validateRhs, executed by every skill in both the GraphExecutor
  and GraphTraveler lanes) rejects reserved-metadata write targets before the model.*
  short-circuit — dry-run drafts and any future mapping path are covered by
  construction.
- The per-node `ttl` is the sanctioned deadline mechanism.

## Also fixed in this round (doc/code divergences found by the investigation)

1. syntax.md claims flow.ttl can exceed the REST timeout — false: rest.yaml `timeout`
   overrides flow.ttl at the HTTP edge (payload-ttl-wins). Correct the prose.
2. flow-schema-reference.md (subflow section) claims "the parent's remaining TTL
   governs the subflow" — false: full value, restarted timer. Correct + document the
   new override.

## Out of scope (recorded, separate rulings)

- `delay` on flow:// tasks is silently dropped (pre-existing asymmetry).
- Orphaned children when a parent dies early (child runs to its own timer; late reply
  dropped) — mitigated by this feature, not eliminated.
- (superseded in-round: Eric ruled the WHOLE metadata family guarded, not just
  model.ttl — see the guard section above)

## Delivery

Event Script reference first, then graph side, docs (incl. divergence fixes),
adversarial review, Rust lock-step handoff (both surfaces are cross-engine contract).
Tests: compile rejections; catch-the-child-timeout e2e; a budgeted-retry e2e (the
field's use case); graph validator rules; skill override behavior; model.ttl guard.
