---
title: Test Report — Dynatrace OneAgent integration
summary: Permanent record of the OneAgent code-level tracing validation - live invocation observers bracketing every
  composable-function hop with the OneAgent SDK, run against a Dynatrace sandbox environment from a dev machine, with
  REST and Kafka entry points, negative controls, and a span-by-span check of Mercury's own telemetry.
layer: reference
audience: [developer, architect, devops]
keywords: [dynatrace, oneagent, purepath, tracing, invocation observer, kafka, w3c, traceparent, test report]
---

# Test Report — Dynatrace OneAgent integration

*Live validation of the [live invocation observers](../guides/observability.md#live-tracing) and the
`dynatrace-oneagent` extension against a **Dynatrace sandbox environment**, conducted 2026-10-22 and 2026-10-23 from a
single developer machine, with the support of Dynatrace's engineering team. The subject is the OneAgent lab
(`lab/dynatrace-oneagent`): two Mercury applications and a Kafka broker in containers, with OneAgent attached. This
report records what was run, the evidence, the findings each iteration produced, and what remains.*

*No environment address, token or tenant identifier appears in this report or in any committed file. The OneAgent
installer, which is commercially licensed, is ignored by git and never committed.*

## Summary

**One Mercury request is one connected Dynatrace trace**, from the REST entry through every composable function, across
Kafka into a second application, and back to the HTTP response. The Dynatrace trace id equals Mercury's trace id,
Mercury's first span is a child of OneAgent's HTTP node, and Mercury's own span lineage is unbroken in both
applications. The maintainer judged the implementation ready to deploy to the field.

| Claim | Evidence | Result |
|---|---|---|
| Every hop of a request is in one PurePath, the RPC and the `Mono` included | Dynatrace waterfalls | PASS |
| The trace crosses Kafka into the consuming application | Dynatrace waterfalls; A-B-A on `dt.trace_id` in the logs | PASS |
| The Dynatrace trace id is Mercury's trace id, with or without a client `traceparent` | Log lines (`trace_id` = `dt.trace_id`); Dynatrace | PASS |
| Mercury's edge span is a child of OneAgent's server node | Dynatrace span id against Mercury's `parent_span_id` | PASS |
| Mercury's own telemetry has no broken spans | Span-by-span check of the Telemetry log, three orders | PASS |
| The negative control breaks one link and only one | Dynatrace; log lines | PASS |
| No OneAgent SDK warnings | Application logs | PASS |
| Each hop is named after its application and route, in one service | Dynatrace request list | PASS |
| `/info` reports the application's own version under OneAgent | `/info` | PASS |

## The system under test

| Element | Value |
|---|---|
| Machine | macOS developer machine, Apple silicon |
| Container runtime | Colima, an `aarch64` Ubuntu 24.04.5 LTS VM with the Docker runtime |
| Agent | Dynatrace OneAgent 1.347.64, installed full stack in Colima's VM (signature verified), injected into every JVM a container starts |
| Images | Ubuntu 26.04.1 LTS with OpenJDK 25.0.4.1 (`lab/dynatrace-oneagent/base`) |
| Mercury | branch `feature/dynatrace-oneagent-compatibility` (PR #583), version 4.12.25, OneAgent SDK 1.9.0 |
| Broker | `helpers/kafka-standalone` |

Three containers share one network namespace, because the broker advertises `127.0.0.1:9092`:

- **`lab-edge`, the REST entry point.** `POST /api/lab/orders` runs the `lab-order` flow: `lab.validate`, which makes
  an RPC to `lab.lookup` from inside the function; `lab.price`, which returns a `Mono` that completes 200 ms later on a
  Reactor thread; `lab.audit`, the negative control (`invocation.observer.unlinked`); `simple.kafka.notification` to
  the topic `lab.orders`; and `lab.receipt`.
- **`lab-worker`, the Kafka entry point.** The `minimalist-kafka` flow adapter consumes `lab.orders` and runs
  `lab-fulfil`: `lab.reserve`, then `lab.ship`.
- **`lab-broker`.**

The settings under test, in both applications:

```properties
dynatrace.oneagent.enabled=true
dynatrace.oneagent.destination=application-route
dynatrace.oneagent.log.context=true
log.format=compact
invocation.observer.unlinked=lab.audit
```

```bash
cd lab/dynatrace-oneagent
./lab.sh build && ./lab.sh up oneagent   # ./lab.sh up none for the baseline
./lab.sh order                           # one order; repeat
./lab.sh lineage                         # the span check of Mercury's own telemetry (Scenario 9)
```

## Scenario 1 — the baseline: OneAgent without the adapter

With OneAgent attached and the adapter off, each request was **one span** (`/api/lab/orders`, about 230 ms) under a
generic Netty service, with no child spans: the hops between composable functions were invisible, because Mercury hands
an event through its event bus and a per-route queue, where an agent's thread-bound context is lost. Mercury's own
trace id still crossed Kafka.

## Scenario 2 — every hop in one PurePath

With the adapter on, each request became **one connected, nested PurePath** from `server /api/lab/orders`: a producer
and consumer pair per hop, the RPC from inside `lab.validate`, and the `Mono` hop continued by OneAgent's own Reactor
support (`reactor.core.scheduler.WorkerTask`). Every function's log line carries `dt.trace_id` and `dt.span_id`, and
every node carries the captured attributes `mercury.route` and `mercury.trace_id`, so a Mercury trace id is searchable
in Dynatrace with no environment setup.

## Scenario 3 — across Kafka, proven A-B-A

`simple.kafka.notification` carries the observers' tags as `x-mercury-tag-<tag>` headers, and the flow adapter opens
each message as one incoming hop on `kafka.flow.adapter`. The check reads the log lines: the worker's functions are
linked when their `dt.trace_id` is the edge's.

| Leg | Edge setting | Worker's Dynatrace trace |
|---|---|---|
| A | normal | the edge's, 3 of 3 orders |
| B | `lab.orders` excluded from observation, so no tag is exported | its own, 3 of 3 |
| A | normal again | the edge's, 3 of 3 |

Leg B is the experiment that makes the other two mean something: a link that cannot be shown to break proves nothing.

## Scenario 4 — the negative control isolates its own route

`invocation.observer.unlinked` makes a route start a new trace, so a run can show that a missing link is visible. In
the first run it moved the rest of the flow into its fragment, because the route's reply carried a tag of its new
trace. The route's reply now keeps the caller's tag, and the route's scope closes before the reply leaves. For 3 of 3
orders, the request is one trace and `lab.audit` alone the other.

Withholding the tag alone changed nothing. `task.executor` runs on an executor task submitted from the replying thread,
and OneAgent carries that thread's active PurePath into the task; an SDK tracer started there ignores its tag. Ending
the route's path before the handoff is what breaks the link. With no unlinked route, each Mercury trace is exactly one
Dynatrace trace across both applications (3 of 3).

## Scenario 5 — one trace id, one lineage

A request that carries a W3C `traceparent` keeps it, and OneAgent continues the same one: the ids agreed end to end
(`4bf92f35…`, `0af76519…`). A request without one now continues the agent's trace context (`InvocationObserver.traceparent`)
at the REST edge, and a Kafka message without one at the flow adapter, as if the caller had sent it.

- With no client `traceparent`, Mercury's trace id equals the Dynatrace trace id in both applications, 3 of 3 orders.
- **Confirmed in the Dynatrace UI:** in trace `6f9157b5c7a6e46ba1f43a79ef51ccd5`, the server span of `/api/lab/orders`
  is `a8b280c1ddc4af29`, the parent that Mercury recorded for its edge span. The maintainer's reading of that trace:
  `http.flow.adapter`, the edge's hops and `simple.kafka.notification`, then `kafka.flow.adapter`,
  `event.script.manager` and the worker's hops, then back to `async.http.response`, in one tree.

## Scenario 6 — no SDK warnings on a deferred result

The first runs logged "incoming inProcessLink received on already started path ... is being lost" twice per order: the
adapter linked a `Mono` completion to its hop, but OneAgent's Reactor support had already continued the path onto the
completion thread. The link is now started only when the completion thread is not already in the hop's trace. Result:
**0 SDK warnings** in either application across every later run, with the `Mono` hop still in the trace.

## Scenario 7 — each hop named after its application and route

The endpoint of an SDK message tracer is `<destination type>: <destination name>`, and the type is a queue or a topic,
so the prefix cannot be dropped from the one tracer kind that carries the linkage tag. Every hop's destination is now a
topic, the Kafka term, named `<application> - <route>` by default. The request list in Dynatrace shows
`Topic: lab-edge - lab.validate`, `Topic: lab-worker - lab.reserve` and so on, all under **one** service, "Mercury
Messaging", so per-route names add no service (orders `5b95a65b…`, `bb3d424c…`, `985653c8…`).

## Scenario 8 — the application's own version under OneAgent

Under full-stack OneAgent, the startup log said "Application name: oneagentjava.17" and `/info` reported version
`1.347.64.20261021-162634`: the version came from the class loader's first `META-INF/MANIFEST.MF`, and OneAgent's jar
on the boot class path is searched first. Fixed on main by #585. The lab, rebuilt with it, reports `lab-edge` `4.12.25`.

## Scenario 9 — Mercury's own telemetry has no broken spans

The Dynatrace tree includes OneAgent's plumbing nodes, such as the `EventEmitter$$Lambda` executor tasks. Mercury's
Telemetry log records only its own spans: the REST edge, the composable functions, `task.executor` and
`async.http.response`. To check that its lineage is whole, both applications' logs were saved and every Telemetry
record of the three most recent orders was linked by `span_id` and `parent_span_id` (`./lab.sh lineage`).

| Order (Mercury trace id) | Span records | Roots | Unreachable | Links across Kafka | Result |
|---|---|---|---|---|---|
| `d0c236ae8fa7e618cb5f3a57ab7bd22c` | 14 (10 edge, 4 worker) | 1 | 0 | 2 | CONNECTED |
| `589cea5ad70b1930ccb25f05aa3016e3` | 14 (10 edge, 4 worker) | 1 | 0 | 2 | CONNECTED |
| `e7e917c6e165f9fb3163c9f8e665e269` | 14 (10 edge, 4 worker) | 1 | 0 | 2 | CONNECTED |

The check was first shown to fail: with one `lab.price` record removed from a copy of the log, it found two roots and
reported the order BROKEN (exit code 1). The one root is the REST edge, `http.request`, whose parent is OneAgent's
server node and no Mercury span. No span id is
repeated. Each order has one record without a span id: the flow adapter's RPC round-trip record for `event.script.manager`,
attached to a valid parent. It has no span of its own by design (#215): the RPC inbox records the responder's span id
only when the route it called also replied, and a flow replies from its last task. On the log lines, each order is the
same two Dynatrace traces: the request, with Mercury's trace id, and `lab.audit` alone. One of the three trees:

```text
http.request               [lab-edge]   +  0.0 ms  243.46 ms   root; parent 4448bdda80e074a2 is OneAgent's server node
   http.flow.adapter          [lab-edge]   +  3.0 ms
      task.executor              [lab-edge]   +  5.0 ms
      lab.validate               [lab-edge]   +  7.0 ms
         lab.lookup                 [lab-edge]   +  8.0 ms   (the RPC from inside lab.validate)
         lab.price                  [lab-edge]   + 13.0 ms   (the Mono; its successor starts about 200 ms later)
            lab.audit                  [lab-edge]   +219.0 ms
               simple.kafka.notification  [lab-edge]   +222.0 ms
                  lab.receipt                [lab-edge]   +241.0 ms
                     async.http.response        [lab-edge]   +242.0 ms
                  task.executor              [lab-worker] +244.0 ms   crosses Kafka
                  lab.reserve                [lab-worker] +245.0 ms   crosses Kafka
                     lab.ship                   [lab-worker] +248.0 ms
                  event.script.manager       [lab-worker]  RPC round-trip record, no span of its own
```

`lab.audit` is a normal span in Mercury's own tree: the negative control acts only on the observers' tags.

## Scenario 10 — OneAgent off: the original design

With the switch off, or with no agent attached (the SDK reports itself inactive and the observer is not registered),
the observer list is empty: every hook returns after one emptiness check, the trace id is chosen as before (a
`traceparent`, then the trace-id header, then a fresh id), no `x-mercury-tag-*` header is written, and replies go out
unchanged. Mercury's Telemetry log is identical to before. The whole reactor, whose telemetry and W3C tests run with no
observer registered, passed after the merge of main: BUILD SUCCESS, 39 of 39 modules, 2,772 tests, 0 failures, 3
skipped, all coverage gates met.

## Findings and their fixes

| Finding | Resolution |
|---|---|
| `dynatrace.oneagent=true` was lost beside `dynatrace.oneagent.*`: a configuration key holds a value or child keys, never both | The switch is `dynatrace.oneagent.enabled`. An audit of every key found the same collision in the mini-scheduler, fixed by #584 |
| `/info` reported OneAgent's version | `ManifestReader` reads the application's own jar (#585) |
| The negative control moved the rest of the flow into its fragment | The route's reply keeps the caller's tag, and its scope closes before the reply leaves (Scenario 4) |
| No link across Kafka | The tags travel as `x-mercury-tag-*` headers; one hop per message (Scenario 3) |
| Two SDK warnings per order on the `Mono` hop | The link is skipped where OneAgent already continued the path (Scenario 6) |
| Two trace ids per request without a client `traceparent` | The agent's `traceparent` is continued at the REST edge and the Kafka consumer (Scenario 5) |
| Every hop was `Queue: <application>` | `Topic: <application> - <route>` by default, one service (Scenario 7) |
| A lab left running answered the reactor tests' requests on port 8100 | The lab publishes host port 18100 |
| The lab in the root reactor would be registered by the field's static code analysis | The lab has its own aggregator, `lab/dynatrace-oneagent/pom.xml`, and the root pom does not list it |

**The OpenTelemetry API adapter was built and removed.** Under OneAgent its spans were recorded, named after their
routes, but every hop through the event bus became a separate request. A diagnostic build showed why: OneAgent links an
API span only through its own context on the thread and ignores a W3C parent set by the caller (`SERVER` spans behave
the same; `INTERNAL` spans are not created at all), and its PurePath is not OpenTelemetry's current context at the REST
entry. The observer seam stays vendor-neutral, so an adapter for an agent that honours W3C parents can come later.

## What remains

- **Overhead was not measured.** The lab checks correctness, not cost; a load comparison with the adapter on and off is
  still to do.
- **A Kafka message without any trace** continuing the agent's context is covered by unit tests only: a Mercury
  publisher always sends a `traceparent`, so the lab does not produce one.
- **Not exercised:**
  - Event-over-HTTP between applications. The tag is an envelope tag, which Event-over-HTTP serializes with the
    envelope, but no test or lab run has driven the linkage across it.
  - `Flux` streams, which the adapter does not cover.
  - OneAgent's own log enrichment combined with `dynatrace.oneagent.log.context`, which the guide advises against.
- **The field.** The maintainer judged the implementation ready to deploy; a field installation's own run is the next
  evidence.
