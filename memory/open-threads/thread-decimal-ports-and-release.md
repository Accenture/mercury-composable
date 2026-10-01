- [ ] **Decimal release: which release carries the decimal work.** The Java and Rust engines carry the DECIMAL statement, the `f:decimal*` plugins, the
  numeric-string comparison rule and `round` half-up, all UNRELEASED (CHANGELOG `## Unreleased` in both repos, with READ notes for the comparison and `round`
  changes), plus the DECIMAL money-loop guide (composable #480, mercury #337). **No language-pack work:** `mercury-python` and `mercury-nodejs` are minimalist
  LLM extensions that serve functions to a Java or Rust application over Event-over-HTTP as if local — no event script, no minigraph — so they never evaluate a
  statement or a plugin and need no engine change (Eric, 2026-10-01; RFC-0001/0002 and ADR-0025 corrected); `mercury-go` is the agent-memory tool, not a
  language pack. One optional docs item: a pack function that returns money should return a plain-notation string (Python `format(d, 'f')`, since
  `str(Decimal('1E+3'))` is `1E+3`, which `DECIMAL` rejects as not a canonical number). **Next:** Eric decides the release; every port adopts the Java
  number on catch-up ([[conv-ports-adopt-java-release-number]]), so the Rust crates follow the Java release that carries this. Release rhythm
  [[eric-release-rhythm]]: prepare, never merge or tag without Eric's go. Relates [[decimal-statement-exact-arithmetic]].
  → serves: vision-mercury-composable
  <!-- id: decimal-ports-and-release | created: 2026-10-01 | last_used: 2026-10-01 | uses: 2 | tier: working -->
