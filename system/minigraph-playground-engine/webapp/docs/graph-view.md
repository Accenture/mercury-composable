# The graph view and the authoring layer

The Graph tab renders the live session graph on a React Flow canvas and lets the user author it
in place. Rendering is `utils/graphTransformer.ts` + `components/GraphView/`; authoring is
`components/GraphAuthoring/useGraphAuthoring.ts` + `graphActions/` + `hooks/useGraphUndo.ts`.
Nothing in this layer talks to the engine directly: the canvas redraws from the `graphData` prop,
and every gesture becomes a console command sent by the parent.

## Data model (`utils/graphTypes.ts`)

```ts
interface MinigraphNode       { alias: string; types: string[]; properties: { skill?; description?; question?; mapping?: string[]; [key: string]: unknown } }
interface MinigraphRelation   { type: string; properties: Record<string, unknown> }
interface MinigraphConnection { source: string; target: string; relations: MinigraphRelation[] }   // one block per ordered pair
interface MinigraphGraphData  { nodes: MinigraphNode[]; connections: MinigraphConnection[] }
```

`isMinigraphGraphData` only checks that `nodes` is an array; `connections` may be absent on a
partial graph, so consumers read `data.connections ?? []`. Rendering and theming use `types[0]`
(`'default'` / `'unknown'` when empty); the layout's root/end tests use `types.includes('Root' |
'End' | 'entry_point')` or the aliases `root` / `end`.

## Layout: `transformGraphData` and `computeLayout` (`utils/graphTransformer.ts`)

`transformGraphData(data, { supportsConnectionAuthoring, compactNodes })` returns React Flow
`{ nodes, edges }`; `computeMeasuredPositions(data, measuredHeights, { compactNodes })` re-runs the
same layout with real DOM heights. The algorithm, in order:

1. **Classify.** A node that appears in any connection is a *flow* node. Unconnected nodes go to
   segregated rows by category: `types[0]` Dictionary, Provider, a `skill` of `graph.math` /
   `graph.js` (Module), no skill (Entity), anything else (`__unknown__`).
2. **Adjacency** over flow-to-flow edges, neighbours sorted alphabetically (determinism).
3. **Break cycles** with an iterative DFS seeded from in-degree-0, `entry_point` and root-like
   nodes, then every alias in sorted order; an edge into a grey node is a back edge. Back edges are
   left out of the level assignment but still rendered.
4. **Connected components** (undirected DFS): the component holding a root-like node first, the
   one holding an end-like node last, the rest by first alias; laid out left to right with a
   360 px gap.
5. **Levels** by longest-path layering: a BFS from the seeds that never moves a node to a
   shallower level, skipping back and cross-component edges; unreached nodes land at max + 1. A
   join node therefore sits one column after its deepest branch; siblings share the next column.
6. **Crossing minimisation** per component (`minimizeCrossings`): the alphabetical order is the
   baseline; long forward edges become virtual slots at the intermediate levels (height 100) when
   the total span is ≤ 10,000; the score is (node intrusions, edge crossings), intrusions first.
   Four barycentre passes (down then up), each followed by a bounded transpose search (≤ 64
   slots, ≤ 256 segments, 512 evaluations) when the component is small enough; ties keep the
   previous position. If the baseline has fewer intrusions than the best result, the baseline wins.
   Intrusion scoring models React Flow's bezier (control points at mid-x, handle offsets as
   assigned later) and bisects the curve's y-range over a node's x-span; it is skipped above
   2,000 edge–node pairs.
7. **Pixels.** `x = componentOffset + level × (240 + 120)`; each column is stacked with 60 px gaps
   and centred on y = 0, with virtual slots reserving corridors for long edges.
8. **Intrusion relief** (`relieveNodeIntrusions`, ≤ 8 passes): the first remaining intrusion, in
   canonical order, moves its node (and every node in the same column on that side) just past the
   curve plus 16 px.
9. **Segregated rows** below the flow (+120 px): Dictionary, Provider, Module, Entity, unknown,
   one row each, alphabetical, 360 px apart, 80 px between rows.

Node heights are estimated first (40 px header + 9 px + 18 px × ⌈chars / 22⌉ per property line;
floor `NODE_HEIGHT` 100; a handle floor of `(n − 1) × 24 + 64` when a side has several handles).
Thumbnail (compact) mode uses the floor only. `GraphView` then **relayouts once with measured
heights** per (graphData, compact) pair: it waits until every node has `measured.height`, checks
the nodes really come from this `graphData` (same `properties` object identity, same mode), applies
only the positions that changed and `fitView({padding: 0.1})` in a `requestAnimationFrame`. Later
resizes with the `NodeResizer` do not snap the layout back. Expanded nodes get `initialHeight`
(the DOM then follows the content, `style.minHeight` as the floor); a fixed `height` only ever
comes from the user.

### Edges and handles

A connection is a **back edge** when `level(source) ≥ level(target)`: it leaves the source's left
side and enters the target's right side, so the bezier curls backwards; forward edges run right
→ left. On each side, handles are sorted by the peer's y, then alias, and offset from the vertical
centre by 0 (one handle), ±24 (two) or `(i − (n−1)/2) × 24`; ids are `source-N`, `target-N`,
`back-source-N`, `back-target-N`. Edge ids are `${source}__${target}__${index}`, type `bezier`,
stroke and arrow always the neutral `rgba(148,163,184,0.42)`; the label is the relation types
joined with ", ", coloured by the first relation (`getConnectionRelationColor`: a preset palette for
the 18 known relations, otherwise one of 8 hashed colours).

### Node styling (`utils/minigraphNodeTheme.ts`)

`getMinigraphNodeTypeMeta(type)` gives an icon and label for 15 known types (Root, End, Fetcher,
mapper, Math, JavaScript, Provider, Dictionary, Join, Extension, Island, Decision, Suspend, Resume,
Suspensible; 📦 and the raw name otherwise); `getMinigraphNodeAccent(type)` the per-type colour
(`#6c7086` for unknown); `getMinigraphNodeShellStyle(type)` the card style with `borderColor` and
the CSS custom property `--node-accent`. The shell style is applied to React Flow's node wrapper —
the wrapper *is* the card; `NodeTypes.tsx` renders a Fragment (resizer, hidden edge handles,
`MinigraphNodeBody`, and the two authoring handles when connections may be authored). The same
shell style draws clipboard previews and colours the node editor.

## `GraphView` (`components/GraphView/GraphView.tsx`)

Props: `graphData`, `graphName?`, `onCopySuccess?`/`onCopyError?` (toolbar copy),
`graphRunControls?`, `onRenderError?`, `isRefreshing?`, `onClipNode?`, `onClipNodes?`,
`onClipboardDrop?`, `isActive` (the Graph tab is visible — enables the minimap hotkey),
`isConnected`, `supportsAuthoring?`, `onCreateNode?(source)`, `onCreateConnection?(source,
target, anchor?)`, `onEditNode?`, `onDeleteNode?`, `onDeleteNodes?`, `onDeleteConnections?
(requests)`, `panelLayoutKey?`. Authoring actions need `supportsAuthoring && callback &&
isConnected`; clip actions need only their callback; a clipboard drop needs `isConnected`.

What it renders:

- `GraphToolbar` (name, counts, copy) with `GraphRunControls` in its actions slot, only when the
  graph has nodes.
- `<ReactFlow>` with controlled nodes/edges (every new transform replaces both, clears the
  selection and closes the menus), `fitView` padding 0.1, zoom 0.2–4, zoom on scroll / pinch /
  double-click, `panOnScroll` off (spelled out so an upgrade cannot change them),
  `nodesConnectable` only when a connection may be authored, `edgesReconnectable={false}`,
  `deleteKeyCode=['Delete','Backspace']` with `onBeforeDelete` (below), a dotted background.
- `GraphMinimap`: the one React Flow `<Controls>` group (zoom, fit, the thumbnail/details toggle
  passed in as children, the minimap toggle last). The minimap is a draggable "island"
  (`{left, bottom}`, default `{50, 15}`, clamped to the pane with an 8 px margin, persisted in
  `localStorage` `graph-minimap-position`); its open flag is plain state and survives a keyed
  remount. **Ctrl+M** toggles it while the Graph tab is active (Control, not Cmd, on macOS; not
  while typing in a field).
- The empty state ("No graph data yet." + a **Create Node** button when authoring is supported;
  disabled with "Connect WebSocket to create a node." while offline), the
  click-to-connect banner, the refresh spinner and the "Drop to paste workspace node" overlay.
- The three context menus (below) and a keyed `GraphViewErrorBoundary`: a transform error shows
  "Graph could not be rendered." and reports to `onRenderError` from an effect, never from inside
  `useMemo`; the boundary is keyed by the alias list so a corrected graph remounts cleanly.
- Thumbnail vs details: `localStorage` `graph-nodes-compact` (default false).
- When `panelLayoutKey` changes (`${leftPanelMode}|${clipboardOpen}|${helpOpen}` from the
  parent), the graph re-fits after two nested animation frames; the key describes panel
  *visibility* only, so separator drags never re-fit.

### Selection, menus and gestures

- **Multi-select:** `Shift+drag` draws a box (`SelectionMode.Partial`); `Shift`/`Ctrl`/`Cmd` +
  click toggles nodes (nodes carry the class `nokey` so modifier clicks reach React Flow's
  selection). No on-screen hint: the modifier-click convention is the common one (the tip was
  removed on 2026-10-02).
- **Node menu** (right-click): a multi-node target when the node belongs to a selection of two or
  more (aliases de-duplicated, case-insensitive), else a single-node target. Items: "Clip to
  Workspace", "Connect to…", "Edit Node", "Delete Node" (inline confirmation `Delete "alias"?`);
  multi: "Clip N selected nodes…", "Delete N selected nodes" (confirmation). Targets are
  re-resolved against the current graph when the action runs, so stale aliases are dropped.
- **Edge menu:** one "Delete '{relation}'" item per relation plus "Delete all (N)"; no confirmation
  because the removal is undoable.
- **Pane menu:** "Create Node" (`source: 'pane-context-menu'`; the empty-state button is
  `'empty-graph'`, which pre-fills alias `root` and type `Root`).
- **Connecting:** drag from a node's halo ring (`authoring-source`) onto another node's body
  (`authoring-target`, active only during a drag from a *different* node); `isValidConnection`
  requires two distinct existing nodes. The drop point comes from a capture-phase `pointerup`
  listener that runs before React Flow's handlers, and the parent renders `ConnectionPopover` at
  that anchor. "Connect to…" arms click-to-connect ("Connecting from X — click a target node",
  cancelled by Escape, a pane click, a graph change, or losing the capability).
- **Delete / Backspace on selected edges:** `onBeforeDelete` always returns `false` (the backend
  owns mutations) and forwards the selected edges as one whole-edge request per directed pair to
  `onDeleteConnections`; selected *nodes* are ignored on purpose (nodes delete only through the
  confirmed menu flow).
- **Clipboard drop:** accepts drags carrying `application/x-minigraph-clipboard-item` (payload =
  item id) and hands the id to `onClipboardDrop`.

## Authoring

### The session state machine (`useGraphAuthoring`)

States: `closed { pendingSubmit, serverMessage }` and `open` for `create-node` / `edit-node`
(`formState: NodeFormState`, `originalAlias`) or `create-connection` (`ConnectionFormState`),
each with `phase: 'editing' | 'sending'`, `pendingSubmit`, `serverMessage`, `connectionLost`.
Only one action is pending at a time ("A graph authoring action is already pending…").

API: `openCreateNode(source)`, `openEditNode(node)` (re-reads the node from the latest
`graphData`, case-insensitively, and refuses nodes the form cannot represent), `openCreateConnection
(src, tgt)` (relation starts empty; refused for a self-connection or an unknown alias),
`deleteNode(node)` (no dialog: validate, build, send, pending), `deleteNodes(nodes)` (≤ 100,
de-duplicated, every command validated before any is sent, then sent one after another; an info
toast "N delete-node commands sent…"), `updateFormState`, `submit`, `close` (blocked while
sending). Options: `bus`, `connected`, `graphData`, `executor`, `timeoutMs` (10 s), `onAccepted`,
`onUserMessage`.

Results come back as `minigraph.nodeAction.textResult` events (see [protocol.md](protocol.md)) and
are matched to the pending action by alias (trimmed, case-insensitive) and action; any `error`
event matches a single pending submit because backend error text can be generic, and never matches
a batch, which then ends by timeout. Accepted → `closed` + `onAccepted(result)`; rejected or error →
back to `editing` with the server message (errors prefixed "Backend returned an error while this
submit was pending: "). A timeout returns an open dialog to `editing` with "…no backend result was
observed yet. The outcome is unknown."; a disconnect locks an open dialog (`connectionLost`) with
"Connection disconnected. Refresh the page and … again after the app reconnects."

### Commands (`graphActions/minigraphCommandBuilder.ts`)

The only module allowed to produce raw command text, so validation and injection guards stay in
one place; undo uses it too. Every builder re-validates and size-checks (`MAX_BUFFER` 63,488
characters):

```
create node {alias}            update node {originalAlias}       delete node {alias}
with type {Type}               (same shape as create)            delete connection {a} and {b}
with properties                                                  connect {a} to {b} with {relation}
{key}={value}                  ← single-line value
{key}='''                      ← a value containing a newline
{lines}
'''
```

Keys are trimmed, values keep their whitespace (CRLF → LF); a row is dropped only when key and
value are both blank; a key with a blank value is sent as `key=`. Rows sharing a key use the
`key[]=` append signature, so their order is the array order the engine stores.

### Validation (`graphActions/validation.ts`)

| Rule | Message |
|---|---|
| alias, type, relation and path segments match `^[A-Za-z0-9_-]+$` (aligned with the engine's `GraphProperties.validateName`) | "Use only letters, numbers, underscore, and hyphen." |
| create: alias required / reserved (`input`, `output`, `model`, `response`, `result`, `parameter`, `none`, `next`, `api`, `error`, case-insensitive) / already in the graph (advisory, the backend is authoritative) | "Alias is required." / `"{alias}" is reserved.` / `Node "{alias}" already exists in the current graph.` |
| property key: a dot/bracket path, brackets empty or a number | "Use a property name or dot/bracket path, for example mapping[] or config.value." |
| a value with a key missing / a value containing `'''` | "Property key is required when value is present." / "Property value cannot contain '''." |
| connection: source/target required, present in the graph, different (case-insensitive); relation required; connected | "Source is required.", `Node "{x}" is no longer available in the current graph.`, "Source and target nodes must be different.", "Relation is required.", "Connection disconnected. Reconnect before creating a connection." |
| delete connection: source `===` target | "Source and target must be different nodes." |
| command text longer than `MAX_BUFFER` | "The node command is too large. Shorten property values before submitting." |

The edit form is built by `graphActions/propertyRows.ts`: arrays flatten to `key[i]` (displayed
as `key[]`), objects to `a.b`, `null` to the string `'null'`; rows are sorted like the engine's
`edit node` listing (single bracket indices zero-padded to three digits for the sort). A node with
several types, an invalid alias, an empty array or object, an invalid path or a `'''` value is
refused: "This node contains data that cannot be safely represented in the edit form. Use the
console edit command for this node."

### Connection edits (`graphActions/connectionEdits.ts`)

`delete connection a and b` wipes **every** relation between the pair in both directions, so a
partial removal is planned as: `delete connection a and b`, then `connect a to b with r` for each
surviving a→b relation, then `connect b to a with r` for each b→a relation. Stale requests are
skipped; the plan returns `{commands, removed}` or null. The 18 preset relations and their colours
are in `connectionRelations.ts`; the relation is free text.

### Undo (`graphActions/undoCommands.ts`, `hooks/useGraphUndo.ts`)

Frontend-only compensation: every UI mutation snapshots the graph *before* the change and pushes
the inverse commands *after* the backend accepts it. Recipes: create node → `delete node`; edit →
the previous `update node …` (full replace); delete → the previous `create node …` plus `connect`
for every relation that touched it; create connection → `delete connection` plus the pair's previous
relations in both directions; relation removal → `connect` each removed directed relation.
Relation *properties* cannot be restored (`connect` only carries the type). The stack holds 20
entries in a ref, clears on disconnect and on an `import-graph` mutation, and never tracks commands
typed in the console. Commands are **paced**: after each send the hook waits for the next
`graph.mutation` event or 2.5 s, because the engine was seen answering a `connect` before a slow
multi-line `create` had finished. `Ctrl/Cmd+Z` (outside editable fields and authoring sessions) and
the toast's Undo button undo the newest entry; undoing an older entry out of order is refused.
Batch node delete is not undoable.

## Known rough edges (from the code, not fixed yet)

- `Suspend`, `Resume` and `Suspensible` are themed but absent from the `nodeTypes` map and the
  minimap palette; React Flow falls back to the default renderer (same component) with a console
  warning. Add new types to both maps.
- A connection naming an alias that is not in `nodes` makes `transformGraphData` throw ("Graph could
  not be rendered.") although the layout step skips it.
- The two self-connection checks disagree (delete is case-sensitive, connect case-insensitive).
- The command size check counts UTF-16 code units, not bytes.
