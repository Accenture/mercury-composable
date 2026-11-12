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

import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.EventStreamWriter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The way back to the caller of one graph run (RFC-0011 section 8.4): progress events while the run goes on, then
 * one terminal event, {@code result} or {@code error}. The entry decides the transport, and the channel frames each
 * event for it:
 * <ul>
 *   <li>a REST request with {@code stream: true} - one server-sent event per event on the request's reply lane, the
 *       stream opened by the first event, so a run that sends none answers with its single-shot response;</li>
 *   <li>a WebSocket session - one text frame per event on the session's outbound route, the session closed after
 *       the terminal event;</li>
 *   <li>any other entry - one event to the route its {@code x-return-path} names, when the application declares it
 *       in {@code return.path.routes}.</li>
 * </ul>
 * A node that carries {@code progress} gets a relay route of this channel, private to the application, and the
 * engine passes that route to the node's function (or to a synchronous subgraph) as {@code x-return-path}. A relay
 * names the sender's graph and node when the sender does not, so a function such as {@code llm.run} only sends to
 * the route it is given. The engine never waits on a send, and a failing return route never fails the run.
 */
public final class ReturnChannel {
    private static final Logger log = LoggerFactory.getLogger(ReturnChannel.class);
    public static final String X_RETURN_PATH = "x-return-path";
    public static final String PROGRESS = "progress";
    private static final String RESULT = "result";
    private static final String ERROR = "error";
    private static final String TYPE = "type";
    private static final String EVENT = "event";
    private static final String SEQ = "seq";
    private static final String TIME = "time";
    private static final String CID = "cid";
    private static final String GRAPH = "graph";
    private static final String NODE = "node";
    private static final String MESSAGE = "message";
    private static final String STATUS = "status";
    private static final String BODY = "body";
    private static final String NODE_START = "node.start";
    private static final String RELAY_PREFIX = "graph.progress.";
    static final String DRAIN = "x-drain";
    private static final long DRAIN_MS = 3000;
    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
    private static final Set<String> LIVE_RELAYS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentMap<String, String> SOCKETS = new ConcurrentHashMap<>();

    private enum Kind { STREAM, EVENT, SOCKET }

    private final Kind kind;
    private final String target;
    private final String instanceId;
    private final String graphId;
    private final EventStreamWriter writer;
    private final AtomicInteger sequence = new AtomicInteger();
    private final Map<String, String> relays = new HashMap<>();
    private final AtomicReference<String> businessCid = new AtomicReference<>();
    private boolean streaming;
    private boolean closed;
    private boolean unavailable;

    private ReturnChannel(Kind kind, String target, String requestId, String instanceId, String graphId) {
        this.kind = kind;
        this.target = target;
        this.instanceId = instanceId;
        this.graphId = graphId;
        this.writer = kind == Kind.STREAM? new EventStreamWriter(target, requestId) : null;
    }

    /**
     * A REST request's reply lane ({@code stream: true}).
     *
     * @param lane the request's reply lane
     * @param requestId the request's correlation id, which the lane matches its response context by
     * @param instanceId the run's flow instance id
     * @param graphId the graph
     * @return the channel
     */
    public static ReturnChannel stream(String lane, String requestId, String instanceId, String graphId) {
        return new ReturnChannel(Kind.STREAM, lane, requestId, instanceId, graphId);
    }

    /**
     * A route the caller named and the application declares, or the relay route of a parent run.
     *
     * @param route the return route
     * @param instanceId the run's flow instance id
     * @param graphId the graph
     * @return the channel
     */
    public static ReturnChannel event(String route, String instanceId, String graphId) {
        return new ReturnChannel(Kind.EVENT, route, null, instanceId, graphId);
    }

    /**
     * A WebSocket session's outbound route.
     *
     * @param txPath the session's outbound route
     * @param instanceId the run's flow instance id
     * @param graphId the graph
     * @return the channel
     */
    public static ReturnChannel socket(String txPath, String instanceId, String graphId) {
        return new ReturnChannel(Kind.SOCKET, txPath, null, instanceId, graphId);
    }

    /**
     * The WebSocket entry names its session for the run it starts, before the run exists.
     *
     * @param flowCorrelationId the correlation id the run's flow is started with
     * @param txPath the session's outbound route
     */
    public static void watchFromSocket(String flowCorrelationId, String txPath) {
        SOCKETS.put(flowCorrelationId, txPath);
    }

    /**
     * Take the session the WebSocket entry named for a run.
     *
     * @param flowCorrelationId the correlation id the run's flow was started with
     * @return the session's outbound route, or null for a run that no session watches
     */
    public static String takeSocket(String flowCorrelationId) {
        return flowCorrelationId == null? null : SOCKETS.remove(flowCorrelationId);
    }

    /**
     * Whether a route is the relay of a run that is still going: a synchronous subgraph returns its progress through
     * the parent's relay, which no application declares.
     *
     * @param route a return route
     * @return true for a live relay route
     */
    public static boolean isLiveRelay(String route) {
        return route != null && LIVE_RELAYS.contains(route);
    }

    public void setCid(String cid) {
        businessCid.set(cid);
    }

    /**
     * The engine's event for a node that carries a {@code progress} property.
     *
     * @param node the node's alias
     * @param message the property's text
     */
    public void nodeStart(String node, String message) {
        var event = engineEvent(PROGRESS);
        event.put(EVENT, NODE_START);
        event.put(NODE, node);
        event.put(MESSAGE, message);
        deliver(event);
    }

    /**
     * A sender's progress event, through the relay of the node that called it. Only a progress event passes: a
     * synchronous subgraph's own terminal event is not the end of this run.
     *
     * @param node the node the relay serves
     * @param event the sender's event
     */
    void relay(String node, Map<?, ?> event) {
        if (!PROGRESS.equals(event.get(TYPE))) {
            return;
        }
        var copy = new LinkedHashMap<String, Object>();
        event.forEach((k, v) -> copy.put(String.valueOf(k), v));
        copy.putIfAbsent(GRAPH, graphId);
        copy.putIfAbsent(NODE, node);
        if (businessCid.get() != null) {
            copy.putIfAbsent(CID, businessCid.get());
        }
        deliver(copy);
    }

    private synchronized void deliver(Map<String, Object> event) {
        if (closed) {
            return;
        }
        if (kind == Kind.STREAM) {
            if (!streaming) {
                writer.first(200, TEXT_EVENT_STREAM);
                streaming = true;
            }
            writer.write((String) event.get(TYPE), event);
        } else {
            send(event);
        }
    }

    /**
     * The run's terminal event, from the response the run returns: {@code error} for a status of 400 or more,
     * otherwise {@code result} (a suspension's advice included). A stream the run never opened sends nothing, so the
     * single-shot response answers the request.
     *
     * @param response the run's response
     */
    public void finish(EventEnvelope response) {
        drainRelays();
        finishAfterDrain(response);
    }

    private synchronized void finishAfterDrain(EventEnvelope response) {
        if (closed) {
            return;
        }
        closed = true;
        var status = response.getStatus();
        var terminal = engineEvent(status >= 400? ERROR : RESULT);
        terminal.put(STATUS, status);
        if (status >= 400) {
            terminal.put(MESSAGE, messageOf(response.getBody()));
        } else {
            terminal.put(BODY, response.getBody());
        }
        switch (kind) {
            case STREAM -> endStream(terminal);
            case EVENT -> send(terminal);
            case SOCKET -> {
                send(terminal);
                Utility.getInstance().closeConnection(target, 1000, "done");
            }
        }
    }

    /**
     * A stream the run opened ends with the terminal event. A stream it never opened sends nothing, so the single-shot
     * response answers the request. (Java 21 allows a guard only on a pattern label, never on the enum constant of
     * this switch, so the condition lives here rather than in the case.)
     *
     * @param terminal the run's terminal event
     */
    private void endStream(Map<String, Object> terminal) {
        if (streaming) {
            writer.write((String) terminal.get(TYPE), terminal);
            writer.close();
        }
    }

    /**
     * A sender's last events may still wait in its relay when the run ends. Each relay is one instance, so its answer
     * to a drain request means every event queued before the request has been delivered: the terminal event follows
     * them. Called without the channel's lock, which the relay needs to deliver.
     */
    private void drainRelays() {
        List<String> routes;
        synchronized (this) {
            if (closed || relays.isEmpty()) {
                return;
            }
            routes = List.copyOf(relays.values());
        }
        var emitter = EventEmitter.getInstance();
        var pending = new ArrayList<CompletableFuture<EventEnvelope>>();
        for (var route : routes) {
            try {
                var request = new EventEnvelope().setTo(route).setHeader(DRAIN, "true");
                pending.add(emitter.eRequest(request, DRAIN_MS, false));
            } catch (RuntimeException e) {
                log.debug("Relay {} not drained - {}", route, e.getMessage());
            }
        }
        for (var future : pending) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                log.debug("Relay not drained - {}", e.getMessage());
            }
        }
    }

    /**
     * A run that ended without a terminal event (a flow timeout, for example) still ends its channel, with the error
     * its flow reported.
     *
     * @param advice the end-of-flow advice, carrying the flow's error when it was aborted
     */
    public void abandon(Object advice) {
        if (advice instanceof Map<?, ?> error && error.get(STATUS) instanceof Integer status && status >= 400) {
            finish(new EventEnvelope().setStatus(status).setBody(String.valueOf(error.get(MESSAGE))));
        } else {
            finish(new EventEnvelope().setStatus(500).setBody("The run ended without a result"));
        }
    }

    /**
     * The relay route a node's function sends its progress events to, registered when the node first needs it.
     *
     * @param node the node's alias
     * @return the relay route, or null when the run has ended
     */
    public synchronized String relayFor(String node) {
        if (closed) {
            return null;
        }
        var existing = relays.get(node);
        if (existing != null) {
            return existing;
        }
        var route = RELAY_PREFIX + instanceId + "." + (relays.size() + 1);
        try {
            Platform.getInstance().registerPrivate(route, new ProgressRelay(this, node), 1);
        } catch (RuntimeException e) {
            log.warn("Unable to register progress relay {} for node {} - {}", route, node, e.getMessage());
            return null;
        }
        LIVE_RELAYS.add(route);
        relays.put(node, route);
        return route;
    }

    /**
     * Release the run's relay routes. A sender's event after this is not delivered.
     */
    public void close() {
        Map<String, String> routes;
        synchronized (this) {
            closed = true;
            routes = new HashMap<>(relays);
            relays.clear();
        }
        var platform = Platform.getInstance();
        for (var route : routes.values()) {
            LIVE_RELAYS.remove(route);
            platform.release(route);
        }
    }

    private Map<String, Object> engineEvent(String type) {
        var event = new LinkedHashMap<String, Object>();
        event.put(TYPE, type);
        event.put(SEQ, sequence.incrementAndGet());
        event.put(TIME, TIME_FORMAT.format(Instant.now()));
        if (businessCid.get() != null) {
            event.put(CID, businessCid.get());
        }
        event.put(GRAPH, graphId);
        return event;
    }

    private void send(Map<String, Object> event) {
        try {
            EventEmitter.getInstance().send(new EventEnvelope().setTo(target).setBody(event));
        } catch (RuntimeException e) {
            if (!unavailable) {
                unavailable = true;
                log.warn("Return route {} of graph {} is not available - {}", target, graphId, e.getMessage());
            }
        }
    }

    private static String messageOf(Object body) {
        if (body instanceof Map<?, ?> map && map.get(MESSAGE) != null) {
            return String.valueOf(map.get(MESSAGE));
        }
        return String.valueOf(body);
    }
}
