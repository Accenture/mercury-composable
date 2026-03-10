/**
 * Command grammar for MiniGraph Playground autocomplete.
 *
 * Each entry describes a command template:
 *  - `tokens`   — the fixed keyword tokens that make up this suggestion,
 *                 exactly as they should appear in the input.
 *  - `template` — the full text inserted when the suggestion is accepted.
 *                 Placeholders use {curly-brace} notation for user-supplied values.
 *  - `hint`     — a short description shown next to the suggestion.
 *  - `multiline`— true when accepting the suggestion should also enable multiline mode.
 */

export interface CommandSuggestion {
  /** The ordered keywords that identify this suggestion (used for matching). */
  tokens: string[];
  /** Text inserted into the command input when the suggestion is accepted. */
  template: string;
  /** Short description displayed in the dropdown. */
  hint: string;
  /** When true the CommandInput multiline toggle should be activated. */
  multiline?: boolean;
}

// ---------------------------------------------------------------------------
// Built-in skill routes — shown as completions after "describe skill "
// ---------------------------------------------------------------------------
export const BUILTIN_SKILLS: string[] = [
  'graph.data.mapper',
  'graph.math',
  'graph.js',
  'graph.api.fetcher',
  'graph.extension',
  'graph.island',
  'graph.join',
];

// ---------------------------------------------------------------------------
// Quickstart reference — shown in the info-icon popover on the Command label.
// Each entry is a top-level keyword and a short description of what it does.
// ---------------------------------------------------------------------------
export interface QuickstartEntry {
  keyword:     string;
  description: string;
}

export const COMMAND_QUICKSTART: QuickstartEntry[] = [
  { keyword: 'help',        description: 'List all help topics or get help for a specific command' },
  { keyword: 'create',      description: 'Create a new graph node' },
  { keyword: 'update',      description: 'Update an existing node' },
  { keyword: 'delete',      description: 'Delete a node or a connection' },
  { keyword: 'connect',     description: 'Connect two nodes with a named relation' },
  { keyword: 'describe',    description: 'Describe the graph, a node, connection or skill' },
  { keyword: 'export',      description: 'Export the graph model to a JSON file' },
  { keyword: 'import',      description: 'Import a graph model from a JSON file' },
  { keyword: 'instantiate', description: 'Create a runnable graph instance' },
  { keyword: 'execute',     description: 'Execute a single node skill in isolation' },
  { keyword: 'inspect',     description: 'Inspect a state-machine variable' },
  { keyword: 'run',         description: 'Run the graph instance from root to end' },
  { keyword: 'close',       description: 'Close the current graph instance' },
];

// ---------------------------------------------------------------------------
// Full command grammar
// ---------------------------------------------------------------------------
export const COMMAND_SUGGESTIONS: CommandSuggestion[] = [
  // ── help ────────────────────────────────────────────────────────────────
  {
    tokens:   ['help'],
    template: 'help',
    hint:     'List all help topics',
  },
  {
    tokens:   ['help', 'create'],
    template: 'help create',
    hint:     'Help: create node',
  },
  {
    tokens:   ['help', 'update'],
    template: 'help update',
    hint:     'Help: update node',
  },
  {
    tokens:   ['help', 'delete'],
    template: 'help delete',
    hint:     'Help: delete node or connection',
  },
  {
    tokens:   ['help', 'connect'],
    template: 'help connect',
    hint:     'Help: connect two nodes',
  },
  {
    tokens:   ['help', 'describe'],
    template: 'help describe',
    hint:     'Help: describe graph / node / connection / skill',
  },
  {
    tokens:   ['help', 'export'],
    template: 'help export',
    hint:     'Help: export graph as a JSON file',
  },
  {
    tokens:   ['help', 'import'],
    template: 'help import',
    hint:     'Help: import graph from a JSON file',
  },
  {
    tokens:   ['help', 'instantiate'],
    template: 'help instantiate',
    hint:     'Help: instantiate a graph instance',
  },
  {
    tokens:   ['help', 'execute'],
    template: 'help execute',
    hint:     'Help: execute a node skill in isolation',
  },
  {
    tokens:   ['help', 'inspect'],
    template: 'help inspect',
    hint:     'Help: inspect state-machine variables',
  },
  {
    tokens:   ['help', 'run'],
    template: 'help run',
    hint:     'Help: run a graph instance end-to-end',
  },
  {
    tokens:   ['help', 'close'],
    template: 'help close',
    hint:     'Help: close a graph instance',
  },

  // ── create ──────────────────────────────────────────────────────────────
  {
    tokens:   ['create', 'node'],
    template: 'create node {name}\nwith type {type}\nwith properties\n{key}={value}',
    hint:     'Create a new graph node (multi-line)',
    multiline: true,
  },

  // ── update ──────────────────────────────────────────────────────────────
  {
    tokens:   ['update', 'node'],
    template: 'update node {name}\nwith type {type}\nwith properties\n{key}={value}',
    hint:     'Update an existing node (multi-line)',
    multiline: true,
  },

  // ── delete ──────────────────────────────────────────────────────────────
  {
    tokens:   ['delete', 'node'],
    template: 'delete node {name}',
    hint:     'Delete a node by name',
  },
  {
    tokens:   ['delete', 'connection'],
    template: 'delete connection {nodeA} and {nodeB}',
    hint:     'Delete connection(s) between two nodes',
  },

  // ── connect ─────────────────────────────────────────────────────────────
  {
    tokens:   ['connect'],
    template: 'connect {node-A} to {node-B} with {relation}',
    hint:     'Connect two nodes with a relation',
  },

  // ── describe ────────────────────────────────────────────────────────────
  {
    tokens:   ['describe', 'graph'],
    template: 'describe graph',
    hint:     'Describe the current graph model',
  },
  {
    tokens:   ['describe', 'node'],
    template: 'describe node {name}',
    hint:     'Describe a specific node',
  },
  {
    tokens:   ['describe', 'connection'],
    template: 'describe connection {node-A} and {node-B}',
    hint:     'Describe connection(s) between two nodes',
  },
  {
    tokens:   ['describe', 'skill'],
    template: 'describe skill {skill.route.name}',
    hint:     'Show documentation for a skill',
  },
  // Built-in skill shortcuts for "describe skill <route>"
  ...BUILTIN_SKILLS.map(skill => ({
    tokens:   ['describe', 'skill', skill],
    template: `describe skill ${skill}`,
    hint:     `Describe built-in skill: ${skill}`,
  })),

  // ── export / import ─────────────────────────────────────────────────────
  {
    tokens:   ['export', 'graph', 'as'],
    template: 'export graph as {name}',
    hint:     'Export the graph model to a named JSON file',
  },
  {
    tokens:   ['import', 'graph', 'from'],
    template: 'import graph from {name}',
    hint:     'Import a graph model from a named JSON file',
  },

  // ── instantiate ─────────────────────────────────────────────────────────
  {
    tokens:   ['instantiate', 'graph'],
    template: 'instantiate graph\n{constant} -> input.body.{key}',
    hint:     'Create a graph instance (multi-line)',
    multiline: true,
  },

  // ── execute ─────────────────────────────────────────────────────────────
  {
    tokens:   ['execute', 'node'],
    template: 'execute node {name}',
    hint:     'Execute the skill of a single node in isolation',
  },

  // ── inspect ─────────────────────────────────────────────────────────────
  {
    tokens:   ['inspect'],
    template: 'inspect {variable_name}',
    hint:     'Inspect a state-machine variable',
  },

  // ── run ─────────────────────────────────────────────────────────────────
  {
    tokens:   ['run'],
    template: 'run',
    hint:     'Run the graph instance from root to end',
  },

  // ── close ───────────────────────────────────────────────────────────────
  {
    tokens:   ['close'],
    template: 'close',
    hint:     'Close the current graph instance',
  },
];

// ---------------------------------------------------------------------------
// Matching logic
// ---------------------------------------------------------------------------

/**
 * Given the current single-line command text, return the subset of
 * COMMAND_SUGGESTIONS that are relevant completions.
 *
 * Matching rules (all case-insensitive):
 *  1. Split the input on whitespace.
 *  2. A suggestion matches if every input token is a prefix of the
 *     corresponding suggestion token at the same position — AND the input
 *     has fewer tokens than (or equal tokens to) the suggestion.
 *  3. Exact full matches (input === template) are excluded so the dropdown
 *     disappears once the user has already completed the command.
 */
export function getSuggestions(rawInput: string): CommandSuggestion[] {
  // Only operate on the first line — multi-line mode has its own flow.
  const firstLine = rawInput.split('\n')[0];
  // trimStart (not trim) so a trailing space like "describe " still matches
  // sub-commands (the user has finished typing the first token).
  const trimmed   = firstLine.trimStart();
  if (trimmed === '') return [];

  const inputTokens = trimmed.toLowerCase().split(/\s+/);

  return COMMAND_SUGGESTIONS.filter(suggestion => {
    const { tokens, template } = suggestion;

    // Don't suggest if the input is already the completed template.
    if (trimmed.toLowerCase() === template.split('\n')[0].toLowerCase()) return false;

    // The suggestion must have at least as many tokens as the input.
    if (tokens.length < inputTokens.length) return false;

    // Every input token must be a prefix of the corresponding suggestion token.
    return inputTokens.every((inputTok, i) =>
      tokens[i]?.startsWith(inputTok)
    );
  });
}
