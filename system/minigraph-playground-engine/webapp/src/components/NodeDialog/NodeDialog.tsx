import { useCallback, useEffect, useRef } from 'react';
import type { NodeDraft } from '../../graphActions/nodeAuthoringTypes';
import { createPropertyRow } from '../../graphActions/propertyRows';
import { getValidationErrorKeyForProperty } from '../../graphActions/validation';
import CloseIcon from '../../icons/CloseIcon.svg?react';
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
  const aliasRef = useRef<HTMLInputElement>(null);
  const propertyKeyRefs = useRef(new Map<string, HTMLInputElement>());
  const pendingFocusPropertyIdRef = useRef<string | null>(null);
  const sending = phase === 'sending';
  const disconnected = lockReason === 'disconnected';
  const controlsDisabled = sending || disconnected;

  // The overlay is a real fixed element, not a native dialog backdrop, so it
  // reliably absorbs pointer events before underlying resize handles can drag.
  useEffect(() => {
    if (!open) return;
    aliasRef.current?.focus();
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      if (!sending) onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose, open, sending]);

  useEffect(() => {
    const rowId = pendingFocusPropertyIdRef.current;
    if (!rowId) return;

    const input = propertyKeyRefs.current.get(rowId);
    if (!input) return;

    input.focus();
    pendingFocusPropertyIdRef.current = null;
  }, [draft.properties]);

  const handleOverlayPointerDown = useCallback((event: React.PointerEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
  }, []);

  const handleOverlayClick = useCallback((event: React.MouseEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    if (!sending) onClose();
  }, [onClose, sending]);

  const stopPanelPointer = useCallback((event: React.PointerEvent<HTMLDivElement>) => {
    event.stopPropagation();
  }, []);

  const handleFormSubmit = useCallback((event: React.SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (controlsDisabled) return;
    onSubmit();
  }, [controlsDisabled, onSubmit]);

  const updateDraft = useCallback((patch: Partial<NodeDraft>) => {
    onDraftChange({ ...draft, ...patch });
  }, [draft, onDraftChange]);

  const updateProperty = useCallback((rowId: string, patch: { key?: string; value?: string }) => {
    onDraftChange({
      ...draft,
      properties: draft.properties.map((row) => row.id === rowId ? { ...row, ...patch } : row),
    });
  }, [draft, onDraftChange]);

  const addProperty = useCallback(() => {
    const nextRow = createPropertyRow();
    pendingFocusPropertyIdRef.current = nextRow.id;
    onDraftChange({
      ...draft,
      properties: [...draft.properties, nextRow],
    });
  }, [draft, onDraftChange]);

  const removeProperty = useCallback((rowId: string) => {
    const nextRows = draft.properties.filter((row) => row.id !== rowId);
    onDraftChange({
      ...draft,
      properties: nextRows.length > 0 ? nextRows : [createPropertyRow()],
    });
  }, [draft, onDraftChange]);

  if (!open) return null;

  return (
    <div
      className={styles.overlay}
      onPointerDown={handleOverlayPointerDown}
      onClick={handleOverlayClick}
    >
      <div
        className={styles.panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby="node-dialog-title"
        onPointerDown={stopPanelPointer}
        onClick={(event) => event.stopPropagation()}
      >
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
            <CloseIcon className={styles.buttonIcon} aria-hidden="true" focusable="false" />
          </button>
        </header>

        <form className={styles.form} onSubmit={handleFormSubmit}>
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
                          ref={(element) => {
                            if (element) {
                              propertyKeyRefs.current.set(row.id, element);
                            } else {
                              propertyKeyRefs.current.delete(row.id);
                            }
                          }}
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
                        <CloseIcon className={styles.buttonIcon} aria-hidden="true" focusable="false" />
                      </button>
                    </div>
                  );
                })}
              </div>
              <div className={styles.propertyActions}>
                <button
                  type="button"
                  className={`${styles.secondaryButton} ${styles.addPropertyButton}`}
                  disabled={controlsDisabled}
                  onClick={addProperty}
                >
                  <span aria-hidden="true">+</span>
                  <span>Add Property</span>
                </button>
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
              type="submit"
              className={styles.primaryButton}
              disabled={controlsDisabled}
            >
              {sending ? 'Creating...' : 'Create Node'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}
