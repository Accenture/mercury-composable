- [ ] **(twin) The Rust Playground webapp still disables Instantiate and Run in a subscribed session.** The Java fix is on branch
  `fix/playground-subscriber-run-controls` (commit `0fafd86a`, PR #495): `useGraphRunWorkflow` no longer gates on `isPrimary` and resets the run only
  when the session subscribes or unsubscribes, `Playground.tsx` passes `sessionCollaboration.isPrimary`, two tests replace the gate test, and the webapp README
  and technical documentation drop the host-only rule. In mercury, `crates/knowledge-graph/webapp/src/hooks/useGraphRunWorkflow.ts` was byte-identical to Java
  `main` before the fix, and the Rust engine forwards and replays a subscriber's commands the same way (`commands.rs`), so the twin is a port of those files plus
  a Rust bundle release. The webapp fact is `webapp-run-controls-equal-partners`. origin: 2026-10-02-050449.
  → serves: vision-mercury-composable
  <!-- id: playground-subscriber-run-controls-rust-twin | created: 2026-10-02 | last_used: 2026-10-02 | uses: 1 | tier: working | origin: 2026-10-02-050449 -->
