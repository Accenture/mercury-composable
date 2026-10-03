import { describe, expect, it } from 'vitest';
import {
  buildGraphFileText,
  graphFileName,
  isValidGraphId,
  suggestGraphId,
  validateGraphModel,
} from '../graphFile';
import type { MinigraphGraphData } from '../graphTypes';

const MODEL: MinigraphGraphData = {
  nodes: [
    { alias: 'root', types: ['Root'], properties: { name: 'demo' } },
    { alias: 'end', types: ['End'], properties: {} },
  ],
  connections: [
    { source: 'root', target: 'end', relations: [{ type: 'finish', properties: {} }] },
  ],
};

describe('validateGraphModel', () => {
  it('accepts nodes with connections and reports the root name and the counts', () => {
    const result = validateGraphModel(MODEL);
    expect(result).toMatchObject({ ok: true, name: 'demo', nodeCount: 2, connectionCount: 1 });
    expect(result.ok && result.model).toBe(MODEL);
  });

  it('accepts a work in progress without connections', () => {
    expect(validateGraphModel({ nodes: MODEL.nodes }))
      .toMatchObject({ ok: true, nodeCount: 2, connectionCount: 0 });
  });

  it('refuses anything that is not a JSON object', () => {
    for (const value of [null, 'text', 42, [MODEL]]) {
      const result = validateGraphModel(value);
      expect(result.ok).toBe(false);
      expect(!result.ok && result.error).toContain("JSON object with a 'nodes' section");
    }
  });

  it('refuses foreign top-level sections and names them, sorted', () => {
    const result = validateGraphModel({ nodes: MODEL.nodes, zeta: 1, alpha: 2 });
    expect(result.ok).toBe(false);
    expect(!result.ok && result.error).toBe(
      "Unexpected top-level sections: alpha, zeta. A graph model has only 'nodes' and 'connections'.",
    );
  });

  it('requires nodes to be a non-empty list', () => {
    expect(validateGraphModel({ nodes: 'root' })).toMatchObject({ ok: false, error: "The 'nodes' section is mandatory and must be a list." });
    expect(validateGraphModel({ connections: [] })).toMatchObject({ ok: false, error: "The 'nodes' section is mandatory and must be a list." });
    expect(validateGraphModel({ nodes: [] })).toMatchObject({ ok: false, error: "The 'nodes' section is empty - there is nothing to import." });
  });

  it('requires each node to carry an alias and types', () => {
    expect(validateGraphModel({ nodes: [{ types: ['Root'] }] })).toMatchObject({ ok: false, error: 'Node entry 1 has no alias.' });
    expect(validateGraphModel({ nodes: [{ alias: 'a', types: [] }] })).toMatchObject({ ok: false, error: "Node entry 1 ('a') has no types." });
    expect(validateGraphModel({ nodes: [{ alias: 'a', types: ['T'], properties: 'x' }] }))
      .toMatchObject({ ok: false, error: "Node entry 1 ('a') has properties that are not an object." });
    expect(validateGraphModel({ nodes: ['a'] })).toMatchObject({ ok: false, error: 'Node entry 1 is not an object.' });
  });

  it('requires connections to be a list of source and target pairs', () => {
    expect(validateGraphModel({ nodes: MODEL.nodes, connections: 'x' }))
      .toMatchObject({ ok: false, error: "The 'connections' section must be a list." });
    expect(validateGraphModel({ nodes: MODEL.nodes, connections: [{ source: 'root' }] }))
      .toMatchObject({ ok: false, error: "Connection entry 1 needs a 'source' and a 'target'." });
  });
});

describe('graph ids', () => {
  it('follows the engine rule: letters, digits, hyphen and underscore', () => {
    expect(isValidGraphId('tutorial-1')).toBe(true);
    expect(isValidGraphId('my_graph')).toBe(true);
    expect(isValidGraphId('my graph')).toBe(false);
    expect(isValidGraphId('graph.json')).toBe(false);
    expect(isValidGraphId('')).toBe(false);
  });

  it('suggests an id from a display name and falls back to untitled', () => {
    expect(suggestGraphId('My Graph (v2)')).toBe('My-Graph-v2');
    expect(suggestGraphId('tutorial-13')).toBe('tutorial-13');
    expect(suggestGraphId('  ')).toBe('untitled');
    expect(suggestGraphId(undefined)).toBe('untitled');
  });

  it('always saves as .json', () => {
    expect(graphFileName('demo')).toBe('demo.json');
  });
});

describe('buildGraphFileText', () => {
  it('names the root node after the graph id, as export graph as does, without touching the live graph', () => {
    const text = buildGraphFileText(MODEL, 'renamed');
    const parsed = JSON.parse(text) as MinigraphGraphData;
    expect(parsed.nodes.find(node => node.alias === 'root')?.properties.name).toBe('renamed');
    expect(parsed.nodes.find(node => node.alias === 'end')).toEqual(MODEL.nodes[1]);
    expect(parsed.connections).toEqual(MODEL.connections);
    expect(MODEL.nodes[0].properties.name).toBe('demo');
    expect(text.endsWith('\n')).toBe(true);
  });

  it('leaves a graph without a root node as it is', () => {
    const noRoot: MinigraphGraphData = { nodes: [MODEL.nodes[1]], connections: [] };
    expect(JSON.parse(buildGraphFileText(noRoot, 'x'))).toEqual(noRoot);
  });
});
