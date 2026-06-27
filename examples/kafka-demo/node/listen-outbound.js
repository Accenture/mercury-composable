// listen-outbound.js (program-1) - listen on demo.outbound and log every message the Java app publishes.
// Run in its own terminal:  node listen-outbound.js
'use strict';

const { Kafka } = require('kafkajs');
const cfg = require('./config');

(async () => {
  const kafka = new Kafka({ clientId: 'kafka-demo-listener', brokers: cfg.brokers });
  const consumer = kafka.consumer({ groupId: 'kafka-demo-node-listener' });
  await consumer.connect();
  await consumer.subscribe({ topic: cfg.outboundTopic, fromBeginning: false });
  console.log(`[${cfg.ts()}] listening on '${cfg.outboundTopic}' (Ctrl-C to quit) ...`);

  await consumer.run({
    eachMessage: async ({ partition, message }) => {
      const cid = message.headers && message.headers.cid ? message.headers.cid.toString() : '(none)';
      console.log(`[${cfg.ts()}] <- ${cfg.outboundTopic}[p${partition}] cid=${cid} ${message.value.toString()}`);
    },
  });
})().catch((e) => {
  console.error(`[${cfg.ts()}] error:`, e.message);
  process.exit(1);
});
