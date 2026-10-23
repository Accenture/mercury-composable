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
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.spi.InvocationCarrier;
import org.platformlambda.core.spi.InvocationContinuation;
import org.platformlambda.core.spi.InvocationInfo;
import org.platformlambda.core.spi.InvocationScope;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The mapping of a hop onto OpenTelemetry spans.
 */
class OtelInvocationObserverTest {
    private static final InvocationInfo HOP = new InvocationInfo("demo.route", "sender", "event-1",
            "trace-1", "GET /api/demo", "span-1", 1);

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(exporter)).build())
            .build();
    private final OtelInvocationObserver observer = new OtelInvocationObserver(sdk);

    @Test
    void aHopIsAConsumerSpanCurrentWhileTheFunctionRuns() {
        InvocationScope scope = observer.begin(HOP, new MapCarrier(Map.of()));
        String current = Span.current().getSpanContext().getSpanId();
        scope.end();

        SpanData span = exporter.getFinishedSpanItems().getFirst();
        assertEquals("demo.route", span.getName());
        assertEquals(SpanKind.CONSUMER, span.getKind());
        assertEquals(span.getSpanId(), current);
        assertEquals("mercury", span.getAttributes().asMap().entrySet().stream()
                .filter(e -> e.getKey().getKey().equals("messaging.system")).findFirst().orElseThrow().getValue());
        assertEquals("trace-1", attribute(span, "mercury.trace_id"));
        assertEquals("GET /api/demo", attribute(span, "mercury.trace_path"));
        assertEquals("sender", attribute(span, "mercury.from"));
        assertFalse(Span.current().getSpanContext().isValid(), "the hop's span stays current after the hop");
    }

    @Test
    void aSendCarriesTheCurrentSpanAndTheReceiverBecomesItsChild() {
        MapCarrier carrier = new MapCarrier(Map.of(OtelInvocationObserver.TRACESTATE_TAG, "stale=1"));
        InvocationScope sender = observer.begin(HOP, new MapCarrier(Map.of()));
        observer.send(HOP, carrier);
        sender.end();

        assertTrue(carrier.get(OtelInvocationObserver.TRACEPARENT_TAG).startsWith("00-"));
        assertNull(carrier.get(OtelInvocationObserver.TRACESTATE_TAG), "a stale trace state is not forwarded");
        observer.begin(HOP, carrier).end();

        List<SpanData> spans = exporter.getFinishedSpanItems();
        assertEquals(spans.get(0).getSpanId(), spans.get(1).getParentSpanId());
        assertEquals(spans.get(0).getTraceId(), spans.get(1).getTraceId());
    }

    @Test
    void aSendWithoutAContextRemovesAStaleOne() {
        MapCarrier carrier = new MapCarrier(Map.of(OtelInvocationObserver.TRACEPARENT_TAG,
                "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"));

        observer.send(HOP, carrier);

        assertNull(carrier.get(OtelInvocationObserver.TRACEPARENT_TAG));
    }

    @Test
    void withoutATagTheThreadsCurrentContextIsTheParent() {
        Span outer = sdk.getTracer("test").spanBuilder("agent-request").startSpan();
        try (Scope ignored = outer.makeCurrent()) {
            observer.begin(HOP, new MapCarrier(Map.of())).end();
        }
        outer.end();

        SpanData hop = exporter.getFinishedSpanItems().getFirst();
        assertEquals(outer.getSpanContext().getSpanId(), hop.getParentSpanId());
    }

    @Test
    void aFailedHopHasAnErrorStatus() {
        InvocationScope scope = observer.begin(HOP, new MapCarrier(Map.of()));
        scope.error("demo failure");
        scope.end();

        SpanData span = exporter.getFinishedSpanItems().getFirst();
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("demo failure", span.getStatus().getDescription());
    }

    @Test
    void aDeferredResultEndsTheSameSpanOnTheCompletionThread() throws Exception {
        InvocationScope scope = observer.begin(HOP, new MapCarrier(Map.of()));
        InvocationContinuation continuation = scope.detach();
        scope.end();
        assertTrue(exporter.getFinishedSpanItems().isEmpty(), "the span must stay open until the result");
        String[] currentOnCompletion = new String[1];
        Thread completion = Thread.ofVirtual().start(() -> {
            Utility.getInstance().sleep(150);
            InvocationScope resumed = continuation.resume();
            currentOnCompletion[0] = Span.current().getSpanContext().getSpanId();
            resumed.error("late failure");
            resumed.end();
            resumed.end();
        });
        completion.join();

        List<SpanData> spans = exporter.getFinishedSpanItems();
        assertEquals(1, spans.size(), "ended exactly once");
        SpanData span = spans.getFirst();
        assertEquals(span.getSpanId(), currentOnCompletion[0]);
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertTrue(span.getEndEpochNanos() - span.getStartEpochNanos() >= 150_000_000L,
                "a reactive hop shows its real duration");
    }

    @Test
    void theSetupUsesTheGivenImplementation() {
        BinaryOperator<String> settings = (key, defaultValue) ->
                Map.of("dynatrace.oneagent.enabled", "true", "otel.forwarding", "true").getOrDefault(key, defaultValue);

        assertNotNull(OtelTracerSetup.configure(sdk, settings));
        assertNotNull(OtelTracerSetup.configure(OpenTelemetry.noop(), (key, defaultValue) -> defaultValue));
    }

    private static Object attribute(SpanData span, String key) {
        return span.getAttributes().asMap().entrySet().stream()
                .filter(e -> e.getKey().getKey().equals(key)).findFirst().orElseThrow().getValue();
    }

    /**
     * A carrier over a plain map.
     */
    static final class MapCarrier implements InvocationCarrier {
        private final Map<String, String> tags = new HashMap<>();

        MapCarrier(Map<String, String> initial) {
            tags.putAll(initial);
        }

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
}
