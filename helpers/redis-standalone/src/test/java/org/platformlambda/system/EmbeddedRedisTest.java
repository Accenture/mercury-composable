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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link EmbeddedRedis}'s failure-handling branches, which are otherwise unreachable: the
 * standalone server fails fast via {@code System.exit(-1)}, and {@code redisServer.stop()} does not throw
 * in normal use. Test mode emulates an {@code IOException} so those catch/guard paths can be exercised
 * without starting a real subprocess or terminating the test JVM. No broker is started here, so this runs
 * alongside the real end-to-end test in {@code org.platformlambda.example.RedisStandaloneTest}.
 */
class EmbeddedRedisTest {

    @Test
    void stopWithoutStartIsSafe() {
        // stop() before start() is a no-op (no server was created), not an error
        assertDoesNotThrow(() -> new EmbeddedRedis(0).stop());
    }

    @Test
    void startServerSurfacesFailure() {
        EmbeddedRedis redis = new EmbeddedRedis(0);
        redis.setTestMode(true);
        assertThrows(IOException.class, redis::startServer);
    }

    @Test
    void startSwallowsStartupFailureInTestMode() {
        // when startServer() fails, start() logs the error and (in test mode) skips System.exit
        EmbeddedRedis redis = new EmbeddedRedis(0);
        redis.setTestMode(true);
        assertDoesNotThrow(redis::start);
    }

    @Test
    void stopSwallowsFailure() {
        // stop() logs a warning and swallows an IOException rather than propagating it
        EmbeddedRedis redis = new EmbeddedRedis(0);
        redis.setTestMode(true);
        assertDoesNotThrow(redis::stop);
    }
}
