- [x] Re-verify invariants (2026-09-16, **complete** — Eric walked the 17+1 set against live-tree
  evidence). **Confirmed unchanged:** the 3 Architectural Invariants, the 5 Stack facts, the 5 Key
  Decisions, conv-add-capability, conv-serialization-gotchas, conv-declare-consulted-references,
  eric-release-rhythm, and the Vision. **Two changes:** `stack-language-java21` re-confirmed with
  Eric's framing (Java 21 baseline, Java 25 now the LTS and the recommended runtime, toolchain stays
  on 21 until Java 25 is mainstream — substance unchanged, the trigger is still field adoption);
  `virtual-threads-rpc` **enriched** from ADR-0024 (dispatch itself is now a per-route virtual thread,
  one mode, event loop only enqueues). **One retirement:** `conv-telemetry-presentation-parity`
  retired as a pure invalidation and archived. Lesson: a fact promoted to `core` can still be wrong
  12 days later — core means "does not decay", not "cannot be retired"; only the cadence prompt
  surfaced it. origin: 2026-09-16-041500.
  <!-- id: ot-reverify-invariants-20260916 | created: 2026-09-16 | last_used: 2026-09-16 | uses: 1 | tier: archive-candidate | origin: 2026-09-16-041500 -->
