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

import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.common.ConfigBase;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An empty {@link ConfigReader} is a no-op {@link ConfigBase} (every {@code getProperty(key, default)}
 * returns the default), so these load the default classpath templates (file:/tmp/... falls back to
 * classpath) and assert that the library pins the wire contract over whatever the template says. The
 * delivery-mode overlay ({@code enable.auto.commit} / {@code max.poll.records}) is applied per-binding by
 * {@link KafkaFlowAdapter#applyDeliveryMode}, not here - see {@code KafkaFlowAdapterConfigTest}.
 */
class KafkaClientConfigTest {

    private static final ConfigBase EMPTY = new ConfigReader();

    @Test
    void producerLoadsTemplateAndPinsSerializers() {
        Properties p = KafkaClientConfig.producerProperties(EMPTY);
        assertEquals(StringSerializer.class.getName(), p.getProperty("key.serializer"));
        assertEquals(ByteArraySerializer.class.getName(), p.getProperty("value.serializer"));
        assertEquals("all", p.getProperty("acks"), "non-pinned value comes from the template");
        assertNotNull(p.getProperty("bootstrap.servers"), "connection comes from the template");
    }

    @Test
    void producerDefaultsToSimpleRandomPartitioner() {
        Properties p = KafkaClientConfig.producerProperties(EMPTY);
        assertEquals(SimpleRandomPartitioner.class.getName(), p.getProperty("partitioner.class"),
                "random distribution is the default when the template does not choose a partitioner");
    }

    @Test
    void templatePartitionerOverridesTheRandomDefault() {
        ConfigBase config = new ConfigReader("classpath:/custom-partitioner-config.properties");
        Properties p = KafkaClientConfig.producerProperties(config);
        assertEquals("org.apache.kafka.clients.producer.RoundRobinPartitioner",
                p.getProperty("partitioner.class"), "a template-selected partitioner wins over the default");
    }

    @Test
    void consumerLoadsTemplateAndPinsDeserializers() {
        Properties p = KafkaClientConfig.consumerProperties(EMPTY);
        assertEquals(StringDeserializer.class.getName(), p.getProperty("key.deserializer"));
        assertEquals(ByteArrayDeserializer.class.getName(), p.getProperty("value.deserializer"));
        assertNotNull(p.getProperty("bootstrap.servers"));
    }

    private static ConfigBase config(Map<String, Object> values) {
        return new ConfigReader().load(values);
    }

    @Test
    void bothClientsAreEnabledByDefault() {
        assertTrue(KafkaClientConfig.producerEnabled(EMPTY));
        assertTrue(KafkaClientConfig.consumerEnabled(EMPTY));
    }

    @Test
    void onlyTheLiteralFalseDisablesAClient() {
        assertFalse(KafkaClientConfig.producerEnabled(config(Map.of("kafka.producer.enabled", "false"))));
        assertFalse(KafkaClientConfig.consumerEnabled(config(Map.of("kafka.consumer.enabled", " FALSE "))),
                "case and surrounding blanks are tolerated");
        assertTrue(KafkaClientConfig.producerEnabled(config(Map.of("kafka.producer.enabled", "true"))));
        // a veto, not a trigger: anything that is not 'false' leaves the client on
        assertTrue(KafkaClientConfig.producerEnabled(config(Map.of("kafka.producer.enabled", "no"))));
    }

    @Test
    void eachClusterReadsItsOwnFlag() {
        ConfigBase config = config(Map.of("kafka.producer.enabled", "true",
                "secondary.kafka.producer.enabled", "false"));
        assertTrue(KafkaClientConfig.producerEnabled(config));
        assertFalse(KafkaClientConfig.clientEnabled(config, "secondary.kafka.producer.enabled"),
                "a bridge disables one cluster's producer without touching the other's");
    }

    @Test
    void healthProbeUsesTheConsumerTemplateByDefault() {
        Properties p = KafkaClientConfig.healthProbeProperties(EMPTY);
        assertEquals(StringDeserializer.class.getName(), p.getProperty("key.deserializer"));
        assertNotNull(p.getProperty("auto.offset.reset"), "the consumer template is used verbatim");
    }

    @Test
    void healthProbeFallsBackToTheProducerTemplateOnAProduceOnlyLeg() {
        Properties p = KafkaClientConfig.healthProbeProperties(config(
                Map.of("kafka.consumer.enabled", "false")));
        assertNotNull(p.getProperty("bootstrap.servers"),
                "connection settings are named identically in both client surfaces");
        assertEquals(ByteArrayDeserializer.class.getName(), p.getProperty("value.deserializer"),
                "the probe is still a consumer, so the wire contract is pinned");
        assertNull(p.getProperty("acks"),
                "producer-only settings are filtered out rather than logged as unknown config");
        assertNull(p.getProperty("auto.offset.reset"), "the consumer template was not consulted");
    }

    @Test
    void healthProbeKeepsTheConsumerTemplateWhenBothClientsAreDisabled() {
        Properties p = KafkaClientConfig.healthProbeProperties(config(
                Map.of("kafka.consumer.enabled", "false", "kafka.producer.enabled", "false")));
        assertNotNull(p.getProperty("auto.offset.reset"),
                "an inert module has nothing to probe; the consumer template stays the default");
    }
}
