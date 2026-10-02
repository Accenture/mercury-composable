- [ ] **(drift) The AI agent guide mislabels `flows/flow-11.yml`.** The boilerplate manifest in `docs/guides/knowledge-graph/ai-agent-guide.md` (the row "`flows/flow-11.yml` and other example flows") gives its role as
  "Support-triage demo flows" and says "drop unless used"; flow-11 is Tutorial 11's echo flow, the `graph.extension` target (`flow://flow-11`), and its `flows.yaml` entry goes with the file. The Rust guide's twin
  table already says so. A docs-only PR; the AI contract snapshot packages that page, so run the contract tests. Eric decides whether to do it now.
  → serves: vision-mercury-composable
  <!-- id: flow-11-doc-label | created: 2026-10-01 | last_used: 2026-10-01 | uses: 1 | tier: working | origin: 2026-10-02-001806 -->
