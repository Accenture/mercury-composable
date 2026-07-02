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

import com.accenture.models.Flows;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
 *     dlq-topic: 'topic-1-poison'  # optional; no DLQ if omitted (failed messages are dropped w/ ERROR)
 *   - topic: 'topic-2'
 *     flow: 'soa-reply'
 *     partition: 0                 # optional; pins this partition (manual assign)
 *   - topic-pattern: 'events\.[a-z]{2}'  # optional; regex subscribe instead of a literal 'topic'
 *     flow: 'process-region-event'
 *     group: 'region-events-group' # required for topic-pattern bindings (no sensible default)
 *   - topic: 'clickstream'
 *     flow: 'ingest-clickstream'
 *     auto-commit: true            # optional; trades pod-death redelivery for throughput
 *     max-poll-records: 500        # optional; only meaningful with auto-commit
 * </pre>
 *
 * <p>Exactly one of {@code topic} (a literal name) or {@code topic-pattern} (a {@link Pattern} regex,
 * subscribed via {@code subscribe(Pattern)} so newly-matching topics join automatically) must be set.
 * {@code topic-pattern} cannot be combined with {@code partition} (manual assignment needs concrete
 * topic-partitions up front) and requires an explicit {@code group} - see below.</p>
 *
 * <p>{@code group} (within the {@code consumer} section) is the Kafka consumer group id, used <b>exactly</b>
 * as given - enterprise DevSecOps teams typically create topics, ACLs and consumer groups administratively,
 * so the library must not decorate the value. This YAML is read by {@code ConfigReader}, so the value
 * supports {@code ${ENV_VAR:default}} environment-variable substitution. For a literal {@code topic} it is
 * optional and defaults to {@code kafka-flow-adapter.<topic>} (convenient for dev/test); for a
 * {@code topic-pattern} binding it is <b>required</b> (a regex string is not a sensible default group id).</p>
 *
 * <p>{@code partition} is optional and enables <b>partition pinning</b>: when present, the consumer manually
 * assigns that single partition instead of joining the consumer group for dynamic assignment (see
 * {@link KafkaFlowConsumer}). Also {@code ${ENV_VAR:default}}-substitutable, so each pod can pin a distinct
 * partition. When omitted, the consumer subscribes group-managed as usual.</p>
 *
 * <p>{@code schema.enabled: true} opts the binding into Confluent Schema Registry decoding: the consumer
 * reads the embedded schema id, looks up the registered {@code schemaType}, deserializes the value with the
 * matching Confluent deserializer, and hands the flow a {@code Map} (instead of raw byte[]). Requires
 * {@code schema.registry.url} to be configured (else the adapter fails fast at startup). When omitted, the
 * value is delivered as raw byte[] as before.</p>
 *
 * <p>{@code dlq-topic} is optional and names the pre-provisioned topic a message is parked on after
 * exhausting retries. One DLQ topic per binding - a {@code topic-pattern} binding does not get one DLQ per
 * matched concrete topic, since the same flow that consumed the message can reprocess it later regardless
 * of which concrete topic it originated from (preserved via the {@code dlq.origin.topic} header). It must
 * not equal the source {@code topic}, nor match {@code topic-pattern}, or a dead-lettered message would be
 * re-consumed by the same binding and fail forever. When omitted, a message that exhausts retries is
 * dropped with a logged {@code ERROR} instead of being dead-lettered.</p>
 *
 * <p>{@code auto-commit: true} (default {@code false}) trades the default at-least-once, commit-after-
 * process contract for Kafka-native periodic auto-commit - higher throughput, but a message being processed
 * when a pod dies may already be considered committed and is not redelivered. Retry/DLQ handling on flow
 * failure is unaffected either way; only commit timing changes. {@code max-poll-records} is an optional
 * companion (positive integer) - defaults to {@code 1} in manual-commit mode and {@code 500} in
 * auto-commit mode, either way overridable per binding.</p>
 */
public class KafkaFlowAdapter implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaFlowAdapter.class);
    private static final String CONSUMER = "consumer";
    private static final String TOPIC = "topic";
    private static final String TOPIC_PATTERN = "topic-pattern";
    private static final String FLOW = "flow";
    private static final String GROUP = "group";
    private static final String PARTITION = "partition";
    private static final String SCHEMA = "schema";
    private static final String SCHEMA_ENABLED_FLAT = "schema.enabled";
    private static final String ENABLED = "enabled";
    private static final String DLQ_TOPIC = "dlq-topic";
    private static final String AUTO_COMMIT = "auto-commit";
    private static final String MAX_POLL_RECORDS = "max-poll-records";
    private static final String DEFAULT_GROUP_PREFIX = "kafka-flow-adapter";
    private static final int MANUAL_COMMIT_MAX_POLL_RECORDS = 1;
    private static final int AUTO_COMMIT_MAX_POLL_RECORDS = 500;   // Kafka client's own default

    private final List<KafkaFlowConsumer> consumers = new ArrayList<>();
    private final Properties consumerProps;

    public KafkaFlowAdapter(Properties consumerProps, ConfigReader config, long dlqTimeout,
                            RetryPolicy retryPolicy, SchemaCodec schemaCodec) {
        this.consumerProps = consumerProps;
        Object entries = config.get(CONSUMER);
        if (!(entries instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("kafka-flow-adapter config must contain a non-empty 'consumer' list");
        }
        // Validate the whole config before opening any consumer, so a malformed entry fails fast and
        // loud (the old behavior silently skipped it) without leaking half-created consumers.
        for (int i = 0; i < list.size(); i++) {
            consumers.add(buildConsumer(i, list.get(i), dlqTimeout, retryPolicy, schemaCodec));
        }
    }

    /** Validate one consumer-binding entry and build its {@link KafkaFlowConsumer} (fail-fast on any error). */
    private KafkaFlowConsumer buildConsumer(int i, Object item, long dlqTimeout, RetryPolicy retryPolicy,
                                            SchemaCodec schemaCodec) {
        if (!(item instanceof Map<?, ?> entry)) {
            throw new IllegalArgumentException("consumer[" + i + "] must be a map with 'topic' and 'flow'");
        }
        String topic = text(entry.get(TOPIC));
        String topicPattern = text(entry.get(TOPIC_PATTERN));
        if (topic == null && topicPattern == null) {
            throw new IllegalArgumentException("consumer[" + i + "] is missing a 'topic' or 'topic-pattern'");
        }
        if (topic != null && topicPattern != null) {
            throw new IllegalArgumentException(
                    "consumer[" + i + "] cannot set both 'topic' and 'topic-pattern'");
        }
        String label = topic != null ? "topic '" + topic + "'" : "topic-pattern '" + topicPattern + "'";
        String flowId = text(entry.get(FLOW));
        if (flowId == null) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label + ") is missing a 'flow'");
        }
        boolean schemaEnabled = isSchemaEnabled(entry);
        Integer partition = parsePartition(entry.get(PARTITION));
        if (topicPattern != null && partition != null) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label + ") cannot combine "
                    + "'topic-pattern' with 'partition' - manual partition assignment requires a literal 'topic'");
        }
        if (topicPattern != null) {
            try {
                Pattern.compile(topicPattern);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("consumer[" + i + "] (" + label
                        + ") is not a valid regex: " + e.getMessage(), e);
            }
        }
        String groupId = resolveGroupId(entry, topic, topicPattern != null);
        String dlqTopic = resolveDlqTopic(entry, i, label, topic, topicPattern);
        boolean autoCommit = isAutoCommit(entry);
        Integer maxPollRecords = parseMaxPollRecords(entry.get(MAX_POLL_RECORDS));
        // Cross-reference checks last: they depend on external wiring (compiled flows, schema registry),
        // whereas everything above is self-contained validation of this one config entry's own shape.
        // fail fast if the binding names a flow that was never compiled (CompileFlows runs before this
        // @MainApplication), rather than failing every message at runtime.
        if (Flows.getFlow(flowId) == null) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label
                    + ") references unknown flow '" + flowId + "'");
        }
        if (schemaEnabled && schemaCodec == null) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label + ") sets "
                    + "schema.enabled but 'schema.registry.url' is not configured");
        }
        log.info("Kafka flow adapter binding: {} -> flow '{}' (consumer group '{}'{}{}{}{})",
                label, flowId, groupId, partition != null ? ", pinned to partition " + partition : "",
                schemaEnabled ? ", schema decode on" : "",
                dlqTopic != null ? ", dlq-topic '" + dlqTopic + "'" : "",
                autoCommit ? ", auto-commit on" : "");
        KafkaConsumerBinding.Builder builder = KafkaConsumerBinding.builder()
                .flowId(flowId).groupId(groupId).partition(partition).schemaEnabled(schemaEnabled)
                .dlqTopic(dlqTopic).autoCommit(autoCommit).maxPollRecords(maxPollRecords);
        KafkaConsumerBinding binding = (topicPattern != null ? builder.topicPattern(topicPattern)
                : builder.topic(topic)).build();
        return new KafkaFlowConsumer(newConsumer(binding), binding, dlqTimeout, retryPolicy,
                schemaEnabled ? schemaCodec : null);
    }

    /**
     * Resolve the optional {@code dlq-topic}, rejecting a self-referencing configuration that would let a
     * dead-lettered message be re-consumed by the very same binding (a poison-message loop). Visible for
     * testing.
     */
    static String resolveDlqTopic(Map<?, ?> entry, int i, String label, String topic, String topicPattern) {
        String dlqTopic = text(entry.get(DLQ_TOPIC));
        if (dlqTopic == null) {
            return null;
        }
        if (topic != null && dlqTopic.equals(topic)) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label
                    + ") 'dlq-topic' must not equal the source 'topic'");
        }
        if (topicPattern != null && Pattern.matches(topicPattern, dlqTopic)) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label + ") 'dlq-topic' ('" + dlqTopic
                    + "') must not match 'topic-pattern' - it would re-consume its own dead letters");
        }
        return dlqTopic;
    }

    /**
     * Whether the binding opts into Kafka-native auto-commit instead of the default manual commit-after-
     * process contract. Visible for testing.
     */
    static boolean isAutoCommit(Map<?, ?> entry) {
        return "true".equalsIgnoreCase(text(entry.get(AUTO_COMMIT)));
    }

    /**
     * Whether the binding opts into Confluent schema decoding. Accepts a flat {@code schema.enabled: true}
     * or a nested {@code schema:\n  enabled: true} (depending on how the YAML is authored). Visible for testing.
     */
    static boolean isSchemaEnabled(Map<?, ?> entry) {
        if (entry.get(SCHEMA) instanceof Map<?, ?> schema) {
            return "true".equalsIgnoreCase(text(schema.get(ENABLED)));
        }
        return "true".equalsIgnoreCase(text(entry.get(SCHEMA_ENABLED_FLAT)));
    }

    /**
     * Resolve the consumer group id for a binding: the {@code group} value used exactly as given, or
     * {@code kafka-flow-adapter.<topic>} when omitted (a literal-topic binding only - a {@code topic-pattern}
     * binding has no sensible default and must set {@code group} explicitly). Visible for testing.
     *
     * @throws IllegalArgumentException if {@code isPattern} and no explicit {@code group} was given
     */
    static String resolveGroupId(Map<?, ?> entry, String topic, boolean isPattern) {
        String group = text(entry.get(GROUP));
        if (group != null) {
            return group;
        }
        if (isPattern) {
            throw new IllegalArgumentException(
                    "consumer 'topic-pattern' requires an explicit 'group' - no sensible default exists for a "
                            + "pattern-based binding");
        }
        return DEFAULT_GROUP_PREFIX + "." + topic;
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

    /**
     * Parse the optional {@code max-poll-records}: {@code null} when absent, otherwise a positive integer.
     * Visible for testing.
     *
     * @throws IllegalArgumentException if present but not a valid positive integer
     */
    static Integer parseMaxPollRecords(Object value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        int maxPollRecords;
        try {
            maxPollRecords = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("consumer 'max-poll-records' must be an integer, got '" + text + "'");
        }
        if (maxPollRecords <= 0) {
            throw new IllegalArgumentException("consumer 'max-poll-records' must be > 0, got " + maxPollRecords);
        }
        return maxPollRecords;
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

    /**
     * Consumer: the shared template props (serializers pinned) + the binding's group id + the binding's
     * delivery-mode overlay ({@code enable.auto.commit} / {@code max.poll.records}) - this is the one place
     * that decides the commit contract, so there is no ambiguity with {@link KafkaClientConfig}'s base
     * template about which setting wins.
     */
    private Consumer<String, byte[]> newConsumer(KafkaConsumerBinding binding) {
        Properties p = new Properties();
        p.putAll(consumerProps);
        p.setProperty(ConsumerConfig.GROUP_ID_CONFIG, binding.groupId());
        applyDeliveryMode(p, binding);
        return new KafkaConsumer<>(p);
    }

    /**
     * Apply the binding's delivery-mode overlay onto a consumer {@link Properties}: {@code enable.auto.commit}
     * exactly as configured, and {@code max.poll.records} either the binding's explicit override or the
     * mode's own default (1 for manual-commit, {@value #AUTO_COMMIT_MAX_POLL_RECORDS} for auto-commit).
     * Visible for testing.
     */
    static void applyDeliveryMode(Properties p, KafkaConsumerBinding binding) {
        p.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(binding.autoCommit()));
        int maxPollRecords = binding.maxPollRecords() != null ? binding.maxPollRecords()
                : (binding.autoCommit() ? AUTO_COMMIT_MAX_POLL_RECORDS : MANUAL_COMMIT_MAX_POLL_RECORDS);
        p.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(maxPollRecords));
    }

    @Override
    public void close() {
        consumers.forEach(KafkaFlowConsumer::close);
    }
}
