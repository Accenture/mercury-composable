# Retrospective: VoltaicMesh Dispatch Evaluator — A Meta-Audit of the Four-Phase Graph Workflow

Status: session postmortem. Audits how the `/graph-requirements → /graph-design → /graph-build → /graph-test` pipeline performed building the VoltaicMesh Dispatch Evaluator, with attention to where each phase's *instructions* (the canonical specs) succeeded, failed, and could improve.

---

## I. The thesis, stated plainly

The pipeline is **excellent at transmitting intent faithfully and at catching execution errors, and structurally blind to conceptual errors held consistently across phases.**

Every gate validates *conformance to the previous phase*. That is the pipeline's great virtue — nothing is invented, each phase has a clean contract with the next — and it is precisely the source of its one systemic weakness. A misconception introduced early and carried consistently is, by construction, *consistent at every gate*. It never trips a check, because no check asks "is the thing we all agree on actually true?" The pipeline will build — with full structural verification, a passing smoke test, and a green scenario suite — the **wrong thing, correctly**, if the wrong thing is what was understood.

This session produced a clean specimen of that failure (DEF-1) and, by contrast, a clean specimen of the only thing that reliably catches it (independent adversarial review). The gap between the two is the lesson.

---

## II. Anatomy of the defect that survived all four gates (DEF-1)

The graph had to label *why* a dispatch fell short: `SAFETY` (the transformer headroom capped it) versus `SHORTAGE` (we ran out of batteries). The correct discriminator is **"did the allocation actually reach the headroom ceiling?"** The implemented discriminator was **"is demand greater than headroom?"** (`req > head`). Those differ exactly when supply gives out *below* the ceiling — a supply drought that the graph then mislabels as a grid-safety event, lying to the event bus about the nature of the constraint.

Trace its life through the pipeline:

1. **`/graph-requirements`.** The defect was *born inside the fix for a different defect.* The independent review (good) caught K2 — that `SAFETY` and `SHORTAGE` collapsed into one signal — and I introduced `binding_constraint` to separate them. But I wrote its derivation as `BOTH if (clamped_by_headroom AND supply<required)`, where `clamped_by_headroom` silently meant `req>head`. The coarse structural gap was closed; the **subtle semantic predicate was wrong from the first keystroke**, and the review's lens ("is there a discriminator?") was not the lens that would catch it ("is the discriminator correct at the supply-below-headroom boundary?").

2. **`/graph-design`.** The spec instructs `/design` *not to rediscover requirements*. It dutifully echoed the brief's derivation into `derived_values` and `FLOW-006`, unexamined. Correct scope discipline; zero chance of catching the error.

3. **`/graph-build`.** Garbage-in, faithfully-lowered. The single-line `graph.js` script encoded `clamped = req > head` exactly as specified. The build smoke test is happy-path only (by design), so a branch-label error on a partial-allocation path was invisible to it.

4. **`/graph-test`.** The fatal step. I authored a scenario named `T-09 "both bind"` with a fixture (headroom 5, supply 3) that is *not* a both-bind case at all — supply 3 never reaches the 5 MW ceiling — and an expected output (`SAFETY/BOTH`) drawn from **the same flawed mental model that wrote the code.** The test passed. The green check was not verification; it was a mirror.

It was caught only by **external adversarial review** (the user), with a concrete counter-scenario. Four gates, one consistent misconception, zero detections.

**The deeper observation:** even the requirements independent review — the one control that worked — caught the *structural* error (missing discriminator) but not the *boundary-semantic* error (wrong predicate). Adversarial review has resolution; a reviewer hunting "is a signal missing?" is not the same as one hunting "is this predicate right where two constraints interact?" Catching DEF-1 required *interaction/boundary analysis* specifically. This matters for the improvements below.

---

## III. The two error classes

| | Execution errors | Conceptual errors |
|---|---|---|
| Examples this session | `:boolean` flags not written → node halt; lone `{path}` sharing braces with a colon/newline halts the node; inspect scalar-leaf 404 (read the container, not the leaf); test-harness `printf` `\n` bug | K1 unit incoherence (ms vs epoch-s); K2/DEF-1 binding predicate; (K3/K4 — caught in review) |
| Caught by | Build mechanics + smoke run, **early and reliably** | **Nothing in the pipeline.** Only external adversarial review (subagent in requirements; user in test) |
| Why | They break something observable — the run stops, the key is absent, the output contradicts itself | They are internally consistent across all artifacts; every gate sees agreement |

The pipeline's instrumentation is tuned almost entirely to the left column. That tuning is real and valuable — this session's build phase caught four genuine execution failures within minutes. But a safety-relevant graph fails in the right column, and the right column had **no instrumentation except the reviews I chose to run.**

---

## IV. Per-phase audit of the instructions

### `/graph-requirements`

- **Succeeded.** Gate A (Scope Boundary) and the "gather obligations, not syntax" principle drove the single most valuable act of the session: refusing to build the "living AKG control plane" and carving out the request-scoped decision slice. The mock-and-proceed rule and the `blocks: requirements|design|build|deploy` taxonomy kept momentum without erasing risk. Requirement IDs gave downstream traceability that genuinely paid off.
- **Failed.** The self-administered gate passed a brief with four real defects (K1–K4) filed as build-level mocks. The spec's Gate D *invites* this: "if the behavior is clear but an implementation detail is unavailable, classify it build/deploy." A self-author rationalizes defects into the cheapest bucket — I filed a unit contradiction (K1) as "blocks: build." The gate trusts the author's classification and has no adversarial step.
- **Improve.** (a) Mandate the independent adversarial review for ship-class briefs *as part of the gate*, not as an optional courtesy. (b) Add two targeted checks that would have caught K1 and the seed of DEF-1 at the source: **"every numeric quantity states its unit and is cross-checked against each consumer,"** and **"every enum/status value is defined by an exact, testable predicate, sanity-checked at its boundaries and where predicates interact."**

### `/graph-design`

- **Succeeded.** "Smallest shape," source-verified primitive selection (the subagent capability probe before committing topology was excellent and prevented worse build failures), `requirement_traceability`, and "present tradeoffs before locking the gate." The Mock-Or-Placeholder rule kept dependencies visible. The chosen topology survived build essentially intact.
- **Failed.** Two things. (a) It propagated the brief's flawed `binding_constraint` predicate unexamined — correct per "don't rediscover requirements," but it means a logically-unsound-yet-traceable requirement sails through, because the design gate checks *structural* completeness ("every requirement maps to a node/edge/path") not *semantic* soundness. (b) It deferred the entire `graph.js` interpolation risk (OQ-D02) to build, where it caused multiple failed attempts; the `graph.js` `{path}` substitution behavior was discoverable a phase earlier. (W41: the failed attempts were the colon/newline-brace substitution rule, *not* any "multi-line IIFE non-buildability" — multi-line COMPUTE works; see §IV `/graph-build` correction trail.)
- **Improve.** (a) Add a **predicate-realizability check** to the design gate: for each decision/branch requirement, write the exact boolean and dry-run it against 2–3 boundary and interaction cases. This is where "req>head ≠ headroom-bound" becomes obvious. (b) For any flagged engine-capability unknown, require a **minimal empirical probe during design**, not a deferral — the design phase already spawns capability-verification subagents, so this is a small extension.

### `/graph-build`

- **Succeeded.** The strongest spec of the four. "One verified mutation at a time," mandatory post-mutation re-fetch + assert, the smoke run as a *buildability* proof, the explicit footgun list, and `companion.mjs` enforcement together caught every execution error fast and prevented building on unverified ground. The deviation log (D-B1…D-B4) preserved honesty about how the design was lowered. The empirical probe loop that cracked the `graph.js` substitution model was disciplined and decisive.
- **Failed.** Not a fault of the spec so much as a documented blind spot: the build had to **rediscover the `graph.js` authoring constraints empirically** because `minigraph-syntax.md` documented the verified-function table but not the rule this session pinned down: `{path}` substitution is innermost-first and a brace group whose inner text holds a colon/newline/tab/CR is left verbatim — so refs nested inside object literals / IIFE bodies resolve, and only a lone `{path}` sharing its braces with a colon/newline gets swallowed. (A COMPUTE may be single- or multi-line; both reach the JS engine identically — see the correction trail below.) The build leaned on a reference that had a hole. Separately, the happy-path-only smoke test gives a "PASS" that says nothing about branch logic — true to scope, but a source of false confidence if a reader forgets that.

  > **Correction trail (W41, 2026-06-10) — three records; the last was the wrong one.** The original bullet over-generalized one real footgun into "`{var}` swallowed inside any brace" — **wrong** (substitution is innermost-first; nested refs resolve; only a lone `{path}` sharing braces with a colon/newline halts). W39 corrected that and found **multi-line COMPUTE works** (`15`). A W40 revision then "re-corrected" to "multi-line **fails** (`404`)" with a "verified" tag and source citations — **itself wrong**: that `404` came from inspecting a **scalar leaf** (`enrich.result.mln`, a number → `404` *by design*, `InspectStateMachine.java:47-51` serves only `Map`/`List`), misread as a halt; the container always held `{mln:15}`. Re-verified W41 two independent ways — a direct re-run (distinct keys, reproduced twice, stored statement shown to retain its `\n`s) and a fresh-context `/graph-verify` reviewer given no hint of the verdict — **multi-line works.** The only COMPUTE footgun is a lone `{path}` sharing braces with a colon/newline; this is folded into `minigraph-syntax.md` (cited to source), along with the inspect scalar-leaf-404 gotcha that *caused* the W40 misread. §V's thesis turned recursive: *verify against the engine, and verify the verifications* — including a confident correction. The §IV-(a) "Improve" item is **done** (accurate version folded), not pending.
- **Improve.** (a) Fold this session's `graph.js` findings into `minigraph-syntax.md` — the doc system explicitly runs "one incident behind reality"; this is the incident. (b) Add the inspect-scalar-404 gotcha to the build debugging guidance (I briefly misread "all model paths 404" as state loss before recalling the endpoint only serves maps/lists). (c) Have the build gate state explicitly, in its output, that PASS covers buildability and the happy path *only* — to inoculate against the false confidence that the next phase then failed to puncture.

### `/graph-test`

- **Succeeded.** The seed→run→read-state discipline, `--expect` assertions over eyeballing, and the mandate to inspect *intermediate* state (not just final output) are all sound, and they did confirm the genuinely-correct behaviors (fail-closed no-leak, headroom absence on denial, distressed-mode reserve unlock).
- **Failed — the consequential failure of the session.** It passed a graph with a real logic bug, for three compounding reasons the spec does not guard against: (1) **the test oracle was authored by the same mind that held the misconception** — `T-09`'s "expected" output was the bug, written down; (2) the spec enumerates scenario *categories* (happy / missing / empty / failure / fallback / branch) but **not interaction or boundary cases** — and DEF-1 lives exactly at the interaction of two constraints; (3) the spec's boundary rule handles "the spec was *silent*" (→ requirements) but not **"the spec was *wrong* and the test faithfully agrees with it."** Proving conformance to a wrong spec yields a confidently-passing test for buggy behavior.
- **Improve.** (a) **Independent oracle authorship:** expected outputs for ship-class graphs should be derived by a fresh instance from the requirements *without sight of the implementation*, so the oracle is free to disagree with the code. (Separation of duties, applied to the test, not just the brief.) (b) **Mandate boundary and interaction scenarios** explicitly: for every pair of constraints that can co-bind, a case for each one binding alone *and* both binding, with the predicate dry-run independently. (c) Add a gate item: **"does each expected output trace to a requirement predicate, and has that predicate been re-derived here rather than copied?"** — re-examine the oracle, don't just execute it.

---

## V. The discipline that worked, and that I dropped

I applied independent adversarial review **once** — to the requirements brief — and it caught four real defects (K1–K4) that my self-gate had waved through. I then **dropped it** for design (self-gate), build (self-gate), and test (self-authored oracle). The one conceptual defect that survived to the end is precisely the one in a phase where I reviewed myself.

This is not a coincidence; it is the moral. The repo's own `adversarial-review-checklist.md` already says it (Separation of Duties; Incident 0002: "a self-review can pass every gate and still ship a flawed artifact"). DEF-1 is a fresh instance of Incident 0002, relocated from a spec doc to a **test oracle** — arguably a more dangerous host, because a green test suite *feels* like proof in a way a self-review does not. The checklist's Update Protocol arguably warrants a new incident entry to that effect.

Honest credit where due: when the user later arrived with four confident claims, I did *not* accept them sycophantically — I verified each against engine source and live runs, confirmed one (DEF-1), and refuted three with evidence (the NaN-race, the NPE-crash, and the null-metrics claims were all disproven by source or by a targeted run). That symmetric rigor — distrusting a confident critic exactly as much as a confident author — is the same muscle that should have been on the test oracle in the first place. It worked when consciously invoked; the failure was not invoking it by default.

---

## VI. Resolutions taken this session

- **DEF-1 fixed and re-verified.** Re-keyed the label on `hit_cap = total ≥ cap` rather than `req > head`. Re-ran the binding battery: shortage→`SHORTAGE/SUPPLY`, headroom cap→`SAFETY/HEADROOM`, supply-below-ceiling (old T-09)→`SHORTAGE/SUPPLY`, and a *new* genuine both-bind fixture (T-10, supply 8 > headroom 5, both < demand)→`SAFETY/BOTH`. Corrected graph re-exported.
- **Three refuted claims documented with evidence** under `adversarial_review_findings` in the test report, so the refutations are auditable, not just asserted.
- **Artifacts synced** so the corrected predicate lives in the brief, design spec, build log (DEF-1 cross-reference), and test report — no divergent truth surfaces.
- **The test fixture that lied was named and replaced**, not quietly patched: T-09 reframed as the supply-below-headroom case it actually is, T-10 added as the real both-bind.

## VII. The one change that would have prevented it

If I could install a single rule: **the test oracle for a ship-class graph must be authored independently of the implementation, and must include, for every pair of constraints that can co-bind, the case where each binds alone.** That rule, and only that rule among everything discussed, would have caught DEF-1 — because it would have produced a `T-09` whose expected output disagreed with the code, and the disagreement *is* the bug.
