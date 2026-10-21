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

describe('GraphToolbar Export PNG button', () => {
  it('sits after Download and before Copy and names the file in its tooltip', () => {
    render(<GraphToolbar graphData={GRAPH} graphName="My Graph" onDownload={vi.fn()} onExportImage={vi.fn()} />);

    expect(screen.getAllByRole('button').map((button) => button.getAttribute('aria-label'))).toEqual([
      'Download graph as a JSON file',
      'Export graph as a PNG image',
      'Copy raw graph JSON to clipboard',
    ]);
    const exportButton = screen.getByRole('button', { name: 'Export graph as a PNG image' });
    expect(exportButton.getAttribute('title')).toBe('Export the whole graph as My-Graph.png');
    expect(exportButton.hasAttribute('aria-haspopup')).toBe(false);
  });

  it('exports straight from the click', () => {
    const onExportImage = vi.fn();
    render(<GraphToolbar graphData={GRAPH} graphName="demo" onExportImage={onExportImage} />);

    fireEvent.click(screen.getByRole('button', { name: 'Export graph as a PNG image' }));

    expect(onExportImage).toHaveBeenCalledTimes(1);
  });

  it('is disabled while the image is being exported, and without a graph', () => {
    const { rerender } = render(
      <GraphToolbar graphData={GRAPH} graphName="demo" onExportImage={vi.fn()} exportingImage />,
    );
    let exportButton = screen.getByRole('button', { name: 'Export graph as a PNG image' }) as HTMLButtonElement;
    expect(exportButton.disabled).toBe(true);
    expect(exportButton.title).toBe('Exporting the graph image…');

    rerender(<GraphToolbar graphData={null} onExportImage={vi.fn()} />);
    exportButton = screen.getByRole('button', { name: 'Export graph as a PNG image' }) as HTMLButtonElement;
    expect(exportButton.disabled).toBe(true);
    expect(exportButton.title).toBe('No graph to export');
  });
});
