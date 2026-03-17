import { useId } from 'react';
import PayloadEditor from '../PayloadEditor/PayloadEditor';
import MarkdownPreview from '../MarkdownPreview/MarkdownPreview';
import GraphView from '../GraphView/GraphView';
import GraphDataView from '../GraphDataView/GraphDataView';
import styles from './RightPanel.module.css';
import { type ValidationResult } from '../../utils/validators';
import type { MinigraphGraphData } from '../../utils/graphTypes';

export type RightTab = 'payload' | 'preview' | 'graph' | 'graph-data';

interface RightPanelProps {
  /** Ordered list of tabs to render for this playground (from PlaygroundConfig). */
  tabs:           RightTab[];
  payload:        string;
  onChange:       (value: string) => void;
  validation:     ValidationResult;
  onFormat:       () => void;
  onUpload?:      () => void;
  previewMessage: string | null;
  pinnedMessage:  string | null;
  graphData:      MinigraphGraphData | null;
  activeTab:      RightTab;
  onTabChange:    (tab: RightTab) => void;
  onGraphRenderError?: (message: string) => void;
  /** Called after the raw graph JSON is successfully copied from the Graph Data tab. */
  onGraphDataCopySuccess?: () => void;
  /** Called when the clipboard write fails from the Graph Data tab. */
  onGraphDataCopyError?:   () => void;
  /** When true, forwards the loading-overlay state to GraphView. */
  isGraphRefreshing?:      boolean;
}

export default function RightPanel({
  tabs,
  payload,
  onChange,
  validation,
  onFormat,
  onUpload,
  previewMessage,
  pinnedMessage,
  graphData,
  activeTab,
  onTabChange,
  onGraphRenderError,
  onGraphDataCopySuccess,
  onGraphDataCopyError,
  isGraphRefreshing,
}: RightPanelProps) {
  const uid              = useId();
  const payloadPanelId   = `${uid}-tab-payload`;
  const previewPanelId   = `${uid}-tab-preview`;
  const graphPanelId     = `${uid}-tab-graph`;
  const graphDataPanelId = `${uid}-tab-graph-data`;

  return (
    <div className={styles.rightPanel}>
      {/* Tab strip — only tabs listed in `tabs` are rendered */}
      <div className={styles.tabStrip} role="tablist" aria-label="Right panel tabs">
        {tabs.includes('payload') && (
          <button
            role="tab"
            aria-selected={activeTab === 'payload'}
            aria-controls={payloadPanelId}
            className={`${styles.tab}${activeTab === 'payload' ? ` ${styles.tabActive}` : ''}`}
            onClick={() => onTabChange('payload')}
          >
            Payload Editor
          </button>
        )}
        {tabs.includes('preview') && (
          <button
            role="tab"
            aria-selected={activeTab === 'preview'}
            aria-controls={previewPanelId}
            className={`${styles.tab}${activeTab === 'preview' ? ` ${styles.tabActive}` : ''}`}
            onClick={() => onTabChange('preview')}
          >
            Developer Guides
            {pinnedMessage !== null && (
              <span className={styles.pinnedBadge} aria-label="Message pinned">📌</span>
            )}
          </button>
        )}
        {tabs.includes('graph') && (
          <button
            role="tab"
            aria-selected={activeTab === 'graph'}
            aria-controls={graphPanelId}
            className={`${styles.tab}${activeTab === 'graph' ? ` ${styles.tabActive}` : ''}`}
            onClick={() => onTabChange('graph')}
          >
            Graph
            {graphData !== null && (
              <span className={styles.pinnedBadge} aria-label="Graph data available">🕸️</span>
            )}
          </button>
        )}
        {tabs.includes('graph-data') && (
          <button
            role="tab"
            aria-selected={activeTab === 'graph-data'}
            aria-controls={graphDataPanelId}
            className={`${styles.tab}${activeTab === 'graph-data' ? ` ${styles.tabActive}` : ''}`}
            onClick={() => onTabChange('graph-data')}
          >
            Graph Data (Raw)
          </button>
        )}
      </div>

      {/* Payload Editor tab body — only mounted when enabled for this playground */}
      {tabs.includes('payload') && (
        <div
          role="tabpanel"
          id={payloadPanelId}
          tabIndex={activeTab === 'payload' ? 0 : -1}
          className={`${styles.tabBody}${activeTab !== 'payload' ? ` ${styles.tabBodyHidden}` : ''}`}
        >
          <PayloadEditor
            payload={payload}
            onChange={onChange}
            validation={validation}
            onFormat={onFormat}
            onUpload={onUpload}
          />
        </div>
      )}

      {/* Developer Guides tab body — only mounted when enabled for this playground */}
      {tabs.includes('preview') && (
        <div
          role="tabpanel"
          id={previewPanelId}
          tabIndex={activeTab === 'preview' ? 0 : -1}
          className={`${styles.tabBody}${activeTab !== 'preview' ? ` ${styles.tabBodyHidden}` : ''}`}
        >
          <MarkdownPreview message={previewMessage} />
        </div>
      )}

      {/* Graph View tab body — always mounted when enabled to preserve zoom/pan state */}
      {tabs.includes('graph') && (
        <div
          role="tabpanel"
          id={graphPanelId}
          tabIndex={activeTab === 'graph' ? 0 : -1}
          className={`${styles.tabBody}${activeTab !== 'graph' ? ` ${styles.tabBodyHidden}` : ''}`}
        >
          <GraphView
            graphData={graphData}
            onRenderError={onGraphRenderError}
            isRefreshing={isGraphRefreshing}
            onCopySuccess={onGraphDataCopySuccess}
            onCopyError={onGraphDataCopyError}
          />
        </div>
      )}

      {/* Graph Data tab body — always mounted when enabled; shows pretty-printed raw JSON */}
      {tabs.includes('graph-data') && (
        <div
          role="tabpanel"
          id={graphDataPanelId}
          tabIndex={activeTab === 'graph-data' ? 0 : -1}
          className={`${styles.tabBody}${activeTab !== 'graph-data' ? ` ${styles.tabBodyHidden}` : ''}`}
        >
          <GraphDataView
            graphData={graphData}
            onCopySuccess={onGraphDataCopySuccess}
            onCopyError={onGraphDataCopyError}
          />
        </div>
      )}

    </div>
  );
}
