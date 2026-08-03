# MiniGraph Webapp Architecture Review (2026-07-29)

**Agent:** GitHub Copilot

## Context

This review was triggered after the `feature/ui-merges` stack integrated the multi-select,
sorting, and session-collaboration UI work, and after the MiniGraph engine gained graph
suspend/resume support. The immediate production regression was repeated raw `session` command
firing on load.

The tactical loop fix has already been applied: `useSessionCollaboration` now keeps the latest
sender/toast callbacks in refs and keeps `refreshSession` stable across sender identity churn.
Validation at the time of review: focused session hook tests 5/5, full webapp suite 164/164,
`npm run release` passed.

## Executive Summary

The webapp has grown into a capable tool surface, but its architecture now has one recurring risk:
imperative command transport and reactive WebSocket/message state are too tightly coupled. That
coupling made a render/effect identity change capable of sending backend commands. The same pattern
can affect `session`, `describe graph`, upload, saved-graph, and collaboration commands unless the
transport boundary is made explicit and stable.

The next architecture pass should focus on state ownership and command causality, not visual polish.

## Findings

### P0 Fixed: Session Auto-Refresh Command Loop

**Files:**

- `src/session/useSessionCollaboration.ts`
- `src/session/__tests__/useSessionCollaboration.test.ts`
- `src/hooks/useWebSocket.ts`
- `src/contexts/WebSocketContext.tsx`

**What happened:** `useSessionCollaboration` sent `session` on connected mount. The refresh function
depended on `sendRawText`, and production `sendRawText` depends on the WebSocket context object. That
context changes when slot/message state changes, so a backend session-status response could change the
sender identity, recreate the refresh callback, rerun the effect, and send `session` again.

**Status:** fixed tactically. The regression test now injects an unstable sender identity and verifies
the initial status request is not repeated after status processing.

**Strategic fix:** separate stable imperative transport methods from reactive slot/message state.
Command effects should be transition-driven or event-driven, never rerender-driven by message-list
identity churn.

### P1: WebSocket Transport Identity Leaks Into Business Logic

**Files:**

- `src/contexts/WebSocketContext.tsx`
- `src/hooks/useWebSocket.ts`
- `src/components/Playground.tsx`

`WebSocketContext` exposes both reactive state access (`getSlot`) and imperative operations (`send`,
`connect`, `disconnect`) through one context value. Because `getSlot` depends on `slots`, the context
value changes whenever slot state/messages change. Consumers that depend on the whole context or on
callbacks derived from it can accidentally treat message updates as transport changes.

**Why it matters:** command-sending hooks can become sensitive to unrelated console/message updates.
The session loop exposed this once; graph auto-refresh, saved graph workflow, upload, and authoring
actions are adjacent surfaces.

**Recommended fix:** split the provider or context shape into:

- stable imperative transport API: `send`, `connect`, `disconnect`, `appendMessage`, `clearMessages`
- reactive slot selector/state: phase/messages per `wsPath`

If keeping one provider, avoid callbacks depending on the full `ctx` object; destructure stable methods
and store live phase in refs where needed.

### P1: Command Causality Is Implicit Raw Text

**Files:**

- `src/hooks/useWebSocket.ts`
- `src/hooks/useAutoGraphRefresh.ts`
- `src/session/useSessionCollaboration.ts`
- `src/hooks/useSavedGraphWorkflow.ts`
- `src/components/Playground.tsx`

User-entered commands, UI button commands, auto-refresh commands, upload handshakes, session refreshes,
and collaboration commands all eventually become raw text sent over the same WebSocket pipe.

**Why it matters:** the frontend cannot reliably answer why a command was sent, whether it is safe to
retry, which response it expects, or whether a duplicate response should be ignored. This gets harder in
multi-session collaboration and with graph suspend/resume workflows.

**Recommended fix:** introduce a small command envelope or command dispatcher internal to the frontend:

```ts
interface CommandIntent {
  text: string;
  cause: 'user' | 'mount' | 'session-reset' | 'auto-refresh' | 'upload' | 'saved-graph' | 'authoring';
  idempotent: boolean;
  expected?: string;
}
```

This does not need backend protocol changes at first. It can be frontend-only metadata for logging,
dedupe, tests, and response correlation.

### P1: Protocol Classification And Backlog Dedupe Have Multiple Owners

**Files:**

- `src/protocol/useProtocolKernel.ts`
- `src/protocol/classifier.ts`
- `src/session/useSessionCollaboration.ts`

`useProtocolKernel` owns message classification and a new-message watermark for bus emission.
`useSessionCollaboration` also owns a backlog watermark and a processed-event key set so it can catch
already-classified console backlog events.

**Why it matters:** the same event may be observed through live bus delivery and backlog catch-up. The
current dedupe works for the narrow session path, but ownership is split, making future protocol events
easy to double-apply or miss.

**Recommended fix:** make protocol delivery a single abstraction. Ideally `useProtocolKernel` exposes a
stable event stream or processed-event iterator with one dedupe/watermark policy. Feature hooks should
subscribe to typed events rather than manage their own backlog scan rules.

### P1: Graph Refresh And Session Reset Need A Generation Boundary

**Files:**

- `src/hooks/useAutoGraphRefresh.ts`
- `src/hooks/usePinnedGraphPath.ts`
- `src/components/Playground.tsx`
- backend session behavior in `../src/main/java/com/accenture/minigraph/services/GraphCommandService.java`

Graph API paths are WebSocket-session-bound, and the webapp clears pinned graph state on disconnect/reset.
Auto-refresh sends `describe graph` after mutation events. With session collaboration and reset events,
old-session graph events can race with new-session state.

**Why it matters:** after reset or reconnect, a delayed graph-link/mutation response from the previous
session could repopulate graph state for the wrong backend session.

**Recommended fix:** add a frontend session generation key. Every pinned graph path, refresh request, and
graph-link response should be associated with the current session/generation; stale events should be
ignored.

### P2: Suspend/Resume Needs Frontend Refresh Semantics

**Files:**

- `src/hooks/useAutoGraphRefresh.ts`
- `src/utils/minigraphNodeTheme.ts`
- bundled help content generated from `../src/main/resources/help/`
- engine suspend/resume classes under `../src/main/java/com/accenture/minigraph/`

Graph suspend/resume is an engine feature, but it changes the user workflow: a graph run can complete as
`suspended`, later resume from persisted state, and skip previously completed checkpoints. The webapp has
visual/help support, but there is no frontend lifecycle model for suspended/resumed runs.

**Why it matters:** automatic `describe graph` refreshes are authoring-focused. They do not distinguish
graph model mutation from graph execution lifecycle. Suspend/resume may need clearer UI state, run history,
or guarded refresh behavior as the UX matures.

**Recommended fix:** keep this separate from the tactical transport fix. During architecture review,
decide whether frontend needs a minimal graph-run lifecycle event model or whether help/docs are enough
for the current playground scope.

### P2: Deploy Script Is A Plain Copy

**Files:**

- `package.json`
- `scripts/deploy.js`
- `vite.config.ts`

`npm run release` performs clean, build, deploy. The deploy script copies `dist/` into the Java resource
directory without validating manifest/index references or required assets.

**Why it matters:** the checked-in served bundle is part of the Java app. A stale or partial deploy can
produce confusing runtime failures, especially with hashed assets.

**Recommended fix:** add a lightweight deploy validation step:

- assert `dist/index.html` exists before copy
- assert every asset referenced by `index.html` exists after copy
- optionally fail if `dist/` is empty or contains stale old hashed bundles
- decide whether production source maps should be deployed or stripped

## Proposed Work Plan

1. **Transport Boundary Hardening**
   Split stable WebSocket imperative methods from reactive slot/message state, or refactor consumers to
   stop depending on the full context object. Add tests that simulate message churn without command resend.

2. **Command Intent Layer**
   Wrap frontend-originated commands in a small internal intent object. Use it first for logging/tests and
   response correlation; avoid backend protocol changes unless a real need emerges.

3. **Protocol Delivery Cleanup**
   Move backlog/live bus dedupe into a single protocol-kernel-owned path. Feature hooks should consume
   typed events from one source.

4. **Session Generation Guard**
   Tie graph paths and graph-link responses to the current WebSocket/session generation so stale reset or
   reconnect responses cannot repopulate state.

5. **Bundle Deploy Validation**
   Add a small validation script around `npm run release` for checked-in Java-served assets.

## Test Gaps To Add

- Sender/context identity churn does not resend mount commands.
- A session reset/reconnect ignores stale graph-link responses from the prior generation.
- Live bus event plus backlog catch-up applies a session event once.
- Auto `describe graph` is sent once per mutation burst and not sent after disconnect/reset.
- Deploy validation fails when `index.html` references a missing hashed asset.

## Notes

The architecture is not broken. The current shape has useful seams: `ProtocolBus`, `classifier`,
feature hooks, and `useWebSocket` already make the right responsibilities visible. The next step is to
make ownership stricter so command side effects cannot leak across those seams.