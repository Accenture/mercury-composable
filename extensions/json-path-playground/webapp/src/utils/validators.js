// Helper to check if text is valid XML
const isValidXML = (text) => {
  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(text.trim(), 'text/xml');
    // Check for parse errors
    const parserError = doc.querySelector('parsererror');
    return !parserError;
  } catch {
    return false;
  }
};

// Helper to check if text is valid JSON
const isValidJSON = (text) => {
  try {
    JSON.parse(text);
    return true;
  } catch {
    return false;
  }
};

export const validateJSON = (text) => {
  if (!text.trim()) {
    return { valid: true, error: null, type: null };
  }
  
  // Try JSON first
  if (isValidJSON(text)) {
    return { valid: true, error: null, type: 'json' };
  }
  
  // Try XML
  if (isValidXML(text)) {
    return { valid: true, error: null, type: 'xml' };
  }
  
  // Neither JSON nor XML
  return { 
    valid: false, 
    error: 'Invalid JSON/XML format',
    type: null
  };
};

export const formatJSON = (text) => {
  try {
    const parsed = JSON.parse(text);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return text;
  }
};
