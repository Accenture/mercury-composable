import { useId } from 'react';
import PayloadEditor from '../PayloadEditor/PayloadEditor';
import MarkdownPreview from '../MarkdownPreview/MarkdownPreview';
import GraphView from '../GraphView/GraphView';
import styles from './RightPanel.module.css';
import { type ValidationResult } from '../../utils/validators';
import type { MinigraphGraphData } from '../../utils/graphTypes';

export type RightTab = 'payload' | 'preview' | 'graph';

interface RightPanelProps {
  payload:        string;
  onChange:       (value: string) => void;
  validation:     ValidationResult;
  onFormat:       () => void;
  previewMessage: string | null;
  pinnedMessage:  string | null;
  graphData:      MinigraphGraphData | null;
  activeTab:      RightTab;
  onTabChange:    (tab: RightTab) => void;
}

export default function RightPanel({
  payload,
  onChange,
  validation,
  onFormat,
  previewMessage,
  pinnedMessage,
  graphData,
  activeTab,
  onTabChange,
}: RightPanelProps) {
  const uid            = useId();
  const payloadPanelId = `${uid}-tab-payload`;
  const previewPanelId = `${uid}-tab-preview`;
  const graphPanelId   = `${uid}-tab-graph`;

  return (
    <div className={styles.rightPanel}>
      {/* Tab strip */}
      <div className={styles.tabStrip} role="tablist" aria-label="Right panel tabs">
        <button
          role="tab"
          aria-selected={activeTab === 'payload'}
          aria-controls={payloadPanelId}
          className={`${styles.tab}${activeTab === 'payload' ? ` ${styles.tabActive}` : ''}`}
          onClick={() => onTabChange('payload')}
        >
          Payload Editor
        </button>
        <button
          role="tab"
          aria-selected={activeTab === 'preview'}
          aria-controls={previewPanelId}
          className={`${styles.tab}${activeTab === 'preview' ? ` ${styles.tabActive}` : ''}`}
          onClick={() => onTabChange('preview')}
        >
          Markdown Preview
          {pinnedMessage !== null && (
            <span className={styles.pinnedBadge} aria-label="Message pinned">📌</span>
          )}
        </button>
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
      </div>

      {/* Payload Editor tab body — always mounted */}
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
        />
      </div>

      {/* Markdown Preview tab body — always mounted */}
      <div
        role="tabpanel"
        id={previewPanelId}
        tabIndex={activeTab === 'preview' ? 0 : -1}
        className={`${styles.tabBody}${activeTab !== 'preview' ? ` ${styles.tabBodyHidden}` : ''}`}
      >
        <MarkdownPreview message={previewMessage} />
      </div>

      {/* Graph View tab body — always mounted to preserve zoom/pan state */}
      <div
        role="tabpanel"
        id={graphPanelId}
        tabIndex={activeTab === 'graph' ? 0 : -1}
        className={`${styles.tabBody}${activeTab !== 'graph' ? ` ${styles.tabBodyHidden}` : ''}`}
      >
        <GraphView graphData={graphData} />
      </div>
    </div>
  );
}
