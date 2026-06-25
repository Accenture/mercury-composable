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
    void responseRoundTripWithTtl() {
        store.saveResponse("cid-1", "{\"status\":\"200\"}", 30);
        assertEquals("{\"status\":\"200\"}", store.getResponse("cid-1"));
        long ttl = connection.sync().ttl("response:cid-1");
        assertTrue(ttl > 0 && ttl <= 30, "response TTL should be set, got " + ttl);
    }
}
