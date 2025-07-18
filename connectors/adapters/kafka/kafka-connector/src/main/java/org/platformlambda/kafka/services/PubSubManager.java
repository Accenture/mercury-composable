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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.platformlambda.cloud.ConnectorConfig;
import org.platformlambda.cloud.EventProducer;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.models.PubSubProvider;
import org.platformlambda.core.serializers.MsgPack;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class PubSubManager implements PubSubProvider {
    private static final Logger log = LoggerFactory.getLogger(PubSubManager.class);

    private static final MsgPack msgPack = new MsgPack();
    private static final String TYPE = "type";
    private static final String PARTITIONS = "partitions";
    private static final String CREATE = "create";
    private static final String LIST = "list";
    private static final String EXISTS = "exists";
    private static final String DELETE = "delete";
    private static final String TOPIC = "topic";
    private static final AtomicLong seq = new AtomicLong(0);
    private final ConcurrentMap<String, EventConsumer> subscribers = new ConcurrentHashMap<>();
    private long totalEvents = 0;
    private final Properties baseProperties;
    private final String cloudManager;
    private Map<String, String> preAllocatedTopics;
    private String producerId = null;
    private KafkaProducer<String, byte[]> producer = null;

    public PubSubManager(String domain, Properties baseProperties, String cloudManager) {
        this.baseProperties = baseProperties;
        this.cloudManager = cloudManager;
        // start Kafka Topic Manager
        log.info("Starting {} pub/sub manager - {}", domain, cloudManager);
        Platform.getInstance().registerPrivate(cloudManager,
                new TopicManager(baseProperties, cloudManager), 1);
        preAllocatedTopics = ConnectorConfig.getTopicSubstitution();
        // clean up subscribers when application stops
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.putAll(baseProperties);
        properties.put(ProducerConfig.ACKS_CONFIG, "1"); // Setting to "1" ensures that the message is received by the leader
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class);
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 15000);
        return properties;
    }

    private void sendEvent(String topic, int partition, List<Header> headers, byte[] payload) {
        Utility util = Utility.getInstance();
        final String realTopic;
        final int realPartition;
        String virtualTopic = topic + (partition < 0? "" : "." + partition);
        if (ConnectorConfig.topicSubstitutionEnabled()) {
            String topicPartition = topic + (partition < 0? "" : "#" + partition);
            topicPartition = preAllocatedTopics.getOrDefault(virtualTopic, topicPartition);
            int sep = topicPartition.lastIndexOf('#');
            if (sep == -1) {
                realTopic = topicPartition;
                realPartition = -1;
            } else {
                realTopic = topicPartition.substring(0, sep);
                realPartition = util.str2int(topicPartition.substring(sep+1));
            }
        } else {
            realTopic = topic;
            realPartition = partition;
        }
        startProducer();
        try {
            long t1 = System.currentTimeMillis();
            String id = util.getUuid();
            if (realPartition < 0) {
                producer.send(new ProducerRecord<>(realTopic, null, id, payload, headers))
                        .get(20, TimeUnit.SECONDS);
            } else {
                producer.send(new ProducerRecord<>(realTopic, realPartition, id, payload, headers))
                        .get(20, TimeUnit.SECONDS);
            }
            long diff = System.currentTimeMillis() - t1;
            if (diff > 5000) {
                log.error("Kafka is slow - took {} ms to send to {}", diff, virtualTopic);
            }
            totalEvents++;

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // when this happens, it is better to shut down so that it can be restarted by infrastructure automatically
            log.error("Unable to publish event to {} - {}", virtualTopic, e.getMessage());
            closeProducer();
            System.exit(20);
        }
    }

    @Override
    public boolean createTopic(String topic) {
        return createTopic(topic, 1);
    }

    @Override
    public boolean createTopic(String topic, int partitions) {
        ConnectorConfig.validateTopicName(topic);
        final long timeout = 20 * 1000L;
        EventEnvelope req = new EventEnvelope().setTo(cloudManager).setHeader(TYPE, CREATE)
                .setHeader(TOPIC, topic).setHeader(PARTITIONS, partitions);
        try {
            EventEnvelope res = EventEmitter.getInstance().request(req, timeout).get();
            if (res != null && res.getBody() instanceof Boolean status) {
                return status;
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    @Override
    public void deleteTopic(String topic) {
        final long timeout = 20 * 1000L;
        EventEnvelope req = new EventEnvelope().setTo(cloudManager).setHeader(TYPE, DELETE).setHeader(TOPIC, topic);
        try {
            EventEmitter.getInstance().request(req, timeout).get();
        } catch (InterruptedException | ExecutionException e) {
            // ok to ignore
        }
    }

    @Override
    public boolean createQueue(String queue) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void deleteQueue(String queue) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public boolean exists(String topic) {
        final long timeout = 20 * 1000L;
        EventEnvelope req = new EventEnvelope().setTo(cloudManager).setHeader(TYPE, EXISTS).setHeader(TOPIC, topic);
        try {
            EventEnvelope res = EventEmitter.getInstance().request(req, timeout).get();
            if (res != null && res.getBody() instanceof Boolean status) {
                return status;
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    @Override
    public int partitionCount(String topic) {
        final long timeout = 20 * 1000L;
        EventEnvelope req = new EventEnvelope().setTo(cloudManager).setHeader(TYPE, PARTITIONS).setHeader(TOPIC, topic);
        try {
            EventEnvelope res = EventEmitter.getInstance().request(req, timeout).get();
            if (res != null && res.getBody() instanceof Integer count) {
                return count;
            } else {
                return -1;
            }
        } catch (InterruptedException | ExecutionException e) {
            return -1;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> list() {
        long timeout = 20 * 1000L;
        EventEnvelope req = new EventEnvelope().setTo(cloudManager).setHeader(TYPE, LIST);
        try {
            EventEnvelope res = EventEmitter.getInstance().request(req, timeout).get();
            if (res != null && res.getBody() instanceof List) {
                return (List<String>) res.getBody();
            } else {
                return Collections.emptyList();
            }
        } catch (InterruptedException | ExecutionException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isStreamingPubSub() {
        return true;
    }

    @Override
    public void cleanup() {
        closeProducer();
    }

    private synchronized void startProducer() {
        if (producer == null) {
            // create unique ID from origin ID by dropping date prefix and adding a sequence suffix
            String id = (Platform.getInstance().getOrigin()+"ps"+(seq.incrementAndGet())).substring(8);
            Properties properties = getProperties();
            properties.put(ProducerConfig.CLIENT_ID_CONFIG, id);
            producer = new KafkaProducer<>(properties);
            producerId = properties.getProperty(ProducerConfig.CLIENT_ID_CONFIG);
            log.info("Producer {} ready", properties.getProperty(ProducerConfig.CLIENT_ID_CONFIG));
        }
    }

    private synchronized void closeProducer() {
        if (producer != null) {
            try {
                producer.close();
                log.info("Producer {} released, delivered: {}", producerId, totalEvents);
            } catch (Exception e) {
                // ok to ignore
            }
            producer = null;
            producerId = null;
            totalEvents = 0;
        }
    }

    @Override
    public void publish(String topic, Map<String, String> headers, Object body) {
        publish(topic, -1, headers, body);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void publish(String topic, int partition, Map<String, String> headers, Object body) {
        ConnectorConfig.validateTopicName(topic);
        Utility util = Utility.getInstance();
        Map<String, String> eventHeaders = headers == null? new HashMap<>() : headers;
        List<Header> headerList = new ArrayList<>();
        if (eventHeaders.containsKey(EventProducer.EMBED_EVENT) && body instanceof byte[] bytes) {
            headerList.add(new RecordHeader(EventProducer.EMBED_EVENT, util.getUTF("1")));
            String recipient = eventHeaders.get(EventProducer.RECIPIENT);
            if (recipient != null) {
                headerList.add(new RecordHeader(EventProducer.RECIPIENT, util.getUTF(recipient)));
            }
            sendEvent(topic, partition, headerList, bytes);
        } else {
            for (var entry: eventHeaders.entrySet()) {
                headerList.add(new RecordHeader(entry.getKey(), util.getUTF(entry.getValue())));
            }
            final byte[] payload;
            switch (body) {
                case byte[] bytes -> {
                    payload = bytes;
                    headerList.add(new RecordHeader(EventProducer.DATA_TYPE, util.getUTF(EventProducer.BYTES_DATA)));
                }
                case String s -> {
                    payload = util.getUTF(s);
                    headerList.add(new RecordHeader(EventProducer.DATA_TYPE, util.getUTF(EventProducer.TEXT_DATA)));
                }
                case Map m -> {
                    try {
                        payload = msgPack.pack(m);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                    headerList.add(new RecordHeader(EventProducer.DATA_TYPE, util.getUTF(EventProducer.MAP_DATA)));
                }
                case List data -> {
                    try {
                        payload = msgPack.pack(data);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                    headerList.add(new RecordHeader(EventProducer.DATA_TYPE, util.getUTF(EventProducer.LIST_DATA)));
                }
                case null, default -> {
                    // other primitive and PoJo are serialized as JSON string
                    payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(body);
                    headerList.add(new RecordHeader(EventProducer.DATA_TYPE, util.getUTF(EventProducer.TEXT_DATA)));
                }
            }
            sendEvent(topic, partition, headerList, payload);
        }
    }

    @Override
    public void subscribe(String topic, LambdaFunction listener, String... parameters) {
        subscribe(topic, -1, listener, parameters);
    }

    @Override
    public void subscribe(String topic, int partition, LambdaFunction listener, String... parameters) {
        ConnectorConfig.validateTopicName(topic);
        String topicPartition = (topic + (partition < 0? "" : "." + partition)).toLowerCase();
        if (parameters.length == 2 || parameters.length == 3) {
            if (parameters.length == 3 && !Utility.getInstance().isNumeric(parameters[2])) {
                throw new IllegalArgumentException("topic offset must be numeric");
            }
            if (subscribers.containsKey(topicPartition) || Platform.getInstance().hasRoute(topicPartition)) {
                String tp = (topic + (partition < 0? "" : " partition " + partition)).toLowerCase();
                throw new IllegalArgumentException(tp+" is already subscribed");
            }
            EventConsumer consumer = new EventConsumer(getProperties(), topic, partition, parameters);
            consumer.start();
            Platform.getInstance().registerPrivate(topicPartition.toLowerCase(), listener, 1);
            subscribers.put(topicPartition, consumer);
        } else {
            throw new IllegalArgumentException("Check parameters: clientId, groupId and optional offset pointer");
        }
    }

    @Override
    public void send(String queue, Map<String, String> headers, Object body) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void listen(String queue, LambdaFunction listener, String... parameters) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void unsubscribe(String topic) {
        unsubscribe(topic, -1);
    }

    @Override
    public void unsubscribe(String topic, int partition) {
        String topicPartition = (topic + (partition < 0? "" : "." + partition)).toLowerCase();
        Platform platform = Platform.getInstance();
        if (platform.hasRoute(topicPartition) && subscribers.containsKey(topicPartition)) {
            EventConsumer consumer = subscribers.get(topicPartition);
            platform.release(topicPartition);
            subscribers.remove(topicPartition);
            consumer.shutdown();
        } else {
            String tp = (topic + (partition < 0? "" : " partition " + partition)).toLowerCase();
            throw new IllegalArgumentException("No subscription found for " + tp);
        }
    }

    private void shutdown() {
        closeProducer();
        for (var entry: subscribers.entrySet()) {
            EventConsumer consumer = entry.getValue();
            consumer.shutdown();
        }
    }
}
