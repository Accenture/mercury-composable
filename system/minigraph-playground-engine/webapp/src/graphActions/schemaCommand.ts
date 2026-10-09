import type { MinigraphNode } from '../utils/graphTypes';
import { buildUpdateNodeCommand } from './minigraphCommandBuilder';
import { createEditNodeFormState } from './propertyRows';
import { isSchemaPropertyKey, schemaToPropertyRows } from '../utils/graphSchema';

/**
 * The `update node <alias>` command that rewrites a root or end node's `schema` property and nothing
 * else. `update node` clears the node's property set before re-adding, so the node's other
 * properties are re-sent exactly as the node editor would send them (`createEditNodeFormState`:
 * the same flattening, the same `[]` append signature), with the `schema.*` rows replaced by the
 * new declaration - or dropped when `schema` is null, which removes the declaration. A node the
 * node editor cannot represent (more than one type, a value holding `'''`) is refused with the
 * editor's own message, because the panel would otherwise lose a property it cannot carry.
 */
export function buildSchemaUpdateCommand(node: MinigraphNode, schema: Record<string, unknown> | null): string {
  const conversion = createEditNodeFormState(node);
  if (!conversion.valid || !conversion.formState) {
    throw new Error(conversion.message ?? 'The node cannot be edited from the Schema panel.');
  }
  const others = conversion.formState.properties.filter(row => row.key.trim() !== '' && !isSchemaPropertyKey(row.key));
  const schemaRows = schema ? schemaToPropertyRows(schema) : [];
  return buildUpdateNodeCommand({
    ...conversion.formState,
    properties: [...others, ...schemaRows],
  }, node.alias);
}
