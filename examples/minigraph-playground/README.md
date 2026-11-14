# MiniGraph Playground

This is a sample MiniGraph Playground application for user to create and test Knowledge Graphs

## Build and run

To build this application from source, use `mvn clean package`.

To run this application, do `java -jar target/minigraph-playground-{version}.jar`

## Getting started

1. Visit http://127.0.0.1:8085 to get started
2. Select "MiniGraph Playground" from the top right selection box
3. Click the help button to learn more
4. Follow the tutorials as a step-by-step study guide

## Model answers

While the model answers for tutorials 1 to 14 are embedded, we recommend you follow the tutorials one by one.

When you want to compare your graph model with the model answer, you can use the 'import tutorial-n' where
n is a tutorial chapter number.

## AI demo graphs

Two deployed graphs reach an LLM through the LLM helper app of a language pack, with the engine holding no credential:
`support-triage` (one AI node, the graph routes on its verdict) and `order-assistant` (an agent loop whose one write
waits for a supervisor through suspend and resume). See `src/main/resources/graph/README.md` for the setup and the
protocol, and `OrderAssistantTest` for the token-free end-to-end test.

## Demo: an MCP server's tool in an agent loop, watched from the Playground

The `diagram-assistant` graph turns a description into a Mermaid diagram, validated and rendered by a Mermaid MCP
server through the MCP binding of a language pack, with the rendered image stored out of band. Four processes take part:
this application, the LLM helper, the MCP binding (both from mercury-python or mercury-nodejs) and the AI companion
broker that lets an agent drive the Playground while you watch.

1. **The MCP binding**, bound to the Mermaid server by this example's server file (`mcp/mermaid-servers.yml`; the digest in
   it pins the tool's input schema, so a server-side change stops the binding by name):

   ```bash
   cd mercury-python && pip install -e '.[mcp]'
   mercury-serve extensions/mcp-binding/mcp_binding.py \
     -Dmcp.servers=/absolute/path/to/examples/minigraph-playground/src/main/resources/mcp/mermaid-servers.yml
   ```

   Its log ends with `MCP server mermaid: tool validate_and_render_mermaid_diagram served as diagram.render.mcp`.
2. **The LLM helper**, with the credential in its environment only (`ANTHROPIC_API_KEY`), on port 8086:

   ```bash
   mercury-serve extensions/llm-helper/llm_helper.py -Dllm.model=claude-haiku-4-5
   ```
3. **This application**, from a copy of its jar outside `target/` (a rebuild rewrites the jar under a running JVM):

   ```bash
   java -jar minigraph-playground-x.y.z.jar
   ```

   The startup log shows `Compiled graph diagram-assistant` and `Event-over-HTTP diagram.render.mcp -> http://127.0.0.1:8088/api/event`.
4. **The Playground**, at http://127.0.0.1:8085. Two ways to watch a run:
   - **Alone:** in the console, `import graph from diagram-assistant` (the deployed model), then a dry run, which uses the
     real helper, binding and server:

     ```text
     instantiate graph
     text(Draw a flowchart of a refund approval with four steps, the request, the review, approve or reject.) -> input.body.question
     long(60000) -> model.ttl
     ```

     then `run`. The 60 s budget matters: a dry run's default deadline is 30 s, and the loop node allows 50 s for the model.
   - **With an agent (the recommended collaboration):** the agent starts the AI companion broker against this application
     (`node ../../templates/starter-graph/scripts/ai-companion-broker.mjs --target http://127.0.0.1:8085`), reads the
     session id from its `GET /session`, and you type `session subscribe <that id>` in your console. If your session
     came first, the agent's broker subscribes to yours instead (`POST /subscribe`). Either way the agent sends the two
     commands above through `/api/companion/<its session id>/sync`, one per request, and every line lands on your
     canvas and console too. The companion endpoint answers within 30 s, so for a long run the agent reads the outcome from
     the broker's `GET /console`.

What you see: the walk from `root` through `check-input` to `assist`; `assist` runs the agent loop, two model turns on
Haiku 4.5 in about 10 s: the model drafts the diagram and calls `render_diagram`, the loop calls `diagram.render`, a
function of this application that calls the MCP-bound route `diagram.render.mcp` and stores the SVG the server returned
as `/tmp/rendered_diagrams/<reference>.svg` (`diagram.store.folder`) beside a page, `<reference>.html`, that inlines it,
returning the verdict, the code and the reference; the model answers with the validated code, one sentence, and the
reference. Open `<reference>.html` from Finder or a browser to see the rendered image with its Mermaid code (the server's
SVG is not a standalone document, so the page inlines it rather than linking it). The result body carries the call log (`render_diagram` → `diagram.render`, ok) and the usage; the image
never reaches the model, so a turn costs a few thousand tokens. An invalid draft comes back as `valid: false` with the
server's error, and the model fixes it and calls again.

Without the Playground, the same run is one request: `POST /api/graph/diagram-assistant` with `{"question": "..."}`, or
`POST /api/stream/graph/diagram-assistant` with `accept: text/event-stream` to watch the loop's events.

## Application template

You can use this subproject as a template to build your own MiniGraph application.
