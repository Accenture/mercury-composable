- [x] **The decimal-places limit on both engines (ADR-0034): the Rust twin and the bundle and help mirror (closed
  2026-11-17).** Outcome: Java #637 (squash `84fec92c9`) and the Rust twin mercury #412 (Increment 202, merge
  `f8bdacae`) both merged the same day; `graph.math` on both engines rounds a `DECIMAL` result HALF_UP to the node's
  `precision`, else `big.decimal.precision` (default 6), proven with one fixture copied verbatim; the Playground bundle
  and 44 help pages byte-identical on both. Lesson: a limit on a result goes at the statement boundary, so the
  arithmetic's exactness and the shared vectors stay untouched ([[decimal-result-precision-limit]]).
  → origin: 2026-11-17-055029.md
  <!-- id: decimal-precision-rust-twin | created: 2026-11-17 | last_used: 2026-11-17 | uses: 1 | tier: working | origin: 2026-11-17-055029 -->
