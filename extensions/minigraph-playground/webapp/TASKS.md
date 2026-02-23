# Task List — Left-Panel Redesign v2.3

**Plan:** `IMPLEMENTATION-PLAN-v2.md`  
**Spec:** `SPEC-v2-left-panel.md`  
**Branch:** `feature/minigraph-playground`  
**Generated:** 2026-02-23

Mark each task `[x]` when complete. Tasks within a phase that have no listed dependency can be done in any order.

---

## Phase 0 — Foundation

> All Phase 0 tasks must complete before any Phase 1–4 work begins.

- [ ] **0.1** Verify `react-resizable-panels@^4.6.5` is installed
  - Run `grep react-resizable-panels package.json` — confirm `^4.6.5` is present
  - No `npm install` needed; package is already in `package.json` and `node_modules`

- [ ] **0.2** Update `src/config/playgrounds.ts`
  - Change `MAX_ITEMS` from `30` → `200`
  - Add `export const MAX_HISTORY = 50;`
  - Both constants must be exported (hook imports `MAX_HISTORY`)

- [ ] **0.3** Update `src/index.css`
  - **A.** Add 7 new CSS custom properties inside `:root` after `--radius`:
    `--focus-ring`, `--radius-sm`, `--text-muted`, `--console-bg`, `--console-text`, `--disabled-bg`, `--input-focus-border`
  - **B.** Add `@keyframes pulse` as a global rule (after `#root`, before component selectors) — move it out of `ConnectionStatus.module.css`

- [ ] **0.4** Create `src/hooks/useMediaQuery.ts`
  - New file; no new package dependency
  - `export function useMediaQuery(query: string): boolean`
  - Uses `useState(() => window.matchMedia(query).matches)` + `useEffect` with `mql.addEventListener('change', handler)`

---

## Phase 1 — Hook Changes

> Apply all Phase 1 tasks as a **single commit** to avoid intermediate TypeScript errors.
> **0.2 must be complete** before starting (needs `MAX_HISTORY`).

- [ ] **1.1** Fix `src/hooks/useToast.ts` — memoize callbacks
  - Add `useCallback` to import
  - Wrap `addToast` in `useCallback(..., [])` — preserve existing body
  - Wrap `removeToast` in `useCallback(..., [])` — preserve existing body

- [ ] **1.2** Change `messages` shape in `src/hooks/useWebSocket.ts`: `string[]` → `{ id: number; raw: string }[]`
  - **A.** Update `WsState.messages` type
  - **B.** Add `const msgIdRef = useRef(0)` alongside other refs
  - **C.** Update `initialState.messages` to `[]` (type now inferred as `{ id; raw }[]`)
  - **D.** Add `id: number` field to `CONNECTED`, `MESSAGE_RECEIVED`, `DISCONNECTED` action variants in the union
  - **E.** Update the three reducer cases (`CONNECTED`, `MESSAGE_RECEIVED`, `DISCONNECTED`) to push `{ id: action.id, raw: action.msg }` instead of `action.msg`
  - **F.** Update the four dispatch call-sites to pass `id: ++msgIdRef.current` (onopen, onmessage, onclose, disconnect-fallback)
  - **G.** Update `UseWebSocketReturn.messages` type to `{ id: number; raw: string }[]`
  - **H.** Fix `copyMessages`: change `messages.join('\n')` → `messages.map(m => m.raw).join('\n')` *(silent runtime bug without this fix)*

- [ ] **1.3** Extract `sendCommand` from `handleKeyDown` in `src/hooks/useWebSocket.ts`
  - **A.** Add `MAX_HISTORY` to the import from `../config/playgrounds`
  - **B.** Add new `sendCommand` `useCallback` before `handleKeyDown`:
    - Guard: `if (!wsRef.current || phase !== 'connected') return`
    - `ws.send(text)`; handle `load` command with `payload` / `MAX_BUFFER` checks (dispatching via `msgIdRef`)
    - Consecutive-dedup history: `if (history[0] !== text) setHistory(prev => [text, ...prev].slice(0, MAX_HISTORY))`
    - Dispatch `CLEAR_COMMAND`
    - Deps: `[phase, command, payload, history, setHistory]`
  - **C.** Strip all Enter/send logic from `handleKeyDown`; remove `connected` guard and `submitKey` reference; keep only ArrowUp/ArrowDown history navigation
    - Deps: `[history, historyIndex]`

- [ ] **1.4** Remove `submitKey` from `src/hooks/useWebSocket.ts`
  - Delete `submitKey?: 'enter' | 'ctrl+enter'` from `UseWebSocketOptions`
  - Remove `submitKey = 'enter'` from hook destructuring
  - Add `sendCommand: () => void` to `UseWebSocketReturn` interface
  - Add `sendCommand` to the `return` statement

---

## Phase 2 — New Components

> **2.1 must complete before Phase 3 step 3.1** (Console imports it).
> 2.2 and 2.3 are independent of each other and can run in parallel after Phase 0.

- [ ] **2.1** Create `src/components/Console/ConsoleErrorBoundary.tsx`
  - Class component: `ConsoleErrorBoundaryProps { fallback: string; children: React.ReactNode }`
  - `state = { hasError: false }`
  - `static getDerivedStateFromError()` returns `{ hasError: true }`
  - `render()`: if `hasError` → `<span>{this.props.fallback}</span>`, else `this.props.children`
  - Named export: `export class ConsoleErrorBoundary`

- [ ] **2.2** Create `src/components/ConnectionBar/ConnectionBar.tsx`
  - Props: `connected: boolean`, `connecting: boolean`, `url: string`, `onConnect: () => void`, `onDisconnect: () => void`
  - Renders single inline flex row: dot + status label + url + conditional button
  - Dot classes: `.dotIdle` / `.dotConnecting` (pulse 1.5s) / `.dotConnected` (pulse 2s)
  - Button states: idle → `Start`; connecting → `Connecting…` (disabled); connected → `Stop Service`
  - All `aria-label` attributes per plan §2.2

- [ ] **2.3** Create `src/components/ConnectionBar/ConnectionBar.module.css`
  - `.bar`: `display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap`
  - `.dot`: 10 × 10 px circle, `flex-shrink: 0`
  - `.dotIdle` / `.dotConnecting` / `.dotConnected` with `var(--danger/warning/success-color)`; connecting + connected use `animation: pulse` (global, defined in `index.css`)
  - `.status`, `.url` (monospace, `flex: 1 1 0`, `text-overflow: ellipsis`), `.button` styles per plan
  - Uses `var(--radius-sm)` and `var(--primary-color)` tokens (requires 0.3 to be done first)

- [ ] **2.4** Create `src/components/LeftPanel/LeftPanel.tsx`
  - Props: Console group (`messages`, `autoScroll`, `onToggleAutoScroll`, `onCopy`, `onClear`, `consoleRef`) + CommandInput group (`command`, `onCommandChange`, `onCommandKeyDown`, `onSend`, `sendDisabled`, `inputDisabled`, `multiline`, `onToggleMultiline`)
  - Renders `<div className={styles.root}><Console .../><CommandInput .../></div>`
  - Passes `onCommandKeyDown` to `CommandInput` as `onKeyDown` *(different prop name at each boundary — do not rename either end)*

- [ ] **2.5** Create `src/components/LeftPanel/LeftPanel.module.css`
  - Single rule: `.root { display: flex; flex-direction: column; height: 100%; overflow: hidden; }`

---

## Phase 3 — Modified Components

> **Dependencies within this phase:**
> - **3.1** requires Phase 1 (1.2, 1.3) and Phase 2 (2.1) to be complete
> - **3.7** must complete before **3.9**
> - **3.9** is last — it wires everything together

- [ ] **3.1** Modify `src/components/Console/Console.tsx`
  - Import `ConsoleErrorBoundary` from `./ConsoleErrorBoundary`
  - Update `ConsoleProps.messages` to `{ id: number; raw: string }[]`
  - Replace root `<div className={styles.card}>` with `<div className={styles.consoleRoot}>`
  - Replace `messages.map((msg, idx) => <ConsoleMessage key={idx} message={msg} />)` with:
    ```tsx
    messages.map((msg) => (
      <ConsoleErrorBoundary key={msg.id} fallback={msg.raw}>
        <ConsoleMessage message={msg.raw} />
      </ConsoleErrorBoundary>
    ))
    ```
  - Update empty-state text to: `No messages yet. Use the <strong>Start</strong> button in the header to connect.`
  - **`ConsoleMessage.tsx` is NOT changed** — it still accepts `message: string`; `msg.raw` satisfies that

- [ ] **3.2** Modify `src/components/Console/Console.module.css`
  - Delete `.card` rule entirely
  - Add `.consoleRoot { display: flex; flex-direction: column; height: 100%; overflow: hidden; }`
  - Add `flex: 0 0 auto` to `.consoleHeader`
  - Replace `.console` `min-height: 300px; max-height: 600px` with `flex: 1 1 auto; min-height: 150px` (remove `max-height`)
  - Token replacements: `color: #e2e8f0` → `var(--console-text)` · `background-color: var(--bg-dark)` → `var(--console-bg)` · `color: #64748b` → `var(--text-secondary)` · `color: #94a3b8` → `var(--text-muted)`

- [ ] **3.3** Modify `src/components/CommandInput/CommandInput.tsx`
  - Add `import { useRef } from 'react'`
  - Add props: `onSend: () => void`, `sendDisabled: boolean` to `CommandInputProps`; rename `disabled` → keep as `disabled: boolean` *(controls textarea, not Send button)*
  - Replace toggle `<button>` with `<label><input type="checkbox" checked={multiline} onChange={onToggleMultiline} /> Multiline</label>`
  - Add `const textareaRef = useRef<HTMLTextAreaElement>(null)` and attach `ref={textareaRef}` to the textarea
  - Replace `onKeyDown={onKeyDown}` pass-through with internal merge handler:
    - `Enter` key: if multiline → Ctrl/Meta+Enter calls `onSend()` + focus; plain Enter falls through. If single-line → non-Shift Enter calls `onSend()` + focus; Shift+Enter falls through
    - All other keys (ArrowUp/Down etc.) → delegate to passed-in `onKeyDown(e)`
  - Render Send button: single-line → right of textarea in `.inputRow`; multiline → full-width below textarea with `.sendButtonFullWidth`
  - Send button uses `disabled={sendDisabled}` *(separate from textarea `disabled`)*
  - Update hint text and placeholder strings per plan §3.3

- [ ] **3.4** Modify `src/components/CommandInput/CommandInput.module.css`
  - Delete: `.card`, `.toggleButton`, `.toggleButton:hover`, `.toggleButton.active`, `.toggleButton.active:hover`
  - Add: `.inputRow` (flex row for single-line layout), `.inputRow .textarea` (`flex: 1 1 auto`), `.sendButtonFullWidth`, `.sendButton`, `.checkboxLabel`
  - Token replacements: `#ced4da` → `var(--border-color)` · `#80bdff` → `var(--input-focus-border)` · `rgba(0,123,255,0.25)` → `var(--focus-ring)` · `#e9ecef` → `var(--disabled-bg)` · `color: #495057` → `var(--text-secondary)`

- [ ] **3.5** Modify `src/components/PayloadEditor/PayloadEditor.tsx`
  - Replace root `<div className={styles.card}>` with `<div className={styles.payloadRoot}>`
  - Remove the `.inputGroup` wrapper `<div>` — its children become direct children of `.payloadRoot`
  - Wrap `<SampleButtons>` in `<div className={styles.sampleButtonsRow}>`
  - Remove `rows={8}` from textarea
  - **No `disabled` prop added** — textarea is always enabled; `load` command validates at send time

- [ ] **3.6** Modify `src/components/PayloadEditor/PayloadEditor.module.css`
  - Delete: `.card`, `.inputGroup`
  - Add: `.payloadRoot { display: flex; flex-direction: column; height: 100%; overflow: hidden; padding: 1rem; }`, `.sampleButtonsRow { flex: 0 0 auto; padding-top: 0.5rem; }`
  - Update `.textarea`: add `flex: 1 1 auto; resize: none; min-height: 0`
  - Token replacements: `#ced4da` → `var(--border-color)` · `#80bdff` → `var(--input-focus-border)` · `rgba(0,123,255,0.25)` → `var(--focus-ring)` · `#e9ecef` → `var(--disabled-bg)` · `color: #495057` → `var(--text-secondary)` · `border-radius: 0.25rem` → `var(--radius-sm)`

- [ ] **3.7** Modify `src/components/Navigation.tsx`
  - Add `interface NavigationProps { connectionBar?: React.ReactNode; }`
  - Add `connectionBar` to destructured props
  - Insert as first child of `<nav>`: `{connectionBar && <div className={styles.connectionSection}>{connectionBar}</div>}`

- [ ] **3.8** Modify `src/components/Navigation.module.css`
  - Add `.connectionSection { width: 100%; padding: 0.5rem 0; border-bottom: 1px solid var(--border-color); margin-bottom: 0.5rem; }`
  - No other changes to this file

- [ ] **3.9** Modify `src/components/Playground.tsx` *(do this last — pulls everything together)*
  - **Imports to add:** `Group`, `Panel`, `Separator`, `useDefaultLayout` from `react-resizable-panels`; `useMediaQuery` from `../hooks/useMediaQuery`; `ConnectionBar` from `./ConnectionBar/ConnectionBar`; `LeftPanel` from `./LeftPanel/LeftPanel`
  - **Imports to remove:** `ConnectionStatus`, `CommandInput`, `Console`
  - **State removals:** delete `showConsole` state; remove `submitKey` from `useWebSocket` call
  - **New hooks/state:**
    - `const isMobile = useMediaQuery('(max-width: 768px)')`
    - `const { defaultLayout, onLayoutChanged } = useDefaultLayout({ id: config.path + '-panel-split', storage: localStorage })`
  - **Simplify `handleConnect`:** `const handleConnect = () => ws.connect();`
  - **`handleFormatPayload` is unchanged** — keep as-is, still passed to `<PayloadEditor>`
  - **Header JSX:** pass `<ConnectionBar connected={ws.connected} connecting={ws.connecting} url={ws.wsUrl} onConnect={handleConnect} onDisconnect={ws.disconnect} />` as `connectionBar` prop to `<Navigation>`
  - **Body JSX:** replace `.container` div with `<Group>` / `<Panel>` / `<Separator>` / `<Panel>` structure per plan §3.9; left panel renders `<LeftPanel>` with all props wired; right panel renders `<PayloadEditor>` inside `.rightPanelContent`
  - `sendDisabled` passed to `LeftPanel` as `{!ws.connected || !ws.command.trim()}`; `inputDisabled` as `{!ws.connected}`

- [ ] **3.10** Modify `src/components/Playground.module.css`
  - **Delete rules:** `.container`, `.leftPanel`, `.rightPanel`, `.card`, `.inputGroup`, `.label`, `.input`, `.input:focus`, `.input:disabled`, `.buttonGroup`, `.button`, `.buttonPrimary`, `.buttonWarning`, `@media (max-width: 768px)` block
  - **Modify `.wrapper`:** add `display: flex; flex-direction: column`; change `background-color: #f8f9fa` → `var(--bg-secondary)`
  - **Modify `.header`:** remove `margin-bottom: 2rem`
  - **Keep `.title`** unchanged
  - **Add `.panelGroup`:** `flex: 1 1 0; min-height: 0; display: flex`
  - **Add `.resizeHandle`:** `width: 6px; cursor: col-resize; background: transparent; transition: background 0.15s; flex-shrink: 0`
  - **Add `.resizeHandle:hover`:** `background: rgba(37, 99, 235, 0.2)`
  - **Add `.resizeHandle[data-separator="active"]`:** `background: rgba(37, 99, 235, 0.4)`
  - **Add `.rightPanelContent`:** `display: flex; flex-direction: column; height: 100%; overflow: hidden`

---

## Phase 4 — Cleanup

- [ ] **4.1** Delete `ConnectionStatus` component
  - Confirm `grep -r "ConnectionStatus" src/` returns no hits (after 3.9 is done)
  - Delete `src/components/ConnectionStatus/ConnectionStatus.tsx`
  - Delete `src/components/ConnectionStatus/ConnectionStatus.module.css`
  - Remove the now-empty `src/components/ConnectionStatus/` directory

---

## Phase 5 — Verification

- [ ] **5.1** TypeScript compile check
  - Run `npx tsc --noEmit`
  - Expected: zero errors
  - Common failure modes:
    - `messages` typed as `string[]` anywhere → 1.2 missed a call-site
    - `sendCommand` not in scope → 1.4 return value incomplete
    - `submitKey` still in `UseWebSocketOptions` → 1.4 incomplete
    - `ConnectionBar` props mismatch → 2.2 vs 3.9 out of sync

- [ ] **5.2** Dev server smoke test
  - Run `npm run dev`
  - Work through the full checklist in plan §5.2 (page load → connect → send → history → load → multiline → panel resize → double-click reset → keyboard resize → persist on refresh → mobile stack → disconnect → error boundary)

- [ ] **5.3** Accessibility spot-check
  - Tab to `<Separator>` → `role="separator"` present; arrow keys resize
  - Screen reader announces `ConnectionBar` dot via `aria-label`
  - Send button has `aria-disabled="true"` when disabled

---

## Commit Strategy

One commit per logical unit (can be squashed before merge):

```
fix(webapp): memoize useToast addToast/removeToast
feat(webapp): add MAX_HISTORY constant; raise MAX_ITEMS to 200
feat(webapp): add CSS tokens; move @keyframes pulse to index.css
feat(webapp): add useMediaQuery hook
fix(webapp): change messages to {id,raw}[]; fix copyMessages; add msgIdRef
refactor(webapp): extract sendCommand; slim handleKeyDown; remove submitKey
feat(webapp): add ConsoleErrorBoundary
feat(webapp): add ConnectionBar component + CSS
feat(webapp): add LeftPanel component + CSS
refactor(webapp): Console bare component + flex layout + error boundary wrap
refactor(webapp): CommandInput – onSend, checkbox, adaptive Send, key merge
refactor(webapp): PayloadEditor bare component + flex layout
feat(webapp): Navigation accepts connectionBar slot
refactor(webapp): Playground – Group/Panel wiring, ConnectionBar, remove old layout
chore(webapp): delete ConnectionStatus (superseded by ConnectionBar)
```

---

## Files Changed at a Glance

| File | Action |
|---|---|
| `package.json` | ✅ No change — `react-resizable-panels@^4.6.5` already present |
| `src/index.css` | Modify — add 7 tokens + global `@keyframes pulse` |
| `src/config/playgrounds.ts` | Modify — `MAX_ITEMS` 30→200; add `MAX_HISTORY = 50` |
| `src/hooks/useMediaQuery.ts` | **New** |
| `src/hooks/useToast.ts` | Modify — memoize `addToast`, `removeToast` |
| `src/hooks/useWebSocket.ts` | Modify — `{id,raw}[]` messages; `sendCommand`; slim `handleKeyDown`; remove `submitKey`; fix `copyMessages` |
| `src/components/Console/ConsoleErrorBoundary.tsx` | **New** |
| `src/components/Console/Console.tsx` | Modify — bare component; `{id,raw}` messages; error boundary wrap; new empty-state text |
| `src/components/Console/Console.module.css` | Modify — flex layout; remove `.card`; token replacements |
| `src/components/CommandInput/CommandInput.tsx` | Modify — `onSend`; checkbox; internal key merge; adaptive Send; `textareaRef` |
| `src/components/CommandInput/CommandInput.module.css` | Modify — adaptive layout; remove `.card` + `.toggleButton`; token replacements |
| `src/components/PayloadEditor/PayloadEditor.tsx` | Modify — bare component; remove `.card`; remove `rows`; wrap `SampleButtons` |
| `src/components/PayloadEditor/PayloadEditor.module.css` | Modify — flex layout; remove `.card`; token replacements |
| `src/components/ConnectionBar/ConnectionBar.tsx` | **New** |
| `src/components/ConnectionBar/ConnectionBar.module.css` | **New** |
| `src/components/LeftPanel/LeftPanel.tsx` | **New** |
| `src/components/LeftPanel/LeftPanel.module.css` | **New** |
| `src/components/Navigation.tsx` | Modify — add `connectionBar` prop |
| `src/components/Navigation.module.css` | Modify — add `.connectionSection` |
| `src/components/Playground.tsx` | Modify — full rewire: Group/Panel/Separator; ConnectionBar; LeftPanel; remove old layout |
| `src/components/Playground.module.css` | Modify — remove grid; add flex column + panel/handle rules; token replacements |
| `src/components/ConnectionStatus/ConnectionStatus.tsx` | **Deleted** |
| `src/components/ConnectionStatus/ConnectionStatus.module.css` | **Deleted** |
| `src/components/ConsoleMessage.tsx` | ✅ No change |
| `src/components/PayloadEditor/SampleButtons.tsx` | ✅ No change |
| `src/components/Toast.tsx` / `Toast.module.css` | ✅ No change |
| `src/hooks/useLocalStorage.ts` | ✅ No change |
| `src/App.tsx` | ✅ No change |
