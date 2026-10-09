// @vitest-environment happy-dom

import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useSchemaPanel } from '../useSchemaPanel';
import { ProtocolBus } from '../../protocol/bus';
import type { MinigraphGraphData } from '../../utils/graphTypes';

const SESSION = 'ws-100-1';

const GRAPH: MinigraphGraphData = {
  nodes: [
    {
      alias: 'root', types: ['Root'],
      properties: {
        name: 'demo',
        mapping: ['input.body.a -> model.a'],
        schema: { body: { type: 'object', properties: { a: { type: 'number', description: 'first', minimum: 0 } }, required: ['a'] } },
      },
    },
    { alias: 'compute', types: ['Math'], properties: { skill: 'graph.math' } },
    { alias: 'end', types: ['End'], properties: {} },
  ],
  connections: [],
};

/** The engine's contract view of GRAPH: the declaration merged with discovery. */
const CONTRACT = {
  graph: 'demo',
  input: {
    body: {
      declared: true,
      schema: { type: 'object', properties: { a: { type: 'number', description: 'first', minimum: 0 }, d: { description: 'Referenced by compute' } }, required: ['a'] },
      paths: [
        { path: 'input.body.a', type: 'number', origin: 'both', required: true, usedBy: ['compute'] },
        { path: 'input.body.d', origin: 'discovered', usedBy: ['compute'] },
      ],
    },
    header: { declared: false, paths: [{ path: 'input.header.X-Tenant', origin: 'discovered', usedBy: ['compute'] }] },
  },
  output: {
    body: { declared: false, schema: { type: 'object', properties: { sum: { type: 'number' } } }, paths: [{ path: 'output.body.sum', type: 'number', origin: 'discovered', usedBy: ['end'] }] },
    header: { declared: false, paths: [] },
    status: [],
  },
  issues: ['input.body.d is referenced by compute but not declared'],
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

function stubFetch(routes: Record<string, () => Response>) {
  const calls: string[] = [];
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    calls.push(url);
    const handler = routes[url];
    return Promise.resolve(handler ? handler() : json({ message: 'Not found' }, 404));
  });
  vi.stubGlobal('fetch', fetchMock);
  return { fetchMock, calls };
}

function setup(options: { graphData?: MinigraphGraphData | null; sessionId?: string | null; connected?: boolean } = {}) {
  const bus = new ProtocolBus();
  const addToast = vi.fn();
  const sendRawText = vi.fn().mockReturnValue(true);
  const hook = renderHook(() => useSchemaPanel({
    addToast,
    sessionId: options.sessionId === undefined ? SESSION : options.sessionId,
    connected: options.connected ?? true,
    graphData: options.graphData === undefined ? GRAPH : options.graphData,
    graphName: 'demo',
    bus,
    sendRawText,
  }));
  return { ...hook, bus, addToast, sendRawText };
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.useRealTimers();
});

describe('useSchemaPanel - loading', () => {
  it('opens on a side and reads the contract view into rows for the four parts', async () => {
    const { calls } = stubFetch({ [`/api/openapi/session/${SESSION}?view=contract`]: () => json(CONTRACT) });
    const { result } = setup();
    expect(result.current.isOpen).toBe(false);

    act(() => result.current.open('output'));
    expect(result.current.isOpen).toBe(true);
    expect(result.current.side).toBe('output');
    await waitFor(() => expect(result.current.contract).not.toBeNull());
    expect(calls).toEqual([`/api/openapi/session/${SESSION}?view=contract`]);

    expect(result.current.rows['input.body'].map(r => [r.path, r.type, r.required, r.source, r.description]))
      .toEqual([['a', 'number', true, 'both', 'first'], ['d', '', false, 'discovered', '']]);
    expect(result.current.rows['input.body'][0].constraints).toEqual({ minimum: 0 });
    expect(result.current.rows['input.header'].map(r => [r.path, r.type, r.source])).toEqual([['X-Tenant', '', 'discovered']]);
    expect(result.current.rows['output.body'].map(r => [r.path, r.type])).toEqual([['sum', 'number']]);
    expect(result.current.rows['output.header']).toEqual([]);
    expect(result.current.issuesOf('input')).toEqual(['input.body.d is referenced by compute but not declared']);
    expect(result.current.issuesOf('output')).toEqual([]);
    expect(result.current.dirty).toEqual({ input: false, output: false });
    expect(result.current.canSave).toBe(false);
  });

  it('reports a refused read and a session not yet known', async () => {
    stubFetch({ [`/api/openapi/session/${SESSION}?view=contract`]: () => json({ message: "Session 'ws-100-1' does not exist" }, 404) });
    const { result } = setup();
    act(() => result.current.open());
    await waitFor(() => expect(result.current.loadError).toBe("Session 'ws-100-1' does not exist"));

    const noSession = setup({ sessionId: null });
    act(() => noSession.result.current.open());
    expect(noSession.result.current.loadError).toBe('The session id is not known yet.');
  });

  it('reloads on a graph mutation unless rows are being edited, and keeps unsaved rows across a close', async () => {
    const { calls } = stubFetch({ [`/api/openapi/session/${SESSION}?view=contract`]: () => json(CONTRACT) });
    const { result, bus } = setup();
    act(() => result.current.open());
    await waitFor(() => expect(result.current.contract).not.toBeNull());
    expect(calls).toHaveLength(1);

    act(() => bus.emit({ kind: 'graph.mutation', mutationType: 'node-mutation', msgId: 1, raw: 'node x updated' }));
    await waitFor(() => expect(calls).toHaveLength(2));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    const row = result.current.rows['input.body'][1];
    act(() => result.current.updateRow('input.body', row.key, { type: 'string' }));
    expect(result.current.dirty.input).toBe(true);
    act(() => bus.emit({ kind: 'graph.mutation', mutationType: 'node-mutation', msgId: 2, raw: 'node x updated' }));
    await new Promise(resolve => setTimeout(resolve, 10));
    expect(calls).toHaveLength(2);

    act(() => result.current.close());
    expect(result.current.isOpen).toBe(false);
    act(() => result.current.open());
    expect(calls).toHaveLength(2);     // dirty rows are not thrown away by a re-open
    expect(result.current.rows['input.body'][1].type).toBe('string');

    act(() => result.current.reload());
    await waitFor(() => expect(calls).toHaveLength(3));
    await waitFor(() => expect(result.current.dirty.input).toBe(false));
  });
});

describe('useSchemaPanel - editing and saving', () => {
  it('sends one update node for the root with the other properties kept, and the engine reply closes the save', async () => {
    stubFetch({ [`/api/openapi/session/${SESSION}?view=contract`]: () => json(CONTRACT) });
    const { result, bus, sendRawText, addToast } = setup();
    act(() => result.current.open());
    await waitFor(() => expect(result.current.contract).not.toBeNull());

    const d = result.current.rows['input.body'][1];
    act(() => result.current.updateRow('input.body', d.key, { type: 'string', required: true, description: 'the d' }));
    const tenant = result.current.rows['input.header'][0];
    act(() => result.current.updateRow('input.header', tenant.key, { type: 'string', required: true }));
    expect(result.current.canSave).toBe(true);

    act(() => result.current.save());
    expect(result.current.isSaving).toBe(true);
    expect(sendRawText).toHaveBeenCalledTimes(1);
    expect(sendRawText.mock.calls[0][0].split('\n')).toEqual([
      'update node root',
      'with type Root',
      'with properties',
      'mapping[]=input.body.a -> model.a',
      'name=demo',
      'schema.body.type=object',
      'schema.body.properties.a.type=number',
      'schema.body.properties.a.description=first',
      'schema.body.properties.a.minimum=0',
      'schema.body.properties.d.type=string',
      'schema.body.properties.d.description=the d',
      'schema.body.required[]=a',
      'schema.body.required[]=d',
      'schema.header.type=object',
      'schema.header.properties.X-Tenant.type=string',
      'schema.header.required[]=X-Tenant',
    ]);

    act(() => bus.emit({
      kind: 'minigraph.nodeAction.textResult', msgId: 3, raw: 'node root updated',
      status: 'accepted', action: 'edit-node', alias: 'root', targetAlias: null, message: 'node root updated',
    }));
    expect(result.current.isSaving).toBe(false);
    expect(addToast).toHaveBeenCalledWith('Schema of node root saved', 'success');
    await waitFor(() => expect(result.current.dirty.input).toBe(false));
  });

  it('reports a rejected save, a save the engine never answers, and a missing end node', async () => {
    vi.useFakeTimers();
    stubFetch({ [`/api/openapi/session/${SESSION}?view=contract`]: () => json(CONTRACT) });
    const { result, bus } = setup();
    act(() => result.current.open());
    await act(async () => { await vi.advanceTimersByTimeAsync(0); });
    expect(result.current.contract).not.toBeNull();

    act(() => result.current.addRow('input.body'));
    const added = result.current.rows['input.body'][2];
    expect(result.current.issues['input.body'].get(added.key)).toBe('A path is required.');
    expect(result.current.canSave).toBe(false);
    act(() => result.current.updateRow('input.body', added.key, { path: 'extra', type: 'boolean' }));
    expect(result.current.canSave).toBe(true);

    act(() => result.current.save());
    act(() => bus.emit({
      kind: 'minigraph.nodeAction.textResult', msgId: 4, raw: 'ERROR: boom',
      status: 'error', action: null, alias: null, targetAlias: null, message: 'ERROR: boom',
    }));
    expect(result.current.isSaving).toBe(false);
    expect(result.current.saveError).toBe('ERROR: boom');
    expect(result.current.dirty.input).toBe(true);

    act(() => result.current.save());
    expect(result.current.saveError).toBeNull();
    act(() => { vi.advanceTimersByTime(10_000); });
    expect(result.current.isSaving).toBe(false);
    expect(result.current.saveError).toMatch(/did not answer within 10 s/);

    // the output side has no end node in this graph
    const graphWithoutEnd = { ...GRAPH, nodes: GRAPH.nodes.slice(0, 2) };
    const other = setup({ graphData: graphWithoutEnd });
    act(() => other.result.current.open('output'));
    await act(async () => { await vi.advanceTimersByTimeAsync(0); });
    expect(other.result.current.nodeOf('output')).toBeNull();
    act(() => other.result.current.addRow('output.body'));
    const row = other.result.current.rows['output.body'][1];
    act(() => other.result.current.updateRow('output.body', row.key, { path: 'total', type: 'number' }));
    expect(other.result.current.canSave).toBe(false);
  });

  it('fills types and examples from the instance through the inspect endpoint', async () => {
    stubFetch({
      [`/api/openapi/session/${SESSION}?view=contract`]: () => json(CONTRACT),
      [`/api/inspect/${SESSION}/input.body`]: () => json({ inspect: 'input.body', outcome: { a: 2, d: 'text', extra: true } }),
      [`/api/inspect/${SESSION}/input.header`]: () => json({ inspect: 'input.header', outcome: { 'X-Tenant': 'acme' } }),
      [`/api/inspect/${SESSION}/output.body`]: () => json({ inspect: 'output.body', outcome: { sum: 3 } }),
    });
    const { result, addToast } = setup();
    act(() => result.current.open());
    await waitFor(() => expect(result.current.contract).not.toBeNull());

    await act(async () => { await result.current.fillFromRun(); });
    expect(result.current.rows['input.body'].map(r => [r.path, r.type, r.example, r.source])).toEqual([
      ['a', 'number', '2', 'both'],
      ['d', 'string', 'text', 'run'],
      ['extra', 'boolean', 'true', 'run'],
    ]);
    expect(result.current.rows['input.header'][0]).toMatchObject({ path: 'X-Tenant', type: 'string', example: 'acme', source: 'run' });
    expect(result.current.rows['output.body'][0]).toMatchObject({ path: 'sum', type: 'number', example: '3', source: 'discovered' });
    expect(result.current.dirty).toEqual({ input: true, output: true });
    expect(addToast).toHaveBeenCalledWith('5 rows filled from the last run - review and Save.', 'success');

    // nothing to fill the second time
    await act(async () => { await result.current.fillFromRun(); });
    expect(addToast).toHaveBeenLastCalledWith(expect.stringMatching(/adds nothing/), 'info');
  });

  it('says so when there is no instance yet', async () => {
    stubFetch({ [`/api/openapi/session/${SESSION}?view=contract`]: () => json(CONTRACT) });
    const { result, addToast } = setup();
    act(() => result.current.open());
    await waitFor(() => expect(result.current.contract).not.toBeNull());
    await act(async () => { await result.current.fillFromRun(); });
    expect(addToast).toHaveBeenCalledWith('No instance data yet - Instantiate, Upload and Run first.', 'info');
    expect(result.current.dirty).toEqual({ input: false, output: false });
  });

  it('downloads the draft document under the engine\'s file name', async () => {
    const yaml = 'openapi: 3.0.3\n';
    stubFetch({
      [`/api/openapi/session/${SESSION}?view=contract`]: () => json(CONTRACT),
      [`/api/openapi/session/${SESSION}`]: () => new Response(yaml, {
        status: 200,
        headers: { 'Content-Type': 'application/yaml; charset=utf-8', 'Content-Disposition': 'attachment; filename="demo.yaml"' },
      }),
    });
    const anchorClick = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    vi.stubGlobal('URL', { ...URL, createObjectURL: vi.fn(() => 'blob:demo'), revokeObjectURL: vi.fn() });
    const { result, addToast } = setup();
    act(() => result.current.open());
    await waitFor(() => expect(result.current.contract).not.toBeNull());

    await act(async () => { await result.current.download(); });
    expect(anchorClick).toHaveBeenCalledTimes(1);
    expect(addToast).toHaveBeenCalledWith('OpenAPI document saved as demo.yaml', 'success');
    anchorClick.mockRestore();
  });
});
