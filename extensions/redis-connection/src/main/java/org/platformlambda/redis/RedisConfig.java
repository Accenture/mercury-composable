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

import io.lettuce.core.RedisURI;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis connection startup parameters, read from {@code application.properties} as discrete keys
 * (resolved through {@link ConfigBase}, so {@code ${ENV_VAR:default}} substitution applies — keep secrets
 * like the password out of the file via {@code ${REDIS_PASSWORD}}, populated by a credential bootstrap
 * that runs in a lower-sequence {@code @MainApplication} before the consumer starts).
 *
 * <p><b>Configurable key prefix.</b> The record itself holds resolved values and is prefix-agnostic; the
 * {@link #from(ConfigBase, String)} loader reads {@code <prefix>*} keys. Two consumers share this one
 * loader with different prefixes so they never collide in the same application:
 * <ul>
 *   <li>{@code from(config, RedisConfig.SOA_PREFIX)} — sync-over-async's {@code soa.redis.*} namespace;</li>
 *   <li>{@code from(config, RedisConfig.BASE_PREFIX)} — the distributed cache's plain {@code redis.*} namespace.</li>
 * </ul>
 * {@link #from(ConfigBase)} defaults to {@link #SOA_PREFIX} (unchanged behaviour for existing callers).
 * <b>Each key falls back to the un-prefixed base {@code redis.*} form</b> when the prefixed one is absent
 * (the config reader's nested-default, {@code get(prefixKey, get(redisKey, default))}), so a pre-existing
 * {@code redis.*} deployment keeps working with no migration, and a deployment can run one Redis for both
 * consumers (set {@code redis.*}) or decouple them (also set {@code soa.redis.*}). When the prefix is
 * already {@code redis.} the prefixed and base keys coincide.
 *
 * <pre>
 * redis.host=127.0.0.1
 * redis.port=6379
 * redis.username=${REDIS_USERNAME:}   # blank = default user; set for an ACL/RBAC user (e.g. AWS ElastiCache RBAC)
 * redis.password=${REDIS_PASSWORD:}   # blank = no auth
 * redis.ssl=false                     # true = rediss:// (required for AUTH on ElastiCache)
 * redis.database=0                    # standalone only; a cluster is database 0
 * redis.timeout.ms=5000               # default command timeout
 * redis.cluster.detect=auto           # auto = detect at start-up; anything else = decide by the boolean below
 * redis.cluster.mode=false            # true = cluster, false = standalone (used when detect is not 'auto',
 *                                     #   and as the fallback when auto-detection is inconclusive)
 * redis.cluster.nodes=                # cluster seeds host:port,host:port (blank = use redis.host:redis.port)
 * </pre>
 *
 * <p><b>Cluster selection (two keys).</b> {@code <prefix>cluster.detect=auto} (the default) probes the seed
 * at start-up and picks cluster or standalone from what the server reports; otherwise the boolean
 * {@code <prefix>cluster.mode} ({@code true}/{@code false}) decides. The boolean form matches a common cache
 * convention. When detect is {@code auto} but the probe cannot decide (e.g. {@code INFO} restricted), the
 * {@code cluster.mode} boolean is the fallback.
 *
 * <p>Authentication is identical for standalone and cluster: AWS applies one AUTH token or RBAC user to the
 * whole replication group, and the cluster client sends it on every node connection. The credential values
 * come from {@code application.properties} (typically {@code ${ENV_VAR}} placeholders a vault-backed
 * bootstrap publishes first); nothing here is vendor-specific.
 *
 * @param host              Redis host (or the cluster configuration endpoint for a single-seed cluster).
 * @param port              Redis port.
 * @param username          ACL/RBAC username; blank/null = the default user (password-only or no auth).
 * @param password          auth password; blank/null = no authentication.
 * @param ssl               use TLS ({@code rediss://}).
 * @param database          logical database index (standalone only; ignored for cluster).
 * @param timeoutMs         default command timeout in milliseconds.
 * @param autoDetectCluster probe the seed at start-up to choose cluster vs standalone
 *                          ({@code <prefix>cluster.detect=auto}).
 * @param clusterEnabled    when not auto-detecting (or when detection is inconclusive), {@code true} = cluster
 *                          client, {@code false} = standalone ({@code <prefix>cluster.mode}).
 * @param clusterNodes      comma-separated {@code host:port} cluster seeds; blank = the single {@code host:port}.
 */
public record RedisConfig(String host, int port, String username, String password, boolean ssl,
                          int database, long timeoutMs, boolean autoDetectCluster, boolean clusterEnabled,
                          String clusterNodes) {

    /** sync-over-async's key namespace ({@code soa.redis.*}). */
    public static final String SOA_PREFIX = "soa.redis.";
    /** The base namespace ({@code redis.*}) — the distributed cache's prefix, and the universal fallback. */
    public static final String BASE_PREFIX = "redis.";

    private static final String AUTO = "auto";

    // un-prefixed key names, appended to a prefix (e.g. SOA_PREFIX + HOST_KEY = "soa.redis.host").
    // The _KEY suffix keeps these distinct from the record's like-named components (Sonar S1845).
    private static final String HOST_KEY = "host";
    private static final String PORT_KEY = "port";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final String SSL_KEY = "ssl";
    private static final String DATABASE_KEY = "database";
    private static final String TIMEOUT_MS_KEY = "timeout.ms";
    private static final String CLUSTER_DETECT_KEY = "cluster.detect";
    private static final String CLUSTER_MODE_KEY = "cluster.mode";
    private static final String CLUSTER_NODES_KEY = "cluster.nodes";

    /**
     * Backward-compatible standalone constructor (default user, no cluster, no probe): keeps callers and
     * tests that predate cluster support working unchanged.
     */
    public RedisConfig(String host, int port, String password, boolean ssl, int database, long timeoutMs) {
        this(host, port, "", password, ssl, database, timeoutMs, false, false, "");
    }

    /** Default entry point: the {@link #SOA_PREFIX} namespace with a {@link #BASE_PREFIX} fallback. */
    public static RedisConfig from(ConfigBase config) {
        return from(config, SOA_PREFIX);
    }

    /**
     * Read the connection parameters from the {@code <prefix>*} keys, each falling back to the un-prefixed
     * base {@code redis.*} form when the prefixed key is absent. {@code from(config, SOA_PREFIX)} reads
     * sync-over-async's namespace, {@code from(config, BASE_PREFIX)} the plain base namespace the cache uses.
     */
    public static RedisConfig from(ConfigBase config, String prefix) {
        Utility util = Utility.getInstance();
        // Each key prefers the <prefix> form and falls back to the un-prefixed redis.* form when absent
        // (the config reader's own nested-default pattern), so an existing redis.* deployment keeps working
        // and two consumers can decouple by setting their prefixed keys.
        boolean autoDetect = AUTO.equalsIgnoreCase(get(config, prefix, CLUSTER_DETECT_KEY, AUTO));
        boolean clusterEnabled = "true".equalsIgnoreCase(get(config, prefix, CLUSTER_MODE_KEY, "false"));
        return new RedisConfig(
                get(config, prefix, HOST_KEY, "127.0.0.1"),
                util.str2int(get(config, prefix, PORT_KEY, "6379")),
                get(config, prefix, USERNAME_KEY, ""),
                get(config, prefix, PASSWORD_KEY, ""),
                "true".equalsIgnoreCase(get(config, prefix, SSL_KEY, "false")),
                util.str2int(get(config, prefix, DATABASE_KEY, "0")),
                util.str2long(get(config, prefix, TIMEOUT_MS_KEY, "5000")),
                autoDetect,
                clusterEnabled,
                get(config, prefix, CLUSTER_NODES_KEY, ""));
    }

    /**
     * {@code <prefix><suffix>}, falling back to the base {@code redis.<suffix>} when the prefixed key is
     * absent. When {@code prefix} is already {@code "redis."} the two keys coincide and the base is read
     * directly.
     */
    private static String get(ConfigBase config, String prefix, String suffix, String def) {
        return config.getProperty(prefix + suffix, config.getProperty(BASE_PREFIX + suffix, def));
    }

    /** A copy with the command timeout overridden — the health check bounds its probe with its own timeout. */
    public RedisConfig withTimeout(long newTimeoutMs) {
        return new RedisConfig(host, port, username, password, ssl, database, newTimeoutMs,
                autoDetectCluster, clusterEnabled, clusterNodes);
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
     * Cluster seed URIs: the explicit {@code cluster.nodes} list ({@code host:port,host:port}) when set, else
     * the single {@code host}:{@code port} (an AWS ElastiCache configuration endpoint is one seed — the client
     * discovers the shards from it). Redis Cluster is database 0 only, so no database index is applied; auth
     * and TLS carry over identically to every node.
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
