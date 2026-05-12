import { useCallback, useEffect, useRef, useState } from 'react';
import type { ProtocolBus } from '../../protocol/bus';
import type { CreateNodeTextResultEvent } from '../../protocol/events';
import type { MinigraphGraphData } from '../../utils/graphTypes';
import type { CreateNodeTextResult } from '../../utils/messageParser';
import type { GraphAuthoringExecutor } from '../../graphActions/graphAuthoringExecutor';
import { buildCreateNodeCommand } from '../../graphActions/minigraphCommandBuilder';
import type { NodeDraft, NodeDraftSource, NodeDraftValidationErrors } from '../../graphActions/nodeAuthoringTypes';
import { createDefaultNodeDraft } from '../../graphActions/propertyRows';
import { validateNodeDraft } from '../../graphActions/validation';

export const DEFAULT_AUTHORING_TIMEOUT_MS = 10_000;

export type AuthoringState =
  | { status: 'closed' }
  | {
      status: 'open';
      action: 'create-node';
      phase: 'editing' | 'sending';
      draft: NodeDraft;
      pendingSubmit: PendingCreateNodeSubmit | null;
      serverMessage: string | null;
      connectionLost: boolean;
    };

export interface PendingCreateNodeSubmit {
  alias: string;
  command: string;
  sentAt: string;
}

export interface UseGraphAuthoringOptions {
  bus: ProtocolBus;
  connected: boolean;
  graphData: MinigraphGraphData | null;
  executor: GraphAuthoringExecutor;
  timeoutMs?: number;
  onAccepted?: (result: CreateNodeTextResult) => void;
}

export interface UseGraphAuthoringReturn {
  state: AuthoringState;
  validationErrors: NodeDraftValidationErrors;
  openCreateNode: (source: NodeDraftSource) => void;
  updateDraft: (draft: NodeDraft) => void;
  submit: () => void;
  close: () => void;
}

const SEND_FAILURE_MESSAGE = 'Could not send the create-node command because the WebSocket is not open. The draft was preserved in this dialog.';
const TIMEOUT_MESSAGE = 'The create-node command was sent, but no backend result was observed yet. The outcome is unknown.';
const DISCONNECTED_EDITING_MESSAGE = 'Connection disconnected. This graph session may no longer be valid. Refresh the page and create the node again after the app reconnects.';
const DISCONNECTED_SENDING_MESSAGE = 'Connection disconnected while the create-node command was pending. The outcome is unknown. Refresh the page and check the graph before trying again.';

// Owns the complete create-node lifecycle: draft state, validation, raw-command
// send, best-effort text-result matching, timeout handling, and disconnect
// handling. UI components should call this hook instead of sending commands or
// interpreting backend text themselves.
export function useGraphAuthoring({
  bus,
  connected,
  graphData,
  executor,
  timeoutMs = DEFAULT_AUTHORING_TIMEOUT_MS,
  onAccepted,
}: UseGraphAuthoringOptions): UseGraphAuthoringReturn {
  const [state, setState] = useState<AuthoringState>({ status: 'closed' });
  const [validationErrors, setValidationErrors] = useState<NodeDraftValidationErrors>({});

  const stateRef = useRef(state);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const wasConnectedRef = useRef(connected);
  const graphDataRef = useRef(graphData);
  const onAcceptedRef = useRef(onAccepted);

  // ProtocolBus and timers can fire after render, so callbacks read refs for
  // the latest state/context without re-subscribing on every draft keystroke.
  useEffect(() => { stateRef.current = state; }, [state]);
  useEffect(() => { graphDataRef.current = graphData; }, [graphData]);
  useEffect(() => { onAcceptedRef.current = onAccepted; }, [onAccepted]);

  const setAuthoringState = useCallback((next: AuthoringState) => {
    stateRef.current = next;
    setState(next);
  }, []);

  const clearPendingTimer = useCallback(() => {
    if (timeoutRef.current !== null) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, []);

  const openCreateNode = useCallback((source: NodeDraftSource) => {
    if (!connected) return;
    const draft = createDefaultNodeDraft(source);
    setValidationErrors({});
    setAuthoringState({
      status: 'open',
      action: 'create-node',
      phase: 'editing',
      draft,
      pendingSubmit: null,
      serverMessage: null,
      connectionLost: false,
    });
  }, [connected, setAuthoringState]);

  const updateDraft = useCallback((draft: NodeDraft) => {
    const current = stateRef.current;
    if (current.status !== 'open') return;
    if (current.phase === 'sending' || current.connectionLost) return;

    setValidationErrors({});
    setAuthoringState({
      ...current,
      draft,
      pendingSubmit: null,
      serverMessage: null,
      connectionLost: false,
    });
  }, [setAuthoringState]);

  const startSubmitTimer = useCallback(() => {
    clearPendingTimer();
    timeoutRef.current = setTimeout(() => {
      const current = stateRef.current;
      if (current.status !== 'open' || current.phase !== 'sending' || !current.pendingSubmit) return;
      setAuthoringState({
        ...current,
        phase: 'editing',
        serverMessage: TIMEOUT_MESSAGE,
      });
      timeoutRef.current = null;
    }, timeoutMs);
  }, [clearPendingTimer, setAuthoringState, timeoutMs]);

  // Submit is intentionally conservative: validate first, build command second,
  // call the executor third. No optimistic graph mutation happens here.
  const submit = useCallback(() => {
    const current = stateRef.current;
    if (current.status !== 'open') return;
    if (current.phase === 'sending') return;
    if (current.connectionLost) return;
    if (!connected) {
      setAuthoringState({
        ...current,
        serverMessage: SEND_FAILURE_MESSAGE,
      });
      return;
    }

    const validation = validateNodeDraft(current.draft, { graphData: graphDataRef.current });
    if (!validation.valid) {
      setValidationErrors(validation.errors);
      return;
    }

    let command: string;
    try {
      command = buildCreateNodeCommand(current.draft);
    } catch (err) {
      setValidationErrors({ command: err instanceof Error ? err.message : String(err) });
      return;
    }

    const sent = executor.execute(command);
    if (!sent) {
      setAuthoringState({
        ...current,
        phase: 'editing',
        pendingSubmit: null,
        serverMessage: SEND_FAILURE_MESSAGE,
      });
      return;
    }

    const pending: PendingCreateNodeSubmit = {
      alias: current.draft.alias.trim(),
      command,
      sentAt: new Date().toISOString(),
    };
    setValidationErrors({});
    setAuthoringState({
      ...current,
      phase: 'sending',
      pendingSubmit: pending,
      serverMessage: null,
      connectionLost: false,
    });
    startSubmitTimer();
  }, [connected, executor, setAuthoringState, startSubmitTimer]);

  const close = useCallback(() => {
    const current = stateRef.current;
    if (current.status !== 'open') return;
    if (current.phase === 'sending') return;
    clearPendingTimer();
    setValidationErrors({});
    setAuthoringState({ status: 'closed' });
  }, [clearPendingTimer, setAuthoringState]);

  useEffect(() => {
    return bus.on('minigraph.createNode.textResult', (event: CreateNodeTextResultEvent) => {
      const current = stateRef.current;
      if (current.status !== 'open' || !current.pendingSubmit) return;

      const pending = current.pendingSubmit;
      if (event.status === 'accepted' && event.alias === pending.alias) {
        clearPendingTimer();
        setValidationErrors({});
        setAuthoringState({ status: 'closed' });
        onAcceptedRef.current?.({ status: event.status, alias: event.alias, message: event.message });
        return;
      }

      if (event.status === 'rejected' && event.alias === pending.alias) {
        clearPendingTimer();
        setAuthoringState({
          ...current,
          phase: 'editing',
          pendingSubmit: null,
          serverMessage: event.message,
        });
        return;
      }

      if (event.status === 'error') {
        clearPendingTimer();
        setAuthoringState({
          ...current,
          phase: 'editing',
          pendingSubmit: null,
          serverMessage: `Backend returned an error while this submit was pending: ${event.message}`,
        });
      }
    });
  }, [bus, clearPendingTimer, setAuthoringState]);

  // A disconnect changes the backend WebSocket session. The draft remains only
  // in the currently open dialog; closing or refreshing discards it.
  useEffect(() => {
    if (wasConnectedRef.current && !connected) {
      const current = stateRef.current;
      if (current.status === 'open') {
        clearPendingTimer();
        const message = current.pendingSubmit ? DISCONNECTED_SENDING_MESSAGE : DISCONNECTED_EDITING_MESSAGE;
        setAuthoringState({
          ...current,
          phase: 'editing',
          pendingSubmit: null,
          serverMessage: message,
          connectionLost: true,
        });
      }
    }
    wasConnectedRef.current = connected;
  }, [clearPendingTimer, connected, setAuthoringState]);

  useEffect(() => {
    return () => {
      clearPendingTimer();
    };
  }, [clearPendingTimer]);

  return {
    state,
    validationErrors,
    openCreateNode,
    updateDraft,
    submit,
    close,
  };
}
