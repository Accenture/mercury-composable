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

package com.accenture.minigraph.websocket.server;

import com.accenture.minigraph.services.GraphCommandService;
import org.platformlambda.core.annotations.WebSocketService;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleXmlParser;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.websocket.server.WsEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@WebSocketService("graph")
public class GraphUserInterface implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(GraphUserInterface.class);
    private static final String GRAPH_COMMAND_SERVICE = GraphCommandService.ROUTE;
    private static final SimpleXmlParser xml = new SimpleXmlParser();
    private static final ConcurrentMap<String, Object> textMap = new ConcurrentHashMap<>();
    private static final String RESPONSE = "response";
    
    /**
     * This function will be rendered as a web-socket listener
     * <p>
     * If you want to close the websocket connection for whatever reason, you may issue:
     * Utility.getInstance().closeConnection(txPath, reasonCode, message);
     *
     * @param headers contains routing information of a websocket connection
     * @param body can be string or byte array as per websocket specification
     * @param instance is always 1 because the service is a singleton for each websocket connection
     * @return null because there is nothing to return
     */
    @Override
    public Object handleEvent(Map<String, String> headers, Object body, int instance) {
        // EventEmitter can be used instead of PostOffice when tracing is not required
        EventEmitter po = EventEmitter.getInstance();
        String route;
        String token;
        String txPath;
        if (headers.containsKey(WsEnvelope.TYPE)) {
            switch (headers.get(WsEnvelope.TYPE)) {
                case WsEnvelope.OPEN:
                    // the open event contains route, txPath, ip, path, query and token
                    route = headers.get(WsEnvelope.ROUTE);
                    txPath = headers.get(WsEnvelope.TX_PATH);
                    token = headers.get(WsEnvelope.TOKEN);
                    String ip = headers.get(WsEnvelope.IP);
                    String path = headers.get(WsEnvelope.PATH);
                    String query = headers.get(WsEnvelope.QUERY);
                    po.send(new EventEnvelope().setTo(GRAPH_COMMAND_SERVICE)
                                .setBody(Map.of("in", route, "type", "open")));
                    log.info("Started {}, {}, ip={}, path={}, query={}, token={}", route, txPath, ip, path, query, token);
                    break;
                case WsEnvelope.CLOSE:
                    // the close event contains route and token for this websocket
                    route = headers.get(WsEnvelope.ROUTE);
                    token = headers.get(WsEnvelope.TOKEN);
                    log.info("Stopped {}, token={}", route, token);
                    textMap.remove(route);
                    po.send(new EventEnvelope().setTo(GRAPH_COMMAND_SERVICE)
                            .setBody(Map.of("in", route, "type", "close")));
                    break;
                case WsEnvelope.BYTES:
                    // the data event for byteArray payload contains route and txPath
                    route = headers.get(WsEnvelope.ROUTE);
                    txPath = headers.get(WsEnvelope.TX_PATH);
                    byte[] payload = (byte[]) body;
                    // just tell the browser that I have received the bytes
                    po.send(txPath, "received " + payload.length + " bytes");
                    log.info("{} got {} bytes", route, payload.length);
                    break;
                case WsEnvelope.STRING:
                    // the data event for string payload contains route and txPath
                    route = headers.get(WsEnvelope.ROUTE);
                    txPath = headers.get(WsEnvelope.TX_PATH);
                    String message = String.valueOf(body).trim();
                    po.send(new EventEnvelope().setTo(GRAPH_COMMAND_SERVICE)
                            .setBody(Map.of("type", "command", "in", route,
                                    "message", message, "out", txPath)));
                    break;
                default:
                    // this should not happen
                    log.error("Invalid event {} {}", headers, body);
                    break;
            }
        }
        // nothing to return because this is asynchronous
        return null;
    }

    private void handleTextMessage(String route, String txPath, String message) {
        EventEmitter po = EventEmitter.getInstance();
        switch (message) {
            case "help" -> {
                var helpMessage = """
                                    load: load XML/JSON string as JSON object,
                                    unload: clear stored JSON object,
                                    {command}: any simple data retrieval or JSON-Path command,
                                    help: this command
                                    """;
                po.send(txPath, helpMessage);
            }
            case "load" -> textMap.put(route, "?");
            case "unload" -> {
                textMap.remove(route);
                po.send(txPath, "JSON unloaded\n");
            }
            default -> {
                if (message.equals("{\"type\":\"welcome\"}")) {
                    po.send(txPath, "Ready. Enter 'help' for more instruction.\n");
                } else if (message.startsWith("{\"type\":\"ping\"")) {
                    po.send(txPath, message);
                } else {
                    var text = textMap.get(route);
                    if (text instanceof String) {
                        String error = renderXmlOrJson(route, txPath, message);
                        if (error != null) {
                            textMap.remove(route);
                            po.send(txPath, "Invalid JSON/XML - " + error);
                        }
                    } else if (text instanceof MultiLevelMap mm) {
                        executeCommand(mm, txPath, message);
                    } else {
                        po.send(txPath, "JSON/XML not loaded - you entered: "+message+"\n");
                    }
                }
            }
        }
    }

    private void executeCommand(MultiLevelMap mm, String txPath, String message) {
        var mapper = SimpleMapper.getInstance().getMapper();
        var po = EventEmitter.getInstance();
        try {
            var result = mm.getElement(message);
            po.send(txPath, mapper.writeValueAsString(result) + "\n");
        } catch (Exception ex) {
            po.send(txPath, ex.getMessage());
        }
        po.send(txPath, (message.startsWith("$") ? "JSON-Path " : "Simple retrieval ") + "command: " + message);
    }

    private String renderXmlOrJson(String route, String txPath, String message) {
        var mapper = SimpleMapper.getInstance().getMapper();
        var po = EventEmitter.getInstance();
        Map<String, Object> map = new HashMap<>();
        if (message.startsWith("<") && message.endsWith(">")) {
            try {
                map.put(RESPONSE, xml.parse(message));
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        if (message.startsWith("{") && message.endsWith("}")) {
            try {
                map.put(RESPONSE, mapper.readValue(message, Map.class));
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        if (message.startsWith("[") && message.endsWith("]")) {
            try {
                map.put(RESPONSE, mapper.readValue(message, List.class));
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        if (map.containsKey(RESPONSE)) {
            try {
                var mm = new MultiLevelMap(map);
                textMap.put(route, mm);
                po.send(txPath, mapper.writeValueAsString(mm.getMap()) + "\n");
                po.send(txPath, "JSON rendered in a 'data' node. Enter simple retrieval or JSON-Path commands.");
                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        return "Payload cannot be parsed as XML or JSON";
    }
}