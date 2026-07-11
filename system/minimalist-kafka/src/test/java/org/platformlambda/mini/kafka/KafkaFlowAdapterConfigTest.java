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
import org.platformlambda.core.util.ConfigReader;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
