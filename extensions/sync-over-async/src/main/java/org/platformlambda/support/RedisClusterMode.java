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

/**
 * How the Redis client is selected for sync-over-async, from {@code redis.cluster.mode}:
 * <ul>
 *   <li>{@link #AUTO} (default) - probe the seed node once at start-up ({@code INFO cluster}); build a
 *       cluster client when the server reports {@code cluster_enabled:1}, otherwise a standalone client.
 *       If the probe cannot decide (e.g. {@code INFO} is restricted), fall back to standalone.</li>
 *   <li>{@link #STANDALONE} - always a single-node client (the original behaviour).</li>
 *   <li>{@link #CLUSTER} - always a cluster client, seeded from {@code redis.cluster.nodes} (or the single
 *       {@code redis.host}:{@code redis.port}, e.g. an AWS ElastiCache configuration endpoint).</li>
 * </ul>
 */
public enum RedisClusterMode {
    AUTO, STANDALONE, CLUSTER;

    /**
     * Parse the {@code redis.cluster.mode} value case-insensitively. Anything other than
     * {@code standalone} or {@code cluster} - including a blank value or an unrecognised one - resolves to
     * {@link #AUTO}, the safe self-detecting default.
     */
    public static RedisClusterMode from(String value) {
        if (value == null) {
            return AUTO;
        }
        return switch (value.trim().toLowerCase()) {
            case "cluster" -> CLUSTER;
            case "standalone" -> STANDALONE;
            default -> AUTO;
        };
    }
}
