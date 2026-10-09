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
  mercury #369 (Increment 170); all three MERGED 2026-10-09 (`6c53389c`, `46d19b06`, `c4932455`). Open: the open points in the entry (the subset, strict types,
  `additionalProperties`, the violation cap, the draft route), then WP1 (contract class, discovery, endpoints, both engines, one shared
  fixture), WP2 (validator, assumed step, gate, shared vectors), WP3 (the panel), WP4 (docs, help, AI guide, api-playground, claims).
  Relates [[graph-set-pack-and-deploy]] (the panel and endpoint precedents), [[canonical-packager-wire-contract]] (the shared-vector
  method), [[clean-knowledge-design-over-engine-coverage]] (why the keyword subset is closed), [[conv-proposals-not-in-adr-ledger]].
  → proposal: RFC-0007
  → serves: vision-mercury-composable
  <!-- id: graph-contract-schema | created: 2026-10-08 | last_used: 2026-10-08 | uses: 1 | tier: working | origin: 2026-10-08-190305 -->
