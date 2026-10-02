import type { MinigraphNode } from '../utils/graphTypes';
import { createEditNodeFormState } from '../graphActions/propertyRows';
import { buildCreateNodeCommand, buildUpdateNodeCommand } from '../graphActions/minigraphCommandBuilder';

/**
 * Build a create or update node multi-line command string from a clipped MinigraphNode.
 *
 * The clipped node is a snapshot of the engine's graph JSON, so it is converted exactly as the node
 * editor converts a node (`createEditNodeFormState`) and serialized by the one authoring boundary
 * (`minigraphCommandBuilder`): a scalar is one `key=value` line, a list is one `key[]=element` line
 * per element in list order, a nested map is one `path.key=value` line per leaf, and a value with a
 * newline is wrapped in `'''`. That is the shape the engine's own `edit node` prints and
 * `update node` accepts. Writing `key[]=value` for a scalar would make the engine store a
 * one-element list (`MultiLevelMap.setElement` appends on the `[]` signature), which is what this
 * builder did until 2026-10-02.
 *
 * Throws when the snapshot cannot be written as command text (an empty list or map, a value
 * containing `'''`, more than one type, an alias or key the grammar rejects, or a command over the
 * size limit); the caller reports the message.
 */
export function buildNodeCommand(
  verb: 'create' | 'update',
  node: MinigraphNode,
): string {
  const conversion = createEditNodeFormState(node);
  if (!conversion.valid || conversion.formState === null) {
    throw new Error(
      `Node "${node.alias}" cannot be pasted: its data cannot be written as a ${verb} node command ` +
      "(an empty list or map, a value containing ''', or more than one type).",
    );
  }

  return verb === 'update'
    ? buildUpdateNodeCommand(conversion.formState, node.alias)
    : buildCreateNodeCommand(conversion.formState);
}
