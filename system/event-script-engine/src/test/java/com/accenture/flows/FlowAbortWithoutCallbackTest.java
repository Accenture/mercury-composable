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

package com.accenture.flows;

import com.accenture.adapters.FlowExecutor;
import com.accenture.models.Flows;
import com.accenture.setup.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An aborted flow is closed even when its caller cannot be told.
 * <p>
 * A flow launched with no callback has no reply-to address, and a callback route that is not
 * served cannot take the error. Either way, when the flow aborts the task executor must still
 * end the flow instance, so the instance leaves the flow registry and its task references are
 * released. The probe function records the flow instance id, then fails, stalls past the flow
 * ttl, or passes on to a task whose function is not deployed.
 */
class FlowAbortWithoutCallbackTest extends TestBase {
    private static final String ORIGINATOR = "unit.test";
    private static final String FLOW_ID = "abort-without-callback-test";
    private static final String PROBE = "abort.probe";
    private static final String CALLBACK = "abort.probe.callback";
    // a callback route that no function serves, as when the caller's instance has gone
    private static final String GONE_CALLBACK = "abort.probe.callback.gone";
    private static final String TOKEN = "token";
    private static final String MODE = "mode";
    private static final String FAIL = "fail";
    private static final String STALL = "stall";
    private static final String PASS = "pass";
    private static final String STATUS = "status";
    // a short ttl for the timeout cases, and a probe that stalls well past it and past the closing deadline.
    // The other cases keep the flow's own ttl of 10 seconds, so only the abort itself can close them in time.
    private static final long SHORT_TTL_MS = 500;
    private static final long STALL_MS = 5000;
    private static final long CLOSE_DEADLINE_MS = 2500;
    private static final ConcurrentMap<String, CompletableFuture<String>> INSTANCES = new ConcurrentHashMap<>();
    private static final BlockingQueue<Map<String, Object>> CALLBACKS = new ArrayBlockingQueue<>(10);

    @BeforeAll
    static void registerProbe() {
        Platform platform = Platform.getInstance();
        if (!platform.hasRoute(PROBE)) {
            LambdaFunction probe = (headers, body, instance) -> {
                Map<?, ?> input = (Map<?, ?>) body;
                instanceOf(String.valueOf(input.get(TOKEN))).complete(String.valueOf(input.get("instance")));
                String mode = String.valueOf(input.get(MODE));
                if (FAIL.equals(mode)) {
                    throw new AppException(500, "Simulated task failure");
                }
                if (STALL.equals(mode)) {
                    Utility.getInstance().sleep(STALL_MS);
                }
                return Map.of(MODE, mode);
            };
            platform.registerPrivate(PROBE, probe, 10);
        }
        if (!platform.hasRoute(CALLBACK)) {
            LambdaFunction callback = (headers, body, instance) -> {
                if (body instanceof Map<?, ?> map) {
                    Map<String, Object> result = new HashMap<>();
                    map.forEach((k, v) -> result.put(String.valueOf(k), v));
                    CALLBACKS.add(result);
                }
                return null;
            };
            platform.registerPrivate(CALLBACK, callback, 1);
        }
    }

    private static CompletableFuture<String> instanceOf(String token) {
        return INSTANCES.computeIfAbsent(token, k -> new CompletableFuture<>());
    }

    /**
     * Launch the flow and return the flow instance id the probe recorded.
     *
     * @param mode of the probe
     * @param ttl of the flow in milliseconds, or 0 for the flow's own ttl
     * @param callback route, or null for a launch with no callback
     * @return flow instance id
     */
    private String launch(String mode, long ttl, String callback) throws Exception {
        Utility util = Utility.getInstance();
        String token = util.getUuid();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", Map.of());
        dataset.put("body", Map.of(TOKEN, token, MODE, mode));
        if (ttl > 0) {
            dataset.put("ttl", ttl);
        }
        if (callback == null) {
            FlowExecutor.getInstance().launch(ORIGINATOR, FLOW_ID, dataset, util.getUuid());
        } else {
            FlowExecutor.getInstance().launch(ORIGINATOR, FLOW_ID, dataset, callback, util.getUuid());
        }
        String id = instanceOf(token).get(5, TimeUnit.SECONDS);
        INSTANCES.remove(token);
        return id;
    }

    /**
     * Wait for the flow instance to leave the flow registry.
     *
     * @param id of the flow instance
     * @return true when it is closed before the deadline
     */
    private boolean closedInTime(String id) {
        long deadline = System.currentTimeMillis() + CLOSE_DEADLINE_MS;
        while (System.currentTimeMillis() < deadline) {
            if (Flows.getFlowInstance(id) == null) {
                return true;
            }
            Utility.getInstance().sleep(50);
        }
        return Flows.getFlowInstance(id) == null;
    }

    @Test
    void taskFailureWithoutCallbackClosesTheFlow() throws Exception {
        String id = launch(FAIL, 0, null);
        assertTrue(closedInTime(id), "a flow that aborts on a task failure must be closed");
    }

    @Test
    void timeoutWithoutCallbackClosesTheFlow() throws Exception {
        // the probe is still stalling when the deadline passes, so only the timeout can close the flow
        String id = launch(STALL, SHORT_TTL_MS, null);
        assertTrue(closedInTime(id), "a flow that times out must be closed at its ttl");
    }

    @Test
    void missingRouteWithoutCallbackClosesTheFlow() throws Exception {
        String id = launch(PASS, 0, null);
        assertTrue(closedInTime(id), "a flow that aborts on a missing route must be closed");
    }

    @Test
    void timeoutWithCallbackClosesTheFlowAndTellsTheCaller() throws Exception {
        // control: the same timeout with a callback, so the failures above come from the missing callback
        CALLBACKS.clear();
        String id = launch(STALL, SHORT_TTL_MS, CALLBACK);
        Map<String, Object> error = CALLBACKS.poll(5, TimeUnit.SECONDS);
        assertNotNull(error, "the callback must receive the timeout");
        assertEquals(408, error.get(STATUS), "the callback must receive status 408: " + error);
        assertTrue(closedInTime(id), "a flow that times out must be closed at its ttl");
    }

    @Test
    void taskFailureWithGoneCallbackClosesTheFlow() throws Exception {
        String id = launch(FAIL, 0, GONE_CALLBACK);
        assertTrue(closedInTime(id), "a flow that aborts on a task failure must be closed when its callback is gone");
    }

    @Test
    void timeoutWithGoneCallbackClosesTheFlow() throws Exception {
        String id = launch(STALL, SHORT_TTL_MS, GONE_CALLBACK);
        assertTrue(closedInTime(id), "a flow that times out must be closed at its ttl when its callback is gone");
    }

    @Test
    void missingRouteWithGoneCallbackClosesTheFlow() throws Exception {
        String id = launch(PASS, 0, GONE_CALLBACK);
        assertTrue(closedInTime(id), "a flow that aborts on a missing route must be closed when its callback is gone");
    }
}
