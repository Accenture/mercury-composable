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

import org.platformlambda.support.SyncOverAsyncConfig;
import org.platformlambda.sync.RedisTestBase;
import org.platformlambda.sync.ReturnRouteCoordinator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof of the Phase-2 async round-trip, minus the REST layer (Phase 3):
 *
 * <pre>
 *   begin(cid) --&gt; Kafka request topic --&gt; MockSystemOfRecord --&gt; Kafka response topic
 *              --&gt; KafkaResponseConsumer --&gt; coordinator.deliver --&gt; Redis return route --&gt; awaitResponse
 * </pre>
 *
 * It runs against a real embedded KRaft broker (Kafka) and a real embedded {@code redis-server} (the
 * {@link RedisTestBase} super-class), and asserts the correlation-id and {@code traceparent} survive the
 * full path.
 */
class SyncOverAsyncKafkaIntegrationTest extends RedisTestBase {

    private static final String REQUEST_TOPIC = "soa.request";
    private static final String RESPONSE_TOPIC = "soa.response";
    private static final String TRACE_PARENT = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
    private static final SyncOverAsyncConfig CONFIG = new SyncOverAsyncConfig("svc-return", 90, 30, 100);

    private static EmbeddedKafka kafka;

    @BeforeAll
    static void startKafka() throws Exception {
        kafka = new EmbeddedKafka();
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), REQUEST_TOPIC);
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), RESPONSE_TOPIC);
    }

    @AfterAll
    static void stopKafka() {
        if (kafka != null) {
            kafka.close();
        }
    }

    @Test
    void requestIsProcessedAsyncAndResponseRoutesBackSynchronously() throws Exception {
        String bootstrap = kafka.bootstrapServers();
        try (ReturnRouteCoordinator coordinator = new ReturnRouteCoordinator(redisClient, "pod-A", CONFIG);
             KafkaResponseConsumer responseConsumer = new KafkaResponseConsumer(
                     KafkaTestSupport.newConsumer(bootstrap, "soa-return"), RESPONSE_TOPIC, coordinator::deliver);
             MockSystemOfRecord systemOfRecord = new MockSystemOfRecord(bootstrap, REQUEST_TOPIC, RESPONSE_TOPIC);
             KafkaRequestPublisher publisher =
                     new KafkaRequestPublisher(KafkaTestSupport.newProducer(bootstrap), REQUEST_TOPIC)) {

            coordinator.start();
            responseConsumer.start();
            systemOfRecord.start();

            String cid = UUID.randomUUID().toString().replace("-", "");
            CompletableFuture<String> future = coordinator.begin(cid);

            publisher.publish(cid, TRACE_PARENT, "{\"action\":\"create\"}".getBytes(UTF_8));

            String response = coordinator.awaitResponse(cid, future, 20_000);
            assertTrue(response.contains(cid), "response carries the correlation-id: " + response);
            assertTrue(response.contains(TRACE_PARENT),
                    "traceparent propagated end-to-end across Kafka + Redis: " + response);
            assertEquals(0, coordinator.pendingCount(), "pending entry cleaned up after delivery");
        }
    }
}
