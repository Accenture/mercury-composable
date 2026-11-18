- [x] **Essential built-in tools for agent loops (RFC-0013 → ADR-0036) - CLOSED 2026-11-18 (Eric: "since we have
  demonstrated that we can use existing MCP servers, please promote RFC-0013 as ADR").** Outcome: twelve built-in tools as
  pack functions with published rows and sandboxes in configuration (mercury-python #63/#64, mercury-nodejs #130; the
  pages #644/#645/#646), the Playground's three graphs driven live through both packs (#647,
  `docs/test-reports/agent-tools-live-drive.md`), the recipes page with the pinned digests of seven MCP servers and the
  gated digest test (#648/#649, mercury-python #66/#67, mercury-nodejs #132/#133); the ADR landed as #650 (squash `0aa0e8b60`, MERGED 2026-11-18), the
  Key Decision is [[agent-tools-built-or-bound]]. Lesson: build when the sandbox must be the pack's, bind when the server
  is a product's own front door - and pin the digest either way, because the model reads the graph's row, never the
  server's. Full narrative: the session logs from 2026-11-16-105602 to 2026-11-18-094338.
  → proposal: RFC-0013
  → serves: vision-mercury-composable
  <!-- id: agent-essential-tools | created: 2026-11-16 | last_used: 2026-11-18 | uses: 8 | tier: active | origin: 2026-11-16-105602 -->
