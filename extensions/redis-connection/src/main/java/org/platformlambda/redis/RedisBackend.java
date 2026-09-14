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

import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * The Redis connection seam that hides <b>standalone vs cluster</b> from a consumer (the sync-over-async
 * return-route engine, the distributed cache). Both topologies are reached through one command type:
 * Lettuce's standalone {@code RedisCommands} and the cluster {@code RedisAdvancedClusterCommands} both
 * extend {@link RedisClusterCommands}, so every single-key command (plus {@code PUBLISH}) resolves on that
 * common interface with no per-topology branching in the caller. The same holds for the async variants,
 * which both extend {@link RedisClusterAsyncCommands}.
 *
 * <p>Keys are always {@code String}; the <b>value type {@code V}</b> is chosen by the codec the backend was
 * built with — {@code String} for sync-over-async (text payloads), opaque {@code byte[]} for the cache
 * (caller-owned serialisation, cross-language interop). A single {@link RedisBackendFactory#create(RedisConfig, io.lettuce.core.codec.RedisCodec)}
 * builds either topology for either value type; {@link RedisBackendFactory#create(RedisConfig)} is the
 * String-valued convenience.
 *
 * <p>An implementation owns one long-lived, multiplexed command connection (exposed via {@link #commands()}
 * for blocking use and {@link #async()} for pipelining) and can open pub/sub connections on demand
 * ({@link #openPubSub()} — the caller owns and closes each). Lettuce multiplexes: any number of threads
 * pipeline over the one connection and Lettuce correlates the ordered replies, so a connection pool is not
 * needed for this (non-blocking, no {@code MULTI}/{@code EXEC}) command set. Closing the backend closes the
 * command connection and, when the backend created the client, shuts the client down.
 */
public interface RedisBackend<V> extends AutoCloseable {

    /**
     * The synchronous command API of this backend's long-lived command connection — the common super-type
     * of standalone and cluster commands. Used for the single-key operations and {@code PUBLISH}.
     */
    RedisClusterCommands<String, V> commands();

    /**
     * The asynchronous command API of the same connection — the common super-type of the standalone and
     * cluster async commands. Used to pipeline a batch (e.g. the cache's bulk {@code MPUT}: fire every
     * {@code SETEX} without waiting, then await all replies) over the one multiplexed connection.
     */
    RedisClusterAsyncCommands<String, V> async();

    /**
     * Open a <b>new</b> pub/sub connection; the caller adds its listener, subscribes, and closes it.
     * The cluster variant is a subtype of the standalone one, so both return the same type here.
     */
    StatefulRedisPubSubConnection<String, V> openPubSub();

    /** @return {@code true} if this backend talks to a Redis Cluster (for start-up logging / diagnostics). */
    boolean cluster();

    @Override
    void close();
}
