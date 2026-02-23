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
