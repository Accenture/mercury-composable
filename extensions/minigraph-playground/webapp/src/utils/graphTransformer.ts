import type { Node, Edge } from '@xyflow/react';
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
const NODE_WIDTH  = 200;
const NODE_HEIGHT = 90;
const H_GAP       = 80;   // horizontal gap between nodes on the same level
const V_GAP       = 100;  // vertical gap between levels

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
    const totalHeight = aliases.length * NODE_HEIGHT + (aliases.length - 1) * H_GAP;
    aliases.forEach((alias, idx) => {
      positions.set(alias, {
        x: level * (NODE_WIDTH + V_GAP),
        y: idx * (NODE_HEIGHT + H_GAP) - totalHeight / 2,
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
