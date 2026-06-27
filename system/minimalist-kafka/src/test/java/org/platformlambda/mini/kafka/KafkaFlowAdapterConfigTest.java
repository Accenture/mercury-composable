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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validates that the flow adapter rejects malformed {@code consumer} config loudly (fail-fast) instead
 * of silently skipping bad entries. Each bad entry is the first/only one, so the adapter throws during
 * validation before any real {@code KafkaConsumer} is opened.
 */
class KafkaFlowAdapterConfigTest {

    private static final RetryPolicy POLICY = new RetryPolicy(0, 0, ".dlq", null);

    private static ConfigReader config(Object consumerSection) {
        ConfigReader reader = new ConfigReader();
        reader.load(Map.of("consumer", consumerSection));
        return reader;
    }

    private static void build(ConfigReader config) {
        // consumer props are unused: every case here fails validation before any consumer is built
        new KafkaFlowAdapter(new Properties(), config, 1000, POLICY);
    }

    @Test
    void rejectsMissingConsumerList() {
        ConfigReader reader = new ConfigReader();
        reader.load(Map.of("something.else", "x"));
        assertThrows(IllegalArgumentException.class, () -> build(reader));
    }

    @Test
    void rejectsEmptyConsumerList() {
        assertThrows(IllegalArgumentException.class, () -> build(config(List.of())));
    }

    @Test
    void rejectsNonMapEntry() {
        assertThrows(IllegalArgumentException.class, () -> build(config(List.of("just-a-string"))));
    }

    @Test
    void rejectsEntryMissingTopic() {
        assertThrows(IllegalArgumentException.class, () -> build(config(List.of(Map.of("flow", "f")))));
    }

    @Test
    void rejectsEntryMissingFlow() {
        assertThrows(IllegalArgumentException.class, () -> build(config(List.of(Map.of("topic", "t")))));
    }

    @Test
    void rejectsBlankTopic() {
        assertThrows(IllegalArgumentException.class,
                () -> build(config(List.of(Map.of("topic", "  ", "flow", "f")))));
    }

    @Test
    void usesExplicitGroupExactly() {
        assertEquals("sales-order-group",
                KafkaFlowAdapter.resolveGroupId(
                        Map.of("topic", "orders", "flow", "f", "group", "sales-order-group"), "orders"),
                "an administratively-assigned group id is used verbatim, no suffix");
    }

    @Test
    void defaultsGroupPerTopicWhenOmitted() {
        assertEquals("kafka-flow-adapter.orders",
                KafkaFlowAdapter.resolveGroupId(Map.of("topic", "orders", "flow", "f"), "orders"));
    }

    @Test
    void blankGroupFallsBackToDefault() {
        assertEquals("kafka-flow-adapter.orders",
                KafkaFlowAdapter.resolveGroupId(Map.of("topic", "orders", "flow", "f", "group", "  "), "orders"));
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
