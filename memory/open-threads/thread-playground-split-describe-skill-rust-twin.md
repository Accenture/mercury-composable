- [ ] **(twin) The Rust Playground webapp has neither the one-third console default nor `describe skill` in the help panel.** The Java change is on branch
  `fix/playground-console-third-describe-skill-help` (commit `110b4bbe`; the PR is opened by Eric, since the GitHub CLI's account here cannot create one). In
  mercury, `crates/knowledge-graph/webapp` held the five touched files byte-identical to Java `main` before the change, and the Rust engine answers
  `describe skill {route}` with the same `help {route}` page (`handle_describe` in `commands.rs`), so the twin is a port of the four sources, their tests and the docs,
  then a Rust bundle release (`npm run release`; the Rust repo ignores source maps). The lesson to carry over: a saved drag beat the default sizes at mount, so the
  split is no longer persisted (the webapp fact `webapp-panel-split-not-persisted`). origin: 2026-10-02-031835.
  → serves: vision-mercury-composable
  <!-- id: playground-split-describe-skill-rust-twin | created: 2026-10-01 | last_used: 2026-10-01 | uses: 1 | tier: working | origin: 2026-10-02-031835 -->
