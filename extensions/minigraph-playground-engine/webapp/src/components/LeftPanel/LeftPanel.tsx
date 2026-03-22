import React from 'react';
import Console from '../Console/Console';
import CommandInput from '../CommandInput/CommandInput';
import styles from './LeftPanel.module.css';

interface LeftPanelProps {
  // Console props
  messages:           { id: number; raw: string }[];
  onCopy:             () => void;
  onClear:            () => void;
  consoleRef:         React.RefObject<HTMLDivElement | null>;
  onPinMessage?:      (msg: { id: number; raw: string }) => void;
  /** The id of the currently-pinned message, or null if nothing is pinned. */
  pinnedMessageId?:   number | null;
  /** Called after any per-message copy succeeds — use this to show a toast. */
  onCopyMessage?:     () => void;
  /** When provided, a "Send to JSON-Path" button appears on JSON messages. */
  onSendToJsonPath?:  (json: string) => void;
  /** When provided, a "⬆️ Upload JSON…" re-open button appears on mock-upload invitation rows. */
  onUploadMockData?:  (uploadPath: string) => void;
  /** Set of POST paths for which a mock upload has succeeded this session. */
  successfulUploadPaths?: Set<string>;
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
  messages, onCopy, onClear, consoleRef,
  onPinMessage, pinnedMessageId, onCopyMessage, onSendToJsonPath,
  onUploadMockData, successfulUploadPaths,
  command, onCommandChange, onCommandKeyDown, onSend,
  sendDisabled, inputDisabled, multiline, onToggleMultiline,
}: LeftPanelProps) {
  return (
    <div className={styles.root}>
      <Console
        messages={messages}
        onCopy={onCopy}
        onClear={onClear}
        consoleRef={consoleRef}
        onPinMessage={onPinMessage}
        pinnedMessageId={pinnedMessageId}
        onCopyMessage={onCopyMessage}
        onSendToJsonPath={onSendToJsonPath}
        onUploadMockData={onUploadMockData}
        successfulUploadPaths={successfulUploadPaths}
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
