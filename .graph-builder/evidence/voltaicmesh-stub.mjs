#!/usr/bin/env node
/*
 * Dependency-free HTTP stub for building/testing the VoltaicMesh Dispatch Evaluator.
 * Mocks the SCADA telemetry API and the market registry API.
 *
 * Run: node voltaicmesh-stub.mjs   (override port with STUB_PORT, default 8099)
 * Reachable from the engine at http://localhost:8099.
 *
 * Scenario hooks via substation id substring (so /graph-test can drive cases):
 *   default / *west*     -> NORMAL grid, ample headroom, candidates cover demand   (FULLY_ALLOCATED)
 *   *distressed*         -> frequency 59.5 Hz (DISTRESSED mode)
 *   *stale*              -> telemetry_timestamp far in the past (fail-closed denial)
 *   *highload*           -> tiny headroom (safety clamp)
 *   *empty*              -> registry returns []                                    (NO_CANDIDATES)
 *   *shortage*           -> candidates total < demand                              (SHORTAGE)
 *   *reserve*            -> includes a CRITICAL_RESERVE asset
 *   *both*               -> tiny headroom AND candidates total < demand            (SAFETY + BOTH)
 * Failure routes:
 *   /fail/*              -> 500 (exercise fetcher exception handling)
 */
import { createServer } from 'node:http';

const PORT = Number(process.env.STUB_PORT || 8099);
const nowSec = () => Math.floor(Date.now() / 1000);

const json = (res, code, body) => {
  res.writeHead(code, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
};

function telemetryFor(id) {
  const t = { grid_frequency_hz: 60.0, current_load_mw: 40.0, operating_max_capacity_mw: 100.0, telemetry_timestamp: nowSec() };
  if (id.includes('distressed')) t.grid_frequency_hz = 59.5;
  if (id.includes('highload') || id.includes('both') || id.includes('dualbind')) { t.current_load_mw = 95.0; t.operating_max_capacity_mw = 100.0; } // headroom 5
  if (id.includes('stale')) t.telemetry_timestamp = nowSec() - 9999;
  return t;
}

function candidatesFor(id) {
  if (id.includes('empty')) return [];
  // base set: total 13.0 MW, all sustain >= 30, price-ascending bat_a < bat_b
  const base = [
    { storage_node_id: 'bat_a', dischargeable_mw: 8.0, sustain_window_minutes: 60, sla_tier: 'STANDARD', price: 142.5, ramp_rate_mw_per_s: 2.0 },
    { storage_node_id: 'bat_b', dischargeable_mw: 5.0, sustain_window_minutes: 45, sla_tier: 'STANDARD', price: 155.0, ramp_rate_mw_per_s: 1.5 },
  ];
  if (id.includes('shortage') || id.includes('both')) {
    return [{ storage_node_id: 'bat_small', dischargeable_mw: 3.0, sustain_window_minutes: 60, sla_tier: 'STANDARD', price: 130.0, ramp_rate_mw_per_s: 1.0 }];
  }
  if (id.includes('reserve')) {
    return [
      ...base,
      { storage_node_id: 'bat_reserve', dischargeable_mw: 20.0, sustain_window_minutes: 120, sla_tier: 'CRITICAL_RESERVE', price: 90.0, ramp_rate_mw_per_s: 9.0 },
    ];
  }
  // genuine both-bind: supply (8) exceeds headroom (5) so headroom IS the operative cap, AND supply (8) < demand (12.5)
  if (id.includes('dualbind')) {
    return [{ storage_node_id: 'bat_mid', dischargeable_mw: 8.0, sustain_window_minutes: 60, sla_tier: 'STANDARD', price: 140.0, ramp_rate_mw_per_s: 3.0 }];
  }
  return base;
}

const server = createServer((req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);
  const { pathname } = url;
  console.error(`${new Date().toISOString()} ${req.method} ${pathname}${url.search}`);

  if (pathname.startsWith('/fail/')) return json(res, 500, { error: 'upstream_unavailable' });

  // SCADA telemetry: /scada/substations/{id}/telemetry
  const sc = pathname.match(/^\/scada\/substations\/([^/]+)\/telemetry$/);
  if (sc) {
    const id = decodeURIComponent(sc[1]);
    if (id.includes('scadafail')) return json(res, 500, { error: 'scada_unavailable' });
    return json(res, 200, telemetryFor(id));
  }

  // Market registry: /market/candidates?substation={id}&type=storage
  if (pathname === '/market/candidates') {
    const id = url.searchParams.get('substation') || '';
    if (id.includes('registryfail')) return json(res, 500, { error: 'registry_unavailable' });
    if (id.includes('nullarray')) return json(res, 200, { meta: 'ok' }); // 200 OK but NO candidates key (malformed)
    return json(res, 200, { candidates: candidatesFor(id) });
  }

  return json(res, 404, { error: 'not_found', path: pathname });
});

server.listen(PORT, () => console.error(`voltaicmesh stub listening on http://localhost:${PORT}`));
