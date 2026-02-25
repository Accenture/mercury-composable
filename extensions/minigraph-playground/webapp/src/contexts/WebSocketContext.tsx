/**
 * WebSocketContext
 *
 * Provides a single, navigation-persistent store of WebSocket connections
 * keyed by wsPath (e.g. "/ws/json/path", "/ws/graph/playground").
 *
 * By living above <Routes> in the component tree, connections survive when the
 * user switches playground tabs — the socket stays open and the status dot in
 * the nav bar correctly reflects every active connection simultaneously.
 */

import {
  createContext,
  useCallback,
  useContext,
  useReducer,
  useRef,
  type ReactNode,
} from 'react';
import { MAX_ITEMS, PING_INTERVAL } from '../config/playgrounds';
import { type ToastType } from '../hooks/useToast';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type WsPhase = 'idle' | 'connecting' | 'connected';

export interface WsSlot {
  phase:    WsPhase;
  messages: { id: number; raw: string }[];
  wsRef:    React.MutableRefObject<WebSocket | null>;
  msgIdRef: React.MutableRefObject<number>;
  pingRef:  React.MutableRefObject<ReturnType<typeof setInterval> | null>;
}

/** The shape of the context value exposed to consumers. */
export interface WebSocketContextValue {
  /** Get the reactive state (phase + messages) for a given wsPath. */
  getSlot: (wsPath: string) => { phase: WsPhase; messages: { id: number; raw: string }[] };
  /** Open a WebSocket connection for the given path. */
  connect: (wsPath: string, onToast: (msg: string, type?: ToastType) => void) => void;
  /** Close the connection for the given path. */
  disconnect: (wsPath: string) => void;
  /** Send a raw string on the given path's socket. */
  send: (wsPath: string, data: string) => boolean;
  /** Append a local-only error/info message to a slot's console. */
  appendMessage: (wsPath: string, raw: string) => void;
  /** Clear all messages for a given path. */
  clearMessages: (wsPath: string) => void;
}

// ---------------------------------------------------------------------------
// Internal reducer
// ---------------------------------------------------------------------------

interface SlotState {
  phase:    WsPhase;
  messages: { id: number; raw: string }[];
}

type SlotAction =
  | { type: 'CONNECTING';        path: string }
  | { type: 'CONNECTED';         path: string; id: number; msg: string }
  | { type: 'MESSAGE_RECEIVED';  path: string; id: number; msg: string }
  | { type: 'DISCONNECTED';      path: string; id: number; msg: string }
  | { type: 'CONNECT_ERROR';     path: string }
  | { type: 'CLEAR_MESSAGES';    path: string };

type AllSlots = Record<string, SlotState>;

function appendMsg(
  slots: AllSlots,
  path: string,
  id: number,
  msg: string,
): AllSlots {
  const prev = slots[path] ?? { phase: 'idle', messages: [] };
  const msgs = [...prev.messages, { id, raw: msg }];
  if (msgs.length > MAX_ITEMS) msgs.shift();
  return { ...slots, [path]: { ...prev, messages: msgs } };
}

function slotsReducer(state: AllSlots, action: SlotAction): AllSlots {
  const prev = state[action.path] ?? { phase: 'idle', messages: [] };

  switch (action.type) {
    case 'CONNECTING':
      return { ...state, [action.path]: { ...prev, phase: 'connecting' } };

    case 'CONNECTED':
      return appendMsg(
        { ...state, [action.path]: { ...prev, phase: 'connected' } },
        action.path, action.id, action.msg,
      );

    case 'MESSAGE_RECEIVED':
      return appendMsg(state, action.path, action.id, action.msg);

    case 'DISCONNECTED':
      return appendMsg(
        { ...state, [action.path]: { ...prev, phase: 'idle' } },
        action.path, action.id, action.msg,
      );

    case 'CONNECT_ERROR':
      return { ...state, [action.path]: { ...prev, phase: 'idle' } };

    case 'CLEAR_MESSAGES':
      return { ...state, [action.path]: { ...prev, messages: [] } };

    default:
      return state;
  }
}

// ---------------------------------------------------------------------------
// Context
// ---------------------------------------------------------------------------

const WebSocketContext = createContext<WebSocketContextValue | null>(null);

// ---------------------------------------------------------------------------
// Provider
// ---------------------------------------------------------------------------

export function WebSocketProvider({ children }: { children: ReactNode }) {
  const [slots, dispatch] = useReducer(slotsReducer, {} as AllSlots);

  // Per-path refs live outside the reducer so they never cause re-renders.
  // wsRefs[path] = live WebSocket instance (or null)
  // pingRefs[path] = setInterval handle for keep-alive
  // msgIdRefs[path] = monotonic message counter
  const wsRefs    = useRef<Record<string, WebSocket | null>>({});
  const pingRefs  = useRef<Record<string, ReturnType<typeof setInterval> | null>>({});
  const msgIdRefs = useRef<Record<string, number>>({});

  // Helper: derive a stable WebSocket URL from wsPath
  const makeUrl = (wsPath: string) =>
    import.meta.env.DEV
      ? `ws://localhost:3000${wsPath}`
      : `ws://${window.location.host}${wsPath}`;

  // Helper: next message id for a path
  const nextId = (path: string) => {
    msgIdRefs.current[path] = (msgIdRefs.current[path] ?? 0) + 1;
    return msgIdRefs.current[path];
  };

  const getTimestamp = () => {
    const s   = new Date().toString();
    const gmt = s.indexOf('GMT');
    return gmt > 0 ? s.substring(0, gmt).trim() : s;
  };

  const eventWithTimestamp = (type: string, message: string) =>
    JSON.stringify({ type, message, time: getTimestamp() });

  // ── connect ──────────────────────────────────────────────────────────────
  const connect = useCallback((wsPath: string, onToast: (msg: string, type?: ToastType) => void) => {
    if (!window.WebSocket) {
      onToast('WebSocket not supported by your browser', 'error');
      return;
    }
    const existing = wsRefs.current[wsPath];
    if (existing && (existing.readyState === WebSocket.OPEN || existing.readyState === WebSocket.CONNECTING)) {
      onToast('Already connected', 'error');
      return;
    }

    dispatch({ type: 'CONNECTING', path: wsPath });

    const ws = new WebSocket(makeUrl(wsPath));
    wsRefs.current[wsPath] = ws;

    ws.onopen = () => {
      dispatch({
        type: 'CONNECTED',
        path: wsPath,
        id:   nextId(wsPath),
        msg:  eventWithTimestamp('info', 'connected'),
      });
      onToast('Connected to WebSocket', 'success');
      ws.send(JSON.stringify({ type: 'welcome' }));

      pingRefs.current[wsPath] = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(eventWithTimestamp('ping', 'keep alive'));
        }
      }, PING_INTERVAL);
    };

    ws.onmessage = (evt) => {
      if (!evt.data.startsWith('{"type":"ping"')) {
        dispatch({
          type: 'MESSAGE_RECEIVED',
          path: wsPath,
          id:   nextId(wsPath),
          msg:  evt.data,
        });
      }
    };

    ws.onerror = () => {
      dispatch({ type: 'CONNECT_ERROR', path: wsPath });
    };

    ws.onclose = (evt) => {
      const ping = pingRefs.current[wsPath];
      if (ping) { clearInterval(ping); pingRefs.current[wsPath] = null; }
      dispatch({
        type: 'DISCONNECTED',
        path: wsPath,
        id:   nextId(wsPath),
        msg:  eventWithTimestamp('info', `disconnected - (${evt.code}) ${evt.reason}`),
      });
      onToast('Disconnected from WebSocket', 'info');
      wsRefs.current[wsPath] = null;
    };
  }, []);

  // ── disconnect ───────────────────────────────────────────────────────────
  const disconnect = useCallback((wsPath: string) => {
    const ws = wsRefs.current[wsPath];
    if (ws) {
      ws.close();
    } else {
      dispatch({
        type: 'MESSAGE_RECEIVED',
        path: wsPath,
        id:   nextId(wsPath),
        msg:  eventWithTimestamp('error', 'already disconnected'),
      });
    }
  }, []);

  // ── send ─────────────────────────────────────────────────────────────────
  const send = useCallback((wsPath: string, data: string): boolean => {
    const ws = wsRefs.current[wsPath];
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(data);
      return true;
    }
    return false;
  }, []);

  // ── appendMessage ────────────────────────────────────────────────────────
  const appendMessage = useCallback((wsPath: string, raw: string) => {
    dispatch({
      type: 'MESSAGE_RECEIVED',
      path: wsPath,
      id:   nextId(wsPath),
      msg:  raw,
    });
  }, []);

  // ── clearMessages ────────────────────────────────────────────────────────
  const clearMessages = useCallback((wsPath: string) => {
    dispatch({ type: 'CLEAR_MESSAGES', path: wsPath });
  }, []);

  // ── getSlot ──────────────────────────────────────────────────────────────
  const getSlot = useCallback((wsPath: string) => {
    return slots[wsPath] ?? { phase: 'idle' as WsPhase, messages: [] };
  }, [slots]);

  return (
    <WebSocketContext.Provider value={{ getSlot, connect, disconnect, send, appendMessage, clearMessages }}>
      {children}
    </WebSocketContext.Provider>
  );
}

// ---------------------------------------------------------------------------
// Consumer hook
// ---------------------------------------------------------------------------

export function useWebSocketContext(): WebSocketContextValue {
  const ctx = useContext(WebSocketContext);
  if (!ctx) throw new Error('useWebSocketContext must be used inside <WebSocketProvider>');
  return ctx;
}
