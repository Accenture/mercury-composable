- [ ] **(twin) The Rust Playground webapp has neither the one-third console default nor `describe skill` in the help panel.** The Java change merged as
  PR #493 (squash `1619a4f3`, 2026-10-02; branch `fix/playground-console-third-describe-skill-help`). In
  mercury, `crates/knowledge-graph/webapp` held the five touched files byte-identical to Java `main` before the change, and the Rust engine answers
  `describe skill {route}` with the same `help {route}` page (`handle_describe` in `commands.rs`), so the twin is a port of the four sources, their tests and the docs,
  then a Rust bundle release (`npm run release`; the Rust repo ignores source maps). The lesson to carry over: a saved drag beat the default sizes at mount, so the
  split is no longer persisted (the webapp fact `webapp-panel-split-not-persisted`). origin: 2026-10-02-031835.
  → serves: vision-mercury-composable
  **Update 2026-10-02:** resolved by the single-source deploy, not a port: mercury #347 removes the Rust copy and carries the Java bundle with both fixes (PR #496, [[playground-webapp-single-source]]); closes when it merges.
  <!-- id: playground-split-describe-skill-rust-twin | created: 2026-10-01 | last_used: 2026-10-01 | uses: 1 | tier: working | origin: 2026-10-02-031835 -->
