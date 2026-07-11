// listen_response.js - watch the on-prem response topic and print each profile Response with its Kafka
// metadata (topic, partition), schema id, trace id and correlation id.
//
//   node listen_response.js
//
// The on-prem cluster uses the Schema Registry, so each value is in the Confluent wire format:
// magic byte 0x0 + 4-byte big-endian schema id + the payload. For JSON Schema the payload is the plain
// UTF-8 JSON string (the serde does not transform it), so after stripping the 5-byte frame the message
// prints directly. (This trick is JSON-Schema-specific: Avro would leave Avro binary after the frame.)

import kafkajs from 'kafkajs';
import cfg from './config.js';

const { Kafka, logLevel } = kafkajs;

const header = (message, name) => {
  const value = message.headers?.[name];
  return value ? value.toString() : '(none)';
};

(async () => {
  const kafka = new Kafka({ clientId: 'twin-demo-listener', brokers: cfg.onPremBrokers, logLevel: logLevel.NOTHING });
  const consumer = kafka.consumer({ groupId: 'twin-demo-listener-group' });
  await consumer.connect();
  await consumer.subscribe({ topic: cfg.responseTopic, fromBeginning: false });
  console.log(`[${cfg.ts()}] listening on ${cfg.responseTopic} (${cfg.onPremBrokers.join(',')}) ...`);

  await consumer.run({
    eachMessage: async ({ topic, partition, message }) => {
      const value = message.value ?? Buffer.alloc(0);
      let schemaId = '(none)';
      let json = value.toString('utf8');
      if (value.length >= 5 && value[0] === 0) {
        schemaId = value.readUInt32BE(1);
        json = value.subarray(5).toString('utf8');
      }
      console.log(`[${cfg.ts()}] topic=${topic} partition=${partition} schemaId=${schemaId}`);
      console.log(`  ${cfg.traceHeader}=${header(message, cfg.traceHeader)}`);
      console.log(`  ${cfg.correlationHeader}=${header(message, cfg.correlationHeader)}`);
      try {
        console.log(`  ${JSON.stringify(JSON.parse(json))}`);
      } catch {
        console.log(`  (not JSON) ${json}`);
      }
    },
  });
})().catch((e) => {
  console.error(`[${cfg.ts()}] error:`, e.message);
  process.exit(1);
});
