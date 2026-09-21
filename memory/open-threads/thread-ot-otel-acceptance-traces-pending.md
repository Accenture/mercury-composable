- [x] (feature) **CONFIRMED 2026-09-17 — the OTel certification is closed end to end.** Dynatrace
  support located both v4.12.11 field-acceptance traces in the UI: `45a6e43c…` (write, 6 spans) and
  `1c32bbe5…` (read, 5 spans), service `mercury-otel-cert`, both `Status OK`, parentage reconstructed
  and the two flow shapes differing as predicted. **`OTel scope version` = 4.12.11 on both** (4.12.10
  on the Scenario 1–5 traces) — the check this thread existed for. Report updated per its own
  instruction (row + "What remains" only, scenario unrewritten): PR #412, squash `680de06f`.
  **Durable lesson:** the discriminator was chosen because it is resolved at runtime and *displayed by
  the backend* — no local check could distinguish the released artifacts from a leftover build, the
  failure that cost two dead ends during the round. Pick evidence the other side has to show you.
  Completes [[otel-optional-service-and-negative-control]].
  origin: `memory/sessions/2026-09-16-211927.md`
  <!-- id: ot-otel-acceptance-traces-pending | created: 2026-09-16 | last_used: 2026-09-17 | uses: 3 | tier: archive-candidate | origin: 2026-09-16-211927 -->
