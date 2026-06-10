# Graph Builder — Agent Distribution Plan

Status: active (drafted, independently reviewed three times, revised; not yet executed). See [Review dispositions](#review-dispositions).
Owner: maintainer of `docs/graph-builder/` — set an explicit owner before others execute against it.
Lifecycle: archive when D0–D5 close; supersede with a new dated plan rather than silently rewriting history.
Assessed against: commit `2ff2a082` ("workflow v1 complete, ready for walkthrough"), branch `feature/companion-skill-framework`. The four phase specs are validated and closed per [improvement-plan.md](./improvement-plan.md) (P0–P3, P5 closed; P4/CI deferred).
Date: 2026-06-09.

This plan covers a **new phase**: packaging the validated four-phase workflow as installable commands for multiple coding agents. It is distribution/product work, not doc-system governance — [worklog.md](./worklog.md) W30 closed the governance build-out, and this plan does **not** reopen it. Decisions below were locked with the maintainer on 2026-06-09 (see [Locked decisions](#locked-decisions)).

---

## Goal

Let a user on **any** of four agents run the workflow's four phases as native commands, installed by a single tool:

1. **Claude Code** — the reference implementation (built and dogfooded first).
2. **GitHub Copilot**
3. **GPT Codex**
4. **Amazon Kiro**

The four phases are the existing canonical specs — [requirements-gathering.md](./requirements-gathering.md), [graph-design.md](./graph-design.md), [build.md](./build.md), [test.md](./test.md). This phase does not change them; it wraps them.

---

## Locked decisions

Settled with the maintainer 2026-06-09. Treat as constraints, not open questions.

| # | Decision | Consequence |
|---|---|---|
| 1 | **Hybrid packaging:** one canonical body → generated per-agent wrappers. | Needs a small generator; no hand-maintained 4×4 matrix. |
| 2 | **Command body = the spec's own step checklist + gate + link to the canonical spec.** | The full spec ships alongside and is referenced. The wrapper carries the spec's `## Step checklist` (the author's hand-distilled step form) + its `## Gate`, **lifted verbatim by one extraction rule** — not a re-narration of the full spec, and not a hand-authored restatement that could drift (see D1 / Gate 3). |
| 3 | **Claude Code first, then template** the other three. | Prove + dogfood the pattern before replicating it (the W24 lesson). |
| 4 | **Namespaced command prefix** (fixed, not bare `/requirements`). | A chosen convention to keep the commands grouped. Prefix is **`graph`** (`/graph-requirements` …; locked item 1). Any *specific* collision (e.g. an existing `/graph-build` skill in a target agent) is a **D0 evidence item to verify**, not an assumed fact. |
| 5 | **Engine deps documented as prerequisites**, not wired by the installer. | Installer drops command files only. **Only `/build` and `/test` touch the engine** (Node ≥20 / `companion.mjs` / `MINIGRAPH_HOST` / stub server); `/requirements` and `/design` produce documents and need no engine. The prerequisites doc states this **per command**, so a user isn't told they need a live engine to run `/requirements`. |
| 6 | **Installer = a runnable Node CLI** — **no npm publish, no npx-from-git** (maintainer, 2026-06-09). | Run via `node docs/graph-builder/installer/bin/cli.mjs <cmd>`; dependency-free, Node ≥20, mirroring `companion.mjs`. **No root `package.json` is added to the repo** — the earlier npx wrinkle is moot. |
| 7 | **Per-project install only — no global `~/.claude` option** (maintainer, 2026-06-09). | The CLI installs into a project directory (`--to <dir>`, default cwd): wrappers to `<project>/.claude/skills/…`, specs to `<project>/.graph-builder/`. It never writes to the user's home config. |
| 7 | **Verify each agent's command format** before generating for it. | A hard gate (Gate 0): no wrapper is emitted against an asserted format. |

---

## Diagnosis / starting state

- The four phases are **validated specs but not installed anywhere** — there is no `.claude/commands`, no skill, no prompt file. Today a phase is "run" by pointing an agent at the doc. This phase closes that gap.
- The agents have **different native mechanisms** for custom commands (Claude skills/commands; Copilot prompt/chat-mode files; Codex `AGENTS.md` + prompt files; Kiro steering/specs). The hybrid generator (decision 1) absorbs that difference; the format-verification step (decision 7) keeps each adapter honest.
- The biggest authoring risk is **re-creating a parallel authority**: a distilled prompt that drifts from — or silently contradicts — its canonical spec. That is the exact failure the README precedence rule and Incident 0002 exist to prevent. The distilled prompts must stay **subordinate**: link to the spec, defer to it on any conflict, and never restate engine facts the spec owns.

---

## Source-of-truth layering

Three tiers, highest authority first. This is an application of the existing [README precedence rule](./README.md#authority-and-precedence), not a new one.

1. **Engine source / observed behavior** — authoritative (unchanged).
2. **Canonical phase specs** (`requirements-gathering.md`, `graph-design.md`, `build.md`, `test.md`) + `minigraph-syntax.md` — own all content, gates, and artifact schemas.
3. **Generated operating prompts** (new, this phase) — a spec's `## Step checklist` + `## Gate`, lifted for an agent to follow. Subordinate to tier 2 **by construction, not by promise**, via one mechanical extraction rule: *take a heading's content until the next heading of level ≤ its own.* That single rule extracts both sections from all four specs — **verified by running it** (W33): requirements' checklist (16 lines) stops at `## Step R1`, its gate (60 lines) captures the `### Gate A–D` sub-tables and stops at `## Mock-And-Proceed Rule`; design/build/test extract cleanly likewise. *(Prerequisite, now satisfied: all four specs carry a uniform `## Step checklist` + `## Gate`; `requirements-gathering.md` and `graph-design.md` were normalized to conform with [workflow.md](./workflow.md)'s "Common Phase-Spec Skeleton," which already mandates both — W33.)* The **distillation is the spec author's**: the `## Step checklist` is the hand-distilled step form that already lives in the spec; the generator lifts it **verbatim**, adding no paraphrase of its own. So "distilled" describes the source section and "verbatim" describes the lift — they coexist, and the wrapper body **is** the spec's text, unable to state something the spec doesn't. The only hand-authored wrapper part is the **connective framing** (command metadata, a one-line purpose, the invocation, the link to the full spec). This is what makes "subordinate" enforceable rather than aspirational.
4. **Generated per-agent wrappers** — pure output of the generator over tier 3. **Never hand-edited** (edits go to the spec — for the step/gate content — or to the framing template, then regenerate).

---

## Workstreams

Each lists **Why**, **Cost**, **Done when**, **Timing**. IDs are `D#` (distribution) to avoid collision with `improvement-plan.md`'s `P#`.

### D0 — Verify each agent's command format *(research; gates generation)*
For each agent, pin the **current** native command mechanism with a cited source: install location/path, file format, frontmatter/metadata schema, invocation syntax, and how namespacing/subdir nesting works.
- **Why:** decision 7. The agents' formats (Kiro and Codex especially) move faster than training knowledge; "verified, not asserted" is this repo's standing rule.
- **Cost:** low–medium per agent.
- **Output:** a per-agent format table in `docs/graph-builder/evidence/agent-command-formats.md`, one cited row per agent. Each row also records **whether the phase command names collide** with anything that agent ships by default (the decision-4 collision check — verified, not assumed).
- **Done when:** Claude Code is fully verified + cited (format **and** collision check); the other three each have a verified-or-explicitly-flagged row before any generation targets them.
- **Timing:** now — Claude first; the other three just-in-time before D5 templates them.

### D1 — Define the extraction + author the connective framing *(the generator's source)*
The wrapper body is **not** a hand-written restatement of the spec. Instead:
- **Extraction (mechanical):** the wrapper's deterministic body is the spec's `## Step checklist` + `## Gate`, lifted **verbatim** by one rule — *a heading's content until the next heading of level ≤ its own*. Verified to extract cleanly from all four specs (W33). No per-spec section-map of delimiters is needed: the single rule suffices — requirements' richer `### Gate A–D` sub-tables are captured because the rule descends into deeper headings and stops only at the next `##`.
- **Connective framing (hand-authored, small):** per phase, write only the command metadata, a one-line purpose, the invocation line, and the link back to the full spec. Style: directed and deterministic (decision 2 / maintainer note) — the framing routes the agent into the extracted step sequence; it does not duplicate it.
- **Why:** decision 1 + the K2 mitigation (see Source-of-truth layering / Review dispositions). Making the body a generated projection is what makes "subordinate" structural.
- **Cost:** low–medium (the judgment is in the section-mapping, done once; not in re-prose-ing four 200-line specs).
- **Done when:** the per-phase section-map exists, the four framing templates exist and link their spec, and no framing template restates a step sequence, gate, engine fact, or schema the spec owns. Whether to also inline the artifact schema is **deferred to D4** (dogfooding will show whether the agent needs it inline).
- **Timing:** alongside D0-Claude.

### D2 — Generator + Claude Code target adapter
`generate(phase_def, target) → wrapper file`, where `phase_def` = the framing template + the extracted spec sections (D1). The Claude adapter emits the D0-verified Claude format with the `graph` prefix (`/graph-requirements` …). **The generator must be idempotent** — re-running it over an unchanged spec + template produces a byte-identical wrapper (this is what Gate 3 checks). Dependency-free, Node ≥20 (mirrors `companion.mjs`).
- **Why:** the hybrid mechanism (decision 1).
- **Cost:** medium.
- **Done when:** running the generator produces four Claude command wrappers that match the D0-verified Claude format, carry the namespaced prefix, embed the extracted step/gate content, and regenerate idempotently.
- **Timing:** after D0-Claude + D1.

### D3 — npx CLI + prerequisites doc
A runnable Node CLI (`node docs/graph-builder/installer/bin/cli.mjs install claude --to <project>`; decision 6, locked item 3) that copies the referenced canonical spec set into **`<project>/.graph-builder/`** (locked item 2) and writes the generated wrappers into **`<project>/.claude/skills/…`**. **Per-project only — never the home config** (decision 7). A `check` subcommand regenerates and diffs the installed wrappers (Gate 3a). The README states the engine prerequisites the installer deliberately does **not** wire (decision 5), **per command**: `/graph-requirements` and `/graph-design` have no engine prerequisite; `/graph-build` and `/graph-test` need Node ≥20 + `companion.mjs` + a reachable `MINIGRAPH_HOST` (+ the stub-server pattern for mocked sources in `/graph-test`).
- **Why:** decision 6.
- **Cost:** medium.
- **Done when:** the CLI installs the Claude command set into a target dir, the referenced specs resolve from the installed location, and the prerequisites doc exists.
- **Timing:** after D2.

### D4 — Dogfood the Claude reference end-to-end *(gates templating)*
Install via the CLI into a scratch/target, then **actually invoke the namespaced commands in Claude Code** and drive the workflow — at minimum `/<prefix>:requirements` → `/<prefix>:design`, ideally the full pipeline against the live engine reusing `customer-360` or the `classify-score` example. Fix gaps the run reveals.
- **Why:** W24 is explicit — execution caught what five review rounds did not. Prove the reference before replicating it (decision 3).
- **Cost:** medium.
- **Done when:** a no-extra-guidance run drives the phases through the installed commands and produces the phase artifacts; gaps found are fixed in the framing templates / adapter and re-verified. D4 also **records two verdicts as named outputs**: (1) the K1 kill-switch — was the generator worth it vs. committed files + an install doc; (2) open item 4 — does the artifact schema need to be inlined in the wrapper or does linking the spec suffice.
- **Timing:** after D3. This is Gate 1.

### D5 — Template the remaining three agents *(cleanest to verify first)*
For each of Copilot / Codex / Kiro, in order of which format verifies fastest: complete its D0 row, add its target adapter, generate, install via the CLI, and smoke-verify the command loads and drives the phase.
- **Why:** the pattern is proven once D4 passes; replication is now mechanical adapter work.
- **Cost:** medium (mostly per-agent format edge cases).
- **Done when:** all three have a verified adapter, an installable command set, and a passing smoke check (Gate 2 each).
- **Timing:** after D4.

---

## Recommended sequence

```text
D0(Claude) ─┬─> D2 ─> D3 ─> D4 ──> D5 (Copilot / Codex / Kiro, cleanest-first)
D1 ─────────┘                       ▲ each agent needs its own D0 row first
```

D0+D1 first (cheap, gate the rest). D4 is the proof gate before any templating. D5 last.

---

## Deletion list — do NOT do

- **A distilled prompt that competes with its spec.** It defers to the canonical spec and never owns engine facts/schemas (the P2 / Incident 0002 / W30 "no parallel truth surfaces" trap).
- **Hand-editing generated wrappers.** They are output; edits go to the distilled-prompt source, then regenerate. A hand-edit is silent drift.
- **Bundling engine setup into the installer.** Prerequisites only (decision 5).
- **Asserting an agent's format from memory.** Every format claim is cited in `agent-command-formats.md` before generation (decision 7 / Gate 0).
- **Publishing to npm.** Not this phase (decision 6 — npx-from-git only).
- **Reopening governance.** W30 closed it; this is product work and stays proportionate.

---

## Decision gates

Stop/go checkpoints, each naming a checkable artifact by path:

- **Gate 0 (before generating for *any* agent):** that agent's row exists in `docs/graph-builder/evidence/agent-command-formats.md` and is cited to a current source. No wrapper is generated against an unverified format.
- **Gate 1 (after D4):** the Claude command set installs via the CLI **and** a dogfood run drives the workflow through the installed commands without extra guidance, producing the phase artifacts. Templating (D5) does not begin until this passes.
- **Gate 2 (per templated agent in D5):** the generated wrappers install into that agent's verified location, the agent recognizes the command, and the distilled prompt drives the phase in a smoke check.
- **Gate 3 (single-source integrity, checkable):** two parts, because a byte-diff alone is not proof of faithfulness.
  - **(a) Idempotency** — re-running the generator over the current specs + framing templates reproduces every committed wrapper **byte-for-byte**; a non-empty diff means a wrapper was hand-edited or a spec changed without regeneration. Artifact: generator `--check` mode that exits non-zero on drift. *This catches hand-edits — not a faithless extraction.*
  - **(b) Extraction fidelity** — because the body is a verbatim lift, fidelity is itself mechanically checkable: assert each wrapper's step/gate block is **string-equal** to the result of applying the extraction rule to the current spec. This is a script (folded into the same `--check`), not a dogfooding deferral. D4 still confirms the right sections drive the phase, but a single fixed rule over uniform headings leaves no room for a wrong choice.

---

## Locked items (settled 2026-06-09)

1. **Command prefix: `graph`.** The four commands are `/graph-requirements`, `/graph-design`, `/graph-build`, `/graph-test` — rendered as `/graph:requirements`-style where an agent supports `:` namespacing. Meaningful, short, and works as both a hyphen-prefix and a colon-namespace across agents. (Any *specific* collision is still a D0 evidence check per decision 4.)
2. **Referenced specs live in `.graph-builder/` at the target repo root.** The CLI copies one shared canonical spec set there at install; every agent's wrappers reference that path. Tool-owned dot-dir — doesn't touch the user's `docs/`, travels with the repo (offline, version-pinned at install), no per-agent duplication.
3. **Installer home: `docs/graph-builder/installer/`** (in *this* repo). Co-located with the specs it serves, consistent with the existing pattern that graph-builder's executable tooling (`companion.mjs`, `evidence/stub-server.mjs`) already lives under `docs/graph-builder/`. Package: `type: module`, `engines.node >= 20`, no deps. *Invocation (resolved, decision 6):* `node docs/graph-builder/installer/bin/cli.mjs install claude --to <project>` — no npm publish, no root `package.json`, no npx.

## Open items (decided during execution, not now)

4. **Schema inline vs. linked** in the wrapper — decided at **D4** as a named output (dogfooding shows whether the agent needs the artifact schema inline), not deferred to nowhere.
5. **Cleanest-to-verify order** for the three remaining agents — decided during **D0** by which format verifies fastest against a live check.

---

## Review dispositions

An independent reviewer (fresh instance, authoring context withheld — satisfying the [adversarial-review-checklist](../adversarial-review-checklist.md) Separation-of-duties rule) reviewed the v1 draft on 2026-06-09 and returned **NO-SHIP-as-an-execute-plan**. Each finding's disposition, made visible per Gate G:

| # | Finding | Disposition |
|---|---|---|
| K1 | Gate B: generator + npx CLI not justified vs. 16 committed files + an install doc; cost depends on format complexity. | **Overridden (maintainer call), reasoning corrected.** *Consistency* is delivered by single-sourcing the spec, not uniquely by the generator (the second review was right that the first override double-counted it). The generator's *unique* value is two things committed files don't give: (i) porting one body into N different agent formats without hand-copying it N times, and (ii) regenerating all agents when a spec changes. That is the accepted bet. The residual risk (it under-pays for a 4-agent matrix, and neither CI nor `--check` is built yet) is real; **D4 is the explicit kill-switch** — it records whether the generator earned its place vs. committed files before D5 pays the cost 3 more times. |
| K2 | The "distilled operating prompt" tier is an unenforced second authority (Incident 0002 / W19 pattern); Gate 3 named no artifact. | **Applied.** The wrapper's step sequence + gate are now *extracted from the spec at generate-time*, not hand-authored (decision 2, Source-of-truth layering tier 3, D1, D2). Gate 3 is now a byte-for-byte regenerate-and-diff check with a named artifact. Subordination is structural, not promised. |
| K3 | Proportionality vs. W30: the D0–D5 + gates + deletion-list apparatus reproduces the over-spend W30 closed. | **Overridden, reasoning corrected.** "It was requested" is provenance, not worth (and author/requester being the same party makes it circular). On worth: the gates + deletion list are a few paragraphs that make each workstream checkable by a named artifact — cheap relative to wiring four agents — and the plan stays one planning authority, D4-gated, with **no new governance docs**. If it sprawls past this, cut it. |
| K4 | The `/build` `/test` collision justification was asserted, not verified — violating the plan's own Gate 0 / "verified not asserted." | **Applied.** Decision 4 reframed: namespacing is a chosen convention; any specific collision is a D0 evidence item to verify, not an assumed fact. |
| K5 | "Engine deps as prerequisites" framed workflow-wide, but only `/build` + `/test` touch the engine. | **Applied.** Decision 5 + D3 now state engine dependence *per command*; `/requirements` and `/design` need no engine. |

A **second** independent reviewer (fresh instance) re-reviewed the revised plan on 2026-06-09 and returned **NO-SHIP**, catching that the K2 fix above rested on a false premise. Dispositions:

| # | Finding | Disposition |
|---|---|---|
| Kill #1 | The K2 "extract from the spec" mechanism assumed all four specs carry uniform `## Step checklist` + `## Gate` sections — but `/requirements` and `/design` did not (verified by grep). The plan asserted the uniformity it needed. | **Applied — the central fix.** Normalized `requirements-gathering.md` and `graph-design.md` to carry both sections (W33), conforming with workflow.md's mandated skeleton. The premise is now true rather than assumed; the projection claim holds. |
| Kill #2 | Even where extraction works, judgment relocates into the section-map; "nothing is hand-authored" overclaimed. | **Applied.** Extraction is now **verbatim** of named headings (not a distilled paraphrase), so the map selects *which* section, never *what it says*. The body equals the spec text by construction. |
| Kill #3 | Gate 3's "a passing diff *is* proof no wrapper contradicts its spec" was an overclaim — a diff catches hand-edits, not a faithless map. | **Applied.** Gate 3 split into (a) idempotency (a script) and (b) extraction fidelity (verbatim-by-construction + D4 as the fidelity gate). The overclaim sentence is gone. |
| Kill #4 | K1 override double-counted "consistency" (single-sourcing gives it) and omitted the generator's only unique benefit (regen/format-porting). | **Applied.** K1 override reasoning corrected (above): consistency credited to single-sourcing; the generator justified by format-porting + regen; D4 is the kill-switch. |
| Kill #5 | K3 override answered a worth question with provenance ("it was requested"), author = requester. | **Applied.** K3 override reworded to a worth basis (above). |
| Kill #6 | Open item 4 deferred a decision to D4, which had no obligation to make it. | **Applied.** D4 now records it (and the K1 verdict) as named outputs; open item 4 reworded. |

A **third** independent reviewer re-reviewed the plan + the W33 spec edits on 2026-06-09 and returned NO-SHIP on both. Two central findings were verified **false** by running the extraction rule (precedence #1: observed behavior over prose); the rest were applied:

| # | Finding | Disposition |
|---|---|---|
| Kill A | No single rule extracts all four specs uniformly (requirements' checklist "stops at `## Step R1`"; its gate carries `### Gate A–D` sub-tables the others lack). | **Overridden — verified false.** One rule — *a heading's content until the next heading of level ≤ its own* — extracts both sections from all four specs (run in W33: requirements checklist 16 lines, gate 60 lines incl. `### Gate A–D`; design/build/test clean). Stopping at `## Step R1` is *correct* for the checklist lift — checklist and gate are separate extractions. Legitimate kernel: the rule was unstated. Fixed — tier 3 / D1 now name it. |
| Kill B | The new `## Step checklist` duplicates the R#/D# detail = a within-spec parallel surface; violates requirements' Gate D. | **Overridden — it's the mandated skeleton.** [workflow.md](./workflow.md)'s Common Phase-Spec Skeleton lists Operating protocol *and* Step checklist as separate sections; `build.md`/`test.md` already carry both. W33 brought the two laggards into conformance — it didn't invent a surface. Gate D bars a divergent *gate* checklist vs the step quality-checks, not a step index (which points to the R#/D# sections as the detail). |
| Kill C | "Verbatim lift" contradicts decision 2's "distilled / lean". | **Applied.** Reworded: the `## Step checklist` is the spec author's hand-distilled form; the generator lifts it verbatim. "Distilled" = source section, "verbatim" = the lift. Dropped the blanket "lean". |
| Kill D | Gate 3(b) fidelity deferred to D4 ⇒ Gate 3 not fully checkable. | **Applied as an upgrade.** Verbatim lift makes fidelity a string-equality check (wrapper block == extraction of current spec), folded into `--check`. No longer deferred. |
| Kill E | Three reviews + a disposition ledger + Meta prose is disproportionate (W30). | **Partially applied.** Apparatus kept (bounded, D4-gated, no new governance docs); the Meta paragraphs + confidence note were cut from this section — they live in the worklog (W31–W34). |
| Kill F | K1/K3 overrides still partly alibis. | **Applied.** K3's circular "prevents the failure modes this review chain exercised" sentence cut; K1 already defers worth to the D4 kill-switch. |

Two verified-false findings in one pass is itself a signal: adversarial review is now turning up wording, not substance. Per W30's diminishing-returns caution, further review rounds are unlikely to pay.

## Keeping this doc fresh

Before executing any workstream, confirm the four phase specs are still closed in [improvement-plan.md](./improvement-plan.md) and that HEAD still matches the `Assessed against` commit; if not, re-ground before acting. As `agent-command-formats.md` rows are filled, treat a row as stale if the agent ships a new command mechanism — re-verify before regenerating that adapter.
