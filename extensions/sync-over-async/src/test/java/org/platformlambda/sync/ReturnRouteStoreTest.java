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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnRouteStoreTest extends RedisTestBase {

    private StatefulRedisConnection<String, String> connection;
    private ReturnRouteStore store;

    @BeforeEach
    void setup() {
        connection = redisClient.connect();
        connection.sync().flushall();
        store = new ReturnRouteStore(connection);
    }

    @AfterEach
    void teardown() {
        connection.close();
    }

    @Test
    void routeRoundTripWithTtl() {
        store.saveRoute("cid-1", "svc-return:origin-1", 90);
        assertEquals("svc-return:origin-1", store.getRoute("cid-1"));
        long ttl = connection.sync().ttl("request:cid-1");
        assertTrue(ttl > 0 && ttl <= 90, "route TTL should be set, got " + ttl);
    }

    @Test
    void missingRouteIsNull() {
        assertNull(store.getRoute("nope"));   // orphan / expired
    }

    @Test
    void segmentQueueIsFifoWithTtl() {
        store.appendSegment("cid-1", "{\"type\":\"data\",\"body\":\"first\"}", 30);
        store.appendSegment("cid-1", "{\"type\":\"eof\"}", 30);
        assertEquals(2, store.queueLength("cid-1"));
        long ttl = connection.sync().ttl("queue:cid-1");
        assertTrue(ttl > 0 && ttl <= 30, "queue TTL should be set atomically with the append, got " + ttl);
        // destructive pops drain in append (list) order; a fully drained list ceases to exist
        assertEquals("{\"type\":\"data\",\"body\":\"first\"}", store.popSegment("cid-1"));
        assertEquals("{\"type\":\"eof\"}", store.popSegment("cid-1"));
        assertNull(store.popSegment("cid-1"), "empty queue pops null");
        assertEquals(0, store.queueLength("cid-1"), "drained list auto-deletes");
    }

    @Test
    void cleanupDeletesBothKeys() {
        store.saveRoute("cid-1", "svc-return:origin-1", 90);
        store.appendSegment("cid-1", "{\"type\":\"eof\",\"body\":\"{}\"}", 30);
        store.cleanup("cid-1");
        assertNull(store.getRoute("cid-1"), "route deleted on cleanup");
        assertEquals(0, store.queueLength("cid-1"), "queue deleted on cleanup");
    }
}
