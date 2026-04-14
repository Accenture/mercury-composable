import { Handle, Position, NodeResizer, type NodeProps, type Node } from '@xyflow/react';
import type { GraphNodeData } from '../../utils/graphTransformer';
import styles from './NodeTypes.module.css';

/** ReactFlow Node type for minigraph nodes. */
export type MinigraphRFNode = Node<GraphNodeData>;

// ─── Per-type metadata ────────────────────────────────────────────────────────
const TYPE_META: Record<string, { icon: string; label: string }> = {
  Root:        { icon: '🚀', label: 'Root'         },
  End:         { icon: '🏁', label: 'End'          },
  Fetcher:     { icon: '🌐', label: 'Fetcher'      },
  mapper:      { icon: '🗺️', label: 'Mapper'       },
  Math:        { icon: '🔢', label: 'Math'         },
  JavaScript:  { icon: '📜', label: 'JavaScript'   },
  Provider:    { icon: '🔌', label: 'Provider'     },
  Dictionary:  { icon: '📖', label: 'Dictionary'   },
  Join:        { icon: '🔀', label: 'Join'         },
  Extension:   { icon: '🧩', label: 'Extension'    },
  Island:      { icon: '🏝️', label: 'Island'       },
  Decision:    { icon: '❓', label: 'Decision'     },
};

function getMeta(nodeType: string) {
  return TYPE_META[nodeType] ?? { icon: '📦', label: nodeType };
}

// ─── Shared detail rows ───────────────────────────────────────────────────────

/** Render a single key=value row. */
function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className={styles.row}>
      <span className={styles.label}>{label}</span>
      <span className={styles.value} title={value}>{value}</span>
    </div>
  );
}

/** Render all properties generically. Arrays get one row per element. */
function PropertyRows({ properties }: { properties: Record<string, unknown> }) {
  const entries = Object.entries(properties).filter(
    ([, v]) => v !== undefined && v !== null,
  );
  if (entries.length === 0) return null;

  return (
    <>
      {entries.map(([key, value]) => {
        if (Array.isArray(value)) {
          return value.map((item, i) => {
            const str = typeof item === 'string' ? item : JSON.stringify(item);
            return (
              <Row key={`${key}-${i}`} label={i === 0 ? key : ''} value={str} />
            );
          });
        }
        const str = typeof value === 'string' ? value : JSON.stringify(value);
        return <Row key={key} label={key} value={str} />;
      })}
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
          <PropertyRows properties={data.properties} />
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
