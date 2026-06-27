// Shared configuration for the kafka-demo Node helpers.
// Override the broker list with KAFKA_BOOTSTRAP_SERVERS (matches the Java app's default).
'use strict';

module.exports = {
  brokers: (process.env.KAFKA_BOOTSTRAP_SERVERS || '127.0.0.1:9092').split(','),
  inboundTopic: 'demo.inbound',   // node publisher -> here -> kafka-demo Java app
  outboundTopic: 'demo.outbound', // kafka-demo Java app -> here -> node listener
  partitions: 10,
  // ISO-8601 timestamp for simple, sortable console logging
  ts: () => new Date().toISOString(),
};
