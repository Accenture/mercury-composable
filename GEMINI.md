# mercury-composable — Gemini CLI

Event-driven Java framework (Maven reactor, `com.accenture.mercury`) — self-contained functions wired by YAML event flows.

This project uses the agent-memory shared memory system. **Follow
[`memory/PROTOCOL.md`](./memory/PROTOCOL.md) first.** Root `AGENTS.md` is its universal
discovery shim, forking contributors from Mercury consumers.

The protocol and core memory files are imported below, so they are structurally present at
session start (presence is guaranteed; *attending* to them is still the protocol). Imports
can't express dynamic paths — still scan the newest 2–3 logs in `memory/sessions/` per the
protocol.

@./AGENTS.md
@./memory/PROTOCOL.md
@./memory/instructions.md
@./memory/continuity.md
@./memory/vision.md

Identify yourself as **Gemini CLI** in all session logs.
