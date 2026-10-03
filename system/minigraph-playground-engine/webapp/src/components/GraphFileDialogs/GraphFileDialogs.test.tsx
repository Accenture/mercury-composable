// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { GraphDownloadDialog } from './GraphDownloadDialog';
import { GraphImportConfirmDialog } from './GraphImportConfirmDialog';

afterEach(cleanup);

describe('GraphDownloadDialog', () => {
  it('pre-fills the graph id and confirms the trimmed value on submit', () => {
    const onConfirm = vi.fn();
    render(
      <GraphDownloadDialog defaultGraphId="tutorial-1" supportsFolderPicker={false} onConfirm={onConfirm} onCancel={vi.fn()} />,
    );

    const input = screen.getByLabelText('Graph ID') as HTMLInputElement;
    expect(input.value).toBe('tutorial-1');
    fireEvent.change(input, { target: { value: ' my-graph ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Download' }));

    expect(onConfirm).toHaveBeenCalledWith('my-graph');
  });

  it('refuses an id outside the engine rule and says so', () => {
    const onConfirm = vi.fn();
    render(
      <GraphDownloadDialog defaultGraphId="demo" supportsFolderPicker={false} onConfirm={onConfirm} onCancel={vi.fn()} />,
    );

    fireEvent.change(screen.getByLabelText('Graph ID'), { target: { value: 'my graph' } });

    expect((screen.getByRole('button', { name: 'Download' }) as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByRole('alert').textContent).toContain('letters, digits, hyphen or underscore');
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('says where the file goes and that the root node takes the graph id', () => {
    const { unmount } = render(
      <GraphDownloadDialog defaultGraphId="demo" supportsFolderPicker onConfirm={vi.fn()} onCancel={vi.fn()} />,
    );
    expect(screen.getByText(/Saved as demo\.json/).textContent).toContain('The next dialog picks the folder.');
    expect(screen.getByText(/Saved as demo\.json/).textContent).toContain('root node');
    unmount();

    render(
      <GraphDownloadDialog defaultGraphId="demo" supportsFolderPicker={false} onConfirm={vi.fn()} onCancel={vi.fn()} />,
    );
    expect(screen.getByText(/Saved as demo\.json/).textContent).toContain('download folder');
  });

  it('cancels', () => {
    const onCancel = vi.fn();
    render(<GraphDownloadDialog defaultGraphId="demo" supportsFolderPicker={false} onConfirm={vi.fn()} onCancel={onCancel} />);

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});

describe('GraphImportConfirmDialog', () => {
  const pending = {
    ok: true as const,
    fileName: 'demo.json',
    model: {},
    name: 'demo',
    nodeCount: 2,
    connectionCount: 1,
  };

  it('summarizes the file and the consequence, then replaces or cancels', () => {
    const onReplace = vi.fn();
    const onCancel = vi.fn();
    render(<GraphImportConfirmDialog pending={pending} onReplace={onReplace} onCancel={onCancel} />);

    expect(screen.getByRole('heading').textContent).toBe('Replace the current graph?');
    expect(screen.getByText(/replaces the draft/).textContent)
      .toContain('"demo.json" (2 nodes, 1 connection, named "demo")');
    fireEvent.click(screen.getByRole('button', { name: 'Replace' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onReplace).toHaveBeenCalledTimes(1);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
