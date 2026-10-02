# Components

The component tree for one playground, as `Playground.tsx` composes it. The graph canvas and its
sub-components are in [graph-view.md](graph-view.md).

```
Playground
├── ToastContainer
├── ConnectionPopover                      (only during a create-connection session)
├── header: title, GraphSaveButton, SavedGraphsMenu, Console toggle, Workspace toggle,
│           Navigation ── SessionMenu, NavMenu "Tools", NavMenu "Quick Links",   help toggle (?)
├── ClipboardDuplicateDialog               (only while a clip collides)
└── panel group (react-resizable-panels)
    ├── left slot:  NodeEditPanel | MockUploadPanel | LeftPanel ── Console (ConsoleMessage*) + CommandInput
    ├── right:      RightPanel ── tabs: PayloadEditor (+SampleButtons) | GraphView | GraphDataView
    │                             └── vertical split with HelpBrowser when help is open
    └── sidebar:    ClipboardSidebar ── ClipboardItem*, ClipboardItemContextMenu, ClipboardEmptyState
```

## Rules every component follows

- **No transport in components.** None of them subscribes to the `ProtocolBus`, calls
  `sendRawText` or reads `localStorage`; they receive classified events through the
  `classificationMap` prop and call parent callbacks. The commands behind those callbacks live in
  `Playground.tsx` and the hooks ([hooks-and-state.md](hooks-and-state.md)). Two exceptions by
  design: `MockUploadPanel` POSTs through `useMockUpload`, and `Navigation` connects and
  disconnects sockets through the WebSocket context. `RightPanel` keeps its help split in
  `sessionStorage`.
- **In-place editors, not modals.** `NodeEditPanel` and `MockUploadPanel` take the console's slot;
  the parent swaps the slot and the console's open flag is never touched, so closing the editor
  gives the slot back to whatever it held. `ConnectionPopover` is a popover anchored at the gesture.
- **Escape closes the thing that opened last.** The node editor, the upload panel, the connection
  popover, the clipboard context menu and the sort popover each register a document-level Escape
  handler; an in-flight submit blocks closing, exactly like the disabled Cancel button.
- **Presentational components own only transient UI state** (an open flag, an inline form); all
  domain state and validation sit in the hooks.

## Console

### `Console` (`components/Console/Console.tsx`)

Props: `messages: {id, raw}[]`, `classificationMap?`, `onCopy`, `onClear`, `consoleRef`,
`onGraphLinkMessage?(msg)`, `onCopyMessage?()`, `onSendToJsonPath?(json)`,
`onUploadMockData?(uploadPath)`, `successfulUploadPaths?: Set<string>`.

Renders the "Console Output" header with copy-all and clear buttons, then the log
(`role="log"`, `aria-live="polite"`) with one `ConsoleErrorBoundary`-wrapped `ConsoleMessage` per
row, or the empty state "No messages yet. Use the Start button in the header to connect." The
parent owns `consoleRef` (auto-scroll lives in `useWebSocket`, plus a re-scroll when the console
remounts).

### `ConsoleMessage` (`components/Console/ConsoleMessage.tsx`)

Looks up its events in `classificationMap.get(msgId)` and renders by kind:

| Row | Rendering |
|---|---|
| JSON body (`json.response`) | collapsible tree (`react-json-view-lite`), top level expanded |
| graph link | the 🕸 icon; the row is a `role="button"` ("Click to load graph in Graph View") that calls `onGraphLink` |
| mock-upload invitation | ⬆ icon; a "⬆ Upload JSON…" button re-opens the upload panel for that path; a ✅ "Upload succeeded" badge once the path is in `successfulUploadPaths` |
| large payload | ⬇ icon (the content itself is appended as a separate JSON row by `useLargePayloadDownload`) |
| lifecycle JSON | the type icon (ℹ️ info, ❌ error, 👋 welcome) and the timestamp |
| anything else | plain text (markdown candidates are not rendered as markdown here; the help panel renders markdown) |

Per-row hover buttons: copy (📄 → ✅ for two seconds, via `useCopyToClipboard`), ➡️ "send to
JSON-Path" for JSON rows (sends the pretty-printed JSON), and the upload re-open. Invitation and
large-payload rows are never activatable as a whole row (WCAG: no nested interactive controls).

`ConsoleErrorBoundary` shows the raw text and logs to `console.error` if a row fails to render.

### `CommandInput` (`components/CommandInput/CommandInput.tsx`)

Props: `command`, `onChange`, `onKeyDown` (the history navigation from `useWebSocket`), `onSend`,
`sendDisabled`, `disabled`, `history` (newest first).

- A `role="combobox"` textarea that grows with its content; placeholder "Not connected" when
  disabled. **Send** button and hint line below.
- **Command palette** (the prompt icon next to the label): `COMMAND_QUICKSTART` rows from
  `utils/commandSuggestions.ts` (help, create, update, edit, delete node/connection/cache, connect,
  list nodes/connections, describe graph/node/connection/skill, export, import graph/node,
  instantiate (`start`), upload mock data, execute, inspect, run). Picking a row **replaces** the
  input with the row's template and refocuses the textarea.
- **History dropup** ("Recent Commands"): `useHistoryAutocomplete` filters the history by a
  case-insensitive prefix of the first line, at most 8 entries; navigated with
  `aria-activedescendant`, focus never leaves the textarea.
- Keys: `Enter` accepts a highlighted suggestion or sends; `Shift+Enter` inserts a newline; `Tab`
  accepts the active (or first) suggestion and is always consumed; `Escape` closes the dropup;
  `ArrowUp`/`ArrowDown` move in the dropup when open, otherwise reach the history only from the
  first/last line of the text. In the palette, arrows wrap and `Escape` returns to the textarea.

## Payload editor (JSON-Path)

### `PayloadEditor` and `SampleButtons`

Props: `payload`, `onChange`, `validation` (`{valid, error, type}` from `utils/validators.ts`),
`onFormat`, `onUpload?`. Shows the character count, a JSON/XML badge with ✅/❌, **Format** (JSON
only), **Upload** (only when `onUpload` is passed and the JSON is valid: it starts the
`upload` handshake, see [protocol.md](protocol.md)), the textarea and the error line.
`SampleButtons` renders the `SAMPLE_DATA` keys of `config/playgrounds.ts` grouped by prefix
(`json_*`, `xml_*`); a click replaces the payload.

## Panels and help

### `LeftPanel`

A pass-through that stacks `Console` over `CommandInput` and forwards their props.

### `RightPanel` (`components/RightPanel/RightPanel.tsx`)

Props: the tab set (`tabs`, `activeTab`, `onTabChange`), the payload editor props, the graph
props (`graphData`, `graphName`, `graphRunControls?`, `isGraphRefreshing?`, the clip and
authoring callbacks, `isConnected`, `supportsAuthoring`), `panelLayoutKey` and `helpPanel`.

- The tab strip renders only with more than one tab: "Payload Editor", "Graph" (with a 🕸 badge
  while a graph is loaded), "Graph Data (Raw)". **Every enabled tab stays mounted** and is hidden
  by CSS, so the canvas keeps its zoom and pan.
- With `helpPanel` set, the panel becomes a vertical split: the tab content above (`minSize` 0%),
  the help below (`minSize` 15%, default 45%). `helpPanel` may be a render function receiving
  `(toggleMaximize, isMaximized)`; maximize resizes to 0/100 and restore returns to the saved
  resting size. Dragging to ≥ 98% counts as maximized.
- `sessionStorage` keys `help-split-percent` (resting size only) and `help-split-maximized`
  (`'1'`/`'0'`): the split survives help toggles and playground navigation and clears with the
  browser tab, matching the engine session's lifetime.

### `HelpBrowser` (`components/HelpBrowser/HelpBrowser.tsx`)

Props: `activeTopic` (`''` = the root `help.md`; `'create'` = `help create.md`), `onNavigate`,
`onClose?`, `onToggleMaximize?`, `isMaximized?`, `contentProfile` (`'minigraph'` default, or
`'json-path'`).

Category tabs from `getHelpCategories(profile)` (Overview, Graph Model, Graph Skills, Instance
Model, Tutorials with the "Chapters" chip strip), a chip strip when a category has several pages,
maximize (⛶/⊞) and close (×) buttons, and the page as markdown (`react-markdown` + `remark-gfm`).
On the root page, list items that start with `help ` become buttons when the topic is bundled
("help create (node)" opens `create`). The body scrolls to the top on every topic change.
`useHelpScrollNavigation` flips to the previous/next page in the global order when the user keeps
scrolling past the top or bottom (120 px of overscroll with a rubber-band hint, 650 ms cooldown).
Content comes only from the bundled `data/helpContent.ts`; a missing topic shows
"`help <topic>` not found in the local bundle."

## Header bar

### `Navigation` (`components/Navigation.tsx`)

Props: `addToast`, `sessionCollaboration?` (null hides the Session menu). Renders:

- `SessionMenu` when enabled.
- **Tools** menu with an aggregate status dot (connected / idle / connecting / partial): a
  "Connect All" / "Disconnect All" button (connects only idle slots, disconnects connected and
  connecting ones) and, per playground, a `NavLink` with its dot plus a Start / Stop button whose
  title is the socket URL. The button is separate from the link so it never navigates.
- **Quick Links** (new tab): `/info`, `/info/lib`, `/info/routes`, `/health`, `/env`, and the two
  legacy pages `http://localhost:8085/api/ws/json` and `…/api/ws/graph` (hard-coded host).

Uses `useWebSocketContext()` directly: `getSlot(wsPath).phase`, `connect(wsPath, addToast)`,
`disconnect(wsPath)`.

### `NavMenu`

A dropdown primitive: trigger with an optional status dot and `aria-expanded`, a `role="menu"`
that is **unmounted while closed** (children lose their state on close), closing on the trigger,
an outside `mousedown`, or Escape (which refocuses the trigger). Nothing closes it when an item is
clicked.

### `SessionMenu` (`components/SessionMenu/SessionMenu.tsx`)

Prop: `controller: SessionCollaborationController` (from `useSessionCollaboration`). The "Session"
NavMenu shows: the session id with a copy button (`navigator.clipboard`, check icon for 1.4 s),
"Started since …", a **+** toggle for the subscribe form (placeholder `ws-123456-1`, the Subscribe
button calls `controller.subscribeToSession`), the "Subscribed to" row with **Unsubscribe**, the
"Subscribers" list, **Reset Session** (shown only for a primary session that has subscribers), and
the controller's error line. While disconnected it says "Connect Minigraph to view session
details." The dot is connecting while a refresh is pending, connected once the id is known.

### `SavedGraphsMenu` and `GraphSaveButton`

`SavedGraphsMenu` ("Load Graph (N)"): one row per `localStorage` bookmark (name, saved date)
with **Load** (disabled while disconnected; sends `import graph from <name>`) and **Delete**
(removes the bookmark only, no confirmation). `GraphSaveButton` shows "💾 Save Graph" or
"✅ Saved: <name>" and opens an inline name form (pre-filled with the default name from
`useGraphSaveName`, "Overwrite?" when the name exists, Enter confirms, Escape cancels); `onSave`
receives the trimmed name and the parent runs `export graph as <name>`. It is disabled with no
graph or no connection (`connected` defaults to `false`, so the parent must pass it).

## Graph toolbar and data view

### `GraphToolbar` and `GraphRunControls` (`components/GraphToolbar/`)

`GraphToolbar` shows the graph name (default "Untitled"), "N node(s) · M connection(s)", optional
`extraActions`, and a copy button that writes the graph JSON (pretty-printed) to the clipboard.
`GraphRunControls` (rendered inside the toolbar by `GraphView`) is a `role="group"` with
**Instantiate** and **Run**:

| Phase | Instantiate label | Run |
|---|---|---|
| idle | Instantiate | disabled ("Instantiate the graph first") |
| instantiating / requesting-input / awaiting-input / outcome-uncertain | Instantiating… / Preparing… / Input required / Waiting… (`aria-busy`) | disabled |
| ready | Instantiate (enabled again only when the graph reads `input.body`) | Run |
| running | — | Running… |

Each button has a tooltip with its purpose and the current reason when disabled
(`disabledReason`: "Load a graph first", "Connect first to run the graph", …); a disabled button's
wrapper is focusable so keyboard users can read it. The phase machine is `useGraphRunWorkflow`.

### `GraphDataView`

The raw graph JSON with the toolbar's "Expand all" / "Collapse all" actions and a `JsonView`;
"No graph data yet." when nothing is loaded.

## Toasts

`ToastContainer` (`components/Toast.tsx`) renders `useToast`'s list: an icon per type (✅ ❌ ℹ️),
click to dismiss, an optional action button (used for **Undo**). Auto-dismiss (3 s by default,
6 s for undo toasts) is timed in `useToast`.

## In-place editors and popovers

### `MockUploadPanel` (`components/MockUploadPanel/MockUploadPanel.tsx`)

Props: `uploadPath` (e.g. `/api/mock/ws-417669-24`), `onSuccess(body, uploadPath)`,
`onClose(uploadPath)`, `onError(message)`, and the optional `title`, `description`,
`inputPathHints` and `submitLabel` the run workflow passes ("▶ Mock Graph Input", the
`input.body.*` paths the graph references, "Upload & Instantiate").

A ribbon with the path and a close button, the hints (first six as `<code>`), a drop zone with a
hidden `.json` file input and "Browse file…", the textarea (focused on mount), the status line
("⚠ Invalid JSON — check syntax"), **Format**, **Cancel** and the submit button. Only a JSON
object or array can be submitted (`tryParseJSON`, primitives refused on purpose: the mock endpoint
is JSON-only). `Ctrl/⌘+Enter` submits, `Escape` closes unless an upload is in flight. The POST is
`useMockUpload` (raw textarea text, `Content-Type: application/json`); a non-2xx answer shows
"❌ Upload failed: HTTP <status> — <body>" inline and reaches the parent's toast. One panel is open
at a time; further invitations queue FIFO in `useMockUploadPanel`.

### `NodeEditPanel` (`components/NodeEditPanel/NodeEditPanel.tsx`)

Props: `mode` (`'create'` | `'edit'`), `formState` (`alias`, `nodeType`, `properties[{id, key,
value}]`), `phase` (`'editing'` | `'sending'`), `lockReason` (`null` | `'sending'` |
`'disconnected'`), `serverMessage`, `validationErrors`, `onFormStateChange`, `onSubmit`, `onClose`.

A form card coloured by the node type's accent (`utils/minigraphNodeTheme.ts`): the ribbon (alias
input in create mode, text in edit mode; the type badge; close), the server message and validation
alerts, the "type" input, property rows (⠿ grip, key input, value textarea sized 1–10 rows,
remove) and "+ Add Property", then **Cancel** and **Create Node** / **Save Changes**. Rows can be
reordered by drag and drop and are kept sorted by key: rows sharing a key use the `[]` append
signature, so their order is the array order on submit. Initial focus: alias (create) or type
(edit). `Escape` closes unless sending; inputs lock while sending or disconnected. Validation,
transport and result matching are `useGraphAuthoring`'s ([graph-view.md](graph-view.md)).

### `ConnectionPopover` (`components/ConnectionPopover/ConnectionPopover.tsx`)

Props: `formState` (`sourceAlias`, `targetAlias`, `relation`), `phase`, `lockReason`,
`serverMessage`, `validationErrors`, `anchor` (viewport point of the drop; null = top centre),
`onFormStateChange`, `onSubmit`, `onClose`. A `role="dialog"` "Create connection from A to B":
the relation chips from `graphActions/connectionRelations.ts` (fetch, details, ext-call, mapping,
compute, calculate, evaluate, fork, join, one, two, three, more, done, complete, finish, positive,
negative), a custom relation input (autofocused, Enter submits) and **Connect**. Clicking a chip
sets the relation and submits once the new value has come back through props. Positioned 14 px
below the anchor and clamped 12 px inside the viewport; `Escape` or a pointer-down outside closes
it unless sending.

## Workspace clipboard sidebar

### `ClipboardSidebar` (`components/ClipboardSidebar/ClipboardSidebar.tsx`)

Props: `connected`, `onPasteToInput(item)`. Reads the `ClipboardContext` (IndexedDB-backed items,
`removeItem`, `clearAll`). Header "Workspace" with **Clear**, a **Sort** popover (Recent, Type,
Alias, Source, Connections, Property with a key input; direction; defaults from
`getDefaultClipboardSortDirection`), the item list (or "Loading…" / `ClipboardEmptyState`), an
inline "Inspect node <alias>" JSON panel, and the item context menu ("Paste to Input" when
connected, "Inspect"). Sort state is local and not persisted. A menu or inspect panel whose item
vanished (removed in another tab) closes itself.

### `ClipboardItem`, `ClipboardItemContextMenu`, `ClipboardDuplicateDialog`, `ClipboardEmptyState`

`ClipboardItem` shows the node preview (`MinigraphNodeBody` with alias, first type and
properties) and "Clipped <relative time> from <source>"; it is draggable (MIME type
`application/x-minigraph-clipboard-item`, payload = the item id) and opens its menu on right-click,
the ContextMenu key or Shift+F10. The menu clamps 16 px inside the viewport and closes on outside
pointer-down, Escape, scroll or resize. `ClipboardDuplicateDialog` is a native `<dialog>`
("Duplicate Node… Replace it with the new snapshot?") with Cancel / Replace; `Escape` cancels.
`ClipboardEmptyState` says "Right-click a node in the Graph view to get started."
