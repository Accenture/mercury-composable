/** Discriminated string union of all known message type values. */
export type MessageType = 'info' | 'error' | 'ping' | 'welcome' | 'raw';

/** Parsed structure of a single console message. */
export interface ParsedMessage {
  type:    MessageType;
  message: string;
  time:    string | null;
  raw:     string;
}

/** Result of attempting to parse a string as a JSON object/array. */
export interface JSONParseResult {
  isJSON: boolean;
  data:   object | null;
}

export const parseMessage = (msg: string): ParsedMessage => {
  try {
    const parsed = JSON.parse(msg);
    return {
      type: parsed.type || 'info',
      message: parsed.message || msg,
      time: parsed.time,
      raw: msg
    };
  } catch {
    return {
      type: 'raw',
      message: msg,
      time: null,
      raw: msg
    };
  }
};

export const getMessageIcon = (type: MessageType): string => {
  const icons: Record<MessageType, string> = {
    info:    'ℹ️',
    error:   '❌',
    ping:    '🔄',
    welcome: '👋',
    raw:     '📝',
  };
  return icons[type] ?? '•';
};

export const tryParseJSON = (str: string): JSONParseResult => {
  try {
    const parsed = JSON.parse(str);
    // Only consider it JSON if it's an object or array, not primitives
    if (typeof parsed === 'object' && parsed !== null) {
      return { isJSON: true, data: parsed };
    }
  } catch {
    // Not valid JSON
  }
  return { isJSON: false, data: null };
};

/**
 * Returns true when a raw WebSocket message string is a plain-text
 * (non-JSON) candidate for Markdown rendering.
 *
 * A message is NOT a Markdown candidate if:
 *  - it is a valid JSON object or array (handled by JsonView)
 *  - it is a JSON-encoded lifecycle event ({ type, message, time })
 *    i.e. tryParseJSON succeeds AND the parsed object has a "type" field
 *
 * Everything else — including multi-line text, Markdown syntax, XML snippets
 * that are not valid JSON — is considered a Markdown candidate.
 *
 * Edge case — bare JSON primitives (e.g. "hello", 42, true):
 *   tryParseJSON returns isJSON: false for primitives, so they fall through
 *   to `return true` and are treated as Markdown candidates. This is accepted
 *   as correct behaviour: the backend never sends bare primitives, and
 *   rendering the raw string as Markdown is a safe fallback if it ever does.
 *   Do NOT change tryParseJSON to accept primitives — that would break JsonView.
 */
export function isMarkdownCandidate(raw: string): boolean {
  const result = tryParseJSON(raw);
  if (!result.isJSON) return true;                             // not JSON at all → candidate
  const obj = result.data as Record<string, unknown>;
  if (typeof obj['type'] === 'string') return false;           // lifecycle event ({ type, message, time }) → not candidate
  return false;                                                // any other JSON object/array → not candidate
}

/**
 * Extracts the first `/api/graph/model/<id>` path found in a raw message
 * string. Returns `null` if no such path is present.
 *
 * Matches both forms the server emits:
 *   "Graph exported to '/tmp/…'\nDescribed in /api/graph/model/my-graph"
 *   "Graph described in /api/graph/model/ws-122189-1"
 */
export function extractGraphApiPath(raw: string): string | null {
  const match = raw.match(/\/api\/graph\/model\/([^\s'"]+)/);
  return match ? match[0] : null;
}

/**
 * Returns true when a raw message contains a graph model API path that the
 * user can pin to load the graph visualiser.
 */
export function isGraphLinkMessage(raw: string): boolean {
  // Must be a plain-text message (not a JSON lifecycle event)
  if (!isMarkdownCandidate(raw)) return false;
  return extractGraphApiPath(raw) !== null;
}

/**
 * Extracts the `/api/json/content/{id}` path from the server's upload-ready
 * message: "Please upload XML/JSON text to /api/json/content/{id}"
 *
 * Returns the path string (e.g. "/api/json/content/{id}") or null
 * if the message does not contain such a path.
 */
export function extractUploadPath(raw: string): string | null {
  const match = raw.match(/\/api\/json\/content\/([\w-]+)/);
  return match ? match[0] : null;
}

