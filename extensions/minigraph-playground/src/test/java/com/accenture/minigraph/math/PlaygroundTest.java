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

import static org.junit.jupiter.api.Assertions.*;

class PlaygroundTest {
    private static final Logger log = LoggerFactory.getLogger(PlaygroundTest.class);
    private static final Utility util = Utility.getInstance();
    private static final String UPLOAD_ADVICE = "Please upload XML/JSON text to";

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
    }

    // TODO: fix unit test
//    @Test
    void jsonPathWebSocketTest() throws InterruptedException {
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(10);
        final String welcome = """
                { "type": "welcome" }
                """;
        final String sample = """
                { 
                    "hello": "world",
                    "list": [1,2,3]
                 }
                """;
        final String ping = """
                { "type": "ping" }
                """;
        final AppConfigReader config = AppConfigReader.getInstance();
        final int port = util.str2int(config.getProperty("rest.server.port",
                config.getProperty("server.port", "8085")));
        var received = new ArrayList<String>();
        var po = EventEmitter.getInstance();
        LambdaFunction connector = (headers, input, instance) -> {
            String txPath = headers.get("tx_path");
            if ("open".equals(headers.get("type"))) {
                po.send(txPath, welcome);
                po.send(txPath, ping);
                po.send(txPath, util.getUTF("OK"));
                po.send(txPath, "<hello>test</hello>");
                po.send(txPath, "help");
                po.send(txPath, "unload");
                po.send(new EventEnvelope().setTo(txPath).setBody("{invalid-json}").setReplyTo("json.parsing.error"));
                po.send(txPath, "load");
                po.send(txPath, sample);
            }
            if ("string".equals(headers.get("type")) && input instanceof String message) {
                if (message.startsWith("JSON rendered.")) {
                    po.send(txPath, "$.response.hello");
                }
                var text = message.trim();
                if (text.equals("\"world\"")) {
                    received.add("world");
                    po.send(txPath, "response.list");
                }
                if (text.startsWith("[") && text.endsWith("]")) {
                    received.add(text);
                    po.send(txPath, "upload");
                }
                if (text.startsWith(UPLOAD_ADVICE)) {
                    var uri = text.substring(UPLOAD_ADVICE.length()).trim();
                    var request = new AsyncHttpRequest();
                    request.setUrl(uri).setTargetHost("http://127.0.0.1:" + port).setMethod("POST")
                            .setHeader("Content-Type", "application/json").setBody(sample);
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
        assertEquals(2,  received.size());
        assertEquals("world", received.getFirst());
        // the underlying GSON serializer is configured to treat number as Long
        assertEquals(List.of(1L, 2L, 3L), SimpleMapper.getInstance().getMapper().readValue(received.get(1), List.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void miniGraphWebSocketTest() throws InterruptedException {
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(10);
        final String welcome = """
                { "type": "welcome" }
                """;
        final String ping = """
                { "type": "ping" }
                """;
        final String createMapperNode = """
                create node mapper
                   with type mapper
                   with properties
                   skill=graph.data.mapper
                   mapping[]=text(123) -> root.test
                   mapping[]=input.body.id -> end.message
                """;
        final AppConfigReader config = AppConfigReader.getInstance();
        final int port = util.str2int(config.getProperty("rest.server.port",
                config.getProperty("server.port", "8085")));
        var received = new ArrayList<Map<String, Object>>();
        var po = EventEmitter.getInstance();
        LambdaFunction connector = (headers, input, instance) -> {
            String txPath = headers.get("tx_path");
            if ("open".equals(headers.get("type"))) {
                po.send(txPath, welcome);
                po.send(txPath, ping);
                po.send(txPath, util.getUTF("OK"));
                po.send(txPath, "help");
            }
            if ("string".equals(headers.get("type")) && input instanceof String text) {
                var message = text.trim();
                if (message.startsWith("Welcome to MiniGraph Playground")) {
                    po.send(txPath, "help connect");
                    po.send(txPath, "help create");
                    po.send(txPath, "help delete");
                    po.send(txPath, "help describe");
                    po.send(txPath, "help execute");
                    po.send(txPath, "help export");
                    po.send(txPath, "help import");
                    po.send(txPath, "help inspect");
                    po.send(txPath, "help instantiate");
                    po.send(txPath, "help run");
                    po.send(txPath, "help update");
                    po.send(txPath, "describe skill graph.math");
                    po.send(txPath, "create node root");
                }
                if (message.startsWith("node root created")) {
                    po.send(txPath, "create node end");
                }
                if (message.startsWith("node end created")) {
                    po.send(txPath, createMapperNode);
                }
                if (message.startsWith("node mapper created")) {
                    po.send(txPath, "connect root to mapper with first");
                }
                if (message.startsWith("node root connected to mapper")) {
                    po.send(txPath, "connect mapper to end with second");
                }
                if (message.startsWith("node mapper connected to end")) {
                    po.send(txPath, "instantiate graph\ntext(100) -> input.body.id");
                }
                if (message.startsWith("Graph instance created.")) {
                    po.send(txPath, "run");
                }
                if (message.startsWith("Knowledge graph executed")) {
                    po.send(txPath, "inspect root");
                }
                if (message.startsWith("{") && message.endsWith("}")) {
                    var map = SimpleMapper.getInstance().getMapper().readValue(message, Map.class);
                    if ("root".equals(map.get("inspect"))) {
                        po.send(txPath, "inspect end");
                        received.add(map);
                    }
                    if ("end".equals(map.get("inspect"))) {
                        received.add(map);
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
        assertEquals(2,  received.size());
        assertEquals(Map.of("test", "123"), received.getFirst().get("outcome"));
        assertEquals(Map.of("message", "100"), received.get(1).get("outcome"));
    }
}
