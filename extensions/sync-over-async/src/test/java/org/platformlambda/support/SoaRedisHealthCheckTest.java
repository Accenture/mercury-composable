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

package org.platformlambda.support;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The thin sync-over-async binding of the shared {@link org.platformlambda.redis.RedisHealthProbe}: this
 * subclass fixes the {@code soa.redis.health} route and reads the {@code soa.redis.*} namespace. The probe
 * logic itself is covered exhaustively in the redis-connection foundation; here we prove the subclass
 * resolves its own namespace (the href reflects {@code soa.redis.host}/{@code soa.redis.port} from the test
 * {@code application.properties}) and stays in the start-up placeholder window during the grace period. No
 * live server is contacted: {@code info} and the in-grace {@code health} response are both pre-probe.
 */
class SoaRedisHealthCheckTest {

    private static final Map<String, String> INFO = Map.of("type", "info");
    private static final Map<String, String> HEALTH = Map.of("type", "health");

    @SuppressWarnings("unchecked")
    @Test
    void infoResolvesTheSoaNamespaceHref() {
        var health = new SoaRedisHealthCheck();
        Map<String, Object> info = (Map<String, Object>) health.handleEvent(INFO, null, 1);
        assertEquals("redis", info.get("service"));
        // soa.redis.host / soa.redis.port from the test application.properties (127.0.0.1:6379)
        assertEquals("127.0.0.1:6379", info.get("href"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void withinGraceReturnsTheStartupPlaceholder() {
        // the default grace is 30s, so the first health check is a placeholder healthy status (the client
        // warms up in the background) - deterministic and does not depend on a reachable server
        var health = new SoaRedisHealthCheck();
        Object result = health.handleEvent(HEALTH, null, 1);
        assertInstanceOf(Map.class, result);
        assertEquals("Redis client is starting up", ((Map<String, Object>) result).get("status"));
    }
}
