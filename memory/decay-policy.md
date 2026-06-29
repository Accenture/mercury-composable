# Memory Policy — mercury-composable

> Tunable windows and triggers for the evolving-memory layer. All windows are in
> **sessions**, not days — they measure project activity, not wall-clock time, so a
> vacation doesn't fade your memory. Integers only: the agent *counts session
> files*, it never computes a score. The rules these feed live in `DECAY.md` /
> `REVIEW.md` at the repo root.

## Lifecycle windows (sessions)
- working_window:   3     # new facts stay "working" until re-referenced within this many sessions
- active_window:    8     # referenced within this many sessions → active
- archive_window:   20    # not referenced for more than this → archived

## Review triggers
- review_every:         10   # run a review this many sessions after the last one
- continuity_max_facts:  30  # ...or when continuity.md holds more than this many decaying facts/threads
                             #    (the PRIMARY lean signal — a count, immune to verbosity & session velocity)
- continuity_max_lines: 600  # ...or this many lines (a coarse backstop; raised from 300 in v4.24.0 — a mature,
                             #    actively-developed layer legitimately sits ~450–600 lines even when healthy)

## Invariant verification
- verify_invariants_every: 40  # sessions between human re-checks of core / invariants (raised from 20 in
                               # v4.24.0: anti-fatigue at burst velocity)

## Auto-core (default: off — core is human-set)
- enabled:          false
- core_min_uses:    12
- core_min_reviews: 5

## Never decays
- tier: core
- anything under "## Architectural Invariants"
- unchecked Open Threads ( - [ ] )

> Edit the integers to taste. Hand-setting a `tier:` in `continuity.md` always wins
> over these rules.
