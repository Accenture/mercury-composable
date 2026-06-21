---
title: AI agent guide — building graphs via the companion endpoint
summary: The authoritative context an AI agent needs to build Active Knowledge Graphs
  deterministically — the companion-endpoint contract, how to use the command grammar as the
  source of truth, a validation checklist, and a canonical build recipe.
layer: knowledge-graph
audience: [ai-agent, developer]
keywords: [ai companion, companion endpoint, context engineering, deterministic, minigraph, websocket, graph generation]
related:
  - guides/knowledge-graph/command-reference.md
  - guides/knowledge-graph/playground-and-companion.md
  - guides/knowledge-graph/skills-reference.md
---

# AI agent guide — building graphs via the companion endpoint

> **At a glance**
>
> - **Read this if you are an AI agent** asked to build or modify a MiniGraph. It is the
>   single context you need — you should **not** need to read the engine source.
> - **Generate from rules, not guesses.** The [command grammar](command-reference.md) and its
>   machine-readable form [`minigraph-commands.json`](minigraph-commands.json) are the source of
>   truth. Validate every command against them before sending.
> - **Two endpoints, two jobs** — see below. Both are **dev-only** (`app.env=dev`), no auth.

## Which endpoint? {#endpoints}

| Goal | Endpoint | Notes |
|---|---|---|
| **Execute a deployed graph** | `POST /api/graph/{graph-id}` | Send the request body; get the response. No session. |
| **Build/edit a graph in a live session** | `POST /api/companion/{session-id}` | Dispatch Playground commands into an open browser session. |
| **Read the live model** | `GET /api/graph/session/{session-id}` | Returns the current graph as JSON. |

This guide is about the **companion** flow — co-authoring a graph with a human watching the
Playground.

## The companion contract {#contract}

```
1. A human opens the Playground:  ws://{host}/ws/graph
2. The first WebSocket frame carries the session id, format  ws-<6 digits>-<counter>
   (e.g. ws-384729-17). Get this id from the human.
3. For each command:  POST /api/companion/{session-id}  with Content-Type: text/plain
   and exactly ONE command in the body.
4. The HTTP response is just an acknowledgement (HTTP 200 = dispatched).
   The real output — including syntax errors — streams to the WebSocket console, NOT the
   HTTP response. Ask the human to confirm on the console between steps.
5. Read state with  GET /api/graph/session/{session-id}.
```

Status codes: `200` dispatched (a *syntax* error still returns 200 — its text appears on the WS);
`400` missing/empty/non-text body; `404` no active session for that id.

**Rules of engagement:** one command per POST (multi-line commands are fine — see the grammar);
the session must already be open (you do not create it); single operator — don't POST while a
human is typing in the same instant; never expose this beyond a trusted dev host.

## Generate deterministically {#deterministic}

1. **Use the grammar as source of truth** — [`command-reference.md`](command-reference.md) for the
   rules, [`minigraph-commands.json`](minigraph-commands.json) to look up a command's exact syntax,
   params, and allowed values. Do not infer syntax from a single example.
2. **Validate before sending** — check each command against this list (the engine's
   [invariants](command-reference.md#invariants)):

> **Pre-send checklist**
> - [ ] The root node is named `root`; the end node is named `end`.
> - [ ] Node names and types are **lowercase + hyphen** only.
> - [ ] Each node has **0 or 1** skill (`skill={route}`); the skill's required properties are present
>       (see the [skill→property matrix](command-reference.md#skill-matrix)).
> - [ ] Every node *in the traversal path* connects to ≥1 node (or `export` fails). **Config nodes**
>       (`Dictionary`/`Provider`) are referenced by name (`dictionary[]=`, `provider=`) and need **no** connections.
> - [ ] Multi-line commands (`create`/`update`/`instantiate`) are sent as one block; multi-line
>       *values* use `'''…'''`.
> - [ ] `instantiate graph` precedes `run`/`execute`/`inspect`.
> - [ ] Exactly **one** command per POST.

## Canonical build recipe {#recipe}

A reliable order for building a graph:

1. **Plan** the nodes and the connections (root → … → end) before issuing commands.
2. **Create nodes:** `create node root` (type `Root`), the active/skill nodes, and `create node end`
   (type `End`, usually with `graph.data.mapper` to shape `output.body`).
3. **Connect** them so traversal flows root → end, with no orphans.
4. **Instantiate** with mock input: `instantiate graph` + `{constant} -> input.body.{key}` lines.
5. **Run and inspect:** `run` (or `execute {node}`), then `inspect {output.body}`; iterate.
6. **Export & deploy:** `export graph as {name}`, deploy the JSON, then call
   `POST /api/graph/{name}`.

## Worked example {#example}

Building the hello-world graph via the companion endpoint, one command per request:

```bash
SID="ws-384729-17"   # from the WebSocket welcome frame

curl -sS -X POST "http://{host}/api/companion/${SID}" -H 'Content-Type: text/plain' \
  --data-binary $'create node root\nwith type Root\nwith properties\npurpose=demo'

curl -sS -X POST "http://{host}/api/companion/${SID}" -H 'Content-Type: text/plain' \
  --data-binary $'create node end\nwith type End\nwith properties\nskill=graph.data.mapper\nmapping[]=text(hello world) -> output.body'

curl -sS -X POST "http://{host}/api/companion/${SID}" -H 'Content-Type: text/plain' \
  --data-binary 'connect root to end with done'

curl -sS -X POST "http://{host}/api/companion/${SID}" -H 'Content-Type: text/plain' \
  --data-binary 'instantiate graph'

curl -sS -X POST "http://{host}/api/companion/${SID}" -H 'Content-Type: text/plain' \
  --data-binary 'run'

curl -sS "http://{host}/api/graph/session/${SID}"   # read the resulting model
```

Each `run`/result appears on the human's WebSocket console — pause there for confirmation before
continuing, so a mistake doesn't cascade.

## See also {#see-also}

- [MiniGraph command grammar](command-reference.md) + [`minigraph-commands.json`](minigraph-commands.json) — the source of truth.
- [Playground & AI companion](playground-and-companion.md) — the human-facing view of the same surface.
- [Built-in skills reference](skills-reference.md) — per-skill properties and examples.
