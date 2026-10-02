// @vitest-environment happy-dom

import { describe, expect, it } from 'vitest';

// What setupWebStorage.ts guarantees: happy-dom tests see happy-dom's storage under both names
// on every Node version. On Node 25+ without it, `localStorage` is undefined here and
// `sessionStorage` is Node's own store, so every case below fails.
describe('web storage in the happy-dom environment', () => {
  it.each(['localStorage', 'sessionStorage'] as const)('%s is a working happy-dom Storage', (name) => {
    const storage = globalThis[name];
    expect(storage).toBeInstanceOf(Storage);
    storage.setItem('probe', '1');
    expect(storage.getItem('probe')).toBe('1');
    storage.clear();
    expect(storage).toHaveLength(0);
  });

  it('keeps localStorage and sessionStorage apart', () => {
    localStorage.setItem('probe', 'local');
    expect(sessionStorage.getItem('probe')).toBeNull();
    localStorage.clear();
  });
});
