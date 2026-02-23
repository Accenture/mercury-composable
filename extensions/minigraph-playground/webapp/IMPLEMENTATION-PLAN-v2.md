# Implementation Plan — Left-Panel Redesign v2.3
**Spec:** `SPEC-v2-left-panel.md` v2.3  
**Date:** 2026-02-23  
**Branch:** `feature/minigraph-playground`  
**Status:** Ready to execute

---

## Pre-flight Checks (resolved before planning)

The following spec ambiguities from the initial analysis have been **resolved in v2.3** and are baked into this plan:

| Item | Resolution |
|---|---|
| `react-resizable-panels` API version | v4.6.5 confirmed. Components: `Group`, `Panel`, `Separator`. Hooks: `useDefaultLayout`, `useGroupRef`. No `PanelGroup`, `PanelResizeHandle`, `autoSaveId`, `keyboardResizeBy`, or `direction` prop. |
| `data-separator` active CSS selector | Runtime sets `data-separator="active"` when dragging — `[data-separator="active"]` is correct. |
| `useDefaultLayout` return type | Verified against installed v4.6.5 types: returns `{ defaultLayout: Layout \| undefined; onLayoutChange: (layout: Layout) => void \| undefined; onLayoutChanged: (layout: Layout) => void \| undefined }`. Call as `useDefaultLayout({ id: string, storage?: LayoutStorage })`. `onLayoutChange` is deprecated — use `onLayoutChanged` on `<Group>`. |
| Double-click reset | Built-in in v4.5+, no handler needed. Requires `defaultSize` on each `<Panel>`. |
| Keyboard resize | Built-in at 5 % per arrow step. No `keyboardResizeBy` prop exists in v4. |
| `.wrapper` height strategy | `display: flex; flex-direction: column` on `.wrapper` + `flex: 1 1 0; min-height: 0` on `.panelGroup`. No `calc(100vh - X)`. |
| `.header` `margin-bottom` | Remove — vertical spacing is handled by the flex column. |
| `handleConnect` post-redesign | One-liner: `const handleConnect = () => ws.connect();`. |
| `CommandInput` dual `onKeyDown` | `CommandInput` owns an internal merge handler. Enter/Ctrl+Enter → `onSend()`. All other keys delegate to the passed-in `onKeyDown` prop (arrow history). |
| `MAX_HISTORY` in `sendCommand` | History slice must use `MAX_HISTORY` (50), not `MAX_ITEMS` (200). The old code used `MAX_ITEMS` — this was a bug. |
| `ConsoleErrorBoundary` props | `{ children: React.ReactNode; fallback: string }` — fully typed in spec §11.3. |
| `autoSaveId` key normalisation | Use `config.path + '-panel-split'` verbatim (leading `/` is harmless in localStorage). |
| `ConnectionStatus` usage scope | Used only in `Playground.tsx` — safe to delete. |
| `Navigation.module.css` height impact | `ConnectionBar` adds ~40 px (one inline row). `margin-top: 1rem` on `.nav` stays. No header height budget issue. |
| `messages` type in `UseWebSocketReturn` | Spec §5.1 annotates it as `string[]` in the interface comment but adds a note that the shape changes to `{ id: number; raw: string }[]` (§11.2). The TypeScript interface must reflect the real shape. |

---

## Dependency Map

The following tasks have hard ordering constraints. All others within a phase are independent and can be done in any order (or in parallel).

```
Phase 0 (foundation) → must complete before any component work
  0.3 (CSS tokens + pulse) → ConnectionBar.module.css depends on @keyframes pulse
  0.2 (MAX_HISTORY) → Phase 1 hook changes depend on this constant

Phase 1 (hook) → must complete before Playground wiring (Phase 3, step 3.9)
  1.2 (message shape { id, raw }) ──┐
  1.3 (sendCommand extraction)      │→ both must land before Console changes (3.1)
                                    │   because Console.tsx will TypeScript-error
                                    │   against string[] after 1.2

Phase 2 (new components) → 2.1 (ConsoleErrorBoundary) must exist before 3.1 (Console)

Phase 3 ordering within phase:
  3.1 (Console) after 1.2 + 1.3 + 2.1
  3.7 (Navigation) before 3.9 (Playground) — Playground passes connectionBar prop
  3.9 (Playground) last in phase — pulls in everything
```

---

## Phase 0 — Foundation

### 0.1 Verify `react-resizable-panels` installation

> **Already done:** `package.json` already lists `"react-resizable-panels": "^4.6.5"` and the package is present in `node_modules`. No `npm install` step is required.

**Verify only:**
```bash
grep react-resizable-panels package.json
# Expected: "react-resizable-panels": "^4.6.5"
```

---

### 0.2 Update `src/config/playgrounds.ts`

Two constant changes:

| Constant | Old value | New value | Reason |
|---|---|---|---|
| `MAX_ITEMS` | `30` | `200` | Full-height console needs more buffer (§11.4) |
| `MAX_HISTORY` | *(new)* | `50` | Decouple command history from message buffer (Decision #10) |

```ts
// Before
export const MAX_ITEMS  = 30;

// After
export const MAX_ITEMS    = 200;
export const MAX_HISTORY  = 50;
```

`MAX_HISTORY` must be exported — it is imported by `useWebSocket.ts`.

---

### 0.3 Update `src/index.css`

**Two changes:**

**A. Add new CSS custom properties** inside the existing `:root` block (append after `--radius`):

```css
/* ── NEW tokens ── */
--focus-ring:          0 0 0 0.2rem rgba(37, 99, 235, 0.25);
--radius-sm:           0.25rem;
--text-muted:          #94a3b8;
--console-bg:          var(--bg-dark);
--console-text:        #e2e8f0;
--disabled-bg:         #e9ecef;
--input-focus-border:  var(--primary-color);
```

**B. Move `@keyframes pulse`** — add after `:root` (or after `#root`), before any component-specific selectors. This is currently defined only in `ConnectionStatus.module.css`; making it global allows `ConnectionBar.module.css` to use it without re-declaring:

```css
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.5; }
}
```

---

### 0.4 Create `src/hooks/useMediaQuery.ts`

New file. No new package dependency.

```ts
import { useState, useEffect } from 'react';

/**
 * Returns true while the document matches the given CSS media query string.
 * Re-evaluates reactively whenever the match state changes.
 */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => window.matchMedia(query).matches);

  useEffect(() => {
    const mql = window.matchMedia(query);
    const handler = (e: MediaQueryListEvent) => setMatches(e.matches);
    mql.addEventListener('change', handler);
    return () => mql.removeEventListener('change', handler);
  }, [query]);

  return matches;
}
```

---

## Phase 1 — Hook Changes (`useWebSocket.ts`)

All five tasks touch the same file. Apply them as a single commit to avoid intermediate TypeScript errors.

### 1.1 Fix `useToast.ts` — memoize `addToast` / `removeToast` (§11.1)

**File:** `src/hooks/useToast.ts`

Wrap both functions in `useCallback`. Add `useCallback` to the import. Remove `useState` from the import if it becomes the only hook (it won't — `useState` is still needed).

```ts
import { useState, useCallback } from 'react';

// ...

const addToast = useCallback((message: string, type: ToastType = 'info') => {
  const id = Date.now() + Math.random();
  setToasts(prev => [...prev, { id, message, type }]);
  setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 3000);
}, []);

const removeToast = useCallback((id: number) => {
  setToasts(prev => prev.filter(t => t.id !== id));
}, []);
```

> **Why this is Phase 1:** `addToast` flows into `useWebSocket` as a dependency. Stabilising it first avoids spurious `useCallback` re-creation cascades when testing later phases.

---

### 1.2 Change `messages` type: `string[]` → `{ id: number; raw: string }[]` (§11.2)

**File:** `src/hooks/useWebSocket.ts`

**A. Update `WsState`:**
```ts
interface WsState {
  phase:        WsPhase;
  messages:     { id: number; raw: string }[];   // was: string[]
  command:      string;
  autoScroll:   boolean;
  historyIndex: number;
}
```

**B. Add a message ID counter ref** (in the hook body, alongside other refs):
```ts
const msgIdRef = useRef(0);
```

**C. Update `initialState`:**
```ts
const initialState: WsState = {
  phase:        'idle',
  messages:     [],   // type is now { id: number; raw: string }[]
  command:      '',
  autoScroll:   true,
  historyIndex: -1,
};
```

**D. Update the three reducer cases that append messages.** The reducer cannot access `msgIdRef` directly (it is outside the reducer). The solution: pass `id` via the action payload.

Update the action union:
```ts
type WsAction =
  | { type: 'CONNECTING' }
  | { type: 'CONNECTED';         id: number; msg: string }
  | { type: 'MESSAGE_RECEIVED';  id: number; msg: string }
  | { type: 'DISCONNECTED';      id: number; msg: string }
  | { type: 'CONNECT_ERROR' }
  | { type: 'SET_COMMAND';       value: string }
  | { type: 'CLEAR_COMMAND' }
  | { type: 'SET_HISTORY_INDEX'; index: number; command: string }
  | { type: 'CLEAR_MESSAGES' }
  | { type: 'TOGGLE_AUTO_SCROLL' };
```

Update the three reducer cases:
```ts
case 'CONNECTED': {
  const msgs = [...state.messages, { id: action.id, raw: action.msg }];
  if (msgs.length > MAX_ITEMS) msgs.shift();
  return { ...state, phase: 'connected', messages: msgs };
}
case 'MESSAGE_RECEIVED': {
  const msgs = [...state.messages, { id: action.id, raw: action.msg }];
  if (msgs.length > MAX_ITEMS) msgs.shift();
  return { ...state, messages: msgs };
}
case 'DISCONNECTED': {
  const msgs = [...state.messages, { id: action.id, raw: action.msg }];
  if (msgs.length > MAX_ITEMS) msgs.shift();
  return { ...state, phase: 'idle', messages: msgs };
}
```

**E. Update the four dispatch call-sites** (in `connect` and `onclose` callbacks) to pass `id: ++msgIdRef.current`:

```ts
// ws.onopen
dispatch({ type: 'CONNECTED', id: ++msgIdRef.current, msg: eventWithTimestamp('info', 'connected') });

// ws.onmessage
dispatch({ type: 'MESSAGE_RECEIVED', id: ++msgIdRef.current, msg: evt.data });

// ws.onerror (no message dispatched — no change needed)

// ws.onclose
dispatch({ type: 'DISCONNECTED', id: ++msgIdRef.current, msg: eventWithTimestamp('info', `disconnected - (${evt.code}) ${evt.reason}`) });
```

Also update the `disconnect` fallback in the `disconnect` callback:
```ts
dispatch({ type: 'MESSAGE_RECEIVED', id: ++msgIdRef.current, msg: eventWithTimestamp('error', 'already disconnected') });
```

**F. Update `UseWebSocketReturn`** — the type comment in §5.1 notes the shape change:
```ts
messages: { id: number; raw: string }[];
```

**G. Fix `copyMessages`** — the existing implementation calls `messages.join('\n')` which would produce `[object Object]\n[object Object]...` after the type change. Update it to map over `.raw`:

```ts
const copyMessages = useCallback(() => {
  navigator.clipboard.writeText(messages.map(m => m.raw).join('\n'));
  addToast('Console copied to clipboard!', 'success');
}, [messages, addToast]);
```

> ⚠️ After this change, `Console.tsx` will TypeScript-error until Phase 4 step 4.1 is applied. Keep these two steps in the same working session.

---

### 1.3 Extract `sendCommand` from `handleKeyDown` (§5.2)

**File:** `src/hooks/useWebSocket.ts`

**A. Add `MAX_HISTORY` to the import from `playgrounds.ts`:**
```ts
import { MAX_ITEMS, MAX_BUFFER, PING_INTERVAL, MAX_HISTORY } from '../config/playgrounds';
```

**B. Add `sendCommand` as a new `useCallback` before `handleKeyDown`:**

```ts
const sendCommand = useCallback(() => {
  if (!wsRef.current || phase !== 'connected') return;
  const text = command.trim();
  if (!text) return;

  wsRef.current.send(text);

  if (text === 'load') {
    if (!payload) {
      dispatch({ type: 'MESSAGE_RECEIVED', id: ++msgIdRef.current,
        msg: eventWithTimestamp('error', 'please paste a JSON/XML payload in the Payload panel') });
    } else if (payload.length > MAX_BUFFER) {
      dispatch({ type: 'MESSAGE_RECEIVED', id: ++msgIdRef.current,
        msg: eventWithTimestamp('error', `payload exceeds the ${MAX_BUFFER} character limit`) });
    } else {
      wsRef.current.send(payload);
    }
  }

  // Consecutive-dedup: mirrors bash ignoredups
  if (history[0] !== text) {
    setHistory(prev => [text, ...prev].slice(0, MAX_HISTORY));
  }

  dispatch({ type: 'CLEAR_COMMAND' });
}, [phase, command, payload, history, setHistory]);
```

**C. Replace `handleKeyDown` body** — strip all Enter/send logic, remove `connected` guard, remove `submitKey` reference:

```ts
const handleKeyDown = useCallback((e: React.KeyboardEvent<HTMLElement>) => {
  if (e.key === 'ArrowUp') {
    e.preventDefault();
    if (history.length > 0 && historyIndex < history.length - 1) {
      const newIndex = historyIndex + 1;
      dispatch({ type: 'SET_HISTORY_INDEX', index: newIndex, command: history[newIndex] });
    }
  } else if (e.key === 'ArrowDown') {
    e.preventDefault();
    if (historyIndex > 0) {
      const newIndex = historyIndex - 1;
      dispatch({ type: 'SET_HISTORY_INDEX', index: newIndex, command: history[newIndex] });
    } else if (historyIndex === 0) {
      dispatch({ type: 'CLEAR_COMMAND' });
    }
  }
  // Enter/Ctrl+Enter: CommandInput calls onSend() directly.
  // Shift+Enter: native textarea newline — not intercepted.
}, [history, historyIndex]);
```

---

### 1.4 Remove `submitKey` from `UseWebSocketOptions` and hook signature

**File:** `src/hooks/useWebSocket.ts`

- Delete `submitKey?: 'enter' | 'ctrl+enter'` from the `UseWebSocketOptions` interface.
- Remove `submitKey = 'enter'` from the destructuring in the hook signature.
- Remove `submitKey` from the `handleKeyDown` `useCallback` dependency array (already done by 1.3).
- Add `sendCommand` to the `UseWebSocketReturn` interface and to the `return` object.

**Updated `UseWebSocketReturn`:**
```ts
export interface UseWebSocketReturn {
  wsUrl:            string;
  connected:        boolean;
  connecting:       boolean;
  messages:         { id: number; raw: string }[];
  command:          string;
  setCommand:       (value: string) => void;
  connect:          () => void;
  disconnect:       () => void;
  sendCommand:      () => void;        // NEW
  handleKeyDown:    (e: React.KeyboardEvent<HTMLElement>) => void;
  consoleRef:       React.RefObject<HTMLDivElement | null>;
  autoScroll:       boolean;
  toggleAutoScroll: () => void;
  copyMessages:     () => void;
  clearMessages:    () => void;
}
```

**Updated return statement** — add `sendCommand`:
```ts
return {
  wsUrl, connected, connecting, messages, command, setCommand,
  connect, disconnect, sendCommand, handleKeyDown,
  consoleRef, autoScroll, toggleAutoScroll, copyMessages, clearMessages,
};
```

---

## Phase 2 — New Components

### 2.1 Create `src/components/Console/ConsoleErrorBoundary.tsx` (§11.3)

New class component. Must be created before `Console.tsx` is modified.

```tsx
import React from 'react';

interface ConsoleErrorBoundaryProps {
  fallback: string;
  children: React.ReactNode;
}

interface ConsoleErrorBoundaryState {
  hasError: boolean;
}

export class ConsoleErrorBoundary extends React.Component<
  ConsoleErrorBoundaryProps,
  ConsoleErrorBoundaryState
> {
  state: ConsoleErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ConsoleErrorBoundaryState {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return <span>{this.props.fallback}</span>;
    }
    return this.props.children;
  }
}
```

---

### 2.2 Create `src/components/ConnectionBar/ConnectionBar.tsx` + `ConnectionBar.module.css` (§3.5)

**`ConnectionBar.tsx`:**

```tsx
import styles from './ConnectionBar.module.css';

interface ConnectionBarProps {
  connected:    boolean;
  connecting:   boolean;
  url:          string;
  onConnect:    () => void;
  onDisconnect: () => void;
}

export default function ConnectionBar({
  connected, connecting, url, onConnect, onDisconnect
}: ConnectionBarProps) {
  const dotClass = connected
    ? styles.dotConnected
    : connecting
      ? styles.dotConnecting
      : styles.dotIdle;

  const dotLabel = connected ? 'Connected' : connecting ? 'Connecting' : 'Disconnected';

  return (
    <div className={styles.bar}>
      <span
        className={`${styles.dot} ${dotClass}`}
        aria-label={dotLabel}
      />
      <span className={styles.status}>{dotLabel}</span>
      <span className={styles.url}>{url}</span>

      {!connected && !connecting && (
        <button
          className={styles.button}
          onClick={onConnect}
          aria-label="Start WebSocket connection"
        >
          Start
        </button>
      )}
      {connecting && (
        <button
          className={styles.button}
          disabled
          aria-label="Connecting…"
          aria-disabled="true"
        >
          Connecting…
        </button>
      )}
      {connected && (
        <button
          className={styles.button}
          onClick={onDisconnect}
          aria-label="Stop WebSocket connection"
        >
          Stop Service
        </button>
      )}
    </div>
  );
}
```

**`ConnectionBar.module.css`:**

```css
/* Single inline row */
.bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dotIdle       { background: var(--danger-color); }
.dotConnecting { background: var(--warning-color); animation: pulse 1.5s infinite; }
.dotConnected  { background: var(--success-color); animation: pulse 2s infinite; }
/* pulse is defined globally in index.css */

.status {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--text-primary);
  white-space: nowrap;
}

.url {
  font-family: monospace;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1 1 0;
  min-width: 0;
}

.button {
  padding: 0.375rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.2s;
  white-space: nowrap;
  background: var(--primary-color);
  color: white;
}

.button:hover:not(:disabled) {
  background: var(--primary-hover);
}

.button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
```

---

### 2.3 Create `src/components/LeftPanel/LeftPanel.tsx` + `LeftPanel.module.css` (§3.1)

**`LeftPanel.tsx`:**

```tsx
import Console from '../Console/Console';
import CommandInput from '../CommandInput/CommandInput';
import styles from './LeftPanel.module.css';

interface LeftPanelProps {
  // Console props
  messages:           { id: number; raw: string }[];
  autoScroll:         boolean;
  onToggleAutoScroll: () => void;
  onCopy:             () => void;
  onClear:            () => void;
  consoleRef:         React.RefObject<HTMLDivElement | null>;
  // CommandInput props
  command:            string;
  onCommandChange:    (value: string) => void;
  onCommandKeyDown:   (e: React.KeyboardEvent<HTMLElement>) => void;
  onSend:             () => void;
  sendDisabled:       boolean;
  inputDisabled:      boolean;
  multiline:          boolean;
  onToggleMultiline:  () => void;
}

export default function LeftPanel({
  messages, autoScroll, onToggleAutoScroll, onCopy, onClear, consoleRef,
  command, onCommandChange, onCommandKeyDown, onSend,
  sendDisabled, inputDisabled, multiline, onToggleMultiline,
}: LeftPanelProps) {
  return (
    <div className={styles.root}>
      <Console
        messages={messages}
        autoScroll={autoScroll}
        onToggleAutoScroll={onToggleAutoScroll}
        onCopy={onCopy}
        onClear={onClear}
        consoleRef={consoleRef}
      />
      <CommandInput
        command={command}
        onChange={onCommandChange}
        onKeyDown={onCommandKeyDown}
        onSend={onSend}
        disabled={inputDisabled}
        sendDisabled={sendDisabled}
        multiline={multiline}
        onToggleMultiline={onToggleMultiline}
      />
    </div>
  );
}
```

**`LeftPanel.module.css`:**

```css
.root {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
```

> The `Console` component is `flex: 1 1 auto` (set in its own CSS). `CommandInput` is `flex: 0 0 auto` (set in its own CSS). `LeftPanel` simply provides the flex container.

> **Prop name threading at the `LeftPanel` → `CommandInput` boundary:**  
> `Playground` passes the keyboard history handler as **`onCommandKeyDown`** to `LeftPanel`. `LeftPanel` forwards it to `CommandInput` as **`onKeyDown`** — which is the name `CommandInput` already uses for that prop. These are two names for the same function, differing only at the component boundary. Do not rename either end.

---

## Phase 3 — Modified Components

### 3.1 Modify `src/components/Console/Console.tsx` (§3.2 + §11.2 + §11.3)

⚠️ **Apply §11.2 and §11.3 atomically** — TypeScript requires both at once.

**Changes:**
1. Import `ConsoleErrorBoundary`.
2. Update `ConsoleProps.messages` type to `{ id: number; raw: string }[]`.
3. Replace the root `<div className={styles.card}>` with `<div className={styles.consoleRoot}>`.
4. Replace the `messages.map` to use `msg.id` as key, `msg.raw` as message, and wrap in `ConsoleErrorBoundary`.
5. Update empty-state text — old text references the "Start" button by implication; new text is: `No messages yet. Use the Start button in the header to connect.` (rendered as `No messages yet. Use the <strong>Start</strong> button in the header to connect.`)

> **`ConsoleMessage.tsx` is unchanged.** It still accepts `message: string`. Step 4 passes `msg.raw` (which is a `string`) to it. No edits to `ConsoleMessage.tsx` are needed.

```tsx
import { ConsoleErrorBoundary } from './ConsoleErrorBoundary';
import ConsoleMessage from './ConsoleMessage';
import styles from './Console.module.css';

interface ConsoleProps {
  messages:           { id: number; raw: string }[];
  autoScroll:         boolean;
  onToggleAutoScroll: () => void;
  onCopy:             () => void;
  onClear:            () => void;
  consoleRef:         React.RefObject<HTMLDivElement | null>;
}

export default function Console({ messages, autoScroll, onToggleAutoScroll, onCopy, onClear, consoleRef }: ConsoleProps) {
  return (
    <div className={styles.consoleRoot}>
      <div className={styles.consoleHeader}>
        {/* header content unchanged */}
      </div>

      <div className={styles.console} ref={consoleRef} role="log" aria-live="polite">
        {messages.map((msg) => (
          <ConsoleErrorBoundary key={msg.id} fallback={msg.raw}>
            <ConsoleMessage message={msg.raw} />
          </ConsoleErrorBoundary>
        ))}
        {messages.length === 0 && (
          <div className={styles.emptyConsole}>
            No messages yet. Use the <strong>Start</strong> button in the header to connect.
          </div>
        )}
      </div>
    </div>
  );
}
```

---

### 3.2 Modify `src/components/Console/Console.module.css` (§3.2 + §6.2)

**Structural changes:**

| Rule | Change |
|---|---|
| `.card` | **Delete** entire rule |
| `.consoleRoot` | **Add**: `display: flex; flex-direction: column; height: 100%; overflow: hidden` |
| `.consoleHeader` | Add `flex: 0 0 auto` |
| `.console` | Replace `min-height: 300px; max-height: 600px` with `flex: 1 1 auto; min-height: 150px`; remove `max-height` |

**Token replacements (§6.2):**

| Hardcoded value | Replace with |
|---|---|
| `color: #e2e8f0` | `color: var(--console-text)` |
| `background-color: var(--bg-dark)` | `background-color: var(--console-bg)` |
| `color: #64748b` (empty state) | `color: var(--text-secondary)` |
| `color: #94a3b8` (timestamps) | `color: var(--text-muted)` |

---

### 3.3 Modify `src/components/CommandInput/CommandInput.tsx` (§3.3)

**Full rewrite of component logic:**

1. Add `import { useRef } from 'react'`.
2. Add `onSend: () => void` and `sendDisabled: boolean` to `CommandInputProps`.
3. Replace the toggle button `<button>` with a `<label><input type="checkbox" /></label>`.
4. Add `const textareaRef = useRef<HTMLTextAreaElement>(null)`.
5. Replace the `onKeyDown` pass-through with an internal merge handler.
6. Render Send button adaptively: right of textarea in single-line, full-width below in multiline.

**Updated `CommandInputProps`:**
```ts
interface CommandInputProps {
  command:            string;
  onChange:           (value: string) => void;
  onKeyDown:          (e: React.KeyboardEvent<HTMLElement>) => void;
  onSend:             () => void;
  sendDisabled:       boolean;
  disabled:           boolean;
  multiline?:         boolean;
  onToggleMultiline?: () => void;
}
```

**Internal key handler merge pattern (exact code to implement):**
```ts
const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
  if (e.key === 'Enter') {
    if (multiline) {
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault();
        onSend();
        textareaRef.current?.focus();
      }
      // plain Enter in multiline: fall through → browser inserts newline
    } else {
      if (!e.shiftKey) {
        e.preventDefault();
        onSend();
        textareaRef.current?.focus();
      }
      // Shift+Enter in single-line: fall through → browser expands textarea
    }
  } else {
    // ArrowUp / ArrowDown (and all other keys) → delegate to history handler
    onKeyDown(e);
  }
};
```

**Send button disabled condition:** `disabled={sendDisabled}` — the parent (`Playground` via `LeftPanel`) computes `sendDisabled={!ws.connected || !ws.command.trim()}`.

> **`disabled` vs `sendDisabled` — two separate props, two separate concerns:**
>
> | Prop | Controls | Computed by `Playground` as |
> |---|---|---|
> | `disabled: boolean` | The **textarea** — when `true`, the textarea is non-editable and greyed out. Prevents typing before a connection exists. | `!ws.connected` |
> | `sendDisabled: boolean` | The **Send button** only — when `true`, the button is `disabled`. Prevents sending an empty command or sending while disconnected. | `!ws.connected \|\| !ws.command.trim()` |
>
> Both props are kept. They are independent: a user is always prevented from typing when not connected (`disabled`), and the Send button is independently gated on having non-empty input (`sendDisabled`). The `disabled` prop is **not removed** from the updated `CommandInputProps`.

**Hint text:**
- Disabled: `'Enter your test message once it is connected'`
- Multiline + enabled: `'Ctrl+Enter to send · Enter for new line · Shift+Enter for new line'`
- Single-line + enabled: `'Enter to send · Shift+Enter for new line'`

**Placeholder:**
- Disabled: `'Not connected'`
- Multiline: `'Enter command (Ctrl+Enter to send · ↑↓ for history)'`
- Single-line: `'Enter command (Enter to send · ↑↓ for history)'`

---

### 3.4 Modify `src/components/CommandInput/CommandInput.module.css` (§6.2)

**Structural additions** for adaptive Send button placement:

```css
/* Single-line layout: textarea and Send button share one row */
.inputRow {
  display: flex;
  gap: 0.5rem;
  align-items: flex-end;
}

.inputRow .textarea {
  flex: 1 1 auto;
}

/* Multiline layout: Send button is full-width below the textarea */
.sendButtonFullWidth {
  width: 100%;
  margin-top: 0.5rem;
}

/* Send button base style */
.sendButton {
  padding: 0.5rem 1.25rem;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: background 0.2s;
  white-space: nowrap;
}

.sendButton:hover:not(:disabled) { background: var(--primary-hover); }
.sendButton:disabled { opacity: 0.5; cursor: not-allowed; }

/* Checkbox label */
.checkboxLabel {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
  color: var(--text-secondary);
  cursor: pointer;
  user-select: none;
}
```

**Remove:**
- `.toggleButton`, `.toggleButton:hover`, `.toggleButton.active`, `.toggleButton.active:hover` — replaced by checkbox.
- `.card` wrapper rule — `CommandInput` is no longer wrapped in a card (it is inside `LeftPanel` which is in a `<Panel>`).

**Token replacements (§6.2):**

| Hardcoded value | Replace with |
|---|---|
| `border: 1px solid #ced4da` | `border: 1px solid var(--border-color)` |
| `border-color: #80bdff` (focus) | `border-color: var(--input-focus-border)` |
| `box-shadow: 0 0 0 0.2rem rgba(0,123,255,0.25)` | `box-shadow: var(--focus-ring)` |
| `background-color: #e9ecef` (disabled) | `background-color: var(--disabled-bg)` |
| `color: #495057` (label) | `color: var(--text-secondary)` |

---

### 3.5 Modify `src/components/PayloadEditor/PayloadEditor.tsx` (§3.4)

1. Replace the outer `<div className={styles.card}>` root with `<div className={styles.payloadRoot}>`.
2. Remove the `.inputGroup` wrapper `<div>` — its children become direct children of `.payloadRoot`.
3. Add `flex: 0 0 auto` to `SampleButtons`' containing `<div>` — or wrap in a `<div className={styles.sampleButtonsRow}>` (see CSS below).

> The `rows={8}` attribute on the textarea is removed — the flex layout drives height, not `rows`.

> **`PayloadEditor` textarea is always enabled (no `disabled` prop).** Payload is a pre-connection document — the user should be able to paste/edit it before, during, and after a connection. The `load` command validates payload size and emptiness at send time. No `disabled` prop is added to `PayloadEditor` or its textarea.

---

### 3.6 Modify `src/components/PayloadEditor/PayloadEditor.module.css` (§3.4 + §6.2)

**Structural changes:**

| Rule | Change |
|---|---|
| `.card` | **Delete** entire rule |
| `.payloadRoot` | **Add**: `display: flex; flex-direction: column; height: 100%; overflow: hidden; padding: 1rem` |
| `.inputGroup` | **Delete** |
| `.textarea` | Add `flex: 1 1 auto; resize: none; min-height: 0` |
| `.sampleButtonsRow` (new) | `flex: 0 0 auto; padding-top: 0.5rem` |

**Token replacements (§6.2):**

| Hardcoded value | Replace with |
|---|---|
| `border: 1px solid #ced4da` | `border: 1px solid var(--border-color)` |
| `border-color: #80bdff` (focus) | `border-color: var(--input-focus-border)` |
| `box-shadow: 0 0 0 0.2rem rgba(0,123,255,0.25)` | `box-shadow: var(--focus-ring)` |
| `background-color: #e9ecef` (disabled) | `background-color: var(--disabled-bg)` |
| `color: #495057` (label) | `color: var(--text-secondary)` |
| `border-radius: 0.25rem` | `border-radius: var(--radius-sm)` |

---

### 3.7 Modify `src/components/Navigation.tsx` (§3.5)

1. Add `connectionBar?: React.ReactNode` to `NavigationProps`.
2. Insert the `.connectionSection` block as the first child of `<nav>`, conditionally rendered.

```tsx
interface NavigationProps {
  connectionBar?: React.ReactNode;
}

export default function Navigation({ connectionBar }: NavigationProps) {
  // ...existing toolLinks / externalLinks arrays — no change...
  return (
    <nav className={styles.nav}>
      {connectionBar && (
        <div className={styles.connectionSection}>
          {connectionBar}
        </div>
      )}
      <div className={styles.navSection}>
        {/* Tools — unchanged */}
      </div>
      <div className={styles.navSection}>
        {/* Quick Links — unchanged */}
      </div>
    </nav>
  );
}
```

---

### 3.8 Modify `src/components/Navigation.module.css` (§3.5)

Add `.connectionSection`. No other changes to this file.

```css
/* Hosts the ConnectionBar row — sits above the tool/quick-link rows */
.connectionSection {
  width: 100%;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 0.5rem;
}
```

Add `width: 100%` so the separator line stretches the full nav width even when `.nav` is `flex-wrap: wrap`.

---

### 3.9 Modify `src/components/Playground.tsx` (§3.6)

This is the largest change. Apply all sub-steps in one pass.

**Imports to add:**
```ts
import { Group, Panel, Separator, useDefaultLayout } from 'react-resizable-panels';
import { useMediaQuery } from '../hooks/useMediaQuery';
import ConnectionBar from './ConnectionBar/ConnectionBar';
import LeftPanel from './LeftPanel/LeftPanel';
```

**Imports to remove:**
```ts
// Remove:
import ConnectionStatus from './ConnectionStatus/ConnectionStatus';
import CommandInput from './CommandInput/CommandInput';
import Console from './Console/Console';
```

**State / hook changes:**
- Remove `showConsole` state.
- Remove `submitKey` from `useWebSocket` call options.
- Add `const isMobile = useMediaQuery('(max-width: 768px)')`.
- Add layout persistence:
  ```ts
  // Verified call signature from react-resizable-panels@4.6.5 types.
  // storage accepts localStorage, sessionStorage, or a custom LayoutStorage impl.
  const { defaultLayout, onLayoutChanged } = useDefaultLayout({
    id: config.path + '-panel-split',
    storage: localStorage,
  });
  ```
- Simplify `handleConnect`:
  ```ts
  const handleConnect = () => ws.connect();
  ```

> **`handleFormatPayload` is unchanged and stays in `Playground`.** It is still passed as `onFormat` to `<PayloadEditor>`, which remains a direct child of `Playground` in the right panel. Do not remove or move this function.

**JSX — header:** Pass `ConnectionBar` as `connectionBar` prop to `Navigation`:

> **Complete `ConnectionBar` prop mapping from `Playground`:**
>
> | Prop | Source in `Playground` |
> |---|---|
> | `connected` | `ws.connected` |
> | `connecting` | `ws.connecting` |
> | `url` | `ws.wsUrl` |
> | `onConnect` | `handleConnect` (stable named function, `() => ws.connect()`) |
> | `onDisconnect` | `ws.disconnect` |
>
> All props are `() => void` or primitive — no special typing needed.

```tsx
<header className={styles.header}>
  <h1 className={styles.title}>{title}</h1>
  <Navigation connectionBar={
    <ConnectionBar
      connected={ws.connected}
      connecting={ws.connecting}
      url={ws.wsUrl}
      onConnect={handleConnect}
      onDisconnect={ws.disconnect}
    />
  } />
</header>
```

**JSX — body:** Replace the `.container` div with `Group`:
```tsx
<Group
  className={styles.panelGroup}
  orientation={isMobile ? 'vertical' : 'horizontal'}
  defaultLayout={defaultLayout}
  onLayoutChanged={onLayoutChanged}
>
  <Panel defaultSize="60%" minSize="25%">
    <LeftPanel
      messages={ws.messages}
      autoScroll={ws.autoScroll}
      onToggleAutoScroll={ws.toggleAutoScroll}
      onCopy={ws.copyMessages}
      onClear={ws.clearMessages}
      consoleRef={ws.consoleRef}
      command={ws.command}
      onCommandChange={ws.setCommand}
      onCommandKeyDown={ws.handleKeyDown}
      onSend={ws.sendCommand}
      sendDisabled={!ws.connected || !ws.command.trim()}
      inputDisabled={!ws.connected}
      multiline={multiline}
      onToggleMultiline={() => setMultiline(m => !m)}
    />
  </Panel>
  <Separator className={styles.resizeHandle} aria-label="Resize panels" />
  <Panel defaultSize="40%" minSize="20%">
    <div className={styles.rightPanelContent}>
      <PayloadEditor
        payload={payload}
        onChange={setPayload}
        validation={payloadValidation}
        onFormat={handleFormatPayload}
      />
    </div>
  </Panel>
</Group>
```

---

### 3.10 Modify `src/components/Playground.module.css` (§3.6)

**Delete these rules entirely:**
- `.container`
- `.leftPanel` / `.rightPanel`
- `.card`
- `.inputGroup`
- `.label`
- `.input` / `.input:focus` / `.input:disabled`
- `.buttonGroup`
- `.button` / `.buttonPrimary` / `.buttonWarning`
- `@media (max-width: 768px)` block

**Modify existing rules:**

```css
/* .wrapper — add flex column */
.wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-secondary);   /* was hardcoded #f8f9fa */
}

/* .header — remove margin-bottom */
.header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  padding: 1.5rem 2rem;
  /* margin-bottom: 2rem; ← DELETE */
}
```

**Add new rules:**

```css
/* Fills remaining viewport height below the header */
.panelGroup {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
}

/* Drag handle between panels */
.resizeHandle {
  width: 6px;
  cursor: col-resize;
  background: transparent;
  transition: background 0.15s;
  flex-shrink: 0;
}

.resizeHandle:hover {
  background: rgba(37, 99, 235, 0.2);
}

.resizeHandle[data-separator="active"] {
  background: rgba(37, 99, 235, 0.4);
}

/* Right panel bare wrapper */
.rightPanelContent {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
```

---

## Phase 4 — Cleanup

### 4.1 Delete `ConnectionStatus` files

```bash
rm src/components/ConnectionStatus/ConnectionStatus.tsx
rm src/components/ConnectionStatus/ConnectionStatus.module.css
rmdir src/components/ConnectionStatus
```

> Confirm first: `grep -r "ConnectionStatus" src/` should return only `Playground.tsx` (which will no longer import it after Phase 3 step 3.9).

### 4.2 Clean up the `react-resizable-panels` tarball

```bash
rm /Users/germs/Documents/mercury-composable/extensions/minigraph-playground/webapp/react-resizable-panels-4.6.5.tgz
```

---

## Phase 5 — Verification

### 5.1 TypeScript compile

```bash
npx tsc --noEmit
```

Expected: zero errors.

**Common failure modes to watch for:**
- `messages` typed as `string[]` anywhere → Phase 1 step 1.2 missed a call-site.
- `sendCommand` not in scope → Phase 1 step 1.4 return value incomplete.
- `submitKey` still in `UseWebSocketOptions` → Phase 1 step 1.4 incomplete.
- `ConnectionBar` props mismatch → Phase 2 step 2.2 vs Phase 3 step 3.9 out of sync.

### 5.2 Dev server smoke test checklist

```bash
npm run dev
```

| Scenario | Expected result |
|---|---|
| Page load | Console visible with "No messages yet" placeholder |
| Header | ConnectionBar shows red dot + "Disconnected" + `[Start]` button |
| Click `[Start]` | Dot turns green + pulsing; "Stop Service" button appears; Console shows "connected" info message |
| Type command + Enter | Command sent; Console shows response; textarea cleared; focus returns to textarea |
| `↑` key | Previous command recalled |
| `↑` / `↓` navigation | Scrolls history; `↓` past first entry clears field |
| `load` command | Sends payload from right panel; error if payload empty or too large |
| Shift+Enter (single-line) | Textarea expands — no send |
| Check "Multiline" checkbox | Rows expand to 5; hint changes; Enter no longer sends; Ctrl+Enter sends |
| Panel resize | Drag handle moves panels; cursor changes; handle highlights on hover/drag |
| Double-click handle | Panels reset to 60/40 |
| Keyboard resize | `←`/`→` on focused handle moves by 5 % |
| Resize then refresh | Layout persists (localStorage `useDefaultLayout`) |
| Different playground route | Each route has independent layout persistence |
| Narrow viewport (< 768px) | Panels stack vertically |
| Click `[Stop Service]` | Dot turns red; "Start" button returns; Console shows disconnect message |
| `ConsoleErrorBoundary` | Inject malformed JSON into a message — Console does not unmount; raw text shown |

### 5.3 Accessibility spot-check

- Tab to `<Separator>` → `role="separator"` is present; arrow keys resize.
- Screen reader announces ConnectionBar dot via `aria-label`.
- Send button `aria-disabled="true"` when disabled.

---

## File Change Summary

| File | Change |
|---|---|
| `package.json` | ✅ Already has `react-resizable-panels ^4.6.5` — no change needed |
| `src/index.css` | Add 7 new CSS tokens; add global `@keyframes pulse` |
| `src/config/playgrounds.ts` | `MAX_ITEMS` 30→200; add `MAX_HISTORY = 50` |
| `src/hooks/useMediaQuery.ts` | **NEW** |
| `src/hooks/useToast.ts` | Memoize `addToast`, `removeToast` |
| `src/hooks/useWebSocket.ts` | Message shape; `sendCommand`; slim `handleKeyDown`; remove `submitKey` |
| `src/components/Console/ConsoleErrorBoundary.tsx` | **NEW** |
| `src/components/Console/Console.tsx` | Bare component; `{ id, raw }` messages; error boundary wrap |
| `src/components/Console/Console.module.css` | Flex layout; remove `.card`; token replacements |
| `src/components/CommandInput/CommandInput.tsx` | `onSend`; checkbox; internal key merge; adaptive Send button; focus ref |
| `src/components/CommandInput/CommandInput.module.css` | Adaptive layout; remove `.card` + `.toggleButton`; token replacements |
| `src/components/PayloadEditor/PayloadEditor.tsx` | Bare component; remove `.card`; remove `rows` attr |
| `src/components/PayloadEditor/PayloadEditor.module.css` | Flex layout; remove `.card`; token replacements |
| `src/components/ConnectionBar/ConnectionBar.tsx` | **NEW** |
| `src/components/ConnectionBar/ConnectionBar.module.css` | **NEW** |
| `src/components/LeftPanel/LeftPanel.tsx` | **NEW** |
| `src/components/LeftPanel/LeftPanel.module.css` | **NEW** |
| `src/components/Navigation.tsx` | Add `connectionBar` prop |
| `src/components/Navigation.module.css` | Add `.connectionSection` |
| `src/components/Playground.tsx` | Full rewire: Group/Panel/Separator; ConnectionBar; LeftPanel; remove old layout |
| `src/components/Playground.module.css` | Remove grid; add flex column; add panel/handle rules; token replacements |
| `src/components/ConnectionStatus/ConnectionStatus.tsx` | **DELETED** |
| `src/components/ConnectionStatus/ConnectionStatus.module.css` | **DELETED** |

**Files with NO changes:** `ConsoleMessage.tsx`, `SampleButtons.tsx`, `Toast.tsx/*`, `useLocalStorage.ts`, `App.tsx`, `vite.config.ts`, `index.html`.

---

## Commit Strategy

```
feat(webapp): install react-resizable-panels@^4.6.5
fix(webapp): memoize useToast addToast/removeToast
feat(webapp): add MAX_HISTORY constant; raise MAX_ITEMS to 200
feat(webapp): add CSS tokens; move @keyframes pulse to index.css
feat(webapp): add useMediaQuery hook
fix(webapp): change messages to {id,raw}[] with stable keys; add ConsoleErrorBoundary
refactor(webapp): extract sendCommand from handleKeyDown; remove submitKey
feat(webapp): add ConnectionBar component
feat(webapp): add LeftPanel component
refactor(webapp): Console bare component + flex layout
refactor(webapp): CommandInput – onSend, checkbox, adaptive Send, key merge
refactor(webapp): PayloadEditor bare component + flex layout
feat(webapp): Navigation accepts connectionBar slot
refactor(webapp): Playground – PanelGroup wiring, ConnectionBar, remove old layout
chore(webapp): delete ConnectionStatus (superseded by ConnectionBar)
```
