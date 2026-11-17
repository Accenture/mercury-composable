- [ ] **The decimal-places limit on both engines: the Rust twin of ADR-0034, then the bundle and help mirrored to the
  Rust repo (2026-11-17).** Java: `graph.math` rounds a `DECIMAL` result HALF_UP to the node's `precision`, else the
  application's `big.decimal.precision` (default 6); `GraphModelValidator` checks the node property at deploy;
  `DecimalEvaluator.evaluate(expression, maxScale)` applies it, `evaluate(expression)` stays unbounded for the vectors
  ([[decimal-result-precision-limit]]). The Rust twin (mercury, `crates/knowledge-graph`: `skills.rs` `decimal()`,
  `math/decimal_evaluator.rs`, `model_validator.rs`, its help mirror) reads the same two settings and rounds the
  statement's result the same way, verified against the Java fixture `unit-test-decimal-precision` (eleven results).
  The Playground bundle was rebuilt here Java-only (`npm run release`); once the twin lands, `npm run release:rust`
  deploys the bundle and the help mirror there ([[help-edit-needs-bundle-release]]). Java PR #637 squash-MERGED 2026-11-17 (`84fec92c9`); the Rust twin OPENED the same day as mercury #412 (Increment
  202, one commit `696cafaf`; 22 binaries, 113 tests; the bundle and 44 help pages deployed, byte-identical). Open: the
  merge of #412; Eric's call on padding (the limit is a cap - a fixed scale was the rejected alternative
  (b) of ADR-0034) if the field asks for it.
  → origin: 2026-11-17-055029.md
  <!-- id: decimal-precision-rust-twin | created: 2026-11-17 | last_used: 2026-11-17 | uses: 1 | tier: working | origin: 2026-11-17-055029 -->
