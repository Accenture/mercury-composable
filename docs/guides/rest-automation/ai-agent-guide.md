---
title: AI agent guide — authoring rest.yaml
summary: The authoritative context an AI agent needs to author REST automation (rest.yaml)
  deterministically — the structure, binding modes, a pre-write checklist, and a worked example.
layer: platform-core
audience: [ai-agent, developer]
keywords: [rest automation, rest.yaml, authoring, context engineering, deterministic, http endpoint, language packs, python, node.js]
related:
  - guides/rest-automation/rest-grammar.md
  - guides/rest-automation/index.md
---

# AI agent guide — authoring rest.yaml

> **At a glance**
>
> - **Read this if you are an AI agent** asked to add or change an HTTP endpoint. It is the context
>   you need — you should not have to read the parser source.
> - **Generate from rules.** The [REST grammar](rest-grammar.md) and its machine-readable form
>   [`rest-automation.json`](rest-automation.json) are the source of truth.
> - `rest.yaml` is parsed and validated at startup (`RoutingEntry`); an invalid entry fails to load.

## Decide the binding first {#binding}

Pick how the endpoint reaches its backend, then fill the entry:

- **Function** → `service: <function.route>`.
- **Flow** → `service: http.flow.adapter` **and** `flow: <flow-id>`.
- **HTTP relay** → `service: https://host` (single URL; `url_rewrite`/`trust_all_cert` allowed).
- **In a language pack** (Python, Node.js) only the **Function** binding exists — see
  [Authoring for a language pack](#language-packs).

## Generate deterministically {#deterministic}

Look up exact fields/values in [`rest-automation.json`](rest-automation.json); follow the rules in
[`rest-grammar.md`](rest-grammar.md). Then verify:

> **Pre-write checklist**
> - [ ] The entry has `service`, `methods`, and `url`.
> - [ ] `methods` are from `GET PUT POST DELETE HEAD PATCH` — do **not** list `OPTIONS` (auto-added).
> - [ ] Binding is consistent: a `flow` uses `service: http.flow.adapter`; an `http(s)://` relay
>       `service` is a single URL.
> - [ ] Any `cors:`/`headers:` value matches an existing `cors`/`headers` `id` in the same file.
> - [ ] `url_rewrite` (if present) is a list of exactly two strings, and only on a relay service.
> - [ ] `{param}` tokens in `url` are balanced and not nested.

## Worked example {#example}

The canonical example below covers all three binding forms — a flow-backed endpoint with
reusable CORS and response-header configs, a function-backed endpoint with a path parameter,
and an HTTP relay with `url_rewrite`. This exact file is also loaded through the production
`RoutingEntry` parser in a platform-core test, so the example cannot drift from the router:

```yaml
--8<-- "system/platform-core/src/test/resources/guide-fixtures/rest-bindings.yaml"
```

A traced endpoint serving a legacy caller that uses its own trace/correlation header names
(per-endpoint impedance matching — the optional `trace.id.header` / `correlation.id.header` /
`traceparent.header` keys override the global `http.trace.id.header` / `http.correlation.id.header` /
`http.traceparent.header` names for this entry only; the standard W3C `traceparent` always takes
precedence, and a custom `traceparent.header` name is read only when the standard header is
absent. These overrides are for backward compatibility with legacy systems only — the standard
W3C/OTel `traceparent` needs no configuration and departing from it is discouraged):

```yaml
rest:
  - service: 'legacy.orders'
    methods: ['POST']
    url: '/api/legacy/orders'
    timeout: 15s
    tracing: true
    trace.id.header: 'X-Legacy-Trace'
    correlation.id.header: 'X-Legacy-Cid'
```

## Authoring for a language pack {#language-packs}

A `rest.yaml` served by the Python or Node.js pack (RFC-0018) follows this grammar with one binding and a few
boundaries; the file lives at `resources/rest.yaml` next to the application file and is read when the pack's
`application.yml` sets `rest.automation: true`.

> **Pre-write checklist (pack)**
> - [ ] `service` is a **function route of the pack application** — never `flow:` or
>       `service: http.flow.adapter`, never an `http(s)://` relay, never a list (each fails the load by name).
> - [ ] The function reads the `AsyncHttpRequest` map (`AsyncHttpRequest(body)` in Python,
>       `new AsyncHttpRequest(body)` in Node.js), never the bare HTTP body, and replies with a value, an
>       envelope (status, headers) or an exception (the standard error body).
> - [ ] `authentication:` names a function that returns `true` — its reply headers become the request's
>       `session` map — or raises a 401.
> - [ ] `stream: true` → an interceptor writing through the pack's `EventStreamWriter`; `upload: true` → a
>       `POST` entry whose files arrive in the request map within `rest.upload.max.bytes`.
> - [ ] No `custom.content.types`, `mime.types` or XML (deferred); `static-content`'s `no-cache-pages` and
>       `filter` apply; `POST /api/event` and the actuators are the default entries.

```yaml
rest:
  - service: 'profile.lookup'
    methods: ['GET']
    url: '/api/profile/{id}'
    timeout: 10s
    authentication: 'api.key.check'
    tracing: true
```

The function code for each language is in the packs' AI agent guides:
[Python](https://accenture.github.io/mercury-python/guides/ai-agent-guide/) ·
[Node.js](https://accenture.github.io/mercury-nodejs/guides/ai-agent-guide/).

## See also {#see-also}

- [REST automation grammar](rest-grammar.md) + [`rest-automation.json`](rest-automation.json) — the source of truth.
- [REST Automation](index.md) — worked examples and the full feature set;
  [in the language packs](index.md#rest-automation-in-the-language-packs).
- The packs' REST Automation guides:
  [Python](https://accenture.github.io/mercury-python/guides/rest-automation/) ·
  [Node.js](https://accenture.github.io/mercury-nodejs/guides/rest-automation/).
