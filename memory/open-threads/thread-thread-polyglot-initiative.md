- [ ] (feature — design RATIFIED D0–D8 2026-08-22; **P1–P4 COMPLETE across the four
  repos**; completed-phase narrative condensed 2026-09-01 — full detail: origin log,
  2026-08-24-170545, and the wrapper repos' memory) **Polyglot initiative — python/node.js
  Event-over-HTTP wrappers.** Shipped: wrapper reboots + envelope codecs vs the golden
  vectors + live python⇄node/engine interop (python PR #15, node PR #86, 2026-08-22);
  the graph.task event-over-http guard, both engines (Java
  [PR #292](https://github.com/Accenture/mercury-composable/pull/292), Rust PR #212 —
  the sole content of field release v4.11.11); the wrapper feature round — primitive
  event bus, actuator endpoints, log.format (python PR #17, node PR #87); P4 docs —
  wrapper sites live + engine chapters/ADR-0016 + nav parity (python PR #20, node
  PR #89, Java PR #294, Rust PR #214/#215). Lesson: new guide pages linked from packaged
  references must join the ai-contract-provider files.list inventory on BOTH engines.
  **Remaining — P5 publication, GREEN-LIT by Eric 2026-09-01** ("tomorrow prepare to
  publish Rust, Python and Node to their public artifactory" — the AI-SDLC-iteration
  sequencing gate is satisfied by E0). Scope: python → PyPI (name availability to
  verify), node → npm (legacy mercury-composable v4.3.28 history on the name), **plus
  the Rust engine → crates.io (NEW surface, widened beyond the wrapper pair)**. Agent
  prepares everything (dry-runs, metadata, checklists); the publish acts stay Eric's
  per [[eric-release-rhythm]]. Optional extras still offered: a live
  Rust-engine→wrapper drive; Rust layer-tab label parity. Design:
  [[polyglot-event-over-http-design]]; serves [[bp-polyglot-functions]].
  <!-- id: thread-polyglot-initiative | created: 2026-08-22 | last_used: 2026-09-01 | uses: 10 | tier: working | origin: 2026-08-22-164936 -->
