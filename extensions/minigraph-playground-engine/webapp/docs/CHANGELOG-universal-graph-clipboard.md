# Universal Graph Clipboard — Spec Changelog

**Spec:** `docs/SPEC-universal-graph-clipboard.md`  
**Audit:** `docs/AUDIT-universal-graph-clipboard.md`

---

## Rev 4 (2026-03-31)

Final revision addressing the 2 minor findings from the Rev 3 audit.

| Audit # | Finding | Resolution | Section |
|---------|---------|------------|---------|
| **R3-1** | `ClipboardItemRecord` imported from `ClipboardContext` but defined in `clipboard/db.ts` — import would fail at build time | Changed S11.2 import to `import type { ClipboardItemRecord } from '../clipboard/db'` | S11.2 |
| **R3-2** | `onReplace` handler in duplicate dialog lacks error boundary — `confirmReplace` rejection would be unhandled | Added `try/catch` wrapping the `confirmReplace` call, matching the pattern already used in `handleClipNode` | S11.2 |

---

## Rev 3 (2026-03-31)

Revision addressing all 6 findings from the Rev 2 audit, plus a design change
requested by the spec author.

### Design Change: Remove Console Command (`clip node {alias}`)

The `clip node {alias}` console command has been removed. The command input is
reserved exclusively for WebSocket messages to the backend — no client-side
command interception.

**Rationale:** Mixing client-side-only commands with server commands blurs the
boundary between local and remote operations and confuses users about which
commands the server understands.

| Section | Change |
|---------|--------|
| S3 Decision #5 | Changed from two triggers (context menu + console command) to context menu only |
| S3.1 | Console command added as a third rejected alternative with rationale |
| S4.1 | Removed "Console command interception" from Layer 3 description |
| S7.4.4 | Removed `clip node {name}` reference from empty-state text |
| S8.2 (old) | Entire section removed (was "Clip Trigger (b): Console Command"). Old S8.3 renumbered to S8.2. |
| S11.2 | Removed `handleClipNodeByAlias` callback, removed `onClipNodeByAlias` wiring to `useWebSocket`, removed `ws.appendMessage` calls from `handleClipNode` (toasts only) |
| S11.5 (old) | Entire section removed (was useWebSocket.ts changes) |
| S12.2 | Removed `src/hooks/useWebSocket.ts` from modified files; moved to unchanged files list |
| S13.1 | Removed `clip node X` with null graphData scenario (no longer possible) |
| S13.3 | Removed `clip node {alias}` regex reference |
| S14.2 | Removed "Clip via `clip node` command" integration test |
| S14.3 | Removed two `clip node` QA checklist items |

### Audit Fixes (Rev 2 Findings)

| Audit # | Severity | Finding | Resolution | Sections Changed |
|---------|----------|---------|------------|------------------|
| **R2-1** | Medium | S13.9 claims an update-not-found fallback that S9.3 doesn't implement | Added **symmetrical fallback** to Step 2b: when `update node` returns "Node {alias} not found" (detected via `docs.response`), the hook falls back to `create node`. Mirrors Step 2a's "already exists" → update fallback. Added to S9.5 correlation table and S9.6 error handling table. Fixed S13.9 to cite the implemented mechanism. | S9.3 Step 2b, S9.5, S9.6, S13.9, S14.2 |
| **R2-2** | Low-medium | `serializePropertyValue` wraps `'''`-containing single-line values in triple-quotes, causing a larger truncation than leaving them unquoted | Changed logic: only wrap on `\n`. Warn on `'''` regardless. Single-line values containing `'''` are left unquoted to preserve as much of the value as possible. Updated S14.1 test case. | S9.4, S14.1 |
| **R2-3** | Low-medium | `handleClipNodeByAlias` lacked top-level error boundary — unhandled Promise rejections possible | With the console command removed, `handleClipNodeByAlias` no longer exists. The remaining `handleClipNode` (context menu path) now has a **top-level `try/catch`** that catches unexpected errors and shows a toast. Added explanatory note. | S11.2 |
| **R2-4** | Minor | Console message appeared on context-menu clip too (orphaned result line without preceding command echo) | Resolved by removing all `ws.appendMessage` calls from `handleClipNode`. Clip operations now produce **toasts only** — no console messages. This is appropriate because the context menu is the only clip trigger and console echo is a server-command pattern. | S11.2 |
| **R2-5** | Minor | Step 4 toast says "connections created" but connections are fire-and-forget (count is commands sent, not confirmed) | Reworded toast to `"connections sent"`. Added a note in S9.3 that the count is optimistic (commands sent after local counterpart verification; failures are rare). | S9.3 Step 4 |
| **R2-6** | Minor | Context menu dismissal uses global `document.querySelector('[role="menu"]')` which is fragile if other menus exist | Replaced with a **`menuRef` ref** (`useRef<HTMLDivElement>`) attached to the menu element. The `useEffect` now uses `menuRef.current.contains()` instead of querying the DOM. | S11.3 |

### Additional Consistency Edits (Non-Audit)

| Change | Sections |
|--------|----------|
| Response correlation paragraph updated to describe both fallback paths (already-exists and not-found) symmetrically | S9.3 |
| `Playground.tsx` imports updated: removed `findNodeByAlias`, `extractDirectConnections` (these are imported in `GraphView.tsx` only); added note explaining the indirection | S11.2 |
| S13.7 remains unchanged — `isPasting` still disables the send button during paste operations. This is a guard (not a command interception) and is consistent with the principle that the command input is for backend messages only. | — |

---

## Rev 2 (2026-03-31)

Revision addressing all 24 findings from the spec audit. Every finding was
verified as valid against the codebase at commit `e75cb384`.

### Critical Fixes

| Audit # | Finding | Resolution | Sections Changed |
|---------|---------|------------|------------------|
| **1a** | Paste response correlation underspecified — shared `ProtocolBus` events could be misinterpreted by the paste hook | Replaced WebSocket `describe node` round-trips with **local `graphData` checks** for both node existence (Step 1) and connection counterpart existence (Step 3). This eliminates the N+1 bus-event disambiguation problem entirely. Remaining correlation (for `create`/`update` confirmation in Steps 2a/2b) is now a single `graph.mutation` listener with a 10-second step timeout. Added explicit correlation table in S9.5. | S1, S4.1, S4.3, S9.1 (new subsection on why local checks), S9.2 (`graphData` added to `UsePasteNodeOptions`), S9.3 (rewritten steps), S9.5 (rewritten), S13.7, S13.9 (new) |
| **1b** | `create node` "already exists" unhandled — server returns `"Node {alias} already exists"` (confirmed `GraphCommandService.java:1010`) which doesn't match any `detectMutation()` pattern, stalling the paste state machine | Added **fallback path** in S9.3 Step 2a: when `create node` returns "already exists" (detected as a `docs.response` event containing the text), the hook retries as `update node`. Added to S9.5 correlation table and S9.6 error handling table. | S9.3 Step 2a, S9.5, S9.6, Appendix C |
| **1c** | Self-connection (`source === target`) causes backend rejection — server returns `"source and target node names cannot be the same"` (confirmed `GraphLambdaFunction.java:106`) | `extractDirectConnections()` in `src/clipboard/helpers.ts` now **filters out self-connections** with `c.source !== c.target`. Added explanatory comment citing the backend constant `SAME_SOURCE_TARGET`. Updated the Terminology entry for "Direct connections" to note the exclusion. | S2 (Terminology), S5.3 (`connections` field doc), S6.4 (`connections` param doc), S8.1 (helpers code), S14.1 (helpers test cases), Appendix C |
| **1d** | Console echo for successful clip was inconsistent — S8.2 showed a success line but S11.2's `handleClipNodeByAlias` only appended messages on error | `handleClipNode` now calls `ws.appendMessage()` on **both success and error**. S8.2 interception code now appends the command echo (`> clip node {alias}`) and adds to history. Clarified the duplicate case: the dialog opens without an immediate console message. | S8.2 (rewritten interception logic and console echo section), S11.2 (`handleClipNode` and `handleClipNodeByAlias`) |
| **1e** | `handleClipNodeByAlias` didn't `await` the async `handleClipNode` — silent error swallowing and race on `duplicateDialogState` | `handleClipNodeByAlias` is now explicitly `async` and **`await`s `handleClipNode`**. | S11.2 |
| **1f** | Multiple node types reduced to one on paste (data loss) — `buildNodeCommand` emits only `types[0]` but `MinigraphNode.types` is `string[]` | Documented as **Known Limitation 13.4.1**. `buildNodeCommand` now includes a JSDoc note about the constraint. During paste, a console warning is appended when `node.types.length > 1`. Added to manual QA checklist. | S9.4 (`buildNodeCommand` doc), S13.4.1 (new), S14.3 |
| **1g** | Connection relation properties lost on paste — `MinigraphRelation.properties` exists but `connect` grammar has no syntax for them | Documented as **Known Limitation 13.4.2**. `buildConnectCommand` now includes a JSDoc note. During paste, a console warning is appended when any relation has non-empty properties. | S9.4 (`buildConnectCommand` doc), S13.4.2 (new), Appendix C (`MinigraphRelation` added) |

### Design Concern Fixes

| Audit # | Finding | Resolution | Sections Changed |
|---------|---------|------------|------------------|
| **2a** | `by-alias` index lacked unique constraint — concurrent clips from two tabs could create duplicate entries | Added `{ unique: true }` to the `by-alias` index definition. `clipNode()` now catches `ConstraintError` on `db.addItem()` and re-checks `findByAlias()` to surface the duplicate to the user. | S5.4 (index definition), S5.5 (`addItem` doc), S6.5 (`clipNode` implementation), S14.1 (test case added) |
| **2b** | `confirmReplace` used two separate IndexedDB transactions — crash between delete and insert loses both records | Introduced `db.replaceItem(previousId, newItem)` that wraps delete + add in a **single IndexedDB transaction** (`db.transaction(STORE_NAME, 'readwrite')`). `confirmReplace` now calls `replaceItem` instead of separate `removeItem` + `addItem`. | S5.5 (new `replaceItem` function), S6.4 (`confirmReplace` doc updated), S6.5 (`confirmReplace` implementation), S12.1 (db.ts description updated), S14.1 (test case added) |
| **2c** | Paste counterpart check was expensive — N WebSocket round-trips for connection counterparts | **Merged with 1a fix.** All existence checks now use local `graphData.nodes` lookups (O(1) per node, zero network). Added `graphData` to `UsePasteNodeOptions`. Documented staleness caveat and recovery behaviour in S9.1 and S13.9. | S9.1, S9.2, S9.3 Step 3 |
| **2d** | `isPasting` not wired to disable command input | `isPasting` is now passed through to `sendDisabled` in `Playground.tsx` JSX: `sendDisabled={!ws.connected \|\| !ws.command.trim() \|\| isPasting}`. Added `isPasting` prop to `ClipboardSidebarProps` to disable all Paste buttons during an operation. | S7.4.1 (new `isPasting` prop), S7.4.2 (Paste button disabled-when column), S11.2, S13.7 |
| **2e** | `serializePropertyValue` only checked `\n` for triple-quoting, missing edge cases with embedded `'''` | Now also checks `str.includes("'''")` and emits a `console.warn` when the value cannot be losslessly serialised. Documented as **Known Limitation 13.4.3**. | S9.4 (`serializePropertyValue` code), S13.4.3 (new) |
| **2f** | Relative timestamps go stale in the sidebar | Documented as an accepted cosmetic trade-off in S7.4.2 with a note. Added **Future Enhancement 15.7** for auto-refreshing timestamps via `setInterval`. | S7.4.2, S15.7 (new) |

### Gap Fixes

| Audit # | Finding | Resolution | Sections Changed |
|---------|---------|------------|------------------|
| **3a** | Clip triggers (b) and (c) from original design review unexplained | Added **Section 3.1 "Rejected Clip Trigger Alternatives"** documenting the two rejected options (per-node button in GraphDataView; toolbar button on selected node) with reasons. Renumbered surviving triggers to (a) and (b) for consistency. | S3 Decision #5, S3.1 (new), S8.1 heading, S8.2 heading |
| **3b** | `fake-indexeddb` not listed in dependency additions | Added `fake-indexeddb` to **Appendix A** as a dev dependency with install command. | Appendix A |
| **3c** | No keyboard accessibility for context menu | Added accessibility subsection to S8.1: `Shift+F10` keyboard trigger, focus management (auto-focus first item on open, return focus to node on close), Escape key dismissal, ARIA attributes (`role="menu"`, `role="menuitem"`). Updated S11.3 code examples with `autoFocus` and ARIA roles. | S8.1, S11.3, S14.2 (new test cases) |
| **3d** | Context menu dismissal was incomplete — only `onPaneClick` handled | Added `useEffect` in S11.3 with global `mousedown` and `keydown` listeners for outside-click and Escape dismissal. Updated S8.1 to list all dismissal triggers. | S8.1, S11.3 (new `useEffect`), S14.2 (new test cases), S14.3 (new QA items) |
| **3e** | Multi-relation iteration not explicit in paste flow Step 3 | Rewrote Step 3 pseudocode with an explicit **nested loop**: outer loop over connections, inner loop over `connection.relations`. Updated `buildConnectCommand` doc to clarify "call once per relation." | S9.3 Step 3, S14.2 (new test case for multi-relation) |
| **3f** | No documentation that re-paste after interruption is safe | Added explicit **"Re-paste after interruption"** row to the S9.6 error handling table stating that re-paste is safe due to idempotency and explaining the recovery path. | S9.6 |

### Minor Fixes

| Audit # | Finding | Resolution | Sections Changed |
|---------|---------|------------|------------------|
| **4a** | `ClipboardSidebar.open` prop was redundant (always `true` when component mounted) | Changed rendering strategy: sidebar is **always mounted** when `config.supportsClipboard` is true, and uses `open` prop for **CSS slide-in/slide-out transitions** rather than conditional mount/unmount. Updated `ClipboardSidebarProps` doc to explain the CSS transition role. | S7.4.1, S11.2 (JSX) |
| **4b** | Appendix C commit reference was stale (`156ad7fa` vs actual `e75cb384`) | Updated commit hash to **`e75cb384`**. | Appendix C |
| **4c** | `ClipboardProvider` position over-constrained — spec implied dependency on `WebSocketContext` | Added note to S4.2: "ClipboardProvider has no dependency on WebSocketContext. The nesting order is interchangeable. The only requirement is that both wrap BrowserRouter." | S4.2 |
| **4d** | `MinigraphGraphData.connections` may be absent at runtime | Added explanatory comment in `extractDirectConnections` (S8.1) documenting why `?? []` is used and citing `isMinigraphGraphData` type guard at `src/utils/graphTypes.ts:42–46`. Added `isMinigraphGraphData` to Appendix C. | S8.1, Appendix C |
| **4e** | `buildCreateNodeCommand` / `buildUpdateNodeCommand` duplication | Merged into single **`buildNodeCommand(verb, node)`** function parameterised on `'create' \| 'update'`. All references updated (`commandBuilder.ts`, Step 2a/2b pseudocode, test file descriptions, file inventory). | S9.4, S9.3, S12.1, S14.1 |

### Additional Consistency Edits (Non-Audit)

These changes were made during the full-document consistency pass following the
audit fixes:

| Change | Sections |
|--------|----------|
| Section 13 title changed from "Edge Cases" to "Edge Cases and Known Limitations" to reflect new S13.4 subsections | S13 heading, Table of Contents |
| Renumbered clip triggers from "(a) and (d)" to "(a) and (b)" throughout — the original lettering skipped (b) and (c) which caused confusion (audit finding 3a). All references in S3, S8, S11 updated. | S3 Decision #5, S8.1 heading, S8.2 heading, S11.5 |
| Replaced "describe → create/update → connect" in S4.1 Layer 2 description with "local check → create/update → connect" to match the new paste strategy | S4.1 |
| Updated S4.1 Layer 1 description from "Indexes: by-alias, by-clippedAt" to "Indexes: by-alias (unique), by-clippedAt" | S4.1 |
| Added `MinigraphRelation` interface to Appendix C (was missing; needed for S13.4.2 citation) | Appendix C |
| Added `isMinigraphGraphData` type guard to Appendix C (needed for S8.1 citation) | Appendix C |
| Added backend source citations to Appendix C: `GraphCommandService.java:1010` ("already exists") and `GraphLambdaFunction.java:106` (`SAME_SOURCE_TARGET`) | Appendix C |
| S12.1 file descriptions updated to reflect new function names and capabilities (`replaceItem`, self-connection filter, step timeouts, `buildNodeCommand`) | S12.1 |
| S12.2 `Playground.tsx` changes description updated to include `isPasting` wiring | S12.2 |
| S12.2 `package.json` description updated to include `fake-indexeddb` | S12.2 |
| S14.1 test case descriptions updated across all modules to reflect revised APIs | S14.1 |
| S14.2 integration tests expanded with new scenarios: context menu accessibility, context menu dismissal, "already exists" fallback, multi-relation connections | S14.2 |
| S14.3 manual QA checklist expanded with: multi-type warning, paste-during-paste disabled, context menu keyboard and dismissal tests | S14.3 |

---

## Rev 1 (2026-03-31)

Initial draft.
