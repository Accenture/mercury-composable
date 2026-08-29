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
import org.platformlambda.automation.services.EventStreamRenderer;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.annotations.EventInterceptor;
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
 * Event-over-HTTP peer streaming (envelope mode): a send with reply_to that
 * declares the "accept: text/event-stream" event header relays a remote
 * streaming function's segments progressively to the caller's reply route.
 * The wire is the hybrid dialect - envelope frames (base64 MsgPack) for the
 * head, the terminals and non-text segments; raw SSE frames for text tokens.
 * A non-streaming target and every existing calling mode stay byte-identical.
 */
class EventOverHttpStreamTest extends TestBase {
    private static final String X_EVENT_STREAM = EventStreamWriter.X_EVENT_STREAM;
    private static final String X_EVENT_NAME = EventStreamWriter.X_EVENT_NAME;
    private static final String DATA = EventStreamWriter.DATA;
    private static final String EOF = EventStreamWriter.EOF;
    private static final String EXCEPTION = EventStreamWriter.EXCEPTION;
    private static final String ACCEPT = "accept";
    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final String STREAMING_TARGET = "hello.stream.remote";
    private static final String SINGLE_SHOT_TARGET = "hello.single.remote";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final AtomicInteger seq = new AtomicInteger(0);
    private static HttpServer mockPeer;

    @EventInterceptor
    private record CaptureRoute(BlockingQueue<EventEnvelope> received) implements LambdaFunction {
        @Override
        public Object handleEvent(Map<String, String> headers, Object input, int instance) {
            received.add((EventEnvelope) input);
            return null;
        }
    }

    @BeforeAll
    static void startMisbehavingPeer() throws InterruptedException {
        // a fixed-port mock so the static event-over-http.yaml can address it -
        // it violates the envelope dialect in the exact ways the client must catch
        var util = Utility.getInstance();
        var config = AppConfigReader.getInstance();
        int mockPort = util.str2int(config.getProperty("sse.mock.port", "58586"));
        var latch = new CountDownLatch(1);
        mockPeer = Platform.getInstance().getVertx().createHttpServer();
        mockPeer.requestHandler(request -> {
            HttpServerResponse response = request.response();
            response.putHeader("content-type", TEXT_EVENT_STREAM);
            response.setChunked(true);
            switch (request.path()) {
                case "/mock/raw-first" -> {
                    // the dialect guarantees an envelope frame first
                    response.write("data: hello\n\n");
                    response.end();
                }
                case "/mock/no-terminal" -> {
                    // a clean transport end without a decoded terminal is a truncation
                    response.write(envelopeFrame(new EventEnvelope()
                            .setHeader(X_EVENT_STREAM, DATA)
                            .setHeader("content-type", TEXT_EVENT_STREAM)
                            .setStatus(200).setBody("mock-head")));
                    response.end();
                }
                case "/mock/foreign-dialect" -> {
                    // a conforming foreign peer: envelope head, raw token, envelope eof -
                    // plus a trailing frame that must be discarded after the terminal
                    response.write(envelopeFrame(new EventEnvelope()
                                    .setHeader(X_EVENT_STREAM, DATA)
                                    .setHeader("content-type", TEXT_EVENT_STREAM)
                                    .setStatus(200).setBody("mock-head"))
                            + "data: mock-token\n\n"
                            + envelopeFrame(new EventEnvelope()
                                    .setHeader(X_EVENT_STREAM, EOF).setBody(Map.of("done", true)))
                            + "data: trailing-noise\n\n");
                    response.end();
                }
                default -> {
                    response.setStatusCode(404);
                    response.end();
                }
            }
        }).listen(mockPort).onSuccess(server -> latch.countDown());
        assertTrue(latch.await(10, TimeUnit.SECONDS), "misbehaving-peer mock must start");
    }

    @AfterAll
    static void stopMisbehavingPeer() {
        if (mockPeer != null) {
            mockPeer.close();
        }
    }

    private static String envelopeFrame(EventEnvelope event) {
        return "event: envelope\ndata: " + Utility.getInstance().bytesToBase64(event.toBytes()) + "\n\n";
    }

    /**
     * Send to an event-over-http mapped route with the streaming opt-in and a capture
     * route as reply_to; return the captured envelopes up to and including the first
     * terminal marker (eof or exception) - or the first envelope for single-shot pins.
     */
    private List<EventEnvelope> sendStreaming(String route, String mode, String ttlMs,
                                              int expectedEvents) throws InterruptedException {
        var platform = Platform.getInstance();
        var captured = new LinkedBlockingQueue<EventEnvelope>();
        String capture = "capture.eoh.stream." + seq.incrementAndGet();
        platform.registerPrivate(capture, new CaptureRoute(captured), 1);
        try {
            EventEnvelope event = new EventEnvelope().setTo(route)
                    .setReplyTo(capture).setCorrelationId("cid-eoh-" + seq.get())
                    .setHeader(ACCEPT, TEXT_EVENT_STREAM);
            if (mode != null) {
                event.setHeader("mode", mode);
            }
            if (ttlMs != null) {
                event.setHeader("x-ttl", ttlMs);
            }
            EventEmitter.getInstance().send(event);
            List<EventEnvelope> events = new ArrayList<>();
            for (int i = 0; i < expectedEvents; i++) {
                EventEnvelope received = captured.poll(20, TimeUnit.SECONDS);
                assertNotNull(received, "expected event " + (i + 1) + " of " + expectedEvents);
                events.add(received);
                String marker = marker(received);
                if (EOF.equals(marker) || EXCEPTION.equals(marker)) {
                    break;
                }
            }
            return events;
        } finally {
            platform.release(capture);
        }
    }

    private String marker(EventEnvelope event) {
        return event.getHeaders().get(X_EVENT_STREAM);
    }

    @Test
    void streamingTargetRelaysProgressivelyToCallback() throws InterruptedException {
        var events = sendStreaming(STREAMING_TARGET, "tokens", null, 3);
        assertEquals(3, events.size(), "2 data envelopes + eof");
        // the decoded head is the target's first envelope, verbatim
        EventEnvelope head = events.getFirst();
        assertEquals(DATA, marker(head));
        assertEquals(200, head.getStatus());
        assertEquals(TEXT_EVENT_STREAM, head.getHeaders().get("content-type"));
        assertEquals("alpha", head.getRawBody());
        assertEquals("cid-eoh-" + seq.get(), head.getCorrelationId(), "original correlation id restored");
        // the second token rode a raw frame and was synthesized back
        EventEnvelope token = events.get(1);
        assertEquals(DATA, marker(token));
        assertEquals("beta", token.getRawBody());
        // eof carries the trailing metadata with its exact Map type
        EventEnvelope eof = events.get(2);
        assertEquals(EOF, marker(eof));
        assertInstanceOf(Map.class, eof.getRawBody());
        assertEquals("2", String.valueOf(((Map<?, ?>) eof.getRawBody()).get("segments")));
    }

    @Test
    void typedSegmentsRoundTripExactly() throws InterruptedException {
        // every escape-hatch trigger rides an envelope frame and keeps its exact form
        var events = sendStreaming(STREAMING_TARGET, "typed", null, 6);
        assertEquals(6, events.size(), "5 data envelopes + eof");
        EventEnvelope mapSegment = events.getFirst();
        assertInstanceOf(Map.class, mapSegment.getRawBody());
        assertEquals("1", String.valueOf(((Map<?, ?>) mapSegment.getRawBody()).get("n")));
        EventEnvelope crlf = events.get(1);
        assertEquals("crlf", crlf.getHeaders().get(X_EVENT_NAME));
        assertEquals("line1\r\nline2", crlf.getRawBody(), "carriage return preserved");
        EventEnvelope reservedName = events.get(2);
        assertEquals("envelope", reservedName.getHeaders().get(X_EVENT_NAME),
                "a user event name colliding with the reserved word survives");
        assertEquals("reserved-name", reservedName.getRawBody());
        EventEnvelope bytes = events.get(3);
        assertInstanceOf(byte[].class, bytes.getRawBody());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, (byte[]) bytes.getRawBody());
        assertEquals("plain token", events.get(4).getRawBody(), "text segment rides a raw frame");
        EventEnvelope eof = events.get(5);
        assertEquals(EOF, marker(eof));
        assertEquals("true", String.valueOf(((Map<?, ?>) eof.getRawBody()).get("done")));
    }

    @Test
    void singleShotTargetOverCapablePathIsClassic() throws InterruptedException {
        // the capable path against a non-streaming function delivers exactly the
        // classic callback reply - the buffered fallback decode
        var platform = Platform.getInstance();
        var captured = new LinkedBlockingQueue<EventEnvelope>();
        String capture = "capture.eoh.single." + seq.incrementAndGet();
        platform.registerPrivate(capture, new CaptureRoute(captured), 1);
        try {
            // classic callback (no accept header) as the baseline
            EventEmitter.getInstance().send(new EventEnvelope().setTo(SINGLE_SHOT_TARGET)
                    .setReplyTo(capture).setCorrelationId("cid-classic").setBody("ping-1"));
            EventEnvelope classic = captured.poll(20, TimeUnit.SECONDS);
            assertNotNull(classic);
            // streaming-capable call to the same target
            EventEmitter.getInstance().send(new EventEnvelope().setTo(SINGLE_SHOT_TARGET)
                    .setReplyTo(capture).setCorrelationId("cid-capable").setBody("ping-1")
                    .setHeader(ACCEPT, TEXT_EVENT_STREAM));
            EventEnvelope capable = captured.poll(20, TimeUnit.SECONDS);
            assertNotNull(capable);
            assertNull(marker(capable), "a single-shot reply carries no stream marker");
            assertEquals(classic.getStatus(), capable.getStatus());
            assertEquals(classic.getRawBody(), capable.getRawBody());
            assertEquals("cid-capable", capable.getCorrelationId());
            assertNull(captured.poll(500, TimeUnit.MILLISECONDS), "single-shot means one reply");
        } finally {
            platform.release(capture);
        }
    }

    @Test
    void streamingTargetWithoutAcceptIsRefused406() throws InterruptedException {
        // classic callback mode (no accept opt-in) against a streaming function:
        // the peer answers with an explicit refusal instead of a truncated reply
        var platform = Platform.getInstance();
        var captured = new LinkedBlockingQueue<EventEnvelope>();
        String capture = "capture.eoh.refuse." + seq.incrementAndGet();
        platform.registerPrivate(capture, new CaptureRoute(captured), 1);
        try {
            EventEmitter.getInstance().send(new EventEnvelope().setTo(STREAMING_TARGET)
                    .setReplyTo(capture).setCorrelationId("cid-refuse")
                    .setHeader("mode", "tokens"));
            EventEnvelope reply = captured.poll(20, TimeUnit.SECONDS);
            assertNotNull(reply);
            assertEquals(406, reply.getStatus());
            assertEquals("Streaming function requires a caller that accepts text/event-stream",
                    reply.getError());
        } finally {
            platform.release(capture);
        }
    }

    @Test
    void streamingTargetViaRpcIsRefused406() {
        // the RPC path never streams (a Future completes once)
        var po = EventEmitter.getInstance();
        EventEnvelope response = assertDoesNotThrow(() -> po.asyncRequest(
                        new EventEnvelope().setTo(STREAMING_TARGET).setHeader("mode", "tokens"), 15000)
                .toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS));
        assertEquals(406, response.getStatus());
        assertEquals("Streaming function requires a caller that accepts text/event-stream",
                response.getError());
    }

    @Test
    void midStreamFailurePropagatesExactStatus() throws InterruptedException {
        var events = sendStreaming(STREAMING_TARGET, "error-mid", null, 3);
        assertEquals("partial", events.getFirst().getRawBody());
        EventEnvelope error = events.get(1);
        assertEquals(EXCEPTION, marker(error));
        assertEquals(503, error.getStatus());
        assertInstanceOf(Map.class, error.getRawBody());
        // the standard error key-values: '{"type": "error", "status": n, "message": text}'
        Map<?, ?> body = (Map<?, ?>) error.getRawBody();
        assertEquals("error", body.get("type"));
        assertEquals("503", String.valueOf(body.get("status")));
        assertEquals("backend on fire", body.get("message"));
    }

    @Test
    void failureBeforeFirstSegmentArrivesAsException() throws InterruptedException {
        // a pre-head failure still rides the stream (SSE-uniform) - the caller
        // receives the exact error envelope
        var events = sendStreaming(STREAMING_TARGET, "error-first", null, 1);
        EventEnvelope error = events.getFirst();
        assertEquals(EXCEPTION, marker(error));
        assertEquals(503, error.getStatus());
        assertEquals("no backend", ((Map<?, ?>) error.getRawBody()).get("message"));
    }

    @Test
    void idleStallFailsInBand408() throws InterruptedException {
        // the target declares a one-second idle allowance and goes silent - the server
        // housekeeper (10s sweep) or the client's own idle timer must fail it in-band;
        // both produce the pinned 408 wording
        long started = System.currentTimeMillis();
        var events = sendStreaming(STREAMING_TARGET, "stall", "5000", 2);
        long elapsed = System.currentTimeMillis() - started;
        assertEquals("one", events.getFirst().getRawBody());
        EventEnvelope error = events.get(1);
        assertEquals(EXCEPTION, marker(error));
        assertEquals(408, error.getStatus());
        Map<?, ?> body = (Map<?, ?>) error.getRawBody();
        assertEquals("error", body.get("type"));
        String message = String.valueOf(body.get("message"));
        assertTrue(message.startsWith("Timeout for "), message);
        assertTrue(elapsed < 15000, "in-band timeout expected within one sweep, took " + elapsed + " ms");
    }

    @Test
    void serverPoolExhaustionAnswers503() throws InterruptedException {
        // with no reply lane available, a streaming-capable call is refused
        // single-shot with the pinned message - and recovers after release
        List<String> drained = new ArrayList<>();
        String lane;
        while ((lane = EventStreamRenderer.checkoutLane()) != null) {
            drained.add(lane);
        }
        assertFalse(drained.isEmpty(), "the pool should have lanes to drain");
        try {
            var events = sendStreaming(STREAMING_TARGET, "tokens", null, 1);
            EventEnvelope reply = events.getFirst();
            assertEquals(503, reply.getStatus());
            assertEquals("Streaming response pool exhausted", reply.getError());
        } finally {
            drained.forEach(EventStreamRenderer::releaseLane);
        }
        // capacity restored - the same call streams normally again
        var events = sendStreaming(STREAMING_TARGET, "tokens", null, 3);
        assertEquals(EOF, marker(events.get(2)));
    }

    @Test
    void restLevelErrorUnwrapsToCallback() throws InterruptedException {
        // the relay POSTs to a GET-only endpoint: the edge answers a REST error
        // (JSON, not a packed envelope) - the client unwraps it classically
        var events = sendStreaming("mock.rest.error", null, null, 1);
        EventEnvelope reply = events.getFirst();
        assertNull(marker(reply), "an edge error is a single-shot reply");
        assertEquals(405, reply.getStatus());
        assertEquals("Method not allowed", reply.getError());
    }

    @Test
    void rawFirstFrameFromForeignServerIsRejected() throws InterruptedException {
        var events = sendStreaming("mock.sse.raw.first", null, null, 1);
        EventEnvelope error = events.getFirst();
        assertEquals(EXCEPTION, marker(error));
        assertEquals(500, error.getStatus());
        assertEquals("Invalid event stream - missing envelope head",
                ((Map<?, ?>) error.getRawBody()).get("message"));
    }

    @Test
    void transportEndWithoutTerminalIsTruncation() throws InterruptedException {
        var events = sendStreaming("mock.sse.no.terminal", null, null, 2);
        assertEquals("mock-head", events.getFirst().getRawBody());
        EventEnvelope error = events.get(1);
        assertEquals(EXCEPTION, marker(error));
        assertEquals(500, error.getStatus());
        assertEquals("Event stream ended without eof",
                ((Map<?, ?>) error.getRawBody()).get("message"));
    }

    @Test
    void foreignDialectPeerWorksAndTrailingFramesDrop() throws InterruptedException {
        var platform = Platform.getInstance();
        var captured = new LinkedBlockingQueue<EventEnvelope>();
        String capture = "capture.eoh.foreign." + seq.incrementAndGet();
        platform.registerPrivate(capture, new CaptureRoute(captured), 1);
        try {
            EventEmitter.getInstance().send(new EventEnvelope().setTo("mock.sse.foreign")
                    .setReplyTo(capture).setCorrelationId("cid-foreign")
                    .setHeader(ACCEPT, TEXT_EVENT_STREAM));
            EventEnvelope head = captured.poll(20, TimeUnit.SECONDS);
            assertNotNull(head);
            assertEquals("mock-head", head.getRawBody());
            EventEnvelope token = captured.poll(20, TimeUnit.SECONDS);
            assertNotNull(token);
            assertEquals(DATA, marker(token));
            assertEquals("mock-token", token.getRawBody());
            EventEnvelope eof = captured.poll(20, TimeUnit.SECONDS);
            assertNotNull(eof);
            assertEquals(EOF, marker(eof));
            assertNull(captured.poll(500, TimeUnit.MILLISECONDS),
                    "frames after the decoded terminal are discarded");
        } finally {
            platform.release(capture);
        }
    }

    @Test
    void remoteStreamRendersProgressivelyOutTheEdge() throws IOException, InterruptedException {
        // the engine-to-engine composition: a streaming edge endpoint forwards its
        // reply lane into a 'send' to the event-over-http mapped streaming function -
        // segments relay through /api/event and re-render progressively here
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(localHost + "/api/hello/remote"))
                .timeout(Duration.ofSeconds(20)).header("Accept", TEXT_EVENT_STREAM).build();
        long started = System.currentTimeMillis();
        HttpResponse<java.util.stream.Stream<String>> response =
                client.send(request, HttpResponse.BodyHandlers.ofLines());
        assertEquals(200, response.statusCode());
        assertEquals(TEXT_EVENT_STREAM, response.headers().firstValue("content-type").orElse(""));
        List<String> lines = new ArrayList<>();
        List<Long> arrivals = new ArrayList<>();
        response.body().forEach(line -> {
            lines.add(line);
            arrivals.add(System.currentTimeMillis() - started);
        });
        int alpha = lines.indexOf("data: alpha");
        int beta = lines.indexOf("data: beta");
        int done = lines.indexOf("event: done");
        assertTrue(alpha >= 0 && beta > alpha && done > beta, "ordered relay: " + lines);
        // the remote eof's trailing metadata is the terminal frame's data
        assertTrue(lines.contains("data: {\"segments\":2}"), lines.toString());
        // progressive end to end: the remote target paces segments 250ms apart
        long elapsed = arrivals.get(beta) - arrivals.get(alpha);
        assertTrue(elapsed >= 150, "progressive relay expected, elapsed " + elapsed + " ms");
        Utility.getInstance().sleep(100);
    }
}
