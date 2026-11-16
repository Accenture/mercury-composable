- [ ] **The transcript audit store: a durable, queryable record of every model turn, written by the pack, with a
  published schema (RFC-0012; design sprint 4, Eric's topic of 2026-11-16).** Round 1 drafted: the working store's TTL
  stays the transaction's expiry; a second, append-only store (PostgreSQL first, the schema published so an installation
  may implement another) keeps a conversation row and a turn row per model turn - trace id per turn, correlation id,
  UTC times, model, status, usage, the request message and the assistant content in the provider's format, the
  normalized calls - written by the helper after the working store's compare-and-set; `required` or `best-effort` on a
  failed write; Colima runs PostgreSQL and pgweb for development; `x-graph-id` and `x-node` from `graph.task` and the
  loop as an engine step. Spec `draft-design-specs/transcript-audit-store.md`; rulings R1-R8 (section 11) are Eric's.
  Next: Eric's review round 1, then the build plan (Python, Node, the engines' headers, the `order-assistant` drive).
  → proposal: RFC-0012
  → serves: vision-mercury-composable
  <!-- id: transcript-audit-store | created: 2026-11-16 | last_used: 2026-11-16 | uses: 1 | tier: working | origin: 2026-11-16-105602 -->
