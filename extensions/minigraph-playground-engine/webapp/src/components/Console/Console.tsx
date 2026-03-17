import { ConsoleErrorBoundary } from './ConsoleErrorBoundary';
import ConsoleMessage from './ConsoleMessage';
import styles from './Console.module.css';

interface ConsoleProps {
  messages:           { id: number; raw: string }[];
  onCopy:             () => void;
  onClear:            () => void;
  consoleRef:         React.RefObject<HTMLDivElement | null>;
  onPinMessage?:      (msg: { id: number; raw: string }) => void;
  /** The id of the currently-pinned message, or null if nothing is pinned. */
  pinnedMessageId?:   number | null;
  /** Called after any per-message copy succeeds — use this to show a toast. */
  onCopyMessage?:     () => void;
}

export default function Console({
  messages,
  onCopy,
  onClear,
  consoleRef,
  onPinMessage,
  pinnedMessageId,
  onCopyMessage,
}: ConsoleProps) {
  return (
    <div className={styles.consoleRoot}>
      <div className={styles.consoleHeader}>
        <span className={styles.consoleTitle}>Console Output</span>
        <div className={styles.consoleControls}>
          <button
            className={styles.controlButton}
            onClick={onCopy}
            title="Copy console output"
            aria-label="Copy console output to clipboard"
          >
            📑
          </button>
          <button
            className={styles.controlButton}
            onClick={onClear}
            title="Clear console"
            aria-label="Clear console"
          >
            🆑
          </button>
        </div>
      </div>

      <div className={styles.console} ref={consoleRef} role="log" aria-live="polite">
        {messages.map((msg) => (
          <ConsoleErrorBoundary key={msg.id} fallback={msg.raw}>
            <ConsoleMessage
              message={msg.raw}
              onPin={onPinMessage ? () => onPinMessage(msg) : undefined}
              pinned={pinnedMessageId === msg.id}
              onCopyMessage={onCopyMessage}
            />
          </ConsoleErrorBoundary>
        ))}
        {messages.length === 0 && (
          <div className={styles.emptyConsole}>
            No messages yet. Use the <strong>Start</strong> button in the header to connect.
          </div>
        )}
      </div>
    </div>
  );
}
