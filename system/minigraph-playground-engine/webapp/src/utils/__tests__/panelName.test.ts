// @vitest-environment happy-dom

import { afterEach, describe, expect, it } from 'vitest';
import { PANEL_NAME_META, resolvePanelTitle } from '../panelName';

const DEFAULT_TITLE = 'Minigraph Playground';

function setMeta(content: string | null): void {
  document.head.querySelectorAll(`meta[name="${PANEL_NAME_META}"]`).forEach(meta => meta.remove());
  if (content !== null) {
    const meta = document.createElement('meta');
    meta.setAttribute('name', PANEL_NAME_META);
    meta.setAttribute('content', content);
    document.head.appendChild(meta);
  }
}

describe('resolvePanelTitle', () => {
  afterEach(() => setMeta(null));

  it('shows the name the server wrote into the page', () => {
    setMeta('My Awesome Dev Dashboard');
    expect(resolvePanelTitle(DEFAULT_TITLE)).toBe('My Awesome Dev Dashboard');
  });

  it('keeps the default title when the tag is empty, as index.html ships it', () => {
    setMeta('');
    expect(resolvePanelTitle(DEFAULT_TITLE)).toBe(DEFAULT_TITLE);
  });

  it('keeps the default title for a blank name and trims a padded one', () => {
    setMeta('   ');
    expect(resolvePanelTitle(DEFAULT_TITLE)).toBe(DEFAULT_TITLE);
    setMeta('  Ops Dashboard  ');
    expect(resolvePanelTitle(DEFAULT_TITLE)).toBe('Ops Dashboard');
  });

  it('keeps the default title when the page has no tag', () => {
    expect(resolvePanelTitle(DEFAULT_TITLE)).toBe(DEFAULT_TITLE);
  });

  it('keeps the default title without a document', () => {
    expect(resolvePanelTitle(DEFAULT_TITLE, undefined)).toBe(DEFAULT_TITLE);
  });

  it('reads the decoded text of an escaped name', () => {
    // the server escapes the property for the attribute; the browser decodes it
    const doc = new DOMParser().parseFromString(
      `<html><head><meta name="${PANEL_NAME_META}" content="R&amp;D &lt;dev&gt; &quot;blue&quot;"></head></html>`,
      'text/html');
    expect(resolvePanelTitle(DEFAULT_TITLE, doc)).toBe('R&D <dev> "blue"');
  });
});
