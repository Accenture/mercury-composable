import { describe, expect, it } from 'vitest';
import {
  buildPartSchema,
  buildSchemaProperty,
  mergeRunRows,
  parseContractView,
  parsePath,
  relativePath,
  rowsFromContractPart,
  rowsFromValue,
  schemaAt,
  schemaRowIssues,
  schemaToPropertyRows,
  summarizeConstraints,
  type ContractPartView,
  type SchemaRow,
} from '../graphSchema';

let keys = 0;
const nextKey = () => ++keys;

function row(partial: Partial<SchemaRow> & { path: string }): SchemaRow {
  return {
    key: nextKey(), type: '', itemType: '', required: false, description: '', example: '',
    source: 'new', usedBy: [], constraints: {}, ...partial,
  };
}

/** The `declared-schema` case of the shared fixture `graph-contract-vectors.json`, input body side. */
const DECLARED_BODY: ContractPartView = {
  declared: true,
  schema: {
    type: 'object',
    properties: {
      a: { type: 'number', description: 'first operand' },
      b: { type: 'integer', minimum: 0 },
      c: { type: 'string', enum: ['x', 'y'] },
      d: { description: 'Referenced by compute' },
    },
    required: ['a', 'b'],
  },
  paths: [
    { path: 'input.body.a', type: 'number', origin: 'both', required: true, usedBy: ['compute'] },
    { path: 'input.body.b', type: 'integer', origin: 'both', required: true, usedBy: ['compute'] },
    { path: 'input.body.c', type: 'string', origin: 'declared' },
    { path: 'input.body.d', origin: 'discovered', usedBy: ['compute'] },
  ],
};
const DECLARED_NODE_BODY = {
  type: 'object',
  properties: {
    a: { type: 'number', description: 'first operand' },
    b: { type: 'integer', minimum: 0 },
    c: { type: 'string', enum: ['x', 'y'] },
  },
  required: ['a', 'b'],
};

describe('paths', () => {
  it('parses the grammar and refuses what it cannot carry', () => {
    expect(parsePath('items[].sku')).toEqual([{ name: 'items', array: true }, { name: 'sku', array: false }]);
    expect(parsePath('profile.name')).toEqual([{ name: 'profile', array: false }, { name: 'name', array: false }]);
    expect(parsePath('')).toBeNull();
    expect(parsePath('a b')).toBeNull();
    expect(parsePath('a[0]')).toBeNull();
    expect(parsePath('a..b')).toBeNull();
  });

  it('strips the namespace and reads the engine markers as []', () => {
    expect(relativePath('input.body.items[*].sku', 'input.body')).toBe('items[].sku');
    expect(relativePath('output.body.rows[0]', 'output.body')).toBe('rows[]');
    expect(relativePath('input.header.X-Api-Key', 'input.header')).toBe('X-Api-Key');
  });

  it('walks a schema by path through properties and items', () => {
    const schema = { type: 'object', properties: { items: { type: 'array', items: { type: 'object', properties: { sku: { type: 'string' } } } } } };
    expect(schemaAt(schema, 'items[].sku')).toEqual({ type: 'string' });
    expect(schemaAt(schema, 'items[]')).toEqual({ type: 'object', properties: { sku: { type: 'string' } } });
    expect(schemaAt(schema, 'missing')).toBeUndefined();
  });
});

describe('rowsFromContractPart', () => {
  it('reads the fixture case: types and chips from the view, text and constraints from the declaration only', () => {
    const rows = rowsFromContractPart(DECLARED_BODY, DECLARED_NODE_BODY, 'body', 'input.body', nextKey);
    expect(rows.map(r => [r.path, r.type, r.required, r.source, r.description, r.usedBy])).toEqual([
      ['a', 'number', true, 'both', 'first operand', ['compute']],
      ['b', 'integer', true, 'both', '', ['compute']],
      ['c', 'string', false, 'declared', '', []],
      ['d', '', false, 'discovered', '', ['compute']],     // never "Referenced by compute"
    ]);
    expect(rows[1].constraints).toEqual({ minimum: 0 });
    expect(rows[2].constraints).toEqual({ enum: ['x', 'y'] });
    expect(rows[3].constraints).toEqual({});
  });

  it('reads arrays with their element type and headers with their names', () => {
    const view: ContractPartView = {
      declared: false,
      schema: { type: 'object', properties: { tags: { type: 'array', items: { type: 'string' } }, items: { type: 'array', items: { type: 'object', properties: { sku: {} } } } } },
      paths: [
        { path: 'input.body.tags[]', type: 'array', origin: 'discovered' },
        { path: 'input.body.items[]', type: 'array', origin: 'discovered' },
        { path: 'input.body.items[].sku', origin: 'discovered', usedBy: ['prepare'] },
      ],
    };
    const rows = rowsFromContractPart(view, undefined, 'body', 'input.body', nextKey);
    expect(rows.map(r => [r.path, r.type, r.itemType])).toEqual([
      ['tags[]', 'array', 'string'], ['items[]', 'array', ''], ['items[].sku', '', ''],
    ]);
    const headers: ContractPartView = {
      declared: true,
      schema: { type: 'object', properties: { 'X-Api-Key': { type: 'string' } }, required: ['X-Api-Key'] },
      paths: [{ path: 'input.header.X-Api-Key', type: 'string', origin: 'both', required: true, usedBy: ['compute'] }],
    };
    const headerRows = rowsFromContractPart(headers, headers.schema, 'header', 'input.header', nextKey);
    expect(headerRows.map(r => [r.path, r.type, r.required])).toEqual([['X-Api-Key', 'string', true]]);
  });

  it('parses the contract view and refuses another shape', () => {
    const view = parseContractView({
      graph: 'g', purpose: 'p',
      input: { body: DECLARED_BODY, header: { declared: false, paths: [] } },
      output: { body: { declared: false, paths: [] }, header: { declared: false, paths: [] }, status: [404] },
      issues: ['input.body.c is declared but never referenced by the model'],
    });
    expect(view.input.body.paths).toHaveLength(4);
    expect(view.output.status).toEqual([404]);
    expect(view.issues).toHaveLength(1);
    expect(() => parseContractView({ nodes: [] })).toThrow(/not a graph contract/);
    expect(() => parseContractView({ graph: 'g', input: {}, output: {} })).toThrow(/'input.body'/);
  });
});

describe('rowsFromValue and mergeRunRows', () => {
  it('infers types, arrays and examples from a run value', () => {
    const rows = rowsFromValue({
      amount: 12.5, count: 3, ok: true, name: 'Ann', tags: ['a', 'b'],
      items: [{ sku: 'x', qty: 1 }, { sku: 'y', note: 'n' }], profile: { city: 'Rome' }, nothing: null,
      'bad name': 1,
    }, 'body', nextKey);
    expect(rows.map(r => [r.path, r.type, r.itemType, r.example])).toEqual([
      ['amount', 'number', '', '12.5'],
      ['count', 'integer', '', '3'],
      ['ok', 'boolean', '', 'true'],
      ['name', 'string', '', 'Ann'],
      ['tags[]', 'array', 'string', ''],
      ['items[]', 'array', '', ''],
      ['items[].sku', 'string', '', 'x'],
      ['items[].qty', 'integer', '', '1'],
      ['items[].note', 'string', '', 'n'],
      ['profile', 'object', '', ''],
      ['profile.city', 'string', '', 'Rome'],
      ['nothing', '', '', ''],
    ]);
    expect(rows.every(r => r.source === 'run')).toBe(true);
    const headers = rowsFromValue({ 'X-Tenant': 'acme', 'X-Count': '3' }, 'header', nextKey);
    expect(headers.map(r => [r.path, r.type, r.example])).toEqual([['X-Tenant', 'string', 'acme'], ['X-Count', 'string', '3']]);
  });

  it('fills the gaps of the contract rows and appends what the contract never saw', () => {
    const rows = [
      row({ path: 'a', type: 'number', source: 'both' }),
      row({ path: 'd', type: '', source: 'discovered' }),
      row({ path: 'tags[]', type: 'array', source: 'discovered' }),
    ];
    const run = rowsFromValue({ a: 1, d: 'text', tags: ['x'], extra: false }, 'body', nextKey);
    const { rows: merged, changed } = mergeRunRows(rows, run);
    expect(changed).toBe(4);
    expect(merged.map(r => [r.path, r.type, r.itemType, r.example, r.source])).toEqual([
      ['a', 'number', '', '1', 'both'],          // a declared row keeps its chip
      ['d', 'string', '', 'text', 'run'],        // typed by the run
      ['tags[]', 'array', 'string', '', 'run'],
      ['extra', 'boolean', '', 'false', 'run'],
    ]);
    // a second fill changes nothing
    expect(mergeRunRows(merged, run).changed).toBe(0);
  });
});

describe('schemaRowIssues', () => {
  it('flags what the gate or the grammar would refuse', () => {
    const rows = [
      row({ path: '' }),
      row({ path: 'a b' }),
      row({ path: 'dup', type: 'string' }),
      row({ path: 'dup', type: 'number' }),
      row({ path: 'list[]', type: 'string' }),
      row({ path: 'other', type: 'array' }),
      row({ path: 'profile', type: 'string' }),
      row({ path: 'profile.name', type: 'string' }),
      row({ path: 'items[]', type: 'array', itemType: 'string' }),
      row({ path: 'items[].sku' }),
      row({ path: 'fine', type: 'integer' }),
    ];
    const issues = schemaRowIssues(rows, 'body');
    expect(issues.get(rows[0].key)).toBe('A path is required.');
    expect(issues.get(rows[1].key)).toMatch(/dot-separated names/);
    expect(issues.get(rows[2].key)).toBe('Duplicate path "dup".');
    expect(issues.get(rows[3].key)).toBe('Duplicate path "dup".');
    expect(issues.get(rows[4].key)).toBe('A path ending in [] is an array.');
    expect(issues.get(rows[5].key)).toMatch(/ends in \[\]/);
    expect(issues.get(rows[6].key)).toMatch(/nested paths, so its type is object/);
    expect(issues.has(rows[7].key)).toBe(false);
    expect(issues.get(rows[8].key)).toMatch(/clear the item type/);
    expect(issues.has(rows[9].key)).toBe(false);
    expect(issues.has(rows[10].key)).toBe(false);
  });

  it('holds a header to one name and a text type', () => {
    const rows = [
      row({ path: 'X-Api-Key', type: 'string' }),
      row({ path: 'X-Api-Key', type: 'string' }),
      row({ path: 'Content.Type', type: 'string' }),
      row({ path: 'X-Count', type: 'object' }),
      row({ path: '' }),
    ];
    const issues = schemaRowIssues(rows, 'header');
    expect(issues.get(rows[0].key)).toBe('Duplicate header "X-Api-Key".');
    expect(issues.get(rows[2].key)).toMatch(/letters, digits, hyphen or underscore/);
    expect(issues.get(rows[3].key)).toMatch(/A header is text/);
    expect(issues.get(rows[4].key)).toBe('A header name is required.');
  });
});

describe('buildPartSchema', () => {
  it('rebuilds the fixture declaration from its rows, constraints carried unchanged', () => {
    const rows = rowsFromContractPart(DECLARED_BODY, DECLARED_NODE_BODY, 'body', 'input.body', nextKey);
    // the discovered 'd' row stays untyped: it is declared as an empty schema, as the view shows it
    expect(buildPartSchema(rows, 'body')).toEqual({
      type: 'object',
      properties: {
        a: { type: 'number', description: 'first operand' },
        b: { type: 'integer', minimum: 0 },
        c: { type: 'string', enum: ['x', 'y'] },
        d: {},
      },
      required: ['a', 'b'],
    });
  });

  it('nests objects and arrays, puts required at each level, and keeps an example', () => {
    const rows = [
      row({ path: 'profile', type: 'object', required: true }),
      row({ path: 'profile.name', type: 'string', required: true, example: 'Ann' }),
      row({ path: 'items[]', type: 'array' }),
      row({ path: 'items[].sku', type: 'string', required: true }),
      row({ path: 'items[].qty', type: 'integer' }),
      row({ path: 'tags[]', type: 'array', itemType: 'string', description: 'labels' }),
      row({ path: 'implicit.child', type: 'boolean' }),         // no row for 'implicit' itself
      row({ path: 'untyped' }),
    ];
    expect(buildPartSchema(rows, 'body')).toEqual({
      type: 'object',
      properties: {
        profile: { type: 'object', properties: { name: { type: 'string', example: 'Ann' } }, required: ['name'] },
        items: { type: 'array', items: { type: 'object', properties: { sku: { type: 'string' }, qty: { type: 'integer' } }, required: ['sku'] } },
        tags: { type: 'array', items: { type: 'string' }, description: 'labels' },
        implicit: { type: 'object', properties: { child: { type: 'boolean' } } },
        untyped: {},
      },
      required: ['profile'],
    });
  });

  it('builds the header part, and no rows mean no part', () => {
    const rows = [
      row({ path: 'X-Api-Key', type: 'string', required: true, description: 'the key' }),
      row({ path: 'X-Count', type: 'integer' }),
      row({ path: '   ' }),
    ];
    expect(buildPartSchema(rows, 'header')).toEqual({
      type: 'object',
      properties: { 'X-Api-Key': { type: 'string', description: 'the key' }, 'X-Count': { type: 'integer' } },
      required: ['X-Api-Key'],
    });
    expect(buildPartSchema([], 'header')).toBeNull();
    expect(buildSchemaProperty(null, null)).toBeNull();
    expect(buildSchemaProperty({ type: 'object' }, null)).toEqual({ body: { type: 'object' } });
  });

  it('keeps a declared items fragment of an array the rows do not type', () => {
    const rows = [row({ path: 'codes[]', type: 'array', constraints: { items: { type: 'string', pattern: '^[A-Z]+$' }, minItems: 1 } })];
    expect(buildPartSchema(rows, 'body')).toEqual({
      type: 'object',
      properties: { codes: { type: 'array', items: { type: 'string', pattern: '^[A-Z]+$' }, minItems: 1 } },
    });
  });
});

describe('schemaToPropertyRows', () => {
  it('writes the composite keys of the Playground grammar, lists with [] in order, scalars as text', () => {
    const rows = schemaToPropertyRows({
      body: {
        type: 'object',
        properties: { a: { type: 'number', minimum: 0, nullable: true }, c: { type: 'string', enum: ['x', 'y'] }, d: {} },
        required: ['a', 'c'],
        additionalProperties: false,
      },
      header: { type: 'object', properties: { 'X-Api-Key': { type: 'string' } }, required: ['X-Api-Key'] },
    });
    expect(rows.map(r => `${r.key}=${r.value}`)).toEqual([
      'schema.body.type=object',
      'schema.body.properties.a.type=number',
      'schema.body.properties.a.minimum=0',
      'schema.body.properties.a.nullable=true',
      'schema.body.properties.c.type=string',
      'schema.body.properties.c.enum[]=x',
      'schema.body.properties.c.enum[]=y',
      'schema.body.required[]=a',
      'schema.body.required[]=c',
      'schema.body.additionalProperties=false',
      'schema.header.type=object',
      'schema.header.properties.X-Api-Key.type=string',
      'schema.header.required[]=X-Api-Key',
    ]);
  });

  it('refuses a name or a value the grammar cannot carry', () => {
    expect(() => schemaToPropertyRows({ body: { properties: { 'first name': {} } } })).toThrow(/property name uses/);
    expect(() => schemaToPropertyRows({ body: { description: "it's '''quoted" } })).toThrow(/'''/);
    expect(() => schemaToPropertyRows({ body: { enum: [{ a: 1 }] } })).toThrow(/list of objects/);
  });

  it('summarizes constraints for the chip', () => {
    expect(summarizeConstraints({ enum: ['x', 'y'], minimum: 0, pattern: '^[A-Z]{2,40}[0-9]+$', items: { type: 'string' } }))
      .toBe('enum (2) · minimum 0 · pattern · items');
    expect(summarizeConstraints({})).toBe('');
  });
});
