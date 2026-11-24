- [ ] **The Rust engine gains `f:addMonths`, `f:monthsBetween`, `f:addYears` and `f:yearsBetween`** (the twin of branch
  `feat/date-month-year-plugins`, issue #664; Java only so far).
  - **Scope:** four `#[simple_plugin]` functions in `crates/event-script/src/plugins_date.rs`; `date-plugin-vectors.json`
    (175 vectors) and the graph fixture `unit-test-date-arithmetic-2` copied byte-identical, with `graphs.yaml`, the twin
    of the three `GraphTaskTest` methods in `graph_runtime.rs` and the fixture count in `compiler.rs`; the vector runner's
    codes `bad-months` and `bad-years`, and `bad-days` matched on "The days of"; the result types in
    `crates/knowledge-graph/src/contract.rs`; the six guide files and the CHANGELOG; `npm run release:rust` from this
    repository's webapp for the help page and the bundle (`index-wWr4LR2P`); then replace "in the Java engine for now" in
    `skills-reference.md` and the CHANGELOG entry's last sentence here.
  - **Rules to match:** the day plugins' dates, whole-number count and refusals; the day of the month kept, a day the
    resulting month lacks becoming that month's last day; the whole months or years completed, absolute, as an integer;
    a count beyond 119,999 months or 9,999 years refused before any arithmetic.
  - **Measured before the Java change (jiff 1.0.1, the port's date library):** `checked_add` of a month or year span and
    `until` with the largest unit month or year give `java.time`'s answer on every row of the issue's table, and the month
    count equals the JDK's formula on all 267,289 pairs of dates from 2027-11-01 to 2029-03-31, both orders. jiff holds a
    year below 0000, so the writable-range check refuses it (vectors `add-months-lands-before-min`,
    `add-years-lands-before-min`).
  - origin: 2026-11-24-033752
  <!-- id: date-month-year-plugins-rust-twin | created: 2026-11-24 | last_used: 2026-11-24 | uses: 1 | tier: working | origin: 2026-11-24-033752 -->
