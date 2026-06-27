// publish-inbound.js (program-2) - read lines from the console and publish each to demo.inbound.
// Run in its own terminal:  node publish-inbound.js   (then type a message and press Enter)

import readline from 'node:readline';
import { randomUUID, randomBytes } from 'node:crypto';
import kafkajs from 'kafkajs';
import cfg from './config.js';

const { Kafka, Partitioners } = kafkajs;

(async () => {
  const kafka = new Kafka({ clientId: 'kafka-demo-publisher', brokers: cfg.brokers });
  // DefaultPartitioner avoids kafkajs' legacy-partitioner warning and spreads across the 10 partitions
  const producer = kafka.producer({ createPartitioner: Partitioners.DefaultPartitioner });
  await producer.connect();
  console.log(
    `[${cfg.ts()}] connected. Type a message + Enter to publish to '${cfg.inboundTopic}' (Ctrl-C to quit).`
  );

  const rl = readline.createInterface({ input: process.stdin, output: process.stdout, prompt: '> ' });
  rl.prompt();

  rl.on('line', async (line) => {
    const text = line.trim();
    if (text.length > 0) {
      const cid = randomUUID();
      // W3C traceparent: 00-<32-hex trace-id>-<16-hex span-id>-01. The Kafka flow adapter adopts this
      // trace-id, so the whole flow (and the message it publishes to demo.outbound) shares it - making
      // the end-to-end trace continuity visible.
      const traceId = randomBytes(16).toString('hex');
      const traceparent = `00-${traceId}-${randomBytes(8).toString('hex')}-01`;
      try {
        await producer.send({
          topic: cfg.inboundTopic,
          messages: [{ value: text, headers: { cid, traceparent } }],
        });
        console.log(`[${cfg.ts()}] -> ${cfg.inboundTopic} cid=${cid} traceId=${traceId} ${text}`);
      } catch (e) {
        console.error(`[${cfg.ts()}] publish failed:`, e.message);
      }
    }
    rl.prompt();
  });

  rl.on('close', async () => {
    await producer.disconnect();
    process.exit(0);
  });
})().catch((e) => {
  console.error(`[${cfg.ts()}] error:`, e.message);
  process.exit(1);
});
