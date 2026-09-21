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

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * {@link RedisBackend} over a single-node {@link RedisClient} — the original topology. {@code commands()}
 * is the facade over the connection's {@code RedisCommands}, which <em>is a</em> {@code RedisClusterCommands};
 * likewise {@code async()} over its {@code RedisAsyncCommands}. The connection (and every pub/sub connection)
 * is opened with the supplied {@link RedisCodec}, so the value type is whatever the codec encodes
 * ({@code String} or opaque {@code byte[]}). The command connection is reset after a command timeout by the
 * {@link ResettableRedisBackend} base.
 *
 * <p>{@code ownsClient} governs {@link #close()}: a backend built by {@link RedisBackendFactory} owns the
 * client it created and shuts it down on close; a backend wrapping a client owned by someone else (a test
 * sharing one client across pods) closes only its own command connection and leaves the client alone.
 */
public class StandaloneRedisBackend<V> extends ResettableRedisBackend<V, StatefulRedisConnection<String, V>> {
    private final RedisClient client;
    private final RedisCodec<String, V> codec;
    private final boolean ownsClient;

    /** Own the client (the factory path): {@link #close()} shuts it down. */
    public StandaloneRedisBackend(RedisClient client, RedisCodec<String, V> codec) {
        this(client, codec, true);
    }

    public StandaloneRedisBackend(RedisClient client, RedisCodec<String, V> codec, boolean ownsClient) {
        super(() -> client.connect(codec), StatefulRedisConnection::sync, StatefulRedisConnection::async);
        this.client = client;
        this.codec = codec;
        this.ownsClient = ownsClient;
    }

    @Override
    public StatefulRedisPubSubConnection<String, V> openPubSub() {
        return client.connectPubSub(codec);
    }

    @Override
    public boolean cluster() {
        return false;
    }

    @Override
    public void close() {
        super.close();
        if (ownsClient) {
            client.close();
        }
    }
}
