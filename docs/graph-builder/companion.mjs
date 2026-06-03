#!/usr/bin/env node
/*
 * companion.mjs — drive the MiniGraph Playground Companion API from any
 * environment that has Node.js installed (>= 20). No npm dependencies.
 *
 * It replaces shell-specific recipes (curl / PowerShell Invoke-RestMethod) with
 * one portable tool that handles the things that bite agents:
 *   - sends the command as text/plain (the only body type the endpoint accepts)
 *   - normalizes CRLF/CR to LF so the parser never sees a stray '\r'
 *   - rejects a literal "..." line (a docs placeholder that triggers
 *     "ERROR: Missing composite path" if sent verbatim)
 *   - exits non-zero on HTTP errors so a calling agent can detect failure
 *
 * MANDATORY VERIFICATION (cannot be skipped): after every mutating command
 * (create / update / delete / connect / import / clear / reset), `send`
 * automatically re-fetches the live graph and asserts the change actually took
 * effect. Because the POST only confirms DISPATCH, this is the ONLY way to know
 * the command succeeded. If the assertion fails, `send` exits non-zero with
 * "VERIFICATION FAILED" — there is no flag to bypass it. Author one command at
 * a time and let each verify before sending the next.
 *
 * RUNTIME VERIFICATION (the other half): `instantiate` and `run` are not CRUD,
 * so the structural check above does not apply — yet they fail silently too
 * (e.g. a rejected seed line drops the whole instance, after which every inspect
 * returns 404). So `send` also:
 *   - after `instantiate`: confirms a live instance exists and prints the seeded
 *     input.body; HARD-FAILS if the instance was silently dropped.
 *   - after `run`: reads and prints output.body so the result is actually seen,
 *     and warns loudly if it is absent (does not hard-fail — a graph may write
 *     elsewhere; pass --expect <key> to assert a specific output key and turn the
 *     warning into a hard failure). This forces the inspect-after-run read instead
 *     of trusting the "accepted" response.
 *
 * The Companion endpoint only confirms DISPATCH ({"status":"accepted"}); the
 * command's textual output streams to the WebSocket console, not to this HTTP
 * response. Verify results with the GET subcommands instead:
 *   - `graph`   -> structural state (nodes + connections) after authoring
 *   - `inspect` -> one state-machine variable after `instantiate graph` + `run`
 *
 * Usage:
 *   node companion.mjs send <session-id> --file <path>
 *   node companion.mjs send <session-id> --command "describe graph"
 *   echo "list nodes" | node companion.mjs send <session-id>
 *   node companion.mjs graph <session-id>
 *   node companion.mjs inspect <session-id> <key>          # e.g. output.body
 *
 * Options:
 *   --host <url>   Engine base URL (default: $MINIGRAPH_HOST or http://localhost:8085)
 *   -f, --file     Read the command body from a file
 *   -c, --command  Inline command body
 *   --expect <key> After a `run`, assert this state-machine key exists (repeatable).
 *                  HARD-FAILS if any is missing. Use for graphs that return via a
 *                  non-default key, e.g. --expect output.header.status. Without it,
 *                  `run` only warns when output.body is empty.
 *
 * Examples:
 *   node companion.mjs send ws-734563-3 -c "describe graph"
 *   node companion.mjs send ws-734563-3 -f ./commands/create-root.txt
 *   node companion.mjs graph ws-734563-3
 *   node companion.mjs inspect ws-734563-3 output.body
 *   MINIGRAPH_HOST=http://localhost:8090 node companion.mjs graph ws-100-1
 */

import { readFileSync } from 'node:fs';
import { parseArgs } from 'node:util';

const DEFAULT_HOST = process.env.MINIGRAPH_HOST || 'http://localhost:8085';
const REQUEST_TIMEOUT_MS = 30_000; // matches the endpoint timeout in rest.yaml

const USAGE = `Usage:
  node companion.mjs send <session-id> [--file <path> | --command <text>] [--expect <key>]... [--host <url>]
  node companion.mjs graph <session-id> [--host <url>]
  node companion.mjs inspect <session-id> <key> [--host <url>]

If neither --file nor --command is given to \`send\`, the command is read from stdin.
--expect <key> (repeatable) asserts an output key exists after a \`run\` and hard-fails if missing.
Default host: $MINIGRAPH_HOST or http://localhost:8085`;

/** Print an error and exit. Default code 2 = usage/validation, 1 = runtime/HTTP. */
const fail = (message, code = 2) => {
  console.error(`error: ${message}`);
  process.exit(code);
};

/** LF-only line endings — the MiniGraph parser attaches a stray '\r' to node names. */
const toLF = (text) => text.replaceAll('\r\n', '\n').replaceAll('\r', '\n');

/** Pretty-print JSON when possible; otherwise echo the raw text. */
const print = (text) => {
  try {
    console.log(JSON.stringify(JSON.parse(text), null, 2));
  } catch {
    console.log(text);
  }
};

const validate = (command) => {
  const trimmed = command.trim();
  if (!trimmed) {
    fail('command body is empty (POST /api/companion requires a non-empty text/plain body)');
  }
  // "..." is a documentation placeholder, not a terminator — sending it verbatim
  // produces "ERROR: Missing composite path".
  if (trimmed.split('\n').some((line) => line.trim() === '...')) {
    fail('command contains a literal "..." line — remove it (it is a docs placeholder, not a terminator)');
  }
  return trimmed;
};

const readCommand = ({ command, file }) => {
  if (command !== undefined) return command;
  if (file !== undefined) return readFileSync(file, 'utf8');
  if (process.stdin.isTTY) {
    fail('no command given — pass --command, --file, or pipe one via stdin');
  }
  return readFileSync(process.stdin.fd, 'utf8');
};

const api = async (method, path, { host, body } = {}) => {
  const url = new URL(path, host.endsWith('/') ? host : `${host}/`);
  try {
    const res = await fetch(url, {
      method,
      headers: body === undefined ? undefined : { 'Content-Type': 'text/plain; charset=utf-8' },
      body,
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
    return { ok: res.ok, status: res.status, text: await res.text() };
  } catch (err) {
    throw new Error(`${method} ${url} failed: ${err.message} (is the engine reachable at ${host}?)`);
  }
};

/** Print a GET response and exit non-zero if the request failed. */
const emit = (res) => {
  print(res.text);
  if (!res.ok) process.exit(1);
};

/**
 * Inspect the first line of a command and return the structural assertion that
 * proves it landed — or null for non-mutating commands (describe / list / run /
 * instantiate / inspect / seen / export / help), which produce no structural
 * change and stream their output to the WebSocket instead.
 */
const planVerification = (command) => {
  const firstLine = command.split('\n').find((line) => line.trim().length > 0)?.trim() ?? '';
  const t = firstLine.split(/\s+/);
  const verb = t[0]?.toLowerCase();
  const noun = t[1]?.toLowerCase();
  switch (verb) {
    case 'create':
    case 'update':
      if (noun === 'node') return { kind: 'node-present', node: t[2], desc: `node "${t[2]}" present` };
      return null;
    case 'import':
      if (noun === 'node') return { kind: 'node-present', node: t[2], desc: `node "${t[2]}" imported` };
      return { kind: 'state', desc: 'graph reachable after import' };
    case 'delete':
      if (noun === 'node') return { kind: 'node-absent', node: t[2], desc: `node "${t[2]}" removed` };
      if (noun === 'connection') return { kind: 'edge-absent', from: t[2], to: t[4], desc: `connection "${t[2]}"–"${t[4]}" removed` };
      return { kind: 'state', desc: 'graph reachable after delete' };
    case 'connect': // connect <A> to <B> with <relation>
      return { kind: 'edge-present', from: t[1], to: t[3], desc: `connection "${t[1]}" → "${t[3]}" present` };
    case 'clear': // clear cache
    case 'reset':
      return { kind: 'state', desc: 'graph reachable after state change' };
    default:
      return null;
  }
};

/** Assert the planned mutation against a freshly-fetched live graph model. */
const checkVerification = (plan, graph) => {
  const nodes = new Set((graph.nodes ?? []).map((n) => n.alias));
  const edges = (graph.connections ?? []).map((c) => `${c.source} ${c.target}`);
  const hasDirected = (a, b) => edges.includes(`${a} ${b}`);
  const hasEither = (a, b) => hasDirected(a, b) || hasDirected(b, a);
  switch (plan.kind) {
    case 'node-present': return nodes.has(plan.node);
    case 'node-absent': return !nodes.has(plan.node);
    case 'edge-present': return hasDirected(plan.from, plan.to);
    case 'edge-absent': return !hasEither(plan.from, plan.to);
    case 'state': return true; // a parseable graph model is the best we can assert
    default: return true;
  }
};

/**
 * MANDATORY post-mutation verification. Re-fetches the live graph and exits
 * non-zero if the dispatched command did not actually take effect.
 */
const verifyMutation = async (plan, { host, sessionId }) => {
  const res = await api('GET', `api/graph/session/${sessionId}`, { host });
  if (!res.ok) {
    fail(`mutation dispatched but verification GET failed (HTTP ${res.status}) — state UNVERIFIED`, 1);
  }
  let graph;
  try {
    graph = JSON.parse(res.text);
  } catch {
    fail('verification GET did not return JSON — state UNVERIFIED', 1);
  }
  const summary = `${(graph.nodes ?? []).length} node(s), ${(graph.connections ?? []).length} connection(s)`;
  if (!checkVerification(plan, graph)) {
    fail(
      `VERIFICATION FAILED — expected ${plan.desc}, but the live graph shows ${summary}. ` +
        'The command was accepted but did NOT take effect. Fix it before sending the next command.',
      1,
    );
  }
  console.error(`✓ verified: ${plan.desc}  (live graph: ${summary})`);
};

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/** GET a state-machine variable, retrying briefly on 404 to absorb async-dispatch latency. */
const inspectWithRetry = async (key, { host, sessionId }, attempts = 4) => {
  let res;
  for (let i = 0; i < attempts; i++) {
    res = await api('GET', `api/inspect/${sessionId}/${key}`, { host });
    if (res.status !== 404 || i === attempts - 1) return res;
    await sleep(150);
  }
  return res;
};

/**
 * Resolve whether a state-machine key exists, returning its value when found.
 * The inspect endpoint only serves Map/List values (a scalar leaf like
 * output.body.verdict returns 404 even though it exists), so on a 404 we fall
 * back to its parent container and test membership.
 * Returns { exists, value? } or { error } for an unexpected HTTP status.
 */
const resolveKey = async (key, { host, sessionId }) => {
  const direct = await inspectWithRetry(key, { host, sessionId });
  if (direct.ok) {
    try {
      return { exists: true, value: JSON.parse(direct.text) };
    } catch {
      return { exists: true, value: direct.text };
    }
  }
  if (direct.status !== 404) return { error: direct.status };

  const dot = key.lastIndexOf('.');
  if (dot === -1) return { exists: false };
  const parent = await inspectWithRetry(key.slice(0, dot), { host, sessionId });
  if (parent.status === 404) return { exists: false };
  if (!parent.ok) return { error: parent.status };
  try {
    const obj = JSON.parse(parent.text);
    const leaf = key.slice(dot + 1);
    if (obj && typeof obj === 'object' && Object.prototype.hasOwnProperty.call(obj, leaf)) {
      return { exists: true, value: obj[leaf] };
    }
    return { exists: false };
  } catch {
    return { error: -1 };
  }
};

/** Print a resolved value: raw for strings, pretty JSON otherwise. */
const printValue = (value) => console.log(typeof value === 'string' ? value : JSON.stringify(value, null, 2));

/**
 * MANDATORY runtime verification for `instantiate`. A rejected seed line (e.g. an
 * unknown constant such as number()) makes the engine drop the instance silently,
 * after which every inspect returns 404. Confirm the instance is actually live.
 */
const verifyInstantiate = async ({ host, sessionId }) => {
  const res = await inspectWithRetry('input.body', { host, sessionId });
  if (res.status === 404) {
    fail(
      'VERIFICATION FAILED — instantiate left NO live instance (input.body is 404). A seed line was rejected ' +
        '(often an unknown constant — there is no number(); use double()/float()), so the engine dropped the instance. ' +
        'Fix the instantiate command before running.',
      1,
    );
  }
  if (!res.ok) fail(`instantiate verification failed (HTTP ${res.status}) — state UNVERIFIED`, 1);
  console.error('✓ instance live — seeded input.body:');
  print(res.text);
};

/**
 * MANDATORY runtime read for `run`. Forces the agent to actually see the result
 * rather than trusting the "accepted" response.
 *
 * With no `--expect`: reads output.body and warns loudly (no hard-fail) if absent,
 * since a graph may legitimately write elsewhere.
 * With `--expect <key>` (repeatable): treats each key as an explicit assertion and
 * HARD-FAILS if any is missing — when the caller declares the output, its absence
 * is unambiguous failure.
 */
const verifyRun = async ({ host, sessionId, expect = [] }) => {
  if (expect.length > 0) {
    const missing = [];
    for (const key of expect) {
      const r = await resolveKey(key, { host, sessionId });
      if (r.error) {
        fail(`run verification of ${key} failed (HTTP ${r.error}) — state UNVERIFIED`, 1);
      } else if (r.exists) {
        console.error(`✓ run produced ${key}:`);
        printValue(r.value);
      } else {
        missing.push(key);
      }
    }
    if (missing.length > 0) {
      fail(
        `VERIFICATION FAILED — run did not produce expected output key(s): ${missing.join(', ')}. ` +
          `Inspect node results (node companion.mjs inspect ${sessionId} <node>.result) and check \`seen\` on the ` +
          'WebSocket console to find where traversal stopped.',
        1,
      );
    }
    return;
  }

  const res = await inspectWithRetry('output.body', { host, sessionId });
  if (res.ok) {
    console.error('✓ run completed — output.body:');
    print(res.text);
    return;
  }
  if (res.status === 404) {
    console.error(
      `⚠ run produced NO output.body. If this graph is meant to return a value, the run did not complete as expected —\n` +
        `  inspect node results (node companion.mjs inspect ${sessionId} <node>.result) and check \`seen\` on the\n` +
        `  WebSocket console to find where traversal stopped. If the graph writes a different output key, re-run with\n` +
        `  --expect <key> (e.g. --expect output.header.status) to assert it.`,
    );
    return;
  }
  fail(`run verification failed (HTTP ${res.status}) — state UNVERIFIED`, 1);
};

// --- parse arguments -------------------------------------------------------

let parsed;
try {
  parsed = parseArgs({
    allowPositionals: true,
    options: {
      host: { type: 'string' },
      file: { type: 'string', short: 'f' },
      command: { type: 'string', short: 'c' },
      expect: { type: 'string', multiple: true },
      help: { type: 'boolean', short: 'h' },
    },
  });
} catch (err) {
  fail(err.message);
}

const { values, positionals } = parsed;
const [subcommand, sessionId, key] = positionals;
const host = values.host ?? DEFAULT_HOST;

if (values.help || !subcommand) {
  console.error(USAGE);
  process.exit(values.help ? 0 : 2);
}
if (!sessionId) fail(`missing <session-id> for "${subcommand}"`);

// --- run the subcommand ----------------------------------------------------

try {
  switch (subcommand) {
    case 'send': {
      const command = validate(toLF(readCommand(values)));
      const res = await api('POST', `api/companion/${sessionId}`, { host, body: command });
      print(res.text);
      if (!res.ok) fail(`HTTP ${res.status} — command was NOT accepted.`, 1);

      const plan = planVerification(command);
      const verb = command.trim().split(/\s+/)[0]?.toLowerCase();
      if (plan) {
        // Mutating (CRUD) command: structural verification is mandatory.
        await verifyMutation(plan, { host, sessionId });
      } else if (verb === 'instantiate') {
        // Runtime: confirm the instance materialized (hard-fail if silently dropped).
        await verifyInstantiate({ host, sessionId });
      } else if (verb === 'run') {
        // Runtime: force the inspect-after-run read and surface output (or asserted keys).
        await verifyRun({ host, sessionId, expect: values.expect ?? [] });
      } else {
        // Read-only command (describe / list / inspect / seen / export): WebSocket output.
        console.error(
          `\nDispatched (status "accepted"). Output streams to the WebSocket console — verify results with:\n` +
            `  node companion.mjs graph ${sessionId}\n` +
            `  node companion.mjs inspect ${sessionId} <key>`,
        );
      }
      break;
    }

    case 'graph':
      emit(await api('GET', `api/graph/session/${sessionId}`, { host }));
      break;

    case 'inspect':
      if (!key) fail('missing <key> for "inspect" (e.g. output.body)');
      emit(await api('GET', `api/inspect/${sessionId}/${key}`, { host }));
      break;

    default:
      fail(`unknown subcommand: ${subcommand} (expected send, graph, or inspect)`);
  }
} catch (err) {
  fail(err.message, 1);
}
