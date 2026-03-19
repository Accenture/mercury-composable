import { useEffect, useMemo, useRef } from 'react';
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
import type { MinigraphGraphData } from '../../utils/graphTypes';
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
}

const EMPTY_NODES: Node<GraphNodeData>[]  = [];
const EMPTY_EDGES: Edge<GraphEdgeData>[]  = [];

export default function GraphView({ graphData, onCopySuccess, onCopyError, onRenderError, isRefreshing = false }: GraphViewProps) {

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
        >
          <Background variant={BackgroundVariant.Dots} gap={18} size={1} color="rgba(255,255,255,0.07)" />
          <Controls showInteractive={false} />
          <MiniMap
            nodeColor={(node) => {
              const colorMap: Record<string, string> = {
                entry_point: '#a6e3a1',
                api_fetcher: '#89b4fa',
                mapper:      '#fab387',
                terminator:  '#f38ba8',
              };
              return colorMap[node.type ?? ''] ?? '#6c7086';
            }}
            maskColor="rgba(0,0,0,0.6)"
            style={{ background: '#181825' }}
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
      </div>
    </GraphViewErrorBoundary>
  );
}
