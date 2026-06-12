import {
  BaseEdge,
  EdgeLabelRenderer,
  getSmoothStepPath,
  useInternalNode,
  Position,
  type EdgeProps,
  type Edge,
  type InternalNode,
  type Node,
} from '@xyflow/react';
import type { GraphEdgeData } from '../../utils/graphTransformer';

type ElkPoint = { x: number; y: number };

// Corner rounding radius for ELK's orthogonal routes. Each bend is replaced
// with a short quadratic curve so the polyline reads as a smooth wire rather
// than hard right angles.
const CORNER_RADIUS = 12;

// Fallback node dimensions when React Flow has not measured a node yet.
const FALLBACK_W = 240;
const FALLBACK_H = 80;

/**
 * Compute the live anchor point + exit side for an edge endpoint directly from
 * the node's current absolute position and measured size. We do NOT trust the
 * sourceX/sourceY props React Flow passes: under the React Compiler those can
 * lag a drag, whereas useInternalNode's positionAbsolute is always current.
 * `side` is the bare handle side ('top' | 'right' | 'bottom' | 'left').
 */
function liveAnchor(
  node: InternalNode<Node> | undefined,
  side: string,
): { x: number; y: number; position: Position } {
  const pos = node?.internals.positionAbsolute ?? { x: 0, y: 0 };
  const w = node?.measured.width ?? FALLBACK_W;
  const h = node?.measured.height ?? FALLBACK_H;
  switch (side) {
    case 'top':    return { x: pos.x + w / 2, y: pos.y,         position: Position.Top };
    case 'bottom': return { x: pos.x + w / 2, y: pos.y + h,     position: Position.Bottom };
    case 'left':   return { x: pos.x,         y: pos.y + h / 2, position: Position.Left };
    default:       return { x: pos.x + w,     y: pos.y + h / 2, position: Position.Right };
  }
}

function distance(a: ElkPoint, b: ElkPoint): number {
  return Math.hypot(b.x - a.x, b.y - a.y);
}

/**
 * Build an SVG path from ELK's routed points, rounding each interior bend.
 * For every interior point we draw a line up to `radius` short of the corner,
 * then a quadratic curve through the corner to `radius` past it.
 */
function roundedPath(points: ElkPoint[], radius = CORNER_RADIUS): string {
  if (points.length < 2) return '';
  if (points.length === 2) {
    return `M ${points[0].x},${points[0].y} L ${points[1].x},${points[1].y}`;
  }

  let d = `M ${points[0].x},${points[0].y}`;
  for (let i = 1; i < points.length - 1; i++) {
    const prev = points[i - 1];
    const curr = points[i];
    const next = points[i + 1];

    const dPrev = distance(prev, curr) || 1;
    const dNext = distance(next, curr) || 1;
    const rPrev = Math.min(radius, dPrev / 2);
    const rNext = Math.min(radius, dNext / 2);

    // Approach point: rPrev short of the corner along the incoming segment.
    const ax = curr.x + ((prev.x - curr.x) / dPrev) * rPrev;
    const ay = curr.y + ((prev.y - curr.y) / dPrev) * rPrev;
    // Departure point: rNext past the corner along the outgoing segment.
    const bx = curr.x + ((next.x - curr.x) / dNext) * rNext;
    const by = curr.y + ((next.y - curr.y) / dNext) * rNext;

    d += ` L ${ax},${ay} Q ${curr.x},${curr.y} ${bx},${by}`;
  }
  const last = points[points.length - 1];
  d += ` L ${last.x},${last.y}`;
  return d;
}

/** Midpoint of the polyline by arc length — where the relation label sits. */
function midpointByLength(points: ElkPoint[]): ElkPoint {
  if (points.length === 0) return { x: 0, y: 0 };
  if (points.length === 1) return points[0];

  let total = 0;
  for (let i = 1; i < points.length; i++) total += distance(points[i - 1], points[i]);

  let walked = 0;
  const half = total / 2;
  for (let i = 1; i < points.length; i++) {
    const seg = distance(points[i - 1], points[i]);
    if (walked + seg >= half) {
      const t = seg === 0 ? 0 : (half - walked) / seg;
      return {
        x: points[i - 1].x + (points[i].x - points[i - 1].x) * t,
        y: points[i - 1].y + (points[i].y - points[i - 1].y) * t,
      };
    }
    walked += seg;
  }
  return points[points.length - 1];
}

/**
 * Custom edge that renders ELK's routed orthogonal polyline when available
 * (`data.points`), falling back to a smoothstep path for edges ELK did not lay
 * out (edges touching segregated nodes). The relation label is rendered as a
 * badge at the path midpoint, preserving the previous edge-label styling.
 */
export function ElkEdge({
  id,
  source,
  target,
  sourceHandleId,
  targetHandleId,
  data,
  markerEnd,
  style,
  label,
  labelStyle,
  labelBgStyle,
  labelBgPadding,
  labelBgBorderRadius,
}: EdgeProps<Edge<GraphEdgeData>>) {
  const points = data?.points;

  // Subscribe to both nodes' live state. useInternalNode re-renders this edge
  // whenever the node moves, and positionAbsolute is always current (unlike the
  // sourceX/sourceY props, which the React Compiler can memoize a frame behind).
  const sourceNode = useInternalNode(source);
  const targetNode = useInternalNode(target);

  // Has either endpoint moved from where ELK laid it out? ELK routes are static
  // snapshots, so once a node is dragged the stored polyline is stale.
  const sl = data?.sourceLayout;
  const tl = data?.targetLayout;
  const sPos = sourceNode?.internals.positionAbsolute;
  const tPos = targetNode?.internals.positionAbsolute;
  const endpointMoved =
    (!!sl && !!sPos && (sPos.x !== sl.x || sPos.y !== sl.y)) ||
    (!!tl && !!tPos && (tPos.x !== tl.x || tPos.y !== tl.y));

  let path: string;
  let labelX: number;
  let labelY: number;

  if (points && points.length >= 2 && !endpointMoved) {
    // At rest: draw ELK's crisp obstacle-avoiding orthogonal route as-is.
    path = roundedPath(points);
    const mid = midpointByLength(points);
    labelX = mid.x;
    labelY = mid.y;
  } else {
    // Node moved (or no ELK route at all, e.g. segregated/cross edges): draw a
    // smoothstep path between live anchors computed from each node's current
    // position + measured size and the handle side the transformer assigned.
    const sSide = (sourceHandleId ?? 's-right').replace(/^s-/, '');
    const tSide = (targetHandleId ?? 't-left').replace(/^t-/, '');
    const sAnchor = liveAnchor(sourceNode, sSide);
    const tAnchor = liveAnchor(targetNode, tSide);
    const [smoothPath, smoothLabelX, smoothLabelY] = getSmoothStepPath({
      sourceX: sAnchor.x,
      sourceY: sAnchor.y,
      sourcePosition: sAnchor.position,
      targetX: tAnchor.x,
      targetY: tAnchor.y,
      targetPosition: tAnchor.position,
      borderRadius: CORNER_RADIUS,
    });
    path = smoothPath;
    labelX = smoothLabelX;
    labelY = smoothLabelY;
  }

  const [padX, padY] = labelBgPadding ?? [5, 2];

  // labelStyle / labelBgStyle carry SVG-style props (fill, stroke) because the
  // edge objects predate this HTML-div label. Map them onto CSS equivalents.
  const ls = (labelStyle ?? {}) as Record<string, unknown>;
  const bg = (labelBgStyle ?? {}) as Record<string, unknown>;
  const labelCss: React.CSSProperties = {
    position: 'absolute',
    transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
    pointerEvents: 'all',
    padding: `${padY}px ${padX}px`,
    borderRadius: labelBgBorderRadius ?? 6,
    whiteSpace: 'nowrap',
    color: ls.fill as string | undefined,
    fontSize: ls.fontSize as number | undefined,
    fontWeight: ls.fontWeight as number | undefined,
    background: bg.fill as string | undefined,
    opacity: bg.fillOpacity as number | undefined,
    border: bg.stroke ? `${(bg.strokeWidth as number) ?? 1}px solid ${bg.stroke as string}` : undefined,
  };

  return (
    <>
      <BaseEdge id={id} path={path} markerEnd={markerEnd} style={style} />
      {label && (
        <EdgeLabelRenderer>
          <div style={labelCss} className="nodrag nopan">
            {label}
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  );
}
