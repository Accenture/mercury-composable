import { useEffect, useRef } from 'react';
import { extractMockUploadPath, isMockUploadMessage } from '../utils/messageParser';

export interface UseAutoMockUploadOptions {
  /**
   * Full incoming message log from useWebSocket.
   * The hook inspects only messages with IDs greater than the watermark
   * recorded at mount time — older history is never reprocessed.
   */
  messages: { id: number; raw: string }[];

  /**
   * Whether the WebSocket is currently connected.
   * Accepted for symmetry with the other automation hooks and future
   * extensibility (e.g. disabling the re-open button when disconnected).
   * The effect body does not read or depend on it — no flag is reset on
   * disconnect because there is no pending-wait state to clear.
   *
   * Resetting the watermark to -1 on disconnect would cause old invitation
   * messages still in the message store to be replayed as "new" on reconnect,
   * auto-opening the modal for a stale endpoint. The watermark stays at its
   * current value; the server sends a fresh invitation for each new session.
   */
  connected: boolean;

  /**
   * Called with the extracted upload path when a mock-upload invitation is
   * detected in the message stream. This is the only side-effect the hook
   * produces — Playground owns all modal state.
   */
  onOpenModal: (uploadPath: string) => void;
}

/**
 * Watches the WebSocket message stream for mock-data upload invitations
 * ("You may upload JSON payload -> POST /api/mock/{id}") and automatically
 * calls `onOpenModal` with the extracted POST path when one is detected.
 *
 * Design decisions:
 *  - No `waitingForRef` flag: unlike useAutoGraphRefresh, there is no two-step
 *    server handshake. The invitation message itself contains the complete
 *    target URL — we open the modal immediately.
 *  - Watermark NOT reset on disconnect: see `connected` JSDoc above.
 *  - Break on first match per batch: prevents two modals from stacking if the
 *    server sends two invitations in one message batch (consistent with
 *    useLargePayloadDownload).
 *  - `connected` is aliased to `_connected` to signal intentional non-use
 *    while keeping the interface symmetrical with peer hooks.
 *
 * This hook has no return value — its only job is to dispatch side effects.
 */
export function useAutoMockUpload({
  messages,
  connected: _connected,
  onOpenModal,
}: UseAutoMockUploadOptions): void {

  // ── Message-ID watermark ──────────────────────────────────────────────────
  // Initialised to the highest message ID present at mount time.
  // Prevents replaying historical upload invitations on mount.
  const watermarkRef = useRef<number>(-1);

  // ── Set watermark at mount ────────────────────────────────────────────────
  // Runs once. Captures the highest message ID currently in the log so
  // only genuinely new messages are processed.
  useEffect(() => {
    if (messages.length > 0) {
      watermarkRef.current = messages[messages.length - 1].id;
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Main effect ───────────────────────────────────────────────────────────
  useEffect(() => {
    if (messages.length === 0) return;

    const newMessages = messages.filter(m => m.id > watermarkRef.current);
    if (newMessages.length === 0) return;

    // Advance watermark to the latest processed ID.
    watermarkRef.current = messages[messages.length - 1].id;

    for (const msg of newMessages) {
      if (isMockUploadMessage(msg.raw)) {
        const uploadPath = extractMockUploadPath(msg.raw);
        if (uploadPath) {
          onOpenModal(uploadPath);
          // Break on first match — prevents two modals from stacking if
          // the server sends two invitations in one message batch.
          break;
        }
      }
    }
  }, [messages, onOpenModal]);
}
