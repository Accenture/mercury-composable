import { useCallback, useState } from 'react';
import { type ToastType } from './useToast';
import { readFileAsText, validateJsonFileType } from '../utils/jsonFile';
import { validateGraphModel, type ValidGraphFile } from '../utils/graphFile';
import { responseErrorMessage } from '../utils/httpResponse';

export interface PendingGraphImport extends ValidGraphFile {
  fileName: string;
}

export interface UseGraphFileImportOptions {
  /** The session the file is imported into, or null until the session id is known. */
  sessionId: string | null;
  connected: boolean;
  /** True while a graph is rendered: an import then replaces it, so the user is asked first. */
  hasGraph: boolean;
  addToast: (message: string, type?: ToastType) => void;
}

export interface UseGraphFileImportReturn {
  /** True when a file can be imported right now (connected, session id known). */
  canImport: boolean;
  /** Validate dropped or picked files and import the one graph, or ask before replacing a loaded graph. */
  importFiles: (files: FileList | File[]) => Promise<void>;
  /** Open the browser's file picker for one `.json` file. */
  openFilePicker: () => void;
  /** A validated file waiting for the user's confirmation, or null. */
  pending: PendingGraphImport | null;
  confirmPending: () => void;
  cancelPending: () => void;
  isImporting: boolean;
}

export function graphImportPath(sessionId: string): string {
  return `/api/graph/import/${sessionId}`;
}

/**
 * Imports a graph model from a file on the user's computer into the session's draft - the
 * Graph view's "Import Graph" button and a `.json` file dropped on the canvas both end here.
 *
 * The file is validated locally (`validateGraphModel`), then posted to
 * `POST /api/graph/import/{sessionId}`. The engine validates again, replaces the draft and
 * tells every member's console "Graph model imported as draft" - the line that refreshes the
 * view (`useAutoGraphRefresh`), so there is no success toast here. Importing over a loaded
 * graph replaces it and cannot be undone, so the file is parked in `pending` until
 * `confirmPending`.
 */
export function useGraphFileImport({
  sessionId,
  connected,
  hasGraph,
  addToast,
}: UseGraphFileImportOptions): UseGraphFileImportReturn {
  const [pending, setPending] = useState<PendingGraphImport | null>(null);
  const [isImporting, setIsImporting] = useState(false);
  const canImport = connected && sessionId !== null;

  const post = useCallback(async (file: PendingGraphImport) => {
    if (sessionId === null) {
      addToast('Could not import because the session id is not known yet.', 'error');
      return;
    }
    setIsImporting(true);
    try {
      const response = await fetch(graphImportPath(sessionId), {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(file.model),
      });
      if (!response.ok) {
        addToast(`Import of "${file.fileName}" refused - ${await responseErrorMessage(response)}`, 'error');
      }
    } catch (err) {
      addToast(`Import of "${file.fileName}" failed: ${(err as Error).message}`, 'error');
    } finally {
      setIsImporting(false);
    }
  }, [sessionId, addToast]);

  const importFiles = useCallback(async (files: FileList | File[]) => {
    const list = Array.from(files);
    if (list.length === 0) return;
    if (!connected || sessionId === null) {
      addToast('Connect first to import a graph.', 'error');
      return;
    }
    if (list.length > 1) {
      addToast('Drop one graph file at a time.', 'error');
      return;
    }
    const file = list[0];
    const typeError = validateJsonFileType(file);
    if (typeError) {
      addToast(typeError, 'error');
      return;
    }
    let parsed: unknown;
    try {
      parsed = JSON.parse(await readFileAsText(file));
    } catch (err) {
      addToast(`"${file.name}" is not valid JSON: ${(err as Error).message}`, 'error');
      return;
    }
    const result = validateGraphModel(parsed);
    if (!result.ok) {
      addToast(`"${file.name}" is not a graph model: ${result.error}`, 'error');
      return;
    }
    const candidate: PendingGraphImport = { ...result, fileName: file.name };
    if (hasGraph) {
      setPending(candidate);
      return;
    }
    await post(candidate);
  }, [connected, sessionId, hasGraph, addToast, post]);

  const confirmPending = useCallback(() => {
    if (!pending) return;
    const file = pending;
    setPending(null);
    void post(file);
  }, [pending, post]);

  const cancelPending = useCallback(() => setPending(null), []);

  const openFilePicker = useCallback(() => {
    if (!connected || sessionId === null) {
      addToast('Connect first to import a graph.', 'error');
      return;
    }
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json,application/json';
    input.style.display = 'none';
    const cleanup = () => input.remove();
    input.addEventListener('change', () => {
      const files = input.files;
      cleanup();
      if (files) void importFiles(files);
    }, { once: true });
    input.addEventListener('cancel', cleanup, { once: true });
    document.body.appendChild(input);
    input.click();
  }, [connected, sessionId, addToast, importFiles]);

  return { canImport, importFiles, openFilePicker, pending, confirmPending, cancelPending, isImporting };
}
