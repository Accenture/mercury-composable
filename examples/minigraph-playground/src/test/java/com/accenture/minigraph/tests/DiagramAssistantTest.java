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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The diagram-assistant demo graph: an {@code llm.run} node whose one catalog row reaches an MCP server's tool
 * through {@code diagram.render}, the application's own function that calls the MCP-bound route
 * {@code diagram.render.mcp} and stores the rendered image out of band. In production the MCP binding of a
 * language pack serves that route by calling a Mermaid MCP server; here {@link MockDiagramRender} serves it and
 * {@link MockLlmChat} scripts the model, so the graph, the loop, the storing function and the route contract are
 * tested without a token or a network.
 */
class DiagramAssistantTest {
    private static final long TIMEOUT = 10000;
    private static final String GRAPH = "diagram-assistant";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static String target;

    @BeforeAll
    static void beforeAll() {
        MainApp.main(new String[0]);
        var config = AppConfigReader.getInstance();
        target = "http://127.0.0.1:" + config.getProperty("rest.server.port");
    }

    @Test
    void aDescriptionIsRequired() throws TimeoutException {
        var response = runGraph(Map.of("topic", "nothing"));
        assertEquals(400, response.getStatus());
        assertEquals("rejected", body(response).getElement("type"));
    }

    @Test
    void theModelValidatesTheDiagramThroughTheMcpBoundRouteBeforeAnswering() throws TimeoutException {
        var response = runGraph(Map.of("question", "Draw a flowchart of a refund approval: request, review, approve or reject."));
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        var body = body(response);
        assertEquals("answer", body.getElement("type"));
        var text = String.valueOf(body.getElement("message"));
        assertTrue(text.contains("```mermaid") && text.contains("flowchart TD"), text);
        assertEquals(2, body.getElement("turns"), "the draft and the validated answer");
        var calls = calls(body);
        assertEquals(1, calls.size(), String.valueOf(calls));
        assertEquals("render_diagram", calls.getFirst().get("name"));
        assertEquals("diagram.render", calls.getFirst().get("route"), "the catalog maps the tool name to the bound route");
        assertEquals("ok", calls.getFirst().get("status"));
        assertEquals("mock-llm", body.getElement("model"));
        // the tool stored the image out of band: the model reports the reference, the file holds the SVG
        var reference = reference(text);
        var file = new File(AppConfigReader.getInstance().getProperty("diagram.store.folder"), reference + ".svg");
        assertTrue(file.isFile(), "the rendered image is stored as " + file);
        assertTrue(Utility.getInstance().file2str(file).contains("<svg"), "the file holds the SVG");
    }

    @Test
    void theRenderFunctionStoresAValidDiagramAndNothingForAnInvalidOne() throws Exception {
        var po = PostOffice.trackable("unit.test", Utility.getInstance().getUuid(), "TEST diagram.render");
        var valid = po.asyncRequest(new EventEnvelope().setTo("diagram.render")
                .setBody(Map.of("diagramCode", "flowchart LR\n  A --> B", "title", "two steps")), TIMEOUT)
                .await(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(200, valid.getStatus(), String.valueOf(valid.getBody()));
        var stored = body(valid);
        assertEquals(true, stored.getElement("valid"));
        assertEquals("two steps", stored.getElement("title"));
        assertNull(stored.getElement("rawSVG"), "the image never rides the reply");
        var file = new File(String.valueOf(stored.getElement("file")));
        assertTrue(file.isFile() && file.getName().equals(stored.getElement("reference") + ".svg"), String.valueOf(stored.getMap()));
        assertTrue(((Number) stored.getElement("bytes")).longValue() > 0);
        var page = new File(String.valueOf(stored.getElement("page")));
        assertTrue(page.isFile() && page.getName().equals(stored.getElement("reference") + ".html"), String.valueOf(stored.getMap()));
        var html = Utility.getInstance().file2str(page);
        assertTrue(html.contains("<svg") && html.contains("two steps") && html.contains("flowchart LR"),
                "the page inlines the SVG and shows the title and the code");
        var invalid = po.asyncRequest(new EventEnvelope().setTo("diagram.render")
                .setBody(Map.of("diagramCode", "this is not a diagram")), TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(200, invalid.getStatus(), String.valueOf(invalid.getBody()));
        var refused = body(invalid);
        assertEquals(false, refused.getElement("valid"));
        assertNotNull(refused.getElement("error"), "the server's error reaches the model");
        assertNull(refused.getElement("reference"), "nothing is stored for an invalid diagram");
        var missing = po.asyncRequest(new EventEnvelope().setTo("diagram.render").setBody(Map.of("title", "no code")), TIMEOUT)
                .await(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(400, missing.getStatus());
    }

    private static String reference(String text) {
        var matcher = Pattern.compile("reference ([0-9a-f]{32})").matcher(text);
        assertTrue(matcher.find(), "the answer names the stored image's reference: " + text);
        return matcher.group(1);
    }

    @Test
    void aWatchedRunShowsTheMcpToolCall() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(target + "/api/stream/graph/" + GRAPH))
                .timeout(Duration.ofMillis(TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("X-Correlation-Id", Utility.getInstance().getUuid())
                .POST(HttpRequest.BodyPublishers.ofString("{\"question\": \"A sequence of a login\"}")).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        assertEquals(200, response.statusCode());
        var events = response.body().filter(l -> l.startsWith("data:") && l.contains("\"type\""))
                .map(l -> json(l.substring(5).trim())).toList();
        var toolStarts = events.stream().filter(e -> "tool.start".equals(e.get("event"))).toList();
        assertEquals(1, toolStarts.size(), String.valueOf(events));
        var data = asMap(toolStarts.getFirst().get("data"));
        assertEquals("render_diagram", data.get("name"));
        assertEquals("diagram.render", data.get("route"));
        assertEquals("Drawing the diagram", events.getFirst().get("message"));
        assertEquals("result", events.getLast().get("type"));
    }

    @SuppressWarnings("unchecked")
    private static MultiLevelMap body(EventEnvelope response) {
        assertInstanceOf(Map.class, response.getBody(), String.valueOf(response.getBody()));
        return new MultiLevelMap((Map<String, Object>) response.getBody());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> calls(MultiLevelMap body) {
        Object calls = body.getElement("calls");
        assertInstanceOf(List.class, calls, String.valueOf(body.getMap()));
        return (List<Map<String, Object>>) calls;
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

    private EventEnvelope runGraph(Map<String, Object> body) throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/" + GRAPH).setBody(body)
                .setHeader("Content-Type", "application/json").setHeader("Accept", "application/json")
                .setHeader("X-Correlation-Id", cid);
        var event = new EventEnvelope().setTo("async.http.request").setBody(request);
        var po = PostOffice.trackable("unit.test", String.format("%032x", Math.abs(cid.hashCode())), "TEST /graph/" + GRAPH);
        return po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
    }
}
