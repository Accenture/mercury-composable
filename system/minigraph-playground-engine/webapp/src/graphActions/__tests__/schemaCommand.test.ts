import { describe, expect, it } from 'vitest';
import { buildSchemaUpdateCommand } from '../schemaCommand';
import type { MinigraphNode } from '../../utils/graphTypes';

const ROOT: MinigraphNode = {
  alias: 'root',
  types: ['Root'],
  properties: {
    name: 'demo',
    purpose: 'A demo',
    mapping: ['input.body.a -> model.a', 'input.body.b -> model.b'],
    schema: {
      body: { type: 'object', properties: { old: { type: 'string' } } },
    },
  },
};

describe('buildSchemaUpdateCommand', () => {
  it('re-sends the node as the editor would, with the schema rows replaced', () => {
    const command = buildSchemaUpdateCommand(ROOT, {
      body: { type: 'object', properties: { a: { type: 'number' } }, required: ['a'] },
      header: { type: 'object', properties: { 'X-Tenant': { type: 'string' } } },
    });
    expect(command.split('\n')).toEqual([
      'update node root',
      'with type Root',
      'with properties',
      'mapping[]=input.body.a -> model.a',
      'mapping[]=input.body.b -> model.b',
      'name=demo',
      'purpose=A demo',
      'schema.body.type=object',
      'schema.body.properties.a.type=number',
      'schema.body.required[]=a',
      'schema.header.type=object',
      'schema.header.properties.X-Tenant.type=string',
    ]);
  });

  it('removes the declaration when the schema is null', () => {
    const command = buildSchemaUpdateCommand(ROOT, null);
    expect(command).not.toContain('schema');
    expect(command).toContain('purpose=A demo');
  });

  it('refuses a node the editor cannot represent', () => {
    expect(() => buildSchemaUpdateCommand({ ...ROOT, types: ['Root', 'Other'] }, null))
      .toThrow(/cannot be safely represented/);
  });
});
