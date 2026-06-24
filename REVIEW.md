# REVIEW — The Memory Review Ritual

> When and how to recompute usage metadata, reshuffle tiers, and keep
> `memory/continuity.md` lean. Applies the rules in `DECAY.md`.
>
> Like `DECAY.md`, this doc is generic and **ships into every enabled repo**
> (installed at the repo root by `ENABLE.md`): the ritual runs *inside* the repo
> as part of the normal session routine, so the agent needs it locally.

---

## When it runs

Three triggers:
1. **Cadence** — when `sessions_since_last_review ≥ review_every` (from
   `memory/decay-policy.md`). Checked during the post-session update.
2. **On command** — the user says *"review memory"* / *"compact memory"*.
3. **Size** — when `memory/continuity.md` exceeds `continuity_max_lines`.

Within a review, one more cadence is checked — **invariant verification**: when
`sessions_since_last_invariant_check ≥ verify_invariants_every`, the review prompts a
human to re-confirm the never-decay facts (routine step 6). It rides on the review, so
it never fires more often than reviews do.

`last_review` and `last_invariant_check` are tracked in `continuity.md` Project State
(each a `YYYY-MM-DD` plus the session file it last ran through).

## Inputs

- `memory/continuity.md` — facts + metadata
- `memory/decay-policy.md` — windows + triggers
- `memory/sessions/` — the event log; read each `## Memory References`
- `memory/archive/` — cold storage + `INDEX.md`

---

## The routine (incremental — the normal path)

1. **Gather the window.** List session files after `last_review`. Read each one's
   `## Memory References`.
2. **Apply events.** For every id named:
   - `Referenced` / `Created`: increment `uses`; set `last_used` to the latest
     session date that names the id.
   - `Reactivated`: if the id currently lives in the archive, move it back into
     `continuity.md` as `active`, then apply the Referenced bump.
   - `Superseded: <old> → <new>` (or `<old> (invalidated)`): confirm the old fact is
     marked `tier: superseded` + `superseded-by: <new>` (the agent marks it at write
     time — `DECAY.md` §9; set it here if missing) and the successor carries
     `supersedes: <old>`.
3. **Re-tier every fact.** For each fact in `continuity.md`, compute
   `sessions_since_last_used` (count files — `DECAY.md` §4) and apply the
   `DECAY.md` §5 rules in order. Record each tier change.
4. **Archive.** Facts that resolve to `archived` (faded) **or** `superseded` (false):
   - append the fact *with its metadata comment* to `memory/archive/<YYYY>-Q<n>.md`
     under a dated heading, noting the reason — `faded` or `superseded by <new-id>`,
   - add/refresh its line in `memory/archive/INDEX.md`
     (`id — one-line — <reason> — <quarter file>`),
   - remove it from `continuity.md`.
   Superseded facts archive **promptly** — no `archive_window` wait, since they are
   false, not merely stale — and carry their `superseded-by` link into the archive.
5. **Sweep completed threads.** `- [x]` Open Threads whose completion is older than
   `archive_window` sessions move to the archive the same way (usually the biggest
   lean-up). Keep recently-completed threads for context.
6. **Verify archival (required — guards against a miscounted `sessions_since_last_used`).**
   Archival is the costliest error, and "sessions since last used" is the easiest count to get wrong.
   A *"use"* is an id under a session's `## Memory References` (§2 / `DECAY.md` §2) — **not** a passing
   mention in prose. Verify against that definition:
   - **Preferred — run the `memory-lint` skill** (`agent-skills/memory-lint/`; Python *or* Node,
     whichever the machine has). It recomputes `sessions_since_last_used` from `## Memory References`
     **only**, exits non-zero if any archived-as-faded fact was actually referenced within
     `archive_window` (⇒ reactivate it), and confirms **no id lives in both `continuity.md` and the
     archive**. The script counts, so it is immune to the prose trap below. (No runtime? `SKILL.md`
     says install Python or Node — don't hand-count if you can avoid it.)
   - **By hand (fallback):** for **each** fact you archived as *faded*, grep the last `archive_window`
     session files for its id — **but only count a hit that sits inside a `## Memory References`
     block.** A hit *outside* it — e.g. a prior **review summary** (`## Memory Review`) that names the
     id while recording its decay status, or a `## What happened` mention — is **not** a use; ignore
     it. *(A raw full-text grep that counts such mentions creates an **archival livelock**: every
     review that defers a fact re-names it, so the guard never clears — the `ot-review-step6-prose`
     bug, same class as the v4.10.1 prose-vs-heading false positive.)* If a genuine `## Memory
     References` hit appears, your count was wrong — do **not** archive it (it is still `active` /
     `archive-candidate`); move it back into `continuity.md`.

   Either way, then confirm **no id lives in both `continuity.md` and the archive** (a fact exists in
   exactly one place). Record the result in the summary. (Superseded facts are exempt — they archive
   on truth-state, not recency.)
7. **Verify invariants (cadence).** If `sessions_since_last_invariant_check ≥
   verify_invariants_every` (or `last_invariant_check` is unset and that many session
   files exist), raise **one** Open Thread listing every never-decay fact —
   `tier: core`, everything under `## Architectural Invariants`, **and the Vision
   (`memory/vision.md`)** — for a human to re-confirm:
   `- [ ] Re-verify invariants (due): confirm <id>, <id>, … and the Vision still hold, or supersede any that don't (DECAY.md §9)`.
   The review **never auto-invalidates** an invariant — it only prompts; the human
   confirms (checks the thread off) or supersedes the false ones (§9). Then set
   `last_invariant_check` to today + the latest session file. (Never-decay ≠
   never-checked.) If not due, skip this step.
8. **Stamp.** Set `last_review` to today + the latest session file name.
9. **Summarise.** Write a `## Memory Review` block into *this* session's log.

**Contradiction backstop.** The review reads every fact anyway, so give them a quick
contradiction scan — the write-time check (`DECAY.md` §10) may have missed one, or two
facts may have drifted into conflict over time. Surface any conflict as a
`- [ ] Contradiction: <fact> conflicts with <id> — resolve (supersede one, or reconcile)`
Open Thread; never silently reconcile or pick a winner. Extend the same scan **up the
altitudes** (VBDI, `DECAY.md` §12): flag any Implementation / Design / Blueprint item that
no longer serves the one above it — `- [ ] Drift: <item> doesn't serve <id>`.

**Smoke test.** A review is also a natural time to run `memory/smoke-test.md` — a quick
manual check that memory still answers the orientation questions a newcomer would ask.

## Full rebuild (the ground-truth path)

Because metadata is *derived*, you can discard stored `uses`/`last_used`/`tier`
and recompute everything from scratch by scanning **all** session logs'
`## Memory References`. Use this to repair drift, after heavy manual edits, or if
reviews were skipped for a long stretch. The result is deterministic and
reproducible by any agent. The same scan repairs each fact's `origin` — the earliest
session whose `## Memory References` names the id under `Created` (`DECAY.md` §11).

## Reactivation

When an archived id is named in a session (`Referenced`/`Reactivated`):
- move the fact from its `archive/<quarter>.md` back into `continuity.md`,
- set `tier: active`, refresh `last_used`, increment `uses`,
- remove or annotate its `archive/INDEX.md` line,
- note it in the review summary.

This two-way movement is what keeps the system smart rather than merely lossy.
**Superseded facts are the exception** — they are terminal (`DECAY.md` §9) and are
*not* reactivated by a reference; only a human can reverse a supersession by hand.

---

## Review summary format

```markdown
## Memory Review (2026-06-20, through 2026-06-20-141503)
- Reactivated:   1  (drizzle-over-prisma — referenced today after 9 dormant sessions)
- Superseded:    1  (rest-versioning-v1 → rest-versioning-v2; archived flagged superseded)
- Archived:      3  facts → memory/archive/2026-Q2.md (faded)
- Swept threads: 4  completed Open Threads → archive
- Archive-verify: pass (no archived id appears in the last archive_window sessions; no id in both places)
- Tier changes:  6  (2 working→active, 1 active→archive-candidate, 3 →archived)
- Invariants:    not due (next re-verify in 6 sessions)   # or: "prompted — 2 invariants up for re-confirmation"
- Promoted core: 0  (auto-core off; core is human-set)
```

## Safety

- Never delete a fact — archiving is a *move*, not a removal.
- Never overwrite a hand-set `tier:` (especially `core`) or a hand-set `id`.
- Never edit past session logs — they are the immutable ledger this ritual reads.
- Stay within the repo's `memory/` and `archive/`; never touch `~/`, `~/.claude/`,
  Application Support, AppData, or system paths.
