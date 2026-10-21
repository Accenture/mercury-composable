// @vitest-environment happy-dom

import { cleanup, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import CommandInput from './CommandInput';

afterEach(cleanup);

function renderInput(disabled: boolean) {
  const { container } = render(
    <CommandInput
      command=""
      onChange={vi.fn()}
      onKeyDown={vi.fn()}
      onSend={vi.fn()}
      sendDisabled
      disabled={disabled}
      history={[]}
    />,
  );
  // The hint is the component's last paragraph (the command palette's caption comes first).
  const paragraphs = container.querySelectorAll('p');
  return {
    placeholder: container.querySelector('textarea')!.getAttribute('placeholder'),
    hint: paragraphs[paragraphs.length - 1]?.textContent,
  };
}

describe('CommandInput hint', () => {
  it('states the keys once, as symbols below the box, and keeps the box to "Enter command"', () => {
    expect(renderInput(false)).toEqual({
      placeholder: 'Enter command',
      hint: '⏎ to send · ⇧⏎ for new line · ↑↓ for history',
    });
  });

  it('asks for a connection while disconnected', () => {
    expect(renderInput(true)).toEqual({
      placeholder: 'Not connected',
      hint: 'Enter your test message once it is connected',
    });
  });
});
