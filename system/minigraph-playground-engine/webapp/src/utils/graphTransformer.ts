import { MarkerType, Position, type Node, type Edge } from '@xyflow/react';
import ELK from 'elkjs/lib/elk.bundled.js';
import type { ElkNode, ElkExtendedEdge, ElkPoint, LayoutOptions } from 'elkjs/lib/elk-api';
import type { MinigraphGraphData } from './graphTypes';
import { getMinigraphNodeShellStyle } from './minigraphNodeTheme';

/** Data bag attached to every ReactFlow node we create. */
export interface GraphNodeData extends Record<string, unknown> {
  alias: string;
  nodeType: string;   // primary type label, e.g. "Root"
  /** All properties from the MinigraphNode, passed through for rendering. */
  properties: Record<string, unknown>;
  /** Estimated content height — the NodeResizer floor so a node can't shrink
   *  below the height the layout reserved for it. */
  minHeight: number;
}

/** Data bag attached to every ReactFlow edge we create. */
export interface GraphEdgeData extends Record<string, unknown> {
  relationTypes: string[];   // e.g. ["fetch"]
  /** Routed polyline computed by ELK (start → bend points → end), in flow
   *  coordinates. Absent for edges ELK did not lay out (segregated/cross
   *  edges), which fall back to a smoothstep path in the edge component. */
  points?: ElkPoint[];
  /** Source/target node top-left positions at layout time. Used by the edge
   *  component to warp the ELK polyline by each endpoint's live drag delta so
   *  the routed path follows the nodes when they are moved. */
  sourceLayout?: { x: number; y: number };
  targetLayout?: { x: number; y: number };
}

// ─── Layout constants ────────────────────────────────────────────────────────
// NODE_WIDTH is the fixed node width handed to ELK and used as the initial
// React Flow wrapper width.  Heights are estimated per node from their content
// (see estimateNodeHeight) so ELK reserves roughly the right vertical space;
// React Flow's ResizeObserver measures the real DOM size after mount and keeps
// NodeResizer in sync.
const NODE_WIDTH  = 240;
const NODE_HEIGHT = 80;   // minimum / fallback node height

// Per-node height estimate inputs (mirror NodeTypes.module.css spacing).
const NODE_HEADER_H     = 34;   // header row (icon + alias + badge)
const NODE_BODY_PAD     = 10;   // body vertical padding
const NODE_ROW_H        = 26;   // one property row
const MAX_ESTIMATE_ROWS = 8;    // cap so a huge mapping array doesn't balloon layout

const COL_GAP            = 120;  // horizontal gap between nodes within a segregated row
const SECTION_GAP        = 120;  // vertical gap between the ELK flow and the first segregated row
const SEGREGATED_ROW_GAP = 80;   // vertical gap between successive segregated rows

// ─── Edge styling constants ──────────────────────────────────────────────────
// EDGE_STROKE: --text-muted (rgb 148 163 184) at 42% opacity — slate-400 tinted stroke
// EDGE_LABEL_BG: references the --bg-secondary token directly so label badges
//   automatically track any future token-level surface change.
const EDGE_STROKE   = 'rgba(148, 163, 184, 0.42)';   // --text-muted at 42%
const EDGE_LABEL_BG = 'var(--bg-secondary)';          // token: --bg-secondary = #f8fafc

// Semantic edge-type colors — intentional per-relation accent palette.
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
  then:         '#15803d',   // derived Evaluator THEN branch — taken path
  negative:     '#b91c1c',   // red-700
  else:         '#b45309',   // derived Evaluator ELSE branch — amber
  exception:    '#b91c1c',   // derived Fetcher exception branch — red
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

// ─── Node height estimate ─────────────────────────────────────────────────────
// ELK needs each node's size up front.  We approximate the rendered height from
// the property rows MinigraphNodeBody will draw (one row per scalar property,
// one per array element), mirroring the CSS spacing.  The real size is measured
// by React Flow after mount; this only needs to be close enough that the layout
// reserves sensible vertical space.
function estimateNodeHeight(node: MinigraphGraphData['nodes'][number]): number {
  let rows = 0;
  for (const value of Object.values(node.properties)) {
    if (value === undefined || value === null) continue;
    rows += Array.isArray(value) ? Math.max(1, value.length) : 1;
  }
  rows = Math.min(rows, MAX_ESTIMATE_ROWS);
  return Math.max(NODE_HEIGHT, NODE_HEADER_H + NODE_BODY_PAD + rows * NODE_ROW_H);
}

// ─── Layout node classification ─────────────────────────────────────────────
// Nodes are classified into one of several layout categories that determine
// where they appear in the rendered graph.  'flow' nodes are laid out by ELK;
// every other category is placed in its own horizontal row below the flow.
//
// MODULE_SKILLS — skill values that identify "module" nodes: reusable
// computation blocks invoked via the EXECUTE keyword rather than graph traversal.
const MODULE_SKILLS = new Set(['graph.math', 'graph.js']);

// SEGREGATED_ROW_ORDER — the ordered list of non-flow layout categories.  Each
// gets its own horizontal row below the ELK flow.  Anything unmatched falls into
// a trailing '__unknown__' catch-all row.
const SEGREGATED_ROW_ORDER: readonly string[] = [
  'Dictionary',   // data extraction contracts from API responses
  'Provider',     // reusable API endpoint configurations
  'Module',       // reusable computation blocks (EXECUTE keyword)
  'Entity',       // skill-less data-holder nodes (business domain objects)
];

type LayoutCategory = 'flow' | 'Dictionary' | 'Provider' | 'Entity' | 'Module' | '__unknown__';
type MinigraphNodeModel = MinigraphGraphData['nodes'][number];

// Root-like nodes define the primary execution tree; used only as a model-order
// hint so ELK tends to anchor them on the left.
function isRootLikeNode(node: MinigraphNodeModel): boolean {
  return node.alias.toLowerCase() === 'root' ||
    node.types.includes('Root') ||
    node.types.includes('entry_point');
}

// End-like nodes are terminal branches; ranked last so they tend to the right.
function isEndLikeNode(node: MinigraphNodeModel): boolean {
  return node.alias.toLowerCase() === 'end' || node.types.includes('End');
}

// Model-order rank: root first, end last, everything else in the middle. ELK's
// considerModelOrder uses input order within and across components, so feeding
// nodes in this order biases the entry point to the left and terminals right.
function modelOrderRank(node: MinigraphNodeModel): number {
  if (isRootLikeNode(node)) return 0;
  if (isEndLikeNode(node)) return 2;
  return 1;
}

/**
 * Classify a node into its layout category.
 *
 * Dictionary and Provider nodes are ALWAYS segregated by type.  Every other
 * connected node participates in the main ELK flow; the remaining orphans fall
 * into categorised rows below it.
 *
 * Priority order (first match wins):
 *  1. Dictionary / Provider — type-based segregation, always.
 *  2. Connected — participates in at least one edge → main flow.
 *  3. Module — has a compute skill (graph.math / graph.js) with no
 *     connections.  Reusable computation blocks invoked via EXECUTE.
 *  4. Entity — no skill property; a passive data-holder node.
 *  5. __unknown__ — catch-all safety net for anything else.
 */
function classifyNode(
  node: MinigraphGraphData['nodes'][number],
  connectedAliases: Set<string>,
): LayoutCategory {
  const pt = node.types[0] ?? '';
  if (pt === 'Dictionary') return 'Dictionary';
  if (pt === 'Provider')   return 'Provider';

  if (connectedAliases.has(node.alias)) return 'flow';

  const skill = typeof node.properties.skill === 'string' ? node.properties.skill : undefined;
  if (skill && MODULE_SKILLS.has(skill)) return 'Module';
  if (!skill) return 'Entity';

  return '__unknown__';
}

// ─── ELK ──────────────────────────────────────────────────────────────────────
// Single ELK instance, reused across layouts. elk.bundled.js inlines its web
// worker, so construction is cheap and runs entirely in the browser.
const elk = new ELK();

// Layered (Sugiyama) layout, flowing left-to-right with orthogonal edge routing.
// ELK handles layering, crossing minimisation, coordinate assignment, cycle
// breaking (back-edges are reversed internally) and component separation — all
// the work the previous hand-rolled algorithm did, plus the edge routes.
const ELK_LAYOUT_OPTIONS: LayoutOptions = {
  'elk.algorithm': 'layered',
  'elk.direction': 'RIGHT',
  'elk.edgeRouting': 'ORTHOGONAL',
  'elk.layered.spacing.nodeNodeBetweenLayers': '120',
  'elk.spacing.nodeNode': '60',
  'elk.spacing.edgeNode': '24',
  'elk.spacing.edgeEdge': '16',
  'elk.layered.spacing.edgeNodeBetweenLayers': '24',
  'elk.separateConnectedComponents': 'true',
  'elk.spacing.componentComponent': '120',
  // Honour the input order (root first, end last) when breaking ties so the
  // entry point anchors on the left and terminals trail to the right.
  'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
  'elk.layered.crossingMinimization.forceNodeModelOrder': 'true',
};

/**
 * Run ELK over the connected flow nodes.  Returns node top-left positions and,
 * per laid-out connection index, the routed polyline (start → bends → end) in
 * flow coordinates.  Segregated nodes are placed separately by the caller.
 */
async function layoutFlowWithElk(
  flowNodes: MinigraphNodeModel[],
  connections: MinigraphGraphData['connections'],
  flowAliases: Set<string>,
  heights: Map<string, number>,
): Promise<{
  positions: Map<string, { x: number; y: number }>;
  edgePoints: Map<number, ElkPoint[]>;
}> {
  const positions = new Map<string, { x: number; y: number }>();
  const edgePoints = new Map<number, ElkPoint[]>();
  if (flowNodes.length === 0) return { positions, edgePoints };

  // Feed nodes in model order (root → middle → end) so ELK's model-order
  // tie-breaking biases the entry point left and terminals right.
  const orderedNodes = [...flowNodes].sort(
    (a, b) => modelOrderRank(a) - modelOrderRank(b) || a.alias.localeCompare(b.alias),
  );
  const children: ElkNode[] = orderedNodes.map(n => ({
    id: n.alias,
    width: NODE_WIDTH,
    height: heights.get(n.alias) ?? NODE_HEIGHT,
  }));

  // Only edges entirely within the flow set are laid out by ELK; edges touching
  // segregated nodes are routed by the edge component's fallback path. Map each
  // ELK edge id back to its connection index so we can attach the route later.
  const edges: ElkExtendedEdge[] = [];
  const connByElkId = new Map<string, number>();
  connections.forEach((conn, index) => {
    if (conn.source === conn.target) return;
    if (!flowAliases.has(conn.source) || !flowAliases.has(conn.target)) return;
    const id = `e${index}`;
    edges.push({ id, sources: [conn.source], targets: [conn.target] });
    connByElkId.set(id, index);
  });

  const layout = await elk.layout({
    id: 'root',
    layoutOptions: ELK_LAYOUT_OPTIONS,
    children,
    edges,
  });

  for (const child of layout.children ?? []) {
    positions.set(child.id, { x: child.x ?? 0, y: child.y ?? 0 });
  }
  for (const edge of layout.edges ?? []) {
    const index = connByElkId.get(edge.id);
    if (index === undefined) continue;
    const section = edge.sections?.[0];
    if (!section) continue;
    edgePoints.set(index, [
      section.startPoint,
      ...(section.bendPoints ?? []),
      section.endPoint,
    ]);
  }

  return { positions, edgePoints };
}

// Pulls the `next` keyword out of THEN/ELSE branches: it means "fall through to
// the node already wired by the explicit execute/ask edge", so it is not a new
// edge of its own.
const BRANCH_TARGET_RE = /\b(THEN|ELSE)\s*:\s*([A-Za-z0-9_-]+)/g;

/**
 * Derive the branch edges the graph encodes inside node properties but omits
 * from the `connections` array, so the layout sees the real control flow:
 *
 *  - Evaluator `statement` lines of the form `THEN: <alias>` / `ELSE: <alias>`
 *    (the `next` keyword is skipped — it reuses the explicit execute/ask edge).
 *  - Fetcher `exception: <alias>` — the error-handler branch.
 *
 * Without these, the targeted decision / shape / error nodes have no incoming
 * edge, so the layout treats them as roots and has to guess their column.
 * Each derived edge is tagged `properties.derived` so it can be rendered
 * distinctly (dashed, branch-coloured) and deduped against explicit edges.
 */
function deriveImplicitConnections(
  nodes: MinigraphGraphData['nodes'],
  connections: MinigraphGraphData['connections'],
): MinigraphGraphData['connections'] {
  const aliases = new Set(nodes.map(n => n.alias));
  const seen = new Set(connections.map(c => `${c.source}\t${c.target}`));
  const derived: MinigraphGraphData['connections'] = [];

  const add = (source: string, target: string, type: string) => {
    if (target === 'next' || !aliases.has(target) || source === target) return;
    const key = `${source}\t${target}`;
    if (seen.has(key)) return;          // already an explicit edge — don't duplicate
    seen.add(key);
    derived.push({ source, target, relations: [{ type, properties: { derived: true } }] });
  };

  for (const n of nodes) {
    const props = n.properties as Record<string, unknown>;

    const statements = props.statement;
    if (Array.isArray(statements)) {
      for (const line of statements) {
        if (typeof line !== 'string') continue;
        for (const m of line.matchAll(BRANCH_TARGET_RE)) {
          add(n.alias, m[2], m[1].toLowerCase()); // 'then' | 'else'
        }
      }
    }

    const exception = props.exception;
    if (typeof exception === 'string' && exception.length > 0) {
      add(n.alias, exception, 'exception');
    }
  }

  return derived;
}

/**
 * Converts a MinigraphGraphData object into the ReactFlow `nodes` + `edges`
 * arrays ready to be passed to `<ReactFlow>`.
 *
 * Layout is delegated to ELK (elkjs): the connected "flow" nodes are laid out
 * by ELK's layered algorithm with orthogonal edge routing, and the segregated
 * category nodes (Dictionary / Provider / Module / Entity) are placed in their
 * own rows below the ELK bounding box.  Because ELK runs asynchronously this
 * function returns a Promise.
 */
export async function transformGraphData(
  data: MinigraphGraphData,
): Promise<{ nodes: Node<GraphNodeData>[]; edges: Edge<GraphEdgeData>[] }> {
  // Merge the explicit connections with the branch edges encoded in node
  // properties so layout and rendering both operate on the real control flow.
  const explicitConnections = data.connections ?? [];
  const connections = [
    ...explicitConnections,
    ...deriveImplicitConnections(data.nodes, explicitConnections),
  ];

  // ── Classify & partition ────────────────────────────────────────────────
  const connectedAliases = new Set<string>();
  for (const conn of connections) {
    connectedAliases.add(conn.source);
    connectedAliases.add(conn.target);
  }

  const categoryOf = new Map<string, LayoutCategory>();
  const flowAliases = new Set<string>();
  const flowNodes: MinigraphNodeModel[] = [];
  for (const n of data.nodes) {
    const cat = classifyNode(n, connectedAliases);
    categoryOf.set(n.alias, cat);
    if (cat === 'flow') {
      flowAliases.add(n.alias);
      flowNodes.push(n);
    }
  }

  // ── Node height estimates (used by ELK and as the resize floor) ───────────
  const heights = new Map(data.nodes.map(n => [n.alias, estimateNodeHeight(n)]));

  // ── ELK layout of the connected flow ──────────────────────────────────────
  const { positions, edgePoints } = await layoutFlowWithElk(
    flowNodes,
    connections,
    flowAliases,
    heights,
  );

  // ── Segregated rows, stacked below the ELK bounding box ───────────────────
  // Anchor the rows at the flow's left edge and just below its lowest node.
  let flowMinX = Number.POSITIVE_INFINITY;
  let flowMaxY = 0;
  for (const [alias, pos] of positions) {
    flowMinX = Math.min(flowMinX, pos.x);
    flowMaxY = Math.max(flowMaxY, pos.y + (heights.get(alias) ?? NODE_HEIGHT));
  }
  const rowStartX = Number.isFinite(flowMinX) ? flowMinX : 0;
  let nextRowY = positions.size > 0 ? flowMaxY + SECTION_GAP : 0;

  const groupMap = new Map<string, string[]>();
  for (const key of SEGREGATED_ROW_ORDER) groupMap.set(key, []);
  groupMap.set('__unknown__', []);
  for (const n of data.nodes) {
    const cat = categoryOf.get(n.alias)!;
    if (cat === 'flow') continue;
    groupMap.get(cat)!.push(n.alias);
  }

  for (const key of [...SEGREGATED_ROW_ORDER, '__unknown__']) {
    const aliases = (groupMap.get(key) ?? []).slice().sort(); // alphabetical for stability
    if (aliases.length === 0) continue;

    const rowHeight = aliases.reduce(
      (max, alias) => Math.max(max, heights.get(alias) ?? NODE_HEIGHT),
      0,
    );
    aliases.forEach((alias, i) => {
      positions.set(alias, {
        x: rowStartX + i * (NODE_WIDTH + COL_GAP),
        y: nextRowY,
      });
    });
    nextRowY += rowHeight + SEGREGATED_ROW_GAP;
  }

  // ── Build ReactFlow nodes ─────────────────────────────────────────────────
  // ELK owns routing now, so each node carries a single source handle (right)
  // and a single target handle (left); the rendered edge path comes from ELK,
  // not from the handle geometry.
  const rfNodes: Node<GraphNodeData>[] = data.nodes.map(n => {
    const nodeType = n.types[0] ?? 'unknown';
    const minHeight = heights.get(n.alias) ?? NODE_HEIGHT;
    return {
      id:       n.alias,
      type:     n.types[0] ?? 'default',
      position: positions.get(n.alias) ?? { x: 0, y: 0 },
      width:  NODE_WIDTH,
      height: minHeight,
      sourcePosition: Position.Right,
      targetPosition: Position.Left,
      style: getMinigraphNodeShellStyle(nodeType),
      data: {
        alias:      n.alias,
        nodeType,
        properties: n.properties,
        minHeight,
      },
    };
  });

  // ── Build ReactFlow edges ─────────────────────────────────────────────────
  // The 'elk' custom edge draws ELK's routed polyline when `data.points` is
  // present and falls back to a smoothstep path otherwise.
  //
  // Handle assignment: pick the source/target anchor side that matches the
  // connection's geometry so the smoothstep fallback (used for segregated /
  // cross edges and for any node the user drags) routes cleanly. Vertical links
  // — e.g. a Provider stacked directly under its Dictionary — drop bottom→top
  // instead of bending out the right and back into the left. React Flow derives
  // sourcePosition/targetPosition from the chosen handle, so no extra wiring is
  // needed. ELK-routed edges ignore the handle (their path comes from ELK).
  const HALF_W = NODE_WIDTH / 2;
  const centerOf = (alias: string) => {
    const p = positions.get(alias) ?? { x: 0, y: 0 };
    const h = heights.get(alias) ?? NODE_HEIGHT;
    return { x: p.x + HALF_W, y: p.y + h / 2 };
  };
  const pickHandles = (source: string, target: string): { sourceHandle: string; targetHandle: string } => {
    const s = centerOf(source);
    const t = centerOf(target);
    const dx = t.x - s.x;
    const dy = t.y - s.y;
    if (Math.abs(dy) > Math.abs(dx)) {
      return dy >= 0
        ? { sourceHandle: 's-bottom', targetHandle: 't-top' }
        : { sourceHandle: 's-top',    targetHandle: 't-bottom' };
    }
    return dx >= 0
      ? { sourceHandle: 's-right', targetHandle: 't-left' }
      : { sourceHandle: 's-left',  targetHandle: 't-right' };
  };

  const rfEdges: Edge<GraphEdgeData>[] = connections.map((conn, index) => {
    const relationTypes = conn.relations.map(r => r.type);
    const labelColor = edgeColor(relationTypes);
    const points = edgePoints.get(index);
    const { sourceHandle, targetHandle } = pickHandles(conn.source, conn.target);
    return {
      id:           `${conn.source}__${conn.target}__${index}`,
      source:       conn.source,
      target:       conn.target,
      sourceHandle,
      targetHandle,
      type:         'elk',
      label:        relationTypes.join(', '),
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
        stroke:      'rgba(15, 23, 42, 0.16)',
        strokeWidth: 1,
      },
      labelBgPadding:      [5, 2],
      labelBgBorderRadius: 6,
      data: {
        relationTypes,
        points,
        // Only ELK-routed edges need warp anchors; fallback edges already use
        // live handle coords via getSmoothStepPath.
        sourceLayout: points ? positions.get(conn.source) : undefined,
        targetLayout: points ? positions.get(conn.target) : undefined,
      },
    };
  });

  return { nodes: rfNodes, edges: rfEdges };
}
