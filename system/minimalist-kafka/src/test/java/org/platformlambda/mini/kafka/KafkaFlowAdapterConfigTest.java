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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.platformlambda.core.util.ConfigReader;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates that the flow adapter rejects malformed {@code consumer} config loudly (fail-fast) instead
 * of silently skipping bad entries. Each bad entry is the first/only one, so the adapter throws during
 * validation before any real {@code KafkaConsumer} is opened.
 */
class KafkaFlowAdapterConfigTest {

    private static final RetryPolicy POLICY = new RetryPolicy(0, 0, null);

    private static ConfigReader config(Object consumerSection) {
        ConfigReader reader = new ConfigReader();
        reader.load(Map.of("consumer", consumerSection));
        return reader;
    }

    // S2095: the constructor throws during config validation, so no adapter escapes to be closed (and if one ever did, the assertThrows would fail).
    @SuppressWarnings({"java:S2095", "resource"})
    private static void build(ConfigReader config) {
        // consumer props are unused: every case here fails validation before any consumer is built
        new KafkaFlowAdapter(new Properties(), config, 1000, POLICY, null);
    }

    @Test
    void rejectsMissingConsumerList() {
        ConfigReader reader = new ConfigReader();
        reader.load(Map.of("something.else", "x"));
        assertThrows(IllegalArgumentException.class, () -> build(reader));
    }

    @Test
    void nestedTextReadsNormalizedAndFlatForms() {
        // ConfigReader normalizes a dotted key such as trace.id.header into nested maps
        // keyed by its segments (trace, then id, then header)
        Map<String, Object> normalized = Map.of("trace", Map.of("id", Map.of("header", "X-Legacy-Trace")));
        assertEquals("X-Legacy-Trace", KafkaFlowAdapter.nestedText(normalized, "trace.id.header"));
        // a flat key (programmatically authored map) is accepted too
        Map<String, Object> flat = Map.of("correlation.id.header", "X-Correlation-ID");
        assertEquals("X-Correlation-ID", KafkaFlowAdapter.nestedText(flat, "correlation.id.header"));
        // absent -> null; a non-map intermediate node -> null (not an exception)
        assertNull(KafkaFlowAdapter.nestedText(Map.of("topic", "t1"), "trace.id.header"));
        assertNull(KafkaFlowAdapter.nestedText(Map.of("trace", "not-a-map"), "trace.id.header"));
    }

    @Test
    void rejectsEmptyConsumerList() {
        ConfigReader config = config(List.of());
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsNonMapEntry() {
        ConfigReader config = config(List.of("just-a-string"));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsEntryMissingTopic() {
        ConfigReader config = config(List.of(Map.of("flow", "f")));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsBothTopicAndTopicPattern() {
        ConfigReader config = config(List.of(Map.of("topic", "orders", "topic-pattern", "orders-.*",
                "flow", "f", "group", "g")));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsPatternWithPartition() {
        ConfigReader config = config(List.of(Map.of("topic-pattern", "events\\.[a-z]{2}",
                "flow", "f", "group", "g", "partition", 0)));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsInvalidRegexPattern() {
        ConfigReader config = config(List.of(Map.of("topic-pattern", "events\\.[a-z", "flow", "f", "group", "g")));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsPatternWithoutExplicitGroup() {
        ConfigReader config = config(List.of(Map.of("topic-pattern", "events\\.[a-z]{2}", "flow", "f")));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsDlqTopicEqualToSourceTopic() {
        ConfigReader config = config(List.of(Map.of("topic", "orders", "flow", "f", "dlq-topic", "orders")));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsDlqTopicMatchingSourcePattern() {
        ConfigReader config = config(List.of(Map.of("topic-pattern", "events\\.[a-z]{2}", "flow", "f",
                "group", "g", "dlq-topic", "events.de")));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsNonPositiveMaxPollRecords() {
        ConfigReader config = config(List.of(Map.of("topic", "orders", "flow", "f", "max-poll-records", 0)));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsEntryMissingFlow() {
        ConfigReader config = config(List.of(Map.of("topic", "t")));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void rejectsBlankTopic() {
        ConfigReader config = config(List.of(Map.of("topic", "  ", "flow", "f")));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void usesExplicitGroupExactly() {
        assertEquals("sales-order-group",
                KafkaFlowAdapter.resolveGroupId(
                        Map.of("topic", "orders", "flow", "f", "group", "sales-order-group"), "orders", false),
                "an administratively-assigned group id is used verbatim, no suffix");
    }

    @Test
    void defaultsGroupPerTopicWhenOmitted() {
        assertEquals("kafka-flow-adapter.orders",
                KafkaFlowAdapter.resolveGroupId(Map.of("topic", "orders", "flow", "f"), "orders", false));
    }

    @Test
    void blankGroupFallsBackToDefault() {
        assertEquals("kafka-flow-adapter.orders",
                KafkaFlowAdapter.resolveGroupId(
                        Map.of("topic", "orders", "flow", "f", "group", "  "), "orders", false));
    }

    @Test
    void patternBindingRequiresExplicitGroup() {
        var binding = Map.of("topic-pattern", "events\\.[a-z]{2}", "flow", "f");
        assertThrows(IllegalArgumentException.class,
                () -> KafkaFlowAdapter.resolveGroupId(binding, null, true));
    }

    @Test
    void patternBindingUsesExplicitGroupExactly() {
        assertEquals("region-events-group", KafkaFlowAdapter.resolveGroupId(
                Map.of("topic-pattern", "events\\.[a-z]{2}", "flow", "f", "group", "region-events-group"),
                null, true));
    }

    @Test
    void resolvesExplicitDlqTopic() {
        assertEquals("orders-poison", KafkaFlowAdapter.resolveDlqTopic(
                Map.of("dlq-topic", "orders-poison"), 0, "topic 'orders'", "orders", null));
    }

    @Test
    void absentDlqTopicIsNull() {
        assertNull(KafkaFlowAdapter.resolveDlqTopic(Map.of(), 0, "topic 'orders'", "orders", null));
    }

    @Test
    void isAutoCommitParsesBooleanFlag() {
        assertTrue(KafkaFlowAdapter.isAutoCommit(Map.of("auto-commit", "true")));
        assertFalse(KafkaFlowAdapter.isAutoCommit(Map.of()));
        assertFalse(KafkaFlowAdapter.isAutoCommit(Map.of("auto-commit", "false")));
    }

    @Test
    void parsesMaxPollRecordsWhenPresent() {
        assertEquals(500, KafkaFlowAdapter.parseMaxPollRecords("500"));
        assertEquals(500, KafkaFlowAdapter.parseMaxPollRecords(500));
    }

    @Test
    void absentMaxPollRecordsIsNull() {
        assertNull(KafkaFlowAdapter.parseMaxPollRecords(null));
    }

    @Test
    void rejectsNonIntegerMaxPollRecords() {
        assertThrows(IllegalArgumentException.class, () -> KafkaFlowAdapter.parseMaxPollRecords("abc"));
    }

    @Test
    void manualCommitModeDefaultsMaxPollRecordsToOne() {
        Properties p = new Properties();
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("orders").flowId("f").build();
        KafkaFlowAdapter.applyDeliveryMode(p, binding);
        assertEquals("false", p.getProperty("enable.auto.commit"));
        assertEquals("1", p.getProperty("max.poll.records"));
    }

    @Test
    void autoCommitModeDefaultsMaxPollRecordsTo500() {
        Properties p = new Properties();
        KafkaConsumerBinding binding =
                KafkaConsumerBinding.builder().topic("orders").flowId("f").autoCommit(true).build();
        KafkaFlowAdapter.applyDeliveryMode(p, binding);
        assertEquals("true", p.getProperty("enable.auto.commit"));
        assertEquals("500", p.getProperty("max.poll.records"));
    }

    @Test
    void explicitMaxPollRecordsOverridesModeDefault() {
        Properties p = new Properties();
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("orders").flowId("f")
                .autoCommit(true).maxPollRecords(50).build();
        KafkaFlowAdapter.applyDeliveryMode(p, binding);
        assertEquals("50", p.getProperty("max.poll.records"));
    }

    // ---- max.poll.interval.ms derivation (the poll-thread eviction guard) ----
    // Flow 'f' is not in the compiled Flows registry in this unit context, so the derivation falls
    // back to the binding's task ttl (default 30000) - the same fallback resolveTtl uses at runtime.

    @Test
    void smallRetryEnvelopeKeepsThePollIntervalAtKafkaDefault() {
        Properties p = new Properties();
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("orders").flowId("f").build();
        KafkaFlowAdapter.applyDeliveryMode(p, binding);   // manual commit -> max.poll.records = 1
        KafkaFlowAdapter.applyPollInterval(p, binding, new RetryPolicy(3, 500, null));
        // envelope = 4 x 30000 + 3 x 500 + 10000 headroom = 131500 < the 300000 floor
        assertEquals("300000", p.getProperty("max.poll.interval.ms"));
    }

    @Test
    void derivesPollIntervalFromTheRetryEnvelopeWhenItExceedsTheDefault() {
        Properties p = new Properties();
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("orders").flowId("f")
                .taskTtlMs(120000L).build();
        KafkaFlowAdapter.applyDeliveryMode(p, binding);
        KafkaFlowAdapter.applyPollInterval(p, binding, new RetryPolicy(3, 500, null));
        // (3+1) x 120000 + 3 x 500 + 10000 headroom = 491500 - the coordinator no longer evicts a
        // consumer that is legitimately waiting out its retry envelope
        assertEquals("491500", p.getProperty("max.poll.interval.ms"));
    }

    @Test
    void autoCommitBatchSizeMultipliesThePollIntervalEnvelope() {
        Properties p = new Properties();
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("orders").flowId("f")
                .autoCommit(true).maxPollRecords(100).build();
        KafkaFlowAdapter.applyDeliveryMode(p, binding);
        KafkaFlowAdapter.applyPollInterval(p, binding, new RetryPolicy(0, 0, null));
        // a poll batch is processed sequentially on the poll thread: 100 x 30000 + 10000 = 3010000
        assertEquals("3010000", p.getProperty("max.poll.interval.ms"));
    }

    @Test
    void explicitPollIntervalFromTheConsumerTemplateIsRespected() {
        Properties p = new Properties();
        p.setProperty("max.poll.interval.ms", "60000");
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("orders").flowId("f")
                .taskTtlMs(120000L).build();
        KafkaFlowAdapter.applyDeliveryMode(p, binding);
        KafkaFlowAdapter.applyPollInterval(p, binding, new RetryPolicy(3, 500, null));
        // an operator's explicit value is a decision, not a bug - kept as-is (with a WARN in the log)
        assertEquals("60000", p.getProperty("max.poll.interval.ms"));
    }

    @Test
    void oversizedPollIntervalEnvelopeClampsToIntegerRange() {
        Properties p = new Properties();
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("orders").flowId("f")
                .taskTtlMs(Long.MAX_VALUE / 4).build();
        KafkaFlowAdapter.applyDeliveryMode(p, binding);
        KafkaFlowAdapter.applyPollInterval(p, binding, new RetryPolicy(3, 500, null));
        // max.poll.interval.ms is an int config - the derivation must not overflow past it
        assertEquals(String.valueOf(Integer.MAX_VALUE), p.getProperty("max.poll.interval.ms"));
    }

    @Test
    void parsesPartitionWhenPresent() {
        assertEquals(3, KafkaFlowAdapter.parsePartition("3"));
        assertEquals(0, KafkaFlowAdapter.parsePartition(0));   // numeric YAML value
    }

    @Test
    void absentOrBlankPartitionIsNull() {
        assertNull(KafkaFlowAdapter.parsePartition(null));
        assertNull(KafkaFlowAdapter.parsePartition("  "));
    }

    @Test
    void rejectsNonIntegerPartition() {
        assertThrows(IllegalArgumentException.class, () -> KafkaFlowAdapter.parsePartition("abc"));
    }

    @Test
    void rejectsNegativePartition() {
        assertThrows(IllegalArgumentException.class, () -> KafkaFlowAdapter.parsePartition("-1"));
    }

    @Test
    void rejectsBothFlowAndFlows() {
        ConfigReader config = config(List.of(Map.of("topic", "orders", "flow", "f",
                "flows", List.of("default -> flow://f"))));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    /**
     * The second-level routing and serializer validation rejections share one shape
     * (a bad consumer entry -> fail-fast at build), so they run as one parameterized
     * test with named cases. The unknown-flow case works because no flow is compiled
     * in this test JVM - any flow:// target fails the cross-reference check.
     */
    private static Stream<Arguments> invalidRoutingOrSerializerEntries() {
        return Stream.of(
                Arguments.of("flows is not a list",
                        Map.of("topic", "orders", "flows", "default -> flow://f")),
                Arguments.of("empty flows list",
                        Map.of("topic", "orders", "flows", List.of())),
                Arguments.of("malformed routing rule",
                        Map.of("topic", "orders",
                                "flows", List.of("this is not a rule", "default -> flow://f"))),
                Arguments.of("flows without a default rule",
                        Map.of("topic", "orders",
                                "flows", List.of("input.header.type(order) -> flow://order-flow"))),
                Arguments.of("routing rule referencing an unknown flow",
                        Map.of("topic", "orders",
                                "flows", List.of("default -> flow://no-such-flow"))),
                Arguments.of("unsupported serializer",
                        Map.of("topic", "orders", "flow", "f", "serializer", "xml")),
                Arguments.of("serializer combined with schema decoding",
                        Map.of("topic", "orders", "flow", "f",
                                "serializer", "json", "schema.enabled", "true")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRoutingOrSerializerEntries")
    void rejectsInvalidRoutingOrSerializerEntry(String name, Map<String, Object> entry) {
        ConfigReader config = config(List.of(entry));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void isJsonSerializerParsesTheFlag() {
        assertTrue(KafkaFlowAdapter.isJsonSerializer(Map.of("serializer", "json"), 0, "topic 'x'", false));
        assertTrue(KafkaFlowAdapter.isJsonSerializer(Map.of("serializer", "JSON"), 0, "topic 'x'", false));
        assertFalse(KafkaFlowAdapter.isJsonSerializer(Map.of(), 0, "topic 'x'", false));
    }

    @Test
    void parseTaskTtlAcceptsDurationSyntax() {
        assertEquals(30000L, KafkaFlowAdapter.parseTaskTtl("30s"));
        assertEquals(300000L, KafkaFlowAdapter.parseTaskTtl("5m"));
        assertEquals(20000L, KafkaFlowAdapter.parseTaskTtl(20));   // bare number = seconds
        assertNull(KafkaFlowAdapter.parseTaskTtl(null));
        assertNull(KafkaFlowAdapter.parseTaskTtl("  "));
    }

    @Test
    void rejectsNonPositiveOrMalformedTaskTtl() {
        assertThrows(IllegalArgumentException.class, () -> KafkaFlowAdapter.parseTaskTtl("0"));
        assertThrows(IllegalArgumentException.class, () -> KafkaFlowAdapter.parseTaskTtl("abc"));
        assertThrows(IllegalArgumentException.class, () -> KafkaFlowAdapter.parseTaskTtl("-5s"));
    }

    @Test
    void taskTtlUsesLongMathWithNoSilentWrap() {
        // integer arithmetic would wrap '50000d' (4.32e9 seconds) to a silently WRONG positive value.
        // long math honors an absurd-but-accepted duration exactly as written.
        assertEquals(50000L * 86400 * 1000, KafkaFlowAdapter.parseTaskTtl("50000d"));
        assertEquals(86400000L, KafkaFlowAdapter.parseTaskTtl("1d"));
        assertEquals(3600000L, KafkaFlowAdapter.parseTaskTtl("1h"));
    }

    @Test
    void rejectsFlowEngineAsTaskRoute() {
        // the flow engine is a registered route, but a bare task envelope carries no flow_id -
        // flows are dispatched with flow:// only (rejected before any Platform lookup)
        ConfigReader config = config(List.of(Map.of("topic", "orders",
                "flows", List.of("default -> task://event.script.manager"))));
        assertThrows(IllegalArgumentException.class, () -> build(config));
    }

    @Test
    void resolveRoutingReturnsNullForDirectFlowBinding() {
        assertNull(KafkaFlowAdapter.resolveRouting(0, "topic 'x'", Map.of("topic", "x", "flow", "f"), "f"));
    }

    @Test
    void resolveRoutingCompilesTheRuleList() {
        RoutingRuleSet rules = KafkaFlowAdapter.resolveRouting(0, "topic 'x'",
                Map.of("flows", List.of("input.header.type(a) -> flow://a-flow", "default -> flow://d-flow")),
                null);
        assertNotNull(rules);
        assertEquals(1, rules.size());
    }
}
