import { useState, useCallback } from 'react';
import { JsonView, darkStyles, allExpanded, collapseAllNested } from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import { useToast } from '../../hooks/useToast';
import type { MinigraphGraphData } from '../../utils/graphTypes';
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

interface GraphDataViewProps {
  graphData: MinigraphGraphData | null;
}

export default function GraphDataView({ graphData }: GraphDataViewProps) {
  const { addToast } = useToast();
  const [expandMode, setExpandMode] = useState<ExpandMode>('all'); // set to default for 2 levels;

  const handleCopy = useCallback(() => {
    if (!graphData) return;
    navigator.clipboard
      .writeText(JSON.stringify(graphData, null, 2))
      .then(() => addToast('Graph JSON copied to clipboard!', 'success'))
      .catch(() => addToast('Copy failed', 'error'));
  }, [graphData, addToast]);

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
      <div className={styles.toolbar}>
        <span className={styles.label}>
          {graphData.nodes.length} node{graphData.nodes.length !== 1 ? 's' : ''}
          {' · '}
          {(graphData.connections ?? []).length} connection{(graphData.connections ?? []).length !== 1 ? 's' : ''}
        </span>
        <div className={styles.toolbarActions}>
          <button
            className={`${styles.copyButton}${expandMode === 'all' ? ` ${styles.copyButtonActive}` : ''}`}
            onClick={() => setExpandMode('all')}
            title="Expand all nodes"
            aria-label="Expand all JSON nodes"
            aria-pressed={expandMode === 'all'}
          >
            Expand All
          </button>
          <button
            className={`${styles.copyButton}${expandMode === 'none' ? ` ${styles.copyButtonActive}` : ''}`}
            onClick={() => setExpandMode('none')}
            title="Collapse all nodes"
            aria-label="Collapse all JSON nodes"
            aria-pressed={expandMode === 'none'}
          >
            Collapse All
          </button>
          <button
            className={styles.copyButton}
            onClick={handleCopy}
            title="Copy raw graph JSON to clipboard"
            aria-label="Copy raw graph JSON to clipboard"
          >
            Copy JSON
          </button>
        </div>
      </div>

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
