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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.automation.services.EventStreamRenderer;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.serializers.SimpleMapper;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end verification of HTTP response streaming: a callee streams events to the
 * caller's reply_to (a dedicated ordered reply lane checked out from the pool for the
 * request's lifetime) until end of transmission, and the edge renders them progressively -
 * SSE framing for text/event-stream, chunked/NDJSON otherwise. The wire carries only
 * standard HTTP.
 */
class EventStreamResponseTest extends TestBase {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static String base;

    @BeforeAll
    static void resolveBaseUrl() {
        // the streaming mock itself is registered by TestBase before endpoint rendering
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port", config.getProperty("server.port", "8100"));
        base = "http://127.0.0.1:" + port;
    }

    private record TimedLine(String line, long nanoTime) { }

    private HttpResponse<java.util.stream.Stream<String>> get(String pathAndQuery, String accept)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + pathAndQuery))
                .timeout(Duration.ofSeconds(20)).header("Accept", accept).build();
        return client.send(request, HttpResponse.BodyHandlers.ofLines());
    }

    private List<TimedLine> collect(HttpResponse<java.util.stream.Stream<String>> response) {
        List<TimedLine> lines = new ArrayList<>();
        response.body().forEach(line -> lines.add(new TimedLine(line, System.nanoTime())));
        return lines;
    }

    private List<String> plain(List<TimedLine> lines) {
        return lines.stream().map(TimedLine::line).toList();
    }

    @Test
    void sseSegmentsArriveProgressivelyWithTerminalDoneEvent() throws IOException, InterruptedException {
        var response = get("/api/hello/stream?mode=sse", "text/event-stream");
        List<TimedLine> lines = collect(response);
        assertEquals(200, response.statusCode());
        assertEquals("text/event-stream", response.headers().firstValue("content-type").orElse(""));
        assertEquals("no-cache", response.headers().firstValue("cache-control").orElse(""));
        assertTrue(response.headers().firstValue("content-length").isEmpty(), "a stream has no content length");
        List<String> body = plain(lines);
        int first = body.indexOf("data: Hello");
        int second = body.indexOf("data: token stream");
        int done = body.indexOf("event: done");
        assertTrue(first >= 0 && second > first && done > second, "ordered SSE frames: " + body);
        assertTrue(body.contains("data: {\"segments\":2}"), "eof body rides the done event: " + body);
        // the producer paces segments 250 ms apart - a buffered response would arrive all at once
        long elapsedMs = (lines.get(done).nanoTime() - lines.get(first).nanoTime()) / 1_000_000;
        assertTrue(elapsedMs >= 300, "progressive delivery expected, elapsed " + elapsedMs + " ms");
    }

    @Test
    void namedSegmentsBecomeTypedSseEvents() throws IOException, InterruptedException {
        var response = get("/api/hello/stream?mode=sse-named", "text/event-stream");
        List<String> body = plain(collect(response));
        int name = body.indexOf("event: tokens");
        assertTrue(name >= 0 && "data: {\"n\":1}".equals(body.get(name + 1)), "typed event framing: " + body);
        assertTrue(body.contains("event: done"), body.toString());
    }

    @Test
    void multiLineSegmentSplitsIntoSuccessiveDataLines() throws IOException, InterruptedException {
        var response = get("/api/hello/stream?mode=sse-multiline", "text/event-stream");
        List<String> body = plain(collect(response));
        int first = body.indexOf("data: line1");
        assertTrue(first >= 0 && "data: line2".equals(body.get(first + 1)),
                "SSE multi-line data framing: " + body);
    }

    @Test
    void structuredSegmentsStreamAsJsonLinesInChunkedMode() throws IOException, InterruptedException {
        var response = get("/api/hello/stream?mode=ndjson", "*/*");
        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""));
        List<String> body = plain(collect(response)).stream().filter(s -> !s.isEmpty()).toList();
        assertEquals(3, body.size(), "one JSON object per line: " + body);
        var util = Utility.getInstance();
        for (int i = 0; i < 3; i++) {
            Map<?, ?> map = SimpleMapper.getInstance().getMapper().readValue(body.get(i), Map.class);
            // customized Gson reads Map integers as Long - compare via the utility converter
            assertEquals(i + 1, util.str2int(String.valueOf(map.get("seq"))));
        }
    }

    @Test
    void textSegmentsAppendInChunkedMode() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/hello/stream?mode=chunk"))
                .timeout(Duration.ofSeconds(20)).header("Accept", "text/plain").build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("text/plain", response.headers().firstValue("content-type").orElse(""));
        assertEquals("alphabeta", response.body());
        assertTrue(response.headers().firstValue("content-length").isEmpty(), "a stream has no content length");
    }

    @Test
    void midStreamFailureArrivesAsInBandErrorEvent() throws IOException, InterruptedException {
        var response = get("/api/hello/stream?mode=error", "text/event-stream");
        // the head is committed by the first segment, so the status stays 200
        assertEquals(200, response.statusCode());
        List<String> body = plain(collect(response));
        assertTrue(body.contains("data: partial"), body.toString());
        int error = body.indexOf("event: error");
        assertTrue(error >= 0, "in-band error event expected: " + body);
        assertTrue(body.get(error + 1).contains("backend on fire"), body.toString());
        assertTrue(body.get(error + 1).contains("503"), body.toString());
        assertFalse(body.contains("event: done"), "a failed stream has no done event");
    }

    @Test
    void failureBeforeFirstSegmentIsANormalHttpError() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/hello/stream?mode=error-first"))
                .timeout(Duration.ofSeconds(20)).header("Accept", "application/json").build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("no backend"), response.body());
    }

    @Test
    void eofOnlyStreamRendersTerminalEventImmediately() throws IOException, InterruptedException {
        var response = get("/api/hello/stream?mode=empty-close", "text/event-stream");
        assertEquals(200, response.statusCode());
        assertEquals("text/event-stream", response.headers().firstValue("content-type").orElse(""));
        List<String> body = plain(collect(response));
        int done = body.indexOf("event: done");
        assertTrue(done >= 0 && body.get(done + 1).contains("\"done\":true"), body.toString());
    }

    @Test
    void keepAliveCommentsFlowWhileTheProducerIsQuiet() throws IOException, InterruptedException {
        // test configuration sets event.stream.keep.alive=1s; the producer is quiet for 2.5s
        var response = get("/api/hello/stream?mode=ping", "text/event-stream");
        List<String> body = plain(collect(response));
        assertTrue(body.contains(": ping"), "keep-alive comment expected: " + body);
        assertTrue(body.contains("data: late"), body.toString());
        assertTrue(body.contains("event: done"), body.toString());
    }

    @Test
    void burstSegmentsRenderInStrictFifoOrder() throws IOException, InterruptedException {
        // 50 unpaced segments from the callee - the request's dedicated reply lane
        // (a single-instance route) must preserve exact FIFO order
        var response = get("/api/hello/stream?mode=burst", "text/plain");
        assertEquals(200, response.statusCode());
        List<String> body = plain(collect(response)).stream().filter(s -> !s.isEmpty()).toList();
        assertEquals(50, body.size(), "all segments delivered");
        for (int i = 1; i <= 50; i++) {
            assertEquals(String.valueOf(i), body.get(i - 1).split("\\|")[0], "strict segment order");
        }
    }

    @Test
    void concurrentStreamsRenderIndependentlyAndInOrder() {
        // four parallel 50-segment bursts: each request checks out its own dedicated
        // reply lane, so segments stay in strict FIFO while the requests stream concurrently
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return plain(collect(get("/api/hello/stream?mode=burst", "text/plain")));
                } catch (IOException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            }));
        }
        for (var future : futures) {
            List<String> body = future.join().stream().filter(s -> !s.isEmpty()).toList();
            assertEquals(50, body.size(), "all segments delivered");
            for (int i = 1; i <= 50; i++) {
                assertEquals(String.valueOf(i), body.get(i - 1).split("\\|")[0], "strict per-request order");
            }
        }
    }

    @Test
    void poolExhaustionRejectsWithHttp503AndRecovers() throws IOException, InterruptedException {
        // drain the reply-lane pool: a streaming endpoint without an available lane
        // is rejected immediately with HTTP-503 (deterministic back-pressure)
        List<String> drained = new ArrayList<>();
        String lane;
        while ((lane = EventStreamRenderer.checkoutLane()) != null) {
            drained.add(lane);
        }
        assertFalse(drained.isEmpty(), "the pool should have lanes to drain");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/hello/stream?mode=chunk"))
                    .timeout(Duration.ofSeconds(20)).header("Accept", "text/plain").build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(503, response.statusCode());
            assertTrue(response.body().contains("Streaming response pool exhausted"), response.body());
        } finally {
            drained.forEach(EventStreamRenderer::releaseLane);
        }
        // capacity restored - the same endpoint streams normally again
        HttpRequest again = HttpRequest.newBuilder(URI.create(base + "/api/hello/stream?mode=chunk"))
                .timeout(Duration.ofSeconds(20)).header("Accept", "text/plain").build();
        HttpResponse<String> ok = client.send(again, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, ok.statusCode());
        assertEquals("alphabeta", ok.body());
    }

    @Test
    void aCompletedStreamReturnsItsLaneToThePool() throws IOException, InterruptedException {
        int before = EventStreamRenderer.getAvailableLanes();
        assertTrue(before > 0, "lanes should be available before the request");
        var response = get("/api/hello/stream?mode=sse", "text/event-stream");
        assertTrue(plain(collect(response)).contains("event: done"));
        // the lane is released when the request context closes, just before the
        // response ends; a short settle keeps this robust under load
        var util = Utility.getInstance();
        for (int i = 0; i < 50 && EventStreamRenderer.getAvailableLanes() < before; i++) {
            util.sleep(100);
        }
        assertEquals(before, EventStreamRenderer.getAvailableLanes(), "checkout/release must balance");
    }

    @Test
    void laneCheckoutRotatesThroughThePool() {
        // the pool is a rotating FIFO queue: a released lane rejoins at the tail,
        // so consecutive requests take successive lanes (round-robin) and a
        // just-released lane gets the longest possible rest before reuse
        String first = EventStreamRenderer.checkoutLane();
        assertNotNull(first);
        EventStreamRenderer.releaseLane(first);
        String second = EventStreamRenderer.checkoutLane();
        assertNotNull(second);
        assertNotEquals(first, second, "a released lane must go to the tail, not be reused immediately");
        EventStreamRenderer.releaseLane(second);
    }

    @Test
    void responseHeaderTransformAppliesToTheStreamedHead() throws IOException, InterruptedException {
        // the endpoint declares "headers: header_2" (add x-stream-transform, drop
        // x-secret-header) - the streamed head must honor it like a single-shot response
        var response = get("/api/hello/stream?mode=headers", "text/event-stream");
        assertEquals(200, response.statusCode());
        assertEquals("applied", response.headers().firstValue("x-stream-transform").orElse(""),
                "add directive must apply");
        assertTrue(response.headers().firstValue("x-secret-header").isEmpty(), "drop directive must apply");
        assertEquals("visible", response.headers().firstValue("x-custom-note").orElse(""),
                "unlisted headers pass through");
        List<String> body = plain(collect(response));
        assertTrue(body.contains("data: transformed"), body.toString());
        assertTrue(body.contains("event: done"), body.toString());
    }

    @Test
    void streamMarkerWinsOverAStrayStreamId() throws IOException, InterruptedException {
        var response = get("/api/hello/stream?mode=conflict", "text/event-stream");
        assertEquals(200, response.statusCode());
        List<String> body = plain(collect(response));
        assertTrue(body.contains("data: resolved"), body.toString());
        assertTrue(body.contains("event: done"), body.toString());
    }

    @Test
    void arrivingSegmentsExtendTheIdleAllowance() throws IOException, InterruptedException {
        // 3 segments 700ms apart under a 1s idle ttl: total 2.1s > ttl, but every
        // gap is within it - the per-segment touch must keep the stream alive
        var response = get("/api/hello/stream?mode=slow-paced", "text/event-stream");
        List<String> body = plain(collect(response));
        assertTrue(body.contains("data: segment-3"), body.toString());
        assertTrue(body.contains("event: done"), "the paced stream must complete: " + body);
        assertFalse(body.contains("event: error"), body.toString());
    }

    @Test
    void stalledProducerTimesOutInBand() throws IOException, InterruptedException {
        // the producer declares a one-second idle allowance (x-ttl) and then goes silent;
        // the context housekeeper (10s sweep) must fail the stream in-band
        long started = System.currentTimeMillis();
        var response = get("/api/hello/stream?mode=stall", "text/event-stream");
        List<String> body = plain(collect(response));
        long elapsed = System.currentTimeMillis() - started;
        assertTrue(body.contains("data: one"), body.toString());
        int error = body.indexOf("event: error");
        assertTrue(error >= 0, "in-band timeout expected: " + body);
        assertTrue(body.get(error + 1).contains("Timeout for 1 seconds"), body.toString());
        assertTrue(elapsed < 15000, "the housekeeper should fail the stream within one sweep, took "
                + elapsed + " ms");
        Utility.getInstance().sleep(100);
    }
}
