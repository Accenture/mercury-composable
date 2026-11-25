- [ ] **REST automation in the language packs: `rest.yaml` as the router of a Python or Node application, so a field
  utility app is built and deployed as an engine application is (RFC-0018, raised 2026-11-24 from the maintainer's
  direction).** The field runs many Python and Node scripts that are not standard anything; a pack serves `/api/event`,
  the actuators, a static folder and the routes an extension mounts in code. Round 1, drafted from the investigation in
  the Python pack (its log 2026-11-24-233943): the engines' REST automation in both packs - the engines' grammar and
  never a dialect, `flow` and the relay refused by name so the grammar shows the scope fence, no new dependency, the
  Rust port's subset in version 1, `/api/event` and the actuators as the default entries, proof by a shared vector
  file across the packs with the engines' `rest-automation.json` as a fixture and the Rust port's tests as the
  checklist. The legacy Node port (tags v4.2.40 to v4.3.28, on Express) is a reading aid, never a dependency source.
  - **The maintainer's ruling (2026-11-24):** the packs' Vision statements amended - the language pack becomes a
    full-service application development server, inspired by the AI SDLC work where utility functions run in the
    packs; the Blueprint gap opened in each pack (`bp-rest-automation-python`, `bp-rest-automation-nodejs`).
  - **For the review:** `/api/event` as a default entry with the engines' default timeout; `upload` in version 1 or
    deferred with the Rust port; the packs' extensions (the agent-question app, the ask-person flow) migrating their
    hand-mounted routes afterwards.
  - **Next:** the shape ruled; then the shared vector file, the Python pack, the Node twin, the polyglot guide here,
    a live drive through a browser and an engine; promotion after the drive. No design spec and no code.
  - **Agreed and built 2026-11-25:** the Python pack's step 1 MERGED as mercury-python #83 (the three modules, the
    shared vector file of 126 cases, 45 tests) and upload the same day as #84 (the bounded form, the vector file at
    version 2, the LLM helper's `llm.upload` as the first intake - the maintainer's AI SDLC use case); the open
    questions on `/api/event`'s default timeout and on `upload` answered, the extensions' migration still open.
  - **Step 2 MERGED 2026-11-25:** the Node twin as mercury-nodejs #146, the vector file byte-identical across the packs.
  - **The last open question answered 2026-11-25 (the maintainer: "for python, please refactor the hand-mounted
    routes with rest.yaml"):** the extensions' routes are `rest.yaml` entries in both packs - mercury-python #87 and
    mercury-nodejs #148 - behind private functions, with authentication functions resolving the identity into the
    `session` map and the device stream as a streaming entry; `Platform.add_routes` / `addRoutes` the escape hatch.
    One departure from the proposal: the engines' `rest-automation.json` is not loaded as a fixture in the packs; the
    shared vector file is the drift guard.
  - **Documented 2026-11-25 (the maintainer: "please update RFC-0018 and the polyglot guide ... add REST automation
    documentation for python and node"), branch `docs/rest-automation-language-packs`, raised as PR #679 and MERGED 2026-11-25 (`b86e9c378`):** RFC-0018's resolution
    records step 2, the adopters and the departure; the polyglot guide gains "REST automation in the packs"; the REST
    automation guide, the grammar and the REST AI agent guide say what a pack serves and refuses (a section, a note
    under the binding modes, a pack checklist); `llms.txt` and `system/AGENTS.md` point at them; each pack's
    documentation site gains its REST Automation guide in the same change set (mercury-python #88 and mercury-nodejs #149,
    both MERGED 2026-11-25). The doc canon and the grammar drift checks are clean.
  - **Next:** the live drive through an engine's `async.http.request` with the trace lineage checked; then promotion
    to an ADR.
  → proposal: RFC-0018
  → serves: vision-mercury-composable
  <!-- id: rest-automation-packs | created: 2026-11-24 | last_used: 2026-11-25 | uses: 4 | tier: working | origin: 2026-11-24-235243 -->
