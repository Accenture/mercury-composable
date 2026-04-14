import type { ClipboardItemRecord } from '../../clipboard/db';
import { formatRelativeTime } from '../../utils/timeFormat';
import styles from './ClipboardItem.module.css';

interface ClipboardItemProps {
  item: ClipboardItemRecord;
  connected: boolean;
  onPaste: (item: ClipboardItemRecord) => void;
  onRemove: () => void;
  onInspect: () => void;
}

export function ClipboardItem({ item, connected, onPaste, onRemove, onInspect }: ClipboardItemProps) {
  const { node, connections, clippedAt, sourceLabel } = item;
  const primaryType = node.types[0] ?? '—';
  const skill = node.properties.skill ?? '—';

  // Truncated props summary
  const propKeys = Object.entries(node.properties)
    .filter(([k]) => k !== 'skill')
    .map(([k, v]) => {
      const val = typeof v === 'string' ? v : JSON.stringify(v);
      return `${k}=${val && val.length > 30 ? val.slice(0, 30) + '…' : val}`;
    });
  const propsSummary = propKeys.length > 0 ? propKeys.join(', ') : '—';

  // Connection counts
  const outgoing = connections.filter(c => c.source === node.alias).length;
  const incoming = connections.filter(c => c.target === node.alias).length;
  const connSummary = `${connections.length} (${outgoing} out, ${incoming} in)`;

  return (
    <div className={styles.card}>
      <div className={styles.alias}>{node.alias}</div>
      <div className={styles.meta}>Type: {primaryType}</div>
      <div className={styles.meta}>Skill: {skill}</div>
      <div className={styles.meta} title={propsSummary}>
        <span className={styles.propsLine}>Props: {propsSummary}</span>
      </div>
      <div className={styles.meta}>Connections: {connSummary}</div>
      <div className={styles.timestamp}>
        Clipped {formatRelativeTime(clippedAt)} from {sourceLabel}
      </div>
      <div className={styles.actions}>
        <button
          className={styles.pasteBtn}
          onClick={() => onPaste(item)}
          disabled={!connected}
          aria-label={`Paste node ${node.alias}`}
        >
          Paste
        </button>
        <button
          className={styles.inspectBtn}
          onClick={onInspect}
          aria-label={`Inspect node ${node.alias}`}
        >
          Describe
        </button>
        <button
          className={styles.removeBtn}
          onClick={onRemove}
          aria-label={`Remove node ${node.alias} from clipboard`}
        >
          Remove
        </button>
      </div>
    </div>
  );
}
