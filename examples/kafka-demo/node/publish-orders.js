// publish-orders.js (program-3) - drive the SECOND-LEVEL ROUTING demo: publish mixed event types to
// demo.orders and watch the adapter's rule list pick a different target per message.
// Run in its own terminal:  node publish-orders.js   (then type a command and press Enter)
//
// Console commands (first word decides the record shape; each exercises one routing rule):
//
//   order [json]       'type: order' header (+ JSON body)   -> exact rule    -> flow://demo-order-flow
//   order-<x> [json]   'type: order-<x>' header (+ body)    -> wildcard rule -> flow://demo-order-flow
//   refund [json]      NO type header, {"event":{"kind":"refund"},...} body
//                                                           -> body rule     -> task://demo.refund.processor
//   <anything else>    raw text (not JSON, no type header)  -> default       -> flow://demo-catch-all-flow
//
// The optional [json] overrides the canned payload, e.g.:  order {"item":"laptop","qty":2}
// Rule evaluation is first-match-wins in declaration order; a non-match falls through to 'default'.

import readline from 'node:readline';
import { randomUUID, randomBytes } from 'node:crypto';
import kafkajs from 'kafkajs';
import cfg from './config.js';

const { Kafka, Partitioners } = kafkajs;

// Build {headers, value, rule} for one console line - the record shape decides which rule fires.
function toRecord(line) {
  const space = line.indexOf(' ');
  const keyword = space === -1 ? line : line.substring(0, space);
  const rest = space === -1 ? '' : line.substring(space + 1).trim();
  const customJson = rest.startsWith('{') || rest.startsWith('[') ? rest : null;
  if (keyword === 'order' || keyword.startsWith('order-')) {
    const rule = keyword === 'order'
      ? 'input.header.type(order) [exact]' : 'input.header.type(order-*) [wildcard]';
    return {
      headers: { type: keyword },
      value: customJson || JSON.stringify({ item: 'keyboard', qty: 1 }),
      rule: `${rule} -> flow://demo-order-flow`,
    };
  }
  if (keyword === 'refund') {
    // no 'type' header: the header rules cannot match, so the input.body rule decides
    return {
      headers: {},
      value: customJson
        || JSON.stringify({ event: { kind: 'refund' }, orderId: randomUUID().substring(0, 8) }),
      rule: 'input.body.event.kind(refund) [body path] -> task://demo.refund.processor (see the Java log)',
    };
  }
  // raw text: not a JSON object/array, so serializer 'json' keeps the byte[] and no rule matches
  return {
    headers: {},
    value: line,
    rule: 'default -> flow://demo-catch-all-flow (raw bytes pass through)',
  };
}

(async () => {
  const kafka = new Kafka({ clientId: 'kafka-demo-orders-publisher', brokers: cfg.brokers });
  // DefaultPartitioner avoids kafkajs' legacy-partitioner warning and spreads across the 10 partitions
  const producer = kafka.producer({ createPartitioner: Partitioners.DefaultPartitioner });
  await producer.connect();
  console.log(`[${cfg.ts()}] connected. Publish to '${cfg.ordersTopic}' (Ctrl-C to quit). Commands:`);
  console.log('  order [json]      -> exact rule    -> demo-order-flow');
  console.log('  order-<x> [json]  -> wildcard rule -> demo-order-flow');
  console.log('  refund [json]     -> body rule     -> task demo.refund.processor');
  console.log('  <anything else>   -> default       -> demo-catch-all-flow');

  async function publishOne(text) {
    const { headers, value, rule } = toRecord(text);
    const cid = randomUUID();
    // W3C traceparent: the adapter adopts this trace-id, so the selected flow/task (and any message
    // it publishes to demo.outbound) shares it - end-to-end trace continuity per routed message.
    const traceId = randomBytes(16).toString('hex');
    const traceparent = `00-${traceId}-${randomBytes(8).toString('hex')}-01`;
    try {
      await producer.send({
        topic: cfg.ordersTopic,
        messages: [{ value, headers: { ...headers, cid, traceparent } }],
      });
      console.log(`[${cfg.ts()}] -> ${cfg.ordersTopic} cid=${cid} traceId=${traceId}`);
      console.log(`[${cfg.ts()}]    expected: ${rule}`);
    } catch (e) {
      console.error(`[${cfg.ts()}] publish failed:`, e.message);
    }
  }

  const rl = readline.createInterface({ input: process.stdin, output: process.stdout, prompt: '> ' });
  rl.prompt();

  // chain the sends so they stay ordered and can be awaited on close - the script then works both
  // interactively AND with piped input for scripted regression runs, e.g.
  //   printf 'order\nrefund\nhello\n' | node publish-orders.js
  let inflight = Promise.resolve();

  rl.on('line', (line) => {
    const text = line.trim();
    if (text.length > 0) {
      inflight = inflight.then(() => publishOne(text));
    }
    rl.prompt();
  });

  rl.on('close', async () => {
    await inflight;
    await producer.disconnect();
    process.exit(0);
  });
})().catch((e) => {
  console.error(`[${cfg.ts()}] error:`, e.message);
  process.exit(1);
});
