import { useCallback } from 'react';
import { JsonView, darkStyles } from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import { useToast } from '../../hooks/useToast';
import type { MinigraphGraphData } from '../../utils/graphTypes';
import styles from './GraphDataView.module.css';

interface GraphDataViewProps {
  graphData: MinigraphGraphData | null;
}

export default function GraphDataView({ graphData }: GraphDataViewProps) {
  const { addToast } = useToast();

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
        <button
          className={styles.copyButton}
          onClick={handleCopy}
          title="Copy raw graph JSON to clipboard"
          aria-label="Copy raw graph JSON to clipboard"
        >
          Copy JSON
        </button>
      </div>

      <div className={styles.scrollBody}>
        <JsonView
          data={graphData as unknown as object}
          shouldExpandNode={(level) => level < 3}
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
