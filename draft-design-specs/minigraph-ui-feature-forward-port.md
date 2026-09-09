# MiniGraph UI Feature Forward-Port

Status: Implementation plan
Run: `ui-forward-port-20260909`
Source behavior: `feature/minimap-jsonpHelp-startButton` / `348b684f`
Target base: latest `main` / `6af49289`

## Goal

Forward-port three already-designed MiniGraph Playground capabilities onto the current UI without
replacing newer graph authoring, layout, connection, undo, session, or panel behavior:

1. JSON-Path uses the shared Help shell with an isolated starter content profile.
2. MiniGraph exposes a default-collapsed, pannable minimap with an accessible toggle and one-shot hint.
3. MiniGraph exposes separate acknowledged `Instantiate` and `Run` toolbar actions, including optional
   graph-input upload, primary-session gating, lifecycle invalidation, and manual-console compatibility.

The target actor is a Playground user. Success is visible when each capability works on the current
UI and the focused tests, complete Vitest suite, typecheck, production build, deployed-bundle parity,
and affected Maven tests are green.

## Acceptance Criteria

- **AC1 — JSON-Path Help:** JSON-Path exposes the existing Help panel, shortcut, persistence, and
  maximize/close behavior, but only its own content profile. Bare `help` resolves locally; unsupported
  MiniGraph topics still reach the backend. MiniGraph Help remains unchanged.
- **AC2 — Minimap:** A non-empty active Graph tab can toggle a default-collapsed pannable minimap by
  control or `Ctrl+M`. The shortcut ignores editable targets and inactive tabs. The one-shot hint and
  mobile toast safe area behave as before. The current thumbnail/detail control remains in the same
  React Flow control stack.
- **AC3 — Run workflow:** MiniGraph renders `Instantiate` and `Run` before Copy in the graph toolbar.
  `Run` remains disabled until backend-acknowledged instantiation and any required input upload finish.
  Only a connected primary session can act; JSON-Path gets no controls. Disabled/busy explanations are
  discoverable by hover and keyboard focus.
- **AC4 — State correctness:** The backend remains authoritative. Typed ProtocolBus events advance the
  transient run mirror; mutation, export, graph identity, session, connection, and instance lifecycle
  changes invalidate or reset it. Delayed uncorrelated acknowledgements cannot unlock a new action.
- **AC5 — Input workflow:** Conservative `input.body` references appear only as hints. Workflow-owned
  upload cancellation/success updates run state only when the callback carries the exact claimed
  invitation path, without closing or relabeling a different upload modal. Invitations that arrive
  behind an open modal are queued rather than discarded.
- **AC6 — Main preservation:** Current measured non-overlap layout, thumbnail mode, panel-aware fit,
  Neo4j connection UX, relation-aware deletion, inline node editing, undo, and session collaboration
  remain intact.
- **AC7 — Delivery:** Source, tests, documentation, and the checked-in Java-served bundle agree. The
  pre-existing local `package-lock.json` normalization is preserved but excluded from this change.

## Non-Goals

- No backend command, WebSocket message, persistence, or public API change.
- No redesign of the three source features.
- No automatic run after instantiation.
- No authoritative graph-input schema inference.
- No merge of old memory/session files or the old nested skill relocation.

## Selected Architecture

Latest `main` is the source of truth for composition and UI structure. Leaf modules and tests that are
new on the old feature branch are transplanted, cleanly-applicable profile/protocol changes are replayed,
and the three overlapping composition files (`GraphView`, `RightPanel`, `Playground`) are integrated by
hand. The minimap and thumbnail actions share one React Flow `Controls` instance; the run mirror owns no
server state and consumes typed events from the existing ProtocolBus.

This is an **already-approved, non-durable forward-port**. It preserves the old accepted behavior and
the current approved frontend boundaries; it creates no new architecture decision or public contract.

### Rejected alternatives

- **Merge the old branch:** rejected because it brings stale main history, obsolete UI composition,
  memory conflicts, and deleted/moved skill files.
- **Cherry-pick `348b684f`:** rejected because the monolithic commit includes 62 files and would ask
  Git to reconcile obsolete `GraphView`/`Playground` shapes.
- **Rewrite from screenshots/requirements:** rejected because the source branch already contains
  reviewed state machines, accessibility behavior, and regression tests.

## Source Of Truth And Boundaries

| Concept | Owner | Derived UI state | Invalidation |
| --- | --- | --- | --- |
| WebSocket/session status | Backend + `useSessionCollaboration` | primary/connected gating | connection epoch/session reset |
| Graph instance/run state | Backend text acknowledgements | `useGraphRunWorkflow` phase | graph mutation/export/identity/session/connection |
| Graph model | fetched graph API response | rendered React Flow nodes/edges and input hints | graph refresh/identity change |
| Help availability/content | `PlaygroundConfig` + bundled profiles | active topic/open panel | route config/profile change |
| Minimap visibility | `GraphView` instance state | React Flow MiniMap | component lifetime; intentionally not persisted |
| Upload modal ownership | `useMockUploadModal` path ref | open modal and success badges | exact-path close/success/cancel |

Boundary contracts:

- Existing WebSocket strings remain centralized in `graphRunProtocol.ts` and are converted to typed
  events by `protocol/classifier.ts`.
- React composition crosses only typed props through `Playground` → `RightPanel` → `GraphView`.
- JSON uploaded to the existing session-scoped mock endpoint retains current validation and encoding.
- No new storage or network boundary is introduced; localStorage keys remain route/profile scoped.

## Failure And Lifecycle Behavior

| Situation | Behavior |
| --- | --- |
| no graph / disconnected / subscriber session | actions disabled with a state-aware explanation |
| command send fails | return to idle and show an error toast |
| setup acknowledgement is slow | enter `outcome-uncertain`; continue quarantining the old outcome |
| graph changes during setup/run | close only workflow-owned input and invalidate the pending result |
| upload cancelled | return setup workflow to idle; manual upload behavior remains unchanged |
| unrelated upload modal completes while graph input is pending | ignore its path; keep Run locked |
| workflow invitation arrives behind another modal | queue it and open it when the active modal closes |
| malformed JSON upload | existing inline validation/error behavior; run state does not advance |
| run aborts | reset and show an error toast; completion resets without a redundant toast |
| inactive graph tab / editable focus | minimap shortcut is ignored |
| unsupported JSON-Path help topic | command is not captured locally and continues to the backend |

User data is limited to existing graph responses, commands, and mock JSON. Existing validation,
React rendering/Markdown escaping, and backend authorization boundaries remain unchanged. No secrets,
HTML injection, or new privileged operation is introduced.

## Implementation Slices

1. **Help profile:** port profile types/content, configuration, local command resolution, shared Help
   shell plumbing, and focused tests. Rollback is removal of the JSON-Path config flags/profile.
2. **Minimap:** port minimap leaf modules and tests; integrate into the current shared Controls stack;
   add shortcut docs and mobile overlay spacing. Rollback removes the control without affecting layout.
3. **Run workflow:** port protocol parser/events, state hook, toolbar controls/icons, modal extensions,
   and tests; hand-integrate current `Playground`/`RightPanel`/`GraphView`. Rollback removes the optional
   `supportsGraphRun` plumbing and leaves console commands untouched.
4. **Delivery:** run full verification and deploy the Vite output into the checked-in Java public
   resources. Generated bundle changes are accepted only when parity with `dist/` is proven.

## Verification Mapping

| Acceptance | Evidence |
| --- | --- |
| AC1 | help content/resolver/HelpBrowser/Playground tests + full Vitest |
| AC2 | GraphMinimap, hint, GraphView control-composition tests + typecheck + UI inspection where available |
| AC3–AC5 | graph protocol, input-path, run-hook, toolbar, mock-upload and Playground wiring tests |
| AC6 | existing full Vitest suite, current layout/connection/undo tests, diff inspection |
| AC7 | `npm run typecheck`, `npm test`, `npm run release`, dist/public parity, affected Maven tests, `git diff --check` |

## Rollout And Rollback

The feature is isolated by existing `PlaygroundConfig` flags (`supportsHelp`, content profile, and
`supportsGraphRun`). Removing those optional flags disables the new surfaces without data migration.
The old branch remains an immutable comparison point. Runtime diagnosis remains the visible console,
typed ProtocolBus events, action state/tooltips, and existing error toasts.

## Appendix - UI Loop Engineer Planning Artifact

**Phase 0 - Domain calibration**
- Domain mix: Frontend UI/rendering 45%, state/data flow 35%, product workflow 15%, build 5%.
- Dominant failure mode: composition bugs caused by transplanting old components over newer shared UI primitives.
- Pre-mortem watch-items: duplicate React Flow controls; multiple owners of run state; overwriting current authoring/undo integration.
- Calibration: emphasized primitive composition, backend authority, lifecycle cleanup, and current-regression tests.

**Step 1 - Requirement and journeys**
- Actor / trigger / success signal: Playground user selects Help/minimap/run actions and receives the accepted current behavior.
- Primary journeys: JSON-Path Help; minimap toggle/hotkey/hint; instantiate without input; instantiate with input; run/abort/invalidate.
- Non-goals: backend changes, auto-run, redesign, old-history merge.

**Step 2 - Research and primitive budget**
- Framework/platform sources: current `GraphView`, `Playground`, ProtocolBus, session hook, React Flow Controls/MiniMap usage, source-branch tests and memory.
- Applicable primitive levels: React Flow primitives; current typed hooks/ProtocolBus; raw local state/listeners; static Help-only fallback; do nothing.
- Lightest workable primitive: reuse source leaf modules while composing through current main props/hooks.

**Step 3 - Anti-anchoring and compatibility**
- Silent assumptions: old composition is obsolete; source behavior remains desired; backend messages are unchanged; current main owns layout/authoring; package-lock diff is unrelated.
- Fresh-team delta: a fresh implementation would still use current Controls, ProtocolBus, config flags, and existing modal rather than add parallel primitives.
- Existing behavior contract: source features restored; main layout/authoring/undo unchanged; backend/public contracts unchanged.

**Step 4 - Three architectures**
- Option A - root primitive: merge/cherry-pick old branch and resolve conflicts.
- Option B - root primitive: forward-port leaf modules and hand-compose into current main.
- Option C - root primitive: reimplement features around current components without source code reuse.
- Picked: Option B; it retains reviewed logic with the lowest compatibility risk.
- Axis spread verified: Git/history merge vs typed component composition vs clean rewrite differ in root primitive, abstraction, and review surface.

**Step 5 - Source of truth and boundaries**
- Source-of-truth inventory: backend owns graph/session/run truth; config owns feature availability; component instance owns minimap; modal path ref owns upload.
- Boundary map: typed React props, typed ProtocolBus events, existing WebSocket strings, existing mock-upload HTTP call, scoped localStorage.

**Step 6 - Contracts**
- Boundary-crossing contracts: existing graph lifecycle strings/events, optional config flags/profile, GraphRunControls props, exact upload path ownership.
- Shared constants / schema strategy: `GRAPH_RUN_COMMANDS` and parsers centralize strings; TypeScript unions/interfaces centralize events and phases.

**Step 7 - Complexity budget**
- Budget exceptions and justification: no new dependency; one existing string boundary; no new per-node component except React Flow's existing MiniMap rendering.
- Feature-disable and per-N results: MiniMap uses native panning and existing default zoom behavior; one instance per active graph, not per node.
- Authority-claim audit: claims are grounded in current/source files and source-branch test/session evidence; red-flag wording avoided.

**Step 8 - Purity discipline**
- Pure components claimed: protocol parsers and input-path collector are deterministic helpers; React hooks/components are not labeled pure.
- Mechanical enforcement or not applicable: deterministic unit tests cover helper outputs; no global mutable state in helpers.

**Step 9 - Primitive fit and composition**
- Under-use / over-use findings: use the existing ProtocolBus, Help shell, upload modal, GraphToolbar extraActions, and React Flow controls.
- Composed primitives: MiniMap + Controls; run hook + session + upload; Help profile + local command resolver.
- Overlaps: thumbnail/minimap controls; manual/workflow uploads; authoring/run invalidation; help hotkeys/input focus.
- Guardrails: one Controls stack; exact upload-path ownership; backend acknowledgements; editable-target shortcut guard; optional config gating.

**Step 10 - From-scratch comparison**
- Materially simpler?: No; rewriting would discard reviewed lifecycle and accessibility coverage while preserving the same primitives.
- If yes, redesign adopted: not applicable.

**Step 11 - Failure and lifecycle**
- Failure matrix summary: disconnected, permission, send failure, timeout, stale acknowledgement, mutation, cancellation, malformed input, abort, and unsupported help are defined above.
- Lifecycle data-access findings: refs capture current callbacks/modal ownership; connection epoch resets session state; DOM focus is used only after mount.

**Step 12 - Spike / performance**
- Spike required?: No; no new unbounded loop, network fanout, timing-sensitive layout algorithm, or per-render large-data transformation beyond the accepted source behavior.
- Result or reason not required: source behavior was previously verified with full tests/build and the current MiniMap is library-native.
- Per-N cost: one MiniMap renders the existing graph node collection only while open; input hint traversal is one pass when graph data changes.

**Step 13 - Security / trust**
- Input boundaries: graph API data, WebSocket text, help command text, and user-supplied JSON.
- Validation / auth / encoding: existing parsers, JSON validation, React escaping, and backend session/HTTP controls remain authoritative.

**Step 14 - Observability / rollout**
- Debuggability: console output, typed event classification, state-aware tooltips, and error/info toasts.
- Rollout / rollback: optional config flags and isolated UI commits; no migration or persistent server state.

**Step 15 - Verification**
- Verification mapping summary: focused unit/component tests plus full Vitest, typecheck, release bundle parity, Maven tests, and diff checks.
- Design cross-reference complete: Yes; every acceptance criterion maps to an existing or ported artifact.

**Step 16 - Implementation / consumer readiness**
- Implementation slices: Help profile, minimap composition, run workflow, integrated delivery.
- Review questions / decision log: preserve main architecture, avoid duplicate controls/state owners, verify exact modal ownership and main regression coverage.

**Step 17 - Pre-draft self-check**
- All self-check answers yes?: Yes.
- Enumeration completeness: creation/mutation/cleanup triggers, identity/order/cardinality assumptions, and visible error/no-op states are recorded.
- Cross-iteration regression: preserves stable transport refs, backend-authoritative session state, current measured layout, zero-modal authoring, and compensating-command undo; abandons only obsolete old-file composition.
