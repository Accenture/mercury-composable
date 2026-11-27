---
title: Built-in Agent Tools
summary: The essential tools an agent loop names in its tool catalog - files inside one workspace, the web
  inside an allowlist of sites, and a clock - served by the language packs' agent-tools extension as composable
  functions, each with a published catalog row, a sandbox that is deployment configuration and limits the tool
  enforces itself.
layer: knowledge-graph
audience: [developer, architect, ai-agent]
keywords: [agent tools, built-in tools, llm.run, tool catalog, file.read, file.write, file.list, file.search,
  web.fetch, web.crawl, time.now, tools.file.root, tools.web.allow, sandbox, RFC-0013, ADR-0036]
related:
  - guides/knowledge-graph/skills-reference.md
  - guides/polyglot-functions.md
  - guides/transcript-audit-schema.md
---

# Built-in Agent Tools

*Reference: the tools every second agent use case needs, where they live, how each is governed, and how an author
puts one on a graph.*

> **At a glance**
>
> - **What** — twelve composable functions in the language packs' `extensions/agent-tools/`: `file.read`,
>   `file.write`, `file.list`, `file.search` and `file.extract` inside one workspace; `web.fetch` and `web.crawl`
>   inside an allowlist of sites; `graph.schema`, `graph.query`, `graph.write` and `graph.ingest` inside one graph
>   database; `time.now`. A graph's `llm.run` node names them in its tool catalog like any other route.
> - **Why it matters** — the diagram assistant needed one tool; the next graphs want to read the file the person
>   pointed at, write a note, fetch a page. Without a shared set, every graph repeats the same security decisions:
>   which directory, which hosts, how big a result, what counts as a write.
> - **The rule** — the sandbox is deployment configuration, never an argument of a call; a tool whose sandbox is not
>   configured does not register; every refusal is a 400 naming the rule; results are text, capped and untrusted.
> - **Versions** — version 1: the file, web and clock tools (mercury-python #63) and the knowledge-graph tools with
>   `file.extract` (mercury-python #64), and the Node.js twin (mercury-nodejs #130), all 2026-11-17; the two packs
>   serve the same routes from the same catalog files, byte for byte. Accepted as
>   [ADR-0036](../arch-decisions/ADR.md#adr-0036) on 2026-11-18, after the live drives through both packs and the recipes for
>   existing MCP servers (raised as RFC-0013 in the [RFC register](../arch-decisions/RFC.md)).

## A built-in tool is a composable function

An agent loop ([`llm.run`](knowledge-graph/skills-reference.md#task)) calls the tools its catalog names through the
engine's ordinary routing: a route is local, an Event-over-HTTP target or on the mesh, and the loop neither knows nor
cares which. The built-in tools are functions of a pack extension, served next to the LLM helper that every agent
deployment already runs; the engine maps their routes in its `yaml.event.over.http` file and never contains tool
code. A route names the capability (`file.read`, `web.fetch`), never the implementation, so a built-in may later be
replaced by an MCP tool behind the same route, or an MCP tool by a built-in, and the graph does not change.

Six principles, from the design spec:

1. **A built-in tool is a composable function**, declared in the pack extension, addressed by a route.
2. **Each tool ships its catalog row** - the name the model sees, the description it reads, the input schema the loop
   validates against, and `write: true` where it applies - so an author copies a row rather than writing one.
3. **The sandbox is deployment configuration.** Which directory tree, which hosts: keys of the pack's configuration,
   certified with the deployment, never arguments of a call. An unconfigured tool does not register, so it is a 404
   to the loop's pre-flight check, not an open door.
4. **Results are text, capped and untrusted.** No binary leaves a tool; each caps its own result; a fetched page that
   says "ignore your instructions" is a page.
5. **Few, essential, boring.** No shell, no arbitrary HTTP request, no SQL against a system of record. A use case with
   another need writes its own route and certifies it on its graph.
6. **Build or bind, by rule.** Built in-house when the sandbox must be the pack's, when no second runtime may be
   needed, or when it is a few lines; bound from an existing MCP server (through the pack's MCP binding) when the
   server is a product's own front door - the recipes are on [Tools from MCP Servers](tools-from-mcp-servers.md).

## The set

Every tool refuses an argument outside its sandbox with a **400 that names the rule and the key that governs it**;
the loop returns it to the model as that call's error, and the node never fails because of a tool. A missing file is
a 404, a site that does not answer in time a 408, an existing file under `create` a 409, a host that cannot be
resolved a 502.

### Files, inside one workspace

The workspace is `tools.file.root`, one directory tree. Every `path` is relative to it, resolved with symbolic links
followed, and refused when the result is outside the root (`..`, an absolute path, a link that escapes). Hidden
entries (a name starting with `.`) are invisible to every file tool unless `tools.file.hidden=true`. The pack's
process should run as a user with no rights beyond the tree.

| Route | Row | Arguments | Result and limits |
| --- | --- | --- | --- |
| `file.read` | `file_read` | `path`; `max_bytes` (default and cap `tools.file.max.bytes`, 256 KiB) | `{path, bytes, text, truncated}`; UTF-8 text only, a binary file is an error naming the detected type |
| `file.write` | `file_write`, **`write: true`** | `path`, `content`; `mode` = `create` (default), `overwrite`, `append` | `{path, bytes, mode}`; `create` refuses an existing file; parent folders are created inside the root; `tools.file.write.extensions` may restrict the suffixes |
| `file.list` | `file_list` | `path` (a folder, default `.`); `glob` (`*.md`, `**/*.md`); `max_entries` (default 200, cap 2000) | `{entries: [{path, type, bytes, modified}], truncated}` |
| `file.search` | `file_search` | `pattern` (a literal, or a regular expression with `regex: true`); `path`; `glob`; `case_sensitive`; `context` (cap 3); `max_results` (default 100, cap 1000) | `{matches: [{path, line, text, before, after}], files_searched, truncated}`; binaries skipped, lines cut to `tools.file.line.max.chars` |
| `file.extract` | `file_extract` | `path`; `pages` (a range such as `1-3,7`); `max_chars` (default and cap `tools.file.extract.max.chars`, 60 000) | `{path, type, pages, text, truncated}`; a PDF or Word document (the pack's `documents` extra), an HTML or a text file, with a `--- page N ---` marker before each page; registers only when the extra is installed |

### The web, inside an allowlist

The web tools register only when `tools.web.allow` names at least one host pattern (`docs.example.com`,
`*.example.org`). A URL is fetched only when its host matches, over `https` (an `http` host must be listed with its
scheme), and every redirect is checked against the same list. The host is resolved before the request, every address
it resolves to is checked, and the request is made to the checked address; addresses in loopback, private,
link-local, multicast and reserved ranges are refused unless `tools.web.allow.private=true` - the request-forgery
route to a cloud metadata service or an internal admin port is closed by default. The download stops at
`tools.web.max.bytes` (1 MiB), the request at `tools.web.timeout.ms` (10 s), and the tool identifies itself with a
Mercury user agent.

| Route | Row | Arguments | Result and limits |
| --- | --- | --- | --- |
| `web.fetch` | `web_fetch` | `url`; `as` = `text` (default), `markdown`, `raw` | `{url, status, content_type, title, text, bytes, truncated}`; HTML reduced to readable text (scripts, styles and navigation dropped; headings, lists, code and links kept as text or Markdown); a text resource as is; `raw` the body as received; the page's own status is a result |
| `web.crawl` | `web_crawl` | `url`; `max_pages` (default 10, cap `tools.web.crawl.max.pages`, 50); `max_depth` (default 2, cap 5); `include` (a path prefix) | `{pages: [{url, title, text, bytes}], visited, truncated}`; same host, breadth-first, `robots.txt` honoured, `tools.web.crawl.delay.ms` between requests, each page cut to `tools.web.page.max.chars`, the whole result to fit the loop's result limit |

The crawl is a bounded reader of a documentation section or a small site, not a spider: a model that needs more asks
again with a narrower start page or an `include` prefix.

### The knowledge graph, inside one database

The agent's own store for what it extracts (principle 7: extract once, query many): documents ingested once, their
entities, relations and rules queried by every later agent instead of scanning the files again. The sandbox is one
database, `tools.graph.uri` (a Bolt URI), `tools.graph.database` and the credential by reference; the query language is
openCypher, which the models know and which Neo4j, Memgraph, FalkorDB, Apache AGE and Amazon Neptune speak; the
reference backend is the official Bolt driver (the pack's `graph` extra), which reaches Neo4j and Memgraph alike. The
reads register when the URI is set; the writers only when `tools.graph.write=true`; `DELETE`, `DETACH DELETE` and
`DROP` are refused unless `tools.graph.delete=true` as well. Every statement runs in a managed transaction with
`tools.graph.timeout.ms` set server-side, a read in a read transaction.

| Route | Row | Arguments | Result and limits |
| --- | --- | --- | --- |
| `graph.schema` | `graph_schema` | none | `{labels: [{label, count, properties: [{name, types}]}], relationship_types: [{type, count, from, to, properties}]}` from the database's own schema procedures (Neo4j's `db.schema.*`, else Memgraph's `schema.*`) and two counting queries - what the model reads before it writes a query |
| `graph.query` | `graph_query` | `query` (openCypher, read-only); `parameters` | `{columns, rows, truncated}`; refused before sending when it carries `CREATE`, `MERGE`, `SET`, `DELETE`, `REMOVE`, `DROP` or `LOAD CSV` outside a string or a comment; rows capped by `tools.graph.max.rows` (200, cap 2000); a node `{id, labels, properties}`, a relationship `{id, type, start, end, properties}`, a path the list of its nodes and relationships |
| `graph.write` | `graph_write`, **`write: true`** | `query`; `parameters` | `{counters: {nodes_created, nodes_deleted, relationships_created, relationships_deleted, properties_set, labels_added, labels_removed, indexes_added, constraints_added}, columns, rows, truncated}` from a write transaction |
| `graph.ingest` | `graph_ingest`, **`write: true`** | `nodes: [{key, label, properties}]`; `relationships: [{from, to, type, properties}]`; `source` | `{nodes, relationships}`: one parameterized `MERGE` per node on `(:_Ingested:Label {key})` with `SET n += properties`, one per relationship between two keys that exist (in this call or already in the graph; a missing key is a 400 and nothing is written), each end found by the index on `(:_Ingested {key})`, `source` stamped on each; a key names one node, so the same key under another label is a 400; labels and types `^[A-Za-z][A-Za-z0-9_]{0,63}$`, values scalars or lists of scalars, at most `tools.graph.ingest.max.nodes` (500) nodes per call; running it twice changes nothing |

Why `graph.ingest` beside `graph.write`: a list of nodes and relationships is what a model extracts well, and a long
`MERGE` with dozens of parameters is where Cypher goes wrong. The tool makes the Cypher, no value is ever
interpolated into a statement, and the write is idempotent. The row's description tells the model the conventions: a
key unique across the graph, prefixed with the label in lower case (`clause:12`, `party:supplier-a`); a `source` on
every ingest so an answer can cite it; the labels of the domain the graph author names in the system prompt. Both
writers carry the mark: under the node's default mode a person approves each, and an ingestion graph declares
`mode: auto` on its ingest node - the product owner's decision at design time, visible in the graph's certification.

**The ingest label and its index.** Every node `graph.ingest` writes carries the label `_Ingested` beside its own,
and one uniqueness constraint, `mercury_ingested_key` on `(:_Ingested {key})`, gives the index that finds a node by
its key. A database index belongs to a label, so a match on `key` alone reads every node of the database: on a graph
of 180,000 nodes, 150 relationships took 9.6 s that way and take about 30 ms through the label. A caller's label
starts with a letter, so it can never be `_Ingested`; `graph.schema` leaves the label out, a query result shows it
among a node's labels, and the `graph_query` row tells the model to find a node by key as
`MATCH (n:_Ingested {key: $key})`. The constraint makes the key unique across the graph, as the row says: the same
key under another label is refused, and two ingests of one key at the same moment merge into one node. The first
ingest of a process prepares the graph, each step in its own transaction: when the constraint is missing, every node
that has a `key` takes the label, so a graph written before the label existed is carried over, and then the
constraint is created. Three things stop it before anything is merged, each naming its repair: a key on more than
one node (409, with the query that lists them), a database user that may not create a constraint (503, with the
statement an administrator runs once), and a labelling that does not finish within `tools.graph.timeout.ms` (408,
with the statement to run in batches). A node written by `graph.write` is an end of an ingested relationship when it
carries the label and a key. On a database that does not take Neo4j's constraint commands, such as Memgraph, the tool
labels and matches by the label, logs one warning, and leaves the index to the installation; each pack's README has
the statements.

### The clock

`time.now` (`time_now`) takes an optional `timezone` (an IANA name, default `UTC`) and returns
`{utc, local, timezone, offset, weekday, epoch_ms}`. A model has no clock, and "today", "within 30 days" and "last
quarter" need one. It reads nothing and registers unless `tools.time=false`.

## Putting a tool on a graph

Three things, none of them engine code:

**1. The pack's configuration** names the sandbox - two keys for the common case, reviewed with the deployment:

```bash
mercury-serve extensions/agent-tools/agent_tools.py -Dtools.file.root=/srv/workspace -Dtools.web.allow=docs.example.com
```

**2. The engine's Event-over-HTTP map** reaches the pack's routes:

```yaml
event:
  http:
    - route: 'file.read'
      target: 'http://127.0.0.1:8089/api/event'
    - route: 'web.fetch'
      target: 'http://127.0.0.1:8089/api/event'
    # ... one entry per route the graph names; the map takes no comma-separated list
```

**3. The catalog node** carries the rows. Copy them from the extension's `catalog/` folder (`file_read.json`, ...) or
from `python agent_tools.py describe`, which prints every row and says which tools register under the current
configuration; the loop's pre-flight check then certifies what the graph names:

```text
create node research-tools
with type ToolCatalog
with properties
tools='''[
  { "name": "web_fetch", "route": "web.fetch", "description": "...", "input_schema": { ... } },
  { "name": "file_write", "route": "file.write", "write": true, "description": "...", "input_schema": { ... } }
]'''
```

The row's description is what the model reads about the tool; the shipped text is a reviewed default an author may
sharpen for the graph's domain. The input schema is a closed object (`additionalProperties: false`) in the loop's
[schema subset](knowledge-graph/skills-reference.md#task); the tool refuses an unknown argument by name as well. No
shipped row sets `strict` - every row has optional properties, and the graph's author adds the flag when the model in
use constrains them by grammar.

## Governance

- **The catalog is still the boundary.** A graph names the tools its agent reaches; a built-in the graph does not
  name is as unreachable as any route. The pre-flight check verifies the routes exist, so a deployment whose sandbox
  is not configured fails the graph before a token is spent.
- **Writes are marked.** `file.write`, `graph.write` and `graph.ingest` carry `write: true`; under the node's default
  mode a person approves each write, and only a node that declares `mode: auto` runs it unasked (ADR-0033). The other
  tools are reads, bounded by the root, the allowlist, the read transaction and the caps.
- **A wait reaches a person through [Ask a Person](ask-a-person.md).** A marked write under the default mode and the
  ask row (`ask: true`, no route - the model's question for a person) both hold the turn; the graph's hold path calls
  `person.ask` before it suspends, and the person answers through the resume link. The loop spec's section 7.3 is the
  ask row's contract.
- **The root, the allowlist and the database are certification artifacts.** They answer "what can this agent reach?"
  without reading code, and they live with the pack's configuration, not in a prompt or a call. The graph has two
  gates: writing needs `tools.graph.write=true`, deleting `tools.graph.delete=true` as well.
- **Untrusted content.** A page or a file may carry instructions; the loop never obeys a result, and the graph
  author's system prompt should say that fetched and stored content is data.
- **Logs.** A tool logs its route, the status, the byte, entry or page count and the host of a URL - never a path's
  content, a page's text or a full URL (a query string may carry a token). The engine's trace names the route like
  any function's.
- **Limits are defaults a deployment lowers, never raises past the caps** named above; the caps are the tool's.

## Configuration (the pack's)

| Key | Default | Meaning |
| --- | --- | --- |
| `tools.file.root` | unset | the workspace; unset, the file tools do not register |
| `tools.file.max.bytes` | `262144` | the most bytes `file.read` returns and `file.write` accepts |
| `tools.file.hidden` | `false` | expose hidden entries to the file tools |
| `tools.file.write.extensions` | any | the suffixes `file.write` accepts (`.md, .txt`) |
| `tools.file.line.max.chars` | `500` | the longest line `file.search` returns |
| `tools.web.allow` | empty | the host patterns the web tools may reach; empty, they do not register |
| `tools.web.allow.private` | `false` | allow loopback, private, link-local and multicast addresses |
| `tools.web.max.bytes` | `1048576` | the most bytes read from one response |
| `tools.web.timeout.ms` | `10000` | how long one request may take |
| `tools.web.crawl.max.pages` | `50` | the most pages one crawl may ask for (the cap; a deployment may lower it) |
| `tools.web.crawl.delay.ms` | `500` | the pause between two requests of a crawl |
| `tools.web.page.max.chars` | `20000` | the characters kept of a page's text |
| `tools.file.extract.max.chars` | `60000` | the most characters `file.extract` returns |
| `tools.graph.uri` | unset | the graph database, a Bolt URI; unset, the graph tools do not register |
| `tools.graph.database`, `tools.graph.username`, `tools.graph.password` | unset | the database and the credential, by reference |
| `tools.graph.write` | `false` | register `graph.write` and `graph.ingest` |
| `tools.graph.delete` | `false` | allow `DELETE` and `DROP` in `graph.write` |
| `tools.graph.max.rows` | `200` | the most rows a query returns (the cap is 2000) |
| `tools.graph.timeout.ms` | `10000` | how long one transaction may take, set server-side |
| `tools.graph.ingest.max.nodes` | `500` | the most nodes one `graph.ingest` call may carry |
| `tools.time` | `true` | register `time.now` |

## The two packs

The Python pack (`extensions/agent-tools/agent_tools.py`) and the Node.js pack (`extensions/agent-tools/agent-tools.mjs`)
serve the same twelve routes with the same rows, limits, statuses and messages; the catalog files under each pack's
`catalog/` folder are byte for byte the same, pinned by a test in each. A deployment runs whichever pack already hosts
its `llm.chat`. Three differences are visible in a result and recorded in the packs' memory: the Node reader decodes
the common named HTML character references and every numeric one where Python's decodes all of HTML5's; a Word table
renders through mammoth's HTML in Node (cells separated by spaces) where Python joins cells with ` | `; and a graph
integer comes back as a JavaScript number in Node. What a model extracts belongs in the knowledge graph; what it said
belongs in the [transcript audit store](transcript-audit-schema.md).

## What comes next

- **The Playground examples are built, token-free:** `site-notes` (a crawl, then a supervised write of the notes into
  the pack's workspace), `document-ingest` (a document of the synthetic corpus extracted into the knowledge graph by a
  node that declares `mode: auto`) and `document-query` (the graph read, the sources cited), with their catalog rows
  copied from the packs' catalog files and `AgentToolsGraphsTest` on scripted stand-ins for the model and the tools
  (`examples/minigraph-playground`, its `graph/README.md` has the protocols), and **driven live through both packs**
  on Haiku 4.5 - the record is `docs/test-reports/agent-tools-live-drive.md`. The recipes for binding existing MCP servers - Git, Time, Fetch,
  Filesystem, Neo4j's Cypher server, GitHub's and Playwright's - are on [Tools from MCP Servers](tools-from-mcp-servers.md)
  (step 5, 2026-11-18), with the digests read from the pinned server versions and a gated test in each pack that
  re-reads them.

## See also

- [Built-in skills reference](knowledge-graph/skills-reference.md) — `graph.task` and the agent loop's node.
- [Tools from MCP Servers](tools-from-mcp-servers.md) — binding an existing server's tool behind a route of your own.
- [Ask a Person](ask-a-person.md) — how a marked write or an ask row reaches a person, and how the decision comes back.
- [Polyglot Functions](polyglot-functions.md) — how the engines reach a pack's functions over Event-over-HTTP.
- [Transcript Audit Schema](transcript-audit-schema.md) — the record of what the model was told and said.
- The pack's own guide, `extensions/agent-tools/README.md` in mercury-python — the function contract, the
  configuration and the tests.
