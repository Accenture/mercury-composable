/**
 * All help markdown files bundled as raw strings via Vite's import.meta.glob.
 *
 * The path is relative to THIS file (src/data/helpContent.ts).
 * It resolves to extensions/minigraph-playground-engine/src/main/resources/help/
 *
 * `eager: true`   — all files are included in the bundle; no async loading needed
 *                   (18 files, each < 10 KB — total overhead is negligible).
 * `query: '?raw'` — each file is imported as a plain string, not a JS module.
 */
const RAW_FILES = import.meta.glob(
  '../../../src/main/resources/help/*.md',
  { query: '?raw', import: 'default', eager: true }
) as Record<string, string>;

/**
 * Normalise the glob key (a full filesystem path) into a bare filename,
 * then strip the ".md" extension to get the base name.
 *
 * Examples:
 *   ".../help/help create.md"       → "help create"
 *   ".../help/help.md"              → "help"
 *   ".../help/help tutorial 1.md"   → "help tutorial 1"
 */
function toBaseName(globKey: string): string {
  const parts = globKey.split('/');
  const filename = parts[parts.length - 1] ?? globKey;
  return filename.replace(/\.md$/, '');
}

/**
 * Internal lookup map: base-name (e.g. "help create") → raw markdown string.
 * Populated at module-load time from the Vite-bundled file contents.
 */
const FILE_MAP: Record<string, string> = Object.fromEntries(
  Object.entries(RAW_FILES).map(([key, content]) => [toBaseName(key), content])
);

/**
 * Get the raw markdown string for a given topic key.
 *
 * @param topic  "" (empty string) = root index ("help.md");
 *               "create"          = "help create.md";
 *               "data-dictionary" = "help data-dictionary.md";
 *               "tutorial 1"      = "help tutorial 1.md"
 * @returns      The raw markdown string, or null if the topic is not found.
 */
export function getHelpContent(topic: string): string | null {
  const baseName = topic === '' ? 'help' : `help ${topic}`;
  return FILE_MAP[baseName] ?? null;
}

/**
 * Ordered list of valid topic keys (everything except root "").
 * Derived directly from the bundled file map so it is always in sync.
 */
export const HELP_TOPIC_KEYS: ReadonlyArray<string> = Object.keys(FILE_MAP)
  .filter(name => name !== 'help')            // exclude root
  .map(name => name.replace(/^help\s+/, ''))  // strip "help " prefix → bare topic
  .sort();

/**
 * Category identifiers for grouping help topics in the navigation tabs.
 * 'overview' is the root help index page (single page, no sub-topics).
 */
export type HelpCategory = 'overview' | 'graph-model' | 'graph-skills' | 'instance-model' | 'tutorials';

/**
 * Display metadata for each category.
 */
export interface HelpCategoryInfo {
  id:    HelpCategory;
  label: string;
}

/**
 * Ordered list of categories as they appear in the tab strip.
 * Overview first, then the conceptual flow: graph modeling → skills →
 * instance execution → tutorials.
 */
export const HELP_CATEGORIES: ReadonlyArray<HelpCategoryInfo> = [
  { id: 'overview',       label: 'Overview' },
  { id: 'graph-model',    label: 'Graph Model' },
  { id: 'graph-skills',   label: 'Graph Skills' },
  { id: 'instance-model', label: 'Instance Model' },
  { id: 'tutorials',      label: 'Tutorials' },
];

/**
 * Instance Model topic keys — the smaller of the two command groups.
 * Only these need explicit listing; all other bare commands default
 * to Graph Model.
 */
const INSTANCE_MODEL_TOPICS: ReadonlySet<string> = new Set([
  'execute', 'inspect', 'instantiate', 'run', 'seen', 'upload',
]);

/**
 * Resolves the category for a topic key using prefix-based rules and
 * a small explicit set for instance-model commands:
 *
 *   ""             → overview      (root index page)
 *   "tutorial *"   → tutorials     (any number of tutorials)
 *   "graph-*"      → graph-skills  (any number of graph skills)
 *   INSTANCE_MODEL_TOPICS          → instance-model
 *   everything else                → graph-model (default for bare commands)
 */
export function resolveCategory(key: string): HelpCategory {
  if (key === '')                     return 'overview';
  if (key.startsWith('tutorial '))    return 'tutorials';
  if (key.startsWith('graph-'))       return 'graph-skills';
  if (INSTANCE_MODEL_TOPICS.has(key)) return 'instance-model';
  return 'graph-model';
}

/**
 * Returns the ordered page sequence for a given category.
 * For 'overview', returns [''] (the single root page).
 * For other categories, returns the alphabetically sorted topic keys.
 */
export function getCategoryPages(categoryId: HelpCategory): ReadonlyArray<string> {
  if (categoryId === 'overview') return [''];
  return HELP_TOPIC_KEYS.filter(key => resolveCategory(key) === categoryId);
}

/**
 * The full ordered page sequence across all categories for cross-category
 * scroll wrapping. Overview (root) first, then each category in display
 * order, topics within each category in alphabetical order.
 */
export const ORDERED_HELP_PAGES: ReadonlyArray<string> =
  HELP_CATEGORIES.flatMap(cat => getCategoryPages(cat.id));
