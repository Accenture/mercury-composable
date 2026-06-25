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

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaRequestPublisherTest {

    private static final String TRACE_PARENT = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";

    private static MockProducer<String, byte[]> autoCompletingProducer() {
        return new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
    }

    @Test
    void publishSetsKeyHeadersAndPayload() {
        MockProducer<String, byte[]> producer = autoCompletingProducer();
        try (KafkaRequestPublisher publisher = new KafkaRequestPublisher(producer, "req.topic")) {
            publisher.publish("cid-1", TRACE_PARENT, "body".getBytes(UTF_8));
        }
        List<ProducerRecord<String, byte[]>> sent = producer.history();
        assertEquals(1, sent.size());
        ProducerRecord<String, byte[]> producerRecord = sent.getFirst();
        assertEquals("req.topic", producerRecord.topic());
        assertEquals("cid-1", producerRecord.key());
        assertEquals("body", new String(producerRecord.value(), UTF_8));
        assertEquals("cid-1", new String(producerRecord.headers().lastHeader("cid").value(), UTF_8));
        assertEquals(TRACE_PARENT, new String(producerRecord.headers().lastHeader("traceparent").value(), UTF_8));
    }

    @Test
    void publishOmitsTraceparentWhenBlank() {
        MockProducer<String, byte[]> producer = autoCompletingProducer();
        new KafkaRequestPublisher(producer, "req.topic").publish("cid-2", "  ", "x".getBytes(UTF_8));
        ProducerRecord<String, byte[]> producerRecord = producer.history().getFirst();
        assertNull(producerRecord.headers().lastHeader("traceparent"));
        assertEquals("cid-2", new String(producerRecord.headers().lastHeader("cid").value(), UTF_8));
    }

    @Test
    void publishFailureSurfacesAsIllegalState() {
        // autoComplete=false -> the send future never completes; a tiny timeout forces the failure path.
        MockProducer<String, byte[]> producer =
                new MockProducer<>(false, null, new StringSerializer(), new ByteArraySerializer());
        KafkaRequestPublisher publisher = new KafkaRequestPublisher(producer, "req.topic", Duration.ofMillis(1));
        byte[] payload = "x".getBytes(UTF_8);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> publisher.publish("cid-err", null, payload));
        assertTrue(ex.getMessage().contains("cid-err"));
    }
}
