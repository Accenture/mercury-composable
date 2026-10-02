// @vitest-environment happy-dom
/// <reference types="node" />

import { readFileSync } from 'node:fs';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import GraphRunControls, { type GraphRunControlsProps } from './GraphRunControls';
import GraphToolbar from './GraphToolbar';

const graphToolbarCss = readFileSync(
  'src/components/GraphToolbar/GraphToolbar.module.css',
  'utf8',
);

afterEach(cleanup);

function controls(overrides: Partial<GraphRunControlsProps> = {}) {
  const props: GraphRunControlsProps = {
    phase: 'idle',
    canInstantiate: true,
    canUpload: false,
    canRun: false,
    disabledReason: '',
    inputBodyPaths: [],
    onInstantiate: vi.fn(),
    onUpload: vi.fn(),
    onRun: vi.fn(),
    ...overrides,
  };
  return { props, element: <GraphRunControls {...props} /> };
}

function button(name: string): HTMLButtonElement {
  return screen.getByRole('button', { name }) as HTMLButtonElement;
}

describe('GraphRunControls', () => {
  it('requires Instantiate before enabling the optional Upload step and Run', () => {
    const idle = controls();
    const { rerender } = render(idle.element);

    expect(button('Instantiate graph').disabled).toBe(false);
    expect(button('Upload mock input').disabled).toBe(true);
    expect(button('Run graph').disabled).toBe(true);
    fireEvent.click(button('Instantiate graph'));
    expect(idle.props.onInstantiate).toHaveBeenCalledTimes(1);
    expect(idle.props.onUpload).not.toHaveBeenCalled();
    expect(idle.props.onRun).not.toHaveBeenCalled();

    const ready = controls({ phase: 'ready', canUpload: true, canRun: true });
    rerender(ready.element);

    // Instantiate stays available while Ready: it starts a fresh instance.
    expect(button('Instantiate graph').disabled).toBe(false);
    expect(button('Upload mock input').disabled).toBe(false);
    const readyRun = button('Run instantiated graph');
    expect(readyRun.disabled).toBe(false);
    fireEvent.click(button('Upload mock input'));
    fireEvent.click(readyRun);
    expect(ready.props.onUpload).toHaveBeenCalledTimes(1);
    expect(ready.props.onRun).toHaveBeenCalledTimes(1);
  });

  it('disables all three toolbar actions while setup is busy or the graph runs', () => {
    const { rerender } = render(controls({ phase: 'instantiating', canInstantiate: false }).element);

    const instantiate = button('Graph is being instantiated');
    expect(instantiate.disabled).toBe(true);
    expect(instantiate.getAttribute('aria-busy')).toBe('true');
    expect(button('Upload mock input').disabled).toBe(true);
    expect(button('Run graph').disabled).toBe(true);

    rerender(controls({ phase: 'running', canInstantiate: false }).element);
    expect(button('Instantiate graph').disabled).toBe(true);
    expect(button('Upload mock input').disabled).toBe(true);
    expect(button('Graph is running').textContent).toContain('Running…');
  });

  it('renders Instantiate, Upload and Run in order before the existing Copy action', () => {
    render(
      <GraphToolbar
        graphData={{ nodes: [{ alias: 'root', types: ['Root'], properties: {} }], connections: [] }}
        extraActions={controls().element}
      />,
    );

    const instantiate = button('Instantiate graph');
    const upload = button('Upload mock input');
    const run = button('Run graph');
    const copy = button('Copy raw graph JSON to clipboard');
    expect(instantiate.compareDocumentPosition(upload) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(upload.compareDocumentPosition(run) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(run.compareDocumentPosition(copy) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('uses one decorative 16px SVG family instead of platform text glyphs', () => {
    render(
      <GraphToolbar
        graphData={{ nodes: [{ alias: 'root', types: ['Root'], properties: {} }], connections: [] }}
        extraActions={controls().element}
      />,
    );

    const buttons = [
      button('Instantiate graph'),
      button('Upload mock input'),
      button('Run graph'),
      button('Copy raw graph JSON to clipboard'),
    ];

    for (const element of buttons) {
      const icon = element.querySelector('svg');
      expect(icon, element.outerHTML).not.toBeNull();
      expect(icon?.getAttribute('viewBox')).toBe('0 0 16 16');
      expect(icon?.getAttribute('width')).toBe('16');
      expect(icon?.getAttribute('height')).toBe('16');
      expect(icon?.getAttribute('fill')).toBe('none');
      expect(icon?.getAttribute('stroke')).toBe('currentColor');
      expect(icon?.getAttribute('stroke-width')).toBe('1.5');
      expect(icon?.getAttribute('stroke-linecap')).toBe('round');
      expect(icon?.getAttribute('stroke-linejoin')).toBe('round');
      expect(icon?.getAttribute('aria-hidden')).toBe('true');
    }
    expect(buttons[2].textContent).not.toContain('▶');
    expect(buttons[3].textContent).not.toContain('📑');
  });

  it('shows each custom tooltip for its own hover or keyboard focus', () => {
    const idle = controls();
    const { rerender } = render(idle.element);

    const instantiate = button('Instantiate graph');
    const upload = button('Upload mock input');
    const run = button('Run graph');
    const [instantiateTooltip, uploadTooltip, runTooltip] = screen.getAllByRole('tooltip');

    expect(instantiateTooltip.textContent).toBe('Create a runnable instance of the current graph.');
    expect(uploadTooltip.textContent).toBe(
      'Upload a JSON payload as the mock input.body of the instance (optional). Only you see the form. Instantiate the graph first.',
    );
    expect(runTooltip.textContent).toBe(
      'Run the instantiated graph, with the uploaded mock input if any. Instantiate the graph first.',
    );
    expect(run.disabled).toBe(true);
    expect(instantiate.getAttribute('aria-describedby')).toBe(instantiateTooltip.id);
    expect(upload.getAttribute('aria-describedby')).toBe(uploadTooltip.id);
    expect(run.getAttribute('aria-describedby')).toBe(runTooltip.id);
    expect(new Set([instantiateTooltip.id, uploadTooltip.id, runTooltip.id]).size).toBe(3);
    expect(instantiateTooltip.parentElement).toBe(instantiate.parentElement);
    expect(runTooltip.parentElement).toBe(run.parentElement);
    expect(instantiate.getAttribute('title')).toBeNull();
    expect(run.getAttribute('title')).toBeNull();
    expect(instantiateTooltip.getAttribute('data-state')).toBe('closed');
    expect(runTooltip.getAttribute('data-state')).toBe('closed');

    fireEvent.focus(instantiate);
    expect(instantiateTooltip.getAttribute('data-state')).toBe('open');
    expect(runTooltip.getAttribute('data-state')).toBe('closed');
    fireEvent.blur(instantiate, { relatedTarget: document.body });
    expect(instantiateTooltip.getAttribute('data-state')).toBe('closed');

    const disabledRunAnchor = run.parentElement!;
    expect(disabledRunAnchor.getAttribute('tabindex')).toBe('0');
    expect(disabledRunAnchor.getAttribute('aria-describedby')).toBe(runTooltip.id);
    fireEvent.focus(disabledRunAnchor);
    expect(runTooltip.getAttribute('data-state')).toBe('open');
    fireEvent.blur(disabledRunAnchor, { relatedTarget: document.body });
    expect(runTooltip.getAttribute('data-state')).toBe('closed');

    fireEvent.mouseEnter(instantiate.parentElement!);
    expect(instantiateTooltip.getAttribute('data-state')).toBe('open');
    expect(runTooltip.getAttribute('data-state')).toBe('closed');
    fireEvent.click(instantiate);
    expect(idle.props.onInstantiate).toHaveBeenCalledTimes(1);
    fireEvent.mouseLeave(instantiate.parentElement!);
    expect(instantiateTooltip.getAttribute('data-state')).toBe('closed');

    fireEvent.mouseEnter(run.parentElement!);
    expect(instantiateTooltip.getAttribute('data-state')).toBe('closed');
    expect(runTooltip.getAttribute('data-state')).toBe('open');
    fireEvent.click(run);
    expect(idle.props.onRun).not.toHaveBeenCalled();
    fireEvent.mouseLeave(run.parentElement!);
    expect(runTooltip.getAttribute('data-state')).toBe('closed');
    expect(graphToolbarCss).toContain('visibility: hidden');
    expect(graphToolbarCss).toContain('transition: none');

    rerender(controls({ phase: 'ready', canUpload: true, canRun: true }).element);
    const [readyInstantiate, readyUpload, readyRun] = screen.getAllByRole('tooltip');
    expect(readyInstantiate.textContent).toBe(
      'Create a runnable instance of the current graph. Instantiating again starts from a fresh instance.',
    );
    expect(readyUpload.textContent).toBe(
      'Upload a JSON payload as the mock input.body of the instance (optional). Only you see the form. This graph does not read input.body, so uploading is optional.',
    );
    expect(readyRun.textContent).toBe('Run the instantiated graph, with the uploaded mock input if any.');

    rerender(controls({
      phase: 'ready', canUpload: true, canRun: true, inputBodyPaths: ['input.body.person_id', 'input.body.name'],
    }).element);
    expect(screen.getAllByRole('tooltip')[1].textContent).toBe(
      'Upload a JSON payload as the mock input.body of the instance (optional). Only you see the form. This graph reads 2 input.body paths.',
    );
  });
});
