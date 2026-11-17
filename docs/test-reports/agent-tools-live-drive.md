# The built-in agent tools - the Playground's three graphs driven live through both language packs

**Date:** 2026-11-17 · **Design:** [`draft-design-specs/agent-essential-tools.md`](https://github.com/Accenture/mercury-composable/blob/main/draft-design-specs/agent-essential-tools.md) (RFC-0013, build plan step 4)
· **The contract:** [Built-in Agent Tools](../guides/agent-tools.md) · **Earlier records:** [`agent-tool-use-live-drive.md`](agent-tool-use-live-drive.md)
(the MCP binding, 2026-11-16) and [`transcript-audit-live-drive.md`](transcript-audit-live-drive.md) (the audit store, 2026-11-17).

## What was tested

The three Playground graphs on the built-in agent tools, each driven end to end on Haiku 4.5 through the Python pack and
then through the Node.js pack, with the same engine, the same corpus and the same graph database:

- **`document-ingest`** - a document of the synthetic corpus read with `file_extract` and its parties, obligations, rules
  and deadlines merged into the knowledge graph with `graph_ingest`, by a node that declares `mode: auto` (extract once);
- **`document-query`** - the schema read, read-only openCypher through `graph_query`, an answer citing every fact's source
  (query many; no file is read);
- **`site-notes`** - a crawl of this project's published documentation, then a write of the notes into the pack's
  workspace with `file_write`, a marked write that waits for a person at the `advise` checkpoint and runs when approved.

The criteria: every graph answers; the supervised write pauses and resumes; an ingest run twice, or run by the other pack,
changes nothing in the graph; the tools of the two packs behave the same on a real site.

## Setup

| Part | How |
| --- | --- |
| Engine | The Playground jar built from the branch of #647 (`mvn -q -pl examples/minigraph-playground -DskipTests package`, 4.12.30), run from a copy outside `target/`; the peers selected with JVM properties **before** `-jar`: `java -Dllm.peer.port=8087 -Dtools.peer.port=8090 -jar minigraph-playground-4.12.30.jar` |
| Model turn | mercury-python `extensions/llm-helper` on 8086 for the first drive, mercury-nodejs `extensions/llm-helper` on 8087 for the second, both `-Dllm.model=claude-haiku-4-5`, the credential in the helper's environment only |
| Tools | mercury-python `extensions/agent-tools` on 8089 (#64) and mercury-nodejs `extensions/agent-tools` on 8090 (#130), each with `-Dtools.file.root=<a copy of examples/minigraph-playground/corpus> -Dtools.web.allow=accenture.github.io -Dtools.graph.uri=bolt://127.0.0.1:7687 -Dtools.graph.write=true -Dtools.graph.delete=true`, the graph credential in the environment |
| Stores | Redis 7 (the graphs' suspend and resume) and Neo4j 5 Community (`extensions/agent-tools/dev/graph-up.sh`), both on Colima |
| Requests | `POST /api/graph/{graph}` with one correlation id per transaction; the graph counted through the tools' own `graph.query` |

## Result in brief

| Scenario | Pack | Outcome |
| --- | --- | --- |
| A1 - ingest the four documents | Python | 4 answers (turns 3, 3, 4, 3): 47 nodes, 43 relationships, every node with its `source`; one `graph_ingest` call refused by the loop's schema check and corrected by the model on the next turn |
| A2 - ingest the supplier agreement again | Python | every fact merged (the counts of obligations, rules and deadlines unchanged); the document node doubled, because the model keyed it `document:supplier-agreement` the first time and `document:supplier-agreement.md` the second (finding 3) |
| A3 - two questions | Python | answered from the graph with sources, in 4 and 3 turns (the model ran up to four queries before answering) |
| A4 - `site-notes` on the knowledge-graph guides | Python | approval at turn 3 with the notes' path and 3 069 characters of content; approved, the write ran, `notes/built-in-skills.md` (3 083 bytes) in the workspace with its source cited |
| B1 - ingest two documents again, the other pack | Node.js | every fact merged with the same keys; the supplier agreement's document node merged too (the tightened key rule); the travel policy gained a second document node, since the first drive's loose key was `document:travel-policy` (finding 3) |
| B2 - one question | Node.js | answered with sources in 3 turns |
| B3 - `site-notes` | Node.js | first attempt stopped at the turn cap after seven crawls and fetches (finding 5); with the three-step prompt, approval at turn 3 and `notes/workflow-suspension.md` (2 270 bytes, three sources) after approval |
| The two packs on the real site | both | a crawl of the section from either pack returned the same three pages with the same text sizes (10 469, 8 518 and 20 000 characters) |

The Python helper served 27 model turns, the Node.js helper 21. No token, prompt or page text reached a tool's log: the
packs logged `graph.ingest 200 12 nodes, 11 relationships`, `web.crawl accenture.github.io 200 3 pages, 3 fetched (cut)`,
`file.write 200 3083 bytes (create)`.

## The graph, counted

| After | Document | Party | Obligation | Rule | Deadline | PARTY_TO | STATED_IN |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A1, the four documents | 4 | 4 | 15 | 12 | 8 | 8 | 35 |
| A2, the supplier agreement again | 5 | 4 | 15 | 12 | 8 | 10 | 44 |
| the duplicate document removed through `graph.write` | 4 | 4 | 15 | 12 | 8 | 8 | 35 |
| B1, two documents again from the Node.js pack | 5 | 4 | 15 | 12 | 8 | 10 | 43 |

The facts never doubled: every obligation, rule and deadline kept its key across runs and packs
(`obligation:supplier-agreement-obl-1`, ...), so the tool's idempotent `MERGE` held. The document node is the one the
model keyed two ways before the rule was tightened. `Party` stays at four: the two agreements' three companies and the
travel policy's "employees", which the model made a party.

## Findings

1. **The Event-over-HTTP map takes one route per entry.** The example's first map listed the twelve routes comma-separated in
   two entries and the engine refused them at startup (`Invalid Event over HTTP config entry`), so every tool was unreachable.
   One entry per route now, and the packs' READMEs and the engines' page show it that way.
2. **The peer overrides are JVM properties.** `-Dllm.peer.port` after `-jar` is a program argument the engine never reads;
   the first drive ran on the defaults, which happened to be the Python peers. The Playground's README says so now.
3. **Idempotence is the tool's; the keys are the model's.** `graph_ingest` merges on the key, and every fact kept its key
   across runs and packs - but the document node doubled once, because the prompt's `document:<path>` and `<document-stem>`
   left the model a choice. The ingest prompt now spells the rule with an example (`document:supplier-agreement.md`,
   `obligation:supplier-agreement-obl-1`), and the Node.js drive merged the supplier agreement's document into the Python
   drive's node. A graph that must stay clean adds a uniqueness constraint on `key` per label through `graph_write`.
4. **A relative `include` returned the start page alone.** The model named the section as `knowledge-graph/`; the tool
   prepended a slash and no link path began with `/knowledge-graph/`. A relative include now matches anywhere in the path in
   both packs (mercury-python #65, mercury-nodejs #131); an absolute one stays a prefix.
5. **A loop without a plan dithers.** The first Node.js attempt at `site-notes` crawled and fetched seven times, all
   successful, and hit the turn cap without writing - the loop stopped it with `cap: turns`, as designed. The research
   prompt now names three steps (one crawl with `include`, the write, the answer) and the next run wrote at turn 3.
6. **The loop's schema check reaches the model.** One `graph_ingest` call was refused as invalid before any write, the
   model corrected the shape on the next turn, and the graph was loaded; the call log shows `invalid` then `ok`.
7. **A fetched 404 is a result.** The model asked for a page that does not exist; `web.fetch` answered with status 404 and
   the page's text, and the model moved on to the crawl. A page's own status is never the tool's error.

## Reproduce

```bash
# the stores
docker run -d --name mercury-redis -p 6379:6379 redis:7-alpine
GRAPH_PASSWORD=... extensions/agent-tools/dev/graph-up.sh             # in either pack
# the pack (Python shown; the Node.js pack is the same with node dist/src/cli.js and ports 8087 and 8090)
mercury-serve extensions/llm-helper/llm_helper.py -Dllm.model=claude-haiku-4-5
GRAPH_PASSWORD=... mercury-serve extensions/agent-tools/agent_tools.py \
  -Dtools.file.root=<a copy of examples/minigraph-playground/corpus> -Dtools.web.allow=accenture.github.io \
  -Dtools.graph.uri=bolt://127.0.0.1:7687 -Dtools.graph.write=true
# the engine
mvn -q -pl examples/minigraph-playground -DskipTests package
java -Dllm.peer.port=8086 -Dtools.peer.port=8089 -jar minigraph-playground-4.12.30.jar
# the drives
curl -s -X POST http://127.0.0.1:8085/api/graph/document-ingest -H 'content-type: application/json' \
  -H 'x-correlation-id: ingest-1' -d '{"document": "supplier-agreement.md"}'
curl -s -X POST http://127.0.0.1:8085/api/graph/document-query -H 'content-type: application/json' \
  -H 'x-correlation-id: query-1' -d '{"question": "Which deadlines does the supplier agreement set?"}'
curl -s -X POST http://127.0.0.1:8085/api/graph/site-notes -H 'content-type: application/json' -H 'x-correlation-id: notes-1' \
  -d '{"section": "https://accenture.github.io/mercury-composable/guides/knowledge-graph/", "topic": "Built-in skills"}'
# then approve with the same correlation id: {"turn": <the reply's turn>, "decisions": [{"id": "<the call id>", "approved": true}]}
```
