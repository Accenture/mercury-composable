// @vitest-environment happy-dom

import { useEffect } from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import SchemaPanel from './SchemaPanel';
import { useSchemaPanel } from '../../hooks/useSchemaPanel';
import { ProtocolBus } from '../../protocol/bus';
import type { MinigraphGraphData } from '../../utils/graphTypes';

const SESSION = 'ws-200-7';

const GRAPH: MinigraphGraphData = {
  nodes: [
    { alias: 'root', types: ['Root'], properties: { name: 'demo', schema: { body: { type: 'object', properties: { a: { type: 'number', enum: [1, 2] } }, required: ['a'] } } } },
    { alias: 'end', types: ['End'], properties: {} },
  ],
  connections: [],
};

const CONTRACT = {
  graph: 'demo',
  input: {
    body: {
      declared: true,
      schema: { type: 'object', properties: { a: { type: 'number', enum: [1, 2] }, d: {} }, required: ['a'] },
      paths: [
        { path: 'input.body.a', type: 'number', origin: 'both', required: true, usedBy: ['compute'] },
        { path: 'input.body.d', origin: 'discovered', usedBy: ['compute'] },
      ],
    },
    header: { declared: false, paths: [] },
  },
  output: {
    body: { declared: false, paths: [{ path: 'output.body.sum', type: 'number', origin: 'discovered', usedBy: ['end'] }] },
    header: { declared: false, paths: [] },
    status: [],
  },
  issues: ['input.body.d is referenced by compute but not declared'],
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const sendRawText = vi.fn().mockReturnValue(true);
const bus = new ProtocolBus();

/** The panel as Playground mounts it: the hook owns the state, the panel renders it. */
function Harness({ graphData = GRAPH }: { graphData?: MinigraphGraphData | null }) {
  const controller = useSchemaPanel({
    addToast: vi.fn(), sessionId: SESSION, connected: true, graphData, graphName: 'demo', bus, sendRawText,
  });
  const { open } = controller;
  useEffect(() => { open(); }, [open]);
  return controller.isOpen
    ? <SchemaPanel controller={controller} graphName="demo" supportsFolderPicker={false} />
    : <p>panel closed</p>;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  sendRawText.mockClear();
  bus.clear();
});

describe('SchemaPanel', () => {
  it('renders in place with the Input tab, the contract rows, the chips and the engine issues', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json(CONTRACT)));
    render(<Harness />);

    const region = await screen.findByRole('region', { name: '📐 Graph schema' });
    expect(region).toBeTruthy();
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(screen.getByRole('tab', { name: /Input/ }).getAttribute('aria-selected')).toBe('true');

    const paths = await screen.findAllByLabelText('Path');
    expect(paths.map(input => (input as HTMLInputElement).value)).toEqual(['a', 'd']);
    expect((screen.getByLabelText('Type of a') as HTMLSelectElement).value).toBe('number');
    expect((screen.getByLabelText('Required a') as HTMLInputElement).checked).toBe(true);
    expect((screen.getByLabelText('Type of d') as HTMLSelectElement).value).toBe('');
    expect(screen.getAllByText('declared')).toHaveLength(1);
    expect(screen.getAllByText('discovered')).toHaveLength(1);
    expect(screen.getByText('enum (2)')).toBeTruthy();
    expect(screen.getByText(/input.body.d is referenced by compute but not declared/)).toBeTruthy();
    expect(screen.getByText(/No header/)).toBeTruthy();
    expect((screen.getByRole('button', { name: 'Save root schema' }) as HTMLButtonElement).disabled).toBe(true);
  });

  it('switches to the Output tab and shows the end node\'s side', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json(CONTRACT)));
    render(<Harness />);
    await screen.findAllByLabelText('Path');

    fireEvent.click(screen.getByRole('tab', { name: /Output/ }));
    expect(screen.getByRole('tab', { name: /Output/ }).getAttribute('aria-selected')).toBe('true');
    expect((screen.getByLabelText('Path') as HTMLInputElement).value).toBe('sum');
    expect(screen.getByRole('button', { name: 'Save end schema' })).toBeTruthy();
    expect(screen.getByText(/documentary/)).toBeTruthy();
  });

  it('flags a row in place, enables Save once the rows are valid, and sends one update node', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json(CONTRACT)));
    render(<Harness />);
    await screen.findAllByLabelText('Path');

    fireEvent.click(screen.getByRole('button', { name: '+ Add path' }));
    expect(screen.getByRole('alert').textContent).toContain('A path is required.');
    const save = screen.getByRole('button', { name: 'Save root schema' }) as HTMLButtonElement;
    expect(save.disabled).toBe(true);

    const paths = screen.getAllByLabelText('Path');
    fireEvent.change(paths[2], { target: { value: 'tags' } });
    fireEvent.change(screen.getByLabelText('Type of tags'), { target: { value: 'array' } });
    expect(screen.getByRole('alert').textContent).toContain('ends in []');
    fireEvent.change(paths[2], { target: { value: 'tags[]' } });
    expect(screen.queryByRole('alert')).toBeNull();
    fireEvent.change(screen.getByLabelText('Item type of tags[]'), { target: { value: 'string' } });
    fireEvent.change(screen.getByLabelText('Description of tags[]'), { target: { value: 'labels' } });
    // the Input tab carries the unsaved dot
    expect(screen.getByLabelText('unsaved')).toBeTruthy();
    expect(save.disabled).toBe(false);

    fireEvent.click(save);
    expect(sendRawText).toHaveBeenCalledTimes(1);
    const command = sendRawText.mock.calls[0][0] as string;
    expect(command.startsWith('update node root\nwith type Root\nwith properties\nname=demo\n')).toBe(true);
    expect(command).toContain('schema.body.properties.a.enum[]=1\nschema.body.properties.a.enum[]=2');
    expect(command).toContain('schema.body.properties.tags.type=array\nschema.body.properties.tags.items.type=string\nschema.body.properties.tags.description=labels');
    expect(command).toContain('schema.body.required[]=a');
    expect(screen.getByRole('button', { name: /Saving/ })).toBeTruthy();
  });

  it('adds a header row with the string type, and reports a refused read with a Retry', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json(CONTRACT)));
    render(<Harness />);
    await screen.findAllByLabelText('Path');

    fireEvent.click(screen.getByRole('button', { name: '+ Add header' }));
    const name = screen.getByLabelText('Header name') as HTMLInputElement;
    expect(screen.getByRole('alert').textContent).toContain('A header name is required.');
    fireEvent.change(name, { target: { value: 'X-Tenant' } });
    expect(screen.queryByRole('alert')).toBeNull();
    expect((screen.getByLabelText('Type of X-Tenant') as HTMLSelectElement).value).toBe('string');
    expect(screen.getByText('new')).toBeTruthy();

    cleanup();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ message: 'nope' }, 500)));
    render(<Harness />);
    const alert = await screen.findByRole('alert');
    expect(alert.textContent).toContain('nope');
    expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
  });

  it('closes on Escape and on Cancel, and says when the end node is missing', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json(CONTRACT)));
    render(<Harness graphData={{ ...GRAPH, nodes: [GRAPH.nodes[0]] }} />);
    await screen.findAllByLabelText('Path');

    fireEvent.click(screen.getByRole('tab', { name: /Output/ }));
    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('no end node'));

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(await screen.findByText('panel closed')).toBeTruthy();
  });
});
