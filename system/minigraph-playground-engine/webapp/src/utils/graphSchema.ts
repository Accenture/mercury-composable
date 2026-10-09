import type { MinigraphNode } from './graphTypes';
import { createPropertyRow } from '../graphActions/propertyRows';
import type { PropertyRow } from '../graphActions/nodeAuthoringTypes';

/**
 * The graph contract of RFC-0007 as the Schema panel edits it: an optional `schema` property on the
 * root node (the request: `schema.body` for `input.body`, `schema.header` for `input.header`) and
 * one on the end node (the response: `output.body`, `output.header`), each part a schema object in
 * the closed OpenAPI 3.0 subset the engines validate. The panel edits the contract as ROWS - one per
 * body path, one per header name - and this module is the translation in both directions:
 *
 * - the engine's contract view (`GET /api/openapi/session/{id}?view=contract`) and the node's own
 *   declaration become rows (`rowsFromContractPart`), a run's actual values fill types and examples
 *   in (`rowsFromValue`, `mergeRunRows`);
 * - the rows become the two `schema` parts again (`buildPartSchema`), then the flat `key=value`
 *   rows of the Playground grammar (`schemaToPropertyRows`) that `update node` carries
 *   (`buildSchemaUpdateCommandRows`), so every member sees the result and an agent does the same
 *   by command.
 *
 * The panel owns a row's path, type, requiredness, description and example; every other keyword of
 * a declared property (`enum`, `minimum`, `pattern`, …) is kept in `constraints` and written back
 * unchanged - the node editor edits those.
 */

export type SchemaSide = 'input' | 'output';
export type SchemaPart = 'body' | 'header';
export type SchemaRowType = '' | 'string' | 'number' | 'integer' | 'boolean' | 'object' | 'array';
/** A scalar element type of an array row; '' when unknown or an object (described by child rows). */
export type SchemaItemType = '' | 'string' | 'number' | 'integer' | 'boolean';
/** Where a row came from: the chip the panel shows. `new` is a row the user added. */
export type SchemaRowSource = 'declared' | 'discovered' | 'both' | 'run' | 'new';

export const BODY_TYPES: readonly SchemaRowType[] = ['', 'string', 'number', 'integer', 'boolean', 'object', 'array'];
export const HEADER_TYPES: readonly SchemaRowType[] = ['', 'string', 'number', 'integer', 'boolean'];
export const ITEM_TYPES: readonly SchemaItemType[] = ['', 'string', 'number', 'integer', 'boolean'];

/** The keywords a row edits itself; everything else of a declared property is carried through. */
const ROW_KEYWORDS = new Set(['type', 'properties', 'required', 'items', 'description', 'example']);

/** The node aliases the engines read the declaration from (`GraphContract.declare`). */
export const SCHEMA_NODE_ALIAS: Record<SchemaSide, string> = { input: 'root', output: 'end' };
export const SCHEMA_PROPERTY = 'schema';

export interface SchemaRow {
  /** A stable row key for rendering; never saved. */
  key: number;
  /** A body row's path relative to the part (`a`, `profile.name`, `items[].sku`); a header row's name. */
  path: string;
  type: SchemaRowType;
  /** The scalar element type of an array row (`tags[]` of strings); '' for objects or unknown. */
  itemType: SchemaItemType;
  required: boolean;
  description: string;
  /** A documentary example as text; '' when none. */
  example: string;
  source: SchemaRowSource;
  /** The nodes that reference the path, from discovery. */
  usedBy: string[];
  /** The declared keywords the panel does not edit, written back unchanged. */
  constraints: Record<string, unknown>;
}

export interface ContractPathEntry {
  path: string;
  type?: string;
  origin: 'declared' | 'discovered' | 'both';
  required?: boolean;
  usedBy?: string[];
}

export interface ContractPartView {
  declared: boolean;
  schema?: Record<string, unknown>;
  paths: ContractPathEntry[];
}

/** The contract view as `GET /api/openapi/{…}?view=contract` answers it. */
export interface ContractView {
  graph: string;
  purpose?: string;
  input: { body: ContractPartView; header: ContractPartView };
  output: { body: ContractPartView; header: ContractPartView; status: number[] };
  issues: string[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function partView(value: unknown, where: string): ContractPartView {
  if (!isRecord(value) || !Array.isArray(value.paths)) {
    throw new Error(`The contract view has no '${where}' part.`);
  }
  const paths: ContractPathEntry[] = [];
  for (const entry of value.paths) {
    if (!isRecord(entry) || typeof entry.path !== 'string') continue;
    const origin = entry.origin === 'declared' || entry.origin === 'both' ? entry.origin : 'discovered';
    paths.push({
      path: entry.path,
      type: typeof entry.type === 'string' ? entry.type : undefined,
      origin,
      required: entry.required === true,
      usedBy: Array.isArray(entry.usedBy) ? entry.usedBy.filter((n): n is string => typeof n === 'string') : [],
    });
  }
  return {
    declared: value.declared === true,
    schema: isRecord(value.schema) ? value.schema : undefined,
    paths,
  };
}

/** Read the contract view; a shape the panel does not understand is reported, not half-rendered. */
export function parseContractView(value: unknown): ContractView {
  if (!isRecord(value) || typeof value.graph !== 'string' || !isRecord(value.input) || !isRecord(value.output)) {
    throw new Error("The answer is not a graph contract: expected 'graph', 'input' and 'output'.");
  }
  const status = Array.isArray(value.output.status)
    ? value.output.status.filter((s): s is number => typeof s === 'number')
    : [];
  return {
    graph: value.graph,
    purpose: typeof value.purpose === 'string' ? value.purpose : undefined,
    input: { body: partView(value.input.body, 'input.body'), header: partView(value.input.header, 'input.header') },
    output: { body: partView(value.output.body, 'output.body'), header: partView(value.output.header, 'output.header'), status },
    issues: Array.isArray(value.issues) ? value.issues.filter((i): i is string => typeof i === 'string') : [],
  };
}

// ── Paths ──────────────────────────────────────────────────────────────────

/** A path segment of the node grammar: a name, optionally followed by `[]` for an array. */
const SEGMENT_RE = /^([A-Za-z0-9_-]+)(\[\])?$/;
const NAME_RE = /^[A-Za-z0-9_-]+$/;
export const PATH_RULE = 'dot-separated names of letters, digits, hyphen or underscore, with [] after an array';
export const HEADER_NAME_RULE = 'letters, digits, hyphen or underscore';

interface Segment {
  name: string;
  array: boolean;
}

/** `items[].sku` → [{items, array}, {sku}]; null when the path breaks the grammar. */
export function parsePath(path: string): Segment[] | null {
  const trimmed = path.trim();
  if (trimmed === '') return null;
  const segments: Segment[] = [];
  for (const part of trimmed.split('.')) {
    const match = part.match(SEGMENT_RE);
    if (!match) return null;
    segments.push({ name: match[1], array: match[2] !== undefined });
  }
  return segments;
}

export function isValidHeaderName(name: string): boolean {
  return NAME_RE.test(name.trim());
}

/** `input.body.items[].sku` → `items[].sku`; the engine's `[*]` and `[0]` markers read as `[]`. */
export function relativePath(fullPath: string, namespace: string): string {
  const prefix = `${namespace}.`;
  const rest = fullPath.startsWith(prefix) ? fullPath.slice(prefix.length) : fullPath;
  return rest.replace(/\[(\*|\d+)\]/g, '[]');
}

/** The parent row path of `items[].sku` is `items[]`; of `a` it is null. */
export function parentPath(path: string): string | null {
  const index = path.lastIndexOf('.');
  return index < 0 ? null : path.slice(0, index);
}

// ── Rows from the contract ─────────────────────────────────────────────────

/** Walk a schema object down a relative path: `properties` for a name, `items` after `[]`. */
export function schemaAt(schema: Record<string, unknown> | undefined, path: string): Record<string, unknown> | undefined {
  const segments = parsePath(path);
  if (!schema || !segments) return undefined;
  let current: Record<string, unknown> | undefined = schema;
  for (const segment of segments) {
    const properties: unknown = current?.properties;
    const next: unknown = isRecord(properties) ? properties[segment.name] : undefined;
    if (!isRecord(next)) return undefined;
    current = next;
    if (segment.array) {
      current = isRecord(current.items) ? current.items : {};
    }
  }
  return current;
}

/** The `required` list that names a path's last segment, in the schema that holds its parent. */
function isRequiredIn(schema: Record<string, unknown> | undefined, path: string): boolean {
  const parent = parentPath(path);
  const holder = parent === null ? schema : schemaAt(schema, parent);
  const segments = parsePath(path);
  if (!holder || !segments) return false;
  const name = segments[segments.length - 1].name;
  return Array.isArray(holder.required) && holder.required.includes(name);
}

function rowType(value: unknown, allowed: readonly SchemaRowType[]): SchemaRowType {
  return typeof value === 'string' && (allowed as readonly string[]).includes(value) ? value as SchemaRowType : '';
}

function itemTypeOf(schema: Record<string, unknown> | undefined): SchemaItemType {
  const items = schema?.items;
  const type = isRecord(items) ? items.type : undefined;
  return typeof type === 'string' && (ITEM_TYPES as readonly string[]).includes(type) ? type as SchemaItemType : '';
}

function textOf(value: unknown): string {
  if (value === undefined || value === null) return '';
  if (typeof value === 'string') return value;
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  return JSON.stringify(value);
}

function constraintsOf(schema: Record<string, unknown> | undefined): Record<string, unknown> {
  const kept: Record<string, unknown> = {};
  if (!schema) return kept;
  for (const [keyword, value] of Object.entries(schema)) {
    if (!ROW_KEYWORDS.has(keyword)) kept[keyword] = value;
  }
  return kept;
}

/**
 * The rows of one part from the engine's contract view: one row per path, in the view's order, the
 * array markers read as `[]`. A row's type, requiredness and `usedBy` come from the view (discovery
 * merged with the declaration, the declaration winning); its description, example and the carried
 * constraints come from the node's own declaration only - the view's "Referenced by …" description
 * of a discovered leaf is evidence, never text to declare.
 */
export function rowsFromContractPart(
  view: ContractPartView,
  declared: Record<string, unknown> | undefined,
  part: SchemaPart,
  namespace: string,
  nextKey: () => number,
): SchemaRow[] {
  const allowed = part === 'header' ? HEADER_TYPES : BODY_TYPES;
  const rows: SchemaRow[] = [];
  const seen = new Set<string>();
  for (const entry of view.paths) {
    const path = relativePath(entry.path, namespace);
    if (seen.has(path)) continue;
    seen.add(path);
    // an array row describes the array itself (its items, its constraints), not its element
    const lookup = path.endsWith('[]') ? path.slice(0, -2) : path;
    const own = schemaAt(declared, lookup);
    const merged = schemaAt(view.schema, lookup);
    const type = rowType(entry.type ?? merged?.type, allowed);
    rows.push({
      key: nextKey(),
      path,
      type: path.endsWith('[]') ? 'array' : type,
      itemType: path.endsWith('[]') ? (itemTypeOf(own) || itemTypeOf(merged)) : '',
      required: entry.required === true || isRequiredIn(declared, path),
      description: textOf(own?.description),
      example: textOf(own?.example),
      source: entry.origin,
      usedBy: entry.usedBy ?? [],
      constraints: constraintsOf(own),
    });
  }
  return rows;
}

// ── Rows from a run ────────────────────────────────────────────────────────

const EXAMPLE_MAX_LENGTH = 80;

function scalarType(value: unknown): SchemaItemType {
  if (typeof value === 'string') return 'string';
  if (typeof value === 'boolean') return 'boolean';
  if (typeof value === 'number') return Number.isInteger(value) ? 'integer' : 'number';
  return '';
}

function exampleOf(value: unknown): string {
  if (value === null || value === undefined || typeof value === 'object') return '';
  const text = String(value);
  return text.length > EXAMPLE_MAX_LENGTH ? `${text.slice(0, EXAMPLE_MAX_LENGTH - 1)}…` : text;
}

function collectValueRows(prefix: string, value: unknown, rows: SchemaRow[], nextKey: () => number, part: SchemaPart): void {
  if (!isRecord(value)) return;
  for (const [name, item] of Object.entries(value)) {
    if (!NAME_RE.test(name)) continue;       // a name the grammar cannot carry is never declared
    const path = prefix ? `${prefix}.${name}` : name;
    if (part === 'header') {
      rows.push(row(nextKey(), path, 'string', '', exampleOf(item)));
      continue;
    }
    if (Array.isArray(item)) {
      const first = item.find(element => element !== null && element !== undefined);
      const elementType = isRecord(first) ? '' : scalarType(first);
      rows.push(row(nextKey(), `${path}[]`, 'array', elementType, ''));
      if (isRecord(first)) {
        // the union of the elements' keys, in first-seen order
        const union: Record<string, unknown> = {};
        for (const element of item) {
          if (isRecord(element)) {
            for (const [k, v] of Object.entries(element)) {
              if (!(k in union)) union[k] = v;
            }
          }
        }
        collectValueRows(`${path}[]`, union, rows, nextKey, part);
      }
    } else if (isRecord(item)) {
      rows.push(row(nextKey(), path, 'object', '', ''));
      collectValueRows(path, item, rows, nextKey, part);
    } else {
      rows.push(row(nextKey(), path, scalarType(item), '', exampleOf(item)));
    }
  }
}

function row(key: number, path: string, type: SchemaRowType, itemType: SchemaItemType, example: string): SchemaRow {
  return { key, path, type, itemType, required: false, description: '', example, source: 'run', usedBy: [], constraints: {} };
}

/**
 * Rows inferred from an instance's actual value (`input.body`, `output.header`, …): the type of each
 * leaf (an integral number is an integer), `name[]` for an array with the element type or the union
 * of its objects' keys, and a scalar's text as the example (cut at 80 characters). A header is
 * text, so a header row is a string with the value as its example.
 */
export function rowsFromValue(value: unknown, part: SchemaPart, nextKey: () => number): SchemaRow[] {
  const rows: SchemaRow[] = [];
  collectValueRows('', value, rows, nextKey, part);
  return rows;
}

export interface MergeOutcome {
  rows: SchemaRow[];
  /** Rows added or changed by the merge. */
  changed: number;
}

/**
 * "Fill from last run": a run's rows fill what the contract could not say - an untyped row gains the
 * run's type, an array its element type, a row without an example gains the value - and a path the
 * contract never saw is appended as `run`. A declared row keeps its chip; a discovered row that the
 * run typed becomes `run`, since that is where its type now comes from (an example alone does not
 * move the chip).
 */
export function mergeRunRows(rows: readonly SchemaRow[], runRows: readonly SchemaRow[]): MergeOutcome {
  let changed = 0;
  const byPath = new Map(rows.map(r => [r.path, r]));
  const merged = rows.map(r => ({ ...r }));
  for (const run of runRows) {
    const existing = byPath.get(run.path);
    if (!existing) {
      merged.push(run);
      changed++;
      continue;
    }
    const target = merged.find(r => r.key === existing.key)!;
    let typed = false;
    let touched = false;
    if (target.type === '' && run.type !== '') {
      target.type = run.type;
      typed = true;
    }
    if (target.type === 'array' && target.itemType === '' && run.itemType !== '') {
      target.itemType = run.itemType;
      typed = true;
    }
    if (target.example === '' && run.example !== '') {
      target.example = run.example;
      touched = true;
    }
    if (typed || touched) changed++;
    // the chip says where the TYPE came from; an example alone does not move it
    if (typed && target.source === 'discovered') target.source = 'run';
  }
  return { rows: merged, changed };
}

// ── Validation ─────────────────────────────────────────────────────────────

/**
 * What the engine's gate would refuse, flagged in place: a blank or malformed path, a header name
 * outside the grammar, a duplicate, a `[]` path that is not an array (and the reverse), a path with
 * nested rows whose own type is a scalar, and a header type outside string, number, integer and
 * boolean. A body type is otherwise free; '' means "not declared".
 */
export function schemaRowIssues(rows: readonly SchemaRow[], part: SchemaPart): Map<number, string> {
  const issues = new Map<number, string>();
  const counts = new Map<string, number>();
  for (const r of rows) {
    const path = r.path.trim();
    if (path !== '') counts.set(path, (counts.get(path) ?? 0) + 1);
  }
  for (const r of rows) {
    const path = r.path.trim();
    if (path === '') {
      issues.set(r.key, part === 'header' ? 'A header name is required.' : 'A path is required.');
      continue;
    }
    if (part === 'header') {
      if (!isValidHeaderName(path)) {
        issues.set(r.key, `A header name uses ${HEADER_NAME_RULE}.`);
      } else if (!(HEADER_TYPES as readonly string[]).includes(r.type)) {
        issues.set(r.key, 'A header is text: string, number, integer or boolean.');
      } else if ((counts.get(path) ?? 0) > 1) {
        issues.set(r.key, `Duplicate header "${path}".`);
      }
      continue;
    }
    if (!parsePath(path)) {
      issues.set(r.key, `A path uses ${PATH_RULE}.`);
    } else if ((counts.get(path) ?? 0) > 1) {
      issues.set(r.key, `Duplicate path "${path}".`);
    } else if (path.endsWith('[]') && r.type !== 'array') {
      issues.set(r.key, 'A path ending in [] is an array.');
    } else if (!path.endsWith('[]') && r.type === 'array') {
      issues.set(r.key, 'An array path ends in [] - items[] for a list of items.');
    } else if (r.type !== '' && r.type !== 'object' && r.type !== 'array'
      && rows.some(other => other !== r && other.path.trim().startsWith(`${path}.`))) {
      issues.set(r.key, `"${path}" has nested paths, so its type is object (or leave it blank).`);
    } else if (r.type === 'array' && r.itemType !== ''
      && rows.some(other => other !== r && other.path.trim().startsWith(`${path}.`))) {
      issues.set(r.key, `"${path}" has nested paths, so its items are objects - clear the item type.`);
    }
  }
  return issues;
}

// ── Rows to a schema part ──────────────────────────────────────────────────

interface TreeNode {
  row: SchemaRow | null;
  array: boolean;
  children: Map<string, TreeNode>;
}

function newNode(array: boolean): TreeNode {
  return { row: null, array, children: new Map() };
}

/** The schema of one tree node; `null` when the node says nothing at all. */
function nodeSchema(node: TreeNode, part: SchemaPart): Record<string, unknown> {
  const row = node.row;
  const schema: Record<string, unknown> = {};
  const childSchemas: Record<string, unknown> = {};
  const required: string[] = [];
  for (const [name, child] of node.children) {
    childSchemas[name] = nodeSchema(child, part);
    if (child.row?.required) required.push(name);
  }
  const hasChildren = node.children.size > 0;
  if (node.array) {
    schema.type = 'array';
    if (hasChildren) {
      const items: Record<string, unknown> = { type: 'object', properties: childSchemas };
      if (required.length > 0) items.required = required;
      schema.items = items;
    } else if (row && row.itemType !== '') {
      schema.items = { type: row.itemType };
    } else if (row && isRecord(row.constraints.items)) {
      schema.items = row.constraints.items;
    }
  } else {
    const type = row?.type ?? '';
    if (hasChildren || type === 'object') {
      schema.type = 'object';
      if (hasChildren) schema.properties = childSchemas;
      if (required.length > 0) schema.required = required;
    } else if (type !== '') {
      schema.type = type;
    }
  }
  if (row) {
    if (row.description.trim() !== '') schema.description = row.description.trim();
    if (row.example.trim() !== '') schema.example = row.example.trim();
    for (const [keyword, value] of Object.entries(row.constraints)) {
      if (keyword === 'items' && 'items' in schema) continue;   // the row's own element typing wins
      if (!(keyword in schema)) schema[keyword] = value;
    }
  }
  return schema;
}

/**
 * The schema object of one part from its rows: an object schema whose `properties` follow the rows,
 * nested through `properties` and `items`, `required` listing the required names at each level, and
 * each row's description, example and carried constraints on its property. `null` when there is no
 * row, which removes the part from the declaration. Call it only when `schemaRowIssues` is empty.
 */
export function buildPartSchema(rows: readonly SchemaRow[], part: SchemaPart): Record<string, unknown> | null {
  const live = rows.filter(r => r.path.trim() !== '');
  if (live.length === 0) return null;
  const root = newNode(false);
  for (const r of live) {
    const segments = part === 'header' ? [{ name: r.path.trim(), array: false }] : parsePath(r.path.trim());
    if (!segments) continue;
    let node = root;
    for (const segment of segments) {
      let child = node.children.get(segment.name);
      if (!child) {
        child = newNode(segment.array);
        node.children.set(segment.name, child);
      } else if (segment.array) {
        child.array = true;
      }
      node = child;
    }
    node.row = r;
  }
  return nodeSchema(root, part);
}

/** The `schema` property from the two parts; `null` when neither part is declared. */
export function buildSchemaProperty(
  body: Record<string, unknown> | null,
  header: Record<string, unknown> | null,
): Record<string, unknown> | null {
  const schema: Record<string, unknown> = {};
  if (body) schema.body = body;
  if (header) schema.header = header;
  return Object.keys(schema).length > 0 ? schema : null;
}

// ── The node grammar ───────────────────────────────────────────────────────

const TRIPLE_QUOTE = "'''";

function flattenForGrammar(prefix: string, value: unknown, rows: PropertyRow[]): void {
  if (value === null || value === undefined) return;      // a graph holds no null property
  if (Array.isArray(value)) {
    for (const element of value) {
      if (element === null || element === undefined || typeof element === 'object') {
        throw new Error(`'${prefix}' holds a list of objects, which the node grammar cannot carry.`);
      }
      rows.push(createPropertyRow(`${prefix}[]`, grammarText(prefix, element)));
    }
    return;
  }
  if (isRecord(value)) {
    for (const [name, item] of Object.entries(value)) {
      if (!NAME_RE.test(name)) {
        throw new Error(`'${prefix}.${name}' - a property name uses ${HEADER_NAME_RULE}.`);
      }
      flattenForGrammar(`${prefix}.${name}`, item, rows);
    }
    return;
  }
  rows.push(createPropertyRow(prefix, grammarText(prefix, value)));
}

function grammarText(where: string, value: unknown): string {
  const text = typeof value === 'string' ? value : String(value);
  if (text.includes(TRIPLE_QUOTE)) {
    throw new Error(`'${where}' contains ''' which the node grammar cannot carry.`);
  }
  return text;
}

/**
 * The flat `key=value` rows of the Playground grammar for a `schema` property - the composite keys
 * `schema.body.properties.a.type=number`, `schema.body.required[]=a`, `schema.header.properties.X-Api-Key.type=string`
 * - the shape `edit node` prints and `update node` reads. A list appends with `[]` in row order; a
 * boolean or number is written as text, which the engine's schema compiler reads as the keyword
 * expects. An empty object or list says nothing and is left out. A name or a value the grammar cannot
 * carry is refused by name.
 */
export function schemaToPropertyRows(schema: Record<string, unknown>, prefix = SCHEMA_PROPERTY): PropertyRow[] {
  const rows: PropertyRow[] = [];
  flattenForGrammar(prefix, schema, rows);
  return rows;
}

/** True for a node-editor row that belongs to the `schema` property (`schema`, `schema.body.…`, `schema[]`). */
export function isSchemaPropertyKey(key: string): boolean {
  const trimmed = key.trim();
  return trimmed === SCHEMA_PROPERTY || trimmed.startsWith(`${SCHEMA_PROPERTY}.`) || trimmed.startsWith(`${SCHEMA_PROPERTY}[`);
}

/** The node's current declaration, `node.properties.schema`, when it is an object. */
export function declaredSchemaOf(node: MinigraphNode | undefined): Record<string, unknown> | undefined {
  const schema = node?.properties[SCHEMA_PROPERTY];
  return isRecord(schema) ? schema : undefined;
}

/** A short summary of the constraints a row carries, for the chip: `enum (2) · minimum 0 · pattern`. */
export function summarizeConstraints(constraints: Record<string, unknown>): string {
  return Object.entries(constraints).map(([keyword, value]) => {
    if (Array.isArray(value)) return `${keyword} (${value.length})`;
    if (isRecord(value)) return keyword;
    const text = String(value);
    return text.length > 16 ? keyword : `${keyword} ${text}`;
  }).join(' · ');
}
