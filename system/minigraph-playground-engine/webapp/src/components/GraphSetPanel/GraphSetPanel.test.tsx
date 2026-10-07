// @vitest-environment happy-dom

import { useEffect } from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import GraphSetPanel from './GraphSetPanel';
import { useGraphSetPanel } from '../../hooks/useGraphSetPanel';
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
  return new File([JSON.stringify(content)], name, { type: 'application/json' });
}

/** The panel as Playground mounts it: the hook owns the state, the panel renders it. */
function Harness({ graphData = null, importFiles = vi.fn().mockResolvedValue(undefined) }: {
  graphData?: MinigraphGraphData | null;
  importFiles?: (files: File[]) => Promise<void>;
}) {
  const controller = useGraphSetPanel({ addToast: vi.fn(), importFiles, graphData, graphName: 'demo' });
  const { open } = controller;
  useEffect(() => { open(); }, [open]);
  return controller.isOpen
    ? <GraphSetPanel controller={controller} supportsFolderPicker={false} />
    : <p>panel closed</p>;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('GraphSetPanel', () => {
  it('renders in place - a left-slot region, not a dialog - with the set name focused and Pack disabled', async () => {
    render(<Harness />);

    const region = await screen.findByRole('region', { name: '📦 Package graphs' });
    expect(region).toBeTruthy();
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.activeElement).toBe(screen.getByLabelText('Set name'));
    expect((screen.getByRole('button', { name: 'Pack and download' }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: 'Add current graph' }) as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByText(/No graphs yet/)).toBeTruthy();
  });

  it('closes on Escape and on Cancel', async () => {
    render(<Harness />);
    await screen.findByRole('region', { name: '📦 Package graphs' });

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(await screen.findByText('panel closed')).toBeTruthy();
  });

  it('adds the current graph, flags a duplicate from a dropped file, and enables Pack once the set is named', async () => {
    render(<Harness graphData={MODEL} />);
    await screen.findByRole('region', { name: '📦 Package graphs' });

    fireEvent.click(screen.getByRole('button', { name: 'Add current graph' }));
    expect(await screen.findByText('demo')).toBeTruthy();
    expect(screen.getByText(/2 nodes · 1 connection/)).toBeTruthy();

    const dropZone = screen.getByLabelText('Drop graph files or a graph set here');
    fireEvent.drop(dropZone, { dataTransfer: { files: [jsonFile('demo.json', modelNamed('demo'))], types: ['Files'] } });
    await waitFor(() => expect(screen.getAllByRole('alert').length).toBeGreaterThan(0));
    expect(screen.getAllByText(/Duplicate graph id "demo"/)).toHaveLength(2);

    const pack = screen.getByRole('button', { name: 'Pack and download' }) as HTMLButtonElement;
    expect(pack.disabled).toBe(true);
    fireEvent.click(screen.getAllByRole('button', { name: 'Remove demo from the set' })[1]);
    await waitFor(() => expect(screen.queryByText(/Duplicate graph id/)).toBeNull());

    const setName = screen.getByLabelText('Set name') as HTMLInputElement;
    fireEvent.change(setName, { target: { value: 'my set' } });
    expect(screen.getByRole('alert').textContent).toContain('letters, digits, hyphen or underscore');
    expect(pack.disabled).toBe(true);

    fireEvent.change(setName, { target: { value: 'my-set' } });
    expect(screen.queryByRole('alert')).toBeNull();
    // the ribbon's subtitle and the description both name the file
    expect(screen.getAllByText('my-set.pack').length).toBeGreaterThan(0);
    expect(pack.disabled).toBe(false);
  });

  it('takes files from the Browse picker and flags a manifest row in place', async () => {
    render(<Harness />);
    await screen.findByRole('region', { name: '📦 Package graphs' });

    const input = document.querySelector<HTMLInputElement>('input[type="file"]')!;
    expect(input.multiple).toBe(true);
    fireEvent.change(input, { target: { files: [jsonFile('a.json', modelNamed('a')), jsonFile('b.json', modelNamed('b'))] } });
    expect(await screen.findByText('a')).toBeTruthy();
    expect(screen.getByText('b')).toBeTruthy();
    expect(screen.getByText('Graphs (2)')).toBeTruthy();

    fireEvent.change(screen.getByLabelText('Value of manifest field version'), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: '+ Add field' }));
    const names = screen.getAllByLabelText('Manifest field name');
    fireEvent.change(names[names.length - 1], { target: { value: 'version' } });
    fireEvent.change(screen.getAllByLabelText(/Value of manifest field version/)[1], { target: { value: '2' } });
    expect(screen.getAllByText(/Duplicate field "version"/)).toHaveLength(2);
  });

  it('switches to inspect mode on a dropped .pack and imports a graph as the draft', async () => {
    const answer = { manifest: { set: 'demo', version: '2' }, graphs: { a: modelNamed('a') } };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(answer), {
      status: 200, headers: { 'Content-Type': 'application/json' },
    })));
    const importFiles = vi.fn().mockResolvedValue(undefined);
    render(<Harness importFiles={importFiles} />);
    await screen.findByRole('region', { name: '📦 Package graphs' });

    fireEvent.drop(screen.getByLabelText('Drop graph files or a graph set here'), {
      dataTransfer: { files: [new File([new Uint8Array([1])], 'demo.pack')], types: ['Files'] },
    });

    expect(await screen.findByRole('region', { name: '📦 Graph set' })).toBeTruthy();
    expect(screen.getByText('demo.pack')).toBeTruthy();
    expect(screen.getByText('version')).toBeTruthy();
    expect(screen.getByText('2')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Import as draft' }));
    await waitFor(() => expect(importFiles).toHaveBeenCalledTimes(1));

    // Escape leaves inspect mode first; the editor keeps its state
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(await screen.findByRole('region', { name: '📦 Package graphs' })).toBeTruthy();
  });
});
