import type { MinigraphGraphData } from '../utils/graphTypes';

const DESCRIPTIVE_PROPERTY_KEYS = new Set(['description', 'question', 'purpose']);
const BODY_PREFIX = 'input.body';
const HEADER_PREFIX = 'input.header';

function isWordChar(char: string): boolean {
  return /[A-Za-z0-9_.]/.test(char);
}

function isTokenChar(char: string): boolean {
  return /[A-Za-z0-9_.*\[\]-]/.test(char);
}

function collectPathsFromText(text: string, prefix: string, paths: Set<string>): void {
  let searchFrom = 0;
  while (searchFrom < text.length) {
    const begin = text.indexOf(prefix, searchFrom);
    if (begin === -1) return;

    const before = begin > 0 ? text[begin - 1] : '';
    const beforeBefore = begin > 1 ? text[begin - 2] : '';
    const afterPrefix = text[begin + prefix.length] ?? '';
    if (
      (before && isWordChar(before) && !(before === '.' && beforeBefore === '$')) ||
      (afterPrefix && /[A-Za-z0-9_]/.test(afterPrefix))
    ) {
      searchFrom = begin + prefix.length;
      continue;
    }

    let end = begin + prefix.length;
    while (end < text.length && isTokenChar(text[end])) end += 1;

    let token = text.slice(begin, end);
    while (token.endsWith('.') || token.endsWith('-') || token.endsWith('[')) {
      token = token.slice(0, -1);
    }
    while (token.endsWith(']') && !token.includes('[')) {
      token = token.slice(0, -1);
    }
    paths.add(token);
    searchFrom = Math.max(end, begin + prefix.length);
  }
}

function visitPropertyValue(value: unknown, prefix: string, paths: Set<string>, propertyKey?: string): void {
  if (propertyKey && DESCRIPTIVE_PROPERTY_KEYS.has(propertyKey.toLowerCase())) return;
  if (typeof value === 'string') {
    collectPathsFromText(value, prefix, paths);
    return;
  }
  if (Array.isArray(value)) {
    for (const entry of value) visitPropertyValue(entry, prefix, paths);
    return;
  }
  if (typeof value === 'object' && value !== null) {
    for (const [key, entry] of Object.entries(value)) {
      visitPropertyValue(entry, prefix, paths, key);
    }
  }
}

function collectGraphInputPaths(graphData: MinigraphGraphData | null, prefix: string): string[] {
  if (!graphData) return [];
  const paths = new Set<string>();
  for (const node of graphData.nodes) {
    visitPropertyValue(node.properties, prefix, paths);
  }
  return Array.from(paths).sort();
}

/**
 * Derive referenced input.body paths from current node properties.
 * These are UI hints only: MiniGraph does not expose requiredness or input types.
 */
export function collectGraphInputBodyPaths(graphData: MinigraphGraphData | null): string[] {
  return collectGraphInputPaths(graphData, BODY_PREFIX);
}

/**
 * Derive the input header names the graph references (`input.header.<name>` -> `<name>`),
 * sorted and unique. A whole-map reference (`input.header -> ...`) contributes `*`. Header
 * names are case-insensitive to the engine; they are listed as written in the graph.
 */
export function collectGraphInputHeaderNames(graphData: MinigraphGraphData | null): string[] {
  return collectGraphInputPaths(graphData, HEADER_PREFIX).map(path =>
    path === HEADER_PREFIX ? '*' : path.slice(HEADER_PREFIX.length + 1),
  );
}
