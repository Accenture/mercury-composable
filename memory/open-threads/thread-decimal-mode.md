- [x] **Exact decimal arithmetic: a `DECIMAL:` statement for `graph.math` and an explicit plugin family (RFC-0001, Promoted → ADR-0025; thread id `decimal-mode`).**
  Delivered on both engines, UNRELEASED. Java: the statement and the numeric-string comparison rule (#471, `3b4cad82`), `round` half-up (#472), the
  ADR (#473), the `f:decimal*` plugins (#475), the Sonar cleanups (#474, #478), the syntax-guide table (#479), the `f:round` agreement test (#477). Rust
  (`mercury`): one shared core `event_script::decimal` (Eric: where Java keeps two implementations) — #335 plugins (Increment 144) and #336 the
  statement (Increment 145), both merged; 151 + 68 shared vectors pass on both engines. **Lessons:** the `bigdecimal` crate's `/` is neither
  exact-if-terminating nor decimal128 (division written on `BigInt`); a new plugin family touches BOTH plugin tables; a new fixture graph bumps
  the exact-count pin in the Rust `tests/compiler.rs`. Follow-ups moved to [[decimal-ports-and-release]]. Applies [[conv-proposals-not-in-adr-ledger]].
  → proposal: RFC-0001
  → serves: vision-mercury-composable
  <!-- id: decimal-mode | created: 2026-09-30 | last_used: 2026-09-30 | uses: 3 | tier: archive-candidate -->
