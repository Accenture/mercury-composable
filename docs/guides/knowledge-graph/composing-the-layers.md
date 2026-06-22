---
title: Composing the layers
summary: How an Active Knowledge Graph reaches external APIs (data-dictionary + provider nodes),
  delegates to sub-graphs and Event Script flows (graph.extension), and is exposed as a REST
  endpoint through the graph-executor flow.
layer: knowledge-graph
audience: [architect, developer]
keywords: [data dictionary, provider, graph.api.fetcher, graph.extension, flow extension, graph-executor, api/graph]
related:
  - guides/knowledge-graph/skills-reference.md
  - guides/knowledge-graph/index.md
  - guides/event-script/syntax.md
---

# Composing the layers

> **At a glance**
>
> - An Active Knowledge Graph is the top of Mercury's ascent — it **composes the layers beneath it
>   without coupling**: down to Event Script flows and composable functions, out to external APIs,
>   and up to any protocol.
> - **Out** — `graph.api.fetcher` calls HTTP APIs declaratively via **data-dictionary** + **provider**
>   nodes. **Down** — `graph.extension` runs a **sub-graph** or an **Event Script flow**. **Up** — the
>   `graph-executor` flow exposes a deployed graph at `POST /api/graph/{graph-id}`.

## Reaching external APIs: data dictionary + provider {#data-dictionary}

`graph.api.fetcher` ([reference](skills-reference.md#api-fetcher)) doesn't hard-code endpoints. It
reads two kinds of configuration node:

- a **provider** node — *where* and *how* to call a service (URL, method, request mapping);
- a **data-dictionary** node — *what* attribute you want, which provider serves it, and how to map
  the response.

**Provider** — define the endpoint once and reuse it:

```
create node mdm-profile
with type Provider
with properties
purpose=Master Data Management profile endpoint
url=http://127.0.0.1:${rest.server.port:8080}/api/mdm/profile/{id}
method=GET
feature[]=log-request-headers
input[]=text(application/json) -> header.accept
input[]=person_id -> path_parameter.id
```

Provider input targets map into the HTTP request: `header.*`, `query.*`, `path_parameter.*`, `body.*`.
A URL token like `{id}` is filled from `path_parameter.id` (matched by name). Note the two `input[]`
forms: in a **Dictionary**, a bare `input[]=person_id` *declares* a required input parameter (the
fetcher supplies its value); in a **Provider**, `input[]=source -> target` is a *mapping* into the
request.

**Data dictionary** — name an attribute, point it at a provider, and map the response into `result`:

```
create node person-name
with type Dictionary
with properties
purpose=name of a person
provider=mdm-profile
input[]=person_id
output[]=response.profile.name -> result.name
```

**Fetcher** — the active node that pulls one or more dictionary attributes:

```
create node fetcher
with type Fetcher
with properties
skill=graph.api.fetcher
dictionary[]=person-name
dictionary[]=person-address
input[]=input.body.person_id -> person_id
output[]=result.name -> output.body.name
output[]=result.address -> output.body.address
```

Because two dictionary items here share one provider and the same `person_id`, the engine
**deduplicates** them into a single HTTP call. (The shipped *Tutorial 3* is the clean end-to-end
version of this example.)

**Config nodes are referenced, not connected.** The fetcher wires in Dictionary and Provider nodes
*by name* (`dictionary[]=`, `provider=`), so they need **no** `connect` lines and are exempt from
the no-orphans rule — they're configuration, not part of the traversal. You may optionally group
them under a `graph.island` node for visualization (Tutorial 3 shows this), but it is not required.

In a Dictionary's `output[]`, the source namespace `response.*` is the **provider's raw HTTP
response body**, mapped into the dictionary's `result.*` set; the fetcher's `output[]` then maps
that `result.*` into `output.*` (or `model.*`).

## Delegating down: graph.extension {#extension}

`graph.extension` ([reference](skills-reference.md#extension)) lets a graph call another graph as a
reusable module — the composition pattern for building larger capabilities from smaller, certified
graphs:

```
create node performance-evaluator
with type Extension
with properties
skill=graph.extension
extension=evaluate-sales-performance
input[]=input.body.department_id -> id
output[]=result.sales_performance -> output.body.sales_performance
```

The sub-graph runs in isolation; only the mapped `output[]` flows back into the caller's state.

### Delegating to an Event Script flow {#flow-extension}

The same skill bridges to the **composable layer**: prefix the target with `flow://` and the node
delegates to an [Event Script](../event-script/syntax.md) flow instead of a graph. This is the pro-code escape
hatch — hand demanding logic to a flow of composable functions, then return to the graph:

```
create node extension
with type Extension
with properties
skill=graph.extension
extension=flow://flow-11
input[]=input.body.hello -> hello
input[]=input.body.message -> message
output[]=result -> output.body
```

```yaml
# flow-11.yml — the flow on the other side of the bridge
tasks:
  - input:
      - 'input.body -> *'
    process: 'no.op'
    output:
      - 'result -> output.body'
    execution: end
```

A graph can thus call a flow, and a flow can call composable functions — each layer delegating
downward by name, never by direct reference.

## Exposing a graph: the graph-executor flow {#exposure}

A deployed graph is not invoked directly — an Event Script flow wraps it, which is how it gets a
REST endpoint while keeping execution decoupled from the protocol.

```yaml
# rest.yaml — the generic graph endpoint
- service: 'http.flow.adapter'
  methods: ['POST', 'GET']
  url: '/api/graph/{graph_id}'
  flow: 'graph-executor'
  timeout: 30s
  tracing: true
```

```yaml
# graph-executor.yml — wraps the engine as a flow task
flow:
  id: 'graph-executor'
  exception: 'graph.exception.handler'
first.task: 'graph.executor'
tasks:
  - input:
      - 'model.instance -> header.instance'
      - 'input.path_parameter.graph_id -> header.graph'
    process: 'graph.executor'
    output:
      - 'status -> output.status'
      - 'header -> output.header'
      - 'result -> output.body'
    execution: end
```

So a request flows: `http.flow.adapter` → `graph-executor` flow → `graph.executor` (loads the model
by `graph_id`, traverses it) → `async.http.response`. Calling it:

```bash
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-3 \
  -H "Content-Type: application/json" \
  -d '{"person_id": 100}'
```

```json
{"name": "Peter", "address": "100 World Blvd"}
```

Because the protocol lives in the flow, the *same* graph can later be driven by other adapters
(e.g. a Kafka event listener) with no change to the model.

## See also {#see-also}

- [Built-in skills reference](skills-reference.md) — `graph.api.fetcher` and `graph.extension` in full.
- [Event Script Syntax](../event-script/syntax.md) — the flow layer on the other side of `flow://` and the
  `graph-executor` wrapper.
- [Knowledge Graph as Application](index.md#layer-integration) — the layer model this realizes.
