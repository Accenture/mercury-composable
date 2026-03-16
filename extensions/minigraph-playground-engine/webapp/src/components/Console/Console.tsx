import { ConsoleErrorBoundary } from './ConsoleErrorBoundary';
import ConsoleMessage from './ConsoleMessage';
import styles from './Console.module.css';

interface ConsoleProps {
  messages:           { id: number; raw: string }[];
  autoScroll:         boolean;
  onToggleAutoScroll: () => void;
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
  autoScroll,
  onToggleAutoScroll,
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
            onClick={onToggleAutoScroll}
            title={autoScroll ? 'Disable auto-scroll' : 'Enable auto-scroll'}
            aria-label={autoScroll ? 'Disable auto-scroll' : 'Enable auto-scroll'}
          >
            {autoScroll ? 'Disable AutoScroll' : 'Enable AutoScroll'}
          </button>
          <button
            className={styles.controlButton}
            onClick={onCopy}
            title="Copy console output"
            aria-label="Copy console output to clipboard"
          >
            Copy Output
          </button>
          <button
            className={styles.controlButton}
            onClick={onClear}
            title="Clear console"
            aria-label="Clear console"
          >
            Clear
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
