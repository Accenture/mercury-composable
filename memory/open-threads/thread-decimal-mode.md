- [ ] **Decimal mode: exact decimal arithmetic for `graph.math` and the arithmetic plugins (RFC-0001, Open).** A field
  installation needs money and rate math that is exact, reproducible across runs and engines, and readable and certifiable on the
  graph. RFC-0001 (`docs/arch-decisions/RFC.md`, merged in PR #470 as `dd889bd5`) proposes an opt-in decimal mode: `numeric: decimal` on
  the graph root plus `graph.math.numeric`; `BigDecimal` values with specified result scales; a 34-digit HALF_EVEN division
  context; explicit rounding modes; inexact functions refused at run time, not gated at compile time; every high-precision number
  exported and imported as a string; an explicit `f:decimal.*` plugin family (the `f:add` family unchanged); shared conformance
  vectors in every executing engine; and a mode-independent side fix (dialect `round(-2.5)` gives -2, `f:round(-2.5)` gives -3).
  **It deliberately reopens [[graph-math-typed-arithmetic]]'s ruling** that exact-decimal money belongs in a `graph.task` function
  and `BigDecimal` is never added to the dialect (the RFC's option (c) is that ruling); acceptance would supersede that part of
  the fact, so the decision is Eric's at the Design gate. Open questions in the RFC: fixed or configurable division context, the
  Rust decimal type, trailing zeros, strict-by-default, mapper and `f:lookup` scope, which engines and release carry it.
  **State:** raised 2026-09-30, no code; waiting for Eric (promote to an ADR, reshape, park or withdraw). **Next:** on Promoted,
  write the ADR, open a `(blueprint)` gap and plan the Java/Rust lock-step; on Withdrawn, record the reason in the RFC entry and
  close this thread. The `round` side fix can ship on its own if Eric wants it sooner. Applies [[conv-proposals-not-in-adr-ledger]].
  → proposal: RFC-0001
  → serves: vision-mercury-composable
  <!-- id: decimal-mode | created: 2026-09-30 | last_used: 2026-09-30 | uses: 1 | tier: working -->
