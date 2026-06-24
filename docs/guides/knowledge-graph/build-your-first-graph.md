---
title: Build your first Active Knowledge Graph
summary: A hands-on walkthrough — model a service in the MiniGraph Playground, dry-run it,
  then deploy it and call it over REST, without writing application code.
layer: knowledge-graph
audience: [developer]
keywords: [minigraph playground, tutorial, graph.data.mapper, instantiate, run, dry-run, deploy, /api/graph]
related:
  - guides/knowledge-graph/index.md
  - guides/event-script/syntax.md
---

# Build your first Active Knowledge Graph

> **At a glance**
>
> - **You'll build** — a tiny service that returns a message, then enhance it to echo its input,
>   then deploy it and call it over REST. No application code, only a graph model.
> - **You'll use** — the MiniGraph Playground (the interactive workbench) and one skill,
>   `graph.data.mapper`.
> - **Prerequisites** — the app running with `app.env=dev` and the Playground open in a browser;
>   about 10 minutes. New to the idea? Read [Knowledge Graph as Application](index.md) first.
> - **Conventions** — lines you type into the Playground inbox are shown in code blocks; the
>   console's reply follows under `>`.

## Step 1 — a root and an end {#hello-world}

Every graph has a **Root** (entry) and an **End** (exit). Create the root first:

```
create node root
with type Root
with properties
purpose=My first graph
```

Now create the end node and give it a **skill** — `graph.data.mapper` — so it actually does
something. The `mapping[]` entry copies the constant `hello world` into `output.body`, the
response payload:

```
create node end
with type End
with properties
skill=graph.data.mapper
mapping[]=text(hello world) -> output.body
```

Connect them so traversal can flow root → end (the label, here `done`, names the connection):

```
connect root to end with done
```

Run it. `instantiate graph` creates a runnable instance from the model; `run` traverses it:

```
instantiate graph
run
```

The console shows the traversal and the result:

```
> run
Walk to root
Walk to end
Executed end with skill graph.data.mapper in 1.7 ms
{
  "output": {
    "body": "hello world"
  }
}
Graph traversal completed in 9 ms
```

That is a complete, working Active Knowledge Graph: a root, an active node carrying a skill, and
an end — behavior expressed entirely as a model.

## Step 2 — make it echo the input {#echo-input}

A real service reacts to input. Update the end node to map the *request* body to the *response*
body instead of a constant:

```
update node end
with type End
with properties
skill=graph.data.mapper
mapping[]=input.body -> output.body
```

This time, **dry-run** with mock input — `instantiate graph` accepts seed values, here putting the
constant `it works` into `input.body.message`:

```
instantiate graph
  text(it works) -> input.body.message
run
```

```
> run
Walk to root
Walk to end
Executed end with skill graph.data.mapper in 0.4 ms
{
  "output": {
    "body": {
      "message": "it works"
    }
  }
}
Graph traversal completed in 2 ms
```

A dry-run needs no live dependencies — it's how you validate a model before deploying it.

## Step 3 — export the model {#export}

Export the graph to JSON so it can be deployed. The engine writes it to the temp graph location
(`location.graph.temp`, default `file:/tmp/graph`) and stamps the graph's name onto the root node:

```
export graph as my-first-graph
```

```
> export graph as my-first-graph
Added name=my-first-graph to Root node
Graph exported to /tmp/graph/my-first-graph.json
```

## Step 4 — deploy it {#deploy}

Deployment is a file copy: move the exported JSON into your project's deployed-graph folder. The
relevant `application.properties` keys:

```properties
# temp working location (must be file:/ — read/write)
location.graph.temp=file:/tmp/graph
# deployed model location (file:/ or classpath:/ — read-only)
location.graph.deployed=classpath:/graph
```

So copy the model into the classpath graph folder and restart the app to load it:

```
cp /tmp/graph/my-first-graph.json src/main/resources/graph/
```

## Step 5 — call it over REST {#call-over-rest}

A deployed graph is reachable at the generic endpoint `POST /api/graph/{graph-id}`, where the id is
the name you exported. Send it some input:

```
curl -X POST http://127.0.0.1:8085/api/graph/my-first-graph \
  -H "Content-Type: application/json" \
  -d '{"message": "it is a wonderful day"}'
```

```json
{"message": "it is a wonderful day"}
```

(The port follows your app's configuration; `8085` is illustrative.)

## What just happened {#how-it-ran}

The graph isn't called directly — it's wrapped by an [Event Script](../event-script/syntax.md) flow, which is
how it gets a REST endpoint without coupling execution to the protocol:

1. `http.flow.adapter` receives `POST /api/graph/my-first-graph` and runs the `graph-executor` flow.
2. `graph.executor` builds a graph instance and traverses root → end.
3. At the end node it invokes `graph.data.mapper`, which writes `output.body`.
4. `async.http.response` returns that body to your `curl`.

You can see exactly these steps in the application's telemetry log.

## What's next {#whats-next}

You built behavior with one skill. The graph becomes genuinely *active* when nodes **decide**,
**compute**, and **fetch**:

- `graph.math` — inline math and `IF/THEN/ELSE` branching for decisions.
- `graph.api.fetcher` — call external APIs declaratively via data-dictionary and provider nodes.
- `graph.extension` — delegate to a sub-graph or an Event Script flow.

Each of these gets a worked example in the [built-in skills reference](skills-reference.md).

## See also {#see-also}

- [Knowledge Graph as Application](index.md) — the concepts behind what you just built.
- [Event Script Syntax](../event-script/syntax.md) — the data-mapping syntax the skills share, and the flow
  layer that exposes graphs over REST.
