import { useState } from 'react';
import { useClipboardContext } from '../../contexts/ClipboardContext';
import type { ClipboardItemRecord } from '../../clipboard/db';
import { ClipboardItem } from './ClipboardItem';
import { ClipboardEmptyState } from './ClipboardEmptyState';
import styles from './ClipboardSidebar.module.css';
import { JsonView, darkStyles } from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';

interface ClipboardSidebarProps {
  connected: boolean;
  onPaste: (item: ClipboardItemRecord) => void;
}

export default function ClipboardSidebar({ connected, onPaste }: ClipboardSidebarProps) {
  const clipboardCtx = useClipboardContext();
  const [inspectItem, setInspectItem] = useState<ClipboardItemRecord | null>(null);

  return (
    <div className={styles.sidebar}>
      <div className={styles.header}>
        <span className={styles.headerTitle}>Clipboard</span>
        {clipboardCtx.items.length > 0 && (
          <button
            className={styles.clearBtn}
            onClick={() => clipboardCtx.clearAll()}
            aria-label="Clear all clipboard items"
          >
            Clear
          </button>
        )}
      </div>

      <div className={styles.itemList}>
        {clipboardCtx.isLoading ? (
          <div className={styles.loading}>Loading…</div>
        ) : clipboardCtx.items.length === 0 ? (
          <ClipboardEmptyState />
        ) : (
          clipboardCtx.items.map(item => (
            <ClipboardItem
              key={item.id}
              item={item}
              connected={connected}
              onPaste={onPaste}
              onRemove={() => clipboardCtx.removeItem(item.id)}
              onInspect={() => setInspectItem(inspectItem?.id === item.id ? null : item)}
            />
          ))
        )}
      </div>

      {/* Inspect panel (inline expand) */}
      {inspectItem && (
        <div className={styles.inspectPanel}>
          <div className={styles.inspectHeader}>
            <span>Inspect: {inspectItem.node.alias}</span>
            <button
              className={styles.inspectClose}
              onClick={() => setInspectItem(null)}
              aria-label="Close inspect panel"
            >
              ✕
            </button>
          </div>
          <div className={styles.inspectBody}>
            <JsonView
              data={{ node: inspectItem.node, connections: inspectItem.connections }}
              style={darkStyles}
            />
          </div>
        </div>
      )}
    </div>
  );
}
