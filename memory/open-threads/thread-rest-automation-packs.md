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
  → proposal: RFC-0018
  → serves: vision-mercury-composable
  <!-- id: rest-automation-packs | created: 2026-11-24 | last_used: 2026-11-24 | uses: 1 | tier: working | origin: 2026-11-24-235243 -->
