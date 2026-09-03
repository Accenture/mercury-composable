import { useCallback, useEffect, useId, useRef } from 'react';
import { ControlButton, Controls, MiniMap, type Node } from '@xyflow/react';
import styles from './GraphMinimap.module.css';

interface GraphMinimapProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  hotkeyEnabled: boolean;
  hintVisible: boolean;
  hintFading: boolean;
  onDismissHint: () => void;
  onHintFocusChange: (focused: boolean) => void;
}

const NODE_COLORS: Record<string, string> = {
  Root:       '#15803d',
  End:        '#dc2626',
  Fetcher:    '#2563eb',
  mapper:     '#ea580c',
  Math:       '#a16207',
  JavaScript: '#7e22ce',
  Provider:   '#be185d',
  Dictionary: '#0e7490',
  Join:       '#65a30d',
  Extension:  '#4338ca',
  Island:     '#475569',
  Decision:   '#b45309',
};

// React Flow adds a 15px panel margin: 35px places the minimap at x=50px,
// leaving 9px after the 26px-wide controls that begin at x=15px.
const MINIMAP_LEFT_OFFSET = '35px';

function minimapNodeColor(node: Node): string {
  return NODE_COLORS[node.type ?? ''] ?? '#6c7086';
}

function isEditableTarget(target: EventTarget | null): boolean {
  return target instanceof Element && target.closest(
    'input, textarea, select, [contenteditable]:not([contenteditable="false"])'
  ) !== null;
}

export default function GraphMinimap({
  open,
  onOpenChange,
  hotkeyEnabled,
  hintVisible,
  hintFading,
  onDismissHint,
  onHintFocusChange,
}: GraphMinimapProps) {
  const toggleLabel = open ? 'Hide minimap' : 'Show minimap';
  const hintId = useId();
  const showHint = hintVisible && !open;
  const toggleIconRef = useRef<SVGSVGElement | null>(null);

  const focusToggle = useCallback(() => {
    toggleIconRef.current?.closest('button')?.focus();
  }, []);

  const toggleMinimap = useCallback(() => {
    const nextOpen = !open;
    if (nextOpen && showHint) focusToggle();
    onOpenChange(nextOpen);
  }, [focusToggle, onOpenChange, open, showHint]);

  const dismissHint = useCallback(() => {
    focusToggle();
    onDismissHint();
  }, [focusToggle, onDismissHint]);

  useEffect(() => {
    if (!hotkeyEnabled) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (
        event.defaultPrevented
        || event.repeat
        || !event.ctrlKey
        || event.metaKey
        || event.altKey
        || event.shiftKey
        || event.code !== 'KeyM'
        || isEditableTarget(event.target)
      ) {
        return;
      }

      event.preventDefault();
      toggleMinimap();
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [hotkeyEnabled, toggleMinimap]);

  return (
    <>
      {open && (
        <MiniMap
          className={styles.minimap}
          nodeColor={minimapNodeColor}
          maskColor="rgba(0,0,0,0.3)"
          pannable
          position="bottom-left"
          style={{ background: '#fff', left: MINIMAP_LEFT_OFFSET }}
        />
      )}
      <Controls
        position="bottom-left"
        showInteractive={false}
        className={styles.controls}
      >
        {showHint && (
          <div
            id={hintId}
            className={`${styles.hint}${hintFading ? ` ${styles.hintFading}` : ''} nodrag nopan`}
            onFocus={() => onHintFocusChange(true)}
            onBlur={(event) => {
              if (!event.currentTarget.contains(event.relatedTarget)) {
                onHintFocusChange(false);
              }
            }}
            role="status"
          >
            <button
              type="button"
              className={styles.hintDismissButton}
              aria-label="Dismiss minimap shortcut hint"
              onClick={dismissHint}
            >
              <kbd className={styles.hintKbd}>Ctrl + M</kbd> to toggle minimap
            </button>
          </div>
        )}
        <ControlButton
          className={`${styles.toggleButton}${showHint && !hintFading ? ` ${styles.toggleButtonPulsing}` : ''} nodrag nopan`}
          aria-describedby={showHint ? hintId : undefined}
          aria-label={toggleLabel}
          aria-keyshortcuts="Control+M"
          aria-pressed={open}
          title={`${toggleLabel} (Ctrl + M)`}
          onClick={toggleMinimap}
        >
          <svg
            ref={toggleIconRef}
            className={styles.toggleIcon}
            viewBox="1.5 2 17 16"
            fill="none"
            aria-hidden="true"
            focusable="false"
          >
            <rect x="2.5" y="3" width="15" height="14" rx="1.5" />
            <path d="M6 12.5 9 8l2.5 2 2.5-3" />
            <circle cx="6" cy="12.5" r="1" />
            <circle cx="9" cy="8" r="1" />
            <circle cx="11.5" cy="10" r="1" />
            <circle cx="14" cy="7" r="1" />
          </svg>
        </ControlButton>
      </Controls>
    </>
  );
}
