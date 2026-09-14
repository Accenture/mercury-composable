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
import io.lettuce.core.cluster.RedisClusterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the {@link RedisBackend} for a {@link RedisConfig}, choosing standalone or cluster from the two
 * cluster keys: when {@link RedisConfig#autoDetectCluster()} ({@code soa.redis.cluster.detect=auto}) it
 * probes the seed node once ({@code INFO} -> {@code cluster_enabled:1}); otherwise the boolean
 * {@link RedisConfig#clusterEnabled()} ({@code soa.redis.cluster.mode}) decides. If the auto-detect probe
 * cannot decide (for example when {@code INFO} is restricted on a managed Redis), it falls back to that same
 * boolean. Credentials and TLS are carried by the config for both topologies, so nothing here is auth-aware.
 */
public final class RedisBackendFactory {
    private static final Logger log = LoggerFactory.getLogger(RedisBackendFactory.class);

    private RedisBackendFactory() {}

    public static RedisBackend create(RedisConfig config) {
        boolean cluster = config.autoDetectCluster()
                ? detectCluster(config, config.clusterEnabled())
                : config.clusterEnabled();
        if (cluster) {
            return new ClusterRedisBackend(RedisClusterClient.create(config.seedUris()));
        }
        return new StandaloneRedisBackend(RedisClient.create(config.toUri()));
    }

    /**
     * Probe the seed node to decide whether it is a cluster. A dedicated short-lived client runs
     * {@code INFO cluster}; {@code cluster_enabled:1} means cluster. Any failure (unreachable, or
     * {@code INFO} not permitted) is inconclusive and resolves to the configured {@code cluster.mode}
     * boolean - the caller's own connection then surfaces a genuine outage, and the health check's
     * waiting semantics still apply.
     */
    private static boolean detectCluster(RedisConfig config, boolean fallback) {
        RedisClient probe = RedisClient.create(config.toUri());
        try (StatefulRedisConnection<String, String> c = probe.connect()) {
            String info = c.sync().info("cluster");
            boolean cluster = info != null && info.contains("cluster_enabled:1");
            log.debug("Redis auto-detect at {}:{} -> {}", config.host(), config.port(),
                    cluster ? "cluster" : "standalone");
            return cluster;
        } catch (RuntimeException e) {
            log.debug("Redis cluster auto-detect at {}:{} inconclusive ({}); using configured cluster.mode={}",
                    config.host(), config.port(), e.toString(), fallback);
            return fallback;
        } finally {
            probe.close();
        }
    }
}
