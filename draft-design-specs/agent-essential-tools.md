# Essential built-in tools for agent loops — files, the web, a knowledge graph, a clock — design spec

**Status:** DRAFT, round 2 (2026-11-17, Claude Code for Eric). Round 1 (2026-11-16) was raised as RFC-0013 in
[`docs/arch-decisions/RFC.md`](../docs/arch-decisions/RFC.md). Eric's rulings of 2026-11-17 on R1-R7 are recorded in
section 11 and applied: the set gains `file.search`. On his proposal the paper adds the knowledge-graph tools (section
4.3) and a clock, answers his question on the reference MCP servers (section 8), and records the round-2 rulings
R8-R16, all agreed by Eric on 2026-11-17; the paper is settled for the build.
**Serves:** `bp-agent-orchestration` → `vision-mercury-composable` (Track 2, the graph as AI SDLC).
**Extends:** [`agent-tool-use.md`](./agent-tool-use.md) (a tool is a composable function; a route names the capability,
never the transport) and [`llm-helper-agent-loop.md`](./llm-helper-agent-loop.md) section 5 (the tool catalog) and
section 10 (security).
**Repo scope:** a new extension in the packs, `extensions/agent-tools/` (mercury-python first, mercury-nodejs in
lockstep), each tool with its published catalog row; Playground example graphs here; a documentation page for the
tools and one for the reference-server recipes. No engine code.

---

## 1. The problem

Every agent use case built so far needed tools that were not about the use case: the diagram assistant needed one
remote tool and an engine-side wrapper to keep its payload out of band; the next graphs will want to read a file the
person pointed at, write a note or a report, fetch a page, or read a documentation section. Today an author writes a
function per tool, or binds a reference MCP server through the binding. Both work, and both repeat the same security
decisions in every graph: which directory, which hosts, how big a result, what counts as a write.

Eric's direction (2026-11-16): identify and build the built-in tools that are essential for common cases, reading and
writing local files and crawling web content among them. His additions (2026-11-17): a built-in tool for graph
database read and write, for semantic analysis - documents ingested once and their rules extracted into a graph, so
the model does not scan the files again and again - and more built-ins to get started with; and a question, which
reference MCP servers exist out of the box, so that nothing is re-invented in custom code where a server already does
the job. This paper names the set, says where each tool lives, how each is governed, how an author puts one on a graph,
and when to bind a server instead of building a function.

## 2. Principles

1. **A built-in tool is a composable function**, declared with `preload` in a pack extension, addressed by a route that
   names the capability (`file.read`, `web.fetch`, `graph.query`). The loop, the catalog and the graph see nothing
   special about it; a built-in may later be replaced by an MCP tool behind the same route, or an MCP tool by a
   built-in, as `agent-tool-use.md` promises.
2. **Each tool ships its catalog row.** The name, the description the model reads, the input schema the loop validates
   against, `strict`, and `write: true` where it applies, are published with the tool, so an author copies a row onto
   the graph rather than writing one; the loop's pre-flight check still certifies what the graph names.
3. **The sandbox is deployment configuration.** Which directory tree the file tools may touch, which hosts the web
   tools may reach and which database the graph tools may query are keys of the pack's configuration, certified with
   the deployment like the MCP server file, never arguments of a call. A tool whose sandbox is not configured does not
   register, so an unconfigured tool is a 404 to the loop's pre-flight check, not an open door (ruling R4: restricted
   to registered constraints).
4. **Results are text, capped, and untrusted.** A tool returns text or a small map; it never returns a binary; it caps
   its own result below the loop's `result_limit`; and what it returns is data a model reads, never an instruction the
   loop obeys. A fetched page that says "ignore your instructions" is a page.
5. **Few, essential, boring.** The set is what every second use case needs, with the dangerous generalities left out:
   no shell, no arbitrary HTTP request, no SQL against a system of record. **Amended 2026-11-17 (Eric):** the agent's
   own knowledge graph is in - a database the agent fills and queries, inside configured limits, is a capability of
   the agent and not a door into the business's data. A use case with another such need writes its own route and
   certifies it on its graph.
6. **Build or bind, by rule** (section 8). A capability is built in-house when its sandbox must be the pack's, when the
   pack process must not depend on a second runtime, when the contract must be portable across backends, or when it is
   a few lines; it is bound from an existing MCP server when the server is a product's own front door whose surface is
   what would be written anyway. Neither is the default for everything.
7. **Extract once, query many.** Knowledge a model extracts belongs in a store the next agent queries, not in a
   transcript that expires: the transcript store holds a turn, the audit store holds what was said (RFC-0012), the
   knowledge graph holds what was learned, across transactions.

## 3. Shapes considered

**A. The reference MCP servers through the binding** (the filesystem server, the fetch server). Zero code and already
possible; set aside as the default because each server is a child process with its own runtime (Node for the
filesystem server, Python with an optional Node for the fetch server), its schemas and descriptions are the server's
(the author rewrites them on the graph anyway), and the controls differ from the ones a deployment certifies here: the
filesystem server takes allowed directories and a read-only flag, with fifteen tools where a graph wants three or
four; the fetch server (2026) refuses private addresses by default and takes `--allowed-hosts`, but has no crawl and
answers in pages of 5 000 characters. An installation that prefers a server binds it; the routes are the same
(section 8 carries the recipes).

**B. Engine-side functions** in `extensions/agent-loop` (Java, and Rust in lockstep). Available without a pack
process; set aside because an agent deployment always runs a pack for `llm.chat`, the web-content libraries are weaker
there, and two implementations must be kept in lockstep for every tool.

**C. A pack extension, `extensions/agent-tools/`** (ruled, R2). One implementation per pack beside the helper that
every agent deployment runs, the richest libraries for the web, and the packs' lockstep discipline already in place.
The extension is an application on the pack, like the helper and the binding, not part of the package.

**D. A generic `http.request` tool.** Rejected: a URL the model chooses is the server-side request forgery the web
tools exist to prevent; an API the use case needs gets a wrapper route with its own schema, certified on the graph.

## 4. The set (version 1)

Fourteen tools in four groups (`file.extract` joined version 1 by ruling R14). Every tool refuses out-of-sandbox input with a 400 that names the rule, which the loop
returns to the model as that call's error; the node never fails because of a tool.

### 4.1 Files, inside one workspace

The workspace is `tools.file.root`, one directory tree. Every `path` is relative to it, resolved with symbolic links
followed, and refused when the result is outside the root (`..`, an absolute path, a link that escapes). The pack's
process should run as a user with no rights beyond that tree.

| Route | Row | Input | Result |
| --- | --- | --- | --- |
| `file.read` | `file_read` | `path` (string, required); `max_bytes` (integer, optional, capped by `tools.file.max.bytes`, default 256 KiB) | `{path, bytes, text, truncated}`; UTF-8 text only, a binary file is an error naming the detected type |
| `file.write` | `file_write`, **`write: true`** | `path`, `content` (string), `mode` (`create` default, `overwrite`, `append`) | `{path, bytes, mode}`; `create` refuses an existing file (ruling R5); parent directories are created inside the root; `tools.file.write.extensions` may restrict the suffixes (default: any) |
| `file.list` | `file_list` | `path` (directory, default `.`), `glob` (optional, e.g. `**/*.md`), `max_entries` (default 200, cap 2000) | `{entries: [{path, type, bytes, modified}], truncated}`; hidden entries excluded unless `tools.file.hidden=true` |
| `file.search` | `file_search` | `pattern` (string, required: a literal, or a regular expression with `regex: true`), `path` (directory, default `.`), `glob` (optional file filter), `case_sensitive` (default false), `context` (lines around a match, default 0, cap 3), `max_results` (default 100, cap 1000) | `{matches: [{path, line, text, before, after}], files_searched, truncated}`; text files only, binaries skipped, each line cut to `tools.file.line.max.chars` (default 500); the read-only sixth tool of ruling R1 |

`file.write` carries the write mark, so under the node's default mode a person approves it; the other three are reads
and run unasked.

### 4.2 The web, inside an allowlist

The web tools register only when `tools.web.allow` names at least one host pattern (`docs.example.com`,
`*.example.com`). A URL is fetched only when its host matches, over `https` (an `http` host must be listed with its
scheme), and every redirect is checked against the same list. Addresses that resolve to loopback, private, link-local
or multicast ranges are refused unless the host is listed by name for that purpose (`tools.web.allow.private=true`),
which closes the request-forgery route to a cloud metadata service or an internal admin port. The download stops at
`tools.web.max.bytes` (default 1 MiB), the request at `tools.web.timeout.ms` (default 10 s), and the tool identifies
itself with a Mercury user agent.

| Route | Row | Input | Result |
| --- | --- | --- | --- |
| `web.fetch` | `web_fetch` | `url` (string, required); `as` (`text` default, `markdown`, `raw`) | `{url, status, content_type, title, text, bytes, truncated}`; HTML is reduced to its readable text (scripts, styles and navigation dropped; headings, lists and links kept as text or Markdown); `raw` returns the body as received when it is text |
| `web.crawl` | `web_crawl` | `url` (the start page); `max_pages` (default 10, cap `tools.web.crawl.max.pages`, 50); `max_depth` (default 2); `include` (an optional path prefix the crawl stays under) | `{pages: [{url, title, text, bytes}], visited, truncated}`; same host only, links followed breadth-first, `robots.txt` honoured, `tools.web.crawl.delay.ms` between requests (default 500), each page's text cut to `tools.web.page.max.chars` (default 20 000); the defaults and caps of ruling R6 |

The crawl is a bounded reader of a documentation section or a small site, not a spider: the page cap, the depth, the
include prefix and the delay are all limits, and the whole result is cut to fit the loop's `result_limit`, so a model
that needs more asks again with a narrower start.

### 4.3 The knowledge graph, inside one database (Eric's proposal, 2026-11-17)

A built-in tool for graph database read and write, for semantic analysis: an agent ingests documents once - contracts,
regulations, specifications - and extracts their entities, relations and rules into a graph, and later agents answer
from the graph instead of scanning the files again on every question (principle 7). The sandbox is one database:
`tools.graph.uri` (a Bolt URI), `tools.graph.database`, the credential by reference (`tools.graph.username`,
`tools.graph.password: ${GRAPH_PASSWORD}`). The reads register when the URI is set; the writes only when
`tools.graph.write=true`. The query language is openCypher, which the models know well and which Neo4j, Memgraph,
FalkorDB, Apache AGE and Amazon Neptune all speak; the reference backend is the Bolt protocol through the official
driver, which reaches Neo4j and Memgraph alike (section 4.5).

| Route | Row | Input | Result |
| --- | --- | --- | --- |
| `graph.schema` | `graph_schema` | none | `{labels: [{label, count, properties: [{name, types}]}], relationship_types: [{type, count, from, to}]}` from the database's own schema procedures (no plugin: `db.labels`, `db.schema.nodeTypeProperties`, `db.schema.relTypeProperties`, and Memgraph's equivalents); what the model reads before it writes a query |
| `graph.query` | `graph_query` | `query` (openCypher, read-only), `parameters` (object, optional) | `{columns, rows, truncated}`: run in a read transaction, so a statement that writes fails with the database's access-mode error, and refused before sending when it carries a write clause (`CREATE`, `MERGE`, `SET`, `DELETE`, `REMOVE`, `DROP`, `LOAD CSV`); rows capped by `tools.graph.max.rows` (200, cap 2000); a node rendered `{id, labels, properties}`, a relationship `{id, type, start, end, properties}`, a path as a list; `tools.graph.timeout.ms` (10 s) |
| `graph.write` | `graph_write`, **`write: true`** | `query` (openCypher), `parameters` | `{counters: {nodes_created, nodes_deleted, relationships_created, relationships_deleted, properties_set, labels_added, labels_removed, indexes_added, constraints_added}}` from a write transaction; `DELETE`, `DETACH DELETE` and `DROP` refused unless `tools.graph.delete=true` |
| `graph.ingest` | `graph_ingest`, **`write: true`** | `nodes: [{key, label, properties}]`, `relationships: [{from, to, type, properties}]`, `source` (string, optional) | `{nodes, relationships}` counts: one `MERGE` per node on `(:Label {key})` with `SET n += $properties`, one per relationship between two keys (both must exist, in this call or already in the graph), every value a parameter, `source` stamped on each node and relationship; labels and types validated `^[A-Za-z][A-Za-z0-9_]{0,63}$`, values scalars or lists of scalars; at most `tools.graph.ingest.max.nodes` (500) per call; running it twice changes nothing |

Why `graph.ingest` beside `graph.write`: extraction is the ingestion case, and a model writing a long `MERGE`
statement with dozens of parameters is where Cypher goes wrong. A list of nodes and relationships is what a model
extracts well; the tool makes the Cypher, no value is ever interpolated into a statement, and the write is idempotent.
The row's description tells the model the conventions: a key unique across the graph, prefixed with the label in
lower case (`clause:12`, `party:supplier-a`), a `source` on every ingest (the document's path or URL, so an answer
can cite), and the labels of the domain the graph author names in the system prompt. `graph.write` stays for
everything else - an index, a constraint, a correction - under the write mark. Both writers carry the mark: under
the node's default mode a person approves each, and an ingestion graph declares `mode: auto` on its ingest node
(ADR-0033: a write runs unasked only under a declared `auto`), the product owner's decision at design time, visible
in the graph's certification. The reference Memory server's shape (entities, relations, observations in a JSON file)
is the same idea at toy scale; these four tools put it over a database an installation governs.

### 4.4 The clock

A model has no clock, and "today", "within 30 days" and "last quarter" need one. `time.now` (`time_now`): `timezone`
(an IANA name, optional, default `UTC`) returns `{utc, local, timezone, offset, weekday, epoch_ms}`. It reads nothing
and has no sandbox to miss, so it registers unless `tools.time=false`. It replaces a child process for a clock (the
reference Time server, section 8).

### 4.5 Dependencies

Files, web and clock add nothing: the packs already depend on an HTTP client (aiohttp in Python; Node's built-in
`fetch`), and HTML to text is written in-house in each pack (ruling R3), as the OTLP encoder and the MessagePack codec
were: a small tokenizer that drops `script`, `style`, `nav`, `header` and `footer`, keeps the text of the rest, and
renders headings, list items, paragraphs and links in Markdown. It is enough for documentation and articles, and it is
tested on a fixture set of pages; a library (trafilatura, Readability) stays an option an installation may swap in
behind the same route if its pages need main-content extraction.

The graph tools take the official Bolt driver as an optional extra, as the helper takes Redis and PostgreSQL: Python
`graph = ["neo4j>=6,<7"]` (asynchronous, Python ≥ 3.10 like the pack), Node `neo4j-driver` as a dev dependency, so the
published packages stay free of a database client. The same driver reaches Neo4j and Memgraph. FalkorDB (its own
client over the Redis protocol) and Apache AGE (openCypher wrapped in SQL through psycopg, on the PostgreSQL the audit
store already runs) are an installation's alternative: the four routes over another client, behind the same rows.

## 5. Governance and security

- **The catalog is still the boundary.** A graph names the tools its agent reaches; a built-in the graph does not name
  is as unreachable as any route. The pre-flight check verifies the routes exist, so a deployment whose sandbox is not
  configured fails the graph before a token is spent.
- **Writes are marked.** `file.write`, `graph.write` and `graph.ingest` carry `write: true`; the node's mode decides
  whether a person approves each. The web tools and the other file and graph tools are reads, bounded by the allowlist,
  the root and the read transaction.
- **The allowlist, the root and the database are certification artifacts.** They live in the pack's configuration with
  the server file, are reviewed with the deployment, and answer "what can this agent reach?" without reading code.
- **The graph's two gates.** Writing needs `tools.graph.write=true`; deleting needs `tools.graph.delete=true` as well.
  A read runs in a read transaction with a reader's credential where the database offers one; an ingest is parameters
  only, and a label or type that is not an identifier is refused.
- **Untrusted content.** A page, a file or a graph property may carry instructions; the loop never obeys a result
  (section 10 of the loop spec), and the graph author's system prompt should say that fetched and stored content is
  data. The tools add nothing to a result but what they read.
- **Logs.** A tool logs the route, the status, the byte or row count and the host of a URL; never a path's content,
  a page's text, a query's parameters or a full URL (a query string may carry a token).
- **Limits are defaults a deployment lowers, never raises past the caps** named above; the caps are the tool's.

## 6. Publishing the rows

Each tool module carries its row as a constant, and the extension has a `describe` command, as the binding does,
that prints every row as the JSON a catalog node takes:

```bash
uv run mercury-serve extensions/agent-tools/agent_tools.py --describe
```

The same rows are kept as files under `extensions/agent-tools/catalog/` (`file_read.json`, ...) for a copy without
running anything, and the engines' documentation gains a page listing them with their limits, so an author reads the
contract where the catalog syntax is documented. The rows are shared byte for byte between the packs, with a test
that pins them, as the vector files are.

## 7. The example graphs (the Playground)

- **`site-notes`** (ruling R7): the person names a documentation section and a topic; the agent crawls the section
  (the allowlist names the documentation host), reads what it needs, writes `notes/<topic>.md` into the workspace, and
  returns the summary with the file's path. It exercises a read of the outside and a supervised write (the node's
  default mode holds `file_write` for a person, so the graph suspends and resumes as `order-assistant` does).
- **`document-ingest`**: the person names a document in the workspace (a synthetic corpus of a few policies and
  agreements ships with the example, written for it, no real document); the agent reads it, extracts the parties,
  obligations, rules and deadlines it finds, and ingests them with the document as `source`. The ingest node declares
  `mode: auto`, so an ingestion batch runs without a person approving each write; the graph's certification shows it.
- **`document-query`**: a question; the agent reads the schema, queries the graph, and answers with the sources it
  found - no file is read. Run after `document-ingest` over the corpus, it shows the point of the knowledge graph: the
  documents were read once.

Each has a token-free test on a scripted `llm.chat` with a fixture web server or a fake graph store, and a live drive
on Haiku 4.5: `site-notes` against a public documentation site, the two document graphs against Colima's database. The
transcript audit of RFC-0012 records every drive if that build lands first.

## 8. The reference MCP servers (Eric's question, 2026-11-17)

What exists out of the box, as of 2026-11, and what this paper does with each. The reference repository's own words:
the servers are reference implementations to demonstrate the protocol and the SDKs, educational examples for
developers building their own, not production-ready solutions. The MCP Registry is the live catalog; the vendors'
own servers are where the production-grade work is.

**Maintained reference servers** (`modelcontextprotocol/servers`):

| Server | Runtime | What it offers | Here |
| --- | --- | --- | --- |
| Filesystem | Node (`npx @modelcontextprotocol/server-filesystem <dirs>`) | 15 tools: read (text, media, many), write, append, edit, create directory, list (with sizes), tree, move, copy, search by glob, file info; allowed directories, `--read-only` | covered by the six `file.*` tools in-house (workspace root, write mark, no Node runtime in the pack's process); the server stays an alternative behind the same routes |
| Fetch | Python (`uvx mcp-server-fetch`), optional Node for readability | one tool, `fetch` (`url`, `max_length` 5 000, `start_index`, `raw`); Markdown out; `robots.txt`; private addresses refused by default; `--allowed-hosts`; proxies | covered by `web.fetch` and `web.crawl` in-house (the crawl, the allowlist in the pack's configuration, no child process); a fair alternative for `web.fetch` alone |
| Git | Python (`uvx mcp-server-git --repository <path>`) | 14 tools: status, diffs, log, show, add, commit, reset, branches, checkout, merge, remotes | **bind** when a use case works on a repository (a recipe); not built |
| Time | Python (`uvx mcp-server-time`) | `get_current_time`, `convert_time`, `get_time_ago` | `time.now` in-house: a clock is a few lines, not a process; the server is the alternative |
| Memory | Node | a knowledge graph in a JSON file: entities, relations, observations | the right shape, a toy store; the `graph.*` tools give the same shape over a database |
| Sequential Thinking | Node | a scaffold for step-by-step reasoning, no external state | not on a graph: the models' own extended thinking covers it, and it reads and writes nothing of the system |
| Everything | Node | a test server exercising every protocol feature | the binding's tests may use it; never on a graph |

**Archived** (`modelcontextprotocol/servers-archived`, no longer maintained): AWS KB Retrieval, Brave Search, EverArt,
GitHub, GitLab, Google Drive, Google Maps, PostgreSQL, Puppeteer, Redis, Sentry, Slack, SQLite. Where one is wanted,
the vendor's own server is the successor: GitHub's official server, GitLab's built-in one, Brave's search server,
Microsoft's Playwright server for browser automation (Puppeteer's successor), Redis's official server, Sentry's remote
server, and for PostgreSQL the database vendors' or the community's servers. Each is bound through the binding like
any other; none is built here.

**Graph databases** (vendor servers, relevant to section 4.3): Neo4j's `mcp-neo4j-cypher` (Python; `get_neo4j_schema`,
which needs the APOC plugin, `read_neo4j_cypher`, `write_neo4j_cypher`; `--read-only`; stdio or HTTP; a response
token limit), Memgraph's `mcp-memgraph` (Python; its toolbox; stdio or HTTP), FalkorDB's `mcp-server-falkordb`
(Python; schema, read, write), and community servers for Apache AGE. The Neo4j server is the bind alternative to
`graph.schema`, `graph.query` and `graph.write`; `graph.ingest` has no counterpart in any of them - a model would
write the `MERGE` statements itself - which is the reason the graph tools are built rather than bound.

**The rule** (principle 6), applied: build when the sandbox must be the pack's (a workspace root and a write mark; a
host allowlist; row caps and a delete gate), when the pack's process must not depend on a second runtime, when the
contract must be portable across backends and shipped as rows the packs pin byte for byte, or when the capability is a
few lines; bind when the server is a product's own front door - a code forge, a browser, a SaaS, a database vendor's
server - maintained by its vendor, with the surface that would be written anyway, its reads unasked and its writes
approved under the node's mode like any other.

**The recipes.** Binding is already zero code (`agent-tool-use.md`: a server-file entry, a `bind` line, a `preload`
function, a catalog row); what is missing is the worked set. A documentation page in the engines' docs, "Tools from
MCP servers", and commented entries in the binding's `resources/mcp-servers.yml` carry, for each recipe, the
server-file entry with its credential references, the `describe` digests, the `bind` lines and the catalog rows with
the descriptions rewritten for the graph: Git, Time, Fetch and Filesystem (the reference servers), Neo4j's Cypher
server (the alternative to the graph tools), GitHub's server and Playwright's (vendor servers). A gated live test
starts each reference server locally with `uvx` or `npx` and checks its digests, so a recipe that rots is caught at the
digest, as the binding's boot would catch it.

## 9. Candidates kept out of version 1, or in by ruling

- **`file.extract`** (`file_extract`): PDF, Word and HTML files to text with page markers, `path`, `pages` (a range),
  `max_chars`, for the ingestion of documents that are not plain text - which is what contracts and regulations are.
  Python `pypdf` and `python-docx`, Node `pdf-parse` and `mammoth`, behind an optional extra `documents`. The
  ingestion example reads Markdown without it; the field's documents will need it. In version 1 (ruling R14).
- **`sql.query`**: read-only, parameterized SQL on one allowlisted database with a reader's credential; the archived
  PostgreSQL reference server was this. Kept out: a system of record behind a wrapper route with its own schema is the
  certifiable form, and the knowledge graph is in precisely because it is the agent's own store (ruling R16).
- **`web.search`**: a vendor's API (Brave, Tavily, Exa) with a key. Bound from the vendor's server by recipe, never
  built; the key is a reference in the server file.
- **Repositories and browsers**: the Git recipe and the Playwright recipe (section 8).
- **A shell, a generic request, an e-mail or a notification**: never a tool. The first two are principle 5; the last
  two are a graph node's job after the loop, under the graph's own rules, not a model's call.

## 10. Consequences

- **For the packs:** a new extension with fourteen routes, four configuration blocks, an in-house HTML-to-text, a
  fixture web server and a fake graph driver for the tests, a README and the catalog files; two optional extras
  (`graph`, `documents`); lockstep as always.
- **For the engines:** none in code. Two documentation pages, and the example graphs with their tests and corpus.
- **For an author:** a tool is a row copied from the catalog folder and an Event-over-HTTP entry for the pack's host;
  the sandbox is a few configuration keys on the pack; an ingestion node declares `mode: auto` knowingly.
- **For the binding:** unchanged in code; it gains the recipes and their digest test.
- **For the field:** a server-side knowledge graph fed by agents and governed like any database (roles, TLS, backup),
  with an openCypher contract that lets an installation choose its graph database.

## 11. Rulings

**Recorded (Eric, 2026-11-17, on round 1):**

- **R1 — the set.** Extended: the five plus `file.search` (section 4.1).
- **R2 — where.** Agreed: `extensions/agent-tools/`, Python first, Node in lockstep.
- **R3 — HTML to text.** In-house, with no dependency.
- **R4 — registration.** Agreed: a tool is restricted to its registered constraints; unconfigured, it does not register.
- **R5 — `file.write`.** Agreed: `create` the default, refusing an existing file.
- **R6 — the crawl's defaults and caps.** Agreed.
- **R7 — the example.** Agreed: `site-notes` with its supervised write and live drive.

**Round 2 (Eric, 2026-11-17): agreed to all of R8-R16 as proposed.**

- **R8 — the knowledge-graph tools.** Agreed: `graph.schema`, `graph.query`, `graph.write` and `graph.ingest` as in
  section 4.3, with `graph.ingest` the structured, parameterized `MERGE` for extraction results.
- **R9 — the backend.** Agreed: Bolt and openCypher through the official driver as the `graph` extra (Neo4j and
  Memgraph), with FalkorDB and Apache AGE as an installation's alternative behind the same routes.
- **R10 — the graph's gates.** Agreed: writers register only with `tools.graph.write=true`; `DELETE`, `DETACH
  DELETE` and `DROP` refused unless `tools.graph.delete=true`.
- **R11 — development.** Agreed: Neo4j community on Colima, its own Browser as the viewer, in `dev/graph-up.sh`.
- **R12 — the examples.** Agreed: `document-ingest` (its ingest node `mode: auto`) and `document-query` over a
  synthetic corpus, beside `site-notes`.
- **R13 — the clock.** Agreed: `time.now` in version 1, registered unless switched off.
- **R14 — `file.extract`.** Agreed: PDF and Word to text behind the `documents` extra, in version 1.
- **R15 — the reference servers.** Agreed: the recipes page and server-file entries (Git, Time, Fetch, Filesystem,
  Neo4j's Cypher server, GitHub's, Playwright's) as the documented way to leverage existing servers; Memory,
  Sequential Thinking and Everything left off graphs.
- **R16 — `sql.query`.** Agreed: out of version 1; a system of record gets a wrapper route.

## 12. Build plan

1. **Python, the sandboxed tools:** `extensions/agent-tools/` with the six file tools, the two web tools and the
   clock, the sandbox checks, the HTML-to-text, the `describe` command, the catalog files, the fixture web server and
   the tests, the README. The engines' documentation page "Built-in tools".
2. **Python, the knowledge graph:** the four graph tools over the Bolt driver (`graph` extra), the fake driver for the
   unit tests, `dev/graph-up.sh` and `graph-down.sh`, the gated live test against Colima's database; `file.extract`
   behind the `documents` extra (R14).
3. **Node in lockstep:** the same routes, rows and tests; the catalog files byte-identical and pinned.
4. **The Playground:** `site-notes`, `document-ingest` and `document-query` with the synthetic corpus, their token-free
   tests, the Event-over-HTTP entries, the READMEs; then the live drives and their reports under `docs/test-reports/`.
5. **The recipes:** the page "Tools from MCP servers", the server-file entries and the gated digest test. Then promotion
   of RFC-0013 is Eric's call.

## 13. Build notes (mercury-python, 2026-11-17 - PR #63, step 1)

The Python build of step 1 kept the paper as settled and set six defaults the paper left to the build; the Node twin
follows them, so the two extensions serve the same contract from the same catalog files:

1. **Hidden entries are invisible to every file tool** unless `tools.file.hidden=true` - section 4.1 said it for
   `file.list`; a `.env` or a `.git` folder inside the workspace must not be readable by default either, so the rule
   covers `file.read`, `file.write` and `file.search` too (a 400 naming the key).
2. **The resolved address is pinned.** The host is resolved before the request, every address it resolves to is
   checked against the ranges (IPv4-mapped IPv6 included), and the request is made to the checked address with the
   `Host` header and the TLS server name set from the URL - so a second resolution between the check and the connect
   (DNS rebinding) cannot point the request elsewhere. Every redirect goes through the same path.
3. **Bounds the paper did not name.** A `file.write` is bounded by `tools.file.max.bytes` as a read is; a crawl's
   whole text is cut to 60 000 characters, under the loop's default `llm.run.result.limit` (65 536); `max_depth` is
   capped at 5; a crawl skips a page that fails and counts the pages it fetched in `visited`. The page's own HTTP
   status (a 404, a 500) is a result of `web.fetch`, not an error of the tool.
4. **No row sets `strict`.** Every row has optional properties, and whether a grammar-constrained schema may carry
   them is the model's and the API's; the graph's author adds the flag. Every row closes its object
   (`additionalProperties: false`) and the tool refuses an unknown argument by name, so the loop and the tool agree.
5. **Statuses.** 400 a refusal naming the rule and the key, 404 a missing file or folder, 408 a site that did not
   answer within `tools.web.timeout.ms`, 409 an existing file under `create`, 502 a host that cannot be resolved or
   a failed connection.
6. **Row names are the routes with `_` for `.`** (`file_read` for `file.read`), pinned by a test, and the catalog files
   are the rows byte for byte (`json.dumps(row, indent=2, ensure_ascii=False)` plus one newline), so the Node twin
   copies the files and pins the same bytes.

The in-house HTML reader also drops any element marked `role="navigation"`, `role="banner"`, `role="contentinfo"` or
`aria-hidden="true"`: a documentation site's sidebar is a `div` with a role as often as a `nav`, which the first live
fetch of a public documentation page showed.

## 14. Build notes (mercury-python, 2026-11-17 - PR #64, step 2)

The Python build of step 2 kept section 4.3 and ruling R14 as settled and set eight defaults the paper left to the
build; the Node twin follows them:

1. **A missing relationship endpoint fails the whole ingest.** The endpoints are checked inside the transaction after
   the node merges, and a key that exists neither in the call nor in the graph is a 400 naming it, with nothing
   written - never a silent skip of the relationship.
2. **Rows are capped by configuration only** (`tools.graph.max.rows`, 200, the tool's cap 2000), no per-call argument;
   the model narrows a cut result with `WHERE` and `LIMIT`.
3. **The ingest's bounds.** At most `tools.graph.ingest.max.nodes` nodes and four times as many relationships per call;
   a key at most 512 characters; property names identifiers; `key` and `source` reserved for the tool; a null value
   dropped rather than written.
4. **`graph.write` returns `columns`, `rows` and `truncated` beside `counters`**, so a `RETURN` after a write is kept.
5. **The schema joins two sources:** the counting queries (`MATCH (n) UNWIND labels(n) ...`, `MATCH (a)-[r]->(b) ...`)
   and the property procedures, so a label with no instance or a type with no property still appears with count 0;
   Memgraph's `schema.*` procedures are tried when Neo4j's `db.schema.*` are missing (the error code
   `ProcedureNotFound`, or the message).
6. **The rendering:** a node `{id, labels (sorted), properties}`, a relationship `{id, type, start, end, properties}`,
   a path the alternating list, temporal values in ISO text, a point `{srid, coordinates}`, bytes as base64; `id` is
   the driver's element id.
7. **A database that does not answer at boot is logged, not fatal** (the audit store's precedent): the tools register
   and answer 503 until it does. A URI that is not a Bolt URI, or one set without the driver installed, stops the boot
   by name.
8. **`file.extract`** returns `type` (`pdf`, `docx`, `html`, `text`) and the document's page count beside the text with
   its `--- page N ---` markers; a Word document is one page; `tools.file.extract.max.chars` (60 000) is the cap key
   the paper did not name; the tool registers only when both libraries of the `documents` extra import.

The driver's failures map to the tool's statuses by type: a client error (syntax, type, constraint, a write in a read
transaction) is a 400 carrying the database's code and message for the model; a transaction over the timeout a 408; an
unreachable database or a refused credential a 503, logged by type and never by message; the rest a 502. Driven live
on Neo4j 5 Community through the gated test: an ingest run twice leaves one node, a query renders node, relationship and
path, the schema carries the counts, a syntax error comes back by code, a missing endpoint is refused, the delete gate
holds.

## 15. Build notes (mercury-nodejs, 2026-11-17 - PR #130, step 3)

The Node twin follows the Python build's defaults (sections 13 and 14) and keeps the contract, the messages and the
catalog files byte for byte (the rows in the module are generated from the shared files). Three differences are visible
in a result, by the platform rather than by design: the HTML reader decodes the common named character references and
every numeric one (Node has no HTML parser in its standard library; Python's decodes all of HTML5's); a Word table
renders through mammoth's HTML (cells separated by spaces, rows as blocks) where Python joins cells with ` | `; and a
graph integer comes back as a JavaScript number (`disableLosslessIntegers`). Two implementation notes: the pinned
address is a custom `lookup` on `node:http` with the TLS server name from the URL, the same closure of DNS rebinding as
Python's pinned request; and pdfjs-dist is used directly for PDF text, since `pdf-parse` v2 pulls a native canvas
binding the tool does not need. Driven live on Neo4j 5 Community through the same gated sequence as the Python build;
the readiness wait must use the driver, because the Bolt port accepts connections before the server is up.

## 16. Build notes (all three repositories, 2026-11-18 - step 5)

The recipes page `docs/guides/tools-from-mcp-servers.md` here, with the packs' `extensions/mcp-binding/resources/mcp-recipes.yml`,
the shared digest vector `mcp-recipes.json` and the gated live test (mercury-python #66, mercury-nodejs #132). Seven
decisions made without a ruling: (1) the recipes live in a real server file of their own, `mcp-recipes.yml`, that the
binding does not load, rather than as commented entries of `mcp-servers.yml` - the gated test loads it with the binding's
own loader and a deployment copies an entry, while `mcp-servers.yml` keeps its two commented examples; (2) the entries pin
the package versions the digests were read from (`mcp-server-git==2026.11.11`, `@playwright/mcp@0.0.85` and so on), so a
bump is a deliberate re-run of `describe`; (3) the digests are pinned in one vector file shared byte for byte - 56 tools of
the six servers that list without a credential - and GitHub's are not pinned, since its listing needs a token: the page
says to run `describe` with the deployment's own; (4) the test gates on `MCP_RECIPES_LIVE` (the Neo4j server also on
`NEO4J_PASSWORD`), treats a missing or a changed pinned tool as rot and a tool the server added as not rot; (5) the
Filesystem recipe runs `--read-only` (ten tools) and the Neo4j recipe takes its connection from `NEO4J_*` variables rather
than arguments, since an argument is visible in the process list; (6) Playwright runs `--headless --isolated
--allowed-origins`, its actions are `write: true` on a graph and `browser_evaluate` and `browser_run_code_unsafe` are never
bound; (7) a row on the graph is written in the loop's closed vocabulary from the server's schema, never copied from it,
because several servers' schemas carry `$schema` or `anyOf`, which the loop refuses - the function in between adds what
the graph must not expose (a repository path from configuration). What the build found: the binding resolves `${}`
references in `args` as in `env` and `headers`, so a path placeholder works in an argument list; the two bindings print
identical digests for every tool, as the canonical form intends; the Fetch server's own description addresses a chat
assistant, the plainest case for the rule that the model reads the graph's row; the Git server's `--repository` takes a
path inside the repository and confines every tool to it; a credential reference without a default fails the load of the
whole server file, so the tests set placeholders for the credentials they never send.
