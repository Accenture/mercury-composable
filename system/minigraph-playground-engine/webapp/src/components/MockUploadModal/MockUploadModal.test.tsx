// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MockUploadModal } from './MockUploadModal';

afterEach(cleanup);

describe('MockUploadModal graph-run context', () => {
  it('preserves the existing manual upload action label by default', () => {
    render(
      <MockUploadModal
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
      <MockUploadModal
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

  it('keeps the upload modal open when the nested file picker is cancelled', () => {
    const onClose = vi.fn();
    const { container } = render(
      <MockUploadModal
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
    expect(screen.getByRole('dialog')).toBeTruthy();
  });

  it('still closes when the dialog itself owns the cancel event', () => {
    const onClose = vi.fn();
    render(
      <MockUploadModal
        uploadPath="/api/mock/ws-123-1"
        onSuccess={vi.fn()}
        onClose={onClose}
        onError={vi.fn()}
      />,
    );

    fireEvent(screen.getByRole('dialog'), new Event('cancel', { bubbles: true, cancelable: true }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
