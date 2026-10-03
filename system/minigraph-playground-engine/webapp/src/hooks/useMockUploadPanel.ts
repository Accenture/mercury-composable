import { useState, useRef, useCallback } from 'react';
import { type ToastType } from './useToast';

export interface UseMockUploadPanelOptions {
  addToast: (message: string, type?: ToastType) => void;
}

export interface UseMockUploadPanelReturn {
  /** null = panel closed; non-null = panel open for that endpoint path. */
  uploadPanelPath:        string | null;
  /** Set of POST paths that have been successfully uploaded (drives badge). */
  successfulUploadPaths:  Set<string>;
  /** Open the upload panel for a specific endpoint path. */
  handleOpenUploadPanel:  (path: string) => void;
  /** Close the panel and restore focus. */
  handleCloseUploadPanel: () => void;
  /** Close only when the currently-open panel owns this exact upload path. */
  handleCloseUploadPath:  (path: string) => boolean;
  /** Mark a path as successfully uploaded, close panel, restore focus, toast. */
  handleUploadSuccess:    (responseBody: string) => void;
  /** Show an error toast (panel stays open). */
  handleUploadError:      (errorMessage: string) => void;
  /** Reset successful paths (called by handleClearMessages). */
  resetSuccessfulPaths:   () => void;
}

/**
 * Manages the mock-upload panel lifecycle: open/close state, focus
 * restoration and success/error handling. The panel opens only on an
 * explicit local action — the toolbar's Upload button or the re-open button
 * on a console invitation row — never on an `upload.invitation` line by
 * itself: in a collaborative session every member's console receives the
 * replayed invitation, and only the member who asked should see the form.
 */
export function useMockUploadPanel({
  addToast,
}: UseMockUploadPanelOptions): UseMockUploadPanelReturn {
  // Path extracted from the server's upload invitation.
  // null = panel closed; non-null = panel open for that specific endpoint.
  const [uploadPanelPath, setUploadPanelPath] = useState<string | null>(null);
  const uploadPanelPathRef = useRef<string | null>(null);
  const pendingUploadPathsRef = useRef<string[]>([]);

  // Capture the element that triggered the panel so focus can be restored on close.
  const panelTriggerRef = useRef<HTMLElement | null>(null);

  // POST paths of invitations that have been successfully fulfilled — drives badge.
  const [successfulUploadPaths, setSuccessfulUploadPaths] = useState<Set<string>>(new Set());

  const handleOpenUploadPanel = useCallback((path: string) => {
    const currentPath = uploadPanelPathRef.current;
    if (currentPath !== null) {
      if (currentPath !== path && !pendingUploadPathsRef.current.includes(path)) {
        pendingUploadPathsRef.current.push(path);
      }
      return;
    }
    // Capture the focused element before opening so we can restore focus on close.
    panelTriggerRef.current = document.activeElement as HTMLElement;
    uploadPanelPathRef.current = path;
    setUploadPanelPath(path);
  }, []);

  const advanceUploadQueue = useCallback(() => {
    const nextPath = pendingUploadPathsRef.current.shift() ?? null;
    uploadPanelPathRef.current = nextPath;
    setUploadPanelPath(nextPath);
    if (nextPath === null) {
      // Restore focus only after the whole invitation queue has drained.
      setTimeout(() => panelTriggerRef.current?.focus(), 0);
    }
  }, []);

  const handleCloseUploadPanel = useCallback(() => {
    advanceUploadQueue();
  }, [advanceUploadQueue]);

  const handleCloseUploadPath = useCallback((path: string): boolean => {
    if (uploadPanelPathRef.current === path) {
      advanceUploadQueue();
      return true;
    }
    const queuedIndex = pendingUploadPathsRef.current.indexOf(path);
    if (queuedIndex === -1) return false;
    pendingUploadPathsRef.current.splice(queuedIndex, 1);
    return true;
  }, [advanceUploadQueue]);

  const handleUploadSuccess = useCallback((_responseBody: string) => {
    // _responseBody is available but intentionally not surfaced per spec §2.
    const uploadPath = uploadPanelPathRef.current;
    if (uploadPath) {
      setSuccessfulUploadPaths(prev => new Set([...prev, uploadPath]));
    }
    advanceUploadQueue();
    addToast('Mock data uploaded successfully ✓', 'success');
  }, [addToast, advanceUploadQueue]);

  const handleUploadError = useCallback((errorMessage: string) => {
    // Panel stays open — error is displayed inline inside the panel.
    addToast(`Upload failed: ${errorMessage}`, 'error');
  }, [addToast]);

  const resetSuccessfulPaths = useCallback(() => {
    setSuccessfulUploadPaths(new Set());
  }, []);

  return {
    uploadPanelPath,
    successfulUploadPaths,
    handleOpenUploadPanel,
    handleCloseUploadPanel,
    handleCloseUploadPath,
    handleUploadSuccess,
    handleUploadError,
    resetSuccessfulPaths,
  };
}
