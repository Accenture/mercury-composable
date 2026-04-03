# Universal Graph Clipboard — Implementation Specification

**Status:** Draft (Rev 4)  
**Author:** Wes Yu  
**Date:** 2026-03-31  
**Scope:** `extensions/minigraph-playground-engine/webapp/`

---

## Table of Contents

1. [Overview](#1-overview)
2. [Terminology](#2-terminology)
3. [Design Decisions](#3-design-decisions)
4. [Architecture](#4-architecture)
5. [Layer 1 — Storage (IndexedDB)](#5-layer-1--storage-indexeddb)
6. [Layer 2 — Reactivity (BroadcastChannel + React Context)](#6-layer-2--reactivity-broadcastchannel--react-context)
7. [Layer 3 — UI (ClipboardSidebar + Integration Points)](#7-layer-3--ui-clipboardsidebar--integration-points)
8. [Clip Operations](#8-clip-operations)
9. [Paste Execution Strategy](#9-paste-execution-strategy)
10. [PlaygroundConfig Changes](#10-playgroundconfig-changes)
11. [Component Tree Changes](#11-component-tree-changes)
12. [File Inventory](#12-file-inventory)
13. [Edge Cases and Known Limitations](#13-edge-cases-and-known-limitations)
14. [Testing Strategy](#14-testing-strategy)
15. [Future Enhancements](#15-future-enhancements)

---

## 1. Overview

The Universal Graph Clipboard allows a user to **clip** (copy) a node from one
instance of the Minigraph Playground and **paste** it into another instance (or
the same instance) running in a different browser tab. The clipboard persists
across page refreshes and browser restarts.

**Core workflow:**

```
Tab A (Minigraph Playground)             Tab B (Minigraph Playground)
─────────────────────────────            ─────────────────────────────
1. Right-click node "fetcher"
   → "Clip to Clipboard"
2. Node + connections stored
   in IndexedDB
3. BroadcastChannel notifies  ────────►  4. Sidebar updates reactively
   all other tabs                        5. User clicks "Paste" on
                                            the "fetcher" item
                                         6. Check local graphData for
                                            node existence (by alias)
                                         7a. Not found → send `create node …`
                                         7b. Found     → send `update node …`
                                         8. Check local graphData for
                                            connection counterparts;
                                            send `connect` for each
                                            existing counterpart
                                         9. Console shows the mutations;
                                            graph auto-refreshes via
                                            useAutoGraphRefresh
```

---

## 2. Terminology

The following terms are used consistently throughout this document.

| Term | Definition |
|------|------------|
| **Clip** | The act of copying a node snapshot (plus its direct connections) into the clipboard store. Analogous to "copy" but avoids confusion with the browser's system clipboard used by `useCopyToClipboard`. |
| **Paste** | The act of materialising a clipped item into the active playground's graph via WebSocket commands. |
| **Clipboard item** | A single persisted record in IndexedDB representing one clipped node and its associated connections. |
| **Clipboard sidebar** | The dedicated right-edge panel that displays all clipboard items. |
| **Active playground** | The Minigraph Playground instance in the current browser tab where the user is interacting. |
| **Source playground** | The playground instance from which a node was originally clipped. |
| **Target playground** | The playground instance into which a clipboard item is being pasted. |
| **Alias** | The `MinigraphNode.alias` field — the unique identifier of a node within a graph. Defined in `src/utils/graphTypes.ts:12`. Used as the identity key for create-vs-update decisions during paste. |
| **Direct connections** | All entries in `MinigraphGraphData.connections` (defined in `src/utils/graphTypes.ts:32`) where the clipped node's alias appears as either `source` or `target`. Self-connections (where `source === target`) are excluded — see [Section 8.1](#81-clip-trigger-right-click-context-menu-on-reactflow-node). |

---

## 3. Design Decisions

Decisions resolved during the design review, recorded here for future reference.

| # | Question | Decision | Rationale |
|---|----------|----------|-----------|
| 1 | What is copied with a node? | Node snapshot + direct connections (excluding self-connections). On paste, only connections whose counterpart node exists in the target graph's local `graphData` are created; others are silently skipped with a notification. | Allows natural multi-clip workflow: clip several nodes, paste them all, connections wire up as nodes appear. |
| 2 | What determines node identity for create-vs-update? | The `alias` field (`MinigraphNode.alias`, `src/utils/graphTypes.ts:12`). | `alias` is the primary identifier in the backend command grammar: `create node {alias}`, `describe node {alias}`, `update node {alias}`. |
| 3 | Single-node or multi-node clip? | Single-node clip only (one clipboard item per clip action). | Start simple; validate the core workflow before investing in multi-select UI (selection mode, shift-click, lasso). The data model supports future bundling — see [Section 15](#15-future-enhancements). |
| 4 | Cross-playground-type clipboard? | Scoped to Minigraph-type playgrounds only. | Minigraph nodes are not meaningful in the JSON-Path playground context. Gated by a new `supportsClipboard` flag on `PlaygroundConfig`. |
| 5 | How does the user trigger a clip? | Right-click a node in the ReactFlow graph view → context menu → "Clip to Clipboard". | Discoverable and visual. The console command input is reserved exclusively for WebSocket messages to the backend; no client-side command interception. See [Section 3.1](#31-rejected-clip-trigger-alternatives) for rejected alternatives. |
| 6 | How does the user trigger a paste? | "Paste" button on each clipboard item in the sidebar. Pastes into the currently active/connected playground. | Simplest mental model. Each tab's sidebar shows the same items via BroadcastChannel; "Paste" always targets *that tab's* graph. |
| 7 | Duplicate clip handling? | Warn the user and let them choose: replace the existing clipboard entry or cancel. | Preserves user intent. Avoids silent data loss (overwrite) or unbounded growth (multiple versions). |
| 8 | Clipboard item expiry? | No expiry. Items persist until manually removed. | User explicitly chose to clip; auto-deletion would be surprising. IndexedDB has ample capacity. |

### 3.1 Rejected Clip Trigger Alternatives

During the design review, four clip trigger mechanisms were evaluated.
One was selected (see Decision #5); three were rejected:

- **Console command (`clip node {alias}`)** — Rejected. Would require
  client-side interception of user input in `useWebSocket.sendCommand()`
  before it reaches the WebSocket. This violates the design principle that
  the command input is reserved exclusively for messages sent to the backend
  via WebSocket. Mixing client-side-only commands with server commands would
  blur the boundary between local and remote operations and confuse users
  about which commands the server understands.
- **Per-node button in GraphDataView** — Rejected. The raw JSON view
  (`src/components/GraphDataView/GraphDataView.tsx`) renders the entire graph
  as a single JSON tree via `react-json-view-lite`. It has no per-node
  interactive affordances. Adding clip buttons would require a custom renderer
  or a second view mode, adding significant complexity for a secondary
  interaction surface.
- **Toolbar button on selected node** — Rejected. ReactFlow supports node
  selection, but exposing it as a clip trigger requires a visible selection
  mode indicator and changes to the toolbar. This is better suited as a future
  enhancement alongside multi-node clip (Decision #3).

---

## 4. Architecture

### 4.1 Three-Layer Stack

```
┌───────────────────────────────────────────────────────────────────┐
│  Layer 3: UI                                                      │
│  ClipboardSidebar component                                       │
│  GraphView context menu integration                               │
├───────────────────────────────────────────────────────────────────┤
│  Layer 2: Reactivity                                              │
│  ClipboardContext (React Context + useReducer)                     │
│  BroadcastChannel (cross-tab synchronisation)                     │
│  Paste orchestration (local check → create/update → connect)      │
├───────────────────────────────────────────────────────────────────┤
│  Layer 1: Storage                                                 │
│  IndexedDB via `idb` library                                      │
│  Schema: ClipboardItemRecord objects in a single object store     │
│  Indexes: by-alias (unique), by-clippedAt                         │
└───────────────────────────────────────────────────────────────────┘
```

### 4.2 Placement in Component Tree

The `ClipboardProvider` must sit **above** `BrowserRouter` in `src/App.tsx` so
that clipboard state survives playground tab navigation. This mirrors the
existing pattern where `WebSocketProvider` wraps `BrowserRouter` to keep
connections alive across routes (see `src/App.tsx:14–29`).

> **Note:** `ClipboardProvider` has no dependency on `WebSocketContext`. The
> nesting order of `WebSocketProvider` and `ClipboardProvider` is
> interchangeable. The only requirement is that both wrap `BrowserRouter`.

```tsx
// src/App.tsx — proposed structure
<WebSocketProvider>
  <ClipboardProvider>
    <BrowserRouter>
      <Routes>
        {PLAYGROUND_CONFIGS.map((cfg) => (
          <Route key={cfg.path} path={cfg.path} element={<Playground config={cfg} />} />
        ))}
        <Route path="*" element={<Navigate to={defaultPath} replace />} />
      </Routes>
    </BrowserRouter>
  </ClipboardProvider>
</WebSocketProvider>
```

### 4.3 No New Backend Changes

All clipboard operations are implemented client-side. Paste reuses the existing
WebSocket command grammar (`create node`, `update node`, `connect`).
Node existence is checked against local `graphData` (in-memory), not via
WebSocket round-trips. No new Java endpoints or WebSocket message types are
required.

---

## 5. Layer 1 — Storage (IndexedDB)

### 5.1 Why IndexedDB

| Storage API | Persistence | Size Limit | Structured Data | Async | Cross-Tab Reactivity |
|-------------|------------|------------|----------------|-------|----------------------|
| `localStorage` | Yes | ~5–10 MB | No (strings only) | No (blocking) | Via `storage` event (limited) |
| **IndexedDB** | **Yes** | **Hundreds of MB+** | **Yes (structured clones)** | **Yes (Promise-based with `idb`)** | **No (needs BroadcastChannel)** |
| `sessionStorage` | Tab-scoped | ~5 MB | No | No | No |

Nodes can carry large property payloads (nested mappings, multi-line
descriptions, array-valued `mapping` fields). IndexedDB's structured-clone
storage handles these natively without JSON serialisation overhead.

### 5.2 Library Choice: `idb`

The [`idb`](https://github.com/jakearchibald/idb) library (~1.2 KB gzipped)
wraps the callback-based IndexedDB API in Promises. Benefits:

- Type-safe schema via TypeScript generics
- Clean `async`/`await` that composes naturally with React hooks
- Built-in schema migration support via the `upgrade` callback
- Zero runtime dependencies

The raw IndexedDB API is verbose (open request → onsuccess → transaction →
objectStore → request → onsuccess) and error-prone. `idb` eliminates this
boilerplate without adding meaningful bundle weight relative to the existing
dependencies (e.g., `@xyflow/react`, `react-json-view-lite`).

**Installation:**

```bash
npm install idb
```

### 5.3 Database Schema

```typescript
// src/clipboard/db.ts

import { openDB, type DBSchema, type IDBPDatabase } from 'idb';

/**
 * A snapshot of a single node plus its direct connections at the time of
 * clipping. This is the primary record stored in IndexedDB.
 *
 * Nodes are identified by alias (MinigraphNode.alias, src/utils/graphTypes.ts:12).
 * Connections reference nodes by alias via source/target fields
 * (MinigraphConnection.source/target, src/utils/graphTypes.ts:26–27).
 */
export interface ClipboardItemRecord {
  /** Primary key. Generated via crypto.randomUUID() at clip time. */
  id: string;

  /** ISO 8601 timestamp of when the node was clipped. */
  clippedAt: string;

  /**
   * The wsPath of the playground from which the node was clipped.
   * Corresponds to PlaygroundConfig.wsPath (src/config/playgrounds.ts:40).
   * Example: "/ws/graph/playground"
   */
  sourceWsPath: string;

  /**
   * Human-readable label of the source playground.
   * Corresponds to PlaygroundConfig.label (src/config/playgrounds.ts:38).
   * Example: "Minigraph"
   */
  sourceLabel: string;

  /**
   * Full snapshot of the MinigraphNode at clip time.
   * Structure defined in src/utils/graphTypes.ts:10–16.
   */
  node: MinigraphNode;

  /**
   * All direct connections for this node, excluding self-connections.
   * Structure defined in src/utils/graphTypes.ts:25–29.
   *
   * Stored for completeness; during paste, only connections whose counterpart
   * node already exists in the target graph's local graphData are created
   * (Decision #1).
   */
  connections: MinigraphConnection[];
}
```

Where `MinigraphNode` and `MinigraphConnection` are the existing types from
`src/utils/graphTypes.ts`:

```typescript
// src/utils/graphTypes.ts:10–16 (existing, unchanged)
export interface MinigraphNode {
  alias: string;
  types: string[];
  properties: MinigraphNodeProperties;
}

// src/utils/graphTypes.ts:25–29 (existing, unchanged)
export interface MinigraphConnection {
  source: string;
  target: string;
  relations: MinigraphRelation[];
}
```

### 5.4 IndexedDB Database Definition

```typescript
// src/clipboard/db.ts (continued)

import type { MinigraphNode, MinigraphConnection } from '../utils/graphTypes';

const DB_NAME    = 'minigraph-clipboard';
const DB_VERSION = 1;
const STORE_NAME = 'items';

interface ClipboardDB extends DBSchema {
  [STORE_NAME]: {
    key: string;                          // ClipboardItemRecord.id
    value: ClipboardItemRecord;
    indexes: {
      'by-alias':     string;             // node.alias — unique, for duplicate detection
      'by-clippedAt': string;             // clippedAt  — for sorted display
    };
  };
}

/** Singleton database promise. Reused by all callers. */
let dbInstance: Promise<IDBPDatabase<ClipboardDB>> | null = null;

export function getDB(): Promise<IDBPDatabase<ClipboardDB>> {
  if (!dbInstance) {
    dbInstance = openDB<ClipboardDB>(DB_NAME, DB_VERSION, {
      upgrade(db) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'id' });
        store.createIndex('by-alias', 'node.alias', { unique: true });
        store.createIndex('by-clippedAt', 'clippedAt');
      },
    });
  }
  return dbInstance;
}
```

**Design notes:**

- `getDB()` is lazy-initialised and returns the same Promise on subsequent
  calls, avoiding multiple `openDB` calls from concurrent React renders.
- The `by-alias` index is **unique** (`{ unique: true }`). If two tabs race
  to clip the same alias concurrently (before BroadcastChannel delivers the
  notification), the second `db.add()` will throw a `ConstraintError`.
  The `clipNode()` method catches this and returns `{ status: 'duplicate' }`
  with the existing item — see [Section 6.5](#65-provider-implementation-outline).
- The `by-clippedAt` index supports efficient reverse-chronological display
  without loading all records and sorting in memory.
- `DB_VERSION = 1` — the `upgrade` callback handles schema creation. Future
  schema changes increment this version and add migration logic inside the same
  `upgrade` function (an `idb` convention).

### 5.5 Storage Access Functions

```typescript
// src/clipboard/db.ts (continued)

/**
 * Retrieve all clipboard items, sorted newest-first by clippedAt.
 * Uses the by-clippedAt index for efficient ordering.
 */
export async function getAllItems(): Promise<ClipboardItemRecord[]> {
  const db = await getDB();
  const items = await db.getAllFromIndex(STORE_NAME, 'by-clippedAt');
  return items.reverse(); // IDB index is ascending; reverse for newest-first
}

/**
 * Find an existing clipboard item by node alias.
 * Returns the first match or undefined. Used for duplicate detection
 * (Decision #7) before inserting a new clip.
 *
 * Because the by-alias index is unique, at most one result is returned.
 */
export async function findByAlias(alias: string): Promise<ClipboardItemRecord | undefined> {
  const db = await getDB();
  return db.getFromIndex(STORE_NAME, 'by-alias', alias);
}

/**
 * Insert a new clipboard item. Caller is responsible for duplicate
 * checking via findByAlias() before calling this.
 *
 * Throws ConstraintError if a record with the same node.alias already
 * exists (enforced by the unique by-alias index). The caller should
 * catch this and treat it as a duplicate.
 */
export async function addItem(item: ClipboardItemRecord): Promise<void> {
  const db = await getDB();
  await db.add(STORE_NAME, item);
}

/**
 * Replace an existing clipboard item (same id).
 * Used when the user confirms overwrite on a duplicate alias (Decision #7).
 */
export async function putItem(item: ClipboardItemRecord): Promise<void> {
  const db = await getDB();
  await db.put(STORE_NAME, item);
}

/**
 * Atomically remove the old clipboard item and insert a new one in a
 * single IndexedDB transaction. Used by confirmReplace() to ensure
 * that a crash between delete and insert cannot lose both records.
 */
export async function replaceItem(
  previousId: string,
  newItem: ClipboardItemRecord,
): Promise<void> {
  const db = await getDB();
  const tx = db.transaction(STORE_NAME, 'readwrite');
  await tx.store.delete(previousId);
  await tx.store.add(newItem);
  await tx.done;
}

/**
 * Remove a single clipboard item by id.
 */
export async function removeItem(id: string): Promise<void> {
  const db = await getDB();
  await db.delete(STORE_NAME, id);
}

/**
 * Remove all clipboard items.
 */
export async function clearAll(): Promise<void> {
  const db = await getDB();
  await db.clear(STORE_NAME);
}
```

---

## 6. Layer 2 — Reactivity (BroadcastChannel + React Context)

### 6.1 BroadcastChannel Protocol

The `BroadcastChannel` API enables instant, same-origin communication between
browser tabs/windows without polling. When a clipboard mutation occurs in any
tab, the mutating tab writes to IndexedDB **and** posts a message to the
channel. All other tabs receive the message and update their React state
directly — no IndexedDB re-read required.

```typescript
// src/clipboard/channel.ts

import type { ClipboardItemRecord } from './db';

const CHANNEL_NAME = 'minigraph-clipboard-sync';

/**
 * Discriminated union of all messages sent over the clipboard
 * BroadcastChannel. The `type` field is the discriminator.
 */
export type ClipboardChannelMessage =
  | { type: 'item-added';   item: ClipboardItemRecord }
  | { type: 'item-replaced'; item: ClipboardItemRecord; previousId: string }
  | { type: 'item-removed'; id: string }
  | { type: 'items-cleared' };

/**
 * Create a BroadcastChannel for clipboard synchronisation.
 * Each tab creates its own instance; messages sent by a tab are NOT
 * delivered back to that same tab (BroadcastChannel spec behaviour).
 *
 * Returns the channel instance. The caller must close it on unmount.
 */
export function createClipboardChannel(): BroadcastChannel {
  return new BroadcastChannel(CHANNEL_NAME);
}
```

**Why BroadcastChannel over alternatives:**

| Approach | Latency | Complexity | Browser Support |
|----------|---------|------------|-----------------|
| **BroadcastChannel** | **Instant (microtask)** | **Low** | **All modern browsers** |
| `storage` event on localStorage | Instant but string-only, 5 MB limit | Medium (serialise/deserialise) | Universal |
| `SharedWorker` | Instant | High (requires separate worker file, message ports) | Good but not Safari iOS |
| Polling IndexedDB via `setInterval` | 100 ms–1 s lag | Low | Universal |

BroadcastChannel is the best fit: instant delivery, structured message passing
(no serialisation overhead), and zero infrastructure beyond a channel name.

### 6.2 Context State Shape

```typescript
// src/contexts/ClipboardContext.tsx

import type { ClipboardItemRecord } from '../clipboard/db';

/**
 * Reducer state for the clipboard context.
 */
interface ClipboardState {
  /** All clipboard items, sorted newest-first by clippedAt. */
  items: ClipboardItemRecord[];

  /**
   * True during the initial IndexedDB hydration on mount.
   * The sidebar shows a loading skeleton while this is true.
   */
  isLoading: boolean;
}
```

### 6.3 Reducer Actions

```typescript
// src/contexts/ClipboardContext.tsx (continued)

type ClipboardAction =
  | { type: 'HYDRATE';        items: ClipboardItemRecord[] }
  | { type: 'ITEM_ADDED';     item: ClipboardItemRecord }
  | { type: 'ITEM_REPLACED';  item: ClipboardItemRecord; previousId: string }
  | { type: 'ITEM_REMOVED';   id: string }
  | { type: 'ITEMS_CLEARED' };

function clipboardReducer(state: ClipboardState, action: ClipboardAction): ClipboardState {
  switch (action.type) {
    case 'HYDRATE':
      return { items: action.items, isLoading: false };

    case 'ITEM_ADDED':
      // Prepend (newest-first)
      return { ...state, items: [action.item, ...state.items] };

    case 'ITEM_REPLACED': {
      // Remove the previous entry, prepend the replacement
      const filtered = state.items.filter(i => i.id !== action.previousId);
      return { ...state, items: [action.item, ...filtered] };
    }

    case 'ITEM_REMOVED':
      return { ...state, items: state.items.filter(i => i.id !== action.id) };

    case 'ITEMS_CLEARED':
      return { ...state, items: [] };

    default:
      return state;
  }
}
```

### 6.4 Context Value Interface

```typescript
// src/contexts/ClipboardContext.tsx (continued)

import type { MinigraphNode, MinigraphConnection } from '../utils/graphTypes';

/**
 * Metadata provided by the caller when clipping a node.
 * Avoids coupling the clipboard layer to PlaygroundConfig directly.
 */
export interface ClipMeta {
  /** PlaygroundConfig.wsPath of the source playground. */
  sourceWsPath: string;
  /** PlaygroundConfig.label of the source playground. */
  sourceLabel: string;
}

/**
 * Result of a clipNode() call. The UI layer uses this to decide whether
 * to show a confirmation dialog (when a duplicate exists).
 */
export type ClipResult =
  | { status: 'added' }
  | { status: 'duplicate'; existingItem: ClipboardItemRecord; pendingItem: ClipboardItemRecord }
  | { status: 'error'; message: string };

/**
 * Public API of the ClipboardContext.
 */
export interface ClipboardContextValue {
  /** All clipboard items, newest-first. */
  items: ClipboardItemRecord[];

  /** True during initial IndexedDB hydration. */
  isLoading: boolean;

  /**
   * Clip a node (plus its direct connections) to the clipboard.
   *
   * If a clipboard item with the same alias already exists, returns
   * { status: 'duplicate', existingItem, pendingItem } so the UI can
   * prompt the user to confirm replacement (Decision #7).
   *
   * @param node        The MinigraphNode to clip (src/utils/graphTypes.ts:10–16).
   * @param connections Direct connections for this node (src/utils/graphTypes.ts:25–29).
   *                    Self-connections are excluded by the caller.
   * @param meta        Source playground metadata.
   */
  clipNode(
    node: MinigraphNode,
    connections: MinigraphConnection[],
    meta: ClipMeta,
  ): Promise<ClipResult>;

  /**
   * Confirm replacement of a duplicate clipboard item.
   * Called after the user accepts the duplicate-warning dialog.
   *
   * The IndexedDB delete + insert is performed in a single transaction
   * to prevent data loss on crash.
   *
   * @param pendingItem  The ClipboardItemRecord built by clipNode() and returned
   *                     in the 'duplicate' result.
   * @param previousId   The id of the existing item being replaced.
   */
  confirmReplace(pendingItem: ClipboardItemRecord, previousId: string): Promise<void>;

  /**
   * Remove a single clipboard item by its id.
   */
  removeItem(id: string): Promise<void>;

  /**
   * Remove all clipboard items.
   */
  clearAll(): Promise<void>;
}
```

### 6.5 Provider Implementation Outline

```typescript
// src/contexts/ClipboardContext.tsx (continued)

import { createContext, useCallback, useContext, useEffect, useReducer, useRef, type ReactNode } from 'react';
import * as db from '../clipboard/db';
import { createClipboardChannel, type ClipboardChannelMessage } from '../clipboard/channel';

const ClipboardContext = createContext<ClipboardContextValue | null>(null);

export function ClipboardProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(clipboardReducer, { items: [], isLoading: true });
  const channelRef = useRef<BroadcastChannel | null>(null);

  // ── Hydrate from IndexedDB on mount ─────────────────────────────────────
  useEffect(() => {
    db.getAllItems().then(items => dispatch({ type: 'HYDRATE', items }));
  }, []);

  // ── BroadcastChannel setup ──────────────────────────────────────────────
  useEffect(() => {
    let channel: BroadcastChannel;
    try {
      channel = createClipboardChannel();
    } catch {
      // BroadcastChannel unavailable (e.g. embedded WebView).
      // Clipboard still works within a single tab; cross-tab sync is disabled.
      return;
    }
    channelRef.current = channel;

    channel.onmessage = (event: MessageEvent<ClipboardChannelMessage>) => {
      const msg = event.data;
      switch (msg.type) {
        case 'item-added':
          dispatch({ type: 'ITEM_ADDED', item: msg.item });
          break;
        case 'item-replaced':
          dispatch({ type: 'ITEM_REPLACED', item: msg.item, previousId: msg.previousId });
          break;
        case 'item-removed':
          dispatch({ type: 'ITEM_REMOVED', id: msg.id });
          break;
        case 'items-cleared':
          dispatch({ type: 'ITEMS_CLEARED' });
          break;
      }
    };

    return () => { channel.close(); channelRef.current = null; };
  }, []);

  /** Post to BroadcastChannel (other tabs only — same tab uses dispatch). */
  const broadcast = useCallback((msg: ClipboardChannelMessage) => {
    channelRef.current?.postMessage(msg);
  }, []);

  // ── clipNode ────────────────────────────────────────────────────────────
  const clipNode = useCallback(async (
    node: MinigraphNode,
    connections: MinigraphConnection[],
    meta: ClipMeta,
  ): Promise<ClipResult> => {
    try {
      const pendingItem: db.ClipboardItemRecord = {
        id: crypto.randomUUID(),
        clippedAt: new Date().toISOString(),
        sourceWsPath: meta.sourceWsPath,
        sourceLabel: meta.sourceLabel,
        node,
        connections,
      };

      // Duplicate detection (Decision #7)
      const existing = await db.findByAlias(node.alias);
      if (existing) {
        return { status: 'duplicate', existingItem: existing, pendingItem };
      }

      try {
        await db.addItem(pendingItem);
      } catch (err) {
        // Handle race condition: another tab clipped the same alias between
        // our findByAlias check and this addItem call. The unique by-alias
        // index enforces the constraint and throws ConstraintError.
        if (err instanceof DOMException && err.name === 'ConstraintError') {
          const raceExisting = await db.findByAlias(node.alias);
          if (raceExisting) {
            return { status: 'duplicate', existingItem: raceExisting, pendingItem };
          }
        }
        throw err; // Re-throw unexpected errors
      }

      dispatch({ type: 'ITEM_ADDED', item: pendingItem });
      broadcast({ type: 'item-added', item: pendingItem });
      return { status: 'added' };
    } catch (err) {
      return { status: 'error', message: err instanceof Error ? err.message : String(err) };
    }
  }, [broadcast]);

  // ── confirmReplace ──────────────────────────────────────────────────────
  const confirmReplace = useCallback(async (
    pendingItem: db.ClipboardItemRecord,
    previousId: string,
  ): Promise<void> => {
    // Atomic: single IndexedDB transaction prevents data loss on crash.
    await db.replaceItem(previousId, pendingItem);
    dispatch({ type: 'ITEM_REPLACED', item: pendingItem, previousId });
    broadcast({ type: 'item-replaced', item: pendingItem, previousId });
  }, [broadcast]);

  // ── removeItem ──────────────────────────────────────────────────────────
  const removeItem = useCallback(async (id: string): Promise<void> => {
    await db.removeItem(id);
    dispatch({ type: 'ITEM_REMOVED', id });
    broadcast({ type: 'item-removed', id });
  }, [broadcast]);

  // ── clearAll ────────────────────────────────────────────────────────────
  const clearAll = useCallback(async (): Promise<void> => {
    await db.clearAll();
    dispatch({ type: 'ITEMS_CLEARED' });
    broadcast({ type: 'items-cleared' });
  }, [broadcast]);

  return (
    <ClipboardContext.Provider value={{
      items: state.items,
      isLoading: state.isLoading,
      clipNode,
      confirmReplace,
      removeItem,
      clearAll,
    }}>
      {children}
    </ClipboardContext.Provider>
  );
}

/**
 * Consumer hook. Must be called inside <ClipboardProvider>.
 */
export function useClipboardContext(): ClipboardContextValue {
  const ctx = useContext(ClipboardContext);
  if (!ctx) throw new Error('useClipboardContext must be used inside <ClipboardProvider>');
  return ctx;
}
```

### 6.6 Why Context + useReducer (Not Zustand/Jotai)

The existing codebase uses React Context + hooks throughout:
`WebSocketContext` (`src/contexts/WebSocketContext.tsx`), `useLocalStorage`
(`src/hooks/useLocalStorage.ts`), and per-hook state via `useReducer`
(`src/hooks/useWebSocket.ts:75–95`). Adding an external state library for
the clipboard alone would create an architectural inconsistency.

The clipboard state is a flat list with four mutations (add, replace, remove,
clear) — well within the complexity threshold where Context + useReducer
performs efficiently. If the app later grows to require more complex
cross-cutting state management, all Contexts could migrate to an external
store at that point.

---

## 7. Layer 3 — UI (ClipboardSidebar + Integration Points)

### 7.1 Sidebar Placement Decision

Three layout options were evaluated:

| Option | Description | Pros | Cons |
|--------|-------------|------|------|
| **A. Independent right sidebar** | Sits outside `RightPanel`, toggled independently | Coexists with graph/data tabs; can be wider | Adds layout complexity |
| B. New tab in `RightPanel` | Added to `PlaygroundConfig.tabs` alongside `'graph'`, `'graph-data'`, etc. | Consistent with existing tab pattern | Can't view clipboard and graph simultaneously |
| C. Slide-over overlay | Overlays from right edge | Doesn't displace content | Occludes graph view; transient feel |

**Selected: Option A.** The user needs to see both the graph (to right-click
nodes) and the clipboard (to manage/paste items) simultaneously. A tab inside
`RightPanel` would force switching back and forth. An independent sidebar can
be toggled open/closed and sized independently of the existing panel split.

### 7.2 Layout Structure

The `Playground` component (`src/components/Playground.tsx:34`) currently uses
`react-resizable-panels` with a two-panel `<Group>`:

```
┌─────────────────────────────────────────────────────────────┐
│  Header (title + actions + nav)                             │
├──────────────────────────┬──────────────────────────────────┤
│  LeftPanel (console)     │  RightPanel (tabs)               │
│  Panel defaultSize="60%" │  Panel defaultSize="40%"         │
│                          │                                  │
└──────────────────────────┴──────────────────────────────────┘
```

The proposed layout adds the clipboard sidebar as a conditionally rendered
element **to the right of the existing `<Group>`**, outside the resizable
panel system:

```
┌──────────────────────────────────────────────────────────────────────┐
│  Header (title + actions + nav + [clipboard toggle button])         │
├──────────────────────────┬──────────────────────┬───────────────────┤
│  LeftPanel (console)     │  RightPanel (tabs)   │ ClipboardSidebar  │
│  Panel defaultSize="60%" │  Panel defaultSize=  │ width: 320px      │
│                          │  "40%"               │ (collapsible)     │
│                          │                      │                   │
└──────────────────────────┴──────────────────────┴───────────────────┘
```

When the sidebar is collapsed, the `<Group>` fills the full width (current
behaviour). When expanded, the sidebar takes a fixed width (320 px default)
and the `<Group>` shrinks by the same amount via `calc(100% - 320px)`.

**CSS approach:** The `.wrapper` class in `src/components/Playground.module.css`
already uses `display: flex; flex-direction: column`. The panel area below
the header currently contains only the `<Group>`. Wrap the `<Group>` and
`ClipboardSidebar` in a horizontal flex container:

```css
/* src/components/Playground.module.css — new rules */

.mainArea {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  flex-direction: row;
}

.mainArea .panelGroup {
  flex: 1 1 0;
  min-width: 0;
}
```

### 7.3 Sidebar Toggle

A toggle button is added to `header > .headerActions` in `Playground.tsx`,
next to the existing `GraphSaveButton` and `SavedGraphsMenu`. The button
is only rendered when `config.supportsClipboard` is `true` (see
[Section 10](#10-playgroundconfig-changes)).

```
[Save] [Saved Graphs ▾] [Clipboard] [Nav dots]
```

The open/closed state is persisted in `localStorage` under the key
`'clipboard-sidebar-open'` via the existing `useLocalStorage` hook
(`src/hooks/useLocalStorage.ts`) so the sidebar remembers its state across
refreshes.

### 7.4 ClipboardSidebar Component

```
src/components/ClipboardSidebar/
├── ClipboardSidebar.tsx
├── ClipboardSidebar.module.css
├── ClipboardItem.tsx
├── ClipboardItem.module.css
├── ClipboardEmptyState.tsx
└── ClipboardDuplicateDialog.tsx
```

#### 7.4.1 ClipboardSidebar Props

The sidebar is **always mounted** when `config.supportsClipboard` is true, and
uses a CSS class to control visibility (slide-in/slide-out transition). This
avoids remounting on toggle and allows a smooth animation.

```typescript
// src/components/ClipboardSidebar/ClipboardSidebar.tsx

interface ClipboardSidebarProps {
  /**
   * Controls the CSS class for slide-in/slide-out transitions.
   * When false, the sidebar is rendered but visually collapsed (width: 0,
   * overflow: hidden). When true, it expands to its full 320px width.
   */
  open: boolean;

  /**
   * Whether the active playground has a live WebSocket connection.
   * When false, "Paste" buttons are disabled.
   * Mirrors `ws.connected` from useWebSocket (src/hooks/useWebSocket.ts:45).
   */
  connected: boolean;

  /**
   * True while a paste operation is in progress. Disables all "Paste"
   * buttons to prevent concurrent paste operations.
   */
  isPasting: boolean;

  /**
   * Callback to execute the paste operation.
   * Defined in Playground.tsx; delegates to the paste orchestration hook.
   */
  onPaste: (item: ClipboardItemRecord) => void;
}
```

#### 7.4.2 Sidebar Visual Layout

```
┌──────────────────────────────────────────┐
│ Clipboard                       [Clear]  │
├──────────────────────────────────────────┤
│ ┌──────────────────────────────────────┐ │
│ │  fetcher_node                        │ │
│ │  Type: api_fetcher                   │ │
│ │  Skill: graph.api.fetcher            │ │
│ │  Props: dictionary=..., provider=... │ │
│ │  Connections: 2 (1 out, 1 in)        │ │
│ │  Clipped 2 min ago from Minigraph    │ │
│ │                                      │ │
│ │  [Paste]  [Inspect]  [Remove]        │ │
│ └──────────────────────────────────────┘ │
│ ┌──────────────────────────────────────┐ │
│ │  entry_point                         │ │
│ │  Type: entry_point                   │ │
│ │  Skill: —                            │ │
│ │  Props: name=my-graph                │ │
│ │  Connections: 1 (1 out, 0 in)        │ │
│ │  Clipped 5 min ago from Minigraph    │ │
│ │                                      │ │
│ │  [Paste]  [Inspect]  [Remove]        │ │
│ └──────────────────────────────────────┘ │
│                                          │
│      (scrollable list continues...)      │
└──────────────────────────────────────────┘
```

**Display rules:**

- **Alias** is the primary label (bold, top of card).
- **Type** shows `node.types[0]` (the first type label, consistent with how
  `graphTransformer.ts:170` selects the primary type).
- **Skill** shows `node.properties.skill` or "—" if absent.
- **Props** shows a truncated one-line summary of remaining properties. Keys
  are listed comma-separated; values longer than 30 characters are replaced
  with `...`. Uses CSS `text-overflow: ellipsis` for the line.
- **Connections** shows a count summary: total, outgoing (where node is
  `source`), incoming (where node is `target`).
- **Clipped timestamp** uses relative time formatting ("2 min ago", "1 hour
  ago", "yesterday"). A utility function `formatRelativeTime(isoString: string):
  string` should be added to `src/utils/timeFormat.ts`.

  > **Note:** Relative timestamps are computed at render time and do not
  > auto-update. If the sidebar stays open for extended periods, timestamps
  > may appear stale (e.g., "2 min ago" may actually be 30 min ago). This is
  > accepted as a minor cosmetic issue. A future enhancement could add a
  > `setInterval` refresh — see [Section 15](#15-future-enhancements).

- **Source label** shows `sourceLabel` ("from Minigraph").

**Action buttons per item:**

| Button | Behaviour | Disabled when |
|--------|-----------|---------------|
| **Paste** | Calls `onPaste(item)` — see [Section 9](#9-paste-execution-strategy) | `connected` is `false` OR `isPasting` is `true` |
| **Inspect** | Opens a modal (or expands inline) showing the full JSON of `item.node` and `item.connections` using `react-json-view-lite` (already a dependency, used in `GraphDataView`, `src/components/GraphDataView/GraphDataView.tsx:2`) | Never |
| **Remove** | Calls `clipboardCtx.removeItem(item.id)` | Never |

#### 7.4.3 ClipboardDuplicateDialog

When `clipNode()` returns `{ status: 'duplicate' }`, a confirmation dialog is
shown:

```
┌────────────────────────────────────────────────┐
│  Duplicate Node                                │
│                                                │
│  A clipboard item with alias "fetcher_node"    │
│  already exists (clipped 10 min ago).          │
│                                                │
│  Replace it with the new snapshot?             │
│                                                │
│              [Cancel]  [Replace]               │
└────────────────────────────────────────────────┘
```

On **Replace**: calls `clipboardCtx.confirmReplace(pendingItem, existingItem.id)`.  
On **Cancel**: no action; the pending item is discarded.

This dialog follows the same pattern as `MockUploadModal`
(`src/components/MockUploadModal/MockUploadModal.tsx`): rendered conditionally
in `Playground.tsx`, with focus management via `modalTriggerRef` (see
`src/components/Playground.tsx:138`).

#### 7.4.4 ClipboardEmptyState

When `items.length === 0` and `isLoading` is `false`:

```
┌──────────────────────────────────────────┐
│ Clipboard                                │
├──────────────────────────────────────────┤
│                                          │
│           (clipboard icon)               │
│     No items clipped yet.                │
│                                          │
│  Right-click a node in the Graph view    │
│  to get started.                         │
│                                          │
└──────────────────────────────────────────┘
```

Consistent with the empty-state pattern used in `GraphDataView`
(`src/components/GraphDataView/GraphDataView.tsx:43–53`) and `GraphView`
(`src/components/GraphView/GraphView.tsx:92–99`).

---

## 8. Clip Operations

### 8.1 Clip Trigger: Right-Click Context Menu on ReactFlow Node

`GraphView` (`src/components/GraphView/GraphView.tsx`) renders a `<ReactFlow>`
component at line 115. ReactFlow supports a
[`onNodeContextMenu`](https://reactflow.dev/api-reference/react-flow#on-node-context-menu)
callback that fires when a node receives a right-click event.

**Implementation outline:**

1. Add an `onNodeContextMenu` handler to the `<ReactFlow>` component in
   `GraphView.tsx`.
2. The handler receives `(event: React.MouseEvent, node: Node<GraphNodeData>)`.
   It calls `event.preventDefault()` to suppress the browser's default context
   menu, then sets state to position a custom context menu at the cursor's
   `clientX`/`clientY`.
3. The context menu renders a single action: "Clip to Clipboard".
4. On click, the handler resolves the full `MinigraphNode` from `graphData`
   (passed as a prop) by matching `node.data.alias`, extracts its direct
   connections (excluding self-connections), and calls the `onClipNode` callback.

**Props change to GraphView:**

```typescript
// src/components/GraphView/GraphView.tsx — updated interface

interface GraphViewProps {
  graphData:       MinigraphGraphData | null;
  onCopySuccess?:  () => void;
  onCopyError?:    () => void;
  onRenderError?:  (message: string) => void;
  isRefreshing?:   boolean;
  /**
   * Called when the user right-clicks a node and selects "Clip to Clipboard".
   * GraphView resolves the MinigraphNode and its direct connections from
   * graphData, then invokes this callback. Playground.tsx wires this to
   * the ClipboardContext.
   */
  onClipNode?: (node: MinigraphNode, connections: MinigraphConnection[]) => void;
}
```

**Context menu accessibility and dismissal:**

The context menu must be accessible and properly dismissable:

- **Keyboard trigger:** When a ReactFlow node has focus, `Shift+F10` opens
  the context menu positioned at the node's center.
- **Focus management:** When the menu opens, focus moves to the first menu
  item ("Clip to Clipboard"). When the menu closes (via action, Escape, or
  outside click), focus returns to the triggering node.
- **Escape key:** Dismisses the context menu without action.
- **Outside click:** A `useEffect` with a global `mousedown` listener
  dismisses the menu when the click target is outside the menu element.
- **Canvas interaction:** `onPaneClick` (already available on `<ReactFlow>`)
  dismisses the menu on canvas clicks.
- **ARIA attributes:** The menu container uses `role="menu"` and each action
  uses `role="menuitem"`.

**Helper: extracting direct connections for a node:**

```typescript
// src/clipboard/helpers.ts

import type { MinigraphNode, MinigraphConnection, MinigraphGraphData } from '../utils/graphTypes';

/**
 * Extract the full MinigraphNode from graphData by alias.
 * Returns undefined if not found.
 */
export function findNodeByAlias(
  graphData: MinigraphGraphData,
  alias: string,
): MinigraphNode | undefined {
  return graphData.nodes.find(n => n.alias === alias);
}

/**
 * Extract all connections where the given alias appears as source OR target,
 * excluding self-connections (where source === target).
 *
 * Self-connections are excluded because the backend rejects
 * `connect {alias} to {alias} with {relation}` with the error
 * "source and target node names cannot be the same"
 * (GraphLambdaFunction.java:106, constant SAME_SOURCE_TARGET).
 *
 * The server may omit `connections` on partially-built graphs
 * (the TypeScript type declares it as required, but the
 * isMinigraphGraphData type guard at src/utils/graphTypes.ts:42–46 only
 * checks for `nodes`). The `?? []` fallback ensures a clean empty array.
 *
 * Uses MinigraphConnection.source and MinigraphConnection.target
 * (src/utils/graphTypes.ts:26–27).
 */
export function extractDirectConnections(
  graphData: MinigraphGraphData,
  alias: string,
): MinigraphConnection[] {
  return (graphData.connections ?? []).filter(
    c => c.source !== c.target && (c.source === alias || c.target === alias),
  );
}
```

### 8.2 Duplicate Handling Flow

When `clipNode()` detects a duplicate (same alias already in clipboard):

```
clipNode() called
  ↓
db.findByAlias(alias) → existing item found
  ↓
Return { status: 'duplicate', existingItem, pendingItem }
  ↓
Playground.tsx receives the result
  ↓
Opens ClipboardDuplicateDialog
  ├─ User clicks "Replace"
  │   → clipboardCtx.confirmReplace(pendingItem, existingItem.id)
  │   → IndexedDB updated atomically (single transaction), dispatch, broadcast
  │   → Toast: 'Clipboard item "fetcher_node" replaced'
  │
  └─ User clicks "Cancel"
      → No action; pendingItem is discarded
      → Toast: 'Clip cancelled'
```

---

## 9. Paste Execution Strategy

### 9.1 Approach: Client-Constructed WebSocket Commands

Paste is implemented entirely client-side by constructing and sending
standard WebSocket commands through the existing `ctx.send()` path
(`src/contexts/WebSocketContext.tsx:318–325`). Node existence and connection
counterpart existence are checked against the **local `graphData`** (in-memory)
rather than via WebSocket round-trips. No new backend endpoints or
WebSocket message types are needed.

**Why this approach:**

- Zero backend changes required.
- All mutations are visible in the console — the user sees exactly what
  happened (discoverability, debuggability).
- All mutations flow through the same code path as manual commands, so
  `useAutoGraphRefresh` (`src/hooks/useAutoGraphRefresh.ts`) automatically
  refreshes the graph view.
- The server's response messages ("Node X created", "Node X updated")
  are classified by `classifyMessage()` (`src/protocol/classifier.ts:27`)
  as `graph.mutation` events, which trigger the existing auto-refresh
  pipeline.

**Why local `graphData` checks instead of WebSocket `describe node` commands:**

Using `describe node {alias}` for existence checks would require N+1 WebSocket
round-trips (1 for the target node + N for each connection counterpart) and
complex response correlation on a shared `ProtocolBus` where other subscribers
(`useAutoGraphRefresh`, `useAutoMarkdownPin`, `useLargePayloadDownload`) also
listen. Local checks against `graphData.nodes` are instant, zero-latency, and
require no bus coordination.

> **Staleness caveat:** `graphData` reflects the most recent `describe graph`
> fetch. If another tab created a node after the last fetch, the local check
> may incorrectly conclude the node doesn't exist. In the worst case this
> means `create node` is sent for a node that was just created by another tab,
> which the server rejects with "Node {alias} already exists" — handled by
> the fallback in [Section 9.6](#96-paste-error-handling). For connection
> counterparts, staleness means some connections may be skipped; the user can
> simply re-paste after the graph refreshes.

### 9.2 Paste Orchestration Hook

A new hook `usePasteNode` encapsulates the paste flow.

```typescript
// src/hooks/usePasteNode.ts

import type { ClipboardItemRecord } from '../clipboard/db';
import type { MinigraphGraphData } from '../utils/graphTypes';
import type { ToastType } from './useToast';
import type { ProtocolBus } from '../protocol/bus';

export interface UsePasteNodeOptions {
  /** The wsPath of the active playground's WebSocket. */
  wsPath: string;
  /** Whether the WebSocket is connected. */
  connected: boolean;
  /** Send a raw string over the WebSocket. */
  sendRawText: (text: string) => void;
  /** Append a local-only message to the console. */
  appendMessage: (raw: string) => void;
  /** Toast notification callback. */
  addToast: (message: string, type?: ToastType) => void;
  /**
   * Reference to the ProtocolBus so the hook can subscribe to the
   * graph.mutation event confirming node creation/update.
   */
  bus: ProtocolBus;
  /**
   * The current graph data from the target playground. Used for local
   * existence checks (node and connection counterparts) without
   * WebSocket round-trips.
   */
  graphData: MinigraphGraphData | null;
}

export interface UsePasteNodeReturn {
  /**
   * Initiate the paste flow for a clipboard item.
   * Returns a Promise that resolves when the paste sequence completes.
   */
  pasteNode: (item: ClipboardItemRecord) => Promise<void>;

  /** True while a paste operation is in flight. Disables additional paste clicks. */
  isPasting: boolean;
}
```

### 9.3 Paste Flow: Step by Step

Given a `ClipboardItemRecord` with `node` and `connections`, and access to the
target playground's local `graphData`:

```
Step 1: Check existence (local)
  → Search graphData.nodes for an entry where alias === item.node.alias
  → If not found → Step 2a (create)
  → If found    → Step 2b (update)

Step 2a: Create node
  → Build multi-line command via buildNodeCommand('create', item.node)
  → sendRawText(createCommand)
  → Wait for graph.mutation event on the ProtocolBus (with step timeout)
  → If server responds with "Node {alias} already exists" instead:
      → Fall back to update: sendRawText(buildNodeCommand('update', item.node))
      → Wait for graph.mutation event (with step timeout)
  → Proceed to Step 3

Step 2b: Update node
  → Build multi-line command via buildNodeCommand('update', item.node)
  → sendRawText(updateCommand)
  → Wait for graph.mutation event on the ProtocolBus (with step timeout)
  → If server responds with "Node {alias} not found" instead:
      → Fall back to create: sendRawText(buildNodeCommand('create', item.node))
      → Wait for graph.mutation event (with step timeout)
  → Proceed to Step 3

Step 3: Create connections (local counterpart check)
  → For each connection in item.connections:
      → Determine counterpart alias (the other end of the connection)
      → Check if counterpart exists in graphData.nodes (local, instant)
      → If counterpart exists:
          → For each relation in connection.relations:
              → sendRawText(buildConnectCommand(
                    connection.source, connection.target, relation.type))
      → If counterpart not found:
          → Skip; count skipped connections and record counterpart alias
  → Proceed to Step 4

Step 4: Report
  → Toast: "Pasted node '{alias}' — {N} of {M} connections sent"
  → If any connections were skipped:
      → appendMessage("Clipboard paste: skipped {K} connection(s)
         (nodes not found in local graph: {list of aliases}).
         Re-paste after graph refresh to retry.")
  → useAutoGraphRefresh handles the graph view update automatically

Note: The connection count in the toast is optimistic — it reflects the
number of `connect` commands sent, not confirmed. Because counterpart
existence is verified locally before sending, failures are rare (limited
to transient server errors).
```

**Step timeout:** Each step that waits for a `ProtocolBus` event uses a
configurable timeout (default: 10 seconds). If the timeout expires, the paste
is aborted with a toast: "Paste timed out waiting for server response. Some
changes may have been applied."

**Response correlation for Steps 2a/2b:** The paste hook subscribes to
`graph.mutation` events on the bus. Because `isPasting` is true, the hook
knows the next `graph.mutation` with `mutationType: 'node-mutation'` is the
response to its command. To handle the race-condition fallbacks, the hook also
monitors `docs.response` events:
- **Step 2a:** Text containing `"already exists"` triggers the update fallback.
- **Step 2b:** Text starting with `"Node "` and containing `" not found"` triggers the create fallback.
See [Section 9.6](#96-paste-error-handling) for the full error-handling table.

> **Note about `useAutoGraphRefresh`:** The `graph.mutation` events from paste
> commands also trigger `useAutoGraphRefresh`, which debounces and sends
> `describe graph`. This is desirable — it refreshes `graphData` after paste,
> improving the accuracy of subsequent paste operations. The paste hook does
> not need to suppress auto-refresh; both can coexist.

### 9.4 Command Construction

The backend's command grammar is parsed in `GraphCommandService.java`. The
key patterns (confirmed from source):

**Create node:**
```
create node {alias}
with type {type}
with properties
{key1}={value1}
{key2}={value2}
```

**Update node:**
```
update node {alias}
with type {type}
with properties
{key1}={value1}
{key2}={value2}
```

**Connect:**
```
connect {source} to {target} with {relation}
```

**Command builder:**

```typescript
// src/clipboard/commandBuilder.ts

import type { MinigraphNode } from '../utils/graphTypes';

/**
 * Serialise a property value for the "key=value" line in a create/update
 * command. Multiline values are wrapped in triple-quotes (''').
 *
 * The backend parses these in GraphCommandService.getNodeProperties()
 * using the TRIPLE_QUOTE = "'''" delimiter defined in
 * GraphLambdaFunction.java.
 *
 * Known limitation: if a property value contains the literal string "'''"
 * (triple single-quotes), the backend will interpret it as the closing
 * delimiter. The backend grammar has no escape mechanism for this.
 * See Section 13.4 (Known Limitations) for details.
 */
function serializePropertyValue(value: unknown): string {
  if (value === null || value === undefined) return '';

  const str = typeof value === 'string' ? value : JSON.stringify(value);

  // Warn if the value contains triple-quote — the backend has no escape
  // mechanism, so the value will be truncated at the first embedded '''.
  if (str.includes("'''")) {
    console.warn(
      `[commandBuilder] Property value contains "'''" which cannot be escaped ` +
      `in the backend grammar. The value may be truncated on paste.`
    );
  }

  // Only wrap in triple-quotes for multiline values.
  // Single-line values containing ''' are left unquoted to preserve as much
  // of the value as possible — wrapping would cause a larger truncation
  // because the opening ''' introduces a newline before the embedded '''.
  if (str.includes('\n')) {
    return `'''\n${str}\n'''`;
  }

  return str;
}

/**
 * Build a create or update node multi-line command string from a MinigraphNode.
 *
 * The verb parameter selects the command:
 *   - 'create' → `create node {alias}` (GraphCommandService.handleCreateNode())
 *   - 'update' → `update node {alias}` (GraphCommandService.handleUpdateNode())
 *
 * Both share the same grammar; only the leading verb differs.
 *
 * Known limitation: only node.types[0] is emitted. The backend's `with type`
 * clause accepts a single type. Nodes with multiple types will have all types
 * beyond the first silently dropped. See Section 13.4 (Known Limitations).
 *
 * @param verb 'create' or 'update'
 * @param node The node whose data populates the command.
 */
export function buildNodeCommand(
  verb: 'create' | 'update',
  node: MinigraphNode,
): string {
  const lines: string[] = [`${verb} node ${node.alias}`];

  if (node.types.length > 0) {
    lines.push(`with type ${node.types[0]}`);
  }

  const propEntries = Object.entries(node.properties).filter(
    ([, v]) => v !== undefined && v !== null,
  );

  if (propEntries.length > 0) {
    lines.push('with properties');
    for (const [key, value] of propEntries) {
      lines.push(`${key}=${serializePropertyValue(value)}`);
    }
  }

  return lines.join('\n');
}

/**
 * Build a `connect` command for a single relation.
 *
 * Follows the grammar parsed by GraphCommandService.handleConnectCommand():
 *   connect {source} to {target} with {relation}
 *
 * The backend accepts one relation per connect command. For connections with
 * multiple relations, call this function once per relation.
 *
 * Known limitation: MinigraphRelation.properties (src/utils/graphTypes.ts:22)
 * are not included. The backend's `connect` grammar has no syntax for
 * relation properties. See Section 13.4 (Known Limitations).
 */
export function buildConnectCommand(
  source: string,
  target: string,
  relationType: string,
): string {
  return `connect ${source} to ${target} with ${relationType}`;
}
```

### 9.5 Response Correlation During Paste

The paste hook needs to detect the server's confirmation of `create node` and
`update node` commands to advance through its steps. The approach uses the
`ProtocolBus` (`src/protocol/bus.ts`).

**What the paste hook listens for:**

| Paste step | Expected server response | Bus event kind | Detection |
|------------|------------------------|----------------|-----------|
| Create node | `"Node {alias} created"` | `graph.mutation` (`node-mutation`) | `detectMutation()` matches — `src/utils/messageParser.ts:296` |
| Create node (race) | `"Node {alias} already exists"` | `docs.response` | Raw text contains `" already exists"` — triggers update fallback |
| Update node | `"Node {alias} updated"` | `graph.mutation` (`node-mutation`) | `detectMutation()` matches — `src/utils/messageParser.ts:297` |
| Update node (race) | `"Node {alias} not found"` | `docs.response` | Raw text starts with `"Node "` and contains `" not found"` — triggers create fallback |
| Connect | `"Node {source} connected to {target}"` | `graph.mutation` (`node-mutation`) | `detectMutation()` matches — `src/utils/messageParser.ts:299` |

**Correlation mechanism:** The paste hook enters a "waiting" state after
sending each command. It subscribes to the relevant bus event kind and
advances when the expected event arrives. A timeout (10 seconds per step)
prevents indefinite stalls if the server doesn't respond.

**Interaction with other bus subscribers:** `useAutoGraphRefresh` also listens
for `graph.mutation`. Both subscribers receive every event (the bus dispatches
to all listeners). This is harmless: `useAutoGraphRefresh` sends `describe
graph` on mutation, which refreshes the graph — a desirable side effect.
The paste hook's "waiting" flag ensures it consumes only the events it expects.

### 9.6 Paste Error Handling

| Scenario | Behaviour |
|----------|-----------|
| Node doesn't exist locally | `create node` command sent |
| Node exists locally with different properties | `update node` command sent (replaces all properties — this matches backend behaviour) |
| Node exists locally with identical properties | `update node` command still sent (server overwrites with same values; simpler than diffing) |
| `create node` returns "already exists" (race) | Fallback: send `update node` command instead. This handles the race window where another tab or user creates the same node between the local check and the `create node` command. |
| `update node` returns "not found" (race) | Fallback: send `create node` command instead. This handles the symmetric race where another tab deletes the node between the local check and the `update node` command. |
| Connection already exists on server | `connect` command sent; server silently succeeds (adding a duplicate relation is idempotent in the backend) |
| Counterpart node doesn't exist locally | Connection skipped; counted and reported in console message |
| WebSocket disconnects mid-paste | Partial paste. Already-sent commands are processed server-side. Toast: "Paste interrupted — WebSocket disconnected. Some changes may have been applied." |
| Step timeout (10 seconds) | Paste aborted. Toast: "Paste timed out waiting for server response. Some changes may have been applied." |
| Multiple rapid paste clicks | `isPasting` flag disables all Paste buttons until the current sequence completes |
| **Re-paste after interruption** | **Safe.** If the node was created in the first attempt, the local check now finds it → `update node` is sent (replaces with same data). Connections are idempotent. The user can simply click Paste again. |

---

## 10. PlaygroundConfig Changes

A new optional boolean field `supportsClipboard` is added to
`PlaygroundConfig` (`src/config/playgrounds.ts:36–58`):

```typescript
// src/config/playgrounds.ts — updated interface

export interface PlaygroundConfig {
  path:                   string;
  label:                  string;
  title:                  string;
  wsPath:                 string;
  storageKeyPayload:      string;
  storageKeyHistory:      string;
  storageKeyTab:          string;
  storageKeySavedGraphs?: string;
  supportsUpload?:        boolean;
  /**
   * When true, the clipboard sidebar toggle is shown in the header and
   * the clip/paste features are enabled for this playground.
   *
   * Clipboard features are scoped to Minigraph-type playgrounds only
   * (Decision #4). Playgrounds that do not set this flag — or set it
   * to false — hide the toggle button and ignore clipboard operations.
   */
  supportsClipboard?:     boolean;
  tabs:                   RightTab[];
}
```

**Updated config entries:**

```typescript
// src/config/playgrounds.ts — PLAYGROUND_CONFIGS

export const PLAYGROUND_CONFIGS: PlaygroundConfig[] = [
  {
    path: '/json-path',
    label: 'JSON-Path',
    // ... (unchanged)
    supportsClipboard: false, // explicitly false — or simply omitted
    tabs: ['payload', 'graph', 'graph-data'],
  },
  {
    path: '/',
    label: 'Minigraph',
    // ... (unchanged)
    supportsClipboard: true,
    tabs: ['preview', 'graph', 'graph-data'],
  },
];
```

---

## 11. Component Tree Changes

### 11.1 App.tsx

**Current** (`src/App.tsx:17–29`):
```tsx
<WebSocketProvider>
  <BrowserRouter>
    <Routes>...</Routes>
  </BrowserRouter>
</WebSocketProvider>
```

**Proposed:**
```tsx
<WebSocketProvider>
  <ClipboardProvider>
    <BrowserRouter>
      <Routes>...</Routes>
    </BrowserRouter>
  </ClipboardProvider>
</WebSocketProvider>
```

**Import added:**
```typescript
import { ClipboardProvider } from './contexts/ClipboardContext';
```

### 11.2 Playground.tsx

**New imports:**
```typescript
import { useClipboardContext } from '../contexts/ClipboardContext';
import type { ClipboardItemRecord } from '../clipboard/db';
import { usePasteNode } from '../hooks/usePasteNode';
import ClipboardSidebar from './ClipboardSidebar/ClipboardSidebar';
import { ClipboardDuplicateDialog } from './ClipboardSidebar/ClipboardDuplicateDialog';
```

> **Note:** `findNodeByAlias` and `extractDirectConnections` are imported in
> `GraphView.tsx` (see [Section 11.3](#113-graphviewtsx)), not in
> `Playground.tsx`. The `onClipNode` callback passed to `GraphView` already
> provides the resolved `MinigraphNode` and `MinigraphConnection[]`.

**New state variables:**

```typescript
// Clipboard sidebar open/closed (persisted)
const [clipboardOpen, setClipboardOpen] = useLocalStorage<boolean>('clipboard-sidebar-open', false);

// Duplicate dialog state
const [duplicateDialogState, setDuplicateDialogState] = useState<{
  pendingItem: ClipboardItemRecord;
  existingItem: ClipboardItemRecord;
} | null>(null);
```

**New hooks:**
```typescript
const clipboardCtx = useClipboardContext();

const { pasteNode, isPasting } = usePasteNode({
  wsPath,
  connected: ws.connected,
  sendRawText: ws.sendRawText,
  appendMessage: ws.appendMessage,
  addToast,
  bus,
  graphData,
});
```

**New callback for clip operations** (invoked by GraphView context menu):

```typescript
const handleClipNode = useCallback(async (
  node: MinigraphNode,
  connections: MinigraphConnection[],
) => {
  try {
    const result = await clipboardCtx.clipNode(node, connections, {
      sourceWsPath: wsPath,
      sourceLabel: config.label,
    });

    switch (result.status) {
      case 'added':
        addToast(`Node "${node.alias}" clipped to clipboard`, 'success');
        break;
      case 'duplicate':
        setDuplicateDialogState({
          pendingItem: result.pendingItem,
          existingItem: result.existingItem,
        });
        break;
      case 'error':
        addToast(`Clip failed: ${result.message}`, 'error');
        break;
    }
  } catch (err) {
    addToast(`Clip failed: ${err instanceof Error ? err.message : String(err)}`, 'error');
  }
}, [clipboardCtx, wsPath, config.label, addToast]);
```

> **Note:** The top-level `try/catch` ensures that unexpected errors (e.g.,
> `graphData` in an unforeseen shape) do not produce unhandled Promise
> rejections. The `clipNode()` method has its own internal error handling that
> returns `{ status: 'error' }`, but errors outside that path (before or
> after the call) are caught here.

**Updated `sendDisabled` to include `isPasting`:**

```typescript
sendDisabled={!ws.connected || !ws.command.trim() || isPasting}
```

**JSX changes** (conceptual):

```tsx
{/* Duplicate confirmation dialog */}
{duplicateDialogState && (
  <ClipboardDuplicateDialog
    existingItem={duplicateDialogState.existingItem}
    pendingItem={duplicateDialogState.pendingItem}
    onReplace={async () => {
      try {
        await clipboardCtx.confirmReplace(
          duplicateDialogState.pendingItem,
          duplicateDialogState.existingItem.id,
        );
        setDuplicateDialogState(null);
        addToast(`Clipboard item "${duplicateDialogState.pendingItem.node.alias}" replaced`, 'success');
      } catch (err) {
        addToast(`Replace failed: ${err instanceof Error ? err.message : String(err)}`, 'error');
      }
    }}
    onCancel={() => {
      setDuplicateDialogState(null);
      addToast('Clip cancelled', 'info');
    }}
  />
)}

{/* Main content area (replaces direct <Group> usage) */}
<div className={styles.mainArea}>
  <Group ...>
    {/* LeftPanel and RightPanel unchanged */}
  </Group>

  {config.supportsClipboard && (
    <ClipboardSidebar
      open={clipboardOpen}
      connected={ws.connected}
      isPasting={isPasting}
      onPaste={pasteNode}
    />
  )}
</div>
```

### 11.3 GraphView.tsx

**Updated props interface** — adds `onClipNode` (see [Section 8.1](#81-clip-trigger-right-click-context-menu-on-reactflow-node)):

```typescript
interface GraphViewProps {
  graphData:       MinigraphGraphData | null;
  onCopySuccess?:  () => void;
  onCopyError?:    () => void;
  onRenderError?:  (message: string) => void;
  isRefreshing?:   boolean;
  /** Callback for "Clip to Clipboard" from the node context menu. */
  onClipNode?:     (node: MinigraphNode, connections: MinigraphConnection[]) => void;
}
```

**New internal state and ref** for the context menu:

```typescript
const [contextMenu, setContextMenu] = useState<{
  x: number;
  y: number;
  nodeAlias: string;
} | null>(null);

const menuRef = useRef<HTMLDivElement>(null);
```

**`<ReactFlow>` additions:**

```tsx
<ReactFlow
  ...
  onNodeContextMenu={(event, node) => {
    event.preventDefault();
    setContextMenu({ x: event.clientX, y: event.clientY, nodeAlias: node.data.alias });
  }}
  onPaneClick={() => setContextMenu(null)}
>
```

**Context menu rendering** (inside the `<GraphViewErrorBoundary>`):

```tsx
{contextMenu && onClipNode && graphData && (
  <div
    ref={menuRef}
    className={styles.contextMenu}
    style={{ position: 'fixed', top: contextMenu.y, left: contextMenu.x }}
    role="menu"
  >
    <button
      role="menuitem"
      autoFocus
      onClick={() => {
        const node = findNodeByAlias(graphData, contextMenu.nodeAlias);
        if (node) {
          const connections = extractDirectConnections(graphData, contextMenu.nodeAlias);
          onClipNode(node, connections);
        }
        setContextMenu(null);
      }}
    >
      Clip to Clipboard
    </button>
  </div>
)}
```

**Context menu dismissal `useEffect`:**

```typescript
useEffect(() => {
  if (!contextMenu) return;

  const handleDismiss = (e: MouseEvent) => {
    // Close if click is outside the context menu element.
    // Uses the menuRef to avoid ambiguity with other role="menu" elements.
    if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
      setContextMenu(null);
    }
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') setContextMenu(null);
  };

  document.addEventListener('mousedown', handleDismiss);
  document.addEventListener('keydown', handleKeyDown);
  return () => {
    document.removeEventListener('mousedown', handleDismiss);
    document.removeEventListener('keydown', handleKeyDown);
  };
}, [contextMenu]);
```

### 11.4 RightPanel.tsx

**No changes.** The clipboard sidebar is outside the `RightPanel` component.

---

## 12. File Inventory

### 12.1 New Files

| File | Purpose |
|------|---------|
| `src/clipboard/db.ts` | IndexedDB schema, `getDB()`, CRUD functions including atomic `replaceItem()`, for `ClipboardItemRecord` |
| `src/clipboard/channel.ts` | `BroadcastChannel` message types and factory function |
| `src/clipboard/helpers.ts` | `findNodeByAlias()`, `extractDirectConnections()` (with self-connection filter) |
| `src/clipboard/commandBuilder.ts` | `buildNodeCommand()`, `buildConnectCommand()` |
| `src/contexts/ClipboardContext.tsx` | `ClipboardProvider`, `useClipboardContext()`, reducer, types |
| `src/hooks/usePasteNode.ts` | Paste orchestration hook with local existence checks and step timeouts |
| `src/utils/timeFormat.ts` | `formatRelativeTime()` utility for sidebar timestamps |
| `src/components/ClipboardSidebar/ClipboardSidebar.tsx` | Sidebar shell + item list |
| `src/components/ClipboardSidebar/ClipboardSidebar.module.css` | Sidebar styles |
| `src/components/ClipboardSidebar/ClipboardItem.tsx` | Individual item card |
| `src/components/ClipboardSidebar/ClipboardItem.module.css` | Item card styles |
| `src/components/ClipboardSidebar/ClipboardEmptyState.tsx` | Empty clipboard placeholder |
| `src/components/ClipboardSidebar/ClipboardDuplicateDialog.tsx` | Duplicate-alias confirmation dialog |

### 12.2 Modified Files

| File | Changes |
|------|---------|
| `src/App.tsx` | Wrap `<BrowserRouter>` with `<ClipboardProvider>` |
| `src/config/playgrounds.ts` | Add `supportsClipboard?: boolean` to `PlaygroundConfig`; set `true` on Minigraph config |
| `src/components/Playground.tsx` | Add clipboard state, handlers, sidebar rendering, duplicate dialog, wire `isPasting` to `sendDisabled` |
| `src/components/Playground.module.css` | Add `.mainArea` horizontal flex wrapper |
| `src/components/GraphView/GraphView.tsx` | Add `onClipNode` prop, `onNodeContextMenu` handler, context menu rendering with accessibility and dismissal |
| `src/components/GraphView/GraphView.module.css` | Add `.contextMenu` styles |
| `package.json` | Add `idb` dependency, add `fake-indexeddb` devDependency |

### 12.3 Unchanged Files

The following files are explicitly **not modified** by this feature:

- All Java backend files — no server changes needed
- `src/contexts/WebSocketContext.tsx` — no changes to WebSocket management
- `src/protocol/classifier.ts` — existing classification handles all paste responses
- `src/protocol/events.ts` — no new event types needed
- `src/protocol/bus.ts` — no changes to the event bus
- `src/hooks/useWebSocket.ts` — no changes; command input is reserved for backend WebSocket messages only
- `src/hooks/useAutoGraphRefresh.ts` — already handles `graph.mutation` events from paste
- `src/components/RightPanel/RightPanel.tsx` — sidebar is external to RightPanel
- `src/utils/graphTypes.ts` — existing types are sufficient
- `src/utils/graphTransformer.ts` — no layout changes

---

## 13. Edge Cases and Known Limitations

### 13.1 Graph Data Not Loaded

**Scenario:** User right-clicks a node, but `graphData` is `null` (graph not yet
described).

**This cannot happen.** The `<ReactFlow>` component only renders nodes when
`graphData` is non-null and `graphData.nodes.length > 0`
(`src/components/GraphView/GraphView.tsx:92`). If there are no nodes rendered,
there is nothing to right-click. The context menu is the only clip trigger
(Decision #5), so `graphData` is always available when a clip is initiated.

### 13.2 Clipping a Node with Array/Object Property Values

`MinigraphNodeProperties` allows `[key: string]: unknown`
(`src/utils/graphTypes.ts:7`). When building the command, `serializePropertyValue`
in `src/clipboard/commandBuilder.ts` handles this by calling
`JSON.stringify(value)` for non-string values. The backend parses the resulting
string as-is into the property map.

**Potential issue:** If the backend expects a specific format for array-valued
properties (e.g., `mapping` is `string[]` on the TypeScript side but may be
stored differently server-side), the serialised JSON array
`["input.body.field1","input.body.field2"]` may not round-trip correctly.

**Recommendation:** Add integration tests that clip a node with array-valued
`mapping` properties, paste it into a fresh graph, then `describe node` and
compare the output. Adjust `serializePropertyValue` if the backend requires a
different serialisation.

### 13.3 Node Alias Contains Spaces or Special Characters

The backend's command grammar parses `create node {alias}` by splitting on
whitespace. An alias with spaces (e.g., "my node") would break the command.

**In practice this does not occur.** The backend normalises aliases to
single-word identifiers when creating nodes. The
`MinigraphNode.alias` values returned by `describe graph` are always single
tokens without spaces. No special handling needed.

### 13.4 Known Limitations (Backend Grammar Constraints)

These are constraints imposed by the backend's command grammar that cannot be
resolved client-side. They are documented here so that implementers and users
are aware.

**13.4.1 Multiple node types reduced to one on paste.**
`MinigraphNode.types` is `string[]` — a node can have multiple type labels.
`buildNodeCommand()` only emits `node.types[0]` because the backend's
`with type` clause accepts a single type string. All types beyond the first
are silently dropped on paste. During paste, if `node.types.length > 1`,
a console warning is appended:

```
Clipboard paste: node "{alias}" has {N} types but only "{types[0]}" was applied (backend limitation).
```

**13.4.2 Connection relation properties are lost on paste.**
`MinigraphRelation` has a `properties: Record<string, unknown>` field
(`src/utils/graphTypes.ts:22`). The backend's `connect` grammar
(`connect {source} to {target} with {relation}`) has no syntax for relation
properties. They are stored in the clipboard item for completeness (so a
future backend enhancement could use them) but are not emitted during paste.
During paste, if any relation has non-empty properties, a console warning
is appended:

```
Clipboard paste: relation properties were not applied (backend limitation).
```

**13.4.3 Triple-quote in property values.**
The backend uses `'''` (triple single-quotes) as the multiline property value
delimiter (`GraphLambdaFunction.java`, constant `TRIPLE_QUOTE`). There is no
escape mechanism. If a property value contains the literal string `'''`, the
backend will interpret it as the closing delimiter, truncating the value.
`serializePropertyValue()` logs a `console.warn` when this occurs.

### 13.5 BroadcastChannel Not Available

BroadcastChannel is supported by all modern browsers (Chrome 54+, Firefox 38+,
Edge 79+, Safari 15.4+). In the unlikely event it is unavailable (e.g.,
embedded WebViews), the clipboard still works correctly within a single tab
(IndexedDB + React state). Cross-tab sync simply won't occur.

**Handling:** The `createClipboardChannel()` call in `ClipboardProvider` is
wrapped in a try/catch. If it throws, `channelRef.current` remains `null` and
the `broadcast()` helper is a no-op. See [Section 6.5](#65-provider-implementation-outline).

### 13.6 IndexedDB Quota Exceeded

IndexedDB quotas are generous (typically 50% of available disk space).
A single `ClipboardItemRecord` is unlikely to exceed a few KB even for
complex nodes. With no expiry, a user would need to clip thousands of nodes
to approach limits.

**Handling:** All `db.*` functions are `async` and their rejections are caught
by the `clipNode()` try/catch, which returns `{ status: 'error', message }`.
The UI shows a toast.

### 13.7 Concurrent Paste and Manual Commands

If the user manually types a command in the console while a paste operation is
in flight, the server processes commands sequentially (single-threaded WebSocket
handler per session). The paste hook's response-correlation logic listens for
specific `graph.mutation` events on the `ProtocolBus`. An unrelated manual
command's response may arrive between paste steps.

**Mitigation:** The `isPasting` flag is wired to `sendDisabled` on the command
input (see [Section 11.2](#112-playgroundtsx)), preventing the user from
sending manual commands during paste. If the user manages to send a command
via other means (e.g., another automation), unrelated responses are ignored
by the paste hook's step-based state machine — it only advances on
`graph.mutation` events, and the step timeout prevents indefinite stalls.

### 13.8 Pasting a Node Whose Type Doesn't Exist in the Target

The backend creates nodes with whatever type string is provided — types are
labels, not a pre-defined schema. No special handling needed.

### 13.9 Stale `graphData` During Paste

`graphData` reflects the most recent `describe graph` fetch. If another tab
created or deleted nodes since the last fetch, the local existence check may
be wrong.

- **False negative (node exists but `graphData` doesn't have it):** `create
  node` is sent. Server responds with "already exists." The paste hook falls
  back to `update node` — see [Section 9.6](#96-paste-error-handling).
- **False negative (counterpart exists but `graphData` doesn't have it):**
  Connection is skipped. User can re-paste after the graph refreshes to
  create the missing connection.
- **False positive (node was deleted but `graphData` still has it):** `update
  node` is sent. Server responds with `"Node {alias} not found"`. The paste
  hook detects this via `docs.response` on the bus (text starting with `"Node "`
  and containing `" not found"`) and falls back to `create node` — symmetric
  with the Step 2a "already exists" fallback. See [Section 9.5](#95-response-correlation-during-paste).

All scenarios recover gracefully. Re-paste after interruption is always safe
(see [Section 9.6](#96-paste-error-handling)).

---

## 14. Testing Strategy

### 14.1 Unit Tests

| Module | Test File | Key Test Cases |
|--------|-----------|----------------|
| `clipboard/db.ts` | `clipboard/__tests__/db.test.ts` | `addItem` + `getAllItems` round-trip; `findByAlias` returns correct match; unique alias constraint throws `ConstraintError` on duplicate; `replaceItem` is atomic (delete + add in single tx); `removeItem` deletes; `clearAll` empties store; items sorted by `clippedAt` descending |
| `clipboard/commandBuilder.ts` | `clipboard/__tests__/commandBuilder.test.ts` | `buildNodeCommand('create', ...)` with one type and string props; node with no type; node with array-valued `mapping` property (JSON-serialised); multiline property value gets triple-quoted; single-line value containing `'''` is NOT wrapped (left unquoted, triggers console.warn); `buildNodeCommand('update', ...)` produces identical output with `update` verb; `buildConnectCommand` format; properties with `null`/`undefined` values are filtered out |
| `clipboard/helpers.ts` | `clipboard/__tests__/helpers.test.ts` | `findNodeByAlias` returns node or undefined; `extractDirectConnections` returns connections where alias is source, target, or both; self-connections (`source === target`) are excluded; empty connections array; missing `connections` field (uses `?? []` fallback) |
| `clipboard/channel.ts` | `clipboard/__tests__/channel.test.ts` | Messages broadcast and received across two channel instances; channel.close() prevents further messages |
| `contexts/ClipboardContext.tsx` | `contexts/__tests__/ClipboardContext.test.tsx` | Provider renders children; `clipNode` adds item; `clipNode` detects duplicate; `clipNode` catches `ConstraintError` race; `confirmReplace` replaces atomically; `removeItem` removes; `clearAll` clears; reducer state transitions |
| `utils/timeFormat.ts` | `utils/__tests__/timeFormat.test.ts` | "just now", "X min ago", "X hours ago", "yesterday", date fallback |

**IndexedDB in tests:** Use the `fake-indexeddb` package to run IndexedDB
tests without a real browser.

### 14.2 Integration Tests

| Scenario | Test Plan |
|----------|-----------|
| Clip from GraphView context menu | Mount `GraphView` with mock `graphData`, simulate right-click on a node, verify `onClipNode` called with correct `MinigraphNode` and connections (excluding self-connections) |
| Context menu accessibility | Verify menu opens on `Shift+F10`, closes on `Escape`, has `role="menu"` and `role="menuitem"`, focus moves to menu item on open |
| Context menu dismissal | Verify menu closes on Escape, outside click, pane click |
| Paste creates node | Provide `graphData` without the target alias, verify `create node` command sent via `sendRawText` |
| Paste updates node | Provide `graphData` with the target alias, verify `update node` command sent |
| Paste "already exists" fallback | Simulate `docs.response` with "already exists" text after `create node`, verify `update node` fallback is sent |
| Paste "not found" fallback | Simulate `docs.response` with "not found" text after `update node`, verify `create node` fallback is sent |
| Paste with partial connections | Provide `graphData` with some counterpart nodes missing, verify skipped connections counted and reported |
| Paste with multi-relation connections | Provide connection with 3 relations, verify 3 separate `connect` commands sent |
| Duplicate detection dialog | Clip same alias twice, verify dialog appears, verify Replace and Cancel paths |
| Cross-tab sync | Use two `ClipboardProvider` instances sharing the same BroadcastChannel name, verify clip in one appears in the other's state |

### 14.3 Manual QA Checklist

- [ ] Clip a node from Graph view context menu → appears in sidebar
- [ ] Clip same node again → duplicate dialog appears → Replace works
- [ ] Clip same node again → duplicate dialog appears → Cancel discards
- [ ] Open second tab → clipped item appears in sidebar
- [ ] Paste into second tab (node doesn't exist) → node created
- [ ] Paste into same tab (node exists) → node updated
- [ ] Paste node with connections (counterparts exist) → connections created
- [ ] Paste node with connections (some counterparts missing) → skip message shown
- [ ] Paste node with multiple types → warning about type loss shown in console
- [ ] Close all tabs, reopen → clipboard items persist
- [ ] Clear all → IndexedDB emptied, other tabs update
- [ ] Paste while disconnected → button disabled, no action
- [ ] Paste while another paste in progress → button disabled
- [ ] Context menu opens on right-click, closes on Escape
- [ ] Context menu closes on click outside ReactFlow canvas

---

## 15. Future Enhancements

These are out of scope for the initial implementation but the architecture
is designed to support them.

### 15.1 Multi-Node Clip (Bundle)

> **Note:** Decision #3 specifies starting with single-node clips to validate
> the core workflow before investing in multi-select UI.

Add a new record type:

```typescript
interface ClipboardBundleRecord {
  id: string;
  clippedAt: string;
  sourceWsPath: string;
  sourceLabel: string;
  nodes: MinigraphNode[];
  connections: MinigraphConnection[];
  label: string;
}
```

Store in a separate IndexedDB object store (`bundles`) within the same
database. Increment `DB_VERSION` to 2 and add the store in the `upgrade`
callback. The `ClipboardContext` reducer, `BroadcastChannel` protocol, and
sidebar rendering extend naturally to handle both item types.

UI: ReactFlow supports `onSelectionChange` for multi-node selection. A toolbar
button "Clip Selection" would collect the selected nodes and their
inter-connections.

### 15.2 Drag-and-Drop Paste

Drag a clipboard item from the sidebar and drop it onto the ReactFlow canvas.
ReactFlow's `onDrop` + `onDragOver` handlers can receive a custom drag payload
containing the `ClipboardItemRecord.id`.

### 15.3 Server-Side Clipboard Sync

Add a REST endpoint (e.g., `POST /api/clipboard/sync`) that accepts the full
clipboard state and stores it server-side. The `ClipboardProvider` becomes a
sync orchestrator: writes to IndexedDB first (offline-first), then pushes to
the server. On load, merges server state with local state by `clippedAt`
timestamp. Enables clipboard sharing across browsers/devices.

### 15.4 Clipboard Search and Filter

Add a search input at the top of the sidebar. The IndexedDB `by-alias` index
supports efficient prefix queries via `IDBKeyRange.bound()`. For full-text
search across property values, consider a client-side index library like
`FlexSearch`.

### 15.5 Import from External JSON File

Allow users to drag a `.json` file (exported graph) into the clipboard sidebar.
Parse the file as `MinigraphGraphData`, display a node picker, and clip
selected nodes without importing the entire graph into the active playground.

### 15.6 Undo Paste

Record a reverse operation when pasting:
- If `create node` was sent → reverse is `delete node {alias}`
- If `update node` was sent → reverse stores the pre-paste `describe node`
  JSON and rebuilds the original `update node` command

Store the reverse in the clipboard item's metadata. Surface an "Undo" button
in the sidebar for recently pasted items (within a configurable time window).

### 15.7 Auto-Refreshing Relative Timestamps

Add a `setInterval` (e.g., every 60 seconds) inside `ClipboardSidebar` to
force a re-render of the timestamp displays. This resolves the staleness
issue noted in [Section 7.4.2](#742-sidebar-visual-layout).

---

## Appendix A: Dependency Additions

**Runtime dependency:**

```bash
npm install idb
```

`idb` version `^8.0.0` (latest at time of writing). MIT licensed.
~1.2 KB gzipped. Zero dependencies. TypeScript types included.

**Dev dependency (test only):**

```bash
npm install --save-dev fake-indexeddb
```

`fake-indexeddb` provides a complete in-memory IndexedDB implementation for
`vitest`/`jest` environments, enabling unit tests for `clipboard/db.ts`
without a real browser.

## Appendix B: CSS Custom Properties

The sidebar uses the existing design tokens defined in `src/index.css:1–31`:

| Token | Usage in Sidebar |
|-------|-----------------|
| `--bg-primary` (`#ffffff`) | Sidebar background |
| `--bg-secondary` (`#f8fafc`) | Item card background |
| `--border-color` (`#e2e8f0`) | Sidebar left border, card borders |
| `--text-primary` (`#0f172a`) | Alias label, action button text |
| `--text-secondary` (`#64748b`) | Type, skill, metadata labels |
| `--text-muted` (`#94a3b8`) | Timestamp, connection count |
| `--primary-color` (`#2563eb`) | Paste button, hover highlights |
| `--danger-color` (`#ef4444`) | Remove button hover, Clear All |
| `--radius-sm` (`0.25rem`) | Card border-radius |
| `--shadow-sm` | Card box-shadow |
| `--focus-ring` | Focus-visible outline on action buttons |

No new CSS custom properties are needed.

## Appendix C: Cross-Reference of Source Locations

Key source files and line numbers referenced in this specification. Line
numbers are accurate as of the `feature/command-input` branch at commit
`e75cb384`.

| Reference | File | Lines |
|-----------|------|-------|
| `MinigraphNode` interface | `src/utils/graphTypes.ts` | 10–16 |
| `MinigraphNodeProperties` interface | `src/utils/graphTypes.ts` | 2–8 |
| `MinigraphConnection` interface | `src/utils/graphTypes.ts` | 25–29 |
| `MinigraphRelation` interface | `src/utils/graphTypes.ts` | 19–22 |
| `MinigraphGraphData` interface | `src/utils/graphTypes.ts` | 32–35 |
| `isMinigraphGraphData` type guard | `src/utils/graphTypes.ts` | 42–46 |
| `PlaygroundConfig` interface | `src/config/playgrounds.ts` | 36–58 |
| `PLAYGROUND_CONFIGS` array | `src/config/playgrounds.ts` | 60–85 |
| `WebSocketProvider` component | `src/contexts/WebSocketContext.tsx` | 151–381 |
| `WebSocketContextValue` interface | `src/contexts/WebSocketContext.tsx` | 46–75 |
| `ctx.send()` function | `src/contexts/WebSocketContext.tsx` | 318–325 |
| `App` component tree | `src/App.tsx` | 14–31 |
| `Playground` component | `src/components/Playground.tsx` | 34–436 |
| `useWebSocket` hook | `src/hooks/useWebSocket.ts` | 108–334 |
| `UseWebSocketOptions` interface | `src/hooks/useWebSocket.ts` | 35–41 |
| `UseWebSocketReturn` interface | `src/hooks/useWebSocket.ts` | 44–63 |
| `sendCommand()` function | `src/hooks/useWebSocket.ts` | 150–170 |
| `sendRawText()` function | `src/hooks/useWebSocket.ts` | 291–294 |
| `ProtocolBus` class | `src/protocol/bus.ts` | 16–45 |
| `classifyMessage()` function | `src/protocol/classifier.ts` | 27–181 |
| `ProtocolEvent` union type | `src/protocol/events.ts` | 98–110 |
| `ProtocolEventKind` type | `src/protocol/events.ts` | 113 |
| `detectMutation()` function | `src/utils/messageParser.ts` | 279–305 |
| `GraphView` component | `src/components/GraphView/GraphView.tsx` | 36–156 |
| `GraphViewProps` interface | `src/components/GraphView/GraphView.tsx` | 22–31 |
| `GraphDataView` component | `src/components/GraphDataView/GraphDataView.tsx` | 39–103 |
| `GraphToolbar` component | `src/components/GraphToolbar/GraphToolbar.tsx` | 13–51 |
| `RightPanel` component | `src/components/RightPanel/RightPanel.tsx` | 34–179 |
| `RightTab` type | `src/components/RightPanel/RightPanel.tsx` | 10 |
| `useCopyToClipboard` hook | `src/hooks/useCopyToClipboard.ts` | 47–90 |
| `useAutoGraphRefresh` hook | `src/hooks/useAutoGraphRefresh.ts` | 21–107 |
| `transformGraphData()` function | `src/utils/graphTransformer.ts` | 163–206 |
| `GraphNodeData` interface | `src/utils/graphTransformer.ts` | 7–13 |
| CSS design tokens | `src/index.css` | 1–31 |
| Playground layout styles | `src/components/Playground.module.css` | 1–58 |
| Backend: "already exists" response | `GraphCommandService.java` | 1010 |
| Backend: self-connection rejection | `GraphLambdaFunction.java` | 106 |
| Backend: `TRIPLE_QUOTE` constant | `GraphLambdaFunction.java` | — |
