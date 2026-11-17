- [ ] **The transcript audit store: a durable, queryable record of every model turn, written by the pack, with a
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
  day: SETTLED for the build. Next: build plan step 1 (Python), Node in lockstep, the engines' two headers, the
  `order-assistant` drive.
  → proposal: RFC-0012
  → serves: vision-mercury-composable
  <!-- id: transcript-audit-store | created: 2026-11-16 | last_used: 2026-11-16 | uses: 1 | tier: working | origin: 2026-11-16-105602 -->
