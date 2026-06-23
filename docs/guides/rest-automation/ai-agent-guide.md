---
title: AI agent guide — authoring rest.yaml
summary: The authoritative context an AI agent needs to author REST automation (rest.yaml)
  deterministically — the structure, binding modes, a pre-write checklist, and a worked example.
layer: platform-core
audience: [ai-agent, developer]
keywords: [rest automation, rest.yaml, authoring, context engineering, deterministic, http endpoint]
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

A flow-backed endpoint plus a reusable CORS config:

```yaml
rest:
  - service: 'http.flow.adapter'
    flow: 'order-status'
    methods: ['POST']
    url: '/api/orders/{order_id}/status'
    timeout: 30s
    cors: cors_1
    headers: header_1
    tracing: true

cors:
  - id: cors_1
    options:
      - 'Access-Control-Allow-Origin: *'
      - 'Access-Control-Allow-Methods: GET, POST, OPTIONS'
      - 'Access-Control-Allow-Headers: Origin, Authorization, Content-Type'
    headers:
      - 'Access-Control-Allow-Origin: *'

headers:
  - id: header_1
    response:
      add:
        - 'x-powered-by: mercury'
      drop:
        - 'server'
```

A function-backed endpoint with a path parameter:

```yaml
rest:
  - service: 'profile.lookup'
    methods: ['GET']
    url: '/api/profile/{id}'
    timeout: 10s
```

## See also {#see-also}

- [REST automation grammar](rest-grammar.md) + [`rest-automation.json`](rest-automation.json) — the source of truth.
- [REST Automation](index.md) — worked examples and the full feature set.
