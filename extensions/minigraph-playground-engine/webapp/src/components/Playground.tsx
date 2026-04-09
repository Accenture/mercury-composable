import { useState, useMemo, useCallback, useEffect, useRef } from 'react';
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
import { useAutoMockUpload } from '../hooks/useAutoMockUpload';
import { useSavedGraphs } from '../hooks/useSavedGraphs';
import { useGraphSaveName } from '../hooks/useGraphSaveName';
import { buildNodeCommand } from '../clipboard/commandBuilder';
import { ToastContainer } from './Toast';
import Navigation from './Navigation';
import GraphSaveButton from './GraphSaveButton/GraphSaveButton';
import SavedGraphsMenu from './SavedGraphsMenu/SavedGraphsMenu';
import RightPanel from './RightPanel/RightPanel';
import LeftPanel from './LeftPanel/LeftPanel';
import { MockUploadModal } from './MockUploadModal/MockUploadModal';
import ClipboardSidebar from './ClipboardSidebar/ClipboardSidebar';
import { ClipboardDuplicateDialog } from './ClipboardSidebar/ClipboardDuplicateDialog';
import { type PlaygroundConfig, PLAYGROUND_CONFIGS } from '../config/playgrounds';
import { useWebSocketContext } from '../contexts/WebSocketContext';
import { useClipboardContext } from '../contexts/ClipboardContext';
import { ProtocolBus } from '../protocol/bus';
import { useProtocolKernel } from '../protocol/useProtocolKernel';
import type { GraphLinkEvent } from '../protocol/events';
import type { ClipboardItemRecord } from '../clipboard/db';
import type { MinigraphNode, MinigraphConnection } from '../utils/graphTypes';

interface PlaygroundProps {
  config: PlaygroundConfig;
}

export default function Playground({ config }: PlaygroundProps) {
  const { title, wsPath, storageKeyPayload, storageKeyHistory, storageKeyTab, storageKeySavedGraphs, supportsUpload, supportsClipboard, tabs } = config;

  const navigate = useNavigate();

  // Persisted payload (survives navigation / refresh via localStorage).
  const [storedPayload, setStoredPayload] = useLocalStorage<string>(storageKeyPayload, '');
  const ctx = useWebSocketContext();

  // Cross-playground payload transfer: when the user clicks ➡️ in the
  // Minigraph console to send a JSON result to JSON-Path, Playground.tsx
  // calls ctx.setPendingPayload() and navigates.  The receiving playground
  // peeks it here at mount (useState initialiser) and then consumes it
  // reactively via the effect below (if the playground is already mounted).
  // Using a separate useState means we never write cross-playground payloads
  // to localStorage. null = no override; the stored value is used instead.
  const [payloadOverride, setPayloadOverride] = useState<string | null>(() =>
    ctx.peekPendingPayload(wsPath)
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

  // ── Protocol Kernel ─────────────────────────────────────────────────────
  const busRef = useRef(new ProtocolBus());
  const bus = busRef.current;

  // All WebSocket + console logic lives in the hook
  const ws = useWebSocket({ wsPath, storageKeyHistory, payload, addToast, bus });

  const { classificationMap } = useProtocolKernel({
    messages: ws.messages, bus,
  });

  // Pinned message state — stored as a message id (number) so two messages
  // with identical text don't both appear pinned. null = no explicit pin.
  const [pinnedMessageId, setPinnedMessageId] = useState<number | null>(null);

  // Auto-last: most recently received non-JSON, non-graph-link message.
  // Only computed when the Markdown Preview tab is enabled for this playground.
  // Uses docs.response from classificationMap — stricter than the original
  // isMarkdownCandidate check (excludes echoes, mock-upload invitations,
  // upload-content-path messages, and large-payload messages).
  const lastNonJsonMessage = useMemo<string | null>(() => {
    if (!tabs.includes('preview')) return null;
    for (let i = ws.messages.length - 1; i >= 0; i--) {
      const msg = ws.messages[i];
      const events = classificationMap.get(msg.id);
      if (events?.some(e => e.kind === 'docs.response')) return msg.raw;
    }
    return null;
  }, [ws.messages, tabs, classificationMap]);

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
  const { graphData, setGraphData, rightTab, setRightTab, isRefreshing, refetchGraph } = useGraphData(pinnedGraphPath, addToast, tabs[0], storageKeyTab);

  // ── Mock-upload modal state ───────────────────────────────────────────────
  // Path extracted from the server's upload invitation.
  // null = modal closed; non-null = modal open for that specific endpoint.
  const [modalUploadPath, setModalUploadPath] = useState<string | null>(null);

  // Capture the element that triggered the modal so focus can be restored on close.
  const modalTriggerRef = useRef<HTMLElement | null>(null);

  // POST paths of invitations that have been successfully fulfilled — drives ✅ badge.
  const [successfulUploadPaths, setSuccessfulUploadPaths] = useState<Set<string>>(new Set());

  // ── Auto-refresh on mutation commands ────────────────────────────────────
  useAutoGraphRefresh({
    bus,
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
    bus,
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
    bus,
    connected:     ws.connected,
    appendMessage: ws.appendMessage,
    addToast,
  });

  // ── Mock-upload modal callbacks ───────────────────────────────────────────
  const handleOpenUploadModal = useCallback((path: string) => {
    // Capture the focused element before opening so we can restore focus on close.
    modalTriggerRef.current = document.activeElement as HTMLElement;
    setModalUploadPath(path);
  }, []);

  const handleCloseUploadModal = useCallback(() => {
    setModalUploadPath(null);
    // Restore focus to the element that triggered the modal open.
    // setTimeout ensures the dialog is fully unmounted before focus() runs.
    setTimeout(() => modalTriggerRef.current?.focus(), 0);
  }, []);

  const handleUploadSuccess = useCallback((_responseBody: string) => {
    // _responseBody is available but intentionally not surfaced per spec §2.
    setSuccessfulUploadPaths(prev => new Set([...prev, modalUploadPath!]));
    setModalUploadPath(null);
    setTimeout(() => modalTriggerRef.current?.focus(), 0);
    addToast('Mock data uploaded successfully ✓', 'success');
  }, [modalUploadPath, addToast]);

  const handleUploadError = useCallback((errorMessage: string) => {
    // Modal stays open — error is displayed inline inside the modal.
    addToast(`Upload failed: ${errorMessage}`, 'error');
  }, [addToast]);

  // ── Auto-open modal when server sends upload invitation ───────────────────
  useAutoMockUpload({
    bus,
    connected:   ws.connected,
    onOpenModal: handleOpenUploadModal,
    modalOpen:   modalUploadPath !== null,
  });

  // ── Clipboard integration ────────────────────────────────────────────────
  const clipboardCtx = useClipboardContext();

  const [clipboardOpen, setClipboardOpen] = useLocalStorage<boolean>('clipboard-sidebar-open', false);

  const [duplicateDialogState, setDuplicateDialogState] = useState<{
    pendingItem: ClipboardItemRecord;
    existingItem: ClipboardItemRecord;
  } | null>(null);

  const handlePasteToInput = useCallback((item: ClipboardItemRecord) => {
    const existsLocally = graphData?.nodes.some(n => n.alias === item.node.alias) ?? false;
    const verb = existsLocally ? 'update' : 'create';
    const command = buildNodeCommand(verb, item.node);
    ws.setCommand(command);
    addToast(`${verb === 'create' ? 'Create' : 'Update'} command for "${item.node.alias}" pasted to input`, 'info');
  }, [graphData, ws.setCommand, addToast]);

  const handleClipNode = useCallback(async (
    node: MinigraphNode,
    connections: MinigraphConnection[],
  ) => {
    try {
      const result = await clipboardCtx.clipNode(node, connections, {
        sourceWsPath: wsPath,
        sourceLabel: config.label,
      });

      switch (result.status) {
        case 'added':
          addToast(`Node "${node.alias}" clipped to clipboard`, 'success');
          break;
        case 'duplicate':
          setDuplicateDialogState({
            pendingItem: result.pendingItem,
            existingItem: result.existingItem,
          });
          break;
        case 'error':
          addToast(`Clip failed: ${result.message}`, 'error');
          break;
      }
    } catch (err) {
      addToast(`Clip failed: ${err instanceof Error ? err.message : String(err)}`, 'error');
    }
  }, [clipboardCtx, wsPath, config.label, addToast]);

  // ── Saved graphs (localStorage snapshots) ────────────────────────────────
  // Only instantiated when the playground config provides a storage key so
  // playgrounds that don't use this feature have zero overhead.
  const savedGraphs = useSavedGraphs(storageKeySavedGraphs ?? '');

  // ── Save-form default name ────────────────────────────────────────────────
  // Tracks the pre-fill name for the GraphSaveButton input with priority:
  //   1. last-saved name (if this working graph was previously saved)
  //   2. imported name   (if the graph was loaded via `import graph from …`)
  //   3. untitled-{n}    (monotonically incrementing per-playground fallback)
  //
  // The untitled counter key is derived from the saved-graphs key so it stays
  // isolated per playground (e.g. "minigraph-saved-graphs" →
  // "minigraph-untitled-counter").  When there is no saved-graphs key the hook
  // is still instantiated but is effectively unused (the save button is hidden).
  const { defaultName: graphSaveName, setLastSavedName, resetName: resetSaveName } = useGraphSaveName(
    storageKeySavedGraphs ? `${storageKeySavedGraphs}-untitled-counter` : 'untitled-counter',
    bus,
  );

  // ── Pending graph export ──────────────────────────────────────────────────
  // Tracks a save that is waiting for server confirmation.
  // null = no pending save; string = the name being exported.
  const pendingSaveRef = useRef<string | null>(null);

  // Save the current graph under the given name.
  // When connected, the localStorage write and success toast are deferred
  // until the server confirms the export (graph.link → success, lifecycle
  // error → failure).  When disconnected, the bookmark is written
  // optimistically so the user can reconnect and load it later.
  const handleSaveGraph = useCallback((name: string) => {
    setLastSavedName(name);
    if (ws.connected) {
      pendingSaveRef.current = name;
      ws.sendRawText(`export graph as ${name}`);
    } else {
      savedGraphs.saveGraph(name);
      addToast(`Graph saved as "${name}"`, 'success');
    }
  }, [savedGraphs.saveGraph, setLastSavedName, ws.connected, ws.sendRawText, addToast]);

  // Confirm pending save: server responded with a graph-link after export.
  useEffect(() => {
    return bus.on('graph.link', () => {
      if (pendingSaveRef.current === null) return;
      const name = pendingSaveRef.current;
      pendingSaveRef.current = null;
      savedGraphs.saveGraph(name);
      addToast(`Graph saved as "${name}"`, 'success');
    });
  }, [bus, savedGraphs.saveGraph, addToast]);

  // Reject pending save: server responded with a plain-text error after export.
  // The backend sends export errors as raw text (not JSON lifecycle events),
  // which the classifier maps to docs.response.  Any docs.response while a
  // save is pending means the export was rejected.
  useEffect(() => {
    return bus.on('docs.response', (event) => {
      if (pendingSaveRef.current === null) return;
      pendingSaveRef.current = null;
      addToast(`Save failed: ${event.raw}`, 'error');
    });
  }, [bus, addToast]);

  // Safety net: also handle JSON lifecycle errors in case future server
  // versions send export errors as structured JSON.
  useEffect(() => {
    return bus.on('lifecycle', (event) => {
      if (pendingSaveRef.current === null) return;
      if (event.type !== 'error') return;
      pendingSaveRef.current = null;
      addToast(`Save failed: ${event.message}`, 'error');
    });
  }, [bus, addToast]);

  // Clear pending save on disconnect so it doesn't linger.
  useEffect(() => {
    if (!ws.connected) {
      pendingSaveRef.current = null;
    }
  }, [ws.connected]);

  // Load a saved graph by sending `import graph from {name}` over the WebSocket.
  // The backend reads {name}.json from its temp directory — the file that was
  // written by the `export graph as {name}` command issued during save.
  const handleLoadGraph = useCallback((name: string) => {
    if (!ws.connected) return;
    ws.sendRawText(`import graph from ${name}`);
    addToast(`Importing graph "${name}"…`, 'info');
  }, [ws.connected, ws.sendRawText, addToast]);

  // When a message is pinned, decide whether it is a graph link or a Markdown message.
  // Reads from classificationMap — no direct parser calls needed.
  const handlePinMessage = useCallback((msg: { id: number; raw: string }) => {
    const events = classificationMap.get(msg.id);
    const graphLink = events?.find(e => e.kind === 'graph.link') as GraphLinkEvent | undefined;
    if (graphLink) {
      setPinnedGraphPath(graphLink.apiPath);
      setPinnedMessageId(msg.id);
    } else {
      setPinnedMessageId(msg.id);
      setPinnedGraphPath(null);
    }
  }, [classificationMap]);

  // Send an inline JSON response to the JSON-Path playground payload editor.
  // Mirrors the large-payload flow: navigate first, then deposit via context.
  // If JSON-Path is not yet connected, auto-connect and defer the
  // navigation + payload deposit until the socket reaches 'connected' phase.
  const jsonPathConfig = PLAYGROUND_CONFIGS.find(c => c.tabs.includes('payload') && c.supportsUpload);

  // Holds a pending deferred send triggered while JSON-Path was still connecting.
  // Stored as a ref so the watching useEffect below can consume it without
  // needing to be in the dependency array of handleSendToJsonPath.
  const deferredSendRef = useRef<{ wsPath: string; json: string } | null>(null);

  // Watch the JSON-Path slot phase; when it reaches 'connected' and there is a
  // deferred send pending, execute it and clear the ref.
  const jsonPathWsPath = jsonPathConfig?.wsPath;
  useEffect(() => {
    if (!jsonPathWsPath || !deferredSendRef.current) return;
    const slot = ctx.getSlot(jsonPathWsPath);
    if (slot.phase === 'connected') {
      const { wsPath: targetPath, json } = deferredSendRef.current;
      deferredSendRef.current = null;
      ctx.setPendingPayload(targetPath, json);
      navigate(jsonPathConfig!.path);
      addToast('JSON loaded into JSON-Path editor ✓', 'success');
    }
  }, [jsonPathWsPath, ctx, navigate, addToast, jsonPathConfig,
      // getSlot returns a new object reference when the slot changes, so
      // reading it inside the effect (keyed on ctx) is sufficient — but we
      // also need to re-run when the slot's phase changes.  ctx.getSlot is
      // wrapped in useCallback(_, [slots]) so it changes whenever slots does,
      // which is exactly what we want.
     ]);

  const handleSendToJsonPath = useCallback((json: string) => {
    if (!jsonPathConfig) return;
    const slot = ctx.getSlot(jsonPathConfig.wsPath);
    if (slot.phase === 'connected') {
      // Already connected — deposit immediately.
      ctx.setPendingPayload(jsonPathConfig.wsPath, json);
      navigate(jsonPathConfig.path);
      addToast('JSON loaded into JSON-Path editor ✓', 'success');
    } else {
      // Not yet connected — arm a deferred send and auto-connect.
      deferredSendRef.current = { wsPath: jsonPathConfig.wsPath, json };
      if (slot.phase === 'idle') {
        ctx.connect(jsonPathConfig.wsPath, addToast);
      }
      addToast('Connecting to JSON-Path Playground…', 'info');
    }
  }, [ctx, navigate, addToast, jsonPathConfig]);

  // Responsive layout: stack panels vertically on narrow viewports
  const isMobile = useMediaQuery('(max-width: 768px)');

  // Persist panel split ratio per playground route
  const { defaultLayout, onLayoutChanged } = useDefaultLayout({
    id: config.path + '-panel-split',
    storage: localStorage,
  });

  const handleFormatPayload = useCallback(() => setPayload(formatJSON(payload)), [payload]);

  const handleClearMessages = useCallback(() => {
    ws.clearMessages();
    setPinnedMessageId(null);
    setPinnedGraphPath(null);
    setGraphData(null);
    // Reset mock-upload session state so ✅ badges clear with the console.
    // Modal upload path is NOT reset here — if the modal is open while the
    // user clears the console, it remains open for the current upload attempt.
    setSuccessfulUploadPaths(new Set());
    // Advance the untitled counter so the next saved graph gets a fresh name.
    resetSaveName();
  }, [ws.clearMessages, setGraphData, resetSaveName]);

  return (
    <div className={styles.wrapper}>
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      {modalUploadPath && (
        <MockUploadModal
          uploadPath={modalUploadPath}
          onSuccess={handleUploadSuccess}
          onClose={handleCloseUploadModal}
          onError={handleUploadError}
        />
      )}

      <header className={styles.header}>
        <h1 className={styles.title}>{title}</h1>
        <div className={styles.headerActions}>
          {storageKeySavedGraphs && (
            <GraphSaveButton
              disabled={!graphData}
              defaultName={graphSaveName}
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
          {supportsClipboard && (
            <button
              className={styles.clipboardToggle}
              onClick={() => setClipboardOpen(prev => !prev)}
              aria-label={clipboardOpen ? 'Close clipboard sidebar' : 'Open clipboard sidebar'}
              aria-pressed={clipboardOpen}
            >
              Clipboard{clipboardCtx.items.length > 0 ? ` (${clipboardCtx.items.length})` : ''}
            </button>
          )}
          <Navigation addToast={addToast} />
        </div>
      </header>

      {duplicateDialogState && (
        <ClipboardDuplicateDialog
          existingItem={duplicateDialogState.existingItem}
          pendingItem={duplicateDialogState.pendingItem}
          onReplace={async () => {
            try {
              await clipboardCtx.confirmReplace(
                duplicateDialogState.pendingItem,
                duplicateDialogState.existingItem.id,
              );
              setDuplicateDialogState(null);
              addToast(`Clipboard item "${duplicateDialogState.pendingItem.node.alias}" replaced`, 'success');
            } catch (err) {
              addToast(`Replace failed: ${err instanceof Error ? err.message : String(err)}`, 'error');
            }
          }}
          onCancel={() => {
            setDuplicateDialogState(null);
            addToast('Clip cancelled', 'info');
          }}
        />
      )}

      <Group
        className={styles.panelGroup}
        orientation={isMobile ? 'vertical' : 'horizontal'}
        defaultLayout={defaultLayout}
        onLayoutChanged={onLayoutChanged}
      >
        <Panel defaultSize={clipboardOpen ? "50%" : "60%"} minSize="25%">
          <LeftPanel
            messages={ws.messages}
            classificationMap={classificationMap}
            onCopy={ws.copyMessages}
            onClear={handleClearMessages}
            consoleRef={ws.consoleRef}
            command={ws.command}
            onCommandChange={ws.setCommand}
            onCommandKeyDown={ws.handleKeyDown}
            onSend={ws.sendCommand}
            sendDisabled={!ws.connected || !ws.command.trim()}
            inputDisabled={!ws.connected}
            commandHistory={ws.history}
            onPinMessage={handlePinMessage}
            pinnedMessageId={pinnedMessageId}
            onCopyMessage={() => addToast('Copied to clipboard', 'success')}
            onSendToJsonPath={jsonPathConfig && wsPath !== jsonPathConfig.wsPath ? handleSendToJsonPath : undefined}
            onUploadMockData={handleOpenUploadModal}
            successfulUploadPaths={successfulUploadPaths}
          />
        </Panel>
        <Separator className={styles.resizeHandle} aria-label="Resize panels" />
        <Panel defaultSize={clipboardOpen ? "30%" : "40%"} minSize="20%">
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
            onClipNode={supportsClipboard ? handleClipNode : undefined}
          />
        </Panel>
        {supportsClipboard && clipboardOpen && (
          <>
            <Separator className={styles.resizeHandle} aria-label="Resize clipboard" />
            <Panel defaultSize="20%" minSize="10%" maxSize="40%">
              <ClipboardSidebar
                connected={ws.connected}
                onPaste={handlePasteToInput}
              />
            </Panel>
          </>
        )}
      </Group>
    </div>
  );
}
