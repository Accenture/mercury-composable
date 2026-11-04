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

package org.platformlambda.agentloop;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code llm.run} through the platform: the route registers itself from the classpath scan, the model turn is a
 * scripted {@code llm.chat} function, and the tools are real routes, so the path {@code graph.task} uses is exercised -
 * the request and response codec, a route that throws, a route that times out, and a failure that reaches the caller
 * with its status.
 */
class LlmRunPlatformTest {
    private static final String CREDIT_LIMIT = "test.credit.limit";
    private static final String SLOW_LEDGER = "test.slow.ledger";
    private static final String FAILING_LEDGER = "test.failing.ledger";
    private static final String CONV = "0123456789abcdef0123456789abcdef";
    private static final ConcurrentLinkedQueue<Map<String, Object>> REPLIES = new ConcurrentLinkedQueue<>();
    private static final List<Map<String, Object>> REQUESTS = new CopyOnWriteArrayList<>();

    @BeforeAll
    static void boot() throws InterruptedException {
        AutoStart.main(new String[0]);
        var platform = Platform.getInstance();
        BlockingQueue<Boolean> ready = new ArrayBlockingQueue<>(1);
        platform.waitForProvider("llm.run", 20).onSuccess(ready::add);
        assertEquals(Boolean.TRUE, ready.poll(20, TimeUnit.SECONDS), "llm.run did not register itself");
        platform.registerPrivate(PlatformGateway.LLM_CHAT,
                (TypedLambdaFunction<Map<String, Object>, Object>) (headers, input, instance) -> {
                    REQUESTS.add(input);
                    var reply = REPLIES.poll();
                    if (reply == null) {
                        throw new AppException(500, "no scripted reply");
                    }
                    if (reply.get("_status") instanceof Integer status) {
                        throw new AppException(status, String.valueOf(reply.get("_message")));
                    }
                    return reply;
                }, 1);
        platform.registerPrivate(CREDIT_LIMIT,
                (TypedLambdaFunction<Map<String, Object>, Object>) (headers, input, instance) ->
                        Map.of("id", input.get("id"), "limit", 5000, "currency", "EUR"), 5);
        platform.registerPrivate(FAILING_LEDGER,
                (TypedLambdaFunction<Map<String, Object>, Object>) (headers, input, instance) -> {
                    throw new AppException(503, "ledger offline");
                }, 1);
        platform.registerPrivate(SLOW_LEDGER,
                (TypedLambdaFunction<Map<String, Object>, Object>) (headers, input, instance) ->
                        Mono.just(Map.of("balance", 1)).delayElement(Duration.ofMillis(2500)), 1);
    }

    @BeforeEach
    void reset() {
        REPLIES.clear();
        REQUESTS.clear();
    }

    private static Map<String, Object> row(String route) {
        var row = new LinkedHashMap<String, Object>();
        row.put("name", "lookup");
        row.put("description", "A tool for the test.");
        row.put("input_schema", Map.of("type", "object", "properties", Map.of("id", Map.of("type", "string")),
                "required", List.of("id")));
        row.put("route", route);
        return row;
    }

    private static Map<String, Object> input(String route, Object... settings) {
        var input = new LinkedHashMap<String, Object>();
        input.put("tools", List.of(row(route)));
        input.put("prompt", "May C-1 order 4200 EUR?");
        for (int i = 0; i < settings.length; i += 2) {
            input.put((String) settings[i], settings[i + 1]);
        }
        return input;
    }

    private static Map<String, Object> toolUse() {
        return Map.of("status", "tool_use", "calls", List.of(Map.of("id", "c1", "name", "lookup",
                        "input", Map.of("id", "C-1"))), "conversation", CONV, "turn", 1, "model", "claude-opus-5-5",
                "usage", Map.of("input_tokens", 900, "output_tokens", 60));
    }

    private static Map<String, Object> done() {
        return Map.of("status", "done", "text", "Yes.", "conversation", CONV, "turn", 2, "model", "claude-opus-5-5",
                "usage", Map.of("input_tokens", 100, "output_tokens", 20, "cache_read_input_tokens", 800));
    }

    private static EventEnvelope run(Map<String, Object> input) throws Exception {
        var po = new PostOffice("unit.test", "trace-agent-loop", "TEST /agent-loop");
        return po.request(new EventEnvelope().setTo("llm.run").setBody(input), 15000).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    void oneToolThenTheAnswer() throws Exception {
        REPLIES.add(toolUse());
        REPLIES.add(done());
        var response = run(input(CREDIT_LIMIT));
        assertEquals(200, response.getStatus(), String.valueOf(response.getError()));
        var envelope = (Map<String, Object>) response.getBody();
        assertEquals("done", envelope.get("status"));
        assertEquals("Yes.", envelope.get("text"));
        assertEquals(CONV, envelope.get("conversation"));
        assertEquals(2, envelope.get("turn"));
        assertEquals(2, envelope.get("turns"));
        var usage = (Map<String, Object>) envelope.get("usage");
        assertEquals(1000, ((Number) usage.get("input_tokens")).intValue());
        assertEquals(800, ((Number) usage.get("cache_read_input_tokens")).intValue());
        assertEquals(List.of(Map.of("id", "c1", "name", "lookup", "route", CREDIT_LIMIT, "status", "ok")),
                envelope.get("calls"));
        assertEquals(2, REQUESTS.size());
        var first = REQUESTS.getFirst();
        assertEquals("May C-1 order 4200 EUR?", first.get("prompt"));
        var tools = (List<Map<String, Object>>) first.get("tools");
        assertFalse(tools.getFirst().containsKey("route"), "the model never sees a route");
        var timeout = ((Number) ((Map<String, Object>) first.get("params")).get("timeout_ms")).longValue();
        assertTrue(timeout > 20000 && timeout <= 24000, "timeout_ms " + timeout);
        var second = REQUESTS.get(1);
        assertFalse(second.containsKey("prompt"));
        assertEquals(1, second.get("turn"));
        var results = (List<Map<String, Object>>) second.get("results");
        assertEquals("c1", results.getFirst().get("id"));
        var output = (Map<String, Object>) results.getFirst().get("output");
        assertEquals(5000, ((Number) output.get("limit")).intValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aRouteThatThrowsAnswersWithItsStatus() throws Exception {
        REPLIES.add(toolUse());
        REPLIES.add(done());
        var response = run(input(FAILING_LEDGER));
        assertEquals(200, response.getStatus());
        var results = (List<Map<String, Object>>) REQUESTS.get(1).get("results");
        assertEquals("error 503: ledger offline", results.getFirst().get("error"));
        var envelope = (Map<String, Object>) response.getBody();
        assertEquals("error", ((List<Map<String, Object>>) envelope.get("calls")).getFirst().get("status"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aSlowRouteTimesOutAndTheDeadlineStopsTheRun() throws Exception {
        REPLIES.add(toolUse());
        var response = run(input(SLOW_LEDGER, "timeout", "3s"));
        assertEquals(200, response.getStatus());
        var envelope = (Map<String, Object>) response.getBody();
        assertEquals("cap", envelope.get("status"));
        assertEquals("deadline", envelope.get("cap"));
        assertEquals("error", ((List<Map<String, Object>>) envelope.get("calls")).getFirst().get("status"));
        assertEquals(1, REQUESTS.size(), "no model turn starts after the deadline");
    }

    @Test
    void aPreflightFailureReachesTheCallerWithItsStatus() throws Exception {
        var response = run(input("test.nobody.home"));
        assertEquals(400, response.getStatus());
        assertEquals("tools[0].route 'test.nobody.home' is not reachable - no local function, Event-over-HTTP " +
                "target or mesh route serves it", response.getError());
        assertTrue(REQUESTS.isEmpty(), "no token is spent on a bad catalog");
    }

    @Test
    void aFailedTurnReachesTheCallerWithItsStatus() throws Exception {
        REPLIES.add(Map.of("_status", 429, "_message", "LLM provider rate limit - 429 rate_limit_error: slow down"));
        var response = run(input(CREDIT_LIMIT));
        assertEquals(429, response.getStatus());
        assertEquals("llm.chat failed - LLM provider rate limit - 429 rate_limit_error: slow down",
                response.getError());
    }

    @Test
    void configuredDefaultsReachTheLoop() {
        var defaults = LoopSettings.Defaults.fromConfig();
        assertEquals("32768", defaults.resultLimit());
        assertEquals(LoopSettings.Defaults.builtIn().maxTurns(), defaults.maxTurns());
        assertEquals("25s", defaults.timeout());
    }

    @Test
    void theGatewayFindsLocalRoutesOnly() {
        var gateway = new PlatformGateway(new PostOffice("unit.test", "trace-agent-loop", "TEST /agent-loop"));
        assertTrue(gateway.reachable(CREDIT_LIMIT));
        assertTrue(gateway.reachable(PlatformGateway.LLM_CHAT));
        assertFalse(gateway.reachable("test.nobody.home"));
        assertFalse(gateway.reachable("not a route"));
    }

    @Test
    void aRequestThatCannotBeSentBecomesAnErrorResponse() {
        assertEquals(404, AgentLoop.failure(new IllegalArgumentException("Route x not found")).getStatus());
        assertEquals(408, AgentLoop.failure(new java.util.concurrent.TimeoutException("late")).getStatus());
        assertEquals(418, AgentLoop.failure(new java.util.concurrent.CompletionException(
                new AppException(418, "teapot"))).getStatus());
        var other = AgentLoop.failure(new IllegalStateException("broken"));
        assertEquals(500, other.getStatus());
        assertEquals("broken", other.getError());
    }

    @Test
    void aCutResultKeepsWholeCharacters() {
        var text = "😀😀😀";
        var cut = (String) AgentLoop.capped(text, 2);
        assertEquals("😀😀\n[the result was cut at 2 of 3 characters]", cut);
        assertEquals("", AgentLoop.capped(null, 2));
        var structured = new ArrayList<Object>(List.of(Map.of("k", "v")));
        assertNotNull(AgentLoop.capped(structured, 100));
    }
}
