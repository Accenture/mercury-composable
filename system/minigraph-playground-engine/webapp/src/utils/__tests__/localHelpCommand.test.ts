import { describe, it, expect, vi, beforeEach } from 'vitest';
import { resolveBundledHelpTopic } from '../localHelpCommand';

// getHelpContent uses import.meta.glob (Vite-only) — mock at the module level.
vi.mock('../../data/helpContent', () => ({
  getHelpContent: (topic: string, profile = 'minigraph'): string | null => {
    if (profile === 'json-path') return topic === '' ? '# JSON-Path Overview' : null;
    // Simulate the bundled topics that exist in src/main/resources/help/
    const bundled = new Set(['', 'create', 'export', 'import', 'list',
      'tutorial 1', 'tutorial 2', 'tutorial 3', 'tutorial 4', 'tutorial 5', 'tutorial 6',
      'graph-api-fetcher', 'graph-js', 'graph-join', 'graph-extension', 'graph-data-mapper',
      'graph-math', 'edit', 'delete', 'update', 'run', 'upload', 'inspect']);
    return bundled.has(topic) ? `# help ${topic}` : null;
  },
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('resolveBundledHelpTopic', () => {
  it('returns "" for plain "help" when supportsHelp is true', () => {
    expect(resolveBundledHelpTopic('help', true)).toBe('');
  });

  it('returns "create" for "help create" when supportsHelp is true', () => {
    expect(resolveBundledHelpTopic('help create', true)).toBe('create');
  });

  it('returns null for an unbundled topic', () => {
    expect(resolveBundledHelpTopic('help missing-topic-xyz', true)).toBeNull();
  });

  it('returns null for a describe command', () => {
    expect(resolveBundledHelpTopic('describe graph', true)).toBeNull();
  });

  it('returns null when supportsHelp is false', () => {
    expect(resolveBundledHelpTopic('help create', false)).toBeNull();
  });

  it('is case-insensitive for the help prefix', () => {
    expect(resolveBundledHelpTopic('HELP Create', true)).toBe('create');
  });

  it('returns null for a non-help, non-describe command', () => {
    expect(resolveBundledHelpTopic('export graph as foo', true)).toBeNull();
  });

  it('handles only the Overview command locally for the JSON Path profile', () => {
    expect(resolveBundledHelpTopic('help', true, 'json-path')).toBe('');
    expect(resolveBundledHelpTopic('help create', true, 'json-path')).toBeNull();
    expect(resolveBundledHelpTopic('describe skill graph.math', true, 'json-path')).toBeNull();
  });
});

describe('resolveBundledHelpTopic for describe skill', () => {
  it("resolves a built-in skill to its help page, as the engine does ('.' becomes '-')", () => {
    expect(resolveBundledHelpTopic('describe skill graph.math', true)).toBe('graph-math');
    expect(resolveBundledHelpTopic('describe skill graph.api.fetcher', true)).toBe('graph-api-fetcher');
    expect(resolveBundledHelpTopic('describe skill graph.data.mapper', true)).toBe('graph-data-mapper');
  });

  it('is case-insensitive and tolerates extra spaces, like the engine', () => {
    expect(resolveBundledHelpTopic('Describe Skill Graph.JS', true)).toBe('graph-js');
    expect(resolveBundledHelpTopic('  describe   skill   graph.join  ', true)).toBe('graph-join');
  });

  it('accepts the hyphenated page name, which the engine also accepts', () => {
    expect(resolveBundledHelpTopic('describe skill graph-extension', true)).toBe('graph-extension');
  });

  it('sends a skill without a bundled page to the backend', () => {
    expect(resolveBundledHelpTopic('describe skill my.custom.skill', true)).toBeNull();
  });

  it('keeps describe graph, node and connection on the backend (their answers stay in the console)', () => {
    expect(resolveBundledHelpTopic('describe graph', true)).toBeNull();
    expect(resolveBundledHelpTopic('describe node fetcher', true)).toBeNull();
    expect(resolveBundledHelpTopic('describe connection fetcher and decision', true)).toBeNull();
  });

  it('maps only the exact three-word form the engine answers', () => {
    expect(resolveBundledHelpTopic('describe skill', true)).toBeNull();
    expect(resolveBundledHelpTopic('describe skill graph.math now', true)).toBeNull();
  });

  it('returns null when supportsHelp is false', () => {
    expect(resolveBundledHelpTopic('describe skill graph.math', false)).toBeNull();
  });
});
