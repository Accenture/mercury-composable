import { useCallback, useEffect, useId, useRef, useState } from 'react';
import styles from './GraphSetPanel.module.css';
import CloseIcon from '../../icons/CloseIcon.svg?react';
import type { UseGraphSetPanelReturn } from '../../hooks/useGraphSetPanel';
import {
  GRAPH_ID_FIELD, HOT_DEPLOY_KEYS, PACK_EXTENSION, isDeployed, packFileName, shortDigest,
} from '../../utils/graphSet';

interface GraphSetPanelProps {
  controller: UseGraphSetPanelReturn;
  /** True when the browser opens a native "save as" dialog for the download (Chromium). */
  supportsFolderPicker: boolean;
}

const TITLE = '📦 Graph set packaging';
const DEPLOY_TITLE = '🚀 Deploy to this engine';

function plural(count: number, noun: string): string {
  return `${count} ${noun}${count === 1 ? '' : 's'}`;
}

/**
 * The graph-set panel in the left panel slot (the console's space), the in-place pattern of
 * NodeEditPanel and MockUploadPanel: Esc / Cancel closes it and the slot returns to whatever it
 * held. Two modes: assembling a set (graph files and the current graph as entries, a manifest, Pack
 * and download) and inspecting one read back from a dropped `.pack` (its manifest and graphs, each
 * importable as the draft). The state and the transport are `useGraphSetPanel`'s.
 */
export default function GraphSetPanel({ controller, supportsFolderPicker }: GraphSetPanelProps) {
  const {
    mode, busy, close, leaveInspect,
    entries, entryIssues, addFiles, fileErrors, canAddCurrentGraph, addCurrentGraph, removeEntry, clearEntries,
    setName, setSetName, setNameError, rows, rowIssues, updateRow, addRow, removeRow,
    canPack, pack, isPacking, packError,
    inspected, isReading, importInspectedGraph, editInspectedSet,
    openDeploy, leaveDeploy, deployed, isListing, listError, hotDeployOff, refreshDeployed,
    expectedDigest, setExpectedDigest, deployInspected, packAndDeploy, canPackAndDeploy,
    isDeploying, deployError, lastDeployment, lastDigest,
    pendingUndeploy, requestUndeploy, cancelUndeploy, confirmUndeploy, isUndeploying,
  } = controller;
  const [isDragOver, setIsDragOver] = useState(false);
  const [setNameTouched, setSetNameTouched] = useState(false);
  const setNameInputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const setNameId = useId();
  const setNameHintId = useId();
  const expectedDigestId = useId();

  // Escape: back from inspect mode, else close - unless a request is in flight, exactly like the
  // disabled Cancel button. The latest handlers are read through refs so the listener is bound once.
  const escapeRef = useRef<() => void>(() => undefined);
  escapeRef.current = () => {
    if (busy) return;
    if (mode === 'inspect') leaveInspect();
    else if (mode === 'deploy') leaveDeploy();
    else close();
  };
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      escapeRef.current();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  // The set name is the first thing to fill in.
  useEffect(() => {
    if (mode === 'assemble') setNameInputRef.current?.focus();
  }, [mode]);

  // ── Drag and drop, shared by both modes ─────────────────────────────────
  const handleDragOver = useCallback((event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    if (!isDragOver) setIsDragOver(true);
  }, [isDragOver]);
  const handleDragLeave = useCallback((event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    if (event.currentTarget === event.target || !event.currentTarget.contains(event.relatedTarget as Node)) {
      setIsDragOver(false);
    }
  }, []);
  const handleDrop = useCallback((event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragOver(false);
    if (busy) return;
    void addFiles(event.dataTransfer.files);
  }, [busy, addFiles]);
  const handleFileInputChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (files && files.length > 0) void addFiles(files);
    // so the same files can be picked again after a fix
    event.target.value = '';
  }, [addFiles]);

  const trimmedSetName = setName.trim();
  const showSetNameError = setNameTouched && setNameError !== null;
  const fileName = packFileName(trimmedSetName || 'set-name');

  const dropZone = (
    <div
      className={`${styles.dropZone} ${isDragOver ? styles.dropZoneActive : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      aria-label="Drop graph files or a graph set here"
    >
      <span className={styles.dropZoneIcon}>📂</span>
      <span className={styles.dropZoneText}>
        {mode === 'deploy'
          ? <>Drop a <code>{PACK_EXTENSION}</code> here to deploy it to this engine</>
          : <>Drop <code>.json</code> graph files here, or a <code>{PACK_EXTENSION}</code> to inspect</>}
      </span>
      <span className={styles.dropZoneOr}>— or —</span>
      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept={`.json,${PACK_EXTENSION},application/json,application/octet-stream`}
        className={styles.fileInputHidden}
        aria-hidden="true"
        tabIndex={-1}
        onChange={handleFileInputChange}
      />
      <button
        type="button"
        className={styles.browseButton}
        onClick={() => fileInputRef.current?.click()}
        disabled={busy}
        aria-label="Browse for graph files"
      >
        Browse files…
      </button>
    </div>
  );

  if (mode === 'inspect' && inspected) {
    const { fileName: inspectedName, set } = inspected;
    const manifest = Object.entries(set.manifest);
    return (
      <div className={styles.root}>
        <section className={styles.card} aria-label="📦 Graph set">
          <header className={styles.ribbon}>
            <div className={styles.titleGroup}>
              <span className={styles.title}>📦 Graph set</span>
              <span className={styles.subtitle}>{inspectedName}</span>
            </div>
            <button
              type="button"
              className={styles.ribbonClose}
              onClick={close}
              aria-label="Close graph-set panel"
              title="Close"
              disabled={busy}
            >
              <CloseIcon className={styles.ribbonCloseIcon} aria-hidden="true" focusable="false" />
            </button>
          </header>
          <div className={styles.body}>
            <p className={styles.description}>
              What the engine read from the file: its manifest and its graphs. Import a graph as the session's
              draft to review it, or edit the whole set and pack it again.
            </p>
            <h3 className={styles.sectionTitle}>Manifest</h3>
            <dl className={styles.manifestList} aria-label="Manifest">
              {manifest.map(([key, value]) => (
                <div key={key} className={styles.manifestRow}>
                  <dt className={styles.manifestKey}>{key}</dt>
                  <dd className={styles.manifestValue}>{value}</dd>
                </div>
              ))}
            </dl>
            <h3 className={styles.sectionTitle}>Graphs ({set.graphs.length})</h3>
            <ul className={styles.entryList} aria-label="Graphs in the set">
              {set.graphs.map(graph => (
                <li key={graph.id} className={styles.entry}>
                  <div className={styles.entryMain}>
                    <code className={styles.entryId}>{graph.id}</code>
                    <span className={styles.entryCounts}>
                      {plural(graph.nodeCount, 'node')} · {plural(graph.connectionCount, 'connection')}
                    </span>
                  </div>
                  <button
                    type="button"
                    className={styles.entryAction}
                    onClick={() => { void importInspectedGraph(graph.id); }}
                    title="Replace the session's draft with this graph (the UI asks first when a graph is loaded)"
                  >
                    Import as draft
                  </button>
                </li>
              ))}
            </ul>
            {dropZone}
            {fileErrors.length > 0 && (
              <ul className={styles.fileErrors} role="alert">
                {fileErrors.map(error => <li key={error}>⚠️ {error}</li>)}
              </ul>
            )}
          </div>
          <footer className={styles.footer}>
            <button type="button" className={styles.secondaryButton} onClick={leaveInspect} disabled={busy}>
              Back
            </button>
            <div className={styles.footerActions}>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => { void deployInspected(); }}
                disabled={busy}
                title="Deploys this package to this engine (hot deployment, when the engine has it on)"
              >
                Deploy to this engine
              </button>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={editInspectedSet}
                disabled={busy}
                title="Loads this set's graphs and manifest into the editor, replacing its list"
              >
                Edit as new set
              </button>
            </div>
          </footer>
        </section>
      </div>
    );
  }

  if (mode === 'deploy') {
    const sets = deployed?.sets ?? [];
    return (
      <div className={styles.root}>
        <section className={styles.card} aria-label={DEPLOY_TITLE}>
          <header className={styles.ribbon}>
            <div className={styles.titleGroup}>
              <span className={styles.title}>{DEPLOY_TITLE}</span>
              <span className={styles.subtitle}>{deployed?.location ?? 'hot deployment'}</span>
            </div>
            <button
              type="button"
              className={styles.ribbonClose}
              onClick={close}
              aria-label="Close graph-set panel"
              title="Close"
              disabled={busy}
            >
              <CloseIcon className={styles.ribbonCloseIcon} aria-hidden="true" focusable="false" />
            </button>
          </header>
          <div className={styles.body}>
            <p className={styles.description}>
              A certified graph set goes live on this engine without a restart: the set is named after its main graph,
              every graph passes the deployment gate, and the set registers all or none. Its subgraphs stay private.
            </p>
            {hotDeployOff ? (
              <div className={styles.warning} role="status">
                Hot deployment is off on this engine. Turn on <code>{HOT_DEPLOY_KEYS[0]}</code> to deploy and{' '}
                <code>{HOT_DEPLOY_KEYS[1]}</code> to undeploy, then open this view again.
              </div>
            ) : (
              <>
                {dropZone}
                <div className={styles.addRow}>
                  <button
                    type="button"
                    className={styles.secondaryButton}
                    onClick={() => { void packAndDeploy(); }}
                    disabled={!canPackAndDeploy}
                    aria-busy={isPacking || isDeploying}
                    title={canPackAndDeploy
                      ? `Packs the assembled set on the engine and deploys ${fileName} in one step`
                      : 'Assemble and name a set in the editor first'}
                  >
                    {isPacking ? 'Packing…' : isDeploying ? 'Deploying…' : `Pack and deploy ${fileName}`}
                  </button>
                </div>
                <label className={styles.fieldLabel} htmlFor={expectedDigestId}>Expected SHA-256 (optional)</label>
                <input
                  id={expectedDigestId}
                  className={`${styles.input} ${styles.mono}`}
                  type="text"
                  value={expectedDigest}
                  onChange={event => setExpectedDigest(event.target.value)}
                  placeholder="the digest the ledger holds for this package"
                  autoComplete="off"
                  spellCheck={false}
                  disabled={busy}
                />
                <p className={styles.hint}>
                  Pasted from the field's ledger, it is sent as <code>X-Graph-Set-SHA256</code> and the engine refuses other
                  bytes. The digest of the package sent is shown below either way, to record it.
                </p>
              </>
            )}
            {fileErrors.length > 0 && (
              <ul className={styles.fileErrors} role="alert">
                {fileErrors.map(error => <li key={error}>⚠️ {error}</li>)}
              </ul>
            )}
            {deployError && (
              <div className={styles.errorBanner} role="alert">
                ❌ {deployError}
                {lastDigest && <div className={styles.mono}>SHA-256 of the package sent: {lastDigest}</div>}
              </div>
            )}
            {lastDeployment && (
              <div className={styles.resultCard} role="status" aria-label="Last deployment">
                <div className={styles.kv}>
                  <span className={styles.kvKey}>set</span>
                  <code>{lastDeployment.set}</code>
                  <span className={styles.kvKey}>version</span>
                  <span>{lastDeployment.version || '—'}</span>
                  <span className={styles.kvKey}>SHA-256</span>
                  <span className={styles.mono}>{lastDeployment.sha256}</span>
                  <span className={styles.kvKey}>graphs</span>
                  <span>{lastDeployment.graphs.join(', ')}</span>
                  <span className={styles.kvKey}>outcome</span>
                  <span>
                    {!lastDeployment.changed
                      ? 'already deployed with these bytes - nothing changed'
                      : lastDeployment.replaced
                        ? `replaced version ${lastDeployment.replaced.version || '(none)'} (${shortDigest(lastDeployment.replaced.sha256)})`
                        : 'deployed'}
                  </span>
                </div>
                {(lastDeployment.routes.length > 0 || lastDeployment.extensions.length > 0) && (
                  <>
                    <h3 className={styles.sectionTitle}>Advisory</h3>
                    <ul className={styles.entryList} aria-label="Advisory">
                      {lastDeployment.routes.map(row => (
                        <li key={`route:${row.graph}:${row.route}`} className={styles.entry}>
                          <div className={styles.entryMain}>
                            <code className={styles.entryId}>{row.route}</code>
                            <span className={styles.entryCounts}>
                              task route in {row.graph} · {row.status}{row.server ? ` via ${row.server}` : ''}
                            </span>
                          </div>
                        </li>
                      ))}
                      {lastDeployment.extensions.map(row => (
                        <li key={`ext:${row.graph}:${row.target}`} className={styles.entry}>
                          <div className={styles.entryMain}>
                            <code className={styles.entryId}>{row.target}</code>
                            <span className={styles.entryCounts}>
                              extension target outside the set, from {row.graph} · {row.status}
                            </span>
                          </div>
                        </li>
                      ))}
                    </ul>
                  </>
                )}
              </div>
            )}
            {!hotDeployOff && (
              <>
                <div className={styles.sectionHeader}>
                  <h3 className={styles.sectionTitle}>Deployed sets ({sets.length})</h3>
                  <button
                    type="button"
                    className={styles.linkButton}
                    onClick={() => { void refreshDeployed(); }}
                    disabled={isListing || busy}
                  >
                    {isListing ? 'Reading…' : 'Refresh'}
                  </button>
                </div>
                {listError && <div className={styles.errorBanner} role="alert">❌ {listError}</div>}
                {sets.length === 0 ? (
                  <p className={styles.empty}>No set is deployed in the hot folder of this engine.</p>
                ) : (
                  <table className={styles.table} aria-label="Deployed sets">
                    <thead>
                      <tr><th>Set</th><th>Version</th><th>SHA-256</th><th>Graphs</th><th>Status</th><th></th></tr>
                    </thead>
                    <tbody>
                      {sets.map(row => (
                        <tr key={row.set}>
                          <td><code>{row.set}</code></td>
                          <td>{row.version || '—'}</td>
                          <td className={styles.mono} title={row.sha256}>{shortDigest(row.sha256)}</td>
                          <td title={row.graphs.join(', ')}>{row.graphs.length}</td>
                          <td className={isDeployed(row) ? styles.statusDeployed : styles.statusRefused} title={row.deployedAt}>
                            {row.status}
                          </td>
                          <td>
                            {pendingUndeploy === row.set ? (
                              <span className={styles.confirmRow} role="group" aria-label={`Undeploy ${row.set}?`}>
                                <span>Undeploy {plural(row.graphs.length, 'graph')}?</span>
                                <button
                                  type="button"
                                  className={styles.entryAction}
                                  onClick={() => { void confirmUndeploy(); }}
                                  disabled={isUndeploying}
                                >
                                  {isUndeploying ? 'Undeploying…' : 'Yes, undeploy'}
                                </button>
                                <button type="button" className={styles.linkButton} onClick={cancelUndeploy} disabled={isUndeploying}>
                                  Keep
                                </button>
                              </span>
                            ) : (
                              <button
                                type="button"
                                className={styles.entryAction}
                                onClick={() => requestUndeploy(row.set)}
                                disabled={busy}
                                title={`Removes ${row.set} and its ${plural(row.graphs.length, 'graph')} from this engine's hot folder`}
                              >
                                Undeploy
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </>
            )}
          </div>
          <footer className={styles.footer}>
            <button type="button" className={styles.secondaryButton} onClick={leaveDeploy} disabled={busy}>
              Back
            </button>
            <div className={styles.footerActions}>
              <button type="button" className={styles.secondaryButton} onClick={close} disabled={busy}>
                Close
              </button>
            </div>
          </footer>
        </section>
      </div>
    );
  }

  return (
    <div className={styles.root}>
      <section className={styles.card} aria-label={TITLE}>
        <header className={styles.ribbon}>
          <div className={styles.titleGroup}>
            <span className={styles.title}>{TITLE}</span>
            <span className={styles.subtitle}>{fileName}</span>
          </div>
          <button
            type="button"
            className={styles.ribbonClose}
            onClick={close}
            aria-label="Close graph-set panel"
            title="Close (Esc)"
            disabled={busy}
          >
            <CloseIcon className={styles.ribbonCloseIcon} aria-hidden="true" focusable="false" />
          </button>
        </header>

        <div className={styles.body}>
          <p className={styles.description}>
            Graph models packed together as one set, <code>{fileName}</code>, checked by the deployment gate as they are
            packed. A set deploys all or none through <code>graphs.yaml</code>; a set of one graph is how one graph is signed.
          </p>

          {dropZone}
          <div className={styles.addRow}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={addCurrentGraph}
              disabled={!canAddCurrentGraph || busy}
              title={canAddCurrentGraph
                ? 'Adds the graph in the Graph view, named after its graph id'
                : 'Load a graph first'}
            >
              Add current graph
            </button>
            {entries.length > 0 && (
              <button type="button" className={styles.linkButton} onClick={clearEntries} disabled={busy}>
                Clear list
              </button>
            )}
          </div>
          {fileErrors.length > 0 && (
            <ul className={styles.fileErrors} role="alert">
              {fileErrors.map(error => <li key={error}>⚠️ {error}</li>)}
            </ul>
          )}

          <h3 className={styles.sectionTitle}>Graphs ({entries.length})</h3>
          {entries.length === 0 ? (
            <p className={styles.empty}>No graphs yet. Drop <code>&lt;graph-id&gt;.json</code> files or add the current graph.</p>
          ) : (
            <ul className={styles.entryList} aria-label="Graphs in the set">
              {entries.map(entry => {
                const issue = entryIssues.get(entry.key);
                return (
                  <li key={entry.key} className={`${styles.entry}${issue ? ` ${styles.entryFlagged}` : ''}`}>
                    <div className={styles.entryMain}>
                      <code className={styles.entryId}>{entry.id || '(no graph id)'}</code>
                      <span className={styles.entryCounts}>
                        {plural(entry.nodeCount, 'node')} · {plural(entry.connectionCount, 'connection')}
                        {entry.label !== `${entry.id}.json` && <> · {entry.label}</>}
                      </span>
                      {issue && <span className={styles.entryIssue} role="alert">⚠️ {issue}</span>}
                    </div>
                    <button
                      type="button"
                      className={styles.entryRemove}
                      onClick={() => removeEntry(entry.key)}
                      disabled={busy}
                      aria-label={`Remove ${entry.id || entry.label} from the set`}
                      title="Remove from the set"
                    >
                      ×
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          <h3 className={styles.sectionTitle}>Manifest</h3>
          <label className={styles.fieldLabel} htmlFor={setNameId}>Set name</label>
          <div className={styles.inputRow}>
            <input
              id={setNameId}
              ref={setNameInputRef}
              className={`${styles.input}${showSetNameError ? ` ${styles.inputInvalid}` : ''}`}
              type="text"
              value={setName}
              onChange={event => { setSetName(event.target.value); setSetNameTouched(true); }}
              onBlur={() => setSetNameTouched(true)}
              aria-describedby={setNameHintId}
              aria-invalid={showSetNameError}
              autoComplete="off"
              spellCheck={false}
              maxLength={80}
              disabled={busy}
            />
            <span className={styles.extension} aria-hidden="true">{PACK_EXTENSION}</span>
          </div>
          <p
            id={setNameHintId}
            className={`${styles.hint}${showSetNameError ? ` ${styles.hintError}` : ''}`}
            role={showSetNameError ? 'alert' : undefined}
          >
            {showSetNameError
              ? setNameError
              : `Required. It names the set and its file, ${fileName}.`}
          </p>

          <div className={styles.fieldLabel}>Fields</div>
          <div className={styles.rows} role="group" aria-label="Manifest fields">
            {rows.map(row => {
              const issue = rowIssues.get(row.key);
              return (
                <div key={row.key} className={styles.rowBlock}>
                  <div className={styles.row}>
                    <input
                      className={`${styles.input} ${styles.rowName}${issue ? ` ${styles.inputInvalid}` : ''}`}
                      type="text"
                      value={row.name}
                      onChange={event => updateRow(row.key, { name: event.target.value })}
                      placeholder="field"
                      aria-label="Manifest field name"
                      autoComplete="off"
                      spellCheck={false}
                      disabled={busy}
                    />
                    <input
                      className={`${styles.input} ${styles.rowValue}`}
                      type="text"
                      value={row.value}
                      onChange={event => updateRow(row.key, { value: event.target.value })}
                      placeholder="value"
                      aria-label={`Value of manifest field ${row.name || '(unnamed)'}`}
                      autoComplete="off"
                      disabled={busy}
                    />
                    <button
                      type="button"
                      className={styles.entryRemove}
                      onClick={() => removeRow(row.key)}
                      disabled={busy}
                      aria-label={`Remove manifest field ${row.name || '(unnamed)'}`}
                      title="Remove field"
                    >
                      ×
                    </button>
                  </div>
                  {issue && <span className={styles.entryIssue} role="alert">⚠️ {issue}</span>}
                </div>
              );
            })}
            <button type="button" className={styles.linkButton} onClick={addRow} disabled={busy}>
              + Add field
            </button>
          </div>
          <p className={styles.hint}>
            Optional text fields that travel in the manifest, such as <code>version</code> and <code>description</code>;
            a blank value is left out. <code>{GRAPH_ID_FIELD}</code> names the set's entry-point graph.
          </p>

          {packError && (
            <div className={styles.errorBanner} role="alert">
              ❌ {packError}
            </div>
          )}
        </div>

        <footer className={styles.footer}>
          <span className={styles.footerNote}>
            {supportsFolderPicker ? 'The next dialog picks the folder.' : 'The browser saves it to its download folder.'}
          </span>
          <div className={styles.footerActions}>
            <button type="button" className={styles.secondaryButton} onClick={close} disabled={busy}>
              Cancel
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={openDeploy}
              disabled={busy}
              title="Deploy a set to this engine without a restart, or see what is deployed"
            >
              Deploy…
            </button>
            <button
              type="button"
              className={styles.primaryButton}
              onClick={() => { setSetNameTouched(true); void pack(); }}
              disabled={!canPack}
              aria-busy={isPacking}
              title={canPack ? `Packs the set on the engine and saves ${fileName}` : 'Add a graph and name the set first'}
            >
              {isPacking ? (
                <><span className={styles.spinner} aria-hidden="true" /> Packing…</>
              ) : isReading ? (
                <><span className={styles.spinner} aria-hidden="true" /> Reading…</>
              ) : (
                'Pack and download'
              )}
            </button>
          </div>
        </footer>
      </section>
    </div>
  );
}
