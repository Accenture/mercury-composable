import styles from './ConnectionBar.module.css';

interface ConnectionBarProps {
  connected:    boolean;
  connecting:   boolean;
  url:          string;
  onConnect:    () => void;
  onDisconnect: () => void;
}

export default function ConnectionBar({
  connected, connecting, url, onConnect, onDisconnect
}: ConnectionBarProps) {
  const dotClass = connected
    ? styles.dotConnected
    : connecting
      ? styles.dotConnecting
      : styles.dotIdle;

  const dotLabel = connected ? 'Connected' : connecting ? 'Connecting' : 'Disconnected';

  return (
    <div className={styles.bar}>
      <span
        className={`${styles.dot} ${dotClass}`}
        aria-label={dotLabel}
      />
      <span className={styles.status}>{dotLabel}</span>
      <span className={styles.url}>{url}</span>

      {!connected && !connecting && (
        <button
          className={styles.button}
          onClick={onConnect}
          aria-label="Start WebSocket connection"
        >
          Start
        </button>
      )}
      {connecting && (
        <button
          className={styles.button}
          disabled
          aria-label="Connecting…"
          aria-disabled="true"
        >
          Connecting…
        </button>
      )}
      {connected && (
        <button
          className={styles.button}
          onClick={onDisconnect}
          aria-label="Stop WebSocket connection"
        >
          Stop Service
        </button>
      )}
    </div>
  );
}
