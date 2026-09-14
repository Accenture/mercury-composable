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

package org.platformlambda.cache;

import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.util.Utility;
import redis.embedded.RedisServer;

import java.io.File;
import java.io.IOException;

/**
 * Boots a real (embedded) {@code redis-server} on a fixed high port for the duration of the test class and
 * exposes a Lettuce client to it — genuine Redis semantics, no Docker. The port is distinct from every other
 * module's fixture (sync-over-async 16379/16380, redis-connection 16381/16382) so a parallel reactor build
 * ({@code mvn -T}) does not collide.
 */
public abstract class RedisTestBase {

    /** Transient working directory for Redis data (wiped and recreated before each run). */
    protected static final String REDIS_DATA_DIR = "/tmp/cache-redis";
    private static final int REDIS_PORT = 16383;

    protected static RedisServer redisServer;
    protected static RedisClient redisClient;
    protected static int redisPort;

    // S5443: a fixed /tmp path is intentional for this test fixture (wiped before each run).
    @SuppressWarnings("java:S5443")
    @BeforeAll
    static void startRedis() throws IOException {
        redisPort = REDIS_PORT;
        File dir = new File(REDIS_DATA_DIR);
        Utility.getInstance().cleanupDir(dir);
        if (!dir.mkdirs()) {
            throw new IllegalStateException("Unable to create " + REDIS_DATA_DIR);
        }
        redisServer = RedisServer.newRedisServer()
                .port(redisPort)
                .setting("dir " + REDIS_DATA_DIR)
                .setting("save \"\"")
                .setting("appendonly no")
                .build();
        redisServer.start();
        redisClient = RedisClient.create("redis://127.0.0.1:" + redisPort);
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (redisClient != null) {
            redisClient.shutdown();
        }
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
