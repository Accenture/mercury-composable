# SPEC — Mock-Data Upload Modal

**Feature:** When the server responds to a command like `upload mock data` with a
`"You may upload JSON payload -> POST /api/mock/<id>"` message, intercept that
message in the console and **automatically open a modal dialog** so the user
perceives the command itself as the upload action. The modal lets the user paste
a JSON payload and POST it to the provided URL.

**Status:** Implemented — file-upload extension added March 20 2026  
**Branch:** `feature/playground`  
**Last updated:** March 20 2026

---

## Table of Contents

1. [Motivation & Scope](#1-motivation--scope)
2. [Server Contract](#2-server-contract)
3. [UX Flow](#3-ux-flow)
4. [Architecture Overview](#4-architecture-overview)
5. [Layer-by-Layer Design](#5-layer-by-layer-design)
   - 5.1 [`messageParser.ts` — new predicate & extractor](#51-messageparserts--new-predicate--extractor)
   - 5.2 [`useAutoMockUpload` hook — auto-open trigger](#52-useautomockupload-hook--auto-open-trigger)
   - 5.3 [`ConsoleMessage.tsx` — upload-link row treatment](#53-consolemessagetsx--upload-link-row-treatment)
   - 5.4 [`MockUploadModal` component](#54-mockuploadmodal-component)
   - 5.5 [`useMockUpload` hook — fetch lifecycle](#55-usemockupload-hook--fetch-lifecycle)
   - 5.6 [`Playground.tsx` — wiring](#56-playgroundtsx--wiring)
6. [CSS & Theming](#6-css--theming)
7. [Accessibility](#7-accessibility)
8. [Error Handling](#8-error-handling)
9. [Resolved Decisions](#9-resolved-decisions)
10. [Files Changed / Created](#10-files-changed--created)
11. [What Is Explicitly Out of Scope](#11-what-is-explicitly-out-of-scope)

---

## 1. Motivation & Scope

### The analogous existing feature

The **large-payload download** flow (`useLargePayloadDownload`) detects a pattern in
the message stream, takes autonomous action (a `GET` fetch), and surfaces the result
back in the console — all without any new UI widget.

This feature is the **upload mirror**: the server emits an upload invitation in the
stream, we detect it, and instead of acting autonomously we need user-supplied JSON
data. A modal is the right affordance because:

- The Minigraph playground intentionally has **no payload editor tab** (its right
  panel shows `preview`, `graph`, `graph-data`). There is nowhere else to put a
  multi-line text input.
- The payload is ephemeral and context-specific to the upload invitation. Writing it
  to localStorage (as the JSON-Path playground does) would be misleading.
- The action is driven by a specific server response, not a standing toolbar button,
  so a transient overlay is the natural fit.

### Scope

| In scope | Out of scope |
|---|---|
| Detecting the `POST /api/mock/<id>` pattern | Detecting other arbitrary `POST` patterns |
| Opening a modal with a textarea | Adding a payload tab to the Minigraph playground |
| Validating JSON before sending | XML upload support (deferred — see §9.3) |
| `POST`-ing the payload and surfacing the result | Retry logic beyond a single attempt |
| Accessible, keyboard-navigable modal | Full i18n / localisation |
| Drag-and-drop a `.json` file onto the drop zone | Uploading non-JSON file types |
| "Browse file…" button to open the system file-picker | Multi-file selection |
| Client-side file validation (extension + JSON parse check) | Server-side file validation feedback beyond HTTP status |

---

## 2. Server Contract

### Trigger message pattern

```
You may upload JSON payload -> POST /api/mock/{id}
```

The `{id}` segment is an alphanumeric + hyphen token (same shape as websocket session
ids elsewhere in the app, e.g. `ws-417669-24`).

**Regex** (new function `extractMockUploadPath` in `messageParser.ts`):

```typescript
const match = raw.match(/You may upload .*?->\s*POST\s+(\/api\/mock\/[\w-]+)/i);
```

> **Note on pattern breadth:** The regex is intentionally anchored to `/api/mock/`
> (not a generic POST capture) to avoid false positives from future unrelated
> messages. The case-insensitive flag handles any minor casing variation the server
> might introduce.

### Expected HTTP exchange

```
POST {uploadPath}
Content-Type: application/json

{ …user payload… }
```

The server is expected to return `2xx` on success and a non-`2xx` status on failure.

On **failure**, the response body is included in the inline error banner and toast:
`"Upload failed: HTTP 400 — <server response body>"`.

On **success**, the response body is read (to drain the connection) but is **not
surfaced** in the console or toast. The toast always shows the hardcoded message
`'Mock data uploaded successfully ✓'` for consistency, regardless of what the
server returns. `_responseBody` / `responseBody` is available in `handleUploadSuccess`
in `Playground.tsx` but intentionally unused (see §5.6).

---

## 3. UX Flow

```
1. User types: upload mock data
   Console echoes: > upload mock data

2. Server responds:
   "You may upload JSON payload -> POST /api/mock/ws-417669-24"

3. useAutoMockUpload detects the new message (watermark-guarded, same pattern
   as useAutoGraphRefresh / useAutoMarkdownPin).
   → Calls onOpenModal("/api/mock/ws-417669-24") automatically.
   → Modal opens immediately, focused on the textarea.
   The user perceives the upload command itself as having triggered the dialog —
   no extra click required.

4. Meanwhile, ConsoleMessage renders the invitation row with:
   - ⬆️  icon (amber accent — mirrors the ⬇️ for large-payload download)
   - Amber row highlight
   - An "⬆️ Upload JSON…" re-open button visible on hover/focus
     (This is a RE-OPEN affordance for after the modal is dismissed, not
      the primary trigger.  The primary trigger is the auto-open in step 3.)

5. Modal contents:
   ┌─────────────────────────────────────────────────────┐
   │  ⬆️ Upload Mock Data                            ✕  │
   │  POST /api/mock/ws-417669-24                        │
   ├─────────────────────────────────────────────────────┤
   │  📂  Drop a .json file here                         │
   │          — or —                                     │
   │          [Browse file…]                             │
   │                                                     │
   │  JSON Payload                                       │
   │  ┌───────────────────────────────────────────────┐  │
   │  │ {                                             │  │
   │  │   "key": "value"                              │  │
   │  │ }                                             │  │
   │  └───────────────────────────────────────────────┘  │
   │  ⚠️ Invalid JSON — check syntax                     │
   │  ⌘+Enter to upload                                  │
   │                                                     │
   │  [Format]               [Cancel]  [Upload ▶]        │
   └─────────────────────────────────────────────────────┘

6. User provides JSON via one of three methods:

   **a. Type / paste** directly into the textarea.

   **b. Drag-and-drop a `.json` file** onto the drop zone:
      - Drop zone gains an amber active-drag highlight while a file is held over it.
      - On drop: file extension and MIME type are validated client-side.
        If invalid → `fileError` banner shown below the drop zone; textarea unchanged.
        If valid   → file is read as UTF-8 text, validated as JSON object/array
                     via `tryParseJSON`, pretty-printed via `formatJSON`, and loaded
                     into the textarea. Focus returns to the textarea.

   **c. Click "Browse file…"** to open the system file-picker (accepts `.json` only).
      Same validation and load path as drag-and-drop.

   Inline JSON validation runs on every textarea keystroke
   (`tryParseJSON` — JSON-only, not `validatePayload` which also accepts XML).
   "Format" button pretty-prints valid JSON in-place (reuses `formatJSON`).
   Ctrl+Enter / Cmd+Enter submits without leaving the textarea.

7. User clicks "Upload ▶" (or presses Ctrl+Enter / Cmd+Enter).
   → Button shows spinner / disabled state while the fetch is in-flight.
   → Modal stays open until the response arrives.

8a. Success (2xx):
    → Modal closes automatically.
    → Toast: "Mock data uploaded successfully ✓"  (success, 3 s auto-dismiss)
    → Console row gains a ✅ badge (persists for the session).

8b. Error (non-2xx or network failure):
    → Modal stays open.
    → Inline error banner below the textarea:
      "Upload failed: HTTP 400 — <server response body>"
    → Toast: "Upload failed: …"  (error)
    → User can edit and retry, or Cancel.

9. Pressing Escape or clicking the backdrop closes the modal (same as Cancel).
   Any in-flight fetch is aborted via AbortController.
   The "⬆️ Upload JSON…" button on the console row remains available to
   re-open the modal for the same endpoint.
```

---

## 4. Architecture Overview

```
messageParser.ts
  extractMockUploadPath(raw)  ← new
  isMockUploadMessage(raw)    ← new

useAutoMockUpload              ← new hook (message-stream watcher, auto-open)
  watches messages for isMockUploadMessage
  calls onOpenModal(path) when a new invitation arrives
  follows watermark + disconnect-guard pattern of useAutoGraphRefresh

ConsoleMessage.tsx
  detects isMockUploadMessage → renders ⬆️ row + "⬆️ Upload JSON…" RE-OPEN button
  onUploadMockData?: (uploadPath: string) => void  ← new prop (re-open)

MockUploadModal/
  MockUploadModal.tsx          ← new component
  MockUploadModal.module.css   ← new styles

useMockUpload.ts               ← new hook (owns the fetch + abort logic)

Playground.tsx
  useState: modalUploadPath (string | null)
  handleOpenUploadModal(path)  → sets modalUploadPath
  handleCloseUploadModal()     → clears modalUploadPath
  useAutoMockUpload wired here → auto-opens on new invitation message
  passes onUploadMockData={handleOpenUploadModal} down for the re-open button:
    LeftPanel → Console → ConsoleMessage
  renders <MockUploadModal> when modalUploadPath !== null
```

**Two-trigger, one modal:**  The modal is opened either (a) automatically by
`useAutoMockUpload` when the server message arrives, or (b) manually by the
user clicking the "⬆️ Upload JSON…" re-open button on the console row. Both
paths call the same `handleOpenUploadModal(path)` callback, so the modal
implementation is unaffected by how it was opened.

The pattern is deliberately **component-local**: the modal path lives in `Playground`
state (not context, not localStorage), matching the same scoping decision used for
`pinnedGraphPath` and `pinnedMessageId`.

---

## 5. Layer-by-Layer Design

### 5.1 `messageParser.ts` — new predicate & extractor

Add two exported functions after the existing `extractUploadPath` block:

```typescript
/**
 * Extracts the POST path from a mock-data upload invitation:
 *   "You may upload JSON payload -> POST /api/mock/{id}"
 *
 * Returns the path (e.g. "/api/mock/ws-417669-24") or null.
 */
export function extractMockUploadPath(raw: string): string | null {
  const match = raw.match(/You may upload .*?->\s*POST\s+(\/api\/mock\/[\w-]+)/i);
  return match ? match[1] : null;
}

/**
 * Returns true when a raw WebSocket message is a mock-data upload invitation.
 * Used by useAutoMockUpload (auto-open) and ConsoleMessage.tsx (re-open button).
 */
export function isMockUploadMessage(raw: string): boolean {
  return extractMockUploadPath(raw) !== null;
}
```

**Why a separate extractor instead of reusing `extractUploadPath`?**  
`extractUploadPath` matches `/api/json/content/{id}` — the JSON-Path playground's
two-step upload handshake. The new path is `/api/mock/{id}`. Keeping them separate
maintains a clean separation of the two server protocols and avoids a fragile
shared regex.

#### Cross-hook and cross-component interaction: two required guards

Introducing `isMockUploadMessage` requires **two separate fixes** — one in a hook and one in a component. Both must be implemented; missing either one causes a different bug.

**Fix A — `useAutoMarkdownPin.ts`:** The upload invitation is plain text, so `isMarkdownCandidate` returns `true` for it. If `waitingForResponseRef` is armed (e.g. the user sent `help` and then quickly ran `upload mock data` before the help response arrived), `isPinnableResponse` would steal the invitation and pin it to the Developer Guides tab.

Add `isMockUploadMessage` as an exclusion inside `isPinnableResponse` in `useAutoMarkdownPin.ts`:

```typescript
// BEFORE (in useAutoMarkdownPin.ts):
function isPinnableResponse(raw: string): boolean {
  if (raw.startsWith('> ')) return false;
  if (isGraphLinkMessage(raw)) return false;
  return isMarkdownCandidate(raw);
}

// AFTER:
function isPinnableResponse(raw: string): boolean {
  if (raw.startsWith('> ')) return false;
  if (isGraphLinkMessage(raw)) return false;
  if (isMockUploadMessage(raw)) return false;   // ← new: don't steal upload invitations
  return isMarkdownCandidate(raw);
}
```

This file (`useAutoMarkdownPin.ts`) must be added to §10 (Files Changed).

**Fix B — `ConsoleMessage.tsx`:** The same plain-text nature of the invitation means `isMarkdownCandidate(message)` returns `true`, which would cause the existing `isPinnable` derivation to add `role="button"` and `onClick` to the invitation row — alongside the `canUploadMock` re-open button — creating a nested interactive element that violates WCAG. This fix is documented in detail in §5.3. Both fixes are required; §5.3 must not be treated as optional.

> **For implementors reading §5.1 in isolation:** do not stop after adding the `useAutoMarkdownPin` guard. See §5.3 for the mandatory `isPinnable` fix in `ConsoleMessage.tsx`.

---

### 5.2 `useAutoMockUpload` hook — auto-open trigger

**Location:** `src/hooks/useAutoMockUpload.ts`

Follows the **identical pattern** to `useAutoMarkdownPin` and `useAutoGraphRefresh`:
message-ID watermark set at mount, disconnect guard, main effect scanning only
new messages.

```typescript
export interface UseAutoMockUploadOptions {
  messages:    { id: number; raw: string }[];
  connected:   boolean;
  onOpenModal: (uploadPath: string) => void;
}
```

#### Implementation outline

```typescript
import { useEffect, useRef } from 'react';
import { extractMockUploadPath, isMockUploadMessage } from '../utils/messageParser';

export interface UseAutoMockUploadOptions {
  messages:    { id: number; raw: string }[];
  /**
   * Included for interface symmetry with the other automation hooks and for
   * future extensibility (e.g. disabling the re-open button when disconnected).
   * No disconnect-guard effect body is needed in this hook — see key decisions below.
   */
  connected:   boolean;
  onOpenModal: (uploadPath: string) => void;
}

export function useAutoMockUpload({
  messages,
  connected: _connected,   // accepted but not read in any effect body — see decisions
  onOpenModal,
}: UseAutoMockUploadOptions): void {

  const watermarkRef = useRef<number>(-1);

  // ── Set watermark at mount — never replay history ─────────────────────────
  // MUST be declared before the main effect so React fires it first on the
  // initial render, matching the ordering guarantee used by useLargePayloadDownload
  // and useAutoGraphRefresh.  If the main effect ran first on mount, watermarkRef
  // would be -1 and every existing message would be scanned, potentially
  // auto-opening the modal for a stale invitation from a previous session.
  useEffect(() => {
    if (messages.length > 0) {
      watermarkRef.current = messages[messages.length - 1].id;
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Main effect ───────────────────────────────────────────────────────────
  useEffect(() => {
    if (messages.length === 0) return;

    const newMessages = messages.filter(m => m.id > watermarkRef.current);
    if (newMessages.length === 0) return;

    watermarkRef.current = messages[messages.length - 1].id;

    for (const msg of newMessages) {
      const path = extractMockUploadPath(msg.raw);
      if (!path) continue;

      // Open the modal for the first invitation found in this batch.
      // If multiple invitations arrive in one tick (unlikely), only the
      // first is acted upon — consistent with the "break on first match"
      // pattern used by useLargePayloadDownload.
      onOpenModal(path);
      break;
    }
  }, [messages, onOpenModal]);
}
```

#### Key design decisions

| Decision | Rationale |
|---|---|
| No `waitingForRef` flag | Unlike `useAutoGraphRefresh` there is no two-step server handshake. The invitation message itself contains the complete target URL — open immediately. |
| Watermark **not** reset on disconnect | The hook has no pending-wait flag to clear. Resetting the watermark to `-1` on disconnect would cause old invitation messages still in the message store to be replayed as "new" on reconnect, auto-opening the modal for a stale endpoint. The watermark stays at its current value; the server sends a fresh invitation for each new session. |
| `connected` accepted but not read | `connected` is destructured with an `_connected` alias (TypeScript unused-param suppression). It is accepted in the interface for symmetry with the other automation hooks and future extensibility (e.g. disabling the re-open button when disconnected). No effect body reads or depends on it. |
| `onOpenModal` is the only callback | The hook has no knowledge of the modal or `Playground` state — it only calls a callback. This keeps the hook unit-testable and the Playground the single source of truth for modal open/close. |
| Break on first match | Consistent with `useLargePayloadDownload`. Prevents two modals from stacking if the server sends two invitations in one message batch. |

---

### 5.3 `ConsoleMessage.tsx` — upload-link row treatment

The button on this row is a **re-open** affordance. The modal will already be open
(auto-opened by `useAutoMockUpload`) when the row first renders — the button
becomes useful only after the user has dismissed the modal and wants to return
to the same upload endpoint.

#### New props (exact additions to `ConsoleMessageProps`)

```typescript
/**
 * When provided, a "⬆️ Upload JSON…" re-open button appears on hover for
 * mock-upload invitation rows. Called with the extracted POST path.
 * Only rendered when isMockUploadMessage(message) is true.
 */
onUploadMockData?:      (uploadPath: string) => void;
/**
 * Set of POST paths for which a mock upload has succeeded this session.
 * Used to render the ✅ badge on fulfilled invitation rows.
 */
successfulUploadPaths?: Set<string>;
```

> **Note:** Both props are optional (`?`) to match the existing convention for
> feature-specific callbacks (`onSendToJsonPath?`, `onPin?`).
> `Console.tsx` and `LeftPanel.tsx` pass them through unconditionally when provided
> by `Playground`. The exact prop additions for those components:
>
> ```typescript
> // Console.tsx — add to ConsoleProps:
> onUploadMockData?:      (uploadPath: string) => void;
> successfulUploadPaths?: Set<string>;
>
> // LeftPanel.tsx — add to LeftPanelProps:
> onUploadMockData?:      (uploadPath: string) => void;
> successfulUploadPaths?: Set<string>;
> ```

#### New derived flags (alongside existing `isGraphLink`, `isLargePayload`)

```typescript
const isMockUpload   = isMockUploadMessage(message);
const mockUploadPath = isMockUpload ? extractMockUploadPath(message) : null;
const canUploadMock  = !!onUploadMockData && isMockUpload && mockUploadPath !== null;
```

> **⚠️ Critical: `isPinnable` guard**  
> `isMockUploadMessage` returns `true` for a plain-text string, so
> `isMarkdownCandidate(message)` is also `true` for the invitation row. Without an
> explicit exclusion, the existing `isPinnable` derivation would assign
> `role="button"`, `onClick`, and `onKeyDown` to the row div *in addition to* the
> `canUploadMock` re-open button — creating a nested interactive element that
> violates WCAG and the spec's own accessibility rule (§7).
>
> The `isPinnable` line **must** include `&& !isMockUpload`:

```typescript
// BEFORE (existing):
const isPinnable = !!onPin && (!isGraphLink ? isMarkdownCandidate(message) : true);

// AFTER (with mock-upload exclusion):
const isPinnable = !!onPin && !isMockUpload && (!isGraphLink ? isMarkdownCandidate(message) : true);
```

> This is consistent with `isLargePayload` rows, which are also non-pinnable
> (they have their own affordance — the browser download — and no `role="button"`).
> The mock-upload row's sole interactive element is the `canUploadMock` button.

#### Icon

```typescript
// In the icon span — isMockUpload checked before isLargePayload:
{isMockUpload ? '⬆️' : isLargePayload ? '⬇️' : isGraphLink ? '🕸️' : icon}
```

#### CSS class on the row

```typescript
isMockUpload ? styles.consoleMessageMockUpload : '',
```

#### "⬆️ Upload JSON…" re-open button (mirrors the existing ➡️ button)

```tsx
{canUploadMock && (
  <button
    className={styles.uploadMockButton}
    onClick={(e) => { e.stopPropagation(); onUploadMockData!(mockUploadPath!); }}
    onKeyDown={(e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault(); e.stopPropagation();
        onUploadMockData!(mockUploadPath!);
      }
    }}
    title="Re-open upload dialog for this endpoint"
    aria-label={`Re-open upload dialog for ${mockUploadPath}`}
    tabIndex={0}
  >
    ⬆️ Upload JSON…
  </button>
)}
```

The row itself is **not** given `role="button"` — the explicit labelled button is
the sole interactive element, avoiding the nested-interactive-element accessibility
conflict. This is consistent with the large-payload `⬇️` row (which also has no
row-level click handler).

#### Success badge

After a successful upload, `Playground` passes `successfulUploadPaths: Set<string>`
(keyed by upload path) down the prop chain. When `successfulUploadPaths.has(mockUploadPath)` is true, a `✅` span is rendered at the end of the row's message
text. This is session-only (resets on `clearMessages`).

> **Note on the `✅` glyph:** This intentionally reuses the same emoji as the
> copy-button's "copied" confirmation state (`copyButtonCopied` in
> `ConsoleMessage`). The two are semantically distinct (one confirms a clipboard
> copy action, the other marks a fulfilled upload invitation) but visually
> identical. This is intentional — no new icon is introduced.

---

### 5.4 `MockUploadModal` component

**Location:** `src/components/MockUploadModal/MockUploadModal.tsx`  
**Styles:** `src/components/MockUploadModal/MockUploadModal.module.css`

#### Required imports

```typescript
import { useState, useEffect, useRef, useCallback } from 'react';
import styles from './MockUploadModal.module.css';
import { tryParseJSON } from '../../utils/messageParser';
import { formatJSON } from '../../utils/validators';
import { useMockUpload } from '../../hooks/useMockUpload';
```

#### Props

```typescript
interface MockUploadModalProps {
  /** The POST path extracted from the server message, e.g. "/api/mock/ws-417669-24" */
  uploadPath: string;
  /** Called when the user cancels or the upload succeeds (modal self-closes on success). */
  onClose: () => void;
  /** Called with the raw response body text on a successful upload. */
  onSuccess: (responseBody: string) => void;
  /** Called with an error message on failure — modal stays open. */
  onError: (errorMessage: string) => void;
}
```

#### Internal state

```typescript
const [json,        setJson]        = useState('');
const [uploadError, setUploadError] = useState<string | null>(null);
const [fileError,   setFileError]   = useState<string | null>(null);  // ← new
const [isDragOver,  setIsDragOver]  = useState(false);                // ← new
```

`fileError` is separate from `uploadError`:
- `fileError` — set by the client-side file validator (`validateFileType` /
  `readFileAsText` / `tryParseJSON`) when a dragged or browsed file is rejected.
  Displayed below the drop zone with `var(--warning-color)` (amber — not a hard
  failure, just "try a different file"). Cleared when the user types in the textarea
  or successfully loads another file.
- `uploadError` — set by the `useMockUpload` `onError` callback when the HTTP
  request fails. Displayed in the red error banner below the textarea.

`isDragOver` drives the `.dropZoneActive` CSS class on the drop zone div —
toggled by the `dragover` / `dragleave` events.

> **Error ownership:**  
> `isUploading` is **not** local state — it is read from `useMockUpload`'s return
> value (see §5.5). `uploadError` is local state, set by the component's own
> `onError` handler that wraps the `useMockUpload` `onError` callback:
>
> ```typescript
> const { isUploading, upload, cancel } = useMockUpload({
>   uploadPath,
>   json,
>   onSuccess,                     // prop — forwarded straight to Playground
>   onError: (msg) => {
>     setUploadError(msg);          // ← set inline banner (local state)
>     onError(msg);                 // ← call prop so Playground fires a toast
>   },
> });
> ```
>
> `uploadError` is cleared at the start of each new upload attempt:
> ```typescript
> const handleUpload = () => {
>   setUploadError(null);
>   upload();
> };
> ```
> This ensures a stale error from a previous attempt is not shown while a retry
> is in-flight.

#### File loading — `loadFile(file: File)` (shared by drop and picker)

```typescript
/** Read a File as text, resolving with the string or rejecting with an Error. */
function readFileAsText(file: File): Promise<string>

/**
 * Validate that a dropped / selected file is acceptable.
 * Accepts .json extension OR application/json MIME type.
 * Returns null on success, or a human-readable error string on failure.
 */
function validateFileType(file: File): string | null
```

Both helper functions are module-level (outside the component) — they are pure
and have no dependency on React state. `loadFile` is a `useCallback` inside the
component that calls them in sequence:

```
validateFileType(file)
  → error? → setFileError + return
  → ok
readFileAsText(file)
  → error? → setFileError + return
  → ok
tryParseJSON(text)
  → not JSON? → setFileError(`"${file.name}" contains invalid JSON.`) + return
  → ok
setJson(formatJSON(text))   ← pretty-print on load
textareaRef.current?.focus()
```

Key decisions:
- File is pretty-printed on load (`formatJSON`) so the user sees clean output
  without having to click Format manually.
- Both drag-and-drop and the file-picker share the same `loadFile` path — no
  duplicated logic.
- The file input is reset (`e.target.value = ''`) after each selection so the
  same file can be re-selected after a fix.

#### Drop zone

```tsx
<div
  className={`${styles.dropZone} ${isDragOver ? styles.dropZoneActive : ''}`}
  onDragOver={handleDragOver}
  onDragLeave={handleDragLeave}
  onDrop={handleDrop}
  aria-label="Drop a JSON file here"
>
  <span className={styles.dropZoneIcon}>📂</span>
  <span className={styles.dropZoneText}>Drop a <code>.json</code> file here</span>
  <span className={styles.dropZoneOr}>— or —</span>
  <input
    ref={fileInputRef}
    type="file"
    accept=".json,application/json"
    className={styles.fileInputHidden}
    aria-hidden="true"
    tabIndex={-1}
    onChange={handleFileInputChange}
  />
  <button type="button" className={styles.browseButton}
          onClick={() => fileInputRef.current?.click()}
          disabled={isUploading}>
    Browse file…
  </button>
</div>
```

- The `<input type="file">` is hidden (`display: none`) and triggered
  programmatically from the "Browse file…" button via `fileInputRef.current?.click()`.
  This gives full style control over the button while retaining native file-picker
  behaviour (including keyboard-accessible activation via Enter/Space on the button).
- `dragLeave` is guarded: `setIsDragOver(false)` only fires when leaving the drop
  zone container itself (checking `relatedTarget` against `e.currentTarget.contains()`),
  not when moving between child elements inside the zone.

Uses `tryParseJSON(json)` from `messageParser.ts` directly — **not** `validatePayload`
from `validators.ts`. This is intentional: `validatePayload` accepts both JSON and XML
(per the JSON-Path playground's requirements), but the mock-upload endpoint is
JSON-only. Using `tryParseJSON` gives a clean boolean `isJSON` result without
a false-positive for valid XML.

```typescript
const jsonResult  = tryParseJSON(json);
const isValidJson = jsonResult.isJSON;
const canSubmit   = isValidJson && !isUploading && json.trim() !== '';
```

> **Note:** `tryParseJSON` returns `isJSON: false` for JSON primitives (`42`,
> `"hello"`, `true`, `null`). These are **intentionally blocked** by the
> `isValidJson` gate — the mock endpoint expects an object or array, and the server
> would reject a bare primitive with a `4xx` error anyway. This is consistent with
> the existing JSON-Path textarea behaviour.

- `!isValidJson && json.trim() !== ''` → show inline validation message (`role="status"`):
  `"⚠️ Invalid JSON — check syntax"`.
- "Format" button calls `formatJSON(json)` (imported from `../../utils/validators`) and replaces the textarea value.
  It is **only enabled when `isValidJson` is true** — `formatJSON` silently returns
  the original string on invalid JSON (no-op), so gating on validity prevents a
  confusing "Format clicked, nothing changed" experience.
- "Upload ▶" button is disabled while `!isValidJson || isUploading || json.trim() === ''`.

#### Keyboard shortcut

`Ctrl+Enter` (Windows/Linux) and `Cmd+Enter` (macOS) submit the form while focus
is in the textarea. Plain `Enter` inserts a newline (standard textarea behaviour —
essential for pasting multi-line JSON). The `onKeyDown` handler:

```typescript
const handleTextareaKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault();
    if (canSubmit) handleUpload();
  }
};
```

A small hint line below the textarea reads:
`Ctrl+Enter to upload` (or `⌘+Enter to upload` on macOS).

`isMac` is derived with a modern-first, legacy-fallback approach:

```typescript
// navigator.platform is deprecated but still widely supported; use
// navigator.userAgentData?.platform (available in Chromium-based browsers)
// as the preferred source and fall back for Firefox / Safari.
const isMac =
  (navigator.userAgentData?.platform ?? navigator.platform)
    .toLowerCase()
    .includes('mac');
```

This is a single expression at the top of the component; no hook is needed.

#### Focus management

- **On open:** `useEffect` calls `textareaRef.current?.focus()` after the `<dialog>`
  is shown (after `dialogRef.current.showModal()`).
- **On close:** Focus is returned to the element that triggered the open.
  This responsibility lives entirely in **`Playground.tsx`** — `MockUploadModal`
  has no `triggerRef` prop and does not call `.focus()` itself. The modal simply
  calls its `onClose` prop; `Playground`'s `handleCloseUploadModal` (and
  `handleUploadSuccess`) perform the focus restore after the dialog has been
  removed from the DOM.

  > **Why Playground owns focus-return:**  
  > This is consistent with how all other side-effect callbacks in the codebase
  > flow upward to `Playground` — the component stays a pure renderer.  
  > Passing a `triggerRef` down as a prop would create a back-channel from
  > `Playground` state into the component's close handler, coupling them in a
  > way that is harder to test and reason about.

```typescript
// In Playground.tsx — capture the trigger BEFORE setting modal state:
const modalTriggerRef = useRef<HTMLElement | null>(null);

const handleOpenUploadModal = useCallback((path: string) => {
  modalTriggerRef.current = document.activeElement as HTMLElement;
  setModalUploadPath(path);
}, []);

// Both close paths restore focus via setTimeout (dialog must be gone first):
const handleCloseUploadModal = useCallback(() => {
  setModalUploadPath(null);
  setTimeout(() => modalTriggerRef.current?.focus(), 0);
}, []);

const handleUploadSuccess = useCallback((_responseBody: string) => {
  setSuccessfulUploadPaths(prev => new Set([...prev, modalUploadPath!]));
  setModalUploadPath(null);
  setTimeout(() => modalTriggerRef.current?.focus(), 0);
  addToast('Mock data uploaded successfully ✓', 'success');
}, [modalUploadPath, addToast]);
```

```tsx
// MockUploadModal — handleClose simply delegates to the prop:
const handleClose = () => {
  onClose();   // Playground restores focus; the component does nothing extra.
};
```

#### Layout sketch

```tsx
<dialog ref={dialogRef} aria-modal="true" aria-labelledby="modal-title"
        onClick={handleBackdropClick} onCancel={handleClose}>
  <div className={styles.modalHeader}>
    <h2 id="modal-title" className={styles.modalTitle}>Upload JSON Payload</h2>
    <code className={styles.modalPath}>{uploadPath}</code>
    <button className={styles.closeButton} aria-label="Close" onClick={handleClose}>✕</button>
  </div>
  <div className={styles.modalBody}>
    <textarea
      ref={textareaRef}
      className={styles.textarea}
      value={json}
      onChange={e => setJson(e.target.value)}
      onKeyDown={handleTextareaKeyDown}
      placeholder={'{\n  "key": "value"\n}'}
      aria-label="JSON payload"
      aria-describedby={uploadError ? 'upload-error' : undefined}
      spellCheck={false}
    />
    {uploadError && (
      <p id="upload-error" className={styles.errorBanner} role="alert">
        ❌ {uploadError}
      </p>
    )}
    {!isValidJson && json.trim() !== '' && (
      <p className={styles.validationError} role="status">
        ⚠️ Invalid JSON — check syntax
      </p>
    )}
    <p className={styles.keyboardHint}>
      {isMac ? '⌘+Enter' : 'Ctrl+Enter'} to upload
    </p>
  </div>
  <div className={styles.modalFooter}>
    <button className={styles.formatButton} onClick={handleFormat}
      disabled={!isValidJson || json.trim() === ''}>
      Format
    </button>
    <div className={styles.footerActions}>
      <button className={styles.cancelButton} onClick={handleClose}>Cancel</button>
      <button className={styles.uploadButton} onClick={handleUpload}
        disabled={!canSubmit}
        aria-busy={isUploading}
        aria-label={isUploading ? 'Uploading…' : `Upload to ${uploadPath}`}>
        {isUploading ? '⏳ Uploading…' : 'Upload ▶'}
      </button>
    </div>
  </div>
</dialog>
```

> **Layout sketch notes for implementors:**
> - All `class=` attributes are JSX `className=` using CSS Module references
>   (`styles.<ruleName>`), as shown above. The pseudocode has been updated to reflect this.
> - Inline validation uses `!isValidJson` (derived from `tryParseJSON` — not a `validation`
>   object). The error string is hardcoded to `"⚠️ Invalid JSON — check syntax"` per §3 UX
>   Flow step 6. `validatePayload` / `ValidationResult` are **not** imported by this component.

#### Implementation notes

- **Native `<dialog>`** with `.showModal()` called via `useEffect` on mount gives
  the browser's built-in focus-trap and backdrop for free. **`.close()` is NOT
  called explicitly** — the component is unmounted (via Playground's
  `{modalUploadPath && ...}` conditional) which implicitly removes the dialog from
  the DOM; calling `.close()` after `onClose()` would produce a "dialog already
  closed" warning.
- **Backdrop click:** `dialog` click handler checks `e.target === dialogRef.current`
  to distinguish backdrop from inner content clicks; fires `handleClose`.
- **`onCancel`** event (fired by browser on Escape) calls `handleClose` →
  `onClose()` → `setModalUploadPath(null)` in Playground → component unmounts.
  Do **not** call `dialogRef.current.close()` in `handleClose` — the unmount
  handles it.
- **`isMac`** is derived from `(navigator.userAgentData?.platform ?? navigator.platform).toLowerCase().includes('mac')`. `navigator.platform` is deprecated; `userAgentData.platform` is preferred (Chromium) with `navigator.platform` as the Safari/Firefox fallback. A single expression at the top of the component; no new hook required.

---

### 5.5 `useMockUpload` hook — fetch lifecycle

**Location:** `src/hooks/useMockUpload.ts`

This hook owns the `fetch` lifecycle — keeping it out of the component keeps
`MockUploadModal` a pure renderer.

```typescript
export interface UseMockUploadOptions {
  uploadPath: string;
  json:       string;
  /** Called with the raw response body text on a 2xx response. */
  onSuccess:  (responseBody: string) => void;
  /** Called with a formatted error string on network failure or non-2xx. Modal stays open. */
  onError:    (message: string) => void;
}

export interface UseMockUploadReturn {
  isUploading: boolean;
  upload:      () => void;
  cancel:      () => void;
}
```

#### Implementation outline

```typescript
export function useMockUpload({
  uploadPath, json, onSuccess, onError,
}: UseMockUploadOptions): UseMockUploadReturn {

  // isUploading lifecycle:
  //   false  → initial / idle
  //   true   → set immediately before fetch starts (in `upload()`)
  //   false  → reset in .then() (success), .catch() (non-abort error), and
  //            the unmount cleanup below (abort-via-unmount path)
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const abortRef = useRef<AbortController | null>(null);

  const upload = useCallback(() => {
    abortRef.current?.abort();                         // cancel any previous attempt
    const controller = new AbortController();
    abortRef.current = controller;

    // Re-serialise through JSON.parse to normalise whitespace before sending.
    // Wrapped in try/catch to guard against the edge case where `json` state
    // changes between the component's isValidJson check and this callback firing
    // (e.g. a React batched state update in the same tick).  Matches the pattern
    // used in useWebSocket.ts for the JSON-Path playground upload.
    let body: string;
    try {
      body = JSON.stringify(JSON.parse(json));
    } catch {
      setIsUploading(false);
      onError('Invalid JSON — cannot send');
      return;
    }

    setIsUploading(true);                              // ← true before fetch

    fetch(uploadPath, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
      signal: controller.signal,
    })
      .then(async (res) => {
        // Always read the response body to drain the connection.
        // On 4xx/5xx it may contain a useful server error message.
        // On 2xx the body is passed to onSuccess — see note below.
        const responseBody = await res.text();
        if (!res.ok) throw new Error(`HTTP ${res.status}${responseBody ? ` — ${responseBody}` : ''}`);
        setIsUploading(false);                         // ← false on success
        abortRef.current = null;
        // responseBody is available here but Playground intentionally does not
        // surface it — the toast always shows the hardcoded
        // 'Mock data uploaded successfully ✓' message for consistency.
        onSuccess(responseBody);
      })
      .catch((err: Error) => {
        if (err.name === 'AbortError') return;
        setIsUploading(false);                         // ← false on error
        abortRef.current = null;
        onError(err.message);
      });
  }, [uploadPath, json, onSuccess, onError]);

  const cancel = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    setIsUploading(false);
  }, []);

  // ── Abort on unmount — PRIMARY abort mechanism ────────────────────────────
  // When the modal is dismissed (Cancel, Escape, backdrop click, or
  // auto-close on success), Playground sets modalUploadPath = null, which
  // unmounts the modal component, which unmounts this hook.  This cleanup
  // fires abortRef.current?.abort() — cancelling any in-flight fetch without
  // requiring the modal's handleClose to call cancel() explicitly.
  // `setIsUploading(false)` is also called here so that isUploading is
  // consistent even on the unmount-abort path — this prevents React Testing
  // Library tests from seeing an `act()` warning about state updates on an
  // unmounted component when asserting on this hook's state after unmount.
  // `cancel()` is exposed for supplementary use (e.g. a dedicated Cancel
  // button that needs to reset isUploading immediately in the same tick).
  useEffect(() => () => {
    abortRef.current?.abort();
    setIsUploading(false);
  }, []);

  return { isUploading, upload, cancel };
}
```

**Why a hook instead of inline `fetch` inside the component?**  
Consistent with the codebase principle: every side-effect lives in a hook.
`MockUploadModal` is a pure renderer; the hook is independently testable.

---

### 5.6 `Playground.tsx` — wiring

#### New imports (add to existing `Playground.tsx` import statements)

```typescript
// React — add useRef to the existing React import if not already present:
import { useState, useRef, useCallback } from 'react';   // useRef is the addition

// New components and hooks:
import { MockUploadModal } from '../components/MockUploadModal/MockUploadModal';
import { useAutoMockUpload } from '../hooks/useAutoMockUpload';
```

> **Note:** `isMockUploadMessage` and `extractMockUploadPath` are **not** imported
> in `Playground.tsx`. The path is extracted inside `useAutoMockUpload` and delivered
> to `Playground` as the `path` argument of the `onOpenModal` callback — `Playground`
> only ever receives the already-extracted string. Both functions are used directly by
> `ConsoleMessage.tsx` and `useAutoMarkdownPin.ts`, which import them from
> `messageParser.ts` themselves.

#### New state

```typescript
// Path extracted from the server's upload invitation.
// null = modal closed; non-null = modal open for that specific endpoint.
const [modalUploadPath, setModalUploadPath] = useState<string | null>(null);

// Capture the element that triggered the modal so focus can be restored on close.
const modalTriggerRef = useRef<HTMLElement | null>(null);

// POST paths of invitations that have been successfully fulfilled — drives ✅ badge.
const [successfulUploadPaths, setSuccessfulUploadPaths] =
  useState<Set<string>>(new Set());
```

#### New callbacks

```typescript
const handleOpenUploadModal = useCallback((path: string) => {
  // When called by useAutoMockUpload, activeElement is the command input
  // or the send button — restoring focus there after close is correct.
  // When called by the re-open button, activeElement is that button — also correct.
  modalTriggerRef.current = document.activeElement as HTMLElement;
  setModalUploadPath(path);
}, []);

const handleCloseUploadModal = useCallback(() => {
  setModalUploadPath(null);
  // Restore focus to whatever triggered the open (next microtask, after
  // the dialog is removed from the DOM).
  setTimeout(() => modalTriggerRef.current?.focus(), 0);
}, []);

const handleUploadSuccess = useCallback((responseBody: string) => {
  // responseBody is available here but intentionally not appended to the console
  // or toast — the hardcoded message below is used for consistency across all
  // mock-upload invocations, regardless of what the server body contains.
  void responseBody;
  // `modalUploadPath` is read from the closure — no need to pass it as an
  // argument.  The hook already has `uploadPath` as a fixed input; the
  // caller (Playground) already knows the path from its own state.
  setSuccessfulUploadPaths(prev => new Set([...prev, modalUploadPath!]));
  setModalUploadPath(null);
  setTimeout(() => modalTriggerRef.current?.focus(), 0);
  addToast('Mock data uploaded successfully ✓', 'success');
}, [modalUploadPath, addToast]);

const handleUploadError = useCallback((errorMessage: string) => {
  // Modal stays open — error is displayed inline inside the modal.
  // Toast provides secondary feedback.
  addToast(`Upload failed: ${errorMessage}`, 'error');
}, [addToast]);
```

#### Auto-open hook (new, added after existing automation hooks)

```typescript
useAutoMockUpload({
  messages:    ws.messages,
  connected:   ws.connected,
  onOpenModal: handleOpenUploadModal,
});
```

This sits alongside `useAutoGraphRefresh`, `useAutoMarkdownPin`, and
`useLargePayloadDownload` in the hook-invocation block — consistent ordering.

#### Prop threading

`handleOpenUploadModal` (re-open) and `successfulUploadPaths` thread down all
four levels. All four components (`Playground`, `LeftPanel`, `Console`,
`ConsoleMessage`) require new props — none can be skipped:

```
Playground
  → LeftPanel  (onUploadMockData: (path: string) => void,
                successfulUploadPaths: Set<string>)
    → Console  (onUploadMockData: (path: string) => void,
                successfulUploadPaths: Set<string>)
      → ConsoleMessage (onUploadMockData?: (uploadPath: string) => void,
                        successfulUploadPaths?: Set<string>)
```

`ConsoleMessage` marks both props optional (`?`) to stay consistent with the
existing convention for feature-specific callbacks (`onSendToJsonPath?`,
`onPin?`). `Console` and `LeftPanel` pass them through unconditionally when
provided by `Playground`.

#### Modal rendering (after `<ToastContainer>` in the JSX return)

```tsx
{modalUploadPath && (
  <MockUploadModal
    uploadPath={modalUploadPath}
    onClose={handleCloseUploadModal}
    onSuccess={handleUploadSuccess}
    onError={handleUploadError}
  />
)}
```

#### `handleClearMessages` update

```typescript
const handleClearMessages = useCallback(() => {
  ws.clearMessages();
  setPinnedMessageId(null);
  setPinnedGraphPath(null);
  setGraphData(null);
  setModalUploadPath(null);             // ← close modal if open when console is cleared
  setSuccessfulUploadPaths(new Set());  // ← clear session-only badges
  // Note: useAutoMockUpload's internal watermarkRef is NOT reset here.
  // The hook's watermark is managed entirely within the hook (it resets on
  // disconnect, not on message clear).  Any new invitation posted after the
  // clear will have an ID above the current watermark and will be processed
  // correctly — no hook-level reset is needed or desired.
}, [ws.clearMessages, setGraphData]);
```

> **Dependency array note:** `useState` setters (`setPinnedMessageId`,
> `setPinnedGraphPath`, `setModalUploadPath`, `setSuccessfulUploadPaths`) are
> **deliberately omitted** from the dep array. React guarantees that `useState`
> setters are stable references that never change across renders, so including
> them would add noise without any correctness benefit. Only `ws.clearMessages`
> (from `useWebSocket`) and `setGraphData` (from `useGraphData`) are listed because
> they originate from custom hooks whose internal identities are not guaranteed to
> be stable by the React spec — though in practice both are `useCallback`-wrapped
> with empty dep arrays in their respective hooks.

---

## 6. CSS & Theming

### `Console.module.css` additions

> **⚠️ Grid column count update required — apply as one atomic edit**  
> The existing `.consoleMessage` rule uses a 5-column grid:
> ```css
> grid-template-columns: auto 1fr auto auto auto;
> /* columns:            icon  content  copy  send-to-json-path  timestamp */
> ```
> The `sendToJsonPathButton` is already in the grid as column 4 (added in a
> prior feature; the CSS comment was not updated at that time — the comment in
> the source currently reads `[icon] [message body] [copy button] [timestamp]`,
> which is stale). The new `.uploadMockButton` inserts a **sixth column** between
> `send-to-json-path` and `timestamp`. **Both the column count and the comment
> must be updated in the same edit** to avoid the comment becoming stale again
> immediately. Update the rule to:
> ```css
> grid-template-columns: auto 1fr auto auto auto auto;
> /* columns:            icon  content  copy  send-to-json-path  upload-mock  timestamp */
> ```
> Without this change, the new button will collapse into the timestamp column,
> causing layout misalignment on mock-upload rows.

```css
/* Mock-upload invitation row — amber accent (pairs with ⬇️ large-payload style) */
.consoleMessageMockUpload {
  background-color: rgba(245, 158, 11, 0.06);
  outline: 1px solid rgba(245, 158, 11, 0.25);
  border-radius: 4px;
}

/* "Upload JSON…" action button — amber accent on hover */
.uploadMockButton {
  appearance: none;
  cursor: pointer;
  font-size: 0.8rem;
  line-height: 1;
  align-self: start;
  font-weight: 600;
  background-color: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.25rem;
  padding: 0.2rem 0.5rem;
  color: rgba(255, 255, 255, 0.75);
  opacity: 0;
  transition: opacity 0.15s, background-color 0.15s, border-color 0.15s, color 0.15s;
  white-space: nowrap;
}

.consoleMessage:hover .uploadMockButton,
.consoleMessage:focus-within .uploadMockButton {
  opacity: 1;
}

.uploadMockButton:hover {
  background-color: rgba(245, 158, 11, 0.2);
  border-color: rgba(245, 158, 11, 0.6);
  color: #fcd34d;
}

.uploadMockButton:focus-visible {
  opacity: 1;
  outline: 2px solid #f59e0b;
  outline-offset: 2px;
}
```

### `MockUploadModal.module.css`

Follows the app's existing token vocabulary (`--bg-primary`, `--bg-secondary`,
`--border-color`, `--text-primary`, `--text-secondary`, `--primary-color`,
`--danger-color`, `--radius`, `--shadow-md`).

Key structural rules (abridged — see implementation for full source):

```css
/* Native dialog — browser supplies the backdrop */
.dialog { … width: min(560px, 94vw); max-height: 90vh; … }
.dialog::backdrop { background: rgba(0,0,0,0.5); backdrop-filter: blur(2px); }

/* ── Drop zone ── */
.dropZone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  padding: 1rem;
  border: 2px dashed var(--border-color);
  border-radius: var(--radius);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  user-select: none;   /* prevents text selection during drag */
  transition: border-color 0.15s, background-color 0.15s;
}

/* Active drag-over — amber accent to match the row highlight */
.dropZoneActive {
  border-color: rgba(245, 158, 11, 0.8);
  background: rgba(245, 158, 11, 0.06);
  color: var(--text-primary);
}

/* Hidden native <input type="file"> — triggered programmatically */
.fileInputHidden { display: none; }

/* Browse button — ghost secondary style */
.browseButton { … }
.browseButton:hover:not(:disabled) {
  background: rgba(245, 158, 11, 0.08);
  border-color: rgba(245, 158, 11, 0.6);
}

/* File-type / read error — warning (amber), not danger (red) */
.fileError { color: var(--warning-color); font-size: 0.8rem; }
```

> **⚠️ CSS token `--warning-color` — value correction (already applied)**  
> `src/index.css` has been updated from `#f5780b` (amber-600) to `#f59e0b`
> (amber-500) so `.fileError`, `.validationError`, and the row-accent rgba
> values all share the same amber shade.

```css
.modalBody { flex: 1 1 auto; overflow-y: auto; padding: 1rem 1.25rem; … }

.textarea {
  background: var(--bg-dark);
  color: var(--console-text);
  border: 1px solid var(--border-color);
  min-height: 180px;
  resize: vertical;
  font-family: 'Courier New', Courier, monospace;
}
.textarea:focus { border-color: var(--primary-color); box-shadow: var(--focus-ring); }

.validationError { color: var(--warning-color); font-size: 0.8rem; }
.errorBanner     { color: var(--danger-color); background: rgba(239,68,68,0.08); … }
```

---

## 7. Accessibility

| Requirement | Implementation |
|---|---|
| Focus trap inside modal | Native `<dialog>` provides this automatically |
| Initial focus | `useEffect` focuses the `<textarea>` after `.showModal()`. After a file is loaded via drop or browse, focus returns to the textarea via `textareaRef.current?.focus()`. |
| Focus return on close | `modalTriggerRef` in `Playground` captures `document.activeElement` before open; `.focus()` is restored inside `handleCloseUploadModal` and `handleUploadSuccess` (via `setTimeout` to allow the dialog to be removed from the DOM first). `MockUploadModal` itself has no `triggerRef` prop — focus management is exclusively `Playground`'s responsibility. |
| Backdrop click closes | `dialog` click handler with `e.target === dialogRef.current` guard |
| Escape closes | Native `cancel` event on `<dialog>` calls `handleClose` |
| Screen reader label | `aria-labelledby="modal-title"` + `aria-modal="true"` on `<dialog>` |
| Error announced immediately | `role="alert"` on the upload error banner and file error |
| Validation feedback | `role="status"` on the validation message |
| Upload button state | `aria-busy={isUploading}`; `aria-label` changes to "Uploading…" during fetch |
| Keyboard submit hint | Visible `Ctrl+Enter` / `⌘+Enter` hint below the textarea |
| Drop zone | Labelled with `aria-label="Drop a JSON file here"`. Not focusable itself — the "Browse file…" button inside it is the keyboard-accessible entry point. |
| Browse file button | Standard `<button>` element — fully keyboard accessible (Enter/Space); triggers the hidden `<input type="file">` programmatically. `disabled` during upload. |
| File error | `role="alert"` ensures it is announced by screen readers immediately when it appears (same as the upload error banner). |
| Console invitation row | **Not** given `role="button"` — the re-open button is the sole interactive element on the row, avoiding nested-interactive-element issues. |
| Auto-open screen reader announcement | The modal opening automatically is announced by the `<dialog>` focus shift; no extra live region needed |

---

## 8. Error Handling

| Scenario | Behaviour |
|---|---|
| Network error (fetch throws) | `onError` called; error banner shown in modal; modal stays open |
| HTTP 4xx / 5xx | Error includes status code + response body; same path as above |
| `AbortError` (user cancels / modal dismissed mid-flight) | Silently swallowed — no error shown |
| Invalid JSON in textarea | "Upload ▶" button disabled; inline validation message shown |
| Modal dismissed while upload in-flight | `useMockUpload` unmount cleanup aborts the fetch via `AbortController` |
| Messages cleared while modal is open | `handleClearMessages` calls `setModalUploadPath(null)` — the modal closes immediately. `successfulUploadPaths` is also cleared so any future re-invitation starts with a clean badge state. |
| Auto-open while modal already open | `useAutoMockUpload` calls `onOpenModal` unconditionally — `setModalUploadPath` simply replaces the current path. React's effect cleanup runs the old modal's `AbortController.abort()` before the new modal mounts. |
| Dropped file is not a `.json` / wrong MIME | `validateFileType` returns an error string → `fileError` shown below the drop zone (amber, `role="alert"`); textarea unchanged |
| Dropped file has `.json` extension but invalid content | `tryParseJSON` fails → `fileError`: `"<name>" contains invalid JSON.`; textarea unchanged |
| `FileReader` fails (permissions, corrupted file) | `readFileAsText` rejects → `fileError` set to the caught error message |
| Multiple files dropped at once | Only `e.dataTransfer.files[0]` is processed; additional files are silently ignored |

---

## 9. Resolved Decisions

| # | Question | Decision |
|---|---|---|
| 9.1 | Row interactivity | **Button-only + auto-open.** The re-open button (`⬆️ Upload JSON…`) appears on row hover/focus. The modal opens automatically via `useAutoMockUpload` when the invitation first arrives — no click required for the primary path. |
| 9.2 | Success badge | **Yes** — `✅` badge on the console row after a successful upload. Session-only; cleared by `clearMessages`. |
| 9.3 | XML support | **JSON-only** for now. Extendable if the server message changes. The modal uses `tryParseJSON` (not `validatePayload`) so XML never passes validation even though `validatePayload` accepts it. |
| 9.4 | Keyboard submit shortcut | **`Ctrl+Enter` / `Cmd+Enter`** to submit; `Enter` inserts newline. Hint line shown in modal. |
| 9.5 | Pre-fill textarea | **Always empty** — mock upload payload is semantically unrelated to any stored JSON-Path payload. |

---

## 10. Files Changed / Created

| File | Change type | Notes |
|---|---|---|
| `src/utils/messageParser.ts` | Edit | Add `extractMockUploadPath`, `isMockUploadMessage` |
| `src/hooks/useAutoMarkdownPin.ts` | Edit | Add `isMockUploadMessage` guard to `isPinnableResponse` to prevent cross-hook interference |
| `src/hooks/useAutoMockUpload.ts` | **Create** | New automation hook — watermark-guarded message watcher, auto-opens modal |
| `src/components/Console/ConsoleMessage.tsx` | Edit | New prop `onUploadMockData`, new flags, ⬆️ icon, re-open button, success badge |
| `src/components/Console/Console.module.css` | Edit | New `.consoleMessageMockUpload` + `.uploadMockButton` rules |
| `src/components/Console/Console.tsx` | Edit | Thread `onUploadMockData` + `successfulUploadPaths` props |
| `src/components/LeftPanel/LeftPanel.tsx` | Edit | Thread new props to `Console` |
| `src/hooks/useMockUpload.ts` | **Create** | New hook — fetch lifecycle |
| `src/components/MockUploadModal/MockUploadModal.tsx` | **Create** | New modal component |
| `src/components/MockUploadModal/MockUploadModal.module.css` | **Create** | New modal styles |
| `src/components/Playground.tsx` | Edit | State, `modalTriggerRef`, callbacks, `useAutoMockUpload`, modal render, prop threading, `handleClearMessages` update |
| `src/index.css` | Edit | Update **existing** `--warning-color` value from `#f5780b` to `#f59e0b` (amber-500, to match the amber row-accent) — do **not** add a second declaration |

No changes to:
- `playgrounds.ts` — trigger is purely message-driven, no new per-playground config needed
- `WebSocketContext.tsx` — upload is a direct `fetch`, no WS involvement
- `useWebSocket.ts` — the existing `pendingUploadRef` two-step handshake is only for
  the JSON-Path playground's `upload` command; this feature does not use it.
  **Safety confirmation:** `useWebSocket.ts` watches incoming messages via
  `extractUploadPath`, whose regex is anchored to `/api/json/content/…`. The new
  mock-upload path is `/api/mock/…`. The two regexes are entirely disjoint —
  `extractUploadPath` will never match a mock-upload invitation and there is no
  cross-trigger risk between the two upload protocols.

---

## 11. What Is Explicitly Out of Scope

- **Drag-and-drop file upload** — out of scope; paste is sufficient.
- **File browser (input[type=file])** — out of scope for the same reason.
- **Progress reporting** — the payload is JSON text; it is small enough that a
  simple spinner is adequate. No `ReadableStream` / `XMLHttpRequest` progress events.
- **Upload history** — past uploads are not persisted. The `✅` badge is session-only.
- **Multiple concurrent upload invitations** — only the most recently opened modal
  is tracked in state. If the user receives two invitations without closing the
  first modal, clicking the second replaces `modalUploadPath` and the first AbortController is cleaned up via the modal unmount.
- **Server-sent JSON schema validation** — the modal validates only that the input
  is syntactically valid JSON. Business-level validation is the server's responsibility.
