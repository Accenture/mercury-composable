# MiniGraph Webapp Continuity

## Project State

- **scope:** MiniGraph Playground React/Vite webapp
- **root:** `system/minigraph-playground-engine/webapp`
- **served bundle:** `system/minigraph-playground-engine/src/main/resources/public`
- **last_session:** 2026-09-09 | agent: Claude Code (2026-09-09-231740)

## Current Facts

- **UI PR stack integrated on `feature/ui-merges` (2026-07-29).** The three long-open UI PRs were
  integrated as ordered commits rather than merged wholesale: #108 multi-select (`60d76474`) → #116
  sorting-only delta (`f156908c`) → #121 session collaboration (`963f7fde`). The session collaboration
  UI introduced backend-authoritative session status, a Session menu, and raw command helpers for
  `session`, `session subscribe`, `session unsubscribe`, and `session reset`.
  <!-- id: webapp-ui-pr-stack | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: working | origin: 2026-07-29-160756 -->

- **Session auto-refresh loop fixed (2026-07-29).** The session collaboration hook sent `session` on
  connected mount, but its effect was coupled to a transport callback whose identity can change when
  WebSocket slot messages change. The tactical fix keeps the latest sender/toast callbacks in refs and
  makes `refreshSession` depend only on `enabled`/`connected`; the regression test injects unstable
  sender identity and verifies the initial request is not repeated after status processing.
  <!-- id: webapp-session-refresh-loop-fix | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: working | origin: 2026-07-29-160756 -->

- **Strategic transport lesson.** WebSocket imperative operations (`send`, `sendRawText`, connect/
  disconnect) should be stable and separated from reactive slot/message state. Command-sending effects
  should be transition- or event-driven, not rerender-driven by message-list identity churn.
  <!-- id: webapp-stable-transport-boundary | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: working | origin: 2026-07-29-160756 -->

- **Suspend/resume UI boundary.** Graph suspend/resume is an engine feature; the webapp owns visual
  conventions, help surfacing, and refresh/session behavior around it. Engine contracts remain in root
  memory; frontend follow-up belongs here unless it changes a backend contract.
  <!-- id: webapp-suspend-resume-ui-boundary | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: working | origin: 2026-07-29-160756 -->

- **Node resize visibility regression fixed (2026-08-19).** The UI integration commit `0119292d`
  added a global selected-node rule that set every React Flow resize control to `display: none`,
  overriding `NodeResizer isVisible={selected}` and disabling the established resize interaction.
  Removing that rule restores selected-node resize handles without changing multi-select or connection
  authoring; a focused happy-dom test renders the real node type, applies the production CSS, and pins
  the controls' visible computed style.
  <!-- id: webapp-node-resize-regression-fix | created: 2026-08-19 | last_used: 2026-08-19 | uses: 1 | tier: working | origin: 2026-08-19-163020 -->

- **Graph nodes are content-sized with a measured re-layout (2026-09-08).** Nodes carry no fixed
  `height`: `initialHeight` (content-aware estimate) sizes the pre-measurement paint,
  `style.minHeight` keeps the handle-count floor, and React Flow v12 drops `initialHeight` from
  inline styles after measurement so the DOM height follows content (a fixed `height` returns only
  via NodeResizer). GraphView re-runs the layout once per graphData with real measured heights
  (`computeMeasuredPositions`) and re-fits — this is the non-overlap guarantee. A bounded intrusion
  relief pass (computeLayout step 4.5) shifts a column chain vertically when no slot ordering can
  route a long edge around a tall node; it shares the geometry-pair budget with scoring. Stale-window
  guard for refreshes: RF node `data.properties` must be reference-identical to the current
  graphData node's before relaying out. Previously heights were fixed at ~100px and clipped 200–440px
  of content, which is also why older bundles showed overlapping nodes. A thumbnail/expanded toggle
  (React Flow `ControlButton`, persisted `graph-nodes-compact`) renders header-only cards via
  `transformGraphData({ compactNodes })`; toggling rebuilds the nodes and re-enters the same
  measure-then-relayout cycle (done-key = graphData + mode; fitView on every completed pass).
  <!-- id: webapp-content-sized-nodes-measured-relayout | created: 2026-09-08 | last_used: 2026-09-08 | uses: 1 | tier: working | origin: 2026-09-08-164102 -->

- **Node authoring (create AND edit) is an in-place panel, not a modal (2026-09-08, Eric's UX
  direction — same look-n-feel, nothing to learn; `NodeDialog` deleted).** The
  `NodeEditPanel` (mode 'create' | 'edit'; create edits the alias in the ribbon) renders in
  the left panel slot (the console's space) as a "magnified node":
  accent ribbon = node header (icon + alias + type badge, recolored live from the Node Type
  field), node-style aligned key/value grid rows, scrollable body. Esc / Cancel / successful
  save closes the session and the slot returns to its previous content — `consoleOpen` is
  never mutated by the editor. Create node/connection keep their modals
  (`GraphAuthoringModals` returns null for edit-node). useGraphAuthoring stayed the single
  owner of validation/transport; the panel is presentational, mirroring NodeDialog's contract.
  Key presentation mirrors the backend `edit node` listing: rows sorted ascending (zero-fill
  index compare) with array indices as the [] append signature — row order IS the array order
  on submit (`update node` clears properties; MultiLevelMap `[]` appends in line order).
  Per-row drag grips reorder; a drop re-sorts by key with a STABLE sort so same-key groups
  reform around the user's new relative order. CDP-synthesized mouse drags don't trigger
  HTML5 DnD — verify with dispatched DragEvents.
  <!-- id: webapp-edit-node-inplace-panel | created: 2026-09-08 | last_used: 2026-09-08 | uses: 1 | tier: working | origin: 2026-09-08-164102 -->

- **Connect gesture is Neo4j-style: body = move, halo ring = connect; no modal (2026-09-08,
  Eric's direction — "borrow the Neo4j graph browser UX").** The connect source Handle is a
  perimeter band with an evenodd clip-path (node body excluded from hit-testing); the halo
  shows on hover/selected; ring z sits under NodeResizer so resize keeps the border of a
  selected node. Mid-drag, every other node's full-body target overlay activates (inert
  otherwise). Alternative path: context-menu "Connect to…" arms click-a-target mode.
  Both paths end in `ConnectionPopover` anchored at the drop point (relation vocabulary as
  one-click colored chips + free text) — `ConnectionDialog`/`GraphAuthoringModals` deleted;
  the Playground has zero modals. Edges are selectable (blue selected stroke); removal is
  DIRECTED and relation-aware: Delete/Backspace removes selected edges through
  `onBeforeDelete` (ALWAYS returns false — the backend owns mutations), and right-click
  opens `EdgeContextMenu` with one item per relation (`Delete 'fetch'` / `Delete 'test'` /
  `Delete all (n)`). The engine's only removal command (`delete connection {a} and {b}`)
  wipes the pair in BOTH directions, so `planConnectionRemoval` (connectionEdits.ts)
  compiles requested directed removals into pair-delete + reconnect-survivors compounds
  (kept forward relations + the whole reverse direction), batched per gesture so a
  reciprocal selection plans one clean wipe. Keyboard delete deliberately ignores selected
  nodes (confirmed context-menu flow only). React Flow gotchas: Handles listen to
  mouse/touch (not pointer) events; verify body-vs-ring hit-testing with
  `document.elementFromPoint`; default `deleteKeyCode` would delete elements client-side
  only — always intercept.
  <!-- id: webapp-connect-ux-neo4j-halo | created: 2026-09-08 | last_used: 2026-09-08 | uses: 1 | tier: working | origin: 2026-09-08-164102 -->

- **Undo is frontend-only compensating commands (2026-09-08, Eric's ruling: the backend
  stays lightweight — minimalist design principle; no engine undo journal, hence no Rust
  lock-step obligation).** `undoCommands.ts` builds inverse console commands from
  pre-mutation graphData snapshots (connection removal ⇄ reconnect exactly the removed
  DIRECTED relations — the removal compound already preserved survivors; connection create
  ⇄ delete pair + restore both prior directions; node edit ⇄ re-submit full-replace
  snapshot; create ⇄ delete; node delete ⇄ recreate + reconnect); `useGraphUndo` (depth
  20) replays them over the WS and the graph redraws from backend confirmations. Triggers:
  Ctrl/Cmd+Z + per-toast Undo buttons (ToastAction). Invalidation: disconnect + graph
  import only — collaborative sessions keep last-write-wins; console-typed commands
  untracked; relation properties not restored; batch node-delete not undoable; no redo.
  All inverse text flows through the minigraphCommandBuilder boundary. The executor PACES
  commands (await each `graph.mutation` confirmation, 2.5s timeout): the backend can
  process a later single-line command before an earlier multi-line `create node`
  completes — unpaced node-delete undo lost its reconnects ("node X not found"). The same
  paced lane is exposed as `runCommands` for UI compounds (connection removals): batches
  queue FIFO behind the running one and share the busy flag with undo, so edits and undos
  never interleave.
  <!-- id: webapp-undo-compensating-commands | created: 2026-09-08 | last_used: 2026-09-08 | uses: 1 | tier: working | origin: 2026-09-08-164102 -->

- **Playground Help is profile-based (2026-09-09).** MiniGraph keeps its full bundled topic tree;
  JSON-Path uses the same resizable/maximizable Help shell but exposes only its focused Overview.
  Bare JSON-Path `help` resolves locally, while unsupported MiniGraph topics continue to the backend.
  Configuration, local-command interception, and auto-navigation all carry the same content profile.
  <!-- id: webapp-playground-help-profiles | created: 2026-09-09 | last_used: 2026-09-09 | uses: 1 | tier: working | origin: 2026-09-09-153221 -->

- **The graph minimap is optional local UI state (2026-09-09).** It is collapsed by default,
  pannable when open, and shares the single native React Flow Controls stack with zoom/fit and the
  existing thumbnail/detail toggle. `Ctrl+M` works only on the active usable Graph tab and ignores
  editable targets. A one-shot three-second hint pauses while hidden/focused, and mobile toasts use
  the graph-overlay safe area so they do not cover the minimap lane.
  <!-- id: webapp-toggleable-minimap | created: 2026-09-09 | last_used: 2026-09-09 | uses: 1 | tier: superseded | superseded-by: webapp-minimap-floating-island | origin: 2026-09-09-153221 -->

- **The open minimap is a draggable floating island; no onboarding hint (2026-09-09, Eric's
  post-regression direction — the promo hint "sold the feature" and the fixed lane consumed graph
  real estate).** Toggle semantics carry over from [[webapp-toggleable-minimap]] (collapsed by
  default, native Controls stack, `Ctrl+M` on the active Graph tab only, editable-target guard),
  but the map now renders in an absolutely-positioned island: a grip title bar drags it anywhere
  in the graph pane (window-level pointer listeners), position persists in localStorage
  (`graph-minimap-position`, default beside the control stack) and re-clamps on pane resize via
  ResizeObserver. Chosen over flowing it outside the pane: island works in every layout including
  fullscreen and steals no console/help space. React Flow gotcha: `<MiniMap>` renders its own
  absolutely-positioned `react-flow__panel` — neutralize with inline
  `style={{position:'relative', margin:0}}` so the wrapper owns placement; the map surface keeps
  `pannable` viewport-dragging (only the grip moves the island). `useMinimapHint` and the
  `minimapHintEligible` prop chain are deleted.
  <!-- id: webapp-minimap-floating-island | created: 2026-09-09 | last_used: 2026-09-09 | uses: 1 | tier: working | supersedes: webapp-toggleable-minimap | origin: 2026-09-09-223126 -->

- **MiniGraph toolbar execution mirrors backend lifecycle (2026-09-09).** Separate Instantiate and
  Run actions precede Copy; Run unlocks only after the instance acknowledgement and any required
  `input.body` upload. Typed ProtocolBus events, graph/session/connection invalidation, host-session
  gating, and stale-response quarantine keep the backend authoritative. Upload invitations use an
  active-path plus deduplicated FIFO: exact-path success/cancel/invalidation cannot affect another
  upload panel, and a workflow prompt is not lost behind a manual one. The text protocol still has
  no correlation id, so a workflow serially claims the next invitation after its request.
  (Presentation moved from a modal to the in-place [[webapp-mock-input-inplace-panel]] 2026-09-09;
  the lifecycle machine was untouched.)
  <!-- id: webapp-graph-toolbar-run-controls | created: 2026-09-09 | last_used: 2026-09-09 | uses: 1 | tier: working | origin: 2026-09-09-153221 -->

- **Mock-data input (create AND the graph-run workflow step) is an in-place left-slot panel, not
  a modal (2026-09-09, Eric's direction — same in-place convention as the node editor; the
  Playground is back to ZERO modals).** `MockUploadPanel` mirrors the
  [[webapp-edit-node-inplace-panel]] contract: renders in the console's slot, Esc / Cancel / a
  successful upload closes the session and the slot returns to its previous content, `consoleOpen`
  never mutated, and the header Console button unwinds the panel first. Both entry points share it:
  the graph-run workflow step (titled **"▶ Mock Graph Input"** — Eric: we are MOCKING input for a
  dry-run, not adding; the awaiting-input disabled-reason says "mock graph input panel") and the
  manual console-row re-open ("⬆️ Upload Mock Data"). Slot priority: node editor > upload panel >
  console — a hidden upload session survives and reappears when the editor closes. **Left-slot
  default widths (Eric's spec): console 40%, node editor 30%, mock input 30%** — applied on every
  slot-content change via the react-resizable-panels v4 imperative `panelRef.resize()` (deferred
  one rAF for closed→open mounts); `useDefaultLayout` uses `onlySaveAfterUserInteractions: true`
  so imperative resizes never overwrite the user's persisted drag, and `panelLayoutKey` carries
  the slot mode so the graph re-fits on width changes. The layout storage key is VERSIONED
  (`-panel-split-v2`, 2026-09-09): a persisted layout beats `defaultSize` at mount and the mode
  resize only fires on changes, so pre-defaults splits had to be orphaned — bump the suffix again
  if the default scheme ever changes.
  <!-- id: webapp-mock-input-inplace-panel | created: 2026-09-09 | last_used: 2026-09-09 | uses: 2 | tier: working | origin: 2026-09-09-223126 -->

- **The live session graph survives temp-model expiry — restore it, don't toast (2026-09-09,
  Eric's direction).** Described temp-model paths (`/api/graph/model/…`) expire server-side about
  a minute after `describe graph`, while the SESSION's live graph stays at
  `GET /api/graph/session/{id}` for the WebSocket session's lifetime. `useSessionGraphRestore`
  quietly fetches it when the pinned model path is a dead end (HTTP failure OR an HTTP-200
  error-envelope body) or none is pinned — one attempt per (session, pinned-path) pair, same
  shape guard and graph-tab auto-switch as a pinned load; `useGraphData` suppresses the failure
  toast where the restore owns it (`quietInitialFetchFailure`) and exposes `initialFetchFailed`.
  The session id arrives via the mount-time `session` round-trip (react to LATE arrival), each
  playground has its OWN ws-session id, and the pinned path re-arms on the next
  describe/mutation auto-refresh. Known backend bug (Eric, queued engine-side, Java+Rust
  lock-step): the model endpoint answers 200 with the current draft for a bogus id — should 404.
  <!-- id: webapp-session-live-graph-restore | created: 2026-09-09 | last_used: 2026-09-09 | uses: 1 | tier: working | origin: 2026-09-09-231740 -->

- **JSON-Path is a payload-only playground (2026-09-09, Eric's direction — the tool has no graph
  surface).** Its `tabs` config is just `['payload']`; the `tabs` list in `PLAYGROUND_CONFIGS`
  is the single authority for right-panel composition, and RightPanel hides the tab strip
  entirely for single-tab playgrounds. Stale persisted tab selections are already normalized by
  `normalizeRightTab`, so removing tabs from a config is safe without migrations.
  <!-- id: webapp-jsonpath-payload-only | created: 2026-09-09 | last_used: 2026-09-09 | uses: 1 | tier: working | origin: 2026-09-09-223126 -->

## Open Threads

- [ ] Review MiniGraph webapp architecture after the UI PR stack and graph suspend/resume engine work.
  Priority questions: stable WebSocket transport vs reactive slot state; command causality (`session`,
  `describe graph`, upload, collaboration commands); single owner for protocol classification/backlog
  dedupe; graph/session reset ordering; suspend/resume refresh semantics; and release/deploy validation
  for checked-in bundles. Initial report: `memory/architecture-review-2026-07-29.md`.
  <!-- id: thread-webapp-architecture-review | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: working | origin: 2026-07-29-160756 -->

- [ ] Manually inspect the Session menu across route navigation and reset before landing
  `feature/ui-merges`.
  <!-- id: thread-webapp-session-menu-manual-check | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: working | origin: 2026-07-29-160756 -->
