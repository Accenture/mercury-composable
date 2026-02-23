import styles from './ConnectionStatus.module.css';

interface ConnectionStatusProps {
  connected: boolean;
  url:       string;
}

export default function ConnectionStatus({ connected, url }: ConnectionStatusProps) {
  return (
    <div className={styles.statusCard}>
      <div className={styles.statusIndicator}>
        <span className={`${styles.statusDot} ${connected ? styles.connected : styles.disconnected}`} />
        <span className={styles.statusText}>
          {connected ? 'Connected' : 'Disconnected'}
        </span>
      </div>
      <span className={styles.statusUrl}>{url}</span>
    </div>
  );
}
