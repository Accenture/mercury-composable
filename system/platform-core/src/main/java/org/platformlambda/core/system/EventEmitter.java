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

package org.platformlambda.core.system;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.core.models.*;
import org.platformlambda.core.services.TemporaryInbox;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.common.MultipartPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class EventEmitter {
    private static final Logger log = LoggerFactory.getLogger(EventEmitter.class);
    public static final int ONE_MILLISECOND = 1000000;
    public static final String CLOUD_CONNECTOR = "cloud.connector";
    public static final String CLOUD_SERVICES = "cloud.services";
    public static final String RPC = "rpc";
    private static final String MISSING_ROUTING_PATH = "Missing routing path";
    private static final String MISSING_EVENT = "Missing outgoing event";
    private static final long ASYNC_EVENT_HTTP_TIMEOUT = 60 * 1000L; // assume 60 seconds
    private static final String TASK_EXECUTOR = "task.executor";
    private static final String EVENT_MANAGER = "event.script.manager";
    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String X_EVENT_API = "x-event-api";
    private static final String X_NO_STREAM = "x-small-payload-as-bytes";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String HTTP_OR_HTTPS = "Protocol must be http or https";
    private static final String POST = "POST";
    private static final String CONTENT_TYPE = "content-type";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String ACCEPT = "accept";
    private static final String X_TTL = "x-ttl";
    private static final String X_ASYNC = "x-async";
    private static final String X_TRACE_ID = "x-trace-id";
    private static final String ROUTE_SUBSTITUTION = "route.substitution";
    private static final String ROUTE_SUBSTITUTION_YAML = "yaml.route.substitution";
    private static final String ROUTE_SUBSTITUTION_FEATURE = "application.feature.route.substitution";
    private static final String EVENT_OVER_HTTP_YAML = "yaml.event.over.http";
    private static final String MULTICAST_YAML = "yaml.multicast";
    private static final String JOURNAL_YAML = "yaml.journal";
    private static final String APP_GROUP_PREFIX = "monitor-";
    private static final ConcurrentMap<String, FutureEvent> futureEvents = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> reRoutes = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> eventHttpTargets = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Map<String, String>> eventHttpHeaders = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, TraceInfo> traces = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ConcurrentMap<String, String>> cloudRoutes = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> cloudOrigins = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Boolean> journaledRoutes = new ConcurrentHashMap<>();
    private boolean multicastEnabled = false;
    private boolean journalEnabled = false;
    private boolean eventHttpEnabled = false;
    private static final EventEmitter INSTANCE = new EventEmitter();

    private EventEmitter() {
        Platform platform = Platform.getInstance();
        AppConfigReader config = AppConfigReader.getInstance();
        String multicast = config.getProperty(MULTICAST_YAML);
        if (multicast != null) {
            platform.getVirtualThreadExecutor().submit(() -> {
                try {
                    ConfigReader reader = new ConfigReader(multicast);
                    loadMulticast(multicast, reader);
                    multicastEnabled = true;
                    log.info("Loaded multicast config from {}", multicast);
                } catch (IOException e) {
                    log.error("Unable to load multicast config - {}", e.getMessage());
                }
            });
        }
        String journal = config.getProperty(JOURNAL_YAML);
        if (journal != null) {
            platform.getVirtualThreadExecutor().submit(() -> {
                try {
                    ConfigReader reader = new ConfigReader(journal);
                    loadJournalRoutes(reader);
                    journalEnabled = true;
                    log.info("Loaded journal config from {}", journal);
                } catch (IOException e) {
                    log.error("Unable to load journal config - {}", e.getMessage());
                }
            });
        }
        String eventHttpConfig = config.getProperty(EVENT_OVER_HTTP_YAML);
        if (eventHttpConfig != null) {
            platform.getVirtualThreadExecutor().submit(() -> {
                try {
                    ConfigReader reader = new ConfigReader(eventHttpConfig);
                    loadHttpRoutes(eventHttpConfig, reader);
                    eventHttpEnabled = true;
                    log.info("Loaded event-over-http config from {}", eventHttpConfig);
                } catch (IOException e) {
                    log.error("Unable to load event-over-http config - {}", e.getMessage());
                }

            });
        }
        // load route substitution table if any
        if ("true".equals(config.getProperty(ROUTE_SUBSTITUTION_FEATURE, "false"))) {
            platform.getVirtualThreadExecutor().submit(this::loadRouteSubstitution);
        }
    }

    public static EventEmitter getInstance() {
        return INSTANCE;
    }

    public String getId() {
        return Platform.getInstance().getOrigin();
    }

    public boolean isMulticastEnabled() {
        return multicastEnabled;
    }

    public boolean isJournalEnabled() {
        return journalEnabled;
    }
    public boolean isEventHttpConfigEnabled() {
        return eventHttpEnabled;
    }

    public ConcurrentMap<String, ConcurrentMap<String, String>> getCloudRoutes() {
        return cloudRoutes;
    }

    public ConcurrentMap<String, Long> getCloudOrigins() {
        return cloudOrigins;
    }

    public boolean isJournaled(String route) {
        return journaledRoutes.getOrDefault(route, false);
    }

    public List<String> getJournaledRoutes() {
        return new ArrayList<>(journaledRoutes.keySet());
    }

    @SuppressWarnings("unchecked")
    private void loadJournalRoutes(ConfigReader reader) {
        Object o = reader.get("journal");
        if (o == null) {
            log.warn("Missing 'journal' section");
        } else if (o instanceof List) {
            List<Object> entries = (List<Object>) o;
            for (int i=0; i < entries.size(); i++) {
                String route = reader.getProperty("journal["+i+"]");
                journaledRoutes.put(route, true);
            }
            log.info("Total {} route{} will be recorded in journal", journaledRoutes.size(),
                    journaledRoutes.size() == 1? "" : "s");
        } else {
            log.warn("Invalid 'journal' section - it must be a list of route names");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadMulticast(String file, ConfigReader reader) {
        Object o = reader.get("multicast");
        if (o instanceof List) {
            List<Object> multicastList = (List<Object>) o;
            for (int i=0; i < multicastList.size(); i++) {
                String source = reader.getProperty("multicast["+i+"].source");
                List<String> targets = (List<String>) reader.get("multicast["+i+"].targets");
                if (source != null && targets != null && !source.isEmpty() && !targets.isEmpty()) {
                    List<String> targetList = new ArrayList<>(targets);
                    targetList.remove(source);
                    Set<String> members = new HashSet<>(targetList);
                    if (members.size() < targets.size()) {
                        log.warn("Multicast config error - {} -> {} becomes {} -> {}",
                                source, targets, source, members);
                    }
                    LocalPubSub ps = LocalPubSub.getInstance();
                    try {
                        ps.createTopic(source);
                        for (String member: members) {
                            ps.subscribe(source, member);
                        }
                    } catch (IOException e) {
                        log.error("Unable to register multicast {} -> {}, - {}", source, members, e.getMessage());
                    }
                }
            }
        } else {
            log.error("Invalid config {} - the multicast section should be a list of source and targets", file);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void loadHttpRoutes(String file, ConfigReader reader) {
        Utility util = Utility.getInstance();
        Object o = reader.get("event.http");
        if (o instanceof List) {
            List<Object> eventHttpEntries = (List<Object>) o;
            for (int i=0; i < eventHttpEntries.size(); i++) {
                String route = reader.getProperty("event.http["+i+"].route");
                String target = reader.getProperty("event.http["+i+"].target");
                if (route != null && target != null && !route.isEmpty() && !target.isEmpty()) {
                    if (util.validServiceName(route)) {
                        try {
                            new URI(target);
                            eventHttpTargets.put(route, target);
                            int headerCount = 0;
                            Object h = reader.get("event.http["+i+"].headers");
                            if (h instanceof Map m) {
                                Map<String, String> headers = new HashMap<>();
                                for (Object k: m.keySet()) {
                                    headers.put(k.toString(), reader.getProperty("event.http["+i+"].headers."+k));
                                    headerCount++;
                                }
                                eventHttpHeaders.put(route, headers);
                            }
                            log.info("Event-over-HTTP {} -> {} with {} header{}", route, target,
                                    headerCount, headerCount == 1? "" : "s");
                        } catch (URISyntaxException e) {
                            log.error("Invalid Event over HTTP config entry - check target {}", target);
                        }
                    } else {
                        log.error("Invalid Event over HTTP config entry - check route {}", route);
                    }
                }
            }
            log.info("Total {} event-over-http target{} configured", eventHttpTargets.size(),
                    eventHttpTargets.size() == 1? "" : "s");
        } else {
            log.error("Invalid config {} - the event.http section should be a list of route and target", file);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadRouteSubstitution() {
        AppConfigReader config = AppConfigReader.getInstance();
        String location = config.getProperty(ROUTE_SUBSTITUTION_YAML);
        if (location != null) {
            try {
                // try loading from given location(s)
                ConfigReader reader = getRouteSubstitutionConfig(location);
                Object o = reader.get(ROUTE_SUBSTITUTION);
                if (o instanceof List) {
                    List<Object> list = (List<Object>) o;
                    for (int i=0; i < list.size(); i++) {
                        String entry = reader.getProperty(ROUTE_SUBSTITUTION+"["+i+"]");
                        int sep = entry.indexOf("->");
                        if (sep == -1) {
                            log.error("Invalid route substitution entry {}", entry);
                        } else {
                            String route = entry.substring(0, sep).trim();
                            String replacement = entry.substring(sep+2).trim();
                            addRouteSubstitution(entry, route, replacement);
                        }
                    }
                } else {
                    throw new IOException("route.substitution should be a list of strings");
                }
            } catch (IOException e) {
                log.error("Unable to load route substitution entries - {}", e.getMessage());
            }
        } else {
            // load route substitution list from application.properties
            Utility util = Utility.getInstance();
            List<String> substitutionList = util.split(config.getProperty(ROUTE_SUBSTITUTION, ""), ", ");
            for (String entry : substitutionList) {
                int colon = entry.indexOf(':');
                String route = entry.substring(0, colon).trim();
                String replacement = entry.substring(colon + 1).trim();
                addRouteSubstitution(entry, route, replacement);
            }
        }
    }

    private void addRouteSubstitution(String entry, String original, String replacement) {
        try {
            addRouteSubstitution(original, replacement);
        } catch (IllegalArgumentException e) {
            log.error("Unable to add route substitution {} - {}", entry, e.getMessage());
        }
    }

    private void addRouteSubstitution(String original, String replacement) {
        Utility util = Utility.getInstance();
        if (util.validServiceName(original) && util.validServiceName(replacement)
                && original.contains(".") && replacement.contains(".")) {
            if (!original.equals(replacement) && !replacement.equals(reRoutes.get(original))) {
                if (reRoutes.containsKey(replacement)) {
                    throw new IllegalArgumentException("Nested route substitution not supported");
                } else {
                    reRoutes.put(original, replacement);
                    log.info("Route substitution: {} -> {}", original, replacement);
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid route names");
        }
    }

    private ConfigReader getRouteSubstitutionConfig(String location) throws IOException {
        List<String> paths = Utility.getInstance().split(location, ", ");
        for (String p: paths) {
            try {
                ConfigReader config = new ConfigReader(p);
                log.info("Loaded route substitutions from {}", p);
                return config;
            } catch (IOException e) {
                log.warn("Skipping {} - {}", p, e.getMessage());
            }
        }
        throw new IOException("Route substitutions not found in "+paths);
    }

    /**
     * IMPORTANT: This method is reserved by the system. User application MUST NOT access this.
     * @param route name
     * @param instance for the worker serving this transaction
     * @return trace info
     */
    public TraceInfo getTrace(String route, int instance) {
        if (route == null) {
            return null;
        }
        String ref = Thread.currentThread().threadId() + "/" + instance + "/" + route;
        return traces.get(ref);
    }

    /**
     * IMPORTANT: This method is reserved by the system. User application MUST NOT access this.
     * @param route name
     * @param traceId to identify a transaction
     * @param tracePath for the transaction
     * @param instance for the worker serving this transaction
     * @return ref for the thread or coroutine
     */
    public String startTracing(String route, String traceId, String tracePath, int instance) {
        String ref = Thread.currentThread().threadId() + "/" + instance + "/" + route;
        if (route != null && route.contains(".")) {
            traces.put(ref, new TraceInfo(route, traceId, tracePath));
        }
        return ref;
    }

    /**
     * IMPORTANT: This method is reserved by the system. User application MUST NOT access this.
     * @param ref for the thread or coroutine
     * @return current trace info before it is stopped
     */
    public TraceInfo stopTracing(String ref) {
        if (ref != null) {
            TraceInfo trace = traces.get(ref);
            if (trace != null) {
                traces.remove(ref);
                return trace;
            }
        }
        return null;
    }

    /**
     * Service discovery
     *
     * @param to destination
     * @return actor, websocket txPaths or null
     * @throws IOException in case route is not found
     */
    public TargetRoute discover(String to) throws IOException {
        Platform platform = Platform.getInstance();
        if (to.contains("@")) {
            int at = to.indexOf('@');
            String origin = to.substring(at+1);
            String target = to.substring(0, at);
            if (origin.equals(platform.getOrigin())) {
                if (platform.hasRoute(target)) {
                    return new TargetRoute(platform.getManager(target), false);
                }
            } else {
                TargetRoute cloud = getCloudRoute();
                if (cloud != null && (origin.startsWith(APP_GROUP_PREFIX) || cloudOrigins.containsKey(origin))) {
                    return cloud;
                }
            }
        } else {
            if (platform.hasRoute(to)) {
                return new TargetRoute(platform.getManager(to), false);
            } else {
                TargetRoute cloud = getCloudRoute();
                if (cloud != null && exists(to)) {
                    return cloud;
                }
            }
        }
        throw new IOException("Route "+to+" not found");
    }

    public TargetRoute getCloudRoute() {
        Platform platform = Platform.getInstance();
        if (platform.hasRoute(CLOUD_CONNECTOR)) {
            return new TargetRoute(platform.getManager(CLOUD_CONNECTOR), true);
        }
        return null;
    }

    /**
     * Broadcast an event to a target service that may exist in multiple servers
     *
     * @param to target route
     * @param parameters for the event
     * @throws IOException in case of invalid route
     */
    public void broadcast(String to, Kv... parameters) throws IOException {
        broadcast(asEnvelope(to, null, parameters));
    }

    /**
     * Broadcast an event to a target service that may exist in multiple servers
     *
     * @param to target route
     * @param body message payload
     * @throws IOException in case of invalid route
     */
    public void broadcast(String to, Object body) throws IOException {
        if (body instanceof Kv kv) {
            // in case if a single KV is sent
            Kv[] keyValue = new Kv[1];
            keyValue[0] = kv;
            broadcast(asEnvelope(to, null, keyValue));
        } else {
            broadcast(asEnvelope(to, body));
        }
    }

    /**
     * Broadcast an event to a target service that may exist in multiple servers
     *
     * @param to target route
     * @param body message payload
     * @param parameters for the event
     * @throws IOException in case of invalid route
     */
    public void broadcast(String to, Object body, Kv... parameters) throws IOException {
        broadcast(asEnvelope(to, body, parameters));
    }

    /**
     * Send an event to a target service in multiple servers
     *
     * @param to target route
     * @param parameters for the event
     * @throws IOException in case of invalid route
     */
    public void send(String to, Kv... parameters) throws IOException {
        send(asEnvelope(to, null, parameters));
    }

    /**
     * Send an event to a target service
     *
     * @param to target route
     * @param body message payload
     * @throws IOException in case of invalid route
     */
    public void send(String to, Object body) throws IOException {
        if (body instanceof Kv kv) {
            // in case if a single KV is sent
            Kv[] keyValue = new Kv[1];
            keyValue[0] = kv;
            send(asEnvelope(to, null, keyValue));
        } else {
            send(asEnvelope(to, body));
        }
    }

    /**
     * Send an event to a target service
     *
     * @param to target route
     * @param body message payload
     * @param parameters for the event
     * @throws IOException in case of invalid route
     */
    public void send(String to, Object body, Kv... parameters) throws IOException {
        send(asEnvelope(to, body, parameters));
    }

    public EventEnvelope asEnvelope(String to, Object body, Kv... parameters) {
        EventEnvelope event = new EventEnvelope().setTo(to).setBody(body);
        if (parameters != null) {
            for (Kv kv: parameters) {
                if (kv.key != null && kv.value != null) {
                    event.setHeader(kv.key, kv.value);
                }
            }
        }
        return event;
    }

    /**
     * Schedule a future event
     *
     * @param event envelope
     * @param future time
     * @return eventId of the scheduled delivery
     */
    public String sendLater(final EventEnvelope event, Date future) {
        String destination = event.getTo();
        if (destination == null) {
            throw new IllegalArgumentException(MISSING_ROUTING_PATH);
        }
        String to = substituteRouteIfAny(destination);
        event.setTo(to);
        long now = System.currentTimeMillis();
        long futureMs = future.getTime();
        long interval = Math.max(1, futureMs - now);
        log.debug("Future event to {} in {} ms", to, interval);
        // schedule the event delivery
        Vertx vertx = Platform.getInstance().getVertx();
        long taskId = vertx.setTimer(interval, id -> {
            futureEvents.remove(event.getId());
            try {
                EventEmitter.getInstance().send(event);
            } catch (IOException e) {
                log.error("Deferred delivery to {} failed - {}", event.getTo(), e.getMessage());
            }
        });
        futureEvents.put(event.getId(), new FutureEvent(event.getTo(), taskId, future));
        return event.getId();
    }

    /**
     * Cancel a future event
     *
     * @param id of the scheduled event
     */
    public void cancelFutureEvent(String id) {
        FutureEvent event = futureEvents.get(id);
        if (event != null) {
            Vertx vertx = Platform.getInstance().getVertx();
            vertx.cancelTimer(event.taskId);
            futureEvents.remove(id);
            log.debug("Cancel future event {}, {}", event.to, event.getTime());
        }
    }

    /**
     * Find all future events
     *
     * @return a list of targets that have scheduled events
     */
    public List<String> getAllFutureEvents() {
        List<String> result = new ArrayList<>();
        for (var entry: futureEvents.entrySet()) {
            String to = entry.getValue().to;
            if (!result.contains(to)) {
                result.add(to);
            }
        }
        return result;
    }

    /**
     * Get future events for a target
     *
     * @param to target
     * @return event ID list
     */
    public List<String> getFutureEvents(String to) {
        if (to == null || to.isEmpty()) {
            throw new IllegalArgumentException("Missing 'to'");
        }
        List<String> result = new ArrayList<>();
        for (var entry: futureEvents.entrySet()) {
            FutureEvent event = entry.getValue();
            if (event.to.equals(to)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Given an event ID, return the scheduled time
     *
     * @param id of the future event
     * @return scheduled time
     */
    public Date getFutureEventTime(String id) {
        FutureEvent event = futureEvents.get(id);
        return event != null? event.time : null;
    }

    /**
     * Broadcast to multiple target services
     *
     * @param event to the target
     * @throws IOException if invalid route or missing parameters
     */
    public void broadcast(EventEnvelope event) throws IOException {
        send(event.setBroadcastLevel(1));
    }

    public Map<String, String> getRouteSubstitutionList() {
        return reRoutes;
    }

    public String substituteRouteIfAny(String to) {
        if (to != null) {
            int slash = to.indexOf('@');
            if (slash > 0) {
                String replacement = reRoutes.get(to.substring(0, slash));
                return replacement != null? replacement+to.substring(slash) : to;
            } else {
                String replacement = reRoutes.get(to);
                return replacement != null? replacement : to;
            }
        } else {
            throw new IllegalArgumentException("Missing to");
        }
    }

    public String getEventHttpTarget(String route) {
        int slash = route.indexOf('@');
        return eventHttpTargets.get(slash == -1? route : route.substring(0, slash));
    }

    public Map<String, String> getEventHttpHeaders(String route) {
        int slash = route.indexOf('@');
        return eventHttpHeaders.get(slash == -1? route : route.substring(0, slash));
    }

    /**
     * Send an event to a target service
     *
     * @param input event to the target
     * @throws IOException if invalid route or missing parameters
     */
    public void send(final EventEnvelope input) throws IOException {
        var event = input.copy();
        String destination = event.getTo();
        if (destination == null) {
            throw new IllegalArgumentException(MISSING_ROUTING_PATH);
        }
        String to = substituteRouteIfAny(destination);
        event.setTo(to);
        var targetHttp = event.getHeader(X_EVENT_API) == null? getEventHttpTarget(to) : null;
        if (targetHttp != null) {
            String callback = event.getReplyTo();
            String eventApiType = callback == null? "async" : "callback";
            event.setReplyTo(null);
            EventEnvelope forwardEvent = new EventEnvelope(event.toMap()).setHeader(X_EVENT_API, eventApiType);
            Future<EventEnvelope> response = asyncRequest(forwardEvent, ASYNC_EVENT_HTTP_TIMEOUT,
                                                            getEventHttpHeaders(to), targetHttp, callback != null);
            response.onSuccess(evt -> {
                if (callback != null) {
                    // Send the RPC response from the remote target service to the callback
                    evt.setTo(callback).setReplyTo(null).setFrom(to)
                        .setTrace(event.getTraceId(), event.getTracePath())
                        .setCorrelationId(event.getCorrelationId());
                    try {
                        send(evt);
                    } catch (IOException e) {
                        log.error("Error in sending callback event {} from {} to {} - {}",
                                to, targetHttp, callback, e.getMessage());
                    }
                } else {
                    if (evt.getStatus() != 202) {
                        log.error("Error in sending async event {} to {} - status={}, error={}",
                                to, targetHttp, evt.getStatus(), evt.getError());
                    }
                }
            });
            return;
        }
        TargetRoute target = discover(to);
        if (target.isCloud()) {
            /*
             * If broadcast, set broadcast level to 3 because the event will be sent
             * to the target by the cloud connector directly
             */
            MultipartPayload.getInstance().outgoing(target.getManager(),
                    event.getBroadcastLevel() > 0? event.setBroadcastLevel(3) : event);
        } else {
            EventBus system = Platform.getInstance().getEventSystem();
            /*
             * The target is the same memory space. We will route it to the cloud connector if broadcast.
             */
            if (Platform.isCloudSelected() && event.getBroadcastLevel() == 1 && !CLOUD_CONNECTOR.equals(to)) {
                TargetRoute cloud = getCloudRoute();
                if (cloud != null) {
                    if (cloud.isCloud()) {
                        MultipartPayload.getInstance().outgoing(cloud.getManager(), event.setBroadcastLevel(3));
                    }
                } else {
                    system.send(target.getManager().getRoute(), event.setBroadcastLevel(3).toBytes());
                }
            } else {
                // also set broadcast-level to 3 to propagate broadcast in event-over-http use case
                EventEnvelope out = event.getBroadcastLevel() > 0? event.setBroadcastLevel(3) : event;
                String route = target.getManager().getRoute();
                /*
                 * The "event.script.manager" and "task.executor" are reserved function route names
                 * for Event Script. The system will run them directly using virtual threads for
                 * performance optimization.
                 *
                 * Since these two functions are event routers themselves, functional isolation,
                 * serialization and I/O immutability are guaranteed.
                 *
                 * Therefore, it is safe to eliminate additional processing overheads.
                 */
                if (TASK_EXECUTOR.equals(route) || EVENT_MANAGER.equals(route)) {
                    Platform.getInstance().getVirtualThreadExecutor().submit(() ->
                            runTaskExecutor(out, target.getManager().getService().getFunction()));
                } else {
                    system.send(route, out.toBytes());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void runTaskExecutor(EventEnvelope event, Object f) {
        Platform.getInstance().getVirtualThreadExecutor().submit(() -> {
            try {
                if (f instanceof TypedLambdaFunction<?, ?>) {
                    TypedLambdaFunction<EventEnvelope, Void> task = (TypedLambdaFunction<EventEnvelope, Void>) f;
                    task.handleEvent(event.getHeaders(), event, 1);
                } else {
                    throw new IllegalArgumentException("Not a TypedLambdaFunction");
                }
            } catch (Exception e) {
                log.error("Unable to execute event script - {}", e.getMessage());
            }
        });
    }

    /**
     * This method allows your app to send an async or RPC request to another application instance
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * <p>
     *     Note that onFailure is not required because exceptions are returned as regular event.
     * <p>
     * IMPORTANT: This is an asynchronous RPC using Future.
     * If you want sequential non-blocking RPC, please use the request API
     *
     * @param input event to be sent to a peer application instance
     * @param timeout to abort the request
     * @param headers optional security headers such as "Authorization"
     * @param eventEndpoint fully qualified URL such as http://domain:port/api/event
     * @param rpc if true, the target service will return a response.
     *            Otherwise, the response has a status of 202 to indicate that the event is delivered.
     * @return response event
     * @throws IOException in case of routing error
     */
    @SuppressWarnings("unchecked")
    public Future<EventEnvelope> asyncRequest(final EventEnvelope input, long timeout,
                                              Map<String, String> headers,
                                              String eventEndpoint, boolean rpc) throws IOException {
        var event = input.copy();
        String destination = event.getTo();
        if (destination == null) {
            throw new IllegalArgumentException(MISSING_ROUTING_PATH);
        }
        final URI url;
        try {
            url = new URI(eventEndpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod(POST);
        req.setHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
        req.setHeader(X_NO_STREAM, "true");
        req.setHeader(ACCEPT, "*/*");
        req.setHeader(X_TTL, String.valueOf(Math.max(100L, timeout)));
        if (!rpc) {
            req.setHeader(X_ASYNC, "true");
        }
        // optional HTTP request headers
        if (headers != null) {
            for (Map.Entry<String, String> kv : headers.entrySet()) {
                req.setHeader(kv.getKey(), kv.getValue());
            }
        }
        // propagate trace-ID if any
        if (event.getTraceId() != null) {
            req.setHeader(X_TRACE_ID, event.getTraceId());
        }
        req.setUrl(url.getPath());
        req.setTargetHost(getTargetFromUrl(url));
        byte[] b = event.toBytes();
        req.setBody(b);
        req.setContentLength(b.length);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        if (event.getFrom() != null) {
            request.setFrom(event.getFrom());
        }
        if (event.getTraceId() != null) {
            request.setTraceId(event.getTraceId());
        }
        if (event.getTracePath() != null) {
            request.setTracePath(event.getTracePath());
        }
        return Future.future(promise -> {
            try {
                // add 100 ms to make sure it does not time out earlier than the target service
                Future<EventEnvelope> res = asyncRequest(request, Math.max(100L, timeout)+100L);
                res.onSuccess(evt -> {
                    Object o = evt.getBody();
                    if (o instanceof byte[]) {
                        try {
                            EventEnvelope response = new EventEnvelope((byte[]) evt.getBody());
                            promise.complete(response);
                        } catch (IOException e) {
                            // response is not a packed EventEnvelope
                            promise.complete(new EventEnvelope()
                                    .setStatus(400).setBody("Did you configure rest.yaml correctly? " +
                                                            "Invalid result set - "+e.getMessage()));
                        }
                    } else {
                        if (evt.getStatus() >= 400 && evt.getBody() instanceof Map) {
                            Map<String, Object> data = (Map<String, Object>) evt.getBody();
                            if (ERROR.equals(data.get(TYPE)) && data.containsKey(MESSAGE) &&
                                    data.get(MESSAGE) instanceof String) {
                                EventEnvelope error = new EventEnvelope()
                                        .setStatus(evt.getStatus()).setBody(data.get(MESSAGE));
                                promise.complete(error);
                            } else {
                                promise.complete(evt);
                            }
                        } else {
                            promise.complete(evt);
                        }
                    }
                });
                res.onFailure(e -> promise.complete(new EventEnvelope().setStatus(408).setBody(e.getMessage())));

            } catch (IllegalArgumentException e) {
                Platform.getInstance().getVirtualThreadExecutor().submit(() ->
                        promise.complete(new EventEnvelope().setStatus(400).setBody(e.getMessage())));
            } catch (IOException e) {
                Platform.getInstance().getVirtualThreadExecutor().submit(() ->
                        promise.complete(new EventEnvelope().setStatus(500).setBody(e.getMessage())));
            }
        });
    }

    /**
     * This method allows your app to send an RPC request to another application instance
     * <p>
     * You can retrieve result using future.get()
     * <p>
     *     Note that timeout is returned as a regular event with status=408
     *
     * @param input event to be sent to a peer application instance
     * @param timeout to abort the request
     * @param headers optional security headers such as "Authorization"
     * @param eventEndpoint fully qualified URL such as http://domain:port/api/event
     * @param rpc if true, the target service will return a response.
     *            Otherwise, the response has a status of 202 to indicate that the event is delivered.
     * @return response event
     * @throws IOException in case of routing error
     */
    @SuppressWarnings("unchecked")
    public java.util.concurrent.Future<EventEnvelope> request(final EventEnvelope input, long timeout,
                                              Map<String, String> headers,
                                              String eventEndpoint, boolean rpc) throws IOException {
        var event = input.copy();
        String destination = event.getTo();
        if (destination == null) {
            throw new IllegalArgumentException(MISSING_ROUTING_PATH);
        }
        final URI url;
        try {
            url = new URI(eventEndpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod(POST);
        req.setHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
        req.setHeader(X_NO_STREAM, "true");
        req.setHeader(ACCEPT, "*/*");
        req.setHeader(X_TTL, String.valueOf(Math.max(100L, timeout)));
        if (!rpc) {
            req.setHeader(X_ASYNC, "true");
        }
        // optional HTTP request headers
        if (headers != null) {
            for (Map.Entry<String, String> kv : headers.entrySet()) {
                req.setHeader(kv.getKey(), kv.getValue());
            }
        }
        // propagate trace-ID if any
        if (event.getTraceId() != null) {
            req.setHeader(X_TRACE_ID, event.getTraceId());
        }
        req.setUrl(url.getPath());
        req.setTargetHost(getTargetFromUrl(url));
        byte[] b = event.toBytes();
        req.setBody(b);
        req.setContentLength(b.length);
        EventEnvelope apiRequest = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        if (event.getFrom() != null) {
            apiRequest.setFrom(event.getFrom());
        }
        if (event.getTraceId() != null) {
            apiRequest.setTraceId(event.getTraceId());
        }
        if (event.getTracePath() != null) {
            apiRequest.setTracePath(event.getTracePath());
        }
        CompletableFuture<EventEnvelope> future = new CompletableFuture<>();
        // add 100 ms to make sure it does not time out earlier than the target service
        java.util.concurrent.Future<EventEnvelope> eventApi = request(apiRequest,
                                                        Math.max(100L, timeout)+100L, false);
        try {
            EventEnvelope evt = eventApi.get();
            Object o = evt.getBody();
            if (o instanceof byte[]) {
                try {
                    EventEnvelope response = new EventEnvelope((byte[]) evt.getBody());
                    future.complete(response);
                } catch (IOException e) {
                    // response is not a packed EventEnvelope
                    future.complete(new EventEnvelope()
                            .setStatus(400).setBody("Did you configure rest.yaml correctly? " +
                                    "Invalid result set - "+e.getMessage()));
                }
            } else {
                if (evt.getStatus() >= 400 && evt.getBody() instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) evt.getBody();
                    if (ERROR.equals(data.get(TYPE)) && data.containsKey(MESSAGE) &&
                            data.get(MESSAGE) instanceof String) {
                        EventEnvelope error = new EventEnvelope()
                                .setStatus(evt.getStatus()).setBody(data.get(MESSAGE));
                        future.complete(error);
                    } else {
                        future.complete(evt);
                    }
                } else {
                    future.complete(evt);
                }
            }
        } catch (ExecutionException | InterruptedException e)  {
            // just send exception as a response event
            future.complete(new EventEnvelope().setStatus(500).setBody(e.getMessage()));
        }
        return future;
    }

    private String getTargetFromUrl(URI url) {
        final boolean secure;
        String protocol = url.getScheme();
        if (HTTP.equals(protocol)) {
            secure = false;
        } else if (HTTPS.equals(protocol)) {
            secure = true;
        } else {
            throw new IllegalArgumentException(HTTP_OR_HTTPS);
        }
        String host = url.getHost().trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Unable to resolve target host as domain or IP address");
        }
        int port = url.getPort();
        if (port < 0) {
            port = secure? 443 : 80;
        }
        return protocol+"://"+host+":"+port;
    }

    /**
     * Send a request asynchronously with a future result
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * onFailure(Throwable timeoutException)
     * <p>
     * IMPORTANT: This is an asynchronous RPC using Future.
     * If you want sequential non-blocking RPC, please use the request API
     *
     * @param event to the target
     * @param timeout in milliseconds
     * @return future results
     * @throws IOException in case of routing error
     */
    public Future<EventEnvelope> asyncRequest(final EventEnvelope event, long timeout) throws IOException {
        return asyncRequest(event, timeout, true);
    }

    /**
     * Send a request asynchronously with a future result
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * onFailure(Throwable timeoutException)
     * <p>
     * IMPORTANT: This is an asynchronous RPC using Future.
     * If you want sequential non-blocking RPC, please use the request API
     *
     * @param input event to the target
     * @param timeout in milliseconds
     * @param timeoutException if true, return TimeoutException in onFailure method. Otherwise, return timeout event.
     * @return future result
     * @throws IOException in case of routing error
     */
    public Future<EventEnvelope> asyncRequest(final EventEnvelope input, long timeout, boolean timeoutException)
            throws IOException {
        var event = input.copy();
        String destination = event.getTo();
        if (destination == null) {
            throw new IllegalArgumentException(MISSING_ROUTING_PATH);
        }
        String to = substituteRouteIfAny(destination);
        event.setTo(to);
        var targetHttp = event.getHeader(X_EVENT_API) == null? getEventHttpTarget(to) : null;
        if (targetHttp != null) {
            EventEnvelope forwardEvent = new EventEnvelope(event.toMap()).setHeader(X_EVENT_API, "asyncRequest");
            return asyncRequest(forwardEvent, timeout, getEventHttpHeaders(to), targetHttp, true);
        }
        Platform platform = Platform.getInstance();
        TargetRoute target = discover(to);
        AsyncInbox inbox = new AsyncInbox(to, event, timeout, timeoutException);
        event.setCorrelationId(inbox.getCorrelationId());
        event.setReplyTo(TemporaryInbox.TEMPORARY_INBOX + "@" + platform.getOrigin());
        event.addTag(RPC, timeout);
        event.setBroadcastLevel(0);
        if (target.isCloud()) {
            MultipartPayload.getInstance().outgoing(target.getManager(), event);
        } else {
            platform.getEventSystem().send(target.getManager().getRoute(), event.toBytes());
        }
        return inbox.getFuture();
    }

    /**
     * Future request API for RPC
     * <p>
     * You can retrieve result using future.get()
     *
     * @param event to the target
     * @param timeout in milliseconds
     * @return future results
     * @throws IOException in case of routing error
     */
    public java.util.concurrent.Future<EventEnvelope> request(final EventEnvelope event, long timeout)
            throws IOException {
        return request(event, timeout, true);
    }

    /**
     * Future request API for RPC
     * <p>
     * You can retrieve result using future.get()
     *
     * @param input event to the target
     * @param timeout in milliseconds
     * @param timeoutException if true, throws TimeoutException wrapped in an ExecutionException with future.get().
     *                         Otherwise, return timeout as a regular event.
     * @return future result
     * @throws IOException in case of routing error
     */
    public java.util.concurrent.Future<EventEnvelope> request(final EventEnvelope input, long timeout,
                                                              boolean timeoutException) throws IOException {
        var event = input.copy();
        String destination = event.getTo();
        if (destination == null) {
            throw new IllegalArgumentException(MISSING_ROUTING_PATH);
        }
        String to = substituteRouteIfAny(destination);
        event.setTo(to);
        var targetHttp = event.getHeader(X_EVENT_API) == null? getEventHttpTarget(to) : null;
        if (targetHttp != null) {
            EventEnvelope forwardEvent = new EventEnvelope(event.toMap()).setHeader(X_EVENT_API, "request");
            return request(forwardEvent, timeout, getEventHttpHeaders(to), targetHttp, true);
        }
        Platform platform = Platform.getInstance();
        TargetRoute target = discover(to);
        FutureInbox inbox = new FutureInbox(to, event, timeout, timeoutException);
        event.setCorrelationId(inbox.getCorrelationId());
        event.setReplyTo(TemporaryInbox.TEMPORARY_INBOX + "@" + platform.getOrigin());
        event.addTag(RPC, timeout);
        event.setBroadcastLevel(0);
        if (target.isCloud()) {
            MultipartPayload.getInstance().outgoing(target.getManager(), event);
        } else {
            platform.getEventSystem().send(target.getManager().getRoute(), event.toBytes());
        }
        return inbox.getFuture();
    }

    /**
     * Send parallel requests asynchronously with a future result (fork-n-join)
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * onFailure(Throwable timeoutException)
     * <p>
     * IMPORTANT: This is an asynchronous RPC using Future.
     * If you want sequential non-blocking RPC, please use the request API
     *
     * @param events list of envelopes
     * @param timeout in milliseconds
     * @return future list of results
     * @throws IOException in case of error
     */
    public Future<List<EventEnvelope>> asyncRequest(final List<EventEnvelope> events, long timeout) throws IOException {
        return asyncRequest(events, timeout, true);
    }

    /**
     * Send parallel requests asynchronously with a future result (fork-n-join)
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * onFailure(Throwable timeoutException)
     * <p>
     * IMPORTANT: This is an asynchronous RPC using Future.
     * If you want sequential non-blocking RPC, please use the request API
     *
     * @param events list of envelopes
     * @param timeout in milliseconds
     * @param timeoutException if true, send timeout exception to onFailure method when some services do not respond.
     * @return future list of results (partial or complete)
     * @throws IOException in case of error
     */
    public Future<List<EventEnvelope>> asyncRequest(final List<EventEnvelope> events, long timeout,
                                                    boolean timeoutException) throws IOException {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException(MISSING_EVENT);
        }
        EventEnvelope first = events.getFirst();
        String from = first.getFrom();
        String traceId = first.getTraceId();
        String tracePath = first.getTracePath();
        AsyncMultiInbox inbox = new AsyncMultiInbox(events.size(), from, traceId, tracePath, timeout, timeoutException);
        String cid = inbox.getCorrelationId();
        Platform platform = Platform.getInstance();
        EventBus system = platform.getEventSystem();
        String replyTo = TemporaryInbox.TEMPORARY_INBOX + "@" + platform.getOrigin();
        int seq = 0;
        for (EventEnvelope input: events) {
            var event = input.copy();
            seq++;
            String sequencedCid = cid + "-" + seq;
            String destination = event.getTo();
            if (destination == null) {
                throw new IllegalArgumentException(MISSING_ROUTING_PATH);
            }
            String to = substituteRouteIfAny(destination);
            event.setTo(to);
            // propagate trace
            event.setFrom(from).setTraceId(traceId).setTracePath(tracePath);
            event.setReplyTo(replyTo).addTag(RPC, timeout).setBroadcastLevel(0);
            String originalCid = event.getCorrelationId();
            event.setCorrelationId(sequencedCid);
            inbox.setCorrelation(sequencedCid, new InboxCorrelation(to, originalCid));
            TargetRoute target = discover(to);
            if (target.isCloud()) {
                MultipartPayload.getInstance().outgoing(target.getManager(), event);
            } else {
                system.send(target.getManager().getRoute(), event.toBytes());
            }
        }
        return inbox.getFuture();
    }

    /**
     * Future request API for sending parallel requests (Fork-n-join)
     * <p>
     * You can retrieve results using future.get()
     *
     * @param events list of envelopes
     * @param timeout in milliseconds. If timeout, throws TimeoutException
     *                wrapped in an ExecutionException with future.get().
     * @return future list of results (partial or complete)
     * @throws IOException in case of error
     */
    public java.util.concurrent.Future<List<EventEnvelope>> request(final List<EventEnvelope> events, long timeout)
            throws IOException {
        return request(events, timeout, true);
    }

    /**
     * Future request API for sending parallel requests (Fork-n-join)
     * <p>
     * You can retrieve results using future.get()
     *
     * @param events list of envelopes
     * @param timeout in milliseconds
     * @param timeoutException if true, throws TimeoutException wrapped in an ExecutionException with future.get().
     *                         Otherwise, return partial results that are successful.
     * @return future list of results (partial or complete)
     * @throws IOException in case of error
     */
    public java.util.concurrent.Future<List<EventEnvelope>> request(final List<EventEnvelope> events, long timeout,
                                                    boolean timeoutException) throws IOException {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException(MISSING_EVENT);
        }
        EventEnvelope first = events.getFirst();
        String from = first.getFrom();
        String traceId = first.getTraceId();
        String tracePath = first.getTracePath();
        FutureMultiInbox inbox = new FutureMultiInbox(events.size(), from, traceId, tracePath, timeout, timeoutException);
        String cid = inbox.getCorrelationId();
        Platform platform = Platform.getInstance();
        EventBus system = platform.getEventSystem();
        String replyTo = TemporaryInbox.TEMPORARY_INBOX + "@" + platform.getOrigin();
        int seq = 0;
        for (EventEnvelope input: events) {
            var event = input.copy();
            seq++;
            String sequencedCid = cid + "-" + seq;
            String destination = event.getTo();
            if (destination == null) {
                throw new IllegalArgumentException(MISSING_ROUTING_PATH);
            }
            String to = substituteRouteIfAny(destination);
            event.setTo(to);
            // propagate trace
            event.setFrom(from).setTraceId(traceId).setTracePath(tracePath);
            event.setReplyTo(replyTo).addTag(RPC, timeout).setBroadcastLevel(0);
            String originalCid = event.getCorrelationId();
            event.setCorrelationId(sequencedCid);
            inbox.setCorrelation(sequencedCid, new InboxCorrelation(to, originalCid));
            TargetRoute target = discover(to);
            if (target.isCloud()) {
                MultipartPayload.getInstance().outgoing(target.getManager(), event);
            } else {
                system.send(target.getManager().getRoute(), event.toBytes());
            }
        }
        return inbox.getFuture();
    }

    /**
     * Check if all routes in a list exist
     *
     * @param routes list of service route names
     * @return true or false
     */
    public boolean exists(String... routes) {
        if (routes == null || routes.length == 0) {
            return false;
        }
        if (routes.length == 1) {
            return routeExists(routes[0]);
        }
        for (String r: routes) {
            if (!routeExists(r)) {
                return false;
            }
        }
        return true;
    }
    /**
     * Check if a route or origin-ID exists.
     * <p>
     * The response should be instantaneous using a distributed routing table.
     *
     * @param route name of the target service
     * @return true or false
     */
    private boolean routeExists(String route) {
        if (route == null) {
            return false;
        }
        Platform platform = Platform.getInstance();
        if (route.equals(platform.getOrigin())) {
            return true;
        }
        String destination = substituteRouteIfAny(route);
        if (platform.hasRoute(destination)) {
            return true;
        }
        // check if the remote service is reachable
        if (Platform.isCloudSelected() &&
                (platform.hasRoute(ServiceDiscovery.SERVICE_QUERY) || platform.hasRoute(CLOUD_CONNECTOR))) {
            if (destination.contains(".")) {
                ConcurrentMap<String, String> targets = cloudRoutes.get(destination);
                return targets != null && !targets.isEmpty();
            } else {
                return cloudOrigins.containsKey(destination);
            }
        }
        return false;
    }

    /**
     * Search for all application instances that contain the service route
     *
     * @param route for a service
     * @return list of application instance IDs (aka originId)
     */
    public Future<List<String>> search(String route) {
        return search(route, false);
    }

    /**
     * Search for all application instances that contain the service route
     *
     * @param route for a service
     * @param remoteOnly if true, search only from service registry. Otherwise, check local registry too.
     * @return list of application instance IDs (i.e. originId)
     */
    @SuppressWarnings("unchecked")
    public Future<List<String>> search(String route, boolean remoteOnly) {
        return Future.future(promise -> {
            Platform platform = Platform.getInstance();
            String actualRoute = substituteRouteIfAny(route);
            if (!remoteOnly && platform.hasRoute(actualRoute)) {
                platform.getVirtualThreadExecutor().submit(() ->
                        promise.complete(Collections.singletonList(platform.getOrigin())));
            } else if (Platform.isCloudSelected()) {
                try {
                    if (platform.hasRoute(ServiceDiscovery.SERVICE_QUERY) || platform.hasRoute(CLOUD_CONNECTOR)) {
                        EventEnvelope event = new EventEnvelope().setTo(ServiceDiscovery.SERVICE_QUERY)
                                .setHeader(ServiceDiscovery.TYPE, ServiceDiscovery.SEARCH)
                                .setHeader(ServiceDiscovery.ROUTE, actualRoute);
                        Future<EventEnvelope> response = asyncRequest(event, 3000);
                        response.onSuccess(evt -> {
                            if (evt.getBody() instanceof List) {
                                promise.complete((List<String>) evt.getBody());
                            } else {
                                promise.complete(Collections.emptyList());
                            }
                        });
                        response.onFailure(promise::fail);
                    }
                } catch (IOException e) {
                    platform.getVirtualThreadExecutor().submit(() -> promise.fail(e));
                }
            } else {
                platform.getVirtualThreadExecutor().submit(() -> promise.complete(Collections.emptyList()));
            }
        });
    }
}
