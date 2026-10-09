- [ ] **The graph contract: a `schema` on the root and end nodes, discovery, OpenAPI on demand, input validation and a Schema panel
  (Eric's design, 2026-10-08) — the next sprint.** An optional `schema` property on the root node describes `input.body` and one on
  the end node describes `output.body`, each an OpenAPI 3.0 schema object stored through the Playground grammar's composite keys; the
  engine discovers the surface and as much typing as the model gives (the shared `describe graph` scanner, direct evidence, one-hop
  propagation through `model.*`) and the developer or an AI agent fills in the rest; a dev-mode `GET /api/openapi/{graph_id}` answers a
  minimal OpenAPI 3.0 document as a YAML attachment, with a draft variant and a discovery endpoint for the panel; a root `schema` turns
  input validation on as a step the engine ASSUMES (the built-in `graph.schema.validator` through the task skill's machinery, first at
  the root on every run, a root skill after a successful validation, nothing written into the node); the keyword subset is closed and
  the gate refuses more; a Schema panel pre-fills from discovery and from the last run and saves through `update node`. Register entry:
  RFC-0007 (PR #529, docs only, MERGED 2026-10-08 as squash `c4cfff06`; revised for headers in #532: `schema.body` and
  `schema.header` on both nodes, input headers validated beside the body, names case-insensitive). The Upload step's mock headers
  (`POST /api/mock/{id}?namespace=header`, the panel's Headers rows) shipped ahead of the sprint as the dry-run prerequisite: Java #531,
  mercury #369 (Increment 170); all three MERGED 2026-10-09 (`6c53389c`, `46d19b06`, `c4932455`). **WP1 delivered 2026-10-09:** the contract class, the OpenAPI
  document, `GET /api/openapi/{graph_id}` and `/api/openapi/session/{sessionId}`, `describe graph` with types - Java #533, mercury #370
  (Increment 171), one shared fixture `graph-contract-vectors.json` (six cases; the Rust port matched on its first run); query parameters
  out by design; both MERGED 2026-10-09 (`4dfe3dfb`, `df95e04e`). **WP2 delivered 2026-10-09:** the validator function `graph.schema.validator`, the assumed step in both walkers (one synchronous
  request at the first visit to the root, the failure staged under `root.*` so the standard error path applies), the gate rules (an
  unknown or inapplicable keyword, a malformed value or a pattern outside the common regex subset refused with its location; a
  declaration-surface mismatch a WARN at deploy and pack) and the shared `graph-schema-vectors.json` (56 compile, 29 validate, 2
  report cases; the Rust port matched on its first run) - Java #534, mercury #371 (Increment 172), both MERGED 2026-10-09
  (`fd4d39fa`, `867bc56f`); the four open points
  implemented as proposed (the subset, strict types with an integral float as an integer, `additionalProperties` default, cap 10) for
  Eric to confirm at promotion. **WP3 delivered 2026-10-09 (PR #535 and mercury #372 opened by Eric, CI running):** the Schema panel - Tools →
  **Graph schema** and a link in the root/end node editor; Input (root) and Output (end) tabs, one row per body path and per header
  name (path, type, required, description, example, a `declared`/`discovered`/`from last run`/`new` chip, the other keywords a
  read-only chip carried through Save), pre-filled from the WP1 contract view, Fill from last run through `/api/inspect`, Save as one
  `update node` with the node's other properties re-sent, Download of the draft YAML - Java `66829f57`, the Rust bundle twin
  `4958d62b` (Increment 173); vitest 57 files / 453 tests; a live drive on tutorial-8 confirmed the round trip. Two choices to
  confirm: the panel edits path/type/required/description/example only; a Save re-sends the node as the node editor does.
  **WP4 delivered 2026-10-09 (branches pushed, PRs for Eric to open):** the guide page *The graph contract*
  (`docs/guides/knowledge-graph/graph-contract.md`), `help schema`, the command grammar's typed `describe graph` and invariant 7,
  the AI agent guide's recipe *Declare the contract*, `graph.schema.validator` in the configuration reference, the nav/llms/files.list
  entries and five claims on both engines - Java `8877167c`, Rust `9abe0f1a` (Increment 174). Open: promotion of RFC-0007 to an ADR
  on Eric's confirmation of the WP2 and WP3 points.
  Relates [[graph-set-pack-and-deploy]] (the panel and endpoint precedents), [[canonical-packager-wire-contract]] (the shared-vector
  method), [[clean-knowledge-design-over-engine-coverage]] (why the keyword subset is closed), [[conv-proposals-not-in-adr-ledger]].
  → proposal: RFC-0007
  → serves: vision-mercury-composable
  <!-- id: graph-contract-schema | created: 2026-10-08 | last_used: 2026-10-08 | uses: 1 | tier: working | origin: 2026-10-08-190305 -->
