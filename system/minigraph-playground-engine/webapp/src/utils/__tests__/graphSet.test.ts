import { describe, expect, it } from 'vitest';
import {
  buildPackRequest,
  entryIdFromFileName,
  entryIssues,
  isPackFile,
  manifestRowIssues,
  manifestRowsOf,
  packFileName,
  parseUnpackAnswer,
  validateSetName,
  type GraphSetEntry,
  type ManifestRow,
} from '../graphSet';

const MODEL = {
  nodes: [
    { alias: 'root', types: ['Root'], properties: { name: 'demo' } },
    { alias: 'end', types: ['End'], properties: {} },
  ],
  connections: [{ source: 'root', target: 'end', relations: [{ type: 'finish', properties: {} }] }],
};

function entry(key: number, id: string, name: string | null = id): GraphSetEntry {
  return { key, id, label: `${id}.json`, model: MODEL, name, nodeCount: 2, connectionCount: 1, source: 'file' };
}

function row(key: number, name: string, value: string): ManifestRow {
  return { key, name, value };
}

describe('file names', () => {
  it('takes the graph id from <graph-id>.json, whatever the case of the extension', () => {
    expect(entryIdFromFileName('tutorial-1.json')).toBe('tutorial-1');
    expect(entryIdFromFileName(' Tutorial_2.JSON ')).toBe('Tutorial_2');
    expect(entryIdFromFileName('notes.txt')).toBe('');
    expect(entryIdFromFileName('.json')).toBe('');
  });

  it('recognises a .pack by its extension and names the set file', () => {
    expect(isPackFile(new File([''], 'demo.pack'))).toBe(true);
    expect(isPackFile(new File([''], 'DEMO.PACK'))).toBe(true);
    expect(isPackFile(new File([''], 'demo.json'))).toBe(false);
    expect(packFileName('demo')).toBe('demo.pack');
  });
});

describe('validateSetName', () => {
  it('requires a name that follows the file-name rule', () => {
    expect(validateSetName('demo-set_1')).toBeNull();
    expect(validateSetName('  demo  ')).toBeNull();
    expect(validateSetName('')).toContain('required');
    expect(validateSetName('   ')).toContain('required');
    expect(validateSetName('my set')).toContain('letters, digits, hyphen or underscore');
    expect(validateSetName('demo.pack')).toContain('letters, digits, hyphen or underscore');
  });
});

describe('entryIssues', () => {
  it('passes entries whose ids follow the rule and match their root names', () => {
    expect(entryIssues([entry(1, 'a'), entry(2, 'b', null)]).size).toBe(0);
  });

  it('flags an id that breaks the file-name rule, by the entry', () => {
    const issues = entryIssues([entry(1, ''), entry(2, 'my graph'), entry(3, 'ok')]);
    expect(issues.get(1)).toContain('name the file <graph-id>.json');
    expect(issues.get(2)).toContain('letters, digits, hyphen or underscore');
    expect(issues.has(3)).toBe(false);
  });

  it("flags a root name that differs from the id (the engine's root-name rule)", () => {
    const issues = entryIssues([entry(1, 'tutorial-1', 'tutorial-2')]);
    expect(issues.get(1)).toBe('The root node\'s name "tutorial-2" differs from the graph id.');
  });

  it('flags every entry of a duplicated id', () => {
    const issues = entryIssues([entry(1, 'a'), entry(2, 'a'), entry(3, 'b')]);
    expect(issues.get(1)).toContain('Duplicate graph id "a"');
    expect(issues.get(2)).toContain('Duplicate graph id "a"');
    expect(issues.has(3)).toBe(false);
  });
});

describe('manifestRowIssues', () => {
  const ids = new Set(['a', 'b']);

  it('ignores blank rows and named rows with a blank value (the suggested rows start that way)', () => {
    expect(manifestRowIssues([row(1, '', ''), row(2, 'version', ''), row(3, 'description', '  ')], ids).size).toBe(0);
  });

  it('needs a name for a value', () => {
    expect(manifestRowIssues([row(1, '', '1.0')], ids).get(1)).toBe('A value needs a field name.');
  });

  it("refuses the fields the engine writes itself, as the engine refuses them", () => {
    const issues = manifestRowIssues([row(1, 'set', 'x'), row(2, 'format', 'x'), row(3, 'format_version', 'x')], ids);
    expect(issues.get(1)).toBe('"set" is written from the set name.');
    expect(issues.get(2)).toBe('"format" is written by the packager.');
    expect(issues.get(3)).toBe('"format_version" is written by the packager.');
  });

  it('flags a name used twice, trimmed', () => {
    const issues = manifestRowIssues([row(1, 'version', '1'), row(2, ' version ', '2')], ids);
    expect(issues.get(1)).toBe('Duplicate field "version".');
    expect(issues.get(2)).toBe('Duplicate field "version".');
  });

  it('checks graph_id against the entries', () => {
    expect(manifestRowIssues([row(1, 'graph_id', 'a')], ids).size).toBe(0);
    expect(manifestRowIssues([row(1, 'graph_id', 'c')], ids).get(1)).toBe('"c" is not a graph of the set.');
    // a blank graph_id is simply not sent
    expect(manifestRowIssues([row(1, 'graph_id', '')], ids).size).toBe(0);
  });
});

describe('buildPackRequest', () => {
  it('puts the set name in manifest.set, the filled rows after it, and the graphs by id', () => {
    const request = buildPackRequest(' demo ', [row(1, 'version', ' 1.0 '), row(2, 'description', ''), row(3, '', '')],
      [entry(1, 'a'), entry(2, 'b')]);
    expect(Object.keys(request.manifest)).toEqual(['set', 'version']);
    expect(request.manifest).toEqual({ set: 'demo', version: '1.0' });
    expect(Object.keys(request.graphs)).toEqual(['a', 'b']);
    expect(request.graphs.a).toBe(MODEL);
  });
});

describe('parseUnpackAnswer', () => {
  it('reads the manifest and the graphs with their counts, in the order answered', () => {
    const set = parseUnpackAnswer({ manifest: { set: 'demo', version: '1' }, graphs: { b: MODEL, a: MODEL } });
    expect(set.manifest).toEqual({ set: 'demo', version: '1' });
    expect(set.graphs.map(graph => graph.id)).toEqual(['b', 'a']);
    expect(set.graphs[0]).toMatchObject({ name: 'demo', nodeCount: 2, connectionCount: 1 });
    expect(set.graphs[0].model).toBe(MODEL);
  });

  it('refuses a shape it does not understand, by name', () => {
    expect(() => parseUnpackAnswer(null)).toThrow("expected 'manifest' and 'graphs'");
    expect(() => parseUnpackAnswer({ manifest: {}, graphs: [] })).toThrow("expected 'manifest' and 'graphs'");
    expect(() => parseUnpackAnswer({ manifest: { set: 1 }, graphs: {} })).toThrow("Manifest field 'set' is not text.");
    expect(() => parseUnpackAnswer({ manifest: {}, graphs: { a: { nodes: [] } } })).toThrow("Graph 'a':");
  });
});

describe('manifestRowsOf', () => {
  it("keeps the caller fields in order and leaves out the engine's own", () => {
    let next = 10;
    const rows = manifestRowsOf({ format: 'x', version: '1', set: 'demo', format_version: '1', author: 'me' }, () => next++);
    expect(rows).toEqual([{ key: 10, name: 'version', value: '1' }, { key: 11, name: 'author', value: 'me' }]);
  });
});
