- [x] (docs backlog → **MERGED 2026-09-16 into [[thread-doc-improvement-feedback-loop]], Eric**)
  **Documentation improvement — serve both audiences.** Merged because the two threads were the
  machinery and its queue: the feedback loop's step (2) produces exactly this punch-list. The
  acceptance criteria survive in the loop thread — **for humans, storytelling** (why before how, a
  narrative arc); **for AI agents, token-efficient** (shortest path, machine-greppable, "generate
  from this page alone").
  **Audited at merge — 8 of 9 items had shipped** without the thread being updated: boot-and-test
  recipe (`waitForProvider`), test-fixture pattern, runtime-API signature catalogs, reserved-route
  extension contract (`registration-metadata-contract.md`), `${ENV_VAR:default}` substitution,
  `FlowExecutor.request` programmatic drive, inspect-subtree parity, and the auto-registration fact
  (now in five guides — the dedicated "author an extension" recipe was overtaken by three real
  extension modules carrying their own docs). **One genuine residual carried forward:** a
  request-response RPC demo in `lambda-example` — `HelloRemoteRelay` is a hop-through relay
  returning `Void`, so the RPC idiom still lives only in composable-example's `EventOverHttpRpc`.
  origin: sessions of 2026-06-24
  <!-- id: thread-docs-improvement-backlog | created: 2026-06-24 | last_used: 2026-09-02 | uses: 11 | tier: archive-candidate -->
