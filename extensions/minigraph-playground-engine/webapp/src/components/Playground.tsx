import { useState, useMemo, useCallback } from 'react';
import { Group, Panel, Separator, useDefaultLayout } from 'react-resizable-panels';
import styles from './Playground.module.css';
import { validatePayload, formatJSON } from '../utils/validators';
import { useToast } from '../hooks/useToast';
import { useLocalStorage } from '../hooks/useLocalStorage';
import { useWebSocket } from '../hooks/useWebSocket';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { useGraphData } from '../hooks/useGraphData';
import { useAutoGraphRefresh } from '../hooks/useAutoGraphRefresh';
import { useSavedGraphs } from '../hooks/useSavedGraphs';
import { ToastContainer } from './Toast';
import Navigation from './Navigation';
import GraphSaveButton from './GraphSaveButton/GraphSaveButton';
import SavedGraphsMenu from './SavedGraphsMenu/SavedGraphsMenu';
import { deriveDefaultName } from './GraphDataView/GraphDataView';
import { isMarkdownCandidate, isGraphLinkMessage, extractGraphApiPath, isHelpOrDescribeEcho } from '../utils/messageParser';
import RightPanel from './RightPanel/RightPanel';
import LeftPanel from './LeftPanel/LeftPanel';
import { type PlaygroundConfig } from '../config/playgrounds';

interface PlaygroundProps {
  config: PlaygroundConfig;
}

export default function Playground({ config }: PlaygroundProps) {
  const { title, wsPath, storageKeyPayload, storageKeyHistory, storageKeySavedGraphs, supportsUpload, tabs } = config;

  // Persisted payload
  const [payload, setPayload] = useLocalStorage<string>(storageKeyPayload, '');

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

  // Auto-last: the most recent plain-text response to a `help` or `describe`
  // (non-graph) command.  Strategy: walk the message list backwards to find
  // the most recent echoed command that qualifies (isHelpOrDescribeEcho), then
  // take the first non-JSON, non-graph-link message that follows it.
  // Only computed when the Developer Guides tab is enabled for this playground.
  const lastNonJsonMessage = useMemo<string | null>(() => {
    if (!tabs.includes('preview')) return null;

    // Find the index of the most recent qualifying echo.
    let echoIndex = -1;
    for (let i = ws.messages.length - 1; i >= 0; i--) {
      if (isHelpOrDescribeEcho(ws.messages[i].raw)) {
        echoIndex = i;
        break;
      }
    }
    // No qualifying command has been sent yet — nothing to preview.
    if (echoIndex === -1) return null;

    // Scan forward from the echo to find the first response message that is
    // renderable as Markdown (plain-text, not a graph-link, not a JSON event).
    for (let i = echoIndex + 1; i < ws.messages.length; i++) {
      const raw = ws.messages[i].raw;
      if (!isGraphLinkMessage(raw) && isMarkdownCandidate(raw)) return raw;
    }
    return null;
  }, [ws.messages, tabs]);

  // Resolve the pinned id back to its raw string for MarkdownPreview.
  // pinnedMessageId wins; falls back to auto-last.
  // Only computed when the Developer Guides tab is enabled for this playground.
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

