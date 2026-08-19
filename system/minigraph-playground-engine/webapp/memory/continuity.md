# MiniGraph Webapp Continuity

## Project State

- **scope:** MiniGraph Playground React/Vite webapp
- **root:** `system/minigraph-playground-engine/webapp`
- **served bundle:** `system/minigraph-playground-engine/src/main/resources/public`
- **last_session:** 2026-08-19 | agent: Codex (2026-08-19-163020)

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
