import { Handle, Position, NodeResizer, type NodeProps, type Node } from '@xyflow/react';
import type { GraphNodeData } from '../../utils/graphTransformer';
import { MinigraphNodeBody } from './MinigraphNodeBody';
import { ElkEdge } from './ElkEdge';
import styles from './NodeTypes.module.css';

/** ReactFlow Node type for minigraph nodes. */
export type MinigraphRFNode = Node<GraphNodeData>;

// ─── Resizable node ───────────────────────────────────────────────────────────
//
// Following the official React Flow "node-resizer" example pattern:
//   • The component returns a Fragment — no wrapper <div>.
//   • NodeResizer, Handles and content are all siblings at the top level.
//   • The React Flow wrapper element IS the styled shell; its look is driven
//     by `node.style` set in graphTransformer.ts, not by a CSS class here.
//
// Edge routing is owned by ELK (see graphTransformer.ts / ElkEdge.tsx): every
// ELK-routed edge path is drawn from ELK's computed bend points, so for those
// the handle is just an invisible anchor. For edges ELK did NOT lay out
// (segregated dict/provider/cross edges, drawn as smoothstep) the handle side
// DOES drive the path shape, so each node exposes a source AND target anchor on
// all four sides; the transformer picks the pair that matches the connection's
// geometry (vertical links drop bottom→top, horizontal links go right→left).
const HANDLE_SIDES = [
  { side: 'top',    position: Position.Top },
  { side: 'right',  position: Position.Right },
  { side: 'bottom', position: Position.Bottom },
  { side: 'left',   position: Position.Left },
] as const;

function MinigraphNode({ data, isConnectable, selected }: NodeProps<MinigraphRFNode>) {
  return (
    <>
      {/* Resize handles — visible only when the node is selected */}
      <NodeResizer minWidth={180} minHeight={data.minHeight} isVisible={selected} />

      {/* Source + target anchor on every side. IDs (e.g. "s-bottom" / "t-top")
          are referenced by the transformer when it assigns each edge's handles. */}
      {HANDLE_SIDES.map(({ side, position }) => (
        <Handle
          key={`s-${side}`}
          id={`s-${side}`}
          type="source"
          position={position}
          isConnectable={isConnectable}
          className={styles.edgeHandle}
        />
      ))}
      {HANDLE_SIDES.map(({ side, position }) => (
        <Handle
          key={`t-${side}`}
          id={`t-${side}`}
          type="target"
          position={position}
          isConnectable={isConnectable}
          className={styles.edgeHandle}
        />
      ))}

      {/*
        * Content container — fills the React Flow wrapper (which carries the
        * border/background via node.style) and clips its own overflow.
        */}
      <MinigraphNodeBody
        alias={data.alias}
        nodeType={data.nodeType}
        properties={data.properties}
      />
    </>
  );
}

// ─── Exported nodeTypes map for <ReactFlow nodeTypes={...}> ──────────────────
//
// ReactFlow matches the `type` field on each node object to this map.
// Our transformer sets node.type = n.types[0], so we need an entry per known
// type plus a "default" fallback for anything unrecognised.
export const nodeTypes = {
  Root:        MinigraphNode,
  End:         MinigraphNode,
  Fetcher:     MinigraphNode,
  mapper:      MinigraphNode,
  Math:        MinigraphNode,
  JavaScript:  MinigraphNode,
  Provider:    MinigraphNode,
  Dictionary:  MinigraphNode,
  Join:        MinigraphNode,
  Extension:   MinigraphNode,
  Island:      MinigraphNode,
  Decision:    MinigraphNode,
  default:     MinigraphNode,
} as const;

// ─── Exported edgeTypes map for <ReactFlow edgeTypes={...}> ──────────────────
// The transformer tags every edge with type 'elk'; ElkEdge draws ELK's routed
// polyline (or a smoothstep fallback for non-laid-out edges).
export const edgeTypes = {
  elk: ElkEdge,
} as const;
