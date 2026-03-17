import { useEffect, useRef } from 'react';
import {
  detectMutation,
  isGraphLinkMessage,
  extractGraphApiPath,
} from '../utils/messageParser';
import { type ToastType } from './useToast';
import { type RightTab } from '../components/RightPanel/RightPanel';

export interface UseAutoGraphRefreshOptions {
  /**
   * Full incoming message log from useWebSocket.
   * The hook inspects only messages with IDs greater than the watermark
   * recorded at mount time — older history is never reprocessed.
   */
  messages:           { id: number; raw: string }[];
  /**
   * The currently pinned graph API path (e.g. `/api/graph/model/my-graph/1`),
   * or null when no graph has been loaded yet.
   */
  pinnedGraphPath:    string | null;
  /**
   * Setter for pinnedGraphPath — called when the import-graph or
   * node-mutation (no prior pin) auto-describe path discovers a new graph URL.
   */
  setPinnedGraphPath: (path: string | null) => void;
  /**
   * Whether the WebSocket is currently connected.
   * sendRawText is a no-op when disconnected; the guard here prevents
   * setting waitingForDescribeRef = true while disconnected (which would
   * cause the next connection's first graph-link message to be consumed).
   */
  connected:          boolean;
  /**
   * Imperatively re-fetch the currently pinned graph (overlay mode).
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
   */
  rightTab:           RightTab;
  addToast:           (msg: string, type?: ToastType) => void;
}

/**
 * Watches the WebSocket message stream for graph mutation signals and
 * automatically re-renders the graph without requiring user interaction.
 *
 * Behaviour (per spec v4):
 *
 *  node-mutation + graph already pinned (pinnedGraphPath !== null):
 *    - Debounce 300 ms, then call refetchGraph() (overlay mode — no tab switch).
 *    - Shows an info toast.
 *
 *  node-mutation + no graph pinned (pinnedGraphPath === null):
 *    - Send `describe graph` over the WebSocket.
 *    - Set waitingForDescribeRef = true.
 *    - The next graph-link message is consumed: setPinnedGraphPath() is called,
 *      which triggers the initial-load path in useGraphData, which auto-switches
 *      the tab to Graph.
 *    - Shows an info toast.
 *
 *  import-graph (always, regardless of pinnedGraphPath):
 *    - Send `describe graph` over the WebSocket.
 *    - Set waitingForDescribeRef = true.
 *    - Same graph-link consumption as the node-mutation null-path above.
 *    - Shows an info toast.
 *
 * This hook has no return value — its only job is to dispatch side effects.
 */
export function useAutoGraphRefresh({
  messages,
  pinnedGraphPath,
  setPinnedGraphPath,
  connected,
  refetchGraph:  _refetchGraph, // kept in interface for future use; node-mutation path now always re-describes
  sendRawText,
  rightTab: _rightTab,     // reserved for future badge-pulse animation (spec §11)
  addToast,
}: UseAutoGraphRefreshOptions): void {

  // ── Message-ID watermark ──────────────────────────────────────────────────
  // Initialised to the highest ID in the current message log at mount time.
  // This prevents replaying historical messages as mutations on mount.
  const watermarkRef = useRef<number>(-1);

  // ── Debounce timer handle ─────────────────────────────────────────────────
  // Holds the setTimeout ID for the 300 ms debounce on node-mutation re-fetches.
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // ── waitingForDescribeRef ─────────────────────────────────────────────────
  // When true, the next graph-link message arriving in the message stream is
  // consumed by this hook to call setPinnedGraphPath rather than being ignored.
  // Set to true when we fire a `describe graph` command automatically.
  const waitingForDescribeRef = useRef(false);

  // ── Stale-closure fix for pinnedGraphPath ─────────────────────────────────
  // The useEffect below uses pinnedGraphPath in a timer callback. Without a
  // ref, the callback would read a stale closure value after subsequent renders.
  const pinnedGraphPathRef = useRef<string | null>(pinnedGraphPath);
  useEffect(() => {
    pinnedGraphPathRef.current = pinnedGraphPath;
  }, [pinnedGraphPath]);

  // ── Reset waitingForDescribeRef on disconnect ─────────────────────────────
  // If the WebSocket drops while we are waiting for a `describe graph` response,
  // the pending flag must be cleared.  Without this, the first graph-link
  // message that arrives after reconnection would be silently consumed to call
  // setPinnedGraphPath — potentially with a stale or unrelated path from an
  // entirely new session.
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

  // ── Set watermark at mount ────────────────────────────────────────────────
  // Runs once on mount. Captures the highest message ID currently in the log
  // so that only genuinely new messages (posted after mount) are processed.
  useEffect(() => {
    if (messages.length > 0) {
      watermarkRef.current = messages[messages.length - 1].id;
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
  // Intentionally empty dep array — we only want the mount snapshot.

  // ── Main effect ───────────────────────────────────────────────────────────
  // Re-runs on every messages change (the only dependency that matters).
  // Also depends on connected, refetchGraph, sendRawText, setPinnedGraphPath,
  // addToast — all of which are stable references.
  useEffect(() => {
    if (messages.length === 0) return;

    // Only scan messages that arrived after the watermark.
    const newMessages = messages.filter(m => m.id > watermarkRef.current);
    if (newMessages.length === 0) return;

    // Advance watermark to the latest processed ID.
    watermarkRef.current = messages[messages.length - 1].id;

    // ── Pass 1: consume a pending describe-graph response ─────────────────
    // If we previously sent `describe graph` automatically, look for the
    // graph-link response message and call setPinnedGraphPath to kick off
    // the initial-load path in useGraphData (which auto-switches the tab).
    if (waitingForDescribeRef.current) {
      for (const msg of newMessages) {
        if (isGraphLinkMessage(msg.raw)) {
          const path = extractGraphApiPath(msg.raw);
          if (path) {
            waitingForDescribeRef.current = false;
            setPinnedGraphPath(path);
            return; // done — no further processing needed for this batch
          }
        }
      }
    }

    // ── Pass 2: detect mutation commands ──────────────────────────────────
    // Check every new message for a mutation signal. We process all of them
    // so that the debounce correctly collapses rapid-fire commands.
    let hasMutation = false;
    let hasImportGraph = false;

    for (const msg of newMessages) {
      const kind = detectMutation(msg.raw);
      if (kind === 'import-graph') {
        hasImportGraph = true;
      } else if (kind === 'node-mutation') {
        hasMutation = true;
      }
    }

    // ── import-graph always triggers describe-graph ────────────────────────
    if (hasImportGraph && connected) {
      // Cancel any pending node-mutation debounce — import-graph supersedes it.
      if (debounceTimerRef.current !== null) {
        clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = null;
      }
      waitingForDescribeRef.current = true;
      sendRawText('describe graph');
      addToast('Graph imported — refreshing view…', 'info');
      return;
    }

    // ── node-mutation ─────────────────────────────────────────────────────
    if (hasMutation) {
      // Cancel any already-pending debounce so rapid commands collapse to one.
      if (debounceTimerRef.current !== null) {
        clearTimeout(debounceTimerRef.current);
      }

      debounceTimerRef.current = setTimeout(() => {
        debounceTimerRef.current = null;

        if (!connected) return;

        // Always send `describe graph` — regardless of whether a graph is
        // already pinned. Re-fetching the old pinned URL directly is incorrect
        // because the server's temp file is only updated when `describe graph`
        // runs; the file still reflects the last describe, not the mutation
        // that just occurred.
        //
        // - If pinnedGraphPath !== null: the graph-link response will carry a
        //   new URL. setPinnedGraphPath() is called with it, which triggers the
        //   initial-load path in useGraphData and re-renders the graph.
        // - If pinnedGraphPath === null: same flow — graph is auto-generated
        //   and the tab switches to Graph automatically.
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

    // Cancel the debounce timer if the component unmounts (or the effect
    // re-runs) while it is still pending, so sendRawText is never called
    // after teardown.
    return () => {
      if (debounceTimerRef.current !== null) {
        clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = null;
      }
    };
  }, [messages, connected, sendRawText, setPinnedGraphPath, addToast]);
}
