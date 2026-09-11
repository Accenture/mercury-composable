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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;
import redis.embedded.RedisServer;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The late-credential scenario end to end, against an embedded Redis that REQUIRES a password
 * ({@code requirepass}): a probe whose {@code redis.password} has not been published yet is a
 * passing "waiting" status - never a failed {@code /health} - and the check goes live on the
 * first probe after the credential lands, with no restart in between. This is the vault-bootstrap
 * pattern: a {@code @MainApplication} fetches secrets and publishes them as system properties
 * long after this {@code @PreLoad} function is constructed.
 */
class RedisHealthCheckAuthTest {

    private static final String DATA_DIR = "/tmp/soa-redis-auth";
    // fixed high port, distinct from RedisTestBase's server (both may run in one surefire JVM)
    private static final int AUTH_PORT = 16380;
    // an obviously local fixture credential for the embedded test server
    private static final String PASSWORD = "local-test-secret";
    private static final Map<String, String> HEALTH = Map.of("type", "health");
    private static final long TIMEOUT_MS = 2000L;

    private static RedisServer redisServer;

    // S5443: a fixed /tmp path is intentional for this test fixture (wiped before each run)
    @SuppressWarnings("java:S5443")
    @BeforeAll
    static void startRedis() throws IOException {
        File dir = new File(DATA_DIR);
        Utility.getInstance().cleanupDir(dir);
        dir.mkdirs();
        redisServer = RedisServer.newRedisServer()
                .port(AUTH_PORT)
                .setting("dir " + DATA_DIR)
                .setting("save \"\"")
                .setting("appendonly no")
                .setting("requirepass " + PASSWORD)
                .build();
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    private static RedisConfig withPassword(String password) {
        return new RedisConfig("127.0.0.1", AUTH_PORT, password, false, 0, TIMEOUT_MS);
    }

    @SuppressWarnings("unchecked")
    @Test
    void missingCredentialWaitsThenHealsWhenItLands() {
        // the bootstrap has not run: redis.password resolves blank, and the server says NOAUTH
        AtomicReference<RedisConfig> config = new AtomicReference<>(withPassword(""));
        var health = new RedisHealthCheck(config::get, TIMEOUT_MS, 0);
        Map<String, Object> waiting = (Map<String, Object>) health.handleEvent(HEALTH, null, 1);
        assertEquals("Waiting for Redis connection", waiting.get("status"),
                "a credential that has not landed is a start-up condition - /health must pass");
        // the bootstrap publishes the credential - the next probe re-resolves and authenticates
        config.set(withPassword(PASSWORD));
        Map<String, Object> live = (Map<String, Object>) health.handleEvent(HEALTH, null, 1);
        assertEquals("Redis is reachable", live.get("status"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rejectedCredentialIsAlsoWaitingNotAnOutage() {
        // WRONGPASS: still not the real credential - a pod restart cannot fix it either
        var health = new RedisHealthCheck(() -> withPassword("not-the-real-one"), TIMEOUT_MS, 0);
        Map<String, Object> waiting = (Map<String, Object>) health.handleEvent(HEALTH, null, 1);
        assertEquals("Waiting for Redis connection", waiting.get("status"));
    }
}
