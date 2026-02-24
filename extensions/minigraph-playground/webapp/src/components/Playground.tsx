import { useState, useMemo } from 'react';
import { Group, Panel, Separator, useDefaultLayout } from 'react-resizable-panels';
import styles from './Playground.module.css';
import { validateJSON, formatJSON } from '../utils/validators';
import { useToast } from '../hooks/useToast';
import { useLocalStorage } from '../hooks/useLocalStorage';
import { useWebSocket } from '../hooks/useWebSocket';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { ToastContainer } from './Toast';
import Navigation from './Navigation';
import ConnectionBar from './ConnectionBar/ConnectionBar';
import { isMarkdownCandidate } from '../utils/messageParser';
import RightPanel from './RightPanel/RightPanel';
import LeftPanel from './LeftPanel/LeftPanel';
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
  const ws = useWebSocket({ wsPath, storageKeyHistory, payload, addToast });

  // Pinned message state (null = no explicit pin; falls back to auto-last)
  const [pinnedMessage, setPinnedMessage] = useState<string | null>(null);

  // Auto-last: most recently received non-JSON message
  const lastNonJsonMessage = useMemo<string | null>(() => {
    for (let i = ws.messages.length - 1; i >= 0; i--) {
      if (isMarkdownCandidate(ws.messages[i].raw)) return ws.messages[i].raw;
    }
    return null;
  }, [ws.messages]);

  // Shown in MarkdownPreview — pinnedMessage wins; falls back to auto-last
  const resolvedPreviewMessage = pinnedMessage ?? lastNonJsonMessage;

  // Responsive layout: stack panels vertically on narrow viewports
  const isMobile = useMediaQuery('(max-width: 768px)');

  // Persist panel split ratio per playground route
  const { defaultLayout, onLayoutChanged } = useDefaultLayout({
    id: config.path + '-panel-split',
    storage: localStorage,
  });

  const handleConnect = () => ws.connect();

  const handleFormatPayload = () => setPayload(formatJSON(payload));

  const handleClearMessages = () => {
    ws.clearMessages();
    setPinnedMessage(null);
  };

  return (
    <div className={styles.wrapper}>
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      <header className={styles.header}>
        <h1 className={styles.title}>{title}</h1>
        <Navigation connectionBar={
          <ConnectionBar
            connected={ws.connected}
            connecting={ws.connecting}
            url={ws.wsUrl}
            onConnect={handleConnect}
            onDisconnect={ws.disconnect}
          />
        } />
      </header>

      <Group
        className={styles.panelGroup}
        orientation={isMobile ? 'vertical' : 'horizontal'}
        defaultLayout={defaultLayout}
        onLayoutChanged={onLayoutChanged}
      >
        <Panel defaultSize="60%" minSize="25%">
          <LeftPanel
            messages={ws.messages}
            autoScroll={ws.autoScroll}
            onToggleAutoScroll={ws.toggleAutoScroll}
            onCopy={ws.copyMessages}
            onClear={handleClearMessages}
            consoleRef={ws.consoleRef}
            command={ws.command}
            onCommandChange={ws.setCommand}
            onCommandKeyDown={ws.handleKeyDown}
            onSend={ws.sendCommand}
            sendDisabled={!ws.connected || !ws.command.trim()}
            inputDisabled={!ws.connected}
            multiline={multiline}
            onToggleMultiline={() => setMultiline(m => !m)}
            onPinMessage={setPinnedMessage}
            pinnedMessage={pinnedMessage}
          />
        </Panel>
        <Separator className={styles.resizeHandle} aria-label="Resize panels" />
        <Panel defaultSize="40%" minSize="20%">
          <RightPanel
            payload={payload}
            onChange={setPayload}
            validation={payloadValidation}
            onFormat={handleFormatPayload}
            previewMessage={resolvedPreviewMessage}
            pinnedMessage={pinnedMessage}
          />
        </Panel>
      </Group>
    </div>
  );
}

