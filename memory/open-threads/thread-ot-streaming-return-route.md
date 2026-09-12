- [ ] **Streaming return route — cross-pod progressive rendering for sync-over-async.**
  Spec drafted 2026-09-12 (`draft-design-specs/streaming-return-route.md`; PR #365,
  squash `705d2117`): generalize the
  Redis return route from one-shot response to a per-cid segment stream — segments XADDed
  to `stream:{cid}` as the source of truth (store-first invariant kept; Pub/Sub stays a
  wake-up signal, never the payload carrier), the existing `request:{cid}` registry and
  per-pod channel reused as the pod-discovery rendezvous, eof/exception as stream entries,
  and the UI pod draining into the shipped x-event-stream edge via EventStreamWriter.
  **Direction RATIFIED 2026-09-12 — Q1–Q6 answered by Eric, folded in as D1–D6**
  (log 2026-09-12-032830): D1 store-first/pub-sub-wake-up-only; D2 interceptor facade,
  NO Event Script composition (UI app on an SSE-enabled REST endpoint); D3 **Kafka
  decoupled — Redis is the sole transport**, the producing server posts segments directly
  (new `StreamResponder` producer API; kills the ordering contract and the duplicate
  question); D4 no re-drain, TTL expiry is the cleanup; D5 no fan-out; D6 **per-seq keys
  `segment:{cid}:{seq}` replace the Redis Stream** (seq = retrieval index; MAXLEN/trim
  dissolve). One nuance awaiting Eric's confirmation: a single final drain at edge idle
  expiry (the one-shot "final read before timeout" analogue). Serves
  [[bp-agent-orchestration]] Q8 second half (graph-run streaming); inherits
  [[soa-transport-neutral-cid]]. Next: E1 (coordinator + responder primitives,
  embedded-Redis unit tests).
  <!-- id: ot-streaming-return-route | created: 2026-09-12 | last_used: 2026-09-12 | uses: 1 | tier: working | origin: 2026-09-12-021649 -->
