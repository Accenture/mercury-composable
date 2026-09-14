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

import io.lettuce.core.RedisURI;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis connection startup parameters, read from {@code application.properties} as discrete keys
 * (resolved through {@link ConfigBase}, so {@code ${ENV_VAR:default}} substitution applies - keep secrets
 * like {@code redis.password} out of the file via {@code ${REDIS_PASSWORD}}, populated by a credential
 * bootstrap that runs in a lower-sequence {@code @MainApplication} before sync-over-async starts):
 *
 * <pre>
 * redis.host=127.0.0.1
 * redis.port=6379
 * redis.username=${REDIS_USERNAME:}   # blank = default user; set for an ACL/RBAC user (e.g. AWS ElastiCache RBAC)
 * redis.password=${REDIS_PASSWORD:}   # blank = no auth
 * redis.ssl=false                     # true = rediss:// (required for AUTH on ElastiCache)
 * redis.database=0                    # standalone only; a cluster is database 0
 * redis.timeout.ms=5000               # default command timeout
 * redis.cluster.mode=auto             # auto | standalone | cluster
 * redis.cluster.nodes=                # cluster seeds host:port,host:port (blank = use redis.host:redis.port)
 * </pre>
 *
 * <p>Authentication is identical for standalone and cluster: AWS applies one AUTH token or RBAC user to the
 * whole replication group, and the cluster client sends it on every node connection. The credential values
 * come from {@code application.properties} (typically {@code ${ENV_VAR}} placeholders a vault-backed
 * bootstrap publishes first); nothing here is vendor-specific.
 *
 * @param host         Redis host (or the cluster configuration endpoint for a single-seed cluster).
 * @param port         Redis port.
 * @param username     ACL/RBAC username; blank/null = the default user (password-only or no auth).
 * @param password     auth password; blank/null = no authentication.
 * @param ssl          use TLS ({@code rediss://}).
 * @param database     logical database index (standalone only; ignored for cluster).
 * @param timeoutMs    default command timeout in milliseconds.
 * @param clusterMode  standalone / cluster / auto-detect selection.
 * @param clusterNodes comma-separated {@code host:port} cluster seeds; blank = the single {@code host:port}.
 */
public record RedisConfig(String host, int port, String username, String password, boolean ssl,
                          int database, long timeoutMs, RedisClusterMode clusterMode, String clusterNodes) {

    private static final String HOST_KEY = "redis.host";
    private static final String PORT_KEY = "redis.port";
    private static final String USERNAME_KEY = "redis.username";
    private static final String PASSWORD_KEY = "redis.password";
    private static final String SSL_KEY = "redis.ssl";
    private static final String DATABASE_KEY = "redis.database";
    private static final String TIMEOUT_MS_KEY = "redis.timeout.ms";
    private static final String CLUSTER_MODE_KEY = "redis.cluster.mode";
    private static final String CLUSTER_NODES_KEY = "redis.cluster.nodes";

    /**
     * Backward-compatible standalone constructor (default user, no cluster): keeps callers and tests that
     * predate cluster support working unchanged. Explicitly {@link RedisClusterMode#STANDALONE} so a bare
     * {@code new RedisConfig(...)} never triggers auto-detection.
     */
    public RedisConfig(String host, int port, String password, boolean ssl, int database, long timeoutMs) {
        this(host, port, "", password, ssl, database, timeoutMs, RedisClusterMode.STANDALONE, "");
    }

    public static RedisConfig from(ConfigBase config) {
        Utility util = Utility.getInstance();
        return new RedisConfig(
                config.getProperty(HOST_KEY, "127.0.0.1"),
                util.str2int(config.getProperty(PORT_KEY, "6379")),
                config.getProperty(USERNAME_KEY, ""),
                config.getProperty(PASSWORD_KEY, ""),
                "true".equalsIgnoreCase(config.getProperty(SSL_KEY, "false")),
                util.str2int(config.getProperty(DATABASE_KEY, "0")),
                util.str2long(config.getProperty(TIMEOUT_MS_KEY, "5000")),
                RedisClusterMode.from(config.getProperty(CLUSTER_MODE_KEY, "auto")),
                config.getProperty(CLUSTER_NODES_KEY, ""));
    }

    /** A copy with the command timeout overridden - the health check bounds its probe with its own timeout. */
    public RedisConfig withTimeout(long newTimeoutMs) {
        return new RedisConfig(host, port, username, password, ssl, database, newTimeoutMs, clusterMode, clusterNodes);
    }

    /** Map the discrete parameters onto a single-node Lettuce {@link RedisURI} (standalone client / probe). */
    public RedisURI toUri() {
        return applyAuth(RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withSsl(ssl)
                .withDatabase(database)
                .withTimeout(Duration.ofMillis(timeoutMs))).build();
    }

    /**
     * Cluster seed URIs: the explicit {@code redis.cluster.nodes} list ({@code host:port,host:port}) when
     * set, else the single {@code redis.host}:{@code redis.port} (an AWS ElastiCache configuration endpoint
     * is one seed - the client discovers the shards from it). Redis Cluster is database 0 only, so no
     * database index is applied; auth and TLS carry over identically to every node.
     */
    public List<RedisURI> seedUris() {
        List<RedisURI> uris = new ArrayList<>();
        if (clusterNodes != null && !clusterNodes.isBlank()) {
            for (String node : clusterNodes.split(",")) {
                String trimmed = node.trim();
                if (!trimmed.isEmpty()) {
                    uris.add(seedUri(trimmed));
                }
            }
        }
        if (uris.isEmpty()) {
            uris.add(seedUri(host + ":" + port));
        }
        return uris;
    }

    private RedisURI seedUri(String hostPort) {
        int colon = hostPort.lastIndexOf(':');
        String h = colon > 0 ? hostPort.substring(0, colon) : hostPort;
        int p = colon > 0 ? Utility.getInstance().str2int(hostPort.substring(colon + 1)) : port;
        return applyAuth(RedisURI.builder()
                .withHost(h)
                .withPort(p)
                .withSsl(ssl)
                .withTimeout(Duration.ofMillis(timeoutMs))).build();
    }

    private RedisURI.Builder applyAuth(RedisURI.Builder builder) {
        if (username != null && !username.isBlank()) {
            builder.withAuthentication(username, password == null ? new char[0] : password.toCharArray());
        } else if (password != null && !password.isBlank()) {
            builder.withPassword(password.toCharArray());
        }
        return builder;
    }
}
