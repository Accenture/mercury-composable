// @vitest-environment happy-dom

import { type ReactNode } from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import GraphView from '../GraphView';
import type { GraphImageFormat, GraphRect } from '../../../utils/graphImage';

const mocks = vi.hoisted(() => ({
  renderGraphImage: vi.fn(),
  nodesBounds: { x: 0, y: -50, width: 600, height: 100 } as GraphRect,
  // Stable setters, as React Flow's are: GraphView's re-sync effect depends on them.
  setNodes: vi.fn(),
  onNodesChange: vi.fn(),
  setEdges: vi.fn(),
  onEdgesChange: vi.fn(),
}));

// React Flow renders its viewport element and hands GraphView an instance
// whose node bounds are fixed; the nodes themselves are not needed here.
vi.mock('@xyflow/react', () => ({
  ReactFlow: ({ children, onInit }: { children: ReactNode; onInit?: (instance: unknown) => void }) => {
    onInit?.({
      getNodes: () => [],
      getNodesBounds: () => mocks.nodesBounds,
      fitView: () => undefined,
    });
    return (
      <div className="react-flow">
        <div className="react-flow__viewport" />
        {children}
      </div>
    );
  },
  Background: () => null,
  ControlButton: ({ children }: { children: ReactNode }) => <button>{children}</button>,
  useNodesState: (nodes: unknown[]) => [nodes, mocks.setNodes, mocks.onNodesChange],
  useEdgesState: (edges: unknown[]) => [edges, mocks.setEdges, mocks.onEdgesChange],
  BackgroundVariant: { Dots: 'dots' },
  SelectionMode: { Partial: 'partial' },
}));

vi.mock('../NodeTypes', () => ({
  AUTHORING_SOURCE_HANDLE_ID: 'authoring-source',
  AUTHORING_TARGET_HANDLE_ID: 'authoring-target',
  nodeTypes: {},
}));

vi.mock('../../../utils/graphTransformer', () => ({
  transformGraphData: () => ({
    nodes: [{ id: 'root', type: 'Root', position: { x: 0, y: 0 }, data: {} }],
    edges: [],
  }),
}));

vi.mock('../../../utils/graphImage', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../../utils/graphImage')>()),
  renderGraphImage: mocks.renderGraphImage,
}));

// The real toolbar opens a menu; these two buttons stand for its items.
vi.mock('../../GraphToolbar/GraphToolbar', () => ({
  default: ({ onExportImage, exportingImage }: {
    onExportImage?: (format: GraphImageFormat) => void;
    exportingImage?: boolean;
  }) => (
    <div data-exporting-image={String(Boolean(exportingImage))}>
      <button type="button" onClick={() => onExportImage?.('png')}>Export PNG</button>
      <button type="button" onClick={() => onExportImage?.('svg')}>Export SVG</button>
    </div>
  ),
}));

vi.mock('../GraphContextMenu', () => ({ default: () => null }));
vi.mock('../NodeContextMenu', () => ({ default: () => null }));
vi.mock('../EdgeContextMenu', () => ({ default: () => null }));
vi.mock('../GraphMinimap', () => ({ default: ({ children }: { children?: ReactNode }) => <div>{children}</div> }));

type PickerWindow = Window & { showSaveFilePicker?: unknown };

const GRAPH = {
  nodes: [{ alias: 'root', types: ['Root'], properties: { name: 'tutorial-1' } }],
  connections: [],
};

afterEach(() => {
  cleanup();
  delete (window as PickerWindow).showSaveFilePicker;
  vi.clearAllMocks();
});

function mockSaveDialog(name: string) {
  const write = vi.fn().mockResolvedValue(undefined);
  const picker = vi.fn().mockResolvedValue({ name, createWritable: async () => ({ write, close: vi.fn() }) });
  (window as PickerWindow).showSaveFilePicker = picker;
  return { picker, write };
}

describe('GraphView image export', () => {
  it('renders the whole graph with the selection styling off, saves it and reports the file', async () => {
    const { picker, write } = mockSaveDialog('tutorial-1.png');
    const png = new Blob(['png'], { type: 'image/png' });
    let surfaceDuringRender: HTMLElement | null = null;
    mocks.renderGraphImage.mockImplementation(async () => {
      surfaceDuringRender = document.querySelector('[data-exporting]');
      return png;
    });
    const onExportImageSaved = vi.fn();
    render(<GraphView graphData={GRAPH} graphName="tutorial-1" isActive isConnected onExportImageSaved={onExportImageSaved} />);

    fireEvent.click(screen.getByRole('button', { name: 'Export PNG' }));

    await waitFor(() => expect(onExportImageSaved).toHaveBeenCalledWith('tutorial-1.png'));
    expect(picker.mock.calls[0][0].suggestedName).toBe('tutorial-1.png');
    expect(surfaceDuringRender).not.toBeNull();
    expect(surfaceDuringRender!.querySelector('.react-flow__viewport')).not.toBeNull();
    const [viewport, bounds, format] = mocks.renderGraphImage.mock.calls[0];
    expect((viewport as HTMLElement).className).toBe('react-flow__viewport');
    expect(bounds).toEqual(mocks.nodesBounds);
    expect(format).toBe('png');
    expect(write).toHaveBeenCalledWith(png);
    expect(document.querySelector('[data-exporting]')).toBeNull();
    expect(document.querySelector('[data-exporting-image]')!.getAttribute('data-exporting-image')).toBe('false');
  });

  it('saves the SVG under <graph-id>.svg', async () => {
    const { picker } = mockSaveDialog('tutorial-1.svg');
    mocks.renderGraphImage.mockResolvedValue(new Blob(['<svg/>'], { type: 'image/svg+xml' }));
    const onExportImageSaved = vi.fn();
    render(<GraphView graphData={GRAPH} graphName="tutorial-1" isActive isConnected onExportImageSaved={onExportImageSaved} />);

    fireEvent.click(screen.getByRole('button', { name: 'Export SVG' }));

    await waitFor(() => expect(onExportImageSaved).toHaveBeenCalledWith('tutorial-1.svg'));
    expect(picker.mock.calls[0][0].suggestedName).toBe('tutorial-1.svg');
    expect(mocks.renderGraphImage.mock.calls[0][2]).toBe('svg');
  });

  it('reports a failed render and switches the selection styling back on', async () => {
    mockSaveDialog('tutorial-1.png');
    mocks.renderGraphImage.mockRejectedValue(new Error('The browser could not draw the image.'));
    const onExportImageSaved = vi.fn();
    const onExportImageError = vi.fn();
    render(
      <GraphView
        graphData={GRAPH}
        graphName="tutorial-1"
        isActive
        isConnected
        onExportImageSaved={onExportImageSaved}
        onExportImageError={onExportImageError}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Export PNG' }));

    await waitFor(() => expect(onExportImageError).toHaveBeenCalledWith('The browser could not draw the image.'));
    expect(onExportImageSaved).not.toHaveBeenCalled();
    expect(document.querySelector('[data-exporting]')).toBeNull();
  });

  it('reports nothing when the save dialog is cancelled', async () => {
    (window as PickerWindow).showSaveFilePicker = vi.fn()
      .mockRejectedValue(Object.assign(new Error('cancelled'), { name: 'AbortError' }));
    const onExportImageSaved = vi.fn();
    const onExportImageError = vi.fn();
    render(
      <GraphView
        graphData={GRAPH}
        graphName="tutorial-1"
        isActive
        isConnected
        onExportImageSaved={onExportImageSaved}
        onExportImageError={onExportImageError}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Export PNG' }));

    await waitFor(() => expect((window as PickerWindow).showSaveFilePicker).toHaveBeenCalled());
    await Promise.resolve();
    expect(mocks.renderGraphImage).not.toHaveBeenCalled();
    expect(onExportImageSaved).not.toHaveBeenCalled();
    expect(onExportImageError).not.toHaveBeenCalled();
  });
});
