import { MarkerType, type Node, type Edge } from '@xyflow/react';
import type { CSSProperties } from 'react';
import type { MinigraphGraphData } from './graphTypes';

/** Data bag attached to every ReactFlow node we create. */
export interface GraphNodeData extends Record<string, unknown> {
  alias: string;
  nodeType: string;   // primary type label, e.g. "Root"
  /** All properties from the MinigraphNode, passed through for rendering. */
  properties: Record<string, unknown>;
  sourceHandles: GraphHandleData[];
  targetHandles: GraphHandleData[];
  minHeight: number;
}

export interface GraphHandleData {
  id: string;
  offset: number;
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
  Root:        '#15803d',   // green-700
  End:         '#dc2626',   // red-600
  Fetcher:     '#2563eb',   // blue-600
  mapper:      '#ea580c',   // orange-600
  Math:        '#a16207',   // yellow-700
  JavaScript:  '#7e22ce',   // purple-700
  Provider:    '#be185d',   // pink-700
  Dictionary:  '#0e7490',   // cyan-700
  Join:        '#65a30d',   // lime-700
  Extension:   '#4338ca',   // indigo-700
  Island:      '#475569',   // slate-600
  Decision:    '#b45309',   // amber-700
};
const UNKNOWN_ACCENT = '#6c7086';

// ─── Edge styling constants ──────────────────────────────────────────────────
// EDGE_STROKE: --text-muted (rgb 148 163 184) at 42% opacity — slate-400 tinted stroke
// EDGE_LABEL_BG: references the --bg-secondary token directly so label badges
//   automatically track any future token-level surface change.
const EDGE_STROKE         = 'rgba(148, 163, 184, 0.42)';   // --text-muted at 42%
const EDGE_LABEL_BG       = 'var(--bg-secondary)';          // token: --bg-secondary = #f8fafc
const EDGE_HANDLE_GAP     = 24;   // px between adjacent handle anchors on the same side
const EDGE_HANDLE_PADDING = 32;   // min px from node top/bottom to first/last handle

// Semantic edge-type colors — intentional per-relation accent palette.
// Matches the same amber/green/purple axis used by NODE_ACCENT above.
const EDGE_FALLBACK_COLORS = [
  '#0369a1',   // sky-700
  '#15803d',   // green-700
  '#b45309',   // amber-700
  '#7e22ce',   // purple-700
  '#b91c1c',   // red-700
  '#0f766e',   // teal-700
  '#c2410c',   // orange-700
  '#a16207',   // yellow-700
];

// Named relation-type → accent color map.
const EDGE_COLOR_BY_RELATION: Record<string, string> = {
  fetch:        '#0369a1',   // sky-700
  details:      '#0369a1',
  'ext-call':   '#0369a1',
  mapping:      '#b45309',   // amber-700
  compute:      '#b45309',
  calculate:    '#b45309',
  evaluate:     '#b45309',
  fork:         '#7e22ce',   // purple-700
  join:         '#7e22ce',
  one:          '#7e22ce',
  two:          '#6d28d9',   // purple-800
  three:        '#5b21b6',   // violet-800
  more:         '#4c1d95',   // violet-900
  done:         '#15803d',   // green-700
  complete:     '#15803d',
  finish:       '#15803d',
  positive:     '#15803d',
  negative:     '#b91c1c',   // red-700
};

function hashString(value: string): number {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = ((hash << 5) - hash) + value.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

function edgeColor(relationTypes: string[]): string {
  if (relationTypes.length === 0) return EDGE_STROKE;
  const primary = relationTypes[0].trim().toLowerCase();
  const known = EDGE_COLOR_BY_RELATION[primary];
  if (known) return known;
  return EDGE_FALLBACK_COLORS[hashString(primary) % EDGE_FALLBACK_COLORS.length];
}

function edgeSourceHandleId(index: number): string {
  return `source-${index}`;
}

function edgeTargetHandleId(index: number): string {
  return `target-${index}`;
}

function edgeHandleOffset(index: number, total: number): number {
  if (total <= 1) return 0;
  if (total === 2) return index === 0 ? -EDGE_HANDLE_GAP : EDGE_HANDLE_GAP;
  return (index - (total - 1) / 2) * EDGE_HANDLE_GAP;
}

function buildEdgeHandles(total: number, idForIndex: (index: number) => string): GraphHandleData[] {
  return Array.from({ length: total }, (_, index) => ({
    id: idForIndex(index),
    offset: edgeHandleOffset(index, total),
  }));
}

function nodeHeightForHandleCount(handleCount: number): number {
  if (handleCount <= 1) return NODE_HEIGHT;
  return Math.max(NODE_HEIGHT, ((handleCount - 1) * EDGE_HANDLE_GAP) + (EDGE_HANDLE_PADDING * 2));
}

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
  nodeHeights: Map<string, number>,
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

  // Assign pixel positions — use per-node heights to avoid overlap when nodes
  // are taller than NODE_HEIGHT due to many edge handles.
  const positions = new Map<string, { x: number; y: number }>();
  for (const [level, aliases] of byLevel) {
    const totalHeight = aliases.reduce(
      (sum, alias) => sum + (nodeHeights.get(alias) ?? NODE_HEIGHT),
      0,
    ) + Math.max(0, aliases.length - 1) * ROW_GAP;

    let cursorY = -totalHeight / 2;
    aliases.forEach((alias) => {
      const nodeHeight = nodeHeights.get(alias) ?? NODE_HEIGHT;
      positions.set(alias, {
        x: level * (NODE_WIDTH + COL_GAP),
        y: cursorY,
      });
      cursorY += nodeHeight + ROW_GAP;
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
  const connections = data.connections ?? [];

  // Count outgoing/incoming connections per node to size handles.
  const outgoingEdgeCounts = new Map<string, number>();
  const incomingEdgeCounts = new Map<string, number>();
  for (const conn of connections) {
    outgoingEdgeCounts.set(conn.source, (outgoingEdgeCounts.get(conn.source) ?? 0) + 1);
    incomingEdgeCounts.set(conn.target, (incomingEdgeCounts.get(conn.target) ?? 0) + 1);
  }

  // Compute per-node heights so nodes with many connections don't overlap.
  const nodeHeights = new Map(
    data.nodes.map(n => [
      n.alias,
      nodeHeightForHandleCount(Math.max(
        outgoingEdgeCounts.get(n.alias) ?? 0,
        incomingEdgeCounts.get(n.alias) ?? 0,
      )),
    ]),
  );
  const positions = computeLayout(data.nodes, connections, nodeHeights);

  const rfNodes: Node<GraphNodeData>[] = data.nodes.map(n => {
    const sourceHandleCount = outgoingEdgeCounts.get(n.alias) ?? 0;
    const targetHandleCount = incomingEdgeCounts.get(n.alias) ?? 0;
    const nodeHeight = nodeHeights.get(n.alias) ?? NODE_HEIGHT;

    return {
      id:       n.alias,
      type:     n.types[0] ?? 'default',     // matches the nodeTypes key in GraphView
      position: positions.get(n.alias) ?? { x: 0, y: 0 },
      // width / height give the React Flow wrapper its initial size.
      // NodeResizer will update these as the user drags a handle.
      width:  NODE_WIDTH,
      height: nodeHeight,
      // style is applied directly to the React Flow wrapper element — since
      // MinigraphNode renders as a Fragment there is no inner shell div, so
      // this IS the visible node appearance.
      style: nodeStyle(n.types[0] ?? 'unknown'),
      data: {
        alias:         n.alias,
        nodeType:      n.types[0] ?? 'unknown',
        properties:    n.properties,
        sourceHandles: buildEdgeHandles(sourceHandleCount, edgeSourceHandleId),
        targetHandles: buildEdgeHandles(targetHandleCount, edgeTargetHandleId),
        minHeight:     nodeHeight,
      },
    };
  });

  const outgoingEdgeIndex = new Map<string, number>();
  const incomingEdgeIndex = new Map<string, number>();

  // Flatten connections → edges (one edge per connection with per-handle routing).
  const rfEdges: Edge<GraphEdgeData>[] = [];
  for (const [index, conn] of connections.entries()) {
    const relationTypes = conn.relations.map(r => r.type);
    const edgeId = `${conn.source}__${conn.target}__${index}`;
    const labelColor = edgeColor(relationTypes);
    const sourceEdgeIndex = outgoingEdgeIndex.get(conn.source) ?? 0;
    const targetEdgeIndex = incomingEdgeIndex.get(conn.target) ?? 0;
    outgoingEdgeIndex.set(conn.source, sourceEdgeIndex + 1);
    incomingEdgeIndex.set(conn.target, targetEdgeIndex + 1);

    rfEdges.push({
      id:           edgeId,
      source:       conn.source,
      target:       conn.target,
      sourceHandle: edgeSourceHandleId(sourceEdgeIndex),
      targetHandle: edgeTargetHandleId(targetEdgeIndex),
      label:        relationTypes.join(', '),
      type:         'bezier',
      markerEnd: {
        type:   MarkerType.ArrowClosed,
        width:  16,
        height: 16,
        color:  EDGE_STROKE,
      },
      style: {
        stroke:      EDGE_STROKE,
        strokeWidth: 2,
      },
      labelStyle: {
        fill:       labelColor,
        fontSize:   10,
        fontWeight: 700,
      },
      labelBgStyle: {
        fill:        EDGE_LABEL_BG,
        fillOpacity: 0.94,
        // --text-primary (rgb 15 23 42) at 16% opacity — subtle label border
        stroke:      'rgba(15, 23, 42, 0.16)',
        strokeWidth: 1,
      },
      labelBgPadding:      [5, 2],
      labelBgBorderRadius: 6,
      data: { relationTypes },
    });
  }

  return { nodes: rfNodes, edges: rfEdges };
}
