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

import com.accenture.automation.EventScriptManager;
import com.accenture.models.Flows;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.platformlambda.core.system.Platform;
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
 *   - topic: 'mixed-events'        # second-level routing: pick the target per record
 *     serializer: 'json'           # optional; best-effort JSON decode on a non-schema topic
 *     ttl: '30s'                   # optional; deadline for task:// targets (default 30s)
 *     flows:
 *       - 'input.header.type(order) -> flow://order-flow'
 *       - 'input.body.event.kind(refund) -> task://v1.refund.processor'
 *       - 'default -> flow://catch-all-flow'
 * </pre>
 *
 * <p>Exactly one of {@code topic} (a literal name) or {@code topic-pattern} (a {@link Pattern} regex,
 * subscribed via {@code subscribe(Pattern)} so newly-matching topics join automatically) must be set.
 * {@code topic-pattern} cannot be combined with {@code partition} (manual assignment needs concrete
 * topic-partitions up front) and requires an explicit {@code group} - see below.</p>
 *
 * <p><b>Second-level routing (opt-in).</b> Exactly one of {@code flow} (every record goes to one flow)
 * or {@code flows} (a rule list inspecting one key-value of each record to pick the target per message)
 * must be set. See {@link RoutingRuleSet} for the rule grammar (selectors, the three matcher modes,
 * first-match-wins evaluation, the mandatory {@code default}) and {@link KafkaFlowConsumer} for the
 * {@code task://} dispatch contract. All rule targets are validated at startup: a {@code flow://} target
 * must be a compiled flow and a {@code task://} target must be a registered route (functions preload
 * before this adapter starts).</p>
 *
 * <p>{@code serializer: 'json'} (optional, non-schema bindings only) makes the consumer TRY deserializing
 * each record value with the default SimpleMapper before routing: a JSON object becomes a {@code Map}
 * (enabling {@code input.body.*} rules and Map delivery), a JSON array becomes a {@code List}, and
 * anything else - malformed text included - keeps the raw byte[] and simply passes it to the selected
 * target (best-effort by design: no special poison handling; a target that cannot digest the bytes fails
 * normally into the retry/DLQ path). Mutually exclusive with {@code schema.enabled}, which remains the
 * strict decode-before-routing contract. The parameter is open-ended for later extension; {@code json}
 * is the only supported value today.</p>
 *
 * <p>{@code ttl} (optional, duration syntax like {@code 30s}/{@code 5m}) is the deadline for a
 * {@code task://} invocation - a bare function has no flow ttl of its own (default 30s). Flow targets
 * always use their own flow ttl.</p>
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
    private static final String FLOWS = "flows";
    private static final String SERIALIZER = "serializer";
    private static final String SERIALIZER_JSON = "json";
    private static final String TTL = "ttl";
    private static final String GROUP = "group";
    private static final String PARTITION = "partition";
    private static final String SCHEMA = "schema";
    private static final String SCHEMA_ENABLED_FLAT = "schema.enabled";
    private static final String ENABLED = "enabled";
    // Optional per-binding overrides of the global kafka.trace.id.header / kafka.correlation.id.header
    // / kafka.traceparent.header, for impedance matching with an upstream that uses its own header convention.
    private static final String TRACE_ID_HEADER_FLAT = "trace.id.header";
    private static final String CORRELATION_ID_HEADER_FLAT = "correlation.id.header";
    private static final String TRACEPARENT_HEADER_FLAT = "traceparent.header";
    private static final String DLQ_TOPIC = "dlq-topic";
    private static final String AUTO_COMMIT = "auto-commit";
    private static final String MAX_POLL_RECORDS = "max-poll-records";
    private static final String DEFAULT_GROUP_PREFIX = "kafka-flow-adapter";
    private static final int MANUAL_COMMIT_MAX_POLL_RECORDS = 1;
    private static final int AUTO_COMMIT_MAX_POLL_RECORDS = 500;   // Kafka client's own default
    // Kafka's own max.poll.interval.ms default - the floor for the derivation, which only ever raises it.
    private static final long KAFKA_DEFAULT_MAX_POLL_INTERVAL_MS = 300000;
    // margin on top of the computed worst-case processing envelope (poll overhead, DLQ confirm-write, GC)
    private static final long POLL_INTERVAL_HEADROOM_MS = 10000;

    private final List<KafkaFlowConsumer> consumers = new ArrayList<>();
    private final Properties consumerProps;
    // the registry-url application property named in error messages (for accurate diagnostics) -
    // twin-kafka passes its secondary key so operators are pointed at the right setting
    private final String registryUrlKey;

    public KafkaFlowAdapter(Properties consumerProps, ConfigReader config, long dlqTimeout,
                            RetryPolicy retryPolicy, SchemaCodec schemaCodec) {
        this(consumerProps, config, dlqTimeout, retryPolicy, schemaCodec, "schema.registry.url");
    }

    public KafkaFlowAdapter(Properties consumerProps, ConfigReader config, long dlqTimeout,
                            RetryPolicy retryPolicy, SchemaCodec schemaCodec, String registryUrlKey) {
        this.consumerProps = consumerProps;
        this.registryUrlKey = registryUrlKey;
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
        String label = validateTopicSelector(i, topic, topicPattern);
        String flowId = text(entry.get(FLOW));
        RoutingRuleSet routing = resolveRouting(i, label, entry, flowId);
        boolean schemaEnabled = isSchemaEnabled(entry);
        boolean jsonSerializer = isJsonSerializer(entry, i, label, schemaEnabled);
        Long taskTtlMs = parseTaskTtl(entry.get(TTL));
        Integer partition = parsePartition(entry.get(PARTITION));
        if (topicPattern != null && partition != null) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label + ") cannot combine "
                    + "'topic-pattern' with 'partition' - manual partition assignment requires a literal 'topic'");
        }
        String groupId = resolveGroupId(entry, topic, topicPattern != null);
        String dlqTopic = resolveDlqTopic(entry, i, label, topic, topicPattern);
        boolean autoCommit = isAutoCommit(entry);
        Integer maxPollRecords = parseMaxPollRecords(entry.get(MAX_POLL_RECORDS));
        // Cross-reference checks last: they depend on external wiring (compiled flows, the platform
        // registry, schema registry), whereas everything above is self-contained validation of this one
        // config entry's own shape. Fail fast if the binding names a flow that was never compiled or a
        // task route that was never registered (CompileFlows and function preload both run before this
        // @MainApplication), rather than failing every message at runtime.
        if (routing != null) {
            validateRoutingTargets(i, label, routing);
        } else if (Flows.getFlow(flowId) == null) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label
                    + ") references unknown flow '" + flowId + "'");
        }
        if (schemaEnabled && schemaCodec == null) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label + ") sets "
                    + "schema.enabled but '" + registryUrlKey + "' is not configured");
        }
        KafkaConsumerBinding.Builder builder = KafkaConsumerBinding.builder()
                .flowId(flowId).routingRules(routing).groupId(groupId).partition(partition)
                .schemaEnabled(schemaEnabled).jsonSerializer(jsonSerializer).taskTtlMs(taskTtlMs)
                .dlqTopic(dlqTopic).autoCommit(autoCommit).maxPollRecords(maxPollRecords)
                .traceIdHeader(nestedText(entry, TRACE_ID_HEADER_FLAT))
                .correlationIdHeader(nestedText(entry, CORRELATION_ID_HEADER_FLAT))
                .traceparentHeader(nestedText(entry, TRACEPARENT_HEADER_FLAT));
        KafkaConsumerBinding binding = (topicPattern != null ? builder.topicPattern(topicPattern)
                : builder.topic(topic)).build();
        logBinding(label, binding);
        return new KafkaFlowConsumer(newConsumer(binding, retryPolicy), binding, dlqTimeout, retryPolicy,
                schemaEnabled ? schemaCodec : null);
    }

    /**
     * Read an optional dotted key from a binding entry. ConfigReader normalizes dotted keys into nested
     * maps (e.g. {@code trace.id.header} becomes {@code {trace: {id: {header: value}}}}), so this walks
     * the nested form and also accepts a flat key (for a map authored programmatically). Visible for testing.
     *
     * @param entry   the consumer-binding entry
     * @param flatKey the dotted key, e.g. {@code trace.id.header}
     * @return the value as text, or {@code null} when absent
     */
    static String nestedText(Map<?, ?> entry, String flatKey) {
        Object direct = entry.get(flatKey);
        if (direct != null) {
            return text(direct);
        }
        Object node = entry;
        for (String segment : flatKey.split("\\.")) {
            if (!(node instanceof Map<?, ?> map)) {
                return null;
            }
            node = map.get(segment);
        }
        return text(node);
    }

    /**
     * Resolve the binding's routing: exactly one of {@code flow} (direct routing) or {@code flows}
     * (second-level routing rules) must be set. Returns the compiled rule set for {@code flows}, or
     * {@code null} for direct {@code flow} routing. Visible for testing.
     */
    static RoutingRuleSet resolveRouting(int i, String label, Map<?, ?> entry, String flowId) {
        Object flowsValue = entry.get(FLOWS);
        if (flowId != null && flowsValue != null) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label
                    + ") cannot set both 'flow' and 'flows' - use 'flow' for direct routing or "
                    + "'flows' for second-level routing");
        }
        if (flowId == null && flowsValue == null) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label
                    + ") is missing a 'flow' or 'flows'");
        }
        if (flowsValue == null) {
            return null;
        }
        if (!(flowsValue instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label
                    + ") 'flows' must be a non-empty list of routing rules");
        }
        List<String> ruleStrings = new ArrayList<>();
        for (Object item : list) {
            String rule = text(item);
            if (rule == null) {
                throw new IllegalArgumentException("consumer[" + i + "] (" + label
                        + ") 'flows' contains an empty routing rule");
            }
            ruleStrings.add(rule);
        }
        try {
            return RoutingRuleSet.compile(ruleStrings);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label + ") " + e.getMessage(), e);
        }
    }

    /**
     * Fail fast when a routing rule names a flow that was never compiled or a task route that is not in
     * the platform registry - functions preload before this {@code @MainApplication} adapter starts.
     * Therefore, target existence is checkable here rather than failing every matching message at runtime.
     */
    private static void validateRoutingTargets(int i, String label, RoutingRuleSet routing) {
        for (RoutingRuleSet.Target target : routing.allTargets()) {
            if (target.task()) {
                // the flow engine is a registered route, but addressing it as a bare task would bypass
                // the flow-launch contract (no flow_id) - flows are dispatched with flow:// only
                if (EventScriptManager.SERVICE_NAME.equals(target.destination())) {
                    throw new IllegalArgumentException("consumer[" + i + "] (" + label + ") 'task://"
                            + EventScriptManager.SERVICE_NAME
                            + "' is not allowed - use 'flow://<flow-id>' to dispatch to the flow engine");
                }
                // Platform is touched only when a task:// target exists (flow-only rule sets stay
                // platform-free, which also keeps config-validation unit tests lightweight)
                if (!Platform.getInstance().hasRoute(target.destination())) {
                    throw new IllegalArgumentException("consumer[" + i + "] (" + label
                            + ") references unknown task route '" + target.destination() + "'");
                }
            } else if (Flows.getFlow(target.destination()) == null) {
                throw new IllegalArgumentException("consumer[" + i + "] (" + label
                        + ") references unknown flow '" + target.destination() + "'");
            }
        }
    }

    /**
     * Whether the binding opts into best-effort JSON deserialization on a non-schema topic
     * ({@code serializer: 'json'}). The parameter is open-ended for later extension, so any other value
     * is rejected loudly rather than silently ignored. Visible for testing.
     */
    static boolean isJsonSerializer(Map<?, ?> entry, int i, String label, boolean schemaEnabled) {
        String serializer = text(entry.get(SERIALIZER));
        if (serializer == null) {
            return false;
        }
        if (!SERIALIZER_JSON.equalsIgnoreCase(serializer)) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label
                    + ") unsupported 'serializer' '" + serializer + "' - only 'json' is supported");
        }
        if (schemaEnabled) {
            throw new IllegalArgumentException("consumer[" + i + "] (" + label
                    + ") cannot combine 'serializer' with schema.enabled - the schema registry "
                    + "already owns the decode");
        }
        return true;
    }

    /**
     * Parse the optional per-binding {@code ttl} (duration syntax, e.g. {@code 30s}, {@code 5m}) into
     * milliseconds - the deadline for a {@code task://} invocation, which has no flow ttl of its own.
     * {@code null} when absent (the consumer applies its 30s default). Visible for testing.
     *
     * <p>Long arithmetic throughout - an absurd-but-accepted duration must be rejected or honored as
     * written, never silently wrapped to a different positive value (the ttl overflow-guard precedent).</p>
     *
     * @throws IllegalArgumentException if present but not a positive duration
     */
    static Long parseTaskTtl(Object value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        long seconds = durationSeconds(text);
        if (seconds <= 0) {
            throw new IllegalArgumentException(
                    "consumer 'ttl' must be a positive duration (e.g. '30s'), got '" + text + "'");
        }
        return seconds * 1000L;
    }

    /**
     * Long-math twin of {@code Utility.getDurationInSeconds} (suffixes s/m/h/d; no suffix = seconds),
     * immune to int wrap-around on pathological inputs. Returns -1 on anything malformed.
     */
    private static long durationSeconds(String duration) {
        long multiplier = 1;
        String number = duration;
        char last = duration.charAt(duration.length() - 1);
        if (!Character.isDigit(last)) {
            multiplier = switch (Character.toLowerCase(last)) {
                case 's' -> 1;
                case 'm' -> 60;
                case 'h' -> 3600;
                case 'd' -> 86400;
                default -> -1;
            };
            number = duration.substring(0, duration.length() - 1).trim();
        }
        if (multiplier < 0 || number.isEmpty()) {
            return -1;
        }
        try {
            return Long.parseLong(number) * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Validate the topic/topic-pattern selector (exactly one is set, valid regex) and return a display label. */
    private String validateTopicSelector(int i, String topic, String topicPattern) {
        if (topic == null && topicPattern == null) {
            throw new IllegalArgumentException("consumer[" + i + "] is missing a 'topic' or 'topic-pattern'");
        }
        if (topic != null && topicPattern != null) {
            throw new IllegalArgumentException(
                    "consumer[" + i + "] cannot set both 'topic' and 'topic-pattern'");
        }
        if (topicPattern != null) {
            try {
                Pattern.compile(topicPattern);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("consumer[" + i + "] (topic-pattern '" + topicPattern
                        + "') is not a valid regex: " + e.getMessage(), e);
            }
        }
        return topic != null ? "topic '" + topic + "'" : "topic-pattern '" + topicPattern + "'";
    }

    /** Log a one-line summary of a resolved consumer binding. */
    private void logBinding(String label, KafkaConsumerBinding binding) {
        log.info("Kafka flow adapter binding: {} -> {} (consumer group '{}'{}{}{}{}{}{}{}{}{})",
                label,
                binding.routingRules() != null
                        ? "second-level routing (" + binding.routingRules().size() + " rules + default)"
                        : "flow '" + binding.flowId() + "'",
                binding.groupId(),
                binding.partition() != null ? ", pinned to partition " + binding.partition() : "",
                binding.schemaEnabled() ? ", schema decode on" : "",
                binding.jsonSerializer() ? ", serializer 'json'" : "",
                binding.taskTtlMs() != null ? ", task ttl " + (binding.taskTtlMs() / 1000) + "s" : "",
                binding.dlqTopic() != null ? ", dlq-topic '" + binding.dlqTopic() + "'" : "",
                binding.autoCommit() ? ", auto-commit on" : "",
                binding.traceIdHeader() != null ? ", trace-id header '" + binding.traceIdHeader() + "'" : "",
                binding.correlationIdHeader() != null
                        ? ", correlation-id header '" + binding.correlationIdHeader() + "'" : "",
                binding.traceparentHeader() != null
                        ? ", traceparent header '" + binding.traceparentHeader() + "'" : "");
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
        if (dlqTopic.equals(topic)) {
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
    private Consumer<String, byte[]> newConsumer(KafkaConsumerBinding binding, RetryPolicy retryPolicy) {
        Properties p = new Properties();
        p.putAll(consumerProps);
        p.setProperty(ConsumerConfig.GROUP_ID_CONFIG, binding.groupId());
        applyDeliveryMode(p, binding);
        applyPollInterval(p, binding, retryPolicy);
        return new KafkaConsumer<>(p);
    }

    /**
     * Guard against poll-thread eviction. Message processing happens ON the poll thread (a flow's own
     * {@code ttl} is its deadline), so the worst-case time between two {@code poll()} calls is the
     * binding's full retry envelope - {@code (maxRetries+1) x} the slowest reachable target ttl
     * {@code + maxRetries x backoff} - times {@code max.poll.records}, plus headroom. If that envelope
     * exceeds Kafka's {@code max.poll.interval.ms} (default 5 minutes), the group coordinator evicts the
     * consumer mid-processing and the subsequent commit fails. This derives the interval from the
     * envelope, never lowering it below the Kafka default. An explicit {@code max.poll.interval.ms} in
     * the consumer template is an operator decision and is respected as-is, with a {@code WARN} when the
     * computed envelope exceeds it. Raising the interval is low-risk: crash liveness is detected by
     * heartbeats/{@code session.timeout.ms} - this setting only bounds time between polls. Visible for
     * testing.
     */
    static void applyPollInterval(Properties p, KafkaConsumerBinding binding, RetryPolicy retryPolicy) {
        long maxPollRecords = Long.parseLong(p.getProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
        long envelopeMs;
        try {
            long perRecordMs = Math.addExact(
                    Math.multiplyExact(retryPolicy.maxRetries() + 1L, maxTargetTtlMs(binding)),
                    Math.multiplyExact((long) retryPolicy.maxRetries(), retryPolicy.backoffMs()));
            envelopeMs = Math.addExact(Math.multiplyExact(perRecordMs, maxPollRecords),
                    POLL_INTERVAL_HEADROOM_MS);
        } catch (ArithmeticException e) {
            envelopeMs = Integer.MAX_VALUE;   // an absurd configuration saturates at the config's int range
        }
        envelopeMs = Math.min(envelopeMs, Integer.MAX_VALUE);
        String explicit = p.getProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG);
        if (explicit != null) {
            warnWhenEnvelopeExceedsExplicitInterval(binding, explicit, envelopeMs);
            return;
        }
        long derived = Math.max(KAFKA_DEFAULT_MAX_POLL_INTERVAL_MS, envelopeMs);
        p.setProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, String.valueOf(derived));
        if (derived > KAFKA_DEFAULT_MAX_POLL_INTERVAL_MS) {
            log.info("Binding '{}' derives max.poll.interval.ms={} from its worst-case retry envelope "
                    + "(slowest target ttl x retries x max.poll.records + headroom)",
                    binding.topicOrPattern(), derived);
        }
    }

    private static void warnWhenEnvelopeExceedsExplicitInterval(KafkaConsumerBinding binding,
                                                                String explicit, long envelopeMs) {
        try {
            if (envelopeMs > Long.parseLong(explicit.trim())) {
                log.warn("Binding '{}' sets max.poll.interval.ms={} but its worst-case retry envelope "
                        + "is {} ms - a slow-failing message may get this consumer evicted from the "
                        + "group mid-processing", binding.topicOrPattern(), explicit.trim(), envelopeMs);
            }
        } catch (NumberFormatException e) {
            // a malformed template value is the Kafka client's to reject with its own config error
        }
    }

    /**
     * The slowest reachable target deadline for a binding: flow targets use the compiled flow's own ttl
     * (every target was validated to exist at construction); {@code task://} targets use the binding's
     * task ttl (default {@value KafkaFlowConsumer#DEFAULT_TASK_TTL_MS} ms).
     */
    private static long maxTargetTtlMs(KafkaConsumerBinding binding) {
        long taskTtl = binding.taskTtlMs() != null ? binding.taskTtlMs() : KafkaFlowConsumer.DEFAULT_TASK_TTL_MS;
        if (binding.routingRules() == null) {
            return flowTtl(binding.flowId(), taskTtl);
        }
        long max = 0;
        for (RoutingRuleSet.Target target : binding.routingRules().allTargets()) {
            max = Math.max(max, target.task() ? taskTtl : flowTtl(target.destination(), taskTtl));
        }
        return max;
    }

    /** A compiled flow's ttl, or the fallback for a flow not in the registry (defensive; validated earlier). */
    private static long flowTtl(String flowId, long fallback) {
        var flow = Flows.getFlow(flowId);
        return flow != null ? flow.ttl : fallback;
    }

    /**
     * Apply the binding's delivery-mode overlay onto a consumer {@link Properties}: {@code enable.auto.commit}
     * exactly as configured, and {@code max.poll.records} either the binding's explicit override or the
     * mode's own default (1 for manual-commit, {@value #AUTO_COMMIT_MAX_POLL_RECORDS} for auto-commit).
     * Visible for testing.
     */
    static void applyDeliveryMode(Properties p, KafkaConsumerBinding binding) {
        p.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(binding.autoCommit()));
        int defaultMaxPoll = binding.autoCommit() ? AUTO_COMMIT_MAX_POLL_RECORDS : MANUAL_COMMIT_MAX_POLL_RECORDS;
        int maxPollRecords = binding.maxPollRecords() != null ? binding.maxPollRecords() : defaultMaxPoll;
        p.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(maxPollRecords));
    }

    @Override
    public void close() {
        consumers.forEach(KafkaFlowConsumer::close);
    }
}
