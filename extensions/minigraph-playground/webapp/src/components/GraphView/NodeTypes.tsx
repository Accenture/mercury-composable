import { Handle, Position, type NodeProps, type Node } from '@xyflow/react';
import type { GraphNodeData } from '../../utils/graphTransformer';
import styles from './NodeTypes.module.css';

/** ReactFlow Node type for minigraph nodes. */
export type MinigraphRFNode = Node<GraphNodeData>;

// ─── Per-type metadata ────────────────────────────────────────────────────────
const TYPE_META: Record<string, { icon: string; label: string; colorClass: string }> = {
  entry_point: { icon: '🚀', label: 'Entry Point', colorClass: styles.entryPoint },
  api_fetcher: { icon: '🌐', label: 'API Fetcher',  colorClass: styles.apiFetcher  },
  mapper:      { icon: '🗺️', label: 'Mapper',       colorClass: styles.mapper      },
  terminator:  { icon: '🏁', label: 'Terminator',   colorClass: styles.terminator  },
};

function getMeta(nodeType: string) {
  return TYPE_META[nodeType] ?? { icon: '📦', label: nodeType, colorClass: styles.unknown };
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
        <div key={i} className={styles.row}>
          <span className={styles.label}>{i === 0 ? 'map' : ''}</span>
          <span className={styles.value} title={m}>{m}</span>
        </div>
      ))}
    </>
  );
}

// ─── Shared node shell ────────────────────────────────────────────────────────
function MinigraphNode({ data, isConnectable }: NodeProps<MinigraphRFNode>) {
  const meta = getMeta(data.nodeType);
  return (
    <div className={`${styles.node} ${meta.colorClass}`}>
      {/* Target handle (left) */}
      <Handle type="target" position={Position.Left} isConnectable={isConnectable} />

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

      {/* Source handle (right) */}
      <Handle type="source" position={Position.Right} isConnectable={isConnectable} />
    </div>
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
