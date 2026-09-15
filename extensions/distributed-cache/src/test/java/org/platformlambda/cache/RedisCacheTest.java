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

import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.redis.RedisBackend;
import org.platformlambda.redis.StandaloneRedisBackend;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code v1.cache.redis} function contract: action dispatch, the header/body shapes per action, opaque
 * byte[] values (with the String convenience), TTL-header handling, and the error cases. The store is a real
 * one over an embedded Redis, injected through the function's test seam.
 */
class RedisCacheTest extends RedisTestBase {

    private static final long DEFAULT_TTL = 60;
    private static final long TIMEOUT_MS = 2000;
    private static final int INSTANCE = 1;

    private static RedisBackend<byte[]> backend;
    private static RedisClusterCommands<String, byte[]> raw;
    private static RedisCache function;

    @BeforeAll
    static void connect() {
        backend = new StandaloneRedisBackend<>(redisClient, CacheRuntime.CODEC, false);
        raw = backend.commands();
        RedisCacheStore store = new RedisCacheStore(backend, "", DEFAULT_TTL, TIMEOUT_MS);
        function = new RedisCache(() -> store);
    }

    @AfterAll
    static void disconnect() {
        if (backend != null) {
            backend.close();
        }
    }

    @BeforeEach
    void flush() {
        raw.flushall();
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, String> headers(String... kv) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }

    @Test
    void putReturnsTrueAndGetRoundTrips() {
        assertEquals(Boolean.TRUE, function.handleEvent(headers("action", "PUT", "key", "k1"), bytes("hi"), INSTANCE));
        assertArrayEquals(bytes("hi"), (byte[]) function.handleEvent(headers("action", "GET", "key", "k1"), null, INSTANCE));
    }

    @Test
    void actionIsCaseInsensitive() {
        function.handleEvent(headers("action", "put", "key", "k1"), bytes("v"), INSTANCE);
        assertArrayEquals(bytes("v"), (byte[]) function.handleEvent(headers("action", "get", "key", "k1"), null, INSTANCE));
    }

    @Test
    void getMissReturnsNull() {
        assertNull(function.handleEvent(headers("action", "GET", "key", "absent"), null, INSTANCE));
    }

    @Test
    void aStringBodyIsStoredAsUtf8Bytes() {
        function.handleEvent(headers("action", "PUT", "key", "k1"), "text-value", INSTANCE);
        assertArrayEquals(bytes("text-value"), (byte[]) function.handleEvent(headers("action", "GET", "key", "k1"), null, INSTANCE));
    }

    @Test
    void ttlHeaderIsHonoured() {
        function.handleEvent(headers("action", "PUT", "key", "k1", "ttl", "30s"), bytes("v"), INSTANCE);
        long ttl = raw.ttl("k1");
        assertTrue(ttl > 0 && ttl <= 30, "the 30s ttl header must bound the key's TTL, got " + ttl);
    }

    @Test
    void deleteReturnsCount() {
        function.handleEvent(headers("action", "PUT", "key", "k1"), bytes("v"), INSTANCE);
        assertEquals(1L, function.handleEvent(headers("action", "DELETE", "key", "k1"), null, INSTANCE));
    }

    @Test
    void putIfNotPresentReturnsBoolean() {
        assertEquals(Boolean.TRUE, function.handleEvent(headers("action", "PUT_IF_NOT_PRESENT", "key", "k1"), bytes("a"), INSTANCE));
        assertEquals(Boolean.FALSE, function.handleEvent(headers("action", "PUT_IF_NOT_PRESENT", "key", "k1"), bytes("b"), INSTANCE));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mgetReturnsAMapOfPresentKeys() {
        function.handleEvent(headers("action", "PUT", "key", "a"), bytes("1"), INSTANCE);
        function.handleEvent(headers("action", "PUT", "key", "c"), bytes("3"), INSTANCE);
        Object result = function.handleEvent(headers("action", "MGET"), List.of("a", "b", "c"), INSTANCE);
        assertInstanceOf(Map.class, result);
        Map<String, byte[]> found = (Map<String, byte[]>) result;
        assertEquals(2, found.size());
        assertArrayEquals(bytes("1"), found.get("a"));
        assertFalse(found.containsKey("b"));
    }

    @Test
    void mputWritesEveryEntryThenReadsBack() {
        Map<String, byte[]> entries = Map.of("a", bytes("1"), "b", bytes("2"));
        assertEquals(Boolean.TRUE, function.handleEvent(headers("action", "MPUT"), entries, INSTANCE));
        assertArrayEquals(bytes("1"), (byte[]) function.handleEvent(headers("action", "GET", "key", "a"), null, INSTANCE));
        assertArrayEquals(bytes("2"), (byte[]) function.handleEvent(headers("action", "GET", "key", "b"), null, INSTANCE));
    }

    @Test
    void listPushPopLen() {
        assertEquals(1L, function.handleEvent(headers("action", "LIST_PUSH", "key", "q"), bytes("first"), INSTANCE));
        assertEquals(2L, function.handleEvent(headers("action", "LIST_PUSH", "key", "q"), bytes("second"), INSTANCE));
        assertEquals(2L, function.handleEvent(headers("action", "LIST_LEN", "key", "q"), null, INSTANCE));
        assertArrayEquals(bytes("first"), (byte[]) function.handleEvent(headers("action", "LIST_POP", "key", "q"), null, INSTANCE));
    }

    @Test
    void missingActionIsRejected() {
        Map<String, String> noAction = headers("key", "k1");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> function.handleEvent(noAction, null, INSTANCE));
        assertTrue(error.getMessage().contains("action"));
    }

    @Test
    void unsupportedActionIsRejected() {
        Map<String, String> unsupported = headers("action", "INCR", "key", "k1");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> function.handleEvent(unsupported, null, INSTANCE));
        assertTrue(error.getMessage().contains("INCR"));
    }

    @Test
    void putWithoutAValueIsRejected() {
        Map<String, String> put = headers("action", "PUT", "key", "k1");
        assertThrows(IllegalArgumentException.class,
                () -> function.handleEvent(put, null, INSTANCE));
    }

    @Test
    void mgetWithoutAListBodyIsRejected() {
        Map<String, String> mget = headers("action", "MGET");
        assertThrows(IllegalArgumentException.class,
                () -> function.handleEvent(mget, "not-a-list", INSTANCE));
    }
}
