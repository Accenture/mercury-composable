- [ ] **Ask a person: an agent's question or approval delivered to a person over an occasionally connected channel,
  answered through a return link that resumes the graph (RFC-0014; Eric's concept of 2026-11-19).** The loop can
  already wait - a marked write under the default mode, the ask row of loop spec 7.3, both built in both engines - and
  the graph suspends durably; nothing carried a waiting call to a person or brought a decision back. Round 1 (the same
  day): a composable function delivers and collects while the engine waits; the inbox as a mailbox for a device that is
  not always on; notify anywhere, answer in one place through a return link that opens the wait and resumes only on a
  submit; a reply never starts a fresh run; the message carries the graph id, a description and the correlation id.
  **Round 2 (Eric, R1-R6):** the agent-question UI is one store-and-forward channel like a chat, mail or a topic - a
  standalone app on the pack framework with a streaming server and a PostgreSQL inbox - and the tool knows nothing of
  it: `person.ask` sends one message event with a signed, stateless ticket and the resume link to configured routes;
  the main flow (suspend, send, accept the link's invocation, resume) and delivery-and-collection are separate parts;
  **R7:** no authentication in development, SSO or an OAuth 2.0 access token in production. **Round 3 (Eric):** Q2-Q9
  agreed as proposed - the re-suspend keeps the original expiry, `person.ask` and `agent.question.inbox`, the host
  hook, the hold node writes the texts, the reply page's identity by extension, nothing new in the approval block,
  `plan-assistant`, the page shows the wait as the engine returns it - and **R8:** `kind` gains `advice`, a
  notification with no decision (a status update of a long run, an acknowledgement after a response), appended in the
  inbox where waits upsert. Spec `draft-design-specs/ask-a-person-channel.md` (round 3), raised as PR #653 and MERGED 2026-11-19 (squash `b739f3edd`); the
  engines' loop changes nothing in version 1. **Step 1 BUILT 2026-11-19 (mercury-python #68, MERGED):** `extensions/ask-person` - `person.ask` with the three kinds,
  the ticket, the event to the channel routes, the reply page and endpoint with `none`/`proxy`/`oauth2` and a channel
  app's bearer, `Platform.add_routes`; 72 shared vectors (the HMAC cross-checked with openssl), 100 tests. **Step 2 BUILT 2026-11-19 (here, PR #654, MERGED, squash `52de93675`):** `order-assistant`'s hold path (`notify` → `advise` → `suspend`; the `gate` answering
  410 to a late decision; `acknowledge`, `report-answer`, `report-stop`; `notify-again` after a refused decision; handlers
  for a pack that is down), `plan-assistant` on the ask row, `MockAskPerson`, 12 new or extended tests, the page
  `docs/guides/ask-a-person.md`. Found: `graph.suspend` takes `ttl` as written, so a re-advice renews the wait while
  the ticket keeps its expiry - documented; the original expiry across a re-suspend is the engine refinement for a
  later increment. The Rust twin's Playground lags until its next increment. **Step 3 BUILT 2026-11-19 (mercury-python #70,
  MERGED):** `extensions/agent-question` - the intake upserting a wait and appending an advice (memory or PostgreSQL,
  `question_inbox_seq` as the replay cursor), the page, the replay, the stream with `Last-Event-ID`, the automated
  click to the link's reply endpoint under the app's bearer, `seen`; 30 shared vectors, 48 tests; the spec's section
  19 records the defaults (memory default, the audit store's connection keys, no retention key, states without
  `superseded`). **Step 4 BUILT 2026-11-19 (mercury-nodejs #134, MERGED):** the Node twins of both extensions from the
  shared vectors byte for byte - the same ticket from the same file, the reference inbox on the same rows, ids and change
  sequence - the schema files byte-identical, `Platform.addRoutes` in the Node host; 145 tests. **Step 5 DRIVEN 2026-11-19 (here, PR #657 with the gate fix;
  mercury-python #71 and mercury-nodejs #136 with the first-decision fix):** the whole chain live through the Playground on
  Haiku 4.5 - negative controls, a pack down, the bare link, a device online, a device off and reconnecting, a decision after
  expiry, two devices on one ticket - on the Python pack and the Node twins (`docs/test-reports/ask-a-person-live-drive.md`,
  with the manual regression test at its end). Found and fixed: the gate recognized only `decisions`, so the reply page's
  re-advice of a dead wait ran the fresh path (now a `turn` probe too, both graphs, two tests); the inbox's decision write was
  unconditional, so two devices on one card could leave a late 410 over the accepted decision (an acceptance always wins the
  row, vectors version 2). Narrowed: who decided reaches the audit store only through a refused call's note (the loop
  carrying the decisions into the turn's facts is the follow-up); a wait decided elsewhere stays open on another device's
  card until tried or expired (a question for the review). Next: step 6 (the chat, mail and topic examples) and promotion,
  on Eric's call. **Round 4 (Eric's sprint of 2026-11-20 after the drive; spec section 21, R9-R13; PR #660 MERGED 2026-11-20, squash
  `0e2e6c1b9`):** the questions,
  answers and advices join the transcript audit log, decoupled from the channels - the main flow records (a recorder in
  the ask-person process behind local events, order-independent writes so it may run several instances, a published
  store contract with PostgreSQL out of the box and a memory implementation for tests, the `question` table moved from
  the app's inbox and owned by the main flow's extension, `seen` dropped) and the channels only deliver, so the record
  holds what every channel delivered, an AI node's and a deterministic node's alike; the agent-question app becomes the
  backend of a React UI built in the Node pack and deployed into both packs' `resources/public` (questions and answers,
  a history with infinite scroll, a transcript browser; same origin, no CORS; useful to inspect without answering since
  a topic has no browser of its history); the pack hosts serve static content (`static.html.folder`, BUILT 2026-11-20:
  mercury-python #73, mercury-nodejs #138); identity `none` or `oauth2` alone with PKCE in the browser, `proxy`
  removed (the field's installations use 3-legged SSO and OAuth 2.0 access tokens). Eric's question on relaxing the
  audit writer's single instance: viable with a per-conversation row lock, a monotonic guard and a forward-only mark -
  `llm.transcript.audit.instances`, default 1, the follow-up. Build plan steps 8-11 BUILT 2026-11-20 in both packs (static content
  mercury-python #73 / mercury-nodejs #138, the record #74 / #139, the backend's reads #75 / #140, the React UI
  mercury-nodejs #141 with the bundle in mercury-python #76) and the UI driven live the same day through both backends
  and a Keycloak issuer (`docs/test-reports/agent-question-ui-live-drive.md`, #661 MERGED 2026-11-20): one host defect fixed in both packs - a
  stopping backend held its streams, so the hosts gained `Platform.on_shutdown` / `onShutdown` and the app ends its
  streams there; next step 6 (the chat, mail and topic examples) and promotion (step 7) on Eric's call, with the reply
  page's own sign-in against an issuer as a follow-up.
  → proposal: RFC-0014
  → serves: vision-mercury-composable
  <!-- id: ask-a-person-channel | created: 2026-11-19 | last_used: 2026-11-20 | uses: 7 | tier: working | origin: 2026-11-19-023727 -->
