import { parseMessage, getMessageIcon, tryParseJSON, isMarkdownCandidate } from '../../utils/messageParser';
import { JsonView, darkStyles } from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import styles from './Console.module.css';

interface ConsoleMessageProps {
  message: string;
  onPin?:  (message: string) => void;
  pinned?: boolean;
}

export default function ConsoleMessage({ message, onPin, pinned }: ConsoleMessageProps) {
  const parsed    = parseMessage(message);
  const icon      = getMessageIcon(parsed.type);
  const jsonCheck = tryParseJSON(parsed.message);

  return (
    <div
      className={[
        styles.consoleMessage,
        styles[`messageType-${parsed.type}`],
        onPin ? styles.consoleMessagePinnable : '',
        pinned ? styles.consoleMessagePinned : '',
      ].filter(Boolean).join(' ')}
      onClick={onPin && isMarkdownCandidate(message) ? () => onPin(message) : undefined}
      title={onPin && isMarkdownCandidate(message) ? 'Click to pin to Markdown Preview' : undefined}
      role={onPin && isMarkdownCandidate(message) ? 'button' : undefined}
      tabIndex={onPin && isMarkdownCandidate(message) ? 0 : undefined}
      onKeyDown={onPin && isMarkdownCandidate(message)
        ? (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onPin(message); } }
        : undefined}
      aria-label={onPin && isMarkdownCandidate(message) ? 'Pin to Markdown Preview' : undefined}
      aria-pressed={onPin && isMarkdownCandidate(message) ? pinned : undefined}
    >
      <span className={styles.messageIcon}>{icon}</span>
      <div className={styles.messageContent}>
        {jsonCheck.isJSON ? (
          <div className={styles.jsonViewWrapper}>
            <JsonView
              data={jsonCheck.data!}
              shouldExpandNode={(level) => level < 2}
              style={{
                ...darkStyles,
                container: `${darkStyles.container} ${styles.jsonContainer}`,
                label: styles.jsonLabel,
                stringValue: styles.jsonString,
                numberValue: styles.jsonNumber,
                booleanValue: styles.jsonBoolean,
                nullValue: styles.jsonNull,
              }}
            />
          </div>
        ) : (
          <span className={styles.messageText}>{parsed.message}</span>
        )}
      </div>
      {parsed.time && (
        <span className={styles.messageTime}>{parsed.time}</span>
      )}
    </div>
  );
}
