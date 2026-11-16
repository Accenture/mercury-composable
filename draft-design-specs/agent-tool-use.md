# Agent tool use — a tool is a composable function — design spec

**Status:** ACCEPTED in review on 2026-11-16 (Eric), with two revisions (section 6). The implementation follows in the
language packs, Python first, then Node.js in lockstep, then the Playground example here.
**Serves:** `bp-agent-orchestration` → `vision-mercury-composable` (Track 2, the graph as AI SDLC).
**Extends:** [`llm-helper-agent-loop.md`](./llm-helper-agent-loop.md) section 9 (the MCP bindings in the language packs,
built 2026-11-14) and [`ai-agent-orchestration.md`](./ai-agent-orchestration.md) section 3.3 (adapter family B).
**ADR-0033 stands:** one route per tool, bound in the packs, boot failing on a missing tool or a changed input-schema
digest, no generic route. This paper changes how a pack *declares* that binding, not the decision.
**Repo scope:** the packs' `extensions/mcp-binding` (mercury-python, mercury-nodejs) and the Playground's
`diagram-assistant` example in this repository. No engine code changes.

---

## 1. The principle

A tool an agent calls is a composable function, addressed by a route, whatever stands behind it: engine code, a pack
function, a vendor's HTTP API, or a tool of an MCP server. The graph's catalog row names the route and carries the
description and the input schema the model reads; the engines' agent loop (`llm.run`) dispatches the call to the route
like any other call; nothing in the loop, the catalog or the graph says how the route is served. Two consequences follow:

- **The MCP client is fully encapsulated** in the pack function that uses it. A tool may begin life as an MCP tool and
  later be reimplemented as ordinary code, or move from one server to another, without a change to the graph.
- **A route is declared the way every route is declared**, as a `preload` function in the pack's code, so a developer
  reading the pack sees every route it serves, and the host's guarantees apply unchanged: an undeclared route is a 404,
  a private one is a 403 on the wire.

## 2. The loose end this closes

Step 4 of the agent loop's build plan (2026-11-14) built the MCP binding as an application that reads a server file:
servers by name with their transport and credential references, and under each server an allowlist mapping a remote
tool to a route and the SHA-256 digest of its input schema. At boot the binding connected the servers, verified the
allowlist and registered each tool's route as an alias of one `preload` function, `mcp.tool` (private, bound to no
tool), which read the route it was called as from its execution context and resolved the server and the tool from it.

The host had no special treatment for it, and the guarantees held: the alias was a real registry entry, an unknown
route a 404, `mcp.tool` on the wire a 403. The objection (Eric, 2026-11-16) was to the shape: routes came into being
from a YAML file at boot rather than from a declaration in code, the route-to-tool table was a second place to look,
and a developer could reasonably forget the non-standard mechanism. The Playground's `diagram-assistant` showed it: the
engine function `diagram.render` called a pack route, `diagram.render.mcp`, that no `preload` in the pack declared.

## 3. Shapes considered

**A. The allowlist row** (as built 2026-11-14). Declarative: a new tool is a YAML row, a catalog row and an
Event-over-HTTP entry, no code. Set aside for the reason above: a route that appears from configuration is not the
composable shape, and any per-tool logic needs a wrapper elsewhere anyway.

**B. One generic public route, `mcp.tool`, with the server and the tool named in the call** (the first proposal of the
2026-11-16 review). The pack becomes a generic MCP client; an engine-side wrapper function per tool fixes the target in
code, so the catalog row names the wrapper and stays certifiable; the pack's configuration shrinks to servers only.
Rejected in the debate: it is ADR-0033's rejected alternative (c) with a rule bolted on (a catalog row must never name
`mcp.tool` directly, or the model picks the tool), it puts the tool identity in headers, and it demands Java or Rust code
for every tool, a plain pass-through included.

**C. A pack function per tool, bound in code to the MCP tool through an encapsulated client** (chosen). The
architecture of A is kept, one route per tool, the digest pinned, boot failing fast, and the declaration moves into the
code where every other route is declared. The generic route disappears instead of being guarded.

## 4. The design

### 4.1 The function and the binding

```python
# extensions/mcp-binding (mercury-python)
render = mcp.bind("mermaid", "validate_and_render_mermaid_diagram", schema_sha256="5ae6d75d…")

@preload(route="mermaid.render", instances=10)
async def mermaid_render(_headers, body):
    return await render(body)
```

```javascript
// extensions/mcp-binding (mercury-nodejs)
const render = mcp.bind('mermaid', 'validate_and_render_mermaid_diagram', { schemaSha256: '5ae6d75d…' });
preload('mermaid.render', { instances: 10 }, async (_headers, body) => render(body));
```

- **`bind(server, tool, schema_sha256)`** declares the binding and returns the tool as an async callable: the arguments
  in, the result out, with the contract the binding already speaks - the structured content when the server gives it,
  else the text of the content blocks; a result marked `isError` a 400 carrying the server's text; a timeout 408; a
  protocol failure 502; a server not connected 503; arguments that are not a map 400.
- **The function is ordinary.** It may pass the arguments through, shape them, or add behaviour around the call; it is
  public or private like any function; its `instances` are its own.
- **Boot** (the platform's startup hook, before the host listens) connects every configured server, lists its tools and
  verifies every declared binding: a server that is not configured, a tool the server does not offer, or an input
  schema whose digest differs from the pinned one stops the application by name, exactly as before. A configured server
  with no binding is a warning; a tool the server offers but nothing binds is unreachable.
- **The server file keeps what is environment-specific:** transport, command or URL, credential references, timeouts.
  The `tools` allowlist leaves it. The bindings in code are the allowlist, and the digest sits next to the function that
  pins it, so one place declares route, server, tool and digest.
- **`describe`** still connects to a server, lists its tools and prints each digest, now with a `bind` line to paste;
  the catalog row stays the developer's own text on the graph.
- **`mcp.health`** stays the binding's own route: the servers, their connections, the number of bound tools.
- **Out of the box,** the extension's default server file names the public Mermaid Chart MCP endpoint and the extension's
  example function binds its one tool as `mermaid.render`, so the binding starts and serves a tool with no credential; a
  deployment replaces both with its own servers and functions.

### 4.2 Naming (revision 1)

A route names the capability, never the transport: `mermaid.render`, not `mcp.mermaid.render` or `diagram.render.mcp`.
The reason is the principle itself: a tool may start as an MCP tool and later be implemented another way, and the graph
must not know. The `mcp.` namespace is reserved for the binding's own routes (`mcp.health`). The operational view of
which functions a binding serves comes from `/info/routes`, the health route and the trace.

### 4.3 The engine-side wrapper (revision 2)

An engine function in front of a pack route is business logic of the graph, not part of the binding, and it stays when
it does more than call the tool. The Playground's `DiagramRender` is the case: it calls the pack's function and stores the
SVG out of band with an HTML page the engine serves, giving the model a reference instead of the image. It keeps that
role; only the route it calls changes, to `mermaid.render`.

### 4.4 Certification, unchanged

The catalog row names a route whose code fixes the server and the tool; the pinned digest means the server cannot change
the tool under the graph; the model reads the graph's description, never the server's. Nothing is hidden in the call, so
ADR-0033's rejection of a generic route stands and needs no rule.

## 5. Consequences

- **The packs** (mercury-python, mercury-nodejs, in lockstep): the binding becomes a library with `bind`, the boot
  verification and the health route, plus the example function; the server file loses `tools`; the shared vector file
  keeps its digest and result cases and re-expresses its verification cases over declared bindings; the tests, the
  extension READMEs and the unreleased CHANGELOG entries follow. The pack-side fact `mcp-binding-route-per-tool` is
  superseded.
- **The Playground example** (this repository): the server file keeps the server only; `event-over-http.yaml` maps
  `mermaid.render`; `DiagramRender`, its mock and its test call `mermaid.render`; the READMEs follow. The engines' code does
  not change. ADR-0033 receives a consequence line, not an amendment.
- **Adding a tool** becomes: a server entry when the server is new, three lines of pack code, a catalog row on the graph
  and an Event-over-HTTP entry. Against the allowlist row it costs three lines of code; against shape B it is the same
  effort in the pack's language, without headers and without engine code.

## 6. Review rulings (Eric, 2026-11-16)

1. Shape C over shape B: the per-tool function lives in the pack, bound in code; `mcp.tool` is no longer a route.
2. **Revision 1:** no `mcp.` prefix on a tool's route. A tool can start as an MCP tool and later be updated to a regular
   composable function, so the name must not carry the transport.
3. **Revision 2:** `DiagramRender` stays. What it does beyond the call is business logic of the graph itself.

## 7. Build plan

1. mercury-python: the library and the example function, the tests, the README and the CHANGELOG; the boot driven live
   against the Mermaid server.
2. mercury-nodejs: the twin, the shared vector file byte-identical.
3. mercury-composable: the Playground example and its test, the READMEs, the ADR consequence line; then the
   `diagram-assistant` graph driven end to end again.
