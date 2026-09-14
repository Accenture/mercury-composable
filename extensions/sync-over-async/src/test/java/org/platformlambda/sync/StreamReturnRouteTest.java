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

package org.platformlambda.sync;

import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;
import org.platformlambda.redis.RedisConfig;
import org.platformlambda.support.SyncOverAsyncConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The E1 suite of the streaming return route (draft-design-specs/streaming-return-route.md): the UI pod
 * opens a streaming rendezvous ({@code beginStream}) and {@link StreamResponder} producers - constructed
 * from plain {@link RedisConfig}, no coordinator, exactly as a backend service would - post segments
 * cross-pod through Redis alone. Covers both driving use cases: the sequential single producer whose
 * ordering must hold end-to-end (AI chat), and several uncoordinated producers on one cid where either
 * side may close the channel (event notification) - plus the recovery paths (missed wake-up, final drain
 * at idle expiry), the orphan contract, capacity, and the D8 unification (a one-shot response completed
 * by a stream producer's terminal post: the degenerate stream shown to be degenerate).
 */
class StreamReturnRouteTest extends RedisTestBase {

    private static final SyncOverAsyncConfig CONFIG = new SyncOverAsyncConfig("svc-return", 90, 30, 100, 1800, 100);

    private ReturnRouteCoordinator uiPod;   // the pod holding the (would-be) HTTP connection

    @BeforeEach
    void setup() {
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            c.sync().flushall();
        }
        uiPod = new ReturnRouteCoordinator(redisClient, "pod-UI", CONFIG);
        uiPod.start();
    }

    @AfterEach
    void teardown() {
        uiPod.close();
    }

    private static String newCid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static RedisConfig responderConfig() {
        return new RedisConfig("127.0.0.1", redisPort, "", false, 0, 5000);
    }

    /** Test sink: collects every delivered segment in forward order and latches on the terminal one. */
    private static final class CollectingSink implements Consumer<StreamSegment> {
        final List<StreamSegment> segments = new CopyOnWriteArrayList<>();
        final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public void accept(StreamSegment segment) {
            segments.add(segment);
            if (segment.isTerminal()) {
                closed.countDown();
            }
        }

        List<String> bodies() {
            return segments.stream().map(StreamSegment::body).toList();
        }
    }

    /** Poll until the route key is gone - i.e. the drain's terminal cleanup has fully completed. */
    private void awaitRouteGone(String cid) {
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

    /** Poll until the sink has received its first segment (for expectations with no terminal latch). */
    private static void awaitFirstSegment(CollectingSink sink) {
        for (int i = 0; i < 500; i++) {
            if (!sink.segments.isEmpty()) {
                return;
            }
            Utility.getInstance().sleep(10);
        }
        fail("expected a first segment, got none in time");
    }

    /**
     * Post a <b>terminal</b> segment without asserting liveness.
     *
     * <p>A closing post races its own consumption by construction: the segment is appended
     * store-first, so a drain already in flight (woken by an earlier post) may pop it, deliver it,
     * and close the rendezvous - deleting the route - before the producer's own route check runs.
     * {@code post} then answers {@code false} although the segment <em>was</em> delivered. That
     * answer is still correct under the contract ("false = stop producing", and a producer has
     * nothing to send after a terminal segment), so these scenarios assert on what actually
     * matters: the sink saw the terminal segment, and the rendezvous closed. Liveness stays
     * asserted on DATA posts, where a {@code false} would mean lost segments - and on a terminal
     * post that provably cannot race (see the call sites that keep it).</p>
     */
    private static void postTerminal(StreamResponder responder, String cid, String body) {
        responder.post(cid, StreamSegment.EOF, null, body);
    }

    // ------------------------------------------------------------------
    // Use case: chat with an AI agent - one sequential producer, strict order
    // ------------------------------------------------------------------

    @Test
    void sequentialProducerDeliversInExactOrder() throws Exception {
        String cid = newCid();
        CollectingSink sink = new CollectingSink();
        uiPod.beginStream(cid, sink);
        List<String> tokens = List.of("The", " quick", " brown", " fox", " jumps", " over", " the", " lazy dog");
        try (StreamResponder responder = new StreamResponder(responderConfig())) {
            for (String token : tokens) {
                assertTrue(responder.post(cid, StreamSegment.DATA, null, token), "rendezvous is live");
            }
            postTerminal(responder, cid, "{\"total\":8}");
        }
        assertTrue(sink.closed.await(5, TimeUnit.SECONDS), "terminal segment delivered");
        // no sequence number anywhere: posting discipline -> list order -> serialized drain == exact order
        assertEquals(tokens, sink.bodies().subList(0, tokens.size()), "forward order equals posting order");
        StreamSegment terminal = sink.segments.getLast();
        assertEquals(StreamSegment.EOF, terminal.type());
        assertEquals("{\"total\":8}", terminal.body());
        assertEquals(tokens.size() + 1, sink.segments.size(), "nothing lost, nothing duplicated");
        // terminal cleanup: entry closed, keys deleted eagerly
        awaitRouteGone(cid);
        assertEquals(0, uiPod.activeStreams());
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            assertEquals(0, new ReturnRouteStore(c).queueLength(cid), "queue deleted with the rendezvous");
        }
    }

    // ------------------------------------------------------------------
    // Use case: event notification - several producers, unordered, anyone may close
    // ------------------------------------------------------------------

    @Test
    void concurrentProducersInterleaveWithoutLossOrDuplication() throws Exception {
        final int perProducer = 25;
        String cid = newCid();
        CollectingSink sink = new CollectingSink();
        uiPod.beginStream(cid, sink);
        List<String> producers = List.of("svc-A", "svc-B", "svc-C");
        try (ExecutorService pool = Executors.newFixedThreadPool(producers.size())) {
            List<Future<?>> posts = new ArrayList<>();
            for (String producer : producers) {
                posts.add(pool.submit(() -> {
                    // each producer owns its responder = its own connection, as separate pods would
                    try (StreamResponder responder = new StreamResponder(responderConfig())) {
                        for (int i = 1; i <= perProducer; i++) {
                            assertTrue(responder.post(cid, StreamSegment.DATA, producer,
                                    producer + "-" + i), "rendezvous stays live while producing");
                        }
                    }
                }));
            }
            for (Future<?> post : posts) {
                post.get(10, TimeUnit.SECONDS);
            }
        }
        try (StreamResponder closer = new StreamResponder(responderConfig())) {
            postTerminal(closer, cid, null);
        }
        assertTrue(sink.closed.await(5, TimeUnit.SECONDS), "terminal segment delivered");
        List<StreamSegment> delivered = sink.segments;
        assertEquals(producers.size() * perProducer + 1, delivered.size(), "no loss, no duplication");
        // global interleave is arbitrary by design, but each producer's own subsequence must keep its
        // posting order (Redis per-connection command ordering + serialized drains)
        for (String producer : producers) {
            List<String> subsequence = delivered.stream()
                    .filter(s -> producer.equals(s.name())).map(StreamSegment::body).toList();
            List<String> expected = new ArrayList<>();
            for (int i = 1; i <= perProducer; i++) {
                expected.add(producer + "-" + i);
            }
            assertEquals(expected, subsequence, "per-producer order preserved for " + producer);
        }
        awaitRouteGone(cid);
        assertEquals(0, uiPod.activeStreams());
    }

    @Test
    void anyProducerMayCloseTheChannel() throws Exception {
        String cid = newCid();
        CollectingSink sink = new CollectingSink();
        uiPod.beginStream(cid, sink);
        try (StreamResponder notifier = new StreamResponder(responderConfig());
             StreamResponder other = new StreamResponder(responderConfig())) {
            assertTrue(notifier.post(cid, StreamSegment.DATA, "orders", "order 42 shipped"));
            assertTrue(notifier.post(cid, StreamSegment.DATA, "orders", "order 43 shipped"));
            // a NON-originating producer posts the terminal entry - "the end signal is also an event"
            postTerminal(other, cid, null);
            assertTrue(sink.closed.await(5, TimeUnit.SECONDS), "close from another producer completes the stream");
            awaitRouteGone(cid);
            // the still-active producer learns the rendezvous is over from its next post: orphan -> stop
            assertFalse(notifier.post(cid, StreamSegment.DATA, "orders", "order 44 shipped"),
                    "route deletion stops the remaining producers");
        }
        assertEquals(0, uiPod.activeStreams());
    }

    // ------------------------------------------------------------------
    // Orphan and consumer-side close contracts
    // ------------------------------------------------------------------

    @Test
    void postWithoutRendezvousIsOrphan() {
        String cid = newCid();   // nobody ever called beginStream for it
        try (StreamResponder responder = new StreamResponder(responderConfig())) {
            assertFalse(responder.post(cid, StreamSegment.DATA, null, "nobody is listening"));
        }
        // store-first is deliberate: the segment was appended before the route check, and simply ages out
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            assertEquals(1, new ReturnRouteStore(c).queueLength(cid), "orphan segment queued under its TTL");
            long ttl = c.sync().ttl("queue:" + cid);
            assertTrue(ttl > 0, "orphan remnant carries a TTL from birth, got " + ttl);
        }
    }

    @Test
    void consumerSideCloseStopsProducers() {
        String cid = newCid();
        CollectingSink sink = new CollectingSink();
        uiPod.beginStream(cid, sink);
        try (StreamResponder responder = new StreamResponder(responderConfig())) {
            assertTrue(responder.post(cid, StreamSegment.DATA, null, "before the client left"));
            awaitFirstSegment(sink);
            // client disconnect (or edge idle expiry): the facade closes from the consumer side
            uiPod.closeStream(cid);
            assertEquals(0, uiPod.activeStreams());
            awaitRouteGone(cid);
            assertFalse(responder.post(cid, StreamSegment.DATA, null, "after the client left"),
                    "producers stop on their next post");
        }
    }

    // ------------------------------------------------------------------
    // Recovery cornerstones: missed wake-up, final drain at idle expiry
    // ------------------------------------------------------------------

    @Test
    void missedWakeUpIsHealedByTheNextDrain() throws Exception {
        String cid = newCid();
        CollectingSink sink = new CollectingSink();
        uiPod.beginStream(cid, sink);
        // simulate a lost notification: a segment is stored (store-first!) but its wake-up never arrives
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            new ReturnRouteStore(c).appendSegment(cid,
                    StreamSegment.of(StreamSegment.DATA, null, "the silent one").toJson(), 60);
        }
        assertEquals(0, sink.segments.size(), "no wake-up, no drain - the segment waits in the queue");
        // the NEXT post's wake-up drains everything queued, in order - the dropped signal costs latency only
        try (StreamResponder responder = new StreamResponder(responderConfig())) {
            // liveness is sound to assert here: no wake-up was ever published for this cid, so no
            // drain can be in flight to consume this terminal segment before the route check
            assertTrue(responder.post(cid, StreamSegment.EOF, null, null));
        }
        assertTrue(sink.closed.await(5, TimeUnit.SECONDS));
        assertEquals(Arrays.asList("the silent one", null), sink.bodies(), "healed drain delivered both, in order");
    }

    @Test
    void finalDrainAtIdleExpiryRecoversADroppedClose() {
        String cid = newCid();
        CollectingSink sink = new CollectingSink();
        uiPod.beginStream(cid, sink);
        // every notification is lost, including the final one - the worst case D4's nuance exists for
        try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
            ReturnRouteStore direct = new ReturnRouteStore(c);
            direct.appendSegment(cid, StreamSegment.of(StreamSegment.DATA, null, "last tokens").toJson(), 60);
            direct.appendSegment(cid, StreamSegment.of(StreamSegment.EOF, null, "{\"done\":true}").toJson(), 60);
        }
        // at edge idle expiry the facade does ONE last-chance drain before failing the render in-band
        assertTrue(uiPod.finalDrain(cid), "the single final drain completes the stream");
        assertEquals(List.of("last tokens", "{\"done\":true}"), sink.bodies());
        assertEquals(0, uiPod.activeStreams(), "stream closed by its recovered terminal segment");
        awaitRouteGone(cid);
    }

    @Test
    void finalDrainOnAQuietStreamFindsNothingAndKeepsItOpen() {
        String cid = newCid();
        CollectingSink sink = new CollectingSink();
        uiPod.beginStream(cid, sink);
        assertFalse(uiPod.finalDrain(cid), "nothing queued, stream not completed");
        assertEquals(1, uiPod.activeStreams(), "a quiet stream stays open - closing is the caller's decision");
        uiPod.closeStream(cid);
        assertEquals(0, uiPod.activeStreams());
    }

    // ------------------------------------------------------------------
    // Robustness: duplicate wake-ups, capacity, sink failure
    // ------------------------------------------------------------------

    @Test
    void duplicateWakeUpDeliversNothingTwice() throws Exception {
        String cid = newCid();
        CollectingSink sink = new CollectingSink();
        uiPod.beginStream(cid, sink);
        try (StreamResponder responder = new StreamResponder(responderConfig())) {
            assertTrue(responder.post(cid, StreamSegment.DATA, null, "once"));
            awaitFirstSegment(sink);
            // duplicate/spurious wake-ups pop nothing: destructive reads need no consumer bookkeeping
            try (StatefulRedisConnection<String, String> c = redisClient.connect()) {
                c.sync().publish("svc-return:pod-UI", cid);
                c.sync().publish("svc-return:pod-UI", cid);
            }
            Utility.getInstance().sleep(200);   // give the spurious drains time to (not) deliver
            assertEquals(List.of("once"), sink.bodies(), "no duplicate delivery");
            postTerminal(responder, cid, null);
        }
        assertTrue(sink.closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void streamCapacityIsBoundedAndReleasedOnClose() {
        SyncOverAsyncConfig tinyCap = new SyncOverAsyncConfig("svc-return", 90, 30, 100, 1800, 1);
        try (ReturnRouteCoordinator smallPod = new ReturnRouteCoordinator(redisClient, "pod-CAP", tinyCap)) {
            smallPod.beginStream("cap-1", segment -> { });
            assertThrows(IllegalStateException.class, () -> smallPod.beginStream("cap-2", segment -> { }),
                    "beginStream rejects deterministically at capacity");
            smallPod.closeStream("cap-1");
            smallPod.beginStream("cap-3", segment -> { });   // slot released on close
            assertEquals(1, smallPod.activeStreams());
        }
    }

    @Test
    void sinkFailureClosesTheStream() {
        String cid = newCid();
        uiPod.beginStream(cid, segment -> {
            throw new IllegalStateException("simulated: the HTTP edge is gone");
        });
        try (StreamResponder responder = new StreamResponder(responderConfig())) {
            assertTrue(responder.post(cid, StreamSegment.DATA, null, "undeliverable"));
            awaitRouteGone(cid);   // the failed drain closes the stream and deletes the keys
            assertEquals(0, uiPod.activeStreams(), "a broken consumer cannot leak its stream entry");
            assertFalse(responder.post(cid, StreamSegment.DATA, null, "more"), "producers stop (orphan)");
        }
    }

    // ------------------------------------------------------------------
    // D8: one mechanism - the one-shot response is the degenerate stream
    // ------------------------------------------------------------------

    @Test
    void streamProducerCanCompleteAOneShotRequest() throws Exception {
        // begin() registers an ordinary one-shot request; a StreamResponder's terminal post completes it -
        // deliver() and a closing post are the same act on the same store (D8 made literal)
        String cid = newCid();
        CompletableFuture<String> future = uiPod.begin(cid);
        try (StreamResponder responder = new StreamResponder(responderConfig())) {
            // a one-shot route is deleted only by awaitResponse's cleanup, which runs after this
            // post returns - so liveness is sound here, and it proves the wake-up path was taken
            // rather than the final-drain fallback
            assertTrue(responder.post(cid, StreamSegment.EOF, null, "{\"result\":\"accepted\"}"));
        }
        assertEquals("{\"result\":\"accepted\"}", uiPod.awaitResponse(cid, future, 5000));
        assertEquals(0, uiPod.pendingCount());
    }

    @Test
    void oneShotAndStreamShareTheReturnChannel() throws Exception {
        // one subscription serves both patterns: a wake-up is checked against streams first, then requests
        String oneShotCid = newCid();
        String streamCid = newCid();
        CollectingSink sink = new CollectingSink();
        CompletableFuture<String> future = uiPod.begin(oneShotCid);
        uiPod.beginStream(streamCid, sink);
        try (StreamResponder responder = new StreamResponder(responderConfig())) {
            assertTrue(responder.post(streamCid, StreamSegment.DATA, null, "progress 50%"));
            // the one-shot route survives until awaitResponse cleans it up (sound to assert).
            // the stream's terminal post can be consumed by the drain the line above woke, so it
            // does not assert liveness - see postTerminal
            assertTrue(responder.post(oneShotCid, StreamSegment.EOF, null, "{\"status\":\"200\"}"));
            postTerminal(responder, streamCid, null);
        }
        assertEquals("{\"status\":\"200\"}", uiPod.awaitResponse(oneShotCid, future, 5000));
        assertTrue(sink.closed.await(5, TimeUnit.SECONDS));
        assertEquals(Arrays.asList("progress 50%", null), sink.bodies());
        assertEquals(0, uiPod.pendingCount());
        awaitRouteGone(streamCid);
        assertEquals(0, uiPod.activeStreams());
    }
}
