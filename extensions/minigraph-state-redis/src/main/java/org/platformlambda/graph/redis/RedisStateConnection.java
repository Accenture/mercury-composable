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
 * configures Redis once): redis.host, redis.port, redis.password, redis.ssl,
 * redis.database, redis.timeout.ms - all resolvable through the usual
 * ${ENV_VAR:default} substitution, e.g. redis.password=${REDIS_PASSWORD:}.
 */
class RedisStateConnection {
    private static final Logger log = LoggerFactory.getLogger(RedisStateConnection.class);
    // one shared namespace so a workflow suspended by one application instance
    // can resume on any other instance sharing the same Redis
    static final String KEY_PREFIX = "graph:state:";
    private static final Object SAFETY = new Object();
    private static RedisClient client;
    private static StatefulRedisConnection<String, byte[]> connection;

    private RedisStateConnection() {}

    static RedisCommands<String, byte[]> commands() {
        synchronized (SAFETY) {
            if (connection == null || !connection.isOpen()) {
                connect();
            }
            return connection.sync();
        }
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
        log.info("Graph state store connected to redis://{}:{}/{}", host, port, database);
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
