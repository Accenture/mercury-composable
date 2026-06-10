# Graph Builder — Continuity & Wisdom Handoff

Status: living handoff. Read this first if you are picking up this work (especially after a context compaction). It is the distilled *judgment* behind the project — what to do, what I kept getting wrong, and why. It complements, not replaces:
- [README.md](./README.md) — the doc index + authority/precedence + glossary.
- [worklog.md](./worklog.md) — the blow-by-blow (W1–W38+). When in doubt, the worklog is the record; this file is the synthesis.

---

## 1. What this project is

A four-phase workflow for authoring **MiniGraph** graphs — `/requirements → /design → /build → /test` — with canonical specs, a verified syntax reference, an execution/verification helper, and (latest thread) an installer that ports the four phases to coding agents (Claude first).

The work has two threads:
- **The doc system** (P0–P5): validated against the live engine, independently reviewed, verification debt cleared. Effectively done.
- **Distribution** (D0–D5, worklog W31–W37): packaging the four phases as installable agent commands. **Claude reference is built and dogfooded (D0–D4 mechanical PASS); the live-invoke confirmation and the other three agents (D5) remain.**

---

## 2. Current state (as of W38)

- **Phases P0–P3, P5: closed**, validated by execution, independently reviewed. P4 (CI engine-truth automation) deferred by design.
- **Verification debt: cleared** — every function, `model.none`, and the `graph.math` verbs are execution- or source-verified (evidence claims 40, 44–49).
- **Distribution: D0–D4 done for Claude.** `docs/graph-builder/installer/` is a no-dependency Node CLI: `node docs/graph-builder/installer/bin/cli.mjs install claude` copies the spec set to `<project>/.graph-builder/` and writes four `SKILL.md` to `<project>/.claude/skills/`; `check` regenerates + diffs (drift gate, dogfood-proven). Per-project only, no npm/npx (decisions 6/7).
- **Workflow meta-audit done (W38):** the pipeline's one structural blind spot is named (below); fixes recommended, **not applied**.

**Open threads** (also in worklog "Open (carried)"):
1. **Live-invoke confirmation** — actually run `/graph-requirements` in a real Claude project + fresh session. Mechanics are proven; the live load + the "subdirectories don't namespace" claim are not.
2. **D5** — Copilot / Codex / Kiro adapters (each needs a D0 format-verification first).
3. Copied `.graph-builder/README.md` has a few dangling links to excluded planning docs — cosmetic.
4. The **W38 workflow fixes** — apply if/when the maintainer wants them (they're recommendations).
5. **Separation-of-duties owner** — the checklist/plan still say "set an owner"; unnamed. A standing decision, not a blocker.

---

## 3. The disciplines that earned their place (do these)

1. **Dogfood > review > assert.** Execution against the live engine is the highest-ROI source of truth. It caught the most, and caught things review couldn't. Anything tagged **`(verified)`** must trace to a specific execution verdict in [evidence/dogfood-customer-360.md](./evidence/dogfood-customer-360.md). Untagged = asserted.
2. **Verify by *running*, not by reasoning.** Repeatedly, a claim that "passed" on paper failed when run: the unexecutable export fix (W24), the `/design`→`/graph-design` name seam (W36), drift detection (W37). Run it; eyeball the output; don't trust the boolean alone.
3. **Independent review for ship-class changes** — by a *fresh no-context subagent*, so the checklist's Gate A ("review as if a rival built it") is literally true, not imagined. It caught a real authoring defect on almost every first pass (W12, W15, W18, W22, W27, W32, W33).
4. **Verify the reviewer too.** The reviewer is fallible in *both* directions — it has produced false NO-SHIPs (W29 loop-protection; W34 two false findings). When it makes a claim about source/specs, check the source. **The tiebreaker is always the engine source / running the thing (precedence rule #1).**
5. **Proportionality (W30).** The governance layer was sized to *the author's error rate*, not the artifact's risk. It is now at "sufficient." Re-checks reach diminishing returns fast — when a review round returns only wording or false findings, **stop reviewing and execute.** Don't re-grow what W30 trimmed.

---

## 4. The failure modes to guard against — these are MINE, documented and recurring

This is the most useful part of the handoff. The reason independent review is load-bearing is that it compensates for these specific weaknesses. Guard against them *before* writing, and you'll need less review:

- **Overclaiming `(verified)`.** Caught 6+ times (W12, W18, W22, W27, W33). The shape: tagging something verified that wasn't executed, or that traces to a *different* scenario than the one claimed. **Pre-write rule:** never write `(verified)` without pointing to a specific evidence verdict; if it was done live but not recorded, it is "asserted," not verified.
- **Asserting about specs/source from memory instead of re-reading.** Got two spec characterizations wrong in W38 — including a doc *I wrote*. **Re-read the file before characterizing it.**
- **Inflation / persuasive self-review.** Turning one root issue into four to look thorough; polishing past the evidence. The tell is absolutist prose ("structurally blind," "the one change, and only that"). Collapse to the minimal honest set; if two "findings" reduce to one sentence, they're one.
- **Over-fitting from n=1.** Generalizing a universal rule from a single defect (the predicate-only "one rule" in W38 was this). Ask whether the rule survives a graph of a different shape.

When you catch yourself doing one of these, the move is the same one that works every time: **go to the source or run it.**

---

## 5. The workflow's one structural blind spot (W38)

Every gate checks **conformance to the previous artifact**; none checks the artifact against reality. Intent is recorded once — in the brief — so a misconception in the brief is consistent at every gate and never trips one (it builds the wrong thing, correctly). The only thing that reliably catches it is an **independent re-derivation taken without sight of the brief.**

Two genuinely distinct phase weaknesses (the rest collapse to the above): `/requirements` is self-graded and its Gate D invites filing a conceptual contradiction as cheap build/deploy debt; `/test` authors its oracle from the contaminated brief, so a green test is a mirror. Minimal fixes (free unless noted), per W38: (a) conceptual contradictions block `/requirements`, not bucketable as debt; (b) `/test` gains a "spec-was-wrong → `/requirements`" route; (c) define "intent" as independent of the brief; (d, ship-class, the high-leverage one) independent test-oracle authorship. Plus two gaps: **no post-deploy net**, and **operator variance is the only conceptual floor.** Recommendations, not yet applied.

---

## 6. The map (where things live) + precedence

Canonical: `workflow.md` (phase boundaries) → `requirements-gathering.md` / `graph-design.md` / `build.md` / `test.md` (the four phases) → `minigraph-syntax.md` (command syntax + verified behavior) + `companion.mjs` (the driver: one-mutation-at-a-time verify-after-mutation; `--expect`; scalar-leaf resolution). Support: `evidence/dogfood-customer-360.md` (the execution claim matrix — the asset), `evidence/stub-server.mjs`, `improvement-plan.md`, `worklog.md`, `distribution-plan.md`, `installer/`, and the repo-level `../adversarial-review-checklist.md`.

**Precedence (README "Authority And Precedence"):** engine source / observed behavior > `minigraph-syntax.md` > `requirements-gathering.md` (brief schema) > `graph-design.md` (primitive selection) > `workflow.md` (phase order). Process/working docs (checklist, plan, worklog, this file) are **advisory**, never authoritative over content.

---

## 7. Engine footguns a fresh agent will hit (all in the evidence file)

- Null detection is a **mapper** step (`:boolean(null=true)`), not an inline `graph.math` statement; a `graph.math` IF on an unresolved value **halts the node silently**. No bare `!= null`.
- No `number()` constant — `int()`/`double()`. A bad seed silently drops the whole instance.
- `with type` is **cosmetic for skill nodes** (skill drives behavior); load-bearing only for Root/End/no-skill.
- `export` writes to `/tmp/graph`; `extension` resolves from `classpath:/graph` (read-only) — different stores, so an exported name is importable but **not** extension-callable.
- `graph.join` **deadlocks** if a backward-linked predecessor can be skipped; converge conditional branches through one always-completing node first.
- `companion.mjs` verifies mutations but **not** `export` — confirm export by re-import round-trip.
- The playground session can go stale between calls → transient `404` on reads (not necessarily state loss; the inspect endpoint also 404s on scalar leaves by design).

---

## 8. The spirit (carry this)

Lean, honest, and verified-by-execution. **Verify, don't assert; run it, don't reason about it; check the source when anyone — author or reviewer — sounds confident.** The governance layer is finished — don't build more process; the next value is in *using* the docs (live-invoke, D5) and, if asked, applying the W38 fixes. When you're about to write `(verified)` or "this is the one fix," stop and ask what evidence backs it. That single habit is the whole flame.
