# Essential built-in tools for agent loops — files and the web, governed — design spec

**Status:** DRAFT for review, round 1 (2026-11-16, Claude Code for Eric). Raised as RFC-0013 in
[`docs/arch-decisions/RFC.md`](../docs/arch-decisions/RFC.md); the rulings this paper needs are listed in section 9.
**Serves:** `bp-agent-orchestration` → `vision-mercury-composable` (Track 2, the graph as AI SDLC).
**Extends:** [`agent-tool-use.md`](./agent-tool-use.md) (a tool is a composable function; a route names the capability,
never the transport) and [`llm-helper-agent-loop.md`](./llm-helper-agent-loop.md) section 5 (the tool catalog) and
section 10 (security).
**Repo scope:** a new extension in the packs, `extensions/agent-tools/` (mercury-python first, mercury-nodejs in
lockstep), each tool with its published catalog row; a Playground example graph here. No engine code.

---

## 1. The problem

Every agent use case built so far needed tools that were not about the use case: the diagram assistant needed one
remote tool and an engine-side wrapper to keep its payload out of band; the next graphs will want to read a file the
person pointed at, write a note or a report, fetch a page, or read a documentation section. Today an author writes a
function per tool, or binds a reference MCP server through the binding. Both work, and both repeat the same security
decisions in every graph: which directory, which hosts, how big a result, what counts as a write.

Eric's direction (2026-11-16): identify and build the built-in tools that are essential for common cases, reading and
writing local files and crawling web content among them. This paper names a first set, says where they live, how each
is governed, and how an author puts one on a graph.

## 2. Principles

1. **A built-in tool is a composable function**, declared with `preload` in a pack extension, addressed by a route that
   names the capability (`file.read`, `web.fetch`). The loop, the catalog and the graph see nothing special about it; a
   built-in may later be replaced by an MCP tool behind the same route, or an MCP tool by a built-in, as
   `agent-tool-use.md` promises.
2. **Each tool ships its catalog row.** The name, the description the model reads, the input schema the loop validates
   against, `strict`, and `write: true` where it applies, are published with the tool, so an author copies a row onto
   the graph rather than writing one; the loop's pre-flight check still certifies what the graph names.
3. **The sandbox is deployment configuration.** Which directory tree the file tools may touch and which hosts the web
   tools may reach are keys of the pack's configuration, certified with the deployment like the MCP server file, never
   arguments of a call. A tool whose sandbox is not configured does not register, so an unconfigured tool is a 404 to
   the loop's pre-flight check, not an open door.
4. **Results are text, capped, and untrusted.** A tool returns text or a small map; it never returns a binary; it caps
   its own result below the loop's `result_limit`; and what it returns is data a model reads, never an instruction the
   loop obeys. A fetched page that says "ignore your instructions" is a page.
5. **Few, essential, boring.** The set is what every second use case needs, with the dangerous generalities left out:
   no shell, no arbitrary HTTP request, no database query. A use case with such a need writes its own route and
   certifies it on its graph.

## 3. Shapes considered

**A. The reference MCP servers through the binding** (the filesystem server, the fetch server). Zero code and already
possible; set aside as the default because each server is a child process with its own runtime, its schemas and
descriptions are the server's (the author rewrites them on the graph anyway), and the controls differ: the filesystem
server takes allowed directories, the fetch server takes no host allowlist at all. An installation that prefers a
server binds it; the routes are the same.

**B. Engine-side functions** in `extensions/agent-loop` (Java, and Rust in lockstep). Available without a pack
process; set aside because an agent deployment always runs a pack for `llm.chat`, the web-content libraries are weaker
there, and two implementations must be kept in lockstep for every tool.

**C. A pack extension, `extensions/agent-tools/`** (chosen). One implementation per pack beside the helper that every
agent deployment runs, the richest libraries for the web, and the packs' lockstep discipline already in place. The
extension is an application on the pack, like the helper and the binding, not part of the package.

**D. A generic `http.request` tool.** Rejected: a URL the model chooses is the server-side request forgery the web
tools exist to prevent; an API the use case needs gets a wrapper route with its own schema, certified on the graph.

## 4. The set (version 1)

Five tools, two groups. Every tool refuses out-of-sandbox input with a 400 that names the rule, which the loop returns
to the model as that call's error; the node never fails because of a tool.

### 4.1 Files, inside one workspace

The workspace is `tools.file.root`, one directory tree. Every `path` is relative to it, resolved with symbolic links
followed, and refused when the result is outside the root (`..`, an absolute path, a link that escapes). The pack's
process should run as a user with no rights beyond that tree.

| Route | Row | Input | Result |
| --- | --- | --- | --- |
| `file.read` | `file_read` | `path` (string, required); `max_bytes` (integer, optional, capped by `tools.file.max.bytes`, default 256 KiB) | `{path, bytes, text, truncated}`; UTF-8 text only, a binary file is an error naming the detected type |
| `file.write` | `file_write`, **`write: true`** | `path`, `content` (string), `mode` (`create` default, `overwrite`, `append`) | `{path, bytes, mode}`; `create` refuses an existing file; parent directories are created inside the root; `tools.file.write.extensions` may restrict the suffixes (default: any) |
| `file.list` | `file_list` | `path` (directory, default `.`), `glob` (optional, e.g. `**/*.md`), `max_entries` (default 200, cap 2000) | `{entries: [{path, type, bytes, modified}], truncated}`; hidden entries excluded unless `tools.file.hidden=true` |

`file.write` carries the write mark, so under the node's default mode a person approves it; `file.read` and
`file.list` are reads and run unasked. A `file.search` (text search across the workspace) is a natural sixth tool and
is listed in section 9 as a question rather than built unasked.

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
| `web.crawl` | `web_crawl` | `url` (the start page); `max_pages` (default 10, cap `tools.web.crawl.max.pages`, 50); `max_depth` (default 2); `include` (an optional path prefix the crawl stays under) | `{pages: [{url, title, text, bytes}], visited, truncated}`; same host only, links followed breadth-first, `robots.txt` honoured, `tools.web.crawl.delay.ms` between requests (default 500), each page's text cut to `tools.web.page.max.chars` (default 20 000) |

The crawl is a bounded reader of a documentation section or a small site, not a spider: the page cap, the depth, the
include prefix and the delay are all limits, and the whole result is cut to fit the loop's `result_limit`, so a model
that needs more asks again with a narrower start.

### 4.3 Dependencies

The packs already depend on an HTTP client (aiohttp in Python; Node's built-in `fetch`), so the web tools add no
client. HTML to text is written in-house in each pack, as the OTLP encoder and the MessagePack codec were: a small
tokenizer that drops `script`, `style`, `nav`, `header` and `footer`, keeps the text of the rest, and renders headings,
list items, paragraphs and links in Markdown. It is enough for documentation and articles, it has no dependency, and
it is tested on a fixture set of pages. A library (trafilatura, Readability) stays an option an installation may swap
in behind the same route if its pages need main-content extraction; ruling R3.

## 5. Governance and security

- **The catalog is still the boundary.** A graph names the tools its agent reaches; a built-in the graph does not name
  is as unreachable as any route. The pre-flight check verifies the routes exist, so a deployment whose sandbox is not
  configured fails the graph before a token is spent.
- **Writes are marked.** `file.write` is the only writer and carries `write: true`; the node's mode decides whether a
  person approves it. `web.fetch` and `web.crawl` are reads of the outside, bounded by the allowlist.
- **The allowlist and the root are certification artifacts.** They live in the pack's configuration with the server
  file, are reviewed with the deployment, and answer "what can this agent reach?" without reading code.
- **Untrusted content.** A page or a file may carry instructions; the loop never obeys a result (section 10 of the loop
  spec), and the graph author's system prompt should say that fetched content is data. The tools add nothing to a
  result but what they read.
- **Logs.** A tool logs the route, the status, the byte count and the host of a URL; never the path's content, the page
  text or the full URL (a query string may carry a token).
- **Limits are defaults a deployment lowers, never raises past the caps** named above; the caps are the tool's.

## 6. Publishing the rows

Each tool module carries its row as a constant, and the extension has a `describe` command, as the binding does,
that prints every row as the JSON a catalog node takes:

```bash
uv run mercury-serve extensions/agent-tools/agent_tools.py --describe
```

The same rows are kept as files under `extensions/agent-tools/catalog/` (`file_read.json`, ...) for a copy without
running anything, and the engine's documentation gains a page listing them with their limits, so an author reads the
contract where the catalog syntax is documented. The rows are shared byte for byte between the packs, with a test
that pins them, as the vector files are.

## 7. The example graph

`site-notes` in the Playground: the person names a documentation section and a topic; the agent crawls the section
(the allowlist names the documentation host), reads what it needs, writes `notes/<topic>.md` into the workspace, and
returns the summary with the file's path. It exercises a read of the outside, a supervised write (the node's default
mode holds `file_write` for a person, so the graph suspends and resumes as `order-assistant` does), and the
transcript audit of RFC-0012 if that build lands first. A token-free test drives it on a scripted `llm.chat` and a
fixture web server; a live drive runs it on Haiku 4.5 against a public documentation site.

## 8. Consequences

- **For the packs:** a new extension with five routes, a configuration block, an in-house HTML-to-text, a fixture web
  server for the tests, a README and the catalog files; no new dependency; lockstep as always.
- **For the engines:** none in code. One documentation page, and the example graph with its test.
- **For an author:** a tool is a row copied from the catalog folder and an Event-over-HTTP entry for the pack's host;
  the sandbox is two configuration keys on the pack.
- **For the binding:** unchanged. An installation that prefers the reference MCP servers binds them to the same routes
  and the graphs do not change.

## 9. Rulings needed (Eric)

- **R1 — the set.** The five above, or the five plus `file.search` (a text search across the workspace, read-only)?
- **R2 — where.** The pack extension `extensions/agent-tools/`, Python first, Node in lockstep?
- **R3 — HTML to text.** In-house with no dependency, as proposed, or a library from the start?
- **R4 — registration.** The file tools register only with `tools.file.root` set and the web tools only with
  `tools.web.allow` set, so an unconfigured tool is a 404 to the pre-flight check?
- **R5 — `file.write`.** `create` as the default mode, refusing an existing file unless `overwrite` or `append` is asked?
- **R6 — the crawl's defaults and caps.** 10 pages, depth 2, 500 ms between requests, 50 pages at most?
- **R7 — the example.** `site-notes` as described, with its supervised write and its live drive?

## 10. Build plan

1. **Python:** `extensions/agent-tools/` with the five tools, the sandbox checks, the HTML-to-text, the `describe`
   command, the catalog files, the fixture web server and the tests, the README. The engines' documentation page.
2. **Node in lockstep:** the same routes, rows and tests; the catalog files byte-identical and pinned.
3. **The Playground:** the `site-notes` graph, its token-free test, the Event-over-HTTP entries, the README; then the
   live drive and its report under `docs/test-reports/`. Then promotion of RFC-0013 is Eric's call.
