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
  **Next:** surface `## Doc Gaps` at next review sweep; measure guide sufficiency rate.
  PR: [#306](https://github.com/Accenture/mercury-composable/pull/306) squash `6f6c3989` — merged.
  **Guide-first rule propagated 2026-09-01 to all four repos** (Rust
  [PR #222](https://github.com/Accenture/mercury/pull/222) squash `c730e17e`; python
  [PR #23](https://github.com/Accenture/mercury-python/pull/23) squash `a12c56b6`; node
  [PR #91](https://github.com/Accenture/mercury-nodejs/pull/91) squash `0899ad59`).
  <!-- id: thread-doc-improvement-feedback-loop | created: 2026-09-01 | last_used: 2026-09-01 | uses: 1 | tier: working | origin: 2026-09-01-032130 -->
