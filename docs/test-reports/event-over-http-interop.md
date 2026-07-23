---
title: Interop Test Report — Event over HTTP, Java ⇄ Rust
summary: Permanent record of the live bidirectional interoperability validation between the
  Java and Rust implementations, covering the programmatic and declarative patterns.
layer: reference
audience: [developer, architect]
keywords: [interop, event over http, rust, wire format, test report, tracing]
---

# Interop Test Report — Event over HTTP, Java ⇄ Rust

*Live bidirectional interoperability validation between the Java engine
([mercury-composable](https://github.com/Accenture/mercury-composable)) and the official
Rust implementation ([mercury](https://github.com/Accenture/mercury)), conducted
2026-07-22 as the release gate for the v4.10 line.*

This report is a permanent record. It documents what was tested, the evidence collected,
and — in the interest of honest engineering — the defects the drives surfaced and how each
was fixed and re-verified. Everything here is reproducible from the shipped examples by
following the [Event over HTTP](../guides/event-over-http.md#zero-code-demo) walk-through.

## Background: the wire-format validation drive

The first interop drive (2026-07-22, morning) validated the language-neutral
[standard event envelope wire format](../guides/event-envelope-wire-format.md) end-to-end:
Java→Rust 7/7 and Rust→Java 7/7 test cases, including binary payloads, in-band error
semantics (403/404/408 as envelope status), async acknowledgement, and W3C trace
continuity in both directions. That drive found and fixed three defects — a Java HTTP
client read-timeout truncation
([PR #214](https://github.com/Accenture/mercury-composable/pull/214)) and its Rust mirror
plus an example echo binary drop
([mercury PR #166](https://github.com/Accenture/mercury/pull/166)). Golden conformance
vectors are shared verbatim between the two repositories.

## This drive: the two calling patterns, both directions

The second drive validated the two Event-over-HTTP usage patterns — **programmatic** (the
PostOffice request API with an explicit Event API endpoint URL) and **declarative** (a
foreign route resolved through `event-over-http.yaml`) — using the shipped example
applications as-is.

| Role | Drive A | Drive B |
|------|---------|---------|
| Caller (port 8100) | Java composable-example | Rust hello-flow |
| Callee (port 8085) | Rust hello-world | Java lambda-example |

Endpoints exercised on each caller: `POST /api/event/http/demo` (declarative),
`POST /api/event/http/programmatic`, and `GET /api/event/http/demo`. **Zero configuration
changes between drives** — the callees are drop-in counterparts of each other (same port,
same public routes `hello.world` / `hello.declarative`), which is the point of the demo.

**Versions under test:** Java at
[PR #215](https://github.com/Accenture/mercury-composable/pull/215) (pre-4.10, CI green);
Rust at [PR #167](https://github.com/Accenture/mercury/pull/167).

## Results — functionality: 6/6 pass

- Every request returned HTTP 200 with `Content-Type: application/json` and the correct
  echo: the request body round-tripped intact, and the `origin` field identified the
  application instance that actually executed the function — in the other language.
- Java callee: the echoed `my_route` header discriminates the pattern —
  `hello.declarative` (declarative) vs `hello.world` (programmatic) — demonstrating why
  the echo function registers two route names.

## Results — trace continuity

Distributed traces were inspected at the individual span level in both applications' logs.

**Java → Rust: a fully connected cross-language span tree in both patterns.** For the
declarative call (trace `a6b8fa67…`): Java `http.flow.adapter` (span `9fbd1bdc`) → Rust
`event.api.service` (span `a092275c`, parent `9fbd1bdc` — chained across the wire) → Rust
`hello.declarative` (span `8e505921`) — and back on the Java side, the response callback
span parents onto the **Rust** function's span, closing the loop. The programmatic call
chains the same way through the Java `v1.event.over.http.rpc` task span.

**Rust → Java:** the declarative call chained fully (Java `hello.declarative` parented
onto the Rust flow-adapter span, matching Java-caller behavior exactly; even the echo's
internal fire-and-forget `hello.pojo` call chained onto the echo's span). The programmatic
call initially joined by trace id only — see finding I3 below.

With the [default-on application log context](../guides/observability.md#log-context),
every structured log line on both sides carried the same trace id — logs and spans join up
across the language boundary with zero setup.

## Findings and resolution

Span-level analysis surfaced three telemetry-fidelity defects, all in the Rust port and
none affecting functionality. Recording them here is deliberate: the drives exist to find
exactly this class of issue.

- **I1 — duplicate span records.** The Rust callee emitted two records per RPC-served span
  (its worker record plus the caller-side round-trip record). The Java engine emits exactly
  one: the worker suppresses its own record when serving an RPC and the caller's inbox
  record — carrying `exec_time`, `round_trip`, and span lineage — is *the* record for that
  span. The Rust port now applies the same suppression, and the callee's annotations ride
  the reply envelope (a wire-compatible field that survives Event-over-HTTP hops in either
  direction).
- **I2 — foreign span adoption on relayed replies.** A round-trip record adopted the span
  id of whatever function produced the reply — for a flow, that is the flow's final task,
  not the requested route — misattributing and duplicating a span reported elsewhere. Both
  engines now adopt the reply's span id only when the reply comes from the requested route
  itself (Java: `InboxBase.spanIdFromResponder`, commit `140640d8`; Rust: commit
  `8328d720`). The same defect had been caught on the Java side by the event-script
  engine's span-uniqueness regression suite during this same release cycle.
- **I3 — missing caller span on the programmatic wire envelope.** The Rust programmatic
  client did not stamp the calling function's span id onto the outbound envelope, so the
  remote callee's record lost its `parent_span_id` (the declarative path was correct). Now
  fixed; both patterns parent identically.

**Re-drive after the fixes (both directions, both patterns):** exactly one record per span
(zero duplicates), no foreign span ids on round-trip records, and callee records parent
onto the caller's task span in both patterns — every parent id cross-checked as a real
span in the caller's log.

## Verdict

**Interop validation passed in full.** Both Event-over-HTTP patterns are functionally
proven Java ⇄ Rust with zero configuration changes, and distributed-trace telemetry is
span-accurate across the language boundary in both directions. The shipped examples are
drop-in cross-language counterparts, and the walk-through in the
[Event over HTTP guide](../guides/event-over-http.md#zero-code-demo) reproduces this
validation with a single `curl`.
