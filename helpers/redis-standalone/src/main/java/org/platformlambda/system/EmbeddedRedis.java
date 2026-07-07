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

package org.platformlambda.system;

import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisServer;

import java.io.File;
import java.io.IOException;

/**
 * Thin lifecycle wrapper around the {@code embedded-redis} server. The library bundles a real
 * {@code redis-server} binary (macOS/Linux, arm64 + amd64) and runs it as a subprocess, so a developer
 * can spin up a standalone Redis for local development and testing with <b>no Docker image</b>.
 *
 * <p>Like the {@code kafka-standalone} helper, this is for development and testing only - not production.
 * A single server is started on the configured port (default 6379).</p>
 *
 * <p>Working files (the RDB dump) are pinned to {@link #DATA_DIR} - a transient {@code /tmp} location,
 * matching the cloud-native pattern used by {@code kafka-standalone} (working files under {@code /tmp},
 * wiped on reboot). The same directory is used by the sync-over-async test suite, so both exercise Redis
 * against a known, transient store.</p>
 *
 * <p>Like {@code kafka-standalone}, the data directory is <b>wiped and recreated before each start</b>, so
 * a restart begins from a clean slate with no residual records from a previous run - the standalone server
 * is for development and testing, where that is the intended behavior.</p>
 */
public class EmbeddedRedis {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedRedis.class);

    /** Transient working directory for Redis data (cloud-native {@code /tmp} pattern). */
    public static final String DATA_DIR = "/tmp/soa-redis";
    private static final String EMULATED_FAILURE = "emulate exception";

    private final int port;
    private RedisServer redisServer;
    private boolean testMode;

    public EmbeddedRedis(int port) {
        this.port = port;
    }

    /**
     * Enable test mode so {@link #startServer()} and {@link #stop()} emulate an {@code IOException}. This
     * exists purely so the failure-handling branches can be covered by a unit test: a standalone dev server
     * otherwise fails fast via {@code System.exit(-1)}, which cannot be exercised without terminating the
     * test JVM. Never enabled in production.
     */
    void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /** Start the embedded redis-server subprocess and register a shutdown hook to stop it cleanly. */
    public void start() {
        try {
            startServer();
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } catch (IOException e) {
            log.error("Unable to start Redis server on port {} - {}", port, e.getMessage());
            if (!testMode) {
                System.exit(-1);
            }
        }
    }

    /**
     * Wipe the transient store, (re)create it, and start the embedded redis-server, blocking until it is
     * ready. Package-visible so a unit test can drive startup directly; {@link #start()} wraps this and
     * exits the JVM on failure (a standalone dev server has nothing to fall back to).
     */
    // S5443: the fixed /tmp/soa-redis path is intentional for this dev/test-only standalone server - a
    // known, transient store, shared with the test suite and wiped before each start (never production).
    @SuppressWarnings("java:S5443")
    void startServer() throws IOException {
        if (testMode) {
            throw new IOException(EMULATED_FAILURE);
        }
        // wipe the transient store and recreate it, so a restart begins from a clean slate
        File dir = new File(DATA_DIR);
        Utility.getInstance().cleanupDir(dir);
        if (!dir.mkdirs()) {
            throw new IOException("Unable to create Redis data directory " + DATA_DIR);
        }
        redisServer = RedisServer.newRedisServer()
                .port(port)
                .setting("dir " + DATA_DIR)        // transient /tmp working dir
                .setting("dbfilename dump.rdb")
                .setting("appendonly no")
                .build();
        redisServer.start();
        log.info("Standalone Redis server started on 127.0.0.1:{} (data dir {})", port, DATA_DIR);
    }

    public void stop() {
        try {
            if (testMode) {
                throw new IOException(EMULATED_FAILURE);
            }
            if (redisServer != null) {
                redisServer.stop();
                log.info("Standalone Redis server on port {} stopped", port);
            }
        } catch (IOException e) {
            log.warn("Unable to stop Redis server cleanly - {}", e.getMessage());
        }
    }
}
