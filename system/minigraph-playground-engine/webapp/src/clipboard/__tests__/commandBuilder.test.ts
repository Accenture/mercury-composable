import { buildNodeCommand } from '../commandBuilder';
import type { MinigraphNode } from '../../utils/graphTypes';

function makeNode(properties: MinigraphNode['properties'], types: string[] = ['Decision']): MinigraphNode {
  return { alias: 'route-selector', types, properties };
}

describe('buildNodeCommand', () => {
  it('writes a scalar property as key=value, never with the [] append signature', () => {
    // The engine appends on `key[]=`, so `skill[]=graph.math` would store ["graph.math"].
    expect(buildNodeCommand('create', makeNode({ skill: 'graph.math', description: 'picks a route' }))).toBe([
      'create node route-selector',
      'with type Decision',
      'with properties',
      'description=picks a route',
      'skill=graph.math',
    ].join('\n'));
  });

  it('writes one key[]=element line per list element, in list order', () => {
    expect(buildNodeCommand('create', makeNode({
      skill: 'graph.math',
      mapping: ['input.body.a -> a', 'input.body.b -> b'],
      compute: ['total -> a + b'],
    }))).toBe([
      'create node route-selector',
      'with type Decision',
      'with properties',
      'compute[]=total -> a + b',
      'mapping[]=input.body.a -> a',
      'mapping[]=input.body.b -> b',
      'skill=graph.math',
    ].join('\n'));
  });

  it('flattens a nested map into dotted paths and prints numbers, booleans and null as text', () => {
    expect(buildNodeCommand('create', makeNode({
      config: { retries: 3, enabled: true, note: null },
      skill: 'graph.task',
    }))).toBe([
      'create node route-selector',
      'with type Decision',
      'with properties',
      'config.enabled=true',
      'config.note=null',
      'config.retries=3',
      'skill=graph.task',
    ].join('\n'));
  });

  it("wraps a multiline value in ''' like the engine's own edit node output", () => {
    expect(buildNodeCommand('create', makeNode({ description: 'line one\nline two' }))).toBe([
      'create node route-selector',
      'with type Decision',
      'with properties',
      "description='''",
      'line one\nline two',
      "'''",
    ].join('\n'));
  });

  it('writes an update command with the same body and the node alias as the original alias', () => {
    expect(buildNodeCommand('update', makeNode({ skill: 'graph.math' }))).toBe([
      'update node route-selector',
      'with type Decision',
      'with properties',
      'skill=graph.math',
    ].join('\n'));
  });

  it('omits the type and the property block when the node has neither', () => {
    expect(buildNodeCommand('create', makeNode({}, []))).toBe('create node route-selector');
  });

  it('refuses a node the command grammar cannot carry', () => {
    expect(() => buildNodeCommand('create', makeNode({ description: "has ''' inside" })))
      .toThrow(/cannot be pasted/);
    expect(() => buildNodeCommand('create', makeNode({ mapping: [] })))
      .toThrow(/cannot be pasted/);
    expect(() => buildNodeCommand('create', makeNode({ skill: 'graph.math' }, ['Decision', 'Extra'])))
      .toThrow(/cannot be pasted/);
  });

  it('refuses a reserved alias through the authoring validation', () => {
    expect(() => buildNodeCommand('create', { alias: 'model', types: ['Root'], properties: {} }))
      .toThrow(/reserved/);
  });
});
