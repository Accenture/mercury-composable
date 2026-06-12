#!/usr/bin/env node
/*
 * Tiny dependency-free HTTP stub for dogfooding MiniGraph fetchers (P0).
 * Success routes return JSON; /fail/* returns 500 so exception handling can be
 * exercised. Run: node stub-server.mjs   (override port with STUB_PORT)
 * Reachable from the engine on the same host at http://localhost:8099.
 */
import { createServer } from 'node:http';

const PORT = Number(process.env.STUB_PORT || 8099);

const json = (res, code, body) => {
  res.writeHead(code, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
};

const server = createServer((req, res) => {
  const { pathname } = new URL(req.url, `http://localhost:${PORT}`);
  console.error(`${new Date().toISOString()} ${req.method} ${pathname}`);

  // failure route first — exercises fetcher exception handling
  if (pathname.startsWith('/fail/')) return json(res, 500, { error: 'upstream_unavailable' });

  const m = pathname.match(/^\/(profile|accounts|preferences|risk)\/([^/]+)$/);
  if (m) {
    const [, kind, id] = m;
    if (kind === 'profile')     return json(res, 200, { id, name: 'Ada Example', tier: 'gold' });
    if (kind === 'accounts')    return json(res, 200, { accounts: [{ acct: 'A-1', balance: 100 }, { acct: 'A-2', balance: 50 }] });
    if (kind === 'preferences') return json(res, 200, { preferences: { channel: 'email', locale: 'en-US' } });
    if (kind === 'risk')        return json(res, 200, { risk: { score: 'low', value: 12 } });
  }
  return json(res, 404, { error: 'not_found', path: pathname });
});

server.listen(PORT, () => console.error(`stub listening on http://localhost:${PORT}`));
