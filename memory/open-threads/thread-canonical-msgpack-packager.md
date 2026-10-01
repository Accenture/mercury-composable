- [ ] **Canonical MsgPack packager: sorted-key maps and a manifest as one deterministic byte array, integrity external and
  optional (RFC-0002, Open; first Java PR #481).** A field installation must promote a set of related documents (graphs first) as one artifact with a
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
  must pass the vectors. **State (2026-10-01):** Eric narrowed the scope — digital signature, the hash mode and the verifier seam are OUT (the user application
  decides whether to protect the exact bytes and with which algorithm); the work is the key ordering and the MsgPack packaging, with
  byte-for-byte Rust interop as a requirement. The ordering is the packager's own step (Gson has no ordered-keys option; Jackson's would
  sort by UTF-16). First Java PR #481 open (`CanonicalPackager` beside `MsgPack`, builder + `encode`/`decode`/`unpack`, strict read by
  default; 14 + 5 tests; platform-core 523/0): bytes are written through msgpack-core, not `MsgPack.pack` (null dropping is config-dependent,
  Float packs as float32, a BigDecimal zero is "0.00"). **The byte contract is `canonical-package-vectors.json`** — expected bytes from an
  independent encoder written from the spec, not from an engine — with a seeded 60-document differential corpus; `msgpack-core` and `rmpv`
  agree on the smallest-integer and str encodings. RFC-0002 revised and still Open. **Next:** #481 review; the Rust twin against the
  byte-identical vector file plus an engine-to-engine drive (each engine packs the same documents, bytes compared, each strict read accepts
  the other's); then Eric decides promotion (ADR only after implementation and review, per [[conv-proposals-not-in-adr-ledger]]); the
  folder pack/unpack tooling and the graph-set loader RFC stay deferred. Applies [[functions-decoupled-routes]].
  → proposal: RFC-0002
  → serves: vision-mercury-composable
  <!-- id: canonical-msgpack-packager | created: 2026-09-30 | last_used: 2026-10-01 | uses: 2 | tier: working -->
