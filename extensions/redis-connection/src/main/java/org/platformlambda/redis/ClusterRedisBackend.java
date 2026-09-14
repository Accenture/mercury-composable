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

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * {@link RedisBackend} over a {@link RedisClusterClient}. {@code connect(codec)} discovers the shard
 * topology from the seed node(s) — so a single seed (an AWS ElastiCache configuration endpoint) is enough —
 * and the client routes each single-key command to the node owning its slot and follows {@code MOVED}/
 * {@code ASK} redirects. {@code commands()} returns {@code RedisAdvancedClusterCommands} and {@code async()}
 * its async twin, which <em>are a</em> {@link RedisClusterCommands} / {@link RedisClusterAsyncCommands};
 * classic {@code PUBLISH} still reaches a subscriber via the cluster bus. Consumers keep every operation
 * single-key (a cross-slot {@code MGET} is scatter-gathered by the client; a bulk write pipelines one
 * single-key {@code SETEX} per entry), so no command ever spans two slots.
 */
public class ClusterRedisBackend<V> implements RedisBackend<V> {

    private final RedisClusterClient client;
    private final RedisCodec<String, V> codec;
    private final StatefulRedisClusterConnection<String, V> connection;

    public ClusterRedisBackend(RedisClusterClient client, RedisCodec<String, V> codec) {
        this.client = client;
        this.codec = codec;
        this.connection = client.connect(codec);
    }

    @Override
    public RedisClusterCommands<String, V> commands() {
        return connection.sync();
    }

    @Override
    public RedisClusterAsyncCommands<String, V> async() {
        return connection.async();
    }

    @Override
    public StatefulRedisPubSubConnection<String, V> openPubSub() {
        return client.connectPubSub(codec);
    }

    @Override
    public boolean cluster() {
        return true;
    }

    @Override
    public void close() {
        connection.close();
        client.close();
    }
}
