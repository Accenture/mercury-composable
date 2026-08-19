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

package org.platformlambda.opentelemetry.support;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the widened export retry against the exact transport failure that dropped a span in CI:
 * the collector end closes the connection after reading the request but before answering, so the
 * client sees a bare EOF - a plain {@link IOException} ("unexpected end of stream") that the
 * OpenTelemetry SDK's <b>default</b> retry predicate does NOT retry (its whitelist covers only
 * connect/socket-timeout, UnknownHost and SocketException). With the widened predicate the second
 * attempt lands on a healthy connection and the span survives.
 * <p>
 * Mutation check: removing {@code setRetryPolicy(...)} from
 * {@link OtelForwarderContext#buildExporter} makes this test fail on the first-attempt drop.
 */
class OtlpExportRetryTest {
    // HTTP requires CRLF line endings: the \r escapes pair with the text block's own \n
    private static final String OTLP_200 = """
            HTTP/1.1 200 OK\r
            content-type: application/x-protobuf\r
            content-length: 0\r
            connection: close\r
            \r
            """;

    @Test
    void widenedRetryRecoversTheSpanWhenTheServerKillsTheFirstConnection() throws Exception {
        try (FlakyOtlpServer server = new FlakyOtlpServer()) {
            String endpoint = "http://127.0.0.1:" + server.port() + "/v1/traces";
            try (SpanExporter exporter = OtelForwarderContext.buildExporter(endpoint, 10_000, Map.of())) {
                CompletableResultCode rc = exporter.export(List.of(sampleSpan()));
                // first attempt fails on the killed connection; the retry backoff starts at ~1s
                rc.join(20, TimeUnit.SECONDS);
                assertTrue(rc.isSuccess(),
                        "the widened retry must recover a span from a killed connection "
                                + "instead of dropping it");
                assertTrue(server.connections() >= 2,
                        "success requires a SECOND connection - the first was deliberately killed "
                                + "after reading the request (got " + server.connections() + ")");
            }
        }
    }

    /**
     * The second failure class from CI: the sender's managed dispatcher runs a zero-queue pool
     * whose {@code execute()} REJECTS during transient full-occupancy races
     * ({@code InterruptedIOException: executor rejected}) - and a rejected call never reaches the
     * retry interceptor, so no retry policy can save it. With the exporter's own unbounded-queue
     * pool, a burst far wider than the pool must queue and deliver every span, never reject.
     */
    @Test
    void saturationBurstQueuesEveryExportInsteadOfRejecting() throws Exception {
        try (HealthyOtlpServer server = new HealthyOtlpServer()) {
            String endpoint = "http://127.0.0.1:" + server.port() + "/v1/traces";
            try (SpanExporter exporter = OtelForwarderContext.buildExporter(endpoint, 10_000, Map.of())) {
                List<CompletableResultCode> results = new ArrayList<>();
                for (int i = 0; i < 16; i++) {
                    results.add(exporter.export(List.of(sampleSpan())));
                }
                CompletableResultCode all = CompletableResultCode.ofAll(results);
                all.join(30, TimeUnit.SECONDS);
                assertTrue(all.isSuccess(), "a burst wider than the export pool must queue, never "
                        + "be rejected at enqueue");
                assertTrue(server.requests() >= 16,
                        "every span of the burst must reach the collector (got "
                                + server.requests() + ")");
            }
        }
    }

    /**
     * Closing the exporter must also stop its export pool - tests build many exporters.
     * Baseline-relative: the app's own forwarder (booted by sibling test classes in this JVM)
     * runs identically-named threads, so the pin counts only the DELTA this test creates.
     */
    @Test
    void closingTheExporterStopsItsExportThreads() throws Exception {
        try (HealthyOtlpServer server = new HealthyOtlpServer()) {
            String endpoint = "http://127.0.0.1:" + server.port() + "/v1/traces";
            long baseline = liveExportThreads();
            SpanExporter exporter = OtelForwarderContext.buildExporter(endpoint, 10_000, Map.of());
            exporter.export(List.of(sampleSpan())).join(10, TimeUnit.SECONDS);
            assertTrue(liveExportThreads() > baseline,
                    "an active exporter runs at least one export thread of its own");
            exporter.close();
            long deadline = System.currentTimeMillis() + 5000;
            while (liveExportThreads() > baseline && System.currentTimeMillis() < deadline) {
                Utility.getInstance().sleep(50);
            }
            assertTrue(liveExportThreads() <= baseline,
                    "the export pool must shut down with the exporter (threads leaked)");
        }
    }

    private long liveExportThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(t -> "otlp-export".equals(t.getName()))
                .count();
    }

    private SpanData sampleSpan() {
        Map<String, Object> trace = new HashMap<>();
        trace.put("id", "4bf92f3577b34da6a3ce929d0e0e4736");
        trace.put("span_id", "00f067aa0ba902b7");
        trace.put("service", "retry.pin");
        trace.put("start", "2026-06-24T10:00:00Z");
        trace.put("exec_time", 1.0);
        trace.put("success", true);
        Map<String, Object> ds = new HashMap<>();
        ds.put("trace", trace);
        Resource resource = Resource.create(
                Attributes.of(AttributeKey.stringKey("service.name"), "mercury-otel-demo"));
        return TraceMetricsSpanData.map(ds, resource, InstrumentationScopeInfo.create("test"));
    }

    /**
     * A minimal raw-socket HTTP endpoint that fully reads each request, then KILLS the first
     * connection without a response (the client observes EOF where the status line should be)
     * and answers an empty OTLP 200 on every later connection.
     */
    private static final class FlakyOtlpServer implements AutoCloseable {
        private final ServerSocket server;
        private final AtomicInteger connections = new AtomicInteger();
        private volatile boolean running = true;

        FlakyOtlpServer() throws IOException {
            this.server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread acceptor = new Thread(this::acceptLoop, "flaky-otlp-server");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return server.getLocalPort();
        }

        int connections() {
            return connections.get();
        }

        private void acceptLoop() {
            while (running) {
                try (Socket socket = server.accept()) {
                    int n = connections.incrementAndGet();
                    drainRequest(socket.getInputStream());
                    if (n > 1) {
                        OutputStream out = socket.getOutputStream();
                        out.write(OTLP_200.getBytes(StandardCharsets.US_ASCII));
                        out.flush();
                    }
                    // n == 1: close without responding - the deliberate mid-exchange kill
                } catch (IOException e) {
                    // server socket closed on shutdown, or a client went away - both fine
                }
            }
        }

        @Override
        public void close() throws IOException {
            running = false;
            server.close();
        }
    }

    /**
     * A raw-socket OTLP endpoint that answers every request with an empty 200 and counts them -
     * the healthy counterpart of {@link FlakyOtlpServer} for the saturation and lifecycle pins.
     */
    private static final class HealthyOtlpServer implements AutoCloseable {
        private final ServerSocket server;
        private final AtomicInteger requests = new AtomicInteger();
        private volatile boolean running = true;

        HealthyOtlpServer() throws IOException {
            this.server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread acceptor = new Thread(this::acceptLoop, "healthy-otlp-server");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return server.getLocalPort();
        }

        int requests() {
            return requests.get();
        }

        private void acceptLoop() {
            while (running) {
                try (Socket socket = server.accept()) {
                    drainRequest(socket.getInputStream());
                    requests.incrementAndGet();
                    OutputStream out = socket.getOutputStream();
                    out.write(OTLP_200.getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                } catch (IOException e) {
                    // server socket closed on shutdown, or a client went away - both fine
                }
            }
        }

        @Override
        public void close() throws IOException {
            running = false;
            server.close();
        }
    }

    /** Read the full request (headers, then content-length body) so the client is
     *  waiting on the RESPONSE after the server has consumed its input. */
    private static void drainRequest(InputStream in) throws IOException {
            StringBuilder head = new StringBuilder();
            int prev3 = -1;
            int prev2 = -1;
            int prev1 = -1;
            int b;
            while ((b = in.read()) != -1) {
                head.append((char) b);
                if (prev3 == '\r' && prev2 == '\n' && prev1 == '\r' && b == '\n') {
                    break;
                }
                prev3 = prev2;
                prev2 = prev1;
                prev1 = b;
            }
            int contentLength = 0;
            for (String line : head.toString().split("\r\n")) {
                String lower = line.toLowerCase();
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                }
            }
            long remaining = contentLength;
            while (remaining > 0) {
                long skipped = in.skip(remaining);
                if (skipped <= 0) {
                    if (in.read() == -1) {
                        break;
                    }
                    skipped = 1;
                }
                remaining -= skipped;
            }
    }
}
