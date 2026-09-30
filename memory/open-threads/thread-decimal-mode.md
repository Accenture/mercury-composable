- [ ] **Exact decimal arithmetic: a `DECIMAL:` statement for `graph.math` and an explicit plugin family (RFC-0001, Promoted → ADR-0025; the
  thread id stays `decimal-mode`).** A field installation needs money and rate math that is exact, reproducible across runs
  and engines, and readable and certifiable on the graph. RFC-0001 (`docs/arch-decisions/RFC.md`, merged in PR #470 as
  `dd889bd5`, revised in PR #471) proposes a `DECIMAL:` statement, the high-precision `COMPUTE:`: a parallel `BigDecimal`
  evaluator; specified result scales; a 34-digit HALF_EVEN division context; explicit three-argument rounding; inexact
  functions refused at run time; a `Double` operand converted through its shortest decimal text (lenient, declared in the
  guide); the result stored as a canonical string; an explicit `f:decimal*` plugin family (the `f:add` family unchanged); a
  numeric-string comparison rule; shared conformance vectors in every executing engine; and a side fix (dialect `round(-2.5)`
  gives -2, `f:round(-2.5)` gives -3). **It deliberately reopens [[graph-math-typed-arithmetic]]'s ruling** that
  exact-decimal money belongs in a `graph.task` function and `BigDecimal` is never added to the dialect (the RFC's option (c)
  is that ruling); acceptance would supersede that part of the fact, so the decision is Eric's at the Design gate. Still open
  in the RFC: the Rust decimal type (`bigdecimal` is the candidate), the exponent bound, and which release carries each step;
  the comparison rule's reach into IF, CONDITION and `COMPUTE:` is implemented as proposed, exact with no precision guard,
  and stays a READ item to confirm at review. **Rulings (Eric, 2026-09-30):** (1) amend the RFC before implementing; (2) the
  first delivery is Java `graph.math` only, then the `f:decimal*` plugins, then the Rust twin, each its own PR against
  shared vectors; (3) the RFC's proposed defaults stand (fixed decimal128 division context, computed scale kept, a zero of
  any scale written `"0"`, scope `graph.math` only); (4) promotion to an ADR comes only after a successful implementation,
  review and tests, so the RFC stays Open until then; (5) decimals are canonical strings at rest, because `graph.suspend`
  saves the state machine and `graph.resume` restores it, so a `BigDecimal` would come back a string; (6) numeric strings
  compare as numbers in `== != < <= > >=`, decided in the evaluator because every value is rendered into the statement text
  before parsing; (7) there is no graph-level mode: `DECIMAL:` is the high-precision `COMPUTE:`, chosen over type-directed
  arithmetic and over keeping the switch, since the switch carried the result type and the guarantee that conversion cannot.
  (8) a `Double` is accepted through its shortest decimal text at its minimal scale, which makes it lenient; Eric chose to
  support that and to declare in the documentation that a double already computed in floating point is only as exact as that
  computation, so sending money as strings is a conscious user decision. **State (2026-09-30):** Java `graph.math` DELIVERED — PR #471 (squash `3b4cad82`, the `DECIMAL:` statement, the decimal
  evaluator, the numeric-string comparison rule, 151 vectors, docs, three claims) and PR #472 (the `round` half-up alignment);
  both UNRELEASED, in the CHANGELOG `## Unreleased`. RFC-0001 promoted by Eric to ADR-0025 (PR #473), the decimal half of
  [[graph-math-typed-arithmetic]] superseded by [[decimal-statement-exact-arithmetic]]. **Remaining:** (1) the `f:decimal*`
  plugin family — PR #475 open (camelCase names, Eric: `decimalAdd`, `decimalSubtract`, `decimalMultiply`, `decimalDiv`, `decimalMod`, `decimalRound`, `decimalCompare`; `decimal-plugin-vectors.json`); (2) the Rust twin — `DECIMAL:`, the comparison rule and `round`
  half-up (`bigdecimal` the candidate; verify rounding modes and result scales against the vectors); (3) the `f:round`
  plugin test pinning `-2.5` → `-3`; (4) the canonical string form for `mercury-go`, `mercury-python`, `mercury-nodejs`;
  (5) which release carries each step. Close this thread when (1) and (2) land. Applies [[conv-proposals-not-in-adr-ledger]].
  → proposal: RFC-0001
  → serves: vision-mercury-composable
  <!-- id: decimal-mode | created: 2026-09-30 | last_used: 2026-09-30 | uses: 1 | tier: working -->
