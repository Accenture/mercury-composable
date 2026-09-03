import { useCallback, useEffect, useRef, useState } from 'react';

const DISPLAY_MS = 3000;
const FADE_MS = 400;

/** Owns the one-shot minimap hint above the keyed graph-canvas boundary. */
export function useMinimapHint(eligible: boolean) {
  const [hintVisible, setHintVisible] = useState(false);
  const [hintFading, setHintFading] = useState(false);
  const [hintFocused, setHintFocused] = useState(false);
  const shownRef = useRef(false);
  const remainingMsRef = useRef(DISPLAY_MS);
  const fadeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const dismissHint = useCallback(() => {
    if (!hintVisible || hintFading) return;
    setHintFading(true);
    if (fadeTimerRef.current !== null) clearTimeout(fadeTimerRef.current);
    fadeTimerRef.current = setTimeout(() => {
      setHintVisible(false);
      fadeTimerRef.current = null;
    }, FADE_MS);
  }, [hintFading, hintVisible]);

  const dismissHintImmediately = useCallback(() => {
    shownRef.current = true;
    remainingMsRef.current = 0;
    if (fadeTimerRef.current !== null) {
      clearTimeout(fadeTimerRef.current);
      fadeTimerRef.current = null;
    }
    setHintFading(false);
    setHintFocused(false);
    setHintVisible(false);
  }, []);

  useEffect(() => {
    if (!eligible || shownRef.current) return;
    shownRef.current = true;
    remainingMsRef.current = DISPLAY_MS;
    setHintFading(false);
    setHintVisible(true);
  }, [eligible]);

  useEffect(() => {
    if (!eligible || !hintVisible || hintFading || hintFocused) return;

    const remainingAtStart = remainingMsRef.current;
    const startedAt = Date.now();
    const timerId = setTimeout(dismissHint, remainingAtStart);

    return () => {
      clearTimeout(timerId);
      const elapsed = Math.max(0, Date.now() - startedAt);
      const nextRemaining = Math.max(0, remainingAtStart - elapsed);
      // An immediate dismissal may already have set this to zero.
      remainingMsRef.current = Math.min(remainingMsRef.current, nextRemaining);
    };
  }, [dismissHint, eligible, hintFading, hintFocused, hintVisible]);

  useEffect(() => () => {
    if (fadeTimerRef.current !== null) clearTimeout(fadeTimerRef.current);
  }, []);

  return {
    hintVisible,
    hintFading,
    dismissHint,
    dismissHintImmediately,
    setHintFocused,
  };
}
