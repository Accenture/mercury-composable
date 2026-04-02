# Command Input — History Autocomplete (Phase 2)

**Status:** Final
**Version:** 1.11
**Phase:** Command Input Revamp — Phase 2 (history-based autocomplete)
**Depends on:** [SPEC-command-input-chat-style.md](./SPEC-command-input-chat-style.md) (Phase 1 — implemented)
**Last updated:** March 27 2026

---

## Changelog

| Version | Date | Changes |
|---|---|---|
| 1.0 | 2026-03-27 | Initial draft |
| 1.1 | 2026-03-27 | All five decision points resolved: DP-1 → prefix match; DP-2 → wrap around; DP-3 → no-op when closed; DP-4 → keep dormant; DP-5 → no highlighting. Spec finalised. |
| 1.2 | 2026-03-27 | Addressed review gaps: `accept()` JSDoc clarified — explicit `index` param supports both keyboard and mouse-click call sites (§12.1); dropup item `onMouseDown`+`onClick` pair made explicit in §12.2 item 9; `onChange` prop naming collision resolved and React 19 batching ordering note added to §12.2 item 2. Gap 1 (Playground.tsx) confirmed already covered by §12.6 — change is minimal and fully specified there. |
| 1.3 | 2026-03-27 | Fixed 2 bugs: §5.1 arrow-key guard strengthened to `ac.isOpen && ac.suggestions.length > 0` to prevent `% 0 = NaN`; §5.2 + §12.2 item 9 — rAF caret placement added to mouse-click handler. Fixed CSS inconsistency: §11.5 updated to use `--autocomplete-highlight` token; §12.3 `aria-selected` rule gains `color: var(--text-primary)`. Fixed 3 clarity issues: §7.2 — `isOpen` wording corrected to JSX guard; §11.2 — `onChangeWrapper` inlined; §13.11 — "or stops" removed (DP-2 resolved as wrap). |
| 1.4 | 2026-03-27 | Fixed CSS opacity mismatch (§11.5 vs §12.3): §11.5 opacity reverted to `0.1` to match canonical §12.3; §11.5 prose corrected. Fixed §12.2 item 5: added `&& ac.suggestions.length > 0` guard to mirror §5.1, preventing `% 0 = NaN` if list empties while dropup is open. Fixed §12.2 items 6 + 7: added `requestAnimationFrame` caret placement to Enter and Tab keyboard-accept paths (§5.2 already stated this was required but items 6 and 7 lacked it). Fixed §13.7 typo: `() => e.preventDefault()` → `e => e.preventDefault()`. Added §11.2 note: `onClick` template is ARIA-focused; full rAF handler is in §12.2 item 9. |
| 1.5 | 2026-03-27 | Fixed §12.2 item 7: rAF guarded on `ac.isOpen && ac.suggestions.length > 0` to prevent unconditional caret jump when Tab is a no-op (dropup closed). Fixed §11.1 ARIA bug: `aria-expanded` changed to `ac.isOpen && ac.suggestions.length > 0` to reflect actual popup visibility; §6 Tab branch code example updated to match. Fixed §11.1 + §11.2 ARIA bug: listbox container always rendered with `hidden` attribute so `aria-controls="history-dropup"` always resolves; items rendered only when visible. Updated §5.1 invariant note to acknowledge `suggestions.length === 0` reachable path. |
| 1.6 | 2026-03-27 | Critical: added `className={styles.dropup}` to listbox container in §11.2 (missing class made all positioning inert); §12.2 item 9 note updated to call out the need to merge §8.1 CSS class and §11.2 ARIA attributes on the same item element. Fixed §11.2 naming inconsistency: `suggestions.map` → `ac.suggestions.map` (no implicit destructuring). Fixed §5.1 pseudocode: `ac.onTab()` → `ac.onTab(v => onChange(v))` to match §6 and §12.2 item 7. |
| 1.7 | 2026-03-27 | Fixed §12.2 item 9: replaced stale "conditionally rendered" with correct always-rendered-with-`hidden` description to match §11.2 pattern. Fixed §7.2: two rows saying "JSX guard prevents rendering" updated to "hidden attribute hides the dropup" to reflect v1.5 always-render pattern. Fixed §11.5 and §12.3: `--autocomplete-highlight` fallback changed from indigo-500 `rgba(99,102,241,0.1)` to blue-600 `rgba(37,99,235,0.1)` to match the project’s `--primary-color: #2563eb`; new token definition added to `src/styles/index.css` in §12.3 notes. |
| 1.8 | 2026-03-27 | [C-1] Corrected CSS token file path from `src/styles/index.css` (non-existent) to `src/index.css` in §11.5, §12.3, and v1.7 changelog entry. [C-2] Fixed `aria-activedescendant` guard in §11.1 to require `ac.suggestions.length > 0` in addition to `ac.isOpen`; expanded prose to explain the three-way guard and the `history-option-{i}` ID scheme; expanded §12.2 item 10 to enumerate all six required ARIA attributes by name. [C-3] Replaced `...` placeholder in §11.2 item body with the complete merged element — CSS classes (§8.1), ARIA attrs, event handlers (§13.7), and content template (§8.1) — so no cross-section synthesis is needed. [I-1] Added simplified-summary note to §5.1 pseudocode and added explicit plain-Enter accept branch with inline comment. [I-2] Expanded §5.1 Tab pseudocode line to show the `isOpen && suggestions.length > 0` guard and `rAF` requirement, matching §12.2 item 7. [I-4] Added `@media (prefers-color-scheme: dark)` token override in §12.3 using `rgba(37,99,235,0.2)` to meet WCAG 2.1 SC 1.4.11 on `--bg-dark: #1e293b`. [M-1] Fixed v1.2 changelog: "no change required" → "change fully specified in §12.6". [M-2] Updated §5 ArrowUp/ArrowDown rows: "Dropup open" → "Dropup open and visible (`suggestions.length > 0`)". [M-3] Added rationale comment to §12.2 item 7 clarifying why the outer guard exists alongside `onTab`'s internal no-op. [M-4] Added cross-reference to §12.2 item 2 in the §5.1 note. [I-3] Confirmed §4.3 deduplication pseudocode already contained the correct `seen.add(entry)` line — no body change required, listed here for audit completeness. [M-5] Added note in §12.1 calling out zero-arg `onCommandChange()` divergence from `useAutocomplete.onCommandChange(value: string)`. |
| 1.9 | 2026-03-27 | Added dropup header label. §3.2 updated to describe the "History" label that appears as a non-interactive chrome row at the top of the dropup panel. §11.2 updated: header `<div>` added to the JSX template with `aria-hidden="true"` so it is excluded from the listbox's accessible children. §12.3 updated: `.dropupHeader` CSS rule added (uppercase small caps, secondary background strip separated by a bottom border). |
| 1.10 | 2026-03-27 | Renamed dropup header label from "History" to "Recent Commands" in §3.2, §11.2, and the implementation. |
| 1.11 | 2026-03-27 | Implemented matched-prefix bold highlighting (reverses DP-5 Option A). §3.2 updated to describe the bold+primary-colour treatment of the typed prefix. §8.1 JSX updated: `firstLine` split into `boldPart`/`normalPart` rendered as `<strong className={styles.matchHighlight}>` + plain text. §12.3 updated: `.matchHighlight` CSS rule added (`font-weight: 700; color: var(--primary-color)`). §13.5 updated to record the decision reversal. |
---

## Resolved Decision Points

| # | Topic | Decision | §  |
|---|---|---|---|
| DP-1 | Matching strategy | **A — Case-insensitive prefix match** | §13.1 |
| DP-2 | Arrow key behavior at dropup boundary | **A — Wrap around (cycle)** | §13.2 |
| DP-3 | Tab behavior when dropup is closed | **C — No-op (Phase 1 behavior retained)** | §13.3 |
| DP-4 | `useAutocomplete.ts` fate | **A — Keep dormant** | §13.4 |
| DP-5 | Highlight matched prefix in rows | **A — No highlighting** | §13.5 |

---

## Table of Contents

1. [Goals and Non-Goals](#1-goals-and-non-goals)
2. [What Changes vs. What Stays](#2-what-changes-vs-what-stays)
3. [New Visual Layout — The Dropup](#3-new-visual-layout--the-dropup)
4. [Filtering and Ranking](#4-filtering-and-ranking)
5. [Keyboard Behavior](#5-keyboard-behavior)
6. [Tab Key Behavior (Phase 2 Activation)](#6-tab-key-behavior-phase-2-activation)
7. [Dropup Lifecycle](#7-dropup-lifecycle)
8. [Multi-Line History Entries in the Dropup](#8-multi-line-history-entries-in-the-dropup)
9. [Deduplication](#9-deduplication)
10. [Disconnected and Empty States](#10-disconnected-and-empty-states)
11. [Accessibility (WCAG 2.1 AA)](#11-accessibility-wcag-21-aa)
12. [Component and Prop Changes](#12-component-and-prop-changes)
13. [Edge Cases and Decision Points](#13-edge-cases-and-decision-points)
14. [Out of Scope (Phase 3)](#14-out-of-scope-phase-3)

---

## 1. Goals and Non-Goals

### Goals

- **Restore Tab to a useful function.** Phase 1 left Tab as a no-op reserved for this phase. Phase 2 gives it a clear, discoverable action: accept the best-matching history entry.
- **Surface history without learning new patterns.** The user already sent these commands — suggesting them as completions feels natural and accelerates re-use of long or multi-line commands without requiring any grammar knowledge.
- **Coexist cleanly with existing history navigation.** ArrowUp/ArrowDown history browsing (driven by `useWebSocket.handleKeyDown`) must continue working exactly as before when the dropup is closed. The two interaction modes are mutually exclusive — one precludes the other.
- **Zero additional network calls.** Suggestions are derived entirely from the in-memory `history: string[]` already owned by `useWebSocket` and persisted in localStorage. No new protocol events, no new API endpoints, no new localStorage keys.
- **Integrate into the existing hook/component architecture.** The new `useHistoryAutocomplete` hook follows the same pattern as the existing `useAutocomplete` (compute via `useMemo`, expose a stable API) and lives alongside other hooks in `src/hooks/`.

### Non-Goals

- This phase does **not** re-introduce command-grammar (template) autocomplete from the original `useAutocomplete.ts`. That feature remains dormant pending a Phase 3 decision.
- This phase does **not** change any WebSocket logic, ProtocolBus events, or backend behavior.
- This phase does **not** redesign the console, graph view, or any other component.
- This phase does **not** change the history storage limit (`MAX_HISTORY = 50` in `src/config/playgrounds.ts`).
- This phase does **not** implement fuzzy/subsequence matching.

---

## 2. What Changes vs. What Stays

| Concern | Phase 1 state | Phase 2 change |
|---|---|---|
| **Tab key** | No-op (`preventDefault`) | Accepts highlighted suggestion when dropup is open; no-op when dropup is closed (DP-3: C) |
| **Autocomplete source** | Disabled | Command history (`history: string[]` from `useWebSocket`) |
| **Dropdown** | Removed entirely in Phase 1 | Re-introduced as a **dropup** (renders above the textarea) |
| **ArrowUp/ArrowDown — dropup open** | Not applicable | Intercepts before boundary check; navigates dropup rows (wraps around — DP-2: A) |
| **ArrowUp/ArrowDown — dropup closed** | Unchanged — boundary-aware history nav | Unchanged |
| **Enter key** | Always sends (when non-empty) | If dropup open **and** `activeIndex ≥ 0`: accepts suggestion; otherwise sends |
| **Escape key** | Not consumed (browser default) | If dropup open: closes dropup; else not consumed |
| **`useAutocomplete.ts`** | Dormant (not imported) | Remains dormant — no changes (DP-4: A) |
| **`useHistoryAutocomplete.ts`** | Does not exist | New hook |
| **`CommandInput.tsx` props** | `command, onChange, onKeyDown, onSend, sendDisabled, disabled` | + `history: string[]` |
| **`useWebSocket.ts` return** | Does not expose `history` | Exposes `history: string[]` |
| **`UseWebSocketReturn` type** | See above | + `history: string[]` |
| **`LeftPanel.tsx` props** | Does not thread `history` | + `commandHistory: string[]` |
| **`localReducer` `SET_COMMAND` action** | Sets `command` only; leaves `historyIndex` | Resets `historyIndex` to `-1` and clears `draftCommand` (see §13.6) |
| **Hint text** | Unchanged | Unchanged — Tab shortcut is not listed to avoid clutter; it is discoverable by use |
| **Placeholder text** | Unchanged | Unchanged |
| **Auto-grow `useEffect`** | Unchanged | Unchanged — also fires after suggestion accept |
| **`useWebSocket.handleKeyDown`** | Unchanged | Unchanged in the hook; intercepted earlier in `CommandInput` when dropup is open |

---

## 3. New Visual Layout — The Dropup

The dropup appears **above the textarea** when there are matching suggestions and the user is actively typing.

```
┌─────────────────────────────────────────────────────────────────┐
│  create node alpha                                              │  ← suggestion row (highlighted)
│  create node beta                                               │  ← suggestion row
│  create node gamma with type mapper   ↵                        │  ← multi-line entry (↵ indicator)
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│  Command  ⓘ                                                     │  ← Label row (unchanged)
├─────────────────────────────────────────────────────────────────┤
│  [ create no ────────────────────────────────── ]  [ Send ]     │  ← Input row (unchanged layout)
├─────────────────────────────────────────────────────────────────┤
│            Enter to send · Shift+Enter for new line · ↑↓        │  ← Hint text (unchanged)
└─────────────────────────────────────────────────────────────────┘
```

### 3.1 Dropup Positioning

The dropup is an absolutely-positioned element with `bottom: 100%; left: 0; right: 0; z-index: 200` relative to the `.inputWrapper` div (which has `position: relative`). The `.inputWrapper` wraps only the textarea within the `.inputRow` flex container — the Send button is a sibling at the flex level of `.inputWrapper`, not a child of it.

**The `.inputWrapper` CSS class is already defined** in `CommandInput.module.css` with `position: relative`. Phase 1 removed it from the JSX; Phase 2 restores it. One additional CSS change is required: the flex-grow rule currently on `.inputRow .textarea` must move onto `.inputRow .inputWrapper` (see §12.3).

### 3.2 Dropup Contents

The dropup panel opens with a non-interactive **header label** at the top — the text "Recent Commands" rendered in uppercase small caps against the secondary background. This chrome row is purely decorative/informational; it is excluded from the ARIA listbox tree via `aria-hidden="true"` (see §11.2).

Below the header, each suggestion row shows:
- The **first line** of the history entry, in monospace font. The characters that match the current input are rendered **bold in the primary colour** (`--primary-color`) to give immediate visual feedback of where the typed prefix ends and the completion begins.
- A `↵` symbol (return arrow, muted secondary colour) appended to the first line when the entry spans more than one line — indicating that accepting will insert multi-line text.
- Long first lines are truncated via `text-overflow: ellipsis; overflow: hidden; white-space: nowrap`. The `↵` indicator is always visible at the right edge when present (implemented via a flex row: truncating `<span>` + fixed `<span>` for the `↵`).

### 3.3 Maximum Visible Rows

The dropup shows at most `MAX_AUTOCOMPLETE_SUGGESTIONS = 8` entries (new constant in `src/config/playgrounds.ts`). Results exceeding this limit are trimmed after deduplication; only the top 8 newest matches are shown. The dropup does not scroll — the list is capped.

**Rationale:** A scrollable dropup adds structural and accessibility complexity. Eight entries is ample for command-line muscle memory recall. The user can always type more characters to narrow the list.

---

## 4. Filtering and Ranking

### 4.1 Matching Rule

**Resolved (DP-1: A) — Case-insensitive prefix match.** A history entry is a candidate if `entry.toLowerCase().startsWith(command.trimStart().toLowerCase())` is true, and `command.trimStart().length > 0`. An empty or whitespace-only `command` produces no matches; the dropup does not open.

### 4.2 Ranking

Suggestions are returned in **insertion order from the `history` array**, which is already newest-first (index 0 = most recently sent command). No additional scoring is applied; recency is the only signal.

### 4.3 Deduplication

The `history` array avoids consecutive duplicates via the `if (history[0] !== text)` guard in `sendCommand`. However, non-consecutive identical commands can exist. The filtered suggestion list deduplicates by **exact (case-sensitive) text** before display — each unique command appears at most once, represented by its most recent (lowest-index) occurrence.

```typescript
// Pseudocode — the filter step inside useHistoryAutocomplete useMemo:
const seen = new Set<string>();
const deduped = allMatching.filter(entry => {
  if (seen.has(entry)) return false;
  seen.add(entry);
  return true;
});
const suggestions = deduped.slice(0, MAX_AUTOCOMPLETE_SUGGESTIONS);
```

---

## 5. Keyboard Behavior

Phase 2 extends the Phase 1 keyboard rule set. All Phase 1 rules are preserved; changes and additions are noted.

| Key | Condition | Result |
|---|---|---|
| **Enter** | Dropup open **and** `activeIndex ≥ 0` | Accepts highlighted suggestion; closes dropup; focus stays on textarea |
| **Enter** | Dropup open **and** `activeIndex = -1` | Sends the command (unchanged from Phase 1) |
| **Enter** | Dropup closed, command non-empty | Sends the command (unchanged) |
| **Enter** | Dropup closed, command empty | No-op (unchanged) |
| **Shift+Enter** | Any | Inserts a newline; dropup closes (command changed; multiline prefix rarely matches) |
| **Ctrl/⌘+Enter** | Dropup open **and** `activeIndex ≥ 0` | Accepts highlighted suggestion (consistent with plain Enter) |
| **Ctrl/⌘+Enter** | Command non-empty | Sends the command (unchanged) |
| **Tab** | Dropup open, suggestions exist | Accepts highlighted item, or first item if `activeIndex = -1`; closes dropup |
| **Tab** | Dropup closed | No-op (`preventDefault`) — same as Phase 1 (DP-3: C) |
| **Tab** | No matches exist | No-op (`preventDefault`) — same as Phase 1 |
| **ArrowDown** | Dropup open **and visible** (`ac.isOpen && ac.suggestions.length > 0`) | Moves highlight down one row; wraps from last to first (DP-2: A) |
| **ArrowDown** | Dropup closed, **or** open but `suggestions.length === 0` | Boundary-aware history navigation (unchanged from Phase 1) |
| **ArrowUp** | Dropup open **and visible** (`ac.isOpen && ac.suggestions.length > 0`) | Moves highlight up one row; wraps from first to last (DP-2: A) |
| **ArrowUp** | Dropup closed, **or** open but `suggestions.length === 0` | Boundary-aware history navigation (unchanged from Phase 1) |
| **Escape** | Dropup open | Closes dropup; command text and caret position are unchanged |
| **Escape** | Dropup closed | Not consumed — browser default applies |
| **All other keys** | — | Default browser behavior (unchanged) |

### 5.1 Key Handling Priority in `CommandInput.handleKeyDown`

The revised handler executes checks in this order, which ensures the two navigation systems (dropup vs. history) are mutually exclusive:

> **Note:** This pseudocode is a simplified priority reference. Each `accept` shorthand means calling `ac.accept(...)` then scheduling a `requestAnimationFrame` caret placement (see §12.2 items 6 and 7 for full code). The `onChange`/`onCommandChange` wiring is in §12.2 item 2.

```
1. Tab        → if (ac.isOpen && ac.suggestions.length > 0): ac.onTab(v=>onChange(v)) + rAF; return
               else: no-op (e.preventDefault already called above); return
2. Enter      → if Shift: browser newline (return)
                elif (Ctrl/Meta && ac.isOpen && ac.activeIndex >= 0): accept + rAF; return
                elif (Ctrl/Meta): send; return
                elif (ac.isOpen && ac.activeIndex >= 0): accept + rAF; return  ← plain Enter accept
                else: send; return
3. Escape     → if ac.isOpen: ac.dismiss(); e.preventDefault(); return
                else: fall through (browser default)
4. ArrowUp/⇓  → if (ac.isOpen && ac.suggestions.length > 0): ac.navigate(dir); e.preventDefault(); return
                else: existing boundary-aware history nav (onKeyDown delegation) + rAF caret placement
5. All other  → onKeyDown(e) [unchanged delegation to useWebSocket.handleKeyDown]
```

The critical invariant: the `onKeyDown` delegation to `useWebSocket.handleKeyDown` (step 5 and the `else` branch of step 4) is only reached when the dropup is **closed or has no visible suggestions** (`!ac.isOpen || ac.suggestions.length === 0`). Arrow key events never reach `useWebSocket.handleKeyDown` while the dropup is actively displaying rows.

### 5.2 "Accept" — Effect on the Command Field

When a suggestion is accepted (Enter, Tab, or mouse click):

1. `onChange(suggestion)` is called with the full history entry text (including any embedded `\n` characters for multi-line entries).
2. React re-renders `CommandInput` with the new `command` prop.
3. The `useEffect([command])` auto-grow fires and expands the textarea to fit the accepted text.
4. A `requestAnimationFrame` places the caret at end-of-text — identical to the pattern already used in Phase 1 for history navigation (§11.1 of the Phase 1 spec). For **keyboard accepts** (Enter, Tab) the rAF is placed inline in the `handleKeyDown` branch that calls `ac.accept()`. For **mouse-click accepts** the rAF must be placed inside the `onClick` handler in `CommandInput` (see §12.2 item 9), because `accept()` has no access to `textareaRef`.
5. The dropup closes: `isOpen = false; activeIndex = -1`.
6. Focus remains on the textarea.

---

## 6. Tab Key Behavior (Phase 2 Activation)

Phase 1 left Tab as `e.preventDefault(); return`. Phase 2 activates it when the dropup is open:

```typescript
// In CommandInput.handleKeyDown — replaces the Phase 1 Tab branch:
if (e.key === 'Tab') {
  e.preventDefault();
  if (ac.isOpen && ac.suggestions.length > 0) {
    ac.onTab(v => onChange(v));  // see §12.1 for onTab() definition
    requestAnimationFrame(() => {
      const el = textareaRef.current;
      if (el) el.selectionStart = el.selectionEnd = el.value.length;
    });
  }
  return;
}
```

`ac.onTab(setCommand)` behaviour (resolved — DP-3: C):

| Dropup state | Suggestions? | Result |
|---|---|---|
| **Open** | Yes | Accepts active item (`activeIndex`) or first item if `activeIndex = -1` |
| **Closed** | Yes | No-op — Tab is consumed (`preventDefault` already called) but nothing changes |
| **Closed** | No | No-op |

The `setCommand` argument is threaded in because `ac.onTab` needs to update the textarea value when accepting. Specifically: `(v) => onChange(v)`.

**Rationale for DP-3: C.** The dropup is the user's explicit signal that they want a suggestion. If they closed the dropup via Escape, declined to open it, or finished typing, a blind Tab-accept could replace their carefully typed command with an unexpected history entry. The Phase 1 no-op contract is the safest and most predictable behavior. Users who want to Tab-complete must first open the dropup by typing.

---

## 7. Dropup Lifecycle

### 7.1 Opening

The dropup opens when ALL of the following are true:

1. `ac.onCommandChange()` is called (this happens inside `CommandInput`'s `onChange` handler — only on real user input, not programmatic command changes via `dispatch`).
2. The resulting `command.trimStart()` is non-empty.
3. At least one matching history entry exists after deduplication.

**History navigation does NOT open the dropup.** When the user presses ArrowUp in history-nav mode, `useWebSocket.handleKeyDown` dispatches `ENTER_HISTORY` or `SET_HISTORY_INDEX`, which updates `command` via React state. This reaches `CommandInput` as a new `command` prop value but does **not** fire `onChange` on the textarea. The `useHistoryAutocomplete` hook's `onCommandChange` is never called, so `isOpen` stays `false`.

This is the same gate established in the original `useAutocomplete` design (the `onCommandChange` vs. prop-change duality).

### 7.2 Closing

The dropup closes when any of the following occurs:

| Trigger | Mechanism |
|---|---|
| Suggestion accepted (Enter / Tab / click) | `ac.accept()` sets `isOpen = false` |
| Escape pressed | `ac.dismiss()` sets `isOpen = false` |
| Input becomes empty | `onCommandChange()` called with empty value — `useMemo` yields `[]`; the `hidden` attribute hides the dropup (`ac.isOpen` remains `true` in hook state but is irrelevant while `suggestions.length === 0`). |
| No more matches while typing | Same as above — filter produces empty list; `hidden` attribute hides the dropup |
| Textarea loses focus (`blur`) | `ac.dismiss()` called directly from `onBlur` handler |
| Send clears the field | `CLEAR_COMMAND` sets `command = ""`; `onCommandChange` is NOT called here (send goes through a different path), but the `suggestions` `useMemo` recomputes to `[]` on the next render and the `hidden` attribute hides the dropup |

### 7.3 `onBlur` and Mouse-Click Coordination

The standard problem with dropdowns: `blur` fires before `click` on a dropdown item, so a naïve `onBlur → dismiss()` closes the dropup before the click handler runs, making items unclickable.

**Resolution:** Each dropup item uses `onMouseDown={e => e.preventDefault()}` to suppress the focus-steal. When `mousedown` is suppressed, the textarea never loses focus, so `blur` never fires. The `onClick` handler then fires normally and calls `ac.accept()`.

Since the blur-on-click problem is eliminated at the source (mousedown prevention), `onBlur` can call `ac.dismiss()` directly with **no delay timer**. This is simpler than the 150 ms timeout pattern in the original `useAutocomplete` and avoids a class of timer-cancellation bugs.

### 7.4 State Reset on Close

On close: `isOpen = false; activeIndex = -1`. The `suggestions` array recomputes (via `useMemo`) on the next render from the current `command` value.

---

## 8. Multi-Line History Entries in the Dropup

History entries containing `\n` (multi-line commands entered via Shift+Enter) require special handling.

### 8.1 Display

Each dropup row displays **only the first line** (`entry.split('\n')[0]`) with the matched prefix bolded, plus a `↵` symbol for multi-line entries. The first line is split into two parts:

- **`boldPart`** — `firstLine.slice(0, matchLen)`, rendered as `<strong className={styles.matchHighlight}>`. `matchLen` is `Math.min(needle.length, firstLine.length)` where `needle = command.trimStart().split('\n')[0]` (first line of the typed input only — see edge-case note below).
- **`normalPart`** — `firstLine.slice(matchLen)`, rendered as plain text immediately after.
- The `…` continuation character (for multi-line entries) follows `normalPart` in the same `<span>`.

Implemented as a two-part flex row:

```tsx
<span className={styles.dropupItemText}>
  {matchLen > 0 && <strong className={styles.matchHighlight}>{boldPart}</strong>}
  {normalPart}{isMultiLine ? '…' : ''}
</span>
{isMultiLine && (
  <span className={styles.multilineIndicator} aria-label="multi-line command">↵</span>
)}
```

**Edge case — multiline needle (§13.10):** When the user has typed a newline via Shift+Enter, `command.trimStart()` contains `\n`. Using the full needle length would exceed `firstLine.length` for most entries. The needle is therefore truncated to its first line (`needle.split('\n')[0]`) before computing `matchLen`. In practice, once the user has typed a newline, the suggestion list is almost certainly empty (§13.10), so this path is rarely reached.

`dropupItemText` has `overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1 1 auto`.
`multilineIndicator` has `flex: 0 0 auto; margin-left: 0.25rem; color: var(--text-secondary)`.
`matchHighlight` has `font-weight: 700; color: var(--primary-color)`.

### 8.2 Matching

Matching is applied to the **full history entry text** (all lines). Since prefix matching starts from the very beginning of the string, a multi-line entry `"create node foo\nwith type mapper"` is matched by a single-line input `"create no"` (the entry does start with that prefix). This means the user can discover a multi-line command by typing only the beginning of its first line — the most common and natural usage pattern.

A multi-line input `"create node foo\nwith "` only matches entries with that exact multiline prefix. This is consistent with prefix semantics.

### 8.3 Accept

When a multi-line suggestion is accepted, the **full text** (including `\n` characters) is inserted into the textarea via `onChange(suggestion)`. The auto-grow `useEffect([command])` fires, expanding the textarea to show all lines. The caret is placed at end-of-text via `requestAnimationFrame`.

---

## 9. Deduplication

The `history` array suppresses consecutive duplicates (`if (history[0] !== text)` in `sendCommand`). Non-consecutive identical commands can still accumulate over time. The suggestion list deduplicates so the user never sees the same command twice in the dropup.

Deduplication is applied **after** filtering and **before** slicing to `MAX_AUTOCOMPLETE_SUGGESTIONS`. Order is preserved: the first occurrence (most recent) of any duplicate stays; later occurrences are discarded.

This is a pure `useMemo` computation — no side effects, no state mutation.

---

## 10. Disconnected and Empty States

| State | Dropup behavior |
|---|---|
| `disabled = true` (not connected) | Dropup never opens — textarea is disabled, no `onChange` fires |
| `command = ""` (empty) | Dropup does not open; `onCommandChange()` called with empty value triggers no match |
| `command` is whitespace-only | Same as empty — `command.trimStart()` used as the match operand |
| `history.length = 0` | Dropup never opens — no entries to filter |
| Send clears the field | `CLEAR_COMMAND` sets `command = ""`; `suggestions` collapses to `[]`; dropup hides |
| History navigation loads an entry | Dropup does NOT open — the command change goes through `dispatch`, not `onChange` |

---

## 11. Accessibility (WCAG 2.1 AA)

The dropup uses the **ARIA combobox + listbox** pattern (ARIA 1.2 `role="combobox"` with an owned `role="listbox"`). Focus never leaves the textarea.

### 11.1 Required ARIA Attributes on the Textarea

```tsx
<textarea
  id="command"
  role="combobox"
  aria-expanded={ac.isOpen && ac.suggestions.length > 0}
  aria-haspopup="listbox"
  aria-controls="history-dropup"
  aria-activedescendant={
    ac.isOpen && ac.suggestions.length > 0 && ac.activeIndex >= 0
      ? `history-option-${ac.activeIndex}`
      : undefined
  }
  aria-autocomplete="list"
  ...
/>
```

`aria-expanded` uses `ac.isOpen && ac.suggestions.length > 0` to reflect whether the popup is **actually visible** in the DOM. `aria-controls` always points to `"history-dropup"`, which is always present in the DOM (see §11.2). `aria-activedescendant` uses the same three-way guard (`ac.isOpen && ac.suggestions.length > 0 && ac.activeIndex >= 0`) so it is only set while the dropup is visible and a row is keyboard-highlighted; the referenced IDs follow the pattern `history-option-{i}` matching the `id` attribute on each item in §11.2. `aria-autocomplete="list"` declares that a list of suggestions reflects the current input value — the semantically correct value for this interaction model.

### 11.2 Required ARIA on the Dropup Container and Items

The listbox container is **always rendered** so that `aria-controls="history-dropup"` always resolves to a real DOM element (required by ARIA validators). Visibility is controlled via the `hidden` attribute; items are rendered only when visible:

```tsx
<div
  id="history-dropup"
  role="listbox"
  aria-label="Command history suggestions"
  className={styles.dropup}
  hidden={!(ac.isOpen && ac.suggestions.length > 0)}
>
  {/* Header label — aria-hidden so it is not treated as a listbox option */}
  <div className={styles.dropupHeader} aria-hidden="true">
    Recent Commands
  </div>
  {ac.isOpen && ac.suggestions.length > 0 && ac.suggestions.map((entry, i) => {
    const firstLine = entry.split('\n')[0];
    const isMultiLine = entry.includes('\n');
    return (
      <div
        key={entry}
        id={`history-option-${i}`}              /* ID pattern matches aria-activedescendant in §11.1 */
        role="option"
        aria-selected={i === ac.activeIndex}
        className={styles.dropupItem}            /* CSS class from §8.1; merged here with ARIA attrs */
        onMouseDown={e => e.preventDefault()}    /* prevents textarea blur (§7.3) */
        onClick={() => {                         /* rAF caret placement per §5.2 */
          ac.accept(i, v => onChange(v));
          requestAnimationFrame(() => {
            const el = textareaRef.current;
            if (el) el.selectionStart = el.selectionEnd = el.value.length;
          });
        }}
      >
        <span className={styles.dropupItemText}>
          {matchLen > 0 && <strong className={styles.matchHighlight}>{boldPart}</strong>}
          {normalPart}{isMultiLine ? '…' : ''}
        </span>
        {isMultiLine && (
          <span className={styles.multilineIndicator} aria-label="multi-line command">↵</span>
        )}
      </div>
    );
  })}
</div>
```

This is the **complete merged item element** — it unifies the CSS classes from §8.1, the ARIA attributes from this section, the event handlers from §13.7, and the content template from §8.1. No cross-section synthesis is required. The `id="history-option-{i}"` scheme matches the `aria-activedescendant` expression in §11.1 exactly.

> **Implementer note — `key={entry}` correctness:** `key={entry}` is valid because deduplication (§4.3, §9) guarantees every displayed entry is a unique string. If the deduplication guarantee is ever relaxed, `key` must change to `key={i}` to avoid React reconciliation collisions. The current dedup logic fully prevents that scenario; this note exists only to make the dependency explicit.

### 11.3 Focus Management

The textarea remains focused for the entire dropup lifecycle. `onMouseDown + preventDefault` on each dropup item is the standard technique to prevent focus theft. No `tabIndex` is needed on the dropup or its items — they are navigated via `aria-activedescendant`, not browser focus.

### 11.4 Keyboard Trap

There is no keyboard trap. The full set of exit paths:
- **Tab** accepts a suggestion and returns to normal textarea mode.
- **Escape** closes the dropup without changing the command or moving focus.
- **Enter** sends or accepts; in either case the dropup closes.
- Clicking outside the textarea triggers `onBlur → ac.dismiss()`.

### 11.5 Visible Highlight

The active suggestion row (`aria-selected="true"`) uses a distinct background colour. The CSS rule targets `[aria-selected="true"]` on the `.dropupItem` class so the visual and semantic states are always in sync. No separate `isActive` prop class toggle is needed; the ARIA attribute drives the style.

```css
.dropupItem[aria-selected="true"] {
  background-color: var(--autocomplete-highlight, rgba(37, 99, 235, 0.1));
  color: var(--text-primary);
}
```

The `--autocomplete-highlight` token uses `0.1` opacity. The hardcoded fallback `rgba(37, 99, 235, 0.1)` matches the project’s `--primary-color: #2563eb` (Tailwind blue-600) rather than indigo-500. Because the token is defined in `src/index.css` (see §12.3), the fallback only fires in environments where the CSS variables are not loaded. The `color: var(--text-primary)` declaration ensures text remains readable in dark-mode themes where the highlight background may be relatively opaque.

### 11.6 Screen Reader Announcement

When `aria-activedescendant` changes, screen readers announce the newly focused option text automatically (per the combobox pattern). No additional live regions are needed.

---

## 12. Component and Prop Changes

### 12.1 New Hook: `src/hooks/useHistoryAutocomplete.ts`

```typescript
export interface UseHistoryAutocompleteReturn {
  /** Filtered, deduplicated history suggestions. Newest-first, capped at MAX_AUTOCOMPLETE_SUGGESTIONS. */
  suggestions: string[];
  /** Whether the dropup is currently visible. */
  isOpen: boolean;
  /** Index of the keyboard-highlighted row. -1 = none. */
  activeIndex: number;
  /**
   * Call on every real onChange event. Opens the dropup and resets activeIndex.
   * Must NOT be called after programmatic command changes (history nav dispatches).
   */
  onCommandChange: () => void;
  /**
   * Move the highlight by `direction` rows (+1 = down, -1 = up).
   * Wraps around at both ends: ArrowDown on last item → index 0;
   * ArrowUp on index 0 → last item; ArrowUp from -1 → last item (DP-2: A).
   */
  navigate: (direction: 1 | -1) => void;
  /**
   * Accept the suggestion at `index` and insert it via `setCommand`.
   * Takes an explicit `index` rather than using internal `activeIndex` to
   * support two distinct call sites:
   *   - Keyboard accept (Enter / Tab): caller passes `ac.activeIndex` (or `0`
   *     when `activeIndex === -1`, which is handled internally by `onTab`).
   *   - Mouse-click accept: caller passes the item's own map index `i`,
   *     independent of keyboard highlight — so clicking any row works even
   *     when no row is keyboard-highlighted.
   * Closes the dropup and resets activeIndex to -1.
   */
  accept: (index: number, setCommand: (v: string) => void) => void;
  /**
   * Handle a Tab keypress.
   *  - Dropup open with suggestions: accepts active item (or index 0 if none active).
   *  - Dropup closed: no-op (DP-3: C — Tab only works when the dropup is open).
   *  - No suggestions: no-op.
   */
  onTab: (setCommand: (v: string) => void) => void;
  /** Close the dropup without changing the command text. */
  dismiss: () => void;
}

export function useHistoryAutocomplete(
  history: string[],
  command: string,
): UseHistoryAutocompleteReturn
```

**Internal state:** `useState<boolean>` for `isOpen`, `useState<number>` for `activeIndex`.

**Computed `suggestions`:** single `useMemo` over `[history, command]`. The computation:
1. If `command.trimStart().length === 0` → return `[]`.
2. Filter `history` by case-insensitive prefix match: `entry.toLowerCase().startsWith(command.trimStart().toLowerCase())` (DP-1: A).
3. Deduplicate (§9).
4. Slice to `MAX_AUTOCOMPLETE_SUGGESTIONS`.

The React Compiler will handle all other memoisation — no manual `useCallback` wrappers needed on `navigate`, `accept`, `onTab`, or `dismiss` unless their identity must be stable for a specific reason (no such reason exists here).

**Key implementation note:** `onCommandChange` sets `isOpen = true` and `activeIndex = -1`. It is intentionally a **zero-argument** function — it does not receive the new value, because `suggestions` is already recomputed from `command` via `useMemo`. Calling `onCommandChange()` only needs to flip the open-state flag.

**Important divergence from `useAutocomplete`:** The dormant `useAutocomplete.onCommandChange` has signature `(value: string) => void`. `useHistoryAutocomplete.onCommandChange` is `() => void` — it takes **no arguments**. Developers familiar with the existing hook must not pass the command value here; TypeScript will catch this at compile time as an extra argument on a `() => void` type, but the divergence should be understood as intentional.

### 12.2 `CommandInput.tsx`

**New prop:**
- `history: string[]` — the ordered command history (newest-first), flowing from `useWebSocket`.

**Logic changes:**

1. Import `useHistoryAutocomplete` and call it: `const ac = useHistoryAutocomplete(history, command)`.
2. Wire the textarea's `onChange` to both the prop callback and the autocomplete open trigger. Note: `onChange` in the snippet below refers to the **prop** — the local prop name and the inner call share the same identifier, which is valid but worth noting explicitly:
   ```tsx
   onChange={(e) => {
     onChange(e.target.value);  // prop — updates `command` in parent reducer (SET_COMMAND)
     ac.onCommandChange();       // flips isOpen = true; React 19 batches both updates
   }}
   ```
   **Ordering is intentional:** `onChange(value)` runs first so `command` is updated in the same render batch that `isOpen` flips to `true`. The `useMemo([history, command])` in the hook then computes suggestions against the already-updated value in that single batched render.
3. Add `onBlur` handler to the textarea: `onBlur={() => ac.dismiss()}`.
4. Add Escape handling in `handleKeyDown` (before the ArrowUp/ArrowDown block): if `ac.isOpen`, call `ac.dismiss()`, `e.preventDefault()`, and `return`.
5. Modify ArrowUp/ArrowDown handling: if `ac.isOpen && ac.suggestions.length > 0`, call `ac.navigate(...)`, prevent default, and return — only reach the existing boundary-aware history nav when `!ac.isOpen || ac.suggestions.length === 0`.
6. Modify Enter handling: before sending, check `ac.isOpen && ac.activeIndex >= 0` and accept instead. When accepting, call `ac.accept(ac.activeIndex, v => onChange(v))` and place the caret at end-of-text via `requestAnimationFrame`:
   ```tsx
   ac.accept(ac.activeIndex, v => onChange(v));
   requestAnimationFrame(() => {
     const el = textareaRef.current;
     if (el) el.selectionStart = el.selectionEnd = el.value.length;
   });
   ```
7. Replace the Phase 1 Tab no-op with a guarded call to `ac.onTab(v => onChange(v))`. The outer `if (ac.isOpen && ac.suggestions.length > 0)` guard has two purposes: (a) it prevents the rAF from firing when Tab is a no-op — an unconditional rAF would silently jump the caret to end-of-text when the dropup is closed, a Phase 1 regression; (b) it keeps the code path clear even though `onTab` already handles the no-op case internally (DP-3: C). The outer guard is therefore redundant for `onTab`'s correctness but is essential for the rAF. The `accept()` called inside `onTab` has no access to `textareaRef`, so the rAF must live in `handleKeyDown`:
   ```tsx
   if (e.key === 'Tab') {
     e.preventDefault();
     if (ac.isOpen && ac.suggestions.length > 0) {
       ac.onTab(v => onChange(v));
       requestAnimationFrame(() => {
         const el = textareaRef.current;
         if (el) el.selectionStart = el.selectionEnd = el.value.length;
       });
     }
     return;
   }
   ```
8. Restore the `.inputWrapper` div wrapping the textarea in the JSX (already defined in CSS).
9. Add the always-rendered listbox container to the JSX (visible when `ac.isOpen && ac.suggestions.length > 0`, hidden otherwise via the `hidden` attribute — see §11.2 for the full ARIA-annotated template). Each suggestion row must carry **both** of the following event handlers (per §7.3 and §13.7), plus a `requestAnimationFrame` caret placement on click:
   ```tsx
   onMouseDown={(e) => e.preventDefault()}  // prevents textarea blur
   onClick={() => {
     ac.accept(i, v => onChange(v));
     // Place caret at end-of-text after React commits the new value.
     // accept() has no access to textareaRef, so the rAF must live here.
     requestAnimationFrame(() => {
       const el = textareaRef.current;
       if (el) el.selectionStart = el.selectionEnd = el.value.length;
     });
   }}
   ```
   See §11.2 for the full ARIA-annotated dropup JSX including `role`, `id`, `aria-selected`, and `className={styles.dropup}` on the container. See §11.2 for the complete, ready-to-use item element — no cross-section synthesis required.
10. Add the **full set of ARIA attributes** to `<textarea>` as specified in §11.1. This means all six attributes — not only `role`:
    - `role="combobox"` — replaces the default implicit textarea role
    - `aria-expanded={ac.isOpen && ac.suggestions.length > 0}` — reflects actual popup visibility
    - `aria-haspopup="listbox"` — declares the popup type to screen readers
    - `aria-controls="history-dropup"` — points to the always-present listbox container
    - `aria-activedescendant={ac.isOpen && ac.suggestions.length > 0 && ac.activeIndex >= 0 ? \`history-option-${ac.activeIndex}\` : undefined}` — drives screen-reader announcement of the highlighted row; `undefined` when nothing is highlighted or the dropup is hidden
    - `aria-autocomplete="list"` — declares suggestion-list semantics

    Omitting `aria-activedescendant` would cause screen readers to never announce which item is keyboard-highlighted, silently breaking the WCAG 2.1 AA combobox interaction pattern described in §11.6.
11. Because `onMouseDown` suppression prevents blur during item clicks (§7.3), a direct `onBlur={() => ac.dismiss()}` on the textarea is sufficient with no delay timer.

**Props unchanged:** `command, onChange, onKeyDown, onSend, sendDisabled, disabled`.

### 12.3 `CommandInput.module.css`

**CSS rules changed:**

The existing rule `.inputRow .textarea { flex: 1 1 auto; }` must be updated because the textarea is now wrapped in `.inputWrapper`:

```css
/* BEFORE (Phase 1): */
.inputRow .textarea {
  flex: 1 1 auto;
}

/* AFTER (Phase 2): */
.inputRow .inputWrapper {
  flex: 1 1 auto;   /* inputWrapper takes the available width */
}
/* .textarea width: 100% is already set globally; no changes needed there */
```

**CSS rules added:**

```css
/* Dropup container — positioned above the textarea */
.dropup {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  z-index: 200;
  background: var(--bg-primary, #fff);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.1);  /* shadow projects upward */
  margin-bottom: 0.25rem;                         /* small gap between dropup and textarea */
  overflow: hidden;                               /* clips item border-radius */
}

/* Dropup header label — identifies the panel as history-based autocomplete */
.dropupHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.25rem 0.75rem;
  background: var(--bg-secondary, #f8fafc);
  border-bottom: 1px solid var(--border-color);
  font-size: 0.65rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-secondary);
  user-select: none;
}

/* Individual suggestion row */
.dropupItem {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.45rem 0.75rem;
  cursor: pointer;
  font-family: ui-monospace, 'Cascadia Code', 'Fira Code', monospace;
  font-size: 0.85rem;
  color: var(--text-primary);
  transition: background 0.1s;
}

.dropupItem:hover {
  background-color: var(--hover-bg, rgba(0, 0, 0, 0.04));
}

/* Driven by aria-selected so visual and semantic states are always in sync */
.dropupItem[aria-selected="true"] {
  background-color: var(--autocomplete-highlight, rgba(37, 99, 235, 0.1));
  color: var(--text-primary);  /* ensures readable text in dark-mode themes */
}

/* Text part of the row — truncates long lines */
.dropupItemText {
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Bold matched prefix inside a suggestion row */
.matchHighlight {
  font-weight: 700;
  color: var(--primary-color);
}

/* Multi-line indicator symbol */
.multilineIndicator {
  flex: 0 0 auto;
  font-style: normal;
  color: var(--text-secondary);
  font-size: 0.8rem;
  user-select: none;
}
```

**CSS rules unchanged:** all existing Phase 1 styles including `.inputWrapper { position: relative; }` (already correct).

**Token to add in `src/index.css`** (in the `:root` block alongside the other design tokens):

```css
--autocomplete-highlight: rgba(37, 99, 235, 0.1);  /* blue-600 at 10% — matches --primary-color */
```

This ensures the highlight colour is consistent with all other interactive elements (buttons, focus rings, nav links) that use `--primary-color: #2563eb`. The hardcoded fallback in `.dropupItem[aria-selected="true"]` acts as a safety net if the CSS variable file is not loaded.

**Dark-mode contrast override — also add to `src/index.css`:**

The app's dark background is `--bg-dark: #1e293b` (slate-800). Blue at 10% opacity over slate-800 produces a contrast ratio below the 3:1 threshold required by WCAG 2.1 SC 1.4.11 (Non-text Contrast) for UI component states. A dark-mode media query raises the opacity to meet the standard:

```css
@media (prefers-color-scheme: dark) {
  :root {
    --autocomplete-highlight: rgba(37, 99, 235, 0.2);  /* 20% opacity passes WCAG SC 1.4.11 on --bg-dark */
  }
}
```

The light-mode value (`0.1`) is unchanged; only the dark-mode fallback is raised. Text legibility is handled separately via `color: var(--text-primary)` on the item rule.

### 12.4 `useWebSocket.ts`

**Change 1 — expose `history` in the return value:**

```typescript
// In UseWebSocketReturn — add:
history: string[];

// In the return statement — add:
history,
```

**Change 2 — `SET_COMMAND` resets history navigation state:**

The current `SET_COMMAND` reducer case leaves `historyIndex` and `draftCommand` untouched. This creates a subtle bug when a suggestion is accepted while the user is in history-navigation mode (see §13.6). The fix:

```typescript
// BEFORE:
case 'SET_COMMAND':
  return { ...state, command: action.value };

// AFTER:
case 'SET_COMMAND':
  // Any explicit setCommand call (including suggestion accept) exits history
  // navigation: reset the cursor and discard the draft. Semantically correct —
  // if text is being set externally, history browsing is over.
  return { ...state, command: action.value, historyIndex: -1, draftCommand: '' };
```

This is a small but important invariant improvement: `SET_COMMAND` is now self-contained for the "command set from outside the history nav flow" case. The `EXIT_HISTORY` action (ArrowDown past index 0) still handles the "restore draft and exit" path explicitly — these two actions serve different semantics.

### 12.5 `LeftPanel.tsx`

**New prop:**
- `commandHistory: string[]`

**Change at `CommandInput` call site:**

```tsx
<CommandInput
  command={command}
  onChange={onCommandChange}
  onKeyDown={onCommandKeyDown}
  onSend={onSend}
  disabled={inputDisabled}
  sendDisabled={sendDisabled}
  history={commandHistory}          {/* NEW */}
/>
```

**Props unchanged:** all existing props.

### 12.6 `Playground.tsx`

**Change at `LeftPanel` call site:**

```tsx
<LeftPanel
  ...{/* all existing props unchanged */}
  commandHistory={ws.history}         {/* NEW */}
/>
```

No other changes to `Playground.tsx`.

### 12.7 `src/config/playgrounds.ts`

**New constant:**

```typescript
/** Maximum number of history-based autocomplete suggestions shown in the dropup. */
export const MAX_AUTOCOMPLETE_SUGGESTIONS = 8;
```

**No changes to existing constants or `PLAYGROUND_CONFIGS`.**

### 12.8 `useAutocomplete.ts`

**Resolved (DP-4: A) — No changes.** The file remains in the codebase, dormant and unimported, exactly as left by Phase 1. A comment may optionally be added to the top of the file referencing Phase 3 as the planned re-introduction point, but no functional changes are made.

---

## 13. Edge Cases and Decision Points

---

### 13.1 [DP-1] Matching Strategy — **Resolved: Option A**

**Decision: Case-insensitive prefix match.** A history entry is included if `entry.toLowerCase().startsWith(needle.toLowerCase())` where `needle = command.trimStart()`. Typing `"c"` surfaces only entries starting with `"c"` — focused, CLI-natural results. Substring matching is deferred to Phase 3 if a search-palette interface is introduced.

---

### 13.2 [DP-2] Arrow Key Behavior at Boundary Items — **Resolved: Option A**

**Decision: Wrap around (cycle).** ArrowDown on the last item → index 0. ArrowUp on index 0 → last item. Consistent with the original `useAutocomplete` design and the convention of most autocomplete dropdowns.

**Sub-case: ArrowUp at `activeIndex = -1` (nothing highlighted):**
Currently the dropup opens with `activeIndex = -1`. If the user immediately presses ArrowUp (rather than ArrowDown), they should move to the **last** item (not wrap to index 0 and then go down). This is the standard combobox behavior: ArrowUp from "no selection" goes to the bottom of the list; ArrowDown from "no selection" goes to the top.

```typescript
navigate(direction: 1 | -1):
  if n === 0: return  // guard: no-op when suggestions list is empty (prevents % 0 = NaN)
  if direction === +1:
    setActiveIndex(i => i < 0 ? 0 : (i + 1) % n)
  else:
    setActiveIndex(i => i <= 0 ? n - 1 : i - 1)
```

---

### 13.3 [DP-3] Tab Behavior When the Dropup Is Closed — **Resolved: Option C**

**Decision: No-op when closed (Phase 1 behavior retained).** Tab only acts when the dropup is open — the user's explicit signal that they want a completion. A blind accept on a closed dropup risks silently overwriting a carefully-typed command with an unexpected history entry. This is the safest, most predictable contract. If the user wants a completion, they type characters to open the dropup, then Tab to accept.

---

### 13.4 [DP-4] `useAutocomplete.ts` — **Resolved: Option A**

**Decision: Keep dormant.** No changes to the file. Consistent with the Phase 1 intent of "suspended, not deleted". `useHistoryAutocomplete.ts` is added alongside it. If Phase 3 introduces a hybrid grammar+history approach, both hooks are available. Retirement can happen at that point if the grammar hook is superseded.

---

### 13.5 [DP-5] Highlight the Matched Prefix in Suggestion Rows — **Reversed: Now Implemented**

**Original decision (v1.1):** No highlighting — plain text for all rows.

**Reversal (v1.11):** Bold prefix highlighting is now implemented. The matched prefix is rendered as `<strong className={styles.matchHighlight}>` with `font-weight: 700` and `color: var(--primary-color)`, giving immediate visual feedback of where the typed characters end and the completion begins. The implementation is contained entirely in the row JSX and the `.matchHighlight` CSS rule — no changes to the hook or filtering logic were required.

---

### 13.6 Accept While in History-Navigation Mode

**Scenario:** The user presses ArrowUp (entering history-nav mode, `historyIndex = 2`), the loaded command opens the dropup (but only if the loaded command matches — which it won't, because history-nav does not call `onCommandChange`). So by design the dropup is never open when the user is in history-nav mode.

**Residual concern:** The user is in history-nav mode (`historyIndex ≥ 0`), presses ArrowDown multiple times past index 0 to exit history-nav, is now back to their draft — but if they had started typing *before* pressing ArrowUp, the draft is the original typed text. If they then accept a suggestion, `SET_COMMAND` fires. Before the §12.4 fix, `historyIndex` would remain at `-1` (it was already reset by `EXIT_HISTORY`). After the fix, `SET_COMMAND` also resets it anyway. No issue.

**The real fix target (§12.4):** The user presses ArrowUp (enters history-nav, `historyIndex = 0`), then types a character (which fires `onChange`, then dispatches `SET_COMMAND`, which under the old reducer leaves `historyIndex = 0`). They see the dropup appear. They press Enter to accept a suggestion: `onChange(suggestion)` → `SET_COMMAND`. Without the fix, `historyIndex` is still `0`, so the next ArrowDown press dispatches `EXIT_HISTORY` unexpectedly. With the fix, `SET_COMMAND` resets `historyIndex = -1`, so the next ArrowDown is a plain boundary-check. This edge case is resolved.

---

### 13.7 Mouse Click on a Dropup Item

Mouse click handling is covered by:
1. `onMouseDown={e => e.preventDefault()}` on each item — prevents textarea from losing focus.
2. `onClick={() => ac.accept(i, v => onChange(v))}` on each item — inserts the suggestion.

There is no `onMouseDown`-triggered close-then-reopen race condition because Step 1 prevents the blur from firing entirely.

---

### 13.8 Very Long History Entries

A history entry with an extremely long first line (e.g., the user pasted a multi-thousand-character JSON into the command field and sent it with `load`) is truncated in the dropup row via CSS `text-overflow: ellipsis`. The accepted suggestion always contains the full text — nothing is lost. The textarea will auto-grow to accommodate it.

Note: Under `MAX_BUFFER = 63_488` (62 KB), the maximum size of any single history entry is bounded by the WebSocket send limit in `sendCommand`, so pathologically large entries are impossible.

---

### 13.9 Command Trimming and Match Operands

`sendCommand` trims the command before storing it in `history`. Therefore all stored history entries have no leading or trailing whitespace.

The `command` in the textarea may have leading spaces (unusual but possible). Matching uses `command.trimStart()` as the needle so leading whitespace in the current input does not suppress suggestions. Trailing whitespace is NOT trimmed for the needle — `"create node "` (trailing space) should only match entries starting with `"create node "` (which in practice means entries starting with `"create node "` followed by more text, since stored entries have no trailing space; this effectively narrows the match, which is correct).

---

### 13.10 `command` with Newlines Already Present

If the user has typed a multi-line command (via Shift+Enter) and there is a newline already in the command, the matching needle includes the newline and all subsequent lines. This severely constrains the match (only entries with that exact multiline prefix match). In practice, once the user has a newline in the input, the dropup is almost certainly empty, and it will not open. This is correct behavior — the user is composing a multi-line command and does not need suggestions for it.

---

### 13.11 Testing

The `useHistoryAutocomplete` hook is a pure UI hook with no side effects (only `useState` + `useMemo`). The filtering and deduplication logic can be tested as a pure function extracted from the `useMemo` body:

**Suggested test file:** `src/hooks/__tests__/useHistoryAutocomplete.test.ts`

Test cases to cover:
- Empty `command` returns `[]`
- Whitespace-only `command` returns `[]`
- No matching entries returns `[]`
- Prefix match (case-insensitive) returns correct entries newest-first
- Substring that is NOT a prefix does not appear in results (when using prefix matching)
- Deduplication: duplicate entries return only the first occurrence
- Results capped at `MAX_AUTOCOMPLETE_SUGGESTIONS`
- Multi-line entry matched by single-line prefix (entry `"create node\nwith type"` matched by `"create no"`)
- `navigate(+1)` wraps from last to first (DP-2: A)
- `navigate(-1)` from `activeIndex = -1` goes to last item

---

## 14. Out of Scope (Phase 3)

The following items are explicitly deferred:

- **Grammar-template autocomplete:** Re-introduction of the original `useAutocomplete.ts` command-grammar suggestions alongside history-based suggestions (e.g., a separate section in the dropup, or a merged ranked list with grammar entries pinned to the bottom).
- **Fuzzy / subsequence matching** (e.g., typing `"cna"` to match `"create node alpha"`).
- **Search-as-you-type within history browsing:** History navigation (ArrowUp/ArrowDown when the dropup is closed) keeps its current behavior of loading exact history entries sequentially.
- **Delete history entries from the dropup** (e.g., a per-row ✕ button that removes the entry from localStorage).
- **Pinning / favouriting** individual commands.
- **Highlighted matched prefix in dropup rows** (pending DP-5 resolution).
- **Keyboard shortcut to open the dropup explicitly** (e.g., `Ctrl+Space` to show all history entries for the current prefix when the dropup is closed).
