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
        assertEquals("", config.password());
        assertFalse(config.ssl());
        assertEquals(0, config.database());
        assertEquals(5000, config.timeoutMs());
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
}
