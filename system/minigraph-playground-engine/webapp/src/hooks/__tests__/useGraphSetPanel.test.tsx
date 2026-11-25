// @vitest-environment happy-dom

import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useGraphSetPanel } from '../useGraphSetPanel';
import type { MinigraphGraphData } from '../../utils/graphTypes';

const MODEL: MinigraphGraphData = {
  nodes: [
    { alias: 'root', types: ['Root'], properties: { name: 'demo' } },
    { alias: 'end', types: ['End'], properties: {} },
  ],
  connections: [{ source: 'root', target: 'end', relations: [{ type: 'finish', properties: {} }] }],
};

function modelNamed(name: string) {
  return { ...MODEL, nodes: [{ ...MODEL.nodes[0], properties: { name } }, MODEL.nodes[1]] };
}

function jsonFile(name: string, content: unknown): File {
  const text = typeof content === 'string' ? content : JSON.stringify(content);
  return new File([text], name, { type: 'application/json' });
}

type PickerWindow = Window & { showSaveFilePicker?: unknown };

function setup(options: { graphData?: MinigraphGraphData | null; graphName?: string | null } = {}) {
  const addToast = vi.fn();
  const importFiles = vi.fn().mockResolvedValue(undefined);
  const hook = renderHook(() => useGraphSetPanel({
    addToast,
    importFiles,
    graphData: options.graphData ?? null,
    graphName: options.graphName ?? null,
  }));
  return { ...hook, addToast, importFiles };
}

afterEach(() => {
  vi.unstubAllGlobals();
  delete (window as PickerWindow).showSaveFilePicker;
});

describe('useGraphSetPanel - the entries', () => {
  it('takes several graph files at once, each named after its file, and keeps them across a close', async () => {
    const { result } = setup();
    expect(result.current.isOpen).toBe(false);
    act(() => result.current.open());

    await act(async () => {
      await result.current.addFiles([jsonFile('tutorial-1.json', modelNamed('tutorial-1')), jsonFile('tutorial-2.json', modelNamed('tutorial-2'))]);
    });

    expect(result.current.entries.map(entry => [entry.id, entry.nodeCount, entry.connectionCount, entry.source]))
      .toEqual([['tutorial-1', 2, 1, 'file'], ['tutorial-2', 2, 1, 'file']]);
    expect(result.current.entryIssues.size).toBe(0);
    expect(result.current.fileErrors).toEqual([]);

    act(() => result.current.close());
    expect(result.current.isOpen).toBe(false);
    expect(result.current.entries).toHaveLength(2);
  });

  it('reports each file it did not take and flags a duplicate and a root-name mismatch in place', async () => {
    const { result } = setup();
    await act(async () => {
      await result.current.addFiles([
        jsonFile('a.json', modelNamed('a')),
        jsonFile('a.json', modelNamed('a')),
        jsonFile('b.json', modelNamed('other')),
        jsonFile('broken.json', '{not json'),
        jsonFile('plain.json', { nodes: [], extra: true }),
        new File(['x'], 'notes.md', { type: 'text/markdown' }),
      ]);
    });

    expect(result.current.entries.map(entry => entry.id)).toEqual(['a', 'a', 'b']);
    const [first, second, third] = result.current.entries;
    expect(result.current.entryIssues.get(first.key)).toContain('Duplicate graph id "a"');
    expect(result.current.entryIssues.get(second.key)).toContain('Duplicate graph id "a"');
    expect(result.current.entryIssues.get(third.key)).toContain('"other" differs from the graph id');
    expect(result.current.fileErrors).toHaveLength(3);
    expect(result.current.fileErrors[0]).toContain('"broken.json" is not valid JSON');
    expect(result.current.fileErrors[1]).toContain('"plain.json" is not a graph model');
    expect(result.current.fileErrors[2]).toContain('"notes.md" does not appear to be a JSON file');

    act(() => result.current.removeEntry(second.key));
    expect(result.current.entries.map(entry => entry.id)).toEqual(['a', 'b']);
    expect(result.current.entryIssues.has(first.key)).toBe(false);
  });

  it('adds the current graph named after its display name, with the root renamed, and a re-add replaces it', () => {
    const { result } = setup({ graphData: MODEL, graphName: 'My Graph' });
    expect(result.current.canAddCurrentGraph).toBe(true);

    act(() => result.current.addCurrentGraph());
    expect(result.current.entries).toHaveLength(1);
    const entry = result.current.entries[0];
    expect(entry).toMatchObject({ id: 'My-Graph', name: 'My-Graph', source: 'draft', nodeCount: 2, connectionCount: 1 });
    expect((entry.model.nodes as { properties: { name: string } }[])[0].properties.name).toBe('My-Graph');
    // the live graph is untouched
    expect(MODEL.nodes[0].properties.name).toBe('demo');

    act(() => result.current.addCurrentGraph());
    expect(result.current.entries).toHaveLength(1);
    expect(result.current.entries[0].key).not.toBe(entry.key);
  });

  it('cannot add a graph while none is loaded', () => {
    const { result } = setup();
    expect(result.current.canAddCurrentGraph).toBe(false);
    act(() => result.current.addCurrentGraph());
    expect(result.current.entries).toHaveLength(0);
  });
});

describe('useGraphSetPanel - the manifest and the pack', () => {
  it('starts with the suggested fields, validates the set name and the rows, and packs only when all is well', async () => {
    const { result } = setup();
    expect(result.current.rows.map(row => [row.name, row.value])).toEqual([['version', ''], ['description', '']]);
    expect(result.current.setNameError).toContain('required');
    expect(result.current.canPack).toBe(false);

    await act(async () => { await result.current.addFiles([jsonFile('a.json', modelNamed('a'))]); });
    expect(result.current.canPack).toBe(false);

    act(() => result.current.setSetName('demo'));
    expect(result.current.setNameError).toBeNull();
    expect(result.current.canPack).toBe(true);

    act(() => result.current.addRow());
    const added = result.current.rows[2];
    act(() => result.current.updateRow(added.key, { name: 'graph_id', value: 'missing' }));
    expect(result.current.rowIssues.get(added.key)).toBe('"missing" is not a graph of the set.');
    expect(result.current.canPack).toBe(false);

    act(() => result.current.updateRow(added.key, { value: 'a' }));
    expect(result.current.rowIssues.size).toBe(0);
    expect(result.current.canPack).toBe(true);

    act(() => result.current.removeRow(added.key));
    expect(result.current.rows).toHaveLength(2);
  });

  it('posts the request to the pack endpoint and saves the answered bytes as <set>.pack', async () => {
    const bytes = new Uint8Array([0x82, 0xa8, 0x6d, 0x61, 0x6e, 0x69]);
    const fetchMock = vi.fn().mockResolvedValue(new Response(bytes, {
      status: 200,
      headers: { 'Content-Type': 'application/octet-stream', 'Content-Disposition': 'attachment; filename="demo.pack"' },
    }));
    vi.stubGlobal('fetch', fetchMock);
    const write = vi.fn().mockResolvedValue(undefined);
    (window as PickerWindow).showSaveFilePicker = vi.fn().mockResolvedValue({
      name: 'demo.pack',
      createWritable: async () => ({ write, close: vi.fn().mockResolvedValue(undefined) }),
    });
    const { result, addToast } = setup();
    await act(async () => { await result.current.addFiles([jsonFile('a.json', modelNamed('a'))]); });
    act(() => result.current.setSetName(' demo '));
    act(() => result.current.updateRow(result.current.rows[0].key, { value: '1.0.0' }));

    await act(async () => { await result.current.pack(); });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/graph-set/pack');
    expect(init.method).toBe('POST');
    expect((init.headers as Record<string, string>)['Content-Type']).toBe('application/json');
    expect(JSON.parse(init.body as string)).toEqual({
      manifest: { set: 'demo', version: '1.0.0' },
      graphs: { a: modelNamed('a') },
    });
    expect(write).toHaveBeenCalledTimes(1);
    expect(Array.from(write.mock.calls[0][0] as Uint8Array)).toEqual(Array.from(bytes));
    expect(addToast).toHaveBeenCalledWith('Graph set saved as demo.pack', 'success');
    expect(result.current.packError).toBeNull();
    expect(result.current.isPacking).toBe(false);
  });

  it("shows the engine's refusal in place and saves nothing", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ status: 400, message: "Set not packed - a: graph must have an 'end' node", type: 'error' }),
      { status: 400, headers: { 'Content-Type': 'application/json' } },
    ));
    vi.stubGlobal('fetch', fetchMock);
    const picker = vi.fn();
    (window as PickerWindow).showSaveFilePicker = picker;
    const { result, addToast } = setup();
    await act(async () => { await result.current.addFiles([jsonFile('a.json', modelNamed('a'))]); });
    act(() => result.current.setSetName('demo'));

    await act(async () => { await result.current.pack(); });

    expect(result.current.packError).toBe("Set not packed - a: graph must have an 'end' node");
    expect(picker).not.toHaveBeenCalled();
    expect(addToast).not.toHaveBeenCalled();

    // the next edit clears the stale message
    act(() => result.current.setSetName('demo2'));
    act(() => result.current.removeEntry(result.current.entries[0].key));
    expect(result.current.packError).toBeNull();
  });
});

describe('useGraphSetPanel - inspect mode', () => {
  const ANSWER = { manifest: { set: 'demo', format: 'canonical', format_version: '1', version: '2' }, graphs: { b: modelNamed('b'), a: modelNamed('a') } };

  it('reads a dropped .pack through the unpack endpoint and shows its manifest and graphs', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(ANSWER), {
      status: 200, headers: { 'Content-Type': 'application/json' },
    }));
    vi.stubGlobal('fetch', fetchMock);
    const { result, importFiles } = setup();
    const pack = new File([new Uint8Array([1, 2, 3])], 'demo.pack', { type: 'application/octet-stream' });

    await act(async () => { await result.current.addFiles([pack]); });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/graph-set/unpack');
    expect((init.headers as Record<string, string>)['Content-Type']).toBe('application/octet-stream');
    expect(init.body).toBe(pack);
    expect(result.current.mode).toBe('inspect');
    expect(result.current.inspected?.fileName).toBe('demo.pack');
    expect(result.current.inspected?.set.manifest).toEqual(ANSWER.manifest);
    expect(result.current.inspected?.set.graphs.map(graph => graph.id)).toEqual(['b', 'a']);

    // one graph as the draft, through the file-import path (which asks before replacing a loaded graph)
    await act(async () => { await result.current.importInspectedGraph('a'); });
    expect(importFiles).toHaveBeenCalledTimes(1);
    const [files] = importFiles.mock.calls[0] as [File[]];
    expect(files[0].name).toBe('a.json');
    expect(JSON.parse(await files[0].text())).toEqual(modelNamed('a'));

    act(() => result.current.leaveInspect());
    expect(result.current.mode).toBe('assemble');
    expect(result.current.inspected).toBeNull();
  });

  it('loads an inspected set into the editor to pack it again, without the fields the engine writes', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(ANSWER), {
      status: 200, headers: { 'Content-Type': 'application/json' },
    })));
    const { result, addToast } = setup();
    await act(async () => { await result.current.addFiles([jsonFile('old.json', modelNamed('old'))]); });
    await act(async () => { await result.current.addFiles([new File([new Uint8Array([1])], 'demo.pack')]); });
    expect(result.current.mode).toBe('inspect');

    act(() => result.current.editInspectedSet());

    expect(result.current.mode).toBe('assemble');
    expect(result.current.setName).toBe('demo');
    expect(result.current.entries.map(entry => [entry.id, entry.source])).toEqual([['b', 'set'], ['a', 'set']]);
    expect(result.current.rows.map(row => [row.name, row.value])).toEqual([['version', '2']]);
    expect(result.current.canPack).toBe(true);
    expect(addToast).toHaveBeenCalledWith('Editing demo.pack: 2 graphs and 1 manifest field loaded', 'info');
  });

  it('refuses a .pack dropped together with other files, and reports a file the engine refuses', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ status: 400, message: 'Not a graph set - the package holds no graph', type: 'error' }),
      { status: 400, headers: { 'Content-Type': 'application/json' } },
    ));
    vi.stubGlobal('fetch', fetchMock);
    const { result } = setup();

    await act(async () => { await result.current.addFiles([new File([''], 'demo.pack'), jsonFile('a.json', modelNamed('a'))]); });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fileErrors).toEqual(['Drop one .pack file on its own to inspect it, or .json graph files to pack.']);

    await act(async () => { await result.current.addFiles([new File([''], 'demo.pack')]); });
    await waitFor(() => expect(result.current.isReading).toBe(false));
    expect(result.current.mode).toBe('assemble');
    expect(result.current.fileErrors).toEqual(['"demo.pack" was not read - Not a graph set - the package holds no graph']);
  });
});

describe('useGraphSetPanel - deploy mode (RFC-0017)', () => {
  const LIST = {
    location: 'file:/tmp/deploy',
    sets: [{ set: 'settlement', version: '1.0', sha256: 'ab'.repeat(32), deployed_at: '2026-11-24T18:00:00.000Z',
             graphs: ['settlement', 'settlement-review'], status: 'deployed' }],
  };
  const DEPLOYED = {
    set: 'pricing', version: '2', sha256: 'cd'.repeat(32), graphs: ['pricing', 'pricing-quote'],
    deployed_at: '2026-11-24T18:05:00.000Z', changed: true,
    replaced: { version: '1', sha256: 'ef'.repeat(32) },
    advisory: { routes: [{ graph: 'pricing', route: 'v1.rate', status: 'remote', server: 'http://util:8086/api/event' }],
                extensions: [{ graph: 'pricing', target: 'settlement-review', status: 'private' }] },
  };
  const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
    status, headers: { 'Content-Type': 'application/json' },
  });
  function packFile(name = 'pricing.pack'): File {
    return new File([new Uint8Array([1, 2, 3, 4])], name, { type: 'application/octet-stream' });
  }

  it('opens the deploy view, lists the deployed sets, and deploys a dropped .pack with the expected digest as the header', async () => {
    const calls: { url: string; method: string; headers: Record<string, string> }[] = [];
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      calls.push({ url, method: init?.method ?? 'GET', headers: (init?.headers as Record<string, string>) ?? {} });
      if (url === '/api/graph-set/deployed') return json(LIST);
      if (url === '/api/graph-set/deploy') return json(DEPLOYED);
      throw new Error(`unexpected ${url}`);
    });
    vi.stubGlobal('fetch', fetchMock);
    const { result, addToast } = setup();
    act(() => { result.current.open(); result.current.openDeploy(); });
    expect(result.current.mode).toBe('deploy');
    await waitFor(() => expect(result.current.deployed?.sets).toHaveLength(1));
    expect(result.current.hotDeployOff).toBe(false);
    expect(result.current.deployed?.location).toBe('file:/tmp/deploy');

    act(() => result.current.setExpectedDigest('cd'.repeat(32)));
    await act(async () => { await result.current.addFiles([packFile()]); });

    const deploy = calls.find(call => call.url === '/api/graph-set/deploy');
    expect(deploy?.method).toBe('POST');
    expect(deploy?.headers['Content-Type']).toBe('application/octet-stream');
    expect(deploy?.headers['X-Graph-Set-SHA256']).toBe('cd'.repeat(32));
    expect(result.current.lastDigest).toMatch(/^[0-9a-f]{64}$/);
    expect(result.current.lastDeployment).toMatchObject({ set: 'pricing', version: '2', changed: true });
    expect(result.current.lastDeployment?.replaced).toEqual({ version: '1', sha256: 'ef'.repeat(32) });
    expect(result.current.lastDeployment?.routes).toEqual([{ graph: 'pricing', route: 'v1.rate', status: 'remote', server: 'http://util:8086/api/event' }]);
    expect(result.current.lastDeployment?.extensions).toEqual([{ graph: 'pricing', target: 'settlement-review', status: 'private' }]);
    expect(result.current.deployError).toBeNull();
    expect(addToast).toHaveBeenCalledWith('Deployed pricing 2', 'success');
    // the list is read again after a deploy
    expect(calls.filter(call => call.url === '/api/graph-set/deployed')).toHaveLength(2);
  });

  it('opens the panel straight in the deploy view from the Tools menu entry and reads the list', async () => {
    const fetchMock = vi.fn(async (url: string) => {
      if (url === '/api/graph-set/deployed') return json(LIST);
      throw new Error(`unexpected ${url}`);
    });
    vi.stubGlobal('fetch', fetchMock);
    const { result } = setup();
    expect(result.current.isOpen).toBe(false);
    act(() => result.current.openInDeploy());
    expect(result.current.isOpen).toBe(true);
    expect(result.current.mode).toBe('deploy');
    await waitFor(() => expect(result.current.deployed?.sets).toHaveLength(1));
    expect(fetchMock).toHaveBeenCalledTimes(1);
    // closing returns the panel to assemble mode, as from any view
    act(() => result.current.close());
    expect(result.current.isOpen).toBe(false);
    expect(result.current.mode).toBe('assemble');
  });

  it('says that hot deployment is off when the engine answers 503, and reports a refusal in place', async () => {
    const fetchMock = vi.fn(async (url: string) => {
      if (url === '/api/graph-set/deployed') return json({ status: 503, message: 'Service list.deployed.sets not reachable' }, 503);
      return json({ status: 409, message: 'graph id tutorial-1 is owned by classpath:/graph' }, 409);
    });
    vi.stubGlobal('fetch', fetchMock);
    const { result } = setup();
    act(() => { result.current.open(); result.current.openDeploy(); });
    await waitFor(() => expect(result.current.hotDeployOff).toBe(true));
    expect(result.current.deployed).toBeNull();
    expect(result.current.listError).toBeNull();

    // another engine answers 409 to the upload: the reason stays in place, nothing else changes
    vi.stubGlobal('fetch', vi.fn(async (url: string) => {
      if (url === '/api/graph-set/deployed') return json({ location: 'file:/tmp/deploy', sets: [] });
      return json({ status: 409, message: 'graph id tutorial-1 is owned by classpath:/graph' }, 409);
    }));
    await act(async () => { await result.current.refreshDeployed(); });
    expect(result.current.hotDeployOff).toBe(false);
    await act(async () => { await result.current.deployFile(packFile('shadow.pack')); });
    expect(result.current.deployError).toContain('"shadow.pack" was not deployed');
    expect(result.current.deployError).toContain('tutorial-1 is owned by classpath:/graph');
    expect(result.current.lastDeployment).toBeNull();
    expect(result.current.lastDigest).toMatch(/^[0-9a-f]{64}$/);
  });

  it('asks before an undeploy, posts it to the set\'s path, and refreshes the list', async () => {
    const calls: string[] = [];
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      calls.push(`${init?.method ?? 'GET'} ${url}`);
      if (url === '/api/graph-set/deployed') {
        return json(calls.filter(call => call.startsWith('POST')).length > 0 ? { location: 'file:/tmp/deploy', sets: [] } : LIST);
      }
      return json({ set: 'settlement', graphs: ['settlement', 'settlement-review'], undeployed: true });
    });
    vi.stubGlobal('fetch', fetchMock);
    const { result, addToast } = setup();
    act(() => { result.current.open(); result.current.openDeploy(); });
    await waitFor(() => expect(result.current.deployed?.sets).toHaveLength(1));

    act(() => result.current.requestUndeploy('settlement'));
    expect(result.current.pendingUndeploy).toBe('settlement');
    act(() => result.current.cancelUndeploy());
    expect(result.current.pendingUndeploy).toBeNull();
    expect(calls.filter(call => call.startsWith('POST'))).toHaveLength(0);

    act(() => result.current.requestUndeploy('settlement'));
    await act(async () => { await result.current.confirmUndeploy(); });
    expect(calls).toContain('POST /api/graph-set/undeploy/settlement');
    expect(result.current.pendingUndeploy).toBeNull();
    expect(addToast).toHaveBeenCalledWith('Undeployed settlement', 'success');
    await waitFor(() => expect(result.current.deployed?.sets).toHaveLength(0));
  });

  it('packs the assembled set on the engine and deploys the bytes in one step', async () => {
    const packed = new Uint8Array([9, 9, 9]);
    const calls: string[] = [];
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      calls.push(`${init?.method ?? 'GET'} ${url}`);
      if (url === '/api/graph-set/pack') return new Response(packed, { status: 200, headers: { 'Content-Type': 'application/octet-stream' } });
      if (url === '/api/graph-set/deploy') return json({ ...DEPLOYED, set: 'demo', graphs: ['demo'], replaced: null, advisory: {} });
      return json({ location: 'file:/tmp/deploy', sets: [] });
    });
    vi.stubGlobal('fetch', fetchMock);
    const { result } = setup();
    await act(async () => { await result.current.addFiles([jsonFile('demo.json', modelNamed('demo'))]); });
    act(() => result.current.setSetName('demo'));
    act(() => { result.current.open(); result.current.openDeploy(); });
    await waitFor(() => expect(result.current.deployed).not.toBeNull());
    expect(result.current.canPackAndDeploy).toBe(true);

    await act(async () => { await result.current.packAndDeploy(); });
    expect(calls).toEqual(['GET /api/graph-set/deployed', 'POST /api/graph-set/pack', 'POST /api/graph-set/deploy', 'GET /api/graph-set/deployed']);
    expect(result.current.lastDeployment?.set).toBe('demo');
    expect(result.current.lastDeployment?.routes).toEqual([]);
    expect(result.current.deployError).toBeNull();
    // the digest is of the bytes the engine packed
    const expected = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', packed)), b => b.toString(16).padStart(2, '0')).join('');
    expect(result.current.lastDigest).toBe(expected);
  });
});
