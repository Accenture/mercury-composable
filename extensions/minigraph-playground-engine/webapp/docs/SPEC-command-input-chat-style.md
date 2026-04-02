# Command Input — Chat-Style Redesign

**Status:** Proposed  
**Version:** 1.7  
**Phase:** Command Input Revamp — Phase 1 (chat-style UX + autocomplete disabled)  
**Depends on:** [SPEC-command-input-behavior.md](./SPEC-command-input-behavior.md) (current state)  
**Last updated:** March 26 2026

---

## Changelog

| Version | Date | Changes |
|---|---|---|
| 1.0 | 2026-03-26 | Initial draft |
| 1.1 | 2026-03-26 | Fixed 5 gaps: `useEffect([command])` made required (§5.1, §11.1); Tab no-op handler added to §11.1 checklist; autocomplete CSS classes given explicit delete guidance (§11.6); `dropdownRef`/`activeItemRef`/`handleAccept`/`ac.dismiss()` listed for removal (§11.1); `autoComplete`/`autoCorrect`/`spellCheck` carry-over noted (§11.1); caret end-of-text placement promoted from recommendation to requirement with component owner assigned (§12.5) |
| 1.2 | 2026-03-26 | Fixed 3 remaining issues: incorrect "harmless" caveat in §12.5 replaced with accurate regression description and two implementation options; two separate `useEffect([command])` blocks in §11.1 consolidated into a single effect to eliminate ordering ambiguity; §12.10 optimization note updated to reflect the `useEffect` model instead of key-event branching |
| 1.3 | 2026-03-26 | Fixed 2 remaining issues: replaced `isHistoryNavRef` gating pattern (which leaked to `true` when history navigation produced no command change) with a `requestAnimationFrame` caret placement co-located in the arrow-key branch of `handleKeyDown` (§11.1, §12.5); corrected §5.3 — manual resize is discarded on every command change via `useEffect`, not only on send |
| 1.4 | 2026-03-26 | Fixed 2 residual wording issues: §12.4 "after the next send" corrected to "on the next command change" to match §5.3; §12.5 "first-line boundary" broadened to "boundary" to cover both `ArrowUp` and `ArrowDown` no-op cases |
| 1.5 | 2026-03-26 | Fixed `overflow-y` contradiction between §5.2 and §11.6: both sections now consistently specify `overflow-y: auto`; `overflow-y: hidden` removed from §11.6 (Option A chosen — CSS `auto` throughout, one-frame scrollbar flash accepted as imperceptible); restored missing v1.3 changelog row |
| 1.6 | 2026-03-26 | Added `handleChange` removal to §11.1 checklist (dead one-line wrapper once `ac` is removed; textarea wired directly to `onChange`); corrected §5.2 scrollbar flash explanation — reading `scrollHeight` forces a synchronous layout reflow so the intermediate `height: auto` state never reaches the screen; there is no flash |
| 1.7 | 2026-03-26 | Removed stale `handleChange` clause from §5.1 — the sentence *"handleChange can call the same shared resize helper"* described a pattern that no longer exists once the textarea is wired directly to `onChange` |

---

## Table of Contents

1. [Goals and Non-Goals](#1-goals-and-non-goals)
2. [What Changes vs. What Stays](#2-what-changes-vs-what-stays)
3. [New Visual Layout](#3-new-visual-layout)
4. [Unified Keyboard Behavior](#4-unified-keyboard-behavior)
5. [Auto-Grow Textarea](#5-auto-grow-textarea)
6. [History Navigation](#6-history-navigation)
7. [Disconnected State](#7-disconnected-state)
8. [Sending a Command](#8-sending-a-command)
9. [Tab Key Behavior](#9-tab-key-behavior)
10. [Hint Text and Placeholders](#10-hint-text-and-placeholders)
11. [Component and Prop Changes](#11-component-and-prop-changes)
12. [Edge Cases and Decision Points](#12-edge-cases-and-decision-points)
13. [Out of Scope (Phase 2)](#13-out-of-scope-phase-2)

---

## 1. Goals and Non-Goals

### Goals

- **Eliminate modal complexity.** Remove the Multiline checkbox and the two distinct send-key modes (`Enter` vs `Ctrl+Enter`). The user should never have to think about which mode they are in.
- **Adopt the universal chat convention.** Every developer knows: `Enter` sends, `Shift+Enter` inserts a newline. No learning curve, no documentation required.
- **Seamless single-to-multi-line flow.** The textarea grows naturally as the user types or pastes. There is no moment where the user must "switch modes" before they can write a multi-line command.
- **Disable autocomplete cleanly.** The dropdown and all associated keyhandling are switched off without leaving dead code pathways. The feature is suspended, not deleted, to ease the Phase 2 re-introduction.

### Non-Goals

- This phase does **not** redesign or replace the autocomplete feature. Autocomplete is disabled as-is and left for a separate phase.
- This phase does **not** change anything about the Console, graph view, or any other component.
- This phase does **not** change server-side behavior or the WebSocket protocol.

---

## 2. What Changes vs. What Stays

| Concern | Current (phase 0) | New (phase 1) |
|---|---|---|
| **Send key** | `Enter` in single-line mode; `Ctrl+Enter` in multiline mode | `Enter` always (+ `Ctrl/⌘+Enter` as secondary shortcut) |
| **Newline key** | `Shift+Enter` always inserts a newline | Same — `Shift+Enter` always inserts a newline |
| **Multiline toggle** | Explicit "Multiline" checkbox in the label row | Removed entirely |
| **Textarea height** | Fixed at 1 row (single-line) or 5 rows (multiline) | Auto-grows from 1 row to a capped maximum based on content |
| **Autocomplete** | Active in single-line mode; hidden in multiline mode | Disabled entirely (hook and dropdown bypassed) |
| **Tab key** | Triggers autocomplete accept | No-op (reserved for Phase 2) |
| **Arrow-key history boundary logic** | Only active in multiline mode | Always active (same logic, mode guard removed) |
| **`multiline` state in Playground.tsx** | `useState(false)`; toggled by checkbox and autocomplete | Removed |
| **`onToggleMultiline` prop** | Threaded through Playground → LeftPanel → CommandInput | Removed |
| **Send button layout** | Single-line: right of textarea; multiline: full-width below textarea | Always right of textarea, anchored to the bottom edge of the (growing) textarea |
| **Hint text** | Mode-dependent (two variants) | Single unified string for connected state |
| **Placeholder text** | Two mode-dependent variants | Single unified variant |
| **Info icon popover** | Unchanged | Unchanged |
| **`load` command dual-message** | Unchanged | Unchanged |
| **History storage (localStorage)** | Unchanged | Unchanged |
| **Draft save/restore on history nav** | Unchanged | Unchanged |

---

## 3. New Visual Layout

The component collapses to a single layout — no conditional rendering based on mode.

```
┌──────────────────────────────────────────────────────────┐
│  Command  ⓘ                                              │  ← Label row (Multiline checkbox removed)
├──────────────────────────────────────────────────────────┤
│  [ textarea — grows vertically ─────────── ]  [ Send ]   │  ← Input row (always this layout)
├──────────────────────────────────────────────────────────┤
│           Enter to send · Shift+Enter for new line · ↑↓  │  ← Hint text (single static string)
└──────────────────────────────────────────────────────────┘
```

**Label row:**
- **"Command"** label (left) — unchanged.
- **ⓘ info icon** (immediately right of label) — unchanged.
- **Multiline checkbox — removed.** The right side of the label row is now empty.

**Input row:**
- The textarea and Send button always share one horizontal row.
- The Send button is anchored to the **bottom** of the input row (`align-items: flex-end`) so that as the textarea grows the button stays at the bottom edge, not the top.
- The textarea takes all remaining horizontal width.

**Hint text:**
- One static string for the connected state (see [Section 10](#10-hint-text-and-placeholders)).

---

## 4. Unified Keyboard Behavior

There is now exactly one set of keyboard rules, applied at all times.

| Key | Condition | Result |
|---|---|---|
| **Enter** | Command is non-empty, not Shift-held | Sends the command |
| **Enter** | Command is empty | No-op (send is also disabled on the button) |
| **Shift+Enter** | Any | Inserts a newline at the caret; textarea grows |
| **Ctrl+Enter** or **⌘+Enter** | Command is non-empty | Sends the command (secondary shortcut for muscle memory) |
| **↑ ArrowUp** | Caret is on the **first line** (or input is empty) | Enters / continues history navigation |
| **↑ ArrowUp** | Caret is **not** on the first line | Browser moves caret up one line (no history navigation) |
| **↓ ArrowDown** | Caret is on the **last line** | Continues / exits history navigation |
| **↓ ArrowDown** | Caret is **not** on the last line | Browser moves caret down one line (no history navigation) |
| **Tab** | Any | No-op — key is consumed but nothing happens (see [Section 9](#9-tab-key-behavior)) |
| **Escape** | Any | Not consumed — browser default applies |
| All other keys | — | Default browser behavior |

### Key Simplifications vs. Current

- **No mode check before Enter.** The same rule applies whether the content is one line or five.
- **No Ctrl+Enter required for multi-line content.** A user who has typed a 4-line command with `Shift+Enter` presses plain `Enter` to send it — identical to sending a one-line command.
- **Arrow-key boundary logic is always active.** The `if (multiline)` guard that previously restricted boundary-aware behaviour is removed. The boundary check runs on every `ArrowUp` / `ArrowDown` press.

---

## 5. Auto-Grow Textarea

### 5.1 Growth Mechanics

The textarea starts at a minimum height of 1 row and grows as content is added. Growth is driven by JavaScript (not CSS alone) because CSS `height: auto` on a textarea does not resize based on content.

**Implementation: `useEffect` watching `command` is required.** Because `CommandInput` is a React controlled component, the `onChange` event fires only on user interaction. When history navigation fires (`ENTER_HISTORY` / `SET_HISTORY_INDEX` dispatched by `useWebSocket`), the `command` prop changes externally and no `onChange` event fires on the textarea — the component simply re-renders with a new `value`. An `onChange`-only implementation would fail to resize the textarea when a history entry is loaded. A `useEffect(() => { /* resize */ }, [command])` must be the single canonical resize trigger — with `onChange` wired directly to the textarea, there is no `handleChange` wrapper to also update.

Resize logic:
1. Set `element.style.height = 'auto'` to collapse the element and get the true `scrollHeight`.
2. Set `element.style.height = element.scrollHeight + 'px'` to expand back to fit content.
3. CSS `max-height` caps the growth at a defined maximum; above that limit, the textarea scrolls internally and `overflow-y` becomes `auto`.

**On send (command cleared):**
- After the command field is set to an empty string, the `useEffect` fires again and the textarea collapses back to its minimum 1-row height.

### 5.2 Height Constraints

| Constraint | Value | Rationale |
|---|---|---|
| Minimum height | ~`2.5rem` (approx 1 row) | Consistent with current single-line appearance |
| Maximum height | `~10rem` (approx 5–6 rows) | Prevents the input from pushing the Send button off-screen on small viewports; user can still scroll the textarea above this cap |
| Overflow above max | `overflow-y: auto` (scrollbar appears) | CSS `auto` means: no scrollbar when `height = scrollHeight ≤ max-height`; scrollbar appears when content exceeds the cap and the browser clamps at `max-height`. There is no scrollbar flicker during the `height: auto` collapse step: reading `el.scrollHeight` in the resize effect forces a synchronous layout reflow, so the browser resolves the intermediate `height: auto` state before returning the measurement and never paints it. Only the final `height = scrollHeight + 'px'` value is ever rendered. |

> **Decision point:** The exact max-height value should be validated against the actual rendered font size and line-height so that the cap corresponds cleanly to a whole number of visible rows. `10rem` is the initial recommendation.

### 5.3 Resize Handle

The CSS `resize: vertical` property on the textarea currently allows the user to manually drag the textarea taller. This behavior is **retained** so users working with very long commands can expand further than the auto-grow maximum.

If the user manually drags the textarea and then makes any command change (types a character, pastes, or clears on send), the `useEffect([command])` runs and resets `style.height = 'auto'`, discarding the manual override immediately. The manual drag does **not** persist until the next send — it is overwritten on the very next keystroke.

> **Decision point:** This is the inherent behaviour of the `useEffect([command])` auto-grow approach. If persisting the manual height across keystrokes were required, the implementation would need to track an explicit "user has manually resized" flag and skip the `height = 'auto'` reset step until send. That complexity is not warranted for this phase; the tradeoff is accepted consciously.

### 5.4 Paste Behavior

When the user **pastes** content containing newlines:
- The `onChange` event fires normally.
- The auto-grow logic runs and the textarea expands to fit the pasted content (up to the max-height cap).
- No special handling is required.

---

## 6. History Navigation

History behavior is unchanged in logic. The only change is that the **boundary-aware arrow-key guard** (previously only active in multiline mode) is now **always active**. This is effectively the behaviour that was already in the multiline code path, promoted to be the single universal path.

For completeness, the full behaviour is restated here:

### 6.1 Entering History

**Condition:** `ArrowUp` is pressed while the caret is on the first line (or the input is empty/single-line).

1. The current text in the input is saved as the **draft**.
2. The most recent history entry is loaded into the input. The textarea auto-grows to fit the loaded entry.
3. The history cursor is set to index 0 (most recent).

### 6.2 Navigating Backward (older)

**Condition:** `ArrowUp` while already in history navigation mode.

- The next older entry is loaded.
- Stops at the oldest entry; further `ArrowUp` presses have no effect.

### 6.3 Navigating Forward (newer)

**Condition:** `ArrowDown` while in history navigation mode, caret on the last line of the loaded entry.

- The next newer entry is loaded.
- When advancing past index 0 (the most recent entry), the draft is restored and history navigation mode exits.

### 6.4 Multi-Line History Entries

A command with newlines (entered via `Shift+Enter`) is stored as a single history entry containing literal newline characters. When loaded from history:
- The full multi-line text is placed into the textarea.
- The auto-grow logic expands the textarea to show all lines.
- `ArrowUp` while the caret is **not** on the first line of the loaded entry moves the caret within the textarea (does not go further back in history).
- `ArrowUp` while the caret **is** on the first line of the loaded entry moves back one more history step.

### 6.5 What is Unchanged

- Limit of 50 entries per playground, persisted in localStorage.
- Per-playground isolation (Minigraph and JSON-Path histories are separate).
- No duplicate consecutive entries.
- Draft save/restore invariant: typing in progress before pressing `ArrowUp` is never lost.

---

## 7. Disconnected State

Disconnected state behavior is unchanged from the current implementation, with one simplification: there is no Multiline checkbox to describe.

| Element | Appearance / Behaviour |
|---|---|
| Textarea | Greyed out, `cursor: not-allowed`, 60 % opacity. Cannot be typed into. |
| Textarea placeholder | `"Not connected"` |
| Send button | Disabled, 50 % opacity, `cursor: not-allowed`. |
| Hint text | `"Enter your test message once it is connected"` |
| Info icon | Remains interactive — popover still appears on hover. |

---

## 8. Sending a Command

### 8.1 Preconditions (unchanged)

- The WebSocket must be **connected**.
- The command field must be **non-empty after trimming**.

### 8.2 Send Sequence (unchanged)

1. The command text is trimmed.
2. The trimmed text is sent as a WebSocket message.
3. The command is pushed to history.
4. The command field is cleared.
5. History navigation state is reset.
6. The textarea collapses back to its minimum 1-row height (via the auto-grow logic triggered by the empty string change).
7. Focus returns to the textarea.

> Note: step 7 — "multiline mode is switched off" — is removed because there is no longer a mode to switch.

### 8.3 Special Case: `load` Command (unchanged)

If the trimmed command is exactly `"load"`:
- If the payload editor contains content, that content is sent as a second WebSocket message.
- If the payload editor is empty, a client-side error is appended to the console.

---

## 9. Tab Key Behavior

With autocomplete disabled, Tab loses its previous function. The specified behavior for this phase is:

**Tab is consumed (preventDefault) and does nothing.**

### Rationale

| Option | Assessment |
|---|---|
| `preventDefault` + no-op | **Chosen.** Safe; prevents focus from jumping to the Send button (which would be the default browser action). Reserves Tab for future autocomplete without any behavioral change in Phase 2. |
| Let browser default fire | The browser would tab-focus to the Send button — unexpected and likely annoying. |
| Insert a literal tab character | Tab characters are meaningless in the command grammar and would confuse the server. |
| Insert 4 spaces | A common code-editor convention, but this is a command prompt, not a code editor. Would need stripping before send. |

> **Phase 2 note:** When autocomplete is re-introduced, Tab's handler is replaced with the autocomplete-accept logic. No behavioral migration required for users since Tab did nothing in this phase.

---

## 10. Hint Text and Placeholders

Both strings are now constant for the connected state — they no longer vary by mode.

| State | Placeholder text | Hint text |
|---|---|---|
| Connected | `"Enter command (Enter to send · Shift+Enter for new line)"` | `"Enter to send · Shift+Enter for new line · ↑↓ for history"` |
| Disconnected | `"Not connected"` | `"Enter your test message once it is connected"` |

The hint text is intentionally concise. Ctrl/⌘+Enter is not listed in the hint to avoid information overload — it exists as a silent secondary shortcut for users who discover it through muscle memory.

---

## 11. Component and Prop Changes

### 11.1 `CommandInput.tsx`

**Props removed:**
- `multiline?: boolean` — no longer needed.
- `onToggleMultiline?: (force?: boolean) => void` — no longer needed.

**Props unchanged:**
- `command`, `onChange`, `onKeyDown`, `onSend`, `sendDisabled`, `disabled`.

**Logic changes:**
- Remove all `if (multiline) { … } else { … }` branches. The single-line path becomes the only path, extended with the boundary-aware arrow-key logic from the multiline path.
- Remove the Multiline checkbox from the label row JSX.
- Remove the `dropdown` constant and all `useAutocomplete` hook usage. The `ac` variable and the `import { useAutocomplete }` statement are removed.
- Remove `dropdownRef`, `activeItemRef`, and `handleAccept` — all three are dead code once `ac` and the dropdown JSX are gone.
- Remove `handleChange` — it exists solely to call `ac.onCommandChange(value)` before forwarding to `onChange`. Once `ac` is removed it is a no-op wrapper. Wire the textarea's `onChange` prop directly to `(e) => onChange(e.target.value)`.
- Remove the `ac.dismiss()` call that currently precedes the `onKeyDown(e)` delegation in the arrow-key branch of `handleKeyDown`.
- Remove the `onBlur` handler on the textarea (its only purpose was the 150 ms dropdown dismiss delay; no longer needed).
- **Add a `Tab` no-op handler** in `handleKeyDown`: `if (e.key === 'Tab') { e.preventDefault(); return; }`. This is required — without it, Tab falls through to the browser default, which moves focus to the Send button. The handler must call `e.preventDefault()` before `useAutocomplete` is re-introduced in Phase 2.
- Add a **single** `useEffect(() => { /* resize textarea */ }, [command])` for auto-grow. This is the single canonical resize trigger — it handles both user typing and programmatic command changes (history loads, clear on send). The effect body runs `el.style.height = 'auto'` then `el.style.height = el.scrollHeight + 'px'`. See Section 5.1.
- For **caret end-of-text placement** after history navigation: place a `requestAnimationFrame` call directly in the arrow-key branch of `handleKeyDown`, immediately after calling `onKeyDown(e)`. This keeps the caret placement co-located with its trigger and eliminates all async coordination:
  ```typescript
  onKeyDown(e); // dispatch history action
  requestAnimationFrame(() => {
    const el = textareaRef.current;
    if (el) el.selectionStart = el.selectionEnd = el.value.length;
  });
  ```
  The `requestAnimationFrame` fires after React has committed the new `command` value to the DOM, guaranteeing the caret is placed at the end of the loaded history entry. When the history dispatch produces no change (empty history, already at oldest entry, etc.), the rAF still fires but places the caret at the end of the unchanged text — harmless, since the user was at the boundary anyway. No `isHistoryNavRef` is needed.
- `handleKeyDown` is simplified to the single unified rule set.
- The two textarea+button layouts collapse to one.
- `rows` attribute: always `1` (minimum; auto-grow handles expansion from there).
- Add `style` attribute management for dynamic height (`element.style.height`).
- Carry over `autoComplete="off"`, `autoCorrect="off"`, and `spellCheck={false}` from the current single-line textarea to the unified textarea. These attributes are currently only on the single-line branch; without this, the browser may offer autocorrect suggestions for command names.

### 11.2 `LeftPanel.tsx`

**Props removed:**
- `multiline: boolean`
- `onToggleMultiline: (force?: boolean) => void`

**Props unchanged:** all remaining props.

**Logic changes:** Remove the two props from the `CommandInput` call-site in the JSX.

### 11.3 `Playground.tsx`

**State removed:**
- `const [multiline, setMultiline] = useState(false)`

**Callbacks removed:**
- `handleToggleMultiline`

**Call-site changes:** Remove `multiline` and `onToggleMultiline` from the `<LeftPanel>` prop list.

### 11.4 `useAutocomplete.ts`

**No changes to the file itself.** The hook remains in the codebase, intact, for Phase 2 re-introduction. It is simply no longer imported or called from `CommandInput.tsx`.

### 11.5 `useWebSocket.ts`

**No changes required.** The `handleKeyDown` function in this hook handles only history navigation (`ArrowUp` / `ArrowDown`). It is unaware of multiline mode, so no changes are needed.

### 11.6 CSS — `CommandInput.module.css`

**Styles removed:**
- `.checkboxLabel` — the Multiline checkbox wrapper.
- `.sendButtonFullWidth` — dead code once the multiline JSX branch is removed.
- **Autocomplete dropdown styles** — `.suggestions`, `.suggestionItem`, `.suggestionItemActive`, `.suggestionTokens`, `.suggestionHint`, `.suggestionBadge`. These become dead CSS once the dropdown JSX constant is removed. Unlike `useAutocomplete.ts` (which is kept intact to ease Phase 2 re-introduction), the stylesheet does not carry self-documenting intent — it is just visual rules for DOM nodes that no longer exist. Deleting these classes reduces noise and the risk of accidental re-use. Phase 2 can reintroduce them alongside the new dropdown design (which may differ from the current one anyway).

**Styles changed:**
- `.textarea`: add `overflow-y: auto` and a `max-height` constraint (e.g. `10rem`). With `overflow-y: auto`, the auto-grow JS sets `height = scrollHeight`; when `scrollHeight ≤ max-height` no scrollbar appears; when content exceeds the cap the browser clamps the height at `max-height` and a scrollbar appears automatically. No JS overflow toggling is required.
- Remove the multiline layout variant if it was encoded as a conditional class (in the current implementation there is no separate multiline CSS class — the two layouts are controlled by conditional JSX, so the only CSS concern is the textarea height).

**Styles added:**
- None required beyond the `max-height` + `overflow-y` changes to `.textarea`.

**Styles unchanged:**
- `.inputRow`, `.inputWrapper`, `.sendButton`, `.hint`, `.label`, `.labelRow`, `.labelGroup`, `.infoWrapper`, `.infoIcon`, `.popover`, and all popover-related styles.

---

## 12. Edge Cases and Decision Points

### 12.1 Ctrl+Enter / ⌘+Enter as a Secondary Send Shortcut

**Included.** Users who built muscle memory from the old multiline send shortcut will not be broken. Both `e.ctrlKey` and `e.metaKey` + `Enter` trigger send.

**Ambiguity:** Should Ctrl+Enter also work when the command is empty? No — the same `sendDisabled` guard applies. Ctrl+Enter on an empty field is a no-op, consistent with plain Enter.

### 12.2 Pressing Enter on a Multi-Line Command

**Scenario:** A user types:
```
create node foo
with type mapper
with properties
key=value
```
and then presses Enter at the end of the last line.

**Behavior:** The entire four-line string is sent as a single WebSocket message. This is correct — the server expects multi-line command syntax.

**Potential user confusion:** A user who presses Enter to move to a new line (having forgotten the Shift+Enter convention) will accidentally send a partial command. This is the fundamental trade-off of chat-style input and is explicitly accepted by this design. The hint text and placeholder are the primary mitigations.

### 12.3 Pressing Enter in the Middle of a Multi-Line Command

**Scenario:** The caret is on line 2 of a 4-line command, not at the end.

**Behavior:** The command is sent immediately, with the caret position having no effect on the send behavior. Enter always sends regardless of caret position.

**Assessment:** This is consistent with how all chat apps behave. It may surprise users who think of Enter as a "go to next line" key, but the Shift+Enter convention resolves this.

### 12.4 Auto-Grow and the Manual Resize Handle

As specified in [Section 5.3](#53-resize-handle), the user can drag the textarea taller than the auto-grow maximum. On the next command change (typing, paste, or send), the `useEffect([command])` resets `style.height = 'auto'`, discarding the manual override immediately.

**Gap:** If the user pastes a very large payload (many hundreds of lines), the auto-grow logic would attempt to set a very large height before the `max-height` CSS kicks in. The `max-height` constraint handles this correctly — `scrollHeight` may be large but `max-height` caps the rendered height. The `overflow-y: auto` on the textarea displays a scrollbar.

### 12.5 History Entry that Exceeds the Max Height

**Scenario:** A previously saved command is very long (many lines). When loaded from history via `ArrowUp`:

1. The loaded text is placed into the textarea.
2. Auto-grow runs: the textarea grows to `max-height` and shows a scrollbar.
3. The caret is at the start of the text (browser default for programmatic value sets — this may vary).

**Requirement:** The caret must be placed at **end-of-text** after a history entry is loaded, for consistency with terminal behavior and to ensure the arrow-key boundary logic works correctly (see Section 12.6). This is implemented via a `requestAnimationFrame` call placed directly in the arrow-key branch of `CommandInput.tsx`'s `handleKeyDown`, immediately after `onKeyDown(e)` is called. See Section 11.1 for the exact code pattern.

> **Why `requestAnimationFrame` rather than a `useEffect` gate:** A `useEffect([command])` approach requires a flag (e.g. `isHistoryNavRef`) to distinguish history loads from user typing. That flag leaks to `true` when history navigation produces no `command` change — empty history, already at the oldest entry, or `ArrowDown` with no active history — because the effect never fires and never resets the ref. The next user keystroke then incorrectly snaps the caret to end-of-text mid-edit. The `requestAnimationFrame` approach avoids this entirely: it is co-located with the trigger, fires unconditionally after the React commit, and the `useEffect([command])` remains a pure resize-only concern with no caret logic.

### 12.6 Arrow Key at Boundary When Caret Position is Ambiguous

**Scenario in multiline text:** After loading a history entry, the browser may place the caret at position 0 (beginning of text). If the caret is at position 0 and the user presses `ArrowUp`, the boundary check (`isFirstLine = !beforeCaret.includes('\n')`) will return `true` (nothing before the caret), and history navigation will advance — potentially unintended if the user wanted to move the caret within the loaded text.

**Assessment:** This is a corner case of the existing boundary logic (not introduced by this redesign). The most pragmatic resolution is to accept it — if the caret is at the very beginning and the user presses `ArrowUp`, it is far more likely they want to go further back in history than navigate within the text.

**If the spec in 12.5 is implemented** (caret forced to end-of-text on load), then immediately after loading a history entry the caret will be at the end of the last line. Pressing `ArrowUp` will correctly move the caret up within the text until it reaches the first line, at which point further `ArrowUp` presses navigate history. This is the preferred flow.

### 12.7 Draft Containing Newlines

**Scenario:** The user types a multi-line command (with `Shift+Enter`), then presses `ArrowUp` to browse history.

- The multi-line in-progress text is saved as the draft (the `ENTER_HISTORY` action saves `state.command` — the full multi-line string).
- When the user presses `ArrowDown` past the newest history entry, the multi-line draft is restored.
- Auto-grow runs and the textarea re-expands to show the restored draft.

**Assessment:** This works correctly with the existing draft mechanism. No code change is required beyond removing the mode guard.

### 12.8 Send Button Alignment During Growth

As the textarea grows, the Send button must stay at the **bottom** of the input row, not float up with the top of the textarea. This is achieved by `align-items: flex-end` on `.inputRow`, which is already present in the current CSS. No change is required.

### 12.9 Focusing the Textarea After History Navigation

The current `handleKeyDown` in `CommandInput.tsx` calls `textareaRef.current?.focus()` after history operations (inside the autocomplete's `dismiss()` path). With autocomplete removed, ensure the focus call is placed appropriately in the simplified `handleKeyDown` after history-navigating arrow key presses are delegated to the parent.

> **Implementation note:** The `onKeyDown` prop in `CommandInput` delegates to `useWebSocket`'s `handleKeyDown`, which dispatches the reducer action. The textarea retains focus naturally unless explicitly blurred — no explicit `focus()` call is needed after history navigation. However, after accepting a history entry via the parent dispatch, developers should verify focus is not inadvertently stolen.

### 12.10 Resetting Height to `auto` on Every Keystroke

The auto-grow pattern (`el.style.height = 'auto'; el.style.height = el.scrollHeight + 'px'`) sets `height: auto` briefly on every keystroke. This causes a layout reflow. On modern hardware this is imperceptible for normal command lengths. For very large pastes it could be noticeable, but this is an edge case (users are not expected to paste thousands of lines into a command prompt).

If profiling ever shows a problem, the optimization is to track the previous `command` length in a `useRef` and skip the `style.height = 'auto'` reset step when the new length is greater than the previous length (content can only have grown, so the textarea cannot need to shrink). A ref storing the previous length is sufficient — no additional state is needed. For this phase, run the full resize on every `useEffect` invocation.

### 12.11 Autocomplete Suggestions Still Computed (`useAutocomplete` call removed)

When `useAutocomplete` is no longer called in `CommandInput`, the `getSuggestions()` function is also no longer called. Any command-matching logic that previously ran on every keystroke is now skipped entirely. There is no residual cost.

> **Phase 2 note:** When autocomplete is re-introduced, the `useAutocomplete` import and call are simply added back. Because no intermediate state depends on them, re-introduction has no side effects on the rest of the component.

### 12.12 The `onToggleMultiline` Callback in Existing Autocomplete Suggestion Acceptance

In the current code, accepting a `multiline`-flagged suggestion calls `onToggleMultiline(true)`. With `onToggleMultiline` removed and autocomplete disabled, this code path no longer exists. When autocomplete returns in Phase 2:

- Multi-line suggestion templates (e.g., `create node`) will be inserted as a string with literal `\n` characters.
- Because auto-grow handles expansion automatically, there is **no need to re-introduce `onToggleMultiline`**. The template fills the textarea and it grows. Phase 2 autocomplete can simply call `setCommand(template)` and auto-grow does the rest.
- The `multiline` flag on `CommandSuggestion` becomes advisory (for badge display only) rather than triggering a mode switch.

---

## 13. Out of Scope (Phase 2)

The following are explicitly deferred:

| Item | Notes |
|---|---|
| **Autocomplete redesign** | The `useAutocomplete` hook and `COMMAND_SUGGESTIONS` data remain in place; only the call site in `CommandInput` is removed. Phase 2 should re-attach the hook, reconsider the dropdown UX, and remove the now-unnecessary `multiline` flag from `CommandSuggestion`. |
| **Caret placement on suggestion accept** | Phase 2 can optionally position the caret at the first `{placeholder}` in the accepted template. |
| **Trigger key for autocomplete** | Now that Tab is a no-op, Phase 2 should decide whether Tab re-opens/accepts, or whether autocomplete is purely triggered by typing (the current model). |
| **Keyboard shortcut discoverability** | A tooltip on the Send button (`title="Enter to send"`) could help discoverability, but this is cosmetic and deferred. |
