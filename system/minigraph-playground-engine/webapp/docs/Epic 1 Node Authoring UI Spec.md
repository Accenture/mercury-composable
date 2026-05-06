# Epic 1 Spec: UI Node Creation For Minigraph Graph Surface

**Status:** final implementation spec

**Decision:** implement UI-based create-node authoring as a frontend authoring layer over the existing Minigraph raw command path. The backend remains unchanged: validated UI draft -> legacy `create node` command text -> existing WebSocket command flow -> existing backend text result -> frontend best-effort result observation -> backend-sourced graph refresh.

## 1. Product Requirement

Minigraph users can create nodes without typing raw console commands.

This feature supports two user-visible entry points:

1. First node from no imported graph: when the Minigraph WebSocket is connected and the Graph tab has no loaded graph projection, the user can click `Create Node` in the Graph tab empty state.
2. Additional node from an existing graph: when a graph is rendered, the user can right-click empty graph canvas and choose `Create Node` from a pane context menu.

Both entry points open the same Create Node modal. The user enters alias, optional type, and flat scalar properties. On submit, the frontend validates the draft, builds backend-compatible raw command text, sends it through the existing WebSocket command path, observes existing backend text best-effort, and relies on the existing graph refresh flow to show the new node.

Success is not an optimistic UI mutation. Success is backend acceptance plus refreshed graph data that contains the new node.

## 2. User Journeys

### 2.1 First Node Without Imported Graph

Starting state:

- User is on the Minigraph playground.
- WebSocket is connected.
- No graph was imported or pinned in the right-panel Graph tab.
- `graphData === null`, or the current loaded projection is empty with `graphData.nodes.length === 0`.
- `graphData === null` means "no loaded frontend graph projection"; it does not prove the backend session graph is empty because the user may have typed console commands without running `describe graph`.

Flow:

1. Graph tab empty state shows `Create Node`.
2. User clicks `Create Node`.
3. Create Node modal opens.
4. Empty-state create opens with a UX default of `alias = root` and `nodeType = Root`; this is only a starting suggestion for the common first runnable graph node, not a forced value. The user can edit both fields before submit.
5. User submits.
6. Frontend sends raw command text such as:

```text
create node root
with type Root
with properties
name=demo
```

7. Backend creates the node in the existing WebSocket session graph.
8. Backend returns existing text such as `node root created`.
9. Classifier still emits the existing `graph.mutation`.
10. Existing auto-refresh sends `describe graph`, pins/fetches the graph JSON, and Graph tab displays the node.

Failure, empty, and loading states:

- If disconnected, the button remains visible but disabled with connection-specific wording.
- If validation fails, modal stays open and no command is sent.
- If backend rejects, modal stays open with backend message and preserved draft.
- If send fails or timeout fires, the draft remains editable only while the modal stays open.
- If connection drops, the modal remains visible but locks all fields and submit controls until the user closes or reloads it.

### 2.2 Create Node From Existing Graph Pane

Starting state:

- WebSocket is connected.
- `graphData !== null`.
- ReactFlow renders the graph.

Flow:

1. User right-clicks empty ReactFlow pane, not a node.
2. Pane context menu opens at the right-click pointer position.
3. User selects `Create Node`.
4. Create Node modal opens with an empty draft.
5. Submit lifecycle follows the same raw-command path as the first-node journey.

Success signal:

- Existing graph refresh completes.
- Refreshed graph contains the new node.

Failure, empty, and loading states:

- Right-clicking a node opens the existing node context menu, not the pane create menu.
- If create action is unavailable or disconnected, pane context menu does not offer an enabled create action.
- If graph refresh fails after backend acceptance, the modal is already accepted/closed and the existing graph fetch error is shown.

### 2.3 Invalid Input

Starting state:

- Create Node modal is open.

Flow:

1. User enters invalid alias, invalid node type, invalid property key, unsupported property value, reserved alias, or a duplicate alias known from current graph data.
2. User submits.
3. Client validation blocks send.

Success signal:

- User can correct fields and submit again.

Failure behavior:

- No WebSocket command is sent for client-invalid input.

### 2.4 Backend Rejection Or Error

Starting state:

- Create Node modal submitted a command.
- Hook is waiting for backend text or timeout.

Flow:

1. Backend rejects, for example because alias exists in authoritative graph state.
2. Backend returns existing text such as `node foo already exists`, or generic `ERROR: ...`.
3. Frontend applies the result only while a submit is pending.

Success signal:

- Modal returns to editable state.
- Draft is preserved.
- Inline backend message is shown.

### 2.5 Timeout, Disconnect, And Refresh

Starting state:

- Create Node modal is open, or a submit is pending.

Flow:

- If send fails before reaching an open socket, show send failure and preserve the draft.
- If timeout fires after successful send, show unknown-outcome wording.
- If WebSocket disconnects while editing or sending, lock the current modal, disable all fields and submit controls, and show a connection-disconnected message.
- If the user closes the modal or hard-refreshes the browser, the unsaved draft is discarded.

Success signal:

- User can distinguish timeout from disconnect: timeout leaves the current draft editable, while disconnect locks the current modal.
- After disconnect, the current create-node form cannot be submitted safely. The user must close or reload, reconnect, and start a fresh create-node attempt.

Important rule:

Timeout and disconnect while sending are unknown outcomes, not proven failures. If the previous raw command actually succeeded, a fresh later create attempt may receive an authoritative duplicate-alias backend rejection. There is no draft recovery mode in this design.

Disconnect wording:

- while editing: `Connection disconnected. This graph session may no longer be valid. Refresh the page and create the node again after the app reconnects.`
- while sending: `Connection disconnected while the create-node command was pending. The outcome is unknown. Refresh the page and check the graph before trying again.`

Console interleaving rule:

- The left console remains enabled while a modal submit is pending.
- Alias-matched created/rejected text is the only result treated as alias-specific.
- Generic `ERROR: ...` has no command id or alias. While a create-node submit is pending, the hook must surface it only with cautious wording such as `Backend returned an error while this submit was pending: ...`; it must not say `node <alias> failed`.

## 3. Non-Goals

This feature does not implement:

- edit-node UI;
- delete-node UI;
- edge or connection authoring;
- drag-to-place coordinates;
- optimistic graph mutation;
- backend CRUD API;
- backend structured authoring envelope;
- command-id correlation;
- graph persistence changes;
- reconnect workflow inside the dialog;
- nested/composite property authoring;
- multiline property authoring;
- replacement of the console command input;
- authoring for JSON-Path playground.

The design must leave a clean future path for edit/delete, but those actions are outside the current scope.

## 4. Codebase Evidence

### 4.1 Graph Surface

`GraphView` currently renders the Graph tab empty state when there is no graph data or no nodes: [GraphView.tsx:127](../src/components/GraphView/GraphView.tsx#L127).

`GraphView` renders ReactFlow for loaded graphs: [GraphView.tsx:151](../src/components/GraphView/GraphView.tsx#L151).

The existing node-specific context menu is attached through `onNodeContextMenu`: [GraphView.tsx:163](../src/components/GraphView/GraphView.tsx#L163).

ReactFlow exposes pane-level right-click handling through `onPaneContextMenu`: [component-props.d.ts:196](../node_modules/@xyflow/react/dist/esm/types/component-props.d.ts#L196).

### 4.2 Empty Graph Backend Session

When the WebSocket opens, `GraphCommandService` creates a new empty `MiniGraph` for the route: [GraphCommandService.java:126](../../src/main/java/com/accenture/minigraph/services/GraphCommandService.java#L126).

Therefore, the "no imported graph" journey can send `create node ...` as long as the Minigraph WebSocket is connected. It does not need an imported graph file first.

### 4.3 Existing WebSocket Command Path

`GraphUserInterface` receives WebSocket strings and forwards them as current command events with `type`, `in`, `message`, and `out`: [GraphUserInterface.java:88](../../src/main/java/com/accenture/minigraph/websocket/server/GraphUserInterface.java#L88).

`GraphCommandService.handleCommand(...)` routes string commands through the existing single-line or multiline command handlers: [GraphCommandService.java:139](../../src/main/java/com/accenture/minigraph/services/GraphCommandService.java#L139).

The backend echoes commands with `> ` before sending operation results: [GraphCommandService.java:154](../../src/main/java/com/accenture/minigraph/services/GraphCommandService.java#L154).

Backend create-node logic already exists and returns existing text responses:

- duplicate alias: [GraphCommandService.java:1012](../../src/main/java/com/accenture/minigraph/services/GraphCommandService.java#L1012);
- created: [GraphCommandService.java:1019](../../src/main/java/com/accenture/minigraph/services/GraphCommandService.java#L1019).

Backend command exceptions are wrapped as `ERROR: ...`: [GraphCommandService.java:105](../../src/main/java/com/accenture/minigraph/services/GraphCommandService.java#L105).

### 4.4 Existing Refresh Flow

The classifier emits `graph.mutation` for existing raw mutation text: [classifier.ts:133](../src/protocol/classifier.ts#L133).

`detectMutation(...)` recognizes `node <alias> created` as node mutation text: [messageParser.ts:329](../src/utils/messageParser.ts#L329).

`useAutoGraphRefresh` listens for `graph.mutation` and sends `describe graph`: [useAutoGraphRefresh.ts:59](../src/hooks/useAutoGraphRefresh.ts#L59), [useAutoGraphRefresh.ts:75](../src/hooks/useAutoGraphRefresh.ts#L75).

`useGraphData` fetches backend graph JSON from the pinned graph path and updates `graphData`: [useGraphData.ts:121](../src/hooks/useGraphData.ts#L121).

### 4.5 Send Boundary

`WebSocketContext.send(...)` returns `false` unless the socket is open: [WebSocketContext.tsx:353](../src/contexts/WebSocketContext.tsx#L353).

`useWebSocket.sendRawText(...)` must return that boolean so authoring can surface send failure: [useWebSocket.ts:306](../src/hooks/useWebSocket.ts#L306).

### 4.6 Validation Source

Backend name validation accepts only `0-9`, `A-Z`, `a-z`, underscore, and hyphen: [GraphProperties.java:53](../../../platform-core/src/main/java/org/platformlambda/core/models/GraphProperties.java#L53).

`MiniGraph` reserves aliases such as `input`, `output`, `model`, `response`, `result`, `parameter`, `none`, `next`, `api`, and `error`: [MiniGraph.java:42](../../../platform-core/src/main/java/org/platformlambda/core/graph/MiniGraph.java#L42).

`MiniGraph.createNode(...)` rejects empty alias/type, reserved aliases, and duplicate aliases case-insensitively: [MiniGraph.java:349](../../../platform-core/src/main/java/org/platformlambda/core/graph/MiniGraph.java#L349).

Existing tests cover reserved alias behavior: [GraphTest.java:268](../../../platform-core/src/test/java/org/platformlambda/core/GraphTest.java#L268).

## 5. Existing Behavior Compatibility

| Existing behavior | Treatment | Reason |
|---|---|---|
| Console users type raw commands. | Unchanged. | UI authoring sends the same backend command grammar. |
| Backend sends raw text responses. | Unchanged. | Avoid breaking console rendering and existing classifier rules. |
| Graph refresh is backend-sourced. | Preserved. | No optimistic mutation. |
| WebSocket session owns graph state. | Preserved. | No REST graph identity is introduced. |
| Node context menu clips node to clipboard. | Extended only by separation. | Node right-click remains node-specific; pane right-click is new. |
| JSON-Path graph tab. | Unchanged; authoring disabled. | JSON-Path is not a Minigraph authoring surface. |

Compatibility standard:

```text
Console path:
  raw text command -> GraphCommandService -> raw text response

UI authoring path:
  validated UI draft -> raw text command -> GraphCommandService -> raw text response
```

## 6. Architecture Decision

### 6.1 Chosen Option

Use a frontend authoring layer over the existing raw command path.

```text
GraphView create intent
  -> useGraphAuthoring opens NodeDialog
  -> validate NodeDraft
  -> buildCreateNodeCommand(draft)
  -> GraphAuthoringExecutor.execute(rawText)
  -> useWebSocket.sendRawText(rawText)
  -> existing GraphUserInterface and GraphCommandService
  -> existing raw text result
  -> classifier emits create-node text result plus existing graph.mutation
  -> useGraphAuthoring applies best-effort modal outcome
  -> useAutoGraphRefresh fetches backend graph projection
```

### 6.2 Alternatives Considered

| Option | Root primitive | Summary | Decision |
|---|---|---|---|
| A - direct modal raw send | React modal plus string concatenation | Modal builds/sends command directly. | Rejected: validation and result lifecycle would be coupled to UI components. |
| B - frontend authoring layer over raw text | Typed UI draft plus executor/parser/hook | UI owns draft, validation, timeout/disconnect handling, and best-effort result observation; backend unchanged. | Selected. |
| C - structured backend authoring protocol | JSON envelope plus command id | Frontend sends command id; backend returns structured result. | Deferred: best correctness, but changes backend contracts. |

### 6.3 Why Not REST CRUD

A fresh project might add `POST /graph/nodes`. In this codebase, graph state is WebSocket-session-scoped. REST CRUD would require a new durable graph identity or a second session mapping. That is outside the current scope and unnecessary for UI create-node entry points.

## 7. Source Of Truth And Boundary Map

### 7.1 Source Of Truth

| Concept | Owner/source of truth | Read path | Write path | Sync rule |
|---|---|---|---|---|
| WebSocket connection | `WebSocketContext` slot phase | `useWebSocket` and authoring hook | socket lifecycle handlers | Send only when connected and `send(...)` returns true. |
| Backend session graph | `GraphCommandService.graphModels` keyed by WebSocket route | backend commands, graph export/describe | WebSocket open/close and graph commands | Frontend graph data is only a projection. |
| Rendered graph | `useGraphData.graphData` | `GraphView`, validation duplicate hint | backend graph fetch | Refetched after `graph.mutation`; cleared on disconnect. |
| Create draft | `useGraphAuthoring` | NodeDialog | user edits while modal is open | In-memory only; discarded on close, unmount, or browser refresh. |
| Pending submit | `useGraphAuthoring` | timeout/result/disconnect handlers | successful send | Cleared on accepted/rejected/error/disconnect/unmount; timeout keeps it only for a possible late alias-matched result until edit, resubmit, close, or unmount. |
| Create result | existing backend raw text | classifier and authoring hook | backend text response | Best-effort only; backend graph remains authoritative. |
| Parser literals | `messageParser.ts` constants/tests | classifier | code change | Tests catch known backend wording drift. |

No frontend state is authoritative for node existence.

### 7.2 Boundary Map

| Boundary | Contract |
|---|---|
| `GraphView -> Playground` | GraphView emits create intent only; no command send. |
| `RightPanel -> GraphView` | Pass authoring availability, connection state, and create callback. |
| `NodeDialog -> useGraphAuthoring` | Dialog edits/submits draft; hook owns lifecycle. |
| `useGraphAuthoring -> executor` | Executor accepts serialized command and returns send boolean. |
| Browser -> backend | WebSocket string is the same raw command the console sends. |
| `GraphUserInterface -> GraphCommandService` | Existing command event shape: `type`, `in`, `message`, `out`. |
| Backend -> browser | Existing raw text: echo, created, already exists, `ERROR: ...`. |
| Classifier -> ProtocolBus | Existing events remain; this feature adds a frontend-only create-node text result event. |
| Trust boundary | Modal fields are untrusted. |

## 8. Contracts

### 8.1 Playground Feature Flag

`playgrounds.ts` adds:

```ts
supportsAuthoring?: boolean;
```

Enable only Minigraph:

```ts
{
  label: 'Minigraph',
  supportsAuthoring: true,
}
```

JSON-Path omits or sets `supportsAuthoring: false`.

### 8.2 Draft Types

```ts
export interface PropertyRow {
  id: string;
  key: string;
  value: string;
}

export interface NodeDraft {
  alias: string;
  nodeType: string;
  properties: PropertyRow[];
  source: 'empty-graph' | 'pane-context-menu';
}
```

`source` is frontend-only. It is not serialized into the backend command.

`source` is only the UI entry point. It must not be used as proof that the backend graph is empty or loaded.

### 8.3 Raw Command Request

Only `graphActions/minigraphCommandBuilder.ts` serializes a draft:

```ts
export function buildCreateNodeCommand(draft: NodeDraft): string;
```

Output shape:

```text
create node <alias>
with type <nodeType>
with properties
<key>=<value>
```

Rules:

- first line is always `create node <alias>`;
- omit `with type` if `nodeType.trim()` is empty;
- omit `with properties` if no non-ignored property rows exist;
- emit one `key=value` line per non-ignored row;
- preserve property row order;
- do not append trailing newline;
- throw if called with invalid draft;
- never allow user fields to contribute raw command line breaks;
- reject the draft if the serialized command would exceed the existing WebSocket command budget `MAX_BUFFER`.

### 8.4 Raw Text Result Parser

`utils/messageParser.ts` adds:

```ts
export type CreateNodeTextResultStatus = 'accepted' | 'rejected' | 'error';

export interface CreateNodeTextResult {
  status: CreateNodeTextResultStatus;
  alias: string | null;
  message: string;
}

export function parseCreateNodeTextResult(raw: string): CreateNodeTextResult | null;
```

Parser rules:

```ts
const CREATE_NODE_CREATED_RE = /^node ([A-Za-z0-9_-]+) created$/;
const CREATE_NODE_ALREADY_EXISTS_RE = /^node ([A-Za-z0-9_-]+) already exists$/;
const ERROR_RE = /^ERROR: (.+)$/;
```

- parse `raw.trim()`;
- ignore command echoes where text starts with `> `;
- created -> `{ status: 'accepted', alias, message }`;
- already exists -> `{ status: 'rejected', alias, message }`;
- `ERROR: ...` -> `{ status: 'error', alias: null, message }`;
- return `null` for JSON, graph links, docs/help text, command echoes, and unrelated node operations such as update/delete/connect.

### 8.5 Protocol Event

`events.ts` adds:

```ts
export interface CreateNodeTextResultEvent extends ProtocolEventBase {
  kind: 'minigraph.createNode.textResult';
  status: 'accepted' | 'rejected' | 'error';
  alias: string | null;
  message: string;
}
```

`classifier.ts` emits this event from `parseCreateNodeTextResult(raw)`.

A successful `node <alias> created` must still emit the existing `graph.mutation`. The create-node text result event is additive; it does not replace mutation detection.

### 8.6 Draft Persistence

There is no draft recovery record and no `sessionStorage` persistence for create-node drafts.

Rules:

- Keep the draft only in `useGraphAuthoring` while the modal is mounted.
- Closing the modal discards the current draft.
- Browser refresh, route unmount, or playground remount discards the current draft.
- Timeout keeps the draft editable in the current open modal.
- Disconnect locks the current open modal and disables all create-node fields and submit controls.
- Property row ids are UI-only and are never serialized or persisted.

## 9. Frontend Design

### 9.1 File Plan

Add:

```text
src/
  components/
    GraphAuthoring/
      GraphAuthoringModals.tsx
      useGraphAuthoring.ts
    GraphView/
      GraphContextMenu.tsx
      GraphContextMenu.module.css
    NodeDialog/
      NodeDialog.tsx
      NodeDialog.module.css
  graphActions/
    graphAuthoringExecutor.ts
    minigraphCommandBuilder.ts
    nodeAuthoringTypes.ts
    propertyRows.ts
    validation.ts
```

Modify:

| File | Change |
|---|---|
| `playgrounds.ts` | Add and enable `supportsAuthoring` for Minigraph only. |
| `useWebSocket.ts` | Return boolean from `sendRawText`. |
| `events.ts` | Add create-node text result event. |
| `classifier.ts` | Emit create-node text result events without changing `graph.mutation`. |
| `messageParser.ts` | Add create-node text result parser. |
| `GraphView.tsx` | Add empty-state create button and pane context menu. |
| `RightPanel.tsx` | Pass authoring props to GraphView. |
| `Playground.tsx` | Own authoring hook, executor, and modal mount. |

### 9.2 `GraphView`

`GraphView` remains a display and intent component. It does not validate drafts or send commands.

Add props:

```ts
interface GraphViewAuthoringProps {
  isConnected: boolean;
  supportsAuthoring?: boolean;
  onCreateNode?: (source: 'empty-graph' | 'pane-context-menu') => void;
}
```

Empty state:

- show `Create Node` when `supportsAuthoring && onCreateNode`;
- enable only when `isConnected`;
- invoke `onCreateNode('empty-graph')`;
- keep existing empty-state guidance secondary.

Pane context menu:

- use ReactFlow `onPaneContextMenu`;
- prevent browser default menu;
- use `event.clientX` and `event.clientY` from the pane right-click event for menu placement;
- open only for connected Minigraph authoring; when disconnected, do not open the pane menu;
- invoke `onCreateNode('pane-context-menu')`;
- render the menu at the pointer position in viewport coordinates, not inside or relative to the ReactFlow pane;
- close on outside click, Escape, pane click, scroll, resize, and create action.

Node context menu:

- keep current node menu behavior.
- Opening pane menu closes node menu.
- Opening node menu closes pane menu.
- Node right-click must call both `event.preventDefault()` and `event.stopPropagation()` so it does not open the pane create menu.

### 9.3 `GraphContextMenu`

```ts
interface GraphContextMenuProps {
  open: boolean;
  x: number;
  y: number;
  canCreateNode: boolean;
  onCreateNode: () => void;
  onClose: () => void;
}
```

Behavior:

- render a lightweight menu with one action: `Create Node`;
- use `role="menu"` and `role="menuitem"`;
- auto-focus the first enabled menu item;
- do not trap focus like a modal;
- close on Escape and outside pointer down.

Layout/CSS:

- use fixed viewport positioning, not absolute positioning inside `GraphView`;
- position the menu with `left: x` and `top: y` from the pane right-click `clientX/clientY`;
- size the menu with viewport-safe constraints such as `width: min(240px, calc(100vw - 32px))`;
- keep it above the graph canvas and below any modal/backdrop z-index;
- do not center the pane context menu; only the create-node modal is centered.

### 9.4 `NodeDialog`

Fields:

- alias;
- optional node type;
- flat scalar property rows.

Recommended defaults:

- empty-graph source: alias `root`, node type `Root`;
- pane-context-menu source: blank alias/type;

These default field values are mandatory for the empty-state entry point so implementation stays deterministic. They are not forced values and do not bypass normal validation. Do not choose context-sensitive aliases such as `node-1`.

Modal behavior:

- centered over the whole viewport, not clipped by the right panel;
- backdrop covers left and right panels;
- native `<dialog>` is allowed;
- content scrolls internally;
- footer actions remain visible;
- focus moves into dialog on open;
- close button and Escape work while editing, rejected, timeout, error, or disconnected;
- close button and Escape are disabled/prevented while sending;
- controls are disabled while sending or disconnected;
- submit uses explicit button handler, not implicit form submit.

Layout/CSS:

- use fixed viewport positioning for the dialog/backdrop layer;
- center the dialog with a viewport-level grid/flex centering container or native `<dialog>` equivalent;
- size the dialog with viewport-safe constraints such as `width: min(520px, calc(100vw - 32px))` and `max-height: calc(100vh - 32px)`;
- keep header and footer visible while the form body scrolls internally;
- do not anchor or clip the dialog to the left graph pane, right detail panel, or ReactFlow canvas.

Disconnect behavior:

- If disconnect happens while the modal is open, set `connectionLost = true`, clear any pending submit, and show connection-disconnected wording.
- While `connectionLost = true`, disable alias, node type, property key/value fields, Add Property, Remove Property, and Create Node.
- The user can close the locked modal, but cannot edit or submit it.
- The draft is not persisted. Closing the modal, refreshing the browser, or unmounting the playground discards it.
- Reconnect does not unlock the current modal or prove the previous backend session state. The user must close or reload and start a fresh create-node attempt.

Props:

```ts
interface NodeDialogProps {
  open: boolean;
  draft: NodeDraft;
  phase: 'editing' | 'sending';
  lockReason: null | 'sending' | 'disconnected';
  serverMessage: string | null;
  validationErrors: Record<string, string>;
  onDraftChange: (draft: NodeDraft) => void;
  onSubmit: () => void;
  onClose: () => void;
}
```

### 9.5 `useGraphAuthoring`

State:

```ts
type AuthoringState =
  | { status: 'closed' }
  | {
      status: 'open';
      action: 'create-node';
      phase: 'editing' | 'sending';
      draft: NodeDraft;
      pendingSubmit: PendingCreateNodeSubmit | null;
      serverMessage: string | null;
      connectionLost: boolean;
    };

interface PendingCreateNodeSubmit {
  alias: string;
  command: string;
  sentAt: string;
}
```

Hook API:

```ts
interface UseGraphAuthoringOptions {
  bus: ProtocolBus;
  connected: boolean;
  graphData: MinigraphGraphData | null;
  executor: GraphAuthoringExecutor;
  timeoutMs?: number;
  onAccepted?: (result: CreateNodeTextResult) => void;
}

interface UseGraphAuthoringReturn {
  state: AuthoringState;
  validationErrors: Record<string, string>;
  openCreateNode: (source: 'empty-graph' | 'pane-context-menu') => void;
  updateDraft: (draft: NodeDraft) => void;
  submit: () => void;
  close: () => void;
}
```

Default timeout:

```ts
export const DEFAULT_AUTHORING_TIMEOUT_MS = 10_000;
```

Responsibilities:

- no-op `openCreateNode` if disconnected;
- default draft by source;
- validate before submit;
- call `buildCreateNodeCommand(draft)` only after validation;
- call executor and start timeout only after successful send;
- no-op `submit()` while already sending;
- no-op `submit()` while `connectionLost` is true;
- no-op `updateDraft()` while sending or while `connectionLost` is true;
- no-op `close()` while sending;
- subscribe to `minigraph.createNode.textResult`;
- accepted result closes modal only when alias matches pending submit;
- rejected result applies only when alias matches pending submit;
- generic `ERROR: ...` applies only while a submit is pending and must be worded as best-effort;
- timeout preserves draft only in the currently open modal;
- disconnect locks the currently open modal, clears pending submit, clears the timer, and requires close/reload before another create attempt;
- editing after rejected/error/timeout clears stale `serverMessage` and stale pending submit matching;
- unmount clears timeout and discards any in-memory draft;
- normal close while editing discards the current unsent draft;
- generic `ERROR: ...` keeps the draft editable and clears the pending timer;
- ignore non-matching or stale text results.

### 9.6 `GraphAuthoringModals`

Thin mount component:

```ts
interface GraphAuthoringModalsProps {
  state: AuthoringState;
  validationErrors: Record<string, string>;
  onDraftChange: (draft: NodeDraft) => void;
  onSubmit: () => void;
  onClose: () => void;
}
```

Behavior:

- render nothing when closed;
- render `NodeDialog` for `create-node`;
- compute `lockReason` from `phase` and `connectionLost`;
- own no command/send logic.

### 9.7 Executor

```ts
export interface GraphAuthoringExecutor {
  execute(commandText: string): boolean;
}

export function createGraphAuthoringExecutor(
  sendRawText: (text: string) => boolean,
): GraphAuthoringExecutor {
  return {
    execute(commandText) {
      return sendRawText(commandText);
    },
  };
}
```

Executor does not build commands, parse results, mutate React state, or show toasts.

## 10. Validation And Command Building

All modal input is untrusted.

### 10.1 Alias

Rules:

- trim before validation;
- required;
- allow only `^[A-Za-z0-9_-]+$`;
- reject reserved aliases case-insensitively: `input`, `output`, `model`, `response`, `result`, `parameter`, `none`, `next`, `api`, `error`;
- duplicate check against current `graphData.nodes` is advisory and case-insensitive;
- backend duplicate/rejection remains authoritative.

### 10.2 Node Type

Rules:

- optional in UI;
- trim before validation and serialization;
- if blank after trim, omit `with type` so backend command path uses its default behavior;
- if present, allow only `^[A-Za-z0-9_-]+$`;
- reject whitespace, dots, brackets, equals, quotes, CR, LF, and command syntax.

### 10.3 Properties

This feature supports flat scalar properties only.

Property row rules:

- trim key and value before validation and serialization;
- rows with blank key and blank value after trim are ignored;
- blank key with non-blank value is invalid;
- non-blank key with blank value is valid and serializes as `key=`;
- key allowlist is `^[A-Za-z0-9_-]+$`;
- value is a single-line string;
- reject CR, LF, and `'''`;
- preserve row order.

Composite paths such as `a.b`, `items[]`, and multiline values are future work for a more explicit property editor.

Command size:

- After validation and serialization, reject the draft if `buildCreateNodeCommand(draft).length > MAX_BUFFER`.
- Use the existing `MAX_BUFFER` constant from `config/playgrounds.ts`; do not invent a second limit.
- The user-visible message should say the node command is too large and that property values must be shortened.

### 10.4 Injection Guard

The builder must have tests proving these inputs cannot create extra command lines:

- alias with `\n`;
- node type with `\r`;
- property key containing `=` is rejected before command serialization;
- property value containing `\nwith properties`;
- property value containing `'''`.

## 11. Failure Matrix

| Path | Detection | User-visible result | Cleanup |
|---|---|---|---|
| Disconnected before open | `connected === false` | Create entry disabled or no-op | no draft opened |
| Invalid input | validation | inline field errors | no command sent |
| Send false | executor returns false | send failure message, draft preserved | pending cleared |
| Accepted | `node <alias> created` matches pending alias | modal closes; graph refresh proceeds | timeout cleared, draft discarded |
| Duplicate/rejected | `node <alias> already exists` matches pending alias | modal editable with backend message | timeout cleared, pending cleared |
| Generic backend error | `ERROR: ...` while pending | cautious best-effort backend error message; do not claim the alias failed | timeout cleared, pending cleared |
| Timeout | timer fires | unknown-outcome warning; modal remains editable | timeout cleared; draft remains only in open modal |
| Disconnect while editing | connection transition | modal locks; all fields and submit controls disabled; refresh guidance shown | timeout cleared; pending cleared; no persistence |
| Disconnect while sending | connection transition | modal locks; all fields and submit controls disabled; unknown-outcome refresh guidance shown | timeout cleared; pending cleared; no persistence |
| Hard refresh | browser reload | no draft restore | previous pending observation and draft are lost |
| Non-matching text result | alias differs | no dialog change | none |
| Late accepted after timeout | matching pending alias while modal remains open and before user edits | close matching dialog if still open | graph refresh may still run |
| Late result after disconnect | pending submit was cleared by disconnect | no dialog change | locked modal remains until close/reload |
| Late result after draft edit | pending submit was cleared by edit | no dialog change | current draft remains |
| Close while sending | `phase === 'sending'` | ignored/prevented | none |
| Graph refresh failure after accepted | fetch error | existing graph fetch error toast/message | modal remains closed |

## 12. Lifecycle Data Access Audit

| Runtime datum | Read moment | Rule |
|---|---|---|
| WebSocket open state | before open/submit | Check `connected` and send boolean. |
| Graph session | no-import create | WebSocket open has already created empty `MiniGraph`; no import required. |
| `graphData.nodes` | validation duplicate hint | Advisory only; may be stale. |
| Pane context-menu event | context menu open | Use `clientX/clientY` to place the menu at the pointer and prevent browser default. |
| Dialog ref/native dialog | after mount | Use effect; prevent `cancel` while sending. |
| Timeout id | submit/result/disconnect/unmount | Clear on every terminal path. |
| Late backend text | result callback | Apply only to matching pending alias while the modal remains open. |
| `connectionLost` | disconnect transition | Lock the current modal; do not unlock it on reconnect. |

## 13. Security And Trust

| Boundary/input | Trust level | Rule |
|---|---|---|
| alias | untrusted | client allowlist plus backend validation |
| node type | untrusted | optional strict token |
| property key | untrusted | flat strict token only |
| property value | untrusted | single-line scalar; reject multiline delimiters |
| serialized command | derived from untrusted input | emitted only by command builder after validation |
| backend raw text | untrusted transport output | parse exact known create-node patterns only |

Authorization:

- This feature introduces no new authorization surface.
- The dev playground continues using current WebSocket session scope.
- No cross-user or durable graph access is added.

Logging rule:

- logs may include action, status, alias, rawTextMatched, and graphMutation;
- logs should not include full property values by default.

## 14. Observability, Rollout, Reversibility

### 14.1 Debuggability

Because authoring uses the existing raw text transport, there is no backend command id.

Debug using:

- frontend action: `create-node`;
- source: `empty-graph` or `pane-context-menu`;
- expected alias;
- send boolean;
- raw backend text;
- classifier event kind;
- hook transition;
- existing graph refresh result.

Recommended fields:

```text
action, source, alias, status, sendAcceptedBySocket, rawTextMatched, graphMutation
```

### 14.2 Rollout

Roll out behind `supportsAuthoring`.

Release gates:

- no backend file changes;
- console create/update/delete/import/export smoke still passes;
- parser/classifier tests prove existing `graph.mutation` behavior remains;
- empty-state create works with no imported graph while connected;
- pane context menu create works on loaded graph;
- timeout keeps the draft editable only while the modal remains open;
- disconnect locks the current modal and requires close/reload before a fresh create attempt.

### 14.3 Rollback

Rollback:

1. Set `supportsAuthoring` false for Minigraph.
2. UI entry points disappear.
3. Existing console raw command path remains available.
4. No backend rollback or migration is required.

There are no create-node draft records to migrate or clean up.

## 15. Runtime Cost And Spike

Per playground instance:

- one authoring hook;
- one create-node text result bus listener;
- zero or one dialog;
- zero or one pane context menu;
- one timeout per visible pending submit;
- no per-node hook;
- no new runtime dependency.

Parser spike:

```text
2,000,000 parser iterations
baseline trim: 8.99575 ms
parser regex path: 66.033375 ms
parser cost: about 0.033 microseconds/message
```

Given current message retention `MAX_ITEMS = 200`, the create-node parser is not a performance risk.

## 16. Verification Plan

| Claim | Verification | Design section |
|---|---|---|
| First-node create works without imported graph while connected. | hook/integration smoke with mocked connected executor | Sections 2, 4, 9 |
| Pane right-click opens create menu only on empty pane. | GraphView component tests | Section 9 |
| Node right-click menu remains separate. | GraphView component tests | Section 9 |
| `sendRawText` surfaces send failure. | hook/executor tests | Sections 4, 9 |
| Builder emits exact command text and blocks injection. | command builder tests | Sections 8, 10 |
| Validation mirrors backend token/reserved-name rules. | validation tests | Sections 4, 10 |
| Parser recognizes only known create-node text. | message parser tests | Section 8 |
| Accepted create text still emits `graph.mutation`. | classifier tests | Sections 4, 8 |
| Hook applies only alias/command-matched result. | hook tests | Sections 9, 11 |
| Timeout preserves an editable draft only in the open modal. | hook tests | Sections 8, 11 |
| Disconnect locks the open modal and disables fields plus submit. | hook/dialog tests | Sections 8, 9, 11 |
| Modal cannot close while sending. | dialog tests | Section 9 |
| No backend files are changed. | git diff review | Sections 5, 14 |
| JSON-Path authoring disabled. | Playground/config tests | Sections 3, 8 |

Suggested test inventory:

```text
src/graphActions/__tests__/minigraphCommandBuilder.test.ts
src/graphActions/__tests__/validation.test.ts
src/graphActions/__tests__/graphAuthoringExecutor.test.ts
src/utils/__tests__/messageParser.createNodeTextResult.test.ts
src/protocol/__tests__/classifier.test.ts
src/components/GraphView/__tests__/GraphContextMenu.test.tsx
src/components/GraphView/__tests__/GraphView.authoring.test.tsx
src/components/GraphAuthoring/__tests__/useGraphAuthoring.test.tsx
src/components/GraphAuthoring/__tests__/GraphAuthoringModals.test.tsx
src/components/NodeDialog/__tests__/NodeDialog.test.tsx
src/components/__tests__/Playground.authoring.test.tsx
```

Current Vitest config uses `environment: "node"`: [vitest.config.ts:4](../vitest.config.ts#L4). DOM `.tsx` tests need a DOM-capable setup such as jsdom.

Test-environment rule:

- Do not add a runtime dependency for tests.
- If jsdom is already available or approved as a dev dependency, run the DOM component tests listed above.
- If no DOM test environment is available during implementation, keep the pure parser/builder/validation/executor/hook tests in CI and record the dialog/context-menu checks as manual viewport smoke for that slice instead of silently weakening the implementation behavior.

## 17. Implementation Slices

### Slice 1 - Protocol And Transport Surface

Files:

- `utils/messageParser.ts`
- `protocol/events.ts`
- `protocol/classifier.ts`
- `hooks/useWebSocket.ts`
- `graphActions/graphAuthoringExecutor.ts`

Behavior:

- create-node text parser exists;
- classifier emits frontend-only result event;
- accepted create text still emits existing `graph.mutation`;
- authoring executor can observe send failure.

Rollback:

- Safe because no UI entry point depends on it yet.

### Slice 2 - Draft, Validation, Builder, Hook

Files:

- `graphActions/nodeAuthoringTypes.ts`
- `graphActions/propertyRows.ts`
- `graphActions/validation.ts`
- `graphActions/minigraphCommandBuilder.ts`
- `components/GraphAuthoring/useGraphAuthoring.ts`

Behavior:

- open/create/submit lifecycle works under mocked bus/executor;
- timeout keeps the modal editable, and disconnect locks the modal;
- invalid input blocks send.

Rollback:

- Components are not mounted until Slice 3 wiring.

### Slice 3 - Dialog UI

Files:

- `components/NodeDialog/NodeDialog.tsx`
- `components/NodeDialog/NodeDialog.module.css`
- `components/GraphAuthoring/GraphAuthoringModals.tsx`

Behavior:

- modal renders, validates, submits, and blocks close while sending.

Rollback:

- Disable Slice 4 wiring or feature flag.

### Slice 4 - Graph Surface Wiring

Files:

- `components/GraphView/GraphView.tsx`
- `components/GraphView/GraphContextMenu.tsx`
- `components/GraphView/GraphContextMenu.module.css`
- `components/RightPanel/RightPanel.tsx`
- `components/Playground.tsx`
- `config/playgrounds.ts`

Behavior:

- empty-state `Create Node`;
- pane right-click `Create Node`;
- Minigraph-only gating.

Rollback:

- Set `supportsAuthoring` false.

## 18. Consumer Readiness

Product/manager:

- Users can create the first node from the empty Graph tab without importing a graph.
- Users can create another node from the existing graph pane context menu.
- Edit/delete/connection authoring remain future work.

Engineer:

- Backend is unchanged.
- UI authoring uses typed draft, validation, command builder, executor, and hook lifecycle.

Reviewer:

- Approve Plan B's best-effort raw text result matching.
- Review context-menu composition and timeout/disconnect wording carefully.
- Confirm no optimistic graph mutation.

QA:

- Test no-import first-node create.
- Test existing graph pane right-click create.
- Test invalid input, duplicate alias, backend error, send failure, timeout, disconnect, hard refresh draft loss, and console regression.

Operator/support:

- Debug by alias, source, send boolean, raw backend text, classifier event, and graph refresh result.
- There is no backend correlation id.

## 19. Decision Log

Decision:

- Use frontend authoring layer over existing raw command text.

Alternatives:

- direct modal raw command send;
- structured backend authoring envelope;
- REST CRUD endpoint.

Why selected:

- satisfies both requested UI entry points;
- no backend contract change;
- preserves console behavior;
- keeps validation, serialization, result observation, and UI rendering separated;
- leaves a future migration path to structured backend correlation.

Tradeoffs accepted:

- no command-instance correlation;
- raw text parser depends on known backend wording;
- generic `ERROR: ...` cannot identify alias;
- timeout and disconnect while sending are unknown outcomes;
- no drag-to-place coordinates.

Revisit if:

- backend authoring protocol becomes approved;
- multi-user concurrent graph editing enters scope;
- graph persistence becomes first-class;
- edit/delete/connection authoring are added.

## Appendix - Planning Artifact (from /spec-plan)

**Phase 0 - Domain calibration**

- Domain mix: Frontend UI/rendering 35%; state/data flow 25%; network/RPC 15%; product workflow/UX 15%; security/privacy 5%; backend/platform 5%.
- Dominant failure mode: UI primitive composition and state cleanup can overstate certainty while the underlying transport only provides raw text best-effort matching.
- Pre-mortem watch-items:
  - pane and node context menus conflict;
  - no-import empty graph is confused with no backend session;
  - raw text result is treated as command-correlated when it is only alias-matched;
  - timeout or disconnect-while-sending wording implies failure instead of unknown outcome.
- Calibration: emphasized Step 5 source of truth, Step 6 contracts, Step 9 composition, Step 11 lifecycle, and Step 17 cleanup enumeration.

**Step 1 - Requirement and journeys**

- Actor / trigger / success signal: Minigraph user triggers `Create Node` from empty Graph tab or existing graph pane context menu; success is backend acceptance plus refreshed graph showing the new node.
- Primary journeys: first node without imported graph; pane context menu create on existing graph; invalid input; backend rejection/error; timeout/disconnect handling; hard-refresh draft loss.
- Non-goals: edit/delete, edges, drag placement, optimistic mutation, backend protocol changes, REST CRUD, nested/multiline properties, JSON-Path authoring.

**Step 3 - Anti-anchoring and compatibility**

- Silent assumptions:
  - ReactFlow remains the graph rendering surface.
  - WebSocket open creates the backend session graph.
  - `graphData` is only a projection, not graph truth.
  - ProtocolBus/classifier is the frontend message boundary.
  - Console raw command behavior must remain unchanged.
- Fresh-team delta: a fresh team might add REST CRUD or a structured authoring envelope; current codebase favors WebSocket session graph plus existing command grammar.
- Existing behavior contract: console path unchanged; backend text responses unchanged; `graph.mutation` refresh reused; JSON-Path unchanged; node clipboard menu preserved.

**Step 4 - Three architectures**

- Option A - root primitive: direct modal raw command send.
- Option B - root primitive: frontend authoring layer over raw command text.
- Option C - root primitive: backend structured authoring envelope with command id.
- Picked: Option B.
- Axis spread verified: yes; options vary by root primitive, transport contract, state abstraction, backend impact, and review size.

**Step 5 - Source of truth and boundaries**

- Source-of-truth inventory: WebSocketContext owns connection; GraphCommandService owns backend session graph; useGraphData owns frontend projection; useGraphAuthoring owns draft/pending state; backend raw text plus refreshed graph owns outcome.
- Boundary map: GraphView intent boundary; dialog/hook boundary; hook/executor boundary; browser/backend WebSocket boundary; classifier/bus boundary; trust boundary.

**Step 6 - Contracts**

- Boundary-crossing contracts: `NodeDraft`; raw `create node` command text; `CreateNodeTextResult`; `minigraph.createNode.textResult`; `supportsAuthoring`.
- Shared constants / schema strategy: frontend parser regex constants and command builder/validation constants; no backend/frontend generated type sharing in this feature.

**Step 9 - Primitive fit and composition**

- Under-use / over-use findings: structured backend correlation would be stronger but is outside the current scope; ReactFlow pane event is the right primitive for canvas context menu; native/custom dialog must not own command lifecycle.
- Composed primitives: ReactFlow pane/node context-menu triggers, pointer-positioned custom menu, centered NodeDialog, WebSocket send boolean, timeout, ProtocolBus events, existing graph refresh.
- Overlaps: pane vs node right-click; outside click vs menu click; Escape while sending; raw create result vs existing graph.mutation; timeout/result/disconnect race; late result after user edits draft.
- Guardrails: separate menu state; opening one menu closes the other; alias result matching; close no-op while sending; timeout cleanup; stale pending submit cleared on edit.

**Step 10 - From-scratch comparison**

- Materially simpler?: no for this codebase.
- If yes, redesign adopted: not applicable. REST CRUD is cleaner from scratch but requires a new graph identity/session contract here.

**Step 11 - Failure and lifecycle**

- Failure matrix summary: disconnected open, invalid input, send false, accepted, duplicate, generic error, timeout, disconnect, hard refresh, non-matching/late result, close while sending, refresh failure.
- Lifecycle data-access findings: WebSocket open state must be checked; open event creates empty backend graph; graphData aliases are stale hints; dialog ref exists only after mount; timeout id must clear on terminal paths.

**Step 12 - Spike / performance**

- Spike required?: yes, because parser runs per classified WebSocket message.
- Result or reason not required: 2,000,000 parser iterations took 66.033375 ms, about 0.033 microseconds/message.
- Per-N cost: with `MAX_ITEMS = 200`, create-node text parser cost is negligible; no per-node hooks or per-node menus are added.

**Step 13 - Security / trust**

- Input boundaries: alias, node type, property key/value, serialized command, backend raw text.
- Validation / auth / encoding: strict allowlists, reserved-name checks, single-line scalar values, builder-only serialization, exact parser regex, existing backend validation and WebSocket session scope.

**Step 14 - Observability / rollout**

- Debuggability: action, source, alias, sendAcceptedBySocket, rawTextMatched, classifier event kind, graphMutation, hook transition, graph fetch result.
- Rollout / rollback: `supportsAuthoring` gates Minigraph only; disable flag to rollback UI; no backend migration or rollback required.

**Step 15 - Verification**

- Verification mapping summary: validation tests, command builder tests, parser tests, classifier tests, executor tests, hook tests, dialog tests, GraphView context-menu tests, Playground config tests, no-backend-diff review, console regression smoke.
- Design cross-reference complete: yes.

**Step 16 - Implementation / consumer readiness**

- Implementation slices: protocol/transport; draft/validation/builder/hook; dialog UI; graph surface wiring.
- Review questions / decision log:
  - Do we accept best-effort alias/retained-command matching for this feature?
  - Do we accept no backend changes?
  - Do we accept timeout and disconnect while sending as unknown outcomes?
  - Do we accept flat scalar properties only?
  - Do we accept no drag-to-place coordinates?

**Step 17 - Pre-draft self-check**

- All self-check answers yes?: yes.
- Enumeration completeness:
  - state cleanup: pane menu, node menu, dialog draft, validation errors, serverMessage, pendingSubmit, timeout, connectionLost;
  - assumptions: WebSocket open creates empty graph, backend is authoritative, graphData may be stale, parser wording is exact, Minigraph-only authoring;
  - error/no-op feedback: disconnected open disabled, invalid input inline, send false inline, backend rejection inline, timeout unknown-outcome messaging, disconnect lock/refresh guidance, hard-refresh draft loss.
- Cross-iteration regression:
  - preserved the frontend authoring layer, ProtocolBus, backend-sourced refresh, no backend changes, and console compatibility;
  - strengthened no-import first-node evidence using WebSocket open creating `MiniGraph`;
  - strengthened pane context menu design using ReactFlow `onPaneContextMenu`;
  - kept structured backend protocol out of the current scope while documenting the future migration point.
