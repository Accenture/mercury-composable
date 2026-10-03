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
