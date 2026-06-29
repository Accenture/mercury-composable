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

package org.platformlambda.sync;

import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.util.Utility;
import redis.embedded.RedisServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Boots a real (embedded) {@code redis-server} on a free port for the duration of the test class and
 * exposes a Lettuce client to it. The bundled binary covers macOS arm64/amd64 and Linux arm64/amd64,
 * so the return-route mechanism is exercised against genuine Redis semantics (incl. Pub/Sub) with no
 * Docker dependency.
 *
 * <p>The working directory is pinned to {@code /tmp/soa-redis} - the same transient {@code /tmp} location
 * the {@code redis-standalone} helper uses (cloud-native pattern). It is <b>wiped and recreated before the
 * server starts</b> (as {@code kafka-standalone} does with its log dir), and persistence is disabled
 * ({@code save ""} + {@code appendonly no}), so each test class starts from a clean, isolated slate with no
 * residual records carried over between runs.</p>
 */
public abstract class RedisTestBase {

    /** Transient working directory for Redis data; shared with the redis-standalone helper. */
    protected static final String REDIS_DATA_DIR = "/tmp/soa-redis";

    protected static RedisServer redisServer;
    protected static RedisClient redisClient;
    protected static int redisPort;

    // S5443: a fixed /tmp/soa-redis path is intentional for this test fixture (wiped before each run).
    @SuppressWarnings("java:S5443")
    @BeforeAll
    static void startRedis() throws IOException {
        redisPort = freePort();
        // wipe the transient store and recreate it, so each run begins from a clean slate
        File dir = new File(REDIS_DATA_DIR);
        Utility.getInstance().cleanupDir(dir);
        dir.mkdirs();
        redisServer = RedisServer.newRedisServer()
                .port(redisPort)
                .setting("dir " + REDIS_DATA_DIR)   // transient /tmp working dir
                .setting("save \"\"")               // no RDB snapshots -> clean, isolated state per run
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

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
