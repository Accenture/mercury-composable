# Deploy your graph models

To deploy a graph model, drop the JSON file that is exported from a graph model that you created in the MiniGraph
Playground into this folder and list it in `graphs.yaml`.

## The AI demo graphs

All of them reach the LLM helper app of a language pack (mercury-python on port 8086, or mercury-nodejs on port 8087)
through the declarative Event-over-HTTP map in `event-over-http.yaml`, so the engine holds no credential and no
provider SDK. Run one helper with `ANTHROPIC_API_KEY` in its environment (its README has the command), then start
this application. `-Dllm.peer.port=8087` selects the Node.js helper. In the test scope the same route names resolve
to a scripted mock (`MockLlmChat`), so the graphs are tested without tokens. The Rust port's Playground example
(`examples/minigraph-playground` of mercury) ships these JSON models unchanged, compiled by that engine, and drove
every one of them live (its `docs/test-reports/agent-graphs-live-drive.md`).

### `support-triage` - one AI node, the graph decides

The model classifies a request into an enumerated category (`llm.chat` through `graph.task`), and the graph routes on
the verdict. The AI agent orchestration concept's experiment E0.

### `order-assistant` - an agent loop with a supervised write

A customer-support assistant built on the agent loop of RFC-0011 (`llm.run`, the `extensions/agent-loop` module):

- **The tool catalog is graph data.** The `order-tools` node under the island holds three rows, each mapping an API-safe
  name to a composable route: `order_lookup` (`demo.order.lookup`), `shipment_status` (`demo.shipment.status`) and
  `issue_refund` (`demo.refund.issue`, marked `write: true`). The three functions live in `com.accenture.minigraph.demo`
  and read an in-memory order book: ORD-1001 was delivered, ORD-1002 and ORD-1004 were lost by the carrier, ORD-1003 is
  in transit. The model sees the descriptions written on the graph, never the routes.
- **The loop node `assist`** sends each model turn to `llm.chat` and each tool call to its route, in the default
  `write` mode: a marked write waits for a person. It carries `progress=Investigating the order`, so a watched run
  gets the loop's events (`run.start`, `turn.start`, `tool.start`, `tool.end`, `turn.end`, `approval.wait`,
  `run.end`).
- **The graph decides on the status.** `done` goes to `respond` (the answer, the call log, the usage and the model);
  `approval` goes to the `advise` checkpoint, which stages the waiting calls with their arguments and pauses the run
  with `graph.suspend` on the Redis state store; a refusal or a cap goes to `stopped` (HTTP 422).
- **A resumed run lands on `decide`.** A request without decisions, or for another turn than the one that waits,
  re-advises and pauses again. A request with the decisions for this turn resets the loop node and runs it again with
  the conversation, the turn and the approval block kept in the model - the loop runs the calls it showed, never calls
  taken from the request. A decision the loop refuses (an unknown call id, a call decided twice) raises a 400 that the
  `failed` handler and `triage-failure` turn back into an advice, so the transaction is not lost.

The protocol, with one correlation id per transaction (Redis must be running - `helpers/redis-standalone`, or a
container on port 6379):

```bash
# 1. the question - the model checks the order and the shipment, then asks for the refund: the write waits
curl -s -X POST http://127.0.0.1:8085/api/graph/order-assistant \
  -H 'content-type: application/json' -H 'x-correlation-id: order-1002-case' \
  -d '{"question": "My order ORD-1002 never arrived. Can you help?"}'
```

```json
{"type": "approval", "turn": 2, "run": "fresh",
 "calls": [{"id": "toolu_01...", "name": "issue_refund", "route": "demo.refund.issue", "wait": true,
            "input": {"order_id": "ORD-1002", "amount": 174.0, "reason": "Shipment lost in transit"}}],
 "answered": [{"name": "order_lookup", "status": "ok"}, {"name": "shipment_status", "status": "ok"}],
 "message": "A tool call waits for a supervisor's decision. ...", "cid": "order-1002-case"}
```

```bash
# 2. the supervisor decides - the same correlation id, the advised turn, one decision per waiting call
curl -s -X POST http://127.0.0.1:8085/api/graph/order-assistant \
  -H 'content-type: application/json' -H 'x-correlation-id: order-1002-case' \
  -d '{"turn": 2, "decisions": [{"id": "toolu_01...", "approved": true, "note": "Carrier confirmed the loss"}]}'
```

```json
{"type": "answer", "turn": 3, "run": "resume", "model": "claude-haiku-4-5-20251001",
 "message": "Your refund of $174.00 USD has been issued under refund ID RF-5A0E91FC. ...",
 "calls": [{"name": "issue_refund", "route": "demo.refund.issue", "status": "ok"}],
 "usage": {"input_tokens": 1595, "output_tokens": 60, "cache_creation_input_tokens": 0, "cache_read_input_tokens": 0}}
```

`"approved": false` with a note sends the note back to the model as the call's error, and the model answers with it
(it offers a replacement instead). A delivered order needs no approval: the answer comes back in one request.

To watch a run, POST the question to `/api/stream/graph/order-assistant` with `accept: text/event-stream`, or start
the application with `-Dgraph.websocket.enabled=true` and open `ws://127.0.0.1:8085/ws/graph-run/order-assistant`:
the first text frame `{"header": {"X-Correlation-Id": "..."}, "body": {"question": "..."}}` starts the run, and each
event comes back as a frame before the result closes the session.

### `diagram-assistant` - an MCP server's tool in the catalog

The same agent loop with one tool that reaches an MCP server: the catalog row `render_diagram` maps to `diagram.render`,
a function of this application (`DiagramRender`) that calls `mermaid.render` - a function of the **MCP binding** of a
language pack, bound in the binding's code to the tool of a Mermaid MCP server reached over streamable HTTP, the digest of
the tool's input schema pinned next to it (`mcp/mermaid-servers.yml` in this folder is a copy of the server entry the
binding's own default file carries) - and keeps
the rendered image out of band: the server answers the verdict with the SVG, the function stores the SVG as
`/tmp/rendered_diagrams/<reference>.svg` (`diagram.store.folder`) beside `<reference>.html`, a page that inlines it for a
browser, and returns the verdict, the code and the reference, never the image. The model drafts a Mermaid diagram from the description, validates it through the tool, fixes it if
the server says it is invalid, and answers with the validated code and the reference. The graph names routes, the
binding names the server, and the engine stays MCP-free (RFC-0011 / ADR-0033, step 4). The full walkthrough, with the
Playground and the AI companion broker, is in the example's README.

```bash
# 1. the MCP binding of a language pack, on port 8088 (Python) or 8089 (Node.js); its default server file names
#    the Mermaid server and its own function mermaid.render binds the tool
mercury-serve extensions/mcp-binding/mcp_binding.py
# 2. the LLM helper, as for the other graphs; then this application (-Dmcp.peer.port=8089 for the Node.js binding)
# 3. a description
curl -s -X POST http://127.0.0.1:8085/api/graph/diagram-assistant \
  -H 'content-type: application/json' \
  -d '{"question": "Draw a flowchart of a refund approval: request, review, approve or reject."}'
```

The answer carries the validated Mermaid code in a fenced block, the reference of the stored image, the call log
(`render_diagram` → `diagram.render`, `ok`) and the usage. Watched over `/api/stream/graph/diagram-assistant`, the run
shows `tool.start` and `tool.end` for the route like any other tool. Before the storing function existed, the server's
image rode back to the model as a 36,000 to 43,000-token tool result; a `result_limit` on the node cut it to 4,300, and the
out-of-band store makes the result a few hundred tokens with the image kept - the shape to copy for any tool whose
reply carries a payload the model does not need. In the test scope `MockDiagramRender` serves `mermaid.render` and the
scripted `llm.chat` drafts and validates, so the graph and the storing function are tested without a token, a network or
an MCP server.

The graph names only routes, so the model, the provider and the pack are the helper's configuration, never the graph's:
the same graph ran unchanged on Claude Haiku 4.5 and Opus 5.5 through both helpers
(`docs/test-reports/agent-loop-live-drive.md`).

### `site-notes`, `document-ingest` and `document-query` - the built-in agent tools

Three graphs on the essential built-in tools of RFC-0013, served by a language pack's `extensions/agent-tools` (mercury-python
on port 8089, or mercury-nodejs on port 8090; `-Dtools.peer.port=8090` selects the Node.js one). Each catalog row is a
copy of the row the extension publishes (`extensions/agent-tools/catalog/*.json`, or `describe`), and the sandboxes are
the pack's configuration, never an argument of a call. Run the extension with the sandboxes the demos need:

```bash
mercury-serve extensions/agent-tools/agent_tools.py \
  -Dtools.file.root=<this repository>/examples/minigraph-playground/corpus \
  -Dtools.web.allow=accenture.github.io \
  -Dtools.graph.uri=bolt://127.0.0.1:7687 -Dtools.graph.write=true
```

(`GRAPH_PASSWORD` in its environment for the database; `extensions/agent-tools/dev/graph-up.sh` of either pack starts
Neo4j Community on Colima.) The LLM helper and Redis run as for `order-assistant`. The peer overrides are JVM properties,
before `-jar`: `java -Dllm.peer.port=8087 -Dtools.peer.port=8090 -jar minigraph-playground-4.12.30.jar` selects the
Node.js pack. The three graphs were driven live through both packs on 2026-11-17 (`docs/test-reports/agent-tools-live-drive.md`). In the test scope the tool routes
resolve to a scripted stand-in (`MockAgentTools`) and the model turns to `MockLlmChat`, so `AgentToolsGraphsTest`
runs the three graphs without tokens, a network or a database.

- **`site-notes` - a read of the outside and a supervised write.** `POST /api/graph/site-notes` with
  `{"section": "https://accenture.github.io/mercury-composable/guides/", "topic": "Configuration keys"}`. The
  `research` node crawls the section (`web_crawl`, a read, unasked), then asks to write `notes/<topic>.md` into the
  workspace with `file_write` - a marked write, so under the node's default mode the run pauses at the `advise`
  checkpoint with the write's path and content, exactly as `order-assistant` pauses for a refund. The person approves
  with the same correlation id (`{"turn": 2, "decisions": [{"id": "...", "approved": true}]}`), the loop runs the write it
  showed, and the answer names the file. A refused write goes back to the model as the call's error.
- **`document-ingest` - extract once.** `POST /api/graph/document-ingest` with `{"document": "supplier-agreement.md"}`
  (a name in the corpus, the pack's workspace). The `ingest` node reads the document with `file_extract` and merges the
  parties, obligations, rules and deadlines it finds into the knowledge graph with `graph_ingest`: one node per fact,
  keyed and labelled by convention, every node and relationship stamped with the document as its `source`. The node
  declares **`mode: auto`**, so the marked write runs without a person approving each batch - the product owner's
  decision, written on the graph and visible in its certification. Running it twice changes nothing.
- **`document-query` - query many.** `POST /api/graph/document-query` with `{"question": "Which deadlines do the
  policies set?"}`. The `answer` node reads the schema (`graph_schema`), runs one read-only query (`graph_query`) and
  answers with the facts and their sources. No file is read: the documents were read once.

The synthetic corpus in `corpus/` (two agreements and two policies of fictional companies, written for the demo) is
the workspace of the pack; the tests read it through the stand-in. The built-in tools' contract is the engines' page
`docs/guides/agent-tools.md`.
