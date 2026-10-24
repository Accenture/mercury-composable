- [ ] **The Rust engine's companion endpoint reads the engine's refusal flag, as the Java engine's does since PR #586.** On
  the Java engine, `GraphCommandService` sends every refusal with an error status through one `refuse()` helper. An
  `execute` that relays a skill's error keeps the skill's status, and the traveler's abort already carried one.
  `PostCompanionCommandSync` reports the first flagged line as the error, else the dispatch RPC's own failure, else a
  truncated capture, and the word list is gone. In the Rust port, `crates/knowledge-graph/src/commands.rs` prints the same
  refusals without a flag, and `rest.rs` `is_error_line` keeps the word list, so the same misses and false positives are
  expected. That is not yet checked against the Rust engine.
  **To port:** the flag; the two refusal lines Java gained (`import node` from a missing file, `session` with an unknown
  argument); the RPC-failure branch; the tests (`CompanionSyncTest`: every help page `ok: true`, every refusal `ok: false`).
  Then drop the "(the Java engine ...)" qualifier from `ai-agent-guide.md`, `minigraph-commands.json` and
  `command-reference.md`. Applies [[conv-ports-adopt-java-release-number]] (the Java repo is the reference).
  <!-- id: companion-refusal-flag-rust-twin | created: 2026-10-24 | last_used: 2026-10-24 | uses: 1 | tier: working | origin: 2026-10-24-023905 -->
