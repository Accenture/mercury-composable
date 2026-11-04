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

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs the shared vector file {@code llm-run-vectors.json} (byte-identical in the Rust repository) against the
 * loop, with a scripted llm.chat and scripted tool routes behind the gateway and a clock the script advances. No
 * token is spent and nothing waits on real time.
 */
class LlmRunVectorsTest {
    private static final Map<String, Object> VECTORS = load();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load() {
        try (InputStream in = LlmRunVectorsTest.class.getResourceAsStream("/vectors/llm-run-vectors.json")) {
            if (in == null) {
                throw new IllegalStateException("llm-run-vectors.json is missing");
            }
            var text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return SimpleMapper.getInstance().getMapper().readValue(text, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(String key) {
        return (List<Map<String, Object>>) VECTORS.get(key);
    }

    @Test
    void theFileNamesItsContract() {
        assertEquals("llm-run-loop", VECTORS.get("contract"));
        assertEquals(1, ((Number) VECTORS.get("version")).intValue());
        var names = new HashSet<String>();
        for (var c : list("cases")) {
            assertTrue(names.add((String) c.get("name")), "duplicate case name " + c.get("name"));
        }
        for (var c : list("schema_cases")) {
            assertTrue(names.add((String) c.get("name")), "duplicate case name " + c.get("name"));
        }
    }

    @TestFactory
    Stream<DynamicTest> loopCases() {
        return list("cases").stream().map(c -> DynamicTest.dynamicTest((String) c.get("name"), () -> runCase(c)));
    }

    @TestFactory
    Stream<DynamicTest> schemaCases() {
        return list("schema_cases").stream().map(c ->
                DynamicTest.dynamicTest((String) c.get("name"), () -> runSchemaCase(c)));
    }

    @SuppressWarnings("unchecked")
    private static void runCase(Map<String, Object> c) throws AppException {
        var clock = new AtomicLong();
        var gateway = new ScriptedGateway(c, clock);
        var loop = new AgentLoop(gateway, defaults((Map<String, Object>) c.get("config")), clock::get);
        var input = (Map<String, Object>) c.get("input");
        var expect = (Map<String, Object>) c.get("expect");
        if (expect.get("error") instanceof Map<?, ?> error) {
            var e = assertThrows(AppException.class, () -> loop.run(input));
            assertEquals(((Number) error.get("status")).intValue(), e.getStatus(), e.getMessage());
            assertEquals(error.get("message"), e.getMessage());
        } else {
            var envelope = loop.run(input);
            var expected = expect.get("envelope");
            if (!ToolSchema.jsonEquals(expected, envelope)) {
                fail("envelope\n expected " + Json.compact(expected) + "\n      got " + Json.compact(envelope));
            }
        }
        gateway.verifyUsed();
    }

    private static LoopSettings.Defaults defaults(Map<String, Object> config) {
        var builtIn = LoopSettings.Defaults.builtIn();
        if (config == null) {
            return builtIn;
        }
        return new LoopSettings.Defaults(
                (String) config.getOrDefault(LoopSettings.Defaults.MAX_TURNS_KEY, builtIn.maxTurns()),
                (String) config.getOrDefault(LoopSettings.Defaults.RESULT_LIMIT_KEY, builtIn.resultLimit()),
                (String) config.getOrDefault(LoopSettings.Defaults.TRANSCRIPT_TTL_KEY, builtIn.transcriptTtl()),
                (String) config.getOrDefault(LoopSettings.Defaults.RETENTION_KEY, builtIn.retention()),
                (String) config.getOrDefault(LoopSettings.Defaults.TIMEOUT_KEY, builtIn.timeout()));
    }

    @SuppressWarnings("unchecked")
    private static void runSchemaCase(Map<String, Object> c) {
        if (c.containsKey("error")) {
            var schema = c.get("schema");
            var e = assertThrows(IllegalArgumentException.class, () -> ToolSchema.compile("schema", schema));
            assertEquals(c.get("error"), e.getMessage());
            return;
        }
        var schema = ToolSchema.compile("schema", c.get("schema"));
        var violations = new ArrayList<String>();
        schema.validate("input", c.get("input"), violations);
        assertEquals(c.get("violations"), violations);
        if (c.containsKey("report")) {
            assertEquals(c.get("report"), ToolSchema.report(violations));
        }
    }

    /**
     * The scripted side of a case: llm.chat answers each turn in order after checking the request the loop sent,
     * and each route call is matched by route and body among the calls the script expects before the next turn.
     */
    private static final class ScriptedGateway implements LoopGateway {
        private final Set<String> routes = new HashSet<>();
        private final boolean llmChat;
        private final AtomicLong clock;
        private final Deque<Map<String, Object>> turns = new ArrayDeque<>();
        private List<Map<String, Object>> pending = new ArrayList<>();
        private int turnNumber;

        @SuppressWarnings("unchecked")
        ScriptedGateway(Map<String, Object> c, AtomicLong clock) {
            this.clock = clock;
            this.llmChat = !Boolean.FALSE.equals(c.get("llm_chat"));
            routes.addAll((List<String>) c.get("routes"));
            turns.addAll((List<Map<String, Object>>) c.get("turns"));
            if (!turns.isEmpty() && turns.peek().containsKey("before")) {
                pending = new ArrayList<>((List<Map<String, Object>>) turns.poll().get("before"));
            }
        }

        @Override
        public boolean reachable(String route) {
            return PlatformGateway.LLM_CHAT.equals(route) ? llmChat : routes.contains(route);
        }

        @Override
        @SuppressWarnings("unchecked")
        public CompletableFuture<EventEnvelope> chat(Map<String, Object> request, long timeoutMs) {
            turnNumber++;
            if (!pending.isEmpty()) {
                throw new AssertionError("turn " + turnNumber + " began before these calls were made: " +
                        Json.compact(pending));
            }
            var turn = turns.poll();
            if (turn == null) {
                throw new AssertionError("an unscripted llm.chat turn " + turnNumber + ": " + Json.compact(request));
            }
            if (!ToolSchema.jsonEquals(turn.get("request"), request)) {
                throw new AssertionError("turn " + turnNumber + " request\n expected " +
                        Json.compact(turn.get("request")) + "\n      got " + Json.compact(request));
            }
            if (turn.get("elapsed_ms") instanceof Number elapsed) {
                clock.addAndGet(elapsed.longValue());
            }
            pending = new ArrayList<>(turn.get("calls") == null ? List.of() :
                    (List<Map<String, Object>>) turn.get("calls"));
            return CompletableFuture.completedFuture(answer(turn));
        }

        @Override
        public CompletableFuture<EventEnvelope> call(String route, Object body, long timeoutMs) {
            for (int i = 0; i < pending.size(); i++) {
                var expected = pending.get(i);
                if (route.equals(expected.get("route")) && ToolSchema.jsonEquals(expected.get("body"), body)) {
                    pending.remove(i);
                    return CompletableFuture.completedFuture(answer(expected));
                }
            }
            throw new AssertionError("an unscripted call to " + route + " with " + Json.compact(body));
        }

        private static EventEnvelope answer(Map<String, Object> scripted) {
            if (scripted.get("error") instanceof Map<?, ?> error) {
                return new EventEnvelope().setStatus(((Number) error.get("status")).intValue())
                        .setBody(error.get("message"));
            }
            return new EventEnvelope().setStatus(200).setBody(scripted.get("reply"));
        }

        void verifyUsed() {
            if (!turns.isEmpty()) {
                fail(turns.size() + " scripted turn(s) never reached");
            }
            if (!pending.isEmpty()) {
                fail("scripted call(s) never made: " + Json.compact(pending));
            }
        }
    }
}
