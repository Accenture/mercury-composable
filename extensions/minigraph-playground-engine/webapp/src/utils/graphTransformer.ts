import type { Node, Edge } from '@xyflow/react';
import type { CSSProperties } from 'react';
import type { MinigraphGraphData } from './graphTypes';

/** Data bag attached to every ReactFlow node we create. */
export interface GraphNodeData extends Record<string, unknown> {
  alias: string;
  nodeType: string;   // primary type label, e.g. "entry_point"
  skill?: string;
  description?: string;
  question?: string;
  mapping?: string[];
}

/** Data bag attached to every ReactFlow edge we create. */
export interface GraphEdgeData extends Record<string, unknown> {
  relationTypes: string[];   // e.g. ["fetch"]
}

// ─── Layout constants ────────────────────────────────────────────────────────
// NODE_WIDTH / NODE_HEIGHT drive column/row spacing in the layout pass.
// They are also used as the initial `width`/`height` on each node so React
// Flow's wrapper has a reasonable size from the very first render.  After
// mount React Flow's own ResizeObserver measures the real DOM dimensions and
// updates accordingly, keeping NodeResizer in sync.
const NODE_WIDTH  = 240;
const NODE_HEIGHT = 100; // rough estimate; ResizeObserver will correct it post-mount
const ROW_GAP     = 60;   // vertical gap between nodes stacked in the same column
const COL_GAP     = 120;  // horizontal gap between columns (levels)

// ─── Per-type visual style ───────────────────────────────────────────────────
// Because MinigraphNode renders as a React Fragment (no wrapper <div>),
// the React Flow wrapper element IS the visible shell.  We apply its visual
// style via `node.style` so NodeResizer can resize it directly — the exact
// pattern shown in https://reactflow.dev/examples/nodes/node-resizer.
//
// Per-type accent colours are passed as the CSS custom property --node-accent.
// The component's CSS module reads that variable for the header background,
// badge colours, and border — keeping the component itself colour-agnostic and
// following the CSS-variables theming approach described in the React Flow
// theming guide: https://reactflow.dev/learn/customization/theming
const BASE_NODE_STYLE: CSSProperties = {
  boxSizing:    'border-box',
  borderRadius: '8px',
  borderWidth:  '1.5px',
  borderStyle:  'solid',
  background:   'var(--bg-secondary, #1e1e2e)',
  color:        'var(--text-primary, #cdd6f4)',
  fontSize:     '0.75rem',
  boxShadow:    '0 2px 8px rgba(0,0,0,0.45)',
  // overflow:visible so NodeResizer handles (absolutely positioned outside the
  // wrapper bounds) are not clipped — clipping them is what prevents resizing.
  overflow:     'visible',
  // Reset the 10px padding React Flow's built-in stylesheet injects on
  // .react-flow__node-default / -output / -group.
  padding:      0,
};

// Accent colours per node type.  Only the accent value differs between types;
// everything else is shared via BASE_NODE_STYLE.
const NODE_ACCENT: Record<string, string> = {
  entry_point: '#a6e3a1',
  api_fetcher: '#89b4fa',
  mapper:      '#fab387',
  terminator:  '#f38ba8',
};
const UNKNOWN_ACCENT = '#6c7086';

function nodeStyle(nodeType: string): CSSProperties {
  const accent = NODE_ACCENT[nodeType] ?? UNKNOWN_ACCENT;
  return {
    ...BASE_NODE_STYLE,
    borderColor: accent,
    // Expose the accent as a CSS custom property so the CSS module can theme
    // the header, badge, and any other child elements without touching JS.
    ['--node-accent' as string]: accent,
  };
}

/**
 * Very lightweight left-to-right topological layout.
 *
 * Strategy:
 *  1. Build an adjacency list from the connections.
 *  2. Assign each node a "level" (column) via BFS from root nodes.
 *  3. Within each level, stack nodes vertically.
 *
 * If no root is found (no entry_point and no node lacking incoming edges) all
 * nodes are placed on level 0 so the graph is still renderable.
 */
function computeLayout(
  nodes: MinigraphGraphData['nodes'],
  connections: MinigraphGraphData['connections'],
): Map<string, { x: number; y: number }> {
  // Build in-degree and adjacency maps
  const outEdges = new Map<string, string[]>();
  const inDegree  = new Map<string, number>();

  for (const n of nodes) {
    outEdges.set(n.alias, []);
    inDegree.set(n.alias, 0);
  }

  for (const conn of connections ?? []) {
    outEdges.get(conn.source)?.push(conn.target);
    inDegree.set(conn.target, (inDegree.get(conn.target) ?? 0) + 1);
  }

  // Seeds: nodes with in-degree 0, or explicitly typed as entry_point
  const seeds = nodes
    .filter(n => inDegree.get(n.alias) === 0 || n.types.includes('entry_point'))
    .map(n => n.alias);

  const levelOf = new Map<string, number>();
  const queue: string[] = [...seeds];
  seeds.forEach(s => levelOf.set(s, 0));

  // BFS to assign levels
  while (queue.length > 0) {
    const current = queue.shift()!;
    const currentLevel = levelOf.get(current) ?? 0;
    for (const neighbor of outEdges.get(current) ?? []) {
      // Only advance the level; never move a node to a shallower level
      if (!levelOf.has(neighbor) || levelOf.get(neighbor)! <= currentLevel) {
        levelOf.set(neighbor, currentLevel + 1);
        queue.push(neighbor);
      }
    }
  }

  // Nodes that BFS never visited (disconnected) go to the last level + 1
  const maxLevel = levelOf.size > 0 ? Math.max(...levelOf.values()) : 0;
  for (const n of nodes) {
    if (!levelOf.has(n.alias)) levelOf.set(n.alias, maxLevel + 1);
  }

  // Group nodes by level
  const byLevel = new Map<number, string[]>();
  for (const [alias, level] of levelOf) {
    if (!byLevel.has(level)) byLevel.set(level, []);
    byLevel.get(level)!.push(alias);
  }

  // Assign pixel positions
  const positions = new Map<string, { x: number; y: number }>();
  for (const [level, aliases] of byLevel) {
    const totalHeight = aliases.length * NODE_HEIGHT + (aliases.length - 1) * ROW_GAP;
    aliases.forEach((alias, idx) => {
      positions.set(alias, {
        x: level * (NODE_WIDTH + COL_GAP),
        y: idx * (NODE_HEIGHT + ROW_GAP) - totalHeight / 2,
      });
    });
  }

  return positions;
}

/**
 * Converts a MinigraphGraphData object into the ReactFlow `nodes` + `edges`
 * arrays ready to be passed to `<ReactFlow>`.
 */
export function transformGraphData(
  data: MinigraphGraphData,
): { nodes: Node<GraphNodeData>[]; edges: Edge<GraphEdgeData>[] } {
  const positions = computeLayout(data.nodes, data.connections ?? []);

  const rfNodes: Node<GraphNodeData>[] = data.nodes.map(n => ({
    id:       n.alias,
    type:     n.types[0] ?? 'default',     // matches the nodeTypes key in GraphView
    position: positions.get(n.alias) ?? { x: 0, y: 0 },
    // width / height give the React Flow wrapper its initial size.
    // NodeResizer will update these as the user drags a handle.
    width:  NODE_WIDTH,
    height: NODE_HEIGHT,
    // style is applied directly to the React Flow wrapper element — since
    // MinigraphNode renders as a Fragment there is no inner shell div, so
    // this IS the visible node appearance.
    style: nodeStyle(n.types[0] ?? 'unknown'),
    data: {
      alias:       n.alias,
      nodeType:    n.types[0] ?? 'unknown',
      skill:       n.properties.skill,
      description: n.properties.description,
      question:    n.properties.question,
      mapping:     n.properties.mapping,
    },
  }));

  // Flatten connections → edges (one edge per relation per connection)
  const rfEdges: Edge<GraphEdgeData>[] = [];
  for (const conn of data.connections ?? []) {
    const relationTypes = conn.relations.map(r => r.type);
    const edgeId = `${conn.source}__${conn.target}`;
    rfEdges.push({
      id:     edgeId,
      source: conn.source,
      target: conn.target,
      label:  relationTypes.join(', '),
      type:   'smoothstep',
      data:   { relationTypes },
    });
  }

  return { nodes: rfNodes, edges: rfEdges };
}
