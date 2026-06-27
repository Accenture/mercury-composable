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

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaRequestPublisherTest {

    private static MockProducer<String, byte[]> autoCompletingProducer() {
        return new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
    }

    @Test
    void publishSetsTopicHeadersAndBody() {
        MockProducer<String, byte[]> producer = autoCompletingProducer();
        try (KafkaRequestPublisher publisher = new KafkaRequestPublisher(producer)) {
            publisher.publish("topic-1", null,
                    Map.of("cid", "c1".getBytes(UTF_8), "traceparent", "tp".getBytes(UTF_8)),
                    "body".getBytes(UTF_8));
        }
        ProducerRecord<String, byte[]> sent = producer.history().getFirst();
        assertEquals("topic-1", sent.topic());
        assertEquals("body", new String(sent.value(), UTF_8));
        assertEquals("c1", new String(sent.headers().lastHeader("cid").value(), UTF_8));
        assertEquals("tp", new String(sent.headers().lastHeader("traceparent").value(), UTF_8));
    }

    @Test
    void publishHonorsExplicitPartition() {
        MockProducer<String, byte[]> producer = autoCompletingProducer();
        new KafkaRequestPublisher(producer).publish("topic-1", 3, null, "x".getBytes(UTF_8));
        assertEquals(3, producer.history().getFirst().partition());
    }

    @Test
    void publishSyncConfirmsDelivery() throws Exception {
        MockProducer<String, byte[]> producer = autoCompletingProducer();
        try (KafkaRequestPublisher publisher = new KafkaRequestPublisher(producer)) {
            publisher.publishSync("orders.dlq", null,
                    Map.of("dlq.error", "boom".getBytes(UTF_8)), "body".getBytes(UTF_8), 5000);
        }
        ProducerRecord<String, byte[]> sent = producer.history().getFirst();
        assertEquals("orders.dlq", sent.topic());
        assertEquals("body", new String(sent.value(), UTF_8));
        assertEquals("boom", new String(sent.headers().lastHeader("dlq.error").value(), UTF_8));
    }
}
