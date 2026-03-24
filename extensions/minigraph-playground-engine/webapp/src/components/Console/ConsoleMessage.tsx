import { parseMessage, getMessageIcon, tryParseJSON, isMarkdownCandidate, isGraphLinkMessage, isLargePayloadMessage, isMockUploadMessage, extractMockUploadPath } from '../../utils/messageParser';
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
  /**
   * When provided, a "⬆️ Upload JSON…" re-open button appears on hover for
   * mock-upload invitation rows. Called with the extracted POST path.
   */
  onUploadMockData?:    (uploadPath: string) => void;
  /**
   * Set of POST paths for which a mock upload has succeeded this session.
   * When the current row's path is in this set, a ✅ badge is appended.
   */
  successfulUploadPaths?: Set<string>;
}

export default function ConsoleMessage({ message, onPin, pinned, onCopyMessage, onSendToJsonPath, onUploadMockData, successfulUploadPaths }: ConsoleMessageProps) {
  const parsed    = parseMessage(message);
  const icon      = getMessageIcon(parsed.type);
  const jsonCheck = tryParseJSON(parsed.message);

  const isGraphLink      = isGraphLinkMessage(message);
  const isLargePayload   = isLargePayloadMessage(message);
  const isMockUpload     = isMockUploadMessage(message);
  const mockUploadPath   = isMockUpload ? extractMockUploadPath(message) : null;
  const canUploadMock    = !!onUploadMockData && isMockUpload && mockUploadPath !== null;
  const uploadSucceeded  = canUploadMock && !!successfulUploadPaths?.has(mockUploadPath!);

  // ⚠️ Critical: isMockUpload must be excluded to prevent the invitation row
  // from getting role="button" + onClick alongside the canUploadMock button,
  // which would create a nested interactive element violating WCAG.
  // Consistent with isLargePayload rows which are also non-pinnable.
  const isPinnable       = !!onPin && !isMockUpload && (!isGraphLink ? isMarkdownCandidate(message) : true);
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

  const handleUploadMockData = (e: React.MouseEvent | React.KeyboardEvent) => {
    e.stopPropagation();
    if (!onUploadMockData || !mockUploadPath) return;
    onUploadMockData(mockUploadPath);
  };

  return (
    <div
      className={[
        styles.consoleMessage,
        styles[`messageType-${parsed.type}`],
        isPinnable     ? styles.consoleMessagePinnable    : '',
        pinned         ? styles.consoleMessagePinned      : '',
        isGraphLink    ? styles.consoleMessageGraphLink   : '',
        isLargePayload ? styles.consoleMessageLargePayload : '',
        isMockUpload   ? styles.consoleMessageMockUpload  : '',
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
        {isMockUpload ? '⬆️' : isLargePayload ? '⬇️' : isGraphLink ? '🕸️' : icon}
      </span>

      <div className={styles.messageContent}>
        {jsonCheck.isJSON ? (
          <div className={styles.jsonViewWrapper}>
            <JsonView
              data={jsonCheck.data!}
              shouldExpandNode={(level) => level < 1}
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
          <span className={styles.messageText}>
            {parsed.message}
            {uploadSucceeded && <span title="Upload succeeded"> ✅</span>}
          </span>
        )}
      </div>

      {/* ── Copy button — visible on row hover ─── */}
      <button
        className={`${styles.copyButton} ${copied ? styles.copyButtonCopied : ''}`}
        onClick={handleCopy}
        onKeyDown={handleCopyKeyDown}
        title={copied ? 'Copied!' : 'Copy message'}
        aria-label={copied ? 'Copied to clipboard' : 'Copy message to clipboard'}
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

      {/* ── Upload mock data re-open button — only on upload invitation rows ─── */}
      {canUploadMock && (
        <button
          className={styles.uploadMockButton}
          onClick={handleUploadMockData}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleUploadMockData(e); }}
          title="Re-open upload dialog"
          aria-label="Re-open mock data upload dialog"
          tabIndex={0}
        >
          ⬆️ Upload JSON…
        </button>
      )}

      {parsed.time && (
        <span className={styles.messageTime}>{parsed.time}</span>
      )}
    </div>
  );
}
