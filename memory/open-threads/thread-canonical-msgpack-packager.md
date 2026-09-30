- [ ] **Canonical MsgPack packager: sorted-key maps and a manifest as one deterministic byte array, integrity external and
  optional (RFC-0002, Open).** A field installation must promote a set of related documents (graphs first) as one artifact with a
  stable identity. RFC-0002 (`docs/arch-decisions/RFC.md`, merged in PR #470 as `dd889bd5`) proposes a packager beside `MsgPack` in
  platform-core and its Rust twin: map keys sorted recursively in UTF-8 byte order; a canonical MsgPack profile (smallest
  integers, finite float64 only, shortest str/bin headers, exact numbers as strings per RFC-0001, nulls kept as nil, no extension
  types); multiple maps saved in sorted filename order under `{manifest, maps}`, the `manifest` a metadata map (`format`,
  `format_version`, caller-defined string fields, by convention `graph_id`; Eric's direction 2026-09-30). Integrity is not in the
  package: the field picks `none`, `hash` (SHA-256 recorded outside) or a detached `signature` checked through a route-addressed
  verifier seam; the engine never signs and fails closed. **Scope fence:** the graph-set loader (all-or-none registration,
  precedence against manifests per [[graph-manifest-list-later-wins]], the `graph_id` check, a static subgraph-target check
  whose limit is declared per [[clean-knowledge-design-over-engine-coverage]]) is a separate later RFC, the packager's first
  consumer. Open questions: `graph_id` as convention or typed field, strict read by default, reject or widen `Float`, verifier
  route or built-in RSA through `CryptoApi`, the smallest-integer check in `msgpack-core` and `rmpv`, which engines and packs
  must pass the vectors. **State:** raised 2026-09-30 (generalized from a graph-set-specific first draft at Eric's direction),
  no code; waiting for Eric. **Next:** on Promoted, write the ADR, open a `(blueprint)` gap, build the packager and shared
  vectors in Java and Rust, then raise the loader RFC; on Withdrawn, record the reason and close. Applies
  [[functions-decoupled-routes]] (the verifier is a route) and [[conv-proposals-not-in-adr-ledger]].
  → proposal: RFC-0002
  → serves: vision-mercury-composable
  <!-- id: canonical-msgpack-packager | created: 2026-09-30 | last_used: 2026-09-30 | uses: 1 | tier: working -->
