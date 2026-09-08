// @vitest-environment happy-dom

import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import NodeEditPanel from '../NodeEditPanel';
import type { NodeFormState } from '../../../graphActions/nodeAuthoringTypes';

function makeFormState(): NodeFormState {
  return {
    alias: 'fetcher',
    nodeType: 'Fetcher',
    properties: [
      { id: 'row-1', key: 'exception', value: 'error-handler' },
      { id: 'row-2', key: 'skill', value: 'graph.api.fetcher' },
    ],
    source: 'edit-node',
  };
}

function renderPanel(overrides: Partial<React.ComponentProps<typeof NodeEditPanel>> = {}) {
  const props = {
    formState: makeFormState(),
    phase: 'editing' as const,
    lockReason: null,
    serverMessage: null,
    validationErrors: {},
    onFormStateChange: vi.fn(),
    onSubmit: vi.fn(),
    onClose: vi.fn(),
    ...overrides,
  };
  const view = render(<NodeEditPanel {...props} />);
  return { view, props };
}

describe('NodeEditPanel', () => {
  it('renders the magnified-node ribbon with alias and type badge', () => {
    renderPanel();

    const form = screen.getByRole('form', { name: 'Edit node fetcher' });
    expect(form.style.borderColor).toBe('#2563eb'); // Fetcher accent
    expect(screen.getByText('fetcher')).toBeTruthy();
    expect(screen.getByText('Fetcher')).toBeTruthy();
    // Property rows render as editable key/value pairs.
    expect(screen.getAllByLabelText('Property key')).toHaveLength(2);
    expect(screen.getAllByLabelText('Property value')).toHaveLength(2);
  });

  it('reports key edits through onFormStateChange', () => {
    const { props } = renderPanel();

    const keyInputs = screen.getAllByLabelText('Property key');
    fireEvent.change(keyInputs[0], { target: { value: 'timeout' } });

    expect(props.onFormStateChange).toHaveBeenCalledWith(expect.objectContaining({
      properties: [
        expect.objectContaining({ id: 'row-1', key: 'timeout' }),
        expect.objectContaining({ id: 'row-2', key: 'skill' }),
      ],
    }));
  });

  it('closes on Escape unless a save is in flight', () => {
    const { props, view } = renderPanel();
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(props.onClose).toHaveBeenCalledTimes(1);
    view.unmount();

    const { props: sendingProps } = renderPanel({ phase: 'sending', lockReason: 'sending' });
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(sendingProps.onClose).not.toHaveBeenCalled();
  });

  it('locks the form and shows progress while sending', () => {
    renderPanel({ phase: 'sending', lockReason: 'sending' });

    expect(screen.getByRole('button', { name: 'Saving...' })).toBeTruthy();
    for (const input of screen.getAllByLabelText('Property key')) {
      expect((input as HTMLInputElement).disabled).toBe(true);
    }
  });
});
