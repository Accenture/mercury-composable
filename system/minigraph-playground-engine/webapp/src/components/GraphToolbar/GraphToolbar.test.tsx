// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import GraphToolbar from './GraphToolbar';
import type { MinigraphGraphData } from '../../utils/graphTypes';

const GRAPH: MinigraphGraphData = {
  nodes: [{ alias: 'root', types: ['Root'], properties: { name: 'demo' } }],
  connections: [],
};

afterEach(cleanup);

describe('GraphToolbar file actions', () => {
  it('renders Import, Download and Copy in that order when the callbacks are wired', () => {
    render(<GraphToolbar graphData={GRAPH} graphName="demo" onImport={vi.fn()} onDownload={vi.fn()} />);

    expect(screen.getAllByRole('button').map((button) => button.getAttribute('aria-label'))).toEqual([
      'Import graph model from a JSON file',
      'Download graph as a JSON file',
      'Copy raw graph JSON to clipboard',
    ]);
  });

  it('omits the file actions when no callback is wired', () => {
    render(<GraphToolbar graphData={GRAPH} graphName="demo" />);

    expect(screen.getAllByRole('button')).toHaveLength(1);
  });

  it('calls the callbacks and names the download file after the graph id', () => {
    const onImport = vi.fn();
    const onDownload = vi.fn();
    render(<GraphToolbar graphData={GRAPH} graphName="My Graph" onImport={onImport} onDownload={onDownload} />);

    fireEvent.click(screen.getByRole('button', { name: 'Import graph model from a JSON file' }));
    fireEvent.click(screen.getByRole('button', { name: 'Download graph as a JSON file' }));

    expect(onImport).toHaveBeenCalledTimes(1);
    expect(onDownload).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: 'Download graph as a JSON file' }).getAttribute('title'))
      .toBe('Download the graph as My-Graph.json');
  });

  it('disables Import with its reason, and Download without a graph', () => {
    render(
      <GraphToolbar
        graphData={null}
        onImport={vi.fn()}
        importDisabledReason="Connect first to import a graph"
        onDownload={vi.fn()}
      />,
    );

    const importButton = screen.getByRole('button', { name: 'Import graph model from a JSON file' }) as HTMLButtonElement;
    expect(importButton.disabled).toBe(true);
    expect(importButton.title).toBe('Connect first to import a graph');
    const downloadButton = screen.getByRole('button', { name: 'Download graph as a JSON file' }) as HTMLButtonElement;
    expect(downloadButton.disabled).toBe(true);
    expect(downloadButton.title).toBe('No graph to download');
  });
});

describe('GraphToolbar Export image menu', () => {
  it('sits after Download and before Copy, closed, and names both files in its tooltip', () => {
    render(<GraphToolbar graphData={GRAPH} graphName="My Graph" onDownload={vi.fn()} onExportImage={vi.fn()} />);

    expect(screen.getAllByRole('button').map((button) => button.getAttribute('aria-label'))).toEqual([
      'Download graph as a JSON file',
      'Export graph as an image',
      'Copy raw graph JSON to clipboard',
    ]);
    const exportButton = screen.getByRole('button', { name: 'Export graph as an image' });
    expect(exportButton.getAttribute('aria-haspopup')).toBe('menu');
    expect(exportButton.getAttribute('aria-expanded')).toBe('false');
    expect(exportButton.getAttribute('title'))
      .toBe('Export the whole graph as an image (My-Graph.png or My-Graph.svg)');
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('offers Export PNG and Export SVG, focusing the first, and hands the format over', () => {
    const onExportImage = vi.fn();
    render(<GraphToolbar graphData={GRAPH} graphName="demo" onExportImage={onExportImage} />);

    fireEvent.click(screen.getByRole('button', { name: 'Export graph as an image' }));

    const items = screen.getAllByRole('menuitem');
    expect(items.map((item) => item.textContent)).toEqual(['Export PNG', 'Export SVG']);
    expect(document.activeElement).toBe(items[0]);
    expect(screen.getByRole('button', { name: 'Export graph as an image' }).getAttribute('aria-expanded')).toBe('true');

    fireEvent.click(items[1]);

    expect(onExportImage).toHaveBeenCalledWith('svg');
    expect(screen.queryByRole('menu')).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Export graph as an image' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Export PNG' }));
    expect(onExportImage).toHaveBeenLastCalledWith('png');
  });

  it('closes on Escape, returning focus to the button, and on a pointer down outside', () => {
    render(<GraphToolbar graphData={GRAPH} graphName="demo" onExportImage={vi.fn()} />);
    const exportButton = screen.getByRole('button', { name: 'Export graph as an image' });

    fireEvent.click(exportButton);
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByRole('menu')).toBeNull();
    expect(document.activeElement).toBe(exportButton);

    fireEvent.click(exportButton);
    fireEvent.pointerDown(document.body);
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('is disabled while an image is being exported, and without a graph', () => {
    const { rerender } = render(
      <GraphToolbar graphData={GRAPH} graphName="demo" onExportImage={vi.fn()} exportingImage />,
    );
    let exportButton = screen.getByRole('button', { name: 'Export graph as an image' }) as HTMLButtonElement;
    expect(exportButton.disabled).toBe(true);
    expect(exportButton.title).toBe('Exporting the graph image…');

    rerender(<GraphToolbar graphData={null} onExportImage={vi.fn()} />);
    exportButton = screen.getByRole('button', { name: 'Export graph as an image' }) as HTMLButtonElement;
    expect(exportButton.disabled).toBe(true);
    expect(exportButton.title).toBe('No graph to export');
  });
});
