import { useState } from 'react';
import { JsonView, darkStyles, allExpanded, collapseAllNested } from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import type { MinigraphGraphData } from '../../utils/graphTypes';
import GraphToolbar from '../GraphToolbar/GraphToolbar';
import styles from './GraphDataView.module.css';

// ---------------------------------------------------------------------------
// Stable shouldExpandNode functions — defined at module level so their
// references never change between renders, which would force react-json-view-lite
// to re-evaluate every node on every keystroke / state update elsewhere.
// ---------------------------------------------------------------------------

/** Expand every node in the tree. Re-uses the library's own stable export. */
const expandAll = allExpanded;

/** Collapse to top-level only. Re-uses the library's own stable export. */
const collapseAll = collapseAllNested;

/** Default view: expand nodes up to depth 2 (nodes + their direct fields). */
const expandDefault = (level: number) => level < 3;

type ExpandMode = 'default' | 'all' | 'none';

const EXPAND_FN: Record<ExpandMode, (level: number) => boolean> = {
  default: expandDefault,
  all:     expandAll,
  none:    collapseAll,
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Derive a sensible default save name from the graph data.
 * Uses the root node's `name` property if present, otherwise falls back to
 * the entry-point node's alias, or a timestamped default.
 */
export function deriveDefaultName(graphData: MinigraphGraphData): string {
  const root = graphData.nodes.find(n => n.types.includes('entry_point'));
  const nameFromRoot = root?.properties?.name as string | undefined;
  if (nameFromRoot && typeof nameFromRoot === 'string') return nameFromRoot;
  if (root) return root.alias;
  const first = graphData.nodes[0];
  if (first) return first.alias;
  return `graph-${new Date().toISOString().slice(0, 10)}`;
}

interface GraphDataViewProps {
  graphData:       MinigraphGraphData | null;
  /** Called after the raw graph JSON is successfully copied to the clipboard. */
  onCopySuccess?:  () => void;
  /** Called when the clipboard write fails. */
  onCopyError?:    () => void;
}

export default function GraphDataView({ graphData, onCopySuccess, onCopyError }: GraphDataViewProps) {
  const [expandMode, setExpandMode] = useState<ExpandMode>('all');

  if (!graphData) {
    return (
      <div className={styles.root}>
        <div className={styles.empty}>
          <span className={styles.emptyIcon}>🕸️</span>
          <span>No graph data yet.</span>
          <span>
            Pin a graph-link message in the Console to load the raw data here.
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.root}>
      <GraphToolbar
        graphData={graphData}
        onCopySuccess={onCopySuccess}
        onCopyError={onCopyError}
        extraActions={
          <>
            <button
              className={styles.toolbarButton}
              onClick={() => setExpandMode('all')}
              title="Expand all nodes"
              aria-label="Expand all JSON nodes"
              aria-pressed={expandMode === 'all'}
            >
              ➖
            </button>
            <button
              className={styles.toolbarButton}
              onClick={() => setExpandMode('none')}
              title="Collapse all nodes"
              aria-label="Collapse all JSON nodes"
              aria-pressed={expandMode === 'none'}
            >
              ➕
            </button>
          </>
        }
      />

      <div className={styles.scrollBody}>
        <JsonView
          data={graphData as unknown as object}
          shouldExpandNode={EXPAND_FN[expandMode]}
          style={{
            ...darkStyles,
            container: `${darkStyles.container} ${styles.jsonContainer}`,
            label:        styles.jsonLabel,
            stringValue:  styles.jsonString,
            numberValue:  styles.jsonNumber,
            booleanValue: styles.jsonBoolean,
            nullValue:    styles.jsonNull,
          }}
        />
      </div>
    </div>
  );
}
