# Benchmark log

A running record of `benchmark-reporter` runs across milestone releases, so performance is tracked
release-over-release rather than measured once and forgotten. Newest first.

Full HTML reports and per-run findings live in [`analysis/`](analysis/README.md); this file is the
index and the trend.

**Adding an entry.** Run the suite on the same machine and flags as the entry you intend to compare
against, save the report as `analysis/v<version>-<UTC timestamp>.html`, then add a row here with the
build, the headline number, and anything that moved. Entry dates are **local** (matching the
CHANGELOG convention); report filenames carry a UTC timestamp with a `Z` suffix, so an evening run
can appear as the next UTC day — e.g. the 9/15/2026 entry below is
`v4.12.10-20260916-025610Z.html`.

> **Dev-laptop figures — indicative, not SLAs.** Absolute numbers vary by hardware, OS, and spill
> location. Only compare runs from the same machine class with the same flags, and treat differences
> under ~5% as noise. Re-run in each target environment for figures that mean anything there.

---

## 9/15/2026 — v4.12.10 · first run after the Berkeley DB retirement

**Build:** `main` @ `9aa40089` (PR #399) · **Report:** [`v4.12.10-20260916-025610Z.html`](analysis/v4.12.10-20260916-025610Z.html)
· **Finding:** [`v4.12.10-20260916-025610Z.md`](analysis/v4.12.10-20260916-025610Z.md)
**Environment:** Apple Silicon 12-core, JDK 21.0.12.1, macOS 26.6.2, spill on `/tmp` (not tmpfs),
200k ops/scenario, 256-byte payload, 50 consumers.

**Purpose:** regression check on the riskiest part of the retirement — collapsing `ServiceQueue`'s
dual dispatch into one per-route virtual thread, the hot path for every route.

**Verdict: no regression.**

| Scenario | 7/2026 `file`+vthread | 9/15/2026 v4.12.10 | Δ |
|---|---:|---:|---|
| RPC 1 → 50 · throughput | 18,537 /s | **21,354 /s** | +15% |
| RPC 1 → 50 · p99.9 | 1.32 ms | **0.103 ms** | much better |
| RPC 50 → 50 · throughput | 179,005 /s | 177,063 /s | −1% (noise) |
| RPC 50 → 50 · p99.9 | 1.45 ms | **1.063 ms** | better |
| Callback 50 → 50 paced · throughput | 39,691 /s | 38,863 /s | −2% (noise) |
| Callback 50 → 50 paced · p99.9 | 3.18 ms | **1.982 ms** | better |
| RPC 100 → 50 overload · throughput | 69,557 /s | **72,826 /s** | +5% |
| RPC 100 → 50 overload · p99.9 | 7.01 ms | **5.081 ms** | better |
| Callback flood · throughput | 73,806 /s | 74,091 /s | +0.4% (noise) |
| Callback flood · p99.9 | 36.69 ms | 39.63 ms | ~8% worse — see below |
| **Probe under flood · p99** | 0.845 ms | **0.634 ms** | better |
| **Probe under flood · p99.9** | **1.54 ms** | **1.141 ms** | **better** |
| **Probe under flood · max** | 1.62 ms | **1.386 ms** | better |

Zero failures, zero loss, every scenario.

**The line that matters:** probe-under-flood p99.9 = **1.141 ms**. Spill is still landing off the
shared Vert.x event loop after the dispatch branch was removed — the property ADR-0024 commits to.
This is the standing regression signal: blocking work returning to the shared loop shows here first,
well before any throughput figure moves.

**The one number that got worse:** callback-flood p99.9, +8%. That scenario's latency is
queue-bounded by Little's law (2,000 in-flight against 50 consumers), and throughput was flat, so the
delta is queue-residency drift rather than dispatch cost. The scenario exists to prove stability and
zero loss under overload, which it did.

---

## 7/2026 — baseline study · `file`+vthread vs `bdb`+loop

**Reports:** [`file-vthread.html`](analysis/file-vthread.html) · [`bdb-loop.html`](analysis/bdb-loop.html)
· **Analysis:** [`analysis/README.md`](analysis/README.md)
**Environment:** Apple Silicon 12-core, JDK 21, spill on `/tmp/reactive`, 200k ops/scenario,
256-byte payload, 50 consumers.

The A/B that decided the elastic-queue store. **It is no longer reproducible** — Berkeley DB was
retired in v4.12.10 — and it is kept because the result is counter-intuitive enough to be worth
preserving:

| Scenario | `bdb`+loop | `file`+vthread | winner |
|---|---:|---:|---|
| RPC 1 → 50 · throughput | **30,438 /s** | 18,537 /s | bdb, by 64% |
| RPC 50 → 50 · throughput | 173,909 /s | **179,005 /s** | file, marginally |
| Callback 50 → 50 paced · throughput | **39,998 /s** | 39,691 /s | tie |
| RPC 100 → 50 overload · throughput | **187,057 /s** | 69,557 /s | bdb, by 2.7× |
| Callback flood · throughput | **91,872 /s** | 73,806 /s | bdb, by 25% |
| **Probe under flood · p99.9** | 12.12 ms | **1.54 ms** | **file, by 8×** |
| **Probe under flood · max** | 24.63 ms | **1.62 ms** | **file, by 15×** |
| **Probe under flood · throughput** | 263 /s | **388 /s** | file |

**Berkeley DB won most of the isolated benchmarks — several of them decisively — and the file store
was still the right choice.** On a single isolated route there is no event loop to protect, so a
faster store simply looks faster. In a real multi-route service sharing one Vert.x event loop, a
carrier-pinning store forces spill I/O to run *inline on that loop*, so a burst on one route inflates
the tail latency of unrelated latency-sensitive routes: an 8× worse p99.9 and a 15× worse max on a
route doing nothing but a paced RPC.

**Durable lesson: benchmark the interference, not just the component.** A single-workload benchmark
would have kept Berkeley DB. Recorded as ADR-0024 and in
`memory/continuity.md` → `elastic-queue-file-store`.
