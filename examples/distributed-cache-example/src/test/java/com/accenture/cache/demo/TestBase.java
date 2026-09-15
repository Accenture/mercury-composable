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

package com.accenture.cache.demo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import redis.embedded.RedisServer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Boots the demo application once against an embedded Redis. The embedded server starts <b>before</b>
 * {@code AutoStart.main} so the cache (which connects lazily on first use) finds it; the app's test config
 * ({@code src/test/resources/application.properties}) points {@code redis.port} at this server. The runnable
 * app instead uses a standalone Redis from {@code helpers/redis-standalone} - see README.md.
 */
public class TestBase {

    private static final AtomicInteger startCounter = new AtomicInteger(0);
    private static final String REDIS_DATA_DIR = "/tmp/cache-demo-redis";
    private static final int REDIS_PORT = 16385;
    protected static RedisServer redisServer;
    protected static String host;

    // S5443: a fixed /tmp path is intentional for this test fixture (wiped before each run)
    @SuppressWarnings("java:S5443")
    @BeforeAll
    static void setup() throws IOException {
        if (startCounter.incrementAndGet() == 1) {
            File dir = new File(REDIS_DATA_DIR);
            Utility.getInstance().cleanupDir(dir);
            if (!dir.mkdirs()) {
                throw new IllegalStateException("Unable to create " + REDIS_DATA_DIR);
            }
            redisServer = RedisServer.newRedisServer()
                    .port(REDIS_PORT)
                    .setting("dir " + REDIS_DATA_DIR)
                    .setting("save \"\"")
                    .setting("appendonly no")
                    .build();
            redisServer.start();
            AppConfigReader config = AppConfigReader.getInstance();
            host = "http://127.0.0.1:" + config.getProperty("rest.server.port", "8306");
            AutoStart.main(new String[0]);
        }
    }

    @AfterAll
    static void teardown() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
