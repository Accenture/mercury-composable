- [ ] **Essential built-in tools for agent loops: files inside a workspace and the web inside an allowlist, as pack
  functions with published catalog rows (RFC-0013; design sprint 4, Eric's topic of 2026-11-16).** Round 1 drafted:
  `extensions/agent-tools/` in the packs (Python first, Node in lockstep) with `file.read`, `file.write` (`write: true`),
  `file.list` under `tools.file.root`, and `web.fetch`, `web.crawl` under `tools.web.allow` (private ranges refused,
  redirects checked, sizes, pages and depth capped, `robots.txt` honoured, HTML to text in-house); a tool whose sandbox is
  not configured does not register; rows published as files and by a `describe` command; the Playground example
  `site-notes` (a crawl and a supervised write). Spec `draft-design-specs/agent-essential-tools.md` (#634, `57a267f02`); rulings
  R1-R7 (section 9) are Eric's. **Round 2 (2026-11-17):** R1-R7 ruled - `file.search` added; the extension, the in-house HTML-to-text, the gated
  registration, `create`, the crawl caps and `site-notes` confirmed. Eric proposed a graph-database tool for semantic
  analysis (documents ingested once, rules extracted into a graph, never re-scanned) and more built-ins, and asked which
  reference MCP servers exist: the spec now carries `graph.schema`, `graph.query` (read transaction), `graph.write` and
  `graph.ingest` (parameterized `MERGE` of what a model extracts) over openCypher and the official Bolt driver (Neo4j,
  Memgraph; FalkorDB and Apache AGE behind the same routes), `time.now`, `file.extract` as a candidate, and section 8's
  survey (seven maintained reference servers, thirteen archived, the vendors' graph servers, the build-or-bind rule, the
  bind recipes). Eric agreed to all of R8-R16 the same day (`file.extract` in version 1): SETTLED for the build. Next: the
  build (Python, Node, the Playground's `document-ingest`/`document-query`, the recipes page).
  → proposal: RFC-0013
  → serves: vision-mercury-composable
  <!-- id: agent-essential-tools | created: 2026-11-16 | last_used: 2026-11-16 | uses: 1 | tier: working | origin: 2026-11-16-105602 -->
