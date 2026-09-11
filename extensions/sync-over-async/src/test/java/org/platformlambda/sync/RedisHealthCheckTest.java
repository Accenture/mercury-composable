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

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.Utility;
import org.platformlambda.support.RedisConfig;
import org.platformlambda.support.RedisHealthCheck;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The health contract against a real (embedded) Redis server: info, live probe, placeholder
 * during the start-up grace, outage failure - and the self-healing path where an unusable
 * configuration completes while the application is running.
 */
class RedisHealthCheckTest extends RedisTestBase {

    private static final Map<String, String> INFO = Map.of("type", "info");
    private static final Map<String, String> HEALTH = Map.of("type", "health");
    private static final long TIMEOUT_MS = 2000L;

    private static RedisConfig serverConfig() {
        return new RedisConfig("127.0.0.1", redisPort, "", false, 0, TIMEOUT_MS);
    }

    @SuppressWarnings("unchecked")
    @Test
    void infoDescribesTheRedisDependency() {
        var health = new RedisHealthCheck(RedisHealthCheckTest::serverConfig, TIMEOUT_MS, 0);
        var result = health.handleEvent(INFO, null, 1);
        assertInstanceOf(Map.class, result);
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("redis", map.get("service"));
        assertEquals("127.0.0.1:" + redisPort, map.get("href"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void probesImmediatelyWhenGraceExpired() {
        var health = new RedisHealthCheck(RedisHealthCheckTest::serverConfig, TIMEOUT_MS, 0);
        Map<String, Object> result = (Map<String, Object>) health.handleEvent(HEALTH, null, 1);
        assertEquals("Redis is reachable", result.get("status"));
        assertEquals("127.0.0.1:" + redisPort, result.get("href"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void startupReturnsPlaceholderThenLiveStatus() {
        var health = new RedisHealthCheck(RedisHealthCheckTest::serverConfig, TIMEOUT_MS, 60000);
        // within the grace period, the first check is a placeholder healthy status - the Redis
        // client warms up in the background so /health never blocks during app start-up
        var first = health.handleEvent(HEALTH, null, 1);
        assertInstanceOf(Map.class, first);
        assertEquals("Redis client is starting up", ((Map<String, Object>) first).get("status"));
        // once warm-up completes, checks report the live server status
        Map<String, Object> live = null;
        // generous for busy CI executors - the poll returns as soon as warm-up completes
        long deadline = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> result = (Map<String, Object>) health.handleEvent(HEALTH, null, 1);
            if ("Redis is reachable".equals(result.get("status"))) {
                live = result;
                break;
            }
            Utility.getInstance().sleep(200);
        }
        assertNotNull(live, "warm-up should complete and report live server status");
    }

    @SuppressWarnings("unchecked")
    @Test
    void reportsWaitingUntilTheConfigCompletesThenGoesLive() {
        // simulate a @MainApplication credential bootstrap that has not run yet: the configuration is
        // unusable at first (the client cannot be built), then completes while the app is running
        AtomicReference<RedisConfig> config = new AtomicReference<>(
                new RedisConfig("127.0.0.1", -1, "", false, 0, TIMEOUT_MS));
        var health = new RedisHealthCheck(config::get, TIMEOUT_MS, 0);
        Map<String, Object> waiting = (Map<String, Object>) health.handleEvent(HEALTH, null, 1);
        assertEquals("Waiting for Redis connection", waiting.get("status"));
        // the bootstrap publishes the missing values - the next probe re-resolves and goes live,
        // with no restart and no failed /health in between
        config.set(serverConfig());
        Map<String, Object> live = (Map<String, Object>) health.handleEvent(HEALTH, null, 1);
        assertEquals("Redis is reachable", live.get("status"));
        assertEquals("127.0.0.1:" + redisPort, live.get("href"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void unreachableRedisFailsTheHealthCheck() {
        // closed port = fast connection-refused failure: a genuine outage, not a waiting condition
        var health = new RedisHealthCheck(
                () -> new RedisConfig("127.0.0.1", 1, "", false, 0, TIMEOUT_MS), TIMEOUT_MS, 0);
        Object result = health.handleEvent(HEALTH, null, 1);
        // an outage is a 503 response carrying a key-value map - the status code for the health
        // aggregation to detect, text + status for the DevOps reader
        assertInstanceOf(EventEnvelope.class, result);
        EventEnvelope offline = (EventEnvelope) result;
        assertEquals(503, offline.getStatus());
        Map<String, Object> body = (Map<String, Object>) offline.getBody();
        assertEquals(503, body.get("status"));
        assertTrue(body.get("text").toString().contains("not reachable"));
    }

    @Test
    void invalidTypeIsRejected() {
        var health = new RedisHealthCheck(RedisHealthCheckTest::serverConfig, TIMEOUT_MS, 0);
        Map<String, String> badType = Map.of("type", "unknown");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> health.handleEvent(badType, null, 1));
        assertEquals("type must be info or health", error.getMessage());
    }
}
