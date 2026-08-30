# SSE consumption in AsyncHttpClient + Event-over-HTTP peer streaming — design spec

**Status:** APPROVED — option 2 ratified by Eric 2026-08-29 (SSE response on the same
call; WebSocket and multi-POST alternatives rejected — see §2); **D1–D9 approved as
recommended 2026-08-29, with D5 REVISED to Eric's hybrid framing** (envelope frames only
where envelope semantics matter; raw frames on the hot path — see D5). Phasing
directive: **start with the AsyncHttpClient enhancement (Phase 1)**.
**Phase 1 IMPLEMENTED (Java) 2026-08-29:** the SSE-candidate branch in AsyncHttpClient
(responseTimeout exempted for candidates; per-read idle timer with in-band 408 "Timeout
for N seconds"; lock-serialized relay per the drain-handler lesson), incremental
byte-level SSE parser (UTF-8-safe line split, comment/id/retry suppression, multi-line
data join), buffered fallback for non-SSE responses on the candidate path, and the
self-relay demo endpoint (/api/hello/relay in the test app). Gates: 9 new tests
(mapping, multi-field frames, 50-event FIFO burst, idle 408, comment-reset,
mid-stream disconnect, buffered fallback, no-Accept backward-compat pin, progressive
self-relay e2e) — platform-core 459/459. ADR-0019 Proposed (Accepted at the Phase-1
merge; Rust twin same day).
**Phase 2 P2-1…P2-6 RATIFIED by Eric and IMPLEMENTED (Java) 2026-08-29:** the
envelope-mode relay per §7 — EventApiService streaming branch with dynamic lane
binding, envelope-mode rendering in the edge renderer, the caller-side relay in
EventEmitter (accept event-header opt-in), the envelope submode in AsyncHttpClient
(dialect decode, strict-head and truncation guards, buffered-fallback decode), and
the 406/503 refusals. One contract alignment found during implementation (completed
by Eric's review): every in-band exception body now carries the standard error
key-values {"type": "error", "status": n, "message": text} — the edge housekeeper's
408 abort was String-bodied, and EventStreamWriter.fail() and the client's in-band
failures were missing the "type" key (wire-invisible at the local edge, which
re-wraps; shape-visible to envelope-mode callers and reply-route consumers). Gates: 14 new tests
(EventOverHttpStreamTest — dialect round-trips incl. every escape-hatch trigger,
byte-identical single-shot fallback, both 406 paths, 503 pool exhaustion, REST-error
unwrap, misbehaving-peer conformance guards, progressive engine⇄engine e2e out the
edge) — platform-core 473/473. Next: the Rust twin, then Phase 3 (wrapper twins).
**Serves:** `bp-agent-orchestration` (an `llm.chat` node consuming provider token streams)
and `bp-polyglot-functions` (wrapper functions streaming back to the engine) — the two
remaining gaps after [[thread-http-response-streaming]] closed the engine→HTTP-edge leg.
**Engine parity:** AsyncHttpClient and Event-over-HTTP are engine surface → Java + Rust
lock-step with engine-identical vocabulary and messages (Java is the reference); the
wrapper halves land in mercury-python / mercury-nodejs with their own twins.

---

## 1. Problem

Progressive rendering now exists on the HTTP **service** side (both engines): a callee
streams `x-event-stream: data | eof | exception` envelopes to the caller's reply lane and
the edge renders SSE/chunked progressively. Two consumption gaps remain:

1. **Engine as SSE client.** `async.http.request` aggregates the whole response before
   replying (`responseSingle` on reactor-netty; body collect on hyper). An engine
   function calling an LLM provider's SSE API (OpenAI/Anthropic/Gemini-compatible —
   `text/event-stream` responses) receives everything buffered at completion — no
   token-by-token relay is possible.
2. **Wrapper functions cannot stream to the engine.** Event-over-HTTP is drop-n-forget
   (`x-async: true` → 202) or request-response (RPC — the reply rides the single HTTP
   response body). A python/node function has no channel for a segment sequence.

Both gaps close with ONE mechanism because the Event-over-HTTP relay already flows
through AsyncHttpClient (`EventEmitter.asyncRequest(event, timeout, headers, endpoint,
rpc)` wraps the envelope as a POST via `async.http.request`): teach the client to consume
a `text/event-stream` response progressively, and the wrapper's `/api/event` host becomes
just another SSE producer.

## 2. Decision record (option analysis ratified 2026-08-29)

- **CHOSEN — SSE response on the same call.** One request → one ordered stream (TCP
  ordering is FIFO for free); the terminal signal is in-band (the `eof`/`exception`
  envelope — the protocol both engines already implement); no new inbound surface on the
  engine (segments ride the response of a request the engine itself made); the most
  middleware-survivable wire (gateways handle SSE because LLM traffic forced them to);
  and the same client capability is needed anyway for direct LLM-provider calls.
- **REJECTED — on-demand WebSocket engine⇄wrapper.** Breaks the wrapper scope fence
  (D0–D8: codec + /api/event host + thin client — no connection-lifecycle machinery);
  four codebases grow a WS surface + conformance vectors; historically gateway-hostile;
  a standing engine⇄wrapper connection drifts toward the presence/mesh architecture the
  framework deliberately keeps opt-in (`kafka-mesh-opt-in`).
- **REJECTED — per-segment POSTs back to the reply lane** (the earlier v1.1 sketch).
  Zero new machinery, but FIFO forces the wrapper to serialize posts (per-segment
  latency = one network RTT — fatal for token streams) and it requires letting remote
  peers address reply lanes (a new inbound trust surface the chosen option avoids).
- **REJECTED — gRPC/HTTP-2 push** (foreign dependency stack), **long-polling** (chatty,
  stateful), **the legacy `x-stream-id` relay** (Reactor/JVM-shaped; absent from the
  Rust port; already rejected in the D1 round of the edge feature).

## 3. The model

```
consumer function ──send──▶ async.http.request ──POST──▶ upstream (SSE server)
        │ reply_to + cid                                     │ text/event-stream
        ▼                                                    ▼
reply route (e.g. the HTTP edge's reply lane) ◀── x-event-stream envelopes,
                                                   one per SSE event, then eof
```

The client becomes a **streaming producer** toward the caller's `reply_to` — exactly the
producer contract the edge already consumes. Composition falls out with zero new code:
a `stream: true` endpoint whose function forwards its own `reply_to`/`cid` into the
`async.http.request` event turns the engine into an **SSE-to-SSE relay by
configuration** (upstream tokens → client → reply lane → SSE out the edge).

Two consumption modes, selected by context:

- **Raw mode** (Phase 1, user-facing): the upstream is any SSE API. Each SSE event maps
  to one `x-event-stream: data` envelope (body = the event's data text; `event:` name →
  `x-event-name`). For LLM providers and arbitrary SSE sources.
- **Envelope mode** (Phase 2, the Event-over-HTTP relay): the upstream is a peer's
  `/api/event`. HYBRID framing per D5: envelope frames (`event: envelope` +
  base64 MsgPack) for the head, the terminals, and non-text segments; raw frames for
  text segments. The client decodes/constructs and forwards each event to the original
  `reply_to` with the original correlation id. Mode selection is structural: the relay
  path (internal `x-event-api` context) uses envelope mode; user calls to
  `async.http.request` use raw mode.

## 4. Proposed decisions (D-series — for ruling)

- **D1 — Activation is explicit and standard: the caller's Accept header.** Progressive
  mode activates only when (a) the request declared `Accept: text/event-stream` AND
  (b) the response arrives with `Content-Type: text/event-stream` AND (c) the request
  event carries a `reply_to`. Anything else keeps today's buffered single-shot behavior
  — fully backward compatible (existing callers that hit SSE endpoints and buffer them
  keep working; an RPC caller who accidentally opts in would consume only the first
  segment, hence the explicit Accept requirement). Recommended over a proprietary
  opt-in header: "you asked for SSE, you receive SSE semantics" — and it follows the
  house lesson that Accept must always be declared explicitly.
- **D2 — Raw-mode event mapping.** One upstream SSE event → one `data` envelope:
  multi-line `data:` fields joined with `\n` (SSE spec), `event:` → `x-event-name`,
  comment lines (`: ping`) and `id:`/`retry:` fields consumed by the client, never
  forwarded. The FIRST forwarded envelope carries head control: upstream HTTP status +
  `content-type: text/event-stream` — so a relay to the edge opens SSE mode there
  automatically. Stream end → `eof` envelope (empty body). Mid-stream transport error →
  `exception` envelope `{status: 500, message}` — in-band, matching the edge contract.
  A non-2xx upstream head with an SSE body still streams (status rides the first
  envelope); a non-2xx non-SSE head is today's single-shot error reply.
- **D3 — No payload interpretation in raw mode.** The client does NOT parse provider
  conventions (OpenAI's `data: [DONE]`, JSON deltas, etc.) — data text is forwarded
  verbatim and the consumer (an `llm.chat`-style function or the end client) owns
  provider semantics. Keeps the client vendor-neutral (the AI-companion non-goal:
  never locked to one LLM vendor).
- **D4 — Idle allowance replaces total timeout for streams.** Today the request's
  `x-ttl` bounds the whole exchange. In progressive mode it becomes the per-read idle
  allowance (edge parity: each arriving event resets it); on idle expiry the client
  forwards an in-band `exception` envelope `{status: 408, message: "Timeout for N
  seconds"}` (engine-identical message) and aborts the upstream connection. Upstream
  keep-alive comments DO reset the client's idle timer (they prove upstream liveness —
  LLM providers ping during long tool-use pauses) but are never forwarded; a relay
  flow should therefore set the producer-side idle override (`x-ttl` on the first
  event, or a generous endpoint timeout) to cover quiet-but-alive upstreams
  end-to-end. *(This asymmetry — upstream pings feed the client's timer but nothing
  reaches the edge's timer — is the one nuance to confirm.)*
- **D5 (REVISED — Eric's hybrid, 2026-08-29) — Envelope-mode framing is a HYBRID:
  envelope frames only where envelope semantics matter; raw frames on the hot path.**
  Rationale: pure envelope-per-frame repeats the whole envelope structure (cid, route,
  headers, marker) plus base64 on EVERY token — ~200+ wire bytes for a 5-byte token;
  raw frames cut the hot path to `data: <text>` (~90% reduction). One request = one
  stream = one cid, so per-frame addressing carries no information.
  **Eric's formulation (2026-08-29): control plane vs data plane — all control signals
  ride as key-value headers in envelope frames (the first envelope carries the head:
  status, content-type, idle allowance; the terminals ARE control signals — end of
  transmission and in-band failure); subsequent blocks are tokens (raw frames).**
  The dialect (self-describing, no sniffing):
  - Reserved SSE event name **`event: envelope`** marks a frame whose `data:` is one
    base64-encoded MsgPack envelope (the one codec; golden vectors still the gate).
  - **First frame** = envelope frame (head control: status, content-type, x-ttl).
  - **Terminal frames** (`eof` / `exception`) = envelope frames — one per stream, so
    zero meaningful cost, and trailing metadata / the standard error body
    {"type": "error", "status": n, "message": text} keep exact Map types end-to-end
    (matters when the consumer is a function, not the edge).
  - **Text segments** = raw frames: `data: <text>`, optional user `event: <name>` →
    `x-event-name` (SSE's native field; the same grammar the edge renders).
  - **Adaptive escape hatch**: the writer emits an envelope frame for any segment that
    cannot round-trip raw — Map or byte bodies, text containing `\r` (SSE normalizes
    line endings), or a user event name equal to the reserved word. Deterministic,
    checked per segment, never lossy.
  Keep-alive comments allowed, ignored. The raw dialect adds one small frame-mapping
  conformance fixture, mirrored from the edge rendering both engines already pin.
- **D6 — Envelope-mode capability negotiation (version skew).** The engine relay
  advertises `Accept: text/event-stream` on `/api/event` calls once it can consume
  progressively. A peer streams ONLY when the request accepts SSE and the target
  function actually streams; a streaming function invoked by a non-accepting (older)
  caller receives an explicit error envelope ("Streaming function requires a caller
  that accepts text/event-stream" — engine-identical wording TBD) rather than a
  silently buffered or truncated reply. Old wrappers ignore the Accept header and
  behave exactly as today.
- **D7 — Correlation and tracing across the hop.** The relayed segments are forwarded
  to the ORIGINAL `reply_to` with the ORIGINAL `cid` (the client rewrites, exactly as
  the edge's lane consumes them); trace context propagates on the outbound POST as
  today (X-Trace-Id + traceparent); the client's forwarding leg is the visible span.
- **D8 — Ordering guarantee.** One streaming call is consumed by ONE client worker
  reading one HTTP response sequentially → per-stream FIFO into the reply route by
  construction (no lane needed on the client side; the reply route's own discipline —
  e.g. the edge lane — preserves order downstream).
- **D9 — Phasing.** Phase 1 (START, this repo then Rust): raw-mode SSE consumption in
  AsyncHttpClient + the self-relay proof (see §6). Phase 2 (both engines): envelope
  mode on the Event-over-HTTP relay + `/api/event` SSE production on the ENGINE side
  (engine⇄engine streaming works before wrappers move). Phase 3 (wrapper repos):
  `/api/event` SSE production + `EventStreamWriter` twins in mercury-python and
  mercury-nodejs, conformance-tested against the engines. Java-first with vocabulary
  pinned; Rust immediately after each phase (D7 precedent).

## 5. Implementation sketch (Phase 1)

- **Java** (`AsyncHttpClient`): branch after response head — if progressive conditions
  hold (D1), switch from `responseSingle` to `response((head, content) -> ...)` /
  `responseContent()` (reactor-netty's streaming `ByteBufFlux`), feed an incremental
  SSE frame parser (buffer split on `\n\n`, tolerate `\r\n`, partial-frame carry),
  and emit envelopes to `reply_to` via `po.send` as frames complete; idle timer per
  D4; cancellation on downstream failure (reply route gone → abort upstream).
- **Rust** (`automation/http_client.rs`): same branch; hyper's `Incoming` body is
  already frame-streaming — feed the same incremental parser (the test-side
  de-chunking reader from `tests/event_stream.rs` is the shape, production-hardened).
- Reuse the pinned SSE vocabulary end-to-end; no new config keys in Phase 1.

## 6. Test plan (Phase 1)

- **The self-relay e2e (flagship):** one app, no external dependency — a `stream: true`
  endpoint whose function forwards its own `reply_to`/`cid` into an
  `async.http.request` aimed at the app's OWN SSE demo endpoint. Proves consumption,
  mapping, ordering, and edge re-rendering in a single process (progressive timing
  asserted with the timed-line reader).
- Raw-mode unit/e2e set against a mock SSE upstream: event mapping (data/multi-line/
  named), comment and id/retry suppression, head-status propagation, eof on clean end,
  in-band 408 on idle stall (with comment-reset case), in-band exception on mid-stream
  disconnect, non-SSE response unchanged (backward-compat pin), Accept-not-declared →
  buffered (backward-compat pin), burst FIFO through the relay.
- Mutation-proof the idle timer and the terminal mapping (kill the fix, watch it fail).

## 7. Phase 2 design — envelope-mode relay internals (RATIFIED by Eric 2026-08-29; implemented same day)

The engine⇄engine leg: a streaming function on engine B, invoked over Event-over-HTTP
from engine A, streams its segments back to the original caller's reply route on A.
Zero new config keys; zero new endpoints; the wire is the D5 hybrid dialect.

### P2-1 — Trigger: the caller's `accept` EVENT header on the callback path

Today's `send()` over an event-over-http mapping already has a **callback mode**
(`reply_to` present → RPC + response delivered to the callback route). Streaming extends
exactly that path: when the outbound event carries `reply_to` AND the event header
`accept: text/event-stream`, the relay switches to streaming-capable mode — the POST
advertises `Accept: text/event-stream` (D6) and the caller's `reply_to`/`cid` pass
through to `async.http.request` (the Phase-1 relay pattern) with an internal
envelope-mode marker. Without the accept header, the callback path is byte-identical to
today. The RPC path (`po.request` — a Future completes once, it cannot stream) and the
drop-n-forget path (`x-async`) never stream and never advertise; a streaming target
invoked via RPC gets the P2-4 refusal. D1 parity: explicit Accept opt-in, now at the
event level — a stray user `accept` header is harmless (single-shot targets fall back
byte-identically).

### P2-2 — Server realization: dynamic reply lane + envelope mode in the edge renderer

`/api/event` stays a normal (non-`stream: true`) endpoint — plain RPC traffic keeps its
unbounded concurrency. When the POST accepts SSE (and is not `x-async`), EventApiService,
after the existing route/private validation:

1. binds an envelope-mode stream context: reuse the holder's lane if the endpoint was
   (unusually) declared `stream: true`, else check out a lane from the SAME 500-lane pool
   (dry pool → single-shot error, the pinned "Streaming response pool exhausted" 503);
   the bind records the requester's wire format (COMPACT/STANDARD mirroring, as today)
   and an atomic release guard closes the checkout race with client disconnect;
2. rewrites the relayed request's `reply_to` to the lane and its `cid` to the edge
   requestId (exact parity: today's RPC inbox rewrites the cid the same way), then
   dispatches with `po.send` — no RPC future; the holder's idle timer (x-ttl from the
   POST, per P2-6) is the guard.

The lane's handler (AsyncHttpResponse) sees the envelope-mode context and renders the
**wire dialect** instead of user-facing SSE:

- **first envelope (whatever it is — EventStreamWriter puts head control on the first
  outgoing event)** → `event: envelope` + `data: <base64(MsgPack(envelope))>` — packed
  verbatim in the mirrored wire format after clearing `to`/`reply_to` (server-internal
  route names never leak; the client rewrites addressing anyway per D7). Outer HTTP head:
  status mirrors the envelope status, content-type `text/event-stream`, Cache-Control
  no-cache; keep-alive pings, back-pressure queue and slow-client guard inherited.
- **subsequent data envelopes** → raw frame (`data: <text>`, `event: <x-event-name>`)
  when losslessly raw-able: String body, no `\r`, no custom headers, name ≠ `envelope`;
  otherwise the envelope-frame escape hatch (Map/bytes segments keep exact types).
- **terminals (`eof`/`exception`)** → one envelope frame, then clean HTTP end. The
  engine⇄engine wire carries NO cosmetic `event: done` / `event: error` frames — those
  are the edge's human-facing dialect; the decoded terminal envelope is the signal
  (P2-5). A pre-head exception also rides SSE-uniform (head + exception frame + end) so
  the caller always receives the exact error envelope.
- **a single-shot reply (no x-event-stream marker)** → NOT SSE: packed-envelope
  octet-stream response, byte-identical to today's RPC wire (P2-3) — so a streaming-
  capable call to a non-streaming function is indistinguishable from today.

### P2-3 — Compatibility matrix (all paths explicit)

| Caller | Target | Result |
| --- | --- | --- |
| new, accept declared | streams | progressive envelope-mode relay (NEW) |
| new, accept declared | single-shot | byte-identical to today (buffered fallback decode) |
| new, no accept / RPC / async | single-shot | byte-identical to today |
| any non-accepting caller (incl. old engines) | streams | explicit refusal per P2-4 |
| new, accept declared | old peer (no SSE production) | non-SSE response → buffered fallback, today's semantics |

### P2-4 — Refusal wording (D6 pinned, engine-identical)

A streaming target invoked by a non-accepting caller: the first segment completes the
server's RPC inbox carrying `x-event-stream` → EventApiService answers **406**
`"Streaming function requires a caller that accepts text/event-stream"` instead of
relaying a truncated first segment. Orphan follow-up segments die on the closed inbox.

### P2-5 — Client realization (AsyncHttpClient envelope submode)

The Phase-1 SSE branch gains the envelope dialect when the request event carries the
internal relay marker (`x-event-api: stream`, stripped before wire):

- `event: envelope` frame → base64 → MsgPack decode → forward to the original `reply_to`
  with the original `cid` (D7 rewrite); a decoded terminal stops forwarding, cancels the
  idle timer, and discards any trailing frames (tolerance for wrapper dialects).
- raw frame → data envelope (`x-event-stream: data`, body = text, `event:` name →
  `x-event-name`) — the inverse of the server's raw framing.
- **strict head rule:** the first frame MUST be an envelope frame; a raw first frame is
  an in-band exception 500 "Invalid event stream - missing envelope head" (conformance
  guard for wrapper implementations).
- clean HTTP end WITHOUT a decoded terminal → in-band exception 500 "Event stream ended
  without eof" (catches truncating middleware); transport eof after a decoded terminal
  is suppressed (no double-terminal).
- buffered (non-SSE) fallback → decode the packed envelope and deliver to the callback
  with today's rewrite — including today's tolerant wrap of a non-envelope error body.
- Phase-1 in-band 408/500 idle/transport handling unchanged.

### P2-6 — TTL semantics across the hop (D4 extended)

The outbound event's `x-ttl` header (default: the existing 60s relay allowance) becomes
the idle allowance on BOTH hops: it rides the POST's `x-ttl` (server holder idle timer —
housekeeper aborts with the in-band 408 exception frame) and sets the client's per-read
idle allowance. The target can extend it mid-relationship via `first(status, ct, ttl)` —
the head frame's x-ttl reaches both the server holder and (decoded) the caller's edge,
exactly as a local stream would.

### Phase 2 test plan

Full-chain e2e in one process (edge `stream: true` endpoint → function relays via
event-over-http to the app's OWN `/api/event` → local streaming target), progressive
timing asserted; single-shot-over-capable-path byte-compare vs classic RPC; 406 refusal
pin; Map/bytes/`\r`/reserved-name escape-hatch round-trips (exact types); eof trailing
metadata round-trip; mid-stream `fail()` propagation; server idle 408 through the chain;
pool-dry 503; x-async and RPC pins unchanged; trailing-frame discard tolerance.

## 8. Phase 3 design — wrapper twins (RATIFIED by Eric 2026-08-30, incl. the reply_to revision)

The python/node language packs gain both halves of the dialect: their `/api/event`
host answers streaming-capable calls with the envelope-mode SSE response (a wrapper
FUNCTION streams to an engine caller), and their client consumes SSE (a wrapper
function calls a remote streaming function). Everything stays inside the wrapper
scope fence — codec + host + thin client; no orchestration.

### W1 — reply_to in the primitive bus; interceptor handlers; engine-identical writer

**Eric's revision (2026-08-30), replacing the contextvar proposal:** the wrappers'
primitive event bus allows the **reply_to mechanism** — envelope-addressed delivery
to a LOCAL route — so the producer paradigm is identical across all four runtimes:
*the caller provides a reply address; the callee streams events to it until a
terminal signal.* It is not orchestration — simple routing to a local function,
exactly what the engines' in-memory bus does. reply_to never routes across the wire
(cross-wire replies ride the SSE response, as on the engines). Consequences:

- `@preload` gains the engines' **interceptor flavor**: an envelope-receiving
  handler whose return value is not auto-replied — manual sends via reply_to.
  Streaming producers and relay functions are interceptors, as on the engines.
- `EventStreamWriter.from_request(event)` — the engines' exact producer API
  (reads reply_to + cid from the incoming envelope; head control on the first
  outgoing event; write / write_named / close(metadata) / fail; writes after
  close dropped; the standard error triple on fail).
- Per-request **inbox sinks** (generated local route names backed by queues, the
  engines' AsyncInbox idea) give interceptor dispatch its reply addressing; the
  single-process host needs no lane pool — a per-request sink IS the lane.
- This unlocks the **relay composition** in wrappers: a streaming function
  forwards its own reply_to into a call against a remote streaming peer, and
  segments flow engine → wrapper → peer → back with zero buffering.

### W2 — host dispatch (the EventApiService half)

For a capable call (`Accept: text/event-stream`, not x-async) to an interceptor
target, the host dispatches with reply_to = a per-request sink and classifies the
first envelope exactly like the engines: unmarked → the classic single-shot
response, byte-identical; marked → the envelope-mode SSE dialect (same frame
rules; terminals then clean end; x-ttl ms as per-segment idle with the in-band
408 triple; `event.stream.keep.alive` pings, same config key, default 30s).
A NON-capable call whose first reply carries `x-event-stream` answers the pinned
`406 Streaming function requires a caller that accepts text/event-stream` — the
engines' exact detection mechanism. Non-interceptor targets keep today's
future-based dispatch untouched. An uncaught handler exception after the head
becomes an in-band exception frame (the host awaits the handler — a small
wrapper-side improvement over engine interceptors).

### W3 — client surface (the AsyncHttpClient half)

- `po.stream(route, body, ...)` → an async iterator (`for await` on node) yielding
  the SAME decoded envelopes an engine reply route receives — data segments then
  the terminal; a non-streaming target yields its one classic reply (graceful
  degradation). Local routes are supported through the same sink mechanics (a
  dividend of W1's reply_to ruling — the earlier "local = error" proposal is
  superseded).
- `po.stream_to(route, body, reply_to=..., cid=...)` → the raw relay form for
  composition: decoded envelopes forward verbatim to the named LOCAL route with
  the given correlation id; awaits the terminal.
- Conformance guards identical to the engines: missing envelope head / malformed
  frame / transport end without a decoded terminal synthesize the pinned in-band
  exception envelopes; per-read idle = x-ttl with the 408 triple; frames after a
  decoded terminal are discarded.

### W3a — business correlation-id parity (added at the interop verification, 2026-08-30)

The continuity check surfaced the missing outbound half in the wrappers: they
injected the `my_cid` tag as the read-only `my_correlation_id` header view at
HTTP delivery but never re-stamped it on outbound calls, and local bus
deliveries skipped the injection. Completed to full engine parity
(`PostOffice.touch` / WorkerHandler): the trace context carries the business
correlation-id (`get_trace()` / `trace_context(...)`; node `getTrace()` /
`runWithTrace`, now exported), outbound client envelopes fill-stamp the
`my_cid` tag from context, and local deliveries inject the header view exactly
like the HTTP host. Pinned by two tests per wrapper; live-proven in the interop
report's business-correlation-id section (both engine edges → python echo;
wrapper ⇄ wrapper both directions).

### W7 — wrapper span lineage (RATIFIED direction 2026-08-30; implemented same day)

Eric's framing: for the AI SDLC, OpenTelemetry lineage is the observability
substrate - *user → agent → MCP → tools*, "the lineage documents the graphs."
Wrapper functions are exactly where the agent-orchestration adapters (LLM, MCP,
tools) will live, so a span-less wrapper boundary would break the trace tree at
its most important nodes. The wrappers therefore implement the engines' full
span model rather than trace-id passthrough:

- Every traced execution mints a 16-hex (W3C-shaped) span; the caller's span
  (from the inbound envelope) is its parent. The trace context exposes
  `span_id`/`parent_span_id` (python; camelCase on node), and
  `trace_context()`/`runWithTrace()` accept a caller span so a batch edge can
  parent onto an external OpenTelemetry span.
- Outbound client events and EventStreamWriter segments carry the CURRENT
  span (PostOffice.touch parity), which also lights the W3C `traceparent`
  header the wrapper clients already stamped conditionally.
- Non-RPC executions emit the engines' distributed-trace dataset record
  (`{"trace": {...}, "annotations": {...}}`, Java-reference key set incl.
  origin/start/exec_time/status/span_id/parent_span_id) on a
  `distributed.tracing` logger; the wrapper log pipeline renders structured
  messages as real JSON objects in json/compact mode and compact JSON in text
  mode. The stdout log stream is the field-grade sink: log-ingest agents
  (e.g. a Dynatrace stdout ingest agent) forward it to dashboards - per Eric,
  often a better field solution than an OTLP forwarder; OTLP export from
  wrappers stays out of the minimalist fence.
- RPC round-trips emit no dataset (engine WorkerHandler parity): the wrapper
  clients stamp the engines' `rpc` envelope tag on `request()` calls and the
  bus suppresses the record for reply-future or rpc-tagged deliveries.
- Live-proven three ways under single traces: Java relay span → python span
  (parent = relay) → Java lane spans (parent = python span); python caller's
  external span → engine event.api.auth/service parents; node execution
  parented on a python caller's span. Interop report carries the evidence.
- Known nuance: raw SSE token frames carry no envelope metadata, so their
  engine-side delivery spans join the trace unparented (head/terminal parent
  correctly via envelope frames) - see the §9 follow-up.
- Sender attribution (Eric's review find): the hosts fill `from` with
  `event.api.service` for an anonymous wire caller (the Java EventApiService's
  touch fill), the trace context carries the executing route so onward calls
  and stream segments attribute themselves, and bare local callers fall back
  to the engines' `unknown`.
- Application log context (Eric's follow-on gap): the app-log-context twin -
  `app.log.context` on by default with a packaged `default-log-context.yaml`
  (the engines' classpath-resource twin, not a code constant) carrying the
  engines' template
  (cid/traceId/tracePath/spanId/parentSpanId/service/timestamp), replaceable
  via `resources/app-log-context.yaml` ($tokens + `${ENV:default}` constants,
  invalid tokens fail fast), a `context` block on json/compact log lines
  inside traced requests only, `update_context`/`updateContext` developer API
  with reserved-key guard - so app logs and trace records correlate end to
  end. Long-lived bus workers detach from the creating caller's context
  (a leak the sample drives exposed).

### W4–W6

- **W4** — a sync-bridge `stream_sync` (blocking iterator for plain-`def` python
  handlers) is DEFERRED to field demand; the async form covers the LLM-relay case.
- **W5** — config: the same `event.stream.keep.alive` key (30s default, 0 off);
  x-ttl stays milliseconds on the wire.
- **W6** — conformance: wrapper suites against conforming and misbehaving mock
  peers, then a live interop round against BOTH engines at ship (wrapper→engine
  and engine→wrapper token streams), recorded in the interop report per
  `conv-telemetry-presentation-parity`.

Rollout: mercury-python first (the reference wrapper), mercury-nodejs as its twin,
then the live interop round and the wrapper docs chapters.

## 9. Follow-ups staged (not this spec's scope)

- Event Script flow handoff (`model.http.*` reserved keys) — unchanged from the edge
  spec's v1.1 list.
- graph-run streaming (bp-agent-orchestration).
- Raw-frame lane parenting (optional polish, Java+Rust lock-step): the engines'
  SSE-consuming client could remember the stream head's span id and stamp it on
  the envelopes it rebuilds from RAW token frames, so per-token delivery spans
  parent onto the producer like the head/terminal already do.
