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

package org.platformlambda.redis;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link RedisBackendFactory} against a real (embedded, single-node) Redis: auto-detection
 * correctly resolves a non-cluster server to a standalone backend, an explicit standalone mode does the
 * same, either backend round-trips a value through the common command interface (sync and async), and the
 * byte[] codec path the distributed cache uses stores opaque bytes. Cluster-mode wiring (topology discovery,
 * cross-slot safety) is validated against a real Redis Cluster in the field — no embedded cluster fixture
 * exists — but the standalone path and the mode selection are proven here.
 */
class RedisBackendFactoryTest extends RedisTestBase {

    private RedisConfig config(boolean autoDetect, boolean clusterEnabled) {
        return new RedisConfig("127.0.0.1", redisPort, "", "", false, 0, 2000, autoDetect, clusterEnabled, "");
    }

    @Test
    void autoDetectResolvesAStandaloneServerAndRoundTrips() {
        // detect=auto: the embedded server reports cluster_enabled:0, so detection must pick standalone
        try (RedisBackend<String> backend = RedisBackendFactory.create(config(true, false))) {
            assertFalse(backend.cluster(), "a single-node server must auto-detect as standalone");
            backend.commands().set("factory-probe", "value");
            assertEquals("value", backend.commands().get("factory-probe"));
            backend.commands().del("factory-probe");
        }
    }

    @Test
    void explicitStandaloneModeSkipsDetectionAndRoundTrips() {
        // detect off, cluster.mode false: standalone by config, no probe
        try (RedisBackend<String> backend = RedisBackendFactory.create(config(false, false))) {
            assertFalse(backend.cluster());
            backend.commands().set("standalone-probe", "value");
            assertEquals("value", backend.commands().get("standalone-probe"));
            backend.commands().del("standalone-probe");
        }
    }

    @Test
    void asyncCommandsPipelineOverTheSameConnection() throws ExecutionException, InterruptedException {
        // the async seam backs the cache's pipelined bulk write; prove it round-trips on the shared connection
        try (RedisBackend<String> backend = RedisBackendFactory.create(config(false, false))) {
            backend.async().set("async-probe", "value").get();
            assertEquals("value", backend.async().get("async-probe").get());
            backend.async().del("async-probe").get();
        }
    }

    @Test
    void byteArrayCodecStoresOpaqueBytes() {
        // the distributed cache builds a String-key / byte[]-value backend from the same factory
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        try (RedisBackend<byte[]> backend = RedisBackendFactory.create(config(false, false), codec)) {
            byte[] payload = "opaque-bytes".getBytes(StandardCharsets.UTF_8);
            backend.commands().set("bytes-probe", payload);
            assertArrayEquals(payload, backend.commands().get("bytes-probe"));
            backend.commands().del("bytes-probe");
        }
    }

    @Test
    void explicitClusterModeRoutesToTheClusterBranch() {
        // detect off, cluster.mode true: the factory takes the cluster branch and builds a cluster client,
        // which cannot form a topology against the single-node embedded server and fails fast — proving the
        // boolean routes to cluster. (A real cluster's success path is validated in the field; no embedded
        // cluster fixture exists.)
        RedisConfig clusterConfig = config(false, true);
        assertThrows(RuntimeException.class, () -> {
            try (RedisBackend<String> ignored = RedisBackendFactory.create(clusterConfig)) {
                // create() throws before returning; a returned backend would be closed by try-with-resources
            }
        });
    }
}
