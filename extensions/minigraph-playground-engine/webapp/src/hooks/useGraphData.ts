import { useState, useEffect, useRef, useCallback } from 'react';
import { isMinigraphGraphData, type MinigraphGraphData } from '../utils/graphTypes';
import { type ToastType } from './useToast';
import { type RightTab } from '../components/RightPanel/RightPanel';

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

/**
 * Manages all graph-data state for the Playground:
 *
 *  Initial-load path (triggered by pinnedGraphPath changing):
 *   - Clears graphData to null while fetch is in-flight (intentional — new graph).
 *   - Auto-switches rightTab to 'graph' on success.
 *   - Cancels in-flight requests on path change or unmount.
 *
 *  Auto-refresh path (triggered by calling refetchGraph()):
 *   - Does NOT clear graphData — stale graph stays visible under overlay.
 *   - Does NOT switch rightTab.
 *   - Sets isRefreshing = true while fetch is in-flight.
 *   - Cancels previous in-flight request if refetchGraph() is called again.
 *
 * @param pinnedGraphPath  Relative API path e.g. `/api/graph/model/my-graph/123-1`,
 *                         or null when no graph is pinned.
 * @param addToast         Toast callback from the parent's useToast hook.
 * @param initialTab       The tab to show before any graph is loaded.
 *                         Should be the first entry in the playground's `tabs` config.
 */
export function useGraphData(
  pinnedGraphPath: string | null,
  addToast: (message: string, type?: ToastType) => void,
  initialTab: RightTab,
): UseGraphDataReturn {
  const [graphData, setGraphData] = useState<MinigraphGraphData | null>(null);
  const [rightTab, setRightTab]   = useState<RightTab>(initialTab);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // useState only uses its initialiser on the very first mount — it ignores
  // subsequent changes to `initialTab`.  When the user switches playgrounds,
  // Playground remounts with a different config.tabs[0], so we must
  // explicitly sync rightTab back to the new playground's first tab.
  useEffect(() => {
    setRightTab(initialTab);
  }, [initialTab]);

  // Keep a ref in sync with the prop so that refetchGraph() (which has an
  // empty dep array) always reads the latest path rather than a stale closure.
  const pinnedGraphPathRef = useRef<string | null>(pinnedGraphPath);
  useEffect(() => {
    pinnedGraphPathRef.current = pinnedGraphPath;
  }, [pinnedGraphPath]);

  // Ref to the AbortController used by refetchGraph() so successive calls
  // cancel the previous in-flight request.
  const refetchAbortRef = useRef<AbortController | null>(null);

  // ── Initial-load / path-change effect ──────────────────────────────────
  // Runs whenever pinnedGraphPath changes (including to null).
  // Nulls graphData while fetching so the UI shows a clean loading state
  // (desired for first load / switching to a different graph).
  // Auto-switches to the Graph tab on success.
  useEffect(() => {
    if (!pinnedGraphPath) return;

    let cancelled = false;
    setGraphData(null); // clear stale data while the new fetch is in-flight

    fetch(pinnedGraphPath)
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((json: unknown) => {
        if (!cancelled && isMinigraphGraphData(json)) {
          setGraphData(json);
          setRightTab('graph'); // auto-switch to Graph tab on success
        }
      })
      .catch((err: Error) => {
        if (!cancelled) addToast(`Graph fetch failed: ${err.message}`, 'error');
      });

    return () => { cancelled = true; };
  }, [pinnedGraphPath, addToast]);

  // ── Imperative re-fetch (auto-refresh path) ─────────────────────────────
  // Empty dep array — this function is intentionally stable across renders.
  // It reads pinnedGraphPath via pinnedGraphPathRef, never via closure.
  const refetchGraph = useCallback(() => {
    const path = pinnedGraphPathRef.current;
    if (!path) return;

    // Cancel any previous in-flight refetch.
    refetchAbortRef.current?.abort();
    const controller = new AbortController();
    refetchAbortRef.current = controller;

    setIsRefreshing(true);

    fetch(path, { signal: controller.signal })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((json: unknown) => {
        if (isMinigraphGraphData(json)) {
          setGraphData(json);
        }
        setIsRefreshing(false);
      })
      .catch((err: Error) => {
        if (err.name === 'AbortError') return;
        addToast(`Graph refresh failed: ${err.message}`, 'error');
        setIsRefreshing(false);
      });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
  // The empty dep array is intentional — see pinnedGraphPathRef for path access.
  // addToast is intentionally excluded: it is stable (from useToast) and including
  // it would require listing it which would force the hook consumer to stabilise it.

  return { graphData, setGraphData, rightTab, setRightTab, isRefreshing, refetchGraph };
}
