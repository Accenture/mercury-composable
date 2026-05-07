import { useCallback, useEffect, useRef } from 'react';
import type { NodeDraft } from '../../graphActions/nodeAuthoringTypes';
import { createPropertyRow } from '../../graphActions/propertyRows';
import { getValidationErrorKeyForProperty } from '../../graphActions/validation';
import styles from './NodeDialog.module.css';

interface NodeDialogProps {
  open: boolean;
  draft: NodeDraft;
  phase: 'editing' | 'sending';
  lockReason: null | 'sending' | 'disconnected';
  serverMessage: string | null;
  validationErrors: Record<string, string>;
  onDraftChange: (draft: NodeDraft) => void;
  onSubmit: () => void;
  onClose: () => void;
}

// Presentational modal only. It edits a NodeDraft and reports submit/close
// intents upward; useGraphAuthoring owns validation, transport, and result handling.
export default function NodeDialog({
  open,
  draft,
  phase,
  lockReason,
  serverMessage,
  validationErrors,
  onDraftChange,
  onSubmit,
  onClose,
}: NodeDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const aliasRef = useRef<HTMLInputElement>(null);
  const sending = phase === 'sending';
  const disconnected = lockReason === 'disconnected';
  const controlsDisabled = sending || disconnected;

  // Native dialog gives us modal focus behavior; the hook still controls whether
  // Escape/backdrop close is allowed for the current phase.
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      dialog.showModal();
      aliasRef.current?.focus();
    }
    return () => {
      if (dialog.open) dialog.close();
    };
  }, [open]);

  const handleCancel = useCallback((event: React.SyntheticEvent<HTMLDialogElement>) => {
    event.preventDefault();
    if (!sending) onClose();
  }, [onClose, sending]);

  // Backdrop clicks target the <dialog> element itself. Inner panel clicks are
  // stopped below so normal form interaction never closes the modal.
  const handleBackdropClick = useCallback((event: React.MouseEvent<HTMLDialogElement>) => {
    if (event.target === dialogRef.current && !sending) {
      onClose();
    }
  }, [onClose, sending]);

  const updateDraft = useCallback((patch: Partial<NodeDraft>) => {
    onDraftChange({ ...draft, ...patch });
  }, [draft, onDraftChange]);

  const updateProperty = useCallback((rowId: string, patch: { key?: string; value?: string }) => {
    onDraftChange({
      ...draft,
      properties: draft.properties.map((row) => row.id === rowId ? { ...row, ...patch } : row),
    });
  }, [draft, onDraftChange]);

  const removeProperty = useCallback((rowId: string) => {
    const nextRows = draft.properties.filter((row) => row.id !== rowId);
    onDraftChange({
      ...draft,
      properties: nextRows.length > 0 ? nextRows : [createPropertyRow()],
    });
  }, [draft, onDraftChange]);

  return (
    <dialog
      ref={dialogRef}
      className={styles.dialog}
      aria-modal="true"
      aria-labelledby="node-dialog-title"
      onCancel={handleCancel}
      onClick={handleBackdropClick}
    >
      <div className={styles.panel} onClick={(event) => event.stopPropagation()}>
        <header className={styles.header}>
          <div>
            <h2 id="node-dialog-title" className={styles.title}>Create Node</h2>
          </div>
          <button
            type="button"
            className={styles.iconButton}
            aria-label="Close create node dialog"
            onClick={onClose}
            disabled={sending}
          >
            <span className={styles.buttonIcon} aria-hidden="true">ⓧ</span>
          </button>
        </header>

        <div className={styles.body}>
          {serverMessage && !disconnected && (
            <div className={styles.message} role="status">
              {serverMessage}
            </div>
          )}
          {validationErrors.command && (
            <div className={styles.errorMessage} role="alert">
              {validationErrors.command}
            </div>
          )}
          {disconnected && (
            <div className={styles.warningMessage} role="status">
              {serverMessage ?? 'Connection disconnected. Refresh the page and create the node again after the app reconnects.'}
            </div>
          )}

          <label className={styles.field}>
            <span className={styles.label}>Alias</span>
            <input
              ref={aliasRef}
              className={styles.input}
              value={draft.alias}
              disabled={controlsDisabled}
              aria-invalid={Boolean(validationErrors.alias)}
              aria-describedby={validationErrors.alias ? 'node-alias-error' : undefined}
              onChange={(event) => updateDraft({ alias: event.target.value })}
            />
            {validationErrors.alias && (
              <span id="node-alias-error" className={styles.errorText}>{validationErrors.alias}</span>
            )}
          </label>

          <label className={styles.field}>
            <span className={styles.label}>Node Type</span>
            <input
              className={styles.input}
              value={draft.nodeType}
              disabled={controlsDisabled}
              aria-invalid={Boolean(validationErrors.nodeType)}
              aria-describedby={validationErrors.nodeType ? 'node-type-error' : undefined}
              onChange={(event) => updateDraft({ nodeType: event.target.value })}
            />
            {validationErrors.nodeType && (
              <span id="node-type-error" className={styles.errorText}>{validationErrors.nodeType}</span>
            )}
          </label>

          <section className={styles.properties} aria-labelledby="node-properties-title">
            <div className={styles.propertiesHeader}>
              <h3 id="node-properties-title" className={styles.sectionTitle}>Properties</h3>
              <button
                type="button"
                className={styles.secondaryButton}
                disabled={controlsDisabled}
                onClick={() => updateDraft({ properties: [...draft.properties, createPropertyRow()] })}
              >
                Add Property
              </button>
            </div>

            <div className={styles.propertyRows}>
              {draft.properties.map((row) => {
                const keyError = validationErrors[getValidationErrorKeyForProperty(row.id, 'key')];
                const valueError = validationErrors[getValidationErrorKeyForProperty(row.id, 'value')];
                return (
                  <div key={row.id} className={styles.propertyRow}>
                    <label className={styles.propertyField}>
                      <span className={styles.label}>Key</span>
                      <input
                        className={styles.input}
                        value={row.key}
                        disabled={controlsDisabled}
                        aria-invalid={Boolean(keyError)}
                        onChange={(event) => updateProperty(row.id, { key: event.target.value })}
                      />
                      {keyError && <span className={styles.errorText}>{keyError}</span>}
                    </label>
                    <label className={styles.propertyField}>
                      <span className={styles.label}>Value</span>
                      <input
                        className={styles.input}
                        value={row.value}
                        disabled={controlsDisabled}
                        aria-invalid={Boolean(valueError)}
                        onChange={(event) => updateProperty(row.id, { value: event.target.value })}
                      />
                      {valueError && <span className={styles.errorText}>{valueError}</span>}
                    </label>
                    <button
                      type="button"
                      className={styles.removeButton}
                      aria-label="Remove property"
                      disabled={controlsDisabled}
                      onClick={() => removeProperty(row.id)}
                    >
                      <span className={styles.buttonIcon} aria-hidden="true">ⓧ</span>
                    </button>
                  </div>
                );
              })}
            </div>
          </section>
        </div>

        <footer className={styles.footer}>
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={onClose}
            disabled={sending}
          >
            Cancel
          </button>
          <button
            type="button"
            className={styles.primaryButton}
            disabled={controlsDisabled}
            onClick={onSubmit}
          >
            {sending ? 'Creating...' : 'Create Node'}
          </button>
        </footer>
      </div>
    </dialog>
  );
}
