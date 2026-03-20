import { useState, useMemo, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Group, Panel, Separator, useDefaultLayout } from 'react-resizable-panels';
import styles from './Playground.module.css';
import { validatePayload, formatJSON } from '../utils/validators';
import { useToast } from '../hooks/useToast';
import { useLocalStorage } from '../hooks/useLocalStorage';
import { useWebSocket } from '../hooks/useWebSocket';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { useGraphData } from '../hooks/useGraphData';
import { useAutoGraphRefresh } from '../hooks/useAutoGraphRefresh';
import { useAutoMarkdownPin } from '../hooks/useAutoMarkdownPin';
import { useLargePayloadDownload } from '../hooks/useLargePayloadDownload';
import { useSavedGraphs } from '../hooks/useSavedGraphs';
import { ToastContainer } from './Toast';
import Navigation from './Navigation';
import GraphSaveButton from './GraphSaveButton/GraphSaveButton';
import SavedGraphsMenu from './SavedGraphsMenu/SavedGraphsMenu';
import { deriveDefaultName } from './GraphDataView/GraphDataView';
import { isMarkdownCandidate, isGraphLinkMessage, extractGraphApiPath } from '../utils/messageParser';
import RightPanel from './RightPanel/RightPanel';
import LeftPanel from './LeftPanel/LeftPanel';
import { type PlaygroundConfig, PLAYGROUND_CONFIGS } from '../config/playgrounds';
import { useWebSocketContext } from '../contexts/WebSocketContext';

interface PlaygroundProps {
  config: PlaygroundConfig;
}

export default function Playground({ config }: PlaygroundProps) {
  const { title, wsPath, storageKeyPayload, storageKeyHistory, storageKeySavedGraphs, supportsUpload, tabs } = config;

  const navigate = useNavigate();

  // Persisted payload (survives navigation / refresh via localStorage).
  const [storedPayload, setStoredPayload] = useLocalStorage<string>(storageKeyPayload, '');

  // Large-payload override — set by useLargePayloadDownload via WebSocketContext
  // when the server returns a payload too big to echo inline.  Using a separate
  // useState means we never write large blobs to localStorage, avoiding quota
  // exhaustion.  null = no override; the stored value is used instead.
  const ctx = useWebSocketContext();
  const [payloadOverride, setPayloadOverride] = useState<string | null>(() =>
    ctx.takePendingPayload(wsPath)
  );

  // Reactively consume a pending payload deposited by useLargePayloadDownload.
  // takePendingPayload has a new identity whenever a payload is deposited
  // (backed by useState in the context), so this effect fires precisely when
  // a new payload arrives — covering both the first-mount and already-mounted cases.
  const { takePendingPayload } = ctx;
  useEffect(() => {
    const pending = takePendingPayload(wsPath);
    if (pending !== null) {
      setPayloadOverride(pending);
    }
  }, [takePendingPayload, wsPath]);

  // The active payload: override wins over the persisted value.
  const payload    = payloadOverride ?? storedPayload;
  // Writers: the textarea and format/clear actions always write to localStorage.
  // Clearing the override on any manual edit lets the user take back control.
  const setPayload = useCallback((value: string | ((prev: string) => string)) => {
    setPayloadOverride(null);
    setStoredPayload(value as string);
  }, [setStoredPayload]);

  // Live payload validation — derived synchronously from payload, no extra render cycle needed
  const payloadValidation = useMemo(
    () => payload ? validatePayload(payload) : { valid: true, error: null, type: null },
    [payload]
  );

  // Toast notifications
  const { toasts, addToast, removeToast } = useToast();

  // Multiline command input toggle
  const [multiline, setMultiline] = useState(false);

  // All WebSocket + console logic lives in the hook
  const ws = useWebSocket({ wsPath, storageKeyHistory, payload, addToast });

  // Pinned message state — stored as a message id (number) so two messages
  // with identical text don't both appear pinned. null = no explicit pin.
  const [pinnedMessageId, setPinnedMessageId] = useState<number | null>(null);

  // Auto-last: most recently received non-JSON, non-graph-link message.
  // Only computed when the Markdown Preview tab is enabled for this playground.
  const lastNonJsonMessage = useMemo<string | null>(() => {
    if (!tabs.includes('preview')) return null;
    for (let i = ws.messages.length - 1; i >= 0; i--) {
      const raw = ws.messages[i].raw;
      if (!isGraphLinkMessage(raw) && isMarkdownCandidate(raw)) return raw;
    }
    return null;
  }, [ws.messages, tabs]);

  // Resolve the pinned id back to its raw string for MarkdownPreview.
  // pinnedMessageId wins; falls back to auto-last.
  // Only computed when the Markdown Preview tab is enabled for this playground.
  const resolvedPreviewMessage = useMemo<string | null>(() => {
    if (!tabs.includes('preview')) return null;
    if (pinnedMessageId !== null) {
      return ws.messages.find(m => m.id === pinnedMessageId)?.raw ?? null;
    }
    return lastNonJsonMessage;
  }, [pinnedMessageId, ws.messages, lastNonJsonMessage, tabs]);

  // ── Graph state ──────────────────────────────────────────────────────────
  // The API path extracted from the currently-pinned graph-link message.
  const [pinnedGraphPath, setPinnedGraphPath] = useState<string | null>(null);

  // Fetch + parse graph data, auto-switch to Graph tab — logic lives in the hook.
  const { graphData, setGraphData, rightTab, setRightTab, isRefreshing, refetchGraph } = useGraphData(pinnedGraphPath, addToast, tabs[0]);

  // ── Auto-refresh on mutation commands ────────────────────────────────────
  useAutoGraphRefresh({
    messages:           ws.messages,
    pinnedGraphPath,
    setPinnedGraphPath,
    connected:          ws.connected,
    refetchGraph,
    sendRawText:        ws.sendRawText,
    rightTab,
    addToast,
  });

  // ── Auto-pin Markdown Preview on help / describe responses ───────────────
  // When the user sends a `help` or text-producing `describe` command, the
  // first plain-text response is automatically pinned to the preview panel
  // and the panel switches to the Developer Guides tab.
  // This hook is a no-op when the playground config does not include 'preview'.
  useAutoMarkdownPin({
    messages:           ws.messages,
    connected:          ws.connected,
    setPinnedMessageId,
    onAutoPin:          tabs.includes('preview')
                          ? () => setRightTab('preview')
                          : undefined,
  });

  // ── Auto-download large payloads ──────────────────────────────────────────
  // When the server responds with a "Large payload (N) -> GET /api/inspect/…"
  // message (i.e. a namespace value that exceeds the 64 KB inline limit),
  // this hook fetches the data from the provided endpoint and immediately
  // triggers a browser file download — no extra user interaction required.
  useLargePayloadDownload({
    messages:      ws.messages,
    connected:     ws.connected,
    appendMessage: ws.appendMessage,
    addToast,
  });

  // ── Saved graphs (localStorage snapshots) ────────────────────────────────
  // Only instantiated when the playground config provides a storage key so
  // playgrounds that don't use this feature have zero overhead.
  const savedGraphs = useSavedGraphs(storageKeySavedGraphs ?? '');

  // Save the current graph name as a bookmark in localStorage.
  // Also sends `export graph as {name}` over the WebSocket so the server
  // writes (or refreshes) the corresponding {name}.json file in its temp
  // directory — that file is what `import graph from {name}` reads back.
  // The WS send is a no-op when disconnected; the bookmark is still written
  // so the user can reconnect and load it later.
  const handleSaveGraph = useCallback((name: string) => {
    savedGraphs.saveGraph(name);
    if (ws.connected) {
      ws.sendRawText(`export graph as ${name}`);
    }
    addToast(`Graph saved as "${name}"`, 'success');
  }, [savedGraphs.saveGraph, ws.connected, ws.sendRawText, addToast]);

  // Load a saved graph by sending `import graph from {name}` over the WebSocket.
  // The backend reads {name}.json from its temp directory — the file that was
  // written by the `export graph as {name}` command issued during save.
  const handleLoadGraph = useCallback((name: string) => {
    if (!ws.connected) return;
    ws.sendRawText(`import graph from ${name}`);
    addToast(`Importing graph "${name}"…`, 'info');
  }, [ws.connected, ws.sendRawText, addToast]);

  // When a message is pinned, decide whether it is a graph link or a Markdown message.
  // Receives the full message object so we can store the stable id, not the raw string.
  // In both cases we store the id — it drives the highlight in the Console.
  const handlePinMessage = useCallback((msg: { id: number; raw: string }) => {
    if (isGraphLinkMessage(msg.raw)) {
      const path = extractGraphApiPath(msg.raw);
      setPinnedGraphPath(path);
      setPinnedMessageId(msg.id);  // highlight graph-link row too
    } else {
      setPinnedMessageId(msg.id);
      setPinnedGraphPath(null);    // clear any graph pin
    }
  }, []);

  // Send an inline JSON response to the JSON-Path playground payload editor.
  // Mirrors the large-payload flow: navigate first, then deposit via context.
  // Only offered when the JSON-Path playground is connected; otherwise toast.
  const jsonPathConfig = PLAYGROUND_CONFIGS.find(c => c.tabs.includes('payload') && c.supportsUpload);
  const handleSendToJsonPath = useCallback((json: string) => {
    if (!jsonPathConfig) return;
    const slot = ctx.getSlot(jsonPathConfig.wsPath);
    if (slot.phase !== 'connected') {
      addToast('Open JSON-Path Playground and connect first, then try again.', 'error');
      return;
    }
    ctx.setPendingPayload(jsonPathConfig.wsPath, json);
    navigate(jsonPathConfig.path);
    addToast('JSON loaded into JSON-Path editor ✓', 'success');
  }, [ctx, navigate, addToast, jsonPathConfig]);

  // Responsive layout: stack panels vertically on narrow viewports
  const isMobile = useMediaQuery('(max-width: 768px)');

  // Persist panel split ratio per playground route
  const { defaultLayout, onLayoutChanged } = useDefaultLayout({
    id: config.path + '-panel-split',
    storage: localStorage,
  });

  const handleFormatPayload = useCallback(() => setPayload(formatJSON(payload)), [payload]);

  // Toggle multiline mode; pass force=true/false to set it explicitly
  // (used by the autocomplete hook when it accepts a multiline template).
  const handleToggleMultiline = useCallback((force?: boolean) => {
    setMultiline(m => force !== undefined ? force : !m);
  }, []);

  const handleClearMessages = useCallback(() => {
    ws.clearMessages();
    setPinnedMessageId(null);
    setPinnedGraphPath(null);
    setGraphData(null);
  }, [ws.clearMessages, setGraphData]);

  return (
    <div className={styles.wrapper}>
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      <header className={styles.header}>
        <h1 className={styles.title}>{title}</h1>
        <div className={styles.headerActions}>
          {storageKeySavedGraphs && (
            <GraphSaveButton
              disabled={!graphData}
              defaultName={graphData ? deriveDefaultName(graphData) : ''}
              onSave={handleSaveGraph}
              nameExists={savedGraphs.hasGraph}
              connected={ws.connected}
            />
          )}
          {storageKeySavedGraphs && savedGraphs.savedGraphs.length > 0 && (
            <SavedGraphsMenu
              savedGraphs={savedGraphs.savedGraphs}
              onLoad={handleLoadGraph}
              onDelete={savedGraphs.deleteGraph}
              connected={ws.connected}
            />
          )}
          <Navigation addToast={addToast} />
        </div>
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
            onToggleMultiline={handleToggleMultiline}
            onPinMessage={handlePinMessage}
            pinnedMessageId={pinnedMessageId}
            onCopyMessage={() => addToast('Copied to clipboard', 'success')}
            onSendToJsonPath={jsonPathConfig && wsPath !== jsonPathConfig.wsPath ? handleSendToJsonPath : undefined}
          />
        </Panel>
        <Separator className={styles.resizeHandle} aria-label="Resize panels" />
        <Panel defaultSize="40%" minSize="20%">
          <RightPanel
            tabs={tabs}
            payload={payload}
            onChange={setPayload}
            validation={payloadValidation}
            onFormat={handleFormatPayload}
            onUpload={supportsUpload ? ws.uploadPayload : undefined}
            previewMessage={resolvedPreviewMessage}
            pinnedMessage={pinnedMessageId !== null ? 'pinned' : null}
            graphData={graphData}
            activeTab={rightTab}
            onTabChange={setRightTab}
            onGraphRenderError={(msg) => addToast(msg, 'error')}
            onGraphDataCopySuccess={() => addToast('Graph JSON copied to clipboard!', 'success')}
            onGraphDataCopyError={() => addToast('Copy failed', 'error')}
            isGraphRefreshing={isRefreshing}
          />
        </Panel>
      </Group>
    </div>
  );
}

