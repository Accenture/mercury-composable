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

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Self-contained, single-JVM end-to-end performance reporter for the Mercury framework. It registers one
 * echo {@link Worker} route (with {@code bench.consumers} instances) and runs a suite of scenarios against
 * it, in two groups, to show the whole robustness picture:
 *
 * <p><b>Normal operation (no back-pressure)</b> — arrival rate at or below capacity, so the ElasticQueue
 * stays under its 20-event memory buffer and never spills. These characterise a well-architected production
 * service:</p>
 * <ul>
 *   <li>RPC 1 → C — baseline request/reply round-trip (in-flight = 1).</li>
 *   <li>RPC C → C — balanced; throughput scales ~linearly, latency stays near baseline.</li>
 *   <li>RPC 2C → C — over-subscribed 2:1; latency rises ~2× (queueing), throughput holds at capacity.</li>
 *   <li>Callback C → C paced — async at a sustainable rate; consumers keep up, latency ≈ service time.</li>
 * </ul>
 *
 * <p><b>Overload (back-pressure engaged)</b> — the original stress case:</p>
 * <ul>
 *   <li>Callback flood → C — open-loop async, in-flight ≫ 20; events spill through the ElasticQueue and
 *       latency becomes queue-bounded (Little's law), but the system stays stable and loss-free.</li>
 * </ul>
 *
 * <p>It captures per-operation end-to-end latency and writes a self-contained HTML report (inline SVG
 * histogram + percentile plot + environment metadata). Because it needs only the in-JVM event bus, it runs
 * anywhere a JRE does — a real deployed environment or a benchmark pipeline. All parameters are system
 * properties, e.g. {@code java -Dbench.consumers=50 -Dbench.report=out.html -jar benchmark-reporter.jar}.</p>
 */
@MainApplication
public class BenchmarkApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkApp.class);

    private static final String WORKER = "benchmark.worker";
    private static final String NORMAL = "Normal operation (no back-pressure)";
    private static final String OVERLOAD = "Overload (back-pressure engaged)";

    private int ops;
    private int warmup;
    private int payloadBytes;
    private int consumers;
    private int callbackInflight;
    private int callbackProducers;
    private long pacingMicros;
    private long timeoutMs;
    private String reportPath;

    public static void main(String[] args) {
        org.platformlambda.core.system.AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        ops = Integer.getInteger("bench.ops", 200_000);
        warmup = Integer.getInteger("bench.warmup", 20_000);
        payloadBytes = Integer.getInteger("bench.payload", 256);
        consumers = Math.max(1, Integer.getInteger("bench.consumers", 50));
        callbackInflight = Math.max(1, Integer.getInteger("bench.callback.inflight", 2_000));
        callbackProducers = Math.max(1, Integer.getInteger("bench.callback.producers", 1));
        pacingMicros = Long.getLong("bench.callback.pacing.micros", 1_000L);
        timeoutMs = Long.getLong("bench.timeout", 30_000L);
        reportPath = System.getProperty("bench.report", "benchmark-report.html");

        Platform platform = Platform.getInstance();
        platform.registerPrivate(WORKER, new Worker(), consumers);

        // Run the (blocking) benchmark off the platform startup callback, then exit.
        Thread.ofPlatform().name("benchmark-driver").start(this::drive);
    }

    private void drive() {
        try {
            EventEmitter po = EventEmitter.getInstance();
            byte[] payload = new byte[payloadBytes];
            for (int i = 0; i < payloadBytes; i++) {
                payload[i] = (byte) (i & 0x7f);
            }
            int c = consumers;

            log.info("Warming up…");
            runRpc(po, payload, "warmup", "warmup", "", Math.min(c, 16), warmup);
            runCallback(po, payload, "warmup", "warmup", "", callbackProducers, 0, callbackInflight, warmup);

            List<WorkloadResult> results = new ArrayList<>();

            // --- Normal operation (no back-pressure): backlog stays within the 20-event memory buffer ---
            log.info("[1/5] RPC 1 → {}", c);
            results.add(runRpc(po, payload, "RPC · 1 → " + c + " consumers", NORMAL,
                    "One publisher, in-flight = 1: pure request/reply round-trip. Baseline latency; the "
                            + "ElasticQueue is idle.", 1, ops));

            log.info("[2/5] RPC {} → {}", c, c);
            results.add(runRpc(po, payload, "RPC · " + c + " → " + c + " consumers", NORMAL,
                    "Balanced (publishers = consumers): the backlog stays within the 20-event memory buffer, so "
                            + "it runs on the fast in-memory tier — throughput reaches the single-route dispatch "
                            + "ceiling (~an order of magnitude over 1→" + c + ") and latency stays sub-millisecond.",
                    c, ops));

            log.info("[3/5] Callback {} → {} paced ~{}µs", c, c, pacingMicros);
            results.add(runCallback(po, payload, "Callback · " + c + " → " + c + " consumers (paced)", NORMAL,
                    "Async callback at a sustainable rate: " + c + " publishers spaced ~" + pacingMicros
                            + "µs apart so consumers keep up. The backlog stays under the 20-event memory buffer "
                            + "— no disk spill, latency ≈ service time. Healthy steady-state async operation.",
                    c, pacingMicros, Integer.MAX_VALUE, ops));

            // --- Overload (back-pressure engaged): backlog exceeds the buffer and spills to disk ---
            log.info("[4/5] RPC {} → {} (over-subscribed 2:1)", 2 * c, c);
            results.add(runRpc(po, payload, "RPC · " + (2 * c) + " → " + c + " consumers", OVERLOAD,
                    "Over-subscribed 2:1: with " + (2 * c) + " publishers and " + c + " consumers, ~" + c
                            + " requests queue behind the workers — above the 20-event memory buffer, so events "
                            + "spill to disk. Throughput drops to the disk-spill ceiling (≈ the flood below) and "
                            + "latency rises well beyond a naive 2×. This is back-pressure via oversubscription.",
                    2 * c, ops));

            log.info("[5/5] Callback flood → {} (in-flight {})", c, callbackInflight);
            results.add(runCallback(po, payload, "Callback · flood → " + c + " consumers", OVERLOAD,
                    "Async callback with no pacing, up to " + String.format("%,d", callbackInflight)
                            + " in-flight — far above the 20-event memory buffer. Events spill through the "
                            + "ElasticQueue back-pressure buffer and latency becomes queue-bounded (Little's law), "
                            + "yet the system stays stable and loss-free.",
                    callbackProducers, 0, callbackInflight, ops));

            Map<String, String> env = environment();
            printSummary(env, results);
            Path out = Path.of(reportPath).toAbsolutePath();
            Files.writeString(out, HtmlReport.render(env, results));
            System.out.println("\nHTML report written to " + out);
            System.exit(0);
        } catch (Throwable e) {
            log.error("Benchmark failed", e);
            System.exit(1);
        }
    }

    /** Closed-loop RPC: {@code publishers} virtual-thread clients each block on {@code request()}. */
    private WorkloadResult runRpc(EventEmitter po, byte[] payload, String name, String category,
                                  String desc, int publishers, int totalOps) throws InterruptedException {
        int per = Math.max(1, totalOps / publishers);
        int total = per * publishers;
        long[] ns = new long[total];
        AtomicInteger idx = new AtomicInteger();
        AtomicLong failures = new AtomicLong();
        CountDownLatch done = new CountDownLatch(publishers);
        long t0 = System.nanoTime();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int p = 0; p < publishers; p++) {
                ex.submit(() -> {
                    for (int i = 0; i < per; i++) {
                        long s = System.nanoTime();
                        try {
                            po.request(new EventEnvelope().setTo(WORKER).setBody(payload), timeoutMs).get();
                            ns[idx.getAndIncrement()] = System.nanoTime() - s;
                        } catch (Exception e) {
                            failures.incrementAndGet();
                        }
                    }
                    done.countDown();
                });
            }
            done.await();
        }
        double elapsed = (System.nanoTime() - t0) / 1e9;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("publishers (in-flight)", String.valueOf(publishers));
        params.put("consumers", String.valueOf(consumers));
        params.put("operations", String.format("%,d", total));
        params.put("payload", payload.length + " bytes");
        return new WorkloadResult(name, category, desc, params, total, failures.get(), elapsed,
                Stats.compute(ns, idx.get()));
    }

    /**
     * Open-loop callback: {@code publishers} threads fire {@code asyncRequest()} without blocking, sharing one
     * global in-flight window (a semaphore). A per-fire {@code pacingMicros} pause (0 = flood) sets the offered
     * rate — pacing below capacity keeps the queue shallow (no back-pressure), while a flood drives it above
     * the memory buffer and into the ElasticQueue spill.
     */
    private WorkloadResult runCallback(EventEmitter po, byte[] payload, String name, String category,
                                       String desc, int publishers, long pacingMicros, int maxInflight,
                                       int totalOps) throws InterruptedException {
        int per = Math.max(1, totalOps / publishers);
        int total = per * publishers;
        long[] ns = new long[total];
        AtomicInteger idx = new AtomicInteger();
        AtomicLong failures = new AtomicLong();
        Semaphore permits = new Semaphore(maxInflight);
        CountDownLatch done = new CountDownLatch(total);
        long t0 = System.nanoTime();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int p = 0; p < publishers; p++) {
                ex.submit(() -> {
                    for (int i = 0; i < per; i++) {
                        try {
                            permits.acquire();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        final long s = System.nanoTime();
                        po.asyncRequest(new EventEnvelope().setTo(WORKER).setBody(payload), timeoutMs)
                                .onComplete(ar -> {
                                    if (ar.succeeded()) {
                                        ns[idx.getAndIncrement()] = System.nanoTime() - s;
                                    } else {
                                        failures.incrementAndGet();
                                    }
                                    permits.release();
                                    done.countDown();
                                });
                        if (pacingMicros > 0) {
                            LockSupport.parkNanos(pacingMicros * 1_000L);
                        }
                    }
                });
            }
            done.await();
        }
        double elapsed = (System.nanoTime() - t0) / 1e9;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("publishers", String.valueOf(publishers));
        params.put("consumers", String.valueOf(consumers));
        if (pacingMicros > 0) {
            params.put("pacing", pacingMicros + " µs/publisher");
        } else {
            params.put("max in-flight", String.format("%,d", maxInflight));
        }
        params.put("operations", String.format("%,d", total));
        params.put("payload", payload.length + " bytes");
        return new WorkloadResult(name, category, desc, params, total, failures.get(), elapsed,
                Stats.compute(ns, idx.get()));
    }

    private Map<String, String> environment() {
        AppConfigReader config = AppConfigReader.getInstance();
        String store = config.getProperty("elastic.queue.store", "file");
        Map<String, String> env = new LinkedHashMap<>();
        env.put("Generated", ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        env.put("Java", System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")");
        env.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " / " + System.getProperty("os.arch"));
        env.put("Available processors", String.valueOf(Runtime.getRuntime().availableProcessors()));
        env.put("Max heap", (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
        env.put("ElasticQueue store", store);
        env.put("Dispatch", "bdb".equalsIgnoreCase(store) ? "event-loop (inline)" : "virtual-thread (off-loop)");
        env.put("transient.data.store", config.getProperty("transient.data.store", "/tmp/reactive"));
        return env;
    }

    private void printSummary(Map<String, String> env, List<WorkloadResult> results) {
        StringBuilder sb = new StringBuilder("\n===== benchmark-reporter =====\n");
        env.forEach((k, v) -> sb.append(String.format("  %-22s %s%n", k, v)));
        String lastCategory = "";
        for (WorkloadResult r : results) {
            if (!r.category().equals(lastCategory)) {
                sb.append(String.format("%n-- %s --%n", r.category()));
                lastCategory = r.category();
            }
            Stats s = r.stats();
            sb.append(String.format("%n%s%n", r.name()));
            sb.append(String.format("  throughput=%,.0f ops/s  ok=%,d  failures=%,d  elapsed=%.2fs%n",
                    r.throughput(), s.count(), r.failures(), r.elapsedSec()));
            sb.append(String.format("  latency ms: mean=%.3f p50=%.3f p90=%.3f p99=%.3f p99.9=%.3f "
                            + "p99.99=%.3f max=%.3f%n",
                    s.meanMs(), s.p50(), s.p90(), s.p99(), s.p999(), s.p9999(), s.maxMs()));
        }
        sb.append("==============================\n");
        System.out.print(sb);
    }
}
