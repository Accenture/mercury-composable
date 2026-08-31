# HTTP response streaming (event/token streaming to the HTTP edge) — design spec

**Status:** APPROVED AND IMPLEMENTED (Java) — D1 ratified 2026-08-27; **D2–D8 approved
as recommended by Eric 2026-08-28**; implemented same day with one design addition made
during implementation (D9, the ordered reply lane — see §2a; **revised same day to
Eric's checkout-pool design**: dedicated lane per active stream, LIFO stack of 500,
HTTP-503 back-pressure, no config knob). Rust twin pending (D7:
Java-first with the vocabulary pinned). **Serves:** `bp-agent-orchestration` (this is Q8 of
`ai-agent-orchestration.md`, promoted to prerequisite: without it an `llm.chat` node can
only deliver a buffered completion). **Engine parity:** REST automation is engine
surface → Java + Rust lock-step with engine-identical vocabulary and framing
(`conv-telemetry-presentation-parity`; Java is the reference).

---

## 1. Problem and driver

The AI SDLC work needs progressive delivery over plain HTTP for three shapes, all of
which the industry serves with SSE today: chat token streams (OpenAI/Anthropic/Gemini
and every OpenAI-compatible server), agent progress events (MCP Streamable HTTP, A2A,
LangGraph/AI-SDK protocols — typed SSE events), and live watch of a running
workflow/graph (SSE for the live window; suspension stays on the existing
suspend/resume + cid model).

**Why the existing Flux/Mono path is not good enough (Eric's ruling).** Today a callee
can return a `Flux`/hand back `x-stream-id`, and `AsyncHttpResponse.handleStreamContent`
relays it chunked. But:

- The producer API is **Reactor-typed and JVM-only** — invisible to Event Script,
  graphs, and the polyglot wrappers (exactly where LLM tokens will come from).
- **Structured segments do not stream**: a `Map` chunk is buffered into `listOfMap` and
  rendered as one JSON array at completion — only raw text/bytes flow progressively.
- **No SSE**: no `text/event-stream` framing, no typed events, no in-band terminal
  event, and the mid-stream error path rewrites the HTTP status after the head may
  already be committed.
- The stream is a **separate object with its own lifecycle** (ObjectStreamIO create /
  expiry / housekeeping) when the event system already has a simpler primitive.

## 2. The model (RATIFIED)

Streaming is already native to platform-core at the event layer:

```
caller  ->  callee                      (the request, carrying reply_to + cid)
callee  ->  caller's reply_to           (continuous events - batches of tokens -
                                         until a signal declares end of transmission)
```

The HTTP edge **is** the caller: `HttpRouter` already sets
`reply_to = async.http.response@origin` with the request id as correlation id, and keeps
a per-request `AsyncContextHolder` with `touch()` keep-alive. Today that return route is
**single-shot** — the first event writes the body, closes the context, ends the
response. This feature makes it **multi-shot**: segment events append to the wire as
they arrive; an end-of-transmission event closes; the endpoint timeout bounds the whole
exchange.

Consequences: no new substrate, no Reactor dependency, fully backward compatible
(unmarked responses behave exactly as today), and language-neutral by construction —
anything that can send an envelope to a route can stream.

## 2a. Ordered delivery (D9 — added during implementation; REVISED per Eric's design)

`async.http.response` runs with 500 instances: consecutive segment events dispatch to
concurrent workers, so FIFO order is NOT guaranteed through that lane. The platform's
own ordering idiom is a route with one instance (cf. ObjectStreamIO's `instances=1`
consumer). Solution, keeping the D1 contract intact (the callee still just sends to
reply_to): a streaming endpoint is declared with **`stream: true` in rest.yaml**, and
the request **checks out a dedicated ordered reply lane for its lifetime** — a
single-instance route (`async.http.response.stream.{n}`) drawn from a **LIFO stack pool
of 500** (pinned to the `async.http.response` instance count — no configuration knob).
The lane rides on the AsyncContextHolder (direct access to the original HTTP
request/response) and returns to the stack when the request context closes — the "ready"
signal pattern of the reactive manager/worker design. One request's segments are strict
FIFO through its own lane; different requests never share a lane. An exhausted pool
rejects further streaming requests immediately with **HTTP-503 "Streaming response pool
exhausted"** (deterministic back-pressure). An idle lane costs a little memory and no
CPU. History: hash-sharded lanes (`event.stream.lanes` default 8, lane =
hash(requestId)) were the first cut — REPLACED because two concurrent requests hashing
to one lane share a serialization point (cross-request latency coupling and a routing
hazard); checkout eliminates collisions by construction. A per-request dynamic route was
also considered and rejected — register/release lifecycle + leak surface for µs-scale
rendering work. The checkout removed the `streamResponse` flag from HttpRequestEvent
(tag "12") — both reply sites resolve the lane from the holder by requestId. The
housekeeper's in-band timeout targets the holder's lane so it cannot overtake segments.
The 50-segment unpaced burst test pins per-request ordering; four parallel bursts pin
cross-request independence; exhaustion (503 + recovery), lane return, and LIFO reuse
have dedicated tests. A stream-marked response arriving on the ordinary lane still
renders (single-segment or tolerant producers), but only the declared lane guarantees
order. `stream: true` and the 503 message are rest.yaml/wire surface → part of the
cross-engine contract (Rust twin mirrors both).

## 3. Envelope protocol (internal — never on the wire)

One reserved **envelope** header, consumed by the edge exactly as `x-stream-id`/`x-ttl`
are today (never emitted to the HTTP client):

```
x-event-stream: data | eof | exception
```

- The value vocabulary is deliberately the framework's existing ObjectStream vocabulary.
- **`data`** — the body is one segment (String, byte[], or Map). The FIRST data event
  commits the HTTP head: its envelope status and headers (content-type etc.) shape the
  response; later envelopes' status/headers are ignored (debug log). Each arrival
  touches the context.
- **`eof`** — end of transmission. Optional body = trailing metadata (rendered as the
  terminal SSE event's data; ignored in chunked mode). Closes the context, ends the
  response.
- **`exception`** — in-band failure after the head is committed: SSE renders an `error`
  event then closes; chunked mode truncates and closes. Body = {status, message} map or
  a String message (rendered in the error event, mirroring today's error map shape).
- **Absence of the header = single-shot response (today's behavior, unchanged).**
- Mutual exclusivity: `x-event-stream` and `x-stream-id` must not both appear; proposed
  rule — `x-event-stream` wins, warn log (D5).

Optional companion (SSE only): `x-event-name: <name>` on a data event maps to the SSE
`event:` field — the typed-events idiom of MCP/A2A-era protocols (D2).

## 4. Wire rendering (public — standards only)

Nothing proprietary reaches the HTTP client. Streaming is expressed the way HTTP
defines it; the mode is selected by the response `Content-Type` set on the first event:

**SSE mode — `Content-Type: text/event-stream`** (first-class in v1, D2):
- String segment → `data: <text>` (multi-line text split into successive `data:` lines
  per the SSE spec); Map segment → `data: <one-line JSON>`.
- `x-event-name` present → prepend `event: <name>`.
- eof → terminal `event: done` + `data: <eof body as JSON, or {}>`. (Neutral framing —
  an application wanting OpenAI-compatible `[DONE]` sends it as its own last data
  segment before eof.)
- exception → `event: error` + `data: {"status":n,"message":"...","type":"error"}`,
  then close.
- Keep-alive: an SSE comment line (`: ping`) every `event.stream.keep.alive` seconds
  while idle (proposed default 30s, configurable; defeats idle proxy timeouts —
  the same trick as Anthropic's `ping` events) (D2).
- Standard SSE response headers set by the edge: `Cache-Control: no-cache`,
  `Connection: keep-alive` (HTTP/1.1), no `Content-Length`.

**Chunked mode — any other content type:**
- String/byte[] segments append raw (`Transfer-Encoding: chunked` on HTTP/1.1; plain
  DATA frames on HTTP/2).
- Map segments → **NDJSON** (one JSON object + `\n` per segment) (D4 — replaces the
  buffered-array behavior *only* in the new marked mode; the legacy `x-stream-id` path
  keeps its existing semantics untouched).
- eof ends the response; exception truncates + closes (status is already committed).

Reconnect/`Last-Event-ID` resume and per-event `id:` are explicitly **out of scope for
v1** (left open by the framing design; token streams industry-wide fail-and-retry
rather than resume).

## 5. Edge behavior (implementation surface)

`AsyncHttpResponse` (+ context lifecycle in `HttpRouter`):
- Multi-event dispatch: `data` → render + `holder.touch()` + keep context; `eof` →
  render terminal + `closeContext` + `end()`; `exception` → error render + close.
- Streaming state lives on the `AsyncContextHolder` (mode committed, SSE/chunked,
  content-type, first-event-seen) — no new registry.
- **Timeouts:** the endpoint timeout (rest.yaml) is the overall budget as today; each
  segment touches the holder, so the effective contract is an inter-segment idle
  timeout. Optional `x-ttl` on the FIRST event overrides the idle allowance (same key
  the stream path uses today). Housekeeping closing an idle streaming context emits the
  in-band error event first (SSE) rather than silently dropping the connection.
- **Client disconnect:** the existing connection-close handling closes the context;
  late segments address a dead correlation id and drop as no-ops — cheap, no cancel
  channel in v1 (D-noted as future work if producers ever need positive cancellation).
- **Slow client:** respect the vert.x write queue — on `writeQueueFull`, buffer up to a
  small cap (proposed 1 MB) and resume on drain; beyond the cap, treat as a failed
  client: error-close (D6). Token streams are small and paced, so this is a guard rail,
  not a hot path.
- HEAD requests never stream (no body) — first event handled as single-shot.
- Tracing: no per-segment spans (noise); the request span gains summary annotations at
  close (`stream_events=N`, `stream_bytes=M`, `stream_outcome=eof|error|timeout`).

## 6. Producer contracts

**Java function (v1):** streaming producers are interceptor-style functions
(`@EventInterceptor` — they receive the raw envelope incl. `reply_to`/`cid` and return
null), the established house pattern for system functions. A small helper is the
developer surface:

```java
var out = new EventStreamWriter(event.getReplyTo(), event.getCorrelationId());
out.first(200, "text/event-stream");        // optional head control (status, content-type)
out.write("Hello");                          // x-event-stream: data
out.write(Map.of("delta", " world"));        // structured segment
out.write("tokens", Map.of("n", 2));         // named event -> x-event-name: tokens
out.close(Map.of("usage", usage));           // x-event-stream: eof (+ trailing metadata)
// or out.fail(e);                           // x-event-stream: exception
```

Sugar over plain `po.send(...)` — no new runtime concept; its Rust (and later
python/node) twins keep the contract portable.

**rest.yaml wiring (v1 scope, D3):** streaming endpoints target a `service:` directly
(still declared in rest.yaml — the no-ad-hoc-controllers rule holds). Streaming
*through* an Event Script flow needs the http flow adapter to hand `{reply_to, cid}`
into the transaction (e.g., reserved `model.http.*` keys) so a task can construct the
writer — proposed as the immediate follow-up (v1.1) unless ruled into v1.

**Graph runs** (`POST /api/graph/{id}`) and **graph.task nodes**: follow-up on the same
mechanism once the executor forwards reply context — this is the E0+ streaming story
for `bp-agent-orchestration`, not v1.

**Polyglot wrappers (v1.1, D8):** mechanically possible with zero engine change beyond
this feature — a wrapper-side function streams by POSTing envelopes over Event-over-HTTP
addressed to `async.http.response@<engine>` with the relayed `cid` and `x-event-stream`
headers (one POST per batch; batching amortizes). Formalized wrapper-side
`EventStreamWriter` twins + relay header-preservation verification are the v1.1 work
items in the wrapper repos.

## 7. Validation plan

platform-core tests (the release gate for the feature):
- SSE end-to-end: typed + untyped segments, terminal `done` with eof body, framing
  byte-exact (multi-line data splitting, keep-alive comment).
- Chunked text and NDJSON map streaming; eof; HEAD unaffected.
- Mid-stream `exception` → in-band error event (SSE) / truncate (chunked).
- Idle timeout → error event + close; each-segment touch extends life.
- Client-disconnect: late segments are no-ops (no leak, no error).
- Backward compatibility: unmarked single-shot responses and the legacy `x-stream-id`
  path byte-identical to today (existing tests as the regression net).
- Cross-engine: an SSE conformance fixture both engines must render byte-identically
  after normalizing volatiles (the reference-signature procedure), extending the interop
  report when the Rust twin lands.
- Demo: a token-stream service in an example app (`hello.token.stream`, N segments with
  pacing) — later the E0 `llm.chat` drive plugs into exactly this endpoint shape.

## 8. D-series (rulings)

- **D1 — RATIFIED 2026-08-27:** multi-shot reply_to model; envelope marker
  `x-event-stream: data|eof|exception`; internal-envelope vs standards-only-wire
  separation.
- **D2:** SSE first-class in v1: `text/event-stream` framing, `done`/`error` terminal
  events, optional `x-event-name` → `event:`, keep-alive comment (default 30s,
  configurable). *(Recommended: yes.)*
- **D3:** v1 producer scope = `service:`-target endpoints + `EventStreamWriter`; the
  Event Script flow handoff (`model.http.*` reserved keys) as v1.1. *(Or pull the flow
  handoff into v1.)*
- **D4:** non-SSE structured segments render as NDJSON in the new mode.
- **D5:** `x-event-stream` + `x-stream-id` together = `x-event-stream` wins, warn log.
- **D6:** slow-client policy: drain-aware buffering with a 1 MB cap, then error-close.
- **D7:** Rust lock-step timing — same-day PR pair, or Java-first with the vocabulary
  pinned and the Rust twin immediately after.
- **D8:** sanction the wrapper streaming pattern (Event-over-HTTP posts to the reply
  route) as v1.1 with wrapper-side writer twins.

## 8a. Implementation status (2026-08-28, Java)

- New: `EventStreamWriter` (core.system — producer helper + the wire-contract
  constants), `EventStreamRenderer` (automation.services — SSE/chunked rendering, head
  commit, keep-alive, drain-aware cap, in-band error/timeout), `EventStreamState`
  (automation.models — per-request state + bounded pending queue).
- Response header transforms (rest.yaml `headers.response` add/keep/drop) apply to
  the streamed head via the same `filterHeaders` as single-shot responses; the SSE
  `Cache-Control: no-cache` is a set-if-absent default (explicit header or transform
  add wins). Closed on Eric's review question, pinned by a dedicated e2e.
- Modified: `AsyncHttpResponse` (early streaming branch), `AsyncContextHolder`
  (+eventStream, +streamLane — the checked-out lane rides the request context),
  `HttpRouter` (lane checkout on `stream: true` + 503 on exhaustion; release in
  closeContext, the funnel for every termination path; holder-based reply-route
  resolution), `RoutingEntry`/`RouteInfo` (`stream: true`), `AppStarter` (lane-pool
  registration + streaming-aware housekeeper timeout), `AsyncHttpClient` (route-prefix
  constant). `HttpRequestEvent` returned to its pre-feature shape (the interim
  `streamResponse` tag "12" was removed — the holder carries the lane instead).
- Config: `event.stream.keep.alive` (default 30s, 0=off). No lane-count knob (D9 revised).
- Tests: 24 (18 e2e incl. progressive-delivery timing, typed events, NDJSON, in-band
  error, pre-head error, eof-only, keep-alive pings, in-band housekeeper timeout,
  touch-extends-life pacing, 8KB×50 FIFO burst forcing the drain path, four parallel
  bursts (which caught and now pin a real drain-handler race — fixed with a per-request
  lock), D5 conflict, pool exhaustion → 503 → recovery, lane returns on completion,
  LIFO reuse, response-header-transform parity; 3 writer-contract; 3 state/cap units).
  platform-core full suite 450/450; mkdocs strict clean;
  ai-contract-provider 19/19 with the new guide in files.list.
- Docs: guides/http-streaming.md (+nav, llms.txt, event-over-http See-also), ADR-0018
  Proposed, CHANGELOG Unreleased.

## 9. Non-goals (v1)

- WebSockets (exists separately; SSE is the target idiom for these use cases).
- Reconnect/resume (`Last-Event-ID`), per-event `id:`.
- Positive cancellation channel to producers on client disconnect.
- Graph-run streaming endpoint (follow-up on this mechanism).
- Any change to the legacy `x-stream-id`/Flux relay path — it remains for
  file-download-style relaying, untouched.
