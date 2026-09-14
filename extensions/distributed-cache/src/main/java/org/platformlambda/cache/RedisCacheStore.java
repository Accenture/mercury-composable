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

import io.lettuce.core.KeyValue;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import org.platformlambda.redis.RedisBackend;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The cache operations over a {@link RedisBackend} whose values are opaque {@code byte[]} — the caller owns
 * serialisation, which maximises cross-layer and cross-language interop (design spec §4.4, Q3). Keys are
 * {@code String}, each transparently namespaced by an optional application key-prefix (Q6) so several apps
 * can share one Redis without colliding; the prefix is stripped again on the way out of {@code MGET}.
 *
 * <p>Every operation is <b>cluster-safe by construction</b> (spec §4.5): all single-key ops route to their
 * slot as-is; {@code MGET} keys may span slots and Lettuce's cluster client scatter-gathers them; {@code MPUT}
 * is a pipelined batch of single-key {@code SETEX} (each routes to its own slot, non-atomic across the map —
 * correct for a cache, and unavoidable on a cluster). Every stored key carries a <b>TTL from creation</b>:
 * {@code SETEX} for values, atomic {@code SET NX EX} for put-if-absent, an atomic {@code RPUSH}+{@code EXPIRE}
 * Lua step for list push (never a two-command sequence that could leave a TTL-less key if the client died
 * between them — the same discipline the sync-over-async return route follows).
 *
 * <p>Thread-safe: Lettuce multiplexes over the one shared connection, so the shared command handles are used
 * concurrently by every worker instance.
 */
public class RedisCacheStore {

    // Append + TTL as ONE atomic server-side step, returning the new list length. Two discrete commands would
    // leave a TTL-less list key if the client died between them - every cache key carries a TTL from birth.
    private static final String RPUSH_EXPIRE =
            "local n = redis.call('RPUSH', KEYS[1], ARGV[1]); redis.call('EXPIRE', KEYS[1], ARGV[2]); return n";
    private static final String OK = "OK";

    private final RedisBackend<byte[]> backend;
    private final RedisClusterCommands<String, byte[]> commands;
    private final String keyPrefix;
    private final long defaultTtlSeconds;
    private final long timeoutMs;

    /**
     * @param backend           the standalone-or-cluster byte[] backend (one shared, multiplexed connection)
     * @param keyPrefix         prepended to every key; blank = no prefix
     * @param defaultTtlSeconds default TTL for writes that do not specify one
     * @param timeoutMs         bounds the {@code MPUT} pipeline's await-all
     */
    public RedisCacheStore(RedisBackend<byte[]> backend, String keyPrefix, long defaultTtlSeconds, long timeoutMs) {
        this.backend = backend;
        this.commands = backend.commands();
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
        this.defaultTtlSeconds = defaultTtlSeconds;
        this.timeoutMs = timeoutMs;
    }

    /** The default TTL (seconds) applied when a write omits one — resolved by the function from its config. */
    public long defaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    /** {@code SETEX key ttl value}. */
    public void put(String key, byte[] value, long ttlSeconds) {
        commands.setex(prefixed(key), ttlSeconds, value);
    }

    /** {@code GET key}. @return the value, or {@code null} on a miss. */
    public byte[] get(String key) {
        return commands.get(prefixed(key));
    }

    /**
     * {@code MGET k1 k2 ...} — misses omitted. On a cluster the keys may span slots; Lettuce's cluster client
     * scatter-gathers the request. Insertion order follows the requested keys (a {@link LinkedHashMap}).
     */
    public Map<String, byte[]> mget(List<String> keys) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        if (keys == null || keys.isEmpty()) {
            return result;
        }
        String[] prefixed = keys.stream().map(this::prefixed).toArray(String[]::new);
        List<KeyValue<String, byte[]>> values = commands.mget(prefixed);
        for (KeyValue<String, byte[]> kv : values) {
            if (kv.hasValue()) {
                result.put(unprefixed(kv.getKey()), kv.getValue());
            }
        }
        return result;
    }

    /**
     * Bulk write as a <b>pipelined</b> batch of single-key {@code SETEX} — one round trip, each key keeping
     * its TTL (raw {@code MSET} sets none). Non-atomic across the map, and each key routes to its own slot, so
     * the map may span cluster slots freely. Commands are fired without waiting (Lettuce multiplexes and
     * auto-flushes), then all replies are awaited together.
     */
    public void mput(Map<String, byte[]> entries, long ttlSeconds) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        RedisClusterAsyncCommands<String, byte[]> async = backend.async();
        List<RedisFuture<String>> futures = new ArrayList<>(entries.size());
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            futures.add(async.setex(prefixed(entry.getKey()), ttlSeconds, entry.getValue()));
        }
        boolean completed = LettuceFutures.awaitAll(timeoutMs, TimeUnit.MILLISECONDS,
                futures.toArray(new RedisFuture[0]));
        if (!completed) {
            throw new IllegalStateException(
                    "MPUT timed out after " + timeoutMs + "ms for " + entries.size() + " entries");
        }
    }

    /** {@code DEL key}. @return the number of keys removed (0 or 1). */
    public long delete(String key) {
        Long removed = commands.del(prefixed(key));
        return removed == null ? 0 : removed;
    }

    /**
     * {@code SET key value NX EX ttl} — atomic put-if-absent with a TTL in one command (not {@code SETNX}
     * then {@code EXPIRE}, which leaves a TTL-less key if the process dies between them).
     *
     * @return {@code true} if the key was stored, {@code false} if it already existed
     */
    public boolean putIfAbsent(String key, byte[] value, long ttlSeconds) {
        String reply = commands.set(prefixed(key), value, SetArgs.Builder.nx().ex(ttlSeconds));
        return OK.equals(reply);
    }

    /**
     * {@code RPUSH key value} then {@code EXPIRE key ttl} as one atomic Lua step (so the list key is never
     * left TTL-less). @return the new list length.
     */
    public long listPush(String key, byte[] value, long ttlSeconds) {
        byte[] ttlArg = Long.toString(ttlSeconds).getBytes(StandardCharsets.UTF_8);
        Long length = commands.eval(RPUSH_EXPIRE, ScriptOutputType.INTEGER,
                new String[]{prefixed(key)}, value, ttlArg);
        return length == null ? 0 : length;
    }

    /** {@code LPOP key} — destructive. @return the oldest value, or {@code null} when the list is empty. */
    public byte[] listPop(String key) {
        return commands.lpop(prefixed(key));
    }

    /** {@code LLEN key}. @return the list length (0 for an absent list). */
    public long listLen(String key) {
        Long length = commands.llen(prefixed(key));
        return length == null ? 0 : length;
    }

    private String prefixed(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Missing 'key'");
        }
        return keyPrefix.isEmpty() ? key : keyPrefix + key;
    }

    private String unprefixed(String key) {
        return keyPrefix.isEmpty() || !key.startsWith(keyPrefix) ? key : key.substring(keyPrefix.length());
    }
}
