---
title: Test Report — The agent loop's trace lineage, continuous from the engine into both language packs
summary: Permanent record of the first end-to-end span lineage drive of the agent loop - the diagram-assistant graph of
  the Playground example on Claude Haiku 4.5 through the Java engine, the Python and the Node.js LLM helper and MCP
  binding, with the public Mermaid Chart MCP server - what the baseline showed (the pack legs absent), what was changed
  in both packs (the callee's records, as a Java engine leaves them), and the continuous tree the re-drive produced.
layer: reference
audience: [developer, architect]
keywords: [tracing, span lineage, opentelemetry, distributed trace, event-over-http, agent loop, llm.run, llm.chat, mcp binding, test report]
---

# Test Report — The agent loop's trace lineage through both language packs

*The first drive whose question was the trace itself: does a span lineage stay continuous from the Java engine's REST edge
through the agent loop into the language packs - the LLM helper's `llm.chat` and the MCP binding's route - and back? Conducted
2026-11-14 (UTC+8) with the Playground example's `diagram-assistant` graph on Claude Haiku 4.5, the Java Playground
4.12.30, the Python pack ([mercury-python](https://github.com/Accenture/mercury-python)) and the Node.js pack
([mercury-nodejs](https://github.com/Accenture/mercury-nodejs)) from `main` of the day, and the public Mermaid Chart MCP
server behind the binding. Eric set the acceptance criterion before the drive: for OpenTelemetry to be certified, the span
lineage ought to be continuous end to end. The drive ran negative control first - the packs as they were - then again after
the change this report records.*

## Result in brief

- **Baseline: every record chained, and the pack legs were missing.** Each run logged 11 engine records, each with a parent
  some record owned, and no record from either pack. `llm.chat` (two turns) and `diagram.render.mcp` (the MCP tool) appeared
  only as time inside the engine's `llm.run` and `diagram.render`. A tree with no orphans is not the same as a tree with
  every hop.
- **Cause: two rules met, with nothing between them.** The calling engine records nothing for an RPC over Event-over-HTTP,
  because its HTTP client leg `async.http.request` is in `skip.rpc.tracing` by default; a Java or Rust callee fills that
  gap with its own three records - the `http.request` edge for `POST /api/event`, `event.api.service` and the function's
  round-trip record from its local inbox. The packs suppressed the worker-side record of every RPC-served execution, as the
  engines do, but left none of the callee's records in its place, and their replies carried no span for an engine inbox
  to chain onto. The forwarder drops a record without a valid span id, so no backend could have shown the hop either.
- **Fix: the packs now leave what a Java callee leaves** ([mercury-python #53](https://github.com/Accenture/mercury-python/pull/53),
  [mercury-nodejs #120](https://github.com/Accenture/mercury-nodejs/pull/120), Eric's choice among three shapes): a traced
  call at `/api/event` leaves `http.request` (the edge's round trip, parented onto the caller's span - the envelope's span,
  else the `traceparent` header's; the backend's SERVER span), `event.api.service` (the ingress, parented onto the edge)
  and, for an RPC, the function's round-trip record with the responder's span, parented onto the service; a reply carries
  the responder's span (`WorkerHandler` parity) and the packs' `PostOffice` records a local RPC round trip as `InboxBase` does.
- **After: 20 records per run, every parent owned by a record, in both packs.** The helper's edge sits under the engine's
  `llm.run` and the binding's under `diagram.render`; each function sits under its `event.api.service`; the timings nest.
  The tree is now the same shape whether the callee is a Java engine or a pack.
- **A declared function's name never enters a trace.** The MCP binding's `mcp.tool`, the one function behind every bound
  route since mercury-python #52 and mercury-nodejs #119, appears nowhere: every trace field comes from the service entry
  the event was addressed to, and the route name `diagram.render.mcp` is the hop's identity.

## Setup

| | |
|---|---|
| Graph | `diagram-assistant` (`examples/minigraph-playground`), deployed at startup; one request `POST /api/graph/diagram-assistant` with `{"question": "Draw a flowchart of a coffee order: order, pay, brew, serve"}`, the REST entry traced (`tracing: true`) |
| Engine | Java Playground 4.12.30 (`minigraph-playground`), `-Dllm.peer.port` and `-Dmcp.peer.port` selecting the pack |
| Python pack | LLM helper on 8086 (`-Dllm.model=claude-haiku-4-5`), MCP binding on 8088 with the example's `mcp/mermaid-servers.yml` |
| Node.js pack | LLM helper on 8087, MCP binding on 8089, the same servers file |
| Model | Claude Haiku 4.5 (`claude-haiku-4-5-20251001`), the Claude API backend; two turns per run, one tool call |
| MCP server | the public Mermaid Chart endpoint, streamable HTTP, no credential; `diagram.render` stores the SVG out of band |
| Records | the engine's `Telemetry` INFO lines; the packs' `distributed.tracing` log lines (`{"trace": {...}}`, the same record) |
| Lineage check | every record of the trace from every process, sorted by `start`; a record's `parent_span_id` must be the `span_id` of a record in the set, or absent (the root); a record without a `span_id` counts as broken, because the forwarder would drop it |

The baseline ran the packs at `main` after the `preload` shape refactors (mercury-python #52, mercury-nodejs #119); the
re-drive ran them from the branches of #53 and #120. The Playground jar, the graph, the servers file and the request were
identical in all four runs.

## Scenario 1 — the baseline, negative control first

Both packs: 11 engine records, no pack record. The `Handled` lines of the packs carried the engine's trace id, proof that
the trace context crossed the wire, but nothing was recorded for the execution on either side.

**Python pack** (trace `c355fab55c6f469a8d1b59b57e9b24e4`): the engine's records, the pack's three `Handled` lines (`llm.chat` 3820 ms and 2607 ms,
`diagram.render.mcp` 3701 ms) and no pack record. `llm.run` spans 10,296 ms, `diagram.render` 3,718 ms; the pack's time is
inside them.

| host | service | from | span_id | parent_span_id | exec ms | round trip ms | parent |
|---|---|---|---|---|---|---|---|
| engine | `http.request` | - | `8641da954e777566` | `-` | 10334.725 | - | root |
| engine | `http.flow.adapter` | http.request | `b7df1ae81a12717a` | `8641da954e777566` | 1.071 | - | ok |
| engine | `task.executor` | event.script.manager | `ba4baffab6d98b43` | `b7df1ae81a12717a` | 10324.0 | - | ok |
| engine | `graph.island` | graph.executor | `bc0873791fe0bfb1` | `b7df1ae81a12717a` | 0.028 | - | ok |
| engine | `graph.math` | graph.executor | `8b3f6c1ed167d4fc` | `b7df1ae81a12717a` | 4.923 | - | ok |
| engine | `graph.task` | graph.executor | `9a9ccbdc31cedc6f` | `8b3f6c1ed167d4fc` | 26.601 | - | ok |
| engine | `llm.run` | graph.task | `939793b4bba9f1ab` | `9a9ccbdc31cedc6f` | 10296.427 | 10299.124 | ok |
| engine | `diagram.render` | llm.run | `9c59e0762aabe43a` | `939793b4bba9f1ab` | 3718.051 | 3719.147 | ok |
| engine | `graph.math` | graph.executor | `8ab661311854695a` | `9a9ccbdc31cedc6f` | 0.414 | - | ok |
| engine | `graph.data.mapper` | graph.executor | `b996855d5b845668` | `8ab661311854695a` | 0.191 | - | ok |
| engine | `async.http.response` | task.executor | `b6c3fe29510f78a6` | `b996855d5b845668` | 2.381 | - | ok |

**Node.js pack** (trace `b9651c5106b34d93bc6ee4cb845875d7`): the same shape; the pack's `Handled` lines read `llm.chat` 3146 ms and 2609 ms,
`diagram.render.mcp` 3580 ms.

| host | service | from | span_id | parent_span_id | exec ms | round trip ms | parent |
|---|---|---|---|---|---|---|---|
| engine | `http.request` | - | `bf8084a22cceada5` | `-` | 9572.623 | - | root |
| engine | `http.flow.adapter` | http.request | `90a1496ec0c522ea` | `bf8084a22cceada5` | 0.839 | - | ok |
| engine | `task.executor` | event.script.manager | `bdaa3b9053fe711f` | `90a1496ec0c522ea` | 9561.0 | - | ok |
| engine | `graph.island` | graph.executor | `bdca61ddaf384824` | `90a1496ec0c522ea` | 0.03 | - | ok |
| engine | `graph.math` | graph.executor | `8da6de0393ffd2cf` | `90a1496ec0c522ea` | 5.917 | - | ok |
| engine | `graph.task` | graph.executor | `87158136c095b10a` | `8da6de0393ffd2cf` | 30.062 | - | ok |
| engine | `llm.run` | graph.task | `a0e87f1be17a7e84` | `87158136c095b10a` | 9533.167 | 9535.805 | ok |
| engine | `diagram.render` | llm.run | `8cd27d68b53df6e1` | `a0e87f1be17a7e84` | 3609.309 | 3610.847 | ok |
| engine | `graph.math` | graph.executor | `84d92f8f55442549` | `87158136c095b10a` | 0.289 | - | ok |
| engine | `graph.data.mapper` | graph.executor | `9f4bb96eeaf01aba` | `84d92f8f55442549` | 0.19 | - | ok |
| engine | `async.http.response` | task.executor | `b1563c706333c33c` | `9f4bb96eeaf01aba` | 2.797 | - | ok |

## Scenario 2 — after the change, both packs

**Python pack** (trace `4c46977ba8fc4ecea5bfac870a51bbb3`): 20 records, continuous.

| host | service | from | span_id | parent_span_id | exec ms | round trip ms | parent |
|---|---|---|---|---|---|---|---|
| engine | `http.request` | - | `90533de404457c66` | `-` | 10069.744 | - | root |
| engine | `http.flow.adapter` | http.request | `b3a2c3c9b598c13f` | `90533de404457c66` | 0.883 | - | ok |
| engine | `task.executor` | event.script.manager | `9d4bda26039b91b3` | `b3a2c3c9b598c13f` | 10059.0 | - | ok |
| engine | `graph.island` | graph.executor | `ab1a30d1b018c70c` | `b3a2c3c9b598c13f` | 0.021 | - | ok |
| engine | `graph.math` | graph.executor | `92c88a8d2cf99afd` | `b3a2c3c9b598c13f` | 4.348 | - | ok |
| engine | `graph.task` | graph.executor | `a425f855ffa0691a` | `92c88a8d2cf99afd` | 23.687 | - | ok |
| engine | `llm.run` | graph.task | `9fc9d0fe9f1a5f3c` | `a425f855ffa0691a` | 10031.002 | 10033.3 | ok |
| helper | `event.api.service` | http.request | `d5d1e3d1616202bd` | `46ac21f457226487` | 0.019 | - | ok |
| helper | `llm.chat` | llm.run | `99708517341c4a39` | `d5d1e3d1616202bd` | 3499.869 | 3500.314 | ok |
| helper | `http.request` | - | `46ac21f457226487` | `9fc9d0fe9f1a5f3c` | 3500.668 | - | ok |
| engine | `diagram.render` | llm.run | `ad3646ad1b7a328e` | `9fc9d0fe9f1a5f3c` | 3588.896 | 3590.376 | ok |
| binding | `event.api.service` | http.request | `ccc149d697f97815` | `a94233dbd794064f` | 0.026 | - | ok |
| binding | `diagram.render.mcp` | diagram.render | `a039ef66d152f5cc` | `ccc149d697f97815` | 3566.176 | 3566.967 | ok |
| binding | `http.request` | - | `a94233dbd794064f` | `ad3646ad1b7a328e` | 3567.438 | - | ok |
| helper | `event.api.service` | http.request | `61e2db42b2eaecd2` | `bd7689db3f676a2e` | 0.012 | - | ok |
| helper | `llm.chat` | llm.run | `b6bb117919c06d7a` | `61e2db42b2eaecd2` | 2798.049 | 2798.473 | ok |
| helper | `http.request` | - | `bd7689db3f676a2e` | `9fc9d0fe9f1a5f3c` | 2798.951 | - | ok |
| engine | `graph.math` | graph.executor | `aa5768e4b4642161` | `a425f855ffa0691a` | 0.451 | - | ok |
| engine | `graph.data.mapper` | graph.executor | `bdb794a15c6c984f` | `aa5768e4b4642161` | 0.17 | - | ok |
| engine | `async.http.response` | task.executor | `b0bcb7743b47b62b` | `bdb794a15c6c984f` | 2.067 | - | ok |

**Node.js pack** (trace `b20b5f5f47fa46a49c3e753f5ca38e85`): 20 records, continuous.

| host | service | from | span_id | parent_span_id | exec ms | round trip ms | parent |
|---|---|---|---|---|---|---|---|
| engine | `http.request` | - | `82124d253853443c` | `-` | 8876.159 | - | root |
| engine | `http.flow.adapter` | http.request | `b8149dc0340230f1` | `82124d253853443c` | 1.046 | - | ok |
| engine | `task.executor` | event.script.manager | `be26e6919cdfc376` | `b8149dc0340230f1` | 8864.0 | - | ok |
| engine | `graph.island` | graph.executor | `a473f64319c93c4b` | `b8149dc0340230f1` | 0.023 | - | ok |
| engine | `graph.math` | graph.executor | `a093f15db84b2bf9` | `b8149dc0340230f1` | 4.753 | - | ok |
| engine | `graph.task` | graph.executor | `a48f81d4537cb301` | `a093f15db84b2bf9` | 24.197 | - | ok |
| engine | `llm.run` | graph.task | `8d1d736484978dfb` | `a48f81d4537cb301` | 8840.7 | 8842.776 | ok |
| helper | `event.api.service` | http.request | `e014988bc1a89bff` | `0b2e0700e756a83f` | 0.066 | - | ok |
| helper | `llm.chat` | llm.run | `a3e87dc00d436777` | `e014988bc1a89bff` | 2776.574 | 2781.262 | ok |
| helper | `http.request` | - | `0b2e0700e756a83f` | `8d1d736484978dfb` | 2782.075 | - | ok |
| engine | `diagram.render` | llm.run | `bcfa910a8cf06485` | `8d1d736484978dfb` | 3460.984 | 3462.028 | ok |
| binding | `event.api.service` | http.request | `2df4e455aaf9d740` | `e69aa4d1bdfa5c36` | 0.076 | - | ok |
| binding | `http.request` | - | `e69aa4d1bdfa5c36` | `bcfa910a8cf06485` | 3441.39 | - | ok |
| binding | `diagram.render.mcp` | diagram.render | `c5979c25c62dcc01` | `2df4e455aaf9d740` | 3433.784 | 3440.407 | ok |
| helper | `event.api.service` | http.request | `7b373c19bf8a3a6f` | `0546a93f2f0123df` | 0.013 | - | ok |
| helper | `http.request` | - | `0546a93f2f0123df` | `8d1d736484978dfb` | 2454.382 | - | ok |
| helper | `llm.chat` | llm.run | `da1d512af3a0b6e4` | `7b373c19bf8a3a6f` | 2452.795 | 2453.944 | ok |
| engine | `graph.math` | graph.executor | `b60a8efcef274296` | `a48f81d4537cb301` | 0.183 | - | ok |
| engine | `graph.data.mapper` | graph.executor | `917d0a5f279912ff` | `b60a8efcef274296` | 0.136 | - | ok |
| engine | `async.http.response` | task.executor | `b70bea2461138349` | `917d0a5f279912ff` | 2.436 | - | ok |

Reading the tables: the engine's `llm.run` calls the helper twice (two model turns), and each call appears as the helper's
`http.request` edge under `llm.run`'s span, `event.api.service` under the edge, and `llm.chat` under the service with its
`round_trip`; the engine's `diagram.render` calls the binding once, and the binding's `http.request` edge sits under
`diagram.render`'s span with `event.api.service` and `diagram.render.mcp` beneath it. The engine's own records are the
baseline's eleven, unchanged. The packs' `from` fields name the engine function that called (`llm.run`, `diagram.render`)
because the engine's PostOffice stamps the calling route on the wire envelope.

## Findings

1. **An RPC over Event-over-HTTP is the callee's to record.** The caller's `async.http.request` leg is in `skip.rpc.tracing`
   by default so that an HTTP call folds into the calling function's span; the Java callee's `event.api.service` and REST
   edge are traced for exactly this reason (its javadoc: so the `/api/event` edge is visible in the span tree). A runtime
   that suppresses the responder's record for an RPC, as the engines do, has to leave the callee's records instead - the
   packs' "engine parity" had copied the suppression without the records that justify it. The rule, stated for all four
   runtimes: every traced execution leaves exactly one record; who emits it depends on the side, never on the runtime.
2. **The responder's span has to ride the reply.** A Java inbox names the round-trip record's span from the reply
   (`InboxBase.spanIdFromResponder`), and `WorkerHandler` puts it there. The packs' replies carried none, so even a Java
   caller recording the hop would have produced a record the forwarder drops (no valid span id). Both packs now set it.
3. **The alias shape is invisible, by construction.** `mcp.tool` never appears in any record; the trace knows the bound route
   only. This settles the question the drive began with.
4. **Timings nest as they should.** In every call the edge's `exec_time` covers the function's `round_trip`, which covers its
   `exec_time`; the engine's round trip to the pack exceeds the pack's edge by the wire's few milliseconds (for example
   `diagram.render` 3,588.9 ms and 3,590.4 ms round trip against the Python binding's edge 3,567.4 ms and
   `diagram.render.mcp` 3,566.2 ms with a 3,567.0 ms round trip).
5. **The guards.** mercury-python `tests/test_event_stream.py` and mercury-nodejs `test/event-stream.test.ts` pin the three
   callee records and their parents for a streamed call and for a plain RPC, the span on the reply, the refusal's edge
   record (404) and the silence of an untraced call; the local round-trip record replaces the former "RPC legs emit no
   dataset" pin in both packs.

## What this drive did not do

- **No backend confirmation.** The records were read from the logs, which are the forwarder's input, mapped one to one to
  spans (the 2026-09-22 [OpenTelemetry forwarder certification](otel-dynatrace-certification.md)). Eric's review in a
  Dynatrace tree remains the certification step, as it was then; that report's Scenario 2 carries an addendum pointing here.
- **The Rust engine was not the caller.** It speaks the same wire contract and stamps the same spans; a Rust Playground
  drive would show the same tree.
- **One model, one graph.** The trace shape does not depend on the model or the graph; the drive used the cheapest model that
  completes the diagram task.
