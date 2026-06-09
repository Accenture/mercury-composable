# Graph Builder — Improvement Plan

Status: active (under execution).
Owner: maintainer of `docs/graph-builder/` — set an explicit owner before others execute against it.
Lifecycle: archive when P0–P5 close; supersede with a new dated plan rather than silently rewriting history.
Assessed against: commit `14f84348` ("improvment plan written and perform P0-3"), working tree on branch `feature/companion-skill-framework`. P0–P3 are now committed; P4 & P5 pending.
Date: 2026-06-08.

This plan is grounded in a multi-pass review of the graph-builder doc set. It is written to be reviewable **without the originating conversation** — the current state is captured below with file references, and re-verification commands are included at the end so the snapshot can be re-grounded when it goes stale. If any "Verified current state" row no longer matches the tree, re-run the checks before trusting the rest.

---

## Verified current state (post-rollback)

A rollback reset tracked files to HEAD and removed the untracked second-round artifacts. The support files survived as untracked **first drafts**. This is the baseline the plan acts on:

| File | State | Evidence |
|---|---|---|
| `README.md` | **P2 applied (2026-06-08)** — Authority/Precedence + Terminology glossary added; primitive-table ownership declared | worklog W14 |
| `requirements-gathering.md` | **P3 applied (2026-06-08)** — Requirement IDs section + `requirements` catalog in template; structured carried_blockers | worklog W17 |
| `examples/customer-360-requirements-brief.md` | **P3 applied (2026-06-08)** — `requirements` catalog (INV/OUT/SRC/…); structured carried_blockers | worklog W17 |
| `graph-design.md` | **P3 applied (2026-06-08)** — `requirement_ids` in `source_brief` (+ existing `requirement_traceability`) | worklog W17 |
| `minigraph-syntax.md` | **P0/P1 corrections applied (2026-06-08)** — Reserved Skill Properties added (`exception`/`for_each`/`concurrency`/`extension`); null-check fixed; `model.zero`→`int(0)`; `session reset` documented; null-source-skip noted | worklog W11 |
| `companion.mjs` | **Aligned (2026-06-08)** — recognizes `session reset`; phantom bare `reset` removed | worklog W11 |
| `check-docs.mjs` | Removed by rollback | absent |
| `examples/customer-360-design-spec.md` | Removed by rollback | absent |
| `build.md` / `test.md` phase specs | **P5 CLOSED (2026-06-08)** — canonical `/build` + `/test` specs; reviewed/fixed (W22), re-checked SHIP (W23), validated by a no-context follow-along build+test with gaps fixed (W24) | worklog W21–W24 |

---

## Diagnosis

The doc system optimizes **internal coherence** well and is blind to two things:

1. **Engine truth.** Every "source-verified" fact (e.g. "no `number()`", concurrency defaults, fetcher-aborts-vs-extension-continues) is verified by *assertion*, never by *execution*. Drift from the actual MiniGraph engine is silent and undetectable.
2. **The unwritten phases.** `/build` and `/test` exist only as provisional gates in [workflow.md](./workflow.md).

Both are the center of gravity for the workstreams below.

**Caution before any of it:** this is substantial documentation infrastructure built before a single real graph has been authored through the workflow. The highest-value move is likely not more docs — it is to *run the workflow once* and let reality correct the docs. P0 reflects that.

---

## Workstreams

Each item lists: **Why** (grounded reference), **Cost**, **Done when**, **Timing**.

### P0 — Dogfood before polish *(do first; gates the rest)*
Drive `companion.mjs` to build and run the `customer-360` example against a live engine, end to end.
- **Why:** the trust boundary is unguarded; the cheapest way to find wrong syntax claims is to execute them. Doubles as research for the unwritten `/build` spec.
- **Cost:** low — engine runs on `localhost:8085`; `companion.mjs` already enforces verify-after-mutation.
- **Scope:** validate existing claims **and** probe the build-critical syntax the current docs lack (`exception`, `for_each`, `concurrency`) by experiment. This is deliberate — `customer-360`'s risk fallback needs `exception`, which isn't documented, so a pure "validate-existing" dogfood would never exercise it and P1 would have no evidence to document it (see Calibration Log: A→B handoff fix).
- **Outputs:** a dogfood run log and a **claim matrix** — `asserted claim | observed behavior | keep / change / remove` — written to `docs/graph-builder/evidence/dogfood-customer-360.md`. Decision gates reference this by path.
- **Done when:** the graph instantiates and runs once (happy path) **and** at least one negative path executes; every syntax claim touched during the build has an observed verdict recorded in the matrix.
- **Timing:** now. No other workstream's content should be re-added until it can be checked here.

### P1 — Fix what review already proved wrong, *verified not asserted* — ✅ APPLIED 2026-06-08 (W11)
Every item below landed in `minigraph-syntax.md` / `companion.mjs`, each backed by a P0 verdict in `evidence/dogfood-customer-360.md`.
- `model.zero`: undocumented; replaced with `int(0)`.
- `companion.mjs` `reset` ([companion.mjs:166](./companion.mjs#L166)): tool anticipates a command the syntax doesn't define (syntax has `clear cache` + the `RESET` *statement* only). Align tool and spec — let P0 decide the direction (does the engine support `session reset`?).
- Re-add build-critical syntax: `for_each`, `concurrency`, `exception`, `extension` target. Each claim must be confirmed in P0, not copied from the removed draft. (See Calibration Log — these were previously asserted, not verified.)
- **Cost:** low–medium. **Done when:** each fact has a P0 execution behind it and the known bug is gone. **Timing:** after P0.

### P2 — Coherence *(cheap, low-risk, reinstate)* — ✅ CLOSED 2026-06-08 (W14 applied · W15 fixed · W16 independently cleared, SHIP)
- Precedence rule: which doc wins on conflict; engine source trumps all docs.
- Glossary: `Decision` node-type vs decision-*branch*; type vs skill vs primitive.
- Reconcile the two primitive tables (graph-design's Source-Verified Primitives vs minigraph-syntax's Node Types) — one canonical, the other links to it.
- **Why:** no precedence rule and a real `Decision` term collision exist today. **Cost:** low (lean prose). **Done when:** README states precedence + glossary, and only one primitive table is authoritative. **Timing:** parallel with P0.

### P3 — Traceability spine *(keep the invariant, drop the machinery)* — ✅ CLOSED 2026-06-08 (W17 applied · W18 fixed · W19 simplified · W20 confirmed SHIP · W26 round-trip demonstrated); design-spec example regenerated, all 11 brief IDs traced bidirectionally
Reinstate requirement IDs as a **convention**: a catalog in the brief + `requirement_ids` in the design template, so "every requirement maps to a design element" becomes auditable.
- **Structured carried-blockers** (folded from the separate plan): replace string-only carried blockers with objects — `{ id, question_id, blocks, mock_used, replacement_required_before, closure_plan }` — so the design gate's "no build blocker remains carried at pass" is checkable rather than narrative.
- **Cost:** low. **Done when:** the convention is in both phase specs, the worked examples trace bidirectionally, and carried blockers use the structured shape. At most one lightweight check, with prefixes *derived* from the catalog. **Timing:** after P0/P2.
- **Not** the way `check-docs.mjs` did it — see Deletion List.

### P4 — Engine-truth anchoring *(defer)*
A small probe that *executes* the load-bearing engine claims, or runs the built `customer-360` in CI as an executable example.
- **Why:** the deep fix for the trust boundary. **Cost:** high — needs engine in CI, fixtures, and a `/build` spec. **Done when:** the probe **detects a deliberately injected drift** (assert the catch, not merely that a job *can* fail), and failures map to specific doc locations. **Timing:** after P0 is routine and `/build` exists. (See Calibration Log — timing downgraded from "now.")

### P5 — Write `/build` and `/test` specs — ✅ CLOSED 2026-06-08 (W21 written · W22 reviewed/fixed · W23 re-checked SHIP · W24 follow-along validated + gaps fixed); optional second confirming run remains
The actual hole. P0 will largely draft `/build`; `companion.mjs`'s verify-after-mutation protocol is its spine.
- **Cost:** medium. **Done when:** both phases have canonical specs replacing the provisional gates in [workflow.md](./workflow.md), **and two independent runs can follow each spec without extra guidance** (the reproducibility bar). **Timing:** `/build` alongside/after P0; `/test` after.

---

## Deletion list — do NOT resurrect

- The string-matching `check-docs.mjs` (regression-pins like `includes('model.zero')`, hardcoded ID prefixes, anchor-stripping link check). It tests the spelling of past fixes and gives false confidence.
- Example files as **required artifacts** in any gate. Examples are teaching material — check them *as examples* (if present, must trace), never require their existence.
- Any re-added engine fact not re-verified in P0. Copying asserted claims back is how the trust boundary stays broken.

---

## Recommended sequence

```
P0 ──┬──> P1 ──> P3 ──> P4
     └──> P2 (parallel)        P5 (/build alongside P0; /test after)
```

P0 first (cheap, corrects the others). P4 last (most expensive, depends on all above).

---

## Decision gates

Stop/go checkpoints between workstreams. Each names the artifact that must exist — by path — to proceed, so a gate can be *checked*, not argued from prose:

- **Gate 0 (before *any* workstream, mandatory):** re-verify the "Verified current state" table by running the commands in [Keeping this doc fresh](#keeping-this-doc-fresh-re-verify-the-state-table). If any row no longer matches the tree, update the table first. No workstream may begin against an unverified snapshot — freshness is a hard precondition, not end-of-doc advice. Especially required when more than one party is editing.
- **Gate 1 (after P0):** proceed only if `docs/graph-builder/evidence/dogfood-customer-360.md` exists and its claim matrix contains the touched-claim verdicts, including any mismatches.
- **Gate 2 (after P1 / P2):** proceed only if no contradiction remains between `companion.mjs` and `minigraph-syntax.md`, and `README.md` states the precedence rule.
- **Gate 3 (after P3):** proceed only if every `requirement_ids` reference in the design example resolves to a brief catalog id, and no `blocks: build` item remains in the design's carried blockers at gate pass.
- **Gate 4 (after P5):** proceed only if [workflow.md](./workflow.md) can drop its provisional `/build` and `/test` text in favor of the canonical specs.

---

## Calibration Log — revisions to prior review positions

Recorded so the plan carries its own corrections rather than presenting as uniformly confident:

- **Engine probe timing.** Earlier called "the single highest-leverage move, do it now." Downgraded to P4/deferred: it needs CI infra and a `/build` spec that doesn't exist. The insight (verify by execution) stands; the timing was wrong — don't build CI for a workflow never run by hand.
- **Reserved-properties section.** Earlier praised as "behavior, not just syntax." It was good prose but *unverified behavior* (concurrency 3/30, exception abort-vs-continue). Reinstatement is gated on P0 verification, not re-praise.
- **`check-docs.mjs`.** Earlier described as "documentation as a typed system, a strong durable pattern." Retracted: it was mostly tactical regression-pins and conscripted examples as required files. Only the bidirectional ID-traceability idea survives, and only as a lean convention (P3).
- **Folded in the separate execution-plan doc.** `improvement-plan-separate.md` proposed splitting these workstreams into seven standalone plans. The split was **rejected** — it created a second planning authority (the exact problem P2/precedence exists to prevent) on a multi-owner premise it never established. Its genuinely additive parts were folded here: per-workstream outputs and exit criteria, the Decision gates section, and the structured carried-blocker schema (P3). Three defects were **fixed on merge**, not carried forward: (1) the dogfood→corrections handoff — P0 now mandates *discovering* undocumented syntax, not only validating existing claims; (2) the engine-probe exit criterion — P4 now asserts drift *detection*, not merely that "a job fails"; (3) missing evidence locations — outputs and gates now carry file paths. The separate file was deleted to keep a single planning authority. (Logged as Incident 0002 in `docs/adversarial-review-checklist.md`: a self-review whose findings didn't bind its artifact.)

---

## Keeping this doc fresh (re-verify the state table)

The shell on the authoring machine has a shadowed `grep`; use Node to re-check state reliably:

```bash
cd docs/graph-builder
node -e "const f=s=>require('fs').readFileSync(s,'utf8');
const ms=f('minigraph-syntax.md'), cm=f('companion.mjs'), rm=f('README.md');
console.log('model.zero present:', ms.includes('model.zero'));
console.log('reserved props section:', ms.includes('## Reserved Skill Properties'));
console.log('bare reset case:', /case 'reset'/.test(cm));
console.log('README precedence:', rm.includes('Authority And Drift'));
console.log('design-spec example exists:', require('fs').existsSync('examples/customer-360-design-spec.md'));"
git rev-parse --short HEAD   # compare to 'Assessed against' above
```

If outputs differ from the state table, update the table and re-check which workstreams are still open before acting.
