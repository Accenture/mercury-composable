/*

    Copyright 2018-2025 Accenture Technology

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

package com.accenture.ws;

import org.platformlambda.core.annotations.WebSocketService;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.LocalPubSub;
import org.platformlambda.core.websocket.server.WsEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unchecked")
@WebSocketService("test")
public class QueryHandler implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(QueryHandler.class);

    private static final ConcurrentMap<String, String> connections = new ConcurrentHashMap<>();

    private static final String BENCHMARK_USERS = "benchmark.users";
    private static final String BENCHMARK_SERVICE = "benchmark.service";
    private static final String TYPE = "type";
    private static final String QUERY = "query";
    private static final String CLEAR = "CLEAR";
    private static final String SENDER = "sender";
    private static final String COMMAND = "command";

    @Override
    public Object handleEvent(Map<String, String> headers, Object body, int instance) {
        // use local pub/sub so all websocket client gets the same response for monitoring the benchmark progress
        LocalPubSub ps = LocalPubSub.getInstance();
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
                    log.info("Started {}, {}, ip={}, path={}, query={}, token={}", route, txPath, ip, path, query, token);
                    connections.put(route, txPath);
                    ps.subscribe(BENCHMARK_USERS, txPath);
                    break;
                case WsEnvelope.CLOSE:
                    // the close event contains route and token for this websocket
                    route = headers.get(WsEnvelope.ROUTE);
                    token = headers.get(WsEnvelope.TOKEN);
                    log.info("Stopped {}, token={}", route, token);
                    txPath = connections.get(route);
                    if (txPath != null) {
                        connections.remove(route);
                        ps.unsubscribe(BENCHMARK_USERS, txPath);
                    }
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
                    txPath = headers.get(WsEnvelope.TX_PATH);
                    String message = (String) body;
                    if (message.startsWith("{") && message.endsWith("}")) {
                        Map<String, String> data = SimpleMapper.getInstance().getMapper().readValue(message, Map.class);
                        if (QUERY.equals(data.get(TYPE)) && data.containsKey(QUERY)) {
                            String command = data.get(QUERY);
                            if (CLEAR.equalsIgnoreCase(command)) {
                                po.send(txPath, CLEAR);
                            } else {
                                // interpret command
                                po.send(BENCHMARK_SERVICE, new Kv(SENDER, txPath), new Kv(COMMAND, command));
                            }
                        }
                    }
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

}
