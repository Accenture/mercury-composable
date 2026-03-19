# Feature Spec: Auto-Rerender of Graph on Mutation Commands

**Target:** `webapp/` (React/TypeScript frontend)
**Branch:** `feature/playground`
**Date:** 2026-03-12
**Status:** Revised v4 — approved, ready to implement

### Revision history

| Version | Date | Changes |
|---|---|---|
| v1 | 2026-03-11 | Initial spec |
| v2 | 2026-03-11 | Applied peer-review findings: B1 stale-closure fix (`pinnedGraphPathRef`); B2 stable `useCallback` constraint; B3 `sendRawText` new API + changelist entry; I1 `setPinnedGraphPath` + `waitingForDescribeRef` mechanism; I2 mount-time watermark init; I3 dep-array requirement in §8; I4 ARIA on loading overlay; I5 narrowed `removed` match to `' -> ' && 'removed'`; C1 `rightTab` read-only clarification; C2 E1 dep-array cross-reference; C3 `useWebSocket.ts` added to changelist |
| v3 | 2026-03-12 | Tightened node-mutation match rules (A1/B2): added `lower.startsWith('node ')` prefix guard for `created`, `updated`, `deleted`, `connected to`, `imported from`, `overwritten by node from` checks — eliminates false positives from `"Graph instance created…"` (`instantiate graph`) and `"Root node created because it does not exist"` (`export graph`). Updated §2.2.1 table, §3.1 code block + false-positive table, §10 edge cases E16–E17. Spec review fixes: updated §3.1 JSDoc to document `startsWithNode` compound condition explicitly; removed `aria-live="polite"` from §6.2 accessibility bullet (`role="status"` already implies it per ARIA 1.2); updated false-positive table row and E9 to note that `"Node foo already exists"` passes `startsWithNode` but still does not match due to absent `" created"`. |
| v4 | 2026-03-12 | Expanded goal: mutation commands now auto-generate and auto-pin the graph when `pinnedGraphPath === null`, rather than being a silent no-op. Removed `pinnedGraphPath !== null` precondition from §2.2. Removed guard from `import-graph` path (§2.2.2). Both `node-mutation` and `import-graph` now share the same `waitingForDescribeRef` / `describe graph` auto-send path when nothing is pinned. Updated §1 (goal + data flow), §2.2, §4.1 (responsibility table + data flow diagram), §5.1, §5.2, §5.3 (replaced silent no-op with auto-describe path), §5.4 (tab-switch table). Added edge cases E18–E22 to §10. |

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
User sends a mutation command
  → server responds with a plain-text success message (no graph-link)
  → useAutoGraphRefresh detects the mutation in the incoming message stream
  → triggers re-fetch of the currently pinned graph path  ← NEW
  → graph re-renders in place with a subtle loading overlay  ← NEW
  → tab does NOT switch (user is not interrupted)  ← NEW
```

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
> - If `pinnedGraphPath !== null` — re-fetch the existing pinned path in place (loading overlay shown).
> - If `pinnedGraphPath === null` — auto-send `describe graph` over the WebSocket, consume the resulting graph-link, and call `setPinnedGraphPath`. This triggers the initial-load path in `useGraphData`, which fetches the graph and auto-switches the tab to Graph.
>
> Both paths share the same `waitingForDescribeRef` / `describe graph` auto-send mechanism that the `import-graph` path already uses (see §5.3).

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

**File:** `src/utils/messageParser.ts`

```ts
export type MutationKind = 'node-mutation' | 'import-graph';

/**
 * Returns the kind of graph mutation this raw WebSocket message represents,
 * or null if the message is not a mutation signal.
 *
 * Rules:
 *  - JSON messages are never mutations (lifecycle events, JsonView payloads).
 *  - Echoed commands (lines starting with "> ") are never mutations.
 *  - Graph-link messages are never mutations (handled by the pin flow).
 *  - "Graph model imported as draft" → 'import-graph'
 *
 * Returns 'node-mutation' for messages that start with "Node " AND contain any of:
 *  - " created"                   → create node {name}
 *  - " updated"                   → update node {name}
 *  - " deleted"                   → delete node {name}
 *  - " connected to "             → connect {A} to {B} with {rel}
 *  - " imported from "            → import node {name} from {file}
 *  - " overwritten by node from " → import node (overwrite variant)
 * Or for messages that contain both (no "Node " prefix required):
 *  - " -> " AND "removed"         → delete connection {A} and {B}
 *
 * The "Node " prefix guard is required because other server responses contain
 * words like "created" without that prefix (e.g. "Graph instance created…",
 * "Root node created…") and must not trigger a refresh.
 */
export function detectMutation(raw: string): MutationKind | null {
  if (!isMarkdownCandidate(raw)) return null;   // JSON → not a mutation
  if (isGraphLinkMessage(raw))   return null;   // graph-link → pin flow handles it
  if (raw.startsWith('>'))       return null;   // echoed command

  const lower = raw.toLowerCase();

  if (lower.includes('graph model imported as draft')) return 'import-graph';

  // All node operation success messages share the "Node " prefix (Java constant
  // NODE_NAME). The prefix guard eliminates false positives from:
  //   "Graph instance created. Loaded N mock entries…"  (instantiate graph)
  //   "Root node created because it does not exist"     (export graph, missing root)
  const startsWithNode = lower.startsWith('node ');

  if (
    (startsWithNode && lower.includes(' created'))                ||
    (startsWithNode && lower.includes(' updated'))                ||
    (startsWithNode && lower.includes(' deleted'))                ||
    (startsWithNode && lower.includes(' connected to '))          ||
    (lower.includes(' -> ') && lower.includes('removed'))         ||  // "A -> B removed" (delete connection)
    (startsWithNode && lower.includes(' imported from '))         ||
    (startsWithNode && lower.includes(' overwritten by node from '))
  ) return 'node-mutation';

  return null;
}
```

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
| Abort-controller-aware re-fetch | `useGraphData.ts` — new `refetchGraph()` method |
| `isRefreshing` state | `useGraphData.ts` — new `isRefreshing: boolean` |
| `pinnedGraphPath` ref sync (stale-closure fix) | `useGraphData.ts` — `pinnedGraphPathRef` |
| Debounce + message-ID tracking | `useAutoGraphRefresh.ts` (new file) |
| `node-mutation` + graph already pinned → re-fetch | `useAutoGraphRefresh.ts` — calls `refetchGraph()` after 300 ms debounce |
| `node-mutation` + **no graph pinned** → auto-describe | `useAutoGraphRefresh.ts` — sends `describe graph`, sets `waitingForDescribeRef = true` |
| `import-graph` → WS `describe graph` command (always) | `useAutoGraphRefresh.ts` — sends `describe graph` regardless of `pinnedGraphPath` |
| Detecting `describe graph` response, calling `setPinnedGraphPath` | `useAutoGraphRefresh.ts` — `waitingForDescribeRef` flag |
| Sending arbitrary WS text (new API) | `useWebSocket.ts` — new `sendRawText` method |
| Tab-switch on existing-graph refresh | Suppressed — `useAutoGraphRefresh` never calls `setRightTab` |
| Tab-switch on first auto-pin (no prior graph) | Natural — `useGraphData` initial-load path switches to `'graph'` when `pinnedGraphPath` changes from `null` to a value |
| Loading overlay in graph view | `GraphView.tsx` + `GraphView.module.css` |
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
   * Current right-panel tab.
   * Passed as a read-only input for future use (e.g. badge-pulse deferred to §11).
   * This hook does NOT call setRightTab directly — tab switching is handled
   * by useGraphData's initial-load path when setPinnedGraphPath is called.
   */
  rightTab:           RightTab;
  /**
   * Whether the WebSocket is currently connected.
   * Used to reset waitingForDescribeRef on disconnect, preventing a stale
   * flag from triggering setPinnedGraphPath after reconnect (see E21).
   */
  connected:          boolean;
  /**
   * Sends an arbitrary raw text string over the active WebSocket.
   * This is a NEW method that must be added to UseWebSocketReturn in
   * useWebSocket.ts — see §9. It is a thin stable useCallback wrapping
   * ctx.send(wsPath, text) from WebSocketContext.
   */
  sendRawText:        (text: string) => void;
  /**
   * Triggers an imperative re-fetch of pinnedGraphPath.
   * Must be a stable useCallback (empty dep array) from useGraphData —
   * reads pinnedGraphPath via ref internally (see §6.1 stale-closure fix).
   * Including an unstable function reference here would cause the effect
   * to re-run on every render and reprocess the entire message log.
   */
  refetchGraph:       () => void;
  /** Called when import-graph auto-fetch or no-pin auto-describe finds a new graph-link path. */
  setPinnedGraphPath: (path: string) => void;
  /** Toast callback from useToast. */
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
  │       ├── 'node-mutation'
  │       │       ├── pinnedGraphPath !== null
  │       │       │       └── debounce(300ms) → refetchGraph()
  │       │       └── pinnedGraphPath === null
  │       │               └── debounce(300ms) → sendRawText('describe graph')
  │       │                                      sets waitingForDescribeRef = true
  │       │
  │       └── 'import-graph' (always, regardless of pinnedGraphPath)
  │               └── sendRawText('describe graph')
  │                   sets waitingForDescribeRef = true
  │
  │       [both paths above converge here when server replies]
  │               server responds with graph-link message
  │               isGraphLinkMessage(raw) && waitingForDescribeRef
  │                 → setPinnedGraphPath(extractGraphApiPath(raw))
  │                 → waitingForDescribeRef = false
  │                 → pinnedGraphPath was null → initial-load fires:
  │                     graphData populated, setRightTab('graph')  ← tab switches
  │                 → pinnedGraphPath was set → path-change re-fetch fires:
  │                     graphData updated, isRefreshing clears, no tab switch
  │
  └── (no matched mutation kind) → no-op
        │
        ▼
useGraphData
  ├── pinnedGraphPathRef (kept in sync via useEffect)
  ├── fetch(pinnedGraphPathRef.current) + AbortController
  ├── isRefreshing: boolean  (true while auto-refresh fetch is in-flight)
  └── graphData: MinigraphGraphData | null
        │
        ▼
Playground.tsx
  └── RightPanel → GraphView (isRefreshing prop)
```

---

## 5. Detailed Behaviour Specification

### 5.1 `node-mutation` path — graph already pinned

```
Incoming message detected as 'node-mutation'
  AND pinnedGraphPath !== null
    → start/reset 300 ms debounce timer
    → on debounce fire:
        → call refetchGraph()
            → sets isRefreshing = true
            → fires fetch(pinnedGraphPath) with a fresh AbortController
                (cancels any previously in-flight request)
            → on HTTP success (200):
                → validate shape with isMinigraphGraphData()
                → setGraphData(newData)
                → setIsRefreshing(false)
                → NO tab switch — user is not interrupted
            → on HTTP error or shape validation failure:
                → addToast('Graph refresh failed: …', 'error')
                → setIsRefreshing(false)
                → graphData is NOT cleared (stale data preferred over empty state)
```

**Debounce rationale:** 300 ms collapses rapid-fire commands (e.g. pasting history + Enter multiple times) into a single fetch, preventing N redundant requests.

### 5.2 `node-mutation` path — no graph pinned yet

```
Incoming message detected as 'node-mutation'
  AND pinnedGraphPath === null
    → start/reset 300 ms debounce timer
    → on debounce fire:
        → send WebSocket text via sendRawText: 'describe graph'
        → set waitingForDescribeRef = true

Next message scan iteration:
  → new message arrives that passes isGraphLinkMessage()
    AND waitingForDescribeRef === true
      → path = extractGraphApiPath(msg.raw)
      → call setPinnedGraphPath(path)        ← updates Playground.tsx state
      → set waitingForDescribeRef = false
      → mark message as processed (update lastProcessedIdRef watermark)
      → useGraphData initial-load path fires (pinnedGraphPath: null → value):
          → graphData null'd while fetch in-flight
          → fetch(path) succeeds → setGraphData(newData)
          → setRightTab('graph')             ← tab switches to Graph
```

**Why auto-describe rather than a direct fetch when `pinnedGraphPath === null`?** There is no path to fetch — `pinnedGraphPath` is `null`. `describe graph` both serialises the current in-memory graph (making it available via REST) and returns the canonical path in its response. This is the only correct mechanism.

**Why the tab switches here but not on §5.1?** Because `setPinnedGraphPath` triggers the initial-load path in `useGraphData`, which switches the tab unconditionally when `pinnedGraphPath` changes from `null` to a value. This is the same behaviour as a manual pin click — and it is exactly what the user wants: they issued a mutation command with no graph displayed yet, and they should immediately see the result.

### 5.3 `import-graph` path

```
Incoming message = 'Graph model imported as draft'
  (pinnedGraphPath may be null OR non-null — no guard)
    → send WebSocket text via sendRawText: 'describe graph'
    → set waitingForDescribeRef = true
    → toast: 'Graph imported — refreshing view…' (info)

Next message scan iteration:
  → new message arrives that passes isGraphLinkMessage()
    AND waitingForDescribeRef === true
      → path = extractGraphApiPath(msg.raw)
      → call setPinnedGraphPath(path)        ← updates Playground.tsx state
      → set waitingForDescribeRef = false
      → mark message as processed (update lastProcessedIdRef watermark)
      → if pinnedGraphPath was null:
          → useGraphData initial-load path fires
          → graphData populated, setRightTab('graph')  ← tab switches to Graph
      → if pinnedGraphPath was already set (possibly a different path):
          → useGraphData path-change path fires (old path → new path)
          → graphData null'd then re-populated with new graph
          → setRightTab('graph')  ← tab switches to Graph
```

**Why `import-graph` always switches the tab (unlike `node-mutation` §5.1)?** An import replaces the entire in-memory graph — often with a structurally different model. Switching to the Graph tab on import is intentional and expected: the user explicitly replaced the graph and should see it immediately. This is consistent with how `export graph` already works (it emits a graph-link that the manual pin flow processes, switching the tab).

**Why `waitingForDescribeRef` is needed (I1):** The "existing pin flow" is user-driven —
it fires when the user *clicks* a console row, calling `handlePinMessage` in Playground.tsx.
An automatically sent `describe graph` produces a new graph-link message that no one clicks.
The hook must therefore detect this response itself and call `setPinnedGraphPath` directly.
The `waitingForDescribeRef` flag scopes this detection to only the graph-link that arrives
*after* the hook sent `describe graph` — it does not interfere with unrelated graph-link
messages that the user may pin manually.

**Loop prevention:** `detectMutation` returns `null` for all graph-link messages
(`isGraphLinkMessage` guard). The graph-link response from `describe graph` is consumed by
the `waitingForDescribeRef` branch, not the `detectMutation` branch — so there is no loop.

### 5.4 Tab-switching rule

| Scenario | Behaviour |
|---|---|
| `node-mutation`, graph already pinned — refresh completes | No tab switch — graph updates in place under loading overlay |
| `node-mutation`, **no graph pinned** — first auto-pin | **Switches to Graph tab** — initial-load path in `useGraphData` fires |
| `import-graph` (any state) — refresh completes | **Switches to Graph tab** — import is a deliberate whole-graph replacement |
| User is on Graph Data (Raw) tab during background refresh | No tab switch (§5.1 only) |
| User is on Payload Editor / Developer Guides during background refresh | No tab switch (§5.1 only) |
| **Initial manual pin** (existing behaviour) | **Always** switches to Graph tab ← unchanged |

---

## 6. Loading State — `isRefreshing`

### 6.1 Changes to `useGraphData`

The hook's return type is extended:

```ts
export interface UseGraphDataReturn {
  graphData:    MinigraphGraphData | null;
  setGraphData: React.Dispatch<React.SetStateAction<MinigraphGraphData | null>>;
  rightTab:     RightTab;
  setRightTab:  React.Dispatch<React.SetStateAction<RightTab>>;
  isRefreshing: boolean;   // NEW — true while a re-fetch is in-flight
  refetchGraph: () => void; // NEW — imperatively trigger a re-fetch
}
```

**Key design decision:** During auto-refresh `graphData` is **NOT nulled out**. The stale graph remains visible under the loading overlay. Nulling would cause a jarring disappear/reappear cycle. Only the initial load (when `pinnedGraphPath` first becomes non-null) nulls the data, as it does today.

The `refetchGraph()` method:
- Can be called imperatively (e.g. from `useAutoGraphRefresh`)
- Uses the same `AbortController` + `cancelled` pattern as the existing `useEffect`
- Sets `isRefreshing = true` immediately on call
- Does **not** null `graphData` (unlike the initial path-change flow)
- **Must be a stable `useCallback` with an empty dependency array** so that
  `useAutoGraphRefresh` can include it in a `useEffect` dependency array without
  causing the effect to re-run on every render.

**Stale-closure fix (B1):** Because `refetchGraph` has an empty dependency array it cannot
close over `pinnedGraphPath` directly. Instead, `useGraphData` must keep a `useRef` that
is kept in sync with the prop, and `refetchGraph` reads the path through the ref:

```ts
const pinnedGraphPathRef = useRef(pinnedGraphPath);
useEffect(() => {
  pinnedGraphPathRef.current = pinnedGraphPath;
}, [pinnedGraphPath]);

const refetchGraph = useCallback(() => {
  const path = pinnedGraphPathRef.current;
  if (!path) return;
  // ... create AbortController, fetch(path), set isRefreshing, etc.
}, []); // stable — reads via ref, never via closure
```

This guarantees that even if `pinnedGraphPath` changes (e.g. from `/api/graph/model/foo`
to `/api/graph/model/bar`) while a 300 ms debounce is pending, the timer fires and fetches
the **new** path, not the stale one.

### 6.2 GraphView loading overlay

A subtle non-blocking overlay is added to `GraphView` when `isRefreshing === true`:

- Position: absolute, covers the graph area
- Background: semi-transparent (e.g. `rgba(0,0,0,0.3)`)
- `pointer-events: none` — user can still interact with the graph underneath
- Content: a small spinner in one corner (e.g. bottom-right)
- Disappears instantly when new data arrives (React state transition)
- **Accessibility:** The spinner element must carry `role="status"` and `aria-label="Graph refreshing"`.
  Note: `role="status"` already implies `aria-live="polite"` per the ARIA 1.2 spec — do **not**
  add `aria-live` explicitly, as it would be redundant. The element is non-focusable due to
  `pointer-events: none`, but screen readers will still announce it via the implicit live region.
  Additionally, set `aria-busy={isRefreshing}` on the `<div>` wrapping `<ReactFlow>` so assistive
  technology understands that the content region is updating.

**Prop chain:** `Playground` → `RightPanel` → `GraphView`

```ts
// RightPanel additions
interface RightPanelProps {
  // ... existing props
  isGraphRefreshing?: boolean; // NEW
}

// GraphView additions
interface GraphViewProps {
  graphData:        MinigraphGraphData | null;
  onRenderError?:   (message: string) => void;
  isRefreshing?:    boolean; // NEW
}
```

---

## 7. Message ID Tracking — Avoiding Reprocessing

`useAutoGraphRefresh` must process **only messages it has not yet seen**. This is critical because:

- The `messages` array from `WebSocketContext` is rebuilt on every new message (the reducer returns a new array reference), causing the `useEffect` to re-run.
- React Strict Mode double-invokes effects in development.
- Without tracking, the same mutation message would be processed on every subsequent re-render.

**Implementation:**

```ts
// Initialise to the id of the last *already-present* message so that messages
// which arrived before this component mounted (e.g. after a route navigation
// away and back) are never reprocessed. -1 is safe when messages is empty.
const lastProcessedIdRef = useRef<number>(
  messages.length > 0 ? messages[messages.length - 1].id : -1
);

useEffect(() => {
  // Reset waitingForDescribeRef on disconnect so a stale flag cannot
  // trigger setPinnedGraphPath after reconnect (see E21).
  if (!connected) {
    waitingForDescribeRef.current = false;
    return;
  }

  const newMessages = messages.filter(m => m.id > lastProcessedIdRef.current);
  if (newMessages.length === 0) return;

  // update the watermark first — before any async work
  lastProcessedIdRef.current = newMessages[newMessages.length - 1].id;

  for (const msg of newMessages) {
    const kind = detectMutation(msg.raw);
    // ... handle kind
  }
}, [messages, pinnedGraphPath, connected, sendRawText, refetchGraph, setPinnedGraphPath, addToast]);
```

**Why not initialise to `-1`?** When `Playground.tsx` remounts (e.g. user navigates to
another route and back), `useAutoGraphRefresh` remounts with a fresh `useRef`. However,
`WebSocketContext` is mounted above `<Routes>` and survives navigation — its `messages`
array may already contain N messages with ids `[1 … N]`. Initialising to `-1` would cause
all N messages to be re-scanned on mount, potentially re-triggering a `describe graph`
command for a mutation that happened minutes ago. Initialising to the last existing id
skips them correctly.

---

## 8. Debounce Implementation

Use a `useRef<ReturnType<typeof setTimeout>>` (not `useState`) to avoid triggering extra renders:

```ts
const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

// Inside the effect, on 'node-mutation' detection:
if (debounceRef.current) clearTimeout(debounceRef.current);
debounceRef.current = setTimeout(() => {
  refetchGraph();
  debounceRef.current = null;
}, 300);

// Effect cleanup:
return () => {
  if (debounceRef.current) {
    clearTimeout(debounceRef.current);
    debounceRef.current = null;
  }
};
```

`refetchGraph()` itself creates a new `AbortController` on each call and cancels the previous one, so even if the debounce fires twice in close succession, only the last fetch result is applied.

**`pinnedGraphPath` must be in the `useEffect` dependency array (I3).** When
`pinnedGraphPath` changes — either because the user pins a different graph manually, or
because `setPinnedGraphPath` is called from the `import-graph` path — React tears down the
effect and re-runs it. The teardown fires the cleanup function which calls
`clearTimeout(debounceRef.current)`, cancelling any debounce timer that was pending for the
*old* path. The new effect starts with a clean slate. If `pinnedGraphPath` were accidentally
omitted from the dep array, a pending debounce from an old path could fire and call
`refetchGraph` for a path that is no longer pinned (even though `refetchGraph` reads through
`pinnedGraphPathRef`, it would still fetch — just the right path — creating a surprising
ghost refresh).

---

## 9. File Changelist

| File | Type | Summary of change |
|---|---|---|
| `src/utils/messageParser.ts` | Modify | Add `MutationKind` type and `detectMutation()` function |
| `src/hooks/useGraphData.ts` | Modify | Expose `isRefreshing: boolean` and `refetchGraph(): void`; `pinnedGraphPathRef` sync; do NOT null `graphData` on auto-refresh |
| `src/hooks/useWebSocket.ts` | Modify | Add `sendRawText: (text: string) => void` and expose `connected: boolean` in `UseWebSocketReturn` — `sendRawText` is a thin stable `useCallback` wrapping `ctx.send(wsPath, text)` |
| `src/hooks/useAutoGraphRefresh.ts` | **New** | Core auto-refresh logic: message scan, debounce, no-pin auto-describe, `import-graph` WS dispatch, `setPinnedGraphPath` on new graph-link; E21 disconnect reset; `connected` prop in options interface |
| `src/components/GraphView/GraphView.tsx` | Modify | Accept `isRefreshing?: boolean`; render non-blocking loading overlay with ARIA attributes |
| `src/components/GraphView/GraphView.module.css` | Modify | Add `.refreshingOverlay` and `.refreshingSpinner` styles |
| `src/components/RightPanel/RightPanel.tsx` | Modify | Accept + forward `isGraphRefreshing?: boolean` prop |
| `src/components/Playground.tsx` | Modify | Instantiate `useAutoGraphRefresh` (pass `connected` from `ws`); forward `isRefreshing` to `RightPanel` |

---

## 10. Edge Cases & Pitfall Index

| # | Scenario | Mitigation |
|---|---|---|
| E1 | User clears console while refresh is in-flight | `handleClearMessages` in Playground.tsx sets `pinnedGraphPath = null`. Because `pinnedGraphPath` is in `useAutoGraphRefresh`'s `useEffect` dep array (see §8), React tears down the effect, the cleanup fires `clearTimeout(debounceRef.current)`, and any pending debounce is cancelled. Simultaneously, `refetchGraph`'s `AbortController` cancels the in-flight fetch. No stale result is applied. |
| E2 | User disconnects mid-session | No new WS messages arrive; no trigger fires. `waitingForDescribeRef` is reset to `false` on disconnect (see E21) so a stale flag cannot fire `setPinnedGraphPath` on reconnect. On reconnect, session state on the server is fresh — the existing `pinnedGraphPath` will still re-fetch correctly if the user issues a mutation after reconnecting. |
| E3 | `import graph` imports a graph whose root name differs from `pinnedGraphPath` | Handled: `describe graph` is auto-sent, server returns the correct new path, which replaces `pinnedGraphPath` via `waitingForDescribeRef` → `setPinnedGraphPath` |
| E4 | `import graph` fails (file not found in both temp and deployed locations) | Server sends `"Graph model not found in …"` (and possibly `"Found deployed graph model in …"`) — neither message contains `"graph model imported as draft"` → not matched → no action |
| E5 | Echo line `"> create node foo"` matched as mutation | Guarded: `raw.startsWith('>')` in `detectMutation` returns `null` |
| E6 | Graph-link message matched as mutation | Guarded: `isGraphLinkMessage(raw)` check in `detectMutation` returns `null` |
| E7 | React Strict Mode double-effect execution | Guarded: `lastProcessedIdRef` ID watermark prevents reprocessing already-seen messages |
| E8 | User rapid-fires 5 mutation commands while no graph is pinned | Debounce collapses all 5 into a single `describe graph` send. Once `waitingForDescribeRef` is set, subsequent mutation detections within the same debounce window are collapsed. Only one `describe graph` is sent. |
| E9 | Server sends `"Node foo already exists"` (create on existing node) | Starts with `"Node "` so `startsWithNode` is true, but does not contain `" created"` → not matched |
| E10 | Auto-sent `describe graph` triggers another auto-refresh loop | `describe graph` response is a graph-link → `isGraphLinkMessage()` is true → `detectMutation` returns `null` → no loop |
| E11 | Component unmounts during debounce window | `useEffect` cleanup `clearTimeout(debounceRef.current)` fires |
| E12 | `pinnedGraphPath` changes while a debounced refresh is pending | `refetchGraph` reads `pinnedGraphPath` through a `useRef` (not a closure), so it always fetches the path that is current at the moment the debounce fires — never a stale value (see §6.1 stale-closure fix) |
| E13 | ~~`import-graph` triggered but `pinnedGraphPath` is null~~ | **No longer an edge case (v4).** `import-graph` now sends `describe graph` regardless of `pinnedGraphPath`. The `waitingForDescribeRef` path handles both null and non-null cases identically. |
| E14 | `import node` success but the node's graph was never pinned | `pinnedGraphPath === null` → debounce fires → `describe graph` auto-sent → graph-link received → `setPinnedGraphPath` called → initial-load path fires → tab switches to Graph. Same as §5.2. |
| E15 | `delete connection` emits two `"removed"` lines in one message | Both `"A -> B removed"` lines are in a single `msg.raw` — the narrowed `' -> ' && 'removed'` check still matches; `detectMutation` fires once per message; debounce collapses any duplicates |
| E16 | `instantiate graph` response `"Graph instance created. Loaded N mock entries…"` matched as `node-mutation` | Guarded: `lower.startsWith('node ')` is false for this message — the `" created"` branch is never reached. `instantiate graph` creates a runtime instance, not a model change. |
| E17 | `export graph as {name}` auto-creates a root node — `"Root node created because it does not exist"` matched as `node-mutation` | Guarded: `lower.startsWith('node ')` is false (`"root node…"` does not begin with `"node "`). The `export` command already emits a graph-link; the pin flow handles the refresh. A double-refresh is unnecessary and avoided. |
| E18 | `node-mutation` detected while `waitingForDescribeRef` is already `true` (e.g. a second mutation arrives before the `describe graph` response) | The second mutation fires the debounce, but `waitingForDescribeRef` is already set. The debounce callback should check `waitingForDescribeRef` before sending another `describe graph`: if already waiting, skip the send. Only one `describe graph` round-trip should be in flight at a time. |
| E19 | User manually pins a different graph while `waitingForDescribeRef === true` | The user clicks a console row containing a graph-link → `handlePinMessage` fires → `setPinnedGraphPath(oldPath)`. The hook then sees the server's `describe graph` response (a graph-link message) with `waitingForDescribeRef === true` and calls `setPinnedGraphPath(newPath)`, silently overwriting the user's manual pin. `waitingForDescribeRef` must still be cleared immediately when the hook calls `setPinnedGraphPath` to prevent the flag remaining set for a subsequent auto-describe round-trip. **Accepted v4 trade-off:** this race requires the user to manually pin a graph-link row within the < 100 ms window between the hook sending `describe graph` and the server responding — an extremely narrow window in practice. The hook always sets the freshest server-derived path. A future hardening could store the trigger message `id` in `waitingForDescribeRef` (change type from `boolean` to `number | false`) and skip calling `setPinnedGraphPath` if a manual pin has already fired, but this is out of scope for v4. |
| E20 | `node-mutation` detected immediately after session open (empty graph, 0 nodes) | `describe graph` is auto-sent. Server responds with `"Graph with 0 nodes described in /api/graph/model/{id}/{n}"`. This is a valid graph-link. `setPinnedGraphPath` is called, `useGraphData` fetches and renders an empty graph. The Graph tab switches. This is correct — the user will see the result of their mutation (1 node) after the next refresh cycle, or after the debounce fires a second `describe graph`. In practice this shouldn't happen: the mutation itself produces a success message which fires the debounce, so the tab will show the updated 1-node graph shortly after. |
| E21 | `import graph` triggered, `waitingForDescribeRef` set, but WS disconnects before server replies | `describe graph` was sent but the response never arrives. `waitingForDescribeRef` stays `true` indefinitely. On reconnect, a fresh session starts; the user issues any mutation → `waitingForDescribeRef` is still `true` from the previous cycle. **Mitigation:** On WS disconnect (detected via `connected` becoming `false` in `useAutoGraphRefresh`), reset `waitingForDescribeRef = false`. Implementation: add `connected` to the `useEffect` dep array and add a `if (!connected) { waitingForDescribeRef.current = false; return; }` guard at the top of the effect. |
| E22 | User issues `import graph` with no graph previously pinned; server sends the `"Found deployed graph model…"` informational message before `"Graph model imported as draft"` | The informational message does not match `detectMutation` → ignored. `"Graph model imported as draft"` fires the `import-graph` path and sends `describe graph`. Correct behaviour, no spurious trigger. |

---

## 11. Open Questions (Deferred)

These are explicitly **out of scope** for this feature but noted for future consideration:

- **Graph tab badge pulse:** Should the `🕸️` badge on the Graph tab animate during refresh? Currently it is stable as long as `graphData !== null`. Could be a nice UX polish in a follow-up.

- **Auto-refresh toast verbosity:** Currently the spec proposes a toast only on error (and one `info` toast for `import-graph`). If users find silent node-mutation refreshes confusing, a subtle success toast (e.g. "Graph updated") could be added — but this risks being noisy for rapid editing sessions.

- **Multi-session / shared socket:** If two browser tabs were ever to share a WebSocket session (not currently possible — each tab opens its own socket), mutation messages from one tab would trigger refresh in the other. Not a current concern.

- **`export graph as {name}` double-update:** `export` already emits a graph-link (handled by the existing pin flow). It does not need the new auto-refresh path. However, if a user exports and then immediately does a CRUD mutation, they could get two rapid refreshes. The debounce handles this gracefully.

- **Offline / server-restart resilience:** If the server restarts and loses its in-memory graph, the next auto-refresh fetch will return 404/500. The error toast (E1 mitigation) covers this, but a more specific message ("Graph session lost — reconnect and re-describe") could improve the experience.
