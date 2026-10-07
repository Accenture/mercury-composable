// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import NavMenu from './NavMenu';

afterEach(cleanup);

describe('NavMenu', () => {
  it('renders plain children while open and keeps the menu open on an item click', () => {
    render(<NavMenu label="Links"><button type="button" role="menuitem">Item</button></NavMenu>);
    fireEvent.click(screen.getByRole('button', { name: 'Links' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Item' }));
    expect(screen.getByRole('menu')).toBeTruthy();
  });

  it('hands function children a close() so an item can close the menu when picked', () => {
    const picked = vi.fn();
    render(
      <NavMenu label="Tools">
        {(close) => (
          <button type="button" role="menuitem" onClick={() => { close(); picked(); }}>Package graphs…</button>
        )}
      </NavMenu>,
    );
    const trigger = screen.getByRole('button', { name: 'Tools' });
    fireEvent.click(trigger);
    fireEvent.click(screen.getByRole('menuitem', { name: 'Package graphs…' }));

    expect(picked).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('menu')).toBeNull();
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
  });
});
