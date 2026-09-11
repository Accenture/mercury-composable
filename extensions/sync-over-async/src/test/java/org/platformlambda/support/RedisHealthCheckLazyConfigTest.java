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

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisConnectionException;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WHEN the probe's client configuration is resolved, and how failures are classified. This function
 * is {@code @PreLoad}, so it is constructed before a {@code @MainApplication} credential bootstrap
 * (e.g. fetching secrets from a vault and publishing them as system properties) has run - resolving
 * the configuration in the constructor would freeze a late-published {@code redis.password} as
 * missing for the life of the instance.
 *
 * <p>No Redis server is involved: probes are pointed at a closed port or at unbuildable values, so
 * each attempt fails immediately and the assertions are about how often the supplier is consulted
 * and which failures count as "waiting" rather than an outage.
 */
class RedisHealthCheckLazyConfigTest {

    private static final long PROBE_IMMEDIATELY = 0L;
    private static final long TIMEOUT_MS = 500L;

    /** A probe config pointed at a closed port - building the client works, the round trip does not. */
    private static RedisConfig unreachable() {
        return new RedisConfig("127.0.0.1", 1, "", false, 0, TIMEOUT_MS);
    }

    /** A probe config the client cannot even be built from - e.g. an unresolved placeholder port. */
    private static RedisConfig unbuildable() {
        return new RedisConfig("127.0.0.1", -1, "", false, 0, TIMEOUT_MS);
    }

    private static RedisHealthCheck probing(Supplier<RedisConfig> config) {
        return new RedisHealthCheck(config, TIMEOUT_MS, PROBE_IMMEDIATELY);
    }

    @Test
    void constructionResolvesNothing() {
        AtomicInteger resolves = new AtomicInteger();
        probing(() -> {
            resolves.incrementAndGet();
            return unreachable();
        });
        assertEquals(0, resolves.get(),
                "a @PreLoad constructor runs before the credential bootstrap - it must not resolve the config");
    }

    @Test
    void everyRebuildResolvesAgainSoALateCredentialIsPickedUp() {
        AtomicInteger resolves = new AtomicInteger();
        var health = probing(() -> {
            resolves.incrementAndGet();
            return unreachable();
        });
        Map<String, String> probe = Map.of("type", "health");
        assertThrows(AppException.class, () -> health.handleEvent(probe, null, 1));
        assertThrows(AppException.class, () -> health.handleEvent(probe, null, 1));
        assertEquals(2, resolves.get(),
                "a failed probe closes the client, so the next one re-resolves and the check can heal");
    }

    @Test
    void unbuildableConfigIsAPassingWaitingStatusNotAFailure() {
        AtomicInteger resolves = new AtomicInteger();
        var health = probing(() -> {
            resolves.incrementAndGet();
            return unbuildable();
        });
        Map<String, String> probe = Map.of("type", "health");
        assertEquals("Waiting for Redis connection", asMap(health.handleEvent(probe, null, 1)).get("status"),
                "an unbuildable client is a start-up condition, not an outage - /health must pass");
        assertEquals("Waiting for Redis connection", asMap(health.handleEvent(probe, null, 1)).get("status"));
        assertEquals(2, resolves.get(),
                "each waiting probe re-resolves the config so late-published values are picked up");
    }

    @Test
    void typeInfoResolvesOnDemandAndKeepsTheAnswer() {
        AtomicInteger resolves = new AtomicInteger();
        var health = probing(() -> {
            resolves.incrementAndGet();
            return unreachable();
        });
        Map<String, String> info = Map.of("type", "info");
        assertEquals("127.0.0.1:1", asMap(health.handleEvent(info, null, 1)).get("href"));
        assertEquals("127.0.0.1:1", asMap(health.handleEvent(info, null, 1)).get("href"));
        assertEquals(1, resolves.get(),
                "host and port do not depend on a late credential, so one resolve serves every info call");
    }

    @Test
    void authRejectionsCountAsWaitingButOutagesDoNot() {
        // Lettuce surfaces a rejected handshake as a connection exception wrapping the server's error
        assertTrue(RedisHealthCheck.waitingOnConfig(new RedisConnectionException("Unable to connect",
                        new RedisCommandExecutionException("NOAUTH Authentication required."))),
                "no credential sent yet = the bootstrap has not landed - waiting");
        assertTrue(RedisHealthCheck.waitingOnConfig(new RedisConnectionException("Unable to connect",
                        new RedisCommandExecutionException("WRONGPASS invalid username-password pair"))),
                "credential rejected = not yet the real one - a restart cannot fix it - waiting");
        assertTrue(RedisHealthCheck.waitingOnConfig(new RedisCommandExecutionException(
                        "ERR Client sent AUTH, but no password is set")),
                "a stale credential against an auth-less server is also a config condition - waiting");
        assertFalse(RedisHealthCheck.waitingOnConfig(new RedisConnectionException("Unable to connect",
                        new ConnectException("Connection refused"))),
                "a genuine connectivity failure is an outage and must fail /health");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object result) {
        return (Map<String, Object>) result;
    }
}
