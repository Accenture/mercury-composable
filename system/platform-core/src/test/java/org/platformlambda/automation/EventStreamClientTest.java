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

package org.platformlambda.automation;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.EventStreamWriter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Progressive SSE consumption in AsyncHttpClient (raw mode): a request that
 * declares Accept: text/event-stream and carries a reply_to receives one
 * x-event-stream data envelope per upstream SSE event, then eof - the same
 * producer contract the HTTP edge consumes. Everything else keeps the
 * buffered single-shot behavior.
 */
class EventStreamClientTest extends TestBase {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final AtomicInteger seq = new AtomicInteger(0);
    private static HttpServer mockSse;
    private static int mockPort;
    private static String edgePort;

    @EventInterceptor
    private record CaptureRoute(BlockingQueue<EventEnvelope> received) implements LambdaFunction {
        @Override
        public Object handleEvent(Map<String, String> headers, Object input, int instance) {
            received.add((EventEnvelope) input);
            return null;
        }
    }

    @BeforeAll
    static void startMockUpstream() throws InterruptedException {
        var config = AppConfigReader.getInstance();
        edgePort = config.getProperty("rest.server.port", config.getProperty("server.port", "8100"));
        var latch = new CountDownLatch(1);
        mockSse = Platform.getInstance().getVertx().createHttpServer();
        mockSse.requestHandler(request -> {
            HttpServerResponse response = request.response();
            String path = request.path();
            response.putHeader("content-type", "text/event-stream");
            response.setChunked(true);
            var vertx = Platform.getInstance().getVertx();
            switch (path) {
                case "/sse/silent" -> // one event, then silence (no pings, never ends)
                        response.write("data: one\n\n");
                case "/sse/abort" -> {
                    // one event, then the connection dies mid-stream
                    response.write("data: partial\n\n");
                    vertx.setTimer(200, t -> request.connection().close());
                }
                case "/sse/comments" -> {
                    // quiet for ~2.5s but alive: keep-alive comments every 300ms,
                    // then a final event and a clean end
                    response.write("data: early\n\n");
                    for (int i = 1; i <= 8; i++) {
                        vertx.setTimer(300L * i, t -> {
                            if (!response.ended()) {
                                response.write(": ping\n\n");
                            }
                        });
                    }
                    vertx.setTimer(2600, t -> {
                        response.write("data: late\n\n");
                        response.end();
                    });
                }
                case "/sse/burst" -> {
                    // 50 unpaced events - FIFO must be preserved through the client
                    for (int i = 1; i <= 50; i++) {
                        response.write("data: item-" + i + "\n\n");
                    }
                    response.end();
                }
                case "/sse/multifield" -> {
                    // multi-line data, named event, id/retry fields to be ignored
                    response.write("event: tokens\nid: 7\nretry: 1000\ndata: line1\ndata: line2\n\n");
                    response.end();
                }
                default -> {
                    response.setStatusCode(404);
                    response.end();
                }
            }
        }).listen(0).onSuccess(server -> {
            mockPort = server.actualPort();
            latch.countDown();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "mock SSE upstream must start");
    }

    @AfterAll
    static void stopMockUpstream() {
        if (mockSse != null) {
            mockSse.close();
        }
    }

    /**
     * Invoke async.http.request with Accept: text/event-stream and a capture
     * route as reply_to; return the captured envelopes up to and including the
     * first terminal marker (eof or exception).
     */
    private List<EventEnvelope> consume(String targetHost, String url, Map<String, String> query,
                                        int timeoutSeconds, int expectedEvents) throws InterruptedException {
        var platform = Platform.getInstance();
        var captured = new LinkedBlockingQueue<EventEnvelope>();
        String route = "capture.sse.client." + seq.incrementAndGet();
        platform.registerPrivate(route, new CaptureRoute(captured), 1);
        try {
            AsyncHttpRequest req = new AsyncHttpRequest();
            req.setMethod("GET");
            req.setTargetHost(targetHost);
            req.setUrl(url);
            if (query != null) {
                query.forEach(req::setQueryParameter);
            }
            req.setHeader("accept", "text/event-stream");
            req.setTimeoutSeconds(timeoutSeconds);
            EventEmitter.getInstance().send(new EventEnvelope()
                    .setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req.toMap())
                    .setReplyTo(route).setCorrelationId("cid-" + seq.get()));
            List<EventEnvelope> events = new ArrayList<>();
            for (int i = 0; i < expectedEvents; i++) {
                EventEnvelope event = captured.poll(20, TimeUnit.SECONDS);
                assertNotNull(event, "expected segment " + (i + 1) + " of " + expectedEvents);
                events.add(event);
                String marker = event.getHeaders().get(EventStreamWriter.X_EVENT_STREAM);
                if (EventStreamWriter.EOF.equals(marker) || EventStreamWriter.EXCEPTION.equals(marker)) {
                    break;
                }
            }
            return events;
        } finally {
            platform.release(route);
        }
    }

    private String marker(EventEnvelope event) {
        return event.getHeaders().get(EventStreamWriter.X_EVENT_STREAM);
    }

    @Test
    void rawSseEventsMapToDataEnvelopesWithTerminalEof() throws InterruptedException {
        // the app's own SSE demo endpoint is the upstream
        var events = consume("http://127.0.0.1:" + edgePort, "/api/hello/stream",
                Map.of("mode", "sse"), 10, 4);
        assertEquals(4, events.size(), "3 data envelopes + eof");
        EventEnvelope first = events.getFirst();
        assertEquals(EventStreamWriter.DATA, marker(first));
        // head control rides the first envelope: upstream status + SSE content type
        assertEquals(200, first.getStatus());
        assertEquals("text/event-stream", first.getHeaders().get("content-type"));
        assertEquals("Hello", first.getRawBody());
        assertEquals("token stream", events.get(1).getRawBody());
        // the upstream's terminal SSE frame arrives as a NAMED data event - the
        // client does not interpret payloads (D3); its own eof marks the real end
        EventEnvelope done = events.get(2);
        assertEquals(EventStreamWriter.DATA, marker(done));
        assertEquals("done", done.getHeaders().get(EventStreamWriter.X_EVENT_NAME));
        assertEquals("{\"segments\":2}", done.getRawBody());
        assertEquals(EventStreamWriter.EOF, marker(events.get(3)));
    }

    @Test
    void multiFieldFramesMapPerSseSpecification() throws InterruptedException {
        var events = consume("http://127.0.0.1:" + mockPort, "/sse/multifield", null, 5, 2);
        EventEnvelope data = events.getFirst();
        assertEquals(EventStreamWriter.DATA, marker(data));
        assertEquals("tokens", data.getHeaders().get(EventStreamWriter.X_EVENT_NAME));
        assertEquals("line1\nline2", data.getRawBody(), "multi-line data joins with newline");
        assertEquals(EventStreamWriter.EOF, marker(events.get(1)));
    }

    @Test
    void burstEventsArriveInStrictFifoOrder() throws InterruptedException {
        var events = consume("http://127.0.0.1:" + mockPort, "/sse/burst", null, 10, 51);
        assertEquals(51, events.size(), "50 data envelopes + eof");
        for (int i = 1; i <= 50; i++) {
            assertEquals("item-" + i, events.get(i - 1).getRawBody(), "strict FIFO");
        }
        assertEquals(EventStreamWriter.EOF, marker(events.get(50)));
    }

    @Test
    void idleStallFailsInBandWithTimeout408() throws InterruptedException {
        var events = consume("http://127.0.0.1:" + mockPort, "/sse/silent", null, 2, 3);
        assertEquals("one", events.getFirst().getRawBody());
        EventEnvelope error = events.get(1);
        assertEquals(EventStreamWriter.EXCEPTION, marker(error));
        assertEquals(408, error.getStatus());
        assertInstanceOf(Map.class, error.getRawBody());
        // the standard error key-values: '{"type": "error", "status": n, "message": text}'
        Map<?, ?> body = (Map<?, ?>) error.getRawBody();
        assertEquals("error", body.get("type"));
        assertEquals("Timeout for 2 seconds", body.get("message"));
    }

    @Test
    void keepAliveCommentsResetTheIdleAllowance() throws InterruptedException {
        // quiet for 2.5s with 300ms comments under a 2s idle allowance:
        // the comments prove liveness, so the stream must complete
        var events = consume("http://127.0.0.1:" + mockPort, "/sse/comments", null, 2, 3);
        assertEquals(3, events.size());
        assertEquals("early", events.getFirst().getRawBody());
        assertEquals("late", events.get(1).getRawBody());
        assertEquals(EventStreamWriter.EOF, marker(events.get(2)));
    }

    @Test
    void midStreamDisconnectFailsInBand() throws InterruptedException {
        var events = consume("http://127.0.0.1:" + mockPort, "/sse/abort", null, 10, 3);
        assertEquals("partial", events.getFirst().getRawBody());
        EventEnvelope error = events.get(1);
        assertEquals(EventStreamWriter.EXCEPTION, marker(error));
        assertEquals(500, error.getStatus());
    }

    @Test
    void nonSseUpstreamFallsBackToBufferedSingleShot() throws InterruptedException {
        // Accept opted in, but the upstream answers JSON - one unmarked reply
        var platform = Platform.getInstance();
        var captured = new LinkedBlockingQueue<EventEnvelope>();
        String route = "capture.sse.fallback";
        platform.registerPrivate(route, new CaptureRoute(captured), 1);
        try {
            AsyncHttpRequest req = new AsyncHttpRequest();
            req.setMethod("GET");
            req.setTargetHost("http://127.0.0.1:" + edgePort);
            req.setUrl("/api/hello/world");
            req.setHeader("accept", "text/event-stream");
            req.setTimeoutSeconds(10);
            EventEmitter.getInstance().send(new EventEnvelope()
                    .setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req.toMap())
                    .setReplyTo(route).setCorrelationId("cid-fallback"));
            EventEnvelope reply = captured.poll(20, TimeUnit.SECONDS);
            assertNotNull(reply);
            assertNull(reply.getHeaders().get(EventStreamWriter.X_EVENT_STREAM),
                    "a buffered reply carries no stream marker");
            assertInstanceOf(Map.class, reply.getRawBody());
            assertNull(captured.poll(500, TimeUnit.MILLISECONDS), "single-shot means one reply");
        } finally {
            platform.release(route);
        }
    }

    @Test
    void withoutAcceptTheSseResponseBuffersAsBefore() {
        // backward-compat pin: an RPC without Accept: text/event-stream receives
        // the whole SSE payload buffered as one text body (today's behavior)
        var po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setTargetHost("http://127.0.0.1:" + edgePort);
        req.setUrl("/api/hello/stream");
        req.setQueryParameter("mode", "sse");
        req.setTimeoutSeconds(10);
        EventEnvelope response = assertDoesNotThrow(() -> po.asyncRequest(new EventEnvelope()
                        .setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req.toMap()), 15000)
                .toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS));
        assertEquals(200, response.getStatus());
        String text = String.valueOf(response.getRawBody());
        assertTrue(text.contains("data: Hello"), text);
        assertTrue(text.contains("event: done"), text);
    }

    @Test
    void selfRelayStreamsProgressivelyOutTheEdge() throws IOException, InterruptedException {
        // the flagship composition: /api/hello/relay forwards its reply lane into
        // async.http.request aimed at this app's own SSE endpoint - upstream
        // frames re-render at the edge, followed by the relay's own terminal
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + edgePort + "/api/hello/relay"))
                .timeout(Duration.ofSeconds(20)).header("Accept", "text/event-stream").build();
        long started = System.currentTimeMillis();
        HttpResponse<java.util.stream.Stream<String>> response =
                client.send(request, HttpResponse.BodyHandlers.ofLines());
        assertEquals(200, response.statusCode());
        assertEquals("text/event-stream", response.headers().firstValue("content-type").orElse(""));
        List<String> lines = new ArrayList<>();
        List<Long> arrivals = new ArrayList<>();
        response.body().forEach(line -> {
            lines.add(line);
            arrivals.add(System.currentTimeMillis() - started);
        });
        int hello = lines.indexOf("data: Hello");
        int tokens = lines.indexOf("data: token stream");
        // the upstream's done event re-renders as a NAMED frame with its metadata
        int upstreamDone = lines.indexOf("event: done");
        assertTrue(hello >= 0 && tokens > hello && upstreamDone > tokens, "ordered relay: " + lines);
        assertTrue(lines.contains("data: {\"segments\":2}"), lines.toString());
        // the relay's own eof renders one final terminal event
        long doneCount = lines.stream().filter("event: done"::equals).count();
        assertEquals(2, doneCount, "upstream done + relay terminal: " + lines);
        // progressive end to end: the upstream paces segments 250ms apart, so a buffered
        // relay would deliver every line in one flush (~0ms apart). Assert the largest
        // consecutive-arrival gap, not the first-to-last span: a reader that starts late
        // on a loaded runner observes the already-queued frames coalesced, which
        // compresses the span (CI flake 2026-09-02: 127ms observed against a 300ms
        // floor) but can only erase every gap by missing the whole production window.
        long maxGap = 0;
        for (int i = hello + 1; i < arrivals.size(); i++) {
            maxGap = Math.max(maxGap, arrivals.get(i) - arrivals.get(i - 1));
        }
        assertTrue(maxGap >= 100, "progressive relay expected, max arrival gap " + maxGap
                + " ms, arrivals " + arrivals);
        Utility.getInstance().sleep(100);
    }
}
