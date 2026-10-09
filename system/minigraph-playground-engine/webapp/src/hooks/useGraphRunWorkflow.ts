import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { MinigraphGraphData } from '../utils/graphTypes';
import type { ProtocolBus } from '../protocol/bus';
import type { ToastType } from './useToast';
import { collectGraphInputBodyPaths, collectGraphInputHeaderNames } from '../graphRun/graphInputPaths';
import {
  GRAPH_RUN_COMMANDS,
  isInstantiateCommandText,
  isRunCommandText,
} from '../graphRun/graphRunProtocol';

export const GRAPH_RUN_SETUP_TIMEOUT_MS = 10_000;

/**
 * The run lifecycle is three explicit steps — Instantiate, Upload (optional),
 * Run — and this hook mirrors only the two the engine acknowledges:
 * `instantiate graph` and `run`. Uploading mock input is a local action
 * (the Playground opens the upload form for this session's own
 * `/api/mock/{sessionId}` endpoint); the engine confirms the upload in the
 * console of every member of a collaborative session, and nothing about the
 * run phase changes, so a graph that needs no input (tutorial 1) runs right
 * after Instantiate.
 */
export type GraphRunPhase =
  | 'idle'
  | 'instantiating'
  | 'ready'
  | 'running'
  | 'outcome-uncertain';

type PendingSignal = 'instance-created' | 'run-terminal' | null;

interface WorkflowState {
  phase: GraphRunPhase;
  pendingSignal: PendingSignal;
  /**
   * True when this session's own button sent the outstanding command. A typed
   * console command, or one replayed from another member of a collaborative
   * session, is mirrored into the phase without a toast.
   */
  ownAction: boolean;
  /** The graph changed while an earlier backend command was still outstanding. */
  invalidated: boolean;
}

export interface UseGraphRunWorkflowOptions {
  enabled: boolean;
  bus: ProtocolBus;
  connected: boolean;
  connectionEpoch: number | null;
  graphData: MinigraphGraphData | null;
  graphIdentity: string | null;
  /**
   * False while this session is subscribed to another. It does not gate the
   * controls: subscribed sessions are equal partners, and the backend runs a
   * subscriber's command through the primary in every member's session. A
   * change (subscribe or unsubscribe) reroutes commands, so it resets the run.
   */
  isPrimary: boolean;
  sendRawText: (text: string) => boolean;
  addToast: (message: string, type?: ToastType) => void;
}

export interface UseGraphRunWorkflowReturn {
  phase: GraphRunPhase;
  ready: boolean;
  busy: boolean;
  canInteract: boolean;
  canInstantiate: boolean;
  /** Upload is enabled once an instance exists; it is optional. */
  canUpload: boolean;
  canRun: boolean;
  disabledReason: string;
  /** The `input.body.*` paths the graph references — UI hints only. */
  inputBodyPaths: string[];
  /** The input header names the graph references — UI hints only. */
  inputHeaderNames: string[];
  runGraph: () => boolean;
  instantiateGraph: () => boolean;
}

const IDLE_STATE: WorkflowState = {
  phase: 'idle',
  pendingSignal: null,
  ownAction: false,
  invalidated: false,
};

export function useGraphRunWorkflow({
  enabled,
  bus,
  connected,
  connectionEpoch,
  graphData,
  graphIdentity,
  isPrimary,
  sendRawText,
  addToast,
}: UseGraphRunWorkflowOptions): UseGraphRunWorkflowReturn {
  const [state, setState] = useState<WorkflowState>(IDLE_STATE);
  const stateRef = useRef<WorkflowState>(IDLE_STATE);
  const sendRawTextRef = useRef(sendRawText);
  const addToastRef = useRef(addToast);
  const inputBodyPaths = useMemo(() => collectGraphInputBodyPaths(graphData), [graphData]);
  const inputHeaderNames = useMemo(() => collectGraphInputHeaderNames(graphData), [graphData]);
  const needsInput = inputBodyPaths.length > 0 || inputHeaderNames.length > 0;
  const inputBodyPathsRef = useRef(inputBodyPaths);
  const needsInputRef = useRef(needsInput);

  useEffect(() => { sendRawTextRef.current = sendRawText; }, [sendRawText]);
  useEffect(() => { addToastRef.current = addToast; }, [addToast]);
  useEffect(() => { inputBodyPathsRef.current = inputBodyPaths; }, [inputBodyPaths]);
  useEffect(() => { needsInputRef.current = needsInput; }, [needsInput]);

  const transition = useCallback((next: WorkflowState) => {
    stateRef.current = next;
    setState(next);
  }, []);

  const reset = useCallback(() => {
    transition({ ...IDLE_STATE });
  }, [transition]);

  /**
   * Keep an outstanding backend response quarantined after a graph mutation.
   * The text protocol has no correlation IDs, so unlocking early would let the
   * old acknowledgement complete a later button click.
   */
  const invalidate = useCallback(() => {
    const current = stateRef.current;
    if (current.pendingSignal !== null) {
      transition({
        ...current,
        phase: 'outcome-uncertain',
        invalidated: true,
      });
      return;
    }
    reset();
  }, [reset, transition]);

  const sendWithState = useCallback((
    command: string,
    next: WorkflowState,
    failureMessage: string,
  ): boolean => {
    transition(next);
    if (sendRawTextRef.current(command)) return true;
    reset();
    addToastRef.current(failureMessage, 'error');
    return false;
  }, [reset, transition]);

  const canInteract = enabled && connected && graphData !== null;
  const canInstantiate = canInteract && (state.phase === 'idle' || state.phase === 'ready');
  const canUpload = canInteract && state.phase === 'ready';
  const canRun = canInteract && state.phase === 'ready';
  const disabledReason = !enabled
    ? 'Graph run controls are unavailable'
    : !graphData
      ? 'Load a graph first'
      : !connected
        ? 'Connect first to run the graph'
        : '';

  const runGraph = useCallback((): boolean => {
    if (!canRun) return false;
    const current = stateRef.current;
    if (current.phase !== 'ready') return false;
    return sendWithState(
      GRAPH_RUN_COMMANDS.run,
      { phase: 'running', pendingSignal: 'run-terminal', ownAction: true, invalidated: false },
      'Could not run graph because the WebSocket is not open.',
    );
  }, [canRun, sendWithState]);

  const instantiateGraph = useCallback((): boolean => {
    if (!canInstantiate) return false;
    const current = stateRef.current;
    if (current.phase !== 'idle' && current.phase !== 'ready') return false;
    return sendWithState(
      GRAPH_RUN_COMMANDS.instantiate,
      { phase: 'instantiating', pendingSignal: 'instance-created', ownAction: true, invalidated: false },
      'Could not instantiate graph because the WebSocket is not open.',
    );
  }, [canInstantiate, sendWithState]);

  useEffect(() => {
    const offCreated = bus.on('graph.instance.created', () => {
      const current = stateRef.current;
      if (current.pendingSignal !== 'instance-created') return;
      if (current.invalidated) {
        reset();
        return;
      }
      transition({ phase: 'ready', pendingSignal: null, ownAction: false, invalidated: false });
      // A typed or replayed instantiate is mirrored as Ready without a toast.
      if (!current.ownAction) return;
      addToastRef.current(
        needsInputRef.current
          ? 'Graph instantiated. Upload mock input if the run needs it, then run.'
          : 'Graph instantiated and ready to run.',
        'success',
      );
    });

    const offCleared = bus.on('graph.instance.cleared', reset);
    const offMutation = bus.on('graph.mutation', invalidate);
    // A confirmed session restart is authoritative: the old session-bound
    // instance and its acknowledgements can no longer be actionable.
    const offReset = bus.on('session.reset', reset);
    const offExported = bus.on('graph.exported', invalidate);

    const offTerminal = bus.on('graph.run.terminal', (event) => {
      const current = stateRef.current;
      if (current.pendingSignal !== 'run-terminal') return;
      const shouldReportAbort = !current.invalidated && event.status === 'aborted';
      reset();
      if (shouldReportAbort) {
        addToastRef.current('Graph run aborted. See the console for details.', 'error');
      }
    });

    const offError = bus.on('command.error', (event) => {
      const current = stateRef.current;
      if (current.pendingSignal !== 'instance-created') return;
      const shouldReport = !current.invalidated;
      reset();
      if (shouldReport) {
        addToastRef.current(`Could not instantiate graph: ${event.message}`, 'error');
      }
    });

    const offEcho = bus.on('command.echo', (event) => {
      const current = stateRef.current;
      if (current.phase !== 'idle' && current.phase !== 'ready') return;
      if (isInstantiateCommandText(event.commandText)) {
        transition({
          phase: 'instantiating',
          pendingSignal: 'instance-created',
          ownAction: false,
          invalidated: false,
        });
      } else if (isRunCommandText(event.commandText)) {
        transition({
          phase: 'running',
          pendingSignal: 'run-terminal',
          ownAction: false,
          invalidated: false,
        });
      }
    });

    return () => {
      offCreated();
      offCleared();
      offMutation();
      offReset();
      offExported();
      offTerminal();
      offError();
      offEcho();
    };
  }, [bus, invalidate, reset, transition]);

  useEffect(() => {
    if (state.phase !== 'instantiating') return;
    const timer = setTimeout(() => {
      const current = stateRef.current;
      if (current.phase === 'instantiating') {
        transition({ ...current, phase: 'outcome-uncertain' });
        addToastRef.current(
          'Graph setup is taking longer than expected. Waiting for the backend outcome…',
          'info',
        );
      }
    }, GRAPH_RUN_SETUP_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [state.phase, transition]);

  const previousIdentityRef = useRef(graphIdentity);
  useEffect(() => {
    if (previousIdentityRef.current !== graphIdentity) invalidate();
    previousIdentityRef.current = graphIdentity;
  }, [graphIdentity, invalidate]);

  const previousEpochRef = useRef(connectionEpoch);
  useEffect(() => {
    if (previousEpochRef.current !== connectionEpoch) reset();
    previousEpochRef.current = connectionEpoch;
  }, [connectionEpoch, reset]);

  const previousPrimaryRef = useRef(isPrimary);
  useEffect(() => {
    if (previousPrimaryRef.current !== isPrimary) reset();
    previousPrimaryRef.current = isPrimary;
  }, [isPrimary, reset]);

  useEffect(() => {
    if (!enabled || !connected) {
      reset();
    } else if (!graphData) {
      invalidate();
    }
  }, [enabled, connected, graphData, reset, invalidate]);

  const busy = state.phase !== 'idle' && state.phase !== 'ready';
  return {
    phase: state.phase,
    ready: state.phase === 'ready',
    busy,
    canInteract,
    canInstantiate,
    canUpload,
    canRun,
    disabledReason,
    inputBodyPaths,
    inputHeaderNames,
    runGraph,
    instantiateGraph,
  };
}
