---
title: Playground & AI companion
summary: The MiniGraph Playground — an interactive workbench for building, dry-running, and
  inspecting Active Knowledge Graphs — its command grammar and collaborative sessions, plus the
  companion endpoint that lets an AI co-author graphs in a live session.
layer: knowledge-graph
audience: [developer, architect]
keywords: [minigraph playground, cli, session, collaboration, companion endpoint, user-ai collaboration, websocket]
related:
  - guides/knowledge-graph/build-your-first-graph.md
  - guides/knowledge-graph/index.md
---

# Playground & AI companion

> **At a glance**
>
> - The **MiniGraph Playground** is a browser workbench (a WebSocket session at `/ws/graph`) for
>   building, dry-running, and inspecting graphs interactively.
> - You drive it with a small **command grammar**; sessions can be **shared** for collaborative
>   modeling; and the **companion endpoint** lets a script or an AI dispatch commands into a live
>   session.
> - **Dev-only.** The Playground and companion endpoints are gated by `app.env=dev` and are not
>   registered in production.

## The command grammar {#cli}

Everything you do in the Playground is a command typed into the console (multi-line where noted).
The essentials:

| Intent | Command |
|---|---|
| Create a node | `create node <name>` + `with type <Type>` + `with properties` + `k=v` lines |
| Update a node | `update node <name>` + `with type <Type>` + `with properties` + `k=v` lines |
| Connect / disconnect | `connect <a> to <b> with <relation>` · `delete connection <a> and <b>` |
| Delete a node | `delete node <name>` |
| Instantiate (seed input) | `instantiate graph` + optional `type(value) -> input.body.<key>` lines |
| Run / single node | `run` · `execute <node>` |
| Inspect state | `inspect <key>` (e.g. `inspect output`, `inspect model.sum`) |
| Describe | `describe graph` · `describe node <name>` · `describe skill <route>` |
| List | `list nodes` · `list connections` · `seen` |
| Persist | `export graph as <name>` · `import graph from <name>` |
| Help | `help` · `help <topic>` |

A typical loop — build, seed, dry-run, inspect:

```
create node fetcher
with type Fetcher
with properties
skill=graph.api.fetcher
dictionary[]=person-name
input[]=input.body.person_id -> person_id
output[]=result.name -> output.body.name

connect root to fetcher with fetch
connect fetcher to end with done

instantiate graph
int(100) -> input.body.person_id

execute fetcher
inspect output
```

`describe skill <route>` (e.g. `describe skill graph.api.fetcher`) prints the authoritative,
shipped help for each skill — the same content this Part documents.

## Collaborative sessions {#sessions}

Each open Playground connection is a **session** with a public id like `ws-178443-2`. Sessions can
be shared so several people model the same graph together:

```
session                       # show this session + its subscribers
session subscribe ws-178443-2 # mirror another session's commands into yours
session unsubscribe           # detach, keeping your current graph
session reset                 # clear the primary session; subscribers keep their graphs
```

When you subscribe to a primary session, commands flow to both, keeping the model in sync. (You
can't subscribe to yourself or to a non-primary session; a primary session has nothing to
unsubscribe.)

## The companion endpoint {#companion}

The **companion endpoint** lets an HTTP client — a script, a test harness, or an AI agent —
dispatch a Playground command into an **already-open** session. The command runs as if typed, and
output streams back to that session's browser console.

```
POST /api/companion/{session-id}
Content-Type: text/plain

<any Playground command>
```

The response is an immediate acknowledgement — the real output appears on the WebSocket:

```json
{
  "type": "companion",
  "status": "accepted",
  "id": "ws-123456-7",
  "message": "Command dispatched to graph.command.service. Output streams to the WebSocket console for this session."
}
```

Status codes: **200** dispatched (command *syntax* errors still return 200 — the error text appears
on the WebSocket, not in the HTTP response); **400** missing/empty/non-text body; **404** no active
session for that id. To read the current model for a live session, `GET /api/graph/session/{id}`.

A minimal call:

```bash
SESSION_ID="ws-384729-17"
curl -sS -X POST "http://localhost:8300/api/companion/${SESSION_ID}" \
  -H 'Content-Type: text/plain' \
  --data-binary $'create node root\nwith type Root\nwith properties\nskill=graph.math'
```

Under the hood, the endpoint (`post.companion.command`) confirms the session is live, then dispatches
to a **singleton** command handler so commands execute in order without races. Like the Playground
itself, it is gated by `@OptionalService("app.env=dev")` — **do not expose it beyond trusted dev
environments** (there is no auth).

## User–AI collaboration {#collaboration}

Put the two together and you get the seed of the framework's collaboration vision: a person watches
the Playground in the browser while an **AI companion** builds the graph through the companion
endpoint, one command per request, pausing for the human to confirm on the console between steps.
The human steers and certifies; the AI drafts and refines; both work on the *same* live model.

This is collaboration over a *model*, not over code — which is exactly the point of treating a
[Knowledge Graph as the application](index.md). Today the AI is an external session driving the
endpoint; maturing it into an integrated, pluggable companion is on the roadmap (see
[the maturity section](index.md#maturity)).

## See also {#see-also}

- [Build your first Active Knowledge Graph](build-your-first-graph.md) — the command loop in a full walkthrough.
- [Built-in skills reference](skills-reference.md) — what `describe skill` documents in the console.
- [Knowledge Graph as Application](index.md) — the paradigm and where collaboration fits.
