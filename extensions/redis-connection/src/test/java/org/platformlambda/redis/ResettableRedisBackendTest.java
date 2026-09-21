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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.util.Utility;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The connection-reset rule of the shared command connection (interop Finding 4, ruled 2026-09-21): a
 * timeout on a connection that is not open resets it at once; a timeout on an open connection resets it on
 * the second consecutive timeout; the facades handed out by commands()/async() keep working across resets.
 * Against the embedded Redis, with a short command timeout so the timeouts are real.
 */
class ResettableRedisBackendTest extends RedisTestBase {
    private static final long TIMEOUT_MS = 400;
    private StandaloneRedisBackend<String> backend;

    @BeforeEach
    void connect() {
        // the backend owns this client and shuts it down on close()
        RedisClient client = RedisClient.create(RedisURI.builder().withHost("127.0.0.1").withPort(redisPort)
                .withTimeout(Duration.ofMillis(TIMEOUT_MS)).build());
        backend = new StandaloneRedisBackend<>(client, StringCodec.UTF8);
    }

    @AfterEach
    void disconnect() {
        backend.close();
    }

    @Test
    void aTimeoutOnAnOpenConnectionResetsOnlyOnTheSecondConsecutiveOne() {
        RedisClusterCommands<String, String> commands = backend.commands();
        commands.set("reset-test", "hello");
        assertEquals("hello", commands.get("reset-test"));
        assertEquals(0, backend.resets());
        // a blocking pop on an empty list waits a full second server-side while the client gives up after
        // 400 ms: a genuine client-side timeout on a connection that stays open
        assertThrows(RedisCommandTimeoutException.class, () -> commands.blpop(1, "reset-test-empty"));
        assertEquals(0, backend.resets(), "one slow command on a healthy connection keeps the connection");
        assertThrows(RedisCommandTimeoutException.class, () -> commands.blpop(1, "reset-test-empty"));
        assertEquals(1, backend.resets(), "the second consecutive timeout resets the connection");
        // the captured facade still works - it resolved a fresh connection
        assertEquals("hello", commands.get("reset-test"));
        assertTrue(backend.connected());
        assertSame(commands, backend.commands(), "the facade is a stable object");
    }

    @Test
    void aReplyClearsTheConsecutiveTimeoutCount() {
        RedisClusterCommands<String, String> commands = backend.commands();
        assertThrows(RedisCommandTimeoutException.class, () -> commands.blpop(1, "reset-test-empty-2"));
        Utility.getInstance().sleep(700);   // let the server-side pop expire so the connection answers again
        assertNotNull(commands.ping());
        assertThrows(RedisCommandTimeoutException.class, () -> commands.blpop(1, "reset-test-empty-2"));
        assertEquals(0, backend.resets(), "a reply in between restarted the count: still the first timeout");
    }

    @Test
    void aTimeoutOnAClosedConnectionResetsAtOnceAndRecoveryFollowsTheServer() throws IOException {
        RedisClusterCommands<String, String> commands = backend.commands();
        commands.set("reset-test-outage", "before");
        redisServer.stop();
        try {
            // Lettuce buffers the command for its own reconnect; the caller sees the command timeout, and
            // the connection is not open - so the very first timeout resets it
            assertThrows(RedisCommandTimeoutException.class, () -> commands.get("reset-test-outage"));
            assertEquals(1, backend.resets());
            assertFalse(backend.connected());
            // while Redis is down, the next command fails fast on connect instead of waiting out a timeout
            RuntimeException down = assertThrows(RuntimeException.class, () -> commands.get("reset-test-outage"));
            AppException classified = RedisFailure.classify(down);
            assertNotNull(classified, "an unreachable Redis is classified");
            assertEquals(503, classified.getStatus(), "an unreachable Redis is 503, not 408");
            assertEquals(1, backend.resets(), "a connect failure is not a timeout - no further reset");
        } finally {
            redisServer = RedisServer.newRedisServer()
                    .port(redisPort)
                    .setting("dir " + REDIS_DATA_DIR)
                    .setting("save \"\"")
                    .setting("appendonly no")
                    .build();
            redisServer.start();
        }
        // Redis is back: the first command past the connect-retry hold window reconnects - recovery is bounded
        // by the command timeout, not by Lettuce's reconnect backoff (the embedded server restarts in well
        // under the hold window, so wait it out; the data is gone with the restart - the live reply is the point)
        Utility.getInstance().sleep(ResettableRedisBackend.CONNECT_RETRY_HOLD_MS + 100);
        assertEquals("PONG", commands.ping(), "a live reply after the restart");
        assertEquals(0L, commands.exists("reset-test-outage"), "the restarted server is empty");
        assertTrue(backend.connected());
        assertEquals(1, backend.resets());
    }

    @Test
    void aPipelineTimeoutReportedByTheCallerFollowsTheSameRule() {
        assertTrue(backend.connected());
        backend.onCommandTimeout();
        assertEquals(0, backend.resets(), "open connection: the first reported timeout is tolerated");
        backend.onCommandTimeout();
        assertEquals(1, backend.resets(), "the second consecutive report resets");
        assertEquals("PONG", backend.commands().ping());
    }
}
