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
        // absent redis.cluster.mode auto-detects; the legacy 6-arg constructor stays standalone
        assertEquals(RedisClusterMode.AUTO, config.clusterMode());
        assertEquals("", config.clusterNodes());
        assertEquals(RedisClusterMode.STANDALONE,
                new RedisConfig("127.0.0.1", 6379, "", false, 0, 5000).clusterMode());
    }

    @Test
    void readsDiscreteProperties() {
        RedisConfig config = RedisConfig.from(new MapConfig(Map.of(
                "redis.host", "redis.internal",
                "redis.port", "6380",
                "redis.password", "s3cret",
                "redis.ssl", "true",
                "redis.database", "2",
                "redis.timeout.ms", "1500")));
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
    void readsClusterAndRbacProperties() {
        RedisConfig config = RedisConfig.from(new MapConfig(Map.of(
                "redis.username", "app-user",
                "redis.password", "s3cret",
                "redis.cluster.mode", "cluster",
                "redis.cluster.nodes", "node-a:7000, node-b:7001")));
        assertEquals("app-user", config.username());
        assertEquals(RedisClusterMode.CLUSTER, config.clusterMode());
        assertEquals("node-a:7000, node-b:7001", config.clusterNodes());
    }

    @Test
    void clusterModeParsingIsLenient() {
        assertEquals(RedisClusterMode.AUTO, RedisClusterMode.from("auto"));
        assertEquals(RedisClusterMode.STANDALONE, RedisClusterMode.from("STANDALONE"));
        assertEquals(RedisClusterMode.CLUSTER, RedisClusterMode.from(" Cluster "));
        // blank / null / unrecognised resolve to the safe self-detecting default
        assertEquals(RedisClusterMode.AUTO, RedisClusterMode.from(""));
        assertEquals(RedisClusterMode.AUTO, RedisClusterMode.from(null));
        assertEquals(RedisClusterMode.AUTO, RedisClusterMode.from("sentinel"));
    }

    @Test
    void seedUrisFromExplicitNodes() {
        List<RedisURI> seeds = new RedisConfig("ignored", 6379, "", "", true, 0, 2000,
                RedisClusterMode.CLUSTER, "node-a:7000, node-b:7001").seedUris();
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
                RedisClusterMode.CLUSTER, "").seedUris();
        assertEquals(1, seeds.size());
        assertEquals("config-endpoint", seeds.get(0).getHost());
        assertEquals(6380, seeds.get(0).getPort());
    }
}
