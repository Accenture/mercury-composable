# MiniGraph Webapp Continuity

## Project State

- **scope:** MiniGraph Playground React/Vite webapp
- **root:** `system/minigraph-playground-engine/webapp`
- **served bundle:** `system/minigraph-playground-engine/src/main/resources/public`
- **last_session:** 2026-09-08 | agent: Claude Code (2026-09-08-164102)

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
