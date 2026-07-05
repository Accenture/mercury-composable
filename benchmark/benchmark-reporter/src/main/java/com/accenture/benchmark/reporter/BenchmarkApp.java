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

import java.io.IOException;
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

/**
 * Self-contained, single-JVM end-to-end performance reporter for the Mercury framework.
 *
 * <p>It registers one echo {@link Worker} route and drives it two ways in the same process:
 * <ul>
 *   <li><b>RPC</b> — closed-loop {@code request()} (each caller blocks for the reply, so in-flight is
 *       bounded by the client concurrency; below the 20-event memory buffer this does not exercise the
 *       ElasticQueue).</li>
 *   <li><b>Callback</b> — open-loop {@code asyncRequest()} with a bounded in-flight window well above the
 *       memory buffer, so events queue at the worker and spill through the ElasticQueue back-pressure
 *       buffer. This is the path that stresses the disk-backed FIFO.</li>
 * </ul>
 * It captures per-operation end-to-end latency, then writes a self-contained HTML report (inline SVG
 * histogram + percentile plot + environment metadata) and exits. Because it needs only the in-JVM event
 * bus, it runs anywhere a JRE does — including a real deployed environment or a benchmark pipeline.</p>
 *
 * <p>All parameters are system properties so a pipeline can override them:
 * <pre>
 *   java -Dbench.ops=500000 -Dbench.callback.inflight=5000 -Dbench.report=out.html \
 *        -Delastic.queue.store=file -jar benchmark-reporter.jar
 * </pre></p>
 */
@MainApplication
public class BenchmarkApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkApp.class);

    private static final String WORKER = "benchmark.worker";

    private int ops;
    private int warmup;
    private int payloadBytes;
    private int rpcConcurrency;
    private int callbackInflight;
    private int callbackProducers;
    private int workers;
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
        rpcConcurrency = Math.max(1, Integer.getInteger("bench.rpc.concurrency", 1));
        callbackInflight = Math.max(1, Integer.getInteger("bench.callback.inflight", 2_000));
        callbackProducers = Math.max(1, Integer.getInteger("bench.callback.producers", 1));
        workers = Math.max(1, Integer.getInteger("bench.workers", 1));
        timeoutMs = Long.getLong("bench.timeout", 30_000L);
        reportPath = System.getProperty("bench.report", "benchmark-report.html");

        Platform platform = Platform.getInstance();
        platform.registerPrivate(WORKER, new Worker(), workers);

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

            log.info("Warming up ({} ops per workload)…", warmup);
            runRpc(po, payload, rpcConcurrency, warmup);
            runCallback(po, payload, callbackInflight, callbackProducers, warmup);

            log.info("Measuring RPC ({} ops, concurrency {})…", ops, rpcConcurrency);
            WorkloadResult rpc = runRpc(po, payload, rpcConcurrency, ops);
            log.info("Measuring callback ({} ops, in-flight {}, producers {})…",
                    ops, callbackInflight, callbackProducers);
            WorkloadResult callback = runCallback(po, payload, callbackInflight, callbackProducers, ops);

            List<WorkloadResult> results = new ArrayList<>(List.of(rpc, callback));
            Map<String, String> env = environment();
            printSummary(env, results);

            String html = HtmlReport.render(env, results);
            Path out = Path.of(reportPath).toAbsolutePath();
            Files.writeString(out, html);
            System.out.println("\nHTML report written to " + out);
            System.exit(0);
        } catch (Throwable e) {
            log.error("Benchmark failed", e);
            System.exit(1);
        }
    }

    /** Closed-loop RPC: {@code concurrency} virtual-thread clients each block on {@code request()}. */
    private WorkloadResult runRpc(EventEmitter po, byte[] payload, int concurrency, int totalOps)
            throws InterruptedException {
        int per = Math.max(1, totalOps / concurrency);
        int total = per * concurrency;
        long[] ns = new long[total];
        AtomicInteger idx = new AtomicInteger();
        AtomicLong failures = new AtomicLong();
        CountDownLatch done = new CountDownLatch(concurrency);
        long t0 = System.nanoTime();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int c = 0; c < concurrency; c++) {
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
        params.put("operations", String.format("%,d", total));
        params.put("client concurrency (max in-flight)", String.valueOf(concurrency));
        params.put("worker instances", String.valueOf(workers));
        params.put("payload", payload.length + " bytes");
        return new WorkloadResult("RPC (request / reply)",
                "Closed-loop: each client thread blocks on request() until the reply arrives, so in-flight "
                        + "is bounded by client concurrency. Below the 20-event memory buffer the ElasticQueue "
                        + "is not exercised — this measures pure event round-trip.",
                params, total, failures.get(), elapsed, Stats.compute(ns, idx.get()));
    }

    /**
     * Open-loop callback: {@code producers} threads fire {@code asyncRequest()} without blocking, sharing one
     * global in-flight window (a semaphore). Multiple producers mimic many concurrent async clients and lift
     * the single-producer send-rate ceiling, so the measured limit reflects the framework (per-route dispatch
     * + workers), not one caller thread. In-flight well above the 20-event memory buffer makes events queue at
     * the worker and spill through the ElasticQueue back-pressure buffer.
     */
    private WorkloadResult runCallback(EventEmitter po, byte[] payload, int inflight, int producers, int totalOps)
            throws InterruptedException {
        int per = Math.max(1, totalOps / producers);
        int total = per * producers;
        long[] ns = new long[total];
        AtomicInteger idx = new AtomicInteger();
        AtomicLong failures = new AtomicLong();
        Semaphore permits = new Semaphore(inflight);
        CountDownLatch done = new CountDownLatch(total);
        long t0 = System.nanoTime();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int p = 0; p < producers; p++) {
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
                    }
                });
            }
            done.await();
        }
        double elapsed = (System.nanoTime() - t0) / 1e9;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("operations", String.format("%,d", total));
        params.put("max in-flight", String.format("%,d", inflight));
        params.put("producer threads", String.valueOf(producers));
        params.put("worker instances", String.valueOf(workers));
        params.put("payload", payload.length + " bytes");
        return new WorkloadResult("Callback (async request)",
                "Open-loop: producer threads fire asyncRequest() without blocking, sharing a global in-flight "
                        + "window. With in-flight well above the 20-event memory buffer, events queue at the "
                        + "worker and spill through the ElasticQueue back-pressure buffer — the path that "
                        + "exercises the disk-backed FIFO. Multiple producers mimic concurrent async clients.",
                params, total, failures.get(), elapsed, Stats.compute(ns, idx.get()));
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
        for (WorkloadResult r : results) {
            Stats s = r.stats();
            sb.append(String.format("%n%s  (%s)%n", r.name(), r.description().split("\\.")[0]));
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
