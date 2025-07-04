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

package org.platformlambda.cloud.services;

import org.platformlambda.cloud.reporter.PresenceConnector;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.*;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ManagedCache;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
@ZeroTracing
public class ServiceRegistry implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);

    public static final String APP_GROUP = "@monitor-";
    public static final String CLOUD_MANAGER = "cloud.manager";
    private static final String PERSONALITY = "personality";
    private static final String ALIVE = "keep-alive";
    private static final String TYPE = ServiceDiscovery.TYPE;
    private static final String ROUTE = ServiceDiscovery.ROUTE;
    private static final String ORIGIN = ServiceDiscovery.ORIGIN;
    private static final String IS_FINAL = "final";
    private static final String TOPIC = "topic";
    private static final String UNREGISTER = ServiceDiscovery.UNREGISTER;
    private static final String ADD = ServiceDiscovery.ADD;
    private static final String EXCHANGE = "exchange";
    private static final String VERSION = "version";
    private static final String SUBSCRIBE_LIFE_CYCLE = "subscribe_life_cycle";
    private static final String UNSUBSCRIBE_LIFE_CYCLE = "unsubscribe_life_cycle";
    private static final String SUBSCRIBE_PM_STATUS = "subscribe_pm_status";
    private static final String UNSUBSCRIBE_PM_STATUS = "unsubscribe_pm_status";
    private static final String JOIN = "join";
    private static final String CONNECTED = "connected";
    private static final String DISCONNECTED = "disconnected";
    private static final String LEAVE = "leave";
    private static final String NAME = "name";
    private static final String MONITOR = "monitor-";
    private static final long EXPIRY = 90 * 1000L;
    private final boolean presenceMonitor;
    private final int closedUserGroup;
    /*
     * cloudRoutes: route_name -> (origin, personality)
     * cloudOrigins: origin -> last seen
     * originTopic: origin -> topic and partition
     */
    private static final EventEmitter po = EventEmitter.getInstance();
    private static final ConcurrentMap<String, ConcurrentMap<String, String>> cloudRoutes = po.getCloudRoutes();
    private static final ConcurrentMap<String, Long> cloudOrigins = po.getCloudOrigins();
    private static final ConcurrentMap<String, String> originTopic = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> originAppVersion = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Boolean> lifeCycleSubscribers = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Boolean> pmSubscribers = new ConcurrentHashMap<>();
    private static final ManagedCache cache = ManagedCache.createCache("member.life.cycle.events", 5000);
    private long lastBroadcastAdd = 0;
    private static String monitorTopic;

    public ServiceRegistry() {
        Utility util = Utility.getInstance();
        AppConfigReader config = AppConfigReader.getInstance();
        presenceMonitor = "true".equals(config.getProperty("service.monitor", "false"));
        monitorTopic = config.getProperty("monitor.topic", "service.monitor");
        // range: 3 - 30
        int maxGroups = Math.min(30,
                Math.max(3, util.str2int(config.getProperty("max.closed.user.groups", "10"))));
        closedUserGroup = util.str2int(config.getProperty("closed.user.group", "1"));
        if (closedUserGroup < 1 || closedUserGroup > maxGroups) {
            log.error("closed.user.group is invalid. Please select a number from 1 to {}", maxGroups);
            System.exit(-1);
        }
    }

    public static Map<String, Map<String, String>> getAllRoutes() {
        return new HashMap<>(cloudRoutes);
    }

    public static List<String> getInstances(String route) {
        if (cloudRoutes.containsKey(route)) {
            return new ArrayList<>(cloudRoutes.get(route).keySet());
        } else {
            return Collections.emptyList();
        }
    }

    public static Map<String, String> getAllOrigins() {
        Utility util = Utility.getInstance();
        Map<String, String> allOrigins = new HashMap<>();
        for (Map.Entry<String, Long> kv: cloudOrigins.entrySet()) {
            String lastSeen = util.getLocalTimestamp(kv.getValue());
            String topic = originTopic.get(kv.getKey());
            if (topic != null) {
                allOrigins.put(kv.getKey(), topic + ", "+lastSeen);
            }
        }
        return allOrigins;
    }

    public static ConcurrentMap<String, String> getDestinations(String route) {
        return cloudRoutes.get(route);
    }

    public static boolean destinationExists(String origin) {
        if (origin == null) {
            return false;
        } else {
            return  origin.startsWith(MONITOR) || origin.equals(Platform.getInstance().getOrigin()) ||
                    cloudOrigins.containsKey(origin);
        }
    }

    public static String getTopic(String dest) {
        return dest.startsWith(MONITOR)? monitorTopic+"-"+dest.substring(MONITOR.length()) : originTopic.get(dest);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        Platform platform = Platform.getInstance();
        String myOrigin = platform.getOrigin();
        String type = headers.get(TYPE);
        if (SUBSCRIBE_LIFE_CYCLE.equals(type) && headers.containsKey(ROUTE)) {
            String subscriber = headers.get(ROUTE);
            if (!subscriber.contains("@") && !lifeCycleSubscribers.containsKey(subscriber)) {
                lifeCycleSubscribers.put(subscriber, true);
                log.info("{} subscribed to member life-cycle events", subscriber);
            }
        }
        if (UNSUBSCRIBE_LIFE_CYCLE.equals(type) && headers.containsKey(ROUTE)) {
            String subscriber = headers.get(ROUTE);
            if (!subscriber.contains("@") && lifeCycleSubscribers.containsKey(subscriber)) {
                lifeCycleSubscribers.remove(subscriber);
                log.info("{} unsubscribed from member life-cycle events", subscriber);
            }
        }
        if (SUBSCRIBE_PM_STATUS.equals(type) && headers.containsKey(ROUTE)) {
            String subscriber = headers.get(ROUTE);
            if (!subscriber.contains("@") && !pmSubscribers.containsKey(subscriber)) {
                pmSubscribers.put(subscriber, true);
                log.info("{} subscribed to presence monitor connection status", subscriber);
            }
        }
        if (UNSUBSCRIBE_PM_STATUS.equals(type) && headers.containsKey(ROUTE)) {
            String subscriber = headers.get(ROUTE);
            if (!subscriber.contains("@") && pmSubscribers.containsKey(subscriber)) {
                pmSubscribers.remove(subscriber);
                log.info("{} unsubscribed from presence monitor connection status", subscriber);
            }
        }
        // when a node joins
        if (JOIN.equals(type) && headers.containsKey(ORIGIN) && headers.containsKey(TOPIC)) {
            String origin = headers.get(ORIGIN);
            String topic = headers.get(TOPIC);
            cloudOrigins.put(origin, System.currentTimeMillis());
            originTopic.put(origin, topic);
            if (!presenceMonitor) {
                if (origin.equals(myOrigin)) {
                    if (headers.containsKey(VERSION)) {
                        log.info("Presence monitor v{} detected", headers.get(VERSION));
                    } else {
                        notifyLifeCycleSubscribers(new Kv(TYPE, CONNECTED));
                        notifyPmSubscribers(new Kv(TYPE, CONNECTED));
                    }
                    registerMyRoutes();

                } else {
                    // send routing table of this node to the newly joined node
                    sendMyRoutes(true);
                }
            }
        }
        if (ALIVE.equals(type) && headers.containsKey(ORIGIN) && headers.containsKey(TOPIC)) {
            String origin = headers.get(ORIGIN);
            String topic = headers.get(TOPIC);
            String name = headers.get(NAME);
            String version = headers.get(VERSION);
            if (!presenceMonitor) {
                if (myOrigin.equals(origin)) {
                    removeStalledPeers();
                } else {
                    if (!cloudOrigins.containsKey(origin)) {
                        log.info("Peer {} joins ({} {})", origin, name, version);
                        po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, JOIN),
                                new Kv(ORIGIN, origin), new Kv(TOPIC, topic));
                    }
                }
            }
            cloudOrigins.put(origin, System.currentTimeMillis());
            originTopic.put(origin, topic);
            if (!originAppVersion.containsKey(origin) && !myOrigin.equals(origin)) {
                log.info("Peer {} active ({} {})", origin, name, version);
            }
            originAppVersion.put(origin, name+" "+version);
        }
        // when a node leaves
        if (LEAVE.equals(type) && headers.containsKey(ORIGIN)) {
            String origin = headers.get(ORIGIN);
            String appVersion = originAppVersion.get(origin);
            originTopic.remove(origin);
            originAppVersion.remove(origin);
            if (presenceMonitor) {
                cloudOrigins.remove(origin);
            } else {
                // remove corresponding entries from routing table
                if (origin.equals(platform.getOrigin())) {
                    // this happens when the service-monitor is down
                    List<String> all = new ArrayList<>(cloudOrigins.keySet());
                    for (String o : all) {
                        if (!o.equals(origin)) {
                            if (appVersion == null) {
                                log.info("{} disconnected", o);
                            } else {
                                log.info("{} disconnected ({})", o, appVersion);
                            }
                            removeRoutesFromOrigin(o);
                            notifyLifeCycleSubscribers(new Kv(TYPE, LEAVE), new Kv(ORIGIN, o));
                        }
                    }
                    notifyLifeCycleSubscribers(new Kv(TYPE, DISCONNECTED));
                    notifyPmSubscribers(new Kv(TYPE, DISCONNECTED));
                } else {
                    if (appVersion == null) {
                        log.info("Peer {} left", origin);
                    } else {
                        log.info("Peer {} left ({})", origin, appVersion);
                    }
                    removeRoutesFromOrigin(origin);
                    notifyLifeCycleSubscribers(new Kv(TYPE, LEAVE), new Kv(ORIGIN, origin));
                    cache.remove(origin);
                }
            }
        }
        if (!presenceMonitor) {
            // add route
            if (ADD.equals(type) && headers.containsKey(ORIGIN)) {
                String origin = headers.get(ORIGIN);
                if (headers.containsKey(ROUTE) && headers.containsKey(PERSONALITY)) {
                    // add a single route
                    String route = headers.get(ROUTE);
                    String personality = headers.get(PERSONALITY);
                    // add to routing table
                    addRoute(origin, route, personality);
                    if (origin.equals(myOrigin) && !headers.containsKey(IS_FINAL)) {
                        // broadcast to peers
                        EventEnvelope request = new EventEnvelope();
                        request.setTo(ServiceDiscovery.SERVICE_REGISTRY + APP_GROUP + closedUserGroup)
                                .setHeaders(headers).setHeader(IS_FINAL, true);
                        po.send(request);
                    }

                } else if (input instanceof Map && !origin.equals(myOrigin)) {
                    // add a list of routes
                    Map<String, String> routeMap = (Map<String, String>) input;
                    int count = routeMap.size();
                    int n = 0;
                    for (Map.Entry<String, String> kv : routeMap.entrySet()) {
                        String personality = kv.getValue();
                        if (addRoute(origin, kv.getKey(), personality)) n++;
                    }
                    if (n > 0) {
                        log.info("Loaded {} route{} from {}", count, count == 1 ? "" : "s", origin);
                    }
                    if (headers.containsKey(TOPIC)) {
                        originTopic.put(origin, headers.get(TOPIC));
                    }
                    if (headers.containsKey(NAME) && !cache.exists(origin)) {
                        cache.put(origin, true);
                        notifyLifeCycleSubscribers(new Kv(TYPE, JOIN),
                                new Kv(ORIGIN, origin), new Kv(NAME, headers.get(NAME)));
                    }
                    if (headers.containsKey(EXCHANGE)) {
                        sendMyRoutes(false);
                    }
                }
            }
            // clear a route
            if (UNREGISTER.equals(type) && headers.containsKey(ROUTE) && headers.containsKey(ORIGIN)) {
                String route = headers.get(ROUTE);
                String origin = headers.get(ORIGIN);
                // remove from routing table
                removeRoute(origin, route);
                if (origin.equals(myOrigin) && !headers.containsKey(IS_FINAL)) {
                    // broadcast to peers
                    EventEnvelope request = new EventEnvelope();
                    request.setTo(ServiceDiscovery.SERVICE_REGISTRY + APP_GROUP + closedUserGroup)
                            .setHeaders(headers).setHeader(IS_FINAL, true);
                    po.send(request);
                }
            }
        }
        return true;
    }

    private void sendMyRoutes(boolean exchange) {
        long now = System.currentTimeMillis();
        if (!exchange && now - lastBroadcastAdd < 100) {
            log.debug("Duplicated broadcast add ignored");
            return;
        }
        lastBroadcastAdd = now;
        Platform platform = Platform.getInstance();
        String myOrigin = platform.getOrigin();
        Map<String, String> routeMap = new HashMap<>();
        for (Map.Entry<String, ConcurrentMap<String, String>> kv : cloudRoutes.entrySet()) {
            ConcurrentMap<String, String> originMap = kv.getValue();
            if (originMap.containsKey(myOrigin)) {
                routeMap.put(kv.getKey(), originMap.get(myOrigin));
            }
        }
        EventEnvelope request = new EventEnvelope()
                .setTo(ServiceDiscovery.SERVICE_REGISTRY + APP_GROUP + closedUserGroup)
                .setHeader(TOPIC, PresenceConnector.getInstance().getTopic())
                .setHeader(NAME, platform.getName())
                .setHeader(TYPE, ADD).setHeader(ORIGIN, myOrigin).setBody(routeMap);
        if (exchange) {
            request.setHeader(EXCHANGE, true);
        }
        po.send(request);
    }

    private boolean addRoute(String origin, String route, String personality) {
        if (!cloudRoutes.containsKey(route)) {
            cloudRoutes.put(route, new ConcurrentHashMap<>());
        }
        ConcurrentMap<String, String> originMap = cloudRoutes.get(route);
        if (originMap.containsKey(origin)) {
            return false;
        } else {
            originMap.put(origin, personality);
            cloudOrigins.put(origin, System.currentTimeMillis());
            log.info("{} ({}.{}) registered", route, personality, origin);
            return true;
        }
    }

    private void removeRoute(String origin, String route) {
        boolean deleted = false;
        if (cloudRoutes.containsKey(route)) {
            ConcurrentMap<String, String> originMap = cloudRoutes.get(route);
            if (originMap.containsKey(origin)) {
                originMap.remove(origin);
                deleted = true;
            }
            if (originMap.isEmpty()) {
                cloudRoutes.remove(route);
            }
        }
        if (deleted) {
            log.info("{} {} unregistered", route, origin);
        }
    }

    private void removeRoutesFromOrigin(String origin) {
        List<String> routeList = new ArrayList<>(cloudRoutes.keySet());
        for (String r: routeList) {
            removeRoute(origin, r);
        }
        cloudOrigins.remove(origin);
        originTopic.remove(origin);
    }

    private void registerMyRoutes() {
        Platform platform = Platform.getInstance();
        String personality = platform.getName() + ", " + ServerPersonality.getInstance().getType().name();
        String origin = platform.getOrigin();
        // copy local registry to global registry
        ConcurrentMap<String, ServiceDef> routingTable = platform.getLocalRoutingTable();
        List<String> routes = new ArrayList<>(routingTable.keySet());
        for (String r: routes) {
            ServiceDef def = routingTable.get(r);
            if (def != null && !def.isPrivate()) {
                addRoute(origin, def.getRoute(), personality);
            }
        }
    }

    private void removeStalledPeers() {
        long now = System.currentTimeMillis();
        String myOrigin = Platform.getInstance().getOrigin();
        List<String> peers = new ArrayList<>(cloudOrigins.keySet());
        for (String p: peers) {
            if (!p.equals(myOrigin)) {
                long lastActive = cloudOrigins.get(p);
                if (now - lastActive > EXPIRY) {
                    String appVersion = originAppVersion.get(p);
                    log.error("Peer {} stalled ({})", p, appVersion);
                    po.send(ServiceDiscovery.SERVICE_REGISTRY, new Kv(TYPE, LEAVE), new Kv(ORIGIN, p));
                }
            }
        }
    }

    private void notifyLifeCycleSubscribers(Kv...parameters) {
        if (!lifeCycleSubscribers.isEmpty()) {
            for (String subscriber: lifeCycleSubscribers.keySet()) {
                try {
                    EventEnvelope event = new EventEnvelope().setTo(subscriber);
                    for (Kv kv: parameters) {
                        event.setHeader(kv.key, kv.value);
                    }
                    po.send(event);
                } catch (IllegalArgumentException e) {
                    log.warn("Unable to inform Member Life Cycle subscriber {} - {}", subscriber, e.getMessage());
                }
            }
        }
    }

    private void notifyPmSubscribers(Kv...parameters) {
        if (!pmSubscribers.isEmpty()) {
            for (String subscriber: pmSubscribers.keySet()) {
                try {
                    EventEnvelope event = new EventEnvelope().setTo(subscriber);
                    for (Kv kv: parameters) {
                        event.setHeader(kv.key, kv.value);
                    }
                    po.send(event);
                } catch (IllegalArgumentException e) {
                    log.warn("Unable to inform PM Status subscriber {} - {}", subscriber, e.getMessage());
                }
            }
        }
    }
}
