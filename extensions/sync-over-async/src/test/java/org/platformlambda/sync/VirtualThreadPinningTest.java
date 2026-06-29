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
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingStream;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h2>Evidence: the blocking Lettuce <em>sync</em> API does not pin virtual threads</h2>
 *
 * <p>{@link ReturnRouteStore} and {@link ReturnRouteCoordinator} call Redis through Lettuce's
 * {@code connection.sync()} API, which <em>blocks the calling thread</em> until the reply arrives.
 * Mercury runs functions on virtual threads, so a fair reviewer will ask: does a blocking sync call
 * pin its carrier (platform) thread and defeat the point of virtual threads?</p>
 *
 * <p><b>It does not.</b> Lettuce performs the actual socket I/O on Netty event-loop (platform) threads;
 * the caller only awaits the result on a {@code CountDownLatch} inside Lettuce's {@code AsyncCommand}.
 * That latch is built on {@code AbstractQueuedSynchronizer} / {@code LockSupport.park}, which Java 21
 * retrofitted to be virtual-thread-aware: the virtual thread <em>unmounts</em> from its carrier for the
 * duration of the round-trip rather than holding it. Pinning would only occur if the thread blocked
 * while holding a {@code synchronized} monitor (or inside a native frame), which this await path does
 * not. Blocking sync on a virtual thread is therefore the recommended idiom here - simpler than the
 * reactive API and with the same throughput.</p>
 *
 * <p>This test measures it with a JFR {@link RecordingStream} listening for {@code jdk.VirtualThread
 * Pinned} events (the same signal behind {@code -Djdk.tracePinnedThreads}), threshold removed so even a
 * sub-millisecond pin against embedded Redis is caught.</p>
 *
 * <h3>Why we classify pins instead of asserting "zero total"</h3>
 *
 * <p>The JVM itself pins a carrier <em>once</em> while it lazily bootstraps {@code invokedynamic} call
 * sites - notably string concatenation, where {@code StringConcatFactory} generates a hidden class
 * inside a VM frame ({@code pinnedReason = "Native or VM frame on stack"}) - and while it links classes
 * on first use. Those pins are one-time, sub-millisecond, JDK-build-dependent, and have nothing to do
 * with I/O blocking. So this test (a) <b>warms up</b> the workload first to trigger that linkage outside
 * the measurement window, and (b) fails only on pins whose stack actually runs through Lettuce
 * ({@code io.lettuce.*}). Any residual JVM-internal pins are reported for transparency, not failed on.</p>
 */
class VirtualThreadPinningTest extends RedisTestBase {

    private static final System.Logger LOG = System.getLogger(VirtualThreadPinningTest.class.getName());

    private static final int VIRTUAL_THREADS = 100;
    private static final int OPS_PER_THREAD = 5;

    @Test
    void lettuceSyncCallsDoNotPinCarrierThreads() throws Exception {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            ReturnRouteStore store = new ReturnRouteStore(connection);

            // Warm-up: trigger one-time JVM linkage (invokedynamic string-concat call sites, class
            // loading) and Lettuce channel setup BEFORE measuring, so the recorded run reflects
            // steady-state behaviour rather than cold-start VM-frame pins.
            runBlockingRedisWorkloadOnVirtualThreads(store);

            List<String> lettucePins = new CopyOnWriteArrayList<>();
            List<String> jvmInternalPins = new CopyOnWriteArrayList<>();

            try (RecordingStream recording = new RecordingStream()) {
                recording.enable("jdk.VirtualThreadPinned").withoutThreshold();
                recording.onEvent("jdk.VirtualThreadPinned", event -> {
                    if (pinnedInsideLettuce(event)) {
                        lettucePins.add(event.toString());
                    } else {
                        jvmInternalPins.add(event.toString());
                    }
                });
                recording.startAsync();

                runBlockingRedisWorkloadOnVirtualThreads(store);

                recording.stop();   // flushes buffered events to the onEvent callback before returning
            }

            if (!jvmInternalPins.isEmpty()) {
                // Benign one-time JVM bootstrap pins (e.g. StringConcatFactory hidden-class generation).
                // Reported - not hidden - so the result is transparent, but not attributable to Lettuce.
                LOG.log(System.Logger.Level.INFO,
                        "Ignored {0} JVM-internal pin(s) unrelated to Lettuce (invokedynamic / class linking)",
                        jvmInternalPins.size());
            }

            assertTrue(lettucePins.isEmpty(),
                    () -> "Lettuce blocking sync calls pinned a carrier thread " + lettucePins.size()
                            + " time(s); the await should unmount the virtual thread, not pin it.\n"
                            + String.join("\n", lettucePins));
        }
    }

    /** @return true if the pinned virtual thread was blocked somewhere inside Lettuce ({@code io.lettuce.*}). */
    private static boolean pinnedInsideLettuce(RecordedEvent event) {
        RecordedStackTrace stack = event.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (RecordedFrame frame : stack.getFrames()) {
            RecordedMethod method = frame.getMethod();
            if (method != null && method.getType() != null
                    && method.getType().getName().startsWith("io.lettuce")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Each virtual thread blocks on a {@code CountDownLatch} await inside Lettuce for every call below.
     * If that await held a monitor (i.e. pinned), JFR would record a {@code jdk.VirtualThreadPinned}
     * event with {@code io.lettuce.*} frames on the stack. A single shared connection is used on purpose
     * - Lettuce multiplexes commands from many threads over one connection, which is exactly the
     * production return-route design.
     */
    private void runBlockingRedisWorkloadOnVirtualThreads(ReturnRouteStore store) throws Exception {
        List<Future<?>> results = new ArrayList<>();
        try (ExecutorService vthreads = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int worker = 0; worker < VIRTUAL_THREADS; worker++) {
                final int id = worker;
                results.add(vthreads.submit(() -> {
                    for (int i = 0; i < OPS_PER_THREAD; i++) {
                        String cid = "pin-" + id + "-" + i;
                        store.saveRoute(cid, "svc-return:pod-X", 30);
                        store.getRoute(cid);
                        store.saveResponse(cid, "payload", 30);
                        store.getResponse(cid);
                    }
                }));
            }
        }   // ExecutorService.close() blocks until every virtual thread has finished
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS);   // surface any failure raised inside a worker
        }
    }
}
