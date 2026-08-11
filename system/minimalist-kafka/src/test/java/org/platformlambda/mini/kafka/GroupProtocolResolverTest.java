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
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code group.protocol=auto} resolution matrix, pinned against LIVE brokers on both sides of
 * the feature flag: the default embedded broker finalizes {@code group.version=1} (KIP-848 on),
 * and a variant broker is storage-formatted with {@code group.version=0} - the normal state of a
 * cluster whose brokers are upgraded but whose consumer-rebalance-protocol feature has not been
 * enabled. Fallback and guard branches use unreachable bootstrap addresses (nothing listens on
 * the probe ports), so no probe can accidentally succeed.
 */
class GroupProtocolResolverTest {

    private static final String CONSUMER = "consumer";
    private static final String CLASSIC = "classic";
    private static final String DEAD_BOOTSTRAP_1 = "127.0.0.1:65531";
    private static final String DEAD_BOOTSTRAP_2 = "127.0.0.1:65530";
    private static final String E2E_TOPIC = "group-protocol-auto-topic";
    private static final int DEFAULT_PROBE_TIMEOUT_MS = 10_000;

    private static EmbeddedKafka kafka;
    private static EmbeddedKafka classicKafka;

    @BeforeAll
    static void boot() {
        kafka = new EmbeddedKafka();
        classicKafka = new EmbeddedKafka(19094, 19095, "/tmp/mini-kafka-classic",
                Map.of("group.version", (short) 0));
    }

    @AfterAll
    static void shutdown() {
        GroupProtocolResolver.probeTimeoutMs = DEFAULT_PROBE_TIMEOUT_MS;
        GroupProtocolResolver.clearCachedResolutions();
        if (kafka != null) {
            kafka.close();
        }
        if (classicKafka != null) {
            classicKafka.close();
        }
    }

    @BeforeEach
    void isolate() {
        GroupProtocolResolver.probeTimeoutMs = DEFAULT_PROBE_TIMEOUT_MS;
        GroupProtocolResolver.clearCachedResolutions();
    }

    private static Properties autoTemplate(String bootstrap) {
        Properties p = new Properties();
        p.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.setProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "auto");
        return p;
    }

    @Test
    void autoResolvesToConsumerWhenClusterFinalizesGroupVersion() {
        Properties p = autoTemplate(kafka.bootstrapServers());
        GroupProtocolResolver.resolve(p);
        assertEquals(CONSUMER, p.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG));
    }

    @Test
    void autoResolvesToClassicWhenClusterDoesNotFinalizeGroupVersion() {
        Properties p = autoTemplate(classicKafka.bootstrapServers());
        GroupProtocolResolver.resolve(p);
        assertEquals(CLASSIC, p.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG));
    }

    @Test
    void conflictingClientTuningResolvesToClassicWithoutProbing() {
        Properties p = autoTemplate(DEAD_BOOTSTRAP_1);
        p.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "45000");
        long started = System.currentTimeMillis();
        GroupProtocolResolver.resolve(p);
        assertEquals(CLASSIC, p.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG));
        // guard resolution is local - an attempted network probe would spend the full timeout
        assertTrue(System.currentTimeMillis() - started < 2000, "conflict guard must not probe");
        // the operator's tuning stays untouched
        assertEquals("45000", p.getProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG));
    }

    @Test
    void probeFailureResolvesToClassic() {
        GroupProtocolResolver.probeTimeoutMs = 1500;
        Properties p = autoTemplate(DEAD_BOOTSTRAP_2);
        GroupProtocolResolver.resolve(p);
        assertEquals(CLASSIC, p.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG));
    }

    @Test
    void explicitProtocolPassesThroughVerbatimWithoutProbing() {
        Properties p = new Properties();
        p.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DEAD_BOOTSTRAP_1);
        p.setProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG, CONSUMER);
        long started = System.currentTimeMillis();
        GroupProtocolResolver.resolve(p);
        assertEquals(CONSUMER, p.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG));
        assertTrue(System.currentTimeMillis() - started < 2000, "explicit value must not probe");
    }

    @Test
    void absentGroupProtocolIsLeftUntouched() {
        Properties p = new Properties();
        p.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DEAD_BOOTSTRAP_1);
        GroupProtocolResolver.resolve(p);
        assertNull(p.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG));
    }

    @Test
    void secondResolutionForTheSameClusterUsesTheCachedAnswer() {
        Properties first = autoTemplate(kafka.bootstrapServers());
        GroupProtocolResolver.resolve(first);
        assertEquals(CONSUMER, first.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG));
        long started = System.currentTimeMillis();
        Properties second = autoTemplate(kafka.bootstrapServers());
        GroupProtocolResolver.resolve(second);
        assertEquals(CONSUMER, second.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG));
        assertTrue(System.currentTimeMillis() - started < 2000, "cached resolution must not re-probe");
    }

    /**
     * The resolved properties are real: a consumer built from an auto-resolved template joins the
     * group under the KIP-848 consumer protocol and consumes a record end-to-end.
     */
    @Test
    void autoResolvedConsumerProtocolConsumesEndToEnd() throws Exception {
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), E2E_TOPIC);
        Properties p = autoTemplate(kafka.bootstrapServers());
        GroupProtocolResolver.resolve(p);
        assertEquals(CONSUMER, p.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG));
        p.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        p.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "group-protocol-auto-e2e");
        p.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Properties producerProps = new Properties();
        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers());
        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProps);
             Consumer<String, byte[]> consumer = new KafkaConsumer<>(p)) {
            producer.send(new ProducerRecord<>(E2E_TOPIC,
                    "hello kip-848".getBytes(StandardCharsets.UTF_8))).get();
            consumer.subscribe(List.of(E2E_TOPIC));
            String received = null;
            long deadline = System.currentTimeMillis() + 20_000;
            while (received == null && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    received = new String(records.iterator().next().value(), StandardCharsets.UTF_8);
                }
            }
            assertNotNull(received, "record must arrive under the consumer protocol");
            assertEquals("hello kip-848", received);
        }
    }
}
