import { useEffect, useMemo } from 'react';
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
import { transformGraphData, type GraphNodeData, type GraphEdgeData } from '../../utils/graphTransformer';
import type { MinigraphGraphData } from '../../utils/graphTypes';
import styles from './GraphView.module.css';

interface GraphViewProps {
  graphData: MinigraphGraphData | null;
}

const EMPTY_NODES: Node<GraphNodeData>[]  = [];
const EMPTY_EDGES: Edge<GraphEdgeData>[]  = [];

export default function GraphView({ graphData }: GraphViewProps) {
  const { nodes: initialNodes, edges: initialEdges } = useMemo(() => {
    if (!graphData) return { nodes: EMPTY_NODES, edges: EMPTY_EDGES };
    return transformGraphData(graphData);
  }, [graphData]);

  const [nodes, setNodes, onNodesChange] = useNodesState<Node<GraphNodeData>>(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge<GraphEdgeData>>(initialEdges);

  // Re-sync whenever the upstream graphData changes
  useEffect(() => {
    setNodes(initialNodes);
    setEdges(initialEdges);
  }, [initialNodes, initialEdges, setNodes, setEdges]);

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
  );
}
