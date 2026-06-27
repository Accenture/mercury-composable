// listen-outbound.js (program-1) - listen on demo.outbound and log every message the Java app publishes.
// Run in its own terminal:  node listen-outbound.js

import kafkajs from 'kafkajs';
import cfg from './config.js';

const { Kafka } = kafkajs;

(async () => {
  const kafka = new Kafka({ clientId: 'kafka-demo-listener', brokers: cfg.brokers });
  const consumer = kafka.consumer({ groupId: 'kafka-demo-node-listener' });
  await consumer.connect();
  await consumer.subscribe({ topic: cfg.outboundTopic, fromBeginning: false });
  console.log(`[${cfg.ts()}] listening on '${cfg.outboundTopic}' (Ctrl-C to quit) ...`);

  await consumer.run({
    eachMessage: async ({ partition, message }) => {
      const h = message.headers || {};
      const cid = h.cid ? h.cid.toString() : '(none)';
      // simple.kafka.notification stamps a fresh traceparent whose trace-id is the SAME one the publisher
      // sent (continuous across both Kafka hops); the span-id differs (a new hop). Show the trace-id so the
      // continuity is visible: it should match the publisher's traceId and the Java telemetry trace id.
      const traceId = h.traceparent ? h.traceparent.toString().split('-')[1] : '(none)';
      console.log(`[${cfg.ts()}] <- ${cfg.outboundTopic}[p${partition}] cid=${cid} traceId=${traceId} ${message.value.toString()}`);
    },
  });
})().catch((e) => {
  console.error(`[${cfg.ts()}] error:`, e.message);
  process.exit(1);
});
