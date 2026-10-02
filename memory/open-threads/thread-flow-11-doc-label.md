- [x] **(drift) The AI agent guide mislabelled `flows/flow-11.yml`. CLOSED 2026-10-02.** PR #490 (squash `12a514b2`) changed the manifest row's role from
  "Support-triage demo flows" to Tutorial 11's echo flow, the `graph.extension` target (`flow://flow-11`), and says a flow's `flows.yaml` entry goes with its file;
  docs only, `mkdocs --strict`, the doc scripts and the 19 contract tests green. Lesson: a derived project trims against that table, so a wrong Role cell misleads.
  origin: 2026-10-02-001806
  → serves: vision-mercury-composable
  <!-- id: flow-11-doc-label | created: 2026-10-01 | last_used: 2026-10-01 | uses: 1 | tier: working | origin: 2026-10-02-001806 -->
