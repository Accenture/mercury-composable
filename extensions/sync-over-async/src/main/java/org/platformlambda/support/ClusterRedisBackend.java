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

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * {@link RedisBackend} over a {@link RedisClusterClient}. {@code connect()} discovers the shard topology
 * from the seed node(s) - so a single seed (an AWS ElastiCache configuration endpoint) is enough - and the
 * client routes each single-key command to the node owning its slot and follows {@code MOVED}/{@code ASK}
 * redirects. {@code commands()} returns {@code RedisAdvancedClusterCommands}, which <em>is a</em>
 * {@link RedisClusterCommands}; classic {@code PUBLISH} still reaches the pod's subscriber via the cluster
 * bus. The correlation-id keys the module writes are single-key operations, and its one two-key delete is
 * split so no command ever spans two slots.
 */
public class ClusterRedisBackend implements RedisBackend {

    private final RedisClusterClient client;
    private final StatefulRedisClusterConnection<String, String> connection;

    public ClusterRedisBackend(RedisClusterClient client) {
        this.client = client;
        this.connection = client.connect();
    }

    @Override
    public RedisClusterCommands<String, String> commands() {
        return connection.sync();
    }

    @Override
    public StatefulRedisPubSubConnection<String, String> openPubSub() {
        return client.connectPubSub();
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
