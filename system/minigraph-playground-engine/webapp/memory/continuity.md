# MiniGraph Webapp Continuity

## Project State

- **scope:** MiniGraph Playground React/Vite webapp
- **root:** `system/minigraph-playground-engine/webapp`
- **served bundle:** `system/minigraph-playground-engine/src/main/resources/public`
- **last_session:** 2026-10-02 | agent: Claude Code (2026-10-02-050449)
- **last_review:** 2026-10-02 | through 2026-10-02-041143.md (CADENCE — the layer's first review, 10 sessions: 13 footers
  refreshed, 2 superseded facts archived into the new `memory/archive/`, 1 drift thread raised)

## Current Facts

- **UI PR stack integrated on `feature/ui-merges` (2026-07-29).** The three long-open UI PRs were
  integrated as ordered commits rather than merged wholesale: #108 multi-select (`60d76474`) → #116
  sorting-only delta (`f156908c`) → #121 session collaboration (`963f7fde`). The session collaboration
  UI introduced backend-authoritative session status, a Session menu, and raw command helpers for
  `session`, `session subscribe`, `session unsubscribe`, and `session reset`.
  <!-- id: webapp-ui-pr-stack | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: archive-candidate | origin: 2026-07-29-160756 -->

- **Session auto-refresh loop fixed (2026-07-29).** The session collaboration hook sent `session` on
  connected mount, but its effect was coupled to a transport callback whose identity can change when
  WebSocket slot messages change. The tactical fix keeps the latest sender/toast callbacks in refs and
  makes `refreshSession` depend only on `enabled`/`connected`; the regression test injects unstable
  sender identity and verifies the initial request is not repeated after status processing.
  <!-- id: webapp-session-refresh-loop-fix | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: archive-candidate | origin: 2026-07-29-160756 -->

- **Strategic transport lesson.** WebSocket imperative operations (`send`, `sendRawText`, connect/
  disconnect) should be stable and separated from reactive slot/message state. Command-sending effects
  should be transition- or event-driven, not rerender-driven by message-list identity churn.
  <!-- id: webapp-stable-transport-boundary | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: archive-candidate | origin: 2026-07-29-160756 -->

- **Suspend/resume UI boundary.** Graph suspend/resume is an engine feature; the webapp owns visual
  conventions, help surfacing, and refresh/session behavior around it. Engine contracts remain in root
  memory; frontend follow-up belongs here unless it changes a backend contract.
  <!-- id: webapp-suspend-resume-ui-boundary | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: archive-candidate | origin: 2026-07-29-160756 -->

- **Node resize visibility regression fixed (2026-08-19).** The UI integration commit `0119292d`
  added a global selected-node rule that set every React Flow resize control to `display: none`,
  overriding `NodeResizer isVisible={selected}` and disabling the established resize interaction.
  Removing that rule restores selected-node resize handles without changing multi-select or connection
  authoring; a focused happy-dom test renders the real node type, applies the production CSS, and pins
  the controls' visible computed style.
  <!-- id: webapp-node-resize-regression-fix | created: 2026-08-19 | last_used: 2026-08-19 | uses: 1 | tier: active | origin: 2026-08-19-163020 -->

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
  <!-- id: webapp-edit-node-inplace-panel | created: 2026-09-08 | last_used: 2026-09-09 | uses: 1 | tier: active | origin: 2026-09-08-164102 -->

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
  <!-- id: webapp-connect-ux-neo4j-halo | created: 2026-09-08 | last_used: 2026-09-09 | uses: 1 | tier: active | origin: 2026-09-08-164102 -->

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
  <!-- id: webapp-playground-help-profiles | created: 2026-09-09 | last_used: 2026-10-02 | uses: 2 | tier: active | origin: 2026-09-09-153221 -->

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
  `input.body` upload. Typed ProtocolBus events, graph/session/connection invalidation and
  stale-response quarantine keep the backend authoritative (the host-session gating was removed on
  2026-10-02: see [[webapp-run-controls-equal-partners]]). Upload invitations use an
  active-path plus deduplicated FIFO: exact-path success/cancel/invalidation cannot affect another
  upload panel, and a workflow prompt is not lost behind a manual one. The text protocol still has
  no correlation id, so a workflow serially claims the next invitation after its request.
  (Presentation moved from a modal to the in-place [[webapp-mock-input-inplace-panel]] 2026-09-09;
  the lifecycle machine was untouched.)
  <!-- id: webapp-graph-toolbar-run-controls | created: 2026-09-09 | last_used: 2026-09-09 | uses: 3 | tier: active | origin: 2026-09-09-153221 -->

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
  default widths: node editor 30%, mock input 30% (Eric's spec), console one third since
  2026-10-01 (40% before)** — applied on every slot-content change via the react-resizable-panels
  v4 imperative `panelRef.resize()` (deferred one rAF for closed→open mounts), and `panelLayoutKey`
  carries the slot mode so the graph re-fits on width changes. The split is no longer persisted,
  so the versioned `-panel-split-v2` key and its bump-on-change rule are retired: see
  [[webapp-panel-split-not-persisted]].
  <!-- id: webapp-mock-input-inplace-panel | created: 2026-09-09 | last_used: 2026-10-02 | uses: 3 | tier: active | origin: 2026-09-09-223126 -->

- **The graph view's single source is the live session endpoint; describe/export links are a
  human surface (2026-09-10, Eric's design).** `useGraphData` fetches
  `GET /api/graph/session/{id}` (session id from `sessionCollaboration.state.sessionId`, async
  via the mount-time `session` round-trip — the hook must be called ABOVE the graph state in
  Playground.tsx); `useAutoGraphRefresh` re-fetches it on `graph.mutation` (300 ms debounce,
  import immediate) with NO `describe graph` round-trip and NO `graph.link` listener; the
  initial load is always quiet (fresh session = zero-node export, unknown session = 404 — both
  map to the canvas empty state via `toRenderableGraph`, never a toast or tab yank); the Graph
  tab is revealed only when a fetch delivers the FIRST content; mutation refetches ride the
  `isRefreshing` overlay. `usePinnedGraphPath` and `useSessionGraphRestore` are deleted;
  `graphIdentity` = the session path (mutations invalidate via the run workflow's own bus
  subscription). `describe graph` / `export graph` stay console commands minting temp snapshot
  links for humans and companion agents — a 🕸️ row click triggers a live refetch + Graph tab,
  and the URL still opens the raw snapshot. Collaboration needs no link forwarding: propagated
  commands execute in every member's own session, so each member re-fetches its own replica.
  <!-- id: webapp-live-session-graph-source | created: 2026-09-10 | last_used: 2026-09-10 | uses: 1 | tier: working | supersedes: webapp-session-live-graph-restore | origin: 2026-09-10-033122 -->

- **JSON-Path is a payload-only playground (2026-09-09, Eric's direction — the tool has no graph
  surface).** Its `tabs` config is just `['payload']`; the `tabs` list in `PLAYGROUND_CONFIGS`
  is the single authority for right-panel composition, and RightPanel hides the tab strip
  entirely for single-tab playgrounds. Stale persisted tab selections are already normalized by
  `normalizeRightTab`, so removing tabs from a config is safe without migrations.
  <!-- id: webapp-jsonpath-payload-only | created: 2026-09-09 | last_used: 2026-09-09 | uses: 1 | tier: working | origin: 2026-09-09-223126 -->

- **The panel split opens at its defaults on every page load and is never persisted (2026-10-01,
  Eric's report; PR #493, squash `1619a4f3`, merged 2026-10-02; ships with the next engine release).**
  Console one third, right panel the complement, clipboard sidebar 20%, node editor and mock input
  30%. `useDefaultLayout` is gone: a saved layout beats `defaultSize` at mount while the per-mode
  resize fires only on slot-content changes, so a drag saved in an earlier session pinned the
  startup (60% on 2026-09-09, about 70% on 2026-10-01) until a console toggle reset it. Eric read
  it as a wrong default both times, and a storage-key bump fixes one browser only until its next
  drag. A drag now holds until the slot content changes or the page reloads. To reproduce such a
  report, plant `react-resizable-panels:/-panel-split-v2` = `{"_r_1_":71,"_r_3_":29}` in a fresh
  browser on the old bundle: the panel ids are React `useId` values, stable for the same tree.
  Pinned by the `Playground panel split` block in `PlaygroundHelp.test.tsx`. Replaces the
  persisted-split rule of [[webapp-mock-input-inplace-panel]].
  <!-- id: webapp-panel-split-not-persisted | created: 2026-10-01 | last_used: 2026-10-02 | uses: 1 | tier: working | origin: 2026-10-02-032130 -->

- **`describe skill {route}` for a built-in skill is a help command: its page opens in the help
  panel (2026-10-01, Eric's direction; same PR #493, squash `1619a4f3`).** Both engines answer it with the page
  `help {route}`, each `.` replaced by `-` and lowercased (Java `GraphCommandService.describeSkill`,
  Rust `handle_describe`), so `extractCommandHelpTopic` (`utils/helpTopic.ts`) maps the exact
  three-word form the same way, and `resolveBundledHelpTopic` serves both the send path
  (`handleLocalCommand`: a local echo, no round-trip) and the echo path (`useAutoHelpNavigate`, so
  a collaborator's or the companion's command opens the same page). Keep the mapping mirrored with
  the engines. `describe graph` (a Graph-tab link), `describe node` and `describe connection` stay
  console answers, and a skill without a bundled page still goes to the backend. Extends
  [[webapp-playground-help-profiles]].
  <!-- id: webapp-describe-skill-help-panel | created: 2026-10-01 | last_used: 2026-10-02 | uses: 1 | tier: working | origin: 2026-10-02-032130 -->

- **The vitest suite installs happy-dom's web storage itself, so `npm test` needs no `NODE_OPTIONS` on
  any Node version (2026-10-01, Eric's request; PR #494, squash `a40120d1`, merged
  2026-10-02).** Node 25 turned its own Web Storage API on by default: without
  `--localstorage-file` Node's `localStorage` reads as undefined (Node 26.10 also warns), and Vitest 4's
  happy-dom environment does not override a global Node already defines unless the name is on its key
  list (`Storage` is, `localStorage` and `sessionStorage` are not). On Node 26 that failed 23 tests in
  three files at `localStorage.clear()`, ran the `sessionStorage` tests on Node's own store, and let
  `GraphViewControls` pass only through `useLocalStorage`'s catch. `src/test/setupWebStorage.ts` (a
  `setupFiles` entry) defines both names as fresh happy-dom `Storage` instances when `happyDOM` is on the
  global, without reading Node's getter, so the warning is gone too;
  `src/test/__tests__/setupWebStorage.test.ts` pins the contract. No Node flag: Node 26 lists
  `--no-experimental-webstorage` as an alias of `--webstorage`, and a Node that predates the flag refuses
  to start with it. **Remove the setup file at the Vitest 5 upgrade** (vitest-dev/vitest#10293 lists both
  names; the 4.x report #10867 was closed as not planned) and keep the test. Verify storage changes on
  Node 26 without `NODE_OPTIONS`; `--no-experimental-webstorage` stands in for Node 22's globals. The
  setup file is outside the app graph, so it needs no bundle release (a `vite build` matched the
  committed bundle). The Rust twin is the root thread `webapp-tests-webstorage-rust-twin`.
  <!-- id: webapp-tests-node25-webstorage-setup | created: 2026-10-01 | last_used: 2026-10-02 | uses: 1 | tier: working | origin: 2026-10-02-041143 -->

- **The run controls work in every session: a subscriber instantiates, uploads its mock input and runs
  as an equal partner (2026-10-02, Eric's bug report and ruling; PR #495, squash `75ce191f`,
  merged 2026-10-02).** Eric's design: the
  primary and its subscribers, human operators or an AI companion, are equal partners for multi-party
  human-AI collaboration; anyone can enter any command except `session`, which is private to each user,
  and every member sees the others' actions. Both engines already run a subscriber's command through the
  primary and replay it in every member's session (Java `GraphCommandService.runAsPrimary`, Rust
  `commands.rs`), but `useGraphRunWorkflow` disabled Instantiate and Run unless `isPrimary` ("Run the
  graph from the host session"). The gate is gone. `isPrimary` now only resets the run when it changes
  (subscribe or unsubscribe): the old reset was level-triggered and would have reset a subscriber's run
  on every graph refetch, and a test pins that step. **Mock input is per session:** `upload mock data`
  replays too, so every member gets its own invitation (`POST /api/mock/{its session}`) and its own
  panel, and an upload loads only that member's instance; a replayed `run` aborts on a member that did
  not upload (tutorial 3: `Profile {id} not found`); Eric ruled that uploads stay per member, not shared
  (2026-10-02). Verified live with the session broker holding the
  primary. Replaces the host-session gating in [[webapp-graph-toolbar-run-controls]].
  <!-- id: webapp-run-controls-equal-partners | created: 2026-10-02 | last_used: 2026-10-02 | uses: 1 | tier: working | origin: 2026-10-02-050449 -->

## Open Threads

- [x] **Review the MiniGraph webapp architecture: CLOSED 2026-10-02 at Eric's direction, with no second review.** The
  2026-07-29 report (`memory/architecture-review-2026-07-29.md`) stands. Later work removed the stale graph-link path its
  generation guard aimed at (`webapp-live-session-graph-source`) and typed the run workflow's events
  (`webapp-graph-toolbar-run-controls`). Deliberately dropped: the transport split, a command intent layer (the text protocol
  still has no correlation id), one owner for the backlog dedupe, suspend/resume refresh semantics, and deploy validation for
  the checked-in bundle. Lesson: a review thread with no exit criterion outlives the work it asks about. origin: 2026-07-29-160756.
  <!-- id: thread-webapp-architecture-review | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: working | origin: 2026-07-29-160756 -->

- [x] **Manually inspect the Session menu before landing `feature/ui-merges`: CLOSED 2026-10-02 at Eric's direction.** The
  stack landed as PR #262 (`feature/ui-merges-v4`, merge `d1c3f1ff`, 2026-08-07) and no session recorded the check, so it is
  dropped rather than carried. Lesson: a thread gated on "before landing X" must close or be restated when X lands; this one
  outlived its gate by eight weeks. origin: 2026-07-29-160756.
  <!-- id: thread-webapp-session-menu-manual-check | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: working | origin: 2026-07-29-160756 -->

- [x] **Drift: the Session-menu check outlived its gate. RESOLVED 2026-10-02.** Raised by the webapp layer's first review;
  Eric closed `thread-webapp-session-menu-manual-check` (above). Lesson: an unchecked thread never decays, so the review's
  thread-body scan is what catches a gate that has passed. origin: 2026-10-02-041143.
  <!-- id: webapp-session-menu-check-drift | created: 2026-10-02 | last_used: 2026-10-02 | uses: 1 | tier: working | origin: 2026-10-02-041143 -->
