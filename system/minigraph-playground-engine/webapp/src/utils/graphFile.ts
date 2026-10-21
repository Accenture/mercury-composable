import type { MinigraphGraphData } from './graphTypes';

/** The engine's rule for a graph file name (`validGraphFileName`): letters, digits, hyphen and underscore. */
export const GRAPH_ID_RE = /^[A-Za-z0-9_-]+$/;
export const GRAPH_ID_RULE = 'letters, digits, hyphen or underscore';

export function isValidGraphId(id: string): boolean {
  return GRAPH_ID_RE.test(id);
}

/** A graph id suggested from a display name: allowed characters kept, every other run becomes one hyphen. */
export function suggestGraphId(name: string | null | undefined): string {
  const cleaned = (name ?? '').trim().replace(/[^A-Za-z0-9_-]+/g, '-').replace(/^-+|-+$/g, '');
  return cleaned || 'untitled';
}

/** The file a graph id is saved as - the extension is always `.json`. */
export function graphFileName(graphId: string): string {
  return `${graphId}.json`;
}

export interface ValidGraphFile {
  ok: true;
  /** The model exactly as read; it is sent to the engine unchanged. */
  model: Record<string, unknown>;
  /** The root node's "name" property, when it has one. */
  name: string | null;
  nodeCount: number;
  connectionCount: number;
}

export interface InvalidGraphFile {
  ok: false;
  error: string;
}

export type GraphFileValidation = ValidGraphFile | InvalidGraphFile;

const GRAPH_SECTIONS = new Set(['nodes', 'connections']);

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Simple validation of a graph model read from a file - the shape `export graph as` writes and
 * `import graph from` reads: a JSON object whose only top-level sections are `nodes` (mandatory,
 * a non-empty list of `{alias, types}` entries) and `connections` (optional - a work in progress
 * may have none - a list of `{source, target}` entries). Anything else is refused, naming the
 * section. The engine validates again on import, and CompileGraph stays the quality gate for
 * everything it checks at deploy time.
 */
export function validateGraphModel(value: unknown): GraphFileValidation {
  if (!isRecord(value)) {
    return { ok: false, error: "The file is not a JSON object with a 'nodes' section." };
  }
  const unexpected = Object.keys(value).filter(key => !GRAPH_SECTIONS.has(key)).sort();
  if (unexpected.length > 0) {
    return {
      ok: false,
      error: `Unexpected top-level section${unexpected.length > 1 ? 's' : ''}: ${unexpected.join(', ')}. `
        + "A graph model has only 'nodes' and 'connections'.",
    };
  }
  const nodes = value.nodes;
  if (!Array.isArray(nodes)) {
    return { ok: false, error: "The 'nodes' section is mandatory and must be a list." };
  }
  if (nodes.length === 0) {
    return { ok: false, error: "The 'nodes' section is empty - there is nothing to import." };
  }
  let name: string | null = null;
  for (const [index, node] of nodes.entries()) {
    const position = `Node entry ${index + 1}`;
    if (!isRecord(node)) {
      return { ok: false, error: `${position} is not an object.` };
    }
    if (typeof node.alias !== 'string' || node.alias.trim() === '') {
      return { ok: false, error: `${position} has no alias.` };
    }
    const types = node.types;
    if (!Array.isArray(types) || types.length === 0 || !types.every(type => typeof type === 'string')) {
      return { ok: false, error: `${position} ('${node.alias}') has no types.` };
    }
    if (node.properties !== undefined && !isRecord(node.properties)) {
      return { ok: false, error: `${position} ('${node.alias}') has properties that are not an object.` };
    }
    if (types.includes('Root') && isRecord(node.properties) && typeof node.properties.name === 'string') {
      name = node.properties.name;
    }
  }
  if (value.connections !== undefined && !Array.isArray(value.connections)) {
    return { ok: false, error: "The 'connections' section must be a list." };
  }
  const connections: unknown[] = Array.isArray(value.connections) ? value.connections : [];
  for (const [index, connection] of connections.entries()) {
    if (!isRecord(connection) || typeof connection.source !== 'string' || typeof connection.target !== 'string') {
      return { ok: false, error: `Connection entry ${index + 1} needs a 'source' and a 'target'.` };
    }
  }
  return { ok: true, model: value, name, nodeCount: nodes.length, connectionCount: connections.length };
}

/**
 * The text written to `<graphId>.json`: the graph as displayed, with the root node's "name"
 * set to the graph id - what `export graph as {name}` does on the engine, so the file's name
 * and the model inside it agree. The live graph is not modified.
 */
export function buildGraphFileText(graph: MinigraphGraphData, graphId: string): string {
  const copy = JSON.parse(JSON.stringify(graph)) as MinigraphGraphData;
  const root = copy.nodes.find(node => Array.isArray(node.types) && node.types.includes('Root'));
  if (root) {
    root.properties = { ...(root.properties ?? {}), name: graphId };
  }
  return `${JSON.stringify(copy, null, 2)}\n`;
}

interface SaveFilePickerOptions {
  suggestedName?: string;
  excludeAcceptAllOption?: boolean;
  types?: { description?: string; accept: Record<string, string[]> }[];
}

/** What a save writes: text, bytes, or a rendered file such as an image. */
type FileContent = string | Uint8Array<ArrayBuffer> | Blob;

interface WritableFileHandle {
  name: string;
  createWritable(): Promise<{ write(data: FileContent): Promise<void>; close(): Promise<void>; abort(): Promise<void> }>;
  getFile?(): Promise<File>;
  /** Chromium's non-standard `FileSystemHandle.remove()`: deletes the file the handle points at. */
  remove?(): Promise<void>;
}

type PickerWindow = Window & {
  showSaveFilePicker?: (options: SaveFilePickerOptions) => Promise<WritableFileHandle>;
};

/** True when the browser offers a native "save as" dialog (the File System Access API; Chromium browsers). */
export function supportsSaveFilePicker(): boolean {
  return typeof window !== 'undefined' && typeof (window as PickerWindow).showSaveFilePicker === 'function';
}

export type SaveOutcome =
  | { saved: true; fileName: string }
  | { saved: false; cancelled: true };

/** What the native "save as" dialog offers as the file type, and the download's MIME type. */
interface SaveFileType {
  description: string;
  mimeType: string;
  extension: string;
}

const GRAPH_FILE_TYPE: SaveFileType = { description: 'MiniGraph model (JSON)', mimeType: 'application/json', extension: '.json' };
const GRAPH_SET_FILE_TYPE: SaveFileType = { description: 'MiniGraph graph set', mimeType: 'application/octet-stream', extension: '.pack' };
const OPENAPI_FILE_TYPE: SaveFileType = { description: 'OpenAPI document (YAML)', mimeType: 'application/yaml', extension: '.yaml' };
const IMAGE_FILE_TYPE: SaveFileType = { description: 'PNG image', mimeType: 'image/png', extension: '.png' };

/**
 * Save text as a file: through the native "save as" dialog where the browser has one (the user
 * picks the folder and may rename; the `.json` type is kept), otherwise as a download into the
 * browser's download folder under `fileName`.
 */
export function saveTextFile(text: string, fileName: string): Promise<SaveOutcome> {
  return saveFile(text, fileName, GRAPH_FILE_TYPE);
}

/** The YAML twin of {@link saveTextFile}: a graph's OpenAPI document to `<graph-id>.yaml`. */
export function saveYamlFile(text: string, fileName: string): Promise<SaveOutcome> {
  return saveFile(text, fileName, OPENAPI_FILE_TYPE);
}

/**
 * The binary twin of {@link saveTextFile}: a graph set's bytes to `<set>.pack`, through the native
 * dialog where the browser has one, otherwise into the download folder.
 */
export function saveBinaryFile(bytes: Uint8Array<ArrayBuffer>, fileName: string): Promise<SaveOutcome> {
  return saveFile(bytes, fileName, GRAPH_SET_FILE_TYPE);
}

/**
 * An image of the graph to `<graph-id>.png`. `render` runs only AFTER the native dialog closes:
 * the browser opens that dialog only shortly after the user's click, and rendering a large graph
 * can take longer. A cancelled dialog renders nothing, and a failed render removes the empty file
 * the dialog left behind. Without the dialog, `render` runs first and the result goes to the
 * download folder.
 */
export function saveImageFile(render: () => Promise<Blob>, fileName: string): Promise<SaveOutcome> {
  return saveFile(render, fileName, IMAGE_FILE_TYPE);
}

/**
 * Cleanup after a save that failed once the native dialog had closed. Chromium creates the chosen
 * file when the dialog closes, before anything is written, so a failed render or write would leave
 * an empty file behind. It is removed only while it is still empty: a file the user chose to
 * overwrite and that still holds its old content is kept. Best effort - a browser without
 * `remove()` keeps the empty file, and the save's own error is the one reported.
 */
async function removeIfEmpty(handle: WritableFileHandle): Promise<void> {
  if (typeof handle.getFile !== 'function' || typeof handle.remove !== 'function') return;
  try {
    const file = await handle.getFile();
    if (file.size === 0) await handle.remove();
  } catch {
    // the file stays; the caller reports the save's failure
  }
}

async function saveFile(
  data: FileContent | (() => Promise<FileContent>),
  fileName: string,
  type: SaveFileType,
): Promise<SaveOutcome> {
  const content = typeof data === 'function' ? data : () => Promise.resolve(data);
  const picker = (window as PickerWindow).showSaveFilePicker;
  if (typeof picker === 'function') {
    let handle: WritableFileHandle;
    try {
      handle = await picker.call(window, {
        suggestedName: fileName,
        excludeAcceptAllOption: true,
        types: [{ description: type.description, accept: { [type.mimeType]: [type.extension] } }],
      });
    } catch (err) {
      if ((err as Error).name === 'AbortError') return { saved: false, cancelled: true };
      throw err;
    }
    try {
      const written = await content();
      const writable = await handle.createWritable();
      try {
        await writable.write(written);
        await writable.close();
      } catch (err) {
        // discard the browser's temporary copy; the chosen file keeps what it had
        await writable.abort().catch(() => undefined);
        throw err;
      }
    } catch (err) {
      await removeIfEmpty(handle);
      throw err;
    }
    return { saved: true, fileName: handle.name };
  }
  const blob = new Blob([await content()], { type: type.mimeType });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  anchor.rel = 'noopener';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 0);
  return { saved: true, fileName };
}
