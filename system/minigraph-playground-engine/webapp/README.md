# Playground Webapp

A lightweight, browser-based developer tool for interacting with Mercury Composable backend services over WebSocket. Each **playground** is a dedicated page for a specific backend endpoint — you write commands, send payloads, and observe live responses in real time.

This folder is the single source of the Playground UI for both engines: the Java engine in this repository and the Rust engine in the sibling `mercury` repository serve the bundle built here. Technical documentation for developers and AI agents lives in [docs/](docs/README.md).

---

## Table of Contents

- [Getting Started](#getting-started)
- [Layout Overview](#layout-overview)
- [Connection Bar](#connection-bar)
- [Console Output (Left Panel — Top)](#console-output-left-panel--top)
- [Command Input (Left Panel — Bottom)](#command-input-left-panel--bottom)
- [Payload Editor (Right Panel)](#payload-editor-right-panel)
- [Navigation & Quick Links](#navigation--quick-links)
- [Keyboard Reference](#keyboard-reference)
- [Persistence](#persistence)
- [Available Playgrounds](#available-playgrounds)
- [Adding a New Playground](#adding-a-new-playground)

---

## Getting Started

### Development server

```bash
npm ci
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser. The Vite dev server proxies `/ws` (WebSocket), `/api`, `/info`, `/health` and `/env` to an engine on port 8085, so start a MiniGraph playground example first (Java: `examples/minigraph-playground`; Rust: `cargo run -p minigraph-playground` in the `mercury` repo).

### Production release (deploy to an engine)

```bash
npm run release        # the Java engine (this repository) - the default
npm run release:rust   # the Rust engine (the sibling mercury repository)
npm run release:all    # both engines from one build
```

`release` cleans the target, builds the app (typecheck + `vite build`) and deploys the output in two parts: the hashed assets into `src/main/resources/public/assets/` (static content) and the entry page into `src/main/resources/template/playground.html`, which the engine's `get.index.html` function serves only when `app.env=dev`. Outside dev mode the home page is the plain service page, so a production deployment never shows the Playground UI. `src/main/resources/public/index.html` is that plain page and is left alone.

`release:rust` deploys the same bundle into `crates/knowledge-graph/resources/` of the `mercury` repo checked out beside this one (set `MERCURY_RUST_REPO` to point elsewhere) and mirrors the help pages there too: the help markdown is compiled into the bundle at build time and read by the engines at run time, so both copies come from `src/main/resources/help/` in this repository. The committed bundle and help pages in both repos are build artifacts of this folder — edit sources and help here, then release to both. Details: [docs/build-test-deploy.md](docs/build-test-deploy.md).

Once the backend JAR is running:

```
java -jar target/minigraph-playground-{version}.jar
```

visit [http://127.0.0.1:8085](http://127.0.0.1:8085).

---

## Layout Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  Title                                                          │
│  ● Disconnected   ws://…   [Start]    Tools: …   Quick Links:   │  ← Header
├────────────────────────────┬────────────────────────────────────┤
│                            │                                    │
│   Console Output           │   JSON/XML Payload                 │
│   ┌────────────────────┐   │   ┌────────────────────────────┐   │
│   │  (message list)    │   │   │  (textarea)                │   │
│   └────────────────────┘   │   └────────────────────────────┘   │
│                            │   Quick load: JSON: … XML: …       │
│   Command  [Multiline □]   │                                    │
│   [textarea]   [Send]      │                                    │
│   Enter to send · …        │                                    │
├────────────────────────────┤────────────────────────────────────┤
│       Left Panel           │         Right Panel                │
│      (resizable)    ◀ ▶    │         (resizable)                │
└────────────────────────────┴────────────────────────────────────┘
```

The two panels are **resizable** — drag the divider between them. The console opens at one third of the width and the right panel at two thirds, on every page load; a drag holds until the console is toggled or a form takes its place. On viewports narrower than 768 px the panels stack vertically.

---

## Connection Bar

Located in the header below the page title. Shows the current WebSocket state and controls.

| State | Indicator | Button |
|---|---|---|
| **Disconnected** | 🔴 red dot | **Start** — opens the connection |
| **Connecting** | 🟡 yellow dot (pulsing) | **Connecting…** — disabled while handshake is in progress |
| **Connected** | 🟢 green dot (pulsing) | **Stop Service** — closes the connection |

The WebSocket URL is displayed between the status label and the button, e.g. `ws://localhost:3000/ws/graph/playground`.

---

## Console Output (Left Panel — Top)

Displays all WebSocket traffic in chronological order. The console holds up to **200 messages**; the oldest is evicted when the limit is reached.

### Message types

| Icon | Type | Meaning |
|---|---|---|
| ℹ️ | `info` | Lifecycle events — connected, disconnected |
| 👋 | `welcome` | Server greeting on open |
| 📝 | `raw` | Plain-text response (not JSON-structured) |
| ❌ | `error` | Server or client-side error |

When a message body is valid JSON, it is rendered as a **collapsible tree** (via `react-json-view-lite`) with the first two levels expanded by default. Plain text falls back to a preformatted span.

### Toolbar buttons

| Button | Action |
|---|---|
| **Disable AutoScroll / Enable AutoScroll** | Toggles whether the console automatically scrolls to the newest message. Useful when reviewing older output while a connection is active. |
| **Copy Output** | Copies all visible messages to the clipboard (one raw message per line). |
| **Clear** | Removes all messages from the console (does not disconnect). |

---

## Command Input (Left Panel — Bottom)

Type commands here and send them to the connected backend service.

### Single-line mode (default)

- **Enter** — sends the command and clears the input; focus returns to the textarea.
- **Shift+Enter** — inserts a newline (expands the textarea) without sending.
- **↑ / ↓ Arrow keys** — navigate command history (last **50** commands, persisted across sessions).

### Multiline mode

Check the **Multiline** checkbox to switch to a 5-row textarea.

- **Ctrl+Enter** (or **⌘+Enter** on macOS) — sends.
- **Enter** — inserts a newline.
- **Shift+Enter** — also inserts a newline.
- **↑ / ↓ Arrow keys** — navigate history.

### Send button

The **Send** button is disabled when:
- Not connected, **or**
- The command input is empty / whitespace-only.

In single-line mode the Send button sits to the right of the textarea. In multiline mode it appears full-width below it.

### `load` command

Typing `load` and sending triggers a two-step sequence:
1. The literal string `load` is sent to the server.
2. The contents of the **Payload Editor** are sent as a second frame.

This populates the backend's working context with your JSON/XML document. The command is rejected client-side (with an error message in the console) if:
- The payload textarea is empty, or
- The payload exceeds **64 000 characters**.

---

## Payload Editor (Right Panel)

A persistent textarea for the JSON or XML document you want to work with.

### Validation indicators

Shown in the label row as you type:

| Indicator | Meaning |
|---|---|
| `JSON` badge (green) | Content is valid JSON |
| `XML` badge (green) | Content is valid XML |
| ✅ | Payload is valid |
| ❌ | Payload is invalid |
| `0 / 64000` counter | Character count versus the 64 000 limit |

An inline error message appears below the textarea when the content is malformed.

### Format button

Prettifies the payload with 2-space indentation. Only enabled for valid JSON (XML formatting is not supported).

### Quick load samples

Buttons below the textarea load pre-built sample documents instantly:

| Group | Samples |
|---|---|
| **JSON** | simple · nested · array |
| **XML** | simple · nested · array |

Clicking a sample replaces the textarea contents immediately.

> The payload is **always editable** — you can paste and edit before, during, or after a connection. It is not sent automatically; it is only used when the `load` command is issued.

---

## Navigation & Quick Links

The navigation bar in the header has two sections:

**Tools** — links to each configured playground. The active playground link is highlighted.

**Quick Links** — shortcuts to backend info endpoints (open in a new tab):

| Label | URL |
|---|---|
| INFO | `/info` |
| LIBRARIES | `/info/lib` |
| SERVICES | `/info/routes` |
| HEALTH | `/health` |
| ENVIRONMENT | `/env` |

---

## Keyboard Reference

| Key | Context | Action |
|---|---|---|
| **Enter** | Single-line command | Send command |
| **Shift+Enter** | Single-line command | New line (no send) |
| **Ctrl/⌘+Enter** | Multiline command | Send command |
| **Enter** | Multiline command | New line |
| **Ctrl + backtick** | Anywhere in a Help-enabled playground | Toggle the Help panel |
| **Ctrl + M** | Active Minigraph Graph tab | Toggle the graph minimap |
| **↑** | Command input | Recall previous command from history |
| **↓** | Command input | Move forward in history (↓ past index 0 clears the field) |

---

## Persistence

All data is stored in **`localStorage`** per playground — nothing is sent to the server on page load.

| Data | Storage key (example) | Limit |
|---|---|---|
| Payload textarea | `minigraph-last-payload` | — |
| Command history | `minigraph-command-history` | 50 entries |

Each playground has its own independent storage keys, so switching between playgrounds never overwrites the other's data.
The panel split is not stored: every page load opens at the default widths.

---

## Available Playgrounds

| Route | Title | WebSocket endpoint | Purpose |
|---|---|---|---|
| `/json-path` | JSON-Path Playground | `/ws/json/path` | Evaluate JSONPath expressions against a loaded document |
| `/minigraph` | Minigraph Playground | `/ws/graph/playground` | Create and query a Knowledge Graph |

### Minigraph quick start

1. Click **Start** to connect.
2. Type `help` in the command input and press **Enter** to list all available commands.
3. Paste a JSON document into the Payload Editor (or click a **Quick load** sample).
4. Type `load` and press **Enter** — the document is sent and stored in the `response` node.
5. Type `response` to retrieve the full loaded object.
6. Use dot-bracket notation for simple retrieval: `response.hello` returns `"world"` given `{ "hello": "world" }`.
7. Use JSONPath for richer queries: `$.response.hello` — see e.g. [SmartBear JSONPath docs](https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html).

While connected, `help {topic}` and `describe skill {route}` for a built-in skill (for example
`describe skill graph.math`) open that page in the Help panel, which renders its markdown;
`describe graph`, `describe node` and `describe connection` answer in the console.

After loading a graph, use the toolbar's separate **Instantiate** and **Run** actions. Run remains
disabled until the backend confirms the instance; graphs that reference `input.body` prompt for JSON
input before becoming ready. In collaborative sessions every member controls this lifecycle as an equal
partner: the backend runs a subscriber's Instantiate and Run in every member's session, and each member
uploads mock input for its own instance.

### JSON-Path quick start

1. Open the top-right **?** button (or press **Ctrl + backtick**) for a short JSON-Path Overview; this works before connecting.
2. Click **Start** to connect.
3. Paste any JSON document into the Payload Editor and send `load`.
4. Enter a JSONPath expression (starting with `$`) in the command input and press **Enter**.

The JSON-Path Help panel uses the same resizable, maximizable panel as Minigraph, but intentionally
exposes only its own **Overview** section. While connected, the bare `help` command opens that
Overview locally; unsupported Minigraph topics continue to the backend.

---

## Adding a New Playground

1. Open `src/config/playgrounds.ts`.
2. Add a new entry to the `PLAYGROUND_CONFIGS` array:

```ts
{
  path:              '/my-tool',
  label:             'My Tool',           // shown in the nav bar
  title:             'My Tool Playground', // shown as the page heading
  wsPath:            '/ws/my/endpoint',   // backend WebSocket path
  storageKeyPayload: 'mytool-last-payload',
  storageKeyHistory: 'mytool-command-history',
}
```

3. That's it — the route, navigation link, and storage keys are all generated automatically. No changes to `App.tsx` or `Navigation.tsx` are needed.

To add new **Quick load** samples, add entries to the `SAMPLE_DATA` object in the same file using the `json_<label>` or `xml_<label>` key convention.

---

## Tech Stack

| Library | Version | Role |
|---|---|---|
| React | 19 | UI framework (with the React Compiler, `babel-plugin-react-compiler`) |
| TypeScript | 5.9 | Type safety |
| Vite | 8 | Dev server + build (Rolldown) |
| react-router | 8 | Client-side routing |
| @xyflow/react | 12 | Graph canvas (React Flow) |
| react-resizable-panels | 4.6 | Draggable split-panel layout |
| react-json-view-lite | 2.5 | Collapsible JSON tree in console |
| react-markdown + remark-gfm | 10 / 4 | Markdown rendering in the Help panel |
| idb | 8 | IndexedDB wrapper for the workspace clipboard |
| Vitest + happy-dom + Testing Library | 4.1 / 20 / 16 | Unit and component tests |
