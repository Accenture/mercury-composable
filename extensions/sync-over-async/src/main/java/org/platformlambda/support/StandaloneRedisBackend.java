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

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * {@link RedisBackend} over a single-node {@link RedisClient} - the original topology. {@code commands()}
 * returns the connection's {@code RedisCommands}, which <em>is a</em> {@link RedisClusterCommands}.
 *
 * <p>{@code ownsClient} governs {@link #close()}: a backend built by {@link RedisBackendFactory} owns the
 * client it created and shuts it down on close; a backend wrapping a client owned by someone else (a test
 * sharing one client across pods) closes only its own command connection and leaves the client alone.
 */
public class StandaloneRedisBackend implements RedisBackend {

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final boolean ownsClient;

    /** Own the client (the factory path): {@link #close()} shuts it down. */
    public StandaloneRedisBackend(RedisClient client) {
        this(client, true);
    }

    public StandaloneRedisBackend(RedisClient client, boolean ownsClient) {
        this.client = client;
        this.connection = client.connect();
        this.ownsClient = ownsClient;
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
        return false;
    }

    @Override
    public void close() {
        connection.close();
        if (ownsClient) {
            client.close();
        }
    }
}
