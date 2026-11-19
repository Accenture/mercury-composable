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
  app's bearer, `Platform.add_routes`; 72 shared vectors (the HMAC cross-checked with openssl), 100 tests. **Step 2 BUILT 2026-11-19 (here, PR #654):** `order-assistant`'s hold path (`notify` → `advise` → `suspend`; the `gate` answering
  410 to a late decision; `acknowledge`, `report-answer`, `report-stop`; `notify-again` after a refused decision; handlers
  for a pack that is down), `plan-assistant` on the ask row, `MockAskPerson`, 12 new or extended tests, the page
  `docs/guides/ask-a-person.md`. Found: `graph.suspend` takes `ttl` as written, so a re-advice renews the wait while
  the ticket keeps its expiry - documented; the original expiry across a re-suspend is the engine refinement for a
  later increment. The Rust twin's Playground lags until its next increment. Next: step 3, the agent-question app in
  the Python pack.
  → proposal: RFC-0014
  → serves: vision-mercury-composable
  <!-- id: ask-a-person-channel | created: 2026-11-19 | last_used: 2026-11-19 | uses: 1 | tier: working | origin: 2026-11-19-023727 -->
