/*

    Copyright 2018-2025 Accenture Technology

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

package com.accenture.services;

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.util.*;

/**
 * This is a generic resilience handler. It will retry, abort, use an alternative path or exercise a brief backoff.
 * <p>
 * The following parameters (input data mapping) define behavior:
 * max_attempts - when the handler has used all the attempts, it will abort.
 * attempt - this tells the handler how many attempts it has tried
 * status - you should map the error status code in this field
 * message - you should map the error message in this field
 * alternative (start-end, code, code) - the optional codes and range of status codes to tell the handler to reroute
 * delay - the delay in milliseconds before exercising retry or reroute. Minimum valued is 10 ms.
 *         Delay is skipped for the first retry. This slight delay is a protection mechanism.
 * <p>
 * Optional backoff behavior:
 * cumulative - the total number of failures since last success or backoff reset if any
 * backoff - the time of a backoff period (epoch milliseconds) if any
 * backoff_trigger - the total number of failures that triggers a backoff
 * backoff_seconds - the time to backoff after an abort has occurred.
 *                   During this period, It will abort without updating attempt.
 *                   This avoids overwhelming the target service that may result inss
 *                   recovery storm.
 * <p>
 * Return value (output data mapping):
 * result.attempt - the handler will clear or increment this counter
 * result.cumulative - the handler will clear or increment this counter.
 *                     Not set if "backoff_trigger" is not given in input.
 * result.decision - 1, 2 or 3 where 1=retry, 2=abort, 3=reroute that corresponds to the next tasks
 * result.status - the status code that the handler aborts the retry or reroute.
 *                 Not set if retry or reroute.
 * result.message - the reason that the handler aborts the retry or reroute.
 *                 Not set if retry or reroute.
 * result.backoff - the time of a backoff period (epoch milliseconds).
 *                  Not set if not in backoff mode.
 * <p>
 * result.attempt should be saved in the state machine with the "model." namespace
 * result.cumulative and result.backoff should be saved in the temporary file system or an external state machine
 * <p>
 *     For non-blocking operation, this function is defined as an EventInterceptor for scheduling of future retries
 *     that are delayed. While a regular composable function returns a result, an EventInterceptor function must send
 *     its result programmatically using the PostOffice.
 */
@EventInterceptor
@PreLoad(route = "resilience.handler", instances=100)
public class Resilience4Flow implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final Utility util = Utility.getInstance();
    private static final String MAX_ATTEMPTS = "max_attempts";
    private static final String ATTEMPT = "attempt";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String ALTERNATIVE = "alternative";
    private static final String DELAY = "delay";
    private static final String CUMULATIVE = "cumulative";
    private static final String BACKOFF = "backoff";
    private static final String BACKOFF_TRIGGER = "backoff_trigger";
    private static final String BACKOFF_SECONDS = "backoff_seconds";
    private static final String DECISION = "decision";

    @SuppressWarnings("unchecked")
    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance)
            throws InterruptedException, IOException {
        if (event.getRawBody() instanceof Map && event.getReplyTo() != null && event.getCorrelationId() != null) {
            PostOffice po = new PostOffice(headers, instance);
            Map<String, Object> input = (Map<String, Object>) event.getRawBody();
            int cumulative = Math.max(0, util.str2int(String.valueOf(input.getOrDefault(CUMULATIVE, 0))));
            long now = System.currentTimeMillis();
            Map<String, Object> result = new HashMap<>();
            // Still in the backoff period?
            if (input.containsKey(BACKOFF)) {
                long lastBackoff = util.str2long(String.valueOf(input.getOrDefault(BACKOFF, 0)));
                if (now < lastBackoff) {
                    // tell the system to abort the request and execute the second one in the next task list
                    long diff = Math.max(1, (lastBackoff - now) / 1000);
                    var waitPeriod = diff + (diff == 1? " second" : " seconds");
                    result.put(DECISION, 2);
                    result.put(STATUS, 503);
                    result.put(MESSAGE, "Service temporarily not available - please try again in "+waitPeriod);
                    result.put(BACKOFF, lastBackoff);
                    sendResult(po, event.getReplyTo(), event.getCorrelationId(), result, 0);
                    return null;
                } else {
                    // reset cumulative counter because backoff period has ended
                    cumulative = 0;
                }
            }
            // Not in backoff period - evaluate condition for retry, abort or alternative path
            int status = Math.max(200, util.str2int(String.valueOf(input.getOrDefault(STATUS, 200))));
            // When backoff feature is used, you must put the resilient handler as a gatekeeper to the user function.
            // If status code is 200, it should execute the user function immediately.
            if (status == 200) {
                result.put(DECISION, 1);
                result.put(CUMULATIVE, cumulative);
                sendResult(po, event.getReplyTo(), event.getCorrelationId(), result, 0);
                return null;
            }
            // Needs to trigger backoff?
            if (input.containsKey(BACKOFF_TRIGGER) && input.containsKey(BACKOFF_SECONDS)) {
                int backoffTrigger = Math.max(1, util.str2int(String.valueOf(input.getOrDefault(BACKOFF_TRIGGER, 1))));
                int backoffSeconds = Math.max(1, util.str2int(String.valueOf(input.getOrDefault(BACKOFF_SECONDS, 1))));
                cumulative++;
                if (cumulative > backoffTrigger) {
                    // trigger backoff
                    var waitPeriod = backoffSeconds + (backoffSeconds == 1? " second" : " seconds");
                    result.put(DECISION, 2);
                    result.put(STATUS, 503);
                    result.put(MESSAGE, "Service temporarily not available - please try again in "+waitPeriod);
                    result.put(BACKOFF, now + backoffSeconds * 1000L);
                    sendResult(po, event.getReplyTo(), event.getCorrelationId(), result, 0);
                    return null;
                }
            }
            AlternativePath routing = null;
            if (input.containsKey(ALTERNATIVE)) {
                routing = new AlternativePath(String.valueOf(input.get(ALTERNATIVE)));
            }
            int maxAttempt = Math.max(1, util.str2int(String.valueOf(input.getOrDefault(MAX_ATTEMPTS, 1))));
            int attemptCount = Math.max(0, util.str2int(String.valueOf(input.getOrDefault(ATTEMPT, 0))));
            long delay = Math.max(10L, util.str2long(String.valueOf(input.getOrDefault(DELAY, 10))));
            // increment attempts
            attemptCount++;
            result.put(ATTEMPT, attemptCount);
            result.put(CUMULATIVE, cumulative);
            if (attemptCount > maxAttempt) {
                delay = 0;
                String message = String.valueOf(input.getOrDefault(MESSAGE, "Runtime exception"));
                // tell the system to abort the request by executing the 2nd task
                result.put(DECISION, 2);
                result.put(STATUS, status);
                result.put(MESSAGE, message);
            } else {
                if (attemptCount == 1) {
                    delay = 0;
                }
                if (routing != null && routing.needReroute(status)) {
                    // tell the system to execute the alternative execution path
                    result.put(DECISION, 3);
                } else {
                    // otherwise, retry the original task
                    result.put(DECISION, 1);
                }
            }
            sendResult(po, event.getReplyTo(), event.getCorrelationId(), result, delay);
        }
        return null;
    }

    private void sendResult(PostOffice po, String replyTo, String cid, Map<String, Object> result, long delay)
            throws IOException {
        var response = new EventEnvelope().setTo(replyTo).setCorrelationId(cid).setBody(result);
        if (delay > 0) {
            po.sendLater(response, new Date(System.currentTimeMillis() + delay));
        } else {
            po.send(response);
        }
    }

    private static class AlternativePath {
        private final Set<Integer> statusCodes = new HashSet<>();
        private final List<Integer[]> statusRanges = new ArrayList<>();

        public AlternativePath(String codes) {
            List<String> list = util.split(codes, ",");
            for (String item: list) {
                var s = item.trim();
                if (s.contains("-")) {
                    var idx = s.indexOf('-');
                    var n1 = util.str2int(s.substring(0, idx).trim());
                    var n2 = util.str2int(s.substring(idx+1).trim());
                    if (n1 > 200 && n2 > 200) {
                        Integer[] range = new Integer[2];
                        if (n2 > n1) {
                            range[0] = n1;
                            range[1] = n2;
                        } else {
                            range[0] = n2;
                            range[1] = n1;
                        }
                        statusRanges.add(range);
                    }
                } else {
                    var rc = util.str2int(s);
                    if (rc > 200) {
                        statusCodes.add(rc);
                    }
                }
            }
        }

        public boolean needReroute(int status) {
            for (var i: statusCodes) {
                if (i == status) {
                    return true;
                }
            }
            for (var range: statusRanges) {
                if (status >= range[0] && status <= range[1]) {
                    return true;
                }
            }
            return false;
        }
    }
}
