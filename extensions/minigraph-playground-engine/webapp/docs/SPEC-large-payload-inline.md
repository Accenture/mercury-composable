# Feature Spec: Large-Payload Inline Console Rendering

**Target:** `webapp/` (React/TypeScript frontend)
**Branch:** `feature/playground`
**Date:** 2026-03-19
**Status:** v4 — peer-review applied, ready for implementation

---

## Table of Contents

1. [Background & Motivation](#1-background--motivation)
2. [Current Behaviour (to be replaced)](#2-current-behaviour-to-be-replaced)
3. [Target Behaviour](#3-target-behaviour)
4. [Exact Message-Matching Rules (unchanged)](#4-exact-message-matching-rules-unchanged)
5. [Architecture: Where the Logic Lives](#5-architecture-where-the-logic-lives)
6. [State Inventory & Lifecycle](#6-state-inventory--lifecycle)
7. [Detailed Behaviour Specification](#7-detailed-behaviour-specification)
8. [ConsoleMessage Rendering Changes](#8-consolemessage-rendering-changes)
9. [File Changelist](#9-file-changelist)
10. [Edge Cases & Pitfall Index](#10-edge-cases--pitfall-index)
11. [Open Questions (Deferred)](#11-open-questions-deferred)

---

## 1. Background & Motivation

The backend enforces a 64 KB inline WebSocket limit.  When a namespace value
(or any inspectable field) exceeds this threshold it is not echoed through the
socket; instead the server sends a plain-text "Large payload" notification:

```
Large payload (254922) -> GET /api/inspect/ws-734563-3/input.body
```

### Small-payload path (current, unchanged)

For payloads within the 64 KB limit the server sends the value as a regular
WebSocket message.  The existing `ConsoleMessage` component renders it as a
collapsed `JsonView` tree, and a hover-visible **➡️** button lets the user
send the JSON into the JSON-Path Playground payload editor with one click —
_without_ leaving the current playground.

### Large-payload path (current, to be replaced)

The current `useLargePayloadDownload` hook:

1. Detects the notification message.
2. Guards that the JSON-Path Playground WebSocket is already connected.
3. Navigates away to `/json-path`.
4. Fetches the payload from `/api/inspect/…`.
5. Deposits it via `ctx.setPendingPayload` into the payload editor.

This forces a context switch before the user has even seen the data and
requires them to have pre-connected the JSON-Path Playground.

### Goal

> Large payloads should behave identically to small payloads from the user's
> perspective: the fetched JSON appears **in the console** where the
> notification was, collapsed and scrollable, with the same **➡️**
> send-to-JSON-Path button — no navigation, no pre-connection requirement.

---

## 2. Current Behaviour (to be replaced)

```
Server sends "Large payload (N) -> GET /api/inspect/…"
  → useLargePayloadDownload detects it
  → Guards JSON-Path connection (error toast if not connected)
  → navigate('/json-path')                    ← ❌ removed
  → fetch(apiPath)                            ← kept
  → ctx.setPendingPayload(wsPath, content)    ← ❌ removed
  → addToast('Payload loaded…')               ← ❌ removed (replaced)
```

**Side-effects to eliminate:**
- `navigate()` call — removes forced context switch.
- `ctx.setPendingPayload` — no longer needed for the primary flow.
- The JSON-Path connection guard — no longer a prerequisite for fetching.
- The `useNavigate` import in the hook.

---

## 3. Target Behaviour

```
Server sends "Large payload (N) -> GET /api/inspect/…"
  → useLargePayloadDownload detects it
  → addToast(`Fetching large payload (${sizeMB} MB)…`, 'info')  ← NEW
  → fetch(apiPath)
  → on success: appendMessage(prettyJSON)                   ← NEW
  → the new message renders in Console as a collapsed JsonView
  → hover reveals ➡️ button → sends to JSON-Path editor     ← same as small payload
```

The notification row itself (`"Large payload (N) -> GET …"`) **remains in the
console** — it becomes the history record of where the payload came from and
is useful for debugging.  No deletion or replacement of the original message.

---

## 4. Exact Message-Matching Rules (unchanged)

`extractLargePayloadLink` in `messageParser.ts` is **not modified**.

```
/Large payload \((\d+)\)\s*->\s*GET\s+(\/api\/inspect\/[^\s]+)/i
```

`isLargePayloadMessage` continues to drive the amber styling in `ConsoleMessage`.

---

## 5. Architecture: Where the Logic Lives

### 5.1 Responsibility table

| Concern | Owner | Notes |
|---|---|---|
| Detect large-payload notification | `useLargePayloadDownload` | unchanged |
| Fetch JSON from `/api/inspect/…` | `useLargePayloadDownload` | unchanged |
| Append fetched content to console | `useLargePayloadDownload` via `appendMessage(content)` | **new** — replaces `setPendingPayload` + `navigate`; note: 1-arg form — `wsPath` already bound in the hook's option |
| Render JSON tree in console | `ConsoleMessage` | unchanged — already handles JSON via `JsonView` |
| Send to JSON-Path editor (➡️ button) | `Playground.tsx` → `handleSendToJsonPath` | unchanged — already wired to `onSendToJsonPath` |
| Per-message copy button | `ConsoleMessage` | unchanged |

### 5.2 Hook signature changes

`useLargePayloadDownload` **removes** the `navigate` dependency:

```typescript
// BEFORE
import { useNavigate } from 'react-router-dom';
const navigate = useNavigate();

// AFTER — removed entirely
```

The `UseWebSocketDownloadOptions` interface and the hook's `useEffect`
dependency array shrink by two entries.  The final dep array is:

```typescript
[messages, connected, appendMessage, addToast]
```

`navigate` and `ctx` are both removed.  Note: `useNavigate` returns a new
function identity on every render in some React Router versions; removing it
eliminates a source of unnecessary effect re-runs.  No other public API changes.

### 5.3 WebSocketContext changes

**None.**  `setPendingPayload` / `takePendingPayload` are **not removed** —
`handleSendToJsonPath` in `Playground.tsx` still uses them for the ➡️ button
flow on both small and large payloads.  The hook simply stops calling
`setPendingPayload` itself.

### 5.4 Playground.tsx changes

**None.**  `handleSendToJsonPath` already handles any pretty-printed JSON
string regardless of how it arrived in the console.  The hook no longer calls
`navigate`, so the import of `useNavigate` in `Playground.tsx` is unaffected
(it is still needed by `handleSendToJsonPath`).

---

## 6. State Inventory & Lifecycle

### 6.1 Message-ID watermark (`watermarkRef`)

`watermarkRef` is a `useRef<number>` initialised to `-1`.  On mount, the
`useEffect` with an empty dep-array sets it to the id of the last existing
message so historical notifications are never replayed after a navigation
round-trip.

**No change needed** — this guarantee holds identically under the new flow
because `appendMessage` adds a new message with a higher id than the
watermark; that new message is above the watermark and **will** be processed
on the next `messages` tick.

> ⚠️ **Subtle re-entrancy concern:** when `appendMessage` is called, it
> dispatches to `WebSocketContext`'s reducer which increments `msgIdRefs` and
> produces a new `messages` array via `useReducer`.  The `messages` change
> triggers the hook's main `useEffect` again.  The appended message will have
> an id **above** the watermark, so `newMessages` will contain it.
>
> **Fix:** After `appendMessage` is called, advance `watermarkRef.current`
> to the id of the message just appended **before** the effect returns.
> Because `appendMessage` is a synchronous context dispatch, the new id is
> `ctx.msgIdRefs.current[wsPath]` — but that ref is internal to the context
> and not exposed.
>
> **Simpler fix (chosen):** set a local `pendingRef` boolean flag
> (`isFetchingRef`) to `true` while a fetch is in flight and skip the
> inner-loop processing when it is set.  When the fetch resolves, clear the
> flag and advance the watermark to cover the newly-appended message.
> See §7.4 for the full implementation pattern.

### 6.2 In-flight fetch AbortController (`abortRef`)

`abortRef` holds the `AbortController` for any in-flight fetch.

- **On disconnect** (`connected` changes to `false`): `abortRef.current?.abort()` and `abortRef.current = null`.  The aborted fetch's `.catch` branch checks `err.name === 'AbortError'` and silently returns — no error message is appended, no toast.  This is correct; a stale payload is useless.
- **On unmount**: same abort pattern via cleanup effect.
- **On new large-payload message arriving while fetch is in flight**: `abortRef.current?.abort()` is called before starting the new fetch.  Only one in-flight fetch at a time.  This matches current behaviour.

### 6.3 `isFetchingRef` (new)

A `useRef<boolean>` initialised to `false`.

Purpose: prevent the appended result message from being re-processed as a new
large-payload notification by the hook's own main effect.

**Lifecycle:**
| Moment | Value |
|---|---|
| At mount | `false` |
| Just before `fetch()` call | `true` |
| After `appendMessage(content)` returns | `false`, watermark advanced |
| After abort (disconnect / unmount) | `false` |
| After fetch error (non-abort) | `false` |

> **Interaction with the watermark (Gap 2):** The main effect's first
> statement advances the watermark unconditionally:
> ```typescript
> watermarkRef.current = messages[messages.length - 1].id;
> ```
> The `isFetchingRef` early return (`if (isFetchingRef.current) return`)
> fires **before** this line.  Consequently, non-large-payload messages that
> arrive while a fetch is in flight do not advance the watermark on that tick.
> Those messages remain above the watermark and are re-scanned on the next
> tick once `isFetchingRef` is cleared.  This is **safe**: `extractLargePayloadLink`
> returns `null` for any non-notification string, so they are simply skipped
> with no side effects.  The only cost is one redundant scan per batch, which
> is negligible.

> **Mount ordering:** React guarantees that `useEffect` hooks fire in source
> order within the same component.  The mount watermark effect (empty dep-array)
> is declared **before** the main messages effect in the hook, so it runs first
> on the initial render.  By the time the main effect fires for the first time,
> `watermarkRef.current` already reflects the id of the last pre-existing
> message.  `isFetchingRef.current` is `false` at mount, so the early return
> does not interfere with this ordering.

### 6.4 `JSON_PATH_CONFIG` module-level constants

Still resolved at module load time from `PLAYGROUND_CONFIGS`.  However,
`JSON_PATH_ROUTE` and `JSON_PATH_WS` are **no longer used inside the hook**
— `navigate` is removed.  `JSON_PATH_WS` is also no longer needed for the
connection guard.

**Action:** Remove all three constants from the hook — they are not used
anywhere else.  Note: the current code uses `JSON_PATH_CONFIG` to provide
fallback strings (`'/json-path'` and `'/ws/json/path'`) when
`PLAYGROUND_CONFIGS.find(...)` returns `undefined`.  Removing the constants
also removes this fallback concern — **intentionally** — because neither the
fetch path nor the `appendMessage` call requires knowledge of the JSON-Path
playground's route or wsPath.  The fallback strings had value only for the
now-deleted `navigate()` + `getSlot()` calls.

---

## 7. Detailed Behaviour Specification

### 7.1 Normal happy path

```
messages tick arrives with newMessages = [{ id: 42, raw: "Large payload (254922) -> GET /api/inspect/ws-1/input.body" }]

watermark was 41 → newMessages = [id:42]
isFetchingRef = false → proceed

link = extractLargePayloadLink(raw)
  → { apiPath: '/api/inspect/ws-1/input.body', byteSize: 254922, filename: 'input.body.json' }

isFetchingRef = true
abortRef.current?.abort()          // cancel any prior in-flight
const controller = new AbortController()
abortRef.current = controller

sizeMB = '0.24'
addToast(`Fetching large payload (0.24 MB)…`, 'info')

fetch('/api/inspect/ws-1/input.body', { signal })
  → 200 OK, body = '{"foo":…}'
  → content = JSON.stringify(JSON.parse(body), null, 2)    // pretty-print

appendMessage(content)              // 1-arg: wsPath already bound in hook options
watermarkRef.current = newMessages[newMessages.length - 1].id + 1  // safe upper bound — see §7.4
isFetchingRef.current = false
abortRef.current = null
```

The new message appears in the console.
`ConsoleMessage` receives it, `tryParseJSON` succeeds, `JsonView` renders it
collapsed at depth < 2.  `canSendToJsonPath` is `true` because
`onSendToJsonPath` is wired and `jsonCheck.isJSON` is `true`.

### 7.2 Canonical watermark formula: `newMessages[newMessages.length - 1].id + 1`

The canonical formula used everywhere in this spec is:

```typescript
watermarkRef.current = newMessages[newMessages.length - 1].id + 1;
```

**Why `+1`?**  After `appendMessage(content)` is called, the message dispatched
into the context's reducer receives the next sequential id — `notificationId + 1`
or higher (if other messages arrived concurrently).  Using `+1` as a safe upper
bound guarantees the appended row is treated as "already seen" even in the same
synchronous continuation of `.then()`, before React has re-rendered and before
`messages` in the closure is updated.

**Why not `messages[messages.length - 1].id`?** At the point `.then()` runs,
`messages` is the **stale closure value** from the render cycle in which the
large-payload notification was detected.  Setting the watermark to that value
would leave the appended row (id = notification + 1) above the watermark — it
would be scanned on the next tick.  The `isFetchingRef` guard catches this
safely (the appended JSON does not match the large-payload regex), but the
`+1` formula avoids the scan entirely and is unambiguously correct.

**Conclusion:** `isFetchingRef` is the critical re-entrancy guard.  The `+1`
watermark advance in `.then()` is belt-and-suspenders.  Both are required for
robustness.

### 7.3 Disconnect during fetch

```
connected changes false
  → abortRef.current?.abort()
  → isFetchingRef = false   ← MUST be cleared here too
  → abortRef.current = null

fetch rejects with AbortError → caught, silently returns (no toast, no append)
```

> ⚠️ **If `isFetchingRef` is not cleared in the disconnect effect**, the next
> reconnect + large-payload message will be silently skipped because the guard
> still reads `true`.  This is a subtle latent bug in any implementation that
> forgets this step.

### 7.4 Re-entrancy guard: full pattern

The `isFetchingRef` guard must be inserted as the **very first statement** in
the main messages effect body — before the `messages.length === 0` check.
This avoids the `filter()` call entirely when a fetch is in flight and is the
cleanest insertion point:

```typescript
// Main messages effect — isFetchingRef guard is the FIRST line:
if (isFetchingRef.current) return;   // ← inserted before messages.length === 0 check
if (messages.length === 0) return;
const newMessages = messages.filter(m => m.id > watermarkRef.current);
if (newMessages.length === 0) return;
watermarkRef.current = messages[messages.length - 1].id;  // unconditional advance

// Just before fetch():
isFetchingRef.current = true;

// In .then():
appendMessage(content);
watermarkRef.current = newMessages[newMessages.length - 1].id + 1; // canonical — see §7.4
isFetchingRef.current = false;
abortRef.current = null;

// In .catch() (non-abort):
// isFetchingRef is cleared BEFORE appendMessage — intentional and safe:
// the error string cannot match extractLargePayloadLink, so re-entrancy
// risk is zero. Clearing early is consistent with the .then() path.
isFetchingRef.current = false;
abortRef.current = null;
appendMessage(`ERROR: payload fetch failed — ${err.message}`);  // 1-arg form
addToast(...);
```

Note: `watermarkRef.current = newMessages[lastIdx].id + 1` uses `+1` as a
safe upper bound to guarantee the appended message (which will have id =
lastIdx + 1 or higher) is treated as "already seen" on the next tick.

### 7.5 Multiple large-payload notifications in one batch

The current code `break`s after the first link found in a batch.  This is
**unchanged** — only one fetch is initiated per effect run.  Additional
notifications in the same batch will be processed on subsequent ticks once
`isFetchingRef` is cleared.

> The `break` is important: without it, two concurrent fetches could race and
> both attempt to set `abortRef.current`, leaving the second one unabortable.

### 7.6 Server error response (non-2xx)

```
fetch('/api/inspect/…') → 500 Internal Server Error

.then(res => { if (!res.ok) throw new Error(`HTTP ${res.status}`); … })
.catch((err: Error) => {
  if (err.name === 'AbortError') return;
  isFetchingRef.current = false;
  abortRef.current = null;
  appendMessage(`ERROR: payload fetch failed — ${err.message}`);  // 1-arg form
  addToast(`Payload fetch failed: ${err.message}`, 'error');
});
```

The original notification row (`Large payload (N) -> GET …`) remains visible
in the console.  The error message is appended below it.  The user can re-run
the command that produced the large payload if needed.

### 7.7 Non-JSON response body

```typescript
let content = text;
try {
  content = JSON.stringify(JSON.parse(text), null, 2);
} catch {
  // Not JSON — pass raw text through unchanged
}
appendMessage(content);  // 1-arg form
```

If the server returns XML or plain text (edge case — the endpoint is
`/api/inspect/…` which always returns JSON), the raw string is appended.
`ConsoleMessage` will render it as a `<span className={messageText}>` via the
`!jsonCheck.isJSON` branch.  The ➡️ button will NOT appear because
`canSendToJsonPath` requires `jsonCheck.isJSON === true` — correct, since
non-JSON cannot be deposited into the payload editor meaningfully.

---

## 8. ConsoleMessage Rendering Changes

### 8.1 No changes required

The fetched JSON is appended as a plain raw string.  `ConsoleMessage` already
handles this correctly:

- **`parseMessage(raw)`** is called first.  Because the raw string is valid
  JSON, `JSON.parse(msg)` succeeds.  `parseMessage` then reads
  `parsed.message || msg` — a top-level JSON object has no `.message` key, so
  `parsed.message` is `undefined`, and the expression falls back to `msg` (the
  full JSON string).  `parsed.type` similarly falls back to `'info'`.  This is
  the correct pass-through path; the string is not mistaken for a lifecycle
  event.
- **`tryParseJSON(parsed.message)`** — called with the full JSON string —
  returns `{ isJSON: true, data: <object> }`.
- **`JsonView`** renders the tree collapsed at depth < 2.
- **`canSendToJsonPath`** → `true` (when `onSendToJsonPath` is wired).
- **Copy button** present (always).
- **`isLargePayloadMessage(raw)`** returns `false` — the pretty-printed JSON
  does not match `/Large payload \(\d+\)/` — so the row receives **no** amber
  `consoleMessageLargePayload` styling.  This is correct: it is the payload
  content, not a link.  The icon shown will be the standard `'info'` icon
  (`ℹ️`) from `getMessageIcon('info')`.

### 8.2 Visual sequence in the console

```
[⬇️] Large payload (254922) -> GET /api/inspect/ws-1/input.body   ← amber row (existing)
[ℹ️] {                                                              ← new JsonView row
       "foo": "bar",
       …
     }
```

The notification row is preserved, giving the user the context of where the
data came from.  The rendered JSON row immediately follows.

### 8.3 "Send to JSON-Path" button on large payloads

The ➡️ button on the rendered JSON row calls `handleSendToJsonPath(prettyJSON)`
in `Playground.tsx`, which:

1. Checks that the JSON-Path Playground is connected (same guard as before).
2. Calls `ctx.setPendingPayload(JSON_PATH_WS, json)`.
3. Calls `navigate(JSON_PATH_ROUTE)`.
4. Shows a success toast.

This is **identical to the small-payload path**.  No changes are required in
`Playground.tsx` or `handleSendToJsonPath`.

---

## 9. File Changelist

### Modified

#### `src/hooks/useLargePayloadDownload.ts`

| Change | Reason |
|---|---|
| Remove `useNavigate` import and `navigate` call | Navigation is eliminated |
| Remove `JSON_PATH_CONFIG`, `JSON_PATH_ROUTE`, `JSON_PATH_WS` module constants and `PLAYGROUND_CONFIGS` import | No longer used in the hook after constants are removed |
| Update hook-body JSDoc comment to describe new behaviour | The existing comment describes the old navigation/`setPendingPayload` flow in detail and will be actively misleading after implementation |

The replacement JSDoc should read:

```typescript
/**
 * Watches the WebSocket message stream for large-payload inspect links.
 *
 * When the server sends a message matching:
 *   "Large payload (<bytes>) -> GET /api/inspect/<id>/<namespace>"
 *
 * this hook fetches the payload from the backend inspect endpoint and
 * appends it directly to this playground's console as a collapsible
 * JSON row — no navigation, no pre-connection requirement.
 *
 * The user can then use the per-row ➡️ button (onSendToJsonPath) to
 * move the payload into the JSON-Path Playground editor in one click,
 * identical to the flow for inline small payloads.
 *
 * Follows the same patterns as useAutoGraphRefresh / useAutoMarkdownPin:
 *  - Message-ID watermark set at mount prevents replaying history.
 *  - isFetchingRef guard prevents re-entrancy when the appended result
 *    message triggers the effect again.
 *  - AbortController cancels in-flight fetches on disconnect or unmount.
 */
```
| Remove JSON-Path connection guard (`ctx.getSlot(JSON_PATH_WS)`) | Not a prerequisite for fetching |
| Remove `ctx.setPendingPayload(...)` call | Payload goes to console, not editor |
| Remove `navigate(JSON_PATH_ROUTE)` call | Eliminated |
| Add `isFetchingRef = useRef(false)` | Re-entrancy guard (§6.3) |
| In `.then()`: call `appendMessage(content)` (1-arg) | Puts payload in console; `wsPath` already bound in hook options |
| In `.then()`: add empty-body guard before `JSON.parse` | Routes 200-with-empty-body to the error path rather than appending an empty row (see E4) |
| In `.then()`: advance `watermarkRef.current = newMessages[newMessages.length - 1].id + 1` | Belt-and-suspenders guard — prevents re-scan of appended row (§7.2) |
| In `.then()`: clear `isFetchingRef.current = false` | Release guard |
| In `.catch()` (non-abort): clear `isFetchingRef.current = false` | Release guard |
| In `.catch()` (non-abort): call `` appendMessage(`ERROR: payload fetch failed — ${err.message}`) `` (1-arg) | Appends error to console; distinct from the loop variable `msg` |
| In disconnect effect: clear `isFetchingRef.current = false` | Prevent stale guard on reconnect (§7.3) |
| Change `addToast` content to `` `Fetching large payload (${sizeMB} MB)…` `` (`'info'`) | Describes actual new behaviour |
| Remove success toast ("Payload loaded into JSON-Path editor ✓") | No longer navigating away |
| Remove `useWebSocketContext` import and `ctx` variable | Both uses of `ctx` (`getSlot` and `setPendingPayload`) are removed; leaving the import creates a stale dep-array entry on `ctx` |

> **Note:** `UseWebSocketDownloadOptions` interface does not change.
> `appendMessage`, `addToast`, `messages`, and `connected` are still the
> four inputs.  `ctx` (WebSocketContext) is no longer imported.

### Unmodified

| File | Reason untouched |
|---|---|
| `src/utils/messageParser.ts` | Detection logic unchanged |
| `src/components/Console/ConsoleMessage.tsx` | Already handles JSON rendering + ➡️ button |
| `src/components/Console/Console.tsx` | No interface changes |
| `src/components/Console/Console.module.css` | No new CSS classes needed |
| `src/components/Playground.tsx` | `handleSendToJsonPath` and `onSendToJsonPath` wiring unchanged |
| `src/contexts/WebSocketContext.tsx` | `setPendingPayload` / `takePendingPayload` retained for ➡️ button flow |
| `src/config/playgrounds.ts` | No config changes |

---

## 10. Edge Cases & Pitfall Index

### E1 — Stale `isFetchingRef` after disconnect
**Scenario:** Fetch is in flight; user disconnects.
**Risk:** `isFetchingRef` stays `true`; next reconnect + large payload is silently dropped.
**Mitigation:** Clear `isFetchingRef.current = false` in the `connected → false` effect alongside `abortRef.current?.abort()`.

### E2 — Appended message triggers re-processing
**Scenario:** `appendMessage(prettyJSON)` increments the message list; the main effect fires again with the new message in `newMessages`.
**Risk:** The hook calls `extractLargePayloadLink(prettyJSON)` on the JSON content.  Since pretty-printed JSON does not match the pattern (`/Large payload \(\d+\)/`), this returns `null` and is skipped — **no action taken**.  Safe.
**Belt-and-suspenders:** `isFetchingRef` guard still catches this before `extractLargePayloadLink` is even called.

### E3 — Large-payload notification arrives while navigation is in progress (old behaviour)
**Scenario:** Not applicable in the new design — no navigation occurs.

### E4 — `appendMessage` called with an empty string or null
**Scenario:** Server returns an empty body.
**Mitigation:** The `!res.ok` check will have already thrown for a 204 (no content).  For a 200 with empty body, `JSON.parse('')` throws, so the `catch` leaves `content = ''`.  An empty string appended to the console renders as an empty `<span>` — harmless, but ugly.  Add a guard: `if (!text.trim()) throw new Error('empty response body')` before the JSON.parse attempt, routing to the error path instead.

### E5 — Two large-payload messages arrive in the same `newMessages` batch
**Scenario:** Server sends two notifications without any other messages between them (unusual but possible).
**Mitigation:** `isFetchingRef = true` is set and the loop `break`s after the first.  The second notification stays above the watermark.  On the next tick (after the first fetch resolves), `isFetchingRef = false` allows the second to be processed.  Correct sequential behaviour.

### E6 — User clears the console while fetch is in flight
**Scenario:** User clicks the 🗑️ clear button; `ws.clearMessages()` is called; `messages` becomes `[]`.
**Risk:** The fetch resolves; `appendMessage(prettyJSON)` is called.  The message appears in the now-empty console — correct.  Watermark was set to `newMessages[lastIdx].id + 1` which is larger than any current id; on the next render `messages` is empty so `newMessages` will be empty — no double processing.  Safe.

### E7 — Playground unmounts before fetch resolves (user navigates away manually)
**Scenario:** User clicks a different playground in the nav bar mid-fetch.
**Mitigation:** The unmount cleanup effect calls `abortRef.current?.abort()`.  The fetch rejects with `AbortError`; the `.catch` branch silently returns.  `appendMessage` is never called — correct, because the target slot's console is no longer visible.

### E8 — `wsPath` changes (impossible in current architecture)
**Scenario:** `wsPath` is a prop derived from `config.wsPath` which is static for the lifetime of a `Playground` instance.  This edge case does not exist.

### E9 — Backend returns a non-JSON content-type but valid JSON body
**Scenario:** `Content-Type: text/plain` with a JSON body.
**Mitigation:** `res.text()` is used (not `res.json()`), so content-type is irrelevant.  The `JSON.parse` attempt in the hook succeeds; content is pretty-printed.  Safe.

### E10 — `extractLargePayloadLink` returns a path with query parameters
**Scenario:** Hypothetical future server change: `/api/inspect/ws-1/input.body?v=2`.
**Mitigation:** The existing regex `\/api\/inspect\/[^\s]+` captures the full path including query string.  `fetch(apiPath)` would include the query string — correct.  The `lastSegment` filename derivation splits on `/` and pops, returning `input.body?v=2` — slightly ugly filename for the copy button, but functionally correct.  This is a pre-existing consideration, not introduced by this change.

### E11 — JSON-Path Playground not connected when user clicks ➡️
**Scenario:** User sees the fetched payload in the console and clicks ➡️ but has not connected the JSON-Path Playground.
**Mitigation:** `handleSendToJsonPath` in `Playground.tsx` already checks `slot.phase !== 'connected'` and shows an error toast.  **No change needed** — the guard is correctly placed in `Playground.tsx`, not in the hook.

### E12 — `handleSendToJsonPath` not wired (Minigraph Playground's own console)
**Scenario:** The large-payload notification arrives in the **Minigraph** Playground console (wsPath = `/ws/graph/playground`).
**Check in `Playground.tsx`:**
```tsx
onSendToJsonPath={jsonPathConfig && wsPath !== jsonPathConfig.wsPath ? handleSendToJsonPath : undefined}
```
When `wsPath === jsonPathConfig.wsPath` (i.e., we are already on the JSON-Path
Playground), `onSendToJsonPath` is `undefined`, so the ➡️ button is not
rendered — correct.  When we are on the Minigraph Playground
(`wsPath !== jsonPathConfig.wsPath`), the button is rendered — correct.

### E13 — `MAX_ITEMS` console buffer overflow
**Scenario:** Console is near the 200-item limit; `appendMessage` for the fetched payload causes the oldest message (possibly the large-payload notification itself) to be evicted via `msgs.shift()`.
**Analysis:** The notification row and the fetched payload row are two separate messages.  If the console is at 199 items when the notification arrives, the notification is at item 200 (max).  The fetched payload is appended as item 201 → the oldest item is evicted.  This is correct buffer behaviour — no special handling needed.  The amber notification row may disappear, but the JSON payload row remains, which is the more valuable information.

### E14 — Fetch resolves after the console is cleared (`messages = []`) and then the effect runs
**Analysis:** The appended message lands in an empty console (see E6).  `watermarkRef.current` was set to `notificationId + 1` inside `.then()` before clearing `isFetchingRef`.  The empty `messages` array means `newMessages` is empty on the next tick — the watermark check short-circuits.  No issue.

### E15 — `appendMessage` internal dispatch timing vs. watermark update
**Scenario:** Between the synchronous `appendMessage(...)` call and the synchronous `watermarkRef.current = ...` assignment, React has NOT re-rendered yet (React batches state updates).  The watermark is advanced in the same synchronous continuation of the `.then()` handler — before any re-render.  Safe.

---

## 11. Open Questions (Deferred)

### Q1 — Loading indicator on the notification row
**Question:** Should the amber notification row show a spinner while the fetch
is in flight?

**Discussion:** This would require the hook to communicate in-progress state
back to the message row.  Since `ConsoleMessage` renders by `msg.raw` string
alone with no external state, this would require either:
- A context value exposed from `useLargePayloadDownload` (adds coupling), or
- Replacing the notification row's raw string with a modified version that
  contains a marker (mutates shared state — fragile).

**Recommendation:** Defer.  The `addToast('Fetching…', 'info')` provides
sufficient feedback.  The toast is transient and auto-dismisses; by the time
the user reads the notification row, the fetch will typically have resolved.

### Q2 — Collapse/expand state persistence across re-renders
**Question:** `JsonView` renders collapsed at depth < 2 by default.  If the
user expands nodes and then the console re-renders (e.g. new message arrives),
the expansion state resets.

**Discussion:** This is a pre-existing behaviour shared with small-payload
JSON messages.  Not introduced by this change.  Addressed if/when the JsonView
integration is upgraded to a stateful alternative.  Defer.

### Q3 — Size threshold feedback
**Question:** Should the console message for the fetched payload show the byte
size reported by the notification?

**Discussion:** The raw JSON string itself conveys its size implicitly.  The
notification row above it already shows the byte count.  Showing it again
would be redundant.  Defer as a potential UX polish item.
