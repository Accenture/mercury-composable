import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ToastType } from './useToast';
import type { MinigraphGraphData } from '../utils/graphTypes';
import { readFileAsText, validateJsonFileType } from '../utils/jsonFile';
import { buildGraphFileText, saveBinaryFile, suggestGraphId, validateGraphModel } from '../utils/graphFile';
import { responseErrorMessage } from '../utils/httpResponse';
import {
  DEPLOYED_PATH,
  DEPLOY_PATH,
  DIGEST_HEADER,
  PACK_PATH,
  SET_FIELD,
  SUGGESTED_MANIFEST_FIELDS,
  UNPACK_PATH,
  buildPackRequest,
  entryIdFromFileName,
  entryIssues as computeEntryIssues,
  isHotDeployOff,
  isPackFile,
  manifestRowIssues,
  manifestRowsOf,
  packFileName,
  parseDeployAnswer,
  parseDeployedList,
  parseUnpackAnswer,
  sha256Hex,
  undeployPath,
  validateSetName,
  type DeployedList,
  type DeploymentAnswer,
  type GraphSetEntry,
  type InspectedSet,
  type ManifestRow,
} from '../utils/graphSet';

/** The panel assembles a set, shows one it read back, or deploys to this engine (RFC-0017). */
export type GraphSetPanelMode = 'assemble' | 'inspect' | 'deploy';

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
  /** The package as dropped, so the inspect view can deploy it without a second drop. */
  file: File;
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
  // ── deploy mode (RFC-0017) ──
  /** Opens the deploy view and reads the deployed sets; what the engine answers decides what the view offers. */
  openDeploy: () => void;
  /** Back to the editor. */
  leaveDeploy: () => void;
  deployed: DeployedList | null;
  isListing: boolean;
  listError: string | null;
  /** True when the engine answered that hot deployment is off (503; or 404 on an application older than the entries). */
  hotDeployOff: boolean;
  refreshDeployed: () => Promise<void>;
  /** The digest the field's ledger holds, sent as the header when given; the engine refuses other bytes. */
  expectedDigest: string;
  setExpectedDigest: (digest: string) => void;
  /** Deploys a dropped package. */
  deployFile: (file: File) => Promise<void>;
  /** Opens the deploy view and deploys the inspected package. */
  deployInspected: () => Promise<void>;
  /** Packs the assembled set on the engine and deploys the bytes in one step. */
  packAndDeploy: () => Promise<void>;
  canPackAndDeploy: boolean;
  isDeploying: boolean;
  deployError: string | null;
  lastDeployment: DeploymentAnswer | null;
  /** The digest of the last package sent, computed in the browser, for the ledger. */
  lastDigest: string | null;
  /** The set an undeploy is waiting on its confirmation for. */
  pendingUndeploy: string | null;
  requestUndeploy: (set: string) => void;
  cancelUndeploy: () => void;
  confirmUndeploy: () => Promise<void>;
  isUndeploying: boolean;
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
 * <p>
 * The deploy view (RFC-0017, D17) is a client of the engine's three production entries: it deploys the
 * assembled set in one step (pack on the engine, then `POST /api/graph-set/deploy`) or a dropped `.pack`,
 * computes the package's SHA-256 in the browser so the digest can go to the field's ledger, sends an
 * expected digest as `X-Graph-Set-SHA256` when the user pastes one, lists the deployed sets
 * (`GET /api/graph-set/deployed`) and undeploys one behind a confirmation. When the engine's switches are
 * off, its entries answer 503 and the view says so, naming the two properties.
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
  const [deployed, setDeployed] = useState<DeployedList | null>(null);
  const [isListing, setIsListing] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [hotDeployOff, setHotDeployOff] = useState(false);
  const [expectedDigest, setExpectedDigest] = useState('');
  const [isDeploying, setIsDeploying] = useState(false);
  const [deployError, setDeployError] = useState<string | null>(null);
  const [lastDeployment, setLastDeployment] = useState<DeploymentAnswer | null>(null);
  const [lastDigest, setLastDigest] = useState<string | null>(null);
  const [pendingUndeploy, setPendingUndeploy] = useState<string | null>(null);
  const [isUndeploying, setIsUndeploying] = useState(false);

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
    setIsDeploying(false);
    setIsUndeploying(false);
    setPendingUndeploy(null);
    setMode('assemble');
    setInspected(null);
    setIsOpen(false);
  }, [abortInFlight]);

  // ── deploy mode: the three production entries, called like any client ──
  const refreshDeployed = useCallback(async () => {
    setIsListing(true);
    setListError(null);
    try {
      const response = await fetch(DEPLOYED_PATH, { headers: { 'Accept': 'application/json' } });
      if (!response.ok) {
        if (isHotDeployOff(response.status)) {
          setHotDeployOff(true);
          setDeployed(null);
        } else {
          setListError(`The deployed sets were not read - ${await responseErrorMessage(response)}`);
        }
        return;
      }
      setDeployed(parseDeployedList(await response.json()));
      setHotDeployOff(false);
    } catch (err) {
      setListError(`The deployed sets were not read: ${(err as Error).message}`);
    } finally {
      setIsListing(false);
    }
  }, []);

  const openDeploy = useCallback(() => {
    setMode('deploy');
    setDeployError(null);
    setLastDeployment(null);
    setLastDigest(null);
    setPendingUndeploy(null);
    setFileErrors([]);
    void refreshDeployed();
  }, [refreshDeployed]);

  const leaveDeploy = useCallback(() => {
    setPendingUndeploy(null);
    setMode('assemble');
  }, []);

  const deployBytes = useCallback(async (bytes: Uint8Array<ArrayBuffer>, label: string) => {
    abortInFlight();
    const controller = new AbortController();
    abortRef.current = controller;
    setDeployError(null);
    setIsDeploying(true);
    try {
      const digest = await sha256Hex(bytes);
      setLastDigest(digest);
      const headers: Record<string, string> = {
        'Content-Type': 'application/octet-stream',
        'Accept': 'application/json',
      };
      const expected = expectedDigest.trim();
      if (expected !== '') headers[DIGEST_HEADER] = expected;
      const response = await fetch(DEPLOY_PATH, { method: 'POST', headers, body: bytes, signal: controller.signal });
      if (!response.ok) {
        if (isHotDeployOff(response.status)) {
          setHotDeployOff(true);
          setDeployError('Hot deployment is off on this engine.');
        } else {
          setDeployError(`"${label}" was not deployed - ${await responseErrorMessage(response)}`);
        }
        return;
      }
      const deployment = parseDeployAnswer(await response.json());
      setLastDeployment(deployment);
      if (deployment.changed) {
        addToast(`Deployed ${deployment.set}${deployment.version ? ` ${deployment.version}` : ''}`, 'success');
      } else {
        addToast(`${deployment.set} was already deployed with these bytes`, 'info');
      }
      void refreshDeployed();
    } catch (err) {
      if ((err as Error).name === 'AbortError') return;
      setDeployError(`"${label}" was not deployed: ${(err as Error).message}`);
    } finally {
      if (abortRef.current === controller) abortRef.current = null;
      setIsDeploying(false);
    }
  }, [abortInFlight, expectedDigest, addToast, refreshDeployed]);

  const deployFile = useCallback(async (file: File) => {
    await deployBytes(new Uint8Array(await file.arrayBuffer()), file.name);
  }, [deployBytes]);

  const requestUndeploy = useCallback((set: string) => {
    setDeployError(null);
    setPendingUndeploy(set);
  }, []);
  const cancelUndeploy = useCallback(() => setPendingUndeploy(null), []);
  const confirmUndeploy = useCallback(async () => {
    const set = pendingUndeploy;
    if (!set) return;
    setIsUndeploying(true);
    setDeployError(null);
    try {
      const response = await fetch(undeployPath(set), { method: 'POST', headers: { 'Accept': 'application/json' } });
      if (!response.ok) {
        if (isHotDeployOff(response.status)) {
          setHotDeployOff(true);
          setDeployError('Hot undeploy is off on this engine.');
        } else {
          setDeployError(`${set} was not undeployed - ${await responseErrorMessage(response)}`);
        }
        return;
      }
      addToast(`Undeployed ${set}`, 'success');
      setLastDeployment(prev => (prev?.set === set ? null : prev));
      void refreshDeployed();
    } catch (err) {
      setDeployError(`${set} was not undeployed: ${(err as Error).message}`);
    } finally {
      setPendingUndeploy(null);
      setIsUndeploying(false);
    }
  }, [pendingUndeploy, addToast, refreshDeployed]);

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
      setInspected({ fileName: file.name, set, file });
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
      // in the deploy view a package is deployed, elsewhere it is read back and shown
      if (mode === 'deploy') await deployFile(packs[0]);
      else await inspect(packs[0]);
      return;
    }
    if (mode === 'deploy') {
      setFileErrors(['Drop a .pack to deploy it; graph files are packed in the editor first.']);
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
  }, [inspect, deployFile, mode, nextKey]);

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

  const canPackAndDeploy = canPack && !isDeploying && !isUndeploying && !hotDeployOff;
  const packAndDeploy = useCallback(async () => {
    if (!canPackAndDeploy) return;
    abortInFlight();
    const controller = new AbortController();
    abortRef.current = controller;
    setDeployError(null);
    setIsPacking(true);
    const name = setName.trim();
    let bytes: Uint8Array<ArrayBuffer> | null = null;
    try {
      const response = await fetch(PACK_PATH, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/octet-stream' },
        body:    JSON.stringify(buildPackRequest(name, rows, entries)),
        signal:  controller.signal,
      });
      if (!response.ok) {
        setDeployError(`${packFileName(name)} was not packed - ${await responseErrorMessage(response)}`);
        return;
      }
      bytes = new Uint8Array(await response.arrayBuffer());
    } catch (err) {
      if ((err as Error).name === 'AbortError') return;
      setDeployError(`Packing failed: ${(err as Error).message}`);
      return;
    } finally {
      if (abortRef.current === controller) abortRef.current = null;
      setIsPacking(false);
    }
    await deployBytes(bytes, packFileName(name));
  }, [canPackAndDeploy, abortInFlight, setName, rows, entries, deployBytes]);

  const deployInspected = useCallback(async () => {
    if (!inspected) return;
    const { file } = inspected;
    openDeploy();
    await deployFile(file);
  }, [inspected, openDeploy, deployFile]);

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
    isOpen, open, close, mode, busy: isPacking || isReading || isDeploying || isUndeploying,
    entries, entryIssues, addFiles, fileErrors, canAddCurrentGraph, addCurrentGraph, removeEntry, clearEntries,
    setName, setSetName, setNameError, rows, rowIssues, updateRow, addRow, removeRow,
    canPack, pack, isPacking, packError,
    inspected, isReading, importInspectedGraph, editInspectedSet, leaveInspect,
    openDeploy, leaveDeploy, deployed, isListing, listError, hotDeployOff, refreshDeployed,
    expectedDigest, setExpectedDigest, deployFile, deployInspected, packAndDeploy, canPackAndDeploy,
    isDeploying, deployError, lastDeployment, lastDigest,
    pendingUndeploy, requestUndeploy, cancelUndeploy, confirmUndeploy, isUndeploying,
  };
}
