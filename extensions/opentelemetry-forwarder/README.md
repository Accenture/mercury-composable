# OpenTelemetry trace forwarder

A reusable `distributed.trace.forwarder` for Mercury Composable. It receives every completed trace's
performance metrics from the built-in `Telemetry` service and exports them to an OpenTelemetry
collector over **OTLP/HTTP**, preserving the W3C trace / span / parent-span IDs that Mercury already
propagates across the Event Script, Knowledge Graph and HTTP layers.

Because a forwarder reports spans that have *already happened*, this module builds OpenTelemetry
`SpanData` directly (with Mercury's exact IDs) and hands it to the exporter — the standard `Tracer`
API would mint new IDs and break the lineage.

## Use it

Add the dependency to your application:

```xml
<dependency>
    <groupId>org.platformlambda</groupId>
    <artifactId>opentelemetry-forwarder</artifactId>
    <version>4.4.11</version>
</dependency>
```

That is all the wiring required. The forwarder is annotated `@PreLoad(route="distributed.trace.forwarder")`
and lives under `org.platformlambda`, a package Mercury always scans, so it auto-registers on startup.
Enable tracing on the endpoints/flows you care about (`tracing=true` in `rest.yaml`) and the spans flow
to your collector.

## Configuration (`application.properties`)

| Key | Default | Description |
|-----|---------|-------------|
| `otel.trace.forwarder.enabled` | `true` | `false` makes the forwarder a no-op (jar present, no export). |
| `otel.exporter.otlp.endpoint` | `http://localhost:4318/v1/traces` | OTLP/HTTP traces endpoint of your collector. |
| `otel.exporter.otlp.timeout` | `10000` | Export timeout in milliseconds. |
| `otel.service.name` | `application.name`, else `mercury` | `service.name` resource attribute on every span. |
| `otel.exporter.otlp.headers` | — | Comma-separated `key=value` request headers — where backend credentials go. |

The forwarder reads these from `application.properties` at startup, and every value supports
`${ENV_VAR:default}` substitution — so you keep secrets and per-environment settings out of the file and
in the environment. Point `otel.exporter.otlp.endpoint` at an OpenTelemetry Collector, then route on to
Dynatrace, Splunk, Jaeger, Tempo, etc. from the collector.

### Credentials (uploading directly to Dynatrace / Splunk / etc.)

When you export straight to a SaaS backend instead of a local collector, it needs an API token, passed as
a request header via `otel.exporter.otlp.headers`. **Source it from an environment variable with _no
default_** so no secret is hard-coded (a `${VAR}` with no default that is unset resolves to `null`, which
parses to zero headers — static-analysis-safe):

```properties
# application.properties
otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
otel.service.name=${OTEL_SERVICE_NAME:my-app}
otel.exporter.otlp.headers=${OTEL_EXPORTER_OTLP_HEADERS}
```

```bash
# Dynatrace
export OTEL_EXPORTER_OTLP_ENDPOINT="https://{env-id}.live.dynatrace.com/api/v2/otlp/v1/traces"
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Api-Token dt0c01.XXXX"

# Splunk Observability Cloud
export OTEL_EXPORTER_OTLP_ENDPOINT="https://ingest.{realm}.signalfx.com/v2/trace/otlp"
export OTEL_EXPORTER_OTLP_HEADERS="X-SF-Token=YOUR_ACCESS_TOKEN"
```

`otel.exporter.otlp.headers` is a comma-separated list of `key=value` pairs (split on the first `=` only,
so token values may contain `=`); header values are never logged.

## What maps where

| Mercury trace metric | OpenTelemetry span |
|----------------------|--------------------|
| `id` (32-hex) | trace ID |
| `span_id` (16-hex) | span ID |
| `parent_span_id` (16-hex) | parent span ID (root span when absent) |
| `service` (route name) | span name |
| `start` + `exec_time` | start / end timestamps |
| `success` / `status` / `exception` | span status (OK / ERROR + description) |
| `from` = `http.request` | span kind `SERVER` (else `INTERNAL`) |
| `path`, `from`, `origin`, `status`, `exec_time_ms`, `round_trip_ms`, `exception` | span attributes (same names) |
| `service` (route) | `route` attribute |
| `annotations` entries | `annotation.<key>` attributes |

Traces whose ID is not W3C-valid (32-hex) are skipped rather than exported with forged IDs.

## Try it locally (mock collector)

The test sources include a **composable mock OTLP collector** so you can see the export path without a
real backend. It is a plain Mercury function (`MockOtlpCollector`, route `mock.otlp.collector`) exposed at
both backend ingest paths via `src/test/resources/rest.yaml` — no controller, just a route name. Boot
`MockOtlpAppMain` from your IDE (it calls `AutoStart.main`) and the REST server starts on port `8299`; then
point any OTLP/HTTP exporter — or `curl` — at either path:

- `http://127.0.0.1:8299/api/v2/otlp/v1/traces` — Dynatrace-style
- `http://127.0.0.1:8299/v2/trace/otlp` — Splunk Observability-style

`OtlpComposableExportTest` drives the real exporter against both and asserts the credential header arrives.
The mock also **decodes the OTLP protobuf** (`opentelemetry-proto`, test scope) and logs the span key-values
— trace/span/parent IDs, name, timing, status, attributes — so a reviewer can see the exact payload, e.g.:

```
Mock OTLP received POST /api/v2/otlp/v1/traces - 204 bytes, 1 ResourceSpans
  resource: service.name=mercury-otel-demo
  span: name=hello.world kind=SPAN_KIND_INTERNAL trace_id=4bf9…4736 span_id=00f0…02b7 parent_span_id=a3ce…4736 status=STATUS_CODE_OK
    attr route=hello.world
```

## Notes

- Each forwarded metric is exported individually; for very high trace volumes a batching layer in
  front of the exporter would reduce HTTP round-trips (future enhancement).
- The forwarder is `@ZeroTracing`, so it never traces itself.
- Validated end-to-end at **Level-1** (a PostOffice RPC chain) and **Level-2** (an Event Script flow,
  including the synthetic `task.executor` flow-summary span). Trace records without a `span_id` (the RPC
  caller's own round-trip measurement) are skipped rather than emitted as forged spans.
