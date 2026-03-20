import { parseMessage, getMessageIcon, tryParseJSON, isMarkdownCandidate, isGraphLinkMessage, isLargePayloadMessage } from '../../utils/messageParser';
import { JsonView, darkStyles } from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import { useCopyToClipboard } from '../../hooks/useCopyToClipboard';
import styles from './Console.module.css';

interface ConsoleMessageProps {
  message:              string;
  /** Called when the user clicks/activates this row to pin it. The parent
   *  is responsible for capturing the message identity — this component
   *  just signals the intent. */
  onPin?:               () => void;
  pinned?:              boolean;
  /** Called after a successful clipboard write — use this to show a toast. */
  onCopyMessage?:       () => void;
  /**
   * When provided, a "Send to JSON-Path" action button is shown on hover for
   * JSON messages.  Called with the pretty-printed JSON string.
   * Only rendered when the message body is a valid JSON object/array.
   */
  onSendToJsonPath?:    (json: string) => void;
}

export default function ConsoleMessage({ message, onPin, pinned, onCopyMessage, onSendToJsonPath }: ConsoleMessageProps) {
  const parsed    = parseMessage(message);
  const icon      = getMessageIcon(parsed.type);
  const jsonCheck = tryParseJSON(parsed.message);

  const isGraphLink      = isGraphLinkMessage(message);
  const isLargePayload   = isLargePayloadMessage(message);
  const isPinnable       = !!onPin && (!isGraphLink ? isMarkdownCandidate(message) : true);
  const pinTitle         = isGraphLink
    ? 'Click to load graph in Graph View'
    : 'Click to pin to Developer Guides';
  const pinLabel         = isGraphLink
    ? 'Load graph in Graph View'
    : 'Pin to Developer Guides';

  // Only show the "send to JSON-Path" button when the message is a JSON object/array
  // and the parent has wired up the callback.
  const canSendToJsonPath = !!onSendToJsonPath && jsonCheck.isJSON;

  // Each message row owns its own copy state so the "✓" button confirmation
  // is scoped to exactly the row the user clicked — not the whole console.
  // The toast notification is fired via the onCopyMessage callback so this
  // component stays decoupled from the toast system.
  const { copy, copied } = useCopyToClipboard({ onSuccess: onCopyMessage });

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    copy(message);
  };

  const handleCopyKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      e.stopPropagation();
      copy(message);
    }
  };

  const handleSendToJsonPath = (e: React.MouseEvent | React.KeyboardEvent) => {
    e.stopPropagation();
    if (!onSendToJsonPath || !jsonCheck.isJSON) return;
    const pretty = JSON.stringify(jsonCheck.data, null, 2);
    onSendToJsonPath(pretty);
  };

  return (
    <div
      className={[
        styles.consoleMessage,
        styles[`messageType-${parsed.type}`],
        isPinnable    ? styles.consoleMessagePinnable    : '',
        pinned        ? styles.consoleMessagePinned      : '',
        isGraphLink   ? styles.consoleMessageGraphLink   : '',
        isLargePayload ? styles.consoleMessageLargePayload : '',
      ].filter(Boolean).join(' ')}
      onClick={isPinnable ? () => onPin!() : undefined}
      title={isPinnable ? pinTitle : undefined}
      role={isPinnable ? 'button' : undefined}
      tabIndex={isPinnable ? 0 : undefined}
      onKeyDown={isPinnable
        ? (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onPin!(); } }
        : undefined}
      aria-label={isPinnable ? pinLabel : undefined}
      aria-pressed={isPinnable ? pinned : undefined}
    >
      <span className={styles.messageIcon}>
        {isLargePayload ? '⬇️' : isGraphLink ? '🕸️' : icon}
      </span>

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

      {/* ── Copy button — visible on row hover ─── */}
      <button
        className={`${styles.copyButton} ${copied ? styles.copyButtonCopied : ''}`}
        onClick={handleCopy}
        onKeyDown={handleCopyKeyDown}
        title={copied ? 'Copied!' : 'Copy message'}
        aria-label={copied ? 'Copied to clipboard' : 'Copy message to clipboard'}
        // Prevent the button from participating in the pin row's tab stop —
        // it has its own independent tab stop below.
        tabIndex={0}
      >
        {copied ? '✅' : '📄'}
      </button>

      {/* ── Send-to-JSON-Path button — only on JSON messages ─── */}
      {canSendToJsonPath && (
        <button
          className={styles.sendToJsonPathButton}
          onClick={handleSendToJsonPath}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleSendToJsonPath(e); }}
          title="Open in JSON-Path Playground"
          aria-label="Open this JSON in the JSON-Path Playground"
          tabIndex={0}
        >
          ➡️
        </button>
      )}

      {parsed.time && (
        <span className={styles.messageTime}>{parsed.time}</span>
      )}
    </div>
  );
}
