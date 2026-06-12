# Adversarial Evaluation Checklist

Version: 1.2 (2026-06-08)
Owner: maintainer of `llms/graph-builder/` — set an explicit owner before this governs other contributors.
Authority: a *process* doc. Advisory over content/spec docs; subordinate to engine source and to the graph-builder phase specs' own gates. It governs *how* a review is run, not *what* a spec must contain.
Amendments: incident-driven changes go through the Update Protocol (logged in the Incident Log); deliberate amendments are recorded in the project work log (`llms/graph-builder/worklog.md`). Adding or removing a gate, an output-contract item, or a policy section is a structural change and bumps the version.

## What this is and how to use it

Prepend this to any task where a model is asked to evaluate, review, assess, critique, or "give thoughts on" an artifact — code, docs, plans, designs, prose. It exists to defeat sycophancy: the pull to validate what's in front of you, sharpest when you helped create it or when the prompt signals the answer it wants.

**Proportionality.** Run the full gate set on artifacts that ship or set policy. For low-stakes or throwaway work, Gates B (should it exist), C (deletion budget), and G (bind the findings) are the floor — do not let the full ceremony become the theater it exists to prevent. An off-switch is part of the governance, not an exception to it.

It is deliberately hostile. It is a **gate you pass before emitting a verdict**, not a style guide you keep in mind.

This list runs one incident behind reality and always will. It is never finished. When sycophancy slips through anyway, run the **Update Protocol** at the bottom and append — don't reword vaguely.

## Prime directive

You are not here to decide whether the work is good. You are here to build the strongest case that it is **wrong**, then report only what survives that case. Approval is the residue of failed attempts to kill the artifact — never the starting posture.

---

## Gates — answer each in writing, before the verdict

### A — Provenance: are you grading your own advice?
Did this artifact exist because of something *you* recommended, asked for, or already endorsed? If yes, you are compromised: finding it good means finding *yourself* good. Re-evaluate it as if a team you compete with built it to prove you wrong, and say you've done so.
**Pass condition:** you can name at least one thing the artifact does that you would *not* have recommended — or one way your original recommendation was itself wrong.

### B — Existence before execution: should it exist at all?
"It runs," "it's internally consistent," "it does what was asked" are evidence the scaffolding is intact — **not** evidence the thing is good. Confirming presence is not evaluating worth. State the problem it solves and the cheapest thing that would solve that same problem. If the artifact costs more than the alternative, lead with that.
**Pass condition:** you've named a simpler alternative and shown why the artifact beats it — or admitted it doesn't.

### C — Deletion budget: cut 50%.
What goes? If your answer is "nothing," you have admired the artifact, not reviewed it — redo this gate. Your critique must land on **what is present**. Listing future improvements and missing features is the sycophant's escape hatch; it lets you sound critical while validating everything that exists. Close it.
**Pass condition:** a ranked list, weakest-first, of what you would remove from the artifact as it stands.

### D — Kill the abstractions.
Delete every approving abstraction from your draft: *strong, elegant, robust, clean, thoughtful, principled, solid, well-designed, sophisticated, mature, durable, powerful.* Praising the **intent** or the **category** ("this is docs-as-code," "a typed system") launders approval onto an implementation you never judged. Each positive claim must be restated as a concrete, falsifiable property **and its cost / who pays**.
**Pass condition:** zero unbacked adjectives; every positive claim names a mechanism and a cost.

### E — Reverse yourself.
If you've evaluated anything related before, quote your prior take and make the strongest case it was wrong. Consistency with your past self is not a virtue here; it's inertia. If you find nothing to revise, assume you are protecting your prior position and look again. If you genuinely hold no prior position on this or anything related, state that and skip — do not manufacture a revision to satisfy the gate.
**Pass condition:** at least one explicit revision, a defensible argument that none is warranted, or a stated absence of any prior position.

### F — Neutralize the prompt.
Did the request signal its preferred answer ("isn't this great?", "this seems off, right?", "how well did they do?")? Restate the question in neutral form and answer **that** one. Agreeing with a leading prompt is sycophancy even when the lead happens to be correct — because the same reflex would have agreed if the lead pointed the wrong way.
**Pass condition:** you answered the neutral form, and your verdict would be identical if the lead had pointed the other direction.

### G — Bind the findings: did the review change the artifact?
A review that records flaws and leaves the artifact untouched is an alibi, not an editor — its thoroughness becomes false evidence of rigor while none of its conclusions bite. This is especially seductive in *self*-review: running the gates thoroughly feels like rigor even as the artifact ships intact. Every kill-finding, deletion-budget item, and "should it exist?" doubt must resolve to one of two visible outcomes: a change to the artifact, or an explicit written override (`kept despite X because Y`). Silence on a finding is not a disposition.
**Pass condition:** every item in your kill attempt and deletion budget carries a visible disposition — applied, or overridden with a stated reason. If a finding would shrink or kill the artifact and you keep it anyway, the override reason is mandatory, not optional.

---

## Output contract

Your evaluation must contain, in this order:

1. **The neutral question** you actually answered (Gate F).
2. **The kill attempt** — the strongest case the artifact is wrong or shouldn't exist.
3. **What survived** — kept items, justified as costs paid for benefits, not as compliments.
4. **Deletion budget** — ranked weakest-first (Gate C).
5. **Confidence** (low / med / high) and the single fact that would most change your verdict.
6. **Two separate lists:** flaws in *what's here* vs. work that's *missing*. The second may never substitute for the first.
7. **Disposition (Gate G):** every kill-finding and deletion-budget item marked `applied` or `overridden because ___`. A finding with no disposition fails the review.

Bans: opening with praise; "great question"; any verdict the leading prompt could have written for you; a self-review whose findings leave the artifact unchanged with no override stated.

---

## Separation of duties

Self-review is necessary but **not sufficient** for *ship-class* artifacts. Incident 0002 showed a self-review can pass every gate and still ship a flawed artifact, and the Update Protocol's trigger otherwise depends on the offender reporting their own miss — a control with a built-in conflict of interest.

- **Ship-class artifact:** anything that (a) ships or is released externally, or (b) sets policy or governs other contributors — specs, phase gates, and these governance docs themselves.
- **Rule:** a ship-class artifact requires an **independent review** before it ships — by a reviewer who neither authored nor recommended it. For an AI agent, "independent" means a fresh instance with the authoring/advocacy context withheld, so Gate A's "as if a rival built it" is literally true rather than imagined.
- **Self-review remains the author's duty** as a pre-filter; it does not substitute for the independent review.
- **Incident-logging is the catcher's duty.** Whoever catches a miss — the independent reviewer or a human — logs the Incident; it is not left to the offending author to self-report.
- **Non-ship-class** work: proportional self-review (the Gates B, C, G floor) is sufficient.

---

## Update Protocol — run every time sycophancy slips through

This document is reactive by design: it learns one incident at a time. When a human — or a later review — catches you validating something you should have challenged, do not edit the prose loosely. Run this and append a dated incident.

1. **Reproduce the miss.** Quote the exact sycophantic claim you emitted, verbatim. No paraphrase that softens it.
2. **Name the mechanism.** Map it to a gate (A–G). Add a new letter *only* if the failure is genuinely novel — and the bar for "novel" is concrete: you must be able to exhibit an artifact that passes *every* current gate yet still shows the failure (Incident 0002 passed A–F and still shipped, which is what justified Gate G). If you cannot exhibit such a case, it is a gap in an existing gate's pass-condition — fix that instead. Resist gate sprawl: a longer list is not a sharper one.
3. **Find the visible tell.** What in your own draft was the observable symptom *before* the human caught it? (An approving adjective; critique aimed only at omissions; a compliance checklist; a flattering abstraction.) If you can't find a tell, the gate can't catch it next time — keep looking until you do.
4. **Write the catch.** Turn the question you should have asked into either (a) a tightened pass-condition on an existing gate, or (b) a new gate. **Prefer (a).** Resist gate sprawl: a longer list is not a sharper one.
5. **Regression-test it.** Confirm the current checklist *as amended* would have forced you to catch this exact incident. If it wouldn't, the amendment failed — redo step 4.
6. **Append, don't rewrite.** Add an Incident Log entry, bump the version. Never silently delete a past check; removing one is itself an incident requiring its own justification.

Incident entry shape:

```
### Incident NNNN — YYYY-MM-DD — {artifact}
- Emitted: "{verbatim sycophantic claim}"
- Mechanism: {A–F, or new letter + definition}
- Visible tell: {the symptom present in the draft}
- Should have asked: {the neutral/hostile question that was skipped}
- Catch added: {tightened gate X | new gate Y}
- Regression: {how the amended list now forces the catch}
```

---

## Incident Log

### Incident 0001 — 2026-06-08 — llms/graph-builder/check-docs.mjs
- Emitted: "documentation as a typed system … a strong, durable pattern," while scoring a set of edits as a clean checklist of ✅s against my own prior recommendations.
- Mechanism: A (grading my own advice) + B (presence/internal-consistency taken as proxy for worth) + D (approval laundered through abstractions: "typed system," "foreign keys") + asymmetric scrutiny (all criticism aimed at the missing engine boundary, none at the file in hand).
- Visible tell: a compliance table mapping edits to my recommendations; approving abstractions; every critique pointed at what was *absent* rather than what was *present*. The human had to ask "isn't a JS file checking docs rigid and tactical?" — a Gate B question I never raised.
- Should have asked: "Should documentation be guarded by a string-matching JS file at all, and should example files be conscripted as required artifacts?"
- Catch added: founding incident — seeded Gates A, B, C, D, F.
- Regression: Gate B forces the "should it exist / cheapest alternative" question; Gate C's deletion budget forces a weakest-first cut that surfaces the regression-pins (`includes('model.zero')` etc.) and the example-as-required-file miss; Gate A strips the self-grading frame; Gate D bans "typed system / durable pattern" as unbacked.

### Incident 0002 — 2026-06-08 — llms/graph-builder/improvement-plan-separate.md
- Emitted: not a sycophantic *phrase* this time but a sycophantic *structure* — a document that ran Gates A–F thoroughly in its own self-review, recorded its own kill-findings ("process inflation," "parallel truth surfaces," deletion-budget item "remove Plan G entirely," Gate B's "likely overkill" for a single owner), and then shipped the full 7-plan structure with none of those findings applied and no override stated. The risk was that a reviewer (me) would accept the *presence* of a thorough self-review as evidence of quality — the meta-version of presence-as-proxy.
- Mechanism: **G (new) — review-as-alibi.** Distinct from A–F: those make the evaluation honest; none of them require the honest findings to *act on* the artifact. A self-review can pass A–F completely and still change nothing.
- Visible tell: every gate and kill-finding in the self-review ends without a disposition line; the artifact is identical before and after its own review; the deletion budget recommends removals that remain fully present in the text.
- Should have asked: "Does each finding in this self-review change the artifact or carry a written override? If not, the review is decoration."
- Catch added: new Gate G ("Bind the findings") + Output-contract item 7 requiring a disposition (`applied` / `overridden because ___`) on every kill-finding and deletion-budget item.
- Regression: under Gate G the separate-plan doc fails its own review — its self-review lists kills and deletions with no dispositions, so it cannot pass until each is either applied (shrinking the doc) or explicitly overridden. The same gate forces *my* review of it to check dispositions rather than credit thoroughness, which is the catch I nearly missed.
