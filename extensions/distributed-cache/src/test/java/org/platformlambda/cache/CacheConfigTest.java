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

package org.platformlambda.cache;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

    @Test
    void defaultsWhenUnset() {
        CacheConfig config = CacheConfig.from(new MapConfig(Map.of()));
        assertEquals("", config.keyPrefix());
        assertEquals(3600, config.defaultTtlSeconds(), "redis.cache.default.ttl defaults to 1h");
        // connection resolves from the plain redis.* namespace (defaults)
        assertEquals("127.0.0.1", config.redisConfig().host());
        assertEquals(6379, config.redisConfig().port());
    }

    @Test
    void readsCacheTunablesAndPlainRedisNamespace() {
        CacheConfig config = CacheConfig.from(new MapConfig(Map.of(
                "redis.cache.key.prefix", "app1:",
                "redis.cache.default.ttl", "5m",
                "redis.host", "cache-host",
                "redis.port", "6395",
                "redis.password", "s3cret")));
        assertEquals("app1:", config.keyPrefix());
        assertEquals(300, config.defaultTtlSeconds());
        assertEquals("cache-host", config.redisConfig().host());
        assertEquals(6395, config.redisConfig().port());
        assertEquals("s3cret", config.redisConfig().password());
    }

    @Test
    void ignoresTheSoaNamespace() {
        // the cache reads redis.* only; a co-resident soa.redis.* consumer must not leak into it
        CacheConfig config = CacheConfig.from(new MapConfig(Map.of(
                "soa.redis.host", "soa-host",
                "redis.host", "cache-host")));
        assertEquals("cache-host", config.redisConfig().host());
        assertNotEquals("soa-host", config.redisConfig().host());
    }
}
