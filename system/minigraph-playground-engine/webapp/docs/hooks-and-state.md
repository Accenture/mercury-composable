# Hooks, state machines and storage

Every concern of the Playground is a hook with a small state machine that moves on classified
backend lines ([protocol.md](protocol.md)). This page lists them, what they subscribe to, what they
send, and every key the app persists.

## Transport and kernel

| Hook / module | Role |
|---|---|
| `contexts/WebSocketContext.tsx` — `WebSocketProvider`, `useWebSocketContext()` | One slot per `wsPath`: `phase` (`idle` / `connecting` / `connected`), `connectionEpoch`, the message log (200 rows, monotonic ids), `connect(wsPath, onToast?)`, `disconnect`, `send(wsPath, text): boolean`, `appendMessage` (local-only rows), `clearMessages`, and the cross-playground **pending payload** mailbox (`setPendingPayload`, `peekPendingPayload`, `takePendingPayload`; `useState`, so a deposit re-renders the receiving playground). Socket instances, ping timers and id counters live in refs. Auto-connects every configured playground on mount; keep-alive every 20 s. |
| `hooks/useWebSocket.ts` | The per-playground view on a slot: `connected`, `connecting`, `connectionEpoch`, `messages`, the command input with its history cursor (ArrowUp enters history and snapshots the draft; ArrowDown past the newest entry restores it), `sendCommand` (history, `load` second frame, local help interception via `handleLocalCommand`), `sendRawText` (silent), `uploadPayload` (sends `upload`, then POSTs the payload to the `upload.contentPath` line), `copyMessages`, `clearMessages`, `appendMessage`, `consoleRef` (auto-scroll on new messages). |
| `protocol/useProtocolKernel.ts` | Classifies new messages once (id watermark) and emits them on the playground's `ProtocolBus`; returns `classificationMap` for all visible rows. |
| `protocol/bus.ts` — `ProtocolBus` | `on(kind, listener) → unsubscribe`, `emit(event)`; synchronous, listeners in registration order, each isolated by try/catch. |

## Graph data and refresh

**`useGraphData(sessionGraphPath, addToast, initialTab, validTabs, storageKeyTab)`** owns
`graphData`, `rightTab` (persisted, normalised against the playground's tab set) and
`refetchGraph`. The initial load runs whenever the session path changes (the id arrives from the
`session` round-trip, so the path starts `null`): it clears the graph, fetches
`GET /api/graph/session/{id}` with an `AbortController`, reveals the Graph tab when a graph with
nodes arrives, and stays quiet on 404 or an empty graph (both normal). `refetchGraph` is a stable
callback that keeps the stale graph visible under the spinner, sets `isRefreshing`, cancels a
previous in-flight refetch, and toasts on failure (a mutation just happened, so the session should
be live). A zero-node result clears the view.

**`useAutoGraphRefresh({ bus, hasGraph, connected, refetchGraph, clearGraph, addToast })`**
re-fetches on `graph.mutation`: debounced 300 ms for `node-mutation` (a burst of authoring
commands lands as one fetch, toast "Graph updated — refreshing…"), immediately for `import-graph`
("Graph imported — refreshing view…"). On `session.reset` it cancels a pending refresh and clears
the graph. In a collaboration session every member sees the propagated commands' replies in its
own console, so each member re-fetches its own replica — no graph-link forwarding.

## Session collaboration (`session/`)

**`useSessionCollaboration({ enabled, connected, bus, classificationMap, sendRawText, addToast })`**
returns the controller the Session menu renders: `state` (`sessionId`, `startedSince`,
`subscribedTo`, `subscribers`, `loading`, `pendingCommand`, `error`, `lastInfo`), `isPrimary`
(= not subscribed to anyone), `hasSubscribers`, `canSubscribe` / `canUnsubscribe` / `canReset`, and
`subscribeToSession(id)`, `unsubscribe()`, `resetSession()`, `clearMessage()`.

- On connect it sends `session` (a refresh) and reconciles from the `minigraph.session.status`
  answer, which is authoritative (a user-typed `session` counts too). Accepted command results
  update the relation optimistically; notifications (`subscriber-joined`, `subscriber-left`,
  `host-closed`) are peer deltas; `session.reset` clears the snapshot and refreshes.
- `minigraph.session.started` (a new backend session) resets everything to the new id; a
  disconnect resets to the empty state and clears the dedupe keys.
- The hook also consumes the already-classified backlog with its own watermark, deduping by
  `kind:msgId`, so a `session` typed before the menu opened still counts.
- Guards: one subscription at a time; ids must match `^ws-\d+-\d+$`; reset is offered only to a
  primary session that has subscribers (subscribers unsubscribe instead).

`sessionParser.ts` is the only module that knows the backend's session text; `sessionTypes.ts`
holds the types and `isValidSessionId`.

## The run workflow (`hooks/useGraphRunWorkflow.ts`, `graphRun/`)

Phases: `idle → instantiating → ready → running → idle`, plus `outcome-uncertain`. The hook mirrors
acknowledged backend lines and serialises the same text commands a user would type
(`GRAPH_RUN_COMMANDS`: `instantiate graph`, `run`). The lifecycle is three explicit steps, and the
middle one is optional (tutorial 1 needs no input):

- **Instantiate** sends `instantiate graph` and waits for `graph.instance.created`; the run is then
  `ready` (a toast says whether the graph reads `input.body.*` — paths collected from node
  properties by `graphRun/graphInputPaths.ts`, descriptive keys excluded, UI hints only).
  Instantiate stays enabled while `ready`: instantiating again starts a fresh instance.
- **Upload** is a local action, not a workflow phase: `Playground` opens `MockUploadPanel` for this
  session's own `/api/mock/{sessionId}` (the hints are the graph's `input.body.*` paths and its
  `input.header.*` names; the panel's header rows post to `?namespace=header` after the body). No
  console command is sent, so in a collaborative session only the member who clicked sees the
  form; the engine loads the payload into every member's instance and confirms in every console.
  A replayed `upload.invitation` line never opens a panel (that was the 2026-10-02 defect: one
  member's Instantiate opened the form on every member's screen).
- **Run** sends `run` and waits for `graph.run.terminal` (an abort toasts "Graph run aborted. See
  the console for details.").
- A typed `instantiate graph` or `run` echo moves the phase too (Ready without inventing a
  button action). `graph.instance.cleared` and `session.reset` hard-reset; `graph.mutation` and
  `graph.exported` **invalidate**: an outstanding command is quarantined as `outcome-uncertain`
  because the text protocol has no correlation ids, and its late acknowledgement is ignored.
  `command.error` while a signal is pending resets with a toast.
- A setup step that takes more than 10 s (`GRAPH_RUN_SETUP_TIMEOUT_MS`) becomes
  `outcome-uncertain` with an info toast. A new connection epoch or a change of `isPrimary`
  (subscribe / unsubscribe) hard-resets; `isPrimary` is not a gate — a subscribed session runs the
  graph as an equal partner.
- Derived flags for the toolbar: `canInstantiate` (idle, or ready when the graph reads
  `input.body`), `canRun` (ready), `disabledReason`.

## Authoring and undo

`components/GraphAuthoring/useGraphAuthoring.ts` and `hooks/useGraphUndo.ts` are described with
the canvas in [graph-view.md](graph-view.md) (state machine, matching, pacing, recipes).

## Help

- **`useAutoHelpNavigate({ bus, setHelpTopic, onTabSwitch, enabled, contentProfile })`** — on
  `command.helpOrDescribe`, resolves the command with `resolveBundledHelpTopic` (the same resolver
  that handles typed commands locally) and opens the panel on the bundled topic; a topic without a
  bundled page leaves the panel alone. A collaborator's or a companion's echoed `help` opens the
  same page.
- **`useHelpScrollNavigation`** — flips to the previous/next page in the global order after
  120 px of overscroll past the top or bottom, with a rubber-band hint (≤ 18 px), a 180 ms snap-back
  and a 650 ms cooldown; the wheel listener is attached once, passive, reading everything through
  refs.
- `utils/localHelpCommand.ts` / `utils/helpTopic.ts` — `help`, `help {topic}` and the exact
  three-word `describe skill {route}` (route lowercased, `.` → `-`, the engine's own rule) map to
  a topic; everything else returns null and goes to the engine.

## Uploads and large payloads

- **`useMockUploadPanel({ addToast })`** — one panel open at a time, opened only by an explicit
  local action (the toolbar's Upload button, or the "⬆ Upload JSON…" button on a console
  invitation row); a further open request queues FIFO behind the open panel;
  `handleCloseUploadPath(path)` closes only the panel that owns that exact path; a success adds the
  path to `successfulUploadPaths` (the ✅ badge on the console row) and advances the queue; focus
  returns to the element that had it once the queue drains. There is no bus subscription: an
  `upload.invitation` line by itself never opens the panel.
- **`useMockUpload({ uploadPath, json, onSuccess, onError })`** — the POST (`application/json`,
  raw text), one `AbortController` per attempt, `isUploading` cleared before the callbacks.
- **`useLargePayloadDownload({ bus, connected, appendMessage, addToast })`** — on
  `payload.large`, fetches the `/api/inspect/…` path, pretty-prints JSON and appends it as a console
  row ("Fetching large payload (X.XX MB)…"); one fetch at a time (events during a fetch are
  dropped); aborted on disconnect or unmount.

## Graph file import and download (`hooks/useGraphFileImport.ts`, `utils/graphFile.ts`)

- **`useGraphFileImport({ sessionId, connected, hasGraph, addToast })`** — a graph model from a
  file on the user's computer becomes the session's draft. `importFiles(files)` (the canvas drop
  and the file picker both land here) takes one file at a time, checks the type (`.json` or a JSON
  MIME type, `utils/jsonFile.ts`), parses it, validates the model (`validateGraphModel`: a JSON
  object whose only top-level sections are `nodes` — mandatory, non-empty, every node with an alias
  and types — and `connections` — optional; anything else is refused by name) and posts it to
  `POST /api/graph/import/{sessionId}`. The engine validates again, replaces the draft and tells
  every member's console "Graph model imported as draft" — the `import-graph` mutation that
  `useAutoGraphRefresh` already re-fetches on — so the hook toasts errors only (the type, the JSON,
  the model, the engine's refusal with its message). While a graph is loaded the file is parked in
  `pending` for `GraphImportConfirmDialog` (`confirmPending` / `cancelPending`): an import replaces
  every member's draft and cannot be undone. `openFilePicker()` creates a hidden `<input type="file"
  accept=".json,application/json">` and clicks it.
- **Download** is not a hook: `Playground` opens `GraphDownloadDialog`, then `saveTextFile(
  buildGraphFileText(graphData, graphId), graphFileName(graphId))`. `buildGraphFileText` deep-copies
  the graph and sets the root node's `name` to the graph id (what `export graph as` does on the
  engine, so the file and the model agree; the live graph is untouched); `saveTextFile` uses
  `showSaveFilePicker` where the browser has it (the user picks the folder; a cancel resolves
  `{saved: false}`) and otherwise a Blob + `<a download>` into the download folder. `saveBinaryFile`
  is its binary twin for `<set>.pack` (the `application/octet-stream` type, the same two paths).

## Graph sets (`hooks/useGraphSetPanel.ts`, `utils/graphSet.ts`)

**`useGraphSetPanel({ addToast, importFiles, graphData, graphName })`** owns the graph-set panel
(ADR-0027): `isOpen` / `open` / `close` (the entries and the manifest survive a close, so the panel
can be put away while a graph is edited), `mode` (`assemble` | `inspect`) and `busy`.

- **Entries.** `addFiles(files)` takes several files at once: each `.json` is type-checked
  (`utils/jsonFile.ts`), parsed and validated (`validateGraphModel`) and becomes an entry whose id is
  the file name without `.json` (`entryIdFromFileName`); every file not taken is reported in
  `fileErrors`. One `.pack` on its own is read back instead (below); a `.pack` among other files is
  refused. `addCurrentGraph` builds the live graph's entry through `buildGraphFileText` (the root
  node named after `suggestGraphId(graphName)`) and replaces an earlier copy of the current graph.
  `entryIssues` (`utils/graphSet.ts`) flags what the engine would refuse: an id that breaks the
  file-name rule, a root name that differs from the id (D6), a duplicate id.
- **Manifest.** `setName` with `validateSetName` (required, the file-name rule) and the rows
  (`updateRow` / `addRow` / `removeRow`), seeded with `version` and `description`; `manifestRowIssues`
  ignores blank rows and blank values, needs a name for a value, refuses `set`, `format` and
  `format_version` (the engine writes them), a duplicate name, and a `graph_id` that names no entry.
- **Pack.** `canPack` when there is an entry, no issue and a valid name; `pack()` posts
  `buildPackRequest` (`{manifest: {set, ...fields}, graphs: {id: model}}`) to
  `POST /api/graph-set/pack`, where the deployment gate checks every graph (D2), and saves the
  answered bytes with `saveBinaryFile` as `<set>.pack` (toast "Graph set saved as …"); a refusal is
  the engine's message in `packError`, cleared by the next edit. One `AbortController` at a time;
  a close aborts.
- **Inspect.** A dropped `.pack` is posted as `application/octet-stream` to
  `POST /api/graph-set/unpack`; `parseUnpackAnswer` reads `{manifest, graphs}` into `inspected`
  and the mode flips. `importInspectedGraph(id)` wraps the graph as a `<id>.json` `File` and hands
  it to `useGraphFileImport.importFiles`, so the confirm-before-replace dialog applies;
  `editInspectedSet()` loads the graphs and the caller fields (`manifestRowsOf`, without `set`,
  `format` and `format_version`) into the editor under the set's name; `leaveInspect()` returns.

## Saved graphs and naming

- **`useSavedGraphs(storageKey)`** — a `localStorage` map `name → { name, savedAt }` (only the
  names: the graph file lives on the engine side), newest first; `saveGraph`, `deleteGraph`,
  `hasGraph`.
- **`useSavedGraphWorkflow({ bus, connected, sendRawText, saveGraph, addToast })`** — **Save**
  sends `export graph as {name}` and waits up to 10 s for the `graph.exported` line with that name
  (toast `Graph saved as "{name}"`); every confirmed export is bookmarked, console-typed ones
  included, because a bookmark without a server-side file could not be loaded; `graph.export.failed`
  rejects the pending save ("invalid filename (a–z, A–Z, 0–9, hyphen only)" or "root node name does
  not match existing graph"). **Load** sends `import graph from {name}`.
- **`useGraphSaveName(storageKey, bus, connected, connectionEpoch)`** — the default name for the
  save form: the last saved name, else the imported name (`command.importGraph`), else
  `untitled-{n}` from a per-playground counter that advances only when an `untitled-{n}` export
  consumed the slot (so `untitled-{n}` exists only if `untitled-{n-1}` does). `graph.mutation`
  marks the name dirty; `session.reset` resets; a module-level cache keyed by connection epoch
  survives SPA navigation but not a hard refresh.

## Other hooks

| Hook | Notes |
|---|---|
| `useSendToJsonPath({ ctx, navigate, addToast, wsPath })` | The ➡️ action on a JSON console row: deposits the JSON in the context mailbox for the JSON-Path playground (last write wins), navigates, and connects that slot first when it is idle (a watching effect sends once connected). Undefined on the JSON-Path playground itself. |
| `useHistoryAutocomplete(history, command)` | The "Recent Commands" dropup: case-insensitive prefix match on the first line, de-duplicated, ≤ 8; ARIA combobox pattern. `useAutocomplete` (grammar suggestions from `utils/commandSuggestions.ts`) exists but is dormant — nothing imports it. |
| `useCopyToClipboard` | `navigator.clipboard.writeText` with a 2 s "copied" state per row. |
| `useLocalStorage<T>(key, initial)` | JSON in `localStorage`, re-read on key change, on `storage` events from other tabs and on window focus / visibility (a same-tab DevTools `clear()` fires no event). Two instances sharing a key in one tab do not notify each other. |
| `useMediaQuery(query)` | `(max-width: 768px)` → the vertical layout. |
| `useToast()` | `addToast(message, type, { durationMs, action })`; 3 s default, timers cleared on unmount. |

## The workspace clipboard (`contexts/ClipboardContext.tsx`, `clipboard/`)

IndexedDB database `minigraph-clipboard` (version 1; store `items` keyed by `id`; indexes
`by-alias` **unique** and `by-clippedAt`) through `idb`. A record is `{ id (uuid), clippedAt,
sourceWsPath, sourceLabel, node (a full MinigraphNode snapshot), connections (its direct
connections, self-connections excluded) }`. Mutations write the database first, then dispatch to
the reducer, then broadcast on the `BroadcastChannel` `minigraph-clipboard-sync`
(`item-added`, `item-replaced`, `item-removed`, `items-cleared`) so every open tab stays in sync;
a tab that cannot open the channel simply runs without cross-tab sync. `clipNode` reports
`added`, `duplicate` (same alias already clipped — uniqueness is by alias across all playgrounds;
the dialog offers to replace) or `error`. An upgrade recreates the store and a failed open deletes
and recreates the database (clips are expendable). Sorting (`sortItems.ts`) is display-only:
recent / type / alias / source / connections / a property key, natural and case-insensitive,
missing values last. Drag-and-drop carries only the item id under the MIME type
`application/x-minigraph-clipboard-item`. Paste (`paste.ts` + `commandBuilder.ts`) rebuilds a
`create node` (or `update node` when the alias exists in the current graph) through the node
editor's conversion (`createEditNodeFormState`) and the authoring builder, so the text has the
shape the engine's own `edit node` prints: `with type {types[0]}`, then one `key=value` line per
scalar, one `key[]=element` line per list element in list order, one `path.key=value` line per
leaf of a nested map, keys sorted as `edit node` sorts them, and a value containing a newline
wrapped in `'''`. (Until 2026-10-02 the builder wrote `key[]=value` for scalars too, and the
engine's `setElement("key[]", v)` appends, so a pasted `skill` arrived as a one-element list.) A
node the grammar cannot carry (an empty list or map, a value containing `'''`, more than one
type, a reserved alias) makes the plan throw and the Playground shows "Paste failed: …"; the
stored connections are not replayed.

## Everything the app persists

| Key | Store | Owner | Content |
|---|---|---|---|
| `minigraph-last-payload`, `jsonpath-last-payload` | localStorage | `Playground` | payload editor text |
| `minigraph-command-history`, `jsonpath-command-history` | localStorage | `useWebSocket` | last 50 commands, newest first |
| `minigraph-right-tab`, `jsonpath-right-tab` | localStorage | `useGraphData` | selected right-panel tab |
| `minigraph-help-topic`, `jsonpath-help-topic` | localStorage | `Playground` | last help topic (`''` = root) |
| `help-panel-open`, `console-panel-open`, `clipboard-sidebar-open` | localStorage | `Playground` | panel toggles (shared across playgrounds) |
| `graph-nodes-compact` | localStorage | `GraphView` | thumbnail (true) vs details |
| `graph-minimap-position` | localStorage | `GraphMinimap` | `{left, bottom}` of the island |
| `minigraph-saved-graphs` | localStorage | `useSavedGraphs` | bookmark map `name → {name, savedAt}` |
| `minigraph-saved-graphs-untitled-counter` | localStorage | `useGraphSaveName` | next untitled number (the code comments still say `minigraph-untitled-counter`) |
| `help-split-percent`, `help-split-maximized` | sessionStorage | `RightPanel` | the help split's resting size and maximized flag |
| `minigraph-clipboard` / store `items` | IndexedDB | `ClipboardContext` | workspace clipboard records |

The panel split between the console and the right panel is deliberately **not** persisted. The
JSON-Path playground also mounts `useSavedGraphs('')`, which writes `{}` under the empty key — a
harmless leftover.
