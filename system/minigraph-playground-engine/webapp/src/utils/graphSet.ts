import { GRAPH_ID_RULE, isValidGraphId, validateGraphModel } from './graphFile';

/** The dev-mode endpoints of ADR-0027 WP4: the engine packs and reads a graph set. */
export const PACK_PATH = '/api/graph-set/pack';
export const UNPACK_PATH = '/api/graph-set/unpack';

/** A graph set's file is `<set>.pack`. */
export const PACK_EXTENSION = '.pack';
const JSON_EXTENSION = '.json';

/** Manifest fields the engine writes itself; the panel refuses them as caller fields. */
export const SET_FIELD = 'set';
export const GRAPH_ID_FIELD = 'graph_id';
const PACKAGER_FIELDS = new Set(['format', 'format_version']);

/** Fields the manifest editor suggests with an empty value; a blank value is never sent. */
export const SUGGESTED_MANIFEST_FIELDS = ['version', 'description'] as const;

/** Where an entry came from - the list says so, and the current graph may be re-added over itself. */
export type GraphSetEntrySource = 'file' | 'draft' | 'set';

export interface GraphSetEntry {
  /** A stable row key: duplicates share an id but never a key. */
  key: number;
  /** The graph id, which names the entry `<graph-id>.json` in the set. */
  id: string;
  /** What the entry is shown as: the file name, or the graph's name for the current draft. */
  label: string;
  /** The model exactly as read; it is sent to the engine unchanged. */
  model: Record<string, unknown>;
  /** The root node's "name" property, when it has one. */
  name: string | null;
  nodeCount: number;
  connectionCount: number;
  source: GraphSetEntrySource;
}

export interface ManifestRow {
  key: number;
  name: string;
  value: string;
}

/** The graph id a `<graph-id>.json` file name carries, or '' when the file is not a `.json`. */
export function entryIdFromFileName(fileName: string): string {
  const trimmed = fileName.trim();
  if (!trimmed.toLowerCase().endsWith(JSON_EXTENSION)) return '';
  return trimmed.slice(0, trimmed.length - JSON_EXTENSION.length);
}

/** True for a `.pack` file, by its extension - the drop zone switches to inspect mode on one. */
export function isPackFile(file: File): boolean {
  return file.name.toLowerCase().endsWith(PACK_EXTENSION);
}

export function packFileName(setName: string): string {
  return `${setName}${PACK_EXTENSION}`;
}

/** The set name is required and names the file, so it follows the engine's file-name rule. */
export function validateSetName(setName: string): string | null {
  const trimmed = setName.trim();
  if (trimmed === '') return 'A set name is required - it names the set and its file.';
  if (!isValidGraphId(trimmed)) return `A set name uses ${GRAPH_ID_RULE}.`;
  return null;
}

/**
 * What the engine would refuse an entry for, flagged in place so the list can be fixed before
 * packing: an id that breaks the file-name rule, a root name that differs from the id (the engine's
 * D6 rule: the root node is named after the graph id), and an id used by more than one entry.
 */
export function entryIssues(entries: readonly GraphSetEntry[]): Map<number, string> {
  const issues = new Map<number, string>();
  const byId = new Map<string, number>();
  for (const entry of entries) {
    byId.set(entry.id, (byId.get(entry.id) ?? 0) + 1);
  }
  for (const entry of entries) {
    if (!isValidGraphId(entry.id)) {
      issues.set(entry.key, `The graph id uses ${GRAPH_ID_RULE}; name the file <graph-id>.json.`);
    } else if (entry.name !== null && entry.name !== entry.id) {
      issues.set(entry.key, `The root node's name "${entry.name}" differs from the graph id.`);
    } else if ((byId.get(entry.id) ?? 0) > 1) {
      issues.set(entry.key, `Duplicate graph id "${entry.id}" - remove one.`);
    }
  }
  return issues;
}

/**
 * The manifest rows' problems, keyed by row: a row with a blank name and a blank value is ignored,
 * a value needs a name, the fields the engine writes itself are refused (as the engine refuses
 * them), a name is used once, and `graph_id` names a graph of the set. A named row with a blank
 * value is not sent, so it is not an error: the suggested rows start that way.
 */
export function manifestRowIssues(rows: readonly ManifestRow[], entryIds: ReadonlySet<string>): Map<number, string> {
  const issues = new Map<number, string>();
  const seen = new Map<string, number>();
  for (const row of rows) {
    const name = row.name.trim();
    if (name !== '') seen.set(name, (seen.get(name) ?? 0) + 1);
  }
  for (const row of rows) {
    const name = row.name.trim();
    const value = row.value.trim();
    if (name === '' && value === '') continue;
    if (name === '') {
      issues.set(row.key, 'A value needs a field name.');
    } else if (name === SET_FIELD) {
      issues.set(row.key, `"${SET_FIELD}" is written from the set name.`);
    } else if (PACKAGER_FIELDS.has(name)) {
      issues.set(row.key, `"${name}" is written by the packager.`);
    } else if ((seen.get(name) ?? 0) > 1) {
      issues.set(row.key, `Duplicate field "${name}".`);
    } else if (name === GRAPH_ID_FIELD && value !== '' && !entryIds.has(value)) {
      issues.set(row.key, `"${value}" is not a graph of the set.`);
    }
  }
  return issues;
}

export interface PackRequest {
  manifest: Record<string, string>;
  graphs: Record<string, Record<string, unknown>>;
}

/**
 * The body of `POST /api/graph-set/pack`: the set name travels as `manifest.set`, the caller
 * fields follow in row order (a blank value is left out), and the graphs are keyed by id.
 * Build it only when `entryIssues` and `manifestRowIssues` are empty and the set name is valid.
 */
export function buildPackRequest(
  setName: string,
  rows: readonly ManifestRow[],
  entries: readonly GraphSetEntry[],
): PackRequest {
  const manifest: Record<string, string> = { [SET_FIELD]: setName.trim() };
  for (const row of rows) {
    const name = row.name.trim();
    const value = row.value.trim();
    if (name !== '' && value !== '') manifest[name] = value;
  }
  const graphs: Record<string, Record<string, unknown>> = {};
  for (const entry of entries) {
    graphs[entry.id] = entry.model;
  }
  return { manifest, graphs };
}

export interface InspectedGraph {
  id: string;
  model: Record<string, unknown>;
  name: string | null;
  nodeCount: number;
  connectionCount: number;
}

/** What `POST /api/graph-set/unpack` answered, in the order the package holds it. */
export interface InspectedSet {
  manifest: Record<string, string>;
  graphs: InspectedGraph[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Read the unpack endpoint's answer, `{manifest: {k: v}, graphs: {id: model}}`. The engine has
 * already checked the set, so a graph that fails the file validation here is a shape the panel does
 * not understand, reported as such rather than shown half-rendered.
 */
export function parseUnpackAnswer(value: unknown): InspectedSet {
  if (!isRecord(value) || !isRecord(value.manifest) || !isRecord(value.graphs)) {
    throw new Error("The answer is not a graph set: expected 'manifest' and 'graphs'.");
  }
  const manifest: Record<string, string> = {};
  for (const [key, field] of Object.entries(value.manifest)) {
    if (typeof field !== 'string') {
      throw new Error(`Manifest field '${key}' is not text.`);
    }
    manifest[key] = field;
  }
  const graphs: InspectedGraph[] = [];
  for (const [id, model] of Object.entries(value.graphs)) {
    const result = validateGraphModel(model);
    if (!result.ok) {
      throw new Error(`Graph '${id}': ${result.error}`);
    }
    graphs.push({ id, model: result.model, name: result.name, nodeCount: result.nodeCount, connectionCount: result.connectionCount });
  }
  return { manifest, graphs };
}

/**
 * The manifest editor's rows for a set read back: the caller fields in the package's order, the
 * fields the engine writes (`set`, `format`, `format_version`) left out, so the set packs again
 * as it was - or edited - under the name the editor shows.
 */
export function manifestRowsOf(manifest: Record<string, string>, nextKey: () => number): ManifestRow[] {
  return Object.entries(manifest)
    .filter(([name]) => name !== SET_FIELD && !PACKAGER_FIELDS.has(name))
    .map(([name, value]) => ({ key: nextKey(), name, value }));
}
