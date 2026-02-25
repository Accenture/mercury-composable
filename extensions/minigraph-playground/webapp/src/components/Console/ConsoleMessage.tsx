import { parseMessage, getMessageIcon, tryParseJSON, isMarkdownCandidate, isGraphLinkMessage } from '../../utils/messageParser';
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

  const isGraphLink  = isGraphLinkMessage(message);
  const isMarkdown   = !isGraphLink && isMarkdownCandidate(message);
  const isPinnable   = !!onPin && (isMarkdown || isGraphLink);
  const pinTitle     = isGraphLink
    ? 'Click to load graph in Graph View'
    : 'Click to pin to Markdown Preview';
  const pinLabel     = isGraphLink
    ? 'Load graph in Graph View'
    : 'Pin to Markdown Preview';

  return (
    <div
      className={[
        styles.consoleMessage,
        styles[`messageType-${parsed.type}`],
        isPinnable          ? styles.consoleMessagePinnable  : '',
        pinned              ? styles.consoleMessagePinned    : '',
        isGraphLink         ? styles.consoleMessageGraphLink : '',
      ].filter(Boolean).join(' ')}
      onClick={isPinnable ? () => onPin!(message) : undefined}
      title={isPinnable ? pinTitle : undefined}
      role={isPinnable ? 'button' : undefined}
      tabIndex={isPinnable ? 0 : undefined}
      onKeyDown={isPinnable
        ? (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onPin!(message); } }
        : undefined}
      aria-label={isPinnable ? pinLabel : undefined}
      aria-pressed={isPinnable ? pinned : undefined}
    >
      <span className={styles.messageIcon}>{isGraphLink ? '🕸️' : icon}</span>
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
