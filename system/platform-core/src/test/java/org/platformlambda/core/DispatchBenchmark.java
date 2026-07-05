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

import java.util.concurrent.atomic.AtomicLong;

/**
 * P5 fast-path benchmark: end-to-end dispatch throughput of a ServiceQueue. Fires N fire-and-forget events
 * at a fast multi-instance sink and times how long until all N are processed. Comparing
 * elastic.queue.dispatch=loop vs vthread shows whether the per-route virtual-thread hand-off regresses the
 * hot path. Fire-and-forget (no RPC reply routing) keeps the harness simple and robust.
 *
 * Gated on -Dbench.run=true. Run each mode:
 *   mvn -pl system/platform-core test -Dtest=DispatchBenchmark -Dbench.run=true
 *   mvn -pl system/platform-core test -Dtest=DispatchBenchmark -Dbench.run=true -Delastic.queue.dispatch=vthread
 */
class DispatchBenchmark {

    private static final String ROUTE = "dispatch.bench.sink";
    private static final AtomicLong PROCESSED = new AtomicLong(0);

    @Test
    @EnabledIfSystemProperty(named = "bench.run", matches = "true")
    void dispatchThroughput() throws Exception {
        int warmup = Integer.getInteger("bench.warmup", 20000);
        int iterations = Integer.getInteger("bench.iterations", 500000);
        String mode = AppConfigReader.getInstance().getProperty("elastic.queue.dispatch", "loop");

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
    }

    /** Send n fire-and-forget events, then wait until all have been processed. */
    private static void runBatch(EventEmitter po, String payload, int n) throws InterruptedException {
        long target = PROCESSED.get() + n;
        for (int i = 0; i < n; i++) {
            po.send(new EventEnvelope().setTo(ROUTE).setBody(payload));
        }
        long deadline = System.currentTimeMillis() + 60000;
        while (PROCESSED.get() < target) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("timed out: processed " + PROCESSED.get() + " of " + target);
            }
            Thread.sleep(1);
        }
    }
}
