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
- review_every:        10   # run a review this many sessions after the last one
- continuity_max_lines: 300 # ...or when continuity.md grows past this many lines

## Invariant verification
- verify_invariants_every: 20  # sessions between human re-checks of core / invariants

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
