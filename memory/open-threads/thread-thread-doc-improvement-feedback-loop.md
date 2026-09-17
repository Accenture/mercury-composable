- [ ] (initiative — **guide-first rule recorded 2026-09-01**; loop design ratified) **Doc-improvement
  feedback loop** — ongoing tuning of the AI contract provider guides to raise "guide sufficiency rate"
  (fraction of "how do I" questions answerable from the guide alone, without source reads). Three layers:
  (1) **in-session capture** — session logs get `## Doc Gaps` (question / guide page / missing detail)
  whenever source was needed where the guide should have sufficed; (2) **periodic sweep** — at every
  memory review, tally gaps across recent logs and surface top items as a PR punch-list for the relevant
  `docs/` guide page; (3) **verification** — after each guide PR, a fresh-agent probe on the same
  question confirms source is no longer needed. Metric: guide sufficiency rate, measured on the same
  cadence as the onboarding fresh-agent re-measure. Seed gap from this session: guide-first rule was
  absent from `instructions.md` (now added); `minimalist-kafka.md` schema section was sufficient but the
  rule to check it first was not encoded. **First gap CLOSED same session:** `minimalist-kafka.md`
  §Outbound — `@KernelThreadRunner`/keep-instances-small callout added; body-must-be-`byte[]` named
  explicitly with `IllegalArgumentException`; misleading `Map` comment in §Produce YAML fixed.
  **First EXTERNAL-source firing 2026-09-02:** a field AI agent's three-layer build feedback
  (ai-enabled-repo-demo docs/feedback) drove the async-companion retirement (ADR-0021),
  the graph.js formal deprecation (ADR-0022), and an 8-guide docs campaign — Java PR #310
  + Rust PR #224, both merged (origin: 2026-09-02-050213). The loop works on external
  input, not just self-captured gaps.
  **MERGED 2026-09-16 (Eric):** [[thread-docs-improvement-backlog]] folded in here — it was this
  loop's output queue, not a separate initiative. Its acceptance criteria are now this thread's:
  **for humans, storytelling** (why before how, a narrative arc); **for AI agents, token-efficient**
  (shortest path, machine-greppable, "generate from this page alone"). Auditing that queue at merge
  found **8 of 9 items already shipped** with the record never updated — the same
  stale-nested-checklist failure reported upstream to the agent-memory team the same day.
  **Open queue (the only survivor of that audit):**
  - a request-response **RPC demo in `lambda-example`** — `HelloRemoteRelay` is a hop-through relay
    returning `Void`, so the RPC idiom still lives only in composable-example's `EventOverHttpRpc`.
  **Next:** surface `## Doc Gaps` at the next review sweep; measure guide sufficiency rate. **And
  keep the queue honest** — audit it at each sweep rather than only appending, which is what let the
  merged backlog rot for three months.
  PR: [#306](https://github.com/Accenture/mercury-composable/pull/306) squash `6f6c3989` — merged.
  **Guide-first rule propagated 2026-09-01 to all four repos** (Rust
  [PR #222](https://github.com/Accenture/mercury/pull/222) squash `c730e17e`; python
  [PR #23](https://github.com/Accenture/mercury-python/pull/23) squash `a12c56b6`; node
  [PR #91](https://github.com/Accenture/mercury-nodejs/pull/91) squash `0899ad59`).
  <!-- id: thread-doc-improvement-feedback-loop | created: 2026-09-01 | last_used: 2026-09-17 | uses: 3 | tier: working | origin: 2026-09-01-032130 -->
