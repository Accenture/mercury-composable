# Proposed architecture decisions

A working register of architecture decisions **under consideration**. Nothing here is
decided, and nothing here is binding.

This file exists because [`ADR.md`](ADR.md) is an **immutable journey of decisions**. A
proposal is not a decision: it may be reshaped, merged into another, or withdrawn
entirely, and a withdrawn proposal is a non-decision that should never have entered the
ledger in the first place. Writing proposals here keeps the ADR record honest — every
entry in `ADR.md` is something the project actually decided.

## How it works

1. **Raise** a proposal here with the next free `P-NNNN` id. Discuss and revise it in
   place — a proposal is expected to change.
2. **Accept** it: the maintainer approves, and it is written into `ADR.md` as a new
   `ADR-NNNN` with `Status: Accepted`. Record the promotion in *Resolved* below and
   remove the live entry.
3. **Withdraw** it: record it in *Resolved* with the reason. It never reaches `ADR.md`.

`P-NNNN` and `ADR-NNNN` are **separate sequences** — a proposal does not reserve an ADR
number, because proposals and decisions do not map one-to-one. The `P-NNNN` id is a
handle for the conversation; the `ADR-NNNN` number is assigned on acceptance.

An ADR that is superseded or deprecated later is handled in `ADR.md` as it always was —
marked in place, never deleted. This file is only for the *pre-decision* stage.

## Entry template

```markdown
## P-NNNN — <one-line statement of the decision being proposed>
**Raised:** YYYY-MM-DD · **Serves:** <vision or blueprint id> · **Affects:** <ADR-NNNN, …>

**Proposal.** What would change, stated as the decision it would become.

**Why now.** The problem or evidence that raised it.

**Alternatives.** What else was considered, and why it is not preferred.

**Open questions.** What must be answered before this can be accepted.
```

## Open proposals

*(none)*

## Resolved

| id | outcome | note |
| --- | --- | --- |
| — | — | *(none yet — this register starts 2026-09-18)* |
