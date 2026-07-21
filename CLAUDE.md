# mercury-composable — Claude Code

Event-driven Java framework (Maven reactor, `com.accenture.mercury` v4.9.2) — self-contained functions wired by YAML event flows.

This project uses the agent-memory shared memory system.

**Full context:** read [`AGENTS.md`](./AGENTS.md) for the memory protocol, then:
1. `memory/instructions.md`
2. `memory/continuity.md`
3. `memory/sessions/` (latest 2–3 files)

The hub and core memory files are imported below, so they are structurally present at
session start (presence is guaranteed; *attending* to them is still the protocol). Imports
can't express dynamic paths — still scan the newest 2–3 logs in `memory/sessions/` per the
protocol.

@AGENTS.md
@memory/instructions.md
@memory/continuity.md
@memory/vision.md

Identify yourself as **Claude Code** in all session logs.
