- [ ] **Exact decimal arithmetic: a `DECIMAL:` statement for `graph.math` and an explicit plugin family (RFC-0001, Open; the
  thread id stays `decimal-mode`).** A field installation needs money and rate math that is exact, reproducible across runs
  and engines, and readable and certifiable on the graph. RFC-0001 (`docs/arch-decisions/RFC.md`, merged in PR #470 as
  `dd889bd5`, revised in PR #471) proposes a `DECIMAL:` statement, the high-precision `COMPUTE:`: a parallel `BigDecimal`
  evaluator; specified result scales; a 34-digit HALF_EVEN division context; explicit three-argument rounding; inexact
  functions refused at run time; a `Double` operand rejected by name; the result stored as a canonical string; an explicit
  `f:decimal.*` plugin family (the `f:add` family unchanged); a numeric-string comparison rule; shared conformance vectors in
  every executing engine; and a side fix (dialect `round(-2.5)` gives -2, `f:round(-2.5)` gives -3). **It deliberately
  reopens [[graph-math-typed-arithmetic]]'s ruling** that exact-decimal money belongs in a `graph.task` function and
  `BigDecimal` is never added to the dialect (the RFC's option (c) is that ruling); acceptance would supersede that part of
  the fact, so the decision is Eric's at the Design gate. Still open in the RFC: whether IF, CONDITION and `COMPUTE:` get the
  comparison rule (proposed yes, with a precision guard), the Rust decimal type (`bigdecimal` is the candidate), the exponent
  bound, and which release carries each step. **Rulings (Eric, 2026-09-30):** (1) amend the RFC before implementing; (2) the
  first delivery is Java `graph.math` only, then the `f:decimal.*` plugins, then the Rust twin, each its own PR against
  shared vectors; (3) the RFC's proposed defaults stand (fixed decimal128 division context, computed scale kept, a zero of
  any scale written `"0"`, lenient deferred, scope `graph.math` only); (4) promotion to an ADR comes only after a successful
  implementation, review and tests, so the RFC stays Open until then; (5) decimals are canonical strings at rest, because
  `graph.suspend` saves the state machine and `graph.resume` restores it, so a `BigDecimal` would come back a string; (6)
  numeric strings compare as numbers in `== != < <= > >=`, decided in the evaluator because every value is rendered into the
  statement text before parsing; and (7) there is no graph-level mode: `DECIMAL:` is the high-precision `COMPUTE:`, chosen
  over type-directed arithmetic and over keeping the switch, since the switch carried the result type and the guarantee that
  conversion cannot. **State:** the RFC and the Java implementation share one branch and one PR, `feature/decimal-mode` (PR
  #471, draft until implemented, reviewed and tested); no code yet beyond the RFC text, no ADR, no `(blueprint)` gap.
  **Next:** implement Java `graph.math` on that branch (the `DECIMAL:` tag in `GraphLambdaFunction` and its statement
  recognizer, `GraphMath`, the decimal evaluator, the comparison rule in both evaluators, docs, a claim, shared vectors),
  then review and tests, then Eric decides promotion; on Promoted, write the ADR, supersede the decimal part of
  [[graph-math-typed-arithmetic]], and open the plugin and Rust-twin work; on Withdrawn, record the reason in the RFC entry
  and close this thread. The `round` side fix can ship on its own if Eric wants it sooner. Applies
  [[conv-proposals-not-in-adr-ledger]].
  → proposal: RFC-0001
  → serves: vision-mercury-composable
  <!-- id: decimal-mode | created: 2026-09-30 | last_used: 2026-09-30 | uses: 1 | tier: working -->
