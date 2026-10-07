/**
 * NavMenu
 *
 * A compact, accessible dropdown menu for the navigation toolbar.
 *
 * Features:
 *  - Closes on outside click, Escape key, and when focus leaves the widget.
 *  - Renders an optional status dot beside the button label (used by the
 *    Connections menu to reflect aggregate WebSocket state).
 *  - Accepts arbitrary children so callers can render NavLinks, anchors, or
 *    custom ConnectionBar rows without coupling this component to any one use-case.
 *    Children may be a function of `close`, for an item that closes the menu
 *    when it is picked (nothing else closes it on an item click).
 */

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ReactNode,
  type KeyboardEvent,
} from 'react';
import styles from './NavMenu.module.css';

export type DotStatus = 'idle' | 'connecting' | 'connected' | 'partial';

interface NavMenuProps {
  /** Button label text */
  label: string;
  /** Optional status dot shown next to the label */
  dotStatus?: DotStatus;
  /** Menu content, or a function of `close` for content with an item that closes the menu */
  children: ReactNode | ((close: () => void) => ReactNode);
}

export default function NavMenu({ label, dotStatus, children }: NavMenuProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const close = useCallback(() => setOpen(false), []);

  // Close on outside click
  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  // Close on Escape from anywhere inside the widget
  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'Escape') {
      setOpen(false);
      containerRef.current?.querySelector<HTMLButtonElement>('button[aria-haspopup]')?.focus();
    }
  };

  const dotClass =
    dotStatus === 'connected'  ? styles.dotConnected  :
    dotStatus === 'connecting' ? styles.dotConnecting :
    dotStatus === 'partial'    ? styles.dotPartial    :
    dotStatus === 'idle'       ? styles.dotIdle       : undefined;

  return (
    <div
      className={styles.container}
      ref={containerRef}
      onKeyDown={handleKeyDown}
    >
      <button
        className={styles.trigger}
        onClick={() => setOpen(prev => !prev)}
        aria-haspopup="true"
        aria-expanded={open}
      >
        {dotStatus !== undefined && (
          <span className={`${styles.dot} ${dotClass ?? ''}`} aria-hidden="true" />
        )}
        <span>{label}</span>
        <span className={`${styles.chevron} ${open ? styles.chevronOpen : ''}`} aria-hidden="true">
          ▾
        </span>
      </button>

      {open && (
        <div className={styles.dropdown} role="menu">
          {typeof children === 'function' ? children(close) : children}
        </div>
      )}
    </div>
  );
}
