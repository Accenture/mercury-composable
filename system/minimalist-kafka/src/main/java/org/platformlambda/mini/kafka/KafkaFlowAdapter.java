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

package org.platformlambda.mini.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.platformlambda.core.util.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Kafka Flow Adapter - the Kafka counterpart of REST automation's {@code rest.yaml}. It reads a
 * {@code kafka-flow-adapter.yaml} describing {@code topic -> flow} mappings and starts one
 * {@link KafkaFlowConsumer} thread per topic, each routing its messages into the configured Event
 * Script flow.
 *
 * <pre>
 * consumer:
 *   - topic: 'topic-1'
 *     flow: 'system-of-record'
 *   - topic: 'topic-2'
 *     flow: 'soa-reply'
 * </pre>
 */
public class KafkaFlowAdapter implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaFlowAdapter.class);
    private static final String CONSUMER = "consumer";
    private static final String TOPIC = "topic";
    private static final String FLOW = "flow";

    private final List<KafkaFlowConsumer> consumers = new ArrayList<>();

    public KafkaFlowAdapter(String bootstrapServers, ConfigReader config, long flowTimeoutMs) {
        Object entries = config.get(CONSUMER);
        if (!(entries instanceof List<?> list)) {
            throw new IllegalArgumentException("kafka-flow-adapter config must contain a 'consumer' list");
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> entry) {
                String topic = String.valueOf(entry.get(TOPIC));
                String flowId = String.valueOf(entry.get(FLOW));
                Consumer<String, byte[]> consumer = newConsumer(bootstrapServers, topic);
                consumers.add(new KafkaFlowConsumer(consumer, topic, flowId, flowTimeoutMs));
                log.info("Kafka flow adapter binding: topic '{}' -> flow '{}'", topic, flowId);
            }
        }
    }

    public void start() {
        consumers.forEach(KafkaFlowConsumer::start);
    }

    private static Consumer<String, byte[]> newConsumer(String bootstrapServers, String topic) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-flow-adapter." + topic);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // commit-after-process, one message at a time -> at-least-once delivery (see KafkaFlowConsumer)
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        return new KafkaConsumer<>(p);
    }

    @Override
    public void close() {
        consumers.forEach(KafkaFlowConsumer::close);
    }
}
