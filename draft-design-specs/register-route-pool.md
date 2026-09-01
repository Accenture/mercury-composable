# registerRoutePool / releaseRoutePool — platform API for numbered singleton route pools — design spec

**Status:** APPROVED — direction ratified by Eric 2026-08-30 (concept:
`~/Desktop/register-streams.md`, same-day review round corrected the loose ends; **D1
naming and D2 private-only explicitly ratified**; D3–D9 spec'd per the accepted
review; **D10 ruled same day: KEEP `async.http.response.stream.{n}`**; D7 refined per
Eric: `ReentrantLock`, not `synchronized`, for virtual-thread friendliness on Java 21
and codebase consistency). **Same-day follow-up by Eric after implementation:**
`registerPrivateStream` removed — `registerStream` is now private-only (a stream
function's use case is always a private route; `ObjectStreamIO` updated), extending the
pools' private-only philosophy to stream functions; the unused
`ASYNC_HTTP_RESPONSE_STREAM_PREFIX` constant removed after the adoption. **Serves:** platform
hygiene for the streaming lane pattern; the staged pool consumers (graph-run streaming,
wrapper relay pools) live under `bp-agent-orchestration`. **Engine parity:** platform API
surface → Java + Rust lock-step (Java is the reference,
`conv-telemetry-presentation-parity`). Wrappers OUT of scope — they host no edge lane
pool, and the wrapper scope fence keeps platform-registry surface off them.

---

## 1. Problem and driver

The v4.12.0 progressive-rendering milestone introduced the SSE reply-lane pool: 500
single-instance private routes `async.http.response.stream.{0..499}`, each a dedicated
FIFO lane that a streaming request checks out for its lifetime. Registration is
open-coded in `AppStarter.startServer` (AppStarter.java:587):

```java
var streamResponder = new AsyncHttpResponse(contexts);
for (int lane = 0; lane < RESPONSE_HANDLER_INSTANCES; lane++) {
    String laneRoute = AsyncHttpClient.ASYNC_HTTP_RESPONSE_STREAM_PREFIX + lane;
    platform.registerPrivate(laneRoute, streamResponder, 1);
    EventStreamRenderer.releaseLane(laneRoute);
}
```

This works but is suboptimal:

- The family has **no registry-level identity** — the platform cannot tell a pool from
  500 coincidentally-named routes.
- There is **no release counterpart** — acceptable for a process-lifetime edge pool,
  wrong for a general facility (symmetric lifecycle).
- The **next consumers would re-open-code it**: graph-run streaming and wrapper relay
  pools (AI SDLC staged follow-ups) need the same order-preserving lane pattern.

**What is NOT a problem** — already shipped in the streaming release and untouched by
this spec: the `/info/routes` family compression (`ActuatorServices.compressRouteFamilies`,
display-only by design) and the 10-minute `ManagedCache` view (`localRoutingCache`,
ActuatorServices.java:42).

## 2. The pattern being formalized

A **route pool** is a set of numbered singleton routes `{prefix}.{n}` sharing one lambda
instance. Each member has `instances=1`, so each lane is a strict FIFO mailbox; exclusive
checkout of a lane gives a stream per-conversation ordering while unrelated streams run
concurrently through their own lanes. Registration and checkout are separate concerns:
this API owns registration only; checkout stays with the consumer
(`EventStreamRenderer.LANE_POOL`, a rotating FIFO `ConcurrentLinkedDeque` with
`checkoutLane()`/`releaseLane()` — head checkout, tail return, so selection
round-robins through the pool; refined 2026-08-31 from the original LIFO stack).

## 3. Decisions

- **D1 (RATIFIED): names are `registerRoutePool` / `releaseRoutePool`.** The concept
  name `registerStreams` is disqualified: `Platform.registerStream(String, StreamFunction)`
  already exists for the `StreamFunction` API (with `registerPrivateStream`, until the
  same-day follow-up collapsed the pair to a private-only `registerStream`) —
  one character apart with entirely different semantics. The abstraction is a pool
  of order-preserving lanes, not a stream; the codebase already says "pool-style route
  families" (actuator javadoc) and "pool of dedicated single-instance reply lanes"
  (AppStarter comment).

- **D2 (RATIFIED): pools are private-only.** No `isPrivate` parameter. A checkout pool
  is an in-process rendezvous — a remote peer cannot participate in local lane checkout,
  so advertising members to the service mesh is meaningless. Private-only also removes
  the mesh-advertising hazard (see D3) at the root, and sidesteps the style mismatch (the
  platform API expresses privacy as method pairs, never a boolean). If a public use case
  ever materializes, add it then.

- **D3: `getLocalRoutingTable()` is UNTOUCHED** — a correctness constraint, not a
  preference. It returns the live registry (Platform.java:371) and has functional
  consumers: `ServiceRegistry.registerMyRoutes()` advertises every non-private entry to
  the Kafka mesh (ServiceRegistry.java:444), and rest-spring-4's `AppLoader` autowires
  every registered function from `.values()` (AppLoader.java:60). A collapsed display key
  such as `"x.0 - 499"` is not even a valid route name (`validServiceName` rejects
  spaces) and must never appear in the routing table. The collapse stays
  presentation-only in `ActuatorServices` where it shipped.

- **D4: numbering is 0-based** — `{prefix}.0 … {prefix}.{count-1}`, matching the shipped
  lanes. Adopting the API for the existing pool is then observationally invisible: same
  trace `service` names, same `/info/routes` display (`async.http.response.stream.0 - 499`),
  no test churn.

- **D5: re-registration follows the house RELOAD convention.** `register()` on an
  existing route releases-then-replaces with a warning; `registerRoutePool` on an
  existing prefix does the same for the whole member set — release all old members,
  register the new set — which cleanly handles a count change. WARN log, never a refusal.

- **D6: integrity guards live at mutation time, not read time.** The concept's
  first/last-member sampling misses holes (a single `release("{prefix}.2")` in a count=3
  pool passes unseen). Instead: `register()` and `release()` log a WARN when their target
  route is a member of a registered pool (never refuse — the house never refuses
  reloads). Membership is range-checked: a route is a member iff its prefix is in the
  pool registry AND its last segment is canonical digits (no leading zeros) AND
  `0 <= n < count` — so a user route `{prefix}.10` beside a count=3 pool is NOT a member.

- **D7: pool mutations are atomic** under a private `ReentrantLock` (Eric's refinement:
  on Java 21 a `synchronized` block still pins a virtual-thread carrier, and the codebase
  already standardizes on `ReentrantLock`; startup-time operations, contention is nil).
  `releaseRoutePool` removes the pool-registry entry FIRST, then
  releases members — so its own internal `release()` calls never trigger the D6 warning,
  and a concurrent actuator read sees at worst a transient plain-route view (benign:
  the display heuristic renders irregular families individually, no ERROR spam).

- **D8: display and cache are unchanged in v1.** `compressRouteFamilies` already renders
  pools correctly (contiguous + uniform-concurrency guards) and is display-only; wiring
  the pool registry into the display for exactness is a possible later refinement, only
  if a false-positive collapse of a user family ever bites in practice. The
  `localRoutingCache` 10-minute TTL already accepts staleness across ALL register/release
  mutations — pool operations add nothing new, so no invalidation hook.

- **D9: `registerRoutePool` returns the ordered member route list** (`List<String>`).
  Callers (checkout pools) need the generated names to seed their structures; returning
  them avoids duplicated name derivation and makes the naming contract explicit.
  `releaseRoutePool` returns `boolean` like `release()` (true when the pool existed).

- **D10 (RATIFIED 2026-08-30): KEEP `async.http.response.stream.{n}`** — the rename to
  `http.response.stream.{n}` is rejected. The `async` prefix is the namespace family of
  the HTTP edge (`async.http.request`, `async.http.response`), and the lanes are
  literally sibling instances of `async.http.response` (same `AsyncHttpResponse` class,
  pool sized to its instances) — renaming the child while the parent stays would split a
  family that sorts, greps, and reads together. The name also shipped in v4.12.0 across
  trace datasets (lane spans' `service`/`from`), `/info/routes`, both engines' docs, and
  the interop report in all four repos.

## 4. API specification

```java
/**
 * Register a pool of private singleton routes "{prefix}.{n}" for n = 0 to count-1.
 * Each member has instances=1 (a strict FIFO lane). One lambda instance is shared
 * across all members - the function must be stateless, the same contract as any
 * multi-instance function. Registering an existing pool RELOADS it: the previous
 * member set is released first (house reload semantics, with a warning).
 *
 * Registration only - lane checkout/return is the caller's concern.
 *
 * @param prefix route-name base, e.g. "async.http.response.stream" (no trailing dot)
 * @param lambda the shared TypedLambdaFunction
 * @param count  number of lanes, must be >= 1
 * @return the generated member routes in order ({prefix}.0 ... {prefix}.{count-1})
 * @throws IllegalArgumentException for null lambda, count < 1, or an invalid name
 */
public List<String> registerRoutePool(String prefix, TypedLambdaFunction<?, ?> lambda, int count);

/**
 * Release a route pool: removes all members and the pool's registry entry.
 *
 * @param prefix the pool's route-name base
 * @return true if the pool existed and was released
 */
public boolean releaseRoutePool(String prefix);
```

Validation: each member name goes through the existing `getValidatedRoute` (so the
prefix must be composed of `0-9 a-z . - _` and contain a dot); member registration is
identical to `registerPrivate(member, lambda, 1)`. Idle-lane cost is a `ServiceQueue`
plus an empty mailbox — small, per the 500-lane precedent; no hard cap on `count`.

## 5. Registry model

```java
// prefix -> count; lifecycle metadata only - never consulted for routing
private static final ConcurrentMap<String, Integer> poolRegistry = new ConcurrentHashMap<>();
private static final ReentrantLock poolLock = new ReentrantLock();
```

- `registerRoutePool`: under `poolLock` — if `poolRegistry` has the prefix, release the
  old member set (WARN "Reloading route pool ..."); register `count` members via the
  private register path; put the prefix/count entry; return the member list.
- `releaseRoutePool`: under `poolLock` — remove the entry first (D7), then `release()`
  each former member; return whether the entry existed.
- Membership helper (for the D6 guards in `register()`/`release()`): prefix =
  `route.substring(0, route.lastIndexOf('.'))`, suffix must be canonical digits and
  within `[0, count)` for a registered prefix.

## 6. Adoption — refactor the shipped SSE lane pool

`AppStarter.startServer` (AppStarter.java:587) becomes:

```java
var streamResponder = new AsyncHttpResponse(contexts);
platform.registerRoutePool("async.http.response.stream", streamResponder,
                           RESPONSE_HANDLER_INSTANCES)
        .forEach(EventStreamRenderer::releaseLane);
```

`AsyncHttpClient` keeps `ASYNC_HTTP_RESPONSE_STREAM_POOL` (the un-dotted pool name); the
old dotted `ASYNC_HTTP_RESPONSE_STREAM_PREFIX` constant had no remaining consumer after
the adoption and was removed. Behavior-identical by construction: same names, same
0-based numbering, same display, same trace datasets.

## 7. Out of scope / non-goals

- **Checkout mechanics** — stay in `EventStreamRenderer`. If a second consumer wants the
  checkout half, consider promoting the rotating deque into a pool handle then; do not block
  this round on it.
- **Public pools** (D2) — add only when a concrete case appears.
- **`getLocalRoutingTable()` changes** (D3) — permanent non-goal.
- **Actuator display / cache changes** (D8).
- **Wrapper repos** — no lane pools; scope fence.

## 8. Test plan

New platform-core unit tests:

- register creates exactly `{prefix}.0..{count-1}`, all private, `hasRoute` true; the
  returned list is ordered and matches.
- release removes all members and the registry entry; returns false when absent.
- reload with a different count leaves exactly the new set (no orphans either way).
- `count < 1`, null lambda, invalid prefix → `IllegalArgumentException`.
- single `release()` of a member logs the D6 warning; a later `releaseRoutePool` still
  cleans the remainder.
- `register()` over a member logs the D6 warning (and reloads, house semantics).
- a user route `{prefix}.10` beside a count=3 pool is untouched by pool release and
  triggers no member warning.

Regression: `RegistrationVectorsTest` and `AdminEndpointTest` unchanged — the
`/info/routes` display entry stays byte-identical (`async.http.response.stream.0 - 499`);
full platform-core suite green.

## 9. Rollout

1. Java first (reference implementation): `Platform` API + guards + AppStarter adoption
   + tests.
2. Rust twin in the same round: `register_route_pool` / `release_route_pool` on the
   Platform twin, adoption at the lane registration in `server.rs`, twin tests.
3. Docs: developer-guide platform API section gains the two methods (both engines).
   Interop report untouched.
4. Propose an ADR at implementation time (durable platform API addition; human-gated).
