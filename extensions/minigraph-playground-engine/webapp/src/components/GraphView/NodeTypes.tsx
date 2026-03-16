import { Handle, Position, NodeResizer, type NodeProps, type Node } from '@xyflow/react';
import type { GraphNodeData } from '../../utils/graphTransformer';
import styles from './NodeTypes.module.css';

/** ReactFlow Node type for minigraph nodes. */
export type MinigraphRFNode = Node<GraphNodeData>;

// ─── Per-type metadata ────────────────────────────────────────────────────────
const TYPE_META: Record<string, { icon: string; label: string }> = {
  entry_point: { icon: '🚀', label: 'Entry Point' },
  api_fetcher: { icon: '🌐', label: 'API Fetcher'  },
  mapper:      { icon: '🗺️', label: 'Mapper'       },
  terminator:  { icon: '🏁', label: 'Terminator'   },
};

function getMeta(nodeType: string) {
  return TYPE_META[nodeType] ?? { icon: '📦', label: nodeType };
}

// ─── Shared detail rows ───────────────────────────────────────────────────────
function Row({ label, value }: { label: string; value?: string }) {
  if (!value) return null;
  return (
    <div className={styles.row}>
      <span className={styles.label}>{label}</span>
      <span className={styles.value} title={value}>{value}</span>
    </div>
  );
}

function MappingRows({ mapping }: { mapping?: string[] }) {
  if (!mapping?.length) return null;
  return (
    <>
      {mapping.map((m, i) => (
        <div key={m} className={styles.row}>
          <span className={styles.label}>{i === 0 ? 'map' : ''}</span>
          <span className={styles.value} title={m}>{m}</span>
        </div>
      ))}
    </>
  );
}

// ─── Resizable node ───────────────────────────────────────────────────────────
//
// Following the official React Flow "node-resizer" example pattern:
//   • The component returns a Fragment — no wrapper <div>.
//   • NodeResizer, Handles and content are all siblings at the top level.
//   • The React Flow wrapper element IS the styled shell; its look is driven
//     by `node.style` set in graphTransformer.ts, not by a CSS class here.
//   • This eliminates every wrapper-sizing workaround that was previously
//     needed (initialWidth/initialHeight tricks, CSS overrides for
//     .react-flow__node-default, overflow:visible hacks, etc.).
function MinigraphNode({ data, isConnectable, selected }: NodeProps<MinigraphRFNode>) {
  const meta = getMeta(data.nodeType);
  return (
    <>
      {/* Resize handles — visible only when the node is selected */}
      <NodeResizer minWidth={180} minHeight={60} isVisible={selected} />

      {/* Target handle (left) */}
      <Handle type="target" position={Position.Left} isConnectable={isConnectable} />

      {/*
        * Content container — fills the React Flow wrapper (which carries the
        * border/background via node.style) and clips its own overflow.
        * width/height:100% make it track the wrapper when the user resizes.
        */}
      <div className={styles.content}>
        <div className={styles.header}>
          <span className={styles.icon}>{meta.icon}</span>
          <span className={styles.alias}>{data.alias}</span>
          <span className={styles.badge}>{meta.label}</span>
        </div>

        <div className={styles.body}>
          <Row label="skill"    value={data.skill} />
          <Row label="question" value={data.question} />
          <Row label="desc"     value={data.description} />
          <MappingRows mapping={data.mapping} />
        </div>
      </div>

      {/* Source handle (right) */}
      <Handle type="source" position={Position.Right} isConnectable={isConnectable} />
    </>
  );
}

// ─── Exported nodeTypes map for <ReactFlow nodeTypes={...}> ──────────────────
//
// ReactFlow matches the `type` field on each node object to this map.
// Our transformer sets node.type = n.types[0], so we need an entry per known
// type plus a "default" fallback for anything unrecognised.
export const nodeTypes = {
  entry_point: MinigraphNode,
  api_fetcher: MinigraphNode,
  mapper:      MinigraphNode,
  terminator:  MinigraphNode,
  default:     MinigraphNode,
} as const;
