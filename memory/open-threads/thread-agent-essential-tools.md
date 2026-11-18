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
  bind recipes). Eric agreed to all of R8-R16 the same day (`file.extract` in version 1): SETTLED for the build. **Step 1 BUILT 2026-11-17:** mercury-python
  #63 - `extensions/agent-tools/` with the four file tools, the two web tools and the clock, the sandboxes as
  configuration, the catalog files, `describe`, 98 tests and a live https fetch; the spec's section 13 lists six
  defaults set without a ruling (hidden entries invisible to every file tool, the resolved address pinned against DNS
  rebinding, the bounds, no `strict` on the rows, the statuses, row names = routes with `_`); this repo's page
  `docs/guides/agent-tools.md` (nav, `llms.txt`, the skill inventory; #644, merge `640c07577`, MERGED 2026-11-17). **Step 2 BUILT 2026-11-17:** mercury-python #64 - `graph.schema`, `graph.query`, `graph.write`, `graph.ingest` over
  the Bolt driver (a transaction seam, Neo4j's and Memgraph's schema procedures, the endpoint check inside the ingest's
  transaction), `file.extract` behind the `documents` extra, Neo4j on Colima for development, a live round trip; the
  spec's section 14 lists eight defaults set without a ruling; the page here extended (nav unchanged; #645, merge `a16b05d11`, MERGED 2026-11-17). **Step 3 BUILT 2026-11-17:**
  mercury-nodejs #130 - the Node twin with the same twelve routes, the catalog files byte for byte the Python pack's
  (pinned), 55 tests and the same live sequence on Neo4j; the spec's section 15 records three platform differences (the
  entity subset of the HTML reader, a Word table through mammoth's HTML, integers as JavaScript numbers; #646 here, merge `8e3ee2a3e`, MERGED 2026-11-17). **Step 4, the token-free half, BUILT
  2026-11-17** as PR #647 (merge `795b1f088`, MERGED 2026-11-17): the Playground's `site-notes` (crawl, then a supervised `file_write` through the
  advise/suspend/decide shape of `order-assistant`), `document-ingest` (`file_extract` + `graph_ingest` under `mode: auto`)
  and `document-query` (`graph_schema` + `graph_query`, sources cited); the synthetic corpus (four documents of fictional
  companies, one shape); the catalog rows copied from the packs' files; the Event-over-HTTP entries (`tools.peer.*`,
  8089); `AgentToolsGraphsTest` (8 tests) on `MockLlmChat`'s three new scripts and `MockAgentTools`, a stand-in for
  the pack's routes. **Step 4b DRIVEN 2026-11-17:** the three graphs live through the Python pack and then the Node.js pack on Haiku 4.5 (Redis
  and Neo4j on Colima, this project's published documentation as the crawl target) - every fact merged across runs and
  packs, the questions answered with sources, the notes written after approval; seven findings in
  `docs/test-reports/agent-tools-live-drive.md`: one route per Event-over-HTTP entry, the peer overrides as JVM properties
  before `-jar`, the document key spelled out in the ingest prompt (the model keyed it two ways once), a relative
  `include` (both packs changed: mercury-python #65, mercury-nodejs #131), a three-step research prompt against
  dithering, the loop's schema check correcting a call, a fetched 404 as a result. Steps 1-4 are complete in all three repositories. **Step 5 BUILT 2026-11-18** (held overnight at Eric's direction): the page `docs/guides/tools-from-mcp-servers.md` -
  seven recipes (Git, Time, Fetch, Filesystem, Neo4j's Cypher server, GitHub's hosted server, Playwright's browser), each
  the server-file entry, the digests, the bind lines and a row rewritten for the graph in the loop's vocabulary; the
  digests of 95 tools read from the pinned versions through both packs' `describe`, identical - with nav, `llms.txt`,
  the skill inventory and the agent-tools page's links; the packs' `mcp-recipes.yml`, the shared digest vector and the
  gated live test (mercury-python #66, mercury-nodejs #132, 11 live passes each); the spec's section 16 (#648
  here, squash `dac2816f0`, MERGED 2026-11-18; the pack PRs merged the same day). GitHub's digests followed the same day, read with Eric's fine-grained token (39 tools of the default toolsets, 22
  under the read-only header; mercury-python #67, mercury-nodejs #133, #649 here). The build plan is complete. Next: Eric's call
  on RFC-0013's promotion to an ADR.
  → proposal: RFC-0013
  → serves: vision-mercury-composable
  <!-- id: agent-essential-tools | created: 2026-11-16 | last_used: 2026-11-17 | uses: 2 | tier: working | origin: 2026-11-16-105602 -->
