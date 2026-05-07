import { describe, expect, it } from 'vitest';
import { getValidationErrorKeyForProperty, validateNodeDraft } from '../validation';
import type { NodeDraft } from '../nodeAuthoringTypes';

function draft(overrides: Partial<NodeDraft> = {}): NodeDraft {
  return {
    alias: 'node-1',
    nodeType: 'Fetcher',
    properties: [],
    source: 'pane-context-menu',
    ...overrides,
  };
}

describe('validateNodeDraft', () => {
  it('requires alias', () => {
    const result = validateNodeDraft(draft({ alias: '   ' }));
    expect(result.errors.alias).toBeDefined();
  });

  it('rejects reserved aliases case-insensitively', () => {
    const result = validateNodeDraft(draft({ alias: 'Input' }));
    expect(result.errors.alias).toContain('reserved');
  });

  it('rejects duplicate aliases known in current graph data case-insensitively', () => {
    const result = validateNodeDraft(draft({ alias: 'Root' }), {
      graphData: {
        nodes: [{ alias: 'root', types: ['Root'], properties: {} }],
        connections: [],
      },
    });
    expect(result.errors.alias).toContain('already exists');
  });

  it('rejects invalid node type token', () => {
    const result = validateNodeDraft(draft({ nodeType: 'Root.Type' }));
    expect(result.errors.nodeType).toBeDefined();
  });

  it('ignores fully blank property rows', () => {
    const result = validateNodeDraft(draft({
      properties: [{ id: 'p1', key: ' ', value: ' ' }],
    }));
    expect(result.valid).toBe(true);
  });

  it('allows a non-blank key with blank value', () => {
    const result = validateNodeDraft(draft({
      properties: [{ id: 'p1', key: 'name', value: ' ' }],
    }));
    expect(result.valid).toBe(true);
  });

  it('rejects a blank key with non-blank value', () => {
    const result = validateNodeDraft(draft({
      properties: [{ id: 'p1', key: ' ', value: 'demo' }],
    }));
    expect(result.errors[getValidationErrorKeyForProperty('p1', 'key')]).toBeDefined();
  });

  it('rejects property keys outside the backend name token', () => {
    const result = validateNodeDraft(draft({
      properties: [{ id: 'p1', key: 'a.b', value: 'demo' }],
    }));
    expect(result.errors[getValidationErrorKeyForProperty('p1', 'key')]).toBeDefined();
  });

  it('rejects multiline and triple-quote property values', () => {
    const newline = validateNodeDraft(draft({
      properties: [{ id: 'p1', key: 'name', value: 'a\nb' }],
    }));
    const tripleQuote = validateNodeDraft(draft({
      properties: [{ id: 'p2', key: 'name', value: "a'''b" }],
    }));
    expect(newline.errors[getValidationErrorKeyForProperty('p1', 'value')]).toBeDefined();
    expect(tripleQuote.errors[getValidationErrorKeyForProperty('p2', 'value')]).toBeDefined();
  });
});
