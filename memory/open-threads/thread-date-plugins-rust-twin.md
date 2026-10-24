- [ ] **The Rust engine gains `f:addDays` and `f:daysBetween`** (the twin of PR #591, squash `ca2344ae`, merged
  2026-10-24; Java only so far).
  - **Scope:** two `#[simple_plugin]` functions in `crates/event-script/src/plugins.rs`; `date-plugin-vectors.json` copied
    byte-identical with a vectors test beside `range_lookup_vectors.rs`; the graph fixture `unit-test-date-arithmetic-1`;
    the result types in `crates/knowledge-graph/src/contract.rs`; `npm run release:rust` for the help page and bundle; then
    drop "Java engine only for now" from `skills-reference.md` and the CHANGELOG entry's Java-only sentence.
  - **Rules to match:** exactly `YYYY-MM-DD`, a real calendar day, a result within 0000-01-01 to 9999-12-31; the days a
    whole number (an integer, numeric text or a whole float), negative for an earlier date; `daysBetween` the absolute
    count as an integer; a null argument refused.
  - **Progress (2026-10-24):** built on the Rust branch `feat/date-plugins` as Increment 189 (mercury #396), every item
    above done and green; this repository's notes replaced on `docs/date-plugins-both-engines`. Closes when the Rust
    branch merges.
  - origin: 2026-10-24-201909
  <!-- id: date-plugins-rust-twin | created: 2026-10-24 | last_used: 2026-10-24 | uses: 1 | tier: working | origin: 2026-10-24-201909 -->
