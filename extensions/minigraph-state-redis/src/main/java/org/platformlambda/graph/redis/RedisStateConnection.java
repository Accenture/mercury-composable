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

package org.platformlambda.graph.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Lazily initialized, shared Redis connection for the graph state-store functions.
 * <p>
 * The connection is created on first use, so an application that includes this
 * extension boots normally even when Redis is unreachable - the first suspend or
 * resume then fails loudly instead. Lettuce reconnects automatically after an outage.
 * <p>
 * Configuration keys (shared with the sync-over-async extension, so an application
 * configures Redis once): 'redis.host', 'redis.port', 'redis.password', 'redis.ssl',
 * 'redis.database', 'redis.timeout.ms' - all resolvable through the usual
 * ${ENV_VAR:default} substitution, e.g. redis.password=${REDIS_PASSWORD:}.
 */
class RedisStateConnection {
    private static final Logger log = LoggerFactory.getLogger(RedisStateConnection.class);
    // one shared namespace so a workflow suspended by one application instance
    // can resume on any other instance sharing the same Redis
    static final String KEY_PREFIX = "graph:state:";
    private static final String REDIS_VERSION = "redis_version:";
    private static final String UNKNOWN = "unknown";
    private static final Object SAFETY = new Object();
    private static RedisClient client;
    private static StatefulRedisConnection<String, byte[]> connection;
    // Whether the connected server supports GETDEL (Redis 6.2+). Enterprise deployments rarely
    // control their managed Redis version (AWS/Azure/GCP), and the redis-standalone Windows binary
    // is 5.0.14 - detected once per connection from INFO server.
    private static volatile boolean nativeGetdel = true;

    private RedisStateConnection() {}

    static RedisCommands<String, byte[]> commands() {
        synchronized (SAFETY) {
            if (connection == null || !connection.isOpen()) {
                connect();
            }
            return connection.sync();
        }
    }

    /**
     * Consume-on-retrieve: return the record under the key and delete it ATOMICALLY - the
     * at-most-once resume guarantee (a duplicate resume request must find nothing). A Redis 6.2+
     * server uses native GETDEL; an older server gets the same guarantee from a MULTI/EXEC
     * transaction pairing GET with DEL (supported since Redis 1.2). The strategy is chosen per
     * connection from INFO server.
     *
     * @param key the fully-prefixed record key
     * @return the stored value, or null when absent or expired
     */
    static byte[] consume(String key) {
        var cmd = commands();
        if (nativeGetdel) {
            return cmd.getdel(key);
        }
        // the shared connection's MULTI state must not interleave with another thread's commands,
        // so the whole transaction runs under the connection lock (suspend/resume traffic is
        // human-checkpoint scale - serializing the fallback path costs nothing observable)
        synchronized (SAFETY) {
            cmd.multi();
            try {
                cmd.get(key);
                cmd.del(key);
            } catch (RuntimeException e) {
                cmd.discard();   // leave the shared connection clean before failing loudly
                throw e;
            }
            return cmd.exec().get(0);
        }
    }

    /**
     * Extract the server version from {@code INFO server} output, or "unknown" when absent.
     * Visible for testing.
     */
    static String redisVersion(String serverInfo) {
        for (String line : serverInfo.split("\n")) {
            if (line.startsWith(REDIS_VERSION)) {
                return line.substring(REDIS_VERSION.length()).trim();
            }
        }
        return UNKNOWN;
    }

    /**
     * GETDEL needs Redis 6.2 or later; an unparseable version selects the transactional fallback,
     * which works on every server. Visible for testing.
     */
    static boolean supportsGetdel(String version) {
        var parts = version.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return major > 6 || (major == 6 && minor >= 2);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Force the consume strategy (true = native GETDEL, false = transactional). Visible for testing. */
    static void overrideConsumeStrategy(boolean useGetdel) {
        nativeGetdel = useGetdel;
    }

    private static void connect() {
        var util = Utility.getInstance();
        var config = AppConfigReader.getInstance();
        var host = config.getProperty("redis.host", "127.0.0.1");
        var port = util.str2int(config.getProperty("redis.port", "6379"));
        var password = config.getProperty("redis.password", "");
        var ssl = "true".equalsIgnoreCase(config.getProperty("redis.ssl", "false"));
        var database = util.str2int(config.getProperty("redis.database", "0"));
        var timeoutMs = util.str2long(config.getProperty("redis.timeout.ms", "5000"));
        var builder = RedisURI.builder().withHost(host).withPort(port).withSsl(ssl)
                .withDatabase(database).withTimeout(Duration.ofMillis(timeoutMs));
        if (!password.isBlank()) {
            builder.withPassword(password.toCharArray());
        }
        if (client == null) {
            client = RedisClient.create(builder.build());
            Runtime.getRuntime().addShutdownHook(new Thread(RedisStateConnection::shutdown));
        }
        connection = client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        // version-aware consume strategy: enterprise Redis versions are outside our control,
        // so detect GETDEL support (6.2+) instead of assuming it
        String version;
        try {
            version = redisVersion(connection.sync().info("server"));
        } catch (RuntimeException e) {
            // e.g. a managed server restricting INFO: the transactional fallback works everywhere
            version = UNKNOWN;
        }
        nativeGetdel = supportsGetdel(version);
        log.info("Graph state store connected to redis://{}:{}/{} (Redis {}, consume via {})",
                host, port, database, version, nativeGetdel ? "GETDEL" : "transactional GET+DEL");
    }

    private static void shutdown() {
        synchronized (SAFETY) {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            if (client != null) {
                client.shutdown();
            }
        }
    }
}
