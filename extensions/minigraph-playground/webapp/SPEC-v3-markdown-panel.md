# Spec: Markdown Preview Panel
**Version:** 1.0  
**Date:** 2026-02-23  
**Status:** Decisions locked — ready for implementation  

---

## Changelog

| Version | Date | Summary |
|---|---|---|
| 1.0 | 2026-02-23 | Initial spec — tabbed right panel; `react-markdown` + `remark-gfm`; pinnable message selection; `isMarkdownCandidate` helper |

---

## 1. Overview

WebSocket responses from the backend are frequently plain-text documents (Markdown-formatted API descriptions, help text, query results). Currently these render as raw monospace strings inside `ConsoleMessage`. This feature adds a **Markdown Preview** tab to the right panel, alongside the existing `PayloadEditor`.

The right panel becomes a **two-tab container** (`RightPanel`). The active tab is controlled by a tab strip at the top of the panel. `PayloadEditor` and `MarkdownPreview` are both permanently mounted but only one is visible at a time (CSS `display: none` on the inactive tab body — avoids remounting / preserves editor scroll state).

---

## 2. Decisions

| # | Question | Decision | Rationale |
|---|---|---|---|
| A | Panel topology | **Tabbed right panel** | Two-tab container in the existing right panel slot; no second `<Separator>`, no extra saved split, no collapse/expand imperative API. Option B (three panels) is the correct v4 of this feature if an IDE-like layout is needed. |
| B | Markdown library | **`react-markdown ^9` + `remark-gfm ^4`** | No `dangerouslySetInnerHTML`; XSS protection is structural (JSX output). `remark-gfm` adds GFM tables, task lists, and strikethrough — covers likely server output formats. |
| C | Message selection | **Pinnable: user clicks a Console message row to pin it to the preview** | Mirrors a "response body" viewer; explicit selection avoids flickering on rapid message arrival. The last received non-JSON message is shown by default until the user pins a different one. |
| D | Detection logic | **`isMarkdownCandidate` in `messageParser.ts`; derived state in `Playground`** | Pure helper + `useMemo` derivation — zero reducer changes. Fully reversible. |

---

## 3. Architecture

### 3.1 Right panel becomes `RightPanel`

A new component `src/components/RightPanel/RightPanel.tsx` replaces the anonymous `<div className={styles.rightPanelContent}>` in `Playground.tsx`. It owns:

- The tab strip (`PayloadEditor` | `Markdown Preview`)
- Active-tab state (`useState<RightTab>('payload')`)
- Rendering of both tab bodies (always mounted; inactive body hidden via CSS)

`Playground` passes all props for both tabs into `RightPanel` and is otherwise unaware of tab state.

### 3.2 `MarkdownPreview` component

A new component `src/components/MarkdownPreview/MarkdownPreview.tsx`. Responsibilities:

- Accepts `pinnedMessage: string | null` — the raw text to render.
- Renders the Markdown-to-JSX output of `react-markdown` with `remark-gfm`.
- Shows an empty-state placeholder when `pinnedMessage` is `null`.
- Is a **bare component** — no card wrapper; fills the tab body via flex.

### 3.3 Message selection flow

```
useWebSocket.messages  (existing — unchanged)
        │
        ▼
Playground (useMemo)
  lastNonJsonMessage: string | null   ← default when no pin active
  pinnedMessage:      string | null   ← controlled by user click
        │
        ▼  (whichever is non-null: pinnedMessage takes precedence)
  resolvedPreviewMessage: string | null
        │
        ├──→ RightPanel → MarkdownPreview (for rendering)
        └──→ LeftPanel  → Console → ConsoleMessage (click to pin)
```

### 3.4 Message detection

`messageParser.ts` gains one new exported function:

```ts
/**
 * Returns true when a raw WebSocket message string is a plain-text
 * (non-JSON) candidate for Markdown rendering.
 *
 * A message is NOT a Markdown candidate if:
 *  - it is a valid JSON object or array (handled by JsonView)
 *  - it is a JSON-encoded lifecycle event ({ type, message, time })
 *    i.e. tryParseJSON succeeds AND the parsed object has a "type" field
 *
 * Everything else — including multi-line text, Markdown syntax, XML snippets
 * that are not valid JSON — is considered a Markdown candidate.
 */
export function isMarkdownCandidate(raw: string): boolean {
  const result = tryParseJSON(raw);
  if (!result.isJSON) return true;                     // not JSON at all → candidate
  const obj = result.data as Record<string, unknown>;
  if (typeof obj['type'] === 'string') return false;   // lifecycle event → not candidate
  return false;                                        // any other JSON object → not candidate
}
```

### 3.5 Derived state in `Playground`

No new reducer state. Two new pieces of state / derivations:

```ts
// In Playground.tsx

// User-pinned message (null = no pin, fall back to auto-last)
const [pinnedMessage, setPinnedMessage] = useState<string | null>(null);

// Auto-last: the raw text of the most recently received non-JSON message.
// Recomputes only when messages array reference changes.
const lastNonJsonMessage = useMemo<string | null>(() => {
  for (let i = ws.messages.length - 1; i >= 0; i--) {
    if (isMarkdownCandidate(ws.messages[i].raw)) return ws.messages[i].raw;
  }
  return null;
}, [ws.messages]);

// The value actually shown in MarkdownPreview.
// pinnedMessage wins; falls back to auto-last.
const resolvedPreviewMessage = pinnedMessage ?? lastNonJsonMessage;
```

`setPinnedMessage` is threaded down as `onPinMessage: (raw: string) => void` through `LeftPanel` → `Console` → `ConsoleMessage`.

**Clearing the pin:** When `ws.clearMessages` is called, `pinnedMessage` must also be cleared. `Playground` wraps the clear action:

```ts
const handleClearMessages = () => {
  ws.clearMessages();
  setPinnedMessage(null);
};
```

`handleClearMessages` replaces direct `ws.clearMessages` in all props passed to `LeftPanel`.

---

## 4. Component Specifications

### 4.1 `RightPanel` (new)

**File:** `src/components/RightPanel/RightPanel.tsx`  
**CSS Module:** `src/components/RightPanel/RightPanel.module.css`

#### Props

```ts
export type RightTab = 'payload' | 'preview';

interface RightPanelProps {
  // Tab control (uncontrolled — RightPanel owns active tab state internally)
  // No activeTab prop — RightPanel is self-contained.

  // PayloadEditor passthrough
  payload:            string;
  onChange:           (value: string) => void;
  validation:         ValidationResult;
  onFormat:           () => void;

  // MarkdownPreview passthrough
  previewMessage:     string | null;    // resolvedPreviewMessage from Playground
  pinnedMessage:      string | null;    // for "pinned" badge display only
}
```

> **Why is active-tab state inside `RightPanel`, not `Playground`?**  
> The active tab is purely a UI concern with no effect on data flow. Lifting it to `Playground` would add a prop and a setter with zero benefit — the tab choice affects nothing outside the right panel. This is the same principle as why `multiline` state lives in `Playground` (it affects `sendDisabled`) vs. internal state in `CommandInput` (it wouldn't).  
> If a future requirement needs `Playground` to know the active tab (e.g. auto-switch to preview on first Markdown message), the state can be lifted at that point.

#### Tab persistence

The active tab is **not persisted** to `localStorage`. The user's last tab choice is lost on navigation. This is intentional — a "payload" default on every route mount is the correct starting state (user lands on a playground to edit their payload). Persisting the tab is a future enhancement.

#### Behaviour

- Default active tab: `'payload'`
- Both tab bodies are **always mounted** in the DOM; inactive body has `display: none` via CSS class `.tabBodyHidden`. This avoids:
  - Losing `PayloadEditor` scroll / textarea cursor position on tab switch.
  - Unnecessary `MarkdownPreview` re-renders when messages arrive while on the Payload tab.
- Tab strip is `role="tablist"`; each tab button is `role="tab"` with `aria-selected` and `aria-controls`.
- Tab bodies are `role="tabpanel"` with `id` matching the tab's `aria-controls`.

#### JSX structure

```tsx
<div className={styles.rightPanel}>
  {/* Tab strip */}
  <div className={styles.tabStrip} role="tablist" aria-label="Right panel tabs">
    <button
      role="tab"
      aria-selected={activeTab === 'payload'}
      aria-controls="tab-payload"
      className={`${styles.tab} ${activeTab === 'payload' ? styles.tabActive : ''}`}
      onClick={() => setActiveTab('payload')}
    >
      Payload Editor
    </button>
    <button
      role="tab"
      aria-selected={activeTab === 'preview'}
      aria-controls="tab-preview"
      className={`${styles.tab} ${activeTab === 'preview' ? styles.tabActive : ''}`}
      onClick={() => setActiveTab('preview')}
    >
      Markdown Preview
      {pinnedMessage !== null && <span className={styles.pinnedBadge} aria-label="Message pinned">📌</span>}
    </button>
  </div>

  {/* Tab bodies — always mounted, hidden via CSS */}
  <div
    id="tab-payload"
    role="tabpanel"
    className={`${styles.tabBody} ${activeTab !== 'payload' ? styles.tabBodyHidden : ''}`}
  >
    <PayloadEditor payload={payload} onChange={onChange} validation={validation} onFormat={onFormat} />
  </div>
  <div
    id="tab-preview"
    role="tabpanel"
    className={`${styles.tabBody} ${activeTab !== 'preview' ? styles.tabBodyHidden : ''}`}
  >
    <MarkdownPreview message={previewMessage} />
  </div>
</div>
```

---

### 4.2 `RightPanel.module.css` (new)

```css
/* Root — fills the <Panel> slot, flex column */
.rightPanel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* Tab strip */
.tabStrip {
  display: flex;
  flex: 0 0 auto;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
  padding: 0 1rem;
  gap: 0;
}

.tab {
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 0.625rem 1rem;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
  transition: color 0.15s, border-color 0.15s;
  display: flex;
  align-items: center;
  gap: 0.375rem;
  /* Prevent tab strip height jitter when badge appears */
  line-height: 1.25rem;
}

.tab:hover {
  color: var(--text-primary);
}

.tab:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: -2px;
}

.tabActive {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
}

.pinnedBadge {
  font-size: 0.75rem;
  line-height: 1;
}

/* Tab bodies */
.tabBody {
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Hidden tab — stays mounted; hidden from view and from AT */
.tabBodyHidden {
  display: none;
}
```

---

### 4.3 `MarkdownPreview` (new)

**File:** `src/components/MarkdownPreview/MarkdownPreview.tsx`  
**CSS Module:** `src/components/MarkdownPreview/MarkdownPreview.module.css`

#### Props

```ts
interface MarkdownPreviewProps {
  message: string | null;
}
```

#### Behaviour

- When `message` is `null`: render the empty-state placeholder (see §4.3.1).
- When `message` is a non-null string: render `<ReactMarkdown remarkPlugins={[remarkGfm]}>{message}</ReactMarkdown>` inside a scrollable `.previewBody` div.
- `react-markdown` renders to JSX — no `dangerouslySetInnerHTML`, no manual sanitisation.
- The component is a **bare flex column** that fills 100% of its parent (the tab body).

#### JSX structure

```tsx
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import styles from './MarkdownPreview.module.css';

interface MarkdownPreviewProps {
  message: string | null;
}

export default function MarkdownPreview({ message }: MarkdownPreviewProps) {
  return (
    <div className={styles.previewRoot}>
      <div className={styles.previewHeader}>
        <span className={styles.previewTitle}>Markdown Preview</span>
      </div>
      <div className={styles.previewBody}>
        {message === null ? (
          <div className={styles.emptyPreview}>
            No preview yet. Send a command and click a plain-text message in the
            Console to pin it here, or wait for the first text response to appear
            automatically.
          </div>
        ) : (
          <div className={styles.markdownContent}>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {message}
            </ReactMarkdown>
          </div>
        )}
      </div>
    </div>
  );
}
```

#### 4.3.1 Empty-state copy

> "No preview yet. Send a command and click a plain-text message in the Console to pin it here, or wait for the first text response to appear automatically."

---

### 4.4 `MarkdownPreview.module.css` (new)

```css
/* Root — bare flex column, fills tab body */
.previewRoot {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  padding: 0.75rem;
}

/* Header row — mirrors Console's .consoleHeader style */
.previewHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border-color);
  flex: 0 0 auto;
}

.previewTitle {
  font-weight: 600;
  color: var(--text-primary);
}

/* Scrollable body */
.previewBody {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  background-color: var(--bg-primary);
  border-radius: var(--radius);
  padding: 1rem 1.25rem;
}

/* Empty state */
.emptyPreview {
  color: var(--text-secondary);
  text-align: center;
  padding: 2rem;
  font-size: 0.875rem;
}

/* ── Markdown typography ──────────────────────────────────────────── */
/* Scoped to .markdownContent so global styles are not polluted.       */

.markdownContent {
  color: var(--text-primary);
  font-size: 0.9rem;
  line-height: 1.65;
}

.markdownContent h1,
.markdownContent h2,
.markdownContent h3,
.markdownContent h4,
.markdownContent h5,
.markdownContent h6 {
  color: var(--primary-color);
  margin-top: 1.25em;
  margin-bottom: 0.5em;
  font-weight: 600;
  line-height: 1.25;
}

.markdownContent h1 { font-size: 1.5rem; border-bottom: 1px solid var(--border-color); padding-bottom: 0.3em; }
.markdownContent h2 { font-size: 1.25rem; border-bottom: 1px solid var(--border-color); padding-bottom: 0.2em; }
.markdownContent h3 { font-size: 1.1rem; }

.markdownContent p {
  margin-top: 0;
  margin-bottom: 0.75em;
}

.markdownContent a {
  color: var(--primary-color);
  text-decoration: underline;
}

.markdownContent a:hover {
  opacity: 0.8;
}

/* Inline code */
.markdownContent code {
  background: rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-sm);
  padding: 0.15em 0.4em;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.85em;
  color: #86efac;
}

/* Fenced code blocks */
.markdownContent pre {
  background: var(--console-bg);
  border-radius: var(--radius);
  padding: 1rem;
  overflow-x: auto;
  margin-bottom: 0.75em;
}

.markdownContent pre code {
  background: transparent;
  padding: 0;
  font-size: 0.875rem;
  color: var(--console-text);
}

/* Blockquote */
.markdownContent blockquote {
  border-left: 3px solid var(--primary-color);
  margin: 0 0 0.75em 0;
  padding: 0.25em 0 0.25em 1em;
  color: var(--text-secondary);
}

/* Lists */
.markdownContent ul,
.markdownContent ol {
  padding-left: 1.5em;
  margin-bottom: 0.75em;
}

.markdownContent li {
  margin-bottom: 0.25em;
}

/* GFM task list items */
.markdownContent li input[type="checkbox"] {
  margin-right: 0.4em;
  accent-color: var(--primary-color);
}

/* GFM tables */
.markdownContent table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 0.75em;
  font-size: 0.875rem;
}

.markdownContent th,
.markdownContent td {
  border: 1px solid var(--border-color);
  padding: 0.4em 0.75em;
  text-align: left;
}

.markdownContent th {
  background: var(--bg-secondary);
  font-weight: 600;
  color: var(--text-primary);
}

.markdownContent td {
  color: var(--text-secondary);
}

/* GFM strikethrough — rendered as <del> by remark-gfm */
.markdownContent del {
  color: var(--text-muted);
  text-decoration: line-through;
}

/* Horizontal rule */
.markdownContent hr {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 1em 0;
}
```

---

### 4.5 `ConsoleMessage` — changes (pin interaction)

**File:** `src/components/Console/ConsoleMessage.tsx` — **modified**

Add an optional `onPin?: (raw: string) => void` prop. When present, the message row becomes clickable and shows a pin affordance on hover.

```ts
interface ConsoleMessageProps {
  message: string;
  raw:     string;          // NEW — the original raw string passed alongside parsed message
  onPin?:  (raw: string) => void;  // NEW — called when the row is clicked
  pinned?: boolean;         // NEW — true when this message is the currently pinned one
}
```

> **Why add `raw` as a separate prop?**  
> `ConsoleMessage` currently only receives `message` (the post-parse `.message` field). `onPin` must pass back the **original raw WebSocket string** (not the parsed `.message`) because `resolvedPreviewMessage` in `Playground` is derived from `msg.raw` — passing the parsed field would break the round-trip. Rather than re-parsing inside `ConsoleMessage`, `Console` passes `raw={msg.raw}` alongside `message={msg.raw}`. This is a deliberate redundancy that keeps the interfaces honest and avoids re-parsing.

**Updated row JSX** (inside `ConsoleMessage`):

```tsx
<div
  className={`
    ${styles.consoleMessage}
    ${styles[`messageType-${parsed.type}`]}
    ${onPin ? styles.consoleMessagePinnable : ''}
    ${pinned ? styles.consoleMessagePinned : ''}
  `}
  onClick={onPin && isMarkdownCandidate(raw) ? () => onPin(raw) : undefined}
  title={onPin && isMarkdownCandidate(raw) ? 'Click to pin to Markdown Preview' : undefined}
  role={onPin && isMarkdownCandidate(raw) ? 'button' : undefined}
  tabIndex={onPin && isMarkdownCandidate(raw) ? 0 : undefined}
  onKeyDown={onPin && isMarkdownCandidate(raw)
    ? (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onPin(raw); } }
    : undefined}
  aria-label={onPin && isMarkdownCandidate(raw) ? 'Pin to Markdown Preview' : undefined}
  aria-pressed={pinned}
>
  {/* existing icon / content / time structure unchanged */}
```

**Import to add:**
```ts
import { parseMessage, getMessageIcon, tryParseJSON, isMarkdownCandidate } from '../../utils/messageParser';
```

**New CSS rules to add to `Console.module.css`:**

```css
/* Pinnable message row — pointer cursor + hover highlight */
.consoleMessagePinnable {
  cursor: pointer;
}

.consoleMessagePinnable:hover {
  background-color: rgba(37, 99, 235, 0.12);
  outline: 1px solid rgba(37, 99, 235, 0.3);
}

.consoleMessagePinnable:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

/* Pinned message row — persistent highlight */
.consoleMessagePinned {
  background-color: rgba(37, 99, 235, 0.18);
  outline: 1px solid rgba(37, 99, 235, 0.5);
}
```

---

### 4.6 `Console` — changes

**File:** `src/components/Console/Console.tsx` — **modified**

Add two props:

```ts
interface ConsoleProps {
  // ...existing props unchanged...
  onPinMessage?:  (raw: string) => void;  // NEW — forwarded to each ConsoleMessage
  pinnedMessage?: string | null;          // NEW — used to mark the pinned row
}
```

Pass through to each `ConsoleMessage`:

```tsx
<ConsoleMessage
  message={msg.raw}
  raw={msg.raw}
  onPin={onPinMessage}
  pinned={pinnedMessage === msg.raw}
/>
```

---

### 4.7 `LeftPanel` — changes

**File:** `src/components/LeftPanel/LeftPanel.tsx` — **modified**

Add two props and pass them to `Console`:

```ts
interface LeftPanelProps {
  // ...existing props unchanged...
  onPinMessage?:  (raw: string) => void;  // NEW
  pinnedMessage?: string | null;          // NEW
}
```

---

### 4.8 `Playground` — changes

**File:** `src/components/Playground.tsx` — **modified**

**Imports to add:**
```ts
import { isMarkdownCandidate } from '../utils/messageParser';
import RightPanel from './RightPanel/RightPanel';
```

**Import to remove:**
```ts
import PayloadEditor from './PayloadEditor/PayloadEditor';
// (PayloadEditor is now rendered inside RightPanel, not directly in Playground)
```

**New state + derivations** (add after existing hooks):
```ts
const [pinnedMessage, setPinnedMessage] = useState<string | null>(null);

const lastNonJsonMessage = useMemo<string | null>(() => {
  for (let i = ws.messages.length - 1; i >= 0; i--) {
    if (isMarkdownCandidate(ws.messages[i].raw)) return ws.messages[i].raw;
  }
  return null;
}, [ws.messages]);

const resolvedPreviewMessage = pinnedMessage ?? lastNonJsonMessage;
```

**Clear handler wrapper** (replaces direct `ws.clearMessages` in LeftPanel's `onClear` prop):
```ts
const handleClearMessages = () => {
  ws.clearMessages();
  setPinnedMessage(null);
};
```

**Replace right-panel JSX** — the `<Panel>` right child changes from:
```tsx
<Panel defaultSize="40%" minSize="20%">
  <div className={styles.rightPanelContent}>
    <PayloadEditor ... />
  </div>
</Panel>
```
to:
```tsx
<Panel defaultSize="40%" minSize="20%">
  <RightPanel
    payload={payload}
    onChange={setPayload}
    validation={payloadValidation}
    onFormat={handleFormatPayload}
    previewMessage={resolvedPreviewMessage}
    pinnedMessage={pinnedMessage}
  />
</Panel>
```

**Update `LeftPanel` props** — add `onPinMessage` and `pinnedMessage`:
```tsx
<LeftPanel
  {/* ...all existing props unchanged... */}
  onClear={handleClearMessages}        // ← was ws.clearMessages; now the wrapper
  onPinMessage={setPinnedMessage}
  pinnedMessage={pinnedMessage}
/>
```

**Remove from `Playground.module.css`:** the `.rightPanelContent` rule is no longer needed — `RightPanel` owns that flex-column wrapper.

---

## 5. Data Flow Diagram

```
Playground
├── useState: pinnedMessage (string | null)
├── useMemo:  lastNonJsonMessage ← ws.messages filtered by isMarkdownCandidate
├── derived:  resolvedPreviewMessage = pinnedMessage ?? lastNonJsonMessage
│
├── handleClearMessages() → ws.clearMessages() + setPinnedMessage(null)
│
├── <LeftPanel
│     messages={ws.messages}
│     onClear={handleClearMessages}
│     onPinMessage={setPinnedMessage}    ← threads to Console → ConsoleMessage
│     pinnedMessage={pinnedMessage}      ← threads to Console → ConsoleMessage (pinned highlight)
│   />
│     └── <Console
│           onPinMessage={onPinMessage}
│           pinnedMessage={pinnedMessage}
│         />
│           └── <ConsoleMessage
│                 raw={msg.raw}
│                 onPin={onPinMessage}     ← click calls setPinnedMessage(raw)
│                 pinned={pinnedMessage === msg.raw}
│               />
│
└── <RightPanel
      previewMessage={resolvedPreviewMessage}
      pinnedMessage={pinnedMessage}       ← for badge on Markdown Preview tab
      payload / onChange / validation / onFormat (passthrough)
    />
      ├── <PayloadEditor ... />           (always mounted; hidden when preview tab active)
      └── <MarkdownPreview
            message={resolvedPreviewMessage}
          />                             (always mounted; hidden when payload tab active)
```

---

## 6. Dependency

```
react-markdown   ^9.0.0   ~12 KB gzipped
remark-gfm       ^4.0.0   ~3 KB gzipped
npm install react-markdown@^9 remark-gfm@^4
```

> **Why v9 of `react-markdown`?** v9 is the current stable release, ESM-only — compatible with Vite 6 and the project's `"type": "module"` package.

---

## 7. File Structure

```
src/
  utils/
    messageParser.ts           ← MODIFIED: add isMarkdownCandidate()
  components/
    RightPanel/
      RightPanel.tsx           ← NEW
      RightPanel.module.css    ← NEW
    MarkdownPreview/
      MarkdownPreview.tsx      ← NEW
      MarkdownPreview.module.css ← NEW
    Console/
      Console.tsx              ← MODIFIED (onPinMessage, pinnedMessage props)
      ConsoleMessage.tsx       ← MODIFIED (raw, onPin, pinned props; pin interaction)
      Console.module.css       ← MODIFIED (pinnable/pinned row styles)
    LeftPanel/
      LeftPanel.tsx            ← MODIFIED (onPinMessage, pinnedMessage props)
    Playground.tsx             ← MODIFIED (RightPanel; pinnedMessage state; useMemo derivations)
    Playground.module.css      ← MODIFIED (delete .rightPanelContent)
    PayloadEditor/             ← NO CHANGE (rendered inside RightPanel now)
```

---

## 8. Accessibility

| Requirement | Implementation |
|---|---|
| Tab strip keyboard navigation | `role="tablist"` / `role="tab"` / `aria-selected` / `aria-controls` on tab strip in `RightPanel` |
| Tab body identity | `role="tabpanel"` with `id` matching tab's `aria-controls`; `tabIndex={0}` on active panel |
| Pinnable message rows | `role="button"` + `tabIndex={0}` + `onKeyDown` (Enter/Space) on pinnable `ConsoleMessage` rows |
| Pinned row state | `aria-pressed={pinned}` on pinnable row (toggle button semantics) |
| Markdown Preview tab badge | `aria-label="Message pinned"` on `📌` badge span |
| Markdown content | Rendered as semantic HTML via `react-markdown` — headings, lists, tables all produce correct elements automatically |

---

## 9. Out of Scope

- Syntax highlighting inside fenced code blocks (e.g. `react-syntax-highlighter`). Can be added as a `react-markdown` component override in a future iteration.
- Auto-switching to the Markdown Preview tab when a new text message arrives. Left as a future enhancement — auto-switching during rapid message streams would be disruptive.
- Persisting the active tab to `localStorage`.
- Persisting the pinned message across page reload.
- "Unpin" button (explicit deselect) — the pin is cleared automatically when `clearMessages` is called. Manual unpin can be added in a follow-up.
- Multiple pinned messages / a pinned-message list.

---

## 10. Pre-existing Code Notes

- `ConsoleMessage.tsx` currently receives only `message: string` (the post-parse `.message` field). After this change it receives both `message` and `raw`. The caller (`Console.tsx`) passes both as `message={msg.raw}` and `raw={msg.raw}`. This is correct — `msg.raw` is both the original string and the display string for non-lifecycle messages.
- `PayloadEditor` is unchanged and moves to rendering inside `RightPanel` with identical props. The `PayloadEditor` import moves from `Playground.tsx` to `RightPanel.tsx`.
- `Playground.module.css` loses `.rightPanelContent` — that rule's job (flex column, height 100%, overflow hidden) is taken over by `RightPanel.module.css`'s `.rightPanel` rule.
- The `react-markdown` package is ESM-only since v7. Vite handles ESM natively — no special Vite config required.
