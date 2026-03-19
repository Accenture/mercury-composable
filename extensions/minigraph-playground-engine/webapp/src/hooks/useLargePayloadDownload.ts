import { useEffect, useRef } from 'react';
import { extractLargePayloadLink } from '../utils/messageParser';
import { type ToastType } from './useToast';

export interface UseLargePayloadDownloadOptions {
  /**
   * Full incoming message log from the WebSocket slot.
   * Only messages with IDs greater than the watermark set at mount time are
   * processed — historical messages are never replayed.
   */
  messages: { id: number; raw: string }[];

  /**
   * Whether the WebSocket is currently connected.
   * The pending state is cleared on disconnect to prevent cross-session
   * contamination (mirrors the pattern in useAutoMarkdownPin and
   * useAutoGraphRefresh).
   */
  connected: boolean;

  /**
   * Callback to append a local-only message to this playground's console.
   * Used to surface download errors or success info without a round-trip
   * through the WebSocket.
   */
  appendMessage: (raw: string) => void;

  addToast: (msg: string, type?: ToastType) => void;
}

/**
 * Watches the WebSocket message stream for large-payload download links.
 *
 * When the server sends a message matching:
 *   "Large payload (<bytes>) -> GET /api/inspect/<id>/<namespace>"
 *
 * this hook automatically:
 *  1. Issues a GET request to the backend for that path.
 *  2. Triggers a browser file download with a sensible filename derived
 *     from the last path segment (e.g. "input.body.json").
 *  3. Shows a success toast on completion, or surfaces an error toast and
 *     appends an error line to the console on failure.
 *
 * Behaviour is consistent with the existing two-step upload handshake in
 * useWebSocket and the mutation-detection in useAutoGraphRefresh:
 *  - A message-ID watermark set at mount prevents replaying history.
 *  - A ref (not state) tracks any in-flight download to avoid duplicate
 *    requests if the same message is processed twice.
 *  - The pending flag is cleared on disconnect to prevent stale downloads
 *    from being triggered after reconnection.
 *  - AbortController is used to cancel the fetch on unmount.
 *
 * This hook has no return value — all behaviour is side-effect only.
 */
export function useLargePayloadDownload({
  messages,
  connected,
  appendMessage,
  addToast,
}: UseLargePayloadDownloadOptions): void {

  // ── Message-ID watermark ──────────────────────────────────────────────────
  // Set at mount to the highest existing message ID so that messages already
  // in the log (from a previous session or navigation) are never replayed.
  const watermarkRef = useRef<number>(-1);

  // ── In-flight download AbortController ───────────────────────────────────
  // Holds the AbortController for the currently in-flight GET request so it
  // can be cancelled on unmount.  null when no download is in flight.
  const abortRef = useRef<AbortController | null>(null);

  // ── Initialise watermark at mount ─────────────────────────────────────────
  useEffect(() => {
    if (messages.length > 0) {
      watermarkRef.current = messages[messages.length - 1].id;
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
  // Intentionally empty dep array — snapshot only on mount.

  // ── Cancel in-flight download on disconnect ───────────────────────────────
  // Prevents a download started during one session from completing (and
  // triggering toasts/console messages) after the socket has been closed.
  useEffect(() => {
    if (!connected) {
      abortRef.current?.abort();
      abortRef.current = null;
    }
  }, [connected]);

  // ── Cancel in-flight download on unmount ─────────────────────────────────
  useEffect(() => {
    return () => {
      abortRef.current?.abort();
    };
  }, []);

  // ── Main effect ───────────────────────────────────────────────────────────
  // Triggered on every messages change.  Scans only messages that arrived
  // after the watermark, looking for the first large-payload link.
  useEffect(() => {
    if (messages.length === 0) return;

    const newMessages = messages.filter(m => m.id > watermarkRef.current);
    if (newMessages.length === 0) return;

    // Advance watermark to prevent reprocessing.
    watermarkRef.current = messages[messages.length - 1].id;

    for (const msg of newMessages) {
      const link = extractLargePayloadLink(msg.raw);
      if (!link) continue;

      const { apiPath, filename, byteSize } = link;

      // Cancel any previous in-flight download before starting a new one.
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;

      const sizeMB = (byteSize / (1024 * 1024)).toFixed(2);
      addToast(`Downloading payload (${sizeMB} MB)…`, 'info');

      fetch(apiPath, { signal: controller.signal })
        .then(res => {
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          return res.text();
        })
        .then(text => {
          // Attempt to pretty-print if the response is valid JSON.
          let content = text;
          try {
            content = JSON.stringify(JSON.parse(text), null, 2);
          } catch {
            // Not JSON — download as-is.
          }

          // Trigger a browser download via a transient anchor + object URL.
          const blob = new Blob([content], { type: 'application/json' });
          const url  = URL.createObjectURL(blob);
          const a    = document.createElement('a');
          a.href     = url;
          a.download = filename;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          // Revoke after a short delay to let the browser start the download.
          setTimeout(() => URL.revokeObjectURL(url), 10_000);

          abortRef.current = null;
          addToast(`Downloaded "${filename}"`, 'success');
        })
        .catch((err: Error) => {
          if (err.name === 'AbortError') return; // intentional cancellation
          abortRef.current = null;
          appendMessage(`ERROR: large payload download failed — ${err.message}`);
          addToast(`Download failed: ${err.message}`, 'error');
        });

      // Only process the first large-payload link per batch — subsequent ones
      // (if any) will be caught in the next effect run when the watermark advances.
      break;
    }
  }, [messages, appendMessage, addToast]);
}
