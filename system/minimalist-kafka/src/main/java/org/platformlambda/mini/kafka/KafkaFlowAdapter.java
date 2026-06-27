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
 *     group: 'sales-order-group'   # optional; supports ${ENV_VAR:default}
 *   - topic: 'topic-2'
 *     flow: 'soa-reply'
 *     partition: 0                 # optional; pins this partition (manual assign)
 * </pre>
 *
 * <p>{@code group} (within the {@code consumer} section) is the Kafka consumer group id, used <b>exactly</b>
 * as given - enterprise DevSecOps teams typically create topics, ACLs and consumer groups administratively,
 * so the library must not decorate the value. This YAML is read by {@code ConfigReader}, so the value
 * supports {@code ${ENV_VAR:default}} environment-variable substitution. It is optional; when omitted it
 * defaults to {@code kafka-flow-adapter.<topic>} (convenient for dev/test). Set it explicitly to your
 * administratively-assigned group in production.</p>
 *
 * <p>{@code partition} is optional and enables <b>partition pinning</b>: when present, the consumer manually
 * assigns that single partition instead of joining the consumer group for dynamic assignment (see
 * {@link KafkaFlowConsumer}). Also {@code ${ENV_VAR:default}}-substitutable, so each pod can pin a distinct
 * partition. When omitted, the consumer subscribes group-managed as usual.</p>
 */
public class KafkaFlowAdapter implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaFlowAdapter.class);
    private static final String CONSUMER = "consumer";
    private static final String TOPIC = "topic";
    private static final String FLOW = "flow";
    private static final String GROUP = "group";
    private static final String PARTITION = "partition";
    private static final String DEFAULT_GROUP_PREFIX = "kafka-flow-adapter";

    private final List<KafkaFlowConsumer> consumers = new ArrayList<>();
    private final Properties consumerProps;

    public KafkaFlowAdapter(Properties consumerProps, ConfigReader config, long flowTimeoutMs,
                            RetryPolicy retryPolicy) {
        this.consumerProps = consumerProps;
        Object entries = config.get(CONSUMER);
        if (!(entries instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("kafka-flow-adapter config must contain a non-empty 'consumer' list");
        }
        // Validate the whole config before opening any consumer, so a malformed entry fails fast and
        // loud (the old behaviour silently skipped it) without leaking half-created consumers.
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map<?, ?> entry)) {
                throw new IllegalArgumentException("consumer[" + i + "] must be a map with 'topic' and 'flow'");
            }
            String topic = text(entry.get(TOPIC));
            String flowId = text(entry.get(FLOW));
            if (topic == null) {
                throw new IllegalArgumentException("consumer[" + i + "] is missing a 'topic'");
            }
            if (flowId == null) {
                throw new IllegalArgumentException("consumer[" + i + "] (topic '" + topic + "') is missing a 'flow'");
            }
            String groupId = resolveGroupId(entry, topic);
            Integer partition = parsePartition(entry.get(PARTITION));
            Consumer<String, byte[]> consumer = newConsumer(groupId);
            consumers.add(new KafkaFlowConsumer(consumer, topic, flowId, flowTimeoutMs, retryPolicy, partition));
            log.info("Kafka flow adapter binding: topic '{}' -> flow '{}' (consumer group '{}'{})",
                    topic, flowId, groupId, partition != null ? ", pinned to partition " + partition : "");
        }
    }

    /**
     * Resolve the consumer group id for a binding: the {@code group} value used exactly as given, or
     * {@code kafka-flow-adapter.<topic>} when omitted. Visible for testing.
     */
    static String resolveGroupId(Map<?, ?> entry, String topic) {
        String group = text(entry.get(GROUP));
        return group != null ? group : DEFAULT_GROUP_PREFIX + "." + topic;
    }

    /**
     * Parse the optional {@code partition} for partition pinning: {@code null} when absent, otherwise a
     * non-negative integer. Visible for testing.
     *
     * @throws IllegalArgumentException if present but not a valid non-negative integer
     */
    static Integer parsePartition(Object value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        int partition;
        try {
            partition = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("consumer 'partition' must be an integer, got '" + text + "'");
        }
        if (partition < 0) {
            throw new IllegalArgumentException("consumer 'partition' must be >= 0, got " + partition);
        }
        return partition;
    }

    /** @return the trimmed value, or {@code null} if absent/blank. */
    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    public void start() {
        consumers.forEach(KafkaFlowConsumer::start);
    }

    /** Consumer: the shared template props (serializers + at-least-once pinned) + the binding's group id. */
    private Consumer<String, byte[]> newConsumer(String groupId) {
        Properties p = new Properties();
        p.putAll(consumerProps);
        p.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        return new KafkaConsumer<>(p);
    }

    @Override
    public void close() {
        consumers.forEach(KafkaFlowConsumer::close);
    }
}
