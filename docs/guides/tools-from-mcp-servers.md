---
title: Tools from MCP Servers
summary: Recipes for giving an agent loop a tool that an existing MCP server already provides - the reference
  servers Git, Time, Fetch and Filesystem, Neo4j's Cypher server, GitHub's hosted server and Playwright's
  browser - each as a server-file entry for the language packs' MCP binding, the input-schema digests the
  binding pins, the bind lines and a catalog row rewritten for the graph, with the write marks and the
  sandbox each server enforces.
layer: knowledge-graph
audience: [developer, architect, ai-agent]
keywords: [MCP, MCP server, mcp-binding, bind, schema digest, tool catalog, llm.run, Git, Time, Fetch,
  Filesystem, Neo4j, GitHub, Playwright, recipes, mcp-recipes.yml, RFC-0013, ADR-0036]
related:
  - guides/agent-tools.md
  - guides/knowledge-graph/skills-reference.md
  - guides/polyglot-functions.md
---

# Tools from MCP Servers

*Recipes: when an agent loop needs a capability an existing MCP server already provides, how to put that server's
tool on a graph through the language packs' MCP binding, with nothing added to the engine and one digest pinned per
tool.*

> **At a glance**
>
> - **What** — seven recipes, one per server: Git, Time, Fetch and Filesystem (the maintained reference servers),
>   Neo4j's Cypher server (the vendor's alternative to the built-in graph tools), GitHub's hosted server and
>   Playwright's browser (vendor servers). Each is an entry of the packs' `extensions/mcp-binding/resources/mcp-recipes.yml`,
>   the digests `describe` printed, the `bind` lines and a catalog row rewritten for the graph.
> - **The rule** — build when the sandbox must be the pack's or the capability is a few lines; bind when the server is
>   a product's own front door. The [built-in tools](agent-tools.md) cover files, the web, the knowledge graph and the
>   clock; these recipes cover what they do not, and name the alternative where the two overlap.
> - **Governance is unchanged** — the model reads the graph's row, never the server's description; a tool that
>   changes anything is `write: true` on its row and waits for a person under the node's default mode; the binding
>   refuses to boot when a server changes a tool's schema under the graph.
> - **Kept honest** — a gated test in each pack starts the servers from the recipes file and checks the digests
>   (`MCP_RECIPES_LIVE=1`; GitHub's with `GITHUB_MCP_TOKEN`). The 95 digests here were read on 2026-11-18 from the
>   versions the entries pin, through both packs; a changed digest is the recipe rotting, caught at the test or at the boot.

## Build or bind

The sixth principle of the built-in tools, applied: a capability is **built** into the packs' `agent-tools`
extension when its sandbox must be the pack's (a workspace root with a write mark, a host allowlist, row caps and a
delete gate), when the pack's process must not depend on a second runtime, when the contract must be portable across
backends, or when it is a few lines; it is **bound** from an existing MCP server when that server is a product's own
front door, maintained by its vendor, with the surface that would be written anyway. Its reads run unasked and its
writes are approved under the node's mode like any other tool's.

| The need | Built in | Bound, by a recipe below |
| --- | --- | --- |
| Files inside one workspace | `file.read`, `file.write`, `file.list`, `file.search`, `file.extract` | the Filesystem server, as an alternative (read-only recipe) |
| A page, or a section of a site | `web.fetch`, `web.crawl` | the Fetch server, as an alternative to `web.fetch` alone |
| The agent's knowledge graph | `graph.schema`, `graph.query`, `graph.write`, `graph.ingest` | Neo4j's Cypher server, as an alternative without an ingest |
| The clock | `time.now` | the Time server, for zone conversion and relative times |
| A repository's history and state | — | the Git server |
| A code forge | — | GitHub's hosted server |
| A browser | — | Playwright's server |

## How a binding works

Four lines, from the packs' MCP binding ([ADR-0033](../arch-decisions/ADR.md); the function-per-tool shape of
2026-11-16):

1. **A server-file entry** in the pack's `resources/mcp-servers.yml`: the transport (`stdio`, a subprocess of the
   pack's process, or `http`, a streamable HTTP endpoint), the command or the URL, and every credential as a `${NAME}`
   reference resolved from the pack's environment at boot and never logged. `${NAME:default}` carries a default.
2. **`describe`** connects to a server, lists its tools and prints, for each, the SHA-256 of its input schema in
   canonical JSON, the `bind` line to paste and a catalog row to review: `python extensions/mcp-binding/mcp_binding.py
   describe git` in mercury-python, `node extensions/mcp-binding/mcp-binding.mjs describe git` in mercury-nodejs.
3. **A function that binds the tool.** `bind(server, tool, digest)` returns the tool as an async callable; an ordinary
   `preload` function on a route of your choosing calls it, shaping the arguments on the way. The route names the
   capability (`repo.log`), never the transport, so the same route may later be served another way.
4. **A catalog row on the graph** with the description rewritten for the use case and `write: true` where the tool
   changes anything, plus an Event-over-HTTP entry on the engine for the route (one route per entry).

At boot the binding verifies every declaration and refuses to start, by name, when a bound server is not configured,
a tool is no longer offered or its digest changed. The engine's loop then treats the route like any other: the
pre-flight check finds it served, the node's mode governs its writes, and every result returns to the model as text.

```python
# mercury-python, extensions/mcp-binding/mcp_binding.py (or an application that imports it)
git_log = bind("git", "git_log", schema_sha256="c4e5907dca6fbbbb7ba130e7ab57445f91d7c1df0d67f01a610940176b3c708e")

@preload(route="repo.log", instances=10)
async def repo_log(_headers: dict[str, str], body: Body):
    arguments = dict(body) if isinstance(body, dict) else {}
    return await git_log({"repo_path": REPOSITORY, **arguments})   # the path is configuration, never the model's
```

```js
// mercury-nodejs, extensions/mcp-binding/mcp-binding.mjs
const gitLog = bind('git', 'git_log', { schemaSha256: 'c4e5907dca6fbbbb7ba130e7ab57445f91d7c1df0d67f01a610940176b3c708e' });
preload('repo.log', { instances: 10 }, async (_headers, body) => gitLog({ repo_path: REPOSITORY, ...body }));
```

## Reading a recipe

Each recipe gives the entry as `mcp-recipes.yml` holds it (nothing in that file is loaded by default; an entry is
copied into `mcp-servers.yml`), the tools with the digests `describe` printed, which of them belong on a graph and how
each is marked, the sandbox the server itself enforces, and one catalog row as an example. The digests were read on
2026-11-18 from the pinned versions; the Python and the Node.js binding printed identical digests, as the shared
canonical form intends, and the live digests test of each pack re-reads them.

**The row is yours; the digest is the server's.** A row's `input_schema` is what the graph exposes to the model,
written in the loop's closed vocabulary (`type`, `properties`, `required`, `additionalProperties`, `items`, `enum`,
`const`, the numeric and length bounds, `pattern`; `title`, `description`, `default`, `examples` and `format`
documentary); the digest pins the server's schema, which several servers write with keywords the loop refuses
(`$schema`, `anyOf`). So a row is never a copy of `describe`'s output: `describe` prints the server's schema to review,
and the row is written from it, usually narrower - the function in between adds what the graph must not expose (a
repository path, a database name) and leaves out what the model must not choose.

## Git

The reference Git server (Python, `uvx`): the history and state of one repository, which `--repository` confines
every tool to. Bind it when a use case works on a repository - release notes from the log, a review of a diff, a
commit after approval. The function adds `repo_path` from configuration, so no graph names a path.

```yaml
git:
  transport: stdio
  command: uvx
  args: ['mcp-server-git==2026.11.11', '--repository', '${MCP_GIT_REPOSITORY:/srv/repository}']
```

| Tool | Digest (`schema_sha256`) | On a graph |
| --- | --- | --- |
| `git_status` | `7bd4de8036937dec083fd66e127b8e7ba67ec8e38453a3d20872e5eefafd6a48` | read |
| `git_diff_unstaged` | `b5f28e179d77d1f8132248f474d5d7e689ff385ac68eb29cacd14e0b3c93e593` | read |
| `git_diff_staged` | `a34503e2686d88a396e63b81ab6bff39b2ab9dbdca5de355ceab4e6b4a74399e` | read |
| `git_diff` | `32ee0e282fb126d18d4a4c6615b17defcaab9535409999d8625718408f57a8c4` | read |
| `git_log` | `c4e5907dca6fbbbb7ba130e7ab57445f91d7c1df0d67f01a610940176b3c708e` | read |
| `git_show` | `fa979031e75d98d89ad67bda75bdebe7069fbad3f5907c50cb0aafd8fc4c9ad1` | read |
| `git_branch` | `268f353022c7efb49a6e61f3bd3c81522910358facafa35c16fca0a5a305f693` | read |
| `git_remote` | `53800ff477e1de5e8a838b9eaa6b03f2d0b118320bdeae3d2d16e93b3ad20e79` | read |
| `git_add` | `fe607783e69161974a5f7a4ccd35c37a74701420facc8f99a153e9e1bd268a00` | `write: true` |
| `git_commit` | `c5cd9087b800e226a17190a2b110c7d9fe5a1131288faeb0f21478403293b967` | `write: true` |
| `git_reset` | `eea611c41ac2f0dac25fa3cc39a6b9692425bd6bc1de10c91e3ec81846c9707e` | `write: true` |
| `git_create_branch` | `9118849f78270629cb79f009849443a991834e4e6a1a6767671d8efd5306d7b8` | `write: true` |
| `git_checkout` | `b655a056a01c07675207106a974e3c18649ad97cfe5049bbc68b5f033a8ce4d6` | `write: true` |
| `git_merge` | `c26790c6086e8573806324d6f22714afb77481979c1bda9bc9295148b4cd96b9` | `write: true` |

A reading use case binds `git_log`, `git_show` and `git_diff`; one that commits adds `git_add` and `git_commit`, and
under the node's default mode the commit waits for a person with the message in view. The server runs as the pack's
process user, so the repository's file permissions are the second fence. The example row for the function above:

```json
{ "name": "repo_log", "route": "repo.log",
  "description": "The recent commits of the project repository, newest first: hash, author, date and message. max_count limits the list; start_timestamp and end_timestamp, ISO 8601 dates, bound it.",
  "input_schema": { "type": "object", "additionalProperties": false,
    "properties": { "max_count": { "type": "integer", "minimum": 1, "maximum": 50 },
                    "start_timestamp": { "type": "string" }, "end_timestamp": { "type": "string" } } } }
```

## Time

The reference Time server (Python, `uvx`): the current time in a zone, a conversion between zones and a time some
interval ago. The built-in `time.now` answers "now" in a few lines without a process; bind this server when a use case
converts between zones or reasons about relative times. `--local-timezone` fixes the server's default zone.

```yaml
time:
  transport: stdio
  command: uvx
  args: ['mcp-server-time==2026.11.11', '--local-timezone', '${MCP_TIME_ZONE:UTC}']
```

| Tool | Digest (`schema_sha256`) | On a graph |
| --- | --- | --- |
| `get_current_time` | `0fad5d2bdd1e327fb88f14add9d4052c3bbccf1f661cc78cd4261ac1f41788a4` | read |
| `convert_time` | `d4b3b9657b706c76a75371dccba7d10c119e8000c2edc03068c3676d7dc066cb` | read |
| `get_time_ago` | `5e938ffeb16607ee3cf0f6d1e4c9df7de60b37f372f6449e330647267f1fe6dc` | read |

```json
{ "name": "time_convert", "route": "time.convert",
  "description": "Convert a time of day between two IANA time zones, for example 14:30 from Europe/London to Asia/Singapore.",
  "input_schema": { "type": "object", "additionalProperties": false, "required": ["time", "source_timezone", "target_timezone"],
    "properties": { "time": { "type": "string", "pattern": "^[0-2][0-9]:[0-5][0-9]$" },
                    "source_timezone": { "type": "string" }, "target_timezone": { "type": "string" } } } }
```

## Fetch

The reference Fetch server (Python, `uvx`): one tool that fetches a URL and returns its content as Markdown, with
`robots.txt` honoured, private addresses refused by default and `--allowed-hosts` as the allowlist (repeat the option
for more hosts; `*.example.com` matches the subdomains). The built-in `web.fetch` and `web.crawl` cover the same
ground with the allowlist in the pack's configuration and no child process; bind this server when its Markdown output
or its readability extraction is wanted, for a single page at a time - the crawl has no counterpart here.

```yaml
fetch:
  transport: stdio
  command: uvx
  args: ['mcp-server-fetch==2026.11.11', '--allowed-hosts', '${MCP_FETCH_ALLOWED_HOST:docs.example.com}']
```

| Tool | Digest (`schema_sha256`) | On a graph |
| --- | --- | --- |
| `fetch` | `16fdd8e4c71db0b8693474bca52a6a1d81f6d862bb842ccead7287a220e1ef98` | read |

The server's own description of `fetch` addresses a chat assistant ("this tool now grants you internet access"),
which is exactly why the row carries the graph's description and the model never reads the server's. The server
returns `max_length` characters (default 5 000) and the model pages with `start_index`; the row below fixes the length.

```json
{ "name": "page_fetch", "route": "page.fetch",
  "description": "Fetch one page of the product documentation as Markdown. Pass start_index to continue a page that was cut.",
  "input_schema": { "type": "object", "additionalProperties": false, "required": ["url"],
    "properties": { "url": { "type": "string", "pattern": "^https://docs\\.example\\.com/" },
                    "start_index": { "type": "integer", "minimum": 0 } } } }
```

## Filesystem

The reference Filesystem server (Node.js, `npx`), started in read-only mode: the ten tools that read and list, each
confined to the directories named on the command line. The built-in `file.*` tools are the default for files - they
ship their rows and need no second runtime - and this server is the alternative when a Node.js runtime beside the pack
is acceptable and its surface is wanted: a directory tree as JSON, media files as content blocks, several files in one
call. Without `--read-only` the server also offers `write_file`, `edit_file`, `create_directory`, `move_file` and
`copy_file`, each `write: true` on a graph; `--read-only-dir <path>` protects one directory while the others stay
writable.

```yaml
filesystem:
  transport: stdio
  command: npx
  args: ['-y', '@modelcontextprotocol/server-filesystem@2.3.0', '--read-only', '${MCP_FILESYSTEM_ROOT:/srv/documents}']
```

| Tool | Digest (`schema_sha256`) | On a graph |
| --- | --- | --- |
| `read_text_file` | `3c51bad3f48b697f48fd24a1f310baf60fe906021ef15b38cd179ec856b635c6` | read |
| `read_file` | `3c51bad3f48b697f48fd24a1f310baf60fe906021ef15b38cd179ec856b635c6` | read (deprecated alias of `read_text_file`) |
| `read_media_file` | `0bf785608c87d1c51b81eb946f9e7801fff5c59647b4806820eeaa2419e698a3` | read |
| `read_multiple_files` | `e33001b8d3c59a392539941ea3ce0fb40beba8cbbe687f40d850eeee50515aa8` | read |
| `list_directory` | `b82677a25149a3481a3c05f4b769b1c6496911bac868335e54170e09c24d1e66` | read |
| `list_directory_with_sizes` | `c4e7d1e4396f0d78e5506c159a12b540e513da82dd8814ff3f7ae724fc86870d` | read |
| `directory_tree` | `69bdec6a46d6ec8303f47749ba940e28f0bf859ef07aa6a842cf3ac86c14a7cd` | read |
| `search_files` | `ad17e78560702ac5a0cbf3caf5ce0ff09ba13de8e0053642778e138b1bd3b444` | read |
| `get_file_info` | `341692883b0120e165caa3d5736d66b11710a7ff8a5f4dfbef0dbb822b1138b4` | read |
| `list_allowed_directories` | `93ab7499dc3c616f8db8780fed0d9f69270803cda913882ad2ef3943db8d7225` | read |

```json
{ "name": "documents_read", "route": "documents.read",
  "description": "Read a document of the policy folder as text, by its path relative to the folder. head or tail reads only the first or the last N lines.",
  "input_schema": { "type": "object", "additionalProperties": false, "required": ["path"],
    "properties": { "path": { "type": "string" }, "head": { "type": "integer", "minimum": 1 }, "tail": { "type": "integer", "minimum": 1 } } } }
```

## Neo4j's Cypher server

The vendor's server (Python, `uvx`): the schema, a read query and a write query against one Neo4j database, the
connection taken from the environment. It is the alternative to the built-in `graph.schema`, `graph.query` and
`graph.write` when the graph is Neo4j and the vendor's own surface is preferred; the built-in tools stay the default
because their rows are portable across the openCypher databases, `graph.schema` needs no plugin, and `graph.ingest`
has no counterpart here - a model using this server writes its `MERGE` statements itself. `get_neo4j_schema` needs the
APOC plugin in the database; `--read-only` leaves `write_neo4j_cypher` out of the listing.

```yaml
neo4j:
  transport: stdio
  command: uvx
  args: ['mcp-neo4j-cypher==0.6.0']
  env:
    NEO4J_URI: '${NEO4J_URI:bolt://127.0.0.1:7687}'
    NEO4J_USERNAME: '${NEO4J_USERNAME:neo4j}'
    NEO4J_PASSWORD: '${NEO4J_PASSWORD}'
    NEO4J_DATABASE: '${NEO4J_DATABASE:neo4j}'
```

| Tool | Digest (`schema_sha256`) | On a graph |
| --- | --- | --- |
| `get_neo4j_schema` | `51d2e5525a9bb54a5a06451a19eea2832e868b6c9c44637f29294e52c35b70ce` | read (APOC) |
| `read_neo4j_cypher` | `9631746823a32739ed4fa1dd5bd2bd04c93780ade4ba3b48aa9f3915b4069562` | read |
| `write_neo4j_cypher` | `9631746823a32739ed4fa1dd5bd2bd04c93780ade4ba3b48aa9f3915b4069562` | `write: true` |

The two query tools share one schema (`query` and `params`), so they share a digest; the row tells them apart, and
the write row carries the mark. The server runs the statement it is given, so a row for `read_neo4j_cypher` says what
the graph holds and that the statement must read, and the deployment adds `--read-only` where no write is wanted.

```json
{ "name": "kg_read", "route": "kg.read",
  "description": "Run a read-only openCypher statement against the contract knowledge graph (Document, Party, Obligation, Rule and Deadline nodes; PARTY_TO and STATED_IN relationships). Use $parameters for every value.",
  "input_schema": { "type": "object", "additionalProperties": false, "required": ["query"],
    "properties": { "query": { "type": "string", "maxLength": 4000 }, "params": { "type": "object" } } } }
```

## GitHub's server

The vendor's hosted server over streamable HTTP, reached with a fine-grained personal access token of least privilege
(or the deployment's GitHub App installation token). The `X-MCP-Toolsets` header selects the tool families the server
offers - the recipe's default, `repos,issues,pull_requests`, lists the 39 tools below; its documentation names the
others - and `X-MCP-Readonly: true` hides the seventeen that write, leaving the 22 reads. The listing needs the
credential: these digests were read on 2026-11-18 with a maintainer's fine-grained token through both packs'
`describe`, identical, and the packs' test checks them whenever `GITHUB_MCP_TOKEN` is set, in both modes. A
self-hosted alternative is the vendor's container image over stdio (`command: docker`, `args: ['run', '-i', '--rm',
'-e', 'GITHUB_PERSONAL_ACCESS_TOKEN', 'ghcr.io/github/github-mcp-server']`, `env: { GITHUB_PERSONAL_ACCESS_TOKEN:
'${GITHUB_MCP_TOKEN}' }`), with `--toolsets` and `--read-only` as arguments.

```yaml
github:
  transport: http
  url: 'https://api.githubcopilot.com/mcp/'
  headers:
    Authorization: 'Bearer ${GITHUB_MCP_TOKEN}'
    X-MCP-Toolsets: '${GITHUB_MCP_TOOLSETS:repos,issues,pull_requests}'
    X-MCP-Readonly: '${GITHUB_MCP_READONLY:true}'
  timeout_ms: 30000
```

| Tool | Digest (`schema_sha256`) | On a graph |
| --- | --- | --- |
| `get_commit` | `f084d5addb427982ea2cb3064e8c7faf23ef2117009b17707ca1f3dca2e08945` | read |
| `get_file_contents` | `3e8fd49a5ed6d35c07b9b3d712312ffe317391a11f7fdd185fe7c86ed8a241bd` | read |
| `get_label` | `cf89e3a5ff5d176d7be8375ae68fdde2bb90a85a524cb39037d0bf2d272fa474` | read |
| `get_latest_release` | `6bd7211e72d2921f609892e467a7b293cc3ed176557cc391f736361ea718e021` | read |
| `get_release_by_tag` | `9012396d62f53b04de455a466203e7c9ee6c56fa9f61063f28e08c147cb881f9` | read |
| `get_tag` | `8b13651d5e7162c741afd862aa15fa2a1419c539ebcba092bfd07ef1ee88a82a` | read |
| `issue_read` | `4129cf817f37afefbc65885a75ba37817ae95f46ab9658b0b9de8c37d6b26c2e` | read |
| `list_branches` | `cc4d347261dd31b2df9f0a36d7bd49a5acb4dba8043c857f70a5dfb74c6b7804` | read |
| `list_commits` | `591090af37df44793d29ce44be3afe0ec122e3ac2db6edc4e5db62d330b0a35c` | read |
| `list_issue_fields` | `ae44b8d0eadc50902f62a7907ab1c45dfbf95a71ec87ec4aa836fece8477c002` | read |
| `list_issue_types` | `fa18815229ec77ba88b9a249313ed41dd9e62382729cb72bc472587f1c06fb37` | read |
| `list_issues` | `ae3a1945a49d3648618475385a0d3647f107f57e40720f1889e2360b40a98d37` | read |
| `list_pull_requests` | `e1e093b013497fae6ca27152cc7d2ffed822ab389a5983e68773466bf83dc38b` | read |
| `list_releases` | `4118d959bb3b13749813d74442a4d143f9a2a0fad7ac5e23880fc5d1f1aace03` | read |
| `list_repository_collaborators` | `ee69edd2a4af5094260553c303fbc28db0a701ef28a9e93016aed005d6bc89a9` | read |
| `list_tags` | `cc4d347261dd31b2df9f0a36d7bd49a5acb4dba8043c857f70a5dfb74c6b7804` | read |
| `pull_request_read` | `380c0d04d38cb7ae46ac5862485e29277e353f7f7d868b558125e27be06017c1` | read |
| `search_code` | `4ad473c9570e3a825e6d60c9869b87324e0a5d371cf293ffb70fd2b840358cf7` | read |
| `search_commits` | `e477f9b9a99ce91db99e3380c7272485b0ee66cf06d9415674918bc6e8c159f7` | read |
| `search_issues` | `a055d2bd8d73374c84787996a5dae3934ef1570fda9c0aa1ad7061038bc0e12e` | read |
| `search_pull_requests` | `1d885a55314d8a1aff83ac5536600429ac3ae089d0f760335fa8ed559e99a9a1` | read |
| `search_repositories` | `7b4cad1aba3867b93477594b3054d4e6a3b2fb2fa09ba9b7e11cb42b034a7902` | read |
| `add_comment_to_pending_review` | `4db72d6ccc6cbf6c5a5a60db2269bdb78076bf2150838c932c4756fa16bc4667` | `write: true` |
| `add_issue_comment` | `d93b8edaabd74e3c655651f4fcd84fcb30d30bbd9b648cf3194c410faed2f282` | `write: true` |
| `add_reply_to_pull_request_comment` | `b3e21f358fb25d6e1e9b60c6b4133d50823f3401cb28a742b461b7f37bdc7951` | `write: true` |
| `create_branch` | `e65b73ec25a45f23ce7371d5a18b7d2bb749388c330c01f592f1c288016fc385` | `write: true` |
| `create_or_update_file` | `4bbfd9f8df3736e4897c2a0466b2131764d414c3075e3691711282eae7dc209e` | `write: true` |
| `create_pull_request` | `e249ccd5a1f2364cbfc0a5d9e11bebdc298626351cc7e43fd59b851c3d520238` | `write: true` |
| `create_repository` | `eb38a6e274e4b762d210740cdec57eacc9ebf0b5b87c013e32ee80406036f76b` | `write: true` |
| `delete_file` | `af29cf413864c65dee4ac821b10d15f9bb0673e2977d2298918b1317d160d4ba` | `write: true` |
| `fork_repository` | `ac52a51d95996c5af97cac680c7bc1b7dcc423061174a84c727dfd4a1d06b210` | `write: true` |
| `issue_write` | `083aa952baecd960494979902818962438474a84f1985252f3024597e9442f5f` | `write: true` |
| `merge_pull_request` | `d6b25b3006b7124dc4a1d775458febd3e802fe1dd9ff61347fc9bdb5e0176a02` | `write: true` |
| `pull_request_review_write` | `250438d8197b381022f562f96943dfab18a397fafaada3fdca3d3f18696fdf0b` | `write: true` |
| `push_files` | `63d80fecd0ff8d6b927f02faa499f858e13f79a008a6dfedaf04d8783194d51e` | `write: true` |
| `sub_issue_write` | `8b4d8c72ba5b500c5fed41a86b4720f0bbb58eac1f58abf576fbc00d9a90a88b` | `write: true` |
| `update_issue_comment` | `a4a9d37cda657866581bd1a8ca5eb7d6c1af6626d050361b1a3e4d1f63950441` | `write: true` |
| `update_pull_request` | `f9ef3dae6c8d529fa50f1ebd23b6fe351c0def794deb20787d4b55a9503fb9e1` | `write: true` |
| `update_pull_request_branch` | `48c3c0aa12992defea00c3727690442719367b1210187d5da589234bde3b62b6` | `write: true` |

A use case that reads a forge binds a few reads - `get_file_contents`, `search_code`, `list_issues` - and one that
files an issue or opens a pull request binds `issue_write` or `create_pull_request` with `write: true`, so the person
sees the title and the body before anything is created (`issue_write` creates and updates by its `method`, so one row
covers both). The token's scope is the first fence and the read-only header the second; the row's description names
the repositories the use case works on, and the function pins `owner` and `repo` from configuration where the graph
must not choose them. Several of the server's own descriptions instruct a chat assistant ("DO NOT use this tool",
"check with the user") - the graph's row says what the use case needs instead. `list_branches` and `list_tags` share a
schema and a digest, as do the two Neo4j queries; the row tells them apart.

```json
{ "name": "repo_file", "route": "repo.file",
  "description": "The contents of one file of the project repository, by its path, at a branch, tag or commit (ref; the default branch when omitted).",
  "input_schema": { "type": "object", "additionalProperties": false, "required": ["path"],
    "properties": { "path": { "type": "string" }, "ref": { "type": "string" } } } }
```

## Playwright's server

The vendor's browser automation server (Node.js, `npx`): a headless browser with an in-memory profile, driven through
accessibility snapshots. `--allowed-origins` is the allowlist of sites the browser may request (semicolon-separated
origins), `--isolated` keeps no profile on disk, `--output-dir` collects screenshots, and a secrets file for logins
stays with the server and never on a graph.

```yaml
playwright:
  transport: stdio
  command: npx
  args: ['-y', '@playwright/mcp@0.0.85', '--headless', '--isolated', '--allowed-origins', '${MCP_PLAYWRIGHT_ORIGINS:https://docs.example.com}']
  timeout_ms: 60000
```

| Tool | Digest (`schema_sha256`) | On a graph |
| --- | --- | --- |
| `browser_navigate` | `2165538e098634780eec628947d795a2619b4d2e3cef0e36d3084ac46abb94f7` | read |
| `browser_navigate_back` | `71fc596a20f27da3dec80050e5d1f553eba34aea24c2853b853488a811e6b892` | read |
| `browser_snapshot` | `1d7650f3b9f3612bbb4f1511c77944e7b9ed6e98c9e012926a243c58f016df6f` | read |
| `browser_find` | `d060308e6e1797f03317b2368c4d658b0013b0eb00c5ae7534db13b4528f0c3e` | read |
| `browser_take_screenshot` | `78dcb99cc226d741aa8c11a11ee0df62e15a9f9ab9db1a1ce4fdc5512d323d96` | read |
| `browser_console_messages` | `0d6cacf9f20123a6a4559c2bb05816020528d6529e6c1cb14520ea00fb9e23ef` | read |
| `browser_network_requests` | `e943ec891442cf3a071a3d090af243ca4e3db8924c7e95650672e47917d5da56` | read |
| `browser_network_request` | `f0dbfd6ba87b3b7739e5b0aa6fa8c0d981aa214128eb3c1a8891caa46bd60b30` | read |
| `browser_wait_for` | `cee58694f677034e21fc68446200888eb28469696b4ca9259047e15084fb22ff` | read |
| `browser_resize` | `4a80d1f511092cf5e94b689f81afd36ec42b0ac74a510f7220a7c9987af00b84` | read (browser-local) |
| `browser_emulate_media` | `3b4bcf539a7e561f0e7e233035d5e616d295c9fe0bd5d3c165120b305d2edaeb` | read (browser-local) |
| `browser_tabs` | `fc3f534e7428342371db86b438fad5f52abbe45d43b6e18fd79fb3d23de782c8` | read (browser-local) |
| `browser_close` | `71fc596a20f27da3dec80050e5d1f553eba34aea24c2853b853488a811e6b892` | read (browser-local) |
| `browser_hover` | `3b0a5a3333d051dba9f6b023c341eef00f0e5264bef37bc0a11f83156937e803` | read |
| `browser_click` | `7fed82f52ae7018670d967e4429b63501948c80de2df2e9279ce4c103694d0d3` | `write: true` |
| `browser_type` | `ed892740ebc14bb2a49467c08b8bb02717d50f1f3ef8ed06d51abf5ebb3249da` | `write: true` |
| `browser_fill_form` | `c991223a68247bd517c0fa1b61db444178ff6ad546e7214936d29b8de4c2f2ee` | `write: true` |
| `browser_select_option` | `03b097752bdb41817ec0a6a259062598e5c85c1a8c7106d0f2fde506321096bd` | `write: true` |
| `browser_press_key` | `57a7f701ebf865e5a7a759c15e8d04bca12cb756c2e3f4ae7280070bb6e310e2` | `write: true` |
| `browser_drag` | `3dcca018f0b0e9341fee3c91b5420f82557a59a9d9763699e90970b2bd3ed1cb` | `write: true` |
| `browser_drop` | `2f2a2c904744c935fc66547031c43b43466e86043f6c306624b536867b18d8e8` | `write: true` |
| `browser_file_upload` | `c76aede191ef1e3bdd09ff177419a1e77a0e1f3f676bdd7c8053ac78e6a80086` | `write: true` |
| `browser_handle_dialog` | `2d4cacf71e9fd7f902bebbc84f106614b891c2654de32b71b1f0d629a10d8b5c` | `write: true` |
| `browser_evaluate` | `04a3962dec15576f5ad84707db0251b84868e4d7dd4a5cf5e5e0ded786fc7d03` | never bound |
| `browser_run_code_unsafe` | `6a9943c16c5666a93c77010063b536f864936fc22606c48e7bb3a36b5b7fd444` | never bound |

The reads look at a page; the actions act on a site on someone's behalf, so each is `write: true` and waits for a
person under the default mode, with the target element and the value in view. `browser_evaluate` and
`browser_run_code_unsafe` run arbitrary code in the browser or in the server's process and are never bound; the
server's own description of the second says it is "RCE-equivalent". A reading use case binds `browser_navigate`,
`browser_snapshot` and `browser_find`; a form-filling one adds `browser_fill_form` and `browser_click`.

```json
{ "name": "site_snapshot", "route": "site.snapshot",
  "description": "The accessibility snapshot of the page the browser is on: every element with its role, name and reference. Pass depth to limit the tree.",
  "input_schema": { "type": "object", "additionalProperties": false,
    "properties": { "depth": { "type": "integer", "minimum": 1, "maximum": 20 } } } }
```

## Keeping a recipe honest

A recipe rots when the server it names changes a tool's schema, renames a tool or drops it. The binding catches that
at boot, by refusing to start with the server and the tool named; the recipes are checked before that, by a gated test
in each pack that starts every credential-free server from `mcp-recipes.yml` with `uvx` or `npx`, lists its tools
through the binding's own client and compares the digests with the pinned ones; GitHub's hosted server is listed the
same way when a token is in the environment, once under the read-only header (exactly the pinned reads) and once
without it (every pinned tool):

```bash
# mercury-python
MCP_RECIPES_LIVE=1 pytest tests/test_mcp_recipes.py
# mercury-nodejs
MCP_RECIPES_LIVE=1 node --test test/mcp-recipes.test.mjs
# both: the Neo4j server also wants NEO4J_PASSWORD and a database at NEO4J_URI; GitHub's wants GITHUB_MCP_TOKEN
```

The pinned digests live in `tests/vectors/mcp-recipes.json` (`test/vectors/mcp-recipes.json` in the Node.js pack),
one file shared byte for byte, and the test fails by name: `the git recipe rotted - missing [...], changed [...]`. The
fix is a review, never a blind re-pin: run `describe` on the server, read the new schema, decide whether the row on the
graph still says what the tool does, then update the digest next to the function, in the vector file and on this
page in one change. Without the live flag the tests still load the recipes file and check that every pinned server
has an entry, so a broken entry fails the ordinary suite.

## What comes next

- This page, the packs' `mcp-recipes.yml` and the gated digests test were step 5 of RFC-0013's build plan, the last
  step; the proposal was accepted as [ADR-0036](../arch-decisions/ADR.md#adr-0036) on 2026-11-18.
- The other toolsets of GitHub's server (actions, code security, discussions and the rest of its documentation) are
  listed with the same `describe` run under a different `X-MCP-Toolsets` value; a reviewed set may be added here.

## See also

- [Built-in Agent Tools](agent-tools.md) — the tools built into the packs, and the build-or-bind rule these recipes
  apply.
- [Built-in skills reference](knowledge-graph/skills-reference.md) — `graph.task` and the agent loop's node, whose
  catalog names the routes.
- [Polyglot Functions](polyglot-functions.md) — how the engines reach a pack's functions over Event-over-HTTP.
- The packs' own guides, `extensions/mcp-binding/README.md` in mercury-python and mercury-nodejs — the function
  contract, the server file, the digests and the tests.
