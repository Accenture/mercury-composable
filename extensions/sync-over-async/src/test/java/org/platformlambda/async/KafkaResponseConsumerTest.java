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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.platformlambda.support.ResponseDelivery;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit-tests the record decoding/routing in isolation by calling {@code handleRecord} directly - the
 * poll loop and lifecycle are exercised against a real broker in the integration test.
 */
class KafkaResponseConsumerTest {

    private static KafkaResponseConsumer consumerWith(ResponseDelivery delivery) {
        // The MockConsumer only satisfies the constructor; handleRecord does not touch it.
        return new KafkaResponseConsumer(new MockConsumer<>("earliest"), "resp.topic", delivery);
    }

    private static ConsumerRecord<String, byte[]> newRecord(String key, String payload) {
        return new ConsumerRecord<>("resp.topic", 0, 0L, key, payload == null ? null : payload.getBytes(UTF_8));
    }

    @Test
    void deliversUsingCidHeader() {
        AtomicReference<String> cid = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        try (KafkaResponseConsumer consumer = consumerWith((c, p) -> {
            cid.set(c);
            body.set(p);
            return true;
        })) {
            ConsumerRecord<String, byte[]> consumerRecord = newRecord("ignored-key", "{\"status\":\"200\"}");
            consumerRecord.headers().add("cid", "cid-1".getBytes(UTF_8));
            consumer.handleRecord(consumerRecord);
        }
        assertEquals("cid-1", cid.get());
        assertEquals("{\"status\":\"200\"}", body.get());
    }

    @Test
    void fallsBackToRecordKeyWhenNoCidHeader() {
        AtomicReference<String> cid = new AtomicReference<>();
        try (KafkaResponseConsumer consumer = consumerWith((c, p) -> {
            cid.set(c);
            return true;
        })) {
            consumer.handleRecord(newRecord("cid-from-key", "x"));
        }
        assertEquals("cid-from-key", cid.get());
    }

    @Test
    void dropsRecordWithoutCorrelationId() {
        AtomicBoolean delivered = new AtomicBoolean(false);
        try (KafkaResponseConsumer consumer = consumerWith((c, p) -> {
            delivered.set(true);
            return true;
        })) {
            consumer.handleRecord(newRecord(null, "x"));   // no key and no cid header
        }
        assertFalse(delivered.get(), "a response without a correlation-id must be dropped");
    }

    @Test
    void orphanResponseIsHandledQuietly() {
        try (KafkaResponseConsumer consumer = consumerWith((c, p) -> false)) {   // no return route -> orphan
            assertDoesNotThrow(() -> consumer.handleRecord(newRecord("cid-orphan", "x")));
        }
    }
}
