# benchmark-reporter

A self-contained, single-JVM end-to-end performance harness for the Mercury framework. It registers one echo
service (with `bench.consumers` worker instances) and runs a **suite of scenarios** against it, then writes a
**self-contained HTML report** (inline SVG histogram + percentile plot + a statistics table + environment
metadata — no external files, so it opens anywhere). The scenarios come in two groups so the report shows the
whole robustness picture — predictable healthy operation *and* graceful overload:

**Normal operation (no back-pressure)** — arrival rate ≤ capacity, so the ElasticQueue stays within its
20-event in-memory buffer and never spills. These characterise a well-architected production service:
- **RPC 1 → C** — one publisher, in-flight = 1: baseline request/reply round-trip.
- **RPC C → C** — balanced (publishers = consumers): backlog stays in the in-memory tier, so throughput
  reaches the single-route dispatch ceiling (~an order of magnitude over 1→C) and latency stays sub-ms.
- **Callback C → C (paced)** — async at a sustainable rate (publishers spaced `bench.callback.pacing.micros`
  apart) so consumers keep up: latency ≈ service time.

**Overload (back-pressure engaged)** — backlog exceeds the 20-event buffer and spills to disk; both scenarios
land at the same disk-spill throughput ceiling, and the system stays stable and loss-free:
- **RPC 2C → C** — over-subscribed 2:1: ~C requests queue (> 20), so it spills. Throughput drops to the
  spill ceiling and latency rises well beyond a naive 2× — back-pressure via oversubscription.
- **Callback flood → C** — open-loop `asyncRequest()`, in-flight ≫ 20: events spill through the ElasticQueue
  and latency becomes queue-bounded (Little's law).

**Mixed workload (latency isolation under load)** — the production-critical question:
- **Latency probe under background flood** — a paced RPC on a *separate* route (`benchmark.probe`) measured
  *while* a background flood hammers `benchmark.worker`'s ElasticQueue. With **file+vthread** the spill runs
  **off** the event loop, so the probe stays fast; with **bdb+loop** the spill runs **inline** on the shared
  event loop, inflating the probe's tail. Run this under both stores — it's where the isolation win shows,
  which a single isolated workload cannot reveal.

Because it needs only the in-JVM event bus, it runs anywhere a JRE does — a laptop, a real deployed
environment, or a benchmark pipeline. Use it to estimate framework performance across environments and to
re-measure while tuning (e.g. A/B the `file` vs `bdb` ElasticQueue store). `C` = `bench.consumers`.

## Build
```bash
# from the repo root (-am also builds platform-core from source)
mvn -pl benchmark/benchmark-reporter -am package -DskipTests
```
Produces an executable jar: `benchmark/benchmark-reporter/target/benchmark-reporter.jar`.

## Run
```bash
# NOTE: -D system properties MUST come BEFORE -jar (after -jar they are program args and ignored)
java -Dbench.report=/tmp/report.html -jar benchmark/benchmark-reporter/target/benchmark-reporter.jar
```
The report is written to `bench.report` and a summary is printed to stdout; the process exits when done, so
it drops cleanly into a pipeline. Write reports outside the repo (e.g. under `/tmp`) so generated HTML isn't
mixed into the working tree — the committed reference reports live in [`analysis/`](analysis/README.md).
(If `bench.report` is omitted it defaults to `/tmp/benchmark-report.html`.)

### Parameters (all optional; system properties)
| property                       | default | meaning                                                       |
|--------------------------------|---------|---------------------------------------------------------------|
| `bench.ops`                    | 200000  | timed operations per scenario                                 |
| `bench.warmup`                 | 20000   | warm-up operations per path (discarded)                       |
| `bench.payload`                | 256     | request body size in bytes                                    |
| `bench.consumers`              | 50      | worker instances `C`; sets the RPC publisher counts (1, C, 2C) and the paced-callback publisher count |
| `bench.callback.pacing.micros` | 1000    | per-publisher pause in the paced (healthy) callback scenario  |
| `bench.callback.inflight`      | 2000    | flood-scenario max in-flight (≫ 20 ⇒ exercises the ElasticQueue spill) |
| `bench.callback.producers`     | 1       | flood-scenario producer threads sharing the in-flight window (>1 mimics concurrent async clients) |
| `bench.probe.ops`              | 3000    | mixed-workload probe requests (the latency-sensitive path)    |
| `bench.probe.pacing.micros`    | 2000    | pause between probe requests (2000 µs ⇒ ~500 probes/s)        |
| `bench.timeout`                | 30000   | per-request timeout (ms)                                      |
| `bench.report`                 | /tmp/benchmark-report.html | output HTML path                           |

### A/B the ElasticQueue store
The report records the active store and dispatch mode. Compare the default file FIFO against the legacy
Berkeley DB store (a saved A/B lives in [`analysis/`](analysis/README.md)):
```bash
# default: file + off-loop vthread
java -Dbench.report=/tmp/file-vthread.html -jar benchmark/benchmark-reporter/target/benchmark-reporter.jar
# legacy: bdb + inline loop
java -Delastic.queue.store=bdb -Dbench.report=/tmp/bdb-loop.html -jar benchmark/benchmark-reporter/target/benchmark-reporter.jar
```
For latency-sensitive perf runs, point the spill at tmpfs: `-Dtransient.data.store=/dev/shm/reactive`.

## Scope
This module is intentionally separate from `benchmark-client` (a REST/WebSocket load client), which is left
untouched for a future sprint.
