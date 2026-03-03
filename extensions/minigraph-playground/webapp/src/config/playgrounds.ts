/**
 * Central configuration for all playground tools.
 *
 * ─── HOW TO ADD A NEW PLAYGROUND ───────────────────────────────────────────
 *  1. Add an entry to PLAYGROUND_CONFIGS below.
 *  2. Add a matching <Route> in App.tsx pointing to the same `path`.
 *  That's it — the navigation bar updates automatically.
 * ───────────────────────────────────────────────────────────────────────────
 */

// ---------------------------------------------------------------------------
// Shared runtime limits (used by both the hook and the UI)
// ---------------------------------------------------------------------------

/** Maximum number of console messages kept in memory at once. */
export const MAX_ITEMS    = 200;

/** Maximum number of command history entries kept in localStorage. */
export const MAX_HISTORY  = 50;

/** Maximum payload size in characters accepted by the WebSocket send path. */
export const MAX_BUFFER = 63_488; // 62 * 1024 - some overhead for WebSocket framing, to stay safely under the 64KB limit of most browsers

/** Interval in milliseconds between keep-alive ping frames sent to the server. */
export const PING_INTERVAL = 30_000;

/** Shape of a single playground tool entry. */
export interface PlaygroundConfig {
  path:              string;  // URL route path, must start with "/"
  label:             string;  // Short name shown in the navigation bar
  title:             string;  // Full heading shown on the playground page
  wsPath:            string;  // WebSocket endpoint path served by the backend
  storageKeyPayload: string;  // localStorage key for the last payload
  storageKeyHistory: string;  // localStorage key for command history
  supportsUpload?:   boolean; // true when the backend supports POST /api/json/content/{id} upload
}

export const PLAYGROUND_CONFIGS: PlaygroundConfig[] = [
  {
    path: '/json-path',
    label: 'JSON-Path',
    title: 'JSON-Path Playground',
    wsPath: '/ws/json/path',
    storageKeyPayload: 'jsonpath-last-payload',
    storageKeyHistory: 'jsonpath-command-history',
    supportsUpload: true,
  },
  {
    path: '/minigraph',
    label: 'Minigraph',
    title: 'Minigraph Playground',
    wsPath: '/ws/graph/playground',
    storageKeyPayload: 'minigraph-last-payload',
    storageKeyHistory: 'minigraph-command-history',
  },
];

/**
 * Quick-load sample payloads shown below the payload textarea.
 *
 * Keys follow the pattern "<type>_<label>".
 *   - The type prefix ("json" or "xml") groups the buttons by row.
 *   - The label part is the button text (underscores become spaces).
 *
 * Add a new entry here to make a new button appear automatically in the UI.
 */
export const SAMPLE_DATA: Record<string, string> = {
  json_simple: JSON.stringify({ name: 'John Doe', age: 30, city: 'New York' }, null, 2),
  json_nested: JSON.stringify(
    {
      user: {
        name: 'Jane Smith',
        profile: {
          email: 'jane@example.com',
          address: { city: 'San Francisco', country: 'USA' },
        },
      },
    },
    null,
    2
  ),
  json_array: JSON.stringify(
    [
      { id: 1, name: 'Item 1', status: 'active' },
      { id: 2, name: 'Item 2', status: 'pending' },
      { id: 3, name: 'Item 3', status: 'inactive' },
    ],
    null,
    2
  ),
  xml_simple: `<?xml version="1.0" encoding="UTF-8"?>
<person>
  <name>John Doe</name>
  <age>30</age>
  <city>New York</city>
</person>`,
  xml_nested: `<?xml version="1.0" encoding="UTF-8"?>
<user>
  <name>Jane Smith</name>
  <profile>
    <email>jane@example.com</email>
    <address>
      <city>San Francisco</city>
      <country>USA</country>
    </address>
  </profile>
</user>`,
  xml_array: `<?xml version="1.0" encoding="UTF-8"?>
<items>
  <item>
    <id>1</id>
    <name>Item 1</name>
    <status>active</status>
  </item>
  <item>
    <id>2</id>
    <name>Item 2</name>
    <status>pending</status>
  </item>
  <item>
    <id>3</id>
    <name>Item 3</name>
    <status>inactive</status>
  </item>
</items>`,
};
