- [x] **The transcript audit store: a durable, queryable record of every model turn, written by the pack, with a
  published schema (RFC-0012; design sprint 4, Eric's topic of 2026-11-16).** Round 1 drafted: the working store's TTL
  stays the transaction's expiry; a second, append-only store (PostgreSQL first, the schema published so an installation
  may implement another) keeps a conversation row and a turn row per model turn - trace id per turn, correlation id,
  UTC times, model, status, usage, the request message and the assistant content in the provider's format, the
  normalized calls - written by the helper after the working store's compare-and-set; `required` or `best-effort` on a
  failed write; Colima runs PostgreSQL and pgweb for development; `x-graph-id` and `x-node` from `graph.task` and the
  loop as an engine step. Spec `draft-design-specs/transcript-audit-store.md` (#634, `57a267f02`); rulings R1-R8 (section 11) are Eric's.
  **Round 2 (2026-11-17):** Eric ruled R1-R8 - A, asynchronous; decoupled, a failure logged; `retention` confirmed
  and the Redis record released only after the check; PostgreSQL with psycopg 3 and `pg`; graph context in this build;
  a sample purge script, operations automate; pgweb; the first-turn snapshot - and set the shape: a composable function
  `transcript.audit` with ONE worker, a local event per turn after the compare-and-set, the end-of-run data-integrity
  check (turns in Redis counted against rows in PostgreSQL, the missing persisted) as a second event in the same
  mailbox, so the writes and the check run in order with no race; the record is version 2 and carries each turn's
  facts; a sweep repairs lost events and stopped runs. Section 12 lists the defaults set without a ruling (one
  instance, facts in the record, grace `1d`, the sweep, no release of an open record). Eric confirmed section 12 the same
  day: SETTLED for the build. **Step 1 BUILT 2026-11-17:** mercury-python PR #59 (the version 2 record, the
  `transcript.audit` function with one instance behind the write and check events, the sweep, the Redis `audited`
  field, psycopg 3 as the `postgres` extra, the schema and purge files, the Colima scripts with pgweb, 28 tests and a
  live test; verified live) and this repo's PR #639 (the schema page `docs/guides/transcript-audit-schema.md`, the
  spec's build notes in section 14). **mercury-python #59 MERGED 2026-11-17; step 2 BUILT and MERGED the same day:** mercury-nodejs #126 (the Node
  twin, field for field, the schema files byte-identical). **#639 MERGED 2026-11-17** (squash `4a9e8dc0e`, after its CI fix: the
  skill inventory `files.list` must list every docs/guides page). **Step 3 BUILT in both engines 2026-11-17:** Java PR
  #641 (`GraphTask.withGraphContext`, `PlatformGateway` turn headers; squash `e1cefd4cb`) and Rust PR #414 (Increment 204),
  **both MERGED 2026-11-17**. **Step 4 DRIVEN 2026-11-17:** five `order-assistant` conversations on Haiku 4.5 - three through the Python
  helper (one with PostgreSQL paused, one with it stopped: the writes failed by type and the sweep repaired the open
  record 46 s after the restart), one resumed by the Node helper from the Python-written record, one all on Node - every
  turn in PostgreSQL with the graph and the node, one correlation id and two trace ids each, the Redis record released
  after each check; the report `docs/test-reports/transcript-audit-live-drive.md` (#642, squash `a738e8883`, MERGED 2026-11-17). The follow-ups in
  the packs - a server-side `statement_timeout` (a paused database stalled the mailbox) and pgweb's restart policy - MERGED
  the same day (mercury-python #62, mercury-nodejs #129); the packs' threads are closed, the build plan is complete. **CLOSED 2026-11-17: RFC-0012
  PROMOTED to ADR-0035 (Eric, after the drive)**; the durable fact is [[transcript-audit-published-contract]]. Lesson: a
  side effect of a turn is designed as a function behind a local event with one worker - the mailbox is the ordering, the
  record is the carrier, the check is the integrity - and it is proven by a drive that stops the store under it.
  → proposal: RFC-0012
  → serves: vision-mercury-composable
  <!-- id: transcript-audit-store | created: 2026-11-16 | last_used: 2026-11-17 | uses: 2 | tier: working | origin: 2026-11-16-105602 -->
