/**
 * Panel name
 *
 * The Minigraph panel's heading can be renamed per deployment. The server writes the application
 * property `playground.panel.name` into the page's `<meta name="playground-panel-name">` tag when
 * it serves the page; the tag ships empty in `index.html`, so a page served without the property,
 * or by an engine that does not fill the tag, keeps the built-in title.
 */

export const PANEL_NAME_META = 'playground-panel-name';

/**
 * The heading to show: the server's panel name when it set one, otherwise the default title.
 *
 * @param defaultTitle the playground's own title
 * @param doc          the document to read (the page's own by default)
 */
export function resolvePanelTitle(defaultTitle: string, doc: Document | undefined = globalThis.document): string {
  const name = doc?.querySelector<HTMLMetaElement>(`meta[name="${PANEL_NAME_META}"]`)?.content?.trim();
  return name ? name : defaultTitle;
}
