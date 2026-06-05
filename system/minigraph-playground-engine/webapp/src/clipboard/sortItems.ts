import type { ClipboardItemRecord } from './db';

// Sort options available in the Workspace sidebar. These operate on the
// ClipboardItemRecord snapshot, not on live graph data.
export type ClipboardSortField =
  | 'recent'
  | 'type'
  | 'alias'
  | 'source'
  | 'connections'
  | 'property';

export interface ClipboardSortConfig {
  field: ClipboardSortField;
  // Used only when field === 'property'. Examples: "skill", "method", "url".
  propertyKey?: string;
}

// Use the browser's locale-aware comparison so aliases like "node2" sort before
// "node10" and casing does not create surprising groups.
const TEXT_COLLATOR = new Intl.Collator(undefined, {
  sensitivity: 'base',
  numeric: true,
});

function primaryNodeType(item: ClipboardItemRecord): string {
  // Workspace cards display the first node type as the badge. Sort by that same
  // value so the order matches what the user sees in the UI.
  return item.node.types[0]?.trim() || 'unknown';
}

function compareText(left: string, right: string): number {
  return TEXT_COLLATOR.compare(left, right);
}

function stableTie(leftIndex: number, rightIndex: number): number {
  // When two items have the same sort value, preserve their current workspace
  // order. This avoids visual jumping inside a type/source/property group.
  return leftIndex - rightIndex;
}

function propertySortValue(item: ClipboardItemRecord, propertyKey: string): {
  missing: boolean;
  value: string;
} {
  // Property sorting is intentionally shallow because node properties are a flat
  // key/value map in the authoring UI. Missing key means "place this item last".
  const trimmedKey = propertyKey.trim();
  if (!trimmedKey) return { missing: true, value: '' };

  const rawValue = item.node.properties[trimmedKey];
  if (rawValue === undefined || rawValue === null) {
    return { missing: true, value: '' };
  }

  if (typeof rawValue === 'string') return { missing: false, value: rawValue };
  if (typeof rawValue === 'number' || typeof rawValue === 'boolean') {
    return { missing: false, value: String(rawValue) };
  }
  // Arrays/objects are uncommon but valid property values. Stringifying gives a
  // deterministic display-sort value without adding type-specific rules.
  return { missing: false, value: JSON.stringify(rawValue) };
}

function compareProperty(
  left: ClipboardItemRecord,
  right: ClipboardItemRecord,
  propertyKey: string,
): number {
  const leftValue = propertySortValue(left, propertyKey);
  const rightValue = propertySortValue(right, propertyKey);

  // Nodes with the requested property should appear before nodes missing it.
  if (leftValue.missing && !rightValue.missing) return 1;
  if (!leftValue.missing && rightValue.missing) return -1;
  return compareText(leftValue.value, rightValue.value);
}

// Workspace sorting is display-only. IndexedDB still stores items in clipped
// order, and callers receive a new array so the context state is never mutated.
//
// Sort rules:
// - recent: newest clipped item first;
// - type/alias/source/property: text ascending;
// - connections: most direct connections first;
// - ties: keep existing workspace order.
export function sortClipboardItems(
  items: ClipboardItemRecord[],
  config: ClipboardSortConfig,
): ClipboardItemRecord[] {
  return items
    .map((item, originalIndex) => ({ item, originalIndex }))
    .sort((left, right) => {
      let order = 0;
      switch (config.field) {
        case 'type':
          order = compareText(primaryNodeType(left.item), primaryNodeType(right.item));
          break;
        case 'alias':
          order = compareText(left.item.node.alias, right.item.node.alias);
          break;
        case 'source':
          order = compareText(left.item.sourceLabel, right.item.sourceLabel);
          break;
        case 'connections':
          order = right.item.connections.length - left.item.connections.length;
          break;
        case 'property':
          order = compareProperty(left.item, right.item, config.propertyKey ?? '');
          break;
        case 'recent':
        default:
          order = Date.parse(right.item.clippedAt) - Date.parse(left.item.clippedAt);
          break;
      }

      return order !== 0 ? order : stableTie(left.originalIndex, right.originalIndex);
    })
    .map(({ item }) => item);
}
