import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import type { MinigraphGraphData } from '../../utils/graphTypes';
import { CopyIcon, DownloadIcon, ImageIcon, ImportIcon } from '../../icons/GraphToolbarIcons';
import { graphFileName, suggestGraphId } from '../../utils/graphFile';
import type { GraphImageFormat } from '../../utils/graphImage';
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
  /** Saves an image of the whole graph as `<graph-id>.png` or `<graph-id>.svg` (the Export image menu). */
  onExportImage?: (format: GraphImageFormat) => void;
  /** True while an image is being rendered: the Export image button is disabled until it is saved. */
  exportingImage?: boolean;
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
  onExportImage,
  exportingImage = false,
}: GraphToolbarProps) {
  const handleCopy = useCallback(() => {
    if (!graphData) return;
    navigator.clipboard
      .writeText(JSON.stringify(graphData, null, 2))
      .then(() => onCopySuccess?.())
      .catch(() => onCopyError?.());
  }, [graphData, onCopySuccess, onCopyError]);

  // ── Export image menu ──
  // A lightweight menu like the canvas context menus: the first item takes
  // focus, an outside pointer down or Escape closes it, focus is not trapped.
  const [exportMenuOpen, setExportMenuOpen] = useState(false);
  const exportMenuAnchorRef = useRef<HTMLDivElement>(null);
  const exportButtonRef = useRef<HTMLButtonElement>(null);
  const firstExportItemRef = useRef<HTMLButtonElement>(null);
  const canExportImage = Boolean(graphData) && !exportingImage;

  useEffect(() => {
    if (!exportMenuOpen) return;
    firstExportItemRef.current?.focus();

    const handlePointerDown = (event: PointerEvent) => {
      if (exportMenuAnchorRef.current && !exportMenuAnchorRef.current.contains(event.target as Node)) {
        setExportMenuOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      setExportMenuOpen(false);
      exportButtonRef.current?.focus();
    };

    document.addEventListener('pointerdown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [exportMenuOpen]);

  // The export runs straight from the click: the browser opens its "save as"
  // dialog only shortly after a user gesture.
  const handleExport = (format: GraphImageFormat) => {
    setExportMenuOpen(false);
    onExportImage?.(format);
  };

  const graphId         = suggestGraphId(graphName);
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
              ? `Download the graph as ${graphFileName(graphId)}`
              : 'No graph to download'}
            aria-label="Download graph as a JSON file"
          >
            <DownloadIcon className={styles.toolbarIcon} aria-hidden="true" focusable="false" />
          </button>
        )}
        {onExportImage && (
          <div className={styles.menuAnchor} ref={exportMenuAnchorRef}>
            <button
              ref={exportButtonRef}
              type="button"
              className={`${styles.toolbarButton} ${styles.toolbarIconButton}`}
              onClick={() => setExportMenuOpen((open) => !open)}
              disabled={!canExportImage}
              title={exportingImage
                ? 'Exporting the graph image…'
                : graphData
                  ? `Export the whole graph as an image (${graphId}.png or ${graphId}.svg)`
                  : 'No graph to export'}
              aria-label="Export graph as an image"
              aria-haspopup="menu"
              aria-expanded={exportMenuOpen}
            >
              <ImageIcon className={styles.toolbarIcon} aria-hidden="true" focusable="false" />
            </button>
            {exportMenuOpen && canExportImage && (
              <div className={styles.menu} role="menu" aria-label="Export graph image">
                <button
                  ref={firstExportItemRef}
                  role="menuitem"
                  type="button"
                  className={styles.menuItem}
                  onClick={() => handleExport('png')}
                >
                  Export PNG
                </button>
                <button
                  role="menuitem"
                  type="button"
                  className={styles.menuItem}
                  onClick={() => handleExport('svg')}
                >
                  Export SVG
                </button>
              </div>
            )}
          </div>
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
