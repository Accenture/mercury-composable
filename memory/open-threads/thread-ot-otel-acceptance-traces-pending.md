- [ ] (feature) **Confirm the two v4.12.11 field-acceptance traces in Dynatrace.** Raised by the
  2026-09-16 memory review: this commitment lived only inside continuity's `latest_release` entry,
  which is rewritten at the next release, so it had no durable home.
  Dynatrace support is locating **`45a6e43c67ef4087b9351956f50130d7`** (write path, `create-profile`,
  6 spans) and **`1c32bbe5e6b44f12aec201ce78faca3e`** (read path, `get-profile`, 5 spans), both
  submitted 2026-09-16 under service `mercury-otel-cert` with **zero** export failures — so the
  backend returned 2xx for all eleven spans and only UI visibility is unconfirmed.
  **What to check:** both queryable, and **`OTel scope version` = 4.12.11** (4.12.10 for the earlier
  certification traces). That field is resolved at runtime from the running application, so it is the
  proof that the *released* artifacts submitted these traces rather than a leftover build — the
  failure mode that cost two dead ends during the round.
  **On reply:** update Scenario 6's Dynatrace row in `docs/test-reports/otel-dynatrace-certification.md`
  from *pending* to the result, and close this thread. Do NOT rewrite the scenario — the local
  evidence stands on its own and the report deliberately distinguishes what was proven locally from
  what the backend confirmed.
  Does not block anything: v4.12.11 shipped with the feature opt-in and default-off, and the
  certification proper ([[otel-optional-service-and-negative-control]]) was already UI-confirmed.
  → serves: vision-mercury-composable
  <!-- id: ot-otel-acceptance-traces-pending | created: 2026-09-16 | last_used: 2026-09-17 | uses: 2 | tier: working | origin: 2026-09-16-211927 -->
