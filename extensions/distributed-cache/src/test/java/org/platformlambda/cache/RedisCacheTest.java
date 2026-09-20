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
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.platformlambda.core.exception.AppException;
import java.lang.reflect.Proxy;
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
    void putReturnsTrueAndGetRoundTrips() throws Exception {
        assertEquals(Boolean.TRUE, function.handleEvent(headers("action", "PUT", "key", "k1"), bytes("hi"), INSTANCE));
        assertArrayEquals(bytes("hi"), (byte[]) function.handleEvent(headers("action", "GET", "key", "k1"), null, INSTANCE));
    }

    @Test
    void actionIsCaseInsensitive() throws Exception {
        function.handleEvent(headers("action", "put", "key", "k1"), bytes("v"), INSTANCE);
        assertArrayEquals(bytes("v"), (byte[]) function.handleEvent(headers("action", "get", "key", "k1"), null, INSTANCE));
    }

    @Test
    void getMissReturnsNull() throws Exception {
        assertNull(function.handleEvent(headers("action", "GET", "key", "absent"), null, INSTANCE));
    }

    @Test
    void aStringBodyIsStoredAsUtf8Bytes() throws Exception {
        function.handleEvent(headers("action", "PUT", "key", "k1"), "text-value", INSTANCE);
        assertArrayEquals(bytes("text-value"), (byte[]) function.handleEvent(headers("action", "GET", "key", "k1"), null, INSTANCE));
    }

    @Test
    void ttlHeaderIsHonoured() throws Exception {
        function.handleEvent(headers("action", "PUT", "key", "k1", "ttl", "30s"), bytes("v"), INSTANCE);
        long ttl = raw.ttl("k1");
        assertTrue(ttl > 0 && ttl <= 30, "the 30s ttl header must bound the key's TTL, got " + ttl);
    }

    @Test
    void deleteReturnsCount() throws Exception {
        function.handleEvent(headers("action", "PUT", "key", "k1"), bytes("v"), INSTANCE);
        assertEquals(1L, function.handleEvent(headers("action", "DELETE", "key", "k1"), null, INSTANCE));
    }

    @Test
    void putIfNotPresentReturnsBoolean() throws Exception {
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
    void mputWritesEveryEntryThenReadsBack() throws Exception {
        Map<String, byte[]> entries = Map.of("a", bytes("1"), "b", bytes("2"));
        assertEquals(Boolean.TRUE, function.handleEvent(headers("action", "MPUT"), entries, INSTANCE));
        assertArrayEquals(bytes("1"), (byte[]) function.handleEvent(headers("action", "GET", "key", "a"), null, INSTANCE));
        assertArrayEquals(bytes("2"), (byte[]) function.handleEvent(headers("action", "GET", "key", "b"), null, INSTANCE));
    }

    @Test
    void listPushPopLen() throws Exception {
        assertEquals(1L, function.handleEvent(headers("action", "LIST_PUSH", "key", "q"), bytes("first"), INSTANCE));
        assertEquals(2L, function.handleEvent(headers("action", "LIST_PUSH", "key", "q"), bytes("second"), INSTANCE));
        assertEquals(2L, function.handleEvent(headers("action", "LIST_LEN", "key", "q"), null, INSTANCE));
        assertArrayEquals(bytes("first"), (byte[]) function.handleEvent(headers("action", "LIST_POP", "key", "q"), null, INSTANCE));
    }

    @Test
    void missingActionIsRejected() throws Exception {
        Map<String, String> noAction = headers("key", "k1");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> function.handleEvent(noAction, null, INSTANCE));
        assertTrue(error.getMessage().contains("action"));
    }

    @Test
    void unsupportedActionIsRejected() throws Exception {
        Map<String, String> unsupported = headers("action", "INCR", "key", "k1");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> function.handleEvent(unsupported, null, INSTANCE));
        assertTrue(error.getMessage().contains("INCR"));
    }

    @Test
    void putWithoutAValueIsRejected() throws Exception {
        Map<String, String> put = headers("action", "PUT", "key", "k1");
        assertThrows(IllegalArgumentException.class,
                () -> function.handleEvent(put, null, INSTANCE));
    }

    @Test
    void mgetWithoutAListBodyIsRejected() throws Exception {
        Map<String, String> mget = headers("action", "MGET");
        assertThrows(IllegalArgumentException.class,
                () -> function.handleEvent(mget, "not-a-list", INSTANCE));
    }

    /**
     * Redis client failures are classified for the caller (Java/Rust interop drive, 2026-09-20): a command
     * timeout is a 408 with Lettuce's message, an unreachable Redis a 503 "Redis unavailable - ...", and a
     * genuine command error from the server stays unclassified (the platform's default mapping applies).
     */
    @Test
    void redisFailuresAreClassifiedForTheCaller() {
        RedisCache down = new RedisCache(() -> {
            throw new RedisConnectionException("Unable to connect to 127.0.0.1:1");
        });
        AppException unavailable = assertThrows(AppException.class,
                () -> down.handleEvent(headers("action", "GET", "key", "k1"), null, INSTANCE));
        assertEquals(503, unavailable.getStatus());
        assertEquals("Redis unavailable - Unable to connect to 127.0.0.1:1", unavailable.getMessage());
        RedisCache slow = new RedisCache(() -> new RedisCacheStore(
                failingBackend(new RedisCommandTimeoutException("Command timed out after 1 second(s)")),
                "", DEFAULT_TTL, TIMEOUT_MS));
        AppException timeout = assertThrows(AppException.class,
                () -> slow.handleEvent(headers("action", "GET", "key", "k1"), null, INSTANCE));
        assertEquals(408, timeout.getStatus());
        assertEquals("Command timed out after 1 second(s)", timeout.getMessage());
        RedisCache wrongType = new RedisCache(() -> new RedisCacheStore(
                failingBackend(new RedisCommandExecutionException("WRONGTYPE Operation against a key holding the wrong kind of value")),
                "", DEFAULT_TTL, TIMEOUT_MS));
        assertThrows(RedisCommandExecutionException.class,
                () -> wrongType.handleEvent(headers("action", "GET", "key", "k1"), null, INSTANCE));
    }

    /** A backend whose every command fails with the given exception - what the store sees during an outage. */
    @SuppressWarnings("unchecked")
    private static RedisBackend<byte[]> failingBackend(RuntimeException failure) {
        RedisClusterCommands<String, byte[]> commands = (RedisClusterCommands<String, byte[]>) Proxy.newProxyInstance(
                RedisCacheTest.class.getClassLoader(), new Class<?>[] {RedisClusterCommands.class},
                (proxy, method, args) -> {
                    throw failure;
                });
        return new RedisBackend<>() {
            @Override
            public RedisClusterCommands<String, byte[]> commands() {
                return commands;
            }

            @Override
            public RedisClusterAsyncCommands<String, byte[]> async() {
                throw failure;
            }

            @Override
            public StatefulRedisPubSubConnection<String, byte[]> openPubSub() {
                throw failure;
            }

            @Override
            public boolean cluster() {
                return false;
            }

            @Override
            public void close() {
                // nothing to release
            }
        };
    }
}
