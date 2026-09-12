- [ ] **Streaming return route — cross-pod progressive rendering for sync-over-async.**
  Spec drafted 2026-09-12 (`draft-design-specs/streaming-return-route.md`; PR #365,
  squash `705d2117`): generalize the
  Redis return route from one-shot response to a per-cid segment stream — segments XADDed
  to `stream:{cid}` as the source of truth (store-first invariant kept; Pub/Sub stays a
  wake-up signal, never the payload carrier), the existing `request:{cid}` registry and
  per-pod channel reused as the pod-discovery rendezvous, eof/exception as stream entries,
  and the UI pod draining (XREAD from last-delivered id) into the shipped x-event-stream
  edge via EventStreamWriter. Contracts: progressive topic keyed by cid for ordering;
  leading dedupe option is producer seq as explicit stream entry id. Q1–Q6 open (Q2:
  Event Script composition of `stream: true` with http.flow.adapter; Q6: cap hit should
  fail in-band, not trim). Experiments E1–E4 staged (coordinator primitives → single-JVM
  e2e → cross-pod dry-run with chaos checks → LLM/graph-run token stream). Serves
  [[bp-agent-orchestration]] Q8 second half (graph-run streaming); inherits
  [[soa-transport-neutral-cid]]. Next: Eric ratifies the direction (Q1 → D1), then E1.
  <!-- id: ot-streaming-return-route | created: 2026-09-12 | last_used: 2026-09-12 | uses: 1 | tier: working | origin: 2026-09-12-021649 -->
