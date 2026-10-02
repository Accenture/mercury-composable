/**
 * Extracts the bare topic key from a help command's text.
 *
 * The input is the command text WITHOUT the "> " echo prefix
 * (i.e. `event.commandText` from a `command.helpOrDescribe` event).
 *
 * Examples:
 *   "help create"    → "create"
 *   "help"           → ""        (root index)
 *   "HELP Tutorial 1"→ "tutorial 1"
 *
 * Returns the lowercased, trimmed topic key.
 */
export function extractHelpTopic(commandText: string): string {
  return commandText.replace(/^help\s*/i, '').trim().toLowerCase();
}

/**
 * Returns the help topic key a command shows, or null when the command does
 * not show a help page.
 *
 * Two commands show a help page:
 *   "help create"                → "create"
 *   "help"                       → ""            (root index)
 *   "describe skill graph.math"  → "graph-math"  (the skill's page)
 *
 * The engine answers `describe skill {route}` with the page `help {route}`,
 * each '.' replaced by '-' and the name lowercased (GraphCommandService
 * `describeSkill`), so the skill maps to the same topic key. Only the exact
 * three-word form maps, as the engine requires. The other describe commands
 * (graph, node, connection) return null: their answers stay in the console.
 *
 * Whether the topic is bundled is the caller's question (`getHelpContent`).
 */
export function extractCommandHelpTopic(commandText: string): string | null {
  const trimmed = commandText.trim();
  const lower = trimmed.toLowerCase();
  if (lower === 'help' || lower.startsWith('help ')) return extractHelpTopic(trimmed);
  const [verb, noun, route, ...rest] = lower.split(/\s+/);
  if (verb === 'describe' && noun === 'skill' && route !== undefined && rest.length === 0) {
    return route.replace(/\./g, '-');
  }
  return null;
}
