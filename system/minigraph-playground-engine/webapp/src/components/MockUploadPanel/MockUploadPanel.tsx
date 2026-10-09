import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import styles from './MockUploadPanel.module.css';
import { tryParseJSON } from '../../utils/messageParser';
import { formatJSON } from '../../utils/validators';
import { useMockUpload } from '../../hooks/useMockUpload';
import CloseIcon from '../../icons/CloseIcon.svg?react';
import { readFileAsText, validateJsonFileType } from '../../utils/jsonFile';

interface MockUploadPanelProps {
  /** The POST path extracted from the server message, e.g. "/api/mock/ws-417669-24" */
  uploadPath: string;
  /** Called with the drained response body and owning path on a 2xx response. */
  onSuccess: (responseBody: string, uploadPath: string) => void;
  /** Called with the owning path to close the panel. Playground restores focus. */
  onClose: (uploadPath: string) => void;
  /** Called with a human-readable error string on failure. */
  onError: (errorMessage: string) => void;
  /** Optional workflow-specific title. Defaults to the manual mock-upload title. */
  title?: string;
  /** Optional context shown above the JSON editor. */
  description?: string;
  /** Derived graph paths shown as non-authoritative input hints. */
  inputPathHints?: string[];
  /** Derived input header names shown as non-authoritative hints (`*` = the whole map). */
  inputHeaderHints?: string[];
  /** Submit action label. Defaults to the existing Upload action. */
  submitLabel?: string;
}

// Derive macOS status once — no hook needed; navigator APIs are synchronous.
// navigator.userAgentData?.platform is preferred (Chromium-based browsers);
// navigator.platform is the deprecated-but-universal fallback (Firefox, Safari).
/** One mock request header row: a name and a text value. */
interface HeaderRow {
  key: string;
  name: string;
  value: string;
}

let headerRowSeq = 0;
function newHeaderRow(): HeaderRow {
  headerRowSeq += 1;
  return { key: `h${headerRowSeq}`, name: '', value: '' };
}

/**
 * Issues of the header rows, by row key: a value needs a name, and a name may not repeat
 * (header names are case-insensitive to the engine). Blank rows are ignored.
 */
export function headerRowIssues(rows: HeaderRow[]): Map<string, string> {
  const issues = new Map<string, string>();
  const seen = new Set<string>();
  for (const row of rows) {
    const name = row.name.trim();
    if (name === '') {
      if (row.value.trim() !== '') issues.set(row.key, 'A header value needs a name');
      continue;
    }
    const folded = name.toLowerCase();
    if (seen.has(folded)) {
      issues.set(row.key, `Duplicate header name '${name}'`);
    } else {
      seen.add(folded);
    }
  }
  return issues;
}

const isMac =
  ((navigator as Navigator & { userAgentData?: { platform: string } }).userAgentData?.platform
    ?? navigator.platform)
    .toLowerCase()
    .includes('mac');

/**
 * Mock-data upload form rendered in the left panel slot (the console's
 * space) instead of a modal — the same in-place pattern as NodeEditPanel:
 * Esc / Cancel / a successful upload closes the session and the slot
 * returns to whatever it held before (console back if it was open,
 * full-width graph if hidden). consoleOpen itself is never touched.
 *
 * Serves both entry points: the manual re-open button on a console
 * invitation row, and the graph-run workflow's "Mock Graph Input" step
 * (title / description / hints / submit label come in as props).
 */
export default function MockUploadPanel({
  uploadPath,
  onSuccess,
  onClose,
  onError,
  title = '⬆️ Upload Mock Data',
  description,
  inputPathHints = [],
  inputHeaderHints = [],
  submitLabel = 'Upload',
}: MockUploadPanelProps) {
  const [json,        setJson]        = useState('');
  const [headerRows,  setHeaderRows]  = useState<HeaderRow[]>([]);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [fileError,   setFileError]   = useState<string | null>(null);
  const [isDragOver,  setIsDragOver]  = useState(false);

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ── Validation ──────────────────────────────────────────────────────────
  // Uses tryParseJSON directly — not validatePayload — because the mock endpoint
  // is JSON-only. tryParseJSON returns isJSON: false for JSON primitives, which
  // is intentional: the endpoint expects an object or array.
  const jsonResult  = tryParseJSON(json);
  const isValidJson = jsonResult.isJSON;
  const hasBody     = json.trim() !== '';
  const rowIssues   = useMemo(() => headerRowIssues(headerRows), [headerRows]);
  // the named rows become the mock input.header (names as written: the engine reads them
  // case-insensitively, exactly as a real request's)
  const headers = useMemo(() => {
    const map: Record<string, string> = {};
    for (const row of headerRows) {
      const name = row.name.trim();
      if (name !== '' && !rowIssues.has(row.key)) map[name] = row.value;
    }
    return map;
  }, [headerRows, rowIssues]);
  const headerCount = Object.keys(headers).length;
  const canSubmit   = (hasBody ? isValidJson : headerCount > 0) && rowIssues.size === 0;

  // ── useMockUpload ────────────────────────────────────────────────────────
  const { isUploading, upload, cancel } = useMockUpload({
    uploadPath,
    json,
    headers,
    onSuccess: (responseBody) => onSuccess(responseBody, uploadPath),
    onError: (msg) => {
      setUploadError(msg);   // inline error banner
      onError(msg);          // Playground fires a toast
    },
  });

  // ── Close handling ───────────────────────────────────────────────────────
  const handleClose = useCallback(() => {
    cancel();
    onClose(uploadPath); // Playground unmounts the component and restores focus.
  }, [cancel, onClose, uploadPath]);

  // Focus the editor on mount; Escape closes the panel and restores the
  // previous left-panel content (same contract as NodeEditPanel). An upload
  // in flight blocks Esc, exactly like the disabled Cancel button.
  const isUploadingRef = useRef(isUploading);
  useEffect(() => { isUploadingRef.current = isUploading; }, [isUploading]);
  useEffect(() => {
    textareaRef.current?.focus();
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      if (!isUploadingRef.current) handleClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleClose]);

  // ── Upload handling ──────────────────────────────────────────────────────
  const handleUpload = useCallback(() => {
    setUploadError(null); // clear stale error from previous attempt
    upload();
  }, [upload]);

  const handleTextareaKeyDown = useCallback((e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      if (canSubmit && !isUploading) {
        handleUpload();
      }
    }
  }, [canSubmit, isUploading, handleUpload]);

  const addHeaderRow = useCallback(() => setHeaderRows(rows => [...rows, newHeaderRow()]), []);
  const updateHeaderRow = useCallback((key: string, patch: Partial<HeaderRow>) => {
    setHeaderRows(rows => rows.map(row => (row.key === key ? { ...row, ...patch } : row)));
  }, []);
  const removeHeaderRow = useCallback((key: string) => {
    setHeaderRows(rows => rows.filter(row => row.key !== key));
  }, []);

  const handleFormat = useCallback(() => {
    if (!isValidJson) return;
    setJson(formatJSON(json));
  }, [isValidJson, json]);

  // ── File loading (shared by drop and file-picker) ────────────────────────
  const loadFile = useCallback(async (file: File) => {
    setFileError(null);
    setUploadError(null);

    const typeError = validateJsonFileType(file);
    if (typeError) {
      setFileError(typeError);
      return;
    }

    try {
      const text = await readFileAsText(file);
      // Validate it parses as a JSON object/array before loading into textarea.
      const result = tryParseJSON(text);
      if (!result.isJSON) {
        setFileError(`"${file.name}" contains invalid JSON.`);
        return;
      }
      setJson(formatJSON(text));   // pretty-print on load
      textareaRef.current?.focus();
    } catch (err) {
      setFileError((err as Error).message);
    }
  }, []);

  // ── Drag-and-drop handlers ────────────────────────────────────────────────
  const handleDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isDragOver) setIsDragOver(true);
  }, [isDragOver]);

  const handleDragLeave = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    // Only clear if leaving the drop zone itself, not a child element.
    if (e.currentTarget === e.target || !e.currentTarget.contains(e.relatedTarget as Node)) {
      setIsDragOver(false);
    }
  }, []);

  const handleDrop = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);

    const file = e.dataTransfer.files[0];
    if (!file) return;
    loadFile(file);
  }, [loadFile]);

  // ── File-picker handler ───────────────────────────────────────────────────
  const handleFileInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    loadFile(file);
    // Reset the input so the same file can be re-selected after a fix.
    e.target.value = '';
  }, [loadFile]);

  const showValidationError = !isValidJson && hasBody;

  return (
    <div className={styles.root}>
      <section className={styles.card} aria-label={title}>

        {/* ── Header ribbon ──────────────────────────────────────────── */}
        <header className={styles.ribbon}>
          <div className={styles.titleGroup}>
            <span className={styles.title}>{title}</span>
            <span className={styles.path}>{uploadPath}</span>
          </div>
          <button
            className={styles.ribbonClose}
            onClick={handleClose}
            aria-label="Close upload panel"
            title="Close (Esc)"
            disabled={isUploading}
          >
            <CloseIcon className={styles.ribbonCloseIcon} aria-hidden="true" focusable="false" />
          </button>
        </header>

        {/* ── Body ───────────────────────────────────────────────────── */}
        <div className={styles.body}>

          {description && <p className={styles.description}>{description}</p>}

          {inputPathHints.length > 0 && (
            <div className={styles.inputHints} aria-label="Referenced graph input paths">
              <span className={styles.inputHintsLabel}>Referenced input paths</span>
              <div className={styles.inputHintList}>
                {inputPathHints.slice(0, 6).map(path => <code key={path}>{path}</code>)}
                {inputPathHints.length > 6 && (
                  <span className={styles.moreHints}>+{inputPathHints.length - 6} more</span>
                )}
              </div>
              <span className={styles.inputHintsNote}>Hints are derived from graph references.</span>
            </div>
          )}

          {inputHeaderHints.length > 0 && (
            <div className={styles.inputHints} aria-label="Referenced input headers">
              <span className={styles.inputHintsLabel}>Referenced input headers</span>
              <div className={styles.inputHintList}>
                {inputHeaderHints.slice(0, 6).map(name => (
                  <code key={name}>{name === '*' ? 'input.header (all)' : name}</code>
                ))}
                {inputHeaderHints.length > 6 && (
                  <span className={styles.moreHints}>+{inputHeaderHints.length - 6} more</span>
                )}
              </div>
              <span className={styles.inputHintsNote}>Header names are case-insensitive to the graph.</span>
            </div>
          )}

          {/* ── Drop zone ──────────────────────────────────────────── */}
          <div
            className={`${styles.dropZone} ${isDragOver ? styles.dropZoneActive : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            aria-label="Drop a JSON file here"
          >
            <span className={styles.dropZoneIcon}>📂</span>
            <span className={styles.dropZoneText}>
              Drop a <code>.json</code> file here
            </span>
            <span className={styles.dropZoneOr}>— or —</span>
            {/* Hidden file input, triggered by the visible button below */}
            <input
              ref={fileInputRef}
              type="file"
              accept=".json,application/json"
              className={styles.fileInputHidden}
              aria-hidden="true"
              tabIndex={-1}
              onChange={handleFileInputChange}
            />
            <button
              type="button"
              className={styles.browseButton}
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploading}
              aria-label="Browse for a JSON file"
            >
              Browse file…
            </button>
          </div>

          {fileError && (
            <span className={styles.fileError} role="alert">
              ⚠️ {fileError}
            </span>
          )}

          <label htmlFor="mock-upload-textarea" className={styles.textareaLabel}>
            JSON Payload
          </label>
          <textarea
            id="mock-upload-textarea"
            ref={textareaRef}
            className={styles.textarea}
            value={json}
            onChange={(e) => { setJson(e.target.value); setFileError(null); }}
            onKeyDown={handleTextareaKeyDown}
            placeholder='Paste JSON here, or drop / browse a .json file above'
            rows={10}
            spellCheck={false}
            aria-describedby={showValidationError ? 'mock-upload-validation' : undefined}
          />
          {showValidationError && (
            <span
              id="mock-upload-validation"
              className={styles.validationError}
              role="status"
            >
              ⚠️ Invalid JSON — check syntax
            </span>
          )}
          <span className={styles.keyboardHint}>
            {isMac ? `⌘+Enter to ${submitLabel.toLowerCase()}` : `Ctrl+Enter to ${submitLabel.toLowerCase()}`}
          </span>
          {/* ── Mock headers (optional) ─────────────────────────────── */}
          <div className={styles.headers}>
            <span className={styles.textareaLabel}>Headers (optional)</span>
            <div className={styles.rows} role="group" aria-label="Mock headers">
              {headerRows.map(row => {
                const issue = rowIssues.get(row.key);
                return (
                  <div key={row.key} className={styles.rowBlock}>
                    <div className={styles.row}>
                      <input
                        className={`${styles.input} ${styles.rowName}${issue ? ` ${styles.inputInvalid}` : ''}`}
                        type="text"
                        value={row.name}
                        onChange={event => updateHeaderRow(row.key, { name: event.target.value })}
                        placeholder="name"
                        aria-label="Header name"
                        autoComplete="off"
                        spellCheck={false}
                        disabled={isUploading}
                      />
                      <input
                        className={`${styles.input} ${styles.rowValue}`}
                        type="text"
                        value={row.value}
                        onChange={event => updateHeaderRow(row.key, { value: event.target.value })}
                        placeholder="value"
                        aria-label={`Value of header ${row.name || '(unnamed)'}`}
                        autoComplete="off"
                        disabled={isUploading}
                      />
                      <button
                        type="button"
                        className={styles.entryRemove}
                        onClick={() => removeHeaderRow(row.key)}
                        disabled={isUploading}
                        aria-label={`Remove header ${row.name || '(unnamed)'}`}
                        title="Remove header"
                      >
                        ×
                      </button>
                    </div>
                    {issue && <span className={styles.entryIssue} role="alert">⚠️ {issue}</span>}
                  </div>
                );
              })}
              <button type="button" className={styles.linkButton} onClick={addHeaderRow} disabled={isUploading}>
                + Add header
              </button>
            </div>
            <span className={styles.inputHintsNote}>
              The headers become the instance's input.header, read case-insensitively by the graph, as a real
              request's are.
            </span>
          </div>

          {uploadError && (
            <div className={styles.errorBanner} role="alert">
              ❌ Upload failed: {uploadError}
            </div>
          )}
        </div>

        {/* ── Footer ─────────────────────────────────────────────────── */}
        <footer className={styles.footer}>
          <button
            className={styles.formatButton}
            onClick={handleFormat}
            disabled={!isValidJson || isUploading}
            title="Format JSON"
            aria-label="Format JSON"
          >
            Format
          </button>
          <div className={styles.footerActions}>
            <button
              className={styles.cancelButton}
              onClick={handleClose}
              disabled={isUploading}
            >
              Cancel
            </button>
            <button
              className={styles.uploadButton}
              onClick={handleUpload}
              disabled={!canSubmit || isUploading}
              aria-busy={isUploading}
            >
              {isUploading ? (
                <><span className={styles.spinner} aria-hidden="true" /> Uploading…</>
              ) : (
                submitLabel === 'Upload' ? 'Upload ▶' : submitLabel
              )}
            </button>
          </div>
        </footer>

      </section>
    </div>
  );
}
