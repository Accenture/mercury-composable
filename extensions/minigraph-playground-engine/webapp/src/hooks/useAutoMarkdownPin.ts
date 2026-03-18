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
 * Returns true when a raw message is a server response that is a valid
 * auto-pin target: a markdown candidate that is neither a graph-link nor
 * an echoed user command (echoes start with `> `).
 *
 * The echo guard is critical: `> help` passes `isMarkdownCandidate` because
 * it is plain text. Without this check a stranded `waitingForResponseRef`
 * would pin the echo instead of the actual help content.
 */
function isPinnableResponse(raw: string): boolean {
  if (raw.startsWith('> ')) return false;       // echoed command — never pin
  if (isGraphLinkMessage(raw)) return false;    // graph-link → Graph tab, not preview
  return isMarkdownCandidate(raw);              // plain-text response → pin candidate
}

/**
 * Watches the WebSocket message stream for `help` or text-producing
 * `describe` command echoes, then auto-pins the first plain-text
 * (markdown-candidate, non-graph-link, non-echo) response that follows.
 *
 * Behaviour:
 *
 *  1. The backend echoes every sent command as `> <command>` before
 *     sending the response.  When the hook sees an echo that matches
 *     `isHelpOrDescribeCommand`, it sets `waitingForResponseRef = true`.
 *
 *  2. On the next batch of new messages, if `waitingForResponseRef` is true
 *     and a pinnable response is found (plain text, not an echo, not a
 *     graph-link), it is auto-pinned and `onAutoPin` is called.
 *
 *  3. If the expected response turns out to be JSON (e.g. `describe node`
 *     returns a JSON map), the flag is cleared without pinning so it cannot
 *     accumulate across commands and cause a later spurious pin.
 *
 *  4. The flag is always cleared on disconnect to prevent cross-session
 *     contamination.
 *
 * Relationship to useAutoGraphRefresh:
 *  - That hook sends a silent `describe graph` command (no echo) which
 *    produces a graph-link message.  `isPinnableResponse` excludes graph-link
 *    messages, so the two hooks' response-consumption paths are disjoint.
 *  - After `import graph`, useAutoGraphRefresh silently sends `describe
 *    graph`.  The only echo in the stream is `> import graph from {name}`
 *    which does NOT match `isHelpOrDescribeCommand`, so this hook never arms
 *    its flag for that flow and the graph-link response is never stolen.
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
        // Skip echoed commands — they are plain text and would pass
        // isPinnableResponse if not for the explicit echo guard inside it.
        // This is the primary defence against pinning an echo instead of
        // its response (e.g. if the flag was stranded from a prior command).
        if (msg.raw.startsWith('> ')) continue;

        // A new help/describe echo re-arms the flag for the new command
        // instead of waiting for a response to the old one.  This keeps
        // things correct if the user sends two help commands back-to-back
        // quickly enough to land in the same batch.
        if (isHelpOrDescribeCommand(msg.raw)) {
          // flag stays true — re-armed for the new command
          continue;
        }

        if (isPinnableResponse(msg.raw)) {
          // Plain-text response — pin it.
          waitingForResponseRef.current = false;
          setPinnedMessageId(msg.id);
          onAutoPin?.();
          return; // done for this batch
        } else {
          // A non-pinnable server response arrived (e.g. a JSON map from
          // `describe node`, or a graph-link from an unrelated command).
          // Clear the flag so it cannot accumulate across unrelated commands
          // and cause a future spurious pin.
          waitingForResponseRef.current = false;
        }
      } else if (isHelpOrDescribeCommand(msg.raw)) {
        // A help/describe echo arrived — arm the flag so we catch the response.
        waitingForResponseRef.current = true;
      }
    }
  }, [messages, setPinnedMessageId, onAutoPin]);
}
