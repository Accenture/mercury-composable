# MiniGraph Webapp Continuity

## Project State

- **scope:** MiniGraph Playground React/Vite webapp
- **root:** `system/minigraph-playground-engine/webapp`
- **served bundle:** `system/minigraph-playground-engine/src/main/resources/public`
- **last_session:** 2026-08-18 | agent: Codex (2026-08-18-133837)
- **last_review:** 2026-08-10 | through 2026-08-10-185950.md

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
  <!-- id: webapp-stable-transport-boundary | created: 2026-07-29 | last_used: 2026-08-17 | uses: 3 | tier: active | origin: 2026-07-29-160756 -->

- **Suspend/resume UI boundary.** Graph suspend/resume is an engine feature; the webapp owns visual
  conventions, help surfacing, and refresh/session behavior around it. Engine contracts remain in root
  memory; frontend follow-up belongs here unless it changes a backend contract.
  <!-- id: webapp-suspend-resume-ui-boundary | created: 2026-07-29 | last_used: 2026-07-29 | uses: 1 | tier: archive-candidate | origin: 2026-07-29-160756 -->

- **The GraphView minimap is toggleable, pannable, and collapsed by default (2026-08-07).** `GraphView`
  owns instance-local open/closed state above its keyed render-error boundary, so adding or deleting
  graph nodes does not close an open minimap; it renders only for a non-empty graph. The toggle is the
  fourth/bottom native `ControlButton` in React Flow's bottom-left zoom / fit-view stack (26px control /
  12px glyph), inherits the built-in foreground/background colours in both visibility states, and is
  accessible (`aria-label`, `aria-pressed`, and `aria-keyshortcuts`) and isolated from canvas pan/drag.
  Its click action and `Ctrl + M` toggle share the same controlled state; the
  hotkey is registered only for the active Graph tab, ignores editable/handled/repeated events and
  other modifiers, and matches physical `KeyM` for layout-stable handling. The Help overview documents
  the shortcut. Opening the minimap places it bottom-aligned 9px to the right of that left control
  stack, restores the legacy node colours, mask, and white background, enables library-native drag
  panning, and leaves minimap wheel zoom at its disabled default; no preference is persisted. The
  first usable active Graph canvas also shows a Help-style
  dark `Ctrl + M to toggle minimap` hint to the right of the control: it accumulates three seconds of
  eligible display time, fades for 400ms, and is instance-local/one-shot above keyed canvas remounts.
  Inactive tabs, maximized Help, and focus inside its semantic dismiss action pause the remaining time;
  dismissal returns focus to the minimap control, while opening the minimap removes the hint immediately
  and counts it as learned even if the hint was not yet eligible. Desktop toasts stay in their normal
  bottom-right lane; at 768px and below, their full-width layout uses a shared 11rem safe area above the
  bottom controls and 150px minimap so it cannot cover the toggle or hint.
  <!-- id: webapp-toggleable-minimap | created: 2026-08-07 | last_used: 2026-08-18 | uses: 9 | tier: active | origin: 2026-08-07-150537 -->

- **Playground Help uses a shared shell with isolated content profiles (2026-08-11).**
  `PlaygroundConfig.supportsHelp` enables the existing top-right button, first-load
  `Ctrl + backtick` hint, global shortcut, resizable/maximizable right-panel split, and persisted
  open/topic state; `helpContentProfile` selects the content and navigation without duplicating that
  UI. Minigraph keeps its full bundled topic library as the default profile. JSON-Path selects the
  `json-path` profile with its own topic-storage key and an intentionally single-section starter
  Overview. While connected, bare `help` resolves locally to that Overview, while unsupported
  MiniGraph-only topics such as `help create` continue to the backend.
  <!-- id: webapp-playground-help-profiles | created: 2026-08-11 | last_used: 2026-08-18 | uses: 2 | tier: active | origin: 2026-08-11-175533 -->

- **MiniGraph has an acknowledged Run / Instantiate split control (2026-08-14).** A MiniGraph-only
  split button sits immediately after Save Graph. Main Run serializes the existing `instantiate graph`
  then `run` commands; the dropdown can instantiate without running and exposes a backend-acknowledged
  Ready state. Graphs with derived `input.body` references reuse the session-specific mock-upload JSON
  modal before continuing; those references are conservative hints because the graph model exposes no
  authoritative required-input schema. Only the connected primary/host session can act. The transient
  frontend mirror advances from typed ProtocolBus acknowledgements, blocks duplicate/overlapping work,
  quarantines delayed uncorrelated responses in `outcome-uncertain`, and invalidates on graph mutation,
  export, identity/instance/session/connection changes. No backend command or persistence contract changed.
  <!-- id: webapp-acknowledged-graph-run-control | created: 2026-08-14 | last_used: 2026-08-14 | uses: 1 | tier: superseded | origin: 2026-08-14-140833 | superseded-by: webapp-graph-toolbar-run-controls -->

- **MiniGraph graph execution is an explicit acknowledged two-step toolbar flow (2026-08-17).**
  `Instantiate` and `Run` are separate compact actions in the Graph toolbar immediately before the
  raw-JSON Copy button and reuse that button's visual primitive; the old page-header split control is
  gone. `Run` is initially disabled and sends only `run` after the backend has acknowledged the graph
  instance and any derived `input.body` mock upload has succeeded. `Instantiate` never auto-runs and
  retains the existing JSON modal, conservative input-path hints, native file-picker cancellation,
  and re-input option for graphs with body references. The transient ProtocolBus mirror still limits
  actions to the connected primary/host session, handles manual console commands, prevents overlapping
  work, quarantines delayed uncorrelated responses, and invalidates across graph/export/session/
  connection changes. JSON-Path receives no controls; no backend command or persistence contract changed.
  <!-- id: webapp-graph-toolbar-run-controls | created: 2026-08-17 | last_used: 2026-08-18 | uses: 2 | tier: active | origin: 2026-08-17-181556 | supersedes: webapp-acknowledged-graph-run-control -->

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
