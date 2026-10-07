/**
 * The message of a failed response: the engine answers `{ "message": "..." }` for a refused
 * request, so that is read first; otherwise the body text, or the status when the body is empty.
 */
export async function responseErrorMessage(response: Response): Promise<string> {
  const text = await response.text();
  try {
    const parsed = JSON.parse(text) as { message?: unknown };
    if (typeof parsed.message === 'string') return parsed.message;
  } catch {
    // plain text body
  }
  return text || `HTTP ${response.status}`;
}
