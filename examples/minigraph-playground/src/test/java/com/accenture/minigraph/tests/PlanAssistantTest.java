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
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The plan-assistant demo graph end to end: the question shape of RFC-0014. The model drafts the plans and asks the
 * owner through {@code ask_owner}, a catalog row marked {@code ask: true} with no route; the call waits like a marked
 * write - told to the owner through {@code person.ask} ({@link MockAskPerson}), staged at the checkpoint, suspended
 * on the embedded Redis - and the owner's answer comes back into the same conversation as the call's result. The model
 * turns are scripted by {@link MockLlmChat}; the loop, the graph and the suspension are the real ones.
 */
class PlanAssistantTest {
    private static final Logger log = LoggerFactory.getLogger(PlanAssistantTest.class);
    private static final long TIMEOUT = 10000;
    private static final String GRAPH = "plan-assistant";
    private static final String TASK = "task";
    private static final String DECISIONS = "decisions";
    private static final String TYPE = "type";
    private static final String MESSAGE = "message";
    private static final String CALLS = "calls";
    private static final String TURN = "turn";
    private static String target;

    @BeforeAll
    static void beforeAll() throws IOException {
        EmbeddedRedis.start();
        MainApp.main(new String[0]);
        var config = AppConfigReader.getInstance();
        target = "http://127.0.0.1:" + config.getProperty("rest.server.port");
    }

    @Test
    void aTaskIsRequired() throws TimeoutException {
        var response = runGraph(Utility.getInstance().getUuid(), Map.of("owner", "anonymous"));
        assertEquals(400, response.getStatus());
        var body = body(response);
        assertEquals("rejected", body.getElement(TYPE));
        assertTrue(String.valueOf(body.getElement(MESSAGE)).contains("task"), String.valueOf(response.getBody()));
    }

    @Test
    void theOwnerPicksAPlanAndTheAssistantWritesIt() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        // run 1: the model drafts the plans and asks - the ask row waits, and the owner is told
        var advice = body(runGraph(cid, Map.of(TASK, "Ship the payment-service migration")));
        assertEquals("approval", advice.getElement(TYPE), String.valueOf(advice.getMap()));
        assertEquals("fresh", advice.getElement("run"));
        assertEquals(1, advice.getElement(TURN), "the question is asked on the first model turn");
        assertTrue(String.valueOf(advice.getElement(MESSAGE)).contains("owner"), String.valueOf(advice.getElement(MESSAGE)));
        var waiting = calls(advice);
        assertEquals(1, waiting.size(), String.valueOf(waiting));
        var question = waiting.getFirst();
        assertEquals("ask_owner", question.get("name"));
        assertEquals(true, question.get("ask"), "an ask row");
        assertNull(question.get("route"), "an ask row has no route");
        assertEquals(true, question.get("wait"));
        var input = new MultiLevelMap(asMap(question.get("input")));
        assertTrue(String.valueOf(input.getElement("question")).contains("payment-service migration"));
        var options = asStrings(input.getElement("options"));
        assertEquals(3, options.size(), String.valueOf(options));
        assertTrue(String.valueOf(advice.getElement("link")).startsWith(MockAskPerson.REPLY_URL + "/ask/t-"));
        assertEquals(Map.of("x-graph-id", GRAPH, "x-node", "plan"), MockLlmChat.lastGraphContext());
        var told = MockAskPerson.messagesFor(cid);
        assertEquals(1, told.size(), String.valueOf(told));
        var message = new MultiLevelMap(told.getFirst());
        assertEquals("question", message.getElement("kind"), "the kind of a wait on an ask row");
        assertEquals(GRAPH, message.getElement("graph_id"));
        assertEquals("plan", message.getElement("node"));
        assertEquals("2d", message.getElement("ttl"));
        assertEquals(true, asList(message.getElement(CALLS)).getFirst().get("ask"));
        // run 2: the owner answers - the answer is the ask call's result and the model writes the plan
        var chosen = options.get(1);
        var answer = body(runGraph(cid, Map.of(TURN, 1, DECISIONS,
                List.of(Map.of("id", question.get("id"), "answer", chosen)))));
        assertEquals("answer", answer.getElement(TYPE), String.valueOf(answer.getMap()));
        assertEquals("resume", answer.getElement("run"));
        assertEquals(2, answer.getElement(TURN));
        var text = String.valueOf(answer.getElement(MESSAGE));
        assertTrue(text.contains("the owner chose " + chosen) && text.contains("Steps:"), text);
        var log2 = calls(answer);
        assertEquals("ask_owner", log2.getFirst().get("name"));
        assertEquals("answered", log2.getFirst().get("status"), String.valueOf(log2));
        var kinds = MockAskPerson.messagesFor(cid).stream().map(m -> m.get("kind")).toList();
        assertEquals(List.of("question", "advice", "advice"), kinds, "the question, the acknowledgement, the outcome");
        var reported = new MultiLevelMap(MockAskPerson.messagesFor(cid).get(2));
        assertEquals("respond", reported.getElement("node"));
        assertTrue(String.valueOf(reported.getElement("details")).contains(chosen), String.valueOf(reported.getMap()));
        log.info("plan-assistant: the owner chose '{}' for cid {}", chosen, cid);
    }

    @Test
    void aResumeWithoutAnAnswerAsksAgainWithoutTellingTheChannels() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var advice = body(runGraph(cid, Map.of(TASK, "Rotate the signing keys")));
        assertEquals("approval", advice.getElement(TYPE), String.valueOf(advice.getMap()));
        var again = body(runGraph(cid, Map.of("note", "still thinking")));
        assertEquals("approval", again.getElement(TYPE), String.valueOf(again.getMap()));
        assertEquals("resume", again.getElement("run"));
        assertEquals(advice.getElement("link"), again.getElement("link"), "the same link, kept in the model");
        assertEquals(1, MockAskPerson.messagesFor(cid).size(), "a re-advice never tells the channels again");
    }

    @Test
    void anAnswerWithNothingWaitingIsRefused() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph(cid, Map.of(TURN, 1, DECISIONS, List.of(Map.of("id", "toolu_late", "answer", "A"))));
        assertEquals(410, response.getStatus(), String.valueOf(response.getBody()));
        assertEquals("nothing-waiting", body(response).getElement(TYPE));
        assertTrue(MockAskPerson.messagesFor(cid).isEmpty());
    }

    @Test
    void aReadviceWithNothingWaitingIsRefused() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph(cid, Map.of(TURN, 1));
        assertEquals(410, response.getStatus(), String.valueOf(response.getBody()));
        assertEquals("nothing-waiting", body(response).getElement(TYPE));
        assertEquals("fresh", body(response).getElement("run"));
        assertTrue(MockAskPerson.messagesFor(cid).isEmpty());
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
    private static List<String> asStrings(Object value) {
        assertInstanceOf(List.class, value, String.valueOf(value));
        return (List<String>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        assertInstanceOf(Map.class, value, String.valueOf(value));
        return (Map<String, Object>) value;
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
}
