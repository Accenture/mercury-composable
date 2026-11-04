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

package com.accenture.minigraph.progress;

import com.accenture.adapters.FlowExecutor;
import com.accenture.minigraph.mock.ProgressCollector;
import com.accenture.minigraph.mock.ProgressSender;
import com.accenture.minigraph.start.PlaygroundLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Progress events from a graph run (RFC-0011 section 8.4), end to end: the engine's {@code node.start} for a node
 * with {@code progress}, a function's own events through its node's relay, and one terminal event - to a declared
 * {@code x-return-path}, and as server-sent events on a REST request with {@code stream: true}.
 */
class ProgressEventTest {
    private static final long TIMEOUT = 8000;
    private static final String TYPE = "type";
    private static final String PROGRESS = "progress";
    private static final String RESULT = "result";
    private static final String GRAPH_1 = "unit-test-progress-1";
    private static final String UNDECLARED = "test.undeclared.collector";
    private static final List<Map<String, Object>> UNDECLARED_EVENTS = new CopyOnWriteArrayList<>();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static String target;

    @BeforeAll
    static void beforeAll() {
        PlaygroundLoader.main(new String[0]);
        target = "http://localhost:" + AppConfigReader.getInstance().getProperty("rest.server.port");
        Platform.getInstance().registerPrivate(UNDECLARED,
                (TypedLambdaFunction<Map<String, Object>, Void>) (headers, input, instance) -> {
                    UNDECLARED_EVENTS.add(input);
                    return null;
                }, 1);
    }

    private static EventEnvelope runFromEvent(String graphId, String cid, String returnPath, Map<String, Object> body)
            throws Exception {
        return runFromEvent(graphId, cid, returnPath, body, 20000);
    }

    private static EventEnvelope runFromEvent(String graphId, String cid, String returnPath, Map<String, Object> body,
                                              long ttl) throws Exception {
        Map<String, Object> dataset = Map.of("body", body, "header", Map.of("x-return-path", returnPath),
                "path_parameter", Map.of("graph_id", graphId), "ttl", ttl);
        var po = new PostOffice("unit.test", Utility.getInstance().getUuid(), "TEST /progress");
        return FlowExecutor.getInstance().request(po, "graph-executor", dataset, cid, TIMEOUT)
                .get(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private static List<Map<String, Object>> from(List<Map<String, Object>> events, String node) {
        return events.stream().filter(e -> node.equals(e.get("node")) && PROGRESS.equals(e.get(TYPE))).toList();
    }

    @Test
    void anEventEntryGetsProgressThenTheResult() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runFromEvent(GRAPH_1, cid, ProgressCollector.ROUTE, Map.of("item", "x"));
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        var events = ProgressCollector.awaitTerminal(cid, TIMEOUT);
        assertEquals(5, events.size(), String.valueOf(events));
        // the engine's events: node.start for each node with 'progress', numbered by the engine
        var root = from(events, "root").getFirst();
        assertEquals("node.start", root.get("event"));
        assertEquals("Started", root.get("message"));
        assertEquals(1, root.get("seq"));
        assertEquals(GRAPH_1, root.get("graph"));
        assertEquals(cid, root.get("cid"));
        assertTrue(String.valueOf(root.get("time")).matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d{3}Z"),
                String.valueOf(root.get("time")));
        var work = from(events, "work");
        assertEquals(3, work.size(), String.valueOf(work));
        assertEquals("node.start", work.getFirst().get("event"));
        assertEquals("Working on it", work.getFirst().get("message"));
        assertEquals(2, work.getFirst().get("seq"));
        // the function's events, through the node's relay: its own numbering, its graph and node named for it
        var own = work.subList(1, 3);
        assertEquals(List.of("step", "step"), own.stream().map(e -> e.get("event")).toList());
        assertEquals(List.of(1, 2), own.stream().map(e -> e.get("seq")).toList());
        own.forEach(e -> {
            assertEquals(GRAPH_1, e.get("graph"));
            assertEquals(cid, e.get("cid"));
        });
        assertTrue(ProgressSender.returnPathSeen(cid).startsWith("graph.progress."),
                "the function gets its node's relay, never the caller's route");
        // the terminal event is last: the relay is drained before it is sent
        var terminal = events.getLast();
        assertEquals(RESULT, terminal.get(TYPE));
        assertEquals(3, terminal.get("seq"));
        assertEquals(200, terminal.get("status"));
        assertEquals("x", ((Map<?, ?>) terminal.get("body")).get("item"));
    }

    @Test
    void aSynchronousSubgraphReportsThroughItsParent() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runFromEvent("unit-test-progress-4", cid, ProgressCollector.ROUTE, Map.of("item", "p"));
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        var events = ProgressCollector.awaitTerminal(cid, TIMEOUT);
        var parent = events.stream().filter(e -> "unit-test-progress-4".equals(e.get("graph"))).toList();
        var child = events.stream().filter(e -> GRAPH_1.equals(e.get("graph"))).toList();
        assertEquals(List.of(PROGRESS, RESULT), parent.stream().map(e -> e.get(TYPE)).toList(),
                "the parent's node.start, then its terminal event: " + events);
        assertEquals("Delegating", parent.getFirst().get("message"));
        assertEquals(RESULT, parent.getLast().get(TYPE), "the subgraph's own result is not the run's end");
        assertEquals(List.of("root", "work", "work", "work"), child.stream().map(e -> e.get("node")).toList(),
                "the subgraph names itself: " + child);
        assertTrue(child.stream().allMatch(e -> PROGRESS.equals(e.get(TYPE))));
        assertEquals(6, events.size(), String.valueOf(events));
        assertEquals(RESULT, events.getLast().get(TYPE));
    }

    @Test
    void everyEventASenderSentArrivesBeforeTheTerminalEvent() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runFromEvent(GRAPH_1, cid, ProgressCollector.ROUTE, Map.of("item", "b", "mode", "burst"));
        assertEquals(200, response.getStatus());
        var events = ProgressCollector.awaitTerminal(cid, TIMEOUT);
        var own = events.stream().filter(e -> "step".equals(e.get("event"))).toList();
        assertEquals(5000, own.size(), "the relay is drained before the terminal event");
        assertEquals(RESULT, events.getLast().get(TYPE));
    }

    @Test
    void aFailedRunEndsWithAnErrorEvent() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runFromEvent("unit-test-progress-3", cid, ProgressCollector.ROUTE, Map.of("item", "y"));
        assertEquals(422, response.getStatus());
        var terminal = ProgressCollector.awaitTerminal(cid, TIMEOUT).getLast();
        assertEquals("error", terminal.get(TYPE));
        assertEquals(422, terminal.get("status"));
        assertEquals("refused by the test", terminal.get("message"));
    }

    @Test
    void aRunThatTimesOutEndsWithAnErrorEvent() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runFromEvent(GRAPH_1, cid, ProgressCollector.ROUTE, Map.of("item", "w", "mode", "slow"), 1500);
        assertEquals(408, response.getStatus(), String.valueOf(response.getBody()));
        var events = ProgressCollector.awaitTerminal(cid, TIMEOUT);
        assertEquals(4, from(events, "root").size() + from(events, "work").size(), "the progress sent before");
        var terminal = events.getLast();
        assertEquals("error", terminal.get(TYPE), "the flow's end ends the channel: " + events);
        assertEquals(408, terminal.get("status"));
    }

    @Test
    void aStreamThatTimesOutEndsWithAnErrorEvent() throws Exception {
        var request = HttpRequest.newBuilder(URI.create(target + "/api/stream/short/" + GRAPH_1))
                .timeout(Duration.ofMillis(TIMEOUT)).header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString("{\"item\": \"v\", \"mode\": \"slow\"}")).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        assertEquals(200, response.statusCode(), "the head was committed by the first progress event");
        var lines = response.body().filter(line -> !line.isBlank()).toList();
        var names = lines.stream().filter(l -> l.startsWith("event:")).map(l -> l.substring(6).trim()).toList();
        // the flow's own single-shot 408 cannot write to the started stream; the flow's end ends it in-band
        assertEquals(List.of(PROGRESS, PROGRESS, PROGRESS, PROGRESS, "error", "done"), names, String.valueOf(lines));
        var error = lines.stream().filter(l -> l.startsWith("data:") && l.contains("\"type\":\"error\"")).findFirst();
        assertTrue(error.isPresent(), String.valueOf(lines));
        assertEquals(408, ((Number) json(error.get().substring(5).trim()).get("status")).intValue());
    }

    @Test
    void aNodeWithoutProgressSendsNothingAndPassesNoReturnPath() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runFromEvent("unit-test-progress-2", cid, ProgressCollector.ROUTE, Map.of("item", "z"));
        assertEquals(200, response.getStatus());
        var events = ProgressCollector.awaitTerminal(cid, TIMEOUT);
        assertEquals(List.of(RESULT), events.stream().map(e -> e.get(TYPE)).toList(), "only the terminal event");
        assertEquals("none", ProgressSender.returnPathSeen(cid), "a node without 'progress' never sees the header");
    }

    @Test
    void anUndeclaredReturnPathIsIgnored() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var response = runFromEvent(GRAPH_1, cid, UNDECLARED, Map.of("item", "u"));
        assertEquals(200, response.getStatus(), "the run continues without progress");
        assertEquals("none", ProgressSender.returnPathSeen(cid));
        assertTrue(UNDECLARED_EVENTS.stream().noneMatch(e -> cid.equals(e.get("cid"))));
    }

    @Test
    void aPlainRestRequestIgnoresAnInboundReturnPathHeader() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target).setUrl("/api/graph/" + GRAPH_1)
                .setBody(Map.of("item", "r")).setHeader("Content-Type", "application/json")
                .setHeader("X-Correlation-Id", cid).setHeader("X-Return-Path", ProgressCollector.ROUTE);
        var po = new PostOffice("unit.test", Utility.getInstance().getUuid(), "TEST /progress");
        var response = po.request(new EventEnvelope().setTo("async.http.request").setBody(request), TIMEOUT).get();
        assertEquals(200, response.getStatus());
        assertEquals("none", ProgressSender.returnPathSeen(cid), "the REST automation sets a REST entry's return route");
        assertTrue(ProgressCollector.events(cid).isEmpty());
    }

    @Test
    void aStreamingRestRequestGetsServerSentEvents() throws Exception {
        var lines = stream(GRAPH_1, Map.of("item", "s"));
        var names = lines.stream().filter(l -> l.startsWith("event:")).map(l -> l.substring(6).trim()).toList();
        assertEquals(List.of(PROGRESS, PROGRESS, PROGRESS, PROGRESS, RESULT, "done"), names, String.valueOf(lines));
        var data = new ArrayList<Map<String, Object>>();
        for (var line : lines) {
            if (line.startsWith("data:") && line.contains("\"type\"")) {
                data.add(json(line.substring(5).trim()));
            }
        }
        assertEquals("Started", data.getFirst().get("message"));
        assertEquals("s", ((Map<?, ?>) data.get(4).get("body")).get("item"));
    }

    @Test
    void aStreamingRequestThatSendsNoProgressAnswersSingleShot() throws Exception {
        var response = client.send(post("unit-test-progress-2", Map.of("item", "t")),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertNotEquals("text/event-stream", response.headers().firstValue("content-type").orElse(""),
                "a run that sends no progress never opens the stream");
        assertFalse(response.body().contains("event:"), response.body());
        assertEquals("t", json(response.body()).get("item"), "the single-shot response carries the result");
    }

    @Test
    void aWebSocketSessionGetsProgressThenTheResultAndCloses() throws Exception {
        var cid = Utility.getInstance().getUuid();
        var session = new Frames();
        var ws = client.newWebSocketBuilder().buildAsync(URI.create(target.replace("http", "ws") +
                "/ws/graph-run/" + GRAPH_1), session).get(TIMEOUT, TimeUnit.MILLISECONDS);
        ws.sendText("{\"header\": {\"X-Correlation-Id\": \"" + cid + "\"}, \"body\": {\"item\": \"q\"}}", true);
        assertTrue(session.closed.await(TIMEOUT, TimeUnit.MILLISECONDS), "the session closes after the result");
        var frames = session.frames.stream().map(ProgressEventTest::json).toList();
        assertEquals(List.of(PROGRESS, PROGRESS, PROGRESS, PROGRESS, RESULT),
                frames.stream().map(f -> f.get(TYPE)).toList(), String.valueOf(frames));
        frames.forEach(f -> assertEquals(cid, f.get("cid"), "the session's X-Correlation-Id is the run's cid"));
        assertEquals("q", ((Map<?, ?>) frames.getLast().get("body")).get("item"));
    }

    @Test
    void aWebSocketSessionForAGraphThatIsNotDeployedEndsWithAnError() throws Exception {
        var session = new Frames();
        var ws = client.newWebSocketBuilder().buildAsync(URI.create(target.replace("http", "ws") +
                "/ws/graph-run/no-such-graph"), session).get(TIMEOUT, TimeUnit.MILLISECONDS);
        ws.sendText("{\"body\": {}}", true);
        assertTrue(session.closed.await(TIMEOUT, TimeUnit.MILLISECONDS));
        var terminal = json(session.frames.getLast());
        assertEquals("error", terminal.get(TYPE));
        assertEquals(404, ((Number) terminal.get("status")).intValue());
    }

    @Test
    void aWebSocketFrameThatIsNotAnObjectStartsNothing() throws Exception {
        var session = new Frames();
        var ws = client.newWebSocketBuilder().buildAsync(URI.create(target.replace("http", "ws") +
                "/ws/graph-run/" + GRAPH_1), session).get(TIMEOUT, TimeUnit.MILLISECONDS);
        ws.sendText("[1, 2]", true);
        assertTrue(session.closed.await(TIMEOUT, TimeUnit.MILLISECONDS));
        var refusal = json(session.frames.getLast());
        assertEquals(400, ((Number) refusal.get("status")).intValue());
        assertEquals("The run's input is a JSON object with an optional body and header", refusal.get("message"));
    }

    /** A session's text frames, joined from their parts, and its close. */
    private static final class Frames implements java.net.http.WebSocket.Listener {
        final List<String> frames = new CopyOnWriteArrayList<>();
        final java.util.concurrent.CountDownLatch closed = new java.util.concurrent.CountDownLatch(1);
        private final StringBuilder part = new StringBuilder();

        @Override
        public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket ws, CharSequence data,
                                                               boolean last) {
            part.append(data);
            if (last) {
                frames.add(part.toString());
                part.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onClose(java.net.http.WebSocket ws, int status, String why) {
            closed.countDown();
            return null;
        }
    }

    private static HttpRequest post(String graphId, Map<String, Object> body) {
        var text = SimpleMapper.getInstance().getMapper().writeValueAsString(body);
        return HttpRequest.newBuilder(URI.create(target + "/api/stream/graph/" + graphId))
                .timeout(Duration.ofMillis(TIMEOUT)).header("Content-Type", "application/json")
                .header("Accept", "text/event-stream").POST(HttpRequest.BodyPublishers.ofString(text)).build();
    }

    private static List<String> stream(String graphId, Map<String, Object> body) throws Exception {
        var response = client.send(post(graphId, body), HttpResponse.BodyHandlers.ofLines());
        assertEquals(200, response.statusCode());
        assertEquals("text/event-stream", response.headers().firstValue("content-type").orElse(""));
        return response.body().filter(line -> !line.isBlank()).toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> json(String text) {
        return SimpleMapper.getInstance().getMapper().readValue(text, Map.class);
    }
}
