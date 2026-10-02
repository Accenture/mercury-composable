- [ ] **(twin) The Rust Playground webapp's vitest suite has no web storage setup file, so its happy-dom tests should fail on Node 25+ as the Java copy's did.** The
  Java fix is on branch `fix/webapp-tests-node26-webstorage` (commit `8818b2de`, PR #494): `src/test/setupWebStorage.ts`, a `setupFiles` entry in
  `vitest.config.ts`, installs a fresh happy-dom `Storage` as `localStorage` and `sessionStorage`, and `src/test/__tests__/setupWebStorage.test.ts` pins it. In
  mercury, `crates/knowledge-graph/webapp` has the same `vitest.config.ts`, vitest 4.1.11, happy-dom ^20.11.1 and the same four storage-using happy-dom test files
  (not run there), so the twin is those two files and the config lines; test tooling only, no Rust bundle release. The setup file goes at the Vitest 5 upgrade
  (vitest-dev/vitest#10293) and the test stays; the webapp fact is `webapp-tests-node25-webstorage-setup`. origin: 2026-10-02-041143.
  → serves: vision-mercury-composable
  <!-- id: webapp-tests-webstorage-rust-twin | created: 2026-10-01 | last_used: 2026-10-01 | uses: 1 | tier: working | origin: 2026-10-02-041143 -->
