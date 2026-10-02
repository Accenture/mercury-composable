import { extractCommandHelpTopic } from './helpTopic';  // src/utils/helpTopic.ts
import { getHelpContent, type HelpContentProfile } from '../data/helpContent'; // src/data/helpContent.ts

/**
 * Resolves the bundled help topic key for a command, or null if the command
 * should be sent to the backend.
 *
 * A command resolves when it shows a help page (`help {topic}`, or
 * `describe skill {route}` for a built-in skill) and that page is bundled in
 * the active content profile. The help panel then renders the page as
 * markdown, which the console cannot.
 *
 * @param commandText  The trimmed command string as typed by the user (no "> " prefix).
 * @param supportsHelp Whether the current playground has the help panel enabled.
 * @returns            The topic key ("" = root index, "create" = help create page,
 *                     "graph-math" = the graph.math skill page),
 *                     or null when the command should go to the backend.
 */
export function resolveBundledHelpTopic(
  commandText: string,
  supportsHelp: boolean,
  contentProfile: HelpContentProfile = 'minigraph',
): string | null {
  if (!supportsHelp) return null;
  const topic = extractCommandHelpTopic(commandText);
  if (topic === null) return null;
  return getHelpContent(topic, contentProfile) !== null ? topic : null; // null when not bundled
}
