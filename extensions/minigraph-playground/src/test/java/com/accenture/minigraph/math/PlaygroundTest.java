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

package com.accenture.minigraph.math;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.client.PersistentWsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
                    mapping[]=text(123) -> root.test
                    mapping[]=input.body.id -> end.message""",
            "node mapper created", "connect root to mapper with first",
            "node root connected to mapper","connect mapper to end with second",
            "node mapper connected to end", "instantiate graph\ntext(100) -> input.body.id",
            "Graph instance created.", "run",
            "Knowledge graph executed", "inspect root"
    );

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
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
                var next = getNextCommand(dialog1, message);
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
                log.info("Waiting for websocket server at port-{} to get ready", port);
                Thread.sleep(1000);
            }
        }
        PersistentWsClient client = new PersistentWsClient(connector,
                Collections.singletonList("ws://127.0.0.1:"+port+"/ws/json/path"));
        client.start();
        var done = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(done);
        assertTrue(done);
        client.close();
        assertEquals(1,  received.size());
        // the underlying GSON serializer is configured to treat number as Long
        assertEquals(List.of(1L, 2L, 3L), SimpleMapper.getInstance().getMapper().readValue(received.getFirst(), List.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void miniGraphWebSocketTest() throws InterruptedException {
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final AppConfigReader config = AppConfigReader.getInstance();
        final int port = util.str2int(config.getProperty("rest.server.port",
                config.getProperty("server.port", "8085")));
        final var first = new AtomicBoolean(true);
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
                var next = getNextCommand(dialog2, message);
                if (next instanceof List<?> items) {
                    for (Object item : items) {
                        po.send(txPath, item);
                    }
                }
                if (next instanceof String nextCommand) {
                    po.send(txPath, nextCommand);
                }
                if (message.startsWith("{") && message.endsWith("}")) {
                    var map = SimpleMapper.getInstance().getMapper().readValue(message, Map.class);
                    if ("root".equals(map.get("inspect"))) {
                        po.send(txPath, "inspect end");
                        received.add(map);
                    }
                    if ("end".equals(map.get("inspect"))) {
                        received.add(map);
                        po.send(txPath, "delete node root");
                        bench.add(true);
                    }
                }
            }
            return true;
        };
        for (int i=0; i < 3; i++) {
            if (util.portReady("127.0.0.1", port, 3000)) {
                break;
            } else {
                log.info("Waiting for websocket server at port-{} to get ready", port);
                Thread.sleep(1000);
            }
        }
        PersistentWsClient client = new PersistentWsClient(connector,
                Collections.singletonList("ws://127.0.0.1:"+port+"/ws/graph/playground"));
        client.start();
        var done = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(done);
        assertTrue(done);
        client.close();
        assertEquals(2,  received.size());
        assertEquals(Map.of("test", "123"), received.getFirst().get("outcome"));
        assertEquals(Map.of("message", "100"), received.get(1).get("outcome"));
    }

    private Object getNextCommand(Map<String, Object> dialog, String command) throws InterruptedException {
        for (Map.Entry<String, Object> kv : dialog.entrySet()) {
            if (command.startsWith(kv.getKey())) {
                log.info("{}", kv.getKey());
                // simulate human operator delay
                Thread.sleep(50);
                return kv.getValue();
            }
        }
        return null;
    }
}
