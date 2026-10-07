import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ToastType } from './useToast';
import type { MinigraphGraphData } from '../utils/graphTypes';
import { readFileAsText, validateJsonFileType } from '../utils/jsonFile';
import { buildGraphFileText, saveBinaryFile, suggestGraphId, validateGraphModel } from '../utils/graphFile';
import { responseErrorMessage } from '../utils/httpResponse';
import {
  PACK_PATH,
  SET_FIELD,
  SUGGESTED_MANIFEST_FIELDS,
  UNPACK_PATH,
  buildPackRequest,
  entryIdFromFileName,
  entryIssues as computeEntryIssues,
  isPackFile,
  manifestRowIssues,
  manifestRowsOf,
  packFileName,
  parseUnpackAnswer,
  validateSetName,
  type GraphSetEntry,
  type InspectedSet,
  type ManifestRow,
} from '../utils/graphSet';

/** The panel assembles a set, or shows one it read back. */
export type GraphSetPanelMode = 'assemble' | 'inspect';

export interface UseGraphSetPanelOptions {
  addToast: (message: string, type?: ToastType) => void;
  /**
   * Imports one graph file as the session's draft through the file-import path, which validates
   * it, asks before replacing a loaded graph and posts it (`useGraphFileImport.importFiles`).
   */
  importFiles: (files: File[]) => Promise<void>;
  /** The live graph, for "Add current graph"; null while none is loaded. */
  graphData: MinigraphGraphData | null;
  /** The live graph's display name: its id in the set, after `suggestGraphId`. */
  graphName: string | null;
}

export interface InspectedFile {
  fileName: string;
  set: InspectedSet;
}

export interface UseGraphSetPanelReturn {
  /** True while the panel occupies the left slot; the entries survive a close. */
  isOpen: boolean;
  open: () => void;
  /** Closes the panel (a request in flight is aborted) and leaves inspect mode. */
  close: () => void;
  mode: GraphSetPanelMode;
  /** True while a pack or a read is in flight: Escape and the close button wait, like every in-place editor. */
  busy: boolean;
  // ── assemble mode ──
  entries: GraphSetEntry[];
  /** Per entry key: why the engine would refuse it, to fix before packing. */
  entryIssues: Map<number, string>;
  /** Validate dropped or picked files: `.json` graph models become entries; one `.pack` is read back instead. */
  addFiles: (files: FileList | File[]) => Promise<void>;
  /** The errors of the last drop or pick, one per file that was not taken. */
  fileErrors: string[];
  canAddCurrentGraph: boolean;
  /** The live graph as an entry named after its display name; a re-add replaces the earlier copy. */
  addCurrentGraph: () => void;
  removeEntry: (key: number) => void;
  clearEntries: () => void;
  setName: string;
  setSetName: (name: string) => void;
  setNameError: string | null;
  rows: ManifestRow[];
  rowIssues: Map<number, string>;
  updateRow: (key: number, patch: Partial<Pick<ManifestRow, 'name' | 'value'>>) => void;
  addRow: () => void;
  removeRow: (key: number) => void;
  canPack: boolean;
  /** Packs on the engine and saves `<set>.pack`; a refused set is reported in `packError`. */
  pack: () => Promise<void>;
  isPacking: boolean;
  packError: string | null;
  // ── inspect mode ──
  inspected: InspectedFile | null;
  isReading: boolean;
  /** Imports one graph of the inspected set as the session's draft. */
  importInspectedGraph: (id: string) => Promise<void>;
  /** Loads the inspected set's graphs and manifest into the editor, replacing the list, to pack it again. */
  editInspectedSet: () => void;
  /** Back to the editor, keeping its entries. */
  leaveInspect: () => void;
}

function suggestedRows(nextKey: () => number): ManifestRow[] {
  return SUGGESTED_MANIFEST_FIELDS.map(name => ({ key: nextKey(), name, value: '' }));
}

/**
 * The "Graph set packaging" panel: assembles a graph set from `.json` graph files and the current graph,
 * with a manifest, and has the engine pack it (`POST /api/graph-set/pack` - the gate runs there, D2
 * of ADR-0027) before the bytes are saved as `<set>.pack`. A dropped `.pack` is read back through
 * `POST /api/graph-set/unpack` and shown - its manifest and its graphs, each importable as the
 * draft (D9). The entries and the manifest survive a close, so the panel can be put away while a
 * graph is edited and re-opened to add it.
 */
export function useGraphSetPanel({
  addToast,
  importFiles,
  graphData,
  graphName,
}: UseGraphSetPanelOptions): UseGraphSetPanelReturn {
  const keyRef = useRef(1);
  const nextKey = useCallback(() => keyRef.current++, []);

  const [isOpen, setIsOpen] = useState(false);
  const [mode, setMode] = useState<GraphSetPanelMode>('assemble');
  const [entries, setEntries] = useState<GraphSetEntry[]>([]);
  const [fileErrors, setFileErrors] = useState<string[]>([]);
  const [setName, setSetName] = useState('');
  const [rows, setRows] = useState<ManifestRow[]>(() => suggestedRows(nextKey));
  const [isPacking, setIsPacking] = useState(false);
  const [packError, setPackError] = useState<string | null>(null);
  const [inspected, setInspected] = useState<InspectedFile | null>(null);
  const [isReading, setIsReading] = useState(false);

  const abortRef = useRef<AbortController | null>(null);
  const abortInFlight = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
  }, []);
  useEffect(() => abortInFlight, [abortInFlight]);

  const entryIssues = useMemo(() => computeEntryIssues(entries), [entries]);
  const entryIds = useMemo(() => new Set(entries.map(entry => entry.id)), [entries]);
  const rowIssues = useMemo(() => manifestRowIssues(rows, entryIds), [rows, entryIds]);
  const setNameError = useMemo(() => validateSetName(setName), [setName]);
  const canPack = entries.length > 0 && entryIssues.size === 0 && rowIssues.size === 0
    && setNameError === null && !isPacking && !isReading;

  const open = useCallback(() => setIsOpen(true), []);
  const close = useCallback(() => {
    abortInFlight();
    setIsPacking(false);
    setIsReading(false);
    setMode('assemble');
    setInspected(null);
    setIsOpen(false);
  }, [abortInFlight]);

  const inspect = useCallback(async (file: File) => {
    abortInFlight();
    const controller = new AbortController();
    abortRef.current = controller;
    setIsReading(true);
    try {
      const response = await fetch(UNPACK_PATH, {
        method:  'POST',
        headers: { 'Content-Type': 'application/octet-stream', 'Accept': 'application/json' },
        body:    file,
        signal:  controller.signal,
      });
      if (!response.ok) {
        setFileErrors([`"${file.name}" was not read - ${await responseErrorMessage(response)}`]);
        return;
      }
      const set = parseUnpackAnswer(await response.json());
      setInspected({ fileName: file.name, set });
      setMode('inspect');
    } catch (err) {
      if ((err as Error).name === 'AbortError') return;
      setFileErrors([`"${file.name}" was not read: ${(err as Error).message}`]);
    } finally {
      if (abortRef.current === controller) abortRef.current = null;
      setIsReading(false);
    }
  }, [abortInFlight]);

  const addFiles = useCallback(async (files: FileList | File[]) => {
    const list = Array.from(files);
    if (list.length === 0) return;
    setPackError(null);
    const packs = list.filter(isPackFile);
    if (packs.length > 0) {
      if (list.length > 1) {
        setFileErrors(['Drop one .pack file on its own to inspect it, or .json graph files to pack.']);
        return;
      }
      setFileErrors([]);
      await inspect(packs[0]);
      return;
    }
    const errors: string[] = [];
    const added: GraphSetEntry[] = [];
    for (const file of list) {
      const typeError = validateJsonFileType(file);
      if (typeError) {
        errors.push(typeError);
        continue;
      }
      let parsed: unknown;
      try {
        parsed = JSON.parse(await readFileAsText(file));
      } catch (err) {
        errors.push(`"${file.name}" is not valid JSON: ${(err as Error).message}`);
        continue;
      }
      const result = validateGraphModel(parsed);
      if (!result.ok) {
        errors.push(`"${file.name}" is not a graph model: ${result.error}`);
        continue;
      }
      added.push({
        key: nextKey(),
        id: entryIdFromFileName(file.name),
        label: file.name,
        model: result.model,
        name: result.name,
        nodeCount: result.nodeCount,
        connectionCount: result.connectionCount,
        source: 'file',
      });
    }
    setFileErrors(errors);
    if (added.length > 0) setEntries(prev => [...prev, ...added]);
  }, [inspect, nextKey]);

  const canAddCurrentGraph = graphData !== null;
  const addCurrentGraph = useCallback(() => {
    if (!graphData) return;
    const id = suggestGraphId(graphName);
    // the file-download shape: the root node named after the id, the live graph untouched
    const result = validateGraphModel(JSON.parse(buildGraphFileText(graphData, id)));
    if (!result.ok) {
      addToast(`The current graph cannot be packed: ${result.error}`, 'error');
      return;
    }
    const entry: GraphSetEntry = {
      key: nextKey(),
      id,
      label: 'current graph',
      model: result.model,
      name: result.name,
      nodeCount: result.nodeCount,
      connectionCount: result.connectionCount,
      source: 'draft',
    };
    setPackError(null);
    setEntries(prev => {
      const index = prev.findIndex(existing => existing.source === 'draft' && existing.id === id);
      if (index < 0) return [...prev, entry];
      const next = [...prev];
      next[index] = entry;
      return next;
    });
  }, [graphData, graphName, nextKey, addToast]);

  const removeEntry = useCallback((key: number) => {
    setPackError(null);
    setEntries(prev => prev.filter(entry => entry.key !== key));
  }, []);
  const clearEntries = useCallback(() => {
    setPackError(null);
    setFileErrors([]);
    setEntries([]);
  }, []);

  const updateRow = useCallback((key: number, patch: Partial<Pick<ManifestRow, 'name' | 'value'>>) => {
    setPackError(null);
    setRows(prev => prev.map(row => (row.key === key ? { ...row, ...patch } : row)));
  }, []);
  const addRow = useCallback(() => {
    setRows(prev => [...prev, { key: nextKey(), name: '', value: '' }]);
  }, [nextKey]);
  const removeRow = useCallback((key: number) => {
    setPackError(null);
    setRows(prev => prev.filter(row => row.key !== key));
  }, []);

  const pack = useCallback(async () => {
    if (!canPack) return;
    abortInFlight();
    const controller = new AbortController();
    abortRef.current = controller;
    setPackError(null);
    setIsPacking(true);
    const name = setName.trim();
    try {
      const response = await fetch(PACK_PATH, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/octet-stream' },
        body:    JSON.stringify(buildPackRequest(name, rows, entries)),
        signal:  controller.signal,
      });
      if (!response.ok) {
        setPackError(await responseErrorMessage(response));
        return;
      }
      const bytes = new Uint8Array(await response.arrayBuffer());
      const outcome = await saveBinaryFile(bytes, packFileName(name));
      if (outcome.saved) {
        addToast(`Graph set saved as ${outcome.fileName}`, 'success');
      }
    } catch (err) {
      if ((err as Error).name === 'AbortError') return;
      setPackError(`Packing failed: ${(err as Error).message}`);
    } finally {
      if (abortRef.current === controller) abortRef.current = null;
      setIsPacking(false);
    }
  }, [canPack, abortInFlight, setName, rows, entries, addToast]);

  const importInspectedGraph = useCallback(async (id: string) => {
    const graph = inspected?.set.graphs.find(candidate => candidate.id === id);
    if (!graph) return;
    const file = new File([JSON.stringify(graph.model)], `${id}.json`, { type: 'application/json' });
    await importFiles([file]);
  }, [inspected, importFiles]);

  const editInspectedSet = useCallback(() => {
    if (!inspected) return;
    const { fileName, set } = inspected;
    setEntries(set.graphs.map(graph => ({
      key: nextKey(),
      id: graph.id,
      label: `${graph.id}.json from ${fileName}`,
      model: graph.model,
      name: graph.name,
      nodeCount: graph.nodeCount,
      connectionCount: graph.connectionCount,
      source: 'set',
    })));
    const manifestRows = manifestRowsOf(set.manifest, nextKey);
    setRows(manifestRows.length > 0 ? manifestRows : suggestedRows(nextKey));
    setSetName(set.manifest[SET_FIELD] ?? '');
    setFileErrors([]);
    setPackError(null);
    setInspected(null);
    setMode('assemble');
    const graphs = set.graphs.length;
    addToast(`Editing ${fileName}: ${graphs} graph${graphs === 1 ? '' : 's'} and ${manifestRows.length} manifest field${manifestRows.length === 1 ? '' : 's'} loaded`, 'info');
  }, [inspected, nextKey, addToast]);

  const leaveInspect = useCallback(() => {
    setInspected(null);
    setMode('assemble');
  }, []);

  return {
    isOpen, open, close, mode, busy: isPacking || isReading,
    entries, entryIssues, addFiles, fileErrors, canAddCurrentGraph, addCurrentGraph, removeEntry, clearEntries,
    setName, setSetName, setNameError, rows, rowIssues, updateRow, addRow, removeRow,
    canPack, pack, isPacking, packError,
    inspected, isReading, importInspectedGraph, editInspectedSet, leaveInspect,
  };
}
