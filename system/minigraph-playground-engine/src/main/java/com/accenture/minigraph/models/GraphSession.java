package com.accenture.minigraph.models;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GraphSession {
    public final long startTime = System.currentTimeMillis();
    private final ConcurrentMap<String, Boolean> subscribers = new ConcurrentHashMap<>();
    private final String sessionId;
    private String targetId;
    private static final String IN_SUFFIX = ".in";
    private static final String OUT_SUFFIX = ".out";

    public GraphSession(String route) {
        var id =  getSessionId(route);
        this.sessionId = id;
        this.targetId = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTargetId() {
        return targetId;
    }

    public boolean isPrimary() {
        return sessionId.equals(targetId);
    }

    public void setTargetId(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            this.targetId = sessionId;
        }
    }

    public Set<String> getSubscribers() {
        return subscribers.keySet();
    }

    public void subscribe(String outRoute) {
        subscribers.put(outRoute, true);
    }

    public boolean hasSubscriber(String outRoute) {
        return subscribers.containsKey(outRoute);
    }

    public void unsubscribe(String outRoute) {
        subscribers.remove(outRoute);
    }

    public static String getSessionId(String route) {
        final String id;
        if (route.endsWith(IN_SUFFIX)) {
            id = route.substring(0, route.length() - IN_SUFFIX.length());
        } else if (route.endsWith(OUT_SUFFIX)) {
            id = route.substring(0, route.length() - OUT_SUFFIX.length());
        } else {
            return route;
        }
        return id.replace('.', '-');
    }

    public static String getInRoute(String sessionId) {
        if (sessionId.endsWith(IN_SUFFIX)) {
            return sessionId;
        } else if (sessionId.endsWith(OUT_SUFFIX)) {
            return sessionId.substring(0, sessionId.length() - OUT_SUFFIX.length()) + IN_SUFFIX;
        } else {
            return sessionId.replace('-', '.') + IN_SUFFIX;
        }
    }

    public static String getOutRoute(String sessionId) {
        if (sessionId.endsWith(OUT_SUFFIX)) {
            return sessionId;
        } else if (sessionId.endsWith(IN_SUFFIX)) {
            return sessionId.substring(0, sessionId.length() - IN_SUFFIX.length()) + OUT_SUFFIX;
        } else {
            return sessionId.replace('-', '.') + OUT_SUFFIX;
        }
    }
}
