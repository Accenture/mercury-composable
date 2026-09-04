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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The produce-only / consume-only opt-out (a one-way bridge leg, where the cluster grants credentials
 * for one client only). These cases are pure configuration handling - no broker involved.
 */
class KafkaOptOutTest {

    /** A notification function whose cluster has no producer, as when the producer is switched off. */
    private static SimpleKafkaNotification withoutPublisher() {
        return new SimpleKafkaNotification() {
            @Override
            protected KafkaRequestPublisher publisher() {
                return null;
            }
        };
    }

    private static ConfigReader adapterConfig(Map<String, Object> binding) {
        return new ConfigReader().load(Map.of("consumer", List.of(binding)));
    }

    @Test
    void publishingWithoutAProducerNamesTheSettingThatCausedIt() {
        var notification = withoutPublisher();
        Map<String, String> headers = Map.of(KafkaHeaders.TOPIC, "orders.onprem");
        var e = assertThrows(IllegalStateException.class,
                () -> notification.handleEvent(headers, "payload".getBytes(), 0));
        assertTrue(e.getMessage().contains("kafka.producer.enabled"),
                "the error points at the config, not at a missing route: " + e.getMessage());
    }

    @Test
    void callerMistakesAreReportedAheadOfTheProducerCheck() {
        var notification = withoutPublisher();
        // a caller's own error must never be masked by an environment condition, so every input check
        // runs first - the producer is only required at the publish site
        assertThrows(IllegalArgumentException.class, () -> notification.handleEvent(Map.of(), null, 0),
                "missing topic");
        Map<String, String> schemaPath = Map.of(KafkaHeaders.TOPIC, "orders", KafkaHeaders.SUBJECT, "order-value");
        assertThrows(IllegalArgumentException.class,
                () -> notification.handleEvent(schemaPath, "not a byte array", 0),
                "the schema path's byte[] document contract");
    }

    @Test
    void aDeadLetterTopicWithoutAProducerIsRejectedAtStartup() {
        ConfigReader config = adapterConfig(Map.of("topic", "orders.onprem", "flow", "bridge-to-cloud",
                "dlq-topic", "orders.poison"));
        var e = assertThrows(IllegalArgumentException.class, () ->
                KafkaFlowAdapter.rejectDeadLetterWithoutProducer(config, KafkaClientConfig.PRODUCER_ENABLED));
        assertTrue(e.getMessage().contains("consumer[0]"), e.getMessage());
        assertTrue(e.getMessage().contains("dlq-topic"), e.getMessage());
        assertTrue(e.getMessage().contains("kafka.producer.enabled=false"),
                "both halves of the contradiction are named: " + e.getMessage());
    }

    @Test
    void aConsumeOnlyLegWithoutADeadLetterTopicStarts() {
        ConfigReader config = adapterConfig(Map.of("topic", "orders.onprem", "flow", "bridge-to-cloud"));
        assertDoesNotThrow(() ->
                KafkaFlowAdapter.rejectDeadLetterWithoutProducer(config, KafkaClientConfig.PRODUCER_ENABLED));
    }

    @Test
    void theRejectionNamesTheClusterThatIsMisconfigured() {
        ConfigReader config = adapterConfig(Map.of("topic", "orders.cloud", "flow", "bridge-to-onprem",
                "dlq-topic", "orders.cloud.poison"));
        var e = assertThrows(IllegalArgumentException.class, () ->
                KafkaFlowAdapter.rejectDeadLetterWithoutProducer(config, "secondary.kafka.producer.enabled"));
        assertTrue(e.getMessage().contains("secondary.kafka.producer.enabled=false"),
                "twin-kafka points the operator at its own key: " + e.getMessage());
    }
}
