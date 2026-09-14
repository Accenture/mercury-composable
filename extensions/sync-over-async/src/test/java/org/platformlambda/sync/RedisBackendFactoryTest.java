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
import org.platformlambda.support.RedisConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link RedisBackendFactory} against a real (embedded, single-node) Redis: auto-detection
 * correctly resolves a non-cluster server to a standalone backend, an explicit standalone mode does the
 * same, and either backend round-trips a value through the common command interface. Cluster-mode wiring
 * (topology discovery, cross-slot safety) is validated against a real Redis Cluster in the field - no
 * embedded cluster fixture exists - but the standalone path and the mode selection are proven here.
 */
class RedisBackendFactoryTest extends RedisTestBase {

    private RedisConfig config(boolean autoDetect, boolean clusterEnabled) {
        return new RedisConfig("127.0.0.1", redisPort, "", "", false, 0, 2000, autoDetect, clusterEnabled, "");
    }

    @Test
    void autoDetectResolvesAStandaloneServerAndRoundTrips() {
        // detect=auto: the embedded server reports cluster_enabled:0, so detection must pick standalone
        try (RedisBackend backend = RedisBackendFactory.create(config(true, false))) {
            assertFalse(backend.cluster(), "a single-node server must auto-detect as standalone");
            backend.commands().set("soa-factory-probe", "value");
            assertEquals("value", backend.commands().get("soa-factory-probe"));
            backend.commands().del("soa-factory-probe");
        }
    }

    @Test
    void explicitStandaloneModeSkipsDetectionAndRoundTrips() {
        // detect off, cluster.mode false: standalone by config, no probe
        try (RedisBackend backend = RedisBackendFactory.create(config(false, false))) {
            assertFalse(backend.cluster());
            backend.commands().set("soa-standalone-probe", "value");
            assertEquals("value", backend.commands().get("soa-standalone-probe"));
            backend.commands().del("soa-standalone-probe");
        }
    }

    @Test
    void explicitClusterModeRoutesToTheClusterBranch() {
        // detect off, cluster.mode true: the factory takes the cluster branch and builds a cluster client,
        // which cannot form a topology against the single-node embedded server and fails fast - proving the
        // boolean routes to cluster. (A real cluster's success path is validated in the field; no embedded
        // cluster fixture exists.)
        assertThrows(RuntimeException.class, () -> RedisBackendFactory.create(config(false, true)));
    }
}
