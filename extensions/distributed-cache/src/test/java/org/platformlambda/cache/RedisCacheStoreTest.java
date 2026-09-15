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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cache operations against a real (embedded) Redis, over the byte[] backend the module uses in
 * production. Exercises every action plus the key-prefix namespacing and the TTL-from-birth discipline.
 */
class RedisCacheStoreTest extends RedisTestBase {

    private static final long TTL = 60;
    private static final long TIMEOUT_MS = 2000;

    private static RedisBackend<byte[]> backend;
    private static RedisClusterCommands<String, byte[]> raw;   // inspect raw (prefixed) keys / TTLs

    @BeforeAll
    static void connect() {
        // non-owning: RedisTestBase owns the client; this closes only its own connection
        backend = new StandaloneRedisBackend<>(redisClient, CacheRuntime.CODEC, false);
        raw = backend.commands();
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

    private static RedisCacheStore store(String keyPrefix) {
        return new RedisCacheStore(backend, keyPrefix, TTL, TIMEOUT_MS);
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void putThenGetRoundTripsBytesWithATtl() {
        RedisCacheStore cache = store("");
        cache.put("k1", bytes("hello"), TTL);
        assertArrayEquals(bytes("hello"), cache.get("k1"));
        // every stored key carries a TTL from birth
        assertTrue(raw.ttl("k1") > 0, "PUT must set a TTL (SETEX)");
    }

    @Test
    void getReturnsNullOnAMiss() {
        assertNull(store("").get("absent"));
    }

    @Test
    void deleteReturnsTheCountRemoved() {
        RedisCacheStore cache = store("");
        cache.put("k1", bytes("v"), TTL);
        assertEquals(1, cache.delete("k1"));
        assertEquals(0, cache.delete("k1"), "deleting an absent key removes nothing");
        assertNull(cache.get("k1"));
    }

    @Test
    void putIfAbsentStoresOnlyWhenAbsentAndIsAtomicWithTtl() {
        RedisCacheStore cache = store("");
        assertTrue(cache.putIfAbsent("k1", bytes("first"), TTL), "stored when absent");
        assertFalse(cache.putIfAbsent("k1", bytes("second"), TTL), "not stored when present");
        assertArrayEquals(bytes("first"), cache.get("k1"), "the original value is untouched");
        assertTrue(raw.ttl("k1") > 0, "SET NX EX must set a TTL atomically");
    }

    @Test
    void mgetReturnsPresentKeysAndOmitsMisses() {
        RedisCacheStore cache = store("");
        cache.put("a", bytes("1"), TTL);
        cache.put("c", bytes("3"), TTL);
        Map<String, byte[]> found = cache.mget(List.of("a", "b", "c"));
        assertEquals(2, found.size());
        assertArrayEquals(bytes("1"), found.get("a"));
        assertArrayEquals(bytes("3"), found.get("c"));
        assertFalse(found.containsKey("b"), "a miss is omitted, not a null entry");
    }

    @Test
    void mgetOnEmptyKeysIsAnEmptyMap() {
        assertTrue(store("").mget(List.of()).isEmpty());
    }

    @Test
    void mputPipelinesEveryEntryWithItsTtl() {
        RedisCacheStore cache = store("");
        cache.mput(Map.of("a", bytes("1"), "b", bytes("2"), "c", bytes("3")), TTL);
        Map<String, byte[]> found = cache.mget(List.of("a", "b", "c"));
        assertEquals(3, found.size());
        assertArrayEquals(bytes("2"), found.get("b"));
        // MPUT is TTL-preserving (per-entry SETEX), unlike a raw MSET
        assertTrue(raw.ttl("a") > 0, "each MPUT entry must carry a TTL");
        assertTrue(raw.ttl("c") > 0);
    }

    @Test
    void mputOnEmptyMapIsANoOp() {
        RedisCacheStore cache = store("");
        assertDoesNotThrow(() -> cache.mput(Map.of(), TTL));
    }

    @Test
    void listPushPopLenAreFifoWithATtl() {
        RedisCacheStore cache = store("");
        assertEquals(1, cache.listPush("q", bytes("first"), TTL));
        assertEquals(2, cache.listPush("q", bytes("second"), TTL));
        assertEquals(2, cache.listLen("q"));
        // RPUSH + EXPIRE is atomic, so the list key is never left TTL-less
        assertTrue(raw.ttl("q") > 0, "LIST_PUSH must set a TTL atomically");
        // FIFO: pop returns the oldest first
        assertArrayEquals(bytes("first"), cache.listPop("q"));
        assertArrayEquals(bytes("second"), cache.listPop("q"));
        assertNull(cache.listPop("q"), "popping an empty list returns null");
        assertEquals(0, cache.listLen("q"), "a drained list ceases to exist");
    }

    @Test
    void keyPrefixNamespacesEveryKeyAndIsStrippedFromMget() {
        RedisCacheStore cache = store("app1:");
        cache.put("k1", bytes("v"), TTL);
        // the raw Redis key is prefixed; the unprefixed key does not exist
        assertArrayEquals(bytes("v"), raw.get("app1:k1"));
        assertNull(raw.get("k1"));
        // callers use unprefixed keys throughout, and MGET returns them unprefixed
        assertArrayEquals(bytes("v"), cache.get("k1"));
        Map<String, byte[]> found = cache.mget(List.of("k1"));
        assertTrue(found.containsKey("k1"), "MGET keys are returned unprefixed");
        assertArrayEquals(bytes("v"), found.get("k1"));
    }
}
