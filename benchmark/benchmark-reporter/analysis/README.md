# ElasticQueue store analysis — `file`+vthread vs `bdb`+loop

Reference analysis and result snapshots comparing the two ElasticQueue backends behind the ServiceQueue
back-pressure buffer, produced with [`benchmark-reporter`](../README.md).

- [`file-vthread.html`](file-vthread.html) — default: file-backed segmented FIFO + off-loop virtual-thread dispatch
- [`bdb-loop.html`](bdb-loop.html) — legacy fallback: Berkeley DB + inline event-loop dispatch

> **These are dev-laptop figures — indicative, not SLAs.** Absolute numbers vary by hardware/OS; re-run
> `benchmark-reporter` in each target environment. The *relative* findings below are what matter.

## Recommendation

**Default to `file`+vthread for production installations; keep `bdb` as a one-flag fallback
(`elastic.queue.store=bdb`).** Not because it's a faster store — in isolation BDB is actually
competitive-to-faster — but because in a real **multi-route** service that shares one Vert.x event loop, the
file store keeps back-pressure spill **off** that loop, so a burst on one route does not inflate the latency
of unrelated latency-sensitive routes. That isolation is the robustness property that matters at scale, and
it is the one thing a single isolated workload cannot show.

## Method

- Single JVM, one echo worker route with `C` = 50 consumer instances, 200k ops/scenario, 256-byte payload.
- Apple Silicon laptop (12 cores), JDK 21, `transient.data.store=/tmp/reactive`.
- Latency is end-to-end (caller → reply); 0 failures / 0 loss in every scenario for both stores.

## Results — one representative A/B run

| scenario | regime | file+vthread | bdb+loop |
|---|---|--:|--:|
| RPC 1 → 50 | normal | 18.5k ops/s · 0.054 ms | **30.4k ops/s · 0.033 ms** |
| RPC 50 → 50 | normal | **179k ops/s** · 0.278 ms | 174k ops/s · 0.286 ms |
| Callback 50 → 50 (paced) | normal | 39.7k ops/s · 0.382 ms | 40.0k ops/s · **0.279 ms** |
| RPC 100 → 50 | back-pressure | 69.6k ops/s · 1.44 ms | **187k ops/s · 0.53 ms** |
| Callback flood → 50 | back-pressure | 73.8k ops/s · 26.8 ms | **91.9k ops/s · 21.6 ms** |
| **Latency probe under flood** | **mixed** | **0.157 ms mean · 1.6 ms max** | 1.32 ms mean · 24.6 ms max |

(latency = mean unless noted; **bold** = faster/better on that metric)

## Reading the results

### 1. In isolation, BDB is a genuinely fast KV store
On a single route, BDB matches or beats the file store on throughput and median latency — decisively so under
spill (RPC 100→50: 187k vs 70k ops/s), because inline dispatch skips the virtual-thread mailbox hand-off and
BDB's log-structured spill batches writes, whereas the file store does a per-event segment write. **Raw
throughput is not the reason to migrate**; BDB has served production well for years.

### 2. Single-route *tail* latency is noisy — don't rely on it
Run-to-run, the single-route tail (p99.9/max) flips between the two stores. It is **not** a reliable
differentiator on an otherwise-idle single workload, so we don't base any claim on it.

### 3. The decider: latency isolation under concurrent load (mixed workload)
This scenario runs a latency-sensitive **probe** (a paced RPC on a *separate* route) *while* a background
flood hammers the main route's ElasticQueue — the shape of real production, where many routes share the
event loop. The probe result is **large and reproducible** (confirmed across 3 runs):

| probe metric | file+vthread | bdb+loop | BDB penalty |
|---|--:|--:|--:|
| mean | ~0.15 ms | ~1.2 ms | **~8×** |
| p99 | ~0.9 ms | ~5.1 ms | **~5×** |
| p99.9 | ~2 ms | ~10–12 ms | ~5× |
| max | ~1.6–3 ms | ~11–25 ms | **5–15×** |

With `bdb`, the background spill runs **inline on the shared event loop**, so an *unrelated* latency-sensitive
route inherits BDB's spill stalls. With `file`+vthread, the spill runs on a per-route **virtual thread off the
loop**, so the probe stays fast under the identical flood.

## Why this is the production-relevant result

Production Mercury services are **multi-route**, and every route's event routing shares one bounded Vert.x
event-loop pool. BDB's back-pressure spill executes on that shared loop; under load its housekeeping stalls
ripple into every other route, HTTP handler, and timer. The file store removes back-pressure from the loop
entirely. A single-route micro-benchmark can't see this (there's no "other work" to be starved) — which is
exactly why the mixed-workload probe was added, and why it, not raw throughput, drives the recommendation.

Two further architectural properties reinforce the choice, independent of these numbers:
- **VT-safety** — BDB's internal `synchronized`/latches pin virtual-thread carriers, so it *cannot* run
  off-loop safely (validated separately: vthread+bdb starved the reactor's minigraph suite). `file` is the
  only store that supports off-loop dispatch at all.
- **Portability / zero dependency** — a plain `[len][payload]` segment format (portable to a future Rust
  port) with no embedded-DB dependency.

## Honest caveats

- BDB wins raw single-route throughput, especially spill throughput (~2.7× on RPC 100→50 here). The file
  store's per-event writes could later be batched to narrow that gap; throughput was never the driver.
- Laptop figures; re-run per environment. Single-route tail is noisy (see above).
- Both stores were loss-free and degraded predictably (Little's law) under overload — the framework itself is
  robust either way; this is about *where* the spill work runs.

## Reproduce

```bash
mvn -pl benchmark/benchmark-reporter -am package -DskipTests
# file (default):
java -Dbench.report=/tmp/file-vthread.html -jar benchmark/benchmark-reporter/target/benchmark-reporter.jar
# bdb (fallback):
java -Delastic.queue.store=bdb -Dbench.report=/tmp/bdb-loop.html -jar benchmark/benchmark-reporter/target/benchmark-reporter.jar
```
Open the two HTML files side by side; the **Mixed workload** section's probe histogram tells the story at a
glance. To refresh the committed reference snapshots, copy `/tmp/file-vthread.html` and `/tmp/bdb-loop.html`
into this `analysis/` folder.
