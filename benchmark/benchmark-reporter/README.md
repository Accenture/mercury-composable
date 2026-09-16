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
  *while* a background flood hammers `benchmark.worker`'s ElasticQueue. Spill runs **off** the event loop on
  the flooded route's own virtual thread, so the probe should stay fast no matter how hard the background
  route is pushed. This is the scenario that justified off-loop dispatch in the first place (see the
  [historical store analysis](analysis/README.md)), and it is the most valuable regression signal here: if a
  future change puts blocking work back on the shared loop, this probe's tail is where it shows.

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

### Building
This module is **deliberately not in the reactor** (commented out in the root `pom.xml`), so build it
standalone — a `-pl … -am` invocation fails with "Could not find the selected project in the reactor":
```bash
mvn clean install -DskipTests                 # install the engine into ~/.m2 first
mvn clean package -DskipTests -f benchmark/benchmark-reporter/pom.xml
```

### Validating a milestone release
Run the suite on the release candidate and compare against the previous milestone's report — same machine,
same flags, otherwise the comparison is noise:
```bash
TS=$(date -u +%Y%m%d-%H%M%SZ)
java -Dbench.report=benchmark/benchmark-reporter/analysis/v4.12.10-$TS.html \
     -jar benchmark/benchmark-reporter/target/benchmark-reporter.jar
```
Record the finding next to the report as `v<version>-$TS.md`, add a row to the milestone table in
[`analysis/README.md`](analysis/README.md), and add an entry to [`BENCHMARK-LOG.md`](BENCHMARK-LOG.md)
— the running release-over-release record.
The report records the framework version and environment metadata in its header, so a saved HTML file stays
interpretable long after the run. For latency-sensitive perf runs, point the spill at tmpfs:
`-Dtransient.data.store=/dev/shm/reactive`.

## Scope
This module supersedes the retired `benchmark-client` (a REST/WebSocket load client): it is a self-contained,
single-JVM performance harness that needs no external load generator.

It began as an A/B harness for the two ElasticQueue stores. Berkeley DB was retired in **v4.12.10**, so
there is one store and one dispatch mode now, and this is a **single-store benchmark of the in-memory event
system** — a documented performance baseline for validating milestone releases. The original A/B analysis
and both report snapshots are kept in [`analysis/`](analysis/README.md) as the historical record of why the
store changed.
