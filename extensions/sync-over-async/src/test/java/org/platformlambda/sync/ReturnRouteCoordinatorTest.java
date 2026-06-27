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

package org.platformlambda.sync;

import io.lettuce.core.api.StatefulRedisConnection;
import org.platformlambda.support.SyncOverAsyncConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the cross-pod return path: {@code podA} holds the (would-be) HTTP connection, {@code podB}
 * consumes the response and must route it back to {@code podA} purely through Redis - it never touches
 * {@code podA}'s in-memory pending map.
 */
class ReturnRouteCoordinatorTest extends RedisTestBase {

    private static final String RESPONSE = "{\"status\":\"200\",\"payload\":{\"result\":\"accepted\"}}";
    private static final SyncOverAsyncConfig CONFIG = new SyncOverAsyncConfig("svc-return", 90, 30, 100);

    private ReturnRouteCoordinator podA;   // originating pod (waiting on the response)
    private ReturnRouteCoordinator podB;   // pod that consumed the Kafka response

    @BeforeEach
    void setup() {
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            c.sync().flushall();
        }
        podA = new ReturnRouteCoordinator(redisClient, "pod-A", CONFIG);
        podB = new ReturnRouteCoordinator(redisClient, "pod-B", CONFIG);
        podA.start();
        podB.start();
    }

    @AfterEach
    void teardown() {
        podA.close();
        podB.close();
    }

    private static String newCid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    void crossPodReturnRoutesResponseToOriginatingPod() throws Exception {
        String cid = newCid();
        CompletableFuture<String> future = podA.begin(cid);     // POD-1 registers + publishes route
        assertTrue(podB.deliver(cid, RESPONSE));                // POD-2 stores response + notifies POD-1
        assertEquals(RESPONSE, podA.awaitResponse(cid, future, 5000));
        assertEquals(0, podA.pendingCount());
        // success path cleans up the Redis keys instead of waiting out the TTL
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            ReturnRouteStore store = new ReturnRouteStore(c);
            assertNull(store.getRoute(cid), "route key deleted after a successful rendezvous");
            assertNull(store.getResponse(cid), "response key deleted after a successful rendezvous");
        }
    }

    @Test
    void startTwiceIsRejected() {
        assertThrows(IllegalStateException.class, () -> podA.start());   // already started in setup()
    }

    @Test
    void timesOutWhenNoResponse() {
        String cid = newCid();
        CompletableFuture<String> future = podA.begin(cid);
        assertThrows(TimeoutException.class, () -> podA.awaitResponse(cid, future, 400));
        assertEquals(0, podA.pendingCount(), "pending entry cleaned up on timeout");
    }

    @Test
    void missedNotificationRecoveredByFinalRead() throws Exception {
        String cid = newCid();
        CompletableFuture<String> future = podA.begin(cid);
        // simulate a lost wake-up: the response is written to Redis, but NO notification is published
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            new ReturnRouteStore(c).saveResponse(cid, RESPONSE, 30);
        }
        // the subscriber never fires; awaitResponse times out, then the final read recovers the payload
        assertEquals(RESPONSE, podA.awaitResponse(cid, future, 400));
    }

    @Test
    void orphanResponseWhenNoRoute() {
        String cid = newCid();
        // POD-1 never began this request -> no route; the responder reports orphan (but still stores it)
        assertFalse(podB.deliver(cid, RESPONSE));
    }

    @Test
    void duplicateDeliveryCompletesOnce() throws Exception {
        String cid = newCid();
        CompletableFuture<String> future = podA.begin(cid);
        assertTrue(podB.deliver(cid, RESPONSE));
        assertTrue(podB.deliver(cid, RESPONSE));                // duplicate notification is harmless
        assertEquals(RESPONSE, podA.awaitResponse(cid, future, 5000));
        assertEquals(0, podA.pendingCount());
    }
}
