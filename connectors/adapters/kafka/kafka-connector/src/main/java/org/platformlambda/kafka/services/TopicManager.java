/*

    Copyright 2018-2025 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.kafka.services;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.platformlambda.cloud.ConnectorConfig;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class TopicManager implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(TopicManager.class);

    private static final String TYPE = "type";
    private static final String PARTITIONS = "partitions";
    private static final String TOPIC = "topic";
    private static final String CREATE = "create";
    private static final String DELETE = "delete";
    private static final String LIST = "list";
    private static final String EXISTS = "exists";
    private static final String STOP = "stop";
    private Integer replicationFactor = -1;
    private final Properties baseProperties;
    private final boolean topicSubstitution;
    private final Map<String, String> preAllocatedTopics;
    private AdminClient admin;
    private long lastAccess = 0;
    private int processed = 0;
    private int seq = 0;

    public TopicManager(Properties baseProperties, String cloudManager) {
        this.baseProperties = baseProperties;
        this.topicSubstitution = ConnectorConfig.topicSubstitutionEnabled();
        this.preAllocatedTopics = ConnectorConfig.getTopicSubstitution();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopAdmin));
        if (!topicSubstitution) {
            InactivityMonitor monitor = new InactivityMonitor(cloudManager);
            monitor.start();
        }
    }

    private synchronized void startAdmin() {
        if (admin == null) {
            seq++;
            Properties properties = new Properties();
            properties.putAll(baseProperties);
            properties.put(AdminClientConfig.CLIENT_ID_CONFIG, "admin-"+ Platform.getInstance().getOrigin()+"-"+seq);
            admin = AdminClient.create(properties);
            log.info("AdminClient-{} ready", seq);
            lastAccess = System.currentTimeMillis();
        }
    }

    private synchronized void stopAdmin() {
        if (admin != null) {
            try {
                admin.close();
                log.info("AdminClient-{} closed, processed: {}", seq, processed);
            } catch (Exception e) {
                // ok to ignore
            }
            processed = 0;
            admin = null;
        }
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        if (headers.containsKey(TYPE)) {
            if (LIST.equals(headers.get(TYPE))) {
                return listTopics();
            }
            if (EXISTS.equals(headers.get(TYPE)) && headers.containsKey(TOPIC)) {
                String origin = headers.get(TOPIC);
                return topicExists(origin);
            }
            if (PARTITIONS.equals(headers.get(TYPE)) && headers.containsKey(TOPIC)) {
                String origin = headers.get(TOPIC);
                return topicPartitions(origin);
            }
            if (CREATE.equals(headers.get(TYPE)) && headers.containsKey(TOPIC)) {
                int partitions = headers.containsKey(PARTITIONS)?
                                    Math.max(1, Utility.getInstance().str2int(headers.get(PARTITIONS))) : 1;
                createTopic(headers.get(TOPIC), partitions);
                return true;
            }
            if (DELETE.equals(headers.get(TYPE)) && headers.containsKey(TOPIC)) {
                String origin = headers.get(TOPIC);
                if (topicExists(origin)) {
                    deleteTopic(origin);
                }
                return true;
            }
            if (STOP.equals(headers.get(TYPE))) {
                stopAdmin();
            }
        }
        return false;
    }

    private boolean topicExists(String topic) {
        if (topicSubstitution) {
            return preAllocatedTopics.get(topic) != null;
        }
        return topicPartitions(topic) != -1;
    }

    private int topicPartitions(String topic) {
        if (topicSubstitution) {
            int n = 0;
            while (preAllocatedTopics.containsKey(topic+"."+n)) {
                n++;
            }
            return n;
        }
        startAdmin();
        lastAccess = System.currentTimeMillis();
        DescribeTopicsResult topicMetadata = admin.describeTopics(Collections.singletonList(topic));
        try {
            Map<String, TopicDescription> result = topicMetadata.allTopicNames().get();
            processed++;
            if (!result.isEmpty()) {
                Collection<TopicDescription> topics = result.values();
                for (TopicDescription desc: topics) {
                    if (desc.name().equals(topic)) {
                        return desc.partitions().size();
                    }
                }
            }
        } catch (UnknownTopicOrPartitionException | InterruptedException | ExecutionException e) {
            // move on because topic does not exist
        }
        return -1;
    }

    private int getReplicationFactor() {
        if (topicSubstitution) {
            return 1;
        }
        if (replicationFactor == -1) {
            AppConfigReader reader = AppConfigReader.getInstance();
            int factor = Math.max(1, Utility.getInstance().str2int(
                    reader.getProperty("kafka.replication.factor", "3")));
            if (factor > 3) {
                factor = 3;
                log.warn("Default kafka replication factor reset to 3");
            }
            DescribeClusterResult cluster = admin.describeCluster();
            try {
                Collection<Node> nodes = cluster.nodes().get();
                log.info("Kafka cluster information");
                for (Node n : nodes) {
                    log.info("Broker-Id: {}, Host: {}", n.id(), n.host());
                }
                replicationFactor = Math.min(factor, nodes.size());
                log.info("Kafka replication factor set to {}", replicationFactor);

            } catch (InterruptedException | ExecutionException e) {
                log.error("Unable to read cluster information - {}", e.getMessage());
                replicationFactor = 1;
            }
        }
        return replicationFactor;
    }

    private void createTopic(String topic, int partitions) {
        if (topicSubstitution) {
            if (preAllocatedTopics.get(topic) == null) {
                throw new IllegalArgumentException("Missing topic substitution for "+topic);
            }
            return;
        }
        startAdmin();
        lastAccess = System.currentTimeMillis();
        try {
            int currentPartitions = topicPartitions(topic);
            if (currentPartitions == -1) {
                int replication = getReplicationFactor();
                int partitionCount = Math.max(1, partitions);
                CreateTopicsResult createTask = admin.createTopics(
                        Collections.singletonList(new NewTopic(topic, partitionCount, (short) replication)));
                createTask.all().get();
                processed++;
                // check if creation is successful
                boolean found = false;
                // try a few times due to eventual consistency
                for (int i=0; i < 10; i++) {
                    found = topicExists(topic);
                    if (found) {
                        break;
                    } else {
                        Thread.sleep(1000);
                        log.warn("Newly created {} not found. Scanning it again.", topic);
                    }
                }
                if (found) {
                    log.info("Created {} with {} partition{}, replication factor of {}",
                            topic, partitionCount, partitionCount == 1? "" : "s", replication);
                } else {
                    log.error("Unable to create {} after a few attempts", topic);
                    System.exit(-1);
                }
            } else {
                log.warn("{} with {} partition{} already exists", topic, currentPartitions,
                        currentPartitions == 1? "" : "s");
            }
        } catch (Exception e) {
            log.error("Unable to create {} - {}", topic, e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private void deleteTopic(String topic) {
        if (topicSubstitution) {
            if (preAllocatedTopics.get(topic) == null) {
                throw new IllegalArgumentException("Missing topic substitution for "+topic);
            }
            return;
        }
        if (topicExists(topic)) {
            startAdmin();
            lastAccess = System.currentTimeMillis();
            DeleteTopicsResult deleteTask = admin.deleteTopics(Collections.singletonList(topic));
            try {
                deleteTask.all().get();
                processed++;
                // check if removal is successful
                // try a few times due to eventual consistency
                boolean found = false;
                for (int i = 0; i < 10; i++) {
                    found = topicExists(topic);
                    if (found) {
                        // Thread.sleep is fine because the function will be running in a virtual thread
                        Thread.sleep(1000);
                        log.warn("Newly deleted {} still exists. Scanning it again.", topic);
                    } else {
                        break;
                    }
                }
                if (!found) {
                    log.info("Deleted {}", topic);
                } else {
                    log.error("Unable to delete {}", topic);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Unable to delete {} - {}", topic, e.getMessage());
                stopAdmin();
            }
        }
    }

    private List<String> listTopics() {
        if (topicSubstitution) {
            return new ArrayList<>(preAllocatedTopics.keySet());
        }
        startAdmin();
        List<String> result = new ArrayList<>();
        ListTopicsResult list = admin.listTopics();
        try {
            processed++;
            return new ArrayList<>(list.names().get());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Unable to list topics - {}", e.getMessage());
            stopAdmin();
        }
        return result;
    }

    private class InactivityMonitor {
        private final String cloudManager;

        public InactivityMonitor(String cloudManager) {
            this.cloudManager = cloudManager;
        }

        public void start() {
            Platform.getInstance().getVertx().setPeriodic(20 * 1000L, t -> {
                final long idle = 60 * 1000L;
                final long now = System.currentTimeMillis();
                if (admin != null && now - lastAccess > idle) {
                    EventEmitter.getInstance().send(cloudManager, new Kv(TYPE, STOP));
                }
            });
        }
    }

}
