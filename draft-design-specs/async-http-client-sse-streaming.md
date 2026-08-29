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
self-relay e2e) — platform-core 459/459. ADR-0019 Proposed.
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
    zero meaningful cost, and trailing metadata / {status,message} keep exact Map
    types end-to-end (matters when the consumer is a function, not the edge).
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

## 7. Follow-ups staged (not this spec's scope)

- Phase 2/3 detail rounds (envelope-mode relay internals; wrapper host/writer twins;
  interop report round per `conv-telemetry-presentation-parity`).
- Event Script flow handoff (`model.http.*` reserved keys) — unchanged from the edge
  spec's v1.1 list.
- graph-run streaming (bp-agent-orchestration).
