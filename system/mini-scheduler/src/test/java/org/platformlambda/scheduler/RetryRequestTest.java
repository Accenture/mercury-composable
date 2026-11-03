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

package org.platformlambda.scheduler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A request the scheduler waits on is retried after a timeout, up to three times, and never after an error answer.
 * Short timeouts here keep the test quick; the scheduler's own is {@link RetryRequest#TIMEOUT}.
 */
class RetryRequestTest {
    private static final AtomicInteger slowCalls = new AtomicInteger();
    private static final AtomicInteger errorCalls = new AtomicInteger();

    @BeforeAll
    static void setup() {
        AppStarter.main(new String[0]);
        // answers late twice, as at a cold start, then at once
        TypedLambdaFunction<Map<String, Object>, Boolean> slow = (headers, input, instance) -> {
            if (slowCalls.incrementAndGet() <= 2) {
                Utility.getInstance().sleep(1000);
            }
            return true;
        };
        TypedLambdaFunction<Map<String, Object>, Boolean> silent = (headers, input, instance) -> {
            Utility.getInstance().sleep(1000);
            return true;
        };
        TypedLambdaFunction<Map<String, Object>, Boolean> failing = (headers, input, instance) -> {
            errorCalls.incrementAndGet();
            throw new IllegalStateException("store unavailable");
        };
        var platform = Platform.getInstance();
        platform.registerPrivate("retry.unit.slow", slow, 4);
        platform.registerPrivate("retry.unit.silent", silent, 4);
        platform.registerPrivate("retry.unit.failing", failing, 1);
    }

    @Test
    void aLateAnswerIsAskedAgain() throws ExecutionException, InterruptedException {
        var reply = RetryRequest.send(postOffice(), () -> new EventEnvelope().setTo("retry.unit.slow"),
                "Slow function", 300);
        assertFalse(reply.timedOut());
        assertEquals(3, reply.attempts());
        assertEquals(true, reply.event().getBody());
    }

    @Test
    void noAnswerAfterEveryAttemptIsATimeout() throws ExecutionException, InterruptedException {
        var reply = RetryRequest.send(postOffice(), () -> new EventEnvelope().setTo("retry.unit.silent"),
                "Silent function", 100);
        assertTrue(reply.timedOut());
        assertEquals(RetryRequest.RETRIES + 1, reply.attempts());
    }

    @Test
    void anErrorAnswerIsNotRetried() throws ExecutionException, InterruptedException {
        var reply = RetryRequest.send(postOffice(), () -> new EventEnvelope().setTo("retry.unit.failing"),
                "Failing function", 300);
        assertFalse(reply.timedOut());
        assertEquals(1, reply.attempts());
        assertEquals(500, reply.event().getStatus());
        assertEquals(1, errorCalls.get());
    }

    private static PostOffice postOffice() {
        return new PostOffice("unit.test", Utility.getInstance().getUuid(), "RETRY test");
    }
}
