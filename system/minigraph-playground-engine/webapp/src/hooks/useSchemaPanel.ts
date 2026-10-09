import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ToastType } from './useToast';
import type { ProtocolBus } from '../protocol/bus';
import type { MinigraphGraphData, MinigraphNode } from '../utils/graphTypes';
import { saveYamlFile, suggestGraphId } from '../utils/graphFile';
import { responseErrorMessage } from '../utils/httpResponse';
import { buildSchemaUpdateCommand } from '../graphActions/schemaCommand';
import {
  SCHEMA_NODE_ALIAS,
  buildPartSchema,
  buildSchemaProperty,
  declaredSchemaOf,
  mergeRunRows,
  parseContractView,
  rowsFromContractPart,
  rowsFromValue,
  schemaRowIssues,
  type ContractView,
  type SchemaPart,
  type SchemaRow,
  type SchemaSide,
} from '../utils/graphSchema';

/** The dev-mode endpoints of RFC-0007 WP1 and the instance inspector the panel reads. */
export function contractPath(sessionId: string): string {
  return `/api/openapi/session/${sessionId}?view=contract`;
}
export function documentPath(sessionId: string): string {
  return `/api/openapi/session/${sessionId}`;
}
export function inspectPath(sessionId: string, key: string): string {
  return `/api/inspect/${sessionId}/${key}`;
}

/** How long a Save waits for the engine's `node <alias> updated` before it is reported as unanswered. */
export const SCHEMA_SAVE_TIMEOUT_MS = 10_000;

export type SchemaRowsKey = `${SchemaSide}.${SchemaPart}`;
export const ROWS_KEYS: readonly SchemaRowsKey[] = ['input.body', 'input.header', 'output.body', 'output.header'];

export function rowsKey(side: SchemaSide, part: SchemaPart): SchemaRowsKey {
  return `${side}.${part}`;
}

type RowSets = Record<SchemaRowsKey, SchemaRow[]>;

const EMPTY_ROWS: RowSets = { 'input.body': [], 'input.header': [], 'output.body': [], 'output.header': [] };
const NOT_DIRTY: Record<SchemaSide, boolean> = { input: false, output: false };

export interface UseSchemaPanelOptions {
  addToast: (message: string, type?: ToastType) => void;
  /** The session whose draft the panel describes, or null until the session id is known. */
  sessionId: string | null;
  connected: boolean;
  /** The live graph: the root and end nodes carry the declaration the panel edits. */
  graphData: MinigraphGraphData | null;
  /** The graph's display name, for the downloaded document's file name. */
  graphName: string | null;
  bus: ProtocolBus;
  /** The raw command transport: a Save is one `update node` the engine replays to every member. */
  sendRawText: (text: string) => boolean;
}

export interface UseSchemaPanelReturn {
  /** True while the panel occupies the left slot; unsaved rows survive a close. */
  isOpen: boolean;
  open: (side?: SchemaSide) => void;
  close: () => void;
  side: SchemaSide;
  setSide: (side: SchemaSide) => void;
  /** True while a request is in flight: Escape and the close button wait, like every in-place editor. */
  busy: boolean;
  /** The engine's contract view of the draft, or null before the first load. */
  contract: ContractView | null;
  isLoading: boolean;
  loadError: string | null;
  /** Reload the rows from the engine, discarding unsaved edits. */
  reload: () => void;
  rows: RowSets;
  issues: Record<SchemaRowsKey, Map<number, string>>;
  dirty: Record<SchemaSide, boolean>;
  updateRow: (key: SchemaRowsKey, rowKey: number, patch: Partial<Omit<SchemaRow, 'key'>>) => void;
  addRow: (key: SchemaRowsKey) => void;
  removeRow: (key: SchemaRowsKey, rowKey: number) => void;
  /** The node the side's declaration lives on (`root` or `end`), or null when the graph lacks it. */
  nodeOf: (side: SchemaSide) => MinigraphNode | null;
  /** The declaration-versus-model issues of one side, as the engine states them. */
  issuesOf: (side: SchemaSide) => string[];
  /** Types and examples from the instance's actual values, through the inspect endpoint. */
  fillFromRun: () => Promise<void>;
  isFilling: boolean;
  /** Writes the current side's declaration through `update node`; the engine's reply closes the save. */
  save: () => void;
  canSave: boolean;
  isSaving: boolean;
  saveError: string | null;
  /** Fetches the draft's OpenAPI document and saves it as `<graph>.yaml`. */
  download: () => Promise<void>;
  isDownloading: boolean;
}

interface PendingSave {
  side: SchemaSide;
  alias: string;
  timer: ReturnType<typeof setTimeout>;
}

function fileNameOf(response: Response, fallback: string): string {
  const disposition = response.headers.get('Content-Disposition') ?? '';
  const match = disposition.match(/filename="?([^";]+)"?/);
  return match ? match[1] : fallback;
}

/**
 * The Schema panel (RFC-0007, WP3): the graph contract as rows - one per body path, one per header
 * name, on the Input (root) and Output (end) tabs - pre-filled from the engine's contract view of
 * the session's draft (discovery merged with the declaration), typed further from the instance's
 * last run, and saved through the existing `update node` command so every member of the session
 * sees the result and an agent does the same by command. The document is downloaded from the
 * draft route. The rows survive a close; a graph mutation reloads them unless they are being edited.
 */
export function useSchemaPanel({
  addToast,
  sessionId,
  connected,
  graphData,
  graphName,
  bus,
  sendRawText,
}: UseSchemaPanelOptions): UseSchemaPanelReturn {
  const keyRef = useRef(1);
  const nextKey = useCallback(() => keyRef.current++, []);

  const [isOpen, setIsOpen] = useState(false);
  const [side, setSide] = useState<SchemaSide>('input');
  const [contract, setContract] = useState<ContractView | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [rows, setRows] = useState<RowSets>(EMPTY_ROWS);
  const [dirty, setDirty] = useState<Record<SchemaSide, boolean>>(NOT_DIRTY);
  const [isFilling, setIsFilling] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [isDownloading, setIsDownloading] = useState(false);

  const graphDataRef = useRef(graphData);
  graphDataRef.current = graphData;
  const rowsRef = useRef(rows);
  rowsRef.current = rows;
  const dirtyRef = useRef(dirty);
  dirtyRef.current = dirty;
  const isOpenRef = useRef(isOpen);
  isOpenRef.current = isOpen;
  const pendingRef = useRef<PendingSave | null>(null);
  const loadAbortRef = useRef<AbortController | null>(null);

  const nodeOf = useCallback((which: SchemaSide): MinigraphNode | null => {
    return graphData?.nodes.find(n => n.alias === SCHEMA_NODE_ALIAS[which]) ?? null;
  }, [graphData]);

  const clearPending = useCallback(() => {
    if (pendingRef.current) {
      clearTimeout(pendingRef.current.timer);
      pendingRef.current = null;
    }
    setIsSaving(false);
  }, []);

  // ── Load ─────────────────────────────────────────────────────────────────
  const load = useCallback(async (discardEdits = false) => {
    if (sessionId === null) {
      setLoadError('The session id is not known yet.');
      return;
    }
    loadAbortRef.current?.abort();
    const controller = new AbortController();
    loadAbortRef.current = controller;
    setIsLoading(true);
    setLoadError(null);
    try {
      const response = await fetch(contractPath(sessionId), { headers: { Accept: 'application/json' }, signal: controller.signal });
      if (!response.ok) {
        setLoadError(await responseErrorMessage(response));
        return;
      }
      const view = parseContractView(await response.json());
      // rows edited while the read was in flight win over the stale answer - unless the user
      // asked for the reload, which discards them by design
      if (!discardEdits && (dirtyRef.current.input || dirtyRef.current.output)) return;
      const graph = graphDataRef.current;
      const root = declaredSchemaOf(graph?.nodes.find(n => n.alias === SCHEMA_NODE_ALIAS.input));
      const end = declaredSchemaOf(graph?.nodes.find(n => n.alias === SCHEMA_NODE_ALIAS.output));
      const part = (schema: Record<string, unknown> | undefined, name: SchemaPart) =>
        schema && typeof schema[name] === 'object' && schema[name] !== null ? schema[name] as Record<string, unknown> : undefined;
      setContract(view);
      setRows({
        'input.body': rowsFromContractPart(view.input.body, part(root, 'body'), 'body', 'input.body', nextKey),
        'input.header': rowsFromContractPart(view.input.header, part(root, 'header'), 'header', 'input.header', nextKey),
        'output.body': rowsFromContractPart(view.output.body, part(end, 'body'), 'body', 'output.body', nextKey),
        'output.header': rowsFromContractPart(view.output.header, part(end, 'header'), 'header', 'output.header', nextKey),
      });
      setDirty(NOT_DIRTY);
      setSaveError(null);
    } catch (err) {
      if ((err as Error).name === 'AbortError') return;
      setLoadError(`The contract could not be read: ${(err as Error).message}`);
    } finally {
      if (loadAbortRef.current === controller) loadAbortRef.current = null;
      setIsLoading(false);
    }
  }, [sessionId, nextKey]);
  const loadRef = useRef(load);
  loadRef.current = load;

  useEffect(() => () => { loadAbortRef.current?.abort(); }, []);

  // A new session is a different draft: forget what the panel held.
  useEffect(() => {
    setContract(null);
    setRows(EMPTY_ROWS);
    setDirty(NOT_DIRTY);
    setLoadError(null);
    clearPending();
  }, [sessionId, clearPending]);

  const open = useCallback((which: SchemaSide = 'input') => {
    setSide(which);
    setIsOpen(true);
    const d = dirtyRef.current;
    if (!d.input && !d.output) void loadRef.current();
  }, []);

  const close = useCallback(() => {
    loadAbortRef.current?.abort();
    loadAbortRef.current = null;
    setIsLoading(false);
    setIsOpen(false);
  }, []);

  const reload = useCallback(() => { void loadRef.current(true); }, []);

  // A graph mutation - another member's edit, or our own save - changes the contract: reload
  // unless rows are being edited, which the user resolves with Reload or Save.
  useEffect(() => {
    return bus.on('graph.mutation', () => {
      if (!isOpenRef.current) return;
      const d = dirtyRef.current;
      if (d.input || d.output) return;
      if (pendingRef.current) return;      // the save's own reply reloads
      void loadRef.current();
    });
  }, [bus]);

  // ── Rows ─────────────────────────────────────────────────────────────────
  const markDirty = useCallback((key: SchemaRowsKey) => {
    const which: SchemaSide = key.startsWith('input') ? 'input' : 'output';
    setDirty(prev => (prev[which] ? prev : { ...prev, [which]: true }));
    setSaveError(null);
  }, []);

  const updateRow = useCallback((key: SchemaRowsKey, rowKey: number, patch: Partial<Omit<SchemaRow, 'key'>>) => {
    setRows(prev => ({ ...prev, [key]: prev[key].map(r => (r.key === rowKey ? { ...r, ...patch } : r)) }));
    markDirty(key);
  }, [markDirty]);

  const addRow = useCallback((key: SchemaRowsKey) => {
    const type = key.endsWith('header') ? 'string' : '';
    setRows(prev => ({
      ...prev,
      [key]: [...prev[key], {
        key: nextKey(), path: '', type, itemType: '', required: false, description: '', example: '',
        source: 'new', usedBy: [], constraints: {},
      }],
    }));
    markDirty(key);
  }, [nextKey, markDirty]);

  const removeRow = useCallback((key: SchemaRowsKey, rowKey: number) => {
    setRows(prev => ({ ...prev, [key]: prev[key].filter(r => r.key !== rowKey) }));
    markDirty(key);
  }, [markDirty]);

  const issues = useMemo(() => ({
    'input.body': schemaRowIssues(rows['input.body'], 'body'),
    'input.header': schemaRowIssues(rows['input.header'], 'header'),
    'output.body': schemaRowIssues(rows['output.body'], 'body'),
    'output.header': schemaRowIssues(rows['output.header'], 'header'),
  }), [rows]);

  const issuesOf = useCallback((which: SchemaSide): string[] => {
    return (contract?.issues ?? []).filter(issue => issue.startsWith(`${which}.`));
  }, [contract]);

  // ── Fill from last run ───────────────────────────────────────────────────
  const fillFromRun = useCallback(async () => {
    if (sessionId === null) {
      addToast('The session id is not known yet.', 'error');
      return;
    }
    setIsFilling(true);
    try {
      const values = await Promise.all(ROWS_KEYS.map(async key => {
        const response = await fetch(inspectPath(sessionId, key), { headers: { Accept: 'application/json' } });
        if (response.status === 404) return null;
        if (!response.ok) throw new Error(await responseErrorMessage(response));
        const answer = await response.json() as { outcome?: unknown };
        return answer.outcome ?? null;
      }));
      if (values.every(value => value === null)) {
        addToast('No instance data yet - Instantiate, Upload and Run first.', 'info');
        return;
      }
      // merged against the rows as they are now (a ref: the updater form runs later, in the render)
      let changed = 0;
      const touched: Record<SchemaSide, boolean> = { input: false, output: false };
      const next = { ...rowsRef.current };
      ROWS_KEYS.forEach((key, index) => {
        const value = values[index];
        if (value === null) return;
        const part: SchemaPart = key.endsWith('header') ? 'header' : 'body';
        const outcome = mergeRunRows(next[key], rowsFromValue(value, part, nextKey));
        if (outcome.changed > 0) {
          next[key] = outcome.rows;
          changed += outcome.changed;
          touched[key.startsWith('input') ? 'input' : 'output'] = true;
        }
      });
      if (changed === 0) {
        addToast('The last run adds nothing: every row already has its type and example.', 'info');
      } else {
        setRows(next);
        setDirty(prev => ({ input: prev.input || touched.input, output: prev.output || touched.output }));
        setSaveError(null);
        addToast(`${changed} row${changed === 1 ? '' : 's'} filled from the last run - review and Save.`, 'success');
      }
    } catch (err) {
      addToast(`Fill from last run failed: ${(err as Error).message}`, 'error');
    } finally {
      setIsFilling(false);
    }
  }, [sessionId, addToast, nextKey]);

  // ── Save ─────────────────────────────────────────────────────────────────
  const sideIssueCount = issues[rowsKey(side, 'body')].size + issues[rowsKey(side, 'header')].size;
  const canSave = connected && dirty[side] && sideIssueCount === 0 && !isSaving && !isLoading && nodeOf(side) !== null;

  const save = useCallback(() => {
    if (!canSave) return;
    const node = nodeOf(side);
    if (!node) return;
    let command: string;
    try {
      const schema = buildSchemaProperty(
        buildPartSchema(rows[rowsKey(side, 'body')], 'body'),
        buildPartSchema(rows[rowsKey(side, 'header')], 'header'),
      );
      command = buildSchemaUpdateCommand(node, schema);
    } catch (err) {
      setSaveError((err as Error).message);
      return;
    }
    setSaveError(null);
    if (!sendRawText(command)) {
      setSaveError('The command was not sent because the WebSocket is not open. Your rows are kept.');
      return;
    }
    setIsSaving(true);
    pendingRef.current = {
      side,
      alias: node.alias,
      timer: setTimeout(() => {
        pendingRef.current = null;
        setIsSaving(false);
        setSaveError(`The engine did not answer within ${SCHEMA_SAVE_TIMEOUT_MS / 1000} s - check the console.`);
      }, SCHEMA_SAVE_TIMEOUT_MS),
    };
  }, [canSave, nodeOf, side, rows, sendRawText]);

  // The engine's reply to our `update node` closes the save - the same text result the node editor waits for.
  useEffect(() => {
    return bus.on('minigraph.nodeAction.textResult', event => {
      const pending = pendingRef.current;
      if (!pending) return;
      const ours = event.action === 'edit-node' && event.alias === pending.alias;
      const generic = event.status === 'error' && event.action === null;
      if (!ours && !generic) return;
      clearPending();
      if (event.status === 'accepted') {
        setDirty(prev => ({ ...prev, [pending.side]: false }));
        addToast(`Schema of node ${pending.alias} saved`, 'success');
        void loadRef.current();
      } else {
        setSaveError(event.message);
      }
    });
  }, [bus, addToast, clearPending]);

  useEffect(() => () => { if (pendingRef.current) clearTimeout(pendingRef.current.timer); }, []);

  // ── Download ─────────────────────────────────────────────────────────────
  const download = useCallback(async () => {
    if (sessionId === null) {
      addToast('The session id is not known yet.', 'error');
      return;
    }
    setIsDownloading(true);
    try {
      const response = await fetch(documentPath(sessionId), { headers: { Accept: 'application/yaml' } });
      if (!response.ok) {
        addToast(`The document could not be read - ${await responseErrorMessage(response)}`, 'error');
        return;
      }
      const text = await response.text();
      const outcome = await saveYamlFile(text, fileNameOf(response, `${suggestGraphId(graphName)}.yaml`));
      if (outcome.saved) addToast(`OpenAPI document saved as ${outcome.fileName}`, 'success');
    } catch (err) {
      addToast(`Download failed: ${(err as Error).message}`, 'error');
    } finally {
      setIsDownloading(false);
    }
  }, [sessionId, graphName, addToast]);

  return {
    isOpen, open, close, side, setSide,
    busy: isSaving || isFilling || isDownloading,
    contract, isLoading, loadError, reload,
    rows, issues, dirty, updateRow, addRow, removeRow,
    nodeOf, issuesOf,
    fillFromRun, isFilling,
    save, canSave, isSaving, saveError,
    download, isDownloading,
  };
}
