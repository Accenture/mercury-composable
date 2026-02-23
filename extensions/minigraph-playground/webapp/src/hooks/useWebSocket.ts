import { useReducer, useEffect, useRef, useCallback } from 'react';
import { useLocalStorage } from './useLocalStorage';
import { type ToastType } from './useToast';
import { MAX_ITEMS, MAX_BUFFER, PING_INTERVAL } from '../config/playgrounds';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

/** The three mutually-exclusive lifecycle phases of the WebSocket connection. */
type WsPhase = 'idle' | 'connecting' | 'connected';

/** All reactive state owned by the reducer. */
interface WsState {
  phase:        WsPhase;
  messages:     string[];
  command:      string;
  autoScroll:   boolean;
  historyIndex: number;
}

/** Every action the reducer can handle — exhaustively typed as a discriminated union. */
type WsAction =
  | { type: 'CONNECTING' }
  | { type: 'CONNECTED';         msg: string }
  | { type: 'MESSAGE_RECEIVED';  msg: string }
  | { type: 'DISCONNECTED';      msg: string }
  | { type: 'CONNECT_ERROR' }
  | { type: 'SET_COMMAND';       value: string }
  | { type: 'CLEAR_COMMAND' }
  | { type: 'SET_HISTORY_INDEX'; index: number; command: string }
  | { type: 'CLEAR_MESSAGES' }
  | { type: 'TOGGLE_AUTO_SCROLL' };

/** Options accepted by useWebSocket. */
export interface UseWebSocketOptions {
  wsPath:            string;
  storageKeyHistory: string;
  payload:           string;
  addToast:          (message: string, type?: ToastType) => void;
  /** Controls which key combination triggers a send. Defaults to 'enter'. */
  submitKey?:        'enter' | 'ctrl+enter';
}

/** Public API surface returned by useWebSocket. */
export interface UseWebSocketReturn {
  wsUrl:            string;
  connected:        boolean;
  connecting:       boolean;
  messages:         string[];
  command:          string;
  setCommand:       (value: string) => void;
  connect:          () => void;
  disconnect:       () => void;
  handleKeyDown:    (e: React.KeyboardEvent<HTMLElement>) => void;
  consoleRef:       React.RefObject<HTMLDivElement | null>;
  autoScroll:       boolean;
  toggleAutoScroll: () => void;
  copyMessages:     () => void;
  clearMessages:    () => void;
}

// ---------------------------------------------------------------------------
// Reducer
// ---------------------------------------------------------------------------

/**
 * All reactive WebSocket state lives here.
 *
 * Grouping into a reducer makes every lifecycle transition explicit and atomic,
 * preventing illegal in-between states (e.g. connected && connecting both true)
 * that are possible when multiple useState setters are called separately.
 */
const initialState: WsState = {
  phase:        'idle',
  messages:     [],
  command:      '',
  autoScroll:   true,
  historyIndex: -1,
};

function wsReducer(state: WsState, action: WsAction): WsState {
  switch (action.type) {

    case 'CONNECTING':
      return { ...state, phase: 'connecting' };

    // Atomically flip phase→connected and append the
    // "connected" info message in one render instead of three.
    case 'CONNECTED': {
      const msgs = [...state.messages, action.msg];
      if (msgs.length > MAX_ITEMS) msgs.shift();
      return { ...state, phase: 'connected', messages: msgs };
    }

    case 'MESSAGE_RECEIVED': {
      const msgs = [...state.messages, action.msg];
      if (msgs.length > MAX_ITEMS) msgs.shift();
      return { ...state, messages: msgs };
    }

    // Mirror of CONNECTED — flip phase→idle and append disconnect message.
    case 'DISCONNECTED': {
      const msgs = [...state.messages, action.msg];
      if (msgs.length > MAX_ITEMS) msgs.shift();
      return { ...state, phase: 'idle', messages: msgs };
    }

    case 'CONNECT_ERROR':
      return { ...state, phase: 'idle' };

    case 'SET_COMMAND':
      return { ...state, command: action.value };

    case 'CLEAR_COMMAND':
      return { ...state, command: '', historyIndex: -1 };

    case 'SET_HISTORY_INDEX':
      return { ...state, historyIndex: action.index, command: action.command };

    case 'CLEAR_MESSAGES':
      return { ...state, messages: [] };

    case 'TOGGLE_AUTO_SCROLL':
      return { ...state, autoScroll: !state.autoScroll };

    default:
      return state;
  }
}

// ---------------------------------------------------------------------------
// Hook
// ---------------------------------------------------------------------------

/**
 * Encapsulates all WebSocket lifecycle logic for a playground page.
 * Options and return type are defined by UseWebSocketOptions / UseWebSocketReturn.
 */
export function useWebSocket({ wsPath, storageKeyHistory, payload, addToast, submitKey = 'enter' }: UseWebSocketOptions): UseWebSocketReturn {

  // Derive the WebSocket URL from wsPath on every render so that navigating
  // between playgrounds always reflects the correct endpoint.
  // NOTE: do NOT store this in a useRef — refs only capture the initial value
  // and would silently keep the stale URL when the config prop changes.
  const wsUrl = import.meta.env.DEV
    ? `ws://localhost:3000${wsPath}`
    : `ws://${window.location.host}${wsPath}`;

  // --- State (single reducer replaces 5 × useState) ---
  const [state, dispatch] = useReducer(wsReducer, initialState);
  const { phase, messages, command, autoScroll, historyIndex } = state;

  // Derive boolean flags from phase for a backwards-compatible public API.
  const connected  = phase === 'connected';
  const connecting = phase === 'connecting';

  // Persisted command history
  const [history, setHistory] = useLocalStorage<string[]>(storageKeyHistory, []);

  // --- Refs ---
  const wsRef           = useRef<WebSocket | null>(null);
  const pingIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const consoleRef      = useRef<HTMLDivElement>(null);

  // --- Auto-scroll effect ---
  useEffect(() => {
    if (autoScroll && consoleRef.current) {
      consoleRef.current.scrollTop = consoleRef.current.scrollHeight;
    }
  }, [messages, autoScroll]);

  // --- Cleanup on unmount ---
  useEffect(() => {
    return () => {
      if (wsRef.current)           wsRef.current.close();
      if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
    };
  }, []);

  // --- Internal helpers ---
  const getTimestamp = () => {
    const s   = new Date().toString();
    const gmt = s.indexOf('GMT');
    return gmt > 0 ? s.substring(0, gmt).trim() : s;
  };

  const eventWithTimestamp = (type: string, message: string): string =>
    JSON.stringify({ type, message, time: getTimestamp() });

  const keepAlive = () => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(eventWithTimestamp('ping', 'keep alive'));
    }
  };

  // --- Public: connect ---
  const connect = useCallback(() => {
    if (!window.WebSocket) {
      addToast('WebSocket not supported by your browser', 'error');
      return;
    }
    if (connected) {
      addToast('Already connected', 'error');
      return;
    }

    dispatch({ type: 'CONNECTING' });
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      dispatch({ type: 'CONNECTED', msg: eventWithTimestamp('info', 'connected') });
      addToast('Connected to WebSocket', 'success');
      ws.send(JSON.stringify({ type: 'welcome' }));
      pingIntervalRef.current = setInterval(keepAlive, PING_INTERVAL);
    };

    ws.onmessage = (evt) => {
      if (!evt.data.startsWith('{"type":"ping"')) {
        dispatch({ type: 'MESSAGE_RECEIVED', msg: evt.data });
      }
    };

    ws.onerror = () => {
      dispatch({ type: 'CONNECT_ERROR' });
    };

    ws.onclose = (evt) => {
      if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
      dispatch({ type: 'DISCONNECTED', msg: eventWithTimestamp('info', `disconnected - (${evt.code}) ${evt.reason}`) });
      addToast('Disconnected from WebSocket', 'info');
      wsRef.current = null;
    };
  }, [connected, addToast]);

  // --- Public: disconnect ---
  const disconnect = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.close();
    } else {
      dispatch({ type: 'MESSAGE_RECEIVED', msg: eventWithTimestamp('error', 'already disconnected') });
    }
  }, []);

  // --- Public: keyboard handler for the command input ---
  const handleKeyDown = useCallback((e: React.KeyboardEvent<HTMLElement>) => {
    if (!connected) return;

    if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (history.length > 0 && historyIndex < history.length - 1) {
        const newIndex = historyIndex + 1;
        dispatch({ type: 'SET_HISTORY_INDEX', index: newIndex, command: history[newIndex] });
      }
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (historyIndex > 0) {
        const newIndex = historyIndex - 1;
        dispatch({ type: 'SET_HISTORY_INDEX', index: newIndex, command: history[newIndex] });
      } else if (historyIndex === 0) {
        dispatch({ type: 'CLEAR_COMMAND' });
      }
    } else if (e.key === 'Enter') {
      // In ctrl+enter mode, only submit when Ctrl (or ⌘ on Mac) is held.
      // Plain Enter falls through so the browser inserts a newline in the textarea.
      if (submitKey === 'ctrl+enter' && !e.ctrlKey && !e.metaKey) return;

      e.preventDefault();
      const text = command.trim();
      if (text.length === 0) return;

      // wsRef.current is guaranteed non-null when connected === true
      wsRef.current!.send(text);
      setHistory((prev) => [text, ...prev].slice(0, MAX_ITEMS));

      // Special "load" command — also send the payload as a second message
      if (text === 'load') {
        if (payload.length === 0) {
          dispatch({ type: 'MESSAGE_RECEIVED', msg: 'ERROR: please paste JSON/XML payload in input text area' });
        } else if (payload.length > MAX_BUFFER) {
          dispatch({ type: 'MESSAGE_RECEIVED', msg: `ERROR: please reduce JSON/XML payload size. It must be less than ${MAX_BUFFER} characters` });
        } else {
          wsRef.current!.send(payload);
        }
      }

      dispatch({ type: 'CLEAR_COMMAND' });
    }
  }, [connected, command, history, historyIndex, payload, submitKey, setHistory]);

  // --- Console helpers ---
  const toggleAutoScroll = useCallback(() => dispatch({ type: 'TOGGLE_AUTO_SCROLL' }), []);

  const copyMessages = useCallback(() => {
    navigator.clipboard.writeText(messages.join('\n'));
    addToast('Console copied to clipboard!', 'success');
  }, [messages, addToast]);

  const clearMessages = useCallback(() => {
    dispatch({ type: 'CLEAR_MESSAGES' });
    addToast('Console cleared', 'info');
  }, [addToast]);

  // Stable wrapper so callers that do ws.setCommand(value) keep working unchanged.
  const setCommand = useCallback(
    (value: string) => dispatch({ type: 'SET_COMMAND', value }),
    []
  );

  return {
    wsUrl,
    connected,
    connecting,
    messages,
    command,
    setCommand,
    connect,
    disconnect,
    handleKeyDown,
    consoleRef,
    autoScroll,
    toggleAutoScroll,
    copyMessages,
    clearMessages,
  };
}
