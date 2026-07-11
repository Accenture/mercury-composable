// create_topic.js - administratively create the demo topics (10 partitions each) on BOTH clusters.
// Run once after starting kafka-standalone (dual.servers=true):  node create_topic.js
//
// Run this BEFORE the apps - the flow adapter auto-creates missing topics at only 1 partition.

import kafkajs from 'kafkajs';
import cfg from './config.js';

const { Kafka } = kafkajs;

async function createTopics(label, brokers, topics) {
  const kafka = new Kafka({ clientId: 'twin-demo-admin', brokers });
  const admin = kafka.admin();
  await admin.connect();
  try {
    const existing = await admin.listTopics();
    const toCreate = topics
      .filter((t) => !existing.includes(t))
      .map((topic) => ({ topic, numPartitions: cfg.partitions, replicationFactor: 1 }));

    if (toCreate.length === 0) {
      console.log(`[${cfg.ts()}] ${label}: topics already exist: ${topics.join(', ')}`);
    } else {
      await admin.createTopics({ topics: toCreate, waitForLeaders: true });
      console.log(
        `[${cfg.ts()}] ${label}: created (${cfg.partitions} partitions each): ${toCreate.map((t) => t.topic).join(', ')}`
      );
    }
  } finally {
    await admin.disconnect();
  }
}

(async () => {
  await createTopics('on-prem', cfg.onPremBrokers, cfg.onPremTopics);
  await createTopics('cloud', cfg.cloudBrokers, cfg.cloudTopics);
})().catch((e) => {
  console.error(`[${cfg.ts()}] error:`, e.message);
  process.exit(1);
});
