- [ ] **Decimal follow-ups: the canonical string form for the go, python and node packs, and which release carries the decimal work.** The Java and Rust
  engines carry the DECIMAL statement, the `f:decimal*` plugins, the numeric-string comparison rule and `round` half-up, all UNRELEASED (CHANGELOG
  `## Unreleased` in both repos, with READ notes for the comparison and `round` changes). RFC-0001 left two items open: the `mercury-python` and `mercury-nodejs` language packs need at least the canonical decimal string form (`mercury-go` is the agent-memory tool, not a language pack — Eric, 2026-10-01) (plain notation, the computed scale kept, a zero of any scale `"0"`) so a result
  from either engine round-trips through them as a string; and the release number for each step — every port adopts the Java number on catch-up
  ([[conv-ports-adopt-java-release-number]]), so the Rust crates follow the Java release that carries this. **Next:** Eric decides whether the packs
  need more than the string form; then the release. Relates [[decimal-statement-exact-arithmetic]]. **2026-10-01:** an external review's suggestions were triaged (Eric): the DECIMAL guide fixes shipped as docs PRs (composable #480, mercury #337), RFC-0003 (the `for_each` index) is Parked and RFC-0004 (an `f:decimalBracket` plugin) is Withdrawn — both in `docs/arch-decisions/RFC.md`.
  → serves: vision-mercury-composable
  <!-- id: decimal-ports-and-release | created: 2026-10-01 | last_used: 2026-10-01 | uses: 1 | tier: working -->
