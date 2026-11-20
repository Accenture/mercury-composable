---
title: Test Report — The agent-question React UI driven live through both language packs and Keycloak
summary: Permanent record of the live drive of RFC-0014 round 4, build plan step 11 - the React UI of the agent-question
  app, served by the Python and the Node.js backends from their own origin over the question record and the transcript
  audit store, in a real Chrome driven over the DevTools protocol - through the first page, a live arrival, a decision
  from each backend's UI, the transcripts and the actuator panels, a backend stopped and restarted, two devices on one
  card, a seeded history of 120 advices and the infinite scroll, and sign-in by Authorization Code with PKCE against a
  Keycloak development issuer; one host defect found and fixed in both packs (a stopping backend held its streams),
  two UI fixes.
layer: reference
audience: [developer, architect]
keywords: [ask a person, agent-question app, React UI, question record, infinite scroll, transcript browser, actuator,
  PKCE, OAuth 2.0, Keycloak, server-sent events, graceful shutdown, Kubernetes, test report, RFC-0014]
---

# Test Report — The agent-question React UI driven live through both language packs and Keycloak

*The live drive of RFC-0014 round 4, build plan step 11 (`draft-design-specs/ask-a-person-channel.md`, section 21.4):
the React UI built in the Node.js pack (`extensions/agent-question-ui`, mercury-nodejs #141) and committed as one bundle
into both packs' agent-question `resources/public` (mercury-python #76), served by each backend from its own origin
through the hosts' static content service, over the question record the Python main flow writes and the transcript
audit store the Python LLM helper writes, both in one PostgreSQL; conducted 2026-11-20 (UTC+8) with real Claude calls on
Haiku 4.5 through the Java engine's `order-assistant` graph of `examples/minigraph-playground`. The previous drive
(`ask-a-person-live-drive.md`, 2026-11-19) proved the chain with a headless client standing in for the device; this one
drives the real front-end in a real browser.*

The device was Google Chrome 157 in headless mode, driven over the DevTools protocol with a small script on aiohttp
(no browser extension was reachable in the session): the page's DOM was read and clicked as a person would, the
decisions typed into its forms, the screenshots kept beside the session. Two tabs stood for two devices: one on the
Python backend (8092), one on the Node.js backend (8093), both reading the same record.

## Result in brief

- **The UI held on every scenario, on both backends.** The newest page of the record loaded with the first paint and
  the stream started from its highest sequence; a case posted through the engine appeared under *Waiting for you*
  within a second of the recorder's write, without a reload; a decision from either backend's UI reached the engine
  through that backend's click and the main flow's reply endpoint, resumed the graph exactly once, and the
  acknowledgement and the outcome came back as advices under the same case within two to four seconds; the
  transcripts of a case opened from its link with the three model turns as the model exchanged them; the five actuator
  endpoints answered from the menu; a backend stopped and restarted left the device *reconnecting* and then
  *connected* with nothing missed; two devices deciding one card at the same moment got one acceptance and one
  `nothing waiting`, and both ended on the row as the record left it; a seeded history of 120 advices paged through
  the infinite scroll; and under `oauth2` the UI sent the person to Keycloak, came back signed in by Authorization Code
  with PKCE, carried the bearer on every call and on the stream, and the decision was recorded under the token's name.
- **One host defect found and fixed in both packs.** A device holding the stream kept a stopping backend alive: after
  SIGTERM the Python host stopped listening within two seconds but waited for the stream, which never ends on its own,
  and the process was still there 75 seconds later; the Node.js host's `server.close()` waits for the same connection.
  In a container that is the manager's grace period and a SIGKILL. Both hosts gained a shutdown hook
  (`Platform.on_shutdown` / `onShutdown`: the moment the application starts to stop, before the cleanup and before the
  host waits for its connections) and the agent-question app ends every device's stream there; re-driven, the process
  was gone within a second of SIGTERM and the device reconnected two seconds after the restart, nothing missed.
- **Two UI fixes from the drive.** A case's cards are now shown in the order they were recorded, so an answered wait
  stays above the acknowledgement and the outcome that follow it (its answer had moved its sequence below them); and
  a *Mark all notices as seen* button clears a flood of advices at once - a device that had last looked before 120
  advices arrived was asked for 98 clicks.
- **`seen` is the device's, as the round-4 design says,** and the drive confirmed what that means: the two tabs kept
  their own counts of unseen advices, a case decided on one device stayed under *Waiting for you* on the other until
  its advices were marked seen there, and an older page of the history never asked for attention.
- **4 conversations, 12 model turns, 15460 input and 1387 output tokens on Haiku 4.5** for the four drive cases; the record ended with advice 8, approval 4 of the drive plus 120 seeded
  advices, a change sequence of 137. No prompt or answer text in any pack log.

## What was under test

| Piece | Version | Role |
| --- | --- | --- |
| mercury-nodejs `extensions/agent-question-ui` | PR #141 (branch `feat/agent-question-ui`) | the React UI: four panels, PKCE, the fetch-based stream, the device-side `seen`; built with `npm run release` |
| mercury-nodejs `extensions/agent-question` | PR #141 | the Node.js backend on 8093, `question.store: postgres`, the bundle in its `resources/public`, `question.auth` `none` then `oauth2` |
| mercury-python `extensions/agent-question` | PR #76 (branch `feat/agent-question-ui-bundle`) | the Python backend on 8092, the same bundle, the same modes |
| mercury-python `extensions/ask-person` | main at `3cfb48d` (after #75) | the main flow: `person.ask`, the recorder (`ask.audit: postgres`), the reply endpoint on 8091, the channel to the Python backend |
| mercury-python `extensions/llm-helper` | main at `3cfb48d` | `llm.chat` on Haiku 4.5, the transcript store on Redis, the audit store on PostgreSQL, 8086 |
| Java engine, `examples/minigraph-playground` | main at `cb651e1d6` (after #660), the jar of the previous drive | `order-assistant` with the hold path, 8085; `person.ask` at 8091 and the helper at 8086 by default |
| Keycloak 26.8 (container) | `quay.io/keycloak/keycloak:26.8`, the realm of `extensions/agent-question-ui/dev/realm.json` imported by `dev/keycloak.sh` | the development issuer on 8180: realm `mercury`, public client `agent-question-ui` with PKCE, audience `agent-question`, one test person |
| PostgreSQL 17, Redis 7 (containers) | the containers of the previous drive, restarted | the question record and the transcript audit store; the graph's suspend store and the helper's working store |
| Chrome 157 (headless) | `--remote-debugging-port`, driven over the DevTools protocol | the device: two tabs, one per backend |

The database password, the ticket key, the issuer's admin password and the test person's password were random values
generated for the drive and kept in the shell's environment, never in a file of any repository and never printed. The
commands:

```bash
docker start mercury-redis mercury-transcripts                                  # the previous drive's containers
extensions/ask-person/dev/schema-apply.sh                                        # mercury-python; the question record's schema
mercury-serve extensions/llm-helper/llm_helper.py -Dllm.model=claude-haiku-4-5 \
  -Dllm.transcript.store=redis -Dllm.transcript.audit=postgres -Dllm.transcript.audit.postgres.ssl=false
mercury-serve extensions/agent-question/agent_question.py -Dquestion.store=postgres -Dquestion.store.postgres.ssl=false
mercury-serve extensions/ask-person/ask_person.py -Dask.ticket.key='${ASK_TICKET_KEY}' \
  -Dask.graphs='order-assistant=http://127.0.0.1:8085/api/graph/order-assistant,plan-assistant=http://127.0.0.1:8085/api/graph/plan-assistant' \
  -Dask.channels=agent.question.inbox -Dask.channel.endpoints='agent.question.inbox=http://127.0.0.1:8092/api/event' \
  -Dask.audit=postgres -Dask.audit.postgres.ssl=false
node dist/src/cli.js extensions/agent-question/agent-question.mjs -Drest.server.port=8093 \
  -Dquestion.store=postgres -Dquestion.store.postgres.ssl=false                  # mercury-nodejs; the second backend
java -jar examples/minigraph-playground/target/minigraph-playground-4.12.30.jar  # mercury-composable
KC_BOOTSTRAP_ADMIN_PASSWORD=... extensions/agent-question-ui/dev/keycloak.sh start   # mercury-nodejs; later, for U10
KEYCLOAK_TEST_PASSWORD=... extensions/agent-question-ui/dev/keycloak.sh user
```

The Node.js backend read the record through its own feed (a poll every second) rather than the intake's wake-up: the
main flow's one channel pointed at the Python backend, and both backends are readers of the same table, so one main
flow served two UIs.

## U1 — The first page

`GET /` on either backend answered the bundle's `index.html` (`text/html`, `Cache-Control: no-cache`, an ETag), the
hashed script and stylesheet under `/assets`, the favicon. The page read `GET /questions/config` (`auth: none`), then
`GET /questions?before=&limit=100` - the newest page of the record - and opened `GET /questions/events` with
`Last-Event-ID` set to that page's highest sequence; the header read *connected*. The first case, posted a minute
before the tab opened, stood under *Waiting for you*: the use case as the heading, the graph id and the correlation
id, the refund call with its arguments (`ORD-1002`, 174.0, the carrier's loss), *approve* and *refuse*, a note field,
the expiry.

## U2 — A live arrival

With the tab open, a second case was posted through the engine (`ui-case-2`, ORD-1004; 6 s to the hold). The main flow
recorded the wait and told the Python backend's intake; the feed woke; the card appeared on the tab without a reload -
one navigation entry, no second page request - the badge and the tab's title read 2, the status *connected*.

## U3 — A decision from the Python backend's UI, the advices, *Seen*

The person's name was typed into the header (`operator-ui`); on the first case *approve* was chosen, a note typed, *Send
the decision* clicked. The button read *Sending...*; the click posted `{decisions, decided_by}` to
`POST /questions/1/decide`, the backend forwarded the ticket and the stamped decisions to the main flow's reply endpoint,
the engine resumed the graph (200), and the row came back *answered · decided by operator-ui*. Within two seconds the
two advices arrived on the stream under the same case - *The supervisor's decision was received; the assistant
continues* (turn 2) and *The order assistant answered the customer* (turn 3) - so the badge read 3: one open card and
two advices not yet seen. The case, with no open card left, offered *Seen*; the click moved it into *History*, the badge
fell to 1, and the device's state read `{"upTo":1,"ids":[3,4]}` in local storage: the baseline (the first page's highest
sequence when this device first loaded) and the advices it has seen. The record: row 1 `answered`, `answered_by
operator-ui`, outcome status 200, the note stamped `[operator-ui at 2026-11-20T09:53:44Z]`.

The one visual fault of the drive showed here: the answered wait sat *below* its two advices, because the answer had
moved the wait's sequence past theirs and a case's cards were ordered by sequence. Fixed in the UI the same day: a
case's cards are ordered by the time they were recorded, so a wait stays above what follows it.

## U4 — The Node.js backend's UI: a refusal, the same record

The second tab, on 8093, showed the same two cases (its first page and stream from the Node.js backend). Under the name
`operator-node`, the second case was *refused* with a note. The Node.js click forwarded to the same reply endpoint; the
engine resumed (200); the row read *answered · decided by operator-node* and the two advices followed within four
seconds - the Node.js feed found them on its poll. The record: row 2 answered by `operator-node`, the note stamped
`[operator-node at ...] No refund yet - ask the carrier to trace the parcel first`. On the Python tab, the same case
turned answered through the Python feed and its two advices counted as unseen there - the per-device `seen` at work -
while the Node tab, after its own *Seen*, read `{"upTo":5,"ids":[5,6]}`.

## U5 — The transcripts

From the first case's *transcripts* link (in the history by then) the Transcripts panel opened with the correlation-id
filter set to `ui-case-1` and one request, `GET /transcripts?limit=50&correlation_id=ui-case-1`: one conversation -
`order-assistant · assist`, Haiku 4.5, done, 3 turns, 4,275 tokens. Opening it showed the conversation's id, the model,
the status and the system prompt behind a disclosure, then the three turns with their status, stop reason, tokens, trace
id and the request and response arrays as the model exchanged them - the first request the customer's question, the
second turn's request carrying the tool results, the third turn `end_turn` after the resume.

## U6 — The actuator menu

The Actuator panel opened on `GET /info` (200, `application/json`, the application's name and version) and, one click
each, *Services* (`/info/routes`, the public `agent.question.inbox` and the private `question.health`), *Health*
(`/health`, the question store reporting), *Environment* (`/env`, the opt-in lists) and *Liveness probe*
(`/livenessprobe`, `text/plain`, `OK`) - the same origin, the bearer sent when there is one, the status shown green or
red. The endpoints stay at their paths beside the bundle because a host path always wins over a file; both backends'
boot tests now check the five answer with the bundle deployed.

## U7 — A backend goes down and comes back

**The channel down, the hold stands.** The Python backend was stopped and a third case posted. The engine's reply
carried the link and `delivered: [{route: agent.question.inbox, ok: false, error: ClientConnectorError}]`; the main
flow logged `Channel agent.question.inbox failed - ClientConnectorError` by type and recorded the wait regardless (row 7,
open). Nothing a channel does fails the hold.

**The stop that did not stop.** SIGTERM to the Python backend closed its listener within two seconds (`/health`
unreachable) but the process stayed: aiohttp's graceful shutdown waited for the one request in flight - the device's
stream, which never ends on its own - and the feed kept running meanwhile, so the dying process even delivered the
third case to the connected tab. A controlled repeat with a fresh process and the tab connected sampled for 75
seconds: the process alive and holding its one connection on 8092 at every sample, the tab *connected* throughout, no
reconnect attempt. The Node.js host behaves the same by construction: its `server.close()` waits for the connection.
In a container this is the manager's grace period followed by a SIGKILL on every rollout while a device is connected.

**The fix, re-driven.** Both hosts gained `Platform.on_shutdown` (Python, aiohttp's `on_shutdown`: before the host waits
for the requests in flight) and `Platform.onShutdown` (Node.js: before the cleanup callbacks and before `server.close`),
and the agent-question app ends every device's stream there - the wake-ups close, a stream opened while stopping ends
at once. Re-driven with the signed-in tab connected: SIGTERM at 18:12:01, the process gone and the tab *reconnecting*
at the first sample one second later; the backend restarted, the tab *connected* three seconds after that, still signed
in, nothing missed. The Node.js backend the same: gone within a second, the tab reconnected after the restart.

**After the force stop (before the fix).** The tab read *reconnecting* while the backend was away and *connected*
three seconds after the restart, with the third case present; the stream's resource entries showed the failed attempts
while the port was closed.

## U8 — Two devices on one card

The third case was decided from both tabs at the same moment - *approve* from the Python tab, *refuse* from the Node
tab, the two clicks launched together. The Python click reached the engine first and resumed the graph (200, logged
when the resume completed at 17:59:06); the Node click met the gate's `nothing waiting` (410, logged at 17:59:04 as the
first to return). The record kept the accepted decision - row 7 `answered` by `operator-ui`, outcome 200 - and both
tabs ended on that row: the losing card's alert gave way to *answered · decided by operator-ui* when the feed carried
the row. The first accepted decision stands, as the record's rule says.

## U9 — A seeded history and the infinite scroll

To exercise the scroll with more than one page, 120 advices of a fictitious flow (`nightly-reconciliation`, one case
each, three minutes apart) were inserted straight into the record with the record's own sequence - the history a topic
channel would accumulate - and clearly marked as seeded. The Python tab's device state was cleared and the page
reloaded as a fresh device: its baseline became the newest sequence (133), *Nothing waits for you.*, and *History 100* -
the first page's 100 rows as 100 cases, *Load older* at the end of the viewport. Scrolling the viewport to its end made
the observer load `GET /questions?before=34&limit=100`: 123 cases, the oldest three the drive's own, the button gone
(`more: false`). In the headless browser the observer fired only after a second scroll nudge, because a headless tab
reports its document hidden and intersection observers do not deliver to hidden documents; the *Load older* button is
the fallback either way.

The Node tab, whose baseline was 5 from its first load, then showed 98 of the seeded advices under *Waiting for you* -
each a notice this device had not seen, exactly the design, and exactly the moment a person wants one click rather than
98. The *Mark all notices as seen* button was added the same day; the cases that wait for a decision stay.

## U10 — Sign-in with Keycloak

Both backends were restarted with `question.auth: oauth2` and the realm's issuer, audience and client id in their
environment (`dev/keycloak.sh env` prints the lines). Without a bearer, `GET /questions?since=0` answered 401 on both;
`GET /info` stayed open (200) for the probes.

**The Python backend.** The tab loaded the UI, which read `GET /questions/config` (`auth: oauth2`, the issuer, the
public client `agent-question-ui`, the scopes, the audience), found no session and sent the browser to the issuer's
authorization endpoint: Keycloak's *Sign in to Mercury Composable (development)* page. The test person's name and
password were typed into the form and *Sign in* clicked; Keycloak sent the browser back to `http://127.0.0.1:8092/`
with the code, the UI completed the callback (PKCE verifier from session storage), cleaned the address and rendered
signed in: the header read *operator* (the token's `preferred_username`) with *Sign out*, no name field, the status
*connected* - the stream carried the bearer - and the first page had loaded with 200. A fourth case was posted and
decided from the signed-in tab: *answered · decided by operator*, the backend logging `Row 130 decided by operator via
oauth2 - 200 -> answered`, the record's note stamped `[operator at 2026-11-20T10:11:01Z]`.

**The Node.js backend, the issuer's session.** The second tab opened `http://127.0.0.1:8093/` and was sent to the
issuer, which answered *Invalid parameter: redirect_uri* - the drive runs the Node.js backend on a port the committed
realm file does not list (it lists the dev server, 8092 and 8091). The client's redirect URIs were extended through
the admin API, the tab reloaded, and Keycloak signed the person in from its own session without a second form: the
Node.js UI rendered *operator*, the stream *connected* with the bearer, the token validated by the Node.js backend
against the realm's keys. An installation lists every origin the UI is served from in the client's redirect URIs; the
realm file now says so.

## Defects found and fixed

1. **A stopping backend held its streams** (both packs; U7). `Platform.on_shutdown` / `Platform.onShutdown` added to
   the hosts; the agent-question app ends its streams there (`close_streams` / `closeStreams`); a test in each pack
   opens a stream, calls the hook and sees the stream end and a new one end at once; the hook's order is tested on the
   Node.js platform. mercury-python #76, mercury-nodejs #141 (CHANGELOG Added 10 and 9).
2. **A case's cards were ordered by sequence** (UI; U3), so an answered wait fell below its advices. Ordered by the time
   recorded, id as the tiebreak; the unit test pins it.
3. **The Keycloak script's admin-token call** passed the admin password to curl through a process substitution, which
   curl cannot read as a file; the password now travels on stdin (`--data-urlencode password@-`), still never on a
   command line.
4. **A flood of notices asked for one click each** (UI; U9). *Mark all notices as seen*.

## Findings and follow-ups

- **The hook, not a shorter grace.** aiohttp's `shutdown_timeout` and Node's `server.close()` both wait for a
  server-sent events response that never ends; ending the stream is the extension's job, since only it knows the
  response is a stream, and the hook is the place. Any extension that holds a stream or a socket open should end it in
  `on_shutdown` / `onShutdown` and close its stores in the cleanup.
- **The reply page's own sign-in** (`GET /ask/{ticket}` under `ask.reply.auth: oauth2`, the vanilla PKCE script of
  round 4) was not driven here; it is the next thing to put in front of the same Keycloak, with the agent-question
  app's bearer (`question.reply.token` = `ask.reply.auth.token`) on the click.
- **Sign-out** was not driven; the token lives in the tab's session storage, so closing the tab is the other way out.
- **The seeded history is synthetic**, inserted by SQL with the record's sequence; everything else in the drive came
  through the engine.
- **A headless tab is a hidden document**; the infinite scroll's observer needs a visible tab or a nudge, and the
  *Load older* button is the fallback in every browser.

## Reproduce by hand

The same chain in a visible browser. Infrastructure as the previous report's manual regression test (Redis, PostgreSQL
with both schemas, the helper, the Python main flow with the channel to the Python backend, the Playground), then:

1. **The backend and the UI.** `mercury-serve extensions/agent-question/agent_question.py -Dquestion.store=postgres
   -Dquestion.store.postgres.ssl=false`; open http://127.0.0.1:8092/ - the React UI, served from `resources/public`;
   type a name in the header. For the Node.js backend, `node dist/src/cli.js extensions/agent-question/agent-question.mjs
   -Dquestion.store=postgres -Dquestion.store.postgres.ssl=false` on its default port 8092 instead, or another port with
   `-Drest.server.port`.
2. **A case.** `curl -s -X POST http://127.0.0.1:8085/api/graph/order-assistant -H 'content-type: application/json'
   -H 'x-correlation-id: case-1' -d '{"question": "My order ORD-1002 never arrived. Can you help?"}'`. The card appears
   under *Waiting for you* without a reload; approve with a note and send; the two advices follow; *Seen* moves the case
   to *History*. Click the case's *transcripts* link: the conversation and its turns. Open *Actuator*: the five
   endpoints.
3. **A stop.** With the page open, stop the backend (Ctrl-C or `kill`): the process exits at once and the header reads
   *reconnecting*; start it again: *connected*, nothing missed.
4. **Sign-in.** `KC_BOOTSTRAP_ADMIN_PASSWORD=<throwaway> extensions/agent-question-ui/dev/keycloak.sh start` (mercury-nodejs),
   `KEYCLOAK_TEST_PASSWORD=<throwaway> extensions/agent-question-ui/dev/keycloak.sh user`, export the lines
   `dev/keycloak.sh env` prints, restart the backend with `-Dquestion.auth=oauth2`, reload the page: Keycloak's sign-in,
   then the UI with the person's name in the header; decide a case - the record carries the name.

## Before promotion

Step 11 of the build plan is done with this drive. Left before RFC-0014's promotion on Eric's call (step 7): the chat,
mail and topic examples of step 6 when a server is available to drive, and the reply page's own sign-in against the
same issuer.
