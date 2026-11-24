- [ ] **A single `llm.chat` call in the transcript audit store (RFC-0015, raised 2026-11-24 from a field request,
  mercury-python #77).** A call without `tools` leaves no row, so a graph of bounded AI nodes has no audit of its model
  use. Round 1, drafted and not ruled: with `llm.transcript.audit.single` on, `llm.chat` saves a closed, one-turn,
  version 2 record to the working store under the audit's grace and sends `{conversation, check: true}`;
  `transcript.audit` is unchanged, the sweep repairs a failed write, `tools IS NULL` tells the kinds apart, the tables
  stay and the schema page's meanings are extended.
  - **Why not the request's own design** (rows carried in the event): it departs from ADR-0035's decisions 1 and 3,
    leaves a failed write unrepaired, and holds content in the one worker's unbounded mailbox during an outage.
  - **Probed, not built:** on the Python pack's helper at `89aff42`, with the in-memory stores and no change to the
    function, the check alone wrote both rows, a failed write kept the record, the check sent again wrote and released
    it, a third duplicated nothing. PostgreSQL, Redis and the Node twin are unproven.
  - **For the maintainer's review:** the switch's default (proposed off); `llm.stream` under the same switch (proposed
    yes); the model recorded after a server-side fallback (proposed: the one that answered); a stop reason outside the
    loop's table (proposed `done`); whether the reply may wait for the save; `llm.transcript.audit.instances`.
  - **Raised as PR #668, MERGED 2026-11-24** (squash `cc16cfbc0`). **Build ordered the same day** (the maintainer:
    "proceed with implementation for RFC 0015 and 0016"), under the entry's defaults, which stay open to the review.
    **Step 1 BUILT 2026-11-24:** mercury-python #81 (MERGED 2026-11-24, merge `5a4a0be`) - the switch, the one-turn record and the check event,
    `transcript.audit` untouched; a new shared vector file of 25 cases through the real path, six tests, sixteen
    mutations each caught; the entry's resolution records it. Next: the Node twin, the schema page here, the reader's filter and a live drive. No
    design spec is written. [[llm-chat-attachments]] depends on this for the
    audit's part.
  → proposal: RFC-0015
  → serves: vision-mercury-composable
  <!-- id: llm-chat-single-call-audit | created: 2026-11-24 | last_used: 2026-11-24 | uses: 1 | tier: working | origin: 2026-11-24-051753 -->
