import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { GRAPH_ID_RULE, graphFileName, isValidGraphId } from '../../utils/graphFile';
import styles from './GraphFileDialogs.module.css';

interface GraphDownloadDialogProps {
  /** Pre-filled graph id (the root node's name when it has one). */
  defaultGraphId: string;
  /** True when the browser opens a native "save as" dialog next, where the folder is chosen. */
  supportsFolderPicker: boolean;
  onConfirm: (graphId: string) => void;
  onCancel: () => void;
}

/**
 * Asks for the graph id the file is saved under. The extension is always `.json`, and the
 * root node's name in the file becomes the graph id - the same thing `export graph as {name}`
 * does on the engine, so the file name and the model inside it agree.
 */
export function GraphDownloadDialog({
  defaultGraphId,
  supportsFolderPicker,
  onConfirm,
  onCancel,
}: GraphDownloadDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const [graphId, setGraphId] = useState(defaultGraphId);
  const [touched, setTouched] = useState(false);
  const titleId = useId();
  const inputId = useId();
  const hintId = useId();
  const trimmed = graphId.trim();
  const valid = isValidGraphId(trimmed);
  const showError = touched && !valid;

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) {
      dialog.showModal();
    }
    inputRef.current?.select();
  }, []);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (!valid) return;
    onConfirm(trimmed);
  };

  return (
    <dialog
      ref={dialogRef}
      className={styles.dialog}
      onClose={onCancel}
      aria-labelledby={titleId}
    >
      <form onSubmit={handleSubmit} noValidate>
        <h2 id={titleId} className={styles.title}>Download graph</h2>
        <label className={styles.label} htmlFor={inputId}>Graph ID</label>
        <div className={styles.inputRow}>
          <input
            id={inputId}
            ref={inputRef}
            className={`${styles.input}${showError ? ` ${styles.inputInvalid}` : ''}`}
            type="text"
            value={graphId}
            onChange={(event) => { setGraphId(event.target.value); setTouched(true); }}
            aria-describedby={hintId}
            aria-invalid={showError}
            autoComplete="off"
            spellCheck={false}
            maxLength={80}
          />
          <span className={styles.extension} aria-hidden="true">.json</span>
        </div>
        <p
          id={hintId}
          className={`${styles.hint}${showError ? ` ${styles.hintError}` : ''}`}
          role={showError ? 'alert' : undefined}
        >
          {showError
            ? `A graph id uses ${GRAPH_ID_RULE}.`
            : `Saved as ${graphFileName(trimmed || 'graph-id')}. The root node's name in the file becomes the `
              + 'graph id, as "export graph as" does. '
              + (supportsFolderPicker
                ? 'The next dialog picks the folder.'
                : "The browser saves it to its download folder.")}
        </p>
        <div className={styles.actions}>
          <button type="button" className={styles.cancelBtn} onClick={onCancel}>
            Cancel
          </button>
          <button type="submit" className={styles.primaryBtn} disabled={!valid}>
            Download
          </button>
        </div>
      </form>
    </dialog>
  );
}
