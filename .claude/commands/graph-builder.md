---
description: Build and modify MiniGraph graphs via the Companion API (analysis → design → command generation → execution → verification)
argument-hint: [use case or modification to build, e.g. "manager graph for post-term eligibility"]
---

# Graph Builder

Build graphs interactively via the Companion API (`POST /api/companion/{session-id}`).

**Requested use case:** $ARGUMENTS

(If the request above is empty, ask the user what graph they want to build or modify.)

This command follows the portable, agent-agnostic workflow and syntax reference. Read both before generating commands:

@docs/graph-builder/workflow.md
@docs/graph-builder/minigraph-syntax.md

## NON-NEGOTIABLE RULE: verify after EVERY CRUD operation

This is mandatory and cannot be bypassed, batched, deferred, or rationalized away:

1. **Send exactly one mutating command (create / update / delete / connect / import), then verify it before sending the next.** Never queue multiple CRUD commands and verify in bulk at the end.
2. **The POST response (`"status":"accepted"`) is NOT verification.** It only confirms the command was dispatched — a malformed or no-op command still returns `200 accepted`. Verification means re-fetching the live graph (`GET /api/graph/session/{id}`) and confirming the change actually took effect.
3. **Use [`docs/graph-builder/companion.mjs`](../../docs/graph-builder/companion.mjs) to send commands.** Its `send` subcommand performs this verification automatically and exits non-zero with `VERIFICATION FAILED` if the mutation did not land. Do not hand-roll `curl`/`fetch` calls that skip it.
4. **If a verification fails, stop and fix that command before continuing.** Do not send further commands against an unverified graph.
5. **Runtime is verified too: always `inspect` after `instantiate` and after `run`.** These are not CRUD, but they also fail silently (`"accepted"` does not mean the instance was created or that the run produced output — a single rejected seed line drops the whole instance and every `inspect` then returns `404`). Confirm `input.body` after `instantiate` and read `output.body` (or the graph's output key) after `run`. The `companion.mjs send` helper does both automatically and records the result in the transcript.

The full rationale and the verification contract are in [minigraph-syntax.md](../../docs/graph-builder/minigraph-syntax.md#companion-api-execution).
