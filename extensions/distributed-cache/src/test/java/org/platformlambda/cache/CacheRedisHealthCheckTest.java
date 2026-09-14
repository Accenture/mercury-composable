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

package org.platformlambda.cache;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The cache's thin binding of the shared {@link org.platformlambda.redis.RedisHealthProbe} to the reserved
 * {@code redis.health} route and the plain {@code redis.*} namespace. The probe logic is covered in the
 * redis-connection foundation; here we prove the subclass resolves its own namespace (the href reflects
 * {@code redis.host}/{@code redis.port} from the test {@code application.properties}) and stays in the
 * start-up placeholder window during the grace period. No live server is contacted.
 */
class CacheRedisHealthCheckTest {

    private static final Map<String, String> INFO = Map.of("type", "info");
    private static final Map<String, String> HEALTH = Map.of("type", "health");

    @SuppressWarnings("unchecked")
    @Test
    void infoResolvesThePlainRedisNamespaceHref() {
        var health = new CacheRedisHealthCheck();
        Map<String, Object> info = (Map<String, Object>) health.handleEvent(INFO, null, 1);
        assertEquals("redis", info.get("service"));
        // redis.host / redis.port from the test application.properties (127.0.0.1:6379)
        assertEquals("127.0.0.1:6379", info.get("href"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void withinGraceReturnsTheStartupPlaceholder() {
        var health = new CacheRedisHealthCheck();
        Object result = health.handleEvent(HEALTH, null, 1);
        assertInstanceOf(Map.class, result);
        assertEquals("Redis client is starting up", ((Map<String, Object>) result).get("status"));
    }
}
