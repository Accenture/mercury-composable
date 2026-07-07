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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P5 fast-path benchmark: end-to-end dispatch throughput of a ServiceQueue. Fires N fire-and-forget events
 * at a fast multi-instance sink and times how long until all N are processed. Dispatch mode follows the
 * store (bdb⇒loop, file⇒vthread), so the two valid modes are compared by selecting the store. Fire-and-forget
 * (no RPC reply routing) keeps the harness simple and robust.
 *
 * Gated on -Dbench.run=true. Run each mode:
 *   mvn -pl system/platform-core test -Dtest=DispatchBenchmark -Dbench.run=true                          (bdb+loop)
 *   mvn -pl system/platform-core test -Dtest=DispatchBenchmark -Dbench.run=true -Delastic.queue.store=file (file+vthread)
 */
class DispatchBenchmark {

    private static final String ROUTE = "dispatch.bench.sink";
    private static final AtomicLong PROCESSED = new AtomicLong(0);

    // fast-path overhead test (distinct route + state so it can run independently of the throughput test)
    private static final String TIMED_ROUTE = "dispatch.bench.timed";
    private static final AtomicLong TIMED_COUNT = new AtomicLong(0);
    private static volatile long[] timedLatencies;

    @Test
    @EnabledIfSystemProperty(named = "bench.run", matches = "true")
    void dispatchThroughput() throws Exception {
        int warmup = Integer.getInteger("bench.warmup", 20000);
        int iterations = Integer.getInteger("bench.iterations", 500000);
        String store = AppConfigReader.getInstance().getProperty("elastic.queue.store", "bdb");
        String mode = "file".equalsIgnoreCase(store) ? "vthread" : "loop";

        Platform platform = Platform.getInstance();
        if (!platform.hasRoute(ROUTE)) {
            LambdaFunction sink = (headers, input, instance) -> {
                PROCESSED.incrementAndGet();
                return null; // fire-and-forget: return is discarded
            };
            platform.registerPrivate(ROUTE, sink, 5);
        }
        EventEmitter po = EventEmitter.getInstance();
        String payload = "x".repeat(256);

        runBatch(po, payload, warmup);       // warm up
        PROCESSED.set(0);
        long t0 = System.nanoTime();
        runBatch(po, payload, iterations);   // timed
        double elapsedSec = (System.nanoTime() - t0) / 1_000_000_000.0;

        System.out.printf("%n========= ServiceQueue dispatch throughput [dispatch=%s] =========%n", mode);
        System.out.printf("events=%,d  processed=%,d  elapsed=%.2fs  throughput=%,.0f events/s%n",
                iterations, PROCESSED.get(), elapsedSec, iterations / elapsedSec);
        System.out.println("================================================================");
        assertEquals(iterations, PROCESSED.get(), "every dispatched event should be processed");
    }

    /**
     * Fast-path dispatch OVERHEAD: measures the send→worker latency on the non-back-pressure path
     * (in-flight=1 sequential + 5 spare-ready worker instances ⇒ a worker is always ready ⇒ direct
     * dispatch, no ElasticQueue spill). The loop-vs-vthread delta of this latency IS the cost of the
     * ServiceQueue mailbox hand-off (loop enqueue → per-route VT take → dispatch to WorkerQueue).
     */
    @Test
    @EnabledIfSystemProperty(named = "bench.run", matches = "true")
    void fastPathDispatchOverhead() {
        int warmup = Integer.getInteger("bench.warmup", 10000);
        int n = Integer.getInteger("bench.iterations", 200000);
        String store = AppConfigReader.getInstance().getProperty("elastic.queue.store", "bdb");
        String mode = "file".equalsIgnoreCase(store) ? "vthread" : "loop";
        Platform platform = Platform.getInstance();
        if (!platform.hasRoute(TIMED_ROUTE)) {
            LambdaFunction sink = (headers, input, instance) -> {
                long sent = input instanceof Number num ? num.longValue() : Long.parseLong(String.valueOf(input));
                long lat = System.nanoTime() - sent;
                long idx = TIMED_COUNT.getAndIncrement();
                long[] arr = timedLatencies;
                if (arr != null && idx < arr.length) {
                    arr[(int) idx] = lat;
                }
                return null;
            };
            platform.registerPrivate(TIMED_ROUTE, sink, 5); // spare ready workers ⇒ always fast path
        }
        EventEmitter po = EventEmitter.getInstance();
        timedLatencies = new long[warmup];
        TIMED_COUNT.set(0);
        sequential(po, warmup);
        long[] lat = new long[n];
        timedLatencies = lat;
        TIMED_COUNT.set(0);
        sequential(po, n);

        Arrays.sort(lat);
        System.out.printf("%n===== ServiceQueue fast-path dispatch overhead [dispatch=%s] =====%n", mode);
        System.out.printf("in-flight=1 sequential, N=%,d  (send->worker dispatch latency, microseconds)%n", n);
        System.out.printf("p50=%.2f  p90=%.2f  p99=%.2f  p99.9=%.2f  max=%.2f%n",
                us(lat, 50), us(lat, 90), us(lat, 99), us(lat, 99.9), lat[n - 1] / 1000.0);
        System.out.println("================================================================");
        assertEquals(n, TIMED_COUNT.get(), "every timed event should be processed");
    }

    /** In-flight=1: send one timestamped event, wait until it is processed, repeat. */
    private static void sequential(EventEmitter po, int count) {
        long deadline = System.currentTimeMillis() + 120000;
        for (int i = 0; i < count; i++) {
            long target = i + 1L;
            po.send(new EventEnvelope().setTo(TIMED_ROUTE).setBody(System.nanoTime()));
            while (TIMED_COUNT.get() < target) {
                Thread.onSpinWait();
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("timed out at " + i);
                }
            }
        }
    }

    private static double us(long[] sorted, double pct) {
        int idx = (int) Math.min(sorted.length - 1L, Math.round(sorted.length * pct / 100.0));
        return sorted[idx] / 1000.0;
    }

    /** Send n fire-and-forget events, then wait until all have been processed. */
    private static void runBatch(EventEmitter po, String payload, int n) {
        long target = PROCESSED.get() + n;
        for (int i = 0; i < n; i++) {
            po.send(new EventEnvelope().setTo(ROUTE).setBody(payload));
        }
        Utility util = Utility.getInstance();
        long deadline = System.currentTimeMillis() + 60000;
        while (PROCESSED.get() < target) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("timed out: processed " + PROCESSED.get() + " of " + target);
            }
            util.sleep(1);
        }
    }
}
