# Feature Spec: Browser-Side Protocol Kernel

**Target:** `webapp/` (React/TypeScript frontend)  
**Branch:** `feature/playground`  
**Date:** 2026-03-26  
**Status:** v12 — regression fix (appendMessage subscription stability)

### Revision history

| Version | Date | Changes |
|---|---|---|
| v1 | 2026-03-25 | Initial spec |
| v2 | 2026-03-25 | Peer-review applied. I1/A1: merged Phase 0+1 into single PR; renumbered Phases 1–8 → 0–7. I2: corrected §9.2 bullet 1 wording. I3: added `graph.link`, `upload.invitation`, `lifecycle` flag-clearing subscriptions to §12.5. I4: added first-match guard to §11.2. I5: added try/catch to `bus.ts` code example. I6: documented `useWebSocket` latest-message-only semantic in §13.2. G1: added `LeftPanel.tsx`, `Console.tsx` to Phase 7 changelist. G2: changed `useProtocolKernel` return type to include `classificationMap`. G3: added `@testing-library/react`, `happy-dom` to test deps. G4: added `upload.contentPath` to classifier rule 11 exclusion set. G5: documented lifecycle predicate. G6: acknowledged `useGraphSaveName` batch semantics change. G7: added CI note. A2: specified `useEffect` dep arrays. A3: confirmed `command.echo` is co-emitted with `command.helpOrDescribe`. A4: replaced `bus.clear()` with per-hook unsubscribe pattern. A5: made `bus` optional on `useWebSocket`. A6: resolved `ConsoleMessage` prop shape (full `ProtocolEvent[]`). A7: documented `busRef` ordering prerequisite. |
| v3 | 2026-03-25 | Second review. I1: added `knownType: boolean` to `LifecycleEvent` interface in §4 (was decided in §4.2 note but missing from the type definition). I2: documented `upload.contentPath` as a deliberate behavioral bugfix — current code incorrectly auto-pins upload prompts via `isPinnableResponse`; rule 11 exclusion now corrects this (§4.2). I3: added explicit first-match equivalence note to §10.2 — `isFetchingRef` replaces the current `break` guard in the bus model. G1: specified `msgId: number` and `classificationMap` as new required props on `ConsoleMessageProps` and `ConsoleProps` (§14.4). G2: added concrete `modalOpen: boolean` prop + clearing effect for `pendingModalRef` in `useAutoMockUpload` (§11.2). G3: resolved `lastNonJsonMessage` timing — keep direct `isMarkdownCandidate`/`isGraphLinkMessage` calls instead of reading from `classificationMap` to avoid one-render stale read (§14.5, §14.7). G4: added `useProtocolKernel.test.ts` to §16 new-files table. A1: expanded vitest include to `['src/**/*.test.ts', 'src/**/*.test.tsx']`. A2: added formal `Set` insertion-order invariant note to §5.3. A3: fixed stale §16→§15 cross-reference in §2 P2. |
| v4 | 2026-03-25 | Third review. I1: retained `isMarkdownCandidate` as a direct import in `ConsoleMessage` — replacing with `docs.response` check would break echo-message pinnability (§14.4, §14.6). I2: retained `parseMessage` + `getMessageIcon` as direct imports — `getMessageIcon(type: MessageType)` expects the narrow union, not the broadened `LifecycleEvent.type: string` (§14.4, §14.6). G1: added `payload.large` to classifier rule 11 exclusion set and `clearWaiting` subscriptions — same class of auto-pin bug as `upload.contentPath` (§4.2, §5.2, §12.5). G2: added `modalOpen` prop passing to §16 Phase 4 changelist row and `Playground.tsx` change description (§16). G3: added `globals: true` to `vitest.config.ts` for `@testing-library/react` automatic cleanup (§7.2). G4: clarified P6 to "No new **runtime** npm dependencies" — devDependencies are added in Phase 0 (§2). P1: documented `classificationMap` optional-to-required tightening after Phase 7 (§14.4). P2: fixed `handlePinMessage` dep array from `[classificationMap]` to `[]` with eslint-disable comment — `classificationMap` is a stable mutable Map ref (§14.5). P3: documented `unclassified` as an unreachable defensive fallback with rationale for exclusion from `clearWaiting` (§4.2). |
| v5 | 2026-03-25 | Fourth review. I1: removed `connected` from `UseProtocolKernelOptions` interface, hook destructuring, and call sites — the kernel is connection-agnostic (no watermark reset on disconnect) so the parameter was dead code; ESLint `no-unused-vars` would flag it (§5.4, §8.3, §8.4). G1: made `tsconfig.json` `"types": ["vitest/globals"]` prescriptive instead of conditional — always required since the project has no existing `types` field; added `tsconfig.json` to Phase 0 modified files (§7.2, §6, §16). G2: added consume-before-fetch ordering note to §13.2 — `pendingUploadRef.current = false` must be cleared synchronously before `fetch()` to match current code and make E17's second-event no-op reliable (§13.2). P1: moved `payload.large` rule-11 exclusion note from after `graph.link` row to immediately after `payload.large` row, matching the placement convention established by `upload.contentPath` (§4.2). |
| v6 | 2026-03-25 | Fifth review (18-item spec–vs–codebase audit). B1: documented `graph.link` + `graph.mutation` batch coexistence behavioral change — current code's Pass 1 `return` drops mutations, bus model processes both; acknowledged as intentional improvement (§9.4, §1.3). B2: documented `import-graph` + `node-mutation` batch coexistence change — same class of lossy-batch fix (§9.4, §1.3). B3: documented `isHelpOrDescribeCommand` dead code in `useAutoMarkdownPin` — lines 142–145 are unreachable; bus model's behavior is identical (§12.3). G4: added React rules-of-hooks compliance directive for `useWebSocket` `bus?` fallback — both effects called unconditionally, gated by `if (!bus)/if (bus)` body guards (§13.2). G5: added `LifecycleEvent.time` runtime normalization note — classifier must use `parsed.time ?? null` since `JSON.parse` returns `undefined` for missing fields (§4). G6: added `graph.mutation` intentional-preservation note to rule 11 — mutation messages produce `docs.response` (same as current code), explicitly not treated as a bugfix (§4.2). G7: added `_classificationMap` underscore prefix for unused destructuring during Phases 1–6 (§8.4). G8: specified `LeftPanelProps` interface change (`classificationMap?`) in §16 changelist. G9: added `@types/*` side-effect note — `"types"` must include `"react"` and `"react-dom"` alongside `"vitest/globals"` (§7.2, §16). A10: documented `isMarkdownCandidate` dead code — `typeof obj['type']` check has no behavioral impact; classifier uses `tryParseJSON` directly (§5.2). A11: documented bus subscription gap during effect re-runs on `connected` change (§9.4). A12: documented rule 11 redundant `isMarkdownCandidate` call — always returns `true` at that point (§5.2). A13: documented `detectMutation` redundant re-checks as intentional composition-layer design (§5.2). A14: documented `parseMessage` runtime type caveat — `MessageType` is compile-time only (§14.4). A15: documented timer-lifecycle implicit dependency between disconnect-cleanup and listener-lifecycle effects (§9.4). M16: fixed `addToast` parameter name from `msg` to `message` in all spec interfaces (§9.3, §10.1, §13.2). M18: added null-assertion note to `JsonResponseEvent.data` — `tryParseJSON` returns `object | null` but `isJSON: true` guarantees non-null (§4). |
| v7 | 2026-03-25 | Sixth review (6-item audit). I1: corrected line references in B1 (`line 183` → `line 169`) and B3 (`lines 142–145` → `lines 144–146`) to match actual source (§9.4, §12.3). I2: added `connectedRef` stale-closure fix to `useAutoGraphRefresh` sketch — `setTimeout` callback and listener body now read `connectedRef.current` instead of closure-captured `connected`; removed `connected` from `graph.mutation` effect dep array (`[bus, connected, sendRawText, addToast]` → `[bus, sendRawText, addToast]`); supersedes A11 (subscription gap) and A15 (timer-lifecycle) — both concerns eliminated by stable listener identity (§9.4). I3: wrapped all `bus.on(...)` subscriptions in §12.3 and §12.5 in `useEffect` with cleanup returns — matches the pattern established in §9.4 and §11.2; prevents listener leaks (§12.3, §12.5). I6: added explicit StrictMode double-mount test vector to §8.8 — verifies no duplicate events and single active listener after remount. I4, I5 confirmed as no-action (self-consistent analysis). |
| v8 | 2026-03-25 | Seventh review (3 cosmetic items). I1: corrected `isMarkdownCandidate` line reference from `line 51` to `line 50` in §14.4. I2: added Phase 7 row for `Playground.tsx` to §16 modified-files table — pass `classificationMap` to `<LeftPanel>`, migrate `handlePinMessage`, remove `extractGraphApiPath` import, rename `_classificationMap`. I3: reworded E6 (StrictMode double-mount) — watermark ref **persists** across cleanup→remount cycle (`useRef` values are not reset), clarifying that it is not re-initialized to default (§17). |
| v9 | 2026-03-25 | Eighth review (5-item audit). R1: redesigned `classificationMap` from effect-populated mutable `Map` (`useRef`) to render-synchronous `useMemo` over full `messages` array — fixes three defects: empty map on navigation-remount, ref mutation not triggering re-render, stale entries on clear-messages; migrated `lastNonJsonMessage` to read from map (G3 timing caveat eliminated); updated `handlePinMessage` dep array from `[]` to `[classificationMap, setPinnedGraphPath, setPinnedMessageId]`; removed `mapRef`, prune logic, eslint-disable comment; updated §5.4, §8.3, §8.8, §14.3, §14.5, §14.7, §16, §17 E1. R2: added `"resolveJsonModule": true` to Phase 0 `tsconfig.json` changes — required for JSON fixture imports in golden transcript tests (§7.2, §16). R3: rewrote E6 StrictMode rationale — framed around observable invariant (watermark initialized from current messages on each mount) rather than `useRef` persistence implementation detail; updated §8.8 test vector (§17). R4: added `sendRawTextRef` to `useAutoGraphRefresh` sketch — `sendRawText` identity changes on every `phase` transition, causing subscription churn; ref-wrapping stabilizes the `graph.mutation` listener to mount-once (`[bus, addToast]`); added `onAutoPinRef` (Option B) to `useAutoMarkdownPin` §12.3 — inline lambda from `Playground.tsx` caused per-render subscription churn; ref-wrapping stabilizes to `[bus, setPinnedMessageId]` (§9.4, §12.3). R5: normalized all §16 file paths to include `src/` prefix — disambiguates files across the directory tree (§16). |
| v10 | 2026-03-25 | Ninth review (2 documentation items). I1: documented `lastNonJsonMessage` behavioral narrowing — `docs.response` filter excludes echoes, mock-upload invitations, upload-content-path, and large-payload messages that the current code's `isMarkdownCandidate` check allows through; added to §1.3 exceptions list as item 4 and §14.5 with rationale (§1.3, §14.5). I3: added R4 rationale blockquote to §12.3 explaining `onAutoPinRef` motivation — inline ternary lambda from Playground.tsx creates per-render subscription churn (§12.3). I2 (double classification) confirmed as informational, no action. |
| v11 | 2026-03-25 | Tenth review (4-item audit). C1: wrapped §13.1 `useGraphSaveName` `bus.on()` call in `useEffect` with cleanup return — was the only migration section missing the standard subscription pattern (§13.1). C2: changed §8.3 Effect 2 to read from `classificationMap.get()` instead of re-calling `classifyMessage()` — eliminates double classification of new messages; added `classificationMap` to Effect 2 dep array (§8.3). C3: added large-payload `isPinnable` exclusion note to §14.4 — current code's inline comment says large-payload rows are non-pinnable but the guard does not enforce it; Phase 7 should fix by adding `&& !isLargePayload` (§14.4). C4: added frontend-injected lifecycle messages to §7.4 fixture source-of-truth and §7.6 population list — `WebSocketContext.tsx` injects `connected`, `disconnected`, and `already disconnected` lifecycle JSON that the classifier must handle (§7.4, §7.6). |
| v12 | 2026-03-26 | Regression fix (1 item). B1: discovered production regression — large payloads silently dropped after migration; root cause: `appendMessage` from `useWebSocket` closes over `ctx` (the full context value object, an inline object literal with a new reference on every Provider render), so its `useCallback` identity changes on every incoming WebSocket message; with `appendMessage` in the dep array (`[bus, appendMessage, addToast]`), the subscription tears down then re-registers on every message — the kernel emits `payload.large` after the old listener is removed but before the new one is registered; applied the R4 ref-wrapping pattern (`appendMessageRef`, `addToastRef`) reducing the dep array to `[bus]` (mount-once, no subscription gap); `addToast` is currently stable (`useCallback([])`) but is also ref-wrapped for defensive correctness; added implementation sketch and rationale blockquote to §10.2 (§10.2). |

---

## Table of Contents

1.  [Background & Motivation](#1-background--motivation)
2.  [Design Principles & Constraints](#2-design-principles--constraints)
3.  [Architecture Overview](#3-architecture-overview)
4.  [Event Type Catalogue](#4-event-type-catalogue)
5.  [Module Inventory](#5-module-inventory)
6.  [Phased Migration Plan](#6-phased-migration-plan)
7.  [Phase 0 — Golden Transcript Test Suite + Classifier + Event Bus](#7-phase-0--golden-transcript-test-suite--classifier--event-bus)
8.  [Phase 1 — Watermark Manager](#8-phase-1--watermark-manager)
9.  [Phase 2 — Migrate useAutoGraphRefresh](#9-phase-2--migrate-useautographrefresh)
10. [Phase 3 — Migrate useLargePayloadDownload](#10-phase-3--migrate-uselargePayloaddownload)
11. [Phase 4 — Migrate useAutoMockUpload](#11-phase-4--migrate-useautomockupload)
12. [Phase 5 — Migrate useAutoMarkdownPin](#12-phase-5--migrate-useautomarkdownpin)
13. [Phase 6 — Migrate useGraphSaveName & useWebSocket upload](#13-phase-6--migrate-usegraphsavename--usewebsocket-upload)
14. [Phase 7 — Migrate ConsoleMessage render-time classification](#14-phase-7--migrate-consolemessage-render-time-classification)
15. [Phase 8 — Web Worker Promotion Path (Deferred)](#15-phase-8--web-worker-promotion-path-deferred)
16. [File Changelist Summary](#16-file-changelist-summary)
17. [Edge Cases & Pitfall Index](#17-edge-cases--pitfall-index)
18. [Open Questions (Deferred)](#18-open-questions-deferred)

---

## 1. Background & Motivation

### 1.1 The problem

The webapp currently parses and classifies incoming WebSocket messages in **six
independent locations**, each with its own message-ID watermark, filter loop,
and regex matching:

| Consumer | What it scans for | Watermark? |
|---|---|---|
| `useAutoGraphRefresh` | `detectMutation`, `isGraphLinkMessage`, `extractGraphApiPath` | yes |
| `useLargePayloadDownload` | `extractLargePayloadLink` | yes |
| `useAutoMockUpload` | `isMockUploadMessage`, `extractMockUploadPath` | yes |
| `useAutoMarkdownPin` | `isHelpOrDescribeCommand`, `isMarkdownCandidate`, `isGraphLinkMessage`, `isMockUploadMessage` | yes |
| `useGraphSaveName` | `extractImportGraphName` | yes |
| `ConsoleMessage` (render) | `parseMessage`, `tryParseJSON`, `isGraphLinkMessage`, `isLargePayloadMessage`, `isMockUploadMessage`, `extractMockUploadPath`, `isMarkdownCandidate` | no (per-render) |

Additionally, `useWebSocket` watches for `extractUploadPath` in its own
`useEffect`.

This creates three concrete problems:

1. **Redundant work** — every new message batch is regex-matched N times by N
   independent effect runs.  The regexes are cheap individually, but the
   fan-out is structural waste that grows with each new automation hook.

2. **Scattered protocol knowledge** — understanding which messages are "claimed"
   requires reading every hook: e.g. `useAutoMarkdownPin.isPinnableResponse`
   explicitly excludes graph-link and mock-upload messages because those are
   consumed by other hooks.  The SPEC-auto-graph-refresh.md document has five
   revisions largely driven by cross-hook interaction bugs.

3. **Copy-paste watermark boilerplate** — five hooks independently implement the
   identical watermark pattern (init at mount, filter, advance, reset on
   disconnect), totalling ~80 lines of structural duplication.

### 1.2 The goal

> Introduce a single **Protocol Kernel** on the frontend that classifies each
> incoming WebSocket message **once**, emits typed events on a shared event bus,
> and provides a single watermark manager — so that downstream hooks become
> thin event subscribers that no longer parse raw text or manage watermarks.

### 1.3 What does NOT change

- **The backend protocol.** The WebSocket wire format, REST endpoints, and the
  choreography of commands / responses are completely untouched.
- **The `WebSocketContext` reducer.** Messages still flow into `slots[path].messages`
  via the existing `MESSAGE_RECEIVED` dispatch.  The kernel sits between the context
  and the hooks — it is a new subscriber of the same `messages` array.
- **User-visible behaviour.** No console message ordering, toast text, tab-switching
  logic, or modal behaviour changes.  The refactoring is purely internal.

  > **Exceptions (documented behavioral changes):** The bus model introduces
  > deliberate corrections where the current code has latent bugs:
  >
  > 1. `upload.contentPath` auto-pin bugfix — §4.2 (I2).
  > 2. `payload.large` auto-pin bugfix — §4.2 (G1).
  > 3. `useAutoGraphRefresh` batch-coexistence fixes — §9.4 (B1, B2):
  >    the current two-pass `return` structure silently drops mutations in
  >    certain batch compositions; the bus model processes all events.
  > 4. `lastNonJsonMessage` narrowing — §14.5 (I1): the `docs.response`
  >    filter excludes echoes, mock-upload invitations, upload-content-path
  >    messages, and large-payload messages from the Markdown Preview
  >    fallback.  The current code's `isMarkdownCandidate` check allows
  >    all of these through — none are meaningful preview content.

---

## 2. Design Principles & Constraints

| # | Principle | Rationale |
|---|---|---|
| P1 | **Classify once, emit to many** | Eliminates redundant regex work and centralises protocol knowledge. |
| P2 | **Plain module first, Worker later** | Message volume is bounded (`MAX_ITEMS = 200`); structured-clone overhead would negate gains.  The classifier is authored as a pure function in a plain `.ts` module.  §15 documents the Worker promotion path. |
| P3 | **Custom typed emitter, not React context** | Framework-agnostic; unit-testable without rendering; avoids re-render cascading. |
| P4 | **Incremental migration** | Each hook is migrated in a separate, independently testable phase.  At each phase boundary the app is fully functional. |
| P5 | **Golden transcript test suite first** | The test suite is built _before_ any refactoring as a regression safety net for the classifier and the hooks it replaces. |
| P6 | **No new runtime npm dependencies** | The event bus is ~40 lines of TypeScript; no external library needed.  Only `vitest` is added as a test devDependency in Phase 0. The classifier and bus are pure logic with no DOM or React rendering dependency; Vitest's built-in `node` environment is sufficient — no `happy-dom` or `@testing-library/react` needed. |

---

## 3. Architecture Overview

### 3.1 Current data flow

```
WebSocket.onmessage
  → WebSocketContext reducer (MESSAGE_RECEIVED)
    → messages[] array (React state, per-slot)
      ┌──→ useAutoGraphRefresh   (watermark, detectMutation, isGraphLinkMessage)
      ├──→ useLargePayloadDownload (watermark, extractLargePayloadLink)
      ├──→ useAutoMockUpload     (watermark, isMockUploadMessage)
      ├──→ useAutoMarkdownPin    (watermark, isHelpOrDescribeCommand, isMarkdownCandidate, ...)
      ├──→ useGraphSaveName      (watermark, extractImportGraphName)
      ├──→ useWebSocket          (extractUploadPath — upload handshake)
      └──→ ConsoleMessage        (render-time: parseMessage, tryParseJSON, is*Message, ...)
```

### 3.2 Target data flow (after all phases)

```
WebSocket.onmessage
  → WebSocketContext reducer (MESSAGE_RECEIVED)
    → messages[] array (React state, per-slot)
      → useProtocolKernel (single hook, per playground)
          │
          │  1. Single watermark: inits, filters, advances (once)
          │  2. Classifies each new message (once) via classifyMessage()
          │  3. Emits typed events on the ProtocolBus
          │
          ├──→ useAutoGraphRefresh   (subscribes to bus: graph.mutation, graph.link)
          ├──→ useLargePayloadDownload (subscribes to bus: payload.large)
          ├──→ useAutoMockUpload     (subscribes to bus: upload.invitation)
          ├──→ useAutoMarkdownPin    (subscribes to bus: command.helpOrDescribe, docs.response)
          ├──→ useGraphSaveName      (subscribes to bus: command.importGraph)
          └──→ ConsoleMessage        (reads classificationMap for 4 flags; retains parseMessage/getMessageIcon/isMarkdownCandidate/tryParseJSON)

  useWebSocket upload handshake → subscribes to bus: upload.contentPath
```

### 3.3 Module dependency diagram

```
src/
  protocol/
    events.ts          ← Event type definitions (discriminated union)
    classifier.ts      ← classifyMessage() pure function
    bus.ts             ← ProtocolBus typed event emitter
    useProtocolKernel.ts ← React hook: watermark + classify + emit
  hooks/
    useAutoGraphRefresh.ts    ← refactored: subscribes to bus
    useLargePayloadDownload.ts ← refactored: subscribes to bus
    useAutoMockUpload.ts       ← refactored: subscribes to bus
    useAutoMarkdownPin.ts      ← refactored: subscribes to bus
    useGraphSaveName.ts        ← refactored: subscribes to bus
    useWebSocket.ts            ← refactored: subscribes to bus (upload path)
  components/
    Console/ConsoleMessage.tsx ← reads classificationMap + retained parser calls (§14.6)
  utils/
    messageParser.ts           ← retained as the library of pure parse functions
                                  (consumed by classifier.ts; also by ConsoleMessage
                                  and Playground.tsx for retained direct calls — §14.6, §14.7)
```

> **`messageParser.ts` is NOT deleted.** It continues to export the pure
> extraction and predicate functions (`extractGraphApiPath`,
> `extractLargePayloadLink`, `detectMutation`, etc.).  The classifier imports
> and composes them.  Any function that is _only_ consumed by the classifier
> may eventually be unexported, but that is a follow-up hygiene task — not part
> of this spec.

---

## 4. Event Type Catalogue

All events extend a common base and form a discriminated union on `kind`.

```typescript
/** Base fields present on every protocol event. */
interface ProtocolEventBase {
  /** Monotonic message ID from the WebSocket context. */
  msgId: number;
  /** The original raw string as received from the WebSocket. */
  raw: string;
}

// ── Discriminated union ─────────────────────────────────────────────

interface GraphLinkEvent extends ProtocolEventBase {
  kind: 'graph.link';
  /** Extracted API path, e.g. "/api/graph/model/my-graph/1" */
  apiPath: string;
}

interface GraphMutationEvent extends ProtocolEventBase {
  kind: 'graph.mutation';
  mutationType: 'node-mutation' | 'import-graph';
}

interface LargePayloadEvent extends ProtocolEventBase {
  kind: 'payload.large';
  apiPath: string;
  byteSize: number;
  filename: string;
}

interface UploadInvitationEvent extends ProtocolEventBase {
  kind: 'upload.invitation';
  /** The POST path, e.g. "/api/mock/ws-417669-24" */
  uploadPath: string;
}

interface UploadContentPathEvent extends ProtocolEventBase {
  kind: 'upload.contentPath';
  /** The POST path, e.g. "/api/json/content/ws-123-4" */
  uploadPath: string;
}

interface CommandEchoEvent extends ProtocolEventBase {
  kind: 'command.echo';
  /** The command text without the "> " prefix. */
  commandText: string;
}

interface HelpOrDescribeCommandEvent extends ProtocolEventBase {
  kind: 'command.helpOrDescribe';
  /** The command text without the "> " prefix. */
  commandText: string;
}

interface ImportGraphCommandEvent extends ProtocolEventBase {
  kind: 'command.importGraph';
  /** The graph name from "import graph from {name}". */
  graphName: string;
}

interface DocsResponseEvent extends ProtocolEventBase {
  kind: 'docs.response';
  /** True when the raw message is a markdown candidate (non-JSON, non-echo). */
  isMarkdown: true;
}

interface JsonResponseEvent extends ProtocolEventBase {
  kind: 'json.response';
  /**
   * The parsed JSON object/array.  The classifier constructs this via
   * `data: jsonCheck.data!` — the non-null assertion is safe because
   * `isJSON: true` guarantees `data !== null`, but TypeScript cannot infer
   * this from `tryParseJSON`'s return type (`{ isJSON: boolean; data: object | null }`).
   */
  data: object;
}

interface LifecycleEvent extends ProtocolEventBase {
  kind: 'lifecycle';
  /** The type string from the JSON payload.  Known values: 'info', 'error', 'ping', 'welcome'. */
  type: string;
  /** True when `type` is one of the four known lifecycle values. */
  knownType: boolean;
  message: string;
  /**
   * Timestamp from the JSON payload, or null if absent.
   * The classifier must normalize: `time: parsed.time ?? null` — because
   * `JSON.parse` returns `any`, a missing `time` field yields `undefined`
   * at runtime, not `null`.  Without normalization, downstream `=== null`
   * checks would silently fail.
   */
  time: string | null;
}

/** Catch-all for messages that match no other classifier rule. */
interface UnclassifiedEvent extends ProtocolEventBase {
  kind: 'unclassified';
}

export type ProtocolEvent =
  | GraphLinkEvent
  | GraphMutationEvent
  | LargePayloadEvent
  | UploadInvitationEvent
  | UploadContentPathEvent
  | CommandEchoEvent
  | HelpOrDescribeCommandEvent
  | ImportGraphCommandEvent
  | DocsResponseEvent
  | JsonResponseEvent
  | LifecycleEvent
  | UnclassifiedEvent;
```

### 4.1 Why some messages produce multiple events

A message may satisfy more than one classifier rule.  For example:

- `> import graph from my-graph` is both a `command.echo` and a
  `command.importGraph`.
- `> help` is both a `command.echo` and a `command.helpOrDescribe`.
- `> describe skill my-skill` is both a `command.echo` and a
  `command.helpOrDescribe`.

The classifier returns an **array** of events for each message (typically 1,
occasionally 2).  Subscribers filter by `kind` so this is transparent.

### 4.2 Classification priority and exhaustiveness

The classifier runs rules top-to-bottom with **accumulation** (not early-exit),
because overlapping events serve different subscribers:

| Rule | Events produced | Subscribers |
|---|---|---|
| JSON lifecycle? | `lifecycle` | ConsoleMessage (icon rendering) |
| JSON object/array? (non-lifecycle) | `json.response` | ConsoleMessage (JsonView) |

> **Lifecycle predicate (G5):** A JSON object is classified as `lifecycle` when
> `tryParseJSON` returns `isJSON: true` AND the parsed object has a `type` field
> whose value is a `string`.  This matches the existing `isMarkdownCandidate`
> guard: `typeof obj['type'] === 'string'`.  It is deliberately broad — any
> `{"type": "..."}` JSON is treated as lifecycle, not just the four known values
> (`info`, `error`, `ping`, `welcome`).  This mirrors the current code, where
> `isMarkdownCandidate` returns `false` for **any** JSON object with a string
> `type` field regardless of value.  The `LifecycleEvent.type` field is typed as
> `'info' | 'error' | 'ping' | 'welcome'` for known values, but the classifier
> must accept and pass through unknown type strings (cast to the union with a
> fallback, or broaden the type to `string`).  **Decision: broaden
> `LifecycleEvent.type` to `string` and add a `knownType` boolean field so
> consumers can distinguish known from unknown lifecycle types without a cast.**
> The updated interface definition in §4 reflects this decision.

| Large payload link? | `payload.large` | useLargePayloadDownload |

> **Rule 11 exclusion — `payload.large` (G1):** The server's large-payload
> message `"Large payload (254922) -> GET /api/inspect/ws-734563-3/input.body"`
> is plain text that passes `isMarkdownCandidate`.  Without an explicit
> exclusion it would produce both `payload.large` AND `docs.response`, which
> could cause `useAutoMarkdownPin` to auto-pin a download link if
> `waitingForResponseRef` is armed.  This is the same class of bug as the
> `upload.contentPath` exclusion below.  In the current code,
> `isPinnableResponse` does **not** exclude large-payload messages — the
> function only guards against echoes, graph-links, and mock-uploads — so this
> is an additional behavioral bugfix on the same pattern as `upload.contentPath`
> (I2).  Rule 11 therefore excludes messages that matched `payload.large`
> (rule 3) from `docs.response`.
>
> Note that `payload.large` IS added to the `clearWaiting` subscriptions in
> §12.5 (unlike `upload.contentPath`), because a large-payload notification is
> a server response that should clear the armed flag — the user should not have
> a help/describe response auto-pinned after a large-payload download prompt.

| Mock upload invitation? | `upload.invitation` | useAutoMockUpload, ConsoleMessage (badge) |
| Upload content path? | `upload.contentPath` | useWebSocket (POST handshake) |

> **Rule 11 exclusion — `upload.contentPath` (G4):** The server’s upload-ready
> message `"Please upload XML/JSON text to /api/json/content/{id}"` is plain
> text that passes `isMarkdownCandidate`.  Without an explicit exclusion it
> would produce both `upload.contentPath` AND `docs.response`, which could
> cause `useAutoMarkdownPin` to auto-pin an upload prompt if
> `waitingForResponseRef` is armed.  Rule 11 therefore excludes messages that
> matched `upload.contentPath` (rule 5) from `docs.response`.
>
> **Deliberate behavioral bugfix (I2):** In the current code,
> `isPinnableResponse("Please upload XML/JSON text to /api/json/content/ws-123-4")`
> returns **`true`** — the message is not an echo (`> `), `isGraphLinkMessage`
> returns false (no `/api/graph/model/` path), and `isMockUploadMessage`
> returns false (the regex matches `/api/mock/`, not `/api/json/content/`).
> So if `waitingForResponseRef` is armed (e.g. user sent `help` then quickly
> ran `upload`), the current code would **auto-pin the upload prompt** to the
> Markdown Preview — clearly unintended behavior.  The bus model fixes this
> via rule 11 (no `docs.response` emitted for `upload.contentPath` messages).
> This is an intentional correction, not an accidental side effect.
>
> Note that `upload.contentPath` is **intentionally not** added to the
> `clearWaiting` subscriptions in §12.5 — the flag should stay armed, waiting
> for the actual help/describe response that follows.

| Graph link? | `graph.link` | useAutoGraphRefresh, ConsoleMessage (pin style) |
| Graph mutation? | `graph.mutation` | useAutoGraphRefresh |

> **`graph.mutation` intentionally NOT in rule 11 exclusion set (G6):**
> Mutation messages like `"Node foo created"` are plain text that pass
> `isMarkdownCandidate` and would produce BOTH `graph.mutation` AND
> `docs.response`.  If `waitingForResponseRef` is armed, this could auto-pin
> a mutation message to the Markdown Preview.  This is the **same behavior as
> the current code** — `isPinnableResponse("Node foo created")` returns `true`
> (it's not an echo, not a graph-link, not a mock-upload, and passes
> `isMarkdownCandidate`).  Unlike `payload.large` and `upload.contentPath`,
> this is NOT treated as a bugfix because:
> (a) mutation messages ARE meaningful plain-text responses that could
>     legitimately appear in the Markdown Preview, and
> (b) the race condition (user sends `help`, then a mutation arrives before
>     the help response) is unlikely in practice — mutations are responses to
>     user commands, and the user would not normally send `help` and
>     `create node` in the same batch.
> If this changes, `graph.mutation` can be added to rule 11 in a later version.

| Command echo (starts "> ")? | `command.echo` | ConsoleMessage (echo styling) |
| Help/describe command? | `command.helpOrDescribe` | useAutoMarkdownPin |
| Import graph command? | `command.importGraph` | useGraphSaveName |
| Markdown candidate? (non-JSON, non-echo, non-graph-link, non-mock-upload, non-upload-content-path, non-payload-large) | `docs.response` | useAutoMarkdownPin |
| None of the above | `unclassified` | — |

> **`unclassified` reachability (P3):** In the current protocol, rule 12 is
> believed to be **unreachable**: every non-JSON message passes
> `isMarkdownCandidate` (even if it also matches an earlier rule), and JSON
> messages always match either `lifecycle` (rule 1) or `json.response`
> (rule 2).  The `unclassified` event exists as a **defensive fallback** — if
> the backend ever introduces a message shape that bypasses all prior rules,
> it ensures an event is still emitted rather than silently dropping the
> message.  No subscriber listens for `unclassified`, and it is intentionally
> excluded from the `clearWaiting` subscriptions in §12.5 to avoid masking
> missing rules.  The golden transcript test suite should include a
> `negative-cases.json` vector that asserts `unclassified` is NOT produced for
> any known message format.

---

## 5. Module Inventory

### 5.1 `src/protocol/events.ts`

Type definitions only — the discriminated union from §4.  No runtime code.

**Exports:** `ProtocolEvent`, `ProtocolEventKind` (the `kind` string literal
union), and individual event interfaces.

### 5.2 `src/protocol/classifier.ts`

A single **pure function** with no React imports, no side effects, and no
state:

```typescript
import type { ProtocolEvent } from './events';

/**
 * Classifies a single raw WebSocket message string into one or more typed
 * protocol events.
 *
 * This function is the single source of truth for message classification in
 * the entire webapp.  All regex/JSON-parse heuristics are centralised here.
 *
 * Pure — no side effects, no state, no DOM access.  Safe to call from a
 * Web Worker in a future phase.
 */
export function classifyMessage(msgId: number, raw: string): ProtocolEvent[];
```

Implementation delegates to the existing pure functions in `messageParser.ts`:

```
classifyMessage(id, raw)
  1. tryParseJSON → if lifecycle event → push LifecycleEvent
  2. tryParseJSON → if object/array (non-lifecycle) → push JsonResponseEvent
  3. if text: extractLargePayloadLink → push LargePayloadEvent
  4. if text: extractMockUploadPath → push UploadInvitationEvent
  5. if text: extractUploadPath → push UploadContentPathEvent
  6. if text: isGraphLinkMessage + extractGraphApiPath → push GraphLinkEvent
  7. if text: detectMutation → push GraphMutationEvent
  8. if starts with "> ": push CommandEchoEvent
  9. if isHelpOrDescribeCommand → push HelpOrDescribeCommandEvent
  10. if extractImportGraphName → push ImportGraphCommandEvent
  11. if isMarkdownCandidate and none of {echo, graph-link, mock-upload, upload-content-path, payload-large} → push DocsResponseEvent
  12. if nothing pushed → push UnclassifiedEvent
```

Each step is a thin call to an existing `messageParser.ts` function — the
classifier is a **composition layer**, not a rewrite of parsing logic.

> **Redundancy notes for implementers:**
>
> **`isMarkdownCandidate` in rule 11 is always `true` (A12):** By the time
> rule 11 runs, rules 1–2 have already handled all JSON messages (all paths
> after `isJSON` return `false`).  The `isMarkdownCandidate(raw)` check is
> guaranteed to return `true` at rule 11 — the call is technically redundant.
> Keep it for safety (it's a cheap guard) but add a code comment explaining
> why it cannot return `false` at this point.
>
> **`isMarkdownCandidate` dead code (A10):** In `messageParser.ts`, both
> branches after `tryParseJSON(...).isJSON === true` return `false` — the
> `typeof obj['type'] === 'string'` check has no behavioral impact.  The §4.2
> lifecycle predicate note references this check, but it only matters for the
> classifier's direct `tryParseJSON` call in rule 1, not for
> `isMarkdownCandidate`.  The classifier does NOT rely on `isMarkdownCandidate`
> for lifecycle detection — it uses `tryParseJSON` directly.
>
> **`detectMutation` re-checks (A13):** `detectMutation()` internally calls
> `isMarkdownCandidate(raw)`, `isGraphLinkMessage(raw)`, and checks
> `raw.startsWith('> ')` — all conditions already classified by earlier rules.
> Since the classifier uses accumulation (not early-exit), `detectMutation` is
> called even for JSON/graph-link/echo messages and returns `null` for all of
> them, doing redundant work.  This is by design: the classifier delegates to
> existing functions without rewriting their internal guards.  Do NOT optimize
> these away — removing the internal guards would couple `detectMutation` to
> the classifier's call order, making it fragile to future reordering.

### 5.3 `src/protocol/bus.ts`

A lightweight typed event emitter (~40 lines):

```typescript
import type { ProtocolEvent, ProtocolEventKind } from './events';

type Listener<K extends ProtocolEventKind> = (
  event: Extract<ProtocolEvent, { kind: K }>
) => void;

export class ProtocolBus {
  private listeners = new Map<string, Set<Function>>();

  on<K extends ProtocolEventKind>(kind: K, listener: Listener<K>): () => void {
    const key = kind as string;
    if (!this.listeners.has(key)) this.listeners.set(key, new Set());
    this.listeners.get(key)!.add(listener);
    // Return an unsubscribe function (used in useEffect cleanup).
    return () => { this.listeners.get(key)?.delete(listener); };
  }

  emit(event: ProtocolEvent): void {
    const set = this.listeners.get(event.kind);
    if (set) {
      set.forEach(fn => {
        try { fn(event); }
        catch (err) { console.error(`[ProtocolBus] listener for '${event.kind}' threw:`, err); }
      });
    }
  }

  /** Remove all listeners.  Called on unmount / hot-reload. */
  clear(): void {
    this.listeners.clear();
  }
}
```

Key properties:
- **Synchronous dispatch** — `emit` calls listeners in registration order.  No
  microtask deferral.  This preserves the current semantics where hook effects
  run synchronously within a single React commit.

  > **Ordering invariant (A2):** "Registration order" is a formal guarantee,
  > not an implementation accident.  The `Set<Function>` backing store
  > iterates in insertion order per the ECMAScript spec (§24.2.5.4).  The
  > `bus.ts` JSDoc must document this as an invariant so that a future
  > refactor to a different collection type preserves it.

- **Unsubscribe via returned function** — idiomatic for `useEffect` cleanup.
- **No wildcard / `*` listener** — keeps the API minimal.  Can be added later
  if needed.

### 5.4 `src/protocol/useProtocolKernel.ts`

The single React hook that replaces all five watermark implementations:

```typescript
import { useEffect, useMemo, useRef } from 'react';
import { ProtocolBus } from './bus';
import { classifyMessage } from './classifier';

export interface UseProtocolKernelOptions {
  /** The messages array for this playground's WebSocket slot. */
  messages:  { id: number; raw: string }[];
  /** The shared ProtocolBus instance for this playground. */
  bus:       ProtocolBus;
}

/**
 * Drives the protocol kernel lifecycle for one playground slot:
 *
 *  1. Maintains a single message-ID watermark (init at mount, advance on
 *     each batch, reset considerations on disconnect).
 *  2. Classifies each new message via classifyMessage() (once).
 *  3. Emits the resulting typed events on the shared ProtocolBus.
 *
 * Return value (Phase 7):
 *   classificationMap: Map<number, ProtocolEvent[]>
 *   — keyed by message ID, populated during classification.
 *   Used by Playground.tsx → LeftPanel → Console → ConsoleMessage to read
 *   pre-classified events without re-parsing.
 */
export function useProtocolKernel({
  messages,
  bus,
}: UseProtocolKernelOptions): UseProtocolKernelReturn;

export interface UseProtocolKernelReturn {
  /**
   * Lookup map from message ID to the array of ProtocolEvents produced by
   * classifyMessage().  Computed synchronously from the full `messages`
   * array via `useMemo` — a new Map instance is produced whenever
   * `messages` changes.  Covers all visible messages regardless of
   * watermark position.
   */
  classificationMap: Map<number, ProtocolEvent[]>;
}
```

Watermark mechanics are identical to the existing pattern (see §8 for details).

### 5.5 Bus instance ownership

Each `Playground` component creates a `ProtocolBus` instance in a `useRef`
(stable for the component's lifetime) and passes it down via props:

```typescript
// Playground.tsx
const busRef = useRef(new ProtocolBus());
const bus = busRef.current;

const { classificationMap } = useProtocolKernel({
  messages: ws.messages, bus,
});

// Pass bus to each hook that needs it:
useAutoGraphRefresh({ bus, ... });
```

> **Why not React Context for the bus?**
> The bus is per-playground — there are multiple playgrounds mounted behind
> `<Routes>`.  A context would require a `<ProtocolBusProvider>` wrapper per
> playground, which is more ceremony than a prop.  A ref-held instance passed
> as a prop is simpler and explicit.
>
> **Ordering prerequisite (A7):** `busRef` must be declared before both
> `useProtocolKernel` and `useWebSocket` in `Playground.tsx`.  Since `useRef`
> returns a stable object immediately (not state), this is safe regardless of
> call order — but for readability, declare it near the top of the component
> body, before any hook that references `bus`.

---

## 6. Phased Migration Plan

Each phase is a standalone PR that leaves the app fully functional.

> **v2 note (I1/A1):** The original spec had Phases 0 (test suite) and 1
> (classifier + bus) as separate PRs.  This was impossible: the test file
> imports `classifyMessage` which only exists after the classifier is written.
> They are now merged into a single Phase 0.  All subsequent phases are
> renumbered down by one (old Phase 2 → new Phase 1, etc.).

| Phase | Deliverable | Files created | Files modified | Risk |
|---|---|---|---|---|
| **0** | Golden transcript test suite + Vitest setup + `events.ts`, `classifier.ts`, `bus.ts` | `vitest.config.ts`, `src/protocol/__tests__/classifier.test.ts`, `src/protocol/__tests__/bus.test.ts`, `src/protocol/__tests__/fixtures/`, `src/protocol/events.ts`, `src/protocol/classifier.ts`, `src/protocol/bus.ts`, `src/protocol/index.ts` | `package.json`, `tsconfig.json` | None (additive) |
| **1** | Watermark manager (`useProtocolKernel`) wired into Playground | `src/protocol/useProtocolKernel.ts` | `Playground.tsx` | None (dual-path) |
| **2** | Migrate `useAutoGraphRefresh` | — | `useAutoGraphRefresh.ts`, `Playground.tsx` | Medium — most complex hook |
| **3** | Migrate `useLargePayloadDownload` | — | `useLargePayloadDownload.ts`, `Playground.tsx` | Low |
| **4** | Migrate `useAutoMockUpload` | — | `useAutoMockUpload.ts`, `Playground.tsx` | Low |
| **5** | Migrate `useAutoMarkdownPin` | — | `useAutoMarkdownPin.ts`, `Playground.tsx` | Low–medium (cross-event logic) |
| **6** | Migrate `useGraphSaveName` + `useWebSocket` upload | — | `useGraphSaveName.ts`, `useWebSocket.ts`, `Playground.tsx` | Low |
| **7** | Migrate `ConsoleMessage` render-time classification | — | `ConsoleMessage.tsx`, `LeftPanel.tsx`, `Console.tsx`, `Playground.tsx` | Low |
| **8** | (Deferred) Web Worker promotion | `src/protocol/worker.ts` | `useProtocolKernel.ts` | Deferred |

### 6.1 Phase invariant

At the end of every phase:
- `npm run build` succeeds with zero TypeScript errors.
- All existing manual test scenarios pass unchanged.
- The golden transcript test suite passes.

### 6.2 Dual-path coexistence during migration

During Phases 2–7, some hooks will have been migrated to subscribe to the bus
while others still scan `messages[]` directly.  This is safe because:

- `useProtocolKernel` always runs and emits events — migrated hooks subscribe.
- Non-migrated hooks still have their own watermark and scan logic — they are
  unaffected by the kernel's existence.
- There is overlapping work (both the kernel and a non-migrated hook classify
  the same message), but no behavioral conflict — the side effects are
  idempotent from the user's perspective.

The overlap is temporary and eliminated as each hook is migrated.

---

## 7. Phase 0 — Golden Transcript Test Suite + Classifier + Event Bus

### 7.1 Goal

Build a regression safety net and the core protocol kernel modules in a single
PR.  At the end of this phase `events.ts`, `classifier.ts`, and `bus.ts` are
**importable and fully tested** but **not yet wired** into any React component
or hook — zero behavioral change.

> **v2 note (I1/A1):** This was originally two phases.  The test file imports
> `classifyMessage`, which means the classifier must exist in the same PR.

### 7.2 Test framework setup

Add `vitest` and hook-testing dependencies as dev dependencies:

```jsonc
// package.json devDependencies additions:
"vitest": "^3.1.1"
```

> **G3 note:** Neither `happy-dom` nor `@testing-library/react` is required.
> The classifier and bus are pure TypeScript logic with no DOM or React rendering
> dependency; Vitest's built-in `node` environment is sufficient.

```jsonc
// package.json scripts addition:
"test": "vitest run",
"test:watch": "vitest"
```

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
    environment: 'node',
    globals: true,
  },
});
```

> **`globals: true` (G3):** Enables `describe`, `it`, `expect`, `afterEach`,
> etc. as global imports — required for `@testing-library/react`'s automatic
> `cleanup` registration (`afterEach(cleanup)` runs implicitly when globals are
> enabled).  Without this, `renderHook` tests (Phase 1) could leak DOM state
> between test cases.
>
> **`tsconfig.json` change (required):** Add `"types": ["vitest/globals"]` to
> `compilerOptions.types`.  `globals: true` in the Vitest config makes the
> globals available at runtime, but TypeScript type-checking only picks them up
> via this entry.  Without it, test files using `describe`, `it`, `expect` etc.
> will fail to compile with `Cannot find name 'describe'`.  The current
> `tsconfig.json` has no `"types"` field, so this must be added explicitly.
>
> **`@types/*` side effect (G9):** When `tsconfig.json` has no `types` field,
> TypeScript auto-includes all `@types/*` packages (currently `@types/react`
> and `@types/react-dom`).  When `types` is explicitly set, **only** the
> listed packages are included.  Therefore the entry must be:
> ```json
> "types": ["vitest/globals", "react", "react-dom"]
> ```
> Omitting `"react"` and `"react-dom"` would break type resolution for React
> and cause immediate compile failures across the entire `src/` tree.
>
> **`resolveJsonModule` (R2):** The golden transcript test suite (§7.5)
> imports JSON fixture files directly (`import graphMutations from
> './fixtures/graph-mutations.json'`).  Add `"resolveJsonModule": true` to
> `compilerOptions`.  The full prescribed change becomes:
> ```json
> "types": ["vitest/globals", "react", "react-dom"],
> "resolveJsonModule": true
> ```

### 7.3 Golden transcript fixture format

Each fixture is a `.json` file containing an array of test vectors:

```jsonc
// src/protocol/__tests__/fixtures/graph-mutations.json
[
  {
    "name": "node created",
    "raw": "Node my-node created",
    "expectedKinds": ["graph.mutation"],
    "expectedProps": { "mutationType": "node-mutation" }
  },
  {
    "name": "import graph success",
    "raw": "Graph model imported as draft",
    "expectedKinds": ["graph.mutation"],
    "expectedProps": { "mutationType": "import-graph" }
  },
  {
    "name": "graph link in describe response",
    "raw": "Graph with 5 nodes described in /api/graph/model/my-graph/1",
    "expectedKinds": ["graph.link"],
    "expectedProps": { "apiPath": "/api/graph/model/my-graph/1" }
  }
  // ... etc.
]
```

### 7.4 Fixture categories

Each category maps to one fixture file:

| Fixture file | Source of truth | Covers |
|---|---|---|
| `graph-mutations.json` | `detectMutation()` rules + §2.2.1 table in SPEC-auto-graph-refresh | `graph.mutation` events |
| `graph-links.json` | `extractGraphApiPath()` + `isGraphLinkMessage()` | `graph.link` events |
| `large-payloads.json` | `extractLargePayloadLink()` | `payload.large` events |
| `mock-uploads.json` | `extractMockUploadPath()` + `isMockUploadMessage()` | `upload.invitation` events |
| `upload-content-paths.json` | `extractUploadPath()` | `upload.contentPath` events |
| `help-describe-commands.json` | `isHelpOrDescribeCommand()` | `command.helpOrDescribe` + `command.echo` events |
| `import-graph-commands.json` | `extractImportGraphName()` | `command.importGraph` + `command.echo` events |
| `lifecycle-events.json` | `parseMessage()` for `{type, message, time}` JSON; includes frontend-injected messages from `WebSocketContext.tsx` (`connected`, `disconnected`, `already disconnected`) | `lifecycle` events |
| `json-responses.json` | `tryParseJSON()` for non-lifecycle JSON objects | `json.response` events |
| `markdown-candidates.json` | `isMarkdownCandidate()` + exclusion rules in `isPinnableResponse()` | `docs.response` events |
| `negative-cases.json` | False positives from SPEC-auto-graph-refresh §10 (E9, E16, E17) | `unclassified` / absence of mutation |
| `multi-event.json` | Messages that produce 2+ events (e.g. `> help` → `command.echo` + `command.helpOrDescribe`) | multi-event emission |

### 7.5 Test structure

```typescript
// src/protocol/__tests__/classifier.test.ts
import { describe, it, expect } from 'vitest';
import { classifyMessage } from '../classifier';
import graphMutations from './fixtures/graph-mutations.json';
// ... other fixtures

interface TestVector {
  name:          string;
  raw:           string;
  expectedKinds: string[];
  expectedProps?: Record<string, unknown>;
}

function runFixture(vectors: TestVector[]) {
  for (const v of vectors) {
    it(v.name, () => {
      const events = classifyMessage(1, v.raw);
      const kinds = events.map(e => e.kind);
      expect(kinds).toEqual(expect.arrayContaining(v.expectedKinds));
      expect(kinds).toHaveLength(v.expectedKinds.length);
      if (v.expectedProps) {
        // At least one emitted event must contain all expected props
        const match = events.find(e =>
          Object.entries(v.expectedProps!).every(([k, v2]) =>
            (e as Record<string, unknown>)[k] === v2
          )
        );
        expect(match).toBeDefined();
      }
    });
  }
}

describe('classifier — graph mutations', () => runFixture(graphMutations));
// ... one describe() per fixture file
```

### 7.6 Populating fixtures from existing code

The initial fixtures should be built by reading:

1. All test vector tables in SPEC-auto-graph-refresh.md §2.2.1, §3.1, §10.
2. All match/no-match examples in SPEC-large-payload-inline.md §4.
3. All match/no-match examples in SPEC-mock-data-upload-modal.md §2, §5.1.
4. The `messageParser.ts` JSDoc examples and edge-case comments.
5. The real server messages from the Java test fixtures in
   `src/test/resources/graph/` and `src/test/resources/mock/`.
6. The frontend-injected lifecycle messages from
   `src/contexts/WebSocketContext.tsx` — the `eventWithTimestamp()` calls in
   `connect` (`onopen`), `onclose`, and `disconnect` produce
   `{type, message, time}` JSON that is dispatched as `MESSAGE_RECEIVED` and
   reaches the classifier.

### 7.7 CI gate

The test suite must pass before any subsequent phase is merged.

> **G7 note:** No CI workflow file currently exists for the webapp.  The
> implementer should add a step to whichever CI system the project adopts
> (e.g. GitHub Actions, Jenkins):
> ```yaml
> - run: cd extensions/minigraph-playground-engine/webapp && npm ci && npm test
> ```

### 7.8 New protocol modules (formerly Phase 1)

#### New files

| File | Contents |
|---|---|
| `src/protocol/events.ts` | Type definitions from §4 |
| `src/protocol/classifier.ts` | `classifyMessage()` from §5.2 |
| `src/protocol/bus.ts` | `ProtocolBus` class from §5.3 |
| `src/protocol/index.ts` | Barrel re-export |

#### `classifyMessage` implementation contract

```
Input:  msgId: number, raw: string
Output: ProtocolEvent[]   (length >= 1, always)

Invariants:
  - Pure: no side effects, no DOM access, no state.
  - Deterministic: same (msgId, raw) → same output.
  - The result array is never empty; `unclassified` is the fallback.
  - For multi-event messages, the array is ordered by rule priority
    (lifecycle/json first, domain events next, command events last).
```

#### Bus test coverage (`bus.test.ts`)

- `on()` returns an unsubscribe function that works.
- `emit()` calls listeners synchronously in registration order.
- `emit()` wraps each listener in try/catch per E10 (one throw doesn't block others).
- `emit()` is a no-op for kinds with no listeners (no throw).
- `clear()` removes all listeners.
- Type narrowing: a `graph.link` listener receives `GraphLinkEvent`, not the
  full union.

---

## 8. Phase 1 — Watermark Manager

### 8.1 Goal

Implement `useProtocolKernel` — the single React hook that owns the watermark
and drives the classify-and-emit loop.  Wire it into `Playground.tsx` alongside
the existing hooks (dual-path coexistence).

### 8.2 Watermark semantics (unified from 5 existing implementations)

The watermark contract is extracted from the common pattern present in all five
hooks:

```
Mount:
  watermark = messages.length > 0 ? messages[messages.length - 1].id : -1

Each effect run (triggered by messages change):
  newMessages = messages.filter(m => m.id > watermark)
  if newMessages.length === 0 → return
  watermark = messages[messages.length - 1].id
  for each msg in newMessages → classifyMessage(msg.id, msg.raw) → bus.emit(...)

Disconnect:
  No watermark reset (matches useAutoMockUpload / useGraphSaveName pattern).
  Individual hooks that need disconnect cleanup (useAutoGraphRefresh,
  useLargePayloadDownload, useAutoMarkdownPin) handle it in their own
  useEffect([connected]) — those effects remain in the migrated hooks.
```

> **Why no watermark reset on disconnect?**
>
> Resetting the watermark to `-1` on disconnect would cause old messages still
> in the messages array to be replayed as "new" on reconnect.  The existing
> hooks (`useAutoMockUpload` JSDoc documents this explicitly) avoid the reset
> for this reason.  The kernel follows the same rule.

### 8.3 `useEffect` dependency arrays (A2)

The kernel has exactly two effects:

```typescript
// Effect 1: Watermark initialisation (mount only)
useEffect(() => {
  if (messages.length > 0) {
    watermarkRef.current = messages[messages.length - 1].id;
  }
}, []); // eslint-disable-line react-hooks/exhaustive-deps
// Empty dep array — intentionally captures only the mount-time snapshot.

// Render-synchronous classification map (R1)
// Computed over the full messages array so every visible message has an
// entry, including historical messages after navigation-remount.
const classificationMap = useMemo(() => {
  const map = new Map<number, ProtocolEvent[]>();
  for (const msg of messages) {
    map.set(msg.id, classifyMessage(msg.id, msg.raw));
  }
  return map;
}, [messages]);

// Effect 2: Emit new events on the bus (watermark-gated)
useEffect(() => {
  if (messages.length === 0) return;
  const newMessages = messages.filter(m => m.id > watermarkRef.current);
  if (newMessages.length === 0) return;
  watermarkRef.current = messages[messages.length - 1].id;

  for (const msg of newMessages) {
    const events = classificationMap.get(msg.id);
    if (events) {
      for (const event of events) {
        bus.emit(event);
      }
    }
  }
}, [messages, bus, classificationMap]);
// `bus` is a stable ref — included for correctness, never changes.
// `classificationMap` is a new Map on every `messages` change, so the
// effect's firing cadence is identical.  The useMemo runs synchronously
// during render (before the effect), so the map always contains entries
// for all current messages.
// Disconnect cleanup is handled by individual hooks, not the kernel.
```

### 8.4 Playground.tsx wiring

```typescript
// Playground.tsx — added alongside existing hooks
import { useRef } from 'react';
import { ProtocolBus } from '../protocol/bus';
import { useProtocolKernel } from '../protocol/useProtocolKernel';

// Inside Playground component (near the top, before any hook that uses bus):
const busRef = useRef(new ProtocolBus());
const bus = busRef.current;

// classificationMap is unused until Phase 7 — use _ prefix to suppress
// @typescript-eslint/no-unused-vars during Phases 1–6.
const { classificationMap: _classificationMap } = useProtocolKernel({
  messages: ws.messages, bus,
});
// Rename to `classificationMap` (drop _ prefix) in Phase 7 when LeftPanel
// begins consuming it.

// Existing hooks continue to work unchanged — dual-path coexistence.
```

### 8.5 Cleanup — per-hook unsubscribe, NOT `bus.clear()` (A4)

> **v2 note (A4):** The v1 spec called `bus.clear()` in `useProtocolKernel`'s
> cleanup, which would remove ALL listeners — including those registered by
> downstream hooks.  In React StrictMode the teardown ordering between the
> kernel's cleanup and the downstream hooks' cleanups is not guaranteed.
> `bus.clear()` could remove a listener before the hook's own unsubscribe
> function runs, leaving the `Set.delete` call operating on an already-empty
> set — harmless but fragile.  More critically, if effects re-run in a
> different order after the StrictMode double-mount, a hook might end up with
> no listener registered.
>
> **Fix:** `useProtocolKernel` does NOT call `bus.clear()`.  Each downstream
> hook is responsible for calling its own unsubscribe function (the return
> value of `bus.on(...)`) in its `useEffect` cleanup.  `ProtocolBus` still
> exposes `clear()` for use in tests, but it is not called in production code.
>
> `busRef = useRef(new ProtocolBus())` ensures the bus instance is stable
> across HMR and StrictMode — no stale listeners accumulate because the same
> instance is reused and each hook's cleanup removes exactly what it added.

### 8.6 Files created

| File | Contents |
|---|---|
| `src/protocol/useProtocolKernel.ts` | Hook implementation |

### 8.7 Files modified

| File | Change |
|---|---|
| `Playground.tsx` | Add `busRef`, call `useProtocolKernel`, destructure `classificationMap` (initially unused) |

### 8.8 Test coverage

`useProtocolKernel.test.ts`:
- Mount with existing messages → watermark set, no events emitted.
- New message added → classified, event emitted on bus, map entry present.
- Multiple messages in one batch → all classified and emitted in order.
- Disconnect → no watermark reset (verified by adding messages without a new
  connection and confirming they are still processed if their id > watermark).
- Clear messages → map is empty (`useMemo` over empty `messages` array).
- Navigation remount → map covers all historical messages (no empty-map gap).
- StrictMode double-mount → no events emitted for messages present in
  `messages` at mount time, regardless of mount count; single active
  listener after remount (wrap `renderHook` in a `React.StrictMode` wrapper).

---

## 9. Phase 2 — Migrate useAutoGraphRefresh

### 9.1 Goal

Convert `useAutoGraphRefresh` from a direct message-scanner to a bus
subscriber.  This is the highest-risk migration because the hook has the most
complex state machine (debounce timer, `waitingForDescribeRef`, disconnect
resets).

### 9.2 Current responsibilities to preserve

> **v2 note (I2):** v1 of this list referenced `refetchGraph()` (overlay mode).
> The actual code aliases `refetchGraph` as `_refetchGraph` — intentionally
> unused.  Both `node-mutation` and `import-graph` paths always send
> `describe graph` via `sendRawText`, regardless of `pinnedGraphPath`.
> The list below reflects the implemented behavior.

1. **Detect `graph.mutation` events** (replaces `detectMutation()` calls).
2. **Detect `graph.link` events** when `waitingForDescribeRef` is true
   (replaces `isGraphLinkMessage` + `extractGraphApiPath` calls).
3. **Debounce 300 ms** for `node-mutation`, then send `describe graph` via `sendRawText`.
4. **No debounce** for `import-graph` — send `describe graph` immediately via `sendRawText`.
5. **Consume the next `graph.link`** response via `waitingForDescribeRef` →
   call `setPinnedGraphPath(apiPath)` → triggers `useGraphData` initial-load.
6. **Reset `waitingForDescribeRef`** on disconnect.
7. **Cancel debounce timer** on disconnect or unmount.

### 9.3 Interface change

```typescript
export interface UseAutoGraphRefreshOptions {
  bus:                ProtocolBus;      // NEW — replaces messages
  // messages:        REMOVED
  pinnedGraphPath:    string | null;
  setPinnedGraphPath: (path: string | null) => void;
  connected:          boolean;
  refetchGraph:       () => void;      // kept (interface compat; still unused)
  sendRawText:        (text: string) => void;
  rightTab:           RightTab;        // kept (reserved for future)
  addToast:           (message: string, type?: ToastType) => void;
}
```

### 9.4 Implementation sketch

```typescript
export function useAutoGraphRefresh({ bus, ... }: UseAutoGraphRefreshOptions): void {
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const waitingForDescribeRef = useRef(false);
  const pinnedGraphPathRef = useRef(pinnedGraphPath);
  const connectedRef = useRef(connected);
  const sendRawTextRef = useRef(sendRawText);

  // Stale-closure fixes
  useEffect(() => { pinnedGraphPathRef.current = pinnedGraphPath; }, [pinnedGraphPath]);
  useEffect(() => { connectedRef.current = connected; }, [connected]);
  useEffect(() => { sendRawTextRef.current = sendRawText; }, [sendRawText]);

  // Disconnect cleanup — same as current
  useEffect(() => {
    if (!connected) {
      waitingForDescribeRef.current = false;
      if (debounceTimerRef.current !== null) {
        clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = null;
      }
    }
  }, [connected]);

  // Subscribe to graph.link (consume pending describe response)
  useEffect(() => {
    return bus.on('graph.link', (event) => {
      if (waitingForDescribeRef.current) {
        waitingForDescribeRef.current = false;
        setPinnedGraphPath(event.apiPath);
      }
    });
  }, [bus, setPinnedGraphPath]);

  // Subscribe to graph.mutation
  useEffect(() => {
    return bus.on('graph.mutation', (event) => {
      if (!connectedRef.current) return;

      if (event.mutationType === 'import-graph') {
        // Cancel pending debounce
        if (debounceTimerRef.current !== null) {
          clearTimeout(debounceTimerRef.current);
          debounceTimerRef.current = null;
        }
        waitingForDescribeRef.current = true;
        sendRawTextRef.current('describe graph');
        addToast('Graph imported — refreshing view…', 'info');
        return;
      }

      // node-mutation → debounce
      if (debounceTimerRef.current !== null) {
        clearTimeout(debounceTimerRef.current);
      }
      debounceTimerRef.current = setTimeout(() => {
        debounceTimerRef.current = null;
        if (!connectedRef.current) return;
        waitingForDescribeRef.current = true;
        sendRawTextRef.current('describe graph');
        addToast(
          pinnedGraphPathRef.current !== null
            ? 'Graph updated — refreshing…'
            : 'Graph updated — opening Graph tab…',
          'info',
        );
      }, 300);
    });
  }, [bus, addToast]);

  // Cleanup debounce on unmount
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current !== null) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);
}
```

> **Batch-coexistence behavioral changes (B1, B2):** The bus model's
> independent `graph.link` and `graph.mutation` listeners produce different
> behavior from the current code in two batch scenarios:
>
> **B1 — `graph.link` + `graph.mutation` in the same batch:**
> The current code's two-pass structure has a `return` at line 169 after
> consuming a graph-link in Pass 1 — Pass 2 (mutation detection) never
> runs.  The mutation is silently dropped behind the advanced watermark.
> In the bus model, both listeners fire independently, so the mutation IS
> processed (starting a debounce or sending `describe graph`).  This
> produces an extra `describe graph` + toast that the current code never
> generates.  The bus model is strictly more correct: a mutation that
> happens to arrive in the same batch as an unrelated describe-response
> should not be silently discarded.
>
> **B2 — `import-graph` + `node-mutation` in the same batch:**
> The current code collapses all mutations into `hasMutation` /
> `hasImportGraph` booleans and processes `import-graph` first with a
> `return` at line 200 — `node-mutation` events in the same batch are
> dropped.  In the bus model, every `graph.mutation` event fires the
> listener independently: an `import-graph` event sends `describe graph`
> immediately, and a subsequent `node-mutation` event starts a debounce
> timer that eventually sends **another** `describe graph`.  The extra
> describe is harmless (idempotent — the graph is re-fetched) but produces
> an additional toast.  This is a deliberate improvement from the current
> code's lossy batch handling.
>
> Both changes are documented as intentional corrections in §1.3.

> **Stale-closure fix for `connected` (I2):** The `graph.mutation`
> listener and its debounce `setTimeout` callback both need the current
> `connected` value.  A closure capture would be stale: if the user
> disconnects during a 300 ms debounce window, the timer callback's
> closure still holds `connected = true` and would call `sendRawText`.
> The `connectedRef` pattern (identical to `pinnedGraphPathRef`) solves
> this — both the listener body and the `setTimeout` callback read
> `connectedRef.current`.
>
> **Subscription-stable `sendRawText` (R4):** `sendRawText` is a
> `useCallback` in `useWebSocket` whose dep array includes `phase` —
> each connect/disconnect cycle produces a new function identity.
> Without ref-wrapping, the `graph.mutation` listener would be torn down
> and re-registered on every phase transition, creating a subscription
> gap where events could be missed.  `sendRawTextRef` eliminates this:
> the listener captures the ref, and the sync effect keeps it current.
>
> With both `connected` and `sendRawText` ref-wrapped, the dep array
> reduces to `[bus, addToast]`.  `addToast` is stable (`useCallback([])`
> in `useToast`), so the subscription is effectively mount-once:
> - **A11 (subscription gap):** The listener is no longer torn down and
>   re-created on connect/disconnect, so there is no window where events
>   could be missed.
> - **A15 (timer-lifecycle):** Because the listener is not recreated on
>   `connected` change, there is no risk of orphaned timers from a
>   previous listener instance.  The disconnect-cleanup effect still
>   cancels the debounce timer (as a belt-and-suspenders safeguard), but
>   it is no longer the **sole** safety mechanism.

### 9.5 What is removed

- The watermark ref and all watermark logic (now in `useProtocolKernel`).
- Direct imports from `messageParser.ts` (`detectMutation`,
  `isGraphLinkMessage`, `extractGraphApiPath`).
- The `messages` prop.

### 9.6 Playground.tsx change

```diff
  useAutoGraphRefresh({
-   messages:           ws.messages,
+   bus,
    pinnedGraphPath,
    ...
  });
```

### 9.7 Verification

1. Golden transcript tests pass.
2. Manual test: `create node foo` → graph auto-renders.
3. Manual test: `import graph from my-graph` → graph auto-renders.
4. Manual test: rapid `create node a`, `create node b`, `create node c` →
   single debounced refresh.
5. Manual test: disconnect during debounce → no stale sendRawText call.

---

## 10. Phase 3 — Migrate useLargePayloadDownload

### 10.1 Interface change

```typescript
export interface UseLargePayloadDownloadOptions {
  bus:            ProtocolBus;      // NEW — replaces messages
  // messages:    REMOVED
  connected:      boolean;
  appendMessage:  (raw: string) => void;
  addToast:       (message: string, type?: ToastType) => void;
}
```

### 10.2 Implementation change

Replace the watermark + `messages.filter` + `extractLargePayloadLink` loop
with a single `bus.on('payload.large', ...)` subscription.

The `isFetchingRef` re-entrancy guard is **retained** — it is not a watermark
concern but a fetch-lifecycle concern.  When `isFetchingRef.current` is true,
the listener returns early without starting a new fetch.

> **v2 note (I3):** The current code has an explicit `break` after the first
> `payload.large` match per batch (see `useLargePayloadDownload.ts` line 165)
> with a comment explaining why: "Without the break, two concurrent fetches
> could race and both attempt to write `abortRef.current`, leaving the second
> one unabortable."  In the bus model the `isFetchingRef` guard provides the
> same first-match-only semantic: the first `payload.large` event sets
> `isFetchingRef.current = true` before the async fetch begins, so any
> subsequent event in the same batch returns early.  This is a conscious
> preservation, not an accidental omission of the `break`.

The `abortRef` / `AbortController` pattern is retained unchanged.

**Implementation sketch:**

```typescript
// Ref-wrap unstable callbacks — same R4 pattern as sendRawTextRef / onAutoPinRef
const appendMessageRef = useRef(appendMessage);
useEffect(() => { appendMessageRef.current = appendMessage; }, [appendMessage]);

const addToastRef = useRef(addToast);
useEffect(() => { addToastRef.current = addToast; }, [addToast]);

// Subscribe to payload.large events (mount-once)
useEffect(() => {
  return bus.on('payload.large', (event) => {
    if (isFetchingRef.current) return;
    // ... fetch logic uses appendMessageRef.current and addToastRef.current
  });
}, [bus]); // ← [bus] only — appendMessage and addToast are read via refs
```

> **Subscription-stable `appendMessage` (R4-equivalent, v12 B1):**
> `appendMessage` is a `useCallback` in `useWebSocket` whose dep array
> includes `ctx` — the full `WebSocketContext` value object.  Because the
> context value is an inline object literal
> (`{ getSlot, connect, disconnect, send, appendMessage, ... }`), it has a
> **new reference on every Provider render**.  Every incoming WebSocket
> message dispatches `MESSAGE_RECEIVED` into the context reducer, which
> triggers a Provider re-render, which produces a new `ctx` identity, which
> invalidates `appendMessage`'s `useCallback`, which produces a new
> `appendMessage` identity.
>
> With `appendMessage` in the dep array (`[bus, appendMessage, addToast]`),
> the subscription is torn down and re-registered on **every message**:
> - Step 1 (cleanups, all effects): old `payload.large` listener
>   **unsubscribed**
> - Step 2 (new effects, declaration order): `useProtocolKernel` Effect 2
>   runs → emits `payload.large` onto the bus → **no listener registered**
>   → event silently dropped
> - Step 3: `useLargePayloadDownload`'s effect re-runs → new listener added
>   → too late
>
> Ref-wrapping `appendMessage` reduces the dep array to `[bus]` — the
> subscription registers once at mount and never tears down, matching the
> pattern established by `sendRawTextRef` (§9.4) and `onAutoPinRef` (§12.3).
>
> **`addToast` stability:** `addToast` is already stable (`useCallback([])`
> in `useToast`), so its presence in the dep array was not the proximate
> cause of the regression.  It is ref-wrapped anyway for consistency and
> defensive correctness — future changes to `useToast` should not silently
> reintroduce churn.

### 10.3 What is removed

- Watermark ref and all watermark logic.
- Direct import of `extractLargePayloadLink` from `messageParser.ts`.
- The `messages` prop.

### 10.4 Playground.tsx change

```diff
  useLargePayloadDownload({
-   messages:      ws.messages,
+   bus,
    connected:     ws.connected,
    ...
  });
```

---

## 11. Phase 4 — Migrate useAutoMockUpload

### 11.1 Interface change

```typescript
export interface UseAutoMockUploadOptions {
  bus:         ProtocolBus;      // NEW — replaces messages
  // messages: REMOVED
  connected:   boolean;         // kept for interface symmetry
  onOpenModal: (uploadPath: string) => void;
  modalOpen:   boolean;         // NEW (G2) — true while modal is showing
}
```

### 11.2 Implementation change

Replace the watermark + `messages.filter` + `isMockUploadMessage` /
`extractMockUploadPath` loop with a single `bus.on('upload.invitation', ...)`
subscription.

The subscriber calls `onOpenModal(event.uploadPath)` directly — the upload
path is already extracted by the classifier.

> **v2 note (I4):** The current code has an explicit `break` after the first
> match in a batch to prevent stacking modals.  In the bus model, the listener
> fires for **every** `upload.invitation` event in a batch.  To preserve the
> first-match-only semantics, the subscriber must maintain a `pendingModalRef`
> boolean:
>
> ```typescript
> const pendingModalRef = useRef(false);
>
> bus.on('upload.invitation', (event) => {
>   if (pendingModalRef.current) return;   // skip stacked invitations
>   pendingModalRef.current = true;
>   onOpenModal(event.uploadPath);
> });
> ```
>
> `pendingModalRef` is cleared when the modal closes (via a callback or
> effect).  In practice the server sends exactly one invitation per `upload`
> command, but the guard is cheap insurance and matches the current code's
> intent.
>
> **Clearing mechanism (G2):** `Playground.tsx` owns all modal state
> (`modalUploadPath`, `handleCloseUploadModal`, `handleUploadSuccess`).
> `useAutoMockUpload` has no visibility into when the modal closes.  To clear
> `pendingModalRef`, add a new optional prop:
>
> ```typescript
> export interface UseAutoMockUploadOptions {
>   bus:              ProtocolBus;
>   connected:        boolean;
>   onOpenModal:      (uploadPath: string) => void;
>   modalOpen:        boolean;  // NEW — true while modal is showing
> }
> ```
>
> Inside the hook, an effect watches `modalOpen` and clears the guard:
>
> ```typescript
> useEffect(() => {
>   if (!modalOpen) pendingModalRef.current = false;
> }, [modalOpen]);
> ```
>
> In `Playground.tsx`, pass `modalOpen={modalUploadPath !== null}`.  This
> uses the existing `modalUploadPath` state (non-null while the modal is
> open) without requiring any new state or callback plumbing.

### 11.3 What is removed

- Watermark ref and all watermark logic.
- Direct imports from `messageParser.ts`.
- The `messages` prop.

---

## 12. Phase 5 — Migrate useAutoMarkdownPin

### 12.1 Current complexity

This hook has the most nuanced cross-event logic:

1. It watches for `command.helpOrDescribe` echoes to arm
   `waitingForResponseRef`.
2. It then consumes the next `docs.response` event as the auto-pin target.
3. It must skip echoed commands, graph-link messages, and mock-upload
   invitations — all of which are plain text that would otherwise pass the
   markdown-candidate check.

### 12.2 Interface change

```typescript
export interface UseAutoMarkdownPinOptions {
  bus:                ProtocolBus;      // NEW — replaces messages
  // messages:        REMOVED
  connected:          boolean;
  setPinnedMessageId: (id: number) => void;
  onAutoPin?:         () => void;
}
```

### 12.3 Implementation change

Two subscriptions (wrapped in a single `useEffect` for cleanup):

```typescript
const onAutoPinRef = useRef(onAutoPin);
useEffect(() => { onAutoPinRef.current = onAutoPin; });

useEffect(() => {
  // Arm flag on help/describe command
  const unsubArm = bus.on('command.helpOrDescribe', () => {
    waitingForResponseRef.current = true;
  });

  // Consume next docs.response as pin target
  const unsubPin = bus.on('docs.response', (event) => {
    if (waitingForResponseRef.current) {
      waitingForResponseRef.current = false;
      setPinnedMessageId(event.msgId);
      onAutoPinRef.current?.();
    }
  });

  return () => { unsubArm(); unsubPin(); };
}, [bus, setPinnedMessageId]);
```

> **Subscription-stable `onAutoPin` (R4):** `Playground.tsx` passes
> `onAutoPin` as an inline ternary lambda
> (`tabs.includes('preview') ? () => setRightTab('preview') : undefined`),
> which creates a new function reference every render.  Without
> ref-wrapping, the effect's dep array would include `onAutoPin`, causing
> per-render subscription teardown and re-registration.  `onAutoPinRef`
> eliminates this: the listener captures the ref, and the sync effect
> keeps it current.  With `onAutoPin` ref-wrapped, the dep array reduces
> to `[bus, setPinnedMessageId]` — both stable.

> **Dead code note (B3):** The current code at `useAutoMarkdownPin.ts` lines
> 144-146 checks `isHelpOrDescribeCommand(msg.raw)` inside the
> `waitingForResponseRef === true` branch, ostensibly to "re-arm" the flag
> for a new command.  This code is **unreachable**: `isHelpOrDescribeCommand`
> requires `raw.startsWith('> ')`, but line 138 already `continue`s all `> `
> messages before the check is reached.  The "re-arming" never actually
> executes in the current code.  In the bus model, the
> `command.helpOrDescribe` listener fires unconditionally and sets the flag to
> `true` — which is a no-op when it's already `true`.  The end behavior is
> identical, but implementers should not attempt to replicate the dead code
> path.

### 12.4 Why this is safe

The classifier guarantees that `docs.response` is **only** emitted for
messages that pass `isMarkdownCandidate` AND are not echoes, graph-links, or
mock-upload invitations.  The exclusion logic that was previously inline in
`isPinnableResponse` is now embedded in the classifier's rule 11 (§5.2).

### 12.5 Edge case: non-pinnable response clears the flag

> **v2 note (I3):** v1 only subscribed to `json.response` for flag-clearing.
> The actual code clears `waitingForResponseRef` for **any** non-pinnable,
> non-echo response — including graph-links, lifecycle JSON, and mock-upload
> invitations (see `useAutoMarkdownPin.ts` lines 155-161).  The bus model
> must explicitly preserve this behavior.

The current hook's `else` branch fires for every message that is not an echo
(`> ...`) and not pinnable.  That set includes `json.response`, `graph.link`,
`upload.invitation`, and `lifecycle` messages.  Replicate this by subscribing
to all four event kinds:

```typescript
useEffect(() => {
  const clearWaiting = () => {
    if (waitingForResponseRef.current) {
      waitingForResponseRef.current = false;
    }
  };

  const unsubs = [
    bus.on('json.response',      clearWaiting),
    bus.on('graph.link',         clearWaiting),
    bus.on('upload.invitation',  clearWaiting),
    bus.on('lifecycle',          clearWaiting),
    bus.on('payload.large',      clearWaiting),
  ];
  return () => unsubs.forEach(fn => fn());
}, [bus]);
```

This prevents the flag from accumulating across unrelated commands and exactly
matches the current code's behavior where any server response (other than an
echo or a `docs.response` that triggers the pin) resets the flag.

> **`payload.large` addition (G1):** The current code's `else` branch in the
> watermark loop fires for any non-pinnable, non-echo response.  Large-payload
> messages pass `isPinnableResponse` in the current code (the function does not
> exclude them), so the current code would actually **auto-pin** a large-payload
> message if `waitingForResponseRef` is armed — a bug.  In the bus model,
> `payload.large` is excluded from `docs.response` by rule 11, so it needs an
> explicit `clearWaiting` subscription to prevent the flag from staying armed.

### 12.6 What is removed

- Watermark ref and all watermark logic.
- The `isPinnableResponse` helper function (logic absorbed by classifier).
- Direct imports from `messageParser.ts`.
- The `messages` prop.

---

## 13. Phase 6 — Migrate useGraphSaveName & useWebSocket upload

### 13.1 useGraphSaveName

**Interface change:**

```typescript
export function useGraphSaveName(
  storageKey: string,
  bus:        ProtocolBus,     // NEW — replaces messages
  // messages: REMOVED
): UseGraphSaveNameReturn;
```

**Implementation:** Replace the watermark + `extractImportGraphName` scan with:

```typescript
useEffect(() => {
  return bus.on('command.importGraph', (event) => {
    setImportedName(event.graphName);
    setLastSavedNameState(null);
  });
}, [bus]);
```

> **v2 note (G6):** The current hook iterates `newMessages` in reverse with a
> `break`, so the most-recent import in a batch wins.  In the bus model,
> `setImportedName` is called for each `command.importGraph` event in
> chronological order.  React state batching ensures only the last
> `setImportedName` call takes effect before the next render, producing
> identical end-state behavior.  The reverse-iteration + `break` optimization
> is not replicated because it is unnecessary.

### 13.2 useWebSocket upload handshake

**Current:** `useWebSocket` has a `useEffect` that watches `messages` for the
upload path when `pendingUploadRef.current` is true.

> **v2 note (I6):** The current code checks **only**
> `messages[messages.length - 1].raw` (latest message, no watermark).  This is
> NOT the watermark pattern used by the other 5 hooks.  The bus subscription
> fires for every `upload.contentPath` event in a batch.  This is a semantic
> change: if two upload-path messages arrive in one React batch, the bus model
> processes both while the current code only sees the last.  In practice the
> server sends exactly one upload path per `upload` command, so this difference
> is theoretical — but it is documented here for completeness.

**Change:** Subscribe to `bus.on('upload.contentPath', ...)` instead.  The
subscription checks `pendingUploadRef.current` and, if true, fires the POST.

> **Consume-before-fetch ordering (G2):** The listener clears
> `pendingUploadRef.current = false` **synchronously, before calling
> `fetch()`** — same "consume immediately" pattern as the current code
> (`useWebSocket.ts` line 200).  This ensures that a second
> `upload.contentPath` event firing synchronously in the same batch finds the
> flag already cleared and exits early, matching the current code's
> latest-message-only de-duplication semantics (E17).

> **v2 note (A5):** `bus` is optional.  When `bus` is `undefined`, the hook
> falls back to its current `useEffect` + `messages` watch pattern.  This
> preserves backward compatibility for any caller that does not yet have a bus
> instance (e.g. during incremental migration).
>
> **React rules-of-hooks compliance (G4):** `useEffect` cannot be called
> conditionally based on `bus`.  The implementation must call both effects
> unconditionally and gate their bodies:
>
> ```typescript
> // Bus-based subscription (active when bus is provided)
> useEffect(() => {
>   if (!bus) return;
>   return bus.on('upload.contentPath', (event) => {
>     if (!pendingUploadRef.current) return;
>     pendingUploadRef.current = false;   // consume immediately (G2)
>     // ... fire POST
>   });
> }, [bus, ...]);
>
> // Legacy messages-watch fallback (active when bus is NOT provided)
> useEffect(() => {
>   if (bus) return;   // bus takes over — skip legacy path
>   if (!pendingUploadRef.current || messages.length === 0) return;
>   // ... existing extractUploadPath logic
> }, [bus, messages, ...]);
> ```
>
> Both effects are always called (satisfying rules of hooks), but only one
> is active at any time.  The `bus` guard at the top of each effect body
> ensures mutual exclusivity.

This requires `useWebSocket` to accept `bus` as an option:

```typescript
export interface UseWebSocketOptions {
  wsPath:            string;
  storageKeyHistory: string;
  payload:           string;
  addToast:          (message: string, type?: ToastType) => void;
  bus?:              ProtocolBus;      // NEW (optional — A5)
}
```

### 13.3 Playground.tsx changes

Update the `useGraphSaveName` and `useWebSocket` call sites to pass `bus`.

---

## 14. Phase 7 — Migrate ConsoleMessage render-time classification

### 14.1 Goal

`ConsoleMessage` currently calls seven `messageParser.ts` functions on every
render to determine the message's visual treatment (icon, style, pin target,
action buttons).  After this phase, the classification result is precomputed.

### 14.2 Enriched message type

Extend the message store to carry the classification result:

```typescript
export interface ClassifiedMessage {
  id:     number;
  raw:    string;
  events: ProtocolEvent[];   // NEW — set by useProtocolKernel
}
```

### 14.3 useProtocolKernel change

After classifying each new message, attach the `events` array to the message
object.  This requires a small change to the `WebSocketContext` message type or
a parallel lookup map.

**Option A — Parallel map (lower blast radius):**

`useProtocolKernel` computes a `Map<number, ProtocolEvent[]>` via `useMemo`
over the full `messages` array (keyed by message ID).  A new Map instance is
produced on each render where `messages` changes.  `Playground.tsx` passes this
map to `LeftPanel` → `Console` → `ConsoleMessage` as a prop.  `ConsoleMessage`
looks up `classificationMap.get(msg.id)` instead of calling parser functions.

**Option B — Enrich the context message type:**

Change `WebSocketContext`'s message type from `{ id: number; raw: string }` to
`{ id: number; raw: string; events?: ProtocolEvent[] }`.  The kernel writes
`events` after classification.

> **Recommended: Option A** — it avoids modifying the shared context type, which
> is consumed by many files.  The map is a local concern of the playground.

### 14.4 ConsoleMessage change

> **v2 note (G1):** The current `ConsoleMessageProps` has only
> `message: string` — no message ID.  `Console.tsx` has `msg.id` in scope
> (via the `messages.map` loop) but does not pass it down.  Two interface
> changes are required:
>
> 1. Add `msgId: number` to `ConsoleMessageProps`.
> 2. Add `classificationMap?: Map<number, ProtocolEvent[]>` to both
>    `ConsoleProps` and `ConsoleMessageProps`.
> 3. In `Console.tsx`, pass `msgId={msg.id}` and
>    `classificationMap={classificationMap}` to each `<ConsoleMessage>`.
>
> **Post-Phase-7 tightening (P1):** During Phases 1–6, `classificationMap`
> is optional (`?`) because not all consumers pass it yet.  After Phase 7
> completes and all `ConsoleMessage` call sites provide the map, **remove
> the `?`** from both `ConsoleProps` and `ConsoleMessageProps` to make it
> required.  This turns the silent `?? []` fallback into a compile-time
> error if the prop is accidentally omitted — preventing a subtle bug where
> all classification flags silently evaluate to `false`.

```diff
+ // ConsoleMessageProps additions:
+ msgId:               number;
+ classificationMap?:  Map<number, ProtocolEvent[]>;
+
  // Inside ConsoleMessage body:
- const isGraphLink      = isGraphLinkMessage(message);
- const isLargePayload   = isLargePayloadMessage(message);
- const isMockUpload     = isMockUploadMessage(message);
- const mockUploadPath   = isMockUpload ? extractMockUploadPath(message) : null;
+ const events           = classificationMap?.get(msgId) ?? [];
+ const isGraphLink      = events.some(e => e.kind === 'graph.link');
+ const isLargePayload   = events.some(e => e.kind === 'payload.large');
+ const isMockUpload     = events.some(e => e.kind === 'upload.invitation');
+ const mockUploadPath   = (events.find(e => e.kind === 'upload.invitation') as UploadInvitationEvent | undefined)?.uploadPath ?? null;
```

> **Retained parser calls (I1, I2):** The four calls above are the **only**
> `messageParser.ts` calls replaced by `classificationMap` lookups.  The
> following calls are **kept as direct parser imports** and are NOT migrated:
>
> - **`parseMessage` + `getMessageIcon`** — `getMessageIcon(type: MessageType)`
>   expects the narrow `MessageType` union (`'info' | 'error' | 'ping' |
>   'welcome' | 'raw'`).  `LifecycleEvent.type` was broadened to `string` (§4,
>   G5), so deriving the icon from the event would require a cast or a new
>   overload.  `parseMessage` already returns the correct narrow type — keeping
>   these two calls avoids an unnecessary type-safety regression.
>
> - **`isMarkdownCandidate`** — used on line 50 in the `isPinnable` guard:
>   `const isPinnable = !!onPin && !isMockUpload && (!isGraphLink ? isMarkdownCandidate(message) : true);`
>   Replacing this with `events.some(e => e.kind === 'docs.response')` would
>   be a **behavioral change**: echo messages (`> help`) produce
>   `command.echo` + `command.helpOrDescribe` but NOT `docs.response`
>   (rule 11 excludes echoes).  In the current code, echoes pass
>   `isMarkdownCandidate` and ARE manually pinnable.  Keeping the direct call
>   preserves this behavior.
>
> - **`tryParseJSON`** — used for the JSON viewer rendering path
>   (`jsonCheck.isJSON`).  Could theoretically be derived from `json.response`
>   or `lifecycle` events, but both `parseMessage` and `tryParseJSON` are
>   already retained above, and the JSON viewer path needs the parsed
>   `jsonCheck.data` directly.  No benefit to indirecting through the event.
>
> **Runtime type caveat (A14):** The rationale above for retaining
> `parseMessage` (narrow `MessageType` union) is a compile-time guarantee
> only.  At runtime, `parseMessage` does `type: parsed.type || 'info'` where
> `parsed` is the result of `JSON.parse(msg)` (typed as `any`).  An
> unrecognized `type` value (e.g. `"custom"`) passes through as-is, and
> `getMessageIcon` would look up `icons["custom"]` → `undefined` → fallback
> `'•'`.  This is existing behavior and not a regression — but implementers
> should be aware that the narrow type is a TypeScript-level fiction for
> unknown lifecycle types.
>
> **Large-payload manual pinnability (C3):** The current code's `isPinnable`
> guard does not exclude large-payload rows, despite the inline comment at
> `ConsoleMessage.tsx` line 47 claiming "Consistent with isLargePayload rows
> which are also non-pinnable."  The comment describes intended behavior that
> was never implemented.  Phase 7 should fix this by adding
> `&& !isLargePayload` to the `isPinnable` expression:
>
> ```typescript
> const isPinnable = !!onPin && !isMockUpload && !isLargePayload
>   && (!isGraphLink ? isMarkdownCandidate(message) : true);
> ```
>
> Pinning a "Large payload (N) -> GET ..." message to the Markdown Preview
> has no utility — the message is a download prompt, not documentation
> content.

### 14.5 Playground.tsx direct calls

Two call sites in `Playground.tsx` also move to the classification map:

1. **`lastNonJsonMessage` memo** — currently iterates `ws.messages` in reverse,
   calling `isGraphLinkMessage` and `isMarkdownCandidate` per message.

   After migration, iterate the map looking for messages whose events include
   `docs.response` (which already implies "markdown candidate, not graph-link,
   not mock-upload"):

   > **v8 note (R1):** The G3 timing caveat from earlier versions is
   > eliminated.  `classificationMap` is now a `useMemo` result computed
   > synchronously during render, so it always contains entries for all
   > visible messages — including the newest one.  `lastNonJsonMessage` is
   > migrated to read from the map, removing the `isGraphLinkMessage` and
   > `isMarkdownCandidate` direct imports from `Playground.tsx`.
   >
   > **Behavioral narrowing (I1):** The `docs.response` filter is stricter
   > than the original `!isGraphLinkMessage(raw) && isMarkdownCandidate(raw)`
   > check.  The current code would select echoes (`> help`), mock-upload
   > invitations, upload-content-path messages, and large-payload messages as
   > `lastNonJsonMessage` — none of which are meaningful Markdown Preview
   > content.  The `docs.response` filter excludes all of these by rule 11.
   > This is a deliberate improvement, documented in §1.3.

   ```typescript
   const lastNonJsonMessage = useMemo<string | null>(() => {
     if (!tabs.includes('preview')) return null;
     for (let i = ws.messages.length - 1; i >= 0; i--) {
       const msg = ws.messages[i];
       const events = classificationMap.get(msg.id);
       if (events?.some(e => e.kind === 'docs.response')) return msg.raw;
     }
     return null;
   }, [ws.messages, tabs, classificationMap]);
   ```

2. **`handlePinMessage` callback** — currently calls `isGraphLinkMessage` and
   `extractGraphApiPath`.  After migration:

   ```typescript
   const handlePinMessage = useCallback((msg: { id: number; raw: string }) => {
     const events = classificationMap.get(msg.id);
     const graphLink = events?.find(e => e.kind === 'graph.link') as GraphLinkEvent | undefined;
     if (graphLink) {
       setPinnedGraphPath(graphLink.apiPath);
       setPinnedMessageId(msg.id);
     } else {
       setPinnedMessageId(msg.id);
       setPinnedGraphPath(null);
     }
   }, [classificationMap, setPinnedGraphPath, setPinnedMessageId]);
   ```

### 14.6 What is removed from ConsoleMessage

The following `messageParser.ts` imports are **removed** (replaced by
`classificationMap` lookups):

- `isGraphLinkMessage`
- `isLargePayloadMessage`
- `isMockUploadMessage`
- `extractMockUploadPath`

The following imports are **retained** as direct parser calls (see §14.4 notes
on I1, I2):

- `parseMessage` — returns `ParsedMessage` with the narrow `MessageType` union
  needed by `getMessageIcon`.
- `getMessageIcon` — expects `MessageType`, not the broad `string` on
  `LifecycleEvent.type`.
- `isMarkdownCandidate` — used in the `isPinnable` guard; replacing with
  `docs.response` check would change pinnability for echo messages.
- `tryParseJSON` — used for JSON viewer rendering (`jsonCheck.isJSON`,
  `jsonCheck.data`).

### 14.7 What is removed from Playground.tsx

The `import { extractGraphApiPath } from '../utils/messageParser'` call is
removed — `handlePinMessage` now reads from `classificationMap`.

`isMarkdownCandidate` and `isGraphLinkMessage` are **removed** from
`Playground.tsx` — `lastNonJsonMessage` now reads from `classificationMap`
(see R1 note in §14.5).

---

## 15. Phase 8 — Web Worker Promotion Path (Deferred)

> **This phase is NOT part of the initial implementation.  It is documented
> here as the prescribed upgrade path for when message volume or classifier
> complexity grows.**

### 15.1 When to promote

Consider Web Worker promotion when ANY of:
- `MAX_ITEMS` is raised above 1,000.
- The classifier gains CPU-intensive work (e.g. full JSON Schema validation,
  natural language parsing).
- Profiling shows `classifyMessage` appearing in long-task frames (>50 ms).

### 15.2 Promotion plan

1. Create `src/protocol/worker.ts` — a dedicated Worker script that imports
   `classifyMessage` and listens for `MessageEvent<{ msgId, raw }>`.
2. The worker responds with `MessageEvent<{ msgId, events }>`.
3. `useProtocolKernel` is updated to:
   - Instantiate the worker in a `useRef` (one per playground).
   - `postMessage` each new message.
   - On `worker.onmessage`, emit the events on the bus.
4. Structured-clone cost is mitigated by transferring only `msgId` (number) and
   `raw` (string) — two primitives.  The returned `events` array is small.
5. The bus emission becomes asynchronous (microtask delay from worker round-trip).
   All subscribers must already be tolerant of this because React batches state
   updates anyway — but this should be explicitly verified.

### 15.3 Fallback

If `window.Worker` is unavailable (e.g. SSR, legacy browser), fall back to the
synchronous in-main-thread `classifyMessage` call — identical to the non-Worker
path.

---

## 16. File Changelist Summary

### New files (Phases 0–1)

| File | Phase | Description |
|---|---|---|
| `vitest.config.ts` | 0 | Vitest configuration |
| `src/protocol/events.ts` | 0 | Event type definitions |
| `src/protocol/classifier.ts` | 0 | `classifyMessage()` pure function |
| `src/protocol/bus.ts` | 0 | `ProtocolBus` typed emitter |
| `src/protocol/index.ts` | 0 | Barrel re-export |
| `src/protocol/useProtocolKernel.ts` | 1 | React hook: watermark + classify + emit |
| `src/protocol/__tests__/useProtocolKernel.test.ts` | 1 | Kernel hook tests (renderHook) |
| `src/protocol/__tests__/classifier.test.ts` | 0 | Golden transcript tests |
| `src/protocol/__tests__/bus.test.ts` | 0 | Bus unit tests |
| `src/protocol/__tests__/fixtures/*.json` | 0 | Test vector fixtures (~12 files) |

### Modified files (Phases 1–7)

| File | Phase | Change |
|---|---|---|
| `package.json` | 0 | Add `vitest` dev dependency, `test` scripts |
| `tsconfig.json` | 0 | Add `"types": ["vitest/globals", "react", "react-dom"]` and `"resolveJsonModule": true` to `compilerOptions` |
| `src/components/Playground.tsx` | 1 | Add `busRef`, call `useProtocolKernel` |
| `src/components/Playground.tsx` | 2–6 | Pass `bus` to migrated hooks (one change per phase); Phase 4 also adds `modalOpen={modalUploadPath !== null}` to `useAutoMockUpload` call (§11.2 G2) |
| `src/hooks/useAutoGraphRefresh.ts` | 2 | Replace messages/watermark with bus subscription |
| `src/hooks/useLargePayloadDownload.ts` | 3 | Replace messages/watermark with bus subscription |
| `src/hooks/useAutoMockUpload.ts` | 4 | Replace messages/watermark with bus subscription; add `modalOpen` prop + `pendingModalRef` clearing effect (§11.2 G2) |
| `src/hooks/useAutoMarkdownPin.ts` | 5 | Replace messages/watermark with bus subscription |
| `src/hooks/useGraphSaveName.ts` | 6 | Replace messages/watermark with bus subscription |
| `src/hooks/useWebSocket.ts` | 6 | Replace upload-path watch with bus subscription |
| `src/components/LeftPanel/LeftPanel.tsx` | 7 | Add `classificationMap?: Map<number, ProtocolEvent[]>` to `LeftPanelProps`; thread prop to `Console` |
| `src/components/Console/Console.tsx` | 7 | Thread `classificationMap` prop to `ConsoleMessage` |
| `src/components/Console/ConsoleMessage.tsx` | 7 | Replace 4 parser calls with classification map lookups; retain `parseMessage`, `getMessageIcon`, `isMarkdownCandidate`, `tryParseJSON` (§14.6) |
| `src/components/Playground.tsx` | 7 | Pass `classificationMap` to `<LeftPanel>`; migrate `handlePinMessage` and `lastNonJsonMessage` to `classificationMap` lookups; remove `extractGraphApiPath`, `isGraphLinkMessage`, `isMarkdownCandidate` imports; rename `_classificationMap` → `classificationMap` |

### Playground.tsx direct parser calls (migrated in Phase 7)

`Playground.tsx` calls `messageParser.ts` functions directly in two places
that are **not** inside automation hooks.  Both are migrated in Phase 7:

1. **`lastNonJsonMessage` memo** — iterates `ws.messages` in reverse to find
   the most recent plain-text message for the Markdown Preview fallback.

   **Migrated to `classificationMap`** — looks for messages whose events
   include `docs.response`.  The map is a `useMemo` result computed
   synchronously during render, so no timing concerns apply (see R1 note
   in §14.5).

2. **`handlePinMessage` callback** — calls `isGraphLinkMessage(msg.raw)` and
   `extractGraphApiPath(msg.raw)` to decide whether a pinned console row is a
   graph-link or a markdown message.

   **Migrated** — reads from `classificationMap`.  This is safe because the
   callback fires from a user click, well after the map is populated.

### Files NOT modified

| File | Reason |
|---|---|
| `src/utils/messageParser.ts` | Retained as the parser library; consumed by `classifier.ts` and `ConsoleMessage.tsx` (§14.6) |
| `src/contexts/WebSocketContext.tsx` | Message store shape unchanged; kernel sits downstream |
| `src/utils/graphTransformer.ts` | Unrelated to message classification |
| `src/utils/graphTypes.ts` | Unrelated |
| `src/utils/urls.ts` | Unrelated |
| `src/utils/validators.ts` | Unrelated |

---

## 17. Edge Cases & Pitfall Index

| # | Scenario | Expected behaviour | Phase |
|---|---|---|---|
| E1 | `useProtocolKernel` mounts with 150 existing messages | Watermark set to last ID; no events emitted for historical messages.  `classificationMap` covers all 150 messages regardless of watermark. | 1 |
| E2 | Two messages arrive in the same React batch | Both classified and emitted in order (ascending ID). | 1 |
| E3 | `appendMessage` (local-only) triggers the kernel effect | The appended message is classified like any other.  If it is a fetched JSON payload, it emits `json.response`.  Hooks that previously guarded against this with `isFetchingRef` retain that guard in their bus listener. | 3 |
| E4 | Disconnect while `waitingForDescribeRef` is true | The hook's own `useEffect([connected])` clears the flag — same as current.  The kernel does NOT reset the watermark. | 2 |
| E5 | Hot-module-reload (HMR) double-fires effects | Each hook's `bus.on()` returns an unsubscribe function used in `useEffect` cleanup, preventing listener accumulation.  `busRef = useRef(new ProtocolBus())` ensures a stable instance across HMR. | 1 |
| E6 | StrictMode double-mount | Same as E5.  The cleanup from the first mount's effects removes listeners via the returned unsubscribe functions; the second mount's effects re-add them.  No duplicate emissions because **on each mount, the watermark is initialized to the last message ID in the current `messages` array** (Effect 1, §8.3) — all messages present at mount time are treated as historical and are not re-emitted on the bus.  This invariant holds regardless of mount count.  *Implementation note:* In current React, `useRef` values persist across the StrictMode teardown→remount cycle, so the watermark set by the first mount's Effect 1 is still present when the second mount's Effect 1 runs (idempotent write of the same value). | 1 |
| E7 | Message matches multiple classifier rules (e.g. `> help` is both `command.echo` and `command.helpOrDescribe`) | `classifyMessage` returns an array with both events.  Each subscriber only listens to its own kind. | 0 |
| E8 | `isPinnableResponse` exclusion logic after migration | Absorbed into classifier rule 11 — `docs.response` is only emitted for messages that are NOT echoes, graph-links, mock-upload invitations, upload-content-path messages, or large-payload messages.  No separate check needed in `useAutoMarkdownPin`. | 5 |
| E9 | `Node foo already exists` (error, not a mutation) | Does not match `detectMutation` → no `graph.mutation` event → no false positive refresh.  Covered by golden transcript fixture `negative-cases.json`. | 0 |
| E10 | Bus listener throws an exception | `ProtocolBus.emit` wraps each listener call in try/catch (logs to `console.error`) to prevent one failing listener from blocking others. | 0 |
| E11 | Playground unmounts while a bus listener's async work is in flight | The unsubscribe function returned by `bus.on()` is called in `useEffect` cleanup.  The in-flight async work (e.g. fetch in `useLargePayloadDownload`) is cancelled by `abortRef` — same as current.  The bus listener removal prevents new work from being kicked off. | 3 |
| E12 | Two playgrounds mounted simultaneously (navigation + preload) | Each has its own `ProtocolBus` instance (via `useRef`).  No cross-contamination. | 1 |
| E13 | `json.response` event for a lifecycle message | Not emitted — the classifier checks for lifecycle FIRST (rule 1) and only emits `json.response` for non-lifecycle JSON (rule 2).  Test in `lifecycle-events.json`. | 0 |
| E14 | `graph.link` message that is also a `docs.response` candidate | The `graph.link` message is plain text that passes `isMarkdownCandidate`, but the classifier's rule 11 excludes it from `docs.response` because it matched `graph.link` in rule 6.  Test in `multi-event.json`. | 0 |
| E15 | Mock-upload invitation that is also a `docs.response` candidate | Same as E14 — excluded from `docs.response` by rule 11. | 0 |
| E16 | Two `upload.invitation` events arrive in one batch | The `pendingModalRef` guard in the `upload.invitation` listener (§11.2, I4 note) ignores the second event.  Only one modal opens — matching the current code's `break` behavior. | 4 |
| E17 | Two `upload.contentPath` events arrive in one batch | The bus listener fires for both, but `pendingUploadRef` is cleared after the first POST starts, so the second is a no-op.  The current code only checks `messages[messages.length - 1]`, so it would see only the last.  In practice the server sends one path per upload command, making this theoretical (§13.2, I6 note). | 6 |
| E18 | Large-payload message that is also a `docs.response` candidate | The large-payload message is plain text that passes `isMarkdownCandidate`, but the classifier's rule 11 excludes it from `docs.response` because it matched `payload.large` in rule 3.  Without this exclusion, `useAutoMarkdownPin` could auto-pin a download link when `waitingForResponseRef` is armed — same class of bug as `upload.contentPath` (§4.2 G1 note).  Test in `multi-event.json`. | 0 |

---

## 18. Open Questions (Deferred)

| # | Question | Notes |
|---|---|---|
| Q1 | Should `messageParser.ts` functions used only by the classifier be unexported? | Low priority.  Reduces API surface but makes the test fixtures harder to write (they'd need to go through `classifyMessage` only).  Defer to post-migration hygiene. |
| Q2 | Should the bus support `once()` (fire-then-unsubscribe) semantics? | Would simplify `useAutoGraphRefresh`'s `waitingForDescribeRef` pattern.  Evaluate after Phase 2. |
| ~~Q3~~ | ~~Should `ConsoleMessage` receive individual event-derived props or the full `ProtocolEvent[]` array?~~ | **Resolved (A6):** `ConsoleMessage` receives a `classificationMap: Map<number, ProtocolEvent[]>` prop (Option A, §14.3).  Rationale: individual boolean props (`isGraphLink`, `mockUploadPath`, …) would couple `Console.tsx` to every future event kind, requiring prop additions whenever a new event type is added.  The map is a single prop that scales without interface changes.  Each component reads `events.some(e => e.kind === ...)` — no exposure of internal event constructors beyond the `kind` discriminator. |
| Q4 | Should the kernel emit events for keep-alive ping/pong messages? | Currently filtered out before they reach `messages[]` (in `WebSocketContext.onmessage`).  No kernel involvement needed unless that filter is ever removed. |
| Q5 | Integration test strategy for React hooks + bus? | Vitest + `@testing-library/react` `renderHook` would allow testing the full kernel → bus → hook pipeline.  Worth adding after Phase 2. |
