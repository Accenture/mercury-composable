import { useState, useId } from 'react';
import PayloadEditor from '../PayloadEditor/PayloadEditor';
import MarkdownPreview from '../MarkdownPreview/MarkdownPreview';
import styles from './RightPanel.module.css';
import { type ValidationResult } from '../../utils/validators';

export type RightTab = 'payload' | 'preview';

interface RightPanelProps {
  payload:        string;
  onChange:       (value: string) => void;
  validation:     ValidationResult;
  onFormat:       () => void;
  previewMessage: string | null;
  pinnedMessage:  string | null;
}

export default function RightPanel({
  payload,
  onChange,
  validation,
  onFormat,
  previewMessage,
  pinnedMessage,
}: RightPanelProps) {
  const [activeTab, setActiveTab] = useState<RightTab>('payload');
  const uid            = useId();
  const payloadPanelId = `${uid}-tab-payload`;
  const previewPanelId = `${uid}-tab-preview`;

  return (
    <div className={styles.rightPanel}>
      {/* Tab strip */}
      <div className={styles.tabStrip} role="tablist" aria-label="Right panel tabs">
        <button
          role="tab"
          aria-selected={activeTab === 'payload'}
          aria-controls={payloadPanelId}
          className={`${styles.tab}${activeTab === 'payload' ? ` ${styles.tabActive}` : ''}`}
          onClick={() => setActiveTab('payload')}
        >
          Payload Editor
        </button>
        <button
          role="tab"
          aria-selected={activeTab === 'preview'}
          aria-controls={previewPanelId}
          className={`${styles.tab}${activeTab === 'preview' ? ` ${styles.tabActive}` : ''}`}
          onClick={() => setActiveTab('preview')}
        >
          Markdown Preview
          {pinnedMessage !== null && (
            <span className={styles.pinnedBadge} aria-label="Message pinned">📌</span>
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
    </div>
  );
}
