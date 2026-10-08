# The engine protocol as the webapp sees it

The Playground speaks to the engine through a WebSocket session that carries **plain text lines**
(the console), plus a few REST endpoints for payloads too large or too binary for the console. The
Java and the Rust engine answer with the same lines, which is what lets one bundle serve both. This
page lists the contract from the webapp's side: what it sends, what it expects back, and how every
incoming line is classified. The source of truth is `src/protocol/classifier.ts` and the parsers it
calls (`src/utils/messageParser.ts`, `src/session/sessionParser.ts`,
`src/graphRun/graphRunProtocol.ts`).

## The WebSocket session

| Item | Value | Where |
|---|---|---|
| Paths | `/ws/graph/playground` (Minigraph), `/ws/json/path` (JSON-Path) | `config/playgrounds.ts` |
| URL | dev: `ws://localhost:3000<path>` (the Vite proxy forwards to port 8085); production: `ws://<page host><path>`, same origin | `utils/urls.ts` |
| Open | the client sends `{"type":"welcome"}`; the engine answers `session ws-<n>-<m> started`, optionally followed by a second line `Companion endpoint: /api/companion/ws-<n>-<m>` | `WebSocketContext.tsx`, `sessionParser.ts` |
| Keep-alive | the client sends `{"type":"ping","message":"keep alive","time":"<local time>"}` every 20 s; incoming `ping`/`pong` JSON frames are dropped before the console | `WebSocketContext.tsx` |
| Local rows | the UI appends its own JSON lifecycle rows `{"type":"info"|"error","message":…,"time":…}` for `connected`, `disconnected - (<code>) <reason>` and `already disconnected` | `WebSocketContext.tsx` |
| Message ids | monotonic per slot; `connectionEpoch` is the id of the `connected` row, so a changed epoch means a new backend session | `WebSocketContext.tsx` |
| Limits | the console keeps 200 rows (oldest evicted); command history 50 entries; authoring commands are refused above 63,488 characters (`MAX_BUFFER`, `graphActions/validation.ts`) | `config/playgrounds.ts` |

The provider auto-connects every configured playground when the app mounts (silently); the Start
and Stop buttons connect and disconnect with toasts. Sessions are not resumed: a reconnect is a new
engine session, a new id and an empty draft, which is why every hook resets on an epoch change.

## Sending commands

- `useWebSocket.sendCommand` trims the input, records it in the history (consecutive duplicates
  collapse), and sends it as one frame. Multi-line commands (`create node …`, `update node …`,
  `instantiate graph` with seed lines) travel with `\n` separators in that one frame; the engine
  echoes the first line followed by `...` (`> instantiate graph...`).
- The engine echoes every command it receives as a line starting with `> `. Commands handled
  locally (see below) are echoed by the UI itself with the same prefix, so the console reads the same.
- **Local interception**: `help`, `help {topic}` and `describe skill {built-in route}` resolve to a
  bundled help page and open the help panel without a round-trip (`utils/localHelpCommand.ts`).
  Anything else, including `describe node`, `describe connection`, `describe graph` and a skill
  without a bundled page, goes to the engine.
- `sendRawText` sends silently (no history, no local echo); every button-driven action uses it, and
  the backend echo is what shows in the console.
- JSON-Path's `load` sends the literal `load` and then the payload editor's text as a second frame
  (error row when the editor is empty). Its **Upload** button sends `upload`, waits for the line that
  carries `/api/json/content/{id}` and POSTs the payload there (see REST below).

### Commands the UI sends on the user's behalf

| Action | Command text | Built by |
|---|---|---|
| Session menu refresh / subscribe / unsubscribe / reset | `session`, `session subscribe {id}`, `session unsubscribe`, `session reset` | `session/useSessionCollaboration.ts` |
| Instantiate, mock input, Run (toolbar) | `instantiate graph`, `upload mock data`, `run` | `graphRun/graphRunProtocol.ts` (`GRAPH_RUN_COMMANDS`) |
| Create node (editor) | `create node {alias}` ⏎ `with type {Type}` (when set) ⏎ `with properties` ⏎ `{key}={value}` per row; a multi-line value is wrapped in `'''` lines | `graphActions/minigraphCommandBuilder.ts` |
| Edit node (editor) | `update node {originalAlias}` ⏎ same shape as create | same |
| Connect (drag or popover) | `connect {source} to {target} with {relation}` | same |
| Delete node (context menu, Delete key) | `delete node {alias}` | same |
| Delete connection(s) | `delete connection {a} and {b}` — the engine removes **every** relation between the pair in both directions, so the planner re-issues `connect` for the relations that must survive | `graphActions/connectionEdits.ts` |
| Clipboard paste / drop | a `create node …` or `update node …` command rebuilt from the clipped node (update when the alias already exists in the current graph) | `clipboard/paste.ts` |
| Undo | the compensating commands captured before the mutation: `delete node` for a create, the previous `update node …` for an edit, the previous `create node …` plus `connect …` lines for a delete, `delete connection` plus the surviving `connect` lines for a connection change | `graphActions/undoCommands.ts`, `hooks/useGraphUndo.ts` |
| Save Graph / Load Graph (header) | `export graph as {name}` (the bookmark is written only once the `graph.exported` line confirms it); `import graph from {name}` | `hooks/useSavedGraphWorkflow.ts` |

Every command string is validated and size-checked in the builder before it is sent; see
[graph-view.md](graph-view.md) for the validation rules.

## Classification: from a raw line to typed events

`classifyMessage(msgId, raw)` returns one or more `ProtocolEvent`s for every message; the kernel
emits them on the bus once, in rule order, and keeps a `classificationMap` for rendering. Rules, in
the order they run (`src/protocol/classifier.ts`):

| # | Event kind | Fires when the raw message… | Consumers |
|---|---|---|---|
| 1 | `lifecycle` | is JSON with a string `type` (`info`, `error`, `ping`, `welcome` are the known ones); carries `message` and `time`. Returns early. | Console icon rows |
| 2 | `json.response` | is any other JSON object or array (JSON-Path results, `inspect` output such as `{"output": …}`). Returns early. | Console JSON tree; ➡️ send to JSON-Path |
| 3 | `payload.large` | matches `Large payload (<bytes>) -> GET /api/inspect/<session>/<key>` | `useLargePayloadDownload` fetches it and appends the JSON inline |
| 4 | `upload.invitation` | matches `You may upload … -> POST /api/mock/<session>` (the reply to `upload mock data`) | The console row gets its "⬆ Upload JSON…" re-open button; nothing opens by itself — in a shared session the replayed invitation reaches every member |
| 5 | `upload.contentPath` | contains `/api/json/content/<id>` (`Please upload XML/JSON text to …`) | `useWebSocket` POSTs the payload (JSON-Path) |
| 6 | `graph.link` | is plain text containing `/api/graph/model/<id>` — `Graph with N nodes described in /api/graph/model/ws-…/n` after a mutation, or `Graph exported to …` ⏎ `Described in /api/graph/model/{name}/n` | The console row becomes a link: click re-fetches the live graph and opens the Graph tab |
| 6a | `graph.exported` | is a graph link that also contains `Graph exported to ` (the name is the 5th path segment) | `useGraphSaveName`, `useSavedGraphWorkflow` |
| 7 | `graph.mutation` | is plain non-echo text: `node <x> created|updated|deleted|connected to <y>|imported from …|overwritten by node from …` → `node-mutation`; `<a> -> <b> removed` → `node-mutation`; contains `graph model imported as draft` → `import-graph` | `useAutoGraphRefresh` (debounced / immediate re-fetch), `useGraphRunWorkflow` (invalidate), `useGraphUndo` |
| 7a | `graph.instance.created` | matches `Graph instance created. Loaded <n> mock entr(y|ies), model.ttl = <ms> ms` | Run workflow: instantiating → requesting input or ready |
| 7a | `graph.instance.cleared` | equals `Graph instance cleared` | Run workflow hard reset |
| 7a | `graph.run.terminal` | matches `Graph traversal completed in <n> ms` (completed) or starts with `Graph traversal aborted` (aborted; the reason follows) | Run workflow: running → idle, abort toast |
| 7a | `command.error` | matches `ERROR: <message>` | Run workflow failure toasts |
| 7b | `minigraph.nodeAction.textResult` | matches `node <x> created` / `already exists` / `updated` / `deleted` / `connected to <y>` / `not found`, `Source and target nodes must be different`, the `Syntax: connect …` line, or `ERROR: …` — with `status` accepted / rejected / error and the `action` | `useGraphAuthoring` (result matching, alias-scoped), undo push, toasts |
| 7c | `minigraph.createNode.textResult` | the create-node subset of 7b (compatibility) | older consumers |
| 7d | `session.reset` | equals `Session restarted` | Session hook (refresh), auto-refresh (clear graph), run workflow (hard reset) |
| 7e | `minigraph.session.started` | matches `session ws-… started` (+ optional `Companion endpoint: …` line) | Session hook: new session id, state reset |
| 7e | `minigraph.session.status` | first line `Session ws-… started since <timestamp>`, then optional `subscribed to ws-…` and `subscribed by [ws-…, ws-…]` | Session hook: authoritative reconcile |
| 7e | `minigraph.session.commandResult` | `Subscribed to ws-…`, `Session unsubscribed from ws-…` (accepted); `Session ws-… not found`, `ws-… is not a primary session`, `You have already subscribed to ws-…`, `You cannot subscribe to yourself`, `Nothing to unsubscribe`, `Invalid session command` (rejected) | Session hook |
| 7e | `minigraph.session.notification` | `ws-… subscribed to your session`, `ws-… unsubscribed from your session`, `Session ws-… has closed` | Session hook (peer deltas) |
| 8 | `command.echo` | starts with `> ` | run workflow (a typed `instantiate graph` / `run` moves the phase), save-name, authoring |
| 9 | `command.helpOrDescribe` | is an echo of `help…` or `describe …` other than `describe graph` | `useAutoHelpNavigate` opens the bundled page |
| 10 | `command.importGraph` | is an echo of `import graph from <name>` | `useGraphSaveName` (imported name) |
| 11a | `graph.export.failed` | contains `Invalid filename` or `Expect root node name` | save workflow |
| 11 | `docs.response` | is any remaining non-JSON text that is not an echo, a graph link, an upload line, a large-payload line or a session line | Console renders it as markdown |
| 12 | `unclassified` | nothing matched | Console plain row |

Properties of the design worth keeping:

- A line can produce several events (a `Graph exported to …` line is `graph.link` **and**
  `graph.exported`; an `ERROR:` line is `command.error` **and** a node-action error).
- Listeners for one kind fire in registration order; `Playground.tsx` registers the hooks in a
  deliberate order (session, graph data, auto-refresh, undo, authoring, run workflow).
- The kernel emits only *new* messages (watermark). Hooks that must also react to lines received
  before they mounted read the `classificationMap` backlog with their own watermark and dedupe by
  `kind:msgId` (the session hook does this so a `session` typed before the menu opened still counts).
- Session parsing ignores echoes (`> session`) and validates ids against `^ws-\d+-\d+$`.

## REST endpoints used by the webapp

All paths are relative to the page origin (the Vite dev server proxies `/api`, `/info`, `/health`
and `/env` to port 8085).

| Endpoint | Method | Used by | Notes |
|---|---|---|---|
| `/api/graph/session/{sessionId}` | GET | `useGraphData` | The live draft graph as JSON (`{nodes, connections}`, below). 404 for an unknown or closed session; zero nodes means "no graph yet" — both are quiet, the canvas shows its empty state. Re-fetched after every mutation. |
| `/api/mock/{sessionId}` | POST, `application/json` | `MockUploadPanel` / `useMockUpload` | The mock `input.body` of the current graph instance; the engine confirms in the console with `Mock data loaded into 'input.body' namespace`; with `?namespace=header` a JSON object of text values becomes the mock `input.header` (the panel posts its header rows after the body) and the engine confirms `Mock data loaded into 'input.header' namespace`. The toolbar's Upload step builds the path from this session's id; a console invitation row carries it too. The engine loads the payload into every member's instance of a collaborative session. |
| `/api/graph/import/{sessionId}` | POST, `application/json` | `useGraphFileImport` | A graph model from a file (`{nodes, connections?}`, no other top-level section) becomes the session's draft; the engine replays it to every member of a shared session and confirms in every console with `Graph model imported as draft` (the line that refreshes the view). 400 names the refused section or the importer's reason; 404 for an unknown session. |
| `/api/json/content/{id}` | POST, `application/json` | `useWebSocket` (JSON-Path) | The payload for the JSON-Path session after an `upload` command; the path comes from the `upload.contentPath` line. The body is re-serialised through `JSON.parse` first, so invalid JSON is refused client-side. |
| `/api/inspect/{sessionId}/{key}` | GET | `useLargePayloadDownload` | A namespace value too large for the console (the `Large payload (N) -> GET …` line); appended inline as a JSON row, and the JSON-Path playground can receive it. |
| `/api/graph/model/{name}/{n}` | GET | humans | The described temp model behind a console graph link (`export graph`, `describe graph`). The UI never renders from it. |
| `/api/companion/{sessionId}/sync` | POST, `text/plain` | AI agents, not the UI | Shown in the Session menu from the `Companion endpoint:` line; a companion drives the session with the same commands and the console shows its lines. The engine refuses `session subscribe/unsubscribe/reset` there. |
| `/info`, `/info/lib`, `/info/routes`, `/health`, `/env` | GET | `NavMenu` quick links | Open in a new tab. |

### The live graph JSON (`MinigraphGraphData`, `src/utils/graphTypes.ts`)

```json
{
  "nodes": [
    { "alias": "root", "types": ["Root"], "properties": { "name": "tutorial-3", "purpose": "…" } },
    { "alias": "fetcher", "types": ["Fetcher"], "properties": { "skill": "graph.api.fetcher", "dictionary": ["person-name"], "input": ["input.body.person_id -> person_id"] } }
  ],
  "connections": [
    { "source": "root", "target": "fetcher", "relations": [ { "type": "fetch", "properties": {} } ] }
  ]
}
```

`isMinigraphGraphData` only requires a `nodes` array; `connections` may be absent on a partial
graph. A connection object carries every relation between one ordered pair; list-valued properties
arrive as arrays and the editor prints them back as `key[]=` rows.

## Engine lines the help panel relies on

`describe skill {route}` is answered by the engine with the page `help {route}` (dots replaced by
hyphens, lowercased), which is why the UI can resolve it locally when the page is bundled. The
bundled set is the engine's own `help/*.md`; the Rust engine serves a mirror of the same files
([build-test-deploy.md](build-test-deploy.md)).
