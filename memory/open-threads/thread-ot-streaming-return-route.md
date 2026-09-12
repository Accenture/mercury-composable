- [ ] **Streaming return route — cross-pod progressive rendering for sync-over-async.**
  Spec drafted 2026-09-12 (`draft-design-specs/streaming-return-route.md`; PR #365,
  squash `705d2117`): generalize the
  Redis return route from one-shot response to a per-cid segment stream — segments XADDed
  to `stream:{cid}` as the source of truth (store-first invariant kept; Pub/Sub stays a
  wake-up signal, never the payload carrier), the existing `request:{cid}` registry and
  per-pod channel reused as the pod-discovery rendezvous, eof/exception as stream entries,
  and the UI pod draining into the shipped x-event-stream edge via EventStreamWriter.
  **Direction RATIFIED 2026-09-12 — Q1–Q6 answered by Eric, folded in as D1–D6**
  (log 2026-09-12-032830; PR #366, squash `b700b4f0`): D1 store-first/pub-sub-wake-up-only; D2 interceptor facade,
  NO Event Script composition (UI app on an SSE-enabled REST endpoint); D3 **Kafka
  decoupled — Redis is the sole transport**, the producing server posts segments directly
  (new `StreamResponder` producer API; kills the ordering contract and the duplicate
  question); D4 no re-drain, TTL expiry is the cleanup; D5 no fan-out; D6 **per-seq keys
  `segment:{cid}:{seq}` replace the Redis Stream** (seq = retrieval index; MAXLEN/trim
  dissolve). **Refined to D7 same day** (log 2026-09-12-044817) after Eric articulated
  the driving use cases (event notification: SEVERAL backends post to one SSE session,
  unordered, either side closes; AI chat: one sequential source, strict order): **no
  sequence number at all — a Redis List per cid** (`RPUSH queue:{cid}` store-first,
  destructive LPOP drains serialized per cid, any producer may post the terminal entry,
  session-scale TTL 1800s default). Per-seq keys would collide across uncoordinated
  producers; a single sequential producer gets ordering free from Redis per-connection
  command order. Producer contract = post-in-order, no header stamping, no per-cid
  state. **D8 (same day): the one-shot path adopts the list mechanism** — a one-shot
  response is the degenerate stream (first entry terminal); `response:{cid}` retires,
  deliver() ≡ terminal post, one drain path, final-read == final-drain; requires
  PendingRequests.complete-in-place (early-arrival path survives destructive pops;
  removal via awaitResponse finally + abort, exhaustive); rolling-upgrade key-shape
  migration note for release notes; acceptance = existing one-shot suite unchanged.
  One nuance awaiting Eric's confirmation: a single final drain at edge idle expiry
  (the one-shot "final read before timeout" analogue). Serves
  [[bp-agent-orchestration]] Q8 second half (graph-run streaming); inherits
  [[soa-transport-neutral-cid]]. Next: E1 (coordinator + responder primitives,
  embedded-Redis unit tests).
  <!-- id: ot-streaming-return-route | created: 2026-09-12 | last_used: 2026-09-12 | uses: 1 | tier: working | origin: 2026-09-12-021649 -->
