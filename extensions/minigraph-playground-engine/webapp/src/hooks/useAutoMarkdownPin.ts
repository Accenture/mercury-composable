import { useEffect, useRef } from 'react';
import { isHelpOrDescribeCommand, isMarkdownCandidate, isGraphLinkMessage } from '../utils/messageParser';

export interface UseAutoMarkdownPinOptions {
  /**
   * Full incoming message log from useWebSocket.
   * The hook inspects only messages with IDs greater than the watermark
   * recorded at mount time — older history is never reprocessed.
   */
  messages: { id: number; raw: string }[];

  /**
   * Whether the WebSocket is currently connected.
   * The pending-wait flag is cleared on disconnect so a stale wait
   * from one session cannot accidentally auto-pin in the next.
   */
  connected: boolean;

  /**
   * Setter to pin a message by its stable numeric ID in Playground.tsx.
   * The ID (not the raw string) is stored so two messages with identical
   * text don't collapse onto a single pinned row.
   */
  setPinnedMessageId: (id: number) => void;

  /**
   * Optional: called after auto-pin fires so the parent can switch the
   * right panel to the Markdown Preview tab.
   */
  onAutoPin?: () => void;
}

/**
 * Watches the WebSocket message stream for `help` or text-producing
 * `describe` command echoes, then auto-pins the first plain-text
 * (markdown-candidate, non-graph-link) response that follows.
 *
 * Behaviour:
 *
 *  1. The backend echoes every sent command as `> <command>` before
 *     sending the response.  When the hook sees an echo that matches
 *     `isHelpOrDescribeCommand`, it sets `waitingForResponseRef = true`.
 *
 *  2. On the next batch of new messages, if `waitingForResponseRef` is
 *     true and a markdown-candidate (non-graph-link) message is found,
 *     it is auto-pinned via `setPinnedMessageId` and `onAutoPin` is called.
 *
 *  3. The flag is cleared after consuming the response so subsequent
 *     unrelated messages do not trigger spurious pins.
 *
 * Relationship to useAutoGraphRefresh:
 *  - That hook sends a silent `describe graph` command (no echo) which
 *    produces a graph-link message.  Graph-link messages are excluded by
 *    `isMarkdownCandidate`, so the two hooks' response-consumption paths
 *    are completely disjoint and cannot interfere.
 *  - After `import graph`, useAutoGraphRefresh silently sends `describe
 *    graph`.  The only echo present in the stream is `> import graph from
 *    {name}` (which does NOT match `isHelpOrDescribeCommand`), so this
 *    hook will NOT set `waitingForResponseRef = true` for that flow.
 *    The subsequent graph-link response is therefore never stolen by this
 *    hook.
 *
 * This hook has no return value — its only job is to dispatch side effects.
 */
export function useAutoMarkdownPin({
  messages,
  connected,
  setPinnedMessageId,
  onAutoPin,
}: UseAutoMarkdownPinOptions): void {

  // ── Message-ID watermark ──────────────────────────────────────────────────
  // Initialised to the highest message ID present at mount time.
  // This prevents replaying historical help/describe echoes on mount.
  const watermarkRef = useRef<number>(-1);

  // ── waitingForResponseRef ─────────────────────────────────────────────────
  // When true, the hook is looking for the first markdown-candidate message
  // that follows a help/describe command echo.
  const waitingForResponseRef = useRef(false);

  // ── Reset pending flag on disconnect ─────────────────────────────────────
  // If the WebSocket drops while we are waiting for a response, clear the
  // flag so the first message after reconnection is not accidentally pinned.
  useEffect(() => {
    if (!connected) {
      waitingForResponseRef.current = false;
    }
  }, [connected]);

  // ── Set watermark at mount ────────────────────────────────────────────────
  // Runs once.  Captures the highest message ID currently in the log so
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
      if (waitingForResponseRef.current) {
        // Consume the first markdown-candidate (non-graph-link) response.
        // Graph-link messages belong to the Graph tab and must be skipped.
        // JSON messages (e.g. `describe node` replies) are not markdown
        // candidates, so they are also skipped — the flag stays true and
        // the hook will consume the first text message that follows.
        if (!isGraphLinkMessage(msg.raw) && isMarkdownCandidate(msg.raw)) {
          waitingForResponseRef.current = false;
          setPinnedMessageId(msg.id);
          onAutoPin?.();
          return; // done for this batch
        }
      } else if (isHelpOrDescribeCommand(msg.raw)) {
        // A help/describe echo arrived — arm the flag so we catch the response.
        waitingForResponseRef.current = true;
      }
    }
  }, [messages, setPinnedMessageId, onAutoPin]);
}
