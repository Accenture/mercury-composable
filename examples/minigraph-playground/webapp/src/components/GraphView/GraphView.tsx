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
import styles from './GraphView.module.css';

interface GraphViewProps {
  graphData:       MinigraphGraphData | null;
  onRenderError?:  (message: string) => void;
}

const EMPTY_NODES: Node<GraphNodeData>[]  = [];
const EMPTY_EDGES: Edge<GraphEdgeData>[]  = [];

export default function GraphView({ graphData, onRenderError }: GraphViewProps) {
  // Keep a stable ref so the useMemo callback can read the latest prop
  // without needing it in the dependency array (avoids spurious re-transforms).
  const onRenderErrorRef = useRef(onRenderError);
  useEffect(() => { onRenderErrorRef.current = onRenderError; }, [onRenderError]);

  const { nodes: initialNodes, edges: initialEdges, transformError } = useMemo(() => {
    if (!graphData) return { nodes: EMPTY_NODES, edges: EMPTY_EDGES, transformError: null };
    try {
      const result = transformGraphData(graphData);
      return { ...result, transformError: null };
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      // Fire the callback after the render cycle to avoid setState-during-render.
      setTimeout(() => onRenderErrorRef.current?.(`Graph render failed: ${message}`), 0);
      return { nodes: EMPTY_NODES, edges: EMPTY_EDGES, transformError: message };
    }
  }, [graphData]);

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
    // key resets the boundary whenever graphData changes, so a corrected graph
    // after a previous failure renders cleanly without a page reload.
    <GraphViewErrorBoundary
      key={graphData ? JSON.stringify(graphData.nodes.map(n => n.alias)) : 'empty'}
      onRenderError={onRenderError}
    >
      <div className={styles.graphWrapper}>
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
          colorMode="dark"
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
      </div>
    </GraphViewErrorBoundary>
  );
}
