# Graph Builder — Work Log

Append-only record of work performed on the graph-builder doc system and its governance artifacts. Newest entries at the **bottom**. Each entry: date, what changed (files), why, and any open items carried forward. This log is for provenance and handoff — it is not a plan ([improvement-plan.md](./improvement-plan.md)) and not policy ([../adversarial-review-checklist.md](../adversarial-review-checklist.md)).

Convention: do not edit past entries except to mark an open item resolved (with a dated note). Corrections are new entries, not rewrites.

---

### W1 — 2026-06-08 — Anti-sycophancy review checklist created
- **Files:** created `docs/adversarial-review-checklist.md` (v1.0).
- **What:** adversarial evaluation checklist — Gates A–F, an Output contract, an Update Protocol, and Incident Log seeded with Incident 0001.
- **Why:** a self-review found I had over-appraised earlier doc edits (graded my own advice, treated presence as quality, laundered approval through abstractions). The checklist makes that failure mode catchable.
- **Open:** none at creation.

### W2 — 2026-06-08 — Improvement plan created
- **Files:** created `docs/graph-builder/improvement-plan.md`.
- **What:** prioritized, reviewable plan (P0–P5) with verified current-state table, deletion list, sequence, calibration log, and re-verification commands.
- **Why:** the prior round of doc edits was rolled back by the user; needed a grounded plan that reads without the originating conversation. Diagnosis: the doc set optimizes internal coherence but is untethered from the engine and missing the `/build`+`/test` phases.
- **Open:** none at creation; the plan itself tracks P0–P5.

### W3 — 2026-06-08 — Checklist v1.1: Gate G + Incident 0002
- **Files:** `docs/adversarial-review-checklist.md` → v1.1.
- **What:** added **Gate G ("Bind the findings")** + Output-contract item 7; logged **Incident 0002**.
- **Why:** while reviewing a proposed `improvement-plan-separate.md`, found it ran Gates A–F thoroughly in self-review, then shipped its full structure with none of its own kill-findings applied. Novel mechanism (a review can be honest yet change nothing), so it earned a gate rather than a tightening.
- **Open:** none.

### W4 — 2026-06-08 — Separate plan folded in and deleted
- **Files:** edited `docs/graph-builder/improvement-plan.md`; deleted `docs/graph-builder/improvement-plan-separate.md`.
- **What:** lifted the additive parts (per-workstream outputs/exit criteria, Decision gates section, structured carried-blocker schema for P3) into the main plan; deleted the separate file to keep one planning authority.
- **Why:** the split created a second planning authority (the problem P2/precedence exists to prevent) on an unestablished multi-owner premise. Three defects were **fixed on merge** rather than imported: P0 now mandates *discovering* undocumented syntax (not only validating); P4's exit criterion asserts drift *detection* (not "a job fails"); outputs/gates now carry file paths.
- **Open:** none.

### W5 — 2026-06-08 — Governance-quality pass on the two process docs
- **Files:** `docs/adversarial-review-checklist.md` (header + Gate E + Update Protocol step 2); `docs/graph-builder/improvement-plan.md` (header); created `docs/graph-builder/worklog.md` (this file).
- **What (applied):**
  - Checklist: added **Owner / Authority (process-vs-content precedence) / Amendment + version policy**; added a **Proportionality** floor (full gates for ship/policy artifacts; B+C+G for low-stakes); scoped **Gate E** so "no prior position" is a valid pass instead of a manufactured revision; gave the **Update Protocol** a concrete novelty test for adding gates (must exhibit an artifact that passes all current gates yet fails); collapsed the same-day version-churn string.
  - Plan: **Status** → active; added **Owner** and **Lifecycle** (archive on P0–P5 close).
- **Why:** a governance review found the layer had unstated ownership/authority, unenforceable anti-sprawl wording, provenance/version gaps, no proportionality floor, and a stale status. Self-indictments noted: I had added Gate G one turn after writing "resist gate sprawl," and had criticized the separate plan for no owner model while my own plan also lacked one.
- **Open (carried):**
  - **Finding 1 — separation of duties.** The only control is self-review, and Incident 0002 is evidence it can fail; incident-logging also depends on the offender self-reporting. ~~Decision pending.~~ **RESOLVED 2026-06-08 (W6):** independent reviewer required for ship-class artifacts.
  - **Proportionality of the whole layer.** Two governance docs + a log now govern a workflow that has not yet produced a single graph. Recommendation: do not expand governance further until **P0** (dogfood) runs.

### W6 — 2026-06-08 — Separation of duties adopted
- **Files:** `docs/adversarial-review-checklist.md` → v1.2 (new "Separation of duties" section; version-policy/changelog line corrected to cover deliberate amendments, not just incidents); this log.
- **What:** ship-class artifacts (those that ship externally or govern others — including these governance docs) now require an independent review by a reviewer who neither authored nor recommended them; for an agent, a fresh instance with authoring context withheld. Self-review remains a required pre-filter but is not sufficient. Incident-logging reassigned to the catcher, not the offender.
- **Why:** resolves W5 Finding 1 — self-review was the sole control and Incident 0002 proved it can fail; the logging trigger depended on offender self-report.
- **Consequence (flagged, not yet acted on):** by this rule the checklist and improvement-plan are themselves ship-class, so my self-reviews of them do not meet the bar — they need an independent pass. This raises the governance layer's cost; weigh against the standing proportionality caution before applying it retroactively to the existing docs.
- **Open (carried):** proportionality of the whole layer — unchanged; still recommend running **P0** before further governance.

### W7 — 2026-06-08 — P0 dogfood, first run (core paths)
- **Files:** created `docs/graph-builder/evidence/dogfood-customer-360.md` (claim matrix + run log). Live graph built in engine session `ws-844990-2` (not a repo file).
- **What:** built a runnable core graph (`root → mapper-input → check-id → shape-output|validation-error → end`) one verified mutation at a time, then ran happy + negative paths. Both pass: happy → `{customer_id, status:ok, degraded:false}`; negative (missing person_id) → `{error:validation_error, message}`. P0 "Done when" met for the runnable core.
- **Why:** P0 — verify doc claims against the live engine before correcting/adding syntax. Reality-first, as the plan demands.
- **Findings (detail in the evidence file):** the doc's `!= null` "direct null check" does **not** work — a `graph.math` IF halts silently on any unresolved `{...}` reference; working null detection is `:boolean(null=true)` in a **mapper**, not as a math `MAPPING:` statement. THEN/ELSE alias jumps both work when the condition resolves. Engine stores `types[]` though input is `with type`. Null-source mappings silently skip the target. The `THEN: next` semantics were not cleanly isolated (left open).
- **Open (carried):** remaining P0 scope **not yet probed** — Fetcher/Dictionary/Provider cluster (needs a mock HTTP endpoint), `exception`, `for_each`/`concurrency`, `graph.extension`, `graph.join`, numeric constants / "no `number()`", `model.zero`. These gate full P0 completion before Gate 1.

### W8 — 2026-06-08 — P0 dogfood, round 2 (fetcher / dictionary / exception)
- **Files:** created `docs/graph-builder/evidence/stub-server.mjs` (dependency-free HTTP stub on :8099); expanded `evidence/dogfood-customer-360.md` (claims 15–22). Live graph in `ws-844990-2` grown to 13 nodes / 10 connections.
- **What:** stood up a local stub and probed the data-dictionary cluster + `exception` against real HTTP. Profile fetch succeeds end-to-end (URL templating, `response.X` extraction, result→model→output). Risk fetch forced to 500: with `exception=risk-fallback` the handler runs and the graph completes degraded; the failed fetcher records `{status, error, target}`. Without `exception`, forward traversal stops but the upstream error body is auto-copied to `output.body`.
- **Why:** P0 — execution evidence for the build-critical, previously only-asserted `exception` property and the most complex documented pattern (data dictionaries).
- **Findings (detail in evidence file):** `exception` works as designed (now execution-backed). The island/"dictionary container" node is **not** required for a single fetcher. The design's "fetcher failures abort traversal" is too strong — the upstream error body surfaces to `output.body`.
- **Open (carried):** still pending before full P0 / Gate 1 — `graph.join`, `for_each`/`concurrency`, `graph.extension`, numeric/"no `number()`", `model.zero`. Stub server still running (background task `blc3cti1l`) and graph left intact for further probing.

### W9 — 2026-06-08 — P0 dogfood, round 3 (join + for_each + concurrency)
- **Files:** expanded `evidence/dogfood-customer-360.md` (claims 23–30). Live graph in `ws-844990-2` grown to 19 nodes / 16 connections (fan-out-join with a parallel accounts branch).
- **What:** restructured into a real fan-out-join and probed join semantics + iteration. `graph.join` waits for all backward-linked predecessors and proceeds when all complete; the **deadlock hazard is real** (wiring a never-completing branch as a predecessor `.sink`s the join), confirming the design's D-02 convergence requirement. `for_each` iterates a fetcher per list item (3 IDs → 3 HTTP calls); `concurrency` produces observable parallel/out-of-order iteration; `instantiate` supports array-append seeding.
- **Why:** P0 — validate the customer-360 design's load-bearing fan-out-join topology and the iterative primitives against the live engine.
- **Findings (detail in evidence file):** join + convergence pattern work as designed and the hazard is reproducible. `for_each` output via `output[]=...->model.x[]` produces nested arrays (flatten likely needs `f:listOfMap` — document it). Array-append seeding in `instantiate` is undocumented but useful.
- **Open (carried):** remaining before full P0 / Gate 1 — `graph.extension`/`flow://`, `graph.js`, numeric/"no `number()`", `model.zero`. Stub (`blc3cti1l`) and graph left intact.

### W10 — 2026-06-08 — P0 dogfood, round 4 (js / numeric / model.zero / extension) — P0 COMPLETE
- **Files:** expanded `evidence/dogfood-customer-360.md` (claims 31–39; consolidated P1 implications). Stub torn down.
- **What:** `graph.js` executes JS (`1+1→2`) and returns node-level `next` (traversal continued) — but a malformed expression (`$.model.x.length`) halts silently like graph.math. `int()`/`double()` work; `number(7)` is rejected and drops the whole instance (confirms "no `number()`"). `model.zero` is **undefined** (bug confirmed; `int(0)` is correct). `export`/`import` by name and `session reset` all work. `graph.extension` node + error propagation confirmed, but its target resolves from a **deployed-graph registry**, not the `export`/`import` name store (`extension=customer-360-sub` → "not found" 400) — a successful sub-graph invocation isn't testable via playground export.
- **Why:** finish the remaining P0 primitives for completeness.
- **P0 status:** **complete.** Every targeted claim has an execution verdict; the only partial is `graph.extension` success-path (needs a deployed sub-graph). Consolidated doc-correction list captured in the evidence file's "Immediate doc implications" — feeds P1. Gate 1's artifact (`evidence/dogfood-customer-360.md`) now exists with mismatches recorded.
- **Open (carried):** proportionality of the governance layer (unchanged). Next per plan: **P1** apply the corrections, **P2** coherence — both now backed by execution evidence. Finding 1 (separation-of-duties owner) and Gate-0 freshness (reviewer Finding 1) still pending decisions.

### W11 — 2026-06-08 — Gate 0 added + P0 corrections applied (P1)
- **Files:** `improvement-plan.md` (new mandatory **Gate 0** freshness precondition; state table + P1 marked applied); `minigraph-syntax.md` (corrections); `companion.mjs` (reset alignment).
- **Gate 0 (reviewer Finding 1, High):** added a mandatory pre-workstream gate requiring the "Verified current state" table to be re-verified before any workstream begins; freshness is no longer end-of-doc advice. Ran it before applying corrections (state was still fresh at HEAD `6cdacc1b`).
- **P1 corrections applied to `minigraph-syntax.md`** (each backed by a P0 verdict): removed the false `!= null` "direct null check" and documented silent-halt-on-unresolved-IF + node-level `next` traversal; rewrote Null Checking to use a mapper (not an inline math statement); `model.zero`→`int(0)`; added `session reset` + array-append `instantiate` seeding; added a **Reserved Skill Properties** section (`exception`, `for_each`+`concurrency`, `extension`) with the verified nuances (error→output.body, for_each nesting + `f:listOfMap`, extension deployed-registry distinction); noted the dictionary island container is optional and that null-source mappings skip the target.
- **`companion.mjs`:** recognizes `session reset`; removed the phantom bare `reset` case.
- **Not changed:** `graph-design.md`'s join-hazard / convergence guidance — P0 *confirmed* it (claim 25), so no edit needed.
- **Note (governance):** `minigraph-syntax.md` is ship-class (governs contributors); per W6 it warrants an independent review before it "ships." These edits have had author self-review only — flag for an independent pass.
- **Open (carried):** independent review of the corrected spec (ship-class, W6); separation-of-duties owner; proportionality of the governance layer.

### W12 — 2026-06-08 — Independent review of the corrected spec + dispositions applied
- **Files:** `minigraph-syntax.md` (8 fixes), `evidence/dogfood-customer-360.md` (claims 40–41; applied-status note), this log. Resolves the W11 carried "independent review" open item.
- **What:** spawned a fresh subagent (no conversation context) to satisfy the W6 separation-of-duties rule — a genuinely independent reviewer, not author self-review. It applied the adversarial checklist to `minigraph-syntax.md`.
- **Findings it caught that author self-review missed:**
  - **`with type` is cosmetic for skill nodes** — re-tested live; omitting it stores `types: ["untyped"]` and the node still runs. Author never noticed (always wrote `with type`). Now documented (claim 41).
  - **Untested cure shipped** — `f:defaultValue` was prescribed as the null-skip remedy without being run. **Verified live this turn** (claim 40): present→source, null→fallback. Real.
  - **Overclaims introduced in W11** — `f:listOfMap` written as prescription vs evidence's "likely" (K2); `next` semantics stated definitively while evidence #14 marks them open (K3).
  - **Over-broad "verified" voice** — function table, `model.none`, statement verbs documented as fact without execution backing (K1).
- **Dispositions applied:** verified `f:defaultValue` live; added a verification legend (verified vs asserted); marked `with type` optional/cosmetic for skill nodes + the create-vs-update delimiter; restored the `f:listOfMap` "likely" hedge; marked `next` open; relabeled the function table / `model.none` / statement verbs as asserted-untested. **Overridden** (reviewer deletion-budget #6): did not delete the untested commands — relabeled instead, since they're plausibly real.
- **Meta:** second time independent/external review caught the author overclaiming (cf. Incident 0001). Separation-of-duties (W6) paid for itself on first real use — a self-review would not have found the `with type` bug.
- **Open (carried, now as debt):** execution-test the function table beyond `f:defaultValue`, `model.none`, and the statement verbs; `graph.extension` success path; separation-of-duties owner; governance proportionality.

### W13 — 2026-06-08 — Independent re-check: all findings CLOSED, ship verdict
- **What:** a second independent reviewer (fresh agent — `SendMessage` to resume the first wasn't available in this environment, so a no-context fresh instance was given the prior findings to verify closure; arguably a stronger check) re-reviewed the corrected `minigraph-syntax.md`.
- **Result:** all seven prior findings (K1–K4, F5, F6, `f:defaultValue`) marked **CLOSED** with file:line evidence; no contradictions, dead anchors, or overclaims introduced; no claim newly tagged "(verified)" that the evidence doesn't support. **Ship verdict: SHIP** — safe for other engineers to rely on.
- **Two minor, non-blocking flaws (accepted):** (a) table rows carry no inline asserted tag — they rely on the trailing blanket notes + the global legend, so a row copied out of context loses the qualifier (fragility, not a false claim); (b) the instantiate "(All verified…)" parenthetical sits near untested `map/file/classpath`. Both dispositioned by the reviewer as overridden-minor; left as-is to avoid disproportionate gold-plating.
- **One non-blocking follow-up:** a live `model.none` test would convert F6's last hedge into a verdict — already on the debt ledger.
- **Loop closed:** the W6 separation-of-duties requirement for this ship-class spec is now satisfied (independent review + independent re-check, both by no-context instances). The W11 "independent review" open item is fully discharged.

### W14 — 2026-06-08 — P2 coherence (precedence + glossary + table ownership)
- **Files:** `README.md` (Authority And Precedence + Terminology sections; working docs added to the index), `graph-design.md` (Source-Verified Primitives cross-ref note). Ran Gate 0 first — state fresh at HEAD `6cdacc1b`.
- **Authority/Precedence:** explicit conflict-resolution order — engine source/observed behavior > `minigraph-syntax.md` (syntax + canonical node-type/skill enumeration) > `requirements-gathering.md` (brief schema) > `graph-design.md` (primitive selection) > `workflow.md` (phase order); process docs (checklist/plan/worklog) advisory, not authoritative over content.
- **Terminology glossary:** resolves the `Decision` collision (node-type mapper vs decision *branch*), type vs skill vs primitive, and Provider/Dictionary/Island — incorporating the P0 finding that `with type` is cosmetic for skill nodes.
- **Primitive-table reconciliation:** `minigraph-syntax.md`'s **Node Types** declared the canonical type/skill enumeration; `graph-design.md`'s **Source-Verified Primitives** relabeled as selection guidance that defers to it — both tables now have a single owner, removing the drift risk. P2 "Done when" met.
- **Note (governance):** `README.md` is ship-class (governs contributors); per W6 these edits warrant an independent pass before they "ship." Low-risk coherence prose (mostly reinstating previously-reviewed content) — flagged for an independent review rather than auto-commissioned, to respect proportionality. Decide whether to commission.
- **Open (carried):** independent review of the P2 README/graph-design edits (ship-class, W6); separation-of-duties owner; governance proportionality; verification debt (function table, `model.none`, verbs, extension success path).

### W15 — 2026-06-08 — Independent pass on P2 edits → NO-SHIP (K1) → fixed
- **What:** fresh subagent (no authoring context) reviewed the P2 README/graph-design edits via `git diff`. Verdict: **NO-SHIP until K1 fixed**; everything structural held up.
- **Confirmed sound (kills failed):** all new links/anchors resolve (K6); the precedence order doesn't contradict the other docs; the two primitive tables genuinely agree today, so the "single owner" claim isn't hollow (K3); the Provider definition is accurate (K2).
- **K1 (blocker) — FIXED:** glossary "Node type" said "cosmetic for skill nodes" but used `Root` (a no-skill node where type is load-bearing) as an example — could mislead an author into treating `with type Root` as decorative. Reworded: cosmetic for **skill** nodes, **load-bearing** for `Root`/`End`/no-skill; examples changed to skill-node types.
- **Also applied:** K4 — tier-1 precedence now says to weight each evidence row by its own verdict (partial/untestable rows aren't conclusive); deletion-budget #3 — Island/Entity definition tightened to the `graph.island` `.sink` behavior; #2 — dropped the duplicative "headers/features" from Provider; K5 — `requirements-recalibration-assessment.md` now named as a non-authoritative rationale doc in the precedence section.
- **No change (kills failed):** K2, K3, K6.
- **Meta:** third independent review, third real catch (Incident 0001 self-grading; W12 `with type` bug + untested cure; now W15 `with type`/`Root` glossary mislead). Notably K1 is the *same `with type` subtlety* that bit the spec in W12 — it propagated into the glossary and only an independent eye caught the recurrence. Separation-of-duties continues to earn its place.
- **Open (carried):** re-check that K1/K4 fixes close the findings (optional, NO-SHIP was conditional on K1); separation-of-duties owner; governance proportionality; verification debt.

### W16 — 2026-06-08 — P2 independently re-checked: SHIP → P2 CLOSED
- **What:** fresh subagent re-checked the K1/K4 + budget fixes against the current `git diff`. **Verdict: SHIP** — K1 (blocker) and all four recommended findings **CLOSED** with file:line evidence; the previously-failed kills still hold (links/anchors resolve, precedence consistent, the two primitive tables agree so "single owner" is real).
- **Residual (accepted, non-blocking):** glossary Provider/Dictionary rows paraphrase definitions `minigraph-syntax.md` owns — a drift risk, not a contradiction (e.g. Node Types says Provider "(url, method, headers)" vs glossary "(url, method, request-param placement)"). Reviewer overrode as non-blocking; left as-is per proportionality. Carried as a minor drift note.
- **P2 status: CLOSED** — applied (W14), fixed (W15), independently cleared (W16). Full author→review→fix→re-check loop, all reviews by no-context instances.
- **Open (carried):** separation-of-duties owner; governance proportionality; verification debt; glossary paraphrase-drift (minor).

### W17 — 2026-06-08 — P3 traceability spine (convention, not machinery)
- **Files:** `requirements-gathering.md` (new "## Requirement IDs" section + `requirements` catalog in the brief template + structured `carried_blockers` schema + completion-rule point 6), `graph-design.md` (`requirement_ids` added to `source_brief`), `examples/customer-360-requirements-brief.md` (filled `requirements` catalog INV/OUT/SRC/MAP/FLOW/FAIL/NFR/TEST + structured carried_blockers as CB-01/CB-02). Ran Gate 0 first — state fresh at HEAD `6cdacc1b`.
- **What:** reinstated stable requirement IDs as a lightweight convention so `/design` traces each obligation without re-deriving it, and replaced string-only carried blockers with structured objects (`id`, `question_id`, `blocks`, `mock_used`, `replacement_required_before`, `closure_plan`).
- **Deliberately did NOT** build a checker — the spec note says "do not rebuild a string-matching doc checker," honoring the deletion-list ban (the rolled-back `check-docs.mjs` was the anti-pattern) and proportionality.
- **Partial (honest):** P3 "Done when" includes "worked examples trace bidirectionally." The brief example now carries the catalog and the design *template* has `requirement_ids` + `requirement_traceability`, but the **design-spec example was rolled back/deleted**, so the round-trip can't be *demonstrated* in examples. The convention is fully in both specs; only the worked-example round-trip is incomplete.
- **Note (governance):** `requirements-gathering.md` and `graph-design.md` are ship-class specs; per W6 these edits warrant an independent pass. Flagged, not auto-commissioned (proportionality) — decide whether to commission.
- **Open (carried):** regenerate the customer-360 design-spec example to demonstrate bidirectional traceability (debt); independent review of P3 spec edits (ship-class, W6); separation-of-duties owner; governance proportionality; verification debt.

### W18 — 2026-06-08 — Independent review of P3 → NO-SHIP → fixed
- **What:** fresh subagent reviewed the P3 edits. **Verdict: NO-SHIP** — the structured carried-blockers half was correct (field-for-field schema match, cross-refs hold), but the requirement-ID half had real defects.
- **Findings + dispositions:**
  - **K2 (must) — FIXED:** I had written that `/design`'s gate "fails if a requirement maps to no design element **and has no open question**" — an exemption that **does not exist** in the real design gate (graph-design.md). Reworded to match the actual gate (every requirement maps to ≥1 design element; unavailable deps carried as blockers). A cross-doc contradiction I introduced.
  - **K1 (must) — FIXED:** the round-trip was stated present-tense though no design-spec example demonstrates it. Softened to "should echo" + explicit note that the round-trip isn't yet demonstrated end-to-end.
  - **K5 (must) — FIXED:** the canonical example violated the spec's own "every design-relevant obligation has an ID" — missing the **profile/accounts stop-vs-degrade** distinction (added FAIL-003) and the **latency/concurrency** NFR (added NFR-002), plus a granularity clause so "every" means material obligations grouped sensibly.
  - **K4 (should) — partially fixed:** the example's "Derived from generated artifact" provenance contradicted its own "schema example" disclaimer → added an illustrative-provenance comment. FLOW-001's `assumption` label left as-is (its source_note already scopes the assumption to parallelism) — minor.
  - **K3 — overridden:** gate doesn't name the new fields, but "convention, not machinery" is the deliberate choice; acceptable once K1/K2 fixed.
- **Meta (own it):** this is the **4th** independent review and the **4th** real catch. Worse, the root cause here is specific: I reinstated P3 partly by **copying the rolled-back version's catalog/source_notes without re-verifying** — propagating pre-existing defects. That is exactly the anti-pattern the plan's Deletion list bans ("any re-added fact not re-verified… copying asserted claims back is how the trust boundary stays broken"). I violated my own documented principle. The pattern across W12/W15/W18: my first-pass quality on spec prose is mediocre and the independent reviewer is doing real load-bearing work, not ceremony.
- **Open (carried):** re-check that K1/K2/K5 fixes close the findings; regenerate the design-spec example (debt); separation-of-duties owner; governance proportionality; verification debt.

### W19 — 2026-06-08 — P3 re-check (over-engineering lens) → NO-SHIP/SIMPLIFY → simplified
- **What:** re-check requested with an explicit non-sycophancy mandate (catch over-justification / over-complication / anchoring). Verdict: **NO-SHIP + SIMPLIFY** — and it was right.
- **Confirmed:** K2 cleanly closed (gate wording matches the real gate); K1 honestly softened.
- **Caught (legitimate, acted on):**
  - **Granularity clause was a hedge** — it made "every obligation has an ID" unfalsifiable; I'd added it to dodge K5, not fix it. **Cut**; reworded the claim to "capture the obligations /design must trace; not a completeness checklist."
  - **FAIL-003 fabricated an "accounts" stop-condition** the brief never states (only profile is a stop condition) — appeasement padding. **Narrowed to profile-only.**
  - **`requirement_ids: []` (graph-design)** duplicated `requirement_traceability` keys — a parallel truth surface (Incident 0002 pattern), existing only to be diffed (which the convention disavows). **Cut.**
  - **6-field `carried_blocker` over-modeled** — `blocks`/`closure_plan` duplicate the referenced open question; `replacement_required_before` == `blocks` in every instance. **Reduced to `{question_id, mock_used}`** (the one non-duplicated datum is the mock).
- **My own judgment (did NOT take the most aggressive cut):** the reviewer recommended collapsing the catalog to bare inline `id: statement`. I kept `id`/`statement`/`source_category`/`source_note` — `source_category` is a real trust signal for `/design` (which obligations are soft assumptions vs. facts), and the schema is low-harm. Swinging all the way to inline would be anchoring to the reviewer rather than judging. Stated so the choice is visible, not silent.
- **Loop-termination note:** P3 has now had two NO-SHIP reviews; the second concluded the apparatus was over-built and I'd been accreting reactively. The fix this round was **subtraction**, not more patching — which is the correct response to "over-justified accretion." Further review rounds should not add; if the next re-check isn't clean, the answer is more cutting, not more structure.
- **Open (carried):** optional final confirm that the simplification is coherent; regenerate design-spec example (debt); separation-of-duties owner; governance proportionality; verification debt.

### W20 — 2026-06-08 — P3 final confirm: SHIP → P3 CLOSED
- **What:** tight final coherence confirm (subtract-don't-add mandate). **Verdict: SHIP**, all 5 items CLOSED, no remaining defects: `requirement_ids` removed with no dangling ref (`requirement_traceability` remains); `carried_blocker` is `{question_id, mock_used}` in both template and example and they match; granularity hedge gone (no falsifiable completeness claim); FAIL-003 profile-only (matches stop_conditions); no new contradictions; carried_blocker question_ids reference real open questions.
- **P3 status: CLOSED** — convention is in both phase specs, the brief example uses the lean catalog + lean carried_blockers, and the apparatus was cut back to what earns its place. Full loop: applied (W17) → reviewed NO-SHIP (W18) → fixed → re-reviewed NO-SHIP/over-built (W19) → simplified by subtraction → confirmed SHIP (W20).
- **One item remains as debt, NOT a P3 defect:** the brief→design **round-trip is still not demonstrated** because no filled design-spec example exists (was rolled back). Regenerating it is a separate `/design`-example task, tracked below.
- **Meta close-out:** across W12/W15/W18/W19, independent review caught real defects every time, twice including over-engineering; the durable lesson is that the reviewer is load-bearing for spec prose and the right reflex when patches pile up is to cut, not add.
- **Open (carried):** regenerate the customer-360 design-spec example to demonstrate the round-trip (debt); separation-of-duties owner; governance proportionality; verification debt (function table / `model.none` / statement verbs / `graph.extension` success path).
