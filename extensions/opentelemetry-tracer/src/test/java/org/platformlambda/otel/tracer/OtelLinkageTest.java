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

import com.accenture.adapters.FlowExecutor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live spans through real Mercury dispatch, recorded by an OpenTelemetry SDK installed as the global
 * implementation (as an agent would): one connected tree per request, across the REST edge, an RPC, Event
 * Script's task executor and a deferred Mono result. The negative control proves the check can fail.
 */
class OtelLinkageTest {
    private static final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private static final long TIMEOUT = 8000;

    @BeforeAll
    static void boot() throws InterruptedException {
        // installed before the platform starts, so OtelTracerSetup picks it up from GlobalOpenTelemetry
        GlobalOpenTelemetry.set(OpenTelemetrySdk.builder().setTracerProvider(SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()).build());
        AutoStart.main(new String[0]);
        BlockingQueue<Boolean> ready = new ArrayBlockingQueue<>(1);
        Platform.getInstance().waitForProvider(AsyncHttpClient.ASYNC_HTTP_RESPONSE, 20).onSuccess(ready::add);
        assertEquals(Boolean.TRUE, ready.poll(20, TimeUnit.SECONDS), "REST automation did not start");
    }

    @Test
    void anHttpRequestIsOneConnectedTree() throws Exception {
        int port = Utility.getInstance().str2int(AppConfigReader.getInstance().getProperty("server.port", "8306"));
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/demo/hello")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        SpanData hello = await("demo.hello", null);
        SpanData echo = await("demo.echo", hello.getTraceId());
        assertEquals(hello.getSpanId(), echo.getParentSpanId());
    }

    @Test
    void aFlowIsOneTreeThroughTheTaskExecutor() throws Exception {
        Utility util = Utility.getInstance();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("body", Map.of("hello", "world"));
        Span request = GlobalOpenTelemetry.getTracer("test").spanBuilder("flow-request").startSpan();
        String traceId = request.getSpanContext().getTraceId();
        try (Scope ignored = request.makeCurrent()) {
            EventEnvelope result = FlowExecutor.getInstance()
                    .request("unit.test", util.getUuid(), "TEST /api/flow", "otel-tracer-demo", dataset,
                            util.getUuid(), TIMEOUT).get(TIMEOUT, TimeUnit.MILLISECONDS);
            assertEquals(200, result.getStatus());
        }
        request.end();

        SpanData task1 = await("demo.task.1", traceId);
        SpanData task2 = await("demo.task.2", traceId);
        // every span of the flow belongs to the request's trace, and the task spans hang under the routers
        assertEquals("task.executor", nameOf(task1.getParentSpanId()));
        assertEquals("task.executor", nameOf(task2.getParentSpanId()));
        assertNotNull(await("event.script.manager", traceId));
    }

    @Test
    void aDeferredResultShowsItsRealDuration() throws Exception {
        EventEnvelope response = EventEmitter.getInstance().eRequest(new EventEnvelope().setTo("demo.mono")
                .setBody("news").setTrace(Utility.getInstance().getUuid(), "TEST /api/mono"), TIMEOUT)
                .get(TIMEOUT, TimeUnit.MILLISECONDS);

        assertEquals("late news", response.getBody());
        SpanData mono = await("demo.mono", null);
        assertTrue(mono.getEndEpochNanos() - mono.getStartEpochNanos() >= 200_000_000L,
                "the Mono's span must end when the result is delivered");
    }

    @Test
    void theNegativeControlStartsANewTrace() throws Exception {
        Span request = GlobalOpenTelemetry.getTracer("test").spanBuilder("unlinked-request").startSpan();
        String traceId = request.getSpanContext().getTraceId();
        try (Scope ignored = request.makeCurrent()) {
            for (String route : List.of("demo.echo", "demo.unlinked")) {
                EventEmitter.getInstance().eRequest(new EventEnvelope().setTo(route).setBody("x")
                        .setTrace(Utility.getInstance().getUuid(), "TEST /api/unlinked"), TIMEOUT)
                        .get(TIMEOUT, TimeUnit.MILLISECONDS);
            }
        }
        request.end();

        // the linked route joins the caller's trace; the unlinked one does not, by design
        assertEquals(request.getSpanContext().getSpanId(), await("demo.echo", traceId).getParentSpanId());
        Utility.getInstance().sleep(300);
        assertTrue(spans().stream().noneMatch(s -> s.getName().equals("demo.unlinked") && s.getTraceId().equals(traceId)));
        assertNotNull(await("demo.unlinked", null));
    }

    private static List<SpanData> spans() {
        return exporter.getFinishedSpanItems();
    }

    private static String nameOf(String spanId) {
        return spans().stream().filter(s -> s.getSpanId().equals(spanId)).map(SpanData::getName)
                .findFirst().orElse(null);
    }

    private static SpanData await(String name, String traceId) {
        for (int i = 0; i < 100; i++) {
            for (SpanData span : spans()) {
                if (span.getName().equals(name) && (traceId == null || span.getTraceId().equals(traceId))) {
                    return span;
                }
            }
            Utility.getInstance().sleep(50);
        }
        throw new AssertionError("no span " + name + (traceId == null? "" : " in trace " + traceId) + ": "
                + spans().stream().map(s -> s.getName() + "/" + s.getTraceId()).toList());
    }
}
