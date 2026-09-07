# Claims-fixture gate — drift-testing documentation *behavior* claims

**Status:** IMPLEMENTED · **ADR-0023 Accepted** (Eric, 2026-09-06 — entry in
`docs/arch-decisions/ADR.md`)
**Motivation:** the AI grammar coverage study (`ai-grammar-coverage-study.md`) — all 10
doc-drift findings were *prose behavior claims* (defaults, precedence, thresholds, failure
semantics) in exactly the tier no existing gate covers, while the gated surfaces (DSL
catalogs, guide fixtures, golden vectors) held almost perfectly. This gate extends "compiled
documentation" from *shape* to *behavior*.

## The learnings this design incorporates

1. **Drift is asymmetric.** Docs drift where prose asserts engine behavior nobody executes:
   the study's ten items had lived through multiple releases. A claim is only "verified" when
   *both sides* are pinned — the doc sentence and the engine behavior.
2. **The divergence-note near-miss.** The Rust edition's `flow-schema-reference.md` had
   already *observed* the `error.status` drift and documented around it instead of fixing
   upstream. A registry gives an observation like that a place to land as a failing check,
   not a footnote.
3. **The registry is itself grammar.** A machine-readable list of CI-pinned claims is exactly
   the "verified" property agents are told to trust — so it lives in `docs/guides/` and ships
   inside the packaged contract like the other catalogs.
4. **Cheap beats clever.** Line numbers rot and AST-level doc parsing is over-engineering.
   A distinctive quoted substring per claim, checked with whitespace-normalized,
   case-insensitive containment, is deterministic and stdlib-only — it forces the normative
   sentence to stay present (re-casing passes; removal or rewording fails).

## Mechanism

**1. The registry** — `docs/guides/claims-registry.json`:

```json
{ "id": "error-dataset-code-key",
  "claim": "The flow exception context exposes the HTTP status as error.code (not error.status).",
  "pages": ["docs/guides/event-script/flow-grammar.md", "docs/guides/event-script/syntax.md"],
  "quote": "error.code - the status code of the exception",
  "test": "system/event-script-engine::FlowTests#subflowTimeoutIsCatchableWithTaskTtlOverride",
  "since": "4.12.3+" }
```

- `quote` — a distinctive substring that must appear (whitespace-normalized) in at least one
  of `pages`. Removing or rewording the normative sentence breaks the gate, so a doc edit
  that would reintroduce drift is caught at CI time.
- `test` — `module-path::TestClass#method`, an engine test that fails if the behavior
  changes. The gate verifies the referenced method still exists in that module's test tree,
  so a renamed or deleted pin cannot silently orphan a claim.

**2. The doc-side gate** — `scripts/check-doc-canon.py` **check 8**: for every registry
entry, (a) each `pages` path exists, (b) `quote` appears in at least one page, (c) the
`test` reference resolves to an existing test method. Stdlib-only, runs with the existing
canon checks in the docs workflow and the same script locally.

**3. The engine-side pins** — ordinary JUnit tests. Existing tests are referenced where they
already pin a claim; a new pin gets its own narrowly-named class per claim
(`Claim<Topic>Test`, e.g. `ClaimConfigYmlWinsTest`) in the module's existing test package.
The tests run in the normal build — no new CI step. A claim's registered sentence states
exactly what its named test pins — documentation may say more than a pin covers, but the
registry never does.

## First fixture set (v1)

Eleven claims: eight of the coverage study's ten §4A drift items (the two others — a
non-compilable snippet and a stale console transcript — are content fixes with no behavior
claim to pin), plus the decision-value coercion surprise (§4B-5), the `stream` catalog field
(§4B-3), and the nine-name reserved-key set. Seven pins are new `Claim*Test` classes; four
reference existing tests the study's verification identified. See the registry for the
authoritative list.

## Rust portability

The format is engine-neutral: the Rust repo carries its own `claims-registry.json`, its own
check in `scripts/`, and its own test pins; an engine-specific divergence (e.g. "the Rust
engine produces no stack trace") becomes a claim in that repo's registry rather than a prose
footnote. Porting waits on the held sibling sweep (`ot-rust-docs-sibling-sweep`).

## Non-goals

- Not a prose linter, not NLP: the gate checks presence of pinned sentences and existence of
  pinned tests. Judgment about *which* claims deserve pinning stays human/agent-curated
  through the feedback circuit.
- Not exhaustive: the registry grows claim-by-claim as field findings arrive (the same
  friction → fix → gate loop the grammar already runs). Ten to twenty high-value claims is
  the intended steady state, not hundreds.

## ADR-0023 (accepted — the authoritative entry lives in the ledger; draft kept for provenance)

> **ADR-0023 — Claims-fixture gate: documentation behavior claims are drift-tested**
> Accepted YYYY-MM-DD. High-value prose claims about engine behavior (defaults, precedence,
> thresholds, failure semantics) are registered in `docs/guides/claims-registry.json`; CI
> verifies the claim sentence still appears on its page and the named engine test still
> exists, and the named tests pin the behavior in the normal build. Context: the AI grammar
> coverage study found all documentation drift concentrated in ungated prose. Consequence:
> "verified" extends from catalog shape to stated behavior; the registry ships inside the
> version-matched contract as a grammar asset. The Rust engine carries its own registry
> (formalizes: the cross-engine docs-as-contract discipline).
