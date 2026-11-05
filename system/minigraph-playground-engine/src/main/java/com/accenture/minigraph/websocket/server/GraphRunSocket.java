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

import com.accenture.adapters.FlowExecutor;
import com.accenture.minigraph.progress.ReturnChannel;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.WebSocketService;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.server.WsEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A watched graph run over a WebSocket session (RFC-0011 section 8.2): {@code /ws/graph-run/{graph_id}}. The session's
 * first text frame is the run's input, a JSON object with an optional {@code body} and {@code header} (an
 * {@code X-Correlation-Id} header names the business correlation id, as on a REST request). The run sends its
 * progress events, then its result, as text frames to the session, and the session closes after the result. One
 * session runs one graph; a later frame is ignored.
 * <p>
 * Off by default ({@code graph.websocket.enabled=true} turns it on): a WebSocket path is outside the REST automation,
 * so its authentication does not apply here. An application that turns this entry on puts it behind a gateway that
 * authenticates the upgrade request. {@code graph.websocket.ttl} (default 30m) is each run's TTL.
 */
@OptionalService(GraphRunSocket.ENABLED)
@WebSocketService("graph-run")
public class GraphRunSocket implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(GraphRunSocket.class);
    public static final String ENABLED = "graph.websocket.enabled";
    public static final String TTL_CONFIG = "graph.websocket.ttl";
    private static final String GRAPH_EXECUTOR = "graph-executor";
    private static final String BODY = "body";
    private static final String HEADER = "header";
    private static final String TTL = "ttl";
    private static final String PATH_PARAMETER = "path_parameter";
    private static final String GRAPH_ID = "graph_id";
    private static final String X_CORRELATION_ID = "x-correlation-id";
    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final ConcurrentMap<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private record Session(String txPath, String graphId, AtomicBoolean started) { }

    @Override
    public Object handleEvent(Map<String, String> headers, Object body, int instance) {
        var route = headers.get(WsEnvelope.ROUTE);
        var txPath = headers.get(WsEnvelope.TX_PATH);
        var eventType = headers.get(WsEnvelope.TYPE);
        switch (String.valueOf(eventType)) {
            case WsEnvelope.OPEN -> {
                var graphId = headers.get(WsEnvelope.TOKEN);
                SESSIONS.put(route, new Session(txPath, graphId, new AtomicBoolean()));
                log.info("Session {} opened for graph {}", route, graphId);
            }
            case WsEnvelope.STRING -> {
                var session = SESSIONS.get(route);
                if (session != null && session.started().compareAndSet(false, true)) {
                    start(session, String.valueOf(body));
                } else {
                    log.warn("Session {} ignores a frame - one session runs one graph", route);
                }
            }
            case WsEnvelope.BYTES -> refuse(SESSIONS.get(route), 400, "The run's input is a JSON text frame");
            case WsEnvelope.CLOSE -> SESSIONS.remove(route);
            default -> log.warn("Session {} ignores an event of type {}", route, eventType);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void start(Session session, String text) {
        Map<String, Object> frame;
        try {
            var parsed = SimpleMapper.getInstance().getMapper().readValue(text, Object.class);
            if (!(parsed instanceof Map)) {
                refuse(session, 400, "The run's input is a JSON object with an optional body and header");
                return;
            }
            frame = (Map<String, Object>) parsed;
        } catch (RuntimeException e) {
            refuse(session, 400, "The run's input is not JSON - " + e.getMessage());
            return;
        }
        var util = Utility.getInstance();
        var header = frame.get(HEADER) instanceof Map<?, ?> h? h : Map.of();
        var body = frame.get(BODY) instanceof Map<?, ?> b? b : Map.of();
        var businessCid = businessCid(header);
        var flowCid = util.getUuid();
        var dataset = new HashMap<String, Object>();
        dataset.put(BODY, body);
        dataset.put(HEADER, header);
        dataset.put(PATH_PARAMETER, Map.of(GRAPH_ID, session.graphId()));
        dataset.put(TTL, util.getDurationInSeconds(AppConfigReader.getInstance().getProperty(TTL_CONFIG, "30m"))
                * 1000L);
        // the run picks its channel up by the flow's correlation id; the flow's own reply goes to the reply route
        ReturnChannel.watchFromSocket(flowCid, session.txPath());
        var po = new PostOffice(GraphRunReply.ROUTE, util.getUuid(), "WS /ws/graph-run/" + session.graphId());
        try {
            FlowExecutor.getInstance().launch(po, GRAPH_EXECUTOR, dataset, GraphRunReply.ROUTE, flowCid, businessCid);
            log.info("Session started graph {}, cid {}", session.graphId(), businessCid);
        } catch (RuntimeException e) {
            ReturnChannel.takeSocket(flowCid);
            refuse(session, 500, "The run did not start - " + e.getMessage());
        }
    }

    private static String businessCid(Map<?, ?> header) {
        for (var kv : header.entrySet()) {
            if (X_CORRELATION_ID.equalsIgnoreCase(String.valueOf(kv.getKey())) && kv.getValue() != null
                    && !String.valueOf(kv.getValue()).isBlank()) {
                return String.valueOf(kv.getValue()).trim();
            }
        }
        return Utility.getInstance().getUuid();
    }

    /**
     * End a session whose run cannot start, with an error event in the shape of a run's terminal event.
     */
    private static void refuse(Session session, int status, String message) {
        if (session != null) {
            refuse(session.txPath(), status, message);
        }
    }

    static void refuse(String txPath, int status, String message) {
        EventEmitter.getInstance().send(txPath, Map.of(TYPE, ERROR, STATUS, status, MESSAGE, message));
        Utility.getInstance().closeConnection(txPath, 1000, "done");
    }
}
