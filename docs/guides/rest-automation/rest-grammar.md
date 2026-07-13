---
title: REST automation grammar
summary: The authoritative, rule-based reference for the rest.yaml DSL — how HTTP endpoints map to
  services or flows, plus the CORS and header-transform blocks. Designed so a human or an AI agent
  can author rest.yaml deterministically.
layer: platform-core
audience: [developer, architect, ai-agent, reference]
keywords: [rest automation, rest.yaml, http endpoint, cors, headers, flow adapter, deterministic]
related:
  - guides/rest-automation/index.md
  - guides/configuration-reference.md
  - guides/event-script/flow-grammar.md
---

# REST automation grammar

> **At a glance**
>
> - The **source of truth** for `rest.yaml` — declaratively maps HTTP endpoints to a composable
>   function (`service`) or a flow (`flow`), with no controller code.
> - Machine-readable form: [`rest-automation.json`](rest-automation.json). Authoring from an agent?
>   See the [AI agent guide](ai-agent-guide.md). Worked examples: [REST Automation](index.md).
> - The keys below mirror the parser (`RoutingEntry`); unrecognized keys are ignored, and invalid
>   values fail to load.

## Top-level structure {#structure}

`rest.yaml` has these top-level keys (all lists except `static-content`):

| Key | Meaning |
|---|---|
| `rest` | the list of HTTP endpoint entries |
| `cors` | reusable CORS configs (referenced by a `rest` entry's `cors:`) |
| `headers` | reusable header-transform configs (referenced by `headers:`) |
| `static-content` | optional static asset / filter config |

## A `rest:` endpoint entry {#entry}

| Field | Required | Meaning / allowed values |
|---|---|---|
| `service` | **yes** | a function route, **or** a list `[primary, secondary]` (secondary gets a copy, output ignored), **or** an `http(s)://…` URL (relay). Use `http.flow.adapter` when binding to a `flow`. |
| `methods` | **yes** | list from `GET PUT POST DELETE HEAD PATCH` (`OPTIONS` is added automatically) |
| `url` | **yes** | URI path; supports `{param}` path variables and a trailing `*` wildcard (case-insensitive) |
| `flow` | no | a flow id (used with `service: http.flow.adapter`) |
| `timeout` | no | duration like `30s` (default 30s; clamped 1s–5m) |
| `cors` | no | id of a `cors` entry (must exist) |
| `headers` | no | id of a `headers` entry (must exist) |
| `authentication` | no | a service route, or routing specs: `'default: svc'`, `'header: svc'`, `'header: value: svc'` |
| `upload` | no | `true`/`false` (default false) — enable multipart upload |
| `tracing` | no | `true`/`false` (default false) — enable distributed tracing |
| `trace.id.header` | no | per-endpoint override of the global `http.trace.id.header` (default `X-Trace-Id`) — impedance matching for a caller that sends its own trace-id header name; a well-formed W3C `traceparent` still takes precedence |
| `correlation.id.header` | no | per-endpoint override of the global `http.correlation.id.header` (default `X-Correlation-Id`) — impedance matching for a caller that sends its own business correlation-id header name |
| `trust_all_cert` | no | `true`/`false` — **HTTPS relay only** |
| `url_rewrite` | no | a list of **exactly two** strings `[from, to]` — **HTTP(S) relay only** |

Boolean fields (`upload`, `tracing`, `trust_all_cert`) are written **unquoted**, e.g. `tracing: true`.
The header-override values are header names, written quoted like other strings, e.g.
`trace.id.header: 'X-Legacy-Trace'` (header capture is case-insensitive; precedence is
per-entry > `application.properties` global > built-in default — see
[Observability](../observability.md#impedance-matching)).

## Binding modes {#binding}

How an endpoint reaches its backend (set by `service`/`flow`):

| Mode | Config | Behavior |
|---|---|---|
| **Function** | `service: my.function.route` | route the request to a composable function |
| **Flow** | `service: http.flow.adapter` + `flow: my-flow-id` | run an [Event Script flow](../event-script/flow-grammar.md) |
| **HTTP relay** | `service: https://host` | proxy to an external URL (`url_rewrite`, `trust_all_cert` apply) |
| **A/B (dual)** | `service: [primary, secondary]` | primary serves the response; secondary gets a copy |

`{param}` tokens in `url` are extracted and passed to the backend (e.g. `/api/profile/{id}`).

## `cors:` block {#cors}

| Field | Required | Meaning |
|---|---|---|
| `id` | **yes** | unique id referenced by a `rest` entry's `cors:` |
| `options` | **yes** | headers for the preflight (`OPTIONS`) response |
| `headers` | **yes** | headers for normal responses |

Each entry in `options`/`headers` **must** be an `Access-Control-*: value` line, e.g.
`'Access-Control-Allow-Origin: *'`. A complete cross-origin config typically also lists
`Access-Control-Allow-Methods` and `Access-Control-Allow-Headers` (see the
[worked example](ai-agent-guide.md#example)).

## `headers:` block {#headers}

| Field | Required | Meaning |
|---|---|---|
| `id` | **yes** | unique id referenced by a `rest` entry's `headers:` |
| `request` | no | transforms on the inbound request: `add` / `drop` / `keep` |
| `response` | no | transforms on the outbound response: `add` / `drop` / `keep` |

`add` entries are `'header-name: value'`; `drop`/`keep` are header names (case-insensitive). `keep`
retains only the listed headers (drops the rest).

## Invariants {#invariants}

Parser rules — violate them and the route won't load:

1. Every `rest` entry has `service`, `methods`, and `url`.
2. `methods` values come from `GET PUT POST DELETE HEAD PATCH` (`OPTIONS` is auto-added — don't list it).
3. A `cors:`/`headers:` reference must match an existing `cors`/`headers` `id`.
4. `url_rewrite` is a list of **exactly two** strings; `url_rewrite` and `trust_all_cert` apply
   **only** to `http(s)://` relay services.
5. An HTTP-relay `service` must be a **single** URL (not a list, not mixed with function routes).
6. `flow` binding uses `service: http.flow.adapter`.

## See also {#see-also}

- [`rest-automation.json`](rest-automation.json) — the machine-readable form of this grammar.
- [AI agent guide](ai-agent-guide.md) — authoring rest.yaml deterministically.
- [REST Automation](index.md) — worked examples; [Event Script flow grammar](../event-script/flow-grammar.md) — the flows endpoints bind to.
