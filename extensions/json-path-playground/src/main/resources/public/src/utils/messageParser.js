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
