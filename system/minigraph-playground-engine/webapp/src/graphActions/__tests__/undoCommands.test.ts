import { describe, expect, it } from 'vitest';
import {
  buildConnectionCreateUndo,
  buildConnectionDeleteUndo,
  buildNodeCreateUndo,
  buildNodeDeleteUndo,
  buildNodeEditUndo,
} from '../undoCommands';
import type { MinigraphGraphData } from '../../utils/graphTypes';

function graph(): MinigraphGraphData {
  return {
    nodes: [
      { alias: 'root', types: ['Root'], properties: { skill: 'graph.data.mapper' } },
      { alias: 'fetcher', types: ['Fetcher'], properties: { input: ['a', 'b'], skill: 'graph.api.fetcher' } },
      { alias: 'end', types: ['End'], properties: {} },
    ],
    connections: [
      { source: 'root', target: 'fetcher', relations: [{ type: 'fetch', properties: {} }] },
      { source: 'fetcher', target: 'end', relations: [{ type: 'complete', properties: {} }, { type: 'done', properties: {} }] },
      { source: 'end', target: 'fetcher', relations: [{ type: 'retry', properties: {} }] },
    ],
  };
}

describe('buildConnectionDeleteUndo', () => {
  it('recreates every relation of the pair in BOTH directions', () => {
    const entry = buildConnectionDeleteUndo(graph(), 'fetcher', 'end');
    expect(entry?.inverseCommands).toEqual([
      'connect fetcher to end with complete',
      'connect fetcher to end with done',
      'connect end to fetcher with retry',
    ]);
  });

  it('returns null when the pair has no connections', () => {
    expect(buildConnectionDeleteUndo(graph(), 'root', 'end')).toBeNull();
  });
});

describe('buildNodeEditUndo', () => {
  it('rebuilds the full pre-edit node as an update command', () => {
    const entry = buildNodeEditUndo(graph().nodes[1]);
    expect(entry?.label).toBe('edit node fetcher');
    expect(entry?.inverseCommands).toEqual([[
      'update node fetcher',
      'with type Fetcher',
      'with properties',
      'input[]=a',
      'input[]=b',
      'skill=graph.api.fetcher',
    ].join('\n')]);
  });
});

describe('buildNodeCreateUndo', () => {
  it('deletes the created node', () => {
    expect(buildNodeCreateUndo('fetcher')?.inverseCommands).toEqual(['delete node fetcher']);
  });
});

describe('buildConnectionCreateUndo', () => {
  it('deletes the pair and restores the pre-existing relations', () => {
    const entry = buildConnectionCreateUndo(graph(), 'fetcher', 'end');
    expect(entry?.inverseCommands).toEqual([
      'delete connection fetcher and end',
      'connect fetcher to end with complete',
      'connect fetcher to end with done',
      'connect end to fetcher with retry',
    ]);
  });

  it('only deletes when the pair had no prior relations', () => {
    const entry = buildConnectionCreateUndo(graph(), 'root', 'end');
    expect(entry?.inverseCommands).toEqual(['delete connection root and end']);
  });
});

describe('buildNodeDeleteUndo', () => {
  it('recreates the node and reconnects every relation touching it', () => {
    const entry = buildNodeDeleteUndo(graph(), graph().nodes[1]);
    expect(entry?.inverseCommands).toEqual([
      [
        'create node fetcher',
        'with type Fetcher',
        'with properties',
        'input[]=a',
        'input[]=b',
        'skill=graph.api.fetcher',
      ].join('\n'),
      'connect root to fetcher with fetch',
      'connect fetcher to end with complete',
      'connect fetcher to end with done',
      'connect end to fetcher with retry',
    ]);
  });
});
