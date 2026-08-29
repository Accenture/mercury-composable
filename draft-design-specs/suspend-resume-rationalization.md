# Suspend/Resume Rationalization — Design Review + Plan

> Review of the proposed re-design (Eric's `suspend-resume-rationalization.md`, 2026-08-07),
> assessed against the actual engine code in both lanes. Status: **awaiting Eric's rulings**
> (see §5). No implementation until ratified.
>
> Field context (kept generic per compartmentalization): a second field team evaluating
> suspend/resume reports the same conceptual wall the first team hit — a suspensible node
> ignores IF-THEN-ELSE routing and suspends unconditionally, which reads as an
> inconsistency in decision making. Two independent teams hitting the same wall within a
> week of each other is a design signal, not a documentation gap.

## 1. The proposal in one line

Retire `suspend=true`. Suspension becomes a **destination**, not a property: a node whose
traversal proceeds into the reserved `suspend` node (by drawn edge on `next`, or by an
IF-THEN-ELSE jump) is suspended there; `type=Suspensible` stays as UI color only.

## 2. What the code actually does today (verified 2026-08-07)

Both lanes (GraphTraveler dry-run / GraphExecutor deployed) are symmetric; line refs are
the traveler.

1. **`suspend=true` is only a routing trigger.** On a normal `next` completion,
   `nextOrJump` redirects a suspensible node to `walkToSuspendNode` (only the suspend
   node is walked; the drawn continuation edge stays dormant this run)
   (`GraphTraveler.java:376-381`).
2. **Persistence is already predecessor-agnostic.** `GraphSuspend` persists
   `node = FROM header` — whoever routed in — and never consults `suspend=true`
   (`GraphSuspend.java:71-75, 151`). The record contract `{cid, node, ttl, model, seen,
   run}` has no notion of the property.
3. **Resume is already predecessor-agnostic.** `GraphResume` returns
   `resume:<persisted-node>`; `resumeTraversal` marks that node seen + run and walks its
   forward links **excluding `suspend`** (`GraphTraveler.java:413-443`).
4. **Latent divergence A — the plain drawn edge.** The GraphSuspend javadoc, the skill
   help file, and `GraphModelValidator.validateContinuationEdge`'s comment all state "a
   plain connection into the suspend node is an unconditional suspension point" — but the
   walker actually **fans out**: `walkNext` walks ALL forward links, so a non-suspensible
   node with edges to {suspend, continuation} would checkpoint AND continue in parallel
   (incoherent — the run completes at the checkpoint while the branch keeps walking). The
   documented contract and the walker disagree today; the proposal would align them.
5. **Latent divergence B — the jump.** Nothing blocks a graph.math IF from resolving to
   `suspend` today. The jump works (persists `node = <decision>`), but resume then calls
   `walkNext` on the decision — walking **all its outcome edges in parallel**, which is
   wrong (a decision's edges are alternatives, not branches). So the proposal's item 2
   half-exists today with a broken resume.
6. **graph.math can stage the caller's reply.** Its `MAPPING:` statements use the shared
   data-mapping entry, so `-> output.body.x` targets work — a decision can set the
   "awaiting decision" reply itself before routing to suspend.
7. The gate currently enforces: suspend alias⇔skill both directions; suspensible ⇒ drawn
   checkpoint edge + a continuation edge + not graph.math/graph.js (the teaching error,
   4 sites); suspend node ⇒ task + ttl + a forward path.

**Conclusion of the code study: the proposal is far less radical than it looks.** The
persistence and resume layers are already shaped for it; `suspend=true` is a walker-level
trigger plus validation. The re-design is mostly *removing* a concept and *fixing two
latent divergences* the concept was papering over.

## 3. Assessment

**What the proposal fixes**
- The confusion both teams hit disappears structurally: a decision routes one outcome to
  `suspend` like any other outcome — no "decide before you suspend" rule to learn,
  because deciding and suspending compose in one node.
- The walker starts obeying the already-documented plain-edge contract (divergence A).
- The wait-loop pattern (tutorial-14's `await-decision` + `RESET`) collapses: with
  re-execute-on-resume for decisions (§4.3), "waiting" is just an edge to `suspend`, and
  the next request naturally re-runs the decision against the new input. The RESET
  wait-loop idiom shipped yesterday becomes unnecessary for this case.
- One less property, one less node type semantic, one less validator rule family.

**What the proposal under-specifies** (resolved in §4, ruled in §5)
- What "connects to suspend and returns next" means for fan-out (must become a redirect).
- What resume means when the persisted suspension point is a **decision** (its forward
  links are alternatives — the current walkNext contract cannot apply).
- Migration for field models carrying `suspend=true` (field-core surface, both engines).

**Why back-compat is structurally cheap:** every valid v4.11.x model necessarily draws
the checkpoint edge (the gate required it), so edge-inferred redirect reproduces today's
behavior exactly on old models — `suspend=true` degrades to a no-op. And the persisted
record contract is untouched, so records written by v4.11.x resume correctly under the
new engine (their `node` is a working node → continue-past semantics, same as today).
Mixed-version fleets are safe in both directions.

## 4. Resolved design (RATIFIED by Eric 2026-08-07 — R1-R6 accepted; R2 refined by Eric)

**The discriminator between the two suspension modes is graph shape — how the
suspension point relates to the suspend node — not skill class.** Eric's refinement:
this is cheaper (a forward-link lookup at resume, no skill classification, no statement
inspection at the gate) and it makes re-execution impossible to apply to a working node
by construction.

4.1 **Edge mode — the back-compat shape (drawn edge into suspend).** When a node's
skill returns `next` and `suspend` is among its forward links, the walker redirects to
the suspend node (today's suspensible behavior); the drawn edge is the declaration.
On resume: mark seen + run, **skip the suspend path, continue along all other forward
links** — exactly the current implementation, **no re-execution**. Multiple continuation
edges fan out on resume (parallel branches, joined later) — that works today and stays.
Gate (unchanged, structural, cheap): a node with an edge to suspend MUST have at least
one other edge — a suspend-only node is rejected regardless of skill, even a graph.math
whose IF-THEN-ELSE might route elsewhere (inspecting statement logic is deliberately out
of scope; the rule is shape-only). `suspend=true` is accepted and ignored (deprecation
WARN), `type=Suspensible` stays a color.

4.2 **Jump mode — the forward-looking best practice (no drawn edge; IF-THEN-ELSE jump).**
A graph.math IF resolving to `suspend` checkpoints with `node = <decision>`. On resume:
**re-execute the decision** — its persisted seen/run marks are not restored and
resumeTraversal walks the node itself, so it re-evaluates the NEW request input and
routes: approval proceeds, explicit rejection terminates, anything else jumps back to
suspend (re-suspension — the wait loop with no RESET idiom). The decision may stage
`output.*` first (e.g. the awaiting reply); otherwise the default
`{type: suspended, cid}` applies. By construction only routing skills can jump (working
skills always return `next`), so jump mode ⇒ routing skill without any classification.

4.3 **The two modes coexist in one graph.** tutorial-14 shows both naturally: the
order/approval/delivery working checkpoints keep drawn edges (edge mode, semantics
unchanged); check-approval jumps to suspend on the waiting outcome (jump mode) —
`await-decision` and the RESET statements are deleted.

4.4 **Island-anchored suspend (required companion of jump mode).** The export path
rejects orphan nodes ("a graph with orphan nodes cannot be exported" — help connect.md),
so a jump-only suspend node needs an incoming edge that plain traversal never crosses:
`root -> island-node -> suspend` (GraphIsland returns SINK, so the branch stops at the
island). This is the sanctioned anchor pattern; document it wherever jump mode is taught.
(P1 must locate the exact orphan-check site and confirm the anchor satisfies it.)

4.5 **Validator rewrite.**
- Drop: the suspensible property rules (the suspend=true routing-skill ban — the 4-site
  teaching error retires in its current form).
- Keep: suspend alias⇔skill both directions, task + ttl, suspend's forward path to end,
  resume-node task, the continuation-edge rule (now stated as 4.1's shape rule).
- Add: a routing-skill node (graph.math/graph.js) must NOT draw an edge to suspend —
  the teaching error's successor ("a decision reaches suspend by jumping from its
  IF-THEN-ELSE; draw edges to suspend only from working nodes") — because edge-mode
  resume would fan out a decision's outcome alternatives.
- Add: `suspend=true` present ⇒ deprecation WARN naming the drawn-edge replacement.
  `suspend` stays in RESERVED_PARAMETERS through the deprecation window.
- Add (scope cut, R7): `exception=suspend` rejected — checkpoint-on-failure would give
  re-execution retry semantics through the back door; revisit deliberately if a field
  case ever asks for it.

## 5. Rulings

| # | Question | Ruling |
|---|----------|--------|
| R1 | Edge into suspend = checkpoint redirect on `next`, never fan-out into suspend | **ACCEPTED** (Eric 2026-08-07) |
| R2 | Resume semantics | **REFINED + RATIFIED** (Eric): discriminate by shape, not skill — edge mode resumes past the checkpoint with NO re-execution (current behavior; continuation fan-out fine); jump mode re-executes the decision |
| R3 | Deprecation window for `suspend=true` (no-op + WARN) | **ACCEPTED** |
| R4 | Remodel tutorial-14 to the new grammar in the same release | **ACCEPTED** |
| R5 | ADR-0012 proposal partially superseding ADR-0010 vocabulary | **ACCEPTED** |
| R6 | graph.js: same jump semantics implicitly, zero investment | **ACCEPTED** |
| R7 | `exception=suspend` rejected at the gate (scope cut) | proposed with the R2 refinement — needs Eric's nod |

## 6. Phased plan (after ratification)

- **P0 — Ratify.** Eric rules on R1–R6; ADR-0012 drafted for the merge that lands P1.
- **P1 — Java engine.** Both lanes: edge-inferred redirect in `nextOrJump`/`walkTo`
  (replace `isSuspensible` checks with a forward-link probe), jump path unchanged (it
  already persists FROM), `resumeTraversal` bifurcation by skill class (incl. NOT
  restoring a routing node's seen/run marks in GraphResume — restoreMarks gains the same
  exemption it has for `suspend`), validator rewrite per 4.4, deprecation WARN.
- **P2 — Tests.** Matrix: v4.11.x compat model (suspend=true, unchanged behavior +
  WARN); edge-inferred working-node checkpoint; decision-jump checkpoint; wait loop via
  re-execution (two invalid rounds, then approve — the tutorial-14 e2e reshaped);
  v4.11.x-written record resumed by the new engine; decision staging output.* before
  suspend; suspension after a join (sole-active-branch warn unchanged); expired/absent
  record fresh path.
- **P3 — Docs + AI grammar (the surface rewrites as ONE story).** Guide (walkthrough +
  design rules: "route the waiting outcome to suspend" replaces decide-before-you-
  suspend; wait-loop RESET demoted to a generic-loop technique), tutorial-14 help, skill
  help, minigraph-commands.json suspend entry (byte-identical cross-engine), CHANGELOG;
  `npm run release` bundle regen (the Tutorials tab bakes help files — both repos).
- **P4 — Rust lock-step.** Full mirror (model_validator + traveler/executor lanes +
  resume skill + fixtures + e2e + docs + INCREMENTS row); byte-identical catalog entries
  and error strings; graph.js items excluded per phase-out.
- **P5 — Release.** Lock-step version on both engines; field validation invitation to
  both teams (their models keep working unmodified — the WARN guides migration);
  schedule the property's hard retirement for a later release.

## 7. Risk register

- **Field-core, regression-critical surface on both engines** (Eric's standing ruling).
  The compat matrix in P2 is the gate; the v4.11.x-record replay test is mandatory.
- **Behavior change for latent shapes:** a model that today draws a plain edge into
  suspend from a working node currently fans out (divergence A); under R1 it checkpoints.
  Judged acceptable: the fan-out behavior contradicts the shipped documentation and is
  incoherent (parallel complete+continue) — call it a bug fix, note it in CHANGELOG.
- **Docs churn:** this rewrites the grammar surface shipped 2026-08-07 (PR #263/#193).
  Mitigation: P3 lands as one coherent rewrite, not edits-on-edits.
- **Re-execution loop safety:** a decision that always routes to suspend re-suspends
  forever by design (each resume consumes + re-persists the record); the existing
  high-frequency loop detector still bounds a within-run cycle. No new loop hazard.
- **Sole-active-branch:** unchanged semantics (FROM-keyed warn).

## 8. Surface inventory (Java; Rust mirrors)

- `services/GraphTraveler.java`, `services/GraphExecutor.java` — redirect + resume
  bifurcation (drop `isSuspensible` from GraphLambdaFunction when the window closes)
- `skills/GraphResume.java` — restoreMarks exemption for a routing-skill suspension point
- `skills/GraphSuspend.java` — javadoc only (mechanics unchanged)
- `common/GraphModelValidator.java` — rule rewrite + deprecation WARN
- `resources/graph/tutorial-14.json` + `SuspendResumeTutorialTest` / Rust e2e twin
- `resources/help/help graph-suspend.md`, `help tutorial 14.md` (+ bundle regen)
- `docs/guides/knowledge-graph/workflow-suspension.md`, `minigraph-commands.json`
- `docs/arch-decisions/ADR.md` — ADR-0012 proposal (human-gated)
- CHANGELOG (Java) / CHANGELOG + INCREMENTS (Rust)
