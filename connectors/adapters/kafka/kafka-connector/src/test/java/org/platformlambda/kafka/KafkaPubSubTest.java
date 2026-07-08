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

package org.platformlambda.kafka;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.cloud.EventProducer;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PubSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test of the kafka connector against an embedded KRaft broker.
 * <p>
 * The kafka-standalone helper is a test dependency, so its MainApplication
 * (sequence 10) boots the embedded broker at 127.0.0.1:9092 automatically.
 * The TestApp (sequence 20) then waits for the broker and connects the platform
 * to the kafka cloud connector. The test application.properties selects
 * cloud.connector=kafka with service.monitor=true, so connectToCloud() runs the
 * real KafkaConnector setup (PubSubManager, TopicManager and EventConsumer).
 */
class KafkaPubSubTest {
    private static final Logger log = LoggerFactory.getLogger(KafkaPubSubTest.class);

    private static final String SYSTEM = "system";
    private static final String CLOUD_CONNECTOR_HEALTH = "cloud.connector.health";

    @BeforeAll
    static void setup() throws Exception {
        AutoStart.main(new String[0]);
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        Platform.getInstance().waitForProvider(CLOUD_CONNECTOR_HEALTH, 90).onSuccess(bench::add);
        Boolean ready = bench.poll(90, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, ready);
        log.info("Kafka connector ready");
    }

    @Test
    void kafkaPubSubRoundTrip() throws Exception {
        Platform platform = Platform.getInstance();
        PubSub ps = PubSub.getInstance(SYSTEM);
        assertTrue(ps.isStreamingPubSub());
        String topic = "round.trip.demo";
        ps.createTopic(topic, 2);
        assertTrue(ps.exists(topic));
        assertEquals(2, ps.partitionCount(topic));
        assertTrue(ps.list().contains(topic));
        assertFalse(ps.exists("no.such.topic"));
        assertEquals(-1, ps.partitionCount("no.such.topic"));
        // collect delivered events - the listener route is the virtual topic name
        final BlockingQueue<EventEnvelope> inbox = new ArrayBlockingQueue<>(10);
        LambdaFunction listener = (headers, input, instance) -> {
            inbox.add(new EventEnvelope().setHeaders(headers).setBody(input));
            return true;
        };
        // publish before subscribing - the consumer will seek to offset 0 and read from the beginning
        ps.publish(topic, 0, null, "hello world");
        ps.publish(topic, 0, Map.of("sender", "unit-test"), Map.of("hello", "world"));
        ps.publish(topic, 0, null, List.of("a", "b"));
        ps.publish(topic, 0, null, "binary".getBytes(StandardCharsets.UTF_8));
        ps.subscribe(topic, 0, listener, "client-100", "group-100", "0");
        List<Object> bodies = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            EventEnvelope event = inbox.poll(60, TimeUnit.SECONDS);
            assertNotNull(event, "expected delivery of message " + i);
            bodies.add(event.getBody());
        }
        assertTrue(bodies.contains("hello world"));
        assertTrue(bodies.contains(Map.of("hello", "world")));
        assertTrue(bodies.contains(List.of("a", "b")));
        assertTrue(bodies.stream().anyMatch(b -> b instanceof byte[] bs &&
                "binary".equals(new String(bs, StandardCharsets.UTF_8))));
        // embedded event round trip over the same topic and partition
        String rx = "embedded.event.rx";
        final BlockingQueue<Object> embedded = new ArrayBlockingQueue<>(1);
        LambdaFunction rxFunction = (headers, input, instance) -> {
            embedded.add(input);
            return true;
        };
        platform.registerPrivate(rx, rxFunction, 1);
        EventEnvelope inner = new EventEnvelope().setTo(rx).setBody("embedded message");
        Map<String, String> embedHeaders = new HashMap<>();
        embedHeaders.put(EventProducer.EMBED_EVENT, "1");
        embedHeaders.put(EventProducer.RECIPIENT, platform.getOrigin());
        ps.publish(topic, 0, embedHeaders, inner.toBytes());
        assertEquals("embedded message", embedded.poll(60, TimeUnit.SECONDS));
        // the same topic and partition cannot be subscribed twice
        var dup = assertThrows(IllegalArgumentException.class, () ->
                ps.subscribe(topic, 0, listener, "client-101", "group-101", "0"));
        assertTrue(dup.getMessage().contains("already subscribed"));
        ps.unsubscribe(topic, 0);
        ps.deleteTopic(topic);
        assertFalse(ps.exists(topic));
    }

    @Test
    void userDomainPubSubIsEnabled() {
        // PubSubSetup (cloud.services=kafka.pubsub) enables the user-domain pub/sub
        PubSub userDomain = PubSub.getInstance("user");
        assertTrue(userDomain.featureEnabled());
        assertTrue(userDomain.isStreamingPubSub());
        assertNotNull(userDomain.list());
    }

    @Test
    void queueApiIsNotImplemented() {
        PubSub ps = PubSub.getInstance(SYSTEM);
        LambdaFunction noOp = (headers, input, instance) -> true;
        Map<String, String> headers = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> ps.createQueue("demo.queue"));
        assertThrows(IllegalArgumentException.class, () -> ps.deleteQueue("demo.queue"));
        assertThrows(IllegalArgumentException.class, () -> ps.send("demo.queue", headers, "x"));
        assertThrows(IllegalArgumentException.class, () -> ps.listen("demo.queue", noOp));
    }

    @Test
    void subscriptionParametersAreValidated() {
        PubSub ps = PubSub.getInstance(SYSTEM);
        LambdaFunction noOp = (headers, input, instance) -> true;
        var missing = assertThrows(IllegalArgumentException.class, () ->
                ps.subscribe("some.topic", 0, noOp, "only-client-id"));
        assertTrue(missing.getMessage().contains("clientId, groupId"));
        var offset = assertThrows(IllegalArgumentException.class, () ->
                ps.subscribe("some.topic", 0, noOp, "client-x", "group-x", "not-a-number"));
        assertTrue(offset.getMessage().contains("offset must be numeric"));
    }
}
