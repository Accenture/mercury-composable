# Command Input — Current Behavior Reference

**Component:** `src/components/CommandInput/CommandInput.tsx`  
**Hook:** `src/hooks/useWebSocket.ts` (history, send logic)  
**Hook:** `src/hooks/useAutocomplete.ts` (dropdown)  
**Last documented:** March 26 2026

---

## Table of Contents

1. [Visual Layout](#1-visual-layout)
2. [Disconnected State](#2-disconnected-state)
3. [Single-Line Mode (Default)](#3-single-line-mode-default)
4. [Multiline Mode](#4-multiline-mode)
5. [Sending a Command](#5-sending-a-command)
6. [Command History](#6-command-history)
7. [Autocomplete Dropdown](#7-autocomplete-dropdown)
8. [Info Icon Popover](#8-info-icon-popover)
9. [Edge Cases and Interactions](#9-edge-cases-and-interactions)

---

## 1. Visual Layout

The CommandInput is fixed at the bottom of the left panel, below the scrollable console output. It is always visible regardless of scroll position.

The component is structured in three vertical rows:

```
┌──────────────────────────────────────────────────────────┐
│  Command  ⓘ                            [ ] Multiline     │  ← Label row
├──────────────────────────────────────────────────────────┤
│  [ textarea ─────────────────────── ]  [ Send ]          │  ← Input row (single-line)
├──────────────────────────────────────────────────────────┤
│                            Enter to send · Tab to auto…  │  ← Hint text
└──────────────────────────────────────────────────────────┘
```

**Label row** (always visible):
- The text label **"Command"** on the left.
- An **ⓘ info icon** immediately to its right — a small circled "i". Hovering it reveals the quickstart popover (see [Section 8](#8-info-icon-popover)).
- A **"Multiline" checkbox** on the far right of the same row. Checking or unchecking it toggles multiline mode (see [Section 4](#4-multiline-mode)).

**Input area** (layout depends on mode):
- *Single-line mode:* the textarea and the **Send** button share one horizontal row, with the textarea taking all available width and the Send button pinned to the right.
- *Multiline mode:* the textarea occupies its own full-width row (5 rows tall by default); the **Send** button is a separate full-width button directly below the textarea.

**Hint text** (always visible below the input):
- Dynamically reflects the current mode and connection state (see each section below).

---

## 2. Disconnected State

When the WebSocket is **not** connected, the entire input area is locked:

| Element | Appearance / Behaviour |
|---|---|
| Textarea | Greyed out, `cursor: not-allowed`, 60 % opacity. Cannot be typed into. |
| Textarea placeholder | `"Not connected"` |
| Send button | Disabled, 50 % opacity, `cursor: not-allowed`. |
| Hint text | `"Enter your test message once it is connected"` |
| Multiline checkbox | Remains interactive — the mode can still be toggled. |
| Info icon | Remains interactive — the popover still appears on hover. |

Once the connection is established, all elements become interactive immediately.

---

## 3. Single-Line Mode (Default)

Single-line mode is the default state when the playground first loads or after any command is sent.

### 3.1 Appearance

- The textarea renders as a single-row text field (`rows=1`). It can be manually resized vertically by dragging the resize handle in the bottom-right corner of the textarea.
- The Send button sits to the right of the textarea on the same row.
- Placeholder text (when connected and empty): `"Enter command (Enter to send · Tab to autocomplete · ↑↓ for history)"`
- Hint text (below the input): `"Enter to send · Shift+Enter for new line · Tab to autocomplete"`
- Browser autocorrect, autocomplete, and spell-check are disabled on the input.

### 3.2 Keyboard Shortcuts — Single-Line Mode

| Key | Condition | Result |
|---|---|---|
| **Enter** | Autocomplete dropdown closed, or no item highlighted | Sends the command (same as clicking Send). |
| **Enter** | Autocomplete dropdown open and an item is highlighted | Accepts the highlighted suggestion; does **not** send. |
| **Shift+Enter** | Any | Inserts a newline at the caret (the textarea expands visually). The command is not sent. |
| **Tab** | Suggestions exist (dropdown open or not) | Accepts the first (or currently highlighted) suggestion. Dropdown closes. |
| **Tab** | No suggestions | No effect (key is not consumed). |
| **Escape** | Autocomplete dropdown is open | Closes the dropdown. The typed text is unchanged. |
| **Escape** | Dropdown is closed | Not consumed — browser default applies. |
| **↑ ArrowUp** | Autocomplete dropdown is **open** | Moves the highlight one row up in the dropdown (wraps to the bottom). History is **not** navigated. |
| **↑ ArrowUp** | Autocomplete dropdown is **closed** | Enters history navigation — see [Section 6](#6-command-history). |
| **↓ ArrowDown** | Autocomplete dropdown is **open** | Moves the highlight one row down (wraps to the top). History is **not** navigated. |
| **↓ ArrowDown** | Autocomplete dropdown is **closed** | Navigates forward in history — see [Section 6](#6-command-history). |
| Any other key | — | Passed to the parent history handler (no effect unless ArrowUp/Down). |

---

## 4. Multiline Mode

Multiline mode is activated by checking the **"Multiline"** checkbox in the label row, or automatically when an autocomplete suggestion tagged as `multiline` is accepted.

### 4.1 Appearance Changes

| Element | Single-line | Multiline |
|---|---|---|
| Textarea height | 1 row (single line) | 5 rows (expands for multi-line input) |
| Send button position | Right of textarea, same row | Full-width, directly below the textarea |
| Placeholder | `"Enter command (Enter to send · ↑↓ for history)"` | `"Enter command (Ctrl+Enter to send · ↑↓ for history)"` |
| Hint text | `"Enter to send · Shift+Enter for new line · Tab to autocomplete"` | `"Ctrl+Enter to send · Enter for new line · Shift+Enter for new line"` |
| Autocomplete dropdown | Shown when typing | **Hidden** — dropdown never appears in multiline mode |

### 4.2 Keyboard Shortcuts — Multiline Mode

| Key | Result |
|---|---|
| **Ctrl+Enter** (Win/Linux) or **⌘+Enter** (Mac) | Sends the command. Multiline mode is automatically switched **off** after sending. |
| **Enter** | Inserts a newline at the caret. The command is not sent. |
| **Shift+Enter** | Inserts a newline at the caret (same as plain Enter in this mode). |
| **↑ ArrowUp** — caret is **not** on the first line | Browser moves the caret up one line. History is **not** navigated. |
| **↑ ArrowUp** — caret **is** on the first line | Enters history navigation — see [Section 6](#6-command-history). |
| **↓ ArrowDown** — caret is **not** on the last line | Browser moves the caret down one line. History is **not** navigated. |
| **↓ ArrowDown** — caret **is** on the last line | Navigates forward in history — see [Section 6](#6-command-history). |

### 4.3 Toggling Back to Single-Line Mode

Multiline mode is turned off in three ways:
1. The user **unchecks** the "Multiline" checkbox.
2. A command is **sent** (via Ctrl/⌘+Enter or the Send button) — the mode resets to single-line automatically after the send.
3. An autocomplete suggestion is accepted that does **not** have the `multiline` flag (this can happen when multiline mode is still on from a previous suggestion).

In all cases, after switching back to single-line mode, keyboard focus returns to the textarea.

---

## 5. Sending a Command

### 5.1 Preconditions

A command can only be sent when:
- The WebSocket is **connected**.
- The command field is **not empty or whitespace-only** (the Send button is disabled otherwise, and keyboard send shortcuts are ignored).

### 5.2 What Happens on Send

1. The command text is **trimmed** of leading and trailing whitespace.
2. The trimmed text is sent as a WebSocket message.
3. The command is **pushed to history** in localStorage (see [Section 6](#6-command-history)).
4. The command field is **cleared** to an empty string.
5. History navigation state is reset (`historyIndex` → -1, `draftCommand` → empty).
6. Focus returns to the textarea.
7. If multiline mode was on, it is switched off.

### 5.3 Special Case: the `load` Command

If the trimmed command is exactly `"load"`, sending triggers additional behaviour:

- If the **payload editor** (right panel) contains a non-empty payload, that payload is automatically sent as a **second WebSocket message** immediately after `"load"`.
- If the payload editor is empty, a local error message is appended to the console: `ERROR: please paste JSON/XML payload in input text area`. The `"load"` command itself is still sent.

### 5.4 Sending Via the Send Button

Clicking the **Send** button is identical to pressing Enter (single-line) or Ctrl/⌘+Enter (multiline). The button is disabled (visually and functionally) when either:
- The WebSocket is not connected, or
- The command field is empty or contains only whitespace.

---

## 6. Command History

### 6.1 Storage

- Up to **50** commands are stored per playground in **localStorage**.
- History is keyed separately for each playground (Minigraph and JSON-Path have independent histories).
- History persists across browser refreshes and navigation between playgrounds.
- Duplicate consecutive entries are not stored (if the most recent history entry is identical to the new command, the history list is not modified).

### 6.2 Navigating History

History navigation is available in both single-line and multiline modes, with the boundary behaviour detailed in sections 3 and 4. The following describes the general flow:

**Entering history (first ↑ press while not in history mode):**
- The current in-progress text in the input field is saved as a **draft**.
- The **most recent** history entry is loaded into the input.
- The history cursor is set to position 0 (newest).

**Pressing ↑ again (going further back):**
- The next older command is loaded.
- This continues until the oldest entry is reached, at which point further ↑ presses have no effect.

**Pressing ↓ while in history (going forward):**
- The next newer command is loaded.
- When the cursor passes the most recent entry (position 0) and the user presses ↓ once more, **the saved draft is restored** to the input field, and history navigation mode exits.

**Key invariant:** In-progress text typed before pressing ↑ is never lost — it is preserved in the draft and will be restored when the user presses ↓ past the most recent history entry.

---

## 7. Autocomplete Dropdown

The autocomplete dropdown is only active in **single-line mode**. It never appears in multiline mode.

### 7.1 Opening the Dropdown

The dropdown opens automatically whenever the user types (any `onChange` event). It does **not** open when a history entry is loaded via arrow-key navigation — the dropdown only opens on real typing.

### 7.2 What Suggestions Are Shown

Suggestions are filtered from a fixed list of command templates (`COMMAND_SUGGESTIONS`) on every keystroke using these rules (all matching is case-insensitive):

1. The input is split on whitespace. Only the **first line** is used for matching.
2. A suggestion matches if every input token is a **prefix** of the corresponding suggestion token at the same position, and the input has no more tokens than the suggestion.
3. A suggestion is **excluded** if the input already exactly equals the first line of its template (the dropdown hides once the command is fully typed).
4. An **empty** input produces no suggestions (dropdown stays closed).

### 7.3 Suggestion Item Appearance

Each row in the dropdown shows three parts:
- **Keywords** (left, monospace, bold): the suggestion's fixed keyword tokens joined by spaces (e.g. `create node`).
- **Hint** (middle, muted): a short description of the command.
- **"multi-line" badge** (right, pill): only present on suggestions that will activate multiline mode when accepted (e.g. `create node`, `instantiate graph`).

The currently highlighted row has a primary-colour background. All other text on the highlighted row turns white, including the hint and badge.

### 7.4 Mouse Interaction

Clicking a suggestion (technically `mousedown`) accepts it. `mousedown` is used instead of `click` to prevent the textarea from losing focus — focus remains on the textarea after selection.

### 7.5 Keyboard Interaction Inside the Dropdown

| Key | Result |
|---|---|
| **↑ ArrowUp** | Moves the highlight one row up; wraps from the first item to the last. |
| **↓ ArrowDown** | Moves the highlight one row down; wraps from the last item to the first. |
| **Tab** | Accepts the currently highlighted item, or the first item if nothing is highlighted. |
| **Enter** | Accepts the currently highlighted item. If no item is highlighted, Enter sends the command instead (dropdown is bypassed). |
| **Escape** | Closes the dropdown without changing the input. |

The dropdown list auto-scrolls so the highlighted item is always visible.

### 7.6 Accepting a Suggestion

When a suggestion is accepted (by any method):
1. The command field is replaced with the suggestion's **template text** (may be multi-line, with `{placeholder}` tokens indicating where the user should type).
2. The dropdown is closed.
3. Focus returns to the textarea.
4. If the suggestion is tagged as `multiline`, **multiline mode is activated** simultaneously.

### 7.7 Dismissing the Dropdown

The dropdown closes when:
- A suggestion is accepted.
- **Escape** is pressed.
- The textarea loses focus (with a 150 ms delay to ensure a mousedown click on a suggestion can fire before the blur handler runs).
- The user enters history navigation (↑ when dropdown is closed).

### 7.8 Available Command Templates

The following commands have autocomplete entries. The table lists the keyword tokens that trigger each suggestion and notes which ones activate multiline mode.

| Keywords | Template (summarised) | Multi-line? |
|---|---|---|
| `help` | `help` | — |
| `help create` | `help create` | — |
| `help update` | `help update` | — |
| `help edit` | `help edit` | — |
| `help delete` | `help delete` | — |
| `help connect` | `help connect` | — |
| `help list` | `help list` | — |
| `help describe` | `help describe` | — |
| `help export` | `help export` | — |
| `help import` | `help import` | — |
| `help data-dictionary` | `help data-dictionary` | — |
| `help instantiate` | `help instantiate` | — |
| `help execute` | `help execute` | — |
| `help inspect` | `help inspect` | — |
| `help run` | `help run` | — |
| `create node` | `create node {name}\nwith type {type}\nwith properties\n{key}={value}` | ✔ |
| `update node` | `update node {name}\nwith type {type}\nwith properties\n{key}={value}` | ✔ |
| `edit node` | `edit node {name}` | — |
| `delete node` | `delete node {name}` | — |
| `delete connection` | `delete connection {nodeA} and {nodeB}` | — |
| `clear node` | `clear node {name}` *(alias for delete node)* | — |
| `clear connection` | `clear connection {nodeA} and {nodeB}` *(alias for delete connection)* | — |
| `clear cache` | `clear cache` | — |
| `connect` | `connect {node-A} to {node-B} with {relation}` | — |
| `list nodes` | `list nodes` | — |
| `list connections` | `list connections` | — |
| `describe graph` | `describe graph` | — |
| `describe node` | `describe node {name}` | — |
| `describe connection` | `describe connection {node-A} and {node-B}` | — |
| `describe skill` | `describe skill {skill.route.name}` | — |
| `describe skill graph.data.mapper` | `describe skill graph.data.mapper` | — |
| `describe skill graph.math` | `describe skill graph.math` | — |
| `describe skill graph.js` | `describe skill graph.js` | — |
| `describe skill graph.api.fetcher` | `describe skill graph.api.fetcher` | — |
| `describe skill graph.extension` | `describe skill graph.extension` | — |
| `describe skill graph.island` | `describe skill graph.island` | — |
| `describe skill graph.join` | `describe skill graph.join` | — |
| `export graph as` | `export graph as {name}` | — |
| `import graph from` | `import graph from {name}` | — |
| `import node` | `import node {node-name} from {graph-name}` | — |
| `instantiate graph` | `instantiate graph\n{constant} -> input.body.{key}` | ✔ |
| `start graph` | `start graph\n{constant} -> input.body.{key}` *(alias for instantiate)* | ✔ |
| `execute node` | `execute node {name}` | — |
| `execute` | `execute {node-name}` *(short form)* | — |
| `inspect` | `inspect {variable_name}` | — |
| `run` | `run` | — |

---

## 8. Info Icon Popover

Next to the "Command" label is a small circled **"i"** icon. Hovering the mouse over it (no click required) slides up a floating popover panel.

### 8.1 Popover Content

The popover is titled **"Getting started — type a keyword to begin"** and lists the top-level command keywords with a one-line description each:

| Keyword | Description | Alias |
|---|---|---|
| `help` | List all help topics, or get help for a specific command | — |
| `create` | Create a new graph node | — |
| `update` | Update an existing node | — |
| `edit` | Print raw node data ready for editing and re-submitting | — |
| `delete` | Delete a node or a connection | `clear` |
| `clear cache` | Clear cached API fetcher results | — |
| `connect` | Connect two nodes with a named relation | — |
| `list` | List all nodes or connections in the graph | — |
| `describe` | Describe the graph, a node, connection or skill | — |
| `export` | Export the graph model to a JSON file | — |
| `import` | Import a graph model or a single node from a file | — |
| `instantiate` | Create a runnable graph instance with mock input | `start` |
| `execute` | Execute a single node skill in isolation | — |
| `inspect` | Inspect a state-machine variable | — |
| `run` | Run the graph instance from root to end | — |

Aliases are shown inline within the description as `· alias: <alias>` in bold monospace font.

### 8.2 Popover Behaviour

- Appears on **hover** of the icon (CSS `:hover` transition — no JavaScript required).
- Fades in and slides up slightly (150 ms CSS transition).
- Positioned **above** the icon so it does not obscure the input.
- Width is fixed at 25 rem. If the popover would extend off-screen at the left edge, it remains anchored to the left edge of the icon.
- Disappears when the cursor moves away from the icon.
- The icon border and text colour change to the primary accent colour while hovered.

---

## 9. Edge Cases and Interactions

### 9.1 Shift+Enter in Single-Line Mode

Pressing Shift+Enter in single-line mode inserts a literal newline into the textarea. The textarea expands visually. The command will be sent as a multi-line string when Enter (or the Send button) is pressed. This is different from switching to multiline mode — the **Multiline checkbox remains unchecked** and the autocomplete dropdown is still active.

### 9.2 Autocomplete with History Navigation

- When the user loads a history entry via ↑/↓, the dropdown does **not** open (the dropdown only opens on real `onChange` events, not history loads). However, if the loaded command happens to match suggestions, those suggestions exist internally — and as soon as the user types any character, the dropdown will open showing relevant matches.
- History navigation clears the autocomplete highlight (calls `dismiss()`) before delegating to the history handler, so the two systems never conflict.

### 9.3 Accepting a Multiline Suggestion from the Dropdown

When the user accepts a suggestion with the `multiline` flag:
1. The template (which contains literal `\n` newlines, e.g. `create node {name}\nwith type {type}\nwith properties\n{key}={value}`) is inserted into the command field.
2. Multiline mode is activated automatically (the Multiline checkbox becomes checked).
3. The dropdown closes.
4. The user edits the multi-line template in the now-expanded textarea and sends with Ctrl/⌘+Enter.

### 9.4 Send Clears Multiline Mode

After any successful send in multiline mode (Ctrl/⌘+Enter or the Send button), multiline mode is **always turned off**. The next command starts in single-line mode. This ensures the user is never left in multiline mode unexpectedly after a send.

### 9.5 History is Per-Playground

The Minigraph playground and the JSON-Path playground maintain completely separate command histories in localStorage. Switching between playgrounds does not mix or erase either history.

### 9.6 Max History Size

Once 50 history entries have been saved, the oldest entry is dropped when a new command is added. The limit is defined by `MAX_HISTORY = 50` in `src/config/playgrounds.ts`.

### 9.7 Send Button State vs. Textarea Disabled State

These are two independent conditions:
- `inputDisabled` (textarea greyed out): `true` when the WebSocket is **not connected**. Prevents typing.
- `sendDisabled` (Send button greyed out): `true` when not connected **or** the command is empty/whitespace-only. Prevents sending even if somehow the textarea is editable.

In practice, both conditions are always in sync (disconnected = textarea disabled = send disabled). The distinction becomes relevant only if the internals were changed to allow typing while connecting.

### 9.8 The `load` Command and Payload

The word `load` has special backend semantics. When the user sends `load`, the frontend also immediately sends the content of the **Payload Editor** (right panel) as a second WebSocket message. If the payload is empty, a client-side error is appended to the console and no payload message is sent, but the `load` command is still transmitted to the server. This is the only command with this dual-message behaviour.
