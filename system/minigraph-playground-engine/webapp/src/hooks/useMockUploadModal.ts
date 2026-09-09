import { useState, useRef, useCallback } from 'react';
import { type ProtocolBus } from '../protocol/bus';
import { type ToastType } from './useToast';
import { useAutoMockUpload } from './useAutoMockUpload';

export interface UseMockUploadModalOptions {
  bus:      ProtocolBus;
  addToast: (message: string, type?: ToastType) => void;
}

export interface UseMockUploadModalReturn {
  /** null = modal closed; non-null = modal open for that endpoint path. */
  modalUploadPath:        string | null;
  /** Set of POST paths that have been successfully uploaded (drives badge). */
  successfulUploadPaths:  Set<string>;
  /** Open the upload modal for a specific endpoint path. */
  handleOpenUploadModal:  (path: string) => void;
  /** Close the modal and restore focus. */
  handleCloseUploadModal: () => void;
  /** Close only when the currently-open modal owns this exact upload path. */
  handleCloseUploadPath:  (path: string) => boolean;
  /** Mark a path as successfully uploaded, close modal, restore focus, toast. */
  handleUploadSuccess:    (responseBody: string) => void;
  /** Show an error toast (modal stays open). */
  handleUploadError:      (errorMessage: string) => void;
  /** Reset successful paths (called by handleClearMessages). */
  resetSuccessfulPaths:   () => void;
}

/**
 * Manages the mock-upload modal lifecycle: open/close state, focus
 * restoration, success/error handling, and the auto-open subscription
 * via the ProtocolBus.
 *
 * Composes `useAutoMockUpload` internally — callers do not need to
 * invoke that hook separately.
 */
export function useMockUploadModal({
  bus,
  addToast,
}: UseMockUploadModalOptions): UseMockUploadModalReturn {
  // Path extracted from the server's upload invitation.
  // null = modal closed; non-null = modal open for that specific endpoint.
  const [modalUploadPath, setModalUploadPath] = useState<string | null>(null);
  const modalUploadPathRef = useRef<string | null>(null);
  const pendingUploadPathsRef = useRef<string[]>([]);

  // Capture the element that triggered the modal so focus can be restored on close.
  const modalTriggerRef = useRef<HTMLElement | null>(null);

  // POST paths of invitations that have been successfully fulfilled — drives badge.
  const [successfulUploadPaths, setSuccessfulUploadPaths] = useState<Set<string>>(new Set());

  const handleOpenUploadModal = useCallback((path: string) => {
    const currentPath = modalUploadPathRef.current;
    if (currentPath !== null) {
      if (currentPath !== path && !pendingUploadPathsRef.current.includes(path)) {
        pendingUploadPathsRef.current.push(path);
      }
      return;
    }
    // Capture the focused element before opening so we can restore focus on close.
    modalTriggerRef.current = document.activeElement as HTMLElement;
    modalUploadPathRef.current = path;
    setModalUploadPath(path);
  }, []);

  const advanceModalQueue = useCallback(() => {
    const nextPath = pendingUploadPathsRef.current.shift() ?? null;
    modalUploadPathRef.current = nextPath;
    setModalUploadPath(nextPath);
    if (nextPath === null) {
      // Restore focus only after the whole invitation queue has drained.
      setTimeout(() => modalTriggerRef.current?.focus(), 0);
    }
  }, []);

  const handleCloseUploadModal = useCallback(() => {
    advanceModalQueue();
  }, [advanceModalQueue]);

  const handleCloseUploadPath = useCallback((path: string): boolean => {
    if (modalUploadPathRef.current === path) {
      advanceModalQueue();
      return true;
    }
    const queuedIndex = pendingUploadPathsRef.current.indexOf(path);
    if (queuedIndex === -1) return false;
    pendingUploadPathsRef.current.splice(queuedIndex, 1);
    return true;
  }, [advanceModalQueue]);

  const handleUploadSuccess = useCallback((_responseBody: string) => {
    // _responseBody is available but intentionally not surfaced per spec §2.
    const uploadPath = modalUploadPathRef.current;
    if (uploadPath) {
      setSuccessfulUploadPaths(prev => new Set([...prev, uploadPath]));
    }
    advanceModalQueue();
    addToast('Mock data uploaded successfully ✓', 'success');
  }, [addToast, advanceModalQueue]);

  const handleUploadError = useCallback((errorMessage: string) => {
    // Modal stays open — error is displayed inline inside the modal.
    addToast(`Upload failed: ${errorMessage}`, 'error');
  }, [addToast]);

  const resetSuccessfulPaths = useCallback(() => {
    setSuccessfulUploadPaths(new Set());
  }, []);

  // Auto-open modal when server sends upload invitation.
  useAutoMockUpload({
    bus,
    onOpenModal: handleOpenUploadModal,
  });

  return {
    modalUploadPath,
    successfulUploadPaths,
    handleOpenUploadModal,
    handleCloseUploadModal,
    handleCloseUploadPath,
    handleUploadSuccess,
    handleUploadError,
    resetSuccessfulPaths,
  };
}
