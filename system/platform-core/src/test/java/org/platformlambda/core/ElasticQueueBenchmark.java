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
import org.platformlambda.core.util.ElasticQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * P0 benchmark (design: draft-design-specs/elastic_queue_file_fifo_design.md).
 *
 * NOT part of the normal suite — a measurement harness gated on -Dbench.run=true (and the class name
 * does not match surefire's *Test pattern), so `mvn test` never runs it. Run manually:
 *   mvn -pl system/platform-core test -Dtest=ElasticQueueBenchmark -Dbench.run=true \
 *       -Dbench.seconds=120 -Dbench.payload=1024 -Dbench.backlog=10000
 *
 * It sustains a backlog above ElasticQueue.MEMORY_BUFFER so every write/read hits the BDB disk tier,
 * and records the wall-clock latency of each write() and read() call. Because ServiceQueue invokes
 * write()/read() INLINE on the Vert.x event-loop thread, that per-op latency is exactly what blocks
 * the event loop. The tail (p99/p999/max) + the stall timeline is the thing we want to see: BDB's
 * checkpointer (every 1 min here) and log cleaner run on background threads but contend with the
 * inline put/get for latches, I/O and GC — surfacing as latency spikes.
 */
class ElasticQueueBenchmark {

    @Test
    @EnabledIfSystemProperty(named = "bench.run", matches = "true")
    void bdbSpillLatencyProfile() {
        int seconds = Integer.getInteger("bench.seconds", 120);
        int payload = Integer.getInteger("bench.payload", 1024);
        int backlog = Integer.getInteger("bench.backlog", 10000);
        long stallNs = Long.getLong("bench.stallMs", 20) * 1_000_000L;

        byte[] body = new byte[payload];
        for (int i = 0; i < payload; i++) {
            body[i] = (byte) (i & 0x7f);
        }
        byte[] event = new EventEnvelope().setBody(body).toBytes();

        Histogram writes = new Histogram();
        Histogram reads = new Histogram();
        List<String> stalls = new ArrayList<>();
        long start = System.currentTimeMillis();

        ElasticQueue q = new ElasticQueue("benchmark.route");
        try {
            // pre-fill the backlog so both counters climb past MEMORY_BUFFER (all ops hit disk)
            for (int i = 0; i < backlog; i++) {
                q.write(event);
            }
            System.out.printf("Pre-filled backlog=%d, payload=%dB. Sustaining spill for %ds…%n",
                    backlog, payload, seconds);

            long deadline = start + seconds * 1000L;
            long ops = 0;
            long lastReport = start;
            long secWrites = 0;
            long secStalls = 0;
            while (System.currentTimeMillis() < deadline) {
                // keep depth ~constant: one write + one read per iteration, so every op is a disk op
                long t0 = System.nanoTime();
                q.write(event);
                long wNs = System.nanoTime() - t0;
                writes.record(wNs);

                long t1 = System.nanoTime();
                byte[] out = q.read();
                long rNs = System.nanoTime() - t1;
                if (out.length > 0) {
                    reads.record(rNs);
                }
                ops++;
                secWrites++;
                long worst = Math.max(wNs, rNs);
                if (worst >= stallNs) {
                    secStalls++;
                    if (stalls.size() < 500) {
                        stalls.add(String.format("  t=%5.1fs  %s stalled %.1f ms",
                                (System.currentTimeMillis() - start) / 1000.0,
                                wNs >= rNs ? "write" : "read", worst / 1_000_000.0));
                    }
                }
                long now = System.currentTimeMillis();
                if (now - lastReport >= 5000) {
                    System.out.printf("  [%3ds] %,d ops/5s  (stalls>%dms this window: %d)%n",
                            (int) ((now - start) / 1000), secWrites, stallNs / 1_000_000L, secStalls);
                    lastReport = now;
                    secWrites = 0;
                    secStalls = 0;
                }
            }

            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            System.out.println("\n================ ElasticQueue (BDB) spill latency ================");
            System.out.printf("duration=%.0fs  ops=%,d  throughput=%,.0f ops/s%n", elapsed, ops, ops / elapsed);
            System.out.println("(latency = time the event-loop thread is blocked inside ElasticQueue per op)\n");
            System.out.printf("%-8s %10s %10s %10s %10s %10s %10s %12s%n",
                    "op", "p50", "p90", "p99", "p99.9", "max", ">10ms", ">50ms/>100ms");
            writes.print("write");
            reads.print("read");
            System.out.printf("%nStalls (>%dms) captured: %d%s%n",
                    stallNs / 1_000_000L, stalls.size(), stalls.size() == 500 ? " (capped)" : "");
            for (String s : stalls) {
                System.out.println(s);
            }
            System.out.println("==================================================================");
        } finally {
            q.destroy();
        }
    }

    /** Fixed 1-microsecond-resolution histogram up to 1s, with an overflow bucket + true max. */
    private static final class Histogram {
        private static final int BUCKETS = 1_000_000; // 1us .. 1s
        private final long[] us = new long[BUCKETS];
        private long overflow = 0;
        private long count = 0;
        private long maxNs = 0;
        private long over10 = 0, over50 = 0, over100 = 0;

        void record(long nanos) {
            count++;
            if (nanos > maxNs) {
                maxNs = nanos;
            }
            if (nanos >= 10_000_000L) { over10++; }
            if (nanos >= 50_000_000L) { over50++; }
            if (nanos >= 100_000_000L) { over100++; }
            int bucket = (int) (nanos / 1000);
            if (bucket < BUCKETS) {
                us[bucket]++;
            } else {
                overflow++;
            }
        }

        /** @return percentile latency in milliseconds. */
        private double pct(double p) {
            long target = (long) Math.ceil(count * p / 100.0);
            long cum = 0;
            for (int i = 0; i < BUCKETS; i++) {
                cum += us[i];
                if (cum >= target) {
                    return i / 1000.0;
                }
            }
            return maxNs / 1_000_000.0; // in the overflow (>1s) tail
        }

        void print(String label) {
            System.out.printf("%-8s %9.3f %9.3f %9.3f %9.3f %9.3f %10d %6d/%d%n",
                    label, pct(50), pct(90), pct(99), pct(99.9), maxNs / 1_000_000.0,
                    over10, over50, over100);
        }
    }
}
