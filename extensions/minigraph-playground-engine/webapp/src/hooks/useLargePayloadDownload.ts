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
   * Whether the WebSocket is currently connected (for this playground's slot).
   * The pending state is cleared on disconnect to prevent cross-session
   * contamination.
   */
  connected: boolean;

  /**
   * Callback to append a local-only message to this playground's console.
   */
  appendMessage: (raw: string) => void;

  addToast: (msg: string, type?: ToastType) => void;
}

/**
 * Watches the WebSocket message stream for large-payload inspect links.
 *
 * When the server sends a message matching:
 *   "Large payload (<bytes>) -> GET /api/inspect/<id>/<namespace>"
 *
 * this hook fetches the payload from the backend inspect endpoint and
 * appends it directly to this playground's console as a collapsible
 * JSON row — no navigation, no pre-connection requirement.
 *
 * The user can then use the per-row ➡️ button (onSendToJsonPath) to
 * move the payload into the JSON-Path Playground editor in one click,
 * identical to the flow for inline small payloads.
 *
 * Follows the same patterns as useAutoGraphRefresh / useAutoMarkdownPin:
 *  - Message-ID watermark set at mount prevents replaying history.
 *  - isFetchingRef guard prevents re-entrancy when the appended result
 *    message triggers the effect again.
 *  - AbortController cancels in-flight fetches on disconnect or unmount.
 */
export function useLargePayloadDownload({
  messages,
  connected,
  appendMessage,
  addToast,
}: UseLargePayloadDownloadOptions): void {

  // ── Message-ID watermark ──────────────────────────────────────────────────
  const watermarkRef = useRef<number>(-1);

  // ── In-flight fetch AbortController ──────────────────────────────────────
  const abortRef = useRef<AbortController | null>(null);

  // ── Re-entrancy guard ─────────────────────────────────────────────────────
  // Prevents the appended result message from being re-processed as a new
  // large-payload notification by this hook's own main effect.
  const isFetchingRef = useRef<boolean>(false);

  // ── Initialise watermark at mount ─────────────────────────────────────────
  // Declared before the main effect so React fires it first on initial render,
  // matching the ordering guarantee used by useAutoGraphRefresh.
  useEffect(() => {
    if (messages.length > 0) {
      watermarkRef.current = messages[messages.length - 1].id;
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Cancel in-flight fetch on disconnect ──────────────────────────────────
  useEffect(() => {
    if (!connected) {
      abortRef.current?.abort();
      abortRef.current = null;
      // Clear guard so the next reconnect + large-payload message is not
      // silently skipped due to a stale isFetchingRef from the aborted request.
      isFetchingRef.current = false;
    }
  }, [connected]);

  // ── Cancel in-flight fetch on unmount ─────────────────────────────────────
  useEffect(() => {
    return () => { abortRef.current?.abort(); };
  }, []);

  // ── Main effect ───────────────────────────────────────────────────────────
  useEffect(() => {
    // Re-entrancy guard: skip the entire effect while a fetch is in flight.
    // Placed before the messages.length === 0 check so the filter() call is
    // also avoided. Non-large-payload messages that arrive during a fetch
    // remain above the watermark and are re-scanned on the next tick once
    // isFetchingRef is cleared — safe because extractLargePayloadLink returns
    // null for non-notification strings (one redundant scan, negligible cost).
    if (isFetchingRef.current) return;
    if (messages.length === 0) return;

    const newMessages = messages.filter(m => m.id > watermarkRef.current);
    if (newMessages.length === 0) return;

    // Unconditional advance: all messages up to this tick are now "seen".
    watermarkRef.current = messages[messages.length - 1].id;

    for (const msg of newMessages) {
      const link = extractLargePayloadLink(msg.raw);
      if (!link) continue;

      const { apiPath, byteSize } = link;

      // Cancel any previous in-flight fetch.
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;

      const sizeMB = (byteSize / (1024 * 1024)).toFixed(2);
      addToast(`Fetching large payload (${sizeMB} MB)…`, 'info');

      // Arm the re-entrancy guard before the async boundary.
      isFetchingRef.current = true;

      // Fetch the payload from the backend inspect endpoint.
      fetch(apiPath, { signal: controller.signal })
        .then(res => {
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          return res.text();
        })
        .then(text => {
          // Guard against empty response body (e.g. 200 with no content).
          if (!text.trim()) throw new Error('empty response body');

          // Pretty-print if it parses as JSON, otherwise pass through as-is.
          let content = text;
          try { content = JSON.stringify(JSON.parse(text), null, 2); } catch { /* not JSON — pass raw */ }

          // Append the fetched payload to this playground's console.
          // ConsoleMessage already handles JSON via JsonView and will show
          // the ➡️ send-to-JSON-Path button identical to small-payload flow.
          appendMessage(content);

          // Belt-and-suspenders watermark advance: use +1 as a safe upper bound
          // so the appended message (id = lastNotificationId + 1 or higher) is
          // treated as "already seen" on the next tick, even before React
          // re-renders and the messages closure is refreshed.
          watermarkRef.current = newMessages[newMessages.length - 1].id + 1;
          isFetchingRef.current = false;
          abortRef.current = null;
        })
        .catch((err: Error) => {
          if (err.name === 'AbortError') return;
          // Clear guard before appending: the error string cannot match
          // extractLargePayloadLink so re-entrancy risk is zero.
          isFetchingRef.current = false;
          abortRef.current = null;
          appendMessage(`ERROR: payload fetch failed — ${err.message}`);
          addToast(`Payload fetch failed: ${err.message}`, 'error');
        });

      // Only process the first large-payload link per batch.
      // Without the break, two concurrent fetches could race and both attempt
      // to write abortRef.current, leaving the second one unabortable.
      break;
    }
  }, [messages, connected, appendMessage, addToast]);
}
