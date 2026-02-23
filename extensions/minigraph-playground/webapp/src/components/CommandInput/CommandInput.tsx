import styles from './CommandInput.module.css';

interface CommandInputProps {
  command:             string;
  onChange:            (value: string) => void;
  onKeyDown:           (e: React.KeyboardEvent<HTMLElement>) => void;
  disabled:            boolean;
  multiline?:          boolean;
  onToggleMultiline?:  () => void;
}

export default function CommandInput({
  command,
  onChange,
  onKeyDown,
  disabled,
  multiline = false,
  onToggleMultiline,
}: CommandInputProps) {
  const placeholder = disabled
    ? 'Enter your test message once it is connected'
    : multiline
      ? 'Enter command (Ctrl+Enter to send · ↑↓ for history)'
      : 'Enter command (Up Arrow for history)';

  const hint = disabled
    ? null
    : multiline
      ? 'Ctrl+Enter to send · Enter for new line'
      : 'Enter to send';

  return (
    <div className={styles.card}>
      <div className={styles.labelRow}>
        <label htmlFor="command" className={styles.label}>Command</label>
        {onToggleMultiline && (
          <button
            className={`${styles.toggleButton}${multiline ? ` ${styles.active}` : ''}`}
            onClick={onToggleMultiline}
            title={multiline ? 'Switch to single-line input' : 'Switch to multiline input'}
          >
            Multiline
          </button>
        )}
      </div>

      <textarea
        id="command"
        className={styles.textarea}
        rows={multiline ? 5 : 1}
        placeholder={placeholder}
        value={command}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={onKeyDown}
      />

      {hint && <p className={styles.hint}>{hint}</p>}
    </div>
  );
}
