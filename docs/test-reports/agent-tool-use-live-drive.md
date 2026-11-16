# Agent tool use - the `diagram-assistant` graph driven end to end on the reshaped MCP binding

**Date:** 2026-11-16 · **Design:** [`draft-design-specs/agent-tool-use.md`](../../draft-design-specs/agent-tool-use.md) (ADR-0033 stands)
· **Earlier records:** [`agent-loop-live-drive.md`](agent-loop-live-drive.md) (the loop, 2026-11-14) and
[`agent-loop-trace-lineage.md`](agent-loop-trace-lineage.md) (the lineage criterion, 2026-11-14).

## What was tested

The MCP binding of each language pack was reshaped on 2026-11-16: a tool of an MCP server is a `preload` function of the
pack, bound in the pack's code through an encapsulated client (`bind(server, tool, digest)`), the server file keeps
servers only, and the route names the capability - `mermaid.render` - never the transport. The Playground's
`diagram-assistant` graph was driven again on that shape, against the live public Mermaid Chart MCP server, through the
Python binding and through the Node.js binding, with the regression criteria of the earlier drives: the graph answers with
a validated diagram and a stored image, and the span lineage is continuous end to end - every record's parent is a span
some record owns, from the REST entry to the pack's function.

## Setup

| Part | How |
| --- | --- |
| Engine | The Playground jar built from the branch (`mvn -DskipTests package -f examples/minigraph-playground/pom.xml`, 4.12.30), run from a copy outside `target/`, `-Dmcp.peer.port` selecting the binding (8088 Python, 8089 Node.js) |
| Model turn | mercury-python `extensions/llm-helper` on 8086, `-Dllm.model=claude-haiku-4-5`, the credential in its environment only |
| Tool | mercury-python `extensions/mcp-binding` on 8088 and mercury-nodejs `extensions/mcp-binding` on 8089, each with its default configuration: the `mermaid` server in the default server file, the extension's own `mermaid.render` binding `validate_and_render_mermaid_diagram` with the pinned digest |
| Request | One `POST /api/graph/diagram-assistant` per run: `{"question": "Draw a flowchart of a refund approval: request, review, approve or reject."}` |
| Lineage check | The engine's `Telemetry` lines and the packs' `distributed.tracing` lines of each process, parsed into one tree per trace id; the trace id read from the binding's `Handled mermaid.render` line |

Both bindings booted against the live server (`MCP server mermaid: tool validate_and_render_mermaid_diagram bound`,
`MCP binding ready - 1 server(s), 1 bound tool(s)`); the Playground's startup log showed
`Event-over-HTTP mermaid.render -> http://127.0.0.1:808x/api/event`.

## Results

| | Run 1 - Python binding | Run 2 - Node.js binding |
| --- | --- | --- |
| HTTP | 200 in 12.7 s | 200 in 9.9 s |
| Turns, usage | 2 turns; 1,909 input, 325 output tokens | 2 turns; 1,925 input, 366 output tokens |
| Call log | `render_diagram` → `diagram.render`, `ok` | `render_diagram` → `diagram.render`, `ok` |
| Answer | The validated Mermaid flowchart in a fenced block, one sentence, the reference | The same shape |
| Stored image | `/tmp/rendered_diagrams/822d421b….svg`, 98,878 bytes, beside its `.html` page | `/tmp/rendered_diagrams/714774e7….svg`, 60,556 bytes, beside its `.html` page |
| The binding | `Handled mermaid.render status=200 exec_time=5535.88ms` | `Handled mermaid.render status=200 exec_time=3784.88ms` |
| Trace records | 20: engine 11, helper 6, binding 3 | 20: engine 11, helper 6, binding 3 |
| Every parent owned by a record | yes | yes |

The pack leg's ancestry, identical in both runs:

```text
mermaid.render <- event.api.service <- http.request <- diagram.render <- llm.run <- graph.task <- graph.math
               <- http.flow.adapter <- http.request
```

The three records the binding leaves (the edge's `http.request` under the engine's `diagram.render`, `event.api.service`
under it, the function's own `mermaid.render` under that) are the Java callee's shape the packs adopted on 2026-11-14;
the only change in the tree since the 2026-11-14 lineage drive is the pack function's name, `mermaid.render` for
`diagram.render.mcp`. A first pair of runs minutes earlier, before the engine's log was captured per run, answered the
same way (200 in 20.3 s and 11.3 s, the images stored) - four runs, four answers, no failure.

## Reading

- The reshaped declaration changes nothing the graph sees: the catalog row still names `diagram.render`, the engine's
  function still calls one pack route, and the route is a real `preload` function of the pack that a developer can read.
- The two bindings are interchangeable behind `-Dmcp.peer.port`, as the shared vector file (version 2) and the twin test
  suites intend.
- No prompt, completion or credential appears in any log or record; usage rides the result as before.
