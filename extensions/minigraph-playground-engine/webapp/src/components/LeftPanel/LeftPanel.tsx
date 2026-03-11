import React from 'react';
import Console from '../Console/Console';
import CommandInput from '../CommandInput/CommandInput';
import styles from './LeftPanel.module.css';

interface LeftPanelProps {
  // Console props
  messages:           { id: number; raw: string }[];
  autoScroll:         boolean;
  onToggleAutoScroll: () => void;
  onCopy:             () => void;
  onClear:            () => void;
  consoleRef:         React.RefObject<HTMLDivElement | null>;
  onPinMessage?:      (message: string) => void;
  pinnedMessage?:     string | null;
  /** Called after any per-message copy succeeds — use this to show a toast. */
  onCopyMessage?:     () => void;
  // CommandInput props
  command:            string;
  onCommandChange:    (value: string) => void;
  onCommandKeyDown:   (e: React.KeyboardEvent<HTMLElement>) => void;
  onSend:             () => void;
  sendDisabled:       boolean;
  inputDisabled:      boolean;
  multiline:          boolean;
  onToggleMultiline:  (force?: boolean) => void;
}

export default function LeftPanel({
  messages, autoScroll, onToggleAutoScroll, onCopy, onClear, consoleRef,
  onPinMessage, pinnedMessage, onCopyMessage,
  command, onCommandChange, onCommandKeyDown, onSend,
  sendDisabled, inputDisabled, multiline, onToggleMultiline,
}: LeftPanelProps) {
  return (
    <div className={styles.root}>
      <Console
        messages={messages}
        autoScroll={autoScroll}
        onToggleAutoScroll={onToggleAutoScroll}
        onCopy={onCopy}
        onClear={onClear}
        consoleRef={consoleRef}
        onPinMessage={onPinMessage}
        pinnedMessage={pinnedMessage}
        onCopyMessage={onCopyMessage}
      />
      <CommandInput
        command={command}
        onChange={onCommandChange}
        onKeyDown={onCommandKeyDown}
        onSend={onSend}
        disabled={inputDisabled}
        sendDisabled={sendDisabled}
        multiline={multiline}
        onToggleMultiline={onToggleMultiline}
      />
    </div>
  );
}
