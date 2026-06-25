# `.githooks/` — vendor-neutral ritual triggers (agent-memory v4.19.0)

These are **committed, vendor-neutral git hooks** that reinforce the after-session ritual for *any*
agent (Claude, Copilot, Kiro, …) — because everyone commits, regardless of which AI did the work. They
are **advisory** (never block) and the **tool runs nothing itself**: git invokes them in your env at your
opt-in (`no-build-step-agent-run`). See `docs/optional-ritual-hook.md` and `DECAY.md` for the rationale.

## First-run init (one command)

A fresh clone has the gitignored skill **adapters absent** and the hook **unactivated** (git can't
auto-run committed hooks on clone — security). Set both up with one idempotent command:

```sh
bash .githooks/init.sh
```

It regenerates the vendor skill adapters **and** runs `git config core.hooksPath .githooks`. **The agent
runs this itself on a first session** (see `AGENTS.md`), so an untrained user does nothing. To activate
the hook alone: `git config core.hooksPath .githooks` (undo: `git config --unset core.hooksPath`).
**CI is the zero-config floor** (`.github/workflows/agent-memory.yml`) — it runs server-side on push/PR
with no per-user setup, so the ritual is enforced even on a clone where the local hook was never activated.

## Hooks

- **`post-commit`** — after a commit: re-syncs skill adapters if a skill changed; and if the commit did
  real work but carried no session log, **auto-stubs one** (`memory/sessions/<ts>.md`) and nudges you to
  enrich it. The stub guarantees the ledger never has a silent gap; the *thoughtful* summary stays the
  agent's job (capture vs. judgment — same split as `memory-lint`).

To deactivate: `git config --unset core.hooksPath`.
