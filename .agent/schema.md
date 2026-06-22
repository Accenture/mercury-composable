# Memory File Schema

Format reference for all memory files. Use this when creating missing files
or when an agent needs to understand expected structure.

---

## memory/instructions.md

Stable project context and agent rules. Edit rarely.

```
# Agent Instructions — {project name}

## What This Project Is
## Repository Structure
## Module Inventory     (monorepos only — remove for single-package repos)
## Conventions Observed
## Tone & Style
## Core Rules
## Testing
## CI / CD
```

Keep this file at the **enduring** altitude: what kind of project it is, its
structure, and conventions that rarely change. The precise, *volatile* stack facts
(current language version, dependency list, tool versions) are **not** duplicated
here — they live in `continuity.md` → `## Stack & Tools` (the live source of truth).
A one-line high-level descriptor here (e.g. "async Rust CLI") is enough; point to
continuity for specifics.

---

## memory/continuity.md

Live project state. Update every session.

```
# Continuity — {project name}

## Project State
- project:        string
- status:         string
- last_enabled:   YYYY-MM-DD
- last_session:   YYYY-MM-DD | agent: string          (or "none yet")
- last_review:    YYYY-MM-DD | through <session-file>  (or "none yet")
- last_invariant_check: YYYY-MM-DD | through <session-file>  (or "none yet") — see REVIEW.md step 6
- repo:           ~-relative path (e.g. ~/projects/foo) — NEVER absolute /Users/<name>/…; memory is committed & shared

## Architectural Invariants  hard constraints; never decay (omit the section if none)
## Stack & Tools             canonical live home for language/deps/tool versions (key: value)
## Key Decisions             bullet list, present tense
## Conventions               bullet list
## Open Threads              - [ ] incomplete  /  - [x] complete (leave [x] for the review to sweep)
## User Preferences          bullet list — record ONLY what the user explicitly states; never infer
## Team / Members            name: preferred agent
```

`## Stack & Tools` is the single canonical home for the current stack — language
version, dependencies, tool versions. `instructions.md` gives only an enduring
high-level descriptor and points here; don't maintain the dep list in both.

Each fact carries a metadata footer (HTML comment), maintained by the review ritual
— invisible when rendered, read/written by agents:

```
<!-- id: kebab-id | created: YYYY-MM-DD | last_used: YYYY-MM-DD | uses: N | tier: core|active|working|archive-candidate|superseded -->
  id         stable, unique within the file, assigned once at creation
  created    date the fact entered memory
  last_used  date of the most recent session referencing the id  (recomputed at review)
  uses       count of sessions referencing the id                (recomputed at review)
  tier       lifecycle bucket — see DECAY.md / REVIEW.md at the repo root
  superseded-by / supersedes   (optional) supersession links when a fact is replaced or invalidated — see DECAY.md §9
  origin                       (optional) the session-log file where the fact was Created — provenance; see DECAY.md §11
```

**At creation** the agent sets only `id`, `created`, and `tier` — `working` for an
ordinary fact, `core` for an Architectural Invariant — and seeds `last_used: <today>
| uses: 1`. It does **not** hand-edit `uses`/`last_used`/`tier` afterward; those are
recomputed by the review from session-log `## Memory References` (see `DECAY.md` §1).

`## Architectural Invariants` facts and unchecked Open Threads (`- [ ]`) never decay.
Completed threads (`- [x]`) stay in place until the review sweeps them (see below /
`REVIEW.md`) — don't archive them by hand.

When a fact becomes **false** (a decision reversed, a dependency dropped), don't just
delete it: set its footer to `tier: superseded` + `superseded-by: <new-id>` (omit the
link for pure invalidation), record `Superseded: <old> → <new>` in the session log,
and let the review archive it flagged "superseded." See `DECAY.md` §9.

---

## memory/sessions/YYYY-MM-DD-HHMMSS.md

**A "session" is one write of a session-log file** — the unit of work since the last
log was written, not necessarily a whole conversation. A single long conversation
that spans several distinct tasks may produce **multiple** session logs (one per
work segment); that is expected and correct. The decay math counts session *files*
(`DECAY.md` §4), so each log is one event regardless of how conversations are sliced.

Name the file with the UTC timestamp at **persist time** — the moment you write it.
Use `date -u +%Y-%m-%d-%H%M%S` or equivalent; colons omitted for cross-platform
filename compatibility. Filenames sort lexicographically = chronologically, so the
most recent log is always the last file — unambiguous even with multiple
contributors on the same day.

```
# Session (YYYY-MM-DDThh:mm:ss.mmmZ)

**Agent:** string
**User:** brief task context

## What We Did
Prose summary, 2–5 sentences.

## Decisions Made
Bullet list (if any).

## Context for Next Session
What the next agent needs to know.

## Memory References
- Referenced:  <continuity fact ids this session relied on / reinforced>
- Created:     <new fact ids added this session (born tier: working)>
- Reactivated: <fact ids pulled back from the archive>
- Superseded:  <old-id → new-id, or old-id (invalidated) — facts made false this session; see DECAY.md §9>
```

The `## Memory References` section is the **event log** the review ritual reads to
recompute `uses`/`last_used` (see `DECAY.md` §2). List fact ids, not prose; omit any
line that doesn't apply. Don't edit metadata on facts mid-session — just record the
ids here; the review does the counting.

Title format: `# Session (endZ)` — the persist-time UTC timestamp (full ISO 8601 with
milliseconds) is **required**. A start time is **best-effort and optional**: if you
genuinely tracked when you began, you may write `# Session (startZ - endZ)`, but do
not fabricate one — a consuming agent rarely knows its exact start instant, and the
persist time already orders the log.

Rules: never edit past files. Each session-log write creates its own file. To resume
context before responding, sort `memory/sessions/` lexicographically and read
the most recent 2–3 files.

---

## memory/sessions/INDEX.md  (optional)

A lightweight, one-line-per-session index so agents can orient without listing or
opening files: `YYYY-MM-DD-HHMMSS — <agent> — <one-line summary>`. Optional and
progressive — maintain it only if the team wants it. If kept, append one line each
session. A stale index is worse than none, so skip it rather than let it drift. With
`archive/INDEX.md` and fact `origin` pointers, this is the layer's retrieval-at-scale
story — lexical + indexed, by design (see `DECAY.md` §11).

---

## memory/decay-policy.md

Tunable integer windows + triggers for the evolving-memory layer (`working_window`,
`active_window`, `archive_window`, `review_every`, `continuity_max_lines`,
`verify_invariants_every`, and auto-core). All windows are in **sessions**. The rules these feed live in `DECAY.md`
and `REVIEW.md` at the repo root.

---

## memory/smoke-test.md

A short, manual memory-quality check: N questions a *fresh* agent should answer from
`memory/` alone (generic orientation questions + project-specific ones seeded at enable).
Each run marks ✅/❌ and appends a result row; a ❌ is a memory gap to fill, not a question
to soften. Run on demand or alongside a review. App-level memory eval is unsolved
industry-wide — this is the no-code version.

---

## memory/vision.md  (forward layer — VBDI)

The **north star**: the project's target future state — what should exist, for whom,
success criteria, explicit non-goals. One per repo. Carries a kebab `id`
(`vision-<slug>`), tier `core` (never decays) but re-confirmed on the
invariant-verification cadence (a vision can go stale). Created at enable/upgrade as a
⚠️ DRAFT stub — Current-state context inferred, target left for the human — **never
fabricated**. See `DECAY.md` §12 and `docs/DESIGN-vbdi-lifecycle.md`.

The **Blueprint** (the Vision↔Current-State gap) is *not* a separate file — it is a set
of typed Open Threads in `continuity.md`:
`- [ ] (blueprint) <gap> → serves: <vision-id>`. Designs (Key Decisions) and
Implementations (commits/sessions) trace up the altitude chain by `id`; a missing or
broken link is drift, and it's grep-detectable.

---

## docs/ADR.md  (optional — human-facing governance, on-demand)

An **optional** Architecture Decision Record log: a human-facing ledger of significant,
durable architecture decisions, one per entry, at the VBDI **Design** altitude. It lives
under `docs/` so a human (IT governance, a newcomer, an auditor) can find it, and is read
**on demand** — it is **never added to the per-session read path** (zero default token cost,
like `docs/DESIGN-*.md`). Not auto-installed; adopt it only if the team wants one.

**Map, don't duplicate.** Live constraints stay in `continuity.md`
(`## Architectural Invariants` / `## Key Decisions`) — the *what* that holds *now*, with an
`id`. An ADR is the durable *why* (context, alternatives, consequences). They cross-link:
`formalizes: <continuity-id>` on the ADR ↔ a visible **`(ADR-NNNN)` tag in the invariant's
title** (e.g. `Target-repo scope only (ADR-0001)`). That tag is a **pointer for humans** — it
is **not** a cue for the agent to open `docs/ADR.md`; the constraint text in `continuity.md`
stays authoritative and is read every session, the ADR is read on demand only. The constraint
text is never restated as competing truth.

```
## ADR-NNNN — <Title>
**Status:** Accepted · **Date:** YYYY-MM-DDThh:mm:ss.mmmZ · **Serves:** <vision-id>
<!-- id: adr-NNNN | status: accepted | formalizes: <continuity-id> -->

**Abstract.** What was decided + scope.
**Rationale.** Why, alternatives, and the consequences/trade-offs accepted.
```

Status: `Proposed → Accepted → Superseded / Deprecated`. **Never deleted** — a decision that
no longer holds is superseded (replaced by a newer ADR) or deprecated (no longer relevant),
its entry left in place with `Status` updated (mirrors `DECAY.md` §9). Numbering is monotonic;
entries are listed **newest first**.

**When to maintain it.** Adopting the log is on-demand, but once it exists it is **kept in
sync**: when a new durable architecture decision is made, or a continuity fact carrying an
`(ADR-NNNN)` tag is superseded/invalidated, the agent **proposes** the matching ledger edit
(a new ADR, and/or the old one's `Status` → `Superseded`/`Deprecated`), keeping `formalizes:`
↔ `(ADR-NNNN)` consistent. Like every Design-altitude change it is a **human gate** — the agent
proposes, the human approves; it is the one time the on-demand ledger is opened during a session.

---

## memory/archive/

Cold storage for archived facts and swept completed threads. Nothing here is
deleted; reactivation moves a fact back into `continuity.md` (see `REVIEW.md`).

```
archive/
  YYYY-QN.md   facts (with their metadata footers) moved out of continuity.md, grouped by quarter
  INDEX.md     one line per archived fact: `id — one-line summary — <quarter file>`  (greppable)
```

---

## agent-skills/  (capability layer — cross-vendor)

Portable, committed **capabilities** — the third shared leg alongside memory and
steering. Each skill is vendor-neutral markdown:

```
agent-skills/
  <skill-name>/
    SKILL.md     frontmatter (name + description = the when-to-use trigger; optional provenance) + the procedure
    scripts/     (optional) portable helpers (sh / python), referenced by relative path
```

`agent-skills/` is **committed** — it travels with the repo and reaches every contributor, on
any vendor. The `AGENTS.md` "Skills" section is the **universal runtime**: when a task
matches a skill's `description`, the agent reads and follows that `SKILL.md` — no
per-vendor engine needed (the agent is the runtime).

For vendors with a native skill/command system, thin **adapters** auto-trigger the skill
in that runtime — each a generated *pointer* to the neutral skill, never a copy:
`.claude/skills/<name>/SKILL.md`, `.gemini/commands/<name>.toml`, `.cursor/rules/<name>.mdc`.
Adapter dirs are personal/per-machine (gitignored) and are **regenerated locally** on
enable/migrate, and **on demand** — say **"sync skill adapters"** to (re)create them after a
clone/pull, since they're gitignored and don't travel. Only the neutral `agent-skills/` is shared.

**Author** skills in `agent-skills/<name>/SKILL.md` — never in a vendor folder; a skill
authored natively in a vendor folder is **adopted** (promoted) into `agent-skills/`. Skill
work is on-demand, not per-session: authoring, the adapter recipe, **sync**, **adopt**, and
the heavyweight **sanity check** all live in **`SKILLS.md`** (read on demand). See also
`docs/DESIGN-skills-layer.md`.

**Tool-provided (system) skills** — the built-ins agent-memory installs into every repo
(`memory-lint`, `second-opinion`, `apply-critique`) — carry **`provenance: agent-memory-builtin`** in
their frontmatter (the field is optional and absent on skills you author). They are tool-managed copies,
overwritten on upgrade: don't edit one in place — **fork** it under a new name, or **upstream** a
genuine fix to the agent-memory project for back-port + validation (`SKILLS.md` → "Tool-provided
(system) skills").

---

## review-scratch/  (fresh-context review — personal, gitignored)

Working files for the optional **`second-opinion`** / **`apply-critique`** skills (the
fresh-context review pair). When the human invokes `second-opinion`, it writes a compact
snapshot of the current task — **derived from `continuity.md` + recent session logs**, never
a parallel committed state file — to `review-scratch/snapshot-<UTC>.md`, for a clean-memory
reviewer (another vendor or a fresh session) to challenge.

```
review-scratch/        gitignored, per-machine, personal — NOT shared memory
  README.md            marks the folder personal; sharing a file is a conscious decision
  snapshot-<UTC>.md    a task snapshot for an external reviewer
  critique-<UTC>.md    the reviewer's returned critique (consumed by apply-critique)
```

The folder is **gitignored** (like the vendor adapter dirs): sharing a snapshot with another
AI is a trust-boundary event the human owns — `second-opinion` shows a security advisory and
waits for acknowledgment before exporting state. Critique is **advisory**: `apply-critique`
gates it with deterministic checks (build/tests, `memory-lint`) and a human decision. See
`docs/DESIGN-fresh-context-review.md`.

---

## .agent/version.md

Install manifest recording which agent-memory version this repo is on:
`version`, `enabled_with`, `last_upgraded`, `mode`. It gates the in-place upgrade
ladder — see the tool's `UPGRADE.md` (reached only via `ENABLE.md` Mode B).

---

## Bootstrap Files

**Minimal, parallel pointers to `AGENTS.md`** — one per vendor. Each says only: a project
one-liner, "read `AGENTS.md` first" (the hub — it carries the protocol *and* the read
order), and "identify as `<vendor>`". They differ only by vendor name, comment syntax
(`.md` vs the plain `.cursorrules` / `.windsurfrules`), and the `AGENTS.md` path
(`.github/copilot-instructions.md` uses `../AGENTS.md`).

`CLAUDE.md` and `GEMINI.md` carry the inline `{{PROJECT_NAME}}` + `{{PROJECT_ONELINE}}`
header (eager-load runtimes get immediate context); the dotfile rules stay plain. **The
read order lives only in `AGENTS.md`** — a pointer never duplicates it, so a change to
what agents read (e.g. adding `memory/vision.md`) touches one file, not ten.
