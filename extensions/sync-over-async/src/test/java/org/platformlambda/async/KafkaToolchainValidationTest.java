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

package org.platformlambda.async;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates the Phase-2 toolchain: the embedded KRaft broker boots on this platform and a {@code
 * String}/{@code byte[]} record - with the {@code traceparent} header the request/response legs rely on
 * - survives a produce/consume round-trip.
 */
class KafkaToolchainValidationTest {

    private static EmbeddedKafka kafka;

    @BeforeAll
    static void startKafka() {
        kafka = new EmbeddedKafka();
    }

    @AfterAll
    static void stopKafka() {
        if (kafka != null) {
            kafka.close();
        }
    }

    @Test
    void produceConsumeRoundTripPreservesHeaders() throws Exception {
        String topic = "validate.topic";
        String bootstrap = kafka.bootstrapServers();
        KafkaTestSupport.createTopic(bootstrap, topic);

        try (Producer<String, byte[]> producer = KafkaTestSupport.newProducer(bootstrap)) {
            ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, "cid-1", "hello".getBytes(UTF_8));
            producerRecord.headers().add("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01".getBytes(UTF_8));
            producer.send(producerRecord).get(10, TimeUnit.SECONDS);
        }

        try (Consumer<String, byte[]> consumer = KafkaTestSupport.newConsumer(bootstrap, "validate-group")) {
            consumer.subscribe(List.of(topic));
            ConsumerRecord<String, byte[]> consumerRecord = pollForKey(consumer, "cid-1");
            assertEquals("hello", new String(consumerRecord.value(), UTF_8));
            Header traceparent = consumerRecord.headers().lastHeader("traceparent");
            assertNotNull(traceparent, "traceparent header must survive the round-trip");
            assertEquals("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01",
                    new String(traceparent.value(), UTF_8));
        }
    }

    private static ConsumerRecord<String, byte[]> pollForKey(Consumer<String, byte[]> consumer, String key) {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, byte[]> consumerRecord : records) {
                if (key.equals(consumerRecord.key())) {
                    return consumerRecord;
                }
            }
        }
        return fail("did not receive a record with key '" + key + "' within the timeout");
    }
}
