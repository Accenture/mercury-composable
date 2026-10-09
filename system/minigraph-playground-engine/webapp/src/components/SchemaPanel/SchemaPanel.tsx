import { useEffect, useRef } from 'react';
import styles from './SchemaPanel.module.css';
import CloseIcon from '../../icons/CloseIcon.svg?react';
import { rowsKey, type SchemaRowsKey, type UseSchemaPanelReturn } from '../../hooks/useSchemaPanel';
import {
  BODY_TYPES,
  HEADER_TYPES,
  ITEM_TYPES,
  SCHEMA_NODE_ALIAS,
  summarizeConstraints,
  type SchemaPart,
  type SchemaRow,
  type SchemaRowType,
  type SchemaSide,
} from '../../utils/graphSchema';

interface SchemaPanelProps {
  controller: UseSchemaPanelReturn;
  /** The graph's display name, shown in the ribbon. */
  graphName: string | null;
  /** True when the browser opens a native "save as" dialog for the download (Chromium). */
  supportsFolderPicker: boolean;
}

export const SCHEMA_PANEL_TITLE = '📐 Graph schema';

const SIDE_LABEL: Record<SchemaSide, string> = { input: 'Input', output: 'Output' };
const SIDE_WORD: Record<SchemaSide, string> = { input: 'request', output: 'response' };

const SOURCE_LABEL: Record<SchemaRow['source'], string> = {
  declared: 'declared',
  discovered: 'discovered',
  both: 'declared',
  run: 'from last run',
  new: 'new',
};
const SOURCE_CLASS: Record<SchemaRow['source'], string> = {
  declared: styles.chipDeclared,
  discovered: styles.chipDiscovered,
  both: styles.chipDeclared,
  run: styles.chipRun,
  new: styles.chipNew,
};

function sourceTitle(row: SchemaRow): string {
  const referenced = row.usedBy.length > 0 ? `Referenced by ${row.usedBy.join(', ')}` : 'Not referenced by the model';
  switch (row.source) {
    case 'declared': return `Declared on the node. ${referenced}.`;
    case 'both': return `Declared on the node and discovered in the model. ${referenced}.`;
    case 'discovered': return `Discovered in the model, not declared yet. ${referenced}.`;
    case 'run': return 'Typed from the instance\'s last run.';
    default: return 'Added here; saved with the declaration.';
  }
}

function typeLabel(type: SchemaRowType | ''): string {
  return type === '' ? '(any)' : type;
}

/**
 * The Schema panel in the left panel slot (the console's space), the in-place pattern of the other
 * panels: Esc / Cancel closes it and the slot returns to whatever it held. Two tabs - Input (the
 * root node's `schema`, the request) and Output (the end node's, the response) - each a body section
 * with one row per path and a header section with one row per header name: path or name, type,
 * required, description, example and a chip saying where the row came from. Fill from last run
 * types the rows from the instance's values, Save writes the declaration through `update node`,
 * Download fetches the draft's OpenAPI document. The state and the transport are `useSchemaPanel`'s.
 */
export default function SchemaPanel({ controller, graphName, supportsFolderPicker }: SchemaPanelProps) {
  const {
    side, setSide, busy, close,
    contract, isLoading, loadError, reload,
    rows, issues, dirty, updateRow, addRow, removeRow,
    nodeOf, issuesOf,
    fillFromRun, isFilling,
    save, canSave, isSaving, saveError,
    download, isDownloading,
  } = controller;

  // Escape closes - unless a request is in flight, exactly like the disabled Cancel button.
  const escapeRef = useRef<() => void>(() => undefined);
  escapeRef.current = () => { if (!busy) close(); };
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      escapeRef.current();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  const node = nodeOf(side);
  const alias = SCHEMA_NODE_ALIAS[side];
  const contractIssues = issuesOf(side);
  const graphLabel = graphName?.trim() || contract?.graph || 'draft';

  const renderRows = (part: SchemaPart) => {
    const key: SchemaRowsKey = rowsKey(side, part);
    const list = rows[key];
    const rowIssues = issues[key];
    const types = part === 'header' ? HEADER_TYPES : BODY_TYPES;
    const namespace = `${side}.${part}`;
    const noun = part === 'header' ? 'header' : 'path';
    return (
      <>
        <div className={styles.sectionHeader}>
          <h3 className={styles.sectionTitle}>
            {part === 'header' ? 'Headers' : 'Body'} <code>{namespace}</code>
          </h3>
          <span className={styles.hint}>{list.length} {noun}{list.length === 1 ? '' : part === 'header' ? 's' : 's'}</span>
        </div>
        {list.length === 0 ? (
          <p className={styles.empty}>
            {part === 'header'
              ? <>No header. Add one when the {SIDE_WORD[side]} carries a header the graph {side === 'input' ? 'reads' : 'sets'}.</>
              : <>No path. Add one, or Fill from last run after a dry run.</>}
          </p>
        ) : (
          <div className={styles.rows} role="group" aria-label={`${namespace} rows`}>
            {list.map(row => {
              const issue = rowIssues.get(row.key);
              const label = row.path.trim() || `(new ${noun})`;
              const constraints = summarizeConstraints(row.constraints);
              return (
                <div key={row.key} className={`${styles.rowCard}${issue ? ` ${styles.rowFlagged}` : ''}`}>
                  <div className={styles.rowLine}>
                    <input
                      className={`${styles.input} ${styles.pathInput}${issue ? ` ${styles.inputInvalid}` : ''}`}
                      type="text"
                      value={row.path}
                      onChange={event => updateRow(key, row.key, { path: event.target.value })}
                      placeholder={part === 'header' ? 'X-Header-Name' : 'path, items[] for a list'}
                      aria-label={part === 'header' ? 'Header name' : 'Path'}
                      aria-invalid={Boolean(issue)}
                      autoComplete="off"
                      spellCheck={false}
                      disabled={busy}
                    />
                    <select
                      className={`${styles.select} ${styles.typeSelect}`}
                      value={row.type}
                      onChange={event => updateRow(key, row.key, { type: event.target.value as SchemaRowType })}
                      aria-label={`Type of ${label}`}
                      disabled={busy}
                    >
                      {types.map(type => <option key={type} value={type}>{typeLabel(type)}</option>)}
                    </select>
                    {row.type === 'array' && (
                      <select
                        className={`${styles.select} ${styles.itemSelect}`}
                        value={row.itemType}
                        onChange={event => updateRow(key, row.key, { itemType: event.target.value as SchemaRow['itemType'] })}
                        aria-label={`Item type of ${label}`}
                        title="The element type of the list; leave it blank for objects described by nested paths"
                        disabled={busy}
                      >
                        {ITEM_TYPES.map(type => <option key={type} value={type}>{type === '' ? 'of …' : `of ${type}`}</option>)}
                      </select>
                    )}
                    <label className={styles.requiredLabel} title={side === 'input' ? 'A missing value fails validation' : 'Always present in the response'}>
                      <input
                        type="checkbox"
                        checked={row.required}
                        onChange={event => updateRow(key, row.key, { required: event.target.checked })}
                        aria-label={`Required ${label}`}
                        disabled={busy}
                      />
                      req
                    </label>
                    <button
                      type="button"
                      className={styles.removeButton}
                      onClick={() => removeRow(key, row.key)}
                      disabled={busy}
                      aria-label={`Remove ${label}`}
                      title="Remove from the declaration"
                    >
                      ×
                    </button>
                  </div>
                  <div className={styles.rowLine}>
                    <input
                      className={`${styles.input} ${styles.descriptionInput}`}
                      type="text"
                      value={row.description}
                      onChange={event => updateRow(key, row.key, { description: event.target.value })}
                      placeholder="description"
                      aria-label={`Description of ${label}`}
                      autoComplete="off"
                      disabled={busy}
                    />
                    <input
                      className={`${styles.input} ${styles.exampleInput}`}
                      type="text"
                      value={row.example}
                      onChange={event => updateRow(key, row.key, { example: event.target.value })}
                      placeholder="example"
                      aria-label={`Example of ${label}`}
                      autoComplete="off"
                      spellCheck={false}
                      disabled={busy}
                    />
                  </div>
                  <div className={styles.chips}>
                    <span className={`${styles.chip} ${SOURCE_CLASS[row.source]}`} title={sourceTitle(row)}>
                      {SOURCE_LABEL[row.source]}
                    </span>
                    {row.usedBy.length > 0 && (
                      <span className={styles.chip} title={`Referenced by ${row.usedBy.join(', ')}`}>
                        used by {row.usedBy.length === 1 ? row.usedBy[0] : `${row.usedBy.length} nodes`}
                      </span>
                    )}
                    {constraints !== '' && (
                      <span
                        className={`${styles.chip} ${styles.chipConstraints}`}
                        title={`Carried unchanged - edit in the node editor:\n${JSON.stringify(row.constraints, null, 1)}`}
                      >
                        {constraints}
                      </span>
                    )}
                    {issue && <span className={styles.rowIssue} role="alert">⚠️ {issue}</span>}
                  </div>
                </div>
              );
            })}
          </div>
        )}
        <button type="button" className={styles.linkButton} onClick={() => addRow(key)} disabled={busy}>
          + Add {noun}
        </button>
      </>
    );
  };

  return (
    <div className={styles.root}>
      <section className={styles.card} aria-label={SCHEMA_PANEL_TITLE}>
        <header className={styles.ribbon}>
          <div className={styles.titleGroup}>
            <span className={styles.title}>{SCHEMA_PANEL_TITLE}</span>
            <span className={styles.subtitle}>{graphLabel} · node {alias}</span>
          </div>
          <button
            type="button"
            className={styles.ribbonClose}
            onClick={close}
            aria-label="Close schema panel"
            title="Close (Esc)"
            disabled={busy}
          >
            <CloseIcon className={styles.ribbonCloseIcon} aria-hidden="true" focusable="false" />
          </button>
        </header>

        <div className={styles.tabs} role="tablist" aria-label="Contract side">
          {(['input', 'output'] as const).map(which => (
            <button
              key={which}
              type="button"
              role="tab"
              className={`${styles.tab}${side === which ? ` ${styles.tabActive}` : ''}`}
              aria-selected={side === which}
              onClick={() => setSide(which)}
              title={`The ${SIDE_WORD[which]}: schema on node ${SCHEMA_NODE_ALIAS[which]}`}
            >
              {SIDE_LABEL[which]} <span className={styles.tabNode}>{SCHEMA_NODE_ALIAS[which]}</span>
              {dirty[which] && <span className={styles.dirtyDot} aria-label="unsaved" title="Unsaved changes" />}
            </button>
          ))}
        </div>

        <div className={styles.body} role="tabpanel" aria-label={`${SIDE_LABEL[side]} contract`}>
          <p className={styles.description}>
            {side === 'input'
              ? <>The request contract, <code>schema</code> on the root node: <code>input.body</code> is validated against it on every run, before anything else - a bad request never reaches the graph. Headers are matched by name, case-insensitively.</>
              : <>The response contract, <code>schema</code> on the end node: documentary - it describes <code>output.body</code> and <code>output.header</code> in the OpenAPI document and is never enforced.</>}
          </p>

          {loadError && (
            <div className={styles.errorBanner} role="alert">
              <span>❌ {loadError}</span>
              <button type="button" className={styles.linkButton} onClick={reload} disabled={isLoading}>Retry</button>
            </div>
          )}
          {isLoading && (
            <p className={styles.loading}><span className={`${styles.spinner} ${styles.spinnerDark}`} aria-hidden="true" /> Reading the contract…</p>
          )}
          {!node && !isLoading && (
            <div className={styles.warningBanner} role="status">
              This graph has no <code>{alias}</code> node; the {SIDE_WORD[side]} contract is declared on it. Create the node, then Save.
            </div>
          )}
          {contractIssues.length > 0 && (
            <ul className={styles.issueList} aria-label="Declaration versus model">
              {contractIssues.map(issue => <li key={issue}>⚠️ {issue}</li>)}
            </ul>
          )}

          {renderRows('body')}
          {renderRows('header')}

          <p className={styles.hint}>
            Types are strict JSON types: a numeric string is not a number; money is a <code>string</code> with a <code>pattern</code>.
            A row's other keywords (<code>enum</code>, <code>minimum</code>, <code>pattern</code>, …) are kept as declared and edited in the node editor.
          </p>

          {saveError && (
            <div className={styles.errorBanner} role="alert">
              <span>❌ {saveError}</span>
            </div>
          )}
        </div>

        <footer className={styles.footer}>
          <div className={styles.footerTools}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => { void fillFromRun(); }}
              disabled={busy || isLoading}
              aria-busy={isFilling}
              title="Types and examples from the instance's actual input and output, after a dry run"
            >
              {isFilling ? <><span className={`${styles.spinner} ${styles.spinnerDark}`} aria-hidden="true" /> Reading…</> : 'Fill from last run'}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={reload}
              disabled={busy || isLoading}
              title="Read the graph again, discarding unsaved rows"
            >
              Reload
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => { void download(); }}
              disabled={busy || isLoading}
              aria-busy={isDownloading}
              title={`The draft's OpenAPI 3.0 document as YAML - ${supportsFolderPicker ? 'the next dialog picks the folder' : 'saved to the download folder'}`}
            >
              {isDownloading ? <><span className={`${styles.spinner} ${styles.spinnerDark}`} aria-hidden="true" /> Fetching…</> : 'Download YAML'}
            </button>
          </div>
          <div className={styles.footerActions}>
            <button type="button" className={styles.secondaryButton} onClick={close} disabled={busy}>
              Cancel
            </button>
            <button
              type="button"
              className={styles.primaryButton}
              onClick={save}
              disabled={!canSave}
              aria-busy={isSaving}
              title={canSave
                ? `Writes the declaration to node ${alias} through update node - every member sees it`
                : dirty[side] ? 'Fix the flagged rows first' : 'Nothing changed'}
            >
              {isSaving ? <><span className={styles.spinner} aria-hidden="true" /> Saving…</> : `Save ${alias} schema`}
            </button>
          </div>
        </footer>
      </section>
    </div>
  );
}
