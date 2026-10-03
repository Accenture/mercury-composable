/** Read a File as UTF-8 text, resolving with the string or rejecting with an Error. */
export function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload  = () => resolve(reader.result as string);
    reader.onerror = () => reject(new Error(`Could not read file "${file.name}"`));
    reader.readAsText(file, 'utf-8');
  });
}

/**
 * A dropped or picked file is acceptable when it has a `.json` extension OR a JSON
 * (or plain-text) MIME type. Returns null on success, or a human-readable error.
 */
export function validateJsonFileType(file: File): string | null {
  const hasJsonExt  = file.name.toLowerCase().endsWith('.json');
  const hasJsonMime = file.type === 'application/json' || file.type === 'text/plain';
  if (!hasJsonExt && !hasJsonMime) {
    return `"${file.name}" does not appear to be a JSON file. Only .json files are accepted.`;
  }
  return null;
}
