import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  BackgroundVariant,
  type Edge,
  type Node,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

import { nodeTypes } from './NodeTypes';
import { GraphViewErrorBoundary } from './GraphViewErrorBoundary';
import { transformGraphData, type GraphNodeData, type GraphEdgeData } from '../../utils/graphTransformer';
import type { MinigraphGraphData, MinigraphNode, MinigraphConnection } from '../../utils/graphTypes';
import { findNodeByAlias, extractDirectConnections } from '../../clipboard/helpers';
import GraphToolbar from '../GraphToolbar/GraphToolbar';
import styles from './GraphView.module.css';

interface GraphViewProps {
  graphData:       MinigraphGraphData | null;
  /** Called after the raw graph JSON is successfully copied to the clipboard. */
  onCopySuccess?:  () => void;
  /** Called when the clipboard write fails. */
  onCopyError?:    () => void;
  onRenderError?:  (message: string) => void;
  /** When true, renders a semi-transparent overlay with a spinner to indicate a background re-fetch. */
  isRefreshing?:   boolean;
  /** Callback for "Clip to Clipboard" from the node context menu. */
  onClipNode?:     (node: MinigraphNode, connections: MinigraphConnection[]) => void;
}

const EMPTY_NODES: Node<GraphNodeData>[]  = [];
const EMPTY_EDGES: Edge<GraphEdgeData>[]  = [];

export default function GraphView({ graphData, onCopySuccess, onCopyError, onRenderError, isRefreshing = false, onClipNode }: GraphViewProps) {

  // ── Context menu state ──────────────────────────────────────────────────
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    nodeAlias: string;
  } | null>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  // Dismiss context menu on outside click or Escape
  useEffect(() => {
    if (!contextMenu) return;

    const handleDismiss = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as globalThis.Node)) {
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

  // Keep a stable ref so the useEffect below can fire the error callback without
  // needing onRenderError in the useMemo dependency array.
  const onRenderErrorRef = useRef(onRenderError);
  useEffect(() => { onRenderErrorRef.current = onRenderError; }, [onRenderError]);

  const { nodes: initialNodes, edges: initialEdges, transformError } = useMemo(() => {
    if (!graphData) return { nodes: EMPTY_NODES, edges: EMPTY_EDGES, transformError: null };
    try {
      const result = transformGraphData(graphData);
      return { ...result, transformError: null };
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      // Do NOT fire side-effects (toasts, setState) inside useMemo — useMemo must
      // be pure.  The error message is surfaced via state and picked up by the
      // useEffect below, which fires the callback safely after the render cycle.
      return { nodes: EMPTY_NODES, edges: EMPTY_EDGES, transformError: message };
    }
  }, [graphData]);

  // Fire the render-error callback whenever the transform produces a new error.
  // A useEffect is the correct place for side-effects that react to derived state.
  // The ref ensures the callback is always current without adding it to the dep array.
  useEffect(() => {
    if (transformError) {
      onRenderErrorRef.current?.(`Graph render failed: ${transformError}`);
    }
  }, [transformError]);

  // Memoize the boundary key so JSON.stringify only runs when graphData actually
  // changes, not on every render of GraphView triggered by unrelated parent state.
  const boundaryKey = useMemo(
    () => graphData ? JSON.stringify(graphData.nodes.map(n => n.alias)) : 'empty',
    [graphData],
  );

  const [nodes, setNodes, onNodesChange] = useNodesState<Node<GraphNodeData>>(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge<GraphEdgeData>>(initialEdges);

  // Re-sync whenever the upstream graphData changes
  useEffect(() => {
    setNodes(initialNodes);
    setEdges(initialEdges);
  }, [initialNodes, initialEdges, setNodes, setEdges]);

  if (transformError) {
    return (
      <div className={styles.empty}>
        <span className={styles.emptyIcon}>⚠️</span>
        <span>Graph could not be rendered.</span>
        <span>{transformError}</span>
      </div>
    );
  }

  if (!graphData || graphData.nodes.length === 0) {
    return (
      <div className={styles.empty}>
        <span className={styles.emptyIcon}>🕸️</span>
        <span>No graph data yet.</span>
        <span>Run <strong>describe graph</strong> or <strong>export graph</strong> in the playground.</span>
      </div>
    );
  }

  return (
    // key resets the boundary whenever the node set changes, so a corrected graph
    // after a previous render failure renders cleanly without a page reload.
    <GraphViewErrorBoundary
      key={boundaryKey}
      onRenderError={onRenderError}
    >
      <div className={styles.graphWrapper} aria-busy={isRefreshing}>
        <GraphToolbar
          graphData={graphData}
          onCopySuccess={onCopySuccess}
          onCopyError={onCopyError}
        />
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          nodeTypes={nodeTypes}
          fitView
          fitViewOptions={{ padding: 0.25 }}
          minZoom={0.2}
          maxZoom={2.5}
          // colorMode="dark" // enable for dark mode
          proOptions={{ hideAttribution: false }}
          onNodeContextMenu={(event, node) => {
            if (!onClipNode) return;
            event.preventDefault();
            setContextMenu({ x: event.clientX, y: event.clientY, nodeAlias: node.data.alias });
          }}
          onPaneClick={() => setContextMenu(null)}
        >
          <Background variant={BackgroundVariant.Dots} gap={18} size={1} color="rgba(255,255,255,0.07)" />
          <Controls showInteractive={false} />
          <MiniMap
            nodeColor={(node) => {
              const colorMap: Record<string, string> = {
                Root:        '#15803d',
                End:         '#dc2626',
                Fetcher:     '#2563eb',
                mapper:      '#ea580c',
                Math:        '#a16207',
                JavaScript:  '#7e22ce',
                Provider:    '#be185d',
                Dictionary:  '#0e7490',
                Join:        '#65a30d',
                Extension:   '#4338ca',
                Island:      '#475569',
                Decision:    '#b45309',
              };
              return colorMap[node.type ?? ''] ?? '#6c7086';
            }}
            maskColor="rgba(0,0,0,0.3)"
            style={{ background: '#fff' }}
          />
        </ReactFlow>
        {isRefreshing && (
          <div className={styles.refreshingOverlay}>
            <div
              className={styles.refreshingSpinner}
              role="status"
              aria-label="Graph refreshing"
            />
          </div>
        )}
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
              className={styles.contextMenuItem}
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
      </div>
    </GraphViewErrorBoundary>
  );
}
