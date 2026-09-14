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

import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * The Redis connection seam that hides <b>standalone vs cluster</b> from the return-route engine. Both
 * topologies are reached through one command type: Lettuce's standalone {@code RedisCommands} and the
 * cluster {@code RedisAdvancedClusterCommands} both extend {@link RedisClusterCommands}, so every command
 * this module uses (all single-key, plus {@code PUBLISH}) resolves on that common interface with no
 * per-topology branching in the caller.
 *
 * <p>An implementation owns one long-lived command connection (exposed via {@link #commands()}) and can
 * open pub/sub connections on demand ({@link #openPubSub()} - the caller owns and closes each). Closing the
 * backend closes the command connection and, when the backend created the client, shuts the client down.
 */
public interface RedisBackend extends AutoCloseable {

    /**
     * The synchronous command API of this backend's long-lived command connection - the common super-type
     * of standalone and cluster commands. Used for the store's key operations and {@code PUBLISH}.
     */
    RedisClusterCommands<String, String> commands();

    /**
     * Open a <b>new</b> pub/sub connection; the caller adds its listener, subscribes, and closes it.
     * The cluster variant is a subtype of the standalone one, so both return the same type here.
     */
    StatefulRedisPubSubConnection<String, String> openPubSub();

    /** @return {@code true} if this backend talks to a Redis Cluster (for start-up logging / diagnostics). */
    boolean cluster();

    @Override
    void close();
}
