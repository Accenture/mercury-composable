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

package org.platformlambda.async;

import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mock.MockAiBackend;
import org.platformlambda.mock.NotificationStreamFacade;
import org.platformlambda.redis.RedisConfig;
import org.platformlambda.sync.RedisTestBase;
import org.platformlambda.sync.ReturnRouteStore;
import org.platformlambda.sync.StreamResponder;
import org.platformlambda.sync.StreamSegment;
import org.platformlambda.sync.SyncRuntime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * E2 of the streaming return route - single-JVM end-to-end, <b>no broker anywhere</b> (the Kafka
 * building blocks are switched off): both driving use cases run behind real {@code stream: true}
 * endpoints, through real Redis and the real HTTP edge, and are consumed progressively with the shipped
 * SSE client - the full circle:
 *
 * <pre>
 *   HTTP request -> facade interceptor (StreamBridge: beginStream + EventStreamWriter)
 *     ... StreamResponder posts to Redis ... coordinator drains -> reply lane -> SSE out the edge
 *       -> async.http.request (Accept: text/event-stream + reply_to) -> collector envelopes
 * </pre>
 *
 * Scenarios: the chat case (ordered tokens + eof, exact order asserted end-to-end), the notification
 * case (cid announced to the UI, several producers, backend-side close), and the two idle-expiry
 * endings - the final drain RECOVERING a fully-lost close (design D4's confirmed nuance), and the
 * in-band 408 when nothing was queued. The consumer-side close ({@code closeStream}) is exercised by
 * the idle-expiry path, which is also what reclaims a disconnected client's stream.
 */
class StreamingRestE2eTest extends RedisTestBase {

    private static final int REST_PORT = 8305;   // matches rest.server.port in test application.properties
    private static final String HOST = "http://127.0.0.1:" + REST_PORT;
    private static final String X_EVENT_STREAM = "x-event-stream";
    private static final String X_EVENT_NAME = "x-event-name";

    @BeforeAll
    static void boot() throws Exception {
        System.setProperty("soa.redis.port", String.valueOf(redisPort));
        // E2 is deliberately broker-free: the streaming path has no Kafka dependency (design D3),
        // so the Kafka building blocks are switched off and no broker runs in this JVM at all
        System.setProperty("kafka.producer.enabled", "false");
        System.setProperty("kafka.consumer.enabled", "false");
        AutoStart.main(new String[0]);
        BlockingQueue<Boolean> ready = new ArrayBlockingQueue<>(1);
        Platform.getInstance().waitForProvider(AsyncHttpClient.ASYNC_HTTP_RESPONSE, 20).onSuccess(ready::add);
        if (!Boolean.TRUE.equals(ready.poll(20, TimeUnit.SECONDS))) {
            throw new IllegalStateException("REST automation HTTP server did not become ready");
        }
    }

    @AfterAll
    static void cleanup() {
        MockAiBackend.closeResponder();
        SyncRuntime.shutdown();
        System.clearProperty("soa.redis.port");
        System.clearProperty("kafka.producer.enabled");
        System.clearProperty("kafka.consumer.enabled");
    }

    // ------------------------------------------------------------------
    // plumbing: a private reply route collecting the SSE consumer's envelopes
    // ------------------------------------------------------------------

    /** One received reply envelope: the {@code x-event-stream} protocol headers plus the body. */
    private record Frame(Map<String, String> headers, Object body) {
        String marker() {
            return headers.get(X_EVENT_STREAM);
        }
        String name() {
            return headers.get(X_EVENT_NAME);
        }
    }

    private static final class SseCollector implements LambdaFunction {
        final List<Frame> frames = new CopyOnWriteArrayList<>();
        final CountDownLatch ended = new CountDownLatch(1);

        @Override
        public Object handleEvent(Map<String, String> headers, Object input, int instance) {
            frames.add(new Frame(new HashMap<>(headers), input));
            String marker = headers.get(X_EVENT_STREAM);
            if (StreamSegment.EOF.equals(marker) || StreamSegment.EXCEPTION.equals(marker)) {
                ended.countDown();
            }
            return null;
        }

        /** bodies of unnamed data frames, in arrival order */
        List<Object> dataBodies() {
            return frames.stream()
                    .filter(f -> StreamSegment.DATA.equals(f.marker()) && f.name() == null)
                    .map(Frame::body).toList();
        }

        /** data frames carrying this SSE event name, in arrival order */
        List<Frame> named(String eventName) {
            return frames.stream()
                    .filter(f -> StreamSegment.DATA.equals(f.marker()) && eventName.equals(f.name()))
                    .toList();
        }

        boolean endedWithEof() {
            return frames.stream().anyMatch(f -> StreamSegment.EOF.equals(f.marker()));
        }
    }

    /** Open a streaming HTTP call through the shipped SSE consumer, relaying to a fresh collector. */
    private static SseCollector openSse(String collectorRoute, AsyncHttpRequest request) {
        SseCollector collector = new SseCollector();
        Platform.getInstance().registerPrivate(collectorRoute, collector, 1);
        EventEmitter.getInstance().send(new EventEnvelope()
                .setTo("async.http.request").setBody(request.toMap())
                .setReplyTo(collectorRoute).setCorrelationId(Utility.getInstance().getUuid()));
        return collector;
    }

    private static AsyncHttpRequest sseRequest(String method, String url) {
        return new AsyncHttpRequest()
                .setMethod(method).setTargetHost(HOST).setUrl(url)
                .setHeader("accept", "text/event-stream")
                .setTimeoutSeconds(20);   // SSE consumption: the idle allowance between reads
    }

    /** Poll for the notification facade's cid announcement (the first SSE event of the channel). */
    private static String awaitAnnouncedCid(SseCollector collector) {
        for (int i = 0; i < 500; i++) {
            List<Frame> announce = collector.named(NotificationStreamFacade.CID_EVENT);
            if (!announce.isEmpty()) {
                return String.valueOf(announce.getFirst().body());
            }
            Utility.getInstance().sleep(10);
        }
        fail("the notification facade did not announce the session cid in time");
        return null;   // unreachable
    }

    private static RedisConfig responderConfig() {
        return new RedisConfig("127.0.0.1", redisPort, "", false, 0, 5000);
    }

    /** Poll until the route key is gone - the rendezvous is fully closed. */
    private static void awaitRouteGone(String cid) {
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            ReturnRouteStore probe = new ReturnRouteStore(c);
            for (int i = 0; i < 500; i++) {
                if (probe.getRoute(cid) == null) {
                    return;
                }
                Utility.getInstance().sleep(10);
            }
        }
        fail("route key for " + cid + " was not cleaned up in time");
    }

    /**
     * Post a <b>terminal</b> segment without asserting liveness - the twin of
     * {@code StreamReturnRouteTest.postTerminal}, whose Javadoc carries the full reasoning:
     * a closing post races its own consumption by construction (an in-flight drain woken by an
     * earlier post may deliver the terminal and delete the route before the producer's own route
     * check runs), so {@code post} may answer {@code false} although the segment <em>was</em>
     * delivered. The scenario asserts on what actually matters instead: the collector saw the
     * terminal event, and the rendezvous closed.
     */
    private static void postTerminal(StreamResponder responder, String cid) {
        responder.post(cid, StreamSegment.EOF, null, null);
    }

    // ------------------------------------------------------------------
    // use case: chat with an AI agent - strict order, single sequential producer
    // ------------------------------------------------------------------

    @Test
    void chatTokensRenderProgressivelyInExactOrder() throws Exception {
        SseCollector collector = openSse("chat.collector",
                sseRequest("POST", "/api/chat")
                        .setHeader("content-type", "application/json")
                        .setBody(Map.of("prompt", "compose")));

        assertTrue(collector.ended.await(15, TimeUnit.SECONDS), "the stream should end with the terminal event");
        // no sequence number anywhere in the pipeline, yet order holds end-to-end: producer connection
        // order -> list order -> serialized drain -> ordered reply lane -> SSE -> consumer envelopes
        assertEquals(MockAiBackend.TOKENS, collector.dataBodies(), "token order preserved end-to-end");
        List<Frame> done = collector.named("done");
        assertEquals(1, done.size(), "close(metadata) renders as the terminal SSE done event");
        assertEquals(MockAiBackend.EOT_METADATA, done.getFirst().body(), "eof metadata rides the done event");
        assertTrue(collector.endedWithEof(), "a clean upstream end relays as eof");
        assertTrue(collector.frames.size() >= MockAiBackend.TOKENS.size() + 2,
                "segments arrived multi-shot (progressive), not as one buffered response");
        assertEquals(0, SyncRuntime.activeStreams(), "rendezvous closed by its terminal segment");
    }

    // ------------------------------------------------------------------
    // use case: event notification - several producers, backend-side close
    // ------------------------------------------------------------------

    @Test
    void notificationChannelServesSeveralProducersAndBackendClose() throws Exception {
        SseCollector collector = openSse("notify.collector", sseRequest("GET", "/api/notifications"));
        String cid = awaitAnnouncedCid(collector);
        assertNotNull(cid);

        // two backend services, each with its own responder (its own connection) - as separate pods would
        try (StreamResponder orders = new StreamResponder(responderConfig());
             StreamResponder billing = new StreamResponder(responderConfig())) {
            assertTrue(orders.post(cid, StreamSegment.DATA, "orders", "order 42 shipped"));
            assertTrue(billing.post(cid, StreamSegment.DATA, "billing", "invoice 7 ready"));
            // a backend service - not the UI - ends the channel: any producer may post the terminal
            // (liveness deliberately not asserted - the data posts above may have a drain in flight)
            postTerminal(billing, cid);

            assertTrue(collector.ended.await(15, TimeUnit.SECONDS), "backend-side close ends the SSE render");
            assertEquals("order 42 shipped", collector.named("orders").getFirst().body());
            assertEquals("invoice 7 ready", collector.named("billing").getFirst().body());
            assertTrue(collector.endedWithEof());

            awaitRouteGone(cid);
            assertFalse(orders.post(cid, StreamSegment.DATA, "orders", "order 43 shipped"),
                    "the other producer stops on its next post (orphan)");
        }
        assertEquals(0, SyncRuntime.activeStreams());
    }

    // ------------------------------------------------------------------
    // idle expiry, ending 1: the single final drain RECOVERS a fully-lost close (design D4's nuance)
    // ------------------------------------------------------------------

    @Test
    void idleExpiryFinalDrainRecoversALostClose() throws Exception {
        SseCollector collector = openSse("recover.collector",
                sseRequest("GET", "/api/notifications")
                        .setHeader(NotificationStreamFacade.IDLE_HEADER, "2"));
        String cid = awaitAnnouncedCid(collector);

        // worst case: segments are stored (store-first!) but EVERY wake-up is lost - no publish at all
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            ReturnRouteStore direct = new ReturnRouteStore(c);
            direct.appendSegment(cid, StreamSegment.of(StreamSegment.DATA, "orders", "the lost one").toJson(), 60);
            direct.appendSegment(cid, StreamSegment.of(StreamSegment.EOF, null, "{\"recovered\":true}").toJson(), 60);
        }

        // nothing wakes the pod; at idle expiry the watchdog performs ONE final drain - and the render
        // completes successfully despite the lost notifications
        assertTrue(collector.ended.await(15, TimeUnit.SECONDS), "final drain completes the render");
        assertEquals("the lost one", collector.named("orders").getFirst().body(), "queued segment recovered");
        List<Frame> done = collector.named("done");
        assertEquals(1, done.size(), "the recovered eof closes the stream normally");
        assertEquals("{\"recovered\":true}", done.getFirst().body());
        assertTrue(collector.endedWithEof());
        awaitRouteGone(cid);
        assertEquals(0, SyncRuntime.activeStreams());
    }

    // ------------------------------------------------------------------
    // idle expiry, ending 2: nothing queued - fail in-band (408) and close the rendezvous
    // ------------------------------------------------------------------

    @Test
    void idleExpiryWithNothingQueuedFailsInBandAndStopsProducers() throws Exception {
        SseCollector collector = openSse("idle.collector",
                sseRequest("GET", "/api/notifications")
                        .setHeader(NotificationStreamFacade.IDLE_HEADER, "2"));
        String cid = awaitAnnouncedCid(collector);

        // no producer ever posts; the watchdog's final drain finds nothing and fails the render in-band
        assertTrue(collector.ended.await(15, TimeUnit.SECONDS), "idle expiry ends the SSE render");
        List<Frame> error = collector.named("error");
        assertEquals(1, error.size(), "the in-band failure renders as the SSE error event");
        String errorBody = String.valueOf(error.getFirst().body());
        assertTrue(errorBody.contains("408") && errorBody.contains("Stream idle timeout"),
                "the error event carries the 408 timeout, got: " + errorBody);

        // the consumer-side close deleted the route, so a late producer stops immediately
        awaitRouteGone(cid);
        try (StreamResponder late = new StreamResponder(responderConfig())) {
            assertFalse(late.post(cid, StreamSegment.DATA, "orders", "too late"),
                    "producers learn the rendezvous is over from their next post");
        }
        assertEquals(0, SyncRuntime.activeStreams());
    }
}
