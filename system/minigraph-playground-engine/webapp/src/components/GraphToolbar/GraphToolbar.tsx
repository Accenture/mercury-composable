import { useCallback, type ReactNode } from 'react';
import type { MinigraphGraphData } from '../../utils/graphTypes';
import { CopyIcon, DownloadIcon, ImportIcon } from '../../icons/GraphToolbarIcons';
import { graphFileName, suggestGraphId } from '../../utils/graphFile';
import styles from './GraphToolbar.module.css';

interface GraphToolbarProps {
  graphData:      MinigraphGraphData | null;
  /** Resolved display name for the graph (root node name or fallback). */
  graphName?:     string;
  onCopySuccess?: () => void;
  onCopyError?:   () => void;
  /** Optional extra action buttons rendered before the copy button. */
  extraActions?:  ReactNode;
  /** Opens the file picker to import a graph model from a JSON file (replaces the draft). */
  onImport?:      () => void;
  /** When set, Import is disabled and this is its tooltip (e.g. the WebSocket is not connected). */
  importDisabledReason?: string | null;
  /** Opens the download dialog that saves the graph as `<graph-id>.json`. */
  onDownload?:    () => void;
}

export default function GraphToolbar({
  graphData,
  graphName,
  onCopySuccess,
  onCopyError,
  extraActions,
  onImport,
  importDisabledReason = null,
  onDownload,
}: GraphToolbarProps) {
  const handleCopy = useCallback(() => {
    if (!graphData) return;
    navigator.clipboard
      .writeText(JSON.stringify(graphData, null, 2))
      .then(() => onCopySuccess?.())
      .catch(() => onCopyError?.());
  }, [graphData, onCopySuccess, onCopyError]);

  const nodeCount       = graphData?.nodes.length ?? 0;
  const connectionCount = (graphData?.connections ?? []).length;

  return (
    <div className={styles.toolbar}>
      <div className={styles.nameGroup}>
        <span className={styles.graphName}>{graphName ?? 'Untitled'}</span>
        <span className={styles.stats}>
          {nodeCount} node{nodeCount !== 1 ? 's' : ''}
          {' · '}
          {connectionCount} connection{connectionCount !== 1 ? 's' : ''}
        </span>
      </div>

      <div className={styles.toolbarActions}>
        {extraActions}
        {onImport && (
          <button
            type="button"
            className={`${styles.toolbarButton} ${styles.toolbarIconButton}`}
            onClick={onImport}
            disabled={importDisabledReason !== null}
            title={importDisabledReason ?? 'Import a graph model from a JSON file (replaces the current draft)'}
            aria-label="Import graph model from a JSON file"
          >
            <ImportIcon className={styles.toolbarIcon} aria-hidden="true" focusable="false" />
          </button>
        )}
        {onDownload && (
          <button
            type="button"
            className={`${styles.toolbarButton} ${styles.toolbarIconButton}`}
            onClick={onDownload}
            disabled={!graphData}
            title={graphData
              ? `Download the graph as ${graphFileName(suggestGraphId(graphName))}`
              : 'No graph to download'}
            aria-label="Download graph as a JSON file"
          >
            <DownloadIcon className={styles.toolbarIcon} aria-hidden="true" focusable="false" />
          </button>
        )}
        <button
          type="button"
          className={`${styles.toolbarButton} ${styles.toolbarIconButton}`}
          onClick={handleCopy}
          title="Copy raw graph JSON to clipboard"
          aria-label="Copy raw graph JSON to clipboard"
        >
          <CopyIcon className={styles.toolbarIcon} aria-hidden="true" focusable="false" />
        </button>
      </div>
    </div>
  );
}
