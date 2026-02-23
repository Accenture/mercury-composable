import { useRef } from 'react';
import styles from './CommandInput.module.css';

interface CommandInputProps {
  command:            string;
  onChange:           (value: string) => void;
  onKeyDown:          (e: React.KeyboardEvent<HTMLElement>) => void;
  onSend:             () => void;
  sendDisabled:       boolean;
  disabled:           boolean;
  multiline?:         boolean;
  onToggleMultiline?: () => void;
}

export default function CommandInput({
  command,
  onChange,
  onKeyDown,
  onSend,
  sendDisabled,
  disabled,
  multiline = false,
  onToggleMultiline,
}: CommandInputProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const placeholder = disabled
    ? 'Not connected'
    : multiline
      ? 'Enter command (Ctrl+Enter to send · ↑↓ for history)'
      : 'Enter command (Enter to send · ↑↓ for history)';

  const hint = disabled
    ? 'Enter your test message once it is connected'
    : multiline
      ? 'Ctrl+Enter to send · Enter for new line · Shift+Enter for new line'
      : 'Enter to send · Shift+Enter for new line';

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter') {
      if (multiline) {
        if (e.ctrlKey || e.metaKey) {
          e.preventDefault();
          onSend();
          textareaRef.current?.focus();
        }
        // plain Enter in multiline: fall through → browser inserts newline
      } else {
        if (!e.shiftKey) {
          e.preventDefault();
          onSend();
          textareaRef.current?.focus();
        }
        // Shift+Enter in single-line: fall through → browser expands textarea
      }
    } else {
      // ArrowUp / ArrowDown (and all other keys) → delegate to history handler
      onKeyDown(e);
    }
  };

  return (
    <div className={styles.commandInput}>
      <div className={styles.labelRow}>
        <label htmlFor="command" className={styles.label}>Command</label>
        {onToggleMultiline && (
          <label className={styles.checkboxLabel}>
            <input
              type="checkbox"
              checked={multiline}
              onChange={onToggleMultiline}
            />
            Multiline
          </label>
        )}
      </div>

      {multiline ? (
        <>
          <textarea
            ref={textareaRef}
            id="command"
            className={styles.textarea}
            rows={5}
            placeholder={placeholder}
            value={command}
            disabled={disabled}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <button
            className={`${styles.sendButton} ${styles.sendButtonFullWidth}`}
            onClick={() => { onSend(); textareaRef.current?.focus(); }}
            disabled={sendDisabled}
          >
            Send
          </button>
        </>
      ) : (
        <div className={styles.inputRow}>
          <textarea
            ref={textareaRef}
            id="command"
            className={styles.textarea}
            rows={1}
            placeholder={placeholder}
            value={command}
            disabled={disabled}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <button
            className={styles.sendButton}
            onClick={() => { onSend(); textareaRef.current?.focus(); }}
            disabled={sendDisabled}
          >
            Send
          </button>
        </div>
      )}

      {hint && <p className={styles.hint}>{hint}</p>}
    </div>
  );
}

