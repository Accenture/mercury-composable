# Feature Spec: Auto-Rerender of Graph on Mutation Commands

**Target:** `webapp/` (React/TypeScript frontend)
**Branch:** `feature/playground`
**Date:** 2026-03-12
**Status:** Revised v5 — updated to match implemented code

### Revision history

| Version | Date | Changes |
|---|---|---|
| v1 | 2026-03-11 | Initial spec |
| v2 | 2026-03-11 | Applied peer-review findings: B1 stale-closure fix (`pinnedGraphPathRef`); B2 stable `useCallback` constraint; B3 `sendRawText` new API + changelist entry; I1 `setPinnedGraphPath` + `waitingForDescribeRef` mechanism; I2 mount-time watermark init; I3 dep-array requirement in §8; I4 ARIA on loading overlay; I5 narrowed `removed` match to `' -> ' && 'removed'`; C1 `rightTab` read-only clarification; C2 E1 dep-array cross-reference; C3 `useWebSocket.ts` added to changelist |
| v3 | 2026-03-12 | Tightened node-mutation match rules (A1/B2): added `lower.startsWith('node ')` prefix guard for `created`, `updated`, `deleted`, `connected to`, `imported from`, `overwritten by node from` checks — eliminates false positives from `"Graph instance created…"` (`instantiate graph`) and `"Root node created because it does not exist"` (`export graph`). Updated §2.2.1 table, §3.1 code block + false-positive table, §10 edge cases E16–E17. Spec review fixes: updated §3.1 JSDoc to document `startsWithNode` compound condition explicitly; removed `aria-live="polite"` from §6.2 accessibility bullet (`role="status"` already implies it per ARIA 1.2); updated false-positive table row and E9 to note that `"Node foo already exists"` passes `startsWithNode` but still does not match due to absent `" created"`. |
| v4 | 2026-03-12 | Expanded goal: mutation commands now auto-generate and auto-pin the graph when `pinnedGraphPath === null`, rather than being a silent no-op. Removed `pinnedGraphPath !== null` precondition from §2.2. Removed guard from `import-graph` path (§2.2.2). Both `node-mutation` and `import-graph` now share the same `waitingForDescribeRef` / `describe graph` auto-send path when nothing is pinned. Updated §1 (goal + data flow), §2.2, §4.1 (responsibility table + data flow diagram), §5.1, §5.2, §5.3 (replaced silent no-op with auto-describe path), §5.4 (tab-switch table). Added edge cases E18–E22 to §10. |
| v5 | 2026-03-23 | **Post-implementation accuracy pass** — updated spec to match the code as shipped. Key changes: (1) §1/§4.1/§4.3/§5.1/§5.4 — `node-mutation` path **always** sends `describe graph` regardless of `pinnedGraphPath`; `refetchGraph` is accepted in the interface but intentionally unused (`_refetchGraph`). (2) §5.1 — tab **does** switch when a graph was already pinned, because `setPinnedGraphPath(newPath)` fires the initial-load path in `useGraphData` which calls `setRightTab('graph')`. (3) §7 — watermark is a separate mount-only effect (not an inline init); variable named `watermarkRef`; disconnect reset is its own `useEffect([connected])`; main effect dep array is `[messages, connected, sendRawText, setPinnedGraphPath, addToast]` (no `pinnedGraphPath`, no `refetchGraph`). (4) §8 — debounce fires `sendRawText('describe graph')` + sets `waitingForDescribeRef`, not `refetchGraph()`. (5) §3.1 — `detectMutation` match structure is early-return `if` chain, not compound OR; guard order is `isMarkdownCandidate` → `startsWith('> ')` → `isGraphLinkMessage`. (6) §6.2 — overlay CSS uses `rgba(26, 26, 46, 0.55)` background, spinner is bottom-right, keyframe is named `graphRefreshSpin`. (7) §6.1 — clarified that `refetchGraph` is exposed but not called by `useAutoGraphRefresh` in the current implementation. (8) §10 — updated E1 dep-array note and E8 to reflect the new all-describe-graph architecture. |

---

## Table of Contents

1. [Background & Motivation](#1-background--motivation)
2. [Scope: Triggering Commands](#2-scope-triggering-commands)
3. [Exact Message-Matching Rules](#3-exact-message-matching-rules)
4. [Architecture: Where the Logic Lives](#4-architecture-where-the-logic-lives)
5. [Detailed Behaviour Specification](#5-detailed-behaviour-specification)
6. [Loading State — `isRefreshing`](#6-loading-state--isrefreshing)
7. [Message ID Tracking — Avoiding Reprocessing](#7-message-id-tracking--avoiding-reprocessing)
8. [Debounce Implementation](#8-debounce-implementation)
9. [File Changelist](#9-file-changelist)
10. [Edge Cases & Pitfall Index](#10-edge-cases--pitfall-index)
11. [Open Questions (Deferred)](#11-open-questions-deferred)

---

## 1. Background & Motivation

The current flow requires the user to explicitly **pin** a graph-link message in the Console to load the Graph tab. Once a graph is loaded, subsequent commands that mutate the graph (`create`, `update`, `delete`, `connect`, `import`, etc.) do not refresh the view — the Graph tab silently shows stale data until the user manually re-runs `describe graph` or re-pins a message.

The goal of this feature is:

> **Any command that mutates the in-memory graph on the server should automatically re-fetch and re-render the graph currently displayed, without requiring any manual action from the user. If no graph is currently pinned, the graph should be automatically generated and pinned before rendering.**

### Current data flow (unchanged)

```
User pins graph-link message
  → pinnedGraphPath set in Playground.tsx
  → useGraphData fetches /api/graph/model/{id}/{n}
  → graphData populated
  → RightPanel auto-switches to Graph tab
```

### New data flow — mutation with graph already pinned (this feature)

```
User sends a mutation command (pinnedGraphPath !== null)
  → server responds with a plain-text success message (no graph-link)
  → useAutoGraphRefresh detects the mutation in the incoming message stream
  → 300 ms debounce fires → sendRawText('describe graph')  ← NEW
  → sets waitingForDescribeRef = true                       ← NEW
  → server responds with a new graph-link message
  → hook detects graph-link (waitingForDescribeRef === true)
  → calls setPinnedGraphPath(newPath)                       ← NEW
  → useGraphData initial-load path fires (path: old → new)
  → graphData null'd briefly then re-populated              ← NEW
  → setRightTab('graph') fires — tab switches to Graph      ← NEW
```

> **v5 note — `refetchGraph` / overlay not used for `node-mutation`:**
> Earlier spec revisions described this path as calling `refetchGraph()` in overlay
> mode (no tab switch). The implementation (line 83 of `useAutoGraphRefresh.ts`)
> instead receives `refetchGraph` as `_refetchGraph` — intentionally unused — and
> always sends `describe graph` for both pinned and un-pinned cases. The overlay
> (`isRefreshing`) is therefore only set when `useGraphData`'s own `refetchGraph`
> is called externally, which does not currently happen from any hook or component.
> See §6.1 for the preserved `refetchGraph` API.

### New data flow — mutation with NO graph pinned yet (this feature)

```
User sends a mutation command (pinnedGraphPath === null)
  → server responds with a plain-text success message (no graph-link)
  → useAutoGraphRefresh detects the mutation (node-mutation OR import-graph)
  → hook auto-sends "describe graph" over the WebSocket  ← NEW
  → sets waitingForDescribeRef = true  ← NEW
  → server responds with a graph-link message
  → hook detects the graph-link (waitingForDescribeRef === true)
  → calls setPinnedGraphPath(extractedPath)  ← NEW
  → useGraphData initial-load path fires (pinnedGraphPath: null → value)
  → graphData populated, RightPanel auto-switches to Graph tab  ← NEW
```

---

## 2. Scope: Triggering Commands

### 2.1 Graph-link commands (already work — no change needed)

These commands already emit a `/api/graph/model/{id}/{n}` path in their WebSocket response. The existing pin-to-render flow continues unchanged.

| Command | Server WS response |
|---|---|
| `describe graph` | `"Graph with N nodes described in /api/graph/model/{id}/{n}"` |
| `export graph as {name}` | `"Graph exported to …\nDescribed in /api/graph/model/{id}/{n}"` |

The `{n}` suffix is a server-side cache-buster (`getRandomCounter()` in `GraphCommandService.java`). The REST route is `/api/graph/model/{graph_id}/{sequence}` — `DescribeGraph.java` uses only `{graph_id}` and ignores `{sequence}`.

### 2.2 Mutation commands — auto-refresh triggers (NEW)

> **No precondition on `pinnedGraphPath`.** Mutation detection fires regardless of whether a graph is already pinned.
> - If `pinnedGraphPath !== null` — 300 ms debounce fires `sendRawText('describe graph')`; the returned graph-link replaces `pinnedGraphPath`; `useGraphData` initial-load re-renders the graph and switches the tab to Graph.
> - If `pinnedGraphPath === null` — same flow; `useGraphData` initial-load auto-switches the tab to Graph.
>
> Both paths go through the same `waitingForDescribeRef` / `describe graph` auto-send mechanism (see §5.1–§5.2). The `import-graph` path uses the same mechanism without debouncing (see §5.3).

#### 2.2.1 Node & connection structural mutations

These commands produce plain-text success responses with no graph-link.

| Command | Server success response | Match rule |
|---|---|---|
| `create node {name}` | `"Node {name} created"` | starts with `"Node "` **AND** contains `" created"` |
| `update node {name}` | `"Node {name} updated"` | starts with `"Node "` **AND** contains `" updated"` |
| `delete node {name}` | `"Node {name} deleted"` | starts with `"Node "` **AND** contains `" deleted"` |
| `connect {A} to {B} with {rel}` | `"Node {A} connected to {B}"` | starts with `"Node "` **AND** contains `" connected to "` |
| `delete connection {A} and {B}` | `"{A} -> {B} removed"` (one or two lines) | contains `" -> "` **AND** contains `"removed"` |
| `import node {name} from {file}` | `"Node {name} imported from {file}"` or `"Node {name} overwritten by node from {file}"` | starts with `"Node "` **AND** contains `" imported from "` or `" overwritten by node from "` |

> **Why the `"Node "` prefix is required for create / update / delete / connect / import-node:**
> The server uses the Java constant `NODE_NAME = "Node "` as the prefix for all node operation
> success messages. Two other server responses contain the words `"created"` without this prefix
> and must not trigger a refresh:
> - `"Graph instance created. Loaded N mock entries…"` — emitted by `instantiate graph`; creates a
>   runtime instance, not a structural graph-model change.
> - `"Root node created because it does not exist"` — emitted by `export graph as {name}` when the
>   root node is missing; the `export` command already emits a graph-link message that the existing
>   pin flow handles, so a second refresh is unnecessary and would be redundant.
>
> Requiring `lower.startsWith('node ')` eliminates both false positives with a single rule that
> precisely mirrors the server's own naming convention.

#### 2.2.2 `import graph from {name}` — special case

> **Decision:** After detecting `"Graph model imported as draft"`, the frontend automatically sends a `describe graph` WebSocket command regardless of whether a graph was previously pinned. The server responds with a fresh graph-link message, which the hook consumes via the `waitingForDescribeRef` mechanism to call `setPinnedGraphPath` directly.

**Why not just re-fetch the old `pinnedGraphPath` directly?**
`import graph` replaces the in-memory graph with a file whose `root.name` property may differ from the currently pinned filename. A direct re-fetch of the old path would show a graph that no longer matches what is in memory. `describe graph` always serialises the *current* in-memory graph and returns the canonical correct path. This also means `import graph` behaves correctly when `pinnedGraphPath` is `null` — there is no previous path to re-fetch, and `describe graph` gives us the fresh one.

**Note on server messages:** When the target file is not found in the temp directory but exists as a deployed graph, the server sends two messages before `"Graph model imported as draft"`:
1. `"Graph model not found in {path}"` — does **not** match `detectMutation` → ignored
2. `"Found deployed graph model in {location}\nPlease export an updated version…"` — does **not** match `detectMutation` → ignored
3. `"Graph model imported as draft"` — matches `'import-graph'` → triggers the auto-describe flow

The auto-describe is triggered only on step 3, so the extra informational messages do not cause a spurious second `describe graph` send.

### 2.3 Read commands — no action

| Command | Reason |
|---|---|
| `describe node {name}` | Read-only; returns JSON blob |
| `describe connection {A} and {B}` | Read-only |
| `describe skill {route}` | Read-only |
| `list nodes` / `list connections` | Read-only |
| `inspect {key}` | State-machine read; no graph structure change |
| `edit node {name}` | Read-only; prints raw node text for user to resubmit as `update` |
| `execute node {name}` | Executes a skill; does not mutate graph structure |
| `run` | Executes the full graph instance; no structural change |
| `instantiate graph` | Creates a runtime instance; does not change the model graph |
| `delete cache` | Clears state-machine cache only; no graph model change |

---

## 3. Exact Message-Matching Rules

All matching is applied to the **raw** WebSocket message string (`msg.raw`). Rules:

- Matching is **case-insensitive** on the key substring
- Applied **only to non-JSON messages** (i.e., `isMarkdownCandidate(raw) === true`)
- **Never** triggered by the user's own echoed command (lines starting with `> `)
- **Never** triggered by graph-link messages (already handled by the pin flow)

### 3.1 New utility function: `detectMutation`

**File:** `src/utils/messageParser.ts` (lines 243–289)

```ts
export type MutationKind = 'node-mutation' | 'import-graph';

/**
 * Inspects a single raw WebSocket message string and returns whether it
 * represents a graph mutation that should trigger an auto-refresh.
 *
 * Returns:
 *  - `'node-mutation'`  when the message is a plain-text success response for
 *    a structural node or connection change (create / update / delete / connect /
 *    import-node / delete-connection).
 *  - `'import-graph'`   when the message is `"Graph model imported as draft"`.
 *  - `null`             for all other messages (read-only responses, JSON
 *    lifecycle events, echoed commands, graph-link messages, etc.).
 *
 * Matching rules:
 *  - Only non-JSON messages pass (`isMarkdownCandidate` guard).
 *  - Echoed commands (`raw.startsWith('> ')`) are excluded.
 *  - Graph-link messages (`isGraphLinkMessage`) are excluded.
 *  - Node-operation matches require `lower.startsWith('node ')` (mirrors the
 *    server's `NODE_NAME = "Node "` prefix) to avoid false positives from
 *    `"Graph instance created…"` and `"Root node created because…"`.
 *  - Connection-delete match requires both `" -> "` and `"removed"`.
 */
export function detectMutation(raw: string): MutationKind | null {
  if (!isMarkdownCandidate(raw)) return null;   // JSON messages → not a mutation
  if (raw.startsWith('> ')) return null;         // echoed user command → ignore
  if (isGraphLinkMessage(raw)) return null;      // graph-link messages handled by pin flow

  const lower = raw.toLowerCase();

  // ── import-graph ─────────────────────────────────────────────────────────
  if (lower.includes('graph model imported as draft')) return 'import-graph';

  // ── node-mutation ─────────────────────────────────────────────────────────
  // Connection delete: "{A} -> {B} removed" (one or two lines)
  if (lower.includes(' -> ') && lower.includes('removed')) return 'node-mutation';

  // Node structural ops — require "Node " prefix to avoid false positives
  const startsWithNode = lower.startsWith('node ');
  if (startsWithNode) {
    if (lower.includes(' created'))            return 'node-mutation';
    if (lower.includes(' updated'))            return 'node-mutation';
    if (lower.includes(' deleted'))            return 'node-mutation';
    if (lower.includes(' connected to '))      return 'node-mutation';
    if (lower.includes(' imported from '))     return 'node-mutation';
    if (lower.includes(' overwritten by node from ')) return 'node-mutation';
  }

  return null;
}
```

> **Guard order matters:** `isMarkdownCandidate` runs first (cheapest, eliminates all JSON), then `startsWith('> ')` (string prefix), then `isGraphLinkMessage` (requires a regex match). This order minimises work for the common case.

> **Early-return `if` chain vs. compound OR:** The `startsWithNode` block uses individual `if` returns rather than a single compound `||` expression. This makes each branch independently readable and means `lower.startsWith('node ')` is only tested once — it gates the entire block via the outer `if (startsWithNode)`.

> **Why `' -> ' && 'removed'` instead of `' removed'` alone?**
> The `delete connection` server response has a unique structure: `"{A} -> {B} removed\n"`.
> Using both substrings together makes the match structurally specific — no other current
> or plausible future server message combines an arrow (`->`) with the word `removed`.
> In contrast, bare `' removed'` would match any hypothetical message such as
> `"Session removed"` or `"Cache entry removed"`, producing a spurious graph refresh.

#### False-positive analysis

| Potential false positive | Why it is safe |
|---|---|
| `"Node foo already exists"` | Starts with `"Node "` but does not contain `" created"` → not matched |
| `"Node foo not found"` | Does not contain any matched substring |
| `"Graph model not found in …"` | Does not contain `"graph model imported as draft"` |
| `"Did you forget to add …?"` | No matched substring |
| `"cache cleared"` (`delete cache` response) | Does not contain `" -> "` + `"removed"` — the narrowed connection-delete match is not triggered |
| `"> create node foo"` | Filtered by `raw.startsWith('>')` |
| Any graph-link message | Filtered by `isGraphLinkMessage(raw)` |
| Keep-alive ping/pong | Filtered by `isMarkdownCandidate` (they are JSON) |
| Any future `"… removed"` message without `" -> "` | Narrowed match requires both `" -> "` and `"removed"` — a bare `"removed"` does not match |
| `"Graph instance created. Loaded N mock entries…"` (`instantiate graph` response) | Does not start with `"Node "` → `startsWithNode` is false → `" created"` branch is never reached |
| `"Root node created because it does not exist"` (`export graph` when root is missing) | Does not start with `"Node "` → `startsWithNode` is false → `" created"` branch is never reached; the `export` command already emits a graph-link that the pin flow handles |

---

## 4. Architecture: Where the Logic Lives

### 4.1 Responsibility split

| Concern | Owner |
|---|---|
| Detect mutation kind from raw message | `detectMutation()` in `messageParser.ts` |
| Abort-controller-aware re-fetch | `useGraphData.ts` — `refetchGraph()` method (defined; not currently called by `useAutoGraphRefresh`) |
| `isRefreshing` state | `useGraphData.ts` — `isRefreshing: boolean` (set by `refetchGraph()`; currently stays `false` as `refetchGraph` is not invoked by the auto-refresh path) |
| `pinnedGraphPath` ref sync (stale-closure fix) | `useGraphData.ts` — `pinnedGraphPathRef`; also mirrored in `useAutoGraphRefresh.ts` — `pinnedGraphPathRef` |
| Debounce + message-ID (watermark) tracking | `useAutoGraphRefresh.ts` — `debounceTimerRef` / `watermarkRef` |
| `node-mutation` (any `pinnedGraphPath` value) → `describe graph` | `useAutoGraphRefresh.ts` — debounce fires `sendRawText('describe graph')`, sets `waitingForDescribeRef = true` |
| `import-graph` → `describe graph` command (always) | `useAutoGraphRefresh.ts` — sends `describe graph` immediately (no debounce), sets `waitingForDescribeRef = true` |
| Detecting `describe graph` response, calling `setPinnedGraphPath` | `useAutoGraphRefresh.ts` — `waitingForDescribeRef` flag; Pass 1 of the main effect |
| Sending arbitrary WS text | `useWebSocket.ts` — `sendRawText` method |
| Tab-switch after any auto-describe completes | Natural — `useGraphData` initial-load path calls `setRightTab('graph')` whenever `pinnedGraphPath` changes to a non-null value |
| Loading overlay in graph view | `GraphView.tsx` + `GraphView.module.css` — `isRefreshing` prop; overlay currently inactive (see `refetchGraph` row above) |
| Wiring everything together | `Playground.tsx` |

### 4.2 New hook: `useAutoGraphRefresh`

**File:** `src/hooks/useAutoGraphRefresh.ts`

```ts
export interface UseAutoGraphRefreshOptions {
  /** Full incoming message log from useWebSocket. */
  messages:           { id: number; raw: string }[];
  /** Currently pinned graph REST path, or null if nothing is pinned. */
  pinnedGraphPath:    string | null;
  /**
   * Setter for pinnedGraphPath — called when the auto-describe path
   * discovers a new graph URL via waitingForDescribeRef.
   */
  setPinnedGraphPath: (path: string | null) => void;
  /**
   * Whether the WebSocket is currently connected.
   * sendRawText is a no-op when disconnected; the guard here prevents
   * setting waitingForDescribeRef = true while disconnected (which would
   * cause the next connection's first graph-link message to be consumed).
   * Also used in a dedicated useEffect to reset waitingForDescribeRef on
   * disconnect (see E21).
   */
  connected:          boolean;
  /**
   * Imperatively re-fetch the currently pinned graph (overlay mode).
   * Accepted in the interface and kept for future use, but currently
   * aliased as `_refetchGraph` inside the hook body and NOT called.
   * All node-mutation refreshes go through the describe-graph path.
   * Stable reference from useGraphData (empty dep array).
   */
  refetchGraph:       () => void;
  /**
   * Send a raw string over the WebSocket without echoing it to the console.
   * From useWebSocket.sendRawText.
   */
  sendRawText:        (text: string) => void;
  /**
   * The currently active right panel tab.
   * Reserved for a future badge-pulse animation (spec §11).
   * Aliased as `_rightTab` in the hook body — not read.
   */
  rightTab:           RightTab;
  addToast:           (msg: string, type?: ToastType) => void;
}
```

This hook has **no return value** — its only job is to dispatch side effects.

### 4.3 Data flow diagram

```
WebSocketContext (messages array)
        │
        ▼
useWebSocket → ws.messages
        │
        ▼
useAutoGraphRefresh
  ├── detectMutation(raw)
  │       ├── 'node-mutation' (pinnedGraphPath === null OR !== null — no distinction)
  │       │       └── debounce(300ms) → sendRawText('describe graph')
  │       │                             sets waitingForDescribeRef = true
  │       │
  │       └── 'import-graph' (always, regardless of pinnedGraphPath)
  │               └── sendRawText('describe graph')  [no debounce]
  │                   sets waitingForDescribeRef = true
  │
  │       [both paths above converge here when server replies — Pass 1]
  │               server responds with graph-link message
  │               isGraphLinkMessage(raw) && waitingForDescribeRef === true
  │                 → setPinnedGraphPath(extractGraphApiPath(raw))
  │                 → waitingForDescribeRef = false
  │                 → pinnedGraphPath: null → new path
  │                     initial-load fires: graphData populated, setRightTab('graph')
  │                 → pinnedGraphPath: old path → new path
  │                     path-change re-fetch fires: graphData null'd then re-populated
  │                     setRightTab('graph') fires — tab always switches
  │
  └── (no matched mutation kind) → no-op
        │
        ▼
useGraphData
  ├── pinnedGraphPathRef (kept in sync via dedicated useEffect)
  ├── refetchAbortRef (AbortController for imperative refetch)
  ├── isRefreshing: boolean  (only set by refetchGraph(); currently stays false)
  ├── refetchGraph(): void   (stable useCallback, empty dep array — not called by
  │                            useAutoGraphRefresh in the current implementation)
  └── graphData: MinigraphGraphData | null
        │
        ▼
Playground.tsx
  └── RightPanel (isGraphRefreshing={isRefreshing}) → GraphView (isRefreshing prop)
```

---

## 5. Detailed Behaviour Specification

### 5.1 `node-mutation` path — graph already pinned

> **v5 note:** This path no longer calls `refetchGraph()` or shows an overlay.
> `refetchGraph` is received as `_refetchGraph` (line 83 of `useAutoGraphRefresh.ts`)
> and is intentionally not called. Both the "already pinned" and "not pinned" cases
> go through the same `describe graph` send → `waitingForDescribeRef` → `setPinnedGraphPath`
> pipeline, which means the tab **does** switch to Graph via the `useGraphData`
> initial-load path.

```
Incoming message detected as 'node-mutation'
  AND pinnedGraphPath !== null
    → start/reset 300 ms debounce timer (debounceTimerRef)
    → on debounce fire (if still connected):
        → set waitingForDescribeRef = true
        → sendRawText('describe graph')
        → addToast('Graph updated — refreshing…', 'info')

Pass 1 of the next main-effect execution:
  → new message arrives that passes isGraphLinkMessage()
    AND waitingForDescribeRef === true
      → path = extractGraphApiPath(msg.raw)
      → waitingForDescribeRef = false
      → call setPinnedGraphPath(path)        ← updates Playground.tsx state
      → return (batch processing stops)
      → useGraphData initial-load path fires (old pinnedGraphPath → new path):
          → graphData null'd while fetch in-flight
          → fetch(path) succeeds → setGraphData(newData)
          → setRightTab('graph')             ← tab switches to Graph
```

**Debounce rationale:** 300 ms collapses rapid-fire commands (e.g. pasting history + Enter multiple times) into a single `describe graph` send, preventing N redundant round-trips.

**Why `setRightTab('graph')` fires even when a graph was already pinned:** `setPinnedGraphPath` is called with the *new* path returned by `describe graph` (which may differ from the old one due to the server-side cache-buster). This triggers `useGraphData`'s initial-load `useEffect` (which runs whenever `pinnedGraphPath` changes), and that effect calls `setRightTab('graph')` on success — unconditionally.

### 5.2 `node-mutation` path — no graph pinned yet

```
Incoming message detected as 'node-mutation'
  AND pinnedGraphPath === null
    → start/reset 300 ms debounce timer (debounceTimerRef)
    → on debounce fire (if still connected):
        → set waitingForDescribeRef = true
        → sendRawText('describe graph')
        → addToast('Graph updated — opening Graph tab…', 'info')

Pass 1 of the next main-effect execution:
  → new message arrives that passes isGraphLinkMessage()
    AND waitingForDescribeRef === true
      → path = extractGraphApiPath(msg.raw)
      → waitingForDescribeRef = false
      → call setPinnedGraphPath(path)        ← updates Playground.tsx state
      → return (batch processing stops)
      → useGraphData initial-load path fires (pinnedGraphPath: null → value):
          → graphData null'd while fetch in-flight
          → fetch(path) succeeds → setGraphData(newData)
          → setRightTab('graph')             ← tab switches to Graph
```

**Toast differentiation (lines 228-233 of `useAutoGraphRefresh.ts`):** The debounce callback reads `pinnedGraphPathRef.current` at fire time to choose the toast message:
- `pinnedGraphPath !== null` → `'Graph updated — refreshing…'`
- `pinnedGraphPath === null` → `'Graph updated — opening Graph tab…'`

**Why auto-describe rather than a direct fetch when `pinnedGraphPath === null`?** There is no path to fetch — `pinnedGraphPath` is `null`. `describe graph` both serialises the current in-memory graph and returns the canonical path. This is the only correct mechanism.

### 5.3 `import-graph` path

```
Incoming message = 'Graph model imported as draft'
  (pinnedGraphPath may be null OR non-null — no guard)
    → cancel any pending node-mutation debounce   ← import supersedes it
    → set waitingForDescribeRef = true
    → sendRawText('describe graph')               ← no debounce
    → addToast('Graph imported — refreshing view…', 'info')
    → return (no further processing for this batch)

Pass 1 of the next main-effect execution:
  → new message arrives that passes isGraphLinkMessage()
    AND waitingForDescribeRef === true
      → path = extractGraphApiPath(msg.raw)
      → waitingForDescribeRef = false
      → call setPinnedGraphPath(path)        ← updates Playground.tsx state
      → return (batch processing stops)
      → if pinnedGraphPath was null:
          → useGraphData initial-load path fires
          → graphData populated, setRightTab('graph')  ← tab switches to Graph
      → if pinnedGraphPath was already set (possibly a different path):
          → useGraphData path-change effect fires (old path → new path)
          → graphData null'd then re-populated with new graph
          → setRightTab('graph')  ← tab switches to Graph
```

**Why `import-graph` is not debounced:** It is processed immediately (line 189-199 of `useAutoGraphRefresh.ts`), cancelling any pending node-mutation debounce timer first. An import is a heavyweight intentional operation — the user always wants to see the result immediately. Debouncing it would add an unnecessary 300 ms delay.

**Why `waitingForDescribeRef` is needed (I1):** The existing pin flow is user-driven — it fires when the user *clicks* a console row, calling `handlePinMessage` in `Playground.tsx`. An automatically sent `describe graph` produces a new graph-link that no one clicks. The hook must therefore detect this response itself and call `setPinnedGraphPath` directly. The `waitingForDescribeRef` flag scopes this detection to only the graph-link that arrives *after* the hook sent `describe graph`.

**Loop prevention:** `detectMutation` returns `null` for all graph-link messages (`isGraphLinkMessage` guard at line 264 of `messageParser.ts`). The graph-link response from `describe graph` is consumed by the `waitingForDescribeRef` branch (Pass 1), not the mutation-detection branch (Pass 2) — so there is no loop.

### 5.4 Tab-switching rule

| Scenario | Behaviour |
|---|---|
| `node-mutation`, graph **already pinned** — auto-describe completes | **Switches to Graph tab** — `setPinnedGraphPath(newPath)` triggers initial-load path in `useGraphData` which calls `setRightTab('graph')` |
| `node-mutation`, **no graph pinned** — first auto-pin | **Switches to Graph tab** — same initial-load path fires |
| `import-graph` (any state) — auto-describe completes | **Switches to Graph tab** — same initial-load path fires |
| User is on Graph Data (Raw) tab during any auto-refresh | **Switches to Graph tab** (all mutation paths now go through `setPinnedGraphPath`) |
| **Initial manual pin** (existing behaviour) | **Always** switches to Graph tab ← unchanged |

---

## 6. Loading State — `isRefreshing`

### 6.1 Changes to `useGraphData`

The hook's return type is extended with two new members (lines 7-22 of `useGraphData.ts`):

```ts
export interface UseGraphDataReturn {
  graphData:    MinigraphGraphData | null;
  setGraphData: React.Dispatch<React.SetStateAction<MinigraphGraphData | null>>;
  rightTab:     RightTab;
  setRightTab:  React.Dispatch<React.SetStateAction<RightTab>>;
  /** True while an auto-refresh re-fetch is in-flight (NOT set during initial load). */
  isRefreshing: boolean;
  /**
   * Imperatively trigger a re-fetch of the currently pinned graph path.
   * - Does NOT null graphData — stale graph remains visible under the overlay.
   * - Does NOT switch the right tab.
   * - Sets isRefreshing = true while the fetch is in-flight.
   * - Stable reference (empty dep array) — safe to include in useEffect dep arrays.
   */
  refetchGraph: () => void;
}
```

**`refetchGraph` is defined but not currently called by `useAutoGraphRefresh`.** The hook accepts `refetchGraph` in its options interface (and Playground.tsx passes it) but aliases it as `_refetchGraph` (line 83 of `useAutoGraphRefresh.ts`). It is preserved in the interface for future use — for example, a hypothetical "silent background refresh" that does not switch the tab. `isRefreshing` therefore stays `false` during normal mutation flows; the overlay infrastructure is present and correct, ready to be activated.

**Key design decision:** During a `refetchGraph()` call, `graphData` is **NOT nulled out**. The stale graph remains visible under the loading overlay. Nulling would cause a jarring disappear/reappear cycle. Only the initial load (when `pinnedGraphPath` first becomes non-null) nulls the data, as it does today.

The `refetchGraph()` method (lines 100-127 of `useGraphData.ts`):
- Uses its own `refetchAbortRef` — a separate `useRef<AbortController | null>` that is distinct from the initial-load `controller`
- Sets `isRefreshing = true` immediately on call
- Cancels the previous in-flight refetch via `refetchAbortRef.current?.abort()` before creating a new `AbortController`
- Does **not** null `graphData` (unlike the initial path-change flow)
- On `AbortError`, returns immediately — does **not** call `setIsRefreshing(false)` because a newer request is already in-flight and owns the flag
- A separate unmount cleanup effect (lines 130-133) calls `refetchAbortRef.current?.abort()` so any in-flight refetch is cancelled if the component unmounts mid-request
- **Stable `useCallback` with an empty dependency array** — reads `pinnedGraphPath` via `pinnedGraphPathRef` (never via closure)

**Stale-closure fix (B1):** `useGraphData` keeps a `useRef` in sync with the prop (lines 61-65):

```ts
// useGraphData.ts lines 61–65
const pinnedGraphPathRef = useRef<string | null>(pinnedGraphPath);
useEffect(() => {
  pinnedGraphPathRef.current = pinnedGraphPath;
}, [pinnedGraphPath]);
```

`refetchGraph` reads through this ref, never through a closure. Similarly, `useAutoGraphRefresh` maintains its own `pinnedGraphPathRef` (lines 104-108) for reading `pinnedGraphPath` inside the debounce callback.

### 6.2 GraphView loading overlay

A non-blocking overlay is rendered in `GraphView` when `isRefreshing === true` (currently inactive — see §6.1). Implementation in `GraphView.tsx` (lines 105-116) and `GraphView.module.css`:

- **Position:** `.refreshingOverlay` — `position: absolute; inset: 0` anchored to `.graphWrapper` which is `position: relative`
- **Background:** `rgba(26, 26, 46, 0.55)` — matches the graph's dark theme palette
- **Spinner placement:** `align-items: flex-end; justify-content: flex-end; padding: 0.75rem` — small spinner in the **bottom-right** corner
- **Passthrough:** `pointer-events: none` — pan, zoom, and node interactions pass through to ReactFlow underneath
- **Spinner:** `.refreshingSpinner` — `1.25rem × 1.25rem`, `border: 2px solid`, `border-top-color: rgba(255,255,255,0.85)`, `animation: graphRefreshSpin 0.75s linear infinite`
- **Accessibility:** `role="status"` + `aria-label="Graph refreshing"` on the spinner element. `aria-busy={isRefreshing}` on the `<div className={styles.graphWrapper}>` wrapper (line 105) so assistive technology understands the content region is updating. `role="status"` already implies `aria-live="polite"` per ARIA 1.2 — `aria-live` is **not** added explicitly.

**Prop chain as implemented:** `Playground.tsx` line 365: `isGraphRefreshing={isRefreshing}` → `RightPanel.tsx` prop `isGraphRefreshing?: boolean` → `GraphView.tsx` prop `isRefreshing?: boolean` (default `false`).

```ts
// RightPanel.tsx — interface (line 27)
isGraphRefreshing?: boolean;

// GraphView.tsx — interface (line 27)
isRefreshing?: boolean;
```

---

## 7. Message ID Tracking — Avoiding Reprocessing

`useAutoGraphRefresh` must process **only messages it has not yet seen**. This is critical because:

- The `messages` array from `WebSocketContext` is rebuilt on every new message (the reducer returns a new array reference), causing the `useEffect` to re-run.
- React Strict Mode double-invokes effects in development.
- Without tracking, the same mutation message would be processed on every subsequent re-render.

**Implementation (as shipped):**

The watermark is a separate `useRef` initialised to `-1`, then set to the current high-water mark in a **dedicated mount-only `useEffect`** (lines 92, 134-142 of `useAutoGraphRefresh.ts`):

```ts
// useAutoGraphRefresh.ts lines 92, 96
const watermarkRef     = useRef<number>(-1);
const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

// ── Set watermark at mount (lines 134–142) ───────────────────────────────
// Runs once on mount. Captures the highest message ID currently in the log
// so that only genuinely new messages (posted after mount) are processed.
useEffect(() => {
  if (messages.length > 0) {
    watermarkRef.current = messages[messages.length - 1].id;
  }
}, []); // eslint-disable-line react-hooks/exhaustive-deps
// Intentionally empty dep array — we only want the mount snapshot.
```

The disconnect reset lives in its **own dedicated `useEffect`** watching `[connected]` (lines 120-132), **not** inside the main messages effect:

```ts
// useAutoGraphRefresh.ts lines 120–132
useEffect(() => {
  if (!connected) {
    if (waitingForDescribeRef.current) {
      waitingForDescribeRef.current = false;
    }
    // Also cancel any in-flight debounce — a mutation debounce that fires
    // after disconnection would set waitingForDescribeRef = true and call
    // sendRawText, which is a no-op when disconnected but leaves the flag
    // stranded true for the next reconnect.
    if (debounceTimerRef.current !== null) {
      clearTimeout(debounceTimerRef.current);
      debounceTimerRef.current = null;
    }
  }
}, [connected]);
```

The main effect (lines 144-248) filters new messages by watermark, then advances it **before** any async work:

```ts
// useAutoGraphRefresh.ts lines 144–248 (abridged)
useEffect(() => {
  if (messages.length === 0) return;

  const newMessages = messages.filter(m => m.id > watermarkRef.current);
  if (newMessages.length === 0) return;

  // Advance watermark to the latest processed ID.
  watermarkRef.current = messages[messages.length - 1].id;

  // ── Pass 1: consume a pending describe-graph response ─────────────────
  if (waitingForDescribeRef.current) {
    for (const msg of newMessages) {
      if (isGraphLinkMessage(msg.raw)) {
        const path = extractGraphApiPath(msg.raw);
        if (path) {
          waitingForDescribeRef.current = false;
          setPinnedGraphPath(path);
          return;
        }
      }
    }
  }

  // ── Pass 2: detect mutation commands ──────────────────────────────────
  let hasMutation = false;
  let hasImportGraph = false;

  for (const msg of newMessages) {
    const kind = detectMutation(msg.raw);
    if (kind === 'import-graph')  { hasImportGraph = true; }
    else if (kind === 'node-mutation') { hasMutation = true; }
  }
  // ... handle hasImportGraph, then hasMutation
}, [messages, connected, sendRawText, setPinnedGraphPath, addToast]);
```

**Actual dep array** (line 248): `[messages, connected, sendRawText, setPinnedGraphPath, addToast]`

Note: `pinnedGraphPath` and `refetchGraph` are **absent** from the dep array. `pinnedGraphPath` is read via `pinnedGraphPathRef` (not via closure), and `refetchGraph` is not called from the main effect.

**Why not initialise `watermarkRef` to `messages[last].id` inline?** When `Playground.tsx` remounts (e.g. user navigates to another route and back), `useAutoGraphRefresh` remounts with a fresh `useRef` initialised to `-1`. However, `WebSocketContext` is mounted above `<Routes>` and survives navigation — its `messages` array may already contain N messages. The separate mount-only `useEffect` correctly captures the high-water mark after React has committed the component, skipping all historical messages.



## 8. Debounce Implementation

`debounceTimerRef` is a `useRef<ReturnType<typeof setTimeout> | null>` (not `useState`) to avoid triggering extra renders (line 96 of `useAutoGraphRefresh.ts`):

```ts
// useAutoGraphRefresh.ts lines 203–248 (node-mutation debounce block, abridged)
if (hasMutation) {
  // Cancel any already-pending debounce so rapid commands collapse to one.
  if (debounceTimerRef.current !== null) {
    clearTimeout(debounceTimerRef.current);
  }

  debounceTimerRef.current = setTimeout(() => {
    debounceTimerRef.current = null;

    if (!connected) return;  // guard: don't send if disconnected during the 300 ms window

    // Always send `describe graph` — regardless of whether a graph is already pinned.
    waitingForDescribeRef.current = true;
    sendRawText('describe graph');

    const currentPath = pinnedGraphPathRef.current;
    addToast(
      currentPath !== null
        ? 'Graph updated — refreshing…'
        : 'Graph updated — opening Graph tab…',
      'info',
    );
  }, 300);
}

// Effect cleanup (fires on every re-run and on unmount):
return () => {
  if (debounceTimerRef.current !== null) {
    clearTimeout(debounceTimerRef.current);
    debounceTimerRef.current = null;
  }
};
```

**`pinnedGraphPath` is NOT in the dep array.** Unlike earlier spec revisions, the debounce callback no longer calls `refetchGraph()`, so there is no need to re-run the effect when `pinnedGraphPath` changes. The path is read at fire-time via `pinnedGraphPathRef.current` (for the toast message only — the actual `describe graph` send is path-agnostic). The effect's dep array is `[messages, connected, sendRawText, setPinnedGraphPath, addToast]`.

**Effect cleanup cancels the debounce** on every re-run and on unmount. Because `messages` is in the dep array and changes on every new WS message, the effect is re-entrant — each new batch of messages re-runs the effect, running cleanup first. If a debounce timer from a previous run is still pending (rapid-fire commands), it is cancelled and a fresh timer is started, collapsing all commands within the 300 ms window into a single `describe graph` send.

**`import-graph` is not debounced** (lines 189-200). It immediately cancels any pending node-mutation debounce, then fires `sendRawText('describe graph')` synchronously. The `return` statement after the `hasImportGraph` block ensures the node-mutation block (and its debounce) is never reached for the same batch.

---

## 9. File Changelist

| File | Type | Summary of change |
|---|---|---|
| `src/utils/messageParser.ts` | Modified | Added `MutationKind` type and `detectMutation()` function (lines 243-289). Early-return `if` chain structure; guard order: `isMarkdownCandidate` → `startsWith('> ')` → `isGraphLinkMessage`. |
| `src/hooks/useGraphData.ts` | Modified | Exposed `isRefreshing: boolean` and `refetchGraph(): void` in `UseGraphDataReturn`; `pinnedGraphPathRef` sync effect; separate `refetchAbortRef` for imperative refetch; unmount-cleanup effect for `refetchAbortRef`; `useLocalStorage` for `rightTab` persistence with `storageKeyTab` param. |
| `src/hooks/useWebSocket.ts` | Modified | Added `sendRawText: (text: string) => void` to `UseWebSocketReturn` — stable `useCallback` wrapping `ctx.send(wsPath, text)`, guarded by `phase !== 'connected'`. Added `appendMessage: (raw: string) => void` to `UseWebSocketReturn`. |
| `src/hooks/useAutoGraphRefresh.ts` | **New** | Core auto-refresh logic: `watermarkRef` (separate mount-only `useEffect`); dedicated `[connected]` effect for disconnect reset + debounce cancellation; `pinnedGraphPathRef` stale-closure fix; two-pass main effect (Pass 1: consume graph-link; Pass 2: detect mutations); `import-graph` path immediate (no debounce); `node-mutation` path 300 ms debounce → `sendRawText('describe graph')` (both pinned and un-pinned); `refetchGraph` accepted in interface as `_refetchGraph` (unused). |
| `src/components/GraphView/GraphView.tsx` | Modified | Accepted `isRefreshing?: boolean` (default `false`); renders `.refreshingOverlay` + `.refreshingSpinner` when `isRefreshing`; `aria-busy={isRefreshing}` on `.graphWrapper`; `role="status"` + `aria-label="Graph refreshing"` on spinner; `onCopySuccess`/`onCopyError` props forwarded to `GraphToolbar`. |
| `src/components/GraphView/GraphView.module.css` | Modified | Added `.refreshingOverlay` (absolute, `rgba(26,26,46,0.55)` bg, `pointer-events:none`, bottom-right flex alignment) and `.refreshingSpinner` (`1.25rem`, `graphRefreshSpin` keyframe, `0.75s linear infinite`). Added `position: relative` to `.graphWrapper`. |
| `src/components/RightPanel/RightPanel.tsx` | Modified | Accepted + forwarded `isGraphRefreshing?: boolean` prop to `GraphView` as `isRefreshing`; added `onCopySuccess`/`onCopyError` props for `GraphView` clipboard actions. |
| `src/components/Playground.tsx` | Modified | Instantiated `useAutoGraphRefresh` (passes `connected` from `ws`, `refetchGraph` from `useGraphData`, `sendRawText` from `ws`); forwarded `isRefreshing` as `isGraphRefreshing` to `RightPanel`; `useGraphData` now receives `tabs[0]` and `storageKeyTab` params. |

---

## 10. Edge Cases & Pitfall Index

| # | Scenario | Mitigation |
|---|---|---|
| E1 | User clears console while refresh is in-flight | `handleClearMessages` in `Playground.tsx` (line 280) sets `pinnedGraphPath = null`, `setGraphData(null)`. Since `messages` changes (cleared), the main effect in `useAutoGraphRefresh` re-runs; its cleanup cancels any pending debounce. The disconnect-reset effect is independent. Since `refetchGraph` is not called by the hook in the current implementation, there is no in-flight HTTP request to abort from the `refetchGraph` path. If `waitingForDescribeRef` is set, the cleared console produces no new graph-link messages, so it stays set until the next WS reconnect/disconnect cycle clears it. |
| E2 | User disconnects mid-session | No new WS messages arrive; no trigger fires. The dedicated `[connected]` effect resets `waitingForDescribeRef = false` and cancels the pending debounce on disconnect (lines 120-132). On reconnect, session state on the server is fresh — the user can issue a new mutation to trigger a fresh auto-refresh. |
| E3 | `import graph` imports a graph whose root name differs from `pinnedGraphPath` | Handled: `describe graph` is auto-sent, server returns the correct new path, which replaces `pinnedGraphPath` via `waitingForDescribeRef` → `setPinnedGraphPath` |
| E4 | `import graph` fails (file not found in both temp and deployed locations) | Server sends `"Graph model not found in …"` (and possibly `"Found deployed graph model in …"`) — neither message contains `"graph model imported as draft"` → not matched → no action |
| E5 | Echo line `"> create node foo"` matched as mutation | Guarded: `raw.startsWith('>')` in `detectMutation` returns `null` |
| E6 | Graph-link message matched as mutation | Guarded: `isGraphLinkMessage(raw)` check in `detectMutation` returns `null` |
| E7 | React Strict Mode double-effect execution | Guarded: `watermarkRef` watermark prevents reprocessing already-seen messages. The mount-only watermark effect also double-invokes in Strict Mode, but it is idempotent — it only reads `messages[last].id`. |
| E8 | User rapid-fires 5 mutation commands while no graph is pinned | Debounce collapses all 5 into a single `describe graph` send. The watermark advances on the first main-effect run that sees the batch, so no message is re-scanned. If `waitingForDescribeRef` is already `true` when the debounce fires, the debounce callback calls `sendRawText('describe graph')` and sets the flag again (idempotent). Only one `describe graph` round-trip is in flight because the server responds with exactly one graph-link per `describe graph`. |
| E9 | Server sends `"Node foo already exists"` (create on existing node) | Starts with `"Node "` so `startsWithNode` is true, but does not contain `" created"` → not matched |
| E10 | Auto-sent `describe graph` triggers another auto-refresh loop | `describe graph` response is a graph-link → `isGraphLinkMessage()` is true → `detectMutation` returns `null` → no loop |
| E11 | Component unmounts during debounce window | `useEffect` cleanup `clearTimeout(debounceTimerRef.current)` fires on unmount, cancelling the pending timer. `sendRawText` is never called after teardown. |
| E12 | `pinnedGraphPath` changes while a debounced refresh is pending | The debounce callback reads `pinnedGraphPathRef.current` at fire time (for the toast message only). The `sendRawText('describe graph')` call is path-agnostic — the server always serialises the current in-memory graph. A change to `pinnedGraphPath` does not re-run the main effect (it is not in the dep array), so the pending debounce continues unaffected and fires the correct `describe graph`. |
| E13 | ~~`import-graph` triggered but `pinnedGraphPath` is null~~ | **No longer an edge case (v4).** `import-graph` now sends `describe graph` regardless of `pinnedGraphPath`. The `waitingForDescribeRef` path handles both null and non-null cases identically. |
| E14 | `import node` success but the node's graph was never pinned | `pinnedGraphPath === null` → debounce fires → `describe graph` auto-sent → graph-link received → `setPinnedGraphPath` called → initial-load path fires → tab switches to Graph. Same as §5.2. |
| E15 | `delete connection` emits two `"removed"` lines in one message | Both `"A -> B removed"` lines are in a single `msg.raw` — the narrowed `' -> ' && 'removed'` check still matches; `detectMutation` fires once per message; debounce collapses any duplicates |
| E16 | `instantiate graph` response `"Graph instance created. Loaded N mock entries…"` matched as `node-mutation` | Guarded: `lower.startsWith('node ')` is false for this message — the `" created"` branch is never reached. `instantiate graph` creates a runtime instance, not a model change. |
| E17 | `export graph as {name}` auto-creates a root node — `"Root node created because it does not exist"` matched as `node-mutation` | Guarded: `lower.startsWith('node ')` is false (`"root node…"` does not begin with `"node "`). The `export` command already emits a graph-link; the pin flow handles the refresh. A double-refresh is unnecessary and avoided. |
| E18 | `node-mutation` detected while `waitingForDescribeRef` is already `true` (e.g. a second mutation arrives before the `describe graph` response) | The debounce fires and calls `sendRawText('describe graph')` again, setting `waitingForDescribeRef.current = true` (idempotent). This sends a second `describe graph` command. The server will respond with two graph-link messages. Pass 1 of the main effect consumes the first one (sets `pinnedGraphPath`), then `return`s. On the next effect run, `watermarkRef` has already advanced past both graph-link messages, so the second one is never re-processed. Net effect: two `describe graph` commands are sent, but only the first response is consumed — the second graph-link is silently ignored. This is a minor inefficiency but not a correctness issue. |
| E19 | User manually pins a different graph while `waitingForDescribeRef === true` | The user clicks a console row containing a graph-link → `handlePinMessage` fires → `setPinnedGraphPath(oldPath)`. The hook then sees the server's `describe graph` response (a graph-link message) with `waitingForDescribeRef === true` and calls `setPinnedGraphPath(newPath)`, silently overwriting the user's manual pin. `waitingForDescribeRef` must still be cleared immediately when the hook calls `setPinnedGraphPath` to prevent the flag remaining set for a subsequent auto-describe round-trip. **Accepted v4 trade-off:** this race requires the user to manually pin a graph-link row within the < 100 ms window between the hook sending `describe graph` and the server responding — an extremely narrow window in practice. The hook always sets the freshest server-derived path. A future hardening could store the trigger message `id` in `waitingForDescribeRef` (change type from `boolean` to `number | false`) and skip calling `setPinnedGraphPath` if a manual pin has already fired, but this is out of scope for v4. |
| E20 | `node-mutation` detected immediately after session open (empty graph, 0 nodes) | `describe graph` is auto-sent. Server responds with `"Graph with 0 nodes described in /api/graph/model/{id}/{n}"`. This is a valid graph-link. `setPinnedGraphPath` is called, `useGraphData` fetches and renders an empty graph. The Graph tab switches. This is correct — the user will see the result of their mutation (1 node) after the next refresh cycle, or after the debounce fires a second `describe graph`. In practice this shouldn't happen: the mutation itself produces a success message which fires the debounce, so the tab will show the updated 1-node graph shortly after. |
| E21 | `import graph` triggered, `waitingForDescribeRef` set, but WS disconnects before server replies | **Mitigated (implemented).** The dedicated `[connected]` effect (lines 120-132 of `useAutoGraphRefresh.ts`) resets `waitingForDescribeRef = false` **and** cancels any pending `debounceTimerRef` when `connected` becomes `false`. This prevents a stale flag from consuming the first graph-link that arrives after reconnect, and prevents a stranded debounce from calling `sendRawText` after reconnect with a stale `waitingForDescribeRef = true`. |
| E22 | User issues `import graph` with no graph previously pinned; server sends the `"Found deployed graph model…"` informational message before `"Graph model imported as draft"` | The informational message does not match `detectMutation` → ignored. `"Graph model imported as draft"` fires the `import-graph` path and sends `describe graph`. Correct behaviour, no spurious trigger. |

---

## 11. Open Questions (Deferred)

These are explicitly **out of scope** for this feature but noted for future consideration:

- **Graph tab badge pulse:** Should the `🕸️` badge on the Graph tab animate during refresh? Currently it is stable as long as `graphData !== null`. Could be a nice UX polish in a follow-up.

- **Auto-refresh toast verbosity:** Currently the spec proposes a toast only on error (and one `info` toast for `import-graph`). If users find silent node-mutation refreshes confusing, a subtle success toast (e.g. "Graph updated") could be added — but this risks being noisy for rapid editing sessions.

- **Multi-session / shared socket:** If two browser tabs were ever to share a WebSocket session (not currently possible — each tab opens its own socket), mutation messages from one tab would trigger refresh in the other. Not a current concern.

- **`export graph as {name}` double-update:** `export` already emits a graph-link (handled by the existing pin flow). It does not need the new auto-refresh path. However, if a user exports and then immediately does a CRUD mutation, they could get two rapid refreshes. The debounce handles this gracefully.

- **Offline / server-restart resilience:** If the server restarts and loses its in-memory graph, the next auto-refresh fetch will return 404/500. The error toast (E1 mitigation) covers this, but a more specific message ("Graph session lost — reconnect and re-describe") could improve the experience.
