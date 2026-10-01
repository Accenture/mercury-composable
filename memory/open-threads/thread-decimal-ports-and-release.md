- [ ] **Decimal follow-ups: the canonical string form for the go, python and node packs, and which release carries the decimal work.** The Java and Rust
  engines carry the DECIMAL statement, the `f:decimal*` plugins, the numeric-string comparison rule and `round` half-up, all UNRELEASED (CHANGELOG
  `## Unreleased` in both repos, with READ notes for the comparison and `round` changes). RFC-0001 left two items open: `mercury-go`, `mercury-python`
  and `mercury-nodejs` need at least the canonical decimal string form (plain notation, the computed scale kept, a zero of any scale `"0"`) so a result
  from either engine round-trips through them as a string; and the release number for each step — every port adopts the Java number on catch-up
  ([[conv-ports-adopt-java-release-number]]), so the Rust crates follow the Java release that carries this. **Next:** Eric decides whether the packs
  need more than the string form; then the release. Relates [[decimal-statement-exact-arithmetic]].
  → serves: vision-mercury-composable
  <!-- id: decimal-ports-and-release | created: 2026-10-01 | last_used: 2026-10-01 | uses: 1 | tier: working -->
