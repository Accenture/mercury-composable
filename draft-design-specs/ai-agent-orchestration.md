# AI agent orchestration on the Active Knowledge Graph — design concept

**Status:** CONCEPT — direction ratified by Eric 2026-08-25; experiments queued after the
AI workshop. This paper captures the thesis, the architecture concept, the open design
questions (Q-series), and the staged experiment plan (E-series). Numbered design
decisions (D-series) will be proposed against experiment evidence, not up front.
**Blueprint:** `bp-agent-orchestration` → serves `vision-mercury-composable`.
**Repo scope:** no engine surface at concept stage — everything below attaches at the
function tier (wrapper-side Python/Node functions), so no Java↔Rust lock-step is
triggered until/unless a built-in skill emerges (then `conv-telemetry-presentation-parity`
applies in full).

---

## 1. Context and driver

The industry is converging on **"graph engineering" as the next iteration of the agent
loop**: get the control flow of a multi-step AI application out of the model's implicit
`while(true)` and into an explicit, inspectable graph — nodes for reasoning and tool
calls, edges for transitions, checkpoints for human authority. LangGraph popularized the
shape; Anthropic's own "workflows vs agents" guidance points the same way for production
systems.

Mercury already made exactly this move for microservice orchestration: code → Event
Script → Active Knowledge Graph. The thesis of this concept is therefore not "build an
agent framework" but:

> **MiniGraph already is a graph-engineering runtime. The only missing piece is the AI
> node type — and the polyglot Event-over-HTTP work (shipped v4.11.11 + wrapper repos)
> makes that node type a user-land function, not an engine change.**

The pitch to enterprises inverts the usual one: agent orchestration is **just another
workload for the same governed runtime** that runs their integration flows — not a new
bespoke agent silo beside it. Changing what an agent does becomes *refining the model,
certifying it, and deploying the model* — the Vision sentence verbatim, with "agent"
substituted for "system".

## 2. Capability mapping — what the agent loop needs vs what is already shipped

| Agent-loop requirement | Shipped Mercury capability |
| --- | --- |
| Explicit, bounded control flow | Graph model + CompileGraph mandatory gate ("compiled or 404", ADR-0011) |
| Model-driven branching | Decision routing (`graph.math` IF/THEN/ELSE; Event Script `decision`) on an LLM's returned verdict/label |
| Iterative loops (reflect → act → check) | Wait-loop RESET pattern (tutorial-14; seen-marks + self-RESET), dynamic statement targets (v4.11.6) for generic retry/escalation handlers |
| Human-in-the-loop checkpoints | `graph.suspend`/`graph.resume` (ADR-0010/0012/0013): external state store, TTL, at-most-once resume, graph-scoped keys, jump-mode re-evaluation against new input — **production-proven by field multi-suspension approval workflows** |
| Tool calling | Composable functions: every Layer 1/2 capability (HTTP, Kafka, DB, whole flows) is a route-addressed, traced, concurrency-managed, mockable tool; `graph.task` reaches all of it |
| Polyglot AI ecosystem access | Event-over-HTTP wrappers (mercury-python / mercury-nodejs); graph.task's route guard consults `yaml.event.over.http` since v4.11.11 |
| Agent state / memory | State machine `model` namespace — explicit, inspectable (`inspect`), persisted across suspensions |
| Error handling around flaky calls | Walker-staged `error.source/code/message/stack` + recovery semantics (resolved/outstanding) + dynamic `{error.source}` targets |
| Concurrency economics for slow LLM I/O | Virtual threads (ADR-0002): thousands of blocked LLM RPCs suspend cheaply |
| Deadline propagation | flow/graph `ttl` bounds the event call; `headers.x-ttl` (ms) rides the wire end-to-end (tutorial-13 pattern) |
| Observability | OTel end-to-end incl. trace-id propagation into wrapper processes (proven live 2026-08-22, flow → python) |
| Token-free testing | Route decoupling → the LLM node mocks out by config (tutorial-13 `mock.mdm.profile` shape); dry-run lane simulates without spending |

**Evidence discipline (honest-evidence rule):** flows → python function is proven live
(2026-08-22 flagship drive, my_correlation_id + engine trace_id in the python log).
graph.task → wrapper function is pinned by engine tests on both engines (v4.11.11,
unit-test-task-7) but has **no live drive yet** — E0 below produces the first one. No
LLM or MCP call has run through Mercury yet; that is what the experiments are for.
Nothing in this section may be claimed in public docs beyond what has actually run.

## 3. Architecture concept

### 3.1 Attachment point: the function tier, engine stays neutral

All AI capability attaches as **functions**, never as engine features:

- The engine core stays LLM-free and vendor-neutral (Vision non-goal: "not locked to one
  LLM vendor"; posture precedent: `kafka-mesh-opt-in` — heavyweight integrations live in
  optional modules).
- The wrapper **scope fence is untouched** ("NO orchestration" in wrappers): the adapters
  below are *user-land functions built ON the wrappers* — demo-app or contrib-package
  code — not wrapper-core features.
- Taste precedent: `graphjs-phase-out-direction` — bounded, declarative capability beats
  embedded arbitrary power. The graph owns control flow; models advise within it.

### 3.2 Adapter family A — the LLM node (`llm.chat`)

A provider-neutral wrapper-side function (Python first; the LLM ecosystem lives there).
Illustrative contract sketch (Q2 refines it):

```
route: llm.chat                      (registered in a wrapper app; reached via
                                      yaml.event.over.http + graph.task / a flow task)
input (Map):
  messages | prompt                  conversation turns or single-turn text
  system                             optional system prompt
  schema                             optional JSON schema -> function enforces
                                     structured output (the graph needs parseable verdicts)
  params: model, max_tokens, temperature, ...
output (Map):
  text | data                        completion text, or schema-validated object
  usage: input_tokens, output_tokens
  stop_reason
errors:  provider/rate-limit errors ride the standard envelope status (portable to the
         graph's error context: error.source/code/message)
ttl:     function maps its remaining budget onto the provider SDK timeout (x-ttl pattern)
```

### 3.3 Adapter family B — MCP client (route-per-tool)

The official MCP SDKs are Python and TypeScript — exactly the two shipped wrappers.
Two integration shapes considered:

1. **Generic dispatcher** (`mcp.request` + `{server, tool, arguments}`) — quick, but the
   compile gate sees one opaque route; "read a file" and "send an email" are
   indistinguishable to governance. Not preferred.
2. **Route-per-tool** (preferred): the wrapper app connects to configured MCP servers and
   registers each **allowlisted** tool as its own route (`mcp.github.create.issue`, …).
   The graph names exactly which external capabilities it touches; the gate validates
   them; each call traces under its own route; **the allowlist itself is the
   certification artifact** ("what can this agent reach?" answered by config).

Security seam: MCP servers, their stdio child processes, and their credentials live in
the wrapper's process/environment, never the JVM. (This does not contradict the shelved
stdio-subprocess ruling — that ruling was about the *function transport*; an MCP client
managing its own children is internal library business.)

### 3.4 Adapter family C — agent-as-node (graphs of agents)

For genuinely open-ended agency: one node's function runs a classic inner tool-use loop
(e.g., Claude Agent SDK / API tool runner inside the Python wrapper) and returns an
outcome envelope. The graph orchestrates at the macro level — sequencing, approvals,
compensation — while open agency stays encapsulated in the node. This mirrors the
emerging "multi-agent = graph of agents" industry shape.

### 3.5 Reverse direction — Mercury as MCP *server*

Expose certified flows and deployed graphs as MCP tools so any external agent (Claude
Code/Desktop, any MCP client) can invoke Mercury capabilities with the governance gate
intact. Nearer than it looks: `system/ai-contract-provider` (ADR-0015) already serves
machine-readable discovery/contracts at :8999; an MCP facade over that discovery surface
plus the Event API is a thin adapter (Q7). Result: a certified graph becomes a tool in
every agent's toolbox — governed in both directions.

## 4. Orchestration patterns

1. **Bounded agency** (the graph decides, the model advises): LLM nodes classify /
   extract / judge; decision nodes route among *enumerated* branches. Fits the engine
   today and fits the enterprise story best — the certified toolset IS the graph.
2. **Agent-as-node**: macro-orchestration + checkpoints in the graph; inner loop
   encapsulated (3.4).
3. **Human-in-the-loop**: `graph.suspend` before any irreversible/side-effectful action;
   resume with approval; jump-mode re-evaluates the decision against the new input. The
   field's existing approval workflows are literally this pattern.
4. **Agentic loop, made visible**: reflect → act → check → loop-until-done via the
   wait-loop RESET idiom — bounded and inspectable instead of implicit.

## 5. Governance and security posture

- **"Governed nondeterminism" — never "deterministic agents."** The graph bounds the
  blast radius (only enumerated branches/tools reachable), makes decisions inspectable,
  and gates changes; it does not make model reasoning deterministic. Say exactly that.
- Promotion lifecycle (`bp-graph-governance-lifecycle`: dry-run → certify → stage →
  approve → production) is the differentiator no agent-loop framework offers.
- MCP's known risk class is prompt/tool injection via tool descriptions and results.
  Bounded-agency graphs structurally shrink it (tool results feed data mapping, not an
  open loop's next-tool choice); agent-as-node keeps the usual exposure inside the node —
  document the difference honestly.
- Audit: prompt/completion capture (trace annotations) needs a PII/redaction policy
  before any enterprise claim (Q4).
- Dry-run without tokens: mock the LLM/MCP routes by config; GraphTraveler simulates the
  agent for free.

## 6. Open design questions (Q-series — need rulings, mostly per experiment evidence)

- **Q1 — dynamic dispatch.** Should a model ever pick an arbitrary route at runtime?
  Today the gate validates routes statically; the working stance is
  *constraint-as-feature* (bounded agency), with agent-as-node as the escape hatch.
  Ruling needed only if a field case demands open dispatch.
- **Q2 — `llm.chat` production contract.** Streaming (engine supports Flux; the pattern
  to the REST edge needs design — Q8), token/usage accounting, structured-output
  enforcement, provider neutrality/pluggability, rate-limit retry semantics vs the
  graph's own retry handlers (avoid double-retry).
- **Q3 — MCP registration timing.** Static tool declaration vs boot-time enumeration of
  the server's tool list; allowlist config shape; failure behavior when a listed tool is
  absent at boot (fail-fast per the compiled-or-404 taste?).
- **Q4 — audit capture.** What of prompts/completions lands in trace annotations; PII
  redaction policy; retention.
- **Q5 — context economics.** Conversation history lives in the `model` namespace and is
  persisted on suspend — size management conventions (summarize-then-carry? cap?) before
  long-running agents.
- **Q6 — where adapters live.** Wrapper demo apps (E0) → a named contrib package
  (mercury-python extras?) → `extensions/`? Decide after E0–E2 show the shapes.
- **Q7 — Mercury-as-MCP-server facade.** Transport (HTTP), auth, and which surface is
  exposed: deployed graphs only? flows? individual functions? (Gate-respecting default:
  deployed graphs + explicitly exported flows.)
- **Q8 — streaming to the edge.** Token streaming from an LLM node through a graph run
  to the HTTP response — pattern using Flux/ObjectStream, or explicitly out of scope for
  v1 (request-response per node).

## 7. Experiment plan (E-series — post-workshop; each produces evidence for D-rulings)

- **E0 — the AI node exists (hours).** `llm.chat` in the mercury-python demo app
  (Anthropic SDK) + a bounded-agency demo graph: fetch → `llm.chat` (classify with
  `schema`) → decision node routes → respond. Zero engine change. **Also the first live
  graph.task → wrapper drive** (closes that evidence gap incidentally).
  *Evidence out:* the demo graph JSON, the live trace (engine trace_id in the python
  log), token usage surfaced in `model`.
- **E1 — human authority.** Insert `graph.suspend` before a side-effectful step; approve
  → resume; reject → re-suspend loop (tutorial-14 shape, LLM verdict as the decision
  input). *Evidence out:* suspension record contents with conversation context (feeds Q5).
- **E2 — MCP reach.** Route-per-tool prototype against one well-known MCP server
  (e.g., filesystem or github) with an explicit allowlist; graph calls two of its tools.
  *Evidence out:* registration timing findings (Q3), ttl mapping behavior, trace shape.
- **E3 — agent-as-node.** One node runs a bounded inner tool-use loop (Claude Agent
  SDK in the python wrapper), returns an outcome envelope; graph handles the approval
  checkpoint. *Evidence out:* envelope contract for "agent outcome", injection-surface
  notes.
- **E4 (stretch) — Mercury as MCP server.** Thin facade over ai-contract-provider
  discovery + Event API exposing one deployed graph as an MCP tool; drive it from Claude
  Code. *Evidence out:* Q7 answers.

After E0–E2: propose the D-series rulings + an ADR ("AI agent orchestration attaches at
the function tier; the engine stays neutral") and decide the public-docs story. Public
docs claim only drives that ran.

## 8. Relationship to existing blueprints / decisions

- **Distinct from `bp-ai-companion-llm-backend`** — that is AI at *design time*
  (co-authoring graphs); this is AI at *run time* (graphs orchestrating agents). They
  compound: the companion authors the agent graph; the graph governs the agent.
- **Builds on `bp-polyglot-functions`** — the wrappers are the enabler; this is their
  first strategic payload beyond parity demos.
- **Compounds `bp-graph-governance-lifecycle`** — promotion is the enterprise agent
  governance answer; this blueprint gives it a second, high-urgency customer.
- Precedents honored: `polyglot-event-over-http-design` (scope fence),
  `kafka-mesh-opt-in` (opt-in posture), `graphjs-phase-out-direction` (bounded > embedded
  power), `conv-telemetry-presentation-parity` (lock-step only if engine surface appears).

## 9. Non-goals

- Not a new agent framework silo; not a LangGraph clone — the runtime is the existing
  governed graph engine.
- No LLM SDK, MCP client, or vendor coupling in engine core — ever (Vision non-goal).
- No orchestration in wrappers (fence intact); adapters are functions on top.
- No determinism claims for model reasoning — the claim is governed nondeterminism.
- Not (yet) a streaming UX platform — Q8 may defer streaming out of v1.

## 10. Competitive positioning (one paragraph for future docs)

LangGraph is code-first graphs with checkpointing; Temporal is durable execution for
developers; Step Functions/Bedrock Flows are declarative but vendor-locked. Mercury's
angle: **declarative, zero-code-default agent graphs with a mandatory compile gate, a
promotion lifecycle, production-hardened suspend/resume, route-decoupled polyglot tools,
full OTel, and virtual-thread economics — on the same runtime that already runs your
integration flows.** One platform, both workloads, governed the same way.
