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

import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisConfigTest {

    @Test
    void defaultsWhenUnset() {
        RedisConfig config = RedisConfig.from(new MapConfig(Map.of()));
        assertEquals("127.0.0.1", config.host());
        assertEquals(6379, config.port());
        assertEquals("", config.username());
        assertEquals("", config.password());
        assertFalse(config.ssl());
        assertEquals(0, config.database());
        assertEquals(5000, config.timeoutMs());
        // no keys set -> cluster.detect defaults to auto, cluster.mode boolean defaults to false
        assertTrue(config.autoDetectCluster());
        assertFalse(config.clusterEnabled());
        assertEquals("", config.clusterNodes());
        // the legacy 6-arg constructor is standalone with no probe (autoDetect=false, clusterEnabled=false)
        RedisConfig legacy = new RedisConfig("127.0.0.1", 6379, "", false, 0, 5000);
        assertFalse(legacy.autoDetectCluster());
        assertFalse(legacy.clusterEnabled());
    }

    @Test
    void readsDiscreteProperties() {
        RedisConfig config = RedisConfig.from(new MapConfig(Map.of(
                "soa.redis.host", "redis.internal",
                "soa.redis.port", "6380",
                "soa.redis.password", "s3cret",
                "soa.redis.ssl", "true",
                "soa.redis.database", "2",
                "soa.redis.timeout.ms", "1500")));
        assertEquals("redis.internal", config.host());
        assertEquals(6380, config.port());
        assertEquals("s3cret", config.password());
        assertTrue(config.ssl());
        assertEquals(2, config.database());
        assertEquals(1500, config.timeoutMs());
    }

    @Test
    void mapsOntoRedisUri() {
        RedisURI uri = new RedisConfig("redis.internal", 6380, "s3cret", true, 2, 1500).toUri();
        assertEquals("redis.internal", uri.getHost());
        assertEquals(6380, uri.getPort());
        assertTrue(uri.isSsl());
        assertEquals(2, uri.getDatabase());
        assertEquals(Duration.ofMillis(1500), uri.getTimeout());
    }

    @Test
    void clusterSelectionIsTwoKeys() {
        // detect=auto probes regardless of the boolean
        RedisConfig auto = RedisConfig.from(new MapConfig(Map.of(
                "soa.redis.cluster.detect", "auto", "soa.redis.cluster.mode", "false")));
        assertTrue(auto.autoDetectCluster());

        // detect not 'auto' -> the boolean decides; true = cluster
        RedisConfig forcedCluster = RedisConfig.from(new MapConfig(Map.of(
                "soa.redis.cluster.detect", "off", "soa.redis.cluster.mode", "true")));
        assertFalse(forcedCluster.autoDetectCluster());
        assertTrue(forcedCluster.clusterEnabled());

        // detect not 'auto', boolean false = standalone
        RedisConfig forcedStandalone = RedisConfig.from(new MapConfig(Map.of(
                "soa.redis.cluster.detect", "off", "soa.redis.cluster.mode", "false")));
        assertFalse(forcedStandalone.autoDetectCluster());
        assertFalse(forcedStandalone.clusterEnabled());
    }

    @Test
    void rbacUsernameIsRead() {
        RedisConfig config = RedisConfig.from(new MapConfig(Map.of(
                "soa.redis.username", "app-user",
                "soa.redis.password", "s3cret",
                "soa.redis.cluster.nodes", "node-a:7000, node-b:7001")));
        assertEquals("app-user", config.username());
        assertEquals("node-a:7000, node-b:7001", config.clusterNodes());
    }

    @Test
    void fallsBackToLegacyRedisKeysWhenSoaKeysAbsent() {
        // an existing deployment that predates the soa. prefix keeps working unchanged, including the
        // boolean cluster.mode a cache-style config uses
        RedisConfig config = RedisConfig.from(new MapConfig(Map.of(
                "redis.host", "legacy-host",
                "redis.port", "6390",
                "redis.password", "legacy-secret",
                "redis.cluster.detect", "off",
                "redis.cluster.mode", "true")));
        assertEquals("legacy-host", config.host());
        assertEquals(6390, config.port());
        assertEquals("legacy-secret", config.password());
        assertFalse(config.autoDetectCluster());
        assertTrue(config.clusterEnabled());
    }

    @Test
    void soaKeysWinOverLegacyKeysWhenBothPresent() {
        // set soa.redis.* to decouple from a co-resident redis.* consumer in the same application
        RedisConfig config = RedisConfig.from(new MapConfig(Map.of(
                "redis.host", "cache-host",
                "soa.redis.host", "soa-host",
                "redis.password", "cache-secret",
                "soa.redis.password", "soa-secret")));
        assertEquals("soa-host", config.host());
        assertEquals("soa-secret", config.password());
    }

    @Test
    void seedUrisFromExplicitNodes() {
        List<RedisURI> seeds = new RedisConfig("ignored", 6379, "", "", true, 0, 2000,
                false, true, "node-a:7000, node-b:7001").seedUris();
        assertEquals(2, seeds.size());
        assertEquals("node-a", seeds.get(0).getHost());
        assertEquals(7000, seeds.get(0).getPort());
        assertEquals("node-b", seeds.get(1).getHost());
        assertEquals(7001, seeds.get(1).getPort());
        // TLS carries onto every seed; a cluster is database 0 (no index applied)
        assertTrue(seeds.get(0).isSsl());
    }

    @Test
    void seedUrisFallBackToTheSingleHostPort() {
        List<RedisURI> seeds = new RedisConfig("config-endpoint", 6380, "", "", false, 0, 2000,
                false, true, "").seedUris();
        assertEquals(1, seeds.size());
        assertEquals("config-endpoint", seeds.get(0).getHost());
        assertEquals(6380, seeds.get(0).getPort());
    }
}
