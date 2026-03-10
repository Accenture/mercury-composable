import { useState, useCallback } from 'react';

/** How long (ms) the button's in-row "✓" confirmation is shown before resetting. */
const COPIED_RESET_DELAY_MS = 2000;

export interface UseCopyToClipboardOptions {
  /**
   * Called after a successful clipboard write.
   * Intended for side-effects such as showing a toast notification.
   */
  onSuccess?: () => void;
  /**
   * Called when the clipboard write fails (API unavailable or permission denied).
   * Intended for side-effects such as showing an error toast.
   */
  onError?: () => void;
}

export interface UseCopyToClipboardReturn {
  /**
   * Copy the given text to the clipboard.
   * Returns `true` on success, `false` on failure.
   */
  copy:   (text: string) => Promise<boolean>;
  /**
   * `true` for {@link COPIED_RESET_DELAY_MS} ms after a successful copy.
   * Use this to update the button's visual state (e.g. swap icon, change colour).
   */
  copied: boolean;
}

/**
 * Encapsulates clipboard write logic with an optional success/error callback
 * pair so callers can hook in any notification system (e.g. toast) without
 * this hook needing to know about it.
 *
 * The `copied` flag provides a short-lived in-row confirmation independent of
 * the notification callback — both can coexist.
 *
 * @example
 * const { copy, copied } = useCopyToClipboard({
 *   onSuccess: () => addToast('Copied to clipboard', 'success'),
 *   onError:   () => addToast('Failed to copy', 'error'),
 * });
 * <button onClick={() => copy(text)}>{copied ? '✓' : '⎘'}</button>
 */
export const useCopyToClipboard = (
  options: UseCopyToClipboardOptions = {},
): UseCopyToClipboardReturn => {
  const { onSuccess, onError } = options;
  const [copied, setCopied] = useState(false);

  const copy = useCallback(async (text: string): Promise<boolean> => {
    if (!navigator.clipboard) {
      console.warn('useCopyToClipboard: Clipboard API not available in this browser.');
      onError?.();
      return false;
    }

    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      // Reset the in-row visual confirmation after the delay.
      // The state update is a no-op if the component has already unmounted
      // (React 18+ behaviour), so no explicit cleanup is needed.
      setTimeout(() => setCopied(false), COPIED_RESET_DELAY_MS);
      onSuccess?.();
      return true;
    } catch (err) {
      console.error('useCopyToClipboard: Failed to write to clipboard.', err);
      onError?.();
      return false;
    }
  }, [onSuccess, onError]);

  return { copy, copied };
};
