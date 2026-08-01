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
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.Utility;
import redis.embedded.RedisServer;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts an embedded Redis (real redis-server binary, macOS arm64/amd64 and Linux
 * covered - no Docker) on a fixed high port and boots the platform once, so the
 * two state-store functions register through the normal PreLoad classpath scan.
 * Port 16380 avoids both a local Redis on 6379 and the sync-over-async test
 * suite's 16379 when the reactor builds in parallel.
 */
public abstract class RedisStateTestBase {
    protected static final String DATA_DIR = "/tmp/graph-state-redis";
    protected static final int REDIS_PORT = 16380;
    private static final AtomicBoolean started = new AtomicBoolean(false);
    protected static RedisClient testClient;
    protected static StatefulRedisConnection<String, String> testConnection;

    @BeforeAll
    static void startInfrastructure() throws Exception {
        if (started.compareAndSet(false, true)) {
            var dir = new File(DATA_DIR);
            Utility.getInstance().cleanupDir(dir);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Unable to create " + DATA_DIR);
            }
            var redisServer = RedisServer.newRedisServer()
                    .port(REDIS_PORT)
                    .setting("dir " + DATA_DIR)
                    .setting("save \"\"")
                    .setting("appendonly no")
                    .build();
            redisServer.start();
            testClient = RedisClient.create("redis://127.0.0.1:" + REDIS_PORT);
            testConnection = testClient.connect();
            AutoStart.main(new String[0]);
        }
    }

    @AfterAll
    static void keepRunning() {
        // the embedded server and platform stay up for the whole JVM (shared across
        // test classes); the JVM teardown reclaims them
    }
}
