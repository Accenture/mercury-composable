import { MAX_BUFFER } from '../config/playgrounds';
import type { MinigraphGraphData } from '../utils/graphTypes';
import type { NodeDraft, NodeDraftValidationErrors } from './nodeAuthoringTypes';

// Keep this token rule aligned with GraphProperties.validateName on the backend.
// The backend remains authoritative; this only blocks obviously invalid drafts
// before they can become raw command text.
export const NODE_NAME_RE = /^[A-Za-z0-9_-]+$/;

// Mirrors MiniGraph's reserved alias list so the modal can show immediate field
// feedback instead of relying on a later generic backend ERROR response.
export const RESERVED_ALIASES = new Set([
  'input',
  'output',
  'model',
  'response',
  'result',
  'parameter',
  'none',
  'next',
  'api',
  'error',
]);

export interface NodeDraftValidationOptions {
  graphData?: MinigraphGraphData | null;
}

export interface NodeDraftValidationResult {
  valid: boolean;
  errors: NodeDraftValidationErrors;
}

export function getValidationErrorKeyForProperty(rowId: string, field: 'key' | 'value'): string {
  return `properties.${rowId}.${field}`;
}

// Validate the supported authoring surface: alias, optional node type, and flat
// single-line scalar properties. Duplicate aliases from graphData are advisory
// because graphData is a staleable frontend projection.
export function validateNodeDraft(
  draft: NodeDraft,
  options: NodeDraftValidationOptions = {},
): NodeDraftValidationResult {
  const errors: NodeDraftValidationErrors = {};
  const alias = draft.alias.trim();
  const nodeType = draft.nodeType.trim();

  if (!alias) {
    errors.alias = 'Alias is required.';
  } else if (!NODE_NAME_RE.test(alias)) {
    errors.alias = 'Use only letters, numbers, underscore, and hyphen.';
  } else if (RESERVED_ALIASES.has(alias.toLowerCase())) {
    errors.alias = `"${alias}" is reserved.`;
  } else if (options.graphData?.nodes.some((node) => node.alias.toLowerCase() === alias.toLowerCase())) {
    errors.alias = `Node "${alias}" already exists in the current graph.`;
  }

  if (nodeType && !NODE_NAME_RE.test(nodeType)) {
    errors.nodeType = 'Use only letters, numbers, underscore, and hyphen.';
  }

  for (const row of draft.properties) {
    const key = row.key.trim();
    const value = row.value.trim();
    if (!key && !value) continue;

    if (!key && value) {
      errors[getValidationErrorKeyForProperty(row.id, 'key')] = 'Property key is required when value is present.';
    } else if (!NODE_NAME_RE.test(key)) {
      errors[getValidationErrorKeyForProperty(row.id, 'key')] = 'Use only letters, numbers, underscore, and hyphen.';
    }

    if (value.includes('\r') || value.includes('\n')) {
      errors[getValidationErrorKeyForProperty(row.id, 'value')] = 'Property value must be a single line.';
    } else if (value.includes("'''")) {
      errors[getValidationErrorKeyForProperty(row.id, 'value')] = "Property value cannot contain '''.";
    }
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

// The command budget belongs to the WebSocket command path, so size is checked
// after serialization rather than estimating from individual fields.
export function validateCommandSize(commandText: string): NodeDraftValidationResult {
  if (commandText.length <= MAX_BUFFER) return { valid: true, errors: {} };
  return {
    valid: false,
    errors: {
      command: 'The node command is too large. Shorten property values before submitting.',
    },
  };
}
