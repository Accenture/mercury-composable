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

package org.platformlambda.core.spi;

import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.W3cTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The registry of {@link InvocationObserver}s, and the engine's only entry to them. Empty by default:
 * with no observer registered every engine call returns after one emptiness check.
 * <p>
 * An invocation is observed when an observer is registered, the event carries a trace id (the request
 * is traced), and its route passes the route filter:
 * <ul>
 *   <li>{@code invocation.observer.include} - a comma-separated list of routes; when set, only these
 *       are observed. An entry ending with {@code *} matches a route prefix.</li>
 *   <li>{@code invocation.observer.exclude} - routes never observed. Defaults to the telemetry routes
 *       and the RPC reply inbox, which are plumbing; setting the property replaces the default.</li>
 *   <li>{@code invocation.observer.unlinked} - the negative control of a tracing check: these
 *       routes are observed, but {@link InvocationObserver#begin} sees no incoming tags, so each of
 *       their invocations starts a new trace. Their reply keeps the caller's tags ({@link #reply}), so
 *       only the route itself is a separate fragment and the rest of the request stays in the caller's
 *       trace. A test that cannot see that break proves nothing.</li>
 * </ul>
 * Every observer call is guarded: an exception is logged (once per observer class at WARN) and the
 * invocation proceeds unobserved by that observer.
 */
public final class InvocationObservers {
    private static final Logger log = LoggerFactory.getLogger(InvocationObservers.class);
    public static final String INCLUDE = "invocation.observer.include";
    public static final String EXCLUDE = "invocation.observer.exclude";
    public static final String UNLINKED = "invocation.observer.unlinked";
    private static final String DEFAULT_EXCLUDE =
            "distributed.tracing, distributed.trace.forwarder, transaction.journal.recorder, temporary.inbox";
    private static final InvocationScope NONE = () -> { };
    private static final InvocationCarrier NO_TAGS = new InvocationCarrier() {
        @Override
        public String get(String key) {
            return null;
        }
        @Override
        public void set(String key, String value) {
            // the negative control drops the incoming linkage
        }
        @Override
        public void remove(String key) {
            // nothing to remove
        }
    };
    private static final List<InvocationObserver> observers = new CopyOnWriteArrayList<>();
    private static final Set<String> failingObservers = ConcurrentHashMap.newKeySet();
    private static final AtomicReference<RouteFilter> filter = new AtomicReference<>();
    // set while an unlinked route's reply is delivered on this thread: send leaves its tags as they are
    private static final ThreadLocal<Boolean> keepingTags = new ThreadLocal<>();

    private InvocationObservers() { }

    /**
     * Register an observer. An adapter calls this once at startup, typically from a
     * {@code @BeforeApplication} class gated by {@code @OptionalService}.
     *
     * @param observer the observer; registering the same instance twice has no effect
     */
    public static void register(InvocationObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            log.info("Invocation observer {} registered", observer.getClass().getName());
        }
    }

    /**
     * @param observer a registered observer
     * @return true when it was registered
     */
    public static boolean unregister(InvocationObserver observer) {
        return observers.remove(observer);
    }

    /**
     * Engine call, on the thread that is about to run a function.
     *
     * @param event the incoming event
     * @param route the function's route (without the worker instance suffix)
     * @param instance the worker instance number
     * @return the scope to close with {@link #close}; never null
     */
    public static InvocationScope begin(EventEnvelope event, String route, int instance) {
        if (observers.isEmpty() || event.getTraceId() == null) {
            return NONE;
        }
        RouteFilter routes = getFilter();
        if (!routes.isObserved(route)) {
            return NONE;
        }
        InvocationInfo info = new InvocationInfo(route, event.getFrom(), event.getId(),
                event.getTraceId(), event.getTracePath(), event.getSpanId(), instance);
        InvocationCarrier carrier = routes.isUnlinked(route)? NO_TAGS : new EnvelopeCarrier(event);
        List<InvocationScope> scopes = new ArrayList<>(observers.size());
        for (InvocationObserver observer : observers) {
            try {
                InvocationScope scope = observer.begin(info, carrier);
                if (scope != null) {
                    scopes.add(scope);
                }
            } catch (Exception e) {
                failed(observer, "begin", e);
            }
        }
        if (scopes.isEmpty()) {
            return NONE;
        }
        InvocationScope scope = scopes.size() == 1? scopes.getFirst() : new CompositeScope(scopes);
        return carrier == NO_TAGS? new UnlinkedScope(scope) : scope;
    }

    /**
     * Engine call at an entry point that would start a new trace: the trace context of an agent already
     * tracing this thread, to continue as if the caller had sent it, so Mercury's trace and the agent's are
     * one. The first observer that answers a valid W3C {@code traceparent} wins.
     *
     * @return a W3C traceparent value, or null when no observer has one
     */
    public static String traceparent() {
        for (InvocationObserver observer : observers) {
            try {
                String traceparent = observer.traceparent();
                if (W3cTrace.parse(traceparent).length > 0) {
                    return traceparent;
                }
            } catch (Exception e) {
                failed(observer, "traceparent", e);
            }
        }
        return null;
    }

    /**
     * Engine call, on the thread that sends an event, before the event leaves. The event must be the
     * engine's own copy: an observer may add or replace its tag.
     *
     * @param event the outgoing event
     */
    public static void send(EventEnvelope event) {
        if (observers.isEmpty() || event.getTraceId() == null || event.getTo() == null || keepingTags.get() != null) {
            return;
        }
        String route = routeOf(event.getTo());
        if (!getFilter().isObserved(route)) {
            return;
        }
        InvocationInfo info = new InvocationInfo(route, event.getFrom(), event.getId(),
                event.getTraceId(), event.getTracePath(), event.getSpanId(), 1);
        InvocationCarrier carrier = new EnvelopeCarrier(event);
        for (InvocationObserver observer : observers) {
            try {
                observer.send(info, carrier);
            } catch (Exception e) {
                failed(observer, "send", e);
            }
        }
    }

    /**
     * Engine call: deliver a function's reply on the scope's thread while the scope is open. The reply
     * carries the incoming event's tags, which the worker copies. The reply of a route in
     * {@code invocation.observer.unlinked} keeps them, so the caller's next step consumes the caller's own
     * tag, and the negative control breaks one link instead of moving the rest of the request into the
     * route's new trace. Two things make that hold:
     * <ul>
     *   <li>{@link #send} does not re-tag the reply;</li>
     *   <li>the route's scope is closed first, because an agent that carries the thread's context into an
     *       executor would otherwise carry the route's trace into the next step, whatever the tag says.</li>
     * </ul>
     * Any other reply is tagged by {@link #send} as usual, as is every other event the route sends.
     *
     * @param scope the function's scope from {@link #begin}
     * @param error the error message when the function failed, or null
     * @param delivery sends the reply
     */
    public static void reply(InvocationScope scope, String error, Runnable delivery) {
        if (scope instanceof UnlinkedScope) {
            close(scope, error);
            keepingTags(delivery);
        } else {
            delivery.run();
        }
    }

    // an Event-over-HTTP reply passes send twice on this thread, so the whole delivery keeps its tags
    private static void keepingTags(Runnable delivery) {
        keepingTags.set(Boolean.TRUE);
        try {
            delivery.run();
        } finally {
            keepingTags.remove();
        }
    }

    /**
     * Integration call, on the thread that hands a message to a transport outside the event bus (a Kafka
     * producer, for example): the observers' tags for that outgoing message, as {@link #send} would write
     * them on an event. The transport carries them in its own headers, and its consumer puts them back on
     * the event it dispatches and opens the receiving side with {@link #begin}, so the linkage crosses the
     * transport as it crosses the event bus.
     *
     * @param destination where the message goes, such as a topic; the route filter applies to it
     * @param traceId Mercury's trace id; an untraced message carries no tags
     * @param tracePath Mercury's trace path, or null
     * @return the tags by the observers' own names; empty when nothing is observed
     */
    public static Map<String, String> export(String destination, String traceId, String tracePath) {
        if (observers.isEmpty() || destination == null || traceId == null || !getFilter().isObserved(destination)) {
            return Map.of();
        }
        InvocationInfo info = new InvocationInfo(destination, null, Utility.getInstance().getUuid(),
                traceId, tracePath, null, 1);
        MapCarrier carrier = new MapCarrier(new HashMap<>());
        for (InvocationObserver observer : observers) {
            try {
                observer.send(info, carrier);
            } catch (Exception e) {
                failed(observer, "send", e);
            }
        }
        return Map.copyOf(carrier.tags());
    }

    /**
     * Engine call: close a scope on its own thread - error first when the function failed.
     *
     * @param scope the scope from {@link #begin} or {@link #resume}
     * @param error the error message, or null when the function succeeded
     */
    public static void close(InvocationScope scope, String error) {
        if (scope == null || scope == NONE) {
            return;
        }
        if (error != null) {
            try {
                scope.error(error);
            } catch (Exception e) {
                failed(scope, "error", e);
            }
        }
        try {
            scope.end();
        } catch (Exception e) {
            failed(scope, "end", e);
        }
    }

    /**
     * Engine call, on the scope's thread while it is open: the function returned a Mono.
     *
     * @param scope the open scope
     * @return a continuation that resumes at most once, or null when nothing is to be resumed
     */
    public static InvocationContinuation detach(InvocationScope scope) {
        if (scope == null || scope == NONE) {
            return null;
        }
        try {
            InvocationContinuation continuation = scope.detach();
            return continuation == null? null : new OnceContinuation(continuation, scope instanceof UnlinkedScope);
        } catch (Exception e) {
            failed(scope, "detach", e);
            return null;
        }
    }

    /**
     * Engine call, on the completion thread: open the continuation's scope, run the delivery of the
     * deferred result inside it, and close it.
     *
     * @param continuation the continuation from {@link #detach}, or null
     * @param error the error message when the deferred result failed, or null
     * @param delivery sends the result
     */
    public static void resume(InvocationContinuation continuation, String error, Runnable delivery) {
        InvocationScope scope = continuation == null? null : continuation.resume();
        if (continuation instanceof OnceContinuation once && once.unlinked) {
            // the negative control's deferred reply, as in reply: the route's node ends before the reply leaves
            close(scope, error);
            keepingTags(delivery);
            return;
        }
        try {
            delivery.run();
        } finally {
            close(scope, error);
        }
    }

    /**
     * Engine call when a deferred result will never be delivered (cancelled on timeout): resume the
     * continuation if no delivery did, and close it as failed.
     *
     * @param continuation the continuation from {@link #detach}, or null
     * @param reason why nothing was delivered
     */
    public static void abandon(InvocationContinuation continuation, String reason) {
        if (continuation != null) {
            close(continuation.resume(), reason);
        }
    }

    /**
     * Re-read the route filter from the application configuration on next use.
     */
    static void reload() {
        filter.set(null);
    }

    /**
     * Test hook: set the route filter directly.
     */
    static void configure(String include, String exclude, String unlinked) {
        filter.set(new RouteFilter(include, exclude == null? DEFAULT_EXCLUDE : exclude, unlinked));
    }

    private static RouteFilter getFilter() {
        RouteFilter current = filter.get();
        if (current == null) {
            AppConfigReader config = AppConfigReader.getInstance();
            current = new RouteFilter(config.getProperty(INCLUDE, ""),
                    config.getProperty(EXCLUDE, DEFAULT_EXCLUDE), config.getProperty(UNLINKED, ""));
            filter.set(current);
        }
        return current;
    }

    private static String routeOf(String to) {
        int at = to.indexOf('@');
        return at > 0? to.substring(0, at) : to;
    }

    private static void failed(Object source, String operation, Exception e) {
        String name = source.getClass().getName();
        if (failingObservers.add(name)) {
            log.warn("Invocation observer {} failed in {} - {} (further failures are logged at debug level)",
                    name, operation, e.getMessage());
        } else {
            log.debug("Invocation observer {} failed in {} - {}", name, operation, e.getMessage());
        }
    }

    private record EnvelopeCarrier(EventEnvelope event) implements InvocationCarrier {
        @Override
        public String get(String key) {
            return event.getTag(key);
        }

        @Override
        public void set(String key, String value) {
            if (value == null || value.isEmpty()) {
                event.removeTag(key);
            } else {
                event.addTag(key, value);
            }
        }

        @Override
        public void remove(String key) {
            event.removeTag(key);
        }
    }

    private record MapCarrier(Map<String, String> tags) implements InvocationCarrier {
        @Override
        public String get(String key) {
            return tags.get(key);
        }

        @Override
        public void set(String key, String value) {
            if (value == null || value.isEmpty()) {
                tags.remove(key);
            } else {
                tags.put(key, value);
            }
        }

        @Override
        public void remove(String key) {
            tags.remove(key);
        }
    }

    /**
     * The scopes of several observers: closed in reverse order of opening, so a tracer that must
     * close LIFO on its thread is closed after every tracer opened after it.
     */
    private record CompositeScope(List<InvocationScope> scopes) implements InvocationScope {

        @Override
        public void error(String message) {
            for (InvocationScope scope : scopes.reversed()) {
                try {
                    scope.error(message);
                } catch (Exception e) {
                    failed(scope, "error", e);
                }
            }
        }

        @Override
        public void end() {
            for (InvocationScope scope : scopes.reversed()) {
                try {
                    scope.end();
                } catch (Exception e) {
                    failed(scope, "end", e);
                }
            }
        }

        @Override
        public InvocationContinuation detach() {
            List<InvocationContinuation> continuations = new ArrayList<>(scopes.size());
            for (InvocationScope scope : scopes) {
                try {
                    InvocationContinuation continuation = scope.detach();
                    if (continuation != null) {
                        continuations.add(continuation);
                    }
                } catch (Exception e) {
                    failed(scope, "detach", e);
                }
            }
            if (continuations.isEmpty()) {
                return null;
            }
            return () -> resumeAll(continuations);
        }

        private static InvocationScope resumeAll(List<InvocationContinuation> continuations) {
            List<InvocationScope> resumed = new ArrayList<>(continuations.size());
            for (InvocationContinuation continuation : continuations) {
                try {
                    InvocationScope scope = continuation.resume();
                    if (scope != null) {
                        resumed.add(scope);
                    }
                } catch (Exception e) {
                    failed(continuation, "resume", e);
                }
            }
            return resumed.isEmpty()? null : new CompositeScope(resumed);
        }
    }

    /**
     * The scope of a route in {@code invocation.observer.unlinked}: closed at most once, because
     * {@link #reply} closes it before the reply leaves and the engine closes it again when the function
     * returns.
     */
    private static final class UnlinkedScope implements InvocationScope {
        private final InvocationScope scope;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        UnlinkedScope(InvocationScope scope) {
            this.scope = scope;
        }

        @Override
        public void error(String message) {
            if (!closed.get()) {
                scope.error(message);
            }
        }

        @Override
        public void end() {
            if (closed.compareAndSet(false, true)) {
                scope.end();
            }
        }

        @Override
        public InvocationContinuation detach() {
            return scope.detach();
        }
    }

    /**
     * A continuation resumed at most once, whether by the delivery or by {@link #abandon}.
     */
    private static final class OnceContinuation implements InvocationContinuation {
        private final InvocationContinuation continuation;
        private final boolean unlinked;
        private final AtomicBoolean resumed = new AtomicBoolean(false);

        OnceContinuation(InvocationContinuation continuation, boolean unlinked) {
            this.continuation = continuation;
            this.unlinked = unlinked;
        }

        @Override
        public InvocationScope resume() {
            if (!resumed.compareAndSet(false, true)) {
                return null;
            }
            try {
                return continuation.resume();
            } catch (Exception e) {
                failed(continuation, "resume", e);
                return null;
            }
        }
    }

    /**
     * Which routes are observed, and which are the negative control.
     */
    static final class RouteFilter {
        private final List<String> include;
        private final List<String> exclude;
        private final List<String> unlinked;

        RouteFilter(String include, String exclude, String unlinked) {
            this.include = split(include);
            this.exclude = split(exclude);
            this.unlinked = split(unlinked);
        }

        boolean isObserved(String route) {
            return (include.isEmpty() || matches(include, route)) && !matches(exclude, route);
        }

        boolean isUnlinked(String route) {
            return matches(unlinked, route);
        }

        private static boolean matches(List<String> patterns, String route) {
            for (String pattern : patterns) {
                if (pattern.endsWith("*")?
                        route.startsWith(pattern.substring(0, pattern.length() - 1)) : route.equals(pattern)) {
                    return true;
                }
            }
            return false;
        }

        private static List<String> split(String text) {
            Set<String> result = new HashSet<>();
            if (text != null) {
                for (String entry : Utility.getInstance().split(text, ", ")) {
                    if (!entry.isEmpty()) {
                        result.add(entry);
                    }
                }
            }
            return List.copyOf(result);
        }
    }
}
