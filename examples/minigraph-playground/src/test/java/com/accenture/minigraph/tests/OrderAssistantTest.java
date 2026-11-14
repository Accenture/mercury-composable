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

package com.accenture.minigraph.tests;

import com.accenture.minigraph.start.MainApp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The order-assistant demo graph end to end: an {@code llm.run} node with a fixed tool catalog, two read
 * tools, one write that waits for a supervisor through suspend and resume on a real (embedded) Redis,
 * and the run watched through the streaming REST entry and the WebSocket entry. The model turns are
 * scripted by {@link MockLlmChat}, so no token is spent; the loop, the tool routes, the graph's decisions
 * and the suspension are the real ones.
 */
class OrderAssistantTest {
    private static final Logger log = LoggerFactory.getLogger(OrderAssistantTest.class);
    private static final long TIMEOUT = 10000;
    private static final String GRAPH = "order-assistant";
    private static final String QUESTION = "question";
    private static final String DECISIONS = "decisions";
    private static final String TYPE = "type";
    private static final String MESSAGE = "message";
    private static final String CALLS = "calls";
    private static final String TURN = "turn";
    private static final String APPROVAL = "approval";
    private static final String ANSWER = "answer";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static String target;

    @BeforeAll
    static void beforeAll() throws IOException {
        EmbeddedRedis.start();
        MainApp.main(new String[0]);
        var config = AppConfigReader.getInstance();
        target = "http://127.0.0.1:" + config.getProperty("rest.server.port");
    }

    @Test
    void aQuestionIsRequired() throws TimeoutException {
        var response = runGraph(Utility.getInstance().getUuid(), Map.of("customer", "anonymous"));
        assertEquals(400, response.getStatus());
        var body = body(response);
        assertEquals("rejected", body.getElement(TYPE));
        assertTrue(String.valueOf(body.getElement(MESSAGE)).contains("question"), String.valueOf(response.getBody()));
    }

    @Test
    void aDeliveredOrderIsAnsweredWithoutAnApproval() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph(cid, Map.of(QUESTION, "Where is my order ORD-1001?"));
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        var body = body(response);
        assertEquals(ANSWER, body.getElement(TYPE));
        assertTrue(String.valueOf(body.getElement(MESSAGE)).contains("delivered"), String.valueOf(body.getElement(MESSAGE)));
        assertEquals("fresh", body.getElement("run"));
        assertEquals(cid, body.getElement("cid"));
        assertEquals(2, body.getElement(TURN), "two model turns: the tool calls, then the answer");
        var calls = calls(body);
        assertEquals(List.of("order_lookup", "shipment_status"), calls.stream().map(c -> c.get("name")).toList());
        assertEquals(List.of("demo.order.lookup", "demo.shipment.status"), calls.stream().map(c -> c.get("route")).toList());
        calls.forEach(c -> assertEquals("ok", c.get("status"), String.valueOf(c)));
        assertEquals("mock-llm", body.getElement("model"));
        assertNotNull(body.getElement("usage.input_tokens"));
    }

    @Test
    void aLostShipmentWaitsForTheSupervisorsRefundApproval() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        // run 1: the model checks the order and the shipment, then asks for the refund - a marked write waits
        var advice = body(runGraph(cid, Map.of(QUESTION, "My order ORD-1002 never arrived. Can you help?")));
        assertEquals(APPROVAL, advice.getElement(TYPE), String.valueOf(advice.getMap()));
        assertEquals("fresh", advice.getElement("run"));
        assertEquals(2, advice.getElement(TURN), "the refund is asked for on the second model turn");
        assertTrue(String.valueOf(advice.getElement(MESSAGE)).contains("supervisor"), String.valueOf(advice.getElement(MESSAGE)));
        var waiting = calls(advice);
        assertEquals(1, waiting.size(), String.valueOf(waiting));
        var refund = waiting.getFirst();
        assertEquals("issue_refund", refund.get("name"));
        assertEquals("demo.refund.issue", refund.get("route"));
        assertEquals(true, refund.get("wait"));
        var arguments = new MultiLevelMap(asMap(refund.get("input")));
        assertEquals("ORD-1002", arguments.getElement("order_id"));
        assertEquals(174.0, ((Number) arguments.getElement("amount")).doubleValue());
        String callId = String.valueOf(refund.get("id"));
        var answered = asList(advice.getElement("answered"));
        assertEquals(List.of("order_lookup", "shipment_status"), answered.stream().map(c -> c.get("name")).toList(),
                "the advice also shows the calls the loop already answered");
        answered.forEach(c -> assertEquals("ok", c.get("status"), String.valueOf(c)));
        // run 2: a resume without decisions keeps waiting and re-advises
        var again = body(runGraph(cid, Map.of("note", "just checking")));
        assertEquals(APPROVAL, again.getElement(TYPE), String.valueOf(again.getMap()));
        assertEquals("resume", again.getElement("run"));
        assertEquals(2, again.getElement(TURN));
        assertEquals(callId, calls(again).getFirst().get("id"), "the same call still waits");
        // run 3: decisions for another turn than the one that waits are not applied
        var stale = body(runGraph(cid, Map.of(TURN, 1, DECISIONS, List.of(decision(callId, true, "late")))));
        assertEquals(APPROVAL, stale.getElement(TYPE), String.valueOf(stale.getMap()));
        // run 4: a decision for a call that is not waiting is refused by the loop before any token is spent,
        // and the graph re-advises instead of losing the transaction
        var refused = body(runGraph(cid, Map.of(TURN, 2, DECISIONS, List.of(decision("not-a-call", true, "typo")))));
        assertEquals(APPROVAL, refused.getElement(TYPE), String.valueOf(refused.getMap()));
        assertTrue(String.valueOf(refused.getElement("error")).contains("not a call waiting for approval"),
                String.valueOf(refused.getElement("error")));
        assertEquals(callId, calls(refused).getFirst().get("id"), "the same call still waits");
        // run 5: the supervisor approves - the loop runs the call it showed and the model answers
        var answer = body(runGraph(cid, Map.of(TURN, 2, DECISIONS, List.of(decision(callId, true, "Checked with the carrier")))));
        assertEquals(ANSWER, answer.getElement(TYPE), String.valueOf(answer.getMap()));
        assertEquals("resume", answer.getElement("run"));
        assertEquals(3, answer.getElement(TURN));
        var text = String.valueOf(answer.getElement(MESSAGE));
        assertTrue(text.contains("refund RF-") && text.contains("174.0 USD"), text);
        // the resumed call's log holds the calls it answered itself: the first run answered the two lookups
        var calls = calls(answer);
        assertEquals(List.of("issue_refund"), calls.stream().map(c -> c.get("name")).toList());
        assertEquals("ok", calls.getFirst().get("status"), String.valueOf(calls));
        // a second transaction for the same order: the write fails with 409 and the model reads the error
        var second = Utility.getInstance().getUuid();
        var secondAdvice = body(runGraph(second, Map.of(QUESTION, "Please refund ORD-1002, it is lost.")));
        assertEquals(APPROVAL, secondAdvice.getElement(TYPE), String.valueOf(secondAdvice.getMap()));
        var secondId = String.valueOf(calls(secondAdvice).getFirst().get("id"));
        var declined = body(runGraph(second, Map.of(TURN, secondAdvice.getElement(TURN),
                DECISIONS, List.of(decision(secondId, true, "approved")))));
        assertEquals(ANSWER, declined.getElement(TYPE), String.valueOf(declined.getMap()));
        var reason = String.valueOf(declined.getElement(MESSAGE));
        assertTrue(reason.contains("could not issue") && reason.contains("already refunded"), reason);
        assertEquals("error", calls(declined).getLast().get("status"), String.valueOf(calls(declined)));
        log.info("order-assistant: approved refund for cid {}, second attempt refused by the order book", cid);
    }

    @Test
    void aRefusedDecisionGoesBackToTheModelAsTheCallsError() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var advice = body(runGraph(cid, Map.of(QUESTION, "Order ORD-1004 is lost, I want my money back.")));
        assertEquals(APPROVAL, advice.getElement(TYPE), String.valueOf(advice.getMap()));
        var callId = String.valueOf(calls(advice).getFirst().get("id"));
        var answer = body(runGraph(cid, Map.of(TURN, advice.getElement(TURN),
                DECISIONS, List.of(decision(callId, false, "The customer prefers a replacement")))));
        assertEquals(ANSWER, answer.getElement(TYPE), String.valueOf(answer.getMap()));
        var text = String.valueOf(answer.getElement(MESSAGE));
        assertTrue(text.contains("could not issue") && text.contains("prefers a replacement"), text);
        assertEquals("refused", calls(answer).getLast().get("status"), String.valueOf(calls(answer)));
    }

    @Test
    void aWatchedRunStreamsItsProgressEventsThenTheResult() throws IOException, InterruptedException {
        var cid = Utility.getInstance().getUuid();
        var request = HttpRequest.newBuilder(URI.create(target + "/api/stream/graph/" + GRAPH))
                .timeout(Duration.ofMillis(TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("X-Correlation-Id", cid)
                .POST(HttpRequest.BodyPublishers.ofString("{\"question\": \"Has order ORD-1003 shipped?\"}")).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        assertEquals(200, response.statusCode());
        var lines = response.body().filter(line -> !line.isBlank()).toList();
        var names = lines.stream().filter(l -> l.startsWith("event:")).map(l -> l.substring(6).trim()).toList();
        assertEquals("result", names.get(names.size() - 2), String.valueOf(names));
        assertEquals("done", names.getLast(), String.valueOf(names));
        var events = lines.stream().filter(l -> l.startsWith("data:") && l.contains("\"type\""))
                .map(l -> json(l.substring(5).trim())).toList();
        var progress = events.stream().filter(e -> "progress".equals(e.get(TYPE))).toList();
        assertEquals("Investigating the order", progress.getFirst().get(MESSAGE), "the node's own progress text");
        var loopEvents = progress.stream().map(e -> e.get("event")).filter(e -> e != null).toList();
        assertTrue(loopEvents.containsAll(List.of("run.start", "turn.start", "tool.start", "tool.end", "turn.end", "run.end")),
                String.valueOf(loopEvents));
        var tools = progress.stream().filter(e -> "tool.start".equals(e.get("event")))
                .map(e -> asMap(e.get("data")).get("name")).toList();
        assertEquals(List.of("order_lookup", "shipment_status"), tools);
        progress.forEach(e -> assertEquals(cid, e.get("cid")));
        var result = events.getLast();
        assertEquals("result", result.get(TYPE));
        assertEquals(ANSWER, asMap(result.get("body")).get(TYPE), String.valueOf(result));
        assertTrue(String.valueOf(asMap(result.get("body")).get(MESSAGE)).contains("in_transit"), String.valueOf(result));
    }

    @Test
    void aWebSocketSessionGetsTheEventsThenTheResultAndCloses() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var session = new Frames();
        var ws = client.newWebSocketBuilder().buildAsync(URI.create(target.replace("http", "ws") +
                "/ws/graph-run/" + GRAPH), session).get(TIMEOUT, TimeUnit.MILLISECONDS);
        ws.sendText("{\"header\": {\"X-Correlation-Id\": \"" + cid + "\"}, " +
                "\"body\": {\"question\": \"Where is order ORD-1001?\"}}", true);
        assertTrue(session.closed.await(TIMEOUT, TimeUnit.MILLISECONDS), "the session closes after the result");
        var frames = session.frames.stream().map(OrderAssistantTest::json).toList();
        assertTrue(frames.size() > 2, String.valueOf(frames));
        frames.subList(0, frames.size() - 1).forEach(f -> assertEquals("progress", f.get(TYPE), String.valueOf(f)));
        assertEquals("run.end", frames.get(frames.size() - 2).get("event"), "the loop's last event precedes the result");
        var result = frames.getLast();
        assertEquals("result", result.get(TYPE), String.valueOf(result));
        assertEquals(cid, result.get("cid"));
        assertEquals(ANSWER, asMap(result.get("body")).get(TYPE), String.valueOf(result));
    }

    private static Map<String, Object> decision(String id, boolean approved, String note) {
        return Map.of("id", id, "approved", approved, "note", note);
    }

    @SuppressWarnings("unchecked")
    private static MultiLevelMap body(EventEnvelope response) {
        assertInstanceOf(Map.class, response.getBody(), String.valueOf(response.getBody()));
        return new MultiLevelMap((Map<String, Object>) response.getBody());
    }

    private static List<Map<String, Object>> calls(MultiLevelMap body) {
        return asList(body.getElement(CALLS));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object value) {
        assertInstanceOf(List.class, value, String.valueOf(value));
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        assertInstanceOf(Map.class, value, String.valueOf(value));
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> json(String text) {
        return SimpleMapper.getInstance().getMapper().readValue(text, Map.class);
    }

    private EventEnvelope runGraph(String cid, Map<String, Object> body) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/" + GRAPH)
                .setBody(body)
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .setHeader("X-Correlation-Id", cid);
        var event = new EventEnvelope().setTo("async.http.request").setBody(request);
        var po = PostOffice.trackable("unit.test",
                String.format("%032x", Math.abs(cid.hashCode())), "TEST /graph/" + GRAPH);
        var response = po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (response.hasError()) {
            log.warn("HTTP-{} - {}", response.getStatus(), response.getBody());
        }
        return response;
    }

    /** Collects the text frames of a WebSocket session and counts down when the server closes it. */
    private static final class Frames implements WebSocket.Listener {
        private final List<String> frames = new ArrayList<>();
        private final CountDownLatch closed = new CountDownLatch(1);
        private final StringBuilder partial = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                frames.add(partial.toString());
                partial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.countDown();
            return null;
        }
    }
}
