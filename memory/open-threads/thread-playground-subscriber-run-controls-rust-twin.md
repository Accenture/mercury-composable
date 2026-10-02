- [ ] **(twin) The Rust Playground webapp still disables Instantiate and Run in a subscribed session.** The Java fix merged as PR #495
  (squash `75ce191f`, 2026-10-02): `useGraphRunWorkflow` no longer gates on `isPrimary` and resets the run only
  when the session subscribes or unsubscribes, `Playground.tsx` passes `sessionCollaboration.isPrimary`, two tests replace the gate test, and the webapp README
  and technical documentation drop the host-only rule. In mercury, `crates/knowledge-graph/webapp/src/hooks/useGraphRunWorkflow.ts` was byte-identical to Java
  `main` before the fix, and the Rust engine forwards and replays a subscriber's commands the same way (`commands.rs`), so the twin is a port of those files plus
  a Rust bundle release. The webapp fact is `webapp-run-controls-equal-partners`. origin: 2026-10-02-050449.
  → serves: vision-mercury-composable
  **Update 2026-10-02:** resolved by the single-source deploy, not a port: mercury #347 removes the Rust copy and carries the Java bundle with the fix (PR #496, [[playground-webapp-single-source]]); closes when it merges.
  <!-- id: playground-subscriber-run-controls-rust-twin | created: 2026-10-02 | last_used: 2026-10-02 | uses: 1 | tier: working | origin: 2026-10-02-050449 -->
