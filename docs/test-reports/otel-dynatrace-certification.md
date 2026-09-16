---
title: Test Report — OpenTelemetry forwarder against Dynatrace
summary: Permanent record of the live certification run - the opentelemetry-forwarder exporting
  real Mercury spans to a Dynatrace SaaS OTLP endpoint, confirmed queryable in the Dynatrace UI,
  with an A-B-A credential experiment that establishes what a clean run actually proves.
layer: reference
audience: [developer, architect, devops]
keywords: [opentelemetry, otlp, dynatrace, splunk, tracing, forwarder, certification, test report]
---

# Test Report — OpenTelemetry forwarder against Dynatrace

*Live validation of the [OpenTelemetry forwarder](../guides/observability.md#otel-forwarder)
against a **Dynatrace SaaS OTLP endpoint**, conducted 2026-09-16 on a single developer machine
running `composable-example` as the subject application. This report is a permanent record in the
tradition of the [streaming return route report](streaming-return-route-cross-pod.md): what was
run, the evidence, and the observations the round produced.*

*No endpoint URL, token, or tenant identifier appears in this report or in any committed file. All
three come from the environment at launch; the committed configuration keeps the feature off.*

## The scenario under test

Mercury emits its own distributed trace for every transaction. The forwarder extension turns that
into OTLP and ships it to a vendor backend, so a Mercury application appears in the same tracing
tool as everything else the enterprise runs. Two things needed proving on real infrastructure
rather than against a mock:

1. **The opt-in switch does what it claims.** Carrying the dependency must register nothing;
   `otel.forwarding=true` must be the only thing that turns the route on.
2. **A real vendor backend accepts the spans** — the wire format, the endpoint path, the
   vendor-specific credential header, and gzip compression, all together.

| Element | Value |
|---------|-------|
| Subject application | `examples/composable-example`, the `create-profile` flow |
| Backend | Dynatrace SaaS OTLP HTTP endpoint (non-prod tenant) |
| Service name | `mercury-otel-cert` (via `OTLP_SERVICE_NAME`) |
| Credential | `Authorization: Api-Token` + token, both from the environment |
| Compression | gzip |
| Runtime | Java 21.0.12.1, Maven 3.9.16, macOS |
| Build under test | branch `feature/otel-certification`, forwarder commit `2036b5b4` |

The transaction is one `POST /api/profile`. Its `id` must be **numeric** — `Profile.id` is an
`Integer`, and a string id fails the transaction before any span is emitted.

```bash
curl -X POST http://127.0.0.1:8100/api/profile -H 'content-type: application/json' \
  -d '{"id":20260916,"name":"OTel Certification","address":"1 Trace Street","telephone":"123-456-7890"}'
```

## Scenario 1 — the switch is the only thing that turns it on

`composable-example` declares the forwarder as an ordinary dependency and ships
`otel.forwarding=false`. The forwarder class is annotated `@OptionalService("otel.forwarding")`, so
with the switch off the class is never registered and the route does not exist — the application
behaves exactly as it did before the dependency was added. This is pinned by a test in the example
app itself (`OtelForwardingSwitchTest`), so the "dependency present, feature off" property cannot
regress silently.

Launched with the switch on, the route appears and the forwarder announces its configuration:

```text
OpenTelemetry trace forwarder ready - service=mercury-otel-cert,
OTLP endpoint=<redacted>, compression=gzip, credential headers=[Authorization]
PRIVATE distributed.trace.forwarder started as virtual thread
```

Note what that line does **not** contain. It names the credential header but never its value, so
the startup log of a production pod cannot leak the token.

## Scenario 2 — six spans, one trace, zero export failures

One transaction, HTTP 201 in 20 ms. Mercury's own trace contains six spans, and the forwarder
exports each one individually as it arrives:

```text
http.flow.adapter          8d63c9fd65984a73   server
├─ task.executor           81e9b286325117c7   internal
└─ v1.create.profile       95e4510fe14d6419   internal
   ├─ async.http.response  8177b74c744778e3   internal
   └─ v1.encrypt.fields    8dbf4257826c107f   internal
      └─ v1.save.profile   a1df2ce1039869c9   internal

trace 4815a9a3f2cb4768b4b479a90687ee5a — 2026-09-16T19:22:37.656Z
```

The nesting above is **as Dynatrace reconstructed it** from the `parent_span_id` values Mercury
propagated, not as inferred locally — the backend is the authority on parentage, and it is what
proves the W3C context survived the wire.

Zero `OTLP export failed` lines. Because the forwarder exports one span per call and attaches a
completion callback (`exporter.export(singletonList(span))` + `whenComplete`), there is no batch
window in which a failure could still be pending — every span's outcome is known by the time the
transaction finishes.

## Scenario 3 — the A-B-A credential experiment

**Zero failures is only evidence if a failure was possible.** A forwarder that silently skipped
export, or never attached the credential, would also produce zero failures. So the same run was
repeated with a deliberately invalid token, then restored:

| Leg | Credential | Export failures | App response | Trace |
|-----|-----------|-----------------|--------------|-------|
| **A** — clean | real token | **0** of 6 | HTTP 201 | `4815a9a3…` at 19:22:37Z |
| **B** — negative control | bogus token | **6** of 6 | HTTP 201 | `2bbae37f…` at 19:28:50Z |
| **A′** — restored | real token | **0** of 6 | HTTP 201 | `e761110f…` at 19:29:41Z |

Leg B's diagnostic, one per span:

```text
OTLP export failed for span 9ff320a0b0187a10 of trace 2bbae37f1f3446f28cb0984e4032b5f0
- HTTP 401 Unauthorized - Token Authentication failed | the backend rejected the credential
  itself - check otel.exporter.otlp.headers (the header name and any auth scheme must match
  what the backend expects)
```

Three conclusions follow, none of which the clean run alone could support:

- The forwarder **really exports** — six attempts, one per span, not a silent no-op.
- The credential **really is sent and validated by Dynatrace**, which means leg A's six clean
  exports are six 2xx responses from the backend, not six skipped calls.
- The failure path **is self-diagnosing** — the message names the span, the trace, the HTTP
  status, the backend's own words, and the specific configuration key to look at.

A fourth observation matters operationally: **leg B still returned HTTP 201.** A telemetry backend
that is down, or a credential that has expired, degrades observability and nothing else. The
forwarder is a subscriber to the trace stream, not a participant in the transaction.

## Scenario 4 — failures that name their own cause

Two failure modes were diagnosed during this round and are recorded because both are easy to spend
an afternoon on:

| Symptom | Cause | What the forwarder now says |
|---------|-------|-----------------------------|
| `HTTP 404` on every export | The endpoint was the vendor's OTLP **base** URL. `OtlpHttpSpanExporter.setEndpoint()` wants the full **signal** URL. | *check `otel.exporter.otlp.endpoint` includes the signal path (e.g. `.../v1/traces`), not just the vendor base URL* |
| `HTTP 403` on every export | The API token authenticated but lacked the trace-ingest scope. | *the credential was accepted but lacks permission - grant the trace-ingest scope on the token (the response body above names it)* |

The 403 was the substantive blocker for this round: a Dynatrace API token needs
`openpipeline:traces:ingest`, and the backend names the missing scope in its response body, which
the diagnostic prints. Both messages exist because the raw SDK failure — a wrapped internal
exception with a status code buried in it — told the reader nothing actionable.

## Observations and round notes

- **The vendor-specific part is one variable.** `OTLP_AUTH_HEADER` carries the header name *and*
  any auth scheme in literal HTTP syntax (`Authorization: Api-Token` for Dynatrace,
  `X-SF-Token:` for Splunk), which keeps `OTLP_TOKEN` the bare secret and rotation to a single
  variable. Switching backends is an environment change, not a rebuild.
- **The credential is resolved per export, not once at construction.** `@PreLoad` classes are
  constructed before `@MainApplication` runs, so a forwarder that froze its headers in its
  constructor would freeze them as missing in any deployment that fetches secrets from a vault at
  start-up. The headers are supplied as a `Supplier`, re-read on every export.
- **`OTLP_SERVICE_NAME`, deliberately not `OTEL_SERVICE_NAME`.** `OTEL_*` variables are often
  exported machine-wide on instrumented hosts and CI agents; inheriting one by accident would
  silently rename this application's traces.
- **Verify the fat jar, not the build command.** Two dead ends in this round came from running a
  stale `composable-example` jar whose nested forwarder predated the change. The nested jar was
  thereafter checked directly (`unzip -p …/BOOT-INF/lib/opentelemetry-forwarder-*.jar … | strings`)
  before every run.

## Scenario 5 — confirmed queryable in Dynatrace

The exporting side can only prove that Dynatrace *accepted* the spans. Ingest and visibility are
different claims — OpenPipeline processing, sampling and retention all sit between them — so the
round was held open until the traces could be queried in the Dynatrace UI. They can.

Filtering Distributed Tracing on `"Trace id" = 4815a9a3f2cb4768b4b479a90687ee5a` — leg A's trace —
returns **6 spans**, all under service `mercury-otel-cert`, all with span status **Ok**:

| Span | Duration | Span kind |
|------|----------|-----------|
| `http.flow.adapter` | 456 µs | **server** |
| `task.executor` | 28 ms | internal |
| `v1.create.profile` | 1.81 ms | internal |
| `async.http.response` | 509 µs | internal |
| `v1.encrypt.fields` | 1.34 ms | internal |
| `v1.save.profile` | 15.58 ms | internal |

Trace duration 29 ms. Four things in that view are worth naming, because each is a separate part of
the contract holding:

- **The span tree reconstructed correctly.** Dynatrace renders the waterfall from `parent_span_id`,
  and the shape it drew is the flow's actual shape (above). The W3C context Mercury propagates
  survived the OTLP mapping.
- **Span kinds mapped, not defaulted.** The HTTP entry point is `server` and every downstream
  function is `internal` — so a Mercury trace arrives as a *structured* trace, not a flat list of
  identical spans.
- **The instrumentation scope is right, and self-versioning.** `OTel scope name`
  `org.platformlambda.opentelemetry-forwarder`, `OTel scope version` **4.12.10** — resolved at
  runtime from the running application rather than a hard-coded constant, which is what keeps it
  from going stale across releases. Confirmed here against a real backend for the first time.
- **Dynatrace treats it as a first-class service entity** (Smartscape), not an unattributed span
  source, so the application appears in service-level views alongside natively instrumented ones.

The per-span millisecond offsets in the UI match the exporting side's log exactly (`.656`, `.657`,
`.658`, `.664`, `.666`, `.669`); the wall-clock hour differs only because the tenant renders in its
own timezone rather than UTC.

## Scenario 6 — field acceptance on the release build

Scenarios 1–5 ran against the feature branch. Field acceptance re-runs the round against the
**artifacts that actually ship** — the v4.12.11 reactor output — because the thing certified and the
thing released are only the same if you check.

The discriminator is free and backend-visible: the forwarder resolves its instrumentation scope
version at runtime from the running application, so a trace submitted by the release build reports
**`OTel scope version 4.12.11`** in Dynatrace. Given that a stale fat jar cost two dead ends earlier
in this work, a version stamp the backend can show is better evidence than any local check.

| Check | Result |
|-------|--------|
| Nested jar in the shipped fat jar | `opentelemetry-forwarder-4.12.11.jar`, carrying `@OptionalService` + `otel.forwarding` |
| Retired key absent from the shipped class | 0 occurrences of `trace.forwarder.enabled` |
| **Default off** — launched with no `-D` flag | route **not** registered; the gate logs its decision: `Skip optional class …OpenTelemetryForwarder during PreLoad phase` |
| Switch on — `-Dotel.forwarding=true` | `distributed.trace.forwarder started as virtual thread` |
| Write transaction | `POST /api/profile` → **HTTP 201** in 28 ms |
| Read transaction | `GET /api/profile/{id}` → **HTTP 200** in 9 ms |
| `OTLP export failed` | **0**, across both |
| Dynatrace UI | *pending* — submitted for confirmation (see below) |

Two transactions rather than two of the same, because the **shapes differ** and that is the part a
repeat could not show:

```text
trace 45a6e43c67ef4087b9351956f50130d7 — create-profile, 6 spans, 20:42:35.685Z
   http.flow.adapter · task.executor · v1.create.profile · async.http.response
   · v1.encrypt.fields · v1.save.profile

trace 1c32bbe5e6b44f12aec201ce78faca3e — get-profile, 5 spans, 20:45:07.727Z
   http.flow.adapter · v1.get.profile · task.executor · v1.decrypt.fields
   · async.http.response
```

The forwarder maps what each flow actually did — a different function set and a different span count
per flow — rather than emitting a fixed structure. The pair also exercises the data path end to end:
the POST response masked `address` and `telephone` as `***` while the GET returned them in clear, so
encrypt-on-save and decrypt-on-read each ran and each appears as its own span.

The default-off leg is worth calling out on its own. It is the property an application team relies
on when they add the dependency ahead of a decision, and it is now confirmed three ways: a unit test
in the example app, the absence of the route at runtime, and an explicit log line stating that the
optional class was skipped. DevOps reading a startup log can see the feature was considered and
declined, rather than inferring it from silence.

## What remains

**One open item: backend confirmation of the two acceptance traces.** Both were submitted with zero
export failures, so Dynatrace returned 2xx for all eleven spans — but the UI check is with Dynatrace
support, who are locating `45a6e43c67ef4087b9351956f50130d7` and `1c32bbe5e6b44f12aec201ce78faca3e`.
The field to read is **`OTel scope version`**: it should be **4.12.11**, against 4.12.10 for the
Scenario 1–5 traces. That value is resolved at runtime from the running application, which makes it
proof that the *released* artifacts submitted these traces rather than a leftover build — the failure
that cost two dead ends earlier in this work.

Nothing else. The forwarder's loop is closed on the exporting side, on the backend side
(Scenario 5), and on the released artifacts locally (Scenario 6).

Two unrelated follow-ups were noted during the round and are tracked elsewhere: the guides still
show `OTEL_EXPORTER_OTLP_HEADERS` and `OTEL_SERVICE_NAME` in generic examples while this reference
app uses the `OTLP_`-prefixed split (both work; the split keeps the token a bare secret and avoids
inheriting a machine-wide `OTEL_*` value), and Splunk Observability Cloud has not been exercised
live — its header form is documented and parsed, but only Dynatrace has been run end to end.
