# benchmark-reporter

A self-contained, single-JVM end-to-end performance harness for the Mercury framework. It drives one echo
service two ways and writes a **self-contained HTML report** (inline SVG histogram + percentile plot + a
statistics table + environment metadata — no external files, so it opens anywhere):

- **RPC** (`request()`) — closed-loop request/reply. In-flight is bounded by client concurrency; below the
  20-event memory buffer this does **not** exercise the ElasticQueue, so it measures pure event round-trip.
- **Callback** (`asyncRequest()`) — open-loop async, up to a bounded in-flight window. With in-flight well
  above the memory buffer, events queue at the worker and spill through the **ElasticQueue** back-pressure
  buffer. This is the path that stresses the disk-backed FIFO.

Because it needs only the in-JVM event bus, it runs anywhere a JRE does — a laptop, a real deployed
environment, or a benchmark pipeline. Use it to estimate framework performance across environments and to
re-measure while tuning (e.g. A/B the `file` vs `bdb` ElasticQueue store).

## Build
```bash
# from the repo root (-am also builds platform-core from source)
mvn -pl benchmark/benchmark-reporter -am package -DskipTests
```
Produces an executable jar: `benchmark/benchmark-reporter/target/benchmark-reporter.jar`.

## Run
```bash
# NOTE: -D system properties MUST come BEFORE -jar (after -jar they are program args and ignored)
java -Dbench.report=report.html -jar benchmark/benchmark-reporter/target/benchmark-reporter.jar
```
The report is written to `bench.report` (default `benchmark-report.html` in the current directory) and a
summary is printed to stdout. The process exits when done, so it drops cleanly into a pipeline.

### Parameters (all optional; system properties)
| property                   | default | meaning                                                           |
|----------------------------|---------|-------------------------------------------------------------------|
| `bench.ops`                | 200000  | timed operations per workload                                     |
| `bench.warmup`             | 20000   | warm-up operations per workload (discarded)                       |
| `bench.payload`            | 256     | request body size in bytes                                        |
| `bench.rpc.concurrency`    | 1       | RPC client threads = max in-flight (1 ⇒ pure round-trip latency)  |
| `bench.callback.inflight`  | 2000    | callback max in-flight (≫ 20 ⇒ exercises the ElasticQueue spill)  |
| `bench.callback.producers` | 1       | callback producer threads sharing the in-flight window (>1 mimics concurrent async clients; lifts the single-producer send ceiling) |
| `bench.workers`            | 1       | worker route instances (lower ⇒ deeper back-pressure on callback) |
| `bench.timeout`            | 30000   | per-request timeout (ms)                                          |
| `bench.report`             | benchmark-report.html | output HTML path                                    |

### A/B the ElasticQueue store
The report records the active store and dispatch mode. Compare the default file FIFO against the legacy
Berkeley DB store:
```bash
java -Dbench.report=file.html -jar .../benchmark-reporter.jar                       # default: file + off-loop vthread
java -Delastic.queue.store=bdb -Dbench.report=bdb.html -jar .../benchmark-reporter.jar   # legacy: bdb + inline loop
```
For latency-sensitive perf runs, point the spill at tmpfs: `-Dtransient.data.store=/dev/shm/reactive`.

## Scope
This module is intentionally separate from `benchmark-client` (a REST/WebSocket load client), which is left
untouched for a future sprint.
