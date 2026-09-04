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

package org.platformlambda.twin.kafka;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.common.ConfigBase;
import org.platformlambda.mini.kafka.KafkaClientConfig;
import org.platformlambda.mini.kafka.KafkaHeaders;
import org.platformlambda.mini.kafka.KafkaRequestPublisher;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The secondary cluster's half of the produce-only / consume-only opt-out. The point of these cases is
 * that each cluster is switched independently and diagnoses itself with its OWN key - the bridge
 * topology a one-way leg actually deploys as.
 */
class SecondaryKafkaOptOutTest {

    private static final String SECONDARY_CONSUMER_TEMPLATE = "classpath:/secondary-kafka-consumer.properties";
    private static final String SECONDARY_PRODUCER_TEMPLATE = "classpath:/secondary-kafka-producer.properties";

    private static ConfigBase config(Map<String, Object> values) {
        return new ConfigReader().load(values);
    }

    private static Properties secondaryProbe(ConfigBase config) {
        return KafkaClientConfig.healthProbeProperties(config,
                SecondaryKafkaAutoStart.CONSUMER_ENABLED, "secondary.kafka.consumer.properties",
                SECONDARY_CONSUMER_TEMPLATE,
                SecondaryKafkaAutoStart.PRODUCER_ENABLED, "secondary.kafka.producer.properties",
                SECONDARY_PRODUCER_TEMPLATE);
    }

    @Test
    void publishingWithoutASecondaryProducerNamesTheSecondaryKey() {
        var notification = new SecondaryKafkaNotification() {
            @Override
            protected KafkaRequestPublisher publisher() {
                return null;
            }
        };
        Map<String, String> headers = Map.of(KafkaHeaders.TOPIC, "orders.cloud");
        var e = assertThrows(IllegalStateException.class,
                () -> notification.handleEvent(headers, "payload".getBytes(), 0));
        assertTrue(e.getMessage().contains("secondary.kafka.producer.enabled"),
                "the secondary cluster diagnoses itself, not the primary: " + e.getMessage());
    }

    @Test
    void theSecondaryHealthProbeFallsBackToItsOwnProducerTemplate() {
        Properties p = secondaryProbe(config(Map.of("secondary.kafka.consumer.enabled", "false")));
        assertNotNull(p.getProperty("bootstrap.servers"));
        assertNull(p.getProperty("acks"), "producer-only settings are filtered out");
        assertNull(p.getProperty("auto.offset.reset"), "the secondary consumer template was not consulted");
    }

    @Test
    void disablingThePrimaryConsumerLeavesTheSecondaryProbeAlone() {
        // the bridge shape: consume on the secondary, produce on the primary
        Properties p = secondaryProbe(config(Map.of("kafka.consumer.enabled", "false")));
        assertNotNull(p.getProperty("auto.offset.reset"),
                "the secondary cluster reads secondary.* only - the primary's flag must not leak across");
    }

    @Test
    void theTwoClustersResolveTheirFlagsIndependently() {
        ConfigBase bridge = config(Map.of("kafka.producer.enabled", "false",
                "secondary.kafka.consumer.enabled", "false"));
        // a one-way bridge: consume from the primary, publish to the secondary
        assertEquals(false, KafkaClientConfig.producerEnabled(bridge));
        assertEquals(true, KafkaClientConfig.consumerEnabled(bridge));
        assertEquals(true, KafkaClientConfig.clientEnabled(bridge, SecondaryKafkaAutoStart.PRODUCER_ENABLED));
        assertEquals(false, KafkaClientConfig.clientEnabled(bridge, SecondaryKafkaAutoStart.CONSUMER_ENABLED));
    }
}
