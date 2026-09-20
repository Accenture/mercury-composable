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

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;

import java.net.ConnectException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisFailureTest {

    @Test
    void aCommandTimeoutIsA408WithLettuceMessage() {
        AppException e = RedisFailure.classify(new RedisCommandTimeoutException("Command timed out after 5 second(s)"));
        assertNotNull(e);
        assertEquals(408, e.getStatus());
        assertEquals("Command timed out after 5 second(s)", e.getMessage());
    }

    @Test
    void anUnreachableRedisIsA503() {
        Throwable[] outages = {
                new RedisConnectionException("Unable to connect to 127.0.0.1:6379"),
                new RedisException("Currently not connected. Commands are rejected."),
                new RedisException("Connection is closed"),
                new CompletionException(new ConnectException("Connection refused"))
        };
        for (Throwable outage : outages) {
            AppException e = RedisFailure.classify(outage);
            assertNotNull(e, outage.toString());
            assertEquals(503, e.getStatus(), outage.toString());
            assertTrue(e.getMessage().startsWith("Redis unavailable - "), e.getMessage());
        }
    }

    @Test
    void wrappersDoNotHideTheFailure() {
        AppException e = RedisFailure.classify(
                new RuntimeException(new RedisCommandTimeoutException("Command timed out after 2 second(s)")));
        assertNotNull(e);
        assertEquals(408, e.getStatus());
    }

    @Test
    void serverAnswersAndUnrelatedFailuresAreLeftToTheDefaultMapping() {
        assertNull(RedisFailure.classify(
                new RedisCommandExecutionException("WRONGTYPE Operation against a key holding the wrong kind of value")));
        assertNull(RedisFailure.classify(new IllegalArgumentException("Missing 'key'")));
        assertNull(RedisFailure.classify(new RedisException("ERR unknown command")));
    }

    @Test
    void anAppExceptionOnTheChainIsReturnedAsIs() {
        AppException original = new AppException(429, "slow down");
        assertSame(original, RedisFailure.classify(new RuntimeException(original)));
    }
}
