---
name: graph-verify
description: "Graph-builder optional cross-cutting step — Adversarially verify and quality-review any graph-builder artifact — a brief, design spec, build log, test report, doc claim, external contribution, or a live graph (user-invoked)."
argument-hint: "<target> [--report [path]]"
disable-model-invocation: true
---

Adversarially verify and quality-review any graph-builder artifact — a brief, design spec, build log, test report, doc claim, external contribution, or a live graph: spawn a fresh-context reviewer that reproduces the artifact's load-bearing claims against the live engine and framework source, and deliver an honest verdict on whether it is sound and worth trusting, not merely whether it conforms.

**Invocation.** Optional and cross-cutting — not a stage in the /graph-requirements → /graph-design → /graph-build → /graph-test pipeline; run it at any time against any artifact, and it never blocks a phase. Default output is a candid prose verdict; pass --report [path] to also persist the structured record. Reach for it when a wrong-but-internally-consistent, or correct-but-poor, artifact would actually cost something — before trusting a PASS on a safety/money-relevant graph, before incorporating external/'rival' work that carries claims, or when an artifact carries (verified) tags you did not reproduce this session.

**Prerequisites.** The reviewer must run with **fresh context** (authoring/advocacy withheld) — this skill spawns a subagent for that. Reproducing engine-behavior claims needs Node >=20 and a reachable `MINIGRAPH_HOST` (default `localhost:8085`) via `companion.mjs`; source-level claims need the framework source. A doc/spec with no engine claims can be reviewed without a live engine.

**Canonical spec (authoritative).** Full detail and precedence live in [`.graph-builder/verify.md`](.graph-builder/verify.md); defer to it on any conflict. The section(s) below are lifted verbatim from that spec — this wrapper is not a second source of truth.

## What it counters

1. **Laundered truth** — a claim that traces only to a prior artifact, never to ground truth.
2. **Oracle corruption** — expected outputs (test oracles, "should" statements) copied from the implementation/author instead of re-derived from intent.
3. **n=1 over-generalization** — a broad rule resting on one narrow observation, *in either direction* (an author's claim **or** a critic's refutation).
4. **Boundary / interaction errors** — predicates correct in isolation, wrong where two conditions co-occur.
5. **Unreproduced "verified"** — tags, citations, and confident prose accepted as proof.
6. **Conformant-but-poor** — an artifact that is internally consistent and passes its gate yet is over-built, needlessly complex, or solving the wrong problem. Correctness is necessary, not sufficient; quality is the question.

## Core principles

1. **Honest quality judgment first.** The point is a truthful verdict a human can act on — *is this good?* — not a compliance tally. Reproduction against ground truth is how the verdict earns the right to be trusted, not the verdict itself.
2. **Independence is literal.** Spawn a **fresh-context subagent** with the authoring/advocacy context **withheld**, so the checklist's Gate A ("re-evaluate as if a rival built it") is *true*, not imagined. The parent must not state the verdict it wants or pre-judge the artifact.
3. **Ground truth over record.** Reproduce each load-bearing claim by running it against the live engine and/or reading the framework source. The artifact's own words — including `(verified)` — are the hypothesis, never the evidence.
4. **Symmetric skepticism.** Distrust a confident *critic* exactly as much as a confident *author*. A rival's refutation is reproduced too — that is how the false "multi-line" refutation was itself caught.
5. **Proportional, claim-scoped.** The unit of review is **the artifact's claims and its fitness for purpose**, not a fixed list of predicate dry-runs. A transform/fetch graph is judged on its mappings/contracts; a decision graph on its branch outcomes at the boundaries. Do not manufacture work the artifact does not warrant.

## When to run it (optional — the operator chooses)

Never required by a gate. Reach for it when a wrong-but-internally-consistent, or correct-but-poor, artifact would actually cost something — and especially:

- before trusting any phase **PASS** on a safety- or money-relevant graph;
- before **incorporating external / "rival" / prior-session work** that carries claims;
- whenever an artifact carries **`(verified)` tags you did not personally reproduce this session**;
- when the **same agent authored both an artifact and the thing meant to check it** (code + its test oracle; a claim + its own "correction");
- when an artifact asserts a **broad behavioral rule** about the engine, or feels **more elaborate than the problem deserves**.

## When NOT to run it (proportionality — this project resists gate sprawl, W30)

A throwaway/scratch graph; a pure restatement with no new claims; an artifact whose claims you already reproduced against the engine this session. This step is a **scalpel, not a turnstile** — running it on everything recreates the review-as-theater the checklist exists to prevent.

## Operating protocol

Run as a guided, execution-grounded attack aimed at a verdict — not a read-through, and not a form-fill.

- **V1 — Frame the target; extract its claims and its purpose.** Name the artifact, state in one line *what it is for*, and list its *falsifiable, load-bearing* claims: every `(verified)` tag, numeric value **+ unit**, status/branch predicate, "X works / X fails" assertion, and every expected-output oracle. *If nothing here is independently checkable and the artifact's worth is self-evident, it likely does not warrant this step — say so and stop.*
- **V2 — Spawn the independent reviewer.** Launch a fresh-context subagent. Give it: the artifact, access to the **live engine + framework source + [minigraph-syntax.md](./minigraph-syntax.md)**, and the [adversarial-review-checklist](../adversarial-review-checklist.md). Withhold: who wrote it, why, and any desired verdict. Instruct it to treat the artifact as a rival's and to **build the strongest case it is wrong or not worth trusting, reproducing every claim against ground truth.**
- **V3 — Reproduce against ground truth.** For each claim, re-run it on the engine (a minimal rig suffices — `root → node-under-test → end`, seed, `run`, `inspect`) and/or read the cited source path. Record *observed* beside *asserted*. **A claim that cannot be reproduced is refuted, regardless of its tag.**
- **V4 — Judge quality, not just correctness.** Beyond "is each claim true," ask: does this solve the actual problem, and is it the *cheapest sound thing that would* (Gate B)? What would you cut (Gate C)? Is it over-built, ambiguously specified, or carrying false confidence? Hunt the propagation failure modes — laundered truth, oracle corruption, n=1 (run a *different* input to break a broad rule), boundary/interaction (construct the co-occurrence case). For each suspected error produce a **concrete counter-case — an input that breaks it, not a worry.**
- **V5 — Adversarially check your own draft verdict** with the checklist's gates (A–G); do not restate them. Confirm the verdict would be identical if the artifact had been framed the opposite way (Gate F), that praise is restated as property-with-cost (Gate D), and that distrust was applied symmetrically to any critic-claims.
- **V6 — Deliver the verdict; route; correct.** Give the human the candid judgment (next section). Route each confirmed defect to its **owning phase** (structural → `/build`; wrong/ambiguous spec → `/design`/`/requirements`; false doc claim → fix the doc). Correct false claims **per the record's own protocol** — append a dated correction; do **not** silently rewrite an evidence log or worklog. **If `--report` was passed, also write the record.**

## The verdict — the primary deliverable

A truthful answer to *"is this good, and should it be trusted?"*, written for a human. It must:

- **Lead with the bottom line** — sound / flawed / shouldn't-exist — and *why*, in a sentence or two the reader can act on.
- **Name what's wrong or weak first**, each tied to a **failed reproduction or a concrete counter-case**, not a worry.
- **Then what genuinely holds**, stated as a property and its cost — *"X is correct because re-running Y produced Z"* — never as bare praise (*"robust," "clean," "solid"* are banned unless immediately cashed out as a falsifiable property).
- **Keep "wrong" separate from "missing"** — a flaw in what's present must not be replaced by a list of absent features.
- **Speak to quality, not only correctness** — is it the simplest thing that solves the problem? over-built? would you stake a production decision on it? A verified-but-bloated or verified-but-pointless artifact is a finding, stated plainly.
- **State confidence (low/med/high) and the single fact that would most change the call.**

Banned: opening with praise; a verdict the operator's framing could have written; a clean bill of health that rests on the artifact's own `(verified)` tags rather than a re-run.

## What an honest verification delivers (completion)

This step is complete when:

- a **candid quality verdict** exists — which may be *"sound,"* *"flawed, here's why,"* or *"shouldn't exist."* There is **no "PASS"**: a true negative verdict on a bad artifact is a *successful* verification, not a failed one;
- the verdict **rests on re-runs against ground truth**, not on the record's own tags — every load-bearing claim was reproduced or explicitly marked unverifiable (with reason);
- the reviewer was a **genuinely fresh context** (self-review does not satisfy this step);
- **symmetric skepticism** held — critic/refutation claims were reproduced too;
- each **confirmed defect is routed** to its owning phase, and any record correction was **appended, not silently rewritten**.

The bar is the **honesty and groundedness of the judgment**, not the completeness of a form. If `--report` was requested, the record exists and its `verdict` field matches what the human was told.
