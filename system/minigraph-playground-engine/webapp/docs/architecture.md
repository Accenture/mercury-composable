# Architecture

A single-page React application that talks to a MiniGraph engine over one WebSocket per playground
and a handful of REST endpoints. The engine owns every graph, instance and session; the webapp
mirrors what the engine acknowledges and turns gestures into the same console commands a user could
type. Nothing in the app infers graph state that the backend has not confirmed in a line of text.

## Stack

React 19 with the React Compiler (`babel-plugin-react-compiler`, so components are written without
manual memoisation for rendering concerns), TypeScript 5.9 in strict mode, Vite 8 (Rolldown),
react-router 8, `@xyflow/react` 12 for the canvas, `react-resizable-panels` for the layout,
`react-markdown` + `remark-gfm` for help pages, `react-json-view-lite` for JSON rows, `idb` for the
workspace clipboard. Tests: Vitest 4 with happy-dom and Testing Library. Versions and scripts:
[build-test-deploy.md](build-test-deploy.md).

## Source layout

```
src/
  main.tsx, App.tsx          entry; one <Route> per playground from config
  config/playgrounds.ts      the two playground declarations, feature flags, limits, sample payloads
  contexts/
    WebSocketContext.tsx     navigation-persistent sockets, one slot per wsPath; message log; ping
    ClipboardContext.tsx     workspace clipboard over IndexedDB + BroadcastChannel
  protocol/
    events.ts                the typed event union (discriminated on `kind`)
    classifier.ts            classifyMessage(): one raw backend line -> ProtocolEvent[]
    bus.ts                   ProtocolBus: synchronous typed emitter
    useProtocolKernel.ts     classifies new messages once and emits them on the bus
  utils/messageParser.ts     the regexes behind the classifier (graph links, uploads, mutations, node results)
  session/                   session collaboration: parser of the session text lines + the view-model hook
  graphRun/                  run-workflow protocol helpers (command text, terminal lines, input.body hints)
  hooks/                     one hook per concern (socket, graph data, auto-refresh, run workflow, undo, help, ...)
  graphActions/              pure authoring helpers: command builder, validation, connection edits, undo recipes
  clipboard/                 pure clipboard helpers: db schema, sorting, paste plans, drag payloads
  components/                Playground.tsx (the orchestrator) and the UI components
    GraphView/               the React Flow canvas, node renderer, context menus, minimap
    GraphAuthoring/          useGraphAuthoring: the create/edit/connect session state machine
  utils/graphTransformer.ts  layout + edge routing from MinigraphGraphData to React Flow nodes/edges
  data/helpContent.ts        the help pages, compiled in from ../src/main/resources/help/*.md
```

## Runtime layers

```
 engine (Java or Rust)  ──WebSocket text lines──▶  WebSocketContext slot (messages[], phase, epoch)
                                                        │ messages
                                                        ▼
                                              useProtocolKernel ──▶ classifyMessage ──▶ ProtocolBus.emit
                                                        │                                   │ typed events
                                                        ▼                                   ▼
                                               classificationMap            hooks: useAutoGraphRefresh, useGraphRunWorkflow,
                                              (msgId -> events,             useSessionCollaboration, useGraphAuthoring,
                                               for console rendering)       useGraphUndo, useAutoHelpNavigate, useLargePayloadDownload,
                                                                            useMockUploadPanel, useGraphSaveName, ...
                                                        │                                   │
                                                        ▼                                   ▼
                                                   Console rows                  state + commands back to the engine
                                                                                 (sendRawText) and REST fetches
```

1. **Transport** — `WebSocketContext` lives *above* the router, so sockets survive navigation between
   playgrounds. Each `wsPath` has a slot: `phase` (`idle` | `connecting` | `connected`), a monotonic
   message id, the message log (capped at 200), and `connectionEpoch` (the id of the "connected" row —
   a new epoch means a new backend session). The provider auto-connects every configured playground
   on mount; the Start/Stop button connects with toasts. Keep-alive pings go out every 20 s and
   ping/pong frames never reach the console.
2. **Protocol kernel** — `useProtocolKernel` classifies every new message exactly once
   (watermark on the message id) and emits the resulting typed events on the playground's
   `ProtocolBus`. `classificationMap` covers *all* visible messages so `ConsoleMessage` can render a
   row by its classification. The classifier is pure and is the only place that knows the backend's
   text patterns ([protocol.md](protocol.md)).
3. **Hooks** — each concern subscribes to the bus and keeps its own small state machine; the
   subscriptions are the integration points. The engine is authoritative: a hook moves to a new
   state only on an acknowledged line (instance created, mutation confirmed, session status) and
   treats a graph mutation while a command is outstanding as "outcome uncertain" because the text
   protocol has no correlation ids.
4. **Components** — `Playground.tsx` composes everything for one playground from its config; the
   components below it are presentational or own only local UI state
   ([components.md](components.md), [graph-view.md](graph-view.md)).

## Playground.tsx, the orchestrator

For one `PlaygroundConfig` it creates the `ProtocolBus`, runs the kernel, and wires the hooks:

- `useWebSocket` (console state, history, the `load`/`upload` handshakes, local interception of
  bundled help commands) and `useProtocolKernel`.
- `useSessionCollaboration` first, because the session id it captures from the `session` round-trip
  on mount is what addresses the live graph endpoint `GET /api/graph/session/{id}`.
- `useGraphData` (fetch + reveal the Graph tab) and `useAutoGraphRefresh` (re-fetch on mutation
  events; clear on session reset).
- `useGraphUndo` with pre-mutation snapshots captured in refs, pushed as compensating commands
  only once the backend accepts the mutation.
- `useGraphAuthoring` with a `createGraphAuthoringExecutor(ws.sendRawText)`; the create/edit panel
  and the connection popover render from its state.
- `useGraphRunWorkflow` (Instantiate / Run), `useMockUploadPanel` (the upload form in the left
  slot), `useGraphFileImport` (a graph file as the draft), `useGraphSetPanel` (the graph-set panel
  in the left slot), `useLargePayloadDownload`, `useAutoHelpNavigate`, `useSavedGraphs` +
  `useGraphSaveName` + `useSavedGraphWorkflow`, `useSendToJsonPath`, the clipboard context.

Layout: a horizontal `react-resizable-panels` group — the **left slot** (the node editor, or the
mock-upload form, or the graph-set panel, or the console, in that priority), the **right panel**
(tabs: graph / graph data / payload, with the help browser splitting it vertically) and the optional
**workspace sidebar**. Default widths are one third for the console (30% for the in-place cards),
20% for the sidebar, the right panel takes the
complement; the split is deliberately not persisted across page loads (a saved drag kept looking like
a wrong default). On viewports narrower than 768 px the group stacks vertically.

## State ownership

| State | Owner | Persistence |
|---|---|---|
| Socket phase, message log, connection epoch, pending cross-playground payload | `WebSocketContext` | memory (survives navigation) |
| Command text, history cursor, draft | `useWebSocket` local reducer | memory; history in `localStorage` (50 entries) |
| Payload editor text | `Playground` via `useLocalStorage` | `localStorage` per playground |
| Live graph (`MinigraphGraphData`), selected right tab | `useGraphData` | memory; tab in `localStorage` |
| Session id, subscriptions, pending session command | `useSessionCollaboration` | memory (reset on reconnect) |
| Run phase (idle → instantiating → ready → running) | `useGraphRunWorkflow` | memory |
| Authoring session (create / edit / connect form, phase, server message) | `useGraphAuthoring` | memory |
| Undo stack of compensating commands | `useGraphUndo` | memory |
| Help topic, help/console/sidebar open flags, minimap toggle, compact nodes | `useLocalStorage` keys | `localStorage` |
| Saved graph snapshots, untitled counter | `useSavedGraphs`, `useGraphSaveName` | `localStorage` |
| Workspace clipboard items | `ClipboardContext` | IndexedDB (shared across tabs via BroadcastChannel) |

The complete key list is in [hooks-and-state.md](hooks-and-state.md).

## Design decisions that must survive a change

1. **The backend is the source of truth; the UI mirrors acknowledgements.** Graph, instance and
   session state move only on classified backend lines. This is why hooks quarantine an outstanding
   command after a mutation (`outcome-uncertain`) instead of guessing.
2. **Every UI mutation is a console command.** The node editor, the connection popover, drag-to-
   connect, delete keys, clipboard paste and undo all build text through
   `graphActions/minigraphCommandBuilder.ts` and send it with `sendRawText`. The engine stays a
   lightweight command executor and a collaborator sees the same commands in its own console.
3. **Undo is compensation, not rollback.** A mutation's inverse (delete the created node; re-issue
   the previous `update node`; reconnect the removed relations) is captured *before* the command and
   pushed *after* the backend accepts it. Ctrl/Cmd+Z and the toast's Undo button replay it.
4. **The live graph comes from `GET /api/graph/session/{id}`**, never from the `/api/graph/model/…`
   links in the console: those describe temp files for humans. Mutations re-fetch the live endpoint
   (debounced 300 ms for node commands, immediately for an import).
5. **One classifier, pure, fixture-tested.** New backend lines get a classifier rule and a fixture
   entry; components never regex the raw text themselves.
6. **`WebSocketProvider` stays above `<BrowserRouter>`** and socket instances live in refs, never in
   React state: moving either breaks navigation-persistent connections or causes render storms.
7. **Help is bundled.** The help panel works before connecting and `help`/`describe skill` for a
   bundled topic never round-trips; the price is that a help edit needs a bundle release
   ([build-test-deploy.md](build-test-deploy.md)).
8. **Equal partners in a session.** A subscribed session can run the graph; the engine runs a
   subscriber's command through the primary in every member's session. The only role-specific UI
   is the Session menu (subscribe / unsubscribe / reset).

## Where to go next

- The exact lines and patterns: [protocol.md](protocol.md).
- Component by component: [components.md](components.md) and [graph-view.md](graph-view.md).
- Hooks, state machines and storage keys: [hooks-and-state.md](hooks-and-state.md).
- Changing things safely: [extending.md](extending.md).
