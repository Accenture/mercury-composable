// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import MockUploadPanel from './MockUploadPanel';

afterEach(cleanup);

describe('MockUploadPanel graph-run context', () => {
  it('preserves the existing manual upload action label by default', () => {
    render(
      <MockUploadPanel
        uploadPath="/api/mock/ws-123-1"
        onSuccess={vi.fn()}
        onClose={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: 'Upload ▶' })).toBeTruthy();
  });

  it('can explain the derived graph inputs and next action without changing upload mechanics', () => {
    render(
      <MockUploadPanel
        uploadPath="/api/mock/ws-123-1"
        title="Add graph input"
        description="These paths are referenced by the graph."
        inputPathHints={['input.body.user.id', 'input.body.enabled']}
        submitLabel="Upload & Run"
        onSuccess={vi.fn()}
        onClose={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(screen.getByText('Add graph input')).toBeTruthy();
    expect(screen.getByText('These paths are referenced by the graph.')).toBeTruthy();
    expect(screen.getByText('input.body.user.id')).toBeTruthy();
    expect(screen.getByText('input.body.enabled')).toBeTruthy();
    expect((screen.getByRole('button', { name: 'Upload & Run' }) as HTMLButtonElement).disabled).toBe(true);
  });

  it('renders in place — a left-slot panel region, not a dialog', () => {
    render(
      <MockUploadPanel
        uploadPath="/api/mock/ws-123-1"
        onSuccess={vi.fn()}
        onClose={vi.fn()}
        onError={vi.fn()}
      />,
    );

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(screen.getByRole('region', { name: '⬆️ Upload Mock Data' })).toBeTruthy();
    expect(document.activeElement).toBe(screen.getByLabelText('JSON Payload'));
  });

  it('closes on Escape with the owning upload path, like the node editor', () => {
    const onClose = vi.fn();
    render(
      <MockUploadPanel
        uploadPath="/api/mock/ws-123-1"
        onSuccess={vi.fn()}
        onClose={onClose}
        onError={vi.fn()}
      />,
    );

    fireEvent.keyDown(document, { key: 'Escape' });

    expect(onClose).toHaveBeenCalledWith('/api/mock/ws-123-1');
  });

  it('does not close when the nested file picker dispatches a cancel event', () => {
    const onClose = vi.fn();
    const { container } = render(
      <MockUploadPanel
        uploadPath="/api/mock/ws-123-1"
        onSuccess={vi.fn()}
        onClose={onClose}
        onError={vi.fn()}
      />,
    );
    const fileInput = container.querySelector<HTMLInputElement>('input[type="file"]');
    expect(fileInput).not.toBeNull();

    fireEvent(fileInput!, new Event('cancel', { bubbles: true, cancelable: true }));

    expect(onClose).not.toHaveBeenCalled();
    expect(screen.getByRole('region', { name: '⬆️ Upload Mock Data' })).toBeTruthy();
  });
});

describe('MockUploadPanel mock headers', () => {
  it('enables the upload with headers alone and posts them to the header namespace after the body', async () => {
    const calls: Array<{ url: string; body: string }> = [];
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      calls.push({ url, body: String(init?.body ?? '') });
      return new Response('{"message":"Content uploaded"}', { status: 200 });
    });
    vi.stubGlobal('fetch', fetchMock);
    const onSuccess = vi.fn();
    try {
      render(
        <MockUploadPanel uploadPath="/api/mock/ws-123-1" onSuccess={onSuccess} onClose={vi.fn()} onError={vi.fn()} />,
      );
      const upload = screen.getByRole('button', { name: 'Upload ▶' }) as HTMLButtonElement;
      expect(upload.disabled).toBe(true);
      fireEvent.click(screen.getByRole('button', { name: '+ Add header' }));
      fireEvent.change(screen.getByLabelText('Header name'), { target: { value: 'X-Request-Id' } });
      fireEvent.change(screen.getByLabelText('Value of header X-Request-Id'), { target: { value: 'abc-123' } });
      expect(upload.disabled).toBe(false);
      fireEvent.click(upload);
      await screen.findByRole('button', { name: 'Upload ▶' });
      await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
      expect(calls).toEqual([
        { url: '/api/mock/ws-123-1?namespace=header', body: '{"X-Request-Id":"abc-123"}' },
      ]);
      // with a body too, the body goes first
      fireEvent.change(screen.getByLabelText('JSON Payload'), { target: { value: '{"hello":"world"}' } });
      fireEvent.click(screen.getByRole('button', { name: 'Upload ▶' }));
      await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(2));
      expect(calls.slice(1)).toEqual([
        { url: '/api/mock/ws-123-1', body: '{"hello":"world"}' },
        { url: '/api/mock/ws-123-1?namespace=header', body: '{"X-Request-Id":"abc-123"}' },
      ]);
    } finally {
      vi.unstubAllGlobals();
    }
  });

  it('blocks a header value without a name and a duplicate name, and shows the referenced headers', () => {
    render(
      <MockUploadPanel
        uploadPath="/api/mock/ws-123-1"
        inputHeaderHints={['x-request-id', '*']}
        onSuccess={vi.fn()}
        onClose={vi.fn()}
        onError={vi.fn()}
      />,
    );
    expect(screen.getByText('x-request-id')).toBeTruthy();
    expect(screen.getByText('input.header (all)')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '+ Add header' }));
    fireEvent.change(screen.getByLabelText('Value of header (unnamed)'), { target: { value: 'orphan' } });
    expect(screen.getByRole('alert').textContent).toContain('A header value needs a name');
    expect((screen.getByRole('button', { name: 'Upload ▶' }) as HTMLButtonElement).disabled).toBe(true);
    fireEvent.change(screen.getByLabelText('Header name'), { target: { value: 'Authorization' } });
    fireEvent.click(screen.getByRole('button', { name: '+ Add header' }));
    const names = screen.getAllByLabelText('Header name');
    fireEvent.change(names[1], { target: { value: 'authorization' } });
    expect(screen.getByRole('alert').textContent).toContain("Duplicate header name 'authorization'");
    expect((screen.getByRole('button', { name: 'Upload ▶' }) as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByRole('button', { name: 'Remove header authorization' }));
    expect(screen.queryByRole('alert')).toBeNull();
    expect((screen.getByRole('button', { name: 'Upload ▶' }) as HTMLButtonElement).disabled).toBe(false);
  });
});
