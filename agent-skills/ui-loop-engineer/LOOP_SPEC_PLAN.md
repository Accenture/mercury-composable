# Full Planning Checklist (v5)

This checklist is a bundled reference used only by the parent `ui-loop-engineer` skill during
substantial specification work. It is not an independently invocable skill.

**Requirement**: Use the framed requirement from the parent workflow's delivery contract.

**Mode**: The parent workflow runs this checklist autonomously. The user's initial
`ui-loop-engineer` request authorizes a full spec draft after a clean Step 17, while the parent
workflow owns the implementation transition. This does not authorize bypassing a repository human
gate.

If the parent workflow does not supply a material requirement, return a human gate instead of
inventing one.

## Operating Rules

Run **Phase 0** first, then **Steps 1-17** in order. Maintain one versioned planning artifact in the
parent workflow's working plan or gitignored scratch space; return only gate status and a concise
decision digest to the parent, then include the required appendix in the final spec. Do not serialize
the same checklist results inline, in the appendix, and again as a second summary.

Do **not** draft the full spec until:

1. Every phase/step is clean, with no halt condition fired.
2. Step 17 self-check returns honest "yes" on all items.
3. The parent `ui-loop-engineer` invocation supplies prior authorization to continue.

Classify every halt condition:

- **Corrective retry:** the condition prescribes a deterministic re-investigation, regeneration,
  comparison, or redesign and requires no new human decision. Apply that remedy once for the step and
  rerun it. A repeated failure becomes a human gate.
- **Decision halt:** the condition needs material information, approval, a risk tradeoff, or an
  architecture decision reserved for humans. Stop immediately at a human gate.

For a human gate, report:

- the step;
- the halt condition;
- what information or architecture change would unblock it.

Return the halt to the parent workflow so it can ask the user how to proceed; do not silently infer
the blocked decision. Retry budgets are per checklist run and never reset after a redesign.

The checklist's spirit matters more than mechanical completion. Step 0.2 and Step 17 are the anti-ritual gates. Apply them honestly.

The final spec must include the **Planning Artifact Appendix** at the end of this reference.

---

## Phase 0 - Domain Calibration

### 0.1 Domain mix

Pick one or more, with weights:

- Backend framework / platform code
- Frontend UI / rendering
- State management / data flow
- Data layer / persistence
- Network protocol / RPC
- Build / CI / infra
- Security / privacy
- Algorithm / pure logic
- Product workflow / UX

Write the domain mix as one line.

### 0.2 Dominant failure mode

| Domain | Dominant failure mode | Symptom |
|---|---|---|
| Backend framework | Missing the framework's first-class primitive | Manual reply, queue, retry, context, or transaction plumbing |
| Frontend UI | Primitive overshoot or composition bugs | Heavy components with most features disabled; nested event conflicts |
| State/data flow | Multiple sources of truth | UI and backend/cache disagree |
| Data layer | Hidden read/write coupling | Race conditions; stale reads; partial writes |
| Network/RPC | String-typed protocol instead of envelope/contract | Magic strings and weak correlation |
| Build/CI | Bespoke per-feature setup | Many scripts that almost overlap |
| Security/privacy | Trust boundary missed | Unvalidated input, leakage, missing auth |
| Algorithm | Overshoots problem complexity | Too many knobs, weak invariants |
| Product workflow | Engineering detail replaces user outcome | Spec lacks user-visible success/failure criteria |

State the dominant failure mode for this spec in one sentence.

### 0.3 Pre-mortem

List 3 candidate failure modes:

- one from the table;
- one from recent project history if available;
- one specific to this requirement.

### 0.4 Calibration

If the failure mode is not covered by the checklist, add or modify a step and say so explicitly.

Common calibrations:

- Backend framework: emphasize Step 2 and Step 9 under-use checks.
- Frontend UI: emphasize Step 9 composition and per-N render fan-out.
- State/data flow: emphasize Step 5 source of truth and Step 17 state cleanup.
- Network/RPC: emphasize Step 6 contract shape and Step 14 observability.
- Security/privacy: emphasize Step 13.

---

## Step 1 - Requirement Completeness And Actor Journeys

### 1.1 Requirement completeness

Restate:

- actor;
- trigger;
- actor-visible or operator-visible outcome;
- success signal;
- primary failure handling expectation;
- explicit non-goals if known.

### 1.2 Actor journeys

List 3-5 actor-visible journeys. The actor may be an end user, operator, calling system, developer,
or scheduled process. Each journey must include:

- starting state;
- user/system action;
- system response;
- success end state;
- applicable failure, empty, waiting, or no-op state.

### Halt condition

If actor, trigger, or success signal is missing and cannot be inferred safely, ask the user before continuing.

---

## Step 2 - Research Budget

### 2.1 Read framework/platform code

Read the relevant framework/platform capability, not only peer feature code. Cite source by `file:line`.

### 2.2 Span abstraction levels

For each requirement, list applicable primitives at multiple levels:

- heavyweight library/framework primitive;
- lightweight library or existing project primitive;
- raw platform/language primitive;
- configuration, static, generated, or no-code alternative where applicable;
- "do nothing" alternative.

For frontend delivery, explicitly consider static/server-rendered behavior. For backend, build,
protocol, data, or algorithm work, choose an equivalent domain-relevant alternative instead of
inventing a UI option.

### 2.3 Lightest workable primitive

Identify the lightest primitive that could satisfy the requirement. Do not pick yet.

### Halt condition

If the primitive list is dominated by one library/framework, rerun with the constraint: "list five primitives that do not involve `<X>`."

---

## Step 3 - Anti-Anchoring And Compatibility

### 3.1 Silent assumptions

List:

- prior spec principles and whether each still applies;
- silent codebase assumptions inherited from current structure;
- assumptions you are discarding.

### 3.2 Fresh-team check

What would a team build if they had never seen this codebase? State the delta and decide what to keep or discard.

### 3.3 Existing behavior contract

List existing behaviors/protocols this spec touches. Mark each:

- unchanged;
- extended;
- deprecated;
- replaced;
- unknown, needs investigation.

For replaced behavior, state migration and rollback behavior.

### Halt condition

If you cannot name at least one silent codebase assumption, or any touched existing behavior is "unknown", re-investigate before continuing.

---

## Step 4 - Three Architectures (Axis-Diversified)

### 4.1 Required spread

Generate three options that vary on at least three axes:

- root primitive/library/framework;
- rendering/transport/storage strategy;
- abstraction level.

Include:

- one lightest possible option;
- one obvious/codebase-conventional option;
- one framework/platform-native option.

### 4.2 Rubric

Compare:

- new components/modules count;
- implicit assumptions count;
- string-typed protocols count;
- edge cases removed by construction;
- runtime cost per N;
- feature-disable ratio;
- compatibility risk;
- implementation/review size.

Eliminate any option that fails a must-have acceptance criterion or repository constraint. Use a
Pareto comparison across the remaining rubric: prefer an option that is no worse on most criteria
and materially better on at least one. If no option dominates, declare requirement-specific weights
and tie-breakers before scoring; never sum heterogeneous raw values without normalization.

### Halt condition

If all options share the same root primitive or differ only on the same axis, regenerate.

---

## Step 5 - Source Of Truth And Boundary Map

### 5.1 Source of truth inventory

For every important concept, name:

- owner/source of truth;
- read path;
- write path;
- derived copies/caches;
- invalidation/sync rule.

If two owners exist, state the conflict-resolution rule.

### 5.2 Boundary map

List boundaries crossed:

- UI boundary;
- API/RPC/event boundary;
- process/service boundary;
- persistence boundary;
- async boundary;
- trust/security boundary;
- ownership/team boundary.

For each boundary, state the contract.

### Halt condition

If any important concept has no owner, or two owners without a sync/conflict rule, halt.

---

## Step 6 - Contract / Schema Discipline

For every request, response, event, file, config, DB record, CLI output, or local storage shape crossing a boundary, list:

- fields;
- required/optional;
- versioning or migration strategy;
- invalid input behavior;
- example payload;
- producer and consumer;
- whether producer/consumer share constants or generated types.

String protocols must be centralized in one constant set per protocol. A typo should fail compile/tests where practical.

### Halt condition

If a boundary-crossing contract is described only as prose and is load-bearing, define the shape before continuing.

---

## Step 7 - Complexity Budget

| Item | Limit |
|---|---|
| Named implicit assumptions | 5 |
| Tripwire / CI guard test groups | 3 unless justified |
| String-typed protocols | 1 per boundary, centralized |
| "Do not reorder" / "must call X before Y" comments | 0 |
| Disabled features per used primitive | <= 30% |
| Confabulated authority claims without citation | 0 |
| New runtime dependencies | 0 unless justified separately |

### 7.1 Feature-disable count

For each used primitive, list total relevant interactive features and disabled/readOnly/inert features. If disabled ratio > 30%, the primitive may be wrong.

### 7.2 Confabulation audit

Authority, popularity, behavioral-comparison, and parity claims require citation or a spike.

Before declaring this step clean, scan the draft/planning text for:

```text
well within | well-trodden | should be fine | canonical pattern
feels like | matches \w+ semantics | pixel.for.pixel | exactly the same
drop.in | natively support | the standard way | typically
```

Each hit needs a citation, spike, or rewrite.

### 7.3 Per-N quantification

For primitives instantiated per item/request/session, estimate per-instance cost and multiply by realistic N.

### Halt condition

If the budget is exceeded, reconsider architecture before justifying it.

---

## Step 8 - Purity Discipline

If code is labeled "pure", it must be mechanically verifiable:

- no instance/static mutable fields;
- no time/random/network/storage/DOM/framework globals;
- restricted imports or architecture tripwire;
- deterministic tests.

"Pure" is stricter than "unit-testable".

### Halt condition

If a pure core needs a mutex, atomic state, global cache, current time, random, network, or storage, move impurity to a shell or drop the label.

---

## Step 9 - Primitive-Fit Sweep (Under-use, Over-use, Composition)

### 9.1 Under-use tells

Each tell means look harder for a platform primitive:

- manual reply send -> look for deferred return / promise / future / replyTo;
- string header as protocol -> look for correlationId / envelope metadata;
- hand-rolled timer -> look for framework expiry/scheduler;
- custom global context -> look for idiomatic provider/context;
- manual subscription bookkeeping -> look for structured ownership/cleanup;
- bespoke retry loop -> look for retry primitive;
- hand-rolled queue -> look for event bus/channel.

### 9.2 Over-use tells

For each primitive, list:

- what it was designed for;
- what you use it for;
- which features must be disabled.

If design intent and usage differ and disabled features are non-trivial, choose a better primitive.

### 9.3 Composition audit

For each feature composed from multiple primitives, list:

- primitives combined;
- events/lifecycle stages each participates in;
- where they overlap;
- guardrails preventing interference.

Common interference: drag source with buttons, form button click, popover inside modal, focus trap in portal, parent/child click, pointer-events parent/child, textarea shortcuts, custom drag inside graph/canvas.

### Halt condition

Under-use, over-use, or an unguarded composition overlap halts the spec.

---

## Step 10 - One-Page From-Scratch Redesign

Write a one-page redesign that cannot use the draft's root primitives and cannot inherit the anti-anchoring principles you kept.

Compare:

| Draft primitive | From-scratch primitive | Same root primitive? |
|---|---|---|

Start with the simplest thing that could work. If it wins on at least two of assumption count, runtime cost, feature-disable ratio, or component count, adopt it.

### Halt condition

If the from-scratch design is materially simpler, switch to it.

---

## Step 11 - Failure Matrix And Lifecycle/Data-Access Audit

### 11.1 Failure matrix

For each major operation, cover applicable paths:

- validation failure;
- permission/auth failure;
- timeout;
- retry;
- duplicate request;
- partial success;
- stale data;
- concurrent update;
- downstream unavailable;
- user cancellation;
- no-op/early return.

For each path, state what the user/operator sees.

### 11.2 Lifecycle data-access audit

For each runtime datum, state:

- lifecycle moment it is read;
- whether it is accessible then by platform contract;
- fallback if unavailable.

Examples: WebSocket send during CONNECTING, setState during render, DataTransfer during dragover, localStorage blocked, FormData read-once, service worker synchronous respondWith, URL object revocation, refs before commit.

### Halt condition

If any failure path or lifecycle access is "I don't know", resolve it before drafting.

---

## Step 12 - Spike And Performance Trigger

Run a measurable spike if any are true:

- N can exceed 100/1k/10k and cost is per N;
- operation runs per render/request/message;
- unbounded loop over user data;
- network/database fanout;
- synchronous UI-thread blocking;
- large payload/file;
- primitive used outside design intent;
- correctness depends on timing/perf;
- authority claim lacks citation.

A spike has one measurement and one comparison number.

### Halt condition

If you cannot get a publishable spike result for a load-bearing assumption, change the architecture.

---

## Step 13 - Security And Trust Audit

For each input and boundary, state:

- trusted/untrusted source;
- validation and normalization;
- authorization/permission check;
- escaping/encoding/serialization rule;
- sensitive data exposure;
- abuse/rate-limit consideration if applicable.

### Halt condition

If untrusted input crosses a boundary without validation/encoding/authorization story, halt.

---

## Step 14 - Observability, Rollout, Reversibility

### 14.1 Observability/debuggability

For each async/distributed boundary, state:

- correlation id or equivalent;
- log fields/events;
- metric/counter if applicable;
- user-visible error id/message if applicable;
- where a developer debugs it.

### 14.2 Rollout/reversibility

State:

- feature flag/config gate if needed;
- staged rollout path;
- rollback behavior;
- data migration rollback if any;
- compatibility with old clients/protocols;
- blast-radius isolation.

### Halt condition

If the feature changes persistent data or public contracts and has no rollback/compatibility story, halt.

---

## Step 15 - Verification Mapping + Design Cross-Reference

Map each correctness claim to a verification mode:

| Claim type | Verification mode |
|---|---|
| pure function output | unit test |
| banned imports / architecture boundary | AST/ArchUnit tripwire |
| state machine terminal idempotency | inspection + test |
| concurrency invariant | stress test or named premise |
| framework primitive behavior | source citation or integration test |
| external contract | named premise + citation |
| performance bound | spike/benchmark |
| no memory leak | ownership proof or soak test |

Each verification row must reference the design section that creates the artifact it verifies. If
the artifact does not exist, extract it. Do not downgrade verification for a load-bearing claim. For
a non-load-bearing claim, explicitly remove or narrow the claim before selecting weaker evidence.

### Halt condition

Any load-bearing claim without a verification mode or with a broken design cross-reference halts the spec.

---

## Step 16 - Implementation Slicing And Consumer Readiness

### 16.1 Implementation slices

Split the implementation into independently reviewable increments. Each slice names:

- files/modules touched;
- behavior enabled;
- tests added;
- rollback safety.

### 16.2 Consumer readiness

Verify the spec answers:

- Product/manager: what user outcome changes?
- Engineer: what files/contracts change?
- Reviewer: what decisions need approval?
- QA: what scenarios must be tested?
- Operator/support: how failures are diagnosed?

### 16.3 Decision log

Record:

- decision;
- alternatives considered;
- why selected;
- tradeoffs accepted;
- what would make us revisit.

---

## Step 17 - Pre-Draft Review And Cross-Iteration Regression

### 17.1 Self-check

Answer honestly:

1. Requirement and actor journeys are clear? Y/N
2. Three architectures are orthogonal? Y/N
3. Primitive-fit sweep ran both directions and composition audit ran? Y/N
4. Source of truth and boundary contracts are explicit? Y/N
5. Failure matrix and lifecycle audit have no unknowns? Y/N
6. Per-N runtime cost or spike is handled? Y/N
7. Security/trust boundary is addressed? Y/N
8. Observability/rollout/reversibility are addressed? Y/N
9. All authority claims are cited and red-flag scan ran? Y/N
10. Verification rows reference real design artifacts? Y/N
11. Checklist was calibrated to the domain failure mode? Y/N

### 17.2 Enumeration completeness

List:

- state cleanup completeness: creation, mutation, cleanup, trigger;
- implicit assumptions: order, uniqueness, immutability, reference stability, cardinality;
- error/no-op feedback: what user/operator sees.

### 17.3 Cross-iteration regression

If this spec follows prior work in the same area, list:

- patterns preserved, with rationale;
- patterns abandoned, with rationale;
- prior post-mortem/review findings and whether they reappear.

### Halt condition

Any honest "no", any "I don't know", dropped pattern without rationale, or reintroduced prior finding halts the spec.

---

## In-Review Escape Hatch

If review accumulates substantive new issues:

| Review state | Action |
|---|---|
| 1 round, minor issues | Fix and continue |
| 2 rounds, new substantive issues each round | Run one-page from-scratch redesign within the same global budget |
| 3 rounds, any supported actionable issue remains after remediation | Halt for a human decision; do not reset the counter |

---

## Mandatory Planning Artifact Appendix

The final spec must include this appendix, populated from the run:

```markdown
## Appendix - UI Loop Engineer Planning Artifact

**Phase 0 - Domain calibration**
- Domain mix:
- Dominant failure mode:
- Pre-mortem watch-items:
- Calibration:

**Step 1 - Requirement and journeys**
- Actor / trigger / success signal:
- Primary journeys:
- Non-goals:

**Step 2 - Research and primitive budget**
- Framework/platform sources:
- Applicable primitive levels:
- Lightest workable primitive:

**Step 3 - Anti-anchoring and compatibility**
- Silent assumptions:
- Fresh-team delta:
- Existing behavior contract:

**Step 4 - Three architectures**
- Option A - root primitive:
- Option B - root primitive:
- Option C - root primitive:
- Picked:
- Axis spread verified:

**Step 5 - Source of truth and boundaries**
- Source-of-truth inventory:
- Boundary map:

**Step 6 - Contracts**
- Boundary-crossing contracts:
- Shared constants / schema strategy:

**Step 7 - Complexity budget**
- Budget exceptions and justification:
- Feature-disable and per-N results:
- Authority-claim audit:

**Step 8 - Purity discipline**
- Pure components claimed:
- Mechanical enforcement or not applicable:

**Step 9 - Primitive fit and composition**
- Under-use / over-use findings:
- Composed primitives:
- Overlaps:
- Guardrails:

**Step 10 - From-scratch comparison**
- Materially simpler?:
- If yes, redesign adopted:

**Step 11 - Failure and lifecycle**
- Failure matrix summary:
- Lifecycle data-access findings:

**Step 12 - Spike / performance**
- Spike required?:
- Result or reason not required:
- Per-N cost:

**Step 13 - Security / trust**
- Input boundaries:
- Validation / auth / encoding:

**Step 14 - Observability / rollout**
- Debuggability:
- Rollout / rollback:

**Step 15 - Verification**
- Verification mapping summary:
- Design cross-reference complete:

**Step 16 - Implementation / consumer readiness**
- Implementation slices:
- Review questions / decision log:

**Step 17 - Pre-draft self-check**
- All self-check answers yes?:
- Enumeration completeness:
- Cross-iteration regression:
```

---

## Post-Mortem Update Protocol

When the same authorized request explicitly includes and confirms a merge, release, or deployment,
that request owns a one-paragraph post-mortem and proposed checklist update:

```markdown
## Post-mortem: <spec name> (<date>)
**What we missed**:
**Domain**:
**Failure class**: requirement-gap / missing-primitive / primitive-overshoot / composition-boundary / source-of-truth / contract-shape / lifecycle-access / security-trust / observability-rollout / verification-design-mismatch / enumeration-incompleteness / cross-iteration-regression / other
**Would-have-caught**: <step number, or "no current step">
**Checklist update**:
```

This checklist grows from real misses. Otherwise report a post-mortem only as a proposed follow-up;
do not attach it automatically to an unrelated future request. The post-mortem is not part of
implementation Definition of Done unless the current request includes the corresponding merge,
release, or deployment.

## Final Instruction

After running Phase 0 and Steps 1-17:

1. Return the versioned artifact's gate status and concise decision digest to the parent workflow
   without repeating its full contents.
2. Report halt conditions if any.
3. If all clean, state the chosen architecture in 2-3 sentences and classify it as non-durable,
   already approved, or requiring a repository human gate. Continue directly to the full draft;
   return any required design approval to the parent before implementation.
4. When drafting, include the Planning Artifact Appendix.
5. If the requirement is materially unclear, return a human gate to the parent workflow.

Do not skip steps. Do not silently accept unsupported authority claims. Do not generate three architectures that vary on one axis. Do not promise verification artifacts the design does not create. Do not skip the composition audit when primitives are combined.
