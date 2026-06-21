# Agent Instructions

This repository uses the **agent-memory** shared memory system.
It is configured for AI-assisted development with any major agent runtime.

## Two Memory Layers

This repo's `memory/` holds **project state, shared across all agents and committed
to git.** It is separate from any **personal, user-scoped memory your runtime keeps
outside the repo** (e.g. Claude Code's `~/.claude/`, which holds your individual
preferences). Project facts, decisions, and session logs go in this repo's
`memory/`; personal preferences stay in your runtime's own store.

## Before Every Session

Read these files before responding to anything:

1. `memory/instructions.md` — project context, rules, and conventions
2. `memory/continuity.md`   — current state, open threads, key decisions (+ Blueprint gaps)
3. `memory/vision.md`       — the target the work serves (the VBDI north star)
4. `memory/sessions/`       — scan the most recent 2–3 session logs

If a topic seems unfamiliar, grep `memory/archive/INDEX.md` (and follow a fact's
`origin` to its session) before saying you have no context — retrieval here is lexical
+ indexed by design (`DECAY.md` §11); facts fade to the archive but are never deleted.

## The cognitive loop (VBDI)

This repo runs a forward loop on top of the memory layer (`DECAY.md` §12):
**Current State (`continuity.md`) → Vision (`memory/vision.md`) → Blueprint (gap) →
Design → Implementation → Feedback (review) → repeat.** When you propose significant
work, tie it to a Blueprint gap (a `(blueprint)` Open Thread that `serves:` the Vision)
and to the Design it realizes — so intent is traceable and drift is detectable. Each
altitude transition (confirming the Vision, opening or closing a gap) is a **human
gate**: propose, then let the human approve. Never fabricate the Vision.

The Design altitude *may* keep an **optional** Architecture Decision Record log,
`docs/ADR.md` — a human-facing governance ledger of durable architecture decisions
(see `.agent/schema.md`). It is read **on demand**, **not** part of the per-session read;
any `(ADR-NNNN)` tag on an invariant is a human pointer, not a cue to open it.

## Skills

If an `agent-skills/` directory exists, it holds the project's **capabilities** — committed,
vendor-neutral `agent-skills/<name>/SKILL.md` files. **This is the runtime:** when a task
matches a skill's `description`, read and follow that `SKILL.md` (and any scripts it
references). The agent is the runtime — works on any vendor, no engine.

Per-vendor adapters (`.claude/skills/`, `.gemini/commands/`, `.cursor/rules/`, `.kiro/skills/`)
are thin, gitignored, regenerated pointers — **never commit them** (only `agent-skills/` is
shared); the source of truth is always `agent-skills/<name>/SKILL.md`.

**Authoring, syncing, adopting, sanity-checking, or editing a tool-provided skill?** See **`SKILLS.md`**
(read on demand — it is *not* part of this per-session read). Skill work is a deliberate, occasional
action, never part of the session ritual. A skill whose frontmatter says `provenance: agent-memory-builtin`
is **tool-managed** (overwritten on upgrade) — don't edit it in place; fork it under a new name, or
upstream a genuine fix to the agent-memory project (`SKILLS.md` → "Tool-provided (system) skills").

## During the Session

- Treat `memory/continuity.md` as your working memory.
- Reference prior decisions before suggesting changes that might contradict them.
- Note any new facts, preferences, or decisions for post-session write.
- Track which fact **ids** you rely on, create, or pull back from the archive — you
  will list them in the session log's `## Memory References`. Do **not** edit fact
  metadata mid-session; the review ritual does the counting.

## After Every Session

A "session" is **one log-write** — the work since the last log, not necessarily a
whole conversation. A long, multi-task conversation may produce several logs; that's
expected (the decay math counts log files — `DECAY.md` §4).

1. **Create** `memory/sessions/YYYY-MM-DD-HHMMSS.md` using the UTC timestamp at
   **persist time** (when you write the file). Use `date -u +%Y-%m-%d-%H%M%S` or
   equivalent; omit colons for cross-platform compatibility. Title line:
   `# Session (endZ)` — the persist-time UTC stamp (full ISO 8601 ms) is required; a
   start time is optional/best-effort, so don't fabricate one. Never append to
   another contributor's session file.
   Include a `## Memory References` section listing the fact ids you referenced,
   created (born `tier: working`), or reactivated. This is the event log the review
   ritual reads — see `DECAY.md`.
2. **Update** `memory/continuity.md`:
   - Set `last_session` to today's date and your agent name.
   - Mark completed Open Threads `- [x]` and **leave them in place** — the review
     sweeps them once older than `archive_window`; don't archive them by hand.
   - Add new Open Threads surfaced during the session.
   - **Before recording a new fact, check it against existing ones** (`DECAY.md` §10):
     if it clearly replaces one, supersede that one (see below); if it genuinely
     conflicts, raise a `- [ ] Contradiction: …` Open Thread rather than keeping both.
   - Give any new fact a kebab `id` + footer: set `id`, `created`, `tier: working`
     (or `core` for an Architectural Invariant), `origin: <this session's file>`, and
     seed `last_used: today | uses: 1`. Don't hand-edit `uses`/`last_used`/`tier`
     afterward — the review owns them.
   - Update the substance of any fact that changed (not its usage metadata).
   - **Reversed a decision / a fact became false?** Add the successor (born
     `tier: working`, `supersedes: <old>`), mark the old fact `tier: superseded` +
     `superseded-by: <new>` (omit the link for pure invalidation), and record
     `Superseded: <old> → <new>` in `## Memory References`. This is a truth-state edit
     you own; the review archives it flagged "superseded" (`DECAY.md` §9).
3. **Review cadence.** If `sessions_since_last_review ≥ review_every`
   (`memory/decay-policy.md`), or `continuity.md` has grown past
   `continuity_max_lines`, run the review ritual now — see `REVIEW.md`. (Also run it
   on demand if the user says "review memory".)
4. Remind the user: `git add memory/ && git commit -m "session YYYY-MM-DD [agent]"`.
   **Commits are deliberate and human-initiated.** When you commit at the human's direction,
   **identify yourself** the same way you do in session logs — e.g. a `Co-Authored-By: <your agent
   name>` trailer — so authorship is traceable across vendors. (If your runtime already adds one,
   nothing to do.)

**After-session checklist** (the ritual is convention — run it each time):
- [ ] session log written (persist-time filename + `## Memory References`)
- [ ] `continuity.md`: `last_session` set, threads checked, new facts have footers
- [ ] review run if cadence/size triggered (`REVIEW.md`)
- [ ] reminded the user to commit `memory/` (deliberate, human-initiated, with a self-identifying co-author trailer)

> **Lightweight mode — key the write to whether a *tracked* file changed (the *objective* test is the
> git diff, not any filesystem write — and never a "trivial" judgment; both AI and human misjudge "trivial").**
> - **Read-only session** (no tracked file changed — orientation, Q&A, exploration, **or a run whose
>   only writes are gitignored, regenerated artifacts**: `sync skill adapters`, `review-scratch/`
>   snapshots, the compiled lint artifact): **no session log** — nothing entered the repo, nothing to
>   commit, no event to record.
> - **A tracked file changed but produced no memory-relevant event** (no new/changed fact, no decision
>   worth recording, no Open Thread touched, no project-state change — e.g. a one-line fix, a typo):
>   write a **one-line "lite" session log** (persist-time filename + `**Agent:**` + a *lightweight*-marked
>   summary + `## Memory References` → `(none)`) and skip the rest (full template, fact-footers,
>   continuity edits; `last_session` is derivable from the newest session file). **Don't skip the log
>   just because it felt "trivial"** — a misjudged change that actually mattered must still be logged.
> - **A memory-relevant event** (fact / decision / Open Thread / project-state change, or anything
>   touching Vision / Blueprint / invariant / supersession): the **full** ritual.
> The ledger stays continuous for anything that touched a *tracked* file; the review treats a lite log
> as a normal reference-free session, so usage is unaffected.

> Optional reinforcement: wire a lightweight Stop or pre-commit hook in your runtime
> so this ritual is *prompted*, not merely documented. It stays optional — the
> protocol itself is no-code.

## Multi-Agent Continuity

Check `last_session` in `continuity.md` and note the agent name recorded there.
If it is **not your own agent family** (e.g. Claude, Gemini, Copilot, Cursor),
read that day's session log in full before proceeding — the memory files are the
shared ground truth across all agents.

## Memory File Locations

```
memory/
  instructions.md     ← project context + agent rules    (edit rarely)
  continuity.md       ← live project state               (update every session)
  decay-policy.md     ← evolving-memory windows/triggers (tune as needed)
  sessions/           ← dated session logs (event log)   (append; never edit past logs)
  archive/            ← faded facts + swept threads       (cold storage; never deleted)
    INDEX.md          ← greppable index of archived facts
agent-skills/               ← cross-vendor capabilities          (committed; vendor-neutral)
  <name>/SKILL.md     ← one skill: name + when-to-use + procedure (the source of truth)
.agent/
  schema.md           ← file format reference
  version.md          ← which agent-memory version this repo is on
DECAY.md              ← evolving-memory rules (metadata, tiers, deterministic decay)
REVIEW.md             ← the review ritual (when/how to recompute + archive)
SKILLS.md             ← skills reference: authoring, sync, adopt, sanity (read on demand)
```
