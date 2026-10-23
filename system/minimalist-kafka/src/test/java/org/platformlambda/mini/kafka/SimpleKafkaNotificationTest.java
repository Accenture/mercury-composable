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
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.spi.InvocationCarrier;
import org.platformlambda.core.spi.InvocationInfo;
import org.platformlambda.core.spi.InvocationObserver;
import org.platformlambda.core.spi.InvocationObservers;
import org.platformlambda.core.spi.InvocationScope;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the notification function's body handling - the outbound symmetry of the flow
 * adapter's inbound {@code serializer: 'json'}: a Map or List body auto-serializes to JSON bytes,
 * byte[] passes through verbatim, null stays null (a Kafka tombstone). The publishing path itself is
 * proven end-to-end in {@link KafkaFlowAdapterTest}.
 */
class SimpleKafkaNotificationTest {

    @Test
    void anObserversTagsCrossKafkaAsHeaders() {
        String tagHeader = KafkaHeaders.OBSERVER_TAG_PREFIX + "dt_tag";
        MockProducer<String, byte[]> producer =
                new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
        InvocationObserver observer = new InvocationObserver() {
            @Override
            public InvocationScope begin(InvocationInfo info, InvocationCarrier carrier) {
                return null;
            }

            @Override
            public void send(InvocationInfo info, InvocationCarrier carrier) {
                carrier.set("dt_tag", "tag-for-" + info.route());
            }
        };
        Map<String, String> headers = new HashMap<>(Map.of(KafkaHeaders.TOPIC, "orders",
                "my_trace_id", "trace-401", "my_trace_path", "POST /api/orders"));
        // a tag mapped in from an earlier message is stale: this hop exports its own, or none
        headers.put(tagHeader, "stale");
        try (KafkaRequestPublisher publisher = new KafkaRequestPublisher(producer)) {
            SimpleKafkaNotification notification = new SimpleKafkaNotification() {
                @Override
                protected KafkaRequestPublisher publisher() {
                    return publisher;
                }
            };
            InvocationObservers.register(observer);
            try {
                notification.handleEvent(headers, Map.of("item", "widget"), 1).block();
            } finally {
                InvocationObservers.unregister(observer);
            }
            notification.handleEvent(headers, Map.of("item", "widget"), 1).block();
        }

        Header observed = producer.history().get(0).headers().lastHeader(tagHeader);
        assertEquals("tag-for-orders", new String(observed.value(), StandardCharsets.UTF_8));
        assertNull(producer.history().get(1).headers().lastHeader(tagHeader),
                "without an observer, the stale tag is not forwarded");
    }

    @Test
    void mapAndListBodiesAutoSerializeToJsonBytes() {
        // assert the round-trip semantically - the customized Gson pretty-prints, and consumers
        // (incl. the flow adapter's serializer: 'json') are whitespace-insensitive by nature
        byte[] fromMap = SimpleKafkaNotification.toBytes(Map.of("hello", "world"));
        assertEquals(Map.of("hello", "world"),
                SimpleMapper.getInstance().getMapper().readValue(fromMap, Map.class));
        byte[] fromList = SimpleKafkaNotification.toBytes(List.of(Map.of("type", "order")));
        assertEquals(List.of(Map.of("type", "order")),
                SimpleMapper.getInstance().getMapper().readValue(fromList, List.class));
    }

    @Test
    void byteArrayPassesThroughVerbatimAndNullStaysNull() {
        byte[] raw = "raw".getBytes(StandardCharsets.UTF_8);
        assertSame(raw, SimpleKafkaNotification.toBytes(raw));
        assertNull(SimpleKafkaNotification.toBytes(null), "null body stays null - a Kafka tombstone");
    }

    @Test
    void rejectsUnsupportedBodyTypesLoudly() {
        assertThrows(IllegalArgumentException.class, () -> SimpleKafkaNotification.toBytes("a string"));
        assertThrows(IllegalArgumentException.class, () -> SimpleKafkaNotification.toBytes(42));
    }

    @Test
    void schemaPathKeepsItsByteArrayDocumentContract() {
        // the JSON auto-serialization applies to NON-schema topics only: with a 'subject' header the
        // body must be a byte[] JSON document, exactly as before - a Map is rejected loudly
        SimpleKafkaNotification notification = new SimpleKafkaNotification();
        Map<String, String> headers = Map.of(KafkaHeaders.TOPIC, "t1", KafkaHeaders.SUBJECT, "t1-value",
                "my_route", "unit.test", "my_trace_id", "trace-1", "my_trace_path", "TEST /unit");
        Map<String, Object> mapBody = Map.of("hello", "world");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> notification.handleEvent(headers, mapBody, 1));
        assertTrue(ex.getMessage().contains("body must be byte[]"), ex.getMessage());
    }
}
