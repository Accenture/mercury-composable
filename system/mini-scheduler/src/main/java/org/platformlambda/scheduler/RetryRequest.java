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

import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * A request to a function the scheduler waits on - the state resolver or the environment store - with a short
 * timeout and up to three retries. A function that answers slowly at a cold start is asked again, and each timed-out
 * attempt is recorded in the log. Only a timeout is retried: an error answer is returned as it came.
 */
public final class RetryRequest {
    private static final Logger log = LoggerFactory.getLogger(RetryRequest.class);
    /** The time one attempt waits for its answer, in milliseconds. */
    public static final long TIMEOUT = 3000;
    /** The attempts after the first one, each made only when the one before timed out. */
    public static final int RETRIES = 3;
    private static final int TIMEOUT_STATUS = 408;

    private RetryRequest() {
        // static helper
    }

    /**
     * The answer and the attempts it took.
     *
     * @param event the answer, or the last attempt's timeout (status 408) when no attempt was answered
     * @param attempts the attempts made, from 1 to {@link #RETRIES} + 1
     */
    public record Reply(EventEnvelope event, int attempts) {
        /** @return true when no attempt was answered */
        public boolean timedOut() {
            return event.getStatus() == TIMEOUT_STATUS;
        }
    }

    /**
     * Send a request, retrying a timeout.
     *
     * @param po the caller's post office
     * @param request a new request for each attempt
     * @param purpose what is asked, for the log, e.g. "State resolver v1.state.resolver for job demo-task"
     * @return the reply
     * @throws ExecutionException when the request cannot be sent
     * @throws InterruptedException when the waiting thread is interrupted
     */
    public static Reply send(PostOffice po, Supplier<EventEnvelope> request, String purpose)
            throws ExecutionException, InterruptedException {
        return send(po, request, purpose, TIMEOUT);
    }

    static Reply send(PostOffice po, Supplier<EventEnvelope> request, String purpose, long timeout)
            throws ExecutionException, InterruptedException {
        var attempts = RETRIES + 1;
        EventEnvelope result = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            result = po.request(request.get(), timeout, false).get();
            if (result.getStatus() != TIMEOUT_STATUS) {
                if (attempt > 1) {
                    log.info("{} answered at attempt {} of {}", purpose, attempt, attempts);
                }
                return new Reply(result, attempt);
            }
            if (attempt < attempts) {
                log.warn("{} - attempt {} of {} timed out after {} ms, retrying", purpose, attempt, attempts, timeout);
            }
        }
        log.error("{} - no answer after {} attempts of {} ms each", purpose, attempts, timeout);
        return new Reply(result, attempts);
    }
}
