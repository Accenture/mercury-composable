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
  dissolve). **Refined to D7 same day** (log 2026-09-12-044817; D7 merged via PR #367, squash
  `04603fdd` — its title says "D7 and D8" but a merge race caught the D7-only branch;
  D8 + the compat ruling landed via PR #368, squash `69865966`) after Eric articulated
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
  removal via awaitResponse finally + abort, exhaustive); no backward-compat issue —
  near-real-time state, nothing persists across versions (Eric's ruling); acceptance =
  existing one-shot suite unchanged.
  One nuance awaiting Eric's confirmation: a single final drain at edge idle expiry
  (the one-shot "final read before timeout" analogue) — E1 implements and DEMONSTRATES
  it (`finalDrainAtIdleExpiryRecoversADroppedClose`) for his verdict. **E1 SHIPPED
  2026-09-12** (merged via PR #369, squash `adc62997`, title clean): store re-platformed to `queue:{cid}` (atomic RPUSH+EXPIRE via Lua),
  `StreamResponder`/`StreamSegment`/`PendingStreams` shipped, complete-in-place landed
  (plus one E1-found race fix: the timeout path consults the future after an empty
  final drain, spec §4.7 note); D8 gate PASSED — module 73/73 (was 46), demo flows 4/4
  against the unified module; guide/config-reference/README updated. **E2 SHIPPED
  2026-09-12** (merged via PR #370, squash `4bd5bd11`, title clean): `StreamBridge` (facade lifecycle — SSE head + watchdog share one idle number;
  expiry = single final drain then in-band 408 + closeStream; capacity → real 503) +
  `EventStreamSink`; broker-free e2e `StreamingRestE2eTest` proves both use cases behind
  real `stream: true` endpoints through the shipped SSE consumer — chat exact-order,
  notification multi-producer + backend close, idle expiry BOTH ways (lost-close
  recovered by the final drain — the D4 nuance observable end-to-end — and empty → 408 +
  orphan stop); surefire reuseForks=false (two AutoStart-booting classes); module 77/77,
  demo 4/4. **E3 SHIPPED 2026-09-12** (merged via PR #371, squash `b1e57537`, title clean —
  3 commits trimmed: dry-run `1c247872` + guide contract fix `7065e5d0` +
  contract-provider closure registration `3b3b0326`): live two-JVM dry-run against
  redis-standalone — demo gained
  `stream-ui`/`stream-producer` profiles (broker-free) + a `lost` chaos mode; five
  scenarios green first run (ordered chat cross-pod; lost notification healed; lost
  close recovered by the final drain at idle + 1 ms; producer kill -9 → in-band 408;
  UI pod kill -9 → TTL-bounded orphan stop — spec §6 row tightened to name the timing);
  permanent record docs/test-reports/streaming-return-route-cross-pod.md; no mechanism
  defects. **E4 SHIPPED 2026-09-12 — E-SERIES COMPLETE** (colon fix merged via PR #372, squash
  `b9d20d52`; E4 merged via PR #373, squash `04224cb9`, titles clean; follow-up on
  Eric's ask — the emulated-LLM round-trip as a committed CI test, keyless/offline,
  merged via PR #374, squash `e08f28ca`): REAL Gemini tokens
  cross-pod — pod B pulls the
  provider's SSE stream through the shipped SSE consumer, a ~40-line bridge
  (`demo.llm.bridge`, instances=1 BY CONTRACT — a multi-instance reply consumer posts out
  of order and a batch behind the terminal is silently discarded) forwards into the
  rendezvous; terminal `done` carries provider usage (report scenario 6). Round findings:
  platform-core HTTP client percent-encoded `:` in path segments (RFC 3986 pchar; 404s
  Google `:verb` APIs — fixed + pinned, own PR); thinking-model budget gotcha; stale
  gemini model default → `gemini-flash-latest`. Serves [[bp-agent-orchestration]] Q8
  second half — the TRANSPORT is delivered; the in-graph-run drive is that thread's next
  experiment. Inherits [[soa-transport-neutral-cid]]. Remaining on this thread: Eric's
  final-drain confirmation (demonstrated at unit/single-JVM/cross-pod), then the thread
  closes with the next release.
  <!-- id: ot-streaming-return-route | created: 2026-09-12 | last_used: 2026-09-12 | uses: 1 | tier: working | origin: 2026-09-12-021649 -->
