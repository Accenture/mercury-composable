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
import org.platformlambda.cloud.services.ServiceRegistry;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PubSub;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the topic manager's administrative event API (the CLOUD_MANAGER service behind
 * PubSub) and the consumer's live-subscription paths that the round-trip test does not reach:
 * subscription while messages arrive (no seek), non-zero start offset, and re-subscription.
 */
class KafkaAdminEdgeTest {

    private static final String SYSTEM = "system";
    private static final String CLOUD_CONNECTOR_HEALTH = "cloud.connector.health";
    private static final long TIMEOUT = 10000;

    @BeforeAll
    static void setup() throws InterruptedException {
        AutoStart.main(new String[0]);
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        Platform.getInstance().waitForProvider(CLOUD_CONNECTOR_HEALTH, 90).onSuccess(bench::add);
        assertEquals(Boolean.TRUE, bench.poll(90, TimeUnit.SECONDS));
    }

    private EventEnvelope admin(EventEnvelope event) throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter.getInstance().asyncRequest(event.setTo(ServiceRegistry.CLOUD_MANAGER), TIMEOUT)
                .onSuccess(bench::add);
        return bench.poll(TIMEOUT + 5000, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    @Test
    void topicManagerAdminApi() throws InterruptedException {
        String topic = "admin.edge.demo";
        // create with explicit partition count
        EventEnvelope created = admin(new EventEnvelope()
                .setHeader("type", "create").setHeader("topic", topic).setHeader("partitions", "2"));
        assertNotNull(created);
        assertEquals(true, created.getBody());
        // exists
        EventEnvelope exists = admin(new EventEnvelope()
                .setHeader("type", "exists").setHeader("topic", topic));
        assertNotNull(exists);
        assertEquals(true, exists.getBody());
        // partition count
        EventEnvelope partitions = admin(new EventEnvelope()
                .setHeader("type", "partitions").setHeader("topic", topic));
        assertNotNull(partitions);
        assertEquals(2, partitions.getBody());
        // list contains it
        EventEnvelope list = admin(new EventEnvelope().setHeader("type", "list"));
        assertNotNull(list);
        assertTrue(((List<String>) list.getBody()).contains(topic));
        // delete
        EventEnvelope deleted = admin(new EventEnvelope()
                .setHeader("type", "delete").setHeader("topic", topic));
        assertNotNull(deleted);
        assertEquals(true, deleted.getBody());
        // exists is now false
        EventEnvelope gone = admin(new EventEnvelope()
                .setHeader("type", "exists").setHeader("topic", topic));
        assertNotNull(gone);
        assertEquals(false, gone.getBody());
        // an unrecognized request type falls through and returns false
        EventEnvelope invalid = admin(new EventEnvelope().setHeader("type", "nonsense"));
        assertNotNull(invalid);
        assertEquals(false, invalid.getBody());
        // delete of a non-existent topic is idempotent
        EventEnvelope idempotent = admin(new EventEnvelope()
                .setHeader("type", "delete").setHeader("topic", topic));
        assertNotNull(idempotent);
        assertEquals(true, idempotent.getBody());
    }

    @Test
    void liveSubscriptionAndResubscription() throws Exception {
        PubSub ps = PubSub.getInstance(SYSTEM);
        String topic = "live.subscription.demo";
        ps.createTopic(topic, 1);
        final BlockingQueue<Object> inbox = new ArrayBlockingQueue<>(10);
        LambdaFunction listener = (headers, input, instance) -> {
            inbox.offer(input);
            return true;
        };
        // subscribe from the latest position (no explicit offset), then publish
        ps.subscribe(topic, 0, listener, "edge-client-1", "edge-group-1");
        // allow the consumer to join the group before publishing
        Thread.sleep(3000);
        ps.publish(topic, 0, Map.of("k", "v"), "live message");
        Object delivered = inbox.poll(60, TimeUnit.SECONDS);
        assertEquals("live message", delivered);
        ps.unsubscribe(topic, 0);
        // re-subscribe with an explicit numeric offset to replay from the beginning
        final BlockingQueue<Object> replay = new ArrayBlockingQueue<>(10);
        LambdaFunction replayListener = (headers, input, instance) -> {
            replay.offer(input);
            return true;
        };
        ps.subscribe(topic, 0, replayListener, "edge-client-2", "edge-group-2", "0");
        assertEquals("live message", replay.poll(60, TimeUnit.SECONDS));
        ps.unsubscribe(topic, 0);
        ps.deleteTopic(topic);
        // invalid topic name is rejected by the admin path
        assertThrows(IllegalArgumentException.class, () -> ps.createTopic("nodotname"));
    }

    @Test
    void consumerDispatchEdgeCases() throws Exception {
        Platform platform = Platform.getInstance();
        PubSub ps = PubSub.getInstance(SYSTEM);
        String topic = "dispatch.edge.demo";
        // creating the same topic twice is idempotent
        ps.createTopic(topic, 1);
        assertTrue(ps.createTopic(topic, 1));
        final BlockingQueue<Object> inbox = new ArrayBlockingQueue<>(10);
        LambdaFunction listener = (headers, input, instance) -> {
            inbox.offer(input);
            return true;
        };
        ps.subscribe(topic, 0, listener, "edge-client-3", "edge-group-3", "0");
        // an embedded event addressed to ANOTHER origin is quietly dropped by the consumer
        EventEnvelope stranger = new EventEnvelope().setTo("no.where").setBody("not for me");
        Map<String, String> misdirected = Map.of(
                org.platformlambda.cloud.EventProducer.EMBED_EVENT, "1",
                org.platformlambda.cloud.EventProducer.RECIPIENT, "some-other-origin");
        ps.publish(topic, 0, misdirected, stranger.toBytes());
        // a corrupted embedded event exercises the consumer's exception path
        Map<String, String> embedded = Map.of(
                org.platformlambda.cloud.EventProducer.EMBED_EVENT, "1",
                org.platformlambda.cloud.EventProducer.RECIPIENT, platform.getOrigin());
        ps.publish(topic, 0, embedded, "not-a-packed-event".getBytes());
        // a normal raw message still arrives after the two rejected ones
        ps.publish(topic, 0, null, "survivor");
        assertEquals("survivor", inbox.poll(60, TimeUnit.SECONDS));
        ps.unsubscribe(topic, 0);
        ps.deleteTopic(topic);
    }
}
