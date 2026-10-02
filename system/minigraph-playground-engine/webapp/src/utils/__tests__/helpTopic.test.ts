import { describe, expect, it } from 'vitest';
import { extractCommandHelpTopic, extractHelpTopic } from '../helpTopic';

describe('extractHelpTopic', () => {
  it('strips the help prefix and lowercases the topic', () => {
    expect(extractHelpTopic('help create')).toBe('create');
    expect(extractHelpTopic('help')).toBe('');
    expect(extractHelpTopic('HELP Tutorial 1')).toBe('tutorial 1');
  });
});

describe('extractCommandHelpTopic', () => {
  it('maps a help command to its topic', () => {
    expect(extractCommandHelpTopic('help create')).toBe('create');
    expect(extractCommandHelpTopic('help')).toBe('');
    expect(extractCommandHelpTopic('  HELP Tutorial 1 ')).toBe('tutorial 1');
  });

  it("maps describe skill to the page the engine answers with: lowercased, '.' becomes '-'", () => {
    expect(extractCommandHelpTopic('describe skill graph.math')).toBe('graph-math');
    expect(extractCommandHelpTopic('DESCRIBE SKILL Graph.Data.Mapper')).toBe('graph-data-mapper');
    // a route without a bundled page still maps; the caller checks for content
    expect(extractCommandHelpTopic('describe skill my.custom.skill')).toBe('my-custom-skill');
  });

  it('returns null for the describe commands that answer in the console', () => {
    expect(extractCommandHelpTopic('describe graph')).toBeNull();
    expect(extractCommandHelpTopic('describe node fetcher')).toBeNull();
    expect(extractCommandHelpTopic('describe connection a and b')).toBeNull();
  });

  it('returns null unless describe skill has exactly one route', () => {
    expect(extractCommandHelpTopic('describe skill')).toBeNull();
    expect(extractCommandHelpTopic('describe skill graph.math extra')).toBeNull();
  });

  it('returns null for other commands, including words that only start with help', () => {
    expect(extractCommandHelpTopic('helpful')).toBeNull();
    expect(extractCommandHelpTopic('export graph as demo')).toBeNull();
  });
});
