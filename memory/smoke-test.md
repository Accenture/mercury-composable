# Memory Smoke Test — mercury-composable

> A cheap, manual check that the memory layer can actually orient a newcomer. **A fresh
> agent answers these from `memory/` alone** — no source code, no asking the user — then
> marks each ✅ (answerable from memory) or ❌ (gap). An ❌ is a *memory* gap: fix it by
> adding the missing fact, never by softening the question. App-level memory evaluation
> is an unsolved, bespoke problem industry-wide; this is the no-code, markdown version.

## How to run

1. Read **only** `memory/instructions.md`, `memory/continuity.md`, the latest 2–3
   `memory/sessions/`, and `memory/archive/INDEX.md`. Do not read source or ask the user.
2. Answer each question from those alone; mark ✅ or ❌ (with a one-line note on misses).
3. Append a row to the **Result log**. For each ❌, add the missing fact to memory (or
   open a thread to capture it) — then the next run should pass.

Run it **on demand** ("run the memory smoke test"), after a large change, or alongside a
review. Don't edit the questions to make them pass.

## Orientation questions (generic — apply to any repo)

1. What does this project do, and what type is it? *(→ instructions "What This Project Is")*
2. What is the stack — language, key dependencies, versions? *(→ continuity "Stack & Tools")*
3. What are the architectural invariants — things that must never change? *(→ continuity "Architectural Invariants")*
4. What were the last 2–3 key decisions, and **why**? *(→ continuity "Key Decisions" / recent sessions)*
5. What is in progress right now? *(→ continuity "Open Threads")*
6. What conventions should new code follow? *(→ instructions / continuity "Conventions")*
7. Any recorded user preferences or team / agent assignments? *(→ continuity — explicit only)*
8. Has any past decision been reversed or **superseded** — and by what? *(→ continuity superseded facts / `archive/INDEX.md`)*

## Project-specific questions (seeded at enable; grow these as the project does)

9. How are functions coupled to each other, and where does orchestration live — code or config? *(→ continuity "Architectural Invariants": functions-decoupled-routes)*
10. What are the I/O constraints of a `TypedLambdaFunction`? *(→ continuity "Architectural Invariants": typed-io-map-or-pojo)*
11. What is the source of truth for the version, and why does that matter here? *(→ continuity "Key Decisions": pom-version-source-of-truth)*
12. What are the steps to add a new capability, and what's the canonical reference example? *(→ continuity "Conventions")*

## Result log

| Date | Through session | Score (✅/total) | Gaps found → action |
|---|---|---|---|
| 2026-06-14 | (v3.7.0 upgrade) | — | baseline — run the test to populate |
