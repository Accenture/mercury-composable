import type { NodeDraft } from './nodeAuthoringTypes';
import { validateCommandSize, validateNodeDraft } from './validation';

// Single serialization boundary for create-node UI authoring. Callers pass a
// typed draft; only this builder is allowed to produce backend raw command text
// so validation and injection guards stay centralized.
export function buildCreateNodeCommand(draft: NodeDraft): string {
  const validation = validateNodeDraft(draft);
  if (!validation.valid) {
    throw new Error(Object.values(validation.errors)[0] ?? 'Invalid node draft.');
  }

  const alias = draft.alias.trim();
  const nodeType = draft.nodeType.trim();
  const propertyRows = draft.properties
    .map((row) => ({ key: row.key.trim(), value: row.value.trim() }))
    .filter((row) => row.key || row.value);

  // Match the existing multiline command grammar consumed by
  // GraphCommandService.handleMultiLineCommand.
  const lines = [`create node ${alias}`];
  if (nodeType) {
    lines.push(`with type ${nodeType}`);
  }
  if (propertyRows.length > 0) {
    lines.push('with properties');
    for (const row of propertyRows) {
      lines.push(`${row.key}=${row.value}`);
    }
  }

  const command = lines.join('\n');
  const sizeValidation = validateCommandSize(command);
  if (!sizeValidation.valid) {
    throw new Error(sizeValidation.errors.command);
  }

  return command;
}
