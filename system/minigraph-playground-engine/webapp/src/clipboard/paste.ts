import { buildNodeCommand } from './commandBuilder';
import type { ClipboardItemRecord } from './db';
import type { MinigraphGraphData } from '../utils/graphTypes';

export interface ClipboardPastePlan {
  verb: 'create' | 'update';
  command: string;
}

/**
 * Plan the paste of a clipped node into the current graph: `update node` when the alias already
 * exists there, `create node` otherwise. The stored connections are not replayed.
 *
 * Throws (from `buildNodeCommand`) when the clipped node cannot be written as command text.
 */
export function buildClipboardPastePlan(
  item: ClipboardItemRecord,
  graphData: MinigraphGraphData | null,
): ClipboardPastePlan {
  const verb = graphData?.nodes.some(node => node.alias === item.node.alias)
    ? 'update'
    : 'create';

  return {
    verb,
    command: buildNodeCommand(verb, item.node),
  };
}
