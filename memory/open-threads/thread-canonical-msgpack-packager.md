- [x] **Canonical MsgPack packager (RFC-0002, Promoted → ADR-0026; thread id `canonical-msgpack-packager`).** Delivered on both engines, UNRELEASED: Java
  `CanonicalPackager` (#481, the nesting-bound vectors #482, the Sonar cleanup #483) and the Rust twin (mercury #339, Increment 147); the ADR is #484.
  Signature and every integrity concern were left to the user application (Eric). **Lessons:** the interop proof is one shared vector file whose expected
  bytes come from an independent encoder, not a cross-engine drive; `rmpv` counts two depth units per nesting level, so a library-limit corner needs its own
  vector; the ordering must be the packager's own step (Gson has none, Jackson's is UTF-16). Deferred follow-ups (folder tooling, the graph-set loader RFC)
  are named in [[canonical-packager-wire-contract]]. Applies [[conv-proposals-not-in-adr-ledger]].
  → proposal: RFC-0002
  → serves: vision-mercury-composable
  <!-- id: canonical-msgpack-packager | created: 2026-09-30 | last_used: 2026-10-01 | uses: 3 | tier: working -->
