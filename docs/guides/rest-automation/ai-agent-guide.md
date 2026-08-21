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

The canonical fixture covers function, flow, and HTTP relay bindings. The documentation includes
the same file that the production `RoutingEntry` loader resolves in tests:

```yaml
--8<-- "contracts/src/main/resources/mercury/agent-skill/references/fixtures/rest-bindings.yaml"
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

## See also {#see-also}

- [REST automation grammar](rest-grammar.md) + [`rest-automation.json`](rest-automation.json) — the source of truth.
- [REST Automation](index.md) — worked examples and the full feature set.
