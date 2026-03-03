import { useReducer, useEffect, useRef, useCallback } from 'react';
import { useLocalStorage } from './useLocalStorage';
import { type ToastType } from './useToast';
import { MAX_BUFFER, MAX_HISTORY } from '../config/playgrounds';
import { useWebSocketContext } from '../contexts/WebSocketContext';
import { extractUploadPath } from '../utils/messageParser';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

/** Local UI state that does NOT need to persist across navigation. */
interface LocalState {
  command:      string;
  autoScroll:   boolean;
  historyIndex: number;
}

/** Every action the local reducer can handle. */
type LocalAction =
  | { type: 'SET_COMMAND';       value: string }
  | { type: 'CLEAR_COMMAND' }
  | { type: 'SET_HISTORY_INDEX'; index: number; command: string }
  | { type: 'TOGGLE_AUTO_SCROLL' };

/** Options accepted by useWebSocket. */
export interface UseWebSocketOptions {
  wsPath:            string;
  storageKeyHistory: string;
  payload:           string;
  addToast:          (message: string, type?: ToastType) => void;
}

/** Public API surface returned by useWebSocket. */
export interface UseWebSocketReturn {
  connected:        boolean;
  connecting:       boolean;
  messages:         { id: number; raw: string }[];
  command:          string;
  setCommand:       (value: string) => void;
  connect:          () => void;
  disconnect:       () => void;
  sendCommand:      () => void;
  handleKeyDown:    (e: React.KeyboardEvent<HTMLElement>) => void;
  consoleRef:       React.RefObject<HTMLDivElement | null>;
  autoScroll:       boolean;
  toggleAutoScroll: () => void;
  copyMessages:     () => void;
  clearMessages:    () => void;
  uploadPayload:    () => void;
}

// ---------------------------------------------------------------------------
// Local reducer (UI-only state — not shared across routes)
// ---------------------------------------------------------------------------

const localInitial: LocalState = {
  command:      '',
  autoScroll:   true,
  historyIndex: -1,
};

function localReducer(state: LocalState, action: LocalAction): LocalState {
  switch (action.type) {
    case 'SET_COMMAND':
      return { ...state, command: action.value };
    case 'CLEAR_COMMAND':
      return { ...state, command: '', historyIndex: -1 };
    case 'SET_HISTORY_INDEX':
      return { ...state, historyIndex: action.index, command: action.command };
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
 * Thin wrapper that delegates connection / message state to WebSocketContext
 * (so it persists across route changes) while keeping purely local UI state
 * (command input, history cursor, auto-scroll) in a component-scoped reducer.
 *
 * Public API is unchanged — Playground.tsx needs no edits.
 */
export function useWebSocket({ wsPath, storageKeyHistory, payload, addToast }: UseWebSocketOptions): UseWebSocketReturn {

  // Shared context — connection phase + messages live here, surviving navigation.
  const ctx = useWebSocketContext();

  // Pull this playground's slot from the shared store.
  const { phase, messages } = ctx.getSlot(wsPath);
  const connected  = phase === 'connected';
  const connecting = phase === 'connecting';

  // Local UI state (not shared, resets on remount — that's intentional).
  const [localState, dispatch] = useReducer(localReducer, localInitial);
  const { command, autoScroll, historyIndex } = localState;

  // Persisted command history (keyed per playground so they stay separate).
  const [history, setHistory] = useLocalStorage<string[]>(storageKeyHistory, []);

  // consoleRef is only needed for auto-scroll; it is purely presentational.
  const consoleRef = useRef<HTMLDivElement>(null);

  // When true, the next incoming message that contains an upload URL is
  // consumed to fire the HTTP POST.  A ref (not state) avoids a re-render.
  const pendingUploadRef = useRef(false);

  // --- Auto-scroll effect ---
  useEffect(() => {
    if (autoScroll && consoleRef.current) {
      consoleRef.current.scrollTop = consoleRef.current.scrollHeight;
    }
  }, [messages, autoScroll]);

  // --- Public: connect ---
  const connect = useCallback(() => {
    ctx.connect(wsPath, addToast);
  }, [ctx, wsPath, addToast]);

  // --- Public: disconnect ---
  const disconnect = useCallback(() => {
    ctx.disconnect(wsPath);
  }, [ctx, wsPath]);

  // --- Public: send the current command ---
  const sendCommand = useCallback(() => {
    if (phase !== 'connected') return;
    const text = command.trim();
    if (text.length === 0) return;

    ctx.send(wsPath, text);
    if (history[0] !== text) {
      setHistory((prev) => [text, ...prev].slice(0, MAX_HISTORY));
    }

    // Special "load" command — also send the payload as a second message.
    if (text === 'load') {
      if (payload.length === 0) {
        ctx.appendMessage(wsPath, 'ERROR: please paste JSON/XML payload in input text area');
      } else {
        ctx.send(wsPath, payload);
      }
    }

    dispatch({ type: 'CLEAR_COMMAND' });
  }, [ctx, wsPath, phase, command, payload, history, setHistory]);

  // --- Public: keyboard handler (history navigation only) ---
  const handleKeyDown = useCallback((e: React.KeyboardEvent<HTMLElement>) => {
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
    }
  }, [history, historyIndex]);

  // --- Watch incoming messages for the upload URL when a POST is pending ---
  useEffect(() => {
    if (!pendingUploadRef.current || messages.length === 0) return;
    const latest = messages[messages.length - 1].raw;
    const uploadPath = extractUploadPath(latest);
    if (!uploadPath) return;

    // Consume the pending flag immediately so a duplicate message doesn't
    // re-trigger the POST.
    pendingUploadRef.current = false;

    if (payload.length === 0) {
      ctx.appendMessage(wsPath, 'ERROR: please paste JSON/XML payload in the input text area');
      return;
    }

    let body: string;
    try {
      // Re-serialise through JSON.parse to ensure the server receives clean JSON.
      body = JSON.stringify(JSON.parse(payload));
    } catch {
      ctx.appendMessage(wsPath, 'ERROR: payload is not valid JSON — cannot upload');
      return;
    }

    fetch(uploadPath, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
    })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        addToast('Payload uploaded successfully', 'success');
      })
      .catch((err: Error) => {
        ctx.appendMessage(wsPath, `ERROR: upload failed — ${err.message}`);
        addToast(`Upload failed: ${err.message}`, 'error');
      });
  }, [messages, payload, wsPath, ctx, addToast]);

  // --- Public: trigger the two-step upload handshake ---
  const uploadPayload = useCallback(() => {
    if (phase !== 'connected') return;
    if (payload.length === 0) {
      addToast('Nothing to upload — paste a JSON payload first', 'error');
      return;
    }
    // Tell the server we want to upload; it will reply with the upload URL.
    pendingUploadRef.current = true;
    ctx.send(wsPath, 'upload');
  }, [ctx, wsPath, phase, payload, addToast]);

  // --- Console helpers ---
  const toggleAutoScroll = useCallback(() => dispatch({ type: 'TOGGLE_AUTO_SCROLL' }), []);

  const copyMessages = useCallback(() => {
    navigator.clipboard.writeText(messages.map((m: { id: number; raw: string }) => m.raw).join('\n'));
    addToast('Console copied to clipboard!', 'success');
  }, [messages, addToast]);

  const clearMessages = useCallback(() => {
    ctx.clearMessages(wsPath);
    addToast('Console cleared', 'info');
  }, [ctx, wsPath, addToast]);

  const setCommand = useCallback(
    (value: string) => dispatch({ type: 'SET_COMMAND', value }),
    []
  );

  return {
    connected,
    connecting,
    messages,
    command,
    setCommand,
    connect,
    disconnect,
    sendCommand,
    handleKeyDown,
    consoleRef,
    autoScroll,
    toggleAutoScroll,
    copyMessages,
    clearMessages,
    uploadPayload,
  };
}
