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

package org.platformlambda.mini.kafka;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * WHEN the probe's client configuration is resolved. This function is {@code @PreLoad}, so it is
 * constructed before a {@code @MainApplication} credential bootstrap (e.g. fetching secrets from a
 * vault and publishing them as system properties) has run - resolving the template in the
 * constructor would freeze a template that interpolates such a credential with the credential
 * missing, and every later probe would fail with Kafka's
 * "The OAuth configuration option clientId value is required" no matter what the environment does.
 *
 * <p>No broker is involved: the probe is pointed at a closed port with a 1ms timeout, so each
 * attempt fails immediately and the assertions are about how often the supplier is consulted.
 */
class KafkaHealthCheckLazyConfigTest {

    private static final long PROBE_IMMEDIATELY = 0L;
    private static final long TIMEOUT_MS = 1L;

    /** A probe config pointed at a closed port - constructing the client works, the round trip does not. */
    private static Properties unreachable() {
        Properties p = new Properties();
        p.setProperty("bootstrap.servers", "localhost:1");
        p.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        p.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return p;
    }

    private static KafkaHealthCheck probing(Supplier<Properties> config) {
        return new KafkaHealthCheck("kafka", config, TIMEOUT_MS, PROBE_IMMEDIATELY);
    }

    @Test
    void constructionResolvesNothing() {
        AtomicInteger resolves = new AtomicInteger();
        probing(() -> {
            resolves.incrementAndGet();
            return unreachable();
        });
        assertEquals(0, resolves.get(),
                "a @PreLoad constructor runs before the credential bootstrap - it must not resolve the template");
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
    void incompleteConfigIsAPassingWaitingStatusNotAFailure() {
        AtomicInteger resolves = new AtomicInteger();
        // an empty template stands in for one whose values have not been published yet:
        // the Kafka client cannot even be constructed from it
        var health = probing(() -> {
            resolves.incrementAndGet();
            return new Properties();
        });
        Map<String, String> probe = Map.of("type", "health");
        assertEquals("Waiting for Kafka connection", asMap(health.handleEvent(probe, null, 1)).get("status"),
                "an unbuildable client is a start-up condition, not an outage - /health must pass");
        assertEquals("Waiting for Kafka connection", asMap(health.handleEvent(probe, null, 1)).get("status"));
        assertEquals(2, resolves.get(),
                "each waiting probe re-resolves the template so a late credential is picked up");
    }

    @Test
    void typeInfoResolvesOnDemandAndKeepsTheAnswer() {
        AtomicInteger resolves = new AtomicInteger();
        var health = probing(() -> {
            resolves.incrementAndGet();
            return unreachable();
        });
        Map<String, String> info = Map.of("type", "info");
        assertEquals("localhost:1", asMap(health.handleEvent(info, null, 1)).get("href"));
        assertEquals("localhost:1", asMap(health.handleEvent(info, null, 1)).get("href"));
        assertEquals(1, resolves.get(),
                "bootstrap.servers does not depend on a late credential, so one resolve serves every info call");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object result) {
        return (Map<String, Object>) result;
    }
}
