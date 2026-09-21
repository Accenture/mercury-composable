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

package org.platformlambda.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;

/**
 * The request-response (RPC) idiom between two functions in the same application, behind the REST
 * endpoint {@code /api/hello/rpc}: build an event for the callee, wait for its reply within a budget,
 * check the reply's STATUS before reading its body, and return the result.
 * <p>
 * Three things this demo teaches:
 * <ol>
 *   <li>{@code new PostOffice(headers, instance)} is trace-aware - the RPC continues the caller's
 *       distributed trace, so the callee's span is a child of this one.</li>
 *   <li>{@code po.request(event, timeout).get()} is synchronous in style but does not block a kernel
 *       thread: the virtual thread suspends until the reply arrives or the budget expires. A timeout
 *       surfaces as a {@code TimeoutException} that the platform maps to HTTP 408 for the REST caller.</li>
 *   <li>A callee that throws replies with its error status and message as the body - the caller must
 *       check {@code reply.getStatus()} before using the body, or a failure reads as a normal reply.</li>
 * </ol>
 * Try {@code POST /api/hello/rpc} with a JSON body (echoed back by {@code hello.world}), and
 * {@code POST /api/hello/rpc?timeout=300} with {@code {"sleep_ms": 1500}} to see the 408.
 */
@PreLoad(route = "hello.rpc", instances = 10)
public class HelloRpc implements TypedLambdaFunction<AsyncHttpRequest, Map<String, Object>> {
    private static final String CALLEE = "hello.world";
    private static final long DEFAULT_TIMEOUT_MS = 5000;

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance)
                                            throws Exception {
        // trace-aware PostOffice: the outbound request carries this function's trace context
        var po = new PostOffice(headers, instance);
        Utility util = Utility.getInstance();
        String budget = input.getQueryParameter("timeout");
        long timeoutMs = budget == null ? DEFAULT_TIMEOUT_MS : Math.max(100, util.str2long(budget));
        Object payload = input.getBody() == null ? Map.of("message", "hello from rpc") : input.getBody();
        EventEnvelope request = new EventEnvelope().setTo(CALLEE).setBody(payload);
        long start = System.currentTimeMillis();
        // request-response: suspends this virtual thread until the reply arrives or the budget expires
        // (a timeout throws and reaches the REST caller as HTTP 408 "Timeout for N ms")
        EventEnvelope reply = po.request(request, timeoutMs).get();
        // a callee that throws replies with its error status and the message as the body -
        // check the status before trusting the body (an error is not a normal reply)
        if (reply.getStatus() >= 400) {
            throw new AppException(reply.getStatus(), String.valueOf(reply.getError()));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("callee", CALLEE);
        result.put("timeout_ms", timeoutMs);
        result.put("round_trip_ms", System.currentTimeMillis() - start);
        result.put("reply", reply.getBody());
        return result;
    }
}
