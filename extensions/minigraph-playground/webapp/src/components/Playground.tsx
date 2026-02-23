import { useState, useMemo } from 'react';
import styles from './Playground.module.css';
import { validateJSON, formatJSON } from '../utils/validators';
import { useToast } from '../hooks/useToast';
import { useLocalStorage } from '../hooks/useLocalStorage';
import { useWebSocket } from '../hooks/useWebSocket';
import { ToastContainer } from './Toast';
import Navigation from './Navigation';
import ConnectionStatus from './ConnectionStatus/ConnectionStatus';
import PayloadEditor from './PayloadEditor/PayloadEditor';
import Console from './Console/Console';
import CommandInput from './CommandInput/CommandInput';
import { type PlaygroundConfig } from '../config/playgrounds';

interface PlaygroundProps {
  config: PlaygroundConfig;
}

export default function Playground({ config }: PlaygroundProps) {
  const { title, wsPath, storageKeyPayload, storageKeyHistory } = config;

  // Persisted payload
  const [payload, setPayload] = useLocalStorage<string>(storageKeyPayload, '');

  // Live payload validation — derived synchronously from payload, no extra render cycle needed
  const payloadValidation = useMemo(
    () => payload ? validateJSON(payload) : { valid: true, error: null, type: null },
    [payload]
  );

  // Toast notifications
  const { toasts, addToast, removeToast } = useToast();

  // Multiline command input toggle
  const [multiline, setMultiline] = useState(false);

  // All WebSocket + console logic lives in the hook
  const ws = useWebSocket({ wsPath, storageKeyHistory, payload, addToast, submitKey: multiline ? 'ctrl+enter' : 'enter' });

  // Show the console panel once a connection has been attempted
  const [showConsole, setShowConsole] = useState(false);

  const handleConnect = () => {
    setShowConsole(true);
    ws.connect();
  };

  const handleFormatPayload = () => setPayload(formatJSON(payload));

  return (
    <div className={styles.wrapper}>
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      <header className={styles.header}>
        <h1 className={styles.title}>{title}</h1>
        <Navigation />
      </header>

      <div className={styles.container}>
        {/* ── Left panel: inputs & controls ── */}
        <div className={styles.leftPanel}>
          <ConnectionStatus connected={ws.connected} url={ws.wsUrl} />

          <PayloadEditor
            payload={payload}
            onChange={setPayload}
            validation={payloadValidation}
            onFormat={handleFormatPayload}
          />

          <CommandInput
            command={ws.command}
            onChange={ws.setCommand}
            onKeyDown={ws.handleKeyDown}
            disabled={!ws.connected}
            multiline={multiline}
            onToggleMultiline={() => setMultiline(m => !m)}
          />

          <div className={styles.buttonGroup}>
            {!ws.connected && !ws.connecting && (
              <button className={`${styles.button} ${styles.buttonPrimary}`} onClick={handleConnect}>
                Start
              </button>
            )}
            {ws.connecting && (
              <button className={`${styles.button} ${styles.buttonPrimary}`} disabled>
                Connecting...
              </button>
            )}
            {ws.connected && (
              <button className={`${styles.button} ${styles.buttonWarning}`} onClick={ws.disconnect}>
                Stop Service
              </button>
            )}
            {showConsole && !ws.connected && !ws.connecting && (
              <button className={`${styles.button} ${styles.buttonWarning}`} onClick={() => setShowConsole(false)}>
                Clear &amp; Hide Console
              </button>
            )}
          </div>
        </div>

        {/* ── Right panel: console output ── */}
        <div className={styles.rightPanel}>
          {showConsole && (
            <Console
              messages={ws.messages}
              autoScroll={ws.autoScroll}
              onToggleAutoScroll={ws.toggleAutoScroll}
              onCopy={ws.copyMessages}
              onClear={ws.clearMessages}
              consoleRef={ws.consoleRef}
            />
          )}
        </div>
      </div>
    </div>
  );
}
