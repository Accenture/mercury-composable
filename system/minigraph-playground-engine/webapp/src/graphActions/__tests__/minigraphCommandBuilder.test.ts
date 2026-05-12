import { describe, expect, it } from 'vitest';
import { buildCreateNodeCommand } from '../minigraphCommandBuilder';
import type { NodeDraft } from '../nodeAuthoringTypes';

function draft(overrides: Partial<NodeDraft> = {}): NodeDraft {
  return {
    alias: 'root',
    nodeType: 'Root',
    properties: [
      { id: 'p1', key: 'name', value: 'demo' },
      { id: 'p2', key: '', value: '' },
    ],
    source: 'empty-graph',
    ...overrides,
  };
}

describe('buildCreateNodeCommand', () => {
  it('emits backend create-node command text without trailing newline', () => {
    expect(buildCreateNodeCommand(draft())).toBe([
      'create node root',
      'with type Root',
      'with properties',
      'name=demo',
    ].join('\n'));
  });

  it('omits node type and properties when blank', () => {
    expect(buildCreateNodeCommand(draft({
      nodeType: '  ',
      properties: [{ id: 'p1', key: ' ', value: ' ' }],
    }))).toBe('create node root');
  });

  it('preserves property row order and allows blank values', () => {
    expect(buildCreateNodeCommand(draft({
      properties: [
        { id: 'p1', key: 'first', value: 'one' },
        { id: 'p2', key: 'second', value: '' },
      ],
    }))).toBe([
      'create node root',
      'with type Root',
      'with properties',
      'first=one',
      'second=',
    ].join('\n'));
  });

  it('rejects alias line injection before serialization', () => {
    expect(() => buildCreateNodeCommand(draft({ alias: 'root\nwith properties' }))).toThrow();
  });

  it('rejects node type line injection before serialization', () => {
    expect(() => buildCreateNodeCommand(draft({ nodeType: 'Root\rwith properties' }))).toThrow();
  });

  it('rejects property keys containing equals before serialization', () => {
    expect(() => buildCreateNodeCommand(draft({
      properties: [{ id: 'p1', key: 'bad=key', value: 'value' }],
    }))).toThrow();
  });

  it('rejects property value newline injection', () => {
    expect(() => buildCreateNodeCommand(draft({
      properties: [{ id: 'p1', key: 'name', value: 'demo\nwith properties' }],
    }))).toThrow();
  });

  it('rejects multiline property delimiters', () => {
    expect(() => buildCreateNodeCommand(draft({
      properties: [{ id: 'p1', key: 'name', value: "'''demo" }],
    }))).toThrow();
  });
});
