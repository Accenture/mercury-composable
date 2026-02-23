# Spec: Left-Panel Redesign — Resizable Panel with Console + Command Input
**Version:** 2.3  
**Date:** 2026-02-23  
**Status:** Decisions locked — ready for implementation  

---

## Changelog

| Version | Date | Summary |
|---|---|---|
| 2.1 | 2026-02-22 | Initial draft — resizable panels, bare Console, CommandInput Send button |
| 2.2 | 2026-02-23 | Partial amendments — `sendCommand` extraction, `ConsoleErrorBoundary`, known issues |
| 2.3 | 2026-02-23 | **Full rewrite incorporating all resolved decisions:** multi-connection support; `ConnectionBar` lifted into `Navigation`; `Shift+Enter` always inserts newline; `CommandInput` owns focus via internal `useRef`; `load` payload logic stays in `sendCommand`; `PayloadEditor` becomes bare right-panel fill; `MAX_HISTORY = 50` decoupled from `MAX_ITEMS`; new CSS tokens |

---

## 1. Overview

Version 2.3 reorganises the two-panel layout so that the **Console and its Command Input live together in a unified, resizable left panel**. The right panel holds only the `PayloadEditor`. Connection lifecycle controls (Start/Stop/Connecting) and connection status are lifted into a **`ConnectionBar`** component that lives in the `Navigation` row — making the header a true control panel for the active playground connection.

**Driving motivations:**

1. **Discoverability:** writing a command and watching the response happen in the same column — no left-to-right eye travel.
2. **Control-panel header:** `Navigation` is envisioned as a global control panel for a future iteration. Placing connection controls there now is the correct first step.
3. **Always-visible Console:** the console is mounted and visible from page load — even when empty — because lifecycle events (connect, disconnect) appear in it immediately and hiding it before first connect suppresses useful output.
4. **Multi-connection support:** each playground route mounts its own `Playground` instance with its own `useWebSocket` state. Multiple tabs/routes can be active simultaneously; state is never shared between them.

---

## 2. Layout Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Title │ ConnectionBar (● status · url · [Start] / [Stop Service])      │
│  ──────────────────────────────────────────────────────────────────────  │
│  Tools: [JSON-Path] [Minigraph]   Quick Links: [INFO] [HEALTH] ...      │
└─────────────────────────────────────────────────────────────────────────┘
┌───────────────────────────┬──┬──────────────────────────────────────────┐
│                           │  │                                          │
│  LEFT PANEL (resizable)   │▌ │  RIGHT PANEL (resizable)                 │
│                           │▌ │                                          │
│  ┌─────────────────────┐  │  │  ┌────────────────────────────────────┐  │
│  │                     │  │  │  │  PayloadEditor                     │  │
│  │   Console Output    │  │  │  │  ─────────────────────────────     │  │
│  │   (flex: 1 1 auto)  │  │  │  │  JSON/XML textarea (flex-fill)     │  │
│  │                     │  │  │  │  char count · type badge ·         │  │
│  │                     │  │  │  │  Format button                     │  │
│  └─────────────────────┘  │  │  │  ─────────────────────────────     │  │
│                           │  │  │  Quick load: [JSON: simple] ...    │  │
│  ┌─────────────────────┐  │  │  └────────────────────────────────────┘  │
│  │ CommandInput        │  │  │                                          │
│  │ + Send Button       │  │  │                                          │
│  └─────────────────────┘  │  │                                          │
└───────────────────────────┴──┴──────────────────────────────────────────┘
```

### 2.1 Panel Split

> **⚠️ v4 API note:** This spec targets **`react-resizable-panels ^4.6.5`**. v4 renamed components and replaced several props vs. v3. All examples below use the v4 API. Do **not** use v3 names (`PanelGroup`, `PanelResizeHandle`, `direction`, `autoSaveId`, `keyboardResizeBy`).

| Property | Value |
|---|---|
| Mechanism | `<Group orientation="horizontal">` with two `<Panel>` elements separated by `<Separator>` — `react-resizable-panels ^4.6.5` |
| Default split | `defaultSize="60%"` left / `defaultSize="40%"` right |
| Min left-panel size | `minSize="25%"` |
| Min right-panel size | `minSize="20%"` |
| Resize persistence | Use the `useDefaultLayout` hook (exported from `react-resizable-panels`). Pass `id={config.path + '-panel-split'}` and `storage={localStorage}` to scope each playground route to its own key (e.g. `"react-resizable-panels:/minigraph-panel-split:"`). The hook returns `{ defaultLayout, onLayoutChange }` — pass both to `<Group>`. **Do not** wire to `useLocalStorage`. |
| Responsive stacking | `orientation` switches from `"horizontal"` to `"vertical"` via `useMediaQuery('(max-width: 768px)')` — **custom hook** `src/hooks/useMediaQuery.ts` (see Decision #12) |
| Reset on double-click | **Built-in since v4.5.0.** Double-clicking any `<Separator>` automatically resets its adjacent panel to that panel's `defaultSize`. No `onDoubleClick` handler or imperative `setLayout` call is needed — just ensure `defaultSize` is set on the panels. |
| Keyboard resize | **Built-in.** `←`/`→` arrow keys are handled natively by the library at 5 % per keystroke. No `keyboardResizeBy` prop exists in v4. |

> **Decision #1 — resize library: `react-resizable-panels` ✅**  
> The app will grow into a multi-pane layout (graph visualisation, reorganisable panels). `react-resizable-panels ^4.6.5` (~5 KB gzipped) provides TypeScript-first support, accessible keyboard resize (built-in, 5 % per arrow-key step), nested panel groups, double-click-to-reset (built-in), and `localStorage` persistence via `useDefaultLayout`. No custom `usePanelResize` hook is needed.

### 2.2 Left Panel Internal Layout

The left panel is a **bare flex column** that fills the full height provided by the `<Panel>` element.

```
┌──────────────────────────────────┐  ← LeftPanel root
│                                  │    display: flex; flex-direction: column
│  Console (flex: 1 1 auto)        │    height: 100%; overflow: hidden
│  overflow-y: auto                │
│  min-height: 150px               │
├──────────────────────────────────┤
│  CommandInput + Send             │  ← flex: 0 0 auto (pinned, never scrolls away)
└──────────────────────────────────┘
```

### 2.3 Right Panel Internal Layout

The right panel is also a bare flex column. It contains only `PayloadEditor`, which fills the available space.

```
┌──────────────────────────────────┐  ← right panel wrapper (flex column, height: 100%)
│  PayloadEditor (flex: 1 1 auto)  │
└──────────────────────────────────┘
```

`ConnectionStatus` and the Start/Stop `buttonGroup` are **removed from the right panel entirely** — they move to `ConnectionBar` in the header (see §3.5).

---

## 3. Component Changes

### 3.1 `LeftPanel` (new component)

**File:** `src/components/LeftPanel/LeftPanel.tsx`  
**CSS Module:** `src/components/LeftPanel/LeftPanel.module.css`

**Responsibilities:**
- Owns the flex-column layout from §2.2.
- Renders as the content child of a `<Panel>` — receives no `width` prop.
- Renders `<Console>` (top, grows) and `<CommandInput>` (bottom, fixed).
- Does **not** own WebSocket state — all props passed from `Playground`.

**Props interface:**

```ts
interface LeftPanelProps {
  // Console
  messages:           string[];
  autoScroll:         boolean;
  onToggleAutoScroll: () => void;
  onCopy:             () => void;
  onClear:            () => void;
  consoleRef:         React.RefObject<HTMLDivElement | null>;

  // Command input
  command:           string;
  onCommandChange:   (value: string) => void;
  onCommandKeyDown:  (e: React.KeyboardEvent<HTMLElement>) => void;
  onSend:            () => void;
  sendDisabled:      boolean;
  inputDisabled:     boolean;
  multiline:         boolean;
  onToggleMultiline: () => void;
}
```

> **Note — no `submitKey` prop:** keyboard submit semantics live entirely inside `CommandInput` (§3.3). The `submitKey` option on `useWebSocket` is **removed**.

> **Note — prop name threading for keyboard handler:** `LeftPanel` receives the prop as `onCommandKeyDown` (matches the data-flow diagram in §5). Internally, `LeftPanel` passes it to `CommandInput` as `onKeyDown` — the name `CommandInput` already uses. The two names refer to the same function; only the boundary label differs. Summary: `Playground` → `LeftPanel` as `onCommandKeyDown`; `LeftPanel` → `CommandInput` as `onKeyDown`.

---

### 3.2 `Console` — changes

> **Decision #2 — bare component, always mounted ✅**  
> `Console` drops its `.card` wrapper entirely. It is a layout-neutral, bare component. All outer chrome is provided by the parent panel surface. The component is **always mounted** — the `showConsole` boolean and "Clear & Hide Console" button in `Playground` are **removed**. The console renders from page load; when empty it shows the "No messages yet" placeholder.

**Updated CSS structure:**

```
.consoleRoot    → display: flex; flex-direction: column; height: 100%; overflow: hidden
.consoleHeader  → flex: 0 0 auto  (toolbar row — title + controls)
.console        → flex: 1 1 auto; overflow-y: auto; min-height: 150px  (scrollable messages)
```

`overflow-y: auto` stays on `.console`, **not** on `.consoleRoot` or the panel root. See §4.2.

**`Console.module.css` — complete rule changes** (beyond the token substitutions in §6.2):

| Rule | Change |
|---|---|
| `.card` | **Delete entire rule** — bare component has no card wrapper |
| `.consoleRoot` | **Add** `display: flex; flex-direction: column; height: 100%; overflow: hidden` |
| `.consoleHeader` | Add `flex: 0 0 auto` |
| `.console` | Replace `min-height: 300px; max-height: 600px` with `flex: 1 1 auto; min-height: 150px` — remove `max-height` entirely so the panel drives the height |

---

### 3.3 `CommandInput` — changes

1. **Add `onSend: () => void` prop.** Called by the Send button click and also by the keyboard handler after `e.preventDefault()`.
2. **`CommandInput` owns an internal `useRef<HTMLTextAreaElement>`** and calls `textareaRef.current?.focus()` after invoking `onSend`. Focus management is fully encapsulated — `LeftPanel` does not manage a textarea ref.
3. **`Shift+Enter` always inserts a newline** in both modes. There is no mode in which `Shift+Enter` is a no-op. In single-line mode pressing `Shift+Enter` expands the textarea inline, giving the user a multi-line editing surface without touching the checkbox.
4. **Textarea has `resize: vertical`** — the user can drag the textarea taller in either mode. No custom resize mechanism is needed.
5. **"Multiline" checkbox** (replaces the current toggle button) controls the *default* row count and the send key. Checked → `rows={5}`, `Enter` inserts newline, `Ctrl+Enter` sends. Unchecked → `rows={1}`, `Enter` sends.
6. Hint text updates to reflect the active mode.

**Updated props:**

```ts
interface CommandInputProps {
  command:            string;
  onChange:           (value: string) => void;
  onKeyDown:          (e: React.KeyboardEvent<HTMLElement>) => void;
  onSend:             () => void;      // NEW — triggers send + internal .focus()
  disabled:           boolean;
  multiline?:         boolean;
  onToggleMultiline?: () => void;
}
```

**Key binding table:**

| Mode | Key | Action |
|---|---|---|
| Single-line (default) | `Enter` | Send |
| Single-line | `Shift+Enter` | Insert newline (textarea expands) |
| Single-line | `Ctrl+Enter` | No-op |
| Multiline (checkbox on) | `Enter` | Insert newline |
| Multiline | `Shift+Enter` | Insert newline |
| Multiline | `Ctrl+Enter` | Send |

**`CommandInput` internal `onKeyDown` merge pattern:**

The textarea has a single `onKeyDown` handler. It must serve both purposes — Enter/Ctrl+Enter send logic *and* arrow-key history navigation from the passed-in `onKeyDown` prop. `CommandInput` owns the merge:

```ts
const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
  if (e.key === 'Enter') {
    if (multiline) {
      // Multiline mode: Ctrl+Enter sends, plain Enter is a newline (let browser handle it)
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault();
        onSend();
        textareaRef.current?.focus();
      }
      // else: fall through — browser inserts newline natively
    } else {
      // Single-line mode: Enter sends (unless Shift is held — newline)
      if (!e.shiftKey) {
        e.preventDefault();
        onSend();
        textareaRef.current?.focus();
      }
      // Shift+Enter: fall through — browser expands textarea natively
    }
  } else {
    // All other keys (ArrowUp, ArrowDown, etc.) delegate to the passed-in handler
    onKeyDown(e);
  }
};
```

> **The passed-in `onKeyDown` is only called for non-Enter keys.** This is safe because `handleKeyDown` in `useWebSocket` only acts on `ArrowUp`/`ArrowDown` — it ignores all other keys. There is no double-handling risk.

> **Decision #3 — adaptive Send button placement ✅**  
> **Single-line:** Send button to the **right of the textarea** on the same flex row (chat-input pattern).  
> **Multiline:** Send button **below the textarea**, full-width (form-submit pattern).  
> CSS class swap driven by the `multiline` prop only — no extra state.

> **Decision #3a — `Shift+Enter` always inserts a newline ✅**  
> No mode produces a no-op for `Shift+Enter`. In single-line mode the textarea expands inline. The checkbox controls the *default* state (row count + Enter behaviour); `Shift+Enter` is always a safe escape hatch for multi-line composition.

> **Decision #3b — Multiline toggle UI: checkbox ✅**  
> `<label><input type="checkbox"> Multiline</label>` — semantically correct for a persistent setting; replaces the toggle button.

> **Decision #3c — textarea resize: native `resize: vertical` ✅**  
> Works in both modes. No custom implementation. The right-panel `PayloadEditor` textarea uses `resize: none` because panel resizing serves that role there.

---

### 3.4 `PayloadEditor` — changes (bare component)

`PayloadEditor` currently wraps everything in a `.card` div. Like `Console`, it becomes a **bare component** because it occupies a full panel.

- Remove the outer `.card` wrapper div and its CSS rule.
- Component root becomes `.payloadRoot`: `display: flex; flex-direction: column; height: 100%; overflow: hidden`.
- The `<textarea>` gets `flex: 1 1 auto; resize: none` — panel resizing replaces manual textarea resize.
- `SampleButtons` remains pinned at the bottom (`flex: 0 0 auto`).

**No changes to `SampleButtons.tsx`.**

---

### 3.5 `ConnectionBar` (new component) — lifted into `Navigation`

> **Decision #4 — connection controls in `Navigation` ✅**  
> `Navigation` is envisioned as a control panel for the active playground. Moving Start/Stop/status there is the first step. Multiple simultaneous connections are supported — each playground route mounts its own `Playground` with its own `useWebSocket`, and passes its own `ConnectionBar` props to `Navigation` via a `React.ReactNode` slot. `Navigation` remains decoupled from WebSocket types.

**File:** `src/components/ConnectionBar/ConnectionBar.tsx`  
**CSS Module:** `src/components/ConnectionBar/ConnectionBar.module.css`

**Props:**

```ts
interface ConnectionBarProps {
  connected:    boolean;
  connecting:   boolean;
  url:          string;
  onConnect:    () => void;
  onDisconnect: () => void;
}
```

**Rendered layout (single inline row):**

```
● Connected   ws://localhost:3000/ws/graph/playground   [Stop Service]
● Connecting…  ws://…                                   [Connecting…] (disabled)
● Disconnected ws://…                                   [Start]
```

| Phase | Dot colour | Dot animation | Button label | Button enabled |
|---|---|---|---|---|
| `idle` | `var(--danger-color)` | none | `Start` | ✅ |
| `connecting` | `var(--warning-color)` | pulse | `Connecting…` | ❌ |
| `connected` | `var(--success-color)` | pulse | `Stop Service` | ✅ |

The `@keyframes pulse` animation is **moved from `ConnectionStatus.module.css` to `index.css`** as a global animation so `ConnectionBar.module.css` can reference it without re-declaring it.

**`ConnectionStatus.tsx` and `ConnectionStatus.module.css` are deleted** — superseded by `ConnectionBar`.

**`Navigation` updated props:**

```ts
interface NavigationProps {
  connectionBar?: React.ReactNode;  // NEW — rendered as a dedicated nav section
}
```

**Decision: the `<h1>` title stays in `Playground`'s `<header>` element — it does not move inside `Navigation`.** `Navigation` remains a pure nav/control-panel component.

`Playground` composes `<ConnectionBar .../>` and passes it as `connectionBar` to `<Navigation>`. `Navigation` renders it in a `.connectionSection` at the top of the nav, above the tool links.

**Updated `Playground` header JSX** (the outer `<header>` div and its `<h1>` are unchanged; only `<Navigation>` gains a prop):

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

**Updated `Navigation` JSX structure** (`.connectionSection` is inserted above the existing `.navSection` blocks):

```tsx
export default function Navigation({ connectionBar }: NavigationProps) {
  // ...existing toolLinks / externalLinks arrays unchanged...
  return (
    <nav className={styles.nav}>
      {connectionBar && (
        <div className={styles.connectionSection}>
          {connectionBar}
        </div>
      )}
      <div className={styles.navSection}>
        {/* Tools: links — no change */}
      </div>
      <div className={styles.navSection}>
        {/* Quick Links: links — no change */}
      </div>
    </nav>
  );
}
```

**`Navigation.module.css` — add `.connectionSection`:**

```css
/* Hosts the ConnectionBar — sits above the tool/quick-link rows */
.connectionSection {
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 0.5rem;
}
```

---

### 3.6 `Playground` — changes

> **Current state note:** In the existing code (`Playground.tsx`) the **left** panel (`.leftPanel`) contains `ConnectionStatus`, `PayloadEditor`, `CommandInput`, and the Start/Stop button group. The **right** panel (`.rightPanel`) contains `Console`. This redesign **swaps the panels**: Console + CommandInput move to the left, PayloadEditor moves to the right. Keep this inversion in mind when reading the removal/addition instructions below.

- Remove `showConsole` state and the "Clear & Hide Console" button.
- Remove `submitKey` prop from the `useWebSocket` call.
- Remove `<ConnectionStatus>` and the `buttonGroup` div.
- Replace the CSS-grid two-column layout with `<Group>` + `<Panel>` + `<Separator>` (v4 names — see §2.1).
- Left `<Panel defaultSize="60%" minSize="25%">` renders `<LeftPanel>`.
- Right `<Panel defaultSize="40%" minSize="20%">` renders `<PayloadEditor>` (bare, fills panel).
- Pass `<ConnectionBar>` as `connectionBar` prop to `<Navigation>` in the header (see §3.5 for exact JSX).
- Wire `onSend={ws.sendCommand}` into `<LeftPanel>`.
- Add `useMediaQuery('(max-width: 768px)')` to flip `Group orientation` — from `src/hooks/useMediaQuery.ts` (see Decision #12).
- **`multiline` state stays in `Playground`** (`const [multiline, setMultiline] = useState(false)`) — unchanged from the current code. It is passed down as `multiline={multiline}` and `onToggleMultiline={() => setMultiline(m => !m)}` through `LeftPanel` to `CommandInput`.
- **`handleConnect` post-redesign is a one-liner** — `setShowConsole` is removed (Decision #2), so the function body becomes simply:
  ```ts
  const handleConnect = () => ws.connect();
  ```
  It still exists as a named function so the `ConnectionBar` `onConnect` prop has a stable reference.

**`Playground.module.css` — rule-by-rule migration:**

| Rule | Action | Reason |
|---|---|---|
| `.wrapper` | **Modify** — add `display: flex; flex-direction: column` | Makes `.wrapper` a flex column so `.panelGroup` can use `flex: 1 1 0` to fill remaining height without a hardcoded `calc()` |
| `.header` | **Modify** — remove `margin-bottom: 2rem` | The flex gap between `.header` and `.panelGroup` is now controlled by the flex column, not a margin; keeping the margin adds dead space |
| `.title` | **Keep** — unchanged | `<h1>` colour/size |
| `.container` | **Delete** | Was the CSS-grid two-column wrapper; replaced by `<Group>` |
| `.leftPanel` | **Delete** | Replaced by `<Panel>` + `<LeftPanel>` component |
| `.rightPanel` | **Delete** | Replaced by `<Panel>` |
| `.card` | **Delete** | No longer used in `Playground.tsx` after connection controls move out |
| `.inputGroup` | **Delete** | Was inside the old left-panel card; now lives in child components |
| `.label` | **Delete** | Same — lives in child components |
| `.input` / `.input:focus` / `.input:disabled` | **Delete** | No generic inputs remain in `Playground.tsx` |
| `.buttonGroup` | **Delete** | Start/Stop buttons move to `ConnectionBar` |
| `.button` / `.buttonPrimary` / `.buttonWarning` | **Delete** | Same |
| `@media (max-width: 768px)` block | **Delete** | Responsive stacking handled by `Group orientation` swap via `useMediaQuery` |
| `.panelGroup` | **Add** | `flex: 1 1 0; min-height: 0; display: flex` — flex-grow fills the remaining viewport height below the header. **Do not use `height: calc(100vh - X)`** — the header height is dynamic (nav can wrap) and a flex child with `min-height: 0` is the correct constraint-free approach. |
| `.resizeHandle` | **Add** | `width: 6px; cursor: col-resize; background: transparent; transition: background 0.15s` |
| `.resizeHandle:hover` | **Add** | `background: rgba(37,99,235,0.2)` (= `var(--primary-color)` at 20% opacity) |
| `.resizeHandle[data-separator="active"]` | **Add** | `background: rgba(37,99,235,0.4)` |
| `.rightPanelContent` | **Add** | `display: flex; flex-direction: column; height: 100%; overflow: hidden` — bare wrapper for `PayloadEditor` inside the right `<Panel>` |

> **Note — `Navigation.module.css` `.nav` margin:** The existing `.nav` has `margin-top: 1rem`. With `ConnectionBar` inserted as `.connectionSection` above the tool rows, this top margin stays on the `<nav>` element (providing breathing room below the `<h1>`). The `.connectionSection` `padding: 0.5rem 0; margin-bottom: 0.5rem` keeps the visual gap compact. No change to `.nav`'s `margin-top` is needed — total added nav height from `ConnectionBar` is approximately `~40px` (one inline row), which is acceptable.

---

## 4. Interaction Design

### 4.1 Resizable Drag Handle

| Behaviour | Detail |
|---|---|
| Visual affordance | 6 px wide, `cursor: col-resize`; `background: var(--primary-color)` at 20% opacity on `:hover`; 40% when `[data-separator="active"]` |
| Double-click | **Built-in since v4.5.0** — double-clicking `<Separator>` resets the adjacent panel to its `defaultSize` automatically. No handler needed; ensure both `<Panel>` elements have `defaultSize` set. |
| Keyboard | `←`/`→` arrow keys — **built into `<Separator>`** at 5 % per step (no `keyboardResizeBy` prop in v4). |
| Touch | Handled natively by the library |
| `aria` | Library renders `role="separator"` and `aria-orientation` automatically; add `aria-label="Resize panels"` on `<Separator>` |

> **Decision #5 — mobile layout: stacked panels ✅**  
> Below `768px`, `useMediaQuery` flips `orientation` to `"vertical"`. The handle becomes horizontal. No CSS-only `@media` query needed for the panel orientation.

### 4.2 Console Auto-scroll

> **Decision #6 — scroll anchor: `.console` div owns `overflow-y: auto` ✅**  
> `overflow-y: auto` is on `.console`, not `.consoleRoot` or the `<Panel>`. The `consoleRef` in `useWebSocket` points to `.console`; `scrollTop = scrollHeight` remains correct. If overflow moved to the panel root, `scrollHeight` would equal the full panel height and auto-scroll would silently break. **The scrollable element must be the element where overflow is clipped.**

### 4.3 Send Button UX

| State | Label | Enabled |
|---|---|---|
| Not connected | `Send` | ❌ |
| Connected, `command.trim() === ''` | `Send` | ❌ |
| Connected, input has text | `Send` | ✅ |

> **Decision #7 — empty-string send: silent block ✅**  
> Button `disabled` when `command.trim() === ''` or disconnected. No toast. The disabled visual is sufficient feedback.

### 4.4 Command History

Arrow-key history navigation stays in `useWebSocket.handleKeyDown`, forwarded through `LeftPanel → CommandInput` via `onCommandKeyDown`. No changes to the hook's history logic beyond the dedup described in §5.2.

---

## 5. State & Data Flow

```
Playground (state owner)
│
├── useWebSocket()
│     ws.messages, ws.command, ws.setCommand, ws.sendCommand,
│     ws.handleKeyDown, ws.connected, ws.connecting,
│     ws.connect, ws.disconnect, ws.wsUrl, ws.consoleRef,
│     ws.autoScroll, ws.toggleAutoScroll, ws.copyMessages, ws.clearMessages
│
├── <header>
│   └── <Navigation connectionBar={<ConnectionBar connected={…} connecting={…}
│                                   url={ws.wsUrl} onConnect={handleConnect}
│                                   onDisconnect={ws.disconnect} />} />
│
├── <Group orientation={isMobile ? "vertical" : "horizontal"}
│          defaultLayout={defaultLayout}
│          onLayoutChange={onLayoutChange}
│          groupRef={groupRef}>
│   │
│   │   // useDefaultLayout({ id: config.path + '-panel-split', storage: localStorage })
│   │   // → { defaultLayout, onLayoutChange } wired above; groupRef from useGroupRef()
│   │
│   ├── <Panel defaultSize="60%" minSize="25%">
│   │   └── <LeftPanel
│   │         messages={ws.messages}     autoScroll={ws.autoScroll}
│   │         onToggleAutoScroll={…}     onCopy={…}        onClear={…}
│   │         consoleRef={ws.consoleRef}
│   │         command={ws.command}       onCommandChange={ws.setCommand}
│   │         onCommandKeyDown={ws.handleKeyDown}
│   │         onSend={ws.sendCommand}
│   │         sendDisabled={!ws.connected || !ws.command.trim()}
│   │         inputDisabled={!ws.connected}
│   │         multiline={multiline}      onToggleMultiline={…} />
│   │
│   ├── <Separator className={styles.resizeHandle}
│   │             aria-label="Resize panels" />
│   │   // double-click reset: built-in (resets to Panel defaultSize) — no handler needed
│   │   // keyboard resize: built-in (←/→ at 5% per step) — no keyboardResizeBy prop in v4
│   │
│   └── <Panel defaultSize="40%" minSize="20%">
│       └── <PayloadEditor payload={payload} onChange={setPayload}
│                          validation={payloadValidation} onFormat={handleFormatPayload} />
│
└── <ToastContainer toasts={toasts} onRemove={removeToast} />
```

### 5.1 Updated hook interfaces

**`UseWebSocketOptions`** — `submitKey` is removed entirely (keyboard submit semantics now live in `CommandInput`):

```ts
export interface UseWebSocketOptions {
  wsPath:            string;
  storageKeyHistory: string;
  payload:           string;
  addToast:          (message: string, type?: ToastType) => void;
  // submitKey removed — CommandInput owns all keyboard-submit logic
}
```

**`UseWebSocketReturn`** — `sendCommand` is added; all other members are unchanged:

```ts
export interface UseWebSocketReturn {
  wsUrl:            string;
  connected:        boolean;
  connecting:       boolean;
  messages:         string[];          // shape changes to { id: number; raw: string }[] — see §11.2
  command:          string;
  setCommand:       (value: string) => void;
  connect:          () => void;
  disconnect:       () => void;
  sendCommand:      () => void;        // NEW — extracted from handleKeyDown
  handleKeyDown:    (e: React.KeyboardEvent<HTMLElement>) => void;
  consoleRef:       React.RefObject<HTMLDivElement | null>;
  autoScroll:       boolean;
  toggleAutoScroll: () => void;
  copyMessages:     () => void;
  clearMessages:    () => void;
}
```

### 5.2 `sendCommand` — extracted from `handleKeyDown`

```ts
const sendCommand = useCallback(() => {
  if (!wsRef.current || phase !== 'connected') return;
  const text = command.trim();
  if (!text) return;

  wsRef.current.send(text);

  // Special "load" command: send the payload as a second WebSocket message.
  // The payload panel provides the JSON/XML body; "load" triggers ingestion.
  if (text === 'load') {
    if (!payload) {
      dispatch({ type: 'MESSAGE_RECEIVED',
        msg: eventWithTimestamp('error', 'please paste a JSON/XML payload in the Payload panel') });
    } else if (payload.length > MAX_BUFFER) {
      dispatch({ type: 'MESSAGE_RECEIVED',
        msg: eventWithTimestamp('error', `payload exceeds the ${MAX_BUFFER} character limit`) });
    } else {
      wsRef.current.send(payload);
    }
  }

  // Consecutive-dedup history push
  // ⚠️ Use MAX_HISTORY here — NOT MAX_ITEMS. The old handleKeyDown used
  //    slice(0, MAX_ITEMS) which was a bug: history was capped at the message
  //    buffer size (30). MAX_HISTORY = 50 is the correct limit after Decision #10.
  if (history[0] !== text) {
    setHistory(prev => [text, ...prev].slice(0, MAX_HISTORY));
  }

  dispatch({ type: 'CLEAR_COMMAND' });
}, [phase, command, payload, history, setHistory]);
```

> **Decision #8 — `load` payload logic stays in `sendCommand` ✅**  
> `payload` is already a hook-level dependency. Moving load logic to `Playground` would require re-implementing trim/empty/size checks there. All send semantics belong in the hook.

`handleKeyDown` becomes a thin wrapper: handles only `ArrowUp`/`ArrowDown` history navigation. Enter/Ctrl+Enter are **not** handled here — `CommandInput` calls `onSend()` directly. `Shift+Enter` is never intercepted — the `<textarea>` inserts a newline natively. `submitKey` is **removed** from `UseWebSocketOptions`.

**Full new `handleKeyDown` body:**

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
  // Enter/Ctrl+Enter: CommandInput calls onSend() directly — not handled here.
  // Shift+Enter: native textarea newline insertion — no interception needed.
}, [history, historyIndex]);
```

> **Note — `connected` removed from deps:** History navigation while disconnected is harmless; the user may want to review past commands. Send is guarded independently inside `sendCommand`. The old `if (!connected) return` guard at the top of `handleKeyDown` is deleted.

`UseWebSocketReturn` gains `sendCommand: () => void` (already shown in §5.1).

> **Decision #9 — history deduplication: consecutive-only ✅**  
> `history[0] !== text` before pushing. Mirrors bash `ignoredups`. Non-consecutive repeats are preserved normally.

> **Decision #10 — `MAX_HISTORY = 50`, decoupled from `MAX_ITEMS` ✅**  
> `playgrounds.ts` gains: `export const MAX_HISTORY = 50`. This decouples command-history length from the console message buffer. After this change:  
> - `MAX_ITEMS = 200` — console message buffer (raised from 30; see §11.4)  
> - `MAX_HISTORY = 50` — command history, single value for all playgrounds

---

---

## 6. CSS — Token Updates and Strategy

### 6.1 New tokens added to `index.css`

```css
:root {
  /* ── existing tokens (unchanged) ── */
  --primary-color:  #2563eb;
  --primary-hover:  #1d4ed8;
  --success-color:  #10b981;
  --warning-color:  #f59e0b;
  --danger-color:   #ef4444;
  --bg-primary:     #ffffff;
  --bg-secondary:   #f8fafc;
  --bg-dark:        #1e293b;
  --border-color:   #e2e8f0;
  --text-primary:   #0f172a;
  --text-secondary: #64748b;
  --shadow-sm:      0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md:      0 4px 6px -1px rgb(0 0 0 / 0.1);
  --radius:         0.5rem;

  /* ── NEW tokens ── */

  /* Focus ring — replaces hardcoded rgba(0,123,255,0.25) in 4 files */
  --focus-ring: 0 0 0 0.2rem rgba(37, 99, 235, 0.25);

  /* Small radius — buttons, badges, inline chips (replaces hardcoded 0.25rem) */
  --radius-sm: 0.25rem;

  /* Muted text — timestamps, empty-state copy (lighter than --text-secondary) */
  --text-muted: #94a3b8;

  /* Console surface — replaces inconsistent --bg-dark / #1e293b usage */
  --console-bg:   var(--bg-dark);
  --console-text: #e2e8f0;

  /* Disabled input background — replaces hardcoded #e9ecef */
  --disabled-bg: #e9ecef;

  /* Border for focus-adjacent input states — replaces hardcoded #80bdff */
  --input-focus-border: var(--primary-color);
}

/* Shared pulse animation — moved here from ConnectionStatus.module.css */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.5; }
}
```

### 6.2 Hardcoded-value replacements by file

| File | Hardcoded value | Replace with |
|---|---|---|
| `CommandInput.module.css` | `border: 1px solid #ced4da` | `border: 1px solid var(--border-color)` |
| `CommandInput.module.css` | `border-color: #80bdff` (focus) | `border-color: var(--input-focus-border)` |
| `CommandInput.module.css` | `box-shadow: 0 0 0 0.2rem rgba(0,123,255,0.25)` | `box-shadow: var(--focus-ring)` |
| `CommandInput.module.css` | `background: #e9ecef` (disabled) | `background: var(--disabled-bg)` |
| `CommandInput.module.css` | `color: #495057` (label) | `color: var(--text-secondary)` |
| `PayloadEditor.module.css` | Same `#ced4da`, `#80bdff`, `#e9ecef`, `#495057` | Same replacements as above |
| `PayloadEditor.module.css` | `border-radius: 0.25rem` (buttons, badge) | `border-radius: var(--radius-sm)` |
| `Playground.module.css` | `background-color: #f8f9fa` | `background-color: var(--bg-secondary)` |
| `Playground.module.css` | `color: #495057`, `#ced4da` | `var(--text-secondary)`, `var(--border-color)` |
| `Console.module.css` | `color: #e2e8f0` | `color: var(--console-text)` |
| `Console.module.css` | `background-color: var(--bg-dark)` | `background-color: var(--console-bg)` |
| `Console.module.css` | `color: #64748b` (empty state) | `color: var(--text-secondary)` |
| `Console.module.css` | `color: #94a3b8` (timestamps) | `color: var(--text-muted)` |

> **Decision #11 — CSS strategy: patch with tokens ✅**  
> Replace all hardcoded values with new and existing CSS vars. A shared `.card` utility class is deferred — the `LeftPanel`/`PayloadEditor` refactor already removes the two most duplicated card instances. Remaining `.card` rules are small and indirection is not yet justified.

---

## 7. Accessibility

| Requirement | Implementation |
|---|---|
| Drag handle keyboard-operable | Arrow keys — built into `<Separator>` (§4.1) |
| Console is a live region | `role="log"` + `aria-live="polite"` — no change |
| `ConnectionBar` buttons | `aria-label` on Start/Stop; `disabled` + `aria-disabled="true"` when inactive |
| Status dot | `aria-label="Connected"` / `"Connecting"` / `"Disconnected"` on the dot `<span>` |
| Send button | `aria-label="Send command"`; `disabled` + `aria-disabled="true"` when empty/disconnected |
| Focus after Send | `CommandInput` internally calls `textareaRef.current?.focus()` after `onSend` |
| Color contrast | All buttons use design-token colors — WCAG AA (4.5:1) required for `ConnectionBar` button |

---

## 8. File Structure (post-redesign)

```
src/
  index.css                            ← MODIFIED (new tokens; shared @keyframes pulse)
  components/
    LeftPanel/                         ← NEW
      LeftPanel.tsx
      LeftPanel.module.css
    ConnectionBar/                     ← NEW (replaces ConnectionStatus)
      ConnectionBar.tsx
      ConnectionBar.module.css
    CommandInput/
      CommandInput.tsx                 ← MODIFIED (onSend, Send button, focus ref,
      │                                             keybinding table, checkbox,
      │                                             Shift+Enter always newline)
      CommandInput.module.css          ← MODIFIED (adaptive layout; token replacements)
    Console/
      Console.tsx                      ← MODIFIED (bare — drop .card; flex-fill;
      │                                             always mounted; stable msg id key)
      Console.module.css               ← MODIFIED (flex layout; token replacements)
      ConsoleMessage.tsx               ← NO CHANGE
      ConsoleErrorBoundary.tsx         ← NEW (class component; raw-text fallback)
    PayloadEditor/
      PayloadEditor.tsx                ← MODIFIED (bare — drop .card; flex-fill textarea;
      │                                             resize: none)
      PayloadEditor.module.css         ← MODIFIED (flex layout; token replacements)
      SampleButtons.tsx                ← NO CHANGE
    Playground/
      Playground.tsx                   ← MODIFIED (Group/Separator wiring; ConnectionBar;
      │                                             remove showConsole, submitKey,
      │                                             buttonGroup, ConnectionStatus)
      Playground.module.css            ← MODIFIED (remove grid; add resizeHandle,
                                                    panelGroup, rightPanelContent)
    Navigation/
      Navigation.tsx                   ← MODIFIED (accept connectionBar?: React.ReactNode)
      Navigation.module.css            ← MODIFIED (add .connectionSection)
    ConnectionStatus/                  ← DELETED (superseded by ConnectionBar)
      ConnectionStatus.tsx             ← DELETE
      ConnectionStatus.module.css      ← DELETE
    Toast/                             ← NO CHANGE
  config/
    playgrounds.ts                     ← MODIFIED (add MAX_HISTORY = 50; raise MAX_ITEMS to 200)
  hooks/
    useWebSocket.ts                    ← MODIFIED (extract sendCommand; add to return type;
    │                                              remove submitKey; MAX_HISTORY dedup;
    │                                              message shape { id, raw })
    useToast.ts                        ← MODIFIED (wrap addToast/removeToast in useCallback)
    useLocalStorage.ts                 ← NO CHANGE
    useMediaQuery.ts                   ← NEW (Decision #12 — custom hook, no new dependency)
```

**New dependency:**

```
react-resizable-panels ^4.6.5   ~5 KB gzipped, TypeScript-first
npm install react-resizable-panels@^4.6.5
```

> **v4 imports:** Use `import { Group, Panel, Separator, useGroupRef, useDefaultLayout } from 'react-resizable-panels';`  
> Do **not** use v3 names: `PanelGroup`, `PanelResizeHandle`, `direction`, `autoSaveId`, `keyboardResizeBy`.

**New hook — `src/hooks/useMediaQuery.ts`** (Decision #12 — write inline, no package):

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

**Vite dev proxy** — ensure `vite.config.ts` proxies WebSocket paths in dev:

```ts
server: {
  proxy: {
    '/ws': { target: 'ws://localhost:3000', ws: true }
  }
}
```

---

## 9. Decisions Log

| # | Question | Decision | Rationale |
|---|---|---|---|
| 1 | Resize library vs. custom hook? | **`react-resizable-panels ^4.6.5`** | Multi-pane growth path; built-in keyboard resize, built-in double-click reset, and accessibility; `useDefaultLayout` hook for `localStorage` persistence |
| 2 | Console always visible? | **Yes — remove `showConsole` toggle** | Live region; lifecycle events appear immediately; hiding suppresses useful output |
| 3 | Send button placement? | **Adaptive** — right (single-line) / full-width below (multiline) | Chat UX in single-line; form-submit UX in multiline |
| 3a | `Shift+Enter` behaviour? | **Always inserts a newline** — never a no-op | Checkbox controls default state only; `Shift+Enter` is always a newline escape hatch |
| 3b | Multiline toggle UI? | **Checkbox** | Semantically correct for a persistent on/off setting |
| 3c | Textarea resize? | **Native `resize: vertical`** | No custom mechanism needed; works in both modes |
| 4 | Connection controls location? | **`ConnectionBar` in `Navigation`** | Header = control panel vision; decoupled via `React.ReactNode` prop |
| 5 | Mobile layout? | **Stacked panels at `< 768px`** | `react-resizable-panels` handles via `orientation` prop swap |
| 6 | Scroll anchor? | **`.console` div owns `overflow-y: auto`** | Panel-root overflow would break `scrollTop`/`scrollHeight` auto-scroll |
| 7 | Empty-string send? | **Silent block via `disabled`** | Disabled visual is sufficient; no toast noise |
| 8 | `load` logic location? | **Stays in `sendCommand`** | Hook owns all send semantics; avoids re-implementing checks in `Playground` |
| 9 | History deduplication? | **Consecutive-only** | Mirrors bash `ignoredups`; non-consecutive repeats preserved |
| 10 | History size constant? | **`MAX_HISTORY = 50` — one value for all playgrounds** | Decouples history size from console buffer (`MAX_ITEMS`); single source of truth |
| 11 | CSS strategy? | **Patch with new tokens** | Targeted replacements; shared utility class deferred |
| 12 | `useMediaQuery` source? | **Custom hook `src/hooks/useMediaQuery.ts`** | ~15 lines; zero new dependencies; fits existing `hooks/` pattern; avoids adding a package for a single boolean |
| 13 | Panel layout persistence scope? | **Per-playground `id` for `useDefaultLayout`:** `config.path + '-panel-split'` | A single hardcoded `id` shared across all routes causes one playground's saved split to overwrite another's. Pass as `id` to `useDefaultLayout({ id: config.path + '-panel-split', storage: localStorage })` |

---

## 10. Out of Scope for This Iteration

- Further redesign of the right panel beyond making `PayloadEditor` a bare fill component.
- Changes to `Toast`, `ConsoleMessage`, or `SampleButtons`.
- Dark mode or theming overhaul.
- Mobile-first layout (responsive fallback only).
- `useLocalStorage` stale-key fix (risk contained; panel persistence uses `useDefaultLayout`).

---

## 11. Pre-existing Issues — Fix During v2 Build

These code issues were identified during v2.3 analysis. Resolve in the same PR.

### 11.1 `useToast` — `addToast`/`removeToast` not memoised 🟡

Both functions are recreated on every render, causing all `useCallback` dependants in `useWebSocket` to re-create on every toast state change.

**Fix:**
```ts
const addToast = useCallback((message: string, type: ToastType = 'info') => {
  const id = Date.now() + Math.random();
  setToasts(prev => [...prev, { id, message, type }]);
  setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 3000);
}, []);

const removeToast = useCallback((id: number) => {
  setToasts(prev => prev.filter(t => t.id !== id));
}, []);
```

### 11.2 Console message key — array index as React key 🟡

With FIFO eviction (`msgs.shift()`), every retained message's index changes on each new arrival. React re-renders all rows on every new message.

**Fix:** Change `WsState.messages` from `string[]` to `{ id: number; raw: string }[]`. Assign auto-incrementing `id` in the reducer. Use `msg.id` as the React key.

> **⚠️ Co-dependency with §11.3:** §11.2 and §11.3 **must be implemented together in one pass.** Changing `messages` to `{ id, raw }[]` requires updating the `map` in `Console.tsx` (key changes from `idx` to `msg.id`, prop passed to `<ConsoleMessage>` changes from `msg` to `msg.raw`). `ConsoleErrorBoundary` wraps each `<ConsoleMessage key={msg.id} message={msg.raw} />` at the same time. Attempting one without the other will produce TypeScript errors.

### 11.3 `ConsoleMessage` — no error boundary 🟡

`react-json-view-lite` can throw on malformed server data, unmounting the entire console silently.

**Fix:** Add `ConsoleErrorBoundary.tsx` (class component). Wrap each `<ConsoleMessage>` in it. On error, render the raw message string as plain text.

> **⚠️ Co-dependency with §11.2:** See note above — implement together.

**`ConsoleErrorBoundary` TypeScript props interface:**

```ts
interface ConsoleErrorBoundaryProps {
  fallback: string;           // raw message text to render if child throws
  children: React.ReactNode;
}

interface ConsoleErrorBoundaryState {
  hasError: boolean;
}

class ConsoleErrorBoundary extends React.Component<
  ConsoleErrorBoundaryProps,
  ConsoleErrorBoundaryState
> {
  state = { hasError: false };

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

**Updated `Console.tsx` message list** (after both §11.2 and §11.3 are applied):

```tsx
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
```

### 11.4 `MAX_ITEMS = 30` — too low for a full-height console panel 🟡

The v1 console had `max-height: 600px`. The v2 console fills the full left-panel height. 30 messages caps out well before the panel looks full.

**Fix:** Raise `MAX_ITEMS` to `200`. Memory impact is negligible (~200 × ~200 bytes ≈ 40 KB).

### 11.5 `ConnectionStatus` — no `aria-live` region 🟡

Connection state changes are visual-only; screen readers receive no announcement.

**Fix:** Handled by `ConnectionBar` design — `aria-label` on the status dot `<span>` (§7).
