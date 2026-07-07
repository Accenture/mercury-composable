/*

    Copyright 2018-2026 Accenture Technology

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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.platformlambda.cloud.ConnectorConfig;
import org.platformlambda.cloud.EventProducer;
import org.platformlambda.cloud.ServiceLifeCycle;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.MsgPack;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.common.MultipartPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventConsumer extends Thread {
    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private static final MsgPack msgPack = new MsgPack();
    private static final String OFFSET_TAG = "_offset_";
    private static final String PARTITION_TAG = "_partition_";
    private static final String KEY_TAG = "_key_";
    private static final String TIMESTAMP_TAG = "_timestamp_";
    private static final String TYPE = "type";
    private static final String INIT = "init";
    private static final String DONE = "done";
    private static final String TOKEN = "token";
    private static final long INITIALIZE = ServiceLifeCycle.INITIALIZE;
    private static final String MONITOR = "monitor";
    private static final String TO_MONITOR = "@"+MONITOR;
    private static final int INVALID_EVENT_THRESHOLD = 150;
    private final String initToken = UUID.randomUUID().toString();
    private final String topic;
    private final String realTopic;
    private final int partition;
    private int realPartition;
    private final KafkaConsumer<String, byte[]> consumer;
    private final AtomicBoolean normal = new AtomicBoolean(true);
    private int skipped = 0;
    private long offset = -1;
    private int invalidEvents = 0;
    private boolean reset = true;

    public EventConsumer(Properties base, String topic, int partition, String... parameters) {
        Utility util = Utility.getInstance();
        boolean substitute = ConnectorConfig.topicSubstitutionEnabled();
        Map<String, String> preAllocatedTopics = ConnectorConfig.getTopicSubstitution();
        this.topic = topic;
        this.partition = partition;
        this.realTopic = resolveRealTopic(topic, partition, substitute, preAllocatedTopics, util);
        Properties prop = new Properties();
        prop.putAll(base);
        // create unique values for client ID and group ID
        applyConsumerParameters(prop, parameters, util);
        prop.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        prop.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        this.consumer = new KafkaConsumer<>(prop);
    }

    // resolve the real (possibly substituted) topic name; also assigns realPartition
    private String resolveRealTopic(String topic, int partition, boolean substitute,
                                    Map<String, String> preAllocatedTopics, Utility util) {
        if (!substitute) {
            this.realPartition = partition;
            return topic;
        }
        String virtualTopic = topic + (partition < 0? "" : "." + partition);
        String topicPartition = topic + (partition < 0? "" : "#" + partition);
        topicPartition = preAllocatedTopics.getOrDefault(virtualTopic, topicPartition);
        int sep = topicPartition.lastIndexOf('#');
        if (sep == -1) {
            this.realPartition = -1;
            return topicPartition;
        }
        this.realPartition = util.str2int(topicPartition.substring(sep+1));
        return topicPartition.substring(0, sep);
    }

    private void applyConsumerParameters(Properties prop, String[] parameters, Utility util) {
        if (parameters.length != 2 && parameters.length != 3) {
            throw new IllegalArgumentException("Unable to start consumer for " + realTopic +
                                                " - parameters must be clientId, groupId and an optional offset");
        }
        prop.put(ConsumerConfig.CLIENT_ID_CONFIG, parameters[0]);
        prop.put(ConsumerConfig.GROUP_ID_CONFIG, parameters[1]);
        /*
         * If offset is not given, the consumer will read from the latest when it is started for the first time.
         * Subsequent restart of the consumer will resume read from the current offset.
         */
        if (parameters.length == 3) {
            long v = util.str2long(parameters[2]);
            offset = INITIALIZE == v? v : Math.max(-1, v);
        }
    }

    private long getEarliest(TopicPartition tp) {
        Map<TopicPartition, Long> data = consumer.beginningOffsets(Collections.singletonList(tp));
        return data.get(tp);
    }

    private long getLatest(TopicPartition tp) {
        Map<TopicPartition, Long> data = consumer.endOffsets(Collections.singletonList(tp));
        return data.get(tp);
    }

    @Override
    public void run() {
        final boolean requireInitialization = startInitialLoadIfNeeded();
        Platform platform = Platform.getInstance();
        String origin = platform.getOrigin();
        EventEmitter po = EventEmitter.getInstance();
        String virtualTopic = (topic + (partition < 0? "" : "." + partition)).toLowerCase();
        String topicPartition = realTopic + (realPartition < 0? "" : "." + realPartition);
        if (realPartition < 0) {
            consumer.subscribe(Collections.singletonList(realTopic));
        } else {
            consumer.assign(Collections.singletonList(new TopicPartition(realTopic, realPartition)));
        }
        log.info("Subscribed {}", topicPartition);
        try {
            while (normal.get()) {
                long interval = reset? 15 : 30;
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(interval));
                if (reset && prepareOffset()) {
                    continue;
                }
                processRecords(records, origin, virtualTopic, topicPartition);
            }
        } catch(WakeupException e) {
            log.info("Stopping listener for {}", virtualTopic);
        } catch (Exception e) {
            // when this happens, it is better to shut down so that infrastructure can restart the app instance.
            log.error("Event stream error for {} - {} {}", topicPartition, e.getClass(), e.getMessage());
            System.exit(10);
        } finally {
            consumer.close();
            log.info("Unsubscribed {}", topicPartition);
            String initHandler = INIT + "." + (partition < 0 ? topic : topic + "." + partition);
            if (requireInitialization && platform.hasRoute(initHandler)) {
                po.send(initHandler, DONE);
            }
        }
    }

    private boolean startInitialLoadIfNeeded() {
        boolean requireInitialization = offset == INITIALIZE;
        if (requireInitialization) {
            /*
             * IMPORTANT
             * ---------
             * Kafka will do load balancing for different consumers of the same group.
             * Mercury system topics require direct assignment to an exact partition to
             * enable broadcast instead of load balancing.
             *
             * Therefore, we are setting partition to 0 if none is given.
             */
            if (ConnectorConfig.topicSubstitutionEnabled() && realPartition < 0) {
                realPartition = 0;
            }
            new ServiceLifeCycle(topic, partition, initToken).start();
        }
        return requireInitialization;
    }

    // position the READ offset once a partition is assigned; returns true if this poll should be skipped
    private boolean prepareOffset() {
        Set<TopicPartition> p = consumer.assignment();
        if (p.isEmpty()) {
            // wait until a partition is assigned
            return true;
        }
        reset = false;
        if (offset != -1) {
            if (p.size() == 1) {
                seekToOffset(p);
                return true;
            } else if (partition == -1) {
                log.warn("Unable to override '{}' READ offset to {} because there are more than " +
                        "one partitions. Number of partitions assigned: {}", realTopic, offset, p.size());
            }
        }
        return false;
    }

    private void seekToOffset(Set<TopicPartition> p) {
        for (TopicPartition tp : p) {
            long earliest = getEarliest(tp);
            long latest = getLatest(tp);
            if (offset < 0) {
                consumer.seek(tp, latest);
                log.info("Seek '{}' READ offset, partition-{} to latest ({} - {})",
                        realTopic, tp.partition(), earliest, latest);
            } else if (offset < earliest) {
                consumer.seek(tp, earliest);
                log.warn("Set '{}' READ offset, partition-{} to earliest instead of " +
                         "{} ({} - {})", realTopic, tp.partition(), offset, earliest, latest);
            } else if (offset < latest) {
                consumer.seek(tp, offset);
                log.info("Set '{}' READ offset, partition-{} to {} ({} - {})",
                        realTopic, tp.partition(), offset, earliest, latest);
            } else {
                consumer.seek(tp, latest);
                if (latest == offset) {
                    log.info("Set '{}' READ offset, partition-{} to latest ({} - {})",
                            realTopic, tp.partition(), earliest, latest);
                } else {
                    log.warn("Set '{}' READ offset, partition-{} to latest instead of " +
                             "{} ({} - {})", realTopic, tp.partition(), offset, earliest, latest);
                }
            }
        }
    }

    private void processRecords(ConsumerRecords<String, byte[]> records, String origin,
                                String virtualTopic, String topicPartition) {
        for (ConsumerRecord<String, byte[]> rec : records) {
            processRecord(rec, origin, virtualTopic, topicPartition);
        }
    }

    private void processRecord(ConsumerRecord<String, byte[]> rec, String origin,
                               String virtualTopic, String topicPartition) {
        Map<String, String> originalHeaders = getSimpleHeaders(rec.headers());
        String dataType = originalHeaders.getOrDefault(EventProducer.DATA_TYPE, EventProducer.BYTES_DATA);
        boolean embedEvent = originalHeaders.containsKey(EventProducer.EMBED_EVENT);
        String recipient = originalHeaders.get(EventProducer.RECIPIENT);
        if (recipient != null && !recipient.contains(MONITOR) && !recipient.equals(origin)) {
            /*
             * this is an error case when two consumers listen to the same partition
             * or when READ offset is incorrect
             */
            log.error("Skipping record {} because it belongs to {}", rec.offset(), recipient);
            if (++invalidEvents > INVALID_EVENT_THRESHOLD) {
                throw new IllegalArgumentException("Too many outdated events - likely to be a READ offset error");
            }
            return;
        }
        byte[] data = rec.value();
        EventEnvelope message = new EventEnvelope();
        if (embedEvent) {
            deliverEmbeddedEvent(message, data, topicPartition);
        } else {
            deliverRawEvent(rec, data, message, originalHeaders, dataType, virtualTopic, topicPartition);
        }
    }

    private void deliverEmbeddedEvent(EventEnvelope message, byte[] data, String topicPartition) {
        // payload is an embedded event
        try {
            message.load(data);
        } catch (Exception e) {
            log.error("Unable to decode incoming event for {} - {}", topicPartition, e.getMessage());
            return;
        }
        try {
            String to = message.getTo();
            if (to != null) {
                // remove special routing qualifier for presence monitor events
                if (to.contains(TO_MONITOR)) {
                    message.setTo(to.substring(0, to.indexOf(TO_MONITOR)));
                }
                EventEmitter.getInstance().send(message);
            } else {
                MultipartPayload.getInstance().incoming(message);
            }
        } catch (Exception e) {
            log.error("Unable to process incoming event for {} - {} {}",
                    topicPartition, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void deliverRawEvent(ConsumerRecord<String, byte[]> rec, byte[] data, EventEnvelope message,
                                 Map<String, String> originalHeaders, String dataType,
                                 String virtualTopic, String topicPartition) {
        if (offset == INITIALIZE) {
            if (INIT.equals(originalHeaders.get(TYPE)) && initToken.equals(originalHeaders.get(TOKEN))) {
                offset = -1;
                if (skipped > 0) {
                    log.info("Skipped {} outdated event{}", skipped, skipped == 1 ? "" : "s");
                }
            } else {
                skipped++;
                return;
            }
        }
        // transport the headers and payload in original form
        try {
            setMessageBody(message, originalHeaders, dataType, data, Utility.getInstance());
            /*
             * Offset is only meaningful when listening to a specific partition.
             * This allows user application to reposition offset when required.
             *
             * For direct pub/sub use, kafka specific metadata are encoded in:
             * _key_, _timestamp_, _partition_ and _offset_
             */
            message.setHeader(KEY_TAG, rec.key());
            message.setHeader(TIMESTAMP_TAG, rec.timestamp());
            message.setHeader(PARTITION_TAG, rec.partition());
            message.setHeader(OFFSET_TAG, rec.offset());
            EventEmitter.getInstance().send(message.setTo(virtualTopic));
        } catch (Exception e) {
            log.error("Unable to process incoming raw event for {} - {} {}",
                    topicPartition, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void setMessageBody(EventEnvelope message, Map<String, String> originalHeaders,
                                String dataType, byte[] data, Utility util) throws IOException {
        if (EventProducer.TEXT_DATA.equals(dataType)) {
            message.setHeaders(originalHeaders).setBody(util.getUTF(data));
        } else if (EventProducer.MAP_DATA.equals(dataType) || EventProducer.LIST_DATA.equals(dataType)) {
            message.setHeaders(originalHeaders).setBody(msgPack.unpack(data));
        } else {
            message.setHeaders(originalHeaders).setBody(data);
        }
    }

    private Map<String, String> getSimpleHeaders(Headers headers) {
        Utility util = Utility.getInstance();
        Map<String, String> result = new HashMap<>();
        for (Header h: headers) {
            result.put(h.key(), util.getUTF(h.value()));
        }
        return result;
    }

    public void shutdown() {
        if (normal.get()) {
            normal.set(false);
            consumer.wakeup();
        }
    }
}
