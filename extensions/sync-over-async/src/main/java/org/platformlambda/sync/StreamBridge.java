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

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * The generic half of a UI-pod streaming facade (design D2): wires a streaming rendezvous to the HTTP
 * edge's reply lane and owns its lifecycle, so an application interceptor is one line plus its own
 * request leg:
 *
 * <pre>
 * &#64;PreLoad(route = "my.chat.facade", instances = 50)
 * &#64;EventInterceptor
 * public class ChatFacade implements TypedLambdaFunction&lt;EventEnvelope, Void&gt; {
 *     public Void handleEvent(Map&lt;String, String&gt; headers, EventEnvelope request, int instance) {
 *         String cid = request.getCorrelationId();
 *         StreamBridge.open(SyncRuntime.coordinator(), request, cid, 30);
 *         // ... start the backend work however the application likes (the request leg) ...
 *         return null;
 *     }
 * }
 * </pre>
 *
 * {@link #open} commits the SSE head with {@code idleSeconds} as the edge's idle allowance (widen it for
 * a deliberately quiet notification channel), registers an {@link EventStreamSink} via
 * {@code beginStream}, and arms an idle watchdog with the same allowance. Every drained segment resets
 * the watchdog; the terminal segment disarms it (the drain already closed the stream and freed the keys).
 * At idle expiry the watchdog performs <b>one final drain</b> - the streaming analogue of the one-shot
 * final read before timeout, recovering a dropped final notification. And only if that drain does not
 * complete the stream does it fail the render in-band (408) and close the rendezvous, deleting the route
 * so every producer stops. This single watchdog is also what reclaims an abandoned stream: the edge
 * drops late writes after a client disconnect on its own, and the idle expiry then releases the
 * {@code PendingStreams} slot and the Redis keys, so nothing leaks.
 *
 * <p>At stream capacity, the exchange is failed with a real HTTP 503 (the head is not yet committed)
 * and the returned writer is already closed - mirroring the edge's own reply-lane back-pressure.</p>
 *
 * <p>The returned writer may be used for facade-authored events (e.g. announcing the session's cid to
 * the UI on a notification channel) <em>before</em> any producer knows the cid; once producers post,
 * the serialized drain is the only writer.</p>
 */
public final class StreamBridge {
    private static final Logger log = LoggerFactory.getLogger(StreamBridge.class);

    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    // one daemon timer thread arms/re-arms watchdogs; expiry work runs on virtual threads
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stream-bridge-watchdog");
        t.setDaemon(true);
        return t;
    });

    private StreamBridge() {}

    /**
     * Open a streaming rendezvous and bridge it to the request's reply lane as an SSE response.
     *
     * @param coordinator the pod's return-route coordinator ({@code SyncRuntime.coordinator()})
     * @param request     the incoming envelope of the {@code @EventInterceptor} facade
     * @param cid         the rendezvous correlation-id (typically {@code request.getCorrelationId()})
     * @param idleSeconds idle allowance between segments, applied to the HTTP edge and the watchdog alike
     * @return the writer bound to the request - already closed if the pod was at stream capacity (503)
     */
    public static EventStreamWriter open(ReturnRouteCoordinator coordinator, EventEnvelope request,
                                         String cid, long idleSeconds) {
        EventStreamWriter writer = new EventStreamWriter(request);
        writer.first(200, TEXT_EVENT_STREAM, idleSeconds);
        Session session = new Session(coordinator, writer, cid, idleSeconds * 1000);
        try {
            coordinator.beginStream(cid, session);
        } catch (IllegalStateException backPressure) {
            // head not committed yet, so this renders a proper HTTP error - deterministic like the edge's 503
            writer.fail(new AppException(503, backPressure.getMessage()));
            return writer;
        }
        session.arm();
        return writer;
    }

    /** Per-stream lifecycle: forwards segments, tracks activity, and owns the idle-expiry final drain. */
    private static final class Session implements Consumer<StreamSegment> {
        private final ReturnRouteCoordinator coordinator;
        private final EventStreamWriter writer;
        private final EventStreamSink forwarder;
        private final String cid;
        private final long idleMillis;
        private final AtomicReference<ScheduledFuture<?>> pending = new AtomicReference<>();
        private volatile long lastActivity = System.currentTimeMillis();
        private volatile boolean done = false;

        private Session(ReturnRouteCoordinator coordinator, EventStreamWriter writer, String cid,
                        long idleMillis) {
            this.coordinator = coordinator;
            this.writer = writer;
            this.forwarder = new EventStreamSink(writer);
            this.cid = cid;
            this.idleMillis = idleMillis;
        }

        @Override
        public void accept(StreamSegment segment) {
            lastActivity = System.currentTimeMillis();
            forwarder.accept(segment);
            if (segment.isTerminal()) {
                disarm();   // the drain that delivered the terminal also closes the stream and its keys
            }
        }

        private void arm() {
            schedule(idleMillis);
        }

        private void disarm() {
            done = true;
            ScheduledFuture<?> timer = pending.getAndSet(null);
            if (timer != null) {
                timer.cancel(false);
            }
        }

        private void schedule(long delayMillis) {
            if (!done) {
                pending.set(WATCHDOG.schedule(this::check, delayMillis, TimeUnit.MILLISECONDS));
            }
        }

        private void check() {
            if (done) {
                return;
            }
            long idleFor = System.currentTimeMillis() - lastActivity;
            if (idleFor < idleMillis) {
                schedule(idleMillis - idleFor);   // activity since the last arm - sleep out the remainder
            } else {
                // blocking Redis work does not belong on the timer thread
                Thread.startVirtualThread(this::expire);
            }
        }

        private void expire() {
            // ONE last-chance drain (design D4's confirmed nuance): a dropped final notification still
            // completes the render here - the same recovery cornerstone as the one-shot final read
            boolean completed = coordinator.finalDrain(cid);
            if (completed || done) {
                return;   // the drain (this one or a concurrent wake-up's) delivered the terminal
            }
            disarm();
            log.debug("Stream {} idle for {} ms - failing in-band and closing the rendezvous", cid, idleMillis);
            // if a concurrent drain just closed the writer, the CAS inside fail() makes this a no-op
            writer.fail(new AppException(408, "Stream idle timeout"));
            coordinator.closeStream(cid);
        }
    }
}
