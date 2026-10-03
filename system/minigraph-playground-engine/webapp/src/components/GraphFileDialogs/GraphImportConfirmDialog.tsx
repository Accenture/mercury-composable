import { useEffect, useId, useRef } from 'react';
import type { PendingGraphImport } from '../../hooks/useGraphFileImport';
import styles from './GraphFileDialogs.module.css';

interface GraphImportConfirmDialogProps {
  pending: PendingGraphImport;
  onReplace: () => void;
  onCancel: () => void;
}

/**
 * Importing a file while a graph is loaded replaces the draft - in this session and, in a
 * shared session, in every member's - and clears a graph instance; it cannot be undone, so
 * the user confirms first.
 */
export function GraphImportConfirmDialog({ pending, onReplace, onCancel }: GraphImportConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) {
      dialog.showModal();
    }
  }, []);

  const plural = (count: number, noun: string) => `${count} ${noun}${count === 1 ? '' : 's'}`;
  const summary = `${plural(pending.nodeCount, 'node')}, ${plural(pending.connectionCount, 'connection')}`
    + (pending.name ? `, named "${pending.name}"` : '');

  return (
    <dialog
      ref={dialogRef}
      className={styles.dialog}
      onClose={onCancel}
      aria-labelledby={titleId}
    >
      <h2 id={titleId} className={styles.title}>Replace the current graph?</h2>
      <p className={styles.body}>
        Importing <strong>"{pending.fileName}"</strong> ({summary}) replaces the draft in this
        session and in every member of a shared session; a graph instance is cleared.
      </p>
      <p className={styles.body}>
        This cannot be undone. Download or save the current graph first if you want to keep it.
      </p>
      <div className={styles.actions}>
        <button type="button" className={styles.cancelBtn} onClick={onCancel}>
          Cancel
        </button>
        <button type="button" className={styles.primaryBtn} onClick={onReplace}>
          Replace
        </button>
      </div>
    </dialog>
  );
}
