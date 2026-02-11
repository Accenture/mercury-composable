export const validateJSON = (text) => {
  if (!text.trim()) {
    return { valid: true, error: null };
  }
  try {
    JSON.parse(text);
    return { valid: true, error: null };
  } catch (e) {
    return { valid: false, error: e.message };
  }
};

export const formatJSON = (text) => {
  try {
    const parsed = JSON.parse(text);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return text;
  }
};
