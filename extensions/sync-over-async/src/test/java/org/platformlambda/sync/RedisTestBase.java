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
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Boots a real (embedded) {@code redis-server} on a free port for the duration of the test class and
 * exposes a Lettuce client to it. The bundled binary covers macOS arm64/amd64 and Linux arm64/amd64,
 * so the return-route mechanism is exercised against genuine Redis semantics (incl. Pub/Sub) with no
 * Docker dependency.
 */
public abstract class RedisTestBase {

    protected static RedisServer redisServer;
    protected static RedisClient redisClient;
    protected static int redisPort;

    @BeforeAll
    static void startRedis() throws IOException {
        redisPort = freePort();
        redisServer = new RedisServer(redisPort);
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
