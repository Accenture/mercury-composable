// Shared configuration for the twin-kafka-demo node helpers.
// Override the broker lists with KAFKA_BOOTSTRAP_SERVERS (on-prem) and
// CLOUD_KAFKA_BOOTSTRAP_SERVERS (cloud) - matching the Java apps' defaults.

export default {
  // the kafka-standalone helper with dual.servers=true: on-prem on 9092, cloud on 8092
  onPremBrokers: (process.env.KAFKA_BOOTSTRAP_SERVERS || '127.0.0.1:9092').split(','),
  cloudBrokers: (process.env.CLOUD_KAFKA_BOOTSTRAP_SERVERS || '127.0.0.1:8092').split(','),

  onPremTopics: ['OP_PROFILE_REQUEST', 'OP_PROFILE_RESPONSE'],   // JSON Schema wire format
  cloudTopics: ['C_PROFILE_REQUEST', 'C_PROFILE_RESPONSE'],      // plain JSON bytes

  responseTopic: 'OP_PROFILE_RESPONSE',   // what listen_response.js watches

  // header names configured in the Java apps' application.properties
  traceHeader: 'X-Trace-Id',
  correlationHeader: 'X-Correlation-Id',

  // Multiple partitions so the system-of-record consumer group can spread across instances.
  // (Auto-created topics get only 1 partition - run create_topic.js BEFORE the apps.)
  partitions: 10,

  // ISO-8601 timestamp for simple, sortable console logging
  ts: () => new Date().toISOString(),
};
