# Minigraph Playground — Codebase Walkthrough & Knowledge Transfer

**Audience:** Engineers inheriting or maintaining this webapp  
**Branch:** `feature/playground`  
**Last updated:** March 23 2026

---

## Table of Contents

1. [What This App Does](#1-what-this-app-does)
2. [Technology Stack & Tooling](#2-technology-stack--tooling)
3. [High-Level Features](#3-high-level-features)
   - 3.1 [Multi-Playground Architecture](#31-multi-playground-architecture)
   - 3.2 [Navigation-Persistent WebSocket Connections](#32-navigation-persistent-websocket-connections)
   - 3.3 [Interactive Console (REPL-like)](#33-interactive-console-repl-like)
   - 3.4 [Graph Visualisation](#34-graph-visualisation)
   - 3.5 [Auto-Graph Refresh](#35-auto-graph-refresh)
   - 3.6 [Markdown Preview (Developer Guides)](#36-markdown-preview-developer-guides)
   - 3.7 [Large Payload Handling](#37-large-payload-handling)
   - 3.8 [JSON-Path ↔ Minigraph Cross-Playground Routing](#38-json-path--minigraph-cross-playground-routing)
   - 3.9 [Two-Step Payload Upload (REST Handshake)](#39-two-step-payload-upload-rest-handshake)
   - 3.10 [Saved Graph Bookmarks](#310-saved-graph-bookmarks)
   - 3.11 [Keep-Alive Pings](#311-keep-alive-pings)
   - 3.12 [Mock-Data Upload Modal](#312-mock-data-upload-modal)
   - 3.13 [Save-Name Management](#313-save-name-management)
4. [Repository Layout](#4-repository-layout)
5. [Architecture Overview](#5-architecture-overview)
6. [Layer-by-Layer Walkthrough](#6-layer-by-layer-walkthrough)
   - 6.1 [Entry Point & Routing](#61-entry-point--routing)
   - 6.2 [Playground Configuration](#62-playground-configuration)
   - 6.3 [WebSocket State — Context + Hook](#63-websocket-state--context--hook)
   - 6.4 [Playground.tsx — The Orchestrator](#64-playgroundtsx--the-orchestrator)
   - 6.5 [Left Panel — Console & Command Input](#65-left-panel--console--command-input)
   - 6.6 [Right Panel — Tabs](#66-right-panel--tabs)
   - 6.7 [Graph Pipeline](#67-graph-pipeline)
   - 6.8 [Automation Hooks](#68-automation-hooks)
   - 6.9 [Utilities](#69-utilities)
   - 6.10 [Navigation Bar](#610-navigation-bar)
   - 6.11 [Saved Graphs](#611-saved-graphs)
7. [Key Engineering Decisions](#7-key-engineering-decisions)
8. [Data & Message Flows](#8-data--message-flows)
   - 8.1 [User Sends a Command](#81-user-sends-a-command)
   - 8.2 [User Pins a Graph](#82-user-pins-a-graph)
   - 8.3 [Auto-Refresh After Mutation](#83-auto-refresh-after-mutation)
   - 8.4 [Large Payload Flow](#84-large-payload-flow)
   - 8.5 [REST Upload Handshake](#85-rest-upload-handshake)
   - 8.6 [Mock-Data Upload Flow](#86-mock-data-upload-flow)
   - 8.7 [Save-Name Lifecycle](#87-save-name-lifecycle)
9. [State Ownership Map](#9-state-ownership-map)
10. [Build, Dev & Deploy](#10-build-dev--deploy)
11. [Extending the App](#11-extending-the-app)
12. [Pitfalls & Gotchas](#12-pitfalls--gotchas)

---

## 1. What This App Does

The **Minigraph Playground** is a React-based developer tool that communicates with a Java Spring Boot backend over **WebSocket** and **REST**. It provides two interactive playgrounds:

| Playground | URL | Purpose |
|---|---|---|
| **Minigraph** | `/` | CRUD operations on a node-and-edge graph model via text commands |
| **JSON-Path** | `/json-path` | Evaluate JSON-Path expressions against a JSON payload |

The UI offers:
- A **live WebSocket console** (like a REPL) where commands are sent and responses printed
- A **ReactFlow graph visualiser** that renders the current graph model
- A **Markdown preview panel** for `help` and `describe` command responses
- A **JSON/XML payload editor** for the JSON-Path playground
- **Saved graph bookmarks** (export/import graph snapshots by name)
- **Large payload handling** — payloads exceeding 64 KB are automatically fetched via REST and displayed inline
- **Mock-data upload modal** — when the server responds to a command with an upload invitation, a modal dialog opens automatically so the user can paste, drag-and-drop, or browse a JSON file and POST it to the provided endpoint

The built output (`dist/`) is copied into the Java application's `src/main/resources/public/` directory and served as a static SPA by the same Spring Boot server that provides the WebSocket and REST endpoints.

---

## 2. Technology Stack & Tooling

| Concern | Choice | Notes |
|---|---|---|
| Framework | **React 19** | Uses the new React Compiler (`babel-plugin-react-compiler`) |
| Language | **TypeScript 5.9** | Strict, no implicit any |
| Build tool | **Vite 6** | Fast HMR in dev; Rollup-based production bundle |
| Routing | **react-router-dom v7** | `BrowserRouter` + `<Routes>` |
| Graph renderer | **@xyflow/react v12** | ReactFlow — nodes, edges, minimap, controls |
| Panel layout | **react-resizable-panels v4** | Draggable left/right split |
| Markdown | **react-markdown + remark-gfm** | GitHub-flavoured Markdown |
| JSON viewer | **react-json-view-lite** | Collapsible inline JSON tree |
| Styling | **CSS Modules** | Per-component `.module.css` files; global resets in `index.css` |
| State persistence | **localStorage** | Via custom `useLocalStorage` hook |
| Dev proxy | **Vite server.proxy** | `/ws`, `/api`, `/health`, `/info`, `/env` → `localhost:8085` |

### React Compiler

The project opts in to the **React Compiler** (`babel-plugin-react-compiler`), which automatically inserts memoisation. This means you will rarely see manual `useMemo` or `useCallback` calls needed purely for performance — they are present only where the semantics require a stable reference (e.g. a ref updated in an effect, an empty-dep-array `useCallback`). Do not add gratuitous memoisation; let the compiler do its job.

---

## 3. High-Level Features

### 3.1 Multi-Playground Architecture
The app supports multiple independent playgrounds configured entirely in one file (`src/config/playgrounds.ts`). Adding a new playground requires no changes to routing, navigation, or state management — only a new entry in `PLAYGROUND_CONFIGS`.

**Key code locations:**
- `src/config/playgrounds.ts` [L33–L56](../src/config/playgrounds.ts#L33) — `PlaygroundConfig` interface defining every per-playground property
- `src/config/playgrounds.ts` [L57–end](../src/config/playgrounds.ts#L57) — `PLAYGROUND_CONFIGS` array (the only file to edit for a new playground)
- `src/App.tsx` [L21–L23](../src/App.tsx#L21) — routes auto-generated: `PLAYGROUND_CONFIGS.map((cfg) => <Route … element={<Playground config={cfg} />} />)`
- `src/components/Navigation.tsx` [L102–L143](../src/components/Navigation.tsx#L102) — nav links and status dots also auto-generated from the same array

---

### 3.2 Navigation-Persistent WebSocket Connections
Switching between the Minigraph and JSON-Path playgrounds does **not** close either WebSocket connection. Each connection is owned by `WebSocketContext` which lives above `<Routes>` in the tree, so both sockets stay alive simultaneously. The navigation bar shows a live status dot per playground.

**Key code locations:**
- `src/App.tsx` [L17–L29](../src/App.tsx#L17) — `<WebSocketProvider>` wraps `<BrowserRouter>`, not the other way around
- `src/contexts/WebSocketContext.tsx` [L1–L10](../src/contexts/WebSocketContext.tsx#L1) — explains the "above Routes" contract in the module JSDoc
- `src/contexts/WebSocketContext.tsx` [L30](../src/contexts/WebSocketContext.tsx#L30) — `WsPhase = 'idle' | 'connecting' | 'connected'`
- `src/contexts/WebSocketContext.tsx` [L140–L165](../src/contexts/WebSocketContext.tsx#L140) — `useReducer`-managed `slots` map keyed by `wsPath`; `wsRefs` / `pingRefs` / `msgIdRefs` live in `useRef` outside the reducer
- `src/contexts/WebSocketContext.tsx` [L196–L253](../src/contexts/WebSocketContext.tsx#L196) — `connect()`: opens socket, sends `welcome`, starts ping interval
- `src/components/Navigation.tsx` [L21–L33](../src/components/Navigation.tsx#L21) — `aggregateDotStatus()` + `phaseToDotStatus()` derive nav dot colours

---

### 3.3 Interactive Console (REPL-like)
- Messages are printed in real time with type-based icons (ℹ️ ❌ 👋)
- JSON messages are rendered as a collapsible tree (`react-json-view-lite`)
- Graph-link messages (🕸️) are clickable to pin the graph
- Plain-text messages (📌) are clickable to pin them to the Markdown Preview
- Per-row copy button; command history navigation with ↑/↓ arrow keys
- Multiline input mode toggle for multi-line commands

**Key code locations:**
- `src/components/Console/ConsoleMessage.tsx` [L24–L42](../src/components/Console/ConsoleMessage.tsx#L24) — per-message classification: `isGraphLink`, `isLargePayload`, `isPinnable`, `canSendToJsonPath`
- `src/components/Console/ConsoleMessage.tsx` [L70–L149](../src/components/Console/ConsoleMessage.tsx#L70) — JSX: `<JsonView>` for JSON, plain `<span>` for text, copy button, ➡️ send-to-JSON-Path button
- `src/components/Console/ConsoleMessage.tsx` [L29–L31](../src/components/Console/ConsoleMessage.tsx#L29) — `isGraphLinkMessage` / `isLargePayloadMessage` / `isPinnable` flags drive rendering branches
- `src/hooks/useWebSocket.ts` [L143–L163](../src/hooks/useWebSocket.ts#L143) — `sendCommand()`: sends text, pushes to history, handles special `load` command
- `src/hooks/useWebSocket.ts` [L165–L189](../src/hooks/useWebSocket.ts#L165) — `handleKeyDown()`: ↑/↓ history navigation with draft-save/restore (`ENTER_HISTORY` / `EXIT_HISTORY` / `SET_HISTORY_INDEX` reducer actions)
- `src/hooks/useWebSocket.ts` [L11–L32](../src/hooks/useWebSocket.ts#L11) — `LocalState` + `LocalAction` types; `draftCommand` field saves in-progress input on first ↑ press

---

### 3.4 Graph Visualisation
- Fetched via REST (`GET /api/graph/model/{id}`) after the server emits a graph-link in the WebSocket stream
- Rendered with **ReactFlow**: nodes are colour-coded by type (`entry_point`, `api_fetcher`, `mapper`, `terminator`), edges show relation labels
- Nodes are **resizable** (NodeResizer) and can be re-arranged interactively
- A **minimap** provides navigation for large graphs
- A **refreshing overlay** (spinner) is displayed during background re-fetches without clearing the existing graph

**Key code locations:**
- `src/utils/graphTypes.ts` [L1–L47](../src/utils/graphTypes.ts#L1) — `MinigraphGraphData`, `MinigraphNode`, `MinigraphConnection` types + `isMinigraphGraphData()` type guard
- `src/hooks/useGraphData.ts` [L53–L101](../src/hooks/useGraphData.ts#L53) — initial-load path: `fetch(pinnedGraphPath)` → `setGraphData(json)` → `setRightTab('graph')` (L93); clears `graphData` to `null` on path change (L83)
- `src/hooks/useGraphData.ts` [L106–L137](../src/hooks/useGraphData.ts#L106) — `refetchGraph()`: overlay-mode re-fetch; does NOT clear `graphData` (stale graph stays visible), sets `isRefreshing = true` (L116)
- `src/utils/graphTransformer.ts` [L61–L76](../src/utils/graphTransformer.ts#L61) — `NODE_ACCENT` colour map + `nodeStyle()` applying `--node-accent` CSS custom property
- `src/utils/graphTransformer.ts` [L83–L157](../src/utils/graphTransformer.ts#L83) — `computeLayout()`: BFS topological layout (levels = columns, stacked vertically within each level)
- `src/utils/graphTransformer.ts` [L163–L205](../src/utils/graphTransformer.ts#L163) — `transformGraphData()`: converts `MinigraphGraphData` → ReactFlow `nodes[]` + `edges[]`
- `src/components/GraphView/NodeTypes.tsx` [L9–L16](../src/components/GraphView/NodeTypes.tsx#L9) — `TYPE_META` icon/label map per node type
- `src/components/GraphView/NodeTypes.tsx` [L55–L88](../src/components/GraphView/NodeTypes.tsx#L55) — `MinigraphNode`: renders as `<Fragment>` (no wrapper div), `NodeResizer` + `Handle` + content as siblings (L60–L86)
- `src/components/GraphView/NodeTypes.tsx` [L96–L103](../src/components/GraphView/NodeTypes.tsx#L96) — `nodeTypes` export map used by `<ReactFlow nodeTypes={nodeTypes}>`
- `src/components/GraphView/GraphView.tsx` [L115–L157](../src/components/GraphView/GraphView.tsx#L115) — `<ReactFlow>` with `fitView`, `<Controls>`, `<MiniMap>` (L130), and `isRefreshing` overlay (L144–L152)

---

### 3.5 Auto-Graph Refresh
After any graph-mutating command (`create node`, `delete node`, `connect`, etc.), the graph automatically re-fetches and re-renders without user interaction. If no graph was previously loaded, the app issues `describe graph` silently, receives the graph-link, and opens the Graph tab automatically. This is debounced at 300 ms to collapse rapid-fire commands.

**Key code locations:**
- `src/utils/messageParser.ts` [L237–L265](../src/utils/messageParser.ts#L237) — `detectMutation()`: classifies a raw message as `'node-mutation'`, `'import-graph'`, or `null`; includes the critical `startsWith('node ')` prefix guard
- `src/hooks/useAutoGraphRefresh.ts` [L89–L102](../src/hooks/useAutoGraphRefresh.ts#L89) — three core refs: `watermarkRef` (prevents replaying history), `debounceTimerRef` (300 ms collapse), `waitingForDescribeRef` (two-step describe-graph/graph-link handshake)
- `src/hooks/useAutoGraphRefresh.ts` [L112–L131](../src/hooks/useAutoGraphRefresh.ts#L112) — disconnect guard: clears `waitingForDescribeRef` and cancels any pending debounce on socket close
- `src/hooks/useAutoGraphRefresh.ts` [L143–L250](../src/hooks/useAutoGraphRefresh.ts#L143) — main `useEffect`: Pass 1 consumes pending graph-link when `waitingForDescribeRef` is true; Pass 2 detects mutations and fires the debounce or immediate `describe graph` send
- `src/hooks/useWebSocket.ts` [L246–L249](../src/hooks/useWebSocket.ts#L246) — `sendRawText()`: sends without echoing to console or history (used exclusively by automation hooks)

---

### 3.6 Markdown Preview (Developer Guides)
After `help` or text-producing `describe` commands, the response is automatically pinned to the **Developer Guides** tab and the tab switches into view. The panel renders GitHub-flavoured Markdown.

**Key code locations:**
- `src/utils/messageParser.ts` [L192–L210](../src/utils/messageParser.ts#L192) — `isHelpOrDescribeCommand()`: matches `"> help …"` and `"> describe <non-graph>"` echoes (explicitly excludes `"> describe graph"`)
- `src/hooks/useAutoMarkdownPin.ts` [L42–L46](../src/hooks/useAutoMarkdownPin.ts#L42) — `isPinnableResponse()`: a message is pinnable if it is not an echo (`> ` prefix), not a graph-link, and passes `isMarkdownCandidate`
- `src/hooks/useAutoMarkdownPin.ts` [L88–L163](../src/hooks/useAutoMarkdownPin.ts#L88) — main hook: `watermarkRef`, `waitingForResponseRef`; armed when a `help`/`describe` echo is seen; pins first pinnable response and calls `onAutoPin()` to switch the tab
- `src/utils/messageParser.ts` [L80–L92](../src/utils/messageParser.ts#L80) — `isMarkdownCandidate()`: true for non-JSON strings; false for JSON lifecycle events (`{type, message, time}`)

---

### 3.7 Large Payload Handling
When the server reports a payload exceeding 64 KB (`"Large payload (N) -> GET /api/inspect/…"`), the hook fetches it via REST and appends the result directly to the console as a collapsible JSON row — identical in appearance to small payloads.

**Key code locations:**
- `src/utils/messageParser.ts` [L148–L168](../src/utils/messageParser.ts#L148) — `extractLargePayloadLink()`: regex parses byte size and API path from the server notification; `isLargePayloadMessage()` (L167) is the boolean predicate
- `src/hooks/useLargePayloadDownload.ts` [L56–L64](../src/hooks/useLargePayloadDownload.ts#L56) — three key refs: `watermarkRef`, `abortRef` (AbortController for in-flight cancellation), `isFetchingRef` (re-entrancy guard)
- `src/hooks/useLargePayloadDownload.ts` [L92–L167](../src/hooks/useLargePayloadDownload.ts#L92) — main effect: scans new messages for `extractLargePayloadLink`, fetches the endpoint, pretty-prints JSON, calls `appendMessage(content)` (L142); `break` on first match (L165) prevents concurrent fetches
- `src/config/playgrounds.ts` [L27](../src/config/playgrounds.ts#L27) — `MAX_BUFFER = 63_488` — the 62 KB send limit that makes large-payload handling necessary

---

### 3.8 JSON-Path ↔ Minigraph Cross-Playground Routing
A JSON response in the Minigraph console can be sent directly to the JSON-Path Playground payload editor with one click (➡️ button), navigating automatically and pre-filling the editor.

**Key code locations:**
- `src/components/Console/ConsoleMessage.tsx` [L41](../src/components/Console/ConsoleMessage.tsx#L41) — `canSendToJsonPath = !!onSendToJsonPath && jsonCheck.isJSON` — button only shown on JSON messages when the callback is wired
- `src/components/Console/ConsoleMessage.tsx` [L63–L67](../src/components/Console/ConsoleMessage.tsx#L63) — `handleSendToJsonPath`: pretty-prints the JSON and calls `onSendToJsonPath(pretty)`
- `src/components/Playground.tsx` [L200–L211](../src/components/Playground.tsx#L200) — `handleSendToJsonPath`: calls `ctx.setPendingPayload(wsPath, json)` then `navigate(jsonPathConfig.path)`
- `src/contexts/WebSocketContext.tsx` [L59–L64](../src/contexts/WebSocketContext.tsx#L59) — `setPendingPayload` / `takePendingPayload` interface; backed by `useState` (not `useRef`) so that depositing triggers a re-render in the consuming playground
- `src/components/Playground.tsx` [L43–L56](../src/components/Playground.tsx#L43) — receiving side: `payloadOverride` state initialised from `ctx.takePendingPayload(wsPath)` at mount; `useEffect` (L50–L57) also fires reactively when a new payload is deposited into an already-mounted playground

---

### 3.9 Two-Step Payload Upload (REST Handshake)
The JSON-Path Playground supports uploading large JSON payloads over REST:
1. The user clicks **Upload** → the hook sends `"upload"` over the WebSocket
2. The server responds with `"Please upload XML/JSON text to /api/json/content/{id}"`
3. The hook detects that URL in the message stream and fires `POST /api/json/content/{id}` with the payload

**Key code locations:**
- `src/utils/messageParser.ts` [L118–L123](../src/utils/messageParser.ts#L118) — `extractUploadPath()`: regex extracts `/api/json/content/{id}` from the server's reply
- `src/hooks/useWebSocket.ts` [L123](../src/hooks/useWebSocket.ts#L123) — `pendingUploadRef = useRef(false)` — arms the two-step handshake
- `src/hooks/useWebSocket.ts` [L232–L241](../src/hooks/useWebSocket.ts#L232) — `uploadPayload()`: sets `pendingUploadRef.current = true`, sends `"upload"` over the socket
- `src/hooks/useWebSocket.ts` [L191–L230](../src/hooks/useWebSocket.ts#L191) — `useEffect` watching `messages`: when `pendingUploadRef` is true and `extractUploadPath` matches, fires `fetch(uploadPath, { method: 'POST', body })` (L200–L225)

---

### 3.10 Saved Graph Bookmarks
The Minigraph playground lets users save a named graph snapshot. The name is stored in localStorage and sent to the server via `export graph as {name}`. Loading re-issues `import graph from {name}`, and the auto-refresh hook re-renders the graph.

**Key code locations:**
- `src/hooks/useSavedGraphs.ts` [L19–L28](../src/hooks/useSavedGraphs.ts#L19) — `UseSavedGraphsReturn` interface: `savedGraphs`, `saveGraph`, `deleteGraph`, `hasGraph`
- `src/hooks/useSavedGraphs.ts` [L55–L82](../src/hooks/useSavedGraphs.ts#L55) — implementation: `useLocalStorage<SavedGraphsMap>` backing store; `useMemo` sort newest-first (L78)
- `src/components/Playground.tsx` [L165–L173](../src/components/Playground.tsx#L165) — `handleSaveGraph()`: calls `savedGraphs.saveGraph(name)` then `ws.sendRawText('export graph as ${name}')`
- `src/components/Playground.tsx` [L176–L182](../src/components/Playground.tsx#L176) — `handleLoadGraph()`: calls `ws.sendRawText('import graph from ${name}')` — auto-refresh hook takes it from there
- `src/components/GraphSaveButton/GraphSaveButton.tsx` [L1–L116](../src/components/GraphSaveButton/GraphSaveButton.tsx#L1) — self-contained inline save form (open/close, pre-filled name, overwrite warning, Enter/Escape keyboard handling)
- `src/components/SavedGraphsMenu/SavedGraphsMenu.tsx` [L1–L85](../src/components/SavedGraphsMenu/SavedGraphsMenu.tsx#L1) — dropdown list reusing `NavMenu`; Load/Delete actions per entry

---

### 3.11 Keep-Alive Pings
Every 20 seconds the client sends `{"type":"ping","message":"keep alive"}` over each open socket. Pong responses and outgoing pings are silently filtered — they never appear in the console.

**Key code locations:**
- `src/config/playgrounds.ts` [L30](../src/config/playgrounds.ts#L30) — `PING_INTERVAL = 20_000` — the only place to change the frequency
- `src/contexts/WebSocketContext.tsx` [L227–L231](../src/contexts/WebSocketContext.tsx#L227) — `setInterval` started in `ws.onopen`; fires `ws.send(eventWithTimestamp('ping', 'keep alive'))`
- `src/contexts/WebSocketContext.tsx` [L187–L196](../src/contexts/WebSocketContext.tsx#L187) — `isKeepAliveMessage()`: checks `parsed.type === 'ping' || 'pong'`; messages passing this check are dropped before `dispatch(MESSAGE_RECEIVED)` (L234–L239)

---

### 3.12 Mock-Data Upload Modal
When the server responds to a command (e.g. `upload mock data`) with the message `"You may upload JSON payload -> POST /api/mock/{id}"`, the app automatically opens a modal dialog. The user can paste JSON directly, drag-and-drop a `.json` file onto a drop zone, or click "Browse file…" to open the system file-picker. Submitting POSTs the payload to the provided URL.

The console row for the invitation displays a ⬆️ icon and an "⬆️ Upload JSON…" re-open button so the modal can be recalled after it has been dismissed. A ✅ badge appears on the row after a successful upload (session-only).

**Two triggers, one modal:** The modal is opened either (a) automatically by `useAutoMockUpload` when the server message arrives, or (b) manually via the re-open button on the console row. Both paths call the same `handleOpenUploadModal` callback in `Playground.tsx`.

**Key code locations:**
- `src/utils/messageParser.ts` [L182–L193](../src/utils/messageParser.ts#L182) — `extractMockUploadPath()` / `isMockUploadMessage()`: regex extracts `/api/mock/{id}` from the server's invitation message
- `src/hooks/useAutoMockUpload.ts` — watches the message stream for `isMockUploadMessage`, follows the watermark pattern, calls `onOpenModal(uploadPath)` on first match per batch
- `src/hooks/useMockUpload.ts` — owns the `fetch` lifecycle: `AbortController` per attempt, `isUploading` state, `upload()` / `cancel()` functions
- `src/components/MockUploadModal/MockUploadModal.tsx` — native `<dialog>` with `.showModal()`; textarea + drop zone + "Browse file…" button; `Ctrl+Enter` / `⌘+Enter` submit shortcut; inline JSON validation via `tryParseJSON`
- `src/components/MockUploadModal/MockUploadModal.module.css` — amber accent theming; drop zone active state; spinner; `--warning-color` token for file/validation errors
- `src/components/Playground.tsx` [L121–L127](../src/components/Playground.tsx#L121) — `modalUploadPath` / `modalTriggerRef` / `successfulUploadPaths` state
- `src/components/Playground.tsx` [L168–L200](../src/components/Playground.tsx#L168) — `handleOpenUploadModal`, `handleCloseUploadModal`, `handleUploadSuccess`, `handleUploadError` callbacks + `useAutoMockUpload` invocation
- `src/components/Console/ConsoleMessage.tsx` [L41–L50](../src/components/Console/ConsoleMessage.tsx#L41) — `isMockUpload`, `mockUploadPath`, `canUploadMock`, `uploadSucceeded` derived flags; `isPinnable` guard (`&& !isMockUpload`) prevents a nested-interactive-element accessibility violation
- `src/hooks/useAutoMarkdownPin.ts` — `isPinnableResponse()` excludes `isMockUploadMessage` rows so the invitation is never accidentally pinned to the Developer Guides tab

---

### 3.13 Save-Name Management
When the user opens the **💾 Save Graph** inline form, the input is pre-filled according to a strict priority chain:

| Priority | Source | Condition |
|---|---|---|
| 1 | `lastSavedName` | The working graph was previously saved this session |
| 2 | `importedName` | The graph was loaded via `import graph from {name}` |
| 3 | `untitled-{n}` | Fallback — monotonically incrementing per-playground counter |

A new import always supersedes a previous save: when a new `import graph from {name}` echo is detected in the message stream, `lastSavedName` is cleared to `null` immediately, ensuring the imported name wins on the next form open.

**Untitled counter semantics.** The counter only advances when the current `untitled-{n}` slot has actually been *used as a save name*. Clearing the console without ever saving reuses the same slot — so `untitled-2` will never appear in storage unless `untitled-1` was saved first. The counter is persisted in localStorage (keyed per playground) so it survives page refreshes.

**Key code locations:**
- `src/hooks/useGraphSaveName.ts` [L9–L38](../src/hooks/useGraphSaveName.ts#L9) — `UseGraphSaveNameReturn` interface with documented priority order and counter invariant
- `src/hooks/useGraphSaveName.ts` [L68](../src/hooks/useGraphSaveName.ts#L68) — `useLocalStorage<number>(storageKey, 1)` — counter persisted, starts at 1
- `src/hooks/useGraphSaveName.ts` [L80–L83](../src/hooks/useGraphSaveName.ts#L80) — `untitledSlotConsumedRef = useRef(false)` — tracks whether the current slot has been used; stored as a ref (not state) because it never drives a re-render
- `src/hooks/useGraphSaveName.ts` [L104–L118](../src/hooks/useGraphSaveName.ts#L104) — import-echo scanner: clears `lastSavedName` to `null` whenever a new `import graph from {name}` echo arrives, so the imported name takes over immediately
- `src/hooks/useGraphSaveName.ts` [L122–L131](../src/hooks/useGraphSaveName.ts#L122) — `setLastSavedName(name)`: sets the saved name and marks `untitledSlotConsumedRef = true` only when the name equals the current `untitled-{n}` fallback
- `src/hooks/useGraphSaveName.ts` [L133–L142](../src/hooks/useGraphSaveName.ts#L133) — `resetName()`: clears both names; advances counter only when `untitledSlotConsumedRef.current` is `true`, then resets the flag
- `src/hooks/useGraphSaveName.ts` [L145–L148](../src/hooks/useGraphSaveName.ts#L145) — derived `defaultName`: `lastSavedName ?? importedName ?? \`untitled-${untitledCounter}\``
- `src/components/Playground.tsx` [L216–L219](../src/components/Playground.tsx#L216) — hook instantiated with key `\`${storageKeySavedGraphs}-untitled-counter\`` to keep the counter isolated per playground
- `src/utils/messageParser.ts` [L266–L276](../src/utils/messageParser.ts#L266) — `extractImportGraphName()`: parses `> import graph from {name}` echoes; returns the trimmed name or `null`

---

## 4. Repository Layout

```
webapp/
├── src/
│   ├── main.tsx                  # React root mount
│   ├── App.tsx                   # Router + WebSocketProvider bootstrap
│   ├── index.css                 # Global resets / CSS variables
│   ├── config/
│   │   └── playgrounds.ts        # ★ SINGLE source of truth for all playgrounds
│   ├── contexts/
│   │   └── WebSocketContext.tsx  # Shared multi-socket state (above Routes)
│   ├── hooks/
│   │   ├── useWebSocket.ts       # Per-playground WS + command input logic
│   │   ├── useGraphData.ts       # REST fetch + graph state management
│   │   ├── useAutoGraphRefresh.ts# Mutation detection → auto re-fetch
│   │   ├── useAutoMarkdownPin.ts # help/describe echo → auto-pin preview
│   │   ├── useLargePayloadDownload.ts # Large payload REST fetch → console
│   │   ├── useAutoMockUpload.ts  # Mock-upload invitation → auto-open modal
│   │   ├── useMockUpload.ts      # POST fetch lifecycle for mock-upload modal
│   │   ├── useSavedGraphs.ts     # localStorage graph bookmark CRUD
│   │   ├── useGraphSaveName.ts   # Save-form pre-fill name (priority: saved > imported > untitled-n)
│   │   ├── useLocalStorage.ts    # Generic localStorage hook
│   │   ├── useToast.ts           # Toast notification queue
│   │   ├── useCopyToClipboard.ts # Clipboard write with copied state
│   │   ├── useMediaQuery.ts      # Responsive breakpoint detection
│   │   └── useAutocomplete.ts    # Command autocomplete suggestions
│   ├── components/
│   │   ├── Playground.tsx        # ★ Top-level orchestrator per route
│   │   ├── Navigation.tsx        # Header nav bar (Tools + Quick Links menus)
│   │   ├── Toast.tsx             # Toast container + item
│   │   ├── LeftPanel/            # Console + CommandInput wrapper
│   │   ├── RightPanel/           # Tab switcher (payload/preview/graph/graph-data)
│   │   ├── Console/              # Message list + ConsoleMessage renderer
│   │   ├── CommandInput/         # Text input + send button
│   │   ├── GraphView/            # ReactFlow canvas + NodeTypes + ErrorBoundary
│   │   ├── GraphDataView/        # Raw JSON viewer for graph model
│   │   ├── GraphToolbar/         # Copy/download toolbar for graph panel
│   │   ├── GraphSaveButton/      # Inline save-form button in header
│   │   ├── SavedGraphsMenu/      # Dropdown list of saved graph bookmarks
│   │   ├── MockUploadModal/      # Modal dialog for mock-data JSON upload
│   │   ├── PayloadEditor/        # Textarea + validation + SampleButtons
│   │   ├── MarkdownPreview/      # react-markdown renderer
│   │   └── NavMenu/              # Generic accessible dropdown menu
│   └── utils/
│       ├── messageParser.ts      # ★ Message classification & pattern matching
│       ├── graphTransformer.ts   # Backend JSON → ReactFlow nodes + edges
│       ├── graphTypes.ts         # TypeScript interfaces + type guard
│       ├── validators.ts         # JSON/XML payload validation + formatting
│       ├── urls.ts               # WebSocket & HTTP URL construction
│       └── commandSuggestions.ts # Autocomplete command list
├── docs/
│   ├── SPEC-auto-graph-refresh.md
│   ├── SPEC-large-payload-inline.md
│   └── SPEC-mock-data-upload-modal.md
├── scripts/
│   └── upload-json.mjs           # CLI helper: POST a JSON file to the backend
├── vite.config.ts
├── tsconfig.json
└── package.json
```

---

## 5. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                      App.tsx                        │
│  ┌──────────────────────────────────────────────┐  │
│  │           WebSocketProvider                  │  │
│  │   (lives above Routes — sockets persist)     │  │
│  │  ┌────────────────────────────────────────┐  │  │
│  │  │           BrowserRouter                │  │  │
│  │  │   Route /        → Playground (cfg A)  │  │  │
│  │  │   Route /json-path → Playground (cfg B)|  │  │
│  │  └────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘

Each Playground instance:

  Playground.tsx (orchestrator)
  ├── useWebSocket          ← command input + history + WS actions
  ├── useGraphData          ← REST fetch + graph state
  ├── useAutoGraphRefresh   ← mutation detection → silent describe graph
  ├── useAutoMarkdownPin    ← help/describe echo → auto-pin preview
  ├── useLargePayloadDownload ← oversized payload → REST fetch → console
  ├── useAutoMockUpload     ← upload invitation → auto-open modal
  ├── useSavedGraphs        ← localStorage bookmark CRUD
  ├── useGraphSaveName      ← save-form pre-fill name (saved › imported › untitled-n)
  │
  ├── LeftPanel
  │   ├── Console           ← message list (ConsoleMessage per row)
  │   └── CommandInput      ← textarea + send button + history nav
  │
  ├── RightPanel (tabbed)
  │   ├── PayloadEditor     ← textarea + validation (JSON-Path only)
  │   ├── MarkdownPreview   ← react-markdown (Minigraph only)
  │   ├── GraphView         ← ReactFlow canvas
  │   └── GraphDataView     ← raw JSON tree of graph model
  │
  └── MockUploadModal       ← native <dialog>; JSON paste / drop / browse; POST
```

**Key design principle:** State flows top-down through props; side-effects are encapsulated in hooks; components are dumb renderers. `Playground.tsx` is the only component that coordinates between hooks.

---

## 6. Layer-by-Layer Walkthrough

### 6.1 Entry Point & Routing

**`src/main.tsx`** — Standard React 19 root mount. Wraps `<App>` in `<StrictMode>`.

**`src/App.tsx`** — Two responsibilities:
1. Wraps the entire tree in `<WebSocketProvider>` *outside* `<BrowserRouter>` so connections survive route changes.
2. Generates one `<Route>` per entry in `PLAYGROUND_CONFIGS` — adding a playground requires only a config entry, never a code change here.

A catch-all `<Route path="*">` redirects unknown paths to the first playground.

---

### 6.2 Playground Configuration

**`src/config/playgrounds.ts`** — The single source of truth for everything that varies per playground:

```typescript
interface PlaygroundConfig {
  path:                  string;   // URL route, e.g. "/json-path"
  label:                 string;   // Nav bar label
  title:                 string;   // Page heading
  wsPath:                string;   // WebSocket endpoint, e.g. "/ws/json/path"
  storageKeyPayload:     string;   // localStorage key for payload
  storageKeyHistory:     string;   // localStorage key for command history
  storageKeyTab:         string;   // localStorage key for right-panel tab
  storageKeySavedGraphs?: string;  // present → enables saved-graph bookmarks
  supportsUpload?:       boolean;  // present → enables REST upload handshake
  tabs:                  RightTab[]; // ordered list of right-panel tabs to show
}
```

The file also exports shared runtime constants used across the app:

| Constant | Value | Purpose |
|---|---|---|
| `MAX_ITEMS` | 200 | Console message ring buffer size |
| `MAX_HISTORY` | 50 | Command history entries in localStorage |
| `MAX_BUFFER` | 63,488 | WebSocket send character limit (safely under 64 KB) |
| `PING_INTERVAL` | 20,000 ms | Keep-alive ping frequency |

> **Maintainer tip:** To add a new playground, add one object to `PLAYGROUND_CONFIGS`. The route, nav link, connection dot, localStorage namespace, and right-panel tabs are all derived automatically.

---

### 6.3 WebSocket State — Context + Hook

The WebSocket layer is split across two files with distinct responsibilities:

#### `src/contexts/WebSocketContext.tsx` — shared, navigation-persistent state

Holds a `useReducer`-managed map of `SlotState` objects, one per `wsPath`. A "slot" contains:
- `phase: 'idle' | 'connecting' | 'connected'`
- `messages: { id: number; raw: string }[]` (ring-buffered at `MAX_ITEMS`)

WebSocket instances, ping interval handles, and message ID counters live in `useRef` objects (outside the reducer) so they never cause re-renders.

**`connect(wsPath, onToast)`:**
1. Dispatches `CONNECTING`
2. Opens `new WebSocket(makeWsUrl(wsPath))`
3. On `open`: dispatches `CONNECTED`, sends `{"type":"welcome"}`, starts the ping interval
4. On `message`: filters out keep-alive ping/pong frames, dispatches `MESSAGE_RECEIVED`
5. On `close`/`error`: clears ping interval, dispatches `DISCONNECTED`

**`send(wsPath, data)`:** Writes directly to `wsRef.current` — no React re-render.

**`pendingPayloads`** is a `useState` (not `useRef`) map so that depositing a payload via `setPendingPayload` triggers a re-render in consuming components (specifically the JSON-Path Playground's `useEffect` polling for cross-playground payload transfer).

#### `src/hooks/useWebSocket.ts` — per-playground UI state

A thin delegation layer that reads its slot from the context and adds purely local concerns:

- **Command input + history** via a local `useReducer` (not shared — intentionally resets on remount)
- **Auto-scroll** via a `consoleRef` and a `useEffect` watching `messages`
- **History navigation** with ↑/↓ arrow key handling; saves and restores the in-progress draft when entering/exiting history mode
- **`sendCommand()`**: sends the command, pushes to history, and — for the special `load` command — also sends the payload as a second WebSocket message
- **Two-step upload handshake**: a `pendingUploadRef` is armed by `uploadPayload()`; a `useEffect` watches messages for the `extractUploadPath` pattern and fires the `fetch` POST
- **`sendRawText(text)`**: sends without echoing to history, used by automation hooks for silent commands like `describe graph`

---

### 6.4 Playground.tsx — The Orchestrator

`Playground.tsx` is the most important file to understand. It does **no rendering logic** — it wires together all hooks and passes their outputs as props to dumb layout components.

Key responsibilities:

| Concern | How handled |
|---|---|
| Payload persistence | `useLocalStorage(storageKeyPayload)` — survives navigation and refresh |
| Large payload override | Separate `useState(null)` — never written to localStorage to avoid quota exhaustion; wins over stored value when set |
| Payload validation | `useMemo(() => validatePayload(payload))` — synchronous, no extra render |
| Toast notifications | `useToast()` — queue-based, auto-removes after timeout |
| Graph path pinning | `useState<string \| null>(null)` — the REST path extracted from a graph-link message |
| Preview message pinning | `useState<number \| null>(null)` — stores message **id**, not raw string |
| Panel split persistence | `useDefaultLayout` from `react-resizable-panels` — keyed per route |
| Responsive layout | `useMediaQuery('(max-width: 768px)')` → vertical panel stacking on mobile |
| Clear messages | Also clears `pinnedMessageId`, `pinnedGraphPath`, `modalUploadPath`, `successfulUploadPaths`, and `graphData` so no stale state lingers |
| Cross-playground routing | `ctx.setPendingPayload` + `navigate()` — deposits JSON then navigates; consuming playground reads it via `ctx.takePendingPayload` |
| Mock-upload modal path | `useState<string \| null>(null)` — `null` = closed; non-null = open for that specific POST endpoint |
| Modal trigger element | `useRef<HTMLElement \| null>(null)` — captures `document.activeElement` before open; `.focus()` restored on close via `setTimeout` |
| Successful upload paths | `useState<Set<string>>(new Set())` — keyed by POST path; drives ✅ badge on invitation rows; session-only (cleared with `clearMessages`) |
| Graph save-form name | `useGraphSaveName(storageKey, ws.messages)` — provides `defaultName`, `setLastSavedName`, `resetName`; see §3.13 |

**Pinning logic** (`handlePinMessage`):
- If the message is a graph-link → set `pinnedGraphPath` (triggers `useGraphData`) and also highlight the row
- Otherwise → set `pinnedMessageId` and clear `pinnedGraphPath`

---

### 6.5 Left Panel — Console & Command Input

**`LeftPanel.tsx`** — a thin layout shell that positions `Console` above `CommandInput`.

**`Console.tsx`** — renders a scrollable `role="log"` div. Each message is wrapped in a `ConsoleErrorBoundary` so a rendering crash in one row does not take down the whole list. It also has copy-all and clear-all toolbar buttons.

**`ConsoleMessage.tsx`** — the most visually complex component. For each message it:
1. Calls `parseMessage()` to extract `{type, message, time}`
2. Calls `tryParseJSON()` — JSON messages render as a collapsible `<JsonView>` tree
3. Classifies the message as a graph-link, large-payload link, mock-upload invitation, or plain text for appropriate styling and icons
4. Conditionally renders:
   - A **pin button** (📌 for plain-text, 🕸️ for graph-links) — clickable rows with full keyboard support (`role="button"`, `tabIndex`, `Enter`/`Space` handlers). Mock-upload rows are **explicitly excluded** from `isPinnable` via `&& !isMockUpload` to prevent a nested-interactive-element accessibility violation
   - A **copy button** (📄 → ✅) with scoped `copied` state
   - A **send-to-JSON-Path button** (➡️) on JSON messages when the callback is wired
   - A **"⬆️ Upload JSON…" re-open button** on mock-upload invitation rows when `onUploadMockData` is wired; shows a ✅ badge when `successfulUploadPaths` contains the row's POST path

**`CommandInput.tsx`** — textarea (single or multiline mode) with a Send button and multiline toggle. Delegates all logic to `useWebSocket` via callbacks.

---

### 6.6 Right Panel — Tabs

**`RightPanel.tsx`** — renders only the tabs listed in the playground's `tabs` config. Uses `useId()` for accessible `aria-controls` / `role="tab"` associations. Active tab is driven by `rightTab` state from `useGraphData` (persisted in localStorage).

The four possible tabs:

| Tab key | Component | When shown |
|---|---|---|
| `'payload'` | `PayloadEditor` | JSON-Path playground |
| `'preview'` | `MarkdownPreview` | Minigraph playground |
| `'graph'` | `GraphView` | Both playgrounds |
| `'graph-data'` | `GraphDataView` | Both playgrounds |

---

### 6.7 Graph Pipeline

The journey from a WebSocket message to a rendered graph has four stages:

```
1. Server emits graph-link message in WebSocket stream
   e.g. "Graph described in /api/graph/model/ws-123-1"

2. messageParser.extractGraphApiPath() → "/api/graph/model/ws-123-1"
   (either by user clicking the 🕸️ row, or automatically via useAutoGraphRefresh)

3. Playground.tsx sets pinnedGraphPath → useGraphData fires REST fetch
   GET /api/graph/model/ws-123-1
   Response: { nodes: [...], connections: [...] }

4. graphTransformer.transformGraphData(data)
   → ReactFlow nodes[]  (with computed layout positions + per-type styles)
   → ReactFlow edges[]  (with relation labels)

5. GraphView renders <ReactFlow> with the computed nodes/edges
```

#### `src/utils/graphTypes.ts` — Type definitions & type guard

Defines `MinigraphGraphData`, `MinigraphNode`, `MinigraphConnection`, and `MinigraphRelation`. The exported `isMinigraphGraphData(value)` type guard is called on every REST response before setting state — it guards against malformed JSON without throwing.

#### `src/utils/graphTransformer.ts` — Layout algorithm

Uses a **BFS topological layout**:
1. Build adjacency lists and in-degree counts from `connections`
2. Identify seed nodes: in-degree 0 or typed as `entry_point`
3. BFS assigns a column (level) to each node — a node's level is always ≥ its predecessor's level + 1
4. Within each level, nodes are stacked vertically with equal spacing
5. Disconnected nodes are placed in a trailing column

Node styles are applied via `node.style` (not CSS classes) using the CSS custom property `--node-accent` for per-type accent colours. This is the pattern required by ReactFlow's `NodeResizer` — having no inner wrapper div means the RF wrapper element *is* the visual shell.

#### `src/components/GraphView/NodeTypes.tsx` — Custom node

`MinigraphNode` renders as a **React Fragment** (no wrapper div). `NodeResizer`, handles, and content are top-level siblings. This avoids all the sizing workarounds that a nested div structure requires. The `nodeTypes` map exported here is passed directly to `<ReactFlow nodeTypes={nodeTypes}>`.

#### `src/components/GraphView/GraphViewErrorBoundary.tsx`

A class-based error boundary that resets when its `key` prop changes. `GraphView` passes a `boundaryKey` derived from `graphData.nodes.map(n => n.alias)` — a corrected graph after a previous render failure renders cleanly without a page reload.

---

### 6.8 Automation Hooks

These hooks share the same pattern: **message-ID watermark + flag ref + effects**.

#### Pattern common to all automation hooks

```
useEffect (runs once at mount, empty dep []):
  watermarkRef.current = last message ID in existing log

useEffect (runs on connect change):
  if (!connected) clear pending flags + cancel timers/fetches

useEffect (runs on messages change — the main loop):
  newMessages = messages.filter(m => m.id > watermarkRef.current)
  watermarkRef.current = messages[last].id
  for each new message: inspect + act
```

The watermark prevents replaying historical messages as new mutations/echoes/large-payloads when the component mounts into an already-populated message log.

#### `useAutoGraphRefresh`

Watches for `detectMutation(raw)` returning `'node-mutation'` or `'import-graph'`.

- **`'node-mutation'`**: arms a 300 ms debounce timer. On fire, sends `describe graph` silently and sets `waitingForDescribeRef = true`. The next graph-link message is consumed to call `setPinnedGraphPath`.
- **`'import-graph'`**: immediately cancels any pending debounce and sends `describe graph`.
- When `waitingForDescribeRef` is true, a prior "Pass 1" in the main effect scans new messages for a graph-link and calls `setPinnedGraphPath` on the first one found, then returns early.

**Stale-closure fix**: `pinnedGraphPath` is read via `pinnedGraphPathRef` inside the debounce timer callback — if it were read directly from closure it would be stale after subsequent renders.

#### `useAutoMarkdownPin`

Watches for `isHelpOrDescribeCommand(raw)` (echoed `> help ...` or `> describe <non-graph>` commands). Sets `waitingForResponseRef = true`. The next message that passes `isPinnableResponse` (plain text, not an echo, not a graph-link, **not a mock-upload invitation**) is pinned via `setPinnedMessageId` and `onAutoPin()` is called to switch the tab.

**Critical guard**: echoed commands start with `> ` and pass `isMarkdownCandidate`. Without the echo guard in `isPinnableResponse`, the echo itself would be pinned instead of the actual response.

**Mock-upload guard**: `isPinnableResponse` also excludes `isMockUploadMessage` rows. Because the upload invitation is plain text, `isMarkdownCandidate` returns `true` for it. Without this exclusion, if `waitingForResponseRef` were armed (e.g. the user sent `help` and then quickly ran `upload mock data`), the invitation would be stolen and pinned to the Developer Guides tab instead of opening the modal.

#### `useLargePayloadDownload`

Watches for `extractLargePayloadLink(raw)` matching `"Large payload (N) -> GET /api/inspect/..."`. Fetches the endpoint, pretty-prints the JSON, and calls `appendMessage(content)` to inject the result into the console. An `isFetchingRef` re-entrancy guard prevents the appended result from being re-processed as a new notification. Only the first large-payload link in each message batch is processed (a `break` after the first match prevents concurrent fetches).

#### `useAutoMockUpload`

Watches for `isMockUploadMessage(raw)` — the server's upload invitation pattern `"You may upload JSON payload -> POST /api/mock/{id}"`. When detected, calls `onOpenModal(uploadPath)` immediately (no debounce, no two-step handshake — the invitation message itself contains the complete target URL).

Key design decisions compared with the other automation hooks:

| Decision | Rationale |
|---|---|
| No `waitingForRef` flag | No two-step server handshake; the invitation is self-contained |
| Watermark NOT reset on disconnect | No pending-wait flag to clear; resetting would replay old invitations on reconnect and auto-open the modal for stale endpoints |
| `connected` accepted but aliased `_connected` | Symmetry with peer hooks; body does not depend on it |
| Break on first match per batch | Consistent with `useLargePayloadDownload`; prevents two modal opens in one message batch |

---

### 6.9 Utilities

#### `src/utils/messageParser.ts`

The classification engine for all WebSocket messages. Key exports:

| Function | Purpose |
|---|---|
| `parseMessage(raw)` | Parse JSON or return raw as `{type:'raw'}` |
| `tryParseJSON(str)` | Returns `{isJSON, data}` — only true for objects/arrays, not primitives |
| `isMarkdownCandidate(raw)` | True for non-JSON strings; false for JSON lifecycle events |
| `isGraphLinkMessage(raw)` | True when the message contains `/api/graph/model/...` |
| `isLargePayloadMessage(raw)` | True for `"Large payload (N) -> GET ..."` |
| `isMockUploadMessage(raw)` | True for `"You may upload … -> POST /api/mock/..."` upload invitations |
| `isHelpOrDescribeCommand(raw)` | True for `"> help ..."` and `"> describe <non-graph>"` echoes |
| `detectMutation(raw)` | Returns `'node-mutation'`, `'import-graph'`, or `null` |
| `extractGraphApiPath(raw)` | Regex extracts `/api/graph/model/{id}` |
| `extractUploadPath(raw)` | Regex extracts `/api/json/content/{id}` (JSON-Path upload handshake) |
| `extractMockUploadPath(raw)` | Regex extracts `/api/mock/{id}` from the mock-upload invitation |
| `extractLargePayloadLink(raw)` | Parses the size and path from the large-payload notification |

**`detectMutation` matching rules** (important to understand for maintenance):

```
'import-graph'   if lower includes 'graph model imported as draft'
'node-mutation'  if lower includes ' -> ' AND 'removed'         (connection delete)
'node-mutation'  if lower.startsWith('node ') AND:
                    includes ' created'
                    includes ' updated'
                    includes ' deleted'
                    includes ' connected to '
                    includes ' imported from '
                    includes ' overwritten by node from '
```

The `startsWith('node ')` prefix guard is critical — it prevents false positives from `"Graph instance created"` (`instantiate graph`) and `"Root node created because it does not exist"` (`export graph`).

#### `src/utils/graphTransformer.ts`

Converts `MinigraphGraphData` to ReactFlow `nodes[]` and `edges[]`. See §6.7 for the layout algorithm detail.

#### `src/utils/urls.ts`

Single source of truth for URL construction:

```typescript
makeWsUrl(wsPath)
  // dev  → ws://localhost:3000{wsPath}   (Vite proxy on port 3000)
  // prod → ws://{window.location.host}{wsPath}  (same origin)
```

HTTP API paths are always **relative** — the Vite proxy forwards them in dev; the same-origin server handles them in production.

#### `src/utils/validators.ts`

`validatePayload(text)` tries `JSON.parse`, then `DOMParser` XML parse. Returns `{valid, error, type}`. `formatJSON` pretty-prints JSON. Used in `PayloadEditor` for live validation feedback.

---

### 6.10 Navigation Bar

**`Navigation.tsx`** reads all playground configs and renders two dropdown menus built on `NavMenu`:

**Tools menu** — per-playground entry with:
- A `<NavLink>` (navigates) with a status dot
- A separate **Start/Stop** button (connects/disconnects without navigating)

The aggregate dot status across all playgrounds uses `aggregateDotStatus()`:
- All connected → green
- All idle → grey
- Any connecting → pulsing yellow
- Mixed → partial (some connected)

**Quick Links menu** — static links to backend `/info`, `/health`, `/env`, etc.

**`NavMenu.tsx`** — a reusable accessible dropdown:
- `Escape` closes it
- Click outside closes it (`useEffect` + `mousedown` listener)
- `aria-expanded` / `aria-haspopup` for screen readers

---

### 6.11 Saved Graphs

**`useSavedGraphs(storageKey)`** manages a `Record<string, SavedGraphEntry>` in localStorage. The data model stores only the graph **name** (a string), not the graph data itself — the server holds the actual file.

**Save flow:**
```
User clicks Save → GraphSaveButton inline form →
  handleSaveGraph(name):
    savedGraphs.saveGraph(name)          // write to localStorage
    ws.sendRawText(`export graph as ${name}`)  // server writes {name}.json
```

**Load flow:**
```
User clicks Load in SavedGraphsMenu →
  handleLoadGraph(name):
    ws.sendRawText(`import graph from ${name}`)  // server reads {name}.json
    → server responds with a mutation success message
    → useAutoGraphRefresh detects import-graph
    → fires describe graph automatically
    → graph renders
```

**Save-form pre-fill (`useGraphSaveName`):**

The name pre-filled in the save form is managed entirely by `useGraphSaveName` — `GraphSaveButton` receives only the already-computed `defaultName` string and a `setLastSavedName` callback; it knows nothing about the priority logic.

```
Priority (highest → lowest):

1. lastSavedName   set by setLastSavedName(name) after a successful save
2. importedName    extracted from "> import graph from {name}" echo in WS stream
3. untitled-{n}    persisted localStorage counter, starts at 1, never skips
```

The counter increment rule: `untitled-{n}` only advances to `untitled-{n+1}` when `untitled-{n}` was actually saved (i.e. `setLastSavedName` was called with a name matching the current untitled fallback). Clearing the console without ever saving reuses the same slot. This guarantees `untitled-2` never appears unless `untitled-1` was saved first.

`importedName` always supersedes `lastSavedName` for a new import: the import-echo scanner calls `setLastSavedNameState(null)` immediately when a fresh `> import graph from {name}` arrives — `lastSavedName` is cleared in the same effect pass that sets `importedName`, so the priority chain reflects the new state in the very next render.

See §3.13 for the full feature description and all code locations.

---

## 7. Key Engineering Decisions

### 7.1 WebSocket Context Above the Router

**Decision:** `WebSocketProvider` wraps `<BrowserRouter>`, not the other way around.

**Why:** If the provider were inside a route, it would unmount and re-mount on navigation — closing the socket. By living above routes, all playground sockets share a single provider instance that never unmounts during normal navigation.

**Trade-off:** The context holds state for all playgrounds simultaneously, which is a small memory overhead. Given the bounded `MAX_ITEMS = 200` ring buffer per slot, this is negligible.

---

### 7.2 Refs Outside the Reducer for Socket Instances

**Decision:** `WebSocket` instances, ping interval handles, and message ID counters live in `useRef` (not `useState` or inside the reducer).

**Why:** These are imperative handles that must be accessible synchronously in event callbacks. Putting them in state would cause render cycles on every incoming message. The reducer owns only the serialisable view of state (phase + messages array) that React needs to diff and render.

---

### 7.3 Monotonic Message IDs Instead of Array Indices

**Decision:** Each message has a stable `id: number` that increments globally per slot, never recycled.

**Why:** Array indices shift when the ring buffer drops old messages. A pinned message identified by index would shift to a different message after the buffer rotates. The stable ID means `pinnedMessageId` always refers to the correct row.

---

### 7.4 Message Watermark Pattern

**Decision:** All three automation hooks (`useAutoGraphRefresh`, `useAutoMarkdownPin`, `useLargePayloadDownload`) initialise a watermark ref at mount to the highest ID in the existing log.

**Why:** Without this, a `useEffect` that runs on the initial `messages` dependency would scan every historical message as if it were new — triggering spurious mutations, auto-pins, or payload downloads for messages that arrived before the component mounted (e.g. from a previous session's localStorage-restored messages or from messages received while the user was on a different playground tab).

---

### 7.5 `sendRawText` vs `sendCommand`

**Decision:** Two separate send functions.

- `sendCommand()` — writes to history, echoes the command, handles the `load` special case
- `sendRawText(text)` — sends silently, used exclusively by automation hooks

**Why:** Automation hooks must not pollute the command history or produce `> describe graph` echo entries in the console (those would trigger `useAutoMarkdownPin` falsely, since `describe` is a triggering word). `sendRawText` bypasses all that.

---

### 7.6 Node Styles via `node.style` + CSS Custom Properties

**Decision:** ReactFlow node visual styling is applied via `node.style` on the node data object, not via a wrapper `<div className>` inside the component.

**Why:** When `MinigraphNode` renders as a `<Fragment>` (no inner wrapper), the ReactFlow-managed wrapper element *is* the visible shell. `node.style` is the only way to style it. The per-type accent colour is passed as `--node-accent` CSS custom property so the CSS module can reference it for header background, badge colour, and border — without any JS–CSS coupling in the component itself.

---

### 7.7 Two-Path `useGraphData` Design

The hook has two distinct code paths:

- **Initial-load path** (triggered by `pinnedGraphPath` changing): clears `graphData` to `null` (clean loading state for new graphs), auto-switches the tab to `'graph'` on success. Uses a `useEffect`-managed `AbortController`.
- **Auto-refresh path** (triggered by `refetchGraph()` call): does *not* clear `graphData` (stale graph stays visible under a spinner overlay), does *not* switch the tab (user is not interrupted). Uses a `useCallback`-stable imperative function with its own abort ref.

**Why separate paths?** A first-time load of a new graph needs visual loading feedback and should switch context to the Graph tab. A background refresh should be invisible to the user — just a spinner on the existing graph.

---

### 7.8 `useLargePayloadDownload` Re-entrancy Guard

**Decision:** `isFetchingRef` is checked at the top of the main effect.

**Why:** After `appendMessage(content)` injects the fetched payload into the console, `messages` changes, triggering the effect again. Without the guard, the newly appended message would be scanned — `extractLargePayloadLink` would return null for it, but the watermark advance logic uses `+ 1` as a belt-and-suspenders measure to ensure the appended message is treated as seen even before React re-renders.

---

### 7.9 `localStorage` for Persistence, Not Global State

**Decision:** All persistence (payload, command history, right-panel tab, saved graphs) uses `useLocalStorage` — a hook that wraps `useState` with read/write effects and a `storage` event listener.

**Why not Redux or Zustand?** The app's cross-component state needs are modest: the WebSocket context covers the only truly global mutable state. Everything else is either local to a component or derivable from props. Adding a global state library would be over-engineering. The `storage` event listener handles multi-tab synchronisation as a bonus.

---

### 7.10 Payload Override Pattern

**Decision:** `payloadOverride: string | null` is a separate `useState` that wins over `storedPayload` when non-null.

**Why:** Large payloads fetched via REST should not be written to localStorage (they can be megabytes). Instead the override is held in memory. Any manual edit in the textarea calls `setPayload` which calls `setPayloadOverride(null)` first — the user always gets back in control.

---

## 8. Data & Message Flows

### 8.1 User Sends a Command

```
User types in CommandInput → ws.command state
User presses Enter or Send button → ws.sendCommand()
  → ctx.send(wsPath, text)           — fires WebSocket.send()
  → pushes to history in localStorage
  → if text === 'load': also sends payload as second WS message
  → dispatch CLEAR_COMMAND
Server echoes: "> <command>"          — arrives via ws.onmessage
  → dispatch MESSAGE_RECEIVED
  → ConsoleMessage renders it as plain text
Server sends response (JSON or plain text)
  → dispatch MESSAGE_RECEIVED
  → ConsoleMessage renders:
      JSON object/array  → <JsonView>
      graph-link text    → 🕸️ pinnable row
      plain text         → plain text row
```

### 8.2 User Pins a Graph

```
User clicks 🕸️ row in Console → handlePinMessage(msg)
  → extractGraphApiPath(msg.raw) → "/api/graph/model/ws-123-1"
  → setPinnedGraphPath("/api/graph/model/ws-123-1")
  → setPinnedMessageId(msg.id)   — highlights the row
  → useGraphData effect fires:
      fetch("/api/graph/model/ws-123-1")
      → transformGraphData(json)  — BFS layout + ReactFlow nodes/edges
      → setGraphData(result)
      → setRightTab('graph')      — tab switches automatically
```

### 8.3 Auto-Refresh After Mutation

```
User sends "create node foo" → ws.sendCommand()
Server echoes "> create node foo"   — ignored by useAutoGraphRefresh
Server responds "Node foo created"  — detectMutation → 'node-mutation'
  → 300 ms debounce arms
  → (300 ms passes, no further mutations)
  → sendRawText('describe graph')   — silent, no history entry
  → waitingForDescribeRef = true

Server echoes graph-link "Graph described in /api/graph/model/ws-123-2"
  → useAutoGraphRefresh Pass 1: waitingForDescribeRef === true
  → extractGraphApiPath → "/api/graph/model/ws-123-2"
  → setPinnedGraphPath("/api/graph/model/ws-123-2")
  → waitingForDescribeRef = false

  → useGraphData initial-load path:
      fetch("/api/graph/model/ws-123-2")
      → setGraphData(result)
      → setRightTab('graph')
```

### 8.4 Large Payload Flow

```
User sends a command that returns a large payload
Server responds: "Large payload (254922) -> GET /api/inspect/ws-563-1/input.body"
  → useLargePayloadDownload detects via extractLargePayloadLink
  → isFetchingRef = true
  → fetch("/api/inspect/ws-563-1/input.body")
  → appendMessage(JSON.stringify(parsedJson, null, 2))
  → ConsoleMessage renders it as a <JsonView>
  → ➡️ button available: user can send to JSON-Path Playground
```

### 8.5 REST Upload Handshake

```
User pastes JSON in PayloadEditor, clicks Upload
  → ws.uploadPayload():
      pendingUploadRef = true
      ctx.send(wsPath, 'upload')

Server responds: "Please upload XML/JSON text to /api/json/content/ws-123-1"
  → useWebSocket effect detects extractUploadPath
  → pendingUploadRef = false
  → fetch POST /api/json/content/ws-123-1  body: payload JSON
  → addToast('Payload uploaded successfully')
```

### 8.6 Mock-Data Upload Flow

```
User sends "upload mock data" → ws.sendCommand()
Server responds: "You may upload JSON payload -> POST /api/mock/ws-417669-24"
  → useAutoMockUpload detects isMockUploadMessage (watermark-guarded)
  → extractMockUploadPath → "/api/mock/ws-417669-24"
  → handleOpenUploadModal("/api/mock/ws-417669-24")
      modalTriggerRef.current = document.activeElement   (command input)
      setModalUploadPath("/api/mock/ws-417669-24")        → modal mounts + showModal()

ConsoleMessage renders the invitation row with ⬆️ icon + "⬆️ Upload JSON…" button

User provides JSON (paste, drag-and-drop, or Browse file…):
  a. Paste / type → inline validation on every keystroke (tryParseJSON)
  b. Drop .json file onto drop zone → validateFileType → readFileAsText →
       tryParseJSON → formatJSON → textarea (focus restored)
  c. Click "Browse file…" → hidden <input type="file"> → same path as (b)

User clicks "Upload ▶" (or presses Ctrl+Enter / ⌘+Enter):
  → useMockUpload.upload()
      isUploading = true
      fetch POST /api/mock/ws-417669-24  Content-Type: application/json
      → 2xx:  isUploading = false → onSuccess(body)
                → setSuccessfulUploadPaths (adds path → ✅ badge on console row)
                → setModalUploadPath(null)  → modal unmounts
                → setTimeout: modalTriggerRef.current?.focus()
                → addToast('Mock data uploaded successfully ✓', 'success')
      → non-2xx: isUploading = false → onError('HTTP 400 — <body>')
                → setUploadError (inline banner) + addToast (error toast)
                → modal stays open for retry

User dismisses modal (Escape / Cancel / backdrop click):
  → handleClose → cancel() (AbortController.abort()) → onClose()
  → setModalUploadPath(null) → modal unmounts
  → setTimeout: focus restored to modalTriggerRef

User clicks "⬆️ Upload JSON…" re-open button on the console row:
  → handleOpenUploadModal(mockUploadPath) → modal re-opens for same endpoint
```

---

### 8.7 Save-Name Lifecycle

```
── Fresh session ─────────────────────────────────────────────────────
localStorage has no counter yet
  → useLocalStorage initialises untitledCounter = 1
  → defaultName = "untitled-1"

── User saves as "untitled-1" ────────────────────────────────────────
handleSaveGraph("untitled-1"):
  savedGraphs.saveGraph("untitled-1")          // localStorage bookmark
  setLastSavedName("untitled-1")               // in useGraphSaveName
    → setLastSavedNameState("untitled-1")
    → "untitled-1" === `untitled-${1}` → untitledSlotConsumedRef = true
  ws.sendRawText("export graph as untitled-1") // server writes file
  → defaultName = "untitled-1"                 // lastSavedName wins

── User clears console (slot was consumed) ───────────────────────────
handleClearMessages() → resetSaveName() → resetName():
  setImportedName(null)
  setLastSavedNameState(null)
  untitledSlotConsumedRef.current === true
    → setUntitledCounter(1 + 1 = 2)            // advances: slot was used
  untitledSlotConsumedRef.current = false
  → defaultName = "untitled-2"

── User clears console WITHOUT saving ────────────────────────────────
resetName():
  setImportedName(null)
  setLastSavedNameState(null)
  untitledSlotConsumedRef.current === false
    → counter stays at 2                        // slot NOT advanced: never used
  → defaultName = "untitled-2"                 // same slot reused

── User saves as "my-graph" (renaming away from untitled) ────────────
setLastSavedName("my-graph"):
  setLastSavedNameState("my-graph")
  "my-graph" !== `untitled-${2}` → flag stays false
  → defaultName = "my-graph"                   // lastSavedName wins
  → counter is still 2; "untitled-2" was never used

── User imports "other-graph" (supersedes lastSavedName) ─────────────
WS echo: "> import graph from other-graph"
  → extractImportGraphName → "other-graph"
  → setImportedName("other-graph")
  → setLastSavedNameState(null)               // lastSavedName cleared
  → defaultName = "other-graph"              // importedName wins
```

---

## 9. State Ownership Map

| State | Owner | Persistence | Notes |
|---|---|---|---|
| WS phase per slot | `WebSocketContext` reducer | Memory only | Resets on page reload |
| Messages per slot | `WebSocketContext` reducer | Memory only | Ring-buffered at 200 |
| WS refs (socket, ping, msgId) | `useRef` in Context | Memory only | Not in reducer (no renders) |
| Pending payload (cross-playground) | `useState` in Context | Memory only | `useState` (not `useRef`) for re-render trigger |
| Command input | `localReducer` in `useWebSocket` | Memory only | Resets on unmount — intentional |
| Command history | `useLocalStorage` in `useWebSocket` | `localStorage` | Keyed per playground |
| Payload text | `useLocalStorage` in `Playground` | `localStorage` | Keyed per playground |
| Payload override (large) | `useState` in `Playground` | Memory only | Never written to localStorage |
| Pinned message id | `useState` in `Playground` | Memory only | |
| Pinned graph path | `useState` in `Playground` | Memory only | |
| Modal upload path | `useState` in `Playground` | Memory only | `null` = closed; non-null = modal open |
| Modal trigger element | `useRef` in `Playground` | Memory only | Captures active element before open for focus restore |
| Successful upload paths | `useState` in `Playground` | Memory only | `Set<string>`; cleared on `clearMessages` |
| Graph data | `useState` in `useGraphData` | Memory only | |
| Right panel tab | `useLocalStorage` in `useGraphData` | `localStorage` | Keyed per playground |
| Is refreshing | `useState` in `useGraphData` | Memory only | |
| Saved graph names | `useLocalStorage` in `useSavedGraphs` | `localStorage` | Keyed per playground |
| Untitled counter | `useLocalStorage` in `useGraphSaveName` | `localStorage` | Keyed per playground; only increments when slot was consumed by a save |
| Last-saved name | `useState` in `useGraphSaveName` | Memory only | Cleared on import or reset |
| Imported graph name | `useState` in `useGraphSaveName` | Memory only | Set from WS echo; cleared on reset |
| Untitled slot consumed | `useRef` in `useGraphSaveName` | Memory only | Write-only flag; never drives a re-render |
| Multiline mode | `useState` in `Playground` | Memory only | |
| Toasts | `useReducer` in `useToast` | Memory only | Auto-expires |
| Panel split ratio | `react-resizable-panels` | `localStorage` | Keyed per route path |
| Automation watermarks | `useRef` in automation hooks | Memory only | Per hook instance |

---

## 10. Build, Dev & Deploy

### Development

```bash
# Start the Java backend (in examples/minigraph-playground)
java -jar target/minigraph-playground-4.3.77.jar   # port 8085

# Start the Vite dev server (in webapp/)
npm run dev   # port 3000, proxies /ws and /api to 8085
```

Open `http://localhost:3000`. Hot module replacement is active.

### Production Build + Deploy

```bash
npm run build:deploy
# Equivalent to:
npm run build    # → dist/
npm run deploy   # → cp dist/* ../src/main/resources/public/
```

The Java jar is then rebuilt and serves the SPA from the same origin as the WebSocket and REST endpoints — no CORS, no proxy needed.

### Bundle splitting (`vite.config.ts`)

Manual chunks keep each heavy vendor isolated:

| Chunk | Content |
|---|---|
| `vendor-xyflow` | `@xyflow/react` (largest dependency) |
| `vendor-router` | `react-router-dom` |
| `vendor-markdown` | `react-markdown` + `remark-gfm` |
| `vendor-json-view` | `react-json-view-lite` |
| `vendor-panels` | `react-resizable-panels` |

Source maps are enabled for production (`sourcemap: true`).

### Upload JSON CLI

`scripts/upload-json.mjs` is a Node.js script for testing the REST upload endpoint directly without the UI:

```bash
node scripts/upload-json.mjs \
  --url http://localhost:8085/api/mock/ws-563496-16 \
  --file ./64.json
```

---

## 11. Extending the App

### Add a new playground

1. Add an entry to `PLAYGROUND_CONFIGS` in `src/config/playgrounds.ts`
2. That's it. The route, nav link, localStorage namespace, right-panel tabs, and connection management are all derived automatically.

### Add a new right-panel tab type

1. Add the new key to the `RightTab` union in `RightPanel.tsx`
2. Add the tab button and panel in `RightPanel.tsx`
3. Reference it in `tabs:` arrays in `PLAYGROUND_CONFIGS`

### Add a new node type to the graph

1. Add the type string to `NODE_ACCENT` in `graphTransformer.ts` (gives it an accent colour)
2. Add the type to `TYPE_META` in `NodeTypes.tsx` (gives it an icon and label)
3. Add the type to the `nodeTypes` export in `NodeTypes.tsx` (maps it to `MinigraphNode`)
4. (Optionally) add it to the MiniMap `colorMap` in `GraphView.tsx`

### Add a new mutation command to auto-refresh

Edit `detectMutation()` in `messageParser.ts`. Follow the existing patterns — always guard with `!isMarkdownCandidate` (skip JSON), `!raw.startsWith('> ')` (skip echoes), and `!isGraphLinkMessage` (skip graph links). Add the new success message pattern as a return case.

---

## 12. Pitfalls & Gotchas

### P1 — Do not move `WebSocketProvider` inside `<BrowserRouter>`
It must stay *above* the router. Inside the router it would unmount on every navigation, closing all sockets. See §7.1.

### P2 — Do not put WS instances in React state
`WebSocket` instances are imperative handles that must be available synchronously. In state they would cause render cycles on every incoming message. They belong in `useRef`. See §7.2.

### P3 — Message IDs are not array indices
The ring buffer drops old messages. Always identify pinned/processed messages by their stable `id`, never by their position in the array. See §7.3.

### P4 — `detectMutation` requires the `startsWith('node ')` prefix
Without this guard, `"Graph instance created"` and `"Root node created because..."` trigger false-positive auto-refreshes. Do not remove the prefix check. See §6.9 and §7.4.

### P5 — `sendRawText` must be used for silent commands, not `sendCommand`
`sendCommand` pushes to history and echos the command — the echo would trigger `useAutoMarkdownPin`'s `describe` guard and steal the graph-link response. See §7.5.

### P6 — Watermarks must be set in a separate `useEffect` with an empty dep array
The watermark init must run *before* the main scanning effect at mount, not inside it. React guarantees effects in a component run in declaration order on the first render, so declaring it first works. Merging it into the main effect would process historical messages as new ones. See §7.4.

### P7 — `payloadOverride` must be cleared on manual edits
The `setPayload` callback does `setPayloadOverride(null)` before calling `setStoredPayload`. If you add a new code path that modifies the payload, ensure it also clears the override so the user's edit is not silently discarded. See §7.10.

### P8 — `refetchGraph` has an intentionally empty dependency array
It reads `pinnedGraphPath` via `pinnedGraphPathRef` to avoid stale closures. Adding `pinnedGraphPath` to its dep array would break the "stable reference" contract that lets automation hooks include it in their own dep arrays safely. See §7.7.

### P9 — Large payloads must never be written to localStorage
The payload override pattern exists precisely for this reason. Never call `setStoredPayload` (the localStorage-backed setter) with a large blob. See §7.10.

### P10 — ReactFlow node type must be in `nodeTypes` map
If the backend sends a node whose `types[0]` value is not a key in the `nodeTypes` map in `NodeTypes.tsx`, ReactFlow will render a default node without the custom styling. Add new types to all four places listed in §11.

### P11 — `isPinnable` must exclude mock-upload invitation rows
The upload invitation is plain text, so `isMarkdownCandidate` returns `true` for it. Without the `&& !isMockUpload` guard in the `isPinnable` derivation in `ConsoleMessage.tsx`, the row would receive `role="button"` and `onClick` *alongside* the "⬆️ Upload JSON…" re-open button — creating nested interactive elements that violate WCAG. Always keep the `!isMockUpload` exclusion. See §3.12 and §6.5.

### P12 — `useAutoMarkdownPin.isPinnableResponse` must exclude mock-upload invitations
Same plain-text nature: if `waitingForResponseRef` is armed (user sent `help`, then quickly ran `upload mock data`), the invitation message passes `isMarkdownCandidate` and would be stolen as the "response" and pinned to the Developer Guides tab. The `isMockUploadMessage` exclusion in `isPinnableResponse` prevents this. Do not remove it. See §6.8.

### P13 — Do not reset `useAutoMockUpload`'s watermark on disconnect
Unlike the other automation hooks, this hook has no pending-wait flag to clear on disconnect. Resetting the watermark to `−1` on disconnect would cause invitation messages still in the store to be replayed as "new" on reconnect, auto-opening the modal for a stale endpoint from a previous session. Leave the watermark at its current value. See §6.8.

---