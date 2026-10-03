// @vitest-environment happy-dom

import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useGraphFileImport } from '../useGraphFileImport';

const MODEL = {
  nodes: [
    { alias: 'root', types: ['Root'], properties: { name: 'demo' } },
    { alias: 'end', types: ['End'], properties: {} },
  ],
  connections: [{ source: 'root', target: 'end', relations: [{ type: 'finish', properties: {} }] }],
};

function jsonFile(name: string, content: unknown): File {
  const text = typeof content === 'string' ? content : JSON.stringify(content);
  return new File([text], name, { type: 'application/json' });
}

function okResponse(): Response {
  return new Response('{"message":"Graph model imported as draft","type":"import"}', {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useGraphFileImport', () => {
  it('posts a valid file to the session import endpoint when no graph is loaded', async () => {
    const fetchMock = vi.fn().mockResolvedValue(okResponse());
    vi.stubGlobal('fetch', fetchMock);
    const addToast = vi.fn();
    const { result } = renderHook(() =>
      useGraphFileImport({ sessionId: 'ws-1-1', connected: true, hasGraph: false, addToast }));

    expect(result.current.canImport).toBe(true);
    await act(async () => { await result.current.importFiles([jsonFile('demo.json', MODEL)]); });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/graph/import/ws-1-1');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body as string)).toEqual(MODEL);
    // the engine's console line refreshes the view and announces the import; no toast here
    expect(addToast).not.toHaveBeenCalled();
    expect(result.current.pending).toBeNull();
  });

  it('asks before replacing a loaded graph, then posts on confirm', async () => {
    const fetchMock = vi.fn().mockResolvedValue(okResponse());
    vi.stubGlobal('fetch', fetchMock);
    const { result } = renderHook(() =>
      useGraphFileImport({ sessionId: 'ws-1-1', connected: true, hasGraph: true, addToast: vi.fn() }));

    await act(async () => { await result.current.importFiles([jsonFile('demo.json', MODEL)]); });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.pending).toMatchObject({ fileName: 'demo.json', name: 'demo', nodeCount: 2, connectionCount: 1 });

    act(() => result.current.confirmPending());
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(result.current.pending).toBeNull();
  });

  it('drops the pending file on cancel', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const { result } = renderHook(() =>
      useGraphFileImport({ sessionId: 'ws-1-1', connected: true, hasGraph: true, addToast: vi.fn() }));

    await act(async () => { await result.current.importFiles([jsonFile('demo.json', MODEL)]); });
    act(() => result.current.cancelPending());
    expect(result.current.pending).toBeNull();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('refuses when disconnected or when the session id is not known yet', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const addToast = vi.fn();
    const disconnected = renderHook(() =>
      useGraphFileImport({ sessionId: 'ws-1-1', connected: false, hasGraph: false, addToast }));
    expect(disconnected.result.current.canImport).toBe(false);
    await act(async () => { await disconnected.result.current.importFiles([jsonFile('demo.json', MODEL)]); });
    expect(addToast).toHaveBeenLastCalledWith('Connect first to import a graph.', 'error');

    const noSession = renderHook(() =>
      useGraphFileImport({ sessionId: null, connected: true, hasGraph: false, addToast }));
    await act(async () => { await noSession.result.current.importFiles([jsonFile('demo.json', MODEL)]); });
    expect(addToast).toHaveBeenCalledTimes(2);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('refuses a file that is not JSON, not valid JSON, or not a graph model', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const addToast = vi.fn();
    const { result } = renderHook(() =>
      useGraphFileImport({ sessionId: 'ws-1-1', connected: true, hasGraph: false, addToast }));

    await act(async () => {
      await result.current.importFiles([new File(['hello'], 'notes.txt', { type: 'text/markdown' })]);
    });
    expect(addToast.mock.lastCall?.[0]).toContain('does not appear to be a JSON file');

    await act(async () => { await result.current.importFiles([jsonFile('broken.json', '{"nodes": [')]); });
    expect(addToast.mock.lastCall?.[0]).toContain('"broken.json" is not valid JSON');

    await act(async () => {
      await result.current.importFiles([jsonFile('other.json', { nodes: MODEL.nodes, metadata: {} })]);
    });
    expect(addToast.mock.lastCall?.[0]).toContain('"other.json" is not a graph model: Unexpected top-level section: metadata.');

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('reports the engine refusal with its message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      '{"status":400,"message":"Unexpected top-level section(s): metadata - a graph model has only \'nodes\' and \'connections\'","type":"error"}',
      { status: 400, headers: { 'Content-Type': 'application/json' } },
    )));
    const addToast = vi.fn();
    const { result } = renderHook(() =>
      useGraphFileImport({ sessionId: 'ws-1-1', connected: true, hasGraph: false, addToast }));

    await act(async () => { await result.current.importFiles([jsonFile('demo.json', MODEL)]); });
    expect(addToast).toHaveBeenCalledWith(
      'Import of "demo.json" refused - Unexpected top-level section(s): metadata - a graph model has only \'nodes\' and \'connections\'',
      'error',
    );
    expect(result.current.isImporting).toBe(false);
  });

  it('takes one file at a time', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const addToast = vi.fn();
    const { result } = renderHook(() =>
      useGraphFileImport({ sessionId: 'ws-1-1', connected: true, hasGraph: false, addToast }));

    await act(async () => {
      await result.current.importFiles([jsonFile('a.json', MODEL), jsonFile('b.json', MODEL)]);
    });
    expect(addToast).toHaveBeenCalledWith('Drop one graph file at a time.', 'error');
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
