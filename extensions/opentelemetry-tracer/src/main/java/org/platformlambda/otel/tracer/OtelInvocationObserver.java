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

package org.platformlambda.otel.tracer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.platformlambda.core.spi.InvocationCarrier;
import org.platformlambda.core.spi.InvocationContinuation;
import org.platformlambda.core.spi.InvocationInfo;
import org.platformlambda.core.spi.InvocationObserver;
import org.platformlambda.core.spi.InvocationScope;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opens a live OpenTelemetry span for every composable-function hop, through the vendor-neutral API only.
 * Whatever implements the API in this JVM records the spans: Dynatrace OneAgent with its "OpenTelemetry
 * (Java)" feature nests them into its PurePaths, the OpenTelemetry Java agent exports them, and without
 * either the API is a no-op.
 * <p>
 * <b>Each hop is a {@code CONSUMER} span</b> named after the route (OneAgent takes Server and Consumer spans
 * as entry points by default). Its parent is the W3C context the sender wrote into the event's tags
 * ({@link #TRACEPARENT_TAG}); without one, the current context of the thread (an agent may bridge its own),
 * else a new trace. The span is made current on the worker thread, so a library the function calls - an
 * HTTP client the agent instruments, for example - nests under it.
 * <p>
 * <b>A span is not bound to a thread.</b> A function that returns a Mono hands its span over
 * ({@link InvocationScope#detach}): the span ends when the result is delivered, so a reactive hop shows its
 * real duration, which an SDK tracer bound to one thread cannot.
 */
public class OtelInvocationObserver implements InvocationObserver {
    public static final String TRACEPARENT_TAG = "otel_traceparent";
    public static final String TRACESTATE_TAG = "otel_tracestate";
    public static final String INSTRUMENTATION = "org.platformlambda.otel.tracer";
    private static final String TRACEPARENT = "traceparent";
    private static final String TRACESTATE = "tracestate";
    private static final TextMapGetter<InvocationCarrier> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(InvocationCarrier carrier) {
            return List.of(TRACEPARENT, TRACESTATE);
        }

        @Override
        public String get(InvocationCarrier carrier, String key) {
            return carrier == null? null : carrier.get(tagOf(key));
        }
    };
    private static final TextMapSetter<InvocationCarrier> SETTER = (carrier, key, value) -> {
        if (carrier != null) {
            carrier.set(tagOf(key), value);
        }
    };
    private final Tracer tracer;
    private final TextMapPropagator propagator = W3CTraceContextPropagator.getInstance();

    /**
     * @param openTelemetry the API implementation (GlobalOpenTelemetry in production)
     */
    public OtelInvocationObserver(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION);
    }

    @Override
    public InvocationScope begin(InvocationInfo info, InvocationCarrier carrier) {
        Context parent = propagator.extract(Context.root(), carrier, GETTER);
        if (!Span.fromContext(parent).getSpanContext().isValid()) {
            parent = Context.current();
        }
        SpanBuilder builder = tracer.spanBuilder(info.route()).setSpanKind(SpanKind.CONSUMER).setParent(parent)
                .setAttribute("messaging.system", "mercury")
                .setAttribute("messaging.destination.name", info.route())
                .setAttribute("messaging.message.id", info.eventId())
                .setAttribute("mercury.trace_id", info.traceId());
        if (info.tracePath() != null) {
            builder.setAttribute("mercury.trace_path", info.tracePath());
        }
        if (info.from() != null) {
            builder.setAttribute("mercury.from", info.from());
        }
        Span span = builder.startSpan();
        return new SpanScope(span, span.makeCurrent());
    }

    @Override
    public void send(InvocationInfo info, InvocationCarrier carrier) {
        // a stale context copied from an earlier event is never forwarded
        carrier.remove(TRACEPARENT_TAG);
        carrier.remove(TRACESTATE_TAG);
        Context current = Context.current();
        if (Span.fromContext(current).getSpanContext().isValid()) {
            propagator.inject(current, carrier, SETTER);
        }
    }

    private static String tagOf(String key) {
        return TRACESTATE.equals(key)? TRACESTATE_TAG : TRACEPARENT_TAG;
    }

    /**
     * A hop's span, current on the worker thread while the function runs.
     */
    private static final class SpanScope implements InvocationScope {
        private final Span span;
        private final Scope scope;
        private boolean detached = false;

        SpanScope(Span span, Scope scope) {
            this.span = span;
            this.scope = scope;
        }

        @Override
        public void error(String message) {
            span.setStatus(StatusCode.ERROR, message);
        }

        @Override
        public void end() {
            scope.close();
            if (!detached) {
                span.end();
            }
        }

        @Override
        public InvocationContinuation detach() {
            detached = true;
            AtomicBoolean ended = new AtomicBoolean(false);
            return () -> new ResumedScope(span, span.makeCurrent(), ended);
        }
    }

    /**
     * The same span on the completion thread: current while the deferred result is delivered, ended once.
     */
    private record ResumedScope(Span span, Scope scope, AtomicBoolean ended) implements InvocationScope {

        @Override
        public void error(String message) {
            span.setStatus(StatusCode.ERROR, message);
        }

        @Override
        public void end() {
            scope.close();
            if (ended.compareAndSet(false, true)) {
                span.end();
            }
        }
    }
}
