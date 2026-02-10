export const parseMessage = (msg) => {
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

export const getMessageIcon = (type) => {
  const icons = {
    info: 'ℹ️',
    error: '❌',
    ping: '🔄',
    welcome: '👋',
    raw: '📝'
  };
  return icons[type] || '•';
};

export const tryParseJSON = (str) => {
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
