// Shared configuration for the sync-over-async-demo topic admin helper.
// Override the broker list with KAFKA_BOOTSTRAP_SERVERS (matches the Java app's default).

export default {
  brokers: (process.env.KAFKA_BOOTSTRAP_SERVERS || '127.0.0.1:9092').split(','),
  requestTopic: 'soa.request',    // facade -> here -> backend (system-of-record)
  responseTopic: 'soa.response',  // backend -> here -> facade (soa-reply)
  // Multiple partitions so the facade consumer group (soa-reply-group) can spread across more than one
  // facade instance - that is what lets you run two facades and see the Redis return route deliver each
  // reply to the pod that originated the request. (Auto-created topics get only 1 partition.)
  partitions: 10,
  // ISO-8601 timestamp for simple, sortable console logging
  ts: () => new Date().toISOString(),
};
