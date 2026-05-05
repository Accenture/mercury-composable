import type { NodeDraft, NodeDraftSource, PropertyRow } from './nodeAuthoringTypes';

let rowCounter = 0;

// Row ids are only for React rendering and field-error keys. They are never
// sent to the backend.
export function createPropertyRow(key = '', value = ''): PropertyRow {
  rowCounter += 1;
  return { id: `property-row-${rowCounter}`, key, value };
}

// First-node authoring uses deterministic defaults. They are starting values
// only; normal validation still runs after the user edits or submits.
export function createDefaultNodeDraft(source: NodeDraftSource): NodeDraft {
  return {
    alias: source === 'empty-graph' ? 'root' : '',
    nodeType: source === 'empty-graph' ? 'Root' : '',
    properties: [createPropertyRow()],
    source,
  };
}
