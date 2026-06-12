# `/graph-verify` — Adversarial Verification & Quality Review (optional, cross-cutting)

Status: canonical spec for the optional `/graph-verify` step.

**Not a stage in the linear pipeline.** `/graph-requirements → /graph-design → /graph-build → /graph-test` run in order; `/graph-verify` is an **on-demand review the operator may run at any time against any artifact from any stage** — a brief, a design spec, a build log, a test report, a doc claim, an externally-contributed change, or the live graph itself.

Authority & precedence: subordinate to **engine source** (authoritative on behavior) and to each phase's own gate (this step never relaxes them). It *uses* the anti-sycophancy gates in [./adversarial-review-checklist.md](./adversarial-review-checklist.md) rather than restating them. When a finding and an artifact's `(verified)` claim disagree, **a re-run against the engine decides** — not either document.

---

## Purpose

Its job is to tell a human, **honestly, whether an artifact is good** — sound, correct, trustworthy, worth building on — **not merely whether it conforms.** Conformance (does it match the prior phase, does it parse, does the output key exist) is what the pipeline's gates already prove cheaply on every run. This step exists for the harder and more valuable question a conformance gate structurally cannot answer: *is the thing actually right, and would you stake a decision on it?*

It earns that judgment the only honest way — an independent agent reproducing the artifact's load-bearing claims against ground truth — but **the deliverable is the candid judgment, not a filled-in form.** A schema you complete to feel thorough is the exact review-as-theater this step is meant to defeat.

> **Motto: verify against the engine, and verify the verifications.**

This is not theoretical. Three failures from the VoltaicMesh run and the doc-verification that followed it, each caught only by review external to the author:

- **DEF-1** — a branch predicate (`req > head` instead of "did the allocation reach the cap?") wrong from `/requirements`, propagated through design + build, and *passed* `/test` because the oracle was authored from the same misconception. Conformance gates were blind to it.
- **K1** — a ms-vs-epoch-seconds unit contradiction self-classified as cheap "build debt" so the requirements gate would pass.
- **the "multi-line COMPUTE fails" correction** — a confident, source-cited, `(verified)`-tagged *correction* (W40) of a correct earlier finding, itself false: it read a `404` from `inspect` on a **scalar leaf** as a silent node halt. The byte-identical IIFE returns `15` whether written on one physical line or several — reading the **container** (`enrich.result`), not the leaf, shows it, and `InspectStateMachine.java:47-51` confirms the endpoint `404`s on any non-`Map`/`List` leaf by design. Caught by re-running against the engine and by an independent reviewer not told which way the author leaned. **A `(verified)` tag — even on a *correction* — is a claim, not evidence; distrust a confident critic as much as a confident author.**

It operationalizes the one fix worklog W38 found "structurally escapes the chain" (independent review). It deliberately does **not** add the ceremony W38 cut (no mandatory per-predicate dry-run, no forced capability-probe) — see Proportionality.

## Invocation & output mode

```
/graph-verify <target> [--report [path]]
```

- `<target>` — the artifact (file path), a live session id, or the specific claim to judge.
- **Default (no `--report`): a candid, human-facing quality verdict, in prose** (see *The verdict* below). Honest, specific, evidence-backed. No artifact is written — the answer is the response itself.
- `--report [path]` — *additionally* persist the structured record (schema below) for audit or handoff. The record **supports** the verdict; it never substitutes for it. Use it when the review needs to be referenced later or by someone else; skip it otherwise.

<!-- wrapper-body -->
## What it counters

1. **Laundered truth** — a claim that traces only to a prior artifact, never to ground truth.
2. **Oracle corruption** — expected outputs (test oracles, "should" statements) copied from the implementation/author instead of re-derived from intent.
3. **n=1 over-generalization** — a broad rule resting on one narrow observation, *in either direction* (an author's claim **or** a critic's refutation).
4. **Boundary / interaction errors** — predicates correct in isolation, wrong where two conditions co-occur.
5. **Unreproduced "verified"** — tags, citations, and confident prose accepted as proof.
6. **Conformant-but-poor** — an artifact that is internally consistent and passes its gate yet is over-built, needlessly complex, or solving the wrong problem. Correctness is necessary, not sufficient; quality is the question.

<!-- wrapper-body -->
## Core principles

1. **Honest quality judgment first.** The point is a truthful verdict a human can act on — *is this good?* — not a compliance tally. Reproduction against ground truth is how the verdict earns the right to be trusted, not the verdict itself.
2. **Independence is literal.** Spawn a **fresh-context subagent** with the authoring/advocacy context **withheld**, so the checklist's Gate A ("re-evaluate as if a rival built it") is *true*, not imagined. The parent must not state the verdict it wants or pre-judge the artifact.
3. **Ground truth over record.** Reproduce each load-bearing claim by running it against the live engine and/or reading the framework source. The artifact's own words — including `(verified)` — are the hypothesis, never the evidence.
4. **Symmetric skepticism.** Distrust a confident *critic* exactly as much as a confident *author*. A rival's refutation is reproduced too — that is how the false "multi-line" refutation was itself caught.
5. **Proportional, claim-scoped.** The unit of review is **the artifact's claims and its fitness for purpose**, not a fixed list of predicate dry-runs. A transform/fetch graph is judged on its mappings/contracts; a decision graph on its branch outcomes at the boundaries. Do not manufacture work the artifact does not warrant.

<!-- wrapper-body -->
## When to run it (optional — the operator chooses)

Never required by a gate. Reach for it when a wrong-but-internally-consistent, or correct-but-poor, artifact would actually cost something — and especially:

- before trusting any phase **PASS** on a safety- or money-relevant graph;
- before **incorporating external / "rival" / prior-session work** that carries claims;
- whenever an artifact carries **`(verified)` tags you did not personally reproduce this session**;
- when the **same agent authored both an artifact and the thing meant to check it** (code + its test oracle; a claim + its own "correction");
- when an artifact asserts a **broad behavioral rule** about the engine, or feels **more elaborate than the problem deserves**.

<!-- wrapper-body -->
## When NOT to run it (proportionality — this project resists gate sprawl, W30)

A throwaway/scratch graph; a pure restatement with no new claims; an artifact whose claims you already reproduced against the engine this session. This step is a **scalpel, not a turnstile** — running it on everything recreates the review-as-theater the checklist exists to prevent.

<!-- wrapper-body -->
## Operating protocol

Run as a guided, execution-grounded attack aimed at a verdict — not a read-through, and not a form-fill.

- **V1 — Frame the target; extract its claims and its purpose.** Name the artifact, state in one line *what it is for*, and list its *falsifiable, load-bearing* claims: every `(verified)` tag, numeric value **+ unit**, status/branch predicate, "X works / X fails" assertion, and every expected-output oracle. *If nothing here is independently checkable and the artifact's worth is self-evident, it likely does not warrant this step — say so and stop.*
- **V2 — Spawn the independent reviewer.** Launch a fresh-context subagent. Give it: the artifact, access to the **live engine + framework source + [minigraph-syntax.md](./minigraph-syntax.md)**, and the [adversarial-review-checklist](./adversarial-review-checklist.md). Withhold: who wrote it, why, and any desired verdict. Instruct it to treat the artifact as a rival's and to **build the strongest case it is wrong or not worth trusting, reproducing every claim against ground truth.**
- **V3 — Reproduce against ground truth.** For each claim, re-run it on the engine (a minimal rig suffices — `root → node-under-test → end`, seed, `run`, `inspect`) and/or read the cited source path. Record *observed* beside *asserted*. **A claim that cannot be reproduced is refuted, regardless of its tag.**
- **V4 — Judge quality, not just correctness.** Beyond "is each claim true," ask: does this solve the actual problem, and is it the *cheapest sound thing that would* (Gate B)? What would you cut (Gate C)? Is it over-built, ambiguously specified, or carrying false confidence? Hunt the propagation failure modes — laundered truth, oracle corruption, n=1 (run a *different* input to break a broad rule), boundary/interaction (construct the co-occurrence case). For each suspected error produce a **concrete counter-case — an input that breaks it, not a worry.**
- **V5 — Adversarially check your own draft verdict** with the checklist's gates (A–G); do not restate them. Confirm the verdict would be identical if the artifact had been framed the opposite way (Gate F), that praise is restated as property-with-cost (Gate D), and that distrust was applied symmetrically to any critic-claims.
- **V6 — Deliver the verdict; route; correct.** Give the human the candid judgment (next section). Route each confirmed defect to its **owning phase** (structural → `/build`; wrong/ambiguous spec → `/design`/`/requirements`; false doc claim → fix the doc). Correct false claims **per the record's own protocol** — append a dated correction; do **not** silently rewrite an evidence log or worklog. **If `--report` was passed, also write the record.**

<!-- wrapper-body -->
## The verdict — the primary deliverable

A truthful answer to *"is this good, and should it be trusted?"*, written for a human. It must:

- **Lead with the bottom line** — sound / flawed / shouldn't-exist — and *why*, in a sentence or two the reader can act on.
- **Name what's wrong or weak first**, each tied to a **failed reproduction or a concrete counter-case**, not a worry.
- **Then what genuinely holds**, stated as a property and its cost — *"X is correct because re-running Y produced Z"* — never as bare praise (*"robust," "clean," "solid"* are banned unless immediately cashed out as a falsifiable property).
- **Keep "wrong" separate from "missing"** — a flaw in what's present must not be replaced by a list of absent features.
- **Speak to quality, not only correctness** — is it the simplest thing that solves the problem? over-built? would you stake a production decision on it? A verified-but-bloated or verified-but-pointless artifact is a finding, stated plainly.
- **State confidence (low/med/high) and the single fact that would most change the call.**

Banned: opening with praise; a verdict the operator's framing could have written; a clean bill of health that rests on the artifact's own `(verified)` tags rather than a re-run.

## Optional report artifact (`--report`)

Emitted only when `--report` is passed — a persisted record that *backs* the verdict for audit/handoff. It is not the deliverable and must never be produced *instead of* an honest judgment.

```yaml
verification_report:
  target: ""                      # artifact path, live session id, or quoted claim under review
  stage_of_target: "requirements | design | build | test | doc | external-contribution | live-graph"
  verdict: ""                     # the bottom-line quality judgment, in plain language (the headline — required)
  confidence: "low | med | high"
  would_change_my_mind: ""        # the single fact that would most move the verdict
  reviewer: "fresh-context subagent — authoring/advocacy context withheld"
  ground_truth_used: []           # engine session ids; source files:lines; the intent source read independently
  claims:
    - id: "C1"
      claim: ""                   # the falsifiable assertion, quoted
      traces_to: "engine | source | prior-artifact-only"
      check: ""                   # exactly how it was reproduced (command / run / source read)
      asserted: ""
      observed: ""
      verdict: "reproduced | refuted | unverifiable"
  quality_findings: []            # over-build, wrong-problem, needless complexity, false confidence — beyond correctness
  defects:
    - id: "VD-1"
      finding: ""
      owning_phase: "requirements | design | build | test | doc"
      evidence: ""
      disposition: "applied: … | overridden because …"
  refuted_claims: []              # claims (incl. over-confident critiques) that did NOT survive, with evidence
  unverifiable: []                # + why (mock / environment limitation) + any carried deploy blocker
```

<!-- wrapper-body -->
## What an honest verification delivers (completion)

This step is complete when:

- a **candid quality verdict** exists — which may be *"sound,"* *"flawed, here's why,"* or *"shouldn't exist."* There is **no "PASS"**: a true negative verdict on a bad artifact is a *successful* verification, not a failed one;
- the verdict **rests on re-runs against ground truth**, not on the record's own tags — every load-bearing claim was reproduced or explicitly marked unverifiable (with reason);
- the reviewer was a **genuinely fresh context** (self-review does not satisfy this step);
- **symmetric skepticism** held — critic/refutation claims were reproduced too;
- each **confirmed defect is routed** to its owning phase, and any record correction was **appended, not silently rewritten**.

The bar is the **honesty and groundedness of the judgment**, not the completeness of a form. If `--report` was requested, the record exists and its `verdict` field matches what the human was told.

## Relationship to the rest of the workflow

- **Complements, never replaces** the per-phase gates. Those prove conformance (fast, cheap, every run); this judges *correctness against reality and fitness for purpose* (heavier, on demand).
- **Layers on** [./adversarial-review-checklist.md](./adversarial-review-checklist.md): that supplies the general anti-sycophancy gates (A–G) and output discipline; this adds the graph-specific spine — a fresh agent, a live re-run, and the source — and aims them at a quality verdict.
- **Operationalizes** the control worklog W38 endorsed as the only structural escape from the conformance chain, without re-growing the ceremony W38 cut.

> Closing proportionality note: keep this lean and keep it honest. If `/graph-verify` ever becomes a mandatory turnstile, a per-predicate checklist, or a report you file to look rigorous, it has become the theater it exists to prevent — cut it back to a truthful judgment, earned by re-running the things that would cost something if they were wrong.
