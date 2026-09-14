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
import org.platformlambda.support.RedisBackend;
import org.platformlambda.support.RedisBackendFactory;
import org.platformlambda.support.RedisClusterMode;
import org.platformlambda.support.RedisConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies {@link RedisBackendFactory} against a real (embedded, single-node) Redis: auto-detection
 * correctly resolves a non-cluster server to a standalone backend, an explicit standalone mode does the
 * same, and either backend round-trips a value through the common command interface. Cluster-mode wiring
 * (topology discovery, cross-slot safety) is validated against a real Redis Cluster in the field - no
 * embedded cluster fixture exists - but the standalone path and the mode selection are proven here.
 */
class RedisBackendFactoryTest extends RedisTestBase {

    private RedisConfig config(RedisClusterMode mode) {
        return new RedisConfig("127.0.0.1", redisPort, "", "", false, 0, 2000, mode, "");
    }

    @Test
    void autoDetectResolvesAStandaloneServerAndRoundTrips() {
        try (RedisBackend backend = RedisBackendFactory.create(config(RedisClusterMode.AUTO))) {
            // the embedded server reports cluster_enabled:0, so auto-detection must pick standalone
            assertFalse(backend.cluster(), "a single-node server must auto-detect as standalone");
            backend.commands().set("soa-factory-probe", "value");
            assertEquals("value", backend.commands().get("soa-factory-probe"));
            backend.commands().del("soa-factory-probe");
        }
    }

    @Test
    void explicitStandaloneModeSkipsDetectionAndRoundTrips() {
        try (RedisBackend backend = RedisBackendFactory.create(config(RedisClusterMode.STANDALONE))) {
            assertFalse(backend.cluster());
            backend.commands().set("soa-standalone-probe", "value");
            assertEquals("value", backend.commands().get("soa-standalone-probe"));
            backend.commands().del("soa-standalone-probe");
        }
    }
}
