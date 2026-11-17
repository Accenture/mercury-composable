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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The three demo graphs on the built-in agent tools (RFC-0013), end to end and token-free: the model turns are
 * scripted by {@link MockLlmChat} and the tool routes by {@link MockAgentTools}, so the loop, the catalogs, the
 * graphs' decisions and the suspension of the supervised write are the real ones.
 * <ul>
 * <li>{@code site-notes}: a crawl of a documentation section, then a write of the notes that waits for a person
 *     under the node's default mode and runs when approved;</li>
 * <li>{@code document-ingest}: a document of the synthetic corpus read, its facts extracted and merged into the
 *     knowledge graph by an ingest node that declares {@code mode: auto}, so no approval is asked;</li>
 * <li>{@code document-query}: the schema read, one read-only query, an answer that cites the sources.</li>
 * </ul>
 */
class AgentToolsGraphsTest {
    private static final long TIMEOUT = 10000;
    private static final String TYPE = "type";
    private static final String MESSAGE = "message";
    private static final String CALLS = "calls";
    private static final String TURN = "turn";
    private static final String ANSWER = "answer";
    private static final String APPROVAL = "approval";
    private static String target;

    @BeforeAll
    static void beforeAll() throws IOException {
        EmbeddedRedis.start();
        MainApp.main(new String[0]);
        var config = AppConfigReader.getInstance();
        target = "http://127.0.0.1:" + config.getProperty("rest.server.port");
    }

    @BeforeEach
    void cleanSlate() {
        MockAgentTools.reset();
    }

    @Test
    void siteNotesRejectsARequestWithoutASectionOrATopic() throws TimeoutException {
        var response = runGraph("site-notes", Utility.getInstance().getUuid(), Map.of("topic", "configuration"));
        assertEquals(400, response.getStatus());
        assertEquals("rejected", body(response).getElement(TYPE));
        var second = runGraph("site-notes", Utility.getInstance().getUuid(), Map.of("section", MockAgentTools.SECTION));
        assertEquals(400, second.getStatus());
    }

    @Test
    void siteNotesCrawlsThenWaitsForTheWriteThenAnswersWithThePath() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var request = Map.<String, Object>of("section", MockAgentTools.SECTION, "topic", "Configuration keys");
        // run 1: the crawl runs unasked (a read), the write of the notes waits for a person
        var advice = body(runGraph("site-notes", cid, request));
        assertEquals(APPROVAL, advice.getElement(TYPE), String.valueOf(advice.getMap()));
        assertEquals(2, advice.getElement(TURN), "the write is asked for on the second model turn");
        var waiting = calls(advice);
        assertEquals(1, waiting.size(), String.valueOf(waiting));
        var write = waiting.getFirst();
        assertEquals("file_write", write.get("name"));
        assertEquals("file.write", write.get("route"));
        assertEquals(true, write.get("wait"));
        var arguments = new MultiLevelMap(asMap(write.get("input")));
        assertEquals("notes/configuration-keys.md", arguments.getElement("path"));
        assertTrue(String.valueOf(arguments.getElement("content")).contains("Runtime overrides"), "the notes hold the crawled pages");
        var answered = asList(advice.getElement("answered"));
        assertEquals(List.of("web_crawl"), answered.stream().map(c -> c.get("name")).toList());
        assertEquals("ok", answered.getFirst().get("status"));
        assertTrue(MockAgentTools.WRITES.isEmpty(), "nothing is written before the person decides");
        assertEquals(Map.of("x-graph-id", "site-notes", "x-node", "research"), MockLlmChat.lastGraphContext());
        // run 2: the person approves - the loop runs the write it showed, and the model answers with the path
        var answer = body(runGraph("site-notes", cid, Map.of(TURN, 2, "decisions",
                List.of(Map.of("id", write.get("id"), "approved", true, "note", "Notes are welcome")))));
        assertEquals(ANSWER, answer.getElement(TYPE), String.valueOf(answer.getMap()));
        assertEquals("resume", answer.getElement("run"));
        var text = String.valueOf(answer.getElement(MESSAGE));
        assertTrue(text.contains("notes/configuration-keys.md") && text.contains("2 pages"), text);
        assertEquals(List.of("file_write"), calls(answer).stream().map(c -> c.get("name")).toList());
        assertEquals("ok", calls(answer).getFirst().get("status"), String.valueOf(calls(answer)));
        assertTrue(MockAgentTools.WRITES.get("notes/configuration-keys.md").startsWith("# Notes on Configuration keys"));
        assertEquals(List.of("web.crawl", "file.write"), MockAgentTools.CALLS);
    }

    @Test
    void siteNotesRefusedWriteGoesBackToTheModel() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var advice = body(runGraph("site-notes", cid, Map.of("section", MockAgentTools.SECTION, "topic", "Overrides")));
        assertEquals(APPROVAL, advice.getElement(TYPE), String.valueOf(advice.getMap()));
        var callId = String.valueOf(calls(advice).getFirst().get("id"));
        var answer = body(runGraph("site-notes", cid, Map.of(TURN, advice.getElement(TURN), "decisions",
                List.of(Map.of("id", callId, "approved", false, "note", "Not in the shared workspace")))));
        assertEquals(ANSWER, answer.getElement(TYPE), String.valueOf(answer.getMap()));
        var text = String.valueOf(answer.getElement(MESSAGE));
        assertTrue(text.contains("could not write") && text.contains("shared workspace"), text);
        assertEquals("refused", calls(answer).getLast().get("status"), String.valueOf(calls(answer)));
        assertTrue(MockAgentTools.WRITES.isEmpty(), "a refused write never ran");
    }

    @Test
    void siteNotesSectionOutsideTheAllowlistIsAnErrorTheModelReads() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var answer = body(runGraph("site-notes", cid, Map.of("section", "https://elsewhere.example/docs/", "topic", "Anything")));
        assertEquals(ANSWER, answer.getElement(TYPE), String.valueOf(answer.getMap()));
        var text = String.valueOf(answer.getElement(MESSAGE));
        assertTrue(text.contains("could not read the section") && text.contains("not one of the sites"), text);
        assertEquals("error", calls(answer).getFirst().get("status"), String.valueOf(calls(answer)));
    }

    @Test
    void documentIngestRunsTheWriteUnaskedAndLoadsTheGraph() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var answer = body(runGraph("document-ingest", cid, Map.of("document", "supplier-agreement.md")));
        assertEquals(ANSWER, answer.getElement(TYPE), String.valueOf(answer.getMap()));
        assertEquals(3, answer.getElement(TURN), "extract, ingest, answer");
        var text = String.valueOf(answer.getElement(MESSAGE));
        assertTrue(text.contains("Ingested 12 nodes and 11 relationships") && text.contains("2 parties, 4 obligations, 3 rules and 2 deadlines"), text);
        assertEquals(List.of("file_extract", "graph_ingest"), calls(answer).stream().map(c -> c.get("name")).toList());
        calls(answer).forEach(c -> assertEquals("ok", c.get("status"), String.valueOf(c)));
        assertEquals(List.of("file.extract", "graph.ingest"), MockAgentTools.CALLS, "the write ran without an approval - mode auto");
        var document = MockAgentTools.NODES.get("document:supplier-agreement.md");
        assertNotNull(document, String.valueOf(MockAgentTools.NODES.keySet()));
        assertEquals("Document", document.get("label"));
        var obligation = new MultiLevelMap(MockAgentTools.NODES.get("obligation:supplier-agreement-obl-2"));
        assertEquals("Obligation", obligation.getElement("label"));
        assertTrue(String.valueOf(obligation.getElement("properties.text")).contains("30 days"), String.valueOf(obligation.getMap()));
        assertEquals("supplier-agreement.md", obligation.getElement("properties.source"), "every node carries the document as its source");
        assertTrue(MockAgentTools.NODES.containsKey("party:northwind-office-supplies"));
        assertEquals(11, MockAgentTools.RELATIONSHIPS.size());
        assertEquals(Map.of("x-graph-id", "document-ingest", "x-node", "ingest"), MockLlmChat.lastGraphContext());
        // the same document again: the ingest is idempotent, the graph holds the same nodes
        var again = body(runGraph("document-ingest", Utility.getInstance().getUuid(), Map.of("document", "supplier-agreement.md")));
        assertEquals(ANSWER, again.getElement(TYPE));
        assertEquals(12, MockAgentTools.NODES.size());
    }

    @Test
    void documentIngestOfAMissingDocumentIsAnErrorTheModelReads() throws TimeoutException {
        var answer = body(runGraph("document-ingest", Utility.getInstance().getUuid(), Map.of("document", "missing.md")));
        assertEquals(ANSWER, answer.getElement(TYPE), String.valueOf(answer.getMap()));
        var text = String.valueOf(answer.getElement(MESSAGE));
        assertTrue(text.contains("could not read missing.md") && text.contains("file not found"), text);
        assertTrue(MockAgentTools.NODES.isEmpty());
        var rejected = runGraph("document-ingest", Utility.getInstance().getUuid(), Map.of("path", "x.md"));
        assertEquals(400, rejected.getStatus());
    }

    @Test
    void documentQueryReadsTheSchemaThenQueriesThenCitesTheSources() throws TimeoutException {
        for (String document : List.of("travel-policy.md", "data-retention-policy.md")) {
            var loaded = body(runGraph("document-ingest", Utility.getInstance().getUuid(), Map.of("document", document)));
            assertEquals(ANSWER, loaded.getElement(TYPE), String.valueOf(loaded.getMap()));
        }
        assertEquals(4, MockAgentTools.NODES.values().stream().filter(n -> "Deadline".equals(n.get("label"))).count(),
                String.valueOf(MockAgentTools.NODES.keySet()));
        MockAgentTools.CALLS.clear();
        var answer = body(runGraph("document-query", Utility.getInstance().getUuid(),
                Map.of("question", "Which deadlines do the policies set?")));
        assertEquals(ANSWER, answer.getElement(TYPE), String.valueOf(answer.getMap()));
        assertEquals(3, answer.getElement(TURN), "schema, query, answer");
        var text = String.valueOf(answer.getElement(MESSAGE));
        assertTrue(text.contains("From the knowledge graph:") && text.contains("retention audit") && text.contains("reimbursement caps"), text);
        assertEquals(4, text.split("\n- ").length - 1, "the four deadlines of the two policies: " + text);
        assertTrue(text.contains("(source: travel-policy.md)") && text.contains("(source: data-retention-policy.md)"), text);
        assertEquals(List.of("graph_schema", "graph_query"), calls(answer).stream().map(c -> c.get("name")).toList());
        assertEquals(List.of("graph.schema", "graph.query"), MockAgentTools.CALLS, "reads only - no file is read");
        var empty = body(runGraph("document-query", Utility.getInstance().getUuid(), Map.of("question", "Who are the parties?")));
        assertTrue(String.valueOf(empty.getElement(MESSAGE)).contains("Contoso Facilities"), String.valueOf(empty.getElement(MESSAGE)));
        var rejected = runGraph("document-query", Utility.getInstance().getUuid(), Map.of("prompt", "x"));
        assertEquals(400, rejected.getStatus());
    }

    @Test
    void documentQueryOnAnEmptyGraphSaysSo() throws TimeoutException {
        var answer = body(runGraph("document-query", Utility.getInstance().getUuid(), Map.of("question", "Which rules apply?")));
        assertEquals(ANSWER, answer.getElement(TYPE), String.valueOf(answer.getMap()));
        assertTrue(String.valueOf(answer.getElement(MESSAGE)).contains("holds nothing yet"), String.valueOf(answer.getElement(MESSAGE)));
        assertEquals(List.of("graph.schema"), MockAgentTools.CALLS);
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

    private EventEnvelope runGraph(String graph, String cid, Map<String, Object> body) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/" + graph)
                .setBody(body)
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .setHeader("X-Correlation-Id", cid);
        var event = new EventEnvelope().setTo("async.http.request").setBody(request);
        var po = PostOffice.trackable("unit.test",
                String.format("%032x", Math.abs(cid.hashCode())), "TEST /graph/" + graph);
        var response = po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        return response;
    }
}
