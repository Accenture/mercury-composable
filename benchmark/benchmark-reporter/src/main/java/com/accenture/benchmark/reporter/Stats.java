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

package com.accenture.benchmark.reporter;

import java.util.Arrays;
import java.util.List;

/**
 * Latency summary computed from an array of raw nanosecond samples: exact percentiles (nearest-rank over a
 * sorted copy), mean/stddev, and a log-spaced histogram for charting. All latencies are exposed in
 * milliseconds. Computed once, after a workload finishes, so it never perturbs the measured hot path.
 *
 * @param count     number of recorded (successful) samples
 * @param minMs     minimum latency (ms)
 * @param meanMs    arithmetic mean (ms)
 * @param stddevMs  population standard deviation (ms)
 * @param p50       50th percentile (ms)
 * @param p90       90th percentile (ms)
 * @param p99       99th percentile (ms)
 * @param p999      99.9th percentile (ms)
 * @param p9999     99.99th percentile (ms)
 * @param maxMs     maximum latency (ms)
 * @param binCounts sample counts per histogram bin (aligned to {@link #EDGES_MS})
 */
public record Stats(long count, double minMs, double meanMs, double stddevMs,
                    double p50, double p90, double p99, double p999, double p9999, double maxMs,
                    List<Long> binCounts) {

    /** Upper edges (ms) of the log-spaced histogram bins; the last bin absorbs everything larger. */
    public static final double[] EDGES_MS = {
            0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000
    };

    /** Human-readable labels for {@link #EDGES_MS} (upper edge of each bin). */
    public static final String[] EDGE_LABELS = {
            "10µs", "20µs", "50µs", "0.1ms", "0.2ms", "0.5ms", "1ms", "2ms", "5ms", "10ms", "20ms", "50ms",
            "100ms", "200ms", "500ms", "1s", "2s", "5s", "10s+"
    };

    /**
     * @param ns raw latency samples in nanoseconds
     * @param n  number of valid entries in {@code ns} (may be less than {@code ns.length})
     */
    public static Stats compute(long[] ns, int n) {
        if (n <= 0) {
            return new Stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    java.util.Collections.nCopies(EDGES_MS.length, 0L));
        }
        long[] sorted = Arrays.copyOf(ns, n);
        Arrays.sort(sorted);
        double sum = 0;
        double sumSq = 0;
        long[] bins = new long[EDGES_MS.length];
        for (int i = 0; i < n; i++) {
            double ms = sorted[i] / 1e6;
            sum += ms;
            sumSq += ms * ms;
            int b = 0;
            while (b < EDGES_MS.length - 1 && ms > EDGES_MS[b]) {
                b++;
            }
            bins[b]++;
        }
        double mean = sum / n;
        double variance = Math.max(0, sumSq / n - mean * mean);
        return new Stats(n, sorted[0] / 1e6, mean, Math.sqrt(variance),
                pct(sorted, 50), pct(sorted, 90), pct(sorted, 99), pct(sorted, 99.9), pct(sorted, 99.99),
                sorted[n - 1] / 1e6, Arrays.stream(bins).boxed().toList());
    }

    /** Nearest-rank percentile (ms) from an ascending-sorted nanosecond array. */
    private static double pct(long[] sorted, double p) {
        int n = sorted.length;
        int rank = (int) Math.ceil(p / 100.0 * n);
        if (rank < 1) {
            rank = 1;
        }
        if (rank > n) {
            rank = n;
        }
        return sorted[rank - 1] / 1e6;
    }
}
