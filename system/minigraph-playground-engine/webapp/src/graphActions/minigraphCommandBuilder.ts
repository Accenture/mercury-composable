import type { NodeDraft } from './nodeAuthoringTypes';
import { validateCommandSize, validateDeleteNodeAlias, validateNodeDraft, type DeleteNodeValidationOptions } from './validation';

function getSerializablePropertyRows(draft: NodeDraft, preserveValue = false) {
  return draft.properties
    .map((row) => ({
      key: row.key.trim(),
      value: preserveValue ? row.value.replace(/\r\n/g, '\n').replace(/\r/g, '\n') : row.value.trim(),
    }))
    .filter((row) => row.key || row.value.trim());
}

function assertValidCommandSize(command: string): void {
  const sizeValidation = validateCommandSize(command);
  if (!sizeValidation.valid) {
    throw new Error(sizeValidation.errors.command);
  }
}

function appendSerializedProperty(lines: string[], key: string, value: string): void {
  if (value.includes('\n')) {
    lines.push(`${key}='''`);
    lines.push(value);
    lines.push("'''");
    return;
  }

  lines.push(`${key}=${value}`);
}

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
  const propertyRows = getSerializablePropertyRows(draft);

  // Match the existing multiline command grammar consumed by
  // GraphCommandService.handleMultiLineCommand.
  const lines = [`create node ${alias}`];
  if (nodeType) {
    lines.push(`with type ${nodeType}`);
  }
  if (propertyRows.length > 0) {
    lines.push('with properties');
    for (const row of propertyRows) {
      appendSerializedProperty(lines, row.key, row.value);
    }
  }

  const command = lines.join('\n');
  assertValidCommandSize(command);

  return command;
}

export function buildUpdateNodeCommand(draft: NodeDraft, originalAliasInput: string): string {
  const originalAlias = originalAliasInput.trim();
  const validation = validateNodeDraft(draft, { mode: 'edit', originalAlias });
  if (!validation.valid) {
    throw new Error(Object.values(validation.errors)[0] ?? 'Invalid node draft.');
  }

  const nodeType = draft.nodeType.trim();
  const propertyRows = getSerializablePropertyRows(draft, true);

  const lines = [`update node ${originalAlias}`];
  if (nodeType) {
    lines.push(`with type ${nodeType}`);
  }
  if (propertyRows.length > 0) {
    lines.push('with properties');
    for (const row of propertyRows) {
      appendSerializedProperty(lines, row.key, row.value);
    }
  }

  const command = lines.join('\n');
  assertValidCommandSize(command);
  return command;
}

export function buildDeleteNodeCommand(aliasInput: string, options: DeleteNodeValidationOptions = {}): string {
  const alias = aliasInput.trim();
  const validation = validateDeleteNodeAlias(alias, options);
  if (!validation.valid) {
    throw new Error(Object.values(validation.errors)[0] ?? 'Invalid node alias.');
  }

  const command = `delete node ${alias}`;
  assertValidCommandSize(command);
  return command;
}
