import { useRef, useEffect } from 'react';
import styles from './CommandInput.module.css';
import { useAutocomplete } from '../../hooks/useAutocomplete';
import { COMMAND_QUICKSTART } from '../../utils/commandSuggestions';

interface CommandInputProps {
  command:            string;
  onChange:           (value: string) => void;
  onKeyDown:          (e: React.KeyboardEvent<HTMLElement>) => void;
  onSend:             () => void;
  sendDisabled:       boolean;
  disabled:           boolean;
  multiline?:         boolean;
  onToggleMultiline?: (force?: boolean) => void;
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
  const textareaRef  = useRef<HTMLTextAreaElement>(null);
  const dropdownRef  = useRef<HTMLUListElement>(null);   // reserved for future scroll-trap / focus management
  const activeItemRef = useRef<HTMLLIElement>(null);

  const ac = useAutocomplete(command);

  // Scroll the highlighted item into view inside the dropdown.
  useEffect(() => {
    activeItemRef.current?.scrollIntoView({ block: 'nearest' });
  }, [ac.activeIndex]);

  const placeholder = disabled
    ? 'Not connected'
    : multiline
      ? 'Enter command (Ctrl+Enter to send · ↑↓ for history)'
      : 'Enter command (Enter to send · Tab to autocomplete · ↑↓ for history)';

  const hint = disabled
    ? 'Enter your test message once it is connected'
    : multiline
      ? 'Ctrl+Enter to send · Enter for new line · Shift+Enter for new line'
      : 'Enter to send · Shift+Enter for new line · Tab to autocomplete';

  // Wrap the parent's setCommand so the autocomplete hook can track changes.
  const handleChange = (value: string) => {
    ac.onCommandChange(value);
    onChange(value);
  };

  // Accept a suggestion from the dropdown.
  const handleAccept = (index: number) => {
    ac.accept(index, onChange, onToggleMultiline);
    textareaRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Give the autocomplete hook first refusal on the keystroke.
    // In single-line mode only — in multiline mode the dropdown isn't shown.
    if (!multiline) {
      const consumed = ac.onKeyDown(e, onChange, onToggleMultiline);
      if (consumed) {
        textareaRef.current?.focus();
        return;
      }
    }

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
      // ArrowUp / ArrowDown → delegate to history handler only when appropriate.
      if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
        if (multiline) {
          // In multiline mode the textarea handles cursor movement natively.
          // Only fall through to history navigation when the caret is already
          // at the boundary where there is no further line to move to:
          //   ArrowUp  → only if caret is on the first line
          //   ArrowDown → only if caret is on the last line
          const el = textareaRef.current;
          if (el) {
            const { selectionStart, value } = el;
            const beforeCaret = value.slice(0, selectionStart);
            const isFirstLine  = !beforeCaret.includes('\n');
            const isLastLine   = !value.slice(selectionStart).includes('\n');

            const atBoundary =
              (e.key === 'ArrowUp'   && isFirstLine) ||
              (e.key === 'ArrowDown' && isLastLine);

            if (!atBoundary) {
              // Let the browser move the caret between lines — don't intercept.
              return;
            }
          }
        }
        // Single-line mode, or multiline at a boundary → navigate history.
        ac.dismiss();
        onKeyDown(e);
      } else {
        onKeyDown(e);
      }
    }
  };

  // Autocomplete dropdown — only rendered in single-line mode when the dropdown is open.
  const dropdown =
    !multiline && ac.isOpen && ac.suggestions.length > 0 ? (
      <ul ref={dropdownRef} className={styles.suggestions} role="listbox">
        {ac.suggestions.map((s, i) => (
          <li
            key={s.template}
            ref={i === ac.activeIndex ? activeItemRef : null}
            role="option"
            aria-selected={i === ac.activeIndex}
            className={`${styles.suggestionItem} ${i === ac.activeIndex ? styles.suggestionItemActive : ''}`}
            onMouseDown={(e) => {
              // Use mousedown so focus stays on the textarea.
              e.preventDefault();
              handleAccept(i);
            }}
          >
            <span className={styles.suggestionTokens}>
              {s.tokens.join(' ')}
            </span>
            <span className={styles.suggestionHint}>{s.hint}</span>
            {s.multiline && (
              <span className={styles.suggestionBadge}>multi-line</span>
            )}
          </li>
        ))}
      </ul>
    ) : null;

  return (
    <div className={styles.commandInput}>
      <div className={styles.labelRow}>
        {/* Label + info icon with hover popover */}
        <div className={styles.labelGroup}>
          <label htmlFor="command" className={styles.label}>Command</label>
          <span className={styles.infoWrapper} aria-label="Command reference">
            <i className={styles.infoIcon} aria-hidden="true">i</i>
            <div className={styles.popover} role="tooltip">
              <p className={styles.popoverTitle}>Getting started — type a keyword to begin</p>
              {COMMAND_QUICKSTART.map(({ keyword, alias, description }) => (
                <div key={keyword} className={styles.popoverRow}>
                  <span className={styles.popoverKeyword}>{keyword}</span>
                  <span className={styles.popoverDesc}>
                    {description}
                    {alias && (
                      <span className={styles.popoverAlias}> · alias: {alias}</span>
                    )}
                  </span>
                </div>
              ))}
            </div>
          </span>
        </div>
        {onToggleMultiline && (
          <label className={styles.checkboxLabel}>
            <input
              type="checkbox"
              checked={multiline}
              onChange={() => onToggleMultiline()}
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
            onChange={(e) => handleChange(e.target.value)}
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
        <div className={styles.inputWrapper}>
          <div className={styles.inputRow}>
            <textarea
              ref={textareaRef}
              id="command"
              className={styles.textarea}
              rows={1}
              placeholder={placeholder}
              value={command}
              disabled={disabled}
              onChange={(e) => handleChange(e.target.value)}
              onKeyDown={handleKeyDown}
              onBlur={() => {
                // Small delay so a mousedown on a suggestion can fire first.
                setTimeout(() => ac.dismiss(), 150);
              }}
              autoComplete="off"
              autoCorrect="off"
              spellCheck={false}
            />
            <button
              className={styles.sendButton}
              onClick={() => { onSend(); textareaRef.current?.focus(); }}
              disabled={sendDisabled}
            >
              Send
            </button>
          </div>
          {dropdown}
        </div>
      )}

      {hint && <p className={styles.hint}>{hint}</p>}
    </div>
  );
}
