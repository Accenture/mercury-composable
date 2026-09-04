# Memory Smoke Test — mercury-composable

> A cheap, manual check that the memory layer can actually orient a newcomer. **A fresh
> agent answers these from `memory/` alone** — no source code, no asking the user — then
> marks each ✅ (answerable from memory) or ❌ (gap). An ❌ is a *memory* gap: fix it by
> adding the missing fact, never by softening the question. App-level memory evaluation
> is an unsolved, bespoke problem industry-wide; this is the no-code, markdown version.

## How to run

1. Read **only** `memory/instructions.md`, `memory/continuity.md`, the unchecked (`- [ ]`)
   files in `memory/open-threads/`, the latest 2–3 `memory/sessions/`, and
   `memory/archive/INDEX.md`. Do not read source or ask the user.
   *(`open-threads/` joined the read set 2026-09-04: threads moved one-per-file in
   agent-memory v4.39.0, so Q5 became unanswerable without it — found by the smoke test.)*
2. Answer each question from those alone; mark ✅ or ❌ (with a one-line note on misses).
3. Append a row to the **Result log**. For each ❌, add the missing fact to memory (or
   open a thread to capture it) — then the next run should pass.

Run it **on demand** ("run the memory smoke test"), after a large change, or alongside a
review. Don't edit the questions to make them pass.

> Methodology note (2026-09-01): a Claude Code agent in this repo cannot be perfectly
> blind — CLAUDE.md auto-imports PROTOCOL.md, instructions.md, continuity.md and
> vision.md at session start. The constraint to honor is "answer only from the
> prescribed read set", and the runner should disclose the imports in its report.
>
> Methodology note (2026-09-04): **re-read `continuity.md` from disk before answering.**
> The auto-imported copy is a session-start snapshot and can be stale — in the
> 2026-09-04 run it still showed a pre-review `last_review` and carried an
> already-archived fact as live. Answering from the import alone would have
> reported archived facts as current.

## Orientation questions (generic — apply to any repo)

1. What does this project do, and what type is it? *(→ instructions "What This Project Is")*
2. What is the stack — language, key dependencies, versions? *(→ continuity "Stack & Tools")*
3. What are the architectural invariants — things that must never change? *(→ continuity "Architectural Invariants")*
4. What were the last 2–3 key decisions, and **why**? *(→ continuity "Key Decisions" / recent sessions)*
5. What is in progress right now? *(→ the unchecked `- [ ]` files in `memory/open-threads/`; continuity's "Open Threads" section is now only a pointer to that directory)*
6. What conventions should new code follow? *(→ instructions / continuity "Conventions")*
7. Any recorded user preferences or team / agent assignments? *(→ continuity — explicit only)*
8. Has any past decision been reversed or **superseded** — and by what? *(→ continuity superseded facts / `archive/INDEX.md`)*

## Project-specific questions (seeded at enable; grow these as the project does)

9. How are functions coupled to each other, and where does orchestration live — code or config? *(→ continuity "Architectural Invariants": functions-decoupled-routes)*
10. What are the I/O constraints of a `TypedLambdaFunction`? *(→ continuity "Architectural Invariants": typed-io-map-or-pojo)*
11. What is the source of truth for the version, and why does that matter here? *(→ instructions "Coordinates"/"Build"; the original fact pom-version-source-of-truth now lives in archive/INDEX.md)*
12. What are the steps to add a new capability, and what's the canonical reference example? *(→ continuity "Conventions")*

## Result log

| Date | Through session | Score (✅/total) | Gaps found → action |
|---|---|---|---|
| 2026-06-14 | (v3.7.0 upgrade) | — | baseline — run the test to populate |
| 2026-08-14 | 2026-08-14-005928 | 12/12 | none |
| 2026-08-22 | 2026-08-22-164936 | 12/12 | All questions pass (fresh-context agent, memory files only), plus 3 integrity findings: (1) dangling id `eric-release-rhythm` — six logs referenced a fact that lived only in an agent's personal store → materialized into continuity User Preferences under the same id; (2) Q11's pointer was stale (fact archived) → hint retargeted to the live homes; (3) observation for the team: several supersessions are narrated inline without `tier: superseded` footers — readable but not machine-traceable; consider footering at a future review. |
| 2026-09-01 | 2026-09-01-022524 | 12/12 | Ran as the onboarding-efficiency re-measure (fresh-context agent; no [[ref]] dangles — all resolve in archive/INDEX.md). Context-efficiency verdict: shipped-history share of continuity 64% (2026-08-21 baseline) → ~31%; "acceptable, trending lean". Gaps → actions same day: polyglot-initiative done-narrative condensed; "the field" glossary + placeholder-module note + playground run line + draft-design-specs status added to instructions.md; `latest_release` line added to Project State; vision.md current-state version refreshed to v4.12.0; stale "gitignored draft-design-specs" wording fixed. |
| 2026-09-04 | 2026-09-04-043732 | 11/12 | Fresh-context agent, memory files only; run alongside the review. **Q5 ❌ was a read-set regression, not a content gap** — Open Threads moved to `memory/open-threads/` in agent-memory v4.39.0 and the step-1 read set was never updated, so no compliant runner could enumerate a live workstream. Fixed here (read set + Q5 pointer + wiki-ref rule). Other fixes same day: `one-atom-four-roles` made self-resolving in `event-script-over-code` (it is a *concept* named in prose, never an id'd fact — an INDEX line would have fabricated an archive entry); `instructions.md` module map completed (twin-kafka, minimalist-kafka, ai-contract-provider, extensions/*); `vision.md` version replaced by a pointer to `latest_release` (it had drifted twice in 3 days). Methodology note added: re-read `continuity.md` from disk — the auto-import is a session-start snapshot and was stale this run. Rejected one finding: the `thread-<id>.md` filename rule is correct; the apparent double prefix is just ids that begin with `thread-`. Raised for Eric: `eric-release-rhythm` (uses 29, sole User Preferences fact) and `conv-telemetry-presentation-parity` (uses 42, most-used fact) both fell to `archive-candidate`. |
