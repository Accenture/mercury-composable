import { useState, useMemo, useEffect, useCallback } from 'react';
import { Group, Panel, Separator, useDefaultLayout } from 'react-resizable-panels';
import styles from './Playground.module.css';
import { validateJSON, formatJSON } from '../utils/validators';
import { useToast } from '../hooks/useToast';
import { useLocalStorage } from '../hooks/useLocalStorage';
import { useWebSocket } from '../hooks/useWebSocket';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { ToastContainer } from './Toast';
import Navigation from './Navigation';
import { isMarkdownCandidate, isGraphLinkMessage, extractGraphApiPath } from '../utils/messageParser';
import RightPanel from './RightPanel/RightPanel';
import LeftPanel from './LeftPanel/LeftPanel';
import { type PlaygroundConfig } from '../config/playgrounds';
import { isMinigraphGraphData, type MinigraphGraphData } from '../utils/graphTypes';
import type { RightTab } from './RightPanel/RightPanel';

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

  // Auto-last: most recently received non-JSON, non-graph-link message
  const lastNonJsonMessage = useMemo<string | null>(() => {
    for (let i = ws.messages.length - 1; i >= 0; i--) {
      const raw = ws.messages[i].raw;
      if (!isGraphLinkMessage(raw) && isMarkdownCandidate(raw)) return raw;
    }
    return null;
  }, [ws.messages]);

  // Shown in MarkdownPreview — pinnedMessage wins; falls back to auto-last
  const resolvedPreviewMessage = pinnedMessage ?? lastNonJsonMessage;

  // ── Graph state ──────────────────────────────────────────────────────────
  // The API path extracted from the currently-pinned graph-link message.
  const [pinnedGraphPath, setPinnedGraphPath] = useState<string | null>(null);
  // Fetched and parsed graph data, or null while loading / not yet pinned.
  const [graphData, setGraphData] = useState<MinigraphGraphData | null>(null);
  // Which right-panel tab is currently active — lifted here so we can auto-switch.
  const [rightTab, setRightTab] = useState<RightTab>('payload');

  // When a message is pinned, decide whether it is a graph link or a Markdown message.
  const handlePinMessage = useCallback((message: string) => {
    if (isGraphLinkMessage(message)) {
      const path = extractGraphApiPath(message);
      setPinnedGraphPath(path);
      setPinnedMessage(null);      // clear any Markdown pin
    } else {
      setPinnedMessage(message);
      setPinnedGraphPath(null);    // clear any graph pin
    }
  }, []);

  // Fetch graph data whenever pinnedGraphPath changes.
  useEffect(() => {
    if (!pinnedGraphPath) return;

    const baseUrl = import.meta.env.DEV ? 'http://localhost:8085' : '';
    const url = `${baseUrl}${pinnedGraphPath}`;

    let cancelled = false;
    setGraphData(null);   // clear stale data while loading

    fetch(url)
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((json: unknown) => {
        if (!cancelled && isMinigraphGraphData(json)) {
          setGraphData(json);
          setRightTab('graph');   // auto-switch to Graph tab
        }
      })
      .catch(err => {
        if (!cancelled) addToast(`Graph fetch failed: ${err.message}`, 'error');
      });

    return () => { cancelled = true; };
  }, [pinnedGraphPath, addToast]);

  // Responsive layout: stack panels vertically on narrow viewports
  const isMobile = useMediaQuery('(max-width: 768px)');

  // Persist panel split ratio per playground route
  const { defaultLayout, onLayoutChanged } = useDefaultLayout({
    id: config.path + '-panel-split',
    storage: localStorage,
  });

  const handleFormatPayload = () => setPayload(formatJSON(payload));

  const handleClearMessages = () => {
    ws.clearMessages();
    setPinnedMessage(null);
    setPinnedGraphPath(null);
    setGraphData(null);
  };

  return (
    <div className={styles.wrapper}>
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      <header className={styles.header}>
        <h1 className={styles.title}>{title}</h1>
        {/* Navigation reads connection state from WebSocketContext directly —
            no connectionBar prop needed any more. */}
        <Navigation />
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
            onPinMessage={handlePinMessage}
            pinnedMessage={pinnedMessage ?? pinnedGraphPath}
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
            graphData={graphData}
            activeTab={rightTab}
            onTabChange={setRightTab}
          />
        </Panel>
      </Group>
    </div>
  );
}

