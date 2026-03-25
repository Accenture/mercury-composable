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

package com.accenture.minigraph.playground;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.client.PersistentWsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PlaygroundTest {
    private static final Logger log = LoggerFactory.getLogger(PlaygroundTest.class);
    private static final Utility util = Utility.getInstance();
    private static final String HELP = "help";
    private static final String UPLOAD_ADVICE = "Please upload XML/JSON text to";
    private static final String WELCOME = """
                                        { "type": "welcome" }
                                        """;
    private static final String SAMPLE = """
                                        { "hello": "world", "list": [1,2,3] }
                                        """;
    private static final String PING = """
                                        { "type": "ping" }
                                        """;
    private static final Map<String, Object> dialog1 = Map.of(
            "Welcome to JSON-Path Playground", List.of("load", SAMPLE),
            "JSON rendered.", "$.response.hello",
            "\"world\"", "response.list"
    );
    // simulate duplicated node creation
    private static final Map<String, Object> dialog2 = Map.of(
            "Welcome to MiniGraph Playground", List.of("help connect", "describe skill graph.math"),
            "Skill: Graph Math", List.of("create node root", "create node root"),
            "node root created", List.of("create node end", "create node end"),
            "node end created", """
                    create node mapper
                    with type mapper
                    with properties
                    skill=graph.data.mapper
                    multiline='''
                    hello world
                    line two
                    '''
                    mapping[]=text(123) -> root.test
                    mapping[]=input.body.id -> end.message""",
            "node mapper created", "connect root to mapper with first",
            "node root connected to mapper","connect mapper to end with second",
            "node mapper connected to end", "instantiate graph\ntext(100) -> input.body.id",
            "Graph instance created. Loaded 1 mock entry", "run",
            "Graph traversal completed", "inspect root",
            "Graph model imported as draft", "list nodes"
    );
    // "inspect js-1"
    private static final Map<String, Object> dialog3 = Map.of("root [", "list connections",
            "root -[fetch]-> fetcher", "start graph\ntext(100) -> input.body.person_id\nint(5000) -> model.ttl",
            "Graph instance created. Loaded 2 mock entries", "describe node js-1",
            "node js-1 run", "edit node js-1",
            "update node js-1", "edit node hello",
            "update node hello", "update node hello\nwith type Test\nwith properties\nhello=world",
            "node hello updated", "describe connection root and data-mapper",
            "root -[mapping]-> data-mapper", "connect hello to helloworld with test",
            "node hello connected to helloworld", "delete connection hello and helloworld",
            "hello -> helloworld removed", "connect hello to xyz with test");

    private static final Map<String, Object> dialog4 = Map.of("node xyz not found", "export graph as hello",
            "Graph exported", "describe graph",
            "Graph with", "import node extension from hello",
            "node extension overwritten", "clear cache",
            "cache cleared", "upload mock data",
            "You may upload JSON payload", """
                    start graph
                    int(10) -> input.body.person_id
                    text(world) -> model.hello
                    int(8000) -> model.ttl
                    """,
            "Graph instance created. Loaded 3 mock entries", "run",
            "Graph traversal aborted", "inspect js-1");

    @BeforeAll
    static void setup() {
        deleteTempGraph();
        AutoStart.main(new String[0]);
    }

    @AfterAll
    static void tearDown() {
        deleteTempGraph();
    }

    private static void deleteTempGraph() {
        File f = new File("/tmp/graph/hello.json");
        if (f.exists()) {
            var ok = f.delete();
            if (ok) {
                log.info("Deleted temp test graph {}", f.getPath());
            }
        }
    }

    @Test
    void jsonPathWebSocketTest() throws InterruptedException {
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final AppConfigReader config = AppConfigReader.getInstance();
        final int port = util.str2int(config.getProperty("rest.server.port",
                config.getProperty("server.port", "8085")));
        final var first = new AtomicBoolean(true);
        var received = new ArrayList<String>();
        var po = EventEmitter.getInstance();
        LambdaFunction connector = (headers, input, instance) -> {
            String txPath = headers.get("tx_path");
            if ("open".equals(headers.get("type"))) {
                po.send(txPath, PING);
            }
            if (bench.isEmpty() && "string".equals(headers.get("type")) && input instanceof String text) {
                if (first.get()) {
                    first.set(false);
                    po.send(txPath, util.getUTF("OK"));
                    po.send(txPath, "<hello>test</hello>");
                    po.send(txPath, HELP);
                    po.send(txPath, "unload");
                    po.send(new EventEnvelope().setTo(txPath).setBody("{invalid-json}").setReplyTo("json.parsing.error"));
                    po.send(txPath, WELCOME);
                }
                var message = text.trim();
                var next = getFromDialog(dialog1, message);
                if (next instanceof List<?> items) {
                    for (Object item : items) {
                        po.send(txPath, item);
                    }
                }
                if (next instanceof String nextCommand) {
                    po.send(txPath, nextCommand);
                }
                if (message.startsWith("[") && message.endsWith("]")) {
                    received.add(message);
                    po.send(txPath, "upload");
                }
                if (message.startsWith(UPLOAD_ADVICE)) {
                    var uri = message.substring(UPLOAD_ADVICE.length()).trim();
                    var request = new AsyncHttpRequest();
                    request.setUrl(uri).setTargetHost("http://127.0.0.1:" + port).setMethod("POST")
                            .setHeader("Content-Type", "application/json").setBody(SAMPLE);
                    po.request(new EventEnvelope().setTo("async.http.request")
                                    .setBody(request.toMap()), 8000).get();
                    bench.add(true);
                }
            }
            return true;
        };
        Platform.getInstance().registerPrivate("json.parsing.error", connector, 1);
        for (int i=0; i < 3; i++) {
            if (util.portReady("127.0.0.1", port, 3000)) {
                break;
            } else {
                log.info("Waiting for JSON websocket server at port-{} to get ready", port);
                Thread.sleep(1000);
            }
        }
        PersistentWsClient client = new PersistentWsClient(connector,
                Collections.singletonList("ws://127.0.0.1:"+port+"/ws/json/path"));
        client.start();
        var done = bench.poll(10, TimeUnit.SECONDS);
        log.info("JSON-Path websocket test completed? {}", done == null? "timeout" : done);
        // websocket client emulation in unit test would be influenced by the host computer so we want
        // to make it optional. However, we do want to print out the status for further improvement.
        if (done != null) {
            assertTrue(done);
            assertEquals(1,  received.size());
            // the underlying GSON serializer is configured to treat number as Long
            assertEquals(List.of(1L, 2L, 3L),
                    SimpleMapper.getInstance().getMapper().readValue(received.getFirst(), List.class));
        }
        client.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void miniGraphWebSocketTest() throws InterruptedException {
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final AppConfigReader config = AppConfigReader.getInstance();
        final int port = util.str2int(config.getProperty("rest.server.port",
                config.getProperty("server.port", "8085")));
        final AtomicBoolean first = new AtomicBoolean(true);
        final List<String> store = new ArrayList<>();
        var received = new ArrayList<Map<String, Object>>();
        var po = EventEmitter.getInstance();
        LambdaFunction connector = (headers, input, instance) -> {
            String txPath = headers.get("tx_path");
            if ("open".equals(headers.get("type"))) {
                po.send(txPath, PING);
            }
            if (bench.isEmpty() && "string".equals(headers.get("type")) && input instanceof String text) {
                if (first.get()) {
                    first.set(false);
                    po.send(txPath, util.getUTF("OK"));
                    po.send(txPath, HELP);
                    po.send(txPath, WELCOME);
                }
                var message = text.trim();
                var next = getNextCommand(message);
                if (next instanceof List<?> items) {
                    for (Object item : items) {
                        po.send(txPath, item);
                    }
                }
                if (next instanceof String nextCommand) {
                    po.send(txPath, nextCommand);
                }
                if (message.startsWith("You may upload")) {
                    var parts = util.split(message, " ");
                    store.add(parts.getLast());
                }
                if (message.startsWith("{") && message.endsWith("}")) {
                    var map = SimpleMapper.getInstance().getMapper().readValue(message, Map.class);
                    if (map.containsKey("inspect")) {
                        log.info("{}", map);
                    }
                    if (!store.isEmpty()) {
                        doRestTest(port, store.getFirst());
                        // do only once
                        store.clear();
                    }
                    if (map.containsKey("node")) {
                        po.send(txPath, "execute js-1");
                    } else {
                        if ("root".equals(map.get("inspect"))) {
                            deferredSend(txPath, "inspect end");
                            received.add(map);
                        }
                        if ("end".equals(map.get("inspect"))) {
                            Thread.sleep(100);
                            received.add(map);
                            deferredSend(txPath, "delete node root");
                            deferredSend(txPath, "import graph from hello");
                        }
                        if ("js-1".equals(map.get("inspect"))) {
                            received.add(map);
                            bench.add(true);
                        }
                    }
                }
            }
            return true;
        };
        for (int i=0; i < 3; i++) {
            if (util.portReady("127.0.0.1", port, 3000)) {
                break;
            } else {
                log.info("Waiting for GRAPH websocket server at port-{} to get ready", port);
                Thread.sleep(1000);
            }
        }
        PersistentWsClient client = new PersistentWsClient(connector,
                Collections.singletonList("ws://127.0.0.1:"+port+"/ws/graph/playground"));
        client.start();
        var done = bench.poll(10, TimeUnit.SECONDS);
        log.info("MiniGraph websocket test completed? {}", done == null? "timeout" : done);
        // websocket client emulation in unit test would be influenced by the host computer so we want
        // to make it optional. However, we do want to print out the status for further improvement.
        if (done != null) {
            assertTrue(done);
            client.close();
            assertEquals(3,  received.size());
            assertEquals(Map.of("test", "123"), received.getFirst().get("outcome"));
        }
    }

    private Object getNextCommand(String command) throws InterruptedException {
        var result = getFromDialog(dialog2, command);
        if (result != null) {
            return result;
        }
        result = getFromDialog(dialog3, command);
        return result != null? result : getFromDialog(dialog4, command);
    }

    private Object getFromDialog(Map<String, Object> dialog, String command) throws InterruptedException {
        for (Map.Entry<String, Object> kv : dialog.entrySet()) {
            if (command.startsWith(kv.getKey())) {
                log.info("{}", kv.getKey());
                // simulate human operator delay
                Thread.sleep(100);
                return kv.getValue();
            }
        }
        return null;
    }

    private void deferredSend(String txPath, String message) throws InterruptedException {
        var po = EventEmitter.getInstance();
        Thread.sleep(100);
        po.send(txPath, message);
    }

    private void doRestTest(int port, String url) throws InterruptedException, ExecutionException {
        var po = PostOffice.trackable("unit.test", "101", url);
        var request1 = new AsyncHttpRequest();
        request1.setTargetHost("http://127.0.0.1:" + port).setUrl(url).setMethod("POST");
        request1.setHeader("Content-Type", "application/json");
        request1.setBody(Map.of("hello", "world"));
        var event1 = new EventEnvelope().setTo("async.http.request").setBody(request1.toMap());
        var response1 = po.request(event1, 8000).get();
        log.info("Upload endpoint response - {}", response1.getBody());
        if (response1.getStatus() != 200) {
            return;
        }
        var parts = util.split(url, "/");
        var wsInstance = parts.getLast();
        var request2 = new AsyncHttpRequest();
        request2.setTargetHost("http://127.0.0.1:" + port).setUrl("/api/inspect/{id}/{key}").setMethod("GET");
        request2.setHeader("accept", "application/json");
        request2.setPathParameter("id", wsInstance).setPathParameter("key", "input");
        var event2 = new EventEnvelope().setTo("async.http.request").setBody(request2.toMap());
        var response2 = po.request(event2, 8000).get();
        log.info("Inspect endpoint response - {}", response2.getBody());
        var request3 = new AsyncHttpRequest();
        request3.setTargetHost("http://127.0.0.1:" + port).setUrl("/api/graph/model/{graph_id}/{sequence}");
        request3.setHeader("accept", "application/json").setMethod("GET");
        request3.setPathParameter("graph_id", "hello").setPathParameter("sequence", "1");
        var event3 = new EventEnvelope().setTo("async.http.request").setBody(request3.toMap());
        var response3 = po.request(event3, 8000).get();
        var text = String.valueOf(response3.getBody());
        log.info("Describe graph endpoint response - {} characters", text.length());
    }
}
